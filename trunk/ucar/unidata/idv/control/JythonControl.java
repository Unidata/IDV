/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv.control;


import org.python.core.*;


import org.python.util.*;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.idv.IdvConstants;

import ucar.unidata.idv.IntegratedDataViewer;


import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;



import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;

import ucar.visad.display.*;


import visad.*;

import visad.georef.*;

import visad.python.*;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;


import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;



/**
 * Allows for the creation of a display by an end user through the use of Jython
 *
 * @author Jeff McWhirter/Don Murray
 * @version $Revision: 1.39 $
 */

public class JythonControl extends GridDisplayControl {

    /** control id for the parameter */
    public static final String PARAMNAME_THIS = "displayControl";

    /** probe id for the parameter */
    public static final String PARAMNAME_PROBE = "probeData";

    /** control descriptor for this control */
    private static final ControlDescriptor CD = null;


    /** Symbol for no probe */
    public static final String PROBE_NONE = "none";

    /** ID for the point probe */
    public static final String PROBE_POINT = "point";

    /** ID for the level probe */
    public static final String PROBE_LEVEL = "level";

    /** ID for the area probe */
    public static final String PROBE_AREA = "area";

    /** ID for the line probe */
    public static final String PROBE_VERTICAL = "vertical";

    /** ID for the transect probe */
    public static final String PROBE_TRANSECT = "transect";

    /** list of probes - does this need to be public? */
    public static final String[] PROBES = {
        PROBE_NONE, PROBE_POINT, PROBE_LEVEL, PROBE_VERTICAL, PROBE_TRANSECT,
        PROBE_AREA
    };

    /** list of probe names - does this need to be public? */
    public static final String[] PROBE_NAMES = {
        "No probe", "Point probe", "Z level probe", "Vertical probe",
        "Transect probe", "Area probe"
    };


    /**
     *  This control keeps around its own interpreter.
     */
    private PythonInterpreter interpreter;



    /** Holds the data */
    List dataList = new ArrayList();


    /** Hashtable of Jython variable */
    private Hashtable jythonVars = new Hashtable();

    /** Panel for the container */
    private JPanel jythonContainer;

    /** for putting stuff in the legend */
    private JPanel sideLegendHolder;

    /**
     *  For persistence - this is the position of the probe
     */
    private Object initPosition;

    /** the AreaProbe */
    private AreaProbe areaProbe;

    /** the PointProbe */
    private PointProbe pointProbe;

    /** the LineProbe */
    private LineProbe verticalProbe;

    /** the transect probe */
    protected CrossSectionSelector transectProbe;

    /** the level probe */
    private ZSelector levelProbe;

    /** the last data from a probe */
    private Object lastProbeLocation;


    /** label for showing the probe location */
    private JLabel locationLabel;

    /** The categories */
    private String dataCategories;

    /** The display category */
    private String jythonDisplayCategory = "";

    /**
     *  The developerMode property.
     */
    private boolean developerMode = false;


    /**
     *  The probeType property.
     */
    private String probeType = PROBE_POINT;


    /** text field for the label */
    JTextField labelFld;

    /** text field for the categories */
    JTextArea categoriesFld;

    /** gui */
    JTextField displayCategoryFld;

    /** Any code the user has entered */
    private String jythonCode = null;

    /**
     *  The myname property.
     */
    private String myName;

    /** The code editor */
    private JPythonEditor jythonEditor;


    /** Holds menu items from jython */
    private List saveMenuItems = new ArrayList();

    /** Holds menu items from jython */
    private List fileMenuItems = new ArrayList();

    /** Holds menu items from jython */
    private List editMenuItems = new ArrayList();

    /** Holds menu items from jython */
    private List viewMenuItems = new ArrayList();


    /** A flag used when the user selects new data choices. Do we add or replace */
    private boolean replaceNewData = false;

    /** What should we do when the user has chosen new data choices */
    private String newDataCallBack = null;

    /**
     * Ctor
     */
    public JythonControl() {}


    /**
     * Called to make this kind of Display Control; also calls code to
     * made the Displayable.
     * This method is called from inside DisplayControlImpl init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        doMakeProbe();
        if (developerMode && (dataCategories == null)
                && (dataChoice != null)) {
            List l = dataChoice.getDataCategories(true);
            if (l != null) {
                dataCategories = StringUtil.join("\n", l);
            }
        }

        setContents(doMakeContents());
        return true;
    }

    /**
     * init done. call some jython
     */
    public void initDone() {
        super.initDone();
        execJython("handleInit");
        probeMoved();
    }




    /**
     * A hook to allow derived classes to tell us to add this
     * as an animation listener
     *
     * @return Add as animation listener
     */
    protected boolean shouldAddAnimationListener() {
        return true;
    }


    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            lastProbeLocation = getProbeLocation();
            execJython("handleTime", lastProbeLocation);
        } catch (Exception exc) {
            logException("Handling animation time", exc);
        }
        super.timeChanged(time);
    }



    /**
     * Make the probe for this instance
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void doMakeProbe() throws VisADException, RemoteException {
        if (getDisplayInfos().size() > 0) {
            removeDisplayables();
        }
        transectProbe     = null;
        verticalProbe     = null;
        pointProbe        = null;
        areaProbe         = null;
        levelProbe        = null;
        lastProbeLocation = null;

        boolean displayIs3D = isDisplay3D();


        //Make sure we have a probeType
        if (probeType == null) {
            probeType = PROBE_NONE;
        }
        probeType = probeType.trim();

        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if ( !getHaveInitialized()) {
                    return;
                }
                if (evt.getPropertyName().equals(
                        SelectorDisplayable.PROPERTY_POSITION)) {
                    probeMoved();
                }
            }
        };


        SelectorDisplayable probe = null;
        //TODO: Have the non-point probes be fixed in z
        if (probeType.equals(PROBE_POINT)) {
            pointProbe = new PointProbe(0.0, 0.0, 0.0);
            probe      = pointProbe;
            if (initPosition != null) {
                pointProbe.setPosition((RealTuple) initPosition);
            }
        } else if (probeType.equals(PROBE_LEVEL)) {
            levelProbe = new ZSelector(-1, -1, -1);
            probe      = levelProbe;
            if (initPosition != null) {
                levelProbe.setZValue(((Real) initPosition).getValue());
            }
        } else if (probeType.equals(PROBE_AREA)) {
            areaProbe = new AreaProbe();
            probe     = areaProbe;
            if (initPosition != null) {
                areaProbe.setPosition((RealTuple) initPosition);
            }
        } else if (probeType.equals(PROBE_TRANSECT)) {
            transectProbe = new CrossSectionSelector();
            transectProbe.setZValue(.95f);
            probe = transectProbe;
            if (initPosition != null) {
                transectProbe.setPosition((RealTuple[]) initPosition);
            }
        } else if (probeType.equals(PROBE_VERTICAL)) {
            verticalProbe = (getDisplayAltitudeType().equals(Display.Radius))
                            ? new LineProbe(
                                new RealTuple(
                                    RealTupleType.SpatialEarth2DTuple,
                                    new double[] { 0,
                    0 }))
                            : new LineProbe();

            if (initPosition != null) {
                verticalProbe.setPosition((RealTuple) initPosition);
            }
            // it is a little colored cube 8 pixels across
            probe = verticalProbe;
        } else if (probeType.equals(PROBE_NONE)) {}

        if (probe != null) {
            addDisplayable(probe, FLAG_COLOR);
            Color color = getColor();
            if (color == null) {
                color = Color.blue;
            }
            probe.setColor(color);
            probe.addPropertyChangeListener(listener);
            probe.setPointSize(getDisplayScale());
            probe.setVisible(true);
            probe.setAutoSize(true);
        }
        initPosition = null;

    }


    /**
     * Get a variable.
     *
     * @param varName  variable name
     * @return   variable corresponding to the name
     */
    public Object getVar(Object varName) {
        return jythonVars.get(varName);
    }

    /**
     * Set a variable.
     *
     * @param varName   variable name
     * @param value     variable value
     */
    public void setVar(Object varName, Object value) {
        jythonVars.put(varName, value);
    }




    /**
     * Make a Jython procedure with one variable.
     *
     * @param procName    name of the procedure
     * @param p1          procedure param
     * @return  filled out procedure.
     */
    private String makeProc(String procName, String p1) {
        return procName + "(" + p1 + ")";
    }

    /**
     * Make a Jython procedure with two variables.
     *
     * @param procName    name of the procedure
     * @param p1          first procedure param
     * @param p2          second procedure param
     *
     * @return  filled out procedure.
     */
    private String makeProc(String procName, String p1, String p2) {
        return procName + "(" + p1 + "," + p2 + ")";
    }


    /**
     * Set up a property string (ex:, name=value)
     *
     * @param name  name of the property
     * @param value   value for the property
     * @return  property string
     */
    private String prop(String name, String value) {
        return name + "=" + value + ";";
    }

    /**
     * Generate the control's XML
     */
    public void writeToPlugin() {
        setMyName(labelFld.getText().trim());
        String label = getMyName();
        if ((label == null) || (label.trim().length() == 0)) {
            LogUtil.userMessage("You must enter a display name.");
            return;
        }

        /*
        String filename = FileManager.getWriteFile(FileManager.FILTER_XML,
                              FileManager.SUFFIX_XML);
        if (filename == null) {
            return;
            }*/

        label = label.trim();
        String id = "jythoncontrol_"
                    + StringUtil.removeWhitespace(label.toLowerCase());
        String props = prop("displayName", label)
                       + prop("windowVisible", "true")
                       + prop("probeType", probeType)
                       + prop("developerMode", "false");

        String categories =
            StringUtil.join(";",
                            StringUtil.split(categoriesFld.getText(), "\n",
                                             true, true));
        String attrs =
            "\n\t" + XmlUtil.attr(CD.ATTR_ID, id) + "\n" + "\t"
            + XmlUtil.attr(CD.ATTR_DESCRIPTION, label) + "\n" + "\t"
            + XmlUtil.attr(CD.ATTR_CLASS, getClass().getName()) + "\n" + "\t"
            + XmlUtil.attr(CD.ATTR_LABEL, getMyName()) + "\n"
            + XmlUtil.attr(CD.ATTR_DISPLAYCATEGORY,
                           displayCategoryFld.getText().trim()) + "\n" + "\t"
                               + XmlUtil.attr(CD.ATTR_CATEGORIES, categories)
                               + "\n" + "\t"
                               + XmlUtil.attr(CD.ATTR_CANSTANDALONE, "false")
                               + "\n" + "\t"
                               + XmlUtil.attr(CD.ATTR_PROPERTIES, props)
                               + "\n";


        String body = "<property name=\"jythonCode\"><![CDATA[" + jythonCode
                      + "]]></property>";

        String xml = XmlUtil.tag(CD.TAG_CONTROL, attrs, body);
        //        try {
        getControlContext().getIdv().getPluginManager().addText(xml,
                "controls.xml");
        //            IOUtil.writeFile(filename, xml);
        //        } catch (Exception exc) {
        //            logException("Error writing file:" + filename, exc);
        //        }
    }

    /**
     * We have new data. Reload
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void resetData() throws VisADException, RemoteException {
        dataList = null;
        execJython("handleData");
        lastProbeLocation = getProbeLocation();
        execJython("handleTime", lastProbeLocation);
    }

    /**
     * Get the list of DataChoices
     * @return   list of DataChoices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public List getDataList() throws RemoteException, VisADException {
        if ((dataList == null) || (dataList.size() == 0)) {
            dataList = new ArrayList();
            List choices = getInitDataChoices();
            for (int i = 0; i < choices.size(); i++) {
                Data data =
                    ((DataChoice) choices.get(i)).getData(getDataSelection());
                dataList.add(data);
            }
        }
        return dataList;
    }


    /**
     * Execute the jython method
     *
     * @param method jython method to call
     */
    public void execJython(String method) {
        execJython(method, null);
    }


    /**
     * Exectue the probe data
     *
     *
     * @param method method
     * @param probeLocation  probe location
     */
    private void execJython(String method, Object probeLocation) {
        if ((method == null) || (method.length() == 0)) {
            return;
        }

        if (method.equals("handleInit")) {
            saveMenuItems = new ArrayList();
            fileMenuItems = new ArrayList();
            editMenuItems = new ArrayList();
            viewMenuItems = new ArrayList();
        }


        String code = "";
        try {
            PythonInterpreter interp = getMyInterpreter();
            if (probeLocation != null) {
                code = makeProc(method, PARAMNAME_THIS, PARAMNAME_PROBE);
                interp.set(PARAMNAME_PROBE, probeLocation);
            } else {
                code = makeProc(method, PARAMNAME_THIS);
            }
            interp.set(PARAMNAME_THIS, this);
            interp.exec(code);
        } catch (Exception exc) {
            logException("Error evaluating:" + code, exc);
        }
    }



    /**
     * Sample the first data at the probe position
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data sample() throws RemoteException, VisADException {
        return sampleIndex(0, false);
    }


    /**
     * Sample the first data at the probe position and at the current animation time
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data sampleAtTime() throws RemoteException, VisADException {
        return sampleIndex(0, true);
    }

    /**
     * Sample the  data at the index at the probe position
     *
     * @param index Index in the data list
     * @param atTime  If true then also sample at the current animation time
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data sampleIndex(int index, boolean atTime)
            throws RemoteException, VisADException {
        EarthLocationTuple[] probeData = getProbeLocation();
        if (probeData == null) {
            return null;
        }
        List data = getDataList();
        if ((data == null) || (index >= data.size())) {
            return null;
        }
        return sampleData(probeData, (FieldImpl) data.get(index), atTime);
    }


    /**
     * Sample all the data at the probe point
     *
     * @return Sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public List sampleAll() throws RemoteException, VisADException {
        return sampleAll(false);
    }


    /**
     * Sample all the data at the probe point at the current animation time
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public List sampleAllAtTime() throws RemoteException, VisADException {
        return sampleAll(true);
    }


    /**
     * Samle all the data
     *
     * @param atTime If true then also sample at the animation time
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private List sampleAll(boolean atTime)
            throws RemoteException, VisADException {
        EarthLocationTuple[] probeData = getProbeLocation();
        if (probeData == null) {
            return null;
        }
        List data = getDataList();
        if ((data == null) || (data.size() == 0)) {
            return null;
        }
        List result = new ArrayList();
        for (int i = 0; i < data.size(); i++) {
            result.add(sampleData(probeData, (FieldImpl) data.get(i),
                                  atTime));
        }
        return result;
    }


    /**
     * Sample the field at the probe location
     *
     * @param field field to sample
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data sampleDataAtProbe(FieldImpl field)
            throws RemoteException, VisADException {
        return sampleData(getProbeLocation(), field);
    }


    /**
     * Sample at location
     *
     * @param loc location
     * @param field field to sample
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data sampleData(EarthLocationTuple[] loc, FieldImpl field)
            throws RemoteException, VisADException {
        return sampleData(loc, field, false);
    }


    /**
     * Get current animation time or null of none
     *
     * @return animation time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Real getAnimationTime() throws RemoteException, VisADException {
        Animation animation = getViewAnimation();
        if (animation != null) {
            return animation.getAniValue();
        }
        return null;
    }


    /**
     * Get all animation times or null if none
     *
     * @return animation time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public List getAnimationTimes() throws RemoteException, VisADException {
        Animation animation = getViewAnimation();
        if (animation != null) {
            Set set = animation.getSet();
            return Util.toList(set);
        }
        return null;
    }

    /**
     * Get times from data
     *
     * @return animation time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public List getTimesFromData() throws RemoteException, VisADException {
        List data  = getDataList();
        List times = new ArrayList();
        for (int dataIdx = 0; dataIdx < data.size(); dataIdx++) {
            FieldImpl field   = (FieldImpl) data.get(dataIdx);
            Set       timeSet = GridUtil.getTimeSet(field);
            if (timeSet != null) {
                List dataTimes = Util.toList(timeSet);
                for (int timeIdx = 0; timeIdx < dataTimes.size(); timeIdx++) {
                    Object t = dataTimes.get(timeIdx);
                    if ( !times.contains(t)) {
                        times.add(t);
                    }
                }
            }
        }
        return times;
    }

    /**
     * Sample the field at the location and maybe at the animation time
     *
     * @param loc location
     * @param field field
     * @param atTime if true then also sample at anim time
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data sampleData(EarthLocationTuple[] loc, FieldImpl field,
                           boolean atTime)
            throws RemoteException, VisADException {
        Field sample = doSample(loc, field);
        if (sample == null) {
            return null;
        }
        if (atTime) {
            Real aniValue = getAnimationTime();
            if ((aniValue != null) && !aniValue.isMissing()) {
                Set  timeSet = sample.getDomainSet();
                Unit setUnit = timeSet.getSetUnits()[0];
                if (Unit.canConvert(aniValue.getUnit(), setUnit)) {
                    double timeValue =
                        aniValue.getValue(timeSet.getSetUnits()[0]);
                    int[] index = timeSet.doubleToIndex(new double[][] {
                        { timeValue }
                    });
                    return sample.getSample(index[0]);
                }
            }
        }
        return sample;
    }




    /**
     * sample
     *
     * @param loc location to sample
     * @param field field to sample
     *
     * @return sampled data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private Field doSample(EarthLocationTuple[] loc, FieldImpl field)
            throws RemoteException, VisADException {
        if ((loc == null) || (field == null)) {
            return null;
        }

        int samplingMode = getDefaultSamplingModeValue();
        if (probeType.equals(PROBE_POINT)) {
            if (GridUtil.is3D(field)) {
                return GridUtil.sample(field, loc[0], samplingMode);
            } else {
                return GridUtil.sample(field, loc[0].getLatLonPoint(),
                                       samplingMode);
            }
        } else if (probeType.equals(PROBE_LEVEL)) {
            return GridUtil.sliceAtLevel(field, loc[0].getAltitude(),
                                         samplingMode);
        } else if (probeType.equals(PROBE_AREA)) {
            throw new IllegalArgumentException(
                "Sampling on area probe is not supported");
        } else if (probeType.equals(PROBE_TRANSECT)) {
            return GridUtil.sliceAlongLatLonLine(field,
                    loc[0].getLatLonPoint(), loc[1].getLatLonPoint(),
                    samplingMode);


        } else if (probeType.equals(PROBE_VERTICAL)) {
            return GridUtil.getProfileAtLatLonPoint(field,
                    loc[0].getLatLonPoint(), samplingMode);
        }

        return null;

    }



    /**
     * Return the appropriate label text for the menu.
     * @return  the label text
     */
    protected String getChangeParameterLabel() {
        return "Add Parameter...";
    }




    /**
     * Gets called whne user has chosen new data
     *
     * @param newChoices new data choices
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void addNewData(List newChoices)
            throws VisADException, RemoteException {
        dataList = null;
        if (replaceNewData) {
            setDataChoices(newChoices);
        } else {
            appendDataChoices(newChoices);
        }
        if (newDataCallBack == null) {
            execJython("handleData");
        } else {
            execJython(newDataCallBack);
        }
    }




    /**
     * Hook to allow jython to call to bring up data choice selector
     *
     * @param message message to use in dialog
     */
    public void selectData(String message) {
        addData(message);
    }


    /**
     * Hook to allow jython to call to bring up data choice selector
     *
     * @param message message to use in dialog
     */
    public void addData(String message) {
        selectData(message, null, false, false, null);
    }


    /**
     * Hook to allow jython to call to bring up data choice selector
     *
     * @param message message to use in dialog
     * @param callback The jython procedure to callback
     */
    public void addData(String message, String callback) {
        selectData(message, callback, false, false, null);
    }


    /**
     * Hook to allow jython to call to bring up data choice selector
     *
     * @param message message to use in dialog
     */
    public void replaceData(String message) {
        selectData(message, null, true, false, null);
    }

    /**
     * Hook to allow jython to call to bring up data choice selector
     *
     * @param message message to use in dialog
     * @param callback The jython procedure to callback
     */
    public void replaceData(String message, String callback) {
        selectData(message, callback, true, false, null);
    }

    /**
     * Hook to allow jython to call to bring up data choice selector
     *
     * @param message message to use in dialog
     * @param callback The jython procedure to callback
     * @param replace If true then we remove the current list of data choices and replace it with the selected ones.
     * @param multiples Select multiples
     * @param categories Possibly null list of data categories to use
     */
    public void selectData(String message, String callback, boolean replace,
                           boolean multiples, List categories) {
        replaceNewData  = replace;
        newDataCallBack = callback;
        if (categories != null) {
            popupDataDialog(message, null, multiples, categories);
        } else {
            popupDataDialog(message, null, multiples, getCategories());
        }
        newDataCallBack = null;
        replaceNewData  = false;
    }



    /**
     * Remove this control.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        if (interpreter != null) {
            getControlContext().getJythonManager().removeInterpreter(
                interpreter);
            interpreter = null;
        }
    }


    /**
     * Get the interpreter for this control.
     * @return  this control's interpreter
     *
     * @throws Exception On badness
     */
    private PythonInterpreter getMyInterpreter() throws Exception {
        if (interpreter == null) {
            interpreter =
                getControlContext().getJythonManager().createInterpreter();
        }
        String code = getJythonCode();
        if (code != null) {
            interpreter.exec(code);
        }
        return interpreter;
    }




    /**
     * Get the probe being used.
     * @return  current probe.
     */
    public Displayable getCurrentProbe() {
        if (pointProbe != null) {
            return pointProbe;
        }
        if (levelProbe != null) {
            return levelProbe;
        }
        if (verticalProbe != null) {
            return verticalProbe;
        }
        if (transectProbe != null) {
            return transectProbe;
        }
        if (areaProbe != null) {
            return areaProbe;
        }
        return null;
    }

    /**
     * Get the probe's position
     * @return   position of the probe
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public Object getProbePosition() throws VisADException, RemoteException {
        if (pointProbe != null) {
            return pointProbe.getPosition();
        }
        if (levelProbe != null) {
            return levelProbe.getPosition();
        }
        if (verticalProbe != null) {
            return verticalProbe.getPosition();
        }
        if (transectProbe != null) {
            return transectProbe.getPosition();
        }
        if (areaProbe != null) {
            return areaProbe.getArea();
        }
        return null;
    }


    /**
     * Set the probe position property.
     *
     * @param p   position for probe.
     */
    public void setProbePosition(Object p) {
        initPosition = p;
    }



    /**
     * Called when the probe is moved.
     *
     */
    private void probeMoved() {
        try {
            EarthLocationTuple[] probeLocation = getProbeLocation();
            if (probeLocation != null) {
                if (Misc.equals(lastProbeLocation, probeLocation)) {
                    return;
                }
                lastProbeLocation = probeLocation;
                execJython("handleProbe", probeLocation);
            }
        } catch (Exception exc) {
            logException("Moving  probe", exc);
        }
    }


    /**
     *  Return the current probe position in X/Y space.
     *
     *  @return a 2-D array of the form data[N][0] = X, data[N][1] = Y,
     *          data[N][2] = Z
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public double[][] getProbeXYZ() throws VisADException, RemoteException {
        Displayable currentProbe = getCurrentProbe();
        if (currentProbe == null) {
            return null;
        }

        if (pointProbe != null) {
            return getPointProbeLocation();
        }
        if (areaProbe != null) {
            return getAreaProbeLocation();
        }
        if (verticalProbe != null) {
            return getVerticalProbeLocation();
        }
        if (transectProbe != null) {
            return getTransectProbeLocation();
        }
        if (levelProbe != null) {
            return getLevelProbeLocation();
        }
        return null;
    }


    /**
     * Return an array that holds the location of the current probe.
     * For point, level and  vertical probes the length is 1
     * For the horizonal probe the length 2 (the end points of the probe line)
     * For the area probe the  length is 4 (upper-left, upper-right,
     *                                      lower-right, lower-left)
     *
     * @return An array holding the probe location
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public EarthLocationTuple[] getProbeLocation()
            throws VisADException, RemoteException {
        Displayable currentProbe = getCurrentProbe();
        if (currentProbe == null) {
            return null;
        }
        double[][] data = getProbeXYZ();
        if (data == null) {
            return null;
        }
        EarthLocationTuple[] elt = new EarthLocationTuple[data.length];
        for (int i = 0; i < data.length; i++) {
            elt[i] = getELT(data[i][0], data[i][1], data[i][2]);
        }
        return elt;
    }



    /**
     * A utility method to create a double array of length 3
     *
     * @param x    x value
     * @param y    y value
     *
     * @return a double array holding {x, y, 0.0}
     */
    private double[] getArray(double x, double y) {
        return getArray(x, y, 0.0);
    }

    /**
     * A utility method to create a double array of length 3
     *
     *
     * @param x    x value
     * @param y    y value
     * @param z    z value
     *
     * @return a double array holding {x, y, z}
     */
    private double[] getArray(double x, double y, double z) {
        return new double[] { x, y, z };
    }

    /**
     * A utility method to create a EarthLocationTuple
     *
     * @param x    x coordinate
     * @param y    y coordinate
     * @param z    z coordinate
     *
     * @return an EarthLocationTuple from the given x/y/z coordinates
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private EarthLocationTuple getELT(double x, double y, double z)
            throws VisADException, RemoteException {
        return (EarthLocationTuple) boxToEarth(new double[] { x, y, z });
    }



    /**
     * Get the location of the point probe.
     *
     * @return The location (lat,lon,alt) of the point probe
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double[][] getPointProbeLocation()
            throws VisADException, RemoteException {
        double[] values = pointProbe.getPosition().getValues();
        return new double[][] {
            getArray(values[0], values[1], values[2])
        };
    }

    /**
     * Get the location of the area probe.
     *
     * @return Array of size 2 of LatLonPoints that are the upper left
     *         and lower right points of the area.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double[][] getAreaProbeLocation()
            throws VisADException, RemoteException {
        double[] values = areaProbe.getArea().getValues();
        double   left   = Math.min(values[0], values[2]);
        double   right  = Math.max(values[0], values[2]);
        double   top    = Math.max(values[1], values[3]);
        double   bottom = Math.min(values[1], values[3]);
        return new double[][] {
            getArray(left, top), getArray(right, top),
            getArray(right, bottom), getArray(left, bottom)
        };
    }


    /**
     * Get the location of the vertical probe.
     *
     * @return The Lat/Lon  position of the vertical  probe
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double[][] getVerticalProbeLocation()
            throws VisADException, RemoteException {
        double[]      data;
        RealTuple     position = verticalProbe.getPosition();
        RealTupleType rttype   = (RealTupleType) position.getType();
        if (rttype.equals(RealTupleType.SpatialCartesian2DTuple)) {
            // get earth location of the x,y position in the VisAD display
            double[] values = position.getValues();
            data = getArray(values[0], values[1]);
        } else if (rttype.equals(RealTupleType.SpatialEarth2DTuple)) {
            Real[] reals = position.getRealComponents();
            data = getArray(reals[1].getValue(), reals[0].getValue());
        } else if (rttype.equals(RealTupleType.LatitudeLongitudeTuple)) {
            Real[] reals = position.getRealComponents();
            data = getArray(reals[0].getValue(), reals[1].getValue());
        } else {
            throw new VisADException(
                "Can't convert position to navigable point");
        }
        if (data != null) {
            return new double[][] {
                data
            };
        }
        return null;
    }

    /**
     *  Get the location of the Transect probe.
     *
     *  @return An array of length 2 that holds the start point and
     *          end point (as lat/lon)
     *  of the probe.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double[][] getTransectProbeLocation()
            throws VisADException, RemoteException {
        RealTuple start = transectProbe.getStartPoint();
        RealTuple end   = transectProbe.getEndPoint();
        double[]  v1    = start.getValues();
        double[]  v2    = end.getValues();
        return new double[][] {
            getArray(v1[0], v1[1]), getArray(v2[0], v2[1])
        };
    }


    /**
     * Get the location of the level  probe.
     *
     * @return The altitude of the level probe.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private double[][] getLevelProbeLocation()
            throws VisADException, RemoteException {
        return new double[][] {
            getArray(0.0, 0.0, levelProbe.getPosition().getValue())
        };
    }


    /**
     * Method called by other classes that share the probe.
     *
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {

        /*
         *        if (dataId.equals (SHARE_LEVEL)) {
         *          try {
         *          loadDataAtLevel ((Real)data[0]);
         *          } catch (Exception exc) {
         *          LogUtil.printException (log_, "receiveShareData.level", exc);
         *          }
         *          return;
         *          }
         */
        super.receiveShareData(from, dataId, data);
    }


    /**
     * Get the probe name.
     *
     * @param probe   probe name to find.
     * @return  probe name
     */
    private String getProbeName(String probe) {
        for (int i = 0; i < PROBES.length; i++) {
            if (PROBES[i].equals(probe)) {
                return PROBE_NAMES[i];
            }
        }
        return "No probe";
    }

    /**
     * Make some Plan view controls for the UI.
     * @return create the contents for the UI.
     */
    public Container doMakeContents() {
        jythonContainer = new JPanel(new BorderLayout());
        if (developerMode) {
            return doMakeDeveloperContents();
        } else {
            return jythonContainer;
        }
    }

    /**
     * Make the developer UI contents
     * @return  UI for developer code
     */
    private JComponent doMakeDeveloperContents() {

        labelFld = new JTextField((myName != null)
                                  ? myName
                                  : "", 15);
        labelFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setMyName(labelFld.getText().trim());
            }
        });



        Vector probeItems = new Vector();
        for (int i = 0; i < PROBES.length; i++) {
            probeItems.add(new TwoFacedObject(PROBE_NAMES[i], PROBES[i]));
        }
        final JComboBox probeCbx = new JComboBox(probeItems);
        probeCbx.setSelectedItem(new TwoFacedObject(getProbeName(probeType),
                probeType));
        probeCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    probeType =
                        ((TwoFacedObject) probeCbx.getSelectedItem()).getId()
                            .toString();
                    doMakeProbe();
                } catch (Exception exc) {
                    logException("Changing probe type", exc);
                }
            }
        });

        displayCategoryFld = new JTextField(jythonDisplayCategory, 15);

        categoriesFld      = new JTextArea("", 4, 20);
        if (dataCategories != null) {
            categoriesFld.setText(dataCategories);
        }





        Component[] comps = {
            GuiUtils.rLabel("Display name: "), GuiUtils.left(labelFld),
            GuiUtils.rLabel("Display Category:"),
            GuiUtils.left(displayCategoryFld),
            GuiUtils.top(GuiUtils.rLabel("Data Categories:")),
            GuiUtils.left(new JScrollPane(categoriesFld)),
            GuiUtils.rLabel("Probe Type:"),
            GuiUtils.left(GuiUtils.hflow(Misc.newList(probeCbx,
                GuiUtils.rLabel("  Probe Color: "),
                doMakeColorControl(getColor()))))
        };

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JPanel fldPanel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NYN,
                                            GuiUtils.WT_N);

        try {
            if (jythonCode == null) {
                jythonCode = IOUtil.readContents(
                    "/ucar/unidata/idv/control/jythoncontrol.py", "");
            }

            jythonEditor = new JPythonEditor();
            jythonEditor.setText(jythonCode);
            jythonEditor.setPreferredSize(new Dimension(400, 300));
        } catch (VisADException exc) {
            logException("Creating Jython editor", exc);
        }
        JComponent  jythonPanel = new JScrollPane(jythonEditor);
        JTabbedPane tab         = GuiUtils.getNestedTabbedPane();
        tab.add("Settings", GuiUtils.top(fldPanel));
        List buttons = Misc.newList(
                           GuiUtils.makeButton(
                               "Evaluate Init", this, "execJython",
                               "handleInit"), GuiUtils.makeButton(
                                   "Evaluate Data", this, "execJython",
                                   "handleData"));

        JPanel buttonPanel = GuiUtils.left(GuiUtils.hbox(buttons));
        tab.add("Jython", GuiUtils.topCenter(buttonPanel, jythonPanel));
        tab.add("Result GUI", jythonContainer);
        return tab;
    }


    /**
     * Return the extra legend component
     *
     * @param legendType side or bottom
     *
     * @return component to put in legend
     */
    protected JComponent getExtraLegendComponent(int legendType) {
        JComponent parentComp = super.getExtraLegendComponent(legendType);
        if (legendType == BOTTOM_LEGEND) {
            return parentComp;
        }
        if (sideLegendHolder == null) {
            sideLegendHolder = new JPanel(new BorderLayout());
        }
        return sideLegendHolder;
    }


    /**
     * Add a python component
     *
     * @param comp   component to add
     */
    public void addJythonComponent(Component comp) {
        setJythonComponent(comp);
    }


    /**
     * Hook to call from jython to add in the legend component
     *
     * @param comp legend component
     */
    public void setLegendComponent(Component comp) {
        if (sideLegendHolder == null) {
            sideLegendHolder = new JPanel(new BorderLayout());
        }
        sideLegendHolder.removeAll();
        if (comp != null) {
            sideLegendHolder.add(BorderLayout.CENTER, comp);
        }

    }


    /**
     * Hook to call from jython to define gui
     *
     * @param comp gui
     */
    public void setJythonComponent(Component comp) {
        jythonContainer.removeAll();
        if (comp != null) {
            jythonContainer.add(BorderLayout.CENTER, comp);
        }
        redoGuiLayout();
    }

    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        if (developerMode) {
            items.add(GuiUtils.makeMenuItem("Write to Plugin", this,
                                            "writeToPlugin"));
        }
        items.addAll(saveMenuItems);
        super.getSaveMenuItems(items, forMenuBar);
    }

    /**
     * add to menu
     *
     * @param items list of menu items to add to
     * @param forMenuBar for menu bar
     */
    protected void getFileMenuItems(List items, boolean forMenuBar) {
        items.addAll(fileMenuItems);
        super.getFileMenuItems(items, forMenuBar);
    }

    /**
     * add to menu
     *
     * @param items list of menu items to add to
     * @param forMenuBar for menu bar
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        items.addAll(editMenuItems);
        super.getEditMenuItems(items, forMenuBar);
    }

    /**
     * add to menu
     *
     * @param items list of menu items to add to
     * @param forMenuBar for menu bar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        items.addAll(viewMenuItems);
        super.getViewMenuItems(items, forMenuBar);
    }


    /**
     * add the menu item to the list
     *
     * @param l list of items
     * @param name menu item name
     * @param method jython method to call
     */
    private void addMenuItem(List l, String name, String method) {
        l.add(GuiUtils.makeMenuItem(name, this, "execJython", method));
    }

    /**
     * Hook to call from jython to add to menu
     *
     * @param name menu item name
     * @param method jython method name to call
     */
    public void addFileMenuItem(String name, String method) {
        addMenuItem(fileMenuItems, name, method);
    }

    /**
     * Hook to call from jython to add to menu
     *
     * @param name menu item name
     * @param method jython method name to call
     */
    public void addSaveMenuItem(String name, String method) {
        addMenuItem(saveMenuItems, name, method);
    }

    /**
     * Hook to call from jython to add to menu
     *
     * @param name menu item name
     * @param method jython method name to call
     */
    public void addViewMenuItem(String name, String method) {
        addMenuItem(viewMenuItems, name, method);
    }

    /**
     * Hook to call from jython to add to menu
     *
     * @param name menu item name
     * @param method jython method name to call
     */
    public void addEditMenuItem(String name, String method) {
        addMenuItem(editMenuItems, name, method);
    }



    /**
     * Set the ProbeType property.
     *
     * @param value The new value for ProbeType
     */
    public void setProbeType(String value) {
        probeType = value;
    }

    /**
     * Get the ProbeType property.
     *
     * @return The ProbeType
     */
    public String getProbeType() {
        return probeType;
    }

    /**
     * Set the DeveloperMode property.
     *
     * @param value The new value for DeveloperMode
     */
    public void setDeveloperMode(boolean value) {
        developerMode = value;
    }

    /**
     * Get the DeveloperMode property.
     *
     * @return The DeveloperMode
     */
    public boolean getDeveloperMode() {
        return developerMode;
    }


    /**
     * Set the MyName property.
     *
     * @param value The new value for MyName
     */
    public void setMyName(String value) {
        myName = value;
    }

    /**
     * Get the MyName property.
     *
     * @return The MyName
     */
    public String getMyName() {
        if (labelFld != null) {
            return labelFld.getText().trim();
        }
        return myName;
    }

    /**
     * Set the CategoryString property.
     *
     * @param value The new value for CategoryString
     */
    public void setDataCategories(String value) {
        dataCategories = value;
    }

    /**
     * Get the CategoryString property.
     *
     * @return The CategoryString
     */
    public String getDataCategories() {
        if (categoriesFld != null) {
            return categoriesFld.getText();
        }
        return dataCategories;
    }


    /**
     * Set the DisplayCategory property.
     *
     * @param value The new value for DisplayCategory
     */
    public void setJythonDisplayCategory(String value) {
        jythonDisplayCategory = value;
    }

    /**
     * Get the DisplayCategory property.
     *
     * @return The DisplayCategory
     */
    public String getJythonDisplayCategory() {
        if (displayCategoryFld != null) {
            return displayCategoryFld.getText();
        }
        return jythonDisplayCategory;

    }

    /**
     * Set the JythonCode property.
     *
     * @param value The new value for JythonCode
     */
    public void setJythonCode(String value) {
        jythonCode = value;
    }

    /**
     * Get the JythonCode property.
     *
     * @return The JythonCode
     */
    public String getJythonCode() {
        if (developerMode) {
            if (jythonEditor != null) {
                return jythonEditor.getText();
            }
            return null;
        }
        return jythonCode;
    }



}

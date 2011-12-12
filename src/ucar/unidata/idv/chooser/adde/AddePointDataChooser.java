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

package ucar.unidata.idv.chooser.adde;


import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddePointDataReader;

import org.w3c.dom.Element;

import ucar.unidata.data.AddeUtil;
import ucar.unidata.data.point.AddePointDataSource;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.ui.ChooserList;
import ucar.unidata.ui.ChooserPanel;

import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.UtcDate;

import visad.DateTime;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Selection widget for ADDE point data
 *
 * @author MetApps Development Team
 * @version $Revision: 1.6 $ $Date: 2007/08/06 17:03:31 $
 */
public class AddePointDataChooser extends AddeChooser {

    /**
     * Property for the dataset name key.
     * @see #getDatasetName()
     */
    public static String DATASET_NAME_KEY = "name";

    /** Property for the data type. */
    public static String DATA_TYPE = "ADDE.POINT";

    /** UI widget for selecting data types */
    protected JComboBox dataTypes;

    /** UI widget for selecting station models */
    protected JComboBox stationModelBox;

    /** a selector for a particular level */
    protected JComboBox levelBox = null;

    /** label for METAR data */
    private static final String METAR = "Surface (METAR) Data";

    /** label for synoptic data */
    private static final String SYNOPTIC = "Synoptic Data";

    /** label for synoptic data */
    private static final String SHIPBUOY = "Ship/Buoy Data";

    /** station model manager */
    private StationModelManager stationModelManager;

    /** Property for the number of times */
    public static String LEVELS = "data levels";

    /** Property for the time increment */
    public static String SELECTED_LEVEL = "selected level";

    /** box for the relative time */
    private JComboBox relTimeIncBox;

    /** box for the relative time */
    private JComponent relTimeIncComp;

    /** the relative time increment */
    private float relativeTimeIncrement = 1.f;



    /**
     * Create a chooser for Adde POINT data
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddePointDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        init(getIdv().getStationModelManager());
    }


    /**
     * init
     *
     * @param stationModelManager station model manager
     */
    private void init(StationModelManager stationModelManager) {
        this.stationModelManager = stationModelManager;
        Vector stationModels =
            new Vector(stationModelManager.getStationModels());
        stationModelBox = new JComboBox(stationModels);
        //Try to default to 
        for (int i = 0; i < stationModels.size(); i++) {
            if (stationModels.get(i).toString().equalsIgnoreCase(
                    getDefaultStationModel())) {
                stationModelBox.setSelectedItem(stationModels.get(i));
                break;
            }
        }


        dataTypes =
            GuiUtils.getEditableBox(Misc.toList(getDefaultDatasets()), null);

        dataTypes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                setState(STATE_UNCONNECTED);
                String currentType = dataTypes.getSelectedItem().toString();
                if (currentType.equals(SYNOPTIC)) {
                    setRelativeTimeIncrement(3);
                } else {
                    setRelativeTimeIncrement(1);
                }
            }
        });
        if (canDoLevels()) {
            levelBox = GuiUtils.getEditableBox(getLevels(), null);
        }

    }




    /**
     * Make the contents for this chooser
     *
     * @return  a panel with the UI
     */
    protected JComponent doMakeContents() {

        List allComps = new ArrayList();
        clearOnChange(dataTypes);
        addTopComponents(allComps, LABEL_DATATYPE, dataTypes);
        JPanel timesComp = makeTimesPanel();
        allComps.add(addServerComp(GuiUtils.valignLabel(LABEL_TIMES)));
        allComps.add(addServerComp(timesComp));
        if (canDoLevels()) {
            allComps.add(addServerComp(GuiUtils.rLabel("Level:")));
            allComps.add(addServerComp(GuiUtils.left(levelBox)));
        }
        //        allComps.add(addServerComp(GuiUtils.rLabel("Layout Model:")));
        //        allComps.add(addServerComp(GuiUtils.left(lastPanel)));
        JComponent top = GuiUtils.formLayout(allComps, GRID_INSETS);
        return GuiUtils.top(GuiUtils.centerBottom(top, getDefaultButtons()));
    }

    /**
     * Get the default display type
     *
     * @return the default control for automatic display
     */
    protected String getDefaultDisplayType() {
        return "stationmodelcontrol";
    }


    /**
     * Load in an ADDE point data set based on the
     * <code>PropertyChangeEvent<code>.
     *
     */
    public void doLoadInThread() {
        showWaitCursor();
        try {
            StationModel selectedStationModel = getSelectedStationModel();
            String       source               = getRequestUrl();
            // make properties Hashtable to hand the station name
            // to the AddeProfilerDataSource
            Hashtable ht = new Hashtable();
            getDataSourceProperties(ht);
            ht.put(AddePointDataSource.PROP_STATIONMODELNAME,
                   selectedStationModel.getName());
            ht.put(DATASET_NAME_KEY, getDatasetName());
            ht.put(DATA_NAME_KEY, getDataName());
            if (source.indexOf(AddeUtil.RELATIVE_TIME) >= 0) {
                ht.put(AddeUtil.NUM_RELATIVE_TIMES, getRelativeTimeIndices());
                ht.put(AddeUtil.RELATIVE_TIME_INCREMENT,
                       new Float(getRelativeTimeIncrement()));
            }
            if (source.indexOf(AddeUtil.LEVEL) >= 0) {
                ht.put(LEVELS, getLevels());
                ht.put(SELECTED_LEVEL, getSelectedLevel());
            }


            makeDataSource(source, DATA_TYPE, ht);
            saveServerState();
        } catch (Exception excp) {
            logException("Unable to open ADDE point dataset", excp);
        }
        showNormalCursor();
    }





    /**
     * Add the 00 & 12Z checkbox to the component.
     * @return superclass component with extra stuff
     */
    protected JPanel makeTimesPanel() {
        return super.makeTimesPanel(true);
    }

    /**
     * Get the extra time widget.  Subclasses can add their own time
     * widgets.
     *
     * @return a widget that can be selected for more options
     */
    protected JComponent getExtraRelativeTimeComponent() {
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JComboBox box = (JComboBox) ae.getSource();
                if (GuiUtils.anySelected(box)) {
                    setRelativeTimeIncrement(getRelBoxValue());
                }
            }
        };
        String[] nums = new String[] {
            ".5", "1", "3", "6", "12", "24"
        };
        float[]  vals = new float[] {
            .5f, 1f, 3f, 6f, 12f, 24f
        };
        List     l    = new ArrayList();
        for (int i = 0; i < nums.length; i++) {
            l.add(new TwoFacedObject(nums[i], new Float(vals[i])));
        }
        relTimeIncBox = GuiUtils.getEditableBox(l,
                new Float(relativeTimeIncrement));
        relTimeIncBox.addActionListener(listener);
        relTimeIncBox.setToolTipText(
            "Set the increment between most recent times");
        //        Dimension prefSize = relTimeIncBox.getPreferredSize();
        //        if(prefSize!=null) {
        //            relTimeIncBox.setPreferredSize(new Dimension(20,prefSize.height));
        //        }
        relTimeIncComp = GuiUtils.hbox(new JLabel("Increment: "),
                                       relTimeIncBox,
                                       GuiUtils.lLabel(" hours"));
        return GuiUtils.left(relTimeIncComp);
    }

    /**
     * Get the value from the relative increment box
     *
     * @return the seleted value or a default
     */
    private float getRelBoxValue() {
        float value = relativeTimeIncrement;
        if (relTimeIncBox != null) {
            Object o = relTimeIncBox.getSelectedItem();
            if (o != null) {
                String val = TwoFacedObject.getIdString(o);
                value = (float) Misc.parseNumber(val);
            }
        }
        return value;
    }

    /**
     * Get the selected station model.
     *
     * @return StationModel to use by default.
     */
    public StationModel getSelectedStationModel() {
        return (StationModel) stationModelBox.getSelectedItem();
    }


    /**
     * Return the currently selected descriptor form the combobox
     *
     * @return the currently selected descriptor
     */
    protected String getDescriptor() {
        String dataset =
            TwoFacedObject.getIdString(dataTypes.getSelectedItem());
        int index = dataset.indexOf('/');
        if (index == -1) {
            throw new IllegalArgumentException("Bad dataset: \"" + dataset
                    + "\"");
        }
        return dataset.substring(index + 1);
    }

    /**
     * Return the currently selected group form the combobox
     *
     * @return the currently selected group
     */
    protected String getGroup() {
        String dataset =
            TwoFacedObject.getIdString(dataTypes.getSelectedItem());
        int index = dataset.indexOf('/');
        if (index == -1) {
            throw new IllegalArgumentException("Bad dataset: \"" + dataset
                    + "\"");
        }
        return dataset.substring(0, index);
    }



    /**
     * Get the request URL
     *
     * @return  the request URL
     */
    public String getRequestUrl() {
        StringBuffer request = getGroupUrl(REQ_POINTDATA, getGroup());
        appendKeyValue(request, PROP_DESCR, getDescriptor());
        appendRequestSelectClause(request);
        appendKeyValue(request, PROP_NUM, "all");
        //appendKeyValue(request, PROP_DEBUG, "true");
        appendKeyValue(request, PROP_POS, getDoRelativeTimes()
                                          ? "ALL"
                                          : "0");
        return request.toString();
    }


    /**
     * Get the list of possible levels for this chooser.
     * @return list of levels;
     */
    public List getLevels() {
        return new ArrayList();
    }

    /**
     * Get the selected level
     * @return the selected level
     */
    public Object getSelectedLevel() {
        if (levelBox != null) {
            return levelBox.getSelectedItem();
        } else {
            return null;
        }
    }


    /**
     * Get the select clause for the adde request specific to this
     * type of data.
     *
     * @param buf The buffer to append to
     */
    protected void appendRequestSelectClause(StringBuffer buf) {
        StringBuffer selectValue = new StringBuffer();
        selectValue.append("'");
        selectValue.append(getDayTimeSelectString());
        if (getDescriptor().equalsIgnoreCase("SFCHOURLY")) {
            selectValue.append(";type 0");
        }
        selectValue.append(";");
        if (canDoLevels()) {
            selectValue.append(AddeUtil.LEVEL);
            selectValue.append(";");
        }
        selectValue.append(AddeUtil.LATLON_BOX);
        selectValue.append("'");
        appendKeyValue(buf, PROP_SELECT, selectValue.toString());
    }

    /**
     * Does this chooser support level selection
     *
     * @return true if levels are supported by this chooser
     */
    public boolean canDoLevels() {
        return false;
    }


    /**
     * Update the widget with the latest data.
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {
        readTimes();
        saveServerState();
    }


    /**
     * Get the request string for times particular to this chooser
     *
     * @return request string
     */
    protected String getTimesRequest() {
        StringBuffer buf = getGroupUrl(REQ_POINTDATA, getGroup());
        appendKeyValue(buf, PROP_DESCR, getDescriptor());
        // this is hokey, but take a smattering of stations.  
        //appendKeyValue(buf, PROP_SELECT, "'LAT 39.5 40.5;LON 104.5 105.5'");
        // include buoys
        appendKeyValue(buf, PROP_SELECT, "'LAT 38 42;LON 70 75'");
        appendKeyValue(buf, PROP_POS, "0");  // set to 0 for now
        if (getDoAbsoluteTimes()) {
            appendKeyValue(buf, PROP_NUM, "all");
        }
        appendKeyValue(buf, PROP_PARAM, "day time");
        return buf.toString();
    }


    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "Point Data";
    }


    /**
     * Set the list of available times.
     */
    public void readTimes() {
        clearTimesList();
        SortedSet uniqueTimes =
            Collections.synchronizedSortedSet(new TreeSet());
        setState(STATE_CONNECTING);
        try {
            //            System.err.println("TIMES:" + getTimesRequest());
            AddePointDataReader apr =
                new AddePointDataReader(getTimesRequest());
            int[][]  data  = apr.getData();
            String[] units = apr.getUnits();
            if ( !units[0].equals("CYD") || !units[1].equals("HMS")) {
                throw new Exception("can't handle date/time units");
            }
            int numObs = data[0].length;
            //System.out.println("found " + numObs + " obs");
            // loop through and find all the unique times
            for (int i = 0; i < numObs; i++) {
                try {
                    DateTime dt =
                        new DateTime(McIDASUtil.mcDayTimeToSecs(data[0][i],
                            data[1][i]));
                    uniqueTimes.add(dt);
                } catch (Exception e) {}
            }
            setState(STATE_CONNECTED);
            //System.out.println(
            //       "found " + uniqueTimes.size() + " unique times");
        } catch (Exception excp) {
            handleConnectionError(excp);
            return;
        }
        if (getDoAbsoluteTimes()) {
            if ( !uniqueTimes.isEmpty()) {
                setAbsoluteTimes(new ArrayList(uniqueTimes));
            }
            int selectedIndex = getAbsoluteTimes().size() - 1;
            setSelectedAbsoluteTime(selectedIndex);
        }
    }

    /**
     * Get the default number of times to select
     *
     * @return 1
     */
    protected int getNumTimesToSelect() {
        return 1;
    }

    /**
     * Are there any times selected.
     *
     * @return Any times selected.
     */
    protected boolean haveTimeSelected() {
        return !getDoAbsoluteTimes() || getHaveAbsoluteTimesSelected();
    }

    /**
     * Create the date time selection string for the "select" clause
     * of the ADDE URL.
     *
     * @return the select day and time strings
     */
    protected String getDayTimeSelectString() {
        StringBuffer buf = new StringBuffer();
        if (getDoAbsoluteTimes()) {
            buf.append("time ");
            List times = getSelectedAbsoluteTimes();
            for (int i = 0; i < times.size(); i++) {
                DateTime dt = (DateTime) times.get(i);
                buf.append(UtcDate.getHMS(dt));
                if (i != times.size() - 1) {
                    buf.append(",");
                }
            }
        } else {
            buf.append(getRelativeTimeId());
        }
        return buf.toString();
    }

    /**
     * Get the identifier for relative time.  Subclasses can override.
     * @return the identifier
     */
    protected String getRelativeTimeId() {
        return AddeUtil.RELATIVE_TIME;
    }

    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        return dataTypes.getSelectedItem().toString();
    }

    /**
     * Get the data type for this chooser
     *
     * @return  the type
     */
    public String getDataType() {
        return "POINT";
    }


    /**
     * Get the increment between times for relative time requests
     *
     * @return time increment (hours)
     */
    public float getRelativeTimeIncrement() {
        return relativeTimeIncrement;
    }

    /**
     * Set the increment between times for relative time requests
     *
     * @param increment time increment (hours)
     */
    public void setRelativeTimeIncrement(float increment) {
        relativeTimeIncrement = increment;
        if (relTimeIncBox != null) {
            relTimeIncBox.setSelectedItem(new Float(relativeTimeIncrement));
        }
    }

    /**
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        enableWidgets();
    }

    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        super.enableWidgets();
        if (relTimeIncComp != null) {
            GuiUtils.enableTree(relTimeIncComp, getDoRelativeTimes());
        }

    }

    /**
     * Get an array of {@link TwoFacedObject}-s for the datasets.  The
     * two faces are the descriptive name and the actual group/descriptor
     *
     * @return   the default data sets
     */
    protected TwoFacedObject[] getDefaultDatasets() {
        return new TwoFacedObject[] {
            new TwoFacedObject(METAR, "RTPTSRC/SFCHOURLY"),
            new TwoFacedObject(SYNOPTIC, "RTPTSRC/SYNOPTIC"),
            new TwoFacedObject(SHIPBUOY, "RTPTSRC/SHIPBUOY") };
    }


    /**
     * Get the default station model for this chooser.
     * @return name of default station model
     */
    public String getDefaultStationModel() {
        return "observations>metar";
    }

    /**
     * Show the given error to the user. If it was an Adde exception
     * that was a bad server error then print out a nice message.
     *
     * @param excp The exception
     */
    protected void handleConnectionError(Exception excp) {
        String message = excp.getMessage().toLowerCase();
        if (message.indexOf("with position 0") >= 0) {
            LogUtil.userErrorMessage("Unable to handle archive dataset");
            return;
        }
        super.handleConnectionError(excp);
    }






}

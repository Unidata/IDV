/*
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.TimeHeightViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;

import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.*;

import ucar.visad.ShapeUtility;


import ucar.visad.Util;
import ucar.visad.display.ColorScale;
import ucar.visad.display.Contour2DDisplayable;
import ucar.visad.display.Displayable;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.GridDisplayable;
import ucar.visad.display.XYDisplay;

import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.Length;
import visad.*;

import visad.Set;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.lang.reflect.Method;
import java.rmi.RemoteException;

import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import static javax.swing.JOptionPane.getRootFrame;


/**
 * Given an earth-locared 3D VisAD Field with a time domain,
 * make a 2D plot of the range data against height and
 * time for one location on the Earth, and make related controls.
 * The display is in its own window; there is also a related selector point on
 * the other main map display which allows user to select and move the
 * sample location on Earth.
 *
 * @author IDV Development Team
 * @version $Revision: 1.72 $Date: 2006/12/01 20:16:38 $
 */
public class TimeHeightControl extends LineProbeControl {

    /** property for sharing the profile location */
    public static final String SHARE_PROFILE =
        "TimeHeightControl.SHARE_PROFILE";


    /** XY display for displaying time/height diagram */
    protected XYDisplay profileDisplay;

    /** the data */
    protected FieldImpl fieldImpl;

    /** Displayable for color filled contours */
    private DisplayableData dataDisplay;

    /** Displayable for contours */
    protected Contour2DDisplayable contourDisplay;

    /** flag for XAxis time orientation */
    private boolean isLatestOnLeft = true;

    /** flag for type of color display */
    private boolean showAsContours = false;

    /** flag for type of color display */
    private boolean colorFill = false;

    /** foreground color */
    protected Color foreground;

    /** background color */
    protected Color background;

    /** The latlon widget */
    private LatLonWidget latLonWidget;

    /** the control window's view manager */
    protected TimeHeightViewManager timeHeightView;

    /** the control for second variable */
    protected JPanel controlPane;

    /** the old smoothing type for first variable */
    private String OldSmoothingType = LABEL_NONE;
    /** the old smoothing factor for first variable */
    private int OldSmoothingFactor = 0;

    /** the control for second variable */
    private MyTimeHeightControl myTimeHeightControl;
    /** the color for second variable */
    private ColorTable myColorTable;
    /** the contour info for second variable */
    private ContourInfo myContourInfo;
    /** the smoothing type for second variable */
    private String mySmoothingType;
    /** the smoothing factor for second variable */
    private int mySmoothingFactor;
    /**
     *  Default Contructor; sets flags. See init() for creation actions.
     */
    public TimeHeightControl() {
        setAttributeFlags(FLAG_DATACONTROL | FLAG_COLOR );
    }

    /**
     * Construct the display, frame, and controls
     *
     * @param dataChoice the data to use
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        timeHeightView = new TimeHeightViewManager(getViewContext(),
                new ViewDescriptor("timeheight_of_" + paramName),
                "showControlLegend=false;wireframe=true;") {}
        ;
        profileDisplay = timeHeightView.getTimeHeightDisplay();
        profileDisplay.setAspect(1.0, .6);

        //If foreground is not null  then this implies we have been unpersisted
        if (foreground != null) {
            timeHeightView.setColors(foreground, background);
        }
        //Call doMakeProbe here so we link to the right ViewManager
        //If we do it after the addViewManager then we screw up persistence
        doMakeProbe();

        addViewManager(timeHeightView);

        if (showAsContours) {
            contourDisplay = new Contour2DDisplayable("th_color_" + paramName,
                    true, colorFill);
            contourDisplay.setVisible(true);

            addDisplayable(contourDisplay, timeHeightView,
                    FLAG_COLORTABLE | FLAG_CONTOUR | FLAG_DISPLAYUNIT | FLAG_SMOOTHING);

        } else if (this instanceof FlowTimeHeightControl){
            dataDisplay = createDataDisplay();
            addDisplayable(dataDisplay, timeHeightView,
                    getDataDisplayFlags());
        } else {
            dataDisplay = new Grid2DDisplayable("th_color_" + paramName,
                    true);
            dataDisplay.setVisible(true);
            ((Grid2DDisplayable) dataDisplay).setTextureEnable(false);
            addDisplayable(dataDisplay, timeHeightView,
                    FLAG_COLORTABLE | FLAG_CONTOUR | FLAG_DISPLAYUNIT);
        }
        controlPane = new JPanel();


        if ( !setData(dataChoice)) {
            return false;
        }

        fieldImpl = getGridDataInstance().getGrid();

        boolean isSequence = GridUtil.isTimeSequence(fieldImpl);

        // Check to see if this is a time sequence or not.  No sense
        // creating a timeHeight CrossSection with no times or one time.
        if ( !isSequence || (fieldImpl.getDomainSet().getLength() <= 1)) {
            throw new VisADException(
                    "Need more than one time to create a TimeHeight Cross Section");
        }

        RealTuple ss = getGridCenterPosition();
        setProbePosition(ss);
        setXAxisLabels((SampledSet) fieldImpl.getDomainSet());

        timeHeightView.setShowDisplayList(false);
        return true;
    }

    /**
     * Get the default display list template for this control.  Subclasses can override
     * @return the default template
     */
    protected String getDefaultDisplayListTemplate() {
        return MACRO_SHORTNAME + " - " + MACRO_POSITION + " "
               + MACRO_TIMESTAMP;
    }

    /**
     * Get the attribute flags for the data display
     * @return the flags
     */
    protected int getDataDisplayFlags() {
        return FLAG_COLORTABLE | FLAG_DISPLAYUNIT;
    }

    /**
     * Create the default data display if not showAsContours
     *
     * @return the default display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected DisplayableData createDataDisplay()
            throws VisADException, RemoteException {
        DisplayableData dataDisplay = new Grid2DDisplayable("th_color_"
                                          + paramName, true);
        ((Grid2DDisplayable) dataDisplay).setTextureEnable(true);
        dataDisplay.setVisible(true);
        return dataDisplay;
    }

    /**
     * Return the <code>Displayable</code> created by createDataDisplay.
     *
     * @return <code>DisplayableData</code>
     */
    public DisplayableData getDataDisplay() {
        return dataDisplay;
    }

    /**
     * User has asked to see a different new parameter in this existing display.
     * Do everything needed to load display with new kind of parameter.
     *
     * @param dataChoice    choice for data
     * @return  true if successfule
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !super.setData(dataChoice)) {
            return false;
        }

        return true;
    }

    /**
     * Called after init().  Load profile into display.
     */
    public void initDone() {
        super.initDone();
        try {
            loadProfile(getPosition());
            profileDisplay.draw();
        } catch (Exception exc) {
            logException("initDone", exc);
        }
    }

    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     * @param preSelectedDataChoices set of preselected data choices
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties,
                                       List preSelectedDataChoices) {

        super.initAfterUnPersistence(vc, properties, preSelectedDataChoices);


        List choices = getDataChoices();
        if(choices.size() > 1) {
            try {
                DataChoice dc = (DataChoice) choices.get(1);
                myTimeHeightControl = new MyTimeHeightControl(this);
                myTimeHeightControl.controlContext = getControlContext();

                myTimeHeightControl.init(dc);

                RealTuple position = getPosition();

                myTimeHeightControl.setPosition(position);
                myTimeHeightControl.initDone();
                addDisplayable(myTimeHeightControl.contourDisplay, timeHeightView);
                myTimeHeightControl.setMySmoothType(mySmoothingType);
                myTimeHeightControl.setMyColorTable(myColorTable);
                myTimeHeightControl.setMyContourInfo(myContourInfo);

                JButton btn = new JButton(dc.getName());
                controlPane.add(btn);
                btn.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)  {
                        try {
                            JFrame frame = new JFrame();
                            frame.add(myTimeHeightControl.doMakeContents());

                            GuiUtils.showFrameAsDialog(controlPane, frame);
                        } catch (Exception ee){}

                    }
                });

            } catch (Exception ee){}
        }
    }

    /**
     * Get the view manager for the control window.
     * @return  control window's view manager
     */
    protected ViewManager getTimeHeightViewManager() {
        return timeHeightView;
    }


    /**
     * Make the UI contents for this control window.
     *
     * @return  UI container
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        // TODO:  This is what should be done - however legends don't show up.
        return GuiUtils.centerBottom(timeHeightView.getContents(),
                GuiUtils.hsplit( doMakeWidgetComponent(), controlPane));
        //return GuiUtils.centerBottom(profileDisplay.getComponent(),
        //                             doMakeWidgetComponent());
    }



    /**
     * Make a 2D display of the range values against domain coordinate # NN.
     *
     * @param fi a VisAD FlatField or seqence of FlatFields with 3 or more
     *           domain coordinates, manifold dimension 1.
     * @param NN an integer, the index number of the coordinate to use
     *               as profile or y axis of plot (0,1,2,...)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void displayTHForCoord(FieldImpl fi, int NN)
            throws VisADException, RemoteException {

        //Have to assume that we have (time -> ((x,y,z)->(param)) because
        // if we don't have a sequence, how can we do a TimeHeight XS?

        Set        timeSet  = fi.getDomainSet();
        double[][] timeVals = timeSet.getDoubles();

        RealTupleType newDomainType = new RealTupleType(RealType.Altitude,
                                          RealType.Time);
        TupleType    parmType     = GridUtil.getParamType(fi);
        int          numParms     = parmType.getNumberOfRealComponents();

        FunctionType newFieldType = new FunctionType(newDomainType, parmType);

        SampledSet   ss           = GridUtil.getSpatialDomain(fi);
        RealType height =
            (RealType) ((SetType) ss.getType()).getDomain().getComponent(NN);
        float[][] latlonalt = ss.getSamples();
        Unit      zUnit     = ss.getSetUnits()[NN];
        if ( !height.equals(RealType.Altitude)) {
            CoordinateSystem cs = ss.getCoordinateSystem();
            latlonalt = cs.toReference(latlonalt, ss.getSetUnits());
            zUnit = cs
                .getReferenceUnits()[cs.getReference().getIndex(RealType.Altitude)];
        }

        int        numTimes      = timeVals[0].length;
        int        numAlts       = ss.getLength();

        double[][] newDomainVals = new double[2][numTimes * numAlts];
        int        l             = 0;
        for (int j = 0; j < numTimes; j++) {
            for (int i = 0; i < numAlts; i++) {
                newDomainVals[0][l] = latlonalt[NN][i];
                newDomainVals[1][l] = timeVals[0][j];
                l++;
            }
        }
        Gridded2DDoubleSet newDomain = new Gridded2DDoubleSet(newDomainType,
                                           newDomainVals, numAlts, numTimes,
                                           (CoordinateSystem) null,
                                           new Unit[] { zUnit,
                timeSet.getSetUnits()[0] }, (ErrorEstimate[]) null);

        float[][] newRangeVals = new float[numParms][numTimes * numAlts];
        int       index        = 0;
        for (int i = 0; i < numTimes; i++) {
            FlatField ff   = (FlatField) fi.getSample(i);
            float[][] vals = ff.getFloats(false);
            for (int j = 0; j < numParms; j++) {
                System.arraycopy(vals[j], 0, newRangeVals[j], index, numAlts);
            }
            index += numAlts;
        }

        FlatField profile = new FlatField(newFieldType, newDomain);

        profile.setSamples(newRangeVals, false);
        if(dataDisplay == null)
            dataDisplay = createDataDisplay();
        ((GridDisplayable) dataDisplay).loadData(profile);

        if (showAsContours) {  // add in the color filled type
            RealType[] origTypes = parmType.getRealComponents();
            RealType[] parmTypes = new RealType[numParms];
            for (int i = 0; i < numParms; i++) {
                RealType parm = (RealType) origTypes[i];
                parmTypes[i] = RealType.getRealType(parm.getName()
                        + "_color", parm.getDefaultUnit());
            }
            RealTupleType colorType = new RealTupleType(parmTypes);

            FieldImpl profileOther = GridUtil.setParamType(profile,
                                         colorType, false);
            if (checkFlag(FLAG_SMOOTHING)
                    && !getSmoothingType().equals(LABEL_NONE)) {
                profileOther = GridUtil.smooth(profileOther, getSmoothingType(),
                        getSmoothingFactor());
            }
           // ((GridDisplayable) contourDisplay).loadData(profileOther);
            if(!colorFill)
                contourDisplay.setLabeling(true);

            contourDisplay.loadData(profileOther);
        }
    }  // end method displayTHForCoord


    /**
     *  Use the value of the smoothing type and weight to subset the data.
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    protected void applySmoothing() throws VisADException, RemoteException {
        if (checkFlag(FLAG_SMOOTHING)) {
            if ( !getSmoothingType().equals(OldSmoothingType)
                    || (getSmoothingFactor() != OldSmoothingFactor)) {
                OldSmoothingType   = getSmoothingType();
                OldSmoothingFactor = getSmoothingFactor();
                loadProfile(getPosition());
            }
        }
    }


    /**
     * Method to call when the probe position changes
     *
     * @param position   new position
     */
    protected void probePositionChanged(RealTuple position) {
        try {
            loadProfile(position);
            if(myTimeHeightControl != null){
                myTimeHeightControl.loadProfile(position);
            }
        } catch (Exception exc) {
            logException("probePositionChanged", exc);
        }

    }

    /**
     * Given the location of the profile SelectorPoint,
     * create a data set for a profile at that location,
     * and load it in display. Show lat-lon location on the control frame.
     *
     * @param position the location
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void loadProfile(RealTuple position)
            throws VisADException, RemoteException {

        if ( !getHaveInitialized()) {
            return;
        }
        if ((fieldImpl == null) || (position == null)) {
            return;
        }
        LatLonPoint llp = getPositionLL(position);

        FieldImpl newFI = GridUtil.getProfileAtLatLonPoint(fieldImpl, llp,
                              getDefaultSamplingModeValue());

        if (newFI != null) {
            displayTHForCoord(newFI, 2);
        }

        // set location label, if available.
        if (llp != null) {
            positionText = getDisplayConventions().formatLatLonPoint(llp);

            // set location label, if available.
            if (latLonWidget != null) {
                latLonWidget.setLat(
                    getDisplayConventions().formatLatLon(
                        llp.getLatitude().getValue()));
                latLonWidget.setLon(
                    getDisplayConventions().formatLatLon(
                        llp.getLongitude().getValue()));
            }
            updateLegendLabel();
        }
    }


    /**
     * make widgets for check box for latest data time on left of x axis.
     *
     * @param controlWidgets to fill
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        ActionListener llListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                handleLatLonWidgetChange();
            }
        };
        latLonWidget = new LatLonWidget("Lat: ", "Lon: ", llListener);
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Position: "),
                                             latLonWidget));
        super.getControlWidgets(controlWidgets);

        // make check box for latest data time on left of x axis
        JCheckBox toggle = new JCheckBox("", isLatestOnLeft);
        toggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isLatestOnLeft = ((JCheckBox) e.getSource()).isSelected();
                try {
                    setXAxisValues();
                } catch (VisADException ve) {
                    userMessage("couldn't set order");
                }
            }
        });
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Latest Data on Left: "),
                GuiUtils.leftCenter(toggle, GuiUtils.filler())));
    }

    /**
     * Handle the user pressing return
     */
    private void handleLatLonWidgetChange() {
        try {
            double   lat = latLonWidget.getLat();
            double   lon = latLonWidget.getLon();
            double[] xyz = earthToBox(makeEarthLocation(lat, lon, 0));
            setProbePosition(xyz[0], xyz[1]);

        } catch (Exception exc) {
            logException("Error setting lat/lon", exc);
        }

    }


    /**
     * Set x (time) axis values depending on data loaded in displayable.
     * Set the order of the axes based on whether latest is on left.
     *
     * @throws VisADException   VisAD error
     */
    private void setXAxisValues() throws VisADException {
        if (getGridDataInstance() == null) {
            return;
        }
        SampledSet timeSet =
            (SampledSet) getGridDataInstance().getGrid().getDomainSet();
        setXAxisLabels(timeSet);
    }

    /**
     * Set x (time) axis values from the time set supplied.
     *
     * @param timeSet   time set to use
     *
     * @throws VisADException   VisAD error
     */
    private void setXAxisLabels(SampledSet timeSet) throws VisADException {
        try {
            if (timeSet == null) {
                return;
            }
            Real startTime = (Real) DataUtility.getSample(timeSet,
                                 0).getComponent(0);
            double start = startTime.getValue();
            Real endTime = (Real) DataUtility.getSample(timeSet,
                               timeSet.getLength() - 1).getComponent(0);
            double    end        = endTime.getValue();
            Hashtable timeLabels = new Hashtable();
            int       numSteps   = timeSet.getLength();

            int       step       = (numSteps < 5)
                                   ? 1
                                   : timeSet.getLength() / 5;  //no more than 5 labels;
            double majorTickSpacing = endTime.getValue()
                                      - startTime.getValue();
            String format = (majorTickSpacing >= 24 * 60 * 60)
                            ? "dd'/'HH"
                            : "HH:mm";

            for (int k = 0; k < timeSet.getLength(); k += step) {
                Real r = (Real) DataUtility.getSample(timeSet,
                             k).getComponent(0);
                double time = r.getValue();
                if (k == step) {
                    majorTickSpacing = (time - start);
                }
                DateTime dt = new DateTime(r);
                timeLabels.put(new Double(time),
                               dt.formattedString(format,
                                   dt.getFormatTimeZone()));
            }
            // do this so we get the last one
            DateTime dt = new DateTime(endTime);
            timeLabels.put(new Double(end),
                           dt.formattedString(format,
                               dt.getFormatTimeZone()));

            if (isLatestOnLeft) {
                profileDisplay.setXRange(end, start);
            } else {
                profileDisplay.setXRange(start, end);
            }
            AxisScale xScale = profileDisplay.getXAxisScale();
            xScale.setTickBase(start);
            //double averageTickSpacing = (end-start)/(double)(numSteps);
            double averageTickSpacing = (end - start);
            xScale.setMajorTickSpacing(averageTickSpacing * step);
            xScale.setMinorTickSpacing(averageTickSpacing * (1.0f/numSteps));
            xScale.setLabelTable(timeLabels);
            //xScale.setTitle("Time (" + dt.getFormatTimeZone().getDisplayName() + ")");
            xScale.setTitle("Time ("
                            + dt.formattedString("z", dt.getFormatTimeZone())
                            + ")");

        } catch (RemoteException re) {}  // can't happen
    }

    /**
     * Get the position as a lat/lon point
     *
     * @param position  position in XYZ or LatLonAlt space
     *
     * @return position as lat/lon
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public LatLonPoint getPositionLL(RealTuple position)
            throws VisADException, RemoteException {

        LatLonPoint   llp    = null;
        RealTupleType rttype = (RealTupleType) position.getType();

        if (rttype.equals(RealTupleType.SpatialCartesian2DTuple)) {
            Real[] reals = position.getRealComponents();
            // get earth location of the x,y position in the VisAD display
            EarthLocationTuple elt =
                (EarthLocationTuple) boxToEarth(new double[] {
                    reals[0].getValue(),
                    reals[1].getValue(), 1.0 });
            llp = elt.getLatLonPoint();
        } else if (rttype.equals(RealTupleType.SpatialEarth2DTuple)) {
            Real[] reals = position.getRealComponents();
            llp = new LatLonTuple(reals[1], reals[0]);
        } else if (rttype.equals(RealTupleType.LatitudeLongitudeTuple)) {
            Real[] reals = position.getRealComponents();
            llp = new LatLonTuple(reals[0], reals[1]);
        } else {
            throw new VisADException(
                "Can't convert position to navigable point");
        }
        return llp;
    }


    /**
     * Add items to the command menu.
     *
     * @param items  menu to add to.
     * @param forMenuBar  whether for menu bar (true) or popup (false)
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        super.getViewMenuItems(items, forMenuBar);

        items.add(GuiUtils.MENU_SEPARATOR);
        if (forMenuBar) {
            JMenu xsMenu = timeHeightView.makeViewMenu();
            xsMenu.setText("Time Height View");
            items.add(xsMenu);
            //adding some control of probe: size, etc
            items.add(doMakeProbeMenu(new JMenu("Probe")));
        }
        List paramItems = new ArrayList();
        JMenuItem addParamItem = doMakeChangeParameterMenuItem();

        paramItems.add(addParamItem);
        List choices = getDataChoices();
        for (int i = 0; i < choices.size(); i++) {
            paramItems.addAll(getParameterMenuItems(i));
        }

        items.add(GuiUtils.makeMenu("Parameters", paramItems));
    }
    /**
     * A hook to allow derived classes to have their own label in the menu
     * for the change data call.
     *
     * @return Menu label for the change data call.
     */
    protected String getChangeParameterLabel() {
        return "Add Parameter...";
    }
    /**
     * Utility to make the menu item for changing the data choice
     *
     * @return The menu item
     */
    protected JMenuItem doMakeChangeParameterMenuItem() {
        final JMenuItem selectChoices =
                new JMenuItem(getChangeParameterLabel());
        selectChoices.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                popupDataDialog("<html>Choose Parameter</html>",
                        selectChoices);
            }
        });
        List choices = getDataChoices();
        if(choices.size() > 1)
            selectChoices.setEnabled(false);
        else
            selectChoices.setEnabled(true);
        return selectChoices;
    }

    private List getParameterMenuItems(final int row) {
        List               items   = new ArrayList();
        DataChoice dc = (DataChoice)getDataChoices().get(row);
        JMenu paramMenu = new JMenu("Parameter " + dc.getName());
        items.add(paramMenu);
        JMenuItem jmi;

        // change unit choice

        // Remove this parameter
        jmi = new JMenuItem("Remove");
        if(dc.getName() != paramName)
            paramMenu.add(jmi);
        jmi.addActionListener(new ActionListener() {
            public void actionPerformed(
                    ActionEvent ev) {
                removeField(row);
                updateLegendLabel();
            }
        });

        return items;
    }

    /**
     * Called when the user chooses new data for this display
     *
     * @param newChoices List of new {@link ucar.unidata.data.DataChoice}-s
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void addNewData(List newChoices)
            throws VisADException, RemoteException {
        processNewData(newChoices);
        doShare(SHARE_CHOICES, newChoices);
    }

    /**
     * Override base class method which is called when the user has selected
     * new data choices.
     *
     * @param newChoices    new list of choices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void processNewData(List newChoices)
            throws VisADException, RemoteException {
        DataChoice dc = (DataChoice) newChoices.get(0);
        myTimeHeightControl = new MyTimeHeightControl(this);
        myTimeHeightControl.controlContext = getControlContext();

        myTimeHeightControl.init(dc);

        RealTuple position = getPosition();

        myTimeHeightControl.setPosition(position);
        myTimeHeightControl.initDone();
        addDisplayable(myTimeHeightControl.contourDisplay, timeHeightView);
        JButton btn = new JButton(dc.getName());
        controlPane.add(btn);
        btn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)  {
                try {
                    JFrame frame = new JFrame();
                    frame.add(myTimeHeightControl.doMakeContents());
                    GuiUtils.showFrameAsDialog(controlPane, frame);
                } catch (Exception ee){}

            }
        });
        showNormalCursor();
        appendDataChoices(newChoices);

        //doMoveProbe();
    }


    /**
     * remove all widget associated with additional parameter
     * @return
     */
    private void removeField(int idx) {
        if (idx < 0) {
            return;
        }
        List choices = getDataChoices();
        DataChoice dc = (DataChoice) choices.get(idx);
        if (dc != null) {
            removeDataChoice(dc);
        }
        try {
            removeDisplayable(myTimeHeightControl.contourDisplay);
            controlPane.remove(idx-1);
        } catch (Exception ee){}
        //fireStructureChanged();
        doMoveProbe();  // update the side legend label if needed
    }


    /**
     * Add tabs to the properties dialog.
     *
     * @param jtp  the JTabbedPane to add to
     */
    public void addPropertiesComponents(JTabbedPane jtp) {
        super.addPropertiesComponents(jtp);

        if (timeHeightView != null) {
            jtp.add("Time Height View",
                    timeHeightView.getPropertiesComponent());
        }
    }

    /**
     * Apply the properties
     *
     * @return true if successful
     */
    public boolean doApplyProperties() {
        if ( !super.doApplyProperties()) {
            return false;
        }
        if (timeHeightView != null) {
            return timeHeightView.applyProperties();
        }
        return true;
    }

    /**
     * Apply the preferences.  Used to pick up the date format changes.
     */
    public void applyPreferences() {
        super.applyPreferences();
        try {
            setXAxisValues();
        } catch (Exception exc) {
            logException("applyPreferences", exc);
        }
    }

    /**
     * Set whether latest data is displayed on the left or right
     * side of the plot.  Used by XML persistence mainly.
     *
     * @param yesorno  <code>true</code> if want latest is on left.
     */
    public void setLatestOnLeft(boolean yesorno) {
        isLatestOnLeft = yesorno;
    }

    /**
     * Get whether latest data is displayed on the left or right
     * side of the plot.
     *
     * @return  <code>true</code> if latest is on left.
     */
    public boolean getLatestOnLeft() {
        return isLatestOnLeft;
    }

    /**
     * Get whether the display is shown as an image.
     *
     * @param yesorno  <code>true</code> if want an image instead of contours.
     * @deprecated  use #setShowAsContours(boolean) instead
     */
    public void setShowAsImage(boolean yesorno) {
        showAsContours = !yesorno;
    }

    /**
     * Get whether the display is shown as contours.
     *
     * @param yesorno  <code>true</code> if want contours instead of an image.
     */
    public void setShowAsContours(boolean yesorno) {
        showAsContours = yesorno;
    }

    /**
     * Get whether the display is an image or contours.
     *
     * @return  <code>true</code> if contours display, false if image
     */
    public boolean getShowAsContours() {
        return showAsContours;
    }

    /**
     * Get whether the display is shown as contours.
     *
     * @param yesorno  <code>true</code> if want contours instead of an image.
     */
    public void setColorFill(boolean yesorno) {
        colorFill = yesorno;
    }

    /**
     * Get whether the display is an image or contours.
     *
     * @return  <code>true</code> if contours display, false if image
     */
    public boolean getColorFill() {
        return colorFill;
    }
    /**
     * Get the foreground color
     *
     * @return the foreground color
     */
    public Color getForeground() {
        return getTimeHeightViewManager().getForeground();
    }

    /**
     * Set the foreground color
     *
     * @param color    new color
     */
    public void setForeground(Color color) {
        this.foreground = color;
    }



    /**
     * Get the background color
     *
     * @return the background color
     */
    public Color getBackground() {
        return getTimeHeightViewManager().getBackground();
    }

    /**
     * Set the background color
     *
     * @param color   new color
     */
    public void setBackground(Color color) {
        this.background = color;
    }

    /**
     * Get the background color
     *
     * @return the background color
     */
    public ColorTable getMyColorTable() {
        if(myTimeHeightControl != null) {
            myColorTable = myTimeHeightControl.getColorTable();
            return myColorTable;
        } else
            return null;
    }

    /**
     * Set the background color
     *
     * @param color   new color
     */
    public void setMyColorTable(ColorTable color) throws RemoteException, VisADException {
        this.myColorTable = color;
    }

    /**
     * Get the background color
     *
     * @return the background color
     */
    public ContourInfo getMyContourInfo( ){
        if(myTimeHeightControl != null) {
            myContourInfo = myTimeHeightControl.getContourInfo();
            return myContourInfo;
        } else
            return null;
    }

    /**
     * Set the background color
     *
     * @param contourInfo   new color
     */
    public void setMyContourInfo(ContourInfo contourInfo) throws RemoteException, VisADException {
        this.myContourInfo = contourInfo;
    }


    /**
     * Get the background color
     *
     * @return the background color
     */
    public String getMySmoothingType() {
        if(myTimeHeightControl != null) {
            mySmoothingType = myTimeHeightControl.getSmoothingType();
            return mySmoothingType;
        } else
            return LABEL_NONE;
    }

    /**
     * Set the background color
     *
     * @param smoothingType   new color
     */
    public void setMySmoothingType(String smoothingType) throws RemoteException, VisADException {
        this.mySmoothingType = smoothingType;
    }

    /**
     * Get the background color
     *
     * @return the background color
     */
    public int getMySmoothingFactor() {
        if(myTimeHeightControl != null) {
            mySmoothingFactor = myTimeHeightControl.getSmoothingFactor();
            return mySmoothingFactor;
        } else
            return 6;
    }

    /**
     * Set the background color
     *
     * @param smoothingFactor   new color
     */
    public void setMySmoothingType(int smoothingFactor) throws RemoteException, VisADException {
        this.mySmoothingFactor = smoothingFactor;
    }


    static public class MyTimeHeightControl extends TimeHeightControl {
        TimeHeightControl timeHeightControl;
        Unit displayunit = null;
        private Range colorRange = null;

        ContourInfo contourInfo;

        private Color color;

        private String OldSmoothingType = LABEL_NONE;

        private int OldSmoothingFactor = 0;

        public MyTimeHeightControl() {
            setAttributeFlags(FLAG_COLORTABLE | FLAG_CONTOUR | FLAG_DISPLAYUNIT | FLAG_SMOOTHING );
        }
        public MyTimeHeightControl(TimeHeightControl thc) {
            this.timeHeightControl = thc;
            setAttributeFlags(FLAG_COLORTABLE | FLAG_CONTOUR | FLAG_DISPLAYUNIT | FLAG_SMOOTHING );
        }

        /**
         * Construct the display, frame, and controls
         *
         * @param dataChoice the data to use
         *
         * @return  true if successful
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        public boolean init(DataChoice dataChoice)
                throws VisADException, RemoteException {

            timeHeightView = new TimeHeightViewManager(getViewContext(),
                    new ViewDescriptor("timeheight_of_1_" + paramName),
                    "showControlLegend=false;wireframe=true;") {}
            ;
            profileDisplay = timeHeightView.getTimeHeightDisplay();

            //If foreground is not null  then this implies we have been unpersisted
            if (foreground != null) {
                timeHeightView.setColors(foreground, background);
            }
            //Call doMakeProbe here so we link to the right ViewManager
            //If we do it after the addViewManager then we screw up persistence
            //doMakeProbe();
            contourDisplay = new Contour2DDisplayable("th_color_" + paramName,
                    true, false);
            contourDisplay.setVisible(true);

            contourDisplay.setVisible(true);
            addDisplayable(contourDisplay, timeHeightView,
                    FLAG_COLORTABLE | FLAG_CONTOUR | FLAG_DISPLAYUNIT | FLAG_SMOOTHING);

            if ( !setData(dataChoice)) {
                return false;
            }

            return true;
        }

        /**
         * Has this control been initialized
         *
         * @return Is this control initialized
         */
        public boolean getHaveInitialized() {
            return true;
        }

        /**
         * Called after init().  Load profile into display.
         */
        public void initDone() {
            try {
                loadProfile(getPosition());
                profileDisplay.draw();
            } catch (Exception exc) {
                logException("initDone", exc);
            }
        }

        /**
         * Given the location of the profile SelectorPoint,
         * create a data set for a profile at that location,
         * and load it in display.
         *
         * @param position the location
         *
         * @throws VisADException   VisAD failure.
         * @throws RemoteException  Java RMI failure.
         */
        public void loadProfile(RealTuple position)
                throws VisADException, RemoteException {

            if ( !getHaveInitialized()) {
              //  return;
            }
            if(position == null)
                position = getInitialPosition();

            if ((fieldImpl == null) || (position == null)) {
                return;
            }
            LatLonPoint llp = getPositionLL(position);

            FieldImpl newFI = GridUtil.getProfileAtLatLonPoint(fieldImpl, llp,
                    getDefaultSamplingModeValue());

            if (newFI != null) {
                displayTHForCoord(newFI, 2);
            }

        }

        /**
         * Make a 2D display of the range values against domain coordinate # NN.
         *
         * @param fi a VisAD FlatField or seqence of FlatFields with 3 or more
         *           domain coordinates, manifold dimension 1.
         * @param NN an integer, the index number of the coordinate to use
         *               as profile or y axis of plot (0,1,2,...)
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        protected void displayTHForCoord(FieldImpl fi, int NN)
                throws VisADException, RemoteException {

            //Have to assume that we have (time -> ((x,y,z)->(param)) because
            // if we don't have a sequence, how can we do a TimeHeight XS?

            Set        timeSet  = fi.getDomainSet();
            double[][] timeVals = timeSet.getDoubles();

            RealTupleType newDomainType = new RealTupleType(RealType.Altitude,
                    RealType.Time);
            TupleType    parmType     = GridUtil.getParamType(fi);
            int          numParms     = parmType.getNumberOfRealComponents();

            FunctionType newFieldType = new FunctionType(newDomainType, parmType);

            SampledSet   ss           = GridUtil.getSpatialDomain(fi);
            RealType height =
                    (RealType) ((SetType) ss.getType()).getDomain().getComponent(NN);
            float[][] latlonalt = ss.getSamples();
            Unit      zUnit     = ss.getSetUnits()[NN];
            if ( !height.equals(RealType.Altitude)) {
                CoordinateSystem cs = ss.getCoordinateSystem();
                latlonalt = cs.toReference(latlonalt, ss.getSetUnits());
                zUnit = cs
                        .getReferenceUnits()[cs.getReference().getIndex(RealType.Altitude)];
            }

            int        numTimes      = timeVals[0].length;
            int        numAlts       = ss.getLength();

            double[][] newDomainVals = new double[2][numTimes * numAlts];
            int        l             = 0;
            for (int j = 0; j < numTimes; j++) {
                for (int i = 0; i < numAlts; i++) {
                    newDomainVals[0][l] = latlonalt[NN][i];
                    newDomainVals[1][l] = timeVals[0][j];
                    l++;
                }
            }
            Gridded2DDoubleSet newDomain = new Gridded2DDoubleSet(newDomainType,
                    newDomainVals, numAlts, numTimes,
                    (CoordinateSystem) null,
                    new Unit[] { zUnit,
                            timeSet.getSetUnits()[0] }, (ErrorEstimate[]) null);

            float[][] newRangeVals = new float[numParms][numTimes * numAlts];
            int       index        = 0;
            for (int i = 0; i < numTimes; i++) {
                FlatField ff   = (FlatField) fi.getSample(i);
                float[][] vals = ff.getFloats(false);
                for (int j = 0; j < numParms; j++) {
                    System.arraycopy(vals[j], 0, newRangeVals[j], index, numAlts);
                }
                index += numAlts;
            }

            FlatField profile = new FlatField(newFieldType, newDomain);

            profile.setSamples(newRangeVals, false);
            if (checkFlag(FLAG_SMOOTHING)
                    && !getSmoothingType().equals(LABEL_NONE)) {
                profile = (FlatField)GridUtil.smooth((FieldImpl) profile, getSmoothingType(),
                        getSmoothingFactor());
            }
            contourDisplay.loadData(profile);
            contourDisplay.setVisible(true);
            // add in the color filled type


        }  // end method displayTHForCoord

        /**
         *  Use the value of the smoothing type and weight to subset the data.
         *
         * @throws RemoteException Java RMI problem
         * @throws VisADException  VisAD problem
         */
        protected void applySmoothing() throws VisADException, RemoteException {
            if (checkFlag(FLAG_SMOOTHING)) {
                if ( !getSmoothingType().equals(OldSmoothingType)
                        || (getSmoothingFactor() != OldSmoothingFactor)) {
                    OldSmoothingType   = getSmoothingType();
                    OldSmoothingFactor = getSmoothingFactor();
                    loadProfile(getPosition());
                }
            }
        }
        protected String getTitle() {
            //Use the bottom legend text as the window title
            return " ";
        }
        public void setTitle(String title) {

        }

        /**
         * Make the UI contents for this control window.
         *
         * @return  UI container
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        protected Container doMakeContents()
                throws VisADException, RemoteException {

            // TODO:  This is what should be done - however legends don't show up.
            return doMakeWidgetComponent() ;
            //return GuiUtils.centerBottom(profileDisplay.getComponent(),
            //                             doMakeWidgetComponent());
        }
        /**
         * make widgets for check box for latest data time on left of x axis.
         *
         * @param controlWidgets to fill
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        public void getControlWidgets(List controlWidgets)
                throws VisADException, RemoteException {
            controlWidgets.add(contourWidget = new ContourWidget(this,
                    getContourInfo()));
            addRemovable(contourWidget);

            controlWidgets.add(getColorTableWidget(getRangeForColorTable()));

            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Smoothing:"), doMakeSmoothingWidget()));

        }

        public String getColorWidgetLabel() {
            return "Color";
        }
        /**
         * User has asked to see a different new parameter in this existing display.
         * Do everything needed to load display with new kind of parameter.
         *
         * @param dataChoice    choice for data
         * @return  true if successfule
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        protected boolean setData(DataChoice dataChoice)
                throws VisADException, RemoteException {
            super.setData(dataChoice);
            paramName = dataChoice.getName();
            GridDataInstance di = (GridDataInstance)doMakeDataInstance(dataChoice);
            contourInfo = getContourInfo();
            colorRange = di.getRange(0);
            displayunit = ((GridDataInstance) di).getRawUnit(0);
            this.fieldImpl = ((GridDataInstance)di).getGrid();

            return true;
        }

        /**
         * Get the range for the color table.
         *
         * @return range being used
         * @throws RemoteException  some RMI exception occured
         * @throws VisADException  error getting the range in VisAD
         */
        public Range getRangeForColorTable()
                throws RemoteException, VisADException {
            return colorRange;
        }

        public Unit getDisplayUnit() {
            Unit unit = displayunit;

            setDisplayUnit(unit);

            return unit;
        }

        public void setMyColorTable(ColorTable color) throws RemoteException, VisADException {
            setColorTable(color);
        }
        public void setMyContourInfo(ContourInfo contourInfo) throws RemoteException, VisADException {
            setContourInfo(contourInfo);
        }

        public void setMySmoothType(String smoothType)  {
            setSmoothingType(smoothType);
        }
    }
}

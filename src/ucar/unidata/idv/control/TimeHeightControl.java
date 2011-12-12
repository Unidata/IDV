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


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.TimeHeightViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;

import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.*;

import ucar.visad.ShapeUtility;


import ucar.visad.display.ColorScale;
import ucar.visad.display.Contour2DDisplayable;
import ucar.visad.display.Displayable;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.GridDisplayable;
import ucar.visad.display.XYDisplay;

import visad.*;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


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
    private XYDisplay profileDisplay;

    /** the data */
    private FieldImpl fieldImpl;

    /** Displayable for color filled contours */
    private DisplayableData dataDisplay;

    /** Displayable for contours */
    private Contour2DDisplayable contourDisplay;

    /** flag for XAxis time orientation */
    private boolean isLatestOnLeft = true;

    /** flag for type of color display */
    private boolean showAsContours = true;

    /** foreground color */
    private Color foreground;

    /** background color */
    private Color background;

    /** The latlon widget */
    private LatLonWidget latLonWidget;

    /** the control window's view manager */
    protected TimeHeightViewManager timeHeightView;

    /**
     *  Default Contructor; sets flags. See init() for creation actions.
     */
    public TimeHeightControl() {
        setAttributeFlags(FLAG_DATACONTROL | FLAG_COLOR);
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
            dataDisplay = new Contour2DDisplayable("th_color_" + paramName,
                    true, true);
            dataDisplay.setVisible(true);
            contourDisplay = new Contour2DDisplayable("th_contour_"
                    + paramName);
            contourDisplay.setVisible(true);
            addDisplayable(dataDisplay, timeHeightView,
                           FLAG_COLORTABLE | FLAG_CONTOUR | FLAG_DISPLAYUNIT);
            addDisplayable(contourDisplay, timeHeightView,
                           FLAG_COLOR | FLAG_CONTOUR | FLAG_DISPLAYUNIT);

        } else {
            dataDisplay = createDataDisplay();
            addDisplayable(dataDisplay, timeHeightView,
                           getDataDisplayFlags());
        }

        if ( !setData(dataChoice)) {
            return false;
        }

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

        fieldImpl = getGridDataInstance().getGrid();

        boolean isSequence = GridUtil.isTimeSequence(fieldImpl);

        // Check to see if this is a time sequence or not.  No sense
        // creating a timeHeight CrossSection with no times or one time.
        if ( !isSequence || (fieldImpl.getDomainSet().getLength() <= 1)) {
            throw new VisADException(
                "Need more than one time to create a TimeHeight Cross Section");
        }

        setXAxisLabels((SampledSet) fieldImpl.getDomainSet());
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
                                     doMakeWidgetComponent());
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

            contourDisplay.loadData(profileOther);
        }
    }  // end method displayTHForCoord


    /**
     * Method to call when the probe position changes
     *
     * @param position   new position
     */
    protected void probePositionChanged(RealTuple position) {
        try {
            loadProfile(position);
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
            xScale.setMinorTickSpacing(averageTickSpacing);
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
    }

    /**
     * Actually create the color scales.  Override to only show in
     * control window
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void doMakeColorScales()
            throws VisADException, RemoteException {
        colorScales = new ArrayList();
        if (colorScaleInfo == null) {
            colorScaleInfo = getDefaultColorScaleInfo();
        }
        ColorScale colorScale = new ColorScale(getColorScaleInfo());
        addDisplayable(colorScale, timeHeightView, FLAG_COLORTABLE);
        colorScales.add(colorScale);

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

}

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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridMath;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.HovmollerViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.visad.display.ColorScale;
import ucar.visad.display.Contour2DDisplayable;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.GridDisplayable;
import ucar.visad.display.XYDisplay;

import visad.AxisScale;
import visad.CoordinateSystem;
import visad.DateTime;
import visad.ErrorEstimate;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DDoubleSet;
import visad.GriddedSet;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.TupleType;
import visad.Unit;
import visad.VisADException;

import visad.util.DataUtility;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Mon, Apr 25, '11
 * @author         Enter your name here...
 */
public class HovmollerControl extends GridDisplayControl {

    /** the longitude dimenstion id */
    public static final int LON_DIM = 0;

    /** the latitude dimension id */
    public static final int LAT_DIM = 1;

    /** the default averaging dimension */
    private int averageDim = LAT_DIM;

    /** XY display for displaying time/height diagram */
    private XYDisplay hovmollerDisplay;

    /** the data */
    private FieldImpl fieldImpl;

    /** Displayable for color shaded display */
    private DisplayableData dataDisplay;

    /** Displayable for contours */
    private Contour2DDisplayable contourDisplay;

    /** flag for type of color display */
    private boolean showAsContours = true;

    /** foreground color */
    private Color foreground;

    /** background color */
    private Color background;

    /** default time format */
    private static final String defaultTimeFormat = "MMM dd HH'Z'";

    /** time format */
    private String timeFormat = defaultTimeFormat;

    /** Hovmoller ViewManager */
    private HovmollerViewManager hovmollerView;

    /** flag for reversing time */
    private boolean reverseTime = true;

    /**
     *  Default Contructor; sets flags. See init() for creation actions.
     */
    public HovmollerControl() {
        setAttributeFlags(FLAG_DATACONTROL);
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

        if (hovmollerView != null) {
            //If the ViewManager is non-null it means we have been unpersisted.
            //If so, we initialie the VM with the IDV
            hovmollerView.initAfterUnPersistence(getIdv());
            hovmollerDisplay = hovmollerView.getHovmollerDisplay();
        } else {
            hovmollerView = new HovmollerViewManager(getViewContext(),
                    new ViewDescriptor("hovmoller_of_" + paramName),
                    "showControlLegend=false;wireframe=true;");
            hovmollerDisplay = hovmollerView.getHovmollerDisplay();
            hovmollerDisplay.setAspect(.8, 1.0);
        }
        hovmollerDisplay.setYAxisType(RealType.Time);
        hovmollerDisplay.setXAxisType((getAverageDimension() == LAT_DIM)
                                      ? RealType.Longitude
                                      : RealType.Latitude);

        //If foreground is not null  then this implies we have been unpersisted
        if (foreground != null) {
            hovmollerView.setColors(foreground, background);
        }

        addViewManager(hovmollerView);

        if (showAsContours) {
            addAttributeFlags(FLAG_COLOR);
            dataDisplay = new Contour2DDisplayable("ts_color_" + paramName,
                    true, true);
            dataDisplay.setVisible(true);
            contourDisplay = new Contour2DDisplayable("ts_contour_"
                    + paramName);
            contourDisplay.setVisible(true);
            addDisplayable(dataDisplay, hovmollerView,
                           FLAG_COLORTABLE | FLAG_CONTOUR | FLAG_DISPLAYUNIT);
            addDisplayable(contourDisplay, hovmollerView,
                           FLAG_COLOR | FLAG_CONTOUR | FLAG_DISPLAYUNIT);

        } else {
            dataDisplay = createDataDisplay();
            addDisplayable(dataDisplay, hovmollerView, getDataDisplayFlags());
        }

        if ( !setData(dataChoice)) {
            return false;
        }

        return true;
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
        DisplayableData dataDisplay = new Grid2DDisplayable("ts_color_"
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
                "Need more than one time to create a Time-Space Diagram");
        }
        loadData();
        setTimeAxisLabels((SampledSet) fieldImpl.getDomainSet());

        return true;
    }

    /**
     * Load the data into the display
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void loadData() throws VisADException, RemoteException {

        if ( !getHaveInitialized()) {
            return;
        }
        if (fieldImpl == null) {
            return;
        }

        displayTSForCoord(fieldImpl, getAverageDimension());
    }

    /**
     * Called after init().  Load profile into display.
     */
    public void initDone() {
        super.initDone();
        try {
            loadData();
            hovmollerDisplay.draw();
        } catch (Exception exc) {
            logException("initDone", exc);
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
    protected void displayTSForCoord(FieldImpl fi, int NN)
            throws VisADException, RemoteException {

        //Have to assume that we have (time -> ((x,y,z)->(param)) because
        // if we don't have a sequence, how can we do a TimeHeight XS?
        if (GridUtil.isVolume(fi)) {
            throw new VisADException("Need to select a level");
        }
        int           averageIndex  = NN;
        int           spatialIndex  = 1 - NN;

        Set           timeSet       = fi.getDomainSet();
        double[][]    timeVals      = timeSet.getDoubles();
        GriddedSet spatialDomain = (GriddedSet) GridUtil.getSpatialDomain(fi);
        int[]         sizes         = spatialDomain.getLengths();

        TupleType     parmType      = GridUtil.getParamType(fi);
        int           numParms      = parmType.getNumberOfRealComponents();
        RealType      parm = (RealType) parmType.getRealComponents()[0];
        RealType      other         = (spatialIndex == LON_DIM)
                                      ? RealType.Longitude
                                      : RealType.Latitude;

        RealTupleType newDomainType = new RealTupleType(other, RealType.Time);
        FieldImpl data = GridMath.applyFunctionToAxis(fi,
                             GridMath.FUNC_AVERAGE, ((averageIndex == LAT_DIM)
                ? GridMath.AXIS_Y
                : GridMath.AXIS_X));

        FunctionType     newFieldType  = new FunctionType(newDomainType,
                                             parm);
        int              numTimes      = timeVals[0].length;
        int              numSpace      = sizes[spatialIndex];
        int              numOtherSpace = sizes[averageIndex];

        float[][]        latlonalt     = spatialDomain.getSamples();
        Unit             spaceUnit =
            spatialDomain.getSetUnits()[spatialIndex];
        CoordinateSystem cs            = spatialDomain.getCoordinateSystem();
        if (cs != null) {
            spatialIndex = cs.getReference().getIndex(other);
            latlonalt = cs.toReference(latlonalt,
                                       spatialDomain.getSetUnits());
            spaceUnit = cs.getReferenceUnits()[spatialIndex];
        }

        double[][] newDomainVals = new double[2][numTimes * numSpace];
        int        l             = 0;
        for (int j = 0; j < numTimes; j++) {
            for (int i = 0; i < numSpace; i++) {
                int index = (averageIndex == LAT_DIM)
                            ? i
                            : i * numOtherSpace;
                newDomainVals[0][l] = latlonalt[spatialIndex][index];
                newDomainVals[1][l] = timeVals[0][j];
                l++;
            }
        }
        Gridded2DDoubleSet newDomain = new Gridded2DDoubleSet(newDomainType,
                                           newDomainVals, numSpace, numTimes,
                                           (CoordinateSystem) null,
                                           new Unit[] { spaceUnit,
                timeSet.getSetUnits()[0] }, (ErrorEstimate[]) null, false);

        float[][] newRangeVals = new float[numParms][numTimes * numSpace];
        int       index        = 0;
        for (int i = 0; i < numTimes; i++) {
            FlatField ff   = (FlatField) data.getSample(i);
            float[][] vals = ff.getFloats(false);
            for (int j = 0; j < numParms; j++) {
                // for lat (y) averaging, just take the first row
                if (averageIndex == LAT_DIM) {
                    System.arraycopy(vals[j], 0, newRangeVals[j], index,
                                     numSpace);
                } else {
                    // for lon (x) averaging, get the first value in each row
                    for (int k = 0; k < numSpace; k++) {
                        newRangeVals[j][index + k] =
                            vals[j][k * numOtherSpace];
                    }
                }
            }
            index += numSpace;
        }

        FlatField hovData = new FlatField(newFieldType, newDomain);

        hovData.setSamples(newRangeVals, false);
        ((GridDisplayable) dataDisplay).loadData(hovData);

        if (showAsContours) {  // add in the color filled type
            RealType[] origTypes = parmType.getRealComponents();
            RealType[] parmTypes = new RealType[numParms];
            for (int i = 0; i < numParms; i++) {
                RealType param = (RealType) origTypes[i];
                parmTypes[i] = RealType.getRealType(param.getName()
                        + "_color", param.getDefaultUnit());
            }
            RealTupleType colorType = new RealTupleType(parmTypes);

            FieldImpl hovDataOther = GridUtil.setParamType(hovData,
                                         colorType, false);

            contourDisplay.loadData(hovDataOther);
        }
        setXAxisLabels(spatialDomain);
    }  // end method displayTHForCoord

    /**
     * Add DisplaySettings appropriate for this display
     *
     * @param dsd  the dialog to add to
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        dsd.addPropertyValue(getTimeFormat(), "timeFormat",
                             "Time Label Format", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Boolean(getReverseTime()), "reverseTime",
                             "Latest Time at Bottom", SETTINGS_GROUP_DISPLAY);
    }

    /**
     * Return the label that is to be used for the color widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the color widget
     */
    public String getColorWidgetLabel() {
        return "Contour Line Color";
    }

    /**
     * Get the default display list template for this control.  Subclasses can override
     * @return the default template
     */
    protected String getDefaultDisplayListTemplate() {
        if (getGridDataInstance() == null) {
            return MACRO_SHORTNAME;
        }
        try {
            GriddedSet domainSet = (GriddedSet) GridUtil.getSpatialDomain(
                                       getGridDataInstance().getGrid());
            float[][]        ranges   = getLatLonRanges(domainSet);
            CoordinateSystem cs       = domainSet.getCoordinateSystem();
            int              dimIndex = averageDim;
            if (cs != null) {
                dimIndex = cs.getReference().getIndex((averageDim == LAT_DIM)
                        ? RealType.Latitude
                        : RealType.Longitude);
            }
            float high = ranges[dimIndex][1];
            float low  = ranges[dimIndex][0];
            if (averageDim == LON_DIM) {  // switch to be left to right
                float temp = high;
                high = low;
                low  = temp;
            }
            DisplayConventions dc = getDisplayConventions();
            return MACRO_SHORTNAME + " - "
                   + dc.formatLatLonCardinal(high, averageDim) + "-"
                   + dc.formatLatLonCardinal(low, averageDim);
        } catch (VisADException ve) {
            return MACRO_SHORTNAME;
        }
    }

    /**
     * Get the lat/lon ranges of the domainSet
     *
     * @param domainSet  the spatial domain
     *
     * @return  the ranges
     *
     * @throws VisADException problem getting at values
     */
    private float[][] getLatLonRanges(GriddedSet domainSet)
            throws VisADException {
        CoordinateSystem cs       = domainSet.getCoordinateSystem();
        float[]          his      = domainSet.getHi();
        float[]          los      = domainSet.getLow();
        int              dimIndex = averageDim;
        float[][]        ranges   = null;
        if (cs != null) {
            float[][] latlons = cs.toReference(domainSet.getSamples());
            ranges = Misc.getRanges(latlons);
        } else {
            ranges = Misc.getRanges(domainSet.getSamples(false));
        }
        return ranges;
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
        return GuiUtils.centerBottom(hovmollerView.getContents(),
                                     doMakeWidgetComponent());
        //return GuiUtils.centerBottom(profileDisplay.getComponent(),
        //                             doMakeWidgetComponent());
    }

    /**
     * Set x (time) axis values depending on data loaded in displayable.
     * Set the order of the axes based on whether latest is on left.
     *
     * @throws VisADException   VisAD error
     */
    private void setTimeAxisValues() throws VisADException {
        if (getGridDataInstance() == null) {
            return;
        }
        SampledSet timeSet =
            (SampledSet) getGridDataInstance().getGrid().getDomainSet();
        setTimeAxisLabels(timeSet);
    }

    /**
     * Set the X axis values
     *
     * @throws VisADException  problem getting the data for the labels
     */
    private void setXAxisValues() throws VisADException {
        if (getGridDataInstance() == null) {
            return;
        }
        GriddedSet domainSet = (GriddedSet) GridUtil.getSpatialDomain(
                                   getGridDataInstance().getGrid());
        setXAxisLabels(domainSet);
    }

    /**
     * Set the axis labels
     *
     * @throws VisADException  problem retrieving data for labels
     */
    private void setAxisLabels() throws VisADException {
        setXAxisValues();
        setTimeAxisValues();
    }

    /**
     * Method called when the view changes.
     *
     * @param property The property that changed
     */
    public void viewManagerChanged(String property) {
        try {
            setAxisLabels();
        } catch (Exception e) {
            logException("Problem setting labels", e);
        }
        super.viewManagerChanged(property);
    }

    /**
     * Set x (time) axis values from the time set supplied.
     *
     * @param timeSet   time set to use
     *
     * @throws VisADException   VisAD error
     */
    private void setTimeAxisLabels(SampledSet timeSet) throws VisADException {
        try {
            if (timeSet == null) {
                return;
            }
            Real startTime = (Real) DataUtility.getSample(timeSet,
                                 0).getComponent(0);
            double start    = startTime.getValue();
            int    numSteps = timeSet.getLength();
            Real endTime = (Real) DataUtility.getSample(timeSet,
                               numSteps - 1).getComponent(0);
            double end = endTime.getValue();
            Hashtable<Double, String> timeLabels = new Hashtable<Double,
                                                       String>();

            int step = (numSteps < 5)
                       ? 1
                       : timeSet.getLength() / 5;  //no more than 5 labels;
            double majorTickSpacing = endTime.getValue()
                                      - startTime.getValue();
            /*
            String format = (majorTickSpacing >= 24 * 60 * 60)
                            ? "dd'/'HH"
                            : "HH:mm";
            */
            String format = getTimeFormat();

            int    k;
            for (k = 0; k < numSteps; k += step) {
                Real r = (Real) DataUtility.getSample(timeSet,
                             k).getComponent(0);
                double time = r.getValue();
                if (k == step) {
                    majorTickSpacing = (time - start);
                }
                DateTime dt = new DateTime(r);
                timeLabels.put(new Double(time),
                               dt.formattedString(format,
                                   DateTime.getFormatTimeZone()));
            }
            // do this so we get the last one
            if (k - step < numSteps - step / 2) {
                DateTime dt = new DateTime(endTime);
                timeLabels.put(new Double(end),
                               dt.formattedString(format,
                                   DateTime.getFormatTimeZone()));
            }

            if (getReverseTime()) {
                hovmollerDisplay.setYRange(end, start);
            } else {
                hovmollerDisplay.setYRange(start, end);
            }
            Font f    = hovmollerView.getDisplayListFont();
            int  size = (f == null)
                        ? 12
                        : f.getSize();
            if ((f != null)
                    && f.getName().equals(FontSelector.DEFAULT_NAME)) {
                f = null;
            }
            AxisScale timeScale = hovmollerDisplay.getYAxisScale();
            timeScale.setFont(f);
            timeScale.setLabelSize(size);
            timeScale.setSnapToBox(true);
            timeScale.setTickBase(start);
            timeScale.setMajorTickSpacing(majorTickSpacing);
            //timeScale.setMinorTickSpacing(averageTickSpacing*step);
            timeScale.setLabelTable(timeLabels);
            timeScale.setTitle("Time");

            AxisScale spaceScale = hovmollerDisplay.getXAxisScale();
            spaceScale.setSnapToBox(true);

        } catch (RemoteException re) {}  // can't happen
    }

    /**
     * Set x (lat/lon) axis values from spatial domain
     *
     * @param spatialSet   the spatial domain set to use
     *
     * @throws VisADException   VisAD error
     */
    private void setXAxisLabels(GriddedSet spatialSet) throws VisADException {
        if (spatialSet == null) {
            return;
        }
        float[][]        ranges   = getLatLonRanges(spatialSet);
        CoordinateSystem cs       = spatialSet.getCoordinateSystem();
        int              dimIndex = 1 - averageDim;
        if (cs != null) {
            dimIndex = cs.getReference().getIndex((averageDim == LAT_DIM)
                    ? RealType.Longitude
                    : RealType.Latitude);
        }
        double                    min     = ranges[dimIndex][0];
        double                    max     = ranges[dimIndex][1];
        double                    range   = Math.abs(max - min);
        Hashtable<Double, String> xLabels = new Hashtable<Double, String>();

        double                    tens    = 1.0;
        if (range < tens) {
            tens /= 10.0;
            while (range < tens) {
                tens /= 10.0;
            }
        } else {
            while (10.0 * tens <= range) {
                tens *= 10.0;
            }
        }
        // now tens <= range < 10.0 * tens;
        double ratio = range / tens;
        if (ratio < 2.0) {
            tens = tens / 5.0;
        } else if (ratio < 4.0) {
            tens = tens / 2.0;
        }
        double majorTickSpacing = tens;
        // now tens = interval between major tick marks (majorTickSpacing)

        double[] hilo = Misc.computeTicks(max, min, 0, majorTickSpacing);
        // firstValue is the first Tick mark value
        double             firstValue = hilo[0];
        double             botval     = hilo[0];
        double             topval     = hilo[hilo.length - 1];
        int                numSteps   = hilo.length;
        DisplayConventions dc         = getDisplayConventions();

        for (int k = 0; k < hilo.length; k++) {
            double val = hilo[k];
            xLabels.put(new Double(val),
                        dc.formatLatLonCardinal(val, 1 - averageDim));
        }
        Font f    = hovmollerView.getDisplayListFont();
        int  size = (f == null)
                    ? 12
                    : f.getSize();
        if ((f != null) && f.getName().equals(FontSelector.DEFAULT_NAME)) {
            f = null;
        }
        AxisScale xScale = hovmollerDisplay.getXAxisScale();
        xScale.setFont(f);
        xScale.setLabelSize(size);
        xScale.setSnapToBox(true);
        xScale.setTickBase(botval);
        double averageTickSpacing = (topval - botval)
                                    / (double) (numSteps - 1);
        xScale.setMajorTickSpacing(averageTickSpacing);
        xScale.setMinorTickSpacing(averageTickSpacing);
        xScale.setLabelTable(xLabels);

    }

    /**
     * make widgets for time format and latest data time on bottom of y axis.
     *
     * @param controlWidgets to fill
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);

        // make check box for latest data time on bottom of y axis
        JCheckBox toggle = new JCheckBox("Latest Time at Bottom",
                                         reverseTime);
        toggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reverseTime = ((JCheckBox) e.getSource()).isSelected();
                try {
                    setTimeAxisValues();
                } catch (VisADException ve) {
                    userMessage("couldn't set time order");
                }
            }
        });
        final JTextField formatField = new JTextField(getTimeFormat());
        formatField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setTimeFormat(formatField.getText());
            }
        });

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Time: "),
                GuiUtils.centerRight(
                    GuiUtils.leftCenter(
                        GuiUtils.rLabel("Label Format: "),
                        formatField), toggle)));

    }

    /**
     * If the color  is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_COLOR
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void applyColor() throws VisADException, RemoteException {
        displayListUsesColor = false;
        super.applyColor();
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
            JMenu hovMenu = hovmollerView.makeViewMenu();
            hovMenu.setText("Hovmoller View");
            items.add(hovMenu);
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
        addDisplayable(colorScale, hovmollerView, FLAG_COLORTABLE);
        colorScales.add(colorScale);

    }

    /**
     * Add tabs to the properties dialog.
     *
     * @param jtp  the JTabbedPane to add to
     */
    public void addPropertiesComponents(JTabbedPane jtp) {
        super.addPropertiesComponents(jtp);

        if (hovmollerView != null) {
            jtp.add("Hovmoller View", hovmollerView.getPropertiesComponent());
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
        if (hovmollerView != null) {
            return hovmollerView.applyProperties();
        }
        return true;
    }

    /**
     * Apply the preferences.  Used to pick up the date format changes.
     */
    public void applyPreferences() {
        super.applyPreferences();
        try {
            setTimeAxisValues();
        } catch (Exception exc) {
            logException("applyPreferences", exc);
        }
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
     * Set the averaging dimension
     *
     * @param dim  the dimension (LAT_DIM, LON_DIM)
     */
    public void setAverageDimension(int dim) {
        averageDim = dim;
    }

    /**
     * Get the averaging dimension
     *
     * @return  the averaging dimension
     */
    public int getAverageDimension() {
        return averageDim;
    }

    /**
     * Set the time format
     *
     * @param format  time format
     */
    public void setTimeFormat(String format) {
        timeFormat = format;
        if (getHaveInitialized()) {
            try {
                setTimeAxisValues();
            } catch (VisADException ve) {
                logException("Unable to set time format", ve);
            }
        }
    }

    /**
     * Get the time format
     *
     * @return  the time format
     */
    public String getTimeFormat() {
        return timeFormat;
    }

    /**
     * Set reverse times
     *
     * @param yesorno  true to go oldest to youngest
     */
    public void setReverseTime(boolean yesorno) {
        reverseTime = yesorno;
        if (getHaveInitialized()) {
            try {
                setTimeAxisValues();
            } catch (VisADException ve) {
                logException("Unable to set time format", ve);
            }
        }
    }

    /**
     * Get reverse times property
     *
     * @return the reverse times property
     */
    public boolean getReverseTime() {
        return reverseTime;
    }

    /**
     *  Set the HovmollerView property.
     *
     *  @param value The new value for HovmollerView
     */
    public void setHovmollerView(HovmollerViewManager value) {
        hovmollerView = value;
    }

    /**
     *  Get the HovmollerView property.
     *
     *  @return The HovmollerView
     */
    public HovmollerViewManager getHovmollerView() {
        return hovmollerView;
    }



}

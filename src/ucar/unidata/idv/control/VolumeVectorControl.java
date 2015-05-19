package ucar.unidata.idv.control;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.visad.Util;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.WindBarbDisplayable;
import ucar.visad.display.ZSelector;
import visad.*;
import visad.georef.MapProjection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by yuanho on 4/5/15.
 */
public class VolumeVectorControl extends GridDisplayControl {
    /**
     * the display for the volume renderer
     */
    FlowDisplayable myDisplay;

    /** data choice for the data */
    protected DataChoice datachoice;

    /** a component to change the barb size */
    ValueSliderWidget barbSizeWidget;

    /** vector/barb size component */
    JComponent sizeComponent;

    /** a component to change the traj size */
    ValueSliderWidget trajLengthWidget;

    /** vector/traj length component */
    JComponent trajLengthComponent;

    /** a component to change the cvector size */
    ValueSliderWidget cvectorLengthWidget;

    /** cvector length component */
    JComponent cvectorLengthComponent;

    /** a component to change the cvector arrow head size */
    ValueSliderWidget cvectorAHLengthWidget;

    /** a component to change the skip */
    ValueSliderWidget skipFactorWidget;

    /** a component to change the streamline density */
    JComponent densityComponent;

    /** streamline density slider */
    JSlider densitySlider;

    /** a label listing the range of the data */
    JLabel flowRangeLabel;

    /** the density label */
    private JLabel densityLabel;

    /** streamlines button */
    private JRadioButton streamlinesBtn;

    /** vector/barb button */
    private JRadioButton vectorBtn;

    /** trajectory button */
    private JRadioButton trajectoryBtn;

    /** flag for streamlines */
    boolean isStreamlines = false;

    /** _more_ */
    boolean isVectors = true;

    /** _more_ */
    boolean isTrajectories = false;

    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** flag for wind barbs */
    boolean isThreeComponents = true;

    /** autoscale */
    boolean autoSize = false;

    /** arrow */
    boolean arrowHead = false;

    /** a scale factor */
    protected final float scaleFactor = 0.02f;

    /** a scale value */
    float flowScaleValue = 4.0f;

    /** a traj offset value */
    float vectorLengthValue = 2.0f;

    /** a traj offset value */
    float arrowHeadSizeValue = 1.0f;

    /** a traj offset value */
    float trajOffsetValue = 4.0f;

    /** streamline density value */
    float streamlineDensity = 1.0f;

    /** slider components */
    private JComponent[] widthSliderComps;

    /** Range for flow scale */
    private Range flowRange;

    /** the range dialog */
    RangeDialog rangeDialog;

    /** _more_ */
    private boolean useSpeedForColor = false;

    /** _more_ */
    private int colorIndex = -1;



    /**
     * Default constructor; does nothing.
     */
    public VolumeVectorControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH | FLAG_SMOOTHING);
    }


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return true if successful
     *
     * @throws java.rmi.RemoteException  Java RMI error
     * @throws visad.VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        datachoice = dataChoice;
        if ( !isDisplay3D()) {
            LogUtil.userMessage(log_, "Can't render volume in 2D display");
            return false;
        }
        myDisplay = (FlowDisplayable)createPlanDisplay();


        myDisplay.setPointSize(getPointSize());
        addDisplayable(myDisplay, getAttributeFlags());

        //Now, set the data. Return false if it fails.
        if ( !setData(dataChoice)) {
            return false;
        }


        //Now set up the flags and add the displayable
        return true;
    }

    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        FlowDisplayable planDisplay;
        if (isWindBarbs) {
            planDisplay =
                    new WindBarbDisplayable("FlowPlanViewControl_windbarbs_"
                            + ((datachoice != null)
                            ? datachoice.toString()
                            : ""), null);
        } else {
            planDisplay = new FlowDisplayable("FlowPlanViewControl_vectors_"
                    + ((datachoice != null)
                    ? datachoice.toString()
                    : ""), null);

            planDisplay.set3DFlow(true);
        }
        planDisplay.setStreamlinesEnabled(isStreamlines);
        planDisplay.setStreamlineDensity(streamlineDensity);
        planDisplay.setAutoScale(autoSize);

        planDisplay.setTrojectoriesEnabled(isTrajectories, arrowHeadSizeValue, false);

        //addAttributedDisplayable(planDisplay, FLAG_SKIPFACTOR);
        addAttributedDisplayable(planDisplay);
        return planDisplay;
    }

    FlowDisplayable getGridDisplay() {
        return  myDisplay;
    }
    /**
     * Add in any special control widgets to the current list of widgets.
     *
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        //super.getControlWidgets(controlWidgets);

        skipFactorWidget = new ValueSliderWidget(this, 0, 10, "skipValue",
                getSkipWidgetLabel());
        addRemovable(skipFactorWidget);

        barbSizeWidget = new ValueSliderWidget(this, 1, 21, "flowScale",
                "Size");
        addRemovable(barbSizeWidget);

        JCheckBox autoSizeCbx = new JCheckBox("Autosize", autoSize);
        JCheckBox arrowCbx    = new JCheckBox("Arrow", arrowHead);
        autoSizeCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoSize = ((JCheckBox) e.getSource()).isSelected();
                getGridDisplay().setAutoScale(autoSize);
            }
        });

        arrowCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                arrowHead = ((JCheckBox) e.getSource()).isSelected();
                if (arrowHead) {
                    getGridDisplay().setArrowHead(arrowHead);
                } else {
                    getGridDisplay().setArrowHead(arrowHead);
                }
                getGridDisplay().resetTrojectories();
            }
        });

        sizeComponent = GuiUtils.hbox(GuiUtils.rLabel("Size: "),
                barbSizeWidget.getContents(false),
                autoSizeCbx);
        if ( getIsThreeComponents()) {

            vectorBtn      = new JRadioButton((isWindBarbs
                    ? "Wind Barbs:"
                    : "Vectors:"), isVectors);
            trajLengthWidget = new ValueSliderWidget(this, 1, 21,
                    "trajOffset", "LengthOffset");
            trajLengthComponent =
                    GuiUtils.hbox(GuiUtils.rLabel("Length Offset: "),
                            trajLengthWidget.getContents(false), arrowCbx);

            trajectoryBtn = new JRadioButton("Trajectories:", isTrajectories);

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JRadioButton source = (JRadioButton) e.getSource();
                    if (source == trajectoryBtn) {
                        isTrajectories = true;
                        isVectors      = false;
                    } else {
                        isVectors      = true;
                        isTrajectories = false;
                    }
                    setStreamlines();
                }
            };

            vectorBtn.addActionListener(listener);
            trajectoryBtn.addActionListener(listener);
            GuiUtils.buttonGroup(vectorBtn, trajectoryBtn);
            densityLabel     = GuiUtils.rLabel("Density: ");

            enableDensityComponents();
            Insets spacer = new Insets(0, 30, 0, 0);
            JComponent rightComp =
                    GuiUtils.vbox(
                            GuiUtils.left(
                                    GuiUtils.vbox(
                                            vectorBtn,
                                            GuiUtils.inset(
                                                    sizeComponent, spacer))), GuiUtils.left(
                                    GuiUtils.vbox(
                                            trajectoryBtn,
                                            GuiUtils.inset(
                                                    trajLengthComponent,
                                                    spacer))));
            JLabel showLabel = GuiUtils.rLabel("Show:");
            showLabel.setVerticalTextPosition(JLabel.TOP);
            controlWidgets.add(
                    new WrapperWidget(
                            this,
                            GuiUtils.top(
                                    GuiUtils.inset(
                                            showLabel,
                                            new Insets(10, 0, 0, 0))), GuiUtils.left(
                            GuiUtils.top(rightComp))));


        }

        controlWidgets.add(
                new WrapperWidget(
                        this, GuiUtils.rLabel("Skip:"),
                        GuiUtils.left(
                                GuiUtils.hbox(
                                        GuiUtils.rLabel("XY:  "),
                                        skipFactorWidget.getContents(false)))));

        enableTrajLengthBox();

       // super.getControlWidgets(controlWidgets);

    }
    public boolean getIsThreeComponents() {
        return isThreeComponents;
    }

    public void setStreamlines() {
        //isStreamlines = v;
        if (getGridDisplay() != null) {
            getGridDisplay().setStreamlinesEnabled(isStreamlines);

            getGridDisplay().setTrojectoriesEnabled(isTrajectories, arrowHeadSizeValue,
                        false);

            enableDensityComponents();
            enableTrajLengthBox();
        }
        if (streamlinesBtn != null) {
            streamlinesBtn.setSelected(isStreamlines);
            vectorBtn.setSelected(isVectors);
            trajectoryBtn.setSelected(isTrajectories);
        }

    }
    /**
     * enable the barb size box
     */
    private void enableTrajLengthBox() {
        if (trajLengthComponent != null) {
            GuiUtils.enableTree(trajLengthComponent, isTrajectories);
        }
    }

    /**
     * enable the density slider components
     */
    private void enableDensityComponents() {
        if (densityComponent != null) {
            GuiUtils.enableTree(densityComponent, isStreamlines);
        }
        if (densityLabel != null) {
            GuiUtils.enableTree(densityLabel, isStreamlines);
        }
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setPointSize(float value) {
        super.setPointSize(value);
        if (myDisplay != null) {
            try {
                myDisplay.setPointSize(getPointSize());
            } catch (Exception e) {
                logException("Setting point size", e);
            }
        }
    }



    /**
     *  Use the value of the skip factor to subset the data.
     */
    protected void applySkipFactor() {
        try {
            showWaitCursor();
            loadVolumeData();
        } catch (Exception exc) {
            logException("loading volume data", exc);
        } finally {
            showNormalCursor();
        }

    }


    /**
     * Set the data in this control.
     *
     * @param choice  data description
     *
     * @return true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.setData(choice) || (getNavigatedDisplay() == null)) {
            return false;
        }
        loadVolumeData();
        return true;
    }



    /**
     * Make the gui. Align it left
     *
     * @return The gui
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        return GuiUtils.left(doMakeWidgetComponent());
    }



    /**
     * Load the volume data to the display
     *
     * @throws RemoteException   problem loading remote data
     * @throws VisADException    problem loading the data
     */
    private void loadVolumeData() throws VisADException, RemoteException {
        Trace.call1("VRC.loadVolumeData");
        FieldImpl grid    = getGridDataInstance().getGrid();
        FieldImpl newGrid = grid;

        if (getSkipValue() > 0) {
            grid    = GridUtil.subset(grid, getSkipValue() + 1);
            newGrid = grid;
        }


        Trace.call1("VRC.loadVolumeData.loadData");
        myDisplay.loadData(newGrid);
        Trace.call2("VRC.loadVolumeData.loadData");
        Trace.call2("loadVolumeData");
    }

    /**
     * Make a grid with a Linear3DSet for the volume rendering
     *
     * @param grid grid to transform
     * @param cs   coordinate system to transform to XYZ
     *
     * @return transformed grid
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   problem creating grid
     */
    private FieldImpl makeLinearGrid(FieldImpl grid, CoordinateSystem cs)
            throws VisADException, RemoteException {

        Trace.call1("VRC.makeLinearGrid");
        GriddedSet domainSet   = (GriddedSet) GridUtil.getSpatialDomain(grid);
        SampledSet ss          = null;
        boolean    latLonOrder = GridUtil.isLatLonOrder(domainSet);
        //System.out.println("grid is latLonOrder " + latLonOrder);
        Trace.call1("VRC.convertDomain");
        if (latLonOrder) {
            ss = Util.convertDomain(domainSet,
                    RealTupleType.LatitudeLongitudeAltitude,
                    null);
        } else {
            ss = Util.convertDomain(domainSet,
                    RealTupleType.SpatialEarth3DTuple, null);
        }
        Trace.call2("VRC.convertDomain");
        float[][] refVals = ss.getSamples(true);
        MapProjectionDisplay mpd =
                (MapProjectionDisplay) getNavigatedDisplay();
        MapProjection mp             = mpd.getMapProjection();
        boolean       mapLatLonOrder = mp.isLatLonOrder();
        //System.out.println("map is latLonOrder " + mapLatLonOrder);
        float[][] newVals = (latLonOrder)
                ? refVals
                : new float[][] {
                refVals[1], refVals[0], refVals[2]
        };
        Trace.call1("VRC.toRef");
        newVals = cs.toReference(newVals);
        Trace.call2("VRC.toRef");
        Trace.call1("VRC.scaleVerticalValues");
        newVals[2] = mpd.scaleVerticalValues(newVals[2]);
        Trace.call2("VRC.scaleVerticalValues");
        int[] lengths = domainSet.getLengths();
        //Misc.printArray("lengths",lengths);
        GriddedSet xyzSet =
                GriddedSet.create(RealTupleType.SpatialCartesian3DTuple, newVals,
                        domainSet.getLengths(),
                        (CoordinateSystem) null, (Unit[]) null,
                        (ErrorEstimate[]) null, false, true);
        Trace.call1("VRC.setSpatialDomain");
        FieldImpl newGrid = GridUtil.setSpatialDomain(grid, xyzSet);  //, true);
        Trace.call2("VRC.setSpatialDomain");
        float[] lows  = xyzSet.getLow();
        float[] highs = xyzSet.getHi();
        //Misc.printArray("lows",lows);
        //Misc.printArray("highs",highs);
        Linear3DSet volumeXYZ =
                new Linear3DSet(RealTupleType.SpatialCartesian3DTuple, lows[0],
                        highs[0], lengths[0], lows[1], highs[1],
                        lengths[1], lows[2], highs[2], lengths[2]);
        // System.out.println(volumeXYZ);
        Trace.call1("VRC.resampleGrid");
        newGrid = GridUtil.resampleGrid(newGrid, volumeXYZ);
        Trace.call2("VRC.resampleGrid");
        Trace.call2("VRC.makeLinearGrid");
        return newGrid;
    }

    /**
     * Method to call if projection changes.  Subclasses that
     * are worried about such events should implement this.
     */
    public void projectionChanged() {
        //System.out.println("projection changed");
        try {
            loadVolumeData();
        } catch (Exception exc) {
            logException("loading volume data", exc);
        }
        super.projectionChanged();
    }



    /**
     * Is this a raster display
     *
     * @return true
     */
    public boolean getIsRaster() {
        return false;
    }


    /**
     * Get the flow scale.
     * Used by XML persistence
     *
     * @return  the flow scale for this control
     */
    public float getFlowScale() {
        return flowScaleValue;
    }

    /**
     * Set the flow scale.
     * Used by XML persistence
     *
     * @param f   new flow scale
     */
    public void setFlowScale(float f) {
        flowScaleValue = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setFlowScale(getDisplayScale() * scaleFactor
                        * flowScaleValue);
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

    }
    /**
     * _more_
     *
     * @return _more_
     */
    public float getTrajOffset() {
        return trajOffsetValue;
    }
    public void setTrajOffset(float f) {
        trajOffsetValue = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setTrajOffset(trajOffsetValue);
                getGridDisplay().resetTrojectories();
                if (arrowHead) {
                    getGridDisplay().setArrowHead(arrowHead);
                } else {
                    getGridDisplay().setArrowHead(arrowHead);
                }
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

        if (trajLengthWidget != null) {
            trajLengthWidget.setValue(f);
        }
    }


}

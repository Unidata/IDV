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


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.HodographViewManager;
import ucar.unidata.idv.SoundingViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.view.sounding.AerologicalCellNetwork;
import ucar.unidata.view.sounding.AerologicalCoordinateSystem;
import ucar.unidata.view.sounding.AerologicalDisplay;
import ucar.unidata.view.sounding.AerologicalDisplayConstants;
import ucar.unidata.view.sounding.AerologicalReadoutTable;
import ucar.unidata.view.sounding.CellNetwork;
import ucar.unidata.view.sounding.ComputeCell;
import ucar.unidata.view.sounding.Hodograph3DDisplay;
import ucar.unidata.view.sounding.ParcelMode;
import ucar.unidata.view.sounding.PseudoAdiabaticDisplayable;
import ucar.unidata.view.sounding.RealEvaluatorCell;
import ucar.unidata.view.sounding.Sounding;

import ucar.visad.display.Displayable;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.DisplayableDataRef;
import ucar.visad.display.LineDrawing;
import ucar.visad.functiontypes.AirTemperatureProfile;
import ucar.visad.functiontypes.CartesianHorizontalWindOfPressure;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.AirTemperature;
import ucar.visad.quantities.CartesianHorizontalWind;
import ucar.visad.quantities.PolarHorizontalWind;

import visad.ActionImpl;
import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.ErrorEstimate;
import visad.Field;
import visad.FieldImpl;
import visad.FlowControl;
import visad.GraphicsModeControl;
import visad.Gridded1DSet;
import visad.Linear1DSet;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Unit;
import visad.VisADException;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.util.DataUtility;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;


/**
 * Abstract class for displaying an aerological diagram (eg, Skew-T log p) of an
 * atmospheric sounding.
 *
 * @author IDV Development Team
 */
public abstract class AerologicalSoundingControl extends DisplayControlImpl implements AerologicalDisplayConstants {

    /** The view manager for this control */
    protected SoundingViewManager soundingView;

    /** The view manager for this control */
    protected HodographViewManager hodoView;

    /**
     * The Skew-T log p display.
     */
    protected AerologicalDisplay aeroDisplay;  // accessed in subclasses

    /**
     * The 3D hodograph display.
     */
    protected Hodograph3DDisplay hodoDisplay;  // accessed in subclasses

    /**
     * The sounding table
     */
    protected SoundingTable soundingTable;

    /** label for the location */
    protected JLabel headerLabel;

    /** readout panel */
    private JPanel readoutPanel;

    /** show U and V winds checkbox */
    private JCheckBox showUVCbx;

    /** the parcel path */
    private PseudoAdiabaticDisplayable parcelPath;

    /** the parcel virtual temperature path */
    private DisplayableDataRef parVirtTempPath;

    /** the environmental virtual temperature path */
    private DisplayableDataRef envVirtTempPath;

    /** flag for whehter the trajectory is active */
    private boolean trajectoryActive = false;

    /** flag for whehter the virtual temperature is active */
    private boolean virtTempActive = false;

    /** parcel mode index */
    private int parcelModeIndex = 0;

    /** the evaluation network */
    private CellNetwork proEvalNet;

    /** the cell network */
    private AerologicalCellNetwork aeroCellNet;

    /** a reference for the temperature profile */
    private DataReferenceImpl tempProRef;

    /** a reference for the dewpoint profile */
    private DataReferenceImpl dewProRef;

    /** a reference for the wind profile */
    private DataReferenceImpl windProRef;

    /** the temperature profiles */
    private Field[] tempProfiles;

    /** the dewpoint profiles */
    private Field[] dewProfiles;

    /** the wind profiles */
    private Field[] windProfiles;

    /** the current index */
    private int currIndex = 0;

    /** flag for visibility of the spatial locations */
    private boolean spatialLociVisible;

    /** the displayable for the spatial locations */
    private Displayable spatialLoci;

    /** display type */
    private String displayType = SKEWT_DISPLAY;

    /** wind sampling modes */
    private static float[] MANDATORY_LEVEL_VALUES = {
        1000.f, 925.f, 850.f, 700.f, 500.f, 400.f, 300.f, 250.f, 200.f, 150.f,
        100.f
    };

    /** Identifier for all wind levels */
    private static final String ALL_LEVELS = "All";

    /** Identifier for mandatory levels */
    private static final String MANDATORY_LEVELS = "Mandatory Levels";

    /** default wind sampling mode */
    private String windBarbSpacing = ALL_LEVELS;

    /** cursor pressure data reference */
    private DataReferenceImpl cursorPresRef;

    /** point pressure data reference */
    private DataReferenceImpl pointerPresRef;

    /** cursor temperature data reference */
    private DataReferenceImpl cursorTempRef;

    /** cursor wind data reference */
    private DataReferenceImpl cursorWindRef;

    /** pointer temperature data reference */
    private DataReferenceImpl pointerTempRef;

    /** cursor pressure data reference */
    private AerologicalReadoutTable readoutTable;

    /** location */
    private LatLonPoint location;

    /** winds or not? */
    private boolean haveWinds = false;

    /** dewpoint or not? */
    private boolean haveDewpoints = true;

    /** winds as U and V? */
    private boolean haveUAndVWinds = false;

    /** show winds as U and V? */
    private boolean showUAndVWinds = false;

    /** tabbed pane for skewt, hodo and table */
    private JTabbedPane viewTabs;

    /** _more_ */
    private List<MyActionImpl> actions = new ArrayList<MyActionImpl>();

    /**
     *
     * Class ParcelModeInfo
     */
    private static class ParcelModeInfo {

        /** the label for the info */
        private final String label;

        /** the parcel mode */
        private final ParcelMode mode;

        /**
         * Create a new ParcelModeInfo
         *
         * @param label   the label
         * @param mode    the mode
         */
        private ParcelModeInfo(String label, ParcelMode mode) {
            this.label = label;
            this.mode  = mode;
        }

        /**
         * String representation of this object
         *
         * @return String representation of this object
         */
        public String toString() {
            return label;
        }
    }

    /** array of parcel mode infos */
    private static ParcelModeInfo[] parcelModeInfos = { new ParcelModeInfo(
                                                          "Bottom of Sounding",
                                                          ParcelMode.BOTTOM),
            new ParcelModeInfo("Below Cursor", ParcelMode.LAYER),
            new ParcelModeInfo("At Cursor Pressure", ParcelMode.PRESSURE),
            new ParcelModeInfo("At Cursor (Press,Temp)", ParcelMode.POINT), };

    /**
     * Constructs from nothing.
     *
     * @param lociVisible      Whether or not the spatial loci are initially
     *                         visible.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    AerologicalSoundingControl(boolean lociVisible)
            throws VisADException, RemoteException {
        setAttributeFlags(FLAG_COLOR);
        spatialLociVisible = lociVisible;
    }

    /**
     * Initializes this instance according to profile data references.
     *
     * @return                 True if and only if this instance was initialized
     *                         OK.
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException couldn't create a remote object needed
     */
    boolean init() throws VisADException, RemoteException {

        if (soundingView != null) {
            displayType = soundingView.getChartType();
        }
        aeroDisplay = AerologicalDisplay.getInstance(displayType,
                getGraphicsConfiguration(true, false));
        hodoDisplay = new Hodograph3DDisplay();
        if (soundingView != null) {
            //If the ViewManager is non-null it means we have been unpersisted.
            //If so, we initialie the VM with the IDV
            soundingView.setSoundingDisplay(aeroDisplay);
            soundingView.initAfterUnPersistence(getIdv());
        } else {
            soundingView = new SoundingViewManager(
                getViewContext(), aeroDisplay,
                new ViewDescriptor("SoundingView"),
                "showControlLegend=false;wireframe=false;aniReadout=false;chartType="
                + displayType);
        }
        if (hodoView != null) {
            //If the ViewManager is non-null it means we have been unpersisted.
            //If so, we initialie the VM with the IDV
            hodoView.setHodographDisplay(hodoDisplay);
            hodoView.initAfterUnPersistence(getIdv());
        } else {
            hodoView = new HodographViewManager(
                getViewContext(), hodoDisplay,
                new ViewDescriptor("SoundingView"),
                "showControlLegend=false;wireframe=false;aniReadout=false;");
        }

        //TODO: For now don't do this because it screws up the image dumping.
        //If and when we put this back we need to not destroy the
        //VM in our doRemove method
        addViewManager(soundingView);
        addViewManager(hodoView);
        soundingTable = new SoundingTable(this);
        soundingTable.setDefaultRenderer(Number.class,
                                         new TableNumberCellRenderer());
        soundingTable.setShowUVComps(showUAndVWinds);
        // aeroDisplay.setPointMode(true);  // for debugging
        headerLabel = new JLabel(" ", JLabel.LEFT);
        tempProRef  = new DataReferenceImpl("TemperatureProfile");
        dewProRef   = new DataReferenceImpl("DewPointProfile");
        windProRef  = new DataReferenceImpl("WindProfile");

        dewProRef.setData(DewPointProfile.instance().missingData());
        tempProRef.setData(AirTemperatureProfile.instance().missingData());
        windProRef.setData(
            CartesianHorizontalWindOfPressure.instance().missingData());




        cursorPresRef  = new DataReferenceImpl("CursorPressure");
        pointerPresRef = new DataReferenceImpl("PointerPressure");
        cursorTempRef  = new DataReferenceImpl("CursorTemperature");
        cursorWindRef  = new DataReferenceImpl("CursorWind");
        pointerTempRef = new DataReferenceImpl("pointerTemperature");


        DataReferenceImpl minPresRef =
            new DataReferenceImpl("MinimumPressure");

        minPresRef.setData(aeroDisplay.getMinimumPressure());
        cursorPresRef.setData(AirPressure.getRealType().missingData());
        pointerPresRef.setData(AirPressure.getRealType().missingData());
        cursorTempRef.setData(AirTemperature.getRealType().missingData());
        cursorWindRef.setData(
            CartesianHorizontalWind.getRealTupleType().missingData());
        pointerTempRef.setData(AirTemperature.getRealType().missingData());

        aeroCellNet = new AerologicalCellNetwork(tempProRef, dewProRef,
                cursorPresRef, cursorTempRef, minPresRef, windProRef);

        setParcelMode(parcelModeIndex);

        parcelPath =
            new PseudoAdiabaticDisplayable(aeroCellNet.getDryTrajectoryRef(),
                                           aeroCellNet.getWetTrajectoryRef());

        parcelPath.setLineWidth(2);
        parcelPath.setColor(java.awt.Color.pink);
        addDisplayable(parcelPath, soundingView);

        parVirtTempPath = new DisplayableDataRef(
            aeroCellNet.getParcelVirtualTemperatureProfileRef());

        parVirtTempPath.setLineWidth(2);
        parVirtTempPath.setLineStyle(GraphicsModeControl.DASH_STYLE);
        parVirtTempPath.setColor(java.awt.Color.pink);
        addDisplayable(parVirtTempPath, soundingView);

        envVirtTempPath = new DisplayableDataRef(
            aeroCellNet.getEnvironmentVirtualTemperatureProfileRef());

        envVirtTempPath.setLineWidth(2);
        envVirtTempPath.setLineStyle(GraphicsModeControl.DASH_STYLE);
        envVirtTempPath.setColor(java.awt.Color.red);
        addDisplayable(envVirtTempPath, soundingView);

        readoutTable = new AerologicalReadoutTable();


        aeroDisplay.addPropertyChangeListener(this);


        actions.add(new MyActionImpl("ProfileTemperature", readoutTable,
                                     aeroCellNet.getProfileTemperatureRef()) {
            public void doAction() {
                try {
                    readoutTable.setProfileTemperature(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });

        actions.add(new MyActionImpl("ProfileDewPoint", readoutTable,
                                     aeroCellNet.getProfileDewPointRef()) {
            public void doAction() {
                try {
                    readoutTable.setProfileDewPoint(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        actions.add(new MyActionImpl("ProfileMixingRatio", readoutTable,
                                     aeroCellNet.getProfileMixingRatioRef()) {
            public void doAction() {
                try {
                    readoutTable.setProfileMixingRatio(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });

        /*

          actions.add(new MyActionImpl("ProfileWind", readoutTable, aeroCellNet.getProfileWindRef()) {
            public void doAction() {

                try {
                    RealTuple spdDir =
                        PolarHorizontalWind.newRealTuple(
                            (RealTuple) proWindAtPointPresRef.getReal());
                    readoutTable.setProfileWindSpeed((Real)spdDir.getComponent(0));
                    readoutTable.setProfileWindDirection((Real)spdDir.getComponent(1));
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });
        */

        actions.add(new MyActionImpl("LclPressure", readoutTable,
                                     aeroCellNet.getLclPressureRef()) {
            public void doAction() {
                try {
                    readoutTable.setLclPressure(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        actions.add(new MyActionImpl("LclTemperature", readoutTable,
                                     aeroCellNet.getLclTemperatureRef()) {
            public void doAction() {
                try {
                    readoutTable.setLclTemperature(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        actions.add(new MyActionImpl("CAPE", readoutTable,
                                     aeroCellNet.getCapeRef()) {
            public void doAction() {
                try {
                    readoutTable.setCape(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });

        actions.add(new MyActionImpl("LFC", readoutTable,
                                     aeroCellNet.getLfcRef()) {
            public void doAction() {
                try {
                    readoutTable.setLfc(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        actions.add(new MyActionImpl("LfcTemperature", readoutTable,
                                     aeroCellNet.getLfcTemperatureRef()) {
            public void doAction() {
                try {
                    readoutTable.setLfcTemperature(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        actions.add(new MyActionImpl("LNB", readoutTable,
                                     aeroCellNet.getLnbRef()) {
            public void doAction() {
                try {
                    readoutTable.setLnb(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        actions.add(new MyActionImpl("LnbTemperature", readoutTable,
                                     aeroCellNet.getLnbTemperatureRef()) {
            public void doAction() {
                try {
                    readoutTable.setLnbTemperature(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });

        actions.add(new MyActionImpl("CIN", readoutTable,
                                     aeroCellNet.getCinRef()) {
            public void doAction() {
                try {
                    readoutTable.setCin(getReal());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        /*
         * Configure a computational network for evaluating the temperature,
         * dew-point, and wind profiles at the mouse-pointer pressure.
         */
        {
            proEvalNet = new CellNetwork();

            ComputeCell proTempAtPointPres =
                new RealEvaluatorCell(tempProRef, pointerPresRef,
                                      cursorTempRef.getData());

            proEvalNet.add(proTempAtPointPres);

            ComputeCell proDewAtPointPres = new RealEvaluatorCell(dewProRef,
                                                pointerPresRef,
                                                cursorTempRef.getData());

            proEvalNet.add(proDewAtPointPres);

            ComputeCell proWindAtPointPres =
                new RealEvaluatorCell(windProRef, pointerPresRef,
                                      cursorWindRef.getData());

            proEvalNet.add(proWindAtPointPres);
            proEvalNet.configure();



            actions.add(new MyActionImpl("PointerProfileTemperature",
                                         readoutTable,
                                         proTempAtPointPres.getOutputRef()) {
                public void doAction() {
                    try {
                        readoutTable.setProfileTemperature(getReal());
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            });

            actions.add(new MyActionImpl("PointerProfileDewPoint",
                                         readoutTable,
                                         proDewAtPointPres.getOutputRef()) {
                public void doAction() {
                    try {
                        readoutTable.setProfileDewPoint(getReal());
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            });





            actions.add(new MyActionImpl("PointerProfileWind", readoutTable,
                                         proWindAtPointPres.getOutputRef()) {
                public void doAction() {
                    try {
                        RealTuple spdDir =
                            (RealTuple) getDataReference().getData();
                        if ( !(PolarHorizontalWind.getRealTupleType().equals(
                                (RealTupleType) spdDir.getType()))) {
                            spdDir = PolarHorizontalWind.newRealTuple(spdDir);
                        }
                        readoutTable.setProfileWindSpeed(
                            (Real) spdDir.getComponent(0));
                        readoutTable.setProfileWindDirection(
                            (Real) spdDir.getComponent(1));
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            });
        }

        readoutPanel = new JPanel();

        readoutPanel.setLayout(new BorderLayout());
        readoutPanel.add(readoutTable);
        aeroDisplay.draw();
        hodoDisplay.draw();
        setTrajectoryActive(trajectoryActive);
        setVirtTempActive(virtTempActive);

        return true;
    }

    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        super.getSaveMenuItems(items, forMenuBar);
        items.add(GuiUtils.makeMenuItem("Export Sounding Table to File...",
                                        this, "exportTableToCsv"));
    }

    /**
     * Export the sounding table to csv, public by implementation,
     * don't call directly.
     */
    public void exportTableToCsv() {
        if (soundingTable == null) {
            return;
        }
        String header = "";
        if (headerLabel != null) {
            header = headerLabel.getText();
        }
        GuiUtils.exportAsCsv(header, soundingTable.getModel(), true);
    }

    /**
     * Handle property change
     *
     * @param event The event
     */
    public void propertyChange(PropertyChangeEvent event) {
        try {
            if (event.getPropertyName().equals(
                    AerologicalDisplay.CURSOR_PRESSURE)) {
                Real pressure = (Real) event.getNewValue();
                cursorPresRef.setData(pressure);
                readoutTable.setPressure(pressure);
            } else if (event.getPropertyName().equals(
                    AerologicalDisplay.POINTER_PRESSURE)) {
                Real pressure = (Real) event.getNewValue();
                pointerPresRef.setData(pressure);
                readoutTable.setPressure(pressure);
            } else if (event.getPropertyName().equals(
                    AerologicalDisplay.CURSOR_TEMPERATURE)) {
                Real temp = (Real) event.getNewValue();
                cursorTempRef.setData(temp);
                readoutTable.setBackgroundTemperature(temp);
            } else if (event.getPropertyName().equals(
                    AerologicalDisplay.POINTER_TEMPERATURE)) {
                Real temp = (Real) event.getNewValue();
                readoutTable.setBackgroundTemperature(temp);
            } else {
                super.propertyChange(event);
            }
        } catch (Exception e) {
            logException(e);
        }
    }

    /**
     * Add tabs to the properties dialog.
     *
     * @param jtp  the JTabbedPane to add to
     */
    public void addPropertiesComponents(JTabbedPane jtp) {
        super.addPropertiesComponents(jtp);

        if (soundingView != null) {
            jtp.add("Sounding Chart", soundingView.getPropertiesComponent());
        }

        if (hodoView != null) {
            jtp.add("Hodograph", hodoView.getPropertiesComponent());
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
        if (soundingView != null) {
            return soundingView.applyProperties();
        }
        return true;
    }

    /**
     * Remove this control. Call the parent  class doRemove and clears
     * references to gridLocs, etc.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void doRemove() throws VisADException, RemoteException {
        super.doRemove();
        /* Don't need to do this since we have the base class handle the view manager
        if (soundingView != null) {
            soundingView.destroy();
            soundingView = null;
        }
        */

        if (actions != null) {
            for (MyActionImpl action : actions) {
                action.doRemove();
            }
            actions = null;
        }

        soundingView    = null;
        aeroDisplay     = null;
        hodoView        = null;
        hodoDisplay     = null;
        soundingTable   = null;
        headerLabel     = null;
        readoutPanel    = null;
        parcelPath      = null;
        parVirtTempPath = null;
        envVirtTempPath = null;
        proEvalNet      = null;
        aeroCellNet     = null;
        tempProRef      = null;
        dewProRef       = null;
        windProRef      = null;
        tempProfiles    = null;
        dewProfiles     = null;
        windProfiles    = null;
    }


    /**
     * Set the spatial location displayable
     *
     * @param loci  Displayable for spatial locations
     */
    protected void setSpatialLoci(Displayable loci) {
        spatialLoci = loci;
        try {
            setSpatialLociVisible(spatialLociVisible);
        } catch (Exception exc) {}
    }




    /**
     * Sets the visibility of the spatial loci in the main, 3D window.
     *
     * @param visible           If true, then the loci will be rendered visible;
     *                          otherwise, they will be rendered invisible.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void setSpatialLociVisible(boolean visible)
            throws VisADException, RemoteException {
        spatialLociVisible = visible;
        if (spatialLoci != null) {
            spatialLoci.setVisible(getDisplayVisibility()
                                   && spatialLociVisible);
        }
    }

    /**
     * Override base class method so we can control the visiblity of
     * the grid points.
     *
     * @param on true to make it visible
     */
    public void setDisplayVisibility(boolean on) {
        if (settingVisibility) {
            return;
        }
        if ( !getHaveInitialized()) {
            return;
        }
        super.setDisplayVisibility(on);
        try {
            setSpatialLociVisible(spatialLociVisible);
            setTrajectoryActive(trajectoryActive);
            setVirtTempActive(virtTempActive);
        } catch (Exception exc) {}
    }




    /**
     * Returns the visibility of the spatial loci in the main, 3D window.
     *
     *                          otherwise, they will be rendered invisible.
     *
     * @return true if loci are visible
     */
    public boolean getSpatialLociVisible() {
        return spatialLociVisible;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getCurrentIdx() {
        return currIndex;
    }

    /**
     * Sets the currently-displayed sounding.  The index is that of the
     * soundings set by {@link #setSoundings(Field[], Field[], Field[]}.
     * If there are no soundings, then nothing is done.
     *
     * @param index                The index of the sounding to display.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    void setSounding(int index) throws VisADException, RemoteException {

        //System.err.println ("setSounding: index=" +index +"  currIndex:" + currIndex);

        if (tempProfiles != null) {
            /*
             * The following nested try-blocks ensure that the display
             * returns to its original state if an exception occurs.
             */
            try {
                aeroDisplay.setProfileVisible(currIndex, false);
                aeroDisplay.setProfileVisible(index, true);
                if (haveWinds) {
                    hodoDisplay.setProfileVisible(currIndex, false);
                }
                if (haveWinds) {
                    hodoDisplay.setProfileVisible(index, true);
                }
                soundingTable.setCurrentSounding(currIndex);

                try {
                    aeroDisplay.setActiveSounding(index);
                    if (haveWinds) {
                        hodoDisplay.setActiveWindProfile(index);
                    }
                    // need to get the actual data for manipulation
                    Sounding s = aeroDisplay.getActiveSounding();
                    setSounding(s.getTemperatureField(),
                                s.getDewPointField(), windProfiles[index]);  // not manipuable

                    currIndex = index;
                } catch (VisADException ex) {
                    aeroDisplay.setProfileVisible(index, false);
                    if (haveWinds) {
                        hodoDisplay.setProfileVisible(index, false);
                    }

                    throw ex;
                } catch (RemoteException ex) {
                    aeroDisplay.setProfileVisible(index, false);
                    if (haveWinds) {
                        hodoDisplay.setProfileVisible(index, false);
                    }

                    throw ex;
                }
            } catch (VisADException ex) {
                // need to get the actual data for manipulation
                Sounding s = aeroDisplay.getActiveSounding();
                setSounding(s.getTemperatureField(), s.getDewPointField(),
                            windProfiles[currIndex]);  // not manipuable
                throw ex;

            } catch (RemoteException ex) {
                // need to get the actual data for manipulation
                Sounding s = aeroDisplay.getActiveSounding();
                setSounding(tempProfiles[currIndex], dewProfiles[currIndex],
                            windProfiles[currIndex]);

                throw ex;
            }
            updateHeaderLabel();
        }
    }

    /**
     * Get the index of the current sounding
     *
     * @return the current index
     */
    public int getCurrentIndex() {
        return currIndex;
    }

    /**
     * Set the sounding profiles from the data
     *
     * @param tempPro   temperature profile
     * @param dewPro    dewpoint profile
     * @param windPro   wind profile
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private void setSounding(Field tempPro, Field dewPro, Field windPro)
            throws VisADException, RemoteException {
        dewProRef.setData(dewPro);
        tempProRef.setData(tempPro);
        windProRef.setData(windPro);
        updateLegendAndList();
    }

    /**
     * Sets the set of soundings -- completely replacing the previous set.
     *
     * @param tempPros         The temperature profiles.
     * @param dewPros          The dew-point profiles.
     * @param windPros         The wind profiles
     *
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected synchronized void setSoundings(Field[] tempPros,
                                             Field[] dewPros,
                                             Field[] windPros)
            throws VisADException, RemoteException {

        int n = tempPros.length;

        if (tempProfiles == null) {
            tempProfiles = (Field[]) tempPros.clone();
            dewProfiles  = (Field[]) dewPros.clone();
            windProfiles = (Field[]) windPros.clone();
        } else {
            tempProfiles = new Field[n];
            dewProfiles  = new Field[n];
            windProfiles = new Field[n];
            System.arraycopy(tempPros, 0, tempProfiles, 0, n);
            System.arraycopy(dewPros, 0, dewProfiles, 0, n);
            System.arraycopy(windPros, 0, windProfiles, 0, n);
        }
        Field[] tableSoundings = new Field[n];
        hodoDisplay.clear();

        for (int i = 0; i < n; i++) {
            if ((i == 0) && (windProfiles[i] != null)) {
                // assumes that if the first one is null, they are all null
                haveWinds = true;
                RealTupleType windTuple =
                    DataUtility.getFlatRangeType(windProfiles[i]);
                if (Unit.canConvert(((RealType) windTuple.getComponent(0))
                        .getDefaultUnit(), CommonUnit.meterPerSecond) && Unit
                            .canConvert(((RealType) windTuple.getComponent(1))
                                .getDefaultUnit(), CommonUnit
                                .meterPerSecond)) {
                    haveUAndVWinds = true;
                }
                //visad.python.JPythonMethods.dumpTypes(windProfiles[i]);
            }
            if (dewProfiles[i] == null) {
                haveDewpoints = false;
            }
            if (showUVCbx != null) {
                showUVCbx.setEnabled(haveWinds);
            }
            aeroDisplay.addProfile(i, tempProfiles[i], dewProfiles[i],
                                   windProfiles[i]);
            if (haveWinds) {
                hodoDisplay.addProfile(i, windProfiles[i]);
            }
            if ( !haveWinds) {
                if (haveDewpoints == false) {
                    tableSoundings[i] = FieldImpl.combine(new Field[] {
                        tempProfiles[i] }, true);
                } else {
                    tableSoundings[i] = FieldImpl.combine(new Field[] {
                        tempProfiles[i],
                        dewProfiles[i] }, true);
                }
            } else {
                tableSoundings[i] = FieldImpl.combine(new Field[] {
                    tempProfiles[i],
                    dewProfiles[i], windProfiles[i] }, true);
            }
        }
        for (int i = 0; i < n; i++) {
            aeroDisplay.setProfileVisible(i, false);
        }
        aeroDisplay.setProfileVisible(currIndex, true);


        if (haveWinds) {
            hodoDisplay.setProfileVisible(currIndex, true);
        }
        soundingTable.setSoundings(tableSoundings);
        soundingTable.setCurrentSounding(currIndex);
        if (viewTabs != null) {
            viewTabs.setEnabledAt(viewTabs.indexOfTab(HODOGRAPH_DISPLAY),
                                  haveWinds);
        }


        setSounding(currIndex);
    }

    /**
     * <p>Returns the title of this display.</p>
     *
     *
     * @return                       The title of this display.
     */
    protected final String xxxgetTitle() {
        return "SkewT";
    }

    /**
     * Indicates if this instance displays the path of a lifted parcel.
     *
     * @return                       True if and only if the path is or will be
     *                               displayed.
     */
    public synchronized final boolean getTrajectoryActive() {
        return trajectoryActive;
    }

    /**
     * Sets whether or not this instance will display the path of a lifted
     * parcel.
     *
     * @param active                 Whether or not to display the path.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public synchronized final void setTrajectoryActive(boolean active)
            throws VisADException, RemoteException {

        trajectoryActive = active;

        if (parcelPath != null) {
            parcelPath.setVisible(trajectoryActive);
        }

        if (parVirtTempPath != null) {
            parVirtTempPath.setVisible(trajectoryActive && virtTempActive);
        }
    }

    /**
     * Indicates whether or not this instance is or will display virtual
     * temperature paths.
     *
     * @return                       True if and only if virtual temperature
     *                               paths are or will be displayed.
     */
    public synchronized final boolean getVirtTempActive() {
        return virtTempActive;
    }

    /**
     * Sets whether or not this instance will display virtual temperature
     * paths.
     *
     * @param active                 Whether or not to display virtual
     *                               temperature paths.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public synchronized final void setVirtTempActive(boolean active)
            throws VisADException, RemoteException {

        virtTempActive = active;

        if (envVirtTempPath != null) {
            envVirtTempPath.setVisible(virtTempActive);
        }

        if (parVirtTempPath != null) {
            parVirtTempPath.setVisible(trajectoryActive && virtTempActive);
        }
    }

    /**
     * Returns the mode that is used to determine the initial conditions of the
     * lifted parcel.
     *
     * @return                       The mode used to determine the initial
     *                               conditions of the lifted parcel.
     */
    public synchronized final ParcelMode getParcelMode() {
        return parcelModeInfos[parcelModeIndex].mode;
    }

    /**
     * Sets the mode used to determine the initial conditions of the lifted
     * parcel.
     *
     * @param mode                   The mode used to determine the initial
     *                               conditions of the lifted parcel.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public synchronized final void setParcelMode(ParcelMode mode)
            throws VisADException, RemoteException {

        for (int i = 0; i < parcelModeInfos.length; i++) {
            if (parcelModeInfos[i].mode == mode) {
                setParcelMode(i);

                break;
            }
        }
    }

    /**
     * Set the parcel mode
     *
     * @param i   the parcel mode (index into array)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private void setParcelMode(int i) throws VisADException, RemoteException {

        if ((i >= 0) && (i < parcelModeInfos.length)) {
            if (aeroCellNet != null) {
                aeroCellNet.setParcelMode(parcelModeInfos[i].mode);
            }

            parcelModeIndex = i;
        }
    }

    /**
     * Creates the Skew-T display component.
     *
     * @return                       The Skew-T display component.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        JCheckBox trajBox = new JCheckBox("Parcel Path", trajectoryActive);

        trajBox.setToolTipText("Display Parcel's Pseudoadiabatic Path");
        trajBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent event) {

                try {
                    setTrajectoryActive(event.getStateChange()
                                        == ItemEvent.SELECTED);
                } catch (Exception e) {
                    logException(e);
                }
            }
        });

        JCheckBox virtTempBox = new JCheckBox("Virtual Temperature",
                                    virtTempActive);

        virtTempBox.setToolTipText("Display Virtual Temperatures");
        virtTempBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent event) {

                try {
                    setVirtTempActive(event.getStateChange()
                                      == ItemEvent.SELECTED);
                } catch (Exception e) {
                    logException(e);
                }
            }
        });

        final JComboBox parcelModeBox = new JComboBox(parcelModeInfos);

        parcelModeBox.setToolTipText("Parcel Determination Mode");
        parcelModeBox.setSelectedIndex(parcelModeIndex);
        parcelModeBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {

                try {
                    setParcelMode(parcelModeBox.getSelectedIndex());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        String[]        spacings   = new String[] {
            ALL_LEVELS, MANDATORY_LEVELS, "10", "25", "50", "100"
        };
        final JComboBox spacingBox = new JComboBox(spacings);

        spacingBox.setToolTipText("Wind Barb Spacing");
        spacingBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                try {

                    String tmp = (String) spacingBox.getSelectedItem();
                    setWindLevels(tmp);
                    windBarbSpacing = tmp;

                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });
        spacingBox.setSelectedItem(windBarbSpacing);


        JCheckBox showBox = new JCheckBox(getSpatialLociLabel(),
                                          spatialLociVisible);

        showBox.setToolTipText(
            "Show Spatial Loci of Soundings in Main, 3-D Window");
        showBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                try {
                    setSpatialLociVisible(event.getStateChange()
                                          == ItemEvent.SELECTED);
                } catch (Exception e) {
                    logException(e);
                }
            }
        });
        List<JCheckBox> lineControls = new ArrayList<JCheckBox>();

        lineControls.add(GuiUtils.makeCheckbox("Dry Adiabats", soundingView,
                "dryAdiabatVisibility"));
        lineControls.add(GuiUtils.makeCheckbox("Saturation Adiabats",
                soundingView, "saturationAdiabatVisibility"));
        lineControls.add(GuiUtils.makeCheckbox("Mixing Ratio", soundingView,
                "saturationMixingRatioVisibility"));
        JPanel lines = GuiUtils.left(GuiUtils.vbox(lineControls));
        //lines.setBorder(new TitledBorder("Line Visibility"));


        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        Component comboBoxes = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Parcel mode:"), parcelModeBox, GuiUtils.filler(),
            GuiUtils.rLabel("Wind spacing:"), spacingBox,
            GuiUtils.lLabel("(hPa)")
        }, 3,              //3 columns
           GuiUtils.WT_N,  //Don't expand the grid horizontally
           GuiUtils.WT_N   //Don't expand the grid  vertically
               );

        Container checkBoxes = GuiUtils.vbox(trajBox, virtTempBox, showBox);

        /*
         * Do not fill the widgets in the gridbag.
         * Create a 2 column by 2 row GridBagLayout,
         * Don't expand the widgets:
         */
        Component viewOptions = GuiUtils.doLayout(new Component[] { lines,
                checkBoxes,
        //GuiUtils.filler(),
        GuiUtils.top(comboBoxes) }, 3,              //3 columns
                                    GuiUtils.WT_N,  //Don't expand the grid horizontally
                                    GuiUtils.WT_N   //Don't expand the grid  vertically
                                        );
        //Container controlArea    = Box.createHorizontalBox();
        Component specificWidget = getSpecificWidget();

        //if (specificWidget != null) {
        //    controlArea.add(GuiUtils.center(specificWidget));
        //}
        JScrollPane bottomComp =
            new JScrollPane(
                viewOptions,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        //controlArea.add(GuiUtils.inset(viewOptions, 4));
        //        JComponent soundingComp =
        //            GuiUtils.centerBottom(soundingView.getContents(), bottomComp);
        JSplitPane soundingComp = GuiUtils.vsplit(soundingView.getContents(),
                                      bottomComp, 0.75);

        soundingComp.setOneTouchExpandable(true);

        JScrollPane sp =
            new JScrollPane(
                readoutPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setViewportBorder(
            BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        JComponent left = (specificWidget != null)
                          ? GuiUtils.centerBottom(sp, specificWidget)
                          : sp;
        JScrollPane leftSP =
            new JScrollPane(
                left, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        leftSP.setPreferredSize(new Dimension(250, 100));


        viewTabs = new JTabbedPane();
        viewTabs.add("Sounding Chart", soundingComp);
        viewTabs.add(HODOGRAPH_DISPLAY, hodoView.getComponent());
        JScrollPane tableSP =
            new JScrollPane(
                soundingTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableSP.setViewportBorder(
            BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        showUVCbx = GuiUtils.makeCheckbox("Show U and V", this,
                                          "showUAndVWinds");
        showUVCbx.setEnabled(haveWinds);
        JComponent tableComp = GuiUtils.topCenter(GuiUtils.right(showUVCbx),
                                   tableSP);
        viewTabs.add("Table", tableComp);
        GuiUtils.handleHeavyWeightComponentsInTabs(viewTabs);
        viewTabs.setSelectedIndex(0);
        viewTabs.setEnabledAt(viewTabs.indexOfTab(HODOGRAPH_DISPLAY),
                              haveWinds);

        //JSplitPane spl = GuiUtils.hsplit(sp, soundingView.getContents(), .35);

        /*
        JScrollPane rightSP = new JScrollPane(viewTabs,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        */

        JSplitPane spl = GuiUtils.hsplit(leftSP, viewTabs, .4);

        spl.setOneTouchExpandable(true);
        //Container contents = GuiUtils.topCenterBottom(locLabel, spl,
        //                         controlArea);
        Container contents = GuiUtils.topCenter(headerLabel, spl);

        return contents;
    }

    /**
     * <p>Returns the data-specific widget for controlling the data-specific
     * aspects of the display or <code>null</code> if no such widget exists.
     * The widget is added to the window in the apppropriate place.</p>
     *
     * <p>This implementation returns <code>null</code>.
     *
     * @return                      The data-specific control-widget or
     *                              <code>null</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    Component getSpecificWidget() throws VisADException, RemoteException {
        return null;
    }

    /**
     *  Return the label used for the spatial loci checkbox.
     *  This can get overwritten by derived classes to return the
     *  correct label.
     *
     * @return true if they are visible
     */
    protected String getSpatialLociLabel() {
        return "Spatial Loci";
    }


    /**
     * Gets the location of the profile.
     *
     * @return The location of the profile (may be null).
     */
    protected final LatLonPoint getLocation() {
        return location;
    }

    /**
     * Sets the location of the profile.
     *
     * @param loc                   The location of the profile.
     * @throws VisADException       VisAD failure.
     * @throws RemoteException      Java RMI failure.
     */
    final void setLocation(LatLonPoint loc)
            throws VisADException, RemoteException {

        location = loc;
        double lat = loc.getLatitude().getValue();
        /*
        double lon = loc.getLongitude().getValue();

        headerLabel.setText(" Lat: " + getDisplayConventions().format(lat)
                         + "  Long: " + getDisplayConventions().format(lon));
                         */
        aeroDisplay.setBarbOrientation((lat >= 0)
                                       ? FlowControl.NH_ORIENTATION
                                       : FlowControl.SH_ORIENTATION);
    }

    /**
     * Update the location label, subclasses can override.
     */
    protected void updateHeaderLabel() {
        if (location != null) {
            double lat = location.getLatitude().getValue();
            double lon = location.getLongitude().getValue();

            headerLabel.setText(" Lat: "
                                + getDisplayConventions().format(lat)
                                + "  Lon: "
                                + getDisplayConventions().format(lon));

        } else {
            headerLabel.setText("       ");
        }
    }

    /**
     * Returns the latitude/longitude point corresponding to a display X/Y
     * point or <code>null</code> if no such point exists.
     *
     * @param xy                 The display X/Y point.
     * @return                   The corresponding lat/lon position or <code>
     *                           null</code>.
     * @throws VisADException    if a VisAD failure occurs.
     * @throws RemoteException   if a Java RMI failure occurs.
     */
    final LatLonPoint latLon(RealTuple xy)
            throws VisADException, RemoteException {

        LatLonPoint latLon = null;
        Real[]      reals  = xy.getRealComponents();

        if (getControlContext() != null) {
            EarthLocationTuple elt =
                (EarthLocationTuple) boxToEarth(new double[] {
                    reals[0].getValue(),
                    reals[1].getValue(), (xy.getDimension() == 3)
                                         ? reals[0].getValue()
                                         : 1.0 });

            if (elt != null) {
                latLon = elt.getLatLonPoint();
            }
        }

        return latLon;
    }

    /**
     * Returns the display X/Y point corresponding to a latitude/longitude
     * point or <code>null</code> if no such point exists.
     *
     * @param latLon             The display lat/lon point.
     *
     * @return                   The corresponding X/Y position or <code>
     *                           null</code>.
     * @throws VisADException    if a VisAD failure occurs.
     * @throws RemoteException   if a Java RMI failure occurs.
     */
    final RealTuple xY(LatLonPoint latLon)
            throws VisADException, RemoteException {

        RealTuple xy = null;

        if (getControlContext() != null) {
            RealTuple xyz =
                earthToBoxTuple(new EarthLocationTuple(latLon.getLatitude(),
                    latLon.getLongitude(), new Real(RealType.Altitude, 0.0)));

            xy = new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                               new Real[] { (Real) xyz.getComponent(0),
                                            (Real) xyz.getComponent(
                                            1) }, (CoordinateSystem) null);
        }

        return xy;
    }

    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     *                   for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        JMenuItem mi = new JMenuItem("Reset sounding");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetProfile(currIndex);
            }
        });
        items.add(mi);
        super.getEditMenuItems(items, forMenuBar);
    }

    /**
     * Add the  relevant view menu items into the list
     *
     * @param menus List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     *                   for a popup menu in the legend
     */
    protected void getViewMenuItems(List menus, boolean forMenuBar) {
        super.getViewMenuItems(menus, forMenuBar);

        if (forMenuBar) {
            JMenu svMenu = soundingView.makeViewMenu();
            svMenu.setText("Sounding Chart");
            menus.add(svMenu);
            JMenu hvMenu = hodoView.makeViewMenu();
            hvMenu.setText("Hodograph");
            menus.add(hvMenu);
        }
    }

    /**
     * Make the display type menu
     *
     * @param type display type
     *
     * @return the JRadioButtonMenuItem menu
     */
    private JRadioButtonMenuItem makeDisplayTypeMenu(String type) {

        JRadioButtonMenuItem rmi =
            new JRadioButtonMenuItem(getTypeLabel(type), isDisplayType(type));
        rmi.setActionCommand(type);
        rmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JRadioButtonMenuItem myRmi =
                    (JRadioButtonMenuItem) e.getSource();
                setDisplayType(myRmi.getActionCommand());
                setDisplayName(myRmi.getText());
                updateLegendAndList();
            }
        });
        return rmi;
    }

    /**
     * See if the display type in question is the same as this type.
     * @param type   display type
     * @return true if display types are the same
     */
    public boolean isDisplayType(String type) {
        return getDisplayType().equals(type);
    }

    /**
     * Reset the profile to the original field
     *
     * @param index index of the profile to reset
     */
    private void resetProfile(int index) {
        try {
            aeroDisplay.setOriginalProfiles(index);
            if (haveWinds) {
                hodoDisplay.setOriginalProfile(index);
            }
            setSounding(index);
        } catch (Exception excp) {
            logException("Unable to reset sounding", excp);
        }
    }

    /**
     * Set the type of display.  Used by persistence.
     *
     * @param type the display type
     */
    public void setDisplayType(String type) {
        try {
            if (getHaveInitialized()) {
                aeroDisplay.setCoordinateSystem(type);
            }
            displayType = type;
        } catch (Exception e) {
            LogUtil.printMessage("setDisplayType got " + e.toString());
        }
    }

    /**
     * Get the type of display.  Used by persistence.
     *
     * @return the display type
     */
    public String getDisplayType() {
        return displayType;
    }

    /**
     * Get the wind barb spacing as a String
     * @return String representation of the wind barb spacing
     */
    public String getWindBarbSpacing() {
        return windBarbSpacing;
    }

    /**
     * Set the wind barb spacing.
     * @param newSpacing  either ALL_LEVELS or MANDATORY_LEVELS, or the value
     *                    as a String.
     *
     */
    public void setWindBarbSpacing(String newSpacing) {
        try {
            if (getHaveInitialized()) {
                setWindLevels(newSpacing);
            }
            windBarbSpacing = newSpacing;
        } catch (Exception e) {
            LogUtil.printMessage("setWindBarbSpacing got " + e.toString());
        }
    }

    /**
     * Set the wind intervals based on the String
     *
     * @param windInterval  either ALL_LEVELS or MANDATORY_LEVELS, or the value
     *                      as a String.
     *
     */
    private void setWindLevels(String windInterval) {
        AerologicalCoordinateSystem acs = aeroDisplay.getCoordinateSystem();
        Real                        minP       = acs.getMinimumPressure();
        Real                        maxP       = acs.getMaximumPressure();
        RealType                    pRT        = (RealType) minP.getType();
        Unit                        pUnit      = minP.getUnit();
        Gridded1DSet                spacingSet = null;
        float[]                     vals       = null;
        if (windInterval == MANDATORY_LEVELS) {
            vals = MANDATORY_LEVEL_VALUES;
        } else if (windInterval != ALL_LEVELS) {
            try {
                float spacing = Misc.parseFloat(windInterval);
                if ( !Float.isNaN(spacing)) {
                    float maxPVal = (float) maxP.getValue();
                    float minPVal = (float) minP.getValue();
                    vals = Misc.computeTicks(maxPVal, minPVal, minPVal,
                                             spacing);
                }
            } catch (Exception excp) {}
        }
        if (vals != null) {
            try {
                spacingSet = new Gridded1DSet(AirPressure.getRealTupleType(),
                        new float[][] {
                    vals
                }, vals.length, (CoordinateSystem) null,
                   new Unit[] { pUnit }, (ErrorEstimate[]) null);
            } catch (VisADException ve) {}
        }
        try {
            aeroDisplay.setWindLevels(spacingSet);
        } catch (Exception e) {
            LogUtil.printMessage("AerologicalDisplay.setWindLevels got "
                                 + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Get the label for the type of display
     *
     * @param displayType  type name
     *
     * @return the label
     */
    public static String getTypeLabel(String displayType) {
        return SoundingViewManager.getTypeLabel(displayType);
    }

    /**
     *  Set the SoundingView property.
     *
     *  @param value The new value for SoundingView
     */
    public void setSoundingView(SoundingViewManager value) {
        soundingView = value;
    }

    /**
     *  Get the SoundingView property.
     *
     *  @return The SoundingView
     */
    public SoundingViewManager getSoundingView() {
        return soundingView;
    }

    /**
     *  Set the HodographView property.
     *
     *  @param value The new value for HodographView
     */
    public void setHodographView(HodographViewManager value) {
        hodoView = value;
    }

    /**
     *  Get the HodographView property.
     *
     *  @return The HodographView
     */
    public HodographViewManager getHodographView() {
        return hodoView;
    }


    /**
     * Are the winds shown as U and V?
     * @return true if winds are shown as U/V components
     */
    public boolean getShowUAndVWinds() {
        return showUAndVWinds;
    }

    /**
     * Set the show u and v property
     *
     * @param show  true to show winds as u and v
     */
    public void setShowUAndVWinds(boolean show) {
        showUAndVWinds = show;
        if (soundingTable != null) {
            soundingTable.setShowUVComps(show);
        }
    }






    /**
     * Class MyActionImpl _more_
     *
     *
     * @author IDV Development Team
     */
    private static abstract class MyActionImpl extends ActionImpl {

        /** _more_ */
        public DataReference reference;

        /** _more_ */
        public AerologicalReadoutTable readoutTable;

        /**
         * _more_
         *
         * @param name _more_
         * @param readoutTable _more_
         * @param ref _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         * @throws visad.ReferenceException _more_
         */
        public MyActionImpl(String name,
                            AerologicalReadoutTable readoutTable,
                            DataReference ref)
                throws visad.ReferenceException, RemoteException,
                       VisADException {
            super(name);
            this.reference    = ref;
            this.readoutTable = readoutTable;
            addReference(ref);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected DataReference getDataReference() {
            return reference;
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        protected Real getReal() throws VisADException, RemoteException {
            return (Real) reference.getData();
        }

        /**
         * _more_
         */
        protected void doRemove() {
            try {
                removeReference(reference);
            } catch (Exception exc) {}
        }

    }



}

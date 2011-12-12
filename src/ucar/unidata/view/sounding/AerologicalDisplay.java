/*
 * $Id: AerologicalDisplay.java,v 1.53 2007/01/30 22:46:00 dmurray Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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



package ucar.unidata.view.sounding;


import ucar.unidata.beans.*;

import ucar.unidata.util.Misc;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.functiontypes.*;
import ucar.visad.quantities.*;

import visad.*;

import visad.java2d.*;

import visad.java3d.*;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.GraphicsConfiguration;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.*;

import javax.swing.JPanel;


/**
 * Provides a 2-D VisAD display for an aerological ( meteorological
 * thermodynamic) diagram.
 *
 * @author Unidata Development Team
 * @version $Id: AerologicalDisplay.java,v 1.53 2007/01/30 22:46:00 dmurray Exp $
 */
public class AerologicalDisplay extends DisplayMaster implements AerologicalDisplayConstants {

    /**
     * The name of the VisAD-cursor pressure property.
     */
    public static final String CURSOR_PRESSURE = "cursorPressure";

    /**
     * The name of the mouse-pointer pressure property.
     */
    public static final String POINTER_PRESSURE = "pointerPressure";

    /**
     * The name of the VisAD-cursor temperature property.
     */
    public static final String CURSOR_TEMPERATURE = "cursorTemperature";

    /**
     * The name of the mouse-pointer temperature property.
     */
    public static final String POINTER_TEMPERATURE = "pointerTemperature";

    /**
     * The name of the active sounding property.
     */
    public static final String ACTIVE_SOUNDING = "activeSounding";

    /**
     * The name of the active wind profile property.
     */
    public static final String ACTIVE_WIND_PROFILE = "activeWindProfile";

    /**
     * The name of the profile temperature property.
     */
    public static final String PROFILE_TEMPERATURE = "profileTemperature";

    /**
     * The name of the profile dew-point property.
     */
    public static final String PROFILE_DEW_POINT = "profileDewPoint";

    /**
     * The name of the profile wind speed property.
     */
    public static final String PROFILE_WIND_SPEED = "profileWindSpeed";

    /**
     * The name of the profile wind direction property.
     */
    public static final String PROFILE_WIND_DIRECTION =
        "profileWindDirection";

    /**
     * The name of the CAPE property.
     */
    public static final String CAPE = "cAPE";

    /**
     * The set of t/td soundings.
     */
    private SoundingSet soundings;

    /**
     * The set of windProfiles.
     */
    private WindProfileSet winds;

    /**
     * The type of the display pressure.
     */
    private DisplayRealType displayPressureType;

    /**
     * The type of the display temperature.
     */
    private DisplayRealType displayTemperatureType;

    /**
     * The type of the (dummy) Z-axis.
     */
    private DisplayRealType displayZAxisType;

    /**
     * The DisplayTupleType.
     */
    private DisplayTupleType displayTupleType;

    /**
     * The (pressure,temperature) <-> (x,y) coordinate system transformation.
     */
    private AerologicalCoordinateSystem coordinateSystem;

    /** ScalarMap for pressure */
    private ScalarMap pressureMap;

    /** ScalarMap for in-situ air temperature */
    private ScalarMap inSituTempMap;

    /** ScalarMap for air temperature */
    private ScalarMap airTempMap;

    /** ScalarMap for virtual temperature */
    private ScalarMap virtTempMap;

    /** ScalarMap for dewpoint */
    private ScalarMap dewPointMap;

    /**
     * The calculator of CAPE.
     */
    private CapeCalculator capeCalculator;

    /**
     * The background dry adiabats.
     */
    private DryAdiabats dryAdiabats;

    /**
     * The background saturation adiabats.
     */
    private SaturationAdiabats saturationAdiabats;

    /**
     * The background saturation adiabats.
     */
    private SaturationMixingRatioContours satMixingRatios;

    /**
     * The displayed background pressure contours.
     */
    private Isobars isobars;

    /**
     * The displayed background temperature contours.
     */
    private Isotherms isotherms;

    /**
     * The displayed right pressure labels contours.
     */
    private RightPressureAxisLabels rightPressureLabels;

    /**
     * The displayed left pressure labels contours.
     */
    private LeftPressureAxisLabels leftPressureLabels;

    /**
     * The displayed right pressure labels contours.
     */
    private LowerTemperatureAxisLabels lowerTemperatureLabels;

    /**
     * The displayed left pressure labels contours.
     */
    private UpperTemperatureAxisLabels upperTemperatureLabels;

    /**
     * The displayed left pressure labels contours.
     */
    private Box box;

    /**
     * The displayed wind staff.
     */
    private MyWindStaff windStaff;

    /**
     * The mouse-pointer pressure.
     * @serial
     */
    private volatile Real pointerPressure;

    /**
     * The cursor-temperature.
     * @serial
     */
    private volatile Real cursorTemperature;

    /**
     * The pointer-temperature.
     * @serial
     */
    private volatile Real pointerTemperature;

    /**
     * The lifted parcel trajectory.
     */
    private DisplayablePseudoAdiabaticTrajectory pseudoAdiabaticTrajectory;

    /**
     * The constrainProfiles property.
     */
    private boolean constrainProfiles = true;

    /**
     * The control for the winds
     */
    private FlowControl flowControl = null;

    /**
     * The wind barb orientation
     */
    private int barbOrientation = FlowControl.NH_ORIENTATION;

    /**
     * The control for the winds
     */
    private int barbScale = 4;

    /**
     * Active sounding index
     */
    private int activeIndex = 0;

    /**
     * X axis position for wind barb staff (for ConstantMaps)
     */
    private double WIND_STAFF_XPOS = 1.2;

    /** instance counter */
    private static int instance = 0;

    /** instance locking mutex */
    private static Object INSTANCE_MUTEX = new Object();

    /**
     * List of CoordinateSystem dependent displays.
     */
    private List csDependentDisplays = new ArrayList();

    /** flag for whether we've been initialized */
    private boolean init = false;

    /**
     * Constructs.  The value of the constrainProfiles property is
     * initially <code>true</code>.
     * @param display           The VisAD display.
     * @param initialCapacity   The anticipated number of displayables.
     * @param displayPressureType       The display pressure type.
     * @param displayTemperatureType    The display temperature type.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected AerologicalDisplay(
            DisplayImpl display, int initialCapacity,
            final DisplayRealType displayPressureType,
            final DisplayRealType displayTemperatureType,
            AerologicalCoordinateSystem coordinateSystem)
            throws VisADException, RemoteException {

        super(display, initialCapacity);
        this.displayPressureType    = displayPressureType;
        this.displayTemperatureType = displayTemperatureType;
        this.coordinateSystem       = coordinateSystem;
        initializeClass();
    }

    /**
     * Create a new AerologicalDisplay and use the coordinate system
     * for all transforms
     *
     * @param acs AerologicalCoordinateSystem for transforms
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public AerologicalDisplay(AerologicalCoordinateSystem acs)
            throws VisADException, RemoteException {
        this(acs, null);
    }

    /**
     * Create a new AerologicalDisplay and use the coordinate system
     * for all transforms
     *
     * @param acs AerologicalCoordinateSystem for transforms
     * @param gc  GraphicsConfiguration
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public AerologicalDisplay(AerologicalCoordinateSystem acs,
                              GraphicsConfiguration gc)
            throws VisADException, RemoteException {
        this(new DisplayImplJ3D("Aerological Display",
                                new TwoDDisplayRendererJ3D(), gc), 6, null,
                                    null, acs);
        //this(new DisplayImplJ2D("Aerological Display"), 6, null, null, acs);
    }

    /**
     * Initialize the class
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void initializeClass() throws VisADException, RemoteException {

        setDisplayTypes();

        DisplayImpl     display  = (DisplayImpl) getDisplay();
        DisplayRenderer dispRend = display.getDisplayRenderer();
        dispRend.setBoxOn(false);
        dispRend.setCursorStringOn(false);
        if (dispRend instanceof DisplayRendererJ2D) {
            ((DisplayRendererJ2D) dispRend).addKeyboardBehavior(
                new KeyboardBehaviorJ2D((DisplayRendererJ2D) dispRend));
        } else {
            ((DisplayRendererJ3D) dispRend).addKeyboardBehavior(
                new KeyboardBehaviorJ3D((DisplayRendererJ3D) dispRend));
        }

        ((JPanel) getDisplay().getComponent()).setPreferredSize(
            new Dimension(512, 390));
        saveProjection();

        GraphicsModeControl gmc = getDisplay().getGraphicsModeControl();
        gmc.setPointSize(5.0f);


        soundings = new SoundingSet(display);
        winds     = new WindProfileSet(new WindBarbProfile(display), display);
        winds.addConstantMap(new ConstantMap(WIND_STAFF_XPOS, Display.XAxis));

        soundings.setConstrainProfiles(constrainProfiles);

        pseudoAdiabaticTrajectory =
            new DisplayablePseudoAdiabaticTrajectory(getDisplay(),
                displayPressureType, displayTemperatureType);

        soundings.addPropertyChangeListener(SoundingSet.TEMPERATURE,
                                            new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                PropertyChangeEvent newEvent =
                    new PropertyChangeEvent(AerologicalDisplay.this,
                                            PROFILE_TEMPERATURE,
                                            event.getOldValue(),
                                            event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                AerologicalDisplay.this.firePropertyChange(newEvent);
            }
        });
        soundings.addPropertyChangeListener(SoundingSet.DEW_POINT,
                                            new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                PropertyChangeEvent newEvent =
                    new PropertyChangeEvent(AerologicalDisplay.this,
                                            PROFILE_DEW_POINT,
                                            event.getOldValue(),
                                            event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                AerologicalDisplay.this.firePropertyChange(newEvent);

                try {
                    pseudoAdiabaticTrajectory.recompute();
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle profile dew-point change: " + e);
                }
            }
        });
        winds.addPropertyChangeListener(WindProfileSet.SPEED,
                                        new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                PropertyChangeEvent newEvent =
                    new PropertyChangeEvent(AerologicalDisplay.this,
                                            PROFILE_WIND_SPEED,
                                            event.getOldValue(),
                                            event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                AerologicalDisplay.this.firePropertyChange(newEvent);

            }
        });
        winds.addPropertyChangeListener(WindProfileSet.DIRECTION,
                                        new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                PropertyChangeEvent newEvent =
                    new PropertyChangeEvent(AerologicalDisplay.this,
                                            PROFILE_WIND_DIRECTION,
                                            event.getOldValue(),
                                            event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                AerologicalDisplay.this.firePropertyChange(newEvent);

            }
        });
        soundings.addPropertyChangeListener(soundings.ACTIVE_SOUNDING,
                                            new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                /*
                 * Clear the pseudo-adiabatic trajectory whenever it's
                 * disabled and the active sounding changes.
                 */
                try {
                    if ( !isPseudoAdiabaticTrajectoryEnabled()) {
                        pseudoAdiabaticTrajectory.clear();
                    }

                    PropertyChangeEvent newEvent =
                        new PropertyChangeEvent(AerologicalDisplay.this,
                            ACTIVE_SOUNDING, null, event.getNewValue());

                    newEvent.setPropagationId(event.getPropagationId());
                    firePropertyChange(newEvent);
                } catch (Exception e) {
                    System.err.println("ACTIVE_SOUNDING: " + e.toString());
                }
            }
        });
        winds.addPropertyChangeListener(winds.ACTIVE_WIND_PROFILE,
                                        new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {

                    PropertyChangeEvent newEvent =
                        new PropertyChangeEvent(AerologicalDisplay.this,
                            ACTIVE_WIND_PROFILE, null, event.getNewValue());

                    newEvent.setPropagationId(event.getPropagationId());
                    firePropertyChange(newEvent);
                } catch (Exception e) {
                    System.err.println("ACTIVE_WIND_PROFILE: "
                                       + e.toString());
                }
            }
        });
        display.enableEvent(DisplayEvent.MOUSE_MOVED);
        display.enableEvent(DisplayEvent.MOUSE_DRAGGED);
        addDisplayListener(new DisplayListener() {

            public void displayChanged(DisplayEvent event) {

                int id = event.getId();

                try {
                    if (id == event.MOUSE_PRESSED_CENTER) {
                        cursorMoved();
                    } else if (id == event.MOUSE_MOVED) {
                        pointerMoved(event.getX(), event.getY());
                    } else if (id == event.MOUSE_DRAGGED) {
                        int mods = event.getInputEvent().getModifiers();

                        if ((mods & InputEvent.BUTTON2_MASK) != 0) {
                            cursorMoved();
                        } else if ((mods & InputEvent.BUTTON3_MASK) != 0) {
                            pointerMoved(event.getX(), event.getY());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("MOUSE_EVENT: " + e.toString());
                }
            }
        });

        capeCalculator = new CapeCalculator();

        capeCalculator.addPropertyChangeListener(capeCalculator.CAPE,
                new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                // Pass it on.
                PropertyChangeEvent newEvent = new PropertyChangeEvent(this,
                                                   CAPE, event.getOldValue(),
                                                   event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                AerologicalDisplay.this.firePropertyChange(newEvent);
            }
        });
        addDisplayable(soundings);
        setWindColor(getForeground());
        addDisplayable(winds);
        addDisplayable(pseudoAdiabaticTrajectory);
        addDisplayable(box = new Box(coordinateSystem));
        addDisplayable(isobars = new Isobars(coordinateSystem));
        addDisplayable(isotherms = new Isotherms(coordinateSystem));
        setBackgroundLineColor(getForeground());
        addDisplayable(dryAdiabats = new DryAdiabats(coordinateSystem));
        addDisplayable(saturationAdiabats =
            new SaturationAdiabats(coordinateSystem));
        addDisplayable(satMixingRatios =
            new SaturationMixingRatioContours(coordinateSystem));
        satMixingRatios.setVisible(false);
        addDisplayable(windStaff = new MyWindStaff(coordinateSystem));
        windStaff.setVisible(false);  // set to false until we get data

        /*
         * Define the labels for the axes.
         */
        addDisplayable(leftPressureLabels =
            new LeftPressureAxisLabels(coordinateSystem,
                                       isobars.getContourLevels()));
        addDisplayable(rightPressureLabels =
            new RightPressureAxisLabels(coordinateSystem,
                                        isobars.getContourLevels()));
        rightPressureLabels.setVisible(false);
        addDisplayable(lowerTemperatureLabels =
            new LowerTemperatureAxisLabels(coordinateSystem,
                                           isotherms.getContourLevels()));
        addDisplayable(upperTemperatureLabels =
            new UpperTemperatureAxisLabels(coordinateSystem,
                                           isotherms.getContourLevels()));
        setLabelColor(getForeground());
    }

    /**
     * Set up the DisplayTupleType.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void setDisplayTypes() throws VisADException, RemoteException {

        int myInstance;
        synchronized (INSTANCE_MUTEX) {
            myInstance = instance++;
        }
        /*
         * The low and high values in the following definitions for
         * the DisplayRealType-s are necessary to make associated
         * ScalarMap-s correctly convert data values.
         */
        double minP = getCoordinateSystem().getMinimumPressure().getValue(
                          CommonUnits.HECTOPASCAL);
        double maxP = getCoordinateSystem().getMaximumPressure().getValue(
                          CommonUnits.HECTOPASCAL);
        displayPressureType = new DisplayRealType("ADPressure" + myInstance,
                true, minP, maxP, (maxP - minP) / 2, CommonUnits.HECTOPASCAL);

        double minT = getCoordinateSystem().getMinimumTemperature().getValue(
                          CommonUnits.CELSIUS);
        double maxT = getCoordinateSystem().getMaximumTemperature().getValue(
                          CommonUnits.CELSIUS);
        displayTemperatureType = new DisplayRealType("ADTemperature"
                + myInstance, true, minT, maxT, (maxT - minT) / 2,
                              CommonUnits.CELSIUS);

        displayZAxisType = new DisplayRealType("ADZAxis" + myInstance, true,
                0.0, null);

        displayTupleType = new DisplayTupleType(new DisplayRealType[] {
            displayPressureType,
            displayTemperatureType, displayZAxisType }, getCoordinateSystem()
                .createDisplayCoordinateSystem(coordinateSystem));
        setDisplayScalarMaps();
    }

    /**
     * Set up the DisplayScalarMaps for this display.
     *
     * @throws RemoteException   Java RMI exception
     * @throws VisADException    VisAD problem
     */
    protected void setDisplayScalarMaps()
            throws VisADException, RemoteException {

        setDisplayInactive();
        ScalarMapSet mapSet = new ScalarMapSet();

        if ( !init) {
            ScalarMap map   = new ScalarMap(RealType.XAxis, Display.XAxis);
            double[]  range = new double[2];

            Display.XAxis.getRange(range);
            map.setRange(range[0], range[1]);
            mapSet.add(map);
            Display.YAxis.getRange(range);

            map = new ScalarMap(RealType.YAxis, Display.YAxis);

            map.setRange(range[0], range[1]);
            mapSet.add(map);
            ScalarMap xFlow = new ScalarMap(WesterlyWind.getRealType(),
                                            Display.Flow1X);
            xFlow.setRange(-1.0, 1.0);
            xFlow.addScalarMapListener(new ScalarMapListener() {
                public void controlChanged(ScalarMapControlEvent event)
                        throws RemoteException, VisADException {

                    int id = event.getId();

                    if ((id == event.CONTROL_ADDED)
                            || (id == event.CONTROL_REPLACED)) {
                        flowControl = (FlowControl) event.getControl();

                        if (flowControl != null) {

                            flowControl.setBarbOrientation(barbOrientation);
                            setFlowScale();
                        }
                    }
                }

                public void mapChanged(ScalarMapEvent event) {
                    //System.out.println("ContourLines: Autoscaling");
                }  // ignore
            });
            mapSet.add(xFlow);

            ScalarMap yFlow = new ScalarMap(SoutherlyWind.getRealType(),
                                            Display.Flow1Y);
            yFlow.setRange(-1.0, 1.0);
            mapSet.add(yFlow);
            init = true;
        }

        if (pressureMap != null) {
            removeScalarMap(pressureMap);
        }

        pressureMap = new ScalarMap(AirPressure.getRealType(),
                                    displayPressureType);

        pressureMap.setRangeByUnits();
        mapSet.add(pressureMap);

        /**
         * RAOB temperature profiles contain "InSituAirTemperature".
         */
        if (inSituTempMap != null) {
            removeScalarMap(inSituTempMap);
        }
        inSituTempMap = new ScalarMap(InSituAirTemperature.getRealType(),
                                      displayTemperatureType);

        inSituTempMap.setRangeByUnits();
        mapSet.add(inSituTempMap);

        /**
         * Parcel trajectories contain "AirTemperature".
         */
        if (airTempMap != null) {
            removeScalarMap(airTempMap);
        }
        airTempMap = new ScalarMap(AirTemperature.getRealType(),
                                   displayTemperatureType);

        airTempMap.setRangeByUnits();
        mapSet.add(airTempMap);

        if (virtTempMap != null) {
            removeScalarMap(virtTempMap);
        }
        virtTempMap = new ScalarMap(VirtualTemperature.getRealType(),
                                    displayTemperatureType);

        virtTempMap.setRangeByUnits();
        mapSet.add(virtTempMap);

        if (dewPointMap != null) {
            removeScalarMap(dewPointMap);
        }
        dewPointMap = new ScalarMap(DewPoint.getRealType(),
                                    displayTemperatureType);

        dewPointMap.setRangeByUnits();
        mapSet.add(dewPointMap);

        addScalarMaps(mapSet);
        setDisplayActive();
    }

    /**
     * Gets the (p,T) <-> (x,y) coordinate system transformation associated
     * with this display.
     * @return          The (p,T) <-> (x,y) coordinate system transformation
     *                  associated with this display
     */
    public AerologicalCoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Set the CoordinateSystem for this display to the default for
     * the type.
     *
     * @param type  type of display (from {@link AerologicalDisplayConstants}
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setCoordinateSystem(String type)
            throws VisADException, RemoteException {
        if (type.equals(SKEWT_DISPLAY)) {
            setCoordinateSystem(SkewTCoordinateSystem.instance());
        } else if (type.equals(STUVE_DISPLAY)) {
            setCoordinateSystem(StuveCoordinateSystem.instance());
        } else if (type.equals(EMAGRAM_DISPLAY)) {
            setCoordinateSystem(EmagramCoordinateSystem.instance());
        } else {
            throw new IllegalArgumentException(
                "Unknown aerological display type " + type);
        }
    }

    /**
     * Sets the (p,T) <-> (x,y) coordinate system transformation associated
     * with this display.
     *
     * @param acs     the new (p,T) <-> (x,y) coordinate system transformation
     *                associated with this display
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating VisAD object
     */
    public void setCoordinateSystem(AerologicalCoordinateSystem acs)
            throws VisADException, RemoteException {

        if (Misc.equals(coordinateSystem, acs)) {
            return;
        }
        coordinateSystemChange(acs);
    }

    /**
     * Handle a change to the coordinate system.
     *
     * @param acs     the new (p,T) <-> (x,y) coordinate system transformation
     *                associated with this display
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating VisAD object
     */
    protected void coordinateSystemChange(AerologicalCoordinateSystem acs)
            throws VisADException, RemoteException {

        setDisplayInactive();
        box.setCoordinateSystem(acs);
        isobars.setCoordinateSystem(acs);
        isotherms.setCoordinateSystem(acs);
        dryAdiabats.setCoordinateSystem(acs);
        saturationAdiabats.setCoordinateSystem(acs);
        satMixingRatios.setCoordinateSystem(acs);
        rightPressureLabels.setCoordinateSystem(acs);
        leftPressureLabels.setCoordinateSystem(acs);
        lowerTemperatureLabels.setCoordinateSystem(acs);
        upperTemperatureLabels.setCoordinateSystem(acs);
        windStaff.setCoordinateSystem(acs);
        for (Iterator iter = winds.iterator(); iter.hasNext(); ) {
            WindBarbProfile wbp = (WindBarbProfile) iter.next();
            if (wbp != null) {
                wbp.setCoordinateSystem(acs);
            }
        }
        coordinateSystem = acs;
        setDisplayTypes();
        setDisplayActive();
    }

    /**
     * Adds a temperature and dew-point sounding to the display.  The sounding
     * will be added to the end of the set of soundings.
     * @param temperature       The temperature sounding.  Must be Pressure ->
     *                          Temperature.
     * @param dewPoint          The dew-point sounding.  Must be Pressure ->
     *                          DewPoint.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void addProfile(Field temperature, Field dewPoint)
            throws VisADException, RemoteException {
        addProfile(soundings.displayableCount(), temperature, dewPoint, null);
    }

    /**
     * Adds a temperature, dew-point, and wind sounding to the display.
     * The sounding will be added to the end of the set of soundings.
     * @param temperature       The temperature sounding.  Must be Pressure ->
     *                          Temperature.
     * @param dewPoint          The dew-point sounding.  Must be Pressure ->
     *                          DewPoint.
     * @param windProfile       The wind profile.  Must be Pressure -> Wind
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void addProfile(Field temperature, Field dewPoint,
                           Field windProfile)
            throws VisADException, RemoteException {
        addProfile(soundings.displayableCount(), temperature, dewPoint,
                   windProfile);
    }

    /**
     * Adds a temperature, dew-point and wind sounding to the display.
     * The soundings will be inserted at the given index.  The soundings
     * will be invisible.
     *
     * @param index             The index of the sounding.
     * @param temperature       The temperature sounding.  Must be Pressure ->
     *                          Temperature.
     * @param dewPoint          The dew-point sounding.  Must be Pressure ->
     *                          DewPoint.
     * @param windProfile       The wind profile.  Must be Pressure -> Wind
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void addProfile(int index, Field temperature, Field dewPoint,
                           Field windProfile)
            throws VisADException, RemoteException {


        Sounding sounding = (Sounding) soundings.getDisplayable(index);
        if(sounding==null) {
            sounding = new Sounding(getDisplay());
            soundings.addSounding(index, sounding);
        }
        WindBarbProfile windBarbs = (WindBarbProfile)winds.getDisplayable(index);
        if(windBarbs==null){
            windBarbs = new WindBarbProfile(getDisplay(),
                                        getCoordinateSystem());
            winds.addWindProfile(index, windBarbs);
        }
        sounding.setFields(temperature, dewPoint);
        //        sounding.setVisible(false);
        //        soundings.addSounding(index, sounding);
        //WindBarbProfile windBarbs = new WindBarbProfile(getDisplay(),
        //                                getCoordinateSystem());
        Field ff = vetWinds(windProfile);
        if (ff != null) {
            windBarbs.setProfile(ff);
            windStaff.setVisible(true);
        }
        windBarbs.setVisible(false);
        //winds.addWindProfile(index, windBarbs);
    }

    /**
     * Returns the number of profiles in the display.
     *
     * @return                  The number of profiles in the display.
     */
    public int profileCount() {
        return soundings.displayableCount();
    }

    /**
     * Removes all soundings from the display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void removeProfiles() throws VisADException, RemoteException {
        soundings.clear();
        winds.clear();
    }

    /**
     * Removes a given sounding from the display.  The soundings subsequent to
     * that position will have their indexes decremented by one.
     *
     * @param index             The index of the sounding.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void removeProfile(int index)
            throws VisADException, RemoteException {
        soundings.removeSounding(index);
        winds.removeWindProfile(index);
    }

    /**
     * Sets the visibility of a given sounding.
     * @param index             The sounding index.
     * @param visible           Whether or not the sounding should be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfileVisible(int index, boolean visible)
            throws VisADException, RemoteException {

        soundings.setVisible(visible, index, index);
        winds.setVisible(visible, index, index);
    }

    /**
     * Sets the "active" sounding.  This is necessary for determining the
     * trajectory of a lifted parcel, for example.
     *
     * @param index             The index of the "active" sounding.  A value of
     *                          -1 means that there is no active sounding.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setActiveSounding(int index)
            throws VisADException, RemoteException {
        soundings.setActiveSounding(index);
        winds.setActiveWindProfile(index);
        activeIndex = index;
    }

    /**
     * Resets the original sounding in the specified sounding.
     *
     * @param index             The index of the sounding.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setOriginalProfiles(int index)
            throws VisADException, RemoteException {
        soundings.setOriginalProfiles(index);
        winds.setOriginalProfile(index);
    }

    /**
     * Removes all soundings from the display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void clearProfiles() throws VisADException, RemoteException {
        soundings.clear();
        winds.clear();
        pseudoAdiabaticTrajectory.clear();
    }

    /**
     * Sets the constrainProfiles property.  When this property is set, profile
     * temperatures are constrained to be equal to or greater than their
     * corresponding profile dew-points.
     * @param yes                       Whether or not to constrain temperatures
     *                                  to be equal to or greater than
     *                                  corresponding dew-points.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setConstrainProfiles(boolean yes)
            throws VisADException, RemoteException {
        soundings.setConstrainProfiles(yes);
    }

    /**
     * Sets the VisAD-cursor pressure property.
     *
     * @param pressure          The pressure at the VisAD cursor.
     * @throws UnitException    Inappropriate pressure unit.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public final void setCursorPressure(Real pressure)
            throws UnitException, VisADException, RemoteException {

        Real oldPressure = getCursorPressure();

        soundings.setPressure(pressure);
        winds.setPressure(pressure);
        firePropertyChange(CURSOR_PRESSURE, oldPressure, pressure);
    }

    /**
     * Sets the mouse-pointer pressure property.
     *
     * @param pressure          The pressure at the mouse pointer.
     * @throws UnitException    Inappropriate pressure unit.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public final void setPointerPressure(Real pressure)
            throws UnitException, VisADException, RemoteException {

        Real oldPressure = getPointerPressure();

        pointerPressure = pressure;

        firePropertyChange(POINTER_PRESSURE, oldPressure, pressure);
    }

    /**
     * Gets the VisAD-cursor pressure property.
     *
     * @return                  The pressure at the VisAD cursor.  May be
     *                          <code>null</code>.
     */
    public Real getCursorPressure() {
        return soundings.getPressure();
    }

    /**
     * Gets the mouse-pointer pressure property.
     *
     * @return                  The pressure at the mouse pointer.  May be
     *                          <code>null</code>.
     */
    public Real getPointerPressure() {
        return pointerPressure;
    }

    /**
     * Sets the VisAD-cursor temperature property.
     *
     * @param temperature       The cursor temperature.
     * @throws UnitException    Inappropriate unit.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected final void setCursorTemperature(Real temperature)
            throws UnitException, VisADException, RemoteException {

        Real oldTemperature = cursorTemperature;

        cursorTemperature = temperature;

        pseudoAdiabaticTrajectory.recompute();
        firePropertyChange(CURSOR_TEMPERATURE, oldTemperature,
                           cursorTemperature);
    }

    /**
     * Sets the mouse-pointer temperature property.
     *
     * @param temperature       The pointer temperature.
     * @throws UnitException    Inappropriate unit.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected final void setPointerTemperature(Real temperature)
            throws UnitException, VisADException, RemoteException {

        Real oldTemperature = pointerTemperature;

        pointerTemperature = temperature;

        firePropertyChange(POINTER_TEMPERATURE, oldTemperature,
                           pointerTemperature);
    }

    /**
     * Gets the cursor temperature property.
     *
     * @return                  The currently-selected temperature.  May be
     *                          <code>null</code>.
     */
    public Real getCursorTemperature() {
        return cursorTemperature;
    }

    /**
     * Gets the pointer temperature property.
     *
     * @return                  The temperature under the mouse pointer.  May be
     *                          <code>null</code>.
     */
    public Real getPointerTemperature() {
        return pointerTemperature;
    }

    /**
     * Gets the profile temperature property.
     *
     * @return                  The profile temperature.  May be
     *                          <code>null</code>.
     */
    public Real getProfileTemperature() {
        return soundings.getTemperature();
    }

    /**
     * Gets the profile dew-point property.
     *
     * @return                  The profile dew-point.  May be
     *                          <code>null</code>.
     */
    public Real getProfileDewPoint() {
        return soundings.getDewPoint();
    }

    /**
     * Gets the profile wind speed property.
     *
     * @return                  The profile wind speed.  May be
     *                          <code>null</code>.
     */
    public Real getProfileWindSpeed() {
        return winds.getSpeed();
    }

    /**
     * Gets the profile wind direction property.
     *
     * @return                  The profile wind direction.  May be
     *                          <code>null</code>.
     */
    public Real getProfileWindDirection() {
        return winds.getDirection();
    }

    /**
     * Sets the pseudo-adiabatic-trajectory-on property.  An active sounding
     * must also be set in order to compute the trajectory.
     *
     * @param enable            Whether or not showing of the pseudo adiabatic
     *                          trajectory is enabled.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #setActiveSounding(int)
     */
    public void setPseudoAdiabaticTrajectoryEnabled(boolean enable)
            throws VisADException, RemoteException {
        pseudoAdiabaticTrajectory.setEnabled(enable);
    }

    /**
     * Gets the pseudo-adiabatic-trajectory-on property.
     *
     * @return                  Whether or not showing the pseudo adiabatic
     *                          trajectory is enabled.
     */
    public boolean isPseudoAdiabaticTrajectoryEnabled() {
        return pseudoAdiabaticTrajectory.isEnabled();
    }

    /**
     * Sets the visibility of the pseudoadiabatic trajectory.
     *
     * @param visible           If and only if <code>true</code>, then the
     *                          trajectory will be visible if it exists.
     * @throws VisADException   if a VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     */
    public void setPseudoAdiabaticTrajectoryVisible(boolean visible)
            throws VisADException, RemoteException {
        pseudoAdiabaticTrajectory.setVisible(visible);
    }

    /**
     * Returns the visibility of the pseudoadiabatic trajectory.
     *
     * @return                  True if and only if the trajectory will be
     *                          visible if it exists.
     */
    public boolean isPseudoAdiabaticTrajectoryVisible() {
        return pseudoAdiabaticTrajectory.getVisible();
    }

    /**
     * Clears the pseudo-adiabatic-trajectory property.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void clearPseudoAdiabaticTrajectory()
            throws VisADException, RemoteException {
        pseudoAdiabaticTrajectory.clear();
    }

    /**
     * Returns the minimum pressure of this display.
     * @return          The minimum pressure of this display.
     */
    public Real getMinimumPressure() {
        return coordinateSystem.getMinimumPressure();
    }

    /**
     * Returns the maximum pressure of this display.
     * @return          The maximum pressure of this display.
     */
    public Real getMaximumPressure() {
        return coordinateSystem.getMaximumPressure();
    }

    /**
     * Returns the minimum temperature of this display.
     * @return          The minimum temperature of this display.
     */
    public Real getMinimumTemperature() {
        return coordinateSystem.getMinimumTemperature();
    }

    /**
     * Returns the maximum temperature of this display.
     * @return          The maximum temperature of this display.
     */
    public Real getMaximumTemperature() {
        return coordinateSystem.getMaximumTemperature();
    }

    /**
     * Sets the visibility of the background dry adiabats.
     * @param on                Whether the background dry adiabats should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDryAdiabatVisibility(boolean on)
            throws VisADException, RemoteException {
        dryAdiabats.setVisible(on);
    }

    /**
     * Get the visibility of the background dry adiabats.
     * @return true if visible
     */
    public boolean getDryAdiabatVisibility() {
        return dryAdiabats.getVisible();
    }

    /**
     * Sets the visibility of the background saturation adiabats.
     * @param on                Whether the background saturation adiabats
     *                          should be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setSaturationAdiabatVisibility(boolean on)
            throws VisADException, RemoteException {
        saturationAdiabats.setVisible(on);
    }

    /**
     * Get the visibility of the background saturation adiabats.
     * @return true if visible
     */
    public boolean getSaturationAdiabatVisibility() {
        return saturationAdiabats.getVisible();
    }

    /**
     * Sets the visibility of the wind barb staff.
     * @param on                Whether the wind barb staff
     *                          should be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWindStaffVisibility(boolean on)
            throws VisADException, RemoteException {
        windStaff.setVisible(on);
    }

    /**
     * Get the visibility of the wind barb staff
     * @return true if visible
     */
    public boolean getWindStaffVisibility() {
        return windStaff.getVisible();
    }

    /**
     * Set the levels of the wind profile to display.
     * @param levels  the set of levels (if null, display all);
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWindLevels(Gridded1DSet levels)
            throws VisADException, RemoteException {
        winds.setWindLevels(levels);
    }

    /**
     * Sets the visibility of the background saturation adiabats.
     * @param on                Whether the background saturation adiabats
     *                          should be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setSaturationMixingRatioVisibility(boolean on)
            throws VisADException, RemoteException {
        satMixingRatios.setVisible(on);
    }

    /**
     * Get the visibility of the background saturation adiabats.
     * @return true if visible
     */
    public boolean getSaturationMixingRatioVisibility() {
        return satMixingRatios.getVisible();
    }

    /**
     * Handles a change in the position of the VisAD cursor.
     *
     * @throws RemoteException
     * @throws UnitException
     * @throws VisADException
     */
    private void cursorMoved()
            throws UnitException, VisADException, RemoteException {

        double[] cursor     = getDisplay().getDisplayRenderer().getCursor();
        Real[]   aeroCoords = aeroCoords(cursor[0], cursor[1]);

        setCursorPressure(aeroCoords[0]);
        setCursorTemperature(aeroCoords[1]);
    }

    /**
     * Handles a change in the position of the mouse-pointer.
     *
     * @param x
     * @param y
     *
     * @throws RemoteException
     * @throws UnitException
     * @throws VisADException
     */
    private void pointerMoved(int x, int y)
            throws UnitException, VisADException, RemoteException {

        /*
         * Convert from (pixel, line) Java Component coordinates to (pressure,
         * temperature) aerological coordinates.
         */
        VisADRay ray =
            getDisplay().getDisplayRenderer().getMouseBehavior().findRay(x,
                y);
        Real[] aeroCoords = aeroCoords(ray.position[0], ray.position[1]);

        setPointerPressure(aeroCoords[0]);
        setPointerTemperature(aeroCoords[1]);
    }

    /** some factor times the size */
    private void setFlowScale() {
        if (flowControl != null) {
            try {
                flowControl.setFlowScale(getBarbScale() * .02f);
            } catch (Exception e) {
                ;
            }
        }
    }

    /**
     * Set the barb orientation.
     * @param orient  orientation.
     */
    public void setBarbOrientation(int orient) {
        if ((orient != FlowControl.NH_ORIENTATION)
                && (orient != FlowControl.SH_ORIENTATION)) {
            throw new IllegalArgumentException("Unknown orientation: "
                    + orient);
        }
        if (orient != barbOrientation) {
            barbOrientation = orient;
            if (flowControl != null) {
                try {
                    flowControl.setBarbOrientation(barbOrientation);
                } catch (Exception e) {
                    ;
                }
            }
        }
    }

    /**
     * Get the barb orientation being used (NH or SH style).
     * @return  orientation ( FlowControl.NH_ORIENTATION or
     *                        FlowControl.SH_ORIENTATION)
     */
    public int getBarbOrientation() {
        return barbOrientation;
    }

    /**
     * Set the barb scaling.
     *
     * @param size  scaling of barbs (2 = default);
     */
    public void setBarbScale(int size) {

        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        if (size != barbScale) {
            barbScale = size;
            setFlowScale();
        }
    }

    /**
     * Get the barb scaling
     * @return  scaling
     */
    public int getBarbScale() {
        return barbScale;
    }

    /**
     * Vet the winds
     *
     * @param profile  profile to vet
     * @return vetted profile
     *
     * @throws RemoteException   Java RMI exception
     * @throws VisADException    VisAD problem
     */
    private Field vetWinds(Field profile)
            throws VisADException, RemoteException {

        if (profile == null) {
            return profile;
        }
        boolean isSequence = ucar.unidata.data.grid.GridUtil.isTimeSequence(
                                 (FieldImpl) profile);
        FlatField data;
        if (isSequence) {
            data = (FlatField) profile.getSample(0);
        } else {
            data = (FlatField) profile;
        }
        data = ensureCartesian(data);
        /*  moved to WindBarbProfile
        float[][] subPs = {
            {
                1000.f, 925.f, 850.f, 700.f, 500.f, 400.f, 300.f, 250.f,
                200.f, 150.f, 100.f
            }
        };
        SampledSet pSet = (SampledSet) data.getDomainSet();
        Gridded1DSet subSet = new Gridded1DSet((SetType) pSet.getType(),
                                               subPs, subPs[0].length);
        //data = (FlatField) data.resample(subSet);
        pSet = (SampledSet) data.getDomainSet();
        float[][] pressures = pSet.getSamples();
        float[][] pt        = new float[2][pressures[0].length];
        pt[0] = pressures[0];
        float[][] xy = getCoordinateSystem().toReference(pt,
                           new Unit[]{ pSet.getSetUnits()[0],
                                       CommonUnits.CELSIUS });
        Gridded1DSet xSet = new Gridded1DSet(RealType.YAxis, new float[][] {
            xy[1]
        }, xy[1].length);
        FunctionType ftype =
            new FunctionType(RealType.YAxis,
                             ((FunctionType) data.getType()).getRange());
        FlatField ff = new FlatField(ftype, xSet);
        ff.setSamples(data.getFloats());
        return ff;
        */
        return data;
    }

    /**
     * Ensures a wind profile in cartesian coordinates.
     *
     * @param input             Wind profile in cartesian or polar coordinates.
     * @return                  Wind profile in cartesian coordinates.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    private FlatField ensureCartesian(FlatField input)
            throws VisADException, RemoteException {

        FlatField    output;
        FunctionType inputFunction = (FunctionType) input.getType();

        if (inputFunction.getDomain().getComponent(0).equals(
                GeopotentialAltitude.getRealType())) {
            input = Util.convertDomain(input, AirPressure.getRealTupleType(),
                                       AirPressure.getStandardAtmosphereCS());
        }


        if (Unit.canConvert(input.getDefaultRangeUnits()[0], CommonUnit
                .meterPerSecond) && Unit
                    .canConvert(input.getDefaultRangeUnits()[1], CommonUnit
                        .meterPerSecond)) {

            output = input;

        } else {

            RealTupleType cartesianType =
                CartesianHorizontalWind.getRealTupleType();

            output =
                new FlatField(CartesianHorizontalWindOfPressure.instance(),
                              input.getDomainSet());

            RealTupleType inputType =
                (RealTupleType) ((FunctionType) input.getType()).getRange();
            ErrorEstimate[] inputErrors = input.getRangeErrors();
            ErrorEstimate[] outputErrors =
                new ErrorEstimate[inputErrors.length];

            output.setSamples(
                CoordinateSystem.transformCoordinates(
                    cartesianType, cartesianType.getCoordinateSystem(),
                    cartesianType.getDefaultUnits(), outputErrors, inputType,
                    ucar.visad.Util.getRangeCoordinateSystem(input),
                    ucar.visad.Util.getRangeUnits(input), inputErrors,
                    input.getValues()));
        }

        return output;
    }

    /**
     * Get an array of areological coordinates from x and y
     *
     * @param x    x location
     * @param y    y location
     * @return corresponding areological coordinates
     *
     * @throws RemoteException   Java RMI exception
     * @throws UnitException     Unit conversion problem
     * @throws VisADException    VisAD problem
     */
    private Real[] aeroCoords(double x, double y)
            throws UnitException, VisADException, RemoteException {

        double[][] coords =
            getCoordinateSystem().fromReference(new double[][] {
            new double[] { x }, new double[] { y }
        });

        return new Real[] {
            new Real(AirPressure.getRealType(), coords[0][0],
                     getCoordinateSystem().getCoordinateSystemUnits()[0]),
            new Real(AirTemperature.getRealType(), coords[1][0],
                     getCoordinateSystem().getCoordinateSystemUnits()[1]) };
    }

    /**
     * Provides support for labels for the pressure axis.
     */
    protected abstract static class PressureAxisLabels extends ScaleLabels {


        /**
         * Constructs from a name for the displayable.
         *
         * @param name                  The name for the displayable.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected PressureAxisLabels(String name)
                throws RemoteException, VisADException {

            super(name, RealType.YAxis);

            DecimalFormat format = new DecimalFormat("#" + "##0");

            setFormat(format);
            setXAlignment(Component.RIGHT_ALIGNMENT);

            /*
            double[]    range = new double[2];
            ScalarMap   map = new ScalarMap(RealType.YAxis, Display.YAxis);
            Display.YAxis.getRange(range);
            map.setRange(range[0], range[1]);
            addScalarMap(map);
            */
        }

        /**
         * Constructs from another instance.
         *
         * @param that              The other instance.
         * @throws VisADException if a core VisAD failure occurs.
         * @throws RemoteException if a Java RMI failure occurs.
         */
        protected PressureAxisLabels(PressureAxisLabels that)
                throws RemoteException, VisADException {
            super(that);
        }

        /**
         * Sets the labels.
         * @param coordinateSystem      The (p,T) <-> (x,y) coordinate system
         *                              transformation.
         * @param xValue                The x-value for the labels.
         * @param contourLevels         The pressure values.
         * @param temperature           The type of the temperature.
         * @param separation            The distance between the label and the
         *                              axis.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected void set(AerologicalCoordinateSystem coordinateSystem,
                           Real xValue, ContourLevels contourLevels,
                           Real temperature, double separation)
                throws RemoteException, VisADException {

            double x = xValue.getValue();
            float[] pressureValues =
                contourLevels.getLevels(
                    (float) coordinateSystem.fromReference(new double[][] {
                { x }, { coordinateSystem.getMaximumY().getValue() }
            })[0][0], (float) coordinateSystem.fromReference(new double[][] {
                { x }, { coordinateSystem.getMinimumY().getValue() }
            })[0][0]);

            float[] temperatureValues = new float[pressureValues.length];

            Arrays.fill(temperatureValues, (float) temperature.getValue());
            setPositionValues(coordinateSystem.toReference(new float[][] {
                pressureValues, temperatureValues
            })[1]);
            setLabelValues(pressureValues);
            addConstantMap(new ConstantMap(x + separation, Display.XAxis));
        }

        /**
         * Set the new coordinate system.
         *
         * @param acs   the new (p,T) <-> (x,y) coordinate system transformation
         *              associated with this display
         *
         * @throws RemoteException Java RMI Exception
         * @throws VisADException problem creating VisAD object
         */
        public void setCoordinateSystem(AerologicalCoordinateSystem acs)
                throws VisADException, RemoteException {
            this.coordinateSystemChange(acs);
        }

        /**
         * Handle a change to the (p,T) <-> (x,y) coordinate system
         * transformation associated with this display
         *
         * @param coordinateSystem   the new CoordinateSystem
         *
         * @throws RemoteException Java RMI Exception
         * @throws VisADException problem creating VisAD object
         */
        protected void coordinateSystemChange(
                AerologicalCoordinateSystem coordinateSystem)
                throws VisADException, RemoteException {}
    }

    /**
     * Provides support for the left pressure-axis labels.
     */
    protected static class LeftPressureAxisLabels extends PressureAxisLabels {

        /** contour levels */
        private ContourLevels contourLevels;

        /**
         * Constructs from a coordinate system transformation and the
         * pressure levels.
         * @param coordinateSystem      The (p,T) <-> (x,y) transformation.
         * @param contourLevels         The pressure levels.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public LeftPressureAxisLabels(
                AerologicalCoordinateSystem coordinateSystem,
                ContourLevels contourLevels)
                throws RemoteException, VisADException {

            super("LeftPressureAxisLabels");
            this.contourLevels = contourLevels;
            this.setCoordinateSystem(coordinateSystem);

        }

        /**
         * Handle a coordinate system change
         *
         * @param coordinateSystem    new CoordinateSystem
         *
         * @throws RemoteException Java RMI problem
         * @throws VisADException VisAD problem
         */
        protected void coordinateSystemChange(
                AerologicalCoordinateSystem coordinateSystem)
                throws RemoteException, VisADException {
            set(coordinateSystem, coordinateSystem.getMinimumX(),
                contourLevels, coordinateSystem.getMinimumTemperature(),
                -0.23);
        }

        /**
         * Constructs from another instance.
         * @param that                  The other instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected LeftPressureAxisLabels(LeftPressureAxisLabels that)
                throws RemoteException, VisADException {
            super(that);
            contourLevels = that.contourLevels;
        }

        /**
         * Returns a clone of this instance suitable for another VisAD display.
         * Underlying data objects are not cloned.
         * @return                      A clone of this instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public Displayable cloneForDisplay()
                throws RemoteException, VisADException {
            return new LeftPressureAxisLabels(this);
        }
    }

    /**
     * Provides support for the left pressure-axis labels.
     */
    protected static class RightPressureAxisLabels extends PressureAxisLabels {

        /** contour levels */
        private ContourLevels contourLevels;

        /**
         * Constructs from a coordinate system transformation and the
         * pressure levels.
         * @param coordinateSystem      The (p,T) <-> (x,y) transformation.
         * @param contourLevels         The pressure levels.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public RightPressureAxisLabels(
                AerologicalCoordinateSystem coordinateSystem,
                ContourLevels contourLevels)
                throws RemoteException, VisADException {

            super("RightPressureAxisLabels");
            this.contourLevels = contourLevels;
            this.setCoordinateSystem(coordinateSystem);

        }

        /**
         * Handle a coordinate system change
         *
         * @param coordinateSystem    new CoordinateSystem
         *
         * @throws RemoteException Java RMI problem
         * @throws VisADException VisAD problem
         */
        protected void coordinateSystemChange(
                AerologicalCoordinateSystem coordinateSystem)
                throws RemoteException, VisADException {
            set(coordinateSystem, coordinateSystem.getMaximumX(),
                contourLevels, coordinateSystem.getMaximumTemperature(),
                0.02);
        }

        /**
         * Constructs from another instance.
         * @param that                  The other instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected RightPressureAxisLabels(RightPressureAxisLabels that)
                throws RemoteException, VisADException {
            super(that);
            contourLevels = that.contourLevels;
        }

        /**
         * Returns a clone of this instance suitable for another VisAD display.
         * Underlying data objects are not cloned.
         * @return                      A clone of this instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public Displayable cloneForDisplay()
                throws RemoteException, VisADException {
            return new RightPressureAxisLabels(this);
        }
    }

    /**
     * Provides support for labels on the temperature axis.
     */
    protected abstract static class TemperatureAxisLabels extends ScaleLabels {

        /**
         * Constructs from a name for the displayable.
         * @param name                  The name for the displayable.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected TemperatureAxisLabels(String name)
                throws RemoteException, VisADException {

            super(name, RealType.XAxis);

            DecimalFormat format = new DecimalFormat("##0");

            setFormat(format);
            setXAlignment(Component.CENTER_ALIGNMENT);

            /*
            double[]    range = new double[2];
            ScalarMap   map = new ScalarMap(RealType.YAxis, Display.YAxis);
            Display.YAxis.getRange(range);
            map.setRange(range[0], range[1]);
            addScalarMap(map);
            */
        }

        /**
         * Constructs from another instance.
         *
         * @param that            The other instance.
         * @throws VisADException if a core VisAD failure occurs.
         * @throws RemoteException if a Java RMI failure occurs.
         */
        protected TemperatureAxisLabels(TemperatureAxisLabels that)
                throws RemoteException, VisADException {
            super(that);
        }

        /**
         * Sets the labels.
         * @param coordinateSystem      The (p,T) <-> (x,y) coordinate system
         *                              transformation.
         * @param yValue                The y-value for the labels.
         * @param contourLevels         The temperature values.
         * @param pressure              The type of the pressure.
         * @param separation            The distance between the label and the
         *                              axis.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected final void set(
                AerologicalCoordinateSystem coordinateSystem, Real yValue,
                ContourLevels contourLevels, Real pressure, double separation)
                throws RemoteException, VisADException {

            double y = yValue.getValue();
            float[] temperatureValues =
                contourLevels.getLevels(
                    (float) coordinateSystem.fromReference(new double[][] {
                { coordinateSystem.getMinimumX().getValue() }, { y }
            })[1][0], (float) coordinateSystem.fromReference(new double[][] {
                { coordinateSystem.getMaximumX().getValue() }, { y }
            })[1][0]);

            float[] pressureValues = new float[temperatureValues.length];

            Arrays.fill(pressureValues, (float) pressure.getValue());
            setPositionValues(coordinateSystem.toReference(new float[][] {
                pressureValues, temperatureValues
            })[0]);
            setLabelValues(temperatureValues);
            addConstantMap(new ConstantMap(y + separation, Display.YAxis));
        }

        /**
         * Set the new CoordinateSystem
         *
         * @param acs    new CoordinateSystem
         *
         * @throws RemoteException Java RMI problem
         * @throws VisADException VisAD problem
         */
        public void setCoordinateSystem(AerologicalCoordinateSystem acs)
                throws VisADException, RemoteException {
            this.coordinateSystemChange(acs);
        }

        /**
         * Handle a coordinate system change
         *
         * @param coordinateSystem    new CoordinateSystem
         *
         * @throws RemoteException Java RMI problem
         * @throws VisADException VisAD problem
         */
        protected void coordinateSystemChange(
                AerologicalCoordinateSystem coordinateSystem)
                throws VisADException, RemoteException {}

    }

    /**
     * Provides support for the lower temperature labels.
     */
    protected static class LowerTemperatureAxisLabels extends TemperatureAxisLabels {

        /** contour levels */
        private ContourLevels contourLevels;

        /**
         * Constructs.
         * @param coordinateSystem      The (p,T) <-> (x,y) coordinate system
         *                              transformation.
         * @param contourLevels         The label values.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public LowerTemperatureAxisLabels(
                AerologicalCoordinateSystem coordinateSystem,
                ContourLevels contourLevels)
                throws RemoteException, VisADException {

            super("LowerTemperatureAxisLabels");
            this.contourLevels = contourLevels;
            this.setCoordinateSystem(coordinateSystem);

        }

        /**
         * Handle a coordinate system change
         *
         * @param coordinateSystem    new CoordinateSystem
         *
         * @throws RemoteException Java RMI problem
         * @throws VisADException VisAD problem
         */
        protected void coordinateSystemChange(
                AerologicalCoordinateSystem coordinateSystem)
                throws RemoteException, VisADException {

            set(coordinateSystem, coordinateSystem.getMinimumY(),
                contourLevels, coordinateSystem.getMaximumPressure(), -0.08);
        }

        /**
         * Constructs from another instance.
         * @param that                  The other instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected LowerTemperatureAxisLabels(LowerTemperatureAxisLabels that)
                throws RemoteException, VisADException {
            super(that);
            contourLevels = that.contourLevels;
        }

        /**
         * Returns a clone of this instance suitable for another VisAD display.
         * Underlying data objects are not cloned.
         * @return                      A clone of this instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public Displayable cloneForDisplay()
                throws RemoteException, VisADException {
            return new LowerTemperatureAxisLabels(this);
        }
    }

    /**
     * Provides support for the lower temperature labels.
     */
    protected static class UpperTemperatureAxisLabels extends TemperatureAxisLabels {

        /** contour levels */
        private ContourLevels contourLevels;

        /**
         * Constructs.
         * @param coordinateSystem      The (p,T) <-> (x,y) coordinate system
         *                              transformation.
         * @param contourLevels         The label values.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public UpperTemperatureAxisLabels(
                AerologicalCoordinateSystem coordinateSystem,
                ContourLevels contourLevels)
                throws RemoteException, VisADException {

            super("UpperTemperatureAxisLabels");
            this.contourLevels = contourLevels;
            this.setCoordinateSystem(coordinateSystem);

        }

        /**
         * Handle a coordinate system change
         *
         * @param coordinateSystem    new CoordinateSystem
         *
         * @throws RemoteException Java RMI problem
         * @throws VisADException VisAD problem
         */
        protected void coordinateSystemChange(
                AerologicalCoordinateSystem coordinateSystem)
                throws RemoteException, VisADException {

            set(coordinateSystem, coordinateSystem.getMaximumY(),
                contourLevels, coordinateSystem.getMinimumPressure(), 0.02);
        }

        /**
         * Constructs from another instance.
         * @param that                  The other instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        protected UpperTemperatureAxisLabels(UpperTemperatureAxisLabels that)
                throws RemoteException, VisADException {
            super(that);
            contourLevels = that.contourLevels;
        }

        /**
         * Returns a clone of this instance suitable for another VisAD display.
         * Underlying data objects are not cloned.
         * @return                      A clone of this instance.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public Displayable cloneForDisplay()
                throws RemoteException, VisADException {
            return new UpperTemperatureAxisLabels(this);
        }
    }

    /**
     * Provides support for displaying the trajectory of a lifted parcel.
     */
    protected class DisplayablePseudoAdiabaticTrajectory extends CompositeDisplayable {

        /**
         * The dry portion of the trajectory.
         */
        private DryTrajectory dryTrajectory;

        /**
         * The saturated portion of the trajectory.
         */
        private SaturationTrajectory wetTrajectory;

        /**
         * The saturation mixing-ratio segment to the saturation point.
         */
        private MixingRatioTrajectory mixingRatioTrajectory;

        /**
         * Whether or not computation of the trajectory is enabled.
         */
        private boolean isEnabled = false;

        /**
         * The type of the pressure.
         */
        private RealType pressureType;

        /**
         * The type of the temperature.
         */
        private RealType temperatureType;

        /**
         * Constructs from a VisAD display and the display types.
         * @param display               The VisAD display.
         * @param displayPressureType   The display pressure type.
         * @param displayTemperatureType        The display temperature type.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public DisplayablePseudoAdiabaticTrajectory(LocalDisplay display,
                DisplayRealType displayPressureType,
                DisplayRealType displayTemperatureType)
                throws VisADException, RemoteException {

            super(display);

            pressureType          = AirPressure.getRealType();
            temperatureType       = AirTemperature.getRealType();
            dryTrajectory         = new DryTrajectory();
            mixingRatioTrajectory = new MixingRatioTrajectory();
            wetTrajectory = new SaturationTrajectory(
                new MyWetTemperatureCalculatorFactory());

            dryTrajectory.setLineWidth(2);
            dryTrajectory.setHSV(0, 0.2, 1.0);            // light red
            mixingRatioTrajectory.setLineWidth(2);
            mixingRatioTrajectory.setHSV(120, 0.2, 1.0);  // light green
            wetTrajectory.setLineWidth(2);
            wetTrajectory.setHSV(240, 0.2, 1.0);          // light blue
            wetTrajectory.addPropertyChangeListener(wetTrajectory.TRAJECTORY,
                    new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {

                    try {
                        Sounding sounding = soundings.getActiveSounding();

                        capeCalculator.setCape(
                            sounding.getTemperatureField(),
                            sounding.getDewPointField(),
                            (FlatField) event.getNewValue());
                    } catch (Exception e) {
                        System.err.println(
                            getClass().getName() + ".propertyChange(): "
                            + "Couldn't handle change to parcel's saturation-trajectory: "
                            + e);
                    }
                }
            });
            addDisplayable(dryTrajectory);
            addDisplayable(mixingRatioTrajectory);
            addDisplayable(wetTrajectory);
        }

        /**
         * Enable or disable computation of the trajectory.
         * @param enable                Whether or not to enable computation of
         *                              the trajectory.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void setEnabled(boolean enable)
                throws VisADException, RemoteException {

            isEnabled = enable;

            // this.setVisible(enable);
        }

        /**
         * Indicates whether or not computation of the trajectory is enabled.
         * @return                      <code>true</code> if and only if
         *                              computation of the trajectory is
         *                              enabled.
         */
        public boolean isEnabled() {
            return isEnabled;
        }

        /**
         * Returns the type of the pressure.
         * @return              The type of the pressure.
         */
        public RealType getPressureType() {
            return dryTrajectory.getPressureType();
        }

        /**
         * Returns the type of the temperature.
         * @return              The type of the temperature.
         */
        public RealType getTemperatureType() {
            return dryTrajectory.getTemperatureType();
        }

        /**
         * Clears this displayable.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void clear() throws VisADException, RemoteException {

            dryTrajectory.clear();
            mixingRatioTrajectory.clear();
            wetTrajectory.clear();
        }

        /**
         * Computes the trajectory of the lifted parcel.
         * @throws TypeException        Something has the wrong type.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public void recompute()
                throws TypeException, RemoteException, VisADException {

            if (isEnabled) {
                Real startingPressure    = getCursorPressure();
                Real startingTemperature = getCursorTemperature();
                Real startingDewPoint    = getProfileDewPoint();
                Real minPressure         = getMinimumPressure();

                if ((startingPressure == null)
                        || (startingTemperature == null)
                        || (startingDewPoint == null)
                        || (minPressure == null)) {
                    clear();
                } else {
                    Real saturationPointMixingRatio =
                        (Real) WaterVaporMixingRatio.create(startingPressure,
                            startingDewPoint);
                    Real saturationPointTemperature =
                        (Real) SaturationPointTemperature.create(
                            startingPressure, startingTemperature,
                            saturationPointMixingRatio);
                    Real saturationPointPressure =
                        (Real) SaturationPointPressure.create(
                            startingPressure, startingTemperature,
                            saturationPointTemperature);

                    dryTrajectory.setTrajectory(startingPressure,
                            startingTemperature, saturationPointPressure);
                    mixingRatioTrajectory.setTrajectory(
                        saturationPointPressure, saturationPointTemperature,
                        startingPressure);
                    wetTrajectory.setTrajectory(saturationPointPressure,
                            saturationPointTemperature, minPressure);
                }
            }
        }
    }

    /**
     * Provides support for creating calculators of the saturated portion of a
     * lifted parcel's trajectory.
     */
    protected class MyWetTemperatureCalculatorFactory implements TemperatureCalculatorFactory {

        /**
         * Constructs from nothing.
         */
        public MyWetTemperatureCalculatorFactory() {}

        /**
         * Constructs from the saturation point.
         *
         * @param saturationPressure    The saturation pressure.
         * @param saturationTemperature The saturation temperature.
         * @return                      A calculator.
         * @throws TypeException        Argument has wong type.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public TemperatureCalculator newTemperatureCalculator(
                Real saturationPressure, Real saturationTemperature)
                throws TypeException, VisADException, RemoteException {
            return new MyWetTemperatureCalculator(saturationPressure,
                    saturationTemperature);
        }
    }

    /**
     * Provides support for calculating the saturated portion of a lifted
     * parcel's trajectory.
     */
    protected class MyWetTemperatureCalculator implements TemperatureCalculator {

        /** value for saturation equivalent potential temp */
        private Real satEquivPotTemp;

        /**
         * Constructs from the saturation point.
         * @param saturationPressure    The saturation pressure.
         * @param saturationTemperature The saturation temperature.
         * @throws TypeException        Argument has wong type.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public MyWetTemperatureCalculator(Real saturationPressure,
                                          Real saturationTemperature)
                throws TypeException, VisADException, RemoteException {

            satEquivPotTemp =
                (Real) SaturationEquivalentPotentialTemperature.create(
                    saturationPressure, saturationTemperature);
        }

        /**
         * Returns the next temperature in the trajectory.
         *
         * @param nextPressure          The next pressure in the trajectory.
         * @return                      The next temperature.
         * @throws VisADException       VisAD failure.
         */
        public Real nextTemperature(Real nextPressure) throws VisADException {
            return saturationAdiabats.getTemperature(nextPressure,
                    satEquivPotTemp);
        }
    }

    /**
     * Internal class for displaying a wind staff
     *
     * @author IDV Development Team
     * @version $Revision: 1.53 $
     */
    protected class MyWindStaff extends ProfileLine {

        /**
         * Create a new wind staff for the display.
         *
         * @param acs   AerologicalCoordinateSystem
         *
         * @throws RemoteException      Java RMI failure
         * @throws VisADException       VisAD failure.
         */
        public MyWindStaff(AerologicalCoordinateSystem acs)
                throws VisADException, RemoteException {

            super("WindStaff");
            setColor(java.awt.Color.yellow);
            addConstantMap(new ConstantMap(WIND_STAFF_XPOS, Display.XAxis));
            setCoordinateSystem(acs);
        }

        /**
         * Set the data based on the coordinate system
         *
         * @param acs   AerologicalCoordinateSystem
         *
         * @throws RemoteException      Java RMI failure
         * @throws VisADException       VisAD failure.
         */
        public void setCoordinateSystem(AerologicalCoordinateSystem acs)
                throws VisADException, RemoteException {
            setData(new Linear1DSet(RealType.YAxis,
                                    acs.getMinimumY().getValue(),
                                    acs.getMaximumY().getValue(), 2));
        }
    }

    /**
     * Create a new AerologicalDisplay of the given type
     *
     * @param type  type of display (from {@link AerologicalDisplayConstants}
     *
     * @return a new AerologicalDisplay of the given type
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static AerologicalDisplay getInstance(String type)
            throws VisADException, RemoteException {
        return getInstance(type, null);
    }

    /**
     * Create a new AerologicalDisplay of the given type
     *
     * @param type  type of display (from {@link AerologicalDisplayConstants}
     * @param gc    GraphicsConfiguration
     *
     * @return a new AerologicalDisplay of the given type
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static AerologicalDisplay getInstance(String type,
            GraphicsConfiguration gc)
            throws VisADException, RemoteException {

        if (type.equals(SKEWT_DISPLAY)) {
            //return new SkewTDisplay();
            return getInstance(SkewTCoordinateSystem.instance(), gc);
        } else if (type.equals(STUVE_DISPLAY)) {
            //return new StuveDisplay();
            return getInstance(StuveCoordinateSystem.instance(), gc);
        } else if (type.equals(EMAGRAM_DISPLAY)) {
            return getInstance(EmagramCoordinateSystem.instance(), gc);
        } else {
            throw new IllegalArgumentException(
                "Unknown aerological display type " + type);
        }
    }

    /**
     * Create a new AerologicalDisplay with the specified CoordinateSystem
     *
     * @param acs   AerologcialCoordinateSystem
     * @param gc    The GraphicsConfiguration for the display
     *
     * @return a new AerologicalDisplay with the specified CoordinateSystem
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static AerologicalDisplay getInstance(
            AerologicalCoordinateSystem acs, GraphicsConfiguration gc)
            throws VisADException, RemoteException {
        return new AerologicalDisplay(acs, gc);
    }

    /**
     * Get the active sounding.
     * @return the active sounding
     */
    public Sounding getActiveSounding() {
        return soundings.getActiveSounding();
    }

    /**
     * Sets the "foreground" color of this VisAD display
     *
     * @param color  color to use
     */
    public void setForeground(Color color) {

        super.setForeground(color);
        setLabelColor(color);
        setWindColor(color);
        setBackgroundLineColor(color);
    }


    /**
     * Set the label color
     *
     * @param color  label color
     */
    public void setLabelColor(Color color) {

        try {
            if (leftPressureLabels != null) {
                leftPressureLabels.setColor(color);
                rightPressureLabels.setColor(color);
                lowerTemperatureLabels.setColor(color);
                upperTemperatureLabels.setColor(color);
            }
        } catch (VisADException ve) {}
        catch (RemoteException re) {}
    }

    /**
     * Set the wind flag color
     *
     * @param color  the color 
     */
    public void setWindColor(Color color) {
        try {
            if (winds != null) {
                winds.setColor(color);
            }
        } catch (VisADException ve) {}
        catch (RemoteException re) {}
    }

    /**
     * Set the background line colors
     *
     * @param color the color
     */
    public void setBackgroundLineColor(Color color) {
        try {
            if (isotherms != null) {
                box.setColor(color);
                isobars.setColor(color);
                isotherms.setColor(color);
            }
        } catch (VisADException ve) {}
        catch (RemoteException re) {}
    }

}


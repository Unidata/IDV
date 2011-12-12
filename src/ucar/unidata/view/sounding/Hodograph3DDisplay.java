/*
 * $Id: Hodograph3DDisplay.java,v 1.25 2005/05/13 18:33:30 jeffmc Exp $
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


import ucar.visad.display.*;
import ucar.visad.functiontypes.*;
import ucar.visad.quantities.*;

import visad.*;

import visad.data.units.DefaultUnitsDB;

import visad.java3d.*;



import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;

import javax.media.j3d.Transform3D;

import javax.swing.*;


/**
 * Provides support for displaying a 3D wind hodograph.
 *
 * @author Don Murray
 * @author Steven R. Emmerson
 * @version $Id: Hodograph3DDisplay.java,v 1.25 2005/05/13 18:33:30 jeffmc Exp $
 */
public class Hodograph3DDisplay extends WindProfileDisplay {

    /**
     * The default maximum wind speed.
     */
    private static Real defaultMaxSpeed;

    /**
     * The set of display altitudes.
     */
    private Gridded1DSet displayAltitudes;

    /**
     * The maximum, displayed speed.
     */
    private Real maxDisplaySpeed;

    /**
     * The speed increment between displayed rings.
     */
    private Real ringIncrement;

    /**
     * The set of ring speeds.
     */
    private Linear1DSet ringSpeeds;

    /**
     * Whether or not to autoscale the wind speed.
     */
    private boolean autoscaleSpeed;

    /**
     * The displayed speed unit.
     */
    private Unit speedUnit;

    /**
     * The lower set of displayed rings.
     */
    private RingSet lowerRingSet;

    /**
     * The upper set of displayed rings.
     */
    private RingSet upperRingSet;

    /**
     * The set of intermediate-level rings.
     */
    private IntermediateRings intermediateRings;

    /**
     * The speed labels.
     */
    private SpeedLabels speedLabels;

    /**
     * The compass labels.
     */
    private CompassLabels compassLabels;

    /**
     * The lower cross hair.
     */
    private CrossHair lowerCrossHair;

    /**
     * The upper cross hair.
     */
    private CrossHair upperCrossHair;

    /**
     * The center pole.
     */
    private CenterPole centerPole;

    /**
     * The westerly-wind ScalarMap.
     */
    private ScalarMap westerlyMap;

    /**
     * The southerly-wind ScalarMap.
     */
    private ScalarMap southerlyMap;

    /** missing mean wind trace */
    private static MeanWindTrace missingMeanWindTrace;

    static {
        try {
            missingMeanWindTrace = new MeanWindTrace();
        } catch (Exception e) {
            System.err.println(
                "Hodograph3DDisplay.<clinit>: Couldn't initialize class: "
                + e);
        }
    }

    /**
     * Default constructor.  Use default min, max geopotential altitudes
     * and default max wind speed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Hodograph3DDisplay() throws VisADException, RemoteException {
        this(getDefaultMinAltitude(), getDefaultMaxAltitude(),
             getDefaultMaxSpeed());
    }

    /**
     * Constructs with given altitude extent and maximum wind speed.
     * @param minZ              The minimum displayed altitude.
     * @param maxZ              The maximum displayed altitude.
     * @param maxW              The maximum wind speed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Hodograph3DDisplay(Real minZ, Real maxZ, Real maxW)
            throws VisADException, RemoteException {

        super(new DisplayImplJ3D("Hodograph3D",
                                 new HodographDisplayRendererJ3D()), minZ,
                                     maxZ, 7, Display.ZAxis);

        maxDisplaySpeed = maxW;
        autoscaleSpeed  = true;
        speedUnit       = maxW.getUnit();
        ringIncrement   = maxW;

        // Set up the display.  Set projection policy to non-perspective.
        DisplayImpl         display  = (DisplayImpl) getDisplay();
        GraphicsModeControl gmc      = display.getGraphicsModeControl();
        DisplayRenderer     dispRend = display.getDisplayRenderer();
        gmc.setProjectionPolicy(0);

        ((DisplayRendererJ3D) dispRend).addKeyboardBehavior(
            new KeyboardBehaviorJ3D((DisplayRendererJ3D) dispRend));

        // Have display fill up the screen.
        {
            ProjectionControl pc        = getDisplay().getProjectionControl();
            Transform3D       transform = new Transform3D(pc.getMatrix());

            transform.setScale(.75);

            double[] matrix = new double[16];

            transform.get(matrix);

            // pc.setMatrix(matrix);
            ((JPanel) getDisplay().getComponent()).setPreferredSize(
                new Dimension(400, 400));
        }
        saveProjection();

        /*
         * Wind ScalarMap-s:
         */
        westerlyMap = new ScalarMap(WesterlyWind.getRealType(),
                                    Display.XAxis);

        westerlyMap.setScaleEnable(false);

        // westerlyMap.setRangeByUnits();
        addScalarMap(westerlyMap);

        southerlyMap = new ScalarMap(SoutherlyWind.getRealType(),
                                     Display.YAxis);

        southerlyMap.setScaleEnable(false);

        // southerlyMap.setRangeByUnits();
        addScalarMap(southerlyMap);

        /*
         * Upper and Lower Speed Ring Sets:
         */
        RealTupleType polarType = PolarHorizontalWind.getRealTupleType();

        lowerRingSet = new RingSet("lowerRingSet", polarType);

        lowerRingSet.addConstantMap(new ConstantMap(-1, Display.ZAxis));
        lowerRingSet.setHSV(0, 0, 1);
        addDisplayable(lowerRingSet);

        upperRingSet = new RingSet("upperRingSet", polarType);

        upperRingSet.addConstantMap(new ConstantMap(1, Display.ZAxis));
        upperRingSet.setHSV(0, 0, 1);
        addDisplayable(upperRingSet);

        intermediateRings = new IntermediateRings(polarType);

        addDisplayable(intermediateRings);

        /*
         * Speed Labels:
         */
        speedLabels = new SpeedLabels();

        speedLabels.addConstantMap(new ConstantMap(-1, Display.ZAxis));
        speedLabels.addConstantMap(new ConstantMap(0, Display.YAxis));
        addDisplayable(speedLabels);

        /*
         * Compass Labels:
         */
        compassLabels = new CompassLabels();

        compassLabels.addConstantMap(new ConstantMap(-1, Display.ZAxis));
        addDisplayable(compassLabels);

        // Cross Hairs:
        lowerCrossHair =
            new CrossHair("lowerCrossHair",
                          PolarHorizontalWind.getRealTupleType());

        lowerCrossHair.addConstantMap(new ConstantMap(-1, Display.ZAxis));
        addDisplayable(lowerCrossHair);

        upperCrossHair =
            new CrossHair("upperCrossHair",
                          PolarHorizontalWind.getRealTupleType());

        upperCrossHair.addConstantMap(new ConstantMap(1, Display.ZAxis));
        addDisplayable(upperCrossHair);

        // Center Pole:
        centerPole = new CenterPole("centerPole",
                                    GeopotentialAltitude.getRealType());

        addDisplayable(centerPole);

        // Wind profiles:
        getWindProfileSet().addPropertyChangeListener(
            WindProfileSet.MAXIMUM_SPEED, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {
                    setMaxDisplaySpeed();
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to maximum profile speed: "
                        + e);
                }
            }
        });

        setRingSpeeds();
        setWindScalarMapRanges();
        setCenterPoleExtent();
        setDisplayAltitudes();
    }

    /**
     * Returns the set of spatial ScalarMap-s associated with this display.
     * @return                  The set of spatial ScalarMap-s associated with
     *                          this display.
     * protected java.util.Set getSpatialScalarMapSubSet()
     * {
     *   java.util.TreeSet       set = new TreeSet();
     *   set.add(westerlyMap);
     *   set.add(southerlyMap);
     *   return set;
     * }
     *
     * @throws VisADException
     */

    /**
     * Returns the default, maximum displayed wind speed.
     * @return                  The default, maximum displayed wind speed.
     * @throws VisADException   VisAD failure.
     */
    public static Real getDefaultMaxSpeed() throws VisADException {

        if (defaultMaxSpeed == null) {
            synchronized (Hodograph3DDisplay.class) {
                if (defaultMaxSpeed == null) {
                    try {
                        defaultMaxSpeed = new Real(Speed.getRealType(), 100,
                                DefaultUnitsDB.instance().get("kt"));
                    } catch (Exception e) {
                        throw new VisADException(e.getMessage());
                    }
                }
            }
        }

        return defaultMaxSpeed;
    }

    /**
     * Toggle automatic speed-scaling.  When autoscaling is on, the speed scale
     * will automatically be adjusted when a new profile is set in the display.
     *
     * @param  value   Autoscale speed if true, otherwise keep speed scale
     *                 the same.
     */
    public void setAutoscaleSpeed(boolean value) {
        autoscaleSpeed = value;
    }

    /**
     * Get the state of automatic speed-scaling.
     *
     * @return  true if automatic speed-scaling is on, otherwise false
     */
    public boolean isAutoscaleSpeed() {
        return autoscaleSpeed;
    }

    /**
     * Constructs the displayable WindProfile appropriate to this
     * WindProfileDisplay.  Template method.
     * @return                  The displayable WindProfile appropriate to this
     *                          instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected WindProfile newWindProfile()
            throws VisADException, RemoteException {
        return new WindTrace(getDisplay());
    }

    /**
     * Display or hide the background
     *
     * @param  b   display background if true, otherwise hide the background
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setBackgroundVisible(boolean b)
            throws VisADException, RemoteException {

        lowerRingSet.setVisible(b);
        upperRingSet.setVisible(b);
        lowerCrossHair.setVisible(b);
        upperCrossHair.setVisible(b);
        centerPole.setVisible(b);
        intermediateRings.setVisible(b);
        super.setBackgroundVisible(b);
    }

    /**
     * Sets the visiblity of the compass labels.
     * @param visible           Whether or not the displayable should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setCompassLabelsVisible(boolean visible)
            throws VisADException, RemoteException {
        compassLabels.setVisible(visible);
    }

    /**
     * Sets the visiblity of the speed labels.
     * @param visible           Whether or not the displayable should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setSpeedLabelsVisible(boolean visible)
            throws VisADException, RemoteException {
        speedLabels.setVisible(visible);
    }

    /**
     * Sets the visiblity of the cross hairs.
     * @param visible           Whether or not the displayable should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setCrossHairsVisible(boolean visible)
            throws VisADException, RemoteException {
        lowerCrossHair.setVisible(visible);
        upperCrossHair.setVisible(visible);
    }

    /**
     * Sets the visiblity of the upper and lower rings.
     * @param visible           Whether or not the displayable should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRingsVisible(boolean visible)
            throws VisADException, RemoteException {
        lowerRingSet.setVisible(visible);
        upperRingSet.setVisible(visible);
    }

    /**
     * Sets the visiblity of the intermediate-level rings.
     * @param visible           Whether or not the displayable should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setIntermediateRingsVisible(boolean visible)
            throws VisADException, RemoteException {
        intermediateRings.setVisible(visible);
    }

    /**
     * Sets the visiblity of the center pole.
     * @param visible           Whether or not the displayable should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setCenterPoleVisible(boolean visible)
            throws VisADException, RemoteException {
        centerPole.setVisible(visible);
    }

    /**
     * Sets the displayed altitudes.
     * @param altitudes         The altitudes to display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setDisplayAltitudes(Gridded1DSet altitudes)
            throws VisADException, RemoteException {

        if ( !altitudes.equals(displayAltitudes)) {
            displayAltitudes = altitudes;

            int       sampleCount = Math.max(0, altitudes.getLength(0) - 2);
            float[][] samples     = new float[1][sampleCount];

            System.arraycopy(altitudes.getSamples()[0], 1, samples[0], 0,
                             sampleCount);
            intermediateRings.setAltitudes(
                new Gridded1DSet(
                    altitudes.getType(), samples, sampleCount,
                    (CoordinateSystem) null, altitudes.getSetUnits(),
                    (ErrorEstimate[]) null));
        }
    }

    /**
     * Sets the extents of the wind ScalarMap-s.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setWindScalarMapRanges()
            throws RemoteException, VisADException {

        double maxSpeed;

        maxSpeed = getMaxDisplaySpeed().getValue(
            ((RealType) westerlyMap.getScalar()).getDefaultUnit());

        westerlyMap.setRange(-maxSpeed, maxSpeed);

        maxSpeed = getMaxDisplaySpeed().getValue(
            ((RealType) southerlyMap.getScalar()).getDefaultUnit());

        southerlyMap.setRange(-maxSpeed, maxSpeed);
    }

    /**
     * Sets the extent of the center pole
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setCenterPoleExtent()
            throws RemoteException, VisADException {
        centerPole.setExtent(getMinDisplayAltitude(),
                             getMaxDisplayAltitude());
    }

    /**
     * Sets the displayed altitudes.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setDisplayAltitudes()
            throws VisADException, RemoteException {

        Unit   unit   = getAltitudeUnit();
        double min    = getMinDisplayAltitude().getValue(unit);
        double max    = getMaxDisplayAltitude().getValue(unit);
        double extent = max - min;

        if ( !Double.isNaN(extent) && !Double.isInfinite(extent)
                && (extent > 0)) {
            setDisplayAltitudes(
                new Linear1DSet(
                    GeopotentialAltitude.getRealType(), min, max,
                    1 + (int) Math.round(
                        extent
                        / computeIncrement(
                            extent, 5)), (CoordinateSystem) null,
                                         new Unit[] { unit },
                                         (ErrorEstimate[]) null));
        }
    }

    /**
     * Sets the ring speeds.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setRingSpeeds() throws RemoteException, VisADException {

        Unit   spdUnit   = getSpeedUnit();
        double max       = getMaxDisplaySpeed().getValue(spdUnit);
        double increment = computeIncrement(max, 5);

        setRingSpeeds(new Linear1DSet(PolarHorizontalWind.getSpeedRealType(),
                                      increment, max,
                                      (int) Math.round(max / increment),
                                      null, new Unit[] { spdUnit }, null));
    }

    /**
     * Sets the ring speeds from a set of speeds.
     * @param speeds            The set of speeds for the rings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRingSpeeds(Linear1DSet speeds)
            throws VisADException, RemoteException {

        if ( !speeds.equals(ringSpeeds)) {
            ringSpeeds = speeds;

            setSpeedRings();
            setSpeedLabels();
            setCrossHairs();
        }
    }

    /**
     * Returns the set of ring speeds.
     * @return                  The set of ring speeds.
     */
    public Linear1DSet getRingSpeeds() {
        return ringSpeeds;
    }

    /**
     * Sets the speed-rings for the background based on the maximum wind speed
     * for the display and the display speed unit.
     *
     * @throws   VisADException    necessary VisAD object couldn't be created.
     * @throws   RemoteException   Java RMI Exception
     */
    protected void setSpeedRings() throws VisADException, RemoteException {

        Linear1DSet speeds = getRingSpeeds();

        lowerRingSet.setRingValues(speeds);
        upperRingSet.setRingValues(speeds);
        intermediateRings
            .setRingSpeeds(new Gridded1DSet((RealType) ((SetType) speeds
                .getType()).getDomain().getComponent(0), speeds
                    .indexToValue(speeds.valueToIndex(new float[][] {
            { speeds.getHiX() / 2 }
        })), 1, (CoordinateSystem) null, speeds.getSetUnits(),
             (ErrorEstimate[]) null));
    }

    /**
     * Sets the cross hairs.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setCrossHairs() throws VisADException, RemoteException {
        lowerCrossHair.setHairs(getMaxDisplaySpeed());
        upperCrossHair.setHairs(lowerCrossHair);
    }

    /**
     * Sets the speed labels.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setSpeedLabels() throws VisADException, RemoteException {
        speedLabels.setLabels(getRingSpeeds());
    }

    /**
     * Handles a change to the extent of the displayed altitude.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void displayAltitudeExtentChange()
            throws VisADException, RemoteException {

        setAltitudeColorMapRange();
        setCenterPoleExtent();
        setDisplayAltitudes();
    }

    /**
     * Returns the maximum profile wind speed.
     * @return                  The maximum profile wind speed.
     */
    public Real getMaxProfileSpeed() {
        return getWindProfileSet().getMaximumSpeed();
    }

    /**
     * Resets the original sounding in the specified sounding.
     *
     * @param index             The index of the sounding.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setOriginalProfile(int index)
            throws VisADException, RemoteException {
        getWindProfileSet().setOriginalProfile(index);
    }

    /**
     * Sets the maximum, displayed, wind speed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setMaxDisplaySpeed()
            throws VisADException, RemoteException {

        if (isAutoscaleSpeed()) {
            Unit   unit = getSpeedUnit();
            double spd  = getMaxProfileSpeed().getValue(unit);
            double inc  = getRingIncrement().getValue(unit);

            if ( !Double.isNaN(spd) && !Double.isInfinite(spd) && (spd > 0)
                    && !Double.isNaN(inc) && !Double.isInfinite(inc)
                    && (inc > 0)) {
                Real max = new Real(Speed.getRealType(),
                                    Math.ceil(spd / inc) * inc, unit);
                setMaxDisplaySpeed(max);
            }
        }
    }

    /**
     * Sets the maximum, displayed, wind speed from a Real.
     * @param speed             the new, maximum, displayed, wind speed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setMaxDisplaySpeed(Real speed)
            throws VisADException, RemoteException {

        if ( !speed.equals(maxDisplaySpeed)) {
            maxDisplaySpeed = speed;

            setWindScalarMapRanges();
            if (isAutoscaleSpeed()) {
                setRingIncrement();
            }
            setRingSpeeds();
        }
    }

    /**
     * Returns the maximum, displayed, wind speed.
     * @return                  The maximum, displayed, wind speed.
     */
    public Real getMaxDisplaySpeed() {
        return maxDisplaySpeed;
    }

    /**
     * Sets the speed increment between rings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setRingIncrement() throws VisADException, RemoteException {

        Unit   spdUnit = getSpeedUnit();
        double speed   = getMaxProfileSpeed().getValue(spdUnit);

        if ( !Double.isNaN(speed) && !Double.isInfinite(speed)
                && (speed > 0)) {
            double increment = computeIncrement(speed, 5);

            setRingIncrement(new Real(PolarHorizontalWind.getSpeedRealType(),
                                      increment, spdUnit));
        }
    }

    /**
     * Sets the speed increment between rings.
     * @param increment         The speed increment between rings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRingIncrement(Real increment)
            throws VisADException, RemoteException {

        if ( !increment.equals(ringIncrement)) {
            ringIncrement = increment;

            setRingSpeeds();
        }
    }

    /**
     * Returns the speed increment between rings.
     *
     * @return        The increment between rings.
     */
    public Real getRingIncrement() {
        return ringIncrement;
    }

    /**
     * Sets the displayed, speed unit.
     * @param unit              The displayed, speed unit.
     * @throws UnitException    Argument has invalid unit.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setSpeedUnit(Unit unit)
            throws UnitException, VisADException, RemoteException {

        if ( !Unit.canConvert(unit, CommonUnit.meterPerSecond)) {
            throw new UnitException("\"speed\" unit (" + unit + ") isn't");
        }

        if ( !unit.equals(speedUnit)) {
            speedUnit = unit;

            setRingIncrement();
            setMaxDisplaySpeed();
        }
    }

    /**
     * Returns the displayed, speed unit.
     * @return                  The displayed, speed unit.
     */
    public Unit getSpeedUnit() {
        return speedUnit;
    }

    /**
     * Sets the cursor position.
     * @param position          The (speed,direction,altitude) cursor position
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setCursorPosition(double[] position)
            throws VisADException, RemoteException {
        setGeopotentialAltitude(new Real(GeopotentialAltitude.getRealType(),
                                         position[2]));
    }

    /**
     * Returns a MeanWind Displayable corresponding to a mean-wind Tuple.
     * @param meanWind          the mean-wind Tuple
     * @return                  The MeanWind Displayable corresponding to the
     *                          input mean-wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Displayable newMeanWind(Tuple meanWind)
            throws VisADException, RemoteException {

        MeanWindTrace meanWindTrace = new MeanWindTrace();

        meanWindTrace.setWind(meanWind);

        return meanWindTrace;
    }

    /**
     * Returns a MeanWind Displayable corresponding to a data reference for a
     * mean-wind Tuple.
     * @param meanWindRef       The data reference for the mean-wind Tuple
     * @return                  The MeanWind Displayable corresponding to the
     *                          input mean-wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Displayable newMeanWind(DataReference meanWindRef)
            throws VisADException, RemoteException {

        MeanWindTrace meanWindTrace = new MeanWindTrace();

        meanWindTrace.setWind(meanWindRef);

        return meanWindTrace;
    }

    /**
     * Returns the MeanWind Displayable with a missing mean-wind.
     * @return                  The MeanWind Displayable with a missing
     *                          mean-wind.
     */
    protected Displayable newMeanWind() {
        return missingMeanWindTrace;
    }

    /**
     * Tests this class.
     * @param args              Test arguments. Ignored.
     * @throws Exception        Something went wrong.
     */
    public static void main(String[] args) throws Exception {

        JFrame             frame = new JFrame("3D Hodograph");
        Hodograph3DDisplay h3d   = new Hodograph3DDisplay();

        // Create an artificial flat field
        float[][] levels = new float[1][10];
        float[][] uv     = new float[2][10];

        for (int i = 0; i < 10; i++) {
            levels[0][i] = i * 500.f;
            uv[0][i]     = (i % 2 == 0)
                           ? i * 2.f
                           : -i * 2.f;
            uv[1][i]     = (i % 3 == 0)
                           ? -i * 2.f
                           : i * 2.f;
        }

        Field field =
            new FlatField(
                CartesianHorizontalWindOfGeopotentialAltitude.instance(),
                new Gridded1DSet(
                    GeopotentialAltitude.getRealTupleType(), levels, 10));

        field.setSamples(uv);
        h3d.addProfile(0, field);
        h3d.setProfileVisible(0, true);
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.getContentPane().add(h3d.getComponent());
        frame.setSize(512, 512);
        frame.pack();
        frame.setVisible(true);
        h3d.draw();
    }
}


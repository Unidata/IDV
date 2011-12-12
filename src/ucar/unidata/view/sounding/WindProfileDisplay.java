/*
 * $Id: WindProfileDisplay.java,v 1.27 2005/05/13 18:33:41 jeffmc Exp $
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



import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;

import javax.swing.*;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.functiontypes.CartesianHorizontalWindOfGeopotentialAltitude;
import ucar.visad.quantities.*;

import visad.*;
import visad.data.units.DefaultUnitsDB;

import visad.java3d.*;


/**
 * Provides support for displaying vertical profiles of the horizontal wind.
 *
 * @author Don Murray
 * @author Steven R. Emmerson
 * @version $Id: WindProfileDisplay.java,v 1.27 2005/05/13 18:33:41 jeffmc Exp $
 */
public abstract class WindProfileDisplay extends DisplayMaster {

    /**
     * The name of the geopotential altitude property.
     */
    public static String GEOPOTENTIAL_ALTITUDE = "geopotentialAltitude";

    /**
     * The name of the profile wind-speed property.
     */
    public static String PROFILE_SPEED = "profileSpeed";

    /**
     * The name of the profile wind-direction property.
     */
    public static String PROFILE_DIRECTION = "profileDirection";

    /**
     * The name of the active profile property.
     */
    public static String ACTIVE_PROFILE = "activeProfile";

    /**
     * The name of the active mean-wind property.
     */
    public static String ACTIVE_MEAN_WIND = "activeMeanWind";

    /** log of 10 */
    private static final double ln10 = Math.log(10);

    /** default min height */
    private static Real defaultMinAltitude;

    /** default max height */
    private static Real defaultMaxAltitude;

    /** set of wind profiles */
    private WindProfileSet windProfileSet;

    /** set of mean winds */
    private MeanWindSet meanWindSet;

    /** range of heights in display */
    private RealTuple displayAltitudeExtent;

    /** speed unit */
    private Unit speedUnit;

    /** height unit */
    private Unit altitudeUnit;

    /** flag for autoscaling altitude range to data */
    private boolean autoscaleAltitude;

    /** actual altitude range */
    private RealTuple altitudeExtent;

    /** renderer for the wind profile */
    private WindProfileDisplayRenderer renderer;

    /** map for altitude */
    private ScalarMap altitudeMap;

    /** map for color by altitude */
    private ScalarMap altitudeColorMap;

    static {
        try {
            defaultMinAltitude =
                new Real(GeopotentialAltitude.getRealType(), 0,
                         DefaultUnitsDB.instance().get("gpm"));
            defaultMaxAltitude =
                new Real(GeopotentialAltitude.getRealType(), 16000,
                         DefaultUnitsDB.instance().get("gpm"));
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Returns the default, minimum altitude.
     * @return                  The default, minimum altitude.
     */
    protected static Real getDefaultMinAltitude() {
        return defaultMinAltitude;
    }

    /**
     * Returns the default, maximum altitude.
     * @return                  The default, maximum altitude.
     */
    protected static Real getDefaultMaxAltitude() {
        return defaultMaxAltitude;
    }

    /**
     * Constructs with limits on min and max geopotential altitudes.
     * @param displayImpl       The VisAD display.
     * @param minZ              The minimum altitude.
     * @param maxZ              The maximum altitude.
     * @param displayableCount  The anticipated numer of Displayable-s.
     * @param verticalDisplayRealType
     *                          The type of the display vertical dimension.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected WindProfileDisplay(DisplayImpl displayImpl, Real minZ, Real maxZ, int displayableCount, DisplayRealType verticalDisplayRealType)
            throws VisADException, RemoteException {

        super(displayImpl, displayableCount);

        renderer =
            (WindProfileDisplayRenderer) getDisplay().getDisplayRenderer();

        renderer.addCursorPositionListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {
                    setCursorPosition((double[]) event.getNewValue());
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + "propertyChange(): "
                        + "Couldn't handle change to cursor position: " + e);
                }
            }
        });

        displayAltitudeExtent = new RealTuple(new Real[]{ minZ, maxZ });
        autoscaleAltitude     = true;
        altitudeUnit          = minZ.getUnit();

        // Set up the display.
        renderer.setBoxOn(false);
        renderer.setCursorStringOn(false);
        getDisplay().getGraphicsModeControl().setScaleEnable(true);

        altitudeMap = new ScalarMap(GeopotentialAltitude.getRealType(),
                                    verticalDisplayRealType);

        // altitudeMap.setRangeByUnits();       // causes UnitException
        setAltitudeMapRange();
        addScalarMap(altitudeMap);

        altitudeColorMap = new ScalarMap(GeopotentialAltitude.getRealType(),
                                         Display.Hue);

        setAltitudeColorMapRange();
        addScalarMap(altitudeColorMap);
        addScalarMap(new ConstantMap(1, Display.Saturation));

        windProfileSet = new WindProfileSet(newWindProfile(), getDisplay());

        addDisplayable(windProfileSet);
        windProfileSet.addPropertyChangeListener(
            windProfileSet.GEOPOTENTIAL_ALTITUDE_EXTENT,
            new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {
                    if (isAutoscaleAltitude()) {
                        setDisplayAltitudeExtent();
                    }
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to profile altitude extent: "
                        + e);
                }
            }
        });
        windProfileSet.addPropertyChangeListener(
            windProfileSet.SPEED, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                PropertyChangeEvent newEvent =
                    new PropertyChangeEvent(WindProfileDisplay.this,
                                            PROFILE_SPEED,
                                            event.getOldValue(),
                                            event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                WindProfileDisplay.this.firePropertyChange(newEvent);
            }
        });
        windProfileSet.addPropertyChangeListener(
            windProfileSet.DIRECTION, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                PropertyChangeEvent newEvent =
                    new PropertyChangeEvent(WindProfileDisplay.this,
                                            PROFILE_DIRECTION,
                                            event.getOldValue(),
                                            event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                WindProfileDisplay.this.firePropertyChange(newEvent);
            }
        });
        windProfileSet.addPropertyChangeListener(
            windProfileSet.ACTIVE_WIND_PROFILE, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {
                    PropertyChangeEvent newEvent =
                        new PropertyChangeEvent(WindProfileDisplay.this,
                                                ACTIVE_PROFILE,
                                                event.getOldValue(),
                                                event.getNewValue());

                    newEvent.setPropagationId(event.getPropagationId());
                    WindProfileDisplay.this.firePropertyChange(newEvent);
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to active wind-profile: "
                        + e);
                }
            }
        });
        addDisplayListener(new DisplayListener() {

            public void displayChanged(DisplayEvent event) {

                int id = event.getId();

                try {

                    /*
                     * Fire a property change event for the active wind
                     * profile whenever it might have been manipulated.
                     */
                    if (id == event.MOUSE_RELEASED_RIGHT) {
                        WindProfileDisplay.this
                            .firePropertyChange(ACTIVE_PROFILE, null,
                                                windProfileSet
                                                    .getActiveWindProfile()
                                                    .getProfile());
                    }
                } catch (Exception e) {
                    System.err.println(this.getClass().getName()
                                       + ".displayChanged(): "
                                       + "Couldn't handle display change: "
                                       + e);
                }  // not allowed
            }
        });

        meanWindSet = new MeanWindSet(newMeanWind(), getDisplay());

        meanWindSet.addPropertyChangeListener(meanWindSet.ACTIVE_MEAN_WIND,
                                              new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {
                    PropertyChangeEvent newEvent =
                        new PropertyChangeEvent(WindProfileDisplay
                            .this, ACTIVE_MEAN_WIND, ((DisplayableData) event
                                .getOldValue())
                                    .getData(), ((DisplayableData) event
                                        .getNewValue()).getData());

                    newEvent.setPropagationId(event.getPropagationId());
                    WindProfileDisplay.this.firePropertyChange(newEvent);
                } catch (Exception e) {
                    System.err.println(
                        getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to active mean wind: " + e);
                }
            }
        });
        addDisplayable(meanWindSet);
    }

    /**
     * Returns the set of wind profiles.
     * @return                  The set of wind profiles.
     */
    protected WindProfileSet getWindProfileSet() {
        return windProfileSet;
    }

    /**
     * Returns the set of spatial ScalarMap-s.
     *
     * @param value
     * @return                  The set of spatial ScalarMap-s.
     * @throws VisADException   VisAD failure.
     * protected java.util.Set getSpatialScalarMapSet()
     *   throws VisADException
     * {
     *   java.util.TreeSet       set = new TreeSet();
     *   set.add(new ConstantMap(1, Display.Saturation));
     *   set.add(altitudeMap);
     *   set.add(altitudeColorMap);
     *   set.addAll(getSpatialScalarMapSubSet());
     *   return set;
     * }
     */

    /**
     * Returns the set of spatial ScalarMap-s of the subclass.  This is a
     * template method.
     * @return                  The set of spatial ScalarMap-s of the subclass.
     * @throws VisADException   VisAD failure.
     * protected abstract java.util.Set getSpatialScalarMapSubSet()
     *   throws VisADException;
     */

    /**
     * Toggles automatic altitude-scaling.  When autoscaling is on, the altitude
     * scale will automatically be adjusted when a new profile is set in the
     * display.
     *
     * @param  value   Autoscale altitude if true, otherwise keep altitude scale
     *                 the same.
     */
    public void setAutoscaleAltitude(boolean value) {
        autoscaleAltitude = value;
    }

    /**
     * Gets the state of automatic altitude-scaling.
     *
     * @return  true if automatic altitude-scaling is on, otherwise false
     */
    public boolean isAutoscaleAltitude() {
        return autoscaleAltitude;
    }

    /**
     * Adds a wind profile. The profile will be inserted at the given
     * index.  The profile will be invisible.
     * @param index             The index of the profile.
     * @param field             The wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void addProfile(int index, Field field)
            throws VisADException, RemoteException {

        WindProfile profile = newWindProfile();

        profile.setVisible(false);
        FlatField ff = (FlatField) vetWinds(field);
        if (ff != null) {
            profile.setProfile(ff);
        }
        windProfileSet.addWindProfile(index, profile);
    }

    /**
     * Removes a given wind profile.
     * @param index             The index of the profile to be removed.
     * @throws IndexOutOfBoundsException
     *                          The index was out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void removeProfile(int index)
            throws IndexOutOfBoundsException, RemoteException,
                   VisADException {
        windProfileSet.removeWindProfile(index);
    }

    /**
     * Sets the active wind profile.
     * @param index             The index of the active wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setActiveWindProfile(int index)
            throws VisADException, RemoteException {
        windProfileSet.setActiveWindProfile(index);
    }

    /**
     * Sets the visibility of a given wind profile.
     * @param index             The wind profile index.
     * @param visible           Whether or not the wind profile is to be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfileVisible(int index, boolean visible)
            throws VisADException, RemoteException {
        windProfileSet.setVisible(visible, index, index);
    }

    /**
     * Creates the displayable WindProfile appropriate to this instance.  This
     * is a template method.
     * @return                  The displayable WindProfile appropriate to this
     *                          instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract WindProfile newWindProfile()
     throws VisADException, RemoteException;

    /**
     * Gets the currently active wind profile.  The
     * function type of the returned profile is a
     * CartesianHorizontalWindOfGeopotentialAltitude.
     *
     * @see ucar.visad.functiontypes.CartesianHorizontalWindOfGeopotentialAltitude
     * @return   wind profile
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Field getWindProfile() throws RemoteException, VisADException {
        return windProfileSet.getActiveWindProfile().getProfile();
    }

    /**
     * Sets the mean wind.
     *
     * @param index             Which mean wind.
     * @param meanWind          The mean wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setMeanWind(int index, Tuple meanWind)
            throws VisADException, RemoteException {

        Displayable meanWindDisplayable = newMeanWind(meanWind);

        meanWindDisplayable.setVisible(false);
        meanWindSet.setMeanWind(index, meanWindDisplayable);
    }

    /**
     * Sets the mean wind.
     *
     * @param index             The index of the mean wind.
     * @param meanWindRef       The data reference for the mean wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setMeanWind(int index, DataReference meanWindRef)
            throws VisADException, RemoteException {

        Displayable meanWindDisplayable = newMeanWind(meanWindRef);

        meanWindDisplayable.setVisible(false);
        meanWindSet.setMeanWind(index, meanWindDisplayable);
    }

    /**
     * Removes a given mean-wind.
     * @param index             The index of the mean-wind to be removed.
     * @throws IndexOutOfBoundsException
     *                          The index was out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void removeMeanWind(int index)
            throws IndexOutOfBoundsException, VisADException,
                   RemoteException {
        meanWindSet.removeMeanWind(index);
    }

    /**
     * Sets the visibility of a given mean wind.
     * @param index             The index of the mean wind.
     * @param visible           Whether or not the mean wind is to be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setMeanWindVisible(int index, boolean visible)
            throws VisADException, RemoteException {
        meanWindSet.setVisible(visible, index, index);
    }

    /**
     * Sets the active mean-wind.
     * @param index             The index of the active mean-wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setActiveMeanWind(int index)
            throws VisADException, RemoteException {
        meanWindSet.setActiveMeanWind(index);
    }

    /**
     * Clears the wind data.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void clear() throws VisADException, RemoteException {
        windProfileSet.clear();
        meanWindSet.clear();
    }

    /**
     * Returns the minimum profile altitude.
     * @return                  The minimum profile altitude.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Real getMinProfileAltitude()
            throws VisADException, RemoteException {
        return (Real) windProfileSet.getGeopotentialAltitudeExtent()
            .getComponent(0);
    }

    /**
     * Returns the maximum profile altitude.
     * @return                  The maximum profile altitude.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Real getMaxProfileAltitude()
            throws VisADException, RemoteException {
        return (Real) windProfileSet.getGeopotentialAltitudeExtent()
            .getComponent(1);
    }

    /**
     * Sets the geopotential altitude property.
     * @param geoAlt            The new value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setGeopotentialAltitude(Real geoAlt)
            throws VisADException, RemoteException {

        Real old = getGeopotentialAltitude();

        windProfileSet.setGeopotentialAltitude(geoAlt);
        firePropertyChange(GEOPOTENTIAL_ALTITUDE, old, geoAlt);
    }

    /**
     * Returns the geopotential altitude property.
     * @return                  The geopotential altitude property.
     */
    public Real getGeopotentialAltitude() {
        return windProfileSet.getGeopotentialAltitude();
    }

    /**
     * Returns the profile-wind-speed property.
     * @return                  The profile-wind-speed property.
     */
    protected Real getProfileSpeed() {
        return windProfileSet.getSpeed();
    }

    /**
     * Returns the profile-wind-direction property.
     * @return                  The profile-wind-direction property.
     */
    protected Real getProfileDirection() {
        return windProfileSet.getDirection();
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
        altitudeMap.setScaleEnable(b);
    }

    /**
     * Sets the visiblity of the altitude scale.
     * @param visible           Whether or not the altitude scale should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setAltitudeScaleVisible(boolean visible)
            throws VisADException, RemoteException {
        altitudeMap.setScaleEnable(visible);
    }

    /**
     * Computes the increment for displaying an extent.
     * @param extent            The extent.
     * @param maxCount          The maximum number of intervals.
     * @return                  The increment for displaying the extent.
     */
    protected double computeIncrement(double extent, int maxCount) {

        double increment = Math.exp(ln10
                                    * Math.ceil(Math.log(extent / maxCount)
                                                / ln10));
        int count = (int) (extent / increment);

        if (count * 5 <= maxCount) {
            increment /= 5;
        } else if (count * 2 <= maxCount) {
            increment /= 2;
        }

        return increment;
    }

    /**
     * Scales the altitude ScalarMap.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setAltitudeMapRange()
            throws RemoteException, VisADException {

        Unit altitudeUnit =
            ((RealType) altitudeMap.getScalar()).getDefaultUnit();

        altitudeMap.setRange(getMinDisplayAltitude().getValue(altitudeUnit),
                             getMaxDisplayAltitude().getValue(altitudeUnit));
    }

    /**
     * Scales the altitude-color ScalarMap.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setAltitudeColorMapRange()
            throws RemoteException, VisADException {

        Unit altitudeUnit =
            ((RealType) altitudeColorMap.getScalar()).getDefaultUnit();

        altitudeColorMap.setRange(
            getMinDisplayAltitude().getValue(altitudeUnit),
            getMaxDisplayAltitude().getValue(altitudeUnit));
    }

    /**
     * Sets the extent of the displayed altitudes.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setDisplayAltitudeExtent()
            throws VisADException, RemoteException {

        Unit   altUnit       = getAltitudeUnit();
        double minProfileAlt = getMinProfileAltitude().getValue(altUnit);
        double maxProfileAlt = getMaxProfileAltitude().getValue(altUnit);
        double extent        = maxProfileAlt - minProfileAlt;

        if ( !Double.isNaN(extent) && !Double.isInfinite(extent)
                && (extent > 0)) {
            double increment = computeIncrement(extent, 5);

            setDisplayAltitudeExtent(new RealTuple(new Real[]{
                new Real(GeopotentialAltitude.getRealType(),
                         Math.floor(minProfileAlt / increment) * increment,
                         altUnit),
                new Real(GeopotentialAltitude.getRealType(),
                         Math.ceil(maxProfileAlt / increment) * increment,
                         altUnit) }));
        }
    }

    /**
     * Sets the extent of the displayed altitudes.
     * @param extent            The extent of the displayed altitudes.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDisplayAltitudeExtent(RealTuple extent)
            throws VisADException, RemoteException {

        displayAltitudeExtent = extent;

        displayAltitudeExtentChange();
    }

    /**
     * Handles a change to the extent of the displayed altitudes.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void displayAltitudeExtentChange()
     throws VisADException, RemoteException;

    /**
     * Returns the minimum, display altitude.
     * @return                  The minimum, display altitude.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Real getMinDisplayAltitude()
            throws VisADException, RemoteException {
        return (Real) displayAltitudeExtent.getComponent(0);
    }

    /**
     * Returns the maximum, display altitude.
     * @return                  The maximum, display altitude.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Real getMaxDisplayAltitude()
            throws VisADException, RemoteException {
        return (Real) displayAltitudeExtent.getComponent(1);
    }

    /**
     * Sets the displayed, altitude unit.
     * @param geoAltUnit        The displayed, altitude unit.
     * @throws UnitException    Argument has incompatible unit.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setAltitudeUnit(Unit geoAltUnit)
            throws UnitException, VisADException, RemoteException {

        if ( !Unit.canConvert(geoAltUnit, CommonUnit.meterPerSecond)) {
            throw new UnitException("\"Geopotential altitude\" unit ("
                                    + geoAltUnit + ") isn't");
        }

        if ( !geoAltUnit.equals(altitudeUnit)) {
            altitudeUnit = geoAltUnit;

            setDisplayAltitudeExtent();
        }
    }

    /**
     * Returns the displayed, altitude unit.
     * @return                  The displayed, altitude unit.
     */
    public Unit getAltitudeUnit() {
        return altitudeUnit;
    }

    /**
     * Sets the cursor position.
     * @param position          The cursor position.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void setCursorPosition(double[] position)
     throws VisADException, RemoteException;

    /**
     * Returns the cursor position.
     * @return                  The cursor position.
     */
    public double[] getCursorPosition() {
        return renderer.getCursorPosition();
    }

    /**
     * Returns a MeanWind Displayable corresponding to a mean-wind Tuple.
     * @param meanWind          the mean-wind Tuple
     * @return                  The MeanWind Displayable corresponding to the
     *                          input mean-wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract Displayable newMeanWind(Tuple meanWind)
     throws VisADException, RemoteException;

    /**
     * Returns a MeanWind Displayable corresponding to a mean-wind Tuple.
     * @param meanWindRef       The data reference for the mean-wind Tuple
     * @return                  The MeanWind Displayable corresponding to the
     *                          input mean-wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract Displayable newMeanWind(DataReference meanWindRef)
     throws VisADException, RemoteException;

    /**
     * Returns the MeanWind Displayable with a missing mean-wind.
     * @return                  The MeanWind Displayable with a missing
     *                          mean-wind.
     */
    protected abstract Displayable newMeanWind();

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

        if ( !inputFunction.getDomain().getComponent(0).equals(
                GeopotentialAltitude.getRealType())) {
            input =
                Util.convertDomain(input,
                                   GeopotentialAltitude.getRealTupleType(),
                                   null);
        }

        if (Unit.canConvert(input.getDefaultRangeUnits()[0], CommonUnit
                .meterPerSecond) && Unit
                    .canConvert(input.getDefaultRangeUnits()[1], CommonUnit
                        .meterPerSecond)) {

            output = input;

        } else {

            RealTupleType cartesianType =
                CartesianHorizontalWind.getRealTupleType();

            output = new FlatField(
                CartesianHorizontalWindOfGeopotentialAltitude.instance(),
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
}

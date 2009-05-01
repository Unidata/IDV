/*
 * $Id: WindProfileSet.java,v 1.24 2005/05/13 18:33:42 jeffmc Exp $
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

import ucar.visad.display.*;
import ucar.visad.functiontypes.*;
import ucar.visad.quantities.*;

import visad.*;



import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for a CompositeDisplayable of WindProfile-s.
 *
 * @author Steven R. Emmerson
 * @version $Id: WindProfileSet.java,v 1.24 2005/05/13 18:33:42 jeffmc Exp $
 */
public class WindProfileSet extends CompositeDisplayable {

    /**
     * The name of the active wind profile property.
     */
    public static String ACTIVE_WIND_PROFILE = "activeWindProfile";

    /**
     * The name of the geopotential altitude property.
     */
    public static String GEOPOTENTIAL_ALTITUDE = "altitude";

    /**
     * The name of the pressure property.
     */
    public static String PRESSURE = "pressure";

    /**
     * The name of the wind speed property.
     */
    public static String SPEED = "speed";

    /**
     * The name of the wind direction property.
     */
    public static String DIRECTION = "direction";

    /**
     * The name of the geopotential altitude extent property.
     */
    public static String GEOPOTENTIAL_ALTITUDE_EXTENT =
        "geopotentialAltitudeExtent";

    /**
     * The name of the maximum wind speed property.
     */
    public static String MAXIMUM_SPEED = "maximumSpeed";

    /** missing value for altitude range */
    private static RealTuple missingAltitudeExtent;

    /** missing speed value */
    private static Real missingSpeed;

    /** missing altitude value */
    private static Real missingAltitude;

    /** altitude value */
    private Real altitude;

    /** pressure value */
    private Real pressure;

    /** max wind speed value */
    private Real maxSpeed;

    /** speed value */
    private Real speed;

    /** direction value */
    private Real direction;

    /** active wind profile */
    private WindProfile activeWindProfile;

    /** altitude range */
    private RealTuple altitudeExtent;

    /** missing wind profile */
    private WindProfile missingWindProfile;

    /** listener for changes in speed */
    private PropertyChangeListener speedListener =
        new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent event) {

            speed = (Real) event.getNewValue();

            PropertyChangeEvent newEvent =
                new PropertyChangeEvent(WindProfileSet.this, SPEED,
                                        event.getOldValue(), speed);

            newEvent.setPropagationId(event.getPropagationId());
            WindProfileSet.this.firePropertyChange(newEvent);
        }
    };

    /** listener for changes in direction */
    private PropertyChangeListener directionListener =
        new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent event) {

            direction = (Real) event.getNewValue();

            PropertyChangeEvent newEvent =
                new PropertyChangeEvent(WindProfileSet.this, DIRECTION,
                                        event.getOldValue(), direction);

            newEvent.setPropagationId(event.getPropagationId());
            WindProfileSet.this.firePropertyChange(newEvent);
        }
    };

    static {
        try {
            RealType geoAltType = GeopotentialAltitude.getRealType();

            missingAltitudeExtent =
                new RealTuple(new RealTupleType(geoAltType, geoAltType));
            missingSpeed    = new Real(Speed.getRealType());
            missingAltitude = new Real(geoAltType);
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs with a given missing wind profile and VisAD display.
     * @param missingWindProfile        The missing wind profile.
     * @param display                   The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindProfileSet(WindProfile missingWindProfile,
                          LocalDisplay display)
            throws VisADException, RemoteException {

        super(display);

        maxSpeed                = missingSpeed;
        altitude                = missingAltitude;
        altitudeExtent          = missingAltitudeExtent;
        this.missingWindProfile = missingWindProfile;
        activeWindProfile       = missingWindProfile;
        speed                   = activeWindProfile.getSpeed();
        direction               = activeWindProfile.getDirection();
    }

    /**
     * Adds a wind profile to this composite.
     * @param windProfile       The wind profile to be added.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void addWindProfile(WindProfile windProfile)
            throws RemoteException, VisADException {
        addWindProfile(displayableCount(), windProfile);
    }

    /**
     * Sets a wind profile of this composite.
     * @param index             The index of the wind profile.
     * @param windProfile       The wind profile value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void addWindProfile(int index,
                                            WindProfile windProfile)
            throws RemoteException, VisADException {

        setDisplayable(index, windProfile);

        Real thisMax = getMaximumSpeed();
        Real thatMax = windProfile.getMaximumSpeed();

        if (thisMax.isMissing()) {
            setMaximumSpeed(thatMax);
        } else if ( !thatMax.isMissing()) {
            setMaximumSpeed((Real) thisMax.max(thatMax));
        }
    }

    /**
     * Removes a wind profile from this composite.
     * @param index             The index of the wind profile.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void removeWindProfile(int index)
            throws IndexOutOfBoundsException, RemoteException,
                   VisADException {

        WindProfile windProfile = (WindProfile) getDisplayable(index);
        if (windProfile == null) {
            return;
        }

        if (windProfile == activeWindProfile) {
            removeListeners(windProfile);
        }

        removeDisplayable(windProfile);

        if ((displayableCount() == 0) || (windProfile == activeWindProfile)) {
            setActiveWindProfile(missingWindProfile);
            setMaximumSpeed(missingSpeed);
        }
    }

    /**
     * Sets the index of the active wind-profile.
     * @param index             The index of the active wind profile.  A value
     *                          of -1 means that there is to be no active
     *                          wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setActiveWindProfile(int index)
            throws RemoteException, VisADException {

        WindProfile active = (index < 0)
                             ? missingWindProfile
                             : (WindProfile) getDisplayable(index);
        if (active == null) {
            active = missingWindProfile;
        }
        setActiveWindProfile(active);
    }

    /**
     * Sets the active wind profile property.
     * @param profile           The new value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setActiveWindProfile(WindProfile profile)
            throws RemoteException, VisADException {

        if (profile == null) {
            profile = missingWindProfile;
        }
        removeListeners(activeWindProfile);

        WindProfile old = activeWindProfile;

        activeWindProfile = profile;

        activeWindProfile.setGeopotentialAltitude(altitude);
        activeWindProfile.setPressure(activeWindProfile.getPressure());
        setSpeed(activeWindProfile.getSpeed());
        setDirection(activeWindProfile.getDirection());
        setGeopotentialAltitudeExtent();
        addListeners(activeWindProfile);
        firePropertyChange(ACTIVE_WIND_PROFILE, old, activeWindProfile);
    }

    /**
     * Returns the active wind profile property.
     *
     * @return                  The active wind profile property.
     */
    public WindProfile getActiveWindProfile() {
        return activeWindProfile;
    }

    /**
     * Reset the profile at the given index
     *
     * @param index  index of profile
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void setOriginalProfile(int index)
            throws VisADException, RemoteException {
        WindProfile profile = (WindProfile) getDisplayable(index);
        if (profile != null) {
            profile.setOriginalProfile();
        }
    }

    /**
     * Sets the geopotential altitude property.
     *
     * @param geoAlt            The new value.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setGeopotentialAltitude(Real geoAlt)
            throws RemoteException, VisADException {

        Real oldAltitude = altitude;

        altitude = geoAlt;

        activeWindProfile.setGeopotentialAltitude(altitude);
        firePropertyChange(GEOPOTENTIAL_ALTITUDE, oldAltitude, altitude);
    }

    /**
     * Returns the geopotential altitude property.
     * @return                  The geopotential altitude property.
     */
    public Real getGeopotentialAltitude() {
        return altitude;
    }

    /**
     * Sets the geopotential altitude extent property.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setGeopotentialAltitudeExtent()
            throws RemoteException, VisADException {

        WindProfile windProfile = getActiveWindProfile();
        RealTuple   thisExtent  = windProfile.getGeopotentialAltitudeExtent();

        setGeopotentialAltitudeExtent(
            new RealTuple(
                new Real[] {
                    (Real) altitudeExtent.getComponent(0).min(
                        thisExtent.getComponent(0)),
                    (Real) altitudeExtent.getComponent(1).max(
                        thisExtent.getComponent(1)) }));
    }

    /**
     * Sets the geopotential altitude extent property.
     * @param extent            The new value.
     */
    protected void setGeopotentialAltitudeExtent(RealTuple extent) {

        RealTuple old = altitudeExtent;

        altitudeExtent = extent;

        firePropertyChange(GEOPOTENTIAL_ALTITUDE_EXTENT, old, altitudeExtent);
    }

    /**
     * Returns the geopotential altitude extent property.
     * @return                  The geopotential altitude extent property.
     */
    public RealTuple getGeopotentialAltitudeExtent() {
        return altitudeExtent;
    }

    /**
     * Set the levels of the wind profile to display.
     * @param levels  the set of levels (if null, display all);
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setWindLevels(Gridded1DSet levels)
            throws VisADException, RemoteException {
        for (int i = 0; i < displayableCount(); ++i) {
            WindProfile windProfile = (WindProfile) getDisplayable(i);
            if (windProfile != null) {
                windProfile.setWindLevels(levels);
            }
        }
    }

    /**
     * Sets the pressure property.
     * @param pressure          The pressure property.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setPressure(Real pressure)
            throws RemoteException, VisADException {

        Real oldPressure = this.pressure;

        this.pressure = pressure;

        activeWindProfile.setPressure(pressure);
        firePropertyChange(PRESSURE, oldPressure, this.pressure);
    }

    /**
     * Sets the profile wind speed property.
     * @param spd               The new value.
     */
    protected void setSpeed(Real spd) {

        Real old = speed;

        speed = spd;

        firePropertyChange(SPEED, old, speed);
    }

    /**
     * Returns the profile wind speed property.
     * @return                  The profile wind speed property.
     */
    public Real getSpeed() {
        return speed;
    }

    /**
     * Sets the profile wind direction property.
     * @param dir               The new value.
     */
    protected void setDirection(Real dir) {

        Real old = direction;

        direction = dir;

        firePropertyChange(DIRECTION, old, direction);
    }

    /**
     * Returns the profile wind direction property.
     * @return                  The profile wind direction property.
     */
    public Real getDirection() {
        return direction;
    }

    /**
     * Returns the type of the geopotential altitude quantity.
     * @return                  The type of the geopotential altitude quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealType getGeopotentialAltitudeRealType()
            throws VisADException, RemoteException {
        return GeopotentialAltitude.getRealType();
    }

    /**
     * Sets the maximum profile wind speed property.
     * @param speed             The new value.
     */
    protected void setMaximumSpeed(Real speed) {

        Real old = maxSpeed;

        maxSpeed = speed;

        firePropertyChange(MAXIMUM_SPEED, old, maxSpeed);
    }

    /**
     * Returns the maximum profile wind speed property.
     * @return                  The maximum profile wind speed property.
     */
    public Real getMaximumSpeed() {
        return maxSpeed;
    }

    /**
     * Returns the type of the westerly wind quantity.
     * @return                  The type of the westerly wind quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealType getWesterlyWindRealType()
            throws VisADException, RemoteException {
        return WindProfile.getWesterlyWindRealType();
    }

    /**
     * Returns the type of the southerly wind quantity.
     * @return                  The type of the southerly wind quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealType getSoutherlyWindRealType()
            throws VisADException, RemoteException {
        return WindProfile.getSoutherlyWindRealType();
    }

    /**
     * Clears the wind profiles from this composite.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void clear() throws VisADException, RemoteException {

        for (int i = 0; i < displayableCount(); ++i) {
            removeWindProfile(i);
        }
    }

    /**
     * Add listeners to the wind profile
     *
     * @param windProfile   profile to monitor
     */
    private void addListeners(WindProfile windProfile) {

        windProfile.addPropertyChangeListener(WindProfile.SPEED,
                speedListener);
        windProfile.addPropertyChangeListener(WindProfile.DIRECTION,
                directionListener);
    }

    /**
     * Remove listeners from the wind profile
     *
     * @param windProfile   profile to modify
     */
    private void removeListeners(WindProfile windProfile) {

        windProfile.removePropertyChangeListener(WindProfile.SPEED,
                speedListener);
        windProfile.removePropertyChangeListener(WindProfile.DIRECTION,
                directionListener);
    }
}


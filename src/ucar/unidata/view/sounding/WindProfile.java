/*
 * $Id: WindProfile.java,v 1.27 2006/11/13 16:23:51 dmurray Exp $
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


import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.functiontypes.CartesianHorizontalWindOfGeopotentialAltitude;
import ucar.visad.functiontypes.CartesianHorizontalWindOfPressure;
import ucar.visad.quantities.*;

import visad.*;

import visad.java3d.*;

import java.beans.*;



import java.rmi.RemoteException;


/**
 * Provides support for the display of a vertical profile of the horizontal
 * wind.
 *
 * @author Steven R. Emmerson
 * @version $Id: WindProfile.java,v 1.27 2006/11/13 16:23:51 dmurray Exp $
 */
public abstract class WindProfile extends CompositeDisplayable {

    /**
     * The name of the wind profile property.
     */
    public static String WIND_PROFILE = "windProfile";

    /**
     * The name of the geopotential altitude property.
     */
    public static String GEOPOTENTIAL_ALTITUDE = "geopotentialAltitude";

    /**
     * The name of the geopotential altitude property.
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

    /** altitude value */
    private Real geopotentialAltitude;

    /** pressure value */
    private Real pressure;

    /** speed value */
    private Real speed;

    /** direction value */
    private Real direction;

    /** maximum speed value */
    private Real maxSpeed;

    /** altitude range */
    private RealTuple altitudeExtent;

    /** missing altitude value */
    private static Real missingAlt;

    /** missing pressure value */
    private static Real missingPressure;

    /** missing speed value */
    private static Real missingSpeed;

    /** missing direction value */
    private static Real missingDirection;

    /** missing wind field value */
    private FlatField missingWindField;

    static {
        try {
            missingAlt       = new Real(GeopotentialAltitude.getRealType());
            missingPressure  = new Real(AirPressure.getRealType());
            missingSpeed     = new Real(Speed.getRealType());
            missingDirection = new Real(Direction.getRealType());

        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Returns an instance of a wind field with no values.
     *
     * @return                 A wind field with no values.
     */
    protected FlatField getMissingWindField() {
        if (missingWindField == null) {
            try {
                missingWindField = new FlatField(
                    CartesianHorizontalWindOfGeopotentialAltitude.instance());
            } catch (Exception e) {
                missingWindField = null;
            }
        }
        return missingWindField;
    }

    /**
     * Constructs from nothing.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected WindProfile() throws VisADException, RemoteException {

        geopotentialAltitude = missingAlt;
        pressure             = missingPressure;
        speed                = missingSpeed;
        direction            = missingDirection;
        maxSpeed             = missingSpeed;
        altitudeExtent = new RealTuple(new Real[] { missingAlt, missingAlt });
    }

    /**
     * Constructs from another instance.
     *
     * @param that              The other instance.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected WindProfile(WindProfile that)
            throws RemoteException, VisADException {

        geopotentialAltitude = that.geopotentialAltitude;  // immutable
        pressure             = that.pressure;              // immutable
        speed                = that.speed;                 // immutable
        direction            = that.direction;             // immutable
        maxSpeed             = that.maxSpeed;              // immutable
        altitudeExtent       = that.altitudeExtent;        // immutable
    }

    /**
     * Sets the vertical profile of the horizontal wind.
     * @param profile           The vertical profile of the horizontal wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public abstract void setProfile(Field profile)
     throws VisADException, RemoteException;

    /**
     * Resets the vertical profile of the horizontal wind to the profile of
     * the last setProfile().
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public abstract void setOriginalProfile()
     throws VisADException, RemoteException;

    /**
     * Returns the vertical profile of the horizontal wind.
     * @return                  The vertical profile of the horizontal wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract Field getProfile()
     throws VisADException, RemoteException;

    /**
     * Handles a change to the vertical profile of the horizontal wind due
     * to either explicit setting or direct manipulation by a VisAD
     * DataRenderer.
     * @param oldProfile        The old profile.  May be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void profileChange(Field oldProfile)
            throws VisADException, RemoteException {

        firePropertyChange(WIND_PROFILE, oldProfile, getProfile());
        setAltitudeExtent();
        setMaximumSpeed();
        setSpeedAndDirection();
    }

    /**
     * Sets the vertical extent of the wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setAltitudeExtent()
            throws VisADException, RemoteException {

        Field         profile   = getProfile();
        SampledSet    domainSet = (SampledSet) profile.getDomainSet();
        RealTupleType domType   = ((SetType) domainSet.getType()).getDomain();
        RealType      altType   = (RealType) domType.getComponent(0);
        Unit          altUnit   = domainSet.getSetUnits()[0];
        RealTuple     oldExtent = altitudeExtent;

        altitudeExtent = new RealTuple(new Real[] {
            new Real(altType, domainSet.getLow()[0], altUnit),
            new Real(altType, domainSet.getHi()[0], altUnit) });
        if (altType.equals(AirPressure.getRealType())) {
            altitudeExtent =
                (RealTuple) AirPressure.toAltitude(altitudeExtent);
        }

        firePropertyChange(GEOPOTENTIAL_ALTITUDE_EXTENT, oldExtent,
                           altitudeExtent);
    }

    /**
     * Sets the maximum wind speed of the wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setMaximumSpeed() throws RemoteException, VisADException {

        Field     profile = getProfile();
        float[][] values  = profile.getFloats(false);
        float     max     = 0.f;
        float     spd;

        for (int i = 0; i < values[0].length; i++) {
            float u = values[0][i];
            float v = values[1][i];

            spd = (float) Math.sqrt(u * u + v * v);

            if ((spd == spd) && (spd > max)) {
                max = spd;
            }
        }

        Real oldMax = maxSpeed;

        maxSpeed = new Real(Speed.getRealType(), max,
                            profile.getDefaultRangeUnits()[0]);

        firePropertyChange(MAXIMUM_SPEED, oldMax, maxSpeed);
    }

    /**
     * Returns the maximum wind speed property.
     * @return                  The maximum wind speed property.
     */
    public Real getMaximumSpeed() {
        return maxSpeed;
    }

    /**
     * Returns the geopotential altitude extent property.
     * @return                  The geopotential altitude extent property.
     */
    public RealTuple getGeopotentialAltitudeExtent() {
        return altitudeExtent;
    }

    /**
     * Sets the geopotential altitude property.
     * @param geopotentialAltitude      The new value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setGeopotentialAltitude(Real geopotentialAltitude)
            throws RemoteException, VisADException {

        Real oldAltitude = this.geopotentialAltitude;

        this.geopotentialAltitude = geopotentialAltitude;

        firePropertyChange(GEOPOTENTIAL_ALTITUDE, oldAltitude,
                           geopotentialAltitude);
        setSpeedAndDirection();
    }

    /**
     * Returns the geopotential altitude property.
     * @return                  The geopotential altitude property.
     */
    public Real getGeopotentialAltitude() {
        return geopotentialAltitude;
    }

    /**
     * Sets the pressure property.
     * @param pressure      The new value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setPressure(Real pressure)
            throws RemoteException, VisADException {

        Real oldPressure = this.pressure;

        this.pressure = pressure;

        firePropertyChange(PRESSURE, oldPressure, pressure);
        setSpeedAndDirection();
    }

    /**
     * Returns the pressure property.
     * @return                  The pressure property.
     */
    public Real getPressure() {
        return pressure;
    }

    /**
     * Sets the wind speed and direction properties.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setSpeedAndDirection()
            throws RemoteException, VisADException {

        if (getGeopotentialAltitude().isMissing()) {
            return;
        }
        RealTuple spdDir = (RealTuple) ((FunctionImpl) getProfile()).evaluate(
                               getGeopotentialAltitude(),
                               Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
        if ( !((RealTupleType) spdDir.getType()).equals(
                PolarHorizontalWind.getRealTupleType())) {
            spdDir = PolarHorizontalWind.newRealTuple(spdDir);
        }

        setSpeed((Real) spdDir.getComponent(0));
        setDirection((Real) spdDir.getComponent(1));
    }

    /**
     * Returns the wind speed property.
     * @return                  The wind speed property.
     */
    public Real getSpeed() {
        return speed;
    }

    /**
     * Returns the wind direction property.
     * @return                  The wind direction property.
     */
    public Real getDirection() {
        return direction;
    }

    /**
     * Returns the type of the geopotential altitude.
     * @return                  The type of the geopotential altitude.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getGeopotentialAltitudeRealType()
            throws VisADException {
        return GeopotentialAltitude.getRealType();
    }

    /**
     * Returns the type of the westerly wind.
     * @return                  The type of the westerly wind.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getWesterlyWindRealType() throws VisADException {
        return WesterlyWind.getRealType();
    }

    /**
     * Returns the type of the southerly wind.
     * @return                  The type of the southerly wind.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getSoutherlyWindRealType() throws VisADException {
        return SoutherlyWind.getRealType();
    }

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof WindProfile)) {
            equals = false;
        } else {
            WindProfile that = (WindProfile) obj;

            equals =
                (that == this)
                || (geopotentialAltitude.equals(that.geopotentialAltitude)
                    && speed.equals(that.speed)
                    && maxSpeed.equals(that.maxSpeed)
                    && altitudeExtent.equals(that.altitudeExtent)
                    && direction.equals(that.direction)
                    && super.equals(that));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {

        return geopotentialAltitude.hashCode() ^ speed.hashCode()
               ^ maxSpeed.hashCode() ^ altitudeExtent.hashCode()
               ^ direction.hashCode() ^ super.hashCode();
    }

    /**
     * Ensures a wind profile in cartesian coordinates.
     *
     * @param input             Wind profile in cartesian or polar coordinates.
     * @return                  Wind profile in cartesian coordinates.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected static FlatField ensureCartesian(FlatField input)
            throws VisADException, RemoteException {

        FlatField    output;
        FunctionType inputFunction = (FunctionType) input.getType();

        if (Unit.canConvert(input.getDefaultRangeUnits()[0], CommonUnit
                .meterPerSecond) && Unit
                    .canConvert(input.getDefaultRangeUnits()[1], CommonUnit
                        .meterPerSecond)) {
            output = input;
        } else {

            if ( !inputFunction.getDomain().getComponent(0).equals(
                    GeopotentialAltitude.getRealType())) {
                throw new VisADException("Wrong domain type");
            }

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

    /**
     * Set the levels of the wind profile to display.
     * @param levels  the set of levels (if null, display all);
     *
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public abstract void setWindLevels(Gridded1DSet levels)
     throws VisADException, RemoteException;

    /**
     * Set the speed property
     *
     * @param spd   speed value
     */
    protected void setSpeed(Real spd) {

        Real oldSpeed = speed;

        speed = spd;

        firePropertyChange(SPEED, oldSpeed, speed);
    }

    /**
     * Set the direction value
     *
     * @param dir   direction value
     */
    protected void setDirection(Real dir) {

        Real oldDir = direction;

        direction = dir;

        firePropertyChange(DIRECTION, oldDir, direction);
    }
}


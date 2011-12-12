/*
 * $Id: RAOB.java,v 1.32 2006/12/01 20:42:43 jeffmc Exp $
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


package ucar.unidata.data.sounding;


import ucar.unidata.beans.InvisiblePropertiedBean;
import ucar.unidata.beans.NonVetoableProperty;
import ucar.unidata.beans.Property;
import ucar.unidata.beans.VetoableProperty;

import ucar.unidata.util.LogUtil;

import ucar.visad.Segment;
import ucar.visad.SegmentSet;
import ucar.visad.Util;
import ucar.visad.VisADMath;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.functiontypes.InSituAirTemperatureProfile;
import ucar.visad.functiontypes.PolarHorizontalWindOfGeopotentialAltitude;
import ucar.visad.quantities.AirDensity;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.Altitude;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.DewPoint;
import ucar.visad.quantities.GeopotentialAltitude;
import ucar.visad.quantities.Gravity;
import ucar.visad.quantities.InSituAirTemperature;
import ucar.visad.quantities.PolarHorizontalWind;
import ucar.visad.quantities.Pressure;

import visad.CellImpl;
import visad.CoordinateSystem;
import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.EmpiricalCoordinateSystem;
import visad.ErrorEstimate;
import visad.Field;
import visad.FieldException;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetException;
import visad.SetType;
import visad.SingletonSet;
import visad.Tuple;
import visad.TupleType;
import visad.UnimplementedException;
import visad.Unit;
import visad.UnitException;
import visad.VisADException;

import visad.util.DataUtility;



import java.awt.event.ItemEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Provides support for soundings from RAdiosonde OBservations.</p>
 *
 * <p>This class is a JavaBean.</p>
 *
 * <p>Instances are modifiable.
 *
 * @author Steven R. Emmerson
 * @version $Id: RAOB.java,v 1.32 2006/12/01 20:42:43 jeffmc Exp $
 */
public final class RAOB extends InvisiblePropertiedBean {

    /**
     * Empty mandatory pressure profile.
     */
    private static MandatoryPressureProfile emptyMandatoryPressureProfile;

    /**
     * Empty significant temperature profile.
     */
    private static SignificantTemperatureProfile emptySignificantTemperatureProfile;

    /**
     * Empty tropopause profile.
     */
    private static TropopauseProfile emptyTropopauseProfile;

    /**
     * Empty maximum wind profile.
     */
    private static MaximumWindProfile emptyMaximumWindProfile;

    /**
     * Empty mandatory wind profile.
     */
    private static MandatoryWindProfile emptyMandatoryWindProfile;

    /**
     * Empty significant wind profile.
     */
    private static SignificantWindProfile emptySignificantWindProfile;

    /**
     * Empty output-temperature profile.
     */
    private static FlatField emptyTemperatureProfile;

    /**
     * Empty output-dew-point profile.
     */
    private static FlatField emptyDewPointProfile;

    /**
     * Empty output-wind profile.
     */
    private static FlatField emptyWindProfile;

    /**
     * The gravity property.
     * @serial
     */
    private GravityProperty gravityProperty;

    /**
     * The temperature output-profile property.
     * @serial
     */
    private OutputProfileProperty temperatureProfileProperty;

    /**
     * The dew-point output-profile property.
     * @serial
     */
    private OutputProfileProperty dewPointProfileProperty;

    /**
     * The wind output-profile property.
     * @serial
     */
    private OutputProfileProperty windProfileProperty;

    /**
     * The mandatory pressure input-profile property.
     * @serial
     */
    private MandatoryPressureProperty mandatoryPressureProperty;

    /**
     * The significant temperature input-profile property.
     * @serial
     */
    private SignificantTemperatureProperty significantTemperatureProperty;

    /**
     * The tropopause input-profile property.
     * @serial
     */
    private TropopauseProperty tropopauseProperty;

    /**
     * The maximum wind input-profile property.
     * @serial
     */
    private MaximumWindProperty maximumWindProperty;

    /**
     * The mandatory wind input-profile property.
     * @serial
     */
    private MandatoryWindProperty mandatoryWindProperty;

    /**
     * The significant wind input-profile property.
     * @serial
     */
    private SignificantWindProperty significantWindProperty;

    /**
     * The Pressure <-> Altitude coordinate system transformation property.
     * @serial
     */
    private PressureCoordinateSystem pressureCoordinateSystem;

    /**
     * The GeopotentialAltitude <-> Altitude coordinate system transformation
     * property.
     * @serial
     */
    private GeopotentialAltitudeCoordinateSystem geopotentialAltitudeCoordinateSystem;

    /**
     * The PressureCSHelper for the MandatoryPressureProfile.
     * @serial
     */
    private MandatoryPressureCSHelper mandatoryPressureCSHelper;

    /**
     * Contributors to the output, temperature profile.
     */
    private OutputProfileContributors temperatureProfileContributors;

    /**
     * Contributors to the output, dew-point profile.
     */
    private OutputProfileContributors dewPointProfileContributors;

    /**
     * Contributors to the output, wind profile.
     */
    private OutputProfileContributors windProfileContributors;

    /**
     * All possible links between input profiles and output profiles.
     */
    private LinkSet possibleLinks;

    /**
     * Currently active links between input profiles and output profiles.
     */
    private LinkSet activeLinks;

    /**
     * Initializes this class.
     */
    static {
        try {
            emptyMandatoryPressureProfile = new MandatoryPressureProfile();
            emptySignificantTemperatureProfile =
                new SignificantTemperatureProfile();
            emptyTropopauseProfile      = new TropopauseProfile();
            emptyMaximumWindProfile     = new MaximumWindProfile();
            emptyMandatoryWindProfile   = new MandatoryWindProfile();
            emptySignificantWindProfile = new SignificantWindProfile();
            emptyTemperatureProfile =
                new FlatField(InSituAirTemperatureProfile.instance());
            emptyDewPointProfile = new FlatField(DewPointProfile.instance());
            emptyWindProfile = new FlatField(
                PolarHorizontalWindOfGeopotentialAltitude.instance());
        } catch (Exception e) {
            throw new RuntimeException("Couldn't initialize class: "
                                       + e.toString());
        }
    }

    /**
     * Constructs from nothing.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public RAOB() throws VisADException {
        super("Radiosonde Observation");

        addProperty(gravityProperty = new GravityProperty());
        addProperty(mandatoryPressureProperty =
            new MandatoryPressureProperty());
        addProperty(significantTemperatureProperty =
            new SignificantTemperatureProperty());
        addProperty(tropopauseProperty = new TropopauseProperty());
        addProperty(maximumWindProperty = new MaximumWindProperty());
        addProperty(mandatoryWindProperty = new MandatoryWindProperty());
        addProperty(significantWindProperty = new SignificantWindProperty());

        temperatureProfileContributors =
            new OutputProfileContributors(new InputProfileProperty[] {
                mandatoryPressureProperty,
                significantTemperatureProperty, tropopauseProperty });
        dewPointProfileContributors =
            new OutputProfileContributors(new InputProfileProperty[] {
                mandatoryPressureProperty,
                significantTemperatureProperty, tropopauseProperty });
        windProfileContributors =
            new OutputProfileContributors(new InputProfileProperty[] {
                mandatoryPressureProperty,
                tropopauseProperty, maximumWindProperty,
                mandatoryWindProperty, significantWindProperty });

        try {
            addProperty(pressureCoordinateSystem =
                new PressureCoordinateSystem(mandatoryPressureCSHelper =
                    new MandatoryPressureCSHelper()));
            addProperty(geopotentialAltitudeCoordinateSystem =
                new GeopotentialAltitudeCoordinateSystem());

            addProperty(
                temperatureProfileProperty = new OutputProfileProperty(
                    "temperatureProfile", emptyTemperatureProfile,
                    pressureCoordinateSystem.getCoordinateSystem(),
                    temperatureProfileContributors));
            addProperty(
                dewPointProfileProperty = new OutputProfileProperty(
                    "dewPointProfile", emptyDewPointProfile,
                    pressureCoordinateSystem.getCoordinateSystem(),
                    dewPointProfileContributors));
            addProperty(
                windProfileProperty =
                    new OutputProfileProperty(
                        "windProfile", emptyWindProfile,
                        geopotentialAltitudeCoordinateSystem
                            .getCoordinateSystem(), windProfileContributors));
        } catch (RemoteException e) {}  // can't happen because above data objects are local

        possibleLinks = new LinkSet(new Link[] {
            new Contribution(mandatoryPressureProperty,
                             temperatureProfileProperty),
            new Contribution(mandatoryPressureProperty,
                             dewPointProfileProperty),
            new Contribution(mandatoryPressureProperty, windProfileProperty),
            new Contribution(significantTemperatureProperty,
                             temperatureProfileProperty),
            new Contribution(significantTemperatureProperty,
                             dewPointProfileProperty),
            new Contribution(tropopauseProperty, temperatureProfileProperty),
            new Contribution(tropopauseProperty, dewPointProfileProperty),
            new Contribution(tropopauseProperty, windProfileProperty),
            new Contribution(maximumWindProperty, windProfileProperty),
            new Contribution(mandatoryWindProperty, windProfileProperty),
            new Contribution(significantWindProperty, windProfileProperty),
        });
    }

    /**
     * Sets the mandatory-pressure-profile property.
     *
     * @param field             Mandatory pressure profile: pressure ->
     *                          (temperature, dew point, wind, geopotential
     *                          height).  May be <code>null</code>.
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          profile remains unchanged.
     * @throws RemoteException  Java RMI exception.
     */
    public void setMandatoryPressureProfile(Field field)
            throws VisADException, RemoteException, PropertyVetoException {
        mandatoryPressureProperty.setField(field);
    }

    /**
     * Set the mandatory pressure profile for this RAOB. A mandatory pressure
     * profile consists of a set of temperatures, dewpoints, wind speeds,
     * wind directions and geopotential heights at specific pressures.
     *
     * @param pressureUnit             Unit of pressure values
     * @param pressures                array of pressure values
     * @param temperatureUnit          Unit of temperature values
     * @param temperatures             array of temperature values
     * @param dewPointUnit             Unit of dewpoint values
     * @param dewPoints                Array of dewpoint values
     * @param speedUnit                Unit of wind speed values
     * @param speeds                   Array of wind speed values
     * @param directionUnit            Unit of wind speed values
     * @param directions               Array of wind direction values
     * @param geopotentialAltitudeUnit Unit of height values
     * @param geopotentialAltitudes    Array of height values
     *
     * @throws PropertyVetoException     couldn't set the property
     * @throws RemoteException           Java RMI error
     * @throws SetException              bad pressure values
     * @throws VisADException            Couldn't create a VisAD object
     */
    public void setMandatoryPressureProfile(Unit pressureUnit,
                                            float[] pressures,
                                            Unit temperatureUnit,
                                            float[] temperatures,
                                            Unit dewPointUnit,
                                            float[] dewPoints,
                                            Unit speedUnit, float[] speeds,
                                            Unit directionUnit,
                                            float[] directions,
                                            Unit geopotentialAltitudeUnit,
                                            float[] geopotentialAltitudes)
            throws SetException, RemoteException, VisADException,
                   PropertyVetoException {
        setMandatoryPressureProfile(newMandatoryPressureProfile(pressureUnit,
                pressures, temperatureUnit, temperatures, dewPointUnit,
                dewPoints, speedUnit, speeds, directionUnit, directions,
                geopotentialAltitudeUnit, geopotentialAltitudes));
    }


    /**
     * Gets the mandatory-pressure-profile property.
     *
     * @return                  The mandatory pressure profile: pressure ->
     *                          (temperature, dew point, wind, geopotential
     *                          height).  Won't be <code>null</code> but may
     *                          be empty.
     */
    public Field getMandatoryPressureProfile() {
        return mandatoryPressureProperty.getField();
    }

    /**
     * Sets the significant-temperature-profile property.
     *
     * @param field             Significant temperature profile: pressure ->
     *                          (temperature, dew point).  May be
     *                          <code>null</code>.
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          profile remains unchanged.
     * @throws RemoteException  Java RMI exception.
     */
    public void setSignificantTemperatureProfile(Field field)
            throws VisADException, RemoteException, PropertyVetoException {
        significantTemperatureProperty.setField(field);
    }

    /**
     * Set the significant temperature profile for this RAOB. A significant
     * temperature profile consists of a set of temperatures and dewpoints
     * at certain pressures.
     *
     * @param pressureUnit              Unit of pressure values
     * @param pressures                 Array of pressure values
     * @param temperatureUnit           Unit of temperature values
     * @param temperatures              Array of temperature values
     * @param dewPointUnit              Unit of dewpoint values
     * @param dewPoints                 Array of dewpoint values
     *
     * @throws PropertyVetoException    couldn't set the property
     * @throws RemoteException          Java RMI problem
     * @throws VisADException           VisAD problem
     */
    public void setSignificantTemperatureProfile(Unit pressureUnit,
            float[] pressures, Unit temperatureUnit, float[] temperatures,
            Unit dewPointUnit, float[] dewPoints)
            throws VisADException, RemoteException, PropertyVetoException {
        setSignificantTemperatureProfile(
            newSignificantTemperatureProfile(
                pressureUnit, pressures, temperatureUnit, temperatures,
                dewPointUnit, dewPoints));
    }



    /**
     * Gets the significant-temperature-profile property.
     *
     * @return                  The significant temperature profile: pressure ->
     *                          (temperature, dew point).
     *                          Won't be <code>null</code> but may
     *                          be empty.
     */
    public Field getSignificantTemperatureProfile() {
        return significantTemperatureProperty.getField();
    }

    /**
     * Sets the tropopause-profile property.
     *
     * @param field             Tropopause profile: pressure -> (temperature,
     *                          dew point, wind).  May be <code>null</code>.
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          profile remains unchanged.
     * @throws RemoteException  Java RMI exception.
     */
    public void setTropopauseProfile(Field field)
            throws VisADException, RemoteException, PropertyVetoException {
        tropopauseProperty.setField(field);
    }

    /**
     * Gets the tropopause-profile property.
     *
     * @return                  The tropopause profile: pressure ->
     *                          (temperature, dew point, wind).  Won't be
     *                          <code>null</code> but may be empty.
     */
    public Field getTropopauseProfile() {
        return tropopauseProperty.getField();
    }

    /**
     * Sets the maximum-wind-profile property.
     *
     * @param field             Maximum wind profile: pressure -> (speed,
     *                          direction).  May be <code>null</code>.
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          profile remains unchanged.
     * @throws RemoteException  Java RMI exception.
     */
    public void setMaximumWindProfile(Field field)
            throws VisADException, RemoteException, PropertyVetoException {
        maximumWindProperty.setField(field);
    }

    /**
     * Set the maximum wind profile for this RAOB.  The maximum wind
     * profile consists of the wind speed and direction at the maximum
     * wind pressure level
     *
     * @param pressureUnit           Unit of pressure values
     * @param pressures              Array of pressures
     * @param speedUnit              Unit of wind speed values
     * @param speeds                 Array of wind speed values
     * @param directionUnit          Unit of wind direction values
     * @param directions             Array of wind direction values
     *
     * @throws PropertyVetoException       couldn't set the property
     * @throws RemoteException             Java RMI problem
     * @throws VisADException              VisAD problem
     */
    public void setMaximumWindProfile(Unit pressureUnit, float[] pressures,
                                      Unit speedUnit, float[] speeds,
                                      Unit directionUnit, float[] directions)
            throws VisADException, RemoteException, PropertyVetoException {
        setMaximumWindProfile(newMaximumWindProfile(pressureUnit, pressures,
                speedUnit, speeds, directionUnit, directions));
    }

    /**
     * Gets the maximum-wind-profile property.
     *
     * @return                  The maximum wind profile: pressure -> (speed,
     *                          direction).
     *                          Won't be <code>null</code> but may
     *                          be empty.
     */
    public Field getMaximumWindProfile() {
        return maximumWindProperty.getField();
    }

    /**
     * Sets the mandatory-wind-profile property.
     *
     * @param field             Mandatory wind profile: geopotentialAltitude ->
     *                          (speed, direction).  May be <code>null</code>.
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          profile remains unchanged.
     * @throws RemoteException  Java RMI exception.
     */
    public void setMandatoryWindProfile(Field field)
            throws VisADException, RemoteException, PropertyVetoException {
        mandatoryWindProperty.setField(field);
    }

    /**
     * Set the mandatory wind profile.  A mandatory wind profile consists
     * of the wind speed and direction at specific geopotential heights
     *
     * @param geopotentialAltitudeUnit        Unit for heights
     * @param geopotentialAltitudes           Array of height values
     * @param speedUnit                       Unit for wind speed values
     * @param speeds                          Array of wind speed values
     * @param directionUnit                   Unit of wind direction values
     * @param directions                      Array of wind direction values
     *
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws RemoteException  Java RMI exception.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          profile remains unchanged.
     */
    public void setMandatoryWindProfile(Unit geopotentialAltitudeUnit,
                                        float[] geopotentialAltitudes,
                                        Unit speedUnit, float[] speeds,
                                        Unit directionUnit,
                                        float[] directions)
            throws VisADException, RemoteException, PropertyVetoException {
        setMandatoryWindProfile(
            newMandatoryWindProfile(
                geopotentialAltitudeUnit, geopotentialAltitudes, speedUnit,
                speeds, directionUnit, directions));
    }




    /**
     * Gets the mandatory-wind-profile property.
     *
     * @return                  The mandatory wind profile:
     *                          geopotentialAltitude -> (speed, direction).
     *                          Won't be <code>null</code> but may
     *                          be empty.
     */
    public Field getMandatoryWindProfile() {
        return mandatoryWindProperty.getField();
    }

    /**
     * Sets the significant-wind-profile property.
     *
     * @param field             Significant wind profile: geopotentialAltitude
     *                          -> (speed, direction).  May be
     *                          <code>null</code>.
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          profile remains unchanged.
     * @throws RemoteException  Java RMI exception.
     */
    public void setSignificantWindProfile(Field field)
            throws VisADException, RemoteException, PropertyVetoException {
        significantWindProperty.setField(field);
    }

    /**
     * Set the significant wind profiler for this RAOB.  A significant
     * wind profile consists of wind speed and direction at significant
     * height levels.
     *
     * @param geopotentialAltitudeUnit         Unit of height values
     * @param geopotentialAltitudes            Array of height values
     * @param speedUnit                        Unit of wind speed values
     * @param speeds                           Array of wind speed values
     * @param directionUnit                    Unit of wind direction values
     * @param directions                       Array of wind direction values
     *
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          profile remains unchanged.
     * @throws RemoteException  Java RMI exception.
     */
    public void setSignificantWindProfile(Unit geopotentialAltitudeUnit,
                                          float[] geopotentialAltitudes,
                                          Unit speedUnit, float[] speeds,
                                          Unit directionUnit,
                                          float[] directions)
            throws VisADException, RemoteException, PropertyVetoException {
        setSignificantWindProfile(
            newSignificantWindProfile(
                geopotentialAltitudeUnit, geopotentialAltitudes, speedUnit,
                speeds, directionUnit, directions));

    }



    /**
     * Gets the significant-wind-profile property.
     *
     * @return                  The significant wind profile:
     *                          geopotentialAltitude -> (speed, direction).
     *                          Won't be <code>null</code> but may be empty.
     */
    public Field getSignificantWindProfile() {
        return significantWindProperty.getField();
    }

    /**
     * Sets the gravity property.
     *
     * @param gravity           The new value for gravity.
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.  The
     *                          data remains unchanged.
     * @throws RemoteException  Java RMI exception.
     */
    public void setGravity(Real gravity)
            throws VisADException, RemoteException, PropertyVetoException {
        gravityProperty.set(gravity);
    }

    /**
     * Gets the gravity value.
     * @return  the gravity value
     */
    public Real getGravity() {
        return gravityProperty.get();
    }

    /**
     * Gets the Pressure <-> Altitude coordinate system transformation property.
     *
     * @return                  The Pressure <-> Altitude coordinate system
     *                          transformation.
     */
    public CoordinateSystem getPressureCoordinateSystem() {
        return pressureCoordinateSystem.getCoordinateSystem();
    }

    /**
     * Gets the GeopotentialAltitude <-> Altitude coordinate system
     * transformation property.
     *
     * @return                  The GeopotentialAltitude <-> Altitude
     *                          coordinate system transformation.
     */
    public CoordinateSystem getGeopotentialAltitudeCoordinateSystem() {
        return geopotentialAltitudeCoordinateSystem.getCoordinateSystem();
    }

    /**
     * Gets the current temperature profile property.  The {@link MathType} of
     * the returned value is {@link InSituAirTemperatureProfile}.
     *
     * @return                  The temperature profile.
     */
    public FlatField getTemperatureProfile() {
        return temperatureProfileProperty.getFlatField();
    }

    /**
     * Gets the current dew-point profile property.  The {@link MathType} of
     * the return value is {@link DewPointProfile}.
     *
     * @return                  The dew-point profile.
     */
    public FlatField getDewPointProfile() {
        return dewPointProfileProperty.getFlatField();
    }

    /**
     * Gets the current wind profile property.
     *
     * @return                  The wind profile.
     */
    public FlatField getWindProfile() {
        return windProfileProperty.getFlatField();
    }

    /**
     * Gets the set of all possible links between input profiles and output
     * profiles.
     *
     * @return                  The set of all possible links between input
     *                          profiles and output profiles.  Each element of
     *                          the set is a Link.  The set is unmodifiable.
     */
    public LinkSet getPossibleLinks() {
        return possibleLinks;
    }

    /**
     * Gets the active links between input profiles and output profiles.
     *
     * @return                  The active links between input profiles and
     *                          output profiles.  Each element of the set is a
     *                          Link.  The set is unmodifiable.
     */
    public LinkSet getActiveLinks() {
        return activeLinks;
    }

    /**
     * Sets the active links between input profiles and output profiles.
     *
     * @param links             The active links between input profiles
     *                          and output profiles.  Each element of
     *                          the set shall also be an element of
     *                          <code>getPossibleLinks()</code>.
     *
     * @throws PropertyVetoException
     *                          A registered listener objected to the change,
     *                          which was not committed.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI exception.
     */
    public void setActiveLinks(LinkSet links)
            throws VisADException, RemoteException, PropertyVetoException {
        for (Iterator iter = links.SecondEndIterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            ((OutputProfileProperty) entry.getKey()).setContributors(
                (java.util.Set) entry.getValue());
        }
    }

    /**
     * Adjusts outside data.
     *
     * @param domainValues      The domain values.  The array needn't be
     *                          sorted and may contain NaN-s, infinities, and
     *                          duplicates.
     * @param rangeValues       The range values.
     * @param increasing        If true, then sort by increasing domain value;
     *                          otherwise, sort by decreasing domain value.
     */
    protected static void adjustData(float[][] domainValues,
                                     float[][] rangeValues,
                                     boolean increasing) {
        /*
         * Elminate NaN-s and infinities.
         */
        int[]   indexes;
        float[] values = domainValues[0];
        {
            int[] tmpIndexes = new int[values.length];
            int   n          = 0;
            for (int i = values.length; --i >= 0; ) {
                float value = values[i];
                if ( !Float.isNaN(value) && !Float.isInfinite(value)) {
                    tmpIndexes[n++] = i;
                }
            }
            indexes = new int[n];
            System.arraycopy(tmpIndexes, 0, indexes, 0, n);
        }
        if (indexes.length < values.length) {
            values = Util.take(values, indexes);
            for (int i = rangeValues.length; --i >= 0; ) {
                rangeValues[i] = Util.take(rangeValues[i], indexes);
            }
        }
        /*
         * Sort the data.
         */
        if ( !Util.isStrictlySorted(values)) {
            indexes = Util.strictlySortedIndexes(values, increasing);
            values  = Util.take(values, indexes);
            for (int i = rangeValues.length; --i >= 0; ) {
                rangeValues[i] = Util.take(rangeValues[i], indexes);
            }
        }
        domainValues[0] = values;
    }

    /**
     * Creates an instance of a MandatoryPressureProfile.
     *
     * @param pressureUnit      The unit of the pressure values.
     * @param pressures         The pressure values in units of
     *                          <code>pressureUnit</code>.  The array needn't
     *                          be sorted and may contain NaN-s and infinities.
     *                          The array is unmodified.
     * @param temperatureUnit   The unit for the in situ temperature
     *                          values.
     * @param temperatures      The in situ temperature values in units of
     *                          <code>temperatureUnit</code> corresponding to
     *                          the pressure values.  The array is unmodified.
     * @param dewPointUnit      The unit for the dew point temperature values.
     * @param dewPoints         The dew point temperature values in units of
     *                          <code>dewPointUnit</code> corresponding to the
     *                          pressure values.  The array is unmodified.
     * @param speedUnit         The unit of wind speed.
     * @param speeds            The speeds of the wind in units of
     *                          <code>speedUnit</code> corresponding to the
     *                          pressure values.  The array is unmodified.
     * @param directionUnit     The unit of meteorological direction.
     * @param directions        The directions of the wind in units of
     *                          <code>directionUnit</code> corresponding to the
     *                          pressure values.  The array is unmodified.
     * @param geopotentialAltitudeUnit
     *                          The unit of the geopotential altitude values.
     * @param geopotentialAltitudes The geopotential altitude values in units
     *                          of <code>geopotentialAltitudeUnit</code>
     *                          corresponding to the pressure values.  The array
     *                          is unmodified.
     * @return                  The mandatory pressure profile
     * @throws SetException     The pressure array has no valid data.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI exception.
     */
    public static MandatoryPressureProfile newMandatoryPressureProfile(
            Unit pressureUnit, float[] pressures, Unit temperatureUnit,
            float[] temperatures, Unit dewPointUnit, float[] dewPoints,
            Unit speedUnit, float[] speeds, Unit directionUnit,
            float[] directions, Unit geopotentialAltitudeUnit,
            float[] geopotentialAltitudes)
            throws SetException, RemoteException, VisADException {
        float[][] domainValues = {
            pressures
        };
        float[][] rangeValues  = {
            temperatures, dewPoints, speeds, directions, geopotentialAltitudes
        };
        adjustData(domainValues, rangeValues, false);
        return new MandatoryPressureProfile(pressureUnit, domainValues[0],
                                            temperatureUnit, rangeValues[0],
                                            dewPointUnit, rangeValues[1],
                                            speedUnit, rangeValues[2],
                                            directionUnit, rangeValues[3],
                                            geopotentialAltitudeUnit,
                                            rangeValues[4]);
    }

    /**
     * Creates an instance of a SignificantTemperatureProfile.
     *
     * @param pressureUnit      The unit of the pressure values.
     * @param pressures         The pressure values in units of
     *                          <code>pressureUnit</code>.  The array needn't be
     *                          sorted and may contain NaN-s and infinities.
     * @param temperatureUnit   The unit for the in situ temperature
     *                          values.
     * @param temperatures      The in situ temperature values in units of
     *                          <code>temperatureUnit</code> corresponding to
     *                          the pressure values.
     * @param dewPointUnit      The unit for the dew point temperature values.
     * @param dewPoints         The dew point temperature values in units of
     *                          <code>dewPointUnit</code> corresponding to the
     *                          pressure values.
     * @return                  A significant temperature profile
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI exception.
     */
    public static SignificantTemperatureProfile newSignificantTemperatureProfile(
            Unit pressureUnit, float[] pressures, Unit temperatureUnit,
            float[] temperatures, Unit dewPointUnit, float[] dewPoints)
            throws VisADException, RemoteException {
        float[][] domainValues = {
            pressures
        };
        float[][] rangeValues  = {
            temperatures, dewPoints
        };
        adjustData(domainValues, rangeValues, false);
        return new SignificantTemperatureProfile(pressureUnit,
                domainValues[0], temperatureUnit, rangeValues[0],
                dewPointUnit, rangeValues[1]);
    }

    /**
     * Creates an instance of a TropopauseProfile.
     *
     * @param pressureUnit      The unit of the pressure values.
     * @param pressures         The pressure values in units of
     *                          <code>pressureUnit</code>.  The array needn't be
     *                          sorted and may contain NaN-s and infinities.
     * @param temperatureUnit   The unit for the in situ temperature
     *                          values.
     * @param temperatures      The in situ temperature values in units of
     *                          <code>temperatureUnit</code> corresponding to
     *                          the pressure values.
     * @param dewPointUnit      The unit for the dew point temperature values.
     * @param dewPoints         The dew point temperature values in units of
     *                          <code>dewPointUnit</code> corresponding to the
     *                          pressure values.
     * @param speedUnit         The unit of wind speed.
     * @param speeds            The speeds of the wind in units of
     *                          <code>speedUnit</code> corresponding to the
     *                          pressure values.
     * @param directionUnit     The unit of meteorological direction.
     * @param directions        The directions of the wind in units of
     *                          <code>directionUnit</code> corresponding to the
     *                          pressure values.
     * @return                  A tropopause profile based on the input
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI exception.
     */
    public static TropopauseProfile newTropopauseProfile(Unit pressureUnit,
            float[] pressures, Unit temperatureUnit, float[] temperatures,
            Unit dewPointUnit, float[] dewPoints, Unit speedUnit,
            float[] speeds, Unit directionUnit, float[] directions)
            throws VisADException, RemoteException {
        float[][] domainValues = {
            pressures
        };
        float[][] rangeValues  = {
            temperatures, dewPoints, speeds, directions
        };
        adjustData(domainValues, rangeValues, false);
        return new TropopauseProfile(pressureUnit, domainValues[0],
                                     temperatureUnit, rangeValues[0],
                                     dewPointUnit, rangeValues[1], speedUnit,
                                     rangeValues[2], directionUnit,
                                     rangeValues[3]);
    }

    /**
     * Creates an instance of a MaximumWindProfile.
     *
     * @param pressureUnit      The unit of the pressure values.
     * @param pressures         The pressure values in units of
     *                          <code>pressureUnit</code>.  The array needn't be
     *                          sorted and may contain NaN-s and infinities.
     * @param speedUnit         The unit of wind speed.
     * @param speeds            The speeds of the wind in units of
     *                          <code>speedUnit</code> corresponding to the
     *                          pressure values.
     * @param directionUnit     The unit of meteorological direction.
     * @param directions        The directions of the wind in units of
     *                          <code>directionUnit</code> corresponding to the
     *                          pressure values.
     * @return                  A maximum wind profile based on the input
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI exception.
     */
    public static MaximumWindProfile newMaximumWindProfile(Unit pressureUnit,
            float[] pressures, Unit speedUnit, float[] speeds,
            Unit directionUnit, float[] directions)
            throws VisADException, RemoteException {
        float[][] domainValues = {
            pressures
        };
        float[][] rangeValues  = {
            speeds, directions
        };
        adjustData(domainValues, rangeValues, false);
        return new MaximumWindProfile(pressureUnit, domainValues[0],
                                      speedUnit, rangeValues[0],
                                      directionUnit, rangeValues[1]);
    }

    /**
     * Creates an instance of a MandatoryWindProfile.
     *
     * @param geopotentialAltitudeUnit
     *                          The unit of the geopotential altitude values.
     * @param geopotentialAltitudes
     *                          The geopotential altitude values in units
     *                          of <code>geopotentialAltitudeUnit</code>.  The
     *                          array needn't be sorted and may contain NaN-s
     *                          and infinities.
     * @param speedUnit         The unit of wind speed.
     * @param speeds            The speeds of the wind in units of
     *                          <code>speedUnit</code> corresponding to the
     *                          geopotential altitude values.
     * @param directionUnit     The unit of meteorological direction.
     * @param directions        The directions of the wind in units of
     *                          <code>directionUnit</code> corresponding to the
     *                          geopotential altitude values.
     * @return                  A MandatoryWindProfile based on the input
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI exception.
     */
    public static MandatoryWindProfile newMandatoryWindProfile(
            Unit geopotentialAltitudeUnit, float[] geopotentialAltitudes,
            Unit speedUnit, float[] speeds, Unit directionUnit,
            float[] directions)
            throws VisADException, RemoteException {
        float[][] domainValues = {
            geopotentialAltitudes
        };
        float[][] rangeValues  = {
            speeds, directions
        };
        adjustData(domainValues, rangeValues, true);
        return new MandatoryWindProfile(geopotentialAltitudeUnit,
                                        domainValues[0], speedUnit,
                                        rangeValues[0], directionUnit,
                                        rangeValues[1]);
    }

    /**
     * Creates an instance of a SignificantWindProfile.
     *
     * @param geopotentialAltitudeUnit
     *                          The unit of the geopotential altitude values.
     * @param geopotentialAltitudes
     *                          The geopotential altitude values in units
     *                          of <code>geopotentialAltitudeUnit</code>.  The
     *                          array needn't be sorted and may contain NaN-s
     *                          and infinities.
     * @param speedUnit         The unit of wind speed.
     * @param speeds            The speeds of the wind in units of
     *                          <code>speedUnit</code> corresponding to the
     *                          geopotential altitude values.
     * @param directionUnit     The unit of meteorological direction.
     * @param directions        The directions of the wind in units of
     *                          <code>directionUnit</code> corresponding to the
     *                          geopotential altitude values.
     * @return                  A SignificantWindProfile based on the input
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI exception.
     */
    public static SignificantWindProfile newSignificantWindProfile(
            Unit geopotentialAltitudeUnit, float[] geopotentialAltitudes,
            Unit speedUnit, float[] speeds, Unit directionUnit,
            float[] directions)
            throws VisADException, RemoteException {
        float[][] domainValues = {
            geopotentialAltitudes
        };
        float[][] rangeValues  = {
            speeds, directions
        };
        adjustData(domainValues, rangeValues, true);
        return new SignificantWindProfile(geopotentialAltitudeUnit,
                                          domainValues[0], speedUnit,
                                          rangeValues[0], directionUnit,
                                          rangeValues[1]);
    }

    /**
     * Creates a domain set.
     * @param type              The MathType of the set.
     * @param values            The values of the set.  Must have positive
     *                          length.
     * @param unit              The unit of the values.
     * @return                  The VisAD Set corresponding to the input.
     * @throws SetException     The value array has non-positive length.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected static visad.Set newSet(RealTupleType type, float[] values,
                                      Unit unit)
            throws SetException, RemoteException, VisADException {
        visad.Set set;
        if (values.length <= 0) {
            throw new SetException(
                "RAOB.newSet(): Non-positive-length value-array");
        }
        if (values.length == 1) {
            set = (visad.Set) new SingletonSet(
                new RealTuple(
                    type,
                    new Real[] {
                        new Real(
                            (RealType) type.getComponent(0), values[0],
                            unit) }, (CoordinateSystem) null), (CoordinateSystem) null, new Unit[] { unit }, (ErrorEstimate[]) null);
        } else {
            set = (visad.Set) new Gridded1DSet(type, new float[][] {
                values
            }, values.length, (CoordinateSystem) null, new Unit[] { unit },
               (ErrorEstimate[]) null);
        }
        return set;
    }

    /**
     * Handles a change to a coordinate system transformation.
     *
     * @param cs                   new CoordinateSystem
     * @param oldField             old field
     * @param property             property to change
     * @return  true if successful
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected boolean adjustCoordinateSystem(CoordinateSystem cs,
                                             Field oldField,
                                             Property property)
            throws VisADException, RemoteException {
        boolean change;
        if (cs.equals(oldField.getDomainCoordinateSystem())) {
            change = false;
        } else {
            try {
                FlatField newField = (FlatField) Util.clone(
                                         (FlatField) oldField.local(),
                                         Util.clone(
                                             oldField.getDomainSet(), cs));
                property.setValueAndNotifyListeners(newField);
            } catch (Exception e) {
                throw new VisADException(this.getClass().getName()
                                         + ": Couldn't set property "
                                         + property.getName() + ": " + e);
            }
            change = true;
        }
        return change;
    }

    /*
     * Supporting inner classes:
     */

    /**
     * Provides support for Field data on a pressure domain.
     */
    private static abstract class PressureProfile extends FlatField {

        /**
         * Constructs an empty instance from nothing.
         *
         *
         * @param rangeType
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI failure.
         */
        private PressureProfile(MathType rangeType)
                throws VisADException, RemoteException {
            super(new FunctionType(
                AirPressure.getRealTupleType(), rangeType), new SingletonSet(
                new RealTuple(
                    AirPressure.getRealTupleType(),
                    new Real[] {
                        new Real(
                            (RealType) AirPressure.getRealType(), 500,
                            CommonUnits
                                .HECTOPASCAL) }, (CoordinateSystem) null)));
        }

        /**
         * Constructs from data.
         *
         * @param rangeType             The MathType of the range of the
         *                              Function.
         * @param pressureUnit          The unit of the pressure values.
         * @param pressures             The pressure values in units of
         *                              <code>pressureUnit</code>.  The array
         *                              must be sorted (either increasing or
         *                              decreasing) and not contain any NaN-s
         *                              or infinities.
         * @param rangeUnits            The units for the (flat) range
         *                              components.  <code>rangeUnits[i]</code>
         *                              is the unit of the values for the
         *                              <code>i</code_th flat range component.
         * @param rangeValues           The values of the range components.
         *                              <code>rangeValues[i]</code> are the
         *                              values in units of
         *                              <code>rangeUnits[i]</code> for the
         *                              <code>i</code_th flat range component.
         * @throws SetException         The pressure array has non-positive
         *                              length.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public PressureProfile(MathType rangeType, Unit pressureUnit,
                               float[] pressures, Unit[] rangeUnits,
                               float[][] rangeValues)
                throws SetException, RemoteException, VisADException {
            super(new FunctionType(
                AirPressure.getRealTupleType(), rangeType), newSet(
                AirPressure.getRealTupleType(), pressures,
                pressureUnit), (CoordinateSystem[]) null, (visad.Set[]) null,
                               rangeUnits);
            setSamples(rangeValues);
        }
    }

    /**
     * Provides support for mandatory pressure data.
     */
    public static final class MandatoryPressureProfile extends PressureProfile {

        /**
         * The MathType of the range.
         */
        private static MathType rangeType;

        static {
            try {
                rangeType = new TupleType(new MathType[] {
                    InSituAirTemperature.getRealType(),
                    DewPoint.getRealType(),
                    PolarHorizontalWind.getRealTupleType(),
                    GeopotentialAltitude.getRealType() });
            } catch (Exception e) {
                System.err.println(
                    "Couldn't initialize class RAOB.MandatoryPressureProfile: "
                    + e);
            }
        }

        /**
         * Constructs an empty instance from nothing.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public MandatoryPressureProfile()
                throws VisADException, RemoteException {
            super(rangeType);
        }

        /**
         * Constructs from data.
         *
         * @param pressureUnit          The unit of the pressure values.
         * @param pressures             The pressure values in units of
         *                              <code>pressureUnit</code>.  The array
         *                              must be sorted (either increasing or
         *                              decreasing) and not contain any NaN-s
         *                              or infinities.
         * @param temperatureUnit       The unit for the in situ temperature
         *                              values.
         * @param temperatures          The in situ temperature values in
         *                              units of <code>temperatureUnit</code>
         *                              corresponding to the pressure values.
         * @param dewPointUnit          The unit for the dew point temperature
         *                              values.
         * @param dewPoints             The dew point temperature values in
         *                              units of <code>dewPointUnit</code>
         *                              corresponding to the pressure values.
         * @param speedUnit             The unit of wind speed.
         * @param speeds                The speeds of the wind in units of
         *                              <code>speedUnit</code> corresponding to
         *                              the pressure values.
         * @param directionUnit         The unit of meteorological direction.
         * @param directions            The directions of the wind in units of
         *                              <code>directionUnit</code> corresponding
         *                              to the pressure values.
         * @param geopotentialAltitudeUnit
         *                              The unit of the geopotential altitude
         *                              values.
         * @param geopotentialAltitudes The geopotential altitude
         *                              values in units of
         *                              <code>geopotentialAltitudeUnit</code>
         *                              corresponding to the pressure values.
         * @throws SetException         The pressure array has non-positive
         *                              length.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public MandatoryPressureProfile(Unit pressureUnit, float[] pressures,
                                        Unit temperatureUnit,
                                        float[] temperatures,
                                        Unit dewPointUnit, float[] dewPoints,
                                        Unit speedUnit, float[] speeds,
                                        Unit directionUnit,
                                        float[] directions,
                                        Unit geopotentialAltitudeUnit,
                                        float[] geopotentialAltitudes)
                throws SetException, RemoteException, VisADException {
            super(rangeType, pressureUnit, pressures,
                  new Unit[] { temperatureUnit,
                               dewPointUnit, speedUnit, directionUnit,
                               geopotentialAltitudeUnit }, new float[][] {
                temperatures, dewPoints, speeds, directions,
                geopotentialAltitudes
            });
        }
    }

    /**
     * Provides support for significant temperature data.
     */
    public static final class SignificantTemperatureProfile extends PressureProfile {

        /**
         * The MathType of the range.
         */
        private static MathType rangeType;

        static {
            try {
                rangeType =
                    new RealTupleType(InSituAirTemperature.getRealType(),
                                      DewPoint.getRealType());
            } catch (Exception e) {
                System.err.println(
                    "Couldn't initialize class RAOB.SignificantTemperatureProfile: "
                    + e);
            }
        }

        /**
         * Constructs an empty instance from nothing.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public SignificantTemperatureProfile()
                throws VisADException, RemoteException {
            super(rangeType);
        }

        /**
         * Constructs from data.
         *
         * @param pressureUnit          The unit of the pressure values.
         * @param pressures             The pressure values in units of
         *                              <code>pressureUnit</code>.  The array
         *                              must be sorted (either increasing or
         *                              decreasing) and not contain a NaN.
         * @param temperatureUnit       The unit for the in situ temperature
         *                              values.
         * @param temperatures          The in situ temperature values in units
         *                              of <code>temperatureUnit</code>.
         * @param dewPointUnit          The unit for the dew point temperature
         *                              values.
         * @param dewPoints             The dew point temperature values in
         *                              units of <code>dewPointUnit</code>.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public SignificantTemperatureProfile(Unit pressureUnit,
                                             float[] pressures,
                                             Unit temperatureUnit,
                                             float[] temperatures,
                                             Unit dewPointUnit,
                                             float[] dewPoints)
                throws VisADException, RemoteException {
            super(rangeType, pressureUnit, pressures,
                  new Unit[] { temperatureUnit,
                               dewPointUnit }, new float[][] {
                temperatures, dewPoints
            });
        }
    }

    /**
     * Provides support for tropopause data.
     */
    public static final class TropopauseProfile extends PressureProfile {

        /**
         * The MathType of the range.
         */
        private static MathType rangeType;

        static {
            try {
                rangeType = new TupleType(new MathType[] {
                    InSituAirTemperature.getRealType(),
                    DewPoint.getRealType(),
                    PolarHorizontalWind.getRealTupleType() });
            } catch (Exception e) {
                System.err.println(
                    "Couldn't initialize class RAOB.TropopauseProfile" + e);
            }
        }

        /**
         * Constructs an empty instance from nothing.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public TropopauseProfile() throws VisADException, RemoteException {
            super(rangeType);
        }

        /**
         * Constructs from data.
         *
         * @param pressureUnit          The unit of the pressure values.
         * @param pressures             The pressure values in units of
         *                              <code>pressureUnit</code>.  The array
         *                              must be sorted (either increasing or
         *                              decreasing) and not contain a NaN.
         * @param temperatureUnit       The unit for the in situ temperature
         *                              values.
         * @param temperatures          The in situ temperature values in units
         *                              of <code>temperatureUnit</code>.
         * @param dewPointUnit          The unit for the dew point temperature
         *                              values.
         * @param dewPoints             The dew point temperature values in
         *                              units of <code>dewPointUnit</code>.
         * @param speedUnit             The unit of wind speed.
         * @param speeds                The speeds of the wind in units of
         *                              <code>speedUnit</code>.
         * @param directionUnit         The unit of direction of the wind.
         * @param directions            The directions of the wind in units of
         *                              <code>directionUnit</code>.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public TropopauseProfile(Unit pressureUnit, float[] pressures,
                                 Unit temperatureUnit, float[] temperatures,
                                 Unit dewPointUnit, float[] dewPoints,
                                 Unit speedUnit, float[] speeds,
                                 Unit directionUnit, float[] directions)
                throws VisADException, RemoteException {
            super(rangeType, pressureUnit, pressures,
                  new Unit[] { temperatureUnit,
                               dewPointUnit, speedUnit,
                               directionUnit }, new float[][] {
                temperatures, dewPoints, speeds, directions
            });
        }
    }

    /**
     * Provides support for maximum wind data.
     */
    public static final class MaximumWindProfile extends PressureProfile {

        /**
         * The MathType of the range.
         */
        private static MathType rangeType;

        static {
            try {
                rangeType = PolarHorizontalWind.getRealTupleType();
            } catch (Exception e) {
                System.err.println(
                    "Couldn't initialize class RAOB.MaximumWindProfile: "
                    + e);
            }
        }

        /**
         * Constructs an empty instance from nothing.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public MaximumWindProfile() throws VisADException, RemoteException {
            super(rangeType);
        }

        /**
         * Constructs from data.
         *
         * @param pressureUnit          The unit of the pressure values.
         * @param pressures             The pressure values in units of
         *                              <code>pressureUnit</code>.  The array
         *                              must be sorted (either increasing or
         *                              decreasing) and not contain a NaN.
         * @param speedUnit             The unit of wind speed.
         * @param speeds                The speeds of the wind in units of
         *                              <code>speedUnit</code>.
         * @param directionUnit         The unit of meteorological direction.
         * @param directions            The directions of the wind in units of
         *                              <code>directionUnit</code>.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public MaximumWindProfile(Unit pressureUnit, float[] pressures,
                                  Unit speedUnit, float[] speeds,
                                  Unit directionUnit, float[] directions)
                throws VisADException, RemoteException {
            super(rangeType, pressureUnit, pressures, new Unit[] { speedUnit,
                    directionUnit }, new float[][] {
                speeds, directions
            });
        }
    }

    /**
     * Provides support for wind data on a geopotential altitude domain.
     */
    private static abstract class GeopotentialWindProfile extends FlatField {

        /**
         * The FunctionType of the function.
         */
        private static FunctionType functionType;

        static {
            try {
                functionType =
                    new FunctionType(GeopotentialAltitude.getRealTupleType(),
                                     PolarHorizontalWind.getRealTupleType());
            } catch (Exception e) {
                System.err.println(
                    "Couldn't initialize class RAOB.GeopotentialWindProfile: "
                    + e);
            }
        }

        /**
         * Constructs an empty instance from nothing.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI failure.
         */
        private GeopotentialWindProfile()
                throws VisADException, RemoteException {
            super(functionType,
                  new SingletonSet(new RealTuple(new Real[] {
                      new Real(GeopotentialAltitude.getRealType(), 0) })));
        }

        /**
         * Constructs from data.
         *
         * @param geopotentialAltitudeUnit
         *                              The unit of the geopotential altitude
         *                              values.
         * @param geopotentialAltitudes The geopotential altitude values in
         *                              units of
         *                              <code>geopotentialAltitudeUnit</code>.
         *                              The array must be sorted and not contain
         *                              any NaN-s or infinities.
         * @param speedUnit             The unit of wind speed.
         * @param speeds                The speeds of the wind in units of
         *                              <code>speedUnit</code> corresponding
         *                              to the geopotential altitude values.
         * @param directionUnit         The unit of meteorological direction.
         * @param directions            The directions of the wind in units of
         *                              <code>directionUnit</code> corresponding
         *                              to the geopotential altitude values.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        private GeopotentialWindProfile(Unit geopotentialAltitudeUnit,
                                        float[] geopotentialAltitudes,
                                        Unit speedUnit, float[] speeds,
                                        Unit directionUnit,
                                        float[] directions)
                throws VisADException, RemoteException {
            super(functionType, newSet(GeopotentialAltitude
                .getRealTupleType(), geopotentialAltitudes, geopotentialAltitudeUnit), (CoordinateSystem[]) null, (visad
                    .Set[]) null, new Unit[] { speedUnit,
                    directionUnit });
            setSamples(new float[][] {
                speeds, directions
            });
        }
    }

    /**
     * Provides support for mandatory wind data.
     */
    public static final class MandatoryWindProfile extends GeopotentialWindProfile {

        /**
         * Constructs an empty instance from nothing.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public MandatoryWindProfile() throws VisADException, RemoteException {
            super();
        }

        /**
         * Constructs from data.
         *
         * @param geopotentialAltitudeUnit
         *                              The unit of the geopotential altitude
         *                              values.
         * @param geopotentialAltitudes The geopotential altitude values in
         *                              units of
         *                              <code>geopotentialAltitudeUnit</code>.
         *                              The array must be sorted and not contain
         *                              any NaN-s or infinities.
         * @param speedUnit             The unit of wind speed.
         * @param speeds                The speeds of the wind in units of
         *                              <code>speedUnit</code> corresponding
         *                              to the geopotential altitude values.
         * @param directionUnit         The unit of meteorological direction.
         * @param directions            The directions of the wind in units of
         *                              <code>directionUnit</code> corresponding
         *                              to the geopotential altitude values.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public MandatoryWindProfile(Unit geopotentialAltitudeUnit,
                                    float[] geopotentialAltitudes,
                                    Unit speedUnit, float[] speeds,
                                    Unit directionUnit, float[] directions)
                throws VisADException, RemoteException {
            super(geopotentialAltitudeUnit, geopotentialAltitudes, speedUnit,
                  speeds, directionUnit, directions);
        }
    }

    /**
     * Provides support for significant wind data.
     */
    public static final class SignificantWindProfile extends GeopotentialWindProfile {

        /**
         * Constructs an empty instance from nothing.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public SignificantWindProfile()
                throws VisADException, RemoteException {
            super();
        }

        /**
         * Constructs from data.
         *
         * @param geopotentialAltitudeUnit
         *                              The unit of the geopotential altitude
         *                              values.
         * @param geopotentialAltitudes The geopotential altitude values in
         *                              units of
         *                              <code>geopotentialAltitudeUnit</code>.
         *                              The array must be sorted and not contain
         *                              any NaN-s or infinities.
         * @param speedUnit             The unit of wind speed.
         * @param speeds                The speeds of the wind in units of
         *                              <code>speedUnit</code> corresponding
         *                              to the geopotential altitude values.
         * @param directionUnit         The unit of meteorological direction.
         * @param directions            The directions of the wind in units of
         *                              <code>directionUnit</code> corresponding
         *                              to the geopotential altitude values.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public SignificantWindProfile(Unit geopotentialAltitudeUnit,
                                      float[] geopotentialAltitudes,
                                      Unit speedUnit, float[] speeds,
                                      Unit directionUnit, float[] directions)
                throws VisADException, RemoteException {
            super(geopotentialAltitudeUnit, geopotentialAltitudes, speedUnit,
                  speeds, directionUnit, directions);
        }
    }

    /**
     * Provides support for coordinate system transformation Bean properties.
     */
    private abstract class CoordinateSystemProperty extends NonVetoableProperty {

        /**
         * Create a new CoordinateSystemProperty.
         *
         * @param name         name of the property
         * @param defaultCS    the default CoordinateSystem
         *
         */
        protected CoordinateSystemProperty(String name,
                                           CoordinateSystem defaultCS) {
            super(RAOB.this, name);
            setValue(defaultCS);
        }

        /**
         * Respond to changes in Gravity
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public abstract void gravityChanged()
         throws VisADException, RemoteException, PropertyVetoException;

        /**
         * Get the CoordinateSystem for this property
         * @return  the CoordinateSystem
         */
        public CoordinateSystem getCoordinateSystem() {
            return (CoordinateSystem) getValue();
        }
    }

    /**
     * Provides support for the Pressure <-> Altitude coordinate system
     * transformation JavaBean property.
     */
    private final class PressureCoordinateSystem extends CoordinateSystemProperty {

        /** helper for the pressure CS */
        private PressureCSHelper pressureCSHelper = null;

        /**
         * Create a new PressureCoordinateSystem
         *
         * @param pressureCSHelper   helper
         *
         * @throws VisADException    couldn't create the object
         */
        public PressureCoordinateSystem(PressureCSHelper pressureCSHelper)
                throws VisADException {
            super("pressureCoordinateSystem",
                  AirPressure.getRealTupleType().getCoordinateSystem());
            this.pressureCSHelper = pressureCSHelper;
            pressureCSHelper.setActive(true);
        }

        /**
         * Set the helper for this PressureCoordinateSystem
         *
         * @param pressureCSHelper     the helper
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public void setPressureCSHelper(PressureCSHelper pressureCSHelper)
                throws VisADException, RemoteException,
                       PropertyVetoException {
            this.pressureCSHelper.setActive(false);
            this.pressureCSHelper = pressureCSHelper;
            pressureCSHelper.setActive(true);
            setValue(pressureCSHelper.getCoordinateSystem());
            notifyListeners();
            notifyAffectedProfiles();
        }

        /**
         * Notify any affected profiles because of changes
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws UnimplementedException  not implemented
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public void notifyAffectedProfiles()
                throws VisADException, RemoteException,
                       UnimplementedException, PropertyVetoException {
            CoordinateSystem cs = getCoordinateSystem();
            mandatoryPressureProperty.coordinateSystemChanged(cs);
            significantTemperatureProperty.coordinateSystemChanged(cs);
            tropopauseProperty.coordinateSystemChanged(cs);
            maximumWindProperty.coordinateSystemChanged(cs);
            temperatureProfileProperty.coordinateSystemChanged(cs);
            dewPointProfileProperty.coordinateSystemChanged(cs);
        }

        /**
         * Respond to Gravity changes
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public void gravityChanged()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            pressureCSHelper.gravityChanged();
        }

        /**
         * respond to changes in the mandatory pressure profile
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public void mandatoryPressureProfileChanged()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            pressureCSHelper.mandatoryPressureProfileChanged();
        }
    }

    /**
     * Provides support for the PressureCoordinateSystem.
     */
    private static abstract class PressureCSHelper {

        /** flag for activity */
        protected boolean isActive = false;

        /**
         * Create a new PressureCSHelper
         */
        protected PressureCSHelper() {}

        /**
         * Respond to change in Gravity
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public abstract void gravityChanged()
         throws VisADException, RemoteException, PropertyVetoException;

        /**
         * Respond to changes in the mandatory pressure profile
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public abstract void mandatoryPressureProfileChanged()
         throws VisADException, RemoteException, PropertyVetoException;

        /**
         * Get the associated CoordinateSystem
         * @return  the CS
         *
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public abstract CoordinateSystem getCoordinateSystem()
         throws VisADException, RemoteException;

        /**
         * Set the activity flag
         *
         * @param isActive  true if this should be active
         */
        public void setActive(boolean isActive) {
            this.isActive = isActive;
        }
    }

    /**
     * Provides support for a Pressure <-> Altitude CoordinateSystem based on
     * the Pressure -> GeopotentialAltitude data in the mandatory pressure
     * profile.
     */
    private final class MandatoryPressureCSHelper extends PressureCSHelper {

        /** the coordinate system */
        private CoordinateSystem cs;

        /** flag for recomputation */
        private boolean recomputationNeeded = false;

        /**
         * Respond to changes in Gravity
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public void gravityChanged()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            contributorChanged();
        }

        /**
         * Respond to changes in the mandatory pressure profile
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public void mandatoryPressureProfileChanged()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            contributorChanged();
        }

        /**
         * A contributor to this helper changed
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        private void contributorChanged()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            if ( !isActive) {
                recomputationNeeded = true;
            } else {
                recomputationNeeded = true;
                recompute();
                propagateChange();
            }
        }

        /**
         * Propage a change to the system
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        private void propagateChange()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            pressureCoordinateSystem.setValue(cs);
            pressureCoordinateSystem.notifyListeners();
            pressureCoordinateSystem.notifyAffectedProfiles();
        }

        /**
         * Recompute the helper
         *
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        private void recompute() throws VisADException, RemoteException {
            if (recomputationNeeded) {
                recomputationNeeded = false;
                FlatField geopotentialField =
                    ensureValidRange(
                        (FlatField) DataUtility.ensureRange(
                            mandatoryPressureProperty.getFlatField(),
                            GeopotentialAltitude.getRealType()));
                try {
                    cs = ((geopotentialField == null)
                          || geopotentialField.isMissing()
                          || (geopotentialField.getLength() <= 1))
                         ? (CoordinateSystem) null
                         : EmpiricalCoordinateSystem.create(
                             (FlatField) GeopotentialAltitude.toAltitude(
                                 geopotentialField, gravityProperty.get()));
                } catch (SetException e) {
                    cs =  //(CoordinateSystem) null;
                        AirPressure.getRealTupleType().getCoordinateSystem();
                }
            }
        }

        /**
         * Get the coordinate system
         * @return  the CoordinateSystem
         *
         * @throws RemoteException    Java RMI problem
         * @throws VisADException     VisAD problem
         */
        public CoordinateSystem getCoordinateSystem()
                throws VisADException, RemoteException {
            recompute();
            return cs;
        }
    }

    /**
     * Provides support for the GeopotentialAltitude <-> Altitude coordinate
     * system transformation JavaBean property.
     */
    private final class GeopotentialAltitudeCoordinateSystem extends CoordinateSystemProperty {

        /**
         * Create a new GeopotentialAltitudeCoordinateSystem
         *
         * @throws VisADException    couldn't create the CS
         */
        public GeopotentialAltitudeCoordinateSystem() throws VisADException {
            super("geopotentialAltitudeCoordinateSystem",
                  GeopotentialAltitude.getRealTupleType()
                      .getCoordinateSystem());
        }

        /**
         * Respond to changes in gravity
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public void gravityChanged()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            CoordinateSystem cs =
                GeopotentialAltitude.getRealTupleType().getCoordinateSystem();
            setValueAndNotifyListeners(cs);

            mandatoryWindProperty.coordinateSystemChanged(cs);
            significantWindProperty.coordinateSystemChanged(cs);
            windProfileProperty.coordinateSystemChanged(cs);
        }
    }

    /**
     * Provides support for the gravity property of this RAOB.
     */
    private class GravityProperty extends VetoableProperty {

        /**
         * Create a new GravityProperty
         *
         * @throws VisADException    couldn't create the property
         *
         */
        public GravityProperty() throws VisADException {
            super(RAOB.this, "gravity");
            try {
                setValue(Gravity.newReal());
            } catch (PropertyVetoException e) {}  // can't happen
        }

        /**
         * Get the value
         *
         * @return  the value
         */
        protected final Real get() {
            return (Real) getValue();
        }

        /**
         * Sets the value of gravity.
         *
         * @param gravity       The new value of gravity.  If <code>null</code>,
         *                      then the default gravity value is used.
         * @throws PropertyVetoException
         *                      A registered listener objected to the change,
         *                      which was not committed.
         * @throws VisADException
         *                      Couldn't create necessary VisAD object.
         * @throws RemoteException
         *                      Java RMI failure.
         */
        protected final void set(Real gravity)
                throws PropertyVetoException, VisADException,
                       RemoteException {
            setValue(gravity);  // don't notify
            try {
                pressureCoordinateSystem.gravityChanged();
                geopotentialAltitudeCoordinateSystem.gravityChanged();
            } finally {
                notifyListeners();  // notify now
            }
        }
    }

    /**
     * Provides support for a set of contributors to an output profile.
     */
    private final class OutputProfileContributors extends TreeSet {

        /**
         * Create a OutputProfileContributors
         *
         * @param inputProfileProperties   set of properties
         *
         */
        public OutputProfileContributors(
                InputProfileProperty[] inputProfileProperties) {
            super(new Comparator() {
                public int compare(Object obj1, Object obj2) {
                    return ((InputProfileProperty) obj1).getName().compareTo(
                        ((InputProfileProperty) obj2).getName());
                }

                public boolean equals(Object obj1, Object obj2) {
                    return compare(obj1, obj2) == 0;
                }
            });
            for (int i = inputProfileProperties.length; --i >= 0; ) {
                add(inputProfileProperties[i]);
            }
        }

        /**
         * Get the contributors to the ouptut profile
         * @return   array of contributors
         */
        public InputProfileProperty[] getContributors() {
            return (InputProfileProperty[]) toArray(
                new InputProfileProperty[size()]);
        }

        /**
         * Check to see if a given input profile property is a contributo
         * to the output.
         *
         * @param property   property to check
         * @return  true if it is
         */
        public boolean isContributor(InputProfileProperty property) {
            return contains(property);
        }
    }

    /**
     * Provides support for an input-profile property.
     */
    private abstract class InputProfileProperty extends VetoableProperty {

        /**
         * Create a new input profile property
         *
         * @param name              the name of the property
         * @param defaultFlatField  the default value of the property
         *
         */
        public InputProfileProperty(String name, FlatField defaultFlatField) {
            super(RAOB.this, name);
            try {
                setValue(defaultFlatField);
            } catch (PropertyVetoException e) {}  // can't happen
        }

        /**
         * Get the field for this property (may be remote)
         * @return  the Field
         */
        public final Field getField() {
            return (Field) getValue();
        }

        /**
         * Get the FlatField for this property (local copy)
         * @return  the local flat field
         */
        public final FlatField getFlatField() {
            return (FlatField) ((FlatField) getValue()).local();
        }

        /**
         * Set the Field for this property.
         *
         * @param newField      The new Field.  May be <code>null</code>,
         *                      in which case the default Field is used.
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws RemoteException      Java RMI exception.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public void setField(Field newField)
                throws VisADException, RemoteException,
                       PropertyVetoException {
            setValueAndNotifyListeners(newField);
            setOutputProfiles();
        }

        /**
         * Handles a change to the coordinate system transformation.
         *
         *
         * @param cs    the new coordinate system
         *
         * @throws RemoteException              Java RMI problem
         * @throws UnimplementedException       Can't clone domain set of field.
         * @throws VisADException               Couldn't create the VisAD object
         */
        public void coordinateSystemChanged(CoordinateSystem cs)
                throws VisADException, RemoteException,
                       UnimplementedException {
            if (adjustCoordinateSystem(cs, getField(), this)) {
                notifyListeners();
                setOutputProfiles();
            }
        }

        /**
         * Sets potentially-affected output-profiles.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        protected abstract void setOutputProfiles()
         throws VisADException, RemoteException;
    }

    /**
     * Provides support for the mandatory pressure profile-property.  In
     * general, changes to this profile affect the Pressure <-> Altitude
     * coordinate system transformation.
     */
    private final class MandatoryPressureProperty extends InputProfileProperty {

        /**
         * Create a new MandatorPressureProperty
         *
         */
        public MandatoryPressureProperty() {
            super("mandatoryPressureProfile", emptyMandatoryPressureProfile);
        }

        /**
         * Sets the value of this property.  This method is overridden in order
         * to propagate changes to the Pressure <-> Altitude coordinate system
         * transformation.
         *
         * @param newField              The new value for this property.  May
         *                              be <code>null</code>, in which case the
         *                              default Field is used.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI failure.
         */
        public synchronized void setField(Field newField)
                throws VisADException, RemoteException {
            Field original = getField();
            /*
             * Defer ramifications of a new mandatory pressure profile.
             */
            setReporting(false);
            temperatureProfileProperty.disableRecomputation();
            dewPointProfileProperty.disableRecomputation();
            windProfileProperty.disableRecomputation();

            try {
                setValue(newField);
                pressureCoordinateSystem.mandatoryPressureProfileChanged();
                setOutputProfiles();
            } catch (Exception e) {
                try {
                    setReporting(true);
                    temperatureProfileProperty.enableRecomputation();
                    dewPointProfileProperty.enableRecomputation();
                    windProfileProperty.enableRecomputation();

                    setValueAndNotifyListeners(original);
                } catch (PropertyVetoException tmp) {}  // ignore
            }
            /*
             * Enable propagation of change.
             */
            setReporting(true);
            try {
                temperatureProfileProperty.enableRecomputation();
                dewPointProfileProperty.enableRecomputation();
                windProfileProperty.enableRecomputation();
            } catch (PropertyVetoException e) {}  // ignore
              catch (NullPointerException excp) {} // ignore some missing data in raob
        }

        /**
         * Sets potentially-affected output profiles.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        protected void setOutputProfiles()
                throws VisADException, RemoteException {
            try {
                temperatureProfileProperty.contributorChanged(this);
                dewPointProfileProperty.contributorChanged(this);
                windProfileProperty.contributorChanged(this);
            } catch (PropertyVetoException e) {}  // ignore
        }
    }

    /**
     * Provides support for the significant temperature profile-property.
     */
    private final class SignificantTemperatureProperty extends InputProfileProperty {

        /**
         * Create a new SignificantTemperatureProperty
         *
         */
        public SignificantTemperatureProperty() {
            super("significantTemperatureProfile",
                  emptySignificantTemperatureProfile);
        }

        /**
         * Sets potentially-affected output profiles.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        protected void setOutputProfiles()
                throws VisADException, RemoteException {
            try {
                temperatureProfileProperty.contributorChanged(this);
                dewPointProfileProperty.contributorChanged(this);
            } catch (PropertyVetoException e) {}  // ignore
        }
    }

    /**
     * Provides support for the tropopause profile property.
     */
    private final class TropopauseProperty extends InputProfileProperty {

        /**
         * Create a TropopauseProperty
         *
         */
        public TropopauseProperty() {
            super("tropopauseProfile", emptyTropopauseProfile);
        }

        /**
         * Sets potentially-affected output profiles.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        protected void setOutputProfiles()
                throws VisADException, RemoteException {
            try {
                temperatureProfileProperty.contributorChanged(this);
                dewPointProfileProperty.contributorChanged(this);
                windProfileProperty.contributorChanged(this);
            } catch (PropertyVetoException e) {}  // ignore
        }
    }

    /**
     * Provides support for the mandatory wind property.
     */
    private final class MandatoryWindProperty extends InputProfileProperty {

        /**
         * Create a MandatoryWindProperty
         *
         */
        public MandatoryWindProperty() {
            super("mandatoryWindProfile", emptyMandatoryWindProfile);
        }

        /**
         * Sets potentially-affected output profiles.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        protected void setOutputProfiles()
                throws VisADException, RemoteException {
            try {
                windProfileProperty.contributorChanged(this);
            } catch (PropertyVetoException e) {}  // ignore
        }
    }

    /**
     * Provides support for the significant wind property.
     */
    private final class SignificantWindProperty extends InputProfileProperty {

        /**
         * Create a SignificantWindProperty
         */
        public SignificantWindProperty() {
            super("significantWindProfile", emptySignificantWindProfile);
        }

        /**
         * Sets potentially-affected output profiles.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        protected void setOutputProfiles()
                throws VisADException, RemoteException {
            try {
                windProfileProperty.contributorChanged(this);
            } catch (PropertyVetoException e) {}  // ignore
        }
    }

    /**
     * Provides support for the maximum wind property.
     */
    private final class MaximumWindProperty extends InputProfileProperty {

        /**
         * Create a MaximumWindProperty
         */
        public MaximumWindProperty() {
            super("maximumWindProfile", emptyMaximumWindProfile);
        }

        /**
         * Sets potentially-affected output profiles.
         *
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        protected void setOutputProfiles()
                throws VisADException, RemoteException {
            try {
                windProfileProperty.contributorChanged(this);
            } catch (PropertyVetoException e) {}  // ignore
        }
    }

    /**
     * Provides support for an output-profile property.
     */
    private final class OutputProfileProperty extends NonVetoableProperty {

        /**
         * @serial
         */
        private CoordinateSystem domainCoordinateSystem;

        /**
         * @serial
         */
        private final FunctionType functionType;

        /**
         * @serial
         */
        private final OutputProfileContributors contributors;

        /** flag for recompute */
        private boolean recomputationNeeded = false;

        /** flag for enabling recomputation */
        private boolean recomputationEnabled = true;

        /**
         * Create a new OutputProfileProperty
         *
         * @param name                        name of the property
         * @param defaultFlatField            default property value
         * @param domainCoordinateSystem      CS for domain
         * @param contributors                set of contributors
         *
         * @throws RemoteException    Java RMI problem
         * @throws VisADException     VisAD problem
         */
        public OutputProfileProperty(String name, FlatField defaultFlatField,
                                     CoordinateSystem domainCoordinateSystem,
                                     OutputProfileContributors contributors)
                throws VisADException, RemoteException {
            super(RAOB.this, name);
            setValue(defaultFlatField);
            this.domainCoordinateSystem = domainCoordinateSystem;
            this.contributors           = contributors;
            functionType = (FunctionType) defaultFlatField.getType();
        }

        /**
         * Get the FlatField for this property
         * @return   the value
         */
        public final FlatField getFlatField() {
            return (FlatField) getValue();
        }

        /**
         * Set the value for this property
         *
         * @param newFlatField  The new FlatField.  May be <code>null</code>,
         *                      in which case the default FlatField is used.
         *
         * @throws PropertyVetoException    problem setting property
         */
        public void setFlatField(FlatField newFlatField)
                throws PropertyVetoException {
            setValueAndNotifyListeners(newFlatField);
        }

        /**
         * Set the contributors to this OutputProfileProperty
         *
         * @param contributors   set of contributors
         *
         * @throws PropertyVetoException   couldn't set the property
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public void setContributors(java.util.Set contributors)
                throws VisADException, RemoteException,
                       PropertyVetoException {
            this.contributors.clear();
            this.contributors.addAll(contributors);
            recomputationNeeded = true;
            recompute();
        }

        /**
         * Get the of contributors to this property
         * @return   array of contributors
         */
        public final InputProfileProperty[] getContributors() {
            return contributors.getContributors();
        }

        /**
         * Disable the recomputation
         */
        public void disableRecomputation() {
            recomputationEnabled = false;
        }

        /**
         * Enable recomputation.
         *
         * @throws PropertyVetoException  couldn't set the property
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public void enableRecomputation()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            recomputationEnabled = true;
            recompute();
        }

        /**
         * Respond to a change in a contributor
         *
         * @param input  input contributor that changed
         *
         * @throws PropertyVetoException   couldn't set the property
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI exception.
         */
        public void contributorChanged(InputProfileProperty input)
                throws VisADException, RemoteException,
                       PropertyVetoException {
            recomputationNeeded = contributors.isContributor(input);
            recompute();
        }

        /**
         * Recompute the output profile
         *
         * @throws PropertyVetoException   couldn't set the property
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI failure.
         */
        private void recompute()
                throws VisADException, RemoteException,
                       PropertyVetoException {
            if (recomputationNeeded && recomputationEnabled) {
                recomputationNeeded = false;
                InputProfileProperty[] inputProperties =
                    contributors.getContributors();
                if (inputProperties.length == 0) {
                    /*
                     * Set to default FlatField.
                     */
                    setFlatField(null);
                } else {
                    FlatField[] flatFields =
                        new FlatField[inputProperties.length];
                    for (int i = inputProperties.length; --i >= 0; ) {
                        flatFields[i] = inputProperties[i].getFlatField();
                    }
                    setFlatField(consolidate(functionType, flatFields,
                                             domainCoordinateSystem));
                }
            }
        }

        /**
         * Consolidates multiple FlatField-s into a single FlatField.
         *
         * @param outputFuncType
         *                      The type of function for the output FlatField.
         * @param fields        The FlatField-s to consolidate.
         * @param coordinateSystem
         *                      The CoordinateSystem which, together with
         *                      <code>outputFuncType</code>, form the basis for
         *                      the desired output domain.
         * @return              A FlatField that consolidates the given
         *                      range data from the input FlatField-s or
         *                      <code>null</code>.  If more than one input
         *                      FlatField has valid data for a point, then it is
         *                      unspecified from which input FlatField the point
         *                      is taken.  If <code>fields.length == 0</code>,
         *                      then <code>null</code> is returned.
         * @throws VisADException       Couldn't create necessary VisAD object.
         * @throws RemoteException      Java RMI failure.
         */
        private FlatField consolidate(FunctionType outputFuncType,
                                      FlatField[] fields,
                                      CoordinateSystem coordinateSystem)
                throws VisADException, RemoteException {
            FlatField     result;
            ArrayList     fieldList        = new ArrayList(fields.length);
            MathType      outputRangeType  = outputFuncType.getRange();
            RealTupleType outputDomainType = outputFuncType.getDomain();
            for (int i = 0; i < fields.length; ++i) {
                /*
                 * Explicitly handle fields with no data because the semantics
                 * of such fields are not propogated by the rest of VisAD---in
                 * particular by the DataUtility.consolidate(Field[]) method.
                 */
                if ( !fields[i].isMissing()) {
                    try {
                        /*
                         * Ensure the proper domain and range of the
                         * FlatField-s.
                         */
                        FlatField validFlatField =
                            (FlatField) ensureValidRange(
                                ensureDomain1D(
                                    (FlatField) DataUtility.ensureRange(
                                        fields[i],
                                        outputRangeType), outputDomainType,
                                            coordinateSystem));
                        if (validFlatField == null) {
                            LogUtil.consoleMessage(
                                "Warning: Field has no valid "
                                + outputRangeType + " data");
                        } else {
                            fieldList.add(validFlatField);
                        }
                    } catch (SetException e) {
                        System.err.println("Warning: Couldn't use "
                                           + fields[i].getType());
                    }
                }
            }
            int fieldCount = fieldList.size();
            result = (fieldCount == 0)
                     ? (FlatField) null
                     : (FlatField) DataUtility.consolidate(
                         (FlatField[]) fieldList.toArray(
                             new FlatField[fieldCount]));
            result = (FlatField) Util.clone(result,
                                            Util.clone(result.getDomainSet(),
                                                coordinateSystem));
            return result;
        }

        /**
         * Handles a change to the coordinate system transformation.
         *
         *
         * @param cs
         *
         * @throws PropertyVetoException
         * @throws RemoteException
         * @throws UnimplementedException       Can't clone domain set of field.
         * @throws VisADException
         */
        public void coordinateSystemChanged(CoordinateSystem cs)
                throws VisADException, RemoteException,
                       UnimplementedException, PropertyVetoException {
            if (adjustCoordinateSystem(cs, getFlatField(), this)) {
                domainCoordinateSystem = cs;
                notifyListeners();
            }
        }

    }

    /**
     * Ensures that a FlatField has a particular 1-D domain.  Clones the
     * FlatField only if necessary.
     *
     * @param field             The FlatField to have the particular domain.
     * @param newDomainType     The MathType of the particular domain for the
     *                          FlatField to have.  Coordinate conversions are
     *                          done if and when necessary and possible.
     * @param coordinateSystem  The CoordinateSystem which, together with
     *                          <code>newDomainType</code>, form the basis for
     *                          the desired domain.
     * @return                  The FlatField with the particular domain.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    protected static FlatField ensureDomain1D(FlatField field,
            RealTupleType newDomainType, CoordinateSystem coordinateSystem)
            throws VisADException, RemoteException {
        FlatField newField;
        if (DataUtility.getDomainType(field).equals(newDomainType)) {
            newField = coordinateSystem.equals(
                field.getDomainSet().getCoordinateSystem())
                       ? field
                       : (FlatField) Util.clone(field,
                       Util.clone(field.getDomainSet(), coordinateSystem));
        } else {
            CoordinateSystem rangeCoordinateSystem =
                Util.getRangeCoordinateSystem(field);
            CoordinateSystem[] rangeCoordinateSystems =
                Util.getRangeCoordinateSystems(field);
            Set[]      rangeSets  = field.getRangeSets();
            Unit[]     rangeUnits = Util.getRangeUnits(field);
            SampledSet oldDomain  = (SampledSet) field.getDomainSet();
            RealTupleType oldDomainType =
                ((SetType) oldDomain.getType()).getDomain();
            ErrorEstimate[] oldErrors = oldDomain.getSetErrors();
            ErrorEstimate[] newErrors = new ErrorEstimate[oldErrors.length];
            Unit[]          newUnits  = (coordinateSystem == null)
                                        ? newDomainType.getDefaultUnits()
                                        : coordinateSystem
                                            .getCoordinateSystemUnits();
            double[][] newDomainValues =
                CoordinateSystem.transformCoordinates(newDomainType,
                    coordinateSystem, newUnits, newErrors, oldDomainType,
                    oldDomain.getCoordinateSystem(), oldDomain.getSetUnits(),
                    oldErrors, oldDomain.getDoubles());
            SegmentSet goodSegments = new ValidSegmentSet(newDomainValues);
            newField =
                new FlatField(
                    new FunctionType(
                        newDomainType,
                        DataUtility.getRangeType(field)), Util.newSampledSet(
                            new int[] { goodSegments.getTotalCount() },
                            newDomainType,
                            goodSegments.take(newDomainValues),
                            coordinateSystem, newUnits,
                            newErrors), rangeCoordinateSystem,
                                        rangeCoordinateSystems,
                                        field.getRangeSets(), (Unit[]) null);  // default units
            if ( !field.isMissing()) {
                newField.setSamples(goodSegments.take(field.getValues(true)),
                                    false);
            }
        }
        return newField;
    }

    /**
     * Provides support for run-length encoded sets of valid-data segments.
     */
    protected static final class ValidSegmentSet extends SegmentSet {

        /**
         * Constructs.
         *
         * @param values        The values to be examined for segments of
         *                      valid-data.
         */
        public ValidSegmentSet(double[][] values) {
            if (values.length > 0) {
                int pointCount = values[0].length;
                for (int i = values.length; --i > 0; ) {
                    if (values[i].length != pointCount) {
                        throw new IllegalArgumentException(
                            "Subarrays have different lengths");
                    }
                }
                for (int pointIndex = -1; pointIndex < pointCount; ) {
                    /* Skip over invalid data */
                    while (++pointIndex < pointCount) {
                        int componentIndex;
                        for (componentIndex = 0;
                                componentIndex < values.length;
                                ++componentIndex) {
                            double value = values[componentIndex][pointIndex];
                            if ((value != value)
                                    || Double.isInfinite(value)) {
                                break;
                            }
                        }
                        if (componentIndex == values.length) {
                            break;
                        }
                    }
                    /* Found valid point or no more values */
                    if (pointIndex < pointCount) {
                        /* Found valid point */
                        int startIndex = pointIndex;
                        /* Find end of valid-data segment */
                        while (++pointIndex < pointCount) {
                            int componentIndex;
                            for (componentIndex = 0;
                                    componentIndex < values.length;
                                    ++componentIndex) {
                                double value =
                                    values[componentIndex][pointIndex];
                                if ((value != value)
                                        || Double.isInfinite(value)) {
                                    break;
                                }
                            }
                            if (componentIndex < values.length) {
                                break;
                            }
                        }
                        /* Found invalid point or no more values */
                        add(new Segment(startIndex, pointIndex - startIndex));
                    }
                }
            }
        }  // constructor
    }  // class ValidSegmentSet

    /**
     * Ensures that the range of a 1-D FlatField is maximally valid by removing
     * points with invalid data if necessary.  The FlatField will be cloned
     * only if necessary.
     *
     * @param flatField         The FlatField to be validated.  Its domain shall
     *                          be a Gridded1DSet.
     * @return                  The original FlatField if no modification was
     *                          necessary; otherwise, if the original FlatField
     *                          has some valid data, then a new FlatField with
     *                          points with NaN-s or infinities in the range
     *                          removed; otherwise, <code>null</code> if the
     *                          FlatField contains no valid range data.
     * @throws FieldException   The domain of the FlatField isn't 1-D or the
     *                          domain isn't a Gridded1DSet.
     * @throws SetException     A valid domain Set can't be created.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI exception.
     */
    protected static FlatField ensureValidRange(FlatField flatField)
            throws FieldException, SetException, VisADException,
                   RemoteException {
        if (flatField.getDomainDimension() != 1) {
            throw new FieldException("Domain not 1-D");
        }
        FlatField result;
        if (flatField.isMissing()) {
            /* The FlatField has no range data */
            result = flatField;  // best that can be done
        } else {
            /* The FlatField has range data */
            double[][] rangeValues  = flatField.getValues(false);
            SegmentSet goodSegments = new ValidSegmentSet(rangeValues);
            int        goodCount    = goodSegments.getTotalCount();
            if (goodCount == 0) {
                /* The FlatField has no valid range data */
                result = null;
            } else if (goodCount == flatField.getLength()) {
                /* The FlatField has no invalid range data */
                result = flatField;  // the entire range is valid
            } else {
                /* The FlatField has invalid range data */
                SampledSet oldDomainSet =
                    (SampledSet) flatField.getDomainSet();
                result = Util.clone(
                    flatField,
                    Util.newSampledSet(
                        oldDomainSet,
                        goodSegments.take(oldDomainSet.getSamples(false)),
                        new int[] { goodCount }),
                /*copyRange=*/
                false);
                if (goodCount > 0) {
                    result.setSamples(goodSegments.take(rangeValues), true);
                }
            }
        }
        return result;
    }

    /**
     * Class Contribution.  Contribution to a set of linked RAOB profiles
     */
    protected static class Contribution extends Link {

        /**
         * Create a new Contribution
         *
         * @param input   input profiler property
         * @param output  output profile property
         *
         */
        public Contribution(RAOB.InputProfileProperty input,
                            RAOB.OutputProfileProperty output) {
            super(new End(input), new End(output));
        }

        /**
         * Get the input profile property
         * @return  the input profile property
         */
        public RAOB.InputProfileProperty getInputProfileProperty() {
            return (RAOB
                .InputProfileProperty) ((End) getFirstEnd()).getProperty();
        }

        /**
         * Get the output profile property
         * @return  the output profile property
         */
        public RAOB.OutputProfileProperty getOutputProfileProperty() {
            return (RAOB
                .OutputProfileProperty) ((End) getSecondEnd()).getProperty();
        }

        /**
         * Class End
         */
        protected static class End extends Link.End {

            /** The property */
            private final Property property;

            /**
             * Create a new End with the property
             *
             * @param property   property to use
             */
            public End(Property property) {
                super(property.getName());
                this.property = property;
            }

            /**
             * Get the property for this End
             * @return  the property
             */
            public Property getProperty() {
                return property;
            }
        }
    }
}


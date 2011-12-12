/*
 * $Id: CapeCalculator.java,v 1.6 2005/05/13 18:33:25 jeffmc Exp $
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



import java.beans.*;

import java.rmi.RemoteException;

import ucar.visad.Util;
import ucar.visad.VisADMath;
import ucar.visad.display.*;
import ucar.visad.functiontypes.*;
import ucar.visad.quantities.*;

import visad.*;


/**
 * Provides support for calculating Convective Available Potential Energy (CAPE)
 * and Convective Inhibition (CIN) values.
 *
 * @author Steven R. Emmerson
 * @version $Id: CapeCalculator.java,v 1.6 2005/05/13 18:33:25 jeffmc Exp $
 */
public class CapeCalculator {

    /**
     * The name of the Convective Available Potential Energy (CAPE) property.
     */
    public String CAPE = "cape";

    /**
     * The name of the Convective Inhibition (CIN) property.
     */
    public String CIN = "cin";

    /**
     * The CAPE property change listeners.  The "volatile" is necessary for
     * correct behavior in multi-threaded environments.
     */
    private volatile PropertyChangeSupport listeners;

    /**
     * The CAPE property.
     */
    private Real cape;

    /**
     * The CIN property.
     */
    private Real cin;

    /**
     * The missing CAPE value.
     */
    private static Real missingCape;

    static {
        try {
            missingCape = new Real(ucar.visad.quantities.CAPE.getRealType());
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs from nothing.
     */
    public CapeCalculator() {
        cape = missingCape;
    }

    /**
     * Adds a PropertyChangeListener.  The listener will be added to both the
     * CAPE and CIN listener-lists.
     *
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        addPropertyChangeListener(CAPE, listener);
    }

    /**
     * Adds a PropertyChangeListener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {

        if (name.equals(CAPE)) {
            if (listeners == null) {
                synchronized (this) {
                    if (listeners == null) {
                        listeners = new PropertyChangeSupport(this);
                    }
                }
            }

            listeners.addPropertyChangeListener(listener);
        }
    }

    /**
     * Removes a PropertyChangeListener.  The listener will be removed from
     * both the CAPE and CIN listener-lists.
     *
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        removePropertyChangeListener(CAPE, listener);
    }

    /**
     * Removes a PropertyChangeListener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     *
     * @throws NullPointerException if the name is <code>null</code>.
     */
    public synchronized void removePropertyChangeListener(String name,
            PropertyChangeListener listener) {

        if (name.equals(CAPE)) {
            if (listeners != null) {
                listeners.removePropertyChangeListener(name, listener);
            }
        }
    }

    /**
     * Sets the CAPE property from profiles for temperature and dew-point and
     * the trajectory of the saturated portion of a lifted parcel.
     * @param temperatureProfile        The temperature profile.
     * @param dewPointProfile           The dew-point profile.
     * @param wetTrajectory             The trajectory of the saturated portion
     *                                  of the lifted parcel.
     * @throws TypeException    Somthing has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setCape(Field temperatureProfile, Field dewPointProfile, Field wetTrajectory)
            throws TypeException, VisADException, RemoteException {

        {
            FunctionType airTemperatureProfile =
                ucar.visad.functiontypes.AirTemperatureProfile.instance();

            Util.vetType(airTemperatureProfile, temperatureProfile);
            Util.vetType(airTemperatureProfile, dewPointProfile);
            Util.vetType(airTemperatureProfile, wetTrajectory);
        }

        Real    newCape = null;
        Real    newCin  = null;
        boolean problem = false;

        if (temperatureProfile.isMissing() || dewPointProfile.isMissing()
                || wetTrajectory.isMissing()) {
            problem = true;
        } else {

            /*
             * Resample the input profiles to the common domain formed from the
             * union of their domain sets.
             */
            {

                /*
                 * Form the common domain of the union of the input profile's
                 * domain sets.
                 */
                SampledSet commonDomain = null;

                {
                    Unit pressureUnit =
                        AirPressure.getRealType().getDefaultUnit();
                    SampledSet temperatureDomain =
                        (SampledSet) temperatureProfile.getDomainSet();
                    SampledSet dewPointDomain =
                        (SampledSet) dewPointProfile.getDomainSet();
                    SampledSet wetDomain =
                        (SampledSet) wetTrajectory.getDomainSet();
                    int temperatureCount = temperatureDomain.getLength();
                    int dewPointCount    = dewPointDomain.getLength();
                    int wetCount         = wetDomain.getLength();
                    float[] pressures =
                        new float[temperatureCount + dewPointCount + wetCount];

                    {
                        int i = 0;

                        System.arraycopy(
                            pressureUnit.toThis(
                                temperatureDomain
                                    .getSamples()[0], pressureUnit), 0,
                                        pressures, i, temperatureCount);

                        i += temperatureCount;

                        System.arraycopy(
                            pressureUnit.toThis(
                                dewPointDomain
                                    .getSamples()[0], pressureUnit), 0,
                                        pressures, i, dewPointCount);

                        i += dewPointCount;

                        System.arraycopy(
                            pressureUnit.toThis(
                                wetDomain.getSamples()[0], pressureUnit), 0,
                                    pressures, i, wetCount);
                    }

                    for (int i = 0; i < pressures.length; ++i) {
                        if (pressures[i] != pressures[i]) {
                            problem = true;

                            break;
                        }
                    }

                    if ( !problem) {
                        int[] indexes = Util.strictlySortedIndexes(pressures,
                                                                   true);
                        float[] sortedPressures = new float[indexes.length];

                        for (int i = 0; i < indexes.length; ++i) {
                            sortedPressures[i] = pressures[indexes[i]];
                        }

                        commonDomain = (sortedPressures.length == 1)
                                       ? (SampledSet) new SingletonSet(AirPressure.getRealTupleType(),
                                       new double[]{ sortedPressures[0] },
                                       (CoordinateSystem) null,
                                       (Unit[]) null, (ErrorEstimate[]) null)
                                       : new Gridded1DSet(
                                           AirPressure.getRealTupleType(),
                                           new float[][] {
                            sortedPressures
                        }, sortedPressures.length);
                    }
                }

                /*
                 * Resample the input profiles to the common domain.
                 */
                if ( !problem) {
                    temperatureProfile =
                        temperatureProfile.resample(commonDomain,
                                                    Data.WEIGHTED_AVERAGE,
                                                    Data.NO_ERRORS);
                    dewPointProfile =
                        dewPointProfile.resample(commonDomain,
                                                 Data.WEIGHTED_AVERAGE,
                                                 Data.NO_ERRORS);
                    wetTrajectory =
                        wetTrajectory.resample(commonDomain,
                                               Data.WEIGHTED_AVERAGE,
                                               Data.NO_ERRORS);
                }
            }

            /*
             * Set a new value for the CAPE property from the input profiles.
             */
            if ( !problem) {
                {

                    /*
                     * Form the buoyancy profile.
                     */
                    Field buoyancyProfile;

                    {
                        Field envVirtTemp =
                            (Field) VirtualTemperature.createFromDewPoint(
                                temperatureProfile.getDomainSet(),
                                temperatureProfile, dewPointProfile);
                        Field envVirtPotTemp =
                            (Field) VirtualPotentialTemperature
                                .createFromDewPoint(
                                    temperatureProfile.getDomainSet(),
                                    temperatureProfile, dewPointProfile);

                        buoyancyProfile = (Field) VisADMath
                            .divide(VisADMath
                                .subtract(SaturationVirtualPotentialTemperature
                                    .create(wetTrajectory
                                        .getDomainSet(), wetTrajectory), envVirtPotTemp), VisADMath
                                            .multiply(AirDensity
                                                .create(envVirtTemp
                                                    .getDomainSet(), envVirtTemp), envVirtPotTemp));
                    }

                    /*
                     * Replace missing and negative buoyancies with zero
                     * buoyancy.
                     */
                    {
                        float[][] rangeValues =
                            buoyancyProfile.getFloats(true);
                        float[] buoyancies = rangeValues[0];

                        for (int i = 0; i < buoyancies.length; ++i) {
                            if ((buoyancies[i] != buoyancies[i])
                                    || (buoyancies[i] < 0)) {
                                buoyancies[i] = 0;
                            }
                        }

                        rangeValues = Unit.convertTuple(
                            rangeValues,
                            buoyancyProfile.getDefaultRangeUnits(),
                            Util.getRangeUnits((FlatField) buoyancyProfile));

                        buoyancyProfile.setSamples(rangeValues);
                    }

                    /*
                     * Integrate the buoyancy profile to compute the new CAPE.
                     */
                    {
                        FlatField capeProfile =
                            VisADMath.curveIntegralOfGradient(
                                (FlatField) buoyancyProfile);

                        newCape =
                            (Real) Util
                                .clone((Real) capeProfile
                                    .getSample(capeProfile
                                        .getLength() - 1), ucar.visad
                                            .quantities.CAPE.getRealType());
                    }
                }
            }
        }

        setCape(problem
                ? missingCape
                : newCape);
    }

    /**
     * Sets the CAPE property.
     * @param newCape           The new value.
     */
    protected synchronized void setCape(Real newCape) {

        Real oldCAPE = cape;

        cape = newCape;

        if (listeners != null) {
            listeners.firePropertyChange(CAPE, oldCAPE, cape);
        }
    }

    /**
     * Returns the value of the CAPE property.
     * @return                  The value of the CAPE property.
     */
    public synchronized Real getCAPE() {
        return cape;
    }
}








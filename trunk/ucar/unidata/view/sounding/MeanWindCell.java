/*
 * $Id: MeanWindCell.java,v 1.14 2005/05/13 18:33:33 jeffmc Exp $
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
import ucar.visad.quantities.*;
import ucar.visad.functiontypes.CartesianHorizontalWindOfGeopotentialAltitude;

import visad.*;


/**
 * Provides support for the computation of the density-weighted, mean
 * horizontal wind.
 *
 * @author Steven R. Emmerson
 * @version $Id: MeanWindCell.java,v 1.14 2005/05/13 18:33:33 jeffmc Exp $
 */
public class MeanWindCell extends ActionImpl {

    /**
     * The name of the mean wind property.
     */
    public static final String MEAN_WIND = "meanWind";

    /** mean wind type */
    private static TupleType meanWindMathType;

    /** missing mean wind */
    private static Tuple missingMeanWind;

    /** wind profile type */
    private static FunctionType windProfileType;

    /** density profile type */
    private static FunctionType densityProfileType;

    /** wind profile reference */
    private final DataReference windProfileRef;

    /** density profile reference */
    private final DataReference densityProfileRef;

    /** mean wind reference */
    private final DataReference meanWindRef;

    /** change listeners */
    private volatile PropertyChangeSupport changeListeners;

    static {
        try {
            meanWindMathType = new TupleType(new MathType[]{
                GeopotentialAltitude.getRealTupleType(),
                CartesianHorizontalWind.getRealTupleType() });
            missingMeanWind = new Tuple(meanWindMathType);
            windProfileType =
                CartesianHorizontalWindOfGeopotentialAltitude.instance();
            densityProfileType =
                new FunctionType(AirPressure.getRealTupleType(),
                                 AirDensity.getRealType());
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Returns the type of the mean wind.
     *
     * @return            The type of the mean wind.
     */
    public static TupleType getType() {
        return meanWindMathType;
    }

    /**
     * Returns the missing mean-wind value.
     *
     * @return            The missing mean-wind value.
     */
    public static Tuple getMissing() {
        return missingMeanWind;
    }

    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure;
     * @throws RemoteException  Java RMI failure.
     */
    public MeanWindCell() throws VisADException, RemoteException {
        this(new FlatField(windProfileType),
             new FlatField(densityProfileType));
    }

    /**
     * Constructs from a wind profile and a density profile.
     * @param windProfile       The wind profile.
     * @param densityProfile    The density profile.
     * @throws VisADException   VisAD failure;
     * @throws RemoteException  Java RMI failure.
     */
    public MeanWindCell(Field windProfile, Field densityProfile)
            throws VisADException, RemoteException {
        this(newWindProfileRef(windProfile),
             newDensityProfileRef(densityProfile), newMeanWindRef());
    }

    /**
     * Constructs from DataReference-s to a wind profile, a density profile,
     * and a mean wind.
     * @param windProfileRef    The reference to the wind profile.
     * @param densityProfileRef The reference to the density profile.
     * @param meanWindRef       The reference to the mean wind.
     * @throws VisADException   VisAD failure;
     * @throws RemoteException  Java RMI failure.
     */
    public MeanWindCell(DataReference windProfileRef, DataReference densityProfileRef, DataReference meanWindRef)
            throws VisADException, RemoteException {

        super("MeanWindCell");

        this.windProfileRef    = windProfileRef;
        this.densityProfileRef = densityProfileRef;
        this.meanWindRef       = meanWindRef;

        addReference(windProfileRef);
        addReference(densityProfileRef);
        doAction();
    }

    /**
     * Compute a new mean-wind value.  This method will be invoked whenever
     * the wind profile or density profile changes.  This method will put the
     * new value into the DataReference for the mean wind and will fire a
     * PropertyChangeEvent for MEAN_WIND if the new value differs from the old.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void doAction() throws VisADException, RemoteException {

        Tuple oldMeanWind    = (Tuple) meanWindRef.getData();
        Field densityProfile = (Field) densityProfileRef.getData();
        Field windProfile    = (Field) windProfileRef.getData();
        Tuple meanWind;

        if (densityProfile.isMissing() || windProfile.isMissing()) {
            meanWind = missingMeanWind;
        } else {
            SampledSet altitudeSet = (SampledSet) windProfile.getDomainSet();
            FlatField densityField =
                (FlatField) densityProfile.resample(altitudeSet,
                                                    Data.WEIGHTED_AVERAGE,
                                                    Data.NO_ERRORS);
            int lastIndex = densityField.getLength() - 1;
            Real densityIntegral = (Real) VisADMath.curveIntegralOfGradient(
                                       densityField).getSample(lastIndex);
            RealTuple meanAltitude =
                (RealTuple) Util.clone(
                    VisADMath.divide(
                        VisADMath.curveIntegralOfGradient(
                            (FlatField) VisADMath.multiply(
                                VisADMath.newFlatField(altitudeSet).extract(
                                    0), densityField)).getSample(
                                        lastIndex), densityIntegral), meanWindMathType
                                            .getComponent(0));
            Real meanU =
                (Real) Util.clone(
                    VisADMath.divide(
                        VisADMath.curveIntegralOfGradient(
                            (FlatField) VisADMath.multiply(
                                windProfile.extract(0), densityField))
                                    .getSample(
                                        lastIndex), densityIntegral), WesterlyWind
                                            .getRealType());
            Real meanV =
                (Real) Util.clone(
                    VisADMath.divide(
                        VisADMath.curveIntegralOfGradient(
                            (FlatField) VisADMath.multiply(
                                windProfile.extract(1), densityField))
                                    .getSample(
                                        lastIndex), densityIntegral), SoutherlyWind
                                            .getRealType());

            meanWind = new Tuple(meanWindMathType, new Data[]{ meanAltitude,
                                                               new RealTuple(
                                                               (RealTupleType) meanWindMathType
                                                                   .getComponent(
                                                                       1), new Real[]{ meanU,
                                                                                       meanV }, (CoordinateSystem) null) });
        }

        meanWindRef.setData(meanWind);

        if (changeListeners != null) {
            changeListeners.firePropertyChange(MEAN_WIND, oldMeanWind,
                                               meanWind);
        }
    }

    /**
     * Adds a PropertyChangeListener.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        addPropertyChangeListener(MEAN_WIND, listener);
    }

    /**
     * Adds a PropertyChangeListener for a named property.
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {

        if (name.equals(MEAN_WIND)) {
            if (changeListeners == null) {
                synchronized (this) {
                    if (changeListeners == null) {
                        changeListeners = new PropertyChangeSupport(this);
                    }
                }
            }

            changeListeners.addPropertyChangeListener(listener);
        }
    }

    /**
     * Removes a PropertyChangeListener.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        removePropertyChangeListener(MEAN_WIND, listener);
    }

    /**
     * Removes a PropertyChangeListener for a named property.
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            String name, PropertyChangeListener listener) {

        if (changeListeners != null) {
            changeListeners.removePropertyChangeListener(name, listener);
        }
    }

    /**
     * Sets the wind profile.
     * @param windProfile       The horizontal wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWindProfile(Field windProfile)
            throws VisADException, RemoteException {
        Util.vetType(windProfileType, windProfile);
        windProfileRef.setData(windProfile);
    }

    /**
     * Sets the air-density profile.
     * @param densityProfile    The air-density profile.
     * @throws TypeException    Argument has wrong VisAD MathType.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDensityProfile(Field densityProfile)
            throws TypeException, VisADException, RemoteException {
        Util.vetType(densityProfileType, densityProfile);
        densityProfileRef.setData(densityProfile);
    }

    /**
     * Returns the mean wind.
     * @return                  The mean, horizontal wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Tuple getWind() throws VisADException, RemoteException {
        return (Tuple) meanWindRef.getData();
    }

    /**
     * Returns the data reference for the mean wind.
     * @return                  The data reference for the mean wind.
     */
    public DataReference getMeanWindRef() {
        return meanWindRef;
    }

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof MeanWindCell)) {
            equals = false;
        } else {
            MeanWindCell that = (MeanWindCell) obj;

            equals = (this == that)
                     || (meanWindRef.equals(that.meanWindRef)
                         && windProfileRef.equals(that.windProfileRef)
                         && densityProfileRef.equals(that.densityProfileRef)
                         && changeListeners.equals(that.changeListeners));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return meanWindRef.hashCode() ^ windProfileRef.hashCode()
               ^ densityProfileRef.hashCode() ^ changeListeners.hashCode();
    }

    /**
     * Create a new wind profile reference with the given profile
     *
     * @param windProfile   wind profile
     * @return new data reference
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private static DataReference newWindProfileRef(Data windProfile)
            throws VisADException, RemoteException {

        DataReference windProfileRef =
            new DataReferenceImpl("MeanWindCellWindRef");

        windProfileRef.setData(windProfile);

        return windProfileRef;
    }

    /**
     * Create a new density profile data reference with the given profile
     *
     * @param densityProfile   data for referenence
     * @return populated reference
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private static DataReference newDensityProfileRef(Data densityProfile)
            throws VisADException, RemoteException {

        DataReference densityProfileRef =
            new DataReferenceImpl("MeanWindCellDensityProfileRef");

        densityProfileRef.setData(densityProfile);

        return densityProfileRef;
    }

    /**
     * Create a new data reference for the mean wind
     * @return data reference
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private static DataReference newMeanWindRef()
            throws VisADException, RemoteException {

        DataReference meanWindRef =
            new DataReferenceImpl("MeanWindCellMeanWindRef");

        meanWindRef.setData(missingMeanWind);

        return meanWindRef;
    }
}








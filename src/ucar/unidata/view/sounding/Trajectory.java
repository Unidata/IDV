/*
 * $Id: Trajectory.java,v 1.20 2005/05/13 18:33:39 jeffmc Exp $
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



import java.rmi.RemoteException;

import java.beans.*;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;


/**
 * Provides support for displaying a parcel's trajectory.
 *
 * @author Steven R. Emmerson
 * @version $Id: Trajectory.java,v 1.20 2005/05/13 18:33:39 jeffmc Exp $
 */
public abstract class Trajectory extends LineDrawing {

    /**
     * The name of the trajectory property.
     */
    public static String TRAJECTORY = "trajectory";

    /** general pressure type */
    private static RealType generalPressureType;

    /** general temperature type */
    private static RealType generalTemperatureType;

    /** specific pressure type */
    private static RealType specificPressureType;

    /** specific temperature type */
    private static RealType specificTemperatureType;

    /** pressure unit */
    private static Unit pressureUnit;

    /** temperature unit */
    private static Unit temperatureUnit;

    /** log of max pressure ratio */
    private static double logMaxPresRatio;

    /** pressure ratio type */
    private static RealType pressureRatioType;

    /** trajectory type */
    private static FunctionType trajectoryType;

    /** missing trajectory */
    private FlatField missingTrajectory;

    /** factory */
    private TemperatureCalculatorFactory factory;

    /** flag for descending */
    private boolean descending;

    static {
        try {
            generalPressureType     = Pressure.getRealType();
            generalTemperatureType  = Temperature.getRealType();
            specificPressureType    = AirPressure.getRealType();
            specificTemperatureType = AirTemperature.getRealType();
            pressureUnit            = specificPressureType.getDefaultUnit();
            temperatureUnit = specificTemperatureType.getDefaultUnit();
            logMaxPresRatio         = Math.log(0.95);
            pressureRatioType =
                RealType.getRealType("TrajectoryPressureRatio",
                                     CommonUnit.dimensionless, null);
            trajectoryType = new FunctionType(AirPressure.getRealTupleType(),
                                              specificTemperatureType);

            /*            missingTrajectory       =
                new FlatField(trajectoryType,
                              new SingletonSet(new RealTuple(AirPressure
                              .getRealTupleType())));**/
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs from a name and a factory for creating a temperature
     * calculator.  The trajectory is assumed to be an ascending one.
     * @param name              The name for the displayable.
     * @param factory           The factory for creating a temperature
     *                          calculator.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Trajectory(String name, TemperatureCalculatorFactory factory)
            throws RemoteException, VisADException {
        this(name, factory, false);
    }

    /**
     * Constructs from a name, a factory for creating a temperature calculator,
     * and whether or not the trajectory is a descending one.
     * @param name              The name for the displayable.
     * @param factory           The factory for creating a temperature
     *                          calculator.
     * @param descending        Whether or not the trajectory is a descending
     *                          one (i.e towards higher pressures).
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Trajectory(String name, TemperatureCalculatorFactory factory, boolean descending)
            throws RemoteException, VisADException {

        super(name);

        if (missingTrajectory == null) {
            missingTrajectory = new FlatField(
                trajectoryType,
                new SingletonSet(
                    new RealTuple(AirPressure.getRealTupleType())));
        }

        setData(missingTrajectory);

        this.factory    = factory;
        this.descending = descending;
    }

    /**
     * Constructs from another instance.
     *
     * @param that              The other instance.
     * @throws VisADException   if a core VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     */
    protected Trajectory(Trajectory that)
            throws RemoteException, VisADException {

        super(that);

        factory    = that.factory;  // immutable
        descending = that.descending;
    }

    /**
     * Sets the parcel's trajectory from the starting pressre and temperature,
     * and the ending pressure.
     * @param startPressure     The starting pressure.
     * @param startTemperature  The starting temperature.
     * @param endPressure       The ending pressure.
     * @throws TypeException    An argument has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setTrajectory(Real startPressure, Real startTemperature, Real endPressure)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(generalPressureType, startPressure);
        Util.vetType(generalTemperatureType, startTemperature);
        Util.vetType(generalPressureType, endPressure);

        double endPres   = endPressure.getValue(pressureUnit);
        double startPres = startPressure.getValue(pressureUnit);

        if (( !descending && (endPres > startPres))
                || (descending && (endPres < startPres))) {
            setTrajectory(missingTrajectory);
        } else {
            double logPresExtent = Math.log(endPres / startPres);
            double logPresRatio = logPresExtent
                                  / Math.ceil(logPresExtent / (descending
                                                               ? -logMaxPresRatio
                                                               : logMaxPresRatio));
            int count = 1 + (int) Math.round(logPresExtent / logPresRatio);
            Real pressureRatio = new Real(pressureRatioType,
                                          Math.exp(logPresRatio));
            float[] pressures    = new float[count];
            float[] temperatures = new float[count];
            Real    pressure     = startPressure;
            Real    temperature  = startTemperature;
            TemperatureCalculator calculator =
                factory.newTemperatureCalculator(startPressure,
                                                 startTemperature);

            pressures[0]    = (float) pressure.getValue(pressureUnit);
            temperatures[0] = (float) temperature.getValue(temperatureUnit);

            for (int i = 1; i < count; ++i) {
                pressure     = (Real) pressure.multiply(pressureRatio);
                temperature  = calculator.nextTemperature(pressure);
                pressures[i] = (float) pressure.getValue(pressureUnit);
                temperatures[i] =
                    (float) temperature.getValue(temperatureUnit);
            }

            FlatField trajectory = new FlatField(trajectoryType,
                                                 (pressures.length == 1)
                                                 ? (Set) new SingletonSet(AirPressure.getRealTupleType(),
                                                     new double[]{ pressures[0] },
                                                     (CoordinateSystem) null,
                                                     new Unit[]{ pressureUnit },
                                                     (ErrorEstimate[]) null)
                                                 : new Gridded1DSet(
                                                     AirPressure.getRealTupleType(),
                                                     new float[][] {
pressures
},pressures.length, (CoordinateSystem) null, new Unit[]{ pressureUnit }, (ErrorEstimate[]) null), (CoordinateSystem[]) null,
    (Set[]) null, new Unit[]{ temperatureUnit });

            trajectory.setSamples(new float[][] {
                temperatures
            });
            setTrajectory(trajectory);
        }
    }

    /**
     * Clears the trajectory.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void clear() throws VisADException, RemoteException {
        setTrajectory(missingTrajectory);
    }

    /**
     * Returns the trajectory.  NB: Does not return a copy.
     * @return                  The parcel's (p -> T) trajectory.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public FlatField getTrajectory() throws VisADException, RemoteException {
        return (FlatField) getData();
    }

    /**
     * Returns the type of the pressure quantity.
     * @return                  The type of the pressure quantity.
     */
    public RealType getPressureType() {
        return specificPressureType;
    }

    /**
     * Returns the type of the temperature quantity.
     * @return                  The type of the temperature quantity.
     */
    public RealType getTemperatureType() {
        return specificTemperatureType;
    }

    /**
     * Sets the parcel's trajectory.
     * @param trajectory        The parcel's (p -> T) trajectory.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setTrajectory(FlatField trajectory)
            throws VisADException, RemoteException {

        FlatField old = getTrajectory();

        setData(trajectory);
        firePropertyChange(TRAJECTORY, old, trajectory);
    }
}








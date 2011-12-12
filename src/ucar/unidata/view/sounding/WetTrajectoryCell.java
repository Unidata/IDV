/*
 * $Id: WetTrajectoryCell.java,v 1.5 2005/05/13 18:33:40 jeffmc Exp $
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

import ucar.visad.functiontypes.AirTemperatureProfile;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.AirTemperature;
import ucar.visad.Util;

import visad.CommonUnit;

import visad.CoordinateSystem;

import visad.Data;

import visad.DataReference;

import visad.ErrorEstimate;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.Gridded1DSet;

import visad.Real;

import visad.RealType;

import visad.Set;

import visad.SingletonSet;

import visad.TypeException;

import visad.Unit;

import visad.VisADException;


/**
 * Computes the trajectory of a parcel of saturated air lifted
 * pseudo-adiabatically from the LCL.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:40 $
 */
public class WetTrajectoryCell extends ComputeCell {

    /** factory */
    private TemperatureCalculatorFactory factory;

    /** log max pressure ratio */
    private static final double logMaxPresRatio;

    /** pressure ratio type */
    private static final RealType pressureRatioType;

    /** trajectory type */
    private static final FunctionType trajectoryType;

    /** no data */
    private static final Field noData;

    static {
        RealType     prt = null;
        FunctionType tt  = null;

        try {
            tt = AirTemperatureProfile.instance();
            prt = RealType.getRealType("TrajectoryPressureRatio",
                                       CommonUnit.dimensionless, null);
            noData = (Field) AirTemperatureProfile.instance().missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();

            throw new RuntimeException();
        }

        trajectoryType    = tt;
        logMaxPresRatio   = Math.log(0.95);
        pressureRatioType = prt;
    }

    /**
     * Constructs from data references.
     *
     * @param lclTempRef       The input LCL temperature reference.
     * @param lclPresRef       The input LCL pressure reference.
     * @param minPresRef       The input minimum-presure reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public WetTrajectoryCell(DataReference lclTempRef, DataReference lclPresRef, DataReference minPresRef)
            throws VisADException, RemoteException {

        super("WetTrajectoryCell", new DataReference[]{ lclTempRef,
                                                        lclPresRef,
                                                        minPresRef }, noData);

        factory = DefaultWetTemperatureCalculatorFactory.instance();

        enableAllInputRefs();
    }

    /**
     * Computes the output saturated, (AirPressure -> AirTemperature)
     * lifted-parcel trajectory from the pressure and temperature at the
     * Lifting Condensation Level (LCL) and the minimum pressure to which to
     * lift the parcel.
     *
     * @param datums                The input data corresponding to the data
     *                              references of construciton: <code>datums[0]
     *                              </code> is the LCL temperature, <code>
     *                              datums[1]</code> is the LCL pressure, and
     *                              <code>datums[2]</code> is the minimum
     *                              pressure.
     * @return                      The corresponding lifted parcel path.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Real  lclTemp = (Real) datums[0];
        Real  lclPres = (Real) datums[1];
        Real  minPres = (Real) datums[2];
        Field wetTraj = noData;

        if ((minPres != null) && (lclPres != null) && (lclTemp != null)) {
            Util.vetType(AirPressure.getRealType(), minPres);
            Util.vetType(AirPressure.getRealType(), lclPres);
            Util.vetType(AirTemperature.getRealType(), lclTemp);

            Unit   presUnit  = AirPressure.getRealType().getDefaultUnit();
            double endPres   = minPres.getValue(presUnit);
            double startPres = lclPres.getValue(presUnit);

            if (endPres < startPres) {
                double logPresExtent = Math.log(endPres / startPres);
                double logPresRatio = logPresExtent
                                      / Math.ceil(logPresExtent
                                                  / (logMaxPresRatio));
                int count = 1 + (int) Math.round(logPresExtent
                                                 / logPresRatio);
                Real pressureRatio = new Real(pressureRatioType,
                                              Math.exp(logPresRatio));
                float[] pressures    = new float[count];
                float[] temperatures = new float[count];
                Real    pressure     = lclPres;
                Real    temperature  = lclTemp;
                TemperatureCalculator calculator =
                    factory.newTemperatureCalculator(lclPres, lclTemp);

                pressures[0] = (float) pressure.getValue(presUnit);

                Unit tempUnit = AirTemperature.getRealType().getDefaultUnit();

                temperatures[0] = (float) temperature.getValue(tempUnit);

                for (int i = 1; i < count; ++i) {
                    pressure        = (Real) pressure.multiply(pressureRatio);
                    temperature     = calculator.nextTemperature(pressure);
                    pressures[i]    = (float) pressure.getValue(presUnit);
                    temperatures[i] = (float) temperature.getValue(tempUnit);
                }

                wetTraj = new FlatField(trajectoryType,
                                        (pressures.length == 1)
                                        ? (Set) new SingletonSet(
                                            AirPressure.getRealTupleType(),
                                            new double[]{ pressures[0] },
                                            (CoordinateSystem) null,
                                            new Unit[]{ presUnit },
                                            (ErrorEstimate[]) null)
                                        : new Gridded1DSet(
                                            AirPressure.getRealTupleType(),
                                            new float[][] {
    pressures
}, pressures.length, (CoordinateSystem) null, new Unit[]{ presUnit }, (ErrorEstimate[]) null), (CoordinateSystem[]) null,
                        (Set[]) null, new Unit[]{ tempUnit });

                wetTraj.setSamples(new float[][] {
                    temperatures
                });
            }
        }

        return wetTraj;
    }
}








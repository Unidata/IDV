/*
 * $Id: DryTrajectoryCell.java,v 1.6 2005/05/13 18:33:30 jeffmc Exp $
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
 * Computes the trajectory of a parcel of air lifted pseudo-adiabatically from
 * an initial level to the LCL.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:30 $
 */
public class DryTrajectoryCell extends ComputeCell {

    /** temperature calculator factory */
    private TemperatureCalculatorFactory factory;

    /** log of max pressure ratio */
    private static final double logMaxPresRatio;

    /** pressure ratio type */
    private static final RealType pressureRatioType;

    /** trajectory type */
    private static final FunctionType trajectoryType;

    /** not data */
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
     * @param initPresRef      The input initial pressure reference.
     * @param initTempRef      The input initial temperature reference.
     * @param lclPresRef       The input LCL pressure reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public DryTrajectoryCell(DataReference initPresRef, DataReference initTempRef, DataReference lclPresRef)
            throws VisADException, RemoteException {

        super("DryTrajectoryCell", new DataReference[]{ initPresRef,
                                                        initTempRef,
                                                        lclPresRef }, noData);

        factory = DryTemperatureCalculatorFactory.instance();

        enableAllInputRefs();
    }

    /**
     * Computes the output lifted-parcel trajectory from the parcel's initial
     * pressure and temperature, and the pressure at the Lifting Condensation
     * Level (LCL).
     *
     * @param datums                The input data in the same order as during
     *                              construction.  <code>datums[0]</code> is the
     *                              initial pressure; <code>datums[1]</code> is
     *                              the initial, in-situ temperature; and
     *                              <code>datums[2]</code> is the LCL pressure.
     * @return                      The corresponding parcel path as a
     *                              (AirPressure -> AirTemperature) {@link
     *                              visad.Field}.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Real  initPres = (Real) datums[0];
        Real  initTemp = (Real) datums[1];
        Real  lclPres  = (Real) datums[2];
        Field dryTraj  = noData;

        if ((initPres != null) && (lclPres != null) && (initTemp != null)) {
            Util.vetType(AirPressure.getRealType(), initPres);
            Util.vetType(AirPressure.getRealType(), lclPres);
            Util.vetType(AirTemperature.getRealType(), initTemp);

            Unit   presUnit  = AirPressure.getRealType().getDefaultUnit();
            double endPres   = lclPres.getValue(presUnit);
            double startPres = initPres.getValue(presUnit);

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
                Real    pressure     = initPres;
                Real    temperature  = initTemp;
                TemperatureCalculator calculator =
                    factory.newTemperatureCalculator(initPres, initTemp);

                pressures[0] = (float) pressure.getValue(presUnit);

                Unit tempUnit = AirTemperature.getRealType().getDefaultUnit();

                temperatures[0] = (float) temperature.getValue(tempUnit);

                for (int i = 1; i < count; ++i) {
                    pressure        = (Real) pressure.multiply(pressureRatio);
                    temperature     = calculator.nextTemperature(pressure);
                    pressures[i]    = (float) pressure.getValue(presUnit);
                    temperatures[i] = (float) temperature.getValue(tempUnit);
                }

                dryTraj = new FlatField(trajectoryType,
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

                dryTraj.setSamples(new float[][] {
                    temperatures
                });
            }
        }

        return dryTraj;
    }
}








/*
 * $Id: LayerMeanCell.java,v 1.7 2005/05/13 18:33:31 jeffmc Exp $
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

import ucar.visad.quantities.AirPressure;
import ucar.visad.VisADMath;

import visad.CommonUnit;

import visad.Field;

import visad.Data;

import visad.DataReference;

import visad.DataReferenceImpl;

import visad.ErrorEstimate;

import visad.FunctionType;

import visad.Gridded1DSet;

import visad.Gridded1DDoubleSet;

import visad.GriddedSet;

import visad.Real;

import visad.RealType;

import visad.Set;

import visad.SetType;

import visad.TypeException;

import visad.Unit;

import visad.VisADException;


/**
 * Computes the mean value of an atmospheric profile parameter over a layer.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:31 $
 */
public final class LayerMeanCell extends ComputeCell {

    /** no data */
    private final Real noData;

    /** reference for mean pressure */
    private DataReference meanPresRef;

    /** two */
    private static final Real two;

    static {
        try {
            two = new Real(
                RealType.getRealType(
                    "LayerMeanCell_CONSTANT", CommonUnit.dimensionless), 2);
        } catch (Exception ex) {
            System.err.println("Couldn't initialize class");
            ex.printStackTrace();

            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * Constructs from a reference to the atmospheric profile, references to
     * the lower and upper limits of the layer, and a missing-data value.
     *
     * @param proRef                 The atmospheric profile reference.
     * @param lowerRef               The lower layer limit.
     * @param upperRef               The upper layer limit.
     * @param noData                 The missing data value.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public LayerMeanCell(DataReference proRef, DataReference lowerRef, DataReference upperRef, Real noData)
            throws VisADException, RemoteException {

        super("LayerMeanCell", new DataReference[]{ proRef, lowerRef,
                                                    upperRef }, noData);

        this.noData = noData;
        meanPresRef = new DataReferenceImpl("LayerPressureRef");

        meanPresRef.setData(AirPressure.getRealType().missingData());
        enableAllInputRefs();
    }

    /**
     * Computes the layer mean value.
     *
     * @param datums                The input data.  <code>datums[0]</code> is
     *                              the profile; <code> datums[1] </code> is the
     *                              lower layer limit; and <code> datums[2]
     *                              </code> is the upper layer limit.
     * @return                      The mean value of the profile parameter.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Real  value   = noData;
        Field profile = (Field) datums[0];

        if (profile != null) {
            Real lower = (Real) datums[1];

            if (lower != null) {
                Real upper = (Real) datums[2];

                if (upper != null) {
                    Field integral =
                        VisADMath.curveIntegralOfGradient(interval(profile,
                            lower, upper));

                    value =
                        (Real) VisADMath
                            .divide(VisADMath
                                .subtract(integral
                                    .evaluate(upper, Data
                                        .NEAREST_NEIGHBOR, Data
                                        .NO_ERRORS), integral
                                            .evaluate(lower, Data
                                                .NEAREST_NEIGHBOR, Data
                                                .NO_ERRORS)), VisADMath
                                                    .subtract(upper, lower));

                    meanPresRef.setData(
                        VisADMath.divide(VisADMath.add(upper, lower), two));
                }
            }
        }

        return value;
    }

    /**
     * Create a field from another given the specified interval
     *
     * @param field   field  subset
     * @param lower   lower domain value
     * @param upper   upper domain value
     * @return resampled data
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private static Field interval(Field field, Real lower, Real upper)
            throws VisADException, RemoteException {

        Field newField = null;  // returned value
        Set   set      = field.getDomainSet();

        if (set instanceof Gridded1DSet) {
            Gridded1DSet domain   = (Gridded1DSet) set;
            Unit[]       units    = domain.getSetUnits();
            Unit         unit     = units[0];
            double       lowValue = lower.getValue(unit);
            double       upValue  = upper.getValue(unit);
            float[]      coords   = domain.valueToGrid(new float[][] {
                { (float) lowValue, (float) upValue }
            })[0];
            float        lowGrid  = coords[0];
            float        upGrid   = coords[1];

            if ((lowGrid == lowGrid) && (upGrid == upGrid)) {
                if (lowGrid > upGrid) {
                    float grid = lowGrid;

                    lowGrid = upGrid;
                    upGrid  = grid;

                    double value = lowValue;

                    lowValue = upValue;
                    upValue  = value;
                }

                int lowIndex = (int) Math.ceil(lowGrid);

                if (lowIndex == lowGrid) {
                    lowIndex++;
                }

                int upIndex = (int) Math.floor(upGrid);

                if (upIndex == upGrid) {
                    upIndex--;
                }

                // if (lowIndex <= upIndex) {
                Gridded1DSet newDomain;
                boolean      useDouble = domain instanceof Gridded1DDoubleSet;

                if (useDouble) {
                    double[] values =
                        new double[2 + (upIndex - lowIndex) + 1];

                    values[0]                 = lowValue;
                    values[values.length - 1] = upValue;

                    System.arraycopy(domain.getDoubles(false)[0], lowIndex,
                                     values, 1, values.length - 2);

                    newDomain =
                        new Gridded1DDoubleSet((SetType) domain.getType(),
                                               new double[][] {
                        values
                    }, values.length, domain.getCoordinateSystem(), units,
                       (ErrorEstimate[]) null, false);
                } else {
                    float[] values = new float[2 + (upIndex - lowIndex) + 1];

                    values[0]                 = (float) lowValue;
                    values[values.length - 1] = (float) upValue;

                    System.arraycopy(domain.getSamples(false)[0], lowIndex,
                                     values, 1, values.length - 2);

                    newDomain = new Gridded1DSet((SetType) domain.getType(),
                                                 new float[][] {
                        values
                    }, values.length, domain.getCoordinateSystem(), units,
                       (ErrorEstimate[]) null, false);
                }

                newField = field.resample(newDomain, Data.WEIGHTED_AVERAGE,
                                          Data.NO_ERRORS);

                // }
            }
        }

        if (newField == null) {
            newField = (Field) field.getType().missingData();
        }

        return newField;
    }

    /**
     * Returns the mean pressure within the just-computed layer.
     *
     * @return                       The mean pressure of the just-computed
     *                               layer.
     */
    public DataReference getMeanPresRef() {
        return meanPresRef;
    }
}








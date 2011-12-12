/*
 * $Id: EvaluateIntegralCell.java,v 1.6 2005/05/13 18:33:30 jeffmc Exp $
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

import ucar.visad.VisADMath;

import visad.Field;

import visad.Data;

import visad.DataReference;

import visad.Real;

import visad.TypeException;

import visad.VisADException;


/**
 * Computes a definite integral by evaluating an indefinite integral.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:30 $
 */
public final class EvaluateIntegralCell extends ComputeCell {

    /** no data */
    private final Real noData;

    /**
     * Constructs from references to the indefinite integral, lower limit,
     * upper limit, and a missing-data value.
     *
     * @param integralRef            The indefinite integral reference.
     * @param lowerRef               The lower limit reference.
     * @param upperRef               The upper limit reference.
     * @param noData                 The missing data value.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public EvaluateIntegralCell(DataReference integralRef, DataReference lowerRef, DataReference upperRef, Real noData)
            throws VisADException, RemoteException {

        super("EvaluateIntegralCell", new DataReference[]{ integralRef,
                                                           lowerRef,
                                                           upperRef }, noData);

        this.noData = noData;

        enableAllInputRefs();
    }

    /**
     * Computes the definite integral.
     *
     * @param datums                The input data.  <code>datums[0]</code> is
     *                              the indefinite integral; <code>datums[1]
     *                              </code> is the lower limit; and <code>
     *                              datums[2] </code> is the upper limit;
     * @return                      The definite integral value
     *                              (a {@link visad.Real}.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Real  value    = noData;
        Field integral = (Field) datums[0];

        if (integral != null) {
            Real lowerLimit = (Real) datums[1];

            if (lowerLimit != null) {
                Real upperLimit = (Real) datums[2];

                if (upperLimit != null) {
                    value =
                        (Real) VisADMath
                            .subtract(integral
                                .evaluate(upperLimit, Data
                                    .WEIGHTED_AVERAGE, Data
                                    .NO_ERRORS), integral
                                        .evaluate(lowerLimit, Data
                                            .WEIGHTED_AVERAGE, Data
                                            .NO_ERRORS));
                }
            }
        }

        return value;
    }
}








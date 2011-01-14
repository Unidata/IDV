/*
 * $Id: RealEvaluatorCell.java,v 1.6 2005/05/13 18:33:36 jeffmc Exp $
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

import ucar.visad.Util;

import visad.Data;

import visad.DataReference;

import visad.Function;

import visad.Real;

import visad.TypeException;

import visad.VisADException;


/**
 * Evaluates a function at a {@link visad.Real} point.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:36 $
 */
public final class RealEvaluatorCell extends ComputeCell {

    /** no data */
    private final Data noData;

    /**
     * Constructs from a data reference for the function, a data reference for
     * the {@link visad.Real} point, and a missing data value.
     *
     * @param funcRef          The function reference.
     * @param pointRef         The evaluation point reference.
     * @param noData           The missing data value.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public RealEvaluatorCell(DataReference funcRef, DataReference pointRef, Data noData)
            throws VisADException, RemoteException {

        super("RealEvaluatorCell", new DataReference[]{ funcRef, pointRef },
              noData);

        this.noData = (Data) noData.dataClone();

        enableAllInputRefs();
    }

    /**
     * Computes the output value by evaluating the function.
     *
     * @param datums                The input data. <code> datums[0] </code>
     *                              is the function to evaluate and <code>
     *                              datums[1] </code> is the {@link visad.Real}
     *                              point at which to evaluate the function.
     * @return                      The value of the function at the point.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Data     value = noData;
        Function func  = (Function) datums[0];

        if (func != null) {
            Real point = (Real) datums[1];

            if (point != null) {
                value = func.evaluate(point, Data.WEIGHTED_AVERAGE,
                                      Data.NO_ERRORS);
            }
        }

        return value;
    }
}








/*
 * $Id: IntegrateCell.java,v 1.6 2005/05/13 18:33:31 jeffmc Exp $
 *
 * Copyright  1997-2025 Unidata Program Center/University Corporation for
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

import visad.TypeException;

import visad.VisADException;


/**
 * Computes the indefinite integral of a FlatField.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:31 $
 */
public final class IntegrateCell extends ComputeCell {

    /** no data */
    private final Field noData;

    /**
     * Constructs from references to the {@link visad.FlatField} and a
     * missing-data value.
     *
     * @param fieldRef               The Field reference.
     * @param noData                 The missing data value.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public IntegrateCell(DataReference fieldRef, Field noData)
            throws VisADException, RemoteException {

        super("IntegrateCell", new DataReference[]{ fieldRef }, noData);

        this.noData = noData;

        enableAllInputRefs();
    }

    /**
     * Computes the indefinite integral of a {@link visad.Field}.
     *
     * @param datums                The input data.  <code>datums[0]</code> is
     *                              the {@link visad.Field} to integrate.
     * @return                      The corresponding indefinite integral.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field integral = noData;
        Field field    = (Field) datums[0];

        if (field != null) {
            integral = VisADMath.curveIntegralOfGradient(field);
        }

        return integral;
    }
}








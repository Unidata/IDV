/*
 * $Id: AddCell.java,v 1.5 2005/05/13 18:33:21 jeffmc Exp $
 *
 * Copyright  1997-2022 Unidata Program Center/University Corporation for
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

import visad.Data;

import visad.DataReference;

import visad.TypeException;

import visad.VisADException;


/**
 * Computes the sum of two data objects.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:21 $
 */
public final class AddCell extends ComputeCell {

    /** no data */
    private final Data noData;

    /**
     * Constructs from references to two data objects and a missing data value.
     *
     * @param ref1                   The first data reference.
     * @param ref2                   The second data reference.
     * @param noData                 The missing data value.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public AddCell(DataReference ref1, DataReference ref2, Data noData)
            throws VisADException, RemoteException {

        super("AddCell", new DataReference[]{ ref1, ref2 }, noData);

        this.noData = noData;

        enableAllInputRefs();
    }

    /**
     * Computes the sum.
     *
     * @param datums                The input data.  <code>datums[0]</code> is
     *                              the first data object and <code> datums[1]
     *                              </code> is the second.
     * @return                      The maximum pressure of the profile.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Data value = noData;
        Data data1 = (Data) datums[0];

        if (data1 != null) {
            Data data2 = (Data) datums[1];

            if (data2 != null) {
                value = VisADMath.add(data1, data2);
            }
        }

        return value;
    }
}








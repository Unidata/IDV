/*
 * $Id: SelectorCell.java,v 1.7 2005/05/13 18:33:37 jeffmc Exp $
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

import visad.Data;

import visad.DataReference;

import visad.Real;

import visad.RealType;

import visad.VisADException;


/**
 * Creates an output {@link visad.Data} object from an array of input
 * {@link visad.Data} objects.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:37 $
 */
public final class SelectorCell extends ComputeCell {

    /** no data */
    private final Data noData;

    /**
     * Constructs from a reference to a selector index, an array of
     * references to the individual data objects, and a missing output data
     * value.
     *
     * @param indexRef               The selector index reference.
     * @param refs                   The input data references.
     * @param noData                 The missing-data value.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     * @throws IllegalArgumentException if <code>refs.length == 0</code>.
     */
    public SelectorCell(DataReference indexRef, DataReference[] refs, Data noData)
            throws VisADException, RemoteException {

        super("SelectorCell", makeDataReferences(indexRef, refs), noData);

        if (refs.length == 0) {
            throw new IllegalArgumentException();
        }

        this.noData = noData;

        enableAllInputRefs();
    }

    /**
     * Make data references with indexReff as the first in the returned array
     *
     * @param indexRef  0 index reference
     * @param refs      other refs
     * @return array of refs with indexRef at the beginning
     */
    private static DataReference[] makeDataReferences(DataReference indexRef,
            DataReference[] refs) {

        DataReference[] newRefs = new DataReference[1 + refs.length];

        newRefs[0] = indexRef;

        System.arraycopy(refs, 0, newRefs, 1, refs.length);

        return newRefs;
    }

    /**
     * Computes (selects) the output {@link Data}.
     *
     * @param datums                The data objects. <code>datums[0]</code> is
     *                              the (origin zero) selector index and must
     *                              be a {@link Real}. <code>datums[1+i]</code>
     *                              is the data object corresponding to the
     *                              <code>i</code>th {@link DataReference} of
     *                              the array used during construction.
     * @return                      The input datum corresponding to the
     *                              selector index.
     * @throws ArrayIndexOutOfBoundsException
     *                              if <code>((Real)datums[0]).getValue()</code>
     *                              is less than zero or greater than
     *                              <code>datums.length-2</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws VisADException, RemoteException {

        Data output = noData;
        Real index  = (Real) datums[0];

        if (index != null) {
            output = datums[(int) (1 + index.getValue())];
        }

        return output;
    }
}








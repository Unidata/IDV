/*
 * $Id: TuplerCell.java,v 1.6 2005/05/13 18:33:39 jeffmc Exp $
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
import ucar.visad.VisADMath;

import visad.Data;

import visad.DataReference;

import visad.Real;

import visad.RealTuple;

import visad.RealTupleType;

import visad.RealType;

import visad.Tuple;

import visad.TupleType;

import visad.TypeException;

import visad.VisADException;


/**
 * Creates an output VisAD {@link visad.Tuple} individual, data objects.
 * The output {@link visad.Tuple} will be a {@link visad.RealTuple} when
 * appropriate.  If the input comprises a single data object, then that
 * object will be the output.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:39 $
 */
public final class TuplerCell extends ComputeCell {

    /** not data */
    private static final Tuple noData;

    static {
        try {
            noData =
                (Tuple) new RealTupleType(RealType.Generic,
                                          RealType.Generic).missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();

            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Constructs from an array of references to the individual data objects.
     *
     * @param refs                   The input data references.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public TuplerCell(DataReference[] refs)
            throws VisADException, RemoteException {

        super("TuplerCell", refs, noData);

        enableAllInputRefs();
    }

    /**
     * Computes the output Tuple.
     *
     * @param datums                The input data in the same order as during
     *                              construction.
     * @return                      The {@link visad.Tuple} corresponding to the
     *                              input data, in order.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Data output = noData;

        if (datums.length > 0) {
            if (datums.length == 1) {
                output = datums[0];
            } else {
                boolean allReal = true;

                for (int i = 0; i < datums.length; i++) {
                    if ( !(datums[i] instanceof Real)) {
                        allReal = false;

                        break;
                    }
                }

                if ( !allReal) {
                    output = new Tuple(datums);
                } else {
                    Real[] reals = new Real[datums.length];

                    System.arraycopy(datums, 0, reals, 0, reals.length);

                    output = new RealTuple(reals);
                }
            }
        }

        return output;
    }
}








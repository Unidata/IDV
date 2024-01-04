/*
 * $Id: DewPointExtractorCell.java,v 1.5 2005/05/13 18:33:28 jeffmc Exp $
 *
 * Copyright  1997-2024 Unidata Program Center/University Corporation for
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

import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.DewPoint;
import ucar.visad.Util;

import visad.Data;

import visad.DataReference;

import visad.Field;

import visad.TypeException;

import visad.Real;

import visad.VisADException;


/**
 * Extracts the dew-point temperature from a dew-point
 * temperature profile at a given pressure.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:28 $
 */
public final class DewPointExtractorCell extends ComputeCell {

    /** no data */
    private static final Real noData;

    static {
        try {
            noData = (Real) DewPoint.getRealType().missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();

            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Constructs from input data cells.
     *
     * @param dewProfileRef    The input dew-point profile reference.
     * @param presRef          The input pressure reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public DewPointExtractorCell(DataReference dewProfileRef, DataReference presRef)
            throws VisADException, RemoteException {

        super("DewPointExtractorCell", new DataReference[]{ dewProfileRef,
                                                            presRef }, noData);

        enableAllInputRefs();
    }

    /**
     * Computes the output dew-point.
     *
     * @param datums                The input data in the same order as during
     *                              construction. <code>datums[0]</code> is the
     *                              dew-point profile and <code>datums[1]</code>
     *                              is the pressure at which to interpolate the
     *                              dew-point profile.
     * @return                      The dew-point of the profile at the given
     *                              pressure.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field dew    = (Field) datums[0];
        Real  pres   = (Real) datums[1];
        Real  newDew = noData;

        if ((pres != null) && (dew != null)) {
            Util.vetType(AirPressure.getRealType(), pres);
            Util.vetType(DewPointProfile.instance(), dew);

            newDew = (Real) dew.evaluate(pres);
        }

        return newDew;
    }
}








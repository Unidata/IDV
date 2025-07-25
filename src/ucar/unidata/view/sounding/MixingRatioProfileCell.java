/*
 * $Id: MixingRatioProfileCell.java,v 1.6 2005/05/13 18:33:33 jeffmc Exp $
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

import ucar.visad.quantities.WaterVaporMixingRatio;

import visad.Data;

import visad.DataReference;

import visad.Field;

import visad.TypeException;

import visad.VisADException;


/**
 * Computes a profile of water-vapor mixing-ratio from a profile
 * of temperature.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:33 $
 */
public final class MixingRatioProfileCell extends ComputeCell {

    /** no data */
    private final Field noData;

    /**
     * Constructs from data references.
     *
     * @param tempProfileRef   The input temperature profile reference.
     * @param noData           The missing data value.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public MixingRatioProfileCell(DataReference tempProfileRef, Field noData)
            throws VisADException, RemoteException {

        super("MixingRatioProfileCell",
              new DataReference[]{ tempProfileRef }, noData);

        this.noData = noData;

        enableAllInputRefs();
    }

    /**
     * Computes the output profile of water-vapor mixing-ratio.  If the input
     * temperature profile is the in-situ temperature, then the returned
     * profile is the saturation mixing-ratio; if the input profile is the
     * dew-point temperature, then the returned profile is the actual water-
     * vapor mixing-ratio.
     *
     * @param datums                The input data.  <code>datums[0] </code> is
     *                              the input temperature profile.
     * @return                      The corresponding water-vapor mixing-ratio
     *                              profile.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field output  = noData;
        Field tempPro = (Field) datums[0];

        if (tempPro != null) {
            output =
                (Field) WaterVaporMixingRatio.create(tempPro.getDomainSet(),
                                                     tempPro);
        }

        return output;
    }
}








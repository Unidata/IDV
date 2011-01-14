/*
 * $Id: MixingRatioCell.java,v 1.5 2005/05/13 18:33:33 jeffmc Exp $
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
import ucar.visad.quantities.DewPoint;
import ucar.visad.quantities.WaterVaporMixingRatio;
import ucar.visad.Util;

import visad.Data;

import visad.DataReference;

import visad.Real;

import visad.TypeException;

import visad.VisADException;


/**
 * Computes mixing ratio from a pressure and the
 * dew-point at that pressure.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:33 $
 */
public final class MixingRatioCell extends ComputeCell {

    /** no data */
    private static final Real noData;

    static {
        try {
            noData = (Real) WaterVaporMixingRatio.getRealType().missingData();
        } catch (Exception ex) {
            System.err.println("Couldn't initialize class");
            ex.printStackTrace();

            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Constructs from data references.
     *
     * @param dewRef           The input dew-point reference.
     * @param presRef          The input pressure reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public MixingRatioCell(DataReference dewRef, DataReference presRef)
            throws VisADException, RemoteException {

        super("MixingRatioCell", new DataReference[]{ dewRef, presRef },
              noData);

        enableAllInputRefs();
    }

    /**
     * Computes the output water-vapor mixing-ratio from pressure and dew-point
     * temperature.
     *
     * @param datums                The input data in the same order as during
     *                              construction: <code>datums[0]</code> is the
     *                              dew-point temperature and <code>datums[1]
     *                              </code> is the pressure.
     * @return                      The corresponding water-vapor mixing-ratio.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Real dew   = (Real) datums[0];
        Real pres  = (Real) datums[1];
        Real ratio = noData;

        if ((pres != null) && (dew != null)) {
            Util.vetType(AirPressure.getRealType(), pres);
            Util.vetType(DewPoint.getRealType(), dew);

            ratio = (Real) WaterVaporMixingRatio.create(pres, dew);
        }

        return ratio;
    }
}








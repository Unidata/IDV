/*
 * $Id: LclPressureCell.java,v 1.5 2005/05/13 18:33:32 jeffmc Exp $
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
import ucar.visad.quantities.AirTemperature;
import ucar.visad.quantities.SaturationPointTemperature;
import ucar.visad.quantities.SaturationPointPressure;
import ucar.visad.quantities.WaterVaporMixingRatio;
import ucar.visad.Util;

import visad.Data;

import visad.DataReference;

import visad.Real;

import visad.TypeException;

import visad.VisADException;


/**
 * Computes the condensation pressure at the lifting condensation level (LCL).
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:32 $
 */
public final class LclPressureCell extends ComputeCell {

    /** no data */
    private static final Data noData;

    static {
        try {
            noData = AirPressure.getRealType().missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();

            throw new RuntimeException();
        }
    }

    /**
     * Constructs from data references.
     *
     * @param initPresRef      The input initial pressure reference.
     * @param initTempRef      The input initial temperature reference.
     * @param lclTempRef       The input LCL temperature reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public LclPressureCell(DataReference initPresRef, DataReference initTempRef, DataReference lclTempRef)
            throws VisADException, RemoteException {

        super("LclPressureCell", new DataReference[]{ initPresRef,
                                                      initTempRef,
                                                      lclTempRef }, noData);

        enableAllInputRefs();
    }

    /**
     * Computes the output condensation pressure (the pressure at the LCL) from
     * the parcel's initial pressure and temperature, and the temperature at the
     * Lifting Condensation Level (LCL).
     *
     * @param datums                The input data in the same order as during
     *                              construction.  <code>datums[0]</code> is the
     *                              initial pressure; <code>datums[1]</code> is
     *                              the initial temperature; and <code>datums[2]
     *                              </code> is the temperature at the LCL.
     * @return                      The corresponding condensation pressure.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Real initPres = (Real) datums[0];
        Real initTemp = (Real) datums[1];
        Real lclTemp  = (Real) datums[2];
        Data condPres = noData;

        if ((initPres != null) && (initTemp != null) && (lclTemp != null)) {
            Util.vetType(AirPressure.getRealType(), initPres);
            Util.vetType(AirTemperature.getRealType(), initTemp);
            Util.vetType(AirTemperature.getRealType(), lclTemp);

            condPres = (Real) SaturationPointPressure.create(initPres,
                    initTemp, lclTemp);
        }

        return condPres;
    }
}








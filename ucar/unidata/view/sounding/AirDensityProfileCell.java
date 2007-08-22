/*
 * $Id: AirDensityProfileCell.java,v 1.7 2005/05/13 18:33:23 jeffmc Exp $
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

import ucar.visad.functiontypes.AtmosphericProfile;
import ucar.visad.quantities.AirDensity;
import ucar.visad.quantities.VirtualTemperature;
import ucar.visad.Util;

import visad.Data;

import visad.DataReference;

import visad.Field;

import visad.FunctionType;

import visad.TypeException;

import visad.VisADException;


/**
 * Computes a profile of air density from a profile of virtual temperature.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:23 $
 */
public final class AirDensityProfileCell extends ComputeCell {

    /** no data */
    private static final Data noData;

    static {
        try {
            noData = new AtmosphericProfile(
                AirDensity.getRealType()).missingData();
        } catch (Exception ex) {
            System.err.println("Couldn't initialize class");
            ex.printStackTrace();

            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Constructs from input and output data references.
     *
     * @param virtProRef       The input virtual temperature profile reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public AirDensityProfileCell(DataReference virtProRef)
            throws VisADException, RemoteException {

        super("AirDensityProfileCell", new DataReference[]{ virtProRef },
              noData);

        enableAllInputRefs();
    }

    /**
     * Computes the output profile of air density.
     *
     * @param datums                The input data.  <code>datums[0]</code> is
     *                              the virtual temperature profile.
     * @return                      The corresponding profile of air density.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field virtPro = (Field) datums[0];
        Data  denPro  = noData;

        if ((virtPro != null) && !virtPro.isMissing()) {
            Util.vetType(VirtualTemperature.getRealType(), virtPro);

            /*
            System.out.println("virtPro.getType().getRange()=" +
                ((FunctionType)virtPro.getType()).getRange());
            System.out.println("virtPro.getType().getRangeUnits()[0][0]=" +
                virtPro.getRangeUnits()[0][0]);
            System.out.println("virtPro=" + virtPro.longString());
             */
            denPro = AirDensity.create(virtPro.getDomainSet(), virtPro);
        }

        return denPro;
    }
}








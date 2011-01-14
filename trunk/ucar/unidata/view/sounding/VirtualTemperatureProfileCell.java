/*
 * $Id: VirtualTemperatureProfileCell.java,v 1.6 2005/05/13 18:33:40 jeffmc Exp $
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
import ucar.visad.quantities.DewPoint;
import ucar.visad.quantities.VirtualTemperature;
import ucar.visad.quantities.WaterVaporMixingRatio;
import ucar.visad.Util;

import visad.Data;

import visad.DataReference;

import visad.Field;

import visad.TypeException;

import visad.VisADException;


/**
 * Computes a profile of virtual temperature from moisture data and a profile
 * of temperature.  The domain of the output profile will be the domain of
 * the temperature profile.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:40 $
 */
public final class VirtualTemperatureProfileCell extends ComputeCell {

    /** no data */
    private static final Field noData;

    static {
        try {
            noData = (Field) new AtmosphericProfile(
                VirtualTemperature.getRealType()).missingData();
        } catch (Exception ex) {
            System.err.println("Couldn't initialize class");
            ex.printStackTrace();

            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Constructs from data references.  The temperature argument should
     * refer to a {@link visad.Field} and the moisture argument should refer to
     * either a {@link visad.Field} or a {@link visad.Real}.  The
     * {@link visad.Field}'s rangetype or the {@link visad.Real}'s
     * {@link visad.RealType} should be compatible with either
     * {@link ucar.visad.quantities.DewPoint#getRealType()} or {@link
     * ucar.visad.quantities.WaterVaporMixingRatio#getRealType()}.
     *
     * @param tempProfileRef   The input temperature profile reference.
     * @param moistRef         The input moisture reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public VirtualTemperatureProfileCell(DataReference tempProfileRef, DataReference moistRef)
            throws VisADException, RemoteException {

        super("VirtualTemperatureProfileCell",
              new DataReference[]{ tempProfileRef,
                                   moistRef }, noData);

        enableAllInputRefs();
    }

    /**
     * Computes the (AirPressure -> VirtualTemperature) profile corresponding to
     * an (AirPressure -> AirTemperature) profile and a moisture profile.  The
     * moisture profile may be either (AirPressure -> DewPoint) or (AirPressure
     * -> WaterVaporMixingRatio).
     *
     * @param datums                The input data corresponding to the data
     *                              references of construction: <code>datums[0]
     *                              </code> is the temperature profile and
     *                              <code>datums[1]</code> is the moisture
     *                              profile.
     * @return                      The corresponding virtual temperature
     *                              profile.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field virtPro = noData;
        Field tempPro = (Field) datums[0];

        if (tempPro != null) {
            Data moisture = datums[1];

            if (moisture != null) {
                if (Util.isCompatible(moisture, DewPoint.getRealType())) {
                    virtPro = (Field) VirtualTemperature.createFromDewPoint(
                        tempPro.getDomainSet(), tempPro, moisture);
                } else if (Util.isCompatible(
                        moisture, WaterVaporMixingRatio.getRealType())) {
                    virtPro =
                        (Field) VirtualTemperature.createFromMixingRatio(
                            tempPro, moisture);
                } else {
                    throw new TypeException("Unknown moisture type: "
                                            + moisture.getType());
                }
            }
        }

        return virtPro;
    }
}








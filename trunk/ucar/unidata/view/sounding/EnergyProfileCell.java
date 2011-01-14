/*
 * $Id: EnergyProfileCell.java,v 1.5 2005/05/13 18:33:30 jeffmc Exp $
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
import ucar.visad.quantities.MassicEnergy;
import ucar.visad.quantities.MassicVolume;
import ucar.visad.quantities.Pressure;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.Field;

import visad.Data;

import visad.DataReference;

import visad.FunctionType;

import visad.TypeException;

import visad.RealTupleType;

import visad.VisADException;


/**
 * Computes the massic energy profile of an atmospheric buoyancy-profile.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:30 $
 */
public final class EnergyProfileCell extends ComputeCell {

    /** no data */
    private static final Data noData;

    static {
        try {
            noData = new AtmosphericProfile(
                MassicEnergy.getRealType()).missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();

            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Constructs from references to the buoyancy profile.
     *
     * @param buoyProfileRef         The buoyancy profile reference.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public EnergyProfileCell(DataReference buoyProfileRef)
            throws VisADException, RemoteException {

        super("EnergyProfileCell", new DataReference[]{ buoyProfileRef },
              noData);

        enableAllInputRefs();
    }

    /**
     * Computes the (AirPressure -> MassicEnergy) profile from an (AirPressure
     * -> MassicVolume) profile.
     *
     * @param datums                The input data in the same order as during
     *                              construction.  <code>datums[0]</code> is the
     *                              (AirPressure -> MassicVolume) buoyancy
     *                              profile.
     * @return                      The corresponding (AirPressure ->
     *                              MassicEnergy) profile.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field buoyProfile   = (Field) datums[0];
        Data  energyProfile = noData;

        if (buoyProfile != null) {
            Util.vetType(MassicVolume.getRealType(), buoyProfile);

            FunctionType  funcType   = (FunctionType) buoyProfile.getType();
            RealTupleType domainType = funcType.getDomain();

            if ( !Pressure.getRealType().equalsExceptNameButUnits(
                    domainType)) {
                throw new TypeException(domainType.toString());
            }

            /*
             * Integrate the buoyancy profile.
             */
            energyProfile = VisADMath.curveIntegralOfGradient(buoyProfile);
        }

        return energyProfile;
    }
}








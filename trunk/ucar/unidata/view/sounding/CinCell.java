/*
 * $Id: CinCell.java,v 1.7 2005/05/13 18:33:26 jeffmc Exp $
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

import ucar.visad.quantities.CAPE;
import ucar.visad.quantities.MassicEnergy;
import ucar.visad.quantities.Pressure;
import ucar.visad.quantities.AirPressure;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.Field;

import visad.Data;

import visad.DataReference;

import visad.FunctionType;

import visad.TypeException;

import visad.Real;

import visad.RealTupleType;

import visad.VisADException;


/**
 * Computes the Convective INhibition (CIN) from a profile of massic energy,
 * the initial release level, and the level of free convection.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:26 $
 */
public final class CinCell extends EnergyFeatureCell {

    /**
     * Constructs from references to the massic energy profile, the iniital
     * level, and the Level of Free Convection (LFC).
     *
     * @param energyProfileRef       The massic energy profile reference.
     * @param initLevRef             The initial level reference.
     * @param lfcRef                 The LFC reference.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public CinCell(DataReference energyProfileRef, DataReference initLevRef, DataReference lfcRef)
            throws VisADException, RemoteException {

        super("CinCell", energyProfileRef, initLevRef, lfcRef);

        enableAllInputRefs();
    }

    /**
     * Computes the output Convective INhibition (CIN) from a massic energy
     * profile, the parcel's initial pressure, and the pressure at the level of
     * free convection (LFC).
     *
     * @param datums                The input data in the same order as during
     *                              construction.  <code>datums[0]</code> is the
     *                              massic energy profile;
     *                              <code>datums[1]</code> is the initial
     *                              pressure; and <code>datums[2]</code> is the
     *                              LFC pressure.
     * @return                      The corresponding CIN.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field energyProfile = (Field) datums[0];
        Data  cin           = noData;

        if (energyProfile != null) {
            Util.vetType(MassicEnergy.getRealType(), energyProfile);

            Real initPres = (Real) datums[1];

            if (initPres != null) {
                Util.vetType(AirPressure.getRealType(), initPres);

                Real lfc = (Real) datums[2];

                if (lfc != null) {
                    Util.vetType(AirPressure.getRealType(), lfc);

                    FunctionType funcType =
                        (FunctionType) energyProfile.getType();
                    RealTupleType domainType = funcType.getDomain();

                    if ( !Pressure.getRealType().equalsExceptNameButUnits(
                            domainType)) {
                        throw new TypeException(domainType.toString());
                    }

                    /*
                     * CIN is the difference in the massic energy profile
                     * between the initial pressure  and the LFC.
                     */
                    Real value =
                        (Real) Util
                            .clone(VisADMath
                                .subtract(energyProfile
                                    .evaluate(initPres), energyProfile
                                    .evaluate(lfc)), ucar.visad.quantities
                                        .CAPE.getRealType());

                    cin = value;
                }
            }
        }

        return cin;
    }
}








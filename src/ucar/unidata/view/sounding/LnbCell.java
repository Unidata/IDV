/*
 * $Id: LnbCell.java,v 1.6 2005/05/13 18:33:32 jeffmc Exp $
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

import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.MassicVolume;
import ucar.visad.quantities.Pressure;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.Data;

import visad.DataReference;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.MathType;

import visad.Real;

import visad.RealTupleType;

import visad.RealType;

import visad.Set;

import visad.SI;

import visad.TypeException;

import visad.Unit;

import visad.VisADException;


/**
 * Computes the level of neutral buoyancy (LNB) from an atmospheric
 * buoyancy-profile.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:32 $
 */
public final class LnbCell extends ProfileFeatureCell {

    /**
     * Constructs from data references.
     *
     * @param buoyProfileRef   The input buoyancy profile reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public LnbCell(DataReference buoyProfileRef)
            throws VisADException, RemoteException {

        super("LfcCell", buoyProfileRef);

        enableAllInputRefs();
    }

    /**
     * Computes the pressure at the Level of Neutral Buoyancy (LNB) of a
     * (AirPressure -> MassicVolume) buoyancy profile.
     *
     * @param datums                The input data in the same order as during
     *                              construction: <code>datums[0]</code> is the
     *                              buoyancy profile.
     * @return                      The pressure at the LNB of the buoyancy
     *                              profile.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field buoyProfile = (Field) datums[0];
        Real  lnb         = noData;  // default return value

        if (buoyProfile != null) {
            FunctionType  funcType   = (FunctionType) buoyProfile.getType();
            RealTupleType domainType = funcType.getDomain();

            if ( !Pressure.getRealType().equalsExceptNameButUnits(
                    domainType)) {
                throw new TypeException(domainType.toString());
            }

            MathType rangeType = funcType.getRange();

            Util.vetType(MassicVolume.getRealType(), buoyProfile);

            Set      domainSet = buoyProfile.getDomainSet();
            double[] pressures = domainSet.getDoubles()[0];
            float[]  buoys     = buoyProfile.getFloats()[0];

            if (pressures.length > 1) {
                int     lastI     = pressures.length - 1;
                boolean ascending = pressures[0] >= pressures[lastI];

                Unit    presUnit  = domainSet.getSetUnits()[0];
                int     i;

                if (ascending) {
                    /*
                     * For a level of neutral buoyancy to exist, the upper
                     * buoyancy must be negative.
                     */
                    for (i = lastI; (i >= 0) && (buoys[i] >= 0); i--);

                    /*
                     * To find the level of neutral buoyancy, descend
                     * to positive buoyancy.
                     */
                    while ((--i >= 0) && (buoys[i] <= 0));

                    if (i >= 0) {
                        lnb = interpolatePres(pressures[i + 1], buoys[i + 1],
                                              pressures[i], buoys[i],
                                              presUnit);
                    }
                } else {
                    /*
                     * For a level of neutral buoyancy to exist, the upper
                     * buoyancy must be negative.
                     */
                    for (i = 0; (i <= lastI) && (buoys[i] >= 0); i++);

                    /*
                     * To find the level of neutral buoyancy, descend from
                     * the top to positive buoyancy.
                     */
                    while ((++i <= lastI) && (buoys[i] <= 0));

                    if (i <= lastI) {
                        lnb = interpolatePres(pressures[i - 1], buoys[i - 1],
                                              pressures[i], buoys[i],
                                              presUnit);
                    }
                }
            }
        }

        return lnb;
    }
}








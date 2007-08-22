/*
 * $Id: ProfileCleanerCell.java,v 1.5 2005/05/13 18:33:35 jeffmc Exp $
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

import visad.CoordinateSystem;

import visad.Data;

import visad.DataReference;

import visad.ErrorEstimate;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.Gridded1DSet;

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
 * Cleans a profile by eliminating non-finite pressures and ensuring that the
 * profile is ascending.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:35 $
 */
public final class ProfileCleanerCell extends ComputeCell {

    /** no data */
    private final FlatField noData;

    /**
     * Constructs from data references.
     *
     * @param proRef           The input profile reference.
     * @param rangeType        The type of the range of the output profile.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public ProfileCleanerCell(DataReference proRef, RealType rangeType)
            throws VisADException, RemoteException {

        super("ProfileCleanerCell", new DataReference[]{ proRef },
              noDataField(rangeType));

        noData = noDataField(rangeType);

        enableAllInputRefs();
    }

    /**
     * Computes the output profile from an (AirPressure -> X) input profile.
     *
     * @param datums                The input data in the same order as during
     *                              construction: <code>datums[0]</code> is the
     *                              input profile.
     * @return                      The corresponding output profile with no
     *                              missing data in its range.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        FlatField inPro  = (FlatField) datums[0];
        FlatField outPro = noData;

        if (inPro != null) {
            boolean       newProfile = false;
            FunctionType  funcType   = (FunctionType) inPro.getType();
            RealTupleType domainType = funcType.getDomain();

            if ( !Pressure.getRealType().equalsExceptNameButUnits(
                    domainType)) {
                throw new TypeException(domainType.toString());
            }

            MathType rangeType = funcType.getRange();

            Util.vetType(rangeType, inPro);

            Set     domainSet = inPro.getDomainSet();
            float[] pressures = domainSet.getSamples()[0];
            float[] buoys     = inPro.getFloats()[0];

            /* Eliminate non-finite pressures and buoyancies. */
            int n = 0;

            for (int i = 0; i < pressures.length; i++) {
                if ((pressures[i] != pressures[i])
                        || (buoys[i] != buoys[i])) {
                    n++;
                }
            }

            if (n > 0) {
                float[] tmpPres = new float[pressures.length - n];
                float[] tmpBuoy = new float[tmpPres.length];

                n = 0;

                for (int i = 0; i < pressures.length; i++) {
                    if ((pressures[i] != pressures[i])
                            || (buoys[i] != buoys[i])) {
                        continue;
                    }

                    tmpPres[n] = pressures[i];
                    tmpBuoy[n] = buoys[i];

                    n++;
                }

                pressures  = tmpPres;
                buoys      = tmpBuoy;
                newProfile = true;
            }

            Unit presUnit = domainSet.getSetUnits()[0];

            if (pressures.length > 1) {
                boolean ascending = pressures[0] > pressures[1];

                if ( !ascending) {

                    /*
                     * The profile is descending.  Make the temporary value
                     * arrays ascending.
                     */
                    for (int i = 0, j = pressures.length;
                            i < pressures.length / 2; i++) {
                        --j;

                        float pres = pressures[i];

                        pressures[i] = pressures[j];
                        pressures[j] = pres;

                        float buoy = buoys[i];

                        buoys[i] = buoys[j];
                        buoys[j] = buoy;
                    }

                    newProfile = true;
                }

                if ( !newProfile) {
                    outPro = inPro;
                } else {
                    outPro = new FlatField(funcType, Gridded1DSet
                        .create(domainSet
                            .getType(), pressures, (CoordinateSystem) null, domainSet
                            .getSetUnits()[0], (ErrorEstimate) null), (CoordinateSystem) null, (CoordinateSystem[]) null, (Set[]) null, inPro
                                .getRangeUnits()[0]);

                    outPro.setSamples(new float[][] {
                        buoys
                    });
                }
            }
        }

        return outPro;
    }
}








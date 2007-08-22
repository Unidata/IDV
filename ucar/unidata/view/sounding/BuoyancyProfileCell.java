/*
 * $Id: BuoyancyProfileCell.java,v 1.8 2005/05/13 18:33:25 jeffmc Exp $
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
import ucar.visad.quantities.MassicVolume;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.Data;

import visad.DataReference;

import visad.Field;

import visad.TypeException;

import visad.VisADException;


/**
 * <p>Computes a buoyancy profile from profiles of parcel and environmental
 * densities.  The buoyancy is defined as the massic volume (alias "specific
 * volume" or "volume per mass") of the parcel minus the massic volume of the
 * environment.  Positive buoyancy corresponds to an upward force on the parcel.
 * </p>
 *
 * <p>This class is thread-compatible but not thread-safe: clients should
 * synchronize concurrent access to instances of this class.</p>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2005/05/13 18:33:25 $
 */
public final class BuoyancyProfileCell extends ComputeCell {

    /** no data */
    private static final Data noData;

    static {
        try {
            noData = new AtmosphericProfile(
                MassicVolume.getRealType()).missingData();
        } catch (Exception ex) {
            System.err.println("Couldn't initialize class");
            ex.printStackTrace();

            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Constructs from input and output data references.
     *
     * @param envDenProRef     The input environmental density profile
     *                         reference.
     * @param parDenProRef     The input parcel density profile reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public BuoyancyProfileCell(DataReference envDenProRef, DataReference parDenProRef)
            throws VisADException, RemoteException {

        super("BuoyancyProfileCell", new DataReference[]{ envDenProRef,
                                                          parDenProRef }, noData);

        enableAllInputRefs();
    }

    /**
     * Computes the output buoyancy-profile.
     *
     * @param datums                The input data.  <code>datums[0]</code> is
     *                              the environment's air density profile;
     *                              <code>datums[1]</code> is the parcel's air
     *                              density profile.
     * @return                      The corresponding boyancy profile as the
     *                              difference in massic volumes of the two
     *                              input profiles.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Data  buoyPro   = noData;
        Field envDenPro = (Field) datums[0];

        if (envDenPro != null) {
            Util.vetType(AirDensity.getRealType(), envDenPro);

            Field parDenPro = (Field) datums[1];

            if (parDenPro != null) {
                Util.vetType(AirDensity.getRealType(), parDenPro);

                /*
                System.out.println("envDenPro=" + envDenPro.longString());
                System.out.println("parDenPro=" + parDenPro.longString());
                 */
                buoyPro = Util.clone(
                    VisADMath.subtract(
                        VisADMath.invert(parDenPro), VisADMath.invert(
                            envDenPro)), MassicVolume.getRealType());
            }
        }

        return buoyPro;
    }
}








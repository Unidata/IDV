/*
 * $Id: ProfileFeatureCell.java,v 1.7 2005/05/13 18:33:35 jeffmc Exp $
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

import visad.DataReference;

import visad.Real;

import visad.Unit;

import visad.VisADException;


/**
 * Computes the level of a feature from an atmospheric buoyancy-profile.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:35 $
 */
public abstract class ProfileFeatureCell extends ComputeCell {

    /**
     * The output "missing data" value.
     */
    protected static final Real noData;

    static {
        try {
            noData = (Real) AirPressure.getRealType().missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();

            throw new RuntimeException();
        }
    }

    /**
     * Constructs from data references.  The input buoyancy profile must be
     * ascending.
     *
     * @param name             The name of the cell.
     * @param buoyProfileRef   The input buoyancy profile reference.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public ProfileFeatureCell(String name, DataReference buoyProfileRef)
            throws VisADException, RemoteException {
        super(name, new DataReference[]{ buoyProfileRef }, noData);
    }

    /**
     * Interpolates the pressure at zero buoyancy from bracketing pressures
     * and buoyancies.  The buoyancies must not have the same sign.
     *
     * @param topPres         The upper pressure.
     * @param topBuoy         The buoyancy at the upper pressure.
     * @param botPres         The lower pressure.
     * @param botBuoy         The buoyancy at the lower pressure.
     * @param presUnit        The unit of pressure.
     * @return                The interpolated pressure at zero buoyancy.
     * @throws VisADException if a VisAD failure occurs.
     */
    protected final static Real interpolatePres(double topPres, double topBuoy, double botPres, double botBuoy, Unit presUnit)
            throws VisADException {

        double pressure = ((topPres * botBuoy) - (botPres * topBuoy))
                          / (botBuoy - topBuoy);

        return new Real(AirPressure.getRealType(), pressure, presUnit);
    }
}








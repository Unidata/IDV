/*
 * $Id: EnergyFeatureCell.java,v 1.5 2005/05/13 18:33:30 jeffmc Exp $
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

import ucar.visad.quantities.MassicEnergy;

import visad.Data;

import visad.DataReference;

import visad.VisADException;


/**
 * Computes a massic energy feature from a profile of massic energy and two
 * integration limits.  Examples include Convective Available Potential Energy
 * (CAPE) and Convective Inhibition (CIN).
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:30 $
 */
public abstract class EnergyFeatureCell extends ComputeCell {

    /**
     * The output, missing data value.
     */
    protected static final Data noData;

    static {
        try {
            noData = MassicEnergy.getRealType().missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();

            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Constructs from references to the massic energy profile and two
     * integration limits.
     *
     * @param name                   The name of the {@link ComputeCell}.
     * @param energyProfileRef       The massic energy profile reference.
     * @param lowerLim               The lower limit reference.
     * @param upperLim               The upper limit reference.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public EnergyFeatureCell(String name, DataReference energyProfileRef, DataReference lowerLim, DataReference upperLim)
            throws VisADException, RemoteException {
        super(name, new DataReference[]{ energyProfileRef, lowerLim,
                                         upperLim }, noData);
    }
}








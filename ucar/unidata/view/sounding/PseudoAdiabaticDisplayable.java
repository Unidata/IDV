/*
 * $Id: PseudoAdiabaticDisplayable.java,v 1.7 2005/05/13 18:33:35 jeffmc Exp $
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

import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.DisplayableDataRef;

import visad.ConstantMap;

import visad.Data;

import visad.DataReference;

import visad.Display;

import visad.VisADException;


/**
 * {@link ucar.visad.display.Displayable} for the pseudo-adiabatic path
 * of a lifted air parcel.
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:35 $
 */
public final class PseudoAdiabaticDisplayable extends CompositeDisplayable {

    /**
     * Constructs from {@link visad.DataReference}s for the dry portion of
     * the path and the wet portion of the path.
     *
     * @param dryRef              Reference to the dry portion of the path.
     * @param wetRef              Reference to the wet portion of the path.
     * @throws VisADException     if a VisAD failure occurs.
     * @throws RemoteException    if a Java RMI failure occurs.
     */
    public PseudoAdiabaticDisplayable(DataReference dryRef, DataReference wetRef)
            throws VisADException, RemoteException {

        DisplayableDataRef dryPath = new DisplayableDataRef(dryRef);
        DisplayableDataRef wetPath = new DisplayableDataRef(wetRef);

        addDisplayable(dryPath);
        addDisplayable(wetPath);
    }
}








/*
 * $Id: WindDataModel.java,v 1.9 2005/05/13 18:33:41 jeffmc Exp $
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

import visad.*;


/**
 * Defines the API for a database of wind profiles.  The database comprises a
 * mutable list of wind profiles and mean winds and selected wind profiles
 * and mean winds.
 *
 * @author Steven R. Emmerson
 * @version $Id: WindDataModel.java,v 1.9 2005/05/13 18:33:41 jeffmc Exp $
 */
interface WindDataModel extends DataModel {

    /**
     * Returns the wind profile and mean-wind at the given index.
     * @param index             The index of the sounding.
     * @return                  The wind data at the given index.  The returned
     *                          array has 2 elements; the first element is the
     *                          wind profile and the second element is the mean
     *                          wind.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    Object[] getWindData(int index)
     throws IndexOutOfBoundsException, VisADException, RemoteException;

    /**
     * Returns the wind profile at the given index.
     * @param index             The index of the wind profile.
     * @return                  The wind profile at the given index.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    Field getWindProfile(int index)
     throws IndexOutOfBoundsException, VisADException, RemoteException;

    /**
     * Returns the mean-wind at the given index.
     * @param index             The index of the mean-wind.
     * @return                  The mean-wind at the given index.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    Tuple getMeanWind(int index) throws VisADException, RemoteException;

    /**
     * Returns the data reference for the mean wind at the given index.
     * @param index             The index of the mean wind.
     * @return                  The data reference for the mean wind at the
     *                          given index.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    DataReference getMeanWindRef(int index)
     throws VisADException, RemoteException;
}








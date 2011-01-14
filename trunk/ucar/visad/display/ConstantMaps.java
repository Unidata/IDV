/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.visad.display;


import visad.*;



import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;

import java.util.Iterator;


/**
 * Interface for a collection of adapted {@link visad.ConstantMap}-s.
 *
 * <p>Instances of this interface have the following, bound, JavaBean
 * properties:<br>
 * <table border align=center>
 *
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Access</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr align=center>
 * <td>constantMap</td>
 * <td>{@link visad.ConstantMap}</td>
 * <td></td>
 * <td>class-dependent</td>
 * <td align=left>Fired whenever any {@link visad.ConstantMap} changes
 * </td>
 * </tr>
 *
 * </table>
 * {@link java.beans.PropertyChangeEvent}-s fired for the
 * {@link #CONSTANT_MAP} property have the originating class as the
 * source and the old and new values set to the appropriate
 * {@link ConstantMaps}.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $
 */
public interface ConstantMaps extends Propertied {

    /**
     * The name of the {@link visad.ConstantMap} "property".
     */
    public static final String CONSTANT_MAP = "constantMap";

    /**
     * Returns the {@link visad.ConstantMap}(s) of this instance.
     *
     * @return                  The {@link visad.ConstantMap}(s) of this
     *                          instance.
     */
    public ConstantMap[] getConstantMaps();

    /**
     * Accepts a {@link ConstantMaps.Visitor} to this instance.
     *
     * @param visitor           The {@link ConstantMaps.Visitor} to accept.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void accept(Visitor visitor)
     throws VisADException, RemoteException;

    /**
     * Interface for visitors to {@link ConstantMaps}.
     */
    static interface Visitor {

        /**
         * Visits a collection of adapted {@link ConstantMap}s.
         *
         * @param maps                  The {@link ConstantMapComposite} to
         *                              visit.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        void visit(ConstantMapComposite maps)
         throws VisADException, RemoteException;

        /**
         * Visits an adapted {@link visad.ConstantMap}.
         *
         * @param map                   The {@link ConstantMapComposite} to
         *                              visit.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        void visit(ConstantMapAdapter map)
         throws VisADException, RemoteException;
    }

    /**
     * Adds the adapted {@link visad.ConstantMap}(s) of this instance to a VisAD
     * display.  This method should only be invoked by a DisplayAdapter.
     *
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDisplay(Display display)
     throws VisADException, RemoteException;
}

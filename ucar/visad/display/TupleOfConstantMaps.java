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



import java.rmi.RemoteException;


/**
 * Provides support for a composite of adapted VisAD ConstantMap-s with a fixed
 * number of components.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $
 */
public abstract class TupleOfConstantMaps extends ConstantMapComposite {

    /**
     * Constructs.
     */
    protected TupleOfConstantMaps() {}

    /**
     * Constructs.
     *
     * @param maps              The adapted {@link visad.ConstantMap}s that will
     *                          constitute this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected TupleOfConstantMaps(ConstantMaps maps)
            throws VisADException, RemoteException {
        super(maps);
    }

    /**
     * Constructs.
     *
     * @param maps              The adapted {@link visad.ConstantMap}s that will
     *                          constitute this instance.
     * @throws BadMappingException
     *                          Aggregation of the given {@link visad.ConstantMap}s
     *                          would result in a {@link visad.DisplayRealType} with
     *                          multiple values.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected TupleOfConstantMaps(ConstantMaps[] maps)
            throws BadMappingException, VisADException, RemoteException {

        for (int i = 0; i < maps.length; ++i) {
            add(maps[i]);
        }
    }
}

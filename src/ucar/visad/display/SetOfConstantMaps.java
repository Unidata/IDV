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
 * Provides support for a composite of adapted VisAD ConstantMap-s with an
 * extensible number of components.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $
 */
public class SetOfConstantMaps extends ConstantMapComposite {

    /**
     * Constructs.
     */
    public SetOfConstantMaps() {}

    /**
     * Constructs.
     *
     * @param maps              The adapted, {@link ConstantMaps} to initially
     *                          comprise this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @throws BadMappingException
     *                          if addition of the {@link ConstantMaps} would
     *                          result in multiple values for a {@link
     *                          visad.DisplayRealType}.
     */
    public SetOfConstantMaps(ConstantMaps maps)
            throws BadMappingException, VisADException, RemoteException {
        super(maps);
    }

    /**
     * Adds adapted, {@link visad.ConstantMap}s to this instance.
     *
     * @param maps              The adapted, {@link visad.ConstantMap}s to be
     *                          added to this instance.
     * @throws BadMappingException
     *                          Addition of the {@link visad.ConstantMap}s would
     *                          result is a {@link visad.DisplayRealType} having
     *                          multiple values.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void add(ConstantMaps maps)
            throws BadMappingException, VisADException, RemoteException {
        super.add(maps);
    }
}

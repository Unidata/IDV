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
 * Provides support for adapting ScalarMap-s of {@link Display#IsoContour}.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $
 */
public abstract class IsoContourScalarMap extends ScalarMapAdapter {

    /**
     * Constructs.
     *
     * @param realType          The type of data to be contoured.
     * @param display           The adapted, VisAD display for rendering.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected IsoContourScalarMap(RealType realType, DisplayAdapter display)
            throws VisADException, RemoteException {
        super(realType, Display.IsoContour, display);
    }

    /**
     * Returns the {@link ContourControl} associated with this instance.
     *
     * @return                  The {@link ContourControl} associated with this
     *                          instance.  May be <code>null</code>.
     */
    protected final ContourControl getContourControl() {
        return (ContourControl) getControl();
    }
}

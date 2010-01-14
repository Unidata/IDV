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



import visad.VisADException;

import java.rmi.RemoteException;


/**
 * This class is used to draw one line connecting two end points.
 * It is used, for example,
 * to draw a line showing location of a vertical profile in the 3D Viewer.
 *
 * @author Stuart Wier
 * @version $Revision $
 */
public class ProfileLine extends LineDrawing {

    /**
     * Constructor; only invokes super class LineDrawing.
     * @param name a String required for LineDrawing construction.
     * @throws RemoteException if a Java RMI failure occurs.
     * @throws VisADException  if a core VisAD failure occurs.
     */
    public ProfileLine(String name) throws RemoteException, VisADException {
        super(name);
    }

    /**
     * Required method used to make a copy of this object.
     * @return another Displayable
     * @throws RemoteException if a Java RMI failure occurs.
     * @throws VisADException  if a core VisAD failure occurs.
     */
    public synchronized Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new ProfileLine(this.getName());
    }
}

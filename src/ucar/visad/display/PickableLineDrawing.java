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


import visad.DataRenderer;


import visad.VisADException;

import visad.bom.PickManipulationRendererJ3D;

import java.rmi.RemoteException;


/**
 * Subclass of LineDrawing to support a pickable data object
 */
public class PickableLineDrawing extends LineDrawing {

    /** object for getting pick events */
    PickManipulationRendererJ3D picker = null;

    /** mask for the picking */
    int mask = 0;

    /**
     * Create a new PickableLineDrawing
     *
     * @param name   name for this
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public PickableLineDrawing(String name)
            throws VisADException, RemoteException {
        this(name, 0);
    }

    /**
     * Local line drawing implementation
     *
     * @param name   name for this
     * @param pickMask mouse mask for pick events
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public PickableLineDrawing(String name, int pickMask)
            throws VisADException, RemoteException {
        super(name);
        this.mask = mask;
    }

    /**
     * Get the data renderer
     * @return  A pick manipulation renderer
     */
    protected DataRenderer getDataRenderer() {
        if (picker == null) {
            picker = new PickManipulationRendererJ3D(mask, mask);
        }
        return picker;
    }

    /**
     * Get the closest index in the data object.
     *
     * @return return index of the picked object or -1 if not picked.
     */
    public int getCloseIndex() {
        return picker.getCloseIndex();
    }

}

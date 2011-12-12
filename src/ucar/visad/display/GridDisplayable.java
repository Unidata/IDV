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


import visad.FieldImpl;


import visad.VisADException;

import java.rmi.RemoteException;


/**
 * An interface for Displayables that display gridded data.
 * @author Jeff McWhirter
 * @version $Revision: 1.7 $
 */
public interface GridDisplayable {

    /**
     * Load data into this Displayable.
     *
     * @param field  field representing the data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void loadData(FieldImpl field)
     throws VisADException, RemoteException;

    /**
     * Set whether this GridDisplayable should have the data colored
     * by another parameter.
     *
     * @param yesno true if colored by another
     */
    public void setColoredByAnother(boolean yesno);
}

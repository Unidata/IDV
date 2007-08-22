/*
 * $Id: WindProfileDisplayRenderer.java,v 1.10 2005/05/13 18:33:41 jeffmc Exp $
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



import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import visad.VisADException;


/**
 * Provides an interface to a VisAD DisplayRenderer for displaying wind
 * profiles.
 *
 * @author Steven R. Emmerson, Unidata/UCAR
 * @version $Id: WindProfileDisplayRenderer.java,v 1.10 2005/05/13 18:33:41 jeffmc Exp $
 */
public interface WindProfileDisplayRenderer {

    /**
     * Sets the cursor string vector.
     */
    public void setCursorStringVector();

    /**
     * Returns the cursor position.
     * @return                  The cursor position.
     */
    public double[] getCursorPosition();

    /**
     * Sets the visibility of the VisAD box.
     * @param on                Whether or not the box should be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setBoxOn(boolean on) throws VisADException, RemoteException;

    /**
     * Sets the visibility of the cursor strings.
     * @param on                Whether or not the cursor strings should be
     *                          visible.
     */
    public void setCursorStringOn(boolean on);

    /**
     * Adds a PropertyChangeListener for changes to the cursor position.
     * @param listener          The change listener.
     */
    public void addCursorPositionListener(PropertyChangeListener listener);

    /**
     * Removes a PropertyChangeListener for changes to the cursor position.
     * @param listener          The change listener.
     */
    public void removeCursorPositionListener(PropertyChangeListener listener);
}








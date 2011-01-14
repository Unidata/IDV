/*
 * $Id: WindStaffDisplayRenderer.java,v 1.12 2005/05/13 18:33:42 jeffmc Exp $
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

import java.util.Vector;

import javax.swing.event.SwingPropertyChangeSupport;

import visad.*;
import visad.java2d.DefaultDisplayRendererJ2D;


/**
 * Provides support for a VisAD DisplayRenderer for a wind-staff display.
 *
 * @author Steven R. Emmerson, Unidata/UCAR
 * @version $Id: WindStaffDisplayRenderer.java,v 1.12 2005/05/13 18:33:42 jeffmc Exp $
 */
public class WindStaffDisplayRenderer extends DefaultDisplayRendererJ2D
        implements WindProfileDisplayRenderer {

    /**
     * The name of the cursor position property.
     */
    public static String CURSOR_POSITION = "cursorPosition";

    /** cursor position */
    private double[] cursorPosition = null;

    /** listeners */
    private final SwingPropertyChangeSupport changeListeners =
        new SwingPropertyChangeSupport(this);

    /**
     * Constructs from nothing.
     */
    public WindStaffDisplayRenderer() {}

    /**
     * Sets the cursor string vector.
     */
    public void setCursorStringVector() {

        double[] oldCursorPosition = cursorPosition;
        double[] cursorPosition    = getCursorInDisplayCoords();

        changeListeners.firePropertyChange(CURSOR_POSITION,
                                           oldCursorPosition, cursorPosition);
        super.setCursorStringVector();
    }

    /**
     * Returns the cursor position.
     * @return                  The cursor position.
     */
    public double[] getCursorPosition() {
        return cursorPosition;
    }

    /**
     * Adds a listener for changes to the cursor position.
     * @param listener          The change listener.
     */
    public void addCursorPositionListener(PropertyChangeListener listener) {
        changeListeners.addPropertyChangeListener(CURSOR_POSITION, listener);
    }

    /**
     * Removes a listener for changes to the cursor position.
     * @param listener          The change listener.
     */
    public void removeCursorPositionListener(
            PropertyChangeListener listener) {
        changeListeners.removePropertyChangeListener(CURSOR_POSITION,
                listener);
    }

    /**
     * Returns the position of the cursor in RealType coordinates.
     * @return                  The position of the cursor in RealType
     *                          coordinates.
     */
    protected double[] getCursorInDisplayCoords() {

        Vector    mapVector = getDisplay().getMapVector();
        ScalarMap map       = null;

        for (int i = 0; i < mapVector.size(); i++) {
            map = (ScalarMap) mapVector.get(i);

            if (map.getDisplayScalar() == Display.YAxis) {
                break;
            }
        }

        return new double[]{
            map.inverseScaleValues(new float[]{
                (float) getCursor()[1] })[0] };
    }
}








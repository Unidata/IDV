/*
 * $Id: HodographDisplayRendererJ3D.java,v 1.13 2005/05/13 18:33:31 jeffmc Exp $
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
import visad.java3d.DefaultDisplayRendererJ3D;


/**
 * Provides a VisAD DisplayRendererJ3D for a wind hodograph.
 *
 * @author Don Murray, Unidata/UCAR
 * @author Steven R. Emmerson, Unidata/UCAR
 * @version $Id: HodographDisplayRendererJ3D.java,v 1.13 2005/05/13 18:33:31 jeffmc Exp $
 */
public class HodographDisplayRendererJ3D extends DefaultDisplayRendererJ3D
        implements WindProfileDisplayRenderer {

    /**
     * The cursor position;
     */
    private double[] cursorPosition = null;

    /**
     * The property change listeners.
     */
    private final SwingPropertyChangeSupport changeListeners =
        new SwingPropertyChangeSupport(this);

    /**
     * Constructs from nothing.
     */
    public HodographDisplayRendererJ3D() {}

    /**
     * Sets the cursor string vector.
     */
    public void setCursorStringVector() {

        double[] oldCursorPosition = cursorPosition;
        double[] cursorPosition    = getCursorInDisplayCoords();

        changeListeners.firePropertyChange("cursorPosition",
                                           oldCursorPosition, cursorPosition);
        super.setCursorStringVector();
    }

    /**
     * Gets the cursor position.
     * @return                  The (speed,direction,altitude) cursor position.
     */
    public double[] getCursorPosition() {
        return cursorPosition;
    }

    /**
     * Adds a listener for changes to the cursor position.
     * @param listener          The change listener.
     */
    public void addCursorPositionListener(PropertyChangeListener listener) {
        changeListeners.addPropertyChangeListener(listener);
    }

    /**
     * Removes a listener for changes to the cursor position.
     * @param listener          The change listener.
     */
    public void removeCursorPositionListener(
            PropertyChangeListener listener) {
        changeListeners.removePropertyChangeListener(listener);
    }

    /**
     * Returns the cursor position in (speed,direction,altitude) coordinates.
     * @return                  The cursor position.
     */
    protected double[] getCursorInDisplayCoords() {

        double[] cursorCoords = getCursor();
        Vector   v            = getDisplay().getMapVector();
        double   xFactor      = 1.0;
        double   yFactor      = 1.0;
        double[] heightRange  = { -1.0, 1.0 };

        for (int i = 0; i < v.size(); i++) {
            ScalarMap map = (ScalarMap) v.get(i);

            if (map.getDisplayScalar() == Display.XAxis) {
                xFactor = map.getRange()[1];
            }

            if (map.getDisplayScalar() == Display.YAxis) {
                yFactor = map.getRange()[1];
            }

            if (map.getDisplayScalar() == Display.ZAxis) {
                heightRange = map.getRange();
            }
        }

        return new double[]{ cursorCoords[0] * xFactor,
                             cursorCoords[1] * yFactor,
                             ((cursorCoords[2] + 1.0) * (heightRange[1] - heightRange[0]))
                             / 2.0 + heightRange[0] };
    }
}








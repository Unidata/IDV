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


import visad.ContourControl;

import visad.VisADException;



import java.rmi.RemoteException;


/**
 * Provides support for contour levels.
 *
 * Instances are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.10 $
 */
public abstract class ContourLevels {

    /**
     * The base contour level.
     */
    private final float base;

    /**
     * Constructs an instance.  The base contour level will be zero.
     */
    protected ContourLevels() {
        base = 0;
    }

    /**
     * Constructs an instance with a given base contour level.
     *
     * @param base              The base contour level.
     */
    protected ContourLevels(float base) {
        this.base = base;
    }

    /**
     * Returns the base contour level.  The base contour level either
     * determines the transition between dashed and solid contour lines or is a
     * contour line value.
     * @return                  The base contour level.
     */
    public final float getBase() {
        return base;
    }

    /**
     * Gets the contour levels given a data range.
     *
     * @param minimum           The minimum data value.
     * @param maximum           The maximum data value.
     * @return                  Contour levels appropriate for the given data
     *                          range.  No contour level will lie outside the
     *                          range.  The levels will be ordered by increasing
     *                          value.
     * @throws VisADException   Invalid range extrema or VisAD failure.
     */
    public abstract float[] getLevels(float minimum, float maximum)
     throws VisADException;

    /**
     * Sets a VisAD ContourControl.
     *
     * @param control           The VisAD ContourControl to be set by this
     *                          object.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     * @throws RemoteException  Java RMI failure.
     */
    public abstract void setControl(ContourControl control)
     throws VisADException, RemoteException;

    /**
     * Sets a VisAD ContourControl.
     *
     * @param control           The VisAD ContourControl to be set by this
     *                          object.
     * @param minimum           The minimum value.  It shall not be
     *                          Float.NEGATIVE_INFINITY
     * @param maximum           The maximum value.  It shall not be
     *                          Float.POSITIVE_INFINITY
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     * @throws RemoteException  Java RMI failure.
     */
    public abstract void setControl(ContourControl control, float minimum,
                                    float maximum)
     throws VisADException, RemoteException;
}

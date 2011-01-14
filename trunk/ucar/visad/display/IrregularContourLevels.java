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

import java.util.Arrays;


/**
 * Provides support for irregular contours, which are characterized by an
 * explicit set of contour levels.</p>
 *
 * <p>Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $
 */
public final class IrregularContourLevels extends ContourLevels {

    /**
     * The set of contour levels.
     * @serial
     */
    protected final float[] levels;

    /** flag for dashing */
    private boolean dash = false;

    /**
     * Constructs an instance from a list of contour values.  The base
     * contour for dashed lines shall be zero.
     *
     * @param levels            The contour levels.  They shall be ordered by
     *                          increasing value.
     */
    public IrregularContourLevels(float[] levels) {
        this.levels = (float[]) levels.clone();
    }

    /**
     * Constructs an instance from a list of contour values and a base contour
     * level for dashed lines.
     *
     * @param levels            The contour levels.  They shall be ordered by
     *                          increasing value.
     * @param base              The base contour level.  Contour lines below
     *                          this value will be dashed.
     */
    public IrregularContourLevels(float[] levels, float base) {
        this(levels, base, false);
    }

    /**
     * Constructs an instance from a list of contour values and a base contour
     * level for dashed lines.
     *
     * @param levels            The contour levels.  They shall be ordered by
     *                          increasing value.
     * @param base              The base contour level.  Contour lines below
     *                          this value will be dashed.
     * @param dash              dash contours below base if true
     */
    public IrregularContourLevels(float[] levels, float base, boolean dash) {

        super(base);

        this.levels = (float[]) levels.clone();
        this.dash   = dash;
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
     */
    public float[] getLevels(float minimum, float maximum) {

        float[] result;
        int     minIndex = Arrays.binarySearch(levels, minimum);

        if (minIndex == -levels.length - 1) {
            result = new float[0];
        } else {
            if (minIndex < 0) {
                minIndex = 0;
            }

            int maxIndex = Arrays.binarySearch(levels, maximum);

            if (maxIndex == -1) {
                result = new float[0];
            } else {
                if (maxIndex < 0) {
                    maxIndex = levels.length - 1;
                }

                result = new float[maxIndex - minIndex + 1];

                System.arraycopy(levels, minIndex, result, 0, result.length);
            }
        }

        return result;
    }

    /**
     * Sets a VisAD ContourControl.
     *
     * @param control           The VisAD ContourControl to be set by this
     *                          object.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setControl(ContourControl control)
            throws VisADException, RemoteException {
        control.setLevels(levels, getBase(), dash);
    }

    /**
     * Sets a VisAD ContourControl.
     *
     * @param control           The VisAD ContourControl to be set by this
     *                          object.
     * @param minimum           The minimum value.  It shall not be
     *                          Float.NEGATIVE_INFINITY
     * @param maximum           The maximum value.  It shall not be
     *                          Float.POSITIVE_INFINITY
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setControl(ContourControl control, float minimum,
                           float maximum)
            throws VisADException, RemoteException {
        setControl(control);
    }
}

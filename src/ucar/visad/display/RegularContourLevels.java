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



import java.lang.Math;

import java.rmi.RemoteException;


/**
 * Provides support for regular contours, which are characterized by a constant
 * contour interval.</p>
 *
 * <p>Instances of this class are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.12 $
 */
public final class RegularContourLevels extends ContourLevels {

    /**
     * The contour interval.
     * @serial
     */
    protected final float interval;

    /**
     * The minimum contour level.
     * @serial
     */
    protected final float minimum;

    /**
     * The maximum contour level.
     * @serial
     */
    protected final float maximum;

    /**
     * The dashing parameter
     * @serial
     */
    private boolean dash;

    /**
     * Constructs an instance.  The base contour will be zero, the minimum
     * contour will be negative infinity, and the maximum contour will be
     * positive infinity.
     *
     * @param interval          The contour interval.
     */
    public RegularContourLevels(float interval) {
        this(interval, 0);
    }

    /**
     * Constructs an instance.
     *
     * @param interval          The contour interval.
     * @param base              The base contour.
     */
    public RegularContourLevels(float interval, float base) {
        this(interval, base, Float.NEGATIVE_INFINITY,
             Float.POSITIVE_INFINITY);
    }

    /**
     * Constructs an instance.
     *
     * @param interval          The contour interval.
     * @param base              The base contour.  Contours below this
     *                          level will be dashed.
     * @param minimum           The minimum contour.  Must be a finite value.
     * @param maximum           The maximum contour.  Must be a finite value.
     */
    public RegularContourLevels(float interval, float base, float minimum,
                                float maximum) {
        this(interval, base, minimum, maximum, false);
    }

    /**
     * Constructs an instance.
     *
     * @param interval          The contour interval.
     * @param base              The base contour.  Contours below this
     *                          level will be dashed.
     * @param minimum           The minimum contour.  Must be a finite value.
     * @param maximum           The maximum contour.  Must be a finite value.
     * @param dash              Whether or not to draw dashed lines for
     *                          contours less than the base.
     */
    public RegularContourLevels(float interval, float base, float minimum,
                                float maximum, boolean dash) {

        super(base);

        this.interval = interval;
        this.minimum  = minimum;
        this.maximum  = maximum;
        this.dash     = dash;
    }

    /**
     * Gets the contour levels as an array.  This instance must have been
     * constructed with valid range extrema.
     *
     * @return                  Contour levels appropriate for the given data
     *                          range.  No contour level will lie outside the
     *                          valid range.  The levels will be ordered by
     *                          increasing value.
     * @throws VisADException   Invalid range extrema or VisAD failure.
     */
    public synchronized float[] getLevels() throws VisADException {
        return getLevels(minimum, maximum);
    }

    /**
     * Gets the contour levels given a data range.
     *
     * @param minimum           The minimum data value.  Must be a finite value.
     * @param maximum           The maximum data value.  Must be a finite value.
     * @return                  Contour levels appropriate for the given data
     *                          range.  No contour level will lie outside the
     *                          range.  The levels will be ordered by increasing
     *                          value.
     * @throws VisADException   Invalid range extrema or VisAD failure.
     */
    public synchronized float[] getLevels(float minimum, float maximum)
            throws VisADException {

        if (Float.isInfinite(minimum) || Float.isNaN(minimum)
                || Float.isInfinite(maximum) || Float.isNaN(maximum)) {
            throw new VisADException(
                getClass().getName() + ".getLevels(float,float): "
                + "Invalid range extrema (max or min is infinite or NaN)");
        }

        float base = getBase();

        return visad.Contour2D.intervalToLevels(interval, minimum, maximum,
                base, new boolean[1]);
    }

    /**
     * Sets a VisAD ContourControl.  This instance must have been constructed
     * with minimum and maximum range values.
     *
     * @param control           The VisAD ContourControl to be set by this
     *                          object.
     * @throws VisADException   Invalid range extrema or VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setControl(ContourControl control)
            throws VisADException, RemoteException {
        setControl(control, minimum, maximum);
    }

    /**
     * Sets a VisAD ContourControl.
     *
     * @param control           The VisAD ContourControl to be set by this
     *                          object.
     * @param minimum           The minimum value.  Shall be a finite value.
     * @param maximum           The maximum value.  Shall be a finite value.
     * @throws VisADException   Invalid range extrema or VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setControl(ContourControl control,
                                        float minimum, float maximum)
            throws VisADException, RemoteException {

        if (Float.isInfinite(minimum) || Float.isNaN(minimum)
                || Float.isInfinite(maximum) || Float.isNaN(maximum)) {
            throw new VisADException(
                getClass().getName()
                + ".setControl(ContourControl,float,float): "
                + "Invalid range extrema (max or min is infinite or NaN)");
        }

        //control.setContourInterval(interval, minimum, maximum, getBase());
        control.setLevels(visad.Contour2D.intervalToLevels(interval, minimum,
                maximum, getBase(), new boolean[1]), getBase(), dash);
    }
}

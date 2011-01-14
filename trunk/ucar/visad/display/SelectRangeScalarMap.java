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



import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;


/**
 * Provides support for adapting ScalarMap-s of
 * {@link visad.Display#SelectRange}.
 *
 * <p>Instances of this class have the the JavaBean properties of the
 * superclass.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $
 */
public final class SelectRangeScalarMap extends ScalarMapAdapter {

    /** range for selection */
    private float[] range = new float[] { Float.NaN, Float.NaN };

    /**
     * Constructs.
     *
     * @param realType          The type of data to be mapped to the X-axis.
     * @param display           The adapted, VisAD display for rendering.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected SelectRangeScalarMap(RealType realType, DisplayAdapter display)
            throws VisADException, RemoteException {
        super(realType, Display.SelectRange, display);
    }

    /**
     * Sets the data range.
     *
     * @param min               The minimum data value.
     * @param max               The maximum data value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRange(float min, float max)
            throws VisADException, RemoteException {

        range[0] = min;
        range[1] = max;

        setControl();
    }

    /**
     * Sets the data range.
     *
     * @param min               The minimum data value.
     * @param max               The maximum data value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRange(Real min, Real max)
            throws VisADException, RemoteException {

        Unit unit = getDisplayType().getDefaultUnit();

        setRange((float) min.getValue(unit), (float) max.getValue(unit));
    }

    /**
     * Sets the control of the underlying {@link visad.ScalarMap}.  This is a
     * template method.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setControl() throws VisADException, RemoteException {

        RangeControl control = (RangeControl) getControl();

        if (control != null) {
            control.setRange(range);
        }
    }
}

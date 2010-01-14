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

import java.awt.*;

import java.rmi.RemoteException;


/**
 * A VisAD display for cross sections of data fields.
 *
 * @author Don Murray
 * @version $Revision: 1.4 $
 */
public class XSDisplay extends XYDisplay {


    /**
     * The name of the y position property.
     */
    public static final String CURSOR_YVALUE = "cursorYValue";

    /**
     * The name of the x position property.
     */
    public static final String CURSOR_XVALUE = "cursorXValue";

    /**
     * The cursor y position.
     * @serial
     */
    private volatile Real cursorYValue;

    /**
     * The cursor x position.
     * @serial
     */
    private volatile Real cursorXValue;


    /**
     * Default cstr with yAxisType of RealType.YAxis, xAxisType of
     * RealType.XAxis.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public XSDisplay() throws VisADException, RemoteException {
        this(false, null);
    }

    /**
     * Default cstr with yAxisType of RealType.YAxis, xAxisType of
     * RealType.XAxis.
     *
     *
     * @param offScreen Is offscreen
     * @param dimension Size of display. May be null.
     * @throws RemoteException
     * @throws VisADException
     */

    public XSDisplay(boolean offScreen, Dimension dimension)
            throws VisADException, RemoteException {
        super("Cross section Display", RealType.XAxis, RealType.YAxis,
              offScreen, dimension);
    }

    /**
     * Default cstr with yAxisType of RealType.Altitude, xAxisType of
     * RealType.XAxis.
     *
     * @param name  name for this display
     * @param xType  RealType for the X axis
     * @param yType  RealType for the Y axis
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public XSDisplay(String name, RealType xType, RealType yType)
            throws VisADException, RemoteException {
        this(name, xType, yType, false, null);
    }

    /**
     * Default cstr with yAxisType of RealType.Altitude, xAxisType of
     * RealType.XAxis.
     *
     * @param name  name for this display
     * @param xType  RealType for the X axis
     * @param yType  RealType for the Y axis
     * @param offScreen Is offscreen
     * @param dimension Size of display. May be null.
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    public XSDisplay(String name, RealType xType, RealType yType,
                     boolean offScreen, Dimension dimension)
            throws VisADException, RemoteException {
        super(name, xType, yType, offScreen, dimension);
    }

    /**
     * Sets the cursor x value property.  Called by subclasses.
     *
     * @param yxalue          The cursor y value.
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    protected void setCursorYValue(Real yxalue)
            throws VisADException, RemoteException {
        Real oldYValue = cursorYValue;
        cursorYValue = yxalue;
        firePropertyChange(CURSOR_YVALUE, oldYValue, cursorYValue);
    }

    /**
     * Gets the cursor altitude property.
     *
     * @return The currently-selected altitude.  May be <code>null</code>.
     */
    public Real getCursorYValue() {
        return cursorYValue;
    }


    /**
     * Sets the cursor x position property.  Called by subclasses.
     *
     * @param xvalue          The cursor x position.
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    protected void setCursorXValue(Real xvalue)
            throws VisADException, RemoteException {
        Real oldval = xvalue;
        cursorXValue = xvalue;
        firePropertyChange(CURSOR_XVALUE, oldval, cursorXValue);
    }

    /**
     * Gets the cursor x position property.
     *
     * @return  The currently-selected x value.  May be <code>null</code>.
     */
    public Real getCursorXValue() {
        return cursorXValue;
    }

}

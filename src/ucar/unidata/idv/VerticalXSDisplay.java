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

package ucar.unidata.idv;



import ucar.visad.display.XSDisplay;

import ucar.visad.quantities.Length;

import visad.*;

import java.awt.*;

import java.rmi.RemoteException;


/**
 * A VisAD display for 2D vertical cross sections of 3D data fields.
 *
 * @author Don Murray
 */
public class VerticalXSDisplay extends XSDisplay {


    /**
     * The name of the altitude property.
     */
    public static final String CURSOR_ALTITUDE = "cursorAltitude";

    /**
     * The cursor altitude.
     * @serial
     */
    private volatile Real cursorAltitude;

    /**
     * Default cstr with yAxisType of RealType.Altitude, xAxisType of
     * RealType.XAxis.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public VerticalXSDisplay() throws VisADException, RemoteException {
        this(false, null);
    }


    /**
     * Default cstr with yAxisType of RealType.Altitude, xAxisType of
     * RealType.XAxis.
     *
     * @param offScreen are we in offscreen mode
     * @param dimension the offscreen dimension. may be null
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public VerticalXSDisplay(boolean offScreen, Dimension dimension)
            throws VisADException, RemoteException {
        super("Vertical Cross Section", Length.getRealType(),
              RealType.Altitude, offScreen, dimension);
        setAxisParams();
    }

    /**
     * Called on construction to initialize the axis parameters
     */
    private void setAxisParams() {

        // Make AxisScales-s - label the axes.
        //Font ftr = new Font ("Helvetica", Font.BOLD, 4);
        showAxisScales(true);
        AxisScale xscale = getXAxisScale();
        if (xscale != null) {
            setXAxisTitle();
            xscale.setSnapToBox(true);
            xscale.setColor(Color.blue);
            xscale.setAutoComputeTicks(true);
        }
        AxisScale zscale = getYAxisScale();
        if (zscale != null) {
            zscale.setSnapToBox(true);
            zscale.setColor(Color.blue);
            zscale.setAutoComputeTicks(true);

            RealType yAxisType = getYAxisType();
            if (yAxisType != null) {
                Unit unit = yAxisType.getDefaultUnit();
                if (unit != null) {
                    zscale.setTitle(yAxisType.toString() + " (" + unit + ")");
                }
            }
        }
    }

    /**
     * Sets the cursor altitude property.  Called by subclasses.
     *
     * @param altitude          The cursor altitude.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void setCursorAltitude(Real altitude)
            throws VisADException, RemoteException {
        Real oldAltitude = cursorAltitude;
        cursorAltitude = altitude;
        firePropertyChange(CURSOR_ALTITUDE, oldAltitude, cursorAltitude);
    }

    /**
     * Gets the cursor altitude property.
     *
     * @return The currently-selected altitude.  May be <code>null</code>.
     */
    public Real getCursorAltitude() {
        return cursorAltitude;
    }

    /**
     * Set the units of displayed values on the X axis
     *
     * @param  newUnit  units to use
     */
    public void setXDisplayUnit(Unit newUnit) {
        super.setXDisplayUnit(newUnit);
        setXAxisTitle();
    }

    /**
     * Set the title on the XAxis.
     */
    public void setXAxisTitle() {
        AxisScale xscale = getXAxisScale();
        if (xscale != null) {
            xscale.setTitle("Distance along transect (" + getXDisplayUnit()
                            + ")");
        }
    }
}

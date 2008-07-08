/*
 * $Id: ValueSymbol.java,v 1.21 2007/05/22 20:00:23 jeffmc Exp $
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

package ucar.unidata.ui.symbol;


import ucar.unidata.ui.drawing.DisplayCanvas;
import ucar.unidata.util.Misc;

import visad.Unit;

import java.awt.*;
import java.awt.geom.*;

import java.text.DecimalFormat;

import java.text.NumberFormat;


/**
 * Value Symbol meteorological Symbol.  Used for displaying numeric
 * values in specific units.
 * @author MetApps Development Team
 * @version $Revision: 1.21 $
 */
public class ValueSymbol extends TextSymbol {

    /** the value */
    private double value;


    /**
     * Default ctor
     */
    public ValueSymbol() {}

    /**
     * Create a new ValueSymbol
     *
     * @param canvas   canvas for painting
     * @param x        x position
     * @param y        y position
     *
     */
    public ValueSymbol(DisplayCanvas canvas, int x, int y) {
        this(canvas, x, y, "", "Parameter");
    }

    /**
     * Create a new ValueSymbol
     *
     * @param x        x position
     * @param y        y position
     * @param param    the parameter name
     * @param paramDesc  the parameter description
     * @param unit       the unit for the param
     *
     */
    public ValueSymbol(int x, int y, String param, String paramDesc,
                       Unit unit) {
        this(null, x, y, param, paramDesc, unit);
    }


    /**
     * Create a new ValueSymbol
     *
     * @param x        x position
     * @param y        y position
     * @param param    the parameter name
     * @param paramDesc  the parameter description
     */
    public ValueSymbol(int x, int y, String param, String paramDesc) {
        this(null, x, y, param, paramDesc);
    }

    /**
     * Create a new ValueSymbol
     *
     * @param canvas   canvas to paint on
     * @param x        x position
     * @param y        y position
     * @param param    the parameter name
     * @param paramDesc  the parameter description
     *
     */
    public ValueSymbol(DisplayCanvas canvas, int x, int y, String param,
                       String paramDesc) {
        this(canvas, x, y, param, paramDesc, (Unit) null);
    }



    /**
     * Create a new ValueSymbol
     *
     * @param canvas   canvas to paint on
     * @param x        x position
     * @param y        y position
     * @param param    the parameter name
     * @param paramDesc  the parameter description
     * @param u          the unit
     */
    public ValueSymbol(DisplayCanvas canvas, int x, int y, String param,
                       String paramDesc, Unit u) {
        super(canvas, x, y, param, paramDesc);
        setTheDisplayUnit(u);
        setDoubleValue(123.0);
    }



    /**
     * Should we show the display unit widget in the properties dialog
     *
     * @return true
     */
    protected boolean showDisplayUnitInProperties() {
        return true;
    }


    /**
     * Get the label to show the user what I am in the properties
     *
     * @return The label
     */
    public String getLabel() {
        return "Value Symbol: " + getName();
    }



    /**
     * Get the parameter value at the particular index
     *
     * @param index  index into list of params
     * @return the value
     */
    public Object getParamValue(int index) {
        return Misc.format(value);
    }

    /**
     * Set the parameter value at the particular index
     *
     * @param index  index into list of params
     * @param v  the value
     */
    public void setParamValue(int index, Object v) {
        setDoubleValue(Misc.parseNumber(v.toString()));
    }


    /**
     * Get the value as a double
     * @return the double value
     */
    public double getDoubleValue() {
        return value;
    }

    /**
     * Set the double value
     *
     * @param value  the value
     */
    public void setDoubleValue(double value) {
        this.value = value;
        super.setValue(formatNumber(value));
    }


}


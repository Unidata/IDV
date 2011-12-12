/*
 * $Id: LabelSymbol.java,v 1.11 2007/05/22 20:00:21 jeffmc Exp $
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


import org.w3c.dom.Element;

import ucar.unidata.data.point.PointOb;
import ucar.unidata.ui.drawing.DisplayCanvas;


import ucar.unidata.ui.drawing.Glyph;


import ucar.unidata.util.GuiUtils;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapeUtility;


import visad.*;

import java.awt.*;

import java.awt.event.*;
import java.awt.geom.*;


import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Label symbol.
 * @author Metapps development team
 * @version $Revision: 1.11 $
 */
public class LabelSymbol extends TextSymbol {

    /** The shape. One of the fixed ones from ucar.visad.ShapeUtility */
    //    private String label;

    /** The text field */
    private JTextField labelField;

    /** Can this shape resize */
    boolean stretchy = true;

    /**
     * Default constructor.
     */
    public LabelSymbol() {}


    /**
     * Construct a LabelSymbol without a canvas at the
     * position specified.  Use the parameter names and long names specified.
     * @param x              x position
     * @param y              y position
     */
    public LabelSymbol(int x, int y) {
        this(null, x, y);
    }


    /**
     * Construct a LabelSymbol to use on the canvas specified at the
     * position specified.  Use the parameter names and long names specified.
     * @param canvas  canvas to draw on.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     */
    public LabelSymbol(DisplayCanvas canvas, int x, int y) {
        super(canvas, x, y, "Label", "label");
        setSize(20, 20);
    }


    /**
     * Add any components to the list of widgets for the main property dialog
     * tab.
     *
     * @param comps List of components.
     */
    protected void initPropertyComponents(List comps) {
        super.initPropertyComponents(comps);

        labelField = new JTextField(getValueString(), 20);
        comps.add(GuiUtils.rLabel("Label:"));
        comps.add(GuiUtils.left(labelField));
    }


    /**
     * Apply properties from the properties dialog.
     */
    protected void applyProperties() {
        if (labelField != null) {
            setValue(labelField.getText());
            setParamIds(getParamIds());
        }
        super.applyProperties();
    }

    /**
     * Get the parameter ids for this <code>MetSymbol</code>.
     * @return  array of parameter ids
     */
    public String[] getParamIds() {
        return new String[] { getValue() };
    }


    /**
     * Get whether this <code>MetSymbol</code> can be stretched or not.
     * @return true if can be stretched.
     */
    public boolean getStretchy() {
        return stretchy;
    }

    /**
     * Is this stretchy
     *
     * @param s Is this stretchy
     */
    public void setStretchy(boolean s) {
        stretchy = s;
    }



    /**
     * Get the parameter value at the index specified.
     * @param  index  index into param array
     * @return value of uOrSpeedParam if index = 0, otherwise vOrDirParam value
     */
    public Object getParamValue(int index) {
        return getValue();
    }

    /**
     * Get the value string
     * @return  a string representation of the size
     */
    protected String getValueString() {
        return getValue();
    }

    /**
     * Get whether this <code>MetSymbol</code> has equals sides
     * (width and height).
     * @return  true
     */
    public boolean getEqualSides() {
        return true;
    }

    /**
     * Should this symbol do all of the observations. Used by the
     * StationModelDisplayable to determine  if makeShapes should be called.
     *
     * @return Do all obs - true.
     */
    public boolean xxxdoAllObs() {
        return true;
    }

    /**
     * Make the shapes.
     *
     * @param ob The observation. We don't use this.
     *
     * @return The array of shapes.
     */
    public VisADGeometryArray[] xxxmakeShapes(PointOb ob) {
        String label = getValueString();
        if (label == null) {
            return null;
        }
        Font f = getFont();
        if (f == null) {
            f = makeDefaultFont();
        }
        return new VisADGeometryArray[] {
            ShapeUtility.shapeFont(label, f, true) };
    }

    /**
     * get the value string
     *
     * @return value string
     */
    public String getText() {
        return getValueString();
    }


    /**
     * Get a label for this symbol.
     * @return a human readable label
     */
    public String getLabel() {
        return "Label Symbol: " + getValueString();
    }

    /**
     * String representation of this object.
     * @return String version of this object.
     */
    public String toString() {
        return "LabelSymbol " + getValueString();
    }

}


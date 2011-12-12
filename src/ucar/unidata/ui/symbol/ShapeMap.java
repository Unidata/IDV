/**
 * $Id: ShapeMap.java,v 1.6 2007/05/22 20:00:23 jeffmc Exp $
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
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapeUtility;


import visad.*;

import java.awt.*;

import java.awt.event.*;
import java.awt.geom.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * This holds a shape type (from visad.ShapeUtility) to use for the
 * basea classes pattern
 * @author Metapps development team
 * @version $Revision: 1.6 $
 */
public class ShapeMap extends ColorMap {

    /** The shape */
    private String shape = "";

    /** For gui */
    private JComboBox shapeBox;

    /**
     * Default ctor
     */
    public ShapeMap() {}

    /**
     * ctor
     *
     * @param pattern pattern
     * @param shape shape
     * @param c color
     */
    public ShapeMap(String pattern, String shape, Color c) {
        super(pattern, c);
        this.shape = shape;
    }

    /**
     * Get widget
     *
     * @return widget
     */
    public JComponent getShapeWidget() {
        if (shapeBox == null) {
            shapeBox = new JComboBox(ShapeUtility.SHAPES);
            shapeBox.setEditable(false);
            TwoFacedObject tfo = new TwoFacedObject(shape, shape);
            int index          =
                Misc.toList(ShapeUtility.SHAPES).indexOf(tfo);
            if (index >= 0) {
                shapeBox.setSelectedIndex(index);
            } else {
                shapeBox.setSelectedItem(tfo);
            }
        }
        return shapeBox;
    }


    /**
     * Apply properties
     */
    public void applyProperties() {
        if (shapeBox != null) {
            shape = (String) TwoFacedObject.getIdString(
                shapeBox.getSelectedItem());
        }
        super.applyProperties();
    }


    /**
     *  Set the Shape property.
     *
     *  @param value The new value for Shape
     */
    public void setShape(String value) {
        shape = value;
    }

    /**
     *  Get the Shape property.
     *
     *  @return The Shape
     */
    public String getShape() {
        return shape;
    }



}


/**
 * $Id: ColorMap.java,v 1.6 2007/05/22 20:00:21 jeffmc Exp $
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

import java.rmi.RemoteException;
import java.awt.*;

import java.awt.event.*;
import java.awt.geom.*;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * This defines a mapping from a pattern to a color.
 * The pattern is the value of a text point obs field.
 * @author Metapps development team
 * @version $Revision: 1.6 $
 */
public class ColorMap {

    /** Pattern to match */
    private String pattern;

    /** Color to use */
    private Color color;

    /** For the gui */
    private JComponent[] swatchComps;

    /** For the gui */
    private JTextField patternFld;

    /** The range */
    private Real[] range;

    /**
     * Default ctor
     */
    public ColorMap() {
        pattern = "";
    }

    /**
     * ctor
     *
     * @param pattern The pattern
     * @param c The color
     */
    public ColorMap(String pattern, Color c) {
        this.pattern = pattern;
        if (this.pattern == null) {
            this.pattern = "";
        }
        this.color = color;
    }

    /**
     * Utility to call apply properties to a list of ColorMap-s
     * This will return the ones that actually have a pattern.
     * This way the properties dialog for the symbol can prune out the
     * ones that the user removed.
     *
     * @param mappings List of ColorMap-s
     *
     * @return The list of ColorMaps-s that actually have a pattern.
     */
    public static List applyProperties(List mappings) {
        List goodMappings = new ArrayList();
        for (int i = 0; i < mappings.size(); i++) {
            ColorMap colorMap = (ColorMap) mappings.get(i);
            colorMap.applyProperties();
            if (colorMap.hasPattern()) {
                goodMappings.add(colorMap);
            }
        }
        return goodMappings;
    }

    /**
     * Apply the properties
     */
    public void applyProperties() {
        range   = null;
        pattern = patternFld.getText().trim();
        GuiUtils.ColorSwatch colorSwatch =
            (GuiUtils.ColorSwatch) getSwatchComps()[0];
        color = colorSwatch.getSwatchColor();
    }

    /**
     * Get the color swatch components
     *
     * @return color swatch components
     */
    public JComponent[] getSwatchComps() {
        if (swatchComps == null) {
            swatchComps = GuiUtils.makeColorSwatchWidget(color, "");
        }
        return swatchComps;
    }

    /**
     * Make the color widget
     *
     * @return  color widget
     */
    public JComponent getColorWidget() {
        getSwatchComps();
        return GuiUtils.hbox(swatchComps[0], swatchComps[2]);
    }

    /**
     * Pattern widget
     *
     * @return Pattern widget
     */
    public JTextField getPatternWidget() {
        if (patternFld == null) {
            patternFld = new JTextField(pattern, 20);
            patternFld.setToolTipText("<html>A string pattern to match or a comma separated numeric range:<br><i>e.g., -5.0,0</i></html>");
        }
        return patternFld;
    }


    /**
     * Do we have a non-zero length, non-null pattern
     *
     * @return Has a pattern
     */
    public boolean hasPattern() {
        return (pattern != null) && (pattern.length() > 0);
    }

    /**
     * is the pattern string actually a numeric range of the form min,max
     *
     * @return is numeric range
     */
    public boolean isNumericRange() {
        return pattern.indexOf(",") >= 0;
    }



    /**
     * tokenize the pattern string as a numeric range
     *
     * @return min max range values
     *
     * @throws Exception On badness
     */
    public Real[] getNumericRange() throws Exception {
        if (range == null) {
            range = new Real[] { null, null };
            List toks = StringUtil.split(pattern, ",");
            if (toks.size() == 0) {
                throw new IllegalStateException(
                    "Bad format for numeric range:" + pattern);
            }
            String tok1 = toks.get(0).toString();
            String tok2 = ((toks.size() == 1)
                           ? tok1
                           : toks.get(1).toString());
            range[0] = ucar.visad.Util.toReal(tok1);
            range[1] = ucar.visad.Util.toReal(tok2);
        }
        return range;
    }



    /**
     * Does the pattern match the given value
     *
     * @param value value to check
     *
     * @return Pattern matches  value
     */
    public boolean match(Data value) throws Exception {
        if(value instanceof Real && isNumericRange()) {
            Real[]range = getNumericRange();
            Real r = (Real) value;
            if(r.__ge__(range[0])==0) return false;
            if(r.__le__(range[1])==0) return false;
            return true;
        }

        String stringValue = value.toString();
        return StringUtil.stringMatch(stringValue, pattern, true, true);
    }

    /**
     * Set the Pattern property.
     *
     * @param value The new value for Pattern
     */
    public void setPattern(String value) {
        pattern = value;
        range   = null;
    }

    /**
     * Get the Pattern property.
     *
     * @return The Pattern
     */
    public String getPattern() {
        return pattern;
    }


    /**
     * Set the Color property.
     *
     * @param value The new value for Color
     */
    public void setColor(Color value) {
        color = value;
    }

    /**
     * Get the Color property.
     *
     * @return The Color
     */
    public Color getColor() {
        return color;
    }



}


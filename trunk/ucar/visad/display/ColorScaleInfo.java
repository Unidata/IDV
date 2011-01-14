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


import ucar.unidata.ui.drawing.Glyph;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.StringUtil;

import java.awt.Color;
import java.awt.Font;

import java.util.List;


/**
 * Class to hold information about a ColorScale
 *
 * @author IDV Development Team
 * @version $Revision: 1.10 $
 */
public class ColorScaleInfo {

    /** Horizontal orientation */
    public static final int HORIZONTAL = ColorScale.HORIZONTAL_ORIENT;

    /** Vertical orientation */
    public static final int VERTICAL = ColorScale.VERTICAL_ORIENT;

    /** Upper Left Placement */
    public static final String TOP = ColorScale.TOP;

    /** Upper Right Placement */
    public static final String BOTTOM = ColorScale.BOTTOM;

    /** Lower Left Placement */
    public static final String LEFT = ColorScale.LEFT;

    /** Upper Left Placement */
    public static final String RIGHT = ColorScale.RIGHT;

    /** name of the scale */
    private String name;

    /** scale orientation */
    private int orient = HORIZONTAL;

    /** scale placement */
    private String placement = TOP;

    /** x position */
    private float x;

    /** y position */
    private float y;

    /** label color */
    private Color labelColor = null;

    /** color palette */
    private float[][] colorPalette = null;

    /** label font */
    private Font labelFont = null;

    /** label side */
    private int labelSide = ColorScale.PRIMARY;

    /** label font */
    private static Color defaultColor = Color.lightGray;

    /** visibility */
    private boolean isVisible = false;

    /** visibility of the labels */
    private boolean labelVisible = true;

    /** use alpha when drawing */
    private boolean useAlpha = false;

    /** This keeps track of whether we need to update the X, Y and orientation */
    private boolean dirty = true;

    /** default ctor */
    public ColorScaleInfo() {
        this("ColorScaleInfo");
    }

    /**
     * Create a ColorScaleInfo with a name
     *
     * @param name  the name
     */
    public ColorScaleInfo(String name) {
        this.name = name;
    }


    /**
     * Construct a new <code>ColorScaleInfo</code> with the given name
     * and orientation.
     * @param name    name for this color scale object.
     * @param orient  orientation for this <code>ColorScale</code>
     */
    public ColorScaleInfo(String name, int orient) {
        this(name, orient, getDefaultPlace(orient));
    }

    /**
     * Construct a new <code>ColorScaleInfo</code> with the given name
     * and orientation and placement.
     * @param name    name for this color scale object.
     * @param orient  orientation for this <code>ColorScale</code>
     * @param placement  the placement
     */
    public ColorScaleInfo(String name, int orient, String placement) {
        this(name, orient, placement, (Font) null, (float[][]) null);
    }

    /**
     * Create a color scale information object with the given parameters.
     * @param name name of the ColorScale
     * @param orient  orientation (HORIZONTAL, VERTICAL)
     * @param palette  color palette (rgb values)
     */
    public ColorScaleInfo(String name, int orient, float[][] palette) {
        this(name, orient, getX(orient, getDefaultPlace(orient)),
             getY(orient, getDefaultPlace(orient)), (Font) null, palette);
    }

    /**
     * Create a color scale information object with the given parameters.
     *
     * @param name name of the ColorScale
     * @param orient  orientation (HORIZONTAL, VERTICAL)
     * @param x   x location (percent from left side of display)
     * @param y   y location (percent from top side of display)
     * @param labelFont  font used for labels
     * @param colorPalette  color palette (rgb values)
     */
    public ColorScaleInfo(String name, int orient, float x, float y,
                          Font labelFont, float[][] colorPalette) {
        this(name, orient, x, y, labelFont, colorPalette, null);
    }

    /**
     * Create a color scale information object with the given parameters.
     *
     * @param name name of the ColorScale
     * @param orient  orientation (HORIZONTAL, VERTICAL)
     * @param placement placement (TOP, LEFT, etc)
     * @param labelFont  font used for labels
     * @param colorPalette  color palette (rgb values)
     */
    public ColorScaleInfo(String name, int orient, String placement,
                          Font labelFont, float[][] colorPalette) {
        this(name, orient, getX(orient, placement), getY(orient, placement),
             labelFont, colorPalette, null);
    }

    /**
     * Create a color scale information object with the given parameters.
     *
     * @param name name of the ColorScale
     * @param orient  orientation (HORIZONTAL, VERTICAL)
     * @param x   x location (percent from left side of display)
     * @param y   y location (percent from top side of display)
     * @param labelFont  font used for labels
     * @param colorPalette  color palette (rgb values)
     * @param labelColor  color for labels
     */
    public ColorScaleInfo(String name, int orient, float x, float y,
                          Font labelFont, float[][] colorPalette,
                          Color labelColor) {
        this(name, orient, x, y, labelFont, colorPalette, labelColor, false);
    }

    /**
     * Create a color scale information object with the given parameters.
     *
     * @param name name of the ColorScale
     * @param orient  orientation (HORIZONTAL, VERTICAL)
     * @param x   x location (percent from left side of display)
     * @param y   y location (percent from top side of display)
     * @param labelFont  font used for labels
     * @param colorPalette  color palette (rgb values)
     * @param labelColor  color for labels
     * @param useAlpha  true to use alpha when drawing color scale
     */
    public ColorScaleInfo(String name, int orient, float x, float y,
                          Font labelFont, float[][] colorPalette,
                          Color labelColor, boolean useAlpha) {
        this.name         = name;
        this.orient       = orient;
        this.x            = x;
        this.y            = y;
        this.labelFont    = labelFont;
        this.colorPalette = colorPalette;
        this.labelColor   = labelColor;
        this.useAlpha     = useAlpha;
    }

    /**
     * Create a color scale information object from another
     *
     * @param that  the other ColorScaleInfo
     */
    public ColorScaleInfo(ColorScaleInfo that) {
        that.checkState();
        this.name         = that.name;
        this.orient       = that.orient;
        this.placement    = that.placement;
        this.x            = that.x;
        this.y            = that.y;
        this.labelFont    = that.labelFont;
        this.colorPalette = that.colorPalette;
        this.labelColor   = that.labelColor;
        this.labelSide    = that.labelSide;
        this.isVisible    = that.isVisible;
        this.labelVisible = that.labelVisible;
        this.useAlpha     = that.useAlpha;
    }

    /**
     * Create a color scale information object from the given param string
     *
     * @param params the param string.  see getParamStringFormat for details
     * @param isParamString  true if this is a param string
     */
    public ColorScaleInfo(String params, boolean isParamString) {
        dirty = true;
        List<String> toks = StringUtil.split(params, ";", true, true);
        for (String pair : toks) {
            List subToks = StringUtil.split(pair, "=");
            if (subToks.size() != 2) {
                throw new IllegalArgumentException(
                    "Bad color scale info info format: " + params);
            }
            String name  = subToks.get(0).toString().trim();
            String value = subToks.get(1).toString().trim();
            if (name.equals("visible")) {
                this.isVisible = new Boolean(value).booleanValue();
            } else if (name.equals("labelvisible")) {
                this.labelVisible = new Boolean(value).booleanValue();
            } else if (name.equals("name")) {
                this.name = name;
            } else if (name.equals("color")) {
                this.labelColor =
                    ucar.unidata.util.GuiUtils.decodeColor(value,
                        this.labelColor);
            } else if (name.equals("orientation")) {
                if (value.equals("horizontal")) {
                    this.orient = HORIZONTAL;
                } else if (value.equals("vertical")) {
                    this.orient = VERTICAL;
                } else {
                    throw new IllegalArgumentException("Unknown orientation:"
                            + value);
                }
            } else if (name.equals("placement")) {
                if (value.equals("top")) {
                    this.placement = TOP;
                } else if (value.equals("bottom")) {
                    this.placement = BOTTOM;
                } else if (value.equals("left")) {
                    this.placement = LEFT;
                } else if (value.equals("right")) {
                    this.placement = RIGHT;
                } else {
                    throw new IllegalArgumentException("Unknown placement:"
                            + value);
                }
            } else {
                throw new IllegalArgumentException("Unknown ColorScaleInfo:"
                        + name);
            }

        }

    }

    /**
     * Get the param string format
     *
     * @return the param string format
     */
    public static String getParamStringFormat() {
        return "visible=true|false;color=somecolor;orientation=horizontal|vertical;placement=top|left|bottom|right;labelvisible=true";
    }

    /**
     * Set the name of the color scale.
     *
     * @param name  name to use
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of the color scale.
     *
     * @return name of color scale.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the orientation of the color scale.
     *
     * @param orient orientation to use
     */
    public void setOrientation(int orient) {
        dirty       = true;
        this.orient = orient;
    }

    /**
     * Get the orientation of the color scale.
     *
     * @return orientation of color scale.
     */
    public int getOrientation() {
        checkState();
        return orient;
    }

    /**
     * Set the placement of the color scale.
     *
     * @param place placement to use (e.g. TOP)
     */
    public void setPlacement(String place) {
        this.placement = place;
        dirty          = true;
    }


    /**
     * If we are dirty then set the orientation, x, y and labelSide (which all depend on the placement
     */
    private void checkState() {
        if (dirty) {
            orient    = ColorScale.getDefaultOrient(placement);
            x         = getX(orient, placement);
            y         = getY(orient, placement);
            labelSide = placement.equals(RIGHT)
                        ? ColorScale.SECONDARY
                        : ColorScale.PRIMARY;
        }
        dirty = false;
    }

    /**
     * Get the placment of the color scale.
     *
     * @return placement of color scale.
     */
    public String getPlacement() {
        return placement;
    }

    /**
     * Set the x position of the color scale.
     *
     * @param x  x position to use
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Get the x position of the color scale.
     *
     * @return x position of color scale.
     */
    public float getX() {
        checkState();
        return x;
    }

    /**
     * Set the y position of the color scale.
     *
     * @param y  y position to use
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Get the y position of the color scale.
     *
     * @return y position of color scale.
     */
    public float getY() {
        checkState();
        return y;
    }

    /**
     * Set the label <code>Font</code> of the color scale.
     *
     * @param font  font to use
     */
    public void setLabelFont(Font font) {
        this.labelFont = font;
    }

    /**
     * Get the label <code>Font</code> of the color scale.
     *
     * @return label Font of color scale.
     */
    public Font getLabelFont() {
        return labelFont;
    }

    /**
     * Set the color palette of the color scale.
     *
     * @param colorPalette  color palette to use
     */
    public void setColorPalette(float[][] colorPalette) {
        this.colorPalette = colorPalette;
    }

    /**
     * Get the color palette of the color scale.
     *
     * @return color palette color scale.
     */
    public float[][] getColorPalette() {
        return colorPalette;
    }

    /**
     * Set the label <code>Color</code> of the color scale.
     *
     * @param color  Color to use
     */
    public void setLabelColor(Color color) {
        this.labelColor = color;
    }

    /**
     * Get the label <code>Font</code> of the color scale.
     *
     * @return label Font of color scale.
     */
    public Color getLabelColor() {
        return labelColor;
    }

    /**
     * Set the labelling side.
     *
     * @param side labelling side (PRIMARY, SECONDARY);
     */
    public void setLabelSide(int side) {
        labelSide = side;
    }

    /**
     * Get the color of the labels
     * @return label color
     */
    public int getLabelSide() {
        checkState();
        return labelSide;
    }

    /**
     * Set the visibility
     *
     * @param show   true to be visible
     */
    public void setIsVisible(boolean show) {
        isVisible = show;
    }

    /**
     * Get the visibility
     * @return visibility
     */
    public boolean getIsVisible() {
        return isVisible;
    }

    /**
     * Set the label visibility
     *
     * @param show   true to be label visible
     */
    public void setLabelVisible(boolean show) {
        labelVisible = show;
    }

    /**
     * Get the label visibility
     * @return label visibility
     */
    public boolean getLabelVisible() {
        return labelVisible;
    }

    /**
     *     Gets the useAlpha property
     *    
     *     @return the useAlpha
     */
    public boolean getUseAlpha() {
        return useAlpha;
    }

    /**
     * Sets the useAlpha property
     *
     * @param useAlpha the useAlpha to set
     */
    public void setUseAlpha(boolean useAlpha) {
        this.useAlpha = useAlpha;
    }

    /**
     * Get the default place for the given orientation
     *
     * @param orient  orientation
     *
     * @return  the default place for the orientation
     */
    private static String getDefaultPlace(int orient) {
        return ColorScale.getDefaultPlace(orient);
    }

    /**
     * Get the X position
     *
     * @param orient orientation
     * @param placement  placement
     *
     * @return corresponding X position
     */
    private static float getX(int orient, String placement) {
        return ColorScale.getX(orient, placement);
    }

    /**
     * Get the Y position
     *
     * @param orient orientation
     * @param placement  placement
     *
     * @return corresponding Y position
     */
    private static float getY(int orient, String placement) {
        return ColorScale.getY(orient, placement);
    }


    /**
     * To string
     *
     * @return To string
     */
    public String toString() {
        return "placement:" + placement + " orient:" + orient
               + " label side: " + labelSide + " x/y:" + x + "/" + y;
    }

}

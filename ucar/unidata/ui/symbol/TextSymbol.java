/*
 * $Id: TextSymbol.java,v 1.41 2007/05/22 20:00:23 jeffmc Exp $
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

import visad.DateTime;
import visad.Real;
import visad.Unit;

import java.awt.*;
import java.awt.geom.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;


/**
 * A representation of a text MetSymbol.
 * @author Metapps development team
 * @version $Revision: 1.41 $
 */
public class TextSymbol extends MetSymbol {

    /** default font name */
    private static String FONTNAME = "monospaced PLAIN ";

    /** list of fonts */
    private static Font[] fonts = null;

    /** FontMetrics */
    private FontMetrics fontMetrics;

    /** current font number */
    private int currFontNo = 4;

    /** current font size */
    private int currFontSize = 12;

    /** current Font */
    private Font currFont = null;

    /** number format */
    private NumberFormat numberFormat;

    /** value string */
    private String valStr = "";

    /**
     * Format string for numbers.
     */
    protected String numberFormatString = "###0.#";

    /**
     * Array of default font sizes used for incrementing/decrementing
     * fonts.
     * @see #incrFontSize
     * @see #decrFontSize
     */
    public final static String[] FONT_SIZES = {
        "8", "9", "10", "11", "12", "13", "14", "15", "16", "18", "20", "24",
        "26", "28", "32", "36", "40", "48", "56", "64", "72"
    };

    /** list of font sizes */
    private final static int[] fontSizes = {
        8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 24, 26, 28, 32, 36, 40, 48,
        56, 64, 72
    };

    /**
     * Default constructor.
     */
    public TextSymbol() {}

    /**
     * Construct a TextSymbol at the position specified.
     * Use the default name and long name.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     */
    public TextSymbol(int x, int y) {
        this(null, x, y, "param", "Parameter");
    }

    /**
     * Construct a TextSymbol to use on the canvas specified at the
     * position specified.  Use the default name and long name.
     * @param canvas  canvas to draw on.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     */
    public TextSymbol(DisplayCanvas canvas, int x, int y) {
        this(canvas, x, y, "param", "Parameter");
    }

    /**
     * Construct a TextSymbol with the name specified.
     * @param param  parameter to display
     * @param paramDesc  description of the parameter (i.e., long name)
     */
    public TextSymbol(String param, String paramDesc) {
        this(0, 0, param, paramDesc);
    }

    /**
     * Construct a TextSymbol at the position specified.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     * @param param  parameter to display
     * @param paramDesc  description of the parameter (i.e., long name)
     */
    public TextSymbol(int x, int y, String param, String paramDesc) {
        this(null, x, y, param, paramDesc);
    }

    /**
     * Construct a TextSymbol to use on the canvas specified at the
     * position specified.
     * @param canvas  canvas to draw on.
     * @param x  x position on the canvas
     * @param y  y position on the canvas
     * @param param  parameter to display
     * @param paramDesc  description of the parameter (i.e., long name)
     */
    public TextSymbol(DisplayCanvas canvas, int x, int y, String param,
                      String paramDesc) {
        super(canvas, x, y, new String[] { param },
              new String[] { paramDesc });
        setStretchy(false);
        calculateBounds();
    }


    /**
     * Get the label to show the user what I am in the properties
     *
     * @return label
     */
    public String getLabel() {
        return "Text Symbol: " + getName();
    }

    /**
     * Set the font to use when displaying this symbol.
     * @param font  font to use.
     */
    public void setFont(Font font) {
        currFont = font;
        setFontSize((font == null)
                    ? currFontSize
                    : font.getSize());

    }

    /**
     * Get the currently used font.
     * @return  current font.  May be <code>null</code>
     */
    public Font getFont() {
        return currFont;
    }

    /**
     * Increase the font size by one step in the FONT_SIZES array.
     */
    public void incrFontSize() {
        currFontNo++;
        if (currFontNo >= fontSizes.length) {
            currFontNo = fontSizes.length - 1;
        }
        setFontSize(fontSizes[currFontNo]);
    }

    /**
     * Decrease the font size by one step in the fontSizes array.
     */
    public void decrFontSize() {
        currFontNo--;
        if (currFontNo < 0) {
            currFontNo = 0;
        }
        setFontSize(fontSizes[currFontNo]);
    }

    /**
     * Get a list of fonts to use with this object.
     * @return array of fonts
     */
    public static Font[] getFontList() {
        if (fonts == null) {
            GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            fonts = ge.getAllFonts();
        }
        return fonts;
    }

    /**
     * Set the index into the array of font sizes.
     * @param index  index into the <code>FONT_SIZES</code> array
     */
    public void setCurrFontNo(int index) {
        currFontNo = (index > fontSizes.length)
                     ? fontSizes.length - 1
                     : (index < 0)
                       ? 0
                       : index;
        setFontSize(fontSizes[currFontNo]);
    }

    /**
     * Get the current font number
     * @return index of current font in the <code>FONT_SIZES</code> array.
     */
    public int getCurrFontNo() {
        return currFontNo;
    }

    /**
     * Set the font metrics for this object based on the font and font size.
     */
    private void setFontMetrics() {
        if (currFont != null) {
            fontMetrics =
                Toolkit.getDefaultToolkit().getFontMetrics(currFont);
        } else {
            fontMetrics =
                Toolkit.getDefaultToolkit().getFontMetrics(makeDefaultFont());
        }
    }

    /**
     * Get the metrics of the current font.
     * @return current font metrics.
     */
    public FontMetrics getFontMetrics() {
        if (fontMetrics == null) {
            setFontMetrics();
        }
        return fontMetrics;
    }

    /**
     * Set the font size.
     * @param  size  point size of font.
     */
    public void setFontSize(int size) {
        currFontSize = size;
        currFontNo   = java.util.Arrays.binarySearch(fontSizes, size);
        if ((currFont != null) && (currFontSize != currFont.getSize())) {
            currFont = currFont.deriveFont((float) currFontSize);
        }
        setFontMetrics();
        calculateBounds();
    }

    /**
     * Get the current font size.
     * @return currentFontSize
     */
    public int getFontSize() {
        return currFontSize;
    }

    /**
     * Calculate the bounds of this object.
     */
    private void calculateBounds() {
        if (valStr == null) {
            bounds.width  = 0;
            bounds.height = 0;
            return;
        }
        FontMetrics fm = getFontMetrics();
        bounds.width  = fm.stringWidth(getValueString());
        bounds.height = (fm.getMaxDescent() + fm.getMaxAscent());
    }

    /**
     * Get the value string
     * @return  a string representation of the size
     */
    protected String getValueString() {
        String v = null;
        if ((valStr != null) && (valStr.length() > 0)) {
            v = valStr;
        }
        if (v == null) {
            String[] ids = getParamIds();
            if (ids != null) {
                v = ids[0];
            }
        }
        if ((v == null) || (v.length() == 0)) {
            v = "";
        }
        return v;
    }



    /**
     * Get the format pattern for numeric values.
     * @return formatting pattern string.
     * @see java.text.DecimalFormat
     */
    public String getNumberFormatString() {
        return numberFormatString;
    }

    /**
     * Set the format pattern for numeric values.
     * @param s  formatting pattern string.
     * @see java.text.DecimalFormat
     */
    public void setNumberFormatString(String s) {
        numberFormatString = s;
        numberFormat       = null;
    }


    /**
     * Format a number using the format pattern set for this instance.
     * @param d  double to format
     * @return formatted number as a String
     * @see #setNumberFormatString(String)
     */
    public String formatNumber(double d) {
        if (numberFormat == null) {
            numberFormat = new DecimalFormat(numberFormatString);
        }
        return numberFormat.format(d);
    }

    /**
     * Format a Real
     *
     * @param d    the Real
     * @return the formatted value
     */
    public String format(Real d) {
        String fmtString = null;
        try {
            if (d instanceof DateTime) {
                fmtString = ((DateTime) d).toString();
            } else {
                fmtString = formatNumber(d.getValue());
            }
        } catch (Exception exc) {
            return "error: " + exc;
        }
        return fmtString;
    }


    /**
     * Format a double values
     *
     * @param d  the double
     * @return formatted number
     */
    public String format(double d) {
        return formatNumber(d);
    }

    /**
     * Format a float
     *
     * @param d  float
     * @return the formatted number
     */
    public String format(float d) {
        return formatNumber((double) d);

    }

    /**
     * Get the string representation for the value of this object.
     * @return String representation of number.
     */
    public String getValue() {
        return valStr;
    }

    /**
     * Set the value for this object.
     * @param d  value
     */
    public void setValue(double d) {
        setValue(formatNumber(d));
    }

    /**
     * Set the value for this object.
     * @param s  String representation of the value for this object.
     */
    public void setValue(String s) {
        valStr = s;
        calculateBounds();
    }

    /**
     * Get the parameter value for the index specified.
     * @param index   index of the parameter (ignored for this instance)
     * @return  String value associated with this instance.
     */
    public Object getParamValue(int index) {
        return valStr;
    }

    /**
     * Set the parameter value for the index specified.
     * @param index   index of the parameter (ignored for this instance)
     * @param v   <code>Object</code> that this should represent
     */
    public void setParamValue(int index, Object v) {
        setValue(v.toString());
    }


    /**
     * Draw this object to the graphics device.
     * @param g  Graphics object
     * @param x  x position of the object
     * @param y  y position of the object
     * @param width  width to draw
     * @param height  height to draw
     */
    public void draw(Graphics2D g, int x, int y, int width, int height) {
        g.setFont((currFont != null)
                  ? getFont()
                  : makeDefaultFont());
        g.setColor(getForeground());
        g.drawString(getValueString(), x, y + height);
    }

    /**
     * String representation of this object.
     * @return String version of this object.
     */
    public String toString() {
        return "TextSymbol " + valStr;
    }

    /**
     * Make a font from the size and using the default.
     * @return a default Font
     */
    protected Font makeDefaultFont() {
        return Font.decode(FONTNAME + getFontSize());
    }
}


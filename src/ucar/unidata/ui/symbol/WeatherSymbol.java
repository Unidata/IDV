/*
 * $Id: WeatherSymbol.java,v 1.20 2007/05/22 20:00:23 jeffmc Exp $
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
import ucar.unidata.util.GuiUtils;

import visad.VisADLineArray;



import visad.meteorology.WeatherSymbols;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.ImageObserver;


/**
 * Glyphs representing Meteorological symbols.
 *
 * @author Metapps development team
 * @version $Revision: 1.20 $
 */

public class WeatherSymbol extends MetSymbol implements ImageObserver {

    //These int values have to correspond  to the index
    //in the SYMBOLNAMES array

    /** Cloud coverage symbol index */
    public static final int SYMBOL_CLOUDCOVERAGE = 0;

    /** Present weather symbol index */
    public static final int SYMBOL_PRESENTWEATHER = 1;

    /** Low cloud symbol index */
    public static final int SYMBOL_LOWCLOUD = 2;

    /** Mid cloud symbol index */
    public static final int SYMBOL_MIDCLOUD = 3;

    /** High cloud symbol index */
    public static final int SYMBOL_HIGHCLOUD = 4;

    /** Pressure tendency symbol index */
    public static final int SYMBOL_PRESSURETENDENCY = 5;

    /** Icing symbol index */
    public static final int SYMBOL_ICING = 6;

    /** Turbulence symbol index */
    public static final int SYMBOL_TURBULENCE = 7;

    /** Lightning symbol index */
    public static final int SYMBOL_LIGHTNING = 8;

    /** Miscellaneous symbol index */
    public static final int SYMBOL_MISC = 9;

    /** All symbols index */
    public static final int SYMBOL_ALL = 10;


    /** Symbol names */
    public static final String[] SYMBOLNAMES = {
        "cloudcoverage", "presentweather", "lowcloud", "midcloud",
        "highcloud", "pressuretendency", "icing", "turbulence", "lightning",
        "all"
    };


    /** Symbol type attribute */
    public static final String ATTR_SYMBOLTYPE = "symboltype";

    /** The image that corresponds to the symbol group */
    private Image image = null;

    /** Which symbol type */
    private int symbolType = SYMBOL_PRESENTWEATHER;

    /** The example value that maps into the set of symbols for the symbol group we represent */
    private int code = 0;

    /** For drawing */
    private VisADLineArray lines;

    /**
     * Default constructor.
     */
    public WeatherSymbol() {}


    /**
     * Create a <code>WeatherSymbol</code> at the position indicated.
     * @param x   x coordinate
     * @param y   y coordinate
     */
    public WeatherSymbol(int x, int y) {
        this(null, x, y);
    }

    /**
     * Create a <code>WeatherSymbol</code> on the canvas specified
     * at the position indicated.
     * @param canvas   canvas for displaying this symbol
     * @param x        x coordinate
     * @param y        y coordinate
     */
    public WeatherSymbol(DisplayCanvas canvas, int x, int y) {
        this(canvas, x, y, "CC", "Parameter");
    }

    /**
     * Create a <code>WeatherSymbol</code> at the position indicated and
     * use the name and description supplied.
     * @param x          x coordinate
     * @param y          y coordinate
     * @param param      parameter name
     * @param paramDesc  parameter description (used in labels, widgets)
     */
    public WeatherSymbol(int x, int y, String param, String paramDesc) {
        this(null, x, y, param, paramDesc);
    }

    /**
     * Create a <code>WeatherSymbol</code> on <code>canvas</code>
     * at the position indicated and use the name and description supplied.
     * @param canvas   canvas for displaying this symbol
     * @param x          x coordinate
     * @param y          y coordinate
     * @param param      parameter name
     * @param paramDesc  parameter description (used in labels, widgets)
     */
    public WeatherSymbol(DisplayCanvas canvas, int x, int y, String param,
                         String paramDesc) {
        super(canvas, x, y, new String[] { param },
              new String[] { paramDesc });
        setSize(20, 20);
    }

    /**
     * Get the label to show the user
     *
     * @return The label
     */
    public String getLabel() {
        return "Weather Symbol: " + SYMBOLNAMES[symbolType];
    }


    /**
     * This method is called when information about an image which
     * was previously requested using an asynchronous interface
     * becomes available, public due do implementing
     * {@link ImageObserver}.
     * @param img     image being observed
     * @param flags   information flags
     * @param x       x coordinate
     * @param y       y coordinate
     * @param width   the width
     * @param height  the height
     * @return <code>false</code> if the infoflags indicate that the
     *         image is completely loaded; <code>true</code> otherwise.
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {
        if ((flags & ImageObserver.ERROR) != 0) {
            return false;
        }
        return true;
    }


    /**
     * Set the type of symbol that this is.
     *
     * @param value  symbol type (e.g., SYMBOL_LOWCLOUD)
     */
    public void setSymbolType(int value) {
        symbolType = value;
        image      = null;
        lines      = null;
    }

    /**
     * Get the image associated with the symbol type
     * @return The image
     */
    private Image getImage() {
        if ((image == null) && (symbolType < SYMBOLNAMES.length)
                && (symbolType >= 0)) {
            image = GuiUtils.getImage("/ucar/unidata/ui/symbol/images/"
                                      + SYMBOLNAMES[symbolType] + ".gif");
        }
        return image;
    }

    /**
     * Get the line array for drawing me in the canvas.
     * Use the example code value.
     * @return line array
     */
    private VisADLineArray getMyLines() {
        if (lines == null) {
            lines = getLines(symbolType, code);
        }
        return lines;
    }


    /**
     * Gets the VisADLineArray that corresponds to the index for the
     * symbol type.
     * @param index   index for this symbolType.
     * @return  VisADLineArray corresponding to this symbol
     * @see #getLines(int, int)
     */
    public VisADLineArray getLines(int index) {
        return getLines(symbolType, index);
    }

    /**
     * Gets the VisADLineArray that corresponds to the code for the
     * symbol type.
     * @param symbolType   type of symbol
     * @param code         code for the particular item in this symbol type
     * @return  VisADLineArray corresponding to this symbol
     * @see #getLines(int, int)
     */
    public static VisADLineArray getLines(int symbolType, int code) {
        try {
            switch (symbolType) {

            case SYMBOL_PRESENTWEATHER :
                return (code > 3)
                    ? WeatherSymbols.getPresentWeatherSymbol(code)
                    : null;

            case SYMBOL_LOWCLOUD :
                if (code < 1 ||code > WeatherSymbols.LOCLD_NUM) return null;
                return WeatherSymbols.getLowCloudSymbol(code);

            case SYMBOL_MIDCLOUD :
                if (code < 1 ||code > WeatherSymbols.MIDCLD_NUM) return null;
                return WeatherSymbols.getMidCloudSymbol(code);

            case SYMBOL_HIGHCLOUD :
                if (code < 1 ||code > WeatherSymbols.HICLD_NUM) return null;
                return WeatherSymbols.getHighCloudSymbol(code);

            case SYMBOL_PRESSURETENDENCY :
                if (code < 0 || code >= WeatherSymbols.TNDCY_NUM) return null;
                return WeatherSymbols.getPressureTendencySymbol(code);

            case SYMBOL_CLOUDCOVERAGE :
                if (code < 0 || code >= WeatherSymbols.SKY_NUM) return null;
                return WeatherSymbols.getCloudCoverageSymbol(code);

            case SYMBOL_ICING :
                if (code < 0 || code >= WeatherSymbols.ICING_NUM) return null;
                return WeatherSymbols.getIcingSymbol(code);

            case SYMBOL_TURBULENCE :
                if (code < 0 || code >= WeatherSymbols.TURB_NUM) return null;
                return WeatherSymbols.getTurbulenceSymbol(code);

            case SYMBOL_LIGHTNING :
                return (code >= 0)
                    ? WeatherSymbols.getLightningSymbol(0)
                    : WeatherSymbols.getLightningSymbol(1);

            case SYMBOL_MISC :
                if (code < 0) {
                    return null;
                }
                //            if(code < 0)
                //          return WeatherSymbols.getMiscSymbol (0);
                return WeatherSymbols.getMiscSymbol(code);

            case SYMBOL_ALL : {
                VisADLineArray[] vla = WeatherSymbols.getAllMetSymbols();
                if ((code < 0) || (code >= vla.length)) {
                    return null;
                }
                return vla[code];
            }

            default :
                throw new IllegalStateException("Unknown symbol type:"
                                                + symbolType);
            }

        } catch(IllegalArgumentException iae) {
            //degrade a bit gracefully
            System.err.println("bad value: " + code +" for symbol:" + symbolType);
        }
        return null;
    }


    /**
     * Get the value for the parameter at the particular index.  Since
     * this objection only has one value, it is returned.
     * @param index   parameter index (ignored)
     * @return  symbol code for this symbolType as an <code>Integer</code>
     */
    public Object getParamValue(int index) {
        return new Integer(code);
    }

    /**
     * Set the value for the parameter at the particular index.  Since
     * this objection only has one value, it is set with the input.
     * @param index   parameter index (ignored)
     * @param v       value (String version of integer code)
     */
    public void setParamValue(int index, Object v) {
        setCode(new Integer(v.toString()).intValue());
    }


    /**
     * Get the symbol type for this <code>WeatherSymbol</code>.  Used
     * primarily for persistence.
     * @return symbol type (e.g., SYMBOL_LOWCLOUD)
     */
    public int getSymbolType() {
        return symbolType;
    }

    /**
     * Set the code (index) for this symbol type.  Used primarily for
     * XML persistence.
     * @param value   code for the index for this symbol type.
     * @see visad.meteorology.WeatherSymbols
     */
    public void setCode(int value) {
        code = value;
    }

    /**
     * Get the code (index) for this symbol type.  Used primarily for
     * XML persistence.
     * @return code for the index for this symbol type.
     * @see visad.meteorology.WeatherSymbols
     */
    public int getCode() {
        return code;
    }

    /**
     * Get whether this <code>MetSymbol</code> can be stretched or not.
     * @return true if can be stretched.
     */
    public boolean getStretchy() {
        return !getBeingCreated();
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
     * Get whether this <code>MetSymbol</code> should be stretched
     * symetrically or not.
     * @return true
     */
    public boolean getSymetricReshape() {
        return true;
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
        g.drawImage(getImage(), x, y, width, height, this);
        g.setColor(getForeground());
        g.drawRect(x, y, width, height);
    }

    /**
     * Set the attribute with the value supplied.
     * @param name   name of attribute.
     * @param value  value of attribute.
     */
    public void setAttr(String name, String value) {
        if (name.equals(ATTR_SYMBOLTYPE)) {
            for (int i = 0; i < SYMBOLNAMES.length; i++) {
                if (SYMBOLNAMES[i].equals(value)) {
                    setSymbolType(i);
                    return;
                }
            }
            throw new IllegalArgumentException("Unknown symbol:" + name);
        } else {
            super.setAttr(name, value);
        }
    }

}


/*
 * $Id: LatLonData.java,v 1.6 2006/10/26 19:30:50 dmurray Exp $
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





package ucar.unidata.gis.maps;


import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Resource;

import ucar.visad.MapFamily;

import ucar.visad.display.*;

import visad.*;




import java.awt.Color;

import java.net.URL;

import java.rmi.RemoteException;



/**
 * A data structure to hold display attributes for  lat/lon lines
 */


public class LatLonData {

    /** The lat lon lines. We create this when asked. */
    protected LatLonLines myLatLon;

    /** Is the lat/lon visible */
    private boolean visible = true;

    /** The color */
    private Color color = Color.blue;

    /** The line spacing */
    private float spacing;

    /** The line width */
    private float lineWidth;

    /** The line style */
    private int lineStyle;

    /** Is this data representing latitude or longitude */
    private boolean isLatitude = true;


    /** The lat/lon min value */
    private float minValue;


    /** The lat/lon max value */
    private float maxValue;


    /** fast rendering flag */
    protected boolean fastRendering = false;

    /**
     * Default ctor
     */
    public LatLonData() {}


    /**
     * The ctro
     *
     * @param isLatitude Is it lat or lon
     * @param color The color
     * @param defaultSpacing The spacing
     * @param lineWidth The line width
     * @param lineStyle The line style
     *
     */
    public LatLonData(boolean isLatitude, Color color, float defaultSpacing,
                      float lineWidth, int lineStyle) {
        this(isLatitude, color, defaultSpacing, lineWidth, lineStyle, false);
    }

    /**
     * The ctro
     *
     * @param isLatitude Is it lat or lon
     * @param color The color
     * @param defaultSpacing The spacing
     * @param lineWidth The line width
     * @param lineStyle The line style
     * @param fastRendering true to use fast rendering
     *
     */
    public LatLonData(boolean isLatitude, Color color, float defaultSpacing,
                      float lineWidth, int lineStyle, boolean fastRendering) {
        this.isLatitude    = isLatitude;
        this.color         = color;
        this.spacing       = defaultSpacing;
        this.lineWidth     = lineWidth;
        this.lineStyle     = lineStyle;
        this.fastRendering = fastRendering;
    }


    /**
     * Copy constructor
     *
     * @param that the other latlondata object
     */
    protected LatLonData(LatLonData that) {
        initWith(that);
    }


    /**
     * Initialize this object with the state from the given LatLonData
     *
     * @param that The other object
     */
    public void initWith(LatLonData that) {
        if (that == null) {
            return;
        }
        this.isLatitude    = that.isLatitude;
        this.color         = that.color;
        this.spacing       = that.spacing;
        this.lineWidth     = that.lineWidth;
        this.lineStyle     = that.lineStyle;
        this.maxValue      = that.maxValue;
        this.minValue      = that.minValue;
        this.visible       = that.visible;
        this.fastRendering = that.fastRendering;
    }


    /**
     *  Gets called when any of the state has been changed.
     *  Is here so a subclass can be easily notified when something changes.
     */
    protected void stateChanged() {}


    /**
     * Create, if needed, initialize and return the latlonlines object
     * @return The {@link ucar.visad.display.LatLonLines} object
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public LatLonLines getLatLonLines()
            throws VisADException, RemoteException {
        if (myLatLon == null) {
            myLatLon = new LatLonLines((isLatitude
                                        ? RealType.Latitude
                                        : RealType.Longitude), minValue,
                                        maxValue, spacing,
                                        getRealVisibility());

        }
        if (color != null) {
            myLatLon.setColor(color);
        }
        myLatLon.setVisible(getRealVisibility());
        myLatLon.setLineStyle(lineStyle);
        myLatLon.setLineWidth(lineWidth);
        myLatLon.setSpacing(spacing);
        myLatLon.setUseFastRendering(fastRendering);
        return myLatLon;
    }

    /**
     * Meant to be overwrote by a derived class that needs to
     * determine its own visibility.
     *
     * @return The actual visibility to apply to the lines
     */
    protected boolean getRealVisibility() {
        return visible;
    }



    /**
     *  Set the Color property.
     *
     *  @param value The new value for Color
     */
    public void setColor(Color value) {
        color = value;
        stateChanged();
    }

    /**
     *  Get the Color property.
     *
     *  @return The Color
     */
    public Color getColor() {
        return color;
    }

    /**
     *  Set the Spacing property.
     *
     *  @param value The new value for Spacing
     */
    public void setSpacing(float value) {
        spacing = value;
        stateChanged();
    }

    /**
     *  Get the Spacing property.
     *
     *  @return The Spacing
     */
    public float getSpacing() {
        return spacing;
    }

    /**
     *  Set the LineWidth property.
     *
     *  @param value The new value for LineWidth
     */
    public void setLineWidth(float value) {
        lineWidth = value;
        stateChanged();
    }

    /**
     *  Get the LineWidth property.
     *
     *  @return The LineWidth
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     *  Set the LineStyle property.
     *
     *  @param value The new value for LineStyle
     */
    public void setLineStyle(int value) {
        lineStyle = value;
        stateChanged();
    }

    /**
     *  Get the LineStyle property.
     *
     *  @return The LineStyle
     */
    public int getLineStyle() {
        return lineStyle;
    }


    /**
     *  Set the IsLatitude property.
     *
     *  @param value The new value for IsLatitude
     */
    public void setIsLatitude(boolean value) {
        isLatitude = value;
    }

    /**
     *  Get the IsLatitude property.
     *
     *  @return The IsLatitude
     */
    public boolean getIsLatitude() {
        return isLatitude;
    }


    /**
     *  Set the MinValue property.
     *
     *  @param value The new value for MinValue
     */
    public void setMinValue(float value) {
        minValue = value;
    }

    /**
     *  Get the MinValue property.
     *
     *  @return The MinValue
     */
    public float getMinValue() {
        return minValue;
    }

    /**
     *  Set the MaxValue property.
     *
     *  @param value The new value for MaxValue
     */
    public void setMaxValue(float value) {
        maxValue = value;
    }

    /**
     *  Get the MaxValue property.
     *
     *  @return The MaxValue
     */
    public float getMaxValue() {
        return maxValue;
    }

    /**
     *  Set the Visible property.
     *
     *  @param value The new value for Visible
     */
    public void setVisible(boolean value) {
        visible = value;
        stateChanged();
    }

    /**
     *  Get the Visible property.
     *
     *  @return The Visible
     */
    public boolean getVisible() {
        return visible;
    }


    /**
     *  Set the FastRendering property.
     *
     *  @param value The new value for FastRendering
     */
    public void setFastRendering(boolean value) {
        fastRendering = value;
        stateChanged();
    }

    /**
     *  Get the FastRendering property.
     *
     *  @return The FastRendering
     */
    public boolean getFastRendering() {
        return fastRendering;
    }



}


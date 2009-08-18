/*
 * $Id: MapData.java,v 1.13 2007/03/22 10:51:37 jeffmc Exp $
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


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.xml.XmlUtil;



import ucar.visad.display.MapLines;

import java.awt.Color;



/**
 * Holds basic map information
 */

public class MapData {

    /** attribute identifier          */
    public static final String ATTR_COLOR = "color";

    /** attribute identifier          */
    public static final String ATTR_LINEWIDTH = "linewidth";

    /** attribute identifier          */
    public static final String ATTR_LINESTYLE = "linestyle";

    /** attribute identifier          */
    public static final String ATTR_FASTRENDERING = "fastrendering";

    /** attribute identifier          */
    public static final String ATTR_VISIBLE = "visible";


    /** The file, url or java resource of the map */
    protected String source;

    /** The category of this map   */
    protected String category = "Maps";

    /** The description of the map */
    protected String description;

    /** The color */
    protected Color mapColor;

    /** Is the map visible */
    protected boolean visible;

    /** The line width */
    protected float lineWidth = 1.0f;

    /** The line style */
    protected int lineStyle = 0;

    /** spacing */
    protected float spacing = 0;

    /** The MapLines displayable */
    protected MapLines myMap;

    /** fast rendering flag */
    protected boolean fastRendering = false;

    /**
     * ctor for bundles
     *
     */
    public MapData() {}


    /**
     * Create this MapData from the state in the given xml element
     *
     * @param mapNode The xml element
     * @param source  default source
     */
    public MapData(Element mapNode, String source) {
        this.source = source;
        //        source = XmlUtil.getAttribute(mapNode, MapInfo.ATTR_SOURCE, "");
        category = XmlUtil.getAttribute(mapNode, MapInfo.ATTR_CATEGORY,
                                        "Maps");

        description = XmlUtil.getAttribute(mapNode, MapInfo.ATTR_DESCRIPTION,
                                           source);
        mapColor = XmlUtil.getAttribute(mapNode, MapInfo.ATTR_COLOR,
                                        Color.white);
        visible = XmlUtil.getAttribute(mapNode, MapInfo.ATTR_VISIBLE, true);
        lineWidth = XmlUtil.getAttribute(mapNode, MapInfo.ATTR_LINEWIDTH,
                                         1.0f);
        lineStyle = XmlUtil.getAttribute(mapNode, MapInfo.ATTR_LINESTYLE, 0);
        fastRendering = XmlUtil.getAttribute(mapNode,
                                             MapInfo.ATTR_FASTRENDER, false);
    }


    /**
     * A copy constructor
     *
     * @param that The MapData to initialize from
     */
    public MapData(MapData that) {
        this.source        = that.source;
        this.description   = that.description;
        this.mapColor      = that.mapColor;
        this.visible       = that.visible;
        this.lineWidth     = that.lineWidth;
        this.lineStyle     = that.lineStyle;
        this.spacing       = that.spacing;
        this.fastRendering = that.fastRendering;
    }

    /**
     * Construct this MapData.
     *
     * @param mapPath File path, resource path or url of the map file.
     * @param description Map description
     * @param mapColor The initial color to use
     * @param lineWidth The initial line width to use
     * @param lineStyle The initial line style to use
     */
    public MapData(String mapPath, String description, Color mapColor,
                   float lineWidth, int lineStyle) {
        this(mapPath, null);
        this.description = description;
        this.mapColor    = mapColor;
        this.lineWidth   = lineWidth;
        this.lineStyle   = lineStyle;
        this.visible     = true;
    }

    /**
     * Create this object with the given soruce and state held in the map lines
     *
     * @param source The source
     * @param map The map lines
     *
     */
    public MapData(String source, MapLines map) {
        this.source = source;
        this.myMap  = map;
        if (map != null) {
            this.description = map.getName();
            visible          = map.getVisible();
            mapColor         = map.getColor();
            lineWidth        = map.getLineWidth();
            lineStyle        = map.getLineStyle();
            fastRendering    = map.getUseFastRendering();
        }
    }


    /**
     * A method that allows derived classes to be told
     * when the state has changed.
     */
    protected void stateChanged() {}

    /**
     * Get the MapLines
     *
     * @return The map lines
     */
    public MapLines getMap() {
        return myMap;
    }

    /**
     * Get the Color
     *
     * @return The color_
     */
    public Color getColor() {
        return mapColor;
    }

    /**
     * Set the color
     *
     * @param v the new color
     */
    public void setColor(Color v) {
        mapColor = v;
        stateChanged();
    }

    /**
     * Is this map visible
     *
     * @return Visibility
     */
    public boolean getVisible() {
        return visible;
    }

    /**
     * Set the visibility
     *
     * @param v The new visibility
     */
    public void setVisible(boolean v) {
        visible = v;
        stateChanged();
    }

    /**
     * Get the source of this map
     *
     * @return The map source
     */
    public String getSource() {
        return source;
    }

    /**
     * Set the map source
     *
     * @param v The new map source
     */
    public void setSource(String v) {
        source = v;
    }

    /**
     * Get the description
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the  description
     *
     * @param v The  new description
     */
    public void setDescription(String v) {
        description = v;
    }



    /**
     * Get the line width
     *
     * @return The  line width
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Set the line width
     *
     * @param v The new line width
     */
    public void setLineWidth(float v) {
        lineWidth = v;
        stateChanged();
    }


    /**
     * Get the line style
     *
     * @return The line style
     */
    public int getLineStyle() {
        return lineStyle;
    }

    /**
     * Set the line style
     *
     * @param v The new line style
     */
    public void setLineStyle(int v) {
        lineStyle = v;
        stateChanged();
    }

    /**
     * Get the spacing
     *
     * @return The spacing
     */
    public float getSpacing() {
        return spacing;
    }

    /**
     * Set the spacing
     *
     * @param v The new value for the spacing
     */
    public void setSpacing(float v) {
        spacing = v;
        stateChanged();
    }



    /**
     * Use the source as the hashcode
     *
     * @return The hashcode
     */
    public int hashCode() {
        return source.hashCode();
    }

    /**
     * Use the source as the .equals
     *
     * @param o The other object
     * @return Is equals
     */
    public boolean equals(Object o) {
        if ( !(o instanceof MapData)) {
            return false;
        }
        return source.equals(((MapData) o).source);
    }


    /**
     * Overwrite toString
     *
     * @return The string representation of this object
     */
    public String toString() {
        return source + " " + visible;
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

    /**
     *  Set the Category property.
     *
     *  @param value The new value for Category
     */
    public void setCategory(String value) {
        category = value;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public String getCategory() {
        return category;
    }


}


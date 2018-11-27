/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

import ucar.unidata.ui.FontSelector;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.MapFamily;

import visad.SampledSet;
import visad.VisADException;


import java.awt.Color;
import java.awt.Font;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 */

public class MapInfo {


    /** For LogUtil logging */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(MapInfo.class.getName());

    /** Xml tag for the latitude entry */
    public static final String TAG_LATITUDE = "latitude";

    /** Xml tag for the longitude entry */
    public static final String TAG_LONGITUDE = "longitude";

    /** Xml tag for the latitude label entry */
    public static final String TAG_LATITUDELABEL = "latitudelabel";

    /** Xml tag for the longitude entry */
    public static final String TAG_LONGITUDELABEL = "longitudelabel";

    /** Xml tag for the maps entry */
    public static final String TAG_MAPS = "maps";

    /** Xml tag for the map entry */
    public static final String TAG_MAP = "map";

    /** Xml attribute for the color property */
    public static final String ATTR_COLOR = "color";

    /** Xml attribute for the font property */
    public static final String ATTR_FONT = "font";

    /** Xml attribute for the category property */
    public static final String ATTR_CATEGORY = "category";

    /** Xml attribute for the  map description property */
    public static final String ATTR_DESCRIPTION = "description";

    /** Xml attribute for the  map description property */
    public static final String ATTR_FASTRENDER = "fastrender";

    /** Xml attribute for the  linewidth property */
    public static final String ATTR_LINEWIDTH = "linewidth";

    /** Xml attribute for the linestyle  property */
    public static final String ATTR_LINESTYLE = "linestyle";

    /** Xml attribute for the min value  property */
    public static final String ATTR_MINVALUE = "minvalue";

    /** Xml attribute for the max value property */
    public static final String ATTR_MAXVALUE = "maxvalue";

    /** Xml attribute for the  map vertical position property */
    public static final String ATTR_POSITION = "position";

    /** Xml attribute for the  map source property */
    public static final String ATTR_SOURCE = "source";

    /** Xml attribute for the spacing  property */
    public static final String ATTR_SPACING = "spacing";

    /** Xml attribute for the base  property */
    public static final String ATTR_BASE = "base";

    /** Xml attribute for the base  property */
    public static final String ATTR_LABELLINES = "labellines";

    /** Xml attribute for the sphere  property */
    public static final String ATTR_SPHERE = "sphere";

    /** Xml attribute for the alignment  property */
    public static final String ATTR_ALIGNMENT = "alignment";

    /** Xml attribute for the alignment  property */
    public static final String ATTR_LABELFORMAT = "labelformat";

    /** Xml attribute for the alignment  property */
    public static final String ATTR_USE360 = "use360";

    /** Xml attribute for the  property */
    public static final String ATTR_VALID = "valid";

    /** Xml attribute for the valid  property - used to ignore certain map entries */
    public static final String ATTR_VISIBLE = "visible";

    /** Static cache of the maps we have read in */
    private static Hashtable<String, SampledSet> mapCache =
        new Hashtable<String, SampledSet>();


    /**
     *  Keeps track of what nodes we have seen when we are loading
     *  in the users' and the default  xml.
     */
    private Hashtable seenNodes = new Hashtable();


    /**
     *  List of LatLonData objects.
     */
    private List<LatLonData> latLonData = new ArrayList<LatLonData>();

    /**
     *  List of LatLonLabelData objects.
     */
    private List<LatLonLabelData> latLonLabelData =
        new ArrayList<LatLonLabelData>();

    /**
     *  List of mapData objects, one for each map in the gui
     */
    private List mapDataList = new ArrayList();



    /**
     *  Set by the map xml, holds the level of the map used in the positionSlider
     */
    private double mapPosition = Double.NaN;


    /**
     * Private copy of MapFamily
     */
    private static MapFamily mapFamily = new MapFamily("IDV maps");

    /** just load local maps flag */
    private boolean justLoadedLocalMaps = false;

    /**
     * Parameterless ctro for unpersistence
     *
     */
    public MapInfo() {}


    /**
     * Create the MapInfo object with the given MapData list
     *
     * @param mapDataList List of MapData
     */
    public MapInfo(List mapDataList) {
        this(mapDataList, null, null, Float.NaN);
    }

    /**
     * Create the MapInfo object with the given MapData list, LatLonData and map position
     *
     * @param mapDataList List of MapData
     * @param latData The lat data
     * @param lonData The lon data
     * @param mapPosition The map position
     */
    public MapInfo(List mapDataList, LatLonData latData, LatLonData lonData,
                   float mapPosition) {

        this(mapDataList, latData, lonData, null, null, mapPosition);
    }

    /**
     * Create the MapInfo object with the given MapData list, LatLonData and map position
     *
     * @param mapDataList List of MapData
     * @param latData The lat data
     * @param lonData The lon data
     * @param latLabelData The lat label data
     * @param lonLabelData The lon label data
     * @param mapPosition The map position
     */
    public MapInfo(List mapDataList, LatLonData latData, LatLonData lonData,
                   LatLonLabelData latLabelData,
                   LatLonLabelData lonLabelData, float mapPosition) {
        this.mapDataList = mapDataList;
        if (latData != null) {
            this.latLonData.add(latData);
        }
        if (lonData != null) {
            this.latLonData.add(lonData);
        }
        if (latLabelData != null) {
            this.latLonLabelData.add(latLabelData);
        }
        if (lonLabelData != null) {
            this.latLonLabelData.add(lonLabelData);
        }
        this.mapPosition = mapPosition;
    }

    /**
     * Create the MapInfo from the given xml
     *
     * @param root The xml
     */
    public MapInfo(Element root) {
        if (root != null) {
            processMapsNode(root, "");
        }
        //Make sure we have a mapPosition
        if (Double.isNaN(mapPosition)) {
            mapPosition = -.99;
        }
    }


    /**
     * Create the MapInfo from the xml files pointed to by the resource collection
     *
     * @param mapResources Xml resources
     * @param stopAfterFirst Stop loading in maps after the first
     */
    public MapInfo(XmlResourceCollection mapResources,
                   boolean stopAfterFirst) {
        this(mapResources, stopAfterFirst, false);
    }


    /**
     * Create the MapInfo from the xml files pointed to by the resource collection
     *
     * @param mapResources Xml resources
     * @param stopAfterFirst If true then stop after we have loaded an editable resource
     * @param stopAfterEditable If we encounter any local maps.xml file then load it in and quit the loop
     */
    public MapInfo(XmlResourceCollection mapResources,
                   boolean stopAfterFirst, boolean stopAfterEditable) {
        try {
            //Now check the default
            for (int i = 0; i < mapResources.size(); i++) {
                Element root = mapResources.getRoot(i);
                processMapsNode(root, mapResources.get(i).toString());
                if (stopAfterEditable && mapResources.isWritable(i)) {
                    break;
                }
                if (stopAfterFirst && (i == 0) && (root != null)) {
                    justLoadedLocalMaps = true;
                    break;
                }
            }
        } catch (Exception exc) {
            LogUtil.printException(log_, "Creating xml", exc);
            return;
        }
    }



    /**
     *  Go through the latitude, longitude and maps xml tags.
     *  Create the appropriate objects and add them to the collection
     *  if we have not seen them already
     *
     * @param root
     * @param resourcePath path for the resource
     */
    private void processMapsNode(Element root, String resourcePath) {
        if (root == null) {
            return;
        }
        try {
            String resourceDir = IOUtil.getFileRoot(resourcePath);

            List   nodes       = XmlUtil.findChildren(root, TAG_MAP);
            for (int i = 0; i < nodes.size(); i++) {
                Element mapNode = (Element) nodes.get(i);
                if ( !XmlUtil.getAttribute(mapNode, ATTR_VALID, true)) {
                    continue;
                }
                String source = XmlUtil.getAttribute(mapNode, ATTR_SOURCE);
                if (IOUtil.isRelativePath(source)) {
                    source = resourceDir + "/" + source;
                }

                if (seenNodes.get(source) != null) {
                    continue;
                }
                mapDataList.add(new MapData(mapNode, source));
                seenNodes.put(source, mapNode);
            }

            // NB:  The || true is used because these methods didn't used
            // to handle empty nodes.  They now do, but we can easily back
            // out of this change if need be.
            Element latitudeNode = XmlUtil.findChild(root, TAG_LATITUDE);
            if ((latitudeNode != null) || true) {
                latLonData.add(createLatLonData(latitudeNode, true, -90.f,
                        90.f, 15.f));
            }

            Element longitudeNode = XmlUtil.findChild(root, TAG_LONGITUDE);
            if ((longitudeNode != null) || true) {
                latLonData.add(createLatLonData(longitudeNode, false, -180.f,
                        180.f, 15.f));
            }

            Element latitudeLabelNode = XmlUtil.findChild(root,
                                            TAG_LATITUDELABEL);
            if ((latitudeLabelNode != null) || true) {
                latLonLabelData.add(createLatLonLabelData(latitudeLabelNode,
                        true, 15.f));
            }

            Element longitudeLabelNode = XmlUtil.findChild(root,
                                             TAG_LONGITUDELABEL);
            if ((longitudeLabelNode != null) || true) {
                latLonLabelData.add(createLatLonLabelData(longitudeLabelNode,
                        false, 15.f));
            }


            if (Double.isNaN(mapPosition)) {
                mapPosition = (double) XmlUtil.getAttribute(root,
                        ATTR_POSITION, Double.NaN);
            }

        } catch (Exception e) {
            LogUtil.printException(log_, "Couldn't create map state", e);
        }
    }



    /**
     *  Return the current state (as an XML string) of this MapInfo
     *
     * @return The xml representation of the current state
     */
    public String getXml() {
        return getXml(true);
    }

    /**
     * Get the XML
     *
     * @param useFullSourcePath  true to use the full source path
     *
     * @return  the Xml
     */
    public String getXml(boolean useFullSourcePath) {
        Document document     = XmlUtil.makeDocument();
        Element  currentState = document.createElement(TAG_MAPS);
        if (mapPosition == mapPosition) {
            currentState.setAttribute(ATTR_POSITION, "" + mapPosition);
        }
        try {
            for (int i = 0; i < latLonData.size(); i++) {
                LatLonData lld        = (LatLonData) latLonData.get(i);
                Element    newElement =
                    document.createElement(lld.getIsLatitude()
                                           ? TAG_LATITUDE
                                           : TAG_LONGITUDE);
                currentState.appendChild(newElement);
                XmlUtil.setAttributes(newElement, new String[] {
                    ATTR_SPACING, "" + lld.getSpacing(), ATTR_BASE,
                    "" + lld.getBase(), ATTR_COLOR,
                    "" + lld.getColor().getRGB(), ATTR_LINESTYLE,
                    "" + lld.getLineStyle(), ATTR_LINEWIDTH,
                    "" + lld.getLineWidth(), ATTR_VISIBLE,
                    "" + lld.getVisible(), ATTR_FASTRENDER,
                    "" + lld.getFastRendering()
                });
            }

            for (LatLonLabelData llld : latLonLabelData) {
                Element newElement =
                    document.createElement(llld.getIsLatitude()
                                           ? TAG_LATITUDELABEL
                                           : TAG_LONGITUDELABEL);
                currentState.appendChild(newElement);
                XmlUtil.setAttributes(newElement, new String[] {
                    ATTR_SPACING, "" + llld.getInterval(), ATTR_BASE,
                    "" + llld.getBaseValue(), ATTR_LABELLINES,
                    "" + LatLonLabelData.formatLabelLines(
                        llld.getLabelLines()),
                    ATTR_COLOR, "" + llld.getColor().getRGB(), ATTR_VISIBLE,
                    "" + llld.getVisible(),
                    //ATTR_FASTRENDER, "" + llld.getFastRendering(),
                    //ATTR_FONT, ((llld.getFont() == null)
                    //            ? FontSelector.DEFAULT_FONT.toString()
                    //            : llld.getFont().toString()), 
                    ATTR_SPHERE, "" + llld.getSphere(), ATTR_ALIGNMENT,
                    "" + llld.getAlignment(), ATTR_LABELFORMAT,
                    "" + llld.getLabelFormat(), ATTR_USE360,
                    "" + llld.getUse360()
                });
            }

            // Now loop through the maps
            for (int i = 0; i < mapDataList.size(); i++) {
                MapData mapData    = (MapData) mapDataList.get(i);
                Element newElement = document.createElement(TAG_MAP);
                currentState.appendChild(newElement);
                String source = mapData.getSource();
                if ( !useFullSourcePath) {
                    source = "/" + IOUtil.getFileTail(source);
                }
                XmlUtil.setAttributes(newElement, new String[] {
                    ATTR_SOURCE, source, ATTR_CATEGORY, mapData.getCategory(),
                    ATTR_COLOR, "" + mapData.getColor().getRGB(),
                    ATTR_LINEWIDTH, "" + mapData.getLineWidth(),
                    ATTR_LINESTYLE, "" + mapData.getLineStyle(), ATTR_VISIBLE,
                    "" + mapData.getVisible(), ATTR_DESCRIPTION,
                    "" + mapData.getDescription(), ATTR_FASTRENDER,
                    "" + mapData.getFastRendering()
                });
            }
        } catch (Exception e) {
            LogUtil.printException(log_, "Couldn't create map state", e);
        }
        return XmlUtil.toStringWithHeader(currentState);
    }





    /**
     * Get the vertical map position value
     *
     * @return map position value
     */
    public double getMapPosition() {
        return mapPosition;
    }


    /**
     * Set the map position
     *
     * @param position The position
     */
    public void setMapPosition(double position) {
        mapPosition = position;
    }



    /**
     * A utility to read in the map data at the given source.
     *
     * @param source The fil, url or java resource to a map file (mcidas or shp)
     *
     * @return The data or null if this fails.
     */
    public static SampledSet createMapData(String source) {

        //        System.err.println ("CreateMap:" + source);
        SampledSet mapSet = mapCache.get(source);
        if (mapSet != null) {
            return (SampledSet) mapSet.clone();
        }

        ucar.unidata.util.Trace.call1("create map", source);
        /*
        if (source.indexOf("://") > 0) {
            try {
                url = new URL(source);
            } catch (Exception exc) {
                LogUtil.logException("Could not create map from url: "
                                     + source, exc);
                return null;
            }
        } else {
            url = Resource.getURL(source);
        }
            if (url == null) {  // try as a file
                //Try the file system
                if (mapSet == null) {
                    File f = new File(source);
                    if ( !f.exists()) {
                        LogUtil.userMessage("Could not create map: "
                                            + source);
                        return null;
                    }
                }
            }
        */

        try {
            URL url = IOUtil.getURL(source, MapInfo.class);
            mapSet = (url == null)
                     ? (SampledSet) mapFamily.open(source)
                     : (SampledSet) mapFamily.open(url);

        } catch (Exception exc) {
            LogUtil.logException("Could not create map from url: " + source,
                                 exc);
            return null;
        }
        if (mapSet == null) {
            LogUtil.userMessage("Could not create map: " + source);
        } else {
            mapCache.put(source, mapSet);
        }
        ucar.unidata.util.Trace.call2("create map");
        return (SampledSet) mapSet.clone();
    }


    /**
     * Create the {@link LatLonData}
     *
     * @param node The xml node
     * @param latitude Is this for lat or lon
     * @param min The min value
     * @param max The max value
     * @param spacing The spacing
     * @return The new LatLonData
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private LatLonData createLatLonData(Element node, boolean latitude,
                                        float min, float max, float spacing)
            throws VisADException, RemoteException {
        LatLonData lld = (node != null)
                         ? new LatLonData(latitude,
                             XmlUtil.getAttribute(node, ATTR_COLOR,
                                 Color.gray), XmlUtil.getAttribute(node,
                                     ATTR_SPACING,
                                     spacing), XmlUtil.getAttribute(node,
                                         ATTR_BASE,
                                         0.f), XmlUtil.getAttribute(node,
                                             ATTR_LINEWIDTH,
                                             1.0f), XmlUtil.getAttribute(node,
                                                 ATTR_LINESTYLE,
                                                 1), XmlUtil.getAttribute(node,
                                                     ATTR_FASTRENDER, false))
                         : new LatLonData(latitude, Color.gray, spacing, 0.f,
                                          1.0f, 1, false);

        if (node != null) {
            lld.setVisible(XmlUtil.getAttribute(node, ATTR_VISIBLE, false));
        } else {
            lld.setVisible(false);
        }
        lld.setMinValue(min);
        lld.setMaxValue(max);

        return lld;
    }

    /**
     * Create the {@link LatLonLabelData}
     *
     * @param node The xml node
     * @param latitude Is this for lat or lon
     * @param spacing The spacing
     * @return The new LatLonLabelData
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private LatLonLabelData createLatLonLabelData(Element node,
            boolean latitude, float spacing)
            throws VisADException, RemoteException {
        LatLonLabelData llld = (node != null)
                               ? new LatLonLabelData(latitude,
                                   XmlUtil.getAttribute(node, ATTR_SPACING,
                                       spacing))
                               : new LatLonLabelData(latitude, spacing);

        if (node != null) {
            llld.setBaseValue(XmlUtil.getAttribute(node, ATTR_BASE, 0.f));
            llld.setColor(XmlUtil.getAttribute(node, ATTR_COLOR,
                    Color.white));
            llld.setVisible(XmlUtil.getAttribute(node, ATTR_VISIBLE, false));
            llld.setLabelsLineString(XmlUtil.getAttribute(node,
                    ATTR_LABELLINES, "0"));
            llld.setAlignment(XmlUtil.getAttribute(node, ATTR_ALIGNMENT,
                    "MM"));
            llld.setLabelFormat(XmlUtil.getAttribute(node, ATTR_LABELFORMAT,
                    "DD"));
            llld.setSphere(XmlUtil.getAttribute(node, ATTR_SPHERE, false));
            //llld.setFont(Font.decode(XmlUtil.getAttribute(node, ATTR_FONT,
            //        (String) null)));
            llld.setUse360(XmlUtil.getAttribute(node, ATTR_USE360, false));
        } else {
            llld.setBaseValue(0.f);
            llld.setColor(Color.white);
            llld.setVisible(false);
            llld.setLabelsLineString("0");
            llld.setAlignment("MM");
            llld.setLabelFormat("DD");
            llld.setSphere(false);
        }
        return llld;
    }



    /**
     * Set the LatLonData property.
     *
     * @param value The new value for LatLonData
     */
    public void setLatLonData(List value) {
        latLonData = value;
    }

    /**
     * Get the LatLonData property.
     *
     * @return The LatLonData
     */
    public List getLatLonData() {
        return latLonData;
    }

    /**
     * Set the LatLonLabelData property.
     *
     * @param value The new value for LatLonData
     */
    public void setLatLonLabelData(List<LatLonLabelData> value) {
        latLonLabelData = value;
    }


    /**
     * Get the LatLonLabelData property.
     *
     * @return The LatLonLabelData
     */
    public List<LatLonLabelData> getLatLonLabelData() {
        return latLonLabelData;
    }


    /**
     * Set the MapData property.
     *
     * @param value The new value for MapData
     */
    public void setMapDataList(List value) {
        mapDataList = value;
    }

    /**
     * Get the MapData property.
     *
     * @return The MapData
     */
    public List<MapData> getMapDataList() {
        return (List<MapData>) mapDataList;
    }


    /**
     * Find and return the LatLonData object that represents the lat state
     *
     * @return The lat state
     */
    public LatLonData getLatData() {
        for (int i = 0; i < latLonData.size(); i++) {
            LatLonData lld = (LatLonData) latLonData.get(i);
            if (lld.getIsLatitude()) {
                return lld;
            }
        }
        return null;
    }


    /**
     * Find and return the LatLonData object that represents the lon state
     *
     * @return The lon state
     */
    public LatLonData getLonData() {
        for (int i = 0; i < latLonData.size(); i++) {
            LatLonData lld = (LatLonData) latLonData.get(i);
            if ( !lld.getIsLatitude()) {
                return lld;
            }
        }
        return null;
    }

    /**
     * Find and return the LatLonData object that represents the lat state
     *
     * @return The lat state
     */
    public LatLonLabelData getLatLabelData() {
        for (int i = 0; i < latLonLabelData.size(); i++) {
            LatLonLabelData lld = (LatLonLabelData) latLonLabelData.get(i);
            if (lld.getIsLatitude()) {
                return lld;
            }
        }
        return null;
    }


    /**
     * Find and return the LatLonData object that represents the lon state
     *
     * @return The lon state
     */
    public LatLonLabelData getLonLabelData() {
        for (int i = 0; i < latLonLabelData.size(); i++) {
            LatLonLabelData lld = (LatLonLabelData) latLonLabelData.get(i);
            if ( !lld.getIsLatitude()) {
                return lld;
            }
        }
        return null;
    }

    /**
     * Get the Flag for just loading local maps
     *
     * @return the flag
     */
    public boolean getJustLoadedLocalMaps() {
        return justLoadedLocalMaps;
    }

    /**
     * Set the Flag for just loading local maps
     *
     * @param v the flag
     */
    public void setJustLoadedLocalMaps(boolean v) {
        justLoadedLocalMaps = v;
    }

}

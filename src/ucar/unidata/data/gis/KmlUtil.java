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

package ucar.unidata.data.gis;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.awt.Color;


import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;


/**
 * DataSource for Web Map Servers
 *
 * @author IDV development team
 * @version $Revision: 1.38 $ $Date: 2007/04/16 20:34:52 $
 */
public class KmlUtil {

    //J-
    public static final String TAG_ALTITUDE = "altitude";
    public static final String TAG_ALTITUDEMODE = "altitudeMode";
    public static final String TAG_BALLOONSTYLE = "BalloonStyle";
    public static final String TAG_BOTTOMFOV = "bottomFov";
    public static final String TAG_CAMERA = "Camera";
    public static final String TAG_COLOR = "color";
    public static final String TAG_COLORMODE = "colorMode";
    public static final String TAG_COORDINATES = "coordinates";

    public static final String TAG_DESCRIPTION = "description";
    public static final String TAG_DOCUMENT = "Document";
    public static final String TAG_EAST = "east";
    public static final String TAG_EXTRUDE = "extrude";
    public static final String TAG_FOLDER = "Folder";
    public static final String TAG_GROUNDOVERLAY = "GroundOverlay";
    public static final String TAG_HEADING = "heading";
    public static final String TAG_HREF = "href";
    public static final String TAG_ICON = "Icon";
    public static final String TAG_ICONSTYLE = "IconStyle";
	public static final String TAG_KEY = "key";
    public static final String TAG_KML = "kml";
	public static final String TAG_LABELSTYLE = "LabelStyle";
    public static final String TAG_LATITUDE = "latitude";
    public static final String TAG_LATLONBOX = "LatLonBox";
    public static final String TAG_LEFTFOV = "leftFov";
    public static final String TAG_LINESTRING = "LineString";
    public static final String TAG_LINESTYLE = "LineStyle";
    public static final String TAG_LINK = "Link";
    public static final String TAG_LONGITUDE = "longitude";
    public static final String TAG_LOOKAT = "LookAt";
    public static final String TAG_MULTIGEOMETRY = "MultiGeometry";
    public static final String TAG_NAME = "name";
    public static final String TAG_NEAR = "near";
    public static final String TAG_NETWORKLINK = "NetworkLink";
    public static final String TAG_NORTH = "north";
    public static final String TAG_PHOTOOVERLAY = "PhotoOverlay";
    public static final String TAG_OPEN = "open";
    public static final String TAG_OVERLAYXY = "overlayXY";
    public static final String TAG_PAIR = "Pair";
    public static final String TAG_PLACEMARK = "Placemark";
    public static final String TAG_POINT = "Point";
    public static final String TAG_RIGHTFOV = "rightFov";
    public static final String TAG_ROLL = "roll";
    public static final String TAG_ROTATION = "rotation";
    public static final String TAG_SCHEMA = "Schema";
    public static final String TAG_SCREENOVERLAY = "ScreenOverlay";
    public static final String TAG_SCREENXY = "screenXY";
	private static final String TAG_SIZE = "size";
    public static final String TAG_SNIPPET = "Snippet";
    public static final String TAG_SOUTH = "south";
    public static final String TAG_STYLE = "Style";
    public static final String TAG_STYLEMAP = "StyleMap";
    public static final String TAG_STYLEURL = "styleUrl";
    public static final String TAG_TESSELATE = "tesselate";
    public static final String TAG_TEXT = "text";
    public static final String TAG_TILT = "tilt";
    public static final String TAG_TIMESTAMP = "TimeStamp";
    public static final String TAG_TOPFOV = "topFov";
    public static final String TAG_URL = "Url";
    public static final String TAG_VIEWVOLUME = "ViewVolume";
    public static final String TAG_SCALE = "scale";
    public static final String TAG_VIEWBOUNDSCALE = "viewBoundScale";
    public static final String TAG_VISIBILITY = "visibility";
    public static final String TAG_WEST = "west";
    public static final String TAG_WHEN = "when";
    public static final String TAG_WIDTH = "width";
    //J+



    /** the Tour tag */
    public static final String TAG_TOUR = "gx:Tour";

    /** the Playlist tag */
    public static final String TAG_PLAYLIST = "gx:Playlist";

    /** the FlyTo tag */
    public static final String TAG_FLYTO = "gx:FlyTo";

    /** the Wait tag */
    public static final String TAG_WAIT = "gx:Wait";

    /** the id attribute */
    public static final String ATTR_ID = "id";

    /** the name attribute */
    public static final String ATTR_NAME = "name";

    /** the x attribute */
    public static final String ATTR_X = "x";

    /** the y attribute */
    public static final String ATTR_Y = "y";

    /** the xunits attribute */
    public static final String ATTR_XUNITS = "xunits";

    /** the yunits attribute */
    public static final String ATTR_YUNITS = "yunits";

    /** the KML 2.2 XML namespace */
    public static final String XMLNS_KML2_2 =
        "http://www.opengis.net/kml/2.2";

    /**
     * Make a Kml element
     *
     * @param parent  the parent element
     * @param tag  the tag name
     *
     * @return  the Element
     */
    public static Element makeElement(Element parent, String tag) {
        Element child = parent.getOwnerDocument().createElement(tag);
        parent.appendChild(child);
        return child;
    }

    /**
     * Make the kml element
     *
     * @param name  the name of the element (not used)
     *
     * @return  the kml element
     */
    public static Element kml(String name) {
        Document doc   = XmlUtil.makeDocument();
        Element  child = doc.createElement(TAG_KML);
        child.setAttribute("xmlns", XMLNS_KML2_2);
        return child;
    }


    /**
     * Make a KML Document
     *
     * @param parent  parent Element
     * @param name  the name of the Document
     *
     * @return the Document element
     */
    public static Element document(Element parent, String name) {
        return document(parent, name, false);
    }

    /**
     * Make a KML Document Element
     *
     * @param parent  the parent node
     * @param name  the name of the document
     * @param visible  true if visible
     *
     * @return  the Document element
     */
    public static Element document(Element parent, String name,
                                   boolean visible) {
        Element node = makeElement(parent, TAG_DOCUMENT);
        name(node, name);
        visible(node, visible);
        return node;
    }


    /**
     * Make a NetworkLink element
     *
     * @param parent parent node
     * @param name  name of the link
     * @param url   link URL
     *
     * @return  the NetworkLink element
     */
    public static Element networkLink(Element parent, String name,
                                      String url) {
        Element networkLink = makeElement(parent, TAG_NETWORKLINK);
        makeText(networkLink, TAG_NAME, name);
        Element link = makeElement(networkLink, TAG_LINK);
        makeText(link, TAG_HREF, url);
        //        makeElement(link,TAG_HREF);
        return networkLink;
    }

    /*
<NetworkLink>
        <name>SVP Drifter 82224</name>
        <visibility>1</visibility>
        <flyToView>0</flyToView>
        <Link>
                <href>http://dataserver.imedea.uib-csic.es:8080/repository/entry/get/20090511_sinocop_b82224.kmz?entryid=0b13318a-2520-4fcb-915e-55af83c1fede</href>

    */

    /**
     * Make a text node in the parent element
     *
     * @param parent  the parent
     * @param tag  the tag
     * @param text  the text
     *
     * @return  parent with the text element (e.g., <parent><tag>text</tag></parent>)
     */
    public static Element makeText(Element parent, String tag, String text) {
        Element node     = makeElement(parent, tag);
        Text    textNode = parent.getOwnerDocument().createTextNode(text);
        node.appendChild(textNode);
        return node;
    }

    /**
     * Set the visibility on an Element
     *
     * @param parent  the parent element
     * @param visible  true to be visible
     *
     * @return the parent
     */
    public static Element visible(Element parent, boolean visible) {
        return makeText(parent, TAG_VISIBILITY, (visible
                ? "1"
                : "0"));
    }



    /**
     * Make a snippet Element
     *
     * @param parent  the parent
     * @param snippet  the snippet text
     *
     * @return  the parent with the snippet
     */
    public static Element snippet(Element parent, String snippet) {
        return makeText(parent, TAG_SNIPPET, snippet);
    }


    /**
     * Set the open flag on an elemenet
     *
     * @param parent  the parent node
     * @param open  true to be open
     *
     * @return  the parent node
     */
    public static Element open(Element parent, boolean open) {
        return makeText(parent, TAG_OPEN, (open
                                           ? "1"
                                           : "0"));
    }



    /*    public static Element timestamp(Element parent, DateTime dttm) {
        String when = dttm.formattedString("yyyy-MM-dd", DateUtil.TIMEZONE_GMT)
            + "T"
            + dttm.formattedString("HH:mm:ss", DateUtil.TIMEZONE_GMT)
            + "Z";
        Element timestamp = makeElement(parent, TAG_TIMESTAMP);
        makeText(timestamp,TAG_WHEN, when);
        return timestamp;
        }*/


    /** simple date format  for yyyy-MM-dd */
    private static SimpleDateFormat sdf1;

    /** simple date format HH:mm:ss */
    private static SimpleDateFormat sdf2;

    /**
     * Make a timestamp from the date
     *
     * @param parent  the parent node
     * @param dttm  the date
     *
     * @return  the timestamp
     */
    public static Element timestamp(Element parent, Date dttm) {
        if (sdf1 == null) {
            sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            sdf2 = new SimpleDateFormat("HH:mm:ss");
            sdf1.setTimeZone(DateUtil.TIMEZONE_GMT);
            sdf2.setTimeZone(DateUtil.TIMEZONE_GMT);

        }
        String  when      = sdf1.format(dttm) + "T" + sdf2.format(dttm) + "Z";
        Element timestamp = makeElement(parent, TAG_TIMESTAMP);
        makeText(timestamp, TAG_WHEN, when);
        return timestamp;
    }

    /**
     * Make a pair element
     *
     * @param parent  the parent element
     * @param key  the pair key
     * @param styleUrl  the pair styleUrl
     *
     * @return  the Pair element
     */
    public static Element pair(Element parent, String key, String styleUrl) {
        Element pair = makeElement(parent, TAG_PAIR);
        makeText(pair, TAG_KEY, key);
        makeText(pair, TAG_STYLEURL, styleUrl);
        return pair;

    }

    /**
     * Create a StyleMap for normal and highlighted styles
     *
     * @param parent  the parent element
     * @param id  the id of the StyleMap
     * @param normalStyleUrl  the styleUrl of the normal style
     * @param highlightStyleUrl  the styleUrl of the highlighted style
     *
     * @return the StyleMap element
     */
    public static Element stylemap(Element parent, String id,
                                   String normalStyleUrl,
                                   String highlightStyleUrl) {
        Element stylemap = makeElement(parent, TAG_STYLEMAP);
        stylemap.setAttribute(ATTR_ID, id);
        pair(stylemap, "normal", normalStyleUrl);
        pair(stylemap, "highlight", highlightStyleUrl);
        return stylemap;
        /* <StyleMap id="ID">
           <!-- extends StyleSelector -->
          <!-- elements specific to StyleMap -->
          <Pair id="ID">
            <key>normal</key>              <!-- kml:styleStateEnum:  normal or highlight -->
            <styleUrl>...</styleUrl> or <Style>...</Style>
          </Pair>
          </StyleMap>*/
    }

    /**
     * Make a styleUrl element (why isn't the s capitalized?)
     *
     * @param parent  the parent element
     * @param url  the url
     *
     * @return  the styleUrl element
     */
    public static Element styleurl(Element parent, String url) {
        //<styleUrl>#linestyleExample</styleUrl>
        return makeText(parent, TAG_STYLEURL, url);
    }

    /**
     * Make a Style element
     *
     * @param parent  the parent node
     * @param id  the Style id
     *
     * @return  the Style element
     */
    public static Element style(Element parent, String id) {
        Element style = makeElement(parent, TAG_STYLE);
        style.setAttribute(ATTR_ID, id);
        return style;
    }


    /*
        <Style id="globeIcon">
      <IconStyle>
        <Icon>
          <href>http://maps.google.com/mapfiles/kml/pal3/icon19.png</href>
        </Icon>
      </IconStyle>
      </Style>*/

    /**
     * Create an IconStyle element enclosed in a Style element
     *
     * @param parent  the parent Element
     * @param id  name of the enclosing Style element
     * @param url  the icon URL
     *
     * @return  the Style element enclosing the IconStyle element
     */
    public static Element iconstyle(Element parent, String id, String url) {
        return iconstyle(parent, id, url, -1);
    }

    /**
     * Create an IconStyle element enclosed in a Style element
     *
     * @param parent  the parent Element
     * @param id  name of the enclosing Style element
     * @param url  the icon URL
     * @param scale  the size scale (>= 0, 1 = normal size)
     *
     * @return  the Style element enclosing the IconStyle element
     */
    public static Element iconstyle(Element parent, String id, String url,
                                    double scale) {
        return iconstyle(parent, id, url, scale, null);
    }

    /**
     * Create an IconStyle element enclosed in a Style element
     *
     * @param parent  the parent Element
     * @param id  name of the enclosing Style element
     * @param url  the icon URL
     * @param scale  the size scale (>= 0, 1 = normal size)
     * @param color  the icon color
     *
     * @return  the Style element enclosing the IconStyle element
     */
    public static Element iconstyle(Element parent, String id, String url,
                                    double scale, Color color) {
        Element style     = style(parent, id);
        Element iconstyle = iconstyle(style, url, scale, color);
        return iconstyle;
    }


    /**
     * Create an IconStyle element
     *
     * @param parent  the parent (Style) Element
     * @param url  the icon URL
     * @param scale  the size scale (>= 0, 1 = normal size)
     * @param color  the icon color
     *
     * @return  the IconStyle element
     */
    public static Element iconstyle(Element parent, String url, double scale,
                                    Color color) {
        Element iconstyle = makeElement(parent, TAG_ICONSTYLE);
        if (color != null) {
            makeText(iconstyle, TAG_COLOR,
                     "ff" + toBGRHexString(color).substring(1));
            makeText(iconstyle, TAG_COLORMODE, "normal");
        }
        Element icon = makeElement(iconstyle, TAG_ICON);
        makeText(icon, TAG_HREF, url);
        if (scale >= 0) {
            makeText(iconstyle, TAG_SCALE, "" + scale);
        }

        return iconstyle;
    }

    /**
     * Create a BalloonStyle element
     *
     * @param parent  parent (Style) element
     * @param text  the balloon text
     * @param bgColor  the background color
     *
     * @return the BalloonStyle element
     */
    public static Element balloonstyle(Element parent, String text,
                                       Color bgColor) {
        Element bstyle = makeElement(parent, TAG_BALLOONSTYLE);
        if (bgColor != null) {
            makeText(bstyle, TAG_COLOR,
                     "ff" + toBGRHexString(bgColor).substring(1));
        }
        makeText(bstyle, TAG_TEXT, text);
        return bstyle;
    }

    /**
     * Create a BalloonStyle element wrapped in a Style element
     *
     * @param parent  parent element
     * @param id  of the enclosing Style element
     * @param text  the balloon text
     * @param bgColor  the background color
     *
     * @return the Style Element with the BalloonStyle element included
     */
    public static Element balloonstyle(Element parent, String id,
                                       String text, Color bgColor) {
        Element style  = style(parent, id);
        Element bstyle = balloonstyle(style, text, bgColor);
        return bstyle;
    }

    /**
     * Create a LabelStyle element enclosed in a Style Element
     *
     * @param parent The parent for the style
     * @param id  the Style id
     * @param color  the label color
     * @param scale  the label size scale
     *
     * @return  the Style Element enclosing the LabelStyle
     */
    public static Element labelstyle(Element parent, String id, Color color,
                                     int scale) {
        Element style      = style(parent, id);
        Element labelstyle = labelstyle(style, color, scale);
        return labelstyle;
    }

    /**
     * Create a LabelStyle element
     *
     * @param parent The parent (Style) element
     * @param color  the label color
     * @param scale  the label size scale
     *
     * @return  the LabelStyle
     */
    public static Element labelstyle(Element parent, Color color, int scale) {
        Element labelstyle = makeElement(parent, TAG_LABELSTYLE);
        if (color != null) {
            makeText(labelstyle, TAG_COLOR,
                     "ff" + toBGRHexString(color).substring(1));
            makeText(labelstyle, TAG_COLORMODE, "normal");
        }
        if (scale >= 0) {
            makeText(labelstyle, TAG_SCALE, "" + scale);
        }
        return labelstyle;
    }
    /*
    <LabelStyle id="ID">
    <!-- inherited from ColorStyle -->
    <color>ffffffff</color>            <!-- kml:color -->
    <colorMode>normal</colorMode>      <!-- kml:colorModeEnum: normal or random -->

    <!-- specific to LabelStyle -->
    <scale>1</scale>                   <!-- float -->
  </LabelStyle>*/


    /**
     * Create a LineStyle element
     *
     * @param parent  the parent node
     * @param id  the id for the enclosing Style
     * @param color  line color
     * @param width  line width
     *
     * @return  the LineStyle Element
     */
    public static Element linestyle(Element parent, String id, Color color,
                                    int width) {
        Element style     = style(parent, id);
        Element linestyle = makeElement(style, TAG_LINESTYLE);
        if (color != null) {
            makeText(linestyle, TAG_COLOR,
                     "ff" + toBGRHexString(color).substring(1));
            makeText(linestyle, TAG_COLORMODE, "normal");
        }
        if (width > 0) {
            makeText(linestyle, TAG_WIDTH, "" + width);
        }
        return linestyle;
        /*<LineStyle id="ID">
  <color>ffffffff</color>            <!-- kml:color -->
  <colorMode>normal</colorMode>      <!-- colorModeEnum: normal or random -->
  <width>1</width>                   <!-- float -->
  </LineStyle>*/
    }

    /**
     * Create a LineString element
     *
     * @param parent  the parent node
     * @param extrude  true to extrude
     * @param tesselate  true to tesselate
     * @param coordinates  comma separated list of coordinates (lon1,lat1,alt1,lon2,lat2,alt2,....lonN,latN,altN)
     *
     * @return  the LineString element
     */
    public static Element linestring(Element parent, boolean extrude,
                                     boolean tesselate, String coordinates) {
        Element node = makeElement(parent, TAG_LINESTRING);
        makeText(node, TAG_EXTRUDE, (extrude
                                     ? "1"
                                     : "0"));
        makeText(node, TAG_TESSELATE, (tesselate
                                       ? "1"
                                       : "0"));
        coordinates(node, coordinates);
        return node;

    }



    /**
     * Create a LineString element
     *
     * @param parent  the parent node
     * @param extrude  true to extrude
     * @param tesselate  true to tesselate
     * @param coords   array of coordinates (coords[lon,lat] or coords[lon,lat,alt])
     *
     * @return  the LineString element
     */
    public static Element linestring(Element parent, boolean extrude,
                                     boolean tesselate, float[][] coords) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < coords[0].length; i++) {
            sb.append(coords[1][i]);
            sb.append(",");
            sb.append(coords[0][i]);
            if (coords.length > 2) {
                sb.append(",");
                sb.append(coords[2][i]);
            }
            sb.append(" ");
        }
        return linestring(parent, extrude, tesselate, sb.toString());
    }


    /**
     * Make a coordinates element
     *
     * @param parent  the parent node
     * @param coordinates  the coordinates (comma separated values)
     *
     * @return the coordinates element
     */
    public static Element coordinates(Element parent, String coordinates) {
        Element node  = makeElement(parent, TAG_COORDINATES);
        Text textNode = parent.getOwnerDocument().createTextNode(coordinates);
        node.appendChild(textNode);
        return node;
    }

    /**
     * Make a name element (wrapped in a CDATA structure)
     *
     * @param parent  the parent node
     * @param name  the name text
     *
     * @return  the name element
     */
    public static Element name(Element parent, String name) {
        Element node = makeElement(parent, TAG_NAME);
        CDATASection cdata =
            parent.getOwnerDocument().createCDATASection(name);
        node.appendChild(cdata);
        return node;
    }

    /**
     * Make a description element (wrapped in a CDATA structure)
     *
     * @param parent  the parent node
     * @param description  the description
     *
     * @return  the description element
     */
    public static Element description(Element parent, String description) {
        Element node = makeElement(parent, TAG_DESCRIPTION);
        CDATASection cdata =
            parent.getOwnerDocument().createCDATASection(description);
        node.appendChild(cdata);
        return node;
    }


    /**
     * Create a Folder element
     *
     * @param parent  the parent node
     * @param name  the name of the folder
     *
     * @return  the Folder element
     */
    public static Element folder(Element parent, String name) {
        return folder(parent, name, false);
    }

    /**
     * Create a Folder element
     *
     * @param parent  the parent node
     * @param name  the name of the folder
     * @param visible  true to be visible.  (all children must be not visible if you want the folder turned off)
     *
     * @return  the Folder element
     */
    public static Element folder(Element parent, String name,
                                 boolean visible) {
        Element node = makeElement(parent, TAG_FOLDER);
        name(node, name);
        visible(node, visible);
        return node;
    }


    /**
     * Create a Placemark
     *
     * @param parent  the parent node
     * @param name  the name of the placemark
     * @param description  the description of the placemark
     *
     * @return  the Placemark element
     */
    public static Element placemark(Element parent, String name,
                                    String description) {
        Element node = makeElement(parent, TAG_PLACEMARK);
        name(node, name);
        description(node, description);
        return node;
    }


    /*
    public static Element placemark(Element parent, String name, String description, visad.georef.EarthLocation el, String style) throws Exception {
        return placemark(parent, name, description,
                         el.getLatitude().getValue(),
                         el.getLongitude().getValue(visad.CommonUnit.degree),
                         (el.getAltitude()!=null?el.getAltitude().getValue():0), style);
                         }*/


    /**
     * Make a point Element string
     *
     * @param lat  the point latitude
     * @param lon  the point longitude
     * @param alt  the point altitude
     * @param style  the point style (not used)
     *
     * @return  the Point text
     */
    public static String point(double lat, double lon, double alt,
                               String style) {
        return "<Placemark><Point><coordinates>" + lon + "," + lat + ","
               + alt + "</coordinates></Point></Placemark>";
    }

    /**
     * Make a Placemark element
     *
     * @param parent  the parent node
     * @param name    the Placemark name
     * @param description    the Placemark description
     * @param lat  the latitude
     * @param lon  the latitude
     * @param alt  the latitude
     * @param styleUrl  the styleUrl
     *
     * @return  the Placemark element
     */
    public static Element placemark(Element parent, String name,
                                    String description, double lat,
                                    double lon, double alt, String styleUrl) {
        return placemark(parent, name, description, lat, lon, alt,
                         new String[] { styleUrl }, true);
    }


    /**
     * Make a Placemark element
     *
     * @param parent  the parent node
     * @param name    the Placemark name
     * @param description    the Placemark description
     * @param lat  the latitude
     * @param lon  the latitude
     * @param alt  the latitude
     * @param styleUrls  the array styleUrls
     * @param visible  true for this to be showing
     *
     * @return  the Placemark element
     */
    public static Element placemark(Element parent, String name,
                                    String description, double lat,
                                    double lon, double alt,
                                    String[] styleUrls, boolean visible) {
        Element placemark = placemark(parent, name, description);
        if (styleUrls != null) {
            for (int i = 0; i < styleUrls.length; i++) {
                makeText(placemark, TAG_STYLEURL, styleUrls[i]);
            }
        }
        visible(placemark, visible);
        Element point = makeElement(placemark, TAG_POINT);
        makeText(point, TAG_COORDINATES, lon + "," + lat + "," + alt + " ");
        return placemark;
    }


    /**
     * Create a GroundOverlay element
     *
     * @param parent  the parent node
     * @param name    the name of the GroundOverlay
     * @param description    the description of the GroundOverlay
     * @param url    the URL of the overlay
     * @param north  the north coordinate
     * @param south  the south coordinate
     * @param east   the east coordinate
     * @param west   the west coordinate
     *
     * @return the GroundOverlay
     */
    public static Element groundOverlay(Element parent, String name,
                                        String description, String url,
                                        double north, double south,
                                        double east, double west) {

        return groundOverlay(parent, name, description, url, north, south, east, west, false);
    }


    /**
     * Create a GroundOverlay element
     *
     * @param parent  the parent node
     * @param name    the name of the GroundOverlay
     * @param description    the description of the GroundOverlay
     * @param url    the URL of the overlay
     * @param north  the north coordinate
     * @param south  the south coordinate
     * @param east   the east coordinate
     * @param west   the west coordinate
     *
     * @return the GroundOverlay
     */
    public static Element groundOverlay(Element parent, String name,
                                        String description, String url,
                                        double north, double south,
                                        double east, double west, boolean visible) {
        Element node = makeElement(parent, TAG_GROUNDOVERLAY);
        name(node, name);
        description(node, description);
        visible(node, false);
        Element icon = makeElement(node, TAG_ICON);
        Element href = makeText(icon, TAG_HREF, url);
        Element llb  = makeElement(node, TAG_LATLONBOX);
        makeText(llb, TAG_NORTH, "" + north);
        makeText(llb, TAG_SOUTH, "" + south);
        makeText(llb, TAG_EAST, "" + east);
        makeText(llb, TAG_WEST, "" + west);
        return node;
    }

    /**
     * Make a Placemark with a Linestring
     *
     * @param parent  the parent node
     * @param name    the name of the Placemark
     * @param description   the description of the Placemark
     * @param coords    the line coordinates
     * @param color     the line color
     * @param width     the line width
     *
     * @return  the Placemark
     */
    public static Element placemark(Element parent, String name,
                                    String description, float[][] coords,
                                    Color color, int width) {
        Element placemark  = placemark(parent, name, description);
        Element linestring = linestring(placemark, false, false, coords);
        String randomStyle = System.currentTimeMillis() + "_"
                             + (int) (Math.random() * 1000);
        linestyle(placemark, randomStyle, color, width);
        return placemark;
    }


    /*
      <Placemark>
        <name>Floating placemark</name>
        <visibility>0</visibility>
        <description>Floats a defined distance above the ground.</description>
        <LookAt>
          <longitude>-122.0839597145766</longitude>
          <latitude>37.42222904525232</latitude>
          <altitude>0</altitude>
          <range>500.6566641072245</range>
          <tilt>40.5575073395506</tilt>
          <heading>-148.4122922628044</heading>
        </LookAt>
        <styleUrl>#downArrowIcon</styleUrl>
        <Point>
          <altitudeMode>relativeToGround</altitudeMode>
          <coordinates>-122.084075,37.4220033612141,50</coordinates>
        </Point>
        </Placemark>*/


    /**
     * Parse coordinates
     *
     * @param coords  the string of space separated coordinates
     *
     * @return  the parsed coordinates
     */
    public static double[][] parseCoordinates(String coords) {
        coords = StringUtil.replace(coords, "\n", " ");
        while (true) {
            String newCoords = StringUtil.replace(coords, " ,", ",");
            if (newCoords.equals(coords)) {
                break;
            }
            coords = newCoords;
        }
        while (true) {
            String newCoords = StringUtil.replace(coords, ", ", ",");
            if (newCoords.equals(coords)) {
                break;
            }
            coords = newCoords;
        }

        List       tokens = StringUtil.split(coords, " ", true, true);
        double[][] result = null;
        for (int pointIdx = 0; pointIdx < tokens.size(); pointIdx++) {
            String tok     = (String) tokens.get(pointIdx);
            List   numbers = StringUtil.split(tok, ",");
            if ((numbers.size() != 2) && (numbers.size() != 3)) {
                //Maybe its just comma separated
                if ((numbers.size() > 3) && (tokens.size() == 1)
                        && ((int) numbers.size() / 3) * 3 == numbers.size()) {
                    result = new double[3][numbers.size() / 3];
                    int cnt = 0;
                    for (int i = 0; i < numbers.size(); i += 3) {
                        result[0][cnt] = new Double(
                            numbers.get(i).toString()).doubleValue();
                        result[1][cnt] = new Double(numbers.get(i
                                + 1).toString()).doubleValue();
                        result[2][cnt] = new Double(numbers.get(i
                                + 2).toString()).doubleValue();
                        cnt++;
                    }
                    return result;
                }
                throw new IllegalStateException(
                    "Bad number of coordinate values:" + numbers);
            }
            if (result == null) {
                result = new double[numbers.size()][tokens.size()];
            }
            for (int coordIdx = 0; (coordIdx < numbers.size()); coordIdx++) {
                result[coordIdx][pointIdx] = new Double(
                    numbers.get(coordIdx).toString()).doubleValue();
            }
        }
        return result;
    }

    /**
     * Convert the given color to is string BGR hex representation.  KML uses
     * ABGR instead of RGBA
     *
     * @param c color
     *
     * @return hex representation (BGR) of the Color's r,g,b values
     */
    public static String toBGRHexString(java.awt.Color c) {
        return "#"
               + StringUtil.padRight(
                   Integer.toHexString(c.getBlue()), 2,
                   "0") + StringUtil.padRight(
                       Integer.toHexString(c.getGreen()), 2,
                       "0") + StringUtil.padRight(
                           Integer.toHexString(c.getRed()), 2, "0");
    }

    /**
     * Create a ScreenOverlay Element
     *
     * @param parent parent Element
     * @param name   the name of the element
     * @param iconURL  the URL of the icon
     * @param overlayX  x component of a point on the overlay image
     * @param overlayY  y component of a point on the overlay image
     * @param overlayXunits  units of overlayX, can be one of: fraction, pixels, or insetPixels
     * @param overlayYunits  units of overlayY, can be one of: fraction, pixels, or insetPixels
     * @param screenX x component of a point on the screen
     * @param screenY y component of a point on the screen
     * @param screenXunits units of screenX, can be one of: fraction, pixels, or insetPixels
     * @param screenYunits units of screenY, can be one of: fraction, pixels, or insetPixels
     *
     * @return the ScreenOverlay Element
     */
    public static Element screenoverlay(Element parent, String name,
                                        String iconURL, double overlayX,
                                        double overlayY,
                                        String overlayXunits,
                                        String overlayYunits, double screenX,
                                        double screenY, String screenXunits,
                                        String screenYunits) {
        return screenoverlay(parent, name, iconURL, overlayX, overlayY,
                             overlayXunits, overlayYunits, screenX, screenY,
                             screenXunits, screenYunits, -1, -1, "fraction",
                             "fraction");
    }

    /**
     * Create a ScreenOverlay Element
     *
     * @param parent parent Element
     * @param name   the name of the element
     * @param iconURL  the URL of the icon
     * @param overlayX  x component of a point on the overlay image
     * @param overlayY  y component of a point on the overlay image
     * @param overlayXunits  units of overlayX, can be one of: fraction, pixels, or insetPixels
     * @param overlayYunits  units of overlayY, can be one of: fraction, pixels, or insetPixels
     * @param screenX x component of a point on the screen
     * @param screenY y component of a point on the screen
     * @param screenXunits units of screenX, can be one of: fraction, pixels, or insetPixels
     * @param screenYunits units of screenY, can be one of: fraction, pixels, or insetPixels
     * @param sizeX  the x size of the image for the screen overlay, as follows: 1 indicates to use the native dimension,
     *               0 indicates to maintain the aspect ratio, a value of n sets the value of the dimension
     * @param sizeY  the y size of the image for the screen overlay (see sizeX)
     * @param sizeXunits   units of sizeX, fraction or pixels
     * @param sizeYunits   units of sizeY, fraction or pixels
     *
     * @return the ScreenOverlay Element
     */
    public static Element screenoverlay(Element parent, String name,
                                        String iconURL, double overlayX,
                                        double overlayY,
                                        String overlayXunits,
                                        String overlayYunits, double screenX,
                                        double screenY, String screenXunits,
                                        String screenYunits, double sizeX,
                                        double sizeY, String sizeXunits,
                                        String sizeYunits) {
        Element screenOlay = makeElement(parent, TAG_SCREENOVERLAY);
        makeText(screenOlay, TAG_NAME, name);
        olayElement(screenOlay, TAG_OVERLAYXY, overlayX, overlayY,
                    overlayXunits, overlayYunits);
        olayElement(screenOlay, TAG_SCREENXY, screenX, screenY, screenXunits,
                    screenYunits);
        olayElement(screenOlay, TAG_SIZE, sizeX, sizeY, sizeXunits,
                    sizeYunits);
        Element icon = makeElement(screenOlay, TAG_ICON);
        makeText(icon, TAG_HREF, iconURL);
        return screenOlay;
    }
    /*
     <ScreenOverlay id="khScreenOverlay756">
         <name>Simple crosshairs</name>
         <description>This screen overlay uses fractional positioning
         to put the image in the exact center of the screen</description>
         <Icon>
              <href>http://myserver/myimage.jpg</href>
         </Icon>
         <overlayXY x="0.5" y="0.5" xunits="fraction" yunits="fraction"/>
         <screenXY x="0.5" y="0.5" xunits="fraction" yunits="fraction"/>
         <size x="0" y="0" xunits="pixels" yunits="pixels"/>
     </ScreenOverlay>
     */

    /**
     * Create an overlay element of the form:
     * <pre>
     * &lt;tag x="x" y="y" xunits="xunits" yunits="yunits"/&gt;
     * </pre>
     *
     * @param parent  the parent Element
     * @param tag  the type of element (TAG_OVERLAYXY, TAG_SCREENXY, TAG_SIZE)
     * @param x  the x attribute
     * @param y  the y attribute
     * @param xunits  the x units
     * @param yunits  the y units
     *
     * @return the overlay element
     */
    private static Element olayElement(Element parent, String tag, double x,
                                       double y, String xunits,
                                       String yunits) {
        Element olayElement = makeElement(parent, tag);
        olayElement.setAttribute(ATTR_X, "" + x);
        olayElement.setAttribute(ATTR_Y, "" + y);
        olayElement.setAttribute(ATTR_XUNITS, xunits);
        olayElement.setAttribute(ATTR_YUNITS, yunits);
        return olayElement;

    }
}

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
 * 
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
    public static final String TAG_KML = "kml";
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
    public static final String TAG_PLACEMARK = "Placemark";
    public static final String TAG_POINT = "Point";



    public static final String TAG_RIGHTFOV = "rightFov";
    public static final String TAG_ROLL = "roll";
    public static final String TAG_ROTATION = "rotation";
    public static final String TAG_SCHEMA = "Schema";
    public static final String TAG_SOUTH = "south";
    public static final String TAG_STYLE = "Style";
    public static final String TAG_STYLEURL = "styleUrl";
    public static final String TAG_TESSELATE = "tesselate";
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



    /** _more_          */
    public static final String TAG_TOUR = "gx:Tour";

    /** _more_          */
    public static final String TAG_PLAYLIST = "gx:Playlist";

    /** _more_          */
    public static final String TAG_FLYTO = "gx:FlyTo";

    /** _more_          */
    public static final String TAG_WAIT = "gx:Wait";




    /** _more_          */
    public static final String ATTR_ID = "id";

    /** _more_          */
    public static final String ATTR_NAME = "name";



    /**
     * _more_
     *
     * @param parent _more_
     * @param tag _more_
     *
     * @return _more_
     */
    public static Element makeElement(Element parent, String tag) {
        Element child = parent.getOwnerDocument().createElement(tag);
        parent.appendChild(child);
        return child;
    }




    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static Element kml(String name) {
        Document doc   = XmlUtil.makeDocument();
        Element  child = doc.createElement(TAG_KML);
        return child;
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static Element document(Element parent, String name) {
        return document(parent, name, false);
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param url _more_
     *
     * @return _more_
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
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static Element document(Element parent, String name,
                                   boolean visible) {
        Element node = makeElement(parent, TAG_DOCUMENT);
        name(node, name);
        visible(node, visible);
        return node;
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param tag _more_
     * @param text _more_
     *
     * @return _more_
     */
    public static Element makeText(Element parent, String tag, String text) {
        Element node     = makeElement(parent, tag);
        Text    textNode = parent.getOwnerDocument().createTextNode(text);
        node.appendChild(textNode);
        return node;
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static Element visible(Element parent, boolean visible) {
        return makeText(parent, TAG_VISIBILITY, (visible
                ? "1"
                : "0"));
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static Element open(Element parent, boolean visible) {
        return makeText(parent, TAG_OPEN, (visible
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


    /** _more_          */
    private static SimpleDateFormat sdf1;

    /** _more_          */
    private static SimpleDateFormat sdf2;

    /**
     * _more_
     *
     * @param parent _more_
     * @param dttm _more_
     *
     * @return _more_
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
     * _more_
     *
     * @param parent _more_
     * @param url _more_
     *
     * @return _more_
     */
    public static Element styleurl(Element parent, String url) {
        //<styleUrl>#linestyleExample</styleUrl>
        return makeText(parent, TAG_STYLEURL, url);
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param id _more_
     *
     * @return _more_
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
     * _more_
     *
     * @param parent _more_
     * @param id _more_
     * @param url _more_
     *
     * @return _more_
     */
    public static Element iconstyle(Element parent, String id, String url) {
        Element style     = style(parent, id);
        Element iconstyle = makeElement(style, TAG_ICONSTYLE);
        Element icon      = makeElement(iconstyle, TAG_ICON);
        Element href      = makeText(icon, TAG_HREF, url);
        return style;
    }



    /**
     * _more_
     *
     * @param parent _more_
     * @param id _more_
     * @param url _more_
     * @param scale _more_
     *
     * @return _more_
     */
    public static Element iconstyle(Element parent, String id, String url,
                                    double scale) {
        Element style     = style(parent, id);
        Element iconstyle = makeElement(style, TAG_ICONSTYLE);
        Element icon      = makeElement(iconstyle, TAG_ICON);
        Element href      = makeText(icon, TAG_HREF, url);
        makeText(iconstyle, TAG_SCALE, "" + scale);

        return style;
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param id _more_
     * @param color _more_
     * @param width _more_
     *
     * @return _more_
     */
    public static Element linestyle(Element parent, String id, Color color,
                                    int width) {
        Element style     = style(parent, id);
        Element linestyle = makeElement(style, TAG_LINESTYLE);
        if (color != null) {
            makeText(linestyle, TAG_COLOR,
                     "ff" + StringUtil.toHexString(color).substring(1));
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
     * _more_
     *
     * @param parent _more_
     * @param extrude _more_
     * @param tesselate _more_
     * @param coordinates _more_
     *
     * @return _more_
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
     * _more_
     *
     * @param parent _more_
     * @param extrude _more_
     * @param tesselate _more_
     * @param coords _more_
     *
     * @return _more_
     */
    public static Element linestring(Element parent, boolean extrude,
                                     boolean tesselate, float[][] coords) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < coords[0].length; i++) {
            sb.append(coords[1][i]);
            sb.append(",");
            sb.append(coords[0][i]);
            sb.append(",");
            sb.append(coords[2][i]);
            sb.append(" ");
        }
        return linestring(parent, extrude, tesselate, sb.toString());
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param coordinates _more_
     *
     * @return _more_
     */
    public static Element coordinates(Element parent, String coordinates) {
        Element node  = makeElement(parent, TAG_COORDINATES);
        Text textNode = parent.getOwnerDocument().createTextNode(coordinates);
        node.appendChild(textNode);
        return node;
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static Element name(Element parent, String name) {
        Element node = makeElement(parent, TAG_NAME);
        CDATASection cdata =
            parent.getOwnerDocument().createCDATASection(name);
        node.appendChild(cdata);
        return node;
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param description _more_
     *
     * @return _more_
     */
    public static Element description(Element parent, String description) {
        Element node = makeElement(parent, TAG_DESCRIPTION);
        CDATASection cdata =
            parent.getOwnerDocument().createCDATASection(description);
        node.appendChild(cdata);
        return node;
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static Element folder(Element parent, String name) {
        return folder(parent, name, false);
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static Element folder(Element parent, String name,
                                 boolean visible) {
        Element node = makeElement(parent, TAG_FOLDER);
        name(node, name);
        visible(node, visible);
        return node;
    }



    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param description _more_
     *
     * @return _more_
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
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     * @param alt _more_
     * @param style _more_
     *
     * @return _more_
     */
    public static String point(double lat, double lon, double alt,
                               String style) {
        return "<Placemark><Point><coordinates>" + lon + "," + lat + ","
               + alt + "</coordinates></Point></Placemark>";
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param description _more_
     * @param lat _more_
     * @param lon _more_
     * @param alt _more_
     * @param style _more_
     *
     * @return _more_
     */
    public static Element placemark(Element parent, String name,
                                    String description, double lat,
                                    double lon, double alt, String style) {
        Element placemark = placemark(parent, name, description);
        if (style != null) {
            makeText(placemark, TAG_STYLEURL, style);
        }
        visible(placemark, true);
        Element point = makeElement(placemark, TAG_POINT);
        makeText(point, TAG_COORDINATES, lon + "," + lat + "," + alt + " ");
        return placemark;
    }




    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param description _more_
     * @param url _more_
     * @param north _more_
     * @param south _more_
     * @param east _more_
     * @param west _more_
     *
     * @return _more_
     */
    public static Element groundOverlay(Element parent, String name,
                                        String description, String url,
                                        double north, double south,
                                        double east, double west) {
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
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param description _more_
     * @param coords _more_
     * @param color _more_
     * @param width _more_
     *
     * @return _more_
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
            for (int coordIdx = 0;
                    (coordIdx < numbers.size());
                    coordIdx++) {
                result[coordIdx][pointIdx] = new Double(
                    numbers.get(coordIdx).toString()).doubleValue();
            }
        }
        return result;
    }



}

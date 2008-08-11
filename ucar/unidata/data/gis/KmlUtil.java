/*
 * $Id: KmlDataSource.java,v 1.38 2007/04/16 20:34:52 jeffmc Exp $
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

package ucar.unidata.data.gis;


import visad.DateTime;

import ucar.unidata.xml.XmlUtil;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;
import java.awt.Color;


import org.w3c.dom.*;


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
    public static final String TAG_MULTIGEOMETRY = "MultiGeometry";
    public static final String TAG_NAME = "name";
    public static final String TAG_NEAR = "near";
    public static final String TAG_NETWORKLINK = "NetworkLink";
    public static final String TAG_NORTH = "north";
    public static final String TAG_PHOTOOVERLAY = "PhotoOverlay";
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
    public static final String TAG_VIEWBOUNDSCALE = "viewBoundScale";
    public static final String TAG_VISIBLE = "visible";
    public static final String TAG_WEST = "west";
    public static final String TAG_WHEN = "when";
    public static final String TAG_WIDTH = "width";
    //J+





    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";



    public static Element makeElement(Element parent, String tag) {
        Element child = parent.getOwnerDocument().createElement(tag);
        parent.appendChild(child);
        return child;
    }




    public static Element kml(String name) {
        Document doc = XmlUtil.makeDocument();
        Element child = doc.createElement(TAG_KML);
        return child;
    }


    public static Element document(Element parent, String name) {
        return document(parent, name, false);
    }


    public static Element document(Element parent, String name, boolean visible) {
        Element node = makeElement(parent,TAG_DOCUMENT);
        name(node,name);
        visible(node,visible);
        return node;
    }

    public static Element makeText(Element parent, String tag, String text) {
        Element node = makeElement(parent,tag);
        Text textNode = parent.getOwnerDocument().createTextNode(text);
        node.appendChild(textNode);
        return node;
    }

    public static Element visible(Element parent, boolean visible) {
        return makeText(parent,TAG_VISIBLE, (visible?"1":"0"));
    }

    public static Element timestamp(Element parent, DateTime dttm) {
        String when = dttm.formattedString("yyyy-MM-dd", DateUtil.TIMEZONE_GMT)
            + "T"
            + dttm.formattedString("HH:mm:ss", DateUtil.TIMEZONE_GMT)
            + "Z";
        Element timestamp = makeElement(parent, TAG_TIMESTAMP);
        makeText(timestamp,TAG_WHEN, when);
        return timestamp;
    }


    public static Element styleurl(Element parent, String url) {
        //<styleUrl>#linestyleExample</styleUrl>
        return makeText(parent, TAG_STYLEURL, url);
    }

    public  static Element style(Element parent, String id) {
        Element style =  makeElement(parent, TAG_STYLE);
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
    public  static Element iconstyle(Element parent, String id, String url) {
        Element style = style(parent, id);
        Element iconstyle = makeElement(style, TAG_ICONSTYLE);
        Element icon = makeElement(iconstyle, TAG_ICON);
        Element href = makeText(icon, TAG_HREF,url);
        return style;
    }


    public  static Element linestyle(Element parent, String id, Color color, int width) {
        Element style = style(parent, id);
        Element linestyle = makeElement(style, TAG_LINESTYLE);
        if(color!=null) {
            makeText(linestyle, TAG_COLOR, "ff"+StringUtil.toHexString(color).substring(1));
            makeText(linestyle, TAG_COLORMODE, "normal");
        }
        if(width>0)
            makeText(linestyle, TAG_WIDTH,""+width);
        return linestyle;
        /*<LineStyle id="ID">
  <color>ffffffff</color>            <!-- kml:color -->
  <colorMode>normal</colorMode>      <!-- colorModeEnum: normal or random -->
  <width>1</width>                   <!-- float -->
  </LineStyle>*/
     }

    public static Element linestring(Element parent, boolean extrude, boolean tesselate, String coordinates) {        Element node = makeElement(parent, TAG_LINESTRING);
        makeText(node,TAG_EXTRUDE, (extrude?"1":"0"));
        makeText(node,TAG_TESSELATE, (tesselate?"1":"0"));
        coordinates(node, coordinates);
        return node;

    }


    public static Element linestring(Element parent, boolean extrude, boolean tesselate, float[][]coords) {
        StringBuffer sb= new StringBuffer();
        for(int i=0;i<coords[0].length;i++) {
            sb.append(coords[1][i]);
            sb.append(",");
            sb.append(coords[0][i]);
            sb.append(",");
            sb.append(coords[2][i]);
            sb.append(" ");
        }
        return linestring(parent, extrude, tesselate, sb.toString());
    }


    public static Element coordinates(Element parent, String coordinates) {
        Element node = makeElement(parent, TAG_COORDINATES);
        Text textNode = parent.getOwnerDocument().createTextNode(coordinates);
        node.appendChild(textNode);
        return node;
    }

    public static Element name(Element parent, String name) {
        Element node = makeElement(parent,TAG_NAME);
        CDATASection cdata = parent.getOwnerDocument().createCDATASection(name);
        node.appendChild(cdata);
        return node;
    }

    public static Element description(Element parent, String description) {
        Element node = makeElement(parent,TAG_DESCRIPTION);
        CDATASection cdata = parent.getOwnerDocument().createCDATASection(description);
        node.appendChild(cdata);
        return node;
    }






    public static Element folder(Element parent, String name) {
        return folder(parent, name, false);
    }

    public static Element folder(Element parent, String name, boolean visible) {
        Element node = makeElement(parent,TAG_FOLDER);
        name(node, name);
        visible(node,visible);
        return node;
    }



    public static Element placemark(Element parent, String name, String description) {
        Element node = makeElement(parent,TAG_PLACEMARK);
        name(node, name);
        description(node, description);
        return node;
    }




    public static Element placemark(Element parent, String name, String description, visad.georef.EarthLocation el, String style) {
        Element placemark = placemark(parent, name, description);
        makeText(placemark, TAG_STYLEURL, style);
        visible(placemark, true);
        Element point = makeElement(placemark, TAG_POINT);
        makeText(point, TAG_COORDINATES, el.getLongitude().getValue() +"," +
                 el.getLatitude().getValue() +"," +
                 el.getAltitude().getValue() +" ");
        return placemark;
    }


    public static Element placemark(Element parent, String name, String description, float[][]coords, Color color, int width) {
        Element placemark = placemark(parent, name, description);
        Element linestring = linestring(placemark,false,false, coords);
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


}


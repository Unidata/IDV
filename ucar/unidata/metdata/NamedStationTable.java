/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

package ucar.unidata.metdata;


import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import visad.CommonUnit;
import visad.Real;
import visad.RealType;
import visad.Unit;


import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


/**
 * Station table that holds a set of NamedStations
 *
 * @author IDV Development Team
 * @version $Revision: 1.46 $ $Date: 2007/06/08 21:10:35 $
 */
public class NamedStationTable extends StationTableImpl {

    /** valid flag */
    private boolean valid = true;


    /** type of table */
    private String type;

    /** name of table */
    private String name = null;

    /** table description */
    private String description = null;

    /** counter */
    private int counter = 0;

    /** category */
    private String category;

    /** base */
    private String base;

    /** station tables tag */
    private static final String TAG_STATIONTABLES = "stationtables";

    /** coordinates */
    private static final String TAG_COORDINATES = "coordinates";


    /** station table tag */
    private static final String TAG_STATIONTABLE = "stationtable";

    /** station tag */
    private static final String TAG_STATION = "station";

    /** tag for Yahoo ResultSet */
    private static final String TAG_YRESULTSET = "ResultSet";

    /** tag for Yahoo Result */
    private static final String TAG_YRESULT = "Result";

    /** tag for Yahoo latitude */
    private static final String TAG_YLATITUDE = "Latitude";

    /** tag for Yahoo Longitude */
    private static final String TAG_YLONGITUDE = "Longitude";

    /** tag for Yahoo Title */
    private static final String TAG_YTITLE = "Title";

    /** tag for Yahoo Address */
    private static final String TAG_YADDRESS = "Address";

    /** tag for Yahoo city */
    private static final String TAG_YCITY = "City";

    /** tag for Yahoo state */
    private static final String TAG_YSTATE = "State";

    /** tag for Yahoo phone */
    private static final String TAG_YPHONE = "Phone";


    /** tag for channel */
    private static final String TAG_CHANNEL = "channel";

    /** tag for dc:subject */
    private static final String TAG_DC_SUBJECT = "dc:subject";

    /** tag for RSS */
    private static final String TAG_RSS = "rss";

    /** tag for link */
    private static final String TAG_LINK = "link";

    /** tag for item */
    private static final String TAG_ITEM = "item";

    /** tag for geo:lat */
    private static final String TAG_GEO_LAT = "geo:lat";

    /** tag for geo:long */
    private static final String TAG_GEO_LONG = "geo:long";

    /** tag for description */
    private static final String TAG_DESCRIPTION = "description";

    /** title tag */
    private static final String TAG_TITLE = "title";

    /** stn tag */
    private static final String TAG_STN = "stn";

    /** location tag */
    private static final String TAG_LOCATION = "location";

    /** name attribute */
    private static final String ATTR_NAME = "name";

    /** description attribute */
    private static final String ATTR_DESCRIPTION = "description";

    /** type attribute */
    private static final String ATTR_TYPE = "type";

    /** category attribute */
    private static final String ATTR_CATEGORY = "category";

    /** id attribute */
    private static final String ATTR_ID = "id";

    /** latitude attribute */
    private static final String ATTR_LAT = "lat";

    /** longitude attribute */
    private static final String ATTR_LON = "lon";

    /** elevation attribute */
    private static final String ATTR_ELEV = "elev";


    /** elevation unit attribute */
    private static final String ATTR_ELEVUNIT = "elevunit";


    /** Should we keep loading more xml */
    private static final String ATTR_LOADMORE = "loadmore";

    /** Key for Station number in properties */
    public static final String KEY_IDNUMBER = "idn";

    /** Key for state in properties */
    public static final String KEY_STATE = "st";

    /** Key for country in properties */
    public static final String KEY_COUNTRY = "co";

    /** Key for bulletin id in properties */
    public static final String KEY_BULLETIN = "bull";

    /** Key for priority properties */
    public static final String KEY_PRIORITY = "pri";

    /** Key for priority properties */
    public static final String KEY_EXTRA = "extra";

    /** The resource id */
    private String id;


    /** Track whether we have processed the xml yet */
    private boolean haveCreatedMap = false;

    /** root element */
    private List roots = new ArrayList();

    /**
     * Create a NamedStationTable from an XML specification
     *
     * @param root   root element for XML
     */
    public NamedStationTable(Element root) {
        super();
        if (root != null) {
            roots.add(root);
        }
    }


    /**
     * Default constructor
     */
    public NamedStationTable() {
        this((String) null);
    }


    /**
     * Create a new NamedStationTable with <code>name</code>
     *
     * @param name    name of the table
     */
    public NamedStationTable(String name) {
        super();
        setName((name != null)
                ? name
                : "Table " + counter++);
    }



    /**
     * Add the given root element to the list of roots
     *
     * @param root The xml root
     */
    private void addRoot(Element root) {
        if (root != null) {
            roots.add(root);
        }
    }

    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
    }


    /**
     * Set the name of the station table
     *
     * @param name  new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of the table
     *
     * @return  table name
     */
    public String getName() {
        return name;
    }



    /**
     * Get the full name of the table. This is the category concatenated with  the name
     *
     * @return  table name
     */
    public String getFullName() {
        if (category != null) {
            return category + "::" + name;
        }
        return name;
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



    /**
     * Get the station xml
     *
     * @param name   name of the document
     * @param category  the category
     * @param stations  the list of stations
     *
     * @return  the XML as a string
     */
    public static String getStationXml(String name, String category,
                                       List stations) {
        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER);
        sb.append("\n");
        String attrs = XmlUtil.attr(ATTR_NAME, name);
        if (category != null) {
            attrs = attrs + XmlUtil.attr(ATTR_CATEGORY, category);
        }
        sb.append(XmlUtil.openTag(TAG_STATIONTABLE, attrs));
        sb.append("\n");
        for (int i = 0; i < stations.size(); i++) {
            NamedStationImpl station = (NamedStationImpl) stations.get(i);
            sb.append(XmlUtil.tag(TAG_STATION,
                                  XmlUtil.attrs(ATTR_ID, station.getID(),
                                      ATTR_NAME, station.getName(), ATTR_LAT,
                                      station.getLatitude()
                                      + "") + XmlUtil.attrs(ATTR_LON,
                                          station.getLongitude() + "",
                                          ATTR_ELEV,
                                          station.getAltitude() + "")));

            sb.append("\n");
        }
        sb.append(XmlUtil.closeTag(TAG_STATIONTABLE));
        sb.append("\n");
        return sb.toString();
    }


    /**
     * Is this a KML list of locations
     *
     * @param filename  name of the file
     *
     * @return  true if identified as KML
     */
    private static boolean isKml(String filename) {
        //return (filename.toLowerCase().endsWith(".kml")
        //        || filename.toLowerCase().endsWith(".kmz"));
        return (IOUtil.hasSuffix(filename, ".kml")
                || IOUtil.hasSuffix(filename, ".kmz"));
    }

    /**
     * Is this a GEMPAK list of locations
     *
     * @param filename  name of the file
     *
     * @return  true if identified as GEMPAK station file
     */
    private static boolean isGempak(String filename) {
        return IOUtil.hasSuffix(filename, ".tbl");
    }


    /**
     * Create a list of NamedStationTables from a set of resources
     *
     * @param xrc     XML resources
     * @return  List of NamedStationTables
     */
    public static List createStationTables(XmlResourceCollection xrc) {

        Hashtable   nameToTable = new Hashtable();
        List        tables      = new ArrayList();
        List        types       = new ArrayList();
        List        hrefBases   = new ArrayList();
        XmlNodeList tableRoots  = new XmlNodeList();
        List        resourceIds = new ArrayList();
        for (int i = 0; i < xrc.size(); i++) {
            String name  = xrc.get(i).toString();
            String title = xrc.getProperty("name", i);
            if (title == null) {
                title = IOUtil.stripExtension(IOUtil.getFileTail(name));
            }
            String type     = xrc.getProperty("type", i);
            String category = xrc.getProperty("category", i);
            if (name.toLowerCase().endsWith(".csv")) {
                try {
                    String csv = xrc.read(i);
                    if (csv != null) {
                        NamedStationTable table =
                            new NamedStationTable(title);
                        table.createStationTableFromCsv(csv);
                        tables.add(table);
                        table.setType(type);
                        table.setCategory(category);
                    }
                } catch (Exception exc) {
                    System.err.println("error processing locations file:"
                                       + name);
                    exc.printStackTrace();
                }
                continue;
            }

            if (isKml(name)) {
                try {
                    NamedStationTable table =
                        createStationTableFromFile(xrc.get(i).toString());
                    if ((table != null) && table.valid) {
                        tables.add(table);
                        table.setType(type);
                        table.setCategory(category);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
                continue;
            }

            if (isGempak(name)) {
                try {
                    String tbl = xrc.read(i);
                    if (tbl != null) {
                        NamedStationTable table =
                            new NamedStationTable(title);
                        Trace.call1("creating Gempak station table " + name);
                        table.createStationTableFromGempak(tbl);
                        Trace.call2("creating Gempak station table " + name);
                        tables.add(table);
                        table.setType(type);
                        table.setCategory(category);
                    }
                } catch (Exception exc) {
                    System.err.println("error processing locations file:"
                                       + name);
                    exc.printStackTrace();
                }
                continue;
            }

            Element root = xrc.getRoot(i);
            if (root == null) {
                continue;
            }
            String tag        = root.getTagName();
            String resourceId = xrc.getResourceId(i);
            if (Misc.equals(tag, TAG_STATIONTABLES)) {
                List children = XmlUtil.getElements(root, TAG_STATIONTABLE);
                for (int childIdx = 0; childIdx < children.size();
                        childIdx++) {
                    tableRoots.add(children.get(childIdx));
                    types.add(type);
                    resourceIds.add(resourceId);
                    hrefBases.add(XmlUtil.getAttribute(root, "base",
                            (String) null));
                }
            } else {
                tableRoots.add(root);
                types.add(type);
                resourceIds.add(resourceId);
                hrefBases.add(null);
            }
        }


        for (int tableIdx = 0; tableIdx < tableRoots.size(); tableIdx++) {
            Element root = (Element) tableRoots.get(tableIdx);
            String  type = (String) types.get(tableIdx);
            String  name = XmlUtil.getAttribute(root, ATTR_NAME, "");
            String desc = XmlUtil.getAttribute(root, ATTR_DESCRIPTION,
                              (String) null);
            String category = XmlUtil.getAttribute(root, ATTR_CATEGORY,
                                  (String) null);
            String            base  = (String) hrefBases.get(tableIdx);
            String            key   = name + "_" + category;
            NamedStationTable table =
                (NamedStationTable) nameToTable.get(key);
            if (table == null) {
                table = createStationTable(root);
                if (table.getType() == null) {
                    table.setType(type);
                }
                table.setId((String) resourceIds.get(tableIdx));
                table.setDescription(desc);
                if (base != null) {
                    table.base = base;
                }
                nameToTable.put(key, table);
                if (table != null) {
                    tables.add(table);
                }
            } else {
                //              System.err.println ("Adding extra " + key);
                table.addRoot(root);
            }
        }
        return tables;

    }


    /**
     * Make stations from the root element.
     *
     * @param root   root element
     */
    private void makeStations(Element root) {
        Trace.call1("NamedStationTable.makeStations");
        makeStationsInner(root);
        Trace.call2("NamedStationTable.makeStations");
    }


    /**
     * Make stations from the root element
     *
     * @param root  root station table element
     */
    private void makeStationsInner(Element root) {

        try {
            String desc = XmlUtil.getAttribute(root, ATTR_DESCRIPTION,
                              (String) null);
            if (desc != null) {
                description = desc;
            }
            //      System.err.println ("desc:" + desc);
            NodeList children = root.getChildNodes();
            Unit     elevUnit = CommonUnit.meter;
            String elevUnitStr = XmlUtil.getAttribute(root, ATTR_ELEVUNIT,
                                     (String) null);
            if (elevUnitStr != null) {
                elevUnit = ucar.visad.Util.parseUnit(elevUnitStr);
            }
            for (int childIdx = 0; childIdx < children.getLength();
                    childIdx++) {
                Node   node    = children.item(childIdx);
                String tagName = node.getNodeName();
                if ( !(tagName.equals(TAG_STATION) || tagName.equals(TAG_STN)
                        || tagName.equals(TAG_LOCATION))) {
                    continue;
                }
                String id = XmlUtil.getAttribute(node, ATTR_ID,
                                (String) null);
                String stationName = XmlUtil.getAttribute(node, ATTR_NAME,
                                         id);
                if (stationName == null) {
                    stationName = "location";
                }
                if (id == null) {
                    id = stationName;
                }
                Element coordNode   = XmlUtil.findChild(node,
                                          TAG_COORDINATES);
                String  coordString = ((coordNode != null)
                                       ? XmlUtil.getChildText(coordNode)
                                       : null);
                List    coords      = null;
                double
                    lat             = 0.0,
                    lon             = 0.0,
                    alt             = 0.0;

                String elev         = null;
                if (coordString == null) {
                    lat = Misc.decodeLatLon(XmlUtil.getAttribute(node,
                            ATTR_LAT));
                    lon = Misc.decodeLatLon(XmlUtil.getAttribute(node,
                            ATTR_LON));
                } else {
                    double[][] tmp = StringUtil.parseCoordinates(coordString);
                    coords = new ArrayList();
                    coords.add(tmp);
                }
                elev = XmlUtil.getAttribute(node, ATTR_ELEV, (String) null);

                Unit tmpElevUnit = elevUnit;
                if ((elev != null) && (elev.trim().length() > 0)) {
                    try {
                        Real tmp = ucar.visad.Util.toReal(elev);
                        alt = tmp.getValue();
                        //hackage with the UniversalUnit check
                        if ((tmp.getUnit() != null)
                                && !tmp.getUnit().toString().equals(
                                    "UniversalUnit")) {
                            tmpElevUnit = tmp.getUnit();
                        }
                    } catch (Exception exc) {
                        System.err.println("error parsing elevation:" + elev);
                    }
                }

                try {
                    NamedStationImpl station;
                    if (coords != null) {
                        station = new NamedStationImpl(id, stationName,
                                coords, tmpElevUnit);
                        station.addProperty(ATTR_LON,
                                            station.getLongitude() + "");
                        station.addProperty(ATTR_LAT,
                                            station.getLatitude() + "");
                        station.addProperty(ATTR_ELEV, station.getAltitude());
                    } else {
                        station = new NamedStationImpl(id, stationName, lat,
                                lon, alt, tmpElevUnit);
                    }


                    String childText = XmlUtil.getChildText(node).trim();
                    if (childText.length() > 0) {
                        station.addProperty("description", childText);
                    }
                    NamedNodeMap attrs    = node.getAttributes();
                    int          numAttrs = attrs.getLength();
                    for (int i = 0; i < numAttrs; i++) {
                        Attr   attr     = (Attr) attrs.item(i);
                        String attrName = attr.getNodeName();
                        if (attrName.equals(ATTR_ELEV)) {
                            station.addProperty(attrName,
                                    new Real(RealType.Altitude, alt,
                                             tmpElevUnit));
                        } else {
                            station.addProperty(attrName,
                                    attr.getNodeValue());
                        }
                    }
                    this.add(station, true);
                } catch (Exception excp) {
                    System.err.println("Error reading locations:" + excp);
                    excp.printStackTrace();
                    return;
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }


    }





    /**
     * Creates a station table from an RSS XML file that (hopefully) contains
     * geo:lat attributes
     *
     * @param root   root XML element of the RSS
     *
     */
    private void makeStationsFromRss(Element root) {
        try {
            Element channel;
            if (root.getTagName().equals(TAG_CHANNEL)) {
                channel = root;
            } else {
                channel = XmlUtil.findChild(root, TAG_CHANNEL);
            }

            if (channel == null) {
                return;
            }
            String mainTitle = XmlUtil.getGrandChildText(channel, TAG_TITLE);
            if (mainTitle != null) {
                setName(mainTitle);
            }
            description = XmlUtil.getGrandChildText(channel, TAG_DESCRIPTION);
            NodeList children = channel.getChildNodes();

            for (int childIdx = 0; childIdx < children.getLength();
                    childIdx++) {
                Node   itemNode = children.item(childIdx);
                String tagName  = itemNode.getNodeName();
                if ( !(tagName.equals(TAG_ITEM))) {
                    continue;
                }
                String  title = XmlUtil.getGrandChildText(itemNode,
                                    TAG_TITLE);
                Element latNode = XmlUtil.findChild(itemNode, TAG_GEO_LAT);
                Element lonNode = XmlUtil.findChild(itemNode, TAG_GEO_LONG);
                if ((title == null) || (latNode == null)
                        || (lonNode == null)) {
                    continue;
                }
                String id  = title;
                double lat = Misc.decodeLatLon(XmlUtil.getChildText(latNode));
                double lon = Misc.decodeLatLon(XmlUtil.getChildText(lonNode));
                double alt = 0.0;
                try {
                    NamedStationImpl station = new NamedStationImpl(title,
                                                   title, lat, lon, alt,
                                                   CommonUnit.meter);
                    String description = XmlUtil.getGrandChildText(itemNode,
                                             TAG_DESCRIPTION);
                    if (description != null) {
                        station.addProperty("description", description);
                    }
                    String link = XmlUtil.getGrandChildText(itemNode,
                                      TAG_LINK);
                    if (link != null) {
                        station.addProperty("link", link);
                    }
                    List subjects = XmlUtil.findChildren(itemNode,
                                        TAG_DC_SUBJECT);
                    for (int i = 0; i < subjects.size(); i++) {
                        station.addProperty(
                            "subject" + (i + 1),
                            XmlUtil.getChildText((Element) subjects.get(i)));
                    }

                    this.add(station, true);

                } catch (Exception excp) {
                    System.out.println("bad station id=" + id + "name = "
                                       + mainTitle + "lat = " + lat
                                       + "lon = " + lon + "alt = " + alt);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }


    /**
     * Creates a station table from a Yahoo query XML file
     *
     * @param root   root XML element of the Yahoo query
     *
     */
    private void makeStationsFromYahooQuery(Element root) {
        try {
            setName("Yahoo Query");
            List results = XmlUtil.findChildren(root, TAG_YRESULT);
            String[] attrTags = { TAG_YADDRESS, TAG_YCITY, TAG_YSTATE,
                                  TAG_YPHONE };

            for (int resultIdx = 0; resultIdx < results.size(); resultIdx++) {
                Node resultNode = (Node) results.get(resultIdx);
                String title = XmlUtil.getGrandChildText(resultNode,
                                   TAG_YTITLE);
                if (title == null) {
                    title = "name";
                }
                double lat =
                    Misc.decodeLatLon(XmlUtil.getGrandChildText(resultNode,
                        TAG_YLATITUDE));
                double lon =
                    Misc.decodeLatLon(XmlUtil.getGrandChildText(resultNode,
                        TAG_YLONGITUDE));
                double alt = 0.0;
                try {
                    NamedStationImpl station = new NamedStationImpl(title,
                                                   title, lat, lon, alt,
                                                   CommonUnit.meter);
                    StringBuffer sb = new StringBuffer();
                    for (int attrIdx = 0; attrIdx < attrTags.length;
                            attrIdx++) {
                        String text = XmlUtil.getGrandChildText(resultNode,
                                          attrTags[attrIdx]);
                        if (text != null) {
                            sb.append(attrTags[attrIdx] + ": " + text
                                      + "<br>");
                        }
                    }
                    station.addProperty("description", sb.toString());
                    this.add(station, true);

                } catch (Exception excp) {
                    System.out.println("bad station id=" + title);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }





    /*
     * private static NamedStationTable createWorldWindTable(Element root)
     *   throws Exception {
     *   //              <Icon ShowAtStartup="true">
     *   String tableName = XmlUtil.getAttribute(root, "Name");
     *   NamedStationTable table =new NamedStationTable(tableName);
     *   List iconNodes = XmlUtil.findDescendants(root,"Icon");
     *   for(int i=0;i<iconNodes.size();i++) {
     *       Element iconNode = (Element) iconNodes.get(i);
     *       Element nameNode = XmlUtil.findChild(iconNode, "Name");
     *       String name= XmlUtil.getChildText(nameNode);
     *       Element latNode = XmlUtil.findChild(XmlUtil.findChild(iconNode, "Latitude"), "Value");
     *       Element lonNode = XmlUtil.findChild(XmlUtil.findChild(iconNode, "Longitude"), "Value");
     *       double lat = Misc.decodeLatLon(XmlUtil.getChildText(latNode).trim());
     *       double lon = Misc.decodeLatLon(XmlUtil.getChildText(lonNode).trim());
     *       double alt = new Double(XmlUtil.getChildText(XmlUtil.findChild(iconNode,"DistanceAboveSurface"))).doubleValue();
     *       NamedStationImpl station = new NamedStationImpl(name,
     *                                                       name, lat, lon,
     *                                                       alt, CommonUnit.meter);
     *       table.add(station, true);
     *   }
     *   return table;
     * }
     */

    /**
     * Create a station table from KML
     *
     * @param filename
     *
     * @throws Exception problem reading th estation
     */
    public void createStationTableFromKmlFile(String filename)
            throws Exception {
        String kml = null;
        //if (filename.toLowerCase().endsWith(".kmz")) {
        if (IOUtil.hasSuffix(filename, ".kmz")) {
            InputStream is = IOUtil.getInputStream(filename);
            if (is == null) {
                valid = false;
                return;
            }
            BufferedInputStream bin = new BufferedInputStream(is);
            ZipInputStream      zin = new ZipInputStream(bin);
            ZipEntry            ze;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName().toLowerCase();
                if (name.toLowerCase().endsWith(".kml")) {
                    kml = new String(IOUtil.readBytes(zin, null, false));
                    break;
                }
            }
        } else {
            kml = IOUtil.readContents(filename, NamedStationTable.class);
        }
        if (kml == null) {
            valid = false;
            return;
        }
        Element root = XmlUtil.getRoot("xml:" + kml, NamedStationTable.class);
        if (root == null) {
            return;
        }

        List nodes = XmlUtil.findDescendants(root, "Placemark");
        for (int i = 0; i < nodes.size(); i++) {
            Element node       = (Element) nodes.get(i);
            Element coordsNode = XmlUtil.findDescendant(node, "coordinates");
            if (coordsNode == null) {
                continue;
            }
            String coords = XmlUtil.getChildText(coordsNode);
            if (coords == null) {
                continue;
            }
            String name = XmlUtil.getGrandChildText(node, "name");
            if (name == null) {
                continue;
            }
            List toks = StringUtil.split(coords, ",", true, true);
            if (toks.size() <= 1) {
                continue;
            }
            double lon = Misc.decodeLatLon(toks.get(0).toString());
            double lat = Misc.decodeLatLon(toks.get(1).toString());
            double altitude = ((toks.size() >= 3)
                               ? Misc.parseDouble(toks.get(2).toString())
                               : 0);
            String desc = XmlUtil.getGrandChildText(node, "description");
            NamedStationImpl station = new NamedStationImpl(name, name, lat,
                                           lon, altitude, CommonUnit.meter);
            this.add(station, true);
            if (desc != null) {
                station.addProperty("description", desc);
            }

        }

    }


    /**
     * Create a station table from a csv string
     *
     * @param csv The actual csv text
     *
     * @throws Exception problem creating table from file
     */
    public void createStationTableFromCsv(String csv) throws Exception {

        List lines = StringUtil.split(csv, "\n");
        if (lines.size() == 0) {
            return;
        }
        List names = StringUtil.split((String) lines.get(0), ",", true, true);
        int  latIndex   = -1;
        int  lonIndex   = -1;
        int  altIndex   = -1;
        int  titleIndex = -1;
        for (int i = 0; i < names.size(); i++) {
            String name = (String) names.get(i);
            name = name.toLowerCase().trim();
            if (name.equals("latitude") && (latIndex == -1)) {
                latIndex = i;
            }
            if (name.equals("longitude") && (lonIndex == -1)) {
                lonIndex = i;
            }
            if (name.equals("lat") && (latIndex == -1)) {
                latIndex = i;
            }
            if (name.equals("long") && (lonIndex == -1)) {
                lonIndex = i;
            }
            if (name.equals("lon") && (lonIndex == -1)) {
                lonIndex = i;
            }
            if ((name.startsWith("alt") || name.startsWith("altitude"))
                    && (altIndex == -1)) {
                altIndex = i;
                //              int bracketIdx = name.indexOf("[");
            } else if (name.startsWith("elev") && (altIndex == -1)) {
                altIndex = i;
                //              int bracketIdx = name.indexOf("[");
            }
            if (name.equals("id") || name.startsWith("station")
                    || name.startsWith("site")) {
                titleIndex = i;

            }
            if ((i != latIndex) && (i != lonIndex) && (i != altIndex)
                    && (titleIndex == -1)) {
                titleIndex = i;
            }
        }

        if (latIndex == -1) {
            throw new IllegalStateException(
                "Unable to determine the latitude value");
        }
        if (lonIndex == -1) {
            throw new IllegalStateException(
                "Unable to determine the longitude value");
        }
        if (titleIndex == -1) {
            throw new IllegalStateException(
                "Unable to determine the name value");
        }


        //ICAO code,Airport Name,Latitude,Longitude
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).toString().trim();
            if (line.length() == 0) {
                continue;
            }
            List toks = StringUtil.split(line, ",", true, false);
            if (toks.size() == 0) {
                continue;
            }
            if (toks.size() != names.size()) {
                throw new IllegalStateException("CSV line: " + lines.get(i)
                        + " does not have correct number of elements. Has:"
                        + toks.size() + " looking for:" + names.size());
            }
            double lat      = Misc.decodeLatLon((String) toks.get(latIndex));
            double lon      = Misc.decodeLatLon((String) toks.get(lonIndex));
            double altitude = 0.0;
            Unit   altUnit  = CommonUnit.meter;
            if (altIndex != -1) {
                String tok = (String) toks.get(altIndex);
                Real   tmp = ucar.visad.Util.toReal(tok);
                altitude = tmp.getValue();
                if (tok.indexOf("[") >= 0) {
                    altUnit = tmp.getUnit();
                }
            }
            String title = (String) toks.get(titleIndex);
            NamedStationImpl station = new NamedStationImpl(title, title,
                                           lat, lon, altitude, altUnit);
            for (int j = 0; j < toks.size(); j++) {
                if ((j != latIndex) && (j != lonIndex) && (j != titleIndex)
                        && (j != altIndex)) {
                    station.addProperty((String) names.get(j), toks.get(j));
                }
            }


            this.add(station, true);
        }

    }


    /**
     * Create a station table from a Gempak table
     *
     *
     * @param tbl The actual Gempak table as text
     *
     * @throws Exception problem creating table from file
     */
    public void createStationTableFromGempak(String tbl) throws Exception {

        List lines = StringUtil.split(tbl, "\n", false, true);
        if (lines.size() == 0) {
            return;
        }
        int[] indices = {
            0, 9, 16, 49, 52, 55, 61, 68
        };
        int[] lengths = {
            8, 6, 32, 2, 2, 5, 6, 5
        };
        // List toks = StringUtil.parseLineWords(tbl, indices, lengths,"\n", "!", true);

        /*
          STID    STNM   NAME                             ST CO   LAT    LON  ELV (PRI) (EXTRA)
          (4 or 8) (6)   (32)                            (2)(2)   (5)    (6)   (5)(2) (rest)
        */

        //ICAO code,Airport Name,Latitude,Longitude
        boolean readOne    = false;
        int     idIndex    = 0;
        int     idnIndex   = 1;
        int     nameIndex  = 2;
        int     stIndex    = 3;
        int     coIndex    = 4;
        int     latIndex   = 5;
        int     lonIndex   = 6;
        int     altIndex   = 7;
        int     priIndex   = 8;
        int     extraIndex = 9;
        int     numToks    = indices.length;
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.get(i);
            //System.out.println(">" + line + "<");
            if (line.startsWith("!")) {
                continue;  // comment
            }
            if ( !readOne) {
                String station = line.substring(0, 8);
                if (station.substring(4, 8).equals("    ")
                        || !station.substring(4, 5).equals(" ")) {
                    //System.out.println("new format");
                } else {
                    //System.out.println("old format");
                    indices = new int[] {
                        0, 5, 12, 45, 48, 51, 57, 64
                    };
                    lengths = new int[] {
                        4, 6, 32, 2, 2, 5, 6, 5
                    };
                }
                readOne = true;
            }
            String[] words = new String[numToks + 2];
            try {
                for (int idx = 0; idx < indices.length; idx++) {
                    words[idx] = line.substring(indices[idx],
                            indices[idx] + lengths[idx]);
                    if (true) {  // always trim?
                        words[idx] = words[idx].trim();
                    }
                }
                int lastRead = indices[numToks - 1] + lengths[numToks - 1];
                // get the priority and extra stuff if it exists
                if (line.length() > lastRead) {
                    String rest = line.substring(lastRead,
                                      line.length()).trim();
                    int end = Math.min(2, rest.length());
                    // get the priority if it exists
                    if ( !rest.equals("")) {
                        words[priIndex] = rest.substring(0, end);
                    }
                    if (rest.length() > 2) {
                        words[numToks] = rest.substring(2, rest.length());
                    }
                }

                String id   = words[idIndex];
                String name = words[nameIndex];
                double lat = Misc.parseDouble(words[latIndex]) / 100.;
                double lon = Misc.parseDouble(words[lonIndex]) / 100.;
                double alt = Misc.parseDouble(words[altIndex]) / 100.;
                NamedStationImpl station = new NamedStationImpl(id, name,
                                               lat, lon, alt,
                                               CommonUnit.meter);
                station.addProperty(KEY_IDNUMBER, words[idnIndex]);
                station.addProperty(KEY_STATE, words[stIndex]);
                station.addProperty(KEY_COUNTRY, words[coIndex]);
                if (words[priIndex] != null) {
                    station.addProperty(KEY_PRIORITY, words[priIndex]);
                }
                if (words[extraIndex] != null) {
                    station.addProperty(KEY_EXTRA, words[extraIndex]);
                }
                this.add(station, true);
            } catch (Exception e) {
                System.out.println("Unable to parse station [" + i
                                   + "] for:\n" + line);
                System.err.println("error:" + e);
                continue;
            }

        }


    }




    /**
     * Create a station table from a Gempak bulletin table
     *
     *
     * @param tbl The actual Gempak bulletin table as text
     *
     * @throws Exception problem creating table from file
     */
    public void createStationTableFromBulletin(String tbl) throws Exception {
        /*
!BULL  KSTN     NAME                             ST CO   LAT    LON  ELEV
!(6)   (8)      (32)                            (2)(2)   (5)    (6)   (5)
FXAK61 PAFC     ALASKA/PACIFIC_RFC               AK US  6115 -14997     0
        */

        List lines = StringUtil.split(tbl, "\n", false, true);
        if (lines.size() == 0) {
            return;
        }
        /* need to parse these on white space since they  are not consistent
        int[] indices = {
            0, 7, 15, 48, 52, 55, 61, 68
        };
        int[] lengths = {
            6, 8, 32, 2, 2, 5, 6, 5
        };
        */

        //ICAO code,Airport Name,Latitude,Longitude
        int bullIndex  = 0;
        int idIndex    = 1;
        int nameIndex  = 2;
        int stIndex    = 3;
        int coIndex    = 4;
        int latIndex   = 5;
        int lonIndex   = 6;
        int altIndex   = 7;
        int priIndex   = 8;
        int extraIndex = 9;
        int numToks    = 8;  // change this if use pri and extra
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.get(i);
            if (line.startsWith("!")) {
                continue;  // comment
            }
            String[] words = null;
            try {
                words = line.split("\\s++", numToks);
                if (words.length < numToks) {
                    System.err.println("invalid line: " + line);
                    continue;
                }

                String id   = words[idIndex];
                String name = words[nameIndex];
                double lat = Misc.parseDouble(words[latIndex]) / 100.;
                double lon = Misc.parseDouble(words[lonIndex]) / 100.;
                double alt = Misc.parseDouble(words[altIndex]) / 100.;
                NamedStationImpl station = new NamedStationImpl(id, name,
                                               lat, lon, alt,
                                               CommonUnit.meter);
                station.addProperty(KEY_BULLETIN, words[bullIndex]);
                station.addProperty(KEY_STATE, words[stIndex]);
                station.addProperty(KEY_COUNTRY, words[coIndex]);
                /* Bulletins don't have this
                if (words.length > priIndex && words[priIndex] != null) {
                    station.addProperty(KEY_PRIORITY, words[priIndex]);
                }
                if (words.length > extraIndex && words[extraIndex] != null) {
                    station.addProperty(KEY_EXTRA, words[extraIndex]);
                }
                */
                this.add(station, true);
            } catch (Exception e) {
                System.out.println("Unable to parse station [" + i
                                   + "] for:\n" + line);
                System.err.println("error:" + e);
                continue;
            }

        }


    }





    /**
     * Create a station table from a file
     *
     * @param filename filename
     *
     * @return NamedStationTable
     *
     * @throws Exception problem creating table from file
     */
    public static NamedStationTable createStationTableFromFile(
            String filename)
            throws Exception {

        if (filename.indexOf("<" + TAG_STATIONTABLE) >= 0) {
            return createStationTable(filename);
        }

        //if (filename.toLowerCase().endsWith(".csv")) {
        if (IOUtil.hasSuffix(filename, ".csv")) {
            NamedStationTable table = new NamedStationTable(
                                          IOUtil.stripExtension(
                                              IOUtil.getFileTail(filename)));

            table.createStationTableFromCsv(IOUtil.readContents(filename,
                    NamedStationTable.class));
            return table;
        }

        //if (filename.toLowerCase().endsWith(".xls")) {
        if (IOUtil.hasSuffix(filename, ".xls")) {
            NamedStationTable table = new NamedStationTable(
                                          IOUtil.stripExtension(
                                              IOUtil.getFileTail(filename)));

            String csv = DataUtil.xlsToCsv(filename);
            table.createStationTableFromCsv(csv);
            return table;
        }




        if (isKml(filename)) {
            NamedStationTable table = new NamedStationTable(
                                          IOUtil.stripExtension(
                                              IOUtil.getFileTail(filename)));

            table.createStationTableFromKmlFile(filename);
            return table;
        }

        if (isGempak(filename)) {
            NamedStationTable table = new NamedStationTable(
                                          IOUtil.stripExtension(
                                              IOUtil.getFileTail(filename)));

            table.createStationTableFromGempak(IOUtil.readContents(filename,
                    NamedStationTable.class));
            return table;
        }

        return createStationTable(XmlUtil.getRoot(filename,
                NamedStationTable.class));
    }



    /**
     * Creates a station table from an XML file with station information,
     * such as profilerstns.xml
     *
     * @param xml xml
     *
     * @return a NamedStationTable filled with data if possible
     *
     * @throws Exception  problem creating table
     */
    public static NamedStationTable createStationTable(String xml)
            throws Exception {
        Element root = XmlUtil.getRoot(xml);
        return createStationTable(root);
    }



    /**
     * Creates a station table from an XML file with station information,
     * such as profilerstns.xml
     *
     * @param root   root XML element defining table
     *
     * @return a NamedStationTable filled with data if possible
     */
    public static NamedStationTable createStationTable(Element root) {
        if (root == null) {
            return null;
        }

        if (root.getTagName().equals(TAG_RSS)
                || root.getTagName().equals(TAG_CHANNEL)) {
            NamedStationTable table = new NamedStationTable();
            table.makeStationsFromRss(root);
            return table;
        }

        if (root.getTagName().equals(TAG_YRESULTSET)) {
            NamedStationTable table = new NamedStationTable();
            table.makeStationsFromYahooQuery(root);
            return table;
        }

        /**
         * if(root.getTagName().equals("LayerSet")) {
         *   try {
         *       return createWorldWindTable(root);
         *   } catch(Exception exc) {
         *       throw new IllegalArgumentException("Error creating WorldWind table:" + exc);
         *   }
         * }
         */
        String name = XmlUtil.getAttribute(root, ATTR_NAME, "");
        String type = XmlUtil.getAttribute(root, ATTR_TYPE, (String) null);
        String category = XmlUtil.getAttribute(root, ATTR_CATEGORY,
                              (String) null);
        NamedStationTable table = new NamedStationTable(root);
        table.setType(type);
        table.setName(name);
        table.setCategory(category);
        return table;
    }


    /**
     * Override the base class method to lazily instantiate the stations
     * from the XML root element if we have not done so already.
     *
     * @return    the station map
     */
    protected Map getMap() {
        if ( !haveCreatedMap) {
            haveCreatedMap = true;
            for (int i = 0; i < roots.size(); i++) {
                Element root = (Element) roots.get(i);
                String href = XmlUtil.getAttribute(root, "href",
                                  (String) null);
                if (href != null) {
                    if ((base != null) && !href.startsWith("http")) {
                        if ( !href.startsWith("/") && !base.endsWith("/")) {
                            href = base + "/" + href;
                        } else {
                            href = base + href;
                        }
                    }

                    if (isKml(href)) {
                        try {
                            this.createStationTableFromKmlFile(href);
                            return super.getMap();
                        } catch (Exception exc) {
                            return null;
                        }
                    }

                    if (isGempak(href)) {
                        try {
                            this.createStationTableFromGempak(
                                IOUtil.readContents(
                                    href, NamedStationTable.class));
                            return super.getMap();
                        } catch (Exception exc) {
                            return null;
                        }
                    }

                    try {
                        root = XmlUtil.getRoot(href, getClass());
                    } catch (Exception exc) {
                        throw new WrapperException(
                            "Loading in stations from:" + href, exc);
                    }
                }
                if (root.getTagName().equals(TAG_RSS)
                        || root.getTagName().equals(TAG_CHANNEL)) {
                    makeStationsFromRss(root);
                } else if (root.getTagName().equals(TAG_YRESULTSET)) {
                    makeStationsFromYahooQuery(root);
                } else {
                    makeStations(root);
                }
                if ( !XmlUtil.getAttribute(root, ATTR_LOADMORE, true)) {
                    break;
                }
            }
        }
        return super.getMap();
    }

    /**
     * Make stations from the root element.
     *
     *       if (id == null) {
     *               id = ""+childIdx;
     *               //stationName;
     *           }
     *           double lat = Misc.decodeLatLon(XmlUtil.getAttribute(node,
     *                            ATTR_LAT));
     *           double lon = Misc.decodeLatLon(XmlUtil.getAttribute(node,
     *                            ATTR_LON));
     *           String elev = XmlUtil.getAttribute(node, ATTR_ELEV,
     *                                              (String) null);
     *
     *           double alt = ((elev == null)
     *                         ? 0.0
     *                         : Misc.decodeLatLon(elev));
     *           try {
     *               NamedStationImpl station = new NamedStationImpl(id,
     *                                              stationName, lat, lon,
     *                                              alt, elevUnit);
     *               NamedNodeMap attrs    = node.getAttributes();
     *               int          numAttrs = attrs.getLength();
     *               for (int i = 0; i < numAttrs; i++) {
     *                   Attr   attr     = (Attr) attrs.item(i);
     *                   String attrName = attr.getNodeName();
     *                   if(attrName.equals(ATTR_ELEV)) {
     *                       station.addProperty(attrName, new Real(RealType.Altitude, alt, elevUnit));
     *                   } else {
     *                       station.addProperty(attrName, attr.getNodeValue());
     *                   }
     *               }
     *               this.add(station, true);
     *           } catch (Exception excp) {
     *               System.out.println("bad station id=" + id + "name = "
     *                                  + stationName + "lat = " + lat
     *                                  + "lon = " + lon + "alt = " + alt);
     *           }
     *       }
     *   } catch (Exception exc) {
     *       exc.printStackTrace();
     *   }
     * }
     *
     *
     *
     *
     * Make menu items from the list of stations
     *
     * @param stations      stations
     * @param listener      listener for changes
     * @return  List of menus
     */
    public static List xxxxmakeMenuItems(List stations,
                                         final ObjectListener listener) {
        List      items        = new ArrayList();
        JMenu     stationsMenu = null;
        Hashtable menus        = new Hashtable();

        for (int i = 0; i < stations.size(); i++) {
            NamedStationTable stationTable =
                (NamedStationTable) stations.get(i);
            JMenuItem mi = new JMenuItem(stationTable.getName());
            mi.addActionListener(new ObjectListener(stationTable) {
                public void actionPerformed(ActionEvent ae) {
                    listener.actionPerformed(ae, theObject);
                }
            });

            String category = stationTable.getCategory();
            if (category == null) {
                category = "";
            }

            JMenu  catMenu  = null;
            JMenu  lastMenu = null;
            String catSoFar = "";
            List   cats     = StringUtil.split(category, "/", true, true);
            for (int catIdx = 0; catIdx < cats.size(); catIdx++) {
                String subCat = (String) cats.get(catIdx);
                catSoFar = catSoFar + "/" + subCat;
                catMenu  = (JMenu) menus.get(catSoFar);
                if (catMenu == null) {
                    catMenu = new JMenu(subCat);
                    menus.put(catSoFar, catMenu);
                    if (lastMenu == null) {
                        items.add(catMenu);
                    } else {
                        lastMenu.add(catMenu);
                    }
                }
                lastMenu = catMenu;
            }
            if (catMenu != null) {
                catMenu.add(mi);
            } else {
                items.add(mi);
            }
        }
        return items;
    }


    /**
     * Make a set of menu items from the list of locations
     *
     * @param locations list of locations
     * @param listener listener on the objects
     *
     * @return a List of menu items
     */
    public static List<JMenuItem> makeMenuItems(final List locations,
            final ObjectListener listener) {
        //Don't do this for now since we don't use the mac menubar
        if (false && GuiUtils.isMac()) {
            JMenuItem menuItem = new JMenuItem("Add Location...");
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    JMenuBar menuBar = new JMenuBar();
                    List<JMenuItem> items = makeMenuItemsInner(locations,
                                                listener);
                    for (JMenuItem mi : items) {
                        menuBar.add(mi);
                    }
                    GuiUtils.showOkDialog((Window) null, "Locations",
                                          (Component) menuBar,
                                          (Component) null);
                }
            });
            List<JMenuItem> items = new ArrayList<JMenuItem>();
            items.add(menuItem);
            return items;
        } else {
            return makeMenuItemsInner(locations, listener);
        }
    }




    /**
     * Make the menu items
     *
     * @param locations  the locations
     * @param listener   the listener for each
     *
     * @return  the menus
     */
    private static List<JMenuItem> makeMenuItemsInner(final List locations,
            final ObjectListener listener) {
        List<JMenuItem>      items         = new ArrayList<JMenuItem>();

        JMenu                locationsMenu = null;
        Hashtable            menus         = new Hashtable();
        final Hashtable      categoryMap   = new Hashtable();

        final List<Object[]> macItems      = new ArrayList<Object[]>();

        for (int i = 0; i < locations.size(); i++) {
            NamedStationTable stationTable =
                (NamedStationTable) locations.get(i);
            String category = stationTable.getCategory();
            if (category == null) {
                continue;
            }
            category = "/" + category;
            List catLocations = (List) categoryMap.get(category);
            if (catLocations == null) {
                categoryMap.put(category, catLocations = new ArrayList());
            }
            catLocations.add(stationTable);
        }

        for (int i = 0; i < locations.size(); i++) {
            NamedStationTable stationTable =
                (NamedStationTable) locations.get(i);
            String category = stationTable.getCategory();
            if (category == null) {
                category = "";
            }

            JMenu  catMenu  = null;
            JMenu  lastMenu = null;
            String catSoFar = "";
            List   cats     = StringUtil.split(category, "/", true, true);
            for (int catIdx = 0; catIdx < cats.size(); catIdx++) {
                String subCat = (String) cats.get(catIdx);
                catSoFar = catSoFar + "/" + subCat;
                catMenu  = (JMenu) menus.get(catSoFar);
                if (catMenu == null) {
                    catMenu = new JMenu(subCat);
                    menus.put(catSoFar, catMenu);
                    if (lastMenu == null) {
                        items.add(catMenu);
                    } else {
                        lastMenu.add(catMenu);
                    }
                }
                lastMenu = catMenu;
            }
            if (catMenu != null) {
                boolean newOne = catMenu.getMenuListeners().length == 0;
                if (newOne) {
                    final JMenu  theMenu     = catMenu;
                    final String theCategory = catSoFar;
                    //Don't do this for now
                    if (false && GuiUtils.isMac()) {
                        macItems.add(new Object[] { categoryMap, theMenu,
                                theCategory });
                    } else {
                        catMenu.addMenuListener(new MenuListener() {
                            public void menuCanceled(MenuEvent e) {}

                            public void menuDeselected(MenuEvent e) {}

                            public void menuSelected(MenuEvent e) {
                                addMenuItems(categoryMap, theMenu,
                                             theCategory, listener);
                            }
                        });
                    }
                }
            } else {
                JMenuItem mi = new JMenuItem(stationTable.getName());
                mi.addActionListener(new ObjectListener(stationTable) {
                    public void actionPerformed(ActionEvent ae) {
                        listener.actionPerformed(ae, theObject);
                    }
                });
                items.add(mi);
            }
        }

        if (macItems.size() > 0) {
            Misc.run(new Runnable() {
                public void run() {
                    for (Object[] tuple : macItems) {
                        Hashtable categoryMap = (Hashtable) tuple[0];
                        JMenu     theMenu     = (JMenu) tuple[1];
                        String    theCategory = (String) tuple[2];
                        addMenuItems(categoryMap, theMenu, theCategory,
                                     listener);
                    }
                }
            });
        }
        return items;
    }


    /**
     * Add menu items
     *
     * @param categoryMap hashtable of category maps
     * @param menu the menu
     * @param menuCategory the menu category
     * @param listener the listener for actions
     */
    private static void addMenuItems(Hashtable categoryMap, JMenu menu,
                                     String menuCategory,
                                     final ObjectListener listener) {

        //      System.err.println("add:" + menuCategory);
        List locations = (List) categoryMap.get(menuCategory);
        if (locations == null) {
            return;
        }
        categoryMap.remove(menuCategory);
        for (int i = 0; i < locations.size(); i++) {
            NamedStationTable location = (NamedStationTable) locations.get(i);
            JMenuItem         mi       = new JMenuItem(location.getName());
            mi.addActionListener(new ObjectListener(location) {
                public void actionPerformed(ActionEvent ae) {
                    listener.actionPerformed(ae, theObject);
                }
            });

            menu.add(mi);
        }
    }


    /**
     * Is the table empty?
     *
     * @return true if no stations are in the table
     */
    public boolean isEmpty() {
        return byId_.isEmpty();
    }

    /**
     * Return table name and size
     *
     * @return string representing name and size
     */
    public String toString() {
        return getCategory() + ">" + getName() + "(" + size() + ")";
    }


    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Test a particular file
     *
     * @param args  test the file
     *
     * @throws Exception  problem reading the file
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Need to supply a file name");
        }
        NamedStationTable tbl =
            NamedStationTable.createStationTableFromFile(args[0]);
    }
}

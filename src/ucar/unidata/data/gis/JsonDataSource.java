/*
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.unidata.data.*;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.util.*;
import ucar.unidata.view.geoloc.CoordinateFormat;
import ucar.unidata.view.station.StationLocationMap;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import visad.Data;
import visad.DateTime;
import visad.FieldImpl;
import visad.VisADException;

import java.awt.*;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;

import java.net.URL;

import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.*;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;





public class JsonDataSource extends FilesDataSource {

    /** For ximg files */
    public static final PatternFileFilter FILTER_KML =
            new PatternFileFilter("(.+\\.json)",
                    "Geo JSON File (*.json)", ".json");
    /** For ximg files */
    public static final String EXT_KML = ".json";
    /** For ximg files */
    public static final String SUFFIX_KML = ".json";
    /** xml attribute */
    public static final String ATTR_PARENT = "parent";
    /** xml attribute */
    public static final String ATTR_NAME = "name";
    /** kml tag id */
    public static final String TAG_GROUNDOVERLAY = "GroundOverlay";
    /** kml tag  */
    public static final String TAG_PHOTOOVERLAY = "PhotoOverlay";
    /** xml tag */
    public static final String TAG_URL = "Url";
    /** xml tag */
    public static final String TAG_HREF = "href";
    /** xml tag */
    public static final String TAG_LINK = "Link";
    /** xml tag */
    public static final String TAG_LINESTRING = "LineString";
    /** xml tag */
    public static final String TAG_SCHEMA = "Schema";
    /** kml tag id */
    public static final String TAG_NETWORKLINK = "NetworkLink";
    /** kml tag id */
    public static final String TAG_MULTIGEOMETRY = "MultiGeometry";
    /** kml tag id */
    public static final String TAG_DOCUMENT = "Document";
    /** kml tag id */
    public static final String TAG_FOLDER = "Folder";
    /** xml tag */
    public static final String TAG_KML = "json";
    /** kml tag id */
    public static final String TAG_PLACEMARK = "Placemark";
    /** kml tag id */
    public static final String TAG_NAME = "name";
    /** kml tag id */
    public static final String TAG_STYLEURL = "styleUrl";
    /** property */
    private static final String PROP_HREF = "prop.href";
    /** property */
    private static final String PROP_BASEURL = "prop.baseurl";
    /** property */
    private static final String PROP_DISPLAYCATEGORIES =
            "prop.displaycategories";
    /** data choice id */
    private static final String ID_NETWORKLINK = "JsonDataSource.networklink";
    /** dummy id */
    private static final String ID_DUMMY = "JsonDataSource.dummy";
    /** A local cache */
    protected List cachedData = new ArrayList();
    /** The urls */
    protected List cachedUrls = new ArrayList();
    /** The jsonInfo */
    protected HashMap jsonInfo = new HashMap();
    /** The colorMap */
    protected List colorMap;
    /** The timeList */
    protected List timeList = new ArrayList<DateTime>();
    /** mapping of id to kml node */
    private Hashtable idToNode = new Hashtable();
    /** The url or file name */
    private String legacyKmlUrl;
    /** For stopping loads */
    private Object loadId;
    /** Root of the kml xml */
    private Element root;
    /** Maps file paths in the kmz file to the kmz file */
    private Hashtable pathToFile = new Hashtable();
    /** Name to use from the kml */
    private String topName = null;
    /** holds schemas */
    private Hashtable schemas;
    /** The descriptor */
    private DataSourceDescriptor descriptor = null;


    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public JsonDataSource() { }

    /**
     * Create a KmlDataSource from the specification given.
     *
     * @param descriptor          descriptor for the data source
     * @param newSources Where the json came from
     * @param properties          extra properties
     *
     * @throws VisADException     some problem occurred creating data
     */
    public JsonDataSource(DataSourceDescriptor descriptor, List newSources,
                          Hashtable properties) {

        super(descriptor, newSources, "JSON data source", properties);
        String name = "ddd";
        int idx = name.indexOf("?");
        if(idx>=0) {
            name= name.substring(0,idx);
            setName(name);
        }

        if (getName().length() == 0) {
            if (newSources.size() > 0) {
                String s = newSources.get(0).toString();
                idx = s.indexOf("?");
                if(idx>=0) s= s.substring(0,idx);
                setName(s);
            } else {
                //setName(description);
            }
        }
        this.sources = newSources;
        this.descriptor = descriptor;
        initPolygonColorMap();
        initGeoJsonDataSource();
    }
    /**
     * Create a KmlDataSource from the specification given.
     *
     * @param descriptor          descriptor for the data source
     * @param kmlUrl Where the kml came from
     * @param properties          extra properties
     *
     * @throws VisADException     some problem occurred creating data
     */
    public JsonDataSource(DataSourceDescriptor descriptor, String kmlUrl,
                         Hashtable properties)
            throws VisADException, Exception {

        super(descriptor, Misc.newList(kmlUrl), "JSON data source", properties);
        this.sources = Misc.newList(kmlUrl);
        this.descriptor = descriptor;

        setName("PROBSEVERE: " + IOUtil.stripExtension(IOUtil.getFileTail(kmlUrl)));

        initPolygonColorMap();

        initGeoJsonDataSource();
    }

    /**
     * write image as a kml to file
     *
     * @param kmlFilename kml filename
     * @param bounds _image bounds
     * @param imageFileName image filename
     *
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */
    public static void writeToFile(String kmlFilename,
                                   GeoLocationInfo bounds,
                                   String imageFileName)
            throws FileNotFoundException, IOException {
        if (kmlFilename.endsWith(".kmz")) {
            String tail =
                    IOUtil.stripExtension(IOUtil.getFileTail(kmlFilename));
            ZipOutputStream zos =
                    new ZipOutputStream(new FileOutputStream(kmlFilename));

            zos.putNextEntry(new ZipEntry(tail + ".kml"));
            String kml = createKml(bounds, IOUtil.getFileTail(imageFileName));
            byte[] kmlBytes = kml.getBytes();
            zos.write(kmlBytes, 0, kmlBytes.length);

            byte[] imageBytes =
                    IOUtil.readBytes(new FileInputStream(imageFileName));
            zos.putNextEntry(new ZipEntry(IOUtil.getFileTail(imageFileName)));
            zos.write(imageBytes, 0, imageBytes.length);
            zos.close();
        } else {
            String kml = createKml(bounds, imageFileName);
            IOUtil.writeFile(kmlFilename, kml);
        }
    }

    /**
     * Create some kml from the given bounds and image file
     *
     * @param bounds bounds
     * @param imageFileName image
     *
     * @return kml
     *
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */
    public static String createKml(GeoLocationInfo bounds,
                                   String imageFileName)
            throws FileNotFoundException, IOException {
        StringBuffer sb = new StringBuffer();
        String name =
                IOUtil.getFileTail(IOUtil.stripExtension(imageFileName));
        String imageName =
                IOUtil.getFileTail(IOUtil.stripExtension(imageFileName));
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">\n");
        sb.append("<GroundOverlay>\n");
        sb.append("<name>" + name + "</name>\n");
        sb.append("<Icon>\n");
        sb.append("<href>" + imageFileName + "</href>\n");
        sb.append("</Icon>");
        createLatLonBox(bounds, sb);


        sb.append("</GroundOverlay>\n</kml>\n");
        return sb.toString();
    }

    /**
     * Utility to create a latlonbox kml
     *
     * @param bounds bounds
     * @param sb buffer to add to
     */
    public static void createLatLonBox(GeoLocationInfo bounds,
                                       StringBuffer sb) {
        sb.append("<LatLonBox>\n");
        sb.append("<north>" + bounds.getMaxLat() + "</north>\n");
        sb.append("<south>" + bounds.getMinLat() + "</south>\n");
        sb.append("<east>" + CoordinateFormat.formatLongitude(bounds.getMaxLon(), "DD.dddddd", false) + "</east>\n");
        sb.append("<west>" + CoordinateFormat.formatLongitude(bounds.getMinLon(), "DD.dddddd", false) + "</west>\n");
        sb.append("</LatLonBox>\n");

    }

    /**
     * test main
     *
     *
     * @param args cmd line args
     */
    public static void main(String[] args) {

        Hashtable sts = new Hashtable();

        System.exit(0);
    }

    /**
     * load probsevere color map
     */
    protected void initPolygonColorMap() {
        if (this.colorMap == null) {
            List colors     = new ArrayList();
            List resources =
                    Misc.newList("/ucar/unidata/idv/resources/probsevere_cmap.xml");
            XmlResourceCollection colorMapResources = new XmlResourceCollection("", resources);
            Element root = colorMapResources.getRoot(0);
            colors = processXml(root);
            colorMap = colors;
        }
    }

    /**
     *  Get color map from xml file
     *
     * @param
     */
    public List processXml(Element root) {
        List colors     = new ArrayList();
        List colorNodes = XmlUtil.findChildren(root, "color");
        for (int i = 0; i < colorNodes.size(); i++) {
            Element serverNode = (Element) colorNodes.get(i);
            String  aval       = XmlUtil.getAttribute(serverNode, "a");
            String  bval       = XmlUtil.getAttribute(serverNode, "b");
            String  gval       = XmlUtil.getAttribute(serverNode, "g");
            String  rval       = XmlUtil.getAttribute(serverNode, "r");

            float [] values = new float[]{ Float.parseFloat(aval), Float.parseFloat(bval),
                    Float.parseFloat(gval),Float.parseFloat(rval),};

            colors.add(values);
        }
        return colors;
    }

    /**
     *  Get color map from xml file
     *
     * @param
     */
    protected Document readColorMapXml(String href) throws Exception {
        String xml = IOUtil.readContents(href, getClass());
        if (xml == null) {
            return null;
        }
        return XmlUtil.getDocument(xml);
    }

    /**
     * reload
     */
    public void reloadData() {
        root = null;
        super.reloadData();
        dataChoices = null;
        initGeoJsonDataSource();
        getDataChoices();
    }

    /**
     * Initialize after we have been unpersisted
     */
    public void initAfterUnpersistence() {
        //For legacy bundles
        if (legacyKmlUrl != null) {
            sources = Misc.newList(legacyKmlUrl);
        }
        super.initAfterUnpersistence();
        //initKmlDataSource();
        initPolygonColorMap();
        initGeoJsonDataSource();
    }

    /**
     * Read the image
     *
     * @param url image url
     * @param baseUrl Where the kml came from_
     *
     * @return The image
     */
    protected Image readImage(String url, String baseUrl) {
        Image image = null;
        synchronized (cachedUrls) {
            for (int i = 0; i < cachedUrls.size(); i++) {
                if (url.equals(cachedUrls.get(i))) {
                    image = (Image) cachedData.get(i);
                    break;
                }
            }
        }
        if (image != null) {
            return image;
        }
        //        Trace.startTrace();
        byte[] imageContent = null;
        try {
            imageContent = readBytes(url);
            if (imageContent == null) {
                if ( !url.startsWith("http")) {
                    url          = IOUtil.getFileRoot(baseUrl) + "/" + url;
                    imageContent = readBytes(url);
                }
            }
            if (imageContent == null) {
                return null;
            }
            Trace.call1("createImage");
            image = Toolkit.getDefaultToolkit().createImage(imageContent);
            Trace.call2("createImage");
            synchronized (cachedUrls) {
                if (cachedUrls.size() > 5) {
                    cachedUrls.remove(cachedUrls.size() - 1);
                    cachedData.remove(cachedData.size() - 1);
                }
                //For now don't cache
                //      cachedUrls.add(0, url);
                //                    cachedData.add(0, image);
            }
            return image;
        } catch (Exception iexc) {
            LogUtil.logException(
                    "There was an error accessing the image with the url:\n"
                            + url, iexc);
            return null;
        }
    }

    /**
     * Utility to read the bytes from the file or url
     *
     * @param path file or url
     *
     * @return bytes
     *
     * @throws Exception On badness
     */
    protected byte[] readBytes(String path) throws Exception {
        InputStream is = getInputStream(path);
        if (is == null) {
            return null;
        }
        byte[] bytes = IOUtil.readBytes(is, loadId);
        try {
            is.close();
        } catch (Exception exc) {}
        return bytes;
    }

    /**
     * Create the input stream. Handle the case when it is a zip file
     *
     * @param path file or url
     *
     * @return input stream to kml file
     *
     * @throws Exception On badness
     */
    protected InputStream getInputStream(String path) throws Exception {
        if (new File(path).exists()) {
            return new FileInputStream(path);
        }

        try {
            URL           url        = new URL(path);
            URLConnection connection = url.openConnection();
            return connection.getInputStream();
        } catch (Exception mue) {}
        String kmlUrl = (String) pathToFile.get(path);
        if (kmlUrl == null) {
            kmlUrl = getFilePath();
        }

        if (kmlUrl == null) {
            return null;
        }

        if (IOUtil.isRelativePath(path)) {
            path = IOUtil.getFileRoot(kmlUrl) + "/" + path;
        }
        return IOUtil.getInputStream(path);

    }

    /**
     * Initialization method
     */
    private void initGeoJsonDataSource() {
        Trace.call1("JsonDataSource.initJson");
        initGeoJsonDataSourceInner();
        Trace.call2("JsonDataSource.initJson");
    }

    /**
     * init
     */
    private void initGeoJsonDataSourceInner() {

        if (root != null) {
            return;
        }
        if (getFilePath() == null) {
            return;
        }
        try {
            int size = this.sources.size();
            for(int i = 0; i < size; i++) {
                String fPath = (String)this.sources.get(i);
                parseJSON(fPath);
            }
            this.timeList = Misc.sort(this.timeList);
        } catch (Exception iexc) {
            LogUtil.logException(
                "There was an error parsing the xml file:\n "
                + getFilePath(), iexc);
            setInError(true, false, "");
        }
    }

    /**
     * Get the kml root element
     *
     * @param jsonUrl json url
     *
     * @return Root element
     *
     * @throws Exception On badness
     */
    private void parseJSON(String jsonUrl) throws Exception {

        //JSON parser object pour lire le fichier

        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(jsonUrl)) {
            // lecture du fichier
            Object obj         = jsonParser.parse(reader);
            JSONObject feature = (JSONObject) obj;
            JSONArray features = (JSONArray) feature.get("features");
            String dateTimePattern = "yyyyMMdd_hhnnss UTC";
            DateTime dts = null;
            String descrip = (String)feature.get("validTime");
            if(descrip != null){
                //System.out.println(descrip);
                DatePattern  dp  = new DatePattern(dateTimePattern);
                if (dp.match(descrip)) {
                    dts = dp.getDateTime();
                }
                if(dts != null) {
                    this.timeList.add(dts);
                    jsonInfo.put("timeObj", dts);
                }
            }

            // Construction initiale du KML avec un factory
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder        = factory.newDocumentBuilder();
            final Document document             = builder.newDocument();

            // Creation du tag <kml> avec la version voulue
            final Element kmlTag = document.createElement("kml");
            kmlTag.setAttribute("xmlns", "http://www.opengis.net/kml/2.2");
            document.appendChild(kmlTag);

            // Creation du tag <Document>
            final Element documentTag = document.createElement("Document");
            kmlTag.appendChild(documentTag);

            // Creation de la balise <Style> qui permet d'appliquer differents styles suivant les IDs
            final Element styleTag = document.createElement("Style");
            styleTag.setAttribute("id", "style1");
            documentTag.appendChild(styleTag);

            final Element polyStyleTag = document.createElement("PolyStyle");
            styleTag.appendChild(polyStyleTag);

            final Element fillTag = document.createElement("fill");
            fillTag.appendChild(document.createTextNode("0"));
            polyStyleTag.appendChild(fillTag);

            final Element outlineTag = document.createElement("outline");
            outlineTag.appendChild(document.createTextNode("1"));
            polyStyleTag.appendChild(outlineTag);

            HashMap ploygoncoords = new HashMap();
            // Parcours de tous les features
            for (Object feat : features) {
                // Creation de la balise <PlaceMark>
                final Element placeMark = document.createElement("Placemark");
                documentTag.appendChild(placeMark);

                // Creation de la balise <styleUrl> permettant d'appliquer un style sur un placemark
                final Element styleURLTag = document.createElement("styleUrl");
                styleURLTag.appendChild(document.createTextNode("#style1"));
                placeMark.appendChild(styleURLTag);

                // Conversion des features de Object vers JSONObject
                JSONObject featJ    = (JSONObject) feat;
                // Recuperation des prorietes de l'objet feature
                JSONObject featJSON = (JSONObject) featJ.get("properties");

                // Recuperation des valeurs de la propriete.
               // String admin = (featJSON.get("density")).toString();
                String name = (String) featJSON.get("ID");

                // Creation de propriete
                //Properties properties = new Properties(admin , name);

                // Entete de la ligne pour la console
                //String titre          = properties.toString();
                //System.out.println(titre);

                JSONObject coordJSON = (JSONObject) featJ.get("geometry");
                String type          = (String) coordJSON.get("type");
                JSONArray coords     = (JSONArray) coordJSON.get("coordinates");
                JSONObject modelsJSON = (JSONObject) featJ.get("models");
                JSONObject probsevereJSON = (JSONObject) modelsJSON.get("probsevere");
                String prob          = (String)probsevereJSON.get("PROB");
                if (type.equals("Polygon") || type.equals("MultiPolygon")){
                    ploygoncoords.put(name, getCoordinates(coords, prob));
                }
                else {
                    throw new Error("Type mal forme !");
                }
            }
            if(dts != null)
                jsonInfo.put(dts, ploygoncoords);
            else
                jsonInfo.put("polygon", ploygoncoords);
            //this.timeList = Misc.sort(this.timeList);
        } catch (IOException | ParseException | ParserConfigurationException e) {
            e.printStackTrace();
        }

    }

    /**
     *  build the polygon coordinate string
     *
     * @param
     */
    private  String getCoordinates( JSONArray coords, String prob) {
        String outStr = coords.toJSONString();
        float probability = Float.parseFloat(prob);
        int idx = (int)(256*(probability/100.0)) - 1;
        String colorStr = "255,0,0";
        if(this.colorMap != null && idx > 0){
            float [] values = (float [])this.colorMap.get(idx);
            int blue = (int)(values[1] * 255);
            int green = (int)(values[2] * 255);
            int red = (int)(values[3] * 255);
            colorStr = red  +  "," + green + "," + blue;
        }
        outStr = outStr.replaceAll("\\[", "");
        outStr = outStr.replaceAll("\\]", "");
        outStr = "<polygon" + " smooth=" + "\"false\"" + " filled=" + "\"false\"" + " coordtype=" + "\"LONLAT\"" + " points=" + "\"" + outStr + "\"" +
                " color=" + "\"" + colorStr   + "\"" +
                 "/>";
        return outStr;
    }

    /**
     *  Add the xml header if needed
     *
     * @param xml xml
     *
     * @return cleaned up xml
     */
    private String cleanupXml(String xml) {
        //        System.err.println("xml:" + xml.substring(0, 100));
        xml = xml.trim();
        if ( !xml.startsWith("<?xml")) {
            return XmlUtil.XML_HEADER + "\n" + xml;
        }

        return xml;
    }

    /**
     * Add data choice to parent or, if null, to this
     *
     * @param newDataChoice data choice to add
     * @param parentDataChoice parent
     */
    private void addDataChoice(DataChoice newDataChoice,
                               CompositeDataChoice parentDataChoice) {
        if (parentDataChoice != null) {
            parentDataChoice.addDataChoice(newDataChoice);
        } else {
            addDataChoice(newDataChoice);
        }
    }

    /**
     *  Get all the times for the given DataChoice
     *
     * @param
     */
    public List getAllDateTimes(DataChoice dataChoice) {
        if (dataChoice instanceof CompositeDataChoice) {
            return timeList;
        }

        Object dttmObject = getDateTime(dataChoice);
        if (dttmObject != null) {
            return Misc.newList(dttmObject);
        }
        return new ArrayList();

    }

    /**
     *  Try to merge children up into parents of only one child
     *
     * @param
     */
    public List getAllDateTimes() {
        return Misc.sort(timeList);
    }

    /**
     *  get datetime of only one datachoice
     *
     * @param dataChoice
     */
    private Object getDateTime(DataChoice dataChoice) {
        DataSelection   dataSelection  = dataChoice.getDataSelection();
        List timeList = dataSelection.getTimes();
        return timeList.get(0);
    }

    /**
     *  Try to merge children up into parents of only one child
     *
     * @param parent parent
     */
    private void mergeChildren(CompositeDataChoice parent) {
        List children = parent.getDataChoices();
        if (parent.getId().equals(ID_NETWORKLINK) && (children.size() == 1)
                && ((DataChoice) children.get(0)).getId().equals(ID_DUMMY)) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            DataChoice dc = (DataChoice) children.get(i);
            if (dc instanceof CompositeDataChoice) {
                mergeChildren((CompositeDataChoice) dc);
            }
        }

        children = parent.getDataChoices();
        CompositeDataChoice grandParent =
                (CompositeDataChoice) parent.getParent();
        if (children.size() == 0) {
            if (grandParent != null) {
                grandParent.removeDataChoice(parent);
            } else {
                removeDataChoice(parent);
            }
            return;
        }

        if (children.size() != 1) {
            return;
        }
        DataChoice child = (DataChoice) children.get(0);
        if (child.getName().equals("Placemarks")
                || child.getName().equals("Image")) {
            child.setName(parent.getName());
            child.setDescription(parent.getDescription());
        }

        if (grandParent != null) {
            grandParent.replaceDataChoice(parent, child);
        } else {
            replaceDataChoice(parent, child);
        }
    }

    /**
     * Create the data choices associated with this source.
     */
    protected void doMakeDataChoices() {
        idToNode = new Hashtable();

        schemas = new Hashtable();
        //HashMap polygonHMap = (HashMap)jsonInfo.get(timeList.get(0));
        //DateTime dt = (DateTime)timeList.get(0);
        if(timeList.size()  >= 1) {

            //for(int i = 0; i  < timeList.size(); i++ ) {
             //   DateTime dt = (DateTime)timeList.get(i);
             //   HashMap plyCoords = (HashMap)jsonInfo.get(dt);
                processNode(null, null,  new ArrayList(),null);
          //  }
        }

        //processNode(null, polygonHMap,  new ArrayList(),dt);
        Trace.call1("JsonDataSource.processing kml");


        List dataChoices =   getDataChoices();
        // CompositeDataChoice dc0 = (CompositeDataChoice) dataChoices.get(0);
        // List dataChoices0 = dc0.getDataChoices();

        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dc = (DataChoice) dataChoices.get(i);
            DataSelection dataSelection = dc.getDataSelection();
            if(dataSelection == null)
                dataSelection = new DataSelection();
            //dataSelection.setTimes(timeList);
            //dc.setDataSelection();
            if (dc instanceof CompositeDataChoice) {

               // mergeChildren((CompositeDataChoice) dc);
            }
        }
    }

    /**
     * process the kml root
     *
     * @param root Root
     * @param parent Parent to add to
     * @param filepath Where did the kml come from
     * @param categories list of categories we use as we recurse down the tree.
     * This is really used as a unique identifier of the kml nodes
     */
    private void processRootNode1(Element root, CompositeDataChoice parent,
                                 String filepath, List categories) {
        NodeList children = XmlUtil.getElements(root);
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if (topName == null) {
                topName = XmlUtil.getChildText(XmlUtil.findChild(child,
                        TAG_NAME));
                if (topName != null) {
                    setName("KML: " + topName);
                }
            }
         //   processNode(parent, child, categories, filepath);
        }
    }

    /**
     * Expand the data choice if its a networklink
     *
     * @param parent data choice
     */
    public void expandIfNeeded(CompositeDataChoice parent) {
        super.expandIfNeeded(parent);
        List childrenChoices = parent.getDataChoices();
        if ( !(parent.getId().equals(ID_NETWORKLINK)
                && (childrenChoices.size() == 1)
                && ((DataChoice) childrenChoices.get(0)).getId().equals(
                ID_DUMMY))) {
            return;
        }
        parent.removeAllDataChoices();
        String href = (String) parent.getProperty(PROP_HREF);
        String baseUrl = (String) parent.getProperty(PROP_BASEURL);
        List currentDisplayCategories =
                (List) parent.getProperty(PROP_DISPLAYCATEGORIES);
        if ( !href.startsWith("http")) {
            href = IOUtil.getFileRoot(baseUrl) + "/" + href;
        }
        //        System.err.println("Expanding:" + href);
        try {
            incrOutstandingGetDataCalls();
            LogUtil.message("Fetching Network Link:" + href);
            Element networkRoot = null; //parseKml(href);
            if (networkRoot == null) {
                throw new IllegalStateException(
                        "Error fetching network link file:" + href);
            }
            currentDisplayCategories.add(parent.getName());
            processRootNode1(networkRoot, parent, href,
                    currentDisplayCategories);

            /*

            NodeList children = XmlUtil.getElements(networkRoot);
            for (int i = 0; i < children.getLength(); i++) {
                Element child = (Element) children.item(i);
                processNode(parent, child, currentDisplayCategories, href);
            }

            */
            List newChildren = parent.getDataChoices();
            for (int i = 0; i < newChildren.size(); i++) {
                DataChoice dc = (DataChoice) newChildren.get(i);
                if (dc instanceof CompositeDataChoice) {
                    mergeChildren((CompositeDataChoice) dc);
                }
            }
            LogUtil.message("");
        } catch (Exception exc) {
            parent.setName("Error: " + parent.getName());
            LogUtil.logException(
                    "There was an error parsing the network link file:\n "
                            + href, exc);
        }
        decrOutstandingGetDataCalls();
    }

    /**
     * Walk the kml tree
     *
     *
     * @param parentDataChoice data choice
     * @param polygonHMap1 node
     * @param displayCategories display categories for uniqueness
     * @param dts1 where the kml came from
     */
    private void processNode(CompositeDataChoice parentDataChoice,
                             HashMap polygonHMap1, List displayCategories,
                             DateTime dts1) {

        String tagName = "polygon";

        //        System.err.println ("tag:" + tagName);
        //DateTime dts = (DateTime)jsonInfo.get("timeObj");
        String tmp = (String) schemas.get(tagName);
        if (tmp != null) {
            tagName = tmp;
        }

        //        System.err.println("tag:" + tagName);
        List currentDisplayCategories = new ArrayList(displayCategories);


        CompositeDataChoice newParentDataChoice =
            new CompositeDataChoice(
                this, "All Warning", "Probsevere", "Probsevere" ,
                Misc.newList(DataCategory.XGRF_CATEGORY));
        newParentDataChoice.setProperty("JsonWARNING", "folderName");
        newParentDataChoice.setUseDataSourceToFindTimes(true);
        //newParentDataChoice.getProperties().put("timeObj", dts);
        addDataChoice(newParentDataChoice, parentDataChoice);
        List categories =
                Misc.newList(DataCategory.XGRF_CATEGORY);
        //Hashtable properties = new Hashtable();
        for(int i = 0; i  < timeList.size(); i++ ) {
            DateTime dt = (DateTime) timeList.get(i);
            HashMap plyCoords = (HashMap) jsonInfo.get(dt);

           // if (dt != null)
           //     properties.put("timeObj", dt);
            Set keys = plyCoords.keySet();
            for (Object k : keys) {
                String name = (String) k;
                Hashtable newProperties = new Hashtable();
                String coordStr = (String) plyCoords.get(k);
                newProperties.put("coordStr", coordStr);
                newProperties.put("timeObj", dt);
                //System.out.println(" Name " + name);
                //System.out.println(" name " + name + " " + coordStr);
                String displayCategory =
                        StringUtil.join("-", currentDisplayCategories);
                if (name != null) {
                    if (displayCategory.length() > 0) {
                        displayCategory = displayCategory + "-"
                                + name;
                    } else {
                        displayCategory = name;
                    }
                } else {
                    name = "Shapes";
                }
                //System.out.println(" Name " + name);
                KmlId id = new KmlId(KmlId.NODE_SHAPES, name,
                        displayCategory, dt.dateString());
                //newParentDataChoice.addDataChoice(new DirectDataChoice(this,
                //       ID_DUMMY, name, name + "coordinates", categories));
                DataChoice dc = new DirectDataChoice(
                        this, id, name, name, categories,
                        newProperties);
                DataSelection dataSelection = dc.getDataSelection();
                if(dataSelection == null)
                    dataSelection = new DataSelection();
                dataSelection.setTimes(Misc.newList(dt));
                dc.setDataSelection(dataSelection);
                newParentDataChoice.addDataChoice(dc);
            }
        }

    }

    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        loadId = JobManager.getManager().stopAndRestart(loadId, "KMLControl");
        if (requestProperties == null) {
            requestProperties = new Hashtable();
        }
        Object  tmp     = dataChoice.getId();

        KmlInfo kmlInfo = null;
        //For legacy bundles
        if (tmp instanceof KmlInfo) {
            kmlInfo = (KmlInfo) tmp;
        } else if(dataChoice instanceof  CompositeDataChoice && tmp instanceof String
                && (((String) tmp).contains("All Warning"))) {
            //HashMap polygonInfo = (HashMap)jsonInfo.get(timeList.get(0));
            CompositeDataChoice cdc = (CompositeDataChoice)dataChoice;
            List ddcs = cdc.getDataChoices();
            List pdata = new ArrayList();
            for(int i = 0; i < ddcs.size(); i++) {
                DataChoice dc = (DataChoice) ddcs.get(i);
                //String coordsStr = (String)polygonInfo.get(dc.getName());
                pdata.add(getDataInner(dc, category, dataSelection, requestProperties));
            }

            StringBuffer sb = new StringBuffer("<shapes>\n");
            for(int j = 0; j < pdata.size();j++) {
                visad.Text vt = (visad.Text)pdata.get(j);
                if(vt != null) {
                    String ttt = vt.toString();
                    ttt = ttt.replace("<shapes>\n", "");
                    ttt = ttt.replace("</shapes>", "");
                    sb = sb.append(ttt);
                }
            }
            sb.append("</shapes>");
            return new visad.Text(sb.toString());
        } else {
            DateTime dt;
            KmlId  id   = (KmlId) tmp;
            Hashtable dcproperties  = dataChoice.getProperties();
            dt = (DateTime) dcproperties.get("timeObj");
            List subTimeList = getJsonTimes(dataSelection);
            //System.out.println(" ID " + id);
            HashMap polygonInfo = (HashMap)jsonInfo.get(dt);
            DateTime polygonTime = dt;
            if(subTimeList.contains(dt)) {
                String timeStr = null;
                if (polygonTime != null) {
                    timeStr = polygonTime.toString();
                    polygonInfo.put("time", timeStr);
                }
                String coordsStr = (String) dcproperties.get("coordStr");
                StringBuffer sb = new StringBuffer("<shapes>\n");
                sb = sb.append(coordsStr);
                int idx = sb.indexOf("polygon");
                sb.insert(idx + 8, "times=\"" + timeStr + "\" ");
                sb.append("</shapes>");
                //System.out.println(" ss " + sb);
                return new visad.Text(sb.toString());
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * make a list of DateTime-s from a geoJson timeList
     *
     * @param dataSelection  - geoJson time
     * @return corresponding List of DateTime-s.
     */
    private List getJsonTimes(DataSelection dataSelection) {
        List timeSL = dataSelection.getTimes();
        List subTimeList = new ArrayList();

        if (timeSL != null && timeList != null) {
            for (int i = 0; i < timeSL.size(); i++) {
                int ii = (int) timeSL.get(i);
                subTimeList.add(timeList.get(ii));
            }
            return subTimeList;
        }  else {
            return timeList;
        }
    }

    /**
     * See if this DataSource should cache or not
     *
     * @param data   Data to cache
     * @return  false
     */
    protected boolean shouldCache(Data data) {
        return false;
    }

    /**
     * Create a list of times for this data source.  Since shapefiles
     * don't have any times, return an empty List.
     *
     * @return  an empty List
     */
    protected List doMakeDateTimes() {
        return timeList;
    }


}
/*
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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


import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.unidata.data.*;
import ucar.unidata.data.radar.TDSRadarDatasetCollection;
import ucar.unidata.util.*;
import ucar.unidata.view.geoloc.CoordinateFormat;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import visad.Data;
import visad.DateTime;
import visad.VisADException;

import java.awt.*;
import java.io.*;

import java.net.URL;

import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.*;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;



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

    private static String DEFAULT_PATH =
            "https://mrms.ncep.noaa.gov/data/ProbSevere/PROBSEVERE/";
    protected List times;

    /** _more_          */
    public boolean useDriverTime = false;
    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public JsonDataSource() { }

    /**
     * Create a KmlDataSource from the specification given.
     *
     * @param descriptor descriptor for the data source
     * @param newSources Where the json came from
     * @param properties extra properties
     *
     * @throws VisADException some problem occurred creating data
     */
    public JsonDataSource(DataSourceDescriptor descriptor, List newSources,
                          Hashtable properties) throws VisADException, Exception {

        super(descriptor, newSources, "JSON data source", properties);
        this.sources = Misc.sort(newSources);
        try {
            int size = this.sources.size();
            for(int i = 0; i < size; i++) {
                String fPath = (String)this.sources.get(i);
                parseJSONHead(fPath);
            }
            this.timeList = Misc.sort(this.timeList);
            this.times = this.timeList;
        } catch (Exception iexc) {
            LogUtil.logException(
                    "There was an error parsing the xml file:\n "
                            + getFilePath(), iexc);
            setInError(true, false, "");
        }
        this.descriptor = descriptor;

        setName("JSON: " + IOUtil.stripExtension(IOUtil.getFileTail((String)sources.get(0))));
        initPolygonColorMap();
        //initGeoJsonDataSource();
        getIdv().getIdvUIManager().showDataSelector();
    }
    /**
     * Create a KmlDataSource from the specification given.
     *
     * @param descriptor descriptor for the data source
     * @param probSevereUrl     Where the kml came from
     * @param properties extra properties
     *
     * @throws VisADException some problem occurred creating data
     */
    public JsonDataSource(DataSourceDescriptor descriptor, String probSevereUrl,
                          Hashtable properties)
            throws VisADException, Exception {

        super(descriptor, Misc.newList(probSevereUrl), "JSON data source", properties);
        if(probSevereUrl.length() == 0){
            init(DEFAULT_PATH);
            this.timeList = Misc.sort(this.times);
        } else {
            this.sources = Misc.newList(probSevereUrl);
            try {
                int size = this.sources.size();
                for(int i = 0; i < size; i++) {
                    String fPath = (String)this.sources.get(i);
                    parseJSONHead(fPath);
                }
                this.timeList = Misc.sort(this.timeList);
                this.times = this.timeList;
            } catch (Exception iexc) {
                LogUtil.logException(
                        "There was an error parsing the xml file:\n "
                                + getFilePath(), iexc);
                setInError(true, false, "");
            }
        }

        this.descriptor = descriptor;
        if(probSevereUrl == null || probSevereUrl.length() == 0)
            setName("JSON: NCEP ProbSevere (server)" );
        else
            setName("JSON: " + IOUtil.stripExtension(IOUtil.getFileTail(probSevereUrl)));
        initPolygonColorMap();
       //initGeoJsonDataSource();
        getIdv().getIdvUIManager().showDataSelector();
    }

    /**
     * init KmlDataSource from the specification given.
     *
     * @param probSevereUrl     Where the json came from
     *
     * @throws VisADException some problem occurred creating data
     */
    public void init(String probSevereUrl)
            throws VisADException, Exception {

        ArrayList defSources = new ArrayList();
        ArrayList sourceTimes = new ArrayList();
        String dateTimePattern = "yyyymmdd_hhnnss";
        DatePattern dp       = new DatePattern(dateTimePattern);
        org.jsoup.nodes.Document doc = Jsoup.connect(probSevereUrl).get();
        org.jsoup.select.Elements rows = doc.select("tr");

        for(org.jsoup.nodes.Element row :rows)
        {
            org.jsoup.select.Elements columns = row.select("td");
            for (org.jsoup.nodes.Element column:columns)
            {
                if(column.text().endsWith("json") ) {
                    //System.out.print( column.text());
                    int nn = column.text().length();
                    String timeStr = column.text().substring(nn-20, nn-5);
                    String ss = DEFAULT_PATH + column.text();
                    if (dp.match(timeStr)) {
                        defSources.add(ss);
                        sourceTimes.add(dp.getDateTime());
                    }

                }
            }
            //System.out.println();
        }
        this.sources = defSources;
        this.times = sourceTimes;


        this.descriptor = descriptor;
        if(probSevereUrl == null || probSevereUrl.length() == 0)
            setName("JSON: NCEP ProbSevere(server)" );
        else
            setName("JSON: " + IOUtil.stripExtension(IOUtil.getFileTail(probSevereUrl)));
        //initPolygonColorMap();
        //getIdv().getIdvUIManager().showDataSelector();
    }

    /**
     * Get the list of times to compare to the time driver times
     *
     * @param dataChoice  the data choice
     * @param selection   the selection (for things like level)
     * @param timeDriverTimes  the time driver times (use range for server query)
     *
     * @return  the list of times for comparison
     */
    public List<DateTime> getAllTimesForTimeDriver(DataChoice dataChoice,
                                                   DataSelection selection, List<DateTime> timeDriverTimes) {

        int num = timeDriverTimes.size();
        Collections.sort(timeDriverTimes);
        TDSRadarDatasetCollection collection;
        List  collectionTimes = dataChoice.getAllDateTimes();


        List results = null;
        try {
            results = DataUtil.selectDatesFromList(collectionTimes,
                    timeDriverTimes);
            //for(Object dt: results){
            //    System.out.println("Times " + dt);
            //}
        } catch (Exception e) {}
        //initTimes = results;

        return results;

    }
    /**
     * write image as a kml to file
     *
     * @param kmlFilename   kml filename
     * @param bounds        _image bounds
     * @param imageFileName image filename
     * @throws FileNotFoundException On badness
     * @throws IOException           On badness
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
     * @param bounds        bounds
     * @param imageFileName image
     * @return kml
     * @throws FileNotFoundException On badness
     * @throws IOException           On badness
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
     * @param sb     buffer to add to
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
            List colors = new ArrayList();
            List resources =
                    Misc.newList("/ucar/unidata/idv/resources/probsevere_cmap.xml");
            XmlResourceCollection colorMapResources = new XmlResourceCollection("", resources);
            Element root = colorMapResources.getRoot(0);
            colors = processXml(root);
            colorMap = colors;
        }
    }

    /**
     * Get color map from xml file
     *
     * @param
     */
    public List processXml(Element root) {
        List colors = new ArrayList();
        List colorNodes = XmlUtil.findChildren(root, "color");
        for (int i = 0; i < colorNodes.size(); i++) {
            Element serverNode = (Element) colorNodes.get(i);
            String aval = XmlUtil.getAttribute(serverNode, "a");
            String bval = XmlUtil.getAttribute(serverNode, "b");
            String gval = XmlUtil.getAttribute(serverNode, "g");
            String rval = XmlUtil.getAttribute(serverNode, "r");

            float[] values = new float[]{Float.parseFloat(rval),Float.parseFloat(gval),
                    Float.parseFloat(bval),Float.parseFloat(aval)};

            colors.add(values);
        }
        return colors;
    }

    /**
     * Get color map from xml file
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
        dataChoices = null;
        initGeoJsonDataSourceInner();
        getDataChoices();
        super.reloadData();
    }

    /**
     * Initialize after we have been unpersisted
     */
    public void initAfterUnpersistence() {
        //For legacy bundles
        if (legacyKmlUrl != null) {
            sources = Misc.newList(legacyKmlUrl);
        } else {
            try {
                init(legacyKmlUrl);
            } catch (Exception ee){}
        }
        super.initAfterUnpersistence();
        //initKmlDataSource();
        initPolygonColorMap();
        initGeoJsonDataSourceInner();
    }

    /**
     * Read the image
     *
     * @param url     image url
     * @param baseUrl Where the kml came from_
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
                if (!url.startsWith("http")) {
                    url = IOUtil.getFileRoot(baseUrl) + "/" + url;
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
     * @return bytes
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
     * @return input stream to kml file
     * @throws Exception On badness
     */
    protected InputStream getInputStream(String path) throws Exception {
        if (new File(path).exists()) {
            return new FileInputStream(path);
        }

        try {
            URL url = new URL(path);
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
     * init
     */
    private void initGeoJsonDataSourceInner() {
        try {
            if (root != null) {
                return;
            }
            if (getFilePath() == null) {
                return;
            } else if(getFilePath().startsWith(DEFAULT_PATH)){
                init(DEFAULT_PATH);
                String fPath = (String) this.sources.get(0);
                //parseJSON(fPath);
                this.timeList = Misc.sort(this.times);
            } else {
                int size = this.sources.size();
                for (int i = 0; i < size; i++) {
                    String fPath = (String) this.sources.get(i);
                    parseJSONHead(fPath);
                }
                this.timeList = Misc.sort(this.timeList);
                this.times = timeList;
            }
            this.timeList = Misc.sort(this.timeList);
            this.times = timeList;
        } catch (Exception iexc) {
            LogUtil.logException(
                    "There was an error parsing the xml file:\n "
                            + getFilePath(), iexc);
            setInError(true, false, "");
        }
    }
    /**
     * Get the json object from URL
     *
     * @param jsonUrl json url
     * @return Object jsonObj
     * @throws Exception On badness
     */
    private Object getRemoteJSON(String jsonUrl) throws Exception {

        URL url = new URL(jsonUrl);
        URLConnection request = url.openConnection();
        request.connect();

        JSONObject resObj = (JSONObject)new JSONParser().parse(new InputStreamReader((InputStream) request.getContent()));
        return resObj;
    }
    /**
     * Get the kml root element
     *
     * @param jsonUrl json url
     * @return Root element
     * @throws Exception On badness
     */
    protected void parseJSON(String jsonUrl) throws Exception {

        //JSON parser object pour lire le fichier

        JSONParser jsonParser = new JSONParser();
        BufferedReader reader1 = null;
        try {
            // lecture du fichier
            Object obj = null;
            if(!jsonUrl.startsWith("http")) {
                FileReader reader = new FileReader(jsonUrl);
                obj = jsonParser.parse(reader);
            } else {
                obj = getRemoteJSON(jsonUrl);
            }
            JSONObject feature = (JSONObject) obj;
            JSONArray features = (JSONArray) feature.get("features");
            String dateTimePattern = "yyyyMMdd_hhnnss UTC";
            DateTime dts = null;
            String descrip = (String) feature.get("validTime");
            if (descrip != null) {
                //System.out.println(descrip);
                DatePattern dp = new DatePattern(dateTimePattern);
                if (dp.match(descrip)) {
                    dts = dp.getDateTime();
                }
            }

            // Construction initiale du KML avec un factory
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.newDocument();

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
            HashMap ploygoncoordsHail = new HashMap();
            HashMap ploygoncoordsSevere = new HashMap();
            HashMap ploygoncoordsWind = new HashMap();
            HashMap ploygoncoordsTor = new HashMap();
            int ii = 0;
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
                JSONObject featJ = (JSONObject) feat;
                // Recuperation des prorietes de l'objet feature
                JSONObject featJSON = (JSONObject) featJ.get("properties");

                JSONObject coordJSON = (JSONObject) featJ.get("geometry");
                String type = (String) coordJSON.get("type");
                JSONArray coords = (JSONArray) coordJSON.get("coordinates");
                JSONObject modelsJSON = (JSONObject) featJ.get("models");

                if (modelsJSON != null) {
                    String name = (String) featJSON.get("ID");
                    if (name == null) {
                        name = (String) featJ.get("id");
                    }
                    if (type.equals("Polygon") || type.equals("MultiPolygon")) {
                        JSONObject probsevereJSON = (JSONObject) modelsJSON.get("probsevere");
                        JSONObject probwindJSON = (JSONObject) modelsJSON.get("probwind");
                        JSONObject probhailJSON = (JSONObject) modelsJSON.get("probhail");
                        JSONObject probtorJSON = (JSONObject) modelsJSON.get("probtor");

                        ploygoncoordsSevere.put(name, parseJSONModel(coords , probsevereJSON));
                        ploygoncoordsWind.put(name, parseJSONModel(coords , probwindJSON));
                        ploygoncoordsHail.put(name, parseJSONModel(coords , probhailJSON));
                        ploygoncoordsTor.put(name, parseJSONModel(coords , probtorJSON));
                    } else {
                        throw new Error("Type mal forme !");
                    }
                } else {
                    String name1 = (String) featJSON.get("name");
                    if(name1 == null){
                        Object obj1 = featJSON.get("OBJECTID");
                        if(obj1 != null)
                            name1 = obj1.toString();
                        else {
                            name1 = jsonUrl + ii;
                            ii++;
                        }
                    }
                    if (type.equals("Polygon") || type.equals("MultiPolygon")) {
                        ploygoncoords.put(name1, getCoordinates(coords));
                    } else {
                        throw new Error("Type mal forme !");
                    }
                }
            }
            if (dts != null) {
                HashMap ploygons = new HashMap();
                if (ploygoncoordsSevere.size() > 0) {
                    ploygons.put("Probability Severe Weather", ploygoncoordsSevere);
                }
                if (ploygoncoordsWind.size() > 0) {
                    ploygons.put("Probability Wind", ploygoncoordsWind);
                }
                if (ploygoncoordsHail.size() > 0) {
                    ploygons.put("Probability Hail", ploygoncoordsHail);
                }
                if (ploygoncoordsTor.size() > 0) {
                    ploygons.put("Probability Tornado", ploygoncoordsTor);
                }
                jsonInfo.put(dts, ploygons);
                timeList.add(dts);
            } else
                jsonInfo.put("polygon", ploygoncoords);
            //this.timeList = Misc.sort(this.timeList);
        } catch (IOException | ParseException | ParserConfigurationException e) {
            e.printStackTrace();
        }

    }

    /**
     * Get the time list information
     *
     * @param jsonUrl json url
     * @return Root element
     * @throws Exception On badness
     */
    protected void parseJSONHead(String jsonUrl) throws Exception {

        //JSON parser object pour lire le fichier

        JSONParser jsonParser = new JSONParser();
        BufferedReader reader1 = null;
        try {
            // lecture du fichier
            Object obj = null;
            if(!jsonUrl.startsWith("http")) {
                FileReader reader = new FileReader(jsonUrl);
                obj = jsonParser.parse(reader);
            } else {
                obj = getRemoteJSON(jsonUrl);
            }
            JSONObject feature = (JSONObject) obj;
            JSONArray features = (JSONArray) feature.get("features");
            String dateTimePattern = "yyyyMMdd_hhnnss UTC";
            DateTime dts = null;
            String descrip = (String) feature.get("validTime");
            if (descrip != null) {
                //System.out.println(descrip);
                DatePattern dp = new DatePattern(dateTimePattern);
                if (dp.match(descrip)) {
                    dts = dp.getDateTime();
                }
            }
            if(dts != null)
                timeList.add(dts);
            else
                parseJSON(jsonUrl);
        } catch (IOException | ParseException | ParserConfigurationException e) {
            e.printStackTrace();
        }

    }

    /**
     *  parsing the json object coordinate and properties
     *
     * @param
     */
    public HashMap parseJSONModel(JSONArray coords , JSONObject probJSON){
        HashMap coordsNproperties = new HashMap();

        String prob = (String) probJSON.get("PROB");
        String probSevereAll = jsonObjToString(probJSON);
        coordsNproperties.put("coordinates", getCoordinates(coords, prob));
        coordsNproperties.put("properties",  probSevereAll);

        return coordsNproperties;
    }

    /**
     *  build the json object string
     *
     * @param
     */
    public String jsonObjToString(JSONObject jsonObj) {
        StringBuilder sb = new StringBuilder();
        Set keys = jsonObj.keySet();

        for(Object k :keys){
            sb.append(k.toString());
            sb.append('=');
            sb.append(jsonObj.get(k).toString());
            sb.append(System.lineSeparator());
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    /**
     *  build the polygon coordinate string
     *
     * @param
     */
    private  String getCoordinates( JSONArray coords, String prob) {
        String outStr = coords.toJSONString();
        float probability = Float.parseFloat(prob);
        int idx = (int)(255*(probability/100.0)) ;
        String colorStr = "255,0,0";
        //if(probability > 40)
        //    System.out.println("here");
        if(this.colorMap != null ){
            float [] values = (float [])this.colorMap.get(idx);
            int blue = (int)(values[2]*255 );
            int green = (int)(values[1]*255);
            int red = (int)(values[0]*255);
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
     *  build the polygon coordinate string
     *
     * @param
     */
    private  String getCoordinates( JSONArray coords) {
        String outStr = coords.toJSONString();

        outStr = outStr.replaceAll("\\[", "");
        outStr = outStr.replaceAll("\\]", "");
        outStr = "<polygon" + " smooth=" + "\"false\"" + " filled=" + "\"false\"" + " coordtype=" + "\"LONLAT\"" + " points=" + "\"" + outStr + "\"" +
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
        if (timeList != null) {
            return timeList;
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
        if(timeList == null)
            return null;
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
        if(   timeList.size() > 0) {
            processNode(null,   new ArrayList());
        }  else {
            processNodeNoTime(null,  new ArrayList());
        }

        //processNode(null, polygonHMap,  new ArrayList(),dt);
        Trace.call1("JsonDataSource.processing kml");


       // List dataChoices =   getDataChoices();
        // CompositeDataChoice dc0 = (CompositeDataChoice) dataChoices.get(0);
        // List dataChoices0 = dc0.getDataChoices();

        /*for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dc = (DataChoice) dataChoices.get(i);
            DataSelection dataSelection = dc.getDataSelection();
            if(dataSelection == null)
                dataSelection = new DataSelection();
            //dataSelection.setTimes(timeList);
            //dc.setDataSelection();
            if (dc instanceof CompositeDataChoice) {

               // mergeChildren((CompositeDataChoice) dc);
            }
        }  */
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
     * @param parentDataChoice data choice
     * @param displayCategories display categories for uniqueness
     */
    private void processNodeNoTime(CompositeDataChoice parentDataChoice,
                              List displayCategories) {
        //        System.err.println ("tag:" + tagName);
        //DateTime dts = (DateTime)jsonInfo.get("timeObj");
        //        System.err.println("tag:" + tagName);
        List currentDisplayCategories = new ArrayList(displayCategories);
        String dcname = getName();
        CompositeDataChoice newParentDataChoice =
            new CompositeDataChoice(
                this, "All Warning", dcname, dcname ,
                Misc.newList(DataCategory.XGRF_CATEGORY));
        newParentDataChoice.setProperty("Polygon", "folderName");
        newParentDataChoice.setProperty("isProbsevere", true);
        //newParentDataChoice.setUseDataSourceToFindTimes(true);
        //newParentDataChoice.getProperties().put("timeObj", dts);
        addDataChoice(newParentDataChoice, parentDataChoice);
        List categories =
                Misc.newList(DataCategory.XGRF_CATEGORY);
        //Hashtable properties = new Hashtable();

            HashMap plyCoords = (HashMap) jsonInfo.get("polygon");

           // if (dt != null)
           //     properties.put("timeObj", dt);
            Set keys = plyCoords.keySet();
            for (Object k : keys) {
                String name = (String) k;
                Hashtable newProperties = new Hashtable();
                String coordStr = (String) plyCoords.get(k);
                newProperties.put("coordStr", coordStr);
                //newProperties.put("timeObj", dt);
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
                        displayCategory, null);
                //newParentDataChoice.addDataChoice(new DirectDataChoice(this,
                //       ID_DUMMY, name, name + "coordinates", categories));
                newProperties.put("isProbsevere", true);
                DataChoice dc = new DirectDataChoice(
                        this, id, name, name, categories,
                        newProperties);
                DataSelection dataSelection = dc.getDataSelection();
                if(dataSelection == null)
                    dataSelection = new DataSelection();
                //dataSelection.setTimes(Misc.newList(dt));
                dc.setDataSelection(dataSelection);
                newParentDataChoice.addDataChoice(dc);
            }
       // }

    }

    /**
     * Walk the kml tree
     *
     *
     * @param parentDataChoice data choice
     * @param displayCategories display categories for uniqueness
     */
    private void processNode(CompositeDataChoice parentDataChoice, List displayCategories) {
        //        System.err.println("tag:" + tagName);

        List noDisplayCatList = new ArrayList();
        noDisplayCatList.add(DataCategory.NONE_CATEGORY);
        CompositeDataChoice newParentDataChoice =
                new CompositeDataChoice(
                        this, "All Warning", "ProbSevere Models", "Probability Severe Weather Models" ,
                        noDisplayCatList);
        //newParentDataChoice.setProperty("JsonWARNING", "folderName");
        //newParentDataChoice.setUseDataSourceToFindTimes(true);
        //newParentDataChoice.getProperties().put("isProbsevere", true);
        addDataChoice(newParentDataChoice);
        //List categories =
        //        Misc.newList(DataCategory.XGRF_CATEGORY);
        Map<String, String> modelsMap  = new HashMap<String, String>() {{
            put("probSevere", "Probability Severe Weather");
            put("probTornado", "Probability Tornado");
            put("probHail", "Probability Hail");
            put("probWind", "Probability Wind");
        }};
        Set modelkeys =  modelsMap.keySet();
        for (Object mk : modelkeys) {
            String modelName = (String)mk;
            String modelFullName = modelsMap.get(mk);
            Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                    "/ucar/unidata/ui/symbol/images/" + modelName + ".png");
            DataChoice  dataChoice0 =
                        new DirectDataChoice(
                                this, "NCEP ProbSevereModels", modelFullName, modelFullName,
                                Misc.newList(DataCategory.XGRF_CATEGORY), props);

            dataChoice0.setProperty("JsonWARNING", "folderName0");
            dataChoice0.setProperty("isProbsevere", true);
            DataSelection dataSelection = dataChoice0.getDataSelection();
            if (dataSelection == null)
                dataSelection = new DataSelection();
            dataSelection.setTimes( times);
            dataChoice0.setDataSelection(dataSelection);
            addDataChoice(dataChoice0, newParentDataChoice);
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
        List times0 = null;

        Object cu = dataSelection.getProperty(DataSelection.PROP_USESTIMEDRIVER);
        if (cu != null) {
            useDriverTime = ((Boolean) cu).booleanValue();
        }

        if (dataSelection != null) {
            times0 = getTimesFromDataSelection(dataSelection, dataChoice);
        }
        //if (times0 == null) {
        //    times0 = dataChoice.getSelectedDateTimes();
        //}

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
            DateTime dt = null;
            HashMap polygonInfo = null;
            List timeSL = dataSelection.getTimes();
            if(useDriverTime){
                int size = times0.size();
                List  allTimes = dataChoice.getAllDateTimes();
                for (int i = 0; i < size; i++) {
                    int ii = allTimes.indexOf(times0.get(i));
                    String fPath = (String) this.sources.get(ii);
                    try {
                        parseJSON(fPath);
                    } catch (Exception eee) {
                    }
                }

                List pdata0 = new ArrayList();

                for (int i = 0; i < size; i++) {
                    int ii = allTimes.indexOf(times0.get(i));

                    //KmlId id = (KmlId) tmp;
                    Hashtable dcproperties = dataChoice.getProperties();
                    dt = (DateTime) times.get(ii);
                    //System.out.println(" ss " + dt);
                    List subTimeList = times0;
                    //System.out.println(" ID " + id);

                    if (dt != null)
                        polygonInfo = (HashMap) jsonInfo.get(dt);
                    else
                        polygonInfo = (HashMap) jsonInfo.get("polygon");
                    HashMap models = (HashMap) jsonInfo.get(dt);
                    if(models == null)
                        break;
                    HashMap plyCoords = (HashMap) models.get(dataChoice.getDescription());
                    Set keys = plyCoords.keySet();
                    DateTime polygonTime = dt;

                    if (subTimeList.contains(dt)) {
                        String timeStr = null;
                        if (polygonTime != null) {
                            timeStr = polygonTime.toString();
                            polygonInfo.put("time", timeStr);
                        }
                        for (Object k : keys) {
                            String name = (String) k;
                            // Hashtable newProperties = new Hashtable();
                            HashMap coordsNporperties = (HashMap) plyCoords.get(k);
                            String coordStr = (String) coordsNporperties.get("coordinates");
                            String probStr = (String)coordsNporperties.get("properties");
                            //String coordsStr = (String) dcproperties.get("coordStr");
                            List toks = StringUtil.split(probStr, "\n", false, true);
                            String[] stockArr = new String[toks.size()];
                            toks.toArray(stockArr);
                            StringUtil.string_Sort(stockArr, 0, 6);
                            String probStr0 = String.join("\n", stockArr);
                            probStr = probStr0.replaceAll("LINE\\d\\d=", "");
                            dcproperties.put(name, probStr);
                            StringBuffer sb0 = new StringBuffer("<shapes>\n");
                            sb0 = sb0.append(coordStr);
                            int idx = sb0.indexOf("polygon");
                            String nameStr = "name=\"" + name + "\" ";
                            String tStr = "times=\"" + timeStr + "\" ";
                            sb0.insert(idx + 8, nameStr);
                            sb0.insert(idx + 8 + nameStr.length(), tStr);
                            sb0.append("</shapes>");
                            pdata0.add(new visad.Text(sb0.toString()));
                        }
                    }
                }
                StringBuffer sb = new StringBuffer("<shapes>\n");
                for (int j = 0; j < pdata0.size(); j++) {
                    visad.Text vt = (visad.Text) pdata0.get(j);
                    if (vt != null) {
                        String ttt = vt.toString();
                        ttt = ttt.replace("<shapes>\n", "");
                        ttt = ttt.replace("</shapes>", "");
                        sb = sb.append(ttt);
                    }
                }
                sb.append("</shapes>");

                //System.out.println(" ss " + sb);
                return new visad.Text(sb.toString());
            } else if(timeSL != null && timeSL.size() > 0) {
                int size = timeSL.size();
                for (int i = 0; i < size; i++) {
                    int ii = i;
                    if(timeSL.get(i) instanceof Integer) {
                        if((int)timeSL.get(size-1) > this.sources.size()){
                            ii = this.sources.size() - size + i;
                        } else {
                            ii = (int) timeSL.get(i);
                        }
                    }

                    String fPath = (String) this.sources.get(ii);
                    try {
                        parseJSON(fPath);
                    } catch (Exception eee) {
                    }
                }

                List pdata0 = new ArrayList();

                for (int i = 0; i < size; i++) {
                    int ii = i;
                    if (timeSL.get(i) instanceof Integer) {
                        if ((int) timeSL.get(size - 1) > this.sources.size()) {
                            ii = this.sources.size() - size + i;
                        } else {
                            ii = (int) timeSL.get(i);
                        }
                    }

                    //KmlId id = (KmlId) tmp;
                    Hashtable dcproperties = dataChoice.getProperties();
                    dt = (DateTime) times.get(ii);
                    //System.out.println(" ss " + dt);
                    List subTimeList = getJsonTimes(dataSelection);
                    //System.out.println(" ID " + id);

                    if (dt != null)
                        polygonInfo = (HashMap) jsonInfo.get(dt);
                    else
                        polygonInfo = (HashMap) jsonInfo.get("polygon");
                    HashMap models = (HashMap) jsonInfo.get(dt);
                    if(models == null)
                        break;
                    HashMap plyCoords = (HashMap) models.get(dataChoice.getDescription());
                    Set keys = plyCoords.keySet();
                    DateTime polygonTime = dt;

                    if (subTimeList.contains(dt)) {
                        String timeStr = null;
                        if (polygonTime != null) {
                            timeStr = polygonTime.toString();
                            polygonInfo.put("time", timeStr);
                        }
                        for (Object k : keys) {
                            String name = (String) k;
                            // Hashtable newProperties = new Hashtable();
                            HashMap coordsNporperties = (HashMap) plyCoords.get(k);
                            String coordStr = (String) coordsNporperties.get("coordinates");
                            String probStr = (String)coordsNporperties.get("properties");
                            //String coordsStr = (String) dcproperties.get("coordStr");
                            List toks = StringUtil.split(probStr, "\n", false, true);
                            String[] stockArr = new String[toks.size()];
                            toks.toArray(stockArr);
                            StringUtil.string_Sort(stockArr, 0, 6);
                            String probStr0 = String.join("\n", stockArr);
                            probStr = probStr0.replaceAll("LINE\\d\\d=", "");
                            dcproperties.put(name, probStr);
                            StringBuffer sb0 = new StringBuffer("<shapes>\n");
                            sb0 = sb0.append(coordStr);
                            int idx = sb0.indexOf("polygon");
                            String nameStr = "name=\"" + name + "\" ";
                            String tStr = "times=\"" + timeStr + "\" ";
                            sb0.insert(idx + 8, nameStr);
                            sb0.insert(idx + 8 + nameStr.length(), tStr);
                            sb0.append("</shapes>");
                            pdata0.add(new visad.Text(sb0.toString()));
                        }
                    }
                }
                StringBuffer sb = new StringBuffer("<shapes>\n");
                for (int j = 0; j < pdata0.size(); j++) {
                    visad.Text vt = (visad.Text) pdata0.get(j);
                    if (vt != null) {
                        String ttt = vt.toString();
                        ttt = ttt.replace("<shapes>\n", "");
                        ttt = ttt.replace("</shapes>", "");
                        sb = sb.append(ttt);
                    }
                }
                sb.append("</shapes>");

                //System.out.println(" ss " + sb);
                return new visad.Text(sb.toString());
            } else if(times0 != null && times0.size() > 0) {
                int size = times0.size();
                for (int i = 0; i < size; i++) {
                    //int ii = (int) timeSL.get(i);
                    String fPath = (String) this.sources.get(i);
                    try {
                        parseJSON(fPath);
                    } catch (Exception eee) {
                    }
                }

                List pdata0 = new ArrayList();

                for (int i = 0; i < size; i++) {
                    //int ii = (int) timeSL.get(i);

                    //KmlId id = (KmlId) tmp;
                    Hashtable dcproperties = dataChoice.getProperties();
                    dt = (DateTime) times0.get(i);
                    //System.out.println(" ss " + dt);
                    List subTimeList = getJsonTimes(dataSelection);
                    //System.out.println(" ID " + id);

                    if (dt != null)
                        polygonInfo = (HashMap) jsonInfo.get(dt);
                    else
                        polygonInfo = (HashMap) jsonInfo.get("polygon");
                    HashMap models = (HashMap) jsonInfo.get(dt);
                    if(models == null)
                        break;
                    HashMap plyCoords = (HashMap) models.get(dataChoice.getDescription());
                    Set keys = plyCoords.keySet();
                    DateTime polygonTime = dt;

                    if (subTimeList.contains(dt)) {
                        String timeStr = null;
                        if (polygonTime != null) {
                            timeStr = polygonTime.toString();
                            polygonInfo.put("time", timeStr);
                        }
                        for (Object k : keys) {
                            String name = (String) k;
                            // Hashtable newProperties = new Hashtable();
                            HashMap coordsNporperties = (HashMap) plyCoords.get(k);
                            String coordStr = (String) coordsNporperties.get("coordinates");
                            String probStr = (String)coordsNporperties.get("properties");
                            //String coordsStr = (String) dcproperties.get("coordStr");
                            List toks = StringUtil.split(probStr, "\n", false, true);
                            String[] stockArr = new String[toks.size()];
                            toks.toArray(stockArr);
                            StringUtil.string_Sort(stockArr, 0, 6);
                            String probStr0 = String.join("\n", stockArr);
                            probStr = probStr0.replaceAll("LINE\\d\\d=", "");
                            dcproperties.put(name, probStr);
                            StringBuffer sb0 = new StringBuffer("<shapes>\n");
                            sb0 = sb0.append(coordStr);
                            int idx = sb0.indexOf("polygon");
                            String nameStr = "name=\"" + name + "\" ";
                            String tStr = "times=\"" + timeStr + "\" ";
                            sb0.insert(idx + 8, nameStr);
                            sb0.insert(idx + 8 + nameStr.length(), tStr);
                            sb0.append("</shapes>");
                            pdata0.add(new visad.Text(sb0.toString()));
                        }
                    }
                }
                StringBuffer sb = new StringBuffer("<shapes>\n");
                for (int j = 0; j < pdata0.size(); j++) {
                    visad.Text vt = (visad.Text) pdata0.get(j);
                    if (vt != null) {
                        String ttt = vt.toString();
                        ttt = ttt.replace("<shapes>\n", "");
                        ttt = ttt.replace("</shapes>", "");
                        sb = sb.append(ttt);
                    }
                }
                sb.append("</shapes>");

                //System.out.println(" ss " + sb);
                return new visad.Text(sb.toString());
            } else if (dt == null  ){
                Hashtable dcproperties  = dataChoice.getProperties();
                String coordsStr = (String) dcproperties.get("coordStr");
                StringBuffer sb = new StringBuffer("<shapes>\n");
                sb = sb.append(coordsStr);
                sb.append("</shapes>");
                int idx = sb.indexOf("polygon");
                String name = dataChoice.getName();
                String nameStr = "name=\"" + name + "\" ";
                sb.insert(idx + 8, nameStr);
                //System.out.println(" ss " + sb);
                return new visad.Text(sb.toString());
            } else {
                return null;
            }
        }
        return null;
    }

    public int getTimeIndexWithBounds(DateTime dateTime, List<DateTime> allTimes, CoverageCoordAxis1D cca){
        int index = allTimes.indexOf(dateTime);
        if (index < 0)
            return 0;

        int lastindex = allTimes.lastIndexOf(dateTime);
        if(index == lastindex)
            return index;
        Object bbb = cca.getCoordObject(index);
        double [] bounds = null;
        if(bbb instanceof double[])
            bounds = (double [])bbb;
        //double [] bounds = (double [])cca.getCoordObject(index);
        int finalIndex0 = index;
        if(bounds != null && bounds[1] != bounds[0]) {
            double min = bounds[1] - bounds[0];
            for (int ii = index + 1; ii <= lastindex; ii++) {
                bounds = (double [])cca.getCoordObject(ii);
                double tmp = bounds[1] - bounds[0];
                if(tmp < min)
                    finalIndex0 = ii;

            }
            index = finalIndex0;
        }
        return index;
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
                int ii = i;
                int size = timeSL.size();
                if(timeSL.get(i) instanceof Integer) {
                    if((int)timeSL.get(size-1) > this.sources.size()){
                        ii = this.sources.size() - size + i;
                    } else {
                        ii = (int) timeSL.get(i);
                    }
                }
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

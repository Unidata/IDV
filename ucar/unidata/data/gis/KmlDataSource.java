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

import ucar.unidata.data.*;


import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;

import ucar.unidata.xml.XmlUtil;


import visad.*;

import java.awt.*;

import java.io.*;


import java.net.URL;
import java.net.URLConnection;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Hashtable;
import java.util.List;
import java.util.zip.*;



/**
 * DataSource for Web Map Servers
 *
 * @author IDV development team
 * @version $Revision: 1.38 $ $Date: 2007/04/16 20:34:52 $
 */
public class KmlDataSource extends FilesDataSource {

    /** property */
    private static final String PROP_HREF = "prop.href";

    /** property */
    private static final String PROP_BASEURL = "prop.baseurl";

    /** property */
    private static final String PROP_DISPLAYCATEGORIES =
        "prop.displaycategories";


    /** For ximg files */
    public static final PatternFileFilter FILTER_KML =
        new PatternFileFilter("(.+\\.kml|.+\\.kmz)",
                              "Google Earth File (*.kml, *.kmz)", ".kmz");

    /** data choice id */
    private static final String ID_NETWORKLINK = "KmlDataSource.networklink";

    /** dummy id */
    private static final String ID_DUMMY = "KmlDataSource.dummy";


    /** For ximg files */
    public static final String EXT_KML = ".kml";


    /** For ximg files */
    public static final String SUFFIX_KML = ".kml";


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
    public static final String TAG_KML = "kml";

    /** kml tag id */
    public static final String TAG_PLACEMARK = "Placemark";

    /** kml tag id */
    public static final String TAG_NAME = "name";


    /** mapping of id to kml node */
    private Hashtable idToNode = new Hashtable();


    /** A local cache */
    protected List cachedData = new ArrayList();

    /** The urls */
    protected List cachedUrls = new ArrayList();


    /** Is the kml file a zip file */
    private boolean isZip;

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


    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public KmlDataSource() {}


    /**
     * Create a KmlDataSource from the specification given.
     *
     * @param descriptor          descriptor for the data source
     * @param kmlUrl Where the kml came from
     * @param properties          extra properties
     *
     * @throws VisADException     some problem occurred creating data
     */
    public KmlDataSource(DataSourceDescriptor descriptor, String kmlUrl,
                         Hashtable properties)
            throws VisADException {
        super(descriptor, Misc.newList(kmlUrl), "KML data source", kmlUrl,
              properties);
        setName("KML: " + IOUtil.stripExtension(IOUtil.getFileTail(kmlUrl)));
        initKmlDataSource();
    }

    /**
     * reload
     */
    public void reloadData() {
        root = null;
        super.reloadData();
        dataChoices = null;
        initKmlDataSource();
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
        initKmlDataSource();
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
        if (kmlUrl.toLowerCase().endsWith(".kmz")) {
            BufferedInputStream bin =
                new BufferedInputStream(IOUtil.getInputStream(kmlUrl));
            ZipInputStream zin = new ZipInputStream(bin);
            ZipEntry       ze  = null;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName().toLowerCase();
                if (name.equals(path) || name.equalsIgnoreCase(path)) {
                    return zin;
                }
                name = "./" + name;
                if (name.equals(path) || name.equalsIgnoreCase(path)) {
                    return zin;
                }
            }
            return null;
        } else {
            if (IOUtil.isRelativePath(path)) {
                path = IOUtil.getFileRoot(kmlUrl) + "/" + path;
            }
            return IOUtil.getInputStream(path);
        }
    }


    /**
     * Initialization method
     */
    private void initKmlDataSource() {
        Trace.call1("KmlDataSource.initKml");
        initKmlDataSourceInner();
        Trace.call2("KmlDataSource.initKml");
    }

    /**
     * init
     */
    private void initKmlDataSourceInner() {

        if (root != null) {
            return;
        }
        if (getFilePath() == null) {
            return;
        }
        try {
            root = parseKml(getFilePath());
            if (root == null) {
                setInError(true, "Could not find kml file");
            }
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
     * @param kmlUrl kml url
     *
     * @return Root element
     *
     * @throws Exception On badness
     */
    private Element parseKml(String kmlUrl) throws Exception {
        if (kmlUrl == null) {
            return null;
        }
        Element             root = null;
        InputStream         is   = IOUtil.getInputStream(kmlUrl);
        BufferedInputStream bin  = new BufferedInputStream(is);
        ZipInputStream      zin  = null;
        try {
            Trace.call1("zin");
            zin = new ZipInputStream(bin);
            Trace.call2("zin");
        } catch (Exception exc) {}

        boolean anyEntries = false;
        if ((zin != null) && (zin.available() > 0)) {
            Trace.call1("getInputStream");
            Trace.call2("getInputStream");

            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                anyEntries = true;
                String name = ze.getName().toLowerCase();
                if (name.endsWith(".kml") && (root == null)) {
                    isZip = true;
                    Trace.call1("readBytes");
                    String xml = new String(IOUtil.readBytes(zin, null,
                                     false));
                    if ( !xml.startsWith("<")) {
                        //Strip off the crap I keep finding in the kmz files
                        int idx = xml.indexOf("<");
                        if (idx >= 0) {
                            xml = xml.substring(idx);
                        }
                    }
                    Trace.call2("readBytes", " size=" + xml.length());
                    String newName = IOUtil.getFileTail(name);
                    xml = cleanupXml(xml);
                    IOUtil.writeFile(newName, xml);
                    Trace.call1("KmlDataSource.getRoot");
                    root = XmlUtil.getRoot(xml);
                    Trace.call2("KmlDataSource.getRoot");
                    //                      break;
                } else {
                    pathToFile.put(ze.getName(), kmlUrl);
                    zin.closeEntry();
                }
            }
            zin.close();
        }

        if ((root == null) && !anyEntries) {
            Trace.call1("KmlDataSource.readContents");
            String xml = IOUtil.readContents(kmlUrl);
            Trace.call2("KmlDataSource.readContents");
            xml = cleanupXml(xml);
            //            is    = IOUtil.getInputStream(kmlUrl);
            isZip = false;
            Trace.call1("KmlDataSource.getRoot");
            //            root = XmlUtil.getRoot(is);
            root = XmlUtil.getRoot(xml);
            Trace.call2("KmlDataSource.getRoot");
        }
        return root;
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
        if (root == null) {
            initKmlDataSource();
        }
        if (root == null) {
            throw new IllegalStateException("No KML root element");
        }

        schemas = new Hashtable();



        processRootNode(root, null, getFilePath(), new ArrayList());
        Trace.call1("KmlDataSource.processing kml");
        Trace.call2("KmlDataSource.processing kml");


        List dataChoices = getDataChoices();
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice dc = (DataChoice) dataChoices.get(i);
            if (dc instanceof CompositeDataChoice) {
                mergeChildren((CompositeDataChoice) dc);
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
    private void processRootNode(Element root, CompositeDataChoice parent,
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
            processNode(parent, child, categories, filepath);
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
            Element networkRoot = parseKml(href);
            if (networkRoot == null) {
                throw new IllegalStateException(
                    "Error fetching network link file:" + href);
            }
            currentDisplayCategories.add(parent.getName());
            processRootNode(networkRoot, parent, href,
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
     * @param node node
     * @param displayCategories display categories for uniqueness
     * @param baseUrl where the kml came from
     */
    private void processNode(CompositeDataChoice parentDataChoice,
                             Element node, List displayCategories,
                             String baseUrl) {

        String tagName = node.getTagName();
        //        System.err.println ("tag:" + tagName);
        if (tagName.equals(TAG_SCHEMA)) {
            String name = XmlUtil.getAttribute(node, ATTR_NAME,
                              (String) null);
            String parent = XmlUtil.getAttribute(node, ATTR_PARENT,
                                (String) null);
            if ((name != null) && (parent != null)) {
                schemas.put(name, parent);
            }
            return;
        }

        String tmp = (String) schemas.get(tagName);
        if (tmp != null) {
            tagName = tmp;
        }

        //        System.err.println("tag:" + tagName);
        List currentDisplayCategories = new ArrayList(displayCategories);
        if (tagName.equals(TAG_NETWORKLINK)) {
            Element urlNode = XmlUtil.findChild(node, TAG_URL);
            if (urlNode == null) {
                urlNode = XmlUtil.findChild(node, TAG_LINK);
            }
            if (urlNode == null) {
                throw new IllegalStateException("No Url or Link node found");
            }
            Element hrefNode = XmlUtil.findChild(urlNode, TAG_HREF);
            String name = XmlUtil.getChildText(XmlUtil.findChild(node,
                              TAG_NAME));
            String href = XmlUtil.getChildText(hrefNode);
            Hashtable props = Misc.newHashtable(PROP_DISPLAYCATEGORIES,
                                  new ArrayList(currentDisplayCategories),
                                  PROP_HREF, href, PROP_BASEURL, baseUrl);

            if (name == null) {
                name = "Network Link";
            }
            CompositeDataChoice newParentDataChoice =
                new CompositeDataChoice(
                    this, ID_NETWORKLINK, "Network Link:" + href, name,
                    Misc.newList(DataCategory.NONE_CATEGORY), props);
            newParentDataChoice.addDataChoice(new DirectDataChoice(this,
                    ID_DUMMY, "", "", new ArrayList()));
            addDataChoice(newParentDataChoice, parentDataChoice);
        } else if (tagName.equals(TAG_DOCUMENT) || tagName.equals(TAG_FOLDER)
                   || tagName.equals(TAG_KML)) {
            List pointNodes = null;
            String folderName = XmlUtil.getChildText(XmlUtil.findChild(node,
                                    TAG_NAME));
            if (folderName == null) {
                folderName = "Folder";
            }
            CompositeDataChoice newParentDataChoice =
                new CompositeDataChoice(
                    this, "", folderName, folderName,
                    Misc.newList(DataCategory.NONE_CATEGORY));
            addDataChoice(newParentDataChoice, parentDataChoice);

            if (folderName != null) {
                currentDisplayCategories.add(folderName);
            }

            NodeList children = XmlUtil.getElements(node);
            for (int i = 0; i < children.getLength(); i++) {
                Element child        = (Element) children.item(i);
                String  childTagName = child.getTagName();
                tmp = (String) schemas.get(childTagName);
                if (tmp != null) {
                    childTagName = tmp;
                }
                if (childTagName.equals(TAG_PLACEMARK)) {
                    Element multiGeometryNode =
                        (Element) XmlUtil.findChild(child, TAG_MULTIGEOMETRY);
                    Element linestringNode = 
                        (Element) XmlUtil.findChild(child, TAG_LINESTRING);
                    if (multiGeometryNode != null || linestringNode!=null) {
                        String name =
                            XmlUtil.getChildText(XmlUtil.findChild(child,
                                TAG_NAME));
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
                        KmlId id = new KmlId(KmlId.NODE_SHAPES, name,
                                             displayCategory, baseUrl);
                        idToNode.put(id, child);
                        List xcategories =
                            DataCategory.parseCategories(displayCategory
                                + ";" + "xgrf", true);

                        List categories =
                            Misc.newList(DataCategory.XGRF_CATEGORY);
                        newParentDataChoice.addDataChoice(
                            new DirectDataChoice(
                                this, id, name, name, categories,
                                (Hashtable) null));
                    } else {
                        if (pointNodes == null) {
                            pointNodes = new ArrayList();
                        }
                        pointNodes.add(child);
                    }
                    continue;
                }
                processNode(newParentDataChoice, child,
                            currentDisplayCategories, baseUrl);
            }
            if (pointNodes != null) {
                for (Element n : (List<Element>) pointNodes) {
                    String  name   = XmlUtil.getGrandChildText(n, "name");
                    Element ptnode = XmlUtil.findChild(n, "Point");
                    if (ptnode != null) {
                        String coords = XmlUtil.getGrandChildText(ptnode,
                                            "coordinates");
                    }
                }




                String dataChoiceName = "Placemarks";
                String displayCategory = StringUtil.join("-",
                                             currentDisplayCategories);
                KmlId id = new KmlId(KmlId.NODE_PLACEMARKS, dataChoiceName,
                                     displayCategory, baseUrl);
                idToNode.put(id, pointNodes);
                List categories =
                    DataCategory.parseCategories(displayCategory, true);
                categories = Misc.newList(DataCategory.LOCATIONS_CATEGORY);
                Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                                      "/auxdata/ui/icons/Placemark16.gif");
                newParentDataChoice.addDataChoice(new DirectDataChoice(this,
                        id, dataChoiceName, dataChoiceName, categories,
                        props));
            }
        } else if (tagName.equals(TAG_GROUNDOVERLAY)
                   || tagName.equals(TAG_PHOTOOVERLAY)) {
            //            System.err.println ("got ground overlay:" + XmlUtil.toString(node));
            String name = XmlUtil.getChildText(XmlUtil.findChild(node,
                              TAG_NAME));

            if (name == null) {
                name = "Image";
            }
            String displayCategory = StringUtil.join("-",
                                         currentDisplayCategories);
            String href = KmlGroundOverlay.getHref(node);
            KmlId  id   = new KmlId(tagName.equals(TAG_GROUNDOVERLAY)
                                    ? KmlId.NODE_GROUNDOVERLAY
                                    : KmlId.NODE_PHOTOOVERLAY, name,
                                        displayCategory, baseUrl, href);
            int ucnt = 0;
            while (idToNode.get(id) != null) {
                id.setExtra("id" + (ucnt++));
            }
            idToNode.put(id, node);
            List categories = DataCategory.parseCategories(displayCategory,
                                  true);
            categories.add(new DataCategory("RGBIMAGE", false));
            Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                                  "/auxdata/ui/icons/Earth16.gif");
            addDataChoice(new DirectDataChoice(this, id, name, name,
                    categories, props), parentDataChoice);
        } else if (tagName.equals(TAG_PLACEMARK)) {
            Element multiGeometryNode = (Element) XmlUtil.findChild(node,
                                            TAG_MULTIGEOMETRY);
            if (multiGeometryNode != null) {
                //                addDataChoice(new KmlPolygons(node, displayCategory));
            } else {}
        } else {
            //      System.err.println("Unknown tag:" + tagName);
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
        } else {
            KmlId  id   = (KmlId) tmp;
            Object node = idToNode.get(id);
            if (node == null) {
                throw new BadDataException("Could not find kml data " + id);
            }
            if (id.isPlacemarks()) {
                kmlInfo = new KmlPoints((List) node, "");
            } else if (id.isGroundOverlay()) {
                kmlInfo = new KmlGroundOverlay((Element) node, "",
                        id.getDocUrl());
            } else if (id.isPhotoOverlay()) {
                kmlInfo = new KmlPhotoOverlay((Element) node, "",
                        id.getDocUrl());
            } else if (id.isShapes()) {
                kmlInfo = new KmlPolygons((Element) node, "");
            } else {
                throw new BadDataException("Unknown KML node type "
                                           + id.getType());
            }
        }
        return kmlInfo.getData(this, loadId);
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
        return new ArrayList();
    }




    /**
     *  Set the KmlUrl property.
     *
     *  @param value The new value for KmlUrl
     */
    public void setKmlUrl(String value) {
        legacyKmlUrl = value;
    }


    /**
     * Is this a kml or kmz file
     *
     * @param filename file
     *
     * @return  Is this a kml or kmz file
     */
    public static boolean isKmlFile(String filename) {
        return filename.endsWith(".kml") || filename.endsWith(".kmz");
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
        sb.append("<east>" + bounds.getMaxLon() + "</east>\n");
        sb.append("<west>" + bounds.getMinLon() + "</west>\n");
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
        sts.put("AK", "Alaska");
        sts.put("AL", "Alabama");
        sts.put("AR", "Arkansas");
        sts.put("AZ", "Arizona");
        sts.put("CA", "California");
        sts.put("CO", "Colorado");
        sts.put("DE", "Delaware");
        sts.put("FL", "Florida");
        sts.put("GA", "Georgia");
        sts.put("GU", "Guam");
        sts.put("HI", "Hawaii");
        sts.put("IA", "Idaho");
        sts.put("ID", "Indiana");
        sts.put("IL", "Illinois");
        sts.put("IN", "Indiana");
        sts.put("KS", "Kansas");
        sts.put("KY", "Kentucky");
        sts.put("LA", "Louisiana");
        sts.put("MA", "Massachusetts");
        sts.put("MD", "Maryland");
        sts.put("ME", "Maine");
        sts.put("MI", "Michigan");
        sts.put("MN", "Minnesota");
        sts.put("MO", "Missouri");
        sts.put("MS", "Misisspippi");
        sts.put("MT", "Montana");
        sts.put("NC", "North Carolina");
        sts.put("ND", "North Dakota");
        sts.put("NE", "Nebraska");
        sts.put("NJ", "New Jersey");
        sts.put("NM", "New Mexico");
        sts.put("NV", "Nevada");
        sts.put("NY", "New York");
        sts.put("OH", "Ohio");
        sts.put("OK", "Oklahoma");
        sts.put("OR", "Oregon");
        sts.put("PA", "Pennsylvania");
        sts.put("PR", "Puerto Rico");
        sts.put("SC", "South Carolina");
        sts.put("SD", "South Dakota");
        sts.put("TN", "Tennessee");
        sts.put("TX", "Texas");
        sts.put("UT", "Utah");
        sts.put("VA", "Virginia");
        sts.put("VT", "Vermont");
        sts.put("WA", "Washington");
        sts.put("WI", "Wisconsin");
        sts.put("WV", "West Virginia");
        sts.put("WY", "Wyoming");


        try {
            //            <station idn="000525" id="APD" name="Fairbanks/Pedro Dome" st="AK" co="US" lat="65.03" lon="-147.50" elev="790"/>
            StringBuffer sb = new StringBuffer();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">\n");
            sb.append(XmlUtil.tag("name", "", "Nexrad Stations"));
            sb.append(XmlUtil.tag("description", "", "Nexrad Stations"));
            sb.append("<Document>\n");
            sb.append("<LookAt>\n");
            sb.append("<longitude>-100.0</longitude>\n");
            sb.append("<latitude>40</latitude>\n");
            sb.append("<range>5000000</range>\n");
            sb.append("<tilt>0</tilt>\n");
            sb.append("<heading>0</heading>\n");
            sb.append("</LookAt>\n");


            sb.append(XmlUtil.tag("visibility", "", "0"));
            sb.append(XmlUtil.tag("visible", "", "0"));
            sb.append("<Style id=\"radarLocation\">\n");
            sb.append("<IconStyle>\n");
            sb.append("<Icon>\n");
            sb.append(
                XmlUtil.tag(
                    "href", "",
                    "http://www.unidata.ucar.edu/software/idv/logo.gif"));
            sb.append("</Icon>\n");
            sb.append("</IconStyle>\n");
            sb.append("</Style>\n");

            sb.append(
                "<ScreenOverlay>  <name>Unidata Nexrad Radar</name>  <Icon>   <href>http://www.unidata.ucar.edu/img/unidata_logo.gif</href>  </Icon>  <overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>  <screenXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>  <size x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/></ScreenOverlay>");


            Element root = XmlUtil.getRoot("nexradstns.xml",
                                           KmlDataSource.class);
            NodeList elements = XmlUtil.getElements(root);
            //First try breadth first
            double    h        = 41.87594 - 37.63853;
            double    w        = -101.92211 - -107.28374;
            double    h2       = h / 2.0;
            double    w2       = w / 2.0;
            Hashtable stateMap = new Hashtable();
            List      states   = new ArrayList();


            String    alt      = "800000";
            int       year     = 1900;
            for (int i = 0; i < elements.getLength(); i++) {
                Element child = (Element) elements.item(i);
                String st = XmlUtil.encodeString(XmlUtil.getAttribute(child,
                                "st"));
                StringBuffer buff = (StringBuffer) stateMap.get(st);
                if (buff == null) {
                    buff = new StringBuffer("<Folder>\n");
                    String name = (String) sts.get(st);
                    buff.append(XmlUtil.tag("name", "", name));
                    stateMap.put(st, buff);
                    states.add(st);
                }


                String id = XmlUtil.getAttribute(child, "id");
                String name =
                    XmlUtil.encodeString(XmlUtil.getAttribute(child, "name"));
                double lat = XmlUtil.getAttribute(child, "lat", 0.0);
                double lon = XmlUtil.getAttribute(child, "lon", 0.0);

                String desc =
                    "<![CDATA[<a href=\"http://www.unidata.ucar.edu/software/idv/radar.jnlp\">View in the IDV</a>]]>";

                StringBuffer lookAt = new StringBuffer();
                lookAt.append("<LookAt>\n");
                lookAt.append(XmlUtil.tag("longitude", "", "" + lon));
                lookAt.append(XmlUtil.tag("latitude", "", "" + lat));
                lookAt.append(XmlUtil.tag("range", "", alt));
                lookAt.append(XmlUtil.tag("tilt", "", "0"));
                lookAt.append(XmlUtil.tag("heading", "", "0"));
                lookAt.append("</LookAt>\n");


                buff.append("<Folder>\n");




                buff.append(lookAt);
                buff.append(XmlUtil.tag("visibility", "", "0"));
                buff.append(XmlUtil.tag("visible", "", "0"));
                buff.append(XmlUtil.tag("name", "",
                                        XmlUtil.encodeString(name)));
                //              buff.append(XmlUtil.tag("description","",  XmlUtil.encodeString(name)));



                buff.append("<GroundOverlay>\n");

                /**
                 * year++;
                 * buff.append("<TimePeriod>\n");
                 * buff.append("<begin>\n");
                 * buff.append("<TimeInstant>\n");
                 * buff.append("<timePosition>"+year+"</timePosition>\n");
                 * buff.append("</TimeInstant>\n");
                 * buff.append("</begin>\n");
                 *
                 * year++;
                 * buff.append("<end>\n");
                 * buff.append("<TimeInstant>\n");
                 * buff.append("<timePosition>"+year+"</timePosition>\n");
                 * buff.append("</TimeInstant>\n");
                 * buff.append("</end>\n");
                 * buff.append("</TimePeriod>\n");
                 */


                buff.append(XmlUtil.tag("visibility", "", "0"));
                buff.append(XmlUtil.tag("visible", "", "0"));
                buff.append(lookAt);

                buff.append(XmlUtil.tag("description", "", desc));
                buff.append(XmlUtil.tag("name", "", "Image"));
                buff.append("<Icon>\n");
                buff.append(
                    XmlUtil.tag(
                        "href", "",
                        "http://motherlode.ucar.edu/cgi-bin/ldm/radargif?"
                        + id));
                buff.append("</Icon>\n");
                buff.append("<LatLonBox id=\"latlonbox\">\n");
                buff.append("<north>" + (lat + h2) + "</north>\n");
                buff.append("<south>" + (lat - h2) + "</south>\n");
                buff.append("<east>" + (lon + w2) + "</east>\n");
                buff.append("<west>" + (lon - w2) + "</west>\n");
                buff.append("<rotation>0</rotation>\n");
                buff.append("</LatLonBox>\n");
                buff.append("</GroundOverlay>\n");

                buff.append("<Placemark>\n");
                buff.append(XmlUtil.tag("name", "", name));
                buff.append(lookAt);
                buff.append(XmlUtil.tag("styleUrl", "", "#radarLocation"));
                buff.append(XmlUtil.tag("visibility", "", "0"));
                buff.append(XmlUtil.tag("visible", "", "0"));
                buff.append(XmlUtil.tag("description", "", desc));
                buff.append("<Point>\n");
                buff.append(XmlUtil.tag("coordinates", "",
                                        "" + lon + "," + (lat + h2) + ","
                                        + alt));
                buff.append("</Point>\n");
                buff.append("</Placemark>\n");


                buff.append("</Folder>\n");
            }

            for (int i = 0; i < states.size(); i++) {
                StringBuffer b = (StringBuffer) stateMap.get(states.get(i));
                b.append("</Folder>\n");
                sb.append(b);
            }
            sb.append("</Document>\n");
            sb.append("</kml>\n");
            //            System.out.println(sb);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        System.exit(0);
    }






}

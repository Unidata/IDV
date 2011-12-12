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

package ucar.unidata.data.imagery;


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.*;

import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.KmlDataSource;



import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapefileAdapter;

import visad.*;

import visad.data.DefaultFamily;
//import visad.data.jai.JAIForm;
import visad.data.gif.GIFForm;

import visad.util.DataUtility;

import visad.util.ImageHelper;

import java.awt.*;
import java.awt.image.*;

import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;


import javax.swing.*;


/**
 * This is an implementation that will read in a generic data file
 * and return a single Data choice that is a VisAD Data object.
 */
public class ImageXmlDataSource extends FilesDataSource {

    /** For ximg files */
    public static final PatternFileFilter FILTER_XIMG =
        new PatternFileFilter(".+\\.ximg", "Image Xml files (*.ximg)");

    /** For ximg files */
    public static final String EXT_XIMG = ".ximg";


    /** xml tag name */
    public static final String TAG_COLLECTION = "collection";

    /** xml tag name */
    public static final String TAG_GROUP = "group";

    /** xml tag name */
    public static final String TAG_IMAGE = "image";

    /** xml tag name */
    public static final String TAG_BYTES = "ytes";

    /** xml tag name */
    public static final String TAG_SHAPE = "shape";

    /** xml attr name */
    public static final String ATTR_BASE = "base";

    /** xml attr name */
    public static final String ATTR_ID = "id";

    /** xml attr name */
    public static final String ATTR_FORMAT = "format";

    /** xml attr name */
    public static final String ATTR_BYTES = "bytes";

    /** xml attr name */
    public static final String ATTR_DATE = "date";

    /** xml attr name */
    public static final String ATTR_URL = "url";

    /** xml attr name */
    public static final String ATTR_NAME = "name";


    /** Holds the properties for image data choices */
    private Hashtable imageProps;

    /** Holds the properties for map data choices */
    private Hashtable mapProps;


    /** default family for reading data */
    DefaultFamily family = new DefaultFamily("VisADDataSource");

    /** _Mapping of url to element */
    Hashtable urlToElement = new Hashtable();

    /** Image data categories */
    private List imageCategories;

    /** Shape file data categories */
    private List shapeCategories;

    /** Does the ximg file contain the images directly in the xml */
    private boolean haveEmbeddedFiles = false;

    /** Maps id to xml node */
    private Hashtable nodeMap = new Hashtable();

    /** Holds the relative relativeFiles or urls */
    private List relativeFiles = new ArrayList();


    /**
     *  Parameterless ctor for xml encoding.
     */
    public ImageXmlDataSource() {}


    /**
     * Just pass through to the base class the ctor arguments.
     * @param descriptor    Describes this data source, has a label etc.
     * @param filename      This is the filename (or url) that
     *                      points to the actual data source.
     * @param properties General properties used in the base class
     *
     * idv    * @throws VisADException   problem getting the data
     */
    public ImageXmlDataSource(DataSourceDescriptor descriptor,
                              String filename, Hashtable properties)
            throws VisADException {
        super(descriptor, filename, "Image data source", properties);
    }

    /**
     * tmp
     *
     * @return tmp
     */
    public List getDataPaths() {
        List paths = new ArrayList();
        paths.add(getFilePath());
        paths.addAll(relativeFiles);
        return paths;
    }

    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        if (isFileBased()) {
            return false;
        }
        if (haveEmbeddedFiles) {
            return true;
        }
        return true;
    }


    /**
     * Save remote data to local disk.  This is a NOOP.
     *
     * @param prefix file dir and  prefix
     * @param loadId For JobManager
     * @param changeLinks Change internal references
     *
     * @return List of files
     *
     * @throws Exception On badness
     */
    protected List saveDataToLocalDisk(String prefix, Object loadId,
                                       boolean changeLinks)
            throws Exception {

        String ximgFile  = getFilePath();
        File   parentDir = new File(prefix).getParentFile();
        List newXimgFiles = IOUtil.writeTo(Misc.newList(ximgFile), prefix,
                                           "ximg");

        if (newXimgFiles == null) {
            return null;
        }

        List filesToMove = new ArrayList(relativeFiles);
        List newFiles    = new ArrayList(newXimgFiles);
        for (int i = 0; i < filesToMove.size(); i++) {
            String file = (String) filesToMove.get(i);
            String newFilePath = IOUtil.joinDir(parentDir.toString(),
                                     IOUtil.getFileTail(file));
            if (IOUtil.writeTo(IOUtil.getURL(file, getClass()),
                               new File(newFilePath), loadId) > 0) {
                newFiles.add(newFilePath);
            }
        }
        if ((newXimgFiles != null) && changeLinks) {
            setNewFiles(newXimgFiles);
        }
        return newFiles;

    }



    /**
     * Override the base class method to return the times for the data choice
     *
     * @param dataChoice  DataChoice in question
     * @return  List of all times for that choice
     */
    public List getAllDateTimes(DataChoice dataChoice) {
        Object id = dataChoice.getId();
        if ( !(id instanceof List)) {
            return super.getAllDateTimes(dataChoice);
        }
        List l     = (List) id;
        List times = new ArrayList();
        for (int i = 0; i < l.size(); i++) {
            ImageInfo ii = (ImageInfo) l.get(i);
            if (ii.getDate() != null) {
                times.add(ii.getDate());
            }
        }
        return times;
    }


    /**
     * Get the base of th image url
     *
     * @param node  the node
     *
     * @return  the base (or null)
     */
    private String getBase(Element node) {
        if (node == null) {
            return IOUtil.getFileRoot(getFilePath());
        }
        String base = XmlUtil.getAttribute(node, ATTR_BASE, (String) null);
        if (base != null) {
            return base;
        }
        Node parent = node.getParentNode();
        if ((parent != null) && (parent instanceof Element)) {
            return getBase((Element) parent);
        }
        return getBase(null);
    }



    /**
     * This method is called at initialization time and  should create
     * a set of {@link ucar.unidata.data.DirectDataChoice}-s  and add them
     * into the base class managed list of DataChoice-s with the method
     * addDataChoice.
     */
    protected void doMakeDataChoices() {
        String xmlFile = getFilePath();
        try {
            imageCategories = DataCategory.parseCategories("RGBIMAGE", false);
            shapeCategories = DataCategory.parseCategories("GIS-SHAPEFILE",
                    false);
            Element root = XmlUtil.getRoot(xmlFile, getClass());
            if(root == null) {
                throw new IllegalArgumentException("Could not find image xml file:" + xmlFile);
            }
            String dataSourceName = XmlUtil.getAttribute(root, ATTR_NAME,
                                        (String) null);
            if (dataSourceName != null) {
                setName(dataSourceName);
            }
            if (root.getTagName().equals(TAG_IMAGE)
                    || root.getTagName().equals(TAG_GROUP)) {
                processNode(root, null);
            } else {
                recurseXml(root, null);
            }
        } catch (Exception exc) {
            setInError(true);
            throw new WrapperException("Loading image xml file: " + xmlFile,
                                       exc);
        }
    }


    /**
     * Process the xml
     *
     * @param root xml node
     * @param cdc The parent data choice
     *
     * @throws Exception On badness
     */
    protected void recurseXml(Element root, CompositeDataChoice cdc)
            throws Exception {

        imageProps = Misc.newHashtable(DataChoice.PROP_ICON,
                                       "/auxdata/ui/icons/Earth16.gif");
        mapProps = Misc.newHashtable(DataChoice.PROP_ICON,
                                     "/auxdata/ui/icons/Map16.gif");
        NodeList elements = XmlUtil.getElements(root);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            processNode(child, cdc);
        }
    }


    /**
     * utility to get a url from xml
     *
     * @param child xml node
     *
     * @return the url
     */
    private String getUrl(Element child) {
        String base = getBase(child);
        String url  = XmlUtil.getAttribute(child, ATTR_URL, (String) null);
        if (url != null) {
            //TODO: There can be a problem here if the idv is running in the directory of the ximg file
            //and the image references are relative. 
            File fileUrl = new File(url);
            if (fileUrl.exists()) {
                if ( !fileUrl.isAbsolute()) {
                    url = base + "/" + url;
                }
            }
            if ( !fileUrl.exists()) {
                if ( !url.startsWith("http") && !url.startsWith("/")
                        && (base != null)) {
                    if ( !base.endsWith("/")) {
                        url = base + "/" + url;
                    } else {
                        url = base + url;
                    }
                }
            }
        }
        return url;
    }


    /**
     * Process the xml ximg node
     *
     * @param child The node
     * @param cdc The data choice to add to. May be null, 
     *            if so add top level data choice
     *
     * @throws Exception On badness_
     */
    private void processNode(Element child, CompositeDataChoice cdc)
            throws Exception {


        if (child.getTagName().equals(TAG_IMAGE)
                || child.getTagName().equals(TAG_GROUP)) {
            List   categories     = imageCategories;
            Object dataChoiceData = null;
            String name = XmlUtil.getAttribute(child, ATTR_NAME,
                              (String) null);
            if (child.getTagName().equals(TAG_IMAGE)) {
                String id = null;
                if (XmlUtil.hasAttribute(child, ATTR_URL)) {
                    String url = XmlUtil.getAttribute(child, ATTR_URL);
                    if (name == null) {
                        name = url;
                    }
                    id = url;
                    String fullUrl = getUrl(child);
                    //If its relative then add it into the files list
                    if ( !Misc.equals(fullUrl, url)
                            || new File(url).exists()) {
                        //System.err.println ("adding relative:" + fullUrl);
                        relativeFiles.add(fullUrl);
                        //System.err.println ("after:" + relativeFiles);
                    }
                } else if (XmlUtil.hasAttribute(child, ATTR_ID)) {
                    haveEmbeddedFiles = true;
                    id                = XmlUtil.getAttribute(child, ATTR_ID);
                } else {
                    haveEmbeddedFiles = true;
                    id                = "id" + nodeMap.size();
                }
                nodeMap.put(id, child);
                dataChoiceData = new ImageInfo(id, child, null, null);
            } else if (child.getTagName().equals(TAG_GROUP)) {
                String dateFormat = XmlUtil.getAttribute(child, ATTR_FORMAT,
                                        "yyyyMMddHH Z");
                List     imageInfos   = new ArrayList();
                NodeList elements     = XmlUtil.getElements(child);
                boolean  haveDate     = false;
                boolean  dontHaveDate = false;
                for (int i = 0; i < elements.getLength(); i++) {
                    Element imageChild = (Element) elements.item(i);
                    String  url        = getUrl(imageChild);
                    if (name == null) {
                        name = url;
                    }
                    String time = XmlUtil.getAttribute(imageChild, ATTR_DATE,
                                      (String) null);
                    try {
                        DateTime dttm = ((time == null)
                                         ? null
                                         : DateTime.createDateTime(time,
                                             dateFormat));
                        if (dttm == null) {
                            dontHaveDate = true;
                        } else {
                            haveDate = true;
                        }
                        if (imageChild.getTagName().equals(TAG_IMAGE)) {
                            imageInfos.add(new ImageInfo(url, imageChild,
                                    child, dttm));
                        } else if (imageChild.getTagName().equals(
                                TAG_SHAPE)) {
                            imageInfos.add(new ImageInfo(url, dttm, true));
                            categories = shapeCategories;
                        }
                    } catch (Exception exc) {
                        throw new WrapperException("Processing ximg file",
                                exc);
                    }
                }
                dataChoiceData = imageInfos;
                if (dontHaveDate && haveDate) {
                    throw new IllegalArgumentException(
                        "Some of the elements have a date field, some don't");
                }

            } else {
                throw new IllegalArgumentException("Unknown ximg tag:"
                        + child.getTagName());
            }
            DirectDataChoice ddc = new DirectDataChoice(this, dataChoiceData,
                                       name, name, categories, imageProps);
            if (cdc != null) {
                cdc.addDataChoice(ddc);
            } else {
                addDataChoice(ddc);
            }
        } else if (child.getTagName().equals(TAG_SHAPE)) {
            String url  = XmlUtil.getAttribute(child, ATTR_URL);
            String base = getBase(child);
            if ( !url.startsWith("http") && (base != null)) {
                url = base + url;
            }
            //TODO: Handle shapefiles in makeDataLocal/zidv bundles
            //            files.add(url);
            String name = XmlUtil.getAttribute(child, ATTR_NAME, url);
            DirectDataChoice ddc = new DirectDataChoice(this,
                                       new ImageInfo(url, true), name, name,
                                       shapeCategories, mapProps);
            if (cdc != null) {
                cdc.addDataChoice(ddc);
            } else {
                addDataChoice(ddc);
            }

        } else if (child.getTagName().equals(TAG_COLLECTION)) {
            String name = XmlUtil.getAttribute(child, ATTR_NAME);
            CompositeDataChoice newCdc = new CompositeDataChoice(this, "",
                                             name, name, null);
            if (cdc != null) {
                cdc.addDataChoice(newCdc);
            } else {
                addDataChoice(newCdc);
            }
            recurseXml(child, newCdc);
        }

    }

    /**
     * This method should create and return the visad.Data that is
     * identified by the given {@link ucar.unidata.data.DataChoice}.
     *
     * @param dataChoice     This is one of the DataChoice-s that was created
     *                       in the doMakeDataChoices call above.
     * @param category       The specific {@link ucar.unidata.data.DataCategory}
     *                       which the {@link ucar.unidata.idv.DisplayControl}
     *                       was instantiated with. Usually can be ignored.
     * @param dataSelection  This may contain a list of times which
     *                       subsets the request.
     * @param requestProperties  extra request properties
     * @return The {@link visad.Data} object represented by the given dataChoice
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        List selectedTimes = getTimesFromDataSelection(dataSelection,
                                 dataChoice);

        Hashtable okTimes = null;
        for (int i = 0; i < selectedTimes.size(); i++) {
            if (okTimes == null) {
                okTimes = new Hashtable();
            }
            okTimes.put(selectedTimes.get(i), "");
        }
        //      System.err.println ("times:" + selectedTimes);
        Object  stuff = dataChoice.getId();
        List    list;
        boolean justOne = false;
        if (stuff instanceof ImageInfo) {
            list    = Misc.newList(stuff);
            justOne = true;
        } else {
            list = (List) stuff;
        }
        if (list.size() == 0) {
            return null;
        }

        List    dataList      = new ArrayList();
        List    dateTimesList = new ArrayList();


        boolean usingIndex    = false;
        for (int i = 0; i < list.size(); i++) {
            ImageInfo ii = (ImageInfo) list.get(i);
            Data      d  = null;
            if ((okTimes != null) && (ii.getDate() != null)
                    && (okTimes.get(ii.getDate()) == null)) {
                continue;
            }
            InputStream is   = null;
            String      id   = ii.getUrl();
            String      url  = ii.getUrl();
            Element     node = (Element) nodeMap.get(id);
            try {
                if (node != null) {
                    url = getUrl(node);
                }
                if ((node == null) || XmlUtil.hasAttribute(node, ATTR_URL)) {
                    is = IOUtil.getInputStream(url, getClass());
                    //is = IOUtil.getInputStream(id, getClass());
                } else {
                    String byteString = XmlUtil.getChildText(node);
                    if (byteString == null) {
                        throw new IllegalStateException(
                            "Unable to read data bytes");
                    }
                    byteString = byteString.trim();
                    is = new ByteArrayInputStream(
                        XmlUtil.decodeBase64(byteString));
                }
            } catch (Exception exc) {
                throw new WrapperException("Error reading file: "
                                           + ii.getUrl(), exc);
            }


            if (ii.getIsShape()) {
                try {
                    ShapefileAdapter sfa = new ShapefileAdapter(is);
                    d = sfa.getData();
                } catch (Exception exc) {
                    throw new WrapperException("Error reading shapefile: "
                            + ii.getUrl(), exc);
                }
            } else {
                try {
                    byte[] imageContent = IOUtil.readBytes(is);
                    Image image =
                        Toolkit.getDefaultToolkit().createImage(imageContent);
                    ImageHelper ih = new ImageHelper();
                    image.getWidth(ih);
                    if (ih.badImage) {
                        throw new IllegalStateException("Bad image: "
                                + ii.getUrl());
                    }
                    //                    GIFForm form = new GIFForm();
                    //FlatField   ff     = (FlatField) family.open(ii.getUrl());
                    //                    FlatField   ff = (FlatField) DataUtility.makeField(image,true);
                    FlatField ff =
                        (FlatField) ucar.visad.Util.makeField(image, true);
                    Linear2DSet domain = (Linear2DSet) ff.getDomainSet();
                    SampledSet  newDomain;
                    int         width  = domain.getX().getLength();
                    int         height = domain.getY().getLength();
                    int         pixels = width * height;

                    double      ulLat  = ii.getUlLat();
                    double      ulLon  = ii.getUlLon();
                    double      ulAlt  = ii.getUlAlt();

                    double      urLat  = ii.getUrLat();
                    double      urLon  = ii.getUrLon();
                    double      urAlt  = ii.getUrAlt();

                    double      llLat  = ii.getLlLat();
                    double      llLon  = ii.getLlLon();
                    double      llAlt  = ii.getLlAlt();

                    double      lrLat  = ii.getLrLat();
                    double      lrLon  = ii.getLrLon();
                    double      lrAlt  = ii.getLrAlt();


                    //                    System.err.println("Rect:" + ii.isRectilinear());

                    boolean hasAltitude = ii.hasAltitude();
                    if ( !hasAltitude && ii.isRectilinear()) {
                        newDomain = new Linear2DSet(
                            RealTupleType.SpatialEarth2DTuple, ulLon, lrLon,
                            width, ulLat, lrLat, height);
                    } else {
                        float[][] values = (hasAltitude
                                            ? new float[3][pixels]
                                            : new float[2][pixels]);
                        int       pixel  = 0;
                        double
                            alt1         = 0,
                            alt2         = 0;
                        for (int row = 0; row < height; row++) {
                            double rowPercent = row / (double) height;
                            double lat1 = ulLat
                                          + (llLat - ulLat) * rowPercent;
                            double lat2 = urLat
                                          + (lrLat - urLat) * rowPercent;

                            double lon1 = ulLon
                                          + (llLon - ulLon) * rowPercent;
                            double lon2 = urLon
                                          + (lrLon - urLon) * rowPercent;

                            if (hasAltitude) {
                                alt1 = ulAlt + (llAlt - ulAlt) * rowPercent;
                                alt2 = urAlt + (lrAlt - urAlt) * rowPercent;
                            }

                            for (int col = 0; col < width; col++) {
                                double colPercent = col / (double) width;
                                double lat = lat1
                                             + (lat2 - lat1) * colPercent;
                                double lon = lon1
                                             + (lon2 - lon1) * colPercent;
                                values[0][pixel] = (float) lat;
                                values[1][pixel] = (float) lon;
                                if (hasAltitude) {
                                    values[2][pixel] = (float) (alt1
                                            + (alt2 - alt1) * colPercent);
                                }
                                pixel++;
                            }
                        }

                        if (hasAltitude) {
                            newDomain = new Gridded3DSet(
                                RealTupleType.LatitudeLongitudeAltitude,
                                values, width, height, null, null, null,
                                false);
                        } else {
                            newDomain = new Gridded2DSet(
                                RealTupleType.LatitudeLongitudeTuple, values,
                                width, height, null, null, null, false);
                        }

                    }
                    d = GridUtil.setSpatialDomain(ff, newDomain);

                } catch (Exception exc) {
                    exc.printStackTrace();
                    throw new BadDataException("Error reading image file: "
                            + ii.getUrl() + "\n" + exc);
                }
            }
            if (justOne) {
                return d;
            }
            //System.err.println("date :" + ii.getDate());
            if (ii.getDate() != null) {
                dateTimesList.add(ii.getDate());
            } else {
                usingIndex = true;
            }

            dataList.add(d);
        }


        Data[] dataArray = new Data[dataList.size()];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = (Data) dataList.get(i);
        }

        FieldImpl  data        = null;
        RealType   domainType  = null;
        int[]      timeIndices = null;
        SampledSet domain      = null;
        for (int i = 0; i < dataArray.length; i++) {
            if (data == null) {
                if (usingIndex) {
                    domain =
                        new Integer1DSet(domainType =
                            RealType.getRealType("index"), dataArray.length);
                } else {
                    DateTime[] timesArray =
                        new DateTime[dateTimesList.size()];
                    double[] timeVals = new double[dateTimesList.size()];
                    for (int timeIdx = 0; timeIdx < timesArray.length;
                            timeIdx++) {
                        DateTime dt = (DateTime) dateTimesList.get(timeIdx);
                        timesArray[timeIdx] = dt;
                        timeVals[timeIdx] =
                            dt.getValue(CommonUnit.secondsSinceTheEpoch);
                    }
                    domain = (timesArray.length == 1)
                             ? (SampledSet) new SingletonSet(
                                 new RealTuple(new Real[] { timesArray[0] }))
                             : (SampledSet) DateTime.makeTimeSet(timesArray);
                    domainType  = RealType.Time;
                    timeIndices = domain.doubleToIndex(new double[][] {
                        timeVals
                    });
                }
                data = new FieldImpl(new FunctionType(domainType,
                        dataArray[i].getType()), domain);
            }
            int index = (usingIndex || (dataArray.length == 1))
                        ? i
                        : timeIndices[i];
            data.setSample(index, dataArray[i], false);
        }
        return data;

    }



    /**
     * You can also override the base class method to return the list
     * of all date/times that this DataSource holds.
     * @return This should be an List of {@link visad.DateTime} objects.
     */
    protected List doMakeDateTimes() {
        // don't know for this, so return empty ArrayList
        return new ArrayList();
    }


    /**
     * Set the XmlFile property.
     *
     * @param value The new value for XmlFile
     */
    public void setXmlFile(String value) {
        sources = Misc.newList(value);
    }



    /**
     * A utility method that writes out the given image bytes and the ximg file
     *
     * @param filename Filename to write to. Will use this to write the image.
     * @param bounds The lat/lon bounds
     * @param bytes The image bytes
     * @param format Image format
     *
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */
    public static void writeToFile(String filename, GeoLocationInfo bounds,
                                   byte[] bytes, String format)
            throws FileNotFoundException, IOException {
        format = StringUtil.replace(format, "image/", "");
        String imageFileName = IOUtil.stripExtension(filename) + "." + format;
        if (KmlDataSource.isKmlFile(filename)) {
            IOUtil.writeBytes(new File(imageFileName), bytes);
            KmlDataSource.writeToFile(filename, bounds, imageFileName);
            return;
        }

        StringBuffer sb = new StringBuffer();
        String nav = XmlUtil.attrs(ImageInfo.ATTR_ULLAT,
                                   "" + bounds.getMaxLat(),
                                   ImageInfo.ATTR_LRLAT,
                                   "" + bounds.getMinLat());
        nav = nav
              + XmlUtil.attrs(ImageInfo.ATTR_ULLON, "" + bounds.getMinLon(),
                              ImageInfo.ATTR_LRLON, "" + bounds.getMaxLon());

        IOUtil.writeBytes(new File(imageFileName), bytes);

        sb.append(
            XmlUtil.openTag(
                TAG_IMAGE,
                nav
                + XmlUtil.attr(
                    ATTR_NAME,
                    IOUtil.stripExtension(IOUtil.getFileTail(filename))
        //,ATTR_URL, IOUtil.getFileTail(imageFileName)
        )));
        XmlUtil.appendCdataBytes(sb, bytes);
        sb.append(XmlUtil.closeTag(TAG_IMAGE));
        IOUtil.writeFile(filename, sb.toString());
    }


    /**
     * A utility method that writes out the given image bytes and the ximg file
     *
     * @param filename Filename to write to. Will use this to write the image.
     * @param bounds The lat/lon bounds
     * @param imageFileName The image file this represents
     *
     * @throws FileNotFoundException On badness
     * @throws IOException On badness
     */
    public static void writeToFile(String filename, GeoLocationInfo bounds,
                                   String imageFileName)
            throws FileNotFoundException, IOException {
        StringBuffer sb = new StringBuffer();
        String nav = XmlUtil.attrs(ImageInfo.ATTR_ULLAT,
                                   "" + bounds.getMaxLat(),
                                   ImageInfo.ATTR_LRLAT,
                                   "" + bounds.getMinLat());
        nav = nav
              + XmlUtil.attrs(ImageInfo.ATTR_ULLON, "" + bounds.getMinLon(),
                              ImageInfo.ATTR_LRLON, "" + bounds.getMaxLon());
        sb.append(
            XmlUtil.tag(
                TAG_IMAGE,
                nav
                + XmlUtil.attrs(
                    ATTR_NAME,
                    IOUtil.stripExtension(IOUtil.getFileTail(filename)),
                    ATTR_URL, imageFileName)));
        IOUtil.writeFile(filename, sb.toString());
    }





}

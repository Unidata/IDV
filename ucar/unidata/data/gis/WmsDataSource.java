/*
 * $Id: WmsDataSource.java,v 1.33 2007/05/04 22:23:20 dmurray Exp $
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


import org.w3c.dom.Element;

import ucar.unidata.data.*;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.data.imagery.ImageXmlDataSource;

import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;


import visad.*;

import visad.util.DataUtility;
import visad.util.ImageHelper;

import java.awt.*;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * DataSource for Web Map Servers
 *
 * @author IDV development team
 * @version $Revision: 1.33 $ $Date: 2007/05/04 22:23:20 $
 */
public class WmsDataSource extends DataSourceImpl {


    /** request property */
    public static final String PROP_BOUNDS = "prop.wms.bounds";

    /** request property */
    public static final String PROP_ICONPATH = "prop.wms.iconpath";

    /** request property */
    public static final String PROP_WRITEFILE = "prop.wms.writefile";


    /** request property */
    public static final String PROP_RESOLUTION = "prop.wms.resolution";

    /** request property */
    public static final String PROP_IMAGEWIDTH = "prop.wms.imagewidth";

    /** request property */
    public static final String PROP_IMAGEHEIGHT = "prop.wms.imageheight";


    /** request property */
    public static final String PROP_LAYER = "prop.wms.layer";

    /** request property */
    public static final String PROP_LAYERS = "prop.wms.layers";

    /** categories */
    private List categories = DataCategory.parseCategories("GIS-WMS", false);


    /** This allows us to abort concurrent reads */
    private Object loadId;


    /** List of selections (layers) */
    private List wmsSelections;


    /**
     * The bytes of the last image we loaded. Keep this around for resonding easily to the
     * write image request
     */
    byte[] lastImageContent;

    /** The url of the last image */
    String lastUrl;


    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public WmsDataSource() {}


    /**
     * Create a WmsDataSource from the specification given.
     *
     * @param descriptor          descriptor for the data source
     * @param selections The selections
     * @param properties          extra properties
     *
     * @throws VisADException     some problem occurred creating data
     */
    public WmsDataSource(DataSourceDescriptor descriptor, List selections,
                         Hashtable properties)
            throws VisADException {
        super(descriptor, "WMS data source", "WMS data source", properties);
        this.wmsSelections = new ArrayList(selections);
        initWmsDataSource();
    }

    /**
     * Initialize after we have been unpersisted
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        initWmsDataSource();
    }

    /**
     * Initialization method
     */
    private void initWmsDataSource() {}


    /**
     * Create the data choices associated with this source.
     */
    protected void doMakeDataChoices() {
        List layerList = new ArrayList();
        for (int i = 0; i < wmsSelections.size(); i++) {
            WmsSelection selection = (WmsSelection) wmsSelections.get(i);
            layerList.add(new TwoFacedObject(selection.getTitle(),
                                             selection.getLayer()));
        }
        for (int i = 0; i < wmsSelections.size(); i++) {
            Hashtable properties = new Hashtable();
            properties.put(PROP_LAYERS, layerList);
            properties.put(PROP_LAYER, layerList.get(i));
            properties.put(DataChoice.PROP_ICON,
                           "/auxdata/ui/icons/Earth16.gif");
            WmsSelection selection = (WmsSelection) wmsSelections.get(i);

            addDataChoice(new DirectDataChoice(this, selection,
                    selection.toString(), selection.toString(), categories,
                    properties));
        }
    }


    /** A local cache */
    List cachedData = new ArrayList();

    /** The urls */
    List cachedUrls = new ArrayList();

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

        loadId = JobManager.getManager().stopAndRestart(loadId, "WMSControl");
        Object myLoadId = loadId;


        if (requestProperties == null) {
            requestProperties = new Hashtable();
        }
        WmsSelection wmsInfo = (WmsSelection) dataChoice.getId();

        //Look if there was a layer that overrides the one in the data choice
        Object tfoLayer = requestProperties.get(PROP_LAYER);
        if ((tfoLayer != null) && (tfoLayer instanceof TwoFacedObject)) {
            String layer = ((TwoFacedObject) tfoLayer).getId().toString();
            for (int i = 0; i < wmsSelections.size(); i++) {
                WmsSelection tmpSelection =
                    (WmsSelection) wmsSelections.get(i);
                if (Misc.equals(tmpSelection.getLayer(), layer)) {
                    wmsInfo = tmpSelection;
                    break;
                }
            }
        }


        //      LogUtil.message("Data: " + toStringTruncated() + ": "
        //                      + wmsInfo);
        String writeFile = (String) requestProperties.get(PROP_WRITEFILE);
        GeoLocationInfo boundsToUse =
            (GeoLocationInfo) requestProperties.get(PROP_BOUNDS);

        int imageWidth = Misc.getProperty(requestProperties, PROP_IMAGEWIDTH,
                                          800);
        int imageHeight = Misc.getProperty(requestProperties,
                                           PROP_IMAGEHEIGHT, -1);
        double resolution = Misc.getProperty(requestProperties,
                                             PROP_RESOLUTION, (float) 1.0);


        if (wmsInfo.getLegendIcon() != null) {
            requestProperties.put(PROP_ICONPATH, wmsInfo.getLegendIcon());
        }
        if (boundsToUse == null) {
            boundsToUse = wmsInfo.getBounds();
        } else {
            boundsToUse.rectify(wmsInfo.getBounds(), 0.0);
            boundsToUse.snapToGrid();
            boundsToUse.rectify(wmsInfo.getBounds(), 0.0);
        }


        double widthDegrees = boundsToUse.getMaxLon()
                              - boundsToUse.getMinLon();
        double heightDegrees = boundsToUse.getMaxLat()
                               - boundsToUse.getMinLat();

        if (wmsInfo.getFixedWidth() > -1) {
            imageWidth = wmsInfo.getFixedWidth();
        }
        if (imageHeight < 0) {
            if (wmsInfo.getFixedHeight() > -1) {
                imageHeight = wmsInfo.getFixedHeight();
            } else {
                imageHeight = Math.abs((int) (imageWidth
                        * boundsToUse.getDegreesY()
                        / boundsToUse.getDegreesX()));
            }
        }

        imageWidth  = Math.min(Math.max(imageWidth, 50), 2056);
        imageHeight = Math.min(Math.max(imageHeight, 50), 2056);

        imageWidth  = 600;
        imageHeight = 600;


        double diff = Math.abs(boundsToUse.getMinLon()
                               - boundsToUse.getMaxLon());
        String url = wmsInfo.assembleRequest(boundsToUse,
                                             (int) (imageWidth / resolution),
                                             (int) (imageHeight
                                                 / resolution));

        Image  image      = null;
        String cacheGroup = "WMS";
        synchronized (cachedUrls) {
            if (writeFile == null) {
                for (int i = 0; i < cachedUrls.size(); i++) {
                    if (url.equals(cachedUrls.get(i))) {
                        image = (Image) cachedData.get(i);
                        break;
                    }
                }
            }
        }



        byte[]    imageContent = null;
        FieldImpl xyData       = null;
        try {
            if (image == null) {
                if (Misc.equals(url, lastUrl) && (lastImageContent != null)) {
                    imageContent = lastImageContent;
                } else {}

                if (imageContent == null) {
                    long t1 = System.currentTimeMillis();
                    //                    System.err.println("getting image:" + url);
                    LogUtil.message("Reading WMS image: " + wmsInfo);
                    //System.err.println ("url:" + url);

                    InputStream is = IOUtil.getInputStream(url);
                    long        t2 = System.currentTimeMillis();
                    imageContent = IOUtil.readBytes(is, myLoadId);
                    long t3 = System.currentTimeMillis();
                    LogUtil.message("");
                    //                    System.err.println("Done");
                }
                //If it is null then there is another thread that is doing
                //a subsequent read
                lastImageContent = null;
                if (imageContent == null) {
                    return null;
                }
                Trace.call2("Getting image");
                Trace.call1("Making image");
                image = Toolkit.getDefaultToolkit().createImage(imageContent);
                Trace.call2("Making image");
                lastImageContent = imageContent;
                lastUrl          = url;
                updateDetailsText();
                if ( !JobManager.getManager().canContinue(myLoadId)) {
                    Trace.call2("WMSControl.loadImage");
                    return null;
                }
                synchronized (cachedUrls) {
                    if (cachedUrls.size() > 5) {
                        cachedUrls.remove(cachedUrls.size() - 1);
                        cachedData.remove(cachedData.size() - 1);
                    }
                    //For now don't cache
                    //      cachedUrls.add(0, url);
                    //                    cachedData.add(0, image);
                }
            }
            ImageHelper ih    = new ImageHelper();
            int         width = image.getWidth(ih);
            if (ih.badImage) {
                throw new IllegalStateException();
            }
            long tt1 = System.currentTimeMillis();
            xyData = DataUtility.makeField(image);
            long tt2 = System.currentTimeMillis();
            //      System.err.println("time to make field:" + (tt2-tt1));
        } catch (Exception iexc) {
            if (imageContent != null) {
                String msg = new String(imageContent);
                //  System.err.println ("msg:" + msg);
                /* Check to see if this is of the form:

                <?xml version='1.0' encoding="UTF-8" standalone="no" ?>
                <!DOCTYPE ServiceExceptionReport SYSTEM "http://www.digitalearth.gov/wmt/xml/exception_1_1_0.dtd ">
                <ServiceExceptionReport version="1.1.0">
                 <ServiceException>
                   Service denied due to system overload. Please try again later.
                 </ServiceException>
                </ServiceExceptionReport>

                */
                if (msg.indexOf("<ServiceExceptionReport") >= 0) {
                    try {
                        StringBuffer errors = new StringBuffer();
                        errors.append("\n");
                        Element root = XmlUtil.getRoot(msg);
                        List children = XmlUtil.findChildren(root,
                                            "ServiceException");
                        for (int i = 0; i < children.size(); i++) {

                            Element node = (Element) children.get(i);
                            String code = XmlUtil.getAttribute(node, "code",
                                              (String) null);
                            String body = XmlUtil.getChildText(node);
                            if (code != null) {
                                errors.append(code + "\n");
                            }
                            errors.append(body.trim() + "\n");
                        }
                        LogUtil.userErrorMessage(
                            "Error accessing image with the url:\n" + url
                            + "\nError:\n" + errors);
                    } catch (Exception exc) {
                        LogUtil.userErrorMessage(
                            "Error accessing image with the url:\n" + url
                            + "\nError:\n" + StringUtil.stripTags(msg));
                    }
                    return null;
                }


                msg = StringUtil.replace(msg, "\n", " ").toLowerCase();
                if (StringUtil.stringMatch(msg, "service\\s*exception")) {
                    if (StringUtil.stringMatch(msg,
                            "cannot\\s*be\\s*less\\s*than")) {
                        return null;
                    }
                }
                if (msg.indexOf("error") >= 0) {
                    LogUtil.userErrorMessage(
                        "There was an error accessing the image with the url:\n"
                        + url + "\nError:\n" + new String(imageContent));
                    return null;
                }
            }
            logException(
                "There was an error accessing the image with the url:\n"
                + url, iexc);
            return null;
        }

        if (writeFile != null) {
            try {
                ImageXmlDataSource.writeToFile(writeFile, boundsToUse,
                        imageContent, wmsInfo.getFormat());
            } catch (Exception exc) {
                throw new IllegalArgumentException(
                    "Error writing image xml file:" + writeFile + " " + exc);
            }
        }



        Linear2DSet domain = (Linear2DSet) xyData.getDomainSet();
        Linear2DSet imageDomain =
            new Linear2DSet(RealTupleType.SpatialEarth2DTuple,
                            boundsToUse.getMinLon(), boundsToUse.getMaxLon(),
                            domain.getX().getLength(),
                            boundsToUse.getMaxLat(), boundsToUse.getMinLat(),
                            domain.getY().getLength());

        /*
        new Linear2DSet(RealTupleType.SpatialEarth2DTuple,
                            boundsToUse.getMinLon(), boundsToUse.getMaxLon(),
                            domain.getX().getLength(),
                            boundsToUse.getMinLat() +diff, boundsToUse.getMinLat(),
                            domain.getY().getLength());*/


        FieldImpl field = GridUtil.setSpatialDomain(xyData, imageDomain,
                              true);

        return field;

    }



    /**
     * Get the description. This adds on the last url requested.
     *
     * @return description
     */
    public String getFullDescription() {
        String desc = super.getFullDescription();
        if (lastUrl != null) {
            desc = desc + "<p><b>Last request:</b> " + lastUrl;
        }
        return desc;
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
     * Set the WmsSelections property.
     *
     * @param value The new value for WmsSelections
     */
    public void setWmsSelections(List value) {
        wmsSelections = value;
    }

    /**
     * Get the WmsSelections property.
     *
     * @return The WmsSelections
     */
    public List getWmsSelections() {
        return wmsSelections;
    }



    /**
     * overwrite
     *
     * @param o object
     *
     * @return equals
     */
    public boolean equals(Object o) {
        if ( !super.equals(o)) {
            return false;
        }
        WmsDataSource that = (WmsDataSource) o;
        return Misc.equals(this.wmsSelections, that.wmsSelections);
    }



}


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

package ucar.unidata.idv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.KmlDataSource;

import ucar.unidata.data.gis.WmsDataSource;
import ucar.unidata.data.gis.WmsSelection;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.imagery.ImageXmlDataSource;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.IdvResourceManager;

import ucar.unidata.util.CacheManager;

import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;

import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.GlobeDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;


import ucar.visad.GeoUtils;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.ImageRGBDisplayable;


import visad.*;
import visad.RealType;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import visad.georef.MapProjection;

import visad.util.BaseRGBMap;



import visad.util.ColorPreview;

import visad.util.DataUtility;

import java.awt.*;

import java.awt.Color;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;

import java.io.File;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;




/**
 * Class for controlling the display of color images.
 * @author Jeff McWhirter
 */
public class WMSControl extends ImageControl implements ImageObserver {

    /** Are we shown in te globe view */
    private boolean inGlobe = false;

    /** This allows us to abort concurrent reads */
    private Object loadId;

    /** listen for clicks */
    private boolean enableAutoFetch = true;

    /** A virtual timestamp for aborting image get calls */
    private int timestamp = 0;


    /** When created by a data source we keep this around */
    private DataChoice theDataChoice;

    /** The layer */
    private Object theLayer = null;


    /** This is the last image data */
    private FieldImpl imageData;

    /** The current info to use */
    private WmsSelection wmsInfo;

    /** All possible infos */
    private List wmsSelections;

    /** Image resolution */
    private double resolution = 1.0;

    /** Our current bounds */
    private GeoLocationInfo currentBounds;

    /** First event when dragging */
    private DisplayEvent dragStartEvent;

    /** Image width */
    private int imageWidth = 800;

    /** Image height */
    private int imageHeight = 600;

    /** Gui element */
    private JCheckBox reprojectCbx;

    /** Gui element */
    private JCheckBox clickCbx;

    /** A cache of data */
    private List cachedUrls = new ArrayList();

    /** A cache of data */
    private List cachedData = new ArrayList();

    /** A cache of data */
    private static Hashtable<String, FieldImpl> fixedImageCache =
        new Hashtable<String, FieldImpl>();


    /** The lable that shows the legend icon */
    private JLabel legendIconLbl;

    /** This is the current legend icon url */
    private String legendIconUrl;

    /** This is the (possibly null) image that is the legend icon for the layer we are currently displaying */
    private Image legendImage;

    /** Not used yet. */
    private String selectedTime;

    /** List of times in image. Not used yet */
    private JComboBox timeBox;

    /** Use temporarily when writing the image xml file */
    private String writeFile;

    /** The size scale */
    private double scale = 1.0;

    /** scale property widget */
    private JSlider scaleSlider;

    /**
     * Default constructor.
     */
    public WMSControl() {}


    /**
     * Constructor.
     *
     * @param wmsSelections The list of wms infos
     * @param title The title
     */
    public WMSControl(List wmsSelections, String title) {
        setWmsInfos(wmsSelections);
        setDisplayName(title);
    }


    /**
     * Called to make this kind of Display Control;
     * This method is called from inside DisplayControlImpl init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        super.init(dataChoice);
        inGlobe = inGlobeDisplay();
        inGlobe = false;

        if (dataChoice != null) {
            theDataChoice = dataChoice;
            if (theLayer == null) {
                theLayer =
                    theDataChoice.getProperty(WmsDataSource.PROP_LAYER);
            }
            wmsSelections = new ArrayList();
            //            wmsInfo = (WmsSelection)dataChoice.getId();
            //            wmsSelections.add(wmsInfo);
            return true;
        }


        if ((wmsSelections == null) || (wmsSelections.size() == 0)) {
            List defaultInfos =
                WmsSelection.parseWmsResources(
                    getControlContext().getResourceManager().getXmlResources(
                        IdvResourceManager.RSC_BACKGROUNDWMS));
            setWmsInfos(defaultInfos);
        }


        return true;
    }

    /** Do we have a load pending */
    private boolean waitingToLoad = false;


    /**
     * initdone
     */
    public void initDone() {
        if (currentBounds == null) {
            loadImageFromScreen();
        } else {
            loadImage();
        }
        super.initDone();
    }

    /**
     * have we fully initialized
     *
     * @return have we fully initialized
     */
    public boolean isInitDone() {
        if ( !super.isInitDone()) {
            return false;
        }
        return !waitingToLoad;
    }



    /**
     * make the data instance
     *
     * @param dataChoice the data choice
     *
     * @return The data instance
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        return new GridDataInstance(dataChoice, getDataSelection(),
                                    getRequestProperties(), EMPTY_RGB_IMAGE);

    }


    /**
     * Signal base class to add this as a display listener
     *
     * @return Add as display listener
     */
    protected boolean shouldAddDisplayListener() {
        return true;
    }

    /**
     * Signal base class to add this as a projection control listener
     *
     * @return Add as display listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }

    /**
     * Is the layer fixed?
     *
     * @return true if fixed
     */
    private boolean isLayerFixed() {
        if (wmsInfo != null) {
            return wmsInfo.isFixedImage();
        }


        if ((theLayer != null) && (theLayer instanceof TwoFacedObject)) {
            String layer = ((TwoFacedObject) theLayer).getId().toString();
            //A hack but we gotta work with what we got
            if (layer.indexOf("fixed") >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get initial fast rendering option
     *
     * @return false
     */
    protected boolean getInitialFastRendering() {
        return false;
    }

    /**
     * Method to call if projection changes.  Subclasses that
     * are worried about such events should implement this.
     */
    public void projectionChanged() {
        super.projectionChanged();
        if (isLayerFixed()) {
            return;
        }
        if ( !inGlobe && getEnableAutoFetch()) {
            loadImageFromScreen();
        }
    }


    /**
     * Noop for the ControlListener interface
     */
    public void viewpointChanged() {
        super.viewpointChanged();
        if (isLayerFixed()) {
            return;
        }
        if ( !inGlobe && getEnableAutoFetch()) {
            loadImageFromScreen();
        }
    }




    /**
     * Get the contents of the details html
     *
     * @return The contents of the details
     */
    protected String getDetailsContents() {
        if (wmsInfo == null) {
            return super.getDetailsContents();
        }
        StringBuffer sb   = new StringBuffer(super.getDetailsContents());
        String       desc = wmsInfo.getDescription();
        if (desc != null) {
            sb.append("<p>Abstract:<br>\n");
            sb.append(StringUtil.breakText(desc, "<br>", 50));
        }
        return sb.toString();
    }




    /**
     * Listen for DisplayEvents
     *
     * @param event The event
     */
    public void handleDisplayChanged(DisplayEvent event) {
        InputEvent inputEvent = event.getInputEvent();
        try {
            int id = event.getId();
            //Don't do this for now
            if (true) {
                return;
            }

            if ( !getDisplayVisibility()) {
                return;
            }
            if (id == DisplayEvent.MOUSE_DRAGGED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }
                if (dragStartEvent == null) {
                    dragStartEvent = event;
                }
                return;
            }
            if (id == DisplayEvent.MOUSE_RELEASED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }
            }
        } catch (Exception e) {
            logException("Handling display event changed", e);
        }
    }



    /**
     * Go the a street address
     */
    public void goToAddress() {
        //Do it in a thread
        Misc.run(new Runnable() {
            public void run() {
                goToAddressInner();
            }
        });
    }


    /**
     * Go the a street address
     */
    private void goToAddressInner() {
        try {
            showWaitCursor();
            LatLonPoint llp = GeoUtils.getLocationOfAddress();
            showNormalCursor();
            if (llp == null) {
                return;
            }
            getNavigatedDisplay().center(GeoUtils.toEarthLocation(llp));
            loadImageFromScreen();
        } catch (Exception e) {
            showNormalCursor();
            logException("Error going to address", e);
        }

    }


    /**
     * Overwrite base class method to do our own style of reload
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void reloadDataSource() throws RemoteException, VisADException {
        loadImageFromScreen();
    }


    /**
     * Add to view menu
     *
     * @param items List of ites
     * @param forMenuBar for the menu bar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.makeMenuItem("Reload Image", this,
                                        "loadImageFromScreen"));
        items.add(GuiUtils.makeMenuItem("Reproject to Image", this,
                                        "reproject"));

        items.add(GuiUtils.makeMenuItem("Go to Address", this,
                                        "goToAddress"));

        items.add(GuiUtils.MENU_SEPARATOR);
        super.getViewMenuItems(items, forMenuBar);
    }


    /**
     * Make a GeoLocationInfo from the rect
     *
     * @param rect The rect
     *
     * @return  The GeoLocationInfo
     */
    private GeoLocationInfo makeGeoLocationInfo(Rectangle2D.Double rect) {
        GeoLocationInfo gli = null;

        if (wmsInfo == null) {
            if (inGlobe) {
                return null;
            } else {
                double minLon = rect.x;
                double maxLon = rect.x + rect.width;
                double minLat = rect.y;
                double maxLat = rect.y + rect.height;
                return new GeoLocationInfo(minLat, minLon, maxLat, maxLon);
            }
        }


        GeoLocationInfo bounds = wmsInfo.getBounds();
        if ((bounds == null) && wmsInfo.isFixedImage()) {
            return null;
        }

        if (inGlobe || !wmsInfo.getAllowSubsets()) {
            return new GeoLocationInfo(wmsInfo.getBounds());
        } else {
            double minLon = rect.x;
            double maxLon = rect.x + rect.width;
            double minLat = rect.y;
            double maxLat = rect.y + rect.height;
            gli = new GeoLocationInfo(minLat, minLon, maxLat, maxLon);
            if (bounds != null) {
                gli.rectify(bounds, 0.0);
                gli.snapToGrid();
                gli.rectify(bounds, 0.0);
            }
        }
        return gli;
    }


    /**
     * Set the location
     *
     *
     * @param rect Bounding box
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setLocation(Rectangle2D.Double rect)
            throws VisADException, RemoteException {

        currentBounds = makeGeoLocationInfo(rect);

        if (inGlobe) {
            return;
        }


        if (currentBounds == null) {
            return;
        }
        if (wmsInfo == null) {
            return;
        }


        int[] ulScreen =
            earthToScreen(makeEarthLocation(currentBounds.getMaxLat(),
                                            currentBounds.getMinLon(), 0.0));
        int[] lrScreen =
            earthToScreen(makeEarthLocation(currentBounds.getMinLat(),
                                            currentBounds.getMaxLon(), 0.0));

        if (imageWidth > Math.abs(ulScreen[0] - lrScreen[0])) {
            imageWidth = Math.abs(ulScreen[0] - lrScreen[0]);
        }

        double widthDegrees = currentBounds.getMaxLon()
                              - currentBounds.getMinLon();
        double heightDegrees = currentBounds.getMaxLat()
                               - currentBounds.getMinLat();

        if (wmsInfo.getFixedWidth() > -1) {
            imageWidth = wmsInfo.getFixedWidth();
        } else {
            imageWidth = Math.min(Math.max(imageWidth, 50), 2000);
        }

        if (wmsInfo.getFixedHeight() > -1) {
            imageHeight = wmsInfo.getFixedHeight();
        } else {
            imageHeight = Math.abs((int) (imageWidth
                                          * currentBounds.getDegreesY()
                                          / currentBounds.getDegreesX()));
            imageHeight = Math.min(Math.max(imageHeight, 50), 2000);
        }
    }


    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        JMenuItem mi;
        super.getSaveMenuItems(items, forMenuBar);
        if (theDataChoice != null) {
            items.add(GuiUtils.makeMenuItem("Save As Image XML/KML File...",
                                            this, "writeImageXml"));
        }
    }

    /**
     * See if this can save data in cache.
     * @return true if allowable
     */
    protected boolean canSaveDataInCache() {
        return (theDataChoice != null);
    }

    /**
     * Ask the user for an image xml file name and write the image to it
     */
    public void writeImageXml() {
        String filename =
            FileManager.getWriteFile(
                Misc.newList(
                    ImageXmlDataSource.FILTER_XIMG,
                    KmlDataSource.FILTER_KML), ImageXmlDataSource.EXT_XIMG);
        if (filename != null) {
            writeFile = filename;
            loadImage();
        }

    }


    /**
     * Save the data choice into the cache data source
     */
    public void saveDataChoiceInCache() {
        try {
            getControlContext().getIdv().saveInCache(theDataChoice,
                    imageData, getTitle());
        } catch (Exception exc) {
            logException("Saving data to cache", exc);
        }
    }






    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        JComponent zPositionPanel  = doMakeZPositionSlider();

        JComponent alphaSliderComp = doMakeAlphaSlider();

        reprojectCbx = new JCheckBox("And Reproject", false);


        scaleSlider = GuiUtils.makeSlider(0, 100, (int) (scale - 1.0) * 25,
                                          this, "scaleSliderChanged");


        JComboBox layerBox = null;

        if ((wmsSelections != null) && (wmsSelections.size() > 1)) {
            layerBox = GuiUtils.makeComboBox(wmsSelections, wmsInfo, false,
                                             this, "selectLayer");
        } else if (theDataChoice != null) {
            List layerList =
                (List) theDataChoice.getProperty(WmsDataSource.PROP_LAYERS);
            if (layerList != null) {
                List dataSources = new ArrayList();
                theDataChoice.getDataSources(dataSources);
                if ((dataSources.size() == 1)
                        && (dataSources.get(0) instanceof WmsDataSource)) {
                    layerList =
                        ((WmsDataSource) dataSources.get(0)).getLayerList();
                }
            }

            if ((layerList != null) && (layerList.size() > 1)) {
                layerBox = GuiUtils.makeComboBox(layerList, theLayer, false,
                        this, "selectLayer");
            }

        }


        List timeList = ((wmsInfo == null)
                         ? null
                         : wmsInfo.getTimeList());
        if ((timeList != null) && (timeList.size() > 1)) {
            if (selectedTime == null) {
                selectedTime = (String) timeList.get(0);
            }
            timeBox = GuiUtils.makeComboBox(timeList, selectedTime, false,
                                            this, "setSelectedTime");
        }

        String[]       resNames    = { "Very High", "High", "Medium", "Low" };
        double[]       resValues   = { 0.5, 1.0, 2.0, 3.0 };
        ArrayList      resItems    = new ArrayList();
        TwoFacedObject resSelected = null;
        for (int i = 0; i < resValues.length; i++) {
            TwoFacedObject tfo = new TwoFacedObject(resNames[i],
                                     new Double(resValues[i]));
            resItems.add(tfo);
            if (resValues[i] == resolution) {
                resSelected = tfo;
            }
        }


        JComboBox resolutionBox = GuiUtils.makeComboBox(resItems,
                                      resSelected, false, this,
                                      "resolutionChanged");

        clickCbx = GuiUtils.makeCheckbox("Auto-Reload ", this,
                                         "enableAutoFetch");
        clickCbx.setToolTipText("Automatically reload image on pan/zoom");
        JButton reloadBtn =
            GuiUtils.makeImageButton(
                "/ucar/unidata/idv/control/images/Refresh16.gif", this,
                "loadImageFromScreen");
        reloadBtn.setToolTipText("Reload the image");

        JButton projectBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Home16.gif", this,
                                     "reproject");

        projectBtn.setToolTipText("Reproject to image");


        JPanel buttons = GuiUtils.hbox(Misc.newList(reloadBtn, projectBtn,
                             clickCbx), 4);

        List comps = new ArrayList();

        if (layerBox != null) {
            comps.add(GuiUtils.rLabel("Layer:"));
            comps.add(GuiUtils.left(layerBox));
        }

        //comps.add(GuiUtils.rLabel("On Zoom/Pan:"));
        comps.add(GuiUtils.rLabel("Image:"));
        comps.add(GuiUtils.left(buttons));


        JPanel labelPanel = GuiUtils.leftRight(GuiUtils.lLabel("Screen"),
                                GuiUtils.rLabel("5X"));




        comps.add(GuiUtils.rLabel("Resolution:"));
        comps.add(GuiUtils.left(resolutionBox));
        comps.add(GuiUtils.rLabel("Vertical Position:"));
        comps.add(GuiUtils.hgrid(zPositionPanel, GuiUtils.filler()));
        comps.add(GuiUtils.rLabel("Transparency:"));
        comps.add(GuiUtils.hgrid(alphaSliderComp, GuiUtils.filler()));
        comps.add(GuiUtils.rLabel("Dimension Scale:"));
        comps.add(GuiUtils.hgrid(GuiUtils.vbox(scaleSlider, labelPanel),
                                 GuiUtils.filler()));

        JComponent contents = GuiUtils.formLayout(comps);
        return GuiUtils.top(contents);

    }


    /**
     * Handle the slider changing
     *
     * @param value new slider value
     */
    public void scaleSliderChanged(int value) {
        double percent  = value / 100.0;
        double newScale = 1.0 + percent * 4.0;
        if (newScale != scale) {
            scale = newScale;
            loadImageFromScreen();
        }
    }



    /**
     * Load the iamge from screen bounds
     */
    public void loadImageFromScreen() {
        try {
            if ( !getActive()) {
                return;
            }
            NavigatedDisplay navDisplay = getNavigatedDisplay();
            if ((navDisplay == null) || (navDisplay.getDisplay() == null)) {
                //                    || (navDisplay.getDisplay().getComponent() == null)) {
                return;
            }
            Rectangle screenBounds = navDisplay.getScreenBounds();
            imageWidth = (int) (screenBounds.width * scale);
            Rectangle2D.Double rect = getNavigatedDisplay().getLatLonBox();
            if ((scale > 1.0) && !inGlobe) {
                double newWidth  = scale * rect.getWidth();
                double newHeight = scale * rect.getHeight();
                rect.setRect(rect.getX() - (newWidth - rect.getWidth()) / 2,
                             rect.getY()
                             - (newHeight - rect.getHeight()) / 2, newWidth,
                                 newHeight);
            }


            GeoLocationInfo tmp = makeGeoLocationInfo(rect);
            setLocation(rect);
            loadImage();
        } catch (Exception exc) {
            logException("Loading image", exc);
        }
    }


    /**
     * Load image into displayable
     */
    private void loadImage() {
        //      System.err.println (cnt+" WMSControl loadImage");
        //      Misc.printStack (cnt+" WMSControl loadImage");
        waitingToLoad = true;
        Misc.run(new Runnable() {
            public void run() {
                try {
                    loadImage(loadId =
                        JobManager.getManager().stopAndRestart(loadId,
                            "WMSControl"));
                } finally {
                    waitingToLoad = false;
                }
            }
        });
    }




    /**
     * Load image into displayable
     *
     * @param myLoadId Job manager load id
     */
    private void loadImage(Object myLoadId) {

        showWaitCursor();
        try {
            long      t1        = System.currentTimeMillis();
            FieldImpl imageData = readImageData();
            long      t2        = System.currentTimeMillis();
            //            System.err.println ("WMS:reading image data " + (t2-t1)); 
            if (imageData != null) {
                applyData(imageData);
            }
        } catch (Exception exc) {
            logException("There was an error accessing the image.", exc);
        }
        showNormalCursor();
    }


    /**
     * Read the image data
     *
     * @return The field or null
     *
     * @throws Exception On badness
     */
    private FieldImpl readImageData() throws Exception {


        //Hard code the image widths based on the resolution when we're in the globe
        if (inGlobe) {
            if ((wmsInfo != null) && (wmsInfo.getFixedWidth() > -1)) {
                imageWidth = wmsInfo.getFixedWidth();
            } else {
                if (resolution == 0.5) {
                    imageWidth = 2048;
                } else if (resolution == 1.0) {
                    imageWidth = 1024;
                } else if (resolution == 2.0) {
                    imageWidth = 512;
                } else {
                    imageWidth = 256;
                }
            }
            imageHeight = imageWidth / 2;
        }



        if (currentBounds != null) {
            if ((currentBounds.getDegreesX() == 0.0)
                    || (currentBounds.getDegreesY() == 0.0)) {
                //                System.err.println ("current bounds is bad");
                return null;
            }
        }


        if (theDataChoice != null) {
            Hashtable requestProperties = new Hashtable();
            if (theLayer != null) {
                requestProperties.put(WmsDataSource.PROP_LAYER, theLayer);
            }
            if (currentBounds != null) {
                requestProperties.put(WmsDataSource.PROP_BOUNDS,
                                      currentBounds);
            }
            if ( !inGlobe) {
                requestProperties.put(WmsDataSource.PROP_RESOLUTION,
                                      new Double(resolution));
            }
            requestProperties.put(WmsDataSource.PROP_IMAGEWIDTH,
                                  new Integer(imageWidth));
            //            System.err.println ("imageWidth:" + imageWidth + " imageHeight:" + imageHeight);

            requestProperties.put(WmsDataSource.PROP_IMAGEHEIGHT,
                                  new Integer(imageHeight));

            //            theDataChoice.setFixedRequestProperties(requestProperties);
            if (writeFile != null) {
                requestProperties.put(WmsDataSource.PROP_WRITEFILE,
                                      writeFile);
            }

            try {
                FieldImpl data = (FieldImpl) theDataChoice.getData(null,
                                     requestProperties);
                if (data == null) {
                    return null;
                }
                imageData = data;
            } catch (NullPointerException npe) {
                userMessage("Image layer does not exist");
            } finally {
                writeFile = null;
            }

            String iconPath =
                (String) requestProperties.get(WmsDataSource.PROP_ICONPATH);
            if ( !Misc.equals(iconPath, legendIconUrl)) {
                legendIconUrl = iconPath;
                updateLegendAndList();
            }


            return imageData;
        }








        //        System.err.println(wmsInfo.getAllowSubsets() + " " + wmsInfo.getBounds());
        GeoLocationInfo boundsToUse = ((inGlobe || !wmsInfo.getAllowSubsets())
                                       ? wmsInfo.getBounds()
                                       : currentBounds);



        Image  image = null;
        String url;
        if (wmsInfo.isFixedImage()) {
            url = wmsInfo.getImageFile();
            FieldImpl result = fixedImageCache.get(url);
            if (result != null) {
                return result;
            }
        } else {
            url = wmsInfo.assembleRequest(boundsToUse,
                                          (int) (imageWidth / resolution),
                                          (int) (imageHeight / resolution));
        }




        String cacheGroup   = "WMS";
        byte[] imageContent = null;
        if (image == null) {
            synchronized (cachedUrls) {
                for (int i = 0; i < cachedUrls.size(); i++) {
                    if (url.equals(cachedUrls.get(i))) {
                        image = (Image) cachedData.get(i);
                        break;
                    }
                }
            }
        }


        FieldImpl xyData = null;
        try {
            Object myLoadId = loadId;
            if (image == null) {
                if (imageContent == null) {
                    imageContent =
                        IOUtil.readBytes(IOUtil.getInputStream(url), loadId);
                    //If it is null then there is another thread that is doing
                    //a subsequent read
                    if (imageContent == null) {
                        return null;
                    }
                }
                image = Toolkit.getDefaultToolkit().createImage(imageContent);
                if ( !JobManager.getManager().canContinue(myLoadId)) {
                    return null;
                }
                synchronized (cachedUrls) {
                    if (cachedUrls.size() > 5) {
                        cachedUrls.remove(cachedUrls.size() - 1);
                        cachedData.remove(cachedData.size() - 1);
                    }
                    cachedUrls.add(0, url);
                    cachedData.add(0, image);
                }
            }
            xyData = DataUtility.makeField(image);
        } catch (Exception iexc) {
            if (imageContent != null) {
                String msg = new String(imageContent);
                msg = StringUtil.replace(msg, "\n", " ").toLowerCase();
                if (StringUtil.stringMatch(msg, "Service\\s*Exception")) {
                    if (StringUtil.stringMatch(msg,
                            "cannot\\s*be\\s*less\\s*than")) {
                        return null;
                    }
                }
                if (msg.indexOf("error") >= 0) {
                    userErrorMessage(
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
        Linear2DSet domain = (Linear2DSet) xyData.getDomainSet();
        Linear2DSet imageDomain =
            new Linear2DSet(RealTupleType.SpatialEarth2DTuple,
                            boundsToUse.getMinLon(), boundsToUse.getMaxLon(),
                            domain.getX().getLength(),
                            boundsToUse.getMaxLat(), boundsToUse.getMinLat(),
                            domain.getY().getLength());


        FieldImpl result = GridUtil.setSpatialDomain(xyData, imageDomain,
                               true);
        if (wmsInfo.isFixedImage()) {
            fixedImageCache.put(url, result);
        }
        return result;
    }



    /**
     * Even though we do have data that holds a MapProjection we don't
     * want to provide it so we return  null.
     *
     * @return null
     */
    public MapProjection getDataProjection() {
        return null;
    }


    /**
     * This gets called when the user selects the View menu item "Use Native Image Projection"
     *
     * @return MapProjection  for the data
     */
    public MapProjection getDataProjectionForMenu() {
        return super.getDataProjection();
    }


    /**
     * Set the data on the display. Perform the color exclude.
     *
     *
     * @param imageData The image data
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void applyData(FieldImpl imageData)
            throws VisADException, RemoteException {

        //        FieldImpl imageData = GridUtil.setSpatialDomain(xyData, imageDomain,
        //                                  true);

        List colorExcludes = Misc.newList(new ColorExclude(Color.white, 20),
                                          new ColorExclude(Color.yellow, 20));
        float[][] rangeValues = imageData.getFloats(false);
        int       numPts      = rangeValues[0].length;
        int       cnt         = 0;
        for (int i = 0; false && (i < colorExcludes.size()); i++) {
            ColorExclude exclude = (ColorExclude) colorExcludes.get(i);
            int          red     = exclude.baseColor.getRed();
            int          green   = exclude.baseColor.getGreen();
            int          blue    = exclude.baseColor.getBlue();

            for (int ptIdx = 0; ptIdx < numPts; ptIdx++) {
                if (ptIdx < 10) {
                    System.err.println("v:" + rangeValues[0][ptIdx] + " "
                                       + red + " " + exclude.redRange);
                }
                if ((Math.abs(
                        rangeValues[0][ptIdx]
                        - red) < exclude.redRange) && (Math.abs(
                            rangeValues[1][ptIdx]
                            - green) < exclude.greenRange) && (Math.abs(
                                rangeValues[2][ptIdx]
                                - blue) < exclude.blueRange)) {
                    rangeValues[0][ptIdx] = Float.NaN;
                    cnt++;
                }
            }
        }
        //        System.err.println("cnt:" + cnt);
        imageDisplay.loadData(imageData);
    }




    /**
     * Class ColorExclude Holds information to exclude certain colors
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.104 $
     */
    public static class ColorExclude {

        /** The color */
        Color baseColor;

        /** Wiggle room for red */
        int redRange = 10;

        /** Wiggle room for green */
        int greenRange = 10;

        /** Wiggle room for  blue */
        int blueRange = 10;

        /**
         * Ctor
         *
         * @param c The color
         * @param range  Wiggle room for  all colors
         */
        public ColorExclude(Color c, int range) {
            this(c, range, range, range);
        }

        /**
         * Ctor
         *
         * @param c The color
         * @param rr  Wiggle room for  red
         * @param gr  Wiggle room for green
         * @param br  Wiggle room for blue
         */
        public ColorExclude(Color c, int rr, int gr, int br) {
            this.baseColor = c;
            redRange       = rr;
            greenRange     = gr;
            blueRange      = br;
        }
    }


    /**
     * Reproject the image
     */
    public void reproject() {
        setProjectionInView(false);
    }


    /**
     * set the data
     *
     * @param dataChoice The data
     *
     * @return ok
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        imageDisplay.loadData(null);
        return true;
    }



    /**
     * Set the wmsinfo
     *
     * @param layer The new layer
     */
    public void selectLayer(Object layer) {
        legendIconUrl = null;
        legendImage   = null;
        if (layer instanceof WmsSelection) {
            wmsInfo = (WmsSelection) layer;
        } else {
            theLayer = layer;
        }
        updateLegendAndList();
        if (imageDisplay != null) {
            if (isLayerFixed() && getUseFastRendering()) {
                setUseFastRendering(false);
            }
            loadImageFromScreen();
        }
    }


    /**
     *  Set the ImageWidth property.
     *
     *  @param value The new value for ImageWidth
     */
    public void setImageWidth(int value) {
        imageWidth = value;
    }

    /**
     *  Get the ImageWidth property.
     *
     *  @return The ImageWidth
     */
    public int getImageWidth() {
        return imageWidth;
    }

    /**
     *  Set the ImageHeight property.
     *
     *  @param value The new value for ImageHeight
     */
    public void setImageHeight(int value) {
        imageHeight = value;
    }

    /**
     *  Get the ImageHeight property.
     *
     *  @return The ImageHeight
     */
    public int getImageHeight() {
        return imageHeight;
    }




    /**
     *  User changed resolution
     *
     *  @param tfo New resolution
     */
    public void resolutionChanged(TwoFacedObject tfo) {
        resolution = ((Double) tfo.getId()).doubleValue();
        loadImage();
    }


    /**
     * Set the Resolution property.
     *
     * @param value The new value for Resolution
     */
    public void setResolution(double value) {
        resolution = value;
    }

    /**
     * Get the Resolution property.
     *
     * @return The Resolution
     */
    public double getResolution() {
        return resolution;
    }


    /**
     * Set the CurrentBounds property.
     *
     * @param value The new value for CurrentBounds
     */
    public void setCurrentBounds(GeoLocationInfo value) {
        currentBounds = value;
    }

    /**
     * Get the CurrentBounds property.
     *
     * @return The CurrentBounds
     */
    public GeoLocationInfo getCurrentBounds() {
        return currentBounds;
    }


    /**
     *  Set the WmsInfo property.
     *
     *  @param value The new value for WmsInfo
     */
    public void setWmsInfo(WmsSelection value) {
        wmsInfo = value;
        updateLegendAndList();
        if ((value != null)
                && ((wmsSelections == null) || (wmsSelections.size() == 0))) {
            wmsSelections = Misc.newList(value);
        }
    }

    /**
     *  Get the WmsInfo property.
     *
     *  @return The WmsInfo
     */
    public WmsSelection getWmsInfo() {
        return wmsInfo;
    }


    /**
     * Get the window title
     *
     * @return Window title
     */
    protected String getTitle() {
        if (wmsInfo != null) {
            return wmsInfo.getTitle();
        }
        return super.getTitle();
    }

    /**
     * Get the legend labesl
     *
     *
     * @param labels List of (String) labels
     * @param legendType For side or bottom
     */
    protected void getLegendLabels(List labels, int legendType) {
        WmsSelection selection = wmsInfo;
        if ((selection == null) && (theLayer != null)
                && (theLayer instanceof WmsSelection)) {
            selection = (WmsSelection) theLayer;
        }
        if (theLayer != null) {
            labels.add(theLayer.toString());
            setParamName(theLayer.toString());
        } else {
            super.getLegendLabels(labels, legendType);
        }
        if (wmsInfo != null) {
            labels.add(wmsInfo.getTitle());
            setParamName(wmsInfo.getTitle());
        }
        checkLegendIconLbl();
        legendIconLbl.setIcon(null);
        if ((legendIconUrl == null) && (selection != null)) {
            legendIconUrl = selection.getLegendIcon();
        }


        if (legendIconUrl != null) {
            ImageIcon icon  = GuiUtils.getImageIcon(legendIconUrl);
            Image     image = icon.getImage();
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                setLegendIcon(image, image.getWidth(null),
                              image.getHeight(null));
            } else {
                image.getHeight(this);
            }
        } else {
            legendIconLbl.setIcon(null);
        }
    }


    /**
     * Handle the image update
     *
     * @param img The image
     * @param flags flags
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     *
     * @return Continue loading
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {
        if ((flags & ImageObserver.ALLBITS) != 0) {
            setLegendIcon(img, width, height);
            return false;
        }
        return true;
    }


    /**
     * Set the legend icon. This shrinks the image if it is too big.
     *
     * @param img The image
     * @param width width
     * @param height height
     */
    private void setLegendIcon(Image img, int width, int height) {
        legendImage = img;
        Image theImage      = img;
        int   desiredWidth  = 250;
        int   desiredHeight = 100;
        if ((width > desiredWidth) || (height > desiredHeight)) {
            if (width > height) {
                height = (int) (desiredWidth * height / width);
                width  = desiredWidth;
            } else {
                width  = (int) (desiredHeight * width / height);
                height = desiredHeight;
            }
            theImage = img.getScaledInstance(width, height, 0);
        }
        legendIconLbl.setIcon(new ImageIcon(theImage));
    }


    /**
     * Assume that any display controls that have a color table widget
     * will want the color table to show up in the legend.
     *
     * @param  legendType  type of legend
     * @return The extra JComponent to use in legend
     */
    protected JComponent getExtraLegendComponent(int legendType) {
        JComponent parentComp = super.getExtraLegendComponent(legendType);
        if (legendType == BOTTOM_LEGEND) {
            return parentComp;
        }
        checkLegendIconLbl();
        return GuiUtils.vbox(parentComp, legendIconLbl);
    }


    /**
     * Make the legend icon label
     */
    private void checkLegendIconLbl() {
        if (legendIconLbl != null) {
            return;
        }
        legendIconLbl = new JLabel("");
        legendIconLbl.setToolTipText("Click to see full legend");
        legendIconLbl.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (legendImage == null) {
                    return;
                }
                showLegendImage();
            }

            public void mouseEntered(MouseEvent e) {
                if (legendImage == null) {
                    return;
                }
            }

            public void mouseExited(MouseEvent e) {
                if (legendImage == null) {
                    return;
                }
            }
        });
    }

    /**
     * Popup a dialog that shows the full legend image
     */
    private void showLegendImage() {
        JLabel lbl = new JLabel(new ImageIcon(legendImage));
        JScrollPane sp = GuiUtils.makeScrollPane(lbl,
                             legendImage.getWidth(null), 400);
        sp.setPreferredSize(new Dimension(Math.max(300,
                legendImage.getWidth(null)), 400));
        GuiUtils.showDialog("Legend", sp);
    }




    /**
     *  Set the WmsInfos property.
     *
     *  @param value The new value for WmsInfos
     */
    public void setWmsInfos(List value) {
        wmsSelections = value;
        if ((wmsSelections != null) && (wmsSelections.size() > 0)
                && (wmsInfo == null)) {


            if ((theLayer != null) && (theLayer instanceof String)) {
                for (WmsSelection selection :
                        (List<WmsSelection>) wmsSelections) {
                    if (Misc.equals(theLayer, selection.getLayer())) {
                        wmsInfo = selection;
                        break;
                    }
                }
            }


            if (wmsInfo == null) {
                wmsInfo = (WmsSelection) wmsSelections.get(0);
            }
            updateLegendAndList();
        }
    }

    /**
     *  Get the WmsInfos property.
     *
     *  @return The WmsInfos
     */
    public List getWmsInfos() {
        return wmsSelections;
    }


    /**
     * Set the EnableClick property.
     *
     * @param value The new value for EnableClick
     * @deprecated  use setEnableAutoFetch
     */
    public void setEnableClick(boolean value) {
        enableAutoFetch = value;
    }

    /**
     * Get the EnableAutoFetch property.
     *
     * @return The EnableAutoFetch property
     */
    public boolean getEnableAutoFetch() {
        return enableAutoFetch;
    }

    /**
     * Set the EnableImageFetch property.
     *
     * @param value The new value for EnableAutoFetch
     */
    public void setEnableAutoFetch(boolean value) {
        enableAutoFetch = value;
    }

    /**
     * Set the SelectedTime property.
     *
     * @param value The new value for SelectedTime
     */
    public void setSelectedTime(String value) {
        selectedTime = value;
        if (getHaveInitialized()) {
            loadImage();
        }
    }

    /**
     * Get the SelectedTime property.
     *
     * @return The SelectedTime
     */
    public String getSelectedTime() {
        return selectedTime;
    }


    /**
     *  Set the TheLayer property.
     *
     *  @param value The new value for TheLayer
     */
    public void setTheLayer(Object value) {
        theLayer = value;
    }

    /**
     *  Get the TheLayer property.
     *
     *  @return The TheLayer
     */
    public Object getTheLayer() {
        return theLayer;
    }


    /**
     * Set the Scale property.
     *
     * @param value The new value for Scale
     */
    public void setScale(double value) {
        scale = value;
    }

    /**
     * Get the Scale property.
     *
     * @return The Scale
     */
    public double getScale() {
        return scale;
    }

    /**
     * Get default z position to use
     *
     * @return Default z position
     */
    protected double getInitialZPosition() {
        if (inGlobeDisplay()) {
            return super.getInitialZPosition() - ZFUDGE;
        }
        return super.getInitialZPosition();
    }



}

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

package ucar.unidata.idv.control;


import org.w3c.dom.Element;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.gis.SpatialGrid;
import ucar.unidata.gis.WorldWindReader;


import ucar.unidata.idv.DisplayConventions;


import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.symbol.*;

import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.*;

import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.StationLocationDisplayable;
import ucar.visad.display.StationModelDisplayable;


import visad.*;


import visad.georef.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;
import visad.georef.NamedLocation;
import visad.georef.NamedLocationTuple;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;


import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;




/**
 * Class to display a set of locations
 *
 * @author MetApps Development Team
 * @version $Revision: 1.18 $ $Date: 2007/04/11 18:56:05 $
 */


public class WorldWindControl extends StationLocationControl {



    /** xml tag */
    private static final String TAG_MAXIMUMDISPLAYRANGE =
        "MaximumDisplayRange";

    /** xml tag */
    private static final String TAG_MINIMUMDISPLAYRANGE =
        "MinimumDisplayRange";

    /** xml tag */
    private static final String TAG_PLACENAMELISTFILEPATH =
        "PlacenameListFilePath";

    /** xml tag */
    private static final String TAG_TILEDPLACENAMESET = "TiledPlacenameSet";

    /** mutex for synchronization */
    private static Object MUTEX = new Object();

    /** the layers available */
    private static List layers;

    /** We cache the features */
    private static Hashtable cache = new Hashtable();

    /** What layers are shown */
    private Hashtable layerVisibility = new Hashtable();

    /** Are we loading a layer */
    private boolean loading = false;

    /** List of created stations */
    private List stationList = new ArrayList();

    /** The url root where we find location data */
    private String wwPath;


    /** do we keep a list of all shown stations */
    private boolean onlyShowVisible = true;

    /** The stations we have seen */
    private Hashtable seenLocations = new Hashtable();


    /**
     * Default cstr;
     */
    public WorldWindControl() {}

    /**
     * Called to make this kind of Display Control; also calls code to
     * made the Displayable.  This method is called from inside
     * DisplayControlImpl.init(several args).  This implementation
     * gets the list of stationTables to be used.
     *
     * @param dataChoice    the DataChoice of the moment -
     *
     * @return  true if successful
     *
     * @throws  VisADException  there was a VisAD error
     * @throws  RemoteException  there was a remote error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        PointProbe probe = new PointProbe(0.0, 0.0, 0.0);
        probe.setVisible(false);
        addDisplayable(probe);

        synchronized (MUTEX) {
            if (layers == null) {
                try {
                    loadLayers();
                    if (layers == null) {
                        return false;
                    }
                } catch (Exception exc) {
                    logException("Error loading layers", exc);
                    return false;
                }
            }
        }
        return super.init(dataChoice);
    }


    /**
     * Loads the data into the <code>StationModelDisplayable</code>.
     * Declutters the stations if necessary.
     */
    public void loadData() {
        try {
            //      System.err.println("loadData:" + getHaveInitialized());
            createStationList();
            super.loadData();
        } catch (Exception excp) {
            logException("loading data ", excp);
        }
    }

    /**
     * Override base class method to make the station list panel
     *
     * @param comps Holds the components to put in the gui
     */
    protected void doMakeStationListPanel(List comps) {
        List checkBoxes = new ArrayList();
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = (Layer) layers.get(i);
            checkBoxes.add(GuiUtils.makeCheckbox(layer.name, this, "layerOn",
                    layer));
        }


        JPanel layerPanel = GuiUtils.vbox(checkBoxes);
        JScrollPane layerScroller = GuiUtils.makeScrollPane(layerPanel, 200,
                                        100);
        layerScroller.setPreferredSize(new Dimension(200, 100));
        comps.add(GuiUtils.top(GuiUtils.rLabel("Layers:")));
        comps.add(layerScroller);
    }


    /**
     * set visibility of the layer
     *
     * @param b on or off
     * @param layer the layer_
     */
    public void setLayerOn(boolean b, Layer layer) {
        layerVisibility.put(layer.name, new Boolean(b));
        loadData();
    }

    /**
     * Is the layer visibile
     *
     * @param layer The layer
     *
     * @return is visible
     */
    public boolean getLayerOn(Layer layer) {
        Boolean b = (Boolean) layerVisibility.get(layer.name);
        if (b != null) {
            return b.booleanValue();
        }
        return true;
    }


    /**
     * Create the list of stations
     */
    private void createStationList() {
        try {
            WorldWindReader    reader      = new WorldWindReader();
            Rectangle2D.Double rect = getNavigatedDisplay().getLatLonBox();
            List               layersToUse = new ArrayList();
            for (int i = 0; i < layers.size(); i++) {
                Layer layer = (Layer) layers.get(i);
                if (getLayerOn(layer)) {
                    layersToUse.addAll(layer.wwps);
                }
            }

            List wwps = reader.findWwps(layersToUse, rect);
            if (onlyShowVisible) {
                stationList   = new ArrayList();
                seenLocations = new Hashtable();
            }

            /*
            System.out.println(wwps.size()+ " rect:" + rect.x+"/"+rect.y + " " +
                               (rect.width) + "/" +
                               (rect.height));*/

            for (int i = 0; i < wwps.size(); i++) {
                String                      filename = wwps.get(i).toString();
                WorldWindReader.FeatureList features = null;
                synchronized (cache) {
                    features =
                        (WorldWindReader.FeatureList) cache.get(filename);
                    if (features == null) {
                        features = reader.readWWP(filename);
                        cache.put(filename, features);
                    }
                }
                for (int featureIdx = 0; featureIdx < features.names.length;
                        featureIdx++) {
                    if (rect.contains(features.lons[featureIdx],
                                      features.lats[featureIdx])) {
                        String name = new String(features.names[featureIdx]);
                        if ( !onlyShowVisible
                                && (seenLocations.get(name) != null)) {
                            continue;
                        }
                        seenLocations.put(name, name);
                        NamedStationImpl station =
                            new NamedStationImpl(name, name,
                                features.lats[featureIdx],
                                features.lons[featureIdx], 0.0,
                                CommonUnit.meter);
                        stationList.add(station);
                    }
                }
            }

            //      System.err.println ("size:" + stationList.size());      
            //      System.err.println ("list:" + stationList);
        } catch (Exception exc) {
            logException("Error loading WorldWind locations", exc);
        }
        loading = false;
    }


    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {

        super.getSaveMenuItems(items, forMenuBar);
        items.add(GuiUtils.makeMenuItem("Save Locations...", this,
                                        "saveLocations"));

    }

    /**
     * Add the view menu items
     *
     * @param items List of items
     * @param forMenuBar for menubar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        super.getViewMenuItems(items, forMenuBar);
        items.add(
            GuiUtils.makeCheckboxMenuItem(
                "Only show visible locations", this, "onlyShowVisible",
                null));
    }



    /**
     * Write out the current list of locations as a stations xml file
     */
    public void saveLocations() {
        try {
            String filename =
                FileManager.getWriteFile(FileManager.FILTER_XML,
                                         FileManager.SUFFIX_XML);
            if (filename == null) {
                return;
            }
            String xml = NamedStationTable.getStationXml(
                             IOUtil.getFileTail(
                                 IOUtil.stripExtension(filename)), null,
                                     stationList);
            IOUtil.writeFile(filename, xml);
        } catch (Exception exc) {
            logException("Error writing station file", exc);
        }

    }


    /**
     * Initialize the worldwind path
     *
     * @return Where to look for data
     */
    private String initWWPath() {
        wwPath =
            getControlContext().getResourceManager().getResourceUrlBase()
            + "/worldwind";
        String layersXml = null;

        try {
            layersXml = new String(IOUtil.readBytesAndCache(wwPath
                    + "/Configuration/WorldLayers.xml", "WorldWind"));
        } catch (Exception exc) {}
        if (layersXml == null) {
            userMessage(
                "<html>Could not find the 'WorldLayers.xml' configuration file in:<br>"
                + wwPath + "</html>");
        }
        return layersXml;
    }


    /**
     * Load in the layers from the xml
     *
     * @throws Exception On badness
     */
    private void loadLayers() throws Exception {
        String layersXml = initWWPath();
        if (layersXml == null) {
            return;
        }
        Element root    = XmlUtil.getRoot(layersXml);
        List placeNames = XmlUtil.findDescendants(root,
                              TAG_TILEDPLACENAMESET);
        WorldWindReader reader = new WorldWindReader();
        layers = new ArrayList();
        for (int i = 0; i < placeNames.size(); i++) {
            Element placename = (Element) placeNames.get(i);
            String name = XmlUtil.getChildText(XmlUtil.getElement(placename,
                              "Name"));
            double minRange =
                new Double(XmlUtil.getChildText(XmlUtil.getElement(placename,
                    TAG_MINIMUMDISPLAYRANGE))).doubleValue();
            double maxRange =
                new Double(XmlUtil.getChildText(XmlUtil.getElement(placename,
                    TAG_MAXIMUMDISPLAYRANGE))).doubleValue();
            String path = XmlUtil.getChildText(XmlUtil.getElement(placename,
                              TAG_PLACENAMELISTFILEPATH));
            path = StringUtil.replace(path, "\\", "/");
            if ( !path.startsWith("/")) {
                path = "/" + path;
            }
            List wwps = reader.readWPL(wwPath + path, minRange, maxRange);
            layers.add(new Layer(name, wwps));
        }
    }

    /**
     * Class Layer represents a layer
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.18 $
     */
    private static class Layer {

        /** List of wwp info */
        List wwps;

        /** Name of layer */
        String name;

        /**
         * ctor
         *
         * @param name name
         * @param wwps place names
         */
        public Layer(String name, List wwps) {
            this.name = name;
            this.wwps = wwps;
        }
    }

    /**
     * Get the station List.
     *
     * @return  the station list
     */
    protected List getStationList() {
        return stationList;
    }

    /**
     * set of visible layers
     *
     * @return  set of visible layers
     */
    public Hashtable getLayerVisibility() {
        return layerVisibility;
    }

    /**
     *  set of visible layers
     *
     * @param v  set of visible layers
     */
    public void setLayerVisibility(Hashtable v) {
        layerVisibility = v;
    }



    /**
     * Set the OnlyShowVisible property.
     *
     * @param value The new value for OnlyShowVisible
     */
    public void setOnlyShowVisible(boolean value) {
        onlyShowVisible = value;
    }

    /**
     * Get the OnlyShowVisible property.
     *
     * @return The OnlyShowVisible
     */
    public boolean getOnlyShowVisible() {
        return onlyShowVisible;
    }


}

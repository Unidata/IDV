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
package ucar.unidata.idv.chooser;


import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import org.w3c.dom.Element;

import thredds.catalog.XMLEntityResolver;

import ucar.nc2.dt.StationImpl;

import ucar.nc2.thredds.TDSRadarDatasetCollection;
import ucar.nc2.units.DateUnit;

import ucar.unidata.data.radar.RadarQuery;
import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.util.*;

import visad.CommonUnit;
import visad.DateTime;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.io.IOException;

import java.net.URI;

import java.util.*;
import java.util.List;

import javax.swing.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Jan 16, 2008
 * Time: 11:17:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class TDSRadarChooser1 extends TimesChooser {

    /**
     *  Holds the main gui contents. We keep this around so we can replace it with an error message
     *  when the connection to the service fails.
     */
    private JComponent outerContents;


    /** The collection */
    private TDSRadarDatasetCollection collection;


    /** The currently selected station */
    private NamedStation selectedStation;

    /** The currently selected level3 product */
    private String selectedProduct;

    /** Those urls we connect to */
    //"http://motherlode.ucar.edu:8080/thredds/radarServer/catalog.xml";
    private String serverUrl;

    /** Each dataset collection URL */
    //"http://motherlode.ucar.edu:8080/thredds/radarServer/level2/idd/dataset.xml";
    //private String collectionUrl;

    /** Component to hold collections */
    private JComboBox collectionSelector;

    /** Component to hold product list */
    private JComboBox productComboBox;

    /** descriptor label */
    private JComponent productLabel;


    /** components that need a server for activation */
    private List compsThatNeedServer = new ArrayList();

    /** components that need a server for activation */
    private List level3CompsThatNeedServer = new ArrayList();

    /** persistent holder for catalog URLS */
    private PreferenceList urlListHandler;

    /** catalog URL holder */
    private JComboBox urlBox;

    /** ok flag */
    private boolean okToDoUrlListEvents = true;

    /** dataset list */
    private List datasetList;

    /** Command for connecting */
    protected static final String CMD_CONNECT = "cmd.connect";

    /** _more_          */
    private boolean isLevel3;

    /** _more_          */
    public static final String[] level3_ExName = { "NVW", "DPA" };


    /**
     * Create the RadarChooser
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public TDSRadarChooser1(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }



    /**
     * Handle the update event. Just pass it through to the imageChooser
     */
    public void doUpdate() {
        if ((serverUrl == null) || (datasetList == null)
                || (datasetList.size() == 0) || (selectedProduct == null)) {
            if (urlBox != null) {
                setServer((String) urlBox.getSelectedItem());
            }
            return;
        }
        Misc.run(this, "stationOrProductChanged");
    }



    /**
     * Update the status of the gui
     */
    protected void updateStatus() {
        super.updateStatus();
        if (selectedStation == null) {
            setHaveData(false);
            setStatus("Please select a station", "stations");
            return;
        }
        if (isLevel3 && (selectedProduct == null)) {
            setHaveData(false);
            setStatus("Please select a level 3 product", "products");
            return;
        }
        boolean haveTimesSelected;
        if (getDoAbsoluteTimes()) {
            haveTimesSelected = getSelectedAbsoluteTimes().size() > 0;
        } else {
            haveTimesSelected = true;
        }
        setHaveData(haveTimesSelected);
        if (haveTimesSelected) {
            setStatus("Press \"" + CMD_LOAD
                      + "\" to load the selected radar data", "buttons");
        } else {
            setStatus("Please select times", "timepanel");
        }
    }



    /**
     * Handle when there are newly selected stations
     *
     * @param stations list of newly selected stations
     */
    protected void newSelectedStations(List stations) {
        super.newSelectedStations(stations);
        if ((stations == null) || (stations.size() == 0)) {
            selectedStation = null;
        } else {
            NamedStation newStation = (NamedStation) stations.get(0);
            if (Misc.equals(newStation, selectedStation)) {
                return;
            }
            selectedStation = newStation;
        }
        Misc.run(TDSRadarChooser1.this, "stationOrProductChanged");
    }

    /**
     * Make the contents
     *
     * @return  the contents
     */
    protected JComponent doMakeContents() {

        //Get the list of catalogs but remove the old catalog.xml entry
        urlListHandler = getPreferenceList(PREF_TDSRADARSERVER);

        ActionListener catListListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if ( !okToDoUrlListEvents) {
                    return;
                }
                setServer((String) urlBox.getSelectedItem());
            }
        };
        urlBox = urlListHandler.createComboBox(GuiUtils.CMD_UPDATE,
                catListListener, true);


        collectionSelector = new JComboBox();
        collectionSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {

                if (collectionSelector.getSelectedItem() == null) {
                    return;
                }
                String collectionUrl =
                    TwoFacedObject.getIdString(
                        collectionSelector.getSelectedItem());

                if (collectionUrl.contains("level3")) {
                    setLevel3Collection(collectionUrl);
                } else {
                    setCollection(collectionUrl);
                }
            }

        });
        productLabel = addLevel3ServerComp(GuiUtils.rLabel("Product" + ":"));
        productComboBox = new JComboBox();
        productComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (productComboBox.getSelectedItem() == null) {
                    return;
                }
                selectedProduct =
                    productComboBox.getSelectedItem().toString();
                resetProductBox();
                productChanged();
            }

        });
        addLevel3ServerComp(productComboBox);


        JComponent stationMap = getStationMap();
        JComponent buttons    = getDefaultButtons();
        JComponent timesPanel = makeTimesPanel(true, true);
        GuiUtils.tmpInsets = GRID_INSETS;



        JPanel top = GuiUtils.doLayout(new Component[] {
                         GuiUtils.rLabel("Catalog:"),
                         urlBox, }, 2, GuiUtils.WT_NYNY, GuiUtils.WT_N);

        JPanel topb = GuiUtils.doLayout(new Component[] {
                          GuiUtils.rLabel("Collections:"),
                          GuiUtils.hbox(collectionSelector,
                                        GuiUtils.filler()),
                          GuiUtils.hbox(productLabel, GuiUtils.filler()),
                          GuiUtils.hbox(productComboBox,
                                        GuiUtils.filler()) }, 4,
                                            GuiUtils.WT_NYNY, GuiUtils.WT_N);

        GuiUtils.tmpInsets = new Insets(0, 2, 0, 2);

        stationMap.setPreferredSize(new Dimension(230, 200));
        stationMap = registerStatusComp("stations", stationMap);
        timesPanel = registerStatusComp("timepanel", timesPanel);
        addServerComp(stationMap);
        addServerComp(timesPanel);
        JComponent contents = GuiUtils.doLayout(new Component[] { top, topb,
        //  stationMap, timesPanel }, 1, GuiUtils.WT_YYY, ,GuiUtils.WT_NYY);
        stationMap, timesPanel }, 1, GuiUtils.WT_Y, new double[] { 0.5, 0.25,
                4.0, 1.0 });

        contents = GuiUtils.inset(contents, 5);
        GuiUtils.enableComponents(compsThatNeedServer, false);
        GuiUtils.enableComponents(level3CompsThatNeedServer, false);
        //  Misc.run(this, "initializeCollection");
        outerContents =
            GuiUtils.center(GuiUtils.topCenterBottom(getStatusComponent(),
                contents, buttons));
        return outerContents;
    }

    /** A widget for the list of dataset descriptors */


    /** Flag to keep from infinite looping */
    private boolean ignoreProductChange = false;

    /** Selection label text */
    protected static final String LABEL_SELECT = " -- Select -- ";

    /**
     * _more_
     */
    protected void productChanged() {
        stationOrProductChanged();
        // updateStatus();
    }

    /**
     * Reset the descriptor stuff
     */
    private void resetProductBox() {
        ignoreProductChange = true;
        productComboBox.setSelectedItem(LABEL_SELECT);
        ignoreProductChange = false;
    }

    /**
     * Should we update on first display
     *
     * @return true
     */
    protected boolean shouldDoUpdateOnFirstDisplay() {
        return true;
    }

    /**
     * Set the server
     *
     * @param s the server URL
     */
    private void setServer(String s) {
        datasetList = new ArrayList();
        serverUrl   = s;
        try {
            List collections = getRadarCollections(serverUrl);
            GuiUtils.setListData(collectionSelector, collections);
        } catch (Exception e) {
            GuiUtils.setListData(collectionSelector, new ArrayList());
        }
    }

    /**
     * Set the active collection
     *
     * @param s collection URL
     */
    private void setCollection(String s) {
        GuiUtils.enableComponents(level3CompsThatNeedServer, false);
        isLevel3 = false;
        GuiUtils.enableComponents(compsThatNeedServer, true);
        setAbsoluteTimes(new ArrayList());
        selectedStation = null;
        Misc.run(this, "initializeCollection", s);
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    private void setLevel3Collection(String s) {
        isLevel3 = true;
        GuiUtils.enableComponents(level3CompsThatNeedServer, true);
        GuiUtils.enableComponents(compsThatNeedServer, true);
        setAbsoluteTimes(new ArrayList());
        selectedProduct = null;
        selectedStation = null;
        Misc.run(this, "initializeLevel3Collection", s);
    }

    /**
     * Add a component that needs to have a valid server
     *
     * @param comp  the component
     *
     * @return  the component
     */
    protected JComponent addServerComp(JComponent comp) {
        compsThatNeedServer.add(comp);
        return comp;
    }

    /**
     * Add a component that needs to have a valid server
     *
     * @param comp  the component
     *
     * @return  the component
     */
    protected JComponent addLevel3ServerComp(JComponent comp) {
        level3CompsThatNeedServer.add(comp);
        return comp;
    }

    /**
     * Get  the radar collections for  the given server URL
     *
     * @param radarServerURL  server URL
     *
     * @return  a map of the collection names to URL
     */
    private List getRadarCollections(String radarServerURL) {
        SAXBuilder        builder;
        Document          doc  = null;
        XMLEntityResolver jaxp = new XMLEntityResolver(true);
        builder = jaxp.getSAXBuilder();
        List collections = new ArrayList();

        try {
            doc = builder.build(radarServerURL);
        } catch (JDOMException e) {
            userMessage("Invalid catalog");
            //e.printStackTrace();
        } catch (IOException e) {
            userMessage("Unable to open catalog");
            //e.printStackTrace();
        }

        org.jdom.Element rootElem    = doc.getRootElement();
        org.jdom.Element serviceElem = readElements(rootElem, "service");
        String           uriBase     = serviceElem.getAttributeValue("base");
        org.jdom.Element dsElem      = readElements(rootElem, "dataset");
        String           naming      = "catalogRef";
        Namespace        nss         = rootElem.getNamespace("xlink");
        List             children    = dsElem.getChildren();
        for (int j = 0; j < children.size(); j++) {
            org.jdom.Element child     = (org.jdom.Element) children.get(j);
            String           childName = child.getName();
            if (childName.equals(naming)) {
                //String id   = child.getAttributeValue("ID");
                String desc    = child.getAttributeValue("title", nss);
                String urlpath = child.getAttributeValue("href", nss);
                String[] c = radarServerURL.split(uriBase);  //.replaceFirst("catalog.xml", "");
                String         ul     = c[0] + uriBase + urlpath;
                TwoFacedObject twoObj = new TwoFacedObject(desc, ul);
                collections.add(twoObj);
                //collections.put(desc, ul);
            }

        }

        return collections;
    }

    /**
     * Read the elements
     *
     * @param elem  element
     * @param eleName element name
     *
     * @return an element
     */
    public org.jdom.Element readElements(org.jdom.Element elem,
                                         String eleName) {
        List children = elem.getChildren();
        for (int j = 0; j < children.size(); j++) {
            org.jdom.Element child     = (org.jdom.Element) children.get(j);
            String           childName = child.getName();
            if (childName.equals(eleName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Make the collection.  If there is an error, pop up a user message.
     *
     * @param url   URL for the collection
     */
    public void initializeCollection(String url) {

        List stations = new ArrayList();
        try {
            StringBuffer errlog = new StringBuffer();
            try {
                collection = TDSRadarDatasetCollection.factory("test", url,
                        errlog);
            } catch (Exception exc) {
                userMessage("Invalid catalog");

                return;
            }
            List tdsStations = collection.getRadarStations();
            for (int i = 0; i < tdsStations.size(); i++) {
                StationImpl stn = (StationImpl) tdsStations.get(i);
                // thredds.catalog.query.Location loc = stn.getLocation();
                //TODO: need better station  need to switch lat lon
                NamedStationImpl station =
                    new NamedStationImpl(stn.getName(), stn.getName(),
                                         stn.getLatitude(),
                                         stn.getLongitude(),
                                         stn.getAltitude(), CommonUnit.meter);
                stations.add(station);

            }

            getStationMap().setStations(stations);
        } catch (Exception exc) {
            userMessage("Unable to load stations");
            return;
        }
        urlListHandler.saveState(urlBox);
    }

    /**
     * _more_
     *
     * @param url _more_
     */
    public void initializeLevel3Collection(String url) {

        List          stations = new ArrayList();
        List<Product> products;

        try {
            StringBuffer errlog = new StringBuffer();
            try {
                collection = TDSRadarDatasetCollection.factory("test", url,
                        errlog);
            } catch (Exception exc) {
                userMessage("Invalid catalog");
                return;
            }
            products = collection.getRadarProducts();
            List tdsStations = collection.getRadarStations();
            for (int i = 0; i < tdsStations.size(); i++) {
                StationImpl stn = (StationImpl) tdsStations.get(i);
                // thredds.catalog.query.Location loc = stn.getLocation();
                //TODO: need better station  need to switch lat lon
                NamedStationImpl station =
                    new NamedStationImpl(stn.getName(), stn.getName(),
                                         stn.getLatitude(),
                                         stn.getLongitude(),
                                         stn.getAltitude(), CommonUnit.meter);
                stations.add(station);

            }
            List<TwoFacedObject> productNames = new ArrayList();
            for (Product product : products) {
                if ( !product.getID().contains("DPA")
                        && !product.getID().contains("NVW")) {
                    String lable = product.getName() + " (" + product.getID()
                                   + ")";
                    TwoFacedObject twoObj = new TwoFacedObject(lable,
                                                product.getID());
                    productNames.add(twoObj);
                }
            }
            GuiUtils.setListData(productComboBox, productNames);

            // GuiUtils.setListData(dataTypeComboBox, dataTypes);
            getStationMap().setStations(stations);
        } catch (Exception exc) {
            userMessage("Unable to load stations");
            return;
        }
        urlListHandler.saveState(urlBox);
    }


    /**
     * Handle when the user has selected a new station
     */
    public void stationOrProductChanged() {
        Vector times = new Vector();
        setHaveData(false);
        if ((selectedStation != null) && (selectedProduct != null)) {
            Date toDate = new Date(System.currentTimeMillis()
                                   + DateUtil.daysToMillis(1));
            //Go back 10 years (or so)
            Date fromDate = new Date(System.currentTimeMillis()
                                     - DateUtil.daysToMillis(365 * 10));
            try {
                showWaitCursor();
                setAbsoluteTimes(new ArrayList());
                setStatus("Reading times for station: " + selectedStation,
                          "");
                //                LogUtil.message("Reading times for station: "
                //                                + selectedStation);
                String pid = TwoFacedObject.getIdString(
                                 productComboBox.getSelectedItem());
                List allTimes =
                    collection.getRadarStationTimes(selectedStation.getID(),
                        pid, fromDate, toDate);
                for (int timeIdx = 0; timeIdx < allTimes.size(); timeIdx++) {
                    Object timeObj = allTimes.get(timeIdx);
                    Date   date;
                    if (timeObj instanceof Date) {
                        date = (Date) timeObj;
                    } else {
                        date = DateUnit.getStandardOrISO(timeObj.toString());
                    }
                    times.add(new DateTime(date));
                }
                //                LogUtil.message("");
                showNormalCursor();
            } catch (Exception exc) {
                userMessage("Error reading times for station: "
                            + selectedStation);
                //logException("Getting times for station: " + selectedStation,
                //             exc);
                setStatus("Select a different collection", "collections");
                showNormalCursor();
                return;
            }
        }
        setAbsoluteTimes(times);
        updateStatus();
    }





    /**
     * Load the data
     */
    public void doLoadInThread() {
        // to the CDMRadarDataSource
        Hashtable ht = new Hashtable();
        if (selectedStation != null) {
            ht.put(ucar.unidata.data.radar.RadarDataSource.STATION_LOCATION,
                   selectedStation.getNamedLocation());
        } else {
            LogUtil.userMessage("No Station selected");
        }

        if (isLevel3 && (selectedProduct == null)) {

            LogUtil.userMessage("No Product selected");
        }

        try {
            DateSelection dateSelection = new DateSelection();
            String collectionUrl = TwoFacedObject.getIdString(
                                       collectionSelector.getSelectedItem());
            String     pid = null;
            RadarQuery radarQuery;
            if (isLevel3) {
                pid = TwoFacedObject.getIdString(
                    productComboBox.getSelectedItem());
                radarQuery = new RadarQuery(collectionUrl,
                                            selectedStation.getID(), pid,
                                            dateSelection);
            } else {
                radarQuery = new RadarQuery(collectionUrl,
                                            selectedStation.getID(),
                                            dateSelection);
            }

            List urls = new ArrayList();

            if (getDoAbsoluteTimes()) {
                List times    = new ArrayList();
                List selected = makeDatedObjects(getSelectedAbsoluteTimes());
                for (int i = 0; i < selected.size(); i++) {
                    DatedThing datedThing = (DatedThing) selected.get(i);
                    Date       date       = datedThing.getDate();
                    times.add(date);
                    URI uri = null;
                    try {
                        uri = collection.getRadarDatasetURI(
                            selectedStation.getID(), pid, date);
                    } catch (Exception excp) {
                        LogUtil.userMessage("incorrect times selected");
                        return;
                    }
                    urls.add(uri.toString());
                }
                if (urls.size() == 0) {
                    LogUtil.userMessage("No times selected");
                    return;
                }
                dateSelection.setTimes(times);
            } else {
                int count = getRelativeTimesList().getSelectedIndex() + 1;
                if (count == 0) {
                    LogUtil.userMessage("No relative times selected");
                    return;
                }
                Date toDate = new Date(System.currentTimeMillis()
                                       + DateUtil.daysToMillis(365 * 100));
                //Go back 10 years (or so)
                Date fromDate = new Date(System.currentTimeMillis()
                                         - DateUtil.daysToMillis(365 * 10));

                dateSelection.setStartFixedTime(fromDate);
                dateSelection.setEndFixedTime(toDate);
                dateSelection.setCount(count);
            }
            makeDataSource(radarQuery, "FILE.RADAR", ht);
        } catch (Exception exc) {
            logException("Loading radar data", exc);
        }
    }


}

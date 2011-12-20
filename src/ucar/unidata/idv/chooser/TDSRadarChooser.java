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


import org.w3c.dom.*;

import org.w3c.dom.Element;

import thredds.catalog.XMLEntityResolver;

//import ucar.nc2.thredds.TDSRadarDatasetCollection;
import ucar.nc2.units.DateUnit;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.DataSelection;

import ucar.unidata.data.radar.RadarQuery;
import ucar.unidata.geoloc.StationImpl;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.util.*;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.Animation;

import visad.CommonUnit;
import visad.DateTime;
import visad.Set;
import visad.VisADException;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.io.IOException;

import java.net.URI;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.*;

import javax.swing.*;


/**
 * Class for choosing radar data from a THREDDS Radar Server
 * @author IDV Development Team
 */
public class TDSRadarChooser extends TimesChooser {

    /**
     * Holds the main gui contents. We keep this around so we can
     * replace it with an error message when the connection to the
     * service fails.
     */
    private JComponent outerContents;

    /** The collection */
    private IdvRadarDatasetCollection collection;

    /** The currently selected station */
    private NamedStation selectedStation;

    /** The currently selected level3 product */
    private String selectedProduct;

    /** Those urls we connect to */
    // e.g. "http://motherlode.ucar.edu:8080/thredds/radarServer/catalog.xml";
    private String serverUrl;

    /** Component to hold collections */
    private JComboBox collectionSelector;

    /** Component to hold product list */
    private JComboBox productComboBox;

    /** descriptor label */
    private JComponent productLabel;

    /** components that need a server for activation */
    private List<Component> compsThatNeedServer = new ArrayList<Component>();

    /** components that need a server for activation */
    private List<Component> level3CompsThatNeedServer =
        new ArrayList<Component>();

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

    /** is this level 3 */
    private boolean isLevel3;

    /** level 3 extension names */
    public static final String[] level3_ExName = { "NVW", "DPA" };

    /** Flag to keep from infinite looping */
    private boolean ignoreProductChange = false;

    /** Flag to keep from multiple time reading */
    private boolean readingTimes = false;


    /** Selection label text */
    protected static final String LABEL_SELECT = " -- Select -- ";

    /** the select object */
    private static final TwoFacedObject SELECT_OBJECT =
        new TwoFacedObject(LABEL_SELECT, LABEL_SELECT);

    /** _more_ */
    private boolean timeDriverEnabled = false;

    /** _more_ */
    //private Object selectedDriver = null;

    /** _more_ */
    private JComponent timesPanel;



    /**
     * Create the RadarChooser
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public TDSRadarChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }



    /**
     * Handle the update event. Just pass it through to the imageChooser
     */
    public void doUpdate() {
        if ((serverUrl == null) || (datasetList == null)
                || (datasetList.size() == 0)
                || (isLevel3 && !haveSelectedProduct())) {
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
        if (isLevel3 && ( !haveSelectedProduct())) {
            setHaveData(false);
            setStatus("Please select a level 3 product", "products");
            return;
        }
        boolean haveTimesSelected = false;
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
        timeDriverEnabled = false;
        //setDoTimeDrivers(false);
    }

    /**
     * Do we have a product selected
     *
     * @return true if we have a product and it's not the SELECT one
     */
    private boolean haveSelectedProduct() {
        if ( !isLevel3) {
            return true;
        }
        return (selectedProduct != null)
               && !selectedProduct.equals(SELECT_OBJECT);
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
        Misc.run(TDSRadarChooser.this, "stationOrProductChanged");
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
        productLabel    = addLevel3ServerComp(padLabel("Product: "));
        productComboBox = new JComboBox();
        productComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object selected = productComboBox.getSelectedItem();
                if ((selected == null) || selected.equals(SELECT_OBJECT)) {
                    selectedProduct = null;
                    return;
                }
                selectedProduct = selected.toString();
                //resetProductBox();
                productChanged();
            }

        });
        addLevel3ServerComp(productComboBox);


        JComponent stationMap = getStationMap();
        JComponent buttons    = getDefaultButtons();

        if (getIdv().getUseTimeDriver()) {
            timesPanel = makeTimesPanel(true, true, true);
        } else {
            timesPanel = makeTimesPanel(true, true);
        }


        stationMap.setPreferredSize(new Dimension(230, 200));
        stationMap = registerStatusComp("stations", stationMap);
        timesPanel = registerStatusComp("timepanel", timesPanel);
        addServerComp(stationMap);
        addServerComp(timesPanel);

        GuiUtils.tmpInsets = GRID_INSETS;
        JComponent contents = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Catalog:"), urlBox,
            GuiUtils.rLabel("Collections:"),
            GuiUtils.left(collectionSelector), GuiUtils.right(productLabel),
            GuiUtils.left(productComboBox), GuiUtils.valignLabel("Stations:"),
            stationMap, GuiUtils.valignLabel("Times:"), timesPanel
        }, 2, GuiUtils.WT_NY, new double[] { 0, 0, 0, 1, 0.2 });

        GuiUtils.enableComponents(compsThatNeedServer, false);
        GuiUtils.enableComponents(level3CompsThatNeedServer, false);
        showProductWidgets(false);
        outerContents = GuiUtils.center(GuiUtils.centerBottom(contents,
                buttons));
        return outerContents;
    }


    /**
     * The product changed
     */
    protected void productChanged() {
        Misc.run(this, "stationOrProductChanged");
    }

    /**
     * Reset the descriptor stuff
     */
    private void resetProductBox() {
        ignoreProductChange = true;
        productComboBox.setSelectedItem(SELECT_OBJECT);
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
        serverUrl   = s;
        datasetList = new ArrayList();
        try {
            datasetList = getRadarCollections(serverUrl);
            GuiUtils.setListData(collectionSelector, datasetList);
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
        showProductWidgets(false);
        isLevel3 = false;
        GuiUtils.enableComponents(compsThatNeedServer, true);
        setAbsoluteTimes(new ArrayList());
        setTimeDrivers(new ArrayList());
        selectedProduct = null;
        selectedStation = null;
        Misc.run(this, "initializeCollection", s);
    }

    /**
     * Set the level 3 collection
     *
     * @param s  the path to the collection
     */
    private void setLevel3Collection(String s) {
        isLevel3 = true;
        GuiUtils.enableComponents(level3CompsThatNeedServer, true);
        GuiUtils.enableComponents(compsThatNeedServer, true);
        setAbsoluteTimes(new ArrayList());
        setTimeDrivers(new ArrayList());
        selectedProduct = null;
        selectedStation = null;
        Misc.run(this, "initializeLevel3Collection", s);
    }

    /**
     * Show or hide the product widgets
     *
     * @param show  true to show, false to hide
     */
    private void showProductWidgets(boolean show) {
        if (productLabel != null) {
            productLabel.setEnabled(show);
            //            productLabel.setVisible(show);
        }
        if (productComboBox != null) {
            productComboBox.setEnabled(show);
            //            productComboBox.setVisible(show);
        }
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
        List<TwoFacedObject> collections = new ArrayList<TwoFacedObject>();

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

        List<NamedStationImpl> stations = new ArrayList<NamedStationImpl>();
        try {
            StringBuffer errlog = new StringBuffer();
            try {
                collection = IdvRadarDatasetCollection.factory("test", url,
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
                //                System.out.println("\"" + stn.getName() +"\",");

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
     * Initialize the Level 3 Collection
     *
     * @param url URL of the collection
     */
    public void initializeLevel3Collection(String url) {

        List<NamedStationImpl> stations = new ArrayList<NamedStationImpl>();
        List<Product>          products;
        List<String>           exProducts = new ArrayList<String>();

        for (String ename : level3_ExName) {
            exProducts.add(ename);
        }

        try {
            StringBuffer errlog = new StringBuffer();
            try {
                collection = IdvRadarDatasetCollection.factory("test", url,
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
            productComboBox.removeAllItems();
            List<TwoFacedObject> productNames =
                new ArrayList<TwoFacedObject>();
            productNames.add(SELECT_OBJECT);
            for (Product product : products) {
                // if ( !product.getID().contains("DPA")
                //       && !product.getID().contains("NVW")) {
                if ( !exProducts.contains(product.getID())) {
                    String lable = product.getName() + " (" + product.getID()
                                   + ")";
                    TwoFacedObject twoObj = new TwoFacedObject(lable,
                                                product.getID());
                    productNames.add(twoObj);
                }
            }
            GuiUtils.setListData(productComboBox, productNames);
            showProductWidgets(true);

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
        setHaveData(false);
        setDoTimeDrivers(false);
        readTimes();
        readDrivers();
        updateStatus();
    }


    /**
     *  Do what needs to be done to read in the times.  Subclasses
     *  need to implement this.
     */
    public void readTimes() {

        if (readingTimes) {
            return;
        }
        readingTimes = true;
        List<DateTime> times = new Vector<DateTime>();

        if (getDoAbsoluteTimes()) {
            // ucar.unidata.util.Trace.call1("TDSRadarChooser.readTimes");

            if (( !isLevel3 && (selectedStation != null))
                    || (isLevel3 && (selectedStation != null)
                        && (haveSelectedProduct()))) {

                List timeSpan = collection.getRadarTimeSpan();
                Date fromDate =
                    DateUnit.getStandardOrISO((String) timeSpan.get(0));
                Date toDate =
                    DateUnit.getStandardOrISO((String) timeSpan.get(1));
                try {
                    showWaitCursor();
                    setAbsoluteTimes(new ArrayList());
                    setStatus("Reading times for station: "
                              + selectedStation, "");
                    //                LogUtil.message("Reading times for station: "
                    //                                + selectedStation);
                    String pid = null;
                    if (isLevel3) {
                        pid = TwoFacedObject.getIdString(
                            productComboBox.getSelectedItem());
                    }
                    List<Date> allTimes;
                    URI uri = collection.getQueryRadarStationURI(
                                  selectedStation.getID(), pid, fromDate,
                                  toDate);


                    // Trace.call1("getXml");
                    LogUtil.message("Reading radar catalog from server");
                    String xml;
                    try {
                        xml = IOUtil.readContents(uri.toString(), getClass());
                    } finally {
                        LogUtil.message("");
                    }
                    // LogUtil.message("Processing radar catalog");
                    // Trace.call2("getXml"," xml.length=" + xml.length());

                    // Trace.call1("getRoot");
                    Element root = XmlUtil.getRoot(xml);
                    // Trace.call2("getRoot");


                    Element topDatasetNode = XmlUtil.findChild(root,
                                                 CatalogUtil.TAG_DATASET);
                    if (topDatasetNode == null) {
                        throw new IllegalStateException(
                            "Could not find dataset node in radar catalog");
                    }

                    List children = XmlUtil.findChildren(topDatasetNode,
                                        CatalogUtil.TAG_DATASET);
                    allTimes = new ArrayList<Date>();
                    if ((children == null) || children.isEmpty()) {
                        String message = "No data available";
                        if (isLevel3) {
                            message +=
                                " for "
                                + productComboBox.getSelectedItem()
                                    .toString();
                        }
                        message += " at " + selectedStation.getID();
                        userMessage(message);
                    }
                    SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
                    for (int i = 0; i < children.size(); i++) {
                        Element child = (Element) children.get(i);
                        String dttmTxt = XmlUtil.getGrandChildText(child,
                                             CatalogUtil.TAG_DATE,
                                             (String) null);
                        if (dttmTxt == null) {
                            continue;
                        }
                        Date dttm = sdf.parse(dttmTxt);
                        allTimes.add(dttm);
                    }

                    for (Date date : allTimes) {
                        times.add(new DateTime(date));
                    }

                    showNormalCursor();
                } catch (Exception exc) {
                    userMessage("Error reading times for station: "
                                + selectedStation);
                    setStatus("Select a different collection", "collections");
                    showNormalCursor();
                    readingTimes = false;
                    return;
                }
            }
            // ucar.unidata.util.Trace.call2("TDSRadarChooser.readTimes");
        }
        readingTimes = false;
        setAbsoluteTimes(times);

    }

    /**
     * _more_
     *
     * @param id _more_
     */
    public void updatetimelineOld(TwoFacedObject id) {
        super.updatetimeline(id);
        String collectionUrl =
            TwoFacedObject.getIdString(collectionSelector.getSelectedItem());
        DateSelection dateSelection = new DateSelection();
        String        pid           = null;
        RadarQuery    radarQuery;
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

        DateTime[] driverTimes = (DateTime[]) id.getId();


        List<DateTime> dtimes = new ArrayList<DateTime>();

        for (int i = 0; i < driverTimes.length; i++) {
            dtimes.add(driverTimes[i]);
        }
        Collections.sort(dtimes);
        int n = driverTimes.length;

        Date fromDate = DateUnit.getStandardOrISO(driverTimes[0].dateString()
                            + "T" + driverTimes[0].timeString());
        Date toDate =
            DateUnit.getStandardOrISO(driverTimes[n - 1].dateString() + "T"
                                      + driverTimes[n - 1].timeString());


        List collectionTimes = null;
        List selectedTimes   = null;

        try {
            collectionTimes =
                collection.getRadarStationTimes(radarQuery.getStation(),
                    radarQuery.getProduct(), fromDate, toDate);
            selectedTimes = DataUtil.selectTimesFromList(collectionTimes,
                    dtimes);

            List newAbsoluteTimes = makeDatedObjects(selectedTimes);

            int size = newAbsoluteTimes.size();
            Date endDate, startDate;
            if(size > 1) {
                endDate =
                        ((DatedThing) newAbsoluteTimes.get(size-1)).getDate();
                int index = Math.max(0, size
                                     - getNumTimesToSelect());
                startDate =
                    ((DatedThing) newAbsoluteTimes.get(index)).getDate();
                getTimeLine().setDateSelection(new Date[]{startDate, endDate});
                   
            }
            

            setSelectedAbsoluteTimes(selectedTimes);
            setDoTimeDrivers(true);
            absoluteTimesSelectionChanged();
            timeDriverEnabled = true;
        } catch (Exception e) {}

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
            ht.put(DataSelection.PROP_CHOOSERTIMEMATCHING, getDoTimeDrivers());
         /*   if (selectedDriver != null) {
                ht.put("TimeDriver", selectedDriver);
            }    */
        } else {
            LogUtil.userMessage("No Station selected");
            return;
        }

        if (isLevel3 && !haveSelectedProduct()) {

            LogUtil.userMessage("No Product selected");
            return;
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

            List<String> urls = new ArrayList<String>();

            if (getDoAbsoluteTimes()) {
                Trace.msg("TDSRadarChoocer:getting absolute times");
                List<Date> times = new ArrayList<Date>();
                List<DatedThing> selected =
                    makeDatedObjects(getSelectedAbsoluteTimes());
                for (DatedThing datedThing : selected) {
                    times.add(datedThing.getDate());
                }
                if (times.isEmpty()) {
                    LogUtil.userMessage("No times selected");
                    return;
                }
                dateSelection.setTimes(times);
                Trace.msg("TDSRadarChoocer:getting absolute times.end");
            } else {
                int count = getRelativeTimesList().getSelectedIndex() + 1;
                if (count == 0) {
                    LogUtil.userMessage("No relative times selected");
                    return;
                }
                List timeSpan = collection.getRadarTimeSpan();
                Date fromDate =
                    DateUnit.getStandardOrISO((String) timeSpan.get(0));
                Date toDate =
                    DateUnit.getStandardOrISO((String) timeSpan.get(1));
                dateSelection.setStartMode(DateSelection.TIMEMODE_DATA);
                dateSelection.setEndMode(DateSelection.TIMEMODE_DATA);
                dateSelection.setCount(count);
            }
            makeDataSource(radarQuery, "FILE.RADAR", ht);
        } catch (Exception exc) {
            logException("Loading radar data", exc);
        }
    }
}

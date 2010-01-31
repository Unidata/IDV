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


import org.w3c.dom.*;


import ucar.nc2.units.TimeDuration;

import ucar.unidata.util.*;

import ucar.unidata.xml.XmlUtil;

import visad.CommonUnit;
import visad.DateTime;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.io.IOException;

import java.net.URI;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * Chooser for TDS remote point obs collections
 */
public class TDSPointObsChooser extends TimesChooser {

    /**
     * Holds the main gui contents. We keep this around so we can
     * replace it with an error message when the connection to the
     * service fails.
     */
    private JComponent outerContents;

    /** Those urls we connect to */
    private String serverUrl;

    /** Those urls we connect to */
    private String collectionUrl;

    /** Component to hold collections */
    private JComboBox collectionSelector;

    /** components that need a server for activation */
    private List<Component> compsThatNeedServer = new ArrayList<Component>();

    /** persistent holder for catalog URLS */
    private PreferenceList urlListHandler;

    /** catalog URL holder */
    private JComboBox urlBox;

    /** ok flag */
    private boolean okToDoUrlListEvents = true;

    /** dataset list */
    private List datasetList;

    /** dataset list */
    private Element capabilitiesElement;

    /** Command for connecting */
    protected static final String CMD_CONNECT = "cmd.connect";

    /** Flag to keep from infinite looping */
    private boolean ignoreProductChange = false;

    /** Selection label text */
    protected static final String LABEL_SELECT = " -- Select -- ";

    /** the select object */
    private static final TwoFacedObject SELECT_OBJECT =
        new TwoFacedObject(LABEL_SELECT, LABEL_SELECT);

    /** the capabilities request */
    private final String REQ_CAPABILITIES = "?req=capabilities";

    /** the lat/lon box tag */
    private final String TAG_LATLONBOX = "LatLonBox";

    /** the timespan tag */
    private final String TAG_TIMESPAN = "TimeSpan";

    /** date selection */
    private DateSelection collectionDates;

    /**
     * Create the PointObsChooser
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public TDSPointObsChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }



    /**
     * Handle the update event. Just pass it through to the imageChooser
     */
    public void doUpdate() {
        if ((serverUrl == null) || (datasetList == null)
                || (datasetList.size() == 0)) {
            if (urlBox != null) {
                setServer((String) urlBox.getSelectedItem());
            }
            return;
        }
    }



    /**
     * Update the status of the gui
     */
    protected void updateStatus() {
        super.updateStatus();
        boolean haveTimesSelected = false;
        if (getDoAbsoluteTimes()) {
            haveTimesSelected = getSelectedAbsoluteTimes().size() > 0;
        } else {
            haveTimesSelected = true;
        }
        setHaveData(haveTimesSelected);
        if (haveTimesSelected) {
            setStatus(
                "Press \"" + CMD_LOAD
                + "\" to load the selected observation data", "buttons");
        } else {
            setStatus("Please select times", "timepanel");
        }
    }


    /**
     * Make the contents
     *
     * @return  the contents
     */
    protected JComponent doMakeContents() {

        //Get the list of catalogs but remove the old catalog.xml entry
        urlListHandler = getPreferenceList(PREF_TDSPOINTOBSERVER);

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
        collectionSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TwoFacedObject selected =
                    (TwoFacedObject) collectionSelector.getSelectedItem();

                if ((selected == null) || selected.equals(SELECT_OBJECT)) {
                    return;
                }
                String collectionUrl = TwoFacedObject.getIdString(selected);

                setCollection(collectionUrl);
            }

        });

        JComponent stationMap = getStationMap();
        JComponent buttons    = getDefaultButtons();
        JComponent timesPanel = makeTimesPanel(true, true);



        stationMap.setPreferredSize(new Dimension(230, 200));
        stationMap = registerStatusComp("stations", stationMap);
        timesPanel = registerStatusComp("timepanel", timesPanel);
        addServerComp(stationMap);
        addServerComp(timesPanel);

        GuiUtils.tmpInsets = GRID_INSETS;
        JComponent contents = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Catalog:"), urlBox,
            GuiUtils.rLabel("Collections:"),
            GuiUtils.left(collectionSelector),
            GuiUtils.valignLabel("Coverage:"), stationMap,
            GuiUtils.valignLabel("Times:"), timesPanel
        }, 2, GuiUtils.WT_NY, new double[] { 0, 0, 0, 1, 0.2 });

        GuiUtils.enableComponents(compsThatNeedServer, false);
        outerContents = GuiUtils.center(GuiUtils.centerBottom(contents,
                buttons));
        return outerContents;
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
            datasetList = getPointObsCollections(serverUrl);
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
        GuiUtils.enableComponents(compsThatNeedServer, true);
        setAbsoluteTimes(new ArrayList());
        Misc.run(this, "initializeCollection", s);
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
     * Should we update on first display
     *
     * @return true
     */
    protected boolean shouldDoUpdateOnFirstDisplay() {
        return true;
    }

    /**
     * Get  the radar collections for  the given server URL
     *
     * @param pointobServerURL  server URL
     *
     * @return  a map of the collection names to URL
     */
    private List getPointObsCollections(String pointobServerURL) {
        List<TwoFacedObject> collections = new ArrayList<TwoFacedObject>();
        Element              root        = null;
        try {
            root = XmlUtil.getRoot(pointobServerURL, this.getClass());
        } catch (Exception e) {
            userMessage("Unable to open catalog: " + pointobServerURL);
            //e.printStackTrace();
            return collections;
        }
        root.setAttribute(CatalogUtil.ATTR_CATALOGURL, pointobServerURL);
        List children = XmlUtil.findDescendants(root,
                            CatalogUtil.TAG_DATASET);
        collections.add(SELECT_OBJECT);
        for (int j = 0; j < children.size(); j++) {
            Element datasetNode = (Element) children.get(j);
            String  desc = datasetNode.getAttribute(CatalogUtil.ATTR_NAME);
            String urlPath =
                datasetNode.getAttribute(CatalogUtil.ATTR_URLPATH);
            Element serviceNode =
                CatalogUtil.findServiceNodeForDataset(datasetNode, true,
                    null);
            if (serviceNode == null) {
                continue;
            }
            String url = CatalogUtil.getAbsoluteUrl(serviceNode, urlPath);
            TwoFacedObject twoObj = new TwoFacedObject(desc, url);
            collections.add(twoObj);
        }

        return collections;
    }

    /**
     * Make the collection.  If there is an error, pop up a user message.
     *
     * @param url   URL for the collection
     */
    public void initializeCollection(String url) {
        Element root = null;
        try {
            root = XmlUtil.getRoot(url + REQ_CAPABILITIES, this.getClass());
            //System.out.println(XmlUtil.toString(root));
        } catch (Exception e) {
            userMessage("Unable to open collection: " + url);
            //e.printStackTrace();
            return;
        }
        collectionUrl       = url;
        capabilitiesElement = root;
        collectionChanged();
        urlListHandler.saveState(urlBox);
    }

    /**
     * Handle when the user has selected a new station
     */
    public void collectionChanged() {
        setHaveData(false);
        readTimes();
        updateStatus();
    }


    /**
     *  Do what needs to be done to read in the times.  Subclasses
     *  need to implement this.
     */
    public void readTimes() {
        List<DateTime> times = new Vector<DateTime>();
        ucar.unidata.util.Trace.call1("TDSPointObsChooser.readTimes");
        if (capabilitiesElement != null) {

            Element timeSpan = XmlUtil.getElement(capabilitiesElement,
                                   TAG_TIMESPAN);
            collectionDates = new DateSelection();

            if (timeSpan != null) {
                try {
                    String startTime = XmlUtil.getGrandChildText(timeSpan,
                                           "begin");
                    String endTime = XmlUtil.getGrandChildText(timeSpan,
                                         "end");
                    String resolution = XmlUtil.getGrandChildText(timeSpan,
                                            "resolution");
                    Date fromDate = DateUtil.parse(startTime);
                    Date toDate   = DateUtil.parse(endTime);
                    toDate          = DateUtil.min(toDate, new Date());
                    collectionDates = new DateSelection(fromDate, toDate);
                    if (resolution != null) {
                        TimeDuration td       = new TimeDuration(resolution);
                        double intervalMillis = td.getValueInSeconds() * 1000;
                        collectionDates.setInterval(intervalMillis);
                        collectionDates.setRoundTo(intervalMillis);
                    }
                    showWaitCursor();
                    setAbsoluteTimes(new ArrayList());
                    setStatus("Reading times: ", "timepanel");
                    if (getDoAbsoluteTimes()) {
                        double[] millis = collectionDates.getIntervalTicks();
                        for (int i = 0; i < millis.length; i++) {
                            times.add(new DateTime(millis[i] / 1000.));
                        }
                    }
                    showNormalCursor();
                } catch (Exception exc) {
                    exc.printStackTrace();
                    userMessage("Error reading times. ");
                    setStatus("Select a different collection", "collections");
                    showNormalCursor();
                    return;
                }
            }
        }
        ucar.unidata.util.Trace.call2("TDSPointObsChooser.readTimes");
        setAbsoluteTimes(times);

    }




    /**
     * Load the data
     */
    public void doLoadInThread() {
        Hashtable ht = new Hashtable();

        try {
            DateSelection dateSelection = new DateSelection();

            if (getDoAbsoluteTimes()) {
                Trace.msg("TDSPointObsChoocer:getting absolute times");
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
                Trace.msg("TDSPointObsChoocer:getting absolute times.end");
            } else {
                int count = getRelativeTimesList().getSelectedIndex() + 1;
                if (count == 0) {
                    LogUtil.userMessage("No relative times selected");
                    return;
                }
                dateSelection.setEndMode(DateSelection.TIMEMODE_CURRENT);
                dateSelection.setCount(count);
                if (collectionDates.hasInterval()) {
                    dateSelection.setInterval(collectionDates.getInterval());
                }
            }
            ht.put(ucar.unidata.data.DataSelection.PROP_DATESELECTION,
                   dateSelection);
            makeDataSource("cdmremote:" + collectionUrl, "CDMREMOTE.POINT",
                           ht);
        } catch (Exception exc) {
            logException("Loading radar data", exc);
        }
    }

}

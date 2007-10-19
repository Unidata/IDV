/*
 * j * $Id: IdvChooser.java,v 1.91 2007/07/27 20:59:03 jeffmc Exp $
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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;


import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceResults;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.gis.mcidasmap.McidasMap;
import ucar.unidata.gis.shapefile.EsriShapefileRenderer;
import ucar.unidata.idv.*;

import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.metdata.Station;

import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.CompositeRenderer;
import ucar.unidata.view.Renderer;
import ucar.unidata.view.station.StationLocationMap;
import ucar.unidata.xml.XmlUtil;

import visad.DateTime;

import visad.georef.EarthLocation;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;





/**
 * This is the base class of all Chooser classes.
 *
 *
 * @author IDV development team
 * @version $Revision: 1.91 $Date: 2007/07/27 20:59:03 $
 */

public abstract class IdvChooser extends ChooserPanel implements IdvConstants {

    /** Where we get the maps       */
    public static final String PROP_CHOOSER_MAPS = "idv.chooser.maps";

    /** Where we get the projection bounds       */
    public static final String PROP_CHOOSER_PROJECTION_BOUNDS =
        "idv.chooser.projection.bounds";

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(IdvChooser.class.getName());

    /**
     * Can pass in null properties. So instead of  a null or casting null
     * as a Hashtable we just define one here
     */
    public static final Hashtable NULL_PROPERTIES = null;

    /** Like above, a handy way to pass in a null string */
    public static final String NULL_STRING = null;


    /** Used by derived classes to save the last directory the user was in */
    public static final String PREF_DEFAULTDIR = "Data.DefaultDir";

    /** Saves off the pollinginfo_ */
    public static final String PREF_POLLINGINFO = "chooser.pollinginfo";

    /** Used by derived classes to save the list of catalogs */
    public static final String PREF_CATALOGLIST = "idv.data.catalog.list";

    /** Used by derived classes to save the list of adde servers */
    public static final String PREF_ADDESERVERS = "idv.data.adde.servers";

    /** Used by derived classes to save the list of image descriptors */
    public static final String PREF_IMAGEDESCLIST =
        "idv.data.adde.image.descriptors";

    /** Used by derived classes to save the list of radar descriptors */
    public static final String PREF_RADARDESCLIST =
        "idv.data.adde.radar.descriptors";

    /**
     * mapping from the String PREF name to a {@link ucar.unidata.util.PreferenceList}
     */
    private static Hashtable prefLists = new Hashtable();


    /** Name of the id attribute in the choosers.xml */
    public final static String ATTR_ID = "id";

    /** Name of the details attribute in the choosers.xml */
    public final static String ATTR_SHOWDETAILS = "showdetails";

    /** The chooser's id. Preference values are saved using the id */
    String id;


    /** Reference to the {@link IdvChooserManager} */
    IdvChooserManager chooserManager;

    /** Reference to the {@link ucar.unidata.idv.IntegratedDataViewer} */
    IntegratedDataViewer idv;

    /** Reference to the {@link ucar.unidata.data.DataManager} */
    DataManager dataManager;


    /** The xml node from choosers.xml that defines this chooser */
    Element chooserNode;


    /** Different subclasses can use the combobox of data source ids */
    private JComboBox dataSourcesCbx;


    /** This is the list data source ids that can be file based */
    //    private List fileDataSources;


    /**
     * Have we painted ourselves at least once. We keep track of this so
     * the choosers can initialize themselves the first time they are
     * displayed.
     */
    boolean hasContentsBeenPainted = false;



    /** For the dir history */
    protected boolean ignoreDirHistory = false;

    /** For the dir history */
    protected JComboBox dirHistoryCbx;



    /** Autocreate display checkbox */
    JCheckBox autoCreateDisplayCbx;


    /** station location map */
    protected StationLocationMap stationMap;

    /** selected stations */
    private List selectedStations = new ArrayList();



    /**
     *  Create the chooser
     *
     * @param idv The IDV
     * @param chooserNode The Xml
     *
     */
    public IdvChooser(IntegratedDataViewer idv, Element chooserNode) {
        this(idv, null, chooserNode);
    }


    /**
     *  Create the chooser
     *
     * @param chooserManager The manager of all of the choosers
     * @param chooserNode The Xml
     */

    public IdvChooser(IdvChooserManager chooserManager, Element chooserNode) {
        this(chooserManager.getIdv(), chooserManager, chooserNode);
    }

    /**
     *  Create the chooser
     *
     * @param idv The IDV
     * @param chooserManager The manager of all of the choosers
     * @param chooserNode The Xml
     */
    public IdvChooser(IntegratedDataViewer idv,
                      IdvChooserManager chooserManager, Element chooserNode) {
        this.chooserNode    = chooserNode;
        this.chooserManager = chooserManager;
        this.idv            = idv;
        this.dataManager    = idv.getDataManager();
        if (chooserNode != null) {
            id = XmlUtil.getAttribute(chooserNode, ATTR_ID,
                                      getClass().getName());
        } else {
            id = "noid";
        }

        init();

    }


    /**
     * Add extra components to "decorate" the button panel
     *
     * @param buttons  button panel
     *
     * @return decorated buttons
     */
    public JComponent decorateButtons(JComponent buttons) {
        String displayType = getDefaultDisplayType();
        if (displayType != null) {
            autoCreateDisplayCbx = new JCheckBox("Create display",
                    idv.getStore().get("idv." + getId() + ".autocreate",
                                       true));
            autoCreateDisplayCbx.setToolTipText(
                "Automatically create a display when data is loaded");
            buttons = GuiUtils.flow(new Component[] { buttons,
                    autoCreateDisplayCbx });
        }
        return buttons;
    }


    /**
     * Get the default buttons. (Override the default method)
     *
     * @param listener Listener for the buttons
     *
     * @return buttons plus class specific addons
     */
    public JComponent getDefaultButtons(ActionListener listener) {
        JComponent buttons = super.getDefaultButtons(listener);
        return decorateButtons(buttons);
    }

    /**
     * Get the default display type
     *
     * @return  the name of the default display
     */
    protected String getDefaultDisplayType() {
        return null;
    }

    /**
     * Get the properties from the datasource
     *
     * @param ht  a Hashtable of properties
     */
    protected void getDataSourceProperties(Hashtable ht) {
        if (autoCreateDisplayCbx != null) {
            idv.getStore().put("idv." + getId() + ".autocreate",
                               autoCreateDisplayCbx.isSelected());
            if (autoCreateDisplayCbx.isSelected()) {
                String displayType = getDefaultDisplayType();
                ht.put(DataSource.PROP_AUTOCREATEDISPLAY, displayType);
            }
        }
    }




    /**
     * Get the default map to be used. For now needs to be a mcidas map.
     *
     * @return Default map.
     */
    protected String getDefaultMap() {
        return "/auxdata/maps/OUTLUSAM";
    }

    /**
     * Get the default map to be used. For now needs to be a mcidas map.
     *
     * @return Default map.
     */
    protected List getDefaultMaps() {
        String maps = getProperty(PROP_CHOOSER_MAPS,
                                  "/auxdata/maps/OUTLUSAM");
        return StringUtil.split(maps, ";", true, true);
    }


    /**
     * Initialize the stations
     *
     * @param stationMap The station map
     */
    protected void initStationMap(StationLocationMap stationMap) {
        List              maps     = getDefaultMaps();
        CompositeRenderer renderer = new CompositeRenderer();
        for (int i = 0; i < maps.size(); i++) {
            String   map = (String) maps.get(i);
            Renderer mmr;
            if (map.toLowerCase().endsWith(".shp")
                    || map.toLowerCase().endsWith(".zip")) {
                try {
                    mmr = EsriShapefileRenderer.factory(
                        IOUtil.getInputStream(map, getClass()));
                } catch (Exception exc) {
                    System.err.println("Failed to read map:" + map + "\n"
                                       + exc);
                    continue;
                }
                if (mmr == null) {
                    System.err.println("Failed to read map:" + map);
                    continue;
                }
            } else {
                mmr = new McidasMap(map);
            }

            mmr.setColor(MAP_COLOR);
            renderer.addRenderer(mmr);
        }

        stationMap.setMapRenderer(renderer);
        stationMap.setProjectionImpl(getDefaultProjection());
    }




    /**
     * Create the station map
     *
     * @return The new station map.
     */
    protected StationLocationMap createStationMap() {
        StationLocationMap map = new StationLocationMap() {
            public void setDeclutter(boolean declutter) {
                super.setDeclutter(declutter);
                updateStatus();
            }
        };
        return map;
    }

    /**
     * Is this chooser using the station map
     *
     * @return Using stations.
     */
    protected boolean usingStations() {
        return stationMap != null;
    }

    /**
     * If there is a station map created then return whether there are any
     * stations selected.
     *
     * @return Any stations selected.
     */
    protected boolean haveStationSelected() {
        if ((selectedStations != null) && (selectedStations.size() > 0)) {
            return true;
        }
        return false;
    }


    /**
     * Notify of new selected stations.
     *
     * @param stations Selected stations.
     */
    protected void newSelectedStations(List stations) {
        selectedStations = new ArrayList(stations);
        updateStatus();
    }



    /**
     * Create (if needed) and return the station map
     *
     * @return The station map
     */
    protected StationLocationMap getStationMap() {
        if (stationMap == null) {
            stationMap = createStationMap();
            registerStatusComp("stationmap",stationMap);
            initStationMap(stationMap);
            stationMap.setPreferredSize(new Dimension(300, 250));
            stationMap.addPropertyChangeListener(
                new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    newSelectedStations(
                        (List) (stationMap.getSelectedStations()));
                }
            });
        }
        return stationMap;
    }

    /**
     * Get list of selected stations, objects of
     * ucar/unidata/metdata/NamedStationImpl class
     *
     * @return list of selected stations
     */
    public List getSelectedStations() {
        return selectedStations;
    }



    /**
     * Get the first seleted station or null if none selected.
     *
     * @return Get a selected station.
     */
    protected String getSelectedStation() {
        List stations = getSelectedStations();
        if (stations.size() == 0) {
            return null;
        }
        return ((Station) stations.get(0)).getIdentifier();
    }



    /**
     * Clear any selected stations.
     */
    protected void clearSelectedStations() {
        selectedStations = new ArrayList();
        if (stationMap != null) {
            stationMap.setSelectedStations(selectedStations);
        }
    }


    /**
     * Clear all stations in the station map.
     */
    protected void clearStations() {
        if (stationMap != null) {
            stationMap.setStations(new ArrayList());
        }
    }


    /**
     * Create and return the default projection used for the station map.
     *
     * @return _Default map projection
     */
    protected ProjectionImpl getDefaultProjection() {
        String prop = getProperty(PROP_CHOOSER_PROJECTION_BOUNDS,
                                  "40,-100,60,20");
        List toks = StringUtil.split(prop, ",", true, true);
        if (toks.size() != 4) {
            System.err.println("Bad property value for:"
                               + PROP_CHOOSER_PROJECTION_BOUNDS + " " + prop);
            toks = StringUtil.split("40,-100,60,20", ",", true, true);
        }
        LambertConformal proj =
            new LambertConformal(
                new Integer(toks.get(0).toString()).intValue(),
                new Integer(toks.get(1).toString()).intValue(),
                new Integer(toks.get(2).toString()).intValue(),
                new Integer(toks.get(3).toString()).intValue());
        proj.setDefaultMapArea(getDefaultProjectionRect());
        return proj;
    }

    /**
     * Create and return the default projection rect used for the station map.
     *
     * @return Default map projection rectangle
     */
    protected ProjectionRect getDefaultProjectionRect() {
        return new ProjectionRect(-2000, -1800, 2500, 1800);
    }



    /**
     * Make time menus
     *
     * @param listener the listener for this
     *
     * @return  a list of options for times from each display
     */
    public List makeTimeMenus(final ActionListener listener) {
        List menus = new ArrayList();
        List vms   = getIdv().getVMManager().getViewManagers();
        try {
            int cnt = 0;
            for (int i = 0; i < vms.size(); i++) {
                ViewManager      vm    = (ViewManager) vms.get(i);
                final DateTime[] times = vm.getAnimationTimes();
                if ((times == null) || (times.length == 0)) {
                    continue;
                }
                String name = vm.getName();
                if ((name == null) || (name.trim().length() == 0)) {
                    name = "Display " + (++cnt);
                }
                JMenuItem mi = new JMenuItem("Times from " + name);
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        listener.actionPerformed(new ActionEvent(times, 1,
                                "times"));
                    }
                });

                menus.add(mi);
            }
        } catch (Exception exc) {
            logException("Making center menu", exc);
        }
        return menus;
    }




    /**
     * Get the xml node that defined this chooser
     *
     * @return The chooser node
     */
    public Element getXmlNode() {
        return chooserNode;
    }

    /**
     * Utility to get the attribute from the chooser  xml node. If there is one
     * we'll also apply the global macros
     *
     * @param name attr name
     * @param dflt default value
     *
     * @return attribute
     */
    protected String getAttribute(String name, String dflt) {
        String value = XmlUtil.getAttribute(chooserNode, name, (String) null);
        if (value == null) {
            return dflt;
        }
        return idv.getStateManager().applyMacros(value);
    }

    /**
     * Utility to get a String property
     *
     * @param name property name
     * @param dflt default value
     *
     * @return property or default
     */
    public String getProperty(String name, String dflt) {
        String retVal = idv.getProperty("idv." + getId() + "." + name, null);
        if (retVal == null) {
            retVal = idv.getProperty(name, dflt);
        }
        if (retVal == null) {
            retVal = XmlUtil.getAttribute(chooserNode, name, dflt);
        }
        return retVal;
    }


    /**
     * Utility to get a boolean property
     *
     * @param name property name
     * @param dflt default value
     *
     * @return property or default
     */
    public boolean getProperty(String name, boolean dflt) {
        boolean hasIt  = XmlUtil.hasAttribute(chooserNode, name);
        boolean retVal = dflt;
        if (hasIt) {
            retVal = XmlUtil.getAttribute(chooserNode, name, dflt);
        } else {
            retVal = idv.getProperty("idv." + getId() + "." + name, dflt);
        }
        return retVal;
    }

    /**
     * Should this chooser entry be shown in the file menu
     *
     * @return Show this chooser in the file menu?
     */
    public boolean getShowInMenu() {
        return XmlUtil.getAttribute(chooserNode,
                                    IdvChooserManager.ATTR_SHOWINMENU, true);
    }

    /**
     * Get the title to use for menu entries.
     *
     * @return Menu title
     */
    public String getMenuTitle() {
        String title = XmlUtil.getAttribute(chooserNode,
                                            IdvChooserManager.ATTR_MENUTITLE,
                                            (String) null);
        if (title != null) {
            return title;
        }
        return getTitle();
    }


    /**
     * Get the title
     *
     * @return The title
     */
    public String getTitle() {
        return XmlUtil.getAttribute(chooserNode,
                                    IdvChooserManager.ATTR_TITLE, "Chooser");
    }

    /**
     * Initialize the given panel. Set the help path on it.
     *
     * @param chooserPanel The panel to initialize.
     */
    protected void initChooserPanel(ChooserPanel chooserPanel) {
        chooserPanel.setHelpPath(helpPath);
    }


    /**
     * Return the component that holds the dataSources combo box
     *
     * @return The GUI for the data sources selector
     */
    protected JComboBox getDataSourcesComponent() {
        return getDataSourcesComponent(true);

    }

    /**
     * Return the component that holds the dataSources combo box
     *
     * @param justFileSources If true then just use data sources that access files
     * @return The GUI for the data sources selector
     */
    protected JComboBox getDataSourcesComponent(boolean justFileSources) {
        if (dataSourcesCbx == null) {
            dataSourcesCbx = getDataSourcesComponent(justFileSources,
                    getDataManager());
        }
        return dataSourcesCbx;
    }



    /**
     * Return the component that holds the dataSources combo box
     *
     * @param justFileSources If true then just use data sources that access files
     * @param dataManager The data manager
     * @return The GUI for the data sources selector
     */
    public static JComboBox getDataSourcesComponent(boolean justFileSources,
            DataManager dataManager) {
        return getDataSourcesComponent(justFileSources, dataManager, true);
    }

    /**
     * Get the component for listing data source types
     *
     * @param justFileSources  true for just files
     * @param dataManager  the data manager
     * @param addLucky  true to add the "I'm Feeiling Lucky" option
     *
     * @return the component
     */
    public static JComboBox getDataSourcesComponent(boolean justFileSources,
            DataManager dataManager, boolean addLucky) {
        JComboBox dataSourcesCbx  = new JComboBox();
        List      dataSources     = (addLucky
                                     ? Misc.newList("I'm Feeling Lucky")
                                     : (List) new ArrayList());
        List      fileDataSources = (justFileSources
                                     ? dataManager.getFileDataSourceList()
                                     : (List) dataManager
                                         .getAllDataSourceIds());
        dataSources.addAll(fileDataSources);
        GuiUtils.setListData(dataSourcesCbx, dataSources);
        dataSourcesCbx.setToolTipText(
            "<html>Optional way to specifically select<br>the type of datasource.</html>");
        return dataSourcesCbx;
    }

    /**
     * If the dataSources combo box is non-null then
     * return the data source id the user selected.
     * Else, return null
     *
     * @return Data source id
     */
    protected String getDataSourceId() {
        return getDataSourceId(dataSourcesCbx);
    }

    /**
     * Get the data source ID for the particular item selected in the box
     *
     * @param dataSourcesCbx  the list of source descriptions
     *
     * @return  the id of the selected data source type
     */
    protected String getDataSourceId(JComboBox dataSourcesCbx) {
        if (dataSourcesCbx == null) {
            return null;
        }
        Object selected = dataSourcesCbx.getSelectedItem();
        if ((selected != null) && (selected instanceof TwoFacedObject)) {
            return (String) ((TwoFacedObject) selected).getId();
        }
        return null;
    }

    /**
     * Clear the dataSources combo box
     */
    protected void resetDataSourceId() {
        if (dataSourcesCbx != null) {
            dataSourcesCbx.setSelectedIndex(0);
        }
    }


    /**
     * Create the data source defined by the given definingObject
     * (e.g., a string filename, a list of images, etc.
     *
     * @param definingObject What defines the data source
     * @param properties extra properties
     * @return Was this creation successful
     */
    protected boolean makeDataSource(Object definingObject,
                                     Hashtable properties) {
        return makeDataSource(definingObject, null, properties);
    }

    /**
     * Create the data source defined by the given definingObject
     * (ex: a string filename, a list of images).
     * If dataType is non-null then it defines a data source
     * id (defined in idv/resources/datasources.xml)
     * <p>Else the creation is done by looking at the string
     * value of the defining object and seeing if it matches
     * any of the patterns that are defined in datasources.xml
     * <p>One more way to define what data source to create
     * is to add the id into the properties Hashable
     * using {@link ucar.unidata.data.DataManager#DATATYPE_ID}
     *
     * @param definingObject What defines the data source
     * @param dataType Define the data type to create
     * @param properties extra properties
     * @return Was this creation successful
     */

    protected boolean makeDataSource(Object definingObject, String dataType,
                                     Hashtable properties) {
        showWaitCursor();
        boolean result = idv.makeDataSource(definingObject, dataType,
                                            properties);
        showNormalCursor();
        if (result) {
            closeChooser();
            idv.getIdvUIManager().showDataSelector();
        }
        return result;
    }


    protected Object startTask() {
        Object taskId = JobManager.getManager().startLoad("chooser");
        return taskId;
    }

    protected void stopTask(Object taskId) {
        JobManager.getManager().stopLoad(taskId);
    }

    protected boolean stopTaskAndIsOk(Object taskId) {
        boolean ok =  JobManager.getManager().canContinue(taskId);
        stopTask(taskId);
        return ok;
    }

    protected boolean taskOk(Object taskId) {
        return JobManager.getManager().canContinue(taskId);
    }


    /**
     * show wait cursor
     */
    public void showWaitCursor() {
        super.showWaitCursor();
        idv.getIdvUIManager().showWaitCursor();
        if(cancelButton!=null) cancelButton.setEnabled(true);
    }

    /**
     * show regular cursor
     */
    public void showNormalCursor() {
        super.showNormalCursor();
        idv.getIdvUIManager().showNormalCursor();
        if(cancelButton!=null) cancelButton.setEnabled(false);
    }

    /**
     * Close the chooser window
     */
    public void doClose() {
        closeChooser();
    }

    public void doCancel() {
        super.doCancel();
        getIdv().clearWaitCursor();
        if(cancelButton!=null) cancelButton.setEnabled(false);
        updateStatus();
    }


    /**
     * Close the chooser window
     */
    protected void closeChooser() {
        if (chooserManager != null) {
            chooserManager.closeDataChooser();
        }
    }

    /**
     * Initialize after creation
     */
    protected void init() {}


    /**
     * This is called when the contents are painted. If this is
     * the first time they have been painted the chooser
     * shouldDoUpdateOnFirstDisplay then a thread is created and
     * the doUpdate method is called.
     */
    private void contentsPainted() {
        //        if (true) return;
        if (hasContentsBeenPainted) {
            return;
        }
        hasContentsBeenPainted = true;
        if (shouldDoUpdateOnFirstDisplay()) {
            //Do this in a separate thread.
            Misc.run(this, "doUpdate");
        }
    }

    /**
     *  As the name implies should this chooser do call doUpdate the first
     * time it is painted. This is a hook for derived classes to specify this behavior.
     *
     * @return Should update
     */
    protected boolean shouldDoUpdateOnFirstDisplay() {
        return false;
    }


    /**
     * Create and return the GUI contents.
     *
     * @return The GUI
     */
    public final JComponent getContents() {
        if (contents == null) {
            Component innerContents = doMakeContents();
            //            innerContents = GuiUtils.topCenter(getStatusComponent(),
            //                                               innerContents);
            //            setStatus(" ");

            JPanel    topPanel      = new JPanel(new BorderLayout()) {
                public void paint(Graphics g) {
                    contentsPainted();
                    super.paint(g);
                }
            };
            topPanel.add("Center", innerContents);
            this.setLayout(new BorderLayout());
            this.add(BorderLayout.CENTER, topPanel);
            //            contents = topPanel;
            contents = this;
            updateStatus();
        }
        return contents;
    }

    protected void updateStatus() {
        super.updateStatus();
        if(getHaveData()) {
            setStatus("Press \"" + CMD_LOAD + "\" to load the selected data", "buttons");
        }
    }


    /**
     * Return the {@link ucar.unidata.data.DataManager}
     *
     * @return The data manager
     */
    public DataManager getDataManager() {
        return dataManager;
    }


    /**
     *  Return the id
     *
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * Getter method to return the IDV
     *
     * @return The idv
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }

    /**
     *  Helper method that calls LogUtil.printException
     *
     * @param msg The message
     * @param exc The exception
     */
    public void logException(String msg, Exception exc) {
        LogUtil.printException(log_, msg, exc);
    }

    /**
     *  Helper method to show a user error message
     *
     * @param msg The message
     */
    public static void errorMessage(String msg) {
        LogUtil.userErrorMessage(log_, msg);
    }

    /**
     *  Helper method to show a user  message
     *
     * @param msg The message
     */
    public static void userMessage(String msg) {
        LogUtil.userMessage(msg);
    }


    /**
     * Find the {@link ucar.unidata.util.PreferenceList} associated
     * with the given property.
     *
     * @param listProp The property name
     * @return The PreferenceList
     */
    public PreferenceList getPreferenceList(String listProp) {
        boolean merge = idv.getProperty(listProp + ".merge", false);
        return getPreferenceList(listProp, merge,
                                 StringUtil.split(idv.getProperty(listProp
                                     + ".delete", null), ";"));
    }


    /**
     * Find the {@link ucar.unidata.util.PreferenceList} associated
     * with the given property.
     *
     * @param listProp The property name
     * @param mergeTheIdvProperty If true, then we also lookup the String
     *                            property from the IDV and merge it into
     *                            the list.
     * @return The PreferenceList
     */
    public PreferenceList getPreferenceList(String listProp,
                                            boolean mergeTheIdvProperty) {
        return getPreferenceList(listProp, mergeTheIdvProperty,
                                 StringUtil.split(idv.getProperty(listProp
                                     + ".delete", null), ";"));
    }

    /**
     * Find the {@link ucar.unidata.util.PreferenceList} associated
     * with the given listProp.
     *
     * @param listProp             The property key
     * @param mergeTheIdvProperty  If true, then we also lookup
     *                             the String property
     *                             from the IDV and merge it into the list.
     * @param butNotThisOne        If non-null then we don't include this
     *                             property. This is used to remove old
     *                             catalogs from the XmlChooser's list
     * @return The PreferenceList
     */
    public PreferenceList getPreferenceList(String listProp,
                                            boolean mergeTheIdvProperty,
                                            String butNotThisOne) {
        return getPreferenceList(listProp, mergeTheIdvProperty,
                                 (butNotThisOne != null)
                                 ? Misc.newList(butNotThisOne)
                                 : null);
    }

    /**
     * Find the {@link ucar.unidata.util.PreferenceList} associated
     * with the given listProp.
     *
     * @param listProp             The property key
     * @param mergeTheIdvProperty  If true, then we also lookup
     *                             the String property
     *                             from the IDV and merge it into the list.
     * @param butNotThese          If non-null or empty then we don't include
     *                             these properties. This is used to remove old
     *                             values from the list
     * @return The PreferenceList
     */
    public PreferenceList getPreferenceList(String listProp,
                                            boolean mergeTheIdvProperty,
                                            List butNotThese) {
        synchronized (prefLists) {
            String         chosenProp = listProp + ".chosen";
            PreferenceList prefList =
                (PreferenceList) prefLists.get(listProp);
            if (prefList == null) {
                List propertyList = getPropList(listProp,
                                        mergeTheIdvProperty);
                Object chosenProperty = idv.getStore().get(chosenProp);
                if ((butNotThese != null) && !butNotThese.isEmpty()) {
                    for (Iterator iter = butNotThese.iterator();
                            iter.hasNext(); ) {
                        String butNotThisOne = (String) iter.next();
                        if (butNotThisOne == null) {
                            continue;
                        }

                        if (Misc.equals(butNotThisOne, chosenProperty)) {
                            chosenProperty = null;
                        }
                        if (propertyList != null) {
                            propertyList.remove(butNotThisOne);
                        }
                    }
                }
                prefList = new PreferenceList(propertyList, chosenProperty,
                        listProp, chosenProp, idv.getStore());
                prefLists.put(listProp, prefList);
            }
            return prefList;
        }
    }



    /**
     * Find the list (of Strings) defined by the property name
     *
     * @param property The property name
     * @param mergeTheIdvProperty If true, then we also lookup the String property
     *                             from the IDV and merge it into the list.
     * @return Property list
     */
    public List getPropList(String property, boolean mergeTheIdvProperty) {
        Object fromPrefs = idv.getStore().get(property);
        List   prefList  = null;
        if ((fromPrefs != null) && (fromPrefs instanceof List)
                && ((List) fromPrefs).size() > 0) {
            prefList = new ArrayList((List) fromPrefs);
            if ( !mergeTheIdvProperty) {
                return prefList;
            }
        }
        List propertyList = StringUtil.split(idv.getProperty(property, ""),
                                             ";", true, true);
        if ( !mergeTheIdvProperty || (prefList == null)) {
            return propertyList;
        }

        for (int i = 0; i < propertyList.size(); i++) {
            Object prop = propertyList.get(i);
            if ( !prefList.contains(prop)) {
                prefList.add(prop);
            }
        }
        return prefList;

    }


}


/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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
import ucar.unidata.gis.maps.LatLonData;
import ucar.unidata.gis.maps.LatLonLabelData;
import ucar.unidata.gis.maps.MapData;
import ucar.unidata.gis.maps.MapInfo;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.ui.LatLonLabelPanel;
import ucar.unidata.ui.LatLonPanel;
import ucar.unidata.ui.MapPanel;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.Trace;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.Displayable;
import ucar.visad.display.LatLonLabels;
import ucar.visad.display.MapLines;

import visad.SampledSet;
import visad.VisADException;

import visad.georef.EarthLocation;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;




/**
 * A control for displaying map lines.
 *
 * @author IDV Development Team
 * @version  $Revision: 1.95 $
 */
public class MapDisplayControl extends DisplayControlImpl {


    /** default map color */
    public static final Color DEFAULT_MAP_COLOR = new Color(0, 204, 0);


    /** Where we put the map guis */
    private JPanel contents;

    /**
     * Set by the map xml, holds the level of the map used in the
     * positionSlider
     */
    private double mapPosition = Double.NaN;


    /**
     * List of MapState objects
     */
    private List<MapState> mapStates = new ArrayList<MapState>();


    /** Holds the latitude display info */
    private LatLonState latState = null;

    /** Holds the longitude display info */
    private LatLonState lonState = null;

    /** Holds the latitude label info */
    private LatLonLabelState latLabelState = null;

    /** Holds the longitude label info */
    private LatLonLabelState lonLabelState = null;


    /**
     * The initialMap - set by the property in the controls.xml
     */
    private String initialMap;

    /**
     * The description of the initialMap
     */
    private String initialMapDescription;


    /**
     * Holds the mapsHolder and the latLonHolder
     */
    private CompositeDisplayable theHolder;


    /**
     * This holds the map displayables
     */
    private CompositeDisplayable mapsHolder;

    /**
     * This holds the latlon displayables
     */
    private CompositeDisplayable latLonHolder;



    /**
     * Is this map display the one used for the default map of a view manager.
     */
    private boolean isDefaultMap = false;

    /**
     * A flag that gets set when the user creates the 'default map'
     * This flag tells the MapDisplay to initialize itself with the user's
     * default maps.
     */
    private boolean initializeAsDefault = false;

    /**
     * This holds the list of default  map data objects when this display control
     *  is used as the default map in a map view.
     */
    private List defaultMapData;

    /**
     * This holds the default lat. data  when this display control
     *  is used as the default map in a map view.
     */
    private LatLonData defaultLatData;

    /**
     * This holds the default long. data  when this display control
     *  is used as the default map in a map view.
     */
    private LatLonData defaultLonData;

    /**
     * This holds the default lat. data  when this display control
     *  is used as the default map in a map view.
     */
    private LatLonLabelData defaultLatLabelData;

    /**
     * This holds the default long. data  when this display control
     *  is used as the default map in a map view.
     */
    private LatLonLabelData defaultLonLabelData;

    /**
     * Have this around when we are initializing.
     * It tells us whether to not include maps in the map list
     * that are not visible
     */
    private boolean ignoreNonVisibleMaps = true;


    /**
     * Have this around when we are initializing.
     * It tells us whether to not to show this in the display list
     */
    private boolean myShowInDisplayList = false;


    /**
     * position slider
     */
    private ZSlider levelSlider = null;

    /** flag for slider events */
    private boolean ignoreSliderEvents = false;

    /** static counter */
    static int cnt = 0;

    /** instance for this instantiation */
    int mycnt = cnt++;

    /** The link button */
    private JToggleButton applyToAllMapsBtn;

    /** apply to all lat/lon button */
    private JToggleButton applyToAllLatLonBtn;

    /** apply to all lat/lon labels button */
    private JToggleButton applyToAllLabelsBtn;

    /** Do we apply the changes to all the maps */
    private boolean applyChangesToAllMaps = false;

    /** Do we apply the changes to all the lats and lons */
    private boolean applyChangesToAllLatLon = true;

    /** Do we apply the changes to all the lat/lon labels */
    private boolean applyChangesToAllLabels = true;

    /** Are we currently updating the other maps */
    private boolean updatingOtherMapStates = false;


    /**
     * Default Constructor.
     */
    public MapDisplayControl() {
        setLockVisibilityToggle(true);
    }


    /**
     * Special constructor  for creating a map display for a particular
     * MapViewManager
     *
     * @param mapViewManager   The map view manager this map display is
     *                         the default map for
     * @param mapInfo          Holds the map info
     */
    public MapDisplayControl(MapViewManager mapViewManager, MapInfo mapInfo) {
        super(mapViewManager.getIdv());
        setLockVisibilityToggle(true);
        defaultViewManager       = mapViewManager;
        this.mapPosition         = getInitialZPosition();
        this.defaultMapData      = mapInfo.getMapDataList();
        this.defaultLatData      = mapInfo.getLatData();
        this.defaultLonData      = mapInfo.getLonData();
        this.defaultLatLabelData = mapInfo.getLatLabelData();
        this.defaultLonLabelData = mapInfo.getLonLabelData();
        if (mapInfo.getJustLoadedLocalMaps()) {
            ignoreNonVisibleMaps = false;
        }
        if (mapViewManager.getUseGlobeDisplay()) {
            defaultLatLabelData.setSphere(true);
            defaultLonLabelData.setSphere(true);
        }
    }



    /**
     * Special constructor  for creating a map display for a particular
     * MapData
     *
     * @param mapData The mapData  may be null.  If null, then the user
     *                is prompted to choose a map.
     */
    public MapDisplayControl(MapData mapData) {
        setLockVisibilityToggle(true);
        this.defaultMapData = (mapData == null)
                              ? new ArrayList()
                              : Misc.newList(mapData);
    }



    /**
     * Should we add a control listener
     *
     * @return  true if we should
     */
    protected boolean xxxshouldAddControlListener() {
        return true;
    }

    /** last width in degrees */
    double lastWidthDegrees = -1;

    /**
     * Handle the viewpoint changed
     */
    public void xxxviewpointChanged() {
        super.viewpointChanged();
        try {
            if ((latState == null) || (lonState == null)) {
                return;
            }
            NavigatedDisplay navDisplay = getNavigatedDisplay();
            if ((navDisplay == null) || (navDisplay.getDisplay() == null)) {
                return;
            }
            Rectangle     screenBounds = navDisplay.getScreenBounds();

            EarthLocation p1           = screenToEarth(0, 0);
            EarthLocation p2           = screenToEarth(screenBounds.width, 0);
            double        diff = Math.abs(p1.getLongitude().getValue()
                                   - p2.getLongitude().getValue());

            if (diff != lastWidthDegrees) {
                lastWidthDegrees = diff;
                System.err.println("setting spacing:");
                latState.okToShare = false;
                lonState.okToShare = false;
                latState.setSpacing((float) (lastWidthDegrees / 10));
                lonState.setSpacing((float) (lastWidthDegrees / 10));
                latState.okToShare = true;
                lonState.okToShare = true;
            }
        } catch (Exception exc) {
            logException("Viewpoint changed", exc);
        }
    }

    /**
     * Append any label information to the list of labels.
     *
     * @param labels   in/out list of labels
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        //        System.err.println ("getLegendLabels " + mapStates.size());
        super.getLegendLabels(labels, legendType);
        int cnt = 0;
        int i   = 0;

        for (; i < mapStates.size(); i++) {
            MapState mapState = (MapState) mapStates.get(i);
            if ( !mapState.getVisible()) {
                continue;
            }
            labels.add(mapState.getDescription());
            //Only put in three maps
            if (cnt++ > 2) {
                if (i < mapStates.size() - 1) {
                    labels.add("...");
                }
                break;
            }
        }
    }

    /**
     * Clear the current state and copy the state held by the given newMap
     *
     * @param newMap The map display we copy from
     */
    public void loadNewMap(MapDisplayControl newMap) {
        setDisplayInactive();
        try {
            setLegendLabelTemplate(newMap.getLegendLabelTemplate());
            setDisplayListTemplate(newMap.getDisplayListTemplate());
            setShowInDisplayList(newMap.getShowInDisplayList());
            if (newMap.getDisplayListColor() != null) {
                setDisplayListColor(newMap.getDisplayListColor());
            }

            //Use the collapsed/expanded state from the new map
            setCollapseLegend(newMap.getCollapseLegend());
            deactivateDisplays();
            this.latState.initWith(newMap.latState);
            this.lonState.initWith(newMap.lonState);
            this.latLabelState.initWith(newMap.latLabelState);
            this.lonLabelState.initWith(newMap.lonLabelState);
            if (getDisplayVisibility() != newMap.getDisplayVisibility()) {
                setDisplayVisibility(newMap.getDisplayVisibility());
            }

            for (MapState mapState : mapStates) {
                Displayable theMap = mapState.getMap();
                if (theMap != null) {
                    mapsHolder.removeDisplayable(theMap);
                }
            }

            if (latLonHolder != null) {
                latLonHolder.clearDisplayables();
            } else {
                latLonHolder = new CompositeDisplayable("latlonholder "
                        + mycnt);
                theHolder.addDisplayable(latLonHolder);
            }
            latLonHolder.addDisplayable(latState.getLatLonLines());
            latLonHolder.addDisplayable(lonState.getLatLonLines());
            latLonHolder.addDisplayable(latLabelState.getLatLonLabels());
            latLonHolder.addDisplayable(lonLabelState.getLatLonLabels());


            this.mapStates = new ArrayList<MapState>();
            for (int i = 0; i < newMap.mapStates.size(); i++) {
                MapState mapState =
                    new MapState((MapData) newMap.mapStates.get(i));
                if (mapState.init(this)) {
                    mapStates.add(mapState);
                }
            }


            this.mapPosition = newMap.mapPosition;

            if (contents != null) {
                fillContents();
            }

            setSliderPosition();
            applyMapPosition();
            activateDisplays();
            updateLegendLabel();
            if (newMap.getComponentHolder() != null) {
                if (this.getComponentHolder() != null) {
                    this.getComponentHolder().setObject(null);
                    this.getComponentHolder().doRemove();
                }
                this.setComponentHolder(newMap.getComponentHolder());
                newMap.setComponentHolder(null);
                this.getComponentHolder().setObject(this);
                this.getComponentHolder().displayControlHasInitialized();
            }
        } catch (Exception exc) {
            logException("Loading new map", exc);
        }
        setDisplayActive();
    }


    /**
     * Called to make this kind of Display Control; also calls code to
     * made its Displayable.
     *
     * @param dataChoice the DataChoice of the moment -
     *                   not used yet; can be null.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        theHolder    = new CompositeDisplayable("theHolder " + mycnt);
        mapsHolder   = new CompositeDisplayable("maps holder " + mycnt);
        latLonHolder = new CompositeDisplayable("latlonholder " + mycnt);
        theHolder.addDisplayable(mapsHolder);
        theHolder.addDisplayable(latLonHolder);

        addDisplayable(theHolder);
        int cnt = 0;
        Trace.call1("MapDisplayControl.init");



        if (initializeAsDefault) {
            initialMap          = null;
            initializeAsDefault = false;
            isDefaultMap        = true;
            setCanDoRemoveAll(false);
            MapInfo mapInfo =
                getControlContext().getResourceManager().createMapInfo(
                    inGlobeDisplay());
            if (mapInfo.getJustLoadedLocalMaps()) {
                ignoreNonVisibleMaps = false;
            }
            defaultMapData      = mapInfo.getMapDataList();
            defaultLatData      = mapInfo.getLatData();
            defaultLonData      = mapInfo.getLonData();
            defaultLatLabelData = mapInfo.getLatLabelData();
            defaultLonLabelData = mapInfo.getLonLabelData();
            //            mapPosition    = mapInfo.getMapPosition();

            //MapViewManager mvm = getMapViewManager();
            //if(mvm!=null) {
            this.mapPosition = getInitialZPosition();
            //} else {
            //}
        }


        ignoreNonVisibleMaps = false;

        if (defaultMapData != null) {
            if ( !defaultMapData.isEmpty()) {
                for (int i = 0; i < defaultMapData.size(); i++) {
                    MapData mapData = (MapData) defaultMapData.get(i);
                    if (mapData.getVisible() || !ignoreNonVisibleMaps) {
                        addMap(new MapState(mapData));
                    }
                }
            } else {  // constructed with null MapData
                if ( !selectMap()) {
                    return false;
                }
            }

            defaultMapData = null;
        }


        if (defaultLatData != null) {
            latState = new LatLonState(defaultLatData);
            latState.init(this);
        }

        if (defaultLonData != null) {
            lonState = new LatLonState(defaultLonData);
            lonState.init(this);
        }

        if (latState != null) {
            latLonHolder.addDisplayable(latState.getLatLonLines());
        }
        if (lonState != null) {
            latLonHolder.addDisplayable(lonState.getLatLonLines());
        }

        if (defaultLatLabelData != null) {
            latLabelState = new LatLonLabelState(defaultLatLabelData);
            latLabelState.init(this);
        }

        if (defaultLonLabelData != null) {
            lonLabelState = new LatLonLabelState(defaultLonLabelData);
            lonLabelState.init(this);
        }

        if (latLabelState != null) {
            latLonHolder.addDisplayable(latLabelState.getLatLonLabels());
        }
        if (lonLabelState != null) {
            latLonHolder.addDisplayable(lonLabelState.getLatLonLabels());
        }

        if (((initialMap != null) && (initialMap.trim().length() > 0))
                && !isDefaultMap) {
            addMap(initialMap, (initialMapDescription != null)
                               ? initialMapDescription
                               : initialMap);
            initialMap            = null;
            initialMapDescription = null;
        }



        //Now check for any persisted maps
        for (int i = 0; i < mapStates.size(); i++) {
            ((MapState) mapStates.get(i)).init(this);
        }

        if (Double.isNaN(mapPosition)) {
            mapPosition = getInitialZPosition();
        }

        setSliderPosition();
        applyMapPosition();
        Trace.call2("MapDisplayControl.init");
        return true;
    }




    /**
     * Helper method to create a LatLonState object
     *
     * @param latitude Is it latitude or longitude
     * @param min Minimum value
     * @param max Maximum value
     * @param spacing Line spacing
     *
     * @return The LatLonState
     */
    private LatLonState createLatLonState(boolean latitude, float min,
                                          float max, float spacing) {
        LatLonState lls = new LatLonState(latitude, Color.blue, spacing,
                                          1.0f, 1);
        lls.init(this);
        lls.setVisible(false);
        lls.setFastRendering(getDefaultFastRendering());
        lls.setMinValue(min);
        lls.setMaxValue(max);
        return lls;
    }

    /**
     * Create a LatLonLabel State
     *
     * @param latitude  true if this if for latitude labels
     * @param min minimum value
     * @param max maximum value
     * @param spacing spacing between labels (degrees)
     *
     * @return LatLonLabelState
     */
    private LatLonLabelState createLatLonLabelState(boolean latitude,
            float min, float max, float spacing) {
        LatLonLabelState lls = new LatLonLabelState(latitude, spacing);
        lls.init(this);
        lls.setVisible(false);
        lls.setFastRendering(getDefaultFastRendering());
        lls.setMinValue(min);
        lls.setMaxValue(max);
        lls.setSphere(
            ((MapViewManager) getViewManager()).getUseGlobeDisplay());
        return lls;
    }

    /**
     * Merge the maps contained by that into this
     *
     * @param that The other display control to merge its maps
     */
    public void merge(MapDisplayControl that) {}


    /**
     * Overwrite base class method so we can apply the visibility changes
     * to the maps.
     *
     * @param on Is visible
     */
    public void setDisplayVisibility(boolean on) {
        try {
            super.setDisplayVisibility(on);
            for (int i = 0; i < mapStates.size(); i++) {
                MapState mapState = (MapState) mapStates.get(i);
                mapState.checkVisibility();
            }
            if (latState != null) {
                latState.checkVisibility();
                lonState.checkVisibility();
            }
            if (latLabelState != null) {
                latLabelState.checkVisibility();
                lonLabelState.checkVisibility();
            }
        } catch (Exception exc) {
            logException("Setting visibility", exc);
        }
    }


    /**
     * Add the given map path into the list of maps
     *
     * @param mapPath Url or resource path of the map file
     * @param description The description of the map
     */
    private void addMap(String mapPath, String description) {
        addMap(new MapState(mapPath, description, DEFAULT_MAP_COLOR, 1.0f,
                            0));
    }



    /**
     * Add the given MapState into the list of maps
     *
     * @param mapState Url or resource path of the map file
     */
    private void addMap(MapState mapState) {
        setDisplayInactive();
        if (mapState.init(this)) {
            mapStates.add(mapState);
            updateLegendLabel();
        }
        setDisplayActive();
    }

    /**
     * set the visible of the given MapStates
     *
     * @param visible
     */
    public void setVisible(boolean visible) {
        if ((mapStates != null) && (mapStates.size() > 0)) {
            int len = mapStates.size();
            for (int i = 0; i < len; i = i + 1) {
                MapState ms = mapStates.get(i);
                ms.setVisible(visible);
            }
        }
    }

    /**
     * Add the  relevant view menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        JMenu mapsMenu = new JMenu("Maps");
        GuiUtils.setIcon(mapsMenu, "/auxdata/ui/icons/map.png");
        items.add(mapsMenu);
        for (int i = 0; i < mapStates.size(); i++) {
            final MapState    mapState = (MapState) mapStates.get(i);
            JCheckBoxMenuItem cbx      =
                new JCheckBoxMenuItem(mapState.getDescription(),
                                      mapState.getVisible());
            cbx.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    mapState.setVisible( !mapState.getVisible());
                    mapState.mapPanel.setVisibility(mapState.getVisible());
                }
            });
            mapsMenu.add(cbx);
        }
        super.getViewMenuItems(items, forMenuBar);
    }


    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        JMenuItem mi;
        //        if ( !forMenuBar) {            return;        }

        mi = new JMenuItem("Add Your Own Map...");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                selectMap();
            }
        });
        items.add(mi);
        JMenu addMapMenu = new JMenu("Add System Map");
        items.add(addMapMenu);
        List maps = getControlContext().getResourceManager().getMaps();
        for (int i = 0; i < maps.size(); i++) {
            final MapData mapData = (MapData) maps.get(i);
            mi = new JMenuItem(mapData.getDescription());
            addMapMenu.add(mi);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    MapState mapState = new MapState(mapData);
                    mapState.setVisible(true);
                    addMap(mapState);
                    fillContents();
                }
            });
        }
        GuiUtils.limitMenuSize(addMapMenu, "Maps ", 20);


        super.getEditMenuItems(items, forMenuBar);
    }


    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getFileMenuItems(List items, boolean forMenuBar) {

        super.getFileMenuItems(items, forMenuBar);
        JMenu     defaultMenu = new JMenu("Default Maps");
        JMenuItem mi          = new JMenuItem("Save as the Default Map Set");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveAsPreference();
            }
        });
        defaultMenu.add(mi);


        mi = new JMenuItem("Remove Local Map Defaults");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (GuiUtils.askYesNo(
                        "Remove Default Maps",
                        "Are you sure you want to remove the default maps?")) {
                    if (getControlContext().getResourceManager()
                            .removeLocalMaps(inGlobeDisplay())) {
                        LogUtil.userMessage(
                            "This will take effect when you run the IDV next");
                    } else {
                        LogUtil.userMessage(
                            "There were no local default maps defined");
                    }
                }
            }
        });
        defaultMenu.add(mi);
        items.add(defaultMenu);
    }


    /**
     * Add the  relevant File-&gt;Save menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {

        super.getSaveMenuItems(items, forMenuBar);
        items.add(GuiUtils.makeMenuItem("Export to Plugin", this,
                                        "saveToPlugin"));
    }




    /**
     * Ask the user to choose a map file and try to add it.
     *
     * @return true if a map was selected and added
     */
    protected boolean selectMap() {

        final JPanel colorButton =
            new GuiUtils.ColorSwatch(DEFAULT_MAP_COLOR, "Set Map Line Color");
        colorButton.setToolTipText("Set the line color");

        final JTextField catFld = new JTextField("Maps", 20);
        catFld.setToolTipText("Enter a category");

        final JTextField nameFld = new JTextField("", 20);
        nameFld.setToolTipText("Enter an optional map name");
        final JTextField fileFld   = new JTextField("", 20);
        JButton          browseBtn = new JButton("Browse");
        browseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String filename =
                    FileManager.getReadFile(new PatternFileFilter(".+\\.shp",
                        "Shape Files (*.shp)"));
                if (filename != null) {
                    fileFld.setText(filename);
                }
            }
        });

        JComboBox styleBox = new JComboBox(new String[] { "_____", "_ _ _",
                ".....", "_._._" });
        styleBox.setMaximumSize(new Dimension(30, 16));
        styleBox.setToolTipText("Set the line style");
        styleBox.setSelectedIndex(0);

        JComboBox widthBox = new JComboBox(new String[] { "1.0", "1.5", "2.0",
                "2.5", "3.0" });
        widthBox.setToolTipText("Set the line width");
        widthBox.setMaximumSize(new Dimension(30, 16));
        widthBox.setEditable(true);

        while (true) {
            GuiUtils.setHFill();
            JPanel fileLine = GuiUtils.doLayout(new Component[] { fileFld,
                    browseBtn }, 2, GuiUtils.WT_YN, GuiUtils.WT_N);
            JPanel nameLine = GuiUtils.left(GuiUtils.hbox(nameFld,
                                  new JLabel(" (Optional)")));
            GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);

            JPanel panel = GuiUtils.doLayout(new Component[] {
                GuiUtils.rLabel("Map file or URL: "), fileLine,
                GuiUtils.rLabel("Name: "), nameLine,
                GuiUtils.rLabel("Catgegory: "), catFld,
                GuiUtils.rLabel("Color: "), GuiUtils.left(colorButton),
                GuiUtils.rLabel("Line style: "), GuiUtils.left(styleBox),
                GuiUtils.rLabel("Line width: "), GuiUtils.left(widthBox)
            }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
            if ( !GuiUtils.showOkCancelDialog(null, "Add a map",
                    GuiUtils.inset(panel, 4), null)) {
                return false;
            }
            String filename = fileFld.getText().trim();
            if (filename.length() == 0) {
                userMessage("Please select a map file or URL");
                continue;
            }
            String description = nameFld.getText().trim();
            if (description.trim().length() == 0) {
                description =
                    IOUtil.getFileTail(IOUtil.stripExtension(filename));
            }
            MapState mapState =
                new MapState(
                    filename, description, colorButton.getBackground(),
                    Float.parseFloat((String) widthBox.getSelectedItem()),
                    styleBox.getSelectedIndex());
            mapState.setCategory(catFld.getText().trim());
            mapState.setVisible(true);
            addMap(mapState);
            fillContents();
            break;
        }
        return true;

    }

    /**
     * Turn the map state held by this object into xml and write it out as the
     * user's map preference.
     */
    private void saveAsPreference() {
        String xml = new MapInfo(mapStates, latState, lonState,
                                 latLabelState, lonLabelState,
                                 (float) mapPosition).getXml();
        getControlContext().getResourceManager().writeMapState(xml,
                inGlobeDisplay());
    }

    /**
     * Turn the map state held by this object into xml and write it out as the
     * user's map preference.
     */
    public void saveToPlugin() {
        JCheckBox onlySelected = new JCheckBox("Only use visible maps", true);
        JCheckBox includeFiles = new JCheckBox("Include map files in plugin",
                                     true);
        JCheckBox includeSettings = new JCheckBox("Include \"Settings\"",
                                        false);
        JComponent contents = GuiUtils.vbox(onlySelected, includeFiles,
                                            includeSettings);
        contents = GuiUtils.inset(contents, 5);
        if ( !GuiUtils.showOkCancelDialog(null, "Create Map Plugin",
                                          contents, null)) {
            return;
        }
        PluginManager pluginManager =
            getControlContext().getIdv().getPluginManager();
        MapInfo mapInfo;
        List    states = new ArrayList();
        for (int i = 0; i < mapStates.size(); i++) {
            MapState mapState = (MapState) mapStates.get(i);
            if ( !onlySelected.isSelected()) {
                states.add(mapState);
            } else if (mapState.getVisible()) {
                states.add(mapState);
            }
        }


        if (includeFiles.isSelected()) {
            for (int i = 0; i < states.size(); i++) {
                MapState mapState = (MapState) states.get(i);
                pluginManager.addCreateFile(mapState.getSource());
            }
        }
        if (includeSettings.isSelected()) {
            mapInfo = new MapInfo(states, latState, lonState,
                                  (float) mapPosition);
        } else {
            mapInfo = new MapInfo(states);
        }
        String xml = mapInfo.getXml( !includeFiles.isSelected());
        pluginManager.addText(xml, "maps.xml");
    }


    /**
     * Make the UI contents for this control.
     *
     * @return  UI container
     */
    public Container doMakeContents() {

        contents = new JPanel(new BorderLayout());
        if (latState == null) {
            latState = createLatLonState(true, -90.f, 90.f, 30.f);
        }
        if (lonState == null) {
            lonState = createLatLonState(false, -180.f, 180.f, 45.f);
        }
        latState.other = lonState;
        lonState.other = latState;

        if (latLabelState == null) {
            latLabelState = createLatLonLabelState(true, -90.f, 90.f, 30.f);
        }
        if (lonLabelState == null) {
            lonLabelState = createLatLonLabelState(false, -180.f, 180.f,
                    45.f);
        }
        latLabelState.other = lonLabelState;
        lonLabelState.other = latLabelState;

        LatLonPanel latPanel = new LatLonPanel(latState);
        latState.myLatLonPanel = latPanel;
        LatLonPanel lonPanel = new LatLonPanel(lonState);
        lonState.myLatLonPanel = lonPanel;
        JPanel llPanel = LatLonPanel.layoutPanels(latPanel, lonPanel);

        LatLonLabelPanel latLabelPanel = new LatLonLabelPanel(latLabelState);
        latLabelState.myLatLonLabelPanel = latLabelPanel;
        LatLonLabelPanel lonLabelPanel = new LatLonLabelPanel(lonLabelState);
        lonLabelState.myLatLonLabelPanel = lonLabelPanel;
        JPanel lllPanel = LatLonLabelPanel.layoutPanels(latLabelPanel,
                              lonLabelPanel);
        try {
            if (latLonHolder.displayableCount() == 0) {
                latLonHolder.addDisplayable(latState.getLatLonLines());
                latLonHolder.addDisplayable(lonState.getLatLonLines());
                latLonHolder.addDisplayable(latLabelState.getLatLonLabels());
                latLonHolder.addDisplayable(lonLabelState.getLatLonLabels());
            }
        } catch (Exception exc) {
            logException("Initializing latlon lines", exc);
        }

        JScrollPane sp =
            new JScrollPane(
                contents, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(10);

        JViewport vp = sp.getViewport();
        vp.setViewSize(new Dimension(600, 220));
        sp.setPreferredSize(new Dimension(600, 220));

        applyToAllLatLonBtn =
            GuiUtils.getToggleImageButton("/auxdata/ui/icons/link_break.png",
                                          "/auxdata/ui/icons/link.png", 0, 0,
                                          true);
        applyToAllLatLonBtn.setContentAreaFilled(false);
        applyToAllLatLonBtn.setSelected(applyChangesToAllLatLon);
        applyToAllLatLonBtn.setToolTipText(
            "Apply changes to all lat/lon lines");

        applyToAllLabelsBtn =
            GuiUtils.getToggleImageButton("/auxdata/ui/icons/link_break.png",
                                          "/auxdata/ui/icons/link.png", 0, 0,
                                          true);
        applyToAllLabelsBtn.setContentAreaFilled(false);
        applyToAllLabelsBtn.setSelected(applyChangesToAllLabels);
        applyToAllLabelsBtn.setToolTipText(
            "Apply changes to all lat/lon labels");

        /*
        JPanel latlonPanel = GuiUtils.topCenter(
                                 GuiUtils.vbox(
                                     GuiUtils.left(applyToAllLatLonBtn),
                                     GuiUtils.left(llPanel),
                                     GuiUtils.filler(),
                                     GuiUtils.left(
                                         lllPanel)), GuiUtils.filler());
                                         */
        JPanel latlonPanel =
            GuiUtils.topCenter(
                GuiUtils.vbox(
                    GuiUtils.left(
                        GuiUtils.hbox(
                            GuiUtils.top(applyToAllLatLonBtn),
                            GuiUtils.left(llPanel))), GuiUtils.filler(),
                                GuiUtils.left(
                                    GuiUtils.hbox(
                                        GuiUtils.top(applyToAllLabelsBtn),
                                        GuiUtils.left(
                                            lllPanel)))), GuiUtils.filler());

        JScrollPane sp2 =
            new JScrollPane(
                latlonPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        sp2.setBorder(null);
        sp2.getVerticalScrollBar().setUnitIncrement(10);

        JViewport vp2 = sp.getViewport();
        vp2.setViewSize(new Dimension(600, 220));
        sp2.setPreferredSize(new Dimension(600, 220));

        applyToAllMapsBtn =
            GuiUtils.getToggleImageButton("/auxdata/ui/icons/link_break.png",
                                          "/auxdata/ui/icons/link.png", 0, 0,
                                          true);
        applyToAllMapsBtn.setContentAreaFilled(false);
        applyToAllMapsBtn.setSelected(applyChangesToAllMaps);
        applyToAllMapsBtn.setToolTipText("Apply changes to all visible maps");


        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Maps",
                       GuiUtils.topCenter(GuiUtils.left(applyToAllMapsBtn),
                                          sp));
        tabbedPane.add("Lat/Lon", sp2);
        JComponent retComp = tabbedPane;
        if (useZPosition()) {
            //GuiUtils.tmpInsets = new Insets(0, 5, 0, 5);
            JComponent sliderPanel = GuiUtils.doLayout(new Component[] {
                                         GuiUtils.rLabel("Position: "),
                                         makePositionSlider() }, 2,
                                             GuiUtils.WT_YN, GuiUtils.WT_N);
            /* Alternate to span entire width
            JComponent sliderPanel = GuiUtils.leftCenter(
                    GuiUtils.rLabel("Position: "), makePositionSlider());
            */
            retComp = GuiUtils.centerBottom(
                tabbedPane,
                GuiUtils.inset(
                    GuiUtils.left(sliderPanel), new Insets(10, 30, 5, 0)));
        }
        fillContents();
        return retComp;

    }



    /**
     * Apply the map (height) position to the displays
     */
    private void applyMapPosition() {
        try {
            if (Double.isNaN(mapPosition)) {
                mapPosition = getInitialZPosition();
            }
            theHolder.setConstantPosition(
                getVerticalValue(mapPosition),
                getNavigatedDisplay().getDisplayAltitudeType());
        } catch (Exception exc) {
            logException("Setting map position", exc);
        }

    }


    /**
     * Create and return the JSliderused to set the map position
     *
     * @return Map position slider
     */
    private JComponent makePositionSlider() {
        levelSlider = new ZSlider(mapPosition) {
            public void valueHasBeenSet() {
                mapPosition = getValue();
                applyMapPosition();
            }
        };
        JComponent slider = levelSlider.getContents();
        Dimension  size = new Dimension(500, slider.getPreferredSize().height);
        slider.setPreferredSize(size);
        slider.setMinimumSize(size);
        return slider;
    }


    /**
     * Set the slider position without throwing an event
     */
    private void setSliderPosition() {
        if (levelSlider != null) {
            levelSlider.setValue(mapPosition);
        }
    }


    /**
     * This method removes any old components from the contents panel and
     * adds the attrbiute panel for each map.
     */
    private void fillContents() {

        if (contents == null) {
            return;
        }
        contents.removeAll();

        ImageIcon upIcon =
            GuiUtils.getImageIcon(
                "/ucar/unidata/idv/control/images/LevelUp.gif");
        ImageIcon downIcon =
            GuiUtils.getImageIcon(
                "/ucar/unidata/idv/control/images/LevelDown.gif");

        ImageIcon removeIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/map_delete.png");
        ImageIcon addIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/database_add.png");


        int       colCnt = 0;
        Hashtable catMap = new Hashtable();
        List      cats   = new ArrayList();
        for (int i = 0; i < mapStates.size(); i++) {
            final int listIndex = i;
            MapState  mapState  = (MapState) mapStates.get(i);
            String    cat       = mapState.getCategory();
            if (cat == null) {
                cat = "Maps";
            }
            List mapPanels = (List) catMap.get(cat);
            if (mapPanels == null) {
                mapPanels = new ArrayList();
                catMap.put(cat, mapPanels);
                cats.add(cat);
            }
            MapPanel mapPanel = new MapPanel(mapState);
            mapState.mapPanel = mapPanel;

            JButton addBtn = new JButton(addIcon);
            addBtn.setContentAreaFilled(false);
            addBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            addBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    loadMapAsDataSource(listIndex);
                }
            });
            addBtn.setToolTipText("Load this map as a data source");


            JButton removeBtn = new JButton(removeIcon);
            removeBtn.setContentAreaFilled(false);
            removeBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            removeBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    removeMap(listIndex);
                }
            });
            removeBtn.setToolTipText("Remove the map");


            JButton moveUpBtn = new JButton(upIcon);
            moveUpBtn.setContentAreaFilled(false);
            moveUpBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            moveUpBtn.setToolTipText("Move the map up");
            moveUpBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    moveMap(listIndex, -1);
                }
            });

            moveUpBtn.setEnabled(listIndex > 0);
            JButton moveDownBtn = new JButton(downIcon);
            moveDownBtn.setContentAreaFilled(false);
            moveDownBtn.setToolTipText("Move the map down");
            moveDownBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2,
                    2));
            moveDownBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    moveMap(listIndex, 1);
                }
            });
            moveDownBtn.setEnabled(listIndex < mapStates.size() - 1);

            JPanel upDownPanel = GuiUtils.doLayout(new Component[] {
                                     moveUpBtn,
                                     moveDownBtn }, 1, GuiUtils.WT_N,
                                         GuiUtils.WT_N);

            mapPanels.addAll(mapPanel.getGuiComponents());
            mapPanels.add(upDownPanel);
            mapPanels.add(removeBtn);
            mapPanels.add(addBtn);
            if (i == 0) {
                colCnt = mapPanels.size();
            }
            /*            mapPanels.add(
                GuiUtils.leftCenter(
                GuiUtils.leftCenter(removeBtn, upDownPanel), mapPanel));*/
        }

        final ImageIcon openIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/CategoryOpen.gif");

        final ImageIcon closeIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/CategoryClosed.gif");

        List comps = new ArrayList();
        List cbxs  = new ArrayList();
        for (int i = 0; i < cats.size(); i++) {
            String cat       = (String) cats.get(i);
            List   mapPanels = (List) catMap.get(cat);
            GuiUtils.tmpInsets = new Insets(3, 3, 3, 3);
            final JPanel catPanel =
                GuiUtils.inset(GuiUtils.doLayout(mapPanels, colCnt,
                    GuiUtils.WT_YN, GuiUtils.WT_N), new Insets(0, 10, 0, 0));

            final JToggleButton cbx = new JToggleButton(openIcon, true);
            cbxs.add(cbx);
            cbx.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            cbx.setContentAreaFilled(false);
            cbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    catPanel.setVisible( !catPanel.isVisible());
                    if (catPanel.isVisible()) {
                        cbx.setIcon(openIcon);
                    } else {
                        cbx.setIcon(closeIcon);
                    }


                }
            });
            comps.add(GuiUtils.leftCenter(GuiUtils.inset(cbx, 5),
                                          new JLabel(cat)));
            comps.add(catPanel);
        }


        if (cbxs.size() > 1) {
            for (int i = 0; i < cbxs.size(); i++) {
                ((JToggleButton) cbxs.get(i)).doClick();
            }
        }

        JComponent mapGui = GuiUtils.vbox(comps);
        mapGui = GuiUtils.left(mapGui);
        //        GuiUtils.tmpInsets = new Insets(3, 3, 3, 3);
        //        JComponent mapGui = GuiUtils.doLayout(comps, 1,
        //                                GuiUtils.WT_NNY, GuiUtils.WT_N);
        contents.add(GuiUtils.top(mapGui));
        contents.repaint();
        contents.validate();
        updateLegendLabel();

    }


    /**
     * Load the map file at the given index as a data source
     *
     * @param index The map index
     */
    private void loadMapAsDataSource(int index) {
        try {
            MapState mapState = (MapState) mapStates.get(index);
            getIdv().makeDataSource(mapState.getSource(), "file.mapfile",
                                    new Hashtable());
            getIdv().getIdvUIManager().showDataSelector();
        } catch (Exception exc) {
            logException("Removing map", exc);
        }
    }


    /**
     * Remove the map at position index.
     *
     * @param index Index of map to remove
     */
    private void removeMap(int index) {
        try {
            MapState    mapState = (MapState) mapStates.remove(index);
            Displayable theMap   = mapState.getMap();
            if (theMap != null) {
                mapsHolder.removeDisplayable(theMap);
            }
            fillContents();
        } catch (Exception exc) {
            logException("Removing map", exc);
        }
    }



    /**
     * Move the map at position index either up in the list (delta=1)
     * or down (delta=-1).
     *
     * @param index Index of map to move
     * @param delta What direction
     */
    private void moveMap(int index, int delta) {
        setDisplayInactive();
        try {
            MapState mapState = (MapState) mapStates.remove(index);
            mapStates.add(index + delta, mapState);
            //When we have an addDisplayable method here we'll use that
            //but for now we'll remove them all  and then re-add them
            //      mapsHolder.removeDisplayable (mapState.getMap()); make sure the map is non-null
            //      mapsHolder.addDisplayable (index+delta, mapState.getMap());
            deactivateDisplays();
            for (int i = 0; i < mapStates.size(); i++) {
                Displayable theMap = ((MapState) mapStates.get(i)).getMap();
                if (theMap != null) {
                    mapsHolder.removeDisplayable(theMap);
                }
            }
            for (int i = 0; i < mapStates.size(); i++) {
                Displayable theMap = ((MapState) mapStates.get(i)).getMap();
                if (theMap != null) {
                    mapsHolder.addDisplayable(theMap);
                }
            }
            fillContents();
            activateDisplays();
        } catch (Exception exc) {
            logException("Moving map position", exc);
        }
        setDisplayActive();
    }


    /**
     * Method to call if projection changes.  Override superclass
     * method to set vertical position ConstantMaps for new projection.
     */
    public void projectionChanged() {
        super.projectionChanged();
        applyMapPosition();
    }

    /**
     * Set the InitialMap property. This is the property that is set from
     * the controls.xml file that defines the initial, default map to use.
     *
     * @param value The new value for InitialMap
     */
    public void setInitialMap(String value) {
        initialMap = value;
    }

    /**
     * Get the InitialMap property.
     *
     * @return The InitialMap
     */
    public String getInitialMap() {
        return initialMap;
    }


    /**
     * Set the InitialMapDescription property. This is the property that is set
     * from the controls.xml file that defines the initial, default map to use.
     *
     * @param value The new value for InitialMapDescription
     */
    public void setInitialMapDescription(String value) {
        initialMapDescription = value;
    }

    /**
     * Get the InitialMapDescription property.
     *
     * @return The InitialMapDescription
     */
    public String getInitialMapDescription() {
        return initialMapDescription;
    }


    /**
     * This class holds the state associated with a given lat/lon
     */
    public static class LatLonState extends LatLonData {

        /** This display control I am part of */
        private MapDisplayControl mapDisplayControl;

        /** The panel that represents me */
        LatLonPanel myLatLonPanel;

        /** Flag to keep from infinite looping */
        private boolean ignoreStateChange = false;

        /** share flag */
        private boolean okToShare = true;

        /** the other state to share with */
        private LatLonState other;

        /**
         * Parameterless ctor
         */
        public LatLonState() {}


        /**
         * Ctor for creating from a LatLonData
         *
         * @param that The object to copy from
         */
        public LatLonState(LatLonData that) {
            super(that);
        }

        /**
         * The ctor
         *
         * @param isLatitude Is this latitude or longitude
         * @param color Line color
         * @param spacing Line spacing
         * @param lineWidth Line width
         * @param lineStyle Line style
         */
        public LatLonState(boolean isLatitude, Color color, float spacing,
                           float lineWidth, int lineStyle) {
            super(isLatitude, color, spacing, lineWidth, lineStyle);
        }


        /**
         * Set the MapDisplayControl
         *
         * @param mapDisplayControl The display I am part of
         */
        protected void init(MapDisplayControl mapDisplayControl) {
            this.mapDisplayControl = mapDisplayControl;
        }


        /**
         * Copy the state from the given LatLonData
         *
         * @param that The LatLonData to copy from
         */
        public void initWith(LatLonData that) {
            try {
                super.initWith(that);

                if (myLatLonPanel != null) {
                    myLatLonPanel.setLatLonData(this);
                }
                //This applies my state to the latlonlines
                getLatLonLines();
            } catch (Exception exc) {
                mapDisplayControl.logException("initWith", exc);
            }
        }

        /**
         * State was shared
         */
        private void stateWasShared() {
            ignoreStateChange = true;
            if (myLatLonPanel != null) {
                myLatLonPanel.setLatLonData(this);
            }
            ignoreStateChange = false;
            okToShare         = true;
        }


        /**
         * Should we share?
         *
         * @return true if we should share
         */
        private boolean shouldShare() {
            if ((mapDisplayControl != null)
                    && mapDisplayControl.getApplyChangesToAllLatLon()
                    && (other != null) && okToShare && !ignoreStateChange) {
                return true;
            }
            return false;
        }

        /**
         * Set the color
         *
         * @param value  the new Color
         */
        public void setColor(Color value) {
            boolean shouldShare = shouldShare()
                                  && !Misc.equals(value, getColor());
            super.setColor(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setColor(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the visibility
         *
         * @param value  true to be visible
         */
        public void setVisible(boolean value) {
            boolean shouldShare = shouldShare() && (value != getVisible());
            super.setVisible(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setVisible(value);
                other.stateWasShared();
            }
        }


        /**
         * Set the fast rendering flag
         *
         * @param value  true to turn on fast rendering
         */
        public void setFastRendering(boolean value) {
            boolean shouldShare = shouldShare()
                                  && (value != getFastRendering());
            super.setFastRendering(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setFastRendering(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the line style
         *
         * @param value  the line style flag
         */
        public void setLineStyle(int value) {
            boolean shouldShare = shouldShare() && (value != getLineStyle());
            super.setLineStyle(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setLineStyle(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the spacing of the lines
         *
         * @param value  the line spacing
         */
        public void setSpacing(float value) {
            boolean shouldShare = shouldShare() && (value != getSpacing());
            super.setSpacing(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setSpacing(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the base of the lines
         *
         * @param value  the line base
         */
        public void setBase(float value) {
            boolean shouldShare = shouldShare() && (value != getBase());
            super.setBase(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setBase(value);
                other.stateWasShared();
            }
        }


        /**
         * Set the line width
         *
         * @param value  the line width
         */
        public void setLineWidth(float value) {
            boolean shouldShare = shouldShare() && (value != getLineWidth());
            super.setLineWidth(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setLineWidth(value);
                other.stateWasShared();
            }
        }



        /**
         * Apply the visibility to the myLatLon
         */
        protected void checkVisibility() {
            if ((myLatLon == null) || (myLatLonPanel == null)) {
                return;
            }
            try {
                myLatLon.setVisible(getRealVisibility());
            } catch (Exception exc) {
                mapDisplayControl.logException("Setting visibility", exc);
            }
        }

        /**
         * Overwrite the base class method to take into account the
         * display's visibility
         *
         * @return THe actual visibility to use
         */
        protected boolean getRealVisibility() {
            if (mapDisplayControl == null) {
                return super.getRealVisibility();
            }
            return super.getRealVisibility()
                   && mapDisplayControl.getDisplayVisibility();
        }


        /**
         * Called by the base class when one of the latlon values have changed.
         */
        public void stateChanged() {
            if (ignoreStateChange || (myLatLon == null)
                    || (myLatLonPanel == null)) {
                return;
            }

            ignoreStateChange = true;
            try {
                if (okToShare) {
                    myLatLonPanel.applyStateToData();
                }
                //This triggers the application of the state to the myLatLon
                getLatLonLines();
            } catch (Exception exc) {
                logException("State change", exc);
            }
            ignoreStateChange = false;
        }

        /**
         * to string
         *
         * @return to string
         */
        public String toString() {
            return (getIsLatitude()
                    ? "map state:latitude "
                    : "map state:longitude ");
        }


    }


    /**
     * This class holds the state associated with a given lat/lon
     */
    public static class LatLonLabelState extends LatLonLabelData {

        /** This display control I am part of */
        private MapDisplayControl mapDisplayControl;

        /** The panel that represents me */
        LatLonLabelPanel myLatLonLabelPanel;

        /** Flag to keep from infinite looping */
        private boolean ignoreStateChange = false;

        /** share flag */
        private boolean okToShare = true;

        /** the other state to share with */
        private LatLonLabelState other;

        /**
         * Parameterless ctor
         */
        public LatLonLabelState() {}


        /**
         * Ctor for creating from a LatLonData
         *
         * @param that The object to copy from
         */
        public LatLonLabelState(LatLonLabelData that) {
            super(that);
        }

        /**
         * The ctor
         *
         * @param isLatitude Is this latitude or longitude
         * @param spacing Line spacing
         */
        public LatLonLabelState(boolean isLatitude, float spacing) {
            super(isLatitude, spacing);
        }


        /**
         * Set the MapDisplayControl
         *
         * @param mapDisplayControl The display I am part of
         */
        protected void init(MapDisplayControl mapDisplayControl) {
            this.mapDisplayControl = mapDisplayControl;
            MapViewManager mvm = mapDisplayControl.getMapViewManager();
            if (mvm != null) {
                setSphere(mvm.getUseGlobeDisplay());
            }
        }


        /**
         * Copy the state from the given LatLonData
         *
         * @param that The LatLonData to copy from
         */
        public void initWith(LatLonLabelData that) {
            try {
                super.initWith(that);

                if (myLatLonLabelPanel != null) {
                    myLatLonLabelPanel.setLatLonLabelData(this);
                }
                //This applies my state to the latlonlines
                getLatLonLabels();
            } catch (Exception exc) {
                mapDisplayControl.logException("initWith", exc);
            }
        }

        /**
         * State was shared
         */
        private void stateWasShared() {
            ignoreStateChange = true;
            if (myLatLonLabelPanel != null) {
                myLatLonLabelPanel.setLatLonLabelData(this);
            }
            ignoreStateChange = false;
            okToShare         = true;
        }


        /**
         * Should we share?
         *
         * @return true if we should share
         */
        private boolean shouldShare() {
            if ((mapDisplayControl != null)
                    && mapDisplayControl.getApplyChangesToAllLabels()
                    && (other != null) && okToShare && !ignoreStateChange) {
                return true;
            }
            return false;
        }

        /**
         * Set the color
         *
         * @param value  the new Color
         */
        public void setColor(Color value) {
            boolean shouldShare = shouldShare()
                                  && !Misc.equals(value, getColor());
            super.setColor(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setColor(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the visibility
         *
         * @param value  true to be visible
         */
        public void setVisible(boolean value) {
            boolean shouldShare = shouldShare() && (value != getVisible());
            super.setVisible(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setVisible(value);
                other.stateWasShared();
            }
        }


        /**
         * Set the fast rendering flag
         *
         * @param value  true to turn on fast rendering
         */
        public void setFastRendering(boolean value) {
            boolean shouldShare = shouldShare()
                                  && (value != getFastRendering());
            super.setFastRendering(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setFastRendering(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the label lines of the lines
         *
         * @param values  the label lines
         */
        public void setLabelLines(float[] values) {
            boolean shouldShare = false
                                  && ( !Arrays.equals(values,
                                      getLabelLines()));
            super.setLabelLines(values);
            if (shouldShare && (other != null)) {
                other.okToShare = false;
                other.setLabelLines(values);
                other.stateWasShared();
            }
        }

        /**
         * Set the spacing of the lines
         *
         * @param value  the line spacing
         */
        public void setInterval(float value) {
            boolean shouldShare = shouldShare() && (value != getInterval());
            super.setInterval(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setInterval(value);
                other.stateWasShared();
            }
        }


        /**
         * Set the base
         *
         * @param value  the line width
         */
        public void setBaseValue(float value) {
            boolean shouldShare = shouldShare() && (value != getBaseValue());
            super.setBaseValue(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setBaseValue(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the font
         *
         * @param value  the label font
         */
        public void setFont(Object value) {
            //boolean shouldShare = shouldShare() && (!Misc.equals(value,getFont()));
            boolean shouldShare = true && ( !Misc.equals(value, getFont()));
            super.setFont(value);
            if (shouldShare && (other != null)) {
                other.okToShare = false;
                other.setFont(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the alignment point
         *
         * @param value  the alignment point
         */
        public void setAlignment(String value) {
            boolean shouldShare = shouldShare()
                                  && ( !Misc.equals(value, getAlignment()));
            super.setAlignment(value);
            if (shouldShare) {
                other.okToShare = false;
                other.setAlignment(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the label format
         *
         * @param value  the label format
         */
        public void setLabelFormat(String value) {
            boolean shouldShare = true
                                  && ( !Misc.equals(value, getLabelFormat()));
            super.setLabelFormat(value);
            if (shouldShare && (other != null)) {
                other.okToShare = false;
                other.setLabelFormat(value);
                other.stateWasShared();
            }
        }


        /**
         * Set the sphere flag
         *
         * @param value  the sphere flag
         */
        public void setSphere(boolean value) {
            //boolean shouldShare = shouldShare() && (!Misc.equals(value,getFont()));
            boolean shouldShare = true && ( !Misc.equals(value, getSphere()));
            super.setSphere(value);
            if (shouldShare && (other != null)) {
                other.okToShare = false;
                other.setSphere(value);
                other.stateWasShared();
            }
        }

        /**
         * Set the use360 property
         *
         * @param value  true to use 0-360 for longitudes
         */
        public void setUse360(boolean value) {
            boolean shouldShare = false && (value != getVisible());
            super.setUse360(value);
            if (shouldShare && (other != null)) {
                other.okToShare = false;
                other.setUse360(value);
                other.stateWasShared();
            }
        }



        /**
         * Apply the visibility to the myLatLon
         */
        protected void checkVisibility() {
            if ((myLatLonLabels == null) || (myLatLonLabelPanel == null)) {
                return;
            }
            try {
                myLatLonLabels.setVisible(getRealVisibility());
            } catch (Exception exc) {
                mapDisplayControl.logException("Setting visibility", exc);
            }
        }

        /**
         * Overwrite the base class method to take into account the
         * display's visibility
         *
         * @return THe actual visibility to use
         */
        protected boolean getRealVisibility() {
            if (mapDisplayControl == null) {
                return super.getRealVisibility();
            }
            return super.getRealVisibility()
                   && mapDisplayControl.getDisplayVisibility();
        }


        /**
         * Called by the base class when one of the latlon values have changed.
         */
        public void stateChanged() {
            if (ignoreStateChange || (myLatLonLabels == null)
                    || (myLatLonLabelPanel == null)) {
                return;
            }

            ignoreStateChange = true;
            try {
                if (okToShare) {
                    myLatLonLabelPanel.applyStateToData();
                }
                //This triggers the application of the state to the myLatLon
                getLatLonLabels();
            } catch (Exception exc) {
                logException("State change", exc);
            }
            ignoreStateChange = false;
        }

        /**
         * to string
         *
         * @return to string
         */
        public String toString() {
            return (getIsLatitude()
                    ? "map label state:latitude "
                    : "map label state:longitude ");
        }


    }

    /**
     * This method can be overwritten by the derived classes that do not want the
     * general application of the fast rendering flag.
     *
     * @return Don't use fast rendering
     */
    protected boolean shouldApplyFastRendering() {
        return false;
    }

    /**
     * What is the default fast rendering value
     *
     * @return false
     */
    protected boolean getDefaultFastRendering() {
        return false;
    }


    /**
     * This class holds the state associated with a given map
     */
    public static class MapState extends MapData {

        /** This display control I am part of */
        private MapDisplayControl mapDisplayControl;

        /** map panel */
        private MapPanel mapPanel;

        /** A mutex */
        private Object MAP_MUTEX = new Object();

        /**
         * ctor for persistence
         */
        public MapState() {}



        /**
         * ctor for instantiating from a MapData
         *
         * @param that The MapData to copy from
         */
        public MapState(MapData that) {

            this.source        = that.getSource();
            this.category      = that.getCategory();
            this.visible       = that.getVisible();
            this.mapColor      = that.getColor();
            this.lineWidth     = that.getLineWidth();
            this.lineStyle     = that.getLineStyle();
            this.description   = that.getDescription();
            this.fastRendering = that.getFastRendering();
        }


        /**
         * Construct this MapState.
         *
         * @param mapPath File path, resource path or url of the map file.
         * @param description Map description
         * @param mapColor The initial color to use
         * @param lineWidth The initial line width to use
         * @param lineStyle The initial line style to use
         */
        public MapState(String mapPath, String description, Color mapColor,
                        float lineWidth, int lineStyle) {
            super(mapPath, null);
            this.category    = "Maps";
            this.visible     = true;
            this.mapColor    = mapColor;
            this.lineWidth   = lineWidth;
            this.lineStyle   = lineStyle;
            this.description = description;
        }


        /**
         * Create, if needed, and return the SampledSet that represents this map
         *
         * @param source The map source (may be a java resource, url or file)
         *
         * @return The map data
         */
        private SampledSet getData(String source) {
            return MapInfo.createMapData(source);
        }




        /**
         * Take into account the visibility of the display control
         */
        protected void checkVisibility() {
            if (mapDisplayControl == null) {
                return;
            }
            boolean isVisible = getVisible()
                                && mapDisplayControl.getDisplayVisibility();
            try {
                //Check if we need to create the map
                if (myMap == null) {
                    //If we are not visible then don't create the map.
                    if ( !isVisible) {
                        return;
                    }
                    //Load in the data
                    SampledSet mapSet = getData(source);
                    if (mapSet == null) {
                        return;
                    }
                    // System.err.println("creating map:" + description + " from: " + source);
                    myMap = new MapLines("Map:" + source);
                    myMap.setMapLines(mapSet);
                    //Set the colors, etc.
                    applyStateToMap();

                    //Add in the map 
                    mapDisplayControl.mapsHolder.addDisplayable(myMap);
                }
                myMap.setVisible(isVisible);
            } catch (Exception exc) {
                mapDisplayControl.logException("Setting visibility", exc);
            }
        }

        /**
         * Initialize the map.
         *
         * @param mapDisplayControl The display control we are part of
         * @return true if everything ok. False otherwise. If a problem this method
         * will signal the user.
         */
        public boolean init(MapDisplayControl mapDisplayControl) {
            this.mapDisplayControl = mapDisplayControl;
            try {
                //Check if we are already initialized
                if (myMap != null) {
                    return true;
                }
                checkVisibility();
                return true;
            } catch (Exception exc) {
                mapDisplayControl.logException("Making map:" + source, exc);
            }
            return false;
        }

        /**
         * A method that allows derived classes to be told
         * when the state has changed.
         */
        protected void stateChanged() {
            try {
                //Call this first to create the myMap if needed
                checkVisibility();
                if (myMap == null) {
                    return;
                }
                Misc.run(new Runnable() {
                    public void run() {
                        applyStateToMap();
                    }
                });
                if (mapDisplayControl != null) {
                    mapDisplayControl.updateLegendLabel();
                }
            } catch (Exception exc) {
                //
            }
        }



        /**
         * Apply my map state to the map
         */
        protected void applyStateToMap() {
            try {
                synchronized (MAP_MUTEX) {
                    myMap.setDisplayInactive();
                    myMap.setUseFastRendering(fastRendering);
                    myMap.setColor(mapColor);
                    myMap.setLineWidth(lineWidth);
                    myMap.setLineStyle(lineStyle);
                    myMap.setDisplayActive();
                }
            } catch (Exception exc) {
                //
            }
        }


        /**
         * Signal that the given attribute has changed
         *
         * @param attr The attribute identifier
         */
        private void attrChanged(String attr) {
            if (mapDisplayControl != null) {
                mapDisplayControl.mapStateChanged(this, attr);
            }
        }


        /**
         * Subclassed method to pickup changes
         *
         * @param v The value
         */
        public void setVisible(boolean v) {
            super.setVisible(v);
            attrChanged(MapData.ATTR_VISIBLE);
        }

        /**
         * Subclassed method to pickup changes
         *
         * @param v The value
         */
        public void setLineStyle(int v) {
            super.setLineStyle(v);
            attrChanged(ATTR_LINESTYLE);
        }

        /**
         * Subclassed method to pickup changes
         *
         * @param v The value
         */
        public void setColor(Color v) {
            super.setColor(v);
            attrChanged(MapData.ATTR_COLOR);
        }

        /**
         * Subclassed method to pickup changes
         *
         * @param v The value
         */
        public void setLineWidth(float v) {
            super.setLineWidth(v);
            attrChanged(MapData.ATTR_LINEWIDTH);
        }

        /**
         * Subclassed method to pickup changes
         *
         * @param v The value
         */
        public void setFastRendering(boolean v) {
            super.setFastRendering(v);
            attrChanged(MapData.ATTR_FASTRENDERING);
        }

    }



    /**
     * Apply the changed map state to the other visible maps
     *
     * @param changedState Which one changed
     * @param what Which attribute
     */
    private void mapStateChanged(MapState changedState, String what) {
        updateLegendLabel();
        if (what.equals(MapData.ATTR_VISIBLE)) {
            return;
        }
        if (updatingOtherMapStates || !getApplyChangesToAllMaps()) {
            return;
        }
        updatingOtherMapStates = true;
        try {
            for (MapState mapState : mapStates) {
                if (mapState == changedState) {
                    continue;
                }
                if ( !mapState.getVisible()) {
                    continue;
                }
                if (what.equals(MapData.ATTR_COLOR)) {
                    mapState.setColor(changedState.getColor());
                } else if (what.equals(MapData.ATTR_LINEWIDTH)) {
                    mapState.setLineWidth(changedState.getLineWidth());
                } else if (what.equals(MapData.ATTR_LINESTYLE)) {
                    mapState.setLineStyle(changedState.getLineStyle());
                } else if (what.equals(MapData.ATTR_FASTRENDERING)) {
                    mapState.setFastRendering(
                        changedState.getFastRendering());
                } else if (what.equals(MapData.ATTR_VISIBLE)) {
                    continue;
                    //                mapState.setVisible(changedState.getVisible());
                } else {
                    System.err.println("Unknown attribute:" + what);
                }
                if (mapState.mapPanel != null) {
                    mapState.mapPanel.updateUI();
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        updatingOtherMapStates = false;
    }


    /**
     *  Set the MapStates property.
     *
     *  @param value The new value for MapStates
     */
    public void setMapStates(List value) {
        mapStates = value;
    }

    /**
     *  Get the MapStates property.
     *
     *  @return The MapStates
     */
    public List getMapStates() {
        return mapStates;
    }


    /**
     * Get the object that holds the latitude state
     *
     * @return The latitude state
     */
    public LatLonState getLatState() {
        return latState;
    }

    /**
     * Set the object that holds the latitude state
     *
     *
     * @param value The new latitude state
     */
    public void setLatState(LatLonState value) {
        latState = value;
    }


    /**
     * Get the object that holds the longitude state
     *
     * @return The long. state
     */
    public LatLonState getLonState() {
        return lonState;
    }

    /**
     * Set the object that holds the longitude state
     *
     * @param value The new long. state
     */
    public void setLonState(LatLonState value) {
        lonState = value;
    }

    /**
     * Get the object that holds the latitude label state
     *
     * @return The latitude label state
     */
    public LatLonLabelState getLatLabelState() {
        return latLabelState;
    }

    /**
     * Set the object that holds the latitude label state
     *
     *
     * @param value The new latitude label state
     */
    public void setLatLabelState(LatLonLabelState value) {
        latLabelState = value;
    }


    /**
     * Get the object that holds the longitude label state
     *
     * @return The long. label state
     */
    public LatLonLabelState getLonLabelState() {
        return lonLabelState;
    }

    /**
     * Set the object that holds the longitude label state
     *
     * @param value The new long. label state
     */
    public void setLonLabelState(LatLonLabelState value) {
        lonLabelState = value;
    }


    /**
     * Clear out any lingering references
     *
     * @throws RemoteException  remote display problem
     * @throws VisADException   local display problem
     */
    public void doRemove() throws RemoteException, VisADException {
        List infos = getDisplayInfos();
        if ((infos != null) && (infos.size() == 0)) {
            theHolder.destroyAll();
        }
        super.doRemove();
        theHolder    = null;
        mapsHolder   = null;
        latLonHolder = null;
    }


    /**
     * Set the initializeAsDefault property. This is used
     * when we do an addDefaultMap
     *
     * @param value The value
     */
    public void setInitializeAsDefault(boolean value) {
        initializeAsDefault = value;
    }





    /**
     * Get the IsDefaultMap property.
     *
     * @return The IsDefaultMap
     */
    public boolean getIsDefaultMap() {
        return isDefaultMap;
    }

    /**
     * Set the IsDefaultMap property.
     *
     * @param value The new value for IsDefaultMap
     */
    public void setIsDefaultMap(boolean value) {
        isDefaultMap = value;
        if (isDefaultMap) {
            setCanDoRemoveAll(false);
        }
    }

    /**
     *  Set the MapPosition property.
     *
     *  @param value The new value for MapPosition
     */
    public void setMapPosition(double value) {
        mapPosition = value;
    }

    /**
     *  Get the MapPosition property.
     *
     *  @return The MapPosition
     */
    public double getMapPosition() {
        return mapPosition;
    }

    /**
     * Set the ShowInDisplayList property.
     *
     * @param value The new value for ShowInDisplayList
     */
    public void setShowInDisplayList(boolean value) {
        myShowInDisplayList = value;
        super.setShowInDisplayList(value);
    }

    /**
     * Get the ShowInDisplayList property.
     *
     * @return The ShowInDisplayList
     */
    public boolean getShowInDisplayList() {
        return myShowInDisplayList;
    }


    /**
     *  Set the ApplyChangesToAllMaps property.
     *
     *  @param value The new value for ApplyChangesToAllMaps
     */
    public void setApplyChangesToAllMaps(boolean value) {
        applyChangesToAllMaps = value;
    }

    /**
     *  Get the ApplyChangesToAllMaps property.
     *
     *  @return The ApplyChangesToAllMaps
     */
    public boolean getApplyChangesToAllMaps() {
        if (applyToAllMapsBtn != null) {
            return applyToAllMapsBtn.isSelected();
        }
        return applyChangesToAllMaps;
    }




    /**
     *  Set the ApplyChangesToAllLatLon property.
     *
     *  @param value The new value for ApplyChangesToAllLatLon
     */
    public void setApplyChangesToAllLatLon(boolean value) {
        applyChangesToAllLatLon = value;
    }

    /**
     *  Get the ApplyChangesToAllLatLon property.
     *
     *  @return The ApplyChangesToAllLatLon
     */
    public boolean getApplyChangesToAllLatLon() {
        if (applyToAllLatLonBtn != null) {
            return applyToAllLatLonBtn.isSelected();
        }
        return applyChangesToAllLatLon;
    }

    /**
     *  Set the ApplyChangesToAllLatLon property.
     *
     *  @param value The new value for ApplyChangesToAllLatLon
     */
    public void setApplyChangesToAllLabels(boolean value) {
        applyChangesToAllLabels = value;
    }

    /**
     *  Get the ApplyChangesToAllLatLon property.
     *
     *  @return The ApplyChangesToAllLatLon
     */
    public boolean getApplyChangesToAllLabels() {
        if (applyToAllLabelsBtn != null) {
            return applyToAllLabelsBtn.isSelected();
        }
        return applyChangesToAllLabels;
    }

    /**
     * Get default z position to use
     *
     * @return Default z position
     */
    protected double getInitialZPosition() {
        MapViewManager mvm = ((defaultViewManager != null)
                              && (defaultViewManager
                                  instanceof MapViewManager))
                             ? (MapViewManager) defaultViewManager
                             : getMapViewManager();
        //System.out.println("mvm = " + mvm);
        return (mvm != null)
               ? mvm.getDefaultMapPosition()
               : super.getInitialZPosition() + ZFUDGE;
    }

}

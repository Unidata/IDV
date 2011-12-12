/*
 * $Id: MapWidget.java,v 1.48 2007/07/06 20:45:31 jeffmc Exp $
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

package ucar.unidata.ui;


import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



import ucar.unidata.gis.maps.*;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Resource;
import ucar.unidata.xml.XmlResourceCollection;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.MapFamily;
import ucar.visad.display.CompositeDisplayable;


import ucar.visad.display.MapLines;

import visad.SampledSet;




import visad.VisADException;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.URL;

import java.rmi.RemoteException;


import java.util.*;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import javax.swing.event.*;


/**
 * Widget for configuring maps.  Maintains a CompositeDisplayable
 * of MapLines and LatLonLines.  The user can configure the visibility
 * and color of a particular set of map or lat/lon lines.  The increments
 * between lat/lon lines can be configured as well.  The widget is configured
 * through a properties file.  When the properties of the widget are set
 * and the "Apply" button is selected, a PropertyChangeEvent is thrown
 * with the CompositeDisplayable of all visible maps as the newValue.
 */

public class MapWidget extends PanelWithFrame implements ActionListener {

    /** For LogUtil logging */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(MapWidget.class.getName());

    /** The property we fire when something changes */
    public static final String MAP_COMPOSITE = "Map Composite";

    /** Where to look for maps */
    public static final String DEFAULT_MAPFILE = "/auxdata/maps/maps.xml";

    /** Xml tag for the latitude entry */
    public static final String TAG_LATITUDE = "latitude";

    /** Xml tag for the longitude entry */
    public static final String TAG_LONGITUDE = "longitude";

    /** Xml tag for the maps entry */
    public static final String TAG_MAPS = "maps";

    /** Xml tag for the map entry */
    public static final String TAG_MAP = "map";

    /** Xml attribute for the color property */
    public static final String ATTR_COLOR = "color";

    /** Xml attribute for the  map description property */
    public static final String ATTR_DESCRIPTION = "description";

    /** Xml attribute for the  linewidth property */
    public static final String ATTR_LINEWIDTH = "linewidth";

    /** Xml attribute for the linestyle  property */
    public static final String ATTR_LINESTYLE = "linestyle";

    /** Xml attribute for the min value  property */
    public static final String ATTR_MINVALUE = "minvalue";

    /** Xml attribute for the max value property */
    public static final String ATTR_MAXVALUE = "maxvalue";

    /** Xml attribute for the  map vertical position property */
    public static final String ATTR_POSITION = "position";

    /** Xml attribute for the  map source property */
    public static final String ATTR_SOURCE = "source";

    /** Xml attribute for the spacing  property */
    public static final String ATTR_SPACING = "spacing";

    /** Xml attribute for the  property */
    public static final String ATTR_VALID = "valid";

    /** Xml attribute for the valid  property - used to ignore certain map entries */
    public static final String ATTR_VISIBLE = "visible";

    /** Static cache of the maps we have read in */
    private static Hashtable mapCache = new Hashtable();

    /**
     *  Holds the xml of the current state of this widget.
     */
    private String theCurrentState;


    /**
     *  Contains a list of all of the valid map nodes
     */
    private ArrayList mapNodes;

    /**
     *  Keeps track of what nodes we have seen when we are loading
     *  in the users' and the default  xml.
     */
    private Hashtable seenNodes;


    /** Xml node that represents the latitude entry */
    private Element latitudeNode;

    /** Xml node that represents the longitude entry */
    private Element longitudeNode;

    /**
     *  The default xml (usually from /auxdata/maps/...)
     */
    private XmlResourceCollection mapResources;


    /**
     *  List of LatLonPanel objects.
     */
    private ArrayList latLonPanels = new ArrayList();

    /**
     *  List of mapData objects, one for each map in the gui
     */
    private ArrayList mapDatum = new ArrayList();



    /** The map position_ slider */
    private JSlider postionSlider;


    /**
     *  Set by the map xml, holds the level of the map used in the positionSlider
     */
    private double mapPosition = Double.NaN;


    /**
     * Private copy of MapFamily
     */
    private static MapFamily mapFamily = new MapFamily("IDV maps");



    /**
     * Construct the widget using the specified set of default and users xml
     *
     * @param mapResources Where the maps.xml are
     * @param standAlone Is this embedded in another window
     */
    public MapWidget(XmlResourceCollection mapResources, boolean standAlone) {
        if (standAlone) {
            makeFrame("Map Control Widget");
        }

        // Load in the defaults list
        setLayout(new BorderLayout());

        //Add me to the frame
        if (haveFrame()) {
            addContentsToFrame(this);
        }


        if (mapResources == null) {
            mapResources = new XmlResourceCollection(DEFAULT_MAPFILE);
        }
        this.mapResources = mapResources;
        loadDefaults();
    }


    /**
     *  Go through the latitude, longitude and maps xml tags.
     *  Create the appropriate objects and add them to the collection
     *  if we have not seen them already
     *
     * @param root
     */
    private void processMapsNode(Element root) {
        if (root == null) {
            return;
        }
        List nodes = XmlUtil.findChildren(root, TAG_MAP);
        for (int i = 0; i < nodes.size(); i++) {
            Element mapNode = (Element) nodes.get(i);
            if ( !XmlUtil.getAttribute(mapNode, ATTR_VALID, true)) {
                continue;
            }
            String source = XmlUtil.getAttribute(mapNode, ATTR_SOURCE);
            if (seenNodes.get(source) != null) {
                continue;
            }
            mapNodes.add(mapNode);
            seenNodes.put(source, mapNode);
        }

        if (latitudeNode == null) {
            latitudeNode = XmlUtil.findChild(root, TAG_LATITUDE);
        }

        if (longitudeNode == null) {
            longitudeNode = XmlUtil.findChild(root, TAG_LONGITUDE);
        }

        if (Double.isNaN(mapPosition)) {
            mapPosition = (double) XmlUtil.getAttribute(root, ATTR_POSITION,
                    Double.NaN);
        }
    }

    /**
     *  Maps the map source to the XML node
     *
     * @param source
     * @return The Xml node that holds the given source string
     */
    private Element findNode(String source) {
        return (Element) seenNodes.get(source);
    }

    /**
     *  Reinitialize the state, using the xml passed in at ctor time.
     */
    private void loadDefaults() {
        try {
            //Initialize our state
            mapPosition   = Double.NaN;
            mapNodes      = new ArrayList();
            seenNodes     = new Hashtable();
            latitudeNode  = null;
            longitudeNode = null;

            //Check the user's xml first

            System.err.println("MapWidget.init");
            //Now check the default
            for (int i = 0; i < mapResources.size(); i++) {
                processMapsNode(mapResources.getRoot(i));
            }

            //Make sure we have a mapPosition
            if (Double.isNaN(mapPosition)) {
                mapPosition = -.99;
            }

        } catch (Exception exc) {
            LogUtil.printException(log_, "Creating xml", exc);
            return;
        }
        makeUI();
    }


    /**
     *  Recreate the users xml from the given parameter and loadDefaults
     *
     * @param xml The string/xml representation of the map state
     */
    public void setCurrentState(String xml) {
        loadDefaults();
    }

    /**
     *  Return the current state (as an XML string) of this MapWidget.
     *
     * @return The xml representation of the current state
     */
    public String getCurrentState() {
        if (theCurrentState == null) {
            setMapProperties(false);
        }
        return theCurrentState;
    }


    /**
     *  Go  through the gui and create a DOM that holds the current state.
     *  Turn the DOM into a String (theCurrentState).
     */
    private void createCurrentState() {
        Document document     = XmlUtil.makeDocument();
        Element  currentState = document.createElement(TAG_MAPS);
        currentState.setAttribute(ATTR_POSITION, "" + mapPosition);
        try {
            for (int i = 0; i < latLonPanels.size(); i++) {
                LatLonPanel latLonPanel = (LatLonPanel) latLonPanels.get(i);
                LatLonData  lld         = latLonPanel.getLatLonData();
                Element     oldElement  = ((i == 0)
                                           ? latitudeNode
                                           : longitudeNode);
                Element     newElement  = document.createElement((i == 0)
                        ? TAG_LATITUDE
                        : TAG_LONGITUDE);
                currentState.appendChild(newElement);
                XmlUtil.mergeAttributes(newElement, oldElement);
                XmlUtil.setAttributes(newElement, new String[] {
                    ATTR_SPACING, "" + lld.getSpacing(), ATTR_COLOR,
                    "" + lld.getColor().getRGB(), ATTR_LINESTYLE,
                    "" + lld.getLineStyle(), ATTR_LINEWIDTH,
                    "" + lld.getLineWidth(), ATTR_VISIBLE,
                    "" + lld.getVisible()
                });
            }

            // Now loop through the maps
            for (int i = 0; i < mapDatum.size(); i++) {
                MapData mapData    = (MapData) mapDatum.get(i);
                Element oldElement = findNode(mapData.getSource());
                Element newElement = document.createElement(TAG_MAP);
                currentState.appendChild(newElement);
                XmlUtil.mergeAttributes(newElement, oldElement);
                XmlUtil.setAttributes(newElement, new String[] {
                    ATTR_SOURCE, mapData.getSource(), ATTR_COLOR,
                    "" + mapData.getColor().getRGB(), ATTR_LINEWIDTH,
                    "" + mapData.getLineWidth(), ATTR_LINESTYLE,
                    "" + mapData.getLineStyle(), ATTR_VISIBLE,
                    "" + mapData.getVisible(), ATTR_DESCRIPTION,
                    mapData.getDescription()
                });
            }
        } catch (Exception e) {
            LogUtil.printException(log_, "Couldn't create map state", e);
        }
        theCurrentState = XmlUtil.toString(currentState);
    }





    /**
     *  Make (or remake) the UI
     */
    private void makeUI() {
        removeAll();
        JPanel topPanel  = new JPanel(new BorderLayout());
        JPanel mainPanel = GuiUtils.top(topPanel);
        add("Center", mainPanel);
        try {
            // Create entries for Lat/Lon lines at the least
            topPanel.add(makeLatLonLines(), BorderLayout.CENTER);
            // Make  map lines
            mainPanel.add(makeMapEntries(), BorderLayout.CENTER);
            topPanel.add(makePositionSlider(), BorderLayout.SOUTH);
        } catch (Exception excp) {
            System.err.println("Problem creating widgets");
            excp.printStackTrace();
        }
        if (haveFrame()) {
            Container btnPanel = GuiUtils.makeApplyOkResetCancelButtons(this);
            add(btnPanel, BorderLayout.SOUTH);
        }
        setMapProperties(true);
        packFrame();
    }

    /**
     * Apply the gui properties to the internal state. Remake the xml.
     */
    public void doOk() {
        setMapProperties(true);
    }


    /**
     *  Handle OK, Apply, Reset and Cancel buttom commands
     *
     * @param event The action event
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
            setMapProperties(true);
        }
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_CANCEL)) {
            closeFrame();
        }
        if (cmd.equals(GuiUtils.CMD_RESET)) {
            loadDefaults();
        }
    }






    /**
     * Apply the gui to the data structures. Fire a cahnge event if needed.
     *
     * @param andFireChange Should we fire a change event
     */
    private void setMapProperties(boolean andFireChange) {
        try {
            // Lat/lon lines first. Make sure we got all of the gui state applied to
            //the latlondata
            for (int i = 0; i < latLonPanels.size(); i++) {
                LatLonPanel llp = (LatLonPanel) latLonPanels.get(i);
                llp.applyStateToData();
            }
            createCurrentState();
            if (haveFrame() && andFireChange) {
                firePropertyChange(MAP_COMPOSITE, null, this);
            }
        } catch (Exception e) {
            LogUtil.printException(log_, "Couldn't create map composite", e);
        }
    }

    /**
     * Get the vertical map position value
     *
     * @return map position value
     */
    public double getMapPosition() {
        return mapPosition;
    }


    /**
     * Make and return the slider for map vertical position
     *
     * @return map vertical position
     */
    private JComponent makePositionSlider() {
        JSlider levelSlider = new JSlider(-99, 100,
                                          (int) (100 * mapPosition));
        levelSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                if ( !slider.getValueIsAdjusting()) {
                    mapPosition = slider.getValue() / 100.;
                }
            }
        });
        JPanel labelPanel = GuiUtils.leftCenterRight(new JLabel("Bottom"),
                                GuiUtils.cLabel("Middle"),
                                GuiUtils.rLabel("Top"));

        JPanel sliderPanel = GuiUtils.doLayout(new Component[] {
                                 new JLabel("Maps position in display:   "),
                                 levelSlider, new JLabel(" "),
                                 labelPanel }, 2, GuiUtils.WT_NY,
                                     GuiUtils.WT_NN);
        return GuiUtils.inset(sliderPanel, new Insets(20, 4, 4, 4));

    }


    /**
     * Make the gui component for the maps
     *
     * @return gui component for the maps
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private JComponent makeMapEntries()
            throws VisADException, RemoteException {
        mapDatum = new ArrayList();

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        JScrollPane sp =
            new JScrollPane(innerPanel,
                            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);


        JViewport vp = sp.getViewport();
        vp.setViewSize(new Dimension(500, 200));

        JPanel outerPanel = GuiUtils.topCenter(new JLabel(" Maps:"), sp);

        JPanel p = GuiUtils.leftCenterRight(
                       new JLabel(" On/Off"), null,
                       new JLabel("Width/Style/Color             "));
        p.setBorder(BorderFactory.createEtchedBorder());
        sp.setColumnHeaderView(p);
        sp.setPreferredSize(new Dimension(500, 200));

        for (int i = 0; i < mapNodes.size(); i++) {
            Element mapNode = (Element) mapNodes.get(i);
            try {
                String description = XmlUtil.getAttribute(mapNode,
                                         ATTR_DESCRIPTION);
                String source = XmlUtil.getAttribute(mapNode, ATTR_SOURCE);

                // make the displayable and set the name to the description
                MapLines mapLines = new MapLines(description);

                mapLines.setColor(XmlUtil.getAttribute(mapNode, ATTR_COLOR,
                        Color.white));
                mapLines.setVisible(XmlUtil.getAttribute(mapNode,
                        ATTR_VISIBLE, true));
                mapLines.setLineWidth(XmlUtil.getAttribute(mapNode,
                        ATTR_LINEWIDTH, 1.0f));
                mapLines.setLineStyle(XmlUtil.getAttribute(mapNode,
                        ATTR_LINESTYLE, 0));

                // set the width
                if (mapLines.getVisible()) {
                    setMapData(mapLines, source);
                }
                MapData  md = new MapData(source, mapLines);
                MapPanel mp = new MapPanel(md);
                mapDatum.add(md);
                innerPanel.add(mp);
            } catch (Exception ve) {
                ve.printStackTrace();
                continue;
            }
        }

        return GuiUtils.inset(outerPanel, 4);
    }




    /**
     * Make the lat lon lines panel
     *
     * @return the lat lon lines panel
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private JComponent makeLatLonLines()
            throws VisADException, RemoteException {
        latLonPanels = new ArrayList();
        LatLonData latData = createLatLonData(latitudeNode, true, -90.f,
                                 90.f, 30.f);
        LatLonData lonData = createLatLonData(longitudeNode, false, -180.f,
                                 180.f, 45.f);

        LatLonPanel latPanel = new LatLonPanel(latData);
        latLonPanels.add(latPanel);
        LatLonPanel lonPanel = new LatLonPanel(lonData);
        latLonPanels.add(lonPanel);

        return LatLonPanel.layoutPanels(latPanel, lonPanel);
    }


    /**
     * Load in the map data specified by the source (if not loaded already)
     * and add it to the given map object.
     *
     * @param map The map to add data to
     * @param source The file name, resource or url that points to map data
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private void setMapData(MapLines map, String source)
            throws VisADException, RemoteException {
        SampledSet mapSet = MapInfo.createMapData(source);
        if (mapSet != null) {
            map.setMapLines(mapSet);
            return;
        }
    }




    /**
     * Get the map composite which is all the maps that are set to be
     * visible.
     *
     * @return  map composite
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public CompositeDisplayable createMaps()
            throws VisADException, RemoteException {
        CompositeDisplayable maps = new CompositeDisplayable();
        for (int i = 0; i < latLonPanels.size(); i++) {
            LatLonData latLonData =
                ((LatLonPanel) latLonPanels.get(i)).getLatLonData();
            maps.addDisplayable(latLonData.getLatLonLines());
        }

        // Now loop through the maps
        for (int i = 0; i < mapDatum.size(); i++) {
            MapData  mapData  = (MapData) mapDatum.get(i);
            MapLines mapLines = mapData.getMap();

            // Color
            if ( !(mapLines.getColor().equals(mapData.getColor()))) {
                mapLines.setColor(mapData.getColor());
            }

            // Line Width
            if (Float.floatToIntBits(mapLines.getLineWidth())
                    != Float.floatToIntBits(mapData.getLineWidth())) {
                mapLines.setLineWidth(mapData.getLineWidth());
            }

            // Line Style
            if (mapLines.getLineStyle() != mapData.getLineStyle()) {
                mapLines.setLineStyle(mapData.getLineStyle());
            }

            // Visibility
            if (mapLines.isVisible() != mapData.getVisible()) {
                if ( !mapLines.hasData() && mapData.getVisible()) {
                    setMapData(mapLines, mapData.getSource());
                }
                mapLines.setVisible(mapData.getVisible());
            }
            if (mapLines.isVisible()) {
                maps.addDisplayable(mapLines);
            }
        }

        return maps;
    }


    /**
     * Create the {@link LatLonData}
     *
     * @param node The xml node
     * @param latitude Is this for lat or lon
     * @param min The min value
     * @param max The max value
     * @param spacing The spacing
     * @return The new LatLonData
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private LatLonData createLatLonData(Element node, boolean latitude,
                                        float min, float max, float spacing)
            throws VisADException, RemoteException {
        LatLonData lld =
            new LatLonData(latitude,
                           XmlUtil.getAttribute(node, ATTR_COLOR,
                               Color.white), XmlUtil.getAttribute(node,
                                   ATTR_SPACING,
                                   spacing), XmlUtil.getAttribute(node,
                                       ATTR_LINEWIDTH,
                                       1.0f), XmlUtil.getAttribute(node,
                                           ATTR_LINESTYLE, 1));

        lld.setVisible(XmlUtil.getAttribute(node, ATTR_VISIBLE, true));
        lld.setMinValue(min);
        lld.setMaxValue(max);

        return lld;
    }



    /**
     * Return the list of {@link MapData} objects
     *
     * @return List of map data
     */
    public List getMapData() {
        return mapDatum;
    }

    /**
     * Return the latitude state
     *
     * @return The Lat. state
     */
    public LatLonData getLatData() {
        return ((LatLonPanel) latLonPanels.get(0)).getLatLonData();
    }

    /**
     * The Longitude state
     *
     * @return The lon state
     */
    public LatLonData getLonData() {
        return ((LatLonPanel) latLonPanels.get(1)).getLatLonData();
    }



}


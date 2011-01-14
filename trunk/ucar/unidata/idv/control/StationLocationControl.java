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
import ucar.unidata.geoloc.Bearing;

import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.gis.SpatialGrid;


import ucar.unidata.idv.*;


import ucar.unidata.idv.flythrough.FlythroughPoint;



import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.PropertyFilter;
import ucar.unidata.ui.TableSorter;

import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.StationLocationDisplayable;
import ucar.visad.display.StationModelDisplayable;


import visad.*;


import visad.georef.*;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;

import visad.georef.NamedLocation;
import visad.georef.NamedLocationTuple;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;


import java.io.File;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;




/**
 * Class to display a set of locations
 *
 * @author MetApps Development Team
 * @version $Revision: 1.112 $ $Date: 2007/06/08 20:00:51 $
 */


public class StationLocationControl extends StationModelControl {

    /**
     * Synchronize around the setStations
     */
    private final Object DISPLAYABLE_MUTEX = new Object();

    /** List of displayed stations */
    List displayedStations = new ArrayList();

    /** List of sorted displayed stations */
    List sortedDisplayedStations = new ArrayList();


    /** The displayable */
    private StationLocationDisplayable locationDisplayable;

    /** the displayable */
    private StationLocationDisplayable selectedDisplayable;

    /** Shows any lines */
    private LineDrawing coordDisplayable;

    /**
     * In case we have coord lines this is the place holder in the gui where we add
     *   widgets
     */
    private JPanel coordAttributePanel;

    /** In case we have coord lines this is the label in the gui */
    private JLabel coordAttributeLabel;


    /** List of (String) station table names being displayed */
    private List stationTableNames = new ArrayList();


    /** DO we use the normal station model or use the fixed symbols */
    private boolean useStationModel = false;


    /** gui tabbed pane */
    private JTabbedPane tabbedPane;


    /** The panel for setting the station model */
    private JComponent stationModelPanel;

    /** The panel for setting the fixed symbols */
    private JComponent symbolPanel;


    /** Do we show the details of the clicked on location in the legend */
    private boolean readoutInLegend = false;


    /** station table name */
    private String stationTableName = null;


    /** extra stations */
    private Hashtable extraStations = new Hashtable();

    /** Should we look  forthe closest location on a click */
    private boolean enabled = true;

    /** Do we automatically center the display on a mouse click */
    private boolean centerOnClick = true;

    /** Shows the list of station tables */
    private JList stationJList;

    /** Shows the text description of the closest location */
    private JEditorPane readoutText;

    /** Holds the location details in the legend */
    private JScrollPane readoutSP;

    /** Forthe location details in the legend */
    private JPanel readoutComp;

    /** For the location details in the legend */
    private JComponent readoutLegendHolder;

    /** For the location details in the legend */
    private JComponent readoutGuiHolder;


    /** Keeps the selected locations */
    private List selectionList = new ArrayList();


    /**
     *   Keep around the list of the last decluttered stations for
     *   use when we have locked the decluttering.
     */
    private List lastDeclutteredStationList;


    /** the symbol type */
    private int symbolType = StationLocationDisplayable.SYMBOL_CIRCLE;

    /** the ID type */
    private int idType = StationLocationDisplayable.ID_ID;

    /** flag for showing the symbol */
    private boolean showSymbol = true;

    /** flag for showing the id */
    private boolean showId = true;


    /** Shows displayed locations */
    private LocationTable locationsTable;

    /** Shows displayed locations */
    private LocationTableModel locationsTableModel;

    /** Shows all of the locations */
    private LocationTable allLocationsTable;

    /** Shows all of the locations */
    private LocationTableModel allLocationsTableModel;



    /**
     * Default cstr; sets attribute flags
     */
    public StationLocationControl() {
        setStationModelName("Location");
        setShouldUseAltitude(false);
        setAttributeFlags(FLAG_COLOR);
    }


    /**
     * Clear out the station table names
     */
    public void initAsPrototype() {
        super.initAsPrototype();
        stationTableNames = new ArrayList();
    }



    /**
     * Called to make this kind of Display Control; also calls code to
     * made the Displayable.  This method is called from inside
     * DisplayControlImpl.init(several args).  This implementation
     * gets the list of stationTables to be used.
     *
     * @param dataChoice    the DataChoice of the moment -
     *                      not used yet by this implementation; can be null.
     *
     * @return  true if successful
     *
     * @throws  VisADException  there was a VisAD error
     * @throws  RemoteException  there was a remote error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        lastDeclutteredStationList = null;
        if ((stationTableName != null) && (stationTableNames.size() == 0)) {
            stationTableNames.add(stationTableName);
            stationTableName = null;
        }

        if (dataChoice != null) {
            try {
                Data d = dataChoice.getData(null);
                if (d instanceof visad.Text) {
                    loadXml(((visad.Text) d).getValue());
                }
            } catch (Exception exc) {
                logException("Error reading xml", exc);
                return false;
            }
        }
        return super.init(dataChoice);
    }


    /**
     * Load the location xml
     *
     * @param filename xml file
     *
     * @throws Exception On badness
     */
    private void loadXml(String filename) throws Exception {
        Trace.call1("StationLocationControl making stations");
        NamedStationTable table =
            NamedStationTable.createStationTableFromFile(filename);
        Trace.call2("StationLocationControl making stations");
        String tableName = table.getName();
        if ( !stationTableNames.contains(tableName)) {
            stationTableNames.add(tableName);
        }
        extraStations.put(tableName, table);
    }


    /**
     * Overwrite base class method so we don't show the chart
     *
     * @return false
     */
    protected boolean isChartEnabled() {
        return false;
    }



    /** _more_          */
    private boolean checkedCursorReadout = false;

    /** _more_          */
    private boolean doCursorReadout = false;

    /**
     * _more_
     *
     * @param el _more_
     * @param animationValue _more_
     * @param animationStep _more_
     * @param samples _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getCursorReadoutInner(EarthLocation el,
                                         Real animationValue,
                                         int animationStep,
                                         List<ReadoutInfo> samples)
            throws Exception {
        if ( !checkedCursorReadout) {
            List stations = getStationList();
            if (stations.size() == 0) {
                return null;
            }
            NamedStationImpl tmp        = (NamedStationImpl) stations.get(0);
            Hashtable        properties = tmp.getProperties();
            if (properties.get("imageurl") != null) {
                doCursorReadout = true;
            }
            checkedCursorReadout = true;
        }

        if ( !doCursorReadout) {
            return null;
        }
        List             stations    = getStationList();
        NamedStationImpl closest     = null;
        double           minDistance = 0;
        LatLonPointImpl llp =
            new LatLonPointImpl(
                el.getLatitude().getValue(CommonUnit.degree),
                el.getLongitude().getValue(CommonUnit.degree));
        for (Iterator iter = stations.iterator(); iter.hasNext(); ) {
            NamedStationImpl station = (NamedStationImpl) iter.next();
            EarthLocation    el2     = station.getEarthLocation();
            LatLonPointImpl llp2 =
                new LatLonPointImpl(
                    el2.getLatitude().getValue(CommonUnit.degree),
                    el2.getLongitude().getValue(CommonUnit.degree));
            Bearing bearing  = Bearing.calculateBearing(llp, llp2, null);

            double  distance = bearing.getDistance();
            if ((closest == null) || (distance < minDistance)) {
                minDistance = distance;
                closest     = station;
            }
        }

        if (closest != null) {
            Hashtable properties = closest.getProperties();
            String    url        = (String) properties.get("imageurl");
            if (url != null) {
                ReadoutInfo info = new ReadoutInfo(this, null,
                                       closest.getEarthLocation(), null);
                info.setImageUrl(url);
                info.setImageName(closest.getName());
                samples.add(info);
            }
        }
        return null;
    }


    /**
     * get MapProjection of data to display
     *
     * @return The native projection of the data
     */
    public MapProjection getDataProjection() {
        return null;
    }


    /**
     * Do we have a map projection
     *
     * @return true
     */
    public boolean hasMapProjection() {
        return true;
    }


    /**
     * Get the MapProjection for this data; if have a single point data object
     * make synthetic map projection for location
     * @return MapProjection  for the data
     */
    public MapProjection getDataProjectionForMenu() {
        try {
            List   stations = getStationList();
            double minX     = Double.POSITIVE_INFINITY;
            double maxX     = Double.NEGATIVE_INFINITY;
            double minY     = Double.POSITIVE_INFINITY;
            double maxY     = Double.NEGATIVE_INFINITY;
            int    cnt      = 0;
            for (Iterator iter = stations.iterator(); iter.hasNext(); ) {
                NamedStationImpl station = (NamedStationImpl) iter.next();
                NamedLocation    ob      = station.getNamedLocation();
                LatLonPoint      llp = ob.getEarthLocation().getLatLonPoint();
                double lat = llp.getLatitude().getValue(CommonUnit.degree);
                double lon = llp.getLongitude().getValue(CommonUnit.degree);
                if ((lat == lat) && (lon == lon)) {
                    cnt++;
                    if (Math.abs(lat) <= 90) {
                        minY = Math.min(minY, lat);
                        maxY = Math.max(maxY, lat);
                    }
                    if (Math.abs(lon) <= 180) {
                        minX = Math.min(minX, lon);
                        maxX = Math.max(maxX, lon);
                    }
                }
            }

            if (cnt < 1) {
                return null;
            }
            if (cnt == 1) {
                minX -= 2;
                maxX += 2;
                minY -= 2;
                maxY += 2;
            }
            return ucar.visad.Util.makeMapProjection(minY, minX, maxY, maxX);
        } catch (Exception exc) {
            logException("Error reading xml", exc);
            return null;
        }
    }



    /**
     * The data changed. Reload the display.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void reloadDataSource() throws RemoteException, VisADException {
        resetData();
    }

    /**
     * What label to use for the data projection
     *
     * @return projection label
     */
    protected String getDataProjectionLabel() {
        return "Use Projection From Locations";
    }


    /**
     * This gets called when the control has received notification of a
     * dataChange event.
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected void resetData() throws VisADException, RemoteException {
        if ( !getHaveInitialized()) {
            return;
        }
        List choices = getDataChoices();
        if (choices.size() == 0) {
            return;
        }
        stationTableNames = new ArrayList();
        extraStations     = new Hashtable();
        DataChoice dataChoice = (DataChoice) choices.get(0);
        Data       d          = dataChoice.getData(null);
        if (d instanceof visad.Text) {
            try {
                String xml = ((visad.Text) d).getValue();
                loadXml(xml);
                stationsChanged();
            } catch (Exception exc) {
                logException("Error reading xml", exc);
            }
        }
    }



    /**
     * Init is done
     */
    public void initDone() {
        super.initDone();
    }


    /**
     * Station model has changed.
     *
     * @param changedModel The changed model
     */
    protected void handleChangedStationModel(StationModel changedModel) {
        if (useStationModel) {
            super.handleChangedStationModel(changedModel);
        }
    }


    /**
     * Called by the init method to create the
     * <code>StationModelDisplayable</code> used for this instance.
     *
     * @return  this instance's <code>StationModelDisplayable</code>
     *
     * @throws  VisADException  there was an error creating the Displayable.
     * @throws  RemoteException  there was an error creating the Displayable
     */
    protected StationModelDisplayable createStationModelDisplayable()
            throws VisADException, RemoteException {

        locationDisplayable = new StationLocationDisplayable(
            "location displayable", getControlContext().getJythonManager());
        locationDisplayable.setShouldUseAltitude(false);
        addDisplayable(locationDisplayable, FLAG_COLOR | FLAG_ZPOSITION);



        selectedDisplayable = new StationLocationDisplayable(
            "selected displayable", getControlContext().getJythonManager());
        selectedDisplayable.setShouldUseAltitude(false);
        addDisplayable(selectedDisplayable, FLAG_COLOR | FLAG_ZPOSITION);
        selectedDisplayable.setRotateShapes(true);

        StationModel sm =
            getControlContext().getStationModelManager()
                .getSelectedStationModel();
        if (sm != null) {
            selectedDisplayable.setStationModel(sm);
        }
        updateSelectedDisplayable();

        return locationDisplayable;
    }


    /**
     * _more_
     *
     * @param myDisplay _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void initDisplayable(StationModelDisplayable myDisplay)
            throws VisADException, RemoteException {
        super.initDisplayable(myDisplay);
        myDisplay.setRotateShapes(true);
    }



    /**
     * Override the superclass method since currently, the DataChoice
     * for this instance is null or not used.
     *
     * @param choice <code>DataChoice</code>
     *
     * @return true if everything worked.
     *
     * @throws  VisADException  there was an error creating/setting the data.
     * @throws  RemoteException  there was an error creating/setting the data
     *          for a remote object.
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if (getHaveInitialized()) {
            loadDataInAWhile();
        }
        return true;
    }


    /**
     * _more_
     */
    protected void loadDataInAWhile() {
        super.loadDataInAWhile();
        try {
            updateSelectedDisplayable();
        } catch (Exception excp) {
            logException("Updating selected displayable", excp);
        }
    }


    /**
     * Handle when the user clicks in the main display
     *
     * @param el The location
     * @param event The event
     */
    protected void handleMousePressed(EarthLocation el, DisplayEvent event) {
        if ( !isGuiShown()) {
            return;
        }

        if ( !isLeftButtonDown(event)) {
            return;
        }
        if (displayedStations == null) {
            return;
        }
        if ( !getDisplayVisibility()) {
            return;
        }
        if ( !getEnabled() || !getHaveInitialized()
                || (getMakeWindow() && !getWindowVisible())) {
            return;
        }
        try {
            InputEvent inputEvent = event.getInputEvent();
            if (inputEvent.isShiftDown()) {
                return;
            }
            if ( !inputEvent.isControlDown()) {
                selectionList.clear();
            }
            Bearing          bearing  = null;
            NamedStationImpl closest  = null;
            double           distance = Double.MAX_VALUE;
            EarthLocation    minEL    = null;
            List             stations = new ArrayList(displayedStations);
            for (Iterator iter = stations.iterator(); iter.hasNext(); ) {
                NamedStationImpl station = (NamedStationImpl) iter.next();
                NamedLocation    ob      = station.getNamedLocation();

                if ((Math.abs(ob.getEarthLocation().getLatLonPoint()
                        .getLatitude().getValue()) > 90) || (Math
                            .abs(ob.getEarthLocation().getLatLonPoint()
                                .getLongitude().getValue()) > 360)) {
                    continue;
                }
                bearing = ucar.visad.Util.calculateBearing(
                    ob.getEarthLocation().getLatLonPoint(),
                    el.getLatLonPoint(), bearing);
                double tmpDistance = bearing.getDistance();
                if (tmpDistance < distance) {
                    closest  = station;
                    distance = tmpDistance;
                    minEL    = ob.getEarthLocation();
                }
            }
            if (closest != null) {
                int[] obScreen = earthToScreen(minEL);
                double screenDistance = GuiUtils.distance(obScreen[0],
                                            obScreen[1], event.getX(),
                                            event.getY());
                if (screenDistance > 50) {
                    return;
                }
                if ( !selectionList.contains(closest)) {
                    selectionList.add(closest);
                    if (tabbedPane != null) {
                        //                        tabbedPane.setSelectedIndex(1);
                    }
                } else if (inputEvent.isControlDown()) {
                    selectionList.remove(closest);
                }
            }
            selectedStationsChanged(selectionList);
            showSelectedInReadout();

            if (centerOnClick) {
                getNavigatedDisplay().center(el, true);
            }


        } catch (Exception excp) {
            logException("Finding closest location", excp);
        }
    }

    /**
     * The list of stations that changed
     *
     * @param selectionList the list
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void selectedStationsChanged(List selectionList)
            throws VisADException, RemoteException {
        updateSelectedDisplayable();
    }


    /**
     * set the selected stations
     *
     * @param stations the selected stations
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setSelectedStations(List<NamedStationImpl> stations)
            throws VisADException, RemoteException {
        selectionList = new ArrayList(stations);
        updateSelectedDisplayable();
    }

    /**
     * updates the displayable when anything changes.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void updateSelectedDisplayable()
            throws VisADException, RemoteException {
        if (selectedDisplayable == null) {
            return;
        }
        StationModel sm =
            getControlContext().getStationModelManager()
                .getSelectedStationModel();
        if (sm != null) {
            selectedDisplayable.setStationModel(sm);
        }
        selectedDisplayable.setStations(selectionList);
    }

    /**
     * set the scale factor on the displayable
     *
     * @param f the scale
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setScaleOnDisplayable(float f)
            throws RemoteException, VisADException {
        super.setScaleOnDisplayable(f);
        if (selectedDisplayable != null) {
            selectedDisplayable.setScale(f);
        }
    }




    /**
     * Clean up html
     *
     * @param s html
     *
     * @return html_
     */
    private String cleanupHtml(String s) {
        s = StringUtil.replace(s, "/>", ">");
        s = s.trim();
        return s;
    }

    /**
     * _more_
     *
     * @param location _more_
     *
     * @return _more_
     */
    private StringBuffer getHtml(NamedStationImpl location) {
        StringBuffer       sb         = new StringBuffer();
        Hashtable          properties = location.getProperties();
        Enumeration        keys       = properties.keys();
        NamedLocation      locationOb = location.getNamedLocation();
        EarthLocation      locationEl = locationOb.getEarthLocation();
        LatLonPoint        llp        = locationEl.getLatLonPoint();
        DisplayConventions dc         = getDisplayConventions();
        String llLabel = dc.formatLatLon(llp.getLatitude().getValue()) + "/"
                         + dc.formatLatLon(llp.getLongitude().getValue());

        StringBuffer entrySB = new StringBuffer();
        entrySB.append("<table>\n");
        entrySB.append("<tr><td><b>Name</b>:</td><td> " + location.getName()
                       + "</td></tr>\n");
        entrySB.append("<tr><td><b>Lat/Lon</b>:</td><td> " + llLabel
                       + "</td></tr>\n");

        String description = "";
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            String lbl = key.toString();
            if (lbl.equalsIgnoreCase("name") || lbl.equalsIgnoreCase("lat")
                    || lbl.equalsIgnoreCase("lon")) {
                continue;
            }
            if (lbl.equalsIgnoreCase("description")) {
                description = (String) properties.get(key);
                continue;
            }
            lbl = lbl.substring(0, 1).toUpperCase() + lbl.substring(1);
            entrySB.append("<tr valign=\"top\"><td><b>" + lbl
                           + "</b>&nbsp;</td><td> " + properties.get(key)
                           + "</td></tr>\n");
        }
        entrySB.append("</table>\n");
        String html = cleanupHtml(description);
        sb.append(html);
        sb.append(entrySB);
        return sb;

    }


    /**
     * Display the selected location in the gui
     */
    private void showSelectedInReadout() {
        if (selectionList.size() == 0) {
            readoutText.setText("<html></html>");
            return;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < selectionList.size(); i++) {
            NamedStationImpl location =
                (NamedStationImpl) selectionList.get(i);
            sb.append(getHtml(location));
        }

        if ((readoutText != null) && (sb != null)) {
            readoutText.setText("<html>" + getStationTableDescription()
                                + sb.toString() + "</html>");
            GuiUtils.scrollToTop(readoutText);
        }
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
        return GuiUtils.vbox(parentComp, readoutLegendHolder);
    }



    /**
     * Get the extra label used for the legend.
     *
     * @param labels  labels to add to
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        for (int i = 0; (i < stationTableNames.size()) && (i < 3); i++) {
            labels.add(stationTableNames.get(i));
        }
    }

    /**
     * Get the label used for the a menu.
     *
     * @return menu label.
     */
    public String getMenuLabel() {
        if (stationTableNames.size() > 0) {
            String label = "Locations:";
            for (int i = 0; (i < stationTableNames.size()) && (i < 3); i++) {
                label = label + " " + stationTableNames.get(i);
            }
            return label;
        }
        return super.getMenuLabel();
    }


    /**
     * <p>Creates and returns the {@link ucar.unidata.data.DataInstance}
     * corresponding to a {@link ucar.unidata.data.DataChoice}. Returns
     * <code>null</code> if the {@link ucar.unidata.data.DataInstance} was
     * somehow invalid.</p>
     *
     * <p>This method is invoked by the overridable method {@link
     * #setData(DataChoice)}.</p>
     *
     * @param dataChoice       The {@link ucar.unidata.data.DataChoice} from
     *                         which to create a
     *                         {@link ucar.unidata.data.DataInstance}.
     * @return                 for this instance, null.
     * @throws VisADException  if a VisAD Failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        return null;
    }




    /**
     * Return the list of names that shows up in the filter gui names combob box.
     *
     * @return List of filter names
     */
    protected List getFilterNames() {
        List stations = getStationList();
        if ((stations == null) || (stations.size() == 0)) {
            return Misc.newList("Name");
        }
        NamedStationImpl station    = (NamedStationImpl) stations.get(0);
        Hashtable        properties = station.getProperties();
        Enumeration      keys       = properties.keys();
        List             keyList    = new ArrayList();
        keyList.add("--");
        boolean haveName = false;

        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if ( !haveName && key.toLowerCase().equals("name")) {
                haveName = true;
            }
            keyList.add(key);
        }
        if ( !haveName) {
            keyList.add(0, "Name");
        }

        return keyList;
    }


    /**
     * Get the contents of the details html
     *
     * @return The contents of the details
     */
    protected String getDetailsContents() {
        StringBuffer sb    = new StringBuffer(super.getDetailsContents());
        List         names = getFilterNames();
        names.remove(0);
        sb.append("<b>Location property names:</b><ul><li>");
        sb.append(StringUtil.join("<li>", names));
        sb.append("</ul>");
        return sb.toString();
    }


    /**
     * Filter the list of stations
     *
     * @param stations  input list to filter
     * @return  filtered list
     */
    private List filter(List stations) {
        if (stations == null) {
            return new ArrayList();
        }
        initFilters();
        if ( !getFiltersEnabled()) {
            return stations;
        }
        List    result   = new ArrayList();
        boolean matchAll = getMatchAll();
        for (Iterator iter = stations.iterator(); iter.hasNext(); ) {
            NamedStationImpl station          =
                (NamedStationImpl) iter.next();
            boolean          ok               = true;
            Hashtable        objectProperties = station.getProperties();
            for (int i = 0; i < filters.size(); i++) {
                PropertyFilter filter = (PropertyFilter) filters.get(i);
                Object         objectValue;
                if (filter.getName().equals("Name")) {
                    objectValue = station.getName();
                } else {
                    objectValue = objectProperties.get(filter.getName());
                }
                if (objectValue == null) {
                    //                    System.err.println("no property:" + filter.getName());
                    ok = false;
                    break;
                }
                ok = filter.ok(objectValue);
                if (matchAll && !ok) {
                    break;
                }
                if ( !matchAll && ok) {
                    break;
                }
            }
            if (ok) {
                result.add(station);
            }
        }
        return result;
    }

    /**
     * Spatial subset the stations
     *
     * @param stations stations
     *
     * @return stations
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private List subsetStations(List stations)
            throws VisADException, RemoteException {
        Rectangle2D rbounds = calculateRectangle();
        if (rbounds == null) {
            return new ArrayList();
        }
        LinearLatLonSet bounds = calculateLatLonBounds(rbounds);
        if (bounds == null) {
            return new ArrayList();
        }
        Unit[] units = bounds.getSetUnits();
        int latIndex =
            (((RealType) ((SetType) bounds.getType()).getDomain()
                .getComponent(0)).equals(RealType.Latitude) == true)
            ? 0
            : 1;
        Vector    v      = new Vector();
        float[][] values = new float[2][1];
        Trace.call1("StationLocationControl.subset");
        Rectangle screenBounds = getScreenBounds();

        for (int i = 0; i < stations.size(); i++) {
            NamedStationImpl st = (NamedStationImpl) stations.get(i);
            EarthLocation    el = st.getEarthLocation();
            //For now just use the screen bounds
            /*
            int[] xy = earthToScreen(el);
            if (screenBounds.contains(xy[0], xy[1])) {
                v.add(st);
            }
            */
            values[0][0] = (float) el.getLatitude().getValue(units[latIndex]);
            values[1][0] =
                (float) el.getLongitude().getValue(units[1 - latIndex]);
            float[][] grids = bounds.valueToGrid(values);

            if ((grids[0][0] == grids[0][0])
                    && (grids[1][0] == grids[1][0])) {  //not NaN
                //                System.err.println (st +" contains:" + );
                v.add(st);  // is in the bounds
            }
        }
        Trace.call2("StationLocationControl.subset");

        return v;
    }

    /**
     * update the table
     */
    private void updateTable() {
        final List sortedStations    = Misc.sort(displayedStations);
        final List allSortedStations = Misc.sort(getStationList());
        GuiUtils.invokeInSwingThread(new Runnable() {
            public void run() {
                locationsTableModel.setLocations(sortedStations);
                allLocationsTableModel.setLocations(allSortedStations);
            }
        });

    }


    /**
     * Loads the data into the <code>StationModelDisplayable</code>.
     * Declutters the stations if necessary.
     */
    public void loadData() {

        try {
            updateDisplayable();
            List    listOfStations   = getStationList();
            List    filteredStations = filter(listOfStations);
            boolean alwaysShow       = !getOnlyShowFiltered()
                                       && haveFilters();
            if (getOnlyShowFiltered()) {
                listOfStations = filteredStations;
            }

            listOfStations = subsetStations(listOfStations);


            if (getDeclutter()) {
                if (stationsLocked && (lastDeclutteredStationList != null)) {
                    listOfStations = lastDeclutteredStationList;
                } else {
                    Trace.call1("StationLocationControl.declutter");
                    listOfStations = declutter(listOfStations, (alwaysShow
                            ? filteredStations
                            : null));
                    Trace.call2("StationLocationControl.declutter");
                }
                lastDeclutteredStationList = listOfStations;
            } else {
                lastDeclutteredStationList = null;
            }


            if (alwaysShow) {
                for (int i = 0; i < filteredStations.size(); i++) {
                    Object selectedLocation = filteredStations.get(i);
                    if ( !listOfStations.contains(selectedLocation)) {
                        listOfStations.add(selectedLocation);
                    }
                }
            }


            addSelectedToList(listOfStations);


            if (locationsTable != null) {
                if ((locationsTable.lastClicked != null)
                        && !listOfStations.contains(
                            locationsTable.lastClicked)) {
                    listOfStations.add(locationsTable.lastClicked);
                }

                if ((allLocationsTable.lastClicked != null)
                        && !listOfStations.contains(
                            allLocationsTable.lastClicked)) {
                    listOfStations.add(allLocationsTable.lastClicked);
                }

            }

            if (listOfStations != null) {
                synchronized (DISPLAYABLE_MUTEX) {
                    displayedStations = listOfStations;
                    if (selectedDisplayable != null) {
                        //                        selectedDisplayable.setStations(listOfStations);
                    }

                    if (locationDisplayable != null) {
                        Trace.call1("setStations");
                        locationDisplayable.setStations(listOfStations);
                        Trace.call2("setStations");
                        if (locationsTableModel != null) {
                            updateTable();
                        }
                        List datum = null;
                        for (int i = 0; i < listOfStations.size(); i++) {
                            NamedStationImpl st =
                                (NamedStationImpl) listOfStations.get(i);
                            List coords = st.getCoords();
                            if (coords == null) {
                                continue;
                            }
                            for (int coordIdx = 0; coordIdx < coords.size();
                                    coordIdx++) {
                                float[][] coord = Misc.toFloat(
                                                      (double[][]) coords.get(
                                                          coordIdx));
                                Data data;
                                RealTupleType coordMathType = (coord.length
                                                               == 2)
                                        ? new RealTupleType(
                                            RealType.Longitude,
                                            RealType.Latitude)
                                        : new RealTupleType(
                                            RealType.Longitude,
                                            RealType.Latitude,
                                            RealType.Altitude);
                                data = new Gridded3DSet(coordMathType, coord,
                                        coord[0].length);
                                if (datum == null) {
                                    datum = new ArrayList();
                                }
                                datum.add(data);
                            }
                        }
                        if (datum != null) {
                            if (coordDisplayable == null) {
                                coordDisplayable =
                                    new LineDrawing("station lines");
                                addDisplayable(coordDisplayable,
                                        FLAG_COLOR | FLAG_LINEWIDTH);
                                if (coordAttributeLabel == null) {
                                    coordAttributeLabel = new JLabel("");
                                    coordAttributePanel =
                                        new JPanel(new BorderLayout());
                                }
                                coordAttributeLabel.setText("Line Width:");
                                coordAttributePanel.add(
                                    BorderLayout.CENTER,
                                    getLineWidthWidget().getContents(false));

                            }
                            SampledSet[] coordArray =
                                (SampledSet[]) datum.toArray(
                                    new SampledSet[datum.size()]);
                            UnionSet unionSet = new UnionSet(coordArray);
                            coordDisplayable.setData(unionSet);
                        }


                    }
                }
            }
        } catch (Exception excp) {
            logException("loading data ", excp);
        }

    }

    /**
     * add the selected stations to the given list
     *
     * @param listOfStations list to add to
     */
    protected void addSelectedToList(List listOfStations) {
        for (int i = 0; i < selectionList.size(); i++) {
            Object selectedLocation = selectionList.get(i);
            if ( !listOfStations.contains(selectedLocation)) {
                listOfStations.add(selectedLocation);
            }
        }
    }

    /**
     * Get the station table description.
     *
     * @return  the station list
     */
    protected String getStationTableDescription() {
        StringBuffer sb  = new StringBuffer();
        int          cnt = 0;
        for (int j = 0; j < stationTableNames.size(); j++) {
            String name = (String) stationTableNames.get(j);
            NamedStationTable stationTable =
                getControlContext().getResourceManager().findLocations(name);
            if (stationTable == null) {
                stationTable = (NamedStationTable) extraStations.get(name);
            }
            if (stationTable != null) {
                if (stationTable.getDescription() != null) {
                    if (cnt != 0) {
                        sb.append("<br>");
                    }
                    sb.append(stationTable.getDescription());
                    cnt++;
                }

            }
        }
        return sb.toString();
    }


    /**
     * Get the station List.
     *
     * @return  the station list
     */
    protected List getStationList() {
        List stations = new ArrayList();
        for (int j = 0; j < stationTableNames.size(); j++) {
            String name = (String) stationTableNames.get(j);
            NamedStationTable stationTable =
                getControlContext().getResourceManager().findLocations(name);
            if (stationTable != null) {
                stations.addAll(stationTable.values());
            } else {
                stationTable = (NamedStationTable) extraStations.get(name);
                if (stationTable != null) {
                    stations.addAll(stationTable.values());
                }
            }
        }


        return stations;
    }

    /**
     * Get the current station model view.
     *
     * @return station model layout
     */
    public StationModel getMyStationModel() {
        if ((locationDisplayable != null) && !useStationModel) {
            return locationDisplayable.getStationModel();
        } else {
            return super.getStationModel();
        }
    }


    /**
     * Declutter the list of stations based on the bounding box
     * of the display.
     *
     * @param stations  List of stations (NamedStationImpl-s).
     * @param preLoad preload
     * @return list of stations.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private List declutter(List stations, List preLoad)
            throws VisADException, RemoteException {
        if ((stations == null) || stations.isEmpty()) {
            return stations;
        }
        Rectangle obView  = getMyStationModel().getBounds();
        float     myScale = getScale() * .0025f * getDeclutterFilter();

        Rectangle2D viewBounds = new Rectangle2D.Double(obView.getX()
                                     * myScale, obView.getY() * myScale,
                                         obView.getWidth() * myScale,
                                         obView.getHeight() * myScale);
        Vector      v           = new Vector();
        SpatialGrid stationGrid = new SpatialGrid(100, 100);
        stationGrid.clear();
        stationGrid.setGrid(getBounds(), viewBounds);
        double[] xyz = null;
        Rectangle2D.Double obBounds = new Rectangle2D.Double(0, 0,
                                          viewBounds.getWidth(),
                                          viewBounds.getHeight());
        double vbX = viewBounds.getX();
        double vbY = viewBounds.getY();

        if (preLoad != null) {
            for (Iterator iter = preLoad.iterator(); iter.hasNext(); ) {
                NamedStationImpl station = (NamedStationImpl) iter.next();
                EarthLocation    el      = station.getEarthLocation();
                xyz        = earthToBox(el);
                obBounds.x = xyz[0] + vbX;
                obBounds.y = xyz[1] + vbY;
                stationGrid.markIfClear(obBounds, el);
                stations.remove(station);
            }
        }

        for (Iterator iter = stations.iterator(); iter.hasNext(); ) {
            NamedStationImpl station = (NamedStationImpl) iter.next();
            EarthLocation    el      = station.getEarthLocation();
            xyz        = earthToBox(el);
            obBounds.x = xyz[0] + vbX;
            obBounds.y = xyz[1] + vbY;
            if (stationGrid.markIfClear(obBounds, el)) {
                v.add(station);
            }
        }
        return v;
    }


    /**
     * Set the station table name for this instance.  Used by persistence
     * with set/get methods.
     *
     * @param value name of station table.
     */
    public void setStationTableName(String value) {
        stationTableName = value;
    }


    /**
     *  Set the StationTableNames property.
     *
     *  @param value The new value for StationTableNames
     */
    public void setStationTableNames(List value) {
        stationTableNames = value;
    }

    /**
     *  Get the StationTableNames property.
     *
     *  @return The StationTableNames
     */
    public List getStationTableNames() {
        return stationTableNames;
    }


    /**
     * Called by the init method to create the contents of this
     * <code>DisplayControl</code>'s UI.
     *
     * @return the container for the UI.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        return doMakeTabs(true, true);
    }

    /**
     * Make the main tabbed pane gui
     *
     * @param showDataSets show the datasets component
     * @param showFilters show the filters component
     *
     * @return the tabbed pane
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected JTabbedPane doMakeTabs(boolean showDataSets,
                                     boolean showFilters)
            throws VisADException, RemoteException {
        readoutLegendHolder = new JPanel(new BorderLayout());
        readoutGuiHolder    = new JPanel(new BorderLayout());

        readoutText         = new JEditorPane();
        GuiUtils.addLinkListener(readoutText);
        readoutText.setEditable(false);
        readoutText.setContentType("text/html");


        readoutComp = new JPanel(new BorderLayout());
        readoutSP = new JScrollPane(
            readoutText, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        readoutComp.add(BorderLayout.CENTER, readoutSP);
        readoutGuiHolder.add(BorderLayout.CENTER, readoutComp);

        tabbedPane = GuiUtils.getNestedTabbedPane();


        JPanel readoutContents =
            GuiUtils.topCenter(
                GuiUtils.left(
                    GuiUtils.hbox(
                        GuiUtils.makeCheckbox(
                            "Listen for clicks", this,
                            "enabled"), GuiUtils.makeCheckbox(
                                "Center on selected", this,
                                "centerOnClick"))), readoutGuiHolder);
        //        tabbedPane.add("Selected", readoutContents);


        locationsTableModel = new LocationTableModel(sortedDisplayedStations);
        allLocationsTableModel = new LocationTableModel(getStationList());
        locationsTable         = new LocationTable(locationsTableModel) {
            public String toString() {
                return "locations";
            }
        };
        allLocationsTable = new LocationTable(allLocationsTableModel) {
            public String toString() {
                return "all locations";
            }
        };

        JTabbedPane locationTab = GuiUtils.getNestedTabbedPane();
        locationTab.add("All Locations", allLocationsTable.getScroller());
        locationTab.add("Displayed Locations", locationsTable.getScroller());
        locationTab.setPreferredSize(new Dimension(200, 250));
        readoutContents.setPreferredSize(new Dimension(200, 250));
        //        JSplitPane locationComp = GuiUtils.vsplit(
        //                                      locationTab,
        //                                      readoutContents,200,0.5);
        JComponent locationComp = GuiUtils.doLayout(new Component[] {
                                      locationTab,
                                      readoutContents }, 1, GuiUtils.WT_Y,
                                          new double[] { 1.0, 3.0 });
        //        locationComp.setOneTouchExpandable(true);
        tabbedPane.add("Display", doMakeDisplayPanel());
        tabbedPane.add("Locations", locationComp);
        if (showDataSets) {
            tabbedPane.add("Data Sets", doMakeStationListPanel());
        }
        if (showFilters) {
            tabbedPane.add("Filters", doMakeFilterGui(true));
        }
        return tabbedPane;
    }


    /**
     * Class LocationTable shows locations
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.112 $
     */
    private class LocationTable extends JTable {

        /** table model */
        LocationTableModel myTableModel;

        /** Sorter */
        TableSorter mySorter;

        /** Last location clicked */
        NamedStationImpl lastClicked;

        /** have we initialized */
        boolean initialized = false;


        /**
         * ctor
         *
         *
         * @param tableModel my model
         */
        public LocationTable(LocationTableModel tableModel) {
            this.myTableModel = tableModel;
            setModel(this.mySorter = new TableSorter(tableModel));
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setToolTipText(
                "<html>Click to center; <br>Shift-Click to center and zoom in<br>Control-Click to center and zoom out</html>");
            mySorter.setTableHeader(getTableHeader());

            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent ke) {
                    String k = ke.getKeyText(ke.getKeyCode()).toLowerCase();
                    if (k.length() > 1) {
                        return;
                    }
                    int selectedRow = getSelectedRow();
                    if (selectedRow < 0) {
                        selectedRow = 0;
                    } else {
                        NamedStationImpl station =
                            (NamedStationImpl) myTableModel.locations.get(
                                selectedRow);
                        if (k.compareTo(
                                station.getName().toLowerCase().substring(
                                    0, 1)) >= 0) {
                            selectedRow++;
                        } else {
                            selectedRow = 0;
                        }

                    }

                    for (int row = selectedRow;
                            row < myTableModel.locations.size(); row++) {
                        NamedStationImpl station =
                            (NamedStationImpl) myTableModel.locations.get(
                                row);
                        if (station.getName().toLowerCase().startsWith(k)) {
                            lastClicked = station;
                            setRowSelectionInterval(row, row);
                            GuiUtils.makeRowVisible(
                                (JTable) LocationTable.this, row);
                            try {
                                stationSelected(station);
                                if (getCenterOnClick()) {
                                    getNavigatedDisplay().center(
                                        station.getEarthLocation(), true);
                                }
                            } catch (Exception exc) {
                                logException("Setting location", exc);

                            }
                            break;
                        }
                    }

                }
            });

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }
                    final int row =
                        mySorter.modelIndex(rowAtPoint(e.getPoint()));
                    List locations = myTableModel.locations;
                    if ((row < 0) || (row >= locations.size())) {
                        return;
                    }
                    try {

                        NamedStationImpl station =
                            (NamedStationImpl) locations.get(row);
                        lastClicked = station;
                        if (e.isShiftDown() || e.isControlDown()) {
                            if (getCenterOnClick()) {
                                getNavigatedDisplay().centerAndZoom(
                                    station.getEarthLocation(), true,
                                    (e.isShiftDown()
                                     ? 2.0
                                     : 0.5));
                            }
                        } else {
                            if (getCenterOnClick()) {
                                getNavigatedDisplay().center(
                                    station.getEarthLocation(), true);
                            }
                        }
                        stationSelected(station);
                    } catch (Exception exc) {
                        logException("Setting location", exc);

                    }
                }
            });
            initialized = true;
        }

        /**
         * Utility
         *
         * @return The scroll pane
         */
        public JScrollPane getScroller() {
            //            this.setPreferredSize(new Dimension(300, 200));
            JScrollPane sp = new JScrollPane(this);
            sp.setPreferredSize(new Dimension(300, 200));
            return sp;
        }

        /**
         * Select the station
         *
         * @param station station
         *
         * @throws RemoteException On badness
         * @throws VisADException On badness
         */
        protected void stationSelected(NamedStationImpl station)
                throws VisADException, RemoteException {
            if ( !selectionList.contains(station)) {
                selectionList.clear();
                selectionList.add(station);
                selectedStationsChanged(selectionList);
                showSelectedInReadout();
            }
        }

        /**
         * Clear out the selected stations
         *
         * @throws RemoteException On badness
         * @throws VisADException On badness
         */
        protected void clearSelectedStations()
                throws VisADException, RemoteException {
            selectionList.clear();
            selectedStationsChanged(selectionList);
            showSelectedInReadout();
        }

        /**
         * Something changed
         *
         * @param event event
         */
        public void valueChanged(ListSelectionEvent event) {
            super.valueChanged(event);
            if (event.getValueIsAdjusting()) {
                return;
            }
            if ( !initialized) {
                return;
            }
            if ( !getEnabled()) {
                return;
            }

            List locations = myTableModel.locations;
            int  row       = getSelectedRow();
            if ((row < 0) || (row >= locations.size())) {
                return;
            }
            try {
                NamedStationImpl station =
                    (NamedStationImpl) locations.get(row);
                if (lastClicked == station) {
                    return;
                }
                if (getCenterOnClick()) {
                    getNavigatedDisplay().center(station.getEarthLocation(),
                            true);
                }
                stationSelected(station);
            } catch (Exception exc) {
                logException("Setting location", exc);

            }

        }




    }


    /**
     * Class LocationTableModel shows locations
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.112 $
     */
    private class LocationTableModel extends AbstractTableModel {

        /** locations */
        List locations;

        /** Attribute names */
        List attributes;


        /**
         * ctor
         *
         * @param locations locations to show_
         */
        public LocationTableModel(List locations) {
            this.locations = locations;
            attributes     = new ArrayList();
            List tmp = getStationList();
            if ((tmp != null) && (tmp.size() > 0)) {
                NamedStationImpl station    = (NamedStationImpl) tmp.get(0);
                Hashtable        properties = station.getProperties();
                Enumeration      keys       = properties.keys();
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    if ( !key.equals("name")) {
                        attributes.add(key);
                    }
                }
            }
        }

        /**
         * Set the locations
         *
         * @param l locations
         */
        public void setLocations(List l) {
            if ( !Misc.equals(l, locations)) {
                this.locations = l;
                fireTableStructureChanged();
            }
        }

        /**
         * is cell editable
         *
         * @param rowIndex rowindex
         * @param columnIndex colindex_
         *
         * @return is editable
         */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        /**
         * Num rows
         *
         * @return num rows
         */
        public int getRowCount() {
            return locations.size();
        }

        /**
         * num cols
         *
         * @return num cols
         */
        public int getColumnCount() {
            return 1 + attributes.size();
        }

        /**
         * get cell value
         *
         * @param row row
         * @param column col
         *
         * @return cell value
         */
        public Object getValueAt(int row, int column) {
            if (row >= locations.size()) {
                return "";
            }
            NamedStationImpl station = (NamedStationImpl) locations.get(row);
            if (column == 0) {
                String id   = station.getID();
                String name = station.getName();
                if ((id != null) && (id.trim().length() > 0)
                        && !Misc.equals(id, name)) {
                    return name + " (" + id + ")";
                }
                return name;
            }
            if (column > 0) {
                int index = column - 1;
                if ((index >= 0) && (index < attributes.size())) {
                    Object key = attributes.get(index);
                    Object o   = station.getProperties().get(key);
                    if (o != null) {
                        o = StringUtil.stripTags(o.toString());
                    }
                    return o;
                }
            }
            return "";
        }

        /**
         * col name
         *
         * @param column column
         *
         * @return col name
         */
        public String getColumnName(int column) {
            if (column == 0) {
                return "Name";
            }
            if (column > 0) {
                int index = column - 1;
                if ((index >= 0) && (index < attributes.size())) {
                    return (String) attributes.get(index);
                }
            }
            return "";
        }
    }


    /**
     * Make the display gui panel
     *
     * @return display gui panel
     */
    protected JComponent doMakeDisplayPanel() {

        List      comps  = new ArrayList();

        JCheckBox toggle = GuiUtils.makeCheckbox("", this, "declutter");
        comps.add(GuiUtils.rLabel("Declutter:"));
        comps.add(GuiUtils.hbox(Misc.newList(toggle, getLockButton(),
                                             new JLabel("Density: "),
                                             getDensityControl()), 5));

        comps.add(new JLabel(" "));
        comps.add(new JLabel(" "));
        Vector idItems = new Vector();
        for (int i = 0; i < StationLocationDisplayable.IDS.length; i++) {
            idItems.add(
                new TwoFacedObject(
                    StationLocationDisplayable.ID_NAMES[i],
                    new Integer(StationLocationDisplayable.IDS[i])));

        }


        final JComboBox idBox = new JComboBox(idItems);
        idBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                TwoFacedObject tfo = (TwoFacedObject) idBox.getSelectedItem();
                idType = ((Integer) tfo.getId()).intValue();
                updateDisplayable();
            }
        });


        Vector symbolItems = new Vector();
        for (int i = 0; i < StationLocationDisplayable.SYMBOLS.length; i++) {
            symbolItems.add(
                new TwoFacedObject(
                    StationLocationDisplayable.SYMBOL_NAMES[i],
                    new Integer(StationLocationDisplayable.SYMBOLS[i])));

        }


        final JComboBox symbolBox = new JComboBox(symbolItems);
        symbolBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                TwoFacedObject tfo =
                    (TwoFacedObject) symbolBox.getSelectedItem();
                symbolType = ((Integer) tfo.getId()).intValue();
                updateDisplayable();
            }
        });


        //        stationModelPanel = makeStationModelWidget();
        stationModelPanel = layoutModelWidget = new LayoutModelWidget(this,
                this, "setStationModelFromWidget", getStationModel());
        JRadioButton[] displayRbs =
            GuiUtils.makeRadioButtons(Misc.newList("Predefined:",
                "Layout Model:"), (useStationModel
                                   ? 1
                                   : 0), this, "buttonPressed");

        JCheckBox idShow     = GuiUtils.makeCheckbox("", this, "showId");
        JCheckBox symbolShow = GuiUtils.makeCheckbox("", this, "showSymbol");
        List symbolComps = Misc.newList(GuiUtils.rLabel("Id:  "), idBox,
                                        idShow);

        symbolComps.add(GuiUtils.rLabel("Symbol:  "));
        symbolComps.add(symbolBox);
        symbolComps.add(symbolShow);
        symbolComps.add(GuiUtils.rLabel("Color:  "));
        symbolComps.add(doMakeColorControl(getColor()));
        symbolComps.add(GuiUtils.filler());
        GuiUtils.tmpInsets = new Insets(4, 0, 0, 0);
        symbolPanel = GuiUtils.doLayout(symbolComps, 3, GuiUtils.WT_NYN,
                                        GuiUtils.WT_N);

        symbolPanel = GuiUtils.left(symbolPanel);


        if ( !useStationModel) {
            GuiUtils.enableTree(stationModelPanel, false);
        } else {
            GuiUtils.enableTree(symbolPanel, false);
        }

        JPanel displayPanel = GuiUtils.doLayout(new Component[] {
                                  displayRbs[0],
                                  displayRbs[1], symbolPanel,
                                  GuiUtils.top(
                                      GuiUtils.left(stationModelPanel)) }, 2,
                                          GuiUtils.WT_N, GuiUtils.WT_N);


        comps.add(GuiUtils.top(GuiUtils.rLabel("Display:")));
        comps.add(GuiUtils.left(displayPanel));



        comps.add(new JLabel(" "));
        comps.add(new JLabel(" "));

        comps.add(GuiUtils.top(GuiUtils.rLabel("Vertical Position:")));
        comps.add(doMakeVerticalPositionPanel());

        if (coordAttributeLabel == null) {
            coordAttributeLabel = new JLabel("");
            coordAttributePanel = new JPanel(new BorderLayout());
        }

        comps.add(coordAttributeLabel);
        comps.add(coordAttributePanel);


        GuiUtils.tmpInsets = new Insets(4, 4, 0, 4);
        JPanel topPanel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                            GuiUtils.WT_N);

        GuiUtils.enableComponents(densityComps, getDeclutter());
        return GuiUtils.top(topPanel);
    }




    /**
     * Make the list of stations panel
     *
     * @return The station list panel
     */
    protected JComponent doMakeStationListPanel() {
        stationJList = new JList();
        stationJList.setToolTipText(
            "<html>Press 'Up' or 'Down' arrow to move selected station list.<br>Press 'Delete' to remove selected station list.</html>");
        stationJList.setVisibleRowCount(4);
        stationJList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                Object selected = stationJList.getSelectedValue();
                if (selected == null) {
                    return;
                }
                if (e.getKeyCode() == e.VK_DOWN) {
                    int index = stationJList.getSelectedIndex();
                    if (index >= stationTableNames.size() - 1) {
                        return;
                    }
                    selected = stationTableNames.remove(index);
                    stationTableNames.add(index + 1, selected);
                } else if (e.getKeyCode() == e.VK_UP) {
                    int index = stationJList.getSelectedIndex();
                    if (index == 0) {
                        return;
                    }
                    selected = stationTableNames.remove(index);
                    stationTableNames.add(index - 1, selected);
                } else if (GuiUtils.isDeleteEvent(e)) {
                    stationTableNames.remove(selected);
                } else {
                    return;
                }
                stationsChanged();
            }
        });

        JScrollPane stationScroller = GuiUtils.makeScrollPane(stationJList,
                                          200, 100);
        stationJList.setListData(new Vector(stationTableNames));
        return stationScroller;
    }




    /**
     * The id or station button was pressed
     *
     * @param index which one
     */
    public void buttonPressed(int index) {
        if ((stationModelPanel == null) || (symbolPanel == null)) {
            return;
        }
        useStationModel = (index == 1);
        GuiUtils.enableTree(stationModelPanel, useStationModel);
        GuiUtils.enableTree(symbolPanel, !useStationModel);
        updateDisplayable();
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
        List namedStations = getStationList();
        if ((namedStations != null) && (namedStations.size() > 0)) {
            items.add(GuiUtils.makeMenuItem("Export Locations...", this,
                                            "exportLocations"));
        }
    }

    /**
     * _more_
     *
     * @param items _more_
     * @param forMenuBar _more_
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        MapViewManager mvm = getMapViewManager();
        if (mvm != null) {
            items.add(
                GuiUtils.setIcon(
                    GuiUtils.makeMenuItem(
                        "Show Flythrough", this, "showFlythrough",
                        null), "/auxdata/ui/icons/plane.png"));
        }

        super.getViewMenuItems(items, forMenuBar);
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void showFlythrough() throws Exception {
        MapViewManager        mvm            = getMapViewManager();
        List<FlythroughPoint> points = new ArrayList<FlythroughPoint>();
        List                  sortedStations = Misc.sort(displayedStations);
        for (int i = 0; i < sortedStations.size(); i++) {
            NamedStationImpl station =
                (NamedStationImpl) sortedStations.get(i);
            FlythroughPoint pt =
                new FlythroughPoint(station.getEarthLocation());
            pt.setDescription(getHtml(station).toString());
            points.add(pt);
        }

        mvm.flythrough(points);
    }



    /**
     * Write out the locations as an xml file
     */
    public void exportLocations() {
        PatternFileFilter xmlFilter = new PatternFileFilter(".+\\.xml",
                                          "Location XML Format", ".xml");

        String filename = FileManager.getWriteFile(xmlFilter, ".xml");
        if (filename == null) {
            return;
        }
        try {
            List stations = getStationList();
            String xml =
                NamedStationTable.getStationXml(IOUtil.getFileTail(filename),
                    null, stations);
            IOUtil.writeFile(new File(filename), xml);
        } catch (Exception exc) {
            logException("Writing locations", exc);
        }

    }

    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        ObjectListener listener = new ObjectListener("") {
            public void actionPerformed(ActionEvent ae, Object obj) {
                String name = ((NamedStationTable) obj).getFullName();
                stationTableNames.add(name);
                stationsChanged();
            }
        };
        List stationTables = getControlContext().getLocationList();
        items.add(
            GuiUtils.makeMenu(
                "Add Locations",
                NamedStationTable.makeMenuItems(stationTables, listener)));


        super.getEditMenuItems(items, forMenuBar);

    }


    /**
     * Get edit menu items
     *
     * @param items      list of menu items
     * @param forMenuBar  true if for the menu bar
     */
    protected void makeStationModelEditMenuItems(List items,
            boolean forMenuBar) {}




    /**
     * Update the gui and the display when the stations have changed
     */
    private void stationsChanged() {
        lastDeclutteredStationList = null;
        if (stationJList != null) {
            stationJList.setListData(new Vector(stationTableNames));
        }
        loadData();
        updateLegendLabel();

    }




    /**
     * Make a set of JRadioButtons from a list of values and names.
     *
     * @param  prefix  prefix for the action command.
     * @param  onId    which of these should be selected.
     * @param  ids     array of values for actions.
     * @param  names   names associated with each radio button.
     *
     * @return array of components.
     */
    private Component[] makeRadio(String prefix, int onId, int[] ids,
                                  String[] names) {
        ButtonGroup  typeGroup = new ButtonGroup();
        JRadioButton rb;
        Component[]  comps = new Component[ids.length];
        for (int i = 0; i < ids.length; i++) {
            rb = new JRadioButton(names[i], ids[i] == onId);
            rb.setActionCommand(prefix + ids[i]);
            rb.addActionListener(this);
            typeGroup.add(rb);
            comps[i] = rb;
        }
        return comps;
    }

    /**
     * Only public as a result of this being an ActionListener.
     *
     * @param ae    action event to check
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.startsWith("symbol")) {
            symbolType = new Integer(cmd.substring(6)).intValue();
            updateDisplayable();
        } else if (cmd.startsWith("id")) {
            idType = new Integer(cmd.substring(2)).intValue();
            updateDisplayable();
        } else {
            super.actionPerformed(ae);
        }
    }


    /**
     * updates the displayable when anything changes.
     */
    private void updateDisplayable() {
        try {
            if (locationDisplayable != null) {
                if (useStationModel) {
                    locationDisplayable.setStationModel(
                        super.getStationModel());
                    locationDisplayable.updateDisplayable();
                } else {
                    locationDisplayable.setDisplayState(symbolType,
                            showSymbol, idType, showId);
                }
            }
        } catch (Exception exc) {
            logException("Updating displayable", exc);
        }
    }

    /**
     * Set the symbol type for this instance.  Used by persistence.
     *
     * @param value  symbol type (e.g. StationLocationDisplayable.SYMBOL_PLUS)
     */
    public void setSymbolType(int value) {
        symbolType = value;
    }

    /**
     * Get the symbol type for this instance.
     *
     * @return symbol type (e.g. StationLocationDisplayable.SYMBOL_PLUS)
     */
    public int getSymbolType() {
        return symbolType;
    }

    /**
     * Set the id type for this instance.  Used by persistence.
     *
     * @param value  id type (e.g. StationLocationDisplayable.ID_ID)
     */
    public void setIdType(int value) {
        idType = value;
    }

    /**
     * Get the id type for this instance.
     *
     * @return id type (e.g. StationLocationDisplayable.ID_ID)
     */
    public int getIdType() {
        return idType;
    }


    /**
     * Set whether the symbol should be shown (visible).  Used by persistence.
     *
     * @param value  true to show the symbol.
     */
    public void setShowSymbol(boolean value) {
        showSymbol = value;
        if (getHaveInitialized()) {
            updateDisplayable();
        }
    }

    /**
     * Get whether the symbol should be shown (visible).
     *
     * @return true if symbol should be visible.
     */
    public boolean getShowSymbol() {
        return showSymbol;
    }

    /**
     * Set whether the id should be shown (visible).  Used by persistence.
     *
     * @param value  true to show the id.
     */
    public void setShowId(boolean value) {
        showId = value;
        if (getHaveInitialized()) {
            updateDisplayable();
        }
    }

    /**
     * Get whether the id should be shown (visible).
     *
     * @return true if id should be visible.
     */
    public boolean getShowId() {
        return showId;
    }

    /**
     * Set the Enabled property.
     *
     * @param value The new value for Enabled
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Get the Enabled property.
     *
     * @return The Enabled
     */
    public boolean getEnabled() {
        return enabled;
    }



    /**
     * Set the CenterOnClick property.
     *
     * @param value The new value for Center
     */
    public void setCenterOnClick(boolean value) {
        centerOnClick = value;
    }

    /**
     * Get the CenterOnClick property.
     *
     * @return The Center
     */
    public boolean getCenterOnClick() {
        return centerOnClick;
    }


    /**
     * Set the UseStationModel property.
     *
     * @param value The new value for UseStationModel
     */
    public void setUseStationModel(boolean value) {
        useStationModel = value;
    }

    /**
     * Get the UseStationModel property.
     *
     * @return The UseStationModel
     */
    public boolean getUseStationModel() {
        return useStationModel;
    }


    /**
     * Set the OnlyShowFiltered property.
     *
     * @param value The new value for OnlyShowFiltered
     */
    public void setOnlyShowFiltered(boolean value) {
        lastDeclutteredStationList = null;
        super.setOnlyShowFiltered(value);

    }



    /**
     * Override setDeclutter
     *
     * @param v new value
     */
    public void setDeclutter(boolean v) {
        super.setDeclutter(v);
        if (getHaveInitialized()) {
            Misc.run(this, "loadData");
        }
    }

    /**
     * Set the DetailsInLegend property.
     *
     * @param value The new value for DetailsInLegend
     */
    public void setDetailsInLegend(boolean value) {
        if (readoutInLegend == value) {
            return;
        }
        readoutInLegend = value;
        if (readoutLegendHolder != null) {
            if (value) {
                readoutLegendHolder.add(BorderLayout.CENTER, readoutComp);
            } else {
                readoutGuiHolder.add(BorderLayout.CENTER, readoutComp);
            }
        }
    }

    /**
     * Get the DetailsInLegend property.
     *
     * @return The DetailsInLegend
     */
    public boolean getDetailsInLegend() {
        return readoutInLegend;
    }



}

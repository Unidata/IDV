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
import ucar.unidata.view.geoloc.NavigatedDisplay;
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
 * @version $Revision: 1.3 $ $Date: 2006/12/01 20:16:39 $
 */


public class YahooLocationControl extends StationLocationControl {

    /** The query */
    private String query = "";

    /** Do we automatically redo the query when the viewpoint changes */
    private boolean autoLoad = true;

    /** Are we loading a layer */
    private boolean loading = false;

    /** List of created stations */
    private List stationList = new ArrayList();

    /** Holds the query */
    private JTextField queryField;


    /**
     * Default cstr;
     */
    public YahooLocationControl() {}


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
        Container contents = super.doMakeContents();
        queryField = new JTextField(query);
        JCheckBox autoLoadCbx = GuiUtils.makeCheckbox("Auto-load", this,
                                    "autoLoad");
        queryField.addActionListener(GuiUtils.makeActionListener(this,
                "doSearch", null));
        JButton btn = GuiUtils.makeButton("Search", this, "doSearch");

        JComponent top = GuiUtils.leftCenterRight(GuiUtils.rLabel("Query: "),
                             queryField, GuiUtils.hbox(btn, autoLoadCbx));
        top = GuiUtils.inset(top, 5);
        return GuiUtils.topCenter(top, (JComponent) contents);
    }

    /**
     * Handle the event
     */
    public void viewpointChanged() {
        if (autoLoad) {
            super.viewpointChanged();
        }
    }

    /**
     * Handle the event
     */
    public void projectionChanged() {
        if (autoLoad) {
            super.projectionChanged();
        }
    }


    /**
     * Set the query and reload
     */
    public void doSearch() {
        query = queryField.getText().trim();
        loadData();
    }

    /**
     * Loads the data into the <code>StationModelDisplayable</code>.
     * Declutters the stations if necessary.
     */
    public void loadData() {
        try {
            if (query.length() == 0) {
                return;
            }
            createStationList();
            super.loadData();
        } catch (Exception excp) {
            logException("loading data ", excp);
        }
    }

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
        return super.init(dataChoice);
    }


    /**
     * Create the list of stations
     */
    private void createStationList() {
        try {
            NavigatedDisplay navDisplay = getNavigatedDisplay();
            double[]         sc         = navDisplay.getScreenCenter();
            EarthLocation    el         = boxToEarth(sc);
            double lat = el.getLatitude().getValue(CommonUnit.degree);
            double lon = el.getLongitude().getValue(CommonUnit.degree);
            while (lon < -180) {
                lon += 180;
            }
            while (lon > 180) {
                lon -= 180;
            }
            String fullQuery =
                "http://api.local.yahoo.com/LocalSearchService/V3/localSearch?appid=idvunidata&radius=100&results=20&latitude="
                + lat + "&longitude=" + lon + "&query=" + query;
            //      System.err.println(fullQuery);

            NamedStationTable table =
                NamedStationTable.createStationTableFromFile(fullQuery);
            stationList = new ArrayList(table.values());
        } catch (Exception exc) {
            logException("Error loading WorldWind locations", exc);
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
     * Set the Query property.
     *
     * @param value The new value for Query
     */
    public void setQuery(String value) {
        query = value;
    }

    /**
     * Get the Query property.
     *
     * @return The Query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Set the AutoLoad property.
     *
     * @param value The new value for AutoLoad
     */
    public void setAutoLoad(boolean value) {
        autoLoad = value;
    }

    /**
     * Get the AutoLoad property.
     *
     * @return The AutoLoad
     */
    public boolean getAutoLoad() {
        return autoLoad;
    }



}

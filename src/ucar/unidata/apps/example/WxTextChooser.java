/*
 * $Id: WxTextChooser.java,v 1.4 2007/06/04 20:04:59 dmurray Exp $
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



package ucar.unidata.apps.example;


import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.ui.ChooserList;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.UtcDate;

import visad.DateTime;
import visad.VisADException;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;




/**
 * A chooser for ADDE text products
 *
 * @author IDV development team
 * @version $Revision: 1.4 $Date: 2007/06/04 20:04:59 $
 */
public class WxTextChooser extends IdvChooser {


    /** for gui */
    PreferenceList serverList;

    /** for gui */
    JComboBox serverCbx;

    /** for gui */
    JComboBox productCbx;

    /** for gui */
    JComboBox stationCbx;

    /** Property for station id */
    public static final String PROP_STATION_ID = "station.id";

    /** Property for station */
    public static final String PROP_STATION = "station";

    /** CSV station list */
    private static String stations = "ID,Name,Latitude,Longitude\n"
                                     + "MKX,Milwaukee,43.06,-87.96\n"
                                     + "GRB,Green Bay,44.52,-87.98\n"
                                     + "MSN,Madison,43.08,-89.83\n"
                                     + "BOU,Boulder,40.03,-105.24";

    /**
     * Make a new one
     *
     * @param mgr The manager
     * @param root The xml element that defined this object
     *
     */
    public WxTextChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Handle the update event. Just pass it through to the IDV Chooser
     */
    public void doUpdate() {}


    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {

        serverList = getPreferenceList(PREF_ADDESERVERS);
        List comps = new ArrayList();
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        comps.add(GuiUtils.rLabel("Server:"));

        comps.add(GuiUtils.left(serverCbx = serverList.createComboBox("",
                null)));

        productCbx = GuiUtils.getEditableBox(getDefaultProducts(), null);
        comps.add(GuiUtils.rLabel("AFOS Product:"));
        comps.add(GuiUtils.left(productCbx));

        stationCbx = GuiUtils.getEditableBox(getDefaultStations(), null);
        comps.add(GuiUtils.rLabel("AFOS Station:"));
        comps.add(GuiUtils.left(stationCbx));

        JComponent buttons = getDefaultButtons();
        setHaveData(true);
        return GuiUtils.vbox(GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                GuiUtils.WT_N), buttons);
    }


    /**
     * User said go, we go. Simply get the list of images
     * from the imageChooser and create the FILE.ADDETEXT
     * DataSource
     *
     */
    public void doLoadInThread() {
        List   urls       = new ArrayList();
        String server     = serverCbx.getSelectedItem().toString().trim();
        Object rawProduct = productCbx.getSelectedItem();
        String product    = TwoFacedObject.getIdString(rawProduct);
        Object rawStation = stationCbx.getSelectedItem();
        String station = TwoFacedObject.getIdString(rawStation);
        String url = "adde://" + server + "/wxtext?group=RTWXTEXT&apro="
                     + product + "&astn=" + station + "&day="
                     + getJulianDay();
        ;
        urls.add(url);
        Hashtable ht = new Hashtable();
        ht.put(PROP_STATION_ID, station);
        if (rawStation instanceof TwoFacedObject) {
            ht.put(PROP_STATION, ((TwoFacedObject)rawStation).getId());
        }
        makeDataSource(urls, "FILE.ADDETEXT", ht);
    }


    /**
     * Make a list of default products
     *
     * @return the list of default products
     */
    private Vector getDefaultProducts() {
        Vector v = new Vector();
        v.add(new TwoFacedObject("Zone Forecast", "ZFP"));
        v.add(new TwoFacedObject("Area Forecast Discussion", "AFD"));
        v.add(new TwoFacedObject("Area Weather Summary", "AWS"));
        v.add(new TwoFacedObject("Severe Weather Statement", "SWS"));
        return v;
    }

    /**
     * Make a list of default stations
     *
     * @return the list of default stations
     */
    private Vector getDefaultStations() {
        Vector v = new Vector();
        /*
        v.add(new TwoFacedObject("Milwaukee", "MKX"));
        v.add(new TwoFacedObject("Green Bay", "GRB"));
        v.add(new TwoFacedObject("Madison", "MSN"));
        v.add(new TwoFacedObject("Boulder", "BOU"));
        */
        NamedStationTable table = new NamedStationTable();
        try {
            table.createStationTableFromCsv(stations);
            List l = (List) table.values();
            for (int i = 0; i < l.size(); i++) {
                NamedStationImpl station = (NamedStationImpl) l.get(i);
                String name = station.getName();
                Hashtable props = station.getProperties();
                if (props != null) {
                   String testName = (String) props.get("Name");
                   if (testName != null) name = testName;
                }
                v.add(new TwoFacedObject(name, station));
            }
        } catch (Exception e) {}
        return v;
    }

    /**
     * Get the current  Julian day as a String
     *
     * @return the current day as a string (yyyyDDD)
     */
    private String getJulianDay() {
        String retString = "";
        try {
            DateTime dt = new DateTime();
            retString = UtcDate.formatUtcDate(dt, "yyyyDDD");
        } catch (VisADException ve) {}
        return retString;

    }

}


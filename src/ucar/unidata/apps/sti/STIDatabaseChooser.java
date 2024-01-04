/*
 *
 * Copyright  1997-2024 Unidata Program Center/University Corporation for
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

package ucar.unidata.apps.sti;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;


import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.PreferenceList;




import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;


import javax.swing.*;




/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Nov 20, 2008
 * Time: 9:59:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class STIDatabaseChooser extends IdvChooser {
    public static final String PREF_STORMSERVERSURL = "idv.data.storm.servers.url";
    public static final String PREF_STORMSERVERSPORT = "idv.data.storm.servers.port";
    public static final String PREF_STORMSERVERSDB = "idv.data.storm.servers.db";

    /** for gui */
    JComboBox serverCbx;

        /** Property name to get the list or urls */
    public static final String PREF_URLLIST = "idv.urllist";

    /** Manages the pull down list of urls */
    private PreferenceList prefurlList;
    private PreferenceList prefportList;
    private PreferenceList prefdbList;

    /** The list of urls */
    private JComboBox urlbox;

     /** The list of urls */
    private JComboBox portbox;

     /** The list of urls */
    private JComboBox dbbox;

    /** Holds the combo box */
    private JPanel urlPanel;

    /** Are we showing the combo box */
    private boolean showBox = true;


    /** panel */
    private JComponent layoutPanel;

    /**
     * Make a new one
     *
     * @param mgr The manager
     * @param root The xml element that defined this object
     *
     */
    public STIDatabaseChooser(IdvChooserManager mgr, Element root) {
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

        prefurlList = getPreferenceList(PREF_STORMSERVERSURL);

        prefportList = getPreferenceList(PREF_STORMSERVERSPORT);

        prefdbList = getPreferenceList(PREF_STORMSERVERSDB);

        JComponent urlButtons = getDefaultButtons();


        urlbox = prefurlList.createComboBox(CMD_LOAD, this);
        portbox = prefportList.createComboBox(CMD_LOAD, this);
        dbbox = prefdbList.createComboBox(CMD_LOAD, this);

        Object [] obList = new Object[]{"Server:", GuiUtils.left(urlbox),
                                        "Port:" , GuiUtils.left(portbox),
                                        "Database Name:", GuiUtils.left(dbbox)};



       layoutPanel = GuiUtils.formLayout(obList);



        JComponent mainContents;

        mainContents = GuiUtils.vbox( layoutPanel,
                                      urlButtons);

        mainContents = GuiUtils.inset(mainContents,5);
        setHaveData(true);
        return GuiUtils.top(mainContents);
    }


    /**
     * User said go, we go. Simply get the list of images
     * from the imageChooser and create the FILE.ADDETEXT
     * DataSource
     *
     */
    public void doLoadInThread() {
        List   urls       = new ArrayList();
        String serverUrl     = urlbox.getSelectedItem().toString().trim();
        String serverPort     = portbox.getSelectedItem().toString().trim();
        String serverDb     = dbbox.getSelectedItem().toString().trim();

        String url = "jdbc:mysql://" + serverUrl + ":" + serverPort + "/" +serverDb
                     + "?zeroDateTimeBehavior=convertToNull";

        urls.add(url);
        Hashtable ht = new Hashtable();


        prefurlList.saveState(urlbox);
        prefportList.saveState(portbox);
        prefdbList.saveState(dbbox);
        makeDataSource(urls, "DB.STORMTRACK", ht);
    }




}

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

package ucar.unidata.idv.chooser.adde;


import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddeURLException;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.gis.mcidasmap.McidasMap;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.metdata.Station;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.station.StationLocationMap;

import ucar.unidata.xml.XmlResourceCollection;

import ucar.unidata.xml.XmlResourceCollection;

import visad.VisADException;

import visad.georef.NamedLocation;

import java.awt.*;

import java.awt.event.*;

import java.beans.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;




/**
 * Widget to select NEXRAD radar images from a remote ADDE server
 * Displays a list of the descriptors (names) of the radar datasets
 * available for a particular ADDE group on the remote server.
 *
 * @author Don Murray
 */
public class AddeRadarChooser extends AddeImageChooser {


    /** Use to list the stations */
    protected static final String VALUE_LIST = "list";



    /** This is the list of properties that are used in the advanced gui */
    private static final String[] RADAR_PROPS = { PROP_UNIT };

    /** This is the list of labels used for the advanced gui */
    private static final String[] RADAR_LABELS = { "Data Type:" };


    /** Am I currently reading the stations */
    private boolean readingStations = false;

    /** read station object */
    private Object readStationTask;

    /** station table */
    private List nexradStations;



    /**
     * Construct an Adde image selection widget displaying information
     * for the specified dataset located on the specified server.
     *
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeRadarChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        this.nexradStations =
            getIdv().getResourceManager().findLocationsByType("radar");
    }


    /**
     * get the adde server grup type to use
     *
     * @return group type
     */
    protected String getGroupType() {
        return AddeServer.TYPE_RADAR;
    }



    /**
     * Should we show the advanced properties component in a separate panel
     *
     * @return false
     */
    public boolean showAdvancedInTab() {
        return false;
    }


    /**
     * Overwrite base class method to return the correct name
     * (used for labeling, etc.)
     *
     * @return  data name specific to this selector
     */
    public String getDataName() {
        return "Radar Data";
    }

    /**
     * Get the descriptor label
     *
     * @return  the label
     */
    public String getDescriptorLabel() {
        return "Product";
    }


    /**
     * Get the size of the image list
     *
     * @return the image list size
     */
    protected int getImageListSize() {
        return 6;
    }


    /**
     * Make the components (label/widget) and add them to the list.
     *
     *
     * @param comps The list to add to.
     */
    protected void getComponents(List comps) {
        List extraComps = new ArrayList();
        super.getComponents(extraComps);
        extraComps.addAll(processPropertyComponents());
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel extra = GuiUtils.doLayout(extraComps, 2, GuiUtils.WT_NY,
                                         GuiUtils.WT_N);


        JComponent stationMap = getStationMap();
        stationMap.setPreferredSize(new Dimension(230, 200));
        stationMap = registerStatusComp("stations", stationMap);
        addServerComp(stationMap);

        JComponent timesPanel = addServerComp(makeTimesPanel(false, true));

        JComponent panel = GuiUtils.centerRight(stationMap,
                               GuiUtils.topCenter(GuiUtils.filler(250, 1),
                                   GuiUtils.top(extra)));
        comps.add(addServerComp(GuiUtils.valignLabel(LABEL_STATIONS)));
        comps.add(panel);
        comps.add(GuiUtils.valignLabel("Times:"));
        comps.add(timesPanel);

    }


    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        List comps = new ArrayList();
        comps.addAll(processServerComponents());
        getComponents(comps);
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel imagePanel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                GuiUtils.WT_NYN);
        return GuiUtils.centerBottom(imagePanel, getDefaultButtons(this));
    }



    /**
     * Add the times component
     *
     * @param comps list of components
     */
    protected void addTimesComponent(List comps) {}



    /**
     * Get a description of the currently selected dataset
     *
     * @return the data set description.
     */
    public String getDatasetName() {
        return getSelectedStation() + " (" + super.getDatasetName() + ")";
    }

    /**
     * Method to call if the server changed.
     */
    protected void connectToServer() {
        clearStations();
        super.connectToServer();
        setAvailableStations();
    }


    /**
     * Check if we are ready to read times
     *
     * @return  true if times can be read
     */
    protected boolean canReadTimes() {
        return super.canReadTimes() && (getSelectedStation() != null);
    }


    /**
     * Get the advanced property names
     *
     * @return array of advanced properties
     */
    protected String[] getAdvancedProps() {
        return RADAR_PROPS;
    }

    /**
     * Get the labels for the advanced properties
     *
     * @return array of labels
     */
    protected String[] getAdvancedLabels() {
        return RADAR_LABELS;
    }


    /**
     * Update labels, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        if (getState() != STATE_CONNECTED) {
            clearStations();
        }
        if (readStationTask != null) {
            if (taskOk(readStationTask)) {
                setStatus("Reading available stations from server");
            } else {
                readStationTask = null;
                setState(STATE_UNCONNECTED);
            }
        }
    }


    /**
     * A new station was selected. Update the gui.
     *
     * @param stations List of selected stations
     */
    protected void newSelectedStations(List stations) {
        super.newSelectedStations(stations);
        descriptorChanged();
    }

    /**
     * Respond to a change in the descriptor list.
     */
    protected void descriptorChanged() {
        if ( !getHaveStations()) {  // handle archive case
            setAvailableStations();
        }
        super.descriptorChanged();
    }


    /**
     *  Generate a list of radar ids for the id list.
     */
    private void setAvailableStations() {
        readStationTask = startTask();
        clearSelectedStations();
        updateStatus();
        Misc.run(new Runnable() {
            public void run() {
                showWaitCursor();
                List stations = readStations();
                if (stopTaskAndIsOk(readStationTask)) {
                    readStationTask = null;
                    if (stations != null) {
                        getStationMap().setStations(stations);
                    } else {
                        clearStations();
                    }
                    updateStatus();
                    revalidate();
                } else {
                    //User pressed cancel
                    setState(STATE_UNCONNECTED);
                }
                showNormalCursor();
            }
        });

    }

    /**
     * Generate a list of radar ids for the id list.
     *
     * @return  list of station IDs
     */
    private List readStations() {
        ArrayList stations = new ArrayList();
        try {
            if ((descriptorNames == null) || (descriptorNames.length == 0)) {
                return stations;
            }
            StringBuffer buff        = getGroupUrl(REQ_IMAGEDIR, getGroup());
            String       descrForIds = descriptorNames[0];
            Hashtable    dtable      = getDescriptorTable();
            Iterator     iter        = dtable.keySet().iterator();
            String group = getGroup().toLowerCase();
            while (iter.hasNext()) {
                String name       = (String) iter.next();
                String descriptor = ((String) dtable.get(name)).toLowerCase();
                if (group.indexOf("tdw") >= 0 && descriptor.equals("tr0")) {
                    descrForIds = name;
                    break;
                } else if (descriptor.equals("n0r")
                        || descriptor.startsWith("bref")) {
                    descrForIds = name;
                    break;
                }
            }
            appendKeyValue(buff, PROP_DESCR,
                           getDescriptorFromSelection(descrForIds));
            appendKeyValue(buff, PROP_ID, VALUE_LIST);
            if (archiveDay != null) {
                appendKeyValue(buff, PROP_DAY, archiveDay);
            }
            Hashtable         seen    = new Hashtable();
            AreaDirectoryList dirList =
                new AreaDirectoryList(buff.toString());
            for (Iterator it = dirList.getDirs().iterator(); it.hasNext(); ) {
                AreaDirectory ad = (AreaDirectory) it.next();
                String stationId =
                    McIDASUtil.intBitsToString(ad.getValue(20)).trim();
                //Check for uniqueness
                if (seen.get(stationId) != null) {
                    continue;
                }
                seen.put(stationId, stationId);
                //System.err.println ("id:" + stationId);
                Object station = findStation(stationId);
                if (station != null) {
                    stations.add(station);
                }
            }
        } catch (AreaFileException e) {
            String msg = e.getMessage();
            if (msg.toLowerCase().indexOf(
                    "no images meet the selection criteria") >= 0) {
                LogUtil.userErrorMessage(
                    "No stations could be found on the server");
                stations = new ArrayList();
                setState(STATE_UNCONNECTED);
            } else {
                handleConnectionError(e);
            }
        }
        return stations;
    }


    /**
     * Find the station for the given ID
     *
     * @param stationId  the station ID
     *
     * @return  the station or null if not found
     */
    private Object findStation(String stationId) {
        for (int i = 0; i < nexradStations.size(); i++) {
            NamedStationTable table =
                (NamedStationTable) nexradStations.get(i);
            Object station = table.get(stationId);
            if (station != null) {
                return station;
            }
        }
        return null;
    }

    /**
     * Do the cancel
     */
    public void doCancel() {
        readStationTask = null;
        super.doCancel();
    }


    /**
     * Create the appropriate request string for the image.
     *
     * @param ad  <code>AreaDirectory</code> for the image in question.
     * @param doTimes  true if this is for absolute times, false for relative
     * @param cnt  image count (position in dataset)
     *
     * @return  the ADDE request URL
     */
    protected String makeRequestString(AreaDirectory ad, boolean doTimes,
                                       int cnt) {

        StringBuffer buf = getGroupUrl(REQ_IMAGEDATA, getGroup());
        buf.append(makeDateTimeString(ad, cnt, doTimes));
        String[] props = {
            PROP_DESCR, PROP_ID, PROP_UNIT, PROP_SPAC, PROP_MAG, PROP_SIZE
        };
        buf.append(makeProps(props, ad));
        return buf.toString();
    }

    /**
     * Get the list of properties for the base URL
     * @return list of properties
     */
    protected String[] getBaseUrlProps() {
        return new String[] { PROP_DESCR, PROP_ID, PROP_UNIT, PROP_SPAC,
                              PROP_BAND };
    }

    /**
     * Overwrite the base class method to return the default property value
     * for PROP_ID.
     *
     * @param prop The property
     * @param ad The area directory
     * @param forDisplay Is this to show the end user in the gui.
     *
     * @return The value of the property
     */
    protected String getDefaultPropValue(String prop, AreaDirectory ad,
                                         boolean forDisplay) {
        if (prop.equals(PROP_ID)) {
            return getSelectedStation();
        }
        return super.getDefaultPropValue(prop, ad, forDisplay);
    }

    /**
     * Get a description of the properties
     *
     * @return  a description
     */
    protected String getPropertiesDescription() {
        StringBuffer buf = new StringBuffer();
        if (unitComboBox != null) {
            buf.append(getAdvancedLabels()[0]);
            buf.append(" ");
            buf.append(unitComboBox.getSelectedItem().toString());
        }
        return buf.toString();
    }


    /**
     * get properties
     *
     * @param ht properties
     */
    protected void getDataSourceProperties(Hashtable ht) {
        super.getDataSourceProperties(ht);
        ht.put(ImageDataSource.PROP_IMAGETYPE, ImageDataSource.TYPE_RADAR);
    }



}

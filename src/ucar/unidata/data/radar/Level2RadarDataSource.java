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

package ucar.unidata.data.radar;


import ucar.unidata.data.*;
import ucar.unidata.metdata.NamedStation;

import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectArray;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;

import visad.*;
import visad.Real;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.NamedLocation;


import java.awt.event.ActionEvent;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;



import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;


/**
 * A data source for Archive Level II Radar data files.
 *
 * @author Don Murray
 * @version $Revision: 1.63 $ $Date: 2006/12/01 20:42:38 $
 */
public class Level2RadarDataSource extends RadarDataSource {

    /** Logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            Level2RadarDataSource.class.getName());

    /** Identifier for Station location */
    private static final String DATA_DESCRIPTION = "Level II Radar Data";


    /** Local copy of the station name */
    private String stationName = "";

    /** list of stations */
    private static NamedStationTable nexradStations = null;

    /** Widget for properties */
    private JComboBox stationBox;


    /**
     * Construct a new Level II data source.
     */
    public Level2RadarDataSource() {}

    /**
     * Construct a new Level II data source.
     * @param  descriptor  descriptor for this datasource
     * @param  source  file to read
     * @param  properties  hashtable of properties.  Includes radar location
     *
     * @throws VisADException  couldn't create the data
     */
    public Level2RadarDataSource(DataSourceDescriptor descriptor,
                                 String source, Hashtable properties)
            throws VisADException {
        this(descriptor, new String[] { source }, properties);
    }


    /**
     * Construct a new Level II data source.
     * @param  descriptor  descriptor for this datasource
     * @param  sources  files to read
     * @param  properties  hashtable of properties.  Includes radar location
     *
     * @throws VisADException  couldn't create the data
     */
    public Level2RadarDataSource(DataSourceDescriptor descriptor,
                                 String[] sources, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.toList(sources), properties);
    }




    /**
     * Construct a new Level II data source.
     * @param  descriptor  descriptor for this datasource
     * @param  sources  files to read
     * @param  properties  hashtable of properties.  Includes radar location
     *
     * @throws VisADException  couldn't create the data
     */
    public Level2RadarDataSource(DataSourceDescriptor descriptor,
                                 List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, DATA_DESCRIPTION, properties);
        NamedStation ns = (getProperties() != null)
                          ? (NamedStation) getProperties().get(
                              STATION_LOCATION)
                          : null;
        String name = DATA_DESCRIPTION;
        if (sources.size() > 0) {
            name = sources.get(0).toString();
        }
        name = (ns == null)
               ? name
               : DATA_DESCRIPTION + " (" + ns.getIdentifier() + ")";

        setName(name);
    }


    /**
     * Make the set of DataChoices associated with this DataSource.
     */
    public void doMakeDataChoices() {

        List<RadarAdapter> l2as = getRadarAdapters();
        if ((l2as == null) || l2as.isEmpty()) {
            return;
        }

        NamedStation namedStation = (getProperties() != null)
                                    ? (NamedStation) getProperties().get(
                                        STATION_LOCATION)
                                    : null;
        if (namedStation != null) {
            stationName = namedStation.getIdentifier() + " ";
        }

        double[] angles = ((Level2Adapter) l2as.get(0)).getAngles();
        String[] moments = { "Reflectivity", "Radial Velocity",
                             "Spectrum Width" };
        boolean haveTimes     = (l2as.size() > 1);

        List volumeCategories = Misc.toList(new DataCategory[] { CATEGORY_RHI,
        //CATEGORY_VCS,
        CATEGORY_CAPPI,
        //CATEGORY_TH ,CATEGORY_VWP,
        CATEGORY_VOLUME, CATEGORY_ISOSURFACE });
        List sweepCategories;
        if (haveTimes) {
            sweepCategories = Misc.newList(CATEGORY_SWEEP_2D_TIME,
                                           CATEGORY_SWEEP_3D_TIME);
        } else {
            sweepCategories = Misc.newList(CATEGORY_SWEEP_2D,
                                           CATEGORY_SWEEP_3D);
        }



        volumeCategories.addAll(sweepCategories);
        Hashtable properties;
        try {
            for (int moment = 0; moment < moments.length; moment++) {
                Integer momentObj  = new Integer(moment);
                String  momentName = moments[moment];
                Hashtable compositeProperties =
                    Misc.newHashtable(PROP_ANGLES, angles, STATION_LOCATION,
                                      namedStation, PROP_VOLUMEORSWEEP,
                                      VALUE_VOLUME);

                compositeProperties.put(DataChoice.PROP_ICON,
                                        "/auxdata/ui/icons/Radar.gif");
                CompositeDataChoice momentChoice =
                    new CompositeDataChoice(
                        this, new ObjectArray(
                            momentName, momentObj, RadarConstants.VALUE_3D), stationName
                                + " "
                                + momentName, momentName, volumeCategories, compositeProperties);
                // make a DirectDataChoice for 2D plots
                //   for every tilt ("angle") above horizontal;
                for (int i = 0; i < angles.length; i++) {
                    String name = "Elevation Angle " + angles[i];
                    Hashtable dataChoiceProperties =
                        Misc.newHashtable(new Object[] {
                        PROP_ANGLES, new double[] { angles[i] }, PROP_ANGLE,
                        new Double(angles[i]), PROP_VOLUMEORSWEEP, VALUE_SWEEP
                    });
                    dataChoiceProperties.put(
                        DataChoice.PROP_ICON,
                        "/auxdata/ui/icons/RadarAngle.gif");
                    if (namedStation != null) {
                        dataChoiceProperties.put(STATION_LOCATION,
                                namedStation);
                    }
                    momentChoice.addDataChoice(
                        new DirectDataChoice(
                            this, new ObjectArray(
                                new Double(
                                    angles[i]), momentObj, RadarConstants.VALUE_2D), stationName
                                        + " " + momentName, momentName + " "
                                            + name, sweepCategories, dataChoiceProperties));
                }
                addDataChoice(momentChoice);
            }
        } catch (Exception excp) {
            logException("Creating data choices", excp);
        }
    }

    /**
     * Check to see if this NetcdfRadarDataSource is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof Level2RadarDataSource)) {
            return false;
        }
        Level2RadarDataSource that = (Level2RadarDataSource) o;
        return (this == that);
    }

    /**
     * Get the hash code for this object.
     * @return hash code.
     */
    public int hashCode() {
        int hashCode = getName().hashCode();
        return hashCode;
    }





    /**
     * Add the gui components into the list for the properties dialog
     *
     * @param comps List of components
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        Vector items = new Vector();
        items.addAll(getStations().values());
        stationBox = new JComboBox(items);
        NamedStation namedStation = (getProperties() != null)
                                    ? (NamedStation) getProperties().get(
                                        STATION_LOCATION)
                                    : null;


        if (namedStation != null) {
            stationBox.setSelectedItem(namedStation);
        }
        comps.add(GuiUtils.rLabel("Station Location: "));
        comps.add(stationBox);
    }


    /**
     * Apply properties components
     *
     * @return false if something failed and we need to keep showing the dialog
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        Object o = stationBox.getSelectedItem();
        if (o instanceof NamedStation) {
            setStationInfo((NamedStation) o);
        }
        return true;
    }


    /**
     * Set the station information for this DataSource
     *
     * @param station new station information
     */
    private void setStationInfo(NamedStation station) {
        if (station == null) {
            return;
        }
        Hashtable props = getProperties();
        if (props != null) {
            props.put(STATION_LOCATION, station);
        } else {
            setProperties(Misc.newHashtable(STATION_LOCATION, station));
        }
        setName(getName());
        setDescription(getName());

        // update the data adapters
        List<RadarAdapter> l2as = getRadarAdapters();
        if (l2as != null) {
            for (int i = 0; i < l2as.size(); i++) {
                try {
                    ((Level2Adapter) l2as.get(i)).setRadarLocation(
                        (EarthLocation) station.getNamedLocation());
                } catch (Exception excp) {
                    logException("Couldn't set station on adapter", excp);
                }
            }
        }

        // update the data choices
        List l = getDataChoices();
        for (int i = 0; i < l.size(); i++) {
            DataChoice dc = (DataChoice) l.get(i);
            Hashtable  ht = dc.getProperties();
            if (ht != null) {
                ht.put(STATION_LOCATION, station);
            } else {
                dc.setProperties(Misc.newHashtable(STATION_LOCATION,
                        station));
            }
        }

        reloadData();
    }

    /**
     * Make a RadarAdapter from the source
     *
     * @param source  source (file or URL) of data
     *
     * @return corresponding RadarAdapter
     * @throws Exception  problem creating the adapter.
     */
    protected RadarAdapter makeRadarAdapter(String source) throws Exception {
        NamedStation ns = (NamedStation) getProperty(STATION_LOCATION);
        return new Level2Adapter(this, source, (ns != null)
                ? (EarthLocation) ns.getNamedLocation()
                : null);
    }


    /**
     * Main routine for testing.
     *
     * @param args  list of file names
     *
     * @throws Exception  problem occurred
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Must supply a file name");
            System.exit(1);
        }
        Level2RadarDataSource l2rds = new Level2RadarDataSource(null, args,
                                          null);
        for (Iterator iter = l2rds.getDataChoices().iterator();
                iter.hasNext(); ) {
            System.out.println(iter.next());
        }
        /*
        Data testData = l2rds.getData ((DataChoice)
            l2rds.getDataChoices().get(0), null);
        visad.python.JPythonMethods.dumpTypes (testData);
        */
        System.out.println("DateTimes:");
        for (Iterator iter = l2rds.getAllDateTimes().iterator();
                iter.hasNext(); ) {
            System.out.println(iter.next());
        }
    }


    /**
     * Read in the nexrad stations from the
     * idv/resources/nexradstns.xml resource
     *
     * @return List of of {@link ucar.unidata.metdata.NamedStation}-s
     */
    public NamedStationTable getStations() {

        if (nexradStations == null) {
            nexradStations =
                getDataContext().getResourceManager().findLocations(
                    "NEXRAD Sites");
        }
        return nexradStations;
    }

    /**
     * Get the list of all levels available from this DataSource
     *
     *
     * @param dataChoice The data choice we are getting levels for
     * @param dataSelection  the date selection
     * @return  List of all available levels
     */
    public List getAllLevels(DataChoice dataChoice,
                             DataSelection dataSelection) {
        dataSelection = DataSelection.merge(dataSelection,
                                            getDataSelection());

        List      levels = new ArrayList();
        Hashtable props  = dataChoice.getProperties();
        if ((props == null) || (props.get(PROP_ANGLES) == null)) {
            return levels;
        }
        double[] angles = (double[]) props.get(PROP_ANGLES);
        for (int i = 0; i < angles.length; i++) {
            levels.add(new Real(RadarAdapter.ELEVATION_ANGLE_TYPE,
                                angles[i]));
        }
        return levels;
    }

}

/*
 * $Id: CDMRadarDataSource.java,v 1.25 2007/07/03 19:25:24 jeffmc Exp $
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





package ucar.unidata.data.radar;


import ucar.nc2.thredds.TDSRadarDatasetCollection;
import ucar.nc2.units.DateUnit;

import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataChoice;


import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.DatedObject;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectArray;

import visad.CommonUnit;
import visad.DateTime;
import visad.Real;
import visad.RealType;
import visad.VisADException;

import visad.georef.EarthLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;


/**
 * A data source for radar data through the netCDF Common Data Model (CDM)
 * Radial Datatype API.
 * @author IDV Development Team
 * @version $Revision: 1.25 $
 */
public class CDMRadarDataSource extends RadarDataSource {

    /** This holds the RadarQuery in the properties */
    public static final String PROP_RADARQUERY = "prop.radarquery";


    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(DoradeDataSource.class.getName());

    /** Identifier for Station location */
    private static final String DATA_DESCRIPTION =
        "Common Data Model Radial type Data";

    /** list of stations */
    private static NamedStationTable nexradStations = null;

    /** Widget for properties */
    private JComboBox stationBox;

    /** station */
    private NamedStation namedStation = null;

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public CDMRadarDataSource() {}

    /**
     * Construct a new CDM data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  sources   List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public CDMRadarDataSource(DataSourceDescriptor descriptor, List sources,
                              Hashtable properties)
            throws VisADException {
        super(descriptor, sources, DATA_DESCRIPTION, properties);
    }

    /**
     * Construct a new CDM data source.
     * @param  descriptor  descriptor for this datasource
     * @param  sources  files to read
     * @param  properties  hashtable of properties.  Includes radar location
     *
     * @throws VisADException  couldn't create the data
     */
    public CDMRadarDataSource(DataSourceDescriptor descriptor,
                              String[] sources, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.toList(sources), properties);
    }

    /**
     * Construct a new CDM data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the DORADE file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public CDMRadarDataSource(DataSourceDescriptor descriptor,
                              String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
    }

    /**
     * Construct a new CDM data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param radarQuery Holds the query info
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public CDMRadarDataSource(DataSourceDescriptor descriptor,
                              RadarQuery radarQuery, Hashtable properties)
            throws VisADException {
        super(descriptor, new ArrayList(), DATA_DESCRIPTION, properties);
        setProperty(PROP_RADARQUERY, radarQuery);
        makeSources();
    }


    /**
     * handle legacy bundles
     */
    public void initAfterUnpersistence() {
        makeSources();
        super.initAfterUnpersistence();
    }


    /**
     * Clear out and reinitialize the date
     */
    public void reloadData() {
        //Call makeSources to see if we have any new relative times data
        makeSources();
        super.reloadData();
    }


    /**
     * If we have a RadarQuery then make the sources from that
     */
    private void makeSources() {
        try {
            RadarQuery query = (RadarQuery) getProperty(PROP_RADARQUERY);
            if (query == null) {
                return;
            }
            List times = query.getDateSelection().getTimes();
            List         urls   = new ArrayList();
            StringBuffer errlog = new StringBuffer();

            TDSRadarDatasetCollection collection =
                TDSRadarDatasetCollection.factory("test",
                    query.getCollectionUrl(), errlog);
            if ((times == null) || (times.size() == 0)) {
                List allTimes = new ArrayList();
                List timeSpan = collection.getRadarTimeSpan();
                Date fromDate =
                    DateUnit.getStandardOrISO((String) timeSpan.get(0));
                Date toDate =
                    DateUnit.getStandardOrISO((String) timeSpan.get(1));
                //System.out.println("dataset dates: from = " + fromDate + " to = " + toDate);
                /*
                List collectionTimes =
                    collection.getRadarStationTimes(query.getStation(),
                        query.getProduct(),
                        fromDate,
                        toDate);
                if(collectionTimes.size() == 0)  {
                */
                      List collectionTimes =
                        collection.getRadarStationTimes(query.getStation(),
                        query.getProduct(),
                        query.getDateSelection().getStartFixedDate(),
                        query.getDateSelection().getEndFixedDate());
                //}

                for (int timeIdx = 0; timeIdx < collectionTimes.size();
                        timeIdx++) {
                    Object timeObj = collectionTimes.get(timeIdx);
                    Date   date;
                    if (timeObj instanceof Date) {
                        date = (Date) timeObj;
                    } else {
                        date = DateUnit.getStandardOrISO(timeObj.toString());
                    }
                    allTimes.add(new DatedObject(date));
                }
                times = DatedObject.unwrap(
                    query.getDateSelection().apply(allTimes));
            }

            for (int i = 0; i < times.size(); i++) {
                Date date = (Date) times.get(i);
                java.net.URI uri =
                    collection.getRadarDatasetURI(query.getStation(),
                        query.getProduct(), date);
                urls.add(uri.toString());
            }

            setSources(urls);
        } catch (Exception excp) {
            logException("Creating urls to radar data", excp);
        }
    }


    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {

        //
        // just one category
        //
        List categories;
        List categories2D;
        List adapters = getRadarAdapters();
        if ((adapters == null) || adapters.isEmpty()) {
            return;
        }
        CDMRadarAdapter da          = (CDMRadarAdapter) adapters.get(0);
        boolean         haveTimes   = (adapters.size() > 1);
        RealType[]      paramTypes  = da.getParams();
        String          stationID   = da.getStationID();
        String          stationName = da.getStationName();
        String          dataFormat  = da.getDataFormatName();
        //NamedStation    namedStation = null;
        if (getProperties() != null) {
            Object o = getProperties().get(STATION_LOCATION);
            if (o instanceof NamedStation) {
                namedStation = (NamedStation) o;
            }
        }
        EarthLocation rdLocation = da.getStationLocation();
        setName(makeName(da));
        String ids = stationID.length() > 3 ? stationID.substring(1) : stationID.toString();
        if (namedStation == null || (namedStation.getID().equalsIgnoreCase(ids))) {
            try {
                namedStation = new NamedStationImpl(ids,
                        stationName, rdLocation.getLatitude().getValue(),
                        rdLocation.getLongitude().getValue(),
                        rdLocation.getAltitude().getValue(),
                        CommonUnit.meter);
            } catch (Exception excp) {
                logException("Creating data choices", excp);
            }
        }

        //
        // Add a DataChoice for each parameter we have available
        //
        try {
            for (int i = 0; i < paramTypes.length; i++) {
                String  paramName = paramTypes[i].getName();
                Integer momentObj = new Integer(i);

                // String  momentName = moments[i];
                if (da.isVolume()) {
                    categories = Misc.newList(CATEGORY_RHI, CATEGORY_CAPPI,
                            CATEGORY_ISOSURFACE, CATEGORY_VOLUME);
                    //categories.add(CATEGORY_SWEEP_3D);
                    if (haveTimes) {
                        categories.add(CATEGORY_SWEEP_3D_TIME);
                    } else {
                        categories.add(CATEGORY_SWEEP_3D);
                    }

                } else if (da.isRaster()) {
                    categories2D = (haveTimes)
                                   ? Misc.newList(CATEGORY_RASTER_2D_TIME)
                                   : Misc.newList(CATEGORY_RASTER_2D);
                    // categories = Misc.newList(CATEGORY_RASTER_2D);
                    if (haveTimes) {
                        categories = Misc.newList(CATEGORY_RASTER_2D);
                    } else {
                        categories = Misc.newList(CATEGORY_RASTER_2D_TIME);
                    }
                } else {
                    if (haveTimes) {
                        categories = Misc.newList(CATEGORY_SWEEP_2D_TIME,
                                CATEGORY_SWEEP_3D_TIME);
                    } else {
                        categories = Misc.newList(CATEGORY_SWEEP_2D,
                                CATEGORY_SWEEP_3D);
                    }
                    //categories = Misc.newList(CATEGORY_SWEEP_2D,
                    //        CATEGORY_SWEEP_3D);
                }

                // addDataChoice(new DirectDataChoice(this, paramTypes[i], stationName, paramName,
                // sweepCategories, DataChoice.NULL_PROPERTIES));
                double[] angles = da.getAngles(paramName);


                Hashtable compositeProperties =
                    Misc.newHashtable(PROP_ANGLES, angles, STATION_LOCATION,
                                      namedStation, PROP_VOLUMEORSWEEP,
                                      VALUE_VOLUME);

                compositeProperties.put(DataChoice.PROP_ICON,
                                        "/auxdata/ui/icons/Radar.gif");

                CompositeDataChoice momentChoice =
                    new CompositeDataChoice(
                        this, new ObjectArray(
                            momentObj, paramName, paramName, RadarConstants
                                .VALUE_3D), stationID + " "
                                    + paramName, paramName, categories, compositeProperties);

                // make a DirectDataChoice for 2D plots
                // for every tilt ("angle") above horizontal;
                if (da.isRaster()) {
                    categories2D = (haveTimes)
                                   ? Misc.newList(CATEGORY_RASTER_2D_TIME)
                                   : Misc.newList(CATEGORY_RASTER_2D);
                } else {
                    if (haveTimes) {
                        categories2D = Misc.newList(CATEGORY_SWEEP_2D_TIME,
                                CATEGORY_SWEEP_3D_TIME);
                    } else {
                        categories2D = Misc.newList(CATEGORY_SWEEP_2D,
                                CATEGORY_SWEEP_3D);
                    }
                    // categories2D = Misc.newList(CATEGORY_SWEEP_2D,
                    //         CATEGORY_SWEEP_3D);
                }
                for (int j = 0; j < angles.length; j++) {
                    String name = "Elevation Angle " + Misc.format(angles[j]);
                    Hashtable dataChoiceProperties =
                        Misc.newHashtable(new Object[] {
                        PROP_ANGLES, new double[] { angles[j] }, PROP_ANGLE,
                        new Double(j), PROP_VOLUMEORSWEEP, VALUE_SWEEP
                    });

                    dataChoiceProperties.put(STATION_LOCATION, namedStation);
                    dataChoiceProperties.put(
                        DataChoice.PROP_ICON,
                        "/auxdata/ui/icons/RadarAngle.gif");
                    momentChoice.addDataChoice(
                        new DirectDataChoice(
                            this, new ObjectArray(
                                momentObj, new Double(
                                    angles[j]), paramName, RadarConstants.VALUE_2D), stationID
                                        + " " + paramName, paramName + " "
                                            + name, categories2D, dataChoiceProperties));
                }

                addDataChoice(momentChoice);
            }
        } catch (Exception excp) {
            logException("Creating data choices", excp);
        }
    }

    /**
     * Check to see if this <code>DoradeDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof CDMRadarDataSource)) {
            return false;
        }

        return (this == (CDMRadarDataSource) o);
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
     * Test program
     *
     * @param args file name
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: DoradeDataSource <dorade_sweepfile>");
            System.exit(1);
        }

        try {
            DataSourceDescriptor dsDesc;

            dsDesc = new DataSourceDescriptor("FILE.DORADERADAR",
                    "test label", null, CDMRadarDataSource.class, "swp\\.*$",
                    true, false, null);

            CDMRadarDataSource dds = new CDMRadarDataSource(dsDesc, args[0],
                                         null);

            System.out.println("created " + dds.getName() + " ("
                               + dds.getDescription() + ")");

            // encode our DoradeDataSource to an XML string
            ucar.unidata.xml.XmlEncoder encoder =
                new ucar.unidata.xml.XmlEncoder();
            String xmlString = encoder.toXml(dds);

            System.out.println("saved to XML");

            // restore our data source from the XML string
            dds = (CDMRadarDataSource) encoder.toObject(xmlString);
            dds.initAfterUnpersistence();
            System.out.println("restored " + dds.getName() + " ("
                               + dds.getDescription() + ")");
            System.out.println("sweep time is "
                               + dds.doMakeDateTimes().get(0));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Make the RadarAdapter for this class
     *
     * @param source source of the data
     *
     * @return corresponding adapter
     *
     * @throws Exception problem opening the file or creating the data
     */
    protected RadarAdapter makeRadarAdapter(String source) throws Exception {
        CDMRadarAdapter adapter = new CDMRadarAdapter(this, source);

        return adapter;
    }

    /**
     * Add the gui components into the list for the properties dialog
     *
     * @param comps List of components
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        List adapters = getRadarAdapters();
        if ((adapters == null) || adapters.isEmpty()) {
            return;
        }
        if (((CDMRadarAdapter) adapters.get(0)).getDataFormatName().equals(
                ucar.nc2.dt.RadialDatasetSweep.LevelII)) {
            Vector<NamedStation> items = new Vector<NamedStation>();
            items.addAll(getStations().values());
            Collections.sort(items);
            stationBox = new JComboBox(items);
            // NamedStation namedStation = null;
            if (namedStation == null) {
                Object o = (getProperties() != null)
                           ? getProperties().get(STATION_LOCATION)
                           : null;

                if ((o != null) && (o instanceof NamedStation)) {
                    namedStation = (NamedStation) o;
                }
            }
            if (namedStation != null) {
                int selected = 0;
                for(int i=0; i< items.size(); i++){
                    if(Misc.equals(namedStation.getID(),((NamedStation)items.get(i)).getID())){
                        selected = i;
                        break;
                    }

                }
                stationBox.setSelectedIndex(selected);
            }
            comps.add(GuiUtils.rLabel("Station Location: "));
            comps.add(stationBox);
        }
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
        if (stationBox != null) {
            Object o = stationBox.getSelectedItem();
            if (o instanceof NamedStation) {
                setStationInfo((NamedStation) o);
            }
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


        // update the data adapters
        List l2as = getRadarAdapters();

        if (l2as != null) {
            for (int i = 0; i < l2as.size(); i++) {
                try {
                    ((CDMRadarAdapter) l2as.get(i)).setStationLocation(
                        (EarthLocation) station.getNamedLocation());
                } catch (Exception excp) {
                    logException("Couldn't set station on adapter", excp);
                }
            }
        }
        flushCache();

        CDMRadarAdapter da = (CDMRadarAdapter) l2as.get(0);
        dataChoices = null;
        doMakeDataChoices();
        setDescription(getName());

        getDataContext().dataSourceChanged(this);
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
     * @param dataSelection data selection
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

    /**
     * Make a name from the properties of the adapter
     * @param da  data adapter
     * @return name
     */
    private String makeName(CDMRadarAdapter da) {
        String stationID  = da.getStationID();
        String dataFormat = da.getDataFormatName();
        return dataFormat + " Radar Data (" + stationID + ")";
    }
}


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

package ucar.unidata.data.profiler;


import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.ma2.StructureData;

import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;

import ucar.nc2.ft.*;

import ucar.unidata.data.*;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.geoloc.Station;
import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.*;

import ucar.visad.Util;


import visad.*;

import visad.data.mcidas.PointDataAdapter;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import visad.jmet.MetUnits;

import java.awt.*;

import java.io.File;
import java.io.IOException;

import java.rmi.RemoteException;

import java.util.*;
import java.util.List;

import javax.swing.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Jul 23, 2010
 * Time: 3:36:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class CDMProfilerDataSource extends FilesDataSource {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            CDMProfilerDataSource.class.getName());

    /** source file */
    private List<String> fileNameOrUrls;

    /** location */
    EarthLocation location = null;

    /** Input for lat/lon center point */
    private LatLonWidget locWidget;

    /** flag for user set location */
    private boolean locationSetByUser = false;

    /** _more_ */
    private float CAPMissing = 2147483647;

    /** _more_ */
    private float WPDNissing = 1.0E38f;

    /** _more_ */
    String source;

    /**
     * Read in the data.
     *
     */
    private Hashtable<String, List> stationsToProfiles;

    /** _more_ */
    private List<NamedStation> selectedStations;

    /** _more_ */
    private List allProfiles;

    /** _more_ */
    private List<Station> stations;

    /** _more_ */
    private List<Double> times;

    //  "LAT", "LON", "Z", "TIME", "SPD", "DIR"

    /** _more_ */
    RealType[] rTypes = {
        RealType.Latitude, RealType.Longitude, RealType.Altitude,
        RealType.Time,
        DataUtil.makeRealType("SPD", CommonUnit.meterPerSecond),
        DataUtil.makeRealType("DIR", CommonUnit.degree)
    };

    /** _more_ */
    String[] params = {
        "LAT", "LON", "Z", "TIME", "SPD", "DIR"
    };

    /**
     * No argument XML persistence constructor
     *
     * @throws visad.VisADException    problem in VisAD
     *
     * @throws VisADException _more_
     */
    public CDMProfilerDataSource() throws VisADException {}


    /**
     * Create a new CDMProfilerDataSource
     *
     * @param descriptor             description of source
     * @param source                 source of the data
     * @param properties             extra properties
     *
     *
     * @throws IOException _more_
     * @throws VisADException        problem accessing data
     */
    public CDMProfilerDataSource(DataSourceDescriptor descriptor,
                                 String source, Hashtable properties)
            throws VisADException, IOException {
        this(descriptor, Misc.newList(source), properties);

    }

    /**
     * Create a new FrontDataSource
     *
     * @param descriptor    Descriptor for this DataSource
     * @param files         List of files or urls
     * @param properties    Extra data source properties
     *
     * @throws IOException _more_
     * @throws VisADException _more_
     */
    public CDMProfilerDataSource(DataSourceDescriptor descriptor, List files,
                                 Hashtable properties)
            throws VisADException, IOException {
        super(descriptor, files, (String) files.get(0),
              "CDM Profiler data source", properties);
        this.fileNameOrUrls = files;
        setFileNameOrUrls(files);
        String nam = "CDM Profiler Data Source";
        initProfilerAll(files, nam);
    }


    /**
     * Extends method in DataSourceImpl to call local initProfiler ()
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        List files = new ArrayList();
        String nam = "CDM Profiler Data Source";
        Iterator itr = this.sources.iterator();
        while(itr.hasNext()){
            String fn = (String)itr.next();
            files.add(fn);
        }

        try{
            initProfilerAll(files, nam);
        } catch (VisADException e){

        } catch (IOException a) {

        }
    }

    /**
     * If we are polling some directory this method gets called when
     * there is a new file. We set the file name, clear our state,
     * reload the metadata and tell listeners of the change.
     *
     * @param  f  new File to use.
     */
    public void newFileFromPolling(File f) {
        String newFilename = f.getPath();
        System.out.println("new file: " + newFilename);
        if ( !getFileNameOrUrls().contains(newFilename)) {
            locationSetByUser = false;
        }
        try {
            initProfiler(newFilename, "CDM Profiler");
        } catch (Exception exc) {
            LogUtil.printException(log_, "Creating new file", exc);
            return;
        }
        setFileNameOrUrls(Misc.newList(f.getPath()));
        setName(newFilename);
        flushCache();
        notifyDataChange();
    }

    /**
     * Get the location where we poll.
     *
     * @return File to poll on.
     */
    protected List getLocationsForPolling() {
        return Misc.newList(getFileNameOrUrls());
    }



    /**
     * _more_
     *
     * @param stations _more_
     *
     * @return _more_
     */
    private List<NamedStation> getNamedStations(List<Station> stations) {
        int                size      = stations.size();
        Unit               unit      = DataUtil.parseUnit("meter");
        List<NamedStation> nstations = new ArrayList();

        for (int i = 0; i < size; i++) {
            Station      st       = stations.get(i);
            NamedStation nstation = null;
            try {
                nstation = new NamedStationImpl(st.getName(), st.getName(),
                        st.getLatitude(), st.getLongitude(),
                        st.getAltitude(), unit);
            } catch (VisADException ss) {}
            catch (RemoteException re) {}
            nstations.add(nstation);
        }
        return nstations;
    }

    /**
     * _more_
     *
     * @param st _more_
     *
     * @return _more_
     */
    private NamedStation getNamedStation(Station st) {
        Unit         unit     = DataUtil.parseUnit("meter");
        NamedStation nstation = null;


        try {
            nstation = new NamedStationImpl(st.getName(), st.getName(),
                                            st.getLatitude(),
                                            st.getLongitude(),
                                            st.getAltitude(), unit);
        } catch (VisADException ss) {}
        catch (RemoteException re) {}

        return nstation;
    }

    /**
     * _more_
     *
     * @param wantFeatureType _more_
     * @param ds _more_
     *
     * @return _more_
     */
    public boolean isMadis(FeatureType wantFeatureType, NetcdfFile ds) {
        if ((wantFeatureType != FeatureType.ANY_POINT)
                && (wantFeatureType != FeatureType.STATION)
                && (wantFeatureType != FeatureType.POINT)
                && (wantFeatureType != FeatureType.STATION_PROFILE)) {
            return false;
        }

        if ( !ds.hasUnlimitedDimension()) {
            return false;
        }
        if (ds.findDimension("recNum") == null) {
            return false;
        }

        if (ds.findVariable("staticIds") == null) {
            return false;
        }
        if (ds.findVariable("nStaticIds") == null) {
            return false;
        }
        if (ds.findVariable("lastRecord") == null) {
            return false;
        }
        if (ds.findVariable("prevRecord") == null) {
            return false;
        }


        if (ds.findVariable("latitude") == null) {
            return false;
        }
        if (ds.findVariable("longitude") == null) {
            return false;
        }
        if (ds.findVariable("observationTime") == null) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @param wantFeatureType _more_
     * @param ds _more_
     *
     * @return _more_
     */
    public boolean isWPDN(FeatureType wantFeatureType, NetcdfFile ds) {
        String title = ds.findAttValueIgnoreCase(null, "title", null);
        return (title != null)
               && (title.startsWith("WPDN data")
                   || title.startsWith("RASS data"));
    }

    /**
     * _more_
     *
     *
     * @param sources _more_
     * @param sname _more_
     *
     * @throws IOException _more_
     * @throws visad.VisADException _more_
     */
    private void initProfilerAll(List<String> sources, String sname)
            throws IOException, visad.VisADException {
        stationsToProfiles = new Hashtable();
        times              = new ArrayList();
        allProfiles        = new ArrayList();
        int size = sources.size();
        for (int i = 0; i < size; i++) {
            initProfiler(sources.get(i), sname);
        }

    }

    /**
     * _more_
     *
     * @param fileNameOrUrl _more_
     * @param sname _more_
     *
     * @throws IOException _more_
     * @throws visad.VisADException _more_
     */
    private void initProfiler(String fileNameOrUrl, String sname)
            throws IOException, visad.VisADException {

        Formatter log = new Formatter();
        FeatureDatasetPoint dataset =
            (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.STATION_PROFILE, fileNameOrUrl, null, log);

        if (dataset == null) {
            throw new BadDataException("Could not open trajectory file:"
                                       + fileNameOrUrl);
        }


        List<FeatureCollection> fcList =
            dataset.getPointFeatureCollectionList();
        FeatureCollection fc = fcList.get(0);
        StationProfileFeatureCollection spc =
            (StationProfileFeatureCollection) fc;
        FeatureType ftype   = spc.getCollectionFeatureType();

        boolean     ismadis = isMadis(ftype, dataset.getNetcdfFile());
        boolean     iswpdn  = isWPDN(ftype, dataset.getNetcdfFile());
        // StationProfileFeatureCollection stationCollection = (StationProfileFeatureCollection) fc;
        // StationProfileFeature feature = stationCollection.getStationProfileFeature(sb.s);
        //   (String id, String name, double latitude, double longitude, double alt,Unit unit)

        //(index -> (lat, lon, day, TIME, Z, DIR, SPD));
        if (ismadis) {
            initMadis(spc, sname);
        } else if (iswpdn) {
            initWPDN(spc, sname);
        }
    }

    /**
     * _more_
     *
     * @param sts _more_
     */
    private void addStations(List<Station> sts) {
        if (stations == null) {
            stations         = sts;
            selectedStations = getNamedStations(sts);
        } else {
            int size = sts.size();
            for (int i = 0; i < size; i++) {
                Station s = sts.get(i);
                if ( !selectedStations.contains(getNamedStation(s))) {
                    stations.add(s);
                    selectedStations.contains(getNamedStation(s));
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param spc _more_
     * @param sname _more_
     *
     * @throws IOException _more_
     * @throws visad.VisADException _more_
     */
    private void initWPDN(StationProfileFeatureCollection spc, String sname)
            throws IOException, visad.VisADException {

        List<Station>      lstations  = spc.getStations();
        List<NamedStation> lnstations = getNamedStations(lstations);
        addStations(lstations);

        int size = lstations.size();
        if (size < 3) {
            sname = sname + StringUtil.join(", ", lnstations);
        } else {
            sname = sname + size + " stations";
        }

        setName(sname);
        setDescription(sname);

        String[] units = new String[6];

        spc.resetIteration();
        int ii = 0;
        int j0 = 0;
        while (ii < size) {
            Station               st  = lstations.get(ii);
            StationProfileFeature spf = spc.getStationProfileFeature(st);
            //NestedPointFeatureCollection spf2 =  (NestedPointFeatureCollection)spf1;
            PointFeatureCollectionIterator iter =
                spf.getPointFeatureCollectionIterator(-1);  // not multiple

            List<Date> tList         = spf.getTimes();
            int        tsize         = tList.size();
            List       latVector     = new Vector();
            List       lonVector     = new Vector();
            List       altVector     = new Vector();

            List       timeVector    = new Vector();
            List       windSpdVector = new Vector();
            List       windDirVector = new Vector();
            int        jj            = 0;
            while (jj < tsize) {  //iter.hasNext()) {
                //ProfileFeature pf0 =  (ProfileFeature)iter.next();
                List           latList  = new ArrayList<Double>();
                List           lonList  = new ArrayList<Double>();
                List           altList  = new ArrayList<Double>();

                List           timeList = new ArrayList<Double>();
                List           uSpdList = new ArrayList<Double>();
                List           vSpdList = new ArrayList<Double>();
                Date           dt       = tList.get(jj);

                DateTime       dateTime = new DateTime(dt);

                ProfileFeature pf0      = spf.getProfileByDate(dt);
                while (pf0.hasNext()) {
                    PointFeature  p0   = pf0.next();
                    StructureData sd   = p0.getData();
                    float         uspd = sd.convertScalarFloat("uComponent");
                    float         vspd = sd.convertScalarFloat("vComponent");
                    if ((uspd != McIDASUtil.MCMISSING)
                            && (uspd != WPDNissing)
                            && (vspd != McIDASUtil.MCMISSING)
                            && (vspd != WPDNissing)) {
                        latList.add(sd.convertScalarDouble("staLat"));
                        lonList.add(sd.convertScalarDouble("staLon"));
                        altList.add(sd.convertScalarDouble("levels")
                                    + sd.convertScalarDouble("staElev"));
                        timeList.add((double) dateTime.getValue());  //sd.convertScalarFloat("observationTime"));
                        uSpdList.add((double) uspd);
                        vSpdList.add((double) vspd);
                    }
                    if (ii == 0) {
                        units[0] = sd.findMember("staLat").getUnitsString();
                        units[1] = sd.findMember("staLon").getUnitsString();
                        units[2] = "meter";  //sd.findMember("levels").getUnitsString();
                        units[3] = dateTime.getUnit().toString();  //sd.findMember("observationTime").getUnitsString();
                        //units[4] = "knots";
                        //units[5] = "degree_N";
                        units[4] = sd.findMember("uComponent").getUnitsString();
                        units[5] = "degree";

                    }

                }
                FieldImpl  dataFieldImpl = null;

                int        dsize         = latList.size();

                double[][] data          = new double[dsize][6];

                for (int i = 0; i < dsize; i++) {
                    data[i][0] = (Double) latList.get(i);
                    data[i][1] = (Double) lonList.get(i);
                    data[i][2] = (Double) altList.get(i);
                    data[i][3] = (Double) timeList.get(i);
                    data[i][4] = getWindSpd((Double) uSpdList.get(i),
                                            (Double) vSpdList.get(i));
                    data[i][5] = getWindDir((Double) uSpdList.get(i),
                                            (Double) vSpdList.get(i));
                    latVector.add(latList.get(i));
                    lonVector.add(lonList.get(i));
                    altVector.add(altList.get(i));
                    timeVector.add(timeList.get(i));
                    windSpdVector.add(data[i][4]);
                    windDirVector.add(data[i][5]);
                }

                PointFeature   p0 = null;
                ProfileFeature pf = null;


                // {"latitude","longitude", "levels",  "time", "windSpeed", "windDir" };
                int[] scalingFactors = {
                    1, 1, 1, 1, 1, 1
                };

                if (data.length > 0) {
                    dataFieldImpl = makeField(data, units, scalingFactors);
                }

                if (dataFieldImpl != null) {
                    times.add(Double.valueOf(data[0][3]));
                    allProfiles.add(dataFieldImpl);
                                    }
                jj++;
            }

            FieldImpl  dataFieldImpl = null;
            int        dsize         = latVector.size();
            double[][] data1         = new double[dsize][6];

            for (int i = 0; i < dsize; i++) {
                data1[i][0] = (Double) latVector.get(i);
                data1[i][1] = (Double) lonVector.get(i);
                data1[i][2] = (Double) altVector.get(i);
                data1[i][3] = (Double) timeVector.get(i);
                data1[i][4] = (Double) windSpdVector.get(i);
                data1[i][5] = (Double) windDirVector.get(i);
            }



            // String [] params = {"latitude","longitude", "levels",  "time", "windSpeed", "windDir" };
            int[] scalingFactors = {
                1, 1, 1, 1, 1, 1
            };
            if (data1.length > 0) {
                dataFieldImpl = makeField(data1, units, scalingFactors);
            }
            if (dataFieldImpl != null) {
                if (stationsToProfiles.get(st.getName()) == null) {
                    List<FieldImpl> alist = new ArrayList<FieldImpl>();
                    stationsToProfiles.put(st.getName(), alist);
                }
                stationsToProfiles.get(st.getName()).add(dataFieldImpl);
            }
            ii++;
        }
    }

    /**
     * _more_
     *
     * @param spc _more_
     * @param name _more_
     *
     * @throws IOException _more_
     * @throws visad.VisADException _more_
     */
    private void initWPDNOld(StationProfileFeatureCollection spc, String name)
            throws IOException, visad.VisADException {

        stations         = spc.getStations();
        selectedStations = getNamedStations(stations);
        int size = stations.size();
        if (size < 3) {
            name = name + StringUtil.join(", ", selectedStations);
        } else {
            name = name + size + " stations";
        }

        setName(name);
        setDescription(name);


        String[] units = new String[6];

        spc.resetIteration();
        int ii = 0;
        while (ii < size) {
            Station               st  = stations.get(ii);
            StationProfileFeature spf = spc.getStationProfileFeature(st);
            //NestedPointFeatureCollection spf2 =  (NestedPointFeatureCollection)spf1;
            PointFeatureCollectionIterator iter =
                spf.getPointFeatureCollectionIterator(-1);  // not multiple

            List<Date> tList    = spf.getTimes();
            int        tsize    = tList.size();
            List       latList  = new ArrayList<Double>();
            List       lonList  = new ArrayList<Double>();
            List       altList  = new ArrayList<Double>();

            List       timeList = new ArrayList<Double>();
            List       uSpdList = new ArrayList<Double>();
            List       vSpdList = new ArrayList<Double>();
            int        jj       = 0;
            while (jj < tsize) {  //iter.hasNext()) {
                //ProfileFeature pf0 =  (ProfileFeature)iter.next();
                Date           dt       = tList.get(jj);

                DateTime       dateTime = new DateTime(dt);
                ProfileFeature pf0      = spf.getProfileByDate(dt);
                while (pf0.hasNext()) {
                    PointFeature  p0   = pf0.next();
                    StructureData sd   = p0.getData();
                    float         uspd = sd.convertScalarFloat("uComponent");
                    float         vspd = sd.convertScalarFloat("vComponent");
                    if ((uspd != McIDASUtil.MCMISSING)
                            && (uspd != WPDNissing)
                            && (vspd != McIDASUtil.MCMISSING)
                            && (vspd != WPDNissing)) {
                        latList.add(sd.convertScalarDouble("staLat"));
                        lonList.add(sd.convertScalarDouble("staLon"));
                        altList.add(sd.convertScalarDouble("levels")
                                    + sd.convertScalarDouble("staElev"));
                        timeList.add((double) dateTime.getValue());  //sd.convertScalarFloat("observationTime"));
                        uSpdList.add((double) uspd);
                        vSpdList.add((double) vspd);
                    }
                    if (ii == 0) {
                        units[0] = sd.findMember("staLat").getUnitsString();
                        units[1] = sd.findMember("staLon").getUnitsString();
                        units[2] = "meter";  //sd.findMember("levels").getUnitsString();
                        units[3] = dateTime.getUnit().toString();  //sd.findMember("observationTime").getUnitsString();
                        units[4] = "knots";
                        units[5] = "degree_N";

                    }

                }

                jj++;
            }
            FieldImpl  dataFieldImpl = null;
            int        dsize         = latList.size();
            double[][] data          = new double[dsize][6];

            for (int i = 0; i < dsize; i++) {
                data[i][0] = (Double) latList.get(i);
                data[i][1] = (Double) lonList.get(i);
                data[i][2] = (Double) altList.get(i);
                data[i][3] = (Double) timeList.get(i);
                data[i][4] = (Double) getWindSpd((Double) uSpdList.get(i),
                        (Double) vSpdList.get(i));
                data[i][5] = (Double) getWindDir((Double) uSpdList.get(i),
                        (Double) vSpdList.get(i));
            }

            PointFeature   p0 = null;
            ProfileFeature pf = null;

            // String [] params = {"latitude","longitude", "levels",  "time", "windSpeed", "windDir" };
            int[] scalingFactors = {
                1, 1, 1, 1, 1, 1
            };


            dataFieldImpl = makeField(data, units, scalingFactors);

            if (dataFieldImpl != null) {
                if (stationsToProfiles.get(st.getName()) == null) {
                    List<FieldImpl> alist = new ArrayList<FieldImpl>();
                    stationsToProfiles.put(st.getName(), alist);
                }
                stationsToProfiles.get(st.getName()).add(dataFieldImpl);
            }

            ii++;
        }
    }

    /**
     * _more_
     *
     * @param u _more_
     * @param v _more_
     *
     * @return _more_
     */
    private double getWindSpd(double u, double v) {
        return Math.sqrt(u * u + v * v);
    }

    /**
     * _more_
     *
     * @param windU _more_
     * @param windV _more_
     *
     * @return _more_
     */
    private double getWindDir(double windU, double windV) {

        double a    = windU / windV;
        double per  = (180 / Math.PI);
        int    tita = 0;

        if (windV >= 0) {
            tita = 180;
        } else if ((windV < 0) && (windU < 0)) {
            tita = 0;
        } else if ((windU >= 0) && (windV < 0)) {
            tita = 360;
        }

        return Math.atan(a) * per + tita;
        //              return (270 - ( Math.atan2(windV,windU) * (180/Math.PI)));

    }

    /**
     * _more_
     *
     * @param spc _more_
     * @param name _more_
     *
     * @throws IOException _more_
     * @throws visad.VisADException _more_
     */
    private void initMadis(StationProfileFeatureCollection spc, String name)
            throws IOException, visad.VisADException {

        stations         = spc.getStations();
        selectedStations = getNamedStations(stations);

        int size = stations.size();
        if (size < 3) {
            name = name + StringUtil.join(", ", selectedStations);
        } else {
            name = name + " " + size + " stations";
        }

        setName(name);
        setDescription(name);

        String[] units = new String[6];

        spc.resetIteration();
        int ii = 0;
        int j0 = 0;
        while (ii < size) {
            Station               st  = stations.get(ii);
            StationProfileFeature spf = spc.getStationProfileFeature(st);
            //NestedPointFeatureCollection spf2 =  (NestedPointFeatureCollection)spf1;
            PointFeatureCollectionIterator iter =
                spf.getPointFeatureCollectionIterator(-1);  // not multiple

            List<Date> tList         = spf.getTimes();
            int        tsize         = tList.size();
            List       latVector     = new Vector();
            List       lonVector     = new Vector();
            List       altVector     = new Vector();

            List       timeVector    = new Vector();
            List       windSpdVector = new Vector();
            List       windDirVector = new Vector();
            int        jj            = 0;
            while (jj < tsize) {  //iter.hasNext()) {
                //ProfileFeature pf0 =  (ProfileFeature)iter.next();
                List           latList     = new ArrayList<Double>();
                List           lonList     = new ArrayList<Double>();
                List           altList     = new ArrayList<Double>();

                List           timeList    = new ArrayList<Double>();
                List           windSpdList = new ArrayList<Double>();
                List           windDirList = new ArrayList<Double>();
                Date           dt          = tList.get(jj);

                DateTime       dateTime    = new DateTime(dt);

                ProfileFeature pf0         = spf.getProfileByDate(dt);
                while (pf0.hasNext()) {
                    PointFeature  p0  = pf0.next();
                    StructureData sd  = p0.getData();
                    float         spd = sd.convertScalarFloat("windSpeed");
                    float         dir = sd.convertScalarFloat("windDir");
                    if ((spd != McIDASUtil.MCMISSING) && (spd != CAPMissing)
                            && (dir != McIDASUtil.MCMISSING)
                            && (dir != CAPMissing)) {
                        latList.add(sd.convertScalarDouble("latitude"));
                        lonList.add(sd.convertScalarDouble("longitude"));
                        altList.add(sd.convertScalarDouble("levels")
                                    + st.getAltitude());
                        timeList.add((double) dateTime.getValue());  //sd.convertScalarFloat("observationTime"));
                        windSpdList.add(sd.convertScalarDouble("windSpeed"));
                        windDirList.add(sd.convertScalarDouble("windDir"));
                    }
                    if (ii == 0) {
                        units[0] = sd.findMember("latitude").getUnitsString();
                        units[1] =
                            sd.findMember("longitude").getUnitsString();
                        units[2] = sd.findMember("levels").getUnitsString();
                        units[3] = dateTime.getUnit().toString();  //sd.findMember("observationTime").getUnitsString();
                        units[4] =
                            sd.findMember("windSpeed").getUnitsString();
                        units[5] = sd.findMember("windDir").getUnitsString();

                    }

                }
                FieldImpl  dataFieldImpl = null;

                int        dsize         = latList.size();

                double[][] data          = new double[dsize][6];

                for (int i = 0; i < dsize; i++) {
                    data[i][0] = (Double) latList.get(i);
                    data[i][1] = (Double) lonList.get(i);
                    data[i][2] = (Double) altList.get(i);
                    data[i][3] = (Double) timeList.get(i);
                    data[i][4] = (Double) windSpdList.get(i);
                    data[i][5] = (Double) windDirList.get(i);
                    latVector.add(latList.get(i));
                    lonVector.add(lonList.get(i));
                    altVector.add(altList.get(i));
                    timeVector.add(timeList.get(i));
                    windSpdVector.add(windSpdList.get(i));
                    windDirVector.add(windDirList.get(i));
                }

                PointFeature   p0 = null;
                ProfileFeature pf = null;


                // {"latitude","longitude", "levels",  "time", "windSpeed", "windDir" };
                int[] scalingFactors = {
                    1, 1, 1, 1, 1, 1
                };

                if (data.length > 0) {
                    dataFieldImpl = makeField(data, units, scalingFactors);
                }

                if (dataFieldImpl != null) {
                    times.add(Double.valueOf(data[0][3]));
                    allProfiles.add(dataFieldImpl);

                }
                jj++;
            }

            FieldImpl  dataFieldImpl = null;
            int        dsize         = latVector.size();
            double[][] data1         = new double[dsize][6];

            for (int i = 0; i < dsize; i++) {
                data1[i][0] = (Double) latVector.get(i);
                data1[i][1] = (Double) lonVector.get(i);
                data1[i][2] = (Double) altVector.get(i);
                data1[i][3] = (Double) timeVector.get(i);
                data1[i][4] = (Double) windSpdVector.get(i);
                data1[i][5] = (Double) windDirVector.get(i);
            }



            // String [] params = {"latitude","longitude", "levels",  "time", "windSpeed", "windDir" };
            int[] scalingFactors = {
                1, 1, 1, 1, 1, 1
            };
            if (data1.length > 0) {
                dataFieldImpl = makeField(data1, units, scalingFactors);
            }
            if (dataFieldImpl != null) {
                if (stationsToProfiles.get(st.getName()) == null) {
                    List<FieldImpl> alist = new ArrayList<FieldImpl>();
                    stationsToProfiles.put(st.getName(), alist);
                }
                stationsToProfiles.get(st.getName()).add(dataFieldImpl);
            }
            ii++;
        }
    }

    // out of this will either come a FieldImpl, a ObservationDBImpl,
    // or a StationObDBImpl

    /**
     * _more_
     *
     * @param units _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    RealType[] getTypes(String[] units) {

        int        numParams = params.length;
        RealType[] types     = new RealType[numParams];

        for (int i = 0; i < numParams; i++) {
            // get the name
            String name = params[i];
            // make the unit
            Unit unit = DataUtil.parseUnit(units[i]);
            types[i] = DataUtil.makeRealType(name, unit);

        }

        return types;

    }


    /** _more_ */
    private boolean debug = false;

    /**
     * _more_
     *
     * @param data _more_
     * @param units _more_
     * @param scalingFactors _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    private FieldImpl makeField(double[][] data, String[] units,
                                int[] scalingFactors)
            throws VisADException {

        FieldImpl field = null;
        Unit[]    defaultUnits;
        boolean   debug = false;
        //int numObs = data[0].length;
        int numObs = data.length;
        if (numObs == 0) {
            throw new VisADException("No data available");
        }
        if (debug) {
            System.out.println("Number of observations = " + numObs);
        }

        RealType     domainType = RealType.getRealType("index");
        Integer1DSet domain     = new Integer1DSet(domainType, numObs);

        // now make range (Tuple) type
        MetUnits unitTranslator = new MetUnits();
        int      numParams      = params.length;
        if (debug) {
            System.out.println("Number of parameters = " + numParams);
        }

        defaultUnits = new Unit[numParams];
        Vector  usedUnits = new Vector();
        boolean noText    = true;
        for (int i = 0; i < numParams; i++) {
            // get the name
            String name = params[i];
            // make the unit
            Unit unit = DataUtil.parseUnit(units[i]);

            defaultUnits[i] = unit;


        }

        TupleType rangeType;
        if (noText)  // all Reals
        {
            RealType[] newTypes = new RealType[rTypes.length];
            for (int i = 0; i < rTypes.length; i++) {
                newTypes[i] = (RealType) rTypes[i];
            }
            rangeType = new RealTupleType(newTypes);
        } else       // all Texts or mixture of Text and Reals
        {
            rangeType = new TupleType(rTypes);
        }

        // make the field
        FunctionType functionType = new FunctionType(domainType, rangeType);
        /*
        field = (noText)
                ? new FlatField(functionType, domain)
                : new FieldImpl(functionType, domain);
                */
        field = new FieldImpl(functionType, domain);


        if (debug) {
            System.out.println("filling in data");
        }
        long millis = System.currentTimeMillis();
        // now, fill in the data
        Scalar[] firstTuple  = null;  // use this for saving memory/time
        Unit[]   actualUnits = null;
        for (int i = 0; i < numObs; i++) {
            Scalar[] scalars = new Real[numParams];

            for (int j = 0; j < numParams; j++) {
                double value = ((data[i][j] == McIDASUtil.MCMISSING)
                                || (data[i][j] == CAPMissing))
                               ? Double.NaN
                               : data[i][j];
                if (firstTuple == null) {  //
                    try {
                        scalars[j] = new Real((RealType) rTypes[j], value,
                                defaultUnits[j]);
                    } catch (VisADException excp) {  // units problem
                        scalars[j] = new Real((RealType) rTypes[j], value);

                    }
                    usedUnits.add(((Real) scalars[j]).getUnit());
                } else {
                    scalars[j] = ((Real) firstTuple[j]).cloneButValue(value);
                }

            }
            if (noText && (actualUnits == null)) {
                actualUnits = new Unit[usedUnits.size()];
                for (int k = 0; k < usedUnits.size(); k++) {
                    actualUnits[k] = (Unit) usedUnits.get(k);
                }
            }
            try {
                Data sample = (noText == true)
                              ? new RealTuple((RealTupleType) rangeType,
                                  (Real[]) scalars, null, actualUnits, false)
                              : new Tuple(rangeType, scalars, false, false);
                field.setSample(i, sample, false, (i == 0));  // don't make copy, don't check type after first
            } catch (VisADException e) {
                e.printStackTrace();
            } catch (java.rmi.RemoteException e) {
                ;
            }
            if (firstTuple == null) {
                firstTuple = scalars;
            }
        }
        if (debug) {
            System.out.println("data fill took "
                               + (System.currentTimeMillis() - millis)
                               + " ms");
        }

        return field;

    }

    /**
     * Remove missing altitudes
     *
     * @param samples   the samples of all data
     * @param altIndex  the index of altitudes
     *
     * @return an array with missing data removed
     */
    private float[][] removeMissingHeights(float[][] samples, int altIndex) {
        float[][] newSamples = null;
        int[]     indices    = new int[samples[0].length];
        int       counter    = 0;
        for (int i = 0; i < samples[altIndex].length; i++) {
            if ( !Float.isNaN(samples[altIndex][i])) {
                indices[counter++] = i;
            }
        }
        if (counter == 0) {
            return newSamples;
        }
        newSamples = new float[samples.length][counter];
        for (int i = 0; i < counter; i++) {
            for (int j = 0; j < samples.length; j++) {
                newSamples[j][i] = samples[j][indices[i]];
            }
        }
        return newSamples;
    }

    /**
     * Called by DataSourceImpl to make the actual DataChoice(s) and add them
     * to a list of Datachoices; the DataSourceImpl
     * then checks to see if any derived
     * datachoices are possible to derive from these, and shows them if so.
     * Used data supplied in the constructor.
     */
    public void doMakeDataChoices() {

        // to make datachoice
        // 2nd arg is url or request String for data from remote ADDE server.
        // 3rd arg is key to param defaults table
        //                  in resources/paramdefaults.xml
        // 4th arg is "parameter" label in DataSelector gui "Fields" panel.
        // DataCategory.CATEGORY_PROFILER_ONESTA
        //  comes from unidata/data/DataCategory.java; see alos controls.xml -
        // connections which "controls" (displays) each parameter can use.

        DataChoice choice = null;
        List singleDC = DataCategory.parseCategories(
                            DataCategory.CATEGORY_PROFILER_ONESTA, false);
        List compositeDC =
            DataCategory.parseCategories(DataCategory.CATEGORY_PROFILER_PLAN + ";"
                                   + DataCategory.CATEGORY_PROFILER_3D,
                                         false);
        compositeDC.addAll(singleDC);

        CompositeDataChoice composite = new CompositeDataChoice(this, "",
                                            "Winds", "Profiler winds",
                                            compositeDC);
        addDataChoice(composite);
        //  composite.addDataChoice(new DirectDataChoice(this, location, "winds",
        //          location.toString(), singleDC));
        for (int i = 0; i < selectedStations.size(); i++) {
            NamedStation station     = (NamedStation) selectedStations.get(i);
            String       stationName = station.getName();
            composite.addDataChoice(new DirectDataChoice(this, station,
                    "winds", stationName, singleDC));
        }

    }


    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws java.rmi.RemoteException    Java RMI problem
     *
     * @throws RemoteException _more_
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        boolean singleStation = !(dataChoice instanceof CompositeDataChoice);

        //Recast the field into a different FieldImpl we can use.
        FieldImpl obs           = null;
        FieldImpl dataFieldImpl = null;
         
        if (singleStation) {
            List dataFieldImplList =
                stationsToProfiles.get(dataChoice.toString());
            if (dataFieldImplList.size() > 0) {
                Vector datas = new Vector();
                for (int i = 0; i < dataFieldImplList.size(); i++) {
                    datas.add(dataFieldImplList.get(i));
                }
                dataFieldImpl = PointObFactory.mergeData(datas);
            }
            obs = recastProfilerSingleStationData(dataFieldImpl, 1);
        } else {
            Vector           datas = new Vector();
            PointDataAdapter pda   = null;
            int              ssize = times.size();
            FieldImpl        data  = null;
            double[]         dt    = new double[ssize];
            for (int i = 0; i < ssize; i++) {
                dt[i] = times.get(i);
            }
            int[]      sortedAzs = QuickSort.sort(dt);
            DateTime[] dts       = new DateTime[ssize];
            for (int i = 0; i < ssize; i++) {

                data   = (FieldImpl) allProfiles.get(sortedAzs[i]);
                dts[i] = new DateTime(dt[i]);
                if (data != null) {
                    datas.add(data);
                }

            }
            dataFieldImpl = PointObFactory.mergeData(datas);
            if (data == null) {
                return null;
            }
            obs = recastProfilerMultiStationData(dataFieldImpl, 1);
        }

        return obs;
    }


    /**
     * _more_
     *
     * @param input _more_
     * @param obInt _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    protected FieldImpl recastProfilerSingleStationData(FieldImpl input,
            int obInt)
            throws VisADException {

        //long millis = System.currentTimeMillis();

        FieldImpl retField = null;

        try {
            // input expected (index -> (TIME, Z, DIR, SPD, DAY)); all reals.

            TupleType    rangetype = null;
            Integer1DSet indexSet  = null;

            try {
                rangetype =
                    (TupleType) ((FunctionType) input.getType()).getRange();
                indexSet = (Integer1DSet) input.getDomainSet();
            } catch (ClassCastException ce) {
                throw new IllegalArgumentException(
                    "don't know how to convert input to a point ob");
            }

            //System.out.println("      range type = "+rangetype);
            //System.out.println("      index set  = "+indexSet);
            // should show range type = (TIME, Z, DIR, SPD, DAY)
            //        show index set  = Integer1DSet: Length = 1702 or so
            // 1702, is from 23 hours of 74 levels per hour
            // 74 from 16000 meters by every 250 meters or so

            //long mil2 = System.currentTimeMillis();

            // this should be true
            //boolean allReals = (rangetype instanceof RealTupleType);
            // if (allReals) System.out.println("  range is all reals");

            int     timeIndex  = rangetype.getIndex(rTypes[3].toString());
            boolean hasDayTime = (timeIndex != -1);
            boolean hasDateHMS = false;
            if ( !hasDayTime && !hasDateHMS) {
                throw new IllegalArgumentException(
                    "can't find DateTime components");
            }

            // get Z DIR SPD indices
            int zIndex   = rangetype.getIndex(rTypes[2].toString());
            int dirIndex = rangetype.getIndex(rTypes[5].toString());
            int spdIndex = rangetype.getIndex(rTypes[4].toString());



            // check for time groups; time value = first Real component in each ob;
            // there are several obs in a row with the same time
            int          numtimes      = 0;
            DateTime     dateTime      = null;
            Real         ctime         = new Real(-999.0);  // test time
            Real         thisgrouptime = new Real(-999.0);  // test time

            List         timesList     = new ArrayList();
            List         zsList        = new ArrayList();
            List         dsList        = new ArrayList();
            List         obFFsList     = new ArrayList();
            Real         zvalue;
            RealTuple    dirspd = null;
            FunctionType onetimeFT;
            FlatField    onetimeFF;

            int          timecount = 0;

            // The data from the adde server is a list of obs, each ob have
            //one time height dir spd observation. In these list there are
            //list of sequential observations with the same time. Think of
            //these as groups of observations.  observation groups differ in
            //time by 6 minutes. Users may wish to see data plotted every 6
            //minutes, or every 12 minutes, or every 30 minutes. So only
            //select to use data from the list from every Nth group: every
            //group for 6 minute plots, every 2nd group for 12 minutes plots,
            //every 5th group for 30 minute plots. In the code here the test
            //if whether to use a group is
            //  first increment "timecount" for each time group;
            //  then test if( timecount % obInt ==0) which if true
            //means have a time-ob in a group of times to use.

            // hourly data plots get only hourly data from the adde server
            //and this testing is sidesteped by using obInt = 1 in that case.

            // look at every observation of form (TIME, Z, DIR, SPD, DAY)
            for (int i = 0; i < indexSet.getLength(); i++) {

                // get an ob -- one time height wind group
                Tuple ob = (Tuple) input.getSample(i);

                // get time of this ob, in funny format as 240000.0 for 24:00 UTC
                Real thisobstime = (Real) ob.getComponent(3);

                // see if this is the same time as the previous time encountered;
                // if so, group up the ob data for this one, a single
                //    height-dir-spd obs for this time (smallest unit of data)
                if (thisobstime.getValue() == ctime.getValue()) {
                    // for each ob at this time, make a z->(dir, spd)  thingy
                    zvalue = (Real) ob.getComponent(zIndex);


                    dirspd = new RealTuple(new Real[] {
                        (Real) ob.getComponent(dirIndex),
                        (Real) ob.getComponent(spdIndex) });
                    //    if(!ob.getComponent(dirIndex).isMissing() && !ob.getComponent(spdIndex).isMissing()) {
                    zsList.add(zvalue);
                    dsList.add(dirspd);
                    //  }
                    //  added a (z -> dir-spd) observation
                } else {
                    // have read a new time.

                    //  finalize previous time's group (all z-> dir spd groups)
                    // but only IF you are dealing with a "timecount" to save
                    if ((timecount % obInt == 0) && (dsList.size() > 0)) {
                        //  first make sure have processed some data;
                        // (is not very first item in list)

                        if (dirspd != null) {
                            // make domain array of Z-s
                            float[][] zsetfloats =
                                new float[1][zsList.size()];
                            for (int j = 0; j < zsList.size(); j++) {
                                //zs[j] = (Real) zsList.get(j);
                                zsetfloats[0][j] =
                                    (float) ((Real) zsList.get(j)).getValue();
                            }

                            // print out height values per time step
                            //System.out.println(" zsList = "+zsList.toString());
                            // print out how many dir-spd groups per time step
                            //System.out.println("size of dslist is "+dsList.size());

                            // make range array of dir-spd-s
                            Data[] ds = new RealTuple[dsList.size()];
                            for (int j = 0; j < dsList.size(); j++) {
                                ds[j] = (RealTuple) dsList.get(j);
                            }

                            // sort the z and related dir-spd groups mmm
                            if (zsList.size() != dsList.size()) {
                                System.out.println("  Size mismatch");
                            }

                            // The data has mutiple and overlapping Zs; for example,
                            // two obs at one time at 8000 m, due to low mode and
                            // high mode scans from the Profiler, both received here.
                            TreeMap sortedlist = new TreeMap();
                            // load in reverse order so lower z values from
                            // low mode scan have preference over high mode values
                            for (int j = dsList.size() - 1; j >= 0; j--) {
                                sortedlist.put(new Float(zsetfloats[0][j]),
                                        ds[j]);
                            }

                            // extract the sorted lists of z-s, and dir-spd tuples,
                            // with no duplicates
                            float[][] sortedZ =
                                new float[1][sortedlist.size()];
                            Data[] sortedDS =
                                new RealTuple[sortedlist.size()];
                            Object[] zobjarray =
                                (sortedlist.keySet()).toArray();
                            for (int j = 0; j < sortedlist.size(); j++) {
                                sortedZ[0][j] =
                                    ((Float) zobjarray[j]).floatValue();
                                sortedDS[j] = (Data) sortedlist.get(
                                    (Float) zobjarray[j]);
                            }

                            // Make the (z->(dir,spd)) FlatField

                            //make the FunctionType(MathType domain, MathType range)
                            onetimeFT = new FunctionType(RealType.Altitude,
                                    sortedDS[0].getType());

                            // do cstr FlatField(FunctionType type, Set domain_set)
                            // use Gridded1DSet(MathType type, float[][] samples,
                            //                  int lengthX)

                            //IrregularSet zset = new
                            //    IrregularSet(RealType.Altitude, sortedZ);
                            Gridded1DSet zset =
                                new Gridded1DSet(RealType.Altitude, sortedZ,
                                    sortedZ[0].length);

                            onetimeFF = new FlatField(onetimeFT, zset);
                            //set range data with FlatField.setSamples(Data[] range,
                            // boolean copy)
                            onetimeFF.setSamples(sortedDS, false);
                            //System.out.println("onetimeFF is "+onetimeFF.toString())

                            obFFsList.add(onetimeFF);
                            //System.out.println("finished obFFsList with "+dateTime)
                        }
                    }  // if final time is at right time interval


                    // advance time counter, but only once for each distinct new
                    // time of a group of like-timed data obs

                    if ((obInt != 1)
                            && !(thisobstime.toString()).equals(
                                thisgrouptime.toString())) {
                        // a new group time was hit; neither the previous time
                        // as checked and used way up above, nor the just found time

                        // only advance time counter once per group
                        timecount++;

                        //System.out.println("   found new group time at i= "+i+
                        //  "   just past grouptime = "+thisgrouptime+
                        //  "   this new obs time = "+thisobstime+
                        // "  new timecount= "+ timecount);

                        // reset to new group time
                        thisgrouptime = thisobstime;
                    }

                    // if this group time counter is at the right interval
                    if (timecount % obInt == 0) {

                        zsList.clear();
                        dsList.clear();

                        // now advance to first ob for new time value
                        ctime = thisobstime;
                        numtimes++;
                        // make DateTime from TIME and DAY real values
                        double time = (double) ((Real) ob.getComponent(
                                          timeIndex)).getValue();
                        // Date dd = new Date((long)time);
                        dateTime = new DateTime(time);

                        //System.out.println("  i="+i+"  obInt = "+obInt+
                        // "  timecount="+timecount+" tc%obInt = "+timecount%obInt+
                        // "  new datetime is "+dateTime);
                        //System.out.println("  time to use at timecount="+timecount
                        // +"  new datetime is "+dateTime);

                        timesList.add(dateTime);

                        zvalue = (Real) ob.getComponent(zIndex);

                        if ( !ob.getComponent(dirIndex).isMissing()
                                && !ob.getComponent(spdIndex).isMissing()) {
                            zsList.add(zvalue);

                            dirspd = new RealTuple(new Real[] {
                                (Real) ob.getComponent(dirIndex),
                                (Real) ob.getComponent(spdIndex) });

                            dsList.add(dirspd);
                        }
                        //System.out.println("  added first z to d-s ob");
                    }

                }  // else if new time

            }

            // finalize last time
            ///
            //  finalize previous time's group (all z-> dir spd groups)
            // but only IF you are dealing with a "timecount" to save
            if (timecount % obInt == 0) {
                //  first make sure have processed some data;
                // (is not very first item in list)

                if (dirspd != null) {
                    // make domain array of Z-s
                    float[][] zsetfloats = new float[1][zsList.size()];
                    for (int j = 0; j < zsList.size(); j++) {
                        //zs[j] = (Real) zsList.get(j);
                        zsetfloats[0][j] =
                            (float) ((Real) zsList.get(j)).getValue();
                    }

                    // print out height values per time step
                    //System.out.println(" zsList = "+zsList.toString());
                    // print out how many dir-spd groups per time step
                    //System.out.println("size of dslist is "+dsList.size());

                    // make range array of dir-spd-s
                    Data[] ds = new RealTuple[dsList.size()];
                    for (int j = 0; j < dsList.size(); j++) {
                        ds[j] = (RealTuple) dsList.get(j);
                    }

                    // sort the z and related dir-spd groups mmm
                    if (zsList.size() != dsList.size()) {
                        System.out.println("  Size mismatch");
                    }

                    // The data has mutiple and overlapping Zs; for example,
                    // two obs at one time at 8000 m, due to low mode and
                    // high mode scans from the Profiler, both received here.
                    TreeMap sortedlist = new TreeMap();
                    // load in reverse order so lower z values from
                    // low mode scan have preference over high mode values
                    for (int j = dsList.size() - 1; j >= 0; j--) {
                        sortedlist.put(new Float(zsetfloats[0][j]), ds[j]);
                    }

                    // extract the sorted lists of z-s, and dir-spd tuples,
                    // with no duplicates
                    float[][] sortedZ   = new float[1][sortedlist.size()];
                    Data[]    sortedDS  = new RealTuple[sortedlist.size()];
                    Object[]  zobjarray = (sortedlist.keySet()).toArray();
                    for (int j = 0; j < sortedlist.size(); j++) {
                        sortedZ[0][j] = ((Float) zobjarray[j]).floatValue();
                        sortedDS[j] =
                            (Data) sortedlist.get((Float) zobjarray[j]);
                    }

                    // Make the (z->(dir,spd)) FlatField

                    //make the FunctionType(MathType domain, MathType range)
                    onetimeFT = new FunctionType(RealType.Altitude,
                            sortedDS[0].getType());

                    // do cstr FlatField(FunctionType type, Set domain_set)
                    // use Gridded1DSet(MathType type, float[][] samples,
                    //                  int lengthX)

                    //IrregularSet zset = new
                    //    IrregularSet(RealType.Altitude, sortedZ);
                    Gridded1DSet zset = new Gridded1DSet(RealType.Altitude,
                                            sortedZ, sortedZ[0].length);

                    onetimeFF = new FlatField(onetimeFT, zset);
                    //set range data with FlatField.setSamples(Data[] range,
                    // boolean copy)
                    onetimeFF.setSamples(sortedDS, false);
                    //System.out.println("onetimeFF is "+onetimeFF.toString())

                    obFFsList.add(onetimeFF);
                    //System.out.println("finished obFFsList with "+dateTime)
                }
            }  // if final time is at right time interval
            ///


            //System.out.println("  there are "+obFFsList.size()+" obs groups");
            //System.out.println("  there are "+timesList.size()+" times");
            //System.out.println("  times are  = "+timesList.toString());

            // make timeSet the domain of the final FieldImpl;
            // one for each  height-obs group
            double[][] timesetdoubles = new double[1][obFFsList.size()];
            for (int j = 0; j < obFFsList.size(); j++) {
                timesetdoubles[0][j] =
                    ((DateTime) timesList.get(j)).getReal().getValue();
            }
            QuickSort.sort(timesetdoubles[0]);
            Gridded1DDoubleSet timeset =
                new Gridded1DDoubleSet(RealType.Time, timesetdoubles,
                                       obFFsList.size());

            retField = new FieldImpl(
                new FunctionType(
                    ((SetType) timeset.getType()).getDomain(),
                    (((FlatField) obFFsList.get(0)).getType())), timeset);

            // put all the Profiler obs in the FieldImpl using
            // FieldImpl.setSamples(Data[] range, boolean copy)
            Data[] obs = new FlatField[obFFsList.size()];
            for (int j = 0; j < obFFsList.size(); j++) {
                obs[j] = (FlatField) obFFsList.get(j);
            }
            retField.setSamples(obs, false);

        } catch (Exception re) {
            throw new VisADException("got Exception " + re);
        }

        return retField;

    }  // end recastProfilerSingleStationData

    /**
     * Change the data into a field that includes lat/lon info
     *
     *
     * @param input _more_
     * @param obInt _more_
     * @return field with lat/lon info
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */

    protected FieldImpl recastProfilerMultiStationData(FieldImpl input,
            int obInt)
            throws VisADException, RemoteException {

        //long millis = System.currentTimeMillis(); //mmm

        FieldImpl retField = null;

        // for level plots accept range of 250 meters for data near
        // level or altitude desired (Profiler data is at various levels
        // not standard set of altitudes)
        //float zmin=zlevel-125.0f, zmax=zlevel+125.0f;

        //System.out.println("      recast ProfilerObs for multi station");

        TupleType    rangetype = null;
        Integer1DSet indexSet  = null;

        try {
            rangetype =
                (TupleType) ((FunctionType) input.getType()).getRange();
            indexSet = (Integer1DSet) input.getDomainSet();
        } catch (ClassCastException ce) {
            throw new IllegalArgumentException(
                "don't know how to convert input to a point ob");
        }

        //System.out.println("      range type = "+rangetype);
        //System.out.println("      index set  = "+indexSet);
        // shows index set  = Integer1DSet: Length = 1702

        // at each station 74 levels -
        // 16000 meters by every 250 meters or so -
        // by 23 hours in this case

        //long mil2 = System.currentTimeMillis();

        // this should be true
        //boolean allReals = (rangetype instanceof RealTupleType);
        // if (allReals) System.out.println("  range is all reals");
        // "latitude","longitude", "levels", "day", "time", "windSpeed", "windDir"

        int     timeIndex  = rangetype.getIndex(rTypes[3].toString());
        boolean hasDayTime = (timeIndex != -1);
        boolean hasDateHMS = false;
        if ( !hasDayTime && !hasDateHMS) {
            throw new IllegalArgumentException(
                "can't find DateTime components");
        }

        // get Z DIR SPD indices    {"LAT", "LON", "Z", "TIME", "SPD", "DIR" };
        int latIndex = rangetype.getIndex(rTypes[0].toString());
        int lonIndex = rangetype.getIndex(rTypes[1].toString());
        int zIndex   = rangetype.getIndex(rTypes[2].toString());
        int dirIndex = rangetype.getIndex(rTypes[4].toString());
        int spdIndex = rangetype.getIndex(rTypes[5].toString());

        if (zIndex == -1) {
            throw new IllegalArgumentException("can't find Z index");
        }

        // check for time groups;
        // there are several obs in a row with the same time
        int      numtimes          = 0;
        int      timecount         = 0;
        DateTime dateTime          = null,
                 cdatetime         = null,
                 thisobsdatetime   = null;
        DateTime thisgroupdatetime = null;
        //Real ctime = new Real(-999.0);  // test time
        //Real thisgrouptime = new Real(-999.0);  // test time

        List         timesList = new ArrayList();
        List         zsList    = new ArrayList();
        List         locList   = new ArrayList();

        List         latList   = new ArrayList();
        List         lonList   = new ArrayList();
        List         dsList    = new ArrayList();
        List         obFFsList = new ArrayList();
        Real         zvalue;
        RealTuple    dirspd = null;
        FunctionType onetimeFT;
        FlatField    onetimeFF;


        // The data from the adde server is a list of obs, each ob have
        //one time height dir spd observation. In these list there are
        //list of sequential observations with the same time. Think of
        //these as groups of observations.  observation groups differ in
        //time by 6 minutes. Users may wish to see data plotted every 6
        //minutes, or every 12 minutes, or every 30 minutes. So only
        //select to use data from the list from every Nth group: every
        //group for 6 minute plots, every 2nd group for 12 minutes plots,
        //every 5th group for 30 minute plots. In the code here the test
        //if whether to use a group is
        //  first increment "timecount" for each time group;
        //  then test if( timecount % obInt ==0) which if true
        //means have a time-ob in a group of times to use.

        // hourly data plots get only hourly data from the adde server
        //and this testing is sidesteped by using obInt = 1 in that case.

        // look at every observation of form (TIME, Z, DIR, SPD, DAY)

        for (int i = 0; i < indexSet.getLength(); i++) {

            // get an ob -- one lat lon day time height dir spd group
            Tuple ob = (Tuple) input.getSample(i);

            // get time of this ob, in funny format as 240000.0 for 24:00 UTC
            Real thisobstime = (Real) ob.getComponent(timeIndex);

            // make DateTime from TIME and DAY real values

            Real ti = (Real) ob.getComponent(timeIndex);
            thisobsdatetime = new DateTime(ti);

            //System.out.println("       this obs time "+thisobstime);
            //System.out.println("       this    ctime "+ctime);

            // see if this is the same time as the previous time encountered;
            // if so, group up the ob data for this one, a single
            //    height-dir-spd obs for this time (smallest unit of data)
            //if (thisobstime.getValue () == ctime.getValue()) {
            if ((cdatetime != null)
                    && (thisobsdatetime.getValue() == cdatetime.getValue())) {  //.equals(cdatetime)) {
                // check altitude of this ob
                zvalue = (Real) ob.getComponent(zIndex);

                // look for level desired in plan view; or
                // use any level if want 3D plot
                //if (use3D ||
                //         (!use3D && zvalue.getValue()>=zmin
                //          && zvalue.getValue()<=zmax) )
                {

                    RealTuple location = new RealTuple(new Real[] {
                                             (Real) ob.getComponent(latIndex),
                                             (Real) ob
                                                 .getComponent(
                                                     lonIndex), (Real) ob
                                                         .getComponent(
                                                             zIndex), });

                    if ( !ob.getComponent(dirIndex).isMissing()
                            && !ob.getComponent(spdIndex).isMissing()) {
                        locList.add(location);
                        zsList.add(zvalue);
                        //System.out.println("       location lat "
                        //          +(Real)ob.getComponent(latIndex) + "  lon "+
                        //            (Real)ob.getComponent(lonIndex) + "  z "+
                        //                    (Real)ob.getComponent(zIndex) );

                        dirspd = new RealTuple(new Real[] {
                            (Real) ob.getComponent(dirIndex),
                            (Real) ob.getComponent(spdIndex) });

                        dsList.add(dirspd);
                    }
                }
            } else {
                // have read a new time.
                //  finalize previous time's group (all z-> dir spd groups)
                //     but only if you are dealing with a "timecount" to save
                if (timecount % obInt == 0) {
                    //  first make sure have processed some data;
                    // (is not very first item in list)

                    if ((dirspd != null) && (dsList.size() > 0)) {
                        // make domain array of Z-s
                        float[][] zsetfloats = new float[1][zsList.size()];
                        for (int j = 0; j < zsList.size(); j++) {
                            //zs[j] = (Real) zsList.get(j);
                            zsetfloats[0][j] =
                                (float) ((Real) zsList.get(j)).getValue();
                        }

                        // make range array of dir-spd-s
                        Data[] ds = new RealTuple[dsList.size()];
                        for (int j = 0; j < dsList.size(); j++) {
                            ds[j] = (RealTuple) dsList.get(j);
                        }
                        //System.out.println("  there are "+ dsList.size()
                        // +" dir-spd obs ");

                        // sort the z and related dir-spd groups
                        if (zsList.size() != dsList.size()) {
                            System.out.println("  SIZE mismatch");
                        }

                        /*
                        // The data has mutiple and overlapping Zs; for example,
                        // two obs at one time at 8000 m, due to low mode and
                        // high mode scans from the Profiler, both received here.
                        TreeMap sortedlist = new TreeMap();
                        // load in reverse order so lower z values from
                        // low mode scan have preference over high mode values
                        for (int j = zsList.size()-1; j>=0; j--) {
                        sortedlist.put(new Float(zsetfloats[0][j]), ds[j]);
                        }

                        // extract the sorted lists of z-s, and dir-spd tuples,
                        // with no duplicates
                        float [][] sortedZ = new float [1][sortedlist.size()];
                        Data[] sortedDS = new RealTuple[sortedlist.size()];
                        Object [] zobjarray = (sortedlist.keySet()).toArray();
                        for (int j = 0; j < sortedlist.size(); j++) {
                        sortedZ[0][j] = ((Float)zobjarray[j]).floatValue() ;
                        sortedDS[j] = (Data)sortedlist.get
                        ((Float)zobjarray[j]);
                        }
                        */

                        // Make the (z->(dir,spd)) FlatField

                        //make the FunctionType(MathType domain, MathType range)
                        onetimeFT = new FunctionType(
                            RealTupleType.LatitudeLongitudeAltitude,
                        //   sortedDS[0].getType());
                        ds[0].getType());

                        // do cstr FlatField(FunctionType type, Set domain_set)

                        int       numPoints = locList.size();
                        float[][] points    = new float[3][numPoints];
                        int       curPoint  = 0;
                        while (curPoint < numPoints) {
                            points[0][curPoint] =
                                (float) ((Real) ((RealTuple) locList.get(
                                    curPoint)).getComponent(0)).getValue();
                            points[1][curPoint] =
                                (float) ((Real) ((RealTuple) locList.get(
                                    curPoint)).getComponent(1)).getValue();
                            points[2][curPoint] =
                                (float) ((Real) ((RealTuple) locList.get(
                                    curPoint)).getComponent(2)).getValue();
                            curPoint++;
                        }

                        Gridded3DSet locset =
                            new Gridded3DSet(
                                RealTupleType.LatitudeLongitudeAltitude,
                                points, numPoints);

                        onetimeFF = new FlatField(onetimeFT, locset);
                        //System.out.println
                        // ("  one-time flatfield function type "+onetimeFT);
                        //set range data with FlatField.setSamples(Data[] range,
                        // boolean copy)
                        onetimeFF.setSamples(ds /*sortedDS*/, false);

                        obFFsList.add(onetimeFF);
                        //System.out.println("  finished obFFsList with "+dateTime);
                    }
                }  // if final time is at right time interval


                // advance time counter, but only once for each distinct new
                // time of a group of like-timed data obs

                if ((obInt != 1)
                        && !(thisobsdatetime.equals(thisgroupdatetime))) {
                    // a new group time was hit; neither the previous time
                    // as checked and used way up above, nor the just found time

                    // advance time counter once per group - count every time group;
                    // may use only some of them
                    timecount++;

                    //System.out.println("   found new group time at i= "+i+
                    //  "   just past grouptime = "+thisgroupdatetime+
                    //  "   this new obs time = "+thisobsdatetime+
                    // "  new timecount= "+ timecount);

                    // reset to new group time
                    thisgroupdatetime = thisobsdatetime;
                }

                // if this group time counter is at the right interval
                if (timecount % obInt == 0) {
                    //System.out.println("      new this obs time "+thisobsdatetime);

                    zsList.clear();
                    locList.clear();
                    dsList.clear();

                    // now advance to first ob for new time value
                    //ctime = thisobstime ;
                    cdatetime = thisobsdatetime;
                    numtimes++;

                    // make DateTime from TIME and DAY real values
                    //day = (int) ((Real) ob.getComponent(dayIndex)).getValue();
                    //time = (int) ((Real) ob.getComponent(timeIndex)).getValue();
                    //dateTime = new DateTime(McIDASUtil.mcDayTimeToSecs(day, time));
                    dateTime = thisobsdatetime;

                    //System.out.println("  i="+i+"  obInt = "+obInt+
                    // "  timecount="+timecount+" tc%obInt = "+timecount%obInt+
                    // "  new datetime is "+dateTime);
                    //System.out.println("  time to use at timecount="+timecount
                    // +"  new datetime is "+dateTime);

                    timesList.add(dateTime);

                    //System.out.println("     observation at "+dateTime);

                    zvalue = (Real) ob.getComponent(zIndex);

                    // look for level desired
                    //if (use3D||
                    //    (!use3D && zvalue.getValue()>=zmin
                    //     && zvalue.getValue()<=zmax) )
                    {


                        RealTuple location =
                            new RealTuple(new Real[] {
                                (Real) ob.getComponent(latIndex),
                                (Real) ob.getComponent(lonIndex),
                                (Real) ob.getComponent(zIndex), });


                        if ( !ob.getComponent(dirIndex).isMissing()
                                && !ob.getComponent(spdIndex).isMissing()) {
                            locList.add(location);
                            dirspd = new RealTuple(new Real[] {
                                (Real) ob.getComponent(dirIndex),
                                (Real) ob.getComponent(spdIndex) });
                            zsList.add(zvalue);
                            dsList.add(dirspd);
                        }
                    }
                }

            }  // else if new time

        }
        // this will add the last time step dirspd to the list
        if (dirspd != null) {
            // make domain array of Z-s
            float[][] zsetfloats = new float[1][zsList.size()];
            for (int j = 0; j < zsList.size(); j++) {
                //zs[j] = (Real) zsList.get(j);
                zsetfloats[0][j] = (float) ((Real) zsList.get(j)).getValue();
            }

            // make range array of dir-spd-s
            Data[] ds = new RealTuple[dsList.size()];
            for (int j = 0; j < dsList.size(); j++) {
                ds[j] = (RealTuple) dsList.get(j);
            }
            //System.out.println("  there are "+ dsList.size()
            // +" dir-spd obs ");

            // sort the z and related dir-spd groups
            if (zsList.size() != dsList.size()) {
                System.out.println("  SIZE mismatch");
            }



            // Make the (z->(dir,spd)) FlatField

            //make the FunctionType(MathType domain, MathType range)
            onetimeFT =
                new FunctionType(RealTupleType.LatitudeLongitudeAltitude,
            //   sortedDS[0].getType());
            ds[0].getType());

            // do cstr FlatField(FunctionType type, Set domain_set)

            int       numPoints = locList.size();
            float[][] points    = new float[3][numPoints];
            int       curPoint  = 0;
            while (curPoint < numPoints) {
                points[0][curPoint] =
                    (float) ((Real) ((RealTuple) locList.get(
                        curPoint)).getComponent(0)).getValue();
                points[1][curPoint] =
                    (float) ((Real) ((RealTuple) locList.get(
                        curPoint)).getComponent(1)).getValue();
                points[2][curPoint] =
                    (float) ((Real) ((RealTuple) locList.get(
                        curPoint)).getComponent(2)).getValue();
                curPoint++;
            }

            Gridded3DSet locset =
                new Gridded3DSet(RealTupleType.LatitudeLongitudeAltitude,
                                 points, numPoints);

            onetimeFF = new FlatField(onetimeFT, locset);

            onetimeFF.setSamples(ds /*sortedDS*/, false);

            obFFsList.add(onetimeFF);
            //System.out.println("  finished obFFsList with "+dateTime);
        }
        //System.out.println("  there are obFFsList "+obFFsList.size()
        //                       +" obs groups");
        //System.out.println("  there are timesList "+timesList.size()+" times");
        //System.out.println("  times are  = "+timesList.toString());

        // make timeSet the domain of the final FieldImpl;
        // one for each  height-obs group
        int        numFields      = obFFsList.size();

        double[][] timesetdoubles = new double[1][numFields];
        for (int j = 0; j < numFields; j++) {
            timesetdoubles[0][j] =
                ((DateTime) timesList.get(j)).getReal().getValue();
        }

        if (numFields == 0) {
            throw new IllegalStateException("No fields were found");
        }

        QuickSort.sort(timesetdoubles[0]);
        Gridded1DDoubleSet timeset = new Gridded1DDoubleSet(RealType.Time,
                                         timesetdoubles, numFields);

        retField = new FieldImpl(
            new FunctionType(
                ((SetType) timeset.getType()).getDomain(),
                (((FlatField) obFFsList.get(0)).getType())), timeset);

        //System.out.println("  final functiontype "+ new FunctionType(
        //                       ((SetType)timeset.getType()).getDomain(),
        //                         (((FlatField)obFFsList.get(0)).getType())) );

        // put all the Profiler obs in the FieldImpl using
        // FieldImpl.setSamples(Data[] range, boolean copy)
        Data[] obs = new FlatField[numFields];
        for (int j = 0; j < numFields; j++) {
            obs[j] = (FlatField) obFFsList.get(j);
        }
        retField.setSamples(obs, false);

        //System.out.println("recast multi station ProfilerObs ");

        return retField;

    }  // end recastProfilerMultiStationData


    /**
     * Check to see if this CDMProfilerDataSource is equal to the object
     * in question.
     *
     * @param o  object in question
     *
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof CDMProfilerDataSource)) {
            return false;
        }
        CDMProfilerDataSource that = (CDMProfilerDataSource) o;
        if ( !super.equals(o)) {
            return false;
        }
        return (Misc.equals(selectedStations, that.selectedStations));
    }

    /**
     * Return the hashcode for this object
     *
     * @return  hashCode
     */
    public int hashCode() {
        return Misc.hashcode(location) ^ super.hashCode();
    }

    /**
     * Test by running "java ucar.unidata.data.profiler.CDMProfilerDataSource <filename>"
     *
     * @param args  filename
     *
     * @throws Exception  problem running this
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Must supply a file name");
            System.exit(1);
        }
        try {
            CDMProfilerDataSource ncar = new CDMProfilerDataSource(null,
                                             args[0], null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the FileNameOrUrl property.
     *
     * @param value The new value for FileNameOrUrl
     */
    public void setFileNameOrUrls(List<String> value) {
        fileNameOrUrls = value;
    }


    /**
     * Get the FileNameOrUrl property.
     *
     * @return The FileNameOrUrl
     */
    public List<String> getFileNameOrUrls() {
        return fileNameOrUrls;
    }



    /**
     * Add the gui components into the list for the properties dialog
     *
     * @param comps List of components
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        locWidget = new LatLonWidget("Lat: ", " Lon: ", " Alt:", null);
        JPanel locPanel = GuiUtils.hbox(new Component[] {
                              GuiUtils.leftCenter(GuiUtils.rLabel("Lat: "),
                                  locWidget.getLatField()),
                              GuiUtils.leftCenter(GuiUtils.rLabel(" Lon: "),
                                  locWidget.getLonField()),
                              GuiUtils.leftCenter(
                                  GuiUtils.rLabel(" Alt: "),
                                  GuiUtils.centerRight(
                                      locWidget.getAltField(),
                                      GuiUtils.cLabel("m"))) });
        if (location != null) {
            locWidget.setLat(location.getLatitude().getValue());
            locWidget.setLon(location.getLongitude().getValue());
            try {
                locWidget.setAlt(
                    location.getAltitude().getValue(CommonUnit.meter));
            } catch (VisADException ve) {
                locWidget.setAlt(location.getAltitude().getValue());
            }
        }
        comps.add(GuiUtils.filler());
        comps.add(getPropertiesHeader("Profiler Location"));
        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(locPanel));
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
        return setLocationFromWidgets();
    }

    /**
     * Set the location from the widgets
     *
     * @return
     */
    private boolean setLocationFromWidgets() {

        try {
            double             lat = locWidget.getLat();
            double             lon = locWidget.getLon();
            double             alt = locWidget.getAlt();
            EarthLocationTuple elt = new EarthLocationTuple(lat, lon, alt);
            if (elt.equals(location)) {
                return true;
            } else {
                locationSetByUser = true;
                location          = elt;
            }
        } catch (Exception e) {
            return false;
        }
        // update the data choices
        List l = getDataChoices();
        for (int i = 0; i < l.size(); i++) {
            DataChoice          dc       = (DataChoice) l.get(i);
            CompositeDataChoice cdc      = (CompositeDataChoice) dc;
            List                children = cdc.getDataChoices();
            for (int j = 0; j < children.size(); j++) {
                DataChoice child = (DataChoice) children.get(j);
                child.setId(location);
                child.setDescription(location.toString());
            }
        }
        reloadData();
        return true;
    }



}

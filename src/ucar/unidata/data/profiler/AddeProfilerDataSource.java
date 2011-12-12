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

import ucar.unidata.data.*;
import ucar.unidata.data.point.AddePointDataSource;
import ucar.unidata.data.point.PointObFactory;

import ucar.unidata.metdata.NamedStation;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;

import visad.*;
import visad.Real;

import visad.data.mcidas.PointDataAdapter;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.lang.Double;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;



import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;



/**
 * A data source for Profiler Network data from an ADDE server.
 * Can handle three types of Profiler data requests - for station
 * time/heihgt plots of wind, plan views of wind at a level above MSL,
 * and 3D plots of multistation wind at all levels.
 *
 * @author MetApps/Unidata
 * @version $Revision: 1.50 $
 */
public class AddeProfilerDataSource extends DataSourceImpl {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            AddeProfilerDataSource.class.getName());

    /** set of all params to request */
    public static final String PARAMS_ALL = "lat lon day time z dir spd";

    /** set of params for a single station */
    public static final String PARAMS_SINGLE = "time z dir spd day";


    // metadata of data request

    /** selected stations */
    private List selectedStations;

    /** data group */
    private String group = "RTPTSRC";

    /** station name */
    private String stationName = " ";

    /** station ID */
    private String stationID = "     ";

    /** selected ID */
    private String selectedId = null;

    /** server name */
    private String server = " ";

    /** more strings */
    private String latitude  = " ",
                   longitude = " ",
                   elevation = " ";

    /** more strings */
    private String starttime = " ",
                   endtime   = " ",
                   interval  = " ";

    /** obs interval */
    private int obInt;

    /** number of relative times */
    private int numRelativeTimes;

    /** data source interval */
    private String dataSourceInterval;

    /** time request */
    private String addeTimeRequest = "0";

    /** initial altitude, meters MSL, to show plan view data */
    private float zlevel = 3000.0f;


    /** Profile name */
    public static final String PROFILER_NAME = "name";

    /** Profiler times */
    public static final String PROFILER_TIMES = "profilertimes";

    //public static final String PROFILER_ENDTIME    = "endtime";

    /** Data interval identifier */
    public static final String PROFILER_INT = "data_interval";

    /** Data source interval identifier */
    public static final String PROFILER_DATAINT = "datasourceinterval";

    /** Profiler stations identifier */
    public static final String PROFILER_STATIONS = "profilerstations";

    /** Profiler server identifier */
    public static final String PROFILER_SERVER = "profilerserver";

    // allowed values of PROFILER_INT (data time interval) property

    /** 6 minute profiler data identifier */
    public static final String PROFILER_6MIN = "6 minute";

    /** 12 minute profiler data identifier */
    public static final String PROFILER_12MIN = "12 minute";

    /** 30 minute profiler data identifier */
    public static final String PROFILER_30MIN = "30 minute";

    /** 1 hour profiler data identifier */
    public static final String PROFILER_1HR = "Hourly";


    /** 6 minute server interval identifier */
    public static final String PROFILER_SERVER_INT_6MIN = "PROF6MIN";

    /** Hourly server interval identifier */
    public static final String PROFILER_SERVER_INT_HR = "PROFHOURLY";

    /**
     * No argument XML persistence constructor
     *
     * @throws VisADException    problem in VisAD
     */
    public AddeProfilerDataSource() throws VisADException {}


    /**
     * Create a new AddeProfilerDataSource
     *
     * @param descriptor             description of source
     * @param stations               stations to get
     * @param properties             extra properties
     *
     * @throws VisADException        problem accessing data
     */
    public AddeProfilerDataSource(DataSourceDescriptor descriptor,
                                  ArrayList stations, Hashtable properties)
            throws VisADException {
        super(descriptor, "Profiler ", "", properties);

        String name = "Profiler "
                      + properties.get(AddeProfilerDataSource.PROFILER_INT)
                      + " - ";
        if (stations.size() < 3) {
            name = name + StringUtil.join(", ", stations);
        } else {
            name = name + stations.size() + " stations";
        }

        setName(name);
        setDescription(name);
        selectedStations = new ArrayList(stations);
        initProfiler();
    }

    /**
     * Extends method in DataSourceImpl to call local initProfiler ()
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        initProfiler();
    }

    /**
     * Read in values from properties table made by ProfilerChooser;
     * determine data interval desired by user - how many 6 minute
     * files to skip in making a new Field Impl. Data REQUESTed is
     * hourly (hour data returned, no skipping used, obInt = 0),
     * or 6 minute data.
     */
    private void initProfiler() {

        interval           = (String) getProperty(PROFILER_INT);
        dataSourceInterval = (String) getProperty(PROFILER_DATAINT);
        server             = (String) getProperty(PROFILER_SERVER);
        addeTimeRequest    = (String) getProperty(PROFILER_TIMES);
        if (addeTimeRequest == null) {
            addeTimeRequest = "";
        }

        // Data interval desired is  hourly,  or  30 minute (use every 
        // 5th time step), or 12 minute (use every other data ob)
        // or 6 minute (use every time step from 6 minute data file).
        obInt = 1;  // use all data obs
        if ( !addeTimeRequest.equals(AddeUtil.RELATIVE_TIME)) {
            if (((String) getProperty(PROFILER_INT)).equals(PROFILER_30MIN)) {
                obInt = 5;
            } else if (((String) getProperty(PROFILER_INT)).equals(
                    PROFILER_12MIN)) {
                obInt = 2;
            }
        }
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
        List compositeDC = DataCategory.parseCategories(
                               DataCategory.CATEGORY_PROFILER_PLAN + ";"
                               + DataCategory.CATEGORY_PROFILER_3D, false);
        compositeDC.addAll(singleDC);

        CompositeDataChoice composite = new CompositeDataChoice(this, "",
                                            "Winds", "Profiler winds",
                                            compositeDC);
        addDataChoice(composite);
        for (int i = 0; i < selectedStations.size(); i++) {
            NamedStation station = (NamedStation) selectedStations.get(i);
            stationName = station.getName();
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
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        //        try {
        Trace.call1("Profiler.getData");
        Data data = getObs(dataChoice, dataSelection);
        Trace.call2("Profiler.getData");
        return data;
        //        } catch (Exception exc) {
        //            logException("Creating the profiler data\nURL:" + url, exc);
        //        }
        //        return null;
    }


    /**
     * Build the URL to get the data
     *
     * @param stations    station list
     * @param params      parameter list
     * @return   ADDE URL
     */
    private String buildUrl(List stations, String params) {
        StringBuffer request = new StringBuffer();

        request.append("adde://");
        request.append(server);
        request.append("/pointdata?");
        request.append("group=");
        request.append(group);
        request.append("&descr=");
        request.append(dataSourceInterval);
        // specify hours of this day (UTC to get data for;
        // if omit, this request gets all times today available
        request.append("&select='");
        if (addeTimeRequest.length() > 0) {
            request.append(addeTimeRequest);
            request.append("; ");
        }
        request.append("ida ");
        for (int i = 0; i < stations.size(); i++) {
            NamedStation station = (NamedStation) stations.get(i);
            if (i > 0) {
                request.append(",");
            }
            request.append(station.getID().substring(0, 4));
        }
        request.append("'");

        request.append("&param=" + params);
        request.append("&num=all");
        request.append(getProperty(AddeUtil.MISC_KEYWORDS, "&compress=gzip"));

        return request.toString();
    }


    /**
     * Get data as requested, from ADDE server, and recast it into a
     * FieldImpl of Profiler data we can use for displays.
     *
     * "source" is of the form, one line only, for one station time-hgt plot:
     * "adde://adde.ucar.edu/pointdata?group=rtptsrc&descr=profhourly&param=
     * time z dir spd day&select='ida "+station+"'&num=all&compress=true";
     *
     *
     * @param dataChoice                choice for the data
     * @param subset                    subsetting criteria
     *
     * @return the FieldImpl needed for to make the display
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private FieldImpl getObs(DataChoice dataChoice, DataSelection subset)
            throws VisADException, RemoteException {

        FieldImpl obs           = null;

        boolean   singleStation = !(dataChoice
                                    instanceof CompositeDataChoice);

        List      stationsToUse = (singleStation
                                   ? Misc.newList(dataChoice.getId())
                                   : new ArrayList(selectedStations));
        String    paramsToUse   = (singleStation
                                   ? PARAMS_SINGLE
                                   : PARAMS_ALL);

        // make request String to send to remote adde server
        // to gets the data
        String           url   = buildUrl(stationsToUse, paramsToUse);
        List             urls  = AddeUtil.generateTimeUrls(this, url);

        FieldImpl        data  = null;
        Vector           datas = new Vector();
        PointDataAdapter pda   = null;
        // we go backwards so the times will be chronological
        for (int i = urls.size() - 1; i >= 0; i--) {

            String newUrl = (String) urls.get(i);
            // System.err.println("url:" + newUrl);

            try {
                Trace.call1("new PointDataAdapter", " url:" + newUrl);
                pda = new PointDataAdapter(newUrl, false);
                Trace.call2("new PointDataAdapter");

                Trace.call1("pda.getData");
                data = (FieldImpl) pda.getData();
                if (data != null) {
                    datas.add(data);
                }
                Trace.call2("pda.getData");
            } catch (VisADException ve) {  // might be no data
                continue;
            }
        }
        data = PointObFactory.mergeData(datas);
        if (data == null) {
            return data;
        }

        Trace.call1("recast");
        //Recast the field into a different FieldImpl we can use.
        if (singleStation) {
            obs = recastProfilerSingleStationData(data, obInt);
        } else {
            obs = recastProfilerMultiStationData(data, obInt);
        }
        Trace.call2("recast");

        //putCache (source, obs);

        return obs;
    }


    /**
     * Take a FieldImpl of National Profiler Network data
     * obtained from the PointDataAdapter.getData()
     * call based on an ADDE source request,
     * and turn it into a FieldImpl of data of different FunctionType.
     * Input expects (index -> (TIME, Z, DIR, SPD, DAY)); all reals.
     * Want to make FieldImpl with function  (DateTime -> ((z)->(dir,spd)) )
     * suitable for display by a Unidata windBarbDisplayable.
     *
     * Skip some ob times depending on whether have 1 hourly or 6 minute
     * data, and whether want 1 hour, 30 min, 12 min, or 6 min intervals
     * in the displayed data.
     *
     * @param input a FieldImpl of NOAA National Profiler Network data
     * obtained from the PointDataAdapter.getData()
     * call based on an ADDE source request.
     * @param obInt an int how many time values to skip to get desired interval
     *
     * @return FieldImpl of Profiler obs with rearranged function.
     *
     * @throws VisADException
     */
    protected static FieldImpl recastProfilerSingleStationData(FieldImpl input,
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

            int     dayIndex   = rangetype.getIndex("DAY");
            int     timeIndex  = rangetype.getIndex("TIME");
            boolean hasDayTime = ((dayIndex != -1) && (timeIndex != -1));
            boolean hasDateHMS = false;
            if ( !hasDayTime && !hasDateHMS) {
                throw new IllegalArgumentException(
                    "can't find DateTime components");
            }

            // get Z DIR SPD indices
            int zIndex   = rangetype.getIndex("Z");
            int dirIndex = rangetype.getIndex("DIR");
            int spdIndex = rangetype.getIndex("SPD");

            if (zIndex == -1) {
                throw new IllegalArgumentException("can't find Z index");
            }

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
                Real thisobstime = (Real) ob.getComponent(0);

                // see if this is the same time as the previous time encountered; 
                // if so, group up the ob data for this one, a single
                //    height-dir-spd obs for this time (smallest unit of data)
                if (thisobstime.getValue() == ctime.getValue()) {
                    // for each ob at this time, make a z->(dir, spd)  thingy
                    zvalue = (Real) ob.getComponent(zIndex);
                    zsList.add(zvalue);

                    dirspd = new RealTuple(new Real[] {
                        (Real) ob.getComponent(dirIndex),
                        (Real) ob.getComponent(spdIndex) });

                    dsList.add(dirspd);
                    //  added a (z -> dir-spd) observation
                } else {
                    // have read a new time.

                    //  finalize previous time's group (all z-> dir spd groups)
                    // but only IF you are dealing with a "timecount" to save
                    if (timecount % obInt == 0) {
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
                        int day = (int) ((Real) ob.getComponent(
                                      dayIndex)).getValue();
                        int time = (int) ((Real) ob.getComponent(
                                       timeIndex)).getValue();
                        dateTime =
                            new DateTime(McIDASUtil.mcDayTimeToSecs(day,
                                time));

                        //System.out.println("  i="+i+"  obInt = "+obInt+
                        // "  timecount="+timecount+" tc%obInt = "+timecount%obInt+
                        // "  new datetime is "+dateTime);
                        //System.out.println("  time to use at timecount="+timecount
                        // +"  new datetime is "+dateTime);

                        timesList.add(dateTime);

                        zvalue = (Real) ob.getComponent(zIndex);
                        zsList.add(zvalue);

                        dirspd = new RealTuple(new Real[] {
                            (Real) ob.getComponent(dirIndex),
                            (Real) ob.getComponent(spdIndex) });

                        dsList.add(dirspd);
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
     * Take a FieldImpl of National Profiler Network data
     * obtained from the PointDataAdapter.getData()
     * call based on an ADDE source request,
     * and turn it into a FieldImpl of data of different FunctionType.
     * Input expects (index -> (lat, lon, day, TIME, Z, DIR, SPD)); all reals.
     * Want to make FieldImpl with function:
     * <pre>
     * (DateTime -> ((lat,lon,z)->(dir,spd)) )
     * </pre>
     * suitable for display by a Unidata windBarbDisplayable.<p>
     *
     * Skip some ob times depending on whether have 1 hourly or 6 minute
     * data, and whether want 1 hour, 30 min, 12 min, or 6 min intervals
     * in the displayed data.<p>
     *
     * level filtering accepts date in altitude range zlevel +/- 125 meters
     *
     * @param input a FieldImpl of NOAA National Profiler Network data
     * obtained from the PointDataAdapter.getData()
     * call based on an ADDE source request.
     * @param obInt an int how many time values to skip to get desired interval
     *
     * @return FieldImpl of Profiler obs with rearranged function.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected static FieldImpl recastProfilerMultiStationData(FieldImpl input,
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

        int     dayIndex   = rangetype.getIndex("DAY");
        int     timeIndex  = rangetype.getIndex("TIME");
        boolean hasDayTime = ((dayIndex != -1) && (timeIndex != -1));
        boolean hasDateHMS = false;
        if ( !hasDayTime && !hasDateHMS) {
            throw new IllegalArgumentException(
                "can't find DateTime components");
        }

        // get Z DIR SPD indices
        int latIndex = rangetype.getIndex(RealType.Latitude);
        int lonIndex = rangetype.getIndex(RealType.Longitude);
        int zIndex   = rangetype.getIndex("Z");
        int dirIndex = rangetype.getIndex("DIR");
        int spdIndex = rangetype.getIndex("SPD");

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
            Real thisobstime = (Real) ob.getComponent(0);

            // make DateTime from TIME and DAY real values
            int day  = (int) ((Real) ob.getComponent(dayIndex)).getValue();
            int time = (int) ((Real) ob.getComponent(timeIndex)).getValue();
            thisobsdatetime = new DateTime(McIDASUtil.mcDayTimeToSecs(day,
                    time));

            //System.out.println("       this obs time "+thisobstime);
            //System.out.println("       this    ctime "+ctime);

            // see if this is the same time as the previous time encountered; 
            // if so, group up the ob data for this one, a single
            //    height-dir-spd obs for this time (smallest unit of data)
            //if (thisobstime.getValue () == ctime.getValue()) {
            if (thisobsdatetime.equals(cdatetime)) {
                // check altitude of this ob
                zvalue = (Real) ob.getComponent(zIndex);

                // look for level desired in plan view; or
                // use any level if want 3D plot
                //if (use3D || 
                //         (!use3D && zvalue.getValue()>=zmin 
                //          && zvalue.getValue()<=zmax) )
                {

                    zsList.add(zvalue);

                    RealTuple location = new RealTuple(new Real[] {
                                             (Real) ob.getComponent(latIndex),
                                             (Real) ob
                                                 .getComponent(
                                                     lonIndex), (Real) ob
                                                         .getComponent(
                                                             zIndex), });
                    locList.add(location);

                    //System.out.println("       location lat "
                    //          +(Real)ob.getComponent(latIndex) + "  lon "+ 
                    //            (Real)ob.getComponent(lonIndex) + "  z "+
                    //                    (Real)ob.getComponent(zIndex) );

                    dirspd = new RealTuple(new Real[] {
                        (Real) ob.getComponent(dirIndex),
                        (Real) ob.getComponent(spdIndex) });

                    dsList.add(dirspd);
                }
            } else {
                // have read a new time.
                //  finalize previous time's group (all z-> dir spd groups)
                //     but only if you are dealing with a "timecount" to save
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
                                -1.0f
                                * (float) ((Real) ((RealTuple) locList.get(
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
                        zsList.add(zvalue);

                        RealTuple location =
                            new RealTuple(new Real[] {
                                (Real) ob.getComponent(latIndex),
                                (Real) ob.getComponent(lonIndex),
                                (Real) ob.getComponent(zIndex), });
                        locList.add(location);

                        dirspd = new RealTuple(new Real[] {
                            (Real) ob.getComponent(dirIndex),
                            (Real) ob.getComponent(spdIndex) });

                        dsList.add(dirspd);
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
                    -1.0f
                    * (float) ((Real) ((RealTuple) locList.get(
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
     *  Set the SelectedStations property.
     *
     *  @param value The new value for SelectedStations
     */
    public void setSelectedStations(List value) {
        selectedStations = value;
    }

    /**
     *  Get the SelectedStations property.
     *
     *  @return The SelectedStations
     */
    public List getSelectedStations() {
        return selectedStations;
    }

    /**
     * Check to see if this AddeProfilerDataSource is equal to the object
     * in question.
     *
     * @param o  object in question
     *
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof AddeProfilerDataSource)) {
            return false;
        }
        AddeProfilerDataSource that = (AddeProfilerDataSource) o;
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
        return Misc.hashcode(selectedStations) ^ super.hashCode();
    }

}

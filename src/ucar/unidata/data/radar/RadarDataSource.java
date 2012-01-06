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
import ucar.unidata.util.*;

import visad.*;

import visad.util.ThreadManager;

import java.rmi.RemoteException;

import java.util.*;


/**
 * A data source for Radar data files.  This class holds a List of
 * data sources and a set of {@link RadarAdapter}-s to adapte each file.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.38 $ $Date: 2007/07/26 19:45:01 $
 */
public abstract class RadarDataSource extends FilesDataSource implements RadarConstants {

    /** _more_          */
    public boolean useDriverTime = false;

    /** _more_          */
    public Object TimeDriver = null;

    /**
     * Radar data appropriate for RHI
     */
    public static final DataCategory CATEGORY_RHI =
        DataCategory.parseCategory("RHI", false);

    /** RHI sweep category */
    public static final DataCategory CATEGORY_RHISWEEP =
        DataCategory.parseCategory("RHISWEEP", false);

    /**
     * Radar data appropriate for CAPPIE
     */
    public static final DataCategory CATEGORY_CAPPI =
        DataCategory.parseCategory("CAPPI", false);


    /**
     * Radar data appropriate for radar volume
     */
    public static final DataCategory CATEGORY_VOLUME =
        DataCategory.parseCategory("RADAR_VOLUME", false);

    /**
     * Radar data appropriate for radar vertical cross section
     */
    public static final DataCategory CATEGORY_VCS =
        DataCategory.parseCategory("RADAR_CROSS_SECTION", false);

    /**
     * Radar data appropriate for radar isosurface
     */
    public static final DataCategory CATEGORY_ISOSURFACE =
        DataCategory.parseCategory("RADAR_ISOSURFACE", false);


    /**
     * Radar data appropriate for radar isosurface
     */
    public static final DataCategory CATEGORY_ISOSURFACE_TIME =
        DataCategory.parseCategory("RADAR_ISOSURFACE-TIME", false);


    /** 2d sweep category */
    public static final DataCategory CATEGORY_SWEEP_3D =
        DataCategory.parseCategory("SWEEP-3D", false);

    /** 3d sweep category */
    public static final DataCategory CATEGORY_SWEEP_3D_TIME =
        DataCategory.parseCategory("SWEEP-3D-TIME", false);

    /** The 2D radar sweep category */
    public static final DataCategory CATEGORY_SWEEP_2D =
        DataCategory.parseCategory("SWEEP-2D", false);


    /** The 2D radar sweep category */
    public static final DataCategory CATEGORY_SWEEP_2D_TIME =
        DataCategory.parseCategory("SWEEP-2D-TIME", false);


    /** The radar time height category */
    public static final DataCategory CATEGORY_TH =
        DataCategory.parseCategory("RADAR_TIMEHEIGHT", false);

    /** The radar vertical wind profiler category */
    public static final DataCategory CATEGORY_VWP =
        DataCategory.parseCategory("RADAR_VWP", false);

    /** A mapping from filename to adapter */
    private Hashtable fileToAdapter = new Hashtable();

    /** The 2D radar sweep category */
    public static final DataCategory CATEGORY_RASTER_2D =
        DataCategory.parseCategory("IMAGE-RASTER", false);


    /** The 2D radar sweep category with time */
    public static final DataCategory CATEGORY_RASTER_2D_TIME =
        DataCategory.parseCategory("IMAGE-RASTER-TIME", false);





    /**
     * Construct a radar data source.
     *
     */
    public RadarDataSource() {}

    /**
     * Construct a new radar data source.
     * @param  descriptor  descriptor for this datasource
     * @param  sources     List of sources of data (filenames, URLs)
     * @param description  Description of the files
     * @param  properties  hashtable of properties.  Includes radar location
     *
     * @throws VisADException  couldn't create the data
     */
    public RadarDataSource(DataSourceDescriptor descriptor, List sources,
                           String description, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, (sources.size() == 1)
                                   ? (String) sources.get(0)
                                   : description, description, properties);
    }


    /**
     * Can this data source save its dat to local disk
     *
     * @return can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased() && (getProperty(PROP_SERVICE_HTTP) != null);
    }


    /**
     * Transmogrify the filename
     *
     * @param filename filename
     * @param index which one
     *
     * @return new file name
     */
    protected String processDataFilename(String filename, int index) {
        Hashtable properties =
            (Hashtable) getProperty(DataSource.PROP_SUBPROPERTIES + index);
        if (properties == null) {
            return filename;
        }
        String httpServer = (String) properties.get(PROP_SERVICE_HTTP);
        if (httpServer != null) {
            return httpServer;
        }
        return filename;
    }



    /**
     * handle legacy bundles
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        Integer mostRecent = null;
        if (getProperties() != null) {
            mostRecent = (Integer) getProperties().get(MOST_RECENT);
            if (mostRecent == null) {
                mostRecent = (Integer) getProperties().get(RADAR_MOST_RECENT);
            }
        }
        if ((mostRecent != null) && (mostRecent.intValue() > 0)) {
            getPollingInfo().setFileCount(mostRecent.intValue());
        }
    }


    /**
     * Make a RadarAdapter from the source
     *
     * @param source  source (file or URL) of data
     *
     * @return corresponding RadarAdapter
     * @throws Exception  problem creating the adapter.
     */
    protected abstract RadarAdapter makeRadarAdapter(String source)
     throws Exception;



    /**
     * This is called when the CacheManager detects the need ot clear memory.
     * It is intended to be overwritten by derived classes that are holding cached
     * data that is not in the normal putCache facilities provided by this class
     * since that data is actually managed by the CacheManager
     */
    public void clearCachedData() {
        super.clearCachedData();
        List<RadarAdapter> adapters = getAdapters();
        for (Iterator iter = adapters.iterator(); iter.hasNext(); ) {
            RadarAdapter adapter = (RadarAdapter) iter.next();
            adapter.clearCachedData();
        }
    }




    /**
     * Create, if needed, and return the list of adapters.
     * Will return null if there are no valid adapters.
     *
     * @return List of adapters or null
     */
    protected List<RadarAdapter> getAdapters() {
        if ((adapters == null) || (adapters.size() == 0)) {
            try {
                makeAdapters(sources);
            } catch (Exception exc) {
                setInError(true);
                throw new WrapperException(exc);
            }
        }
        if (adapters.size() == 0) {
            adapters = null;
        }
        return adapters;
    }


    /**
     * Make the adapters for the given list of files
     *
     * @param files Data files
     *
     * @throws Exception When bad things happen
     */
    private void makeAdapters(List files) throws Exception {
        adapters = new ArrayList<RadarAdapter>();
        Hashtable<String, RadarAdapter> oldAdapterMap = fileToAdapter;
        fileToAdapter = new Hashtable<String, RadarAdapter>();

        int                      cnt           = 0;

        final List<String>       badFiles      = new ArrayList<String>();
        final List<Exception>    badExceptions = new ArrayList<Exception>();

        final List<RadarAdapter> goodAdapters  =
            new ArrayList<RadarAdapter>();
        final List<String>       goodFiles     = new ArrayList<String>();
        visad.util.ThreadManager threadManager =
            new visad.util.ThreadManager("radar data initialization");
        LogUtil.message("Initializing radar files");
        for (Iterator iter = files.iterator(); iter.hasNext(); ) {
            final String filename = iter.next().toString();
            RadarAdapter adapter  =
                (RadarAdapter) oldAdapterMap.get(filename);
            cnt++;
            if (adapter == null) {
                threadManager.addRunnable(
                    new visad.util.ThreadManager.MyRunnable() {
                    public void run() throws Exception {
                        try {
                            RadarAdapter myAdapter =
                                makeRadarAdapter(filename);
                            synchronized (goodAdapters) {
                                goodAdapters.add(myAdapter);
                                goodFiles.add(filename);
                            }
                        } catch (Exception e) {
                            synchronized (badExceptions) {
                                badExceptions.add(e);
                                badFiles.add(filename);
                            }
                        }
                    }
                });
            } else {
                goodAdapters.add(adapter);
                goodFiles.add(filename);
            }

        }

        //        threadManager.debug = true;
        threadManager.runInParallel(
            getDataContext().getIdv().getMaxDataThreadCount());
        LogUtil.message("");

        for (int i = 0; i < goodAdapters.size(); i++) {
            adapters.add(goodAdapters.get(i));
            fileToAdapter.put(goodFiles.get(i), goodAdapters.get(i));
        }


        if ( !badFiles.isEmpty()) {
            StringBuffer buf = new StringBuffer();
            if (badFiles.size() < files.size()) {
                buf.append("<html>");
                buf.append("There were problems reading these files:");
                buf.append("<ul>");
                for (Iterator iterator = badFiles.iterator();
                        iterator.hasNext(); ) {
                    buf.append("<li>");
                    buf.append((String) iterator.next());
                    buf.append("</li>");
                }
                buf.append("</ul>");
                buf.append("<p>Continue loading good data?<p></html>");
                boolean ok =
                    ucar.unidata.util.GuiUtils.askYesNo("Error reading data",
                        buf.toString());

                badExceptions.get(0).printStackTrace();
                if (ok) {
                    files.removeAll(badFiles);
                } else {
                    throw new VisADException("error reading files");
                }
            } else {
                throw badExceptions.get(0);
            }
        }
        // clean up any old adapters
        for (String source : oldAdapterMap.keySet()) {
            if (fileToAdapter.get(source) == null) {
                ((RadarAdapter) oldAdapterMap.get(source)).doRemove();
            }
        }
        oldAdapterMap = null;

    }


    /**
     * Create the list of times associated with this DataSource.
     * @return list of times.
     */
    protected List doMakeDateTimes() {
        List               times      = new ArrayList();
        boolean            isRealTime = isRealTime();
        List<RadarAdapter> adapters   = getAdapters();
        for (int i = 0; i < adapters.size(); i++) {
            if ( !isRealTime) {
                times.add(((RadarAdapter) adapters.get(i)).getBaseTime());
            } else {
                String name = i + "th most recent";
                if (i == 0) {
                    name = "Most recent";
                }
                if ((i > 0) && (i < DataSource.ordinalNames.length)) {
                    name = DataSource.ordinalNames[i] + " most recent";
                }
                times.add(new TwoFacedObject(name, i));
            }
        }
        Collections.sort(times);

        return   times;
    }

    /**
     * Are we doing real time
     *
     * @return is real time
     */
    protected boolean isRealTime() {
        return (getPollingInfo().getFileCount() > 0);
    }


    /**
     * Get the data for the given DataChoice and selection criteria.
     * @param dataChoice         DataChoice for selection
     * @param category           DataCategory for the DataChoice (not used)
     * @param subset             subsetting criteria
     * @param requestProperties  extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException couldn't create a remote data object
     * @throws VisADException  couldn't create the data
     */
    protected Data getDataInner(final DataChoice dataChoice,
                                DataCategory category,
                                final DataSelection subset,
                                final Hashtable requestProperties)
            throws VisADException, RemoteException {

        try {
            List times = null;
            if (subset != null) {
                times = getTimesFromDataSelection(subset, dataChoice);
            }
            if (times == null) {
                times = dataChoice.getSelectedDateTimes();
            }

            List dtimes = subset.getTimeDriverTimes();

            if(dtimes != null && useDriverTime == false) {
                useDriverTime = true;
            }

            List<RadarAdapter> adapters      = getAdapters();

            DateTime[]         realDateTimes = new DateTime[adapters.size()];
            for (int i = 0; i < adapters.size(); i++) {
                realDateTimes[i] =
                    ((RadarAdapter) adapters.get(i)).getBaseTime();
            }
            Arrays.sort(realDateTimes);
            //Flip it to get youngest date first
            boolean isRealTime = isRealTime();
            if (isRealTime) {
                realDateTimes = (DateTime[]) Misc.reverseArray(realDateTimes,
                        new DateTime[realDateTimes.length]);
            }
            // if use time driver
            if (useDriverTime ) {
                List tests = resetTimesList(realDateTimes, times);
                if (!compareTimeLists(times, tests)) {
                    reloadData();
                    adapters      = getAdapters();

                    realDateTimes = new DateTime[adapters.size()];
                    for (int i = 0; i < adapters.size(); i++) {
                        realDateTimes[i] =
                            ((RadarAdapter) adapters.get(i)).getBaseTime();
                    }
                    Arrays.sort(realDateTimes);
                    //Flip it to get youngest date first
                    isRealTime = isRealTime();
                    if (isRealTime) {
                        realDateTimes =
                            (DateTime[]) Misc.reverseArray(realDateTimes,
                                new DateTime[realDateTimes.length]);
                    }
                    tests = resetTimesList(realDateTimes, times);
                }
                times = tests;
            }
            // if times are null, then that means all times
            DateTime[] dateTimes = null;

            if ((times == null) || (times.size() == 0)) {
                dateTimes = realDateTimes;
            } else {
                dateTimes = new DateTime[times.size()];
                for (int i = 0; i < times.size(); i++) {
                    Object time = times.get(i);
                    if (time instanceof TwoFacedObject) {
                        int index =
                            ((Integer) ((TwoFacedObject) time).getId())
                                .intValue();
                        dateTimes[i] = realDateTimes[index];
                    } else if (time instanceof DateTime) {
                        dateTimes[i] = (DateTime) time;
                    }
                }
            }
            Arrays.sort(dateTimes);
            final Data[]     datas     = new Data[dateTimes.length];
            int              timeIndex = 0;
            final MathType[] mt        = { null };
            // create a new field of (Time -> (radar data)).
            // fill in the times array and data array with dates/data
            // only from those adapters which match the selected times.
            // if a data object is null, stick it in the list.
            // if all are null, then the MathType (mt) will never get set,
            // so return null.
            //            System.err.println ("Reading " + adapters.size() + " radar files");
            int cnt = 0;
            ThreadManager threadManager =
                new visad.util.ThreadManager("radar data reading");

            for (Iterator iter = adapters.iterator(); iter.hasNext(); ) {
                final RadarAdapter adapter = (RadarAdapter) iter.next();
                timeIndex = Arrays.binarySearch(dateTimes,
                        adapter.getBaseTime());
                //              System.err.println ("timeIndex:" + timeIndex);
                if (timeIndex < 0) {
                    continue;
                }
                cnt++;
                LogUtil.message("Time: " + (cnt) + "/" + dateTimes.length
                                + " From:" + toString());
                final int theTimeIndex = timeIndex;
                threadManager.addRunnable(
                    new visad.util.ThreadManager.MyRunnable() {
                    public void run() throws Exception {
                        Trace.call1("RDS.getData");
                        Data d = adapter.getData(dataChoice, subset,
                                     requestProperties);
                        Trace.call2("RDS.getData");
                        datas[theTimeIndex] = d;
                        if (d != null) {
                            mt[0] = d.getType();
                        } else {}
                    }
                });
            }

            try {
                //threadManager.debug = true;
                threadManager.runInParallel(
                    getDataContext().getIdv().getMaxDataThreadCount());
            } catch (VisADException ve) {
                LogUtil.printMessage(ve.toString());
            }


            if (mt[0] == null) {
                return null;
            }


            FunctionType ft        = new FunctionType(RealType.Time, mt[0]);
            SampledSet   domainSet = (dateTimes.length == 1)
                                     ? (SampledSet) new SingletonSet(
                                         new RealTuple(dateTimes))
                                     : (SampledSet) DateTime.makeTimeSet(
                                         dateTimes);
            FieldImpl fi = new FieldImpl(ft, domainSet);
            fi.setSamples(datas, false);
            return fi;
        } catch (Exception exc) {
            logException("Creating obs", exc);
        }
        return null;

    }


    private boolean compareTimeLists(List<DateTime> timesA, List<DateTime> timesB){
        if(timesA.size() != timesB.size())
            return false;

        Collections.sort(timesA);
        Collections.sort(timesB);
        for(int i = 0; i< timesA.size(); i++){
            DateTime a = timesA.get(i);
            DateTime b = timesB.get(i);

            double test = Math.abs(a.getValue() -b.getValue()) ;
            if(test > 65.0)
                return false;
            //System.out.println("TTTTTTT =  " + test );
        }
        return true;
    }

    /**
     * using the time from the adapter to reset the time list
     *
     * @param realDateTimes _more_
     * @param times _more_
     *
     * @return _more_
     */
    private List resetTimesList(DateTime[] realDateTimes,
                                List<DateTime> times) {
        List    results   = new ArrayList();
        int     len       = realDateTimes.length;

        HashSet seenTimes = new HashSet();

        try {
            for (DateTime dateTime : times) {
                Date dttm        = ucar.visad.Util.makeDate(dateTime);
                long minTimeDiff = -1;
                Date minDate     = null;
                for (int i = 0; i < len; i++) {
                    Date sourceDate =
                        ucar.visad.Util.makeDate(realDateTimes[i]);
                    long timeDiff = Math.abs(sourceDate.getTime()
                                             - dttm.getTime());
                    if ((minTimeDiff < 0) || (timeDiff < minTimeDiff)) {
                        minTimeDiff = timeDiff;
                        minDate     = sourceDate;
                    }
                }
                if ((minDate != null) && !seenTimes.contains(minDate)) {
                    results.add(new DateTime(minDate));
                    seenTimes.add(minDate);
                }
            }
        } catch (Exception e) {}

        return results;
    }


    /**
     * Get the list of adapters.
     * @return list of adapters.
     */
    protected List<RadarAdapter> getRadarAdapters() {
        return getAdapters();
    }

    /**
     * Gets called by the {@link DataManager} when this DataSource has
     * been removed.
     */
    public void doRemove() {
        if (getAdapters() != null) {
            for (RadarAdapter ra : getAdapters()) {
                ra.doRemove();
            }
        }
        super.doRemove();
    }


}

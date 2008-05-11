/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.storm;


import org.apache.commons.net.ftp.*;


import ucar.nc2.Attribute;

import ucar.unidata.data.*;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import java.util.Hashtable;
import java.util.List;

import java.util.zip.*;
import java.util.zip.GZIPInputStream;




/**
 */
public class AtcfStormDataSource extends StormDataSource {

    /** _more_ */
    private static final String WAY_BEST = "BEST";

    /** _more_ */
    private static final String WAY_CARQ = "CARQ";

    /** _more_ */
    private static final String WAY_WRNG = "WRNG";



    /** _more_ */
    private static String DEFAULT_PATH =
        "ftp://anonymous:password@ftp.tpc.ncep.noaa.gov/atcf/archive";

    /** _more_ */
    private String path;

    /** _more_ */
    private List<StormInfo> stormInfos;


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public AtcfStormDataSource() throws Exception {}

    /**
     * _more_
     *
     * @param descriptor _more_
     * @param url _more_
     * @param properties _more_
     */
    public AtcfStormDataSource(DataSourceDescriptor descriptor, String url,
                               Hashtable properties) {
        super(descriptor, "ATCF Storm Data", "ATCF Storm Data", properties);
        if ((url == null) || (url.trim().length() == 0)
                || url.trim().equalsIgnoreCase("default")) {
            url = DEFAULT_PATH;
        }
        path = url;
    }



    /**
     * _more_
     */
    protected void initAfter() {
        try {
            stormInfos = new ArrayList<StormInfo>();
            byte[]           bytes      = readFile(path + "/storm.table");
            String           stormTable = new String(bytes);
            List lines = StringUtil.split(stormTable, "\n", true, true);

            SimpleDateFormat fmt        = new SimpleDateFormat("yyyyMMddHH");
            for (int i = 0; i < lines.size(); i++) {
                String line   = (String) lines.get(i);
                List   toks   = StringUtil.split(line, ",", true);
                String name   = (String) toks.get(0);
                String basin  = (String) toks.get(1);
                String number = (String) toks.get(7);
                String year   = (String) toks.get(8);
                int    y      = new Integer(year).intValue();
                String id     = basin + "_" + number + "_" + year;
                if (name.equals("UNNAMED")) {
                    name = id;
                }
                String dttm = (String) toks.get(11);
                Date   date = fmt.parse(dttm);
                StormInfo si = new StormInfo(id, name, basin, number,
                                             new DateTime(date));
                stormInfos.add(si);

            }
        } catch (Exception exc) {
            logException("Error initializing ATCF data", exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormInfo> getStormInfos() {
        return stormInfos;
    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param tracks _more_
     * @param trackFile _more_
     * @param waysToUse _more_
     *
     * @throws Exception _more_
     */
    private void readTracks(StormInfo stormInfo, StormTrackCollection tracks,
                            String trackFile,
                            Hashtable<String, Boolean> waysToUse)
            throws Exception {

        long   t1    = System.currentTimeMillis();
        byte[] bytes = readFile(trackFile);
        long   t2    = System.currentTimeMillis();
        System.err.println("read time:" + (t2 - t1));
        if (bytes == null) {
            return;
        }

        if (trackFile.endsWith(".gz")) {
            GZIPInputStream zin =
                new GZIPInputStream(new ByteArrayInputStream(bytes));
            bytes = IOUtil.readBytes(zin);
            zin.close();
        }
        GregorianCalendar convertCal =
            new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        convertCal.clear();


        String           trackData = new String(bytes);
        List             lines = StringUtil.split(trackData, "\n", true,
                                     true);
        SimpleDateFormat fmt       = new SimpleDateFormat("yyyyMMddHH");
        Hashtable        trackMap  = new Hashtable();
        Real             altReal   = new Real(RealType.Altitude, 0);
        System.err.println("obs:" + lines.size());
        /*        Hashtable okWays = new Hashtable();
        okWays.put(WAY_CARQ, "");
        okWays.put(WAY_WRNG, "");
        okWays.put(WAY_BEST, "");
        okWays.put("ETA", "");
        okWays.put("NGX", "");
        okWays.put("BAMS", "");*/
        Hashtable seenDate = new Hashtable();
        initTypes();
        int xcnt = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.get(i);
            if(i==0) System.err.println(line);
            List   toks = StringUtil.split(line, ",", true);

            //BASIN,CY,YYYYMMDDHH,TECHNUM,TECH,TAU,LatN/S,LonE/W,VMAX,MSLP,TY,RAD,WINDCODE,RAD1,RAD2,RAD3,RAD4,RADP,RRP,MRD,GUSTS,EYE,SUBREGION,MAXSEAS,INITIALS,DIR,SPEED,STORMNAME,DEPTH,SEAS,SEASCODE,SEAS1,SEAS2,SEAS3,SEAS4
            //AL, 01, 2007050612,   , BEST,   0, 355N,  740W,  35, 1012, EX,  34, NEQ,    0,    0,    0,  120, 
            //AL, 01, 2007050812, 01, CARQ, -24, 316N,  723W,  55,    0, DB,  34, AAA,    0,    0,    0,    0, 

            int    category   = getCategory((String) toks.get(10));
            Real windspeed = new Real(TYPE_MAXWINDSPEED_KTS, new Double((String) toks.get(8)).doubleValue());
            double pressure  = new Double((String) toks.get(9)).doubleValue();

            if(category!=CATEGORY_XX) System.err.println("cat:"  + category);
            String dateString = (String) toks.get(2);
            String wayString  = (String) toks.get(4);
            //            if (okWays.get(wayString) == null) {
            //                continue;
            //            }
            boolean isBest    = wayString.equals(WAY_BEST);
            boolean isWarning = wayString.equals(WAY_WRNG);
            boolean isCarq    = wayString.equals(WAY_CARQ);

            int forecastHour  = new Integer((String) toks.get(5)).intValue();
            if (isWarning || isCarq) {
                forecastHour = -forecastHour;
            }

            //Check for unique dates for this way
            String dttmkey = wayString + "_" + dateString + "_"
                             + forecastHour;
            if (seenDate.get(dttmkey) != null) {
                continue;
            }
            seenDate.put(dttmkey, dttmkey);

            Date dttm = fmt.parse(dateString);
            convertCal.setTime(dttm);
            String key;
            Way    way = new Way(wayString);
            if ( !isBest && (waysToUse != null) && (waysToUse.size() > 0)
                    && (waysToUse.get(wayString) == null)) {
                continue;
            }

            if (isBest) {
                key = wayString;
            } else {
                key = wayString + "_" + dateString;
                convertCal.add(Calendar.HOUR_OF_DAY, forecastHour);
            }
            StormTrack track = (StormTrack) trackMap.get(key);
            if (track == null) {
                way   = (isBest
                         ? Way.OBSERVATION
                         : way);
                track = new StormTrack(stormInfo, addWay(way), new DateTime(dttm));
                trackMap.put(key, track);
                tracks.addTrack(track);
            }
            String latString = (String) toks.get(6);
            String lonString = (String) toks.get(7);
            latString = latString.substring(0, 2) + "."
                        + latString.substring(2);
            lonString = lonString.substring(0, 2) + "."
                        + lonString.substring(2);
            double latitude  = Misc.decodeLatLon(latString);
            double longitude = Misc.decodeLatLon(lonString);
            EarthLocation elt =
                new EarthLocationLite(new Real(RealType.Latitude, latitude),
                                      new Real(RealType.Longitude,
                                          longitude), altReal);

            List<Real> attributes = new ArrayList<Real>();
            attributes.add(new Real(TYPE_STORMCATEGORY, (double) category));
            attributes.add(new Real(TYPE_MINPRESSURE, pressure));
            attributes.add(windspeed);


            StormTrackPoint stp = new StormTrackPoint(elt,
                                      new DateTime(dttm), forecastHour,
                                      attributes);

            track.addPoint(stp);
        }

    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param waysToUse _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StormTrackCollection getTrackCollection(StormInfo stormInfo,
            Hashtable<String, Boolean> waysToUse)
            throws Exception {
        long                 t1     = System.currentTimeMillis();
        StormTrackCollection tracks = new StormTrackCollection();

        String trackFile = path + "/" + getYear(stormInfo.getStartTime())
                           + "/" + "a" + stormInfo.getBasin().toLowerCase()
                           + stormInfo.getNumber()
                           + getYear(stormInfo.getStartTime()) + ".dat.gz";
        readTracks(stormInfo, tracks, trackFile, waysToUse);
        trackFile = path + "/" + getYear(stormInfo.getStartTime()) + "/"
                    + "b" + stormInfo.getBasin().toLowerCase()
                    + stormInfo.getNumber()
                    + getYear(stormInfo.getStartTime()) + ".dat.gz";
        readTracks(stormInfo, tracks, trackFile, null);
        long t2 = System.currentTimeMillis();
        System.err.println("time: " + (t2 - t1));

        return tracks;
    }




    /**
     * Set the Directory property.
     *
     * @param value The new value for Directory
     */
    public void setPath(String value) {
        path = value;
    }

    /**
     * Get the Directory property.
     *
     * @return The Directory
     */
    public String getPath() {
        return path;
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private byte[] readFile(String file) throws Exception {
        if (new File(file).exists()) {
            return IOUtil.readBytes(IOUtil.getInputStream(file, getClass()));
        }
        if ( !file.startsWith("ftp:")) {
            throw new FileNotFoundException("Could not read file: " + file);
        }

        URL url = new URL(file);
        //Try to read up to 5 times
        Exception lastException = null;
        for (int i = 0; i < 5; i++) {
            FTPClient ftp = new FTPClient();
            try {
                ftp.connect(url.getHost());
                ftp.login("anonymous", "password");
                ftp.setFileType(FTP.IMAGE_FILE_TYPE);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ftp.enterLocalPassiveMode();
                if (ftp.retrieveFile(url.getPath(), bos)) {
                    return bos.toByteArray();
                }
            } catch (Exception exc) {
                lastException = exc;
            }
            //Wait a bit
            Misc.sleep(100);
        }
        if (lastException != null) {
            throw lastException;
        }
        //            throw new FileNotFoundException("Could not read file: " + file);
        return null;
    }


}


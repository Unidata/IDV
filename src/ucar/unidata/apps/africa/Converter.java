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

package ucar.unidata.apps.africa;


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlEncoder;


import ucar.visad.GeoUtils;

import visad.*;
import visad.CommonUnit;

import visad.georef.LatLonPoint;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;


import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 * @author IDV Development Team
 * @version $Revision: 1.90 $ $Date: 2007/08/06 17:02:27 $
 */
public class Converter {




    /** _more_          */
    static Hashtable<String, double[]> locationMap;


    /** _more_          */
    static SimpleDateFormat sdf2;

    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String format(Date date) throws Exception {
        if (sdf2 == null) {
            sdf2 = new SimpleDateFormat("yyyy-MM-dd");
            sdf2.setTimeZone(DateUtil.TIMEZONE_GMT);
        }
        return sdf2.format(date);
    }



    /** _more_          */
    public static final String HEADER1 =
        "(index)->(Latitude,Longitude,Country(Text),State(Text),Region(Text),Time,rate,Population)\nLatitude[ unit=\"degrees\" ],Longitude[ unit=\"degrees\" ],Country(Text),State(Text),Region(Text),Population[],Time[ fmt=\"yyyy-MM-dd\" ],rate[ missing=\"-9999.9\" ]";

    /**
     * _more_
     *
     * @param lines _more_
     * @param country _more_
     * @param year _more_
     *
     * @throws Exception _more_
     */
    public static void process1(List<String> lines, String country,
                                String year)
            throws Exception {
        System.out.println(HEADER1);
        SimpleDateFormat sdf1 = new SimpleDateFormat("MMM dd yyyy");

        sdf1.setTimeZone(DateUtil.TIMEZONE_GMT);
        List<String> dates     = new ArrayList<String>();
        int          colOffset = 3;
        for (int i = 0; i < lines.size(); i++) {
            if (i == 1) {
                continue;
            }
            String line = lines.get(i).trim();
            if (line.length() == 0) {
                continue;
            }

            List<String> cols = StringUtil.split(line, ",", false, false);

            if (i == 0) {
                //,Latitude,Longitude,Location,Population,Dec 29-Jan 4,Jan 5-Jan 11,Jan 12-Jan 18,Jan 19-Jan 25,Jan 26-Feb 1,Feb 2-Feb 8,Feb 9-Feb 15,Feb 16-Feb 22,Feb 23-Mar 1,Mar 2-Mar 8,Mar 9-Mar 15,Mar 16-Mar 22,Mar 23-Mar 29,Mar 30-Apr 5,Apr 6-Apr 12,Apr 13-Apr 19,Apr 20-Apr 26
                for (int col = colOffset; col < cols.size(); col++) {
                    String dttms = cols.get(col);
                    List<String> toks = StringUtil.split(dttms, "-", true,
                                            true);
                    //Get the second date
                    Date date = sdf1.parse(toks.get(1) + " " + year);
                    dates.add(format(date));
                }
                continue;
            }
            //Kano,11.70,9.10,Albasu,209606.00,,,,,,0.48,2.86,0.48,,,3.82,6.20,9.54,20.99,2.86,,
            int    col        = 0;
            String state      = cols.get(col++);
            String region     = cols.get(col++);
            double population = new Double(cols.get(col++)).doubleValue();
            //      System.err.println ("line:"+i);
            LatLonPoint llp = GeoUtils.getLocationFromAddress(region + ","
                                  + state + "," + country, null);
            System.err.println(region + " " + llp);
            if (llp == null) {
                System.err.println("No location:" + region);
            }
            System.out.println(
                "Latitude=" + llp.getLatitude().getValue(CommonUnit.degree));
            System.out.println(
                "Longitude="
                + llp.getLongitude().getValue(CommonUnit.degree));
            System.out.println("Country=" + country);
            System.out.println("State=" + state);
            System.out.println("Region=" + region);
            System.out.println("Population=" + population);
            for (int dateIdx = colOffset; dateIdx < cols.size(); dateIdx++) {
                System.out.print(dates.get(dateIdx - colOffset));
                System.out.print(",");
                String value = cols.get(dateIdx);
                if (value.trim().length() == 0) {
                    value = "-9999.9";
                }
                System.out.println(new Double(value).doubleValue());
            }
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    static double parse(String s) {
        s = s.trim();
        if (s.equals("NA")) {
            return -9999.9;
        }
        if (s.length() == 0) {
            return -9999.9;
        }
        return Double.parseDouble(s);
    }

    /** _more_          */
    public static final String HEADER2 =
        "(index)->(Latitude,Longitude,Country(Text),State(Text),Region(Text),Time,population,cases,deaths,inc,cfr,alert)\nLatitude[ unit=\"degrees\" ],Longitude[ unit=\"degrees\" ],Country(Text),State(Text),Region(Text),Time[ fmt=\"yyyy-MM-dd\" ],population[ missing=\"-9999.9\" ],cases[ missing=\"-9999.9\" ],deaths[ missing=\"-9999.9\" ],inc[ missing=\"-9999.9\" ],cfr[ missing=\"-9999.9\" ],alert[ missing=\"-9999.9\" ]";

    //GEOID,ANNEE,VAL_CAS,VAL_DECES,POPSIZE,INC,CFR,ALERT,GEONAMLEV1,GEONAMLEV2,GEONAMLEV3,PAYS,YEAR,SEMAINE
    //BFH001002000000000000,200922,2,0,396220,0.505,0,0,Burkina Faso,Centre-Ouest,Koudougou,BF,2009,22
    //BFH001002000000000000,200828,0,0,826882,0,0,0,Burkina Faso,Centre-Ouest,Koudougou,BF,2008,28

    /**
     * _more_
     *
     * @param lines _more_
     *
     * @throws Exception _more_
     */
    public static void process2(List<String> lines) throws Exception {

        System.out.println(HEADER2);
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy w");
        sdf1.setTimeZone(DateUtil.TIMEZONE_GMT);
        List<String> dates       = new ArrayList<String>();
        int          colOffset   = 3;
        String       lastLoc     = null;
        String       line        = "";
        HashSet      seenMissing = new HashSet();
        try {
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    continue;
                }
                line = lines.get(i).trim();
                if (line.length() == 0) {
                    continue;
                }
                List<String> cols = StringUtil.split(line, ",", false, false);

                if ((i % 1000) == 0) {
                    System.err.println("#" + i);
                }
                int    col        = 2;
                double val_cas    = parse(cols.get(col++));
                double val_deces  = parse(cols.get(col++));
                double population = parse(cols.get(col++));
                double inc        = parse(cols.get(col++));
                double cfr        = parse(cols.get(col++));
                double alert      = parse(cols.get(col++));

                String country    = cols.get(col++);
                String state      = cols.get(col++);
                String region     = cols.get(col++);
                String cc         = cols.get(col++);



                String[] locs = { region + "," + country, region + "," + cc,
                                  region + "," + state + "," + cc,
                                  state + "," + cc };

                if (region.equals("NA")) {
                    continue;
                }


                String year = cols.get(col++);
                String week = cols.get(col++);
                Date   date = sdf1.parse(year + " " + week);


                if ( !Misc.equals(lastLoc, locs[0])) {
                    if (seenMissing.contains(locs[0])) {
                        continue;
                    }
                    double[] ll = null;
                    for (String loc : locs) {
                        ll = locationMap.get(loc);
                        if (ll != null) {
                            break;
                        }
                    }

                    if (ll == null) {
                        LatLonPoint llp = null;
                        for (String loc : locs) {
                            llp = GeoUtils.getLocationFromAddress(loc, null);
                            if (llp != null) {
                                ll = new double[] {
                                    llp.getLatitude().getValue(
                                        CommonUnit.degree),
                                    llp.getLongitude().getValue(
                                        CommonUnit.degree) };

                                if ((ll[0] > 25) || (ll[1] < -20)
                                        || (ll[1] > 50)) {
                                    System.err.println("bad location: "
                                            + locs[0] + " " + ll[0] + "/"
                                            + ll[1]);
                                    //                                seenMissing.add(locs[0]);
                                    ll = null;
                                    continue;
                                }
                            }
                            if (ll != null) {
                                break;
                            }
                        }

                        if (ll == null) {
                            seenMissing.add(locs[0]);
                            System.err.println("No location:" + locs[0]
                                    + " or  " + locs[1]);
                            continue;
                        }
                        locationMap.put(locs[0], ll);
                        writeLocs();
                        lastLoc = locs[0];
                    }
                    System.out.println("Latitude=" + ll[0]);
                    System.out.println("Longitude=" + ll[1]);
                    System.out.println("Country=" + country);
                    System.out.println("State=" + state);
                    System.out.println("Region=" + region);

                }
                //GEOID,ANNEE,VAL_CAS,VAL_DECES,POPSIZE,INC,CFR,ALERT,GEONAMLEV1,GEONAMLEV2,GEONAMLEV3,PAYS,YEAR,SEMAINE
                System.out.print(format(date));
                System.out.print(",");
                System.out.print(population);
                System.out.print(",");
                System.out.print(val_cas);
                System.out.print(",");
                System.out.print(val_deces);

                System.out.print(",");
                System.out.print(inc);
                System.out.print(",");
                System.out.print(cfr);
                System.out.print(",");
                System.out.print(alert);
                System.out.print("\n");
            }
        } catch (Exception exc) {
            System.err.println("error reading line:" + line);
            throw exc;
        }

    }


    /** _more_          */
    public static final String HEADER3 =
       "(index)->(Latitude,Longitude,Time,Country(Text),State(Text),Region(Text),population,cases,deaths,inc,cfr,alert)\nLatitude[ unit=\"degrees\" ],Longitude[ unit=\"degrees\" ],Time[ fmt=\"yyyy-MM-dd\" ],Country(Text),State(Text),Region(Text),population[ missing=\"-9999.9\" ],cases[ missing=\"-9999.9\" ],deaths[ missing=\"-9999.9\" ],inc[ missing=\"-9999.9\" ],cfr[ missing=\"-9999.9\" ],alert[ missing=\"-9999.9\" ]";



    /**
     * This parses the meningitis rates data that has the centroid location
     *
     * @param lines _more_
     *
     * @throws Exception _more_
     */
    public static void process3(List<String> lines) throws Exception {

        System.out.println(HEADER3);
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyw");
        sdf1.setTimeZone(DateUtil.TIMEZONE_GMT);
        List<String> dates       = new ArrayList<String>();
        String       lastLoc     = null;
        String       line        = "";
        HashSet      seenMissing = new HashSet();
        try {
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
                    continue;
                }
                line = lines.get(i).trim();
                if (line.length() == 0) {
                    continue;
                }
                List<String> cols = StringUtil.split(line, ",", false, false);

                if ((i % 1000) == 0) {
                    System.err.println("#" + i);
                }
                //Skip over the oid and geoid
                //"OID_","GEOID","ANNEE","VAL_CAS","VAL_DECES","POPSIZE","INC","CFR","ALERT","GEONAMLEV1","GEONAMLEV2","GEONAMLEV3","PAYS","YEAR_","SEMAINE","centroidX","centroidY","area_KM","GEOJoin","lvlid"
                //,"BJP001001000000000000",200832.000000,0.000000,0.000000,172516.000000,0.000000,0.000000,0.000000,"Benin","Donga","BASSILA","BJ",2008.000000,32.000000,1.857341,8.965200,5734.035689," ","BJP001001000000000000"





                int    col        = 2;
                String yearAndWeek = cols.get(col++);
                //Strip off the suffix
                yearAndWeek = yearAndWeek.substring(0,yearAndWeek.indexOf("."));
                Date   date = sdf1.parse(yearAndWeek);
                double val_cas    = parse(cols.get(col++));
                double val_deces  = parse(cols.get(col++));
                double population = parse(cols.get(col++));
                double inc        = parse(cols.get(col++));
                double cfr        = parse(cols.get(col++));
                double alert      = parse(cols.get(col++));

                String geonamelev1    = cols.get(col++);
                String geonamelev2    = cols.get(col++);
                String geonamelev3    = cols.get(col++);
                String pays= cols.get(col++);
                //Skip YEAR_ and SEMAINE
                col++;
                col++;

                double lon = Double.parseDouble(cols.get(col++));
                double lat = Double.parseDouble(cols.get(col++));
                double area = Double.parseDouble(cols.get(col++));

                String locString = lat+"__" + lon;
                if ( !Misc.equals(lastLoc, locString)) {
                    System.out.println("Latitude=" + lat);
                    System.out.println("Longitude=" + lon);
                    System.out.println("Country=" + geonamelev1);
                    System.out.println("State=" + geonamelev2);
                    System.out.println("Region=" + geonamelev3);
                    lastLoc = locString;
                }

                System.out.print(format(date));
                System.out.print(",");
                System.out.print(population);
                System.out.print(",");
                System.out.print(val_cas);
                System.out.print(",");
                System.out.print(val_deces);

                System.out.print(",");
                System.out.print(inc);
                System.out.print(",");
                System.out.print(cfr);
                System.out.print(",");
                System.out.print(alert);
                System.out.print("\n");
            }
        } catch (Exception exc) {
            System.err.println("error reading line:" + line);
            throw exc;
        }

    }






    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public static void writeLocs() throws Exception {
        StringBuffer sb = new StringBuffer();
        for (Enumeration keys =
                locationMap.keys(); keys.hasMoreElements(); ) {
            String   loc = (String) keys.nextElement();
            double[] ll  = locationMap.get(loc);
            sb.append(ll[0] + "," + ll[1] + "," + loc + "\n");
        }
        IOUtil.writeFile("locations.csv", sb.toString());
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {

        //        if(new File("locations.xml").exists()) {
        //XmlEncoder xmlEncoder  =  new XmlEncoder();
        //            locs =  (Hashtable<String,double[]>) xmlEncoder.toObject(IOUtil.readContents("locations.xml", Converter.class));
        //        }


        locationMap = new Hashtable<String, double[]>();
        if (new File("locations.csv").exists()) {
            String contents = IOUtil.readContents("locations.csv",
                                  Converter.class);
            for (String line : StringUtil.split(contents, "\n", false,
                    false)) {
                if (line.length() == 0) {
                    continue;
                }
                List<String> toks = StringUtil.splitUpTo(line, ",", 3);
                double[] ll = new double[] { parse(toks.get(0)),
                                             parse(toks.get(1)) };
                //Sanity check
                if ((ll[0] > 25) || (ll[1] < -20) || (ll[1] > 50)) {
                    System.out.println(toks.get(2) + " " + ll[0] + "/"
                                       + ll[1]);
                    continue;
                }
                locationMap.put(toks.get(2), ll);

            }

        }

        for (String arg : args) {
            String contents = IOUtil.readContents(arg, Converter.class);
            List<String> lines = StringUtil.split(contents, "\n", false,
                                     false);
            if (lines.size() == 1) {
                lines = StringUtil.split(contents, "\r", false, false);
            }
            //process1(lines,"nigeria","2009");
            //process2(lines);
            process3(lines);
        }
    }


}

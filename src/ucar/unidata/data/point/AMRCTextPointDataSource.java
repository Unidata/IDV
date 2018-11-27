/*
 *
 * Copyright  1997-2019 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.point;

import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import visad.DateTime;
import visad.VisADException;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yuanho on 10/22/14.
 */
public class AMRCTextPointDataSource extends TextPointDataSource {

    /** _more_ */
    public static String amrc1 =
            "(index -> (StationId(Text),StationName(Text),ARGOS(Text),Time,Latitude,Longitude,Altitude,"
                    + "Temperature,Pressure,SPD,DIR,RH,DeltaT))\n"
                    + "StationId(Text),StationName(Text),ARGOS(Text),"
                    + "Longitude[unit=\"degree\"],"
                    + "Latitude[unit=\"degree\"],"
                    + "Time[fmt=\"yyyy-MM-dd HH:mm:ss z\"],"
                    + "Altitude[unit=\"m\" miss=\"444.0\"],"
                    + "Temperature[unit=\"celsius\" miss=\"444.0\"],"
                    + "Pressure[unit=\"mb\" miss=\"444.0\"],"
                    + "SPD[unit=\"m/s\" miss=\"444.0\"],"
                    + "DIR[unit=\"deg\" miss=\"444.0\"],"
                    + "RH[unit=\"%\" miss=\"444.0\"],"
                    + "DeltaT[unit=\"celsius\" miss=\"444.0\"]\n"
            ;

    public static String amrc2 =
            "(index -> (StationId(Text),Time,Latitude,Longitude,Altitude,"
                    + "Temperature,Pressure,SPD,DIR,RH,DeltaT))\n"
                    + "StationId(Text),"
                    + "Longitude[unit=\"degree\"],"
                    + "Latitude[unit=\"degree\"],"
                    + "Time[fmt=\"yyyy-MM-dd HH:mm:ss z\"],"
                    + "Altitude[unit=\"m\" miss=\"444.0\"],"
                    + "Temperature[unit=\"celsius\" miss=\"444.0\"],"
                    + "Pressure[unit=\"mb\" miss=\"444.0\"],"
                    + "SPD[unit=\"m/s\" miss=\"444.0\"],"
                    + "DIR[unit=\"deg\" miss=\"444.0\"],"
                    + "RH[unit=\"%\" miss=\"444.0\"],"
                    + "DeltaT[unit=\"celsius\" miss=\"444.0\"]\n"
            ;

    public static String amrc3 =
            "(index -> (StationId(Text),Time,Latitude,Longitude,Altitude,"
                    + "Temperature,Pressure,SPD,DIR,RH,PotentialTemperature))\n"
                    + "StationId(Text),"
                    + "Longitude[unit=\"degrees\"],"
                    + "Latitude[unit=\"degree\"],"
                    + "Time[fmt=\"yyyy-MM-dd HH:mm:ss z\"],"
                    + "Altitude[unit=\"m\" miss=\"444.0\"],"
                    + "Temperature[unit=\"celsius\" miss=\"444.0\"],"
                    + "Pressure[unit=\"mb\" miss=\"444.0\"],"
                    + "SPD[unit=\"m/s\" miss=\"444.0\"],"
                    + "DIR[unit=\"deg\" miss=\"444.0\"],"
                    + "RH[unit=\"%\" miss=\"444.0\"],"
                    + "PotentialTemperature[unit=\"celsius\" miss=\"444.0\"]\n";


    /**
     * Create a new AMRC PointDataSource
     *
     * @param descriptor    data source descriptor
     * @param sources       List of sources of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws visad.VisADException   problem creating the data
     *
     * @throws visad.VisADException _more_
     *
     */
    public AMRCTextPointDataSource(DataSourceDescriptor descriptor,
                                  List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, properties);


    }

    /**
     * Construct a DiamondPointDataSource
     *
     * @throws VisADException _more_
     */
    public AMRCTextPointDataSource() throws VisADException {
        super();


    }

    /**
     * Create a new Diamond PointDataSource
     *
     * @param descriptor    data source descriptor
     * @param source        source of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public AMRCTextPointDataSource(DataSourceDescriptor descriptor,
                                  String source, Hashtable properties)
            throws VisADException {
        super(descriptor, source, properties);

    }


    /**
     * Read the given source file and return the text contents of it.
     * If the source file is a xls file then convert to csv text
     *
     * @param sourceFile The source file (or url)
     * @param sampleIt _more_
     *
     * @return The contents
     *
     * @throws Exception On badness
     */
    protected String getContents(String sourceFile, boolean sampleIt)
            throws Exception {
        String       s            = IOUtil.readContents(sourceFile);

        List<String> lines        = StringUtil.split(s, "\n", true, true);

        if(sourceFile.contains(".q10") || sourceFile.contains(".q1h") || sourceFile.contains(".q3h"))
            return readAMRC1File(lines);
        else if(sourceFile.endsWith(".dat"))
            return readAMRC2File(lines);
        else
            return readAMRC3File(lines);

    }

    /**
     * read AMRC  .q10, .q1h, .q3h format point data
     *
     * @param lines _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String readAMRC1File(List<String> lines) throws Exception {
        StringBuffer output = new StringBuffer();

        output.append(amrc1);

        int currentIndex = 0;

        // Year: 2001  Month: 09  ID: BPT  ARGOS:  8923  Name: Bonaparte Point
        String       headerLine0 = lines.get(currentIndex++);
        List<String> toks0 = StringUtil.split(headerLine0, " ", true, true);
        int pos = toks0.lastIndexOf("ID:");
        String id      = toks0.get(pos+1);
        pos = toks0.indexOf("ARGOS:");
        String argo = toks0.get(pos+1);
        pos = toks0.indexOf("Name:");
        String name = toks0.get(pos+1);

        String tid = "StationId=" + id;
        String tna = "StationName=" + name;
        String tao = "ARGOS=" + argo;

        output.append(tid);
        output.append("\n");
        output.append(tna);
        output.append("\n");
        output.append(tao);
        output.append("\n");

        //Lat: 64.78S  Lon:  64.07W  Elev:    8m
        String       headerLine1 = lines.get(currentIndex++);
        List<String> toks1 = StringUtil.split(headerLine1, " ", true, true);

        pos = toks1.indexOf("Lat:");
        String latStr = toks1.get(pos+1).trim();
        float latitude;
        if(latStr.endsWith("S")){
            latitude = (-1.0f)*Float.parseFloat(latStr.substring(0, latStr.length()-1));
        } else if(latStr.endsWith("N")){
            latitude = (1.0f)*Float.parseFloat(latStr.substring(0, latStr.length()-1));
        } else
            latitude = (1.0f)*Float.parseFloat(latStr);
        pos = toks1.indexOf("Lon:");
        String lonStr = toks1.get(pos+1).trim();
        float longitude;
        if(lonStr.endsWith("W")){
            longitude = (-1.0f)*Float.parseFloat(lonStr.substring(0, lonStr.length()-1));
        } else if(lonStr.endsWith("E")){
            longitude = (1.0f)*Float.parseFloat(lonStr.substring(0, lonStr.length()-1));
        } else
            longitude = (1.0f)*Float.parseFloat(lonStr);

        pos = toks1.indexOf("Elev:");
        String elevStr = toks1.get(pos+1).trim();
        float altitude;
        if(elevStr.endsWith("m")){
            altitude = Float.parseFloat(elevStr.substring(0, elevStr.length()-1));
        } else
            altitude = Float.parseFloat(elevStr);

        String lat = "Latitude=" + latitude;
        String lon = "Longitude=" + longitude;
        String ele = "Altitude=" + altitude;
        output.append(lat);
        output.append("\n");
        output.append(lon);
        output.append("\n");
        output.append(ele);
        output.append("\n");
/*  Column 1 - year
    Column 2 - julian day
    Column 3 - month
    Column 4 - day
    Column 5 - ten minute observation time
    Column 6 - temperature (C)
    Column 7 - pressure (mb)
    Column 8 - wind speed (m/s)
    Column 9 - wind direction
    Column 10- relative humidity (%)
    Column 11- delta-T (C) */
        while (currentIndex < lines.size()) {
            //2001 244  9  1 0000   -2.5  444.0    0.2  110.0  444.0  444.0
            // Time,Temperature,Pressure,SPD,DIR,RH,DeltaT
            StringBuffer outputline = new StringBuffer();
            String       headerLine2 = lines.get(currentIndex++);
            List<String> toks = StringUtil.split(headerLine2, " ", true,
                    true);
            String year      = toks.get(0).trim();
            String jday      = toks.get(1).trim();
            String mon       = toks.get(2).trim();
            String day       = toks.get(3).trim();
            String minutes   = toks.get(4).trim();
            String temp      = toks.get(5).trim();
            String pressure  = toks.get(6).trim();
            String wspd      = toks.get(7).trim();
            String wdir      = toks.get(8).trim();
            String rh        = toks.get(9).trim();
            String dt        = toks.get(10).trim();

            Date date = new SimpleDateFormat("yyyyD").parse(year+jday);

            long time0 = date.getTime() + Integer.parseInt(minutes) * 60 * 1000;
            date.setTime(time0);
            DateTime dtt =  new DateTime(date);

            String ts = dtt.formattedString("yyyy-MM-dd HH:mm:ss z",
                    DateUtil.TIMEZONE_GMT);
            outputline.append(ts);
            outputline.append(",");
            outputline.append(temp);
            outputline.append(",");
            outputline.append(pressure);
            outputline.append(",");
            outputline.append(wspd);
            outputline.append(",");
            outputline.append(wdir);
            outputline.append(",");
            outputline.append(rh);
            outputline.append(",");
            outputline.append(dt);
            outputline.append(",");

            output.append(outputline);
            output.append("\n");
        }

        return output.toString();

    }

    /**
     * read AMRC  .dat format point data
     *
     * @param lines _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String readAMRC2File(List<String> lines) throws Exception {
        StringBuffer output = new StringBuffer();

        output.append(amrc2);

        int currentIndex = 0;

        // May  14   Station :30305  JASE2007
        String       headerLine0 = lines.get(currentIndex++);
        List<String> toks0 = StringUtil.split(headerLine0, " ", true, true);
        String monStr = toks0.get(0);
        String yyStr = toks0.get(1);
        int pos = toks0.lastIndexOf("Station");
        String id0      = toks0.get(pos+1);
        String id = id0.substring(1, id0.length());

        String tid = "StationId=" + id;


        output.append(tid);
        output.append("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yy");
        Date date = sdf.parse(monStr + " " + yyStr);

        //Lat : 75.89S  Long :  25.84E  Elev : 3661 M
        String       headerLine1 = lines.get(currentIndex++);
        List<String> toks1 = StringUtil.split(headerLine1, " ", true, true);

        pos = toks1.indexOf("Lat");
        String latStr = toks1.get(pos+2).trim();
        float latitude;
        if(latStr.endsWith("S")){
            latitude = (-1.0f)*Float.parseFloat(latStr.substring(0, latStr.length()-1));
        } else if(latStr.endsWith("N")){
            latitude = (1.0f)*Float.parseFloat(latStr.substring(0, latStr.length()-1));
        } else
            latitude = (1.0f)*Float.parseFloat(latStr);
        pos = toks1.indexOf("Long");
        String lonStr = toks1.get(pos+2).trim();
        float longitude;
        if(lonStr.endsWith("W")){
            longitude = (-1.0f)*Float.parseFloat(lonStr.substring(0, lonStr.length()-1));
        } else if(lonStr.endsWith("E")){
            longitude = (1.0f)*Float.parseFloat(lonStr.substring(0, lonStr.length()-1));
        } else
            longitude = (1.0f)*Float.parseFloat(lonStr);

        pos = toks1.indexOf("Elev");
        String elevStr = toks1.get(pos+2).trim();
        float altitude;
        if(elevStr.endsWith("m")){
            altitude = Float.parseFloat(elevStr.substring(0, elevStr.length()-1));
        } else
            altitude = Float.parseFloat(elevStr);

        String lat = "Latitude=" + latitude;
        String lon = "Longitude=" + longitude;
        String ele = "Altitude=" + altitude;
        output.append(lat);
        output.append("\n");
        output.append(lon);
        output.append("\n");
        output.append(ele);
        output.append("\n");
/*  Column 1 - temperature (C)
Column 2 - pressure (mb)
Column 3 - wind speed (m/s)
Column 4 - wind direction
Column 5 - relative humidity (%)
Column 6 - delta-T (C) */
        while (currentIndex < lines.size()) {
            //2001 244  9  1 0000   -2.5  444.0    0.2  110.0  444.0  444.0
            // Time,Temperature,Pressure,SPD,DIR,RH,DeltaT
            StringBuffer outputline = new StringBuffer();
            String       headerLine2 = lines.get(currentIndex++);
            List<String> toks = StringUtil.split(headerLine2, " ", true,
                    true);

            String temp      = toks.get(0).trim();
            String pressure  = toks.get(1).trim();
            String wspd      = toks.get(2).trim();
            String wdir      = toks.get(3).trim();
            String rh        = toks.get(4).trim();
            String dt        = toks.get(5).trim();

            long time0 = date.getTime() + (currentIndex -3) * 3 * 3600 * 1000;
            Date date1 = new Date(time0);
            DateTime dtt =  new DateTime(date1);

            String ts = dtt.formattedString("yyyy-MM-dd HH:mm:ss z",
                    DateUtil.TIMEZONE_GMT);
            outputline.append(ts);
            outputline.append(",");
            outputline.append(temp);
            outputline.append(",");
            outputline.append(pressure);
            outputline.append(",");
            outputline.append(wspd);
            outputline.append(",");
            outputline.append(wdir);
            outputline.append(",");
            outputline.append(rh);
            outputline.append(",");
            outputline.append(dt);
            outputline.append(",");

            output.append(outputline);
            output.append("\n");
        }

        return output.toString();

    }

    /**
     * read AMRC  .dat format point data
     *
     * @param lines _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String readAMRC3File(List<String> lines) throws Exception {
        StringBuffer output = new StringBuffer();

        output.append(amrc3);

        int currentIndex = 0;

        // May  14   Station : 8695  Vito
        String       headerLine0 = lines.get(currentIndex++);
        List<String> toks0 = StringUtil.split(headerLine0, " ", true, true);
        String monStr = toks0.get(0);
        String yyStr = toks0.get(1).trim();
        int pos = toks0.lastIndexOf("Station");
        String id     = toks0.get(pos+2);

        String tid = "StationId=" + id;


        output.append(tid);
        output.append("\n");

        //Lat : 75.89S  Long :  25.84E  Elev : 3661 M
        String       headerLine1 = lines.get(currentIndex++);
        List<String> toks1 = StringUtil.split(headerLine1, " ", true, true);

        pos = toks1.indexOf("Lat");
        String latStr = toks1.get(pos+2).trim();
        float latitude;
        if(latStr.endsWith("S")){
            latitude = (-1.0f)*Float.parseFloat(latStr.substring(0, latStr.length()-1));
        } else if(latStr.endsWith("N")){
            latitude = (1.0f)*Float.parseFloat(latStr.substring(0, latStr.length()-1));
        } else
            latitude = (1.0f)*Float.parseFloat(latStr);
        pos = toks1.indexOf("Long");
        String lonStr = toks1.get(pos+2).trim();
        float longitude;
        if(lonStr.endsWith("W")){
            longitude = (-1.0f)*Float.parseFloat(lonStr.substring(0, lonStr.length()-1));
        } else if(lonStr.endsWith("E")){
            longitude = (1.0f)*Float.parseFloat(lonStr.substring(0, lonStr.length()-1));
        } else
            longitude = (1.0f)*Float.parseFloat(lonStr);

        pos = toks1.indexOf("Elev");
        String elevStr = toks1.get(pos+2).trim();
        float altitude;
        if(elevStr.endsWith("m")){
            altitude = Float.parseFloat(elevStr.substring(0, elevStr.length()-1));
        } else
            altitude = Float.parseFloat(elevStr);

        String lat = "Latitude=" + latitude;
        String lon = "Longitude=" + longitude;
        String ele = "Altitude=" + altitude;
        output.append(lat);
        output.append("\n");
        output.append(lon);
        output.append("\n");
        output.append(ele);
        output.append("\n");
/*  Column 1 - julian day
Column 2 - ten minute interval marker
Column 3 - temperature (C)
Column 4 - pressure (mb)
Column 5 - wind speed (m/s)
Column 6 - wind direction
Column 7 - relative humidity (%)
Column 8 - potential temperature (K)
 */
        while (currentIndex < lines.size()) {
            //2001 244  9  1 0000   -2.5  444.0    0.2  110.0  444.0  444.0
            // Time,Temperature,Pressure,SPD,DIR,RH,DeltaT
            StringBuffer outputline = new StringBuffer();
            String       headerLine2 = lines.get(currentIndex++);
            List<String> toks = StringUtil.split(headerLine2, " ", true,
                    true);

            String jday      = toks.get(0).trim();
            String ii        = toks.get(1).trim();
            String temp      = toks.get(2).trim();
            String pressure  = toks.get(3).trim();
            String wspd      = toks.get(4).trim();
            String wdir      = toks.get(5).trim();
            String rh        = toks.get(6).trim();
            String dt        = toks.get(7).trim();

            Date date = new SimpleDateFormat("yyD").parse(yyStr+jday);
            long time0 = date.getTime() + (Integer.parseInt(ii) -1)* 10 * 60 * 1000;
            Date date1 = new Date(time0);
            DateTime dtt =  new DateTime(date1);

            String ts = dtt.formattedString("yyyy-MM-dd HH:mm:ss z",
                    DateUtil.TIMEZONE_GMT);
            outputline.append(ts);
            outputline.append(",");
            outputline.append(temp);
            outputline.append(",");
            outputline.append(pressure);
            outputline.append(",");
            outputline.append(wspd);
            outputline.append(",");
            outputline.append(wdir);
            outputline.append(",");
            outputline.append(rh);
            outputline.append(",");
            outputline.append(dt);
            outputline.append(",");

            output.append(outputline);
            output.append("\n");
        }

        return output.toString();

    }

    /**
     *  format date time
     *
     *  @param year _more_
     *  @param month _more_
     *  @param day _more_
     *  @param hour _more_
     *
     *  @return _more_
     *
     *  @throws Exception _more_
     */
    private DateTime getDateTime(int year, int month, int day, int hour)
            throws Exception {
        GregorianCalendar convertCal =
                new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        convertCal.clear();
        convertCal.set(Calendar.YEAR, year);
        //The MONTH is 0 based. The incoming month is 1 based
        convertCal.set(Calendar.MONTH, month - 1);
        convertCal.set(Calendar.DAY_OF_MONTH, day);
        convertCal.set(Calendar.HOUR_OF_DAY, hour);
        return new DateTime(convertCal.getTime());
    }
}

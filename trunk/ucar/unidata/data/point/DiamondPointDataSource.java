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

package ucar.unidata.data.point;


import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import visad.DateTime;
import visad.VisADException;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Sep 3, 2009
 * Time: 1:36:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class DiamondPointDataSource extends TextPointDataSource {

    /** _more_ */
    public static String diamond1 =
        "(index -> (StationId(Text),Longitude,Latitude,Time,Altitude,CF,"
        + "CloudTotal,DIR,SPD,PressureSFC,Pressure3Hr,Weather1, Weather2,Preci6Hr,LowCloudShape,"
        + "LowCloud,LowCloudHigh,TD,Visibility,Weather,T,MidCouldShape,"
        + "HighCloudShape,ShipDir,ShipSPD))\n" + "StationId(Text),"
        + "Longitude[unit=\"degrees west\"]," + "Latitude[unit=\"deg\"],"
        + "Time[fmt=\"yyyy-MM-dd HH:mm:ss z\"],"
        + "Altitude[unit=\"m\" miss=\"9999\"]," + "CF[miss=\"9999\"],"
        + "CloudTotal[miss=\"9999\"]," + "DIR[unit=\"deg\" miss=\"9999\"],"
        + "SPD[unit=\"m/s\" miss=\"9999\"],"
        + "PressureSFC[unit=\"hPa\" miss=\"9999\"],"
        + "Pressure3Hr[unit=\"hPa\" miss=\"9999\"],"
        + "Weather1[miss=\"9999\"]," + "Weather2[miss=\"9999\"],"
        + "Preci6Hr[unit=\"mm\" miss=\"9999\"],"
        + "LowCloudShape[miss=\"9999\"]," + "LowCloud[miss=\"9999\"],"
        + "LowCloudHigh[miss=\"9999\"],"
        + "TD[unit=\"celsius\" miss=\"9999\"],"
        + "Visibility[miss=\"9999\"]," + "Weather[miss=\"9999\"],"
        + "T[unit=\"celsius\" miss=\"9999\"],"
        + "MidCouldShape[miss=\"9999\"]," + "HighCloudShape[miss=\"9999\"],"
        + "ShipDir[miss=\"9999\"]," + "ShipSPD[miss=\"9999\"]\n"
    ;

    /* StationId(Text),Longitude,Latitude,Time,Altitude,CF,High,T,TD,DIR,SPD*/

    /** _more_ */
    public static String diamond2 =
        "(index -> (StationId(Text),Longitude,Latitude,Time,Altitude,"
        + "CF,High,T,TD,DIR,SPD))\n" + "StationId(Text),"
        + "Longitude[unit=\"degrees west\"]," + "Latitude[unit=\"deg\"],"
        + "Time[fmt=\"yyyy-MM-dd HH:mm:ss z\"],"
        + "Altitude[unit=\"m\" miss=\"9999\"]," + "CF[miss=\"9999\"],"
        + "High[miss=\"9999\"], " + "T[unit=\"celsius\" miss=\"9999\"],"
        + "TD[unit=\"celsius\" miss=\"9999\"],"
        + "DIR[unit=\"deg\" miss=\"9999\"],"
        + "SPD[unit=\"m/s\" miss=\"9999\"]\n";


    /**
     * Create a new Diamond PointDataSource
     *
     * @param descriptor    data source descriptor
     * @param sources       List of sources of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws visad.VisADException   problem creating the data
     *
     * @throws VisADException _more_
     *
     */
    public DiamondPointDataSource(DataSourceDescriptor descriptor,
                                  List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, properties);


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
    public DiamondPointDataSource(DataSourceDescriptor descriptor,
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
        int          currentIndex = 0;
        String       headerLine1  = lines.get(currentIndex++);
        if (headerLine1.contains("diamond 1")) {
            return readDiamond1File(lines);
        } else if (headerLine1.contains("diamond 2")) {
            return readDiamond2File(lines);
        } else {
            return null;
        }
    }

    /**
     * read CMA diamond1 format point data
     *
     * @param lines _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String readDiamond1File(List<String> lines) throws Exception {
        StringBuffer output = new StringBuffer();

        output.append(diamond1);

        int currentIndex = 1;


        while (currentIndex < lines.size()) {

            String       headerLine2 = lines.get(currentIndex++);
            List<String> toks = StringUtil.split(headerLine2, " ", true,
                                    true);
            String year      = toks.get(0);
            String mon       = toks.get(1);
            String day       = toks.get(2);
            String hr        = toks.get(3);
            int    numberPts = Integer.parseInt(toks.get(4));
            int    yy        = Integer.parseInt(year);
            if (yy < 20) {
                yy = 2000 + yy;
            } else if ((yy > 50) && (yy < 99)) {
                yy = 1900 + yy;
            }
            DateTime dtt = getDateTime(yy, Integer.parseInt(mon),
                                       Integer.parseInt(day),
                                       Integer.parseInt(hr));
            String ts = "Time="
                        + dtt.formattedString("yyyy-MM-dd HH:mm:ss z",
                            DateUtil.TIMEZONE_GMT);
            output.append(ts);
            output.append("\n");

            /* StationId(Text),Longitude,Latitude,Time,Altitude,CF,High,T,TD,DIR,SPD*/
            int endPtsIndex = currentIndex + numberPts * 2;

            //System.out.println("endPtsIndex "+ endPtsIndex);
            while (currentIndex < endPtsIndex) {
                StringBuffer outputline = new StringBuffer();
                //System.out.println("currentIndex "+ currentIndex);
                String       line1 = lines.get(currentIndex++);
                String       line2 = lines.get(currentIndex++);
                List<String> toks1 = StringUtil.split(line1, " ", true, true);
                List<String> toks2 = StringUtil.split(line2, " ", true, true);

                for (String a : toks1) {
                    outputline.append(a);
                    outputline.append(",");
                }
                for (String a : toks2) {
                    outputline.append(a);
                    outputline.append(",");
                }
                //outputline.replace(outputline.length()-1, outputline.length()-1, "\n");
                output.append(outputline);
                output.append("\n");
            }


        }
        return output.toString();

    }

    /**
     * read CMA dismond2 format point data
     *
     * @param lines _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String readDiamond2File(List<String> lines) throws Exception {


        StringBuffer output = new StringBuffer();

        output.append(diamond2);

        int currentIndex = 1;
        //  String headerLine1 = lines.get(currentIndex++);


        while (currentIndex < lines.size()) {

            String       headerLine2 = lines.get(currentIndex++);
            List<String> toks = StringUtil.split(headerLine2, " ", true,
                                    true);
            String year      = toks.get(0);
            String mon       = toks.get(1);
            String day       = toks.get(2);
            String hr        = toks.get(3);
            String levels    = toks.get(4);
            int    numberPts = Integer.parseInt(toks.get(5));
            int    yy        = Integer.parseInt(year);
            if (yy < 20) {
                yy = 2000 + yy;
            } else if ((yy > 50) && (yy < 99)) {
                yy = 1900 + yy;
            }
            DateTime dtt = getDateTime(yy, Integer.parseInt(mon),
                                       Integer.parseInt(day),
                                       Integer.parseInt(hr));
            String ts = "Time="
                        + dtt.formattedString("yyyy-MM-dd HH:mm:ss z",
                            DateUtil.TIMEZONE_GMT);
            output.append(ts);
            output.append("\n");

            /* StationId(Text),Longitude,Latitude,Time,Altitude,CF,High,T,TD,DIR,SPD*/
            int endPtsIndex = currentIndex + numberPts;

            //System.out.println("endPtsIndex "+ endPtsIndex);
            while (currentIndex < endPtsIndex) {
                StringBuffer outputline = new StringBuffer();
                //System.out.println("currentIndex "+ currentIndex);
                String line = lines.get(currentIndex++);
                toks = StringUtil.split(line, " ", true, true);
                for (String a : toks) {
                    outputline.append(a);
                    outputline.append(",");
                }


                output.append(outputline);
                output.append("\n");
            }


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

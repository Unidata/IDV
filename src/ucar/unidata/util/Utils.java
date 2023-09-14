/*
 * Copyright 1997-2023 Unidata Program Center/University Corporation for
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

package ucar.unidata.util;



import org.w3c.dom.*;

import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;

import java.awt.Toolkit;
import java.awt.image.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.Constructor;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A collection of utilities
 *
 * @author Jeff McWhirter
 */

public class Utils {

    /**
     * Returns the Julian day number that begins at noon of
     * this day, Positive year signifies A.D., negative year B.C.
     * Remember that the year after 1 B.C. was 1 A.D.
     *
     * ref :
     *  Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
     */
    // Gregorian Calendar adopted Oct. 15, 1582 (2299161)
    public static int JGREG = 15 + 31 * (10 + 12 * 1582);
    /** _more_ */
    public static double HALFSECOND = 0.5;

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     *
     * @return _more_
     */
    public static Appendable append(Appendable sb, String s) {
        try {
            sb.append(s);

            return sb;
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     *  The julian date functions below are from
     *  http://www.rgagnon.com/javadetails/java-0506.html
     */

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static boolean stringDefined(String s) {
        if ((s == null) || (s.trim().length() == 0)) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @param modifiedJulian _more_
     *
     * @return _more_
     */
    public static double modifiedJulianToJulian(double modifiedJulian) {
        // MJD = JD - 2400000.5
        return modifiedJulian + 2400000.5;
    }

    /**
     * _more_
     *
     * @param ymd _more_
     *
     * @return _more_
     */
    public static double toJulian(int[] ymd) {
        int year       = ymd[0];
        int month      = ymd[1];  // jan=1, feb=2,...
        int day        = ymd[2];
        int julianYear = year;
        if (year < 0) {
            julianYear++;
        }
        int julianMonth = month;
        if (month > 2) {
            julianMonth++;
        } else {
            julianYear--;
            julianMonth += 13;
        }

        double julian = (java.lang.Math.floor(365.25 * julianYear)
                + java.lang.Math.floor(30.6001 * julianMonth) + day
                + 1720995.0);
        if (day + 31 * (month + 12 * year) >= JGREG) {
            // change over to Gregorian calendar
            int ja = (int) (0.01 * julianYear);
            julian += 2 - ja + (0.25 * ja);
        }

        return java.lang.Math.floor(julian);
    }

    /**
     * Converts a Julian day to a calendar date
     * ref :
     * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
     *
     * @param injulian _more_
     *
     * @return _more_
     */
    public static int[] fromJulian(double injulian) {
        return fromJulian(injulian, new int[3]);
    }

    /**
     * _more_
     *
     * @param injulian _more_
     * @param src _more_
     *
     * @return _more_
     */
    public static int[] fromJulian(double injulian, int[] src) {
        int    jalpha, ja, jb, jc, jd, je, year, month, day;
        double julian = injulian + HALFSECOND / 86400.0;
        ja = (int) julian;
        if (ja >= JGREG) {
            jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
            ja     = ja + 1 + jalpha - jalpha / 4;
        }

        jb    = ja + 1524;
        jc    = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
        jd    = 365 * jc + jc / 4;
        je    = (int) ((jb - jd) / 30.6001);
        day   = jb - jd - (int) (30.6001 * je);
        month = je - 1;
        if (month > 12) {
            month = month - 12;
        }
        year = jc - 4715;
        if (month > 2) {
            year--;
        }
        if (year <= 0) {
            year--;
        }

        src[0] = year;
        src[1] = month;
        src[2] = day;

        return src;
    }



    /**
     * _more_
     *
     * @param args _more_
     */
    public static void testJulian(String args[]) {
        // FIRST TEST reference point
        System.out.println("Julian date for May 23, 1968 : "
                + toJulian(new int[] { 1968,
                5, 23 }));
        // output : 2440000
        int results[] = fromJulian(toJulian(new int[] { 1968, 5, 23 }));
        System.out.println("... back to calendar : " + results[0] + " "
                + results[1] + " " + results[2]);

        // SECOND TEST today
        Calendar today = Calendar.getInstance();
        double todayJulian = toJulian(new int[] { today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1, today.get(Calendar.DATE) });
        System.out.println("Julian date for today : " + todayJulian);
        results = fromJulian(todayJulian);
        System.out.println("... back to calendar : " + results[0] + " "
                + results[1] + " " + results[2]);

        // THIRD TEST
        double date1 = toJulian(new int[] { 2005, 1, 1 });
        double date2 = toJulian(new int[] { 2005, 1, 31 });
        System.out.println("Between 2005-01-01 and 2005-01-31 : "
                + (date2 - date1) + " days");

        /*
          expected output :
          Julian date for May 23, 1968 : 2440000.0
          ... back to calendar 1968 5 23
          Julian date for today : 2453487.0
          ... back to calendar 2005 4 26
          Between 2005-01-01 and 2005-01-31 : 30.0 days
        */
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String getArticle(String s) {
        s = s.toLowerCase();
        if (s.startsWith("a") || s.startsWith("e") || s.startsWith("i")
                || s.startsWith("o") || s.startsWith("u")) {
            return "an";
        } else {
            return "a";
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static Date extractDate(String s) {
        try {
            String yyyy = "\\d\\d\\d\\d";
            String str = StringUtil.findPattern(s,
                    "(" + yyyy + "-\\d\\d-\\d\\d)");
            if (str != null) {
                //                System.err.println("pattern 1:" + str);
                return DateUtil.parse(str);
            }

            str = StringUtil.findPattern(
                    s, "(" + yyyy + "\\d\\d\\d\\d-\\d\\d\\d\\d\\d\\d)");
            if (str != null) {
                try {
                    //                    System.err.println("pattern 2:" + str);
                    return new SimpleDateFormat("yyyyMMdd-HHmmss").parse(str);
                } catch (Exception ignore) {}
            }

            str = StringUtil.findPattern(s, "(" + yyyy
                    + "\\d\\d\\d\\d-\\d\\d\\d\\d)");
            if (str != null) {
                try {
                    //                   System.err.println("pattern 3:" + str);
                    return new SimpleDateFormat("yyyyMMdd-HHmm").parse(str);
                } catch (Exception ignore) {}
            }


            str = StringUtil.findPattern(s, "[^\\d]*(" + yyyy
                    + "\\d\\d\\d\\d)[^\\d]+");
            if (str != null) {
                try {
                    //                    System.err.println("pattern 4:" + str);
                    return new SimpleDateFormat("yyyyMMdd").parse(str);
                } catch (Exception ignore) {}
            }

            return null;
        } catch (Exception exc) {
            System.err.println("Utils.extractDate:" + exc);

            return null;
        }
    }


    /**
     * _more_
     *
     * @param c _more_
     * @param paramTypes _more_
     *
     * @return _more_
     */
    public static Constructor findConstructor(Class c, Class[] paramTypes) {
        ArrayList     allCtors     = new ArrayList();
        Constructor[] constructors = c.getConstructors();
        if (constructors.length == 0) {
            System.err.println(
                    "*** Could not find any constructors for class:"
                            + c.getName());

            return null;
        }
        for (int i = 0; i < constructors.length; i++) {
            if (typesMatch(constructors[i].getParameterTypes(), paramTypes)) {
                allCtors.add(constructors[i]);
            }
        }
        if (allCtors.size() > 1) {
            throw new IllegalArgumentException(
                    "More than one constructors matched for class:"
                            + c.getName());
        }
        if (allCtors.size() == 1) {
            return (Constructor) allCtors.get(0);
        }


        for (int i = 0; i < constructors.length; i++) {
            Class[] formals = constructors[i].getParameterTypes();
            for (int j = 0; j < formals.length; j++) {
                System.err.println("param " + j + "  " + formals[j].getName()
                        + " " + paramTypes[j].getName());
            }
        }




        return null;
    }

    /**
     * _more_
     *
     * @param formals _more_
     * @param actuals _more_
     *
     * @return _more_
     */
    public static boolean typesMatch(Class[] formals, Class[] actuals) {
        if (formals.length != actuals.length) {
            return false;
        }
        for (int j = 0; j < formals.length; j++) {
            if (actuals[j] == null) {
                continue;
            }
            if ( !formals[j].isAssignableFrom(actuals[j])) {
                return false;
            }
        }

        return true;
    }



    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String args[]) {
        String pattern =
                ".*l(.)(.)(\\d\\d\\d)(\\d\\d\\d)(\\d\\d\\d\\d)(\\d\\d\\d)(...)(\\d\\d)\\.hdf$";



        for (String a : args) {
            System.err.println("file:" + a + " "
                    + a.toLowerCase().matches(pattern));
        }

        if (true) {
            return;
        }


        String name =
                "a79126d2-e879-4354-95d7-78969009cd85_file_lt50480232011289pac01.hdf";
        List<String> patternNames = new ArrayList<String>();
        String patternString =
                ".*l(sensor:.)(satellite:.)(wrs_path_number:\\d\\d\\d)(wrs_row_number:\\d\\d\\d)(year:\\d\\d\\d\\d)(day:\\d\\d\\d)(ground_station:...)(archive_version_number:\\d\\d)\\.hdf$";
        //        String patternString = ".*(inst|tavg|const).*";

        args          = new String[] { name };
        patternString = extractPatternNames(patternString, patternNames);
        System.err.println("pattern:" + patternString);
        System.err.println("pattern names:" + patternNames);

        Pattern filePattern = Pattern.compile(patternString);
        for (String file : args) {
            Matcher matcher = filePattern.matcher(new File(file).getName());
            if ( !matcher.find()) {
                System.err.println("no match:" + file);

                continue;
            }
            System.err.println("match:" + file);

            for (int i = 0; i < patternNames.size(); i++) {
                Object value = matcher.group(i + 1);
                System.err.println("\t" + patternNames.get(i) + "=" + value);
            }

        }
    }



    /**
     * _more_
     *
     * @param s _more_
     * @param regexp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String[] findPatterns(String s, String regexp)
            throws Exception {
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(s);
        if ( !matcher.find()) {
            return null;
        }
        String[] results = new String[matcher.groupCount()];
        for (int i = 0; i < results.length; i++) {
            results[i] = matcher.group(i + 1);
        }

        return results;
    }

    /**
     * _more_
     *
     * @param source _more_
     * @param datePatterns _more_
     * @param dateFormats _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Date findDate(String source, String[] datePatterns,
                                String[] dateFormats)
            throws Exception {
        for (int dateFormatIdx = 0; dateFormatIdx < datePatterns.length;
             dateFormatIdx++) {
            String dttm = StringUtil.findPattern(source,
                    datePatterns[dateFormatIdx]);
            if (dttm != null) {
                dttm = dttm.replaceAll(" _ ", " ");
                dttm = dttm.replaceAll(" / ", "/");

                return makeDateFormat(dateFormats[dateFormatIdx]).parse(dttm);
            }
        }

        return null;
    }

    /**
     * _more_
     *
     * @param format _more_
     *
     * @return _more_
     */
    public static SimpleDateFormat makeDateFormat(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf;
    }


    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    public static int getYear(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(cal.YEAR);
    }


    /**
     * _more_
     *
     * @param date _more_
     *
     * @return _more_
     */
    public static int getMonth(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(cal.MONTH);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String removeNonAscii(String s) {
        s = s.replaceAll("[^\r\n\\x20-\\x7E]+", "_");

        return s;
    }

    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getAttributeOrTag(Element node, String attrOrTag,
                                           String dflt)
            throws Exception {
        String attrValue = XmlUtil.getAttribute(node, attrOrTag,
                (String) null);
        if (attrValue == null) {
            attrValue = XmlUtil.getGrandChildText(node, attrOrTag, dflt);
        }

        return attrValue;

    }


    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static boolean getAttributeOrTag(Element node, String attrOrTag,
                                            boolean dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return attrValue.equals("true");
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static int getAttributeOrTag(Element node, String attrOrTag,
                                        int dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return new Integer(attrValue).intValue();
    }


    /**
     * _more_
     *
     * @param node _more_
     * @param attrOrTag _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static double getAttributeOrTag(Element node, String attrOrTag,
                                           double dflt)
            throws Exception {
        String attrValue = getAttributeOrTag(node, attrOrTag, (String) null);
        if (attrValue == null) {
            return dflt;
        }

        return new Double(attrValue).doubleValue();
    }

    /**
     * _more_
     *
     * @param properties _more_
     *
     * @return _more_
     */
    public static String makeProperties(Hashtable properties) {
        StringBuffer sb      = new StringBuffer();

        List<String> keyList = new ArrayList<String>();
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            keyList.add((String) keys.nextElement());
        }
        keyList = (List<String>) Misc.sort(keyList);
        for (String key : keyList) {
            String value = (String) properties.get(key);
            sb.append(key);
            sb.append("=");
            sb.append(value);
            sb.append("\n");
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static Hashtable<String, String> makeMap(String... args) {
        Hashtable<String, String> map = new Hashtable<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }

        return map;
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static Hashtable getProperties(String s) {
        Hashtable p = new Hashtable();
        for (String line : StringUtil.split(s, "\n", true, true)) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = StringUtil.splitUpTo(line, "=", 2);
            if (toks.size() == 2) {
                p.put(toks.get(0), toks.get(1));
            } else if (toks.size() == 2) {
                p.put(toks.get(0), "");
            }
        }

        return p;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String hexEncode(String s) {
        byte[]       chars = s.getBytes();
        StringBuffer sb    = new StringBuffer();
        for (byte c : chars) {
            sb.append("\\x");
            sb.append(c);
        }

        return sb.toString();

    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public static boolean isImage(String path) {
        if (path == null) {
            return false;
        }
        path = path.toLowerCase();
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".gif") || path.endsWith(".png")
                || path.endsWith(".bmp")) {
            return true;
        }
        //wms layer
        if (path.startsWith("http") && (path.indexOf("format=image") >= 0)) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @param files _more_
     * @param ascending _more_
     *
     * @return _more_
     */
    public static File[] sortFilesOnSize(File[] files,
                                         final boolean ascending) {

        ArrayList<IOUtil.FileWrapper> sorted =
                (ArrayList<IOUtil.FileWrapper>) new ArrayList();

        for (int i = 0; i < files.length; i++) {
            sorted.add(new IOUtil.FileWrapper(files[i], ascending));
        }

        Collections.sort(sorted, new FileSizeCompare(ascending));


        for (int i = 0; i < files.length; i++) {
            files[i] = sorted.get(i).getFile();
        }

        return files;
    }

    /**
     * _more_
     *
     * @param filePatternString _more_
     * @param patternNames _more_
     *
     * @return _more_
     */
    public static String extractPatternNames(String filePatternString,
                                             List<String> patternNames) {
        List<String> names                 = new ArrayList<String>();
        String       tmp                   = filePatternString;
        StringBuffer pattern               = new StringBuffer();
        boolean      gotAttributeInPattern = false;
        while (true) {
            int openParenIdx = tmp.indexOf("(");
            if (openParenIdx < 0) {
                pattern.append(tmp);

                break;
            }
            int closeParenIdx = tmp.indexOf(")");
            if (closeParenIdx < openParenIdx) {
                pattern.append(tmp);

                break;
            }
            int colonIdx = tmp.indexOf(":");
            if (colonIdx < 0) {
                pattern.append(tmp);

                break;
            }
            if (closeParenIdx < colonIdx) {
                pattern.append(tmp.substring(0, closeParenIdx + 1));
                names.add("");
                tmp = tmp.substring(closeParenIdx + 1);

                continue;
            }
            pattern.append(tmp.substring(0, openParenIdx + 1));
            String name = tmp.substring(openParenIdx + 1, colonIdx);
            names.add(name);
            gotAttributeInPattern = true;
            pattern.append(tmp.substring(colonIdx + 1, closeParenIdx + 1));
            tmp = tmp.substring(closeParenIdx + 1);
        }
        if ( !gotAttributeInPattern) {
            pattern = new StringBuffer(filePatternString);
            names   = new ArrayList<String>();
        }
        patternNames.addAll(names);

        return pattern.toString();
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public static Image readImage(String file) {
        if (file == null) {
            return null;
        }
        try {
            InputStream is = IOUtil.getInputStream(file, Utils.class);
            if (is != null) {
                byte[] bytes = IOUtil.readBytes(is);
                Image  image = Toolkit.getDefaultToolkit().createImage(bytes);
                image = ImageUtils.waitOnImage(image);

                return image;
            }
            System.err.println("Could not read image:" + file);
        } catch (Exception exc) {
            System.err.println(exc + " getting image:  " + file);

            return null;
        }

        return null;
    }

    /**
     * This method is taken from Unidatas ucar.unidata.util.Misc method.
     * I moved it here to not have it use the parseNumber because that would use
     * a DecimalFormat which was picking up the Locale
     *
     * Decodes a string representation of a latitude or longitude and
     * returns a double version (in degrees).  Acceptible formats are:
     * <pre>
     * +/-  ddd:mm, ddd:mm:, ddd:mm:ss, ddd::ss, ddd.fffff ===>   [+/-] ddd.fffff
     * +/-  ddd, ddd:, ddd::                               ===>   [+/-] ddd
     * +/-  :mm, :mm:, :mm:ss, ::ss, .fffff                ===>   [+/-] .fffff
     * +/-  :, ::                                          ===>       0.0
     * Any of the above with N,S,E,W appended
     * </pre>
     *
     * @param latlon  string representation of lat or lon
     * @return the decoded value in degrees
     */
    public static double decodeLatLon(String latlon) {
        // first check to see if there is a N,S,E,or W on this
        latlon = latlon.trim();
        int    dirIndex    = -1;
        int    southOrWest = 1;
        double value       = Double.NaN;
        if (latlon.indexOf("S") > 0) {
            southOrWest = -1;
            dirIndex    = latlon.indexOf("S");
        } else if (latlon.indexOf("W") > 0) {
            southOrWest = -1;
            dirIndex    = latlon.indexOf("W");
        } else if (latlon.indexOf("N") > 0) {
            dirIndex = latlon.indexOf("N");
        } else if (latlon.endsWith("E")) {  // account for 9E-3, 9E-3E, etc
            dirIndex = latlon.lastIndexOf("E");
        }

        if (dirIndex > 0) {
            latlon = latlon.substring(0, dirIndex).trim();
        }

        // now see if this is a negative value
        if (latlon.indexOf("-") == 0) {
            southOrWest *= -1;
            latlon      = latlon.substring(latlon.indexOf("-") + 1).trim();
        }

        if (latlon.indexOf(":") >= 0) {  //have something like DD:MM:SS, DD::, DD:MM:, etc
            int    firstIdx = latlon.indexOf(":");
            String hours    = latlon.substring(0, firstIdx);
            String minutes  = latlon.substring(firstIdx + 1);
            String seconds  = "";
            if (minutes.indexOf(":") >= 0) {
                firstIdx = minutes.indexOf(":");
                String temp = minutes.substring(0, firstIdx);
                seconds = minutes.substring(firstIdx + 1);
                minutes = temp;
            }
            try {

                value = (hours.equals("") == true)
                        ? 0
                        : Double.parseDouble(hours);
                if ( !minutes.equals("")) {
                    value += Double.parseDouble(minutes) / 60.;
                }
                if ( !seconds.equals("")) {
                    value += Double.parseDouble(seconds) / 3600.;
                }
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        } else {  //have something like DD.ddd
            try {
                value = Double.parseDouble(latlon);
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
        }

        return value * southOrWest;
    }

    /**
     * _more_
     *
     * @param list _more_
     * @param index _more_
     *
     * @return _more_
     */
    public static Object safeGet(List list, int index) {
        if ((list == null) || (index >= list.size())) {
            return null;
        }

        return list.get(index);
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Apr 12, '14
     * @author         Enter your name here...
     */
    private static class FileSizeCompare implements Comparator<IOUtil
            .FileWrapper> {

        /** _more_ */
        private boolean ascending;

        /**
         * _more_
         *
         * @param ascending _more_
         */
        public FileSizeCompare(boolean ascending) {
            this.ascending = ascending;
        }

        /**
         * _more_
         *
         * @param o1 _more_
         * @param o2 _more_
         *
         * @return _more_
         */
        public int compare(IOUtil.FileWrapper o1, IOUtil.FileWrapper o2) {
            int result;
            if (o1.length() < o2.length()) {
                result = -1;
            } else if (o1.length() > o2.length()) {
                result = 1;
            } else {
                result = o1.getFile().compareTo(o1.getFile());
            }
            if ( !ascending || (result == 0)) {
                return result;
            }

            return -result;

        }

    }


}

/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.geoloc;


import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * The CoordinateFormat class to format geo coordinates.
 */
public class CoordinateFormat {

    /** Empty format. */
    public static final Format EMPTY_FORMAT = new EmptyFormat();

    /** Pattern for degrees */
    private static final Pattern DEGREE_PATTERN =
        Pattern.compile("DD(\\.d+)?(:|\\s*)");

    /** Pattern for minutes */
    private static final Pattern MINUTE_PATTERN =
        Pattern.compile("MM(\\.m+)?('|:|\\s*)");

    /** Pattern for seconds */
    private static final Pattern SECOND_PATTERN =
        Pattern.compile("SS(\\.s+)?(\"|:|\\s*)");

    /** Pattern for cardinality */
    private static final Pattern CARDINALITY_PATTERN =
        Pattern.compile("H\\s*$");

    /** Map from enum string to enum symbol */
    private static final Map<String, DegMinSec> DMS_ENUM_MAP =
        enumMapifer(DegMinSec.class);


    /**
     * The Cardinality enum.
     */
    public enum Cardinality {

        /** NORTH. */
        NORTH("N"),

        /** SOUTH. */
        SOUTH("S"),

        /** EAST. */
        EAST("E"),

        /** WEST. */
        WEST("W"),

        /** NONE. */
        NONE("");

        /** The cardinality. */
        private final String cardinality;

        /**
         * Instantiates a new cardinality.
         *
         * @param cardinality the cardinality
         */
        private Cardinality(final String cardinality) {
            this.cardinality = cardinality;
        }

        /**
         * {@inheritDoc}
         *
         * @return _more_
         */
        @Override
        public String toString() {
            return cardinality;
        }
    }

    /**
     * The Degrees Minutes Seconds enum.
     */
    public enum DegMinSec {

        /** Degrees. */
        DEGREE(" "),  //Just a space for now since VisAD does not support degree symbol.

        /** Minutes. */
        MINUTE("'"),

        /** Seconds. */
        SECOND("\""),

        /** Colon. */
        COLON(":"),

        /** None. */
        NONE(""),

        /** Empty. */
        EMPTY(" ");

        /** The dms. */
        private final String dms;

        /**
         * Instantiates a new deg min sec.
         *
         * @param dms the dms
         */
        private DegMinSec(final String dms) {
            this.dms = dms;
        }

        /**
         * {@inheritDoc}
         *
         *
         * @return _more_
         */
        @Override
        public String toString() {
            return dms;
        }
    }

    /**
     * Accuracy.
     *
     * @param accuracy the accuracy
     * @return the format string
     */
    private static String accuracy(final int accuracy) {
        if (accuracy == 0) {
            return "00";
        } else {
            final StringBuffer sb = new StringBuffer();

            for (int i = 0; i < accuracy; i++) {
                sb.append("0");
            }

            return "00." + sb.toString();
        }
    }

    /**
     * Convert a decimal coordinate.
     *
     * @param coord the decimal coord
     * @param degF the deg format
     * @param minF the min format
     * @param secF the sec format
     * @param card the cardinality
     * @return the formatted coordinate string
     */
    public static String convert(double coord, Format degF, Format minF,
                                 Format secF, Cardinality card) {
        double coordAbs = Math.abs(coord);
        double minutes  = ((coordAbs - (int) coordAbs) * 60.0d);
        double seconds  = ((minutes - (int) minutes) * 60.0d);

        return ((coord < 0)
                ? "-"
                : "") + degF.format(coordAbs) + minF.format(minutes)
                      + secF.format(seconds) + card;
    }

    /**
     * Format a longitude to the given format.
     * {@link #formatLongitude(double, String, boolean)}
     *
     * @param value  the value to format
     * @param format the format
     *
     * @return formatted latitude
     */
    public static String formatLatitude(double value, String format) {
        Matcher     matcherCardinality = CARDINALITY_PATTERN.matcher(format);

        Cardinality c;

        if (value == 0) {
            c = Cardinality.NONE;
        } else if (matcherCardinality.find()) {
            c     = (value > 0)
                    ? Cardinality.NORTH
                    : Cardinality.SOUTH;
            value = Math.abs(value);
        } else {
            c = Cardinality.NONE;
        }

        Iterator<Format> i = tokenize(format).iterator();
        return CoordinateFormat.convert(value, i.next(), i.next(), i.next(),
                                        c);
    }

    /**
     * Format a latitude or longitude value to the given format.  Formats use
     * DD for degrees, MM for minutes, SS for seconds and d, m, s for decimal
     * fractions of degrees, minutes, seconds.  H designates the hemisphere
     * (N,S,E,W).
     * <pre>
     * Examples for value -34.496 degrees
     *
     *     DD:MM:SS      ===>  -34:29:45
     *       (if longitude and use360 ===> 326:29:45)
     *     DDH           ===>   34W     (or 34S if longitude)
     *     DD.d          ===>  -34.5
     *     DD.dddH       ===>   34.496W (or 34.496S if longitude)
     *     DD MM'SS.s"   ===>  -34 29'45.6"
     *
     * </pre>
     *
     * @param value  the value to format
     * @param format the format
     * @param use360      if true use 0-360 notation instead of -180 to 180 notation
     *
     * @return formatted longitude
     */
    public static String formatLongitude(double value, String format,
                                         boolean use360) {

        Matcher     matcherCardinality = CARDINALITY_PATTERN.matcher(format);

        Cardinality c;
        if (value == 0) {
            c = Cardinality.NONE;
        } else if (use360) {
            value = (value < 0)
                    ? value + 360
                    : value;
            c     = Cardinality.NONE;
        } else if (matcherCardinality.find()) {
            value = (value > 180)
                    ? (value - 360)
                    : value;
            c     = (value > 0)
                    ? Cardinality.EAST
                    : Cardinality.WEST;
            value = Math.abs(value);
        } else {
            value = (value > 180)
                    ? (value - 360)
                    : value;
            c = Cardinality.NONE;
        }

        Iterator<Format> i = tokenize(format).iterator();
        return CoordinateFormat.convert(value, i.next(), i.next(), i.next(),
                                        c);

    }

    /**
     * Takes a string containing tokens and cuts it apart into individual tokens.
     *
     * @param tokens the token string that will be cut apart into individual tokens
     * @return the list of three tokens
     */
    private static List<Format> tokenize(String tokens) {
        List<Format> l              = new LinkedList<Format>();

        Matcher      degreeMathcher = DEGREE_PATTERN.matcher(tokens);
        Matcher      matcherMinute  = MINUTE_PATTERN.matcher(tokens);
        Matcher      matcherSecond  = SECOND_PATTERN.matcher(tokens);

        boolean      hasMinute      = matcherMinute.find();
        boolean      hasSecond      = matcherSecond.find();

        if (degreeMathcher.find()) {
            String group    = degreeMathcher.group().trim();
            int    accuracy = group.length()
                           - group.replaceAll("d", "").length();
            DegMinSec dms = DMS_ENUM_MAP.get(group.replaceAll("DD(\\.d+)?",
                                ""));
            dms = (dms.equals(DegMinSec.NONE) && hasMinute)
                  ? DegMinSec.EMPTY
                  : dms;
            l.add(((accuracy == 0) && hasMinute)
                  ? new FloorCoordFormat(dms)
                  : new DecimalCoordFormat(accuracy, dms));
        } else {
            l.add(EMPTY_FORMAT);
        }

        if (hasMinute) {
            String group    = matcherMinute.group().trim();
            int    accuracy = group.length()
                           - group.replaceAll("m", "").length();
            DegMinSec dms = DMS_ENUM_MAP.get(group.replaceAll("MM(\\.m+)?",
                                ""));
            dms = (dms.equals(DegMinSec.NONE) && hasSecond)
                  ? DegMinSec.EMPTY
                  : dms;
            l.add(((accuracy == 0) && hasSecond)
                  ? new FloorCoordFormat(dms)
                  : new DecimalCoordFormat(accuracy, dms));
        } else {
            l.add(EMPTY_FORMAT);
        }

        if (hasSecond) {
            String group     = matcherSecond.group().trim();
            int    charCount = group.length()
                            - group.replaceAll("s", "").length();
            DegMinSec dms = DMS_ENUM_MAP.get(group.replaceAll("SS(\\.s+)?",
                                ""));
            l.add(new DecimalCoordFormat(charCount, dms));
        } else {
            l.add(EMPTY_FORMAT);
        }

        return l;
    }

    /**
     * The Format interface.
     */
    public interface Format {

        /**
         * Format the number.
         *
         * @param number to be formatted
         * @return the formatted number as string
         */
        public String format(double number);
    }


    /**
     * The DecimalCoordFormat.
     */
    public static class DecimalCoordFormat implements Format {

        /** The decimal accuracy. */
        private final int accuracy;

        /** The degrees minutes seconds. */
        private final DegMinSec degminsec;

        /**
         * Instantiates a new coord format.
         *
         * @param accuracy the decimal accuracy
         * @param degminsec the degminsec
         */
        public DecimalCoordFormat(final int accuracy,
                                  final DegMinSec degminsec) {
            this.accuracy  = accuracy;
            this.degminsec = degminsec;
        }

        /**
         * {@inheritDoc}
         *
         * @param number _more_
         *
         * @return _more_
         */
        public String format(double number) {
            //Strangely ' needs to be adjust to '' to make DecimalFormat happy.
            String       d         = (degminsec.equals(DegMinSec.MINUTE))
                                     ? (degminsec + "'")
                                     : degminsec + "";
            NumberFormat formatter = new DecimalFormat(accuracy(accuracy)
                                         + d);

            return formatter.format(number);
        }
    }


    /**
     * The empty format.
     */
    private static class EmptyFormat implements Format {

        /**
         * {@inheritDoc}
         *
         * @param number _more_
         *
         * @return _more_
         */
        @Override
        public String format(double number) {
            return "";
        }
    }


    /**
     * The FloorCoordFormat class. Useful when you simply want to lop off anything after the decimal point.
     */
    public static class FloorCoordFormat implements Format {

        /** The degrees minutes seconds. */
        private final DegMinSec degminsec;

        /**
         * Instantiates a new floor coord format.
         *
         * @param degminsec the degminsec
         */
        public FloorCoordFormat(final DegMinSec degminsec) {
            this.degminsec = degminsec;
        }

        /**
         * {@inheritDoc}
         *
         * @param number _more_
         *
         * @return _more_
         */
        public String format(double number) {
            //Strangely ' needs to be adjust to '' to make DecimalFormat happy.
            String       d         = (degminsec.equals(DegMinSec.MINUTE))
                                     ? (degminsec + "'")
                                     : degminsec + "";
            NumberFormat formatter = new DecimalFormat(accuracy(0) + d);
            return formatter.format((int) number);
        }
    }

    /**
     * Enum mapifer. Create a map from enum string mapped to enum
     *
     * @param <E> the element type
     * @param clazz the class of the enum
     * @return the string enum map
     */
    private static <E extends Enum<E>> Map<String,
                                           E> enumMapifer(Class<E> clazz) {
        Map<String, E> map = new HashMap<String, E>();
        for (E e : clazz.getEnumConstants()) {
            map.put(e.toString(), e);
        }
        return Collections.unmodifiableMap(map);
    }
}

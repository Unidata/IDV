/*
 * $Id: TimestampFormat.java,v 1.11 2006/05/05 19:19:38 jeffmc Exp $
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


package ucar.unidata.util;


import java.text.DateFormat;
import java.text.FieldPosition;



import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;

import java.util.Calendar;

import java.util.Date;


/**
 * TODO: serious synchronization issues
 *
 * @author $Author: jeffmc $
 * @version $Revision: 1.11 $ $Date: 2006/05/05 19:19:38 $
 */
public class TimestampFormat extends java.text.Format {

    /** _more_ */
    static private final UTCCalendar utcCal_ = new UTCCalendar();

    /**
     * SimpleDateFormat pattern to describe
     * ISO 8601 complete date plus hours, minutes, seconds
     * and a decimal fraction of a second.
     * <a href="http://www.w3.org/TR/NOTE-datetime">
     *      http://www.w3.org/TR/NOTE-datetime</a>
     * @see java.text.SimpleDateFormat
     */
    static public final String isoTimeFmtPattern =
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * Formats an Timestamp to produce a string.
     * ISO 8601 Complete date plus hours, minutes, seconds
     * and a decimal fraction of a second.
     * <a href="http://www.w3.org/TR/NOTE-datetime">
     *      http://www.w3.org/TR/NOTE-datetime</a>
     *
     * @param ts    Timestamp to format
     * @param buf   StringBuffer where the text is to be appended
     * @param fp    On input: an alignment field, if desired.
     * On output: the offsets of the alignment field.
     * @return       the value passed in as toAppendTo
     */
    static public StringBuffer isoformat(Timestamp ts, StringBuffer buf,
                                         FieldPosition fp) {
        if (fp == null) {
            fp = new FieldPosition(0);
        }
        final java.text.NumberFormat nf =
            java.text.NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(false);

        synchronized (utcCal_) {
            utcCal_.setTime(ts);

            nf.setMinimumIntegerDigits(4);
            nf.format(utcCal_.get(java.util.Calendar.YEAR), buf, fp);
            buf.append("-");
            nf.setMinimumIntegerDigits(2);
            nf.format(utcCal_.get(java.util.Calendar.MONTH) + 1, buf, fp);
            buf.append("-");
            nf.format(utcCal_.get(java.util.Calendar.DATE), buf, fp);
            buf.append("T");
            nf.format(utcCal_.get(java.util.Calendar.HOUR_OF_DAY), buf, fp);
            buf.append(":");
            nf.format(utcCal_.get(java.util.Calendar.MINUTE), buf, fp);
            buf.append(":");
            nf.format(utcCal_.get(java.util.Calendar.SECOND), buf, fp);
            buf.append(".");
            nf.setMinimumIntegerDigits(3);
            nf.format(utcCal_.get(java.util.Calendar.MILLISECOND), buf, fp);
            buf.append("Z");
        }  // synchronized(utcCal_)
        return buf;
    }

    /**
     * _more_
     *
     * @param ts
     * @return _more_
     */
    static public String isoformat(Timestamp ts) {
        return isoformat(ts, new StringBuffer(),
                         new FieldPosition(0)).toString();
    }

    /**
     * Return the number of days to add() to ref DAY_OF_MONTH
     * to get ref DAY_OF_MONTH = dom, such that the time in ref
     * changes the least. Ties favor earlier time.
     * Assumes dom is in ref MONTH, the month prior, or the month after
     * @param dom int day of month, 1-31
     * @param ref Calendar containing the reference time.
     *      exclusive access to ref should be guaranteed by the caller.
     * @return _more_
     */
    static public int domOff(int dom, Calendar ref) {
        if ((dom < 1) || (dom > 31)) {
            return 0;  // dom unknown
        }

        final int rdom = ref.get(Calendar.DAY_OF_MONTH);
        final int diff = dom - rdom;

        if (diff > 0) {
            // dom gt rdom, dom is in refmonth-- or refmonth
            // revdiff = dayslastmonth - dom + rdom
            ref.add(Calendar.MONTH, -1);
            final int revdiff = ref.getActualMaximum(Calendar.DAY_OF_MONTH)
                                - diff;
            ref.add(Calendar.MONTH, 1);
            if (diff >= revdiff) {
                return -revdiff;
            }
            // else return diff
        } else if (diff < 0) {
            // dom lt rdom, dom is in refmonth++ or refmonth
            // fwddiff = (daysthismonth - rdom) + dom ;
            final int fwddiff = ref.getActualMaximum(Calendar.DAY_OF_MONTH)
                                + diff;
            if (fwddiff < -diff) {  // abs(diff)
                return fwddiff;
            }
            // else return diff
        }
        return diff;
    }

    /**
     * Return the number of hours to add() to ref HOUR_OF_DAY
     * to get ref HOUR_OF_DAY = hod, such that the time in ref
     * changes the least. Ties favor earlier time.
     * @param hod int hour of day, 0-23
     * @param ref Calendar containing the reference time.
     *      exclusive access to ref should be guaranteed by the caller.
     * @return _more_
     */
    static private int hodOff(int hod, Calendar ref) {
        if ((hod < 0) || (hod > 23)) {
            return 0;  // hod unknown
        }

        final int rhod = ref.get(Calendar.HOUR_OF_DAY);
        final int diff = hod - rhod;

        if (diff > 0) {
            // hod gt rhod, hod is in refhour-- or refhour
            // revdiff = (24 - hod) + rhod
            final int revdiff = 24 - diff;
            if (diff >= revdiff) {
                return -revdiff;
            }
            // else return diff
        } else if (diff < 0) {
            // hod lt rhod, hod is in refhour++ or refhour
            // fwddiff = (24 - rhod) + hod ;
            final int fwddiff = 24 + diff;
            if (fwddiff < -diff) {  // abs(diff)
                return fwddiff;
            }
            // else return diff
        }
        return diff;
    }

    /**
     * Given a time string and a time reference, compute
     * the time the string represents. All times are UTC,
     * Gregorian Calendar.
     *
     * @param ddhhmm String timestamp of the form [DD]HHMM
     *      where
     *              DD is 2 digits day of month (01-31) (may be missing)
     *              HH is 2 digits hour of day (00-23)
     *              MM is 2 digits minute of hour (00-59)
     *
     * @param ref Timestamp of some moment near to
     *      and generally after the time represented by ddhhmm
     *
     * @return Timestamp corresponding to ddhhmm.
     *
     * @throws ParseException
     */
    static public Timestamp computeTimestamp(
            String ddhhmm, Timestamp ref) throws ParseException {
        int dd    = -1;
        int hh    = -1;
        int mm    = -1;

        int begin = 0;
        if (ddhhmm.length() > 5) {
            dd = Integer.parseInt(ddhhmm.substring(begin, begin + 2));
            if ((dd < 1) || (dd > 31)) {
                throw new ParseException("No day of month \"" + ddhhmm
                                         + "\"", begin);
            }
        }
        begin += 2;
        hh    = Integer.parseInt(ddhhmm.substring(begin, begin + 2));
        if ((hh < 0) || (hh > 24)) {
            throw new ParseException("No hour \"" + ddhhmm + "\"", begin);
        }
        begin += 2;
        mm    = Integer.parseInt(ddhhmm.substring(begin, begin + 2));
        if ((mm < 0) || (mm > 60)) {
            throw new ParseException("No minutes \"" + ddhhmm + "\"", begin);
        }

        synchronized (utcCal_) {
            /*
             * Use ref to initialize year, month
             * (day, hour, min)
             */
            utcCal_.setTime(ref);
            utcCal_.set(Calendar.MILLISECOND, 0);
            utcCal_.set(Calendar.SECOND, 0);
            if (dd > 0) {
                final int off = domOff(dd, utcCal_);
                utcCal_.set(Calendar.MINUTE, mm);
                utcCal_.set(Calendar.HOUR_OF_DAY, hh);
                if (off != 0) {
                    utcCal_.add(Calendar.DAY_OF_MONTH, off);
                }
            } else {
                final int off = hodOff(hh, utcCal_);
                utcCal_.set(Calendar.MINUTE, mm);
                if (off != 0) {
                    utcCal_.add(Calendar.HOUR_OF_DAY, off);
                }

            }

            return utcCal_.getTimestamp();
        }  // synchronized(utcCal_)
    }

    /** _more_ */
    private DateFormat delegate_ = null;

    /**
     * _more_
     *
     */
    public TimestampFormat() {
        super();
    }

    /**
     * _more_
     *
     * @param delegate
     *
     */
    public TimestampFormat(DateFormat delegate) {
        setDelegate(delegate);
    }

    /**
     * _more_
     *
     * @param delegate
     * @param cal
     *
     */
    public TimestampFormat(DateFormat delegate, Calendar cal) {
        setDelegate(delegate);
        delegate_.setCalendar(cal);
    }

    /**
     * _more_
     *
     * @param delegate
     */
    public void setDelegate(DateFormat delegate) {
        delegate_ = delegate;
    }

    /**
     * _more_
     * @return _more_
     */
    public DateFormat getDelegate() {
        return delegate_;
    }

    /**
     * Formats an Timestamp to produce a string.
     *
     * @param ts    Timestamp to format
     * @param buf   StringBuffer where the text is to be appended
     * @param fp    On input: an alignment field, if desired.
     * On output: the offsets of the alignment field.
     * @return       the value passed in as toAppendTo
     */
    public StringBuffer format(Timestamp ts, StringBuffer buf,
                               FieldPosition fp) {
        if (delegate_ == null) {
            return isoformat(ts, buf, fp);
        }
        return delegate_.format(new Date(ts.getMillis()), buf, fp);
    }

    /**
     * _more_
     *
     * @param ts
     * @param buf
     * @param pos
     * @return _more_
     */
    public StringBuffer format(Object ts, StringBuffer buf,
                               FieldPosition pos) {
        return format((Timestamp) ts, buf, pos);
    }

    /*
    public String
    format(Timestamp ts)
    {
            return format(ts,
                    new StringBuffer(), new FieldPosition(0)).toString();
    }
    */

    /**
     * Parses a string to produce a Timestamp.
     *
     * @param source String to parse
     * @param status Input-Output parameter.
     * <p>
     * Before calling, set status.index to the offset
     * you want to start parsing at in the source.
     * After calling, status.index is the end of the text you parsed.
     * If error occurs, index is unchanged.
     * <p>
     * When parsing, leading whitespace is discarded
     * (with successful parse),
     * while trailing whitespace is left as is.
     * @return _more_
     */
    public Timestamp parse(String source, ParsePosition status) {
        if (delegate_ == null) {
            return null;  // TODO
        }
        final Date dt = delegate_.parse(source, status);
        if (dt == null) {
            return null;
        }
        return new Timestamp(dt);
    }

    /**
     * _more_
     *
     * @param source
     * @return _more_
     *
     * @throws ParseException
     */
    public Timestamp parse(String source) throws ParseException {
        final ParsePosition status = new ParsePosition(0);
        try {
            final Timestamp ret = parse(source, status);
            if (status.getIndex() == 0) {
                throw new ParseException("Unparseable \"" + source + "\"",
                                         status.getErrorIndex());
            }
            return ret;
        } catch (NullPointerException npe) {
            throw new ParseException("Unparseable \"" + source + "\"",
                                     status.getErrorIndex());
        }
    }

    /**
     * _more_
     *
     * @param source
     * @param status
     * @return _more_
     */
    public Object parseObject(String source, ParsePosition status) {
        return parse(source, status);
    }

    /* Begin Test */

    /**
     * _more_
     *
     * @param args
     */
    public static void test0(String[] args) {
        String pattern = isoTimeFmtPattern;
        if (args.length > 0) {
            pattern = args[args.length - 1];
        }

        final TimestampFormat fmt =
            new TimestampFormat(new java.text.SimpleDateFormat(pattern),
                                utcCal_);

        try {
            final java.io.BufferedReader brdr =
                new java.io.BufferedReader(
                    new java.io.InputStreamReader(System.in));
            for (String line = brdr.readLine(); line != null;
                    line = brdr.readLine()) {
                try {
                    Timestamp ts = fmt.parse(line);
                    System.out.println(fmt.format(ts));
                    System.out.println(ts);
                } catch (ParseException pe) {
                    System.err.println("java.text.ParseException: "
                                       + pe.getMessage() + " at pos "
                                       + pe.getErrorOffset());
                }
            }
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * _more_
     *
     * @param args
     */
    public static void tdomOff(String[] args) {
        final TimestampFormat fmt = new TimestampFormat(
                                        new java.text.SimpleDateFormat(
                                            isoTimeFmtPattern), utcCal_);
        try {
            final java.io.BufferedReader brdr =
                new java.io.BufferedReader(
                    new java.io.InputStreamReader(System.in));
            UTCCalendar ref = new UTCCalendar();
            for (String line = brdr.readLine(); line != null;
                    line = brdr.readLine()) {
                try {
                    ref.setTime(fmt.parse(line));
                    line = brdr.readLine();
                    if (line == null) {
                        break;
                    }
                    int dom = Integer.parseInt(line);
                    int off = domOff(dom, ref);
                    System.out.println("off " + Integer.toString(off));
                    ref.add(Calendar.DAY_OF_MONTH, off);

                    System.out.println(ref.getTimestamp());
                } catch (ParseException pe) {
                    System.err.println("java.text.ParseException: "
                                       + pe.getMessage() + " at pos "
                                       + pe.getErrorOffset());
                }
            }
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * _more_
     *
     * @param args
     */
    public static void testHodOff(String[] args) {
        try {
            UTCCalendar ref = new UTCCalendar();
            ref.setTime(Integer.MAX_VALUE);
            ref.set(Calendar.MILLISECOND, 0);
            ref.set(Calendar.SECOND, 0);
            ref.set(Calendar.MINUTE, 0);
            for (int rhod = 0; rhod < 24; rhod++) {
                ref.set(Calendar.HOUR_OF_DAY, rhod);
                for (int hod = 0; hod < 24; hod++) {
                    int off = hodOff(hod, ref);
                    System.out.println(Integer.toString(hod) + " "
                                       + Integer.toString(rhod) + " "
                                       + Integer.toString(off));
                }

            }
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * _more_
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            UTCCalendar ref = new UTCCalendar();
            ref.setTime(Integer.MAX_VALUE);
            ref.set(Calendar.MILLISECOND, 0);
            ref.set(Calendar.SECOND, 0);
            ref.set(Calendar.MINUTE, 0);
            ref.set(Calendar.HOUR, 0);
            for (int mm = 0; mm < 4 * 12 + 2; mm++) {
                for (int rdom = 1; rdom < 32; rdom++) {
                    ref.set(Calendar.DAY_OF_MONTH, rdom);
                    for (int dom = 1; dom < 32; dom++) {
                        int off = domOff(dom, ref);
                        System.out.println(Integer.toString(dom) + " "
                                           + Integer.toString(rdom) + " "
                                           + Integer.toString(off));
                    }

                }
                System.out.println("###");
                ref.add(Calendar.MONTH, 1);
            }
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
    /* End Test */
}


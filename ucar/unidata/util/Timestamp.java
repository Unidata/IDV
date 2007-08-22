/*
 * $Id: Timestamp.java,v 1.13 2006/05/05 19:19:38 jeffmc Exp $
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



import java.io.Serializable;

import java.lang.System;

import java.util.Date;


/**
 * A immutable timestamp and related utilities.
 * Hopefully lighter weight than java.util.Date (which is mutable),
 * and  we don't have to trip over javasoft/Taligent deprecations.
 * <p>
 * We are using the java "long millisecs since the epoch" as
 * internal representation. This doesn't roll over until
 * August of 292,278,994.
 *
 * @author $Author: jeffmc $
 * @version $Revision: 1.13 $ $Date: 2006/05/05 19:19:38 $
 */
public class Timestamp implements Comparable, Serializable {

    /** _more_ */
    static public final Timestamp NONE = new Timestamp(Long.MIN_VALUE);

    /** _more_ */
    protected final long millis_;

    /**
     * Construct a Timestamp and initialized to represent
     * the specified number of milliseconds since the standard
     * base time known as "the epoch",
     * namely January 1, 1970, 00:00:00 GMT.
     *
     * @param millis long milliseconds since January 1, 1970, 00:00:00 GMT
     * @see System#currentTimeMillis()
     */
    public Timestamp(long millis) {
        millis_ = millis;
    }

    /**
     * Construct a Timestamp and initialized to the current time.
     *
     * @see System#currentTimeMillis()
     */
    public Timestamp() {
        millis_ = System.currentTimeMillis();
    }

    /**
     * Construct a Timestamp and initialized by a Date object.
     *
     * @see java.util.Date
     *
     * @param date
     */
    public Timestamp(Date date) {
        millis_ = date.getTime();
    }

    /**
     * Construct a Timestamp initialized by current time minus given seconds.
     * @param secsPrior number oif seconds prior to current time
     */
    public Timestamp(int secsPrior) {
        millis_ = System.currentTimeMillis() - secsPrior * 1000;
    }

    /**
     * @return long time value represented by this,
     * in number of milliseconds since January 1, 1970, 00:00:00 GMT
     */
    public long getMillis() {
        return millis_;
    }

    /**
     * _more_
     *
     * @param buf
     * @return _more_
     */
    public StringBuffer format(StringBuffer buf) {
        return TimestampFormat.isoformat(this, buf, null);
    }

    /*
     * @see java.lang.Long#hashCode
     */

    /**
     * _more_
     * @return _more_
     */
    public int hashCode() {
        return (int) (millis_ ^ (millis_ >> 32));
    }

    /**
     * _more_
     *
     * @param oo
     * @return _more_
     */
    public boolean equals(Object oo) {
        if ((oo != null) && (oo instanceof Timestamp)) {
            return millis_ == ((Timestamp) oo).millis_;
        }
        return false;
    }

    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return format(new StringBuffer()).toString();
    }

    /**
     * _more_
     *
     * @param ts
     * @return _more_
     */
    public int compareTo(Timestamp ts) {
        return ((millis_ < ts.millis_)
                ? -1
                : ((millis_ > ts.millis_)
                   ? 1
                   : 0));
    }

    /**
     * _more_
     *
     * @param oo
     * @return _more_
     */
    public int compareTo(Object oo) {
        return compareTo((Timestamp) oo);
    }

    /* Begin Test */

    /**
     * _more_
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            Timestamp crash = new Timestamp(Long.MAX_VALUE);
            System.out.println("End Epoch: " + crash);
            Timestamp zed = new Timestamp(0);
            System.out.println("Begin Epoch:    " + zed);
            Timestamp now = new Timestamp();
            System.out.println("now:            " + now);
            Timestamp big = new Timestamp(Integer.MAX_VALUE);
            System.out.println("sometime:       " + big);


            TimestampFormat fmt =
                new TimestampFormat(new java.text.SimpleDateFormat());
            System.out.println("sometime: " + fmt.format(big));


            java.text.SimpleDateFormat fmt1 =
                new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            fmt1.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
            fmt.setDelegate(fmt1);
            System.out.println("sometime:       " + fmt.format(big));
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
    /* End Test */
}


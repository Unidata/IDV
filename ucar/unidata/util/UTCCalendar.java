/*
 * $Id: UTCCalendar.java,v 1.11 2006/05/05 19:19:38 jeffmc Exp $
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



import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * A Gregorian calendar which is always
 * on "Coordinated Universal Time".
 * The timezone is initialized to GMT and
 * The setTimeZone() method, among other set methods,
 * throws UnsupportedOperationException
 *
 *
 * @author $Author: jeffmc $
 * @version $Revision: 1.11 $ $Date: 2006/05/05 19:19:38 $
 */
public class UTCCalendar extends GregorianCalendar {

    /**
     * Constructs a UTCCalendar with the default locale,
     * initialized to the current time.
     */
    public UTCCalendar() {
        super(TimeZone.getTimeZone("GMT"));  // Assumes default locale okay for our purposes.
    }

    /**
     * _more_
     *
     * @param ts
     */
    public void setTime(Timestamp ts) {
        setTimeInMillis(ts.getMillis());
    }

    /**
     * _more_
     *
     * @param millis
     */
    public void setTime(long millis) {
        setTimeInMillis(millis);
    }

    /**
     * Override protected method for public access.
     *
     * @param millis
     */
    public void setTimeInMillis(long millis) {
        super.setTimeInMillis(millis);
    }

    /**
     * Override protected method for public access.
     * @return _more_
     */
    public long getTimeInMillis() {
        return super.getTimeInMillis();
    }

    /**
     * _more_
     * @return _more_
     */
    public Timestamp getTimestamp() {
        return new Timestamp(getTimeInMillis());
    }

    /**
     * _more_
     *
     * @param ignored
     */
    public void setFirstDayOfWeek(int ignored) {
        throw new UnsupportedOperationException();
    }

    /**
     * _more_
     *
     * @param ignored
     */
    public void setLenient(boolean ignored) {
        throw new UnsupportedOperationException();
    }

    /**
     * _more_
     *
     * @param ignored
     */
    public void setMinimalDaysInFirstWeek(int ignored) {
        throw new UnsupportedOperationException();
    }

    /**
     * _more_
     *
     * @param ignored
     */
    public void setTimeZone(TimeZone ignored) {
        throw new UnsupportedOperationException();
    }

    /**
     * _more_
     *
     * @param ignored
     */
    public void setGregorianChange(java.util.Date ignored) {
        throw new UnsupportedOperationException();
    }
}


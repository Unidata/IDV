/*
 * $Id: DateGMT.java,v 1.9 2006/05/05 19:19:34 jeffmc Exp $
 *
 * Copyright  1997-2025 Unidata Program Center/University Corporation for
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



/**
 * Convenience routine for dates in GMT.
 * @author davis
 * @version $Revision: 1.9 $ $Date: 2006/05/05 19:19:34 $
 */

public class DateGMT extends java.util.Date {

    /** _more_ */
    private static DateGMT current = new DateGMT();

    /** _more_ */
    private static java.text.SimpleDateFormat dateFormat;
    static {
        dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    }

    /**
     * _more_
     * @return _more_
     */
    static public DateGMT getCurrent() {
        current.setTime(System.currentTimeMillis());
        return current;
    }

    /**
     * _more_
     *
     */
    public DateGMT() {}

    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return dateFormat.format(this);
    }
}


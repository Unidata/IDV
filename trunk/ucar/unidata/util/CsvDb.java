/*
 * $Id: CsvDb.java,v 1.8 2006/05/05 19:19:34 jeffmc Exp $
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


import java.util.ArrayList;



import java.util.List;



/**
 *   This class has serves as a base class for the classes that are generated
 *   from a csv file.
 */

public abstract class CsvDb {

    /** _more_ */
    public static int BAD_INTEGER = -999999;

    /** _more_ */
    public static double BAD_DOUBLE = -999999.0;

    /**
     * _more_
     *
     */
    public CsvDb() {}

    /**
     *  Convert the given string to an integer or return BAD_INTEGER is string is empty.
     *
     * @param s
     * @return _more_
     */
    public static int getInt(String s) {
        if (s.length() == 0) {
            return BAD_INTEGER;
        }
        return Integer.parseInt(s);
    }

    /**
     *  Convert the given string to an double or return BAD_DOUBLE is string is empty.
     *
     * @param s
     * @return _more_
     */

    public static double getDouble(String s) {
        if (s.length() == 0) {
            return BAD_DOUBLE;
        }
        return Double.parseDouble(s);
    }

}


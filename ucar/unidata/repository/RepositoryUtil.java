/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository;

import ucar.unidata.util.HtmlUtil;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryUtil {

    /** timezone */
    public static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");


    /**
     * _more_
     *
     * @param b1 _more_
     * @param b2 _more_
     *
     * @return _more_
     */
    public static String buttons(String b1, String b2) {
        return b1 + HtmlUtil.space(2) + b2;
    }

    /**
     * _more_
     *
     * @param b1 _more_
     * @param b2 _more_
     * @param b3 _more_
     *
     * @return _more_
     */
    public static String buttons(String b1, String b2, String b3) {
        return b1 + HtmlUtil.space(2) + b2 + HtmlUtil.space(2) + b3;
    }




    /**
     * _more_
     *
     * @param formatString _more_
     *
     * @return _more_
     */
    public static SimpleDateFormat makeDateFormat(String formatString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.setTimeZone(TIMEZONE_GMT);
        dateFormat.applyPattern(formatString);
        return dateFormat;
    }


    /**
     * _more_
     *
     * @param path _more_
     */
    public static void checkFilePath(String path) {
        if (path.indexOf("..") >= 0) {
            throw new IllegalArgumentException("bad file path:" + path);
        }
    }


    /**
     * Class MissingEntryException _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class MissingEntryException extends Exception {

        /**
         * _more_
         *
         * @param msg _more_
         */
        public MissingEntryException(String msg) {
            super(msg);
        }
    }

    /**
     * Class AccessException _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class AccessException extends RuntimeException {

        /**
         * _more_
         *
         * @param message _more_
         */
        public AccessException(String message) {
            super(message);
        }
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected static String header(String h) {
        return "<div class=\"pageheading\">" + h + "</div>";
    }

}


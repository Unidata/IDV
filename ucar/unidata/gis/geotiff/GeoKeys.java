/*
 * $Id: GeoKeys.java,v 1.5 2005/05/13 18:29:38 jeffmc Exp $
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

package ucar.unidata.gis.geotiff;



import com.sun.media.jai.codec.*;


/**
 *  This class is a holder of key information for geotiffs.
 *  It is derived form a class that was generated from the set of properties
 *  files that hold all of the different geokey information.
 *
 */


public final class GeoKeys extends GeneratedKeys {

    /**
     * Class Tiff
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    public static class Tiff {

        /** _more_ */
        public static final int GEO_TIEPOINTS = 33922;

        /** _more_ */
        public static final int GEO_PIXEL_SCALE = 33550;

        /** _more_ */
        public static final int GEO_TRANS_MATRIX = 34264;

        /** _more_ */
        public static final int GEO_KEY_DIRECTORY = 34735;

        /** _more_ */
        public static final int GEO_DOUBLE_PARAMS = 34736;

        /** _more_ */
        public static final int GEO_ASCII_PARAMS = 34737;

        /** _more_ */
        private static int[] types = {
            TIFFField.TIFF_ASCII, TIFFField.TIFF_BYTE, TIFFField.TIFF_DOUBLE,
            TIFFField.TIFF_FLOAT, TIFFField.TIFF_LONG,
            TIFFField.TIFF_RATIONAL, TIFFField.TIFF_SBYTE,
            TIFFField.TIFF_SHORT, TIFFField.TIFF_SLONG,
            TIFFField.TIFF_SRATIONAL, TIFFField.TIFF_SSHORT,
            TIFFField.TIFF_UNDEFINED
        };


        /** _more_ */
        private static String[] typeNames = {
            "TIFFField.TIFF_ASCII", "TIFFField.TIFF_BYTE",
            "TIFFField.TIFF_DOUBLE", "TIFFField.TIFF_FLOAT",
            "TIFFField.TIFF_LONG", "TIFFField.TIFF_RATIONAL",
            "TIFFField.TIFF_SBYTE", "TIFFField.TIFF_SHORT",
            "TIFFField.TIFF_SLONG", "TIFFField.TIFF_SRATIONAL",
            "TIFFField.TIFF_SSHORT", "TIFFField.TIFF_UNDEFINED"
        };

        /**
         * _more_
         *
         * @param t
         * @return _more_
         */
        public static String getFieldType(int t) {
            for (int i = 0; i < types.length; i++) {
                if (types[i] == t) {
                    return typeNames[i];
                }
            }
            return null;
        }
    }



}









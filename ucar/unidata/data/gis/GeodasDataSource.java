/*
 * $Id: GeodasDataSource.java,v 1.9 2006/12/01 20:42:31 jeffmc Exp $
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

package ucar.unidata.data.gis;


import HTTPClient.*;

import ucar.unidata.data.*;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import visad.*;

import visad.data.gis.DemFamily;

import java.io.*;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Class GeodasDataSource tmop
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.9 $
 */
public class GeodasDataSource extends DataSourceImpl {



    /** the DEM reader */
    DemFamily dem;

    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public GeodasDataSource() {}


    /**
     * Create a GeodasDataSource from the specification given.
     *
     * @param descriptor              description of the source
     * @param source of file          filename
     * @param properties              extra properties
     *
     * @throws VisADException     VisAD problem
     */
    public GeodasDataSource(DataSourceDescriptor descriptor, String source,
                            Hashtable properties)
            throws VisADException {
        super(descriptor, source, "Geodas DEM data source", properties);
    }


    /** list of data categories */
    private List categories = DataCategory.parseCategories("DEM;GRID-2D;");


    /**
     * Make the {@link DataChoice}s associated with this source.
     */
    protected void doMakeDataChoices() {
        addDataChoice(new DirectDataChoice(this, "Elevation", "Elevation",
                                           "Elevation", categories));
    }

    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        if (dem == null) {
            dem = new DemFamily("dem", RealTupleType.SpatialEarth2DTuple);
        }

        //        FieldImpl fi = (FieldImpl) dem.open(filename);
        //        putCache(filename, fi);

        return null;
    }


    /**
     * Make the list of available times for this data source.
     *
     * @return  list of available data times
     */
    protected List doMakeDateTimes() {
        return new ArrayList();
    }



    /**
     * test code
     *
     * @throws Exception On badness
     */
    public void testit() throws Exception {
        String key = new String("" + Math.random() + ""
                                + Math.random()).substring(0, 8);
        key = StringUtil.replace(key, ".", "");
        System.err.println("key:" + key);
        int   left     = 120;
        int   right    = 100;
        int   top      = 45;
        int   bottom   = 30;

        int   cellSize = 0;
        int[] minutes  = {
            2, 4, 6, 8, 10, 12, 14, 16, 18, 20
        };
        int   lonNum   = 0;
        int   latNum   = 0;
        for (int i = 0; i < minutes.length; i++) {
            cellSize = minutes[i] * 60;
            lonNum   = (left - right) * 60 / minutes[i];
            latNum   = (top - bottom) * 60 / minutes[i];
            if (lonNum * latNum < 200000) {
                break;
            }
        }


        String[] args = {
            "KEY", key, "LATCELLSIZE", "" + cellSize, "LONCELLSIZE",
            "" + cellSize, "LATNUMCELLS", "" + latNum, "LONNUMCELLS",
            "" + lonNum, "TOPDEG", "" + top, "TOPMIN", "0", "TOPHEM", "N",
            "BOTTOMDEG", "" + bottom, "BOTTOMMIN", "0", "BOTTOMHEM", "N",
            "LEFTDEG", "" + left, "LEFTMIN", "0", "LEFTHEM", "W", "RIGHTDEG",
            "" + right, "RIGHTMIN", "0", "RIGHTHEM", "W", "DATA_BASE",
            "grdet2", "PATH_INV", "/net/prod/www/mgg/data/gdas", "PATH_CD",
            "/net/prod/www/mgg/data/cdroms/grddas00", "PATH_WRK",
            "/net/prod/www/mgg/dat/tmp/1/gtran", "FORMAT", "dfARCASCIIGRID",
            "BYTEORDER", "boLITTLEENDIAN", "XYZEMPTYCELLS", "ecINCLUDE",
            "XYZDELIM", "rdNONE", "PRECISION", "1", "NUMBERTYPE", "2", "NAN",
            "-32768", "GRIDRADIUS", "-1", "EXTENT", "deALL", "GRIDAREA",
            "TRUE", "OPSYSTEM", "Windows"
        };


        HTTPClient.NVPair[] opts = new HTTPClient.NVPair[args.length / 2];
        for (int i = 0; i < args.length; i += 2) {
            opts[i / 2] = new HTTPClient.NVPair(args[i], args[i + 1]);
        }
        URL url = new URL("http://www.ngdc.noaa.gov/cgi-bin/mgg/gd_gtran");
        CookieModule.setCookiePolicyHandler(null);
        HTTPConnection conn = new HTTPConnection(url.getHost(),
                                  url.getPort());

        HTTPResponse res  = conn.Post(url.getFile(), opts);
        String       html = new String(res.getData());
        boolean      ok   = !(res.getStatusCode() >= 300);
        if (ok) {
            ok = (html.indexOf("Success") >= 0);
        }
        if ( !ok) {
            System.err.println("bad:" + html);
            return;
        }

        url = new URL("http://www.ngdc.noaa.gov/mgg/tmp/1/gtran/" + key
                      + "_data/" + key + "/" + key + ".asc");
        byte[] ascBytes =
            IOUtil.readBytes(url.openConnection().getInputStream());
        System.err.println("read:" + ascBytes.length);
        IOUtil.writeBytes(new File("test.asc"), ascBytes);

        /**
         * url = new URL("http://www.ngdc.noaa.gov/cgi-bin/mgg/gd_rtvg");
         * opts = new HTTPClient.NVPair[]{
         * new HTTPClient.NVPair(  "KEY","12313131"),
         * new HTTPClient.NVPair("PATH_RTV","/net/prod/www/mgg/dat/tmp/1/gtran")
         * conn = new HTTPConnection(url.getHost(),url.getPort());
         * res  = conn.Post(url.getFile(), opts);
         *   ok = !(res.getStatusCode() >= 300);
         *   if (ok) {
         *       url = new URL("http://www.ngdc.noaa.gov/mgg/tmp/1/gtran/"+key+".zip");
         *       byte[]zipBytes = IOUtil.readBytes(url.openConnection().getInputStream());
         *       System.out.println ("read" + zipBytes.length);
         *       ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes));
         *       ZipEntry       ze  = null;
         *       while ((ze = zin.getNextEntry()) != null) {
         *           String name = ze.getName().toLowerCase();
         *           System.err.println ("name:" + name);
         *       }
         *       IOUtil.writeBytes(new File("test.zip"), zipBytes);
         *
         *
         *   } else {
         *       System.out.println ("bad zip");
         *   }
         *   }
         */


    }






}


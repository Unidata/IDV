/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for Atmospheric Research
 * Copyright 2010- Jeff McWhirter
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
 * 
 */

package ucar.unidata.gis.shapefile;


import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ogc.WKTParser;
import ucar.unidata.util.IOUtil;

import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.Util;

import visad.Unit;
import visad.VisADException;

import java.io.File;
import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;



/**
 * Class to read a shapefile .prj file
 */
public class ProjFile {

    /** the projection */
    private ProjectionImpl projection;

    /** the map lines type */
    private ProjectionCoordinateSystem projCS;

    /** the projection unit */
    private Unit projUnit;

    /**
     * Create a new Projfile from the URL
     *
     * @param url URL to the *.prj file
     *
     * @throws Exception problem reading or parsing the file
     */
    public ProjFile(URL url) throws Exception {
        URLConnection connection = url.openConnection();
        init(connection.getInputStream());
    }

    /**
     * Create a new Projfile from the location spec
     *  @param spec Location of the *.prj file, as either a URL or filename
     *
     * @throws Exception problem reading or parsing the file
     */
    public ProjFile(String spec) throws Exception {
        init(IOUtil.getInputStream(spec));
    }

    /**
     * Create a new Projfile from the location spec
     *  @param file A file object of the *.prj file.
     *
     * @throws Exception problem reading or parsing the file
     */
    public ProjFile(File file) throws Exception {
        this(file.getPath());
    }

    /**
     * Initialize the ProjFile from an input stream
     *
     * @param s the input stream
     *
     * @throws Exception problem reading or parsing the file
     */
    public ProjFile(InputStream s) throws Exception {
        init(s);
    }

    /**
     *  Process the file contents from the stream
     *  @param s  the input stream from the .prj file
     */
    public void init(InputStream s) {
        WKTParser parser = null;
        try {
            String projSpec = new String(IOUtil.readBytes(s, null, false));
            parser = new WKTParser(projSpec);
            if (parser.isPlanarProjection()) {
                projection = WKTParser.convertWKTToProjection(parser);
            }
        } catch (Exception e) {
            projection = null;
        }
        if ((projection != null)
                && !(projection
                     instanceof ucar.unidata.geoloc.projection
                         .LatLonProjection)) {
            try {
                projCS = new ProjectionCoordinateSystem(projection);
                if ( !parser.getProjUnitName().equals(null)) {
                    String unitString = parser.getProjUnitValue() + " "
                                        + parser.getProjUnitName();
                    Unit u = null;
                    try {
                        u = Util.parseUnit(unitString);
                    } catch (VisADException ve) {
                        u = null;
                    }
                    if (u != null) {
                        projUnit = u;
                    }
                }
            } catch (VisADException ve) {
                System.err.println("can't make projection cs");
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "projection:" + projection.getClass().getName();
    }


    /**
     * Get the projection defined by this file
     * @return the projection or null
     */
    public ProjectionImpl getProjection() {
        return projection;
    }

    /**
     * Convert the xy values to lat/lon
     *
     * @param xy  the xy
     *
     * @return the lat/lon values or xy if no projection
     */
    public double[][] convertToLonLat(double[][] xy) {
        if (projCS == null) {
            return xy;
        }
        double[][] latlon = xy;
        try {
            latlon = projCS.toReference(xy,
                                        new Unit[] { projUnit, projUnit });
        } catch (Exception e) {
            return xy;
        }
        return new double[][] {
            latlon[1], latlon[0]
        };
    }
}

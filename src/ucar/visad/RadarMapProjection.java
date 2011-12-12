/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.visad;


import edu.wisc.ssec.mcidas.*;

import org.w3c.dom.Element;

import ucar.unidata.util.Misc;
import ucar.unidata.xml.*;


import visad.*;

import visad.data.mcidas.AREACoordinateSystem;

import visad.georef.LatLonPoint;

import java.util.List;


/**
 * A map projection uses a McIDAS navigation.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.8 $ $Date: 2007/06/22 13:37:52 $
 */
public class RadarMapProjection extends AREACoordinateSystem implements XmlPersistable {

    /** default width of grid */
    private static final int DEFAULT_WIDTH = 520;

    /** default height of grid */
    private static final int DEFAULT_HEIGHT = 520;

    /** center latitude */
    private double lat;

    /** center longitude */
    private double lon;

    /** specified width */
    private int width;

    /** specified height */
    private int height;

    /**
     * Create a <code>RadarMapProjection</code> centered on the
     * lat/lon given.
     *
     * @param ll <code>LatLonPoint</code> for center point
     *
     * @throws VisADException  unable to create the projection
     */
    public RadarMapProjection(LatLonPoint ll) throws VisADException {
        this(ll.getLatitude().getValue(CommonUnit.degree),
             ll.getLongitude().getValue(CommonUnit.degree));
    }

    /**
     * Create a <code>RadarMapProjection</code> centered on the
     * lat/lon and size given
     *
     * @param ll <code>LatLonPoint</code> for center point
     * @param height height of the image (pixels)
     * @param width height of the image (pixels)
     *
     * @throws VisADException  unable to create the projection
     */
    public RadarMapProjection(LatLonPoint ll, int width, int height)
            throws VisADException {
        this(ll.getLatitude().getValue(CommonUnit.degree),
             ll.getLongitude().getValue(CommonUnit.degree), width, height);
    }

    /**
     * Create a <code>RadarMapProjection</code> centered on the
     * lat/lon given.  Use the default size.
     *
     * @param lat  latitude of the center point
     * @param lon  longitude of the center point
     *
     * @throws VisADException
     */
    public RadarMapProjection(double lat, double lon) throws VisADException {
        this(lat, lon, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }


    /**
     * Create a <code>RadarMapProjection</code> centered on the
     * lat/lon and size given.
     *
     * @param lat  latitude of the center point
     * @param lon  longitude of the center point
     * @param height height of the image (pixels)
     * @param width height of the image (pixels)
     * @throws VisADException  unable to create the projection
     */
    public RadarMapProjection(double lat, double lon, int width, int height)
            throws VisADException {
        this(lat, lon, width, height, 1);
    }

    /**
     * Create a <code>RadarMapProjection</code> centered on the
     * lat/lon and size given.
     *
     * @param lat  latitude of the center point
     * @param lon  longitude of the center point
     * @param height height of the image (pixels)
     * @param width height of the image (pixels)
     * @param res resolution of the pixels (km)
     * @throws VisADException  unable to create the projection
     */
    public RadarMapProjection(double lat, double lon, int width, int height,
                              int res)
            throws VisADException {
        super(makeDir(height, width), makeNav(lat, lon, height, width, res),
              null, false);
        this.lat    = lat;
        this.lon    = lon;
        this.width  = width;
        this.height = height;
    }

    /**
     * Create a radar navigation block from the info provided.
     *
     * @param lat   center latitude
     * @param lon   center longitude
     * @param lines number of lines
     * @param eles  number of elements
     * @param res _more_
     * @return
     */
    private static int[] makeNav(double lat, double lon, int lines, int eles,
                                 int res) {
        int[] nav = new int[128];
        nav[0] = AREAnav.RADR;
        nav[1] = lines / 2;
        nav[2] = eles / 2;
        nav[3] = McIDASUtil.doubleLatLonToInteger(lat);
        nav[4] = McIDASUtil.doubleLatLonToInteger(-lon);  // west pos
        nav[5] = 1000 * res;
        return nav;
    }

    /**
     * Create an image directory from the info provided.
     *
     * @param lines      number of lines
     * @param elements   number of elements
     * @return
     */
    private static int[] makeDir(int lines, int elements) {
        int[] dir = new int[64];
        dir[AreaFile.AD_VERSION]   = 4;         //dir[1]
        dir[AreaFile.AD_SENSORID]  = 7;         //dir[2]
        dir[AreaFile.AD_STLINE]    = 1;         //dir[5]
        dir[AreaFile.AD_STELEM]    = 1;         //dir[6]
        dir[AreaFile.AD_NUMLINES]  = lines;     //dir[8]
        dir[AreaFile.AD_NUMELEMS]  = elements;  //dir[9]
        dir[AreaFile.AD_DATAWIDTH] = 1;         //dir[10]
        dir[AreaFile.AD_LINERES]   = 1;         //dir[11]
        dir[AreaFile.AD_ELEMRES]   = 1;         //dir[12]
        dir[AreaFile.AD_NUMBANDS]  = 1;         //dir[13]
        return dir;
    }

    /**
     * Get a <code>String</code> representation of this projection.
     * @return  string
     */
    public String toString() {
        return "Radar Projection (" + Misc.format(lat) + " "
               + Misc.format(lon) + ")";
    }

    /**
     * Create the XML to represent this object.
     *
     * @param encoder  encoder to use
     *
     * @return Element that represents this object.
     */
    public Element createElement(XmlEncoder encoder) {
        List args = Misc.newList(new Double(lat), new Double(lon),
                                 new Integer(width), new Integer(height));
        List types = Misc.newList(Double.TYPE, Double.TYPE, Integer.TYPE,
                                  Integer.TYPE);
        Element result      = encoder.createObjectElement(getClass());
        Element ctorElement = encoder.createConstructorElement(args, types);
        result.appendChild(ctorElement);
        return result;
    }

    /**
     * Do nothing, return true to tell the encoder that it is ok to process
     * any methods or properties.
     *
     * @param  encoder  encoder to use
     * @param  node   node to process
     *
     * @return true
     */
    public boolean initFromXml(XmlEncoder encoder, Element node) {
        return true;
    }

}

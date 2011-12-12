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


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;


import visad.*;

import visad.bom.Radar3DCoordinateSystem;

import visad.georef.EarthLocation;
import visad.georef.NavigatedCoordinateSystem;


/**
 * Class to transform lon/lat/elevation angle to lon/lat/altitude
 *
 * @author MetApps Development Team
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:34:03 $
 */
public class RadarGridCoordinateSystem extends NavigatedCoordinateSystem {

    /** unist for the system */
    private static Unit[] units = { CommonUnit.degree, CommonUnit.degree,
                                    CommonUnit.degree };

    /** 3D coordinate systems for transformations */
    private Radar3DCoordinateSystem rcs = null;

    /** center point of projection */
    private LatLonPointImpl center = null;

    /** working lat/lon point for transformations */
    private LatLonPointImpl workLL = new LatLonPointImpl();

    /**
     * Construct a new RGCS with the center point
     * @param  radLocation  radar location (lat/lon/alt)
     * @throws VisADException couldn't create necessary VisAD objects
     */
    public RadarGridCoordinateSystem(EarthLocation radLocation)
            throws VisADException {
        this(radLocation.getLatitude().getValue(CommonUnit.degree),
             radLocation.getLongitude().getValue(CommonUnit.degree),
             radLocation.getAltitude().getValue(CommonUnit.meter));

    }

    /**
     * Construct a new RGCS with the center point
     * @param  lat  radar latitude
     * @param  lon  radar longitude
     * @param  alt  radar altitude
     * @throws VisADException couldn't create necessary VisAD objects
     */
    public RadarGridCoordinateSystem(double lat, double lon, double alt)
            throws VisADException {

        super(RealTupleType.SpatialEarth3DTuple, units);
        center = new LatLonPointImpl(lat, lon);
        rcs = new Radar3DCoordinateSystem((float) lat, (float) lon,
                                          (float) alt);
    }

    /**
     * Transform lon/lat/elevation angle to lat/lon/alt
     *
     * @param lonlatelev  lon/lat/elevation angle data
     * @return  data as lat/lon/alt
     *
     * @throws VisADException   bad array size or transformation problem
     */
    public double[][] toReference(double[][] lonlatelev)
            throws VisADException {
        int        numPoints = lonlatelev[0].length;
        double[][] latlonalt = new double[3][numPoints];
        for (int i = 0; i < numPoints; i++) {

            workLL.setLatitude(lonlatelev[1][i]);
            workLL.setLongitude(lonlatelev[0][i]);
            Bearing result = Bearing.calculateBearing(center, workLL, null);
            latlonalt[0][i] = result.getDistance() * 1000;
            latlonalt[1][i] = result.getAngle();
            latlonalt[2][i] = lonlatelev[2][i];
            // latlonalt should now be range/azimuth/elevation
        }
        latlonalt = rcs.toReference(latlonalt);
        return new double[][] {
            latlonalt[1], latlonalt[0], latlonalt[2]
        };
    }

    /**
     * Transform lat/lon/alt to lat/lon/elevation angle
     *
     * @param lonlatalt   lat/lon/alt values
     * @return  transformed lon/lat/elevation
     *
     * @throws VisADException   bad array size or transformation problem
     */
    public double[][] fromReference(double[][] lonlatalt)
            throws VisADException {
        int        numPoints  = lonlatalt[0].length;
        double[][] lonlatelev = rcs.fromReference(new double[][] {
            lonlatalt[1], lonlatalt[0], lonlatalt[2]
        });
        for (int i = 0; i < numPoints; i++) {
            lonlatelev[0][i] = lonlatalt[0][i];
            lonlatelev[1][i] = lonlatalt[1][i];
        }
        return lonlatelev;
    }

    /**
     * Transform lon/lat/elevation angle to lat/lon/alt
     *
     * @param lonlatelev  lon/lat/elevation angle data
     * @return  data as lat/lon/alt
     *
     * @throws VisADException   bad array size or transformation problem
     */
    public float[][] toReference(float[][] lonlatelev) throws VisADException {
        int       numPoints = lonlatelev[0].length;
        float[][] latlonalt = new float[3][numPoints];
        for (int i = 0; i < numPoints; i++) {

            workLL.setLatitude(lonlatelev[1][i]);
            workLL.setLongitude(lonlatelev[0][i]);
            Bearing result = Bearing.calculateBearing(center, workLL, null);
            latlonalt[0][i] = (float) (result.getDistance() * 1000.);
            latlonalt[1][i] = (float) result.getAngle();
            latlonalt[2][i] = (float) lonlatelev[2][i];
            // latlonalt should now be range/azimuth/elevation
        }
        latlonalt = rcs.toReference(latlonalt);
        return new float[][] {
            latlonalt[1], latlonalt[0], latlonalt[2]
        };
    }

    /**
     * Transform lat/lon/alt to lat/lon/elevation angle
     *
     * @param lonlatalt   lat/lon/alt values
     * @return  transformed lon/lat/elevation
     *
     * @throws VisADException   bad array size or transformation problem
     */
    public float[][] fromReference(float[][] lonlatalt)
            throws VisADException {
        int       numPoints  = lonlatalt[0].length;
        float[][] lonlatelev = rcs.fromReference(new float[][] {
            lonlatalt[1], lonlatalt[0], lonlatalt[2]
        });
        for (int i = 0; i < numPoints; i++) {
            lonlatelev[0][i] = lonlatalt[0][i];
            lonlatelev[1][i] = lonlatalt[1][i];
        }
        return lonlatelev;
    }

    /**
     * Check to see if the object in question is equal to this.
     *
     * @param obj  object in question
     * @return  true if they are equal
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof RadarGridCoordinateSystem)) {
            return false;
        }
        RadarGridCoordinateSystem that = (RadarGridCoordinateSystem) obj;
        return (this == that) || center.equals(that.center);
    }
}

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


import geotransform.coords.*;

import geotransform.ellipsoids.*;

import geotransform.transforms.*;

import org.w3c.dom.Element;



import ucar.unidata.geoloc.*;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.*;

import visad.*;

import visad.georef.MapProjection;

import java.awt.geom.Rectangle2D;

import java.util.List;


/**
 * Transforms between UTM coordinates and lat/lon in degrees.
 * Limitation: all elevations are treated as 0.0.
 *
 * The VisAD CoordinateSystem "Reference" is lat/lon
 * (RealTupleType.LatitudeLongitudeTuple).
 *
 * Uses the SRI geotransform Java package for converters, ellipsoids, etc.
 *
 * Each UTM position requires a complete specification including an x,y position
 * in meters, a zone number, a hemisphere boolean flag (true in northern),
 * and a reference ellipsoid.
 *
 * @see <a href="http://www.ai.sri.com/geotransform/">
 *      http://www.ai.sri.com/geotransform/</a>
 * @version $Revision: 1.18 $ $Date: 2005/05/13 18:34:06 $
 */
public class UTMCoordinateSystem extends MapProjection implements XmlPersistable {

    // specification of UTM system in use:

    /** ellipsoid being used */
    private Ellipsoid ellipsoid;

    /** the UTM zone numbers (1..60) per position */
    private int zone[];

    /** zone for CS */
    private int onezone;

    /** array of hemisphere flags */
    private boolean hemisphere_north[];  // true  in the northern hemisphere

    /** hemisphere flag for CS */
    private boolean onehemiflag;

    /** starting x point for the zone */
    private double startX = 0;

    /** starting y point of the zone */
    private double startY = 0;

    /** width of the zone */
    private double width = 10E6;

    /** height of the zone */
    private double height = 10E6;

    /**
     * AA = Airy 1830 Ellipsoid
     */
    public static final Ellipsoid AA = new AA_Ellipsoid();

    /**
     * AM = Modified Airy Ellipsoid
     */
    public static final Ellipsoid AM = new AM_Ellipsoid();

    /**
     * AN = Australian National Ellipsoid
     */
    public static final Ellipsoid AN = new AN_Ellipsoid();

    /**
     * BN = Bessel 1841 (Namibia) Ellipsoid
     */
    public static final Ellipsoid BN = new BN_Ellipsoid();

    /**
     * BR = Bessel 1841 (Ethiopia Indonesia Japan Korea)
     */
    public static final Ellipsoid BR = new BR_Ellipsoid();

    /**
     * CC = Clarke 1866 Ellipsoid
     */
    public static final Ellipsoid CC = new CC_Ellipsoid();

    /**
     * CD = Clarke 1880 Ellipsoid
     */
    public static final Ellipsoid CD = new CD_Ellipsoid();

    /**
     * EA = Everest (India 1830) Ellipsoid
     */
    public static final Ellipsoid EA = new EA_Ellipsoid();

    /**
     * EB = Everest (Sabah & Sarawak) Ellipsoid
     */
    public static final Ellipsoid EB = new EB_Ellipsoid();

    /**
     * EC = Everest (India 1956) Ellipsoid
     */
    public static final Ellipsoid EC = new EC_Ellipsoid();

    /**
     * ED = Everest (West Malaysia 1969) Ellipsoid
     */
    public static final Ellipsoid ED = new ED_Ellipsoid();

    /**
     * EE = Everest (West Malaysia & Singapore 1948)
     */
    public static final Ellipsoid EE = new EE_Ellipsoid();

    /**
     * FA = Modified Fischer 1960 Ellipsoid
     */
    public static final Ellipsoid FA = new FA_Ellipsoid();

    /**
     * HE = Helmert 1906 Ellipsoid
     */
    public static final Ellipsoid HE = new HE_Ellipsoid();

    /**
     * HO = Hough 1960 Ellipsoid
     */
    public static final Ellipsoid HO = new HO_Ellipsoid();

    /**
     * IN = International 1924 Ellipsoid
     */
    public static final Ellipsoid IN = new IN_Ellipsoid();

    /**
     * KA = Krassovsky 1940 Ellipsoid
     */
    public static final Ellipsoid KA = new KA_Ellipsoid();

    /**
     * RF = Geodetic Reference System 1980 (GRS 80)
     */
    public static final Ellipsoid RF = new RF_Ellipsoid();

    /**
     * SA = South American 1969 Ellipsoid
     */
    public static final Ellipsoid SA = new SA_Ellipsoid();

    /**
     * WD = WGS 72 Ellipsoid
     */
    public static final Ellipsoid WD = new WD_Ellipsoid();

    /**
     * WE = WGS 84 Ellipsoid
     */
    public static final Ellipsoid WE = new WE_Ellipsoid();


    /**
     * Constructs an instance of a UTM coordinate transform with the
     * supplied Ellipsoid, zone and hemisphere.
     * The ellipsoid should be the one used as a basis for the UTM coordinates.
     * The zone is the UTM zone which has positions to be converted;
     * hemiflag is a boolean, true if points are northern hemisphere.
     * You can convert points from more than one zone and hemisphere
     * by using the method ConvertUtmToLatLon.
     *
     * The reference coordinate system is RealTupleType.LatitudeLongitudeTuple;
     * the incoming units are assumed to be UTM coords based on the
     * input ellipsoid.
     *
     * Most USGS topographic maps use the 1927 North American Datum (NAD 27);
     * new maps are being slowly revised to NAD 83.
     * To construct Ellipsoids for the first argument,
     * import geotransform.jar, and do new CC_Ellipsoid()
     * for NAD 27 (Clark 1866 ellipsoid), or
     * new RF_Ellipsoid()  for NAD 83 (GRS 80 ellipsoid), or
     * new WE_Ellipsoid()  for WSG 84.
     * See http://www.ai.sri.com/geotransform/api.html for more details about
     * 239 supported datums.
     *
     * @param ellipsoid the basis for some UTM coordinate system;
     *                  many choices possible
     * @param zone      the UTM zone which has positions to be converted
     * @param hemiflag  a boolean, true if points are in the northern hemisphere
     *
     * @throws VisADException on badness. Throws a NullPointerException if the ellipsoid is <code>null</code>.
     */
    public UTMCoordinateSystem(Ellipsoid ellipsoid, int zone,
                               boolean hemiflag)
            throws VisADException {
        this(ellipsoid, zone, hemiflag, null);
    }

    /**
     * Constructs an instance of a UTM coordinate transform with the
     * supplied Ellipsoid, zone and hemisphere.
     * The ellipsoid should be the one used as a basis for the UTM coordinates.
     * The zone is the UTM zone which has positions to be converted;
     * hemiflag is a boolean, true if points are northern hemisphere.
     * You can convert points from more than one zone and hemisphere
     * by using the method ConvertUtmToLatLon.
     *
     * The reference coordinate system is RealTupleType.LatitudeLongitudeTuple;
     * the incoming units are assumed to be UTM coords based on the
     * input ellipsoid.
     *
     * Most USGS topographic maps use the 1927 North American Datum (NAD 27);
     * new maps are being slowly revised to NAD 83.
     * To construct Ellipsoids for the first argument,
     * import geotransform.jar, and do new CC_Ellipsoid()
     * for NAD 27 (Clark 1866 ellipsoid), or
     * new RF_Ellipsoid()  for NAD 83 (GRS 80 ellipsoid), or
     * new WE_Ellipsoid()  for WSG 84.
     * See http://www.ai.sri.com/geotransform/api.html for more details about
     * 239 supported datums.
     *
     * @param ellipsoid the basis for some UTM coordinate system;
     *                  many choices possible
     * @param zone      the UTM zone which has positions to be converted
     * @param bounds    Linear2DSet describing the bounds of this
     *                  MapProjection
     * @param hemiflag  a boolean, true if points are in the northern hemisphere
     *
     * @throws VisADException
     */
    public UTMCoordinateSystem(Ellipsoid ellipsoid, int zone,
                               boolean hemiflag, Rectangle2D bounds)
            throws VisADException {

        super(RealTupleType.LatitudeLongitudeTuple,
              new Unit[] { CommonUnit.meter,
                           CommonUnit.meter });

        if (ellipsoid == null) {
            throw new NullPointerException();
        }

        if ((zone < 1) || (zone > 60)) {
            throw new IllegalArgumentException(
                "UTM zone number not in range 1-60");
        }

        if (bounds != null) {
            startX = bounds.getX();
            startY = bounds.getY();
            width  = bounds.getWidth();
            height = bounds.getHeight();
        }

        this.ellipsoid   = ellipsoid;
        this.onezone     = zone;
        this.onehemiflag = hemiflag;

        // initialize the converters
        Utm_To_Gdc_Converter.Init(ellipsoid);
        Gdc_To_Utm_Converter.Init(ellipsoid);
    }

    /**
     * Constructs an instance using the WSG 84 ellipsoid,
     * and given zone and hemisphere.
     * This ellipsoid should be the one used as a basis for the UTM coordinates.
     * If you don't know the ellipsoid for your UTM values you will get
     * approximately correct conversions using this cstr. "A mismatch between
     * datums on your map and GPS receiver can cause errors of
     * several hundred meters."
     * @param zone the UTM zone which has positions to be converted
     * @param hemiflag a boolean, true if points are in the northern hemisphere
     *
     * @throws VisADException
     */
    public UTMCoordinateSystem(int zone, boolean hemiflag)
            throws VisADException {
        this(new WE_Ellipsoid(), zone, hemiflag);
    }


    /**
     *  Print elllipsoid mean radius and inverse flattening
     * @return string version of this
     */
    public String toString() {
        return new String("   UTM CS with ellipsoid of mean radius a = "
                          + ellipsoid.a + "  1/flattening = " + ellipsoid.f);
    }

    /**
     * Check for equality of input object to this UTMCoordinateSystem.
     * @param  obj  other object in question
     * @return  true if the object in question is a UTMCoordinateSystem
     *          and it's ellipsoid, zone, & hemisphere are equal
     *          to this object's values.
     */
    public boolean equals(Object obj) {

        if ( !(obj instanceof UTMCoordinateSystem)) {
            return false;
        }

        UTMCoordinateSystem that = (UTMCoordinateSystem) obj;

        boolean             test = false;
        if ((that.getHemisphereFlag() == onehemiflag)
                && (that.getZone() == onezone)
                && that.ellipsoid.equals(ellipsoid)) {
            test = true;
        }

        return test;
    }


    /**
     * Convert an array of UTM positions to lat/lon. All elevations zero.
     * The UTM positions may be in more than one zone or one hemisphere.
     * Each UTM position has an x,y pair, a zone number, and a hemisphere
     * boolean flag.
     * Lengths of all three arrays should be the same, the number of
     * positions to convert., or both "zone" and "hemisphere_north" may
     * both have one value for the case where all UTM positions are
     * in the same zone.
     *
     * @param utmcoords array with UTM x,y values;
     *                  x=utmcoords[0][i], y=utmcoords[1][i]
     * @param zone      array of the UTM zones for each of these positions
     * @param hemisphere_north array of the UTM flags for each of these
     *                         positions true if the UTM position is in
     *                         the northern hemisphere
     * @return a double[][] array of lat/lon; lat=result[0][i], lon=result[1][i]
     * @throws VisADException
     */
    public double[][] ConvertUtmToLatLon(double[][] utmcoords, int zone[],
                                         boolean hemisphere_north[])
            throws VisADException {

        //this.zone = zone;
        //this.hemisphere_north = hemisphere_north;

        int MAX_POINTS = (utmcoords[0].length);
        // set working array sizes
        Gdc_Coord_3d gdc[] = new Gdc_Coord_3d[MAX_POINTS];
        Utm_Coord_3d utm[] = new Utm_Coord_3d[MAX_POINTS];

        if ((zone.length == 1) && (hemisphere_north.length == 1))
        // case of single zone and hemisphere
        {
            for (int i = 0; i < MAX_POINTS; i++) {
                gdc[i] = new Gdc_Coord_3d();
                utm[i] = new Utm_Coord_3d(utmcoords[0][i], utmcoords[1][i],
                                          0.0, (byte) (zone[0]),
                                          hemisphere_north[0]);
            }
        } else if ((zone.length == MAX_POINTS)
                   && (hemisphere_north.length == MAX_POINTS))
        // usual general purpose case
        {
            for (int i = 0; i < MAX_POINTS; i++) {
                gdc[i] = new Gdc_Coord_3d();
                utm[i] = new Utm_Coord_3d(utmcoords[0][i], utmcoords[1][i],
                                          0.0, (byte) (zone[i]),
                                          hemisphere_north[i]);
            }
        } else {
            System.out.println(
                " size of input zone and hemi arrays does not match number of positions");
            double[][] dummy = {
                { 0.0 }, { 0.0 }
            };
            return dummy;
        }

        // initialize the converter
        //Utm_To_Gdc_Converter.Init(getEllip());

        // convert the positions
        Utm_To_Gdc_Converter.Convert(utm, gdc);

        // switch to double [][] array
        double[][] result = new double[2][MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            result[0][i] = gdc[i].latitude;
            result[1][i] = gdc[i].longitude;
        }

        return result;
    }

    /**
     * Convert an array of UTM positions to lat/lon.
     * The UTM positions are all assumed to be in the given zone
     * and hemisphere args.
     *
     * Each UTM position has an x,y pair, a zone number, and a
     * hemisphere boolean flag.
     * Treats all positions as at zero elevation.
     *
     * @param utmcoords array with UTM x,y values;
     *                  x=utmcoords[0][i], y=utmcoords[1][i]
     * @param zone      the single UTM zone for all of these positions
     * @param hemisphere_north the hemisphere flag for all of
     *                         these positions (true=N)
     * @return a double [][] array of lat/lon; lat=result[0][i], lon=result[1][i]
     * @throws VisADException
     */
    public double[][] ConvertUtmToLatLon(double[][] utmcoords, int zone,
                                         boolean hemisphere_north)
            throws VisADException {

        if ((zone < 1) || (zone > 60)) {
            throw new IllegalArgumentException(
                "UTM zone number not in range 1-60");
        }

        return ConvertUtmToLatLon(utmcoords, new int[] { zone },
                                  new boolean[] { hemisphere_north });
    }

    /**
     * Convert an array of UTM positions to lat/lon.
     * The UTM positions are all assumed have the zone and
     * hemisphere used in the cstr.
     *
     * Each UTM position has an x,y pair, a zone number,
     * and a hemisphere boolean flag.
     * Treats all positions as at zero elevation.
     *
     * @param utmcoords array with UTM x,y values; x=utmcoords[0][i],
     *                  y=utmcoords[1][i]
     * @return a double [][] array of lat/lon; lat=result[0][i], lon=result[1][i]
     * @throws VisADException
     */
    public double[][] toReference(double[][] utmcoords)
            throws VisADException {

        // initialize the coordinates in geotransform arrays
        int MAX_POINTS = (utmcoords[0].length);

        Gdc_Coord_3d gdc[] = new Gdc_Coord_3d[MAX_POINTS];  // these need to be the same len
        Utm_Coord_3d utm[] = new Utm_Coord_3d[MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            gdc[i] = new Gdc_Coord_3d();
            utm[i] = new Utm_Coord_3d(utmcoords[0][i], utmcoords[1][i], 0.0,
                                      (byte) onezone, onehemiflag);
        }

        // convert the positions
        Utm_To_Gdc_Converter.Convert(utm, gdc);

        // switch to double [][] array
        double[][] result = new double[2][MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            result[0][i] = gdc[i].latitude;
            result[1][i] = gdc[i].longitude;
        }

        return result;
    }

    /**
     * Convert an array of UTM positions to lat/lon.
     * The UTM positions are all assumed have the zone and
     * hemisphere used in the cstr.
     *
     * Each UTM position has an x,y pair, a zone number,
     * and a hemisphere boolean flag.
     * Treats all positions as at zero elevation.
     *
     * @param utmcoords array with UTM x,y values; x=utmcoords[0][i],
     *                  y=utmcoords[1][i]
     * @return a double [][] array of lat/lon; lat=result[0][i], lon=result[1][i]
     * @throws VisADException
     */
    public float[][] toReference(float[][] utmcoords) throws VisADException {

        // initialize the coordinates in geotransform arrays
        int          MAX_POINTS = (utmcoords[0].length);

        Gdc_Coord_3d gdc[]      = new Gdc_Coord_3d[MAX_POINTS];
        Utm_Coord_3d utm[]      = new Utm_Coord_3d[MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            gdc[i] = new Gdc_Coord_3d();
            utm[i] = new Utm_Coord_3d(utmcoords[0][i], utmcoords[1][i], 0.0,
                                      (byte) onezone, onehemiflag);
        }

        // convert the positions
        Utm_To_Gdc_Converter.Convert(utm, gdc);

        // switch to double [][] array
        float[][] result = new float[2][MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            result[0][i] = (float) gdc[i].latitude;
            result[1][i] = (float) gdc[i].longitude;
        }

        return result;
    }


    /**
     * Convert from lat/lon (GDC) coordinates to UTM coords. Note this
     * finds UTM x/y
     * and corresponding UTM zones (1...60)  - use method getZoneNumbers(),
     * and hemisphere flags == true if north - use method getHemisphereFlags().
     * All positions' elevations are set to zero.
     * @param   latlon lat/lon values (lat = latlon[0][i], lon=latlon[1][i])
     * @return  UTM coordinates (x = result[0][i], y=result[1][i])
     * @throws  VisADException  unable to make transformation
     */
    public double[][] fromReference(double[][] latlon) throws VisADException {

        // initialize the coordinates in geotransform arrays
        int MAX_POINTS = (latlon[0].length);

        // set array sizes
        zone             = new int[MAX_POINTS];
        hemisphere_north = new boolean[MAX_POINTS];
        Gdc_Coord_3d gdc[] = new Gdc_Coord_3d[MAX_POINTS];  // these need to be the same len
        Utm_Coord_3d utm[] = new Utm_Coord_3d[MAX_POINTS];

        // move lat lon to right kind of object for geotransform menthods
        //use cstr Gdc_Coord_3d( double lat, double lon, double elev )
        for (int i = 0; i < MAX_POINTS; i++) {
            gdc[i] = new Gdc_Coord_3d(latlon[0][i], latlon[1][i], 0.0);
            utm[i] = new Utm_Coord_3d();
        }

        // convert the positions
        Gdc_To_Utm_Converter.Convert(gdc, utm);

        // switch utm coords back to double [][] array
        double[][] result = new double[2][MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            Utm_Coord_3d utmCoord = utm[i];
            result[0][i] =
            //TODO:  Can we convert from one UTM zone to another?
            //(float) ((utmCoord.zone - onezone)*10E6 + utmCoord.x);
            (utmCoord.zone != onezone)
            ? Double.NaN
            : utmCoord.x;
            result[1][i]             = (utmCoord.zone != onezone)
                                       ? Double.NaN
                                       : utmCoord.y;
            this.zone[i]             = utmCoord.zone;
            this.hemisphere_north[i] = utmCoord.hemisphere_north;
        }

        return result;
    }

    /**
     * Convert from lat/lon (GDC) coordinates to UTM coords. Note this
     * finds UTM x/y
     * and corresponding UTM zones (1...60)  - use method getZoneNumbers(),
     * and hemisphere flags == true if north - use method getHemisphereFlags().
     * All positions' elevations are set to zero.
     * @param   latlon lat/lon values (lat = latlon[0][i], lon=latlon[1][i])
     * @return  UTM coordinates (x = result[0][i], y=result[1][i])
     * @throws  VisADException  unable to make transformation
     */
    public float[][] fromReference(float[][] latlon) throws VisADException {

        // initialize the coordinates in geotransform arrays
        int MAX_POINTS = (latlon[0].length);

        // set array sizes
        zone             = new int[MAX_POINTS];
        hemisphere_north = new boolean[MAX_POINTS];
        Gdc_Coord_3d gdc[] = new Gdc_Coord_3d[MAX_POINTS];  // these need to be the same len
        Utm_Coord_3d utm[] = new Utm_Coord_3d[MAX_POINTS];

        // move lat lon to right kind of object for geotransform menthods
        //use cstr Gdc_Coord_3d( double lat, double lon, double elev )
        for (int i = 0; i < MAX_POINTS; i++) {
            gdc[i] = new Gdc_Coord_3d(latlon[0][i], latlon[1][i], 0.0);
            utm[i] = new Utm_Coord_3d();
        }

        // convert the positions
        Gdc_To_Utm_Converter.Convert(gdc, utm);

        // switch utm coords back to double [][] array
        float[][] result = new float[2][MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            Utm_Coord_3d utmCoord = utm[i];
            result[0][i] =
            //TODO:  Can we convert from one UTM zone to another?
            //(float) ((utmCoord.zone - onezone)*10E6 + utmCoord.x);
            (utmCoord.zone != onezone)
            ? Float.NaN
            : (float) utmCoord.x;
            result[1][i]             = (utmCoord.zone != onezone)
                                       ? Float.NaN
                                       : (float) utmCoord.y;
            this.zone[i]             = utmCoord.zone;
            this.hemisphere_north[i] = utmCoord.hemisphere_north;
        }

        return result;
    }


    /**
     * get UTM zone numbers (1...60) for the UTM positions.
     * @return  array of zone numbers for values
     */
    public int[] getZoneNumbers() {
        return zone;
    }

    /**
     * get array of booleans: if a UTM position is in the northern hemisphere, true.
     * @return  array of hemisphere flags for values
     */
    public boolean[] getHemisphereFlags() {
        return hemisphere_north;
    }

    /**
     * get single UTM zone number (1...60) for the UTM positions.
     * Use this only to check constructor argument.
     * @return  get the UTM zone for the positions
     */
    public int getZone() {
        return onezone;
    }

    /**
     * get boolean flag: if UTM positions in the northern hemisphere, true.
     * Use this only to check constructor argument.
     * @return  UTM hemisphere flag for this CS
     */
    public boolean getHemisphereFlag() {
        return onehemiflag;
    }


    /**
     * Get a bounding box in this coordinate system.
     * Return an x-y rectangle that covers most of a  UTM zone, in meters.
     *
     * @return a default MapArea of a UTM zone in UTM x-y meters.
     */
    public Rectangle2D getDefaultMapArea() {
        return new Rectangle2D.Double(startX, startY, width, height);
    }

    /**
     * needed for XmlPersistable
     *
     * @param encoder  encoder for encoding
     * @return  this as an encoded Element
     */
    public Element createElement(XmlEncoder encoder) {
        List arguments = Misc.newList(ellipsoid, new Integer(onezone),
                                      new Boolean(onehemiflag),
                                      getDefaultMapArea());
        List types = Misc.newList(ellipsoid.getClass(), Integer.TYPE,
                                  Boolean.TYPE, Rectangle2D.Double.class);
        Element result = encoder.createObjectElement(getClass());
        Element ctorElement = encoder.createConstructorElement(arguments,
                                  types);
        result.appendChild(ctorElement);
        return result;
    }

    /**
     * Do nothing, return true to tell the encoder that it is ok to process
     * any methods or properties. This is needed for XmlPersistable
     *
     * @param encoder  encoder for encoding
     * @param node  node to initialize from
     * @return true
     */
    public boolean initFromXml(XmlEncoder encoder, Element node) {
        return true;
    }

}

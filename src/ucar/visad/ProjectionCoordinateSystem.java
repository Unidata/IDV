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


import org.w3c.dom.Element;



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.xml.*;

import visad.*;

import visad.georef.MapProjection;

import java.util.ArrayList;


/**
 * Adapts a ucar.unidata.Projection into a VisAD MapProjection CoordinateSystem.
 * Transforms between world coordinates (x,y) in km and lat/lon in degrees.
 * Reference is lat/lon (RealTupleType.LatitudeLongitudeTuple)
 *
 * @see ucar.unidata.geoloc.Projection
 * @see visad.georef.MapProjection
 * @author Don Murray
 * @version $Revision: 1.25 $ $Date: 2006/08/18 17:28:41 $
 */
public class ProjectionCoordinateSystem extends MapProjection implements XmlPersistable {

    /** projection for transformations */
    private ProjectionImpl projection;

    /** working world point for transformations */
    private ProjectionPointImpl workP = new ProjectionPointImpl();

    /** working latlon point for transformations */
    private LatLonPointImpl workL = new LatLonPointImpl();

    /**
     * Constructs an instance from the supplied Projection. The
     * reference coordinate system is RealTupleType.LatitudeLongitudeTuple;
     * the incoming units are assumed to be km (1000 m).
     *
     * @param projection  projection to adapt
     *
     *
     * @throws VisADException
     */
    public ProjectionCoordinateSystem(ProjectionImpl projection)
            throws VisADException {

        this(projection, new Unit[] { CommonUnit.meter.scale(1000.0),
                                      CommonUnit.meter.scale(1000.0) });
    }

    /**
     * Constructs an instance from the supplied Projection. The
     * reference coordinate system is RealTupleType.LatitudeLongitudeTuple;
     * the incoming units are assumed to be km (1000 m).
     *
     * @param projection  projection to adapt
     * @param units units to use
     *
     *
     * @throws VisADException
     */
    public ProjectionCoordinateSystem(ProjectionImpl projection, Unit[] units)
            throws VisADException {

        super(RealTupleType.LatitudeLongitudeTuple, units);

        if (projection == null) {
            throw new NullPointerException();
        }

        this.projection = projection;
        /*
        System.out.println(projection.getName());
        System.out.println(projection.getClassName());
        System.out.println(projection.paramsToString());
        System.out.println(projection.getDefaultMapArea());
        */

    }

    /**
     * Override the parent toString method to use the
     * contained ProjectionImpl's name
     *
     * @return string representation
     */
    public String toString() {
        return projection.getName().equals("")
               ? projection.getClass().toString()
               : projection.getName();
    }


    /**
     * Get a reasonable bounding box in this coordinate system. MapProjections
     * are typically specific to an area of the world; there's no bounding
     * box that works for all projections so each subclass must implement
     * this method.
     *
     * @return the default MapArea of the Projection
     */
    public java.awt.geom.Rectangle2D getDefaultMapArea() {
        return (java.awt.geom.Rectangle2D) projection.getDefaultMapArea();
    }

    /**
     * Get the Projection used for the transformations.
     *
     * @return projection
     */
    public ProjectionImpl getProjection() {
        return projection;
    }

    /** _more_          */
    public static boolean debug = false;

    /**
     * _more_
     *
     * @param what _more_
     * @param array _more_
     * @param t1 _more_
     * @param t2 _more_
     */
    private void debug(String what, double[][] array, long t1, long t2) {
        if (debug && (array[0].length > 100)) {
            System.err.println(what + " projection:"
                               + projection.getClass().getName() + " time:"
                               + (t2 - t1) + " size:" + array[0].length);
            ucar.unidata.util.Misc.printStack(what);
        }
    }

    /**
     * _more_
     *
     * @param what _more_
     * @param array _more_
     * @param t1 _more_
     * @param t2 _more_
     */
    private void debug(String what, float[][] array, long t1, long t2) {
        if (debug && (array[0].length > 100)) {
            //      System.err.println(what +" projection:" + projection.getClass().getName() + " time:" + (t2-t1) +" size:" + array[0].length);
            ucar.unidata.util.Misc.printStack(what + " projection:"
                    + projection.getClass().getName() + " time:" + (t2 - t1)
                    + " size:" + array[0].length);
        }
    }


    /**
     * Convert world coordinates to lat/lon.  Input coords are in km.
     *
     * @param  world   world projection coordinates (x = world[0][i])
     *
     * @return corresponding lat/lon values (lat = latlon[0][i])
     *
     * @throws VisADException  world coordinate array length != 2
     */
    public double[][] toReference(double[][] world) throws VisADException {
        long       t1     = System.currentTimeMillis();
        double[][] result = projection.projToLatLon(world, world);
        long       t2     = System.currentTimeMillis();
        debug("toReference(double)", result, t1, t2);
        return result;
    }


    /**
     * Convert lat/lon coordinates to world (projection) coords.
     *
     * @param latlon  lat/lon values (lat = latlon[0][i])
     *
     * @return  world projection coordinates (x = world[0][i])
     * @throws  VisADException  latlon coordinate array length != 2
     */

    public double[][] fromReference(double[][] latlon) throws VisADException {
        long t1 = System.currentTimeMillis();
        double[][] result = projection.latLonToProj(latlon, latlon,
                                getLatitudeIndex(), getLongitudeIndex());
        long t2 = System.currentTimeMillis();
        debug("fromReference(double)", result, t1, t2);
        return result;
    }



    /**
     * Convert world coordinates to lat/lon.  Input coords are in km.
     *
     * @param  world   world projection coordinates (x = world[0][i])
     *
     * @return corresponding lat/lon values (lat = latlon[0][i])
     *
     * @throws VisADException  world coordinate array length != 2
     */
    public float[][] toReference(float[][] world) throws VisADException {
        long      t1     = System.currentTimeMillis();
        float[][] result = projection.projToLatLon(world, world);
        long      t2     = System.currentTimeMillis();
        debug("toReference(float)", result, t1, t2);
        return result;
    }


    /**
     * Convert lat/lon coordinates to world (projection) coords.
     *
     * @param latlon    lat/lon values (lat = latlon[0][i])
     *
     * @return  world projection coordinates (x = world[0][i])
     *
     * @throws  VisADException  latlon coordinate array length != 2
     */

    public float[][] fromReference(float[][] latlon) throws VisADException {
        ucar.unidata.util.Trace.msg(
            "ProjectionCoordinateSystem.fromReference "
            + projection.getClass().getName());

        long t1 = System.currentTimeMillis();
        float[][] result = projection.latLonToProj(latlon, latlon,
                               getLatitudeIndex(), getLongitudeIndex());
        long t2 = System.currentTimeMillis();
        debug("fromReference(float)", result, t1, t2);
        return result;
    }


    /**
     * Check for equality of CoordinateSystem objects
     *
     * @param  obj  other object in question
     *
     * @return  true if the object in question is a ProjectionCoordinateSystem
     *          and it's Projection is equal the this object's Projection
     */
    public boolean equals(Object obj) {

        if ( !(obj instanceof ProjectionCoordinateSystem)) {
            return false;
        }

        ProjectionCoordinateSystem that = (ProjectionCoordinateSystem) obj;

        return that.projection.equals(projection);
    }

    /**
     * Create the XML to represent this object.
     *
     * @param encoder  encoder to use
     *
     * @return Element that represents this object.
     */
    public Element createElement(XmlEncoder encoder) {
        ArrayList args = new ArrayList(1);
        args.add(projection);
        args.add(getCoordinateSystemUnits());
        Element result      = encoder.createObjectElement(getClass());
        Element ctorElement = encoder.createConstructorElement(args);
        result.appendChild(ctorElement);
        return result;
    }

    /**
     * Do nothing, return true to tell the encoder that it is ok to process
     * any methods or properties.
     *
     * @param  encoder  encoder to use
     *
     * @param  node   node to process
     *
     * @return true
     */
    public boolean initFromXml(XmlEncoder encoder, Element node) {
        return true;
    }
}

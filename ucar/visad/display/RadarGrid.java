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

package ucar.visad.display;


import visad.*;

import visad.bom.Radar2DCoordinateSystem;

import visad.georef.LatLonPoint;

import java.awt.Color;
import java.awt.Font;

import java.rmi.RemoteException;


/**
 * A class to support a standard radar grid display - range rings,
 * radials and labels.
 *
 * @author  IDV Development Team
 * @version $Revision: 1.25 $
 */
public class RadarGrid extends CompositeDisplayable {

    /** Dislayable for range rings */
    private RingSet rangeRings;

    /** Dislayable for radials */
    private Radials radials;

    /** Dislayable for labels */
    private RingLabels labels;

    /** Type for Range */
    private final RealType rangeType = RealType.getRealType("Range",
                                           CommonUnit.meter);

    /** Type for Azimuth */
    private final RealType azimuthType = RealType.getRealType("Azimuth",
                                             CommonUnit.degree);

    /** RealTupleType with CS to lat/lon */
    private RealTupleType rtt;

    /** range ring params - spacing, max, radial increment */
    private double rrSpacing, rrMax, radialInc;

    /** center latitude and longitude */
    private double center_lat, center_lon;

    /** label increment */
    private double label_inc;

    /** label size */
    private float labelSize = 1.f;

    /** color for the grid */
    private Color gridColor;

    /** color for the radial lines */
    private Color lineColor;

    /** color for the range rings */
    private Color ringColor;

    /** color for the labels */
    private Color labelColor;

    /** width for labels */
    private float labelWidth = 1.f;

    /** width for range rings */
    private float rangeRingWidth = 1.f;

    /** width for range rings */
    private float radialWidth = 1.f;

    /**
     * Construct a RadarGrid centered at llp.
     *
     * @param llp  center point
     * @param color  color for the grid
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException  remote error
     */
    public RadarGrid(LatLonPoint llp, Color color)
            throws VisADException, RemoteException {
        this(llp.getLatitude().getValue(CommonUnit.degree),
             llp.getLongitude().getValue(CommonUnit.degree), color);
    }

    /**
     * Construct a RadarGrid centered at specified lat/lon
     *
     * @param lat  center point latitude (degrees)
     * @param lon  center point longitude (degrees)
     * @param color  color for the grid
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException  remote error
     */
    public RadarGrid(double lat, double lon, Color color)
            throws VisADException, RemoteException {

        // initial values of range ring separation, range ring max distance,
        // and radials angular separation.
        rrSpacing  = 50.0;           // km
        rrMax      = 200.0;          // km
        radialInc  = 30.0;           // degrees
        label_inc  = 2 * rrSpacing;  // label spacing
        gridColor  = color;
        lineColor  = color;
        ringColor  = color;
        labelColor = color;

        setCenterPoint(lat, lon);

    }

    /**
     * Set the color of all part of the grid to one color.
     *
     * @param color  color to use
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException  remote error
     */
    public void setColor(Color color) throws VisADException, RemoteException {
        rangeRings.setColor(color);
        radials.setColor(color);
        labels.setColor(color);
        gridColor  = color;
        lineColor  = color;
        ringColor  = color;
        labelColor = color;
    }

    /**
     * Set the color of the azimuth lines (radials).
     *
     * @param color  color to use
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException  remote error
     */
    public void setAzimuthLineColor(Color color)
            throws VisADException, RemoteException {
        radials.setColor(color);
        lineColor = color;
    }

    /**
     * Set the color of the range rings.
     *
     * @param color  color to use
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException  remote error
     */
    public void setRangeRingColor(Color color)
            throws VisADException, RemoteException {
        rangeRings.setColor(color);
        ringColor = color;
    }

    /**
     * Set the color of the labels
     *
     * @param color  Color for labels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelColor(Color color)
            throws VisADException, RemoteException {
        labels.setColor(color);
        labelColor = color;
    }

    /**
     * Set the width of the labels
     *
     * @param width  width in pixels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelLineWidth(float width)
            throws VisADException, RemoteException {
        labels.setLineWidth(width);
        labelWidth = width;
    }

    /**
     * Set the width of the range rings
     *
     * @param width  width in pixels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRangeRingLineWidth(float width)
            throws VisADException, RemoteException {
        rangeRings.setLineWidth(width);
        rangeRingWidth = width;
    }

    /**
     * Set the width of the labels
     *
     * @param width  width in pixels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRadialLineWidth(float width)
            throws VisADException, RemoteException {
        radials.setLineWidth(width);
        radialWidth = width;
    }

    /**
     * Set the visibility of the azimuth lines (radials)
     *
     * @param visible  true to show
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException  remote error
     */
    public void setAzimuthLinesVisible(boolean visible)
            throws VisADException, RemoteException {
        radials.setVisible(visible);
    }


    /**
     * Set the visibility of the range rings.
     *
     * @param visible  true to show
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException  remote error
     */
    public void setRangeRingsVisible(boolean visible)
            throws VisADException, RemoteException {
        rangeRings.setVisible(visible);
    }

    /**
     * Set the labels visible
     *
     * @param visible  true to show the labels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelsVisible(boolean visible)
            throws VisADException, RemoteException {
        labels.setVisible(visible);
    }

    /**
     * Set the point to center the radar grid on.
     *
     * @param llp  new point to center on.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setCenterPoint(LatLonPoint llp)
            throws VisADException, RemoteException {
        setCenterPoint(llp.getLatitude().getValue(CommonUnit.degree),
                       llp.getLongitude().getValue(CommonUnit.degree));
    }

    /**
     * Make a set of range rings and radials on this center location.
     * color is implicit from class member data set elsewhere as in cstr.
     *
     * @param  lat in degrees
     * @param  lon in degrees east
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setCenterPoint(double lat, double lon)
            throws VisADException, RemoteException {
        /*
        clearDisplayables();  //This doesn't work for some reason.
        */
        removeDisplayable(radials);
        removeDisplayable(rangeRings);
        removeDisplayable(labels);

        center_lat = lat;
        center_lon = lon;

        makeRealTupleType(lat, lon);

        makeRadials();
        makeRangeRings();
        makeLabels();
    }

    /**
     * Set the center point and colors of the RadarGrid.
     *
     * @param lat       latitude (degrees)
     * @param lon       longitude (degrees)
     * @param rrcolor   range ring color
     * @param radcolor  radial color
     * @param lcolor    label color
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setCenterPointAndColors(double lat, double lon,
                                        Color rrcolor, Color radcolor,
                                        Color lcolor)
            throws VisADException, RemoteException {
        lineColor  = radcolor;
        ringColor  = rrcolor;
        labelColor = lcolor;
        setCenterPoint(lat, lon);

    }

    /**
     * Get the latitude of the center of radar range rings.
     *
     * @return  the center latitude (in degrees)
     */
    public double getCenterLatitude() {
        return center_lat;
    }

    /**
     * Get the longitude of the center of radar range rings.
     *
     * @return  the center longitude (in degrees)
     */
    public double getCenterLongitude() {
        return center_lon;
    }

    /**
     * Set the spacing and max value of radar range rings distance from center.
     *
     * @param  spacing in kilometers
     * @param  max     max radius in kilometers
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRangeRingSpacing(double spacing, double max)
            throws VisADException, RemoteException {
        rangeRings.setRingValues(
            new Real(rangeType, spacing, CommonUnit.meter.scale(1000)),
            new Real(rangeType, max, CommonUnit.meter.scale(1000)));
        rrSpacing = spacing;
        rrMax     = max;
        // redraw the radials which now may be too short or too long
        setRadialInterval(radialInc);
        // need new labels too; remove old ones first
        removeDisplayable(labels);
        makeLabels();
    }


    /**
     * Set the maximum radius of radar range rings
     *
     * @param  radius in kilometers
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setMaxRadius(double radius)
            throws VisADException, RemoteException {
        setRangeRingSpacing(rrSpacing, radius);
    }


    /**
     * Set the spacing of radar range ring labels.
     *
     * @param  li  label increment in kilometers
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelSpacing(double li)
            throws VisADException, RemoteException {
        label_inc = li;
        removeDisplayable(labels);
        makeLabels();
    }


    /**
     * Set the interval of radials.
     *
     * @param  inc angular degrees separation of radials
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRadialInterval(double inc)
            throws VisADException, RemoteException {
        // args are min distance in km, max distance, angular increment
        radials.setRadials(
            new Real(rangeType, rrSpacing, CommonUnit.meter.scale(1000)),
            new Real(rangeType, rrMax, CommonUnit.meter.scale(1000)), inc);
        radialInc = inc;

    }

    /**
     * Set the scaling size on the labels.
     *
     * @param size  scaling size (0 - 1)
     */
    public void setLabelSize(float size) {
        try {
            labelSize = size;
            labels.setTextSize(labelSize);

        } catch (Exception ve) {}
    }

    /**
     * Make the RealTupleType with a CoordinateSystem to lat/lon
     *
     * @param lat  latitude of center point (degrees)
     * @param lon  longitude of center point (degrees)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void makeRealTupleType(double lat, double lon)
            throws VisADException, RemoteException {
        Radar2DCoordinateSystem r2Dcs =
            new Radar2DCoordinateSystem((float) lat, (float) lon);
        rtt = new RealTupleType(rangeType, azimuthType, r2Dcs, null);
    }

    /**
     * Create the radials.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void makeRadials() throws VisADException, RemoteException {
        boolean oldVisible = ((radials != null)
                              ? radials.getVisible()
                              : getVisible());
        radials = new Radials("radials", rtt, lineColor);
        radials.setRadials(
            new Real(rangeType, rrSpacing, CommonUnit.meter.scale(1000)),
            new Real(rangeType, rrMax, CommonUnit.meter.scale(1000)),
            radialInc);
        radials.setVisible(oldVisible);
        radials.setLineWidth(radialWidth);
        addDisplayable(radials);
    }

    /**
     * Create the range rings
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void makeRangeRings() throws VisADException, RemoteException {
        boolean oldVisible = ((rangeRings != null)
                              ? rangeRings.getVisible()
                              : getVisible());
        rangeRings = new RingSet("range rings", rtt, ringColor);
        // set initial spacing etc.
        rangeRings.setRingValues(
            new Real(rangeType, rrSpacing, CommonUnit.meter.scale(1000)),
            new Real(rangeType, rrMax, CommonUnit.meter.scale(1000)));
        rangeRings.setVisible(oldVisible);
        rangeRings.setLineWidth(rangeRingWidth);
        addDisplayable(rangeRings);
    }

    /**
     * Create the labels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void makeLabels() throws VisADException, RemoteException {
        // labels arre cented along the 15 degree radial (from "north")
        boolean oldVisible = ((labels != null)
                              ? labels.getVisible()
                              : getVisible());
        labels = new RingLabels("Distance", rtt, new Real(azimuthType, 15),
                                labelColor);
        // there is a label at every 2nd range ring
        labels.setLabelValues(
            new Real(rangeType, label_inc, CommonUnit.meter.scale(1000)),
            new Real(rangeType, rrMax, CommonUnit.meter.scale(1000)));
        labels.setLabelUnit(CommonUnit.meter.scale(1000));
        labels.setTextSize(labelSize);
        labels.setVisible(oldVisible);
        labels.setLineWidth(labelWidth);
        addDisplayable(labels);
    }



    /**
     * Set the font on the labels
     *
     * @param f The font
     *
     * @throws VisADException  VisAD error
     * @throws RemoteException  remote error
     */
    public void setFont(Font f) throws VisADException, RemoteException {
        labels.setFont(f);
    }


}

/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.storm;




import visad.*;

import visad.georef.EarthLocation;

import java.util.ArrayList;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 18, 2008
 * Time: 1:45:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormTrackPoint implements Comparable {

    /** _more_          */
    private int id = -1;

    /** _more_          */
    private boolean edited = false;

    /** _more_ */
    private EarthLocation location;

    /** _more_ */
    private DateTime time;

    /** _more_ */
    private List<Real> attributes;

    /** _more_ */
    private int forecastHour = 0;


    /**
     * copy ctor
     *
     * @param that The track point to copy
     */
    public StormTrackPoint(StormTrackPoint that) {
        this.location = that.location;
        this.time = that.time;
        if(that.attributes!=null) {
            this.attributes = (List<Real>) new ArrayList(that.attributes);
        }
        this.forecastHour = that.forecastHour;
        this.id = that.id;
        this.edited = that.edited;
    }


    /**
     * _more_
     *
     * @param pointLocation _more_
     * @param time _more_
     * @param forecastHour _more_
     * @param attrs _more_
     */
    public StormTrackPoint(EarthLocation pointLocation, DateTime time,
                           int forecastHour, List<Real> attrs) {
        this.location     = pointLocation;
        this.time         = time;
        this.forecastHour = forecastHour;
        this.attributes   = attrs;
    }


    /**
     * Compare this object to another.
     * @param o object in question.
     * @return spec from Comparable interface.
     */
    public int compareTo(Object o) {
        if (o instanceof StormTrackPoint) {
            StormTrackPoint that = (StormTrackPoint) o;
            return (time.compareTo(that.time));
        }
        return toString().compareTo(o.toString());
    }


    /**
     *  Set the ForecastHour property.
     *
     *  @param value The new value for ForecastHour
     */
    public void setForecastHour(int value) {
        forecastHour = value;
    }


    /**
     *  Get the ForecastHour property.
     *
     *  @return The ForecastHour
     */
    public int getForecastHour() {
        return forecastHour;
    }


    /**
     * _more_
     *
     * @param time _more_
     */
    public void setTime(DateTime time) {
        this.time = time;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getTime() {
        return time;
    }

    /**
     * _more_
     *
     * @param point _more_
     */
    public void setLocation(EarthLocation point) {
        this.location = point;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public EarthLocation getLocation() {
        return location;
    }


    /*
     * _more_
     *
     * @return _more_
     */

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Real> getTrackAttributes() {
        return attributes;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return location + "";
    }

    /**
     * _more_
     *
     *
     * @param param _more_
     *
     * @return _more_
     */
    public Real getAttribute(StormParam param) {
        return param.getAttribute(attributes);
    }

    /**
     * _more_
     *
     *
     * @param real _more_
     *
     * @return _more_
     */
    public void setAttribute(Real real) {
        for (Real attr : attributes) {
            if (attr.getType().equals(real.getType())) {
               // attr.(real)
                attributes.remove(attr);
                attributes.add(real);
            }
        }

    }

    /**
     *  _more_
     *
     *  @param attr _more_
     *
     */
    public void addAttribute(Real attr) {
        if (attributes == null) {
            attributes = new ArrayList<Real>();
        }

        attributes.add(attr);

    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    /*
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if ( !(o instanceof StormTrackPoint)) {
            return false;
        }
        StormTrackPoint other = (StormTrackPoint) o;
        return ((trackPointId.equals(other.trackPointId)));
        }*/

    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(int value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public int getId() {
        return id;
    }

    /**
     *  Set the Edited property.
     *
     *  @param value The new value for Edited
     */
    public void setEdited(boolean value) {
        edited = value;
    }

    /**
     *  Get the Edited property.
     *
     *  @return The Edited
     */
    public boolean getEdited() {
        return edited;
    }





}


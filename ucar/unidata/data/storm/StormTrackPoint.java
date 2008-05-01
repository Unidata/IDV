package ucar.unidata.data.storm;

import ucar.nc2.Attribute;
import visad.*;
import visad.georef.EarthLocation;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 18, 2008
 * Time: 1:45:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormTrackPoint {

    /** _more_ */
    private EarthLocation trackPointLocation;

    /** _more_ */
    private DateTime trackPointTime;

    /** _more_ */
    private List<Attribute> attributes;


    private int forecastHour = 0;


    public StormTrackPoint(EarthLocation  pointLocation, DateTime time,
                           int forecastHour,
                           List attrs) {
        this.trackPointLocation = pointLocation;
        this.trackPointTime  = time;
        this.forecastHour = forecastHour;
        this.attributes = new ArrayList(attrs);
    }

    /**
       Set the ForecastHour property.

       @param value The new value for ForecastHour
    **/
    public void setForecastHour (int value) {
	forecastHour = value;
    }

    /**
       Get the ForecastHour property.

       @return The ForecastHour
    **/
    public int getForecastHour () {
	return forecastHour;
    }


    public void setTrackPointTime(DateTime time) {
        this.trackPointTime = time;
    }

     /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getTrackPointTime() {

        return  trackPointTime;
    }

    /**
     * _more_
     *
     * @param point _more_
     */
    public void setTrackPointLocation( EarthLocation point) {
        this.trackPointLocation = point;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public EarthLocation getTrackPointLocation() {
        return trackPointLocation;
    }


    /**
     * _more_
     *
     * @param attrs _more_
     */
    public void setTrackAttributes(List<Attribute> attrs) {
        this.attributes = new ArrayList<Attribute>(attrs);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Attribute> getTrackAttributes() {
        return attributes;
    }


    public String toString() {
        return trackPointLocation+"";
    }

    /**
     * _more_
     *
     * @param attrName _more_
     *
     * @return _more_
     */
    public String getAttribute(String attrName) {
        for (Attribute attr : attributes) {
            if (attr.getName().equalsIgnoreCase(attrName)) {
                return attr.getNumericValue().toString();
            }
        }
        return "";
    }

    /**
     * _more_
     *
     * @param o _more_
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
}

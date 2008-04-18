package ucar.unidata.data.storm;

import visad.georef.EarthLocation;

import java.util.List;
import java.util.Date;
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
    private String trackPointId;

    /** _more_ */
    private StormInfo stormInfo;

    /** _more_ */
    private EarthLocation trackPintLocation;

    /** _more_ */
    private Date trackPointTime;

    /** _more_ */
    private List attributes;



    public StormTrackPoint(StormInfo stormInfo, EarthLocation  pointLocation, Date time,
                 List attrs) {

        this.stormInfo   = stormInfo;

        this.trackPintLocation = pointLocation;
        this.trackPointTime  = time;
        this.attributes = new ArrayList(attrs);

        this.trackPointId = stormInfo.toString() + "_" + pointLocation.toString() + "_"
                       + time.getTime();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return trackPointId.hashCode();
    }

    /**
     * _more_
     *
     * @param id _more_
     */
    public void setTrackId(String id) {
        this.trackPointId = id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTrackId() {
        return trackPointId;
    }





    /**
     * _more_
     *
     * @param stormInfo _more_
     */
    public void setStormInfo(StormInfo stormInfo) {
        this.stormInfo = stormInfo;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public StormInfo getStormInfo() {
        return stormInfo;
    }


    public void setTrackPointTime(Date time) {
        this.trackPointTime = time;
    }

     /**
     * _more_
     *
     * @return _more_
     */
    public Date getTrackPointTime() {

        return  trackPointTime;
    }

    /**
     * _more_
     *
     * @param point _more_
     */
    public void setTrackPointLocation( EarthLocation point) {
        this.trackPintLocation = point;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public EarthLocation getTrackPointLocation() {
        return trackPintLocation;
    }


    /**
     * _more_
     *
     * @param attrs _more_
     */
    public void setTrackAttributes(List attrs) {
        this.attributes = new ArrayList(attrs);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTrackAttributes() {
        return attributes;
    }


    public String toString() {
        return trackPointId;
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if ( !(o instanceof StormTrackPoint)) {
            return false;
        }
        StormTrackPoint other = (StormTrackPoint) o;
        return ((trackPointId.equals(other.trackPointId)));
    }
}

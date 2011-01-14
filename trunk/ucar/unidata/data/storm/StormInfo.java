/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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



/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 4:57:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormInfo implements Comparable {

    /** _more_ */
    private String stormID;

    /** _more_ */
    private String name;


    /** _more_ */
    private String basin;

    /** _more_ */
    private String number;


    /** _more_ */
    private DateTime startTime;

    /**
     * _more_
     */
    public StormInfo() {}

    /**
     * _more_
     *
     * @param id _more_
     * @param sTime _more_
     */
    public StormInfo(String id, DateTime sTime) {
        this(id, id, sTime);
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param sTime _more_
     */
    public StormInfo(String id, String name, DateTime sTime) {
        this(id, name, null, null, sTime);
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param basin _more_
     * @param number _more_
     * @param sTime _more_
     */
    public StormInfo(String id, String name, String basin, String number,
                     DateTime sTime) {
        this.stormID   = id;
        this.name      = name;
        this.basin     = basin;
        this.startTime = sTime;
        this.number    = number;
    }


    /**
     * Compare this object to another.
     * @param o object in question.
     * @return spec from Comparable interface.
     */
    public int compareTo(Object o) {
        if (o instanceof StormInfo) {
            StormInfo that = (StormInfo) o;
            if (startTime.getValue() < that.startTime.getValue()) {
                return -1;
            }
            if (startTime.getValue() > that.startTime.getValue()) {
                return 1;
            }
            return 0;
        }
        return toString().compareTo(o.toString());
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    public void setStormId(String id) {
        this.stormID = id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getStormId() {
        return stormID;
    }

    /**
     * _more_
     *
     * @param dt _more_
     */
    public void setStartTime(DateTime dt) {
        this.startTime = dt;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getStartTime() {
        return startTime;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (name != null) {
            return name;
        }
        return stormID;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return stormID.hashCode();
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
        if ( !(o instanceof StormInfo)) {
            return false;
        }
        StormInfo other = (StormInfo) o;
        return (this.stormID.equals(other.stormID));
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     * Set the Basin property.
     *
     * @param value The new value for Basin
     */
    public void setBasin(String value) {
        basin = value;
    }

    /**
     * Get the Basin property.
     *
     * @return The Basin
     */
    public String getBasin() {
        return basin;
    }


    /**
     * Set the Number property.
     *
     * @param value The new value for Number
     */
    public void setNumber(String value) {
        number = value;
    }

    /**
     * Get the Number property.
     *
     * @return The Number
     */
    public String getNumber() {
        return number;
    }


}


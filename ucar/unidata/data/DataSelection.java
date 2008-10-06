/*
 * $Id: DataSelection.java,v 1.43 2006/12/01 20:41:21 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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


package ucar.unidata.data;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * A class that represents some selection of data.
 * @author Jeff McWhirter
 * @version $Revision: 1.43 $
 */
public class DataSelection {

    /** Null DataSelection */
    public static final DataSelection NULL = null;

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            DataSelection.class.getName());

    /** Flag to use any times set here */
    public final static int TIMESMODE_USETHIS = 0;

    /** Flag to use any times set elsewhere */
    public final static int TIMESMODE_USEOTHER = 1;

    /** the time mode being used */
    private int timesMode = TIMESMODE_USEOTHER;

    /** list of times */
    private List times;


    /** From level. Typically null or a Real */
    private Object fromLevel;

    /** To level. Typically null or a Real */
    private Object toLevel;



    /** The subset */
    private GeoSelection geoSelection;

    /** _more_ */
    private Hashtable properties = new Hashtable();

    /**
     * Construct a <code>DataSelection</code>.
     */
    public DataSelection() {
        this((List) null);
    }


    /**
     * _more_
     *
     * @param fromLevel _more_
     * @param toLevel _more_
     */
    public DataSelection(Object fromLevel, Object toLevel) {
        this.fromLevel = fromLevel;
        this.toLevel   = toLevel;
    }


    /**
     * ctor
     *
     * @param zStride z stride
     */
    public DataSelection(int zStride) {
        this.geoSelection = new GeoSelection(null, GeoSelection.STRIDE_BASE,
                                             GeoSelection.STRIDE_BASE,
                                             zStride);
    }


    /**
     * ctor
     *
     * @param xStride stride
     * @param yStride stride
     * @param zStride stride
     */
    public DataSelection(int xStride, int yStride, int zStride) {
        this.geoSelection = new GeoSelection(null, xStride, yStride, zStride);
    }


    /**
     * Construct a <code>DataSelection</code>.
     *
     * @param setDefaults If true then we set our defaults from the global defaults
     */
    public DataSelection(boolean setDefaults) {
        this((List) null);
        if (setDefaults) {
            GeoLocationInfo bbox = GeoSelection.getDefaultBoundingBox();
            if (bbox != null) {
                this.geoSelection = new GeoSelection(bbox);
            }
        }
    }



    /**
     * Construct a <code>DataSelection</code> with a list of times.
     * Use the default times mode
     *
     * @param times  <code>List</code> of DateTimes
     */
    public DataSelection(List times) {
        this(times, TIMESMODE_USEOTHER);
    }

    /**
     * Construct a <code>DataSelection</code> with a list of times and
     * a particular times mode.
     *
     * @param times  <code>List</code> of DateTimes
     * @param timesMode  mode to use (TIMESMODE_USETHIS, TIMESMODE_USEOTHER)
     */
    public DataSelection(List times, int timesMode) {
        this.times     = Misc.cloneList(times);
        this.timesMode = timesMode;

    }


    /**
     * Construct a <code>DataSelection</code> from another instance.
     *
     * @param that  other DataSelection
     */
    public DataSelection(DataSelection that) {
        if (that != null) {
            this.times     = Misc.cloneList(that.times);
            this.timesMode = that.timesMode;
            if (that.geoSelection != null) {
                this.geoSelection = new GeoSelection(that.geoSelection);
            }
            this.fromLevel  = that.fromLevel;
            this.toLevel    = that.toLevel;
            this.properties = (Hashtable) that.properties.clone();
        }
    }


    /**
     * Set the times for this DataSelection from another
     *
     * @param other   other DataSelection
     */
    private void setTimesFromSelection(DataSelection other) {
        setTimes(other.getTimes());
        this.timesMode = other.timesMode;
    }

    /**
     * If either  of the params are null then simply return the other one.
     * Else, create a new DataSelection and set its times and other attributes
     * to those held by the higherPriority param if it contains times, etc.,
     * else set it to the lowerPriority param.
     *
     * @param higherPriority    DataSelection with higher priority
     * @param lowerPriority     DataSelection with lower priority
     * @return  the new DataSelection
     */
    public static DataSelection merge(DataSelection higherPriority,
                                      DataSelection lowerPriority) {
        if ((lowerPriority == null) && (higherPriority == null)) {
            return new DataSelection();
        }
        if (lowerPriority == null) {
            return new DataSelection(higherPriority);
        }
        if (higherPriority == null) {
            return new DataSelection(lowerPriority);
        }
        DataSelection newSelection = new DataSelection();

        Hashtable     props        = new Hashtable(lowerPriority.properties);
        props.putAll(higherPriority.properties);
        newSelection.setProperties(props);


        GeoSelection newGeoSelection =
            GeoSelection.merge(higherPriority.geoSelection,
                               lowerPriority.geoSelection);

        newSelection.setGeoSelection(newGeoSelection);

        if (higherPriority.fromLevel != null) {
            newSelection.fromLevel = higherPriority.fromLevel;
            newSelection.toLevel   = higherPriority.toLevel;
        } else {
            newSelection.fromLevel = lowerPriority.fromLevel;
            newSelection.toLevel   = lowerPriority.toLevel;
        }

        if (higherPriority.hasTimes()) {
            newSelection.setTimesFromSelection(higherPriority);
        } else if (lowerPriority.hasTimes()) {
            newSelection.setTimesFromSelection(lowerPriority);
        } else {
            //      System.err.println ("neither has times");
        }
        return newSelection;
    }


    /**
     * Determine whether we should use the times from this
     * DataSelection or not.
     *
     * @return  true if we should use the times from this
     */
    public boolean getUseThisTimes() {
        return (timesMode == TIMESMODE_USETHIS);
    }

    /**
     * Set the times mode.
     *
     * @param mode (TIMESMODE_USEOTHER, TIMESMODE_USETHIS)
     */
    public void setTimesMode(int mode) {
        timesMode = mode;
    }

    /**
     * Get the times mode.
     *
     * @return TIMESMODE_USEOTHER or TIMESMODE_USETHIS
     */
    public int getTimesMode() {
        return timesMode;
    }

    /**
     * Method for creating a clone of this DataSelection
     *
     * @return  cloned version
     */
    public DataSelection cloneMe() {
        return new DataSelection(this);
    }

    /**
     * Check whether this DataSelection has a time selection or not.
     *
     * @return  true if it has times set
     */
    public boolean hasTimes() {
        return (getUseThisTimes() || ((times != null) && !times.isEmpty()));
    }

    /**
     * Set the times  list held by this DataSelection. The given argument
     * may be null and is cloned. This list represents the set of selected
     * times which may either be DateTime objects or Integer indices.
     *
     * @param selectedTimes      The new times list.
     */
    public void setTimes(List selectedTimes) {
        this.times = Misc.cloneList(selectedTimes);
    }


    /**
     * Get the list of times held by this DataSelection
     *
     * @return  list of times
     */
    public List getTimes() {
        return this.times;
    }


    /**
     * Get the hash code for this DataSelection
     *
     * @return  the hash code
     */
    public int hashCode() {
        return Misc.hashcode(times) ^ Misc.hashcode(properties);
    }


    /**
     * See if the Object in question is equal to this DataSelection.
     *
     * @param o    Object in question
     *
     * @return   true if they are equal
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ( !(o instanceof DataSelection)) {
            return false;
        }
        DataSelection that = (DataSelection) o;
        return Misc.equals(this.times, that.times)
               && Misc.equals(this.properties, that.properties)
               && Misc.equals(this.geoSelection, that.geoSelection)
               && Misc.equals(this.fromLevel, that.fromLevel)
               && Misc.equals(this.toLevel, that.toLevel);
    }

    /**
     * Return a string representation of this DataSelection.
     *
     * @return a string representation of this DataSelection
     */
    public String toString() {
        return super.toString() + " " + times;
        //        return "data selection:" + fromLevel;
        //        return "data selection:" + geoSelection;

    }

    /**
     * Set the level. This in effect sets both the from and the to level to the argument
     *
     * @param level The level
     */
    public void setLevel(Object level) {
        setLevelRange(level, level);
    }

    /**
     * Set the Level property.
     *
     * @param fromLevel from level
     * @param toLevel to level
     */
    public void setLevelRange(Object fromLevel, Object toLevel) {
        this.fromLevel = fromLevel;
        this.toLevel   = toLevel;
    }


    /**
     * Set the FromLevel property.
     *
     * @param value The new value for FromLevel
     */
    public void setFromLevel(Object value) {
        fromLevel = value;
    }

    /**
     * Get the FromLevel property.
     *
     * @return The FromLevel
     */
    public Object getFromLevel() {
        return fromLevel;
    }



    /**
     * Set the ToLevel property.
     *
     * @param value The new value for ToLevel
     */
    public void setToLevel(Object value) {
        toLevel = value;
    }

    /**
     * Get the ToLevel property.
     *
     * @return The ToLevel
     */
    public Object getToLevel() {
        return toLevel;
    }



    /**
     * Set the GeoSelection property.
     *
     * @param value The new value for GeoSelection
     */
    public void setGeoSelection(GeoSelection value) {
        geoSelection = value;
    }

    /**
     * Get the GeoSelection property.
     *
     * @return The GeoSelection
     */
    public GeoSelection getGeoSelection() {
        return geoSelection;
    }


    /**
     * Get the GeoSelection property.
     *
     * @param createIfNeeded If true then create the geosubset of we don't have one already.
     * @return The GeoSelection
     */
    public GeoSelection getGeoSelection(boolean createIfNeeded) {
        if (createIfNeeded && (geoSelection == null)) {
            geoSelection = new GeoSelection();
        }
        return geoSelection;
    }


    /**
     * Set the Properties property.
     *
     * @param value The new value for Properties
     */
    public void setProperties(Hashtable value) {
        properties = value;
    }

    /**
     * Get the Properties property.
     *
     * @return The Properties
     */
    public Hashtable getProperties() {
        return properties;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     */
    public void removeProperty(Object key) {
        properties.remove(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }



}


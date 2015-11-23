/*
 * Copyright 1997-2016 Unidata Program Center/University Corporation for
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


import ucar.unidata.util.HashCodeUtils;
import ucar.unidata.util.Misc;

import visad.DateTime;


import java.util.Hashtable;
import java.util.List;


/**
 * A class that represents some selection of data.
 * @author IDV Development Team
 */
public class DataSelection {

    /** Null DataSelection */
    public static final DataSelection NULL = null;

    /** Property for selected levels */
    public static final String PROP_DEFAULT_LEVELS = "default_levels";

    /** Flag to use any times set elsewhere */
    public final static String PROP_DATESELECTION = "date_selection";

    /** status template */
    public static final String PROP_USESTIMEDRIVER =
        "Use_Display_Driver_Times";

    /** status template */
    public static final String PROP_ASTIMEDRIVER = "As_Display_Driver_Times";

    /** chooser time matching property */
    public static final String PROP_CHOOSERTIMEMATCHING =
        "Chooser_Do_Time_Matching";

    /** as time subset selection */
    public final static String PROP_TIMESUBSET = "Use DataChoice time subset";

    /** progressive resolution */
    public final static String PROP_PROGRESSIVERESOLUTION = "Use_Progressive_Resolution";

    /** region option */
    public final static String PROP_REGIONOPTION = "Region_Selection_Option";

    /** region option */
    public final static String PROP_HASCORNER = "Region_Selection_Has_Conner";

    /** region option */
    public final static String PROP_USEDISPLAYAREA = "Match_Display_Area";

    /** region option */
    public final static String PROP_USEDEFAULTAREA = "Use_Default_Area";

    /** region option */
    public final static String PROP_USESELECTEDAREA = "Use_Selected_Area";
    
    /** view manager - used for time/region matching */
    public final static String PROP_DEFAULTVIEW = "Default_View";
    
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

    /** time driver times */
    private List<DateTime> timeDriverTimes;


    /** From level. Typically null or a Real */
    private Object fromLevel;

    /** To level. Typically null or a Real */
    private Object toLevel;



    /** The subset */
    private GeoSelection geoSelection;

    /** properties for the data selection */
    private Hashtable properties = new Hashtable();

    /**
     * Construct a <code>DataSelection</code>.
     */
    public DataSelection() {
        this((List) null);
    }


    /**
     * ctor
     *
     * @param fromLevel from level
     * @param toLevel -0
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
            this.times = Misc.cloneList(that.times);
            if (that.timeDriverTimes != null) {
                this.timeDriverTimes =
                    (List<DateTime>) Misc.cloneList(that.timeDriverTimes);
            }
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
     * set the x and y stride
     *
     * @param stride the stride
     */
    public void setXYStride(int stride) {
        getGeoSelection(true).setXStride(stride);
        getGeoSelection(true).setYStride(stride);
    }


    /**
     * set the x stride
     *
     * @param stride the stride
     */

    public void setXStride(int stride) {
        getGeoSelection(true).setXStride(stride);
    }

    /**
     * set the y stride
     *
     * @param stride the stride
     */
    public void setYStride(int stride) {
        getGeoSelection(true).setYStride(stride);
    }

    /**
     * set the z stride
     *
     * @param stride the stride
     */
    public void setZStride(int stride) {
        getGeoSelection(true).setZStride(stride);
    }


    /**
     * set the clipping bounds
     *
     * @param north north
     * @param west west
     * @param south south
     * @param east east
     */
    public void setBounds(double north, double west, double south,
                          double east) {
        getGeoSelection(true).setBoundingBox(new GeoLocationInfo(north, west,
                south, east));
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

        if (higherPriority.timeDriverTimes != null) {
            newSelection.setTheTimeDriverTimes(
                higherPriority.timeDriverTimes);
        } else {
            newSelection.setTheTimeDriverTimes(lowerPriority.timeDriverTimes);
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
        return HashCodeUtils.hash(HashCodeUtils.hash( Misc.hashcode(times),
                Misc.hashcode(properties)), Misc.hashcode(geoSelection));

      //  return Misc.hashcode(times) ^ Misc.hashcode(properties);
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
        //        return super.toString() + " " + times;
        //        return "data selection:" + fromLevel;
        return "bounds:" + geoSelection + ", has TimeDriver times: "
               + (getTimeDriverTimes() != null);

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
     * get the property
     *
     * @param key property key
     *
     * @return property value
     */
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    /**
     * remove the property identified by the given key
     *
     * @param key property key
     */
    public void removeProperty(Object key) {
        properties.remove(key);
    }

    /**
     * Get the named boolean property
     *
     * @param name   name of property
     * @param dflt   default value
     *
     * @return  the value of the property or the default
     */
    public boolean getProperty(String name, boolean dflt) {
        Object o = getProperty(name);
        if (o == null) {
            return dflt;
        }
        return new Boolean(o.toString()).booleanValue();
    }

    /**
     * Get the named object property
     *
     * @param name   name of property
     * @param dflt   default value
     *
     * @return  the value of the property or the default
     */
    public String getProperty(String name, String dflt) {
        Object o = getProperty(name);
        if (o == null) {
            return dflt;
        }
        return o.toString();
    }
    /**
     * put the property
     *
     * @param key key
     * @param value value
     */
    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     *  Set the TimeDriverTimes property.
     *
     *  @param value The new value for TimeDriverTimes
     */
    public void setTheTimeDriverTimes(List<DateTime> value) {
        this.timeDriverTimes = value;
    }

    /**
     *  Get the TimeDriverTimes property.
     *
     *  @return The TimeDriverTimes
     */
    public List<DateTime> getTimeDriverTimes() {
        return this.timeDriverTimes;
    }



}

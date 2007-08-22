/*
 *  ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2005, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * ---------------------
 * HistogramDataset.java
 * ---------------------
 * (C) Copyright 2003-2005, by Jelai Wang and Contributors.
 *
 * Original Author:  Jelai Wang (jelaiw AT mindspring.com);
 * Contributor(s):   David Gilbert (for Object Refinery Limited);
 *                   Cameron Hayne;
 *
 * $Id: MyHistogramDataset.java,v 1.5 2006/06/28 15:49:55 jeffmc Exp $
 *
 * Changes
 * -------
 * 06-Jul-2003 : Version 1, contributed by Jelai Wang (DG);
 * 07-Jul-2003 : Changed package and added Javadocs (DG);
 * 15-Oct-2003 : Updated Javadocs and removed array sorting (JW);
 * 09-Jan-2004 : Added fix by "Z." posted in the JFreeChart forum (DG);
 * 01-Mar-2004 : Added equals() and clone() methods and implemented
 *               Serializable.  Also added new addSeries() method (DG);
 * 06-May-2004 : Now extends AbstractIntervalXYDataset (DG);
 * 15-Jul-2004 : Switched getX() with getXValue() and getY() with
 *               getYValue() (DG);
 * 20-May-2005 : Speed up binning - see patch 1026151 contributed by Cameron
 *               Hayne (DG);
 * 08-Jun-2005 : Fixed bug in getSeriesKey() method (DG);
 *
 */

//package org.jfree.data.statistics;
package ucar.unidata.idv.control.chart;


import org.jfree.data.general.DatasetChangeEvent;

import org.jfree.data.statistics.*;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.util.ObjectUtilities;
import org.jfree.util.PublicCloneable;


import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A dataset that can be used for creating histograms.
 *
 * @see SimpleHistogramDataset
 */
public class MyHistogramDataset extends AbstractIntervalXYDataset implements IntervalXYDataset,
        Cloneable, PublicCloneable, Serializable {

    /** For serialization. */
    private static final long serialVersionUID = -6341668077370231153L;

    /** A list of maps. */
    private List list;

    /** The histogram type. */
    private HistogramType type;

    /**
     * Creates a new (empty) dataset with a default type of
     * {@link HistogramType}.FREQUENCY.
     */
    public MyHistogramDataset() {
        this.list = new ArrayList();
        this.type = HistogramType.FREQUENCY;
    }

    /**
     * Remove series
     */
    public void removeAllSeries() {
        this.list = new ArrayList();
    }

    /**
     * Returns the histogram type.
     *
     * @return The type (never <code>null</code>).
     */
    public HistogramType getType() {
        return this.type;
    }

    /**
     * Sets the histogram type and sends a {@link DatasetChangeEvent} to all
     * registered listeners.
     *
     * @param type  the type (<code>null</code> not permitted).
     */
    public void setType(HistogramType type) {
        if (type == null) {
            throw new IllegalArgumentException("Null 'type' argument");
        }
        this.type = type;
        notifyListeners(new DatasetChangeEvent(this, this));
    }

    /**
     * Adds a series to the dataset, using the specified number of bins.
     *
     * @param key  the series key (<code>null</code> not permitted).
     * @param values the values (<code>null</code> not permitted).
     * @param bins  the number of bins (must be at least 1).
     */
    public void addSeries(Comparable key, double[] values, int bins) {
        // defer argument checking...
        double minimum = getMinimum(values);
        double maximum = getMaximum(values);
        addSeries(key, values, bins, minimum, maximum);
    }

    /**
     * Adds a series to the dataset. Any data value falling on a bin boundary
     * will be assigned to the lower value bin, with the exception of the lower
     * bound of the bin range which is always assigned to the first bin.
     *
     * @param key  the series key (<code>null</code> not permitted).
     * @param values  the raw observations.
     * @param bins  the number of bins.
     * @param minimum  the lower bound of the bin range.
     * @param maximum  the upper bound of the bin range.
     */
    public void addSeries(Comparable key, double[] values, int bins,
                          double minimum, double maximum) {

        if (key == null) {
            throw new IllegalArgumentException("Null 'key' argument.");
        }
        if (values == null) {
            throw new IllegalArgumentException("Null 'values' argument.");
        } else if (bins < 1) {
            throw new IllegalArgumentException(
                "The 'bins' value must be at least 1.");
        }
        double binWidth = (maximum - minimum) / bins;

        double tmp      = minimum;
        List   binList  = new ArrayList(bins);
        for (int i = 0; i < bins; i++) {
            HistogramBin bin;
            // make sure bins[bins.length]'s upper boundary ends at maximum
            // to avoid the rounding issue. the bins[0] lower boundary is
            // guaranteed start from min
            if (i == bins - 1) {
                bin = new HistogramBin(tmp, maximum);
            } else {
                bin = new HistogramBin(tmp, tmp + binWidth);
            }
            tmp = tmp + binWidth;
            binList.add(bin);
        }
        // fill the bins
        for (int i = 0; i < values.length; i++) {
            int binIndex = bins - 1;
            if (values[i] < maximum) {
                double fraction = (values[i] - minimum) / (maximum - minimum);
                binIndex = (int) (fraction * bins);
            }
            HistogramBin bin = (HistogramBin) binList.get(binIndex);
            bin.incrementCount();
        }
        // generic map for each series
        Map map = new HashMap();
        map.put("key", key);
        map.put("bins", binList);
        map.put("values.length", new Integer(values.length));
        map.put("bin width", new Double(binWidth));
        this.list.add(map);
    }

    /**
     * Returns the minimum value in an array of values.
     *
     * @param values  the values (<code>null</code> not permitted and
     *                zero-length array not permitted).
     *
     * @return The minimum value.
     */
    private double getMinimum(double[] values) {
        if ((values == null) || (values.length < 1)) {
            throw new IllegalArgumentException(
                "Null or zero length 'values' argument.");
        }
        double min = Double.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    /**
     * Returns the maximum value in an array of values.
     *
     * @param values  the values (<code>null</code> not permitted and
     *                zero-length array not permitted).
     *
     * @return The maximum value.
     */
    private double getMaximum(double[] values) {
        if ((values == null) || (values.length < 1)) {
            throw new IllegalArgumentException(
                "Null or zero length 'values' argument.");
        }
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    /**
     * Returns the bins for a series.
     *
     * @param series  the series index.
     *
     * @return An array of bins.
     */
    List getBins(int series) {
        if ((series < 0) || (series >= this.list.size())) {
            return new ArrayList();
        }

        Map map = (Map) this.list.get(series);
        return (List) map.get("bins");
    }

    /**
     * Returns the total number of observations for a series.
     *
     * @param series  the series index.
     *
     * @return The total.
     */
    private int getTotal(int series) {
        Map map = (Map) this.list.get(series);
        return ((Integer) map.get("values.length")).intValue();
    }

    /**
     * Returns the bin width for a series.
     *
     * @param series  the series index (zero based).
     *
     * @return The bin width.
     */
    private double getBinWidth(int series) {
        Map map = (Map) this.list.get(series);
        return ((Double) map.get("bin width")).doubleValue();
    }

    /**
     * Returns the number of series in the dataset.
     *
     * @return The series count.
     */
    public int getSeriesCount() {
        return this.list.size();
    }

    /**
     * Returns the key for a series.
     *
     * @param series  the series index (zero based).
     *
     * @return The series key.
     */
    public Comparable getSeriesKey(int series) {
        Map map = (Map) this.list.get(series);
        return (String) map.get("key");
    }





    /**
     * Returns the number of data items for a series.
     *
     * @param series  the series index (zero based).
     *
     * @return The item count.
     */
    public int getItemCount(int series) {
        return getBins(series).size();
    }

    /**
     * Returns the X value for a bin.  This value won't be used for plotting
     * histograms, since the renderer will ignore it.  But other renderers can
     * use it (for example, you could use the dataset to create a line
     * chart).
     *
     * @param series  the series index (zero based).
     * @param item  the item index (zero based).
     *
     * @return The start value.
     */
    public Number getX(int series, int item) {
        List         bins = getBins(series);
        HistogramBin bin  = (HistogramBin) bins.get(item);
        double       x = (bin.getStartBoundary() + bin.getEndBoundary()) / 2.;
        return new Double(x);
    }

    /**
     * Returns the y-value for a bin (calculated to take into account the
     * histogram type).
     *
     * @param series  the series index (zero based).
     * @param item  the item index (zero based).
     *
     * @return The y-value.
     */
    public Number getY(int series, int item) {
        List         bins     = getBins(series);
        HistogramBin bin      = (HistogramBin) bins.get(item);
        double       total    = getTotal(series);
        double       binWidth = getBinWidth(series);

        if (this.type == HistogramType.FREQUENCY) {
            return new Double(bin.getCount());
        } else if (this.type == HistogramType.RELATIVE_FREQUENCY) {
            return new Double(bin.getCount() / total);
        } else if (this.type == HistogramType.SCALE_AREA_TO_1) {
            return new Double(bin.getCount() / (binWidth * total));
        } else {  // pretty sure this shouldn't ever happen
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the start value for a bin.
     *
     * @param series  the series index (zero based).
     * @param item  the item index (zero based).
     *
     * @return The start value.
     */
    public Number getStartX(int series, int item) {
        List         bins = getBins(series);
        HistogramBin bin  = (HistogramBin) bins.get(item);
        return new Double(bin.getStartBoundary());
    }

    /**
     * Returns the end value for a bin.
     *
     * @param series  the series index (zero based).
     * @param item  the item index (zero based).
     *
     * @return The end value.
     */
    public Number getEndX(int series, int item) {
        List         bins = getBins(series);
        HistogramBin bin  = (HistogramBin) bins.get(item);
        return new Double(bin.getEndBoundary());
    }

    /**
     * Returns the start y-value for a bin (which is the same as the y-value,
     * this method exists only to support the general form of the
     * {@link IntervalXYDataset} interface).
     *
     * @param series  the series index (zero based).
     * @param item  the item index (zero based).
     *
     * @return The y-value.
     */
    public Number getStartY(int series, int item) {
        return getY(series, item);
    }

    /**
     * Returns the end y-value for a bin (which is the same as the y-value,
     * this method exists only to support the general form of the
     * {@link IntervalXYDataset} interface).
     *
     * @param series  the series index (zero based).
     * @param item  the item index (zero based).
     *
     * @return The Y value.
     */
    public Number getEndY(int series, int item) {
        return getY(series, item);
    }

    /**
     * Tests this dataset for equality with an arbitrary object.
     *
     * @param obj  the object to test against (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ( !(obj instanceof MyHistogramDataset)) {
            return false;
        }
        MyHistogramDataset that = (MyHistogramDataset) obj;
        if ( !ObjectUtilities.equal(this.type, that.type)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.list, that.list)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a clone of the dataset.
     *
     * @return A clone of the dataset.
     *
     * @throws CloneNotSupportedException if the object cannot be cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}


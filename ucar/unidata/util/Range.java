/*
 * $Id: Range.java,v 1.15 2007/08/08 17:15:20 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


package ucar.unidata.util;


/**
 * Holds a simple min/max range
 *
 *
 */
public class Range implements java.io.Serializable {

    /** name of this range */
    private String name;

    /** The range */
    public double min, max;


    /**
     * Default ctor
     *
     */
    public Range() {
        min = 0.0;
        max = 1.0;
    }

    /**
     * Create a range with min, max
     *
     * @param min min
     * @param max max
     *
     */
    public Range(double min, double max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Create a range with min, max and name
     *
     * @param min min
     * @param max max
     * @param name name
     */
    public Range(double min, double max, String name) {
        this(min, max);
        this.name = name;
    }


    /**
     * ctor
     *
     * @param a 2-ary array holding min/max
     *
     */
    public Range(double[] a) {
        this(a[0], a[1]);
    }

    /**
     * copy ctor
     *
     * @param r object
     *
     */
    public Range(Range r) {
        if (r != null) {
            this.min = r.min;
            this.max = r.max;
        }
    }

    /**
     * Equals
     *
     * @param o Object
     * @return equals
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if ( !(o instanceof Range)) {
            return false;
        }
        Range other = (Range) o;
        return ((min == other.min) && (max == other.max));
    }

    /**
     * set the values
     *
     * @param min min
     * @param max max
     */
    public void set(double min, double max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Format the min value
     * @return Min value formatted
     */
    public String formatMin() {
        return Misc.format(min);
    }

    /**
     * Format the mid  value
     * @return Mid value formatted
     */
    public String formatMid() {
        return Misc.format(getMid());
    }

    /**
     * Format the max  value
     * @return Max value formatted
     */
    public String formatMax() {
        return Misc.format(max);
    }


    /**
     * Get the min
     * @return  The min value
     */
    public double getMin() {
        return min;
    }

    /**
     * Get the max
     * @return  The max value
     */
    public double getMax() {
        return max;
    }

    /**
     * Set the min
     *
     * @param v  value
     */
    public void setMin(double v) {
        min = v;
    }

    /**
     * Set the max
     *
     * @param v  value
     */
    public void setMax(double v) {
        max = v;
    }

    /**
     * Get the int value of the span (the difference between max and min)
     * @return int value of span
     */
    public int getSpanInt() {
        return (int) getSpan();
    }

    /**
     * Get int value of min
     * @return int value of min
     */
    public int getMinInt() {
        return (int) min;
    }

    /**
     * Get int value of max
     * @return int value of max
     */
    public int getMaxInt() {
        return (int) max;
    }

    /**
     * Set the min
     *
     * @param v value
     */
    public void setMin(int v) {
        min = (double) v;
    }

    /**
     * Set the max
     *
     * @param v value
     */
    public void setMax(int v) {
        max = (double) v;
    }

    /**
     * Get a 2-aray array holding min/max
     * @return array of min and max
     */
    public double[] asArray() {
        return new double[] { min, max };
    }

    /**
     * Get a 2-aray array holding min/max
     * @return array of min and max
     */
    public float[] asFloatArray() {
        return new float[] { (float) min, (float) max };
    }

    /**
     * max-min
     * @return max-min
     */
    public double span() {
        return (max - min);
    }

    /**
     * max-min
     * @return max-min
     */
    public double getSpan() {
        return span();
    }

    /**
     * get abs(max-min)
     * @return abs(max-min)
     */
    public double getAbsSpan() {
        return Math.abs(span());
    }

    /**
     * Get the mid point
     * @return mid point
     */
    public double getMid() {
        return min + span() / 2.0;
    }

    /**
     * Get the int value of mid point
     * @return int value of mid point
     */
    public int getMidInt() {
        return (int) (min + span() / 2.0);
    }

    /**
     * Get percent along the way between min and max
     *
     * @param percent percent
     * @return value
     */
    public double getValueOfPercent(double percent) {
        return getMin() + getSpan() * percent;
    }

    /**
     * Ge tthe percent the given value is between min and max
     *
     * @param v value
     * @return percent
     */
    public double getPercent(double v) {
        return (v - min) / span();
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
     * to string
     * @return to string
     */
    public String toString() {
        return ((name != null)
                ? name + " "
                : "") + min + "/" + max;
    }





}


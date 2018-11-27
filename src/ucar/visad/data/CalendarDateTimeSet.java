/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

package ucar.visad.data;


import ucar.nc2.time.Calendar;

import visad.CoordinateSystem;
import visad.ErrorEstimate;
import visad.Gridded1DDoubleSet;
import visad.MathType;
import visad.QuickSort;
import visad.Set;
import visad.SetType;
import visad.Unit;
import visad.VisADException;


/**
 * A class to hold a Gridded1DDoubleSet and a calendar.   Units should always
 * be seconds since the epoch.
 */
public class CalendarDateTimeSet extends Gridded1DDoubleSet {

    /** serial version UID */
    private static final long serialVersionUID = 1L;

    /** the calendar */
    private Calendar calendar;

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type    the MathType
     * @param samples the values
     * @param lengthX the number of samples
     *
     * @throws VisADException  problem making the data
     */
    public CalendarDateTimeSet(MathType type, float[][] samples, int lengthX)
            throws VisADException {
        this(type, Set.floatToDouble(samples), lengthX, null);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type    the MathType
     * @param samples the values
     * @param lengthX the number of samples
     *
     * @throws VisADException  problem making the data
     */
    public CalendarDateTimeSet(MathType type, double[][] samples, int lengthX)
            throws VisADException {
        this(type, samples, lengthX, null, null, null, null);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param coord_sys   CoordinateSystem
     * @param units       the set units
     * @param errors      the errors
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, float[][] samples, int lengthX,
                               CoordinateSystem coord_sys, Unit[] units,
                               ErrorEstimate[] errors)
            throws VisADException {
        this(type, Set.floatToDouble(samples), lengthX, coord_sys, units,
             errors, true, null);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param coord_sys   CoordinateSystem
     * @param units       the set units
     * @param errors      the errors
     *
     * @throws VisADException problem creating the Set
     */
    public CalendarDateTimeSet(MathType type, double[][] samples,
                               int lengthX, CoordinateSystem coord_sys,
                               Unit[] units, ErrorEstimate[] errors)
            throws VisADException {
        this(type, samples, lengthX, coord_sys, units, errors, true, null);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param coord_sys   CoordinateSystem
     * @param units       the set units
     * @param errors      the errors
     * @param copy        true to copy the sample
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, float[][] samples, int lengthX,
                               CoordinateSystem coord_sys, Unit[] units,
                               ErrorEstimate[] errors, boolean copy)
            throws VisADException {
        this(type, Set.floatToDouble(samples), lengthX, coord_sys, units,
             errors, copy, null);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param coord_sys   CoordinateSystem
     * @param units       the set units
     * @param errors      the errors
     * @param copy        true to copy the sample
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, double[][] samples,
                               int lengthX, CoordinateSystem coord_sys,
                               Unit[] units, ErrorEstimate[] errors,
                               boolean copy)
            throws VisADException {
        this(type, samples, lengthX, coord_sys, units, errors, copy, null);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param cal      the associated Calendar
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, float[][] samples, int lengthX,
                               Calendar cal)
            throws VisADException {
        this(type, Set.floatToDouble(samples), lengthX, cal);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param cal      the associated Calendar
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, double[][] samples,
                               int lengthX, Calendar cal)
            throws VisADException {
        this(type, samples, lengthX, null, null, null, cal);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param coord_sys   CoordinateSystem
     * @param units       the set units
     * @param errors      the errors
     * @param copy        true to copy the sample
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, float[][] samples, int lengthX,
                               CoordinateSystem coord_sys, Unit[] units,
                               ErrorEstimate[] errors, Calendar cal)
            throws VisADException {
        this(type, Set.floatToDouble(samples), lengthX, coord_sys, units,
             errors, true, cal);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param coord_sys   CoordinateSystem
     * @param units       the set units
     * @param errors      the errors
     * @param copy        true to copy the sample
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, double[][] samples,
                               int lengthX, CoordinateSystem coord_sys,
                               Unit[] units, ErrorEstimate[] errors,
                               Calendar cal)
            throws VisADException {
        this(type, samples, lengthX, coord_sys, units, errors, true, cal);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param coord_sys   CoordinateSystem
     * @param units       the set units
     * @param errors      the errors
     * @param copy        true to copy the sample
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, float[][] samples, int lengthX,
                               CoordinateSystem coord_sys, Unit[] units,
                               ErrorEstimate[] errors, boolean copy,
                               Calendar cal)
            throws VisADException {
        this(type, Set.floatToDouble(samples), lengthX, coord_sys, units,
             errors, copy, cal);
    }

    /**
     * Construct a CalendarDateTimeSet
     *
     * @param type     the MathType
     * @param samples  the Samples
     * @param lengthX  number of samples
     * @param coord_sys   CoordinateSystem
     * @param units       the set units
     * @param errors      the errors
     * @param copy        true to copy the sample
     * @param cal         the associated Calendar
     *
     * @throws VisADException  problem with Set creation
     */
    public CalendarDateTimeSet(MathType type, double[][] samples,
                               int lengthX, CoordinateSystem coord_sys,
                               Unit[] units, ErrorEstimate[] errors,
                               boolean copy, Calendar cal)
            throws VisADException {
        super(type, samples, lengthX, coord_sys, units, errors, copy);
        this.calendar = cal;
    }

    /**
     * Get the Calendar that supports this
     *
     * @return  the Calendar
     */
    public Calendar getCalendar() {
        return calendar;
    }

    /**
     * @override
     */
    public Set merge1DSets(Set set) throws VisADException {

        if ((getDimension() != 1) || (set.getDimension() != 1)
                || equals(set)) {
            return this;
        }
        int length = getLength();
        // all indices in this
        int[] indices = getWedge();
        // all values in this
        double[][] old_values = indexToDouble(indices);  // WLH 21 Nov 2001
        // transform values from this to set
        ErrorEstimate[] errors_out = new ErrorEstimate[1];

        double[][] values = CoordinateSystem.transformCoordinates(
                                ((SetType) set.getType()).getDomain(),
                                set.getCoordinateSystem(), set.getSetUnits(),
                                null /* set.getSetErrors() */,
                                ((SetType) getType()).getDomain(),
                                getCoordinateSystem(), getSetUnits(),
                                null /* SetErrors */, old_values);
        // find indices of set not covered by this
        int       set_length  = set.getLength();
        boolean[] set_indices = new boolean[set_length];
        for (int i = 0; i < set_length; i++) {
            set_indices[i] = true;
        }
        if (set_length > 1) {
            // set indices for values in this
            int[] test_indices = set.doubleToIndex(values);
            try {
                for (int i = 0; i < length; i++) {
                    if (test_indices[i] > -1) {
                        set_indices[test_indices[i]] = false;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                throw new VisADException("Cannot merge sets");
            }
        } else {
            double[][] set_values = set.getDoubles();
            double     set_val    = set_values[0][0];
            double     min        = Double.MAX_VALUE;
            // double max = Double.MIN_VALUE;
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < length; i++) {
                if (values[0][i] > max) {
                    max = values[0][i];
                }
                if (values[0][i] < min) {
                    min = values[0][i];
                }
            }
            double delt = (max - min) / length;
            //System.out.println("min = " + min + " max = " + max + " delt = " + delt);
            if ((min - delt) <= set_val && (set_val <= (max + delt))) {
                set_indices[0] = false;
            }
        }

        // now set_indices = true for indices of set not covered by this
        int num_new = 0;
        for (int i = 0; i < set_length; i++) {
            if (set_indices[i]) {
                num_new++;
            }
        }
        if (num_new == 0) {
            return this;  // all covered, so nothing to do
        }
        // not all covered, so merge values of this with values of set
        // not covered; first get uncovered indices
        int[] new_indices = new int[num_new];
        num_new = 0;
        for (int i = 0; i < set_length; i++) {
            if (set_indices[i]) {
                new_indices[num_new] = i;
                num_new++;
            }
        }

        // get uncovered values
        double[][] new_values = set.indexToDouble(new_indices);

        // transform values for Units and CoordinateSystem
        new_values = CoordinateSystem.transformCoordinates(
            ((SetType) getType()).getDomain(), getCoordinateSystem(),
            getSetUnits(), null /* errors_out */,
            ((SetType) set.getType()).getDomain(), set.getCoordinateSystem(),
            set.getSetUnits(), null /* set.getSetErrors() */, new_values);

        // merge uncovered values with values of this
        double[][] all_values = new double[1][length + num_new];
        // WLH 21 Nov 2001
        for (int i = 0; i < length; i++) {
            all_values[0][i] = old_values[0][i];
        }
        for (int i = 0; i < num_new; i++) {
            all_values[0][length + i] = new_values[0][i];
        }

        // sort all_values then construct Gridded1DSet
        // just use ErrorEstimates from this
        QuickSort.sort(all_values[0]);
        return new CalendarDateTimeSet(getType(), all_values,
                                       all_values[0].length,
                                       getCoordinateSystem(), getSetUnits(),
                                       getSetErrors(), false, calendar);

    }

}

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

package ucar.unidata.view.geoloc;


import visad.CommonUnit;
import visad.Unit;


/**
 * A class to hold vertical range settings for a VisAD display from the
 * dialog box VertScaleDialog class. They are public member
 * data to simply access.  NB:  this should be VertRangeInfo, but history
 * trumps practicality
 *
 * @author   IDV Development Team
 */
public class VertScaleInfo extends ucar.visad.display.AxisScaleInfo {

    /** maximum range of the vertical scale */
    public double maxVertRange;

    /** minimum range of the vertical scale */
    public double minVertRange;

    /** Units of the range values */
    public Unit unit;

    /** Is visible */
    public boolean visible;
    
    /** The major increment. */
    private double majorIncrement;
    
    /** Minor division. */
    private int minorDivision;
    
    public static final int DEFAULT_MAJ_DIVISIONS = 4;
    public static final String DEFAULT_AXIS_LABEL = "Altitude";

    /**
     * Construct a <code>VertScaleInfo</code> with the specified range.
     * Unit is assumed to be meters.
     *
     * @param min  minimum of the range
     * @param max  maximum of the range
     */
    public VertScaleInfo(double min, double max) {
        this(min, max, CommonUnit.meter);
    }

    /**
     * Construct a <code>VertScaleInfo</code> with the specified range..
     *
     * @param min  minimum of the range
     * @param max  maximum of the range
     * @param unit unit of range values
     */
    public VertScaleInfo(double min, double max, Unit unit) {
        this.minVertRange = min;
        this.maxVertRange = max;
        this.unit         = unit;
        this.minorDivision = 1;
        this.visible = true;
        this.setVisible(true);
        this.majorIncrement = (max - min) / DEFAULT_MAJ_DIVISIONS;
        // Include units in what is shown to user in the display
        this.setLabel(DEFAULT_AXIS_LABEL + " (" + unit.getIdentifier() + ")");
    }

    /**
	 * @return the majorIncrement
	 */
	public double getMajorIncrement() {
		return majorIncrement;
	}

	/**
	 * @param majorIncrement the majorIncrement to set
	 */
	public void setMajorIncrement(double majorIncrement) {
		this.majorIncrement = majorIncrement;
	}

	/**
	 * @return the minorDivision
	 */
	public int getMinorDivision() {
		return minorDivision;
	}

	/**
	 * @param minorDivision the minorDivision to set
	 */
	public void setMinorDivision(int minorDivision) {
		this.minorDivision = minorDivision;
	}

	/**
     * _more_
     *
     * @return _more_
     */
    @Override

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime  = 31;
        int       result = 1;
        long      temp;

        temp   = Double.doubleToLongBits(maxVertRange);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp   = Double.doubleToLongBits(minVertRange);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((unit == null)
                                   ? 0
                                   : unit.hashCode());
        result = prime * result + (visible
                                   ? 1231
                                   : 1237);

        return result;
    }

    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    @Override

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        VertScaleInfo other = (VertScaleInfo) obj;

        if (Double.doubleToLongBits(maxVertRange)
                != Double.doubleToLongBits(other.maxVertRange)) {
            return false;
        }

        if (Double.doubleToLongBits(minVertRange)
                != Double.doubleToLongBits(other.minVertRange)) {
            return false;
        }

        if (unit == null) {
            if (other.unit != null) {
                return false;
            }
        } else if ( !unit.equals(other.unit)) {
            return false;
        }

        if (visible != other.visible) {
            return false;
        }
        
        if (majorIncrement != other.majorIncrement) {
            return false;
        }
        
        if (minorDivision != other.minorDivision) {
            return false;
        }

        return true;
    }
}

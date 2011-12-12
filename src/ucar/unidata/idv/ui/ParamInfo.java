/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.ui;


import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;



import visad.Unit;



/**
 * This is used by the {@link ParamDefaultsEditor}
 * to hold the default display information for a parameter.
 * Holds a simple min/max range and other metadata about a parameters data, including
 * unit, color table, and contouring values; with get and set methods.
 *
 */
public class ParamInfo {

    /** The parameter name or pattern to match */
    private String name;

    /** The name of the color table */
    private String colorTableName;

    /** The colortable. This is based on the colorTableName */
    private ColorTable colorTable;


    /** The data range. */
    private Range range;

    /** The contour info */
    private ContourInfo contourInfo;

    /** The display unit to use */
    private Unit displayUnit;


    /**
     * ctor
     */
    public ParamInfo() {}


    /**
     * Create this ParamInfo. Initialize it with the
     * state held in the given other ParamInfo
     *
     * @param other The ParamInfo to initialize from
     */
    public ParamInfo(ParamInfo other) {
        this(other.name, other.colorTableName, other.range,
             other.contourInfo, other.displayUnit);
    }




    /**
     * Create this ParamInfo.
     *
     * @param name The name
     * @param colorTableName The name of the color table
     * @param range The range
     * @param ci The ContourInfo
     * @param displayUnit The display unit
     */
    public ParamInfo(String name, String colorTableName, Range range,
                     ContourInfo ci, Unit displayUnit) {
        this.name           = name;
        this.colorTableName = colorTableName;
        this.range          = range;
        this.contourInfo    = ci;
        this.displayUnit    = displayUnit;
    }

    /**
     * Copy the state
     *
     * @param that Other object to init with
     */
    public void initWith(ParamInfo that) {
        this.name           = that.name;
        this.colorTableName = that.colorTableName;
        this.range          = that.range;
        this.contourInfo    = that.contourInfo;
        this.displayUnit    = that.displayUnit;

    }


    /**
     * Does this have a Range defined
     *
     * @return Is range non-null
     */
    public boolean hasRange() {
        return (range != null);
    }

    /**
     * Does this have a ContourInfo defined
     *
     * @return Is contourInfo non-null
     */
    public boolean hasContourInfo() {
        return (contourInfo != null);
    }

    /**
     * Does this have a display unit defined
     *
     * @return Is displayUnit non-null
     */
    public boolean hasDisplayUnit() {
        return (displayUnit != null);
    }

    /**
     * Does this have a color table name defined
     *
     * @return Is colorTableName non-null
     */
    public boolean hasColorTableName() {
        return (colorTableName != null);
    }

    /**
     * Set the minimum value for the Range. Create it
     * if it is null
     *
     * @param m The minimum range value
     */
    public void setMin(double m) {
        if (range == null) {
            range = new Range(m, m);
        } else {
            range.setMin(m);
        }
    }


    /**
     * Set the maximum value for the Range. Create it
     * if it is null
     *
     * @param m The maximum range value
     */

    public void setMax(double m) {
        if (range == null) {
            range = new Range(m, m);
        } else {
            range.setMax(m);
        }
    }

    /**
     * Get the minimum range value or 0.0 if range is undefined.
     *
     * @return Min range value
     */
    public double getMin() {
        if (range != null) {
            return range.getMin();
        }
        return 0.0;
    }

    /**
     * Get the maximum range value or 0.0 if range is undefined.
     *
     * @return Max range value
     */
    public double getMax() {
        if (range != null) {
            return range.getMax();
        }
        return 0.0;
    }


    /**
     * Set the range with the given
     * {@link ucar.unidata.util.Range} value
     *
     * @param value The new range value
     */
    public void setRange(Range value) {
        range = new Range(value);
    }

    /**
     * Get the {@link ucar.unidata.util.Range}
     *
     * @return The range value
     */
    public Range getRange() {
        return range;
    }

    /**
     * Set the {@link ucar.unidata.util.ContourInfo}
     *
     * @param value The new  contour info
     */
    public void setContourInfo(ContourInfo value) {
        if (value == null) {
            contourInfo = null;
        } else {
            contourInfo = new ContourInfo(value);
        }
    }

    /**
     * Get the {@link ucar.unidata.util.ContourInfo}
     *
     * @return The  contour info
     */
    public ContourInfo getContourInfo() {
        return contourInfo;
    }

    /**
     * Set the color table name
     *
     * @param value The color table name
     */
    public void setColorTableName(String value) {
        colorTableName = value;
    }

    /**
     * Get the color table name
     *
     * @return The color table name
     */
    public String getColorTableName() {
        return colorTableName;
    }

    /**
     * Set the colorTableName to null
     */
    public void clearColorTableName() {
        colorTableName = null;
    }

    /**
     * Set the displayUnit to null
     */
    public void clearDisplayUnit() {
        displayUnit = null;
    }

    /**
     * Set the range to null
     */
    public void clearRange() {
        range = null;
    }

    /**
     * Set the contourInfo to null
     */
    public void clearContourInfo() {
        contourInfo = null;
    }




    /**
     * Set the nae
     *
     * @param value The new name value
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the name
     *
     * @return The name value
     */
    public String getName() {
        return name;
    }

    /**
     * Get the displayUnit
     *
     * @return The display unit
     */
    public Unit getDisplayUnit() {
        return displayUnit;
    }

    /**
     * Set the displayUnit
     *
     * @param newUnit The new display unit
     */
    public void setDisplayUnit(Unit newUnit) {
        displayUnit = newUnit;
    }


    /**
     * Return the name
     *
     * @return The name
     */
    public String toString() {
        return name;
    }


    /**
     * Override equals
     *
     * @param o The object
     * @return Is equals to the object
     */
    public boolean equals(Object o) {
        if ( !(o instanceof ParamInfo)) {
            return false;
        }
        ParamInfo other = (ParamInfo) o;
        boolean   ok    = true;
        return Misc.equals(colorTableName, other.colorTableName)
               && Misc.equals(range, other.range)
               && Misc.equals(displayUnit, other.displayUnit)
               && Misc.equals(contourInfo, other.contourInfo);
    }


}

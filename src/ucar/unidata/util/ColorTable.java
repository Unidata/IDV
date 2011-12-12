/*
 * $Id: ColorTable.java,v 1.30 2006/12/18 22:55:00 jeffmc Exp $
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


import java.awt.Color;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import java.util.Vector;


/**
 * Holds a name and a 2D float array that represents a color table
 *
 * @author IDV development team
 */
public class ColorTable implements NamedObject {

    /** _more_ */
    public static final int IDX_RED = 0;

    /** _more_ */
    public static final int IDX_GREEN = 1;

    /** _more_ */
    public static final int IDX_BLUE = 2;

    /** _more_ */
    public static final int IDX_ALPHA = 3;

    /** The ColorTable id */
    private String id;

    /** The ColorTable name */
    private String name;

    /** The ColorTable description */
    private String description;

    /** default category */
    private String category = CATEGORY_BASIC;

    /** active flag */
    private boolean active = true;

    /** table of values */
    private float[][] tableArray;

    /** table of values */
    private float[] scaleFactor;

    /** list of colors */
    private ArrayList<Color> colorList;

    /** breakpoints */
    private ArrayList breakpoints;


    /** color table range */
    private Range range;

    /** The "Basic" category */
    public static final String CATEGORY_BASIC = "Basic";

    /** The Miscellaneous category */
    public static final String CATEGORY_MISC = "Misc.";

    /** The Satellite category */
    public static final String CATEGORY_SATELLITE = "Satellite";

    /** The Radar category */
    public static final String CATEGORY_RADAR = "Radar";

    /** The solid category */
    public static final String CATEGORY_SOLID = "Solid";

    /** the Atd-Radar category */
    public static final String CATEGORY_ATD_RADAR = "Radar>ATD";

    /**
     * Copy constructor
     *
     * @param otherTable  other table to copy
     */
    public ColorTable(ColorTable otherTable) {
        if (otherTable != null) {
            name        = otherTable.name;
            description = otherTable.description;
            category    = otherTable.category;
            if (otherTable.tableArray != null) {
                tableArray = new float[otherTable.tableArray.length][];
                for (int i = 0; i < otherTable.tableArray.length; i++) {
                    tableArray[i] =
                        (float[]) otherTable.tableArray[i].clone();
                }
            }
            if (otherTable.breakpoints != null) {
                breakpoints = new ArrayList(otherTable.breakpoints);
            }
            range = new Range(otherTable.range);
        }
    }


    /**
     * Default constructor
     */
    public ColorTable() {
        this(null, CATEGORY_BASIC, (float[][]) null);
    }

    /**
     * Create a new ColorTable.
     *
     * @param name      name of the table
     * @param category  category of the table
     * @param table     table values
     *
     */
    public ColorTable(String name, String category, float[][] table) {
        this(name, name, category, table);
    }


    /**
     * Create a new ColorTable.
     *
     * @param id        id of the table
     * @param name      name of the table
     * @param category  category of the table
     * @param table     table values
     */
    public ColorTable(String id, String name, String category,
                      float[][] table) {
        this.id         = id;
        this.tableArray = table;
        this.name       = name;
        this.category   = category;
    }

    /**
     * Create a new ColorTable.
     *
     * @param id        id of the table
     * @param name      name of the table
     * @param category  category of the table
     * @param table     table values
     * @param tableFlipped  flipped flag
     */
    public ColorTable(String id, String name, String category,
                      float[][] table, boolean tableFlipped) {
        this.id       = id;
        this.name     = name;
        this.category = category;
        if ( !tableFlipped) {
            this.tableArray = table;
        } else {
            this.tableArray = new float[table[0].length][table.length];
            for (int i = 0; i < table.length; i++) {
                for (int j = 0; j < table[0].length; j++) {
                    this.tableArray[j][i] = table[i][j];
                }
            }
        }
    }


    /**
     * Initialize a color table a new ColorTable.
     *
     * @param name         name of the table
     * @param category     category of the table
     * @param colors       table colors
     * @param breakpoints  breakpoints
     * @param r            Range of values
     *
     * @return filled in ColorTable
     */
    public ColorTable init(String name, String category, ArrayList colors,
                           ArrayList breakpoints, Range r) {
        return init(name, category, colors, null, breakpoints, r);
    }

    /**
     * Initialize a color table a new ColorTable.
     *
     * @param name         name of the table
     * @param category     category of the table
     * @param colors       table colors
     * @param scales _more_
     * @param breakpoints  breakpoints
     * @param r            Range of values
     *
     * @return filled in ColorTable
     */

    public ColorTable init(String name, String category, ArrayList colors,
                           ArrayList scales, ArrayList breakpoints, Range r) {
        colorList        = null;
        this.range       = new Range(r);
        this.name        = name;
        this.category    = category;
        this.breakpoints = breakpoints;
        scaleFactor      = null;
        if ((scales != null) && (scales.size() == colors.size())) {
            scaleFactor = new float[scales.size()];
            for (int i = 0; i < scales.size(); i++) {
                scaleFactor[i] = ((Float) scales.get(i)).floatValue();
            }
        }
        setTable(colors);
        return this;
    }

    /**
     * See if another color table is equivalent to this one
     *
     * @param other   other table to check
     * @return true ifthey are equal.
     */
    public boolean equalsTable(ColorTable other) {
        if (other == null) {
            return false;
        }
        if ( !Misc.equals(getName(), other.getName())) {
            return false;
        }

        int max = 3;
        if ((tableArray == null) || (other.tableArray == null)) {
            return false;
        }
        if (tableArray.length == other.tableArray.length) {
            max = tableArray.length;
        }

        for (int row = 0; row < max; row++) {
            if ( !java.util.Arrays.equals(tableArray[row],
                                          other.tableArray[row])) {
                return false;
            }
        }
        //If one table has 4 rows and the other has 3 then check if  the one with 4 
        //is fully opaque
        if (tableArray.length != other.tableArray.length) {
            if (tableArray.length == 4) {
                return allEquals(tableArray[3], 1.0f);
            } else {
                return allEquals(other.tableArray[3], 1.0f);
            }
        }
        return true;
    }

    /**
     * See if all the values in the array are equal to the number.
     *
     * @param a    array to check
     * @param num  number to compare
     * @return true if all values are equal to num.
     */
    private boolean allEquals(float[] a, float num) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != num) {
                return false;
            }
        }
        return true;
    }

    /**
     * Set the range for this table.
     *
     * @param r  range
     */
    public void setRange(Range r) {
        range = r;
    }

    /**
     * Get the Range for this color table.
     * @return the range
     */
    public Range getRange() {
        return range;
    }

    /**
     * Set the active flag.
     *
     * @param a  true for active
     */
    public void setActive(boolean a) {
        active = a;
    }

    /**
     * Get the active flag.
     * @return true if active
     */
    public boolean getActive() {
        return active;
    }

    /**
     * Set the breakpoints for this color table.
     *
     * @param l   list of breakpoints
     */
    public void setBreakpoints(ArrayList l) {
        breakpoints = l;
    }

    /**
     * Get the breakpoints for this table.
     * @return the breakpoints
     */
    public ArrayList getBreakpoints() {
        return breakpoints;
    }


    /**
     * Get the table without alpha values.
     * @return table with alpha removed.
     */
    public float[][] getNonAlphaTable() {
        return removeAlpha(getColorTable());
    }

    /**
     * Get the table with alpha.
     * @return the alpha table
     */
    public float[][] getAlphaTable() {
        return addAlpha(getColorTable());
    }


    /**
     * Get the color table pallette. This applies any scale factor.
     * @return the table of values
     */
    public float[][] getColorTable() {
        if (scaleFactor != null) {
            float[][] newArray = new float[tableArray.length][];
            for (int i = 0; i < tableArray.length; i++) {
                newArray[i] = (float[]) tableArray[i].clone();
                for (int j = 0; j < newArray[i].length; j++) {
                    if (j < scaleFactor.length) {
                        newArray[i][j] = Math.min(1.0f,
                                scaleFactor[j] * newArray[i][j]);
                    }
                }
            }
            return newArray;
        }
        return tableArray;
    }


    /**
     * Set the color table pallette
     * Note: this is around for legacy purposes and we will move to
     * a new setTableArray/getTableArray method
     *
     * @param table  table of values
     */
    public void setTable(float[][] table) {
        tableArray = table;
    }


    /**
     * Set the color table pallette
     * Note: this is around for legacy purposes and we will move to
     * a new setTableArray/getTableArray method
     *
     * @return _more_
     */
    public float[][] getTable() {
        return tableArray;
    }


    /**
     * _more_
     *
     * @param table _more_
     */
    public void setTableArray(float[][] table) {
        tableArray = table;
    }

    /**
     * Get the name of this table.
     * @return the name of this table
     */
    public String getName() {
        return name;
    }


    /**
     * Set the name of this table
     *
     * @param name  the new name
     */
    public void setName(String name) {
        this.name = name;
        if (id == null) {
            id = name;
        }
    }

    /**
     * Get the ID of this table
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id of this table
     *
     * @param id  the table id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the table description
     * @return the table description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the table description
     *
     * @param description  the table description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the table category.
     * @return the table category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Set the table category.
     *
     * @param category  the new category
     */
    public void setCategory(String category) {
        this.category = category;
        if (category == null) {
            this.category = CATEGORY_BASIC;
        }
    }


    /**
     * Output the table as a String.
     * @return the table values as a string.
     */
    public String dataToString() {
        StringBuffer sb    = new StringBuffer();
        float[][]    table = getColorTable();
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[0].length; j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append("" + table[i][j]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    /**
     * Print the table info
     * @return the table info
     */
    public String print() {
        return this + " size:" + tableArray[0].length;
    }

    /**
     * Get a descriptive String for this object.
     * @return a descriptive String for this object.
     */
    public String toString() {
        return name;
    }


    /**
     * Set the table with a list of colors
     *
     * @param colors  list of colors
     */
    public void setTable(ArrayList colors) {
        //TODO: add in the transparency if we have it.
        int length = 3;
        if (tableArray != null) {
            length = tableArray.length;
        }
        length = 4;
        float[][] oldTable = tableArray;
        tableArray = new float[length][colors.size()];
        float[] comps = { 0.0f, 0.0f, 0.0f, 0.0f };
        for (int i = 0; i < colors.size(); i++) {
            Color c = (Color) colors.get(i);
            comps                    = c.getRGBComponents(comps);
            tableArray[IDX_RED][i]   = comps[0];
            tableArray[IDX_GREEN][i] = comps[1];
            tableArray[IDX_BLUE][i]  = comps[2];
            tableArray[IDX_ALPHA][i] = comps[3];
        }
        colorList = null;
    }

    /**
     * Convert the colorTable into a list of Color-s
     * @return list of colors
     */
    public ArrayList<Color> getColorList() {
        if ((colorList == null) && (tableArray != null)) {
            colorList = new ArrayList<Color>();
            boolean haveAlpha = (tableArray.length == 4);
            int     length    = tableArray[0].length;
            for (int i = 0; i < length; i++) {
                colorList.add(new Color(tableArray[IDX_RED][i],
                                        tableArray[IDX_GREEN][i],
                                        tableArray[IDX_BLUE][i], (haveAlpha
                        ? tableArray[IDX_ALPHA][i]
                        : 1.0f)));
            }
        }
        return colorList;
    }

    /**
     * Wrapper around {@link #addAlpha(float[][])}.
     *
     * @param colorTable  color table to use
     * @return color table values
     */
    public static final float[][] addAlpha(ColorTable colorTable) {
        return addAlpha(colorTable.getColorTable());
    }

    /**
     * Remove the alpha values
     *
     * @param colorTable  color table values
     * @return color table values without alpha
     */
    public static final float[][] removeAlpha(float[][] colorTable) {
        if (colorTable == null) {
            return colorTable;
        }
        if (colorTable.length == 3) {
            return colorTable;
        }
        int       len      = (colorTable[0]).length;
        float[][] newTable = new float[3][len];
        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                newTable[n][m] = colorTable[n][m];
            }
        }
        return newTable;
    }

    /**
     * Make a color-alpha table for VisAD displays mapped to Display.RGBA;
     * the 4th entry at each level being alpha (1.0) for transparency.
     *
     * @param colorTable a float [3][len], any usual VisAd color table
     * @return float[4][len] the color table
     *
     */
    public static final float[][] addAlpha(float[][] colorTable) {
        if (colorTable.length == 4) {
            return colorTable;
        }

        // get length of incoming array
        int       len     = (colorTable[0]).length;

        float[][] caTable = new float[4][len];

        // copy color values
        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                caTable[n][m] = colorTable[n][m];
            }
        }

        // add in alpha value of 1.0 = fully opaque.
        int n = 3;
        for (int m = 0; m < len; m++) {
            caTable[n][m] = 1.0f;
        }

        return caTable;
    }


    /**
     * _more_
     *
     * @param alpha _more_
     */
    public void setTransparency(float alpha) {
        tableArray = changeTransparency(tableArray, alpha);
        colorList  = null;
    }



    /**
     * Revise a color-alpha table for VisAD displays mapped to Display.RGBA;
     * the 4th entry at each level (alpha) is reset to input arg alpha
     *
     * @param colorTable a float [4][len], a VisAd color-alpha table
     * @param alpha the new alpha value
     * @return float[4][len] the revised color table: same colors; new alpha
     *
     */
    public static final float[][] changeTransparency(float[][] colorTable,
            float alpha) {
        // get length of incoming array
        int len = (colorTable[0]).length;

        // make a new one since don't won't weird effects on
        // onther displays using same table!
        float[][] newCT = new float[4][len];

        // copy color values from old table to new table
        for (int n = 0; n < 3; n++) {
            for (int m = 0; m < len; m++) {
                newCT[n][m] = colorTable[n][m];
            }
        }

        // replace old alpha value with the new one
        int n = 3;
        for (int m = 0; m < len; m++) {
            newCT[n][m] = alpha;
        }

        return newCT;
    }

    /**
     * Set the ScaleFactor property.
     *
     * @param value The new value for ScaleFactor
     */
    public void setScaleFactor(float[] value) {
        scaleFactor = value;
    }

    /**
     * Get the ScaleFactor property.
     *
     * @return The ScaleFactor
     */
    public float[] getScaleFactor() {
        return scaleFactor;
    }



    /**
     * Class Breakpoint _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.30 $
     */
    public static class Breakpoint {

        /** _more_ */
        private double value = 0.0;

        /** _more_ */
        private boolean locked = false;

        /**
         * _more_
         */
        public Breakpoint() {}


        /**
         * _more_
         *
         * @param v _more_
         */
        public Breakpoint(double v) {
            this(v, false);
        }

        /**
         * _more_
         *
         * @param v _more_
         * @param locked _more_
         */
        public Breakpoint(double v, boolean locked) {
            this.value  = v;
            this.locked = locked;
        }

        /**
         * _more_
         *
         * @param that _more_
         */
        public Breakpoint(Breakpoint that) {
            if (that != null) {
                this.value  = that.value;
                this.locked = that.locked;
            }
        }

        /**
         * Set the Value property.
         *
         * @param value The new value for Value
         */
        public void setValue(double value) {
            this.value = value;
        }

        /**
         * Get the Value property.
         *
         * @return The Value
         */
        public double getValue() {
            return value;
        }

        /**
         * Set the Locked property.
         *
         * @param value The new value for Locked
         */
        public void setLocked(boolean value) {
            locked = value;
        }

        /**
         * Get the Locked property.
         *
         * @return The Locked
         */
        public boolean getLocked() {
            return locked;
        }


        /**
         * _more_
         *
         * @param l _more_
         *
         * @return _more_
         */
        public static ArrayList cloneList(List l) {
            if ((l == null) || (l.size() == 0)) {
                return new ArrayList();
            }
            List tmp = l;
            l = new ArrayList();
            if (tmp.get(0) instanceof Double) {
                for (int i = 0; i < tmp.size(); i++) {
                    l.add(new Breakpoint(
                        ((Double) tmp.get(i)).doubleValue()));
                }
            } else {
                for (int i = 0; i < tmp.size(); i++) {
                    l.add(new Breakpoint((Breakpoint) tmp.get(i)));
                }
            }
            return new ArrayList(l);


        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return "" + value + " " + locked;
        }

    }



}


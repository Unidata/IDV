/*
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

// $Id: GridVertCoord.java 63 2006-07-12 21:50:51Z edavis $

package ucar.unidata.data.grid.gempak;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.units.SimpleUnit;

import java.util.*;


/**
 * A Vertical Coordinate variable for a Grid variable.
 *
 * @author caron
 * @version $Revision: 63 $ $Date: 2006-07-12 15:50:51 -0600 (Wed, 12 Jul 2006) $
 */
public class GridVertCoord implements Comparable {

    /** _more_          */
    static private org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(GridVertCoord.class);

    /** _more_          */
    private GridRecord typicalRecord;

    /** _more_          */
    private String levelName;

    /** _more_          */
    private GridTableLookup lookup;

    /** _more_          */
    private int seq = 0;

    /** _more_          */
    private double[] coordValues;

    /** _more_          */
    boolean usesBounds = false;

    /** _more_          */
    boolean dontUseVertical = false;

    /** _more_          */
    String positive = "up";

    /** _more_          */
    String units;

    /** _more_          */
    private boolean debug = false;

    /**
     * _more_
     *
     * @param name _more_
     */
    GridVertCoord(String name) {
        this.levelName  = name;
        dontUseVertical = true;
    }

    /**
     * _more_
     *
     * @param records _more_
     * @param levelName _more_
     * @param lookup _more_
     */
    GridVertCoord(List records, String levelName, GridTableLookup lookup) {
        this.typicalRecord = (GridRecord) records.get(0);
        this.levelName     = levelName;
        this.lookup        = lookup;

        dontUseVertical    = !lookup.isVerticalCoordinate(typicalRecord);
        positive           = lookup.isPositiveUp(typicalRecord)
                             ? "up"
                             : "down";
        units              = lookup.getLevelUnit(typicalRecord);

        usesBounds         = lookup.isLayer(this.typicalRecord);
        addLevels(records);

        if (debug) {
            System.out.println("GribVertCoord: " + getVariableName() + "("
                               + typicalRecord.getLevelType1()
                               + ") useVertical= " + ( !dontUseVertical)
                               + " positive=" + positive + " units=" + units);
        }
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param levelName _more_
     * @param lookup _more_
     * @param level1 _more_
     * @param level2 _more_
     */
    GridVertCoord(GridRecord record, String levelName,
                  GridTableLookup lookup, double[] level1, double[] level2) {
        this.typicalRecord = record;
        this.levelName     = levelName;
        this.lookup        = lookup;

        dontUseVertical    = !lookup.isVerticalCoordinate(record);
        positive           = lookup.isPositiveUp(record)
                             ? "up"
                             : "down";
        units              = lookup.getLevelUnit(record);
        usesBounds         = lookup.isLayer(this.typicalRecord);

        levels             = new ArrayList(level1.length);
        for (int i = 0; i < level1.length; i++) {
            levels.add(new LevelCoord(level1[i], (level2 == null)
                    ? 0.0
                    : level2[i]));
        }

        Collections.sort(levels);
        if (positive.equals("down")) {
            Collections.reverse(levels);
        }
    }

    /**
     * _more_
     *
     * @param seq _more_
     */
    void setSequence(int seq) {
        this.seq = seq;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    String getLevelName() {
        return levelName;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    String getVariableName() {
        return (seq == 0)
               ? levelName
               : levelName + seq;  // more than one with same levelName
    }

    /**
     * _more_
     *
     * @return _more_
     */
    int getNLevels() {
        return dontUseVertical
               ? 1
               : levels.size();
    }

    /**
     * _more_
     *
     * @param records _more_
     */
    void addLevels(List records) {
        for (int i = 0; i < records.size(); i++) {
            GridRecord record = (GridRecord) records.get(i);

            if (coordIndex(record) < 0) {
                levels.add(new LevelCoord(record.getLevel1(),
                                          record.getLevel2()));
                if (dontUseVertical && (levels.size() > 1)) {
                    if (debug) {
                        logger.warn(
                            "GribCoordSys: unused level coordinate has > 1 levels = "
                            + levelName + " " + record.getLevelType1() + " "
                            + levels.size());
                    }
                }
            }
        }
        Collections.sort(levels);
        if (positive.equals("down")) {
            Collections.reverse(levels);
        }
    }

    /**
     * _more_
     *
     * @param records _more_
     *
     * @return _more_
     */
    boolean matchLevels(List records) {

        // first create a new list
        ArrayList levelList = new ArrayList(records.size());
        for (int i = 0; i < records.size(); i++) {
            GridRecord record = (GridRecord) records.get(i);
            LevelCoord lc = new LevelCoord(record.getLevel1(),
                                           record.getLevel2());
            if ( !levelList.contains(lc)) {
                levelList.add(lc);
            }
        }

        Collections.sort(levelList);
        if (positive.equals("down")) {
            Collections.reverse(levelList);
        }

        // gotta equal existing list
        return levelList.equals(levels);
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     * @param g _more_
     */
    void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
        if (dontUseVertical) {
            return;
        }
        int nlevs = levels.size();
        ncfile.addDimension(g, new Dimension(getVariableName(), nlevs, true));
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     * @param g _more_
     */
    void addToNetcdfFile(NetcdfFile ncfile, Group g) {
        if (dontUseVertical) {
            return;
        }

        if (g == null) {
            g = ncfile.getRootGroup();
        }

        // coordinate axis
        Variable v = new Variable(ncfile, g, null, getVariableName());
        v.setDataType(DataType.DOUBLE);

        String desc = lookup.getLevelDescription(typicalRecord);
        // TODO: figure this out
        //boolean isGrib1 = lookup instanceof Grib1Lookup;
        boolean isGrib1 = true;
        if ( !isGrib1 && usesBounds) {
            desc = "Layer between " + desc;
        }

        v.addAttribute(new Attribute("long_name", desc));
        v.addAttribute(new Attribute("units",
                                     lookup.getLevelUnit(typicalRecord)));

        // positive attribute needed for CF-1 Height and Pressure
        if (positive != null) {
            v.addAttribute(new Attribute("positive", positive));
        }

        if (units != null) {
            AxisType axisType;
            if (SimpleUnit.isCompatible("millibar", units)) {
                axisType = AxisType.Pressure;
            } else if (SimpleUnit.isCompatible("m", units)) {
                axisType = AxisType.Height;
            } else {
                axisType = AxisType.GeoZ;
            }

            v.addAttribute(
                new Attribute(
                    "GRIB_level_type",
                    Integer.toString(typicalRecord.getLevelType1())));
            v.addAttribute(new Attribute(_Coordinate.AxisType,
                                         axisType.toString()));
        }

        if (coordValues == null) {
            coordValues = new double[levels.size()];
            for (int i = 0; i < levels.size(); i++) {
                LevelCoord lc = (LevelCoord) levels.get(i);
                coordValues[i] = lc.mid;
            }
        }
        Array dataArray = Array.factory(DataType.DOUBLE.getClassType(),
                                        new int[] { coordValues.length },
                                        coordValues);

        v.setDimensions(getVariableName());
        v.setCachedData(dataArray, true);

        ncfile.addVariable(g, v);

        if (usesBounds) {
            String boundsDimName = "bounds_dim";
            if (g.findDimension(boundsDimName) == null) {
                ncfile.addDimension(g, new Dimension(boundsDimName, 2, true));
            }

            String bname = getVariableName() + "_bounds";
            v.addAttribute(new Attribute("bounds", bname));
            v.addAttribute(new Attribute(_Coordinate.ZisLayer, "true"));

            Variable b = new Variable(ncfile, g, null, bname);
            b.setDataType(DataType.DOUBLE);
            b.setDimensions(getVariableName() + " " + boundsDimName);
            b.addAttribute(new Attribute("long_name",
                                         "bounds for " + v.getName()));
            b.addAttribute(new Attribute("units",
                                         lookup.getLevelUnit(typicalRecord)));

            Array boundsArray = Array.factory(DataType.DOUBLE.getClassType(),
                                    new int[] { coordValues.length,
                    2 });
            ucar.ma2.Index ima = boundsArray.getIndex();
            for (int i = 0; i < coordValues.length; i++) {
                LevelCoord lc = (LevelCoord) levels.get(i);
                boundsArray.setDouble(ima.set(i, 0), lc.value1);
                boundsArray.setDouble(ima.set(i, 1), lc.value2);
            }
            b.setCachedData(boundsArray, true);

            ncfile.addVariable(g, b);
        }
    }

    /**
     * _more_
     *
     * @param record _more_
     *
     * @return _more_
     */
    int getIndex(GridRecord record) {
        if (dontUseVertical) {
            return 0;
        }
        return coordIndex(record);
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public int compareTo(Object o) {
        GridVertCoord gv = (GridVertCoord) o;
        return getLevelName().compareToIgnoreCase(gv.getLevelName());
    }

    /** _more_          */
    private ArrayList levels = new ArrayList();  // LevelCoord

    /**
     * Class LevelCoord _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class LevelCoord implements Comparable {

        /** _more_          */
        double mid;

        /** _more_          */
        double value1, value2;

        /**
         * _more_
         *
         * @param value1 _more_
         * @param value2 _more_
         */
        LevelCoord(double value1, double value2) {
            this.value1 = value1;
            this.value2 = value2;
            if (usesBounds && (value1 > value2)) {
                this.value1 = value2;
                this.value2 = value1;
            }
            mid = usesBounds
                  ? (value1 + value2) / 2
                  : value1;
        }

        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
        public int compareTo(Object o) {
            LevelCoord other = (LevelCoord) o;
            // if (closeEnough(value1, other.value1) && closeEnough(value2, other.value2)) return 0;
            if (mid < other.mid) {
                return -1;
            }
            if (mid > other.mid) {
                return 1;
            }
            return 0;
        }

        /**
         * _more_
         *
         * @param oo _more_
         *
         * @return _more_
         */
        public boolean equals(Object oo) {
            if (this == oo) {
                return true;
            }
            if ( !(oo instanceof LevelCoord)) {
                return false;
            }
            LevelCoord other = (LevelCoord) oo;
            return (ucar.nc2.util.Misc.closeEnough(value1, other.value1)
                    && ucar.nc2.util.Misc.closeEnough(value2, other.value2));
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int hashCode() {
            return (int) (value1 * 100000 + value2 * 100);
        }
    }


    /**
     * _more_
     *
     * @param record _more_
     *
     * @return _more_
     */
    private int coordIndex(GridRecord record) {
        double val  = record.getLevel1();
        double val2 = record.getLevel2();
        if (usesBounds && (val > val2)) {
            val  = record.getLevel2();
            val2 = record.getLevel1();
        }

        for (int i = 0; i < levels.size(); i++) {
            LevelCoord lc = (LevelCoord) levels.get(i);
            if (usesBounds) {
                if (ucar.nc2.util.Misc.closeEnough(lc.value1, val)
                        && ucar.nc2.util.Misc.closeEnough(lc.value2, val2)) {
                    return i;
                }
            } else {
                if (ucar.nc2.util.Misc.closeEnough(lc.value1, val)) {
                    return i;
                }
            }
        }
        return -1;
    }

}


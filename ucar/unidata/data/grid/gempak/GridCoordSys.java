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

// $Id:GridCoordSys.java 63 2006-07-12 21:50:51Z edavis $

package ucar.unidata.data.grid.gempak;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.units.SimpleUnit;

import java.util.*;


/**
 * A Coordinate System for a Grid variable.
 * @author john
 */
public class GridCoordSys {

    /** _more_ */
    private GridHorizCoordSys hcs;

    /** _more_ */
    private GridRecord record;

    /** _more_ */
    private String verticalName;

    /** _more_ */
    private GridTableLookup lookup;

    /** _more_ */
    private List levels;

    /** _more_ */
    boolean dontUseVertical = false;

    /** _more_ */
    private boolean debug = false;

    /** _more_ */
    String positive = "up";

    /** _more_ */
    String units;

    /**
     * _more_
     *
     * @param hcs _more_
     * @param record _more_
     * @param name _more_
     * @param lookup _more_
     */
    GridCoordSys(GridHorizCoordSys hcs, GridRecord record, String name,
                 GridTableLookup lookup) {
        this.hcs          = hcs;
        this.record       = record;
        this.verticalName = name;
        this.lookup       = lookup;
        this.levels       = new ArrayList();

        dontUseVertical   = !lookup.isVerticalCoordinate(record);
        positive          = lookup.isPositiveUp(record)
                            ? "up"
                            : "down";
        units             = lookup.getLevelUnit(record);

        if (debug) {
            System.out.println("GribCoordSys: " + getVerticalDesc()
                               + " useVertical= " + ( !dontUseVertical)
                               + " positive=" + positive + " units=" + units);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    String getCoordSysName() {
        return verticalName + "_CoordSys";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    String getVerticalName() {
        return verticalName;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    String getVerticalDesc() {
        return verticalName + "(" + record.getLevelType1() + ")";
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
            Double     d      = new Double(record.getLevel1());
            if ( !levels.contains(d)) {
                levels.add(d);
            }
            if (dontUseVertical && (levels.size() > 1)) {
                if (debug) {
                    System.out.println(
                        "GribCoordSys: unused level coordinate has > 1 levels = "
                        + verticalName + " " + record.getLevelType1() + " "
                        + levels.size());
                }
            }
        }
        Collections.sort(levels);
        if (positive.equals("down")) {
            Collections.reverse(levels);
            /* for( int i = 0; i < (levels.size()/2); i++ ){
                Double tmp = (Double) levels.get( i );
                levels.set( i, levels.get(levels.size() -i -1));
                levels.set(levels.size() -i -1, tmp );
             } */
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
            Double     d      = new Double(record.getLevel1());
            if ( !levelList.contains(d)) {
                levelList.add(d);
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
        ncfile.addDimension(g, new Dimension(verticalName, nlevs, true));
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

        String dims = "time";
        if ( !dontUseVertical) {
            dims = dims + " " + verticalName;
        }
        if (hcs.isLatLon()) {
            dims = dims + " lat lon";
        } else {
            dims = dims + " y x";
        }

        //Collections.sort( levels);
        int nlevs = levels.size();
        // ncfile.addDimension(g, new Dimension(verticalName, nlevs, true));

        // coordinate axis and coordinate system Variable
        Variable v = new Variable(ncfile, g, null, verticalName);
        v.setDataType(DataType.DOUBLE);

        v.addAttribute(new Attribute("long_name",
                                     lookup.getLevelDescription(record)));
        v.addAttribute(new Attribute("units", lookup.getLevelUnit(record)));

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
                    Integer.toString(record.getLevelType1())));

            v.addAttribute(new Attribute(_Coordinate.AxisType,
                                         axisType.toString()));
            v.addAttribute(new Attribute(_Coordinate.Axes, dims));
            if ( !hcs.isLatLon()) {
                v.addAttribute(new Attribute(_Coordinate.Transforms,
                                             hcs.getGridName()));
            }
        }

        double[] data = new double[nlevs];
        for (int i = 0; i < levels.size(); i++) {
            Double d = (Double) levels.get(i);
            data[i] = d.doubleValue();
        }
        Array dataArray = Array.factory(DataType.DOUBLE.getClassType(),
                                        new int[] { nlevs }, data);

        v.setDimensions(verticalName);
        v.setCachedData(dataArray, false);

        ncfile.addVariable(g, v);

        // look for vertical transforms
        if (record.getLevelType1() == 109) {
            findCoordinateTransform(g, "Pressure", record.getLevelType1());
        }
    }

    /**
     * _more_
     *
     * @param g _more_
     * @param nameStartsWith _more_
     * @param levelType _more_
     */
    void findCoordinateTransform(Group g, String nameStartsWith,
                                 int levelType) {
        // look for variable that uses this coordinate
        List vars = g.getVariables();
        for (int i = 0; i < vars.size(); i++) {
            Variable v = (Variable) vars.get(i);
            if (v.getName().equals(nameStartsWith)) {
                Attribute att = v.findAttribute("GRIB_level_type");
                if ((att == null)
                        || (att.getNumericValue().intValue() != levelType)) {
                    continue;
                }

                v.addAttribute(new Attribute(_Coordinate.TransformType,
                                             "Vertical"));
                v.addAttribute(new Attribute("transform_name",
                                             "Existing3DField"));
            }
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
        Double d = new Double(record.getLevel1());
        return levels.indexOf(d);
    }
}


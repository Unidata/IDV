/*
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

// $Id: GridTableLookup.java,v 1.13 2006/08/03 22:32:59 rkambic Exp $

package ucar.unidata.data.grid.gempak;


/**
 * GridTableLookup.java
 */
public interface GridTableLookup {

    /**
     * .
     * @param gds
     * @return GridName.
     */
    public String getGridName(GridDefRecord gds);

    /**
     * .
     * @param gds
     * @return ShapeName.
     */
    public String getShapeName(GridDefRecord gds);

    /**
     * .
     * @param gr
     * @return DisciplineName.
     */
    public String getDisciplineName(GridRecord gr);

    /**
     * .
     * @param gr
     * @return CategoryName.
     */
    public String getCategoryName(GridRecord gr);

    /**
     * .
     * @param gr
     * @return Parameter.
     */
    public GridParameter getParameter(GridRecord gr);

    /**
     * .
     * @param gr
     * @return LevelName.
     */
    public String getLevelName(GridRecord gr);

    /**
     * .
     * @param gr
     * @return LevelDescription.
     */
    public String getLevelDescription(GridRecord gr);

    /**
     * .
     * @param gr
     * @return LevelUnit.
     */
    public String getLevelUnit(GridRecord gr);

    /**
     * .
     * @return FirstBaseTime.
     */
    public java.util.Date getFirstBaseTime();

    /**
     * .
     * @return FirstBaseTime.
     */
    public String getFirstTimeRangeUnitName();

    /**
     * .
     * @param gds
     * @return is this a LatLon Grid
     */
    public boolean isLatLon(GridDefRecord gds);

    /**
     * if vertical level should be made into a coordinate; dont do for surface, 1D levels.
     * @param gr
     * @return is this a VerticalCoordinate
     */
    public boolean isVerticalCoordinate(GridRecord gr);

    /**
     * .
     * @param gr
     * @return is this positive up level
     */
    public boolean isPositiveUp(GridRecord gr);

    // projection enumerations.

    /**
     * Polar Sterographic
     */
    public int PolarStereographic = 1;

    /**
     * Lambert Conformal
     */
    public int LambertConformal = 2;

    /**
     * Mercator
     */
    public int Mercator = 3;

    /**
     * Universal Transverse Mercator
     */
    public int UTM = 4;

    /**
     * Albers Equal Area
     */
    public int AlbersEqualArea = 5;

    /**
     * Lambert Azimuth Equal Area
     */
    public int LambertAzimuthEqualArea = 6;

    /**
     * Orthographic
     */
    public int Orthographic = 7;

    /**
     * Gausian Lat/Lon
     */
    public int GaussianLatLon = 8;

    // return one of the above

    /**
     * .
     * @param gds
     * @return ProjectionType
     */
    public int getProjectionType(GridDefRecord gds);

    /**
     * .
     * @return FirstMissingValue.
     */
    public float getFirstMissingValue();

    /**
     * Check to see if this grid is a layer variable
     *
     * @param gr  record to check
     *
     * @return  true if a layer
     */
    public boolean isLayer(GridRecord gr);

}


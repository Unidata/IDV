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


package ucar.unidata.data.grid.gempak;


/**
 * GempakLookup
 * get all the information about a GEMPAK file.
 */
public final class GempakLookup implements GridTableLookup {

    /**  sample grid */
    private GempakGridRecord sample;

    /**
     * Gets a representative grid for this lookup
     * @param sample
     */
    public GempakLookup(GempakGridRecord sample) {
        this.sample = sample;
    }

    /**
     * .
     * @param gds
     * @return ShapeName.
     */
    public String getShapeName(GridDefRecord gds) {
        return "Spherical";
    }

    /**
     * gets the grid type.
     * @param gds
     * @return GridName
     */
    public final String getGridName(GridDefRecord gds) {
        return getProjectionName(gds);
    }

    /**
     * gets parameter table, then grib1 parameter based on number.
     * @param gr GridRecord
     * @return Parameter
     */
    public final GridParameter getParameter(GridRecord gr) {
        String name = gr.getParameterName();
        GridParameter p = new GridParameter(1,name,name,null);
        return p;
    }

    /**
     * gets the DisciplineName.
     * @param  gr
     * @return DisciplineName
     */
    public final String getDisciplineName(GridRecord gr) {
        // all disciplines are the same in GEMPAK
        return "Meteorological Products";
    }

    /**
     * gets the CategoryName.
     * @param  gr
     * @return CategoryName
     */
    public final String getCategoryName(GridRecord gr) {
        // no categories in GEMPAK
        return "Meteorological Parameters";
    }

    /**
     * gets the LevelName.
     * @param  gr
     * @return LevelName
     */
    public final String getLevelName(GridRecord gr) {
        return GempakUtil.LV_CCRD(gr.getLevelType1());
    }

    /**
     * gets the LevelDescription.
     * @param  gr
     * @return LevelDescription
     */
    public final String getLevelDescription(GridRecord gr) {
        // TODO:  flesh this out
        return getLevelName(gr);
    }

    /**
     * gets the LevelUnit.
     * @param  gr
     * @return LevelUnit
     */
    public final String getLevelUnit(GridRecord gr) {
        // TODO:  figure this out
        return "hPa";
    }

    /**
     * gets the TimeRangeUnitName.
     * @return TimeRangeUnitName
     */
    public final String getFirstTimeRangeUnitName() {
        return "hours";
    }

    /**
     * gets the BaseTime Forecastime.
     * @return BaseTime
     */
    public final java.util.Date getFirstBaseTime() {
        return sample.getReferenceTime();
    }

    /**
     * is this a LatLon grid.
     * @param  gds
     * @return isLatLon
     */
    public final boolean isLatLon(GridDefRecord gds) {
        return getProjectionName(gds).equals("CED");
    }

    /**
     * gets the ProjectionType.
     * @param  gds
     * @return ProjectionType
     */
    public final int getProjectionType(GridDefRecord gds) {
          String name = getProjectionName(gds).trim();
          if (name.equals("CED")) {
              return -1;
          } else if (name.equals("MER")) {
              return Mercator;
          } else if (name.equals("LCC")) {
              return LambertConformal;
          } else if (name.equals("PS")) {
              return PolarStereographic;
          } else if (name.equals("STR")) {
              return PolarStereographic;
          } else {
              return -1;
          }
    }

    /**
     * is this a VerticalCoordinate.
     * @param  gr
     * @return isVerticalCoordinate
     */
    public final boolean isVerticalCoordinate(GridRecord gr) {
        int type = gr.getLevelType1();
        if (type > GempakUtil.vertCoords.length ||
           !GempakUtil.vertCoords[type].equals("NONE")) {
           return true;
        } 
        return false;
    }

    /**
     * is this a PositiveUp VerticalCoordinate.
     * @param  gr
     * @return isPositiveUp
     */
    public final boolean isPositiveUp(GridRecord gr) {
        int type = gr.getLevelType1();
        if (type == 1 || type == 5) {
            return false;
        } 
        return true;
    }

    /**
     * gets the MissingValue.
     * @return MissingValue
     */
    public final float getFirstMissingValue() {
        return GempakConstants.RMISSD;
    }

    public boolean isLayer(GridRecord gr) {
      if (gr.getLevel2() == -1) return true;
      return false;      
    }

    private String getProjectionName(GridDefRecord gds) {
        return gds.getParam("Proj");
    }

}

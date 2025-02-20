/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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




/**
 * Class to hold lat/lon specific AxisScaleInfo stuff
 */
public class LatLonAxisScaleInfo extends ucar.visad.display.AxisScaleInfo {

    /** Some default coordinate formats */
    public static String[] COORD_FORMATS = {
        "DDH", "DD", "DD.d", "DD MM'", "DD:MM", "DD:MM:SS", "DD MM'SS\"",
        "DD.dH", "DD:MMH", "DD:MM:SSH"
    };

    /** The coord format. */
    private String coordFormat;

    /** The base label. */
    private double baseLabel;

    /** The  increment. */
    private double increment;

    /** Minor division. */
    private int minorDivision;

    /** Is axis visible. */
    private boolean visible;

    /** Use 360 degree angles. */
    private boolean use360;

    /** Does the label have relief angling away from the axis */
    private boolean labelRelief;

    /**
     * Instantiates a new lat lon scale info.
     */
    public LatLonAxisScaleInfo() {}

    /**
     * Gets the coord format.
     *
     * @return the coord format
     */
    public String getCoordFormat() {
        return coordFormat;
    }

    /**
     * Sets the coord format.
     *
     * @param coordFormat the new coord format
     */
    public void setCoordFormat(String coordFormat) {
        this.coordFormat = coordFormat;
    }


    /**
     * Gets the base label.
     *
     * @return the base label
     */
    public double getBaseLabel() {
        return baseLabel;
    }

    /**
     * Sets the base label.
     * deprecated
     *
     * @param baseLabel the new base label
     */
    public void setBaseLabel(String baseLabel) {
    	setBaseLabel(Double.valueOf(baseLabel));
    }

    /**
     * Sets the base label.
     *
     * @param baseLabel the new base label
     */
    public void setBaseLabel(double baseLabel) {
        this.baseLabel = baseLabel;
    }

    /**
     * Gets the increment.
     *
     * @return the increment
     */
    public double getIncrement() {
        return increment;
    }

    /**
     * Sets the increment.
     * deprecated
     *
     * @param increment the new increment
     */
    public void setIncrement(String increment) {
        setIncrement(Double.valueOf(increment));
    }

    /**
     * Sets the increment.
     *
     * @param increment the new increment
     */
    public void setIncrement(double increment) {
        this.increment = increment;
    }

    /**
     * Gets the minor division.
     *
     * @return the minor division
     */
    public int getMinorDivision() {
        return minorDivision;
    }

    /**
     * Sets the minor division.
     *
     * @param minorDivision the new minor division
     */
    public void setMinorDivision(int minorDivision) {
        this.minorDivision = minorDivision;
    }


    /**
     * Checks to see if user wants 360 degree angles
     *
     * @return true, if is use360
     */
    public boolean isUse360() {
        return use360;
    }


    /**
     * The user wants 360 degree angles
     *
     * @param use360 the new use360
     */
    public void setUse360(boolean use360) {
        this.use360 = use360;
    }

    /**
     * Checks if label has relief.
     *
     * @return true, if label has relief
     */
    public boolean isLabelRelief() {
        return labelRelief;
    }

    /**
     * Sets the label relief.
     *
     * @param labelRelief the new label relief
     */
    public void setLabelRelief(boolean labelRelief) {
        this.labelRelief = labelRelief;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public int hashCode() {
        final int prime  = 31;
        int       result = super.hashCode();
        result = prime * result + (int) baseLabel;
        result = prime * result + ((coordFormat == null)
                                   ? 0
                                   : coordFormat.hashCode());
        result = prime * result + (int) increment;
        result = prime * result + (labelRelief
                                   ? 1231
                                   : 1237);
        result = prime * result + minorDivision;
        result = prime * result + (use360
                                   ? 1231
                                   : 1237);
        result = prime * result + (visible
                                   ? 1231
                                   : 1237);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ( !super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LatLonAxisScaleInfo other = (LatLonAxisScaleInfo) obj;
        if (!(baseLabel == other.baseLabel)) {
                return false;
        }
        if (coordFormat == null) {
            if (other.coordFormat != null) {
                return false;
            }
        } else if ( !coordFormat.equals(other.coordFormat)) {
            return false;
        }
        if (!(increment == other.increment)) {
                return false;
        }
        if (labelRelief != other.labelRelief) {
            return false;
        }
        if (minorDivision != other.minorDivision) {
            return false;
        }
        if (use360 != other.use360) {
            return false;
        }
        if (visible != other.visible) {
            return false;
        }
        return true;
    }
}

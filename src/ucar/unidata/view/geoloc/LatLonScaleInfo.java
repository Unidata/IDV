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


package ucar.unidata.view.geoloc;

/**
 * Struct containing Latitude / Longitude scale information.
 */
public class LatLonScaleInfo {

    /** The abscissa label. */
    public String abscissaLabel = "Longitude";    // Default

    /** The ordinate label. */
    public String ordinateLabel = "Latitude";    // Default

    /** The major tick spacing. */
    public int majorTickSpacing = 4;

    /** The minor tick spacing. */
    public int minorTickSpacing = 8;

    /**
     * Instantiates a new lat lon scale info.
     */
    public LatLonScaleInfo() {}

    /**
     * Instantiates a new lat lon scale info.
     *
     * @param abscissaLabel the abscissa label
     *
     * @param ordinateLabel the abscissa label
     *
     * @param majorTickSpacing the major tick spacing
     *
     * @param minorTickSpacing the minor tick spacing
     */
    public LatLonScaleInfo(String abscissaLabel, String ordinateLabel, int majorTickSpacing, int minorTickSpacing) {
        super();
        this.abscissaLabel    = abscissaLabel;
        this.ordinateLabel    = ordinateLabel;
        this.majorTickSpacing = majorTickSpacing;
        this.minorTickSpacing = minorTickSpacing;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((abscissaLabel == null) ? 0 : abscissaLabel.hashCode());
		result = prime * result + majorTickSpacing;
		result = prime * result + minorTickSpacing;
		result = prime * result
				+ ((ordinateLabel == null) ? 0 : ordinateLabel.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LatLonScaleInfo other = (LatLonScaleInfo) obj;
		if (abscissaLabel == null) {
			if (other.abscissaLabel != null)
				return false;
		} else if (!abscissaLabel.equals(other.abscissaLabel))
			return false;
		if (majorTickSpacing != other.majorTickSpacing)
			return false;
		if (minorTickSpacing != other.minorTickSpacing)
			return false;
		if (ordinateLabel == null) {
			if (other.ordinateLabel != null)
				return false;
		} else if (!ordinateLabel.equals(other.ordinateLabel))
			return false;
		return true;
	}

}

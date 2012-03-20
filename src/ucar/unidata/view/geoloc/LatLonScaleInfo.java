/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
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

//~--- non-JDK imports --------------------------------------------------------

import ucar.unidata.view.geoloc.CoordinateFormat.Cardinality;
import ucar.unidata.view.geoloc.CoordinateFormat.DecimalCoordFormat;
import ucar.unidata.view.geoloc.CoordinateFormat.DegMinSec;
import ucar.unidata.view.geoloc.CoordinateFormat.FloorCoordFormat;

import static ucar.unidata.view.geoloc.CoordinateFormat.EMPTY_FORMAT;

/**
 * Struct containing Latitude / Longitude scale information.
 */
public class LatLonScaleInfo {

    /** The abscissa label. */
    public String abscissaLabel;

    /** The coord format. */
    public LatLonScaleInfo.CoordSys coordFormat;

    /** The latitude base label. */
    public String latBaseLabel;

    /** The latitude increment. */
    public String latIncrement;

    /** Latitude minor increment. */
    public int latMinorIncrement;

    /** The longitude base label. */
    public String lonBaseLabel;

    /** The longitude increment. */
    public String lonIncrement;

    /** Longitude minor increment. */
    public int lonMinorIncrement;

    /** The ordinate label. */
    public String ordinateLabel;

    /** Is x axis visible. */
    public boolean xVisible;

    /** Is y axis visible. */
    public boolean yVisible;

    /**
     * Instantiates a new lat lon scale info.
     */
    public LatLonScaleInfo() {}

    /**
     * The Enum CoordSys.
     */
    public enum CoordSys {

        /** The A. */
        A("ex 51 N"),

        /** The B. */
        B("ex 51 28'N"),

        /** The C. */
        C("ex 51 27.635'N"),

        /** The D. */
        D("ex 51 27'38\"N"),

        /** The E. */
        E("ex 51:28N"),

        /** The F. */
        F("ex 51:27:38N"),

        /** The G. */
        G("ex 51:27:38.11536N"),

        /** The H. */
        H("ex 51.46059");

        /** The coord sys. */
        private final String coordSys;

        /**
         * Instantiates a new coord sys.
         *
         * @param coordSys the coord sys
         */
        private CoordSys(final String coordSys) {
            this.coordSys = coordSys;
        }

        /**
         * Format.
         *
         * @param i the i
         * @param card the card
         * @return the string
         */
        public String format(double i, Cardinality card) {
            switch (this) {
            case A :
                return CoordinateFormat.convert(i, new DecimalCoordFormat(0, DegMinSec.DEGREE), EMPTY_FORMAT,
                                                EMPTY_FORMAT, card);

            case B :
                return CoordinateFormat.convert(i, new FloorCoordFormat(DegMinSec.DEGREE),
                                                new DecimalCoordFormat(0, DegMinSec.MINUTE), EMPTY_FORMAT, card);

            case C :
                return CoordinateFormat.convert(i, new FloorCoordFormat(DegMinSec.DEGREE),
                                                new DecimalCoordFormat(3, DegMinSec.MINUTE), EMPTY_FORMAT, card);

            case D :
                return CoordinateFormat.convert(i, new FloorCoordFormat(DegMinSec.COLON),
                                                new DecimalCoordFormat(0, DegMinSec.NONE), EMPTY_FORMAT, card);

            case E :
                return CoordinateFormat.convert(i, new FloorCoordFormat(DegMinSec.COLON),
                                                new DecimalCoordFormat(0, DegMinSec.NONE), EMPTY_FORMAT, card);

            case F :
                return CoordinateFormat.convert(i, new FloorCoordFormat(DegMinSec.COLON),
                                                new FloorCoordFormat(DegMinSec.COLON),
                                                new DecimalCoordFormat(0, DegMinSec.NONE), card);

            case G :
                return CoordinateFormat.convert(i, new FloorCoordFormat(DegMinSec.COLON),
                                                new FloorCoordFormat(DegMinSec.COLON),
                                                new DecimalCoordFormat(5, DegMinSec.NONE), card);

            case H :
                return CoordinateFormat.convert(i, new DecimalCoordFormat(5, DegMinSec.NONE), EMPTY_FORMAT,
                                                EMPTY_FORMAT, card);

            default :
                return CoordinateFormat.convert(i, new CoordinateFormat.DecimalCoordFormat(0, DegMinSec.DEGREE),
                                                new CoordinateFormat.DecimalCoordFormat(0, DegMinSec.MINUTE),
                                                new CoordinateFormat.DecimalCoordFormat(0, DegMinSec.SECOND), card);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return coordSys;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((abscissaLabel == null) ? 0 : abscissaLabel.hashCode());
		result = prime * result
				+ ((coordFormat == null) ? 0 : coordFormat.hashCode());
		result = prime * result
				+ ((latBaseLabel == null) ? 0 : latBaseLabel.hashCode());
		result = prime * result
				+ ((latIncrement == null) ? 0 : latIncrement.hashCode());
		result = prime * result + latMinorIncrement;
		result = prime * result
				+ ((lonBaseLabel == null) ? 0 : lonBaseLabel.hashCode());
		result = prime * result
				+ ((lonIncrement == null) ? 0 : lonIncrement.hashCode());
		result = prime * result + lonMinorIncrement;
		result = prime * result
				+ ((ordinateLabel == null) ? 0 : ordinateLabel.hashCode());
		result = prime * result + (xVisible ? 1231 : 1237);
		result = prime * result + (yVisible ? 1231 : 1237);
		return result;
	}

    /**
     * {@inheritDoc}
     */
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
		if (coordFormat != other.coordFormat)
			return false;
		if (latBaseLabel == null) {
			if (other.latBaseLabel != null)
				return false;
		} else if (!latBaseLabel.equals(other.latBaseLabel))
			return false;
		if (latIncrement == null) {
			if (other.latIncrement != null)
				return false;
		} else if (!latIncrement.equals(other.latIncrement))
			return false;
		if (latMinorIncrement != other.latMinorIncrement)
			return false;
		if (lonBaseLabel == null) {
			if (other.lonBaseLabel != null)
				return false;
		} else if (!lonBaseLabel.equals(other.lonBaseLabel))
			return false;
		if (lonIncrement == null) {
			if (other.lonIncrement != null)
				return false;
		} else if (!lonIncrement.equals(other.lonIncrement))
			return false;
		if (lonMinorIncrement != other.lonMinorIncrement)
			return false;
		if (ordinateLabel == null) {
			if (other.ordinateLabel != null)
				return false;
		} else if (!ordinateLabel.equals(other.ordinateLabel))
			return false;
		if (xVisible != other.xVisible)
			return false;
		if (yVisible != other.yVisible)
			return false;
		return true;
	}

}

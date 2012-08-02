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


import ucar.unidata.view.geoloc.CoordinateFormat.Cardinality;
import ucar.unidata.view.geoloc.CoordinateFormat.DecimalCoordFormat;
import ucar.unidata.view.geoloc.CoordinateFormat.DegMinSec;
import ucar.unidata.view.geoloc.CoordinateFormat.FloorCoordFormat;

import static ucar.unidata.view.geoloc.CoordinateFormat.EMPTY_FORMAT;

import java.awt.Font;


/**
 * Struct containing axis scale information.
 */
public class AxisScaleInfo {

    /** The axis label. */
    public String label;

    /** The coord format. */
    public AxisScaleInfo.CoordSys coordFormat;

    /** The base label. */
    public String baseLabel;

    /** The  increment. */
    public String increment;

    /** Minor increment. */
    public int minorIncrement;

    /** Is axis visible. */
    public boolean visible;

    /** The axis font. */
    public Font font;

    /**
     * Instantiates a new lat lon scale info.
     */
    public AxisScaleInfo() {}


    /**
     * The Enum CoordSys.
     */
    public enum CoordSys {

        /** The A. */
        A("dd D"),

        /** The B. */
        B("dd mm'D"),

        /** The C. */
        C("dd mm.mmm'D"),

        /** The D. */
        D("dd mm'ss\"D"),

        /** The E. */
        E("dd:mmD"),

        /** The F. */
        F("dd:mm:ssD"),

        /** The G. */
        G("dd:mm:ss.sssssD"),

        /** The H. */
        H("dd.dddddD");

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
                  return CoordinateFormat.convert(i,
                          new DecimalCoordFormat(0, DegMinSec.NONE),
                          EMPTY_FORMAT, EMPTY_FORMAT, card);

              case B :
                  return CoordinateFormat.convert(i,
                          new FloorCoordFormat(DegMinSec.DEGREE),
                          new DecimalCoordFormat(0, DegMinSec.MINUTE),
                          EMPTY_FORMAT, card);

              case C :
                  return CoordinateFormat.convert(i,
                          new FloorCoordFormat(DegMinSec.DEGREE),
                          new DecimalCoordFormat(3, DegMinSec.MINUTE),
                          EMPTY_FORMAT, card);

              case D :
                  return CoordinateFormat.convert(i,
                          new FloorCoordFormat(DegMinSec.COLON),
                          new DecimalCoordFormat(0, DegMinSec.NONE),
                          EMPTY_FORMAT, card);

              case E :
                  return CoordinateFormat.convert(i,
                          new FloorCoordFormat(DegMinSec.COLON),
                          new DecimalCoordFormat(0, DegMinSec.NONE),
                          EMPTY_FORMAT, card);

              case F :
                  return CoordinateFormat.convert(i,
                          new FloorCoordFormat(DegMinSec.COLON),
                          new FloorCoordFormat(DegMinSec.COLON),
                          new DecimalCoordFormat(0, DegMinSec.NONE), card);

              case G :
                  return CoordinateFormat.convert(i,
                          new FloorCoordFormat(DegMinSec.COLON),
                          new FloorCoordFormat(DegMinSec.COLON),
                          new DecimalCoordFormat(5, DegMinSec.NONE), card);

              case H :
                  return CoordinateFormat.convert(i,
                          new DecimalCoordFormat(5, DegMinSec.NONE),
                          EMPTY_FORMAT, EMPTY_FORMAT, card);

              default :
                  return CoordinateFormat.convert(i,
                          new DecimalCoordFormat(0, DegMinSec.NONE),
                          EMPTY_FORMAT, EMPTY_FORMAT, card);
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
     *
     */
    @Override
    public int hashCode() {
        final int prime  = 31;
        int       result = 1;
        result = prime * result + ((baseLabel == null)
                                   ? 0
                                   : baseLabel.hashCode());
        result = prime * result + ((coordFormat == null)
                                   ? 0
                                   : coordFormat.hashCode());
        result = prime * result + ((font == null)
                                   ? 0
                                   : font.hashCode());
        result = prime * result + ((increment == null)
                                   ? 0
                                   : increment.hashCode());
        result = prime * result + ((label == null)
                                   ? 0
                                   : label.hashCode());
        result = prime * result + minorIncrement;
        result = prime * result + (visible
                                   ? 1231
                                   : 1237);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        AxisScaleInfo other = (AxisScaleInfo) obj;
        if (baseLabel == null) {
            if (other.baseLabel != null) {
                return false;
            }
        } else if ( !baseLabel.equals(other.baseLabel)) {
            return false;
        }
        if (coordFormat != other.coordFormat) {
            return false;
        }
        if (font == null) {
            if (other.font != null) {
                return false;
            }
        } else if ( !font.equals(other.font)) {
            return false;
        }
        if (increment == null) {
            if (other.increment != null) {
                return false;
            }
        } else if ( !increment.equals(other.increment)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if ( !label.equals(other.label)) {
            return false;
        }
        if (minorIncrement != other.minorIncrement) {
            return false;
        }
        if (visible != other.visible) {
            return false;
        }
        return true;
    }
}

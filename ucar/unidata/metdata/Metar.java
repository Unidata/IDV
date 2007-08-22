/*
 * $Id: Metar.java,v 1.8 2005/05/13 18:31:29 jeffmc Exp $
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

package ucar.unidata.metdata;



import ucar.unidata.util.Timestamp;
import ucar.unidata.util.ByteString;


/**
 * Abstraction for surface observations.
 *
 * TODO: push exceptions back down to decoder
 * decoder uses ByteString directly.
 *
 * @author Glenn Davis
 * @version $Revision: 1.8 $
 */
public class Metar extends StationOb {

    /** the raw text */
    private final ByteString text_;

    /** the header for data */
    private final WMOHeader wmoHeader_;  // may be null

    /**
     * Create a new Metar
     *
     * @param stn            the Station
     * @param timestamp      the time
     * @param ensemble       the decoded values
     * @param text           the raw text
     * @param wmoHeader      the WMO header
     *
     */
    public Metar(Station stn, Timestamp timestamp, Ensemble ensemble,
                 ByteString text, WMOHeader wmoHeader) {
        super(stn, timestamp, ensemble);
        text_      = text;
        wmoHeader_ = wmoHeader;  // optional
    }

    /* public int compareTo(Metar mm) {
      int comp = super.compareTo((StationOb)mm);
      if(comp == 0) {
        comp = text_.compareTo(mm.text_);
      }
      return comp;
    }

    /*
     * If we didn't define this function
     * would we get identical behavior to this def?
     *
    public int compareTo(Object oo) {
      if(oo instanceof Metar)
        return compareTo((Metar) oo);
      return super.compareTo(oo);
    } */

    /**
     * Get the raw METAR text
     *
     * @return  the raw data
     */
    public ByteString getText() {
        return text_;
    }

    /**
     * Get the Altimeter setting for this metar
     *
     * @return  altimeter setting Value
     */
    public Value getAltimeterSetting() {
        return ensemble.get("AltimeterSetting");
    }

    /**
     * Get the ceiling value
     *
     * @return   the ceiling value
     */
    public Value getCeiling() {
        return ensemble.get("Ceiling");
    }

    /**
     * Get the dewpoint value.
     *
     * @return  the dewpoint value
     */
    public Value getDewPoint() {
        return ensemble.get("DewPoint");
    }

    /**
     * Get the sky coverage
     *
     * @return  the sky coverage Value
     */
    public Value getSkyCoverage() {
        return ensemble.get("SkyCoverage");
    }

    /**
     * Get the temperature
     *
     * @return  the temperature Value
     */
    public Value getTemperature() {
        return ensemble.get("Temperature");
    }

    /**
     * Get the vertical visibility value
     *
     * @return  the vertical visibility Value
     */
    public Value getVerticalVisibility() {
        return ensemble.get("VerticalVisibility");
    }

    /**
     * Get the wind direction.
     *
     * @return  wind direction Value
     */
    public Value getWindDirection() {
        return ensemble.get("WindDirection");
    }

    /**
     * Get the wind speed.
     *
     * @return   wind speed Value
     */
    public Value getWindSpeed() {
        return ensemble.get("WindSpeed");
    }

    /**
     * Format this Metar  for toString()
     *
     * @param buf  buffer for output format
     * @return  <code>buf</code> with formatted info
     */
    public StringBuffer format(StringBuffer buf) {
        getText().format(buf);
        buf.append("\n");
        buf.append(getStationIdentifier());
        buf.append(" ");
        getTimestamp().format(buf);
        buf.append("\n");
        if (ensemble == null) {
            return buf;
        }
        return ensemble.format(buf);
    }

    /**
     * Return a formatted string representation for this Metar.
     *
     * @return  formatted string representation
     */
    public String toString() {
        return format(new StringBuffer()).toString();
    }
}

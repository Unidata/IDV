/*
 * $Id: StationOb.java,v 1.8 2005/05/13 18:31:31 jeffmc Exp $
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
 * A 'raw' observation which occured at a Station at
 * a given time.
 * <p>
 * This is used to implement METAR, SAO, and other such
 * Observation classes.
 *
 * @author $Author: jeffmc $
 * @version $Revision: 1.8 $ $Date: 2005/05/13 18:31:31 $
 */
public class StationOb implements java.io.Serializable {

    /** station */
    private final Station stn_;

    /** timestamp */
    private final Timestamp timestamp_;

    /** data */
    protected final Ensemble ensemble;

    /**
     * Create a new StationOb
     *
     * @param stn          Station information
     * @param timestamp    time stamp
     * @param ens          Ensemble of data
     */
    public StationOb(Station stn, Timestamp timestamp, Ensemble ens) {
        stn_       = stn;
        timestamp_ = timestamp;
        ensemble   = ens;
    }

    /* public int compareTo(StationOb so) {
      int comp = getTimestamp().compareTo(so.getTimestamp());
      if(comp == 0) {
        comp = getStation().compareTo(so.getStation());
      }
      return comp;
    }

    public int compareTo(Object oo) {
      if(oo instanceof StationObDB.TimeMarker){
        final int comp = getTimestamp().compareTo((StationObDB.TimeMarker)oo);
        if(comp == 0)
          return 1;
        return comp;
      }
      // may throw ClassCastException, as desired
      return compareTo((StationOb) oo);
    } */

    /**
     * Get the {@link Ensemble} of data.
     *
     * @return  the data
     */
    public Ensemble getEnsemble() {
        return ensemble;
    }

    /**
     * Get the station for this ob
     *
     * @return  the station
     */
    public Station getStation() {
        return stn_;
    }

    /**
     * Get the station identifier.
     *
     * @return station ID
     */
    public String getStationIdentifier() {
        return stn_.getIdentifier();
    }

    /**
     * Get the time of this observation.
     *
     * @return  timestamp
     */
    public Timestamp getTimestamp() {
        return timestamp_;
    }

}

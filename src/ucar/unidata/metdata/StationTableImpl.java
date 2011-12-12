/*
 * $Id: StationTableImpl.java,v 1.11 2006/08/18 21:31:58 jeffmc Exp $
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



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.Collection;
import java.util.Set;


/**
 * Table of reporting stations.
 * <p>
 * @author $Author: jeffmc $
 * @version $Revision: 1.11 $ $Date: 2006/08/18 21:31:58 $
 */
public class StationTableImpl implements StationTable {

    protected List stations = new ArrayList();

    /** Map of stations */
    protected final Map byId_;

    /**
     * Create a new StationTableImpl
     */
    public StationTableImpl() {
        byId_ = new HashMap();
    }

    /**
     * Have this method here so derived classes know when anything was
     * requested of this station table. This allows them to lazily create
     * the stations, etc.
     *
     * @return  station map
     */
    protected Map getMap() {
        return byId_;
    }


    /**
     * Add a station to the table
     *
     * @param stn   station to add
     *
     * @return   true if the station was not already in the table
     */
    public synchronized boolean add(Station stn) {
        return add(stn, false);
    }



    /**
     * Add a station to the table
     *
     * @param stn   station to add
     * @param onlyIfNotInMap
     *
     * @return   true if the station was not already in the table
     */
    public synchronized boolean add(Station stn, boolean onlyIfNotInMap) {
        if (stn == null) {
            throw new NullPointerException();
        }
        stations.add(stn);
        String  id    = stn.getIdentifier();
        boolean hadIt = getMap().get(id) != null;
        if ( !hadIt || !onlyIfNotInMap) {
            getMap().put(id, stn);
        }
        return hadIt;
    }

    /**
     * Remove a station from the table
     *
     * @param stn   station to remove
     * @return  the station being removed or null if it is not in the table
     */
    public synchronized Station remove(Station stn) {
        if(stations.remove(stn)) return stn;
        return null;
        //        return (Station) getMap().remove(stn.getIdentifier());
    }

    /**
     * Remove a station from the table by it's id
     *
     * @param id   station identifier
     *
     * @return   station mapped to the id, or null if none
     */
    public synchronized Station remove(String id) {
        return (Station) getMap().remove(id);
    }

    /**
     * Lookup Station by Identifier
     *
     * @param identifier  the station identifier
     *
     * @return   the station in the table
     */
    public Station get(String identifier) {
        return (Station) getMap().get(identifier);
    }

    /**
     * Returns the set of identifiers
     *
     * @return   Set of identifiers
     */
    public Set keySet() {
        return getMap().keySet();
    }

    /**
     * Returns the number of stations in this table
     *
     * @return  number of stations in the table
     */
    public int size() {
        return getMap().size();
    }

    /**
     * Returns a collection view of the Station values
     * contained in this table.
     *
     * @return  the collection of stations
     */
    public Collection values() {
        getMap();
        if(true) return stations;
        return getMap().values();
    }

    /* Begin Test
      public static void main(String[] args) {
        try {
          StationTable tbl  = getDefault();
          System.out.println("sz: " + tbl.size());
        } catch (Exception ee) {
          ee.printStackTrace(System.err);
          System.exit(1);
        }
        System.exit(0);
      }
    /* End Test */
}

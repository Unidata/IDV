/*
 * $Id: StationTable.java,v 1.8 2005/05/13 18:31:31 jeffmc Exp $
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



import java.util.Collection;
import java.util.Set;


/**
 * Abstraction for (read-only) table of (Fixed) Stations.
 * This is a map by Identifier.
 * <p>
 * TODO: add subsetting by geographical region.
 * TODO: RMI
 * TODO: move to less specialized package.
 *
 * @see Station
 * @author Glenn Davis
 * @version $Revision: 1.8 $ $Date: 2005/05/13 18:31:31 $
 */
public interface StationTable {

    /**
     * Lookup Station by Identifier.
     *
     * @param id   station identifier
     * @return  the Station for that Id
     */
    public Station get(String id);

    /**
     * Lookup Station by numeric id (WMO id).
     * public Station get(int id);
     * @return
     */

    /**
     * Returns the set of Identifiers
     * @return set of IDs
     */
    public Set keySet();

    /*
    public StationTable  subset(latlongbox);
    */

    /**
     * Returns the number of stations in this table
     * @return  number of stations
     */
    public int size();

    /**
     * Returns a collection view of the Station values
     * contained in this table.
     * @return  the Collection of stations
     */
    public Collection values();
}

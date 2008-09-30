/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository;


import org.w3c.dom.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Level3RadarTypeHandler extends GenericTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public Level3RadarTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     * @param links _more_
     * @param forMenu _more_
     * @param forHeader _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links, boolean forHeader)
            throws Exception {
        super.getEntryLinks(request, entry, links, forHeader);
        if (entry.getValues() == null) {
            return;
        }
        Object[] values = entry.getValues();
        if(values.length>=2 && values[0]!=null && values[1]!=null) {
            links.add(
                      new Link(
                               HtmlUtil.url(
                                            "http://radar.weather.gov/radar.php", "rid",
                                            (String) entry.getValues()[0], "product",
                                            (String) entry.getValues()[1]), repository.fileUrl(
                                                                                               "/icons/radar.gif"), "Show NWS Radar Site"));
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void xxxinitializeNewEntry(Entry entry) throws Exception {
        String station = (String) entry.getValues()[0];
        String lat =
            getRepository().getFieldDescription(station + ".lat",
                "/ucar/unidata/repository/resources/level3radar.station.properties",
                null);
        String lon =
            getRepository().getFieldDescription(station + ".lon",
                "/ucar/unidata/repository/resources/level3radar.station.properties",
                null);


        if ((lat != null) && (lon != null)) {
            double latD = Misc.decodeLatLon(lat);
            double lonD = Misc.decodeLatLon(lon);
            entry.setSouth(latD - 2);
            entry.setNorth(latD + 2);
            entry.setEast(lonD + 2);
            entry.setWest(lonD - 2);
            entry.trimAreaResolution();
        }

    }




}


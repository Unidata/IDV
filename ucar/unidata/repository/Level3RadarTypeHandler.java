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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.sql.ResultSet;

import java.sql.PreparedStatement;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.*;

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
     * @param type _more_
     * @param description _more_
     * @param columns _more_
     */
    public Level3RadarTypeHandler(Repository repository, Element entryNode) throws Exception {
        super(repository, entryNode);
    }

    protected String getEntryLinks(Entry entry, Request request) {
        if(entry.getValues()==null) {
            return super.getEntryLinks(entry, request);
        }
        return super.getEntryLinks(entry, request) + " " +
  
            HtmlUtil.href("http://radar.weather.gov/radar.php?rid=" + entry.getValues()[0] +"&product=" +
                          entry.getValues()[0] +"", 
                          HtmlUtil.img(repository.href("/Radar.gif"),
                                       "Show NWS Radar Site")," target=_OTHER");
        

    }

}


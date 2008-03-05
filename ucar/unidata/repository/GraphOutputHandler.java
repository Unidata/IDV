/**
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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GraphOutputHandler extends OutputHandler {




    /** _more_ */
    public static final String OUTPUT_GRAPH = "graph.graph";



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public GraphOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    /**
     * _more_
     *
     *
     * @param output _more_
     *
     * @return _more_
     */
    public boolean canHandle(String output) {
        return output.equals(OUTPUT_GRAPH);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesForEntry(Request request, Entry entry,
                                          List types)
            throws Exception {
        if ( !getRepository().isAppletEnabled(request)) {
            return;
        }
        types.add(new TwoFacedObject("Graph", OUTPUT_GRAPH));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesForGroup(Request request, Group group,
                                          List<Group> subGroups,
                                          List<Entry> entries, List types)
            throws Exception {
        getOutputTypesForEntry(request, group, types);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links)
            throws Exception {
        if ( !getRepository().isAppletEnabled(request)) {
            return;
        }

        //        String url   = HtmlUtil.url(
        //                                    getRepository().URL_ENTRY_SHOW, ARG_ID,
        //                                    entry.getId(), ARG_OUTPUT, OUTPUT_GRAPH);
        //        links.add(new Link(url,
        //                           getRepository().fileUrl(ICON_GRAPH), "Show in graph"));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, Entry entry) throws Exception {
        String graphAppletTemplate =
            getRepository().getResource(PROP_HTML_GRAPHAPPLET);
        String type = request.getString(ARG_NODETYPE, NODETYPE_GROUP);
        String html =
            StringUtil.replace(graphAppletTemplate, "${id}",
                               getRepository().encode(entry.getId()));
        html = StringUtil.replace(html, "${root}",
                                  getRepository().getUrlBase());
        html = StringUtil.replace(html, "${type}",
                                  getRepository().encode(type));
        StringBuffer sb = new StringBuffer();
        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                              false, "");

        String title = crumbs[0];
        sb.append(crumbs[1]);
        sb.append("<br>");
        sb.append(html);


        Result result = new Result(msg("Graph View") + title, sb);
        result.putProperty(
            PROP_NAVSUBLINKS,
            getHeader(
                request, request.getOutput(),
                getRepository().getOutputTypesForEntry(request, entry)));
        return result;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        return outputEntry(request, group);
    }


}


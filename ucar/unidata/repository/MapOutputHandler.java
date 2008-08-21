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


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
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
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MapOutputHandler extends OutputHandler {



    /** _more_ */
    public static final String OUTPUT_MAP = "map.map";


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public MapOutputHandler(Repository repository, Element element)
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
        return output.equals(OUTPUT_MAP);
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
    protected void xxxxgetOutputTypesForGroup(Request request, Group group,
                                          List<Group> subGroups,
                                          List<Entry> entries,
                                          List<OutputType> types)
            throws Exception {
        getOutputTypesForEntries(request, entries, types);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesForEntries(Request request,
                                            List<Entry> entries,
                                            List<OutputType> types)
            throws Exception {
         if (entries.size() > 0) {
            boolean ok = false;
            for (Entry entry : entries) {
                if (entry.hasLocationDefined() || entry.hasAreaDefined()) {
                    ok = true;
                    break;
                }
            }
            if ( !ok) {
                return;
            }
            types.add(new OutputType("Map", OUTPUT_MAP));
        }
    }



    public Result outputEntry(Request request, Entry entry) throws Exception {
        List<Entry> entriesToUse = new ArrayList<Entry>();
        entriesToUse.add(entry);
        StringBuffer sb     = new StringBuffer();
        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                              false, "");
        sb.append(crumbs[1]);
        getMap(request, entriesToUse,sb,700,500,true);
        Result result = new Result("Results", sb);
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
        List<Entry> entriesToUse = new ArrayList<Entry>(subGroups);
        entriesToUse.addAll(entries);
        StringBuffer sb     = new StringBuffer();
        String[] crumbs = getRepository().getBreadCrumbs(request, group,
                              false, "");
        sb.append(crumbs[1]);
        if (entriesToUse.size() == 0) {
            sb.append("<b>Nothing Found</b><p>");
            Result result = new Result("Query Results", sb);
            result.putProperty(
                PROP_NAVSUBLINKS,
                getHeader(
                    request, HtmlOutputHandler.OUTPUT_HTML,
                    getRepository().getOutputTypesForGroup(
                        request, group, subGroups, entries)));
            return result;
        }

        sb.append("<table border=\"0\" width=\"100%\"><tr valign=\"top\"><td width=700>");
        getMap(request, entriesToUse,sb,700,400,true);
        sb.append("</td><td>");
        for (Entry entry : entriesToUse) {
            if (entry.hasLocationDefined() || entry.hasAreaDefined()) {
                sb.append(HtmlUtil.img(getRepository().getIconUrl(entry)));
                sb.append(HtmlUtil.space(1));
                sb.append("<a href=\"javascript:hiliteEntry(mapstraction," + sqt(entry.getId()) +");\">" + entry.getName()+"</a><br>");
            }
        }
        sb.append("</td></tr></table>");

        Result result = new Result("Results", sb);
        result.putProperty(
            PROP_NAVSUBLINKS,
            getHeader(
                request, request.getOutput(),
                getRepository().getOutputTypesForGroup(
                    request, group, subGroups, entries)));


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
    public  void getMap(Request request, List<Entry> entriesToUse, StringBuffer sb, int width, int height, boolean normalControls) 
            throws Exception {
        sb.append(
            importJS(
                "http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6"));
        sb.append(importJS(repository.getUrlBase() + "/mapstraction.js"));
        sb.append(importJS(repository.getUrlBase() + "/mymap.js"));
        sb.append(
                  "<div style=\"width:" + width+"px; height:" +height+"px\" id=\"mapstraction\"></div>\n");
        sb.append(HtmlUtil.script("MapInitialize(" + normalControls+");"));
        StringBuffer js = new StringBuffer();
        js.append("mapstraction.resizeTo(" + width +"," + height +");\n");
        js.append("var marker;\n");
        js.append("var line;\n");


        for (Entry entry : entriesToUse) {
            String idBase = entry.getId();
            if (entry.hasAreaDefined()) {
                js.append("line = new Polyline([");
                js.append(llp(entry.getNorth(), entry.getWest()));
                js.append(",");
                js.append(llp(entry.getNorth(), entry.getEast()));
                js.append(",");
                js.append(llp(entry.getSouth(), entry.getEast()));
                js.append(",");
                js.append(llp(entry.getSouth(), entry.getWest()));
                js.append(",");
                js.append(llp(entry.getNorth(), entry.getWest()));
                js.append("]);\n");
                js.append("initLine(line," + qt(entry.getId())+");\n");
            }
            if (entry.hasLocationDefined() || entry.hasAreaDefined()) {
                String info =
                    "<table>"
                    + entry.getTypeHandler().getInnerEntryContent(entry,
                        request, OutputHandler.OUTPUT_HTML, false,
                        false) + "</table>";
                info = info.replace("\r", " ");
                info = info.replace("\n", " ");
                info = info.replace("\"", "\\\"");
                String icon = getRepository().getIconUrl(entry);
                js.append("marker = new Marker("
                          + llp(entry.getSouth(), entry.getEast()) + ");\n");

                js.append("marker.setIcon(" + qt(icon) + ");\n");
                js.append("marker.setInfoBubble(\"" + info + "\");\n");
                js.append("initMarker(marker," + qt(entry.getId())+");\n");
            }
        }
        js.append("mapstraction.autoCenterAndZoom();\n");
        sb.append(HtmlUtil.script(js.toString()));
    }

    private static String qt(String  s) {
        return "\"" + s +"\"";
    }

    private static String sqt(String  s) {
        return "'" + s +"'";
    }


    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    private static String llp(double lat, double lon) {
        return "new LatLonPoint(" + lat + "," + lon + ")";

    }



    /**
     * _more_
     *
     * @param jsUrl _more_
     *
     * @return _more_
     */
    private static String importJS(String jsUrl) {
        return "<script src=\"" + jsUrl + "\"></script>\n";
    }


}


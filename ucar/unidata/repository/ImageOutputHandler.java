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
public class ImageOutputHandler extends OutputHandler {



    /** _more_ */
    public static final String OUTPUT_GALLERY = "image.gallery";

    /** _more_ */
    public static final String OUTPUT_PLAYER = "image.player";

    /** _more_ */
    public static final String OUTPUT_SLIDESHOW = "image.slideshow";



    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public ImageOutputHandler(Repository repository, Element element)
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
        return output.equals(OUTPUT_GALLERY)
               || output.equals(OUTPUT_SLIDESHOW)
               || output.equals(OUTPUT_PLAYER);
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
                                          List<Entry> entries,
                                          List<OutputType> types)
            throws Exception {
        if (entries.size() == 0) {
            return;
        }
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
                if (entry.getResource().isImage()) {
                    ok = true;
                    break;
                }
            }
            if ( !ok) {
                return;
            }
        }

        types.add(new OutputType("Slideshow", OUTPUT_SLIDESHOW));
        types.add(new OutputType("Gallery", OUTPUT_GALLERY));
        types.add(new OutputType("Image Player", OUTPUT_PLAYER));
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
        Result result = makeResult(request, entries);
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
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        if (output.equals(OUTPUT_GALLERY) || output.equals(OUTPUT_PLAYER)
                || output.equals(OUTPUT_SLIDESHOW)) {
            return repository.getMimeTypeFromSuffix(".html");
        }
        return super.getMimeType(output);
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeResult(Request request, List<Entry> entries)
            throws Exception {

        StringBuffer sb         = new StringBuffer();
        String       output     = request.getOutput();
        boolean      showApplet = repository.isAppletEnabled(request);
        if (entries.size() == 0) {
            sb.append("<b>Nothing Found</b><p>");
            return new Result("Query Results", sb, getMimeType(output));
        }

        if (output.equals(OUTPUT_GALLERY)) {
            sb.append("<table>");
        } else if (output.equals(OUTPUT_PLAYER)) {}


        int    col        = 0;
        String firstImage = "";
        if (output.equals(OUTPUT_PLAYER)) {
            int cnt = 0;
            for (int i = entries.size() - 1; i >= 0; i--) {
                Entry  entry = entries.get(i);
                String url   = getImageUrl(request, entry);
                if (url == null) {
                    continue;
                }
                if (cnt == 0) {
                    firstImage = url;
                }
                String entryUrl = getEntryUrl(request, entry);
                String title =
                    "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">";
                title += "<tr><td><b>Image:</b> " + entryUrl
                         + "</td><td align=right>"
                         + new Date(entry.getStartDate());
                title += "</table>";
                title = title.replace("\"", "\\\"");
                sb.append("addImage(" + HtmlUtil.quote(url) + ","
                          + HtmlUtil.quote(title) + ");\n");
                cnt++;
            }
        } else if (output.equals(OUTPUT_SLIDESHOW)) {
            for (int i = entries.size() - 1; i >= 0; i--) {
                Entry entry = entries.get(i);
                if ( !entry.getResource().isImage()) {
                    continue;
                }
                String url =
                    HtmlUtil.url(request.url(repository.URL_ENTRY_GET) + "/"
                                 + entry.getName(), ARG_ID, entry.getId());
                String thumburl =
                    HtmlUtil.url(request.url(repository.URL_ENTRY_GET) + "/"
                                 + entry.getName(), ARG_ID, entry.getId(),
                                     ARG_IMAGEWIDTH, "" + 50);
                String entryUrl = getEntryUrl(request, entry);
                request.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML);
                String title = entry.getTypeHandler().getEntryContent(entry,
                                   request, false).toString();
                request.put(ARG_OUTPUT, output);
                title = title.replace("\"", "\\\"");
                title = title.replace("\n", " ");
                sb.append("addImage(" + HtmlUtil.quote(url) + ","
                          + HtmlUtil.quote(thumburl) + ","
                          + HtmlUtil.quote(title) + ");\n");

            }
        } else {
            for (Entry entry : entries) {
                String url = getImageUrl(request, entry);
                if (url == null) {
                    continue;
                }
                if (col >= 2) {
                    sb.append("</tr>");
                    col = 0;
                }
                if (col == 0) {
                    sb.append("<tr valign=\"bottom\">");
                }
                col++;

                sb.append("<td>");
                sb.append(HtmlUtil.img(url, "",
                                       XmlUtil.attr(ARG_WIDTH, "400")));
                sb.append("<br>\n");
                sb.append(getEntryUrl(request, entry));
                sb.append(" " + new Date(entry.getStartDate()));
                sb.append("<p></td>");
            }
        }


        if (output.equals(OUTPUT_GALLERY)) {
            sb.append("</table>\n");
        } else if (output.equals(OUTPUT_PLAYER)) {
            String playerTemplate =
                repository.getResource(PROP_HTML_IMAGEPLAYER);
            String tmp = playerTemplate.replace("${imagelist}",
                             sb.toString());
            tmp = tmp.replace("${firstimage}", firstImage);
            tmp = StringUtil.replace(tmp, "${root}", repository.getUrlBase());
            sb  = new StringBuffer(tmp);
        } else if (output.equals(OUTPUT_SLIDESHOW)) {
            String template = repository.getResource(PROP_HTML_SLIDESHOW);
            template = template.replace("${imagelist}", sb.toString());
            template = StringUtil.replace(template, "${root}",
                                          repository.getUrlBase());
            sb = new StringBuffer(template);
        }
        return new Result("Query Results", sb, getMimeType(output));
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    private String getImageUrl(Request request, Entry entry) {
        if ( !entry.getResource().isImage()) {
            if (entry.hasAreaDefined()) {
                return request.url(repository.URL_GETMAP, ARG_SOUTH,
                                   "" + entry.getSouth(), ARG_WEST,
                                   "" + entry.getWest(), ARG_NORTH,
                                   "" + entry.getNorth(), ARG_EAST,
                                   "" + entry.getEast());
            }
            return null;
        }

        return HtmlUtil.url(request.url(repository.URL_ENTRY_GET) + "/"
                            + entry.getName(), ARG_ID, entry.getId());
    }

}


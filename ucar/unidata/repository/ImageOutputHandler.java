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
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ImageOutputHandler extends OutputHandler {



    /** _more_ */
    public static final String OUTPUT_GALLERY = "default.gallery";

    /** _more_ */
    public static final String OUTPUT_PLAYER = "default.player";




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
     * @param request _more_
     *
     * @return _more_
     */
    public boolean canHandle(Request request) {
        String output = (String) request.getOutput();
        return output.equals(OUTPUT_GALLERY) || output.equals(OUTPUT_PLAYER);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getOutputTypesFor(Request request, String what)
            throws Exception {
        List list = new ArrayList();
        if (what.equals(WHAT_ENTRIES)) {
            list.add(new TwoFacedObject("Gallery", OUTPUT_GALLERY));
            list.add(new TwoFacedObject("Image Player", OUTPUT_PLAYER));
        }
        return list;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getOutputTypesForEntries(Request request)
            throws Exception {
        List list = new ArrayList();
        list.add(new TwoFacedObject("Gallery", OUTPUT_GALLERY));
        list.add(new TwoFacedObject("Image Player", OUTPUT_PLAYER));
        return list;
    }








    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        if (output.equals(OUTPUT_GALLERY)) {
            return repository.getMimeTypeFromSuffix(".html");
        } else if (output.equals(OUTPUT_PLAYER)) {
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
    public Result processEntries(Request request, List<Entry> entries)
            throws Exception {
        StringBuffer sb         = new StringBuffer();
        String       output     = request.getOutput();
        boolean      showApplet = repository.isAppletEnabled(request);
        if (entries.size() == 0) {
            sb.append("<b>Nothing Found</b><p>");
            Result result = new Result("Query Results", sb,
                                       getMimeType(output));
            result.putProperty(PROP_NAVSUBLINKS,
                               getEntriesHeader(request, output,
                                   WHAT_ENTRIES));
            return result;
        }

        if (output.equals(OUTPUT_GALLERY)) {
            sb.append("<table>");
        } else if (output.equals(OUTPUT_PLAYER)) {}


        int    col        = 0;
        String firstImage = "";
        if (output.equals(OUTPUT_PLAYER)) {
            int cnt = 0;
            for (int i = entries.size() - 1; i >= 0; i--) {
                Entry entry = entries.get(i);
                if ( !ImageUtils.isImage(entry.getFile())) {
                    continue;
                }
                String url = HtmlUtil.url(repository.URL_GETENTRY
                                          + entry.getName(), ARG_ID,
                                              entry.getId());
                if (cnt == 0) {
                    firstImage = url;
                }
                String entryUrl = getEntryUrl(entry);
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
        } else {
            for (Entry entry : entries) {
                if ( !ImageUtils.isImage(entry.getFile())) {
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
                sb.append(HtmlUtil.img(HtmlUtil.url(repository.URL_GETENTRY
                        + entry.getName(), ARG_ID, entry.getId()), "",
                            XmlUtil.attr(ARG_WIDTH, "400")));
                sb.append("<br>\n");
                sb.append(getEntryUrl(entry));
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
        }
        Result result = new Result("Query Results", sb, getMimeType(output));
        result.putProperty(PROP_NAVSUBLINKS,
                           getEntriesHeader(request, output, WHAT_ENTRIES));
        return result;

    }




}


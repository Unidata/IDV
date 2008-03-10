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
public class DateGridOutputHandler extends OutputHandler {



    /** _more_ */
    public static final String OUTPUT_GRID = "dategrid.grid";


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public DateGridOutputHandler(Repository repository, Element element)
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
        return output.equals(OUTPUT_GRID);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesFor(Request request, String what, List types)
            throws Exception {
        if (what.equals(WHAT_ENTRIES)) {
            types.add(new TwoFacedObject("Date Grid", OUTPUT_GRID));
        }
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
                                            List<Entry> entries, List types)
            throws Exception {
        types.add(new TwoFacedObject("Date Grid", OUTPUT_GRID));
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
        String       output     = request.getOutput();
        String       title      = group.getFullName();
        StringBuffer sb         = new StringBuffer();
        showNext(request, subGroups,  entries,   sb);

        entries.addAll(subGroups);
        List types = new ArrayList();
        List days = new ArrayList();
        Hashtable dayMap = new Hashtable(); 
        Hashtable typeMap = new Hashtable(); 
        Hashtable contents = new Hashtable();

        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern("yyyy/MM/dd");
        SimpleDateFormat timeSdf = new SimpleDateFormat();
        timeSdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        timeSdf.applyPattern("HH:mm");
        StringBuffer header = new StringBuffer();
        header.append(HtmlUtil.cols("Date"));
        for(Entry entry: entries) {
            String type = entry.getTypeHandler().getType();
            String day = sdf.format(new Date(entry.getStartDate()));
            if(typeMap.get(type)==null) {
                types.add(entry.getTypeHandler());
                typeMap.put(type,type);
                header.append("<td>" + entry.getTypeHandler().getLabel() +"</td>");
            }
            if(dayMap.get(day)==null) {
                days.add(new Date(entry.getStartDate()));
                dayMap.put(day, day);
            }
            String time =  timeSdf.format(new Date(entry.getStartDate()));
            String key = type +"_" + day;
            StringBuffer colSB = (StringBuffer) contents.get(key);
            if(colSB == null) {
                colSB = new StringBuffer();
                contents.put(key, colSB);
            }
            colSB.append(HtmlUtil.href(HtmlUtil.url(getRepository().URL_ENTRY_SHOW, ARG_ID, entry.getId()), time));
            colSB.append(HtmlUtil.br());
        }

        sb.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"5\" width=\"100%\">");
        sb.append(HtmlUtil.row(header.toString(), " style=\"background-color:lightblue;\""));
        days = Misc.sort(days);
        for(int dayIdx=0;dayIdx<days.size();dayIdx++) {
            Date date = (Date) days.get(dayIdx);
            String day = sdf.format(date);
            sb.append("<tr valign=\"top\">");
            sb.append("<td width=\"5%\">" + day +"</td>");
            for(int i=0;i<types.size();i++) {
                TypeHandler typeHandler = (TypeHandler) types.get(i);
                String type = typeHandler.getType();
                String key = type +"_" + day;
                StringBuffer cb = (StringBuffer) contents.get(key);
                if(cb==null) {
                    sb.append("<td>" + HtmlUtil.space(1) +"</td>");
                } else {
                    sb.append("<td>" + cb +"</td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append("</table>");

        Result result = new Result(title, sb);
        result.putProperty(
            PROP_NAVSUBLINKS,
            getHeader(
                request, output,
                getRepository().getOutputTypesForGroup(
                    request, group, subGroups, entries)));

        return result;

    }





}


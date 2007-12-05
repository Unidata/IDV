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

import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class TypeHandler implements Constants, Tables {

    /** _more_          */
    public static final String TYPE_ANY = "any";

    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_LEVEL3RADAR = "level3radar";

    /** _more_ */
    public static final String TYPE_LEVEL2RADAR = "level2radar";

    /** _more_          */
    Repository repository;

    /** _more_          */
    String type;

    /** _more_          */
    String description;



    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     */
    public TypeHandler(Repository repository, String type) {
        this(repository, type, "");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param description _more_
     */
    public TypeHandler(Repository repository, String type,
                       String description) {
        this.repository  = repository;
        this.type        = type;
        this.description = description;
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof TypeHandler)) return false;
        return Misc.equals(type,((TypeHandler)obj).getType());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getType() {
        return type;
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean isType(String type) {
        return this.type.equals(type);
    }






    /**
     * _more_
     *
     * @param filesInfo _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result showFile(FilesInfo filesInfo, Request request)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        String         output    = repository.getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<table>");
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Name:"), filesInfo.getName() + " " +
                                          repository.getFileFetchLink(filesInfo) +" " +
                                          repository.getGraphLink(request,filesInfo)));
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Create Date:"),  ""+new Date(filesInfo.getCreateDate())));
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Creator:"),  filesInfo.getUser().getName()));

            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("File:"),  filesInfo.getFile()));

            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Type:"), filesInfo.getType()));
            if (isType(TYPE_LEVEL3RADAR)) {}
            sb.append("</table>");
        } else if(output.equals(OUTPUT_XML)) {

        } else if(output.equals(OUTPUT_CSV)) {

        }
        return new Result("File: "+ filesInfo.getName(),  sb,repository.getMimeTypeFromOutput(output));

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
    public Result processList(Request request, String what)
            throws Exception {
        return processRadarList(request, what);
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
    public Result processRadarList(Request request, String what)
            throws Exception {
        String column;
        String tag;
        String title;
        if (what.equals(WHAT_PRODUCT)) {
            column = COL_LEVEL3RADAR_PRODUCT;
            tag    = "product";
            title  = "Level 3 Radar Products";
        } else /*if(what.equals(WHAT_STATION))*/ {
            column = COL_LEVEL3RADAR_STATION;
            tag    = "station";
            title  = "Level 3 Radar Stations";
        }
        List where = assembleWhereClause(request);
        if(where.toString().indexOf(TABLE_FILES)==0) {
            where.add(SqlUtil.eq(COL_FILES_ID, COL_LEVEL3RADAR_ID));
        }
        String query = SqlUtil.makeSelect(SqlUtil.distinct(column),
                                          getTablesForQuery(request,Misc.newList(TABLE_LEVEL3RADAR)),
                                          SqlUtil.makeAnd(where));
        Statement    statement = repository.execute(query);
        String[]     products  = SqlUtil.readString(statement, 1);
        StringBuffer sb        = new StringBuffer();
        String output = repository.getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h2>Products</h2>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(tag + "s"));

        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        for (int i = 0; i < products.length; i++) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(repository.getLongName(products[i]) + " ("
                          + products[i] + ")");
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(
                    XmlUtil.tag(
                        tag,
                        XmlUtil.attrs(
                            ATTR_ID, products[i], ATTR_NAME,
                            repository.getLongName(products[i]))));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(products[i],
                                        repository.getLongName(products[i])));
                sb.append("\n");
            }
        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(tag + "s"));
        }
        return new Result(title, sb, repository.getMimeTypeFromOutput(output));
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param request _more_
     * @param where _more_
     *
     * @throws Exception _more_
     */
    public void addToForm(StringBuffer formBuffer, StringBuffer headerBuffer, Request request, List where)
            throws Exception {

        String[] maxdates = SqlUtil.readString(
                                repository.execute(
                                    SqlUtil.makeSelect(
                                        SqlUtil.max(COL_FILES_TODATE),
                                        getTablesForQuery(request,Misc.newList(TABLE_FILES)),
                                        SqlUtil.makeAnd(where))), 1);
        String[] mindates = SqlUtil.readString(
                                repository.execute(
                                    SqlUtil.makeSelect(
                                        SqlUtil.min(COL_FILES_FROMDATE),
                                        getTablesForQuery(request,Misc.newList(TABLE_FILES)),
                                        SqlUtil.makeAnd(where))), 1);
        String            maxdate      = ((maxdates.length > 0)
                                          ? maxdates[0]
                                          : "");
        String            mindate      = ((mindates.length > 0)
                                          ? mindates[0]
                                          : "");
        if(maxdate!=null) 
            maxdate = SqlUtil.getDateString(maxdate);
        if(mindate!=null) 
            mindate = SqlUtil.getDateString(mindate);



        List<TypeHandler> typeHandlers = repository.getTypeHandlers(request);
        if (typeHandlers.size() > 1) {
            List tmp = new ArrayList();
            for (TypeHandler typeHandler : typeHandlers) {
                tmp.add(new TwoFacedObject(typeHandler.getType(),
                                           typeHandler.getType()));
            }
            TwoFacedObject anyTfo = new TwoFacedObject(TYPE_ANY,TYPE_ANY);
            if(!tmp.contains(anyTfo)) {
                tmp.add(0, anyTfo);
            }
            String typeSelect = HtmlUtil.select(ARG_TYPE, tmp);
            formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Type:"),
                                          typeSelect));
        } else if (typeHandlers.size() == 1) {
            formBuffer.append(HtmlUtil.hidden(ARG_TYPE,
                                      typeHandlers.get(0).getType()));
            formBuffer.append(HtmlUtil.tableEntry("<b>Type:</b>",typeHandlers.get(0).getDescription())); 
        }

        String name = (String) request.get(ARG_NAME);
        if (name == null) {
            formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Name:"),
                                          HtmlUtil.input(ARG_NAME)));
        }



        String groupSelectSql = SqlUtil.makeSelect(
                                                SqlUtil.distinct(COL_FILES_GROUP_ID), 
                                                getTablesForQuery(request,Misc.newList(TABLE_FILES)),
                                                SqlUtil.makeAnd(where));

        List<Group> groups = repository.getGroups(
                                                  SqlUtil.readString(
                                                                     repository.execute(groupSelectSql),1));



        if (groups.size() > 1) {
            List groupList = new ArrayList();
            groupList.add("All");
            for (Group group : groups) {
                groupList.add(new TwoFacedObject(group.getFullName()));
            }
            String groupSelect = HtmlUtil.select(ARG_GROUP,
                                                 groupList);
            groupSelect+="&nbsp;" +HtmlUtil.checkbox(ARG_GROUP_CHILDREN,"true") +" (Search subgroups)";
            formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Group:"),
                                          groupSelect));
        } else if (groups.size() == 1) {
            formBuffer.append(HtmlUtil.hidden(ARG_GROUP,
                                      groups.get(0).getFullName()));
            formBuffer.append(HtmlUtil.tableEntry("<b>Group:</b>", groups.get(0).getFullName()));
        }


        String tag = (String) request.get(ARG_TAG);
        if(tag == null) {
            tag = "";
        }
        formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Tag:"),
                                              HtmlUtil.input(ARG_TAG,tag)));

        String calClick = "<A HREF=# onClick=\"" +
            "window.dateStyle = '';\n"+
            "window.dateField = document.query.fromdate\n" +
            "calendar =window.open('/repository/calendar.html','cal','WIDTH=300,HEIGHT=350')\n"+
            "\">CLICK</a>";

        formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Date Range:"),
                                      HtmlUtil.input(ARG_FROMDATE,  mindate)
                                                     + " -- " +
                                      HtmlUtil.input(ARG_TODATE,  maxdate)));

        if (isType(TYPE_LEVEL3RADAR)) {
            where.add(SqlUtil.eq(COL_FILES_ID, COL_LEVEL3RADAR_ID));
            String[] products = SqlUtil.readString(
                                    repository.execute(
                                                       SqlUtil.makeSelect(SqlUtil.distinct(COL_LEVEL3RADAR_PRODUCT),
                                                                          getTablesForQuery(request, Misc.newList(TABLE_FILES,TABLE_LEVEL3RADAR)),
                                                                          SqlUtil.makeAnd(where))), 1);
            List productList = new ArrayList();
            for (int i = 0; i < products.length; i++) {
                productList.add(
                    new TwoFacedObject(
                        repository.getLongName(products[i]), products[i]));
            }
            productList.add(0, "All");

            String[] stations = SqlUtil.readString(
                                    repository.execute(
                                                       SqlUtil.makeSelect(SqlUtil.distinct(COL_LEVEL3RADAR_STATION),
                                                                          getTablesForQuery(request, Misc.newList(TABLE_FILES,TABLE_LEVEL3RADAR)),
                                                                          SqlUtil.makeAnd(where))), 1);

            List stationList = new ArrayList();
            stationList.add(0,"All");
            for (int i = 0; i < stations.length; i++) {
                stationList.add(
                    new TwoFacedObject(
                        repository.getLongName(stations[i]), stations[i]));
            }
            productList.add(0, "All");
            if (stations.length > 1) {
                formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Station:"),
                        HtmlUtil.select(ARG_STATION,
                                        stationList)));
            } else if (stations.length == 1) {
                formBuffer.append(HtmlUtil.tableEntry("<b>Station:</b>",stations[0]));
                formBuffer.append(HtmlUtil.hidden(ARG_STATION,
                                          stations[0]));
            }
            if (products.length > 1) {
                formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Product:"),
                        HtmlUtil.select(ARG_PRODUCT,
                                        productList)));
            } else if (products.length == 1) {
                formBuffer.append(HtmlUtil.hidden(ARG_PRODUCT,
                                          products[0]));
            }
        }

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
    protected List assembleWhereClause(Request request) throws Exception {
        List   where = new ArrayList();
        String name  = (String) request.get(ARG_NAME);
        if (name != null) {
            name = name.trim();
        }


        String tag  = (String) request.get(ARG_TAG);
        if(tag!=null) {
            tag = tag.trim();
            if(tag.length()>0) {
                where.add(SqlUtil.eq(COL_FILES_ID, COL_TAGS_FILE_ID));
                addOr(COL_TAGS_NAME, tag,where,true);
            }
        }

        String groupName = (String) request.get(ARG_GROUP);
        if ((groupName != null) && !groupName.toLowerCase().equals("all")) {
            boolean doNot = groupName.startsWith("!");
            if(doNot) {
                groupName = groupName.substring(1);
            }
            Group  group          = repository.findGroupFromName(groupName);
            String searchChildren = (String) request.get(ARG_GROUP_CHILDREN);
            if(Misc.equals(searchChildren,"true")) {
                where.add((doNot?" NOT ":"") + SqlUtil.like(COL_FILES_GROUP_ID,
                                       group.getId()+"%"));
            } else {
                if(doNot) {
                    where.add(SqlUtil.neq(COL_FILES_GROUP_ID,
                                         SqlUtil.quote(group.getId())));
                } else {
                    where.add(SqlUtil.eq(COL_FILES_GROUP_ID,
                                         SqlUtil.quote(group.getId())));
                }
            }
        }
        String type = (String) request.get(ARG_TYPE);
        if ((type != null) && !type.equals(TYPE_ANY)) {
            addOr(COL_FILES_TYPE, type, where,true);
        }

        String fromdate = (String) request.get(ARG_FROMDATE);
        if ((fromdate != null) && (fromdate.trim().length() > 0)) {
            where.add(
                SqlUtil.ge(
                    COL_FILES_FROMDATE,
                    SqlUtil.quote(SqlUtil.getDateString(fromdate))));
        }

        String todate = (String) request.get(ARG_TODATE);
        if ((todate != null) && (todate.trim().length() > 0)) {
            where.add(
                SqlUtil.le(
                    COL_FILES_TODATE,
                    SqlUtil.quote(SqlUtil.getDateString(todate))));
        }

        String createDate = (String) request.get(ARG_CREATEDATE);
        if ((createDate != null) && (createDate.trim().length() > 0)) {
            where.add(
                SqlUtil.le(
                    COL_FILES_CREATEDATE,
                    SqlUtil.quote(SqlUtil.getDateString(createDate))));
        }

        if (isType(TYPE_LEVEL3RADAR)) {
            addOr(COL_LEVEL3RADAR_STATION,
                  (String) request.get(ARG_STATION), where,true);
            addOr(COL_LEVEL3RADAR_PRODUCT,
                  (String) request.get(ARG_PRODUCT), where,true);
            if (request.hasSetParameter(ARG_STATION)
                    || request.hasSetParameter(ARG_PRODUCT)) {
                where.add(SqlUtil.eq(COL_FILES_ID, COL_LEVEL3RADAR_ID));
            }
        }

        //        System.err.println ("name:" + name);
        if ((name != null) && (name.length() > 0)) {
            addOr(COL_FILES_NAME,name,where,true);
        }

        return where;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    protected List getTablesForQuery(Request request) {
        return getTablesForQuery(request, new ArrayList());
    }

    protected List getTablesForQuery(Request request, List initTables) {
        initTables.add(TABLE_FILES);
        if (isType(TYPE_LEVEL3RADAR)) {
            if (request.hasSetParameter(ARG_PRODUCT)
                    || request.hasSetParameter(ARG_STATION)) {
                initTables.add(TABLE_LEVEL3RADAR);
            }
        }

        if(request.hasSetParameter(ARG_TAG)) {
            initTables.add(TABLE_TAGS);        
            initTables.add(TABLE_FILES);        
        }
        return initTables;
    }



    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     * @param list _more_
     */
    private void addOr(String column, String value, List list, boolean quoteThem) {
        if ((value != null) && (value.trim().length() > 0)
                && !value.toLowerCase().equals("all")) {
            list.add("(" + SqlUtil.makeOrSplit(column, value, quoteThem) + ")");
        }
    }




    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

}


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

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TextResult;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



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


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Repository implements Constants {

    /** _more_          */
    private String urlBase = "/repository";

    /** _more_ */
    private long baseTime = System.currentTimeMillis();

    /** _more_ */
    private int keyCnt = 0;


    /** _more_ */
    private Connection connection;

    /** _more_          */
    private Hashtable typeHandlersMap = new Hashtable();


    /** _more_          */
    private static String timelineAppletTemplate;

    /** _more_          */
    private static String graphXmlTemplate;

    /** _more_          */
    private static String graphAppletTemplate;


    /**
     * _more_
     *
     *
     * @param driver _more_
     * @param connectionURL _more_
     * @param userName _more_
     * @param password _more_
     * @throws Exception _more_
     */
    public Repository(String driver, String connectionURL, String userName,
                      String password)
            throws Exception {
        Misc.findClass(driver);
        if (userName != null) {
            connection = DriverManager.getConnection(connectionURL, userName,
                    password);
        } else {
            connection = DriverManager.getConnection(connectionURL);
        }
        initTable();
        initTypeHandlers();
        initGroups();
    }

    /**
     * _more_
     *
     * @param incoming _more_
     * @param request _more_
     *
     * @return _more_
     */
    private boolean isRequest(String incoming, String request) {
        if (incoming.equals(request)) {
            return true;
        }
        if (incoming.equals(getUrlBase() + request)) {
            return true;
        }
        return false;
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
    public TextResult handleRequest(Request request) throws Exception {
        if (isRequest(request.getType(), Request.CALL_QUERY)) {
            return processQuery(request);
        } else if (isRequest(request.getType(), Request.CALL_SQL)) {
            return processSql(request);
        } else if (isRequest(request.getType(), Request.CALL_SEARCHFORM)) {
            return makeQueryForm(request);
        } else if (isRequest(request.getType(), Request.CALL_LIST)) {
            return processList(request);
        } else if (isRequest(request.getType(), Request.CALL_SHOWGROUP)) {
            return showGroup(request);
        } else if (isRequest(request.getType(), Request.CALL_SHOWFILE)) {
            return showFile(request);
        } else if (isRequest(request.getType(), Request.CALL_GRAPHVIEW)) {
            return getGraphApplet(request);
        } else if (isRequest(request.getType(), Request.CALL_GRAPH)) {
            return getGraph(request);
        } else {
            return null;
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initTable() throws Exception {
        boolean ok = true;
        try {
            execute("select * from dummy");
        } catch (Exception dummy) {
            ok = false;
        }

        if (ok) {
            //            loadLevel3RadarFiles();
            return;
        }
        System.err.println("making db");
        String sql =
            IOUtil.readContents("/ucar/unidata/repository/makedb.sql",
                                getClass());
        Statement statement = connection.createStatement();
        SqlUtil.loadSql(sql, statement);
        //        loadLevel3RadarFiles();
        loadTestFiles();
    }


    /**
     * _more_
     */
    protected void initTypeHandlers() {
        addTypeHandler(TypeHandler.TYPE_ANY,
                       new TypeHandler(this, TypeHandler.TYPE_ANY,
                                       "Any file types"));
        addTypeHandler(TypeHandler.TYPE_LEVEL3RADAR,
                       new TypeHandler(this, TypeHandler.TYPE_LEVEL3RADAR,
                                       "Level 3 Radar"));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getGUID() {
        return baseTime + "_" + (keyCnt++);
    }






    /**
     * _more_
     *
     * @param args _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult processSql(Request request) throws Exception {
        String       query = (String) request.get("query");
        StringBuffer sb    = new StringBuffer();
        sb.append(HtmlUtil.form(href("/sql")));
        sb.append("<input  name=\"query\" size=\"60\" value=\""
                  + ((query == null)
                     ? ""
                     : query) + "\"/>");
        sb.append("<input  type=\"submit\" value=\"Query\" />");
        sb.append("</form>\n");
        sb.append("<table>");
        if (query == null) {
            return new TextResult("SQL", sb.toString());
        }

        long             t1        = System.currentTimeMillis();
        Statement        statement = execute(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;
        int              cnt = 0;
        while ((results = iter.next()) != null) {
            ResultSetMetaData rsmd = results.getMetaData();
            while (results.next()) {
                int colcnt = 0;
                if (cnt++ == 0) {
                    sb.append("<table><tr>");
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        sb.append(
                            HtmlUtil.col(
                                HtmlUtil.bold(rsmd.getColumnLabel(i + 1))));
                    }
                    sb.append("</tr>");
                }
                sb.append("<tr>");
                while (colcnt < rsmd.getColumnCount()) {
                    sb.append(HtmlUtil.col(results.getString(++colcnt)));
                }
                sb.append("</tr>\n");
                if (cnt++ > 1000) {
                    sb.append(HtmlUtil.row("..."));
                    break;
                }
            }
        }
        sb.append("</table>");
        long t2 = System.currentTimeMillis();
        return new TextResult("SQL",
                              "Fetched:" + cnt + " rows in: " + (t2 - t1)
                              + "ms <p>" + sb.toString());
    }





    /**
     * _more_
     *
     * @param typeName _more_
     * @param typeHandler _more_
     */
    protected void addTypeHandler(String typeName, TypeHandler typeHandler) {
        typeHandlersMap.put(typeName, typeHandler);
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
    protected TypeHandler getTypeHandler(Request request) throws Exception {
        String type = (String) request.get(ARG_TYPE);
        if (type == null) {
            type = TypeHandler.TYPE_ANY;
        }
        return getTypeHandler(type);
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TypeHandler getTypeHandler(String type) throws Exception {
        TypeHandler typeHandler = (TypeHandler) typeHandlersMap.get(type);
        if (typeHandler == null) {
            try {
                Class c = Misc.findClass("ucar.unidata.repository." + type);
                Constructor ctor = Misc.findConstructor(c,
                                       new Class[] { Repository.class,
                        String.class });
                typeHandler = (TypeHandler) ctor.newInstance(new Object[] {
                    this,
                    type });
            } catch (ClassNotFoundException cnfe) {}
        }

        if (typeHandler == null) {
            typeHandler = new TypeHandler(this, type);
            addTypeHandler(type, typeHandler);
        }
        return typeHandler;
    }

    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(String sql) throws Exception {
        Statement statement = execute(sql);
        return getGroups(SqlUtil.readString(statement, 1));
    }

    /**
     * _more_
     *
     * @param groups _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(String[] groups) throws Exception {
        List<Group> groupList = new ArrayList<Group>();
        for (int i = 0; i < groups.length; i++) {
            Group group = findGroupFromId(groups[i]);
            if (group != null) {
                groupList.add(group);
            }
        }
        return groupList;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult makeQueryForm(Request request) throws Exception {
        List         where = assembleWhereClause(request);
        StringBuffer sb    = new StringBuffer();
        sb.append("<h2>Search Form</h2>");
        sb.append("<table cellpadding=\"5\">");
        sb.append(HtmlUtil.form(href("/query")));

        TypeHandler typeHandler = getTypeHandler(request);
        sb.append(HtmlUtil.hidden(typeHandler.getType(), ARG_TYPE));
        typeHandler.addToForm(sb, request, where);
        String output = (String) request.get(ARG_OUTPUT);
        if (output == null) {
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Output Type:"),
                                          HtmlUtil.select(ARG_OUTPUT,
                                              Misc.newList(OUTPUT_HTML,
                                                  OUTPUT_XML, OUTPUT_CSV))));
        } else {
            sb.append(HtmlUtil.hidden(output, ARG_OUTPUT));
        }

        sb.append(HtmlUtil.tableEntry("", HtmlUtil.submit("Search")));
        sb.append("<table>");
        sb.append("</form>");

        return new TextResult("Search Form", sb);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getNavLinks() {
        StringBuffer sb = new StringBuffer();
        sb.append(href("/showgroup", "Group List", "class=\"navtitle\""));
        sb.append("&nbsp;|&nbsp;");
        sb.append(href("/searchform", "Search", "class=\"navtitle\""));
        sb.append("&nbsp;|&nbsp;");
        sb.append(href("/sql", "SQL", "class=\"navtitle\""));
        sb.append("&nbsp;&nbsp;&nbsp;<span class=\"navtitle\">List:</span> ");
        sb.append(href(HtmlUtil.url("/list", "what", WHAT_TYPE), "Types",
                       "class=\"navtitle\""));
        sb.append("&nbsp;|&nbsp;");
        sb.append(href(HtmlUtil.url("/list", "what", WHAT_GROUP), "Groups",
                       "class=\"navtitle\""));
        sb.append("&nbsp;|&nbsp;");
        sb.append(href(HtmlUtil.url("/list", "what", WHAT_STATION),
                       "Stations", "class=\"navtitle\""));
        sb.append("&nbsp;|&nbsp;");
        sb.append(href(HtmlUtil.url("/list", "what", WHAT_PRODUCT),
                       "Products", "class=\"navtitle\""));
        return sb.toString();
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public int getMax(Request request) {
        String max = (String) request.get(ARG_MAX);
        if (max != null) {
            return new Integer(max.trim()).intValue();
        }
        return MAX_ROWS;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    protected String getValue(Request request, String key, String dflt) {
        String value = (String) request.get(key);
        if (value == null) {
            return dflt;
        }
        return value;
    }


    /**
     * _more_
     *
     * @param args _more_
     * @param column _more_
     * @param tag _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult processList(Request request) throws Exception {
        String what = getValue(request, ARG_WHAT, WHAT_TYPE);
        if (what.equals(WHAT_GROUP)) {
            return listGroups(request);
        } else if (what.equals(WHAT_TYPE)) {
            return listTypes(request);
        }
        TypeHandler typeHandler = getTypeHandler(request);
        return typeHandler.processList(request, what);
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
    protected TextResult showFile(Request request) throws Exception {
        String fileId = (String) request.get(ARG_ID);
        if (fileId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        StringBuffer sb = new StringBuffer();
        String query = SqlUtil.makeSelect(SqlUtil.comma(COL_FILES_ID,
                           COL_FILES_NAME, COL_FILES_DESCRIPTION,
                           COL_FILES_TYPE, COL_FILES_GROUP_ID,
                           COL_FILES_FILE, COL_FILES_FROMDATE,
                           COL_FILES_TODATE), TABLE_FILES,
                               SqlUtil.eq(COL_FILES_ID,
                                          SqlUtil.quote(fileId)));
        ResultSet results = execute(query).getResultSet();
        if ( !results.next()) {
            throw new IllegalArgumentException("Given file id:" + fileId
                    + " is not in database");
        }
        int    col = 1;
        String id  = results.getString(col++);
        DataInfo dataInfo =
            new DataInfo(results.getString(col++), results.getString(col++),
                         results.getString(col++),
                         findGroupFromId(results.getString(col++)),
                         results.getString(col++),
                         results.getDate(col++).getTime(),
                         results.getDate(col++).getTime());
        dataInfo.setId(id);
        TypeHandler typeHandler = getTypeHandler(dataInfo.getType());
        return typeHandler.showFile(dataInfo, request);
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
    protected TextResult showGroup(Request request) throws Exception {

        Group  theGroup  = null;
        String groupName = (String) request.get(ARG_GROUP);

        if (groupName != null) {
            request.getParameters().remove(ARG_GROUP);
            theGroup = findGroup(groupName);
        }
        List<Group> groups      = new ArrayList<Group>();
        TypeHandler typeHandler = getTypeHandler(request);
        boolean     topLevel    = false;
        if (theGroup == null) {
            topLevel = true;
            Statement statement = execute(SqlUtil.makeSelect(COL_GROUPS_ID,
                                      TABLE_GROUPS,
                                      COL_GROUPS_PARENT + " IS NULL"));
            groups.addAll(getGroups(SqlUtil.readString(statement, 1)));
        } else {
            groups.add(theGroup);
        }


        String       output = getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        List         where  = typeHandler.assembleWhereClause(request);
        StringBuffer sb     = new StringBuffer();
        if (topLevel) {
            sb.append(HtmlUtil.bold("Top Level Groups") + "<ul>");
        }


        String title = "Groups";
        for (Group group : groups) {
            if (topLevel) {
                sb.append(
                    "<li>"
                    + href(HtmlUtil.url(
                        "/showgroup", "group",
                        group.getFullName()), group.getFullName()) + "</a> "
                            + href(HtmlUtil.url(
                                "/graphview", "id", group.getFullName(),
                                "type", "group"), HtmlUtil.img(
                                    urlBase + "/tree.gif")));
                continue;
            }
            List  breadcrumbs = new ArrayList();
            List  titleList   = new ArrayList();
            Group parent      = group.getParent();
            while (parent != null) {
                titleList.add(0, parent.getName());
                breadcrumbs.add(0, href(HtmlUtil.url("/showgroup", "group",
                        parent.getFullName()), parent.getName()));
                parent = parent.getParent();
            }
            breadcrumbs.add(0, href("/showgroup", "Top"));
            titleList.add(group.getName());
            breadcrumbs.add(group.getName() + " "
                            + href(HtmlUtil.url("/graphview", "id",
                                group.getFullName(), "type",
                                "group"), HtmlUtil.img(urlBase
                                + "/tree.gif")));
            title = "Group: "
                    + StringUtil.join("&nbsp;&gt;&nbsp;", titleList);
            sb.append(HtmlUtil.bold("Group: "
                                    + StringUtil.join("&nbsp;&gt;&nbsp;",
                                        breadcrumbs)));
            sb.append("<hr>");
            List<Group> subGroups = getGroups(
                                        SqlUtil.makeSelect(
                                            COL_GROUPS_ID, TABLE_GROUPS,
                                            SqlUtil.eq(
                                                COL_GROUPS_PARENT,
                                                SqlUtil.quote(
                                                    group.getId()))));
            if (subGroups.size() > 0) {
                sb.append(HtmlUtil.bold("Sub groups:"));
                sb.append("<ul>");

                for (Group subGroup : subGroups) {
                    sb.append("<li>"
                              + href(HtmlUtil
                                  .url("/showgroup", "group",
                                       subGroup.getFullName()), subGroup
                                           .getFullName()) + "</a>");

                }
                sb.append("</ul>");
            }

            where.add(SqlUtil.eq(COL_FILES_GROUP_ID,
                                 SqlUtil.quote(group.getId())));
            String query =
                SqlUtil.makeSelect(SqlUtil.comma(COL_FILES_ID,
                    COL_FILES_NAME, COL_FILES_TYPE,
                    COL_FILES_FILE), typeHandler.getQueryOnTables(request),
                                     SqlUtil.makeAnd(where));
            Statement        stmt = execute(query);
            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            int              cnt = 0;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    if (cnt++ > 1000) {
                        sb.append("<li> ...");
                        break;
                    }
                    int    col  = 1;
                    String id   = results.getString(col++);
                    String name = results.getString(col++);
                    String type = results.getString(col++);
                    String file = results.getString(col++);
                    if (cnt == 1) {
                        sb.append(HtmlUtil.bold("Files:"));
                        sb.append("<ul>");
                    }
                    sb.append("<li>"
                              + href(HtmlUtil.url("/showfile", ARG_ID, id),
                                     name));
                }
            }
            if (cnt > 0) {
                sb.append("</ul>");
            }
        }
        if (topLevel) {
            sb.append("</ul>");
        }
        return new TextResult(title, sb, getMimeType(output));

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
    protected TextResult getGraphApplet(Request request) throws Exception {
        if (true || (graphAppletTemplate == null)) {
            graphAppletTemplate = IOUtil.readContents(
                "/ucar/unidata/repository/graphapplet.html", getClass());
        }

        String type = getValue(request, ARG_TYPE, "group");
        String id   = getValue(request, ARG_ID, null);

        if ((type == null) || (id == null)) {
            throw new IllegalArgumentException(
                "no type or id argument specified");
        }
        String html = StringUtil.replace(graphAppletTemplate, "%id%", id);
        html = StringUtil.replace(html, "%type%", type);
        return new TextResult("Graph View", html);
    }


    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getFileNodeXml(ResultSet results) throws Exception {
        int    col      = 1;
        String fileId   = results.getString(col++);
        String name     = results.getString(col++);
        String fileType = results.getString(col++);
        String groupId  = results.getString(col++);
        String file     = results.getString(col++);
        String nodeType = TYPE_FILE;
        if (fileType.equals(TypeHandler.TYPE_LEVEL3RADAR)) {
            nodeType = TypeHandler.TYPE_LEVEL3RADAR;
        }
        nodeType = TypeHandler.TYPE_LEVEL3RADAR;
        return XmlUtil.tag(TAG_NODE,
                           XmlUtil.attrs(ATTR_TYPE, nodeType, ATTR_ID,
                                         fileId, ATTR_TITLE, name));
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
    protected TextResult getGraph(Request request) throws Exception {

        if (true || (graphXmlTemplate == null)) {
            graphXmlTemplate = IOUtil.readContents(
                "/ucar/unidata/repository/graphtemplate.xml", getClass());
        }
        String id   = (String) request.get(ARG_ID);
        String type = (String) request.get(ARG_TYPE);
        if (id == null) {
            throw new IllegalArgumentException("Could not find id:"
                    + request);
        }
        if (type == null) {
            type = "group";
        }

        StringBuffer sb = new StringBuffer();
        if ( !type.equals(TYPE_GROUP)) {
            String filesQuery =
                SqlUtil.makeSelect(SqlUtil.comma(COL_FILES_ID,
                    COL_FILES_NAME, COL_FILES_TYPE, COL_FILES_GROUP_ID,
                    COL_FILES_FILE), TABLE_FILES,
                                     SqlUtil.eq(COL_FILES_ID,
                                         SqlUtil.quote(id)));


            ResultSet results = execute(filesQuery).getResultSet();
            if ( !results.next()) {
                throw new IllegalArgumentException("Unknown file id:" + id);
            }

            sb.append(getFileNodeXml(results));
            Group group = findGroupFromId(results.getString(4));
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, "group", ATTR_ID,
                                      group.getFullName(), ATTR_TITLE,
                                      group.getFullName())));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, group.getFullName(),
                                      ATTR_TO, results.getString(1))));

            String xml = StringUtil.replace(graphXmlTemplate, "%content%",
                                            sb.toString());
            return new TextResult("", new StringBuffer(xml),
                                  getMimeType(OUTPUT_GRAPH));
        }

        Group group = findGroup(id);
        if (group == null) {
            throw new IllegalArgumentException("Could not find group:" + id);
        }
        sb.append(XmlUtil.tag(TAG_NODE,
                              XmlUtil.attrs(ATTR_TYPE, "group", ATTR_ID,
                                            group.getFullName(), ATTR_TITLE,
                                            group.getFullName())));
        List<Group> subGroups = getGroups(SqlUtil.makeSelect(COL_GROUPS_ID,
                                    TABLE_GROUPS,
                                    SqlUtil.eq(COL_GROUPS_PARENT,
                                        SqlUtil.quote(group.getId()))));

        Group parent = group.getParent();
        if (parent != null) {
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, "group", ATTR_ID,
                                      parent.getFullName(), ATTR_TITLE,
                                      parent.getFullName())));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, parent.getFullName(),
                                      ATTR_TO, group.getFullName())));
        }


        for (Group subGroup : subGroups) {

            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, "group", ATTR_ID,
                                      subGroup.getFullName(), ATTR_TITLE,
                                      subGroup.getFullName())));

            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, group.getFullName(),
                                      ATTR_TO, subGroup.getFullName())));
        }

        String query = SqlUtil.makeSelect(SqlUtil.comma(COL_FILES_ID,
                           COL_FILES_NAME, COL_FILES_TYPE,
                           COL_FILES_GROUP_ID, COL_FILES_FILE), TABLE_FILES,
                               SqlUtil.eq(COL_FILES_GROUP_ID,
                                          SqlUtil.quote(group.getId())));
        SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                sb.append(getFileNodeXml(results));
                String fileId = results.getString(1);
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                          ATTR_FROM, group.getFullName(),
                                          ATTR_TO, fileId)));
                sb.append("\n");
            }
        }
        String xml = StringUtil.replace(graphXmlTemplate, "%content%",
                                        sb.toString());
        xml = StringUtil.replace(xml, "%root%", urlBase);
        return new TextResult("", new StringBuffer(xml),
                              getMimeType(OUTPUT_GRAPH));
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult listGroups(Request request) throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        String query =
            SqlUtil.makeSelect(SqlUtil.distinct(COL_FILES_GROUP_ID),
                               typeHandler.getQueryOnTables(request),
                               SqlUtil.makeAnd(where));

        Statement    statement = execute(query);
        String[]     groups    = SqlUtil.readString(statement, 1);
        StringBuffer sb        = new StringBuffer();
        String       output    = getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h2>Groups</h2>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_GROUPS));

        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }


        for (int i = 0; i < groups.length; i++) {
            Group group = findGroupFromId(groups[i]);
            if (group == null) {
                continue;
            }

            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>" + group.getFullName());
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_GROUP,
                                      XmlUtil.attrs(ATTR_NAME,
                                          group.getFullName(), ATTR_ID,
                                          group.getId())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(group.getFullName(), group.getId()));
                sb.append("\n");
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>\n");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_GROUPS));
        }

        return new TextResult("", sb, getMimeType(output));
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
    protected List<TypeHandler> getTypeHandlers(Request request)
            throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        String query =
            SqlUtil.makeSelect(SqlUtil.distinct(COL_FILES_TYPE),
                               typeHandler.getQueryOnTables(request),
                               SqlUtil.makeAnd(where));

        List<TypeHandler> typeHandlers = new ArrayList<TypeHandler>();
        String[]          types        = SqlUtil.readString(execute(query),
                                             1);
        for (int i = 0; i < types.length; i++) {
            typeHandlers.add(getTypeHandler(types[i]));
        }
        return typeHandlers;
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
    protected TextResult listTypes(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h2>Types</h2>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_TYPES));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        List<TypeHandler> typeHandlers = getTypeHandlers(request);
        for (TypeHandler theTypeHandler : typeHandlers) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(theTypeHandler.getType());
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_TYPE,
                                      XmlUtil.attrs(ATTR_TYPE,
                                          theTypeHandler.getType())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(theTypeHandler.getType(),
                                        theTypeHandler.getDescription()));
                sb.append("\n");
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_TYPES));
        }
        return new TextResult("", sb, getMimeType(output));
    }

    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    protected String getMimeType(String output) {
        if (output.equals(OUTPUT_CSV)) {
            return TextResult.TYPE_CSV;
        } else if (output.equals(OUTPUT_XML)) {
            return TextResult.TYPE_XML;
        } else if (output.equals(OUTPUT_GRAPH)) {
            return TextResult.TYPE_XML;
        } else {
            return TextResult.TYPE_HTML;
        }
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    protected String href(String url) {
        return urlBase + url;
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     *
     * @return _more_
     */
    protected String href(String url, String label) {
        return href(url, label, "");
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     * @param extra _more_
     *
     * @return _more_
     */
    protected String href(String url, String label, String extra) {
        return HtmlUtil.href(urlBase + url, label, extra);
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
        return getTypeHandler(request).assembleWhereClause(request);
    }






    /** _more_ */
    private Hashtable<String, Group> groupMap = new Hashtable<String,
                                                    Group>();

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void initGroups() throws Exception {
        Statement statement =
            execute(SqlUtil.makeSelect(SqlUtil.comma(COL_GROUPS_ID,
                COL_GROUPS_PARENT, COL_GROUPS_NAME,
                COL_GROUPS_DESCRIPTION), TABLE_GROUPS));

        ResultSet        results;
        SqlUtil.Iterator iter   = SqlUtil.getIterator(statement);
        List<Group>      groups = new ArrayList<Group>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                Group group = new Group(results.getString(col++),
                                        results.getString(col++),
                                        results.getString(col++),
                                        results.getString(col++));
                groups.add(group);
                groupMap.put(group.getId(), group);
            }
        }
        for (Group group : groups) {
            if (group.getParentId() != null) {
                group.setParent(groupMap.get(group.getParentId()));
            }
            groupMap.put(group.getFullName(), group);
        }
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroupFromId(String id) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return null;
        }
        Group group = groupMap.get(id);
        if (group != null) {
            return group;
        }
        String query = SELECT_GROUP + " WHERE "
                       + SqlUtil.eq("id", SqlUtil.quote(id));
        Statement statement = execute(query);
        ResultSet results   = statement.getResultSet();
        if (results.next()) {
            group = new Group(findGroupFromId(results.getString(2)),
                              results.getString(1), results.getString(3),
                              results.getString(4));
        } else {
            //????
            return null;
        }
        groupMap.put(id, group);
        return group;
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroup(String name) throws Exception {
        if (name.indexOf("_") >= 0) {
            Misc.printStack("id:" + name);
        }
        Group group = groupMap.get(name);
        if (group != null) {
            return group;
        }

        List<String> toks = (List<String>) StringUtil.split(name, "/", true,
                                true);

        Group  parent = null;
        String lastName;
        if ((toks.size() == 0) || (toks.size() == 1)) {
            lastName = name;
        } else {
            lastName = toks.get(toks.size() - 1);
            toks.remove(toks.size() - 1);
            parent = findGroup(StringUtil.join("/", toks));
        }
        String where = "";
        if (parent != null) {
            where += SqlUtil.eq(COL_GROUPS_PARENT,
                                SqlUtil.quote(parent.getId())) + " AND ";
        } else {
            where += COL_GROUPS_PARENT + " is null AND ";
        }
        where += SqlUtil.eq(COL_GROUPS_NAME, SqlUtil.quote(lastName));

        String    query     = SELECT_GROUP + " WHERE " + where;

        Statement statement = execute(query);
        ResultSet results   = statement.getResultSet();
        if (results.next()) {
            group = new Group(parent, results.getString(1),
                              results.getString(3), results.getString(4));
        } else {
            String insert = SqlUtil.makeInsert(TABLE_GROUPS, COLUMNS_GROUPS,
                                SqlUtil.comma(SqlUtil.quote(getGUID()),
                                    ((parent != null)
                                     ? SqlUtil.quote(parent.getId())
                                     : "NULL"), SqlUtil.quote(lastName),
                                         SqlUtil.quote(lastName)));
            statement.execute(insert);
            return findGroup(name);
            //            group = new Group(parent,0,lastName,lastName);
        }
        groupMap.put(group.getId(), group);
        groupMap.put(name, group);
        return group;
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
    protected List<DataInfo> getDataInfos(Request request) throws Exception {


        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        String query =
            SqlUtil.makeSelect(
                SqlUtil.comma(
                    COL_FILES_ID, COL_FILES_NAME, COL_FILES_DESCRIPTION,
                    COL_FILES_TYPE, COL_FILES_GROUP_ID, COL_FILES_FILE,
                    COL_FILES_FROMDATE,
                    COL_FILES_TODATE), typeHandler.getQueryOnTables(request),
                                       SqlUtil.makeAnd(where),
                                       "order by " + COL_FILES_FROMDATE);
        Statement        statement = execute(query, getMax(request));
        List<DataInfo>   dataInfos = new ArrayList<DataInfo>();
        ResultSet        results;
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int    col = 1;
                String id  = results.getString(col++);
                DataInfo dataInfo =
                    new DataInfo(results.getString(col++),
                                 results.getString(col++),
                                 results.getString(col++),
                                 findGroupFromId(results.getString(col++)),
                                 results.getString(col++),
                                 results.getTimestamp(col++).getTime());
                dataInfos.add(dataInfo);
                dataInfo.setId(id);

            }
        }
        return dataInfos;
    }


    /** _more_ */
    Properties productMap;


    /**
     * _more_
     *
     * @param product _more_
     *
     * @return _more_
     */
    protected String getLongName(String product) {
        return getLongName(product, product);
    }

    /**
     * _more_
     *
     * @param product _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    protected String getLongName(String product, String dflt) {
        if (productMap == null) {
            productMap = new Properties();
            try {
                InputStream s =
                    IOUtil.getInputStream(
                        "/ucar/unidata/repository/names.properties",
                        getClass());
                productMap.load(s);
            } catch (Exception exc) {
                System.err.println("err:" + exc);
            }
        }
        String name = (String) productMap.get(product);
        if (name != null) {
            return name;
        }
        //        System.err.println("not there:" + product+":");
        return dflt;
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
    protected TextResult processQuery(Request request) throws Exception {
        timelineAppletTemplate = IOUtil.readContents(
            "/ucar/unidata/repository/timelineapplet.html", getClass());
        List           times     = new ArrayList();
        List           labels    = new ArrayList();
        List<DataInfo> dataInfos = getDataInfos(request);
        StringBuffer   sb        = new StringBuffer();
        String         output    = getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h2>Query Results</h2>");
            sb.append("<table>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_CATALOG,
                                      XmlUtil.attrs(ATTR_NAME,
                                          "Query Results")));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        StringBufferCollection sbc = new StringBufferCollection();
        for (DataInfo dataInfo : dataInfos) {
            times.add(SqlUtil.format(new Date(dataInfo.getStartDate())));
            labels.add(dataInfo.getName());
            StringBuffer ssb = sbc.getBuffer(dataInfo.getType());
            if (output.equals(OUTPUT_HTML)) {
                ssb.append(HtmlUtil
                    .row(href(HtmlUtil
                        .url("/graphview", "id", dataInfo
                            .getId(), "type", dataInfo.getType()), HtmlUtil
                                .img(urlBase + "/tree.gif")) + " "
                                    + href(HtmlUtil
                                        .url("/showfile", ARG_ID, dataInfo
                                            .getId()), dataInfo
                                                .getFile()), ""
                                                    + new Date(dataInfo
                                                        .getStartDate())));
            } else if (output.equals(OUTPUT_XML)) {
                ssb.append(
                    XmlUtil.tag(
                        TAG_DATASET,
                        XmlUtil.attrs(
                            ATTR_NAME,
                            "" + new Date(dataInfo.getStartDate()),
                            ATTR_URLPATH, dataInfo.getFile())));

            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(dataInfo.getId(),
                                        dataInfo.getFile()));
            }
        }



        String tmp = StringUtil.replace(timelineAppletTemplate, "%times%",
                                        StringUtil.join(",", times));
        tmp = StringUtil.replace(tmp, "%labels%",
                                 StringUtil.join(",", labels));
        sb.append(tmp);


        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb  = sbc.getBuffer(type);
            if (output.equals(OUTPUT_HTML)) {
                sb.append(HtmlUtil.row(HtmlUtil.bold("Type:" + type)));
                sb.append(ssb);
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.openTag(TAG_DATASET,
                                          XmlUtil.attrs(ATTR_NAME, type)));
                sb.append(ssb);
                sb.append(XmlUtil.closeTag(TAG_DATASET));
            }
        }

        if (output.equals(OUTPUT_HTML)) {
            sb.append("</table>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_CATALOG));
        }
        return new TextResult("Query Results", sb, getMimeType(output));
    }



    /**
     * _more_
     *
     * @param rootDir _more_
     * @param groupName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<RadarInfo> collectLevel3radarFilesxxx(File rootDir,
            String groupName)
            throws Exception {
        long                  t1         = System.currentTimeMillis();
        final List<RadarInfo> radarInfos = new ArrayList();
        long                  baseTime   = new Date().getTime();
        Group                 group      = findGroup(groupName);
        for (int stationIdx = 0; stationIdx < 100; stationIdx++) {
            for (int i = 0; i < 10; i++) {
                String station = "stn" + stationIdx;
                String product = "product" + (i % 20);
                group = findGroup(groupName + "/" + station + "/" + product);
                radarInfos.add(new RadarInfo("", "", group,
                                             "file" + stationIdx + "_" + i
                                             + "_" + group, station, product,
                                                 baseTime
                                                     + radarInfos.size()
                                                         * 100));
            }
        }

        return radarInfos;
    }

    /**
     * _more_
     *
     *
     * @param rootDir _more_
     * @param groupName _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<RadarInfo> collectLevel3radarFiles(File rootDir,
            final String groupName)
            throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<RadarInfo>  radarInfos = new ArrayList();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                "([^/]+)/([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String  name    = f.toString();
                Matcher matcher = pattern.matcher(name);
                if ( !matcher.find()) {
                    return DO_CONTINUE;
                }
                if (radarInfos.size() % 5000 == 0) {
                    System.err.println("Found:" + radarInfos.size());
                }
                String station = matcher.group(1);
                String product = matcher.group(2);
                Group group = findGroup(groupName + "/" + "NIDS" + "/"
                                        + station + "/" + product);
                Date dttm = sdf.parse(matcher.group(3));
                radarInfos.add(new RadarInfo(dttm.toString(), "", group,
                                             f.toString(), station, product,
                                             dttm.getTime()));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        System.err.println("found:" + radarInfos.size() + " in " + (t2 - t1));
        return radarInfos;
    }


    /**
     * _more_
     *
     * @param rootDir _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<DataInfo> collectFiles(File rootDir) throws Exception {
        final String         rootStr    = rootDir.toString();
        final int            rootStrLen = rootStr.length();
        final List<DataInfo> dataInfos  = new ArrayList();
        IOUtil.FileViewer    fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String name = f.getName();
                //                System.err.println(name);
                if (name.startsWith(".")) {
                    return DO_DONTRECURSE;
                }
                if (f.isDirectory()) {
                    return DO_CONTINUE;
                }
                if ( !name.endsWith(".java")) {
                    return DO_CONTINUE;
                }
                String path    = f.toString();
                String noext   = IOUtil.stripExtension(path);


                String dirPath = f.getParent().toString();
                dirPath = dirPath.substring(rootStrLen);
                List toks = StringUtil.split(dirPath, File.separator, true,
                                             true);
                toks.add(0, "Files");
                Group group = findGroup(StringUtil.join("/", toks));
                dataInfos.add(new DataInfo(name, name, TypeHandler.TYPE_ANY,
                                           group, f.toString(),
                                           f.lastModified()));
                if (dataInfos.size() > 100) {
                    //                    return DO_STOP;
                }
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        return dataInfos;
    }

    /**
     * _more_
     *
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public void loadLevel3RadarFiles() throws Exception {
        File            rootDir = new File("/data/ldm/gempak/nexrad/NIDS");
        List<RadarInfo> files   = collectLevel3radarFiles(rootDir, "IDD");
        //        files.addAll(collectLevel3radarFiles(rootDir, "LDM/LDM2"));
        System.err.println("Inserting:" + files.size() + " files");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement filesInsert =
            connection.prepareStatement(INSERT_FILES);
        PreparedStatement radarInsert =
            connection.prepareStatement(INSERT_LEVEL3RADAR);

        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (RadarInfo radarInfo : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }
            int    col = 1;
            String id  = getGUID();
            filesInsert.setString(col++, id);
            filesInsert.setString(col++, radarInfo.getName());
            filesInsert.setString(col++, radarInfo.getDescription());
            filesInsert.setString(col++, TypeHandler.TYPE_LEVEL3RADAR);
            filesInsert.setString(col++, radarInfo.getGroupId());
            filesInsert.setString(col++, radarInfo.getFile().toString());
            filesInsert.setTimestamp(
                col++, new java.sql.Timestamp(radarInfo.getStartDate()));
            filesInsert.setTimestamp(
                col++, new java.sql.Timestamp(radarInfo.getStartDate()));
            filesInsert.addBatch();

            col = 1;
            radarInsert.setString(col++, id);
            radarInsert.setString(col++, radarInfo.getStation());
            radarInsert.setString(col++, radarInfo.getProduct());
            radarInsert.addBatch();
            batchCnt++;
            if (batchCnt > 100) {
                filesInsert.executeBatch();
                radarInsert.executeBatch();
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            filesInsert.executeBatch();
            radarInsert.executeBatch();
        }
        connection.setAutoCommit(true);
        connection.commit();
        long   t2      = System.currentTimeMillis();
        double seconds = (t2 - t1) / 1000.0;
        System.err.println("cnt:" + cnt + " time:" + seconds + " rate:"
                           + (cnt / seconds));

    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadTestFiles() throws Exception {
        File rootDir =
            new File(
                "c:/cygwin/home/jeffmc/unidata/src/idv/trunk/ucar/unidata");
        //o        File            rootDir = new File("/harpo/jeffmc/src/idv/trunk/ucar/unidata");
        List<DataInfo> files = collectFiles(rootDir);
        System.err.println("Inserting:" + files.size() + " files");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement filesInsert =
            connection.prepareStatement(INSERT_FILES);
        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (DataInfo dataInfo : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }
            int    col = 1;
            String id  = getGUID();
            filesInsert.setString(col++, id);
            filesInsert.setString(col++, dataInfo.getName());
            filesInsert.setString(col++, dataInfo.getDescription());
            filesInsert.setString(col++, TypeHandler.TYPE_ANY);
            filesInsert.setString(col++, dataInfo.getGroupId());
            filesInsert.setString(col++, dataInfo.getFile().toString());
            filesInsert.setTimestamp(
                col++, new java.sql.Timestamp(dataInfo.getStartDate()));
            filesInsert.setTimestamp(
                col++, new java.sql.Timestamp(dataInfo.getStartDate()));
            filesInsert.addBatch();

            batchCnt++;
            if (batchCnt > 100) {
                filesInsert.executeBatch();
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            filesInsert.executeBatch();
        }
        connection.setAutoCommit(true);
        connection.commit();
        long   t2      = System.currentTimeMillis();
        double seconds = (t2 - t1) / 1000.0;
        System.err.println("cnt:" + cnt + " time:" + seconds + " rate:"
                           + (cnt / seconds));

    }






    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement execute(String sql) throws Exception {
        return execute(sql, -1);
    }

    /**
     * _more_
     *
     * @param sql _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement execute(String sql, int max) throws Exception {
        Statement statement = connection.createStatement();
        if (max > 0) {
            statement.setMaxRows(max);
        }
        long t1 = System.currentTimeMillis();
        //        System.err.println("query:" + sql);
        statement.execute(sql);
        long t2 = System.currentTimeMillis();
        //        System.err.println("done:" + (t2-t1));
        return statement;
    }


    /**
     * _more_
     *
     * @param stmt _more_
     * @param sql _more_
     *
     * @throws Exception _more_
     */
    public void eval(String sql) throws Exception {
        Statement statement = execute(sql);
        String[]  results   = SqlUtil.readString(statement, 1);
        for (int i = 0; (i < results.length) && (i < 10); i++) {
            System.err.print(results[i] + " ");
            if (i == 9) {
                System.err.print("...");
            }
        }
    }



    /**
     * Set the UrlBase property.
     *
     * @param value The new value for UrlBase
     */
    public void setUrlBase(String value) {
        urlBase = value;
    }

    /**
     * Get the UrlBase property.
     *
     * @return The UrlBase
     */
    public String getUrlBase() {
        return urlBase;
    }




}


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
public class Repository implements Constants, Tables {

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


    /** _more_ */
    private Hashtable<String, Group> groupMap = new Hashtable<String,
                                                    Group>();

    private Hashtable<String, User> userMap = new Hashtable<String,
                                                    User>();


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
        } else if (request.getType().startsWith(getUrlBase()+Request.CALL_FETCH)) {
            return getFile(request);
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
        System.err.println("making db");
        String sql =
            IOUtil.readContents("/ucar/unidata/repository/makedb.sql",
                                getClass());
        Statement statement = connection.createStatement();
        SqlUtil.loadSql(sql, statement, true);
        loadTestData();
    }

    public void loadTestData() throws Exception {
        ResultSet results = execute("select count(*) from files").getResultSet();
        results.next();
        makeUserIfNeeded(new User("jdoe", "John Doe", true));
        makeUserIfNeeded(new User("jsmith", "John Smith", false));
        if(results.getInt(1)==0) {
            System.err.println ("Adding test data");
            loadLevel3RadarFiles();
            loadTestFiles();
        }
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
        String       query = (String) request.get(ARG_QUERY);
        StringBuffer sb    = new StringBuffer();
        sb.append(HtmlUtil.form(href("/sql")));
        sb.append(HtmlUtil.submit("Execute"));
        sb.append(HtmlUtil.input(ARG_QUERY, query, " size=\"60\" "));
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
            Group group = findGroup(groups[i]);
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
        sb.append(HtmlUtil.form(href("/query")," name=\"query\""));

        TypeHandler typeHandler = getTypeHandler(request);
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
        sb.append(href(HtmlUtil.url("/list", "what", WHAT_TAG), "Tags",
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
        } else if (what.equals(WHAT_TAG)) {
            return listTags(request);
        } else if (what.equals(WHAT_TYPE)) {
            return listTypes(request);
        }
        TypeHandler typeHandler = getTypeHandler(request);
        return typeHandler.processList(request, what);
    }


    
    protected TextResult getFile(Request request) throws Exception {
        String fileId = (String) request.get(ARG_ID);
        if (fileId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        StringBuffer sb = new StringBuffer();
        String query = SqlUtil.makeSelect(COL_FILES_FILE, TABLE_FILES,SqlUtil.eq(COL_FILES_ID,
                                                                     SqlUtil.quote(fileId)));
        ResultSet results = execute(query).getResultSet();
        if ( !results.next()) {
            throw new IllegalArgumentException("Given file id:" + fileId
                    + " is not in database");
        }
        String fileName = results.getString(1);
        String contents =  IOUtil.readContents(
                                               fileName, getClass());
        return new TextResult("", new StringBuffer(contents),IOUtil.getFileExtension(fileName));
    }

    protected FilesInfo findFile(String fileId) throws Exception {
        String query = SqlUtil.makeSelect(COLUMNS_FILES,
                                          TABLE_FILES,
                                          SqlUtil.eq(COL_FILES_ID,
                                                     SqlUtil.quote(fileId)));
        ResultSet results = execute(query).getResultSet();
        if ( !results.next()) {
            throw new IllegalArgumentException("Given file id:" + fileId
                    + " is not in database");
        }
        int    col = 1;
        //id,name,desc,type,group,user,file,createdata,fromdate,todate
        FilesInfo filesInfo =
            new FilesInfo(results.getString(col++),
                          results.getString(col++), 
                          results.getString(col++),
                          results.getString(col++),
                          findGroup(results.getString(col++)),
                          findUser(results.getString(col++)),
                          results.getString(col++),
                          results.getDate(col++).getTime(),
                          results.getDate(col++).getTime(),
                          results.getDate(col++).getTime());
        return filesInfo;
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
        FilesInfo filesInfo = findFile(fileId);
        TypeHandler typeHandler = getTypeHandler(filesInfo.getType());
        return typeHandler.showFile(filesInfo, request);
    }

    protected long currentTime() {
        return new Date().getTime();
        
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
            theGroup = findGroupFromName(groupName);
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
        if (type.equals(TYPE_TAG)) {
            String query =
                SqlUtil.makeSelect(SqlUtil.comma(COL_FILES_ID, 
                    COL_FILES_NAME, COL_FILES_TYPE, COL_FILES_GROUP_ID,
                    COL_FILES_FILE), TABLE_FILES+","+TABLE_TAGS,
                                   SqlUtil.eq(COL_TAGS_FILE_ID, COL_FILES_ID) +" AND " +
                                   SqlUtil.eq(COL_TAGS_NAME, SqlUtil.quote(id))); 

            //            System.err.println ("tag query:" + query);
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, TYPE_TAG, ATTR_ID,
                                                id, ATTR_TITLE, id)));
            SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
            ResultSet results;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    sb.append(getFileNodeXml(results));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE, "taggedby",
                                                        ATTR_FROM, id,
                                                        ATTR_TO, results.getString(1))));
                }
            }
            String xml = StringUtil.replace(graphXmlTemplate, "%content%",
                                            sb.toString());
            return new TextResult("", new StringBuffer(xml),
                                  getMimeType(OUTPUT_GRAPH));
        }


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
            Group group = findGroup(results.getString(4));
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

        Group group = findGroupFromName(id);
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
            Group group = findGroup(groups[i]);
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
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult listTags(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h2>Tags</h2>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_TAGS));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        String tables = TABLE_TAGS;
        if(where.size()>0) {
            where.add(0,SqlUtil.eq(COL_TAGS_FILE_ID, COL_FILES_ID));
            tables = tables +","+ typeHandler.getQueryOnTables(request);
        }

        String query =
            SqlUtil.makeSelect(SqlUtil.distinct(COL_TAGS_NAME),
                               tables,
                               SqlUtil.makeAnd(where));

        String[]tags = SqlUtil.readString(execute(query),1);
        for(int i=0;i<tags.length;i++) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(tags[i]);
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_TAG,
                                      XmlUtil.attrs(ATTR_NAME,
                                                    tags[i])));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(tags[i]);
                sb.append("\n");
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_TAGS));
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
                                        findGroup(results.getString(col++)),
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




    protected void makeUserIfNeeded(User user) throws Exception {
        if(findUser(user.getId()) ==null) {
            makeUser(user);
        }
    }

    protected void makeUser(User user) throws Exception {
        execute(INSERT_USERS, new Object[]{user.getId(),user.getName(),new Boolean(user.getAdmin())});
    }

    protected User findUser(String id) throws Exception {
        if(id == null) return null;
        User user = userMap.get(id);
        if(user!=null) return user;
        String query = SqlUtil.makeSelect(COLUMNS_USERS,
                                          TABLE_USERS,
                                          SqlUtil.eq(COL_USERS_ID, SqlUtil.quote(id)));
        ResultSet results = execute(query).getResultSet();
        if (!results.next()) {
            //            throw new IllegalArgumentException ("Could not find  user id:" + id + " sql:" + query);
            return null;
        } else {
            int col = 1;
            user = new User(results.getString(col++),
                            results.getString(col++),
                            results.getBoolean(col++));
        }

        userMap.put(user.getId(), user);
        return  user;
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
    protected Group findGroup(String id) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return null;
        }
        Group group = groupMap.get(id);
        if (group != null) {
            return group;
        }
        String query = SqlUtil.makeSelect(COLUMNS_GROUPS,
                                          TABLE_GROUPS,
                                          SqlUtil.eq(COL_GROUPS_ID, SqlUtil.quote(id)));
        Statement statement = execute(query);
        //id,parent,name,description
        ResultSet results   = statement.getResultSet();
        if (results.next()) {
            group = new Group(results.getString(1), 
                              findGroup(results.getString(2)),
                              results.getString(3),
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
    protected Group findGroupFromName(String name) throws Exception {
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
            parent = findGroupFromName(StringUtil.join("/", toks));
        }
        String where = "";
        if (parent != null) {
            where += SqlUtil.eq(COL_GROUPS_PARENT,
                                SqlUtil.quote(parent.getId())) + " AND ";
        } else {
            where += COL_GROUPS_PARENT + " is null AND ";
        }
        where += SqlUtil.eq(COL_GROUPS_NAME, SqlUtil.quote(lastName));

        String    query     = SqlUtil.makeSelect(COLUMNS_GROUPS, TABLE_GROUPS,where);

        Statement statement = execute(query);
        ResultSet results   = statement.getResultSet();
        if (results.next()) {
            group = new Group(results.getString(1),parent,
                              results.getString(3), results.getString(4));
        } else {
            String id  =   getGUID();
            execute(INSERT_GROUPS, new Object[]{
                id,
                (parent!=null?parent.getId():null),
                lastName,
                lastName});
            group = new Group(id,parent,lastName,lastName);
        }
        groupMap.put(group.getId(), group);
        groupMap.put(name, group);
        return group;
    }


    protected void execute(String insert, Object[]values) throws Exception {
        PreparedStatement pstmt = connection.prepareStatement(insert);
        for(int i=0;i<values.length;i++) {
            //Assume null is a string
            if(values[i]==null) {
                pstmt.setNull(i+1,java.sql.Types.VARCHAR);
            } else {
                pstmt.setObject(i+1, values[i]);
            }
        }
        pstmt.execute();
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
    protected List<FilesInfo> getFilesInfos(Request request) throws Exception {


        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        String query =
            SqlUtil.makeSelect(COLUMNS_FILES, typeHandler.getQueryOnTables(request),
                                       SqlUtil.makeAnd(where),
                                       "order by " + COL_FILES_FROMDATE);
        System.err.println("Query:"+ query);
        Statement        statement = execute(query, getMax(request));
        List<FilesInfo>   filesInfos = new ArrayList<FilesInfo>();
        ResultSet        results;
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int    col = 1;
                //id,name,desc,type,group,user,file,createdata,fromdate,todate
                FilesInfo filesInfo =
                    new FilesInfo(results.getString(col++),results.getString(col++),
                                 results.getString(col++),
                                 results.getString(col++),
                                 findGroup(results.getString(col++)),
                                 findUser(results.getString(col++)),
                                 results.getString(col++),
                                 results.getTimestamp(col++).getTime(),
                                 results.getTimestamp(col++).getTime(),
                                  results.getTimestamp(col++).getTime());
                filesInfos.add(filesInfo);
            }
        }
        return filesInfos;
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

    protected String getFileTreeLink(FilesInfo filesInfo) {
        return href(HtmlUtil.url("/graphview", "id", filesInfo.getId(), "type", filesInfo.getType()), HtmlUtil
                    .img(urlBase + "/tree.gif", "alt=\"Show file in graph\" title=\"Show file in graph\" "));
    }

    protected String getFileFetchLink(FilesInfo filesInfo) {
        return href(HtmlUtil.url("/fetch/" + filesInfo.getName(),"id",filesInfo.getId()),HtmlUtil.img(href("/Fetch.gif"),"alt=\"Download file\"  title=\"Download file\"  "));
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
        List<FilesInfo> filesInfos = getFilesInfos(request);
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
        for (FilesInfo filesInfo : filesInfos) {
            times.add(SqlUtil.format(new Date(filesInfo.getStartDate())));
            labels.add(filesInfo.getName());
            StringBuffer ssb = sbc.getBuffer(filesInfo.getType());
            if (output.equals(OUTPUT_HTML)) {
                String links = getFileFetchLink(filesInfo) +" " +getFileTreeLink(filesInfo);
                ssb.append(HtmlUtil.row(links+" " + href(HtmlUtil.url("/showfile", ARG_ID, filesInfo.getId()),
                                                         filesInfo.getName()), 
                                        "" + new Date(filesInfo.getStartDate())));
            } else if (output.equals(OUTPUT_XML)) {
                ssb.append(
                    XmlUtil.tag(
                        TAG_DATASET,
                        XmlUtil.attrs(
                            ATTR_NAME,
                            "" + new Date(filesInfo.getStartDate()),
                            ATTR_URLPATH, filesInfo.getFile())));

            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(filesInfo.getId(),
                                        filesInfo.getFile()));
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
    public List<Level3RadarInfo> collectLevel3radarFiles(File rootDir,
            String groupName)
            throws Exception {
        long                  t1         = System.currentTimeMillis();
        final List<Level3RadarInfo> radarInfos = new ArrayList();
        long                  baseTime   = currentTime();
        Group                 group      = findGroupFromName(groupName);
        User user = findUser("jdoe");
        for (int stationIdx = 0; stationIdx < 100; stationIdx++) {
            for (int i = 0; i < 10; i++) {
                String station = "stn" + stationIdx;
                String product = "product" + (i % 20);
                group = findGroupFromName(groupName + "/" + station + "/" + product);
                radarInfos.add(new Level3RadarInfo(getGUID(),
                                                   "", "", group,
                                                   user,
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
    public List<Level3RadarInfo> collectLevel3radarFilesxxx(File rootDir,
            final String groupName)
            throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<Level3RadarInfo>  radarInfos = new ArrayList();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                "([^/]+)/([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        final User user = findUser("jdoe");
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
                Group group = findGroupFromName(groupName + "/" + "NIDS" + "/"
                                        + station + "/" + product);
                Date dttm = sdf.parse(matcher.group(3));
                radarInfos.add(new Level3RadarInfo(getGUID(),
                                                   dttm.toString(), "", group, user,
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
    public List<FilesInfo> collectFiles(File rootDir) throws Exception {
        final String         rootStr    = rootDir.toString();
        final int            rootStrLen = rootStr.length();
        final List<FilesInfo> filesInfos  = new ArrayList();
        final User user = findUser("jdoe");
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
                //                if ( !name.endsWith(".java")) {
                //                    return DO_CONTINUE;
                //                }
                String path    = f.toString();
                String noext   = IOUtil.stripExtension(path);


                String dirPath = f.getParent().toString();
                dirPath = dirPath.substring(rootStrLen);
                List toks = StringUtil.split(dirPath, File.separator, true,
                                             true);
                toks.add(0, "Files");
                Group group = findGroupFromName(StringUtil.join("/", toks));
                FilesInfo fileInfo = new FilesInfo(getGUID(),
                                             name, name, TypeHandler.TYPE_ANY,
                                             group, user, f.toString(),
                                             f.lastModified());
                String ext = IOUtil.getFileExtension(path);
                if(ext.startsWith(".")) ext = ext.substring(1);
                if(ext.length()>0) 
                    fileInfo.addTag(ext);
                filesInfos.add(fileInfo);
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        return filesInfos;
    }

    protected void setStatement(FilesInfo filesInfo, PreparedStatement statement) throws Exception {
        int    col = 1;
        //id,name,desc,type,group,user,file,createdata,fromdate,todate
        statement.setString(col++, filesInfo.getId());
        statement.setString(col++, filesInfo.getName());
        statement.setString(col++, filesInfo.getDescription());
        statement.setString(col++, filesInfo.getType());
        statement.setString(col++, filesInfo.getGroupId());
        statement.setString(col++, filesInfo.getUser().getId());
        statement.setString(col++, filesInfo.getFile().toString());
        statement.setTimestamp(
                                 col++, new java.sql.Timestamp(currentTime()));
        statement.setTimestamp(
                                 col++, new java.sql.Timestamp(filesInfo.getStartDate()));
        statement.setTimestamp(
                                 col++, new java.sql.Timestamp(filesInfo.getStartDate()));
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
        List<Level3RadarInfo> files   = collectLevel3radarFiles(rootDir, "IDD");
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
        for (Level3RadarInfo radarInfo : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }

            String id  = getGUID();
            radarInfo.setId(id);
            setStatement(radarInfo, filesInsert);
            filesInsert.addBatch();
            int col = 1;
            radarInsert.setString(col++, radarInfo.getId());
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
        List<FilesInfo> files = collectFiles(rootDir);
        System.err.println("Inserting:" + files.size() + " test files");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement filesInsert =
            connection.prepareStatement(INSERT_FILES);
        PreparedStatement tagsInsert =
            connection.prepareStatement(INSERT_TAGS);
        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (FilesInfo filesInfo : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }
            setStatement(filesInfo, filesInsert);
            filesInsert.addBatch();
            batchCnt++;
            List<String> tags = filesInfo.getTags();
            if(tags !=null) {
                for(String tag: tags) {
                    tagsInsert.setString(1, tag);
                    tagsInsert.setString(2, filesInfo.getId());
                    batchCnt++;
                    tagsInsert.addBatch();
                }
            }

            if (batchCnt > 100) {
                filesInsert.executeBatch();
                tagsInsert.executeBatch();
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            filesInsert.executeBatch();
            tagsInsert.executeBatch();
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
        try {
            statement.execute(sql);
        } catch(Exception exc) {
            System.err.println("ERROR:" + sql);
            throw exc;
        }
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


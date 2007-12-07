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
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;
import org.w3c.dom.*;

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


import java.io.*;
import java.text.SimpleDateFormat;

import java.util.zip.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Date;
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

    private static final int PAGE_CACHE_LIMIT = 100;

    private Harvester harvester;

    private Properties mimeTypes;

    /** _more_ */
    private Properties productMap;

    private Properties  properties = new Properties();

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
     * @throws Exception _more_
     */
    public Repository(String[]args)  throws Exception {
        properties = new Properties();
        properties.load(IOUtil.getInputStream("/ucar/unidata/repository/repository.properties",getClass()));
        for (int i = 0; i < args.length; i++) {
            if(args[i].endsWith(".properties")) {
                properties.load(IOUtil.getInputStream(args[i],getClass()));
            }
        }
        Misc.findClass((String) properties.get(PROP_DB_DRIVER));
        urlBase = (String)properties.get(PROP_HTML_URLBASE);
        if(urlBase == null) urlBase = "";
        harvester = new Harvester(this);
    }

    protected void makeConnection() throws Exception {
        String userName = (String) properties.get(PROP_DB_USER);
        String password = (String) properties.get(PROP_DB_PASSWORD);
        String connectionURL = (String) properties.get(PROP_DB_URL);

        System.err.println ("db:" + connectionURL);
        if (userName != null) {
            connection = DriverManager.getConnection(connectionURL, userName,
                                                     password);
        } else {
            connection = DriverManager.getConnection(connectionURL);
        }
    }


    protected void init() throws Exception {
        makeConnection();
        mimeTypes= new Properties();
        mimeTypes.load(IOUtil.getInputStream("/ucar/unidata/repository/mimetypes.properties",getClass()));
        initTable();
        initTypeHandlers();
        initGroups();
        initRequests();
    }

    protected boolean isAppletEnabled(Request request) {
        if(!getProperty(PROP_SHOW_APPLET, true)) return false;
        String arg = (String) request.get(ARG_APPLET,"true");
        return Misc.equals(arg,"true");
    }


    List api = new ArrayList();
    Hashtable requestMap = new Hashtable();

    protected void addRequest(String request,String methodName, Permission permission, boolean canCache) {
        Class []paramTypes = new Class[]{Request.class};
        Method method = Misc.findMethod(getClass(),methodName, paramTypes);
        if(method == null) throw new IllegalArgumentException ("Unknown request method:" + methodName);
        ApiMethod apiMethod  =new ApiMethod(request, permission, method,canCache);
        api.add(apiMethod);
        requestMap.put(request, apiMethod);
        requestMap.put(getUrlBase()+request, apiMethod);
    }

    protected void initRequests() throws Exception {
        Element apiRoot = XmlUtil.getRoot("/ucar/unidata/repository/api.xml",getClass());
        List children = XmlUtil.findChildren(apiRoot, TAG_METHOD);
        for(int i=0;i<children.size();i++) {
            Element node = (Element) children.get(i);
            String request = XmlUtil.getAttribute(node,ATTR_API_REQUEST);
            String method = XmlUtil.getAttribute(node,ATTR_API_METHOD);
            boolean admin = XmlUtil.getAttribute(node,ATTR_API_ADMIN,true);
            boolean canCache = XmlUtil.getAttribute(node,ATTR_API_CANCACHE,false);
            addRequest(request, method, new Permission(admin),canCache);
        }
    }

    private Hashtable pageCache = new Hashtable();    
    private List     pageCacheList = new ArrayList();



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleRequest(Request request) throws Exception {
        String incoming = request.getType().trim();
        if(incoming.endsWith("/")) {
            incoming = incoming.substring(0,incoming.length()-1);
        }
        //        System.err.println ("incoming:"+incoming+":");
        if(incoming.startsWith(getUrlBase())) {
            incoming = incoming.substring(getUrlBase().length());
        }
        User user = request.getRequestContext().getUser();
        ApiMethod apiMethod = (ApiMethod)requestMap.get(incoming);
        if(apiMethod == null) {
            incoming = incoming;
            for(int i=0;i<api.size();i++) {
                ApiMethod tmp = (ApiMethod) api.get(i);
                String path = tmp.getRequest();
                if(path.endsWith("/*")) {
                    path = path.substring(0,path.length()-2);
                    //                    System.err.println (path +":"+incoming);
                    if(incoming.startsWith(path)) {
                        apiMethod = tmp;
                        break;
                    }
                }
            }
        }
        Result result = null;
        if(apiMethod!=null) {
            if(canCache() && apiMethod.getCanCache()) {
                result = (Result) pageCache.get(request);
                //                System.err.println("from cache:" + request.getType() +  " " +(result!=null));
                if(result!=null) {
                    pageCacheList.remove(request);
                    pageCacheList.add(request);
                }
            }
            if(result == null) {
                if(!apiMethod.getPermission().isRequestOk(request, this)) {
                    result =  new Result("Error",new StringBuffer("Access Violation"));
                } else {
                    if(connection ==null && !incoming.startsWith("/admin")) {
                        result =  new Result("No Database",new StringBuffer("Database is shutdown"));
                    } else {
                        result =(Result) apiMethod.getMethod().invoke(this, new Object[]{request});
                    }
                }
            }
        }  else {
            //            result = new Result("Unknown Request",new StringBuffer("Unknown request:" + request.getType()));
        }
        if(result!=null) {
            if(canCache() && apiMethod.getCanCache()) {
                pageCache.put(request,result);
                pageCacheList.add(request);
                while(pageCacheList.size()>PAGE_CACHE_LIMIT) {
                    Request tmp = (Request)pageCacheList.remove(0);
                    pageCache.remove(tmp);
                }
            }
            result.putProperty(PROP_NAVLINKS, getNavLinks(request));
        }
        return result;
    }


    protected boolean canCache() {
        return getProperty(PROP_DB_CANCACHE, true);
    }

    public String getProperty(String name) {
        return (String)properties.get(name);
    }

    public boolean getProperty(String name, boolean dflt) {
        return Misc.getProperty(properties,name, dflt);
    }



    public Result doAdmin(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        String what = request.get("what","nothing");

        if(what.equals("shutdown")) {
            connection.close();
            connection = null;
            sb.append("Database is shut down");
        } else if(what.equals("restart")) {
            if(connection!=null) {
                sb.append("Already connected to database");
            } else {
                makeConnection();
                sb.append("Database is restarted");
            }
        }
        Result result = new Result("Administration", sb);
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return result;
    }

    public Result showAdmin(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("<h2>Administration</h2>");
        sb.append(HtmlUtil.form(href("/admindo")," name=\"admin\""));
        if(connection ==null) {
            sb.append(HtmlUtil.hidden("what", "restart"));
            sb.append(HtmlUtil.submit("Restart Database"));
        } else {
            sb.append(HtmlUtil.hidden("what", "shutdown"));
            sb.append(HtmlUtil.submit("Shut Down Database"));
        }
        sb.append("</form>");
        Result result = new Result("Administration", sb);
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return  result;
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
        makeUserIfNeeded(new User("jdoe", "John Doe", true));
        makeUserIfNeeded(new User("jsmith", "John Smith", false));
        loadTestData();
    }

    public void loadTestData() throws Exception {
        ResultSet results = execute("select count(*) from " + TABLE_ENTRIES).getResultSet();
        results.next();
        if(results.getInt(1)==0) {
            System.err.println ("Adding test data");
            loadTestFiles();
            loadSatelliteFiles();
            loadLevel3RadarFiles();
            loadLevel2RadarFiles();
        }
    }



    /**
     * _more_
     */
    protected void initTypeHandlers() {
        addTypeHandler(TypeHandler.TYPE_ANY,
                       new TypeHandler(this, TypeHandler.TYPE_ANY,
                                       "Any file types"));
        addTypeHandler(TypeHandler.TYPE_FILE,
                       new TypeHandler(this, TypeHandler.TYPE_FILE,
                                       "File"));
        addTypeHandler(TypeHandler.TYPE_LEVEL3RADAR,
                       new TypeHandler(this, TypeHandler.TYPE_LEVEL3RADAR,
                                       "Level 3 Radar"));
        addTypeHandler(TypeHandler.TYPE_LEVEL2RADAR,
                       new TypeHandler(this, TypeHandler.TYPE_LEVEL2RADAR,
                                       "Level 2 Radar"));
        addTypeHandler(TypeHandler.TYPE_SATELLITE,
                       new TypeHandler(this, TypeHandler.TYPE_SATELLITE,
                                       "Satellite"));
        addTypeHandler(TypeHandler.TYPE_MODEL,
                       new TypeHandler(this, TypeHandler.TYPE_MODEL,
                                       "Model"));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getGUID() {
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
    public Result processSql(Request request) throws Exception {
        String       query = (String) request.get(ARG_QUERY);
        StringBuffer sb    = new StringBuffer();
        sb.append("<H2>SQL</h2>");
        sb.append(HtmlUtil.form(href("/sql")));
        sb.append(HtmlUtil.submit("Execute"));
        sb.append(HtmlUtil.input(ARG_QUERY, query, " size=\"60\" "));
        sb.append("</form>\n");
        sb.append("<table>");
        if (query == null) {
            Result result = new Result("SQL", sb);        
            result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
            return result;
        }

        long             t1        = System.currentTimeMillis();
        
        Statement        statement=null;
        System.err.println ("before");
        try {
            statement = execute(query);
        } catch(Exception exc) {
            exc.printStackTrace();
            throw exc;
        }
        System.err.println ("after");
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;
        int              cnt = 0;
        Hashtable map = new Hashtable();
        int unique = 0;
        while ((results = iter.next()) != null) {
            ResultSetMetaData rsmd = results.getMetaData();
            while (results.next()) {
                cnt++;
                if (cnt > 1000) {
                    continue;
                }
                int colcnt = 0;
                if (cnt == 1) {
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
                //                if (cnt++ > 1000) {
                //                    sb.append(HtmlUtil.row("..."));
                //                    break;
                //                }
            }
        }
        sb.append("</table>");
        long t2 = System.currentTimeMillis();
        Result result =  new Result("SQL",
                          new StringBuffer("Fetched:" + cnt + " rows in: " + (t2 - t1)
                                           + "ms <p>" + sb.toString()));
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return result;
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
    public Result makeQueryForm(Request request) throws Exception {
        List         where = assembleWhereClause(request);
        StringBuffer sb    = new StringBuffer();
        StringBuffer headerBuffer = new StringBuffer();
        headerBuffer.append("<h2>Search Form</h2>");
        headerBuffer.append("<table cellpadding=\"5\">");
        sb.append(HtmlUtil.form(href("/query")," name=\"query\""));

        TypeHandler typeHandler = getTypeHandler(request);

        String what = (String) request.get(ARG_WHAT);
            List whatList = Misc.toList(new Object[]{
                new TwoFacedObject("Entries",WHAT_QUERY),
                new TwoFacedObject("Data Types",WHAT_TYPE),
                new TwoFacedObject("Groups",WHAT_GROUP),
                new TwoFacedObject("Tags",WHAT_TAG),
                new TwoFacedObject("Radar products",WHAT_PRODUCT),
                new TwoFacedObject("Radar Stations",WHAT_STATION)
            }); 

        if (what == null) {
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Search For:"),
                                          HtmlUtil.select(ARG_WHAT,whatList)));

        } else {
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Search For:"),
                                          TwoFacedObject.findLabel(what,whatList)));
            sb.append(HtmlUtil.hidden(ARG_WHAT,what));
        }


        typeHandler.addToForm(sb, headerBuffer,request, where);
        String output = (String) request.get(ARG_OUTPUT);
        if (output == null) {
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Output Type:"),
                                          HtmlUtil.select(ARG_OUTPUT,
                                              Misc.newList(OUTPUT_HTML,
                                                  OUTPUT_XML, OUTPUT_RSS,OUTPUT_CSV))));
        } else {
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Output Type:"),
                                          output));
            sb.append(HtmlUtil.hidden(output, ARG_OUTPUT));
        }

        sb.append(HtmlUtil.tableEntry("", HtmlUtil.submit("Search")));
        sb.append("<table>");
        sb.append("</form>");
        headerBuffer.append(sb.toString());

        Result result =new Result("Search Form", headerBuffer);
        result.putProperty(PROP_NAVSUBLINKS, getSearchFormLinks(request));
        return result;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected List getAdminLinks(Request request) {
        List links = new ArrayList();
        String extra  = " style=\" font-family: Arial, Helvetica, sans-serif;  font-weight: bold; color:#ffffff;\" class=\"navtitle\"";
        links.add(href("/admin/db", "Database", extra));
        links.add(href("/sql", "SQL", extra));
        return links;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected List getSearchFormLinks(Request request) {
        List links = new ArrayList();
        String extra  = " style=\" font-family: Arial, Helvetica, sans-serif;  font-weight: bold; color:#ffffff;\" class=\"navtitle\"";
        links.add(href("/searchform?what="+WHAT_QUERY, "Entries", extra));
        links.add(href("/searchform?what="+WHAT_TYPE, "Data Types", extra));
        links.add(href("/searchform?what="+WHAT_GROUP, "Groups", extra));
        links.add(href("/searchform?what="+WHAT_TAG, "Tags", extra));

        return links;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected List getNavLinks(Request request) {
        List links = new ArrayList();
        String extra  = " style=\" font-family: Arial, Helvetica, sans-serif;  font-weight: bold; color:#ffffff;\" class=\"navtitle\"";
        links.add(href("/showgroup", "Group List", extra));
        links.add(href("/searchform", "Search", extra));
        RequestContext context = request.getRequestContext();
        User user = context.getUser();
        if(user.getAdmin()) {
            links.add(href("/admin", "Admin", extra));
        }
        return links;
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
    public Result processList(Request request) throws Exception {
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


    
    public Result fetchFile(Request request) throws Exception {
        String fileId = (String) request.get(ARG_ID);
        if (fileId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        Entry entry =  getFile(fileId,request);
        if(entry==null) {
            throw new IllegalArgumentException("Could not find file");
        }
        String contents =  IOUtil.readContents(
                                               entry.getFile(), getClass());
        return new Result("", new StringBuffer(contents),IOUtil.getFileExtension(entry.getFile()));
    }


    protected Entry getFile(String fileId, Request request) throws Exception {
        String query = SqlUtil.makeSelect(COLUMNS_ENTRIES,
                                          Misc.newList(TABLE_ENTRIES),
                                          SqlUtil.eq(COL_ENTRIES_ID,
                                                     SqlUtil.quote(fileId)));
        ResultSet results = execute(query).getResultSet();
        if ( !results.next()) {
            return null;
        }
        TypeHandler typeHandler = getTypeHandler(results.getString(4));
        return filterEntry(request,typeHandler.getEntry(results));
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
    public Result showEntry(Request request) throws Exception {
        String fileId = (String) request.get(ARG_ID);
        if (fileId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        Entry entry = getFile(fileId, request);
        if(entry==null) {
            throw new IllegalArgumentException("Could not find file");
        }
        return getTypeHandler(entry.getType()).showEntry(entry, request);
    }


    public Entry filterEntry(Request request,Entry file) throws Exception {
        //TODO: Check for access
        return file;
    }


    public List<Entry> filterEntries(Request request,List<Entry> entries) throws Exception {
        List<Entry> filtered = new ArrayList();
        for(Entry entry: entries) {
            entry = filterEntry(request,entry);
            if(entry!=null) {
                filtered.add(entry);
            }
        }
        return filtered;
    }


    public Result fetchEntries(Request request) throws Exception {
        List<Entry> entries = new ArrayList();
        for (Enumeration keys = request.getParameters().keys();
                    keys.hasMoreElements(); ) {
            String id = (String) keys.nextElement();
            if(!Misc.equals(request.get(id), "true")) continue;
            if(!id.startsWith("file_")) continue;
            id = id.substring("file_".length());
            Entry entry = getFile(id,request);
            if(entry!=null) {
                entries.add(entry);
            }
        }
        entries = filterEntries(request,entries);
        String       output = getValue(request, ARG_OUTPUT, OUTPUT_CATALOG);
        if(output.equals(OUTPUT_CATALOG)) {
            return toCatalog(request, entries,"Directory Listing");
        } else if(output.equals(OUTPUT_ZIP)) {
            return toZip(request, entries);
        } else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }
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
    public Result showGroup(Request request) throws Exception {

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
                                      Misc.newList(TABLE_GROUPS),
                                      COL_GROUPS_PARENT + " IS NULL"));
            groups.addAll(getGroups(SqlUtil.readString(statement, 1)));
        } else {
            groups.add(theGroup);
        }


        String       output = getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        List         where  = typeHandler.assembleWhereClause(request);
        StringBuffer sb     = new StringBuffer();

        if (output.equals(OUTPUT_HTML)) {
            if (topLevel) {
                sb.append(HtmlUtil.bold("Top Level Groups") + "<ul>");
            }
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_CATALOG,
                                      XmlUtil.attrs(ATTR_NAME,
                                          "Query Results")));
        }



        String title = "Groups";
        for (Group group : groups) {
            if (topLevel) {
                sb.append(
                    "<li>"
                    + href(HtmlUtil.url(
                        "/showgroup", "group",
                        group.getFullName()), group.getFullName()) + "</a> "
                    + getGraphLink(request, group));
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
            breadcrumbs.add(group.getName() + " "+ getGroupLinks(request, group));

            title = "Group: "
                    + StringUtil.join("&nbsp;&gt;&nbsp;", titleList);

            sb.append(HtmlUtil.bold("Group: "
                                    + StringUtil.join("&nbsp;&gt;&nbsp;",
                                        breadcrumbs)));
            sb.append("<hr>");
            List<Group> subGroups = getGroups(
                                        SqlUtil.makeSelect(
                                            COL_GROUPS_ID, Misc.newList(TABLE_GROUPS),
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

            where.add(SqlUtil.eq(COL_ENTRIES_GROUP_ID,
                                 SqlUtil.quote(group.getId())));
            String query =
                SqlUtil.makeSelect(SqlUtil.comma(COL_ENTRIES_ID,
                    COL_ENTRIES_NAME, COL_ENTRIES_TYPE,
                    COL_ENTRIES_FILE), typeHandler.getTablesForQuery(request),
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
                        sb.append("\n");
                        sb.append(HtmlUtil.bold("Entries:"));
                        sb.append("<br>");
                        sb.append(HtmlUtil.form("/getfiles","getfiles"));
                        sb.append(HtmlUtil.submit("Get Selected Entries"));
                        List outputList = Misc.toList(new Object[]{
                            new TwoFacedObject("As catalog",OUTPUT_XML),
                            new TwoFacedObject("As zip file",OUTPUT_ZIP)});
                        sb.append(HtmlUtil.select(ARG_OUTPUT,outputList));
                        sb.append("<p>\n");
                        sb.append("<ul>\n");
                    }

                    sb.append("<li>" + HtmlUtil.checkbox("file_" + id,"true") +" " 
                              + href(HtmlUtil.url("/showentry", ARG_ID, id),
                                     name));
                    sb.append("\n");
                }
            }
            if (cnt > 0) {
                sb.append("</ul>");
                sb.append("</form>");
            }
        }
        if (topLevel) {
            sb.append("</ul>");
        }
        return new Result(title, sb, getMimeTypeFromOutput(output));

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
    public Result getGraphApplet(Request request) throws Exception {
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
        return new Result("Graph View", html.getBytes(),Result.TYPE_HTML);
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
        String nodeType = NODETYPE_ENTRY;
        if (fileType.equals(TypeHandler.TYPE_LEVEL3RADAR)) {
            nodeType = TypeHandler.TYPE_LEVEL3RADAR;
        }
        //        nodeType = TypeHandler.TYPE_LEVEL3RADAR;
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
    public Result getGraph(Request request) throws Exception {

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
                SqlUtil.makeSelect(SqlUtil.comma(COL_ENTRIES_ID, 
                    COL_ENTRIES_NAME, COL_ENTRIES_TYPE, COL_ENTRIES_GROUP_ID,
                    COL_ENTRIES_FILE), Misc.newList(TABLE_ENTRIES,TABLE_TAGS),
                                   SqlUtil.eq(COL_TAGS_FILE_ID, COL_ENTRIES_ID) +" AND " +
                                   SqlUtil.eq(COL_TAGS_NAME, SqlUtil.quote(id))); 

            //            System.err.println ("tag query:" + query);
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, TYPE_TAG, ATTR_ID,
                                                id, ATTR_TITLE, id)));
            SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
            ResultSet results;
            int cnt = 0;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    sb.append(getFileNodeXml(results));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE, "taggedby",
                                                        ATTR_FROM, id,
                                                        ATTR_TO, results.getString(1))));
                    cnt++;
                    if(cnt>50) break;
                }
            }
            String xml = StringUtil.replace(graphXmlTemplate, "%content%",
                                            sb.toString());
            return new Result("", new StringBuffer(xml),
                                  getMimeTypeFromOutput(OUTPUT_GRAPH));
        }

        if ( !type.equals(TYPE_GROUP)) {
            String entriesQuery =
                SqlUtil.makeSelect(SqlUtil.comma(COL_ENTRIES_ID,
                    COL_ENTRIES_NAME, COL_ENTRIES_TYPE, COL_ENTRIES_GROUP_ID,
                    COL_ENTRIES_FILE), Misc.newList(TABLE_ENTRIES),
                                     SqlUtil.eq(COL_ENTRIES_ID,
                                         SqlUtil.quote(id)));


            ResultSet results = execute(entriesQuery).getResultSet();
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

            String tagQuery =
                SqlUtil.makeSelect(COL_TAGS_NAME, Misc.newList(TABLE_TAGS),
                                   SqlUtil.eq(COL_TAGS_FILE_ID, SqlUtil.quote(id)));
            String[]tags = SqlUtil.readString(execute(tagQuery),1);
            for(int i=0;i<tags.length;i++) {
                sb.append(XmlUtil.tag(TAG_NODE,
                                      XmlUtil.attrs(ATTR_TYPE, TYPE_TAG, ATTR_ID,
                                                    tags[i], ATTR_TITLE, tags[i])));
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "taggedby",
                                                    ATTR_FROM, tags[i],
                                                    ATTR_TO, id)));
            }




            String xml = StringUtil.replace(graphXmlTemplate, "%content%",
                                            sb.toString());
            return new Result("", new StringBuffer(xml),
                                  getMimeTypeFromOutput(OUTPUT_GRAPH));
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
                                    Misc.newList(TABLE_GROUPS),
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

        String query = SqlUtil.makeSelect(SqlUtil.comma(COL_ENTRIES_ID,
                           COL_ENTRIES_NAME, COL_ENTRIES_TYPE,
                           COL_ENTRIES_GROUP_ID, COL_ENTRIES_FILE), Misc.newList(TABLE_ENTRIES),
                               SqlUtil.eq(COL_ENTRIES_GROUP_ID,
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
        return new Result("", new StringBuffer(xml),
                              getMimeTypeFromOutput(OUTPUT_GRAPH));
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
    protected Result listGroups(Request request) throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        String query =
            SqlUtil.makeSelect(SqlUtil.distinct(COL_ENTRIES_GROUP_ID),
                               typeHandler.getTablesForQuery(request),
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

        return new Result("", sb, getMimeTypeFromOutput(output));
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
            SqlUtil.makeSelect(SqlUtil.distinct(COL_ENTRIES_TYPE),
                               typeHandler.getTablesForQuery(request),
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
    protected Result listTypes(Request request) throws Exception {
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
                sb.append(href(HtmlUtil.url("/searchform",ARG_TYPE,theTypeHandler.getType()),theTypeHandler.getType()));
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
        return new Result("", sb, getMimeTypeFromOutput(output));
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
    protected Result listTags(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h2>Tags</h2>");
            sb.append("<ul>");
        } else         if (output.equals(OUTPUT_CLOUD)) {
            sb.append("<h2>Tag Cloud</h2>");        
        }   else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_TAGS));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        List tables = Misc.newList(TABLE_TAGS);
        if(where.size()>0) {
            where.add(0,SqlUtil.eq(COL_TAGS_FILE_ID, COL_ENTRIES_ID));
            tables.addAll(typeHandler.getTablesForQuery(request));
        }

        String query =
            SqlUtil.makeSelect(SqlUtil.comma(COL_TAGS_NAME, SqlUtil.count("*")),
                               tables,
                               SqlUtil.makeAnd(where),"  group by " + COL_TAGS_NAME);

        List<String>  names = new ArrayList<String>();
        List<Integer>  counts = new ArrayList<Integer>();
        ResultSet        results;
        SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
        int max =-1;
        int min = -1;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                String tag = results.getString(1);
                int count = results.getInt(2);
                if(max<0 || count>max) max  = count;
                if(min<0 || count<min) min  = count;
                names.add(tag);
                counts.add(new Integer(count));
            }
        }
        int diff = max - min;
        double distribution = diff / 5.0;

        for(int i=0;i<names.size();i++) {
            String tag = names.get(i);
            int count = counts.get(i).intValue();
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(tag);
                sb.append("(" + count +")");
                if(isAppletEnabled(request)) {
                    sb.append(href(HtmlUtil.url("/graphview", "id", tag, "type", TYPE_TAG), 
                                   HtmlUtil.img(urlBase + "/tree.gif", "alt=\"Show tag in graph\" title=\"Show file in graph\" ")));
                }
            } else         if (output.equals(OUTPUT_CLOUD)) {

                double percent = count/distribution;
                int bin = (int)(percent*5);
                String css = "font-size:" +(12+bin*2);
                sb.append("<span style=\"" + css +"\">");
                String extra = XmlUtil.attrs("alt", "Count:" + count,"title", "Count:" + count);
                sb.append(href(HtmlUtil.url("/graphview", "id", tag, "type", TYPE_TAG), 
                               tag,extra));
                sb.append("</span>");
                sb.append(" &nbsp; ");
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_TAG,
                                      XmlUtil.attrs(ATTR_NAME,
                                                    tag)));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(tag);
                sb.append("\n");
            }
        }

        String pageTitle = ""; 
        if (output.equals(OUTPUT_HTML)) {
            pageTitle = "Tags"; 
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            pageTitle = "Tag Cloud"; 
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_TAGS));
        }
        return new Result(pageTitle, sb, getMimeTypeFromOutput(output));
    }

    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    protected String getMimeTypeFromOutput(String output) {
        if (output.equals(OUTPUT_CSV)) {
            return getMimeType(".csv");
        } else if (output.equals(OUTPUT_XML)) {
            return getMimeType(".xml");
        } else if (output.equals(OUTPUT_GRAPH)) {
            return getMimeType(".xml");
        } else if (output.equals(OUTPUT_RSS)) {
            return getMimeType(".rss");
        } else if (output.equals(OUTPUT_HTML)) {
            return getMimeType(".html");
        } else if (output.equals(OUTPUT_CLOUD)) {
            return getMimeType(".html");
        } else if (output.equals(OUTPUT_ZIP)) {
            return getMimeType(".zip");
        } else {
            return getMimeType(".txt");
        }
    }


    protected String getMimeType(String suffix) {
        String type = (String)mimeTypes.get(suffix);
        if(type==null) {
            if(suffix.startsWith(".")) suffix = suffix.substring(1);
            type = (String)mimeTypes.get(suffix);
        }
        if(type==null) {
            type = "unknown";
        }
        return type;
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
                COL_GROUPS_DESCRIPTION), Misc.newList(TABLE_GROUPS)));

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
                                          Misc.newList(TABLE_USERS),
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
                                          Misc.newList(TABLE_GROUPS),
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

        String    query     = SqlUtil.makeSelect(COLUMNS_GROUPS, Misc.newList(TABLE_GROUPS),where);

        Statement statement = execute(query);
        ResultSet results   = statement.getResultSet();
        if (results.next()) {
            group = new Group(results.getString(1),parent,
                              results.getString(3), results.getString(4));
        } else {
            String id;
            //            "select count(*) from groups where group.parent is NULL"
            //            int cnt = SqlUtil.readInt(execute(select));
            //            while(true) {
            //            }
            if(parent == null) {
                //                id = ""+(keyCnt++);
                id  =   getGUID();
            } else {
                //                id = parent.getId()+"_"+(keyCnt++);
                id = parent.getId()+"_"+getGUID();
            }

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
    protected List<Entry> getEntries(Request request) throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        String query =
            SqlUtil.makeSelect(COLUMNS_ENTRIES, typeHandler.getTablesForQuery(request),
                                       SqlUtil.makeAnd(where),
                                       "order by " + COL_ENTRIES_FROMDATE);
        //        System.err.println("Query:"+ query);
        Statement        statement = execute(query, getMax(request));
        List<Entry>   entries = new ArrayList<Entry>();
        ResultSet        results;
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                //id,name,desc,type,group,user,file,createdata,fromdate,todate
                TypeHandler localTypeHandler = getTypeHandler(results.getString(4));
                entries.add(localTypeHandler.getEntry(results));
            }
        }
        return entries;
    }





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

    protected String getGroupLinks (Request request, Group group) throws Exception {
        String search = href(HtmlUtil.url(
                                 "/searchform", "group", 
                                 java.net.URLEncoder.encode(group.getId()+"%", "UTF-8")), HtmlUtil.img(
                                                                urlBase + "/Search16.gif"));

        return search+" " + getGraphLink(request, group);
    }

    protected String getGraphLink (Request request, Group group) {
        if(!isAppletEnabled(request)) return "";
        return href(HtmlUtil.url(
                                 "/graphview", "id", group.getFullName(),
                                 "type", "group"), HtmlUtil.img(
                                                                urlBase + "/tree.gif"));
    }

    protected String getGraphLink (Request request, Entry entry) {
        if(!isAppletEnabled(request)) return "";
        return href(HtmlUtil.url("/graphview", "id", entry.getId(), "type", entry.getType()), 
            HtmlUtil.img(urlBase + "/tree.gif", "alt=\"Show file in graph\" title=\"Show file in graph\" "));
    }

    protected String getFileFetchLink(Entry entry) {
        if(getProperty(PROP_HTML_DOWNLOADENTRIESASFILES,false)) {
            return HtmlUtil.href("file://" + entry.getFile(),
                                HtmlUtil.img(href("/Fetch.gif"),"alt=\"Download file\"  title=\"Download file\"  "));
        } else {
            return href(HtmlUtil.url("/fetch/" + entry.getName(),"id",entry.getId()),HtmlUtil.img(href("/Fetch.gif"),"alt=\"Download file\"  title=\"Download file\"  "));
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
    public Result processQuery(Request request) throws Exception {
        String what = request.get(ARG_WHAT,WHAT_QUERY);

        if(!what.equals(WHAT_QUERY)) {
            Result result =   processList(request);
            result.putProperty(PROP_NAVSUBLINKS, getSearchFormLinks(request));
            return result;
        }

        timelineAppletTemplate = IOUtil.readContents(
            "/ucar/unidata/repository/timelineapplet.html", getClass());
        List           times     = new ArrayList();
        List           labels    = new ArrayList();
        List<Entry> entries = getEntries(request);


        StringBuffer   sb        = new StringBuffer();
        String         output    = getValue(request, ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_CATALOG)) {
            return toCatalog(request, entries, "Query Results");
        }

        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h2>Query Results</h2>");
            if(entries.size()==0) {
                sb.append("<b>Nothing Found</b><p>");
            }
            sb.append("<table>");
        } else if (output.equals(OUTPUT_RSS)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_RSS_RSS,
                                      XmlUtil.attrs(ATTR_RSS_VERSION,
                                          "2.0")));
            sb.append(XmlUtil.openTag(TAG_RSS_CHANNEL));
            sb.append(XmlUtil.tag(TAG_RSS_TITLE,"","Repository Query"));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }


        StringBufferCollection sbc = new StringBufferCollection();
        for (Entry entry : entries) {
            times.add(SqlUtil.format(new Date(entry.getStartDate())));
            labels.add(entry.getName());
            StringBuffer ssb = sbc.getBuffer(entry.getType());
            if (output.equals(OUTPUT_HTML)) {
                String links = HtmlUtil.checkbox("file_" + entry.getId(),"true") +" " +
                getFileFetchLink(entry) +" " +getGraphLink(request,entry);
                ssb.append(HtmlUtil.row(links+" " + href(HtmlUtil.url("/showentry", ARG_ID, entry.getId()),
                                                         entry.getName()), 
                                        "" + new Date(entry.getStartDate())));
            } else if (output.equals(OUTPUT_RSS)) {
                sb.append(XmlUtil.openTag(TAG_RSS_ITEM));
                sb.append(XmlUtil.tag(TAG_RSS_PUBDATE,"",
                                      "" + new Date(entry.getStartDate())));
                sb.append(XmlUtil.tag(TAG_RSS_TITLE,"",
                                      entry.getName()));
                sb.append(XmlUtil.tag(TAG_RSS_DESCRIPTION,"",
                                      entry.getDescription()));
                sb.append(XmlUtil.closeTag(TAG_RSS_ITEM));
                //      <link>http://earthquake.usgs.gov/eqcenter/recenteqsww/Quakes/us2007kmae.php</link>
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(entry.getId(),
                                        entry.getFile()));
            }
        }


        if (output.equals(OUTPUT_HTML)) {
            if(entries.size()>0) {
                String tmp = StringUtil.replace(timelineAppletTemplate, "%times%",
                                                StringUtil.join(",", times));
                tmp = StringUtil.replace(tmp, "%labels%",
                                         StringUtil.join(",", labels));
                if(isAppletEnabled(request)) {
                    sb.append(tmp);
                }
            }
            sb.append(HtmlUtil.form("/getfiles","getfiles"));
            sb.append(HtmlUtil.submit("Get Entries"));
            sb.append("<br>");
        }
        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb  = sbc.getBuffer(type);
            if (output.equals(OUTPUT_HTML)) {
                sb.append(HtmlUtil.row(HtmlUtil.bold("Type:" + type)));
                sb.append(ssb);

            }
        }

        if (output.equals(OUTPUT_HTML)) {
            sb.append("</form>");
            sb.append("</table>");
        } else if (output.equals(OUTPUT_RSS)) {
            sb.append(XmlUtil.closeTag(TAG_RSS_CHANNEL));            
            sb.append(XmlUtil.closeTag(TAG_RSS_RSS));
        }
        Result result = new Result("Query Results", sb, getMimeTypeFromOutput(output));
        result.putProperty(PROP_NAVSUBLINKS, getSearchFormLinks(request));
        return result;
    }



    protected Result toZip(Request request, List<Entry> entries) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        Hashtable seen =new Hashtable();
        for (Entry entry: entries) {
            String path = entry.getFile();
            String name  = IOUtil.getFileTail(path);
            int cnt = 1;
            while(seen.get(name)!=null) {
                name = (cnt++)+"_"+name;
            }
            seen.put(name,name);
            zos.putNextEntry(new ZipEntry(name));
            byte[] bytes = IOUtil.readBytes(IOUtil.getInputStream(path, getClass()));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
        zos.close();
        bos.close();
        return new  Result("",bos.toByteArray(),getMimeTypeFromOutput(OUTPUT_ZIP));
    }


    protected Result toCatalog(Request request, List<Entry> entries, String title) throws Exception {
        StringBuffer   sb        = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_CATALOG,
                                  XmlUtil.attrs(ATTR_NAME,title)));
        StringBufferCollection sbc = new StringBufferCollection();
        for (Entry entry : entries) {
            StringBuffer ssb = sbc.getBuffer(entry.getType());
            ssb.append(
                       XmlUtil.tag(
                                   TAG_DATASET,
                                   XmlUtil.attrs(
                                                 ATTR_NAME,
                                                 "" + new Date(entry.getStartDate()),
                                                 ATTR_URLPATH, entry.getFile())));
        }

        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb  = sbc.getBuffer(type);
            sb.append(XmlUtil.openTag(TAG_DATASET,
                                      XmlUtil.attrs(ATTR_NAME, type)));
            sb.append(ssb);
            sb.append(XmlUtil.closeTag(TAG_DATASET));
        }
        sb.append(XmlUtil.closeTag(TAG_CATALOG));
        Result result = new Result("Query Results", sb, getMimeTypeFromOutput(OUTPUT_CATALOG));
        return result;
    }



    protected void setStatement(Entry entry, PreparedStatement statement) throws Exception {
        int    col = 1;
        //id,name,desc,type,group,user,file,createdata,fromdate,todate
        statement.setString(col++, entry.getId());
        statement.setString(col++, entry.getName());
        statement.setString(col++, entry.getDescription());
        statement.setString(col++, entry.getType());
        statement.setString(col++, entry.getGroupId());
        statement.setString(col++, entry.getUser().getId());
        statement.setString(col++, entry.getFile().toString());
        statement.setTimestamp(
                                 col++, new java.sql.Timestamp(currentTime()));
        statement.setTimestamp(
                                 col++, new java.sql.Timestamp(entry.getStartDate()));
        statement.setTimestamp(
                                 col++, new java.sql.Timestamp(entry.getStartDate()));
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
        List<Level3RadarEntry> files   = harvester.collectLevel3radarFiles(rootDir, "IDD");
        //        files.addAll(collectLevel3radarFiles(rootDir, "LDM/LDM2"));
        System.err.println("Inserting:" + files.size() + " radar files");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement entryInsert =
            connection.prepareStatement(INSERT_ENTRIES);
        PreparedStatement radarInsert =
            connection.prepareStatement(INSERT_LEVEL3RADAR);

        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (Level3RadarEntry radarEntry : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }

            String id  = getGUID();
            radarEntry.setId(id);
            setStatement(radarEntry, entryInsert);
            entryInsert.addBatch();
            int col = 1;
            radarInsert.setString(col++, radarEntry.getId());
            radarInsert.setString(col++, radarEntry.getStation());
            radarInsert.setString(col++, radarEntry.getProduct());
            radarInsert.addBatch();
            batchCnt++;
            if (batchCnt > 100) {
                entryInsert.executeBatch();
                radarInsert.executeBatch();
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryInsert.executeBatch();
            radarInsert.executeBatch();
        }
        connection.commit();
        connection.setAutoCommit(true);
        long   t2      = System.currentTimeMillis();
        double seconds = (t2 - t1) / 1000.0;
        System.err.println("cnt:" + cnt + " time:" + seconds + " rate:"
                           + (cnt / seconds));

    }



    /**
     * _more_
     *
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public void loadLevel2RadarFiles() throws Exception {
        File            rootDir = new File("/data/ldm/gempak/nexrad/craft");
        List<Level2RadarEntry> files   = harvester.collectLevel2radarFiles(rootDir, "IDD");
        //        files.addAll(collectLevel2radarFiles(rootDir, "LDM/LDM2"));
        System.err.println("Inserting:" + files.size() + " radar files");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement entryInsert =
            connection.prepareStatement(INSERT_ENTRIES);
        PreparedStatement radarInsert =
            connection.prepareStatement(INSERT_LEVEL2RADAR);

        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (Level2RadarEntry entry : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }

            String id  = getGUID();
            entry.setId(id);
            setStatement(entry, entryInsert);
            entryInsert.addBatch();
            int col = 1;
            radarInsert.setString(col++, entry.getId());
            radarInsert.setString(col++, entry.getStation());
            radarInsert.addBatch();
            batchCnt++;
            if (batchCnt > 100) {
                entryInsert.executeBatch();
                radarInsert.executeBatch();
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryInsert.executeBatch();
            radarInsert.executeBatch();
        }
        connection.commit();
        connection.setAutoCommit(true);
        long   t2      = System.currentTimeMillis();
        double seconds = (t2 - t1) / 1000.0;
        System.err.println("cnt:" + cnt + " time:" + seconds + " rate:"
                           + (cnt / seconds));

    }


    /**
     * _more_
     *
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public void loadModelFiles() throws Exception {
        File            rootDir = new File("/data/ldm/gempak/model");
        List<ModelEntry> files   = harvester.collectModelFiles(rootDir, "IDD");
        System.err.println("Inserting:" + files.size() + " model files");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement entryInsert =
            connection.prepareStatement(INSERT_ENTRIES);
        PreparedStatement modelInsert =
            connection.prepareStatement(INSERT_MODEL);

        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (ModelEntry entry : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }

            String id  = getGUID();
            entry.setId(id);
            setStatement(entry, entryInsert);
            entryInsert.addBatch();
            int col = 1;
            modelInsert.setString(col++, entry.getId());
            modelInsert.setString(col++, entry.getModelGroup());
            modelInsert.setString(col++, entry.getModelRun());
            modelInsert.addBatch();
            batchCnt++;
            if (batchCnt > 100) {
                entryInsert.executeBatch();
                modelInsert.executeBatch();
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryInsert.executeBatch();
            modelInsert.executeBatch();
        }
        connection.commit();
        connection.setAutoCommit(true);
        long   t2      = System.currentTimeMillis();
        double seconds = (t2 - t1) / 1000.0;
        System.err.println("cnt:" + cnt + " time:" + seconds + " rate:"
                           + (cnt / seconds));

    }

    
    /**
     * _more_
     *
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public void loadSatelliteFiles() throws Exception {
        File            rootDir = new File("/data/ldm/gempak/images/sat");
        List<SatelliteEntry> files   = harvester.collectSatelliteFiles(rootDir, "IDD");
        //        files.addAll(collectLevel3radarFiles(rootDir, "LDM/LDM2"));
        System.err.println("Inserting:" + files.size() + " satellite files");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement entryInsert =
            connection.prepareStatement(INSERT_ENTRIES);
        PreparedStatement satelliteInsert =
            connection.prepareStatement(INSERT_SATELLITE);

        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (SatelliteEntry info : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }

            String id  = getGUID();
            info.setId(id);
            setStatement(info, entryInsert);
            entryInsert.addBatch();
            int col = 1;
            satelliteInsert.setString(col++, info.getId());
            satelliteInsert.setString(col++, info.getPlatform());
            satelliteInsert.setString(col++, info.getResolution());
            satelliteInsert.setString(col++, info.getProduct());
            satelliteInsert.addBatch();
            batchCnt++;
            if (batchCnt > 100) {
                entryInsert.executeBatch();
                satelliteInsert.executeBatch();
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryInsert.executeBatch();
            satelliteInsert.executeBatch();
        }
        connection.commit();
        connection.setAutoCommit(true);
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
        //        rootDir = new File("/harpo/jeffmc/src/idv/trunk/ucar/unidata");
        List<Entry> files = harvester.collectFiles(rootDir,"Files");
        System.err.println("Inserting:" + files.size() + " test files");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement entryInsert =
            connection.prepareStatement(INSERT_ENTRIES);
        PreparedStatement tagsInsert =
            connection.prepareStatement(INSERT_TAGS);
        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (Entry entry : files) {
            if ((++cnt) % 10000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }
            setStatement(entry, entryInsert);
            entryInsert.addBatch();
            batchCnt++;
            List<String> tags = entry.getTags();
            if(tags !=null) {
                for(String tag: tags) {
                    tagsInsert.setString(1, tag);
                    tagsInsert.setString(2, entry.getId());
                    batchCnt++;
                    tagsInsert.addBatch();
                }
            }

            if (batchCnt > 100) {
                entryInsert.executeBatch();
                tagsInsert.executeBatch();
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryInsert.executeBatch();
            tagsInsert.executeBatch();
        }
        connection.setAutoCommit(true);
        //        connection.commit();
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
        try {
            statement.execute(sql);
        } catch(Exception exc) {
            System.err.println("ERROR:" + sql);
            throw exc;
        }
        long t2 = System.currentTimeMillis();
        //        System.err.println("query:" + sql);
        if(t2-t1>300) {
            System.err.println("query:" + sql);
            System.err.println("time:" + (t2-t1));}
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


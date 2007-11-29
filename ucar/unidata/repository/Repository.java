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


import ucar.unidata.data.SqlUtils;
import ucar.unidata.util.TextResult;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

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


/**
 * Class SqlUtils _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Repository implements TableDefinitions {



    /** _more_          */
    public static final String ATTR_ID = "id";

    /** _more_          */
    public static final String ATTR_NAME = "name";


    public static final String TAG_GROUPS = "groups";
    public static final String TAG_GROUP = "group";

    /** _more_          */
    public static final String ARG_TYPE = "type";

    public static final String ARG_ID = "id";

    /** _more_          */
    public static final String ARG_GROUP = "group";

    public static final String ARG_GROUPID = "groupid";

    /** _more_          */
    public static final String ARG_TODATE = "todate";

    /** _more_          */
    public static final String ARG_FROMDATE = "fromdate";

    /** _more_          */
    public static final String ARG_PRODUCT = "product";

    /** _more_          */
    public static final String ARG_STATION = "station";



    /** _more_          */
    long baseTime = System.currentTimeMillis();

    /** _more_          */
    int keyCnt = 0;


    /** _more_ */
    private Connection connection;


    /**
     * _more_
     *
     *
     * @param driver _more_
     * @param connectionURL _more_
     * @throws Exception _more_
     */
    public Repository(String driver, String connectionURL, String userName, String password) throws Exception {
        Misc.findClass(driver);
        if(userName!=null) {
            connection = DriverManager.getConnection(connectionURL, userName, password);
        } else {
            connection = DriverManager.getConnection(connectionURL);
        }
        initTable();
        initTypeHandlers();
    }

    protected void initTable() throws Exception {
        boolean ok = true;
        try {
            execute ("select * from dummy");
        } catch(Exception dummy) {
            ok = false;
        }
        if(ok) return;
        System.err.println("making db");
        String sql =
            IOUtil.readContents("/ucar/unidata/repository/makedb.sql",
                                getClass());
        Statement statement = connection.createStatement();
        SqlUtils.loadSql(sql, statement);
        //        loadLevel3RadarFiles();
        loadTestFiles();
    }


    protected void initTypeHandlers() {
        addTypeHandler(TypeHandler.TYPE_ANY, new TypeHandler(this, TypeHandler.TYPE_ANY));
        addTypeHandler(TypeHandler.TYPE_LEVEL3RADAR, new TypeHandler(this,TypeHandler.TYPE_LEVEL3RADAR));
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
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult processSql(Hashtable args) throws Exception {
        long      t1        = System.currentTimeMillis();
        String    query     = (String) args.get("query");
        Statement statement = execute(query);
        SqlUtils.Iterator iter = SqlUtils.getIterator(statement);
        int               cnt  = 0;
        StringBuffer      sb   = new StringBuffer();
        sb.append("<form action=\"/sql\">\n");
        sb.append("<input  name=\"query\" size=\"60\" value=\"" + query
                  + "\"/>");
        sb.append("<input  type=\"submit\" value=\"Query\" />");
        sb.append("</form>\n");
        sb.append("<table>");


        ResultSet results;
        while ((results = iter.next()) != null) {
            ResultSetMetaData rsmd = results.getMetaData();
            while (results.next()) {
                int colcnt = 0;
                if (cnt == 0) {
                    sb.append("<table><tr>");
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        sb.append("<td><b>" + rsmd.getColumnLabel(i + 1)
                                  + "</b></td>");
                    }
                    sb.append("</tr>");
                }
                sb.append("<tr>");
                while (colcnt < rsmd.getColumnCount()) {
                    sb.append("<td>" + results.getString(++colcnt) + "</td>");
                }
                sb.append("</tr>\n");
                if (cnt++ > 100000) {
                    sb.append("<tr><td>...</td></tr>");
                    break;
                }
            }
            if (cnt > 100000) {
                break;
            }
        }
        sb.append("</table>");
        long t2 = System.currentTimeMillis();
        return new TextResult("SQL", 
                              "Fetched:" + cnt + " rows in: " + (t2 - t1)
                                + "ms <p>" + sb.toString());
    }




    private Hashtable typeHandlers = new Hashtable();

    protected void addTypeHandler(String typeName, TypeHandler typeHandler) {
        typeHandlers.put(typeName, typeHandler);
    }

    protected TypeHandler getTypeHandler(Hashtable args) throws Exception {
        String type = (String) args.get(ARG_TYPE);
        if(type == null) type = TypeHandler.TYPE_ANY;
        return getTypeHandler(type);
    }

    protected TypeHandler getTypeHandler(String type) throws Exception {
        TypeHandler typeHandler = (TypeHandler)typeHandlers.get(type);
        if(typeHandler == null) {
            typeHandler = new TypeHandler(this,type);
            addTypeHandler(type, typeHandler);
        }
        return typeHandler;
    }

    public List<Group> getGroups(String sql) throws Exception {
        Statement statement = execute(sql);
        return  getGroups(SqlUtils.readString(statement, 1));
    }

    public List<Group> getGroups(String[]groups) throws Exception {
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
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult makeQueryForm(Hashtable args) throws Exception {
        Statement statement;
        List    where     = assembleWhereClause(args);
        StringBuffer sb       = new StringBuffer();
        sb.append("<h2> Search Form</h2>");
        sb.append("<table cellpadding=\"5\">");
        sb.append("<form action=\"/query\">\n");

        TypeHandler typeHandler = getTypeHandler(args);
        sb.append(HtmlUtil.makeHidden(typeHandler.getType(), ARG_TYPE));
        typeHandler.addToForm(sb, args, where);
        String output = (String) args.get("output");
        if (output == null) {
            sb.append(
                "<tr><td align=\"right\"><b>Output Type: </b></td><td><select name=\"output\"><option>html</option><option>xml</option></select></td></tr>");
        } else {
            sb.append(HtmlUtil.makeHidden(output, "output"));
        }

        sb.append(
            "<tr><td><input  type=\"submit\" value=\"Search\" /></td></tr>");
        sb.append("<table>");
        sb.append("</form>");
        typeHandler.makeLinks(sb);

        sb.append("<form action=\"/sql\">\n");
        sb.append("<input  name=\"query\" size=\"60\"/>");
        sb.append("<input  type=\"submit\" value=\"Query\" />");
        sb.append("</form>\n");

        return new TextResult("Search Form", sb);
    }


    /**
     * _more_
     *
     * @param args _more_
     * @param column _more_
     * @param tag _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult processRadarList(Hashtable args, String column,
                                            String tag)
            throws Exception {
        List where =getTypeHandler(TypeHandler.TYPE_LEVEL3RADAR).assembleWhereClause(args);
        String query = SqlUtils.makeSelect(SqlUtils.distinct(column),
                                           TABLE_LEVEL3RADAR,
                                           SqlUtils.makeAnd(where));
        Statement    statement = execute(query);
        String[]     stations  = SqlUtils.readString(statement, 1);
        StringBuffer sb        = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(tag + "s"));
        for (int i = 0; i < stations.length; i++) {
            sb.append(XmlUtil.tag(tag,
                                  XmlUtil.attrs(ATTR_ID, stations[i],
                                      ATTR_NAME,
                                      getLongName(stations[i]))));
        }
        sb.append(XmlUtil.closeTag(tag + "s"));
        return new TextResult("", sb, TextResult.TYPE_XML);
    }





    protected TextResult showFile(Hashtable args)
        throws Exception {
        String fileId = (String) args.get(ARG_ID);
        if(fileId == null) throw new IllegalArgumentException ("No " + ARG_ID +" given");
        StringBuffer sb        = new StringBuffer();
        String query =
            SqlUtils.makeSelect(SqlUtils.comma(COL_FILES_ID,COL_FILES_TYPE, COL_FILES_FILE),
                                TABLE_FILES,
                                SqlUtils.eq(COL_FILES_ID, SqlUtils.quote(fileId)));
        ResultSet results = execute(query).getResultSet();
        if(!results.next()) {
            throw new IllegalArgumentException("Given file id:" + fileId +" is not in database");
        }
        String id = results.getString(1);
        String type = results.getString(2);
        String file = results.getString(3);
        sb.append("File:" + file +"<br>");
        sb.append("Type:" + type +"<br>");
        return new TextResult("File", sb);
    }

    protected TextResult showGroup(Hashtable args)
        throws Exception {
        Group theGroup = null;
        String groupName = (String) args.get(ARG_GROUP);
        if(groupName!=null) {
            args.remove(ARG_GROUP);
            theGroup = findGroup(groupName);
        }

        List<Group> groups= new ArrayList<Group>();
        TypeHandler typeHandler = getTypeHandler(args);
        boolean topLevel = false;
        if(theGroup == null) {
            topLevel = true;
            Statement statement =  execute(SqlUtils.makeSelect(COL_GROUPS_ID,TABLE_GROUPS,COL_GROUPS_PARENT+ " IS NULL"));
            groups.addAll(getGroups(SqlUtils.readString(statement,1)));
        } else {
            groups.add(theGroup);
        }


        List where = typeHandler.assembleWhereClause(args);
        StringBuffer sb        = new StringBuffer();
        if(topLevel) sb.append("<b>Top Level Groups</b><ul>");

        String title = "Groups";

        for(Group group: groups) {
            if(topLevel) {
                sb.append("<li><a href=\"/showgroup?group=" + group.getFullName() +"\">" +
                          group.getFullName()+"</a>");
                continue;
            }
            List  breadcrumbs = new ArrayList();
            List  titleList = new ArrayList();
            Group parent  = group.getParent();
            while(parent!=null) {
                titleList.add(0,parent.getName());
                breadcrumbs.add(0,HtmlUtil.href("/showgroup?group="+ parent.getFullName(),parent.getName()));
                parent = parent.getParent();
            }
            breadcrumbs.add(0,HtmlUtil.href("/showgroup", "Top"));
            titleList.add(group.getName());
            breadcrumbs.add(group.getName());
            title = "Group: " + StringUtil.join("&nbsp;&gt;&nbsp;", titleList);
            sb.append("<b>Group: " + StringUtil.join("&nbsp;&gt;&nbsp;", breadcrumbs)+"</b><hr>");

            List<Group> subGroups = getGroups(SqlUtils.makeSelect(COL_GROUPS_ID,TABLE_GROUPS,SqlUtils.eq(COL_GROUPS_PARENT, 
                                                                                                         SqlUtils.quote(group.getId()))));
            if(subGroups.size()>0) {
                sb.append("<b>Sub groups:</b><ul>");
                for(Group subGroup: subGroups) {
                    sb.append("<li><a href=\"/showgroup?group=" + subGroup.getFullName() +"\">" +
                              subGroup.getFullName()+"</a>");
                }
                sb.append("</ul>");
            }
            
            where.add(SqlUtils.eq(COL_FILES_GROUP_ID, SqlUtils.quote(group.getId())));
            String query =
                SqlUtils.makeSelect(SqlUtils.comma(COL_FILES_ID,COL_FILES_NAME,COL_FILES_TYPE, COL_FILES_FILE),
                                    typeHandler.getQueryOnTables(args),
                                    SqlUtils.makeAnd(where));
            Statement stmt = execute(query);
            SqlUtils.Iterator  iter    = SqlUtils.getIterator(stmt);
            ResultSet results;
            int cnt = 0;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    if(cnt++>1000) {
                        sb.append("<li> ...");
                        break;
                    }
                    int col = 1;
                    String id = results.getString(col++);
                    String name = results.getString(col++);
                    String type = results.getString(col++);
                    String file = results.getString(col++);
                    if(cnt==1) {
                        sb.append("<b>Files:</b>");
                        sb.append("<ul>");
                    }
                    sb.append("<li>" + HtmlUtil.href("/showfile?" + ARG_ID +"=" + id, name));
                }
            }
            if(cnt>0) sb.append("</ul>");

        }
        if(topLevel) sb.append("</ul>");
        return new TextResult(title,sb);
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TextResult listGroups(Hashtable args)
        throws Exception {
        TypeHandler typeHandler = getTypeHandler(args);
        List where = typeHandler.assembleWhereClause(args);
        String query =
            SqlUtils.makeSelect(SqlUtils.distinct(COL_FILES_GROUP_ID),
                                typeHandler.getQueryOnTables(args),
                                SqlUtils.makeAnd(where));

        Statement    statement = execute(query);
        String[]     groups    = SqlUtils.readString(statement, 1);
        StringBuffer sb        = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_GROUPS));
        for (int i = 0; i < groups.length; i++) {
            Group group = findGroupFromId(groups[i]);
            if (group != null) {
                sb.append(XmlUtil.tag(TAG_GROUP,
                                      XmlUtil.attrs(ATTR_NAME,
                                                    group.getFullName(),
                                                    ATTR_ID,
                                                    group.getId())));
            }
        }
        sb.append(XmlUtil.closeTag(TAG_GROUPS));
        return new TextResult("",sb, TextResult.TYPE_XML);
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List assembleWhereClause(Hashtable args) throws Exception {
        return getTypeHandler(args).assembleWhereClause(args);
    }






    /** _more_          */
    private Hashtable groupMap = new Hashtable();

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
        Group group = (Group) groupMap.get(id);
        if (group != null) {
            return group;
        }
        String query = SELECT_GROUP + " WHERE "
                       + SqlUtils.eq("id", SqlUtils.quote(id));
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
        Group group = (Group) groupMap.get(name);
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
            where += SqlUtils.eq(COL_GROUPS_PARENT,
                                 SqlUtils.quote(parent.getId())) + " AND ";
        } else {
            where += COL_GROUPS_PARENT +" is null AND ";
        }
        where += SqlUtils.eq(COL_GROUPS_NAME, SqlUtils.quote(lastName));

        String    query     = SELECT_GROUP + " WHERE " + where;

        Statement statement = execute(query);
        ResultSet results = statement.getResultSet();
        if (results.next()) {
            group = new Group(parent, results.getString(1),
                              results.getString(3), results.getString(4));
        } else {
            String insert = SqlUtils.makeInsert(TABLE_GROUPS, COLUMNS_GROUPS,
                                SqlUtils.comma(SqlUtils.quote(getGUID()),
                                    ((parent != null)
                                     ? SqlUtils.quote(parent.getId())
                                     : "NULL"), SqlUtils.quote(lastName),
                                         SqlUtils.quote(lastName)));
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
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<DataInfo> getDataInfos(Hashtable args) throws Exception {
        TypeHandler typeHandler = getTypeHandler(args);
        List where =typeHandler.assembleWhereClause(args);
        String query =
            SqlUtils.makeSelect(SqlUtils.comma(COL_FILES_NAME, 
                                               COL_FILES_TYPE, 
                                               COL_FILES_GROUP_ID,
                                               COL_FILES_FILE, 
                                               COL_FILES_FROMDATE,
                                               COL_FILES_TODATE), typeHandler.getQueryOnTables(args),
                                SqlUtils.makeAnd(where), "order by " + COL_FILES_FROMDATE);
        Statement statement = execute(query);
        ResultSet         results;
        SqlUtils.Iterator iter       = SqlUtils.getIterator(statement);
        List<DataInfo>   dataInfos = new ArrayList<DataInfo>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                dataInfos.add(
                    new DataInfo(
                        results.getString(col++),
                        results.getString(col++),
                        findGroupFromId(results.getString(col++)),
                        results.getString(col++), 
                        results.getTimestamp(col++).getTime()));
            }
        }
        return dataInfos;
    }


    /** _more_          */
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
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected StringBuffer processQuery(Hashtable args)
            throws Exception {
        List<DataInfo> dataInfos = getDataInfos(args);
        boolean         html = !Misc.equals(args.get("output"), "xml");
        StringBuffer    sb         = new StringBuffer();
        if (html) {
            sb.append("<ul>");
        } else {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag("catalog",
                                      XmlUtil.attrs("name",
                                          "Query Results")));
        }


        StringBufferCollection sbc = new StringBufferCollection();
        for (DataInfo dataInfo : dataInfos) {
            StringBuffer ssb = sbc.getBuffer(dataInfo.getType());
            if (html) {
                ssb.append("<li>" + dataInfo.getFile() + " "
                           + new Date(dataInfo.getStartDate()));
            } else {
                ssb.append(
                    XmlUtil.tag(
                        "dataset",
                        XmlUtil.attrs(
                            "name", "" + new Date(dataInfo.getStartDate()),
                            "urlPath", dataInfo.getFile())));
            }
        }


        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb     = sbc.getBuffer(type);
            if (html) {
                sb.append("<li>" + type + "<ul>");
                sb.append(ssb);
                sb.append("</ul>");
            } else {
                sb.append("<dataset name=\"" + type   + "\">\n");
                sb.append(ssb);
                sb.append(XmlUtil.closeTag("dataset"));
            }
        }
        if (html) {
            sb.append("</ul>");
        } else {
            sb.append(XmlUtil.closeTag("dataset"));
        }


        if (html) {
            sb.append("</table>");
        } else {
            sb.append(XmlUtil.closeTag("catalog"));
        }
        return sb;
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
    public List<RadarInfo> collectLevel3radarFiles(File rootDir,
            String groupName)
            throws Exception {
        long                  t1         = System.currentTimeMillis();
        final List<RadarInfo> radarInfos = new ArrayList();
        long                  baseTime   = new Date().getTime();
        Group                 group      = findGroup(groupName);
        for (int stationIdx = 0; stationIdx < 100; stationIdx++) {
            for (int i = 0; i < 10; i++) {
                radarInfos.add(new RadarInfo("", group, "file" + stationIdx + "_"
                                             + i + "_" + group, "stn"
                                                 + stationIdx, "product"
                                                     + (i % 20), baseTime
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
    public List<RadarInfo> collectLevel3radarFilesxxx(File rootDir,
            final String groupName)
            throws Exception {
        final Group            group      = findGroup(groupName);
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
                if(radarInfos.size()%5000 == 0) {
                    System.err.println ("Found:" + radarInfos.size());
                }
                String station = matcher.group(1);
                String product = matcher.group(2);
                Date   dttm    = sdf.parse(matcher.group(3));
                radarInfos.add(new RadarInfo("", group, f.toString(), station,
                                             product, dttm.getTime()));
                return DO_CONTINUE;
            }
        };

        IOUtil.walkDirectory(rootDir, fileViewer);
        long t2 = System.currentTimeMillis();
        System.err.println("found:" + radarInfos.size() + " in " + (t2 - t1));
        return radarInfos;
    }


    public List<DataInfo> collectFiles(File rootDir)
            throws Exception {
        final String rootStr = rootDir.toString();
        final int rootStrLen = rootStr.length();
        final List<DataInfo>  dataInfos = new ArrayList();
        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
            public int viewFile(File f) throws Exception {
                String name = f.getName();
                //                System.err.println(name);
                if(name.startsWith(".")) {
                    return DO_DONTRECURSE;
                }
                if(f.isDirectory()) return DO_CONTINUE;
                String  path    = f.toString();
                String noext = IOUtil.stripExtension(path);


                String dirPath = f.getParent().toString();
                dirPath = dirPath.substring(rootStrLen);
                List toks = StringUtil.split(dirPath, File.separator, true, true);
                Group            group      = findGroup(StringUtil.join("/",toks));
                dataInfos.add(new DataInfo(name, TypeHandler.TYPE_ANY, group,  f.toString(), f.lastModified()));
                if(dataInfos.size()>100) {
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

    public void loadTestFiles() throws Exception {
        File            rootDir = new File("c:/cygwin/home/jeffmc/unidata/src/idv/trunk/ucar/unidata");
        //        File            rootDir = new File("/harpo/jeffmc/src/idv/trunk/ucar/unidata");
        List<DataInfo> files   = collectFiles(rootDir);
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
        Statement statement = connection.createStatement();
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
        String[] results = SqlUtils.readString(statement, 1);
        for (int i = 0; (i < results.length) && (i < 10); i++) {
            System.err.print(results[i] + " ");
            if (i == 9) {
                System.err.print("...");
            }
        }
    }






}


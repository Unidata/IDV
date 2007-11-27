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
import ucar.unidata.xml.XmlUtil;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.StringUtil;



import java.net.*;
import java.io.File;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Date;
import java.util.List;



import java.util.regex.*;


/**
 * Class SqlUtils _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Repository implements TableDefinitions {


    public static final String TYPE_LEVEL3RADAR = "level3radar";
    public static final String TYPE_LEVEL2RADAR = "level2radar";


    public static final String ATTR_ID = "id";    
    public static final String ATTR_NAME = "name";


    public static final String ARG_TYPE = "type";
    public static final String ARG_GROUP = "group";
    public static final String ARG_TODATE = "todate";
    public static final String ARG_FROMDATE = "fromdate";

    public static final String ARG_PRODUCT = "product";
    public static final String ARG_STATION = "station";

    long baseTime = System.currentTimeMillis();
    int keyCnt = 0;
    private String getKey() {
        return baseTime +"_"+(keyCnt++);
    }


    /** _more_          */
    private Connection connection;


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public Repository(String driver, String connectionURL) throws Exception {
        Misc.findClass(driver);
        //        connection = DriverManager.getConnection(connectionURL, "jeff", "mypassword");
        connection = DriverManager.getConnection(connectionURL);		 
    }


    protected StringBuffer processQuery(Hashtable args) throws Exception {
        long t1 = System.currentTimeMillis();
        String query = (String)args.get("query");
        Statement statement  =connection.createStatement();
        statement.execute(query);
        SqlUtils.Iterator  iter = SqlUtils.getIterator(statement);
        int cnt =0 ;
        StringBuffer sb = new StringBuffer();
        sb.append("<form action=\"/query\">\n");
        sb.append("<input  name=\"query\" size=\"40\" value=\"" + query +"\"/>");
        sb.append("<input  type=\"submit\" value=\"Query\" />");
        sb.append("</form>\n");


        sb.append("<table>");
        

        ResultSet results;
        while ((results = iter.next()) != null) {
            ResultSetMetaData rsmd = results.getMetaData();
            while (results.next()) {
                int colcnt = 0;
                if(cnt==0) {
                    sb.append("<table><tr>");
                    for(int i=0;i<rsmd.getColumnCount();i++) {
                        sb.append("<td><b>"+rsmd.getColumnLabel(i+1)+"</b></td>");
                    }
                    sb.append("</tr>");
                }
                sb.append("<tr>");
                while(colcnt<rsmd.getColumnCount()) {
                    sb.append("<td>"+results.getString(++colcnt)+"</td>");
                }
                sb.append("</tr>\n");
                if(cnt++>100000) {
                    sb.append("<tr><td>...</td></tr>");
                    break;
                }
            }
            if(cnt>100000) {
                break;
            }
        }
        sb.append("</table>");
        long t2 = System.currentTimeMillis();
        return new StringBuffer("Fetched:" + cnt +" rows in: " + (t2-t1) +"ms <p>"+sb.toString());
    }

    protected void makeHtmlHidden(StringBuffer sb, String value,String name) {
        sb.append("<input type=\"hidden\" name=\"" + name +"\" value=\"" + value +"\"/>");
    }

    protected void makeHtmlSelect(StringBuffer sb, String[]values,String name, String label) {
        sb.append(" <tr><td align=\"right\"><b>"+ label+"</b> </td><td><select name=\"" + name +"\">");
        sb.append("<option>" + "All" +"</option>");
        for(int i=0;i<values.length;i++) {
            String optionLabel = getProductName(values[i],null);
            if(optionLabel!=null) optionLabel = optionLabel +" (" + values[i]+")";
            else
                optionLabel = values[i];
            sb.append("<option value=\"" + values[i] +"\">" + optionLabel+"</option>");
        }
        sb.append("</td></tr>");
    }


    protected StringBuffer processRadarForm(Hashtable args) throws Exception {

        Statement statement  =connection.createStatement();
        long t1 = System.currentTimeMillis();

        String where = assembleRadarWhereClause(args).trim();
        if(where.length()>0) {
            where  = " where " + where;
        }

        statement.execute(SELECT_FILES_GROUPS+where);
        String[]groups = SqlUtils.readString(statement,1);
        long t2 = System.currentTimeMillis();

        statement.execute(SELECT_LEVEL3RADAR_PRODUCTS+where);
        String[]products = SqlUtils.readString(statement,1);
        long t3 = System.currentTimeMillis();

        statement.execute(SELECT_LEVEL3RADAR_STATIONS+where);
        String[]stations = SqlUtils.readString(statement,1);
        long t4 = System.currentTimeMillis();

        System.err.println("time:" + (t2-t1) + " " + (t3-t2) + " " + (t4-t3));

        statement.execute(SELECT_FILES_MAXDATE+where);
        String[]maxdates = SqlUtils.readString(statement,1);

        statement.execute(SELECT_FILES_MINDATE+where);
        String[]mindates = SqlUtils.readString(statement,1);


        String maxdate = (maxdates.length>0?maxdates[0]:"");
        String mindate = (mindates.length>0?mindates[0]:"");


        StringBuffer sb = new StringBuffer();
        sb.append("<h2> Radar Search Form</h2>");

        sb.append("<table cellpadding=\"5\">");
        sb.append("<form action=\"/radar/query\">\n");

        if(groups.length>1) {
            sb.append(" <tr><td align=\"right\"><b>Group:</b> </td><td><select name=\"" + ARG_GROUP +"\">");
            sb.append("<option>" + "All" +"</option>");
            for(int i=0;i<groups.length;i++) {
                Group group = findGroupFromId(groups[i]);
                if(group!=null) {
                    sb.append("<option>" + group.getFullName()+"</option>");
                }
            }
            sb.append("</td></tr>");
        } else if(groups.length==1) {
            Group group = findGroupFromId(groups[0]);
            makeHtmlHidden(sb,group.getFullName(), ARG_GROUP);
        }

        if(stations.length>1) {
            makeHtmlSelect(sb,stations, ARG_STATION, "Station:");
        } else if(stations.length==1) {
            makeHtmlHidden(sb,stations[0], ARG_STATION);
        }
        if(products.length>1) {
            makeHtmlSelect(sb,products, ARG_PRODUCT, "Product:");
        } else if(products.length==1) {
            makeHtmlHidden(sb,products[0], ARG_PRODUCT);
        }



        sb.append("<tr><td align=\"right\"><b>Date Range: </b></td><td><input name=\"fromdate\" value=\"" + mindate +"\"> -- <input name=\"todate\" value=\"" + maxdate +"\"></td></tr>");


        String output = (String)args.get("output");
        if(output == null) {
            sb.append("<tr><td align=\"right\"><b>Output Type: </b></td><td><select name=\"output\"><option>html</option><option>xml</option></select></td></tr>");
        } else {
            makeHtmlHidden(sb, output,"output");
        }


        sb.append("<tr><td><input  type=\"submit\" value=\"Search for radar\" /></td></tr>");
        sb.append("<table>");

        sb.append("</form>");
        sb.append("<p><a href=\"/radar/listgroups\"> List groups</a>");
        sb.append("<p><a href=\"/radar/liststations\"> List stations</a>");
        sb.append("<p><a href=\"/radar/listproducts\"> List products</a>");
        sb.append("<p><a href=\"/radar/maketable\"> Scan disk</a>");

        sb.append("<form action=\"/query\">\n");
        sb.append("<input  name=\"query\" size=\"40\"/>");
        sb.append("<input  type=\"submit\" value=\"Query\" />");
        sb.append("</form>\n");
        
        return sb;
    }


    protected StringBuffer processRadarList(Hashtable args, String column, String tag) throws Exception {
        String query = SqlUtils.makeSelect(SqlUtils.distinct(column), TABLE_LEVEL3RADAR,   assembleRadarWhereClause(args));
        Statement statement  =execute(query);
        String[]stations = SqlUtils.readString(statement,1);
        StringBuffer sb = new StringBuffer();
        sb.append (XmlUtil.XML_HEADER +"\n" );
        sb.append(XmlUtil.openTag(tag+"s"));
        for(int i=0;i<stations.length;i++) {
            sb.append(XmlUtil.tag(tag, XmlUtil.attrs(ATTR_ID, stations[i],ATTR_NAME,getProductName(stations[i]))));
        }
        sb.append(XmlUtil.closeTag(tag+"s"));
        return sb;
    }



    protected StringBuffer processRadarListGroup(Hashtable args) throws Exception {
        String query = SqlUtils.makeSelect(SqlUtils.distinct(COL_FILES_GROUP_ID),
                                           TABLE_FILES+","+TABLE_LEVEL3RADAR,  
                                           SqlUtils.eq(COL_FILES_ID,COL_LEVEL3RADAR_ID) + " " +
                                           assembleRadarWhereClause(args));
        Statement statement  =execute(query);
        String []groups = SqlUtils.readString(statement,1);
        StringBuffer sb = new StringBuffer();
        sb.append (XmlUtil.XML_HEADER +"\n" );
        sb.append(XmlUtil.openTag("groups"));
        for(int i=0;i<groups.length;i++) {
            Group group = findGroupFromId(groups[i]);
            if(group!=null) {
                sb.append(XmlUtil.tag("group", XmlUtil.attrs("name", group.getFullName())));
            }
        }
        sb.append(XmlUtil.closeTag("groups"));
        return sb;
    }


    private String getDateString(String dttm) throws java.text.ParseException {
        Date date = DateUtil.parse(dttm);
        return SqlUtils.format(date);
        
    }

    private void addOr(String column, String value, List list) {
        if(value!=null && value.trim().length()>0 && !value.toLowerCase().equals("all")) {
            list.add("("+SqlUtils.makeOrSplit(column, value,true)+")");
        }
    }

    protected String assembleWhereClause(Hashtable args) throws Exception {
        List where = new ArrayList();
        String groupName = (String) args.get("group");
        if(groupName!=null && !groupName.toLowerCase().equals("all") ) {
            Group group = findGroup(groupName);
            where.add(SqlUtils.eq(COL_FILES_GROUP_ID , SqlUtils.quote(group.getId())));
        }


        addOr(COL_FILES_TYPE, (String) args.get(ARG_TYPE), where);

        String fromdate = (String) args.get(ARG_FROMDATE);
        if(fromdate!=null && fromdate.trim().length()>0) {
            where.add(SqlUtils.ge(COL_FILES_FROMDATE , SqlUtils.quote(getDateString(fromdate))));
        }
        String todate = (String) args.get(ARG_TODATE);
        if(todate!=null && todate.trim().length()>0) {
            where.add(SqlUtils.le(COL_FILES_TODATE , SqlUtils.quote(getDateString(todate))));
        }

        if(where.size()>0) {
            return StringUtil.join( " AND ",where);
        }
        return "";
    }

    protected String assembleRadarWhereClause(Hashtable args) throws Exception {
        List where = new ArrayList();
        String basic = assembleWhereClause(args);
        if(basic.length()>0) {
            where.add(basic);
        }
        addOr(COL_LEVEL3RADAR_STATION, (String) args.get(ARG_STATION), where);
        addOr(COL_LEVEL3RADAR_PRODUCT, (String) args.get(ARG_PRODUCT), where);
        if(where.size()>0) {
            return StringUtil.join( " AND ",where);
        }
        return "";
    }


    private Hashtable groupMap = new Hashtable();

    protected Group findGroupFromId(String id) throws Exception {
        if(id == null || id.length()==0) return  null;
        Group group = (Group) groupMap.get(id);
        if(group!=null) return group;
        String query = SELECT_GROUP+ " WHERE " +SqlUtils.eq("id", SqlUtils.quote(id)) ;
        Statement statement  =execute(query);
        ResultSet results = statement.getResultSet();        
        if(results.next()) {
            group = new Group(findGroupFromId(results.getString(2)), results.getString(1), results.getString(3), results.getString(4));
        } else {
            //????
            return null;
        }
        groupMap.put(id, group);
        return group;
    }


    protected Group findGroup(String name) throws Exception {
        Group group = (Group) groupMap.get(name);
        if(group!=null) return group;
        List<String> toks =  (List<String>)StringUtil.split(name,"/",true,true);
        Group parent = null;
        String lastName;
        if(toks.size()==0 || toks.size()==1) {
            lastName = name;
        } else {
            lastName = toks.get(toks.size()-1);
            toks.remove(toks.size()-1);
            parent = findGroup(StringUtil.join("/",toks));
        }
        String where = "";
        if(parent !=null) {
            where += SqlUtils.eq(COL_GROUPS_PARENT, SqlUtils.quote(parent.getId())) +" AND ";
        }
        where += SqlUtils.eq(COL_GROUPS_NAME, SqlUtils.quote(lastName));

        String query = SELECT_GROUP+ " WHERE " + where;

        Statement statement  =connection.createStatement();
        statement.execute(query);
        ResultSet results = statement.getResultSet();        
        if(results.next()) {
            group = new Group(parent, results.getString(1), results.getString(3), results.getString(4));
        } else {
            String insert = SqlUtils.makeInsert(TABLE_GROUPS,
                                                COLUMNS_GROUPS,
                                                SqlUtils.comma(SqlUtils.quote(getKey()), 
                                                               (parent!=null?SqlUtils.quote(parent.getId()):"NULL"),
                                                               SqlUtils.quote(lastName), 
                                                               SqlUtils.quote(lastName)));
            statement.execute(insert);
            return findGroup(name);
            //            group = new Group(parent,0,lastName,lastName);
        }
        groupMap.put(group.getId(), group);
        groupMap.put(name, group);
        return group;
    }


    protected List<RadarInfo> getRadarInfos(Hashtable args) throws Exception {
        String where = assembleRadarWhereClause(args);
        if(where.trim().length()>0) {
            where  = where + " AND ";
        }
        where += SqlUtils.eq(COL_FILES_ID,COL_LEVEL3RADAR_ID);
        
        
        String query = SqlUtils.makeSelect(SqlUtils.comma(COL_FILES_GROUP_ID,
                                                          COL_FILES_FILE,
                                                          COL_LEVEL3RADAR_STATION,
                                                          COL_LEVEL3RADAR_PRODUCT,
                                                          COL_FILES_FROMDATE,
                                                          COL_FILES_TODATE),
                                           TABLE_LEVEL3RADAR+"," +TABLE_FILES,  
                                           where,
                                           "order by " + COL_FILES_FROMDATE);
        System.err.println("query:" + query);
        Statement statement  =connection.createStatement();
        statement.execute(query);
        ResultSet results;
        SqlUtils.Iterator  iter = SqlUtils.getIterator(statement);
        List<RadarInfo> radarInfos= new ArrayList<RadarInfo>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                radarInfos.add (new RadarInfo(findGroupFromId(results.getString(1)),
                                              results.getString(2),
                                              results.getString(3),
                                              results.getString(4),
                                              results.getTimestamp(5).getTime()));
            }  
        }
        return radarInfos;
    }


    Properties productMap;


    protected String getProductName(String product) {
        return getProductName(product, product);
    }

    protected String getProductName(String product, String dflt) {
        if(productMap==null) {
            productMap = new Properties();
            try {
                InputStream s = IOUtil.getInputStream("/ucar/unidata/repository/names.properties", getClass());
                productMap.load(s);
            } catch(Exception exc) {
                System.err.println ("err:" + exc);
            }
        }
        String name = (String)productMap.get(product);
        if(name!=null) {
            return name;
        }
        //        System.err.println("not there:" + product+":");
        return dflt;
    }

    protected StringBuffer processRadarQuery(Hashtable args) throws Exception {

        long t1 = System.currentTimeMillis();
        List<RadarInfo> radarInfos= getRadarInfos(args);
        long t2 = System.currentTimeMillis();
        String output = (String) args.get("output");        
        boolean html = (output==null || output.equals("html"));


        StringBuffer sb = new StringBuffer();
        if(html) {
            sb.append("<ul>");
        } else {
            sb.append (XmlUtil.XML_HEADER+"\n" );
            sb.append(XmlUtil.openTag("catalog", XmlUtil.attrs("name","Radar Query Results")));
        }

        Hashtable map = new Hashtable();
        for(RadarInfo radarInfo: radarInfos) {
            StringBufferCollection sbc = (StringBufferCollection) map.get(radarInfo.getStation());
            if(sbc == null) {
                sbc = new StringBufferCollection();
                map.put(radarInfo.getStation(),sbc);
            }

            StringBuffer ssb = sbc.getBuffer(radarInfo.getProduct());
            if(html) {
                ssb.append("<li>" +radarInfo.getFile()  +" " +  new Date(radarInfo.getStartDate()));
            } else {
                ssb.append(XmlUtil.tag("dataset", 
                                      XmlUtil.attrs("name",
                                                    ""+new Date(radarInfo.getStartDate()),
                                                    "urlPath" ,radarInfo.getFile())));
            }  
        }
        

        for(Enumeration keys = map.keys();keys.hasMoreElements();) {
            String station = (String)keys.nextElement();
            StringBufferCollection sbc = (StringBufferCollection) map.get(station);
            if(html) {
                sb.append("<li>" + station +"<ul>");
            } else {
                sb.append(XmlUtil.openTag("dataset", XmlUtil.attrs("name",  getProductName(station)+ " (" + station +")")));
            }
            for(int i=0;i<sbc.getKeys().size();i++) {
                String product  = (String) sbc.getKeys().get(i);
                StringBuffer ssb = sbc.getBuffer(product);
                if(html) {
                    sb.append("<li>" + getProductName(product) +"<ul>");
                    sb.append(ssb);
                    sb.append("</ul>");
                } else {
                    sb.append("<dataset name=\""+ getProductName(product)+"\">\n");
                    sb.append(ssb);
                    sb.append(XmlUtil.closeTag("dataset"));
                }
            }
            if(html) {
                sb.append("</ul>");
            } else {
                sb.append(XmlUtil.closeTag("dataset"));
            }
        }



        if(html) {
            sb.append("</table>");
        } else {
                sb.append(XmlUtil.closeTag("catalog"));
        }
        System.err.println("query time:" + (t2 - t1));
        return sb;
    }



    public List<RadarInfo> collectLevel3radarFiles(File rootDir, String groupName)
        throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<RadarInfo>  radarInfos = new ArrayList();
        long baseTime = new Date().getTime();
        Group group = findGroup(groupName);
        for(int stationIdx=0;stationIdx<100;stationIdx++) {
            for(int i=0;i<10;i++) {
                radarInfos.add(new RadarInfo(group, "file"+stationIdx+"_"+i+"_"+group, "stn"+stationIdx, "product"+(i%20),
                                             baseTime + radarInfos.size()*100));
            }
        }

        return radarInfos;
    }

    /**
     * _more_
     *
     *
     * @param rootDir _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public  List<RadarInfo> collectLevel3radarFilesxxx(File rootDir, final String groupName)
        throws Exception {
        final Group group = findGroup(groupName);
        long                   t1         = System.currentTimeMillis();
        final List<RadarInfo>  radarInfos = new ArrayList();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        final Pattern pattern =
            Pattern.compile(
                            "([^/]+)/([^/]+)/[^/]+_(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d)");

        IOUtil.FileViewer fileViewer = new IOUtil.FileViewer() {
                public boolean viewFile(File f) throws Exception {
                    String  name    = f.toString();
                    Matcher matcher = pattern.matcher(name);
                    if ( !matcher.find()) {
                        return true;
                    }
                    String station = matcher.group(1);
                    String product = matcher.group(2);
                    Date   dttm    = sdf.parse(matcher.group(3));
                    radarInfos.add(new RadarInfo(group,f.toString(), station, product,
                                                 dttm.getTime()));
                    return true;
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
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public  void makeLevel3RadarTable()
        throws Exception {
        String sql = IOUtil.readContents("/ucar/unidata/repository/makedb.sql", getClass());
        Statement statement  =connection.createStatement();
        SqlUtils.loadSql(sql, statement);

        File            rootDir = new File("/data/ldm/gempak/nexrad/nids");
        List<RadarInfo> files   = collectLevel3radarFiles(rootDir,"IDD");
        files.addAll(collectLevel3radarFiles(rootDir,"LDM/LDM2"));
        System.err.println ("Inserting:" + files.size() + " files");

        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement filesInsert = connection.prepareStatement(INSERT_FILES);
        PreparedStatement radarInsert = connection.prepareStatement(INSERT_LEVEL3RADAR);

        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (RadarInfo radarInfo : files) {
            if ((++cnt) % 10000 == 0) {
                long tt2 = System.currentTimeMillis();
                double tseconds = (tt2-t1)/1000.0;
                System.err.println("# " + cnt + " rate: " + ((int)(cnt/tseconds))+"/s");
            }
            int col = 1;
            String id = getKey();
            filesInsert.setString(col++,id);
            filesInsert.setString(col++,TYPE_LEVEL3RADAR);
            filesInsert.setString(col++,radarInfo.getGroupId());
            filesInsert.setString(col++,radarInfo.getFile().toString());
            filesInsert.setTimestamp(col++,new java.sql.Timestamp(radarInfo.getStartDate()));
            filesInsert.setTimestamp(col++,new java.sql.Timestamp(radarInfo.getStartDate()));
            filesInsert.addBatch();

            col=1;
            radarInsert.setString(col++,id);
            radarInsert.setString(col++,radarInfo.getStation());
            radarInsert.setString(col++,radarInfo.getProduct());
            radarInsert.addBatch();
            batchCnt++;
            if(batchCnt>100) {
                filesInsert.executeBatch();
                radarInsert.executeBatch();
                batchCnt=0;
            }
        }
        if(batchCnt>0) {
            filesInsert.executeBatch();
            radarInsert.executeBatch();
        }
        connection.setAutoCommit(true);
        connection.commit();
        long t2 = System.currentTimeMillis();
        double seconds = (t2-t1)/1000.0;
        System.err.println("cnt:" + cnt + " time:" + seconds + " rate:" + (cnt/seconds));

    }


    private Statement execute(String sql) throws Exception {
        Statement statement =   connection.createStatement();
        System.err.println ("query:" + sql);
        statement.execute(sql);
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
        long t1 = System.currentTimeMillis();
        System.err.println("sql: " + sql);
        Statement statement =   connection.createStatement();
        statement.execute(sql);
        String[] results = SqlUtils.readString(statement, 1);
        for (int i = 0; (i < results.length) && (i < 10); i++) {
            System.err.print(results[i] + " ");
            if (i == 9) {
                System.err.print("...");
            }
        }

        long t2 = System.currentTimeMillis();
        System.err.println("\ntime:" + (t2 - t1) + " results:"
                           + results.length);
    }




}


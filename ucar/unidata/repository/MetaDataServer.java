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
l * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */





package ucar.unidata.repository;


import ucar.unidata.data.SqlUtils;
import ucar.unidata.xml.XmlUtil;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.StringUtil;



import java.net.*;
import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
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
public class MetaDataServer extends HttpServer {

    /** _more_          */
    private Connection connection;

    /** _more_          */
    private Statement statement;

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public MetaDataServer(String driver, String connectionURL) throws Exception {
        super(8080);
        Misc.findClass(driver);
        //        connection = DriverManager.getConnection(connectionURL, "jeff", "mypassword");
        connection = DriverManager.getConnection(connectionURL);		 
        statement       = connection.createStatement();
    }


    protected RequestHandler doMakeRequestHandler(
            final Socket socket) throws Exception {
        return new RequestHandler(this, socket) {
                protected void handleRequest(String path,   Hashtable formArgs,
                                             Hashtable httpArgs,
                                             String content) throws Exception {
                    try {
                    if(path.equals("/radar/query")) {
                        processRadarQuery(this, formArgs, httpArgs);
                    } else if(path.equals("/query")) {
                        processQuery(this, formArgs, httpArgs);                        

                    } else if(path.equals("/radar/form")) {
                        processRadarForm(this, formArgs, httpArgs);
                    } else if(path.equals("/radar/liststations")) {
                        processRadarList(this, formArgs, "station","station");
                    } else if(path.equals("/radar/listproducts")) {
                        processRadarList(this, formArgs, "product","product");
                    } else if(path.equals("/radar/listcollections")) {
                        processRadarList(this, formArgs, "collection","collection");
                    } else if(path.equals("/radar/maketable")) {
                        long t1 = System.currentTimeMillis();
                        makeNidsTable();
                        long t2 = System.currentTimeMillis();
                        writeResult(true, "Time:" + (t2-t1), "text/html");
                    } else {
                        writeResult(true, "Unknown url:" + path, "text/html");
                    }
                    } catch (Exception exc) {
                        LogUtil.logException ("",exc);
                        writeResult(true, LogUtil.getStackTrace(exc), "text/html");
                    }

                }

            };
    }


    private void processQuery(HttpServer.RequestHandler requestHandler,
                              Hashtable formArgs,
                              Hashtable httpArgs) throws Exception {
        long t1 = System.currentTimeMillis();
        String query = (String)formArgs.get("query");

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
        requestHandler.writeResult(true, "Fetched:" + cnt +" rows in: " + (t2-t1) +"ms <p>"+sb.toString(), "text/html");
    }

    private void makeSelect(StringBuffer sb, String[]values,String name, String label) {
        sb.append(" <tr><td align=\"right\"><b>"+ label+"</b> </td><td><select name=\"" + name +"\">");
        sb.append("<option>" + "All" +"</option>");
        for(int i=0;i<values.length;i++) {
            sb.append("<option>" + values[i] +"</option>");
        }
        sb.append("</td></tr>");
    }


    private void processRadarForm(HttpServer.RequestHandler requestHandler,
                              Hashtable formArgs,
                              Hashtable httpArgs) throws Exception {

        long t1 = System.currentTimeMillis();
        statement.execute("select distinct collection from nids ");
        String[]collections = SqlUtils.readString(statement,1);
        long t2 = System.currentTimeMillis();

        statement.execute("select distinct product from nids ");
        String[]products = SqlUtils.readString(statement,1);
        long t3 = System.currentTimeMillis();

        statement.execute("select distinct station from nids ");
        String[]stations = SqlUtils.readString(statement,1);
        long t4 = System.currentTimeMillis();


        System.err.println("time:" + (t2-t1) + " " + (t3-t2) + " " + (t4-t3));

        statement.execute("select max(date) from nids ");
        String[]maxdates = SqlUtils.readString(statement,1);

        statement.execute("select min(date) from nids ");
        String[]mindates = SqlUtils.readString(statement,1);


        String maxdate = (maxdates.length>0?maxdates[0]:"");
        String mindate = (mindates.length>0?mindates[0]:"");


        StringBuffer sb = new StringBuffer();
        sb.append("<h2> Radar Search Form</h2>");

        sb.append("<table cellpadding=\"5\">");
        sb.append("<form action=\"/radar/query\">\n");

        makeSelect(sb,collections, "collection", "Collection:");
        makeSelect(sb,stations, "station", "Station:");
        makeSelect(sb,products, "product", "Product:");


        sb.append("<tr><td align=\"right\"><b>Date Range: </b></td><td><input name=\"fromdate\" value=\"" + mindate +"\"> -- <input name=\"todate\" value=\"" + maxdate +"\"></td></tr>");


        sb.append("<tr><td align=\"right\"><b>Output Type: </b></td><td><select name=\"output\"><option>html</option><option>xml</option></select></td></tr>");


        sb.append("<tr><td><input  type=\"submit\" value=\"Search for radar\" /></td></tr>");
        sb.append("<table>");

        sb.append("</form>");
        sb.append("<p><a href=\"/radar/listcollections\"> List collections</a>");
        sb.append("<p><a href=\"/radar/liststations\"> List stations</a>");
        sb.append("<p><a href=\"/radar/listproducts\"> List products</a>");
        sb.append("<p><a href=\"/radar/maketable\"> Scan disk</a>");

        sb.append("<form action=\"/query\">\n");
        sb.append("<input  name=\"query\" size=\"40\"/>");
        sb.append("<input  type=\"submit\" value=\"Query\" />");
        sb.append("</form>\n");
        
        requestHandler.writeResult(true, sb.toString(), "text/html");
    }




      private void processRadarList(HttpServer.RequestHandler requestHandler,
                              Hashtable formArgs, String column, String tag) throws Exception {
        String query = "select distinct " + column +" from nids " + assembleRadarWhereClause(formArgs);
        statement.execute(query);
        String[]stations = SqlUtils.readString(statement,1);
        StringBuffer sb = new StringBuffer();
        sb.append (XmlUtil.XML_HEADER +"\n" );

        sb.append(XmlUtil.openTag(tag+"s"));
        for(int i=0;i<stations.length;i++) {
            sb.append(XmlUtil.tag(tag, XmlUtil.attrs("name", stations[i])));
        }
        sb.append(XmlUtil.closeTag(tag+"s"));
        requestHandler.writeResult(true, sb.toString(), "text/xml");
    }


    private String assembleRadarWhereClause(Hashtable formArgs) {
        List where = new ArrayList();

        String collection = (String) formArgs.get("collection");
        if(collection!=null && !collection.toLowerCase().equals("all") ) {
            where.add(" collection='" + collection +"' ");
        }

        String station = (String) formArgs.get("station");
        if(station!=null && !station.toLowerCase().equals("all") ) {
            where.add(" station='" + station +"' ");
        }

        String product = (String) formArgs.get("product");
        if(product!=null && !product.toLowerCase().equals("all") ) {
            where.add(" product='" + product +"' ");
        }

        String fromdate = (String) formArgs.get("fromdate");
        if(fromdate!=null && fromdate.trim().length()>0) {
            where.add(" date >= '" + fromdate +"'");
        }
        String todate = (String) formArgs.get("todate");
        if(todate!=null && todate.trim().length()>0) {
            where.add(" date <= '" + todate +"'");
        }

        if(where.size()>0) {
            return " where " + StringUtil.join( " AND ",where);
        }
        return "";
    }


    private void processRadarQuery(HttpServer.RequestHandler requestHandler,
                              Hashtable formArgs,
                              Hashtable httpArgs) throws Exception {
        String query = SqlUtils.makeSelect("file,date,station,product","nids " + assembleRadarWhereClause(formArgs),  "order by date");
        System.err.println("query:" + query);
        long t1 = System.currentTimeMillis();
        statement.execute(query);
        ResultSet results;
        SqlUtils.Iterator  iter = SqlUtils.getIterator(statement);

        String output = (String) formArgs.get("output");        
        boolean html = (output==null || output.equals("html"));

        StringBuffer sb = new StringBuffer();
        if(html) {
            sb.append("<table><tr><td><b>File</b></td><td><b>Date</b></td></tr>");
            sb.append("Query:<br><i>" + query+"</i>");
        } else {
            sb.append (XmlUtil.XML_HEADER+"\n" );
            sb.append(XmlUtil.openTag("dataset", XmlUtil.attrs("name","Radar Query Results")));
        }
        while ((results = iter.next()) != null) {
            while (results.next()) {
                if(html) {
                    sb.append("<tr><td>" +results.getString(1)  +"</td><td>" +
                              results.getString(2));
                } else {
                    sb.append(XmlUtil.tag("dataset", 
                                          XmlUtil.attrs("name",
                                                        results.getString(3)+"-"+
                                                        results.getString(4), 
                                                        "urlPath" ,results.getString(1))));

                }
            }  
        }
        if(html) {
            sb.append("</table>");
        } else {
            sb.append(XmlUtil.closeTag("dataset"));
        }

        long t2 = System.currentTimeMillis();
        System.err.println("query time:" + (t2 - t1));

        requestHandler.writeResult(true, sb.toString(), (html?"text/html":"text/xml"));
    }





    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    private void processArgs(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("maketable")) {
                makeNidsTable();
            } else {
                eval(args[i]);
            }
        }
    }

    public static List<RadarInfo> collectNidsFiles(File rootDir, String collection)
            throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<RadarInfo>  radarInfos = new ArrayList();
        long baseTime = new Date().getTime();
        for(int stationIdx=0;stationIdx<100;stationIdx++) {
            for(int i=0;i<500;i++) {
                radarInfos.add(new RadarInfo(collection, "foobar_"+stationIdx+"_"+i+"_"+collection, "stn"+stationIdx, "product"+(i%20),
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
    public static List<RadarInfo> collectNidsFilesxxx(File rootDir, final String collection)
            throws Exception {

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
                radarInfos.add(new RadarInfo(collection,f.toString(), station, product,
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
    public  void makeNidsTable()
            throws Exception {
        String sql = IOUtil.readContents("/ucar/unidata/mddb/makedb.sql", getClass());
        SqlUtils.loadSql(sql, statement);

        File            rootDir = new File("/data/ldm/gempak/nexrad/NIDS");
        List<RadarInfo> files   = collectNidsFiles(rootDir,"LDM1");
        files.addAll(collectNidsFiles(rootDir,"LDM2"));
        files.addAll(collectNidsFiles(rootDir,"LDM3"));
        System.err.println ("Inserting:" + files.size() + " files");

        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement psInsert = connection.prepareStatement("insert into nids values (?,?,?,?,?)");
        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (RadarInfo radarInfo : files) {
            if ((++cnt) % 10000 == 0) {
                long tt2 = System.currentTimeMillis();
                double tseconds = (tt2-t1)/1000.0;
                System.err.println("# " + cnt + " rate: " + (cnt/tseconds));

                System.err.println("dttm:" + new Date(radarInfo.getStartDate()));
                System.err.println("sqldttm:" +  new java.sql.Date(radarInfo.getStartDate()));
            }
            psInsert.setString(1,radarInfo.getCollection());
            psInsert.setString(2,radarInfo.getFile().toString());
            psInsert.setDate(3,new java.sql.Date(radarInfo.getStartDate()));
            psInsert.setString(4,radarInfo.getStation());
            psInsert.setString(5,radarInfo.getProduct());
            psInsert.addBatch();
            batchCnt++;
            if(batchCnt>100) {
                psInsert.executeBatch();
                batchCnt=0;
            }
        }
        if(batchCnt>100) {
            psInsert.executeBatch();
        }
        connection.commit();
        long t2 = System.currentTimeMillis();
        double seconds = (t2-t1)/1000.0;
        System.err.println("cnt:" + cnt + " time:" + seconds + " rate:" + (cnt/seconds));

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


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String connectionURL = "jdbc:derby:testdb;create=true";
        //        String driver = "com.mysql.jdbc.Driver";
        //        String connectionURL = "jdbc:mysql://localhost:3306/test?zeroDateTimeBehavior=convertToNull";
        MetaDataServer mds = new MetaDataServer(driver, connectionURL);
        mds.processArgs(args);
        mds.init();
        //        mds.wait();
    }



}


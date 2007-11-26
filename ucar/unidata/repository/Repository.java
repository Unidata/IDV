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
public class Repository {

    long baseTime = System.currentTimeMillis();
    long cnt = 0;


    private String getKey() {
        return baseTime +"_"+(cnt++);
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

    protected void makeSelect(StringBuffer sb, String[]values,String name, String label) {
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

        sb.append(" <tr><td align=\"right\"><b>Collection:</b> </td><td><select name=\"collection\">");
        sb.append("<option>" + "All" +"</option>");
        for(int i=0;i<collections.length;i++) {
            Collection collection = findCollectionFromId(collections[i]);
            if(collection!=null) {
                sb.append("<option>" + collection.getFullName()+"</option>");
            }
        }
        sb.append("</td></tr>");

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
        
        return sb;
    }


    protected StringBuffer processRadarList(Hashtable args, String column, String tag) throws Exception {
        Statement statement  =connection.createStatement();
        String query = "select distinct " + column +" from nids " + assembleRadarWhereClause(args);
        statement.execute(query);
        String[]stations = SqlUtils.readString(statement,1);
        StringBuffer sb = new StringBuffer();
        sb.append (XmlUtil.XML_HEADER +"\n" );
        sb.append(XmlUtil.openTag(tag+"s"));
        for(int i=0;i<stations.length;i++) {
            sb.append(XmlUtil.tag(tag, XmlUtil.attrs("id", stations[i],"name",getProductName(stations[i]))));
        }
        sb.append(XmlUtil.closeTag(tag+"s"));
        return sb;
    }



    protected StringBuffer processRadarListCollection(Hashtable args) throws Exception {
        Statement statement  =connection.createStatement();
        String query = "select distinct collection from nids " + assembleRadarWhereClause(args);
        statement.execute(query);
        String []collections = SqlUtils.readString(statement,1);
        StringBuffer sb = new StringBuffer();
        sb.append (XmlUtil.XML_HEADER +"\n" );
        sb.append(XmlUtil.openTag("collections"));
        for(int i=0;i<collections.length;i++) {
            Collection collection = findCollectionFromId(collections[i]);
            if(collection!=null) {
                sb.append(XmlUtil.tag("collection", XmlUtil.attrs("name", collection.getFullName())));
            }
        }
        sb.append(XmlUtil.closeTag("collections"));
        return sb;
    }


    private String getDateString(String dttm) throws java.text.ParseException {
        Date date = DateUtil.parse(dttm);
        return SqlUtils.format(date);
        
    }

    protected String assembleRadarWhereClause(Hashtable args) throws Exception {
        List where = new ArrayList();
        String collectionName = (String) args.get("collection");
        if(collectionName!=null && !collectionName.toLowerCase().equals("all") ) {
            Collection collection = findCollection(collectionName);
            where.add(" collection=" + SqlUtils.quote(collection.getId()));
        }

        String station = (String) args.get("station");
        if(station!=null && !station.toLowerCase().equals("all") ) {
            where.add(" station=" + SqlUtils.quote(station));
        }

        String product = (String) args.get("product");
        if(product!=null && !product.toLowerCase().equals("all") ) {
            where.add(" product=" + SqlUtils.quote(product));
        }

        String fromdate = (String) args.get("fromdate");
        if(fromdate!=null && fromdate.trim().length()>0) {
            where.add(" date>=" + SqlUtils.quote(getDateString(fromdate)));
        }
        String todate = (String) args.get("todate");
        if(todate!=null && todate.trim().length()>0) {
            where.add(" date<=" + SqlUtils.quote(getDateString(todate)));
        }

        if(where.size()>0) {
            return " where " + StringUtil.join( " AND ",where);
        }
        return "";
    }


    private static Hashtable collectionMap = new Hashtable();

    protected Collection findCollectionFromId(String id) throws Exception {
        if(id == null || id.length()==0) return  null;
        Collection collection = (Collection) collectionMap.get(id);
        if(collection!=null) return collection;
        String query = "select id,parent,name,description from collections where id=" + SqlUtils.quote(id) ;
        Statement statement  =connection.createStatement();
        statement.execute(query);
        ResultSet results = statement.getResultSet();        
        if(results.next()) {
            collection = new Collection(findCollectionFromId(results.getString(2)), results.getString(1), results.getString(3), results.getString(4));
        } else {
            //????
            return null;
        }
        collectionMap.put(id, collection);
        return collection;
    }


    protected Collection findCollection(String name) throws Exception {
        Collection collection = (Collection) collectionMap.get(name);
        if(collection!=null) return collection;
        List<String> toks =  (List<String>)StringUtil.split(name,"/",true,true);
        Collection parent = null;
        String lastName;
        if(toks.size()==0 || toks.size()==1) {
            lastName = name;
        } else {
            lastName = toks.get(toks.size()-1);
            toks.remove(toks.size()-1);
            parent = findCollection(StringUtil.join("/",toks));
        }
        String query = "select id,parent,name,description from collections where ";
        if(parent !=null) {
            query += " parent=" + parent.getId() +" AND ";
        }
        query += " name=" + SqlUtils.quote(lastName);
        Statement statement  =connection.createStatement();
        statement.execute(query);
        ResultSet results = statement.getResultSet();        
        if(results.next()) {
            collection = new Collection(parent, results.getString(1), results.getString(3), results.getString(4));
        } else {
            String insert = "insert into collections (id, parent, name, description) values(" + SqlUtils.comma(SqlUtils.quote(getKey()), (parent!=null?SqlUtils.quote(parent.getId()):"NULL"),
                                                                                                           SqlUtils.quote(lastName), SqlUtils.quote(lastName))+")";
            statement.execute(insert);
            return findCollection(name);
            //            collection = new Collection(parent,0,lastName,lastName);
        }
        collectionMap.put(collection.getId(), collection);
        collectionMap.put(name, collection);
        return collection;
    }


    protected List<RadarInfo> getRadarInfos(Hashtable args) throws Exception {
        String query = SqlUtils.makeSelect("collection,file,station,product,date","nids " + assembleRadarWhereClause(args),  "order by date");
        System.err.println("query:" + query);
        Statement statement  =connection.createStatement();
        statement.execute(query);
        ResultSet results;
        SqlUtils.Iterator  iter = SqlUtils.getIterator(statement);
        List<RadarInfo> radarInfos= new ArrayList<RadarInfo>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                radarInfos.add (new RadarInfo(findCollectionFromId(results.getString(1)),
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
        System.err.println("not there:" + product+":");
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
            sb.append("<table><tr><td><b>File</b></td><td><b>Date</b></td></tr>");
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
                ssb.append("<tr><td>" +radarInfo.getFile()  +"</td><td>" +
                          new Date(radarInfo.getStartDate()) +"</td></tr>");
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
                sb.append("<tr><td colspan=\"2\"><b>" + station +"</td></tr>");
            } else {
                sb.append(XmlUtil.openTag("dataset", XmlUtil.attrs("name",  getProductName(station)+ " (" + station +")")));
            }
            for(int i=0;i<sbc.getKeys().size();i++) {
                String product  = (String) sbc.getKeys().get(i);
                StringBuffer ssb = sbc.getBuffer(product);
                if(html) {
                    sb.append("<tr><td colspan=\"2\"><b>" + getProductName(product) +"</td></tr>");
                    sb.append(ssb);
                } else {
                    sb.append("<dataset name=\""+ getProductName(product)+"\">\n");
                    sb.append(ssb);
                    sb.append(XmlUtil.closeTag("dataset"));
                }
            }
            if(!html) {
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





    public List<RadarInfo> collectNidsFilesxxx(File rootDir, String collectionName)
        throws Exception {
        long                   t1         = System.currentTimeMillis();
        final List<RadarInfo>  radarInfos = new ArrayList();
        long baseTime = new Date().getTime();
        Collection collection = findCollection(collectionName);
        for(int stationIdx=0;stationIdx<100;stationIdx++) {
            for(int i=0;i<500;i++) {
                radarInfos.add(new RadarInfo(collection, "file"+stationIdx+"_"+i+"_"+collection, "stn"+stationIdx, "product"+(i%20),
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
    public  List<RadarInfo> collectNidsFiles(File rootDir, final String collectionName)
        throws Exception {
        final Collection collection = findCollection(collectionName);
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
        String sql = IOUtil.readContents("/ucar/unidata/repository/makedb.sql", getClass());
        Statement statement  =connection.createStatement();
        SqlUtils.loadSql(sql, statement);

        File            rootDir = new File("/data/ldm/gempak/nexrad/NIDS");
        List<RadarInfo> files   = collectNidsFiles(rootDir,"IDD");
        //        files.addAll(collectNidsFiles(rootDir,"LDM/LDM2"));
        System.err.println ("Inserting:" + files.size() + " files");

        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement psInsert = connection.prepareStatement("insert into nids (collection, file, date, station, product) values (?,?,?,?,?)");
        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (RadarInfo radarInfo : files) {
            if ((++cnt) % 10000 == 0) {
                long tt2 = System.currentTimeMillis();
                double tseconds = (tt2-t1)/1000.0;
                System.err.println("# " + cnt + " rate: " + ((int)(cnt/tseconds))+"/s");
            }
            psInsert.setString(1,radarInfo.getCollectionId());
            psInsert.setString(2,radarInfo.getFile().toString());
            psInsert.setTimestamp(3,new java.sql.Timestamp(radarInfo.getStartDate()));
            psInsert.setString(4,radarInfo.getStation());
            psInsert.setString(5,radarInfo.getProduct());
            psInsert.addBatch();
            batchCnt++;
            if(batchCnt>100) {
                psInsert.executeBatch();
                batchCnt=0;
            }
        }
        if(batchCnt>0) {
            psInsert.executeBatch();
        }
        connection.setAutoCommit(true);
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


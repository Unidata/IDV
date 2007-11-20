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


package ucar.unidata.data;



import java.io.File;
import java.sql.*;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;

import java.text.SimpleDateFormat;

public class SqlUtils {

    
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String quote(Object s) {
        return "\"" + s.toString()  +"\"";
    }


    public static String format(Date d) {
        return sdf.format(d);
    }

    public static String comma(Object s1, Object s2) {
        return s1.toString() +","+s2.toString() ;
    }
    public static String comma(Object s1, Object s2,Object s3) {
        return s1.toString() +","+comma(s2,s3);
    }
    public static String comma(Object s1, Object s2,Object s3,Object s4) {
        return s1.toString() +","+comma(s2,s3,s4);
    }

    public static String makeInsert(String table, String values) {
        return "INSERT INTO " + table +" VALUES ("+ values +")";
    }

    public static String makeSelect(String what, String where) {
        return "SELECT " + what + " FROM " + where +";";
    }


    public static String makeSelect(String what, String where, String extra) {
        return "SELECT " + what + " FROM " + where + " " + extra +";";
    }

    public static  double[] readTime(Statement stmt, int column) throws Exception {
        ResultSet results;
        double [] current = new double[100000];
        int cnt = 0;
        Iterator iter = getIterator(stmt);
        while((results = iter.next())!=null) {
            while(results.next()) {
                Date dttm = results.getDate(column);
                double value =dttm.getTime();
                current[cnt++] =value;
                if(cnt>= current.length) {
                    double[] tmp = current;
                    current = new double[current.length*2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }

        double[] actual = new double[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;


    }


    public static  float[] readFloat(Statement stmt, int column, float missing)
        throws Exception {
        float [] current = new float[100000];
        int cnt = 0;
        ResultSet results;
        Iterator iter = getIterator(stmt);
        while((results = iter.next())!=null) {
            while(results.next()) {
                float value =results.getFloat(column);
                if(value == missing) value = Float.NaN;
                current[cnt++] = value;
                if(cnt>= current.length) {
                    float[] tmp = current;
                    current = new float[current.length*2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        float[] actual = new float[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }

    public static  String[] readString(Statement stmt, String columnName)
        throws Exception {
        return readString(stmt,-1,columnName);
    }



    public static  String[] readString(Statement stmt, int column)
        throws Exception {
            return readString(stmt,column,null);
    }


    private static   String[] readString(Statement stmt, int column,String name)
        throws Exception {
        String [] current = new String[100000];
        int cnt = 0;
        ResultSet results;
        Iterator iter = getIterator(stmt);
        while((results = iter.next())!=null) {
            if(name!=null) {
                column = results.findColumn(name);
                name = null;
            }
            while(results.next()) {
                current[cnt++] = results.getString(column);
                if(cnt>= current.length) {
                    String[] tmp = current;
                    current = new String[current.length*2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        String[] actual = new String[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }


    public static double[] readDouble(Statement stmt, int column, double missing)
        throws Exception {
        double [] current = new double[100000];
        int cnt = 0;
        ResultSet results;
        Iterator iter = getIterator(stmt);
        while((results = iter.next())!=null) {
            while(results.next()) {
                double value =results.getDouble(column);
                if(value == missing) value = Double.NaN;
                current[cnt++] =value;
                if(cnt>= current.length) {
                    double[] tmp = current;
                    current = new double[current.length*2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        double[] actual = new double[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }

    public static Date[] readDate(Statement stmt, int column)
        throws Exception {
        Date [] current = new Date[100000];
        int cnt = 0;
        ResultSet results;
        Iterator iter = getIterator(stmt);
        while((results = iter.next())!=null) {
            while(results.next()) {
                Date value =results.getDate(column);
                current[cnt++] =value;
                if(cnt>= current.length) {
                    Date[] tmp = current;
                    current = new Date[current.length*2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        Date[] actual = new Date[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }


    public static Iterator getIterator(Statement stmt) {
        return new Iterator(stmt);
    }


    public static class Iterator {
        Statement stmt;
        int cnt =0;
        public Iterator(Statement stmt) {
            this.stmt = stmt;
        }
        public ResultSet next() throws SQLException {
            if(cnt != 0) {
                stmt.getMoreResults();        
            }
            cnt++;
            return stmt.getResultSet();
        }
    }

 
    public static void makeTable(Statement stmt) throws Exception {
        try {
            stmt.execute("DROP TABLE nids;");
        } catch(Exception exc) {}
        stmt.execute("CREATE TABLE nids (file char(200), KEY(file), date date, INDEX(date),  station char(50) , INDEX(station), product char(5) );");
        File f = new File("/data/ldm/gempak/nexrad/NIDS");
        File[]files = f.listFiles();
        long t1 = System.currentTimeMillis();
        int cnt = 0;
        for(int xxx=0;xxx<100;xxx++) {
        for(int i=0;i<files.length;i++) {
            File stationDir = files[i];
            String stn = IOUtil.getFileTail(stationDir.toString());
            File[]subdirs = stationDir.listFiles();
            for(int subDirIdx=0;subDirIdx<subdirs.length;subDirIdx++) {
                File subdir = subdirs[subDirIdx];
                String product = IOUtil.getFileTail(subdir.toString());                

                File[]radarFiles = subdir.listFiles();
                for(int dataIdx=0;dataIdx<radarFiles.length;dataIdx++) {
                    String name = radarFiles[dataIdx].toString();
                    String sql = SqlUtils.makeInsert("nids",
                                                     SqlUtils.comma(SqlUtils.quote(name+"_"+xxx),
                                                                    SqlUtils.quote(SqlUtils.format(new Date(radarFiles[dataIdx].lastModified()))),
                                                                    SqlUtils.quote(stn),SqlUtils.quote(product)));
                    //                    System.err.println(sql);
                    cnt++;
                    stmt.execute(sql);
                    if(cnt%10000 ==0) System.err.println("# " +cnt);
                }

            }
        }
        }
        System.err.println("");
        long t2 = System.currentTimeMillis();
        System.err.println("cnt:" + cnt +" time:" + (t2-t1));

    }


    public static void eval(Statement stmt, String sql) throws Exception {
        long t1 = System.currentTimeMillis();
        System.err.println("sql: " + sql);
        stmt.execute(sql);
        String[]results = readString(stmt,1);
        for(int i=0;i<results.length&&i<10;i++) {
            System.err.print(results[i]+" ");
            if(i==9)   System.err.print("...");
        }

        long t2 = System.currentTimeMillis();
        System.err.println("\ntime:" + (t2-t1) + " results:" + results.length);
    }

    public static void main(String[]args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/test?zeroDateTimeBehavior=convertToNull";
        Connection connection = DriverManager.getConnection(url, "jeff", "mypassword");
        Statement stmt = connection.createStatement();
        //        makeTable(stmt);
        //        String s1 = "select * from nids";
        //        String s2 = "select * from nids where station=" + quote("ABC");
        for(int i=0;i<args.length;i++) {
            if(args[i].equals("maketable")) {
                makeTable(stmt);
            } else {
                eval(stmt,args[i]);
            }
        }
        //        makeTable(stmt);
        //        SqlShell sqlShell  = new SqlShell("Test", connection);
       

    }



}


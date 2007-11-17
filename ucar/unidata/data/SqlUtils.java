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



import java.sql.*;
import ucar.unidata.util.Misc;


public class SqlUtils {

    
    public static String makeSelect(String what, String where) {
        return "SELECT " + what + " FROM " + where +";";
    }

    public static  double[] readTime(ResultSet results, int column) throws Exception {
        double [] current = new double[100000];
        int cnt = 0;
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
        System.err.println("Read:" + cnt +" doubles");
        double[] actual = new double[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;


    }


    public static  float[] readFloat(ResultSet results, int column, float missing)
        throws Exception {
        float [] current = new float[100000];
        int cnt = 0;
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
        float[] actual = new float[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }

    public static  String[] readString(ResultSet results, int column)
        throws Exception {
        String [] current = new String[100000];
        int cnt = 0;
        while(results.next()) {
            current[cnt++] = results.getString(column);
            if(cnt>= current.length) {
                String[] tmp = current;
                current = new String[current.length*2];
                System.arraycopy(tmp, 0, current, 0, tmp.length);
            }
        }
        String[] actual = new String[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }


    public static double[] readDouble(ResultSet results, int column, double missing)
        throws Exception {
        double [] current = new double[100000];
        int cnt = 0;
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
        System.err.println("Read:" + cnt +" doubles");
        double[] actual = new double[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }




}


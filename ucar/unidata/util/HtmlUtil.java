/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

// $Id: StringUtil.java,v 1.53 2007/06/01 17:02:44 jeffmc Exp $


package ucar.unidata.util;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import ucar.unidata.xml.XmlUtil;




/**
 * @version $Id: StringUtil.java,v 1.53 2007/06/01 17:02:44 jeffmc Exp $
 */

public class HtmlUtil {

    /**
     * _more_
     *
     * @param sb _more_
     * @param value _more_
     * @param name _more_
     */
    public static String hidden(String name,String value) {
        return "<input type=\"hidden\" name=\"" + name + "\" value=\""
            + value + "\">";
    }


    public static String img(String path) {
        return img(path, "");
    }

    public static String img(String path, String title ) {
        return img(path, title, "");
    }

    public static String img(String path, String title, String extra) {
        return "<img "  + XmlUtil.attrs("border","0", "src",path,"title", title, "alt", title) +" " + extra +">";
    }
    public static String bold(String v1) {
        return "<b>" + v1 +"</b>";
    }

    public static String col(String v1) {
        return "<td>"+ v1 +"</td>";
    }

    public static String row(String v1) {
        return "<tr><td>" + v1 +"</td></tr>";
    }


    public static String row(String v1,String v2) {
        return "<tr><td>" + v1 +"</td><td>" + v2 +"</td></tr>";
    }

    public static String span(String content,String extra) {
        return "<span " + extra+">" + content+"</span>";
    }


    public static String url(String path, String n1, String v1) {
        return url(path, new String[]{n1,v1});
    }


    public static String url(String path, String n1, String v1, String n2, String v2) {
        return url(path, new String[]{n1,v1,n2,v2});
    }

    public static String url(String path, String []args) {
        if(args.length==0) return path;
        path = path+"?";
        for(int i=0;i<args.length;i+=2) {
            if(i>0)
                path = path+"&";
            path = path + args[i]+"="+args[i+1];
        }
        return path;
    }

    public static String makeLatLonBox(String baseName, String lat1, String lat2, String lon1, String lon2) {
        return  "<table>" +
            "<tr><td colspan=\"2\" align=\"center\">" +
            input(baseName+"_lat2", lat2," size=\"10\"") +
            "</td></tr>" +
            "<tr><td>" +
            input(baseName+"_lon2", lat2," size=\"10\"") +
            "</td><td>" +
            input(baseName+"_lon1", lat2," size=\"10\"") +
            "</tr>" +
            "<tr><td colspan=\"2\" align=\"center\">" +
            input(baseName+"_lat1", lat2," size=\"10\"") +
            "</table>";
    }

    public static String checkbox(String name, String value) {
        return checkbox(name, value, false);
    }

    public static String checkbox(String name, String value, boolean   checked) {
        return "<input " + XmlUtil.attrs("type","checkbox","name",name,"value", value)+ (checked?" checked ":"")+">";
    }

    public static String form(String url) {
        return form(url,"");
    }
    public static String form(String url, String extra) {
        return "<form action=\"" + url +"\"" + " " + extra +" >";
    }



    public static String href(String url, String label) {
        return "<a href=\"" + url +"\">" + label +"</a>";
    }

    public static String href(String url, String label, String extra) {
        return "<a href=\"" + url + "\"" + " " + extra+">" + label +"</a>";
    }

    public static String submitImage(String img,String name) {
        return "<input " +XmlUtil.attrs("name", name, "border","0", "src",img) +
            XmlUtil.attrs("type","image") +
            " >";

    }


    public static String submitImage(String img,String name,String alt) {
        return "<input " +XmlUtil.attrs("name", name, "border","0", "src",img) +
            XmlUtil.attrs("title", alt, "alt", alt,"type","image") +
            " >";
    }


    public static String submit(String label,String name) {
        return "<input  name=\""+ name +"\"   type=\"submit\" value=\"" + label +"\" >";
    }

    public static String submit(String label) {
        return "<input  type=\"submit\" value=\"" + label +"\" >";
    }

    public static String textArea(String name, String value, int rows, int columns) {
        return "<textarea name=\"" + name +"\" rows=\"" + rows + "\"  cols=\"" + columns +"\">" + value +"</textarea>";
    }

    public static String input(String name) {
        return input(name,null,"");
    }

    public static String input(String name,Object value) {
        return input(name,value,"");
    }

    public static String input(String name, Object value, String extra) {
        return "<input name=\"" + name +"\" value=\"" + (value==null?"":value.toString()) +"\" " + extra +">";
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param values _more_
     * @param name _more_
     * @param label _more_
     */
    public static String select(String name,List values) {
        StringBuffer sb = new StringBuffer();
        sb.append("<select name=\"" + name + "\">\n");
        for (int i = 0; i < values.size(); i++) {
            Object obj = values.get(i);
            String value;
            String label;
            if(obj instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject) obj;
                value = tfo.getId().toString();
                label  = tfo.toString();
           } else {                value = label = obj.toString();
            }
            sb.append("<option value=\"" + value + "\">" + label
                      + "</option>\n");
        }
        sb.append("</select>\n");
        return sb.toString();
    }

    public static String tableEntry(String left, String right) {
        return " <tr><td align=\"right\">" + left     + "</td><td>" + right +"</td></tr>";

    }


}


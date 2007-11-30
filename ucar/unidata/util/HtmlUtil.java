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
import java.util.List;




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
    public static String makeHidden(String name,String value) {
        return "<input type=\"hidden\" name=\"" + name + "\" value=\""
            + value + "\"/>";
    }


    public static String checkbox(String name, String value) {
        return "<input type=\"checkbox\" name=\"" + name +"    value=\"" + value +"\">";
    }

    public static String form(String url) {
        return "<form action=\"" + url +"\">";
    }

    public static String makeInput(String name,String value) {
        return "<input  name=\"" + name + "\" value=\""+ value + "\"/>";
    }

    public static String href(String url, String label) {
        return "<a href=\"" + url +"\">" + label +"</a>";
    }

    public static String submit(String label) {
        return "<input  type=\"submit\" value=\"" + label +"\" />";
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param values _more_
     * @param name _more_
     * @param label _more_
     */
    public static String makeSelect(String name,List values) {
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
        return sb.toString();
    }

    public static String makeTableEntry(String left, String right) {
        return " <tr><td align=\"right\">" + left     + "</td><td>" + right +"</td></tr>";

    }



}


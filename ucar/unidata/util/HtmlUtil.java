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


import ucar.unidata.xml.XmlUtil;


import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;




/**
 * @version $Id: StringUtil.java,v 1.53 2007/06/01 17:02:44 jeffmc Exp $
 */

public class HtmlUtil {

    //j-

    /** _more_          */
    public static final String SIZE_3 = "  size=\"3\" ";

    /** _more_          */
    public static final String SIZE_5 = "  size=\"5\" ";

    /** _more_          */
    public static final String SIZE_8 = "  size=\"8\" ";

    /** _more_          */
    public static final String SIZE_10 = "  size=\"10\" ";

    /** _more_          */
    public static final String SIZE_20 = "  size=\"20\" ";

    /** _more_          */
    public static final String SIZE_15 = "  size=\"15\" ";

    /** _more_          */
    public static final String SIZE_25 = "  size=\"25\" ";

    /** _more_          */
    public static final String SIZE_30 = "  size=\"30\" ";

    /** _more_          */
    public static final String SIZE_40 = "  size=\"40\" ";

    /** _more_          */
    public static final String SIZE_50 = "  size=\"50\" ";

    /** _more_          */
    public static final String SIZE_60 = "  size=\"60\" ";

    /** _more_          */
    public static final String SIZE_70 = "  size=\"70\" ";

    /** _more_          */
    public static final String SIZE_80 = "  size=\"80\" ";

    /** _more_          */
    public static final String SIZE_90 = "  size=\"90\" ";

    /** _more_          */
    public static final String ENTITY_NBSP = "&nbsp;";

    /** _more_          */
    public static final String ENTITY_GT = "&gt;";

    /** _more_          */
    public static final String ENTITY_LT = "&lt;";

    /** _more_          */
    public static final String TAG_A = "a";

    /** _more_          */
    public static final String TAG_B = "b";

    /** _more_          */
    public static final String TAG_BR = "br";

    /** _more_          */
    public static final String TAG_CENTER = "center";

    /** _more_          */
    public static final String TAG_DIV = "div";

    /** _more_          */
    public static final String TAG_FORM = "form";

    /** _more_          */
    public static final String TAG_HR = "hr";

    /** _more_          */
    public static final String TAG_H1 = "h1";

    /** _more_          */
    public static final String TAG_H2 = "h2";

    /** _more_          */
    public static final String TAG_H3 = "h3";

    /** _more_          */
    public static final String TAG_I = "i";

    /** _more_          */
    public static final String TAG_IMG = "img";

    /** _more_          */
    public static final String TAG_INPUT = "input";

    /** _more_          */
    public static final String TAG_LI = "li";

    /** _more_          */
    public static final String TAG_NOBR = "nobr";

    /** _more_          */
    public static final String TAG_OPTION = "option";

    /** _more_          */
    public static final String TAG_P = "p";

    /** _more_          */
    public static final String TAG_PRE = "pre";

    /** _more_          */
    public static final String TAG_SCRIPT = "script";

    /** _more_          */
    public static final String TAG_SPAN = "span";

    /** _more_          */
    public static final String TAG_SELECT = "select";

    /** _more_          */
    public static final String TAG_TABLE = "table";

    /** _more_          */
    public static final String TAG_TD = "td";

    /** _more_          */
    public static final String TAG_TR = "tr";

    /** _more_          */
    public static final String TAG_TEXTAREA = "textarea";

    /** _more_          */
    public static final String TAG_UL = "ul";


    /** _more_          */
    public static final String ATTR_ACTION = "action";

    /** _more_          */
    public static final String ATTR_ALIGN = "align";

    /** _more_          */
    public static final String ATTR_ALT = "alt";

    /** _more_          */
    public static final String ATTR_BORDER = "border";

    /** _more_          */
    public static final String ATTR_BGCOLOR = "bgcolor";

    /** _more_          */
    public static final String ATTR_CELLSPACING = "cellspacing";

    /** _more_          */
    public static final String ATTR_CELLPADDING = "cellpadding";

    /** _more_          */
    public static final String ATTR_CLASS = "class";

    /** _more_          */
    public static final String ATTR_COLS = "cols";

    /** _more_          */
    public static final String ATTR_COLSPAN = "colspan";

    /** _more_          */
    public static final String ATTR_ENCTYPE = "enctype";

    /** _more_          */
    public static final String ATTR_HREF = "href";

    /** _more_          */
    public static final String ATTR_ID = "id";

    /** _more_          */
    public static final String ATTR_METHOD = "method";

    /** _more_          */
    public static final String ATTR_NAME = "name";

    /** _more_          */
    public static final String ATTR_ONMOUSEMOVE = "onmousemove";

    /** _more_          */
    public static final String ATTR_ONMOUSEOVER = "onmouseover";

    /** _more_          */
    public static final String ATTR_ONMOUSEUP = "onmouseup";

    /** _more_          */
    public static final String ATTR_ONMOUSEOUT = "onmouseout";

    /** _more_          */
    public static final String ATTR_ONMOUSEDOWN = "onmousedown";

    /** _more_          */
    public static final String ATTR_ONCLICK = "onClick";

    /** _more_          */
    public static final String ATTR_READONLY = "READONLY";

    /** _more_          */
    public static final String ATTR_ROWS = "rows";

    /** _more_          */
    public static final String ATTR_SELECTED = "selected";

    /** _more_          */
    public static final String ATTR_SIZE = "size";

    /** _more_          */
    public static final String ATTR_SRC = "src";

    /** _more_          */
    public static final String ATTR_STYLE = "style";

    /** _more_          */
    public static final String ATTR_TYPE = "type";

    /** _more_          */
    public static final String ATTR_TITLE = "title";

    /** _more_          */
    public static final String ATTR_VALUE = "value";

    /** _more_          */
    public static final String ATTR_VALIGN = "valign";

    /** _more_          */
    public static final String ATTR_WIDTH = "width";

    /** _more_          */
    public static final String CLASS_BLOCK = "block";

    /** _more_          */
    public static final String CLASS_CHECKBOX = "checkbox";

    /** _more_          */
    public static final String CLASS_DISABLEDINPUT = "disabledinput";

    /** _more_          */
    public static final String CLASS_ERRORLABEL = "errorlabel";

    /** _more_          */
    public static final String CLASS_FILEINPUT = "fileinput";

    /** _more_          */
    public static final String CLASS_FORMLABEL = "formlabel";

    /** _more_          */
    public static final String CLASS_HIDDENINPUT = "hiddeninput";

    /** _more_          */
    public static final String CLASS_INPUT = "input";

    /** _more_          */
    public static final String CLASS_PASSWORD = "password";

    /** _more_          */
    public static final String CLASS_RADIO = "radio";

    /** _more_          */
    public static final String CLASS_SELECT = "select";

    /** _more_          */
    public static final String CLASS_SUBMIT = "submit";

    /** _more_          */
    public static final String CLASS_SUBMITIMAGE = "submitimage";

    /** _more_          */
    public static final String CLASS_TAB_CONTENT = "tab_content";

    /** _more_          */
    public static final String CLASS_TAB_CONTENTS = "tab_contents";

    /** _more_          */
    public static final String CLASS_TEXTAREA = "textarea";



    /** _more_          */
    public static final String TYPE_FILE = "file";

    /** _more_          */
    public static final String TYPE_PASSWORD = "password";

    /** _more_          */
    public static final String TYPE_SUBMIT = "submit";

    /** _more_          */
    public static final String TYPE_IMAGE = "image";

    /** _more_          */
    public static final String TYPE_RADIO = "radio";

    /** _more_          */
    public static final String TYPE_INPUT = "input";

    /** _more_          */
    public static final String TYPE_TEXTAREA = "textarea";

    /** _more_          */
    public static final String TYPE_CHECKBOX = "checkbox";

    /** _more_          */
    public static final String TYPE_HIDDEN = "hidden";



    /** _more_          */
    public static final String VALUE_BOTTOM = "bottom";

    /** _more_          */
    public static final String VALUE_CENTER = "center";

    /** _more_          */
    public static final String VALUE_FALSE = "false";

    /** _more_          */
    public static final String VALUE_LEFT = "left";

    /** _more_          */
    public static final String VALUE_MULTIPART = "multipart/form-data";

    /** _more_          */
    public static final String VALUE_POST = "post";

    /** _more_          */
    public static final String VALUE_RIGHT = "right";

    /** _more_          */
    public static final String VALUE_SELECTED = "selected";

    /** _more_          */
    public static final String VALUE_TOP = "top";

    /** _more_          */
    public static final String VALUE_TRUE = "true";

    //j+


    /**
     * _more_
     *
     * @param comp _more_
     *
     * @return _more_
     */
    public static String open(String comp) {
        return "<" + comp + ">";
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public static String comma(String s1, String s2) {
        return comma(new String[] { s1, s2 });
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     *
     * @return _more_
     */
    public static String comma(String s1, String s2, String s3) {
        return comma(new String[] { s1, s2, s3 });
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     *
     * @return _more_
     */
    public static String comma(String s1, String s2, String s3, String s4) {
        return comma(new String[] { s1, s2, s3, s4 });
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     * @param s5 _more_
     *
     * @return _more_
     */
    public static String comma(String s1, String s2, String s3, String s4,
                               String s5) {
        return comma(new String[] { s1, s2, s3, s4, s5 });
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String comma(String[] s) {
        return StringUtil.join(",", s);
    }


    /**
     * _more_
     *
     * @param comp _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String open(String comp, String attrs) {
        return "<" + comp + attrs + ">";
    }

    /**
     * _more_
     *
     * @param comp _more_
     *
     * @return _more_
     */
    public static String close(String comp) {
        return "</" + comp + ">";
    }

    /**
     * _more_
     *
     * @param comp _more_
     *
     * @return _more_
     */
    public static String tag(String comp) {
        return "<" + comp + "/>";
    }


    /**
     * _more_
     *
     * @param comp _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String tag(String comp, String attrs) {
        return "<" + comp + attrs + "/>";
    }


    /**
     * _more_
     *
     * @param tag _more_
     * @param attrs _more_
     * @param inner _more_
     *
     * @return _more_
     */
    public static String tag(String tag, String attrs, String inner) {
        return open(tag, attrs) + inner + close(tag);
    }


    /**
     * _more_
     *
     * @param value _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static String hidden(String name, Object value) {
        return hidden(name, value, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String hidden(String name, Object value, String extra) {
        return tag(TAG_INPUT,
                   extra
                   + attrs(ATTR_TYPE, TYPE_HIDDEN, ATTR_NAME, name,
                           ATTR_VALUE, "" + value, ATTR_CLASS, CLASS_HIDDENINPUT));
    }



    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public static String hbox(String s1, String s2) {
        return tag(TAG_TABLE,
                   attrs(ATTR_CELLSPACING, "0", ATTR_CELLPADDING, "0"),
                   HtmlUtil.rowTop(HtmlUtil.cols(s1, s2)));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String br() {
        return open(TAG_BR);
    }

    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     */
    public static String br(String line) {
        return line + open(TAG_BR);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String hr() {
        return open(TAG_HR);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String p() {
        return open(TAG_P);
    }

    /**
     * _more_
     *
     * @param inner _more_
     *
     * @return _more_
     */
    public static String nobr(String inner) {
        return tag(TAG_NOBR, "", inner);
    }


    /**
     * _more_
     *
     * @param inner _more_
     *
     * @return _more_
     */
    public static String b(String inner) {
        return tag(TAG_B, "", inner);
    }

    /**
     * _more_
     *
     * @param inner _more_
     *
     * @return _more_
     */
    public static String italics(String inner) {
        return tag(TAG_I, "", inner);
    }

    /**
     * _more_
     *
     * @param inner _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String li(String inner, String extra) {
        return tag(TAG_LI, extra, inner);
    }


    /**
     * _more_
     *
     * @param inner _more_
     *
     * @return _more_
     */
    public static String center(String inner) {
        return tag(TAG_CENTER, "", inner);
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String pad(String s) {
        return space(1) + s + space(1);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String padLeft(String s) {
        return space(1) + s;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String padRight(String s) {
        return s + space(1);
    }


    /**
     * _more_
     *
     * @param cnt _more_
     *
     * @return _more_
     */
    public static String space(int cnt) {
        String s = "";
        while (cnt-- > 0) {
            s = s + ENTITY_NBSP;
        }
        return s;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String quote(String s) {
        return "\"" + s + "\"";
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String squote(String s) {
        return "'" + s + "'";
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public static String img(String path) {
        return img(path, "");
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param title _more_
     *
     * @return _more_
     */
    public static String img(String path, String title) {
        return img(path, title, "");
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param title _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String img(String path, String title, String extra) {
        if (title.length() > 0) {
            return tag(TAG_IMG,
                       attrs(ATTR_BORDER, "0", ATTR_SRC, path, ATTR_TITLE,
                             title, ATTR_ALT, title) + " " + extra);
        }
        return tag(TAG_IMG,
                   attrs(ATTR_BORDER, "0", ATTR_SRC, path) + " " + extra);
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String cssClass(String c) {
        return attr(ATTR_CLASS, c);
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String title(String c) {
        return attr(ATTR_TITLE, c);
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String id(String c) {
        return attr(ATTR_ID, c);
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String style(String c) {
        return attr(ATTR_STYLE, c);
    }

    /**
     * _more_
     *
     * @param v1 _more_
     *
     * @return _more_
     */
    public static String bold(String v1) {
        return tag(TAG_B, "", v1);
    }

    /**
     * _more_
     *
     * @param v1 _more_
     *
     * @return _more_
     */
    public static String col(String v1) {
        return col(v1, "");
    }

    /**
     * _more_
     *
     * @param v1 _more_
     * @param attr _more_
     *
     * @return _more_
     */
    public static String col(String v1, String attr) {
        return tag(TAG_TD, " " + attr + " ", v1);
    }


    /**
     * _more_
     *
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String span(String content, String extra) {
        return tag(TAG_SPAN, extra, content);
    }

    /**
     * _more_
     *
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String div(String content, String extra) {
        return tag(TAG_DIV, extra, content);
    }

    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String pre(String content) {
        return tag(TAG_PRE, "", content);
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param n1 _more_
     * @param v1 _more_
     *
     * @return _more_
     */
    public static String url(String path, String n1, Object v1) {
        return url(path, new String[] { n1, v1.toString() });
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param n1 _more_
     * @param v1 _more_
     * @param n2 _more_
     * @param v2 _more_
     *
     * @return _more_
     */
    public static String url(String path, String n1, Object v1, String n2,
                             Object v2) {
        return url(path,
                   new String[] { n1, v1.toString(), n2, v2.toString() });
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param n1 _more_
     * @param v1 _more_
     * @param n2 _more_
     * @param v2 _more_
     * @param n3 _more_
     * @param v3 _more_
     *
     * @return _more_
     */
    public static String url(String path, String n1, Object v1, String n2,
                             Object v2, String n3, Object v3) {
        return url(path, new String[] {
            n1, v1.toString(), n2, v2.toString(), n3, v3.toString()
        });
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param n1 _more_
     * @param v1 _more_
     * @param n2 _more_
     * @param v2 _more_
     * @param n3 _more_
     * @param v3 _more_
     * @param n4 _more_
     * @param v4 _more_
     *
     * @return _more_
     */
    public static String url(String path, String n1, Object v1, String n2,
                             Object v2, String n3, Object v3, String n4,
                             Object v4) {
        return url(path, new String[] {
            n1, v1.toString(), n2, v2.toString(), n3, v3.toString(), n4,
            v4.toString()
        });
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String url(String path, List args) {
        return url(path, Misc.listToStringArray(args));
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String url(String path, String[] args) {
        if (args.length == 0) {
            return path.toString();
        }
        boolean addAmpersand = false;
        String  url          = path.toString();
        if (url.indexOf("?") >= 0) {
            if ( !url.endsWith("?")) {
                addAmpersand = true;
            }
        } else {
            url = url + "?";
        }

        for (int i = 0; i < args.length; i += 2) {
            if (addAmpersand) {
                url = url + "&";
            }
            try {
                url = url + arg(args[i], args[i + 1]);
            } catch (Exception exc) {
                System.err.println("error encoding arg:" + args[i + 1] + " "
                                   + exc);
            }
            addAmpersand = true;
        }
        return url;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static String args(String[] args) {
        List<String> a = new ArrayList<String>();
        for (int i = 0; i < args.length; i += 2) {
            a.add(arg(args[i], args[i + 1]));
        }
        return StringUtil.join("&", a);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String arg(String name, String value) {
        try {
            return name + "=" + java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception exc) {
            System.err.println("error encoding arg:" + value + " " + exc);
            return "";
        }
    }


    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     */
    public static String row(String row) {
        return tag(TAG_TR, "", row);
    }

    /**
     * _more_
     *
     * @param row _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String row(String row, String extra) {
        return tag(TAG_TR, extra, row);
    }

    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     */
    public static String rowTop(String row) {
        return tag(TAG_TR, attr(ATTR_VALIGN, VALUE_TOP), row);
    }

    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     */
    public static String rowBottom(String row) {
        return tag(TAG_TR, attr(ATTR_VALIGN, VALUE_BOTTOM), row);
    }


    /**
     * _more_
     *
     * @param s1 _more_
     *
     * @return _more_
     */
    public static String cols(String s1) {
        return tag(TAG_TD, "", s1);
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public static String cols(String s1, String s2) {
        return cols(s1) + cols(s2);
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     *
     * @return _more_
     */
    public static String cols(String s1, String s2, String s3) {
        return cols(s1) + cols(s2) + cols(s3);
    }


    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     *
     * @return _more_
     */
    public static String cols(String s1, String s2, String s3, String s4) {
        return cols(s1) + cols(s2) + cols(s3) + cols(s4);
    }




    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     * @param s5 _more_
     *
     * @return _more_
     */
    public static String cols(String s1, String s2, String s3, String s4,
                              String s5) {
        return cols(s1) + cols(s2) + cols(s3) + cols(s4) + cols(s5);
    }


    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     * @param s5 _more_
     * @param s6 _more_
     *
     * @return _more_
     */
    public static String cols(String s1, String s2, String s3, String s4,
                              String s5, String s6) {
        return cols(s1) + cols(s2) + cols(s3) + cols(s4) + cols(s5)
               + cols(s6);
    }

    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static String cols(Object[] columns) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < columns.length; i++) {
            sb.append(cols(columns[i].toString()));
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param baseName _more_
     * @param south _more_
     * @param north _more_
     * @param east _more_
     * @param west _more_
     *
     * @return _more_
     */
    public static String makeLatLonBox(String baseName, String south,
                                       String north, String east,
                                       String west) {
        //TODO: Clean this up
        return table(
            "<tr><td colspan=\"2\" align=\"center\">"
            + input(
                baseName + "_north", north,
                " size=\"5\" id=\"" + baseName + "_north\"") + "</td></tr>"
                    + "<tr><td>"
                    + input(
                        baseName + "_west", west,
                        " size=\"5\" id=\"" + baseName
                        + "_west\"") + "</td><td>"
                                     + input(
                                         baseName + "_east", east,
                                         " size=\"5\"  id=\"" + baseName
                                         + "_east\"") + "</tr>"
                                             + "<tr><td colspan=\"2\" align=\"center\">"
                                             + input(
                                                 baseName + "_south", south,
                                                     " size=\"5\"  id=\""
                                                         + baseName
                                                             + "_south\""));
    }

    /**
     * _more_
     *
     * @param baseName _more_
     * @param south _more_
     * @param north _more_
     * @param east _more_
     * @param west _more_
     *
     * @return _more_
     */
    public static String makeLatLonBox(String baseName, double south,
                                       double north, double east,
                                       double west) {
        return table(
            "<tr><td colspan=\"2\" align=\"center\">"
            + input(baseName + "_north", toString(north), " size=\"5\"")
            + "</td></tr>" + "<tr><td>"
            + input(baseName + "_west", toString(west), " size=\"5\"")
            + "</td><td>"
            + input(baseName + "_east", toString(east), " size=\"5\"")
            + "</tr>" + "<tr><td colspan=\"2\" align=\"center\">"
            + input(baseName + "_south", toString(south), " size=\"5\""));
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    private static String toString(double v) {
        if (v == v) {
            return "" + v;
        }
        return "";
    }

    /**
     * _more_
     *
     * @param south _more_
     * @param north _more_
     * @param east _more_
     * @param west _more_
     *
     * @return _more_
     */
    public static String makeAreaLabel(double south, double north,
                                       double east, double west) {
        return table("<tr><td colspan=\"2\" align=\"center\">"
                     + toString(north) + "</td></tr>" + "<tr><td>"
                     + toString(west) + "</td><td>" + toString(east)
                     + "</tr>" + "<tr><td colspan=\"2\" align=\"center\">"
                     + toString(south));
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String checkbox(String name) {
        return checkbox(name, VALUE_TRUE, false);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String checkbox(String name, String value) {
        return checkbox(name, value, false);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     *
     * @return _more_
     */
    public static String radio(String name, String value, boolean checked) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_RADIO, ATTR_TYPE, TYPE_RADIO,
                         ATTR_NAME, name, ATTR_VALUE, value) + (checked
                ? " checked "
                : ""));
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     *
     * @return _more_
     */
    public static String checkbox(String name, String value,
                                  boolean checked) {
        return checkbox(name, value, checked, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String checkbox(String name, String value, boolean checked,
                                  String extra) {
        return tag(TAG_INPUT,
                   extra
                   + attrs(ATTR_CLASS, CLASS_CHECKBOX, ATTR_TYPE,
                           TYPE_CHECKBOX, ATTR_NAME, name, ATTR_VALUE,
                           value) + (checked
                                     ? " checked "
                                     : ""));
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public static String form(String url) {
        return form(url, "");
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String form(String url, String extra) {
        return open(TAG_FORM, attr(ATTR_ACTION, url) + " " + extra);
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public static String formPost(String url) {
        return open(TAG_FORM,
                    attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url));
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String formPost(String url, String extra) {
        return open(TAG_FORM,
                    attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url) + " "
                    + extra);
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String uploadForm(String url, String extra) {
        return open(TAG_FORM,
                    attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url,
                          ATTR_ENCTYPE, VALUE_MULTIPART) + " " + extra);
    }



    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     *
     * @return _more_
     */
    public static String href(String url, String label) {
        return tag(TAG_A, attrs(ATTR_HREF, url), label);
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
    public static String href(String url, String label, String extra) {
        return tag(TAG_A, attrs(ATTR_HREF, url) + " " + extra, label);
    }

    /**
     * _more_
     *
     * @param img _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static String submitImage(String img, String name) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_SUBMITIMAGE, ATTR_NAME, name)
                   + attrs(ATTR_BORDER, "0", ATTR_SRC, img, ATTR_TYPE,
                           TYPE_IMAGE));

    }


    /**
     * _more_
     *
     * @param img _more_
     * @param name _more_
     * @param alt _more_
     *
     * @return _more_
     */
    public static String submitImage(String img, String name, String alt) {
        return tag(TAG_INPUT,
                   attrs(ATTR_NAME, name, ATTR_BORDER, "0", ATTR_SRC, img)
                   + attrs(ATTR_CLASS, CLASS_SUBMITIMAGE, ATTR_TITLE, alt,
                           ATTR_ALT, alt, ATTR_TYPE, TYPE_IMAGE));
    }


    /**
     * _more_
     *
     * @param label _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static String submit(String label, String name) {
        return tag(TAG_INPUT,
                   attrs(ATTR_NAME, name, ATTR_TYPE, TYPE_SUBMIT, ATTR_VALUE,
                         label, ATTR_CLASS, CLASS_SUBMIT));
    }

    /**
     * _more_
     *
     * @param label _more_
     *
     * @return _more_
     */
    public static String submit(String label) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_SUBMIT, ATTR_TYPE, TYPE_SUBMIT,
                         ATTR_VALUE, label));
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param rows _more_
     * @param columns _more_
     *
     * @return _more_
     */
    public static String textArea(String name, String value, int rows,
                                  int columns) {

        return textArea(name, value, rows, columns, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param rows _more_
     * @param columns _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String textArea(String name, String value, int rows,
                                  int columns, String extra) {
        return tag(TAG_TEXTAREA,
                   attrs(ATTR_NAME, name, ATTR_CLASS, CLASS_TEXTAREA)
                   + attrs(ATTR_ROWS, "" + rows, ATTR_COLS, "" + columns)
                   + extra, value);
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String password(String name) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_PASSWORD, ATTR_TYPE,
                         TYPE_PASSWORD, ATTR_NAME, name));
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String input(String name) {
        return input(name, null, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String input(String name, Object value) {
        return input(name, value, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String input(String name, Object value, String extra) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_INPUT, ATTR_NAME, name,
                         ATTR_VALUE, ((value == null)
                                      ? ""
                                      : value.toString())) + " " + extra);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String disabledInput(String name, Object value,
                                       String extra) {
        return tag(TAG_INPUT,
                   " " + ATTR_READONLY + " "
                   + attrs(ATTR_CLASS, CLASS_DISABLEDINPUT, ATTR_NAME, name,
                           ATTR_VALUE, ((value == null)
                                        ? ""
                                        : value.toString())) + " " + extra);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String fileInput(String name, String extra) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_FILEINPUT, ATTR_TYPE, TYPE_FILE,
                         ATTR_NAME, name) + " " + extra);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     *
     * @return _more_
     */
    public static String select(String name, List values) {
        return select(name, values, null);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     *
     * @return _more_
     */
    public static String select(String name, List values, String selected) {
        return select(name, values, selected, Integer.MAX_VALUE);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String select(String name, List values, String selected,
                                String extra) {
        return select(name, values, selected, extra, Integer.MAX_VALUE);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String select(String name, List values,
                                List<String> selected, String extra) {
        return select(name, values, selected, extra, Integer.MAX_VALUE);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * ,     * @param maxLength _more_
     *
     * @return _more_
     */
    public static String select(String name, String[] values,
                                String selected, int maxLength) {
        return select(name, Misc.toList(values), selected, maxLength);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param maxLength _more_
     *
     * @return _more_
     */
    public static String select(String name, List values, String selected,
                                int maxLength) {
        return select(name, values, selected, "", maxLength);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     * @param maxLength _more_
     *
     * @return _more_
     */
    public static String select(String name, List values, String selected,
                                String extra, int maxLength) {
        List<String> selectedList = null;
        if ((selected != null) && (selected.length() > 0)) {
            selectedList = (List<String>) Misc.newList(selected);
        }
        return select(name, values, selectedList, extra, maxLength);
    }

    /**
     * Class Selector _more_
     *
     *
     * @author IDV Development Team
     */
    public static class Selector {

        /** _more_          */
        String label;

        /** _more_          */
        String id;

        /** _more_          */
        String icon;

        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         * @param icon _more_
         */
        public Selector(String label, String id, String icon) {
            this.label = label;
            this.id    = id;
            this.icon  = icon;
        }
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     * @param maxLength _more_
     *
     * @return _more_
     */
    public static String select(String name, List values,
                                List<String> selected, String extra,
                                int maxLength) {
        StringBuffer sb = new StringBuffer();
        sb.append(open(TAG_SELECT,
                       attrs(ATTR_NAME, name, ATTR_CLASS, CLASS_SELECT)
                       + extra));
        for (int i = 0; i < values.size(); i++) {
            Object obj = values.get(i);
            String value;
            String label;
            String extraAttr = "";
            if (obj instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject) obj;
                value = tfo.getId().toString();
                label = tfo.toString();
            } else if (obj instanceof Selector) {
                Selector selector = (Selector) obj;
                value = selector.id;
                label = selector.label;
                if (selector.icon != null) {
                    extraAttr = style(
                        "margin:3px;padding-left:20px;background-repeat:no-repeat; background-image: url("
                        + selector.icon + ");");
                }
            } else {
                value = label = obj.toString();
            }

            String selectedAttr = "";
            if ((selected != null) && selected.contains(value)) {
                selectedAttr = attrs(ATTR_SELECTED, VALUE_SELECTED);
            }
            if (label.length() > maxLength) {
                label = "..." + label.substring(label.length() - maxLength);
            }

            sb.append(tag(TAG_OPTION,
                          selectedAttr + extraAttr
                          + attrs(ATTR_TITLE, value, ATTR_VALUE,
                                  value), label));
        }
        sb.append(close(TAG_SELECT));
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param html _more_
     * @param top _more_
     * @param left _more_
     * @param bottom _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String inset(String html, int top, int left, int bottom,
                               int right) {
        return span(html, style(((top == 0)
                                 ? ""
                                 : "margin-top:" + top + "px;") + ((left == 0)
                ? ""
                : "margin-left:" + left + "px;") + ((bottom == 0)
                ? ""
                : "margin-bottom:" + bottom + "px;") + ((right == 0)
                ? ""
                : "margin-right:" + top + "px;")));
    }


    /**
     * _more_
     *
     * @param html _more_
     * @param space _more_
     *
     * @return _more_
     */
    public static String inset(String html, int space) {
        return div(html, style("margin:" + space + "px;"));
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param cols _more_
     *
     * @return _more_
     */
    public static String colspan(String s, int cols) {
        return tag(TAG_TD, attr(ATTR_COLSPAN, "" + cols), s);
    }

    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
    public static String formTableTop(String[] cols) {
        StringBuffer sb = new StringBuffer();
        sb.append(formTable());

        for (int i = 0; i < cols.length; i += 2) {
            sb.append(formEntryTop(cols[i], cols[i + 1]));
        }
        sb.append(formTableClose());
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
    public static String formEntryTop(String[] cols) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < cols.length; i += 2) {
            sb.append(formEntryTop(cols[i], cols[i + 1]));
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String leftRight(String left, String right) {
        return tag(TAG_TABLE,
                   attrs(ATTR_WIDTH, "100%", ATTR_CELLPADDING, "0",
                         ATTR_CELLSPACING,
                         "0"), row(col(left)
                         + col(right,
                               attr(ATTR_ALIGN,
                                    VALUE_RIGHT)), attr(ATTR_VALIGN,
                                        VALUE_TOP)));
    }

    /**
     * _more_
     *
     * @param contents _more_
     *
     * @return _more_
     */
    public static String table(String contents) {
        return table(contents,
                     attrs(ATTR_CELLPADDING, "0", ATTR_CELLSPACING, "0"));
    }

    /**
     * _more_
     *
     * @param contents _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String table(String contents, String attrs) {
        return tag(TAG_TABLE, attrs, contents);
    }

    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static String table(Object[] columns) {
        return table(row(cols(columns), attr(ATTR_VALIGN, VALUE_TOP)));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static String formTable() {
        return open(TAG_TABLE,
                    attrs(ATTR_CELLPADDING, "3", ATTR_CELLSPACING, "3"));
    }


    /**
     * _more_
     *
     * @param extra _more_
     *
     * @return _more_
     */
    public static String formTable(String extra) {
        return open(TAG_TABLE,
                    attrs(ATTR_CELLPADDING, "5", ATTR_CELLSPACING, "5") + " "
                    + extra);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String formTableClose() {
        return close(TAG_TABLE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String formClose() {
        return close(TAG_FORM);
    }


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String formEntry(String left, String right) {
        return tag(TAG_TR, "",
                   tag(TAG_TD,
                       attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                             CLASS_FORMLABEL), left) + tag(TAG_TD, "",
                                 right));

    }


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String formEntryTop(String left, String right) {
        return formEntryTop(left, right, "", true);
    }


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     * @param trExtra _more_
     * @param dummy _more_
     *
     * @return _more_
     */
    public static String formEntryTop(String left, String right,
                                      String trExtra, boolean dummy) {
        return tag(TAG_TR, attrs(ATTR_VALIGN, VALUE_TOP) + " " + trExtra,
                   tag(TAG_TD,
                       attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_VALIGN, VALUE_TOP,
                             ATTR_CLASS,
                             CLASS_FORMLABEL), left) + tag(TAG_TD, "",
                                 right));
    }

    /**
     * _more_
     *
     * @param col1 _more_
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String formEntryTop(String col1, String left,
                                      String right) {
        return tag(TAG_TR, attrs(ATTR_VALIGN, VALUE_TOP),
                   col(col1, attr(ATTR_VALIGN, VALUE_TOP))
                   + col(left,
                         attrs(ATTR_VALIGN, VALUE_TOP, ATTR_ALIGN,
                               VALUE_RIGHT, ATTR_CLASS,
                               CLASS_FORMLABEL)) + col(right));
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String attr(String name, String value) {
        return " " + name + "=" + quote(value) + " ";
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String attrs(String name, String value) {
        return " " + name + "=" + quote(value) + " ";
    }


    /**
     *  Return a String with n1=&quot;v1&quot n2=&quot;v2&quot.
     *
     *  @param n1 The first attribute name.
     *  @param v1 The first attribute value.
     *  @param n2 The second attribute name.
     *  @param v2 The second attribute value.
     *  @return The attrbiute string.
     */

    public static String attrs(String n1, String v1, String n2, String v2) {
        return attr(n1, v1) + attr(n2, v2);
    }

    /**
     *  Return a String with n1=&quot;v1&quot n2=&quot;v2&quot.  n3=&quot;v3&quot.
     *
     *  @param n1 The first attribute name.
     *  @param v1 The first attribute value.
     *  @param n2 The second attribute name.
     *  @param v2 The second attribute value.
     *  @param n3 The third attribute name.
     *  @param v3 The third attribute value.
     *  @return The attrbiute string.
     */

    public static String attrs(String n1, String v1, String n2, String v2,
                               String n3, String v3) {
        return attrs(n1, v1, n2, v2) + attr(n3, v3);
    }



    /**
     *  Return a String with n1=&quot;v1&quot n2=&quot;v2&quot.  n3=&quot;v3&quot.  n4=&quot;v4&quot.
     *
     *  @param n1 The first attribute name.
     *  @param v1 The first attribute value.
     *  @param n2 The second attribute name.
     *  @param v2 The second attribute value.
     *  @param n3 The third attribute name.
     *  @param v3 The third attribute value.
     *  @param n4 The fourth attribute name.
     *  @param v4 The fourth attribute value.
     *  @return The attrbiute string.
     */

    public static String attrs(String n1, String v1, String n2, String v2,
                               String n3, String v3, String n4, String v4) {
        return attrs(n1, v1, n2, v2, n3, v3) + attr(n4, v4);
    }



    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseOver(String call) {
        return attrs(ATTR_ONMOUSEOVER, call);
    }

    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseMove(String call) {
        return attrs(ATTR_ONMOUSEMOVE, call);
    }


    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseOut(String call) {
        return attrs(ATTR_ONMOUSEOUT, call);
    }

    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseUp(String call) {
        return attrs(ATTR_ONMOUSEUP, call);
    }

    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseDown(String call) {
        return attrs(ATTR_ONMOUSEDOWN, call);
    }


    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseClick(String call) {
        return attrs(ATTR_ONCLICK, call);
    }


    /**
     * _more_
     *
     * @param call _more_
     * @param label _more_
     *
     * @return _more_
     */
    public static String mouseClickHref(String call, String label) {
        return mouseClickHref(call, label, "");
    }

    /**
     * _more_
     *
     * @param call _more_
     * @param label _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String mouseClickHref(String call, String label,
                                        String extra) {
        //        return "<a href=\"javascript:void(0)\" " +onMouseClick(call) +">" +label +"</a>";
        String result = tag(TAG_A,
                            attrs(ATTR_HREF, "javascript:void(0);")
                            + onMouseClick(call) + extra, label);
        //        System.err.println(result);

        return result;
    }



    /**
     * _more_
     *
     * @param events _more_
     * @param content _more_
     *
     * @return _more_
     */
    public static String jsLink(String events, String content) {
        return jsLink(events, content, "");
    }


    /**
     * _more_
     *
     * @param events _more_
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String jsLink(String events, String content, String extra) {
        return tag(TAG_A,
                   attrs(ATTR_HREF, "javascript:noop();") + " " + events
                   + " " + extra, content);
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String script(String s) {
        return tag(TAG_SCRIPT, attrs(ATTR_TYPE, "text/JavaScript"), s);
    }


    /**
     * _more_
     *
     * @param function _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String call(String function, String args) {
        return function + "(" + args + ");";
    }

    /**
     * _more_
     *
     * @param function _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String callln(String function, String args) {
        return function + "(" + args + ");\n";
    }

    /**
     * _more_
     *
     * @param jsUrl _more_
     *
     * @return _more_
     */
    public static String importJS(String jsUrl) {
        return tag(TAG_SCRIPT, attr(ATTR_SRC, jsUrl), "");
    }




    /** _more_ */
    static int tabCnt = 0;

    /**
     * _more_
     *
     * @param titles _more_
     * @param contents _more_
     * @param skipEmpty _more_
     *
     * @return _more_
     */
    public static String makeTabs(List titles, List contents,
                                  boolean skipEmpty) {
        return makeTabs(titles, contents, skipEmpty, CLASS_TAB_CONTENT);
    }

    /**
     * _more_
     *
     * @param titles _more_
     * @param contents _more_
     * @param skipEmpty _more_
     * @param tabContentClass _more_
     *
     * @return _more_
     */
    public static String makeTabs(List titles, List contents,
                                  boolean skipEmpty, String tabContentClass) {
        return makeTabs(titles, contents, skipEmpty, tabContentClass,
                        CLASS_TAB_CONTENTS);
    }

    /**
     * _more_
     *
     * @param titles _more_
     * @param contents _more_
     * @param skipEmpty _more_
     * @param tabContentClass _more_
     * @param wrapperClass _more_
     *
     * @return _more_
     */
    public static String makeTabs(List titles, List contents,
                                  boolean skipEmpty, String tabContentClass,
                                  String wrapperClass) {

        String       id        = "tab_" + (tabCnt++);
        String       ids       = "tab_" + (tabCnt++) + "_ids";
        StringBuffer titleSB   = new StringBuffer("");
        StringBuffer contentSB = new StringBuffer();
        StringBuffer idArray   = new StringBuffer("new Array(");
        int          cnt       = 0;
        for (int i = 0; i < titles.size(); i++) {
            String content = contents.get(i).toString();
            if (skipEmpty && (content.length() == 0)) {
                continue;
            }

            String tabId = id + "_" + i;
            if (cnt > 0) {
                idArray.append(",");
            }
            cnt++;
            idArray.append(HtmlUtil.squote(tabId));
        }
        if ((cnt == 1) && skipEmpty) {
            return contents.get(0).toString();
        }

        idArray.append(")");

        boolean didone = false;
        for (int i = 0; i < titles.size(); i++) {
            String content = contents.get(i).toString();
            if (skipEmpty && (content.length() == 0)) {
                continue;
            }
            String title = titles.get(i).toString();
            String tabId = id + "_" + i;
            contentSB.append("\n");
            contentSB.append(HtmlUtil.div(content,
                                          HtmlUtil.cssClass(tabContentClass
                                              + ( !didone
                    ? "_on"
                    : "_off")) + HtmlUtil.id("content_" + tabId)
                               + HtmlUtil.style("display:" + ( !didone
                    ? "block"
                    : "none") + ";visibility:" + ( !didone
                    ? "visible"
                    : "hidden"))));
            String link = HtmlUtil.href("javascript:" + "tabPress("
                                        + HtmlUtil.squote(id) + "," + idArray
                                        + "," + HtmlUtil.squote(tabId)
                                        + ")", title);
            titleSB.append(HtmlUtil.span(link, (didone
                    ? HtmlUtil.cssClass("tab_title_off")
                    : HtmlUtil.cssClass("tab_title_on")) + HtmlUtil.id(
                        "title_" + tabId)));
            didone = true;
        }

        return HtmlUtil.div(
            titleSB.toString(),
            HtmlUtil.cssClass("tab_titles")) + HtmlUtil.div(
                contentSB.toString(), HtmlUtil.cssClass(wrapperClass));
    }


    /** _more_ */
    private static String blockHideImageUrl;

    /** _more_ */
    private static String blockShowImageUrl;

    /**
     * _more_
     *
     * @param hideImg _more_
     * @param showImg _more_
     */
    public static void setHideShowImage(String hideImg, String showImg) {
        blockHideImageUrl = hideImg;
        blockShowImageUrl = showImg;
    }




    /**
     * _more_
     *
     * @param id _more_
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible) {
        return makeShowHideBlock(label, content, visible,
                                 cssClass("pagesubheading"));
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     * @param headerExtra _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra) {
        return HtmlUtil.makeShowHideBlock(label, content, visible,
                                          headerExtra,
                                          HtmlUtil.cssClass(CLASS_BLOCK),
                                          blockHideImageUrl,
                                          blockShowImageUrl);
    }





    /**
     * _more_
     *
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     * @param headerExtra _more_
     * @param blockExtra _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra,
                                           String blockExtra) {
        return HtmlUtil.makeShowHideBlock(label, content, visible,
                                          headerExtra, blockExtra,
                                          blockHideImageUrl,
                                          blockShowImageUrl);
    }





    /** _more_ */
    public static int blockCnt = 0;


    /**
     * _more_
     *
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     * @param headerExtra _more_
     * @param blockExtra _more_
     * @param hideImg _more_
     * @param showImg _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra,
                                           String blockExtra, String hideImg,
                                           String showImg) {
        String       id  = "block_" + (blockCnt++);
        StringBuffer sb  = new StringBuffer();
        String       img = "";
        if ((showImg != null) && (showImg.length() > 0)) {
            img = HtmlUtil.img(visible
                               ? hideImg
                               : showImg, "",
                                          " id='" + id
                                          + "img' ") + HtmlUtil.space(1);
        }
        String link =
            HtmlUtil.jsLink(HtmlUtil.onMouseClick("toggleBlockVisibility('"
                + id + "','" + id + "img','" + hideImg + "','" + showImg
                + "')"), img + label,
                         HtmlUtil.cssClass("pagesubheadinglink"));

        //        sb.append(RepositoryManager.tableSubHeader(link));
        sb.append("<div  " + blockExtra + ">");
        sb.append(HtmlUtil.div(link, headerExtra));
        sb.append("<div " + HtmlUtil.cssClass("hideshowblock")
                  + HtmlUtil.id(id)
                  + HtmlUtil.style("display:block;visibility:visible") + ">");
        if ( !visible) {
            sb.append(HtmlUtil.script(HtmlUtil.call("hide",
                    HtmlUtil.squote(id))));
        }

        sb.append(content.toString());
        sb.append(close(TAG_DIV));
        sb.append(close(TAG_DIV));
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param clickHtml _more_
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String clickHtml, String label,
                                           String content, boolean visible) {
        String       id = "block_" + (blockCnt++);
        StringBuffer sb = new StringBuffer();
        String link =
            HtmlUtil.jsLink(HtmlUtil.onMouseClick("toggleBlockVisibility('"
                + id + "','" + id + "img','" + "" + "','" + ""
                + "')"), clickHtml,
                         HtmlUtil.cssClass("pagesubheadinglink")) + label;

        //        sb.append(RepositoryManager.tableSubHeader(link));
        sb.append(link);
        sb.append(open(TAG_SPAN,
                       HtmlUtil.cssClass("hideshowblock") + HtmlUtil.id(id)
                       + HtmlUtil.style("display:block;visibility:visible")));
        if ( !visible) {
            sb.append(HtmlUtil.script(HtmlUtil.call("hide",
                    HtmlUtil.squote(id))));
        }

        sb.append(content.toString());
        sb.append(close(TAG_SPAN));
        return sb.toString();
    }








    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     */
    public static String entityEncode(String input) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < input.length(); ++i) {
            char ch = input.charAt(i);
            if (((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'))
                    || ((ch >= '0') && (ch <= '9'))) {
                sb.append(ch);
            } else {
                sb.append("&#" + (int) ch + ";");
            }
        }
        return sb.toString();
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        System.err.println(java.net.URLEncoder.encode("&", "UTF-8"));
        System.err.println(java.net.URLEncoder.encode("?", "UTF-8"));
        if (true) {
            return;
        }


        Class        c  = HtmlUtil.class;
        StringBuffer sb = new StringBuffer();
        sb.append(
            "//j-\n/** Do not change!!! This has been generated from HtmlUtil **/\n");
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            //            if(!Modifier.isStatic(m.getModifiers())) continue;
            if ( !m.getReturnType().equals(String.class)) {
                continue;
            }
            sb.append("public void " + m.getName() + "(");
            Class[]      params = m.getParameterTypes();
            StringBuffer implSb = new StringBuffer();
            for (int paramIdx = 0; paramIdx < params.length; paramIdx++) {
                if (paramIdx > 0) {
                    sb.append(", ");
                    implSb.append(", ");
                }
                implSb.append("param" + paramIdx);
                String type = params[paramIdx].getName();
                if (params[paramIdx].isArray()) {
                    type = params[paramIdx].getComponentType().getName()
                           + " []";
                }
                type = type.replace("java.lang.", "");
                sb.append(type + " param" + paramIdx);

            }

            sb.append(") {\n");
            sb.append("sb.append(HtmlUtil." + m.getName() + "(" + implSb
                      + "));\n");
            sb.append("}\n");
        }
        sb.append("//j+\n");
        //        System.out.println (sb);

    }


}


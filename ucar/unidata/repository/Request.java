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


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.*;



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



import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Request implements Constants {


    /** _more_ */
    private Hashtable fileUploads;

    /** _more_ */
    private String type;

    /** _more_ */
    private Hashtable parameters;

    /** _more_ */
    private Hashtable originalParameters;

    /** _more_ */
    private Repository repository;

    /** _more_ */
    private Hashtable httpHeaderArgs;

    /** _more_ */
    private String sessionId;

    /** _more_ */
    private OutputStream outputStream;

    /** _more_ */
    private User user;

    /** _more_ */
    private String ip;

    /** _more_ */
    //    private Entry collectionEntry;


    /** _more_ */
    private HttpServletRequest httpServletRequest;

    /** _more_ */
    private HttpServletResponse httpServletResponse;

    /** _more_ */
    private HttpServlet httpServlet;


    /** _more_ */
    private String leftMessage;

    public Request(Repository repository, User user) {
        this.repository         = repository;
        this.user = user;
    }


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param type _more_
     * @param parameters _more_
     */
    public Request(Repository repository, String type, Hashtable parameters) {
        this.repository         = repository;
        this.type               = type;
        this.parameters         = parameters;
        this.originalParameters = new Hashtable();
        originalParameters.putAll(parameters);
    }



    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param parameters _more_
     * @param httpServletRequest _more_
     * @param httpServletResponse _more_
     * @param httpServlet _more_
     */
    public Request(Repository repository, String type, Hashtable parameters,
                   HttpServletRequest httpServletRequest,
                   HttpServletResponse httpServletResponse,
                   HttpServlet httpServlet) {
        this(repository, type, parameters);
        this.httpServletRequest  = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.httpServlet         = httpServlet;
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String entryUrl(RequestUrl theUrl, Entry entry) {
        return entryUrl(theUrl, entry, ARG_ENTRYID);
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     * @param arg _more_
     *
     * @return _more_
     */
    public String entryUrl(RequestUrl theUrl, Entry entry, String arg) {
        if (entry.isTopGroup()) {
            return HtmlUtil.url(theUrl.toString(), arg, entry.getId());
        }
        if (entry.getIsLocalFile()) {
            return HtmlUtil.url(theUrl.toString(), arg, entry.getId());
        }
        
//        Group collectionGroup = entry.getCollectionGroup();
//        if (repository.isSynthEntry(entry.getId())) {
//            return url(theUrl, arg, entry.getId());
//        }

        //if (collectionGroup != null) {
        //            return url(theUrl, arg, entry.getId());
        //            String collectionPath =
        //                repository.getPathFromEntry(collectionGroup);
        //            return HtmlUtil.url(theUrl.getUrl(collectionPath), arg,
        //            entry.getId());
        //        }


        return url(theUrl, arg, entry.getId());
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     * @param arg1 _more_
     * @param value1 _more_
     *
     * @return _more_
     */
    public String entryUrl(RequestUrl theUrl, Entry entry, String arg1,
                           Object value1) {
        return HtmlUtil.url(entryUrl(theUrl, entry), arg1, value1);
    }



    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     * @param args _more_
     *
     * @return _more_
     */
    public String entryUrl(RequestUrl theUrl, Entry entry, List args) {
        return HtmlUtil.url(entryUrl(theUrl, entry), args);
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     * @param entry _more_
     * @param arg1 _more_
     * @param value1 _more_
     * @param arg2 _more_
     * @param value2 _more_
     *
     * @return _more_
     */
    public String entryUrl(RequestUrl theUrl, Entry entry, String arg1,
                           Object value1, String arg2, Object value2) {
        return HtmlUtil.url(entryUrl(theUrl, entry), arg1, value1, arg2,
                            value2);
    }




    /**
     * _more_
     *
     * @param theUrl _more_
     *
     * @return _more_
     */
    public String url(RequestUrl theUrl) {
        /*
        if (collectionEntry != null) {
            String collectionPath =
                repository.getPathFromEntry(collectionEntry);
            return theUrl.getUrl(collectionPath);
            }*/
        return theUrl.toString();
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     *
     * @return _more_
     */
    public String form(RequestUrl theUrl) {
        return HtmlUtil.form(url(theUrl));
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     *
     * @return _more_
     */
    public String formPost(RequestUrl theUrl) {
        return HtmlUtil.formPost(url(theUrl));
    }


    public String formPost(RequestUrl theUrl, String extra) {
        return HtmlUtil.formPost(url(theUrl),extra);
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String form(RequestUrl theUrl, String extra) {
        return HtmlUtil.form(url(theUrl), extra);
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     *
     * @return _more_
     */
    public String uploadForm(RequestUrl theUrl) {
        return uploadForm(theUrl, "");
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String uploadForm(RequestUrl theUrl, String extra) {
        return HtmlUtil.uploadForm(url(theUrl), extra);
    }


    /**
     * _more_
     *
     * @param theUrl _more_
     * @param arg1 _more_
     * @param value1 _more_
     *
     * @return _more_
     */
    public String url(RequestUrl theUrl, String arg1, Object value1) {
        return HtmlUtil.url(url(theUrl), arg1, value1);
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     * @param arg1 _more_
     * @param value1 _more_
     * @param arg2 _more_
     * @param value2 _more_
     *
     * @return _more_
     */
    public String url(RequestUrl theUrl, String arg1, Object value1,
                      String arg2, Object value2) {
        return HtmlUtil.url(url(theUrl), arg1, value1, arg2, value2);
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     * @param arg1 _more_
     * @param value1 _more_
     * @param arg2 _more_
     * @param value2 _more_
     * @param arg3 _more_
     * @param value3 _more_
     *
     * @return _more_
     */
    public String url(RequestUrl theUrl, String arg1, Object value1,
                      String arg2, Object value2, String arg3,
                      Object value3) {
        return HtmlUtil.url(url(theUrl), arg1, value1, arg2, value2, arg3,
                            value3);
    }

    /**
     * _more_
     *
     * @param theUrl _more_
     * @param arg1 _more_
     * @param value1 _more_
     * @param arg2 _more_
     * @param value2 _more_
     * @param arg3 _more_
     * @param value3 _more_
     * @param arg4 _more_
     * @param value4 _more_
     *
     * @return _more_
     */
    public String url(RequestUrl theUrl, String arg1, Object value1,
                      String arg2, Object value2, String arg3, Object value3,
                      String arg4, Object value4) {
        return HtmlUtil.url(url(theUrl), arg1, value1, arg2, value2, arg3,
                            value3, arg4, value4);
    }




    /**
     *  Set the OutputStream property.
     *
     *  @param value The new value for OutputStream
     */
    public void setOutputStream(OutputStream value) {
        outputStream = value;
    }

    /**
     *  Get the OutputStream property.
     *
     *  @return The OutputStream
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }



    /**
     * _more_
     *
     * @param uploads _more_
     */
    public void setFileUploads(Hashtable uploads) {
        fileUploads = uploads;
    }


    /**
     * _more_
     *
     * @param arg _more_
     *
     * @return _more_
     */
    public String getUploadedFile(String arg) {
        if (fileUploads == null) {
            return null;
        }
        return (String) fileUploads.get(arg);
    }

    /** _more_ */
    public StringBuffer tmp = new StringBuffer();

    /**
     * _more_
     *
     * @return _more_
     */
    public String getUrl() {
        return getRequestPath() + "?" + getUrlArgs();
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getUrl(RequestUrl request) {
        return url(request) + "?" + getUrlArgs();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullUrl() {
        return repository.absoluteUrl(getUrl());
    }



    /**
     * _more_
     *
     * @param except _more_
     *
     * @return _more_
     */
    public String getUrl(String except) {
        return getRequestPath() + "?" + getUrlArgs(except);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getUrlArgs() {
        return getUrlArgs((Hashtable) null);
    }

    /**
     * _more_
     *
     * @param except _more_
     *
     * @return _more_
     */
    public String getUrlArgs(String except) {
        return getUrlArgs(Misc.newHashtable(except, except));
    }


    /**
     * _more_
     *
     * @param except _more_
     *
     * @return _more_
     */
    public String getUrlArgs(Hashtable except) {
        StringBuffer sb  = new StringBuffer();
        int          cnt = 0;
        for (Enumeration keys = parameters.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ((except != null) && (except.get(arg) != null)) {
                continue;
            }
            Object value = parameters.get(arg);
            if (value instanceof List) {
                List l = (List) value;
                if (l.size() == 0) {
                    continue;
                }
                for (int i = 0; i < l.size(); i++) {
                    String svalue = (String) l.get(i);
                    if (svalue.length() == 0) {
                        continue;
                    }
                    if (cnt++ > 0) {
                        sb.append("&");
                    }
                    sb.append(arg + "=" + svalue);
                }
                continue;
            }
            String svalue =  value.toString();
            if (svalue.length() == 0) {
                continue;
            }
            if (cnt++ > 0) {
                sb.append("&");
            }
            try {
                svalue = java.net.URLEncoder.encode(svalue, "UTF-8");
            } catch (Exception exc) { /*noop*/
            }
            sb.append(arg + "=" + svalue);
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getPathEmbeddedArgs() {
        try {
            StringBuffer sb  = new StringBuffer();
            int          cnt = 0;
            for (Enumeration keys =
                    parameters.keys(); keys.hasMoreElements(); ) {
                String arg   = (String) keys.nextElement();
                Object value = parameters.get(arg);
                if (value instanceof List) {
                    List l = (List) value;
                    if (l.size() == 0) {
                        continue;
                    }
                    for (int i = 0; i < l.size(); i++) {
                        String svalue = (String) l.get(i);
                        if (svalue.length() == 0) {
                            continue;
                        }
                        if (cnt++ > 0) {
                            sb.append("/");
                        }
                        sb.append(arg + ":" + encodeEmbedded(svalue));
                    }
                    continue;
                }
                String svalue = value.toString();
                if (svalue.length() == 0) {
                    continue;
                }
                if (cnt++ > 0) {
                    sb.append("/");
                }
                sb.append(arg + ":" + encodeEmbedded(svalue));
            }
            return sb.toString();
        } catch (Exception exc) {
            throw new WrapperException(exc);
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String encodeEmbedded(Object o) {
        String s = o.toString();
        try {
            if (s.indexOf("/") >= 0) {
                s = "b64:" + XmlUtil.encodeBase64(s.getBytes()).trim();
            }
            //            s = java.net.URLEncoder.encode(s, "UTF-8");
            return s;
        } catch (Exception exc) {
            throw new WrapperException(exc);
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String decodeEmbedded(String s) {
        try {
            if (s.startsWith("b64:")) {
                s = s.substring(4);
                //s = java.net.URLDecoder.decode(s, "UTF-8");     
                s = new String(XmlUtil.decodeBase64(s));
            }
            return s;
        } catch (Exception exc) {
            throw new WrapperException(exc);
        }

    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public String getFromPath(String key) {
        String path = getRequestPath();
        //Look for .../id:<id>
        String prefix = key + ":";
        int    idx    = path.indexOf(prefix);
        if (idx >= 0) {
            String value = path.substring(idx + prefix.length());
            idx = value.indexOf("/");
            if (idx >= 0) {
                value = value.substring(0, idx);
            }
            try {
                value = decodeEmbedded(value);
            } catch (Exception exc) {
                throw new WrapperException(exc);
            }
            return value;
        }
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getDefinedProperties() {
        Hashtable props = new Hashtable();
        for (Enumeration keys = parameters.keys(); keys.hasMoreElements(); ) {
            String arg   =  keys.nextElement().toString();
            Object value = parameters.get(arg);
            if (value instanceof List) {
                if (((List) value).size() == 0) {
                    continue;
                }
                props.put(arg, value);
                continue;
            }
            if (value.toString().length() == 0) {
                continue;
            }
            props.put(arg, value);
        }
        return props;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getArgs() {
        return parameters;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        Request that = (Request) o;
        return this.type.equals(that.type)
               && Misc.equals(this.user, that.user)
               && this.originalParameters.equals(that.originalParameters);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return type.hashCode() ^ Misc.hashcode(user)
               ^ originalParameters.hashCode();
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object remove(Object key) {
        Object v = parameters.get(key);
        parameters.remove(key);
        return v;
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void put(Object key, Object value) {
        parameters.put(key, value);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean exists(Object key) {
        Object result = getValue(key, (Object) null);
        return result != null;
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean defined(String key) {
        Object result = getValue(key, (Object) null);
        if (result == null) {
            return false;
        }
        if (result instanceof List) {
            return ((List) result).size() > 0;
        }
        String sresult = (String) result;
        if (sresult.trim().length() == 0) {
            return false;
        }
        return true;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public List get(String key, List dflt) {
        Object result = getValue(key, (Object) null);
        if (result == null) {
            return dflt;
        }
        if (result instanceof List) {
            return (List) result;
        }
        List tmp = new ArrayList();
        tmp.add(result);
        return tmp;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getUnsafeString(String key, String dflt) {
        String result = (String) getValue(key, (String) null);
        if (result == null) {
            return dflt;
        }
        return result;
    }


    /** _more_ */
    private static Pattern checker;

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     * @param patternString _more_
     *
     * @return _more_
     */
    public String getCheckedString(String key, String dflt,
                                   String patternString) {
        return getCheckedString(key, dflt, Pattern.compile(patternString));
    }







    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     * @param pattern _more_
     *
     * @return _more_
     */
    public String getCheckedString(String key, String dflt, Pattern pattern) {
        String v = (String) getValue(key, (String) null);
        if (v == null) {
            return dflt;
        }
        /*
        for(int i=0;i<v.length();i++) {
            String tmp = v.substring(0,i);
            Matcher xx = pattern.matcher(tmp);
            if ( !xx.find()) {
                System.err.println("BAD:" + tmp);
            }
            }*/

        Matcher matcher = pattern.matcher(v);
        if ( !matcher.find()) {
            throw new BadInputException("Incorrect input for:" + key
                                        + " value:" + v + ":");
        }
        //TODO:Check the value
        return v;
        //        return repository.getDatabaseManager().escapeString(v);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public String getString(String key) {
        return getString(key, "");
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getString(String key, String dflt) {
        if (checker == null) {
            checker =
                Pattern.compile(repository.getProperty(PROP_REQUEST_PATTERN));
        }
        return getCheckedString(key, dflt, checker);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private Object getValue(Object key, Object dflt) {
        Object result = parameters.get(key);
        if (result == null) {
            result = getFromPath(key.toString());
        }
        if (result == null) {
            return dflt;
        }
        return result;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getValue(Object key, String dflt) {
        Object result = parameters.get(key);
        if (result == null) {
            result = getFromPath(key.toString());
        }
        if (result == null) {
            return dflt;
        }
        if (result instanceof List) {
            List l = (List) result;
            if (l.size() == 0) {
                return dflt;
            }
            return (String) l.get(0);
        }
        return result.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public OutputType getOutput() {
        return getOutput(OutputHandler.OUTPUT_HTML.getId());
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public OutputType getOutput(String dflt) {
        return new OutputType(getString(ARG_OUTPUT, dflt));
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getId(String dflt) {
        return getString(ARG_ENTRYID, dflt);
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getIds(String dflt) {
        return getString(ARG_ENTRYIDS, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getDateSelect(String name, String dflt) {
        String v = getUnsafeString(name, (String) null);
        if (v == null) {
            return dflt;
        }
        if (defined(name + ".time")) {
            v = v + " " + getUnsafeString(name + ".time", "");
        }

        //TODO:Check value
        return v;
    }



    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getWhat(String dflt) {
        return getString(ARG_WHAT, dflt);
    }

    /*
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int get(Object key, int dflt) {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }
        return new Integer(result).intValue();
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public double get(Object key, double dflt) {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }
        return new Double(result).doubleValue();
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException _more_
     */
    public Date get(Object key, Date dflt) throws java.text.ParseException {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }
        return DateUtil.parse(result);
    }

    /**
     * _more_
     *
     * @param from _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getDate(String from, Date dflt) throws Exception {
        if ( !defined(from)) {
            return dflt;
        }
        return repository.parseDate(getUnsafeString(from, ""));
    }


    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException _more_
     */
    public Date[] getDateRange(String from, String to, Date dflt)
            throws java.text.ParseException {
        String fromDate = "";
        String toDate   = "";
        if (defined(from) || defined(to)) {
            fromDate = (String) getDateSelect(from, "").trim();
            toDate   = (String) getDateSelect(to, "").trim();
        } else if (defined(ARG_RELATIVEDATE)) {
            fromDate = (String) getDateSelect(ARG_RELATIVEDATE, "").trim();
            if (fromDate.equals("none")) {
                return new Date[] { null, null };
            }
            toDate = "now";
        }
        return DateUtil.getDateRange(fromDate, toDate, dflt);
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean get(Object key, boolean dflt) {
        String result = (String) getValue(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }
        return new Boolean(result).booleanValue();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Enumeration keys() {
        return parameters.keys();
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getRequestPath() {
        return type;
    }



    /**
     * Class BadInputException _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class BadInputException extends RuntimeException {

        /**
         * _more_
         *
         * @param msg _more_
         */
        public BadInputException(String msg) {
            super(msg);
        }
    }

    /**
     * Set the HttpHeaderArgs property.
     *
     * @param value The new value for HttpHeaderArgs
     */
    public void setHttpHeaderArgs(Hashtable value) {
        httpHeaderArgs = value;
    }

    /**
     * Get the HttpHeaderArgs property.
     *
     * @return The HttpHeaderArgs
     */
    public Hashtable getHttpHeaderArgs() {
        return httpHeaderArgs;
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getHeaderArg(String name) {
        if (httpHeaderArgs == null) {
            return null;
        }
        String arg = (String) httpHeaderArgs.get(name);
        if (arg == null) {
            arg = (String) httpHeaderArgs.get(name.toLowerCase());
        }
        return arg;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        String args = getUrlArgs();
        if (args.trim().length() > 0) {
            return type + " url args:" + args;
        } else {
            return type;
        }
    }

    /** _more_ */
    private boolean sessionIdWasSet = false;

    /**
     * Set the SessionId property.
     *
     * @param value The new value for SessionId
     */
    public void setSessionId(String value) {
        sessionId       = value;
        sessionIdWasSet = true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getSessionIdWasSet() {
        return sessionIdWasSet;
    }


    /**
     * Get the SessionId property.
     *
     * @return The SessionId
     */
    public String getSessionId() {
        return sessionId;
    }




    /**
     * Set the User property.
     *
     * @param value The new value for User
     */
    public void setUser(User value) {
        user = value;
    }

    /**
     * Get the User property.
     *
     * @return The User
     */
    public User getUser() {
        return user;
    }


    /**
     * Set the Ip property.
     *
     * @param value The new value for Ip
     */
    public void setIp(String value) {
        ip = value;
    }

    /**
     * Get the Ip property.
     *
     * @return The Ip
     */
    public String getIp() {
        return ip;
    }


    /**
     *  Set the CollectionEntry property.
     *
     *  @param value The new value for CollectionEntry
     */
/*
    public void setCollectionEntry(Entry value) {
        collectionEntry = value;
    }
*/
    /**
     *  Get the CollectionEntry property.
     *
     *  @return The CollectionEntry
     */
/*
    public Entry getCollectionEntry() {
        return collectionEntry;
    }
*/



    /**
     *  Get the HttpServletRequest property.
     *
     *  @return The HttpServletRequest
     */
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public javax.servlet.http.HttpServlet getHttpServlet() {
        return httpServlet;
    }

    /**
     *  Set the LeftMessage property.
     *
     *  @param value The new value for LeftMessage
     */
    public void setLeftMessage(String value) {
        leftMessage = value;
    }

    /**
     *  Get the LeftMessage property.
     *
     *  @return The LeftMessage
     */
    public String getLeftMessage() {
        return leftMessage;
    }



}


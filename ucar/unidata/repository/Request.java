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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;

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
    private RequestContext requestContext;

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

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param type _more_
     * @param requestContext _more_
     * @param parameters _more_
     */
    public Request(Repository repository, String type,
                   RequestContext requestContext, Hashtable parameters) {
        this.repository         = repository;
        this.type               = type;
        this.requestContext     = requestContext;
        this.parameters         = parameters;
        this.originalParameters = new Hashtable();
        originalParameters.putAll(parameters);
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
            String value = (String) parameters.get(arg);
            if (value.length() == 0) {
                continue;
            }
            if (cnt++ > 0) {
                sb.append("&");
            }
            sb.append(arg + "=" + value);
        }
        return sb.toString();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getDefinedProperties() {
        Hashtable props = new Hashtable();
        for (Enumeration keys = parameters.keys(); keys.hasMoreElements(); ) {
            String arg   = (String) keys.nextElement();
            String value = (String) parameters.get(arg);
            if (value.length() == 0) {
                continue;
            }
            props.put(arg, value);
        }
        return props;
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
               && this.requestContext.equals(that.requestContext)
               && this.originalParameters.equals(that.originalParameters);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return type.hashCode() ^ requestContext.hashCode()
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
    public boolean hasSetParameter(String key) {
        String v = (String) parameters.get(key);
        if ((v == null) || (v.trim().length() == 0)) {
            return false;
        }
        return true;
    }


    /**
     * _more_
     *
     * @param key _more_
     */
    public void remove(String key) {
        parameters.remove(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void put(String key, String value) {
        parameters.put(key, value);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean exists(String key) {
        String result = (String) get(key, (String) null);
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
        String result = (String) get(key, (String) null);
        if (result == null) {
            return false;
        }
        if (result.trim().length() == 0) {
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
    public String getUnsafeString(String key, String dflt) {
        String result = (String) get(key, (String) null);
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
        String v = (String) get(key, (String) null);
        if (v == null) {
            return dflt;
        }
        Matcher matcher = pattern.matcher(v);
        if ( !matcher.find()) {
            throw new BadInputException("Incorrect input for:" + key
                                        + " value:" + v + ":");
        }
        //TODO:Check the value
        return v;
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
    private String get(String key, String dflt) {
        String result = (String) parameters.get(key);
        if (result == null) {
            return dflt;
        }
        return result;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getOutput() {
        return getOutput(OutputHandler.OUTPUT_HTML);
    }

    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getOutput(String dflt) {
        return getString(ARG_OUTPUT, dflt);
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getId(String dflt) {
        return getString(ARG_ID, dflt);
    }


    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getIds(String dflt) {
        return getString(ARG_IDS, dflt);
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

    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getType(String dflt) {
        return getString(ARG_TYPE, dflt);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getUser() {
        return getString(ARG_USER, (String) null);
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int get(String key, int dflt) {
        String result = (String) get(key, (String) null);
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
    public double get(String key, double dflt) {
        String result = (String) get(key, (String) null);
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
    public Date get(String key, Date dflt) throws java.text.ParseException {
        String result = (String) get(key, (String) null);
        if ((result == null) || (result.trim().length() == 0)) {
            return dflt;
        }
        return DateUtil.parse(result);
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
        String fromDate = (String) getDateSelect(from, "").trim();
        String toDate   = (String) getDateSelect(to, "").trim();

        Date   fromDttm = DateUtil.parseRelative(dflt, fromDate, -1);
        Date   toDttm   = DateUtil.parseRelative(dflt, toDate, +1);
        //        System.err.println ("dflt: " + dflt);
        //        System.err.println ("toDttm:" + toDate + " " + toDttm);

        if ((fromDate.length() > 0) && (fromDttm == null)) {
            if ( !fromDate.startsWith("-")) {
                fromDttm = DateUtil.parse(fromDate);
            }
        }
        if ((toDate.length() > 0) && (toDttm == null)) {
            if ( !toDate.startsWith("+")) {
                toDttm = DateUtil.parse(toDate);
            }
        }

        if ((fromDttm == null) && fromDate.startsWith("-")) {
            if (toDttm == null) {
                throw new IllegalArgumentException(
                    "Cannot do relative From Date when To Date is not set");
            }
            fromDttm = DateUtil.getRelativeDate(toDttm, fromDate);
        }

        if ((toDttm == null) && toDate.startsWith("+")) {
            if (fromDttm == null) {
                throw new IllegalArgumentException(
                    "Cannot do relative From Date when To Date is not set");
            }
            toDttm = DateUtil.getRelativeDate(fromDttm, toDate);
        }

        //        System.err.println("from:" + Repository.fmt(fromDttm) + " -- " + Repository.fmt(toDttm));


        return new Date[] { fromDttm, toDttm };
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean get(String key, boolean dflt) {
        String result = (String) get(key, (String) null);
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
     * Set the RequestContext property.
     *
     * @param value The new value for RequestContext
     */
    public void setRequestContext(RequestContext value) {
        requestContext = value;
    }

    /**
     * Get the RequestContext property.
     *
     * @return The RequestContext
     */
    public RequestContext getRequestContext() {
        return requestContext;
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
        return (String) httpHeaderArgs.get(name);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return type + " " + getUrlArgs();
    }

    /**
     * Set the SessionId property.
     *
     * @param value The new value for SessionId
     */
    public void setSessionId(String value) {
        sessionId = value;
    }

    /**
     * Get the SessionId property.
     *
     * @return The SessionId
     */
    public String getSessionId() {
        return sessionId;
    }



}


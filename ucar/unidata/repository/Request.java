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


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Request {

    /** _more_          */
    public static final String CALL_QUERY = "/query";

    public static final String CALL_FETCH = "/fetch";

    /** _more_          */
    public static final String CALL_SQL = "/sql";

    /** _more_          */
    public static final String CALL_SEARCHFORM = "/searchform";

    /** _more_          */
    public static final String CALL_LIST = "/list";

    /** _more_          */
    public static final String CALL_SHOWGROUP = "/showgroup";

    /** _more_          */
    public static final String CALL_SHOWFILE = "/showfile";

    /** _more_          */
    public static final String CALL_GRAPH = "/graph";

    /** _more_          */
    public static final String CALL_GRAPHVIEW = "/graphview";


    /** _more_          */
    private String type;

    /** _more_          */
    private RequestContext requestContext;

    /** _more_          */
    private Hashtable parameters;

    private User user;

    /**
     * _more_
     *
     * @param type _more_
     * @param requestContext _more_
     * @param parameters _more_
     */
    public Request(String type, RequestContext requestContext,
                   Hashtable parameters) {
        this.type           = type;
        this.requestContext = requestContext;
        this.parameters     = parameters;
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

    public boolean hasSetParameter(String key) {
        String v = (String) parameters.get(key);
        if(v == null || v.trim().length() == 0) return false;
        return true;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public String get(String key) {
        return (String) parameters.get(key);
    }

    public String get(String key,String dflt) {
        String result = get(key);
        if(result == null)return dflt;
        return result;
    }

    /**
     * Set the Parameters property.
     *
     * @param value The new value for Parameters
     */
    public void setParameters(Hashtable value) {
        parameters = value;
    }

    /**
     * Get the Parameters property.
     *
     * @return The Parameters
     */
    public Hashtable getParameters() {
        return parameters;
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
    public String getType() {
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



}


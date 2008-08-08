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


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryBase implements Constants, RepositorySource {


    /** _more_ */
    public RequestUrl URL_GETMAP = new RequestUrl(this, "/getmap");


    /** _more_ */
    public RequestUrl URL_MESSAGE = new RequestUrl(this, "/message");

    /** _more_ */
    public RequestUrl URL_DUMMY = new RequestUrl(this, "/dummy");


    /** _more_ */
    public RequestUrl URL_ENTRY_SEARCHFORM = new RequestUrl(this,
                                                 "/entry/searchform");

    /** _more_ */
    public RequestUrl URL_COMMENTS_SHOW = new RequestUrl(this,
                                              "/comments/show");

    /** _more_ */
    public RequestUrl URL_COMMENTS_ADD = new RequestUrl(this,
                                             "/comments/add");

    /** _more_ */
    public RequestUrl URL_COMMENTS_EDIT = new RequestUrl(this,
                                              "/comments/edit");

    /** _more_ */
    public RequestUrl URL_ENTRY_SEARCH = new RequestUrl(this,
                                             "/entry/search");

    public RequestUrl URL_ENTRY_XMLCREATE = new RequestUrl(this,
                                             "/entry/xmlcreate");


    /** _more_ */
    public RequestUrl URL_ASSOCIATION_ADD = new RequestUrl(this,
                                                "/association/add");

    /** _more_          */
    public RequestUrl URL_ASSOCIATION_DELETE = new RequestUrl(this,
                                                   "/association/delete");

    /** _more_ */
    public RequestUrl URL_LIST_HOME = new RequestUrl(this, "/list/home");

    /** _more_ */
    public RequestUrl URL_LIST_SHOW = new RequestUrl(this, "/list/show");

    /** _more_ */
    public RequestUrl URL_GRAPH_VIEW = new RequestUrl(this, "/graph/view");

    /** _more_ */
    public RequestUrl URL_GRAPH_GET = new RequestUrl(this, "/graph/get");

    /** _more_ */
    public RequestUrl URL_ENTRY_SHOW = new RequestUrl(this, "/entry/show",
                                           "View Entry");

    /** _more_ */
    public RequestUrl URL_ENTRY_COPY = new RequestUrl(this, "/entry/copy");


    /** _more_ */
    public RequestUrl URL_ENTRY_DELETE = new RequestUrl(this,
                                             "/entry/delete", "Delete");

    /** _more_ */
    public RequestUrl URL_ENTRY_DELETELIST = new RequestUrl(this,
                                                 "/entry/deletelist");


    /** _more_ */
    public RequestUrl URL_ACCESS_FORM = new RequestUrl(this, "/access/form",
                                            "Access");


    /** _more_ */
    public RequestUrl URL_ACCESS_CHANGE = new RequestUrl(this,
                                              "/access/change");

    /** _more_ */
    public RequestUrl URL_ENTRY_CHANGE = new RequestUrl(this,
                                             "/entry/change");

    /** _more_ */
    public RequestUrl URL_ENTRY_FORM = new RequestUrl(this, "/entry/form",
                                           "Edit Entry");


    /** _more_ */
    public RequestUrl URL_ENTRY_NEW = new RequestUrl(this, "/entry/new");


    /** _more_ */
    public RequestUrl URL_GETENTRIES = new RequestUrl(this, "/getentries");

    /** _more_ */
    public RequestUrl URL_ENTRY_GET = new RequestUrl(this, "/entry/get");


    /** _more_ */
    public RequestUrl URL_USER_LOGIN = new RequestUrl(this, "/user/login");


    /** _more_ */
    public RequestUrl URL_USER_LOGOUT = new RequestUrl(this, "/user/logout");


    /** _more_ */
    public RequestUrl URL_USER_HOME = new RequestUrl(this, "/user/home");

    /** _more_ */
    public RequestUrl URL_USER_SETTINGS = new RequestUrl(this,
                                              "/user/settings", "Settings");

    /** _more_ */
    public RequestUrl URL_USER_CART = new RequestUrl(this, "/user/cart",
                                          "Data Cart");

    /** _more_ */
    public RequestUrl URL_USER_LIST = new RequestUrl(this, "/user/list",
                                          "Users");

    /** _more_ */
    public RequestUrl URL_USER_EDIT = new RequestUrl(this, "/user/edit",
                                          "Users");

    /** _more_ */
    public RequestUrl URL_USER_NEW = new RequestUrl(this, "/user/new");




    /** _more_ */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);




    /** _more_ */
    private String urlBase = "/repository";



    /** _more_ */
    private String hostname;

    /** _more_ */
    private int port;

    /** _more_ */
    private boolean clientMode = false;


    public RepositoryBase() {
    }


    public RepositoryBase(String hostname, int port)
            throws Exception {
        this.hostname = hostname;
        this.port = port;
    }



    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String absoluteUrl(String url) {
        return "http://" + hostname + ":" + port + url;
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String httpsUrl(String url) {
        return "https://" + hostname + ":" + port + url;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }


    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     */
    public void setHostname(String hostname, int port) {
        this.hostname = hostname;
        this.port     = port;
    }


    public RepositoryBase getRepositoryBase() {
        return this;
    }



    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public String fileUrl(String f) {
        return urlBase + f;
    }


    /**
     * Set the UrlBase property.
     *
     * @param value The new value for UrlBase
     */
    public void setUrlBase(String value) {
        urlBase = value;
    }

    /**
     * Get the UrlBase property.
     *
     * @return The UrlBase
     */
    public String getUrlBase() {
        return urlBase;
    }




}


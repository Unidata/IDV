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

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryBase implements Constants, RepositorySource {


    /** _more_ */
    public final RequestUrl URL_HELP = new RequestUrl(this,
                                           "/help/index.html");

    /** _more_ */
    public final RequestUrl URL_PING = new RequestUrl(this, "/ping");

    /** _more_ */
    public final RequestUrl URL_INFO = new RequestUrl(this, "/info");

    /** _more_ */
    public final RequestUrl URL_MESSAGE = new RequestUrl(this, "/message");

    /** _more_ */
    public final RequestUrl URL_DUMMY = new RequestUrl(this, "/dummy");

    public final RequestUrl URL_INSTALL = new RequestUrl(this, "/install");


    public final RequestUrl URL_REGISTRY_ADD = new RequestUrl(this, "/registry/add");

    public final RequestUrl URL_REGISTRY_LIST = new RequestUrl(this, "/registry/list");

    public final RequestUrl URL_REGISTRY_INFO = new RequestUrl(this, "/registry/info");


    /** _more_ */
    public final RequestUrl URL_SEARCH_FORM = new RequestUrl(this,
                                                  "/search/form", "Advanced");

    public final RequestUrl URL_SEARCH_ASSOCIATIONS = new RequestUrl(this,
                                                  "/search/associations/do", "Search Associations");

    public final RequestUrl URL_SEARCH_ASSOCIATIONS_FORM = new RequestUrl(this,
                                                  "/search/associations/form", "Search Associations");

    /** _more_ */
    public final RequestUrl URL_SEARCH_TEXTFORM = new RequestUrl(this,
                                                      "/search/textform",
                                                      "Search");

    /** _more_ */
    public final RequestUrl URL_SEARCH_BROWSE = new RequestUrl(this,
                                                    "/search/browse",
                                                    "Browse");


    public final RequestUrl URL_SEARCH_REMOTE_FORM = new RequestUrl(this,
                                                    "/search/remote/form",
                                                    "Search Remote Servers");

    public final RequestUrl URL_SEARCH_REMOTE_DO = new RequestUrl(this,
                                                    "/search/remote/do",
                                                    "Search Remote Servers");

    /** _more_ */
    public final RequestUrl URL_ENTRY_SEARCH = new RequestUrl(this,
                                                   "/search/do", "Search");

    /** _more_ */
    public final RequestUrl[] searchUrls = { URL_SEARCH_TEXTFORM,
                                             URL_SEARCH_FORM,
                                             URL_SEARCH_BROWSE,
                                             URL_SEARCH_ASSOCIATIONS_FORM };

    /** _more_ */
    public final RequestUrl[] remoteSearchUrls = { URL_SEARCH_TEXTFORM,
                                             URL_SEARCH_FORM,
                                             URL_SEARCH_BROWSE,
                                                      URL_SEARCH_ASSOCIATIONS_FORM,
                                                      URL_SEARCH_REMOTE_FORM};

    /** _more_ */
    public final RequestUrl URL_COMMENTS_SHOW = new RequestUrl(this,
                                                    "/entry/comments/show");

    /** _more_ */
    public final RequestUrl URL_COMMENTS_ADD = new RequestUrl(this,
                                                   "/entry/comments/add");

    /** _more_ */
    public final RequestUrl URL_COMMENTS_EDIT = new RequestUrl(this,
                                                    "/entry/comments/edit");


    /** _more_ */
    public final RequestUrl URL_ENTRY_XMLCREATE = new RequestUrl(this,
                                                      "/entry/xmlcreate");


    /** _more_ */
    public final RequestUrl URL_ASSOCIATION_ADD = new RequestUrl(this,
                                                      "/association/add");

    /** _more_ */
    public final RequestUrl URL_ASSOCIATION_DELETE =
        new RequestUrl(this, "/association/delete");

    /** _more_ */
    public final RequestUrl URL_LIST_HOME = new RequestUrl(this,
                                                "/list/home");

    /** _more_ */
    public final RequestUrl URL_LIST_SHOW = new RequestUrl(this,
                                                "/list/show");

    /** _more_ */
    public final RequestUrl URL_GRAPH_VIEW = new RequestUrl(this,
                                                 "/graph/view");

    /** _more_ */
    public final RequestUrl URL_GRAPH_GET = new RequestUrl(this,
                                                "/graph/get");

    /** _more_ */
    public final RequestUrl URL_ENTRY_SHOW = new RequestUrl(this,
                                                 "/entry/show", "View Entry");

    /** _more_ */
    public final RequestUrl URL_ENTRY_COPY = new RequestUrl(this,
                                                 "/entry/copy");


    /** _more_ */
    public final RequestUrl URL_ENTRY_DELETE = new RequestUrl(this,
                                                   "/entry/delete", "Delete");

    /** _more_ */
    public final RequestUrl URL_ENTRY_DELETELIST = new RequestUrl(this,
                                                       "/entry/deletelist");


    /** _more_ */
    public final RequestUrl URL_ACCESS_FORM = new RequestUrl(this,
                                                  "/access/form", "Access");


    /** _more_ */
    public final RequestUrl URL_ACCESS_CHANGE = new RequestUrl(this,
                                                    "/access/change");

    /** _more_ */
    public final RequestUrl URL_ENTRY_CHANGE = new RequestUrl(this,
                                                   "/entry/change");

    /** _more_ */
    public final RequestUrl URL_ENTRY_FORM = new RequestUrl(this,
                                                 "/entry/form", "Edit Entry");


    /** _more_ */
    public final RequestUrl URL_ENTRY_NEW = new RequestUrl(this,
                                                "/entry/new", "New Entry");

    /** _more_ */
    public final RequestUrl URL_ENTRY_UPLOAD = new RequestUrl(this,
                                                   "/entry/upload",
                                                   "Upload a file");


    /** _more_ */
    public final RequestUrl URL_ENTRY_GETENTRIES = new RequestUrl(this,
                                                       "/entry/getentries");

    /** _more_ */
    public final RequestUrl URL_ENTRY_GET = new RequestUrl(this,
                                                "/entry/get");


    /** _more_ */
    public final RequestUrl URL_USER_LOGIN = new RequestUrl(this,
                                                 "/user/login");


    /** _more_ */
    public final RequestUrl URL_USER_FAVORITE = new RequestUrl(this,
                                                    "/user/favorite");

    /** _more_ */
    public final RequestUrl URL_USER_ACTIVITY = new RequestUrl(this,
                                                    "/user/activity");

    /** _more_ */
    public final RequestUrl URL_USER_RESETPASSWORD =
        new RequestUrl(this, "/user/resetpassword");

    /** _more_ */
    public final RequestUrl URL_USER_FINDUSERID = new RequestUrl(this,
                                                      "/user/finduserid");


    /** _more_ */
    public final RequestUrl URL_USER_LOGOUT = new RequestUrl(this,
                                                  "/user/logout");


    /** _more_ */
    public final RequestUrl URL_USER_HOME = new RequestUrl(this,
                                                "/user/home", "User Home");

    /** _more_ */
    public final RequestUrl URL_USER_PROFILE = new RequestUrl(this,
                                                   "/user/profile",
                                                   "User Profile");

    /** _more_ */
    public final RequestUrl URL_USER_SETTINGS = new RequestUrl(this,
                                                    "/user/settings",
                                                    "Settings");

    /** _more_ */
    public final RequestUrl URL_USER_MONITORS = new RequestUrl(this,
                                                    "/user/monitors",
                                                    "Monitors");

    /** _more_ */
    public final RequestUrl URL_USER_CART = new RequestUrl(this,
                                                "/user/cart", "Data Cart");

    /** _more_ */
    public final RequestUrl URL_USER_LIST = new RequestUrl(this,
                                                "/user/list", "Users");

    /** _more_ */
    public final RequestUrl URL_USER_EDIT = new RequestUrl(this,
                                                "/user/edit", "Users");

    /** _more_ */
    public final RequestUrl URL_USER_NEW = new RequestUrl(this, "/user/new");




    /** _more_ */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);


    /** _more_ */
    private static String urlBase = "/repository";


    /** _more_ */
    private String hostname = "";

    /** _more_ */
    private int httpPort = 80;

    /** _more_ */
    private boolean clientMode = false;



    /** _more_ */
    public static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    /** _more_ */
    public static final String DEFAULT_TIME_SHORTFORMAT =
        "yyyy/MM/dd HH:mm z";

    /** _more_ */
    public static final String DEFAULT_TIME_THISYEARFORMAT =
        "yyyy/MM/dd HH:mm z";


    /** _more_ */
    protected SimpleDateFormat sdf;

    /** _more_ */
    protected SimpleDateFormat displaySdf;

    /** _more_ */
    protected SimpleDateFormat thisYearSdf;


    /** _more_ */
    protected SimpleDateFormat dateSdf =
        RepositoryUtil.makeDateFormat("yyyy-MM-dd");

    /** _more_ */
    protected SimpleDateFormat timeSdf =
        RepositoryUtil.makeDateFormat("HH:mm:ss z");

    /** _more_ */
    protected List<SimpleDateFormat> formats;

    /**
     * _more_
     */
    public RepositoryBase() {}


    /**
     * _more_
     *
     * @param port _more_
     *
     * @throws Exception _more_
     */
    public RepositoryBase(int port) throws Exception {
        this.httpPort = port;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getGUID() {
        return UUID.randomUUID().toString();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected long currentTime() {
        return new Date().getTime();

    }


    /**
     * _more_
     *
     * @param format _more_
     *
     * @return _more_
     */
    protected SimpleDateFormat makeSDF(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern(format);
        return sdf;
    }

    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public String formatDate(Date d) {
        if (sdf == null) {
            sdf = makeSDF(getProperty(PROP_DATE_FORMAT, DEFAULT_TIME_FORMAT));
        }
        if (d == null) {
            return BLANK;
        }
        return sdf.format(d);
    }




    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException _more_
     */
    public Date parseDate(String dttm) throws java.text.ParseException {
        if (formats == null) {
            formats = new ArrayList<SimpleDateFormat>();
            formats.add(makeSDF("yyyy-MM-dd HH:mm:ss z"));
            formats.add(makeSDF("yyyy-MM-dd HH:mm:ss"));
            formats.add(makeSDF("yyyy-MM-dd HH:mm"));
            formats.add(makeSDF("yyyy-MM-dd"));
        }


        for (SimpleDateFormat fmt : formats) {
            try {
                return fmt.parse(dttm);
            } catch (Exception noop) {}
        }
        throw new IllegalArgumentException("Unable to parse date:" + dttm);
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String absoluteUrl(String url) {
        return "http://" + getHostname() + ":" + getPort() + url;
    }



    /**
     * _more_
     *
     * @param requestUrl _more_
     */
    public void initRequestUrl(RequestUrl requestUrl) {}

    /**
     * _more_
     *
     * @param requestUrl _more_
     *
     * @return _more_
     */
    public String getUrlPath(RequestUrl requestUrl) {
        return getUrlBase() + requestUrl.getPath();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHttpsPort() {
        return getProperty(PROP_SSL_PORT, "").trim();
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String httpsUrl(String url) {
        String port = getHttpsPort();
        if ((port != null) && (port.length() == 0)) {
            return "http://" + getHostname() + ":" + getPort() + url;
            //            return url;
            //            throw new IllegalStateException("Do not have ssl port defined");
        }
        if (port.equals("default")) {
            return "https://" + getHostname() + url;
        } else {
            return "https://" + getHostname() + ":" + port + url;
        }
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String name, String dflt) {
        return dflt;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public RepositoryBase getRepositoryBase() {
        return this;
    }

    /**
     * Set the Hostname property.
     *
     * @param value The new value for Hostname
     */
    public void setHostname(String value) {
        hostname = value;
    }

    /**
     * Get the Hostname property.
     *
     * @return The Hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Set the Port property.
     *
     * @param value The new value for Port
     */
    public void setPort(int value) {
        httpPort = value;
    }

    /**
     * Get the Port property.
     *
     * @return The Port
     */
    public int getPort() {
        return httpPort;
    }






    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String note(String h) {
        return getMessage(h, Constants.ICON_INFORMATION, true);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String progress(String h) {
        return getMessage(h, Constants.ICON_PROGRESS, false);
    }


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String warning(String h) {
        return getMessage(h, Constants.ICON_WARNING, true);
    }


    /**
     * _more_
     *
     * @param h _more_
     * @param buttons _more_
     *
     * @return _more_
     */
    public String question(String h, String buttons) {
        return getMessage(h + "<p><hr>" + buttons, Constants.ICON_QUESTION,
                          false);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public String error(String h) {
        return getMessage(h, Constants.ICON_ERROR, true);
    }


    /**
     * _more_
     *
     * @param h _more_
     * @param icon _more_
     * @param showClose _more_
     *
     * @return _more_
     */
    public String getMessage(String h, String icon, boolean showClose) {
        String close =
            HtmlUtil.jsLink(HtmlUtil.onMouseClick("hide('messageblock')"),
                            HtmlUtil.img(iconUrl(Constants.ICON_CLOSE)));
        if ( !showClose) {
            close = "&nbsp;";
        }
        h = "<div class=\"innernote\"><table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td valign=\"top\">"
            + HtmlUtil.img(iconUrl(icon)) + HtmlUtil.space(2)
            + "</td><td valign=\"bottom\"><span class=\"notetext\">" + h
            + "</span></td></tr></table></div>";
        return "\n<table border=\"0\" id=\"messageblock\"><tr><td><div class=\"note\"><table><tr valign=top><td>"
               + h + "</td><td>" + close + "</td></tr></table>"
               + "</div></td></tr></table>\n";
    }



    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public static String fileUrl(String f) {
        return urlBase + f;
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public String iconUrl(String f) {
        if(f==null) return null;
        String path = getProperty(f, f);
        return urlBase + path;
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
    public static String getUrlBase() {
        return urlBase;
    }




}


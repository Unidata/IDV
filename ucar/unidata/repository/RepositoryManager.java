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


import org.w3c.dom.*;




import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.*;

import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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



/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryManager implements RepositorySource, Constants,
                                          Tables, RequestHandler {




    /** _more_ */
    protected Repository repository;

    /** _more_          */
    private DataOutputHandler dataOutputHandler;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public RepositoryManager(Repository repository) {
        this.repository = repository;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return repository;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public RepositoryBase getRepositoryBase() {
        return repository;
    }




    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public DataOutputHandler getDataOutputHandler() throws Exception {
        if (dataOutputHandler == null) {
            dataOutputHandler =
                (DataOutputHandler) getRepository().getOutputHandler(
                    DataOutputHandler.OUTPUT_OPENDAP.toString());
        }
        return dataOutputHandler;
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param label _more_
     * @param value _more_
     */
    public static void addCriteria(StringBuffer sb, String label,
                                   Object value) {
        String sv = value.toString();
        sv = sv.replace("<", "&lt;");
        sv = sv.replace(">", "&gt;");
        sb.append("<tr valign=\"top\"><td align=right>");
        sb.append(HtmlUtil.b(label));
        sb.append("</td><td>");
        sb.append(sv);
        sb.append("</td></tr>");
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msg(String msg) {
        return Repository.msg(msg);
    }



    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msgLabel(String msg) {
        return Repository.msgLabel(msg);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected static String msgHeader(String h) {
        return Repository.msgHeader(h);
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String tableSubHeader(String s) {
        return HtmlUtil.row(HtmlUtil.colspan(subHeader(s), 2));
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String subHeader(String s) {
        return HtmlUtil.div(s, HtmlUtil.cssClass("pagesubheading"));
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     *
     * @return _more_
     */
    public String subHeaderLink(String url, String label) {
        return HtmlUtil.href(url, label,
                             HtmlUtil.cssClass("pagesubheadinglink"));
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     * @param toggle _more_
     *
     * @return _more_
     */
    public String subHeaderLink(String url, String label, boolean toggle) {
        //        if(true) return "x";
        String img = HtmlUtil.img(getRepository().fileUrl(toggle
                ? ICON_MINUS
                : ICON_PLUS));
        label = img + HtmlUtil.space(1) + label;
        String html = HtmlUtil.href(url, label,
                                    HtmlUtil.cssClass("pagesubheadinglink"));
        return html;
        //return "<table border=1><tr valign=bottom><td>" + html +"</table>";
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param d _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Date d) {
        return getRepository().formatDate(request, d);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param ms _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, long ms) {
        return getRepository().formatDate(request, ms);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public DatabaseManager getDatabaseManager() {
        return repository.getDatabaseManager();
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
        return repository.getProperty(name, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String name, boolean dflt) {
        return repository.getProperty(name, dflt);
    }


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String header(String h) {
        return repository.header(h);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected Admin getAdmin() {
        return repository.getAdmin();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected UserManager getUserManager() {
        return repository.getUserManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected ActionManager getActionManager() {
        return repository.getActionManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected AccessManager getAccessManager() {
        return repository.getAccessManager();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected MetadataManager getMetadataManager() {
        return repository.getMetadataManager();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected HarvesterManager getHarvesterManager() {
        return repository.getHarvesterManager();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected StorageManager getStorageManager() {
        return repository.getStorageManager();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param urls _more_
     *
     * @return _more_
     */
    protected List getSubNavLinks(Request request, RequestUrl[] urls) {
        return repository.getSubNavLinks(request, urls);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param message _more_
     */
    protected void log(Request request, String message) {
        repository.log(request, message);

    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    protected void log(String message, Throwable exc) {
        repository.log(message, exc);
    }

}


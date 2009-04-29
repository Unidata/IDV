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




import ucar.unidata.sql.Clause;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;



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
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.zip.*;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RegistryManager extends RepositoryManager {


    public static final String ARG_REGISTRY_SERVER = "registry.server";
    public static final String ARG_REGISTRY_CLIENT = "registry.client";


    /**
     * _more_
     *
     *
     * @param repository _more_
     *
     */
    public RegistryManager(Repository repository) {
        super(repository);
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void doFinalInitialization() throws Exception {
        if (isEnabledAsServer()) {
            Misc.run(new Runnable() {
                public void run() {
                    while (true) {
                        //Every one hour clean up the server collection
                        cleanupServers();
                        Misc.sleep(DateUtil.hoursToMillis(1));
                        //                        Misc.sleep(10000);
                    }
                }
            });
        }


        Misc.runInABit(5000, new Runnable() {
            public void run() {
                doFinalInitializationInner();
            }
        });

    }


    /**
     * _more_
     */
    private void cleanupServers() {
        if ( !isEnabledAsServer()) {
            return;
        }
        try {
            List<ServerInfo> servers = getRegistererServers();
            for (ServerInfo serverInfo : servers) {
                checkServer(serverInfo, true);
            }
        } catch (Exception exc) {
            logError("Cleaning up  servers", exc);
        }


    }


    /**
     * _more_
     */
    public void doFinalInitializationInner() {
        try {
            registerWithServers();
        } catch (Exception exc) {
            logError("Registering with servers", exc);
        }
    }


    /**
     * _more_
     */
    public void checkApi() {
        ApiMethod apiMethod = getRepository().getApiMethod("/registry/list");
        if (apiMethod != null) {
            apiMethod.setIsTopLevel(isEnabledAsServer());
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param csb _more_
     */
    public void addAdminConfig(Request request, StringBuffer csb) {
        csb.append(
            HtmlUtil.row(HtmlUtil.colspan(msgHeader("Server Registry"), 2)));

        csb.append(
            HtmlUtil.formEntry(
                "",
                HtmlUtil.checkbox(
                    PROP_REGISTRY_ENABLED, "true", isEnabledAsServer()) + " "
                        + msg("Enable this server to be a registry for other servers")));

        csb.append(
            HtmlUtil.formEntry(
                "", msg("Servers this server registers with:")));
        csb.append(
            HtmlUtil.formEntry(
                "",
                HtmlUtil.textArea(
                    PROP_REGISTRY_SERVERS,
                    getRepository().getProperty(
                        PROP_REGISTRY_SERVERS,
                        "http://motherlode.ucar.edu/repository"), 5, 60)));

    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyAdminConfig(Request request) throws Exception {
        List<String> oldList = getServersToRegisterWith();
        List<String> newList =
            StringUtil.split(request.getUnsafeString(PROP_REGISTRY_SERVERS,
                ""), "\n", true, true);
        

        getRepository().writeGlobal(PROP_REGISTRY_SERVERS,
                                    StringUtil.join("\n", newList));
        getRepository().writeGlobal(PROP_REGISTRY_ENABLED,
                                    request.get(PROP_REGISTRY_ENABLED, false)
                                    + "");
        checkApi();
        if(!newList.equals(oldList)) {
            for (String url : oldList) {
                newList.remove(url);
                Misc.run(this, "registerWithServer", url);
            }
            for (String url : newList) {
                Misc.run(this, "registerWithServer", url);
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabledAsServer() {
        return getRepository().getProperty(PROP_REGISTRY_ENABLED, false);
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<ServerInfo> getRegistererServers() throws Exception {
        List<ServerInfo> servers = new ArrayList<ServerInfo>();

        Statement stmt =
            getDatabaseManager().select(Tables.SERVERREGISTRY.COLUMNS,
                                        Tables.SERVERREGISTRY.NAME,
                                        (Clause) null);
        SqlUtil.Iterator iter     = SqlUtil.getIterator(stmt);
        List<Comment>    comments = new ArrayList();
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                URL    url   = new URL(results.getString(1));
                String title = results.getString(2);
                String desc  = results.getString(3);
                String email  = results.getString(4);
                
                servers.add(new ServerInfo(url.getHost(), url.getPort(), -1,
                                           url.getPath(), title, desc,email));
            }
        }
        return servers;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getServersToRegisterWith() {
        List<String> urls =
            StringUtil.split(
                getRepository().getProperty(
                    PROP_REGISTRY_SERVERS,
                    "http://motherlode.ucar.edu/repository"), "\n", true,
                        true);
        return urls;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void registerWithServers() throws Exception {
        List<String> urls = getServersToRegisterWith();
        for (String url : urls) {
            Misc.run(this, "registerWithServer", url);
        }
    }


    /**
     * _more_
     *
     * @param url _more_
     */
    public void registerWithServer(String url) {
        ServerInfo serverInfo = getRepository().getServerInfo();
        url = url + getRepository().URL_REGISTRY_ADD.getPath();
        url = HtmlUtil.url(url, ARG_REGISTRY_CLIENT, serverInfo.getUrl());
        try {
            String contents = IOUtil.readContents(url, getClass());
            Element root     = XmlUtil.getRoot(contents);
            if(!responseOk(root)) {
                logInfo("Failed to registered with:" + url);
                logInfo(XmlUtil.getChildText(root).trim());
            } else {
                logInfo("Registered with:" + url);
            }

        } catch (Exception exc) {
            logError("Error registering with:" + url, exc);
        }
    }


    public boolean responseOk(Element root) {
        return XmlUtil.getAttribute(root, ATTR_CODE).equals(CODE_OK);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processRegistryAdd(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        if ( !isEnabledAsServer()) {
            return new Result(
                XmlUtil.tag(
                    TAG_RESPONSE, XmlUtil.attr(ATTR_CODE, CODE_ERROR),
                    "Not enabled as a registry"), MIME_XML);
        }


        String     baseUrl    = request.getString(ARG_REGISTRY_CLIENT, "");

        ServerInfo serverInfo = new ServerInfo(new URL(baseUrl), "", "");

        if (checkServer(serverInfo, true)) {
            return new Result(XmlUtil.tag(TAG_RESPONSE,
                                          XmlUtil.attr(ATTR_CODE, CODE_OK),
                                          "OK"), MIME_XML);
        }
        return new Result(XmlUtil.tag(TAG_RESPONSE,
                                      XmlUtil.attr(ATTR_CODE, CODE_ERROR),
                                      "failed"), MIME_XML);

    }


    public Result processRegistryInfo(Request request) throws Exception {
        String server = request.getString(ARG_REGISTRY_SERVER,"");
        if(!getServersToRegisterWith().contains(server)) {
            logInfo("Was asked to register with a server that is not in our list:" + server);
            return new Result(
                XmlUtil.tag(
                    TAG_RESPONSE, XmlUtil.attr(ATTR_CODE, CODE_ERROR),
                    "Not registering with you"), MIME_XML);
        }
        return getRepository().processInfo(request);
    }


    /**
     * _more_
     *
     * @param serverInfo _more_
     * @param deleteOnFailure _more_
     *
     * @return _more_
     */
    private boolean checkServer(ServerInfo serverInfo,
                                boolean deleteOnFailure) throws Exception {

        String serverUrl =
            HtmlUtil.url(serverInfo.getUrl()
                         + getRepository().URL_REGISTRY_INFO.getPath(), new String[] {
                             ARG_RESPONSE,
                             RESPONSE_XML,
                             ARG_REGISTRY_SERVER,getRepository().getServerInfo().getUrl() });

        try {
            //TODO: check if the response is OK
            String  contents = IOUtil.readContents(serverUrl, getClass());
            Element root     = XmlUtil.getRoot(contents);
            if(responseOk(root)) {
                String  title = XmlUtil.getGrandChildText(root, ServerInfo.TAG_INFO_TITLE,"");
                String description = XmlUtil.getGrandChildText(root,
                                                               ServerInfo.TAG_INFO_DESCRIPTION,"");

                String email = XmlUtil.getGrandChildText(root,
                                                         ServerInfo.TAG_INFO_EMAIL,"");
                getDatabaseManager().delete(
                                            Tables.SERVERREGISTRY.NAME,
                                            Clause.eq(
                                                      Tables.SERVERREGISTRY.COL_URL, serverInfo.getUrl()));
                getDatabaseManager().executeInsert(Tables.SERVERREGISTRY.INSERT,
                                                   new Object[] { serverInfo.getUrl(),
                                                                  title, description,email });
                return true;
            } else {
                System.err.println("Response is not ok:" + contents);
            }
        } catch (Exception exc) {
            logError("Checking server:" + serverInfo,exc);
        }
        if (deleteOnFailure) {
            logInfo("Deleting server:" + serverInfo.getUrl());
            getDatabaseManager().delete(
                Tables.SERVERREGISTRY.NAME,
                Clause.eq(
                    Tables.SERVERREGISTRY.COL_URL, serverInfo.getUrl()));
        }
        return false;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processRegistryList(Request request) throws Exception {
        boolean responseAsXml = request.getString(ARG_RESPONSE,
                                                  "").equals(RESPONSE_XML);

        List<ServerInfo> servers = getRegistererServers();

        if(responseAsXml) {
            Document  resultDoc  = XmlUtil.makeDocument();
            Element resultRoot = XmlUtil.create(resultDoc, TAG_RESPONSE, null,
                                                new String[] { ATTR_CODE,
                                                               CODE_OK });


            for (ServerInfo serverInfo : servers) {
                resultRoot.appendChild(serverInfo.toXml(getRepository(), resultDoc));
            }
            return new Result(XmlUtil.toString(resultRoot,false),
                              MIME_XML);
        } 

        
        StringBuffer sb = new StringBuffer();
        sb.append(msgHeader("Registered Servers"));
        sb.append("<ul>");
        for (ServerInfo serverInfo : servers) {
            sb.append("<li> ");
            sb.append(serverInfo.getLabel());
            sb.append(HtmlUtil.space(1));
            sb.append(HtmlUtil.href(serverInfo.getUrl(),
                                    serverInfo.getUrl()));
            sb.append(HtmlUtil.br());
            sb.append(serverInfo.getDescription());
        }

        sb.append("</ul>");
        if (servers.size() == 0) {
            sb.append(msg("No servers are registered"));
        }
        Result result = new Result(msg("Registry List"), sb);
        return result;
    }


}


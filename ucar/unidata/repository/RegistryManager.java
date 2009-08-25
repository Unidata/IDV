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
import java.util.HashSet;
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


    /** _more_          */
    private Object REMOTE_MUTEX = new Object();

    /** _more_ */
    public static final String ARG_REGISTRY_RELOAD = "registry.reload";

    /** _more_          */
    public static final String ARG_REGISTRY_SERVER = "registry.server";

    /** _more_          */
    public static final String ARG_REGISTRY_SELECTED = "registry.selected";

    /** _more_ */
    public static final String ARG_REGISTRY_CLIENT = "registry.client";
    public static final String ARG_REGISTRY_ADD = "registry.add";

    public static final String ARG_REGISTRY_URL = "registry.url";


    /** _more_          */
    public RequestUrl URL_REGISTRY_REMOTESERVERS =
        new RequestUrl(this, "/admin/remoteservers", "Remote Servers");



    private List<ServerInfo> registeredServers;

    /** _more_          */
    private List<ServerInfo> remoteServers;
    private List<ServerInfo> selectedRemoteServers;


    /** _more_          */
    private Hashtable<String, ServerInfo> remoteServerMap;


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
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processAdminRemoteServers(Request request)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        if (request.exists(ARG_REGISTRY_ADD)) {
            if(request.defined(ARG_REGISTRY_URL)) {
                String url = request.getString(ARG_REGISTRY_URL,"");
                URL fullUrl = new URL(HtmlUtil.url(url+getRepository().URL_INFO.getPath(),
                                              new String[] { ARG_RESPONSE,
                                                             RESPONSE_XML }));

                try {
                    String contents = getStorageManager().readSystemResource(fullUrl.toString());
                    Element root = XmlUtil.getRoot(contents);
                    if(!responseOk(root)) {
                        sb.append(getRepository().showDialogError("Failed to read information from:" + fullUrl));
                    } else{
                        ServerInfo serverInfo = new ServerInfo(root);
                        serverInfo.setSelected(true);
                        addRemoteServer(serverInfo,true);
                        return new Result(request.url(URL_REGISTRY_REMOTESERVERS));
                    }
                } catch(Exception exc) {
                    sb.append(getRepository().showDialogError("Failed to read information from:" + fullUrl));
                }
            }

            sb.append(request.form(URL_REGISTRY_REMOTESERVERS, ""));
            sb.append(HtmlUtil.formTable());
            sb.append(HtmlUtil.p());

            sb.append(HtmlUtil.formEntry(msgLabel("URL"),
                                         HtmlUtil.input(ARG_REGISTRY_URL,request.getString(ARG_REGISTRY_URL,""),HtmlUtil.SIZE_60)));
            sb.append(HtmlUtil.formTableClose());
            sb.append(HtmlUtil.submit("Add New Server", ARG_REGISTRY_ADD));
            sb.append(HtmlUtil.submit("Cancel", ARG_CANCEL));
            sb.append(HtmlUtil.formClose());
            return getAdmin().makeResult(request, msg("Remote Servers"), sb);

        } else if (request.exists(ARG_REGISTRY_RELOAD)) {
            for (String server : getServersToRegisterWith()) {
                fetchRemoteServers(server);
            }
            checkApi();
        } else if (request.exists(ARG_SUBMIT)) {
            List allIds = request.get(ARG_REGISTRY_SERVER, new ArrayList());
            List selectedIds = request.get(ARG_REGISTRY_SELECTED,
                                           new ArrayList());
            Hashtable selected = new Hashtable();
            for (String id : (List<String>) selectedIds) {
                selected.put(id, id);
            }

            for (String id : (List<String>) allIds) {
                boolean isSelected = (selected.get(id) != null);
                getDatabaseManager().update(
                    Tables.REMOTESERVERS.NAME, Tables.REMOTESERVERS.COL_URL,
                    id, new String[] { Tables.REMOTESERVERS.COL_SELECTED },
                    new Object[] { new Boolean(isSelected) });

            }
            clearRemoteServers();
            checkApi();
        } else if (request.exists(ARG_DELETE)) {
            List selectedIds = request.get(ARG_REGISTRY_SELECTED,
                                           new ArrayList());
            for (String id : (List<String>) selectedIds) {
                getDatabaseManager().delete(
                    Tables.REMOTESERVERS.NAME,
                    Clause.eq(Tables.REMOTESERVERS.COL_URL, id));
            }
            clearRemoteServers();
            checkApi();
        }


        sb.append(HtmlUtil.p());
        sb.append(request.form(URL_REGISTRY_REMOTESERVERS, ""));
        sb.append(HtmlUtil.submit("Change Selected", ARG_SUBMIT));
        sb.append(HtmlUtil.submit("Delete Selected", ARG_DELETE));
        sb.append(HtmlUtil.submit("Add New Server", ARG_REGISTRY_ADD));
        sb.append(HtmlUtil.submit("Reload from Registry Servers",
                                  ARG_REGISTRY_RELOAD));
        sb.append(HtmlUtil.open(HtmlUtil.TAG_UL));
        List<ServerInfo> remoteServers = getRemoteServers();
        if (remoteServers.size() == 0) {
            sb.append(msg("No remote servers"));
        } else {
            sb.append(msg("Use selected in search"));
        }
        sb.append(HtmlUtil.br());

        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE));
        int idCnt = 0;

        for (ServerInfo serverInfo : remoteServers) {
            sb.append(HtmlUtil.hidden(ARG_REGISTRY_SERVER,
                                      serverInfo.getId()));
            String cbxId = ARG_REGISTRY_SELECTED + "_" + (idCnt++);
            String call = HtmlUtil.attr(
                              HtmlUtil.ATTR_ONCLICK,
                              HtmlUtil.call(
                                  "checkboxClicked",
                                  HtmlUtil.comma(
                                      "event",
                                      HtmlUtil.squote(ARG_REGISTRY_SELECTED),
                                      HtmlUtil.squote(cbxId))));


            sb.append(
                HtmlUtil.row(
                    HtmlUtil.cols(
                        HtmlUtil.checkbox(
                            ARG_REGISTRY_SELECTED, serverInfo.getId(),
                            serverInfo.getSelected(),
                            HtmlUtil.id(cbxId)
                            + call) + serverInfo.getLabel(), HtmlUtil.space(
                                4), HtmlUtil.href(
                                serverInfo.getUrl(), serverInfo.getUrl()))));
        }

        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));
        sb.append(HtmlUtil.close(HtmlUtil.TAG_UL));
        sb.append(HtmlUtil.submit("Change Selected", ARG_SUBMIT));
        sb.append(HtmlUtil.submit("Delete Selected", ARG_DELETE));
        sb.append(HtmlUtil.submit("Add New Server", ARG_REGISTRY_ADD));
        sb.append(HtmlUtil.submit("Reload from Registry Servers",
                                  ARG_REGISTRY_RELOAD));
        sb.append(HtmlUtil.formClose());


        return getAdmin().makeResult(request, msg("Remote Servers"), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    protected void addToInstallForm(Request request, StringBuffer sb)
            throws Exception {
        String msg =  msg("Servers this server registers with:");
        msg  = HtmlUtil.space(1) + HtmlUtil.href("http://www.unidata.ucar.edu/software/ramadda/docs/userguide/remoteservers.html",msg("Help"),
                                                 HtmlUtil.attr(HtmlUtil.ATTR_TARGET,"_help"));
        sb.append(
            HtmlUtil.formEntry(
                msgLabel("Registry Servers"),
                msg+HtmlUtil.br() +
                HtmlUtil.textArea(
                    PROP_REGISTRY_SERVERS,
                    getRepository().getProperty(
                        PROP_REGISTRY_SERVERS,
                        getRepository().getProperty(
                            PROP_REGISTRY_DEFAULTSERVER,
                            "http://motherlode.ucar.edu/repository")), 5,
                                60)));
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    protected void applyInstallForm(Request request) throws Exception {
        List<String> newList =
            StringUtil.split(request.getUnsafeString(PROP_REGISTRY_SERVERS,
                ""), "\n", true, true);


        getRepository().writeGlobal(PROP_REGISTRY_SERVERS,
                                    StringUtil.join("\n", newList));
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void doFinalInitialization() throws Exception {
        //        Misc.printStack("doFinal");
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
            List<ServerInfo> servers = getRegisteredServers();
            for (ServerInfo serverInfo : servers) {
                checkServer(serverInfo, true);
            }
        } catch (Exception exc) {
            logError("RegistryManager.cleanUpServers:", exc);
        }


    }


    /**
     * _more_
     */
    public void doFinalInitializationInner() {
        try {
            registerWithServers();
        } catch (Exception exc) {
            logError("RegistryManager.doFinalInitialization: Registering with servers", exc);
        }
    }





    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void checkApi() throws Exception {
        ApiMethod apiMethod = getRepository().getApiMethod("/registry/list");
        if (apiMethod != null) {
            apiMethod.setIsTopLevel((isEnabledAsServer() &&
                                     getRegisteredServers().size()>0)
                                    || (getSelectedRemoteServers().size()
                                        > 0));
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param csb _more_
     */
    public void addAdminConfig(Request request, StringBuffer csb) {
        String helpLink = HtmlUtil.href("/repository/help/remoteservers.html",msg("Help"), HtmlUtil.attr(HtmlUtil.ATTR_TARGET,"_help"));
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
                               "", msg("Servers this server registers with:")+HtmlUtil.space(2) + helpLink));
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
        if ( !newList.equals(oldList)) {
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
    public List<ServerInfo> getRegisteredServers() throws Exception {
        List<ServerInfo> servers = registeredServers;
        if(servers!=null) return servers;
        servers = new ArrayList<ServerInfo>();

        Statement stmt =
            getDatabaseManager().select(Tables.SERVERREGISTRY.COLUMNS,
                                        Tables.SERVERREGISTRY.NAME,
                                        (Clause) null,
                                        " order by "
                                        + Tables.SERVERREGISTRY.COL_URL);
        SqlUtil.Iterator iter     = SqlUtil.getIterator(stmt);
        List<Comment>    comments = new ArrayList();
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                URL     url        = new URL(results.getString(1));
                String  title      = results.getString(2);
                String  desc       = results.getString(3);
                String  email      = results.getString(4);
                boolean isRegistry = results.getInt(5) != 0;
                servers.add(new ServerInfo(url.getHost(), url.getPort(), -1,
                                           url.getPath(), title, desc, email,
                                           isRegistry, false));
            }
        }
        registeredServers = servers;
        return servers;
    }



    private void clearRegisteredServers() {
        registeredServers = null;
    }


    /**
     * _more_
     */
    private void clearRemoteServers() {
        selectedRemoteServers = null;
        remoteServers   = null;
        remoteServerMap = null;

    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public ServerInfo findRemoteServer(String id) throws Exception {
        if (id.equals(ServerInfo.ID_THIS)) {
            ServerInfo serverInfo = getRepository().getServerInfo();
            serverInfo.setSelected(true);
            return serverInfo;
        }
        return getRemoteServerMap().get(id);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<ServerInfo> getSelectedRemoteServers() throws Exception {
        List<ServerInfo> selected = selectedRemoteServers;
        if(selected!=null) return selected;
        selected = new ArrayList<ServerInfo>();
        for (ServerInfo serverInfo : getRemoteServers()) {
            if (serverInfo.getSelected()) {
                selected.add(serverInfo);
            }
        }
        selectedRemoteServers = selected;
        return selected;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Hashtable<String, ServerInfo> getRemoteServerMap()
            throws Exception {
        Hashtable<String, ServerInfo> map = remoteServerMap;
        while (map == null) {
            getRemoteServers();
            map = remoteServerMap;
        }
        return map;
    }



    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private ServerInfo makeRemoteServer(ResultSet results) throws Exception {
        URL     url        = new URL(results.getString(1));
        String  title      = results.getString(2);
        String  desc       = results.getString(3);
        String  email      = results.getString(4);
        boolean isRegistry = results.getInt(5) != 0;
        boolean isSelected = results.getInt(6) != 0;

        ServerInfo serverInfo = new ServerInfo(url.getHost(), url.getPort(),
                                    -1, url.getPath(), title, desc, email,
                                    isRegistry, isSelected);
        return serverInfo;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<ServerInfo> getRemoteServers() throws Exception {
        List<ServerInfo> servers = remoteServers;
        if (servers != null) {
            return servers;
        }
        servers = new ArrayList<ServerInfo>();
        Hashtable<String, ServerInfo> map = new Hashtable<String,
                                                ServerInfo>();

        Statement stmt =
            getDatabaseManager().select(Tables.REMOTESERVERS.COLUMNS,
                                        Tables.REMOTESERVERS.NAME,
                                        (Clause) null,
                                        " order by "
                                        + Tables.REMOTESERVERS.COL_URL);
        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                ServerInfo serverInfo = makeRemoteServer(results);
                map.put(serverInfo.getId(), serverInfo);
                servers.add(serverInfo);
            }
        }
        clearRemoteServers();
        remoteServerMap = map;
        remoteServers   = servers;
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
    public void registerWithServer(String url) throws Exception {
        ServerInfo serverInfo = getRepository().getServerInfo();
        url = url + getRepository().URL_REGISTRY_ADD.getPath();
        URL theUrl = new URL(HtmlUtil.url(url, ARG_REGISTRY_CLIENT, serverInfo.getUrl()));
        try {
            String  contents = getStorageManager().readSystemResource(theUrl);
            Element root     = XmlUtil.getRoot(contents);
            if ( !responseOk(root)) {
                logInfo("RegistryManager.registerWithServer: Failed to register with:" + theUrl);
                //                logInfo(XmlUtil.getChildText(root).trim());
            } else {
                logInfo("RegistryManager.registerWithServer: Registered with:" + theUrl);
            }

        } catch (Exception exc) {
            logError("RegistryManager.registerWithServer: Error registering with:" + theUrl, exc);
        }
    }


    /**
     * _more_
     *
     * @param root _more_
     *
     * @return _more_
     */
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
            logInfo("RegistryManager.processRegistryAdd: Was asked to register a server when this server is not configured as a registry server. URL = " + request);
            return new Result(
                XmlUtil.tag(
                    TAG_RESPONSE, XmlUtil.attr(ATTR_CODE, CODE_ERROR),
                    "Not enabled as a registry"), MIME_XML);
        }


        String     baseUrl    = request.getString(ARG_REGISTRY_CLIENT, "");

        ServerInfo serverInfo = new ServerInfo(new URL(baseUrl), "", "");

        logInfo("RegistryManager.processRegistryAdd: calling checkServer url=" + baseUrl);
        if (checkServer(serverInfo, true)) {
            return new Result(XmlUtil.tag(TAG_RESPONSE,
                                          XmlUtil.attr(ATTR_CODE, CODE_OK),
                                          "OK"), MIME_XML);
        }
        return new Result(XmlUtil.tag(TAG_RESPONSE,
                                      XmlUtil.attr(ATTR_CODE, CODE_ERROR),
                                      "failed"), MIME_XML);

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
    public Result processRegistryInfo(Request request) throws Exception {
        final String requestingServer = request.getString(ARG_REGISTRY_SERVER, "");
        URL requestingServerUrl = new URL(requestingServer);
        List<String> servers =   getServersToRegisterWith();
        boolean ok = false;
        for(String myServer: servers) {
            URL myServerUrl = new URL(myServer);
            if(myServerUrl.getHost().toLowerCase().equals(requestingServerUrl.getHost().toLowerCase()) &&
               myServerUrl.getPort()== requestingServerUrl.getPort()) {
                ok = true;
                break;
            }
        }

        if (!ok) {
            logInfo(
                "RegistryManger.processRegistryInfo: Was asked to register with a server:" + requestingServer +" that is not in our list:"+
                servers);
            return new Result(
                XmlUtil.tag(
                    TAG_RESPONSE, XmlUtil.attr(ATTR_CODE, CODE_ERROR),
                    "Not registering with you"), MIME_XML);
        }
        Misc.run(new Runnable() {
            public void run() {
                try {
                    fetchRemoteServers(requestingServer);
                } catch (Exception exc) {
                    logError("RegistryManager.processRegistryInfo: Loading servers from:" + requestingServer, exc);
                }
            }
        });

        return getRepository().processInfo(request);
    }


    /**
     * _more_
     *
     * @param serverUrl _more_
     *
     * @throws Exception _more_
     */
    private void fetchRemoteServers(String serverUrl) throws Exception {
        serverUrl = serverUrl + getRepository().URL_REGISTRY_LIST.getPath();
        serverUrl = HtmlUtil.url(serverUrl, ARG_RESPONSE, RESPONSE_XML);
        String  contents = getStorageManager().readSystemResource(new URL(serverUrl));
        Element root     = XmlUtil.getRoot(contents);

        if ( !responseOk(root)) {
            logInfo("RegistryManager.fetchRemoteServers: Bad response from " + serverUrl);
            return;
        }
        List<ServerInfo> servers  = new ArrayList<ServerInfo>();
        ServerInfo       me       = getRepository().getServerInfo();
        NodeList         children = XmlUtil.getElements(root);
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            servers.add(new ServerInfo(node));
        }


        Hashtable<String, ServerInfo> map = getRemoteServerMap();
        for (ServerInfo serverInfo : servers) {
            if (serverInfo.equals(me)) {
                continue;
            } else {}
            ServerInfo oldServer = map.get(serverInfo.getId());
            if(oldServer!=null) {
                serverInfo.setSelected(oldServer.getSelected());
            }
            addRemoteServer(serverInfo,oldServer!=null);
       }
    }


    private void addRemoteServer(ServerInfo serverInfo, boolean deleteOldServer) throws Exception {
        if (deleteOldServer) {
            getDatabaseManager().delete(
                                        Tables.REMOTESERVERS.NAME,
                                        Clause.eq(
                                                  Tables.REMOTESERVERS.COL_URL, serverInfo.getUrl()));
        }

        
        getDatabaseManager().executeInsert(Tables.REMOTESERVERS.INSERT,
                                           new Object[] {
                                               serverInfo.getUrl(), serverInfo.getTitle(),
                                               serverInfo.getDescription(), serverInfo.getEmail(),
                                               new Boolean(serverInfo.getIsRegistry()),
                                               new Boolean(serverInfo.getSelected())
                                           });
        clearRemoteServers();
    }


    /**
     * _more_
     *
     * @param serverInfo _more_
     * @param deleteOnFailure _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean checkServer(ServerInfo serverInfo,
                                boolean deleteOnFailure)
            throws Exception {

        String serverUrl =
            HtmlUtil.url(
                serverInfo.getUrl()
                + getRepository().URL_REGISTRY_INFO.getPath(), new String[] {
                    ARG_RESPONSE,
                    RESPONSE_XML, ARG_REGISTRY_SERVER,
                    getRepository().getServerInfo().getUrl() });

        try {
            String  contents = getStorageManager().readSystemResource(new URL(serverUrl));
            Element root     = XmlUtil.getRoot(contents);
            if (responseOk(root)) {
                ServerInfo clientServer = new ServerInfo(root);
                if (clientServer.equals(serverInfo)) {
                    logInfo("RegistryManager.checkServer: adding server " + serverUrl);
                    getDatabaseManager().delete(Tables.SERVERREGISTRY.NAME,
                            Clause.eq(Tables.SERVERREGISTRY.COL_URL,
                                      serverInfo.getUrl()));
                    getDatabaseManager().executeInsert(
                        Tables.SERVERREGISTRY.INSERT,
                        new Object[] { clientServer.getUrl(),
                                       clientServer.getTitle(),
                                       clientServer.getDescription(),
                                       clientServer.getEmail(),
                                       new Boolean(
                                           clientServer.getIsRegistry()) });
                    clearRegisteredServers();
                    return true;
                } else {
                    logInfo("RegistryManager.checkServer: not equals:" + serverInfo.getId()
                            + " " + clientServer.getId());

                }
            } else {
                logInfo("RegistryManager.checkServer: response not ok from:" + serverInfo +" with url: " +serverUrl +"response:\n" +contents);
            }
        } catch (Exception exc) {
            logError("RegistryManager.checkServer: Could not fetch server xml from:" + serverInfo +" with url:" + serverUrl, exc);
        }
        if (deleteOnFailure) {
            logInfo("RegistryManager.checkServer: Deleting server:" + serverInfo.getUrl());
            getDatabaseManager().delete(
                Tables.SERVERREGISTRY.NAME,
                Clause.eq(
                    Tables.SERVERREGISTRY.COL_URL, serverInfo.getUrl()));
            clearRegisteredServers();
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

        List<ServerInfo> registeredServers =  getRegisteredServers();
        List<ServerInfo> remoteServers =   getSelectedRemoteServers();
        HashSet<ServerInfo> seen  = new HashSet<ServerInfo>();
        if (responseAsXml) {
            List<ServerInfo> servers  = registeredServers;
            //Add myself to the list
            servers.add(0, getRepository().getServerInfo());
            Document resultDoc = XmlUtil.makeDocument();
            Element resultRoot = XmlUtil.create(resultDoc, TAG_RESPONSE,
                                     null, new String[] { ATTR_CODE,
                    CODE_OK });


            for (ServerInfo serverInfo : servers) {
                resultRoot.appendChild(serverInfo.toXml(getRepository(),
                        resultDoc));
            }
            return new Result(XmlUtil.toString(resultRoot, false), MIME_XML);
        }


        
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<2;i++) {
            boolean evenRow = false;
            boolean didone = false;
            List<ServerInfo> servers  = (i==0?registeredServers:remoteServers);
            for (ServerInfo serverInfo : servers) {
                if(seen.contains(serverInfo)) continue;
                if(!didone) {
                    sb.append(HtmlUtil.p());
                    if (i==0) {
                        sb.append(msgHeader("Registered Servers"));
                    } else {
                        sb.append(msgHeader("Remote Servers"));
                    }
                    sb.append("<table cellspacing=\"0\" cellpadding=\"4\">");
                    sb.append(HtmlUtil.row(HtmlUtil.headerCols(new String[] {
                        msg("Repository"),
                        msg("URL"), msg("Is Registry?") })));
                }
                didone = true;
                seen.add(serverInfo);
                sb.append(HtmlUtil.row(HtmlUtil.cols(new String[] {
                    serverInfo.getLabel(),
                    HtmlUtil.href(serverInfo.getUrl(), serverInfo.getUrl()),
                    (serverInfo.getIsRegistry()
                     ? msg("Yes")
                     : msg("No")) }), HtmlUtil.cssClass(evenRow
                                                        ? "listrow1"
                                                        : "listrow2")));
                String desc = serverInfo.getDescription();
                if ((desc != null) && (desc.trim().length() > 0)) {
                    desc = HtmlUtil.makeShowHideBlock(msg("Description"), desc,
                                                      false);
                    sb.append(HtmlUtil.row(HtmlUtil.colspan(desc, 3),
                                           HtmlUtil.cssClass(evenRow
                                                             ? "listrow1"
                                                             : "listrow2")));
                }
                evenRow = !evenRow;
            }
            if(didone) {
                sb.append("</table>");
            }
            if(isEnabledAsServer()) {
                if (i==0 && !didone) {
                    sb.append(msg("No servers are registered"));
                }
            }
        }
        Result result = new Result(msg("Registry List"), sb);
        return result;
    }


}


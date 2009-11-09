/**
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

import ucar.unidata.repository.auth.*;

import ucar.unidata.repository.metadata.*;

import ucar.unidata.repository.output.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.sql.Clause;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.PluginClassLoader;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;



import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import java.util.jar.*;



import java.util.regex.*;
import java.util.zip.*;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class SearchManager extends RepositoryManager {

    /** _more_ */
    public static final String ARG_SEARCH_SUBMIT = "search.submit";

    /** _more_ */
    public static final String ARG_SEARCH_SUBSET = "search.subset";

    /** _more_ */
    public static final String ARG_SEARCH_SERVERS = "search.servers";


    /**
     * _more_
     *
     * @param repository _more_
     */
    public SearchManager(Repository repository) {
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
    public Result processEntryTextSearchForm(Request request)
            throws Exception {
        return makeSearchForm(request, true, false);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RequestUrl[] getSearchUrls() throws Exception {
        if (getRegistryManager().getSelectedRemoteServers().size() > 0) {
            return getRepository().remoteSearchUrls;
        }
        return getRepository().searchUrls;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param justText _more_
     * @param typeSpecific _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeSearchForm(Request request, boolean justText,
                                 boolean typeSpecific)
            throws Exception {

        StringBuffer sb          = new StringBuffer();
        TypeHandler  typeHandler = getRepository().getTypeHandler(request);

        sb.append(
            HtmlUtil.form(
                request.url(
                    getRepository().URL_ENTRY_SEARCH, ARG_NAME,
                    WHAT_ENTRIES), " name=\"searchform\" "));


        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HtmlUtil.submitImage(iconUrl(ICON_BLANK),
                                       ARG_SEARCH_SUBMIT));

        String what = (String) request.getWhat(BLANK);
        if (what.length() == 0) {
            what = WHAT_ENTRIES;
        }


        List<ServerInfo> servers =
            getRegistryManager().getSelectedRemoteServers();

        String buttons;


        if (servers.size() > 0) {
            buttons =
                RepositoryUtil.buttons(
                    HtmlUtil.submit(
                        msg("Search this Repository"),
                        ARG_SEARCH_SUBMIT), HtmlUtil.submit(
                            msg("Search Remote Repositories"),
                            ARG_SEARCH_SERVERS));
        } else {
            buttons = HtmlUtil.submit(msg("Search"), ARG_SEARCH_SUBMIT);
        }
        sb.append(HtmlUtil.p());
        if ( !justText) {
            sb.append(buttons);
            sb.append(HtmlUtil.p());
        }


        if (justText) {
            sb.append(
                "<table width=\"100%\" border=\"0\"><tr><td width=\"60\">");
            typeHandler.addTextSearch(request, sb);
            sb.append("</table>");
            sb.append(HtmlUtil.p());
        } else {
            Object       oldValue = request.remove(ARG_RELATIVEDATE);
            List<Clause> where    = typeHandler.assembleWhereClause(request);
            if (oldValue != null) {
                request.put(ARG_RELATIVEDATE, oldValue);
            }

            typeHandler.addToSearchForm(request, sb, where, true);


            StringBuffer metadataSB = new StringBuffer();
            metadataSB.append(HtmlUtil.formTable());
            getMetadataManager().addToSearchForm(request, metadataSB);
            metadataSB.append(HtmlUtil.formTableClose());
            sb.append(HtmlUtil.makeShowHideBlock(msg("Metadata"),
                    metadataSB.toString(), false));



            StringBuffer outputForm = new StringBuffer(HtmlUtil.formTable());
            if (request.defined(ARG_OUTPUT)) {
                OutputType output = request.getOutput(BLANK);
                outputForm.append(HtmlUtil.hidden(ARG_OUTPUT,
                        output.getId().toString()));
            }

            List orderByList = new ArrayList();
            orderByList.add(new TwoFacedObject(msg("None"), "none"));
            orderByList.add(new TwoFacedObject(msg("From Date"), "fromdate"));
            orderByList.add(new TwoFacedObject(msg("To Date"), "todate"));
            orderByList.add(new TwoFacedObject(msg("Create Date"),
                    "createdate"));
            orderByList.add(new TwoFacedObject(msg("Name"), "name"));

            String orderBy = HtmlUtil.select(
                                 ARG_ORDERBY, orderByList,
                                 request.getString(
                                     ARG_ORDERBY,
                                     "none")) + HtmlUtil.checkbox(
                                         ARG_ASCENDING, "true",
                                         request.get(
                                             ARG_ASCENDING,
                                             false)) + HtmlUtil.space(1)
                                                 + msg("ascending");
            outputForm.append(HtmlUtil.formEntry(msgLabel("Order By"),
                    orderBy));
            outputForm.append(HtmlUtil.formEntry(msgLabel("Output"),
                    HtmlUtil.select(ARG_OUTPUT, getOutputHandlerSelectList(),
                                    request.getString(ARG_OUTPUT, ""))));

            outputForm.append(HtmlUtil.formTableClose());




            sb.append(HtmlUtil.makeShowHideBlock(msg("Output"),
                    outputForm.toString(), false));

        }

        if (servers.size() > 0) {
            StringBuffer serverSB  = new StringBuffer();
            int          serverCnt = 0;
            String       cbxId;
            String       call;

            cbxId = ATTR_SERVER + (serverCnt++);
            call = HtmlUtil.attr(HtmlUtil.ATTR_ONCLICK,
                                 HtmlUtil.call("checkboxClicked",
                                     HtmlUtil.comma("event",
                                         HtmlUtil.squote(ATTR_SERVER),
                                         HtmlUtil.squote(cbxId))));

            serverSB.append(HtmlUtil.checkbox(ARG_DOFRAMES, "true",
                    request.get(ARG_DOFRAMES, false)));
            serverSB.append(msg("Do frames"));
            serverSB.append(HtmlUtil.br());
            serverSB.append(HtmlUtil.checkbox(ATTR_SERVER,
                    ServerInfo.ID_THIS, false, HtmlUtil.id(cbxId) + call));
            serverSB.append(msg("Include this repository"));
            serverSB.append(HtmlUtil.br());
            for (ServerInfo server : servers) {
                cbxId = ATTR_SERVER + (serverCnt++);
                call = HtmlUtil.attr(HtmlUtil.ATTR_ONCLICK,
                                     HtmlUtil.call("checkboxClicked",
                                         HtmlUtil.comma("event",
                                             HtmlUtil.squote(ATTR_SERVER),
                                             HtmlUtil.squote(cbxId))));
                serverSB.append(HtmlUtil.checkbox(ATTR_SERVER,
                        server.getId(), false, HtmlUtil.id(cbxId) + call));
                serverSB.append(HtmlUtil.space(1));
                serverSB.append(server.getHref(" target=\"server\" "));
                serverSB.append(HtmlUtil.br());
            }
            sb.append(
                HtmlUtil.makeShowHideBlock(
                    msg("Remote Search Settings"),
                    HtmlUtil.div(
                        serverSB.toString(),
                        HtmlUtil.cssClass("serverdiv")), false));
        }




        sb.append(HtmlUtil.p());
        sb.append(buttons);
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.formClose());

        return getRepository().makeResult(request, msg("Search Form"), sb,
                                          getSearchUrls());


    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List getOutputHandlerSelectList() {
        List tfos = new ArrayList<TwoFacedObject>();
        for (OutputHandler outputHandler : getRepository()
                .getOutputHandlers()) {
            for (OutputType type : outputHandler.getTypes()) {
                if (type.getIsForSearch()) {
                    tfos.add(new HtmlUtil.Selector(type.getLabel(),
                            type.getId(),
                            getRepository().iconUrl(type.getIcon())));
                }
            }
        }
        return tfos;
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
    public Result processEntrySearchForm(Request request) throws Exception {
        return makeSearchForm(request, false, false);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param includeThis _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<ServerInfo> findServers(Request request, boolean includeThis)
            throws Exception {
        List<ServerInfo> servers = new ArrayList<ServerInfo>();
        for (String id : (List<String>) request.get(ATTR_SERVER,
                new ArrayList())) {
            if (id.equals(ServerInfo.ID_THIS) && !includeThis) {
                continue;
            }
            ServerInfo server = getRegistryManager().findRemoteServer(id);
            if (server == null) {
                continue;
            }
            servers.add(server);
        }
        return servers;
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
    public Result processRemoteSearch(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        List<String> servers = (List<String>) request.get(ATTR_SERVER,
                                   new ArrayList());
        sb.append(HtmlUtil.p());
        request.remove(ATTR_SERVER);

        boolean      didone   = false;
        StringBuffer serverSB = new StringBuffer();
        for (String id : servers) {
            ServerInfo server = getRegistryManager().findRemoteServer(id);
            if (server == null) {
                continue;
            }
            if ( !didone) {
                sb.append(header(msg("Selected Servers")));
            }
            serverSB.append(server.getHref(" target=\"server\" "));
            serverSB.append(HtmlUtil.br());
            didone = true;
        }

        if ( !didone) {
            sb.append(
                getRepository().showDialogNote(msg("No servers selected")));
        } else {
            sb.append(HtmlUtil.div(serverSB.toString(),
                                   HtmlUtil.cssClass("serverblock")));
            sb.append(HtmlUtil.p());
        }
        sb.append(HtmlUtil.p());
        sb.append(header(msg("Search Results")));
        return getRepository().makeResult(request, msg("Remote Search"), sb,
                                          getSearchUrls());

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
    public Result processEntryBrowseSearchForm(Request request)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        getMetadataManager().addToBrowseSearchForm(request, sb);
        return getRepository().makeResult(request, msg("Search Form"), sb,
                                          getSearchUrls());
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
    public Result processEntrySearch(Request request) throws Exception {


        if (request.get(ARG_WAIT, false)) {
            return getRepository().getMonitorManager().processEntryListen(
                request);
        }

        //        System.err.println("submit:" + request.getString("submit","YYY"));
        if (request.defined("submit_type.x")
                || request.defined(ARG_SEARCH_SUBSET)) {
            request.remove(ARG_OUTPUT);
            return processEntrySearchForm(request);
        }

        String what = request.getWhat(WHAT_ENTRIES);
        if ( !what.equals(WHAT_ENTRIES)) {
            Result result = getRepository().processListShow(request);
            if (result == null) {
                throw new IllegalArgumentException("Unknown list request: "
                        + what);
            }
            //  result.putProperty(PROP_NAVSUBLINKS,
            //                               getSearchFormLinks(request, what));
            return result;
        }


        StringBuffer     searchCriteriaSB = new StringBuffer();
        boolean          searchThis       = true;
        List<ServerInfo> servers          = null;

        ServerInfo       thisServer       = getRepository().getServerInfo();
        boolean          doFrames         = request.get(ARG_DOFRAMES, false);

        if (request.exists(ARG_SEARCH_SERVERS)) {
            servers = findServers(request, true);
            if (servers.size() == 0) {
                servers = getRegistryManager().getSelectedRemoteServers();
            }
            if (request.defined(ATTR_SERVER)) {
                searchThis = servers.contains(thisServer);
                if ( !doFrames) {
                    servers.remove(thisServer);
                }
            }
            if (servers.size() > 100) {
                throw new IllegalArgumentException("Too many remote servers:"
                        + servers.size());
            }
        }








        List<Group> groups  = new ArrayList<Group>();
        List<Entry> entries = new ArrayList<Entry>();

        if (searchThis) {
            List[] pair = getEntryManager().getEntries(request,
                              searchCriteriaSB);
            groups.addAll((List<Group>) pair[0]);
            entries.addAll((List<Entry>) pair[1]);
        }



        if ((servers != null) && (servers.size() > 0)) {
            request.remove(ATTR_SERVER);
            request.remove(ARG_SEARCH_SERVERS);

            if (doFrames) {
                String linkUrl = request.getUrlArgs();
                request.put(ARG_DECORATE, "false");
                request.put(ATTR_TARGET, "_server");
                String       embeddedUrl = request.getUrlArgs();
                StringBuffer sb          = new StringBuffer();
                sb.append(msgHeader("Remote Server Search Results"));
                for (ServerInfo server : servers) {
                    String remoteSearchUrl =
                        server.getUrl()
                        + getRepository().URL_ENTRY_SEARCH.getPath() + "?"
                        + linkUrl;
                    sb.append("\n");
                    sb.append(HtmlUtil.p());
                    String link = HtmlUtil.href(remoteSearchUrl,
                                      server.getUrl());
                    String fullUrl =
                        server.getUrl()
                        + getRepository().URL_ENTRY_SEARCH.getPath() + "?"
                        + embeddedUrl;
                    String content =
                        HtmlUtil.tag(
                            HtmlUtil.TAG_IFRAME,
                            HtmlUtil.attrs(
                                HtmlUtil.ATTR_WIDTH, "100%",
                                HtmlUtil.ATTR_HEIGHT, "200",
                                HtmlUtil.ATTR_SRC,
                                fullUrl), "need to have iframe support");
                    sb.append(HtmlUtil.makeShowHideBlock(server.getLabel()
                            + HtmlUtil.space(2) + link, content, true));

                    sb.append("\n");
                }
                request.remove(ARG_DECORATE);
                request.remove(ARG_TARGET);
                return new Result("Remote Search Results", sb);
            }


            Group tmpGroup = getEntryManager().getDummyGroup();
            doDistributedSearch(request, servers, tmpGroup, groups, entries);

            Result result = getRepository().getOutputHandler(
                                request).outputGroup(
                                request, tmpGroup, groups, entries);
            return result;

        }




        Group theGroup = null;

        if (request.defined(ARG_GROUP)) {
            String groupId = (String) request.getString(ARG_GROUP, "").trim();
            //            System.err.println("group:" + groupId);
            theGroup = getEntryManager().findGroup(request, groupId);
        }



        String s = searchCriteriaSB.toString();
        if (request.defined(ARG_TARGET)) {
            s = "";
        }

        if (s.length() > 0) {
            request.remove(ARG_SEARCH_SUBMIT);
            String url = request.getUrl(getRepository().URL_SEARCH_FORM);
            s = "<table>" + s + "</table>";
            String header = HtmlUtil.href(
                                url,
                                HtmlUtil.img(
                                    iconUrl(ICON_SEARCH),
                                    "Search Again")) + "Search Criteria";
            request.setLeftMessage(HtmlUtil.br(header) + s);
        }
        if (theGroup == null) {
            theGroup = getEntryManager().getDummyGroup();
        }
        Result result =
            getRepository().getOutputHandler(request).outputGroup(request,
                                             theGroup, groups, entries);
        return getEntryManager().addEntryHeader(request, theGroup, result);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param servers _more_
     * @param tmpGroup _more_
     * @param groups _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    private void doDistributedSearch(final Request request,
                                     List<ServerInfo> servers,
                                     Group tmpGroup,
                                     final List<Group> groups,
                                     final List<Entry> entries)
            throws Exception {

        String output = request.getString(ARG_OUTPUT, "");
        request.put(ARG_OUTPUT, XmlOutputHandler.OUTPUT_XML);
        final String    linkUrl     = request.getUrlArgs();
        ServerInfo      thisServer  = getRepository().getServerInfo();
        final int[]     runnableCnt = { 0 };
        final boolean[] running     = { true };
        //TODO: We need to cap the number of servers we're searching on
        List<Runnable> runnables = new ArrayList<Runnable>();
        for (ServerInfo server : servers) {
            if (server.equals(thisServer)) {
                continue;
            }
            final Group parentGroup =
                new Group(getRepository().getGroupTypeHandler(), true);
            parentGroup.setId(
                getEntryManager().getRemoteEntryId(server.getUrl(), ""));
            getEntryManager().cacheEntry(parentGroup);
            parentGroup.setRemoteServer(server.getUrl());
            parentGroup.setIsRemoteEntry(true);
            parentGroup.setUser(getUserManager().getAnonymousUser());
            parentGroup.setParentGroup(tmpGroup);
            parentGroup.setName(server.getUrl());
            final ServerInfo theServer = server;
            Runnable         runnable  = new Runnable() {
                public void run() {
                    String remoteSearchUrl =
                        theServer.getUrl()
                        + getRepository().URL_ENTRY_SEARCH.getPath() + "?"
                        + linkUrl;

                    try {
                        String entriesXml =
                            getStorageManager().readSystemResource(
                                new URL(remoteSearchUrl));
                        //                            System.err.println(entriesXml);
                        if ( !running[0]) {
                            return;
                        }
                        Element  root     = XmlUtil.getRoot(entriesXml);
                        NodeList children = XmlUtil.getElements(root);
                        //Synchronize on the groups list so only one thread at  a time adds its entries to it
                        synchronized (groups) {
                            for (int i = 0; i < children.getLength(); i++) {
                                Element node = (Element) children.item(i);
                                //                    if (!node.getTagName().equals(TAG_ENTRY)) {continue;}
                                Entry entry =
                                    getEntryManager().processEntryXml(
                                        request, node, parentGroup,
                                        new Hashtable(), false, false);

                                entry.setResource(
                                    new Resource(
                                        "remote:"
                                        + XmlUtil.getAttribute(
                                            node, ATTR_RESOURCE,
                                            ""), Resource.TYPE_REMOTE_FILE));
                                entry.setId(
                                    getEntryManager().getRemoteEntryId(
                                        theServer.getUrl(),
                                        XmlUtil.getAttribute(node, ATTR_ID)));
                                entry.setIsRemoteEntry(true);
                                entry.setRemoteServer(theServer.getUrl());
                                getEntryManager().cacheEntry(entry);
                                if (entry.isGroup()) {
                                    groups.add((Group) entry);
                                } else {
                                    entries.add((Entry) entry);
                                }
                            }
                        }
                    } catch (Exception exc) {
                        logException("Error doing search:" + remoteSearchUrl,
                                     exc);
                    } finally {
                        synchronized (runnableCnt) {
                            runnableCnt[0]--;
                        }
                    }
                }

                public String toString() {
                    return "Runnable:" + theServer.getUrl();
                }
            };
            runnables.add(runnable);
        }


        runnableCnt[0] = runnables.size();
        for (Runnable runnable : runnables) {
            Misc.runInABit(0, runnable);
        }


        //Wait at most 10 seconds for all of the thread to finish
        long t1 = System.currentTimeMillis();
        while (true) {
            synchronized (runnableCnt) {
                if (runnableCnt[0] <= 0) {
                    break;
                }
            }
            //Busy loop
            Misc.sleep(100);
            long t2 = System.currentTimeMillis();
            //Wait at most 10 seconds
            if ((t2 - t1) > 20000) {
                logInfo("Remote search waited too long");
                break;
            }
        }
        running[0] = false;



        request.put(ARG_OUTPUT, output);


    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param what _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getSearchFormLinks(Request request, String what)
            throws Exception {
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        List        links       = new ArrayList();
        String      extra1      = " class=subnavnolink ";
        String      extra2      = " class=subnavlink ";
        String[]    whats       = { WHAT_ENTRIES, WHAT_TAG,
                                    WHAT_ASSOCIATION };
        String[]    names       = { "Entries", "Tags", "Associations" };

        String      formType    = request.getString(ARG_FORM_TYPE, "basic");

        for (int i = 0; i < whats.length; i++) {
            String item;
            if (what.equals(whats[i])) {
                item = HtmlUtil.span(names[i], extra1);
            } else {
                item = HtmlUtil.href(
                    request.url(
                        getRepository().URL_SEARCH_FORM, ARG_WHAT, whats[i],
                        ARG_FORM_TYPE, formType), names[i], extra2);
            }
            if (i == 0) {
                item = "<span " + extra1
                       + ">Search For:&nbsp;&nbsp;&nbsp; </span>" + item;
            }
            links.add(item);
        }

        List<TwoFacedObject> whatList = typeHandler.getListTypes(false);
        for (TwoFacedObject tfo : whatList) {
            if (tfo.getId().equals(what)) {
                links.add(HtmlUtil.span(tfo.toString(), extra1));
            } else {
                links.add(
                    HtmlUtil.href(
                        request.url(
                            getRepository().URL_SEARCH_FORM, ARG_WHAT,
                            BLANK + tfo.getId(), ARG_TYPE,
                            typeHandler.getType()), tfo.toString(), extra2));
            }
        }

        return links;
    }

}


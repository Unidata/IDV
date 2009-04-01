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
import ucar.unidata.repository.output.*;

import org.w3c.dom.*;


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
import ucar.unidata.util.TwoFacedObject;
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

    private List<ServerInfo> federatedServers;



    /**
     * _more_
     *
     * @param repository _more_
     */
    public SearchManager(Repository repository) {
        super(repository);
    }


    public ServerInfo findFederatedServer(String id) {
        if(id.equals("this")) {
            return getRepository().getServerInfo();
        }
        for(ServerInfo server: getFederatedServers()) {
            if(server.getId().equals(id)) return server;
        }
        return null;
    }

    public List<ServerInfo> getFederatedServers() {
        if(federatedServers == null) {
            List<ServerInfo> tmp =  new ArrayList<ServerInfo>();
            tmp.add(new ServerInfo("motherlode.ucar.edu",80,"Unidata's RAMADDA  Server",
                                   "This is the main RAMADDA server hosted by Unidata"));

            tmp.add(new ServerInfo("harpo",8080,"harpo@8080",
                                   "RAMADDA on Harpo"));
            federatedServers = tmp;
        }

        return federatedServers;
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

        StringBuffer sb = new StringBuffer();
        sb.append(
            HtmlUtil.form(
                request.url(
                    getRepository().URL_ENTRY_SEARCH, ARG_NAME,
                    WHAT_ENTRIES), " name=\"searchform\" "));


        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HtmlUtil.submitImage(iconUrl(ICON_BLANK), "submit"));
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        OutputType  output      = request.getOutput(BLANK);
        String      buttons     = HtmlUtil.submit(msg("Search"), "submit");
        sb.append("<table width=\"90%\" border=0><tr><td>");
        typeHandler.addTextSearch(request, sb);
        sb.append("</table>");
        sb.append(HtmlUtil.p());
        sb.append(buttons);
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.formClose());
        return getRepository().makeResult(request, msg("Search Form"), sb,
                                          getSearchUrls());
    }


    public RequestUrl[] getSearchUrls() {
        //        if(getFederatedServers().size()>0) {
            //            return getRepository().federatedSearchUrls;
        //        }
        return getRepository().searchUrls;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param typeSpecific _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result xprocessEntrySearchForm(Request request,
                                          boolean typeSpecific)
            throws Exception {

        StringBuffer sb = new StringBuffer();

        sb.append(
            HtmlUtil.form(
                request.url(
                    getRepository().URL_ENTRY_SEARCH, ARG_NAME,
                    WHAT_ENTRIES), " name=\"searchform\" "));


        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HtmlUtil.submitImage(iconUrl(ICON_BLANK), "submit"));
        TypeHandler typeHandler = getRepository().getTypeHandler(request);

        String      what        = (String) request.getWhat(BLANK);
        if (what.length() == 0) {
            what = WHAT_ENTRIES;
        }



        String buttons =
            RepositoryUtil.buttons(HtmlUtil.submit(msg("Search"), "submit"),
                                   HtmlUtil.submit(msg("Search Subset"),
                                       "submit_subset"));

        sb.append(HtmlUtil.p());
        sb.append(buttons);
        sb.append(HtmlUtil.p());
        sb.append("<table width=\"90%\" border=0><tr><td>");

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
        orderByList.add(new TwoFacedObject(msg("Create Date"), "createdate"));
        orderByList.add(new TwoFacedObject(msg("Name"), "name"));

        String orderBy =
            HtmlUtil.select(ARG_ORDERBY, orderByList,
                            request.getString(ARG_ORDERBY,
                                "none")) + HtmlUtil.checkbox(ARG_ASCENDING,
                                    "true",
                                    request.get(ARG_ASCENDING,
                                        false)) + HtmlUtil.space(1)
                                            + msg("ascending");
        outputForm.append(HtmlUtil.formEntry(msgLabel("Order By"), orderBy));
        outputForm.append(HtmlUtil.formEntry(msgLabel("Output"), HtmlUtil.select(ARG_OUTPUT,
                                                                                 getOutputHandlerSelectList(),
                                                                                 request.getString(ARG_OUTPUT,""))));

        outputForm.append(HtmlUtil.formTableClose());


        List<ServerInfo> servers   = getFederatedServers();
        if(servers.size()>0) {
            StringBuffer serverSB = new StringBuffer();
            int serverCnt = 0;
            String cbxId;
            String call;

            cbxId = ATTR_SERVER+(serverCnt++);
            call =HtmlUtil.attr(HtmlUtil.ATTR_ONCLICK,HtmlUtil.call(
                          "checkboxClicked",
                          HtmlUtil.comma(
                                         "event", HtmlUtil.squote(ATTR_SERVER),
                                         HtmlUtil.squote(cbxId))));

            serverSB.append(HtmlUtil.checkbox(ATTR_SERVER,
                                              "this",true,HtmlUtil.id(cbxId) +
                                                  call));
            serverSB.append(msg("Include this repository"));
            serverSB.append(HtmlUtil.br());
            for(ServerInfo server: servers) { 
                cbxId = ATTR_SERVER+(serverCnt++);
                call =HtmlUtil.attr(HtmlUtil.ATTR_ONCLICK,HtmlUtil.call(
                                    "checkboxClicked",
                                    HtmlUtil.comma(
                                                   "event", HtmlUtil.squote(ATTR_SERVER),
                                                   HtmlUtil.squote(cbxId))));
                serverSB.append(HtmlUtil.checkbox(ATTR_SERVER,
                                                  server.getId(),true,HtmlUtil.id(cbxId) +
                                                  call));
                serverSB.append(HtmlUtil.space(1));
                serverSB.append(server.getHref(" target=\"server\" "));
                serverSB.append(HtmlUtil.br());
            }
            sb.append(HtmlUtil.makeShowHideBlock(msg("Search Other Repositories"),
                                                 HtmlUtil.div(serverSB.toString(),HtmlUtil.cssClass("serverblock")),
                                                 false));
        }





        sb.append(HtmlUtil.makeShowHideBlock(msg("Output"),
                                             outputForm.toString(), false));

        sb.append(HtmlUtil.p());
        sb.append(buttons);
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.formClose());

        return getRepository().makeResult(request, msg("Search Form"), sb,
                                          getSearchUrls());

    }



    public List getOutputHandlerSelectList() {
        List tfos = new ArrayList<TwoFacedObject>();
        for (OutputHandler outputHandler: getRepository().getOutputHandlers()) {
            for(OutputType type: outputHandler.getTypes()) {
                if(type.getIsForSearch()) {
                    tfos.add(new HtmlUtil.Selector(type.getLabel(),type.getId(),getRepository().iconUrl(type.getIcon())));
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
        return xprocessEntrySearchForm(request, false);
    }


    public Result processFederatedSearchForm(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(
            HtmlUtil.form(
                request.url(
                            getRepository().URL_SEARCH_FEDERATED_DO), HtmlUtil.attr(HtmlUtil.ATTR_NAME,"searchform")));

        sb.append(HtmlUtil.p());
        sb.append(header(msg("Select Servers to Search")));
        StringBuffer serverSB = new StringBuffer();

        for(ServerInfo server: getFederatedServers()) { 
            serverSB.append(HtmlUtil.checkbox(ATTR_SERVER,
                                              server.getId(),true));
            serverSB.append(HtmlUtil.space(1));
            serverSB.append(server.getHref(" target=\"server\" "));
            serverSB.append(HtmlUtil.br());
        }
        sb.append(HtmlUtil.div(serverSB.toString(),HtmlUtil.cssClass("serverblock")));
        sb.append(HtmlUtil.p());
        sb.append(header(msg("Search Criteria")));
        TypeHandler typeHandler = getRepository().getTypeHandler(request);


        typeHandler.addToSearchForm(request, sb, null, true);

        sb.append(HtmlUtil.p());

        sb.append(HtmlUtil.submit(msg("Search"), "submit"));
        sb.append(HtmlUtil.formClose());
        return getRepository().makeResult(request, msg("Federated Search"), sb,
                                          getSearchUrls());

    }

    public List<ServerInfo> findServers(Request request, boolean includeThis) throws Exception {
        List<ServerInfo> servers = new ArrayList<ServerInfo>();
        for(String id: (List<String>)request.get(ATTR_SERVER, new ArrayList())) {
            if(id.equals("this") && !includeThis) continue;
            ServerInfo server = findFederatedServer(id);
            if(server==null) continue;
            servers.add(server);
        }
        return servers;
    }



    public Result processFederatedSearch(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        List<String> servers = (List<String>)request.get(ATTR_SERVER, new ArrayList());
        System.err.println("servers:" + servers);
        sb.append(HtmlUtil.p());
        request.remove(ATTR_SERVER);

        boolean didone = false;
        StringBuffer serverSB = new StringBuffer();
        for(String id: servers) {
            ServerInfo server = findFederatedServer(id);
            if(server==null) continue;
            if(!didone) {
                sb.append(header(msg("Selected Servers")));
            }
            serverSB.append(server.getHref(" target=\"server\" "));
            serverSB.append(HtmlUtil.br());
            didone = true;
        }

        if(!didone) {
            sb.append(getRepository().note(msg("No servers selected")));
        } else {
            sb.append(HtmlUtil.div(serverSB.toString(),HtmlUtil.cssClass("serverblock")));
            sb.append(HtmlUtil.p());
        }
        sb.append(HtmlUtil.p());
        sb.append(header(msg("Search Results")));
        return getRepository().makeResult(request, msg("Federated Search"), sb,
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
                || request.defined("submit_subset")) {
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


        StringBuffer searchCriteriaSB = new StringBuffer();
        boolean searchThis = true;
        List<ServerInfo> servers = findServers(request, true);
        ServerInfo thisServer = getRepository().getServerInfo();
        
        if(request.defined(ATTR_SERVER)) {
            //            searchThis = servers.contains(thisServer);
            //            servers.remove(thisServer);
        }

        List<Group> groups = new ArrayList<Group>();
        List<Entry> entries = new ArrayList<Entry>();

        Group  theGroup         = null;
        if(searchThis) {
            List[] pair = getEntryManager().getEntries(request, searchCriteriaSB);
            groups.addAll((List<Group>)pair[0]);
            entries.addAll((List<Entry>)pair[1]);
        }

        if (request.defined(ARG_GROUP)) {
            String groupId = (String) request.getString(ARG_GROUP, "").trim();
            //            System.err.println("group:" + groupId);
            theGroup = getEntryManager().findGroup(request, groupId);
        }

        if(servers.size()>0) {
            request.put(ARG_DECORATE,"false");
            request.remove(ATTR_SERVER);
            request.put(ATTR_TARGET,"_server");
            String url = request.getUrlArgs();
            StringBuffer sb = new StringBuffer();
            for(ServerInfo server: servers) {
                System.err.println ("SERVER:" + server);
                sb.append("\n");
                sb.append(HtmlUtil.p());
                sb.append(HtmlUtil.makeShowHideBlock(server.getHref(" target=\"server\" "),
                                                     HtmlUtil.tag("iframe",
                                                                  HtmlUtil.attrs("width","100%", "height",
                                                                                 "200",
                                                                                 "src",
                                                                                 server.getUrl()+getRepository().URL_ENTRY_SEARCH.getPath()+"?"+url),"need to have iframe support"),true));
                
                sb.append("\n");
            }
            request.remove(ARG_DECORATE);
            request.remove(ARG_TARGET);
            return new Result("Federated Search Results",sb);
        }


        String s = searchCriteriaSB.toString();
        if(request.defined(ARG_TARGET)) {
            s = "";
        }

        if (s.length() > 0) {
            request.remove("submit");
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
        Result result =  getRepository().getOutputHandler(request).outputGroup(request,
                theGroup, groups, entries);
        return getEntryManager().addEntryHeader(request, theGroup,
                                                result);
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


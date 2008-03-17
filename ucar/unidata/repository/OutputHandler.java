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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



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

import java.util.zip.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class OutputHandler extends RepositoryManager {

    /** _more_ */
    public static final String OUTPUT_HTML = "default.html";




    /** _more_ */
    protected static String timelineAppletTemplate;

    /** _more_ */
    protected static String graphXmlTemplate;

    /** _more_ */
    protected static String graphAppletTemplate;


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public OutputHandler(Repository repository) throws Exception {
        super(repository);

    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public OutputHandler(Repository repository, Element element)
            throws Exception {
        this(repository);
    }

    public void showNext(Request request, List<Group> subGroups, List<Entry> entries,  StringBuffer sb) throws Exception {
        int cnt = subGroups.size() + entries.size();
        int max = request.get(ARG_MAX, Repository.MAX_ROWS);
        //        System.err.println ("cnt:" + cnt + " " + max);

        if ((cnt > 0) && ((cnt == max) || request.defined(ARG_SKIP))) {
            int skip = Math.max(0, request.get(ARG_SKIP, 0));
            sb.append(msgLabel("Results") + (skip + 1) + "-" + (skip + cnt));
            sb.append(HtmlUtil.space(4));
            if (skip > 0) {
                sb.append(HtmlUtil.href(request.getUrl(ARG_SKIP) + "&"
                                        + ARG_SKIP + "="
                                        + (skip - max), msg("Previous")));
                sb.append(HtmlUtil.space(1));
            }
            if (cnt >= max) {
                sb.append(HtmlUtil.href(request.getUrl(ARG_SKIP) + "&"
                                        + ARG_SKIP + "="
                                        + (skip + max), msg("Next")));
            }
        }

    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public boolean canHandle(Request request) {
        String output = (String) request.getOutput();
        return canHandle(output);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public boolean canHandle(String request) {
        return false;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesFor(Request request, String what, List types)
            throws Exception {}


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesForEntries(Request request,
                                            List<Entry> entries, List types)
            throws Exception {}


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesForEntry(Request request, Entry entry,
                                          List types)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        getOutputTypesForEntries(request, entries, types);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesForGroup(Request request, Group group,
                                          List<Group> subGroups,
                                          List<Entry> entries, List types)
            throws Exception {
        getOutputTypesFor(request, WHAT_ENTRIES, types);
    }


    /**
     * _more_
     *
     *
     * @param method _more_
     * @return _more_
     */
    private Result notImplemented(String method) {
        throw new IllegalArgumentException("Method: " + method
                                           + " not implemented");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, Entry entry) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        return outputGroup(request, getRepository().getDummyGroup(),
                           new ArrayList<Group>(), entries);
    }


    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        return null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links)
            throws Exception {}



    protected String getAjaxLink(Request request, Entry entry) {
        return getAjaxLink(request, entry, entry.getLabel(),true);
    }

    protected String getAjaxLink(Request request, Entry entry, String linkText, boolean includeIcon) {
        StringBuffer sb = new StringBuffer();
        if(includeIcon) {
            boolean okToMove = !request.getUser().getAnonymous();
            String icon = (entry.isGroup()?getRepository().fileUrl(ICON_FOLDER_CLOSED):getRepository().fileUrl(ICON_FILE));
            String dropEvent =   " onmouseup=" + HtmlUtil.quote("mouseUpOnEntry(event,'" + entry.getId()+"');");
            String event = (entry.isGroup()?
                            " onclick=" + HtmlUtil.quote("folderClick('" + entry.getId() +"')") : "");
            
            if(okToMove) {
                event += 
                    (entry.isGroup()?" onmouseover=" + HtmlUtil.quote("mouseOverOnEntry(event,'" + entry.getId() +"');"):"") + 
                    " onmouseout=" + HtmlUtil.quote("mouseOutOnEntry(event,'" + entry.getId() +"');") + 
                    " onmousedown=" + HtmlUtil.quote("mouseDownOnEntry(event,'" + entry.getId() +"');") + 
                    (entry.isGroup()?dropEvent:"");
            }


            String img = HtmlUtil.img(icon,(entry.isGroup()?"Click to open group; ":"") + (okToMove?"Drag to move":"") ," id=" + HtmlUtil.quote("img_" +entry.getId()) + event); 
            if(entry.isGroup()) {
                //                sb.append("<a href=\"JavaScript: noop()\" " + event +"/>" +      img +"</a>");
                sb.append(img);
            } else {
                sb.append(img);
            }
            sb.append(HtmlUtil.space(1));
        }
        String elementId =  entry.getId();
        sb.append(
            HtmlUtil.href(
                          request.url(
                                       getRepository().URL_ENTRY_SHOW, ARG_ID,
                                       entry.getId()), linkText,
                          " id=" + HtmlUtil.quote(elementId) +" " +
                          " onmouseover=" + HtmlUtil.quote("tooltipShow(event,'" + elementId +"');") + 
                          " onmouseout=" + HtmlUtil.quote("tooltipHide(event,'" + elementId +"');")));
        
        return HtmlUtil.span(sb.toString(), " id=" + HtmlUtil.quote("span_" + entry.getId()));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param output _more_
     *
     * @return _more_
     */
    public String getNextPrevLink(Request request, Entry entry,
                                  String output) {
        String nextLink = HtmlUtil.href(
                                        request.url(
                                  getRepository().URL_ENTRY_SHOW, ARG_ID,
                                  entry.getId(), ARG_OUTPUT, output,
                                  ARG_NEXT, "true"), HtmlUtil.img(
                                      getRepository().fileUrl(ICON_RIGHT),
                                      msg("View Next Entry")));
        String prevLink = HtmlUtil.href(
                                        request.url(
                                  getRepository().URL_ENTRY_SHOW, ARG_ID,
                                  entry.getId(), ARG_OUTPUT, output,
                                  ARG_PREVIOUS, "true"), HtmlUtil.img(
                                      getRepository().fileUrl(ICON_LEFT),
                                      msg("View Previous Entry")));
        return prevLink + nextLink;
    }




    /**
     * _more_
     *
     * @param buffer _more_
     */
    public void addToSettingsForm(StringBuffer buffer) {}

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applySettings(Request request) throws Exception {}


    /**
     * _more_
     *
     * @param sb _more_
     * @param entries _more_
     * @param request _more_
     * @param doForm _more_
     * @param dfltSelected _more_
     *
     * @throws Exception _more_
     */
    public void xxxgetEntryHtml(StringBuffer sb, List<Entry> entries,
                                Request request, boolean doForm,
                                boolean dfltSelected)
            throws Exception {
        notImplemented("getEntryHtml");
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param entries _more_
     * @param request _more_
     * @param doForm _more_
     * @param dfltSelected _more_
     * @param showCrumbs _more_
     *
     * @throws Exception _more_
     */
    public String  getEntryHtml(StringBuffer sb, List<Entry> entries,
                             Request request, boolean doForm,
                             boolean dfltSelected, boolean showCrumbs)
            throws Exception {

        String link = "";
        if (doForm) {
            StringBuffer formSB = new StringBuffer();
            formSB.append(request.form(getRepository().URL_GETENTRIES,
                                    "getentries"));
            //            formSB.append(HtmlUtil.space(1));
            List outputList =
                getRepository().getOutputTypesForEntries(request, entries);
            sb.append("\n");
            formSB.append(HtmlUtil.space(4));
            formSB.append(msgLabel("View As"));
            formSB.append(HtmlUtil.select(ARG_OUTPUT, outputList));
            formSB.append(HtmlUtil.submit(msg("Selected"), "getselected"));
            formSB.append(HtmlUtil.submit(msg("All"), "getall"));

            String arrowImg = HtmlUtil.img(getRepository().fileUrl(ICON_DOWNARROW),"Show/Hide Form"," id=\"entryformimg\" ");
            link = HtmlUtil.space(2) + "<a href=\"JavaScript: noop()\" onclick=\"toggleEntryForm()\">" + arrowImg +"</a>";
            sb.append(HtmlUtil.span(formSB.toString()," id = \"entryform\" "));
            sb.append("<ul class=\"folderblock\" style=\"list-style-image : url("
                      + getRepository().fileUrl(ICON_BLANK) + ")\">");
        }
        String img = HtmlUtil.img(getRepository().fileUrl(ICON_FILE));
        int cnt = 0;
        for (Entry entry : entries) {
            sb.append("<li>");
            if (doForm) {
                sb.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
                sb.append(HtmlUtil.span(
                                        HtmlUtil.checkbox("entry_" + entry.getId(), "true",
                                                          dfltSelected)," id=\"entryform" + (cnt++)+"\" "));
            }

            if (showCrumbs) {
                sb.append(img);
                sb.append(HtmlUtil.space(1));
                String crumbs = getRepository().getBreadCrumbs(request,
                                    entry);

                sb.append(crumbs);
            } else {
                sb.append(getAjaxLink(request,  entry, entry.getLabel(), true));
                          //                sb.append(getEntryUrl(request, entry));
            }
            //            sb.append(HtmlUtil.br());
        }
        if (doForm) {
            sb.append("</ul>");
            sb.append(HtmlUtil.formClose());
            sb.append("\n<SCRIPT LANGUAGE=\"JavaScript\">toggleEntryForm();</script>\n"); 
        }
        return link;
    }




    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryUrl(Request request, Entry entry) {
        if(true)
            return getAjaxLink(request,  entry, entry.getLabel(), false);
        return HtmlUtil.href(request.url(getRepository().URL_ENTRY_SHOW,
                                          ARG_ID,
                                          entry.getId()), entry.getLabel());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param output _more_
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getEntriesHeader(Request request, String output,
                                    String what)
            throws Exception {
        return getHeader(request, output,
                         getRepository().getOutputTypesFor(request, what));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param output _more_
     * @param outputTypes _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getHeader(Request request, String output,
                             List<TwoFacedObject> outputTypes)
            throws Exception {
        int    cnt           = 0;
        List   items         = new ArrayList();
        String initialOutput = request.getString(ARG_OUTPUT, "");
        for (TwoFacedObject tfo : outputTypes) {
            request.put(ARG_OUTPUT, (String) tfo.getId());
            if (tfo.getId().equals(output)) {
                items.add(msg(tfo.toString()));
            } else {
                items.add(
                    HtmlUtil.href(
                        request.getRequestPath() + "?"
                        + request.getUrlArgs(ARG_MESSAGE), msg(
                            tfo.toString()), " class=\"subnavlink\" "));
            }
        }
        request.put(ARG_OUTPUT, initialOutput);
        return items;
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandlers _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTypes(Request request,
                               List<TypeHandler> typeHandlers)
            throws Exception {
        return notImplemented("listTypes");
    }





    /**
     * protected Result listTags(Request request, List<Tag> tags)
     *       throws Exception {
     *   return notImplemented("listTags");
     * }
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listAssociations(Request request) throws Exception {
        return notImplemented("listAssociations");
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        return notImplemented("outputGroup");
    }






}


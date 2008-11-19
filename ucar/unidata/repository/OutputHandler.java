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


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
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
    public static final OutputType OUTPUT_HTML = new OutputType("Entry", "default.html");


    private String name;

    private List<OutputType> types = new ArrayList<OutputType>();


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public OutputHandler(Repository repository, String name) throws Exception {
        super(repository);
        this.name = name;
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
        this(repository,XmlUtil.getAttribute(element, ATTR_NAME,(String) null));
    }

    public void init() {
    }


    protected void addType(OutputType type) {
        type.setGroupName(name);
        types.add(type);
    }

    public List<OutputType> getTypes() {
        return types;
    }


    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	name = value;
    }


    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
        if(name == null) {
            name = Misc.getClassName(getClass());
        }
	return name;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param subGroups _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void showNext(Request request, List<Group> subGroups,
                         List<Entry> entries, StringBuffer sb)
            throws Exception {
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
    public boolean canHandleOutput(OutputType output) {
        for(OutputType type: types) {
            if(type.equals(output)) return true;
        }
        return false;
    }

    /**
     * Class State _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class State {

        /** _more_ */
        public static final int FOR_UNKNOWN = 0;

        /** _more_ */
        public static final int FOR_HEADER = 1;

        /** _more_ */
        public int forWhat = FOR_UNKNOWN;

        /** _more_ */
        public Entry entry;

        /** _more_ */
        public Group group;

        /** _more_ */
        public List<Group> subGroups;

        /** _more_ */
        public List<Entry> entries;

        /** _more_ */
        public List<Entry> allEntries;

        /**
         * _more_
         *
         * @param entry _more_
         */
        public State(Entry entry) {
            this.entry = entry;
        }

        /**
         * _more_
         *
         * @param group _more_
         * @param subGroups _more_
         * @param entries _more_
         */
        public State(Group group, List<Group> subGroups,
                     List<Entry> entries) {
            this.group     = group;
            this.entries   = entries;
            this.subGroups = subGroups;
        }


        /**
         * _more_
         *
         * @param entries _more_
         */
        public State(List<Entry> entries) {
            this.entries = entries;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean forHeader() {
            return forWhat == FOR_HEADER;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public List<Entry> getAllEntries() {
            if (allEntries == null) {
                allEntries = new ArrayList();
                if (subGroups != null) {
                    allEntries.addAll(subGroups);
                }
                if (entries != null) {
                    allEntries.addAll(entries);
                }
                if (entry != null) {
                    allEntries.add(entry);
                }
            }
            return (List<Entry>) allEntries;
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param sb _more_
     * @param state _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result makeLinksResult(Request request, String title,
                                     StringBuffer sb, State state)
            throws Exception {
        Result result = new Result(title, sb);
        addLinks(request, result, state);
        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param result _more_
     * @param state _more_
     *
     * @throws Exception _more_
     */
    protected void addLinks(Request request, Result result, State state)
            throws Exception {
        state.forWhat = State.FOR_HEADER;
        result.putProperty(PROP_NAVSUBLINKS,
                           getHeader(request, request.getOutput(),
                                     getRepository().getOutputTypes(request,
                                         state)));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param state _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void addOutputTypes(Request request, State state,
                                  List<OutputType> types)
            throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     * @param forHeader _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links, boolean forHeader)
            throws Exception {}



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




    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        return null;
    }






    protected static String getGroupSelect(Request request,String elementId) throws Exception {
        String event = "selectInitialClick(event," +HtmlUtil.squote(elementId) +")";
        return HtmlUtil.mouseClickHref(event,"Select",HtmlUtil.id(elementId+".selectlink"));
    }

    protected String getSelectLink(Request request, Group group, String target) throws Exception {
        String linkText = group.getLabel();
        StringBuffer sb = new StringBuffer();
        String entryId = group.getId();
        String  icon     = getRepository().getIconUrl(group);
        String event = (group.isGroup()
                        ? HtmlUtil.onMouseClick("folderClick(" + HtmlUtil.squote(entryId) + 
                                                ",'selectxml'," + HtmlUtil.squote(ATTR_TARGET+"=" + target)+")")
                        : "");
        String img = HtmlUtil.img(icon, (group.isGroup()
                                         ? "Click to open group; "
                                         : ""), " id=" + HtmlUtil.quote("img_" + entryId)
                                  + event);
        sb.append(img);
        sb.append(HtmlUtil.space(1));

        String elementId = group.getId();
        String value = group.getFullName();
        sb.append(
                  HtmlUtil.mouseClickHref("selectClick(" + HtmlUtil.squote(target) +"," + HtmlUtil.squote(value)+")",
                                          linkText));
        return sb.toString();
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
    public List<Link> getNextPrevLinks(Request request, Entry entry,
                                       OutputType output) {
        List<Link> links = new ArrayList<Link>();
        links.add(
            new Link(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                    output, ARG_PREVIOUS, "true"), getRepository().fileUrl(
                        ICON_LEFT), msg("View Previous Entry")));

        links.add(
            new Link(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                    output, ARG_NEXT, "true"), getRepository().fileUrl(
                        ICON_RIGHT), msg("View Next Entry")));
        return links;
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
     *
     * @return _more_
     * @throws Exception _more_
     */
    public String getEntryHtml(StringBuffer sb, List entries,
                               Request request, boolean doForm,
                               boolean dfltSelected, boolean showCrumbs)
            throws Exception {

        String link = "";
        if (doForm) {
            StringBuffer formSB = new StringBuffer();
            formSB.append(request.formPost(getRepository().URL_ENTRY_GETENTRIES,
                                       "getentries"));
            //            formSB.append(HtmlUtil.space(1));
            List<OutputType> outputList =
                getRepository().getOutputTypes(request, new State(entries));
            List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
            for(OutputType outputType: outputList) {
                tfos.add(new TwoFacedObject(outputType.getLabel(), outputType.getId()));
            }
            sb.append("\n");
            formSB.append(HtmlUtil.space(4));
            formSB.append(msgLabel("View As"));
            formSB.append(HtmlUtil.select(ARG_OUTPUT, tfos));
            formSB.append(HtmlUtil.submit(msg("Selected"), "getselected"));
            formSB.append(HtmlUtil.submit(msg("All"), "getall"));

            String arrowImg =
                HtmlUtil.img(getRepository().fileUrl(ICON_DOWNARROW),
                             "Show/Hide Form", " id=\"entryformimg\" ");
            link = HtmlUtil.space(2)
                   + HtmlUtil.jsLink(
                       HtmlUtil.onMouseClick("toggleEntryForm()"), arrowImg);
            sb.append(HtmlUtil.span(formSB.toString(),
                                    " id = \"entryform\" "));
            sb.append(
                "<ul class=\"folderblock\" style=\"list-style-image : url("
                + getRepository().fileUrl(ICON_BLANK) + ")\">");
        }
        //        String img = HtmlUtil.img(getRepository().fileUrl(ICON_FILE));
        int cnt = 0;
        for (Entry entry : (List<Entry>) entries) {
            sb.append("<li>");
            if (doForm) {
                sb.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
                sb.append(
                    HtmlUtil.span(
                        HtmlUtil.checkbox(
                            "entry_" + entry.getId(), "true",
                            dfltSelected), " id=\"entryform" + (cnt++) + "\" "));
            }

            if (showCrumbs) {
                String img = HtmlUtil.img(getRepository().getIconUrl(entry));
                sb.append(img);
                sb.append(HtmlUtil.space(1));
                String crumbs = getRepository().getBreadCrumbs(request,
                                    entry);

                sb.append(crumbs);
            } else {
                sb.append(getRepository().getAjaxLink(request, entry, entry.getLabel(),
                                      true));
                //                sb.append(getEntryLink(request, entry));
            }
            //            sb.append(HtmlUtil.br());
        }
        if (doForm) {
            sb.append("</ul>");
            sb.append(HtmlUtil.formClose());
            sb.append(
                "\n<SCRIPT LANGUAGE=\"JavaScript\">toggleEntryForm();</script>\n");
        }
        return link;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryLink(Request request, Entry entry) throws Exception {
        return getRepository().getAjaxLink(request, entry, entry.getLabel(), false);
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
    protected List getHeader(Request request, OutputType output,
                             List<OutputType> outputTypes)
            throws Exception {

        List   items          = new ArrayList();
        String initialOutput  = request.getString(ARG_OUTPUT, "");
        Object initialMessage = request.remove(ARG_MESSAGE);
        String onLinkTemplate = getRepository().getProperty("ramadda.html.sublink.on","");
        String offLinkTemplate = getRepository().getProperty("ramadda.html.sublink.off","");
        for (OutputType outputType : outputTypes) {
            request.put(ARG_OUTPUT, outputType);
            String url = outputType.assembleUrl(request);
            String label = msg(outputType.getLabel());
            String template;
            if (outputType.equals(output)) {
                template = onLinkTemplate;
            } else {
                template = offLinkTemplate;
            }
            String html = template.replace("${label}", label);
            html = html.replace("${url}", url);
            html = html.replace("${root}", getRepository().getUrlBase());
            items.add(html);
        }
        request.put(ARG_OUTPUT, initialOutput);
        if (initialMessage != null) {
            request.put(ARG_MESSAGE, initialMessage);
        }
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









}


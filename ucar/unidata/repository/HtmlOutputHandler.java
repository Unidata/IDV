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
import ucar.unidata.ui.ImageUtils;
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
public class HtmlOutputHandler extends OutputHandler {



    /** _more_ */
    public static final String OUTPUT_TIMELINE = "default.timeline";


    /** _more_ */
    public static final String OUTPUT_GRAPH = "default.graph";

    /** _more_ */
    public static final String OUTPUT_CLOUD = "default.cloud";



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public HtmlOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    /**
     * _more_
     *
     *
     * @param output _more_
     *
     * @return _more_
     */
    public boolean canHandle(String output) {
        return output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)
               || output.equals(OUTPUT_GRAPH) || output.equals(OUTPUT_CLOUD);
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
            throws Exception {
        if (what.equals(WHAT_ENTRIES)) {
            types.add(new TwoFacedObject("Entry", OUTPUT_HTML));
            if (getRepository().isAppletEnabled(request)) {
                types.add(new TwoFacedObject("Timeline", OUTPUT_TIMELINE));
            }
        } else if (what.equals(WHAT_TAG)) {
            types.add(new TwoFacedObject("Tag Html", OUTPUT_HTML));
            types.add(new TwoFacedObject("Tag Cloud", OUTPUT_CLOUD));
        } else if (what.equals(WHAT_TYPE)) {
            types.add(new TwoFacedObject("Type Html", OUTPUT_HTML));
        } else {
            types.add(new TwoFacedObject("Entry", OUTPUT_HTML));
        }
    }

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
            throws Exception {
        types.add(new TwoFacedObject("Entry", OUTPUT_HTML));
        if (entries.size() > 1) {
            types.add(new TwoFacedObject("Timeline", OUTPUT_TIMELINE));
        }
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
        TypeHandler typeHandler =
            getRepository().getTypeHandler(entry.getType());
        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                              false, "");
        StringBuffer sb = new StringBuffer();
        sb.append(crumbs[1]);
        sb.append(typeHandler.getEntryContent(entry, request, true));
        getMetadataHtml(request, entry, sb);

        Result result = new Result(msgLabel("Entry")  + entry.getName(), sb,
                                   getMimeType(request.getOutput()));
        result.putProperty(
            PROP_NAVSUBLINKS,
            getHeader(
                request, request.getOutput(),
                getRepository().getOutputTypesForEntry(request, entry)));
        return result;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param groups _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listGroups(Request request, List<Group> groups)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        //        appendListHeader(request, output, WHAT_GROUP, sb);
        if (output.equals(OUTPUT_HTML)) {
            sb.append(msgHeader("Groups"));
            sb.append("<ul>");
        }

        for (Group group : groups) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>"
                          + getRepository().getEntryLinks(request, group)
                          + " " + group.getFullName());
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>\n");
        }
        return new Result("", sb, getMimeType(output));
    }



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
    public void getEntryHtml(StringBuffer sb, List<Entry> entries,
                             Request request, boolean doForm,
                             boolean dfltSelected)
            throws Exception {
        if (doForm) {
            sb.append(HtmlUtil.form(getRepository().URL_GETENTRIES,
                                    "getentries"));
            sb.append(HtmlUtil.submit(msg("Get selected"), "getselected"));
            sb.append(HtmlUtil.submit(msg("Get all"), "getall"));
            sb.append(HtmlUtil.space(1));
            sb.append(msgLabel("As"));
            List outputList =
                getRepository().getOutputTypesForEntries(request, entries);
            sb.append(HtmlUtil.select(ARG_OUTPUT, outputList));
            sb.append("<p>\n");
            sb.append("<ul>\n");

        }
        for (Entry entry : entries) {
            sb.append(HtmlUtil.checkbox("entry_" + entry.getId(), "true",
                                        dfltSelected));
            sb.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
            sb.append(HtmlUtil.space(1));
            //            sb.append(getRepository().getEntryLinks(request,entry));
            sb.append(HtmlUtil.space(1));
            sb.append(getEntryUrl(entry));
            sb.append(HtmlUtil.br());
        }
        if (doForm) {
            sb.append("</ul>");
            sb.append(HtmlUtil.formClose());
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryUrl(Entry entry) {
        return HtmlUtil.href(HtmlUtil.url(getRepository().URL_ENTRY_SHOW,
                                          ARG_ID,
                                          entry.getId()), entry.getName());
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
        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        if (output.equals(OUTPUT_HTML)) {
            appendListHeader(request, output, WHAT_TYPE, sb);
            sb.append("<ul>");
        }

        for (TypeHandler theTypeHandler : typeHandlers) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(
                    HtmlUtil.href(
                        HtmlUtil.url(
                            getRepository().URL_ENTRY_SEARCHFORM, ARG_TYPE,
                            theTypeHandler.getType()), HtmlUtil.img(
                                getRepository().fileUrl(ICON_SEARCH),
                                msg("Search in Group"))));
                sb.append(" ");
                sb.append(HtmlUtil
                    .href(HtmlUtil
                        .url(getRepository().URL_LIST_HOME, ARG_TYPE,
                             theTypeHandler.getType()), theTypeHandler
                                 .getType()));
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>");
        }
        return new Result("", sb, getMimeType(output));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param output _more_
     * @param what _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    protected void appendListHeader(Request request, String output,
                                    String what, StringBuffer sb)
            throws Exception {
        List<TwoFacedObject> outputTypes =
            getRepository().getOutputTypesFor(request, what);
        int cnt = 0;
        sb.append("<b>");
        String initialOutput = request.getOutput("");
        for (TwoFacedObject tfo : outputTypes) {
            if (cnt++ > 0) {
                sb.append("&nbsp;|&nbsp;");
            }
            request.put(ARG_OUTPUT, (String) tfo.getId());
            if (tfo.getId().equals(output)) {
                sb.append(HtmlUtil.span(tfo.toString(), ""));
            } else {
                sb.append(
                    HtmlUtil.href(
                        request.getRequestPath() + "?"
                        + request.getUrlArgs(), tfo.toString()));
            }
        }
        request.put(ARG_OUTPUT, initialOutput);
        sb.append("</b>");

    }





    /**
     * _more_
     *
     * @param request _more_
     * @param tags _more_
     *
     * @param output _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    /*
    protected Result listTags(Request request, List<Tag> tags)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_CLOUD)) {
            appendListHeader(request, output, WHAT_TAG, sb);
            sb.append("<ul>");
        }
        request.remove(ARG_OUTPUT);
        int max = -1;
        int min = -1;

        for (Tag tag : tags) {
            if ((max < 0) || (tag.getCount() > max)) {
                max = tag.getCount();
            }
            if ((min < 0) || (tag.getCount() < min)) {
                min = tag.getCount();
            }
        }

        int    diff         = max - min;
        double distribution = diff / 5.0;

        for (Tag tag : tags) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li> ");
                sb.append(getRepository().getTagLinks(request, tag.getName()));
                sb.append(" ");
                sb.append(tag.getName());
                sb.append(" (" + tag.getCount() + ")");

            } else if (output.equals(OUTPUT_CLOUD)) {
                double percent = tag.getCount() / distribution;
                int    bin     = (int) (percent * 5);
                String css     = "font-size:" + (12 + bin * 2);
                sb.append("<span style=\"" + css + "\">");
                String extra = XmlUtil.attrs("alt",
                                             "Count:" + tag.getCount(),
                                             "title",
                                             "Count:" + tag.getCount());
                sb.append(
                    HtmlUtil.href(
                        HtmlUtil.url(
                            getRepository().URL_GRAPH_VIEW, ARG_ID, tag.getName(),
                            ARG_NODETYPE, TYPE_TAG), tag.getName(), extra));
                sb.append("</span>");
                sb.append(" &nbsp; ");

            }
        }

        String pageTitle = "";
        if (output.equals(OUTPUT_HTML)) {
            if (tags.size() == 0) {
                sb.append("No tags found");
            }
            pageTitle = "Tags";
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            if (tags.size() == 0) {
                sb.append("No tags found");
            }
            pageTitle = "Tag Cloud";
        }
        Result result = new Result(pageTitle, sb, getMimeType(output));
        //        StringBuffer  tsb = new StringBuffer();
        //        appendListHeader(request, output, WHAT_TAG, tsb);
        //        result.putProperty(PROP_NAVSUBLINKS,
        return result;
    }

    */



    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        if (output.equals(OUTPUT_TIMELINE)) {
            return getRepository().getMimeTypeFromSuffix(".html");
        } else if (output.equals(OUTPUT_GRAPH)) {
            return getRepository().getMimeTypeFromSuffix(".xml");
        } else if (output.equals(OUTPUT_HTML)) {
            return getRepository().getMimeTypeFromSuffix(".html");
        } else if (output.equals(OUTPUT_CLOUD)) {
            return getRepository().getMimeTypeFromSuffix(".html");
        } else {
            return super.getMimeType(output);
        }
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
    protected Result listAssociations(Request request) throws Exception {

        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        if (output.equals(OUTPUT_HTML)) {
            appendListHeader(request, output, WHAT_ASSOCIATION, sb);
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            sb.append(msgHeader("Association Cloud"));
        }
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        if (where.size() > 0) {
            where.add(0, SqlUtil.eq(COL_ASSOCIATIONS_FROM_ENTRY_ID,
                                    COL_ENTRIES_ID));
            where.add(0, SqlUtil.eq(COL_ASSOCIATIONS_TO_ENTRY_ID,
                                    COL_ENTRIES_ID));
        }


        String[] associations =
            SqlUtil.readString(typeHandler.executeSelect(request,
                SqlUtil.distinct(COL_ASSOCIATIONS_NAME), where), 1);


        if (associations.length == 0) {
            if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_CLOUD)) {
                sb.append(msg("No associations found"));
            }
        }
        List<String>  names  = new ArrayList<String>();
        List<Integer> counts = new ArrayList<Integer>();
        ResultSet     results;
        int           max = -1;
        int           min = -1;
        for (int i = 0; i < associations.length; i++) {
            String association = associations[i];
            Statement stmt2 = typeHandler.executeSelect(
                                  request, SqlUtil.count("*"),
                                  Misc.newList(
                                      SqlUtil.eq(
                                          COL_ASSOCIATIONS_NAME,
                                          SqlUtil.quote(association))));

            ResultSet results2 = stmt2.getResultSet();
            if ( !results2.next()) {
                continue;
            }
            int count = results2.getInt(1);
            if ((max < 0) || (count > max)) {
                max = count;
            }
            if ((min < 0) || (count < min)) {
                min = count;
            }
            names.add(association);
            counts.add(new Integer(count));
        }

        int    diff         = max - min;
        double distribution = diff / 5.0;

        for (int i = 0; i < names.size(); i++) {
            String association = names.get(i);
            int    count       = counts.get(i).intValue();
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li> ");
                sb.append(getRepository().getAssociationLinks(request,
                        association));
                sb.append(" ");
                sb.append(association);
                sb.append(" (" + count + ")");

            } else if (output.equals(OUTPUT_CLOUD)) {
                double percent = count / distribution;
                int    bin     = (int) (percent * 5);
                String css     = "font-size:" + (12 + bin * 2);
                sb.append("<span style=\"" + css + "\">");
                String extra = XmlUtil.attrs("alt", msgLabel("Count") + count,
                                             "title", msgLabel("Count") + count);
                sb.append(
                    HtmlUtil.href(
                        HtmlUtil.url(
                            getRepository().URL_GRAPH_VIEW, ARG_ID,
                            association, ARG_NODETYPE,
                            TYPE_ASSOCIATION), association, extra));
                sb.append("</span>");
                sb.append(HtmlUtil.space(1));
            }
        }

        String pageTitle = "";
        if (output.equals(OUTPUT_HTML)) {
            pageTitle = msg("Associations");
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            pageTitle = msg("Association Cloud");
        }
        return new Result(pageTitle, sb, getMimeType(output));

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void getMetadataHtml(Request request, Entry entry, StringBuffer sb)
            throws Exception {
        boolean        showMetadata = request.get(ARG_SHOWMETADATA, false);


        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        if (metadataList.size() == 0) {
            return;
        }
        sb.append(HtmlUtil.p());

        String url = request.getUrl(ARG_SHOWMETADATA) + "&"
                     + ARG_SHOWMETADATA + "=" + (showMetadata
                ? "false"
                : "true");
        String link = HtmlUtil.href(url, (showMetadata
                                          ? "-&nbsp; " +msg("Details")
                                          : "+&nbsp; " + msg("Details")), " class=\"subheaderlink\" ");

        //        sb.append("<tr><td colspan=\"2\">");
        sb.append(HtmlUtil.div(link, " class=\"subheader\""));
        //        sb.append("</td>\n");
        sb.append("<table cellspacing=\"5\">\n");
        List<MetadataHandler> metadataHandlers =
            getMetadataManager().getMetadataHandlers();
        if (showMetadata) {

            for (Metadata metadata : metadataList) {
                for (MetadataHandler metadataHandler : metadataHandlers) {
                    if (metadataHandler.canHandle(metadata)) {
                        String[] html = metadataHandler.getHtml(metadata);
                        if (html != null) {
                            sb.append(HtmlUtil.formEntryTop(html[0],
                                    html[1]));
                            break;
                        }
                    }
                }
            }
        }
        sb.append("</table>\n");
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

        String       output     = request.getOutput();
        boolean      showApplet = getRepository().isAppletEnabled(request);
        String       title      = group.getFullName();
        StringBuffer sb         = new StringBuffer();

        if (request.exists(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }

        int cnt = subGroups.size() + entries.size();
        int max = request.get(ARG_MAX, Repository.MAX_ROWS);
        //        System.err.println ("cnt:" + cnt + " " + max);

        if ((cnt > 0) && ((cnt == max) || request.defined(ARG_SKIP))) {
            int skip = Math.max(0, request.get(ARG_SKIP, 0));
            sb.append(msgLabel("Results") +  (skip + 1) + "-" + (skip + cnt));
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


        if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
            showApplet = output.equals(OUTPUT_TIMELINE);
            sb.append(HtmlUtil.p());
            boolean showMetadata = request.get(ARG_SHOWMETADATA, false);
            if ( !group.isDummy()) {
                String[] crumbs = getRepository().getBreadCrumbs(request,
                                      group, false,
                                      ARG_SHOWMETADATA + "=" + showMetadata);
                title = crumbs[0];
                sb.append(crumbs[1]);
            } else {
                title = group.getName();
                if ((subGroups.size() == 0) && (entries.size() == 0)) {
                    sb.append(getRepository().note(msg("No entries found")));
                }
            }


            if (group.getDescription().length() > 0) {
                sb.append(HtmlUtil.br());
                sb.append(group.getDescription());
                sb.append(HtmlUtil.br());
            }



            if ( !showApplet) {
                getMetadataHtml(request, group, sb);
            }

            //            sb.append("<hr>");

            sb.append(HtmlUtil.p());
            if (subGroups.size() > 0) {
                sb.append(HtmlUtil.bold(msgLabel("Groups")));
                sb.append("<ul>");
                for (Group subGroup : subGroups) {
                    List<Metadata> metadataList =
                        getMetadataManager().getMetadata(subGroup);
                    String icon = "";

                    /**
                     * for (Metadata metadata : metadataList) {
                     *   if(metadata.getType().equals(ThreddsMetadataHandler.TAG_ICON)) {
                     *       icon = HtmlUtil.img(metadata.getAttr1(),"View " + subGroup.getName()," width=75 border=0 ") + HtmlUtil.space(1);
                     *       break;
                     *   }
                     *   }
                     */

                    //                    sb.append(getRepository().getEntryLinks(request, subGroup));
                    sb.append("<li>");
                    String linkText = icon + subGroup.getName();
                    String groupLink =
                        HtmlUtil.href(
                            HtmlUtil.url(
                                getRepository().URL_GROUP_SHOW, ARG_ID,
                                subGroup.getId(), ARG_OUTPUT, output,
                                ARG_SHOWMETADATA, showMetadata
                            ? "true"
                            : "false"), linkText);
                    sb.append(groupLink);
                    if (showMetadata
                            && (subGroup.getDescription().length() > 0)) {
                        sb.append("<br>");
                        sb.append(subGroup.getDescription());
                    }

                    //                    sb.append("\n<br>\n");
                }
                sb.append("</ul>");
            }
            if (entries.size() > 0) {
                sb.append("\n");
                sb.append(HtmlUtil.bold(msgLabel("Entries")));
                if ((entries.size() > 0) && showApplet) {
                    sb.append(getTimelineApplet(request, entries));
                }
                sb.append(HtmlUtil.br());
                getEntryHtml(sb, entries, request, true, false);
            }
        }

        Result result = new Result(title, sb, getMimeType(output));
        result.putProperty(
            PROP_NAVSUBLINKS,
            getHeader(
                request, output,
                getRepository().getOutputTypesForGroup(
                    request, group, subGroups, entries)));

        return result;

    }









    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getTimelineApplet(Request request, List<Entry> entries)
            throws Exception {
        String timelineAppletTemplate =
            getRepository().getResource(PROP_HTML_TIMELINEAPPLET);
        List times  = new ArrayList();
        List labels = new ArrayList();
        List ids    = new ArrayList();
        for (Entry entry : entries) {
            times.add(SqlUtil.format(new Date(entry.getStartDate())));
            labels.add(entry.getName());
            ids.add(entry.getId());
        }
        String tmp = StringUtil.replace(timelineAppletTemplate, "%times%",
                                        StringUtil.join(",", times));
        tmp = StringUtil.replace(tmp, "%labels%",
                                 StringUtil.join(",", labels));
        tmp = StringUtil.replace(tmp, "%ids%", StringUtil.join(",", ids));
        tmp = StringUtil.replace(tmp, "%loadurl%",
                                 HtmlUtil.url(getRepository().URL_GETENTRIES,
                                     ARG_IDS, "%ids%", ARG_OUTPUT,
                                     OUTPUT_HTML));
        return tmp;

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuffer getEntriesList(Request request, List<Entry> entries)
            throws Exception {
        StringBuffer sb         = new StringBuffer();
        String       output     = request.getOutput();
        boolean      showApplet = getRepository().isAppletEnabled(request);
        sb.append(HtmlUtil.p());
        if (entries.size() == 0) {
            sb.append(HtmlUtil.bold(msg("Nothing Found")) + "<p>");
        }
        sb.append(HtmlUtil.formTable());
        showApplet = showApplet && output.equals(OUTPUT_TIMELINE);

        StringBufferCollection sbc = new StringBufferCollection();
        for (Entry entry : entries) {
            StringBuffer ssb =
                sbc.getBuffer(entry.getTypeHandler().getDescription());
            String links =
                HtmlUtil.checkbox("entry_" + entry.getId(), "true") + " "
                + getRepository().getEntryLinks(request, entry);

            ssb.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
            String col1 =
                links + " "
                + HtmlUtil.href(HtmlUtil.url(getRepository().URL_ENTRY_SHOW,
                                             ARG_ID,
                                             entry.getId()), entry.getName());
            String col2 = "" + new Date(entry.getStartDate());
            ssb.append(HtmlUtil.row(HtmlUtil.cols(col1, col2)));
        }


        if ((entries.size() > 0) && showApplet) {
            sb.append(getTimelineApplet(request, entries));
        }

        sb.append(HtmlUtil.form(getRepository().URL_GETENTRIES,
                                "name=\"getentries\" method=\"post\""));
        if (entries.size() > 0) {
            sb.append(HtmlUtil.submit(msg("Get selected"), "getselected"));
            sb.append(HtmlUtil.submit(msg("Get all"), "getall"));
            sb.append(" As: ");
            List outputList =
                getRepository().getOutputTypesForEntries(request, entries);
            sb.append(HtmlUtil.select(ARG_OUTPUT, outputList));
        }
        sb.append("<br>");
        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb  = sbc.getBuffer(type);
            sb.append(HtmlUtil.row(HtmlUtil.cols(HtmlUtil.bold(msgLabel("Type")
                    + type))));
            sb.append(ssb);
        }

        sb.append(HtmlUtil.formClose());
        sb.append(HtmlUtil.formTableClose());
        return sb;
    }




}


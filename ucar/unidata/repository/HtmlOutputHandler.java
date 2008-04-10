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


import ucar.unidata.sql.Clause;
import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
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
public class HtmlOutputHandler extends OutputHandler {



    /** _more_ */
    public static final String OUTPUT_TIMELINE = "default.timeline";

    public static final String OUTPUT_TIMELINE_DATA = "default.timelinedata";


    /** _more_ */
    public static final String OUTPUT_GRAPH = "default.graph";

    /** _more_ */
    public static final String OUTPUT_CLOUD = "default.cloud";

    /** _more_          */
    public static final String OUTPUT_GROUPXML = "groupxml";

    /** _more_          */
    public static final String OUTPUT_METADATAXML = "metadataxml";



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
            || output.equals(OUTPUT_TIMELINE_DATA)
               || output.equals(OUTPUT_GRAPH) || output.equals(OUTPUT_CLOUD)
               || output.equals(OUTPUT_GROUPXML)
               || output.equals(OUTPUT_METADATAXML);
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
    protected void getOutputTypesFor(Request request, String what, List<OutputType> types)
            throws Exception {
        if (what.equals(WHAT_ENTRIES)) {
            types.add(new OutputType("Entry", OUTPUT_HTML));
            if (getRepository().isAppletEnabled(request)) {
                types.add(new OutputType("Timeline", OUTPUT_TIMELINE));
            }
        } else if (what.equals(WHAT_TAG)) {
            types.add(new OutputType("Tag Html", OUTPUT_HTML));
            types.add(new OutputType("Tag Cloud", OUTPUT_CLOUD));
        } else if (what.equals(WHAT_TYPE)) {
            types.add(new OutputType("Type Html", OUTPUT_HTML));
        } else {
            types.add(new OutputType("Entry", OUTPUT_HTML));
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
                                            List<Entry> entries, List<OutputType> types)
            throws Exception {
        types.add(new OutputType("Entry", OUTPUT_HTML));
        if (entries.size() > 1) {
            types.add(new OutputType("Timeline", OUTPUT_TIMELINE));
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
    public Result getMetadataXml(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        request.put(ARG_OUTPUT, OUTPUT_HTML);
        String  links  = getRepository().getEntryLinksHtml(request, entry);
        boolean didOne = false;
        sb.append("<table>");
        sb.append(HtmlUtil.row(HtmlUtil.colspan("<center>" + links
                + "</center>", 2)));
        sb.append(entry.getTypeHandler().getInnerEntryContent(entry, request,
                OutputHandler.OUTPUT_HTML, false));
        getMetadataHtml(request, entry, sb, false);

        sb.append("</table>");
        StringBuffer xml = new StringBuffer("<content>\n");

        XmlUtil.appendCdata(xml,
                            getRepository().translate(request,
                                sb.toString()));
        xml.append("\n</content>");
        //        System.err.println(xml);
        return new Result("", xml, "text/xml");

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
        String output = request.getOutput();
        if (output.equals(OUTPUT_METADATAXML)) {
            return getMetadataXml(request, entry);
        }

        TypeHandler typeHandler =
            getRepository().getTypeHandler(entry.getType());
        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                              false, "");
        StringBuffer sb = new StringBuffer();
        sb.append(crumbs[1]);

        StringBuffer infoSB = typeHandler.getEntryContent(entry, request,
                                  true);
        sb.append(getRepository().makeShowHideBlock(request, "block.info",
                msg("Information"), infoSB, true));


        getMetadataHtml(request, entry, sb, true);
        getCommentBlock(request, entry, sb);
        getAssociationBlock(request, entry, sb);
        Result result = new Result(msgLabel("Entry") + entry.getLabel(), sb,
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
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void getAssociationBlock(Request request, Entry entry,
                                    StringBuffer sb)
            throws Exception {
        boolean canEdit= getAccessManager().canDoAction(request, entry,
                                                        Permission.ACTION_EDIT);
        List<Association> associations =
            getRepository().getAssociations(request, entry);
        if (associations.size() == 0) {
            return;
        }
        StringBuffer assocSB = new StringBuffer();
        assocSB.append("<table>");
        for (Association association : associations) {
            Entry fromEntry = null;
            Entry toEntry   = null;
            if (association.getFromId().equals(entry.getId())) {
                fromEntry = entry;
            } else {
                fromEntry = getRepository().getEntry(request,association.getFromId());
            }
            if (association.getToId().equals(entry.getId())) {
                toEntry = entry;
            } else {
                toEntry = getRepository().getEntry(request,association.getToId());
            }
            if ((fromEntry == null) || (toEntry == null)) {
                continue;
            }
            assocSB.append("<tr>");
            if(canEdit) {
                assocSB.append(HtmlUtil.cols(HtmlUtil.href(
                                                           request.url(getRepository().URL_ASSOCIATION_DELETE,
                                                                       ARG_ASSOCIATION,
                                                                       association.getId()),HtmlUtil.img(getRepository().fileUrl(ICON_DELETE),msg("Delete association")))));
            }
            assocSB.append("<td>");
            assocSB.append(((fromEntry == entry)
                            ? fromEntry.getLabel()
                            : getRepository().getEntryUrl(request,
                            fromEntry)));
            assocSB.append("&nbsp;&nbsp;");
            assocSB.append("</td><td>");
            assocSB.append(HtmlUtil.bold(association.getName()));
            assocSB.append("</td><td>");
            assocSB.append(HtmlUtil.img(getRepository().fileUrl(ICON_ARROW)));
            assocSB.append("&nbsp;&nbsp;");
            assocSB.append("</td><td>");
            assocSB.append(((toEntry == entry)
                            ? toEntry.getLabel()
                            : getRepository().getEntryUrl(request, toEntry)));
            assocSB.append("</td></tr>");
        }
        assocSB.append("</table>");
        sb.append(getRepository().makeShowHideBlock(request,
                "block.associations", msg("Associations"), assocSB, false));
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
    protected String getEntryUrl(Request request, Entry entry) {
        return repository.getEntryUrl(request, entry);
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
                        request.url(
                            getRepository().URL_ENTRY_SEARCHFORM, ARG_TYPE,
                            theTypeHandler.getType()), HtmlUtil.img(
                                getRepository().fileUrl(ICON_SEARCH),
                                msg("Search in Group"))));
                sb.append(" ");
                sb.append(HtmlUtil
                    .href(request
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
        List<OutputType> outputTypes =
            getRepository().getOutputTypesFor(request, what);
        int cnt = 0;
        sb.append("<b>");
        String initialOutput = request.getOutput("");
        for (OutputType tfo : outputTypes) {
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
                        request.url(
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
        TypeHandler typeHandler  = getRepository().getTypeHandler(request);
        String[]    associations = getRepository().getAssociations(request);


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
            Statement stmt2 = typeHandler.select(request, SqlUtil.count("*"),
                                  Clause.eq(COL_ASSOCIATIONS_NAME,
                                            association), "");

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
                String extra = XmlUtil.attrs("alt",
                                             msgLabel("Count") + count,
                                             "title",
                                             msgLabel("Count") + count);
                sb.append(
                    HtmlUtil.href(
                        request.url(
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
    public void getCommentBlock(Request request, Entry entry, StringBuffer sb)
            throws Exception {
        List<Comment> comments = getRepository().getComments(request, entry);
        if (comments.size() > 0) {
            String commentHtml = getRepository().getCommentHtml(request,
                                     entry);
            sb.append(getRepository().makeShowHideBlock(request, "comments",
                    msg("Comments"), new StringBuffer(commentHtml), false));

        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param decorate _more_
     *
     * @throws Exception _more_
     */
    public void getMetadataHtml(Request request, Entry entry,
                                StringBuffer sb, boolean decorate)
            throws Exception {
        boolean        showMetadata = request.get(ARG_SHOWMETADATA, false);
        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        if (metadataList.size() == 0) {
            return;
        }

        StringBuffer detailsSB = new StringBuffer();
        if (decorate) {
            detailsSB.append("<table cellspacing=\"5\">\n");
        }


        List<MetadataHandler> metadataHandlers =
            getMetadataManager().getMetadataHandlers();
        for (Metadata metadata : metadataList) {
            for (MetadataHandler metadataHandler : metadataHandlers) {
                if (metadataHandler.canHandle(metadata)) {
                    String[] html = metadataHandler.getHtml(metadata);
                    if (html != null) {
                        detailsSB.append(HtmlUtil.formEntryTop(html[0],
                                html[1]));
                        break;
                    }
                }
            }
        }
        if (decorate) {
            detailsSB.append("</table>\n");
            sb.append(getRepository().makeShowHideBlock(request, "details",
                    msg("Information"), detailsSB, false));
        } else {
            //            System.err.println (detailsSB);
            sb.append(detailsSB);
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getChildrenXml(Request request, List<Group> subGroups,
                                 List<Entry> entries)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       folder = getRepository().fileUrl(ICON_FOLDER_CLOSED);
        for (Group subGroup : subGroups) {
            sb.append("<li>");
            String groupLink = getAjaxLink(request, subGroup);
            sb.append(groupLink);
            sb.append(
                "<ul style=\"display:none;visibility:hidden\" class=\"folderblock\" id="
                + HtmlUtil.quote("block_" + subGroup.getId()) + "></ul>");
        }

        for (Entry entry : entries) {
            sb.append("<li>");
            sb.append(getAjaxLink(request, entry));
        }

        if ((subGroups.size() == 0) && (entries.size() == 0)) {
            sb.append("No sub-groups");
        }
        StringBuffer xml = new StringBuffer("<content>\n");
        XmlUtil.appendCdata(xml,
                            getRepository().translate(request,
                                sb.toString()));
        xml.append("\n</content>");
        //        System.err.println(xml);
        return new Result("", xml, "text/xml");
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

        String output = request.getOutput();
        if (output.equals(OUTPUT_GROUPXML)) {
            return getChildrenXml(request, subGroups, entries);
        }
        if (output.equals(OUTPUT_METADATAXML)) {
            return getMetadataXml(request, group);
        }
        boolean      showApplet = getRepository().isAppletEnabled(request);
        String       title      = group.getFullName();
        StringBuffer sb         = new StringBuffer();

        if (request.exists(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }


        showNext(request, subGroups, entries, sb);




        if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
            showApplet = output.equals(OUTPUT_TIMELINE);
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
                getMetadataHtml(request, group, sb, true);
                getCommentBlock(request, group, sb);
                getAssociationBlock(request, group, sb);
            }

            if (subGroups.size() > 0) {
                StringBuffer groupsSB = new StringBuffer();
                groupsSB.append(
                    "<ul class=\"folderblock\" style=\"list-style-image : url("
                    + getRepository().fileUrl(ICON_BLANK) + ")\">");
                for (Group subGroup : subGroups) {
                    List<Metadata> metadataList =
                        getMetadataManager().getMetadata(subGroup);
                    groupsSB.append("<li>");
                    String groupLink = getAjaxLink(request, subGroup);
                    groupsSB.append(groupLink);
                    groupsSB.append(
                        "<ul style=\"display:none;visibility:hidden\" class=\"folderblock\" id="
                        + HtmlUtil.quote("block_" + subGroup.getId())
                        + "></ul>");
                }
                groupsSB.append("</ul>");
                sb.append(getRepository().makeShowHideBlock(request,
                        "groups", msg("Groups"), groupsSB, true));

                //sb.append("</div>");
            }

            //            entries.addAll(subGroups);
            if (entries.size() > 0) {
                StringBuffer entriesSB = new StringBuffer();
                if (showApplet) {
                    entriesSB.append(getTimelineApplet(request, entries));
                }
                String link = getEntryHtml(entriesSB, entries, request, true,
                                           false, false);
                sb.append(getRepository().makeShowHideBlock(request,
                        "entries", msg("Entries") + link, entriesSB, true));

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
            labels.add(entry.getLabel());
            ids.add(entry.getId());
        }
        String tmp = StringUtil.replace(timelineAppletTemplate, "%times%",
                                        StringUtil.join(",", times));
        tmp = StringUtil.replace(tmp, "%root%", getRepository().getUrlBase());
        tmp = StringUtil.replace(tmp, "%labels%",
                                 StringUtil.join(",", labels));
        tmp = StringUtil.replace(tmp, "%ids%", StringUtil.join(",", ids));
        tmp = StringUtil.replace(tmp, "%loadurl%",
                                 request.url(getRepository().URL_GETENTRIES,
                                             ARG_IDS, "%ids%", ARG_OUTPUT,
                                             OUTPUT_HTML));
        return tmp;

    }







}


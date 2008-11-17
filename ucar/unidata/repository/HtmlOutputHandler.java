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
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


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
    public static final OutputType OUTPUT_TIMELINE = new OutputType("Timeline","default.timeline");

    /** _more_ */
    public static final OutputType OUTPUT_GRAPH = new OutputType("Graph","default.graph");

    /** _more_ */
    public static final OutputType OUTPUT_CLOUD = new OutputType("Cloud","default.cloud");

    /** _more_ */
    public static final OutputType OUTPUT_GROUPXML = new OutputType("groupxml");

    /** _more_ */
    public static final OutputType OUTPUT_SELECTXML = new OutputType("selectxml");


    /** _more_ */
    public static final OutputType OUTPUT_METADATAXML = new OutputType("metadataxml");



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
        addType(OUTPUT_HTML);
        addType(OUTPUT_TIMELINE);
        addType(OUTPUT_GRAPH);
        addType(OUTPUT_GROUPXML);
        addType(OUTPUT_SELECTXML);
        addType(OUTPUT_METADATAXML);
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
            throws Exception {
        List<Entry> entries = state.getAllEntries();
        types.add(OUTPUT_HTML);
        if (entries.size() > 1) {
            types.add(OUTPUT_TIMELINE);
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
        String links = getRepository().getEntryLinksHtml(request, entry,
                           false);
        boolean didOne = false;
        sb.append(links);
        sb.append("<table>");
        sb.append(entry.getTypeHandler().getInnerEntryContent(entry, request,
                OutputHandler.OUTPUT_HTML, true,false, true));
        for (TwoFacedObject tfo : getMetadataHtml(request, entry, false)) {
            sb.append(tfo.getId().toString());
        }




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
        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_METADATAXML)) {
            return getMetadataXml(request, entry);
        }

        TypeHandler typeHandler =
            getRepository().getTypeHandler(entry.getType());
        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                              false);
        StringBuffer sb = new StringBuffer();
        sb.append(crumbs[1]);

        StringBuffer infoSB = typeHandler.getEntryContent(entry, request, false,
                                  true);

        List          tabTitles  = new ArrayList<String>();
        List          tabContent = new ArrayList<String>();
        List<Boolean> treeShown  = new ArrayList<Boolean>();


        tabTitles.add(msg("Basic"));
        tabContent.add(infoSB);
        treeShown.add(true);

        for (TwoFacedObject tfo : getMetadataHtml(request, entry, true)) {
            tabTitles.add(tfo.toString());
            tabContent.add(tfo.getId());
            treeShown.add(false);
        }

        tabTitles.add("Comments");
        tabContent.add(getCommentBlock(request, entry));
        treeShown.add(false);

        tabTitles.add("Associations");
        tabContent.add(getAssociationBlock(request, entry));
        treeShown.add(false);

        tabTitles.add("Actions");
        tabContent.add(getRepository().getEntryLinksList(request, entry));
        treeShown.add(false);

        addDescription(request, entry, sb);

        getMetadataManager().decorateEntry(request, entry, sb,false);
        sb.append(HtmlUtil.br());
        sb.append(getRepository().makeTabs(tabTitles, tabContent, true));
        return makeLinksResult(request, msgLabel("Entry") + entry.getLabel(),
                               sb, new State(entry));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public StringBuffer getAssociationBlock(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        boolean canEdit = getAccessManager().canDoAction(request, entry,
                              Permission.ACTION_EDIT);
        List<Association> associations =
            getRepository().getAssociations(request, entry);
        if (associations.size() == 0) {
            return sb;
        }
        sb.append("<table>");
        for (Association association : associations) {
            Entry fromEntry = null;
            Entry toEntry   = null;
            if (association.getFromId().equals(entry.getId())) {
                fromEntry = entry;
            } else {
                fromEntry = getRepository().getEntry(request,
                        association.getFromId());
            }
            if (association.getToId().equals(entry.getId())) {
                toEntry = entry;
            } else {
                toEntry = getRepository().getEntry(request,
                        association.getToId());
            }
            if ((fromEntry == null) || (toEntry == null)) {
                continue;
            }
            sb.append("<tr>");
            if (canEdit) {
                sb.append(
                    HtmlUtil.cols(
                        HtmlUtil.href(
                            request.url(
                                getRepository().URL_ASSOCIATION_DELETE,
                                ARG_ASSOCIATION,
                                association.getId()), HtmlUtil.img(
                                    getRepository().fileUrl(ICON_DELETE),
                                    msg("Delete association")))));
            }
            List args = Misc.newList(ARG_SHOW_ASSOCIATIONS, "true");
            sb.append("<td>");
            sb.append(((fromEntry == entry)
                       ? fromEntry.getLabel()
                       : getRepository().getEntryLink(request, fromEntry,
                       args)));
            sb.append("&nbsp;&nbsp;");
            sb.append("</td><td>");
            sb.append(HtmlUtil.bold(association.getName()));
            sb.append("</td><td>");
            sb.append(HtmlUtil.img(getRepository().fileUrl(ICON_ARROW)));
            sb.append("&nbsp;&nbsp;");
            sb.append("</td><td>");
            sb.append(((toEntry == entry)
                       ? toEntry.getLabel()
                       : getRepository().getEntryLink(request, toEntry,
                       args)));
            sb.append("</td></tr>");
        }
        sb.append("</table>");
        return sb;
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
    protected String getEntryLink(Request request, Entry entry) {
        return repository.getEntryLink(request, entry);
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
        OutputType    output = request.getOutput();
        if (output.equals(OUTPUT_HTML)) {
            //            appendListHeader(request, output, WHAT_TYPE, sb);
            sb.append("<ul>");
        }

        for (TypeHandler theTypeHandler : typeHandlers) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(
                    HtmlUtil.href(
                        request.url(
                            getRepository().URL_SEARCH_FORM, ARG_TYPE,
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




    /*
    protected Result listTags(Request request, List<Tag> tags)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        OutputType      output = request.getOutput();
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
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_TIMELINE)) {
            return getRepository().getMimeTypeFromSuffix(".html");
        } else if (output.equals(OUTPUT_GRAPH)) {
            return getRepository().getMimeTypeFromSuffix(".xml");
        } else if (output.equals(OUTPUT_HTML)) {
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
        OutputType      output = request.getOutput();
        if (output.equals(OUTPUT_HTML)) {
            //            appendListHeader(request, output, WHAT_ASSOCIATION, sb);
            sb.append("<ul>");
        }
        TypeHandler typeHandler  = getRepository().getTypeHandler(request);
        String[]    associations = getRepository().getAssociations(request);


        if (associations.length == 0) {
            if (output.equals(OUTPUT_HTML)) {
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
                                  Clause.eq(Tables.ASSOCIATIONS.COL_NAME,
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
     *
     * @return _more_
     * @throws Exception _more_
     */
    public StringBuffer getCommentBlock(Request request, Entry entry)
            throws Exception {
        StringBuffer  sb       = new StringBuffer();
        List<Comment> comments = getRepository().getComments(request, entry);
        if (comments.size() > 0) {
            sb.append(getRepository().getCommentHtml(request, entry));
        }
        return sb;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param decorate _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    private List<TwoFacedObject> getMetadataHtml(Request request,
            Entry entry, boolean decorate)
            throws Exception {
        List<TwoFacedObject> result = new ArrayList<TwoFacedObject>();
        boolean showMetadata        = request.get(ARG_SHOWMETADATA, false);
        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        if (metadataList.size() == 0) {
            return result;
        }


        Hashtable    catMap = new Hashtable();
        List<String> cats   = new ArrayList<String>();
        List<MetadataHandler> metadataHandlers =
            getMetadataManager().getMetadataHandlers();

        boolean canEdit = getAccessManager().canDoAction(request, entry,
                                                         Permission.ACTION_EDIT);
        boolean didone = false;
        for (Metadata metadata : metadataList) {
            for (MetadataHandler metadataHandler : metadataHandlers) {
                if ( !metadataHandler.canHandle(metadata)) {
                    continue;
                }
                String[] html = metadataHandler.getHtml(request, entry, metadata);
                if (html == null) {
                    continue;
                }
                Metadata.Type type =
                    metadataHandler.findType(metadata.getType());
                String cat = type.getCategory();
                if ( !decorate) {
                    cat = "Metadata";
                }
                Object[] blob     = (Object[]) catMap.get(cat);
                boolean  firstOne = false;
                if (blob == null) {
                    firstOne = true;
                    blob = new Object[] { new StringBuffer(),
                                          new Integer(1) };
                    catMap.put(cat, blob);
                    cats.add(cat);
                }
                StringBuffer sb     = (StringBuffer) blob[0];
                int          rowNum = ((Integer) blob[1]).intValue();

                if (firstOne) {
                    if (decorate) {
                        sb.append(
                            "<table width=\"100%\" border=0 cellspacing=\"0\" cellpadding=\"3\">\n");
                    }
                    if(canEdit) {
                        if(decorate)
                            sb.append("<tr><td></td><td>");
                        sb.append(
                                  new Link(
                                           request.entryUrl(getRepository().URL_ENTRY_FORM, entry),
                                           getRepository().fileUrl(ICON_EDIT), msg("Edit Entry")));
                        sb.append(
                                  new Link(
                                           request.entryUrl(getRepository().getMetadataManager().URL_METADATA_ADDFORM, entry),
                                           getRepository().fileUrl(ICON_ADD), msg("Add Metadata")));
                        if(decorate)
                            sb.append("</td></tr>");
                    }
                }
                String theClass = HtmlUtil.cssClass("listrow" + rowNum);
                if (decorate) {
                    String row =
                        " <tr  " + theClass
                        + " valign=\"top\"><td width=\"10%\" align=\"right\" valign=\"top\" class=\"formlabel\"><nobr>"
                        + html[0] + "</nobr></td><td>" + html[1]
                        + "</td></tr>";
                    sb.append(row);
                } else {
                    String row =
                        " <tr  valign=\"top\"><td width=\"10%\" align=\"right\" valign=\"top\" class=\"formlabel\"><nobr>"
                        + html[0] + "</nobr></td><td>" + html[1]
                        + "</td></tr>";
                    sb.append(row);
                }
                if (++rowNum > 2) {
                    rowNum = 1;
                }
                blob[1] = new Integer(rowNum);
            }
        }

        for (String cat : cats) {
            Object[]     blob = (Object[]) catMap.get(cat);
            StringBuffer sb   = (StringBuffer) blob[0];
            if (decorate) {
                sb.append("</table>\n");
            }
            result.add(new TwoFacedObject(cat, sb));
        }

        return result;
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
            String groupLink = getRepository().getAjaxLink(request, subGroup);
            sb.append(groupLink);
            sb.append(
                "<ul style=\"display:none;visibility:hidden\" class=\"folderblock\" id="
                + HtmlUtil.quote("block_" + subGroup.getId()) + "></ul>");
        }

        for (Entry entry : entries) {
            sb.append("<li>");
            sb.append(getRepository().getAjaxLink(request, entry));
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


    public Result getSelectXml(Request request, List<Group> subGroups,
                                 List<Entry> entries)
            throws Exception {
        String target = request.getString(ATTR_TARGET,"");
        StringBuffer sb     = new StringBuffer();
        String       folder = getRepository().fileUrl(ICON_FOLDER_CLOSED);
        for (Group subGroup : subGroups) {
            //            sb.append("<li>");
            String groupLink = getSelectLink(request, subGroup,target);
            sb.append(groupLink);
            sb.append("<br>");
            sb.append(
                "<div style=\"display:none;visibility:hidden\" class=\"folderblock\" id="
                + HtmlUtil.quote("block_" + subGroup.getId()) + "></div>");
        }

        for (Entry entry : entries) {
            //            sb.append("<li>");
            //            sb.append(getSelectLink(request, entry));
        }


        StringBuffer xml = new StringBuffer("<content>\n");
        XmlUtil.appendCdata(xml,
                            getRepository().translate(request,
                                sb.toString()));
        xml.append("\n</content>");
        //        System.err.println(xml);
        return new Result("", xml, "text/xml");
    }



    private void addDescription(Request request, Entry entry, StringBuffer sb) {
        String desc= entry.getDescription();
        if (desc.length() > 0) {
            desc = getRepository().processText(request, entry, desc);
            StringBuffer descSB = new StringBuffer("\n<div class=\"description\">\n");
            descSB.append(desc);
            descSB.append("</div>\n");
            sb.append(getRepository().makeShowHideBlock(request, "Description",
                                                        descSB,true));
        }
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

        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_GROUPXML)) {
            return getChildrenXml(request, subGroups, entries);
        }

        if (output.equals(OUTPUT_SELECTXML)) {
            return getSelectXml(request, subGroups, entries);
        }

        if (output.equals(OUTPUT_METADATAXML)) {
            return getMetadataXml(request, group);
        }

        boolean      showApplet = output.equals(OUTPUT_TIMELINE);

        String       title      = group.getFullName();
        StringBuffer sb         = new StringBuffer();
        if (request.exists(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
            request.remove(ARG_MESSAGE);
        }
        showNext(request, subGroups, entries, sb);

        if ( !group.isDummy()) {
            String[] crumbs = getRepository().getBreadCrumbs(request, group,
                                  false);
            title = crumbs[0];
            sb.append(crumbs[1]);
        } else {
            title = group.getName();
            if ((subGroups.size() == 0) && (entries.size() == 0)) {
                sb.append(getRepository().note(msg("No entries found")));
            }
        }

        if(!showApplet) {
            addDescription(request, group, sb);
            StringBuffer metadataSB = new StringBuffer();
            getMetadataManager().decorateEntry(request, group, metadataSB, false);
            String metataDataHtml = metadataSB.toString();
            if(metataDataHtml.length()>0) {
                sb.append(getRepository().makeShowHideBlock(request, "Attachments",
                                                            new StringBuffer("<div class=\"description\">" +metadataSB+"</div>"),true));
            }

        }



        List          tabTitles  = new ArrayList<String>();
        List          tabContent = new ArrayList<String>();
        List<Boolean> treeShown  = new ArrayList<Boolean>();
        if ( !group.isDummy()) {
            tabTitles.add("Basic");
            tabContent.add(group.getTypeHandler().getEntryContent(group,
                    request, false,true));
            treeShown.add(false);
        }

        if ( !showApplet) {
            if ( !group.isDummy()) {
                for (TwoFacedObject tfo : getMetadataHtml(request, group,
                        true)) {
                    tabTitles.add(tfo.toString());
                    tabContent.add(tfo.getId());
                    treeShown.add(false);
                }
                tabTitles.add("Comments");
                tabContent.add(getCommentBlock(request, group));
                treeShown.add(false);
                tabTitles.add("Associations");
                tabContent.add(getAssociationBlock(request, group));
                treeShown.add(request.get(ARG_SHOW_ASSOCIATIONS, false));
                tabTitles.add("Actions");
                tabContent.add(getRepository().getEntryLinksList(request,
                        group));
                treeShown.add(false);
            }
        } else {
            List allEntries = new ArrayList(entries);
            allEntries.addAll(subGroups);
            sb.append(getTimelineApplet(request, allEntries));
        }

        if ( !showApplet) {
            String tmp = getRepository().makeTabs(tabTitles, tabContent,
                             true, "tabcontent_fixedheight");
            tabTitles  = new ArrayList<String>();
            tabContent = new ArrayList<String>();
            treeShown  = new ArrayList<Boolean>();

            if ( !group.isDummy()) {
                tabContent.add(HtmlUtil.div(tmp,
                                            " style=\"margin-left:15px;\" "));
                tabTitles.add("Information");
                treeShown.add( !((subGroups.size() > 0)
                                 || (entries.size() > 0)));
            }
        }

        if (subGroups.size() > 0) {
            StringBuffer groupsSB = new StringBuffer();
            groupsSB.append(
                "<div><ul class=\"folderblock\" style=\"list-style-image : url("
                + getRepository().fileUrl(ICON_BLANK) + ")\">");
            for (Group subGroup : subGroups) {
                List<Metadata> metadataList =
                    getMetadataManager().getMetadata(subGroup);
                groupsSB.append("<li>");
                String groupLink = getRepository().getAjaxLink(request, subGroup);
                groupsSB.append(groupLink);
                groupsSB.append(
                    "<ul style=\"display:none;visibility:hidden\" class=\"folderblock\" id="
                    + HtmlUtil.quote("block_" + subGroup.getId()) + "></ul>");
            }
            groupsSB.append("</ul></div>");
            tabTitles.add("Groups");
            tabContent.add(groupsSB);
            treeShown.add(true);
        }

        if (entries.size() > 0) {
            StringBuffer entriesSB = new StringBuffer();

            String link = getEntryHtml(entriesSB, entries, request, true,
                                       false, group.isDummy());
            tabTitles.add("Entries" + link);
            tabContent.add(entriesSB.toString());
            treeShown.add(true);
        }


        for (int i = 0; i < tabTitles.size(); i++) {
            String tabTitle = tabTitles.get(i).toString();
            String content  = tabContent.get(i).toString();
            if (content.length() == 0) {
                continue;
            }
            sb.append(getRepository().makeShowHideBlock(request, tabTitle,
                    new StringBuffer(content), treeShown.get(i)));
        }

        String messageLeft = request.getLeftMessage();
        if (messageLeft != null) {
            sb = new StringBuffer(
                "<table width=\"100%\" border=0><tr valign=\"top\"><td width=\"100\"><nobr>"
                + messageLeft + "</nobr></td><td>" + sb
                + "</td></tr></table>");
        }

        return makeLinksResult(request, title, sb,
                               new State(group, subGroups, entries));

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
        tmp = StringUtil.replace(
            tmp, "%loadurl%",
            request.url(
                getRepository().URL_ENTRY_GETENTRIES, ARG_IDS, "%ids%",
                ARG_OUTPUT, OUTPUT_HTML));
        return tmp;

    }







}


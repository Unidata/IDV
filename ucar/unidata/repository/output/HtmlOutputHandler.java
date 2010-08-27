/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository.output;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.metadata.*;
import ucar.unidata.repository.type.*;

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


import java.net.*;

import java.sql.ResultSet;
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
    public static final OutputType OUTPUT_TIMELINE =
        new OutputType("Timeline", "default.timeline",
                       OutputType.TYPE_HTML | OutputType.TYPE_FORSEARCH, "",
                       ICON_CLOCK);

    /** _more_ */
    public static final OutputType OUTPUT_GRAPH =
        new OutputType("Graph", "default.graph",
                       OutputType.TYPE_HTML | OutputType.TYPE_FORSEARCH, "",
                       ICON_GRAPH);

    /** _more_ */
    public static final OutputType OUTPUT_CLOUD = new OutputType("Cloud",
                                                      "default.cloud",
                                                      OutputType.TYPE_HTML);

    /** _more_ */
    public static final OutputType OUTPUT_INLINE =
        new OutputType("inline", OutputType.TYPE_INTERNAL);

    /** _more_ */
    public static final OutputType OUTPUT_SELECTXML =
        new OutputType("selectxml", OutputType.TYPE_INTERNAL);


    /** _more_ */
    public static final OutputType OUTPUT_METADATAXML =
        new OutputType("metadataxml", OutputType.TYPE_INTERNAL);

    /** _more_ */
    public static final OutputType OUTPUT_LINKSXML =
        new OutputType("linksxml", OutputType.TYPE_INTERNAL);



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
        addType(OUTPUT_INLINE);
        addType(OUTPUT_SELECTXML);
        addType(OUTPUT_METADATAXML);
        addType(OUTPUT_LINKSXML);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        List<Entry> entries = state.getAllEntries();
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_HTML));
            if (entries.size() > 1) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_TIMELINE));
            }
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
        boolean didOne = false;
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE));
        sb.append(entry.getTypeHandler().getInnerEntryContent(entry, request,
                OutputHandler.OUTPUT_HTML, true, true, true));
        for (TwoFacedObject tfo :
                getMetadataHtml(request, entry, false, false)) {
            sb.append(tfo.getId().toString());
        }
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));

        String links = getEntryManager().getEntryActionsTable(request, entry,
                           OutputType.TYPE_ALL);
        String contents = HtmlUtil.makeTabs(Misc.newList(msg("Information"),
                              msg(LABEL_LINKS)), Misc.newList(sb.toString(),
                                  links), true, "tab_content");

        //        String       contents = sb.toString();

        StringBuffer xml = new StringBuffer("<content>\n");
        XmlUtil.appendCdata(xml,
                            getRepository().translate(request, contents));
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
    public Result getLinksXml(Request request, Entry entry) throws Exception {
        StringBuffer sb = new StringBuffer("<content>\n");
        String links = getEntryManager().getEntryActionsTable(request, entry,
                           OutputType.TYPE_ALL);
        StringBuffer inner = new StringBuffer();
        String cLink =
            HtmlUtil.jsLink(HtmlUtil.onMouseClick("hidePopupObject();"),
                            HtmlUtil.img(iconUrl(ICON_CLOSE)), "");
        inner.append(cLink);
        inner.append(HtmlUtil.br());
        inner.append(links);
        XmlUtil.appendCdata(sb, inner.toString());
        sb.append("\n</content>");
        return new Result("", sb, "text/xml");
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        TypeHandler typeHandler =
            getRepository().getTypeHandler(entry.getType());
        if (outputType.equals(OUTPUT_METADATAXML)) {
            return getMetadataXml(request, entry);
        }
        if (outputType.equals(OUTPUT_LINKSXML)) {
            return getLinksXml(request, entry);
        }
        if (outputType.equals(OUTPUT_INLINE)) {
            String inline = typeHandler.getInlineHtml(request, entry);
            if (inline != null) {
                inline = getRepository().translate(request, inline);
                StringBuffer xml = new StringBuffer("<content>\n");
                XmlUtil.appendCdata(xml,
                                    "<div class=inline>" + inline + "</div>");
                xml.append("\n</content>");
                return new Result("", xml, "text/xml");
            }
            String wikiTemplate = getWikiText(request, entry);
            if (wikiTemplate != null) {
                String wiki = wikifyEntry(request, entry, wikiTemplate);
                wiki = getRepository().translate(request, wiki);
                StringBuffer xml = new StringBuffer("<content>\n");
                XmlUtil.appendCdata(xml,
                                    "<div class=inline>" + wiki + "</div>");
                xml.append("\n</content>");
                return new Result("", xml, "text/xml");
            }

            return getMetadataXml(request, entry);
        }

        Result typeResult = typeHandler.getHtmlDisplay(request, entry);
        if (typeResult != null) {
            return typeResult;
        }

        StringBuffer sb           = new StringBuffer();

        String       wikiTemplate = getWikiText(request, entry);
        if (wikiTemplate != null) {
            sb.append(wikifyEntry(request, entry, wikiTemplate));
        } else {
            addDescription(request, entry, sb, true);
            String informationBlock = getInformationTabs(request, entry,
                                          false);
            sb.append(HtmlUtil.makeShowHideBlock(msg("Information"),
                    informationBlock, true));

            StringBuffer metadataSB = new StringBuffer();

            getMetadataManager().decorateEntry(request, entry, metadataSB,
                    false);
            String metataDataHtml = metadataSB.toString();
            if (metataDataHtml.length() > 0) {
                sb.append(HtmlUtil.makeShowHideBlock(msg("Attachments"),
                        "<div class=\"description\">" + metadataSB
                        + "</div>", false));
            }
        }

        return makeLinksResult(request, msg("Entry"), sb, new State(entry));
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
    public String getEntryLink(Request request, Entry entry) {
        return getEntryManager().getEntryLink(request, entry);
    }







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
     * @param entry _more_
     * @param decorate _more_
     * @param addLink _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    private List<TwoFacedObject> getMetadataHtml(Request request,
            Entry entry, boolean decorate, boolean addLink)
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
            MetadataType type = getRepository().getMetadataManager().findType(
                                    metadata.getType());
            if (type == null) {
                continue;
            }
            MetadataHandler metadataHandler = type.getHandler();
            String[] html = metadataHandler.getHtml(request, entry, metadata);
            if (html == null) {
                continue;
            }
            String cat = type.getDisplayCategory();
            if ( !decorate) {
                cat = "Properties";
            }
            Object[] blob     = (Object[]) catMap.get(cat);
            boolean  firstOne = false;
            if (blob == null) {
                firstOne = true;
                blob     = new Object[] { new StringBuffer(),
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
                if (addLink && canEdit) {
                    if (decorate) {
                        sb.append("<tr><td></td><td>");
                    }
                    sb.append(
                        new Link(
                            request.entryUrl(
                                getMetadataManager().URL_METADATA_FORM,
                                entry), iconUrl(ICON_METADATA_EDIT),
                                        msg("Edit Metadata")));
                    sb.append(
                        new Link(
                            request.entryUrl(
                                getRepository().getMetadataManager()
                                    .URL_METADATA_ADDFORM, entry), iconUrl(
                                        ICON_METADATA_ADD), msg(
                                        "Add Property")));
                    if (decorate) {
                        sb.append("</td></tr>");
                    }
                }
            }
            String theClass = HtmlUtil.cssClass("listrow" + rowNum);
            if (decorate) {
                String row =
                    " <tr  " + theClass
                    + " valign=\"top\"><td width=\"10%\" align=\"right\" valign=\"top\" class=\"formlabel\"><nobr>"
                    + html[0] + "</nobr></td><td>"
                    + HtmlUtil.makeToggleInline("", html[1], true)
                    + "</td></tr>";
                sb.append(row);
            } else {
                String row =
                    " <tr  valign=\"top\"><td width=\"10%\" align=\"right\" valign=\"top\" class=\"formlabel\"><nobr>"
                    + html[0] + "</nobr></td><td>" + html[1] + "</td></tr>";
                sb.append(row);
            }
            if (++rowNum > 2) {
                rowNum = 1;
            }
            blob[1] = new Integer(rowNum);
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
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getActionXml(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(getEntryManager().getEntryActionsTable(request, entry,
                OutputType.TYPE_ALL));

        StringBuffer xml = new StringBuffer("<content>\n");
        XmlUtil.appendCdata(xml,
                            getRepository().translate(request,
                                sb.toString()));
        xml.append("\n</content>");
        return new Result("", xml, "text/xml");
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getChildrenXml(Request request, Group parent,
                                 List<Group> subGroups, List<Entry> entries)
            throws Exception {
        StringBuffer sb         = new StringBuffer();
        String       folder     = iconUrl(ICON_FOLDER_CLOSED);
        boolean      showLink   = request.get(ARG_SHOWLINK, true);
        boolean      onlyGroups = request.get(ARG_ONLYGROUPS, false);

        int          cnt        = 0;
        StringBuffer jsSB       = new StringBuffer();
        String       rowId;
        String       cbxId;
        String       cbxWrapperId;

        if ( !showingAll(request, subGroups, entries)) {
            sb.append(msgLabel("Showing") + " 1.."
                      + (subGroups.size() + entries.size()));
            sb.append(HtmlUtil.space(2));
            String url = request.getEntryUrl(
                             getRepository().URL_ENTRY_SHOW.toString(),
                             parent);
            url = HtmlUtil.url(url, ARG_ENTRYID, parent.getId());
            sb.append(HtmlUtil.href(url, msg("More...")));
            sb.append(HtmlUtil.br());
        }

        for (Group subGroup : subGroups) {
            cnt++;
            addEntryCheckbox(request, subGroup, sb, jsSB);
        }


        if ( !onlyGroups) {
            for (Entry entry : entries) {
                cnt++;
                addEntryCheckbox(request, entry, sb, jsSB);
            }
        }

        if (cnt == 0) {
            sb.append(HtmlUtil.tag(HtmlUtil.TAG_I, "",
                                   msg("No entries in this folder")));
            if (getAccessManager().hasPermissionSet(parent,
                    Permission.ACTION_VIEWCHILDREN)) {
                if ( !getAccessManager().canDoAction(request, parent,
                        Permission.ACTION_VIEWCHILDREN)) {
                    sb.append(HtmlUtil.space(1));
                    sb.append(
                        msg(
                        "You do not have permission to view the sub-folders of this entry"));
                }
            }
        }


        StringBuffer xml = new StringBuffer("<response><content>\n");
        XmlUtil.appendCdata(xml,
                            getRepository().translate(request,
                                sb.toString()));
        xml.append("\n</content>");

        xml.append("<javascript>");
        XmlUtil.appendCdata(xml,
                            getRepository().translate(request,
                                jsSB.toString()));
        xml.append("</javascript>");
        xml.append("\n</response>");
        return new Result("", xml, "text/xml");
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
    public Result getSelectXml(Request request, List<Group> subGroups,
                               List<Entry> entries)
            throws Exception {
        String       localeId = request.getString(ARG_LOCALEID, null);

        String       target   = request.getString(ATTR_TARGET, "");
        StringBuffer sb       = new StringBuffer();



        //If we have a localeid that means this is the first call
        if (localeId != null) {
            Entry localeEntry = getEntryManager().getEntry(request, localeId);
            if (localeEntry != null) {
                if ( !localeEntry.isGroup()) {
                    localeEntry = getEntryManager().getParent(request,
                            localeEntry);
                }
                if (localeEntry != null) {
                    Entry grandParent = getEntryManager().getParent(request,
                                            localeEntry);
                    String indent = "";
                    if (grandParent != null) {
                        sb.append(getSelectLink(request, grandParent,
                                target));
                        //indent = HtmlUtil.space(2);
                    }
                    sb.append(indent);
                    sb.append(getSelectLink(request, localeEntry, target));
                    localeId = localeEntry.getId();
                    sb.append(
                        "<hr style=\"padding:0px;margin-bottom:2px;  margin:0px;\">");
                }
            }


            List<FavoriteEntry> favoritesList =
                getUserManager().getFavorites(request, request.getUser());
            StringBuffer favorites = new StringBuffer();
            if (favoritesList.size() > 0) {
                sb.append(HtmlUtil.b(msg("Favorites")));
                sb.append(HtmlUtil.br());
                List favoriteLinks = new ArrayList();
                for (FavoriteEntry favorite : favoritesList) {
                    Entry favEntry = favorite.getEntry();
                    sb.append(getSelectLink(request, favEntry, target));
                }
                sb.append(
                    "<hr style=\"padding:0px;margin-bottom:2px;  margin:0px;\">");
            }


            List<Entry> cartEntries = getUserManager().getCart(request);
            if (cartEntries.size() > 0) {
                sb.append(HtmlUtil.b(msg("Cart")));
                sb.append(HtmlUtil.br());
                for (Entry cartEntry : cartEntries) {
                    sb.append(getSelectLink(request, cartEntry, target));
                }
                sb.append(
                    "<hr style=\"padding:0px;margin-bottom:2px;  margin:0px;\">");
            }
        }


        for (Group subGroup : subGroups) {
            if (Misc.equals(localeId, subGroup.getId())) {
                continue;
            }
            sb.append(getSelectLink(request, subGroup, target));
        }

        if (request.get(ARG_ALLENTRIES, false)) {
            for (Entry entry : entries) {
                sb.append(getSelectLink(request, entry, target));
            }
        }
        return makeAjaxResult(request,
                              getRepository().translate(request,
                                  sb.toString()));
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param open _more_
     */
    private void addDescription(Request request, Entry entry,
                                StringBuffer sb, boolean open) {
        String desc = entry.getDescription().trim();
        if ((desc.length() > 0) && !desc.startsWith("<wiki>")
                && !desc.equals("<nolinks>")) {
            desc = getEntryManager().processText(request, entry, desc);
            StringBuffer descSB =
                new StringBuffer("\n<div class=\"description\">\n");
            descSB.append(desc);
            descSB.append("</div>\n");

            //            sb.append(HtmlUtil.makeShowHideBlock(msg("Description"),
            //                    descSB.toString(), open));

            //            sb.append(HtmlUtil.makeToggleInline("",
            //                                                desc, true));
            sb.append(desc);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param fixedHeight _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getInformationTabs(Request request, Entry entry,
                                     boolean fixedHeight)
            throws Exception {
        String desc        = entry.getDescription();
        List   tabTitles   = new ArrayList<String>();
        List   tabContents = new ArrayList<String>();
        if (desc.length() > 0) {
            //            tabTitles.add("Description");
            //            desc = getEntryManager().processText(request, entry, desc);
            //            tabContents.add(desc);
        }

        tabTitles.add("Basic");
        Object basic;
        tabContents.add(basic = entry.getTypeHandler().getEntryContent(entry,
                request, false, true));


        for (TwoFacedObject tfo : getMetadataHtml(request, entry, true,
                true)) {
            tabTitles.add(tfo.toString());
            tabContents.add(tfo.getId());
        }
        tabTitles.add(msg("Comments"));
        tabContents.add(getCommentBlock(request, entry, true));
        if (request.get(ARG_SHOW_ASSOCIATIONS, false)) {
            tabTitles.add("selected:" + msg("Associations"));
        } else {
            tabTitles.add(msg("Associations"));
        }
        tabContents.add(getAssociationManager().getAssociationBlock(request,
                entry));

        //        tabTitles.add(msg(LABEL_LINKS));
        //        tabContents.add(getEntryManager().getEntryActionsTable(request, entry,
        //                OutputType.TYPE_ALL));


        return HtmlUtil.makeTabs(tabTitles, tabContents, true, (fixedHeight
                ? "tab_content_fixedheight"
                : "tab_content"));


    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Group group, List<Group> subGroups,
                              List<Entry> entries)
            throws Exception {


        TypeHandler typeHandler =
            getRepository().getTypeHandler(group.getType());

        if (outputType.equals(OUTPUT_INLINE)) {
            /*
            String wikiTemplate = getWikiText(request, group);
            if (wikiTemplate != null) {
                String wiki = wikifyEntry(request, group, wikiTemplate, true, subGroups,
                                          entries);
                wiki = getRepository().translate(request, wiki);
                StringBuffer xml = new StringBuffer("<content>\n");
                XmlUtil.appendCdata(xml,
                                    "<div class=inline>" +wiki+"</div>");
                xml.append("\n</content>");
                return new Result("", xml, "text/xml");
                }*/

            return getChildrenXml(request, group, subGroups, entries);
        }

        if (outputType.equals(OUTPUT_SELECTXML)) {
            return getSelectXml(request, subGroups, entries);
        }

        if (outputType.equals(OUTPUT_METADATAXML)) {
            return getMetadataXml(request, group);
        }

        if (outputType.equals(OUTPUT_LINKSXML)) {
            return getLinksXml(request, group);
        }

        if(typeHandler!=null) {
            Result typeResult = typeHandler.getHtmlDisplay(request, group,
                                                           subGroups, entries);
            if (typeResult != null) {
                return typeResult;
            }
        }


        boolean      showApplet = outputType.equals(OUTPUT_TIMELINE);

        StringBuffer sb         = new StringBuffer();
        request.appendMessage(sb);
        showNext(request, subGroups, entries, sb);


        boolean hasChildren = ((subGroups.size() != 0)
                               || (entries.size() != 0));

        if (group.isDummy()) {
            if ( !hasChildren) {
                sb.append(
                    getRepository().showDialogNote(msg("No entries found")));
            }
        }


        String wikiTemplate = getWikiText(request, group);

        if (showApplet) {
            List allEntries = new ArrayList(entries);
            allEntries.addAll(subGroups);
            sb.append(getTimelineApplet(request, allEntries));
        } else if ((wikiTemplate == null) && !group.isDummy()) {
            addDescription(request, group, sb, !hasChildren);
            String informationBlock = getInformationTabs(request, group,
                                          false);
            sb.append(HtmlUtil.makeShowHideBlock(msg("Information"),
                    informationBlock,
                    request.get(ARG_SHOW_ASSOCIATIONS, !hasChildren)));

            StringBuffer metadataSB = new StringBuffer();
            getMetadataManager().decorateEntry(request, group, metadataSB,
                    false);
            String metataDataHtml = metadataSB.toString();
            if (metataDataHtml.length() > 0) {
                sb.append(HtmlUtil.makeShowHideBlock(msg("Attachments"),
                        "<div class=\"description\">" + metadataSB
                        + "</div>", false));
            }
        }

        if (wikiTemplate != null) {
            sb.append(wikifyEntry(request, group, wikiTemplate, true,
                                  subGroups, entries));
        } else {
            List<Entry> allEntries = new ArrayList<Entry>();
            allEntries.addAll(subGroups);
            allEntries.addAll(entries);
            if (allEntries.size() > 0) {
                StringBuffer groupsSB = new StringBuffer();
                String link = getEntriesList(request, groupsSB, allEntries,
                                             allEntries, true, false, true,
                                             group.isDummy(),
                                             group.isDummy());
                sb.append(HtmlUtil.makeShowHideBlock(msg("Entries") + link,
                        groupsSB.toString(), true));
            }

            if ( !group.isDummy() && (subGroups.size() == 0)
                    && (entries.size() == 0)) {
                if (getAccessManager().hasPermissionSet(group,
                        Permission.ACTION_VIEWCHILDREN)) {
                    if ( !getAccessManager().canDoAction(request, group,
                            Permission.ACTION_VIEWCHILDREN)) {
                        sb.append(
                            getRepository().showDialogWarning(
                                "You do not have permission to view the sub-folders of this entry"));
                    }
                }
            }

        }


        String messageLeft = request.getLeftMessage();
        if (messageLeft != null) {
            sb = new StringBuffer(
                "<table width=\"100%\" border=0><tr valign=\"top\"><td width=\"200\"><nobr>"
                + messageLeft + "</nobr></td><td>" + sb
                + "</td></tr></table>");
        }

        return makeLinksResult(request, msg("Folder"), sb,
                               new State(group, subGroups, entries));
    }



    /** _more_ */
    private static boolean checkedTemplates = false;

    /** _more_ */
    private static String entryTemplate;

    /** _more_ */
    private static String groupTemplate;

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
    private String getWikiText(Request request, Entry entry)
            throws Exception {
        if ( !checkedTemplates) {
            entryTemplate =
                getRepository().getResource(RESOURCE_ENTRYTEMPLATE, true);
            groupTemplate =
                getRepository().getResource(RESOURCE_GROUPTEMPLATE, true);
            checkedTemplates = true;
        }
        if (entry.getDescription().trim().startsWith("<wiki>")) {
            return entry.getDescription();
        }
        if (entry.isGroup()) {
            return groupTemplate;
        }
        return entryTemplate;
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
    public String getTimelineApplet(Request request, List<Entry> entries)
            throws Exception {
        String timelineAppletTemplate =
            getRepository().getResource(PROP_HTML_TIMELINEAPPLET);
        List times  = new ArrayList();
        List labels = new ArrayList();
        List ids    = new ArrayList();
        for (Entry entry : entries) {
            String label = entry.getLabel();
            label = label.replaceAll(",", " ");
            times.add(SqlUtil.format(new Date(entry.getStartDate())));
            labels.add(label);
            ids.add(entry.getId());
        }
        String tmp = StringUtil.replace(timelineAppletTemplate, "${times}",
                                        StringUtil.join(",", times));
        tmp = StringUtil.replace(tmp, "${root}",
                                 getRepository().getUrlBase());
        tmp = StringUtil.replace(tmp, "${labels}",
                                 StringUtil.join(",", labels));
        tmp = StringUtil.replace(tmp, "${ids}", StringUtil.join(",", ids));
        tmp = StringUtil.replace(
            tmp, "${loadurl}",
            request.url(
                getRepository().URL_ENTRY_GETENTRIES, ARG_ENTRYIDS, "%ids%",
                ARG_OUTPUT, OUTPUT_HTML));
        return tmp;

    }







}

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

package ucar.unidata.repository.collab;


import org.w3c.dom.*;

import ucar.unidata.repository.*;


import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WikiUtil;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class WikiPageTypeHandler extends GenericTypeHandler {

    /** _more_          */
    public static String ASSOC_WIKILINK = "wikilink";



    /** _more_ */
    public static String TYPE_WIKIPAGE = "wikipage";

    /** _more_ */
    public static final String ARG_WIKI_TEXT = "wikipage.wikitext";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public WikiPageTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        if (request.get(ARG_WIKI_DETAILS, false)) {
            return null;
        }
        Result result = getRepository().getOutputHandler(
                            WikiPageOutputHandler.OUTPUT_WIKI).outputEntry(
                            request, entry);
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, Entry entry)
            throws Exception {
        super.deleteEntry(request, statement, entry);
        String query =
            SqlUtil.makeDelete(Tables.WIKIPAGEHISTORY.NAME,
                               Tables.WIKIPAGEHISTORY.COL_ENTRY_ID,
                               SqlUtil.quote(entry.getId()));
        statement.execute(query);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntry(Request request, Entry entry)
            throws Exception {
        Object[] values       = entry.getValues();
        String   originalText = null;
        if (values != null) {
            originalText = (String) values[0];
        }
        boolean wasNew = (values == null);
        super.initializeEntry(request, entry);
        String newText = (String) entry.getValues()[0];
        if ((originalText == null) || !Misc.equals(originalText, newText)) {
            String desc = "";
            if (wasNew) {
                desc = "Created";
            } else {
                desc = request.getString(ARG_WIKI_CHANGEDESCRIPTION, "");
            }

            getDatabaseManager().executeInsert(Tables.WIKIPAGEHISTORY.INSERT,
                    new Object[] { entry.getId(),
                                   request.getUser().getId(), new Date(),
                                   desc, newText });
            WikiUtil wikiUtil = new WikiUtil(Misc.newHashtable(new Object[] {
                                    OutputHandler.PROP_REQUEST,
                                    request, OutputHandler.PROP_ENTRY,
                                    entry }));
            getRepository().getHtmlOutputHandler().wikifyEntry(request,
                    entry, wikiUtil, newText, null, null);

            List categories = (List) wikiUtil.getProperty("wikicategories");
            if (categories == null) {
                categories = new ArrayList();
            }
            //TODO: 
            List<Metadata> metadataList =
                getMetadataManager().getMetadata(entry);
            for (Metadata metadata : (List<Metadata>) new ArrayList(
                    metadataList)) {
                if (metadata.getType().equals("wikicategory")) {
                    if ( !categories.contains(metadata.getAttr1())) {
                        metadataList.remove(metadata);
                        //getMetadataManager().deleteMetadata(metadata);
                    } else {
                        categories.remove(metadata.getAttr1());
                    }
                }
            }
            for (String cat : (List<String>) categories) {
                Metadata metadata = new Metadata(getRepository().getGUID(),
                                        entry.getId(), "wikicategory", false,
                                        cat, "", "", "");
                //                getMetadataManager().insertMetadata(metadata);
                metadataList.add(metadata);
            }
            entry.setMetadata(metadataList);
            Hashtable<Entry, Entry> links =
                (Hashtable<Entry, Entry>) wikiUtil.getProperty("wikilinks");
            if (links == null) {
                links = new Hashtable<Entry, Entry>();
            }
            Hashtable         ids             = new Hashtable();
            List<Association> newAssociations = new ArrayList<Association>();
            for (Enumeration keys = links.keys(); keys.hasMoreElements(); ) {
                Entry linkedEntry = (Entry) keys.nextElement();
                Association tmp = new Association(getRepository().getGUID(),
                                      "", ASSOC_WIKILINK, entry.getId(),
                                      linkedEntry.getId());
                newAssociations.add(tmp);
            }


            List<Association> associations =
                getEntryManager().getAssociations(request, entry);
            for (Association oldAssociation : (List<Association>) new ArrayList(
                    associations)) {
                if (oldAssociation.getType().equals(ASSOC_WIKILINK)
                        && oldAssociation.getFromId().equals(entry.getId())) {
                    if ( !newAssociations.contains(oldAssociation)) {
                        System.err.println("delete:" + oldAssociation);
                        getEntryManager().deleteAssociation(request,
                                oldAssociation);
                    }
                }
            }
            for (Association newAssociation : (List<Association>) new ArrayList(
                    newAssociations)) {
                if ( !associations.contains(newAssociation)) {
                    getEntryManager().addAssociation(request, newAssociation);
                }
            }

        }



    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, StringBuffer sb, Entry entry)
            throws Exception {

        String size = HtmlUtil.SIZE_70;
        String name;
        if (entry != null) {
            name = entry.getName();
        } else {
            name = request.getString(ARG_NAME, "");
            List tmp = new ArrayList();
            for (String tok : (List<String>) StringUtil.split(name, " ",
                    true, true)) {
                tmp.add(StringUtil.camelCase(tok));
            }
            name = StringUtil.join(" ", tmp);
        }

        String wikiText = "";
        if (entry != null) {
            Object[] values = entry.getValues();
            if ((values != null) && (values.length > 0)
                    && (values[0] != null)) {
                wikiText = (String) values[0];
            }
        }
        if (request.defined(ARG_WIKI_EDITWITH)) {
            Date dttm = new Date((long) request.get(ARG_WIKI_EDITWITH, 0.0));
            WikiPageHistory wph = getHistory(entry, dttm);
            if (wph == null) {
                throw new IllegalArgumentException(
                    "Could not find wiki history");
            }
            wikiText = wph.getText();
            sb.append(
                HtmlUtil.formEntry(
                    "",
                    msgLabel("Editing with text from version")
                    + getRepository().formatDate(wph.getDate())));
        }

        sb.append(HtmlUtil.formEntry(msgLabel("Title"),
                                     HtmlUtil.input(ARG_NAME, name, size)));

        if (entry != null) {
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel("Edit&nbsp;Summary"),
                    HtmlUtil.input(ARG_WIKI_CHANGEDESCRIPTION, "", size)));
        }





        StringBuffer help = new StringBuffer();
        help.append("<b>Import:</b><br>");
        help.append(
            "e.g., <i>{{property &lt;optional arguments&gt;}}</i><br>");
        help.append(
            "Or: <i>{{import entryid property &lt;arguments&gt;}}</i><br>");
        help.append("<i>{{&lt;output identifier&gt;}}</i><br>");




        String buttons =
            getRepository().getHtmlOutputHandler().makeWikiEditBar(request,
                entry, ARG_WIKI_TEXT);
        String textWidget = buttons + HtmlUtil.br()
                            + HtmlUtil.textArea(ARG_WIKI_TEXT, wikiText, 250,
                                60, HtmlUtil.id(ARG_WIKI_TEXT));

        String right = HtmlUtil.div(help.toString(),
                                    HtmlUtil.cssClass("smallhelp"));
        right = "";
        textWidget = "<table><tr valign=\"top\"><td>" + textWidget
                     + "</td><td>" + right + "</td></tr></table>";
        sb.append(HtmlUtil.formEntryTop(msgLabel("Wiki Text"), textWidget));

    }





    /**
     * _more_
     *
     * @param entry _more_
     * @param date _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public WikiPageHistory getHistory(Entry entry, Date date)
            throws Exception {
        List<WikiPageHistory> list = getHistoryList(entry, date, true);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param date _more_
     * @param includeText _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<WikiPageHistory> getHistoryList(Entry entry, Date date,
            boolean includeText)
            throws Exception {
        Statement stmt = getDatabaseManager().select(SqlUtil.comma(includeText
                ? new String[] { Tables.WIKIPAGEHISTORY.COL_USER_ID,
                                 Tables.WIKIPAGEHISTORY.COL_DATE,
                                 Tables.WIKIPAGEHISTORY.COL_DESCRIPTION,
                                 Tables.WIKIPAGEHISTORY.COL_WIKITEXT }
                : new String[] { Tables.WIKIPAGEHISTORY.COL_USER_ID,
                                 Tables.WIKIPAGEHISTORY.COL_DATE,
                                 Tables.WIKIPAGEHISTORY
                                     .COL_DESCRIPTION }), Tables
                                         .WIKIPAGEHISTORY
                                         .NAME, ((date != null)
                ? Clause
                    .and(Clause
                        .eq(Tables.WIKIPAGEHISTORY.COL_ENTRY_ID,
                            entry.getId()), Clause
                                .eq(Tables.WIKIPAGEHISTORY.COL_DATE, date))
                : Clause.eq(
                    Tables.WIKIPAGEHISTORY.COL_ENTRY_ID,
                    entry.getId())), " order by "
                                     + Tables.WIKIPAGEHISTORY.COL_DATE
                                     + " asc ");

        SqlUtil.Iterator      iter = SqlUtil.getIterator(stmt);
        ResultSet             results;
        List<WikiPageHistory> history = new ArrayList<WikiPageHistory>();
        int                   version = 1;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                WikiPageHistory wph =
                    new WikiPageHistory(
                        version++,
                        getUserManager().findUser(
                            results.getString(col++),
                            true), getDatabaseManager().getDate(
                                results, col++), results.getString(col++),
                                    (includeText
                                     ? results.getString(col++)
                                     : ""));
                history.add(wph);
            }
        }

        stmt.close();

        return history;
    }



}


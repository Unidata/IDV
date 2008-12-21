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


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import ucar.unidata.sql.Clause;
import ucar.unidata.sql.SqlUtil;
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

    /** _more_ */
    public static String TYPE_WIKIPAGE = "wikipage";

    /** _more_          */
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
        return getRepository().getOutputHandler(
            WikiOutputHandler.OUTPUT_WIKI).outputEntry(request, entry);
    }


    public void deleteEntry(Request request, Statement statement, Entry entry)
            throws Exception {
        super.deleteEntry(request, statement, entry);
        String query = SqlUtil.makeDelete(Tables.WIKIHISTORY.NAME, COL_ID,
                                          SqlUtil.quote(entry.getId()));
        System.err.println("delete:" + query);
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
        String originalText = null;
        Object[] values = entry.getValues();
        if(values!=null) {
            originalText = (String) values[0];
        }
        super.initializeEntry(request, entry);
        String newText = (String)  entry.getValues()[0];

        if(originalText==null || !Misc.equals(originalText, newText)) {
            String desc="";
            if(originalText==null) {
                desc = "Created";
                originalText = "";
            } else {
                desc = request.getString(ARG_WIKI_CHANGEDESCRIPTION,"");
            }

            getDatabaseManager().executeInsert(Tables.WIKIHISTORY.INSERT,
                    new Object[] {
                        entry.getId(), 
                        request.getUser().getId(), new Date(), desc,
                        originalText
                    });
        }
    }


    public static class WikiHistory {
        int version;
        User user;
        Date date;
        String description;
        String text;
        public WikiHistory(User user, Date date, String description) {
            this.user = user;
            this.date = date;
            this.description = description;
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
        if(request.defined(ARG_WIKI_EDITWITH)) {
            Date dttm= new Date((long)request.get(ARG_WIKI_EDITWITH,0.0));
            WikiPageHistory wph = getHistory(entry, dttm);
            if(wph==null) {
                throw new IllegalArgumentException("Could not find wiki history");
            }
            wikiText = wph.getText();
            sb.append(HtmlUtil.formEntry("",msgLabel("Editing with text from version") + getRepository().formatDate(wph.getDate())));
        }

        sb.append(HtmlUtil.formEntry(msgLabel("Title"),
                                     HtmlUtil.input(ARG_NAME, name, size)));

        if(entry!=null) {
            sb.append(HtmlUtil.formEntry(msgLabel("Edit&nbsp;Summary"),
                                         HtmlUtil.input(ARG_WIKI_CHANGEDESCRIPTION,"",size)));
        }





        StringBuffer help = new StringBuffer();
        help.append("<b>Import:</b><br>");
        help.append(
            "e.g., <i>{{property &lt;optional arguments&gt;}}</i><br>");
        help.append(
            "Or: <i>{{import entryid property &lt;arguments&gt;}}</i><br>");
        help.append("<i>{{&lt;output identifier&gt;}}</i><br>");


        String select = OutputHandler.getSelect(request, ARG_WIKI_TEXT,
                            "Add link", true, "wikilink") + HtmlUtil.space(1)
                                + OutputHandler.getSelect(request,
                                    ARG_WIKI_TEXT, "Add import entry", true,
                                    "entryid");

        StringBuffer buttons = new StringBuffer();
        buttons.append(addButton("button_bold.png", "Bold text", "\\'\\'\\'",
                                 "\\'\\'\\'", "Bold text",
                                 "mw-editbutton-bold"));
        buttons.append(addButton("button_italic.png", "Italic text",
                                 "\\'\\'", "\\'\\'", "Italic text",
                                 "mw-editbutton-italic"));
        buttons.append(addButton("button_link.png", "Internal link", "[[",
                                 "]]", "Link title", "mw-editbutton-link"));
        buttons.append(addButton("button_extlink.png",
                                 "External link (remember http:// prefix)",
                                 "[", "]",
                                 "http://www.example.com link title",
                                 "mw-editbutton-extlink"));
        buttons.append(addButton("button_headline.png", "Level 2 headline",
                                 "\\n== ", " ==\\n", "Headline text",
                                 "mw-editbutton-headline"));
        buttons.append(addButton("button_linebreak.png", "Line break",
                                 "<br>", "", "", "mw-editbutton-headline"));
        buttons.append(addButton("button_strike.png", "Strike Through",
                                 "<s>", "</s>", "Strike-through text",
                                 "mw-editbutton-headline"));
        buttons.append(addButton("button_upper_letter.png", "Super Script",
                                 "<sup>", "</sup>", "Super script text",
                                 "mw-editbutton-headline"));
        buttons.append(addButton("button_lower_letter.png", "Sub Script",
                                 "<sub>", "</sub>", "Subscript script text",
                                 "mw-editbutton-headline"));
        buttons.append(addButton("button_small.png", "Small text", "<small>",
                                 "</small>", "Small text",
                                 "mw-editbutton-headline"));
        buttons.append(addButton("button_blockquote.png",
                                 "Insert block quote", "<blockquote>",
                                 "</blockquote>", "Quoted text",
                                 "mw-editbutton-headline"));
        //        buttons.append(addButton("button_image.png","Embedded file","[[File:","]]","Example.jpg","mw-editbutton-image"));
        //        buttons.append(addButton("button_media.png","File link","[[Media:","]]","Example.ogg","mw-editbutton-media"));
        //        buttons.append(addButton("button_nowiki.png","Ignore wiki formatting","\\x3cnowiki\\x3e","\\x3c/nowiki\\x3e","Insert non-formatted text here","mw-editbutton-nowiki"));
        //        buttons.append(addButton("button_sig.png","Your signature with timestamp","--~~~~","","","mw-editbutton-signature"));
        buttons.append(addButton("button_hr.png",
                                 "Horizontal line (use sparingly)",
                                 "\\n----\\n", "", "", "mw-editbutton-hr"));

        StringBuffer propertyMenu = new StringBuffer();
        StringBuffer importMenu   = new StringBuffer();
        for (int i = 0; i < OutputHandler.WIKIPROPS.length; i++) {
            String prop = OutputHandler.WIKIPROPS[i];
            String js = "javascript:insertTags("
                        + HtmlUtil.squote(ARG_WIKI_TEXT) + ","
                        + HtmlUtil.squote("{{") + "," + HtmlUtil.squote("}}")
                        + "," + HtmlUtil.squote(prop) + ");";
            propertyMenu.append(HtmlUtil.href(js, prop));
            propertyMenu.append(HtmlUtil.br());

            String js2 = "javascript:insertTags("
                         + HtmlUtil.squote(ARG_WIKI_TEXT) + ","
                         + HtmlUtil.squote("{{import ") + ","
                         + HtmlUtil.squote(" " + prop + "}}") + ","
                         + HtmlUtil.squote(" entryid ") + ");";
            importMenu.append(HtmlUtil.href(js2, prop));
            importMenu.append(HtmlUtil.br());
        }

        List<Link> links = getRepository().getOutputLinks(request,
                               new OutputHandler.State(entry));


        propertyMenu.append("<hr>");
        for (Link link : links) {
            if (link.getOutputType() == null) {
                continue;
            }

            String prop = link.getOutputType().getId();
            String js = "javascript:insertTags("
                        + HtmlUtil.squote(ARG_WIKI_TEXT) + ","
                        + HtmlUtil.squote("{{") + "," + HtmlUtil.squote("}}")
                        + "," + HtmlUtil.squote(prop) + ");";
            propertyMenu.append(HtmlUtil.href(js, link.getLabel()));
            propertyMenu.append(HtmlUtil.br());
        }



        StringBuffer importOutputMenu = new StringBuffer();
        /*
                List<OutputType> allTypes = getRepository().getOutputTypes();
                //        importMenu.append("<hr>");
                for(OutputType type: allTypes) {
                    String prop = type.getId();
                    String js = "javascript:insertTags(" + HtmlUtil.squote(ARG_WIKI_TEXT)+"," +
                        HtmlUtil.squote("{{import ") +","+
                        HtmlUtil.squote(" " + type.getId()+" }}") +","+
                        HtmlUtil.squote("entryid")+");";
                    importOutputMenu.append(HtmlUtil.href(js,type.getLabel()));
                    importOutputMenu.append(HtmlUtil.br());
                }
        */


        String propertyMenuLabel =
            HtmlUtil.img(fileUrl("/icons/wiki/button_property.png"),
                         "Add Entry Property");
        String propertyButton =
            getRepository().makeMenuPopupLink(propertyMenuLabel,
                propertyMenu.toString());
        buttons.append(propertyButton);
        String importMenuLabel =
            HtmlUtil.img(fileUrl("/icons/wiki/button_import.png"),
                         "Import Entry Property");
        String importButton =
            getRepository().makeMenuPopupLink(importMenuLabel,
                HtmlUtil.hbox(importMenu.toString(),
                              importOutputMenu.toString()));
        buttons.append(importButton);
        buttons.append(HtmlUtil.space(2));
        buttons.append(select);


        String textWidget = buttons + HtmlUtil.br()
                            + HtmlUtil.textArea(ARG_WIKI_TEXT, wikiText, 250,
                                                40, HtmlUtil.id(ARG_WIKI_TEXT));

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
     * @param icon _more_
     * @param label _more_
     * @param prefix _more_
     * @param suffix _more_
     * @param example _more_
     * @param huh _more_
     *
     * @return _more_
     */
    private String addButton(String icon, String label, String prefix,
                             String suffix, String example, String huh) {
        String prop = prefix + example + suffix;
        String js;
        if (suffix.length() == 0) {
            js = "javascript:insertText(" + HtmlUtil.squote(ARG_WIKI_TEXT)
                 + "," + HtmlUtil.squote(prop) + ");";
        } else {
            js = "javascript:insertTags(" + HtmlUtil.squote(ARG_WIKI_TEXT)
                 + "," + HtmlUtil.squote(prefix) + ","
                 + HtmlUtil.squote(suffix) + "," + HtmlUtil.squote(example)
                 + ");";
        }
        return HtmlUtil.href(js,
                             HtmlUtil.img(fileUrl("/icons/wiki/" + icon),
                                          label));

    }


    public WikiPageHistory getHistory(Entry entry, Date date) throws Exception {
        List<WikiPageHistory>         list = getHistoryList(entry,date,true);
        if(list.size()>0) return list.get(0);
        return null;
    }

    public List<WikiPageHistory> getHistoryList(Entry entry, Date date, boolean includeText) throws Exception {
        Statement stmt = getDatabaseManager().select(SqlUtil.comma(includeText?
                                                      new String[]{
                                                          Tables.WIKIHISTORY.COL_USER_ID,
                                                          Tables.WIKIHISTORY.COL_DATE,
                                                          Tables.WIKIHISTORY.COL_DESCRIPTION,
                                                          Tables.WIKIHISTORY.COL_WIKITEXT}:
                                                      new String[]{
                                                          Tables.WIKIHISTORY.COL_USER_ID,
                                                          Tables.WIKIHISTORY.COL_DATE,
                                                          Tables.WIKIHISTORY.COL_DESCRIPTION}),
                                                     Tables.WIKIHISTORY.NAME,
                                                      (date!=null?
                                                       Clause.and(
                                                                  Clause.eq(Tables.WIKIHISTORY.COL_ENTRY_ID, entry.getId()),
                                                                  Clause.eq(Tables.WIKIHISTORY.COL_DATE, date)):
                                                       Clause.eq(Tables.WIKIHISTORY.COL_ENTRY_ID, entry.getId())),
                                                     " order by " + Tables.WIKIHISTORY.COL_DATE +" asc ");

        SqlUtil.Iterator  iter         = SqlUtil.getIterator(stmt);
        ResultSet         results;
        List<WikiPageHistory> history = new ArrayList<WikiPageHistory>();
        int version=1;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                WikiPageHistory wph = new WikiPageHistory(version++,
                                                          getUserManager().findUser(results.getString(col++),true),
                                                          getDatabaseManager().getDate(results, col++),
                                                          results.getString(col++),
                                                          (includeText?results.getString(col++):""));
                history.add(wph);
            }
        }

        stmt.close();

        return history;
    }



}


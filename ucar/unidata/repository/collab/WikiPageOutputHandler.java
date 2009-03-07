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

package ucar.unidata.repository.collab;


import org.incava.util.diff.*;

import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.util.HtmlUtil;




import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WikiUtil;

import ucar.unidata.xml.XmlUtil;


import java.sql.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;




import java.util.Iterator;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class WikiPageOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_WIKI = new OutputType("Wiki",
                                                     "wiki.view",
                                                     OutputType.TYPE_HTML,
                                                     "", ICON_WIKI);

    /** _more_ */
    public static final OutputType OUTPUT_WIKI_HISTORY =
        new OutputType("Wiki History", "wiki.history", OutputType.TYPE_HTML,
                       "", ICON_WIKI);


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public WikiPageOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_WIKI);
        addType(OUTPUT_WIKI_HISTORY);
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
    protected void getEntryLinks(Request request, State state,
                                 List<Link> links)
            throws Exception {

        if (state.entry == null) {
            return;
        }
        if (state.entry.getType().equals(WikiPageTypeHandler.TYPE_WIKIPAGE)) {
            links.add(makeLink(request, state.entry, OUTPUT_WIKI));
            links.add(makeLink(request, state.entry, OUTPUT_WIKI_HISTORY));
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

        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_WIKI_HISTORY)) {
            return outputWikiHistory(request, entry);
        }





        String wikiText = "";
        String header   = "";
        if (request.defined(ARG_WIKI_VERSION)) {
            Date dttm = new Date((long) request.get(ARG_WIKI_VERSION, 0.0));
            WikiPageHistory wph =
                ((WikiPageTypeHandler) entry.getTypeHandler()).getHistory(
                    entry, dttm);
            if (wph == null) {
                throw new IllegalArgumentException(
                    "Could not find wiki history");
            }
            wikiText = wph.getText();
            header = getRepository().note(
                msgLabel("Text from version")
                + getRepository().formatDate(wph.getDate()));
        } else {
            Object[] values = entry.getValues();
            if ((values != null) && (values.length > 0)
                    && (values[0] != null)) {
                wikiText = (String) values[0];
            }
        }

        if (request.get(ARG_WIKI_RAW, false)) {
            StringBuffer sb = new StringBuffer();
            sb.append(HtmlUtil.form(""));
            sb.append(HtmlUtil.textArea(ARG_WIKI_TEXT, wikiText, 250, 60,
                                        HtmlUtil.id(ARG_WIKI_TEXT)));
            sb.append(HtmlUtil.formClose());
            return makeLinksResult(request, msg("Wiki"), sb,
                                   new State(entry));
        }



        String detailsView =
            HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                           entry, ARG_WIKI_DETAILS,
                                           "" + true), msg("Details"));

        String rawLink =
            HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                           entry, ARG_WIKI_RAW,
                                           "" + true), msg("Text"));

        header = HtmlUtil.leftRight(header,
                                    HtmlUtil.div(detailsView + " " + rawLink,
                                        HtmlUtil.cssClass("smalllink")));
        WikiUtil wikiUtil = new WikiUtil(Misc.newHashtable(new Object[] {
                                OutputHandler.PROP_REQUEST,
                                request, OutputHandler.PROP_ENTRY, entry }));
        StringBuffer sb = new StringBuffer();
        sb.append(header);
        sb.append(wikifyEntry(request, entry, wikiUtil, wikiText, null,
                              null));
        Hashtable links = (Hashtable) wikiUtil.getProperty("wikilinks");
        if (links != null) {
            List<Association> associations =
                getAssociationManager().getAssociations(request, entry);
        }

        return makeLinksResult(request, msg("Wiki"), sb, new State(entry));
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
    public Result outputWikiCompare(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();

        Date dttm1 = new Date((long) request.get(ARG_WIKI_COMPARE1, 0.0));
        WikiPageHistory wph1 =
            ((WikiPageTypeHandler) entry.getTypeHandler()).getHistory(entry,
                dttm1);
        if (wph1 == null) {
            throw new IllegalArgumentException("Could not find wiki history");
        }

        Date dttm2 = new Date((long) request.get(ARG_WIKI_COMPARE2, 0.0));
        WikiPageHistory wph2 =
            ((WikiPageTypeHandler) entry.getTypeHandler()).getHistory(entry,
                dttm2);
        if (wph2 == null) {
            throw new IllegalArgumentException("Could not find wiki history");
        }

        String lbl1 = "Revision as of "
                      + getRepository().formatDate(wph1.getDate())
                      + HtmlUtil.br() + wph1.getUser() + HtmlUtil.br()
                      + wph1.getDescription();
        String lbl2 = "Revision as of "
                      + getRepository().formatDate(wph2.getDate())
                      + HtmlUtil.br() + wph2.getUser() + HtmlUtil.br()
                      + wph2.getDescription();
        sb.append("<table width=100% border=0 cellspacing=5 cellpadding=4>");
        sb.append(HtmlUtil.row(HtmlUtil.cols("", lbl1, "", lbl2)));

        getDiff(wph1.getText(), wph2.getText(), sb);
        sb.append("</table>");
        return makeLinksResult(request, msg("Wiki Comparison"), sb,
                               new State(entry));
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
    public Result outputWikiHistory(Request request, Entry entry)
            throws Exception {
        StringBuffer sb      = new StringBuffer();
        boolean      canEdit = getAccessManager().canEditEntry(request,
                                   entry);

        if (request.exists(ARG_WIKI_COMPARE1)
                && request.exists(ARG_WIKI_COMPARE2)) {
            return outputWikiCompare(request, entry);
        }




        sb.append(request.form(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_WIKI_HISTORY));

        List<WikiPageHistory> history =
            ((WikiPageTypeHandler) entry.getTypeHandler()).getHistoryList(
                entry, null, false);
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE,
                                " cellspacing=5 cellpadding=5 "));
        sb.append(HtmlUtil.row(HtmlUtil.cols(new Object[] {
            HtmlUtil.b(msg("Version")), "", "", "", HtmlUtil.b(msg("User")),
            HtmlUtil.b(msg("Date")), HtmlUtil.b(msg("Description"))
        })));
        int version = 1;
        for (int i = history.size() - 1; i >= 0; i--) {
            WikiPageHistory wph  = history.get(i);
            String          edit = "";
            if (canEdit) {
                edit = HtmlUtil
                    .href(request
                        .entryUrl(
                            getRepository().URL_ENTRY_FORM, entry,
                            ARG_WIKI_EDITWITH,
                            wph.getDate().getTime() + ""), HtmlUtil
                                .img(getRepository().iconUrl(ICON_EDIT),
                                     msg("Edit with this version")));
            }
            String view = HtmlUtil.href(
                              request.entryUrl(
                                  getRepository().URL_ENTRY_SHOW, entry,
                                  ARG_WIKI_VERSION,
                                  wph.getDate().getTime() + ""), HtmlUtil.img(
                                      getRepository().iconUrl(ICON_WIKI),
                                      msg("View this page")));
            String btns =
                HtmlUtil.radio(ARG_WIKI_COMPARE1,
                               "" + wph.getDate().getTime(),
                               false) + HtmlUtil.radio(ARG_WIKI_COMPARE2,
                                   "" + wph.getDate().getTime(), false);
            String versionLabel;
            if (i == history.size() - 1) {
                versionLabel = msg("Current");
            } else {
                versionLabel = "" + wph.getVersion();
            }
            sb.append(HtmlUtil.row(HtmlUtil.cols(new Object[] {
                versionLabel, btns, edit, view, wph.getUser().getLabel(),
                getRepository().formatDate(wph.getDate()),
                wph.getDescription()
            })));
        }

        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));

        sb.append(HtmlUtil.submit("Compare Selected Versions"));
        sb.append(HtmlUtil.formClose());

        return makeLinksResult(request, msg("Wiki History"), sb,
                               new State(entry));
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
        return outputEntry(request, group);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String       s1 = IOUtil.readContents(new java.io.File(args[0]));
        String       s2 = IOUtil.readContents(new java.io.File(args[1]));
        StringBuffer sb = new StringBuffer();

        sb.append("<html>");
        sb.append(
            "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\">");
        sb.append("<table width=100% border=0 cellspacing=5 xcellpadding=4>");
        //        sb.append(HtmlUtil.row( HtmlUtil.cols(lbl1,lbl2)));

        getDiff(s1, s2, sb);
        sb.append("</table>");
        System.out.println(sb);


    }


    /**
     * _more_
     *
     * @param text1 _more_
     * @param text2 _more_
     * @param sb _more_
     */
    public static void getDiff(String text1, String text2, StringBuffer sb) {
        String[] aLines = Misc.listToStringArray(StringUtil.split(text1,
                              "\n", false, false));
        String[] bLines = Misc.listToStringArray(StringUtil.split(text2,
                              "\n", false, false));
        List     diffs    = (new Diff(aLines, bLines)).diff();

        Iterator it       = diffs.iterator();
        int      leftIdx  = 0;
        int      rightIdx = 0;
        int      context;
        while (it.hasNext()) {
            Difference diff     = (Difference) it.next();
            int        delStart = diff.getDeletedStart();
            int        delEnd   = diff.getDeletedEnd();
            int        addStart = diff.getAddedStart();
            int        addEnd   = diff.getAddedEnd();
            String     from     = toString(delStart, delEnd);
            String     to       = toString(addStart, addEnd);
            String type = ((delEnd != Difference.NONE)
                           && (addEnd != Difference.NONE))
                          ? "c"
                          : ((delEnd == Difference.NONE)
                             ? "a"
                             : "d");


            if (delEnd != Difference.NONE) {
                context = Math.max(leftIdx, delStart - 4);
                if (context < delStart) {
                    sb.append(
                        "<tr valign=top><td></td><td width=50% class=wikicompare-context>");
                    appendLines(context, delStart - 1, true, aLines, sb);
                }
                if (addEnd != Difference.NONE) {
                    context = Math.max(rightIdx, addStart - 4);
                    if (context < addStart) {
                        sb.append(
                            "<td></td><td width=50% class=wikicompare-context>");
                        appendLines(context, addStart - 1, true, bLines, sb);
                        sb.append("</td>");
                    }
                }

                sb.append(
                    "<tr valign=top><td colspan=2 class=\"wikicompare-lineheader\">Line:"
                    + delStart
                    + "</td><td colspan=2 class=\"wikicompare-lineheader\">");
                if (addEnd != Difference.NONE) {
                    sb.append("Line:" + addStart);
                }
                sb.append("</td></tr>");

                sb.append("<tr valign=top>");
                sb.append("<td valign=center>-</td>");
                sb.append("<td width=50% class=wikicompare-deleted>");
                appendLines(delStart, delEnd, false, aLines, sb);
                leftIdx = delEnd + 1;
                sb.append("</td>");
                if (addEnd != Difference.NONE) {
                    sb.append(
                        "<td>+</td><td width=50% class=wikicompare-added>");
                    appendLines(addStart, addEnd, false, bLines, sb);
                    rightIdx = addEnd + 1;
                    sb.append("</td></tr>");
                    continue;
                }
                sb.append("<td></td><td width=50%>&nbsp;</td></tr>");
            }
            if (addEnd != Difference.NONE) {

                context = leftIdx + 4;
                sb.append(
                    "<tr valign=top><td></td><td width=50% class=wikicompare-context>");
                appendLines(leftIdx, leftIdx + 4, true, aLines, sb);
                context = Math.max(rightIdx, addStart - 4);
                if (context < addStart) {
                    sb.append(
                        "<td></td><td width=50% class=wikicompare-context>");
                    appendLines(context, addStart - 1, true, bLines, sb);
                    sb.append("</td>");
                }


                sb.append(
                    "<tr><td colspan=2></td><td colspan=2 class=\"wikicompare-lineheader\">");
                sb.append("Line:" + addStart);
                sb.append("</td></tr>");

                sb.append(
                    "<tr valign=top><td></td><td width=50%></td><td>+</td><td width=50% class=wikicompare-added>");
                appendLines(addStart, addEnd, false, bLines, sb);
                rightIdx = addEnd + 1;
                sb.append("</td></tr>");
            }

        }
    }

    /**
     * _more_
     *
     * @param start _more_
     * @param end _more_
     * @param includeLineNumber _more_
     * @param lines _more_
     * @param sb _more_
     */
    protected static void appendLines(int start, int end,
                                      boolean includeLineNumber,
                                      String[] lines, StringBuffer sb) {
        includeLineNumber = false;
        for (int lnum = start; lnum <= end; ++lnum) {
            if (lnum < 0) {
                continue;
            }
            if (lnum >= lines.length) {
                break;
            }
            String line = lines[lnum];
            line = HtmlUtil.entityEncode(line);
            if (includeLineNumber) {
                sb.append("<b>" + lnum + ": </b>");
            }
            sb.append(line + "<br>");

        }
    }

    /**
     * _more_
     *
     * @param start _more_
     * @param end _more_
     *
     * @return _more_
     */
    protected static String toString(int start, int end) {
        // adjusted, because file lines are one-indexed, not zero.

        StringBuffer buf = new StringBuffer();

        // match the line numbering from diff(1):
        buf.append((end == Difference.NONE)
                   ? start
                   : (1 + start));

        if ((end != Difference.NONE) && (start != end)) {
            buf.append(",").append(1 + end);
        }
        return buf.toString();
    }



}


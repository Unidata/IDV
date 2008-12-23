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
import ucar.unidata.repository.*;

import org.incava.util.diff.*;

import org.w3c.dom.*;



import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WikiUtil;

import ucar.unidata.xml.XmlUtil;




import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import java.sql.*;
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

    public static final OutputType OUTPUT_WIKI_HISTORY = new OutputType("Wiki History",
                                                                        "wiki.history",
                                                                        OutputType.TYPE_HTML,
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
     * @param entries _more_
     * @param state _more_
     * @param types _more_
     * @param links _more_
     * @param forHeader _more_
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

        OutputType   output = request.getOutput();
        if(output.equals(OUTPUT_WIKI_HISTORY)) {
            return outputWikiHistory(request, entry);
        }

        String   wikiText = "";
        String header = "";
        if(request.defined(ARG_WIKI_VERSION)) {
            Date dttm= new Date((long)request.get(ARG_WIKI_VERSION,0.0));
            WikiPageHistory wph = ((WikiPageTypeHandler)entry.getTypeHandler()).getHistory(entry, dttm);
            if(wph==null) {
                throw new IllegalArgumentException("Could not find wiki history");
            }
            wikiText = wph.getText();
            header = getRepository().note(msgLabel("Text from version") +getRepository().formatDate(wph.getDate())); 
        } else {
            Object[] values   = entry.getValues();
            if ((values != null) && (values.length > 0) && (values[0] != null)) {
                wikiText = (String) values[0];
            }
        }
        StringBuffer sb = new StringBuffer(header+wikifyEntry(request, entry,
                              wikiText));
        return makeLinksResult(request, msg("Wiki"), sb, new State(entry));
    }




    public Result outputWikiCompare(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();

        Date dttm1= new Date((long)request.get(ARG_WIKI_COMPARE1,0.0));
        WikiPageHistory wph1 = ((WikiPageTypeHandler)entry.getTypeHandler()).getHistory(entry, dttm1);
        if(wph1==null) {
            throw new IllegalArgumentException("Could not find wiki history");
        }

        Date dttm2= new Date((long)request.get(ARG_WIKI_COMPARE2,0.0));
        WikiPageHistory wph2 = ((WikiPageTypeHandler)entry.getTypeHandler()).getHistory(entry, dttm2);
        if(wph2==null) {
            throw new IllegalArgumentException("Could not find wiki history");
        }

        String lbl1 = "Revision as of " +getRepository().formatDate(wph1.getDate())+HtmlUtil.br() +
            wph1.getUser();
        String lbl2 = "Revision as of " +getRepository().formatDate(wph2.getDate())+HtmlUtil.br() +
            wph2.getUser();
        sb.append("<table width=100% border=0 cellspacing=5 cellpadding=4>");
        sb.append(HtmlUtil.row( HtmlUtil.cols(lbl1,lbl2)));

        getDiff(wph1.getText(), wph2.getText(),sb);
        sb.append("</table>");
        return makeLinksResult(request, msg("Wiki Comparison"),
                               sb, new State(entry));
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
        StringBuffer sb = new StringBuffer();
        boolean canEdit = getAccessManager().canEditEntry(request, entry);

        if(request.exists(ARG_WIKI_COMPARE1) && request.exists(ARG_WIKI_COMPARE2)) {
            return outputWikiCompare(request, entry);
        }



        
        sb.append(request.form(getRepository().URL_ENTRY_SHOW));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_WIKI_HISTORY));

        List<WikiPageHistory> history = ((WikiPageTypeHandler)entry.getTypeHandler()).getHistoryList(entry,null,false);
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE));
        sb.append(HtmlUtil.row(HtmlUtil.cols(new Object[]{
                                             HtmlUtil.b(msg("Version")),
                                             "",
                                             "","",
                                             HtmlUtil.b(msg("User")),
                                             HtmlUtil.b(msg("Date")),
                                             HtmlUtil.b(msg("Description"))})));
        int version = 1;
        for(int i = history.size()-1;i>=0;i--) {
            WikiPageHistory wph = history.get(i);
            String edit = "";
            if(canEdit) {
                edit = HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_FORM, entry,ARG_WIKI_EDITWITH,wph.getDate().getTime()+""),
                                     HtmlUtil.img(getRepository().fileUrl(ICON_EDIT), msg("Edit with this version")));
            }
            String view = HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,ARG_WIKI_VERSION,wph.getDate().getTime()+""),
                                        HtmlUtil.img(getRepository().fileUrl(ICON_WIKI), msg("View this page")));
            String btns = HtmlUtil.radio(ARG_WIKI_COMPARE1,""+wph.getDate().getTime(),false) +
                 HtmlUtil.radio(ARG_WIKI_COMPARE2,""+wph.getDate().getTime(),false);
            String versionLabel;
            if(i==history.size()-1) {
                versionLabel = msg("Current");
            } else {
                versionLabel = ""+wph.getVersion();
            }
            sb.append(HtmlUtil.row(HtmlUtil.cols(new Object[]{versionLabel,btns,edit,view,
                                                 wph.getUser().getLabel(),
                                                 getRepository().formatDate(wph.getDate()),
                                                 wph.getDescription()})));
        }

        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));

        sb.append(HtmlUtil.submit("Compare Selected Versions"));
        sb.append(HtmlUtil.formClose());

        return makeLinksResult(request, msg("Wiki History"),
                               sb, new State(entry));
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


    public void getDiff(String text1, String text2,StringBuffer sb) {
        String[] aLines = Misc.listToStringArray(StringUtil.split(text1,"\n",false,false));
        String[] bLines = Misc.listToStringArray(StringUtil.split(text2,"\n",false,false));
        List     diffs  = (new Diff(aLines, bLines)).diff();
        
        Iterator it     = diffs.iterator();
        while (it.hasNext()) {
            Difference diff     = (Difference)it.next();
            int        delStart = diff.getDeletedStart();
            int        delEnd   = diff.getDeletedEnd();
            int        addStart = diff.getAddedStart();
            int        addEnd   = diff.getAddedEnd();
            String     from     = toString(delStart, delEnd);
            String     to       = toString(addStart, addEnd);
            String     type     = delEnd != Difference.NONE && addEnd != Difference.NONE ? "c" : (delEnd == Difference.NONE ? "a" : "d");


            if (delEnd != Difference.NONE) {
                sb.append("<tr valign=top><td class=wikicompare-changed><pre>");
                appendLines(delStart, delEnd, "", aLines,sb);
                sb.append("</pre></td>");
                if (addEnd != Difference.NONE) {
                    sb.append("<td class=wikicompare-changed><pre>");
                    appendLines(addStart, addEnd, "", bLines,sb);
                    sb.append("</pre>");
                    sb.append("</td></tr>");
                    continue;
                }
                sb.append("<td></td></tr>");
            }
            if (addEnd != Difference.NONE) {
                sb.append("<tr valign=top><td>&nbsp;</td><td class=wikicompare-changed><pre>");
                appendLines(addStart, addEnd, "", bLines,sb);
                sb.append("</pre></td></tr>");
            }

        }
    }

    protected void appendLines(int start, int end, String ind, String[] lines,StringBuffer sb)
    {
        for (int lnum = start; lnum <= end; ++lnum) {
            sb.append(ind + " " + lines[lnum]+"\n");
        }
    }

    protected String toString(int start, int end)
    {
        // adjusted, because file lines are one-indexed, not zero.

        StringBuffer buf = new StringBuffer();

        // match the line numbering from diff(1):
        buf.append(end == Difference.NONE ? start : (1 + start));
        
        if (end != Difference.NONE && start != end) {
            buf.append(",").append(1 + end);
        }
        return buf.toString();
    }



}


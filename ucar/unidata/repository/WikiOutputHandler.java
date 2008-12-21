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



import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WikiUtil;

import ucar.unidata.xml.XmlUtil;




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
public class WikiOutputHandler extends OutputHandler {



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
    public WikiOutputHandler(Repository repository, Element element)
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
        Object[] values   = entry.getValues();
        if ((values != null) && (values.length > 0) && (values[0] != null)) {
            wikiText = (String) values[0];
        }
        StringBuffer sb = new StringBuffer(wikifyEntry(request, entry,
                              wikiText));
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
    public Result outputWikiHistory(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        boolean canEdit = getAccessManager().canEditEntry(request, entry);

        List<WikiPageHistory> history = ((WikiPageTypeHandler)entry.getTypeHandler()).getHistoryList(entry,null,false);
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE));
        sb.append(HtmlUtil.row(HtmlUtil.cols("",
                                             HtmlUtil.b(msg("Version")),
                                             HtmlUtil.b(msg("User")),
                                             HtmlUtil.b(msg("Date")),
                                             HtmlUtil.b(msg("Description")))));
        int version = 1;
        for(int i = history.size()-1;i>=0;i--) {
            WikiPageHistory wph = history.get(i);
            String edit = "";
            if(canEdit) {
                edit = HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_FORM, entry,ARG_WIKI_EDITWITH,wph.getDate().getTime()+""),
                                     HtmlUtil.img(getRepository().fileUrl(ICON_EDIT), msg("Edit with this version")));
            }
            sb.append(HtmlUtil.row(HtmlUtil.cols(edit,""+wph.getVersion(),
                                                 wph.getUser().getLabel(),
                                                 getRepository().formatDate(wph.getDate()),
                                                 wph.getDescription())));
        }

        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));

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




}


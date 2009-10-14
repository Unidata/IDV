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

package ucar.unidata.repository.data;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.monitor.LdmAction;

import ucar.unidata.repository.output.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

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
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class LdmOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_LDM = new OutputType("LDM Insert",
                                                    "ldm",
                                                    OutputType.TYPE_FILE, "",
                                                    ICON_DATA);


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public LdmOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_LDM);
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

        //Are we configured to do the LDM
        if (getRepository().getProperty(PROP_LDM_PQINSERT, "").length()
                == 0) {
            return;
        }
        if (getRepository().getProperty(PROP_LDM_QUEUE, "").length() == 0) {
            return;
        }

        if ( !request.getUser().getAdmin()) {
            return;
        }
        if (state.entry != null) {
            if ( !state.entry.isFile()) {
                return;
            }
        } else {

            boolean anyFiles = false;
            for (Entry entry : state.getAllEntries()) {
                if (entry.getResource().isFile()) {
                    anyFiles = true;
                    break;
                }
            }
            if ( !anyFiles) {
                return;
            }
        }
        links.add(makeLink(request, state.getEntry(), OUTPUT_LDM));

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
        return handleEntries(request, entry,
                             (List<Entry>) Misc.newList(entry));
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
        return handleEntries(request, group, entries);
    }

    /** _more_ */
    private String lastFeed = "SPARE";

    /** _more_ */
    private String lastProductId = "${filename}";

    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result handleEntries(Request request, Entry parent,
                                 List<Entry> entries)
            throws Exception {
        StringBuffer sb          = new StringBuffer();
        List<Entry>  fileEntries = new ArrayList<Entry>();
        List<String> ids         = new ArrayList<String>();
        for (Entry entry : entries) {
            if (entry.isFile()) {
                fileEntries.add(entry);
                ids.add(entry.getId());
            }
        }

        String feed = request.getString(PROP_LDM_FEED, lastFeed);
        String productId = request.getString(PROP_LDM_PRODUCTID,
                                             lastProductId);
        if ( !request.defined(PROP_LDM_FEED)) {
            String formUrl;
            if (parent.isGroup() && parent.isDummy()) {
                formUrl = request.url(getRepository().URL_ENTRY_GETENTRIES);
                sb.append(HtmlUtil.form(formUrl));
                sb.append(HtmlUtil.hidden(ARG_ENTRYIDS,
                                          StringUtil.join(",", ids)));
            } else {
                formUrl = request.url(getRepository().URL_ENTRY_SHOW);
                sb.append(HtmlUtil.form(formUrl));
                sb.append(HtmlUtil.hidden(ARG_ENTRYID, parent.getId()));
            }
            sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_LDM.getId()));
            sb.append(HtmlUtil.formTable());

            if (fileEntries.size() == 1) {
                File f = fileEntries.get(0).getFile();
                String fileTail =
                    getStorageManager().getFileTail(fileEntries.get(0));
                String size = " (" + f.length() + " bytes)";
                sb.append(HtmlUtil.formEntry("File:", fileTail + size));
            } else {
                int size = 0;
                for (Entry entry : fileEntries) {
                    size += entry.getFile().length();
                }
                sb.append(HtmlUtil.formEntry("Files:",
                                             fileEntries.size()
                                             + " files. Total size:" + size));
            }


            sb.append(HtmlUtil.formEntry("Feed:",
                                         HtmlUtil.select(PROP_LDM_FEED,
                                             Misc.toList(LDM_FEED_TYPES),
                                             feed)));
            String tooltip =
                "macros: ${fromday}  ${frommonth} ${fromyear} ${frommonthname}  <br>"
                + "${today}  ${tomonth} ${toyear} ${tomonthname} <br> "
                + "${filename}  ${fileextension}";
            sb.append(HtmlUtil.formEntry("Product ID:",
                                         HtmlUtil.input(PROP_LDM_PRODUCTID,
                                             productId,
                                             HtmlUtil.SIZE_60
                                             + HtmlUtil.title(tooltip))));

            sb.append(HtmlUtil.formTableClose());
            if (fileEntries.size() > 1) {
                sb.append(HtmlUtil.submit(msg("Insert files into LDM")));
            } else {
                sb.append(HtmlUtil.submit(msg("Insert file into LDM")));
            }
        } else {
            String queue = getRepository().getProperty(PROP_LDM_QUEUE, "");
            String pqinsert = getRepository().getProperty(PROP_LDM_PQINSERT,
                                  "");
            for (Entry entry : fileEntries) {
                String id =
                    getRepository().getEntryManager().replaceMacros(entry,
                        productId);
                LdmAction.insertIntoQueue(getRepository(), pqinsert, queue,
                                          feed, id,
                                          entry.getResource().getPath());
                sb.append("Inserted: "
                          + getStorageManager().getFileTail(entry));
                sb.append(HtmlUtil.br());
            }
            lastFeed      = feed;
            lastProductId = productId;
        }

        return makeLinksResult(request, msg("LDM Insert"), sb,
                               new State(parent));
    }



}


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

package ucar.unidata.repository.output;


import org.w3c.dom.*;

import ucar.unidata.repository.*;


import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class KmlOutputHandler extends OutputHandler {

    public static final String KML_ATTRS =
        " xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ";

    /** _more_ */
    public static final OutputType OUTPUT_KML =
        new OutputType("Google Earth KML", "kml",
                       OutputType.TYPE_NONHTML | OutputType.TYPE_FORSEARCH,
                       "", ICON_KML);




    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public KmlOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_KML);
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
        if (state.getEntry() != null) {
            if(!state.getEntry().isGroup()) {
                if(true) return;
            }
            links.add(makeLink(request, state.getEntry(), OUTPUT_KML));
        }
    }


    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        return repository.getMimeTypeFromSuffix(".kml");
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
        System.err.println ("output group:" + group);
        boolean justOneEntry = group.isDummy() && (entries.size() == 1)
            && (subGroups.size() == 0);


        String   title = (justOneEntry
                          ? entries.get(0).getName()
                          : group.getFullName());
        Element root = KmlUtil.kml(title);
        Element folder = KmlUtil.folder(root, title,false);
        KmlUtil.open(folder, false);
        if(group.getDescription().length()>0) {
            KmlUtil.description(folder, group.getDescription());
        }

        int cnt  = subGroups.size() + entries.size();
        int max  = request.get(ARG_MAX, DB_MAX_ROWS);
        int skip = Math.max(0, request.get(ARG_SKIP, 0));
        for(Group childGroup: subGroups) {
            String url =  getRepository().absoluteUrl(
                                                      request.url(repository.URL_ENTRY_SHOW, ARG_ENTRYID,
                                                                  childGroup.getId(), ARG_OUTPUT, OUTPUT_KML));
            Element link = KmlUtil.networkLink(folder, childGroup.getName(), url);
            if(childGroup.getDescription().length()>0) {
                KmlUtil.description(link, childGroup.getDescription());
            }

            KmlUtil.visible(link, false);
            KmlUtil.open(link, false);
            link.setAttribute(KmlUtil.ATTR_ID,childGroup.getId());
        }

        if ((cnt > 0) && ((cnt == max) || request.defined(ARG_SKIP))) {
            if (cnt >= max) {
                String skipArg = request.getString(ARG_SKIP, null);
                request.remove(ARG_SKIP);
                String url = request.url(repository.URL_ENTRY_SHOW,
                                         ARG_ENTRYID, group.getId(),
                                         ARG_OUTPUT, OUTPUT_KML,
                                         ARG_SKIP, "" + (skip + max),
                                         ARG_MAX, "" + max);

                url = getRepository().absoluteUrl(url);
                Element link = KmlUtil.networkLink(folder, "More...", url);

                if (skipArg != null) {
                    request.put(ARG_SKIP, skipArg);
                }
            }
        }


        for(Entry entry: (List<Entry>)entries) {
            String resource = entry.getResource().getPath();
            if(resource==null) continue;
            if(!IOUtil.hasSuffix(resource,"kml") &&
               !IOUtil.hasSuffix(resource,"kmz")) {
                continue;
            }

            String url;
            if(entry.getResource().isFile()) {
                String fileTail = getStorageManager().getFileTail(entry);
                url =  HtmlUtil.url(request.url(getRepository().URL_ENTRY_GET) + "/"
                                       + fileTail, ARG_ENTRYID, entry.getId());
                url = getRepository().absoluteUrl(url);
            } else if(entry.getResource().isUrl()) {
                url = entry.getResource().getPath();
            } else {
                continue;
            }
            Element link = KmlUtil.networkLink(folder, entry.getName(), url);

            if(entry.getDescription().length()>0) {
                KmlUtil.description(link, entry.getDescription());
            }
            KmlUtil.visible(link, false);
            KmlUtil.open(link, false);
            link.setAttribute(KmlUtil.ATTR_ID,entry.getId());


        }

        StringBuffer sb = new StringBuffer(XmlUtil.XML_HEADER);
        sb.append(XmlUtil.toString(root));
        return new Result(title, sb, "text/xml");
    }






}


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
import ucar.unidata.repository.auth.*;

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

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ZipFileOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_LIST =
        new OutputType("Zip File Listing", "zipfile.list",
                       OutputType.TYPE_FILE, "", ICON_ZIP);




    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public ZipFileOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_LIST);
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

        if (state.entry == null) {
            return;
        }

        if ( !state.entry.isFile()) {
            return;
        }
        if ( !getRepository().getAccessManager().canAccessFile(request,
                state.entry)) {
            return;
        }
        String path = state.entry.getResource().getPath().toLowerCase();
        if (path.endsWith(".zip") || path.endsWith(".jar")
                || path.endsWith(".zidv")) {
            links.add(makeLink(request, state.entry, OUTPUT_LIST));
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
        if ( !getRepository().getAccessManager().canAccessFile(request,
                entry)) {
            throw new AccessException("Cannot access data", request);
        }
        StringBuffer sb = new StringBuffer();

        ZipInputStream zin = new ZipInputStream(
                                 getStorageManager().getFileInputStream(
                                     entry.getResource().getPath()));
        ZipEntry ze = null;
        sb.append("<ul>");
        String fileToFetch = request.getString(ARG_FILE, null);
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.isDirectory()) {
                continue;
            }
            String path = ze.getName();
            if ((fileToFetch != null) && path.equals(fileToFetch)) {
                String type = getRepository().getMimeTypeFromSuffix(
                                  IOUtil.getFileExtension(path));
                return new Result("", zin, type);
            }
            //            if(path.endsWith("MANIFEST.MF")) continue;
            sb.append("<li>");
            String name = IOUtil.getFileTail(path);
            String url  = getRepository().URL_ENTRY_SHOW + "/" + name;

            url = HtmlUtil.url(url, ARG_ENTRYID, entry.getId(), ARG_FILE,
                               path, ARG_OUTPUT, OUTPUT_LIST.getId());
            sb.append(HtmlUtil.href(url, path));
        }
        sb.append("</ul>");

        return makeLinksResult(request, msg("Zip File Listing"), sb,
                               new State(entry));
    }



}


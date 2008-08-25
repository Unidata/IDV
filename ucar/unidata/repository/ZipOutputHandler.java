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


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
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
public class ZipOutputHandler extends OutputHandler {



    /** _more_ */
    public static final String OUTPUT_ZIP = "zip.zip";


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public ZipOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public boolean canHandle(String output) {
        return output.equals(OUTPUT_ZIP);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     * @param forHeader _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links, boolean forHeader)
            throws Exception {
        if ( !entry.isGroup()) {
            String url = request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                          entry, ARG_OUTPUT, OUTPUT_ZIP);
            links.add(new Link(url, getRepository().fileUrl(ICON_ZIP),
                               "Zip file"));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param state _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
    protected void addOutputTypes(Request request, State state,
                                  List<OutputType> types)
            throws Exception {
        if (state.forWhat == state.FOR_HEADER) {
            return;
        }
        List<Entry> entries = state.getAllEntries();
        if (entries.size() > 0) {
            boolean ok = false;
            for (Entry entry : entries) {
                if (getAccessManager().canDownload(request, entry)) {
                    ok = true;
                    break;
                }
            }
            if ( !ok) {
                return;
            }
            types.add(new OutputType("Zip File", OUTPUT_ZIP));
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
        TypeHandler  typeHandler = repository.getTypeHandler(entry.getType());
        StringBuffer sb = typeHandler.getEntryContent(entry, request, true);
        return new Result("Entry: " + entry.getName(), sb,
                          getMimeType(request.getOutput()));
    }


    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        if (output.equals(OUTPUT_ZIP)) {
            return repository.getMimeTypeFromSuffix(".zip");
        } else {
            return super.getMimeType(output);
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
        return toZip(request, entries);
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
    protected Result toZip(Request request, List<Entry> entries)
            throws Exception {
        File tmpFile =
            getRepository().getStorageManager().getTmpFile(request, "zip");
        FileOutputStream fos  = new FileOutputStream(tmpFile);
        ZipOutputStream  zos  = new ZipOutputStream(fos);
        Hashtable        seen = new Hashtable();
        for (Entry entry : entries) {
            if ( !getAccessManager().canDownload(request, entry)) {
                continue;
            }
            String path = entry.getResource().getPath();
            String name = IOUtil.getFileTail(path);
            int    cnt  = 1;
            while (seen.get(name) != null) {
                name = (cnt++) + "_" + name;
            }
            seen.put(name, name);
            zos.putNextEntry(new ZipEntry(name));
            byte[] bytes = IOUtil.readBytes(IOUtil.getInputStream(path,
                               getClass()));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
        zos.close();
        fos.close();
        return new Result("", new FileInputStream(tmpFile),
                          getMimeType(OUTPUT_ZIP));
    }


}


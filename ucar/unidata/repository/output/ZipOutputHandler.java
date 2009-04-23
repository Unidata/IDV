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
import ucar.unidata.repository.*;


import org.w3c.dom.*;


import ucar.unidata.sql.SqlUtil;
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
    public static final OutputType OUTPUT_ZIP = new OutputType("Zip File",
                                                    "zip.zip",
    /*OutputType.TYPE_NONHTML|*/
    OutputType.TYPE_FILE, "", ICON_ZIP);


    /** _more_ */
    public static final OutputType OUTPUT_ZIPGROUP =
        new OutputType("Zip Group", "zip.zipgroup",
    /*OutputType.TYPE_NONHTML|*/
    OutputType.TYPE_FILE, "", ICON_ZIP);


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
        addType(OUTPUT_ZIP);
        addType(OUTPUT_ZIPGROUP);
    }


    public AuthorizationMethod getAuthorizationMethod(Request request) {
        return AuthorizationMethod.AUTH_HTTP;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state,
                                 List<Link> links)
            throws Exception {
        if (state.entry != null) {
            if (getAccessManager().canDownload(request, state.entry)) {
                links.add(
                    makeLink(
                        request, state.entry, OUTPUT_ZIP,
                        "/" + IOUtil.stripExtension(state.entry.getName())
                        + ".zip"));
            }
        } else {
            boolean ok = false;
            for (Entry child : state.getAllEntries()) {
                if (getAccessManager().canDownload(request, child)) {
                    ok = true;
                    break;
                }
            }

            if (ok) {
                if (state.group != null) {
                    links.add(
                        makeLink(
                            request, state.group, OUTPUT_ZIPGROUP,
                            "/"
                            + IOUtil.stripExtension(state.group.getName())
                            + ".zip"));
                } else {
                    links.add(makeLink(request, state.group, OUTPUT_ZIP));
                }
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
    public Result outputEntry(Request request, Entry entry) throws Exception {
        List<Entry>entries = new ArrayList<Entry>();
        entries.add(entry);
        return toZip(request, entries);
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
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_ZIP) || output.equals(OUTPUT_ZIPGROUP)) {
            return repository.getMimeTypeFromSuffix(".zip");
        } else {
            return super.getMimeType(output);
        }
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
        OutputStream os;
        boolean doingFile = false;

        File tmpFile = null;
        if(request.getHttpServletResponse() !=null) {
            os = request.getHttpServletResponse().getOutputStream();
            request.getHttpServletResponse().setContentType(getMimeType(OUTPUT_ZIP));
        } else {
            tmpFile =
                getRepository().getStorageManager().getTmpFile(request, ".zip");
            os  = new FileOutputStream(tmpFile);
            doingFile = true;
        }

        ZipOutputStream  zos  = new ZipOutputStream(os);
        Hashtable        seen = new Hashtable();
        for (Entry entry : entries) {
            if ( !getAccessManager().canDownload(request, entry)) {
                continue;
            }
            String path = entry.getResource().getPath();
            String name = getStorageManager().getFileTail(path);
            int    cnt  = 1;
            while (seen.get(name) != null) {
                name = (cnt++) + "_" + name;
            }
            seen.put(name, name);
            zos.putNextEntry(new ZipEntry(name));
            InputStream fileInputStream = new FileInputStream(path);
            IOUtil.writeTo(fileInputStream,zos);
            fileInputStream.close();
            zos.closeEntry();
        }
        zos.close();
        if(doingFile) {
            os.close();
            return new Result("", new FileInputStream(tmpFile),
                              getMimeType(OUTPUT_ZIP));

        }
        Result result = new Result();
        result.setNeedToWrite(false);
        return result;
    }


}


/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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


import org.apache.log4j.Logger;



import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;


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
    private final Logger LOG = Logger.getLogger(ZipOutputHandler.class);


    /** _more_ */
    public static final OutputType OUTPUT_ZIP = new OutputType("Zip File",
                                                    "zip.zip",
                                                    OutputType.TYPE_FILE, "",
                                                    ICON_ZIP);


    /** _more_ */
    public static final OutputType OUTPUT_ZIPTREE =
        new OutputType("Zip Tree", "zip.tree", OutputType.TYPE_FILE, "",
                       ICON_ZIP);


    /** _more_ */
    public static final OutputType OUTPUT_ZIPGROUP =
        new OutputType("Zip Folder", "zip.zipgroup", OutputType.TYPE_FILE,
                       "", ICON_ZIP);


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
        addType(OUTPUT_ZIPTREE);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
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
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.entry != null) {
            if (getAccessManager().canDownload(request, state.entry)) {
                links.add(
                    makeLink(
                        request, state.entry, OUTPUT_ZIP,
                        "/" + IOUtil.stripExtension(state.entry.getName())
                        + ".zip"));
            }
            return;
        }

        boolean hasFile  = false;
        boolean hasGroup = false;
        for (Entry child : state.getAllEntries()) {
            if (getAccessManager().canDownload(request, child)) {
                hasFile = true;
                break;
            }
            if (child.isGroup()) {
                hasGroup = true;
            }
        }



        if (hasFile) {
            if (state.group != null) {
                links.add(
                    makeLink(
                        request, state.group, OUTPUT_ZIPGROUP,
                        "/" + IOUtil.stripExtension(state.group.getName())
                        + ".zip"));
            } else {
                links.add(makeLink(request, state.group, OUTPUT_ZIP));
            }
        }

        if ((state.group != null) && !state.group.isTopGroup()
                && (hasFile || hasGroup)) {
            links.add(makeLink(request, state.group, OUTPUT_ZIPTREE,
                               "/"
                               + IOUtil.stripExtension(state.group.getName())
                               + ".zip"));
        }

    }




    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        return toZip(request, "", entries, false, false);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, OutputType outputType,
                              Group group, List<Group> subGroups,
                              List<Entry> entries)
            throws Exception {
        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_ZIPTREE)) {
            List<Entry> all = new ArrayList<Entry>();
            all.addAll(subGroups);
            all.addAll(entries);
            getLogManager().logInfo("Doing zip tree");
            return toZip(request, group.getName(), all, true, false);
        } else {
            return toZip(request, group.getName(), entries, false, false);
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
     * @param prefix _more_
     * @param entries _more_
     * @param recurse _more_
     * @param forExport _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result toZip(Request request, String prefix, List<Entry> entries,
                        boolean recurse, boolean forExport)
            throws Exception {
        OutputStream os;
        boolean      doingFile = false;

        File         tmpFile   = null;
        if (request.getHttpServletResponse() != null) {
            os = request.getHttpServletResponse().getOutputStream();
            request.getHttpServletResponse().setContentType(
                getMimeType(OUTPUT_ZIP));
        } else {
            tmpFile = getRepository().getStorageManager().getTmpFile(request,
                    ".zip");
            os        = getStorageManager().getFileOutputStream(tmpFile);
            doingFile = true;
        }

        Result result = new Result();
        result.setNeedToWrite(false);


        Element root = null;
        boolean ok   = true;
        //First recurse down without a zos to check the size
        try {
            processZip(request, entries, recurse, 0, null, prefix, 0,
                       new int[] { 0 }, forExport, null);
        } catch (IllegalArgumentException iae) {
            ok = false;
        }

        if ( !ok) {
            javax.servlet.http.HttpServletResponse response =
                request.getHttpServletResponse();
            response.setStatus(Result.RESPONSE_UNAUTHORIZED);
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                               "Size of request has exceeded maximum size");
            return result;
        }

        ZipOutputStream zos = new ZipOutputStream(os);
        if (request.get(ARG_COMPRESS, true) == false) {
            //You would think that setting the method to stored would work
            //but it throws an error wanting the crc to be set on the ZipEntry
            //            zos.setMethod(ZipOutputStream.STORED);
            zos.setLevel(0);
        }
        Hashtable seen = new Hashtable();
        try {
            if (forExport) {
                Document doc = XmlUtil.makeDocument();
                root = XmlUtil.create(doc, TAG_ENTRIES, null,
                                      new String[] {});

            }
            processZip(request, entries, recurse, 0, zos, prefix, 0,
                       new int[] { 0 }, forExport, root);

            if (root != null) {
                String xml = XmlUtil.toString(root);
                System.err.println(xml);
                zos.putNextEntry(new ZipEntry("entries.xml"));
                byte[] bytes = xml.getBytes();
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();
            }


        } catch (IllegalArgumentException iae) {
            ok = false;
        } finally {
            IOUtil.close(zos);
        }
        if (doingFile) {
            os.close();
            return new Result(
                "", getStorageManager().getFileInputStream(tmpFile),
                getMimeType(OUTPUT_ZIP));

        }

        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param recurse _more_
     * @param level _more_
     * @param zos _more_
     * @param prefix _more_
     * @param sizeSoFar _more_
     * @param counter _more_
     * @param forExport _more_
     * @param entriesRoot _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected long processZip(Request request, List<Entry> entries,
                              boolean recurse, int level,
                              ZipOutputStream zos, String prefix,
                              long sizeSoFar, int[] counter,
                              boolean forExport, Element entriesRoot)
            throws Exception {
        long      sizeProcessed = 0;
        Hashtable seen          = new Hashtable();
        long      sizeLimit;
        if (request.isAnonymous()) {
            sizeLimit = MEGA
                        * getRepository().getProperty(
                            request.PROP_ZIPOUTPUT_ANONYMOUS_MAXSIZEMB, 100);
        } else {
            sizeLimit = MEGA
                        * getRepository().getProperty(
                            request.PROP_ZIPOUTPUT_REGISTERED_MAXSIZEMB,
                            2000);
        }
        for (Entry entry : entries) {
            counter[0]++;
            //We are getting some weirdness in the database connections so lets
            //sleep a bit every 100 entries we see
            if (counter[0] % 100 == 0) {
                System.err.println("zip count:" + counter[0] + " "
                                   + new Date());
                Misc.sleep(10);
            }
            Element entryNode = null;
            if (forExport && (entriesRoot != null)) {
                entryNode =
                    getRepository().getXmlOutputHandler().getEntryTag(null,
                        entry, entriesRoot.getOwnerDocument(), entriesRoot,
                        true, level != 0);
            }

            if (entry.isGroup() && recurse) {
                Group group = (Group) entry;
                List<Entry> children = getEntryManager().getChildren(request,
                                           group);
                String path = group.getName();
                if (prefix.length() > 0) {
                    path = prefix + "/" + path;
                }
                sizeProcessed += processZip(request, children, recurse,
                                            level + 1, zos, path,
                                            sizeProcessed + sizeSoFar,
                                            counter, forExport, entriesRoot);
            }


            if ( !getAccessManager().canDownload(request, entry)) {
                continue;
            }



            String path = entry.getResource().getPath();
            String name = getStorageManager().getFileTail(path);
            int    cnt  = 1;
            if ( !forExport) {
                while (seen.get(name) != null) {
                    name = (cnt++) + "_" + name;
                }
                seen.put(name, name);
                if (prefix.length() > 0) {
                    name = prefix + "/" + name;
                }
            }
            File f = new File(path);
            sizeProcessed += f.length();

            //check for size limit
            if (sizeSoFar + sizeProcessed > sizeLimit) {
                throw new IllegalArgumentException(
                    "Size of request has exceeded maximum size");
            }


            if (zos != null) {
                if ((entryNode != null) && forExport) {
                    zos.putNextEntry(new ZipEntry(entry.getId()));
                    XmlUtil.setAttributes(entryNode, new String[] { ATTR_FILE,
                            entry.getId(), ATTR_FILENAME, name });

                } else {
                    ZipEntry zipEntry = new ZipEntry(name);
                    System.err.println("putting:" + zipEntry);

                    zos.putNextEntry(zipEntry);
                }
                InputStream fis =
                    getStorageManager().getFileInputStream(path);
                try {
                    IOUtil.writeTo(fis, zos);
                    zos.closeEntry();
                } finally {
                    IOUtil.close(fis);
                    zos.closeEntry();
                }
            }
        }
        return sizeProcessed;
    }

}

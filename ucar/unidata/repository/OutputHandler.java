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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
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
public class OutputHandler implements Constants, Tables {

    /** _more_ */
    public static final String OUTPUT_HTML = "default.html";

    /** _more_ */
    public static final String OUTPUT_ZIP = "default.zip";



    /** _more_ */
    protected Repository repository;

    /** _more_ */
    protected static String timelineAppletTemplate;

    /** _more_ */
    protected static String graphXmlTemplate;

    /** _more_ */
    protected static String graphAppletTemplate;


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public OutputHandler(Repository repository, Element element)
            throws Exception {
        this.repository = repository;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public boolean canHandle(Request request) {
        return false;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getOutputTypesFor(Request request, String what)
            throws Exception {
        return new ArrayList();
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getOutputTypesForEntries(Request request)
            throws Exception {
        return new ArrayList();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private Result notImplemented() {
        throw new IllegalArgumentException("Given method  not implemented");
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
    public Result processShowEntry(Request request, Entry entry)
            throws Exception {
        return notImplemented();
    }



    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        return null;
    }

    public String getNextPrevLink(Request request, Entry entry, String output) {
        String nextLink  = HtmlUtil.href(HtmlUtil.url(repository.URL_SHOWENTRY, ARG_ID, entry.getId(), ARG_OUTPUT,output, ARG_NEXT, "true"),
                                         HtmlUtil.img(repository.fileUrl("/Right16.gif"),"View next entry"));
        String prevLink  = HtmlUtil.href(HtmlUtil.url(repository.URL_SHOWENTRY, ARG_ID, entry.getId(),
                                                      ARG_OUTPUT,output, ARG_PREVIOUS, "true"),
                                         HtmlUtil.img(repository.fileUrl("/Left16.gif"),"View Previous Entry"));
        return prevLink+nextLink;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param makeLinkForLastGroup _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String[] getBreadCrumbs(Request request, Group group,
                                      boolean makeLinkForLastGroup)
            throws Exception {
        List   breadcrumbs = new ArrayList();
        List   titleList   = new ArrayList();
        Group  parent      = group.getParent();
        String output      = request.getOutput();
        while (parent != null) {
            titleList.add(0, parent.getName());
            breadcrumbs.add(
                0, HtmlUtil.href(
                    HtmlUtil.url(
                        repository.URL_SHOWGROUP, ARG_GROUP,
                        parent.getFullName(), ARG_OUTPUT,
                        output), parent.getName()));
            parent = parent.getParent();
        }
        breadcrumbs.add(0, HtmlUtil.href(repository.URL_SHOWGROUP, "Top"));
        titleList.add(group.getName());
        if (makeLinkForLastGroup) {
            breadcrumbs.add(
                HtmlUtil.href(
                    HtmlUtil.url(
                        repository.URL_SHOWGROUP, ARG_GROUP,
                        group.getFullName(), ARG_OUTPUT,
                        output), group.getName()));
        } else {
            breadcrumbs.add(HtmlUtil.bold(group.getName()) + "&nbsp;"
                            + getGroupLinks(request, group));
        }
        String title = "Group: "
                       + StringUtil.join("&nbsp;&gt;&nbsp;", titleList);
        return new String[] { title,
                              StringUtil.join("&nbsp;&gt;&nbsp;",
                              breadcrumbs) };
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processShowGroup(Request request) throws Exception {
        return notImplemented();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param entries _more_
     * @param request _more_
     * @param doForm _more_
     * @param dfltSelected _more_
     *
     * @throws Exception _more_
     */
    public void getEntryHtml(StringBuffer sb, List<Entry> entries,
                             Request request, boolean doForm,
                             boolean dfltSelected)
            throws Exception {
        notImplemented();
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryUrl(Entry entry) {
        return HtmlUtil.href(HtmlUtil.url(repository.URL_SHOWENTRY, ARG_ID,
                                          entry.getId()), entry.getName());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param output _more_
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getEntriesHeader(Request request, String output,
                                    String what)
            throws Exception {
        List<TwoFacedObject> outputTypes =
            repository.getOutputTypesFor(request, what);
        int  cnt   = 0;
        List items = new ArrayList();
        for (TwoFacedObject tfo : outputTypes) {
            request.put(ARG_OUTPUT, (String) tfo.getId());
            if (tfo.getId().equals(output)) {
                items.add(tfo.toString());
            } else {
                items.add(
                    HtmlUtil.href(
                        request.getType() + "?" + request.getUrlArgs(),
                        tfo.toString(), " class=\"subnavlink\" "));
            }
        }
        return items;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listGroups(Request request) throws Exception {
        return notImplemented();
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandlers _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTypes(Request request,
                               List<TypeHandler> typeHandlers)
            throws Exception {
        return notImplemented();
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param tags _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTags(Request request, List<Tag> tags)
            throws Exception {
        return notImplemented();
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listAssociations(Request request) throws Exception {
        return notImplemented();
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
    public Result processShowGroup(Request request, Group group,
                                   List<Group> subGroups, List<Entry> entries)
            throws Exception {
        return notImplemented();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getGroupLinks(Request request, Group group)
            throws Exception {
        String search = HtmlUtil.href(
                            HtmlUtil.url(
                                repository.URL_SEARCHFORM, ARG_GROUP,
                                group.getId()), HtmlUtil.img(
                                    repository.fileUrl("/Search16.gif"),
                                    "Search in Group"));
        return search + "&nbsp;" + repository.getGraphLink(request, group);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param groups _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processShowGroups(Request request, List<Group> groups)
            throws Exception {
        return notImplemented();
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
    public Result processEntries(Request request, List<Entry> entries)
            throws Exception {
        return notImplemented();
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
        ByteArrayOutputStream bos  = new ByteArrayOutputStream();
        ZipOutputStream       zos  = new ZipOutputStream(bos);
        Hashtable             seen = new Hashtable();
        for (Entry entry : entries) {
            String path = entry.getFile();
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
        bos.close();
        return new Result("", bos.toByteArray(), getMimeType(OUTPUT_ZIP));
    }



}


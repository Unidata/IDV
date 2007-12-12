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
    public static final String OUTPUT_HTML = "html";

    /** _more_          */
    public static final String OUTPUT_ZIP = "zip";



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
     *
     *
     * @param args _more_
     * @throws Exception _more_
     */
    public OutputHandler(Repository repository,Element element) throws Exception {
        this.repository = repository;
    }

    public boolean canHandle(Request request)  {
        return false;
    }

    protected List getOutputTypesFor(Request request, String what) throws Exception {
        return  new ArrayList();
    }


    protected List getOutputTypesForEntries(Request request) throws Exception {
        return  new ArrayList();
    }


    private Result notImplemented() {
        throw new IllegalArgumentException ("Given method  not implemented");
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
    public Result processShowEntry(Request request, Entry entry) throws Exception {
        return notImplemented();
    }



    public String getMimeType(String output) {
        return null;
    }

    protected String[] getBreadCrumbs(Request request, Group group) throws Exception {
        List  breadcrumbs = new ArrayList();
        List  titleList   = new ArrayList();
        Group parent      = group.getParent();
        String output = request.getOutput();
        while (parent != null) {
            titleList.add(0, parent.getName());
            breadcrumbs.add(0, repository.href(HtmlUtil.url("/showgroup", ARG_GROUP,
                                                 parent.getFullName(),ARG_OUTPUT,output), parent.getName()));
            parent = parent.getParent();
        }
        breadcrumbs.add(0, repository.href("/showgroup", "Top"));
        titleList.add(group.getName());
        breadcrumbs.add(HtmlUtil.bold(group.getName()) + "&nbsp;"
                        + getGroupLinks(request, group));
        String title = "Group: "
            + StringUtil.join("&nbsp;&gt;&nbsp;", titleList);
        return new String[]{title, StringUtil.join("&nbsp;&gt;&nbsp;", breadcrumbs)};
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

    public void getEntryHtml(StringBuffer sb, List<Entry> entries, Request request, boolean doForm, boolean dfltSelected) throws Exception {
        notImplemented();
    }


    /**
     * _more_
     *
     * @param args _more_
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTypes(Request request,List<TypeHandler> typeHandlers) throws Exception {
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
    protected Result listTags(Request request,List<Tag> tags) throws Exception {
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





    public Result processShowGroup(Request request,Group group, List<Group> subGroups, List<Entry>entries) throws Exception {
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
        String search =
            repository.href(HtmlUtil.url("/searchform", ARG_GROUP,
                              group.getId()), HtmlUtil.img(repository.getUrlBase()
                                      + "/Search16.gif", "Search in Group"));
        return search + "&nbsp;" +repository.getGraphLink(request, group);
    }




    public Result processShowGroups(Request request,List<Group> groups) throws Exception {
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
    public Result processEntries(Request request, List<Entry> entries) throws Exception {
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
        return new Result("", bos.toByteArray(),
                          getMimeType(OUTPUT_ZIP));
    }



}


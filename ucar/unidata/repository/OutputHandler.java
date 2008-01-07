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
        String output = (String) request.getOutput();
        return canHandle(output);
    }


    public boolean canHandle(String request) {
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
    protected void getOutputTypesFor(Request request, String what, List types)
            throws Exception {
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
    protected void getOutputTypesForEntries(Request request,List<Entry> entries, List types)
            throws Exception {
    }


    protected void getOutputTypesForGroup(Request request, Group group,
                                          List<Group> subGroups, List<Entry> entries, List types)
            throws Exception {
        getOutputTypesFor(request, WHAT_ENTRIES, types);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private Result notImplemented(String method) {
        throw new IllegalArgumentException("Method: " + method  +" not implemented");
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
    public Result outputEntry(Request request, Entry entry)
            throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        return outputEntries(request,entries);
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

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param output _more_
     *
     * @return _more_
     */
    public String getNextPrevLink(Request request, Entry entry,
                                  String output) {
        String nextLink = HtmlUtil.href(
                              HtmlUtil.url(
                                  repository.URL_ENTRY_SHOW, ARG_ID,
                                  entry.getId(), ARG_OUTPUT, output,
                                  ARG_NEXT, "true"), HtmlUtil.img(
                                      repository.fileUrl("/Right16.gif"),
                                      "View next entry"));
        String prevLink = HtmlUtil.href(
                              HtmlUtil.url(
                                  repository.URL_ENTRY_SHOW, ARG_ID,
                                  entry.getId(), ARG_OUTPUT, output,
                                  ARG_PREVIOUS, "true"), HtmlUtil.img(
                                      repository.fileUrl("/Left16.gif"),
                                      "View Previous Entry"));
        return prevLink + nextLink;
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
        notImplemented("getEntryHtml");
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryUrl(Entry entry) {
        return HtmlUtil.href(HtmlUtil.url(repository.URL_ENTRY_SHOW, ARG_ID,
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
        return getHeader(request, output, repository.getOutputTypesFor(request, what));
    }



    protected List getHeader(Request request, String output,
                             List<TwoFacedObject> outputTypes)
            throws Exception {
        int    cnt           = 0;
        List   items         = new ArrayList();

        String initialOutput = request.getString(ARG_OUTPUT, "");
        for (TwoFacedObject tfo : outputTypes) {
            request.put(ARG_OUTPUT, (String) tfo.getId());
            if (tfo.getId().equals(output)) {
                items.add(tfo.toString());
            } else {
                items.add(
                    HtmlUtil.href(
                        request.getRequestPath() + "?"
                        + request.getUrlArgs(), tfo.toString(),
                            " class=\"subnavlink\" "));
            }
        }
        request.put(ARG_OUTPUT, initialOutput);
        return items;
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
        return notImplemented("listTypes");
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
        return notImplemented("listTags");
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
        return notImplemented("listAssociations");
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
        return notImplemented("outputGroup");
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
        return "";
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
    public Result outputGroups(Request request, List<Group> groups)
            throws Exception {
        return notImplemented("outputGroups");
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
    public Result outputEntries(Request request, List<Entry> entries)
            throws Exception {
        return notImplemented("outputEntries");
    }





}


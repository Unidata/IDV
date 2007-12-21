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
public class CatalogOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String CATALOG_ATTRS =
        " xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ";

    /** _more_ */
    public static final String OUTPUT_CATALOG = "thredds.catalog";



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public CatalogOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
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
        return output.equals(OUTPUT_CATALOG);
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
        if (what.equals(WHAT_ENTRIES) || what.equals(WHAT_GROUP)) {
            return getOutputTypesForEntries(request);
        }
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
        List list = new ArrayList();
        list.add(new TwoFacedObject("Thredds Catalog", OUTPUT_CATALOG));
        return list;
    }



    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        if (output.equals(OUTPUT_CATALOG)) {
            return repository.getMimeTypeFromSuffix(".xml");
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
    public Result processShowGroup(Request request, Group group,
                                   List<Group> subGroups, List<Entry> entries)
            throws Exception {
        String       title = group.getFullName();
        StringBuffer sb    = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_CATALOG,
                                  CATALOG_ATTRS
                                  + XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(XmlUtil.openTag(TAG_DATASET,
                                  XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(toCatalogInner(request, subGroups));
        sb.append(toCatalogInner(request, entries));
        sb.append(XmlUtil.closeTag(TAG_DATASET));
        sb.append(XmlUtil.closeTag(TAG_CATALOG));
        return new Result(title, sb, getMimeType(OUTPUT_CATALOG));

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
        StringBuffer sb    = new StringBuffer();
        String       title = "Groups";
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_CATALOG,
                                  CATALOG_ATTRS
                                  + XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(XmlUtil.openTag(TAG_DATASET,
                                  XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(toCatalogInner(request, groups));
        sb.append(XmlUtil.closeTag(TAG_DATASET));
        sb.append(XmlUtil.closeTag(TAG_CATALOG));
        return new Result(title, sb, getMimeType(OUTPUT_CATALOG));
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
        return toCatalog(request, entries, "Query Results");
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param objects _more_
     * @param title _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result toCatalog(Request request, List objects, String title)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_CATALOG,
                                  CATALOG_ATTRS
                                  + XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(XmlUtil.openTag(TAG_DATASET,
                                  XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(toCatalogInner(request, objects));
        sb.append(XmlUtil.closeTag(TAG_DATASET));
        sb.append(XmlUtil.closeTag(TAG_CATALOG));
        Result result = new Result(title, sb, getMimeType(OUTPUT_CATALOG));
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param objects _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected StringBuffer toCatalogInner(Request request, List objects)
            throws Exception {
        StringBuffer sb      = new StringBuffer();
        List<Entry>  entries = new ArrayList();
        List<Group>  groups  = new ArrayList();
        for (Object obj : objects) {
            if (obj instanceof Entry) {
                entries.add((Entry) obj);
            } else if (obj instanceof Group) {
                groups.add((Group) obj);
            } else {
                throw new IllegalArgumentException("Unknown object type:"
                        + obj.getClass().getName());
            }

        }
        for (Group group : groups) {
            String url =  /* "http://localhost:8080"+*/
                HtmlUtil.url(repository.URL_GROUP_SHOW, ARG_GROUP,
                             group.getFullName(), ARG_OUTPUT, OUTPUT_CATALOG);
            sb.append(XmlUtil.tag(TAG_CATALOGREF,
                                  XmlUtil.attrs(ATTR_XLINKTITLE,
                                      group.getName(), ATTR_XLINKHREF, url)));
        }

        StringBufferCollection sbc = new StringBufferCollection();
        for (Entry entry : entries) {
            StringBuffer ssb =
                sbc.getBuffer(entry.getTypeHandler().getDescription());
            ssb.append(entry.getTypeHandler().getDatasetTag(entry, request));
        }

        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb  = sbc.getBuffer(type);
            if (sbc.getKeys().size() > 1) {
                sb.append(XmlUtil.openTag(TAG_DATASET,
                                          XmlUtil.attrs(ATTR_NAME, type)));
            }
            sb.append(ssb);
            if (sbc.getKeys().size() > 1) {
                sb.append(XmlUtil.closeTag(TAG_DATASET));
            }
        }
        return sb;

    }




}


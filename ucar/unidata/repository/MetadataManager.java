/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;
import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.zip.*;


import javax.swing.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataManager  extends RepositoryManager {

    private Object MUTEX_METADATA = new Object();


    /** _more_ */
    public RequestUrl URL_METADATA_FORM = new RequestUrl(getRepository(),
                                              "/metadata/form",
                                              "Edit Metadata");

    /** _more_ */
    public RequestUrl URL_METADATA_ADDFORM = new RequestUrl(getRepository(),
                                                 "/metadata/addform",
                                                 "Add Metadata");

    /** _more_ */
    public RequestUrl URL_METADATA_ADD = new RequestUrl(getRepository(),
                                             "/metadata/add");

    /** _more_ */
    public RequestUrl URL_METADATA_CHANGE = new RequestUrl(getRepository(),
                                                "/metadata/change");




    protected Hashtable distinctMap = new Hashtable();

    /** _more_ */
    private List<MetadataHandler> metadataHandlers =
        new ArrayList<MetadataHandler>();




    /**
     * _more_
     *
     * @param args _more_
     * @param hostname _more_
     * @param port _more_
     *
     * @throws Exception _more_
     */
    public MetadataManager(Repository repository) {
        super(repository);
    }


    /** _more_ */
    MetadataHandler metadataHandler;

    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public MetadataHandler findMetadataHandler(Metadata metadata) throws Exception {
        for (MetadataHandler handler : metadataHandlers) {
            if (handler.canHandle(metadata)) {
                return handler;
            }
        }
        if(metadataHandler==null) {
            metadataHandler = new MetadataHandler(getRepository(), null);
        }
        return metadataHandler;
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(Entry entry) throws Exception {
        String query = SqlUtil.makeSelect(
                           COLUMNS_METADATA, Misc.newList(
                               TABLE_METADATA), SqlUtil.eq(
                               COL_METADATA_ENTRY_ID, SqlUtil.quote(
                                   entry.getId())), " order by "
                                       + COL_METADATA_TYPE);

        SqlUtil.Iterator iter =
            SqlUtil.getIterator(getDatabaseManager().execute(query));
        ResultSet      results;
        List<Metadata> metadata = new ArrayList();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                String type = results.getString(3);
                MetadataHandler handler = findMetadataHandler(type);
                metadata.add(handler.makeMetadata(results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++)));
            }
        }
        return metadata;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected List<MetadataHandler> getMetadataHandlers() {
        return metadataHandlers;
    }




    public MetadataHandler findMetadataHandler(String type) {
        for (MetadataHandler handler : metadataHandlers) {
            if (handler.canHandle(type)) {
                return handler;
            }
        }
        return metadataHandler;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initMetadataHandlers(List<String> metadataDefFiles) throws Exception {
        for (String file : metadataDefFiles) {
            try {
                Element root = XmlUtil.getRoot(file, getClass());
                List children = XmlUtil.findChildren(root,
                                    TAG_METADATAHANDLER);
                for (int i = 0; i < children.size(); i++) {
                    Element node = (Element) children.get(i);
                    Class c = Misc.findClass(XmlUtil.getAttribute(node,
                                  ATTR_CLASS));
                    Constructor ctor = Misc.findConstructor(c,
                                           new Class[] { Repository.class,
                            Element.class });
                    metadataHandlers.add(
                        (MetadataHandler) ctor.newInstance(
                                                           new Object[] { getRepository(),
                                           node }));
                }
            } catch (Exception exc) {
                System.err.println("Error loading metadata handler file:"
                                   + file);
                throw exc;
            }

        }
    }



    public void addToSearchForm(Request request, StringBuffer sb)
            throws Exception {
        for (MetadataHandler handler : metadataHandlers) {
            handler.addToSearchForm(request, sb);
        }
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
    public Result processMetadataChange(Request request) throws Exception {
        synchronized(MUTEX_METADATA) {
            Entry entry = getRepository().getEntry(request);

            if (request.exists(ARG_DELETE)) {
                Hashtable args = request.getArgs();
                for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
                    String arg = (String) keys.nextElement();
                    if ( !arg.startsWith(ARG_METADATA_ID + ".select.")) {
                        continue;
                    }
                    String id = request.getString(arg, "");
                    getDatabaseManager().executeDelete(TABLE_METADATA,
                                                       COL_METADATA_ID, SqlUtil.quote(id));
                }
            } else {
                List<Metadata> newMetadata = new ArrayList<Metadata>();
                for (MetadataHandler handler : metadataHandlers) {
                    handler.handleFormSubmit(request, entry, newMetadata);
                }

                for (Metadata metadata : newMetadata) {
                    getDatabaseManager().executeDelete(TABLE_METADATA,
                                                       COL_METADATA_ID, SqlUtil.quote(metadata.getId()));
                    insertMetadata(metadata);
                }
            }
            entry.setMetadata(null);
            return new Result(HtmlUtil.url(URL_METADATA_FORM, ARG_ID,
                                           entry.getId()));
        }
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
    public Result processMetadataForm(Request request) throws Exception {
        StringBuffer sb    = new StringBuffer();
        Entry entry = getRepository().getEntry(request);
        
        sb.append(getRepository().makeEntryHeader(request, entry));

        List<Metadata> metadataList = getMetadata(entry);
        sb.append("<p>");
        if (metadataList.size() == 0) {
            sb.append("No metadata defined");
        } else {
            sb.append(HtmlUtil.formPost(URL_METADATA_CHANGE));
            sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
            sb.append(HtmlUtil.submit("Change"));
            sb.append(HtmlUtil.space(2));
            sb.append(HtmlUtil.submit("Delete selected", ARG_DELETE));
            sb.append("<table cellpadding=\"5\">");
            for (Metadata metadata : metadataList) {
                MetadataHandler metadataHandler =
                    findMetadataHandler(metadata);
                if (metadataHandler == null) {
                    continue;
                }
                String[] html = metadataHandler.getForm(metadata, true);
                if (html == null) {
                    continue;
                }
                String cbx = HtmlUtil.checkbox(ARG_METADATA_ID + ".select."
                                 + metadata.getId(), metadata.getId(), false);
                sb.append(HtmlUtil.rowTop(HtmlUtil.cols(cbx + html[0],
                        html[1])));
            }
            sb.append("</table>");
            sb.append(HtmlUtil.submit("Change"));
            sb.append(HtmlUtil.space(2));
            sb.append(HtmlUtil.submit("Delete Selected", ARG_DELETE));
            sb.append(HtmlUtil.formClose());
        }

        return getRepository().makeEntryEditResult(request, entry, "Edit Metadata", sb);

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
    public Result processMetadataAddForm(Request request) throws Exception {
        StringBuffer sb    = new StringBuffer();
        Entry entry = getRepository().getEntry(request);
        sb.append(getRepository().makeEntryHeader(request, entry));
        sb.append(HtmlUtil.formTable());
        for (MetadataHandler handler : metadataHandlers) {
            handler.makeAddForm(request, entry, sb);
        }
        sb.append(HtmlUtil.formTableClose());
        return getRepository().makeEntryEditResult(request, entry, "Add Metadata", sb);
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
    public Result processMetadataAdd(Request request) throws Exception {
        synchronized(MUTEX_METADATA) {
        Entry entry = getRepository().getEntry(request);
        List<Metadata> newMetadata = new ArrayList<Metadata>();
        for (MetadataHandler handler : metadataHandlers) {
            handler.handleAddSubmit(request, entry, newMetadata);
        }

        for (Metadata metadata : newMetadata) {
            insertMetadata(metadata);
        }



        return new Result(HtmlUtil.url(URL_METADATA_FORM, ARG_ID,
                                       entry.getId()));

        }
    }



    public String[] getDistinctValues(Request request, MetadataHandler handler, Metadata.Type type) throws Exception {
        if(distinctMap == null) {
            distinctMap = new Hashtable();
        }
        String[]values = (String[]) distinctMap.get(type.getType());

        if(values == null) {
            Statement stmt = getDatabaseManager().execute(
                                                          SqlUtil.makeSelect(SqlUtil.distinct(COL_METADATA_ATTR1), TABLE_METADATA,
                                                                             SqlUtil.eq(COL_METADATA_TYPE,SqlUtil.quote(type.getType()))));
            values = SqlUtil.readString(stmt, 1);
            distinctMap.put(type.getType(), values);
        }
        return values;
    }


    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    public void insertMetadata(Metadata metadata) throws Exception {
        distinctMap = null;
        getDatabaseManager().executeInsert(INSERT_METADATA, new Object[] {
            metadata.getId(), metadata.getEntryId(), metadata.getType(),
            metadata.getAttr1(), metadata.getAttr2(), metadata.getAttr3(),
            metadata.getAttr4()
        });
    }




}


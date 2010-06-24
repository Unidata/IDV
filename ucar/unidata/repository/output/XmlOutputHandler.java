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


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;

import java.net.*;

import java.sql.ResultSet;
import java.sql.Statement;




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
public class XmlOutputHandler extends OutputHandler {

    /** _more_ */
    public static final OutputType OUTPUT_XML =
        new OutputType("XML", "xml.xml",
                       OutputType.TYPE_NONHTML | OutputType.TYPE_FORSEARCH,
                       "", ICON_XML);


    /** _more_ */
    public static final OutputType OUTPUT_XMLENTRY =
        new OutputType("XML Entry", "xml.xmlentry",
                       OutputType.TYPE_NONHTML | OutputType.TYPE_FORSEARCH,
                       "", ICON_XML);



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public XmlOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_XML);
        addType(OUTPUT_XMLENTRY);
    }












    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_XML) || output.equals(OUTPUT_XMLENTRY)) {
            return repository.getMimeTypeFromSuffix(".xml");
        }
        return super.getMimeType(output);
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
        Document     doc = XmlUtil.makeDocument();
        Element root     = getEntryTag(request, entry, doc, null, false,
                                       true);
        StringBuffer sb  = new StringBuffer(XmlUtil.toString(root));
        return new Result("", sb, repository.getMimeTypeFromSuffix(".xml"));
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

        if (outputType.equals(OUTPUT_XMLENTRY)) {
            return outputEntry(request, outputType, group);
        }

        Document doc  = XmlUtil.makeDocument();
        Element  root = getGroupTag(request, group, doc, null);
        for (Group subgroup : subGroups) {
            getGroupTag(request, subgroup, doc, root);
        }
        for (Entry entry : entries) {
            getEntryTag(request, entry, doc, root, false, true);
        }
        StringBuffer sb = new StringBuffer(XmlUtil.toString(root));
        return new Result("", sb, repository.getMimeTypeFromSuffix(".xml"));
    }




    /**
     * _more_
     *
     * @param entry _more_
     * @param doc _more_
     * @param parent _more_
     *
     * @throws Exception _more_
     */
    private void addMetadata(Entry entry, Document doc, Element parent)
            throws Exception {}


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param doc _more_
     * @param parent _more_
     * @param forExport _more_
     * @param includeParentId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Element getEntryTag(Request request, Entry entry, Document doc,
                               Element parent, boolean forExport,
                               boolean includeParentId)
            throws Exception {

        Element node = XmlUtil.create(doc, TAG_ENTRY, parent, new String[] {
            ATTR_ID, entry.getId(), ATTR_NAME, entry.getName(), ATTR_PARENT,
            (includeParentId
             ? entry.getParentGroupId()
             : ""), ATTR_TYPE, entry.getTypeHandler().getType(),
            ATTR_ISGROUP, "" + entry.isGroup(), ATTR_FROMDATE,
            getRepository().formatDate(new Date(entry.getStartDate())),
            ATTR_TODATE,
            getRepository().formatDate(new Date(entry.getEndDate())),
            ATTR_CREATEDATE,
            getRepository().formatDate(new Date(entry.getCreateDate()))
        });


        if (entry.hasAltitude()) {
            node.setAttribute(ATTR_ALTITUDE, "" + entry.getAltitude());
        } else {
            if (entry.hasAltitudeBottom()) {
                node.setAttribute(ATTR_ALTITUDE_BOTTOM, "" + entry.getAltitudeBottom());
            }
            if (entry.hasAltitudeTop()) {
                node.setAttribute(ATTR_ALTITUDE_TOP, "" + entry.getAltitudeTop());
            }
        }

        if (entry.hasNorth()) {
            node.setAttribute(ATTR_NORTH, "" + entry.getNorth());
        }
        if (entry.hasSouth()) {
            node.setAttribute(ATTR_SOUTH, "" + entry.getSouth());
        }
        if (entry.hasEast()) {
            node.setAttribute(ATTR_EAST, "" + entry.getEast());
        }
        if (entry.hasWest()) {
            node.setAttribute(ATTR_WEST, "" + entry.getWest());
        }

        if ( !entry.isGroup() && entry.getResource().isDefined()) {
            if (forExport) {}
            else {
                Resource resource = entry.getResource();
                XmlUtil.setAttributes(node, new String[] { ATTR_RESOURCE,
                        resource.getPath(), ATTR_RESOURCE_TYPE,
                        resource.getType() });
                String md5 = resource.getMd5();
                if(md5!=null) {
                    node.setAttribute(ATTR_MD5, md5);
                }
                long filesize = resource.getFileSize();
                if(filesize>=0) {
                    node.setAttribute(ATTR_FILESIZE, ""+filesize);
                }
            }

            //Add the service nodes
            if ( !forExport) {
                for (OutputHandler outputHandler :
                        getRepository().getOutputHandlers()) {
                    outputHandler.addToEntryNode(request, entry, node);
                }

                if (getRepository().getAccessManager().canAccessFile(request,
                        entry)) {
                    node.setAttribute(ATTR_FILESIZE,
                                      "" + entry.getResource().getFileSize());
                    String url =
                        getRepository().getEntryManager().getEntryResourceUrl(
                            request, entry, true);
                    Element serviceNode = XmlUtil.create(TAG_SERVICE, node);
                    XmlUtil.setAttributes(serviceNode,
                                          new String[] { ATTR_TYPE,
                            SERVICE_FILE, ATTR_URL, url });
                }
            }
        }


        if ((entry.getDescription() != null)
                && (entry.getDescription().length() > 0)) {
            XmlUtil.create(doc, TAG_DESCRIPTION, node,
                           entry.getDescription(), null);
        }
        addMetadata(entry, doc, node);
        entry.getTypeHandler().addToEntryNode(entry, node);
        return node;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param group _more_
     * @param doc _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element getGroupTag(Request request, Group group, Document doc,
                                Element parent)
            throws Exception {
        Element node = getEntryTag(request, group, doc, parent, false, true);
        boolean canDoNew = getAccessManager().canDoAction(request, group,
                               Permission.ACTION_NEW);
        boolean canDoUpload = getAccessManager().canDoAction(request, group,
                                  Permission.ACTION_UPLOAD);
        node.setAttribute(ATTR_CANDONEW, "" + canDoNew);
        node.setAttribute(ATTR_CANDOUPLOAD, "" + canDoUpload);
        return node;

    }


}

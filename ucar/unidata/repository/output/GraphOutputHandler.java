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

import ucar.unidata.sql.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


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
public class GraphOutputHandler extends OutputHandler {

    /** _more_ */
    public static final OutputType OUTPUT_GRAPH =
        new OutputType("Graph", "graph.graph", OutputType.TYPE_NONHTML, "",
                       ICON_GRAPH);



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public GraphOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GRAPH);
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
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_GRAPH));
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
        String graphAppletTemplate =
            getRepository().getResource(PROP_HTML_GRAPHAPPLET);
        String type = request.getString(ARG_NODETYPE, (String) null);
        if (type == null) {
            type = entry.isGroup()
                   ? NODETYPE_GROUP
                   : NODETYPE_ENTRY;
        }
        String html = StringUtil.replace(graphAppletTemplate, "${id}",
                                         HtmlUtil.urlEncode(entry.getId()));
        html = StringUtil.replace(html, "${root}",
                                  getRepository().getUrlBase());
        html = StringUtil.replace(html, "${type}", HtmlUtil.urlEncode(type));
        StringBuffer sb = new StringBuffer();
        sb.append(html);
        Result result = new Result(msg("Graph"), sb);
        addLinks(request, result, new State(entry));
        return result;

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
        return outputEntry(request, group);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param id _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    protected void getAssociationsGraph(Request request, String id,
                                        StringBuffer sb)
            throws Exception {
        List<Association> associations =
            getAssociationManager().getAssociations(request, id);
        for (Association association : associations) {
            Entry   other  = null;
            boolean isTail = true;
            if (association.getFromId().equals(id)) {
                other = getEntryManager().getEntry(request,
                        association.getToId());
                isTail = true;
            } else {
                other = getEntryManager().getEntry(request,
                        association.getFromId());
                isTail = false;
            }

            if (other != null) {
                sb.append(XmlUtil.tag(TAG_NODE,
                                      XmlUtil.attrs(ATTR_TYPE,
                                          (other.isGroup()
                                           ? NODETYPE_GROUP
                                           : other.getTypeHandler()
                                           .getNodeType()), ATTR_ID,
                                               other.getId(), ATTR_TITLE,
                                                   other.getName())));
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "association",
                                          ATTR_FROM, (isTail
                        ? id
                        : other.getId()), ATTR_TO, (isTail
                        ? other.getId()
                        : id))));
            }
        }
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryNodeXml(Request request, ResultSet results)
            throws Exception {
        int    col      = 1;
        String entryId  = results.getString(col++);
        String name     = results.getString(col++);
        String fileType = results.getString(col++);
        String groupId  = results.getString(col++);
        String resource =
            getStorageManager().resourceFromDB(results.getString(col++));
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        String      nodeType    = typeHandler.getNodeType();
        if (ImageUtils.isImage(resource)) {
            nodeType = "imageentry";
        }
        String attrs = XmlUtil.attrs(ATTR_TYPE, nodeType, ATTR_ID, entryId,
                                     ATTR_TITLE, name);
        Entry entry = getEntryManager().getEntry(request, entryId);
        if (entry != null) {
            attrs += " "
                     + XmlUtil.attrs("imagepath",
                                     getEntryManager().getIconUrl(request,
                                         entry));
        }
        if (ImageUtils.isImage(resource)) {
            String imageUrl = HtmlUtil.url(
                                  getRepository().URL_ENTRY_GET + entryId
                                  + IOUtil.getFileExtension(
                                      resource), ARG_ENTRYID, entryId,
                                          ARG_IMAGEWIDTH, "75");
            attrs = attrs + " " + XmlUtil.attr("image", imageUrl);
        }
        //        System.err.println (XmlUtil.tag(TAG_NODE,attrs));
        return XmlUtil.tag(TAG_NODE, attrs);
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
    public Result processGraphGet(Request request) throws Exception {

        String graphXmlTemplate =
            getRepository().getResource(PROP_HTML_GRAPHTEMPLATE);
        String  id         = (String) request.getId((String) null);
        String  originalId = id;
        String  type = (String) request.getString(ARG_NODETYPE,
                           (String) null);
        int     cnt        = 0;
        int     actualCnt  = 0;

        int     skip       = request.get(ARG_SKIP, 0);
        boolean haveSkip   = false;

        if (id.startsWith("skip_")) {
            haveSkip = true;
            //skip_tag_" +(cnt+skip)+"_"+id;
            List toks = StringUtil.split(id, "_", true, true);
            type = (String) toks.get(1);
            skip = new Integer((String) toks.get(2)).intValue();
            toks.remove(0);
            toks.remove(0);
            toks.remove(0);
            id = StringUtil.join("_", toks);
        }

        int MAX_EDGES = 15;
        if (id == null) {
            throw new IllegalArgumentException("Could not find id:"
                    + request);
        }
        if (type == null) {
            type = NODETYPE_GROUP;
        }
        TypeHandler  typeHandler = getRepository().getTypeHandler(request);
        StringBuffer sb          = new StringBuffer();
        if ( !type.equals(TypeHandler.TYPE_GROUP)) {
            Statement stmt = typeHandler.select(
                                 request,
                                 SqlUtil.comma(
                                     Tables.ENTRIES.COL_ID,
                                     Tables.ENTRIES.COL_NAME,
                                     Tables.ENTRIES.COL_TYPE,
                                     Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                     Tables.ENTRIES.COL_RESOURCE), Clause.eq(
                                         Tables.ENTRIES.COL_ID, id), "");

            ResultSet results = stmt.getResultSet();
            if ( !results.next()) {
                throw new IllegalArgumentException("Unknown entry id:" + id);
            }
            sb.append(getEntryNodeXml(request, results));
            getAssociationsGraph(request, id, sb);

            Group group = getEntryManager().findGroup(request,
                              results.getString(4));
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                      ATTR_ID, group.getId(), ATTR_TOOLTIP,
                                      group.getName(), ATTR_TITLE,
                                      getGraphNodeTitle(group.getName()))));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, group.getId(), ATTR_TO,
                                      results.getString(1))));

            String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                            sb.toString());

            xml = StringUtil.replace(xml, "${root}",
                                     getRepository().getUrlBase());
            //            System.err.println(xml);
            return new Result(BLANK, new StringBuffer(xml),
                              getRepository().getMimeTypeFromSuffix(".xml"));
        }

        Group group = getEntryManager().findGroup(request, id);
        if (group == null) {
            throw new RepositoryUtil.MissingEntryException(
                "Could not find group:" + id);
        }
        sb.append(
            XmlUtil.tag(
                TAG_NODE,
                XmlUtil.attrs(
                    ATTR_TYPE, NODETYPE_GROUP, ATTR_ID, group.getId(),
                    ATTR_TOOLTIP, group.getName(), ATTR_TITLE,
                    getGraphNodeTitle(group.getName()))));
        getAssociationsGraph(request, id, sb);
        List<Group> subGroups = getEntryManager().getGroups(
                                    request,
                                    Clause.eq(
                                        Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                        group.getId()));


        Group parent = getEntryManager().findGroup(request,
                           group.getParentGroupId());
        if (parent != null) {
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                      ATTR_ID, parent.getId(), ATTR_TOOLTIP,
                                      parent.getName(), ATTR_TITLE,
                                      getGraphNodeTitle(parent.getName()))));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, parent.getId(), ATTR_TO,
                                      group.getId())));
        }


        cnt       = 0;
        actualCnt = 0;
        for (Group subGroup : subGroups) {
            if (++cnt <= skip) {
                continue;
            }
            actualCnt++;

            sb.append(
                XmlUtil.tag(
                    TAG_NODE,
                    XmlUtil.attrs(
                        ATTR_TYPE, NODETYPE_GROUP, ATTR_ID, subGroup.getId(),
                        ATTR_TOOLTIP, subGroup.getName(), ATTR_TITLE,
                        getGraphNodeTitle(subGroup.getName()))));

            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, (haveSkip
                    ? originalId
                    : group.getId()), ATTR_TO, subGroup.getId())));

            if (actualCnt >= MAX_EDGES) {
                String skipId = "skip_" + type + "_" + (actualCnt + skip)
                                + "_" + id;
                sb.append(XmlUtil.tag(TAG_NODE,
                                      XmlUtil.attrs(ATTR_TYPE, "skip",
                                          ATTR_ID, skipId, ATTR_TITLE,
                                          "...")));
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "etc",
                                          ATTR_FROM, originalId, ATTR_TO,
                                          skipId)));
                break;
            }
        }


        Statement stmt =
            getDatabaseManager().select(SqlUtil.comma(Tables.ENTRIES.COL_ID,
                Tables.ENTRIES.COL_NAME, Tables.ENTRIES.COL_TYPE,
                Tables.ENTRIES.COL_PARENT_GROUP_ID,
                Tables.ENTRIES.COL_RESOURCE), Tables.ENTRIES.NAME,
                    Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                              group.getId()));
        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        cnt       = 0;
        actualCnt = 0;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                cnt++;
                if (cnt <= skip) {
                    continue;
                }
                actualCnt++;
                sb.append(getEntryNodeXml(request, results));
                String entryId = results.getString(1);
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                          ATTR_FROM, (haveSkip
                        ? originalId
                        : group.getId()), ATTR_TO, entryId)));
                sb.append("\n");
                if (actualCnt >= MAX_EDGES) {
                    String skipId = "skip_" + type + "_" + (actualCnt + skip)
                                    + "_" + id;
                    sb.append(XmlUtil.tag(TAG_NODE,
                                          XmlUtil.attrs(ATTR_TYPE, "skip",
                                              ATTR_ID, skipId, ATTR_TITLE,
                                                  "...")));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE, "etc",
                                              ATTR_FROM, originalId, ATTR_TO,
                                                  skipId)));
                    break;
                }
            }
        }


        String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                        sb.toString());
        xml = StringUtil.replace(xml, "${root}",
                                 getRepository().getUrlBase());
        //        System.err.println(xml);
        return new Result(BLANK, new StringBuffer(xml),
                          getRepository().getMimeTypeFromSuffix(".xml"));

    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String getGraphNodeTitle(String s) {
        if (s.length() > 40) {
            s = s.substring(0, 39) + "...";
        }
        return s;
    }






}


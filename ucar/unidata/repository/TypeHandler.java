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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class TypeHandler implements Constants, Tables {

    /** _more_          */
    public static final int MATCH_UNKNOWN = 0;

    /** _more_          */
    public static final int MATCH_TRUE = 1;

    /** _more_          */
    public static final int MATCH_FALSE = 2;


    /** _more_ */
    public static final TwoFacedObject ALL_OBJECT = new TwoFacedObject("All",
                                                        "");

    /** _more_ */
    public static final String TYPE_ANY = "any";

    /** _more_ */
    Repository repository;

    /** _more_ */
    String type;

    /** _more_ */
    String description;



    /**
     * _more_
     *
     * @param repository _more_
     */
    public TypeHandler(Repository repository) {
        this.repository = repository;
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     */
    public TypeHandler(Repository repository, String type) {
        this(repository, type, "");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param description _more_
     */
    public TypeHandler(Repository repository, String type,
                       String description) {
        this.repository  = repository;
        this.type        = type;
        this.description = description;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     *
     * @return _more_
     */
    public void getDatasetTag(StringBuffer sb, Entry entry, Request request) {
        File f= entry.getResource().getFile();
        sb.append(XmlUtil.openTag(CatalogOutputHandler.TAG_DATASET,
                                  XmlUtil.attrs(ATTR_NAME, entry.getName(),
                                                CatalogOutputHandler.ATTR_URLPATH, entry.getResource().getPath())));

        sb.append(XmlUtil.tag(CatalogOutputHandler.TAG_SERVICENAME,"","self"));
        if(f.exists()) {
            sb.append(XmlUtil.tag(CatalogOutputHandler.TAG_DATASIZE,XmlUtil.attrs(CatalogOutputHandler.ATTR_UNITS,"bytes"),""+f.length()));
        }


        sb.append(XmlUtil.tag(CatalogOutputHandler.TAG_DATE, XmlUtil.attrs(CatalogOutputHandler.ATTR_TYPE,"metadataCreated"),format(new Date(entry.getCreateDate()))));

        sb.append(XmlUtil.openTag(CatalogOutputHandler.TAG_TIMECOVERAGE));
        sb.append(XmlUtil.tag(CatalogOutputHandler.TAG_START,"",""+format(new Date(entry.getStartDate()))));
        sb.append(XmlUtil.tag(CatalogOutputHandler.TAG_END,"",""+format(new Date(entry.getEndDate()))));
        sb.append(XmlUtil.closeTag(CatalogOutputHandler.TAG_TIMECOVERAGE));

        sb.append(XmlUtil.closeTag(CatalogOutputHandler.TAG_DATASET));
    }


    public String format(Date d) {
        return d.toString();
    }

    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public boolean equals(Object obj) {
        if ( !(obj.getClass().equals(getClass()))) {
            return false;
        }
        return Misc.equals(type, ((TypeHandler) obj).getType());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getNodeType() {
        return NODETYPE_ENTRY;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getType() {
        return type;
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean isType(String type) {
        return this.type.equals(type);
    }


    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntry(ResultSet results) throws Exception {
        //id,type,name,desc,group,user,file,createdata,fromdate,todate
        int col = 3;
        Entry entry =
            new Entry(results.getString(1), this, results.getString(col++),
                      results.getString(col++),
                      repository.findGroup(results.getString(col++)),
                      repository.getUserManager().findUser(results.getString(col++)),
                      new Resource(results.getString(col++),results.getString(col++)),
                      results.getTimestamp(col++).getTime(),
                      results.getTimestamp(col++).getTime(),
                      results.getTimestamp(col++).getTime());
        entry.setSouth(results.getDouble(col++));
        entry.setNorth(results.getDouble(col++));
        entry.setEast(results.getDouble(col++));
        entry.setWest(results.getDouble(col++));
        return entry;
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     * @param showResource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuffer getEntryContent(Entry entry, Request request,
                                        boolean showResource)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append("<table cellspacing=\"5\" cellpadding=\"2\">");
            sb.append(getInnerEntryContent(entry, request, output,
                                           showResource));
            sb.append("</table>\n");
            List<Tag> tags = repository.getTags(request, entry.getId());
            if (tags.size() > 0) {
                sb.append(HtmlUtil.bold("Tags"));
                sb.append("<ul>\n");
                for (Tag tag : tags) {
                    sb.append("<li> ");
                    sb.append(repository.getTagLinks(request, tag.getName()));
                    sb.append(tag.getName());
                }
                sb.append("</ul>\n");
            }



            List<Metadata> metadataList = repository.getMetadata(entry);
            if (metadataList.size() > 0) {
                sb.append("<p>");
                sb.append(HtmlUtil.bold("Metadata:"));
                sb.append("<ul>");
                for (Metadata metadata : metadataList) {
                    sb.append("<li>");
                    if (metadata.getMetadataType().equals(
                            Metadata.TYPE_LINK)) {
                        sb.append(metadata.getName() + ": ");
                        sb.append(HtmlUtil.href(metadata.getContent(),
                                metadata.getContent()));
                    } else {
                        sb.append(metadata.getName());
                        sb.append(" ");
                        sb.append(metadata.getContent());
                    }
                }
                sb.append("</ul>");
            }



            List<Association> associations =
                repository.getAssociations(request, entry.getId());
            if (associations.size() > 0) {
                sb.append(HtmlUtil.bold("Associations"));
                sb.append("<ul>\n");
                for (Association association : associations) {
                    Entry fromEntry = null;
                    Entry toEntry   = null;
                    if (association.getFromId().equals(entry.getId())) {
                        fromEntry = entry;
                    } else {
                        fromEntry =
                            repository.getEntry(association.getFromId(),
                                request);
                    }
                    if (association.getToId().equals(entry.getId())) {
                        toEntry = entry;
                    } else {
                        toEntry = repository.getEntry(association.getToId(),
                                request);
                    }
                    if ((fromEntry == null) || (toEntry == null)) {
                        continue;
                    }
                    sb.append("<li>");
                    sb.append(((fromEntry == entry)
                               ? fromEntry.getName()
                               : repository.getEntryUrl(fromEntry)));
                    sb.append("&nbsp;&nbsp;");
                    sb.append(
                        HtmlUtil.bold(association.getName()) + " "
                        + HtmlUtil.img(repository.fileUrl("/Arrow16.gif")));
                    sb.append("&nbsp;&nbsp;");
                    sb.append(((toEntry == entry)
                               ? toEntry.getName()
                               : repository.getEntryUrl(toEntry)));
                }

                sb.append("</ul>\n");
            }

        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {}
        return sb;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryLinks(Entry entry, Request request)
            throws Exception {
        String editEntry = HtmlUtil.href(
                                 HtmlUtil.url(
                                     repository.URL_ENTRY_FORM, ARG_ID,
                                     entry.getId()), HtmlUtil.img(
                                         repository.fileUrl("/Edit16.gif"),
                                         "Edit Entry"));

        return editEntry + HtmlUtil.space(1) + 
            getEntryDownloadLink(request, entry) +  HtmlUtil.space(1) + 
            getGraphLink(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getGraphLink(Request request, Entry entry) {
        if ( !repository.isAppletEnabled(request)) {
            return "";
        }
        return HtmlUtil
            .href(HtmlUtil
                .url(repository.URL_GRAPH_VIEW, ARG_ID, entry.getId(),
                     ARG_NODETYPE, entry.getType()), HtmlUtil
                         .img(repository.fileUrl("/tree.gif"),
                              "Show file in graph"));
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
    protected boolean canDownload(Request request, Entry entry)
            throws Exception {
        if ( !entry.isFile()) {
            return false;
        }
        return true;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryDownloadLink(Request request, Entry entry)
            throws Exception {
        if ( !repository.canDownload(request, entry)) {
            return "";
        }
        File   f    = entry.getResource().getFile();
        String size = " (" + f.length() + " bytes)";
        if (repository.getProperty(PROP_DOWNLOAD_ASFILES, false)) {
            return HtmlUtil.href(
                "file://" + entry.getResource(),
                HtmlUtil.img(
                    repository.fileUrl("/Fetch.gif"),
                    "Download file" + size));
        } else {
            return HtmlUtil.href(
                HtmlUtil.url(
                    repository.URL_ENTRY_GET + "/"
                    + entry.getName(), ARG_ID, entry.getId()), HtmlUtil.img(
                        repository.fileUrl("/Fetch.gif"), "Download file"
                        + size));
        }
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     * @param output _more_
     * @param showResource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuffer getInnerEntryContent(Entry entry, Request request,
                                             String output,
                                             boolean showResource)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            OutputHandler outputHandler =
                repository.getOutputHandler(request);
            String nextPrev = outputHandler.getNextPrevLink(request, entry,
                                  output);
            sb.append(HtmlUtil.formEntry("",
                                          getEntryLinks(entry, request)
                                          + HtmlUtil.space(2) + nextPrev));
            sb.append(HtmlUtil.formEntry("Name:",
                                          entry.getName()));


            String[] crumbs = repository.getBreadCrumbs(request,
                                  entry.getGroup(), true);
            sb.append(HtmlUtil.formEntry("Group:",
                                          crumbs[1]));

            String desc = entry.getDescription();
            if ((desc != null) && (desc.length() > 0)) {
                sb.append(HtmlUtil.formEntry("Description:",
                        desc));
            }
            sb.append(HtmlUtil.formEntry("Created by:",
                                          entry.getUser().getName() + " @ "
                                          + fmt(entry.getCreateDate())));

            sb.append(HtmlUtil.formEntry("Resource:",
                                          entry.getResource().getPath()));

            if (entry.isFile()) {
                sb.append(HtmlUtil.formEntry("Size:",
                                              entry.getResource().getFile().length() + " bytes"));
            }
            System.err.println ("create date:" + new Date(entry.getCreateDate()));
            System.err.println ("start date:" + new Date(entry.getStartDate()));
            System.err.println ("end date:" + new Date(entry.getEndDate()));
            if ((entry.getCreateDate() != entry.getStartDate())
                    || (entry.getCreateDate() != entry.getEndDate())) {
                if (entry.getEndDate() != entry.getStartDate()) {
                    sb.append(
                        HtmlUtil.formEntry(
                            "Date Range:",
                            fmt(entry.getStartDate()) + " -- "
                            + fmt(entry.getEndDate())));
                } else {
                    sb.append(HtmlUtil.formEntry("Date:",
                            fmt(entry.getStartDate())));
                }
            }
            String typeDesc = entry.getTypeHandler().getDescription();
            if ((typeDesc == null) || (typeDesc.trim().length() == 0)) {
                typeDesc = entry.getTypeHandler().getType();
            }
            sb.append(HtmlUtil.formEntry("Entry Type:", typeDesc));

            if (entry.hasLocationDefined()) {
                sb.append(HtmlUtil.formEntry("Location:",
                        entry.getSouth() + "/" + entry.getEast()));
            } else if (entry.hasAreaDefined()) {
                String img = HtmlUtil.img(HtmlUtil.url(repository.URL_GETMAP,
                                                       ARG_SOUTH, "" + entry.getSouth(), 
                                                       ARG_WEST,  "" + entry.getWest(), 
                                                       ARG_NORTH, "" + entry.getNorth(), 
                                                       ARG_EAST,  "" + entry.getEast()));
                sb.append(HtmlUtil.formEntry("Area:", img));
            }

            if (showResource && entry.getResource().isImage()) {
                sb.append(HtmlUtil.formEntry("Image:",
                        HtmlUtil.img(HtmlUtil.url(repository.URL_ENTRY_GET
                            + "/" + entry.getName(), ARG_ID,
                                entry.getId()), "",
                                    XmlUtil.attr(ARG_WIDTH, "400"))));
            }

        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {}
        return sb;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeNewEntry(Entry entry) throws Exception {}

    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     */
    private String fmt(long dttm) {
        return "" + new Date(dttm);
    }


    /**
     * _more_
     *
     * @param longName _more_
     *
     * @return _more_
     */
    public List<TwoFacedObject> getListTypes(boolean longName) {
        return new ArrayList<TwoFacedObject>();

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
    public Result processList(Request request, String what) throws Exception {
        return new Result("Error",
                          new StringBuffer("Unknown listing type:" + what));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTableName() {
        return TABLE_ENTRIES;
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
    protected Statement executeSelect(Request request, String what)
            throws Exception {
        return executeSelect(request, what, assembleWhereClause(request));
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param what _more_
     * @param where _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement executeSelect(Request request, String what,
                                      List where)
            throws Exception {
        return executeSelect(request, what, where, "");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     * @param whereList _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement executeSelect(Request request, String what,
                                      List whereList, String extra)
            throws Exception {
        whereList = new ArrayList(whereList);
        String   where      = SqlUtil.makeAnd(whereList);

        String[] tableNames = {
            TABLE_ENTRIES, getTableName(), TABLE_METADATA, TABLE_USERS,
            TABLE_GROUPS, TABLE_TAGS, TABLE_ASSOCIATIONS
        };
        List    tables     = new ArrayList();
        boolean didEntries = false;
        boolean didOther   = false;
        boolean didMeta    = false;
        for (int i = 0; i < tableNames.length; i++) {
            String pattern = ".*[ =\\(]+" + tableNames[i]+"\\..*";
            if (what.matches(pattern)
                || where.matches(pattern)
                || (extra.matches(pattern))) {
                tables.add(tableNames[i]);
                if (i == 0) {
                    didEntries = true;
                } else if (i == 1) {
                    didOther = true;
                } else if (i == 2) {
                    didMeta = true;
                }
            }
        }

        if (didMeta) {
            whereList.add(SqlUtil.eq(COL_METADATA_ID, COL_ENTRIES_ID));
            didEntries = true;
        }

        if (didEntries) {
            String type = (String) request.getType("").trim();
            if ((type.length() > 0) && !type.equals(TYPE_ANY)) {
                if (whereList.toString().indexOf(COL_ENTRIES_TYPE) < 0) {
                    addOr(COL_ENTRIES_TYPE, type, whereList, true);
                }
            }
        }


        //The join
        if (didEntries && didOther
                && !TABLE_ENTRIES.equalsIgnoreCase(getTableName())) {
            whereList.add(0, SqlUtil.eq(COL_ENTRIES_ID,
                                        getTableName() + ".id"));
        }


        where = SqlUtil.makeAnd(whereList);
        String sql = SqlUtil.makeSelect(what, tables, where, extra);
        //        System.err.println (sql);
        return getRepository().execute(sql, repository.getMax(request));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected Repository getRepository() {
        return repository;
    }

    public void addToEntryForm(Request request, StringBuffer formBuffer, Entry entry)
            throws Exception {

    }


    /**
     * _more_
     *
     * @param formBuffer _more_
     * @param headerBuffer _more_
     * @param request _more_
     * @param where _more_
     * @param simpleForm _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer formBuffer,
                                StringBuffer headerBuffer, 
                                List where, boolean simpleForm)
            throws Exception {

        String minDate = request.getDateSelect(ARG_FROMDATE, (String) null);
        String maxDate = request.getDateSelect(ARG_TODATE, (String) null);
        List<TypeHandler> typeHandlers = repository.getTypeHandlers(request);
        if ((typeHandlers.size() == 0) && request.defined(ARG_TYPE)) {
            typeHandlers.add(repository.getTypeHandler(request));
        }

        /*

        System.err.println("th:" + typeHandlers);
        if(typeHandlers.size()==1 && typeHandlers.get(0)!=this) {
            TypeHandler otherTypeHandler = typeHandlers.get(0);
            otherTypeHandler.addToSearchForm(formBuffer, headerBuffer, request, where);
            return;
        }
        */


        /*
        if(minDate==null || maxDate == null) {
            Statement stmt = executeSelect(request,
                                           SqlUtil.comma(
                                                         SqlUtil.min(COL_ENTRIES_FROMDATE),
                                                         SqlUtil.max(
                                                                     COL_ENTRIES_TODATE)), where);

            ResultSet dateResults = stmt.getResultSet();
            if (dateResults.next()) {
                if (dateResults.getDate(1) != null) {
                    if(minDate == null)
                        minDate = SqlUtil.getDateString("" + dateResults.getDate(1));
                    if(maxDate == null)
                        maxDate = SqlUtil.getDateString("" + dateResults.getDate(2));
                }
            }
            }
*/

        minDate = "";
        maxDate = "";

        if (typeHandlers.size() > 1) {
            List tmp = new ArrayList();
            for (TypeHandler typeHandler : typeHandlers) {
            System.err.println(typeHandler + " " + typeHandler.getClass().getName());
                tmp.add(new TwoFacedObject(typeHandler.getLabel(),
                                           typeHandler.getType()));
            }
            TwoFacedObject anyTfo = new TwoFacedObject(TYPE_ANY, TYPE_ANY);
            if (!tmp.contains(anyTfo)) {
                tmp.add(0, anyTfo);
            }
            String typeSelect = HtmlUtil.select(ARG_TYPE, tmp);
            formBuffer.append(
                HtmlUtil.formEntry(
                    "Entry Type:",
                    typeSelect + " "
                    + HtmlUtil.submitImage(
                        repository.fileUrl("/Search16.gif"), "submit_type",
                        "Show search form with this type")));
        } else if (typeHandlers.size() == 1) {
            formBuffer.append(HtmlUtil.hidden(ARG_TYPE,
                    typeHandlers.get(0).getType()));
            //            System.err.println("type handler: "
            //                               + typeHandlers.get(0).getDescription() + " "
            //                               + typeHandlers.get(0).getType());
            formBuffer.append(HtmlUtil.formEntry("Entry Type:",
                    typeHandlers.get(0).getDescription()));
        }
        formBuffer.append("\n");


        String name = (String) request.getString(ARG_NAME, "");
        String searchMetaData = " "
                                + HtmlUtil.checkbox(ARG_SEARCHMETADATA,
                                    "true",
                                    request.get(ARG_SEARCHMETADATA,
                                        false)) + " Search metadata";
        if (name.trim().length() == 0) {
            formBuffer.append(HtmlUtil.formEntry("Name:",
                    HtmlUtil.input(ARG_NAME) + searchMetaData));
        } else {
            HtmlUtil.hidden(ARG_NAME, name);
            formBuffer.append(HtmlUtil.formEntry("Name:",
                    name + searchMetaData));
        }
        formBuffer.append("\n");


        if ( !simpleForm) {
            String groupArg = (String) request.getString(ARG_GROUP, "");
            String searchChildren = " "
                                    + HtmlUtil.checkbox(ARG_GROUP_CHILDREN,
                                        "true",
                                        request.get(ARG_GROUP_CHILDREN,
                                            false)) + " (Search subgroups)";
            if (groupArg.length() > 0) {
                formBuffer.append(HtmlUtil.hidden(ARG_GROUP, groupArg));
                Group group = repository.findGroup(groupArg);
                if (group != null) {
                    formBuffer.append(
                        HtmlUtil.formEntry(
                            "Group:",
                            group.getFullName() + "&nbsp;" + searchChildren));

                }
            } else {
                Statement stmt = executeSelect(request,
                                     SqlUtil.distinct(COL_ENTRIES_GROUP_ID),
                                     where);

                List<Group> groups =
                    repository.getGroups(SqlUtil.readString(stmt, 1));

                if (groups.size() > 1) {
                    List groupList = new ArrayList();
                    groupList.add(ALL_OBJECT);
                    for (Group group : groups) {
                        groupList.add(
                            new TwoFacedObject(group.getFullName()));
                    }
                    String groupSelect = HtmlUtil.select(ARG_GROUP,
                                             groupList);
                    formBuffer.append(
                        HtmlUtil.formEntry("Group:",
                            groupSelect + searchChildren));
                } else if (groups.size() == 1) {
                    formBuffer.append(HtmlUtil.hidden(ARG_GROUP,
                            groups.get(0).getFullName()));
                    formBuffer.append(
                        HtmlUtil.formEntry("Group:",
                            groups.get(0).getFullName() + searchChildren));
                }
            }
            formBuffer.append("\n");
        }

        if ( !simpleForm) {
            String tag = (String) request.getString(ARG_TAG, "");
            formBuffer.append(HtmlUtil.formEntry("Tag:",
                    HtmlUtil.input(ARG_TAG, tag)));

            formBuffer.append("\n");
        }

        String dateHelp = " (e.g., 2007-12-11 00:00:00)";
        formBuffer.append(HtmlUtil.formEntry("Date Range:",
                HtmlUtil.input(ARG_FROMDATE, minDate) + " -- "
                + HtmlUtil.input(ARG_TODATE, maxDate) + dateHelp));

        formBuffer.append("\n");


        if ( !simpleForm) {
            String nonGeo =
                HtmlUtil.checkbox(ARG_INCLUDENONGEO, "true",
                                  request.get(ARG_INCLUDENONGEO,
                                      true)) + " Include non-geographic";
            String areaWidget = HtmlUtil.makeLatLonBox(ARG_AREA, "", "", "",
                                    "");
            areaWidget = "<table>" + HtmlUtil.cols(areaWidget, nonGeo)
                         + "</table>";
            //            formBuffer.append(HtmlUtil.formEntry("Extent:", areaWidget+"\n"+HtmlUtil.img(repository.URL_GETMAP.toString(),"map"," name=\"map\"  xxxonmouseover = \"mouseMove()\"")));
            formBuffer.append(HtmlUtil.formEntry("Extent:",
                    areaWidget));
            formBuffer.append("\n");

        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isAnyHandler() {
        return getType().equals(TypeHandler.TYPE_ANY);
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
    protected List assembleWhereClause(Request request) throws Exception {

        List where = new ArrayList();


        if (request.defined(ARG_TAG)) {
            String tag = (String) request.getString(ARG_TAG,
                             (String) null).trim();
            where.add(SqlUtil.eq(COL_ENTRIES_ID, COL_TAGS_ENTRY_ID));
            addOr(COL_TAGS_NAME, tag, where, true);
        }

        if (request.defined(ARG_GROUP)) {
            String groupName = (String) request.getString(ARG_GROUP,
                                   "").trim();
            boolean doNot = groupName.startsWith("!");
            if (doNot) {
                groupName = groupName.substring(1);
            }
            if (groupName.endsWith("%")) {
                //                where.add(SqlUtil.eq(COL_GROUPS_ID,ENTRIES_GROUP_ID));
                where.add(SqlUtil.like(COL_ENTRIES_GROUP_ID, groupName));
            } else {
                Group group = repository.findGroupFromName(groupName);
                if (group == null) {
                    throw new IllegalArgumentException(
                        "Could not find group:" + groupName);
                }
                String searchChildren =
                    (String) request.getString(ARG_GROUP_CHILDREN,
                        (String) null);
                if (Misc.equals(searchChildren, "true")) {
                    String sub = (doNot
                                  ? SqlUtil.notLike(COL_ENTRIES_GROUP_ID,
                                      group.getId() + Group.IDDELIMITER + "%")
                                  : SqlUtil.like(COL_ENTRIES_GROUP_ID,
                                      group.getId() + Group.IDDELIMITER
                                      + "%"));
                    String equals = (doNot
                                     ? SqlUtil.neq(COL_ENTRIES_GROUP_ID,
                                         SqlUtil.quote(group.getId()))
                                     : SqlUtil.eq(COL_ENTRIES_GROUP_ID,
                                         SqlUtil.quote(group.getId())));
                    where.add("(" + sub + " OR " + equals + ")");
                } else {
                    if (doNot) {
                        where.add(SqlUtil.neq(COL_ENTRIES_GROUP_ID,
                                SqlUtil.quote(group.getId())));
                    } else {
                        where.add(SqlUtil.eq(COL_ENTRIES_GROUP_ID,
                                             SqlUtil.quote(group.getId())));
                    }
                }
            }
        }

        Date[]dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE, new Date());
        if (dateRange[0] != null) {
            where.add(SqlUtil.ge(COL_ENTRIES_FROMDATE, dateRange[0]));
        }


        if (dateRange[1] != null) {
            where.add(SqlUtil.le(COL_ENTRIES_TODATE, dateRange[1]));
        }


        Date createDate = request.get(ARG_CREATEDATE, (Date) null);
        if (createDate != null) {
            where.add(SqlUtil.le(COL_ENTRIES_CREATEDATE, createDate));
        }


        boolean includeNonGeo   = request.get(ARG_INCLUDENONGEO, false);
        List    areaExpressions = new ArrayList();
        if (request.defined(ARG_AREA + "_south")) {
            areaExpressions.add(SqlUtil.ge(COL_ENTRIES_SOUTH,
                                           request.get(ARG_AREA + "_south",
                                               0.0)));
        }
        if (request.defined(ARG_AREA + "_north")) {
            areaExpressions.add(SqlUtil.le(COL_ENTRIES_NORTH,
                                           request.get(ARG_AREA + "_north",
                                               0.0)));
        }
        if (request.defined(ARG_AREA + "_east")) {
            areaExpressions.add(SqlUtil.ge(COL_ENTRIES_EAST,
                                           request.get(ARG_AREA + "_east",
                                               0.0)));
        }
        if (request.defined(ARG_AREA + "_west")) {
            areaExpressions.add(SqlUtil.le(COL_ENTRIES_WEST,
                                           request.get(ARG_AREA + "_west",
                                               0.0)));
        }
        if (areaExpressions.size() > 0) {
            String areaExpr = SqlUtil.group(SqlUtil.makeAnd(areaExpressions));
            if (includeNonGeo) {
                areaExpr = SqlUtil.group(areaExpr + " OR "
                                         + SqlUtil.eq(COL_ENTRIES_SOUTH,
                                             Entry.NONGEO));

            }
            where.add(areaExpr);
            //            System.err.println (areaExpr);
        }


        String name = (String) request.getString(ARG_NAME, "").trim();
        if ((name != null) && (name.length() > 0)) {
            List ors = new ArrayList();
            ors.add(SqlUtil.makeOrSplit(COL_ENTRIES_NAME, name, true));
            ors.add(SqlUtil.makeOrSplit(COL_ENTRIES_DESCRIPTION, name, true));
            if (request.get(ARG_SEARCHMETADATA, false)) {
                ors.add(SqlUtil.makeOrSplit(COL_METADATA_CONTENT, name,
                                            true));
            }
            where.add("(" + StringUtil.join(" OR ", ors) + ")");
        }
        //        System.err.println("where:" + where);
        return where;

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param stmt _more_
     *
     * @throws Exception _more_
     */
    public void setStatement(Entry entry, PreparedStatement stmt, boolean isNew)
            throws Exception {}

    /**
     * _more_
     *
     * @return _more_
     */
    public String getInsertSql(boolean isNew) {
        return null;
    }

    /**
     * _more_
     *
     * @param requess _more_
     * @param statement _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request requess, Statement statement, Entry entry)
            throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    protected List getTablesForQuery(Request request) {
        return getTablesForQuery(request, new ArrayList());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param initTables _more_
     *
     * @return _more_
     */
    protected List getTablesForQuery(Request request, List initTables) {
        initTables.add(TABLE_ENTRIES);

        if (request.hasSetParameter(ARG_TAG)) {
            initTables.add(TABLE_TAGS);
            initTables.add(TABLE_ENTRIES);
        }
        return initTables;
    }



    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     * @param list _more_
     * @param quoteThem _more_
     *
     * @return _more_
     */
    protected boolean addOr(String column, String value, List list,
                            boolean quoteThem) {
        if ((value != null) && (value.trim().length() > 0)
                && !value.toLowerCase().equals("all")) {
            list.add("(" + SqlUtil.makeOrSplit(column, value, quoteThem)
                     + ")");
            return true;
        }
        return false;
    }




    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        if ((description == null) || (description.trim().length() == 0)) {
            return getType();
        }
        return description;
    }

    /**
     * _more_
     *
     * @param arg _more_
     * @param value _more_
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public int matchValue(String arg, Object value, Request request,
                          Entry entry) {
        return MATCH_UNKNOWN;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return type + " " + description;
    }


}


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

import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
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
public class TypeHandler extends RepositoryManager {

    /** _more_ */
    public static final String TAG_COLUMN = "column";

    /** _more_ */
    public static final String TAG_PROPERTY = "property";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_DATATYPE = "datatype";

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_HANDLER = "handler";




    /** _more_ */
    public static final int MATCH_UNKNOWN = 0;

    /** _more_ */
    public static final int MATCH_TRUE = 1;

    /** _more_ */
    public static final int MATCH_FALSE = 2;


    /** _more_ */
    public static final TwoFacedObject ALL_OBJECT = new TwoFacedObject("All",
                                                        "");

    /** _more_ */
    public static final TwoFacedObject NONE_OBJECT =
        new TwoFacedObject("None", "");

    /** _more_ */
    public static final String TYPE_ANY = "any";

    /** _more_ */
    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_GROUP = "group";


    /** _more_ */
    String type;

    /** _more_ */
    String description;


    /** _more_ */
    private Hashtable dontShowInForm = new Hashtable();

    /** _more_ */
    private Hashtable properties = new Hashtable();

    /** _more_ */
    private String defaultDataType;

    /** _more_          */
    private String displayTemplatePath;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public TypeHandler(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     */
    public TypeHandler(Repository repository, Element entryNode) {
        this(repository);
        displayTemplatePath = XmlUtil.getAttribute(entryNode,
                "displaytemplate", (String) null);
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
        super(repository);
        this.type        = type;
        this.description = description;

    }



    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getProperty(String name) {
        return (String) properties.get(name);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String name, String dflt) {
        return Misc.getProperty(properties, name, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getProperty(String name, int dflt) {
        return Misc.getProperty(properties, name, dflt);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String name, boolean dflt) {
        return Misc.getProperty(properties, name, dflt);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public void putProperty(String name, String value) {
        properties.put(name, value);
    }


    /** _more_ */
    static int cnt = 0;

    /** _more_ */
    int mycnt = cnt++;



    /**
     * _more_
     *
     * @param arg _more_
     *
     * @return _more_
     */
    public boolean okToShowInForm(String arg) {
        return getProperty("form.show." + arg, true);
    }

    /**
     * _more_
     *
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getFormDefault(String arg, String dflt) {
        String prop = getProperty("form.default." + arg);
        if (prop == null) {
            return dflt;
        }
        return prop;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        return new Entry(id, this);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntry(Request request, Entry entry)
            throws Exception {}






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
    public final Entry getEntry(ResultSet results) throws Exception {
        return getEntry(results, false);
    }


    /**
     * _more_
     *
     * @param results _more_
     * @param abbreviated _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntry(ResultSet results, boolean abbreviated)
            throws Exception {
        //id,type,name,desc,group,topGroup, user,file,createdata,fromdate,todate
        int    col   = 3;
        String id    = results.getString(1);
        Entry  entry = createEntry(id);
        entry.initEntry(
            results.getString(col++), results.getString(col++),
            getRepository().findGroup(results.getString(col++)),
            results.getString(col++),
            getUserManager().findUser(results.getString(col++), true),
            new Resource(results.getString(col++), results.getString(col++)),
            results.getString(col++),
            results.getTimestamp(col++, getRepository().calendar).getTime(),
            results.getTimestamp(col++, getRepository().calendar).getTime(),
            results.getTimestamp(col++, getRepository().calendar).getTime(),
            null);
        entry.setSouth(results.getDouble(col++));
        entry.setNorth(results.getDouble(col++));
        entry.setEast(results.getDouble(col++));
        entry.setWest(results.getDouble(col++));
        return entry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param html _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String processDisplayTemplate(Request request, Entry entry,
                                            String html)
            throws Exception {
        html = html.replace("${" + ARG_NAME + "}", entry.getName());
        html = html.replace("${" + ARG_LABEL + "}", entry.getLabel());
        html = html.replace("${" + ARG_DESCRIPTION + "}",
                            entry.getDescription());
        html = html.replace("${" + ARG_CREATEDATE + "}",
                            formatDate(request, entry.getCreateDate()));
        html = html.replace("${" + ARG_FROMDATE + "}",
                            formatDate(request, entry.getStartDate()));
        html = html.replace("${" + ARG_TODATE + "}",
                            formatDate(request, entry.getEndDate()));
        html = html.replace("${" + ARG_CREATOR + "}",
                            entry.getUser().getLabel());

        return html;
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
            if (displayTemplatePath != null) {
                String html =
                    getRepository().getResource(displayTemplatePath);
                return new StringBuffer(processDisplayTemplate(request,
                        entry, html));
            }
            sb.append("<table cellspacing=\"5\" cellpadding=\"2\">");
            sb.append(getInnerEntryContent(entry, request, output,
                                           showResource,true));


            /*
            List<Metadata> metadataList = getRepository().getMetadata(entry);
            if (metadataList.size() > 0) {
                sb.append(HtmlUtil.formEntry("<p>", ""));
                StringBuffer mSB = new StringBuffer();
                mSB.append("<ul>");
                for (Metadata metadata : metadataList) {
                    mSB.append("<li>");
                    if (metadata.getType().equals(Metadata.TYPE_LINK)) {
                        mSB.append(metadata.getAttr1() + ": ");
                        mSB.append(HtmlUtil.href(metadata.getAttr2(),
                                metadata.getAttr3()));
                    } else {
                        mSB.append(metadata.getAttr1());
                        mSB.append(" ");
                        mSB.append(metadata.getAttr2());
                    }
                }
                mSB.append("</ul>");
                sb.append(HtmlUtil.formEntry(msgLabel("Metadata"), mSB.toString()));
            }
            */

            sb.append("</table>\n");


        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {}
        return sb;

    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links, boolean forMenu)
            throws Exception {

        if (getAccessManager().canEditEntry(request, entry)) {
            links.add(
                new Link(
                    request.entryUrl(getRepository().URL_ENTRY_FORM, entry),
                    getRepository().fileUrl(ICON_EDIT), msg("Edit Entry")));
            if(forMenu) {
                links.add(
                          new Link(
                                   request.entryUrl(getMetadataManager().URL_METADATA_FORM, entry),
                                   getRepository().fileUrl(ICON_METADATA), msg("Edit Metadata")));
                
            }
        }

        if (forMenu && getAccessManager().canDoAction(request, entry,
                                           Permission.ACTION_DELETE)) {
            links.add(
                new Link(
                    request.entryUrl(getRepository().URL_ENTRY_DELETE, entry),
                    getRepository().fileUrl(ICON_DELETE), msg("Delete Entry")));

        }            


        Link downloadLink = getEntryDownloadLink(request, entry);
        if (downloadLink != null) {
            links.add(downloadLink);
        }
        links.add(
            new Link(
                request.entryUrl(getRepository().URL_COMMENTS_SHOW, entry),
                getRepository().fileUrl(ICON_COMMENTS),
                msg("Add/View Comments")));

        if ( !request.getUser().getAnonymous()) {
            links.add(
                new Link(
                    request.entryUrl(getRepository().URL_ENTRY_COPY, entry, ARG_FROM),
                    getRepository().fileUrl(ICON_MOVE),
                    msg("Copy/Move Entry")));
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
    protected Link getEntryDownloadLink(Request request, Entry entry)
            throws Exception {

        if ( !getAccessManager().canDownload(request, entry)) {
            return null;
        }
        File   f    = entry.getResource().getFile();
        String size = " (" + f.length() + " bytes)";
        if (getRepository().getProperty(PROP_DOWNLOAD_ASFILES, false)) {
            return new Link("file://" + entry.getResource(),
                            getRepository().fileUrl(ICON_FETCH),
                            "Download file" + size);
        } else {
            return new Link(HtmlUtil
                .url(request.url(getRepository().URL_ENTRY_GET) + "/"
                     + entry.getName(), ARG_ID, entry
                         .getId()), getRepository()
                             .fileUrl(ICON_FETCH), "Download file" + size);
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
                                             boolean showResource, boolean showMap)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            OutputHandler outputHandler =
                getRepository().getOutputHandler(request);
            String nextPrev = StringUtil.join(HtmlUtil.space(1),
                                  outputHandler.getNextPrevLinks(request,
                                      entry, output));
            //            sb.append(HtmlUtil.formEntry("", nextPrev));
            //            sb.append(HtmlUtil.formEntry("<table width=100%><tr><td>" + nextPrev + "</td><td align=right>" + msgLabel("Name")+"</td></tr></table>", entry.getLabel()));
            sb.append(HtmlUtil.formEntry(msgLabel("Name"), entry.getName()));



            String desc = entry.getDescription();
            if ((desc != null) && (desc.length() > 0)) {
                sb.append(HtmlUtil.formEntry(msgLabel("Description"), getRepository().getEntryText(request, entry, desc)));
            }
            sb.append(HtmlUtil.formEntry(msgLabel("Created by"),
                                         entry.getUser().getLabel() + " @ "
                                         + formatDate(request,
                                             entry.getCreateDate())));

            String resourceLink = entry.getResource().getPath();
            if (resourceLink.length() > 0) {
                if (entry.getResource().isUrl()) {
                    resourceLink = "<a href=\"" + resourceLink + "\">"
                                   + resourceLink + "</a>";
                }
                sb.append(HtmlUtil.formEntry(msgLabel("Resource"),
                                             resourceLink));

                if (entry.isFile()) {
                    sb.append(HtmlUtil.formEntry(msgLabel("Size"),
                            entry.getResource().getFile().length()
                            + HtmlUtil.space(1) + msg("bytes")));
                }
            }

            if ((entry.getCreateDate() != entry.getStartDate())
                    || (entry.getCreateDate() != entry.getEndDate())) {
                if (entry.getEndDate() != entry.getStartDate()) {
                    sb.append(HtmlUtil.formEntry(msgLabel("Date Range"),
                            formatDate(request, entry.getStartDate())
                            + " -- "
                            + formatDate(request, entry.getEndDate())));
                } else {
                    sb.append(HtmlUtil.formEntry(msgLabel("Date"),
                            formatDate(request, entry.getStartDate())));
                }
            }
            String typeDesc = entry.getTypeHandler().getDescription();
            if ((typeDesc == null) || (typeDesc.trim().length() == 0)) {
                typeDesc = entry.getTypeHandler().getType();
            }
            sb.append(HtmlUtil.formEntry(msgLabel("Type"), typeDesc));

            if ( !entry.getTypeHandler().hasDefaultDataType()
                    && StringUtil.notEmpty(entry.getDataType())) {
                sb.append(HtmlUtil.formEntry(msgLabel("Data Type"),
                                             entry.getDataType()));
            }

            if(showMap) {
            if (entry.hasLocationDefined()) {
                sb.append(HtmlUtil.formEntry(msgLabel("Location"),
                                             entry.getSouth() + "/"
                                             + entry.getEast()));
            } else if (entry.hasAreaDefined()) {
                String img =
                    HtmlUtil.img(request.url(getRepository().URL_GETMAP,
                                             ARG_SOUTH,
                                             "" + entry.getSouth(), ARG_WEST,
                                             "" + entry.getWest(), ARG_NORTH,
                                             "" + entry.getNorth(), ARG_EAST,
                                             "" + entry.getEast()));
                sb.append(HtmlUtil.formEntry(msgLabel("Area"), img));
            }
            }

            if (showResource && entry.getResource().isImage()) {
                if (entry.getResource().isFile()
                        && getAccessManager().canDownload(request, entry)) {
                    sb.append(
                        HtmlUtil.formEntryTop(
                            msgLabel("Image"),
                            HtmlUtil.img(
                                HtmlUtil.url(
                                    request.url(
                                        getRepository().URL_ENTRY_GET) + "/"
                                            + entry.getName(), ARG_ID,
                                    entry.getId()), "","width=600")));

                } else if (entry.getResource().isUrl()) {
                    sb.append(HtmlUtil.formEntryTop(msgLabel("Image"),
                            HtmlUtil.img(entry.getResource().getPath())));
                }
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
                          new StringBuffer(msgLabel("Unknown listing type")
                                           + what));
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
     * @param s _more_
     *
     * @return _more_
     */
    private String cleanQueryString(String s) {
        s = s.replace("\r\n", " ");
        s = StringUtil.stripAndReplace(s, "'", "'", "'dummy'");
        return s;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     * @param clause _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement select(Request request, String what, Clause clause,
                               String extra)
            throws Exception {
        List<Clause> clauses = new ArrayList<Clause>();
        clauses.add(clause);
        return select(request, what, clauses, extra);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     * @param whereList _more_
     * @param clauses _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement select(Request request, String what,
                               List<Clause> clauses, String extra)
            throws Exception {
        clauses = new ArrayList<Clause>(clauses);
        //We do the replace because (for some reason) any CRNW screws up the pattern matching
        String whatString  = cleanQueryString(what);
        String extraString = cleanQueryString(extra);

        String[] tableNames = { TABLE_ENTRIES, getTableName(), TABLE_METADATA,
                                TABLE_USERS, TABLE_ASSOCIATIONS };
        //        String[] tableNames = { TABLE_ENTRIES, getTableName(), TABLE_METADATA,
        //                                TABLE_USERS, TABLE_ASSOCIATIONS };
        List    tables     = new ArrayList();
        boolean didEntries = false;
        boolean didOther   = false;
        boolean didMeta    = false;
        for (int i = 0; i < tableNames.length; i++) {
            String pattern = ".*[, =\\(]+" + tableNames[i] + "\\..*";
            //            System.out.println("pattern:" + pattern);
            if (Clause.isColumnFromTable(clauses, tableNames[i])
                    || whatString.matches(pattern)
                    || (extraString.matches(pattern))) {
                //                System.out.println("    ***** match");
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
            tables.add(TABLE_METADATA);
            didEntries = true;
        }

        int metadataCnt = 0;





        while (true) {
            String subTable = TABLE_METADATA + "_" + metadataCnt;
            metadataCnt++;
            if ( !Clause.isColumnFromTable(clauses, subTable)) {
                break;
            }
            tables.add(TABLE_METADATA + " " + subTable);
        }


        if (didEntries) {
            List typeList = request.get(ARG_TYPE, new ArrayList());
            typeList.remove(TYPE_ANY);
            if (typeList.size() > 0) {
                String typeString;
                if (request.get(ARG_TYPE_EXCLUDE, false)) {
                    typeString = "!" + StringUtil.join(",!", typeList);
                } else {
                    typeString = StringUtil.join(",", typeList);
                }


                if ( !Clause.isColumn(clauses, COL_ENTRIES_TYPE)) {
                    addOrClause(COL_ENTRIES_TYPE, typeString, clauses);
                }
            }
        }


        //The join
        if (didEntries && didOther
                && !TABLE_ENTRIES.equalsIgnoreCase(getTableName())) {
            clauses.add(0, Clause.join(COL_ENTRIES_ID, getTableName() + ".id"));
        }

        //        System.err.println("tables:" + tables);
        return SqlUtil.select(getConnection(), what, tables,
                              Clause.and(clauses), extra,
                              getRepository().getMax(request));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param sb _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, StringBuffer sb, Entry entry)
            throws Exception {

        String size = HtmlUtil.SIZE_70;
        if (okToShowInForm(ARG_NAME)) {
            sb.append(HtmlUtil.formEntry("Name:",
                                         HtmlUtil.input(ARG_NAME,
                                             ((entry != null)
                    ? entry.getName()
                    : getFormDefault(ARG_NAME, "")), size)));
        } else {
            String nameDefault = getFormDefault(ARG_NAME, null);
            if (nameDefault != null) {
                sb.append(HtmlUtil.hidden(ARG_NAME, nameDefault));
            }
        }
        int rows = getProperty("form.rows.desc", 3);
        if (okToShowInForm(ARG_DESCRIPTION)) {
            sb.append(
                HtmlUtil.formEntryTop(
                    "Description:",
                    HtmlUtil.textArea(ARG_DESCRIPTION, ((entry != null)
                    ? entry.getDescription()
                    : BLANK), rows, 50)));
        }

        if (okToShowInForm(ARG_RESOURCE)) {
            if (entry == null) {
                sb.append(
                    HtmlUtil.formEntry(
                        msgLabel("File"),
                        HtmlUtil.fileInput(ARG_FILE, size)
                        + HtmlUtil.checkbox(ARG_FILE_UNZIP, "true", false)
                        + HtmlUtil.space(1) + msg("Unzip archive")));
                String download = HtmlUtil.space(1)
                                  + HtmlUtil.checkbox(ARG_RESOURCE_DOWNLOAD,
                                      "true", false) + HtmlUtil.space(1)
                                          + msg("Download");
                sb.append(HtmlUtil.formEntry(msgLabel("Or URL"),
                                             HtmlUtil.input(ARG_RESOURCE,
                                                 BLANK, size) + download));
            } else {
                sb.append(HtmlUtil.formEntry(msgLabel("Resource"),
                                             entry.getResource().getPath()));
            }
            if ( !hasDefaultDataType() && okToShowInForm(ARG_DATATYPE)) {
                String selected = "";
                if (entry != null) {
                    selected = entry.getDataType();
                }
                List   types  = getRepository().getDefaultDataTypes();
                String widget = ((types.size() > 1)
                                 ? HtmlUtil.select(ARG_DATATYPE_SELECT,
                                     types, selected) + HtmlUtil.space(1)
                                         + msgLabel("Or")
                                 : "") + HtmlUtil.input(ARG_DATATYPE);
                sb.append(HtmlUtil.formEntry(msgLabel("Data Type"), widget));
            }

        }

        String dateHelp = " (e.g., 2007-12-11 00:00:00)";
        String fromDate = ((entry != null)
                           ? formatDate(request,
                                        new Date(entry.getStartDate()))
                           : BLANK);
        String toDate = ((entry != null)
                         ? formatDate(request, new Date(entry.getEndDate()))
                         : BLANK);
        if (okToShowInForm(ARG_DATE)) {
            if ( !okToShowInForm(ARG_TODATE)) {
                sb.append(HtmlUtil.formEntry("Date:",
                                             HtmlUtil.input(ARG_FROMDATE,
                                                 fromDate,
                                                     HtmlUtil.SIZE_30)));
            } else {
                sb.append(
                    HtmlUtil.formEntry(
                        "Date Range:",
                        HtmlUtil.input(
                            ARG_FROMDATE, fromDate,
                            HtmlUtil.SIZE_30) + " -- "
                                + HtmlUtil.input(
                                    ARG_TODATE, toDate,
                                    HtmlUtil.SIZE_30) + dateHelp));
            }
            if (entry == null) {
                List datePatterns = new ArrayList();

                datePatterns.add(new TwoFacedObject("", BLANK));
                for (int i = 0; i < DateUtil.DATE_PATTERNS.length; i++) {
                    datePatterns.add(DateUtil.DATE_FORMATS[i]);
                }

                if (okToShowInForm(ARG_RESOURCE)) {
                    sb.append(HtmlUtil.formEntry("Date Pattern:",
                            HtmlUtil.select(ARG_DATE_PATTERN, datePatterns)
                            + " (use file name)"));
                }

            }
        }


        if (okToShowInForm(ARG_AREA)) {
            sb.append(HtmlUtil.formEntry("Location:",
                                         HtmlUtil.makeLatLonBox(ARG_AREA,
                                             ((entry != null)
                                                 && entry.hasSouth())
                                             ? entry.getSouth()
                                             : Double.NaN, ((entry != null)
                                             && entry.hasNorth())
                    ? entry.getNorth()
                    : Double.NaN, ((entry != null) && entry.hasEast())
                                  ? entry.getEast()
                                  : Double.NaN, ((entry != null)
                                  && entry.hasWest())
                    ? entry.getWest()
                    : Double.NaN)));
        }


    }




    /**
     * _more_
     *
     * @param formBuffer _more_
     * @param request _more_
     * @param where _more_
     * @param advancedForm _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer formBuffer,
                                List<Clause> where, boolean advancedForm)
            throws Exception {

        List dateSelect = new ArrayList();
        dateSelect.add(new TwoFacedObject(msg("All"), "none"));
        dateSelect.add(new TwoFacedObject(msg("Last hour"), "-1 hour"));
        dateSelect.add(new TwoFacedObject(msg("Last 3 hours"), "-3 hours"));
        dateSelect.add(new TwoFacedObject(msg("Last 6 hours"), "-6 hours"));
        dateSelect.add(new TwoFacedObject(msg("Last 12 hours"), "-12 hours"));
        dateSelect.add(new TwoFacedObject(msg("Last day"), "-1 day"));
        dateSelect.add(new TwoFacedObject(msg("Last 7 days"), "-7 days"));
        dateSelect.add(new TwoFacedObject(msgLabel("Custom"), ""));
        String dateSelectValue;
        if (request.exists(ARG_RELATIVEDATE)) {
            dateSelectValue = request.getString(ARG_RELATIVEDATE, "");
        } else {
            dateSelectValue = "none";
        }

        String dateSelectInput = HtmlUtil.select(ARG_RELATIVEDATE,
                                     dateSelect, dateSelectValue);
        String minDate = request.getDateSelect(ARG_FROMDATE, (String) null);
        String maxDate = request.getDateSelect(ARG_TODATE, (String) null);

        //        request.remove(ARG_FROMDATE);
        //        request.remove(ARG_TODATE);


        List<TypeHandler> typeHandlers =
            getRepository().getTypeHandlers(request);
        //        System.err.println("handlers:" + typeHandlers);


        if (request.defined(ARG_TYPE)) {
            TypeHandler typeHandler = getRepository().getTypeHandler(request);
            if ( !typeHandler.isAnyHandler()) {
                typeHandlers.clear();
                typeHandlers.add(typeHandler);
            }
        }


        /*
        if(minDate==null || maxDate == null) {
            Statement stmt = select(request,
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


        StringBuffer basicSB    = new StringBuffer(HtmlUtil.formTable());
        StringBuffer advancedSB = new StringBuffer(HtmlUtil.formTable());



        if (typeHandlers.size() > 1) {
            List tmp = new ArrayList();
            for (TypeHandler typeHandler : typeHandlers) {
                tmp.add(new TwoFacedObject(typeHandler.getLabel(),
                                           typeHandler.getType()));
            }
            TwoFacedObject anyTfo = new TwoFacedObject(TYPE_ANY, TYPE_ANY);
            if ( !tmp.contains(anyTfo)) {
                tmp.add(0, anyTfo);
            }
            String typeSelect = HtmlUtil.select(ARG_TYPE, tmp, "",
                                    (advancedForm
                                     ? " MULTIPLE SIZE=4 "
                                     : ""));
            String groupCbx = (advancedForm
                               ? HtmlUtil.checkbox(ARG_TYPE_EXCLUDE, "true",
                                   false) + HtmlUtil.space(1) + msg("Exclude")
                               : "");
            basicSB.append(
                HtmlUtil.formEntry(
                    msgLabel("Type"),
                    typeSelect + HtmlUtil.space(1)
                    + HtmlUtil.submitImage(
                        getRepository().fileUrl(ICON_SEARCH), "submit_type",
                        msg(
                        "Show search form with this type")) + HtmlUtil.space(
                            1) + groupCbx));
        } else if (typeHandlers.size() == 1) {
            basicSB.append(HtmlUtil.hidden(ARG_TYPE,
                                           typeHandlers.get(0).getType()));
            basicSB.append(HtmlUtil.formEntry(msgLabel("Type"),
                    typeHandlers.get(0).getDescription()));
        }



        List<Group> collectionGroups = getRepository().getTopGroups(request);
        List<TwoFacedObject> collections = new ArrayList<TwoFacedObject>();
        collections.add(new TwoFacedObject("All",""));
        for(Group group: collectionGroups) {
            collections.add(
                          new TwoFacedObject(group.getLabel(), group.getId()));
            
        }

        Entry collection =  request.getCollectionEntry();
        String collectionSelect = HtmlUtil.select(ARG_COLLECTION,
                                                 collections, (collection!=null?collection.getId():null),100);
        advancedSB.append(HtmlUtil.formEntry(msgLabel("Collection"),
                                          collectionSelect));


        String name = (String) request.getString(ARG_TEXT, "");
        String searchMetaData = " "
                                + HtmlUtil.checkbox(ARG_SEARCHMETADATA,
                                    "true",
                                    request.get(ARG_SEARCHMETADATA,
                                        false)) + " "
                                            + msg("Search metadata");

        String searchExact = " "
                             + HtmlUtil.checkbox(ARG_EXACT, "true",
                                 request.get(ARG_EXACT, false)) + " "
                                     + msg("Match exactly");
        if (name.trim().length() == 0) {
            basicSB.append(HtmlUtil.formEntry(msgLabel("Text"),
                    HtmlUtil.input(ARG_TEXT) + searchExact + searchMetaData));
        } else {
            HtmlUtil.hidden(ARG_TEXT, name);
            basicSB.append(HtmlUtil.formEntry(msgLabel("Name"),
                    name + searchExact + searchMetaData));
        }
        basicSB.append("\n");


        String dateHelp = " (e.g., 2007-12-11 00:00:00)";

        basicSB.append(
            HtmlUtil.formEntry(
                msgLabel("Date Range"),
                dateSelectInput + HtmlUtil.space(1)
                + HtmlUtil.input(ARG_FROMDATE, minDate) + " -- "
                + HtmlUtil.input(ARG_TODATE, maxDate) + dateHelp));


        if (advancedForm || request.defined(ARG_GROUP)) {
            String groupArg = (String) request.getString(ARG_GROUP, "");
            String searchChildren = " "
                                    + HtmlUtil.checkbox(ARG_GROUP_CHILDREN,
                                        "true",
                                        request.get(ARG_GROUP_CHILDREN,
                                            false)) + " ("
                                                + msg("Search subgroups")
                                                + ")";
            if (groupArg.length() > 0) {
                advancedSB.append(HtmlUtil.hidden(ARG_GROUP, groupArg));
                Group group = getRepository().findGroup(groupArg);
                if (group != null) {
                    advancedSB.append(HtmlUtil.formEntry(msgLabel("Group"),
                            group.getFullName() + "&nbsp;" + searchChildren));

                }
            } else {
                /****
                Statement stmt =
                    select(request,
                           SqlUtil.distinct(COL_ENTRIES_PARENT_GROUP_ID),
                           where, "");

                List<Group> groups =
                    getRepository().getGroups(SqlUtil.readString(stmt, 1));
                stmt.close();

                if (groups.size() > 1) {
                    List groupList = new ArrayList();
                    groupList.add(ALL_OBJECT);
                    for (Group group : groups) {
                        groupList.add(
                            new TwoFacedObject(group.getFullName(), group.getId()));
                    }
                    String groupSelect = HtmlUtil.select(ARG_GROUP,
                                             groupList, null, 100);
                    advancedSB.append(HtmlUtil.formEntry(msgLabel("Group"),
                            groupSelect + searchChildren));
                } else if (groups.size() == 1) {
                    advancedSB.append(HtmlUtil.hidden(ARG_GROUP,
                            groups.get(0).getId()));
                    advancedSB.append(HtmlUtil.formEntry(msgLabel("Group"),
                            groups.get(0).getFullName() + searchChildren));
                }
                ****/
            }
            advancedSB.append("\n");
        }


        if (advancedForm) {
            String nonGeo =
                HtmlUtil.checkbox(ARG_INCLUDENONGEO, "true",
                                  request.get(ARG_INCLUDENONGEO,
                                      true)) + " Include non-geographic";
            String areaWidget = HtmlUtil.makeLatLonBox(ARG_AREA, "", "", "",
                                    "");
            areaWidget = "<table>" + HtmlUtil.cols(areaWidget) + "</table>";
            //            formBuffer.append(HtmlUtil.formEntry("Extent:", areaWidget+"\n"+HtmlUtil.img(request.url(getRepository().URL_GETMAP),"map"," name=\"map\"  xxxonmouseover = \"mouseMove()\"")));
            advancedSB.append(HtmlUtil.formEntry(msgLabel("Extent"),
                    areaWidget));
            advancedSB.append("\n");

        }


        basicSB.append(HtmlUtil.formTableClose());
        advancedSB.append(HtmlUtil.formTableClose());


        formBuffer.append(getRepository().makeShowHideBlock(request,
                "search.basic", msg("Basic"), basicSB, true));
        formBuffer.append(getRepository().makeShowHideBlock(request,
                "search.advanced", msg("Advanced"), advancedSB, false));
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
    protected List<Clause> assembleWhereClause(Request request)
            throws Exception {

        List<Clause> where = new ArrayList<Clause>();


        if (request.defined(ARG_RESOURCE)) {
            addOrClause(COL_ENTRIES_RESOURCE,
                        request.getString(ARG_RESOURCE, ""), where);
        }

        if (request.defined(ARG_DATATYPE)) {
            addOrClause(COL_ENTRIES_DATATYPE,
                        request.getString(ARG_DATATYPE, ""), where);
        }

        if (request.defined(ARG_USER_ID)) {
            addOrClause(COL_ENTRIES_USER_ID,
                        request.getString(ARG_USER_ID, ""), where);
        }

        if (request.defined(ARG_COLLECTION)) {
            addOrClause(COL_ENTRIES_TOP_GROUP_ID,
                        request.getString(ARG_COLLECTION, ""), where);
        }

        if (request.defined(ARG_GROUP)) {
            String groupId = (String) request.getString(ARG_GROUP,
                                   "").trim();
            boolean doNot = groupId.startsWith("!");
            if (doNot) {
                groupId = groupId.substring(1);
            }
            if (groupId.endsWith("%")) {
                where.add(Clause.like(COL_ENTRIES_PARENT_GROUP_ID,
                                      groupId));
            } else {
                Group group = getRepository().findGroup(request);
                if (group == null) {
                    throw new IllegalArgumentException(
                        msgLabel("Could not find group") + groupId);
                }
                String searchChildren =
                    (String) request.getString(ARG_GROUP_CHILDREN,
                        (String) null);
                if (Misc.equals(searchChildren, "true")) {
                    Clause sub = (doNot
                                  ? Clause.notLike(
                                      COL_ENTRIES_PARENT_GROUP_ID,
                                      group.getId() + Group.IDDELIMITER + "%")
                                  : Clause.like(COL_ENTRIES_PARENT_GROUP_ID,
                                      group.getId() + Group.IDDELIMITER
                                      + "%"));
                    Clause equals = (doNot
                                     ? Clause.neq(
                                         COL_ENTRIES_PARENT_GROUP_ID,
                                         group.getId())
                                     : Clause.eq(COL_ENTRIES_PARENT_GROUP_ID,
                                         group.getId()));
                    where.add(Clause.or(sub, equals));
                } else {
                    if (doNot) {
                        where.add(Clause.neq(COL_ENTRIES_PARENT_GROUP_ID,
                                             group.getId()));
                    } else {
                        where.add(Clause.eq(COL_ENTRIES_PARENT_GROUP_ID,
                                            group.getId()));
                    }
                }
            }
        }



        Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE,
                               new Date());
        if (dateRange[0] != null) {
            where.add(Clause.ge(COL_ENTRIES_FROMDATE, dateRange[0]));
        }


        if (dateRange[1] != null) {
            where.add(Clause.le(COL_ENTRIES_TODATE, dateRange[1]));
        }


        Date createDate = request.get(ARG_CREATEDATE, (Date) null);
        if (createDate != null) {
            where.add(Clause.le(COL_ENTRIES_CREATEDATE, createDate));
        }


        boolean includeNonGeo   = request.get(ARG_INCLUDENONGEO, false);
        List<Clause>    areaExpressions = new ArrayList<Clause>();
        if (request.defined(ARG_AREA + "_south")) {
            areaExpressions.add(Clause.and(Clause.neq(COL_ENTRIES_SOUTH,new Double(Entry.NONGEO)),
                                           Clause.ge(COL_ENTRIES_SOUTH,
                                                     request.get(ARG_AREA + "_south",
                                                                 0.0))));
        }
        if (request.defined(ARG_AREA + "_north")) {
            areaExpressions.add(Clause.and(Clause.neq(COL_ENTRIES_NORTH,new Double(Entry.NONGEO)),
                                           Clause.le(COL_ENTRIES_NORTH,
                                           request.get(ARG_AREA + "_north",
                                               0.0))));
        }
        if (request.defined(ARG_AREA + "_east")) {
            areaExpressions.add(Clause.and(Clause.neq(COL_ENTRIES_EAST,new Double(Entry.NONGEO)),
                                           Clause.le(COL_ENTRIES_EAST,
                                           request.get(ARG_AREA + "_east",
                                               0.0))));
        }
        if (request.defined(ARG_AREA + "_west")) {
            areaExpressions.add(Clause.and(Clause.neq(COL_ENTRIES_WEST,new Double(Entry.NONGEO)),
                                           Clause.ge(COL_ENTRIES_WEST, request.get(ARG_AREA + "_west",
                                                    0.0))));

        }
        if (areaExpressions.size() > 0) {
            Clause areaExpr = Clause.and(areaExpressions);
            if (includeNonGeo) {
                areaExpr = Clause.or(areaExpr,
                                     Clause.eq(COL_ENTRIES_SOUTH,
                                         new Double(Entry.NONGEO)));
            }
            where.add(areaExpr);
            //            System.err.println (areaExpr);
        }


        Hashtable args           = request.getArgs();
        String    metadataPrefix = ARG_METADATA_TYPE + ".";
        Hashtable typeMap        = new Hashtable();
        List      types          = new ArrayList();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(metadataPrefix)) {
                continue;
            }
            if ( !request.defined(arg)) {
                continue;
            }
            String type = request.getString(arg, "");
            if ( !request.defined(ARG_METADATA_ATTR1 + "." + type)
                    && !request.defined(ARG_METADATA_ATTR2 + "." + type)
                    && !request.defined(ARG_METADATA_ATTR3 + "." + type)
                    && !request.defined(ARG_METADATA_ATTR4 + "." + type)) {
                continue;
            }

            Metadata metadata =
                new Metadata(
                    type,
                    request.getString(ARG_METADATA_ATTR1 + "." + type, ""),
                    request.getString(ARG_METADATA_ATTR2 + "." + type, ""),
                    request.getString(ARG_METADATA_ATTR3 + "." + type, ""),
                    request.getString(ARG_METADATA_ATTR4 + "." + type, ""));

            metadata.setInherited(request.get(ARG_METADATA_INHERITED + "."
                    + type, false));
            List values = (List) typeMap.get(type);
            if (values == null) {
                typeMap.put(type, values = new ArrayList());
                types.add(type);
            }
            values.add(metadata);
        }
        List<Clause> metadataAnds = new ArrayList<Clause>();
        for (int typeIdx = 0; typeIdx < types.size(); typeIdx++) {
            String type        = (String) types.get(typeIdx);
            List   values      = (List) typeMap.get(type);
            List<Clause>   metadataOrs = new ArrayList<Clause>();
            String subTable    = TABLE_METADATA + "_" + typeIdx;
            for (int i = 0; i < values.size(); i++) {
                Metadata metadata = (Metadata) values.get(i);
                Clause clause =
                    Clause.and(
                               new Clause[]{
                                   Clause.join(
                                             subTable + ".entry_id",
                                             COL_ENTRIES_ID), 
                                   Clause.eq(
                                             subTable + ".attr1",
                                             metadata.getAttr1()), 
                                   Clause.eq(
                                             subTable + ".type",
                                             type)});
                /***TODO
                if (metadata.getInherited()) {
                    String subselect =
                        SqlUtil.makeSelect(
                            "metadata.entry_id", TABLE_METADATA,
                            SqlUtil.makeAnd(
                                SqlUtil.like(
                                    COL_ENTRIES_PARENT_GROUP_ID,
                                    COL_METADATA_ENTRY_ID), SqlUtil
                                        .eq(
                                        "metadata.attr1",
                                        SqlUtil.quote(
                                            metadata.getAttr1())), SqlUtil
                                                .eq(
                                                "metadata.type",
                                                SqlUtil.quote(
                                                    metadata.getType()
                                                        .toString()))));

                    String inheritedClause = COL_ENTRIES_PARENT_GROUP_ID
                                             + " LIKE "
                                             + SqlUtil.group(subselect)
                                             + " ||'%'";
                    clause = SqlUtil.group(
                        SqlUtil.makeOr(
                            Misc.newList(
                                SqlUtil.group(clause),
                                SqlUtil.group(inheritedClause))));
                    //                clause = SqlUtil.group(inheritedClause);
                    }***/
                //                System.err.println(clause);
                metadataOrs.add(clause);
            }
            if (metadataOrs.size() > 0) {
                //                metadataAnds.add(SqlUtil.group(SqlUtil.makeOr(metadataOrs)));
                metadataAnds.add(Clause.or(metadataOrs));
            }
        }

        if (metadataAnds.size() > 0) {
            where.add(Clause.and(metadataAnds));
        }



        String name = (String) request.getString(ARG_TEXT, "").trim();
        if (name.length() > 0) {
            if ( !request.get(ARG_EXACT, false)) {
                List tmp = StringUtil.split(name, ",", true, true);
                name = "%" + StringUtil.join("%,%", tmp) + "%";
            }
            List<Clause> ors       = new ArrayList<Clause>();
            boolean searchMetadata = request.get(ARG_SEARCHMETADATA, false);
            if (searchMetadata) {
                List<Clause> metadataOrs = new ArrayList<Clause>();
                metadataOrs.add(Clause.makeOrSplit(COL_METADATA_ATTR1, name));
                metadataOrs.add(Clause.makeOrSplit(COL_METADATA_ATTR2, name));
                metadataOrs.add(Clause.makeOrSplit(COL_METADATA_ATTR3, name));
                metadataOrs.add(Clause.makeOrSplit(COL_METADATA_ATTR4, name));
                ors.add(Clause.and(Clause.or(metadataOrs),
                                   Clause.join(COL_METADATA_ENTRY_ID,
                                             COL_ENTRIES_ID)));
            } else {
                ors.add(Clause.makeOrSplit(COL_ENTRIES_NAME, name));
                ors.add(Clause.makeOrSplit(COL_ENTRIES_DESCRIPTION, name));
            }

            where.add(Clause.or(ors));
            //            where.add("(" + StringUtil.join(" OR ", ors) + ")");
            //            System.err.println("where:" + where);
        }
        //        System.err.println("where:" + where);

        return where;

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param stmt _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    public void setStatement(Entry entry, PreparedStatement stmt,
                             boolean isNew)
            throws Exception {}

    /**
     * _more_
     *
     *
     * @param isNew _more_
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
     * @param requess _more_
     * @param statement _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request requess, Statement statement, String id)
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
        return initTables;
    }



    /**
     * _more_
     *
     * @param columnName _more_
     * @param value _more_
     *
     * @return _more_
     */
    public Object convert(String columnName, String value) {
        return value;
    }

    /**
     * _more_
     *
     * @param map _more_
     *
     * @return _more_
     */
    public Object[] makeValues(Hashtable map) {
        return null;
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
     * _more_
     *
     * @param column _more_
     * @param value _more_
     * @param clauses _more_
     *
     * @return _more_
     */
    protected boolean addOrClause(String column, String value,
                                  List<Clause> clauses) {
        if ((value != null) && (value.trim().length() > 0)
                && !value.toLowerCase().equals("all")) {
            clauses.add(Clause.makeOrSplit(column, value));
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

    /**
     * Set the DfltDataType property.
     *
     * @param value The new value for DfltDataType
     */
    public void setDefaultDataType(String value) {
        defaultDataType = value;
    }

    /**
     * Get the DfltDataType property.
     *
     * @return The DfltDataType
     */
    public String getDefaultDataType() {
        return defaultDataType;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasDefaultDataType() {
        return (defaultDataType != null) && (defaultDataType.length() > 0);
    }



}


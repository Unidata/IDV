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


    public final Entry getEntry(ResultSet results) throws Exception {
        return getEntry(results,false);
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
    public Entry getEntry(ResultSet results, boolean abbreviated) throws Exception {
        //id,type,name,desc,group,user,file,createdata,fromdate,todate
        int    col   = 3;
        String id    = results.getString(1);
        Entry  entry = createEntry(id);
        entry.init(results.getString(col++), results
            .getString(col++), getRepository()
            .findGroup(results.getString(col++)), getRepository()
            .getUserManager()
            .findUser(results.getString(col++), true), new Resource(results
                .getString(col++), results.getString(col++)), results
                    .getTimestamp(col++, getRepository().calendar)
                    .getTime(), results
                    .getTimestamp(col++, getRepository().calendar)
                    .getTime(), results
                    .getTimestamp(col++, getRepository().calendar)
                    .getTime(), null);
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


            List<Association> associations =
                getRepository().getAssociations(request, entry.getId());
            if (associations.size() > 0) {
                StringBuffer assocSB = new StringBuffer();
                assocSB.append("<table>");
                for (Association association : associations) {
                    Entry fromEntry = null;
                    Entry toEntry   = null;
                    if (association.getFromId().equals(entry.getId())) {
                        fromEntry = entry;
                    } else {
                        fromEntry =
                            getRepository().getEntry(association.getFromId(),
                                request);
                    }
                    if (association.getToId().equals(entry.getId())) {
                        toEntry = entry;
                    } else {
                        toEntry =
                            getRepository().getEntry(association.getToId(),
                                request);
                    }
                    if ((fromEntry == null) || (toEntry == null)) {
                        continue;
                    }
                    assocSB.append("<tr><td>");
                    assocSB.append(((fromEntry == entry)
                                    ? fromEntry.getName()
                                    : getRepository().getEntryUrl(
                                        fromEntry)));
                    assocSB.append("&nbsp;&nbsp;");
                    assocSB.append("</td><td>");
                    assocSB.append(HtmlUtil.bold(association.getName()));
                    assocSB.append("</td><td>");
                    assocSB.append(
                        HtmlUtil.img(getRepository().fileUrl(ICON_ARROW)));
                    assocSB.append("&nbsp;&nbsp;");
                    assocSB.append("</td><td>");
                    assocSB.append(((toEntry == entry)
                                    ? toEntry.getName()
                                    : getRepository().getEntryUrl(toEntry)));
                    assocSB.append("</td></tr>");
                }
                assocSB.append("</table>");
                sb.append(HtmlUtil.formEntryTop("Associations:",
                        assocSB.toString()));
            }

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
                sb.append(HtmlUtil.formEntry("Metadata:", mSB.toString()));
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
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links)
            throws Exception {
        Link downloadLink = getEntryDownloadLink(request, entry);
        if (downloadLink != null) {
            links.add(downloadLink);
        }
        links.add(
            new Link(
                HtmlUtil.url(
                    getRepository().URL_COMMENTS_SHOW, ARG_ID,
                    entry.getId()), getRepository().fileUrl(ICON_COMMENTS),
                                    "Add/View Comments"));

        if (getAccessManager().canEditEntry(request, entry)) {
            links.add(
                new Link(
                    HtmlUtil.url(
                        getRepository().URL_ENTRY_FORM, ARG_ID,
                        entry.getId()), getRepository().fileUrl(ICON_EDIT),
                                        "Edit Entry"));
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
                .url(getRepository().URL_ENTRY_GET + "/" + entry.getName(),
                     ARG_ID, entry.getId()), getRepository()
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
                                             boolean showResource)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            OutputHandler outputHandler =
                getRepository().getOutputHandler(request);
            String nextPrev = outputHandler.getNextPrevLink(request, entry,
                                  output);
            sb.append(HtmlUtil.formEntry("", nextPrev));
            sb.append(HtmlUtil.formEntry("Name:", entry.getName()));


            String[] crumbs =
                getRepository().getBreadCrumbs(request,
                    getRepository().findGroup(entry.getParentGroupId()),
                    true, "");
            //            sb.append(HtmlUtil.formEntry("Group:", crumbs[1]));

            String desc = entry.getDescription();
            if ((desc != null) && (desc.length() > 0)) {
                sb.append(HtmlUtil.formEntry("Description:", desc));
            }
            sb.append(
                HtmlUtil.formEntry(
                    "Created by:",
                    entry.getUser().getLabel() + " @ "
                    + getRepository().fmt(entry.getCreateDate())));

            String resourceLink = entry.getResource().getPath();
            if (resourceLink.length() > 0) {
                if (entry.getResource().isUrl()) {
                    resourceLink = "<a href=\"" + resourceLink + "\">"
                                   + resourceLink + "</a>";
                }
                sb.append(HtmlUtil.formEntry("Resource:", resourceLink));

                if (entry.isFile()) {
                    sb.append(HtmlUtil.formEntry("Size:",
                            entry.getResource().getFile().length()
                            + " bytes"));
                }
            }

            if ((entry.getCreateDate() != entry.getStartDate())
                    || (entry.getCreateDate() != entry.getEndDate())) {
                if (entry.getEndDate() != entry.getStartDate()) {
                    sb.append(HtmlUtil.formEntry("Date Range:",
                            getRepository().fmt(entry.getStartDate())
                            + " -- "
                            + getRepository().fmt(entry.getEndDate())));
                } else {
                    sb.append(HtmlUtil.formEntry("Date:",
                            getRepository().fmt(entry.getStartDate())));
                }
            }
            String typeDesc = entry.getTypeHandler().getDescription();
            if ((typeDesc == null) || (typeDesc.trim().length() == 0)) {
                typeDesc = entry.getTypeHandler().getType();
            }
            sb.append(HtmlUtil.formEntry("Type:", typeDesc));

            if (entry.hasLocationDefined()) {
                sb.append(HtmlUtil.formEntry("Location:",
                                             entry.getSouth() + "/"
                                             + entry.getEast()));
            } else if (entry.hasAreaDefined()) {
                String img =
                    HtmlUtil.img(HtmlUtil.url(getRepository().URL_GETMAP,
                        ARG_SOUTH, "" + entry.getSouth(), ARG_WEST,
                        "" + entry.getWest(), ARG_NORTH,
                        "" + entry.getNorth(), ARG_EAST,
                        "" + entry.getEast()));
                sb.append(HtmlUtil.formEntry("Area:", img));
            }

            if (showResource && entry.getResource().isImage()) {
                if (entry.getResource().isFile()
                        && getAccessManager().canDownload(request, entry)) {
                    sb.append(
                        HtmlUtil.formEntryTop(
                            "Image:",
                            HtmlUtil.img(
                                HtmlUtil.url(
                                    getRepository().URL_ENTRY_GET + "/"
                                    + entry.getName(), ARG_ID,
                                        entry.getId()), "")));

                } else if (entry.getResource().isUrl()) {
                    sb.append(HtmlUtil.formEntryTop("Image:",
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
        //We do the replace because (for some reason) any CRNW screws up the pattern matching
        String whereString = cleanQueryString(SqlUtil.makeAnd(whereList));
        String whatString  = cleanQueryString(what);
        String extraString = cleanQueryString(extra);

        String[] tableNames = { TABLE_ENTRIES, getTableName(), 
                                TABLE_USERS, TABLE_ASSOCIATIONS };
        //        String[] tableNames = { TABLE_ENTRIES, getTableName(), TABLE_METADATA,
        //                                TABLE_USERS, TABLE_ASSOCIATIONS };
        List    tables     = new ArrayList();
        boolean didEntries = false;
        boolean didOther   = false;
        boolean didMeta    = false;
        //        System.err.println("what:" + whatString);
        //        System.out.println("where:" + whereString);
        //        System.out.println("extra:" + extraString);
        for (int i = 0; i < tableNames.length; i++) {
            String pattern = ".*[, =\\(]+" + tableNames[i] + "\\..*";
            //            System.out.println("pattern:" + pattern);
            if (whatString.matches(pattern) || whereString.matches(pattern)
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
            whereList.add(SqlUtil.eq(COL_METADATA_ENTRY_ID, COL_ENTRIES_ID));
            tables.add(TABLE_METADATA);
            didEntries = true;
        }

        int metadataCnt = 0;
        while(true) {
            String subTable = TABLE_METADATA+"_"+metadataCnt;
            metadataCnt++;
            if(whereString.indexOf(subTable)<0) break;
            tables.add(TABLE_METADATA+" " +subTable);
            //            whereList.add(SqlUtil.eq(subTable+".entry_id", COL_ENTRIES_ID));
        }


        if (didEntries) {
            String type = (String) request.getType("").trim();
            if (type.equals(TYPE_ANY)) {
                type = "";
            }

            if (request.get(ARG_TYPE_EXCLUDE_GROUP, false)) {
                if (type.length() > 0) {
                    type = type + ",";
                }
                type = type + "!group";
            }
            if (type.length() > 0) {
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


        String where = SqlUtil.makeAnd(whereList);
        String sql   = SqlUtil.makeSelect(what, tables, where, extra);
        System.err.println (sql);
        return getDatabaseManager().execute(sql,
                                            getRepository().getMax(request));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, StringBuffer formBuffer,
                               Entry entry)
            throws Exception {}


    /**
     * _more_
     *
     * @param formBuffer _more_
     * @param request _more_
     * @param where _more_
     * @param simpleForm _more_
     * @param advancedForm _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer formBuffer,
                                List where, boolean advancedForm)
            throws Exception {

        List dateSelect = new ArrayList();
        dateSelect.add(new TwoFacedObject("Last hour", "-1 hour"));
        dateSelect.add(new TwoFacedObject("Last 3 hours", "-3 hours"));
        dateSelect.add(new TwoFacedObject("Last 6 hours", "-6 hours"));
        dateSelect.add(new TwoFacedObject("Last 12 hours", "-12 hours"));
        dateSelect.add(new TwoFacedObject("Last day", "-1 day"));
        dateSelect.add(new TwoFacedObject("Last 7 days", "-7 days"));
        dateSelect.add(new TwoFacedObject("All", "none"));
        dateSelect.add(new TwoFacedObject("Custom:", ""));
        String dateSelectInput = HtmlUtil.select(ARG_RELATIVEDATE,
                                     dateSelect,
                                     request.getString(ARG_RELATIVEDATE,
                                         "-1 hour"));
        String minDate = request.getDateSelect(ARG_FROMDATE, (String) null);
        String maxDate = request.getDateSelect(ARG_TODATE, (String) null);

        //        request.remove(ARG_FROMDATE);
        //        request.remove(ARG_TODATE);


        List<TypeHandler> typeHandlers =
            getRepository().getTypeHandlers(request);
        if ((typeHandlers.size() == 0) && request.defined(ARG_TYPE)) {
            typeHandlers.add(getRepository().getTypeHandler(request));
        }


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
                tmp.add(new TwoFacedObject(typeHandler.getLabel(),
                                           typeHandler.getType()));
            }
            TwoFacedObject anyTfo = new TwoFacedObject(TYPE_ANY, TYPE_ANY);
            if ( !tmp.contains(anyTfo)) {
                tmp.add(0, anyTfo);
            }
            String typeSelect = HtmlUtil.select(ARG_TYPE, tmp);
            String groupCbx = HtmlUtil.checkbox(ARG_TYPE_EXCLUDE_GROUP,
                                  "true", false) + HtmlUtil.space(1)
                                      + "Exclude groups";
            formBuffer.append(
                HtmlUtil.formEntry(
                    "Type:",
                    typeSelect + " "
                    + HtmlUtil.submitImage(
                        getRepository().fileUrl(ICON_SEARCH), "submit_type",
                        "Show search form with this type") + HtmlUtil.space(
                            1) + groupCbx));
        } else if (typeHandlers.size() == 1) {
            formBuffer.append(HtmlUtil.hidden(ARG_TYPE,
                    typeHandlers.get(0).getType()));
            //            System.err.println("type handler: "
            //                               + typeHandlers.get(0).getDescription() + " "
            //                               + typeHandlers.get(0).getType());
            formBuffer.append(HtmlUtil.formEntry("Type:",
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
            formBuffer.append(HtmlUtil.formEntry("Text:",
                    HtmlUtil.input(ARG_NAME) + searchMetaData));
        } else {
            HtmlUtil.hidden(ARG_NAME, name);
            formBuffer.append(HtmlUtil.formEntry("Name:",
                    name + searchMetaData));
        }
        formBuffer.append("\n");


        String dateHelp = " (e.g., 2007-12-11 00:00:00)";

        formBuffer.append(
            HtmlUtil.formEntry(
                "Date Range:",
                dateSelectInput + HtmlUtil.space(1)
                + HtmlUtil.input(ARG_FROMDATE, minDate) + " -- "
                + HtmlUtil.input(ARG_TODATE, maxDate) + dateHelp));

        formBuffer.append("\n");



        request.put(ARG_FORM_ADVANCED, ( !advancedForm) + "");
        String urlArgs = request.getUrlArgs();
        request.put(ARG_FORM_ADVANCED, advancedForm + "");
        String link = HtmlUtil.href(getRepository().URL_ENTRY_SEARCHFORM
                                    + "?" + urlArgs, (advancedForm
                ? "- Advanced"
                : "+ Advanced"), " class=\"subheaderlink\" ");
        formBuffer.append("<tr><td colspan=2>");
        formBuffer.append(HtmlUtil.div(link, " class=\"subheader\""));
        formBuffer.append("</td></tr>");




        if (advancedForm || request.defined(ARG_GROUP)) {
            String groupArg = (String) request.getString(ARG_GROUP, "");
            String searchChildren = " "
                                    + HtmlUtil.checkbox(ARG_GROUP_CHILDREN,
                                        "true",
                                        request.get(ARG_GROUP_CHILDREN,
                                            false)) + " (Search subgroups)";
            if (groupArg.length() > 0) {
                formBuffer.append(HtmlUtil.hidden(ARG_GROUP, groupArg));
                Group group = getRepository().findGroup(groupArg);
                if (group != null) {
                    formBuffer.append(HtmlUtil.formEntry("Group:",
                            group.getFullName() + "&nbsp;" + searchChildren));

                }
            } else {
                Statement stmt = executeSelect(
                                     request,
                                     SqlUtil.distinct(
                                         COL_ENTRIES_PARENT_GROUP_ID), where);

                List<Group> groups =
                    getRepository().getGroups(SqlUtil.readString(stmt, 1));
                stmt.close();

                if (groups.size() > 1) {
                    List groupList = new ArrayList();
                    groupList.add(ALL_OBJECT);
                    for (Group group : groups) {
                        groupList.add(
                            new TwoFacedObject(group.getFullName()));
                    }
                    String groupSelect = HtmlUtil.select(ARG_GROUP,
                                             groupList, null, 100);
                    formBuffer.append(HtmlUtil.formEntry("Group:",
                            groupSelect + searchChildren));
                } else if (groups.size() == 1) {
                    formBuffer.append(HtmlUtil.hidden(ARG_GROUP,
                            groups.get(0).getFullName()));
                    formBuffer.append(HtmlUtil.formEntry("Group:",
                            groups.get(0).getFullName() + searchChildren));
                }
            }
            formBuffer.append("\n");
        }


        if (advancedForm) {
            String nonGeo =
                HtmlUtil.checkbox(ARG_INCLUDENONGEO, "true",
                                  request.get(ARG_INCLUDENONGEO,
                                      true)) + " Include non-geographic";
            String areaWidget = HtmlUtil.makeLatLonBox(ARG_AREA, "", "", "",
                                    "");
            areaWidget = "<table>" + HtmlUtil.cols(areaWidget) + "</table>";
            //            formBuffer.append(HtmlUtil.formEntry("Extent:", areaWidget+"\n"+HtmlUtil.img(getRepository().URL_GETMAP.toString(),"map"," name=\"map\"  xxxonmouseover = \"mouseMove()\"")));
            formBuffer.append(HtmlUtil.formEntry("Extent:", areaWidget));
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


        if (request.defined(ARG_RESOURCE)) {
            addOr(COL_ENTRIES_RESOURCE, request.getString(ARG_RESOURCE, ""),
                  where, true);
        }

        if (request.defined(ARG_USER)) {
            addOr(COL_ENTRIES_USER_ID, request.getString(ARG_USER, ""),
                  where, true);
        }

        if (request.defined(ARG_GROUP)) {
            String groupName = (String) request.getString(ARG_GROUP,
                                   "").trim();
            boolean doNot = groupName.startsWith("!");
            if (doNot) {
                groupName = groupName.substring(1);
            }
            if (groupName.endsWith("%")) {
                //                where.add(SqlUtil.eq(COL_GROUPS_ID,ENTRIES_PARENT_GROUP_ID));
                where.add(SqlUtil.like(COL_ENTRIES_PARENT_GROUP_ID,
                                       groupName));
            } else {
                Group group = getRepository().findGroup(request);
                if (group == null) {
                    throw new IllegalArgumentException(
                        "Could not find group:" + groupName);
                }
                String searchChildren =
                    (String) request.getString(ARG_GROUP_CHILDREN,
                        (String) null);
                if (Misc.equals(searchChildren, "true")) {
                    String sub = (doNot
                                  ? SqlUtil.notLike(
                                      COL_ENTRIES_PARENT_GROUP_ID,
                                      group.getId() + Group.IDDELIMITER + "%")
                                  : SqlUtil.like(COL_ENTRIES_PARENT_GROUP_ID,
                                      group.getId() + Group.IDDELIMITER
                                      + "%"));
                    String equals = (doNot
                                     ? SqlUtil.neq(
                                         COL_ENTRIES_PARENT_GROUP_ID,
                                         SqlUtil.quote(group.getId()))
                                     : SqlUtil.eq(
                                         COL_ENTRIES_PARENT_GROUP_ID,
                                         SqlUtil.quote(group.getId())));
                    where.add("(" + sub + " OR " + equals + ")");
                } else {
                    if (doNot) {
                        where.add(SqlUtil.neq(COL_ENTRIES_PARENT_GROUP_ID,
                                SqlUtil.quote(group.getId())));
                    } else {
                        where.add(SqlUtil.eq(COL_ENTRIES_PARENT_GROUP_ID,
                                             SqlUtil.quote(group.getId())));
                    }
                }
            }
        }



        Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE,
                               new Date());
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


        Hashtable args           = request.getArgs();
        String    metadataPrefix = ARG_METADATA_TYPE + ".";
        Hashtable typeMap = new Hashtable();
        List types = new ArrayList();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(metadataPrefix)) {
                continue;
            }
            if ( !request.defined(arg)) {
                continue;
            }
            String type = arg.substring(metadataPrefix.length());
            String attr1 = request.getString(arg, "");
            List values = (List) typeMap.get(type);
            if(values == null) {
                typeMap.put(type, values = new ArrayList());
                types.add(type);
            }
            values.add(attr1);
        }
        List      metadataAnds    = new ArrayList();

/**
select entries.name, id, parent_group_id from entries where
entries.parent_group_id LIKE
(select  metadata.entry_id ||'%' from metadata where 
entries.parent_group_id LIKE metadata.entry_id ||'%' AND
metadata.attr1='foo')
**/

        for(int typeIdx=0;typeIdx<types.size();typeIdx++) {
            String type = (String) types.get(typeIdx);
            List values = (List) typeMap.get(type);
            List  metadataOrs    = new ArrayList();
            String subTable = TABLE_METADATA +"_"+typeIdx;
            for(int i=0;i<values.size();i++) {
                String attr1 = (String) values.get(i);
                String clause = 
                    SqlUtil.makeAnd(
                                    Misc.newList(
                                                 SqlUtil.eq(subTable+".entry_id",COL_ENTRIES_ID),
                                                 SqlUtil.eq(
                                                            subTable+".attr1",
                                                            SqlUtil.quote(attr1)), SqlUtil.eq(
                                                                                              subTable+".type",
                                                                                              SqlUtil.quote(type))));
                String inheritedClause = COL_ENTRIES_PARENT_GROUP_ID +" LIKE " +
                    SqlUtil.group(SqlUtil.makeSelect("metadata.entry_id ||'%'", TABLE_METADATA,
                                                     "entries.parent_group_id LIKE " + "metadata.entry_id ||'%' AND " +
                                                     "metadata.attr1=" +SqlUtil.quote(attr1)));
 
                clause = SqlUtil.group(SqlUtil.makeOr(Misc.newList(SqlUtil.group(clause), SqlUtil.group(inheritedClause))));
                //                clause = SqlUtil.group(inheritedClause);
                System.err.println(clause);
                System.err.println("");
                metadataOrs.add(SqlUtil.group(clause));
            }
            if (metadataOrs.size() > 0) {
                //                metadataAnds.add(SqlUtil.group(SqlUtil.makeOr(metadataOrs)));
                metadataAnds.add(SqlUtil.makeOr(metadataOrs));
            }
        }

        //        metadataAnds.add(inheritedQuery);
        if(metadataAnds.size()>0) {
        //        System.err.println ("metadata:" + metadataAnds);
            //            where.add(SqlUtil.group(SqlUtil.makeAnd(metadataAnds)));
            where.add(SqlUtil.makeAnd(metadataAnds));
        }



        String name = (String) request.getString(ARG_NAME, "").trim();
        if ((name != null) && (name.length() > 0)) {
            List ors = new ArrayList();
            ors.add(SqlUtil.makeOrSplit(COL_ENTRIES_NAME, name, true));
            ors.add(SqlUtil.makeOrSplit(COL_ENTRIES_DESCRIPTION, name, true));
            if (request.get(ARG_SEARCHMETADATA, false)) {
                ors.add(SqlUtil.makeOrSplit(COL_METADATA_ATTR1, name, true));
                ors.add(SqlUtil.makeOrSplit(COL_METADATA_ATTR2, name, true));
                ors.add(SqlUtil.makeOrSplit(COL_METADATA_ATTR3, name, true));
                ors.add(SqlUtil.makeOrSplit(COL_METADATA_ATTR4, name, true));
            }

            where.add("(" + StringUtil.join(" OR ", ors) + ")");
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


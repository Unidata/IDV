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
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.sql.ResultSet;

import java.sql.Statement;

import java.sql.PreparedStatement;


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

    public static final Object ALL_OBJECT = new TwoFacedObject("All","");

    /** _more_ */
    public static final String TYPE_ANY = "any";

    /** _more_ */
    Repository repository;

    /** _more_ */
    String type;

    /** _more_ */
    String description;



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

    public String getDatasetTag(Entry entry, Request request) {
        return XmlUtil.tag(TAG_DATASET,
                           XmlUtil.attrs(ATTR_NAME,
                                         entry.getName(),
                                         ATTR_URLPATH, entry.getFile()));
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

    public String getNodeType() {
        return  NODETYPE_ENTRY;
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
            new Entry(results.getString(1),
                      this,
                      results.getString(col++), results.getString(col++),
                      repository.findGroup(results.getString(col++)),
                      repository.findUser(results.getString(col++)),
                      results.getString(col++),
                      results.getTimestamp(col++).getTime(),
                      results.getTimestamp(col++).getTime(),
                      results.getTimestamp(col++).getTime());
        return entry;
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
    public StringBuffer getEntryContent(Entry entry, Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        String output = request.getOutput();
        String[] tags = repository.getTags(request,entry.getId());
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append("<table cellspacing=\"5\" cellpadding=\"2\">");
            sb.append(getInnerEntryContent(entry, request, output));
            sb.append("</table>\n");
            if(tags.length>0) {
                sb.append("<h3>Tags</h3><ul>\n");
                for(int i=0;i<tags.length;i++) {
                    sb.append("<li> ");
                    sb.append(repository.getTagLinks(request, tags[i]));
                    sb.append(tags[i]);
                }
                sb.append("</ul>\n");
            }
        } else if (output.equals(DefaultOutputHandler.OUTPUT_XML)) {
        }
        else if (output.equals(DefaultOutputHandler.OUTPUT_CSV)) {
        }
        return sb;
    }

    protected String getEntryLinks(Entry entry, Request request) {
        return getEntryDownloadLink(request, entry)
            + "&nbsp;"
            + getGraphLink(request, entry);
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
        return repository.href(HtmlUtil.url("/graphview", ARG_ID, entry.getId(), ARG_NODETYPE,
                entry.getType()), HtmlUtil.img(repository.href("/tree.gif"),
                                               "Show file in graph"));
    }



    protected boolean isEntryDownloadable(Request request, Entry entry) {
        return true;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryDownloadLink(Request request, Entry entry) {
        if(!isEntryDownloadable(request, entry)) return "";
        if (repository.getProperty(PROP_HTML_DOWNLOADENTRIESASFILES, false)) {
            return HtmlUtil.href(
                "file://" + entry.getFile(),
                HtmlUtil.img(
                    repository.href("/Fetch.gif"),
                    "Download file"));
        } else {
            return repository.href(HtmlUtil.url("/getentry/" + entry.getName(), ARG_ID,
                    entry.getId()), HtmlUtil.img(repository.href("/Fetch.gif"),
                                                 "Download file"));
        }
    }




    public StringBuffer getInnerEntryContent(Entry entry, Request request,String output) throws Exception {
        StringBuffer sb = new StringBuffer();
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append(
                HtmlUtil.tableEntry(
                    HtmlUtil.bold("Name:"),
                    entry.getName() + "&nbsp;" +
                    getEntryLinks(entry, request)));

            String desc = entry.getDescription();
            if(desc!=null && desc.length()>0) {
                sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Description:"), desc));
            }
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Created by:"),
                                          entry.getUser().getName() + " @ " +
                                          fmt(entry.getCreateDate())));

            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("File:"),
                                          entry.getFile()));
            
            if(entry.getCreateDate()!= entry.getStartDate() ||
               entry.getCreateDate()!= entry.getEndDate()) {
                if(entry.getEndDate()!= entry.getStartDate()) {
                    sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Date Range:"), 
                                                  fmt(entry.getStartDate()) +" -- " +
                                                  fmt(entry.getEndDate())));
                } else {
                    sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Date:"), fmt(entry.getStartDate())));
                }
            }
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Type:"),
                                          entry.getTypeHandler().getDescription()));
        } else if (output.equals(DefaultOutputHandler.OUTPUT_XML)) {
        }
        else if (output.equals(DefaultOutputHandler.OUTPUT_CSV)) {
        }
        return sb;
    }


    private String fmt(long dttm) {
        return ""+new Date(dttm);
    }


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
        return  new Result("Error",
                           new StringBuffer("Unknown listing type:" +what));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTableName() {
        return TABLE_ENTRIES;
    }


    protected Statement executeSelect(Request request, String what) throws Exception {
        return executeSelect(request, what, assembleWhereClause(request));
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param where _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement executeSelect(Request request, String what, List where) throws Exception {
        return executeSelect(request, what, where, "");
    }


    protected Statement executeSelect(Request request, String what, List whereList, String extra)
        throws Exception {
        whereList = new ArrayList(whereList);
        String[] tableNames = { TABLE_ENTRIES, getTableName(), TABLE_USERS, TABLE_GROUPS,
                                TABLE_TAGS,TABLE_ASSOCIATIONS};
        List tables = new ArrayList();
        boolean didEntries = false;
        boolean didOther = false;
        String where = SqlUtil.makeAnd(whereList);
        for (int i = 0; i < tableNames.length; i++) {
            if (what.indexOf(tableNames[i] + ".") >= 0 ||
                where.indexOf(tableNames[i] + ".") >= 0 ||
                extra.indexOf(tableNames[i] + ".") >= 0) {
                tables.add(tableNames[i]);
                if(i == 0) didEntries = true;
                else if(i == 1) didOther = true;
            }
        }


        if(didEntries) {
            String type = (String) request.getType("").trim();
            if (type.length()>0 && !type.equals(TYPE_ANY)) {
                addOr(COL_ENTRIES_TYPE, type, whereList, true);
                where = SqlUtil.makeAnd(whereList);
            }
        }

        //The join
        if(didEntries && didOther && !TABLE_ENTRIES.equalsIgnoreCase(getTableName())) {
            whereList.add(0,SqlUtil.eq(COL_ENTRIES_ID, getTableName()+".id"));
            where = SqlUtil.makeAnd(whereList);
        }
        String sql = SqlUtil.makeSelect(what, tables, where,extra);
        return getRepository().execute(sql,repository.getMax(request));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected Repository getRepository() {
        return repository;
    }

    /**
     * _more_
     *
     * @param sb _more_
     *
     * @param formBuffer _more_
     * @param headerBuffer _more_
     * @param request _more_
     * @param where _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(StringBuffer formBuffer, StringBuffer headerBuffer,
                          Request request, List where)
            throws Exception {

        String minDate     = request.getDateSelect(ARG_FROMDATE,(String)null);
        String maxDate     = request.getDateSelect(ARG_TODATE,(String)null);
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
            }*/

        minDate     = "";
        maxDate     = "";

        List<TypeHandler> typeHandlers = repository.getTypeHandlers(request);
        if(typeHandlers.size()==0 && request.defined(ARG_TYPE)) {
            typeHandlers.add(repository.getTypeHandler(request.getType()));
        }
        if (typeHandlers.size() > 1) {
            List tmp = new ArrayList();
            for (TypeHandler typeHandler : typeHandlers) {
                tmp.add(new TwoFacedObject(typeHandler.getType(),
                                           typeHandler.getType()));
            }
            TwoFacedObject anyTfo = new TwoFacedObject(TYPE_ANY, TYPE_ANY);
            if ( !tmp.contains(anyTfo)) {
                tmp.add(0, anyTfo);
            }
            String typeSelect = HtmlUtil.select(ARG_TYPE, tmp);
            formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Type:"),
                    typeSelect+" " + HtmlUtil.submitImage(repository.getUrlBase()+ "/Search16.gif","submit_type","Show search form with this type")));
        } else if (typeHandlers.size() == 1) {
            formBuffer.append(HtmlUtil.hidden(ARG_TYPE,
                    typeHandlers.get(0).getType()));
            formBuffer.append(HtmlUtil.tableEntry("<b>Type:</b>",
                    typeHandlers.get(0).getDescription()));
        }
        formBuffer.append("\n");


        String name = (String) request.getString(ARG_NAME,"");
        if (name.trim().length()==0) {
            formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Name:"),
                    HtmlUtil.input(ARG_NAME)));
        } else {
            formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Name:"),
                                                  name));
        }
        formBuffer.append("\n");


        String groupArg = (String) request.getString(ARG_GROUP,"");
        if (groupArg.length()>0) {
            formBuffer.append(HtmlUtil.hidden(ARG_GROUP, groupArg));
            Group group = repository.findGroup(groupArg); 
            if(group!=null) {
                formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Group:"),
                        group.getFullName() + "&nbsp;"
                                                      + HtmlUtil.checkbox(ARG_GROUP_CHILDREN,
                                                                          "true") + " (Search subgroups)"));

            }
        } else {
            Statement stmt = executeSelect(request,
                                           SqlUtil.distinct(COL_ENTRIES_GROUP_ID),
                                           where);

            List<Group> groups = repository.getGroups(SqlUtil.readString(stmt, 1));

            if (groups.size() > 1) {
                List groupList = new ArrayList();
                groupList.add(ALL_OBJECT);
                for (Group group : groups) {
                    groupList.add(new TwoFacedObject(group.getFullName()));
                }
                String groupSelect = HtmlUtil.select(ARG_GROUP, groupList);
                groupSelect += "&nbsp;"
                               + HtmlUtil.checkbox(ARG_GROUP_CHILDREN,
                                   "true") + " (Search subgroups)";
                formBuffer.append(
                    HtmlUtil.tableEntry(
                        HtmlUtil.bold("Group:"), groupSelect));
            } else if (groups.size() == 1) {
                formBuffer.append(HtmlUtil.hidden(ARG_GROUP,
                        groups.get(0).getFullName()));
                formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Group:"),
                        groups.get(0).getFullName()));
            }
        }
        formBuffer.append("\n");

        String tag = (String) request.getString(ARG_TAG,"");
        formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Tag:"),
                HtmlUtil.input(ARG_TAG, tag)));

        formBuffer.append("\n");

        formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold("Date Range:"),
                HtmlUtil.input(ARG_FROMDATE, minDate) + " -- "
                + HtmlUtil.input(ARG_TODATE, maxDate)));

        formBuffer.append("\n");
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
        List   where = new ArrayList();
        String name  = (String) request.getString(ARG_NAME,"").trim();

        String tag = (String) request.getString(ARG_TAG,(String)null);
        if (tag != null) {
            tag = tag.trim();
            if (tag.length() > 0) {
                where.add(SqlUtil.eq(COL_ENTRIES_ID, COL_TAGS_ENTRY_ID));
                addOr(COL_TAGS_NAME, tag, where, true);
            }
        }

        String groupName = (String) request.getString(ARG_GROUP,"").trim();
        if (groupName.length()>0) {
            boolean doNot = groupName.startsWith("!");
            if (doNot) {
                groupName = groupName.substring(1);
            }
            if (groupName.endsWith("%")) {
                //                where.add(SqlUtil.eq(COL_GROUPS_ID,ENTRIES_GROUP_ID));
                where.add(SqlUtil.like(COL_ENTRIES_GROUP_ID, groupName));
            } else {
                Group group = repository.findGroupFromName(groupName);
                if(group == null) {
                    throw new IllegalArgumentException ("Could not find group:" + groupName);
                }
                String searchChildren =
                    (String) request.getString(ARG_GROUP_CHILDREN,(String)null);
                if (Misc.equals(searchChildren, "true")) {
                    String sub = (doNot?
                                  SqlUtil.notLike(COL_ENTRIES_GROUP_ID, group.getId() + Group.IDDELIMITER+"%"):
                                  SqlUtil.like(COL_ENTRIES_GROUP_ID, group.getId() + Group.IDDELIMITER+ "%"));
                    String equals = (doNot?
                                     SqlUtil.neq(COL_ENTRIES_GROUP_ID,SqlUtil.quote(group.getId())):
                                     SqlUtil.eq(COL_ENTRIES_GROUP_ID,SqlUtil.quote(group.getId())));
                    where.add("("+sub +" OR " + equals+")");
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

        Date now = new Date();
        String fromDate = (String) request.getDateSelect(ARG_FROMDATE,"").trim().toLowerCase();
        String toDate = (String) request.getDateSelect(ARG_TODATE,"").trim().toLowerCase();

        Date fromDttm=DateUtil.parseRelative(now,fromDate,-1);
        Date toDttm=DateUtil.parseRelative(now,toDate,+1);
        if(fromDate.length()>0 && fromDttm==null) {
            if(!fromDate.startsWith("-")) {
                fromDttm = DateUtil.parse(fromDate);
            }
        }
        if(toDate.length()>0 &&toDttm==null) {
            if(!toDate.startsWith("+")) {
                toDttm = DateUtil.parse(toDate);
            }
        }

        if(fromDttm ==null && fromDate.startsWith("-")) {
            if(toDttm == null) throw new IllegalArgumentException("Cannot do relative From Date when To Date is not set");
            fromDttm = DateUtil.getRelativeDate(toDttm, fromDate);
            
        }

        if(toDttm == null && toDate.startsWith("+")) {
            if(fromDttm == null) throw new IllegalArgumentException("Cannot do relative From Date when To Date is not set");
            toDttm = DateUtil.getRelativeDate(fromDttm, toDate);
        }

        if (fromDttm!=null) {
            where.add(SqlUtil.ge(COL_ENTRIES_FROMDATE,fromDttm));
        }


        if (toDttm!=null) {
            where.add(SqlUtil.le(COL_ENTRIES_TODATE,toDttm));
        }


        Date createDate =  request.get(ARG_CREATEDATE,(Date) null);
        if (createDate != null) {
            where.add(SqlUtil.le(COL_ENTRIES_CREATEDATE,createDate));
        }

        if ((name != null) && (name.length() > 0)) {
            addOr(COL_ENTRIES_NAME, name, where, true);
            addOr(COL_ENTRIES_DESCRIPTION, name, where, true);
        }

        return where;
    }


    public void setStatement(Entry entry, PreparedStatement stmt) throws Exception {
    }

    public String getInsertSql() {
        return null;
    }

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
    public String toString() {
        return type + " " + description;
    }


}


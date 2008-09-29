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
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;

import java.lang.reflect.*;

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
public class GenericTypeHandler extends TypeHandler {

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String COL_ID = "id";

    /** _more_ */
    List<Column> columns;

    /** _more_ */
    List colNames;

    /** _more_ */
    Hashtable nameMap = new Hashtable();

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GenericTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
        init(entryNode);
    }


    /**
     * _more_
     *
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    private void init(Element entryNode) throws Exception {
        type = XmlUtil.getAttribute(entryNode, ATTR_DB_NAME);
        setDescription(XmlUtil.getAttribute(entryNode, ATTR_DB_DESCRIPTION,
                                            getType()));

        setDefaultDataType(XmlUtil.getAttribute(entryNode, ATTR_DATATYPE,
                (String) null));

        List propertyNodes = XmlUtil.findChildren(entryNode, TAG_PROPERTY);
        for (int propIdx = 0; propIdx < propertyNodes.size(); propIdx++) {
            Element propertyNode = (Element) propertyNodes.get(propIdx);
            if (XmlUtil.hasAttribute(propertyNode, ATTR_VALUE)) {
                putProperty(XmlUtil.getAttribute(propertyNode, ATTR_NAME),
                            XmlUtil.getAttribute(propertyNode, ATTR_VALUE));
            } else {
                putProperty(XmlUtil.getAttribute(propertyNode, ATTR_NAME),
                            XmlUtil.getChildText(propertyNode));
            }
        }

        this.columns = new ArrayList<Column>();
        colNames     = new ArrayList();

        List columnNodes = XmlUtil.findChildren(entryNode, TAG_COLUMN);
        if (columnNodes.size() == 0) {
            return;
        }


        Statement statement = getDatabaseManager().createStatement();
        colNames.add(COL_ID);
        StringBuffer tableDef = new StringBuffer("CREATE TABLE "
                                    + getTableName() + " (\n");

        tableDef.append(COL_ID + " varchar(200))");
        try {
            statement.execute(tableDef.toString());
        } catch (Throwable exc) {
            if (exc.toString().indexOf("already exists") < 0) {
                //TODO:
                //                throw new WrapperException(exc);
            }
        }

        StringBuffer indexDef = new StringBuffer();
        indexDef.append("CREATE INDEX " + getTableName() + "_INDEX_" + COL_ID
                        + "  ON " + getTableName() + " (" + COL_ID + ");\n");

        try {
            SqlUtil.loadSql(indexDef.toString(), statement, true);
        } catch (Throwable exc) {
            //TODO:
            //            throw new WrapperException(exc);
        }

        for (int colIdx = 0; colIdx < columnNodes.size(); colIdx++) {
            Element columnNode = (Element) columnNodes.get(colIdx);
            String className = XmlUtil.getAttribute(columnNode, ATTR_CLASS,
                                   Column.class.getName());
            Class c = Misc.findClass(className);
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { getClass(),
                    Element.class, Integer.TYPE });
            Column column = (Column) ctor.newInstance(new Object[] { this,
                    columnNode, new Integer(colNames.size() - 1) });
            columns.add(column);
            colNames.addAll(column.getColumnNames());
            column.createTable(statement);
        }
        statement.close();

        //TODO: Run through the table and delete any columns and indices that aren't defined anymore


    }

    /**
     * _more_
     *
     * @param columnName _more_
     *
     * @return _more_
     */
    public Column findColumn(String columnName) {
        for (Column column : columns) {
            if (column.getName().equals(columnName)) {
                return column;
            }
        }
        throw new IllegalArgumentException("Could not find column:"
                                           + columnName);
    }

    /**
     * _more_
     *
     * @param map _more_
     *
     * @return _more_
     */
    public Object[] makeValues(Hashtable map) {
        Object[] values = new Object[columns.size()];
        //For now we just assume each column has a single value
        int idx = 0;
        for (Column column : columns) {
            Object data = map.get(column.getName());
            values[idx] = data;
            idx++;
        }
        return values;
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
        Column column = findColumn(columnName);
        return column.convert(value);
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
            throws Exception {
        super.initializeEntry(request, entry);
        if (colNames.size() <= 1) {
            return;
        }
        Object[] values = new Object[columns.size()];
        for (Column column : columns) {
            column.setValue(request, values);
        }
        entry.setValues(values);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntry(Request request, Entry entry, Element node)
            throws Exception {}



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
        for (Column column : columns) {
            int match = column.matchValue(arg, value, request, entry);
            if (match == MATCH_FALSE) {
                return MATCH_FALSE;
            }
            if (match == MATCH_TRUE) {
                return MATCH_TRUE;
            }
        }
        return MATCH_UNKNOWN;
    }


    /**
     * _more_
     *
     * @param longName _more_
     *
     * @return _more_
     */
    public List<TwoFacedObject> getListTypes(boolean longName) {
        List<TwoFacedObject> list = super.getListTypes(longName);
        for (Column column : columns) {
            if (column.getCanList()) {
                list.add(new TwoFacedObject((longName
                                             ? (getDescription() + " - ")
                                             : "") + column
                                             .getDescription(), column
                                             .getFullName()));
            }
        }
        return list;
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
        Column theColumn = null;
        for (Column column : columns) {
            if (column.getCanList() && column.getFullName().equals(what)) {
                theColumn = column;
                break;
            }
        }

        if (theColumn == null) {
            return super.processList(request, what);
        }

        String       column = theColumn.getFullName();
        String       tag    = theColumn.getName();
        String       title  = theColumn.getDescription();
        List<Clause> where  = assembleWhereClause(request);
        Statement statement = select(request, SqlUtil.distinct(column),
                                     where, "");

        String[]     values = SqlUtil.readString(statement, 1);
        StringBuffer sb     = new StringBuffer();
        OutputType       output = request.getOutput();
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append(repository.header(title));
            sb.append("<ul>");
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(tag + "s"));
        }

        Properties properties =
            repository.getFieldProperties(theColumn.getPropertiesFile());
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                continue;
            }
            String longName = theColumn.getLabel(values[i]);
            if (output.equals(OutputHandler.OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(longName);
            } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
                String attrs = XmlUtil.attrs(ATTR_ID, values[i]);
                if (properties != null) {
                    for (Enumeration keys = properties.keys();
                            keys.hasMoreElements(); ) {
                        String key = (String) keys.nextElement();
                        if (key.startsWith(values[i] + ".")) {
                            String value = (String) properties.get(key);
                            value = value.replace("${value}", values[i]);
                            key   = key.substring((values[i] + ".").length());
                            attrs = attrs + XmlUtil.attr(key, value);
                        }
                    }
                }
                sb.append(XmlUtil.tag(tag, attrs));
            } else if (output.equals(CsvOutputHandler.OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(values[i], longName));
                sb.append("\n");
            }
        }
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(tag + "s"));
        }
        return new Result(
            title, sb,
            repository.getOutputHandler(request).getMimeType(output));
    }




    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public boolean equals(Object obj) {
        if ( !super.equals(obj)) {
            return false;
        }
        //TODO
        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, Entry entry)
            throws Exception {
        super.deleteEntry(request, statement, entry);
        deleteEntry(request, statement, entry.getId());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, String id)
            throws Exception {
        if (colNames.size() == 0) {
            return;
        }

        String query = SqlUtil.makeDelete(getTableName(), COL_ID,
                                          SqlUtil.quote(id));
        statement.execute(query);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param searchCriteria _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Clause> assembleWhereClause(Request request,
            StringBuffer searchCriteria)
            throws Exception {
        List<Clause> where = super.assembleWhereClause(request,
                                 searchCriteria);
        int originalSize = where.size();
        for (Column column : columns) {
            if ( !column.getCanSearch()) {
                continue;
            }
            column.assembleWhereClause(request, where, searchCriteria);
        }
        if ((originalSize != where.size()) && (originalSize > 0)) {
            where.add(Clause.join(COL_ENTRIES_ID, getTableName() + ".id"));
        }
        return where;
    }


    /**
     * _more_
     *
     *
     * @param isNew _more_
     * @return _more_
     */
    public String getInsertSql(boolean isNew) {
        if (colNames.size() == 0) {
            return null;
        }
        if (isNew) {
            return SqlUtil.makeInsert(
                getTableName(), SqlUtil.comma(colNames),
                SqlUtil.getQuestionMarks(colNames.size()));
        } else {
            return SqlUtil.makeUpdate(getTableName(), COL_ID,
                                      Misc.listToStringArray(colNames));
        }
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
            throws Exception {
        int stmtIdx = 1;
        stmt.setString(stmtIdx++, entry.getId());
        Object[] values = entry.getValues();
        if (values != null) {
            for (Column column : columns) {
                stmtIdx = column.setValues(stmt, values, stmtIdx);
            }
        }
        if ( !isNew) {
            stmt.setString(stmtIdx, entry.getId());
        }
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
        Entry entry = super.getEntry(results, abbreviated);
        if (abbreviated) {
            return entry;
        }
        if (colNames.size() == 0) {
            return entry;
        }
        Object[] values = new Object[columns.size()];

        Statement stmt = getDatabaseManager().select(SqlUtil.comma(colNames),
                             getTableName(),
                             Clause.eq(COL_ID, entry.getId()));
        ResultSet results2 = stmt.getResultSet();

        if (results2.next()) {
            //We start at 2, skipping 1, because the first one is the id
            int valueIdx = 2;
            for (Column column : columns) {
                valueIdx = column.readValues(results2, values, valueIdx);
            }
        }
        entry.setValues(values);
        return entry;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     * @param output _more_
     * @param showResource _more_
     * @param showMap _more_
     * @param linkToDownload _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuffer getInnerEntryContent(Entry entry, Request request,
                                             OutputType output,
                                             boolean showDescription,
                                             boolean showResource,
                                             boolean linkToDownload)
            throws Exception {
        StringBuffer sb = super.getInnerEntryContent(entry, request, output,
                                                     showDescription,
                                                     showResource, linkToDownload);
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            int      valueIdx = 0;
            Object[] values   = entry.getValues();
            if (values != null) {
                for (Column column : columns) {
                    if(!column.getCanShow()) continue;
                    StringBuffer tmpSb = new StringBuffer();
                    valueIdx = column.formatValue(tmpSb, output, values,
                            valueIdx);
                    sb.append(HtmlUtil.formEntry(column.getLabel() + ":",
                            tmpSb.toString()));
                }

            }
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {}
        return sb;
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
        html = super.processDisplayTemplate(request, entry, html);
        Object[] values = entry.getValues();
        OutputType   output = request.getOutput();
        if (values != null) {
            int valueIdx = 0;
            for (Column column : columns) {
                StringBuffer tmpSb = new StringBuffer();
                valueIdx = column.formatValue(tmpSb, output, values,
                        valueIdx);
                html = html.replace("${" + column.getName() + ".content}",
                                    tmpSb.toString());
                html = html.replace("${" + column.getName() + ".label}",
                                    column.getLabel());
            }
        }

        return html;
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
        super.getTablesForQuery(request, initTables);
        for (Column column : columns) {
            if ( !column.getCanSearch()) {
                continue;
            }
            if (request.defined(column.getFullName())) {
                initTables.add(getTableName());
                break;
            }
        }
        return initTables;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTableName() {
        return type;
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
            throws Exception {
        super.addToEntryForm(request, formBuffer, entry);
        Hashtable state = new Hashtable();
        for (Column column : columns) {
            column.addToEntryForm(request, formBuffer, entry, state);
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
        super.addToSearchForm(request, formBuffer, where, advancedForm);

        StringBuffer typeSB = new StringBuffer();
        for (Column column : columns) {
            column.addToSearchForm(request, typeSB, where);
        }

        if (typeSB.toString().length() > 0) {
            typeSB = new StringBuffer(HtmlUtil.formTable() + typeSB
                                      + HtmlUtil.formTableClose());
            formBuffer.append(HtmlUtil.p());
            formBuffer.append(getRepository().makeShowHideBlock(request,
                    msg(getLabel()), typeSB, true));
        }


    }


}


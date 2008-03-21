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
public class ReportTypeHandler extends TypeHandler {

    public static final String COL_ID = "id";
    public static final String COL_CONTENT = "content";

    public static final String TAG_FIELD = "column";

    private String formTemplatePath;




    /** _more_ */
    List<Column> columns = new ArrayList<Column>();


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public ReportTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository);
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

        StringBuffer tableDef = new StringBuffer("create table "
                                    + getTableName() + " (\n");
        tableDef.append(COL_ID + " varchar(200)");
        tableDef.append(", ");
        tableDef.append(COL_CONTENT + " varchar(10000)");
        tableDef.append(")");

        Statement statement =
            getRepository().getConnection().createStatement();
        try {
            statement.execute(tableDef.toString());
        } catch (Throwable exc) {
            //            if (exc.toString().indexOf("already exists") < 0) {
            //                throw new WrapperException(exc);
            //            }
        }

        formTemplatePath = XmlUtil.getAttribute(entryNode, "formtemplate");
        setDescription(XmlUtil.getAttribute(entryNode, ATTR_DB_DESCRIPTION,
                                            getType()));


        List columnNodes = XmlUtil.findChildren(entryNode, TAG_FIELD);
        for (int colIdx = 0; colIdx < columnNodes.size(); colIdx++) {
            Element columnNode = (Element) columnNodes.get(colIdx);
            Column  column = new Column(this, columnNode,
                                        columns.size() - 1);
            columns.add(column);
        }



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

    }



    public void addToEntryForm(Request request, StringBuffer formBuffer,
                               Entry entry)
            throws Exception {
        //        super.addToEntryForm(request, formBuffer, entry);
        String html  = getRepository().getResource(formTemplatePath);
        for(Column column: columns) {
            String widget = column.getFormWidget(request, entry);
            html = html.replace("${" + column.getName() +".formwidget}", widget);
            html = html.replace("${" + column.getName() +".label}", column.getLabel());
        }
        formBuffer.append(HtmlUtil.row(HtmlUtil.colspan(html,2)));
        formBuffer.append(HtmlUtil.hidden(ARG_NAME,getLabel()));
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
        Object[] values = new Object[1];
        entry.setValues(values);
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
     * @return _more_
     */
    public String getTableName() {
        return type;
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
        //        deleteEntry(request, statement, entry.getId());
    }


}


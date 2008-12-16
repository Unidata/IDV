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
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class WikiPageTypeHandler extends GenericTypeHandler {

    /** _more_          */
    public static String TYPE_WIKIPAGE = "wikipage";

    public static final String ARG_WIKI_TEXT = "wikipage.wikitext";

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public WikiPageTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
        initWikiTable(entryNode);

    }

    private void initWikiTable(Element node) throws Exception {
        Statement statement = getDatabaseManager().createStatement();

        StringBuffer tableDef = new StringBuffer("CREATE TABLE "
                                    + getTableName()+"_history" + " (\n");

        tableDef.append(COL_ID + " varchar(200),");
        tableDef.append(COL_ID + " varchar(200),");
        tableDef.append(")");
        try {
            statement.execute(tableDef.toString());
        } catch (Throwable exc) {
        }

        

        statement.close();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, StringBuffer sb, Entry entry)
            throws Exception {
        String size = HtmlUtil.SIZE_70;
        sb.append(HtmlUtil.formEntry(msgLabel("Wiki Page Title"),
                                     HtmlUtil.input(ARG_NAME, ((entry != null)
                ? entry.getName()
                : request.getString(ARG_NAME, "")), size)));

        String wikiText = "";
        if(entry!=null) {
            Object[]values = entry.getValues();
            if(values!=null && values.length>0 && values[0]!=null)
                wikiText = (String)values[0];
        }

        String select = OutputHandler.getSelect(request, ARG_WIKI_TEXT,
                            "Add link", true, true);

        String textWidget = HtmlUtil.textArea(ARG_WIKI_TEXT,
                                              wikiText,
                                              200, 80,
                                              HtmlUtil.id(ARG_WIKI_TEXT));
        textWidget = "<table><tr valign=\"top\"><td>" + textWidget
                     + "</td><td>" + select + "</td></tr></table>";
        sb.append(HtmlUtil.formEntryTop(msgLabel("Wiki Text"), textWidget));
    }


}


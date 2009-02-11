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

package ucar.unidata.repository;


import org.w3c.dom.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
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
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class TextOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_TEXT = new OutputType("Text",
                                                     "text",
                                                     OutputType.TYPE_HTML);


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public TextOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_TEXT);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param state _more_
     * @param types _more_
     * @param links _more_
     * @param forHeader _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, State state,
                                 List<Link> links)
            throws Exception {

        if (state.entry == null) {
            return;
        }
        if ( !state.entry.isFile()) {
            return;
        }
        String   path     = state.entry.getResource().getPath();
        String[] suffixes = new String[] { ".csv", ".txt", ".java", ".c" };

        for (int i = 0; i < suffixes.length; i++) {
            if (path.endsWith(suffixes[i])) {
                links.add(makeLink(request, state.entry, OUTPUT_TEXT));
                return;
            }
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
        String contents  = IOUtil.readContents(entry.getResource().getFile());
        StringBuffer sb  = new StringBuffer();
        int          cnt = 0;
        sb.append("<pre>");
        for (String line : (List<String>) StringUtil.split(contents, "\n",
                false, false)) {
            cnt++;
            line = line.replace("\r", "");
            line = HtmlUtil.entityEncode(line);
            sb.append("<a " + HtmlUtil.attr("name", "line" + cnt)
                      + "></a><a href=#line" + cnt + ">" + cnt + "</a>"
                      + HtmlUtil.space(1) + line + "<br>");
        }
        sb.append("</pre>");
        return makeLinksResult(request, msg("Text"), sb, new State(entry));
    }



}


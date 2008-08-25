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

import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
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
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CsvOutputHandler extends OutputHandler {




    /** _more_ */
    public static final String OUTPUT_CSV = "default.csv";



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public CsvOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    /**
     * _more_
     *
     *
     * @param output _more_
     *
     * @return _more_
     */
    public boolean canHandle(String output) {
        return output.equals(OUTPUT_CSV);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
    protected void addOutputTypes(Request request, State state,
                                  List<OutputType> types)
            throws Exception {
        types.add(new OutputType("CSV", OUTPUT_CSV));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param groups _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listGroups(Request request, List<Group> groups)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        for (Group group : groups) {
            sb.append(SqlUtil.comma(group.getFullName(), group.getId()));
            sb.append("\n");
        }
        return new Result("", sb, getMimeType(OUTPUT_CSV));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandlers _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTypes(Request request,
                               List<TypeHandler> typeHandlers)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        for (TypeHandler theTypeHandler : typeHandlers) {
            sb.append(SqlUtil.comma(theTypeHandler.getType(),
                                    theTypeHandler.getDescription()));
            sb.append("\n");
        }
        return new Result("", sb, getMimeType(OUTPUT_CSV));
    }





    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        if (output.equals(OUTPUT_CSV)) {
            return repository.getMimeTypeFromSuffix(".csv");
        }
        return super.getMimeType(output);
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
    protected Result listAssociations(Request request) throws Exception {

        StringBuffer  sb           = new StringBuffer();
        TypeHandler   typeHandler  = repository.getTypeHandler(request);
        String[]      associations = getRepository().getAssociations(request);

        List<String>  names        = new ArrayList<String>();
        List<Integer> counts       = new ArrayList<Integer>();
        ResultSet     results;
        int           max = -1;
        int           min = -1;
        for (int i = 0; i < associations.length; i++) {
            String association = associations[i];
            Statement stmt2 = typeHandler.select(request, SqlUtil.count("*"),
                                  Clause.eq(COL_ASSOCIATIONS_NAME,
                                            association), "");

            ResultSet results2 = stmt2.getResultSet();
            if ( !results2.next()) {
                continue;
            }
            int count = results2.getInt(1);
            if ((max < 0) || (count > max)) {
                max = count;
            }
            if ((min < 0) || (count < min)) {
                min = count;
            }
            names.add(association);
            counts.add(new Integer(count));
        }

        int    diff         = max - min;
        double distribution = diff / 5.0;

        for (int i = 0; i < names.size(); i++) {
            String association = names.get(i);
            sb.append(association);
            sb.append("\n");
        }

        return new Result("", sb, getMimeType(OUTPUT_CSV));

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
        return listGroups(request, subGroups);
    }



}


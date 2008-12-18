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

package ucar.unidata.repository.idv;
import ucar.unidata.repository.*;


import org.w3c.dom.*;


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
public class IdvWebstartOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_WEBSTART = new OutputType("View in IDV","idv.webstart",OutputType.TYPE_NONHTML,"","/icons/idv.gif");



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public IdvWebstartOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_WEBSTART);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, State state,
                                 List<Link> links, boolean forHeader)
            throws Exception {
            if(state.entry==null) return;
            Entry entry = state.entry;
            if(entry.getResource().getPath().endsWith(".xidv") ||
               entry.getResource().getPath().endsWith(".zidv")) {
                String suffix = "/"+entry.getId()+".jnlp";
                links.add(makeLink(request, state.getEntry(),OUTPUT_WEBSTART,suffix));
            } else {
                DataOutputHandler data = (DataOutputHandler) getRepository().getOutputHandler(DataOutputHandler.OUTPUT_OPENDAP);
                if(data !=null) {
                if(data.canLoadAsCdm(entry)) {
                    String suffix = "/"+entry.getId()+".jnlp";
                    links.add(makeLink(request, state.getEntry(),OUTPUT_WEBSTART,suffix));
                }
            }

        }
    }



    public Result outputEntry(Request request, Entry entry) throws Exception {
        String jnlp = getRepository().getResource("/ucar/unidata/repository/idv/template.jnlp");

        if(entry.getResource().getPath().endsWith(".xidv") ||
           entry.getResource().getPath().endsWith(".zidv")) {

            String url = HtmlUtil.url(request.url(getRepository().URL_ENTRY_GET) + "/"
                                      + entry.getName(), ARG_ENTRYID, entry.getId());
            url = getRepository().absoluteUrl(url);
            jnlp = jnlp.replace("%ARG%","-bundle");
            jnlp = jnlp.replace("%URL%",url);
        } else {
            DataOutputHandler data = (DataOutputHandler) getRepository().getOutputHandler(DataOutputHandler.OUTPUT_OPENDAP);
            if(data !=null && data.canLoadAsCdm(entry)) {
                jnlp = jnlp.replace("%ARG%","-data");
                String type = "OPENDAP.GRID";
                if(entry.getDataType()!=null) {
                    if(entry.getDataType().equals("point")) type = "NetCDF.POINT";
                }
                jnlp = jnlp.replace("%URL%","type:"+type+":"+data.getFullTdsUrl(entry));
            }
        }


        return new Result("",new StringBuffer(jnlp),"application/x-java-jnlp-file");
        //        return new Result("",new StringBuffer(jnlp),"text/xml");
    }



}


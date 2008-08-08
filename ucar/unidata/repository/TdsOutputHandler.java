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


import opendap.dap.DAP2Exception;



import opendap.dap.parser.ParseException;

import opendap.servlet.GuardedDataset;
import opendap.servlet.ReqState;

import org.w3c.dom.*;

import thredds.server.opendap.GuardedDatasetImpl;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;



import ucar.unidata.repository.*;

import java.io.*;

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

import javax.servlet.*;

import javax.servlet.http.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class TdsOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String OUTPUT_TDS = "tds";


    /** _more_          */
    private Hashtable<String, Boolean> checkedEntries = new Hashtable<String,
                                                            Boolean>();


    /**
     *     _more_
     *
     *     @param repository _more_
     *     @param element _more_
     *     @throws Exception On badness
     */
    public TdsOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }


    /**
     * Can we handle this output type
     *
     *
     * @param output The output type
     *
     * @return Is it tds?
     */
    public boolean canHandle(String output) {
        return output.equals(OUTPUT_TDS);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param types _more_
     *
     * @throws Exception On badness
     */
    protected void getOutputTypesForEntry(Request request, Entry entry,
                                          List<OutputType> types)
            throws Exception {
        //If we aren't in the tomcat world then exit
        if(request.getHttpServletRequest()==null) return;
        if (canLoad(entry)) {
            types.add(new OutputType("TDS", OUTPUT_TDS) {
                public String assembleUrl(Request request) {
                    return request.getRequestPath() + getSuffix() + "/"
                           + request.getPathEmbeddedArgs() + "/entry.das";
                }
            });
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getTdsUrl(Entry entry) {
        return "/" + ARG_OUTPUT + ":" + OUTPUT_TDS + "/" + ARG_ID + ":"
               + entry.getId() + "/entry.das";
    }


    /**
     * Can the given entry be served by the tds
     *
     * @param entry The entry
     *
     * @return Can the given entry be served by the tds
     */
    public boolean canLoad(Entry entry) {
        Boolean b = checkedEntries.get(entry.getId());
        if (b == null) {
            boolean ok = false;
            if (entry.isGroup()) {
                ok = false;
            } else if ( !entry.getResource().isFile()) {
                ok = false;
            } else {
                try {
                    File file = entry.getResource().getFile();
                    NetcdfFile ncfile =
                        NetcdfDataset.acquireFile(file.toString(), null);
                    ok = true;
                } catch (Exception ignoreThis) {}
            }
            b = new Boolean(ok);
            checkedEntries.put(entry.getId(), b);
        }
        return b.booleanValue();
    }


    /**
     * Serve up the entry
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public Result outputEntry(final Request request, Entry entry)
            throws Exception {
        //Bridge the ramadda servlet to the opendap servlet
        NcDODSServlet servlet = new NcDODSServlet(request, entry) {
            public ServletConfig getServletConfig() {
                return request.getHttpServlet().getServletConfig();
            }
            public ServletContext getServletContext() {
                return request.getHttpServlet().getServletContext();
            }
            public String getServletInfo() {
                return request.getHttpServlet().getServletInfo();
            }
            public Enumeration getInitParameterNames() {
                return request.getHttpServlet().getInitParameterNames();
            }
        };

        servlet.init(request.getHttpServlet().getServletConfig());
        servlet.doGet(request.getHttpServletRequest(),
                      request.getHttpServletResponse());
        //We have to pass back a result though we set needtowrite to false because the opendap servlet handles the writing
        Result result = new Result("");
        result.setNeedToWrite(false);
        return result;
    }



    /**
     * Class NcDODSServlet _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class NcDODSServlet extends opendap.servlet.AbstractServlet {

        /** _more_          */
        Request request;

        /** _more_          */
        Entry entry;

        /**
         * _more_
         *
         * @param request _more_
         * @param entry _more_
         */
        public NcDODSServlet(Request request, Entry entry) {
            this.request = request;
            this.entry   = entry;
        }

        /**
         * _more_
         *
         * @param preq _more_
         *
         * @return _more_
         *
         * @throws DAP2Exception On badness
         * @throws IOException On badness
         * @throws ParseException On badness
         */
        protected GuardedDataset getDataset(ReqState preq)
                throws DAP2Exception, IOException, ParseException {
            HttpServletRequest request = preq.getRequest();
            String             reqPath = entry.getName();
            String location = entry.getResource().getFile().toString();
            GuardedDatasetImpl guardedDataset =
                new GuardedDatasetImpl(reqPath, location);
            return guardedDataset;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getServerVersion() {
            return "opendap/3.7";
        }
    }


}


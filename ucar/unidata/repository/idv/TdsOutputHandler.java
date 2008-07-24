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



import opendap.dap.parser.ParseException;
import opendap.dap.DAP2Exception;

import opendap.servlet.GuardedDataset;
import thredds.server.opendap.GuardedDatasetImpl;
import opendap.servlet.ReqState;

import javax.servlet.http.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class TdsOutputHandler extends OutputHandler {


    //    public static void processScript(String scriptFile) throws Exception {


    /** _more_ */
    public static final String OUTPUT_TDS = "tds";


    private  Hashtable<String,Boolean> checkedEntries = new Hashtable<String,Boolean>();
    private boolean tdsEnabled = false;


    /**
     *     _more_
     *    
     *     @param repository _more_
     *     @param element _more_
     *     @throws Exception _more_
     */
    public TdsOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        tdsEnabled = true;
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
        return output.equals(OUTPUT_TDS);
    }




    protected void getOutputTypesForEntry(Request request, Entry entry,
                                          List<OutputType> types)
            throws Exception {

        //        if(    request.getHttpServletRequest()==null) return;
        if(canLoad(entry)) {
            types.add(new OutputType("TDS", OUTPUT_TDS));
        }
    }


    public boolean canLoad(Entry entry) {
        if(!tdsEnabled) return false;
        Boolean b = checkedEntries.get(entry.getId());
        if(b==null) {
            boolean ok = false;
            if(entry.isGroup()) {
                ok = false;
            } else if(!entry.getResource().isFile()) {
                ok = false;
            } else {
                try {
                    File file = entry.getResource().getFile();
                    NetcdfFile ncfile = NetcdfDataset.acquireFile(file.toString(), null);
                    System.err.println("OK");
                    ok = true;
                } catch(Exception ignoreThis) {}
            }
            b = new Boolean(ok);
            checkedEntries.put(entry.getId(),b);
        }
        return b.booleanValue();
    }


    public Result outputEntry(Request request, Entry entry) throws Exception {
        NcDODSServlet servlet  = new NcDODSServlet(request,entry);
        System.err.println ("have stuff:" + (request.getHttpServletRequest()!=null));
        servlet.doGet(request.getHttpServletRequest(),request.getHttpServletResponse());
        Result result = new Result("");
        result.setNeedToWrite(false);
        return result;
    }



    public class NcDODSServlet extends opendap.servlet.AbstractServlet {
        Request request;
        Entry entry;

        public NcDODSServlet(Request request, Entry entry) {
            this.request = request;
            this.entry  = entry;
        }

        protected GuardedDataset getDataset(ReqState preq) throws DAP2Exception, IOException, ParseException {
            HttpServletRequest request = preq.getRequest();
            String reqPath=entry.getName();
            String location=entry.getResource().getFile().toString();
            GuardedDatasetImpl guardedDataset = new GuardedDatasetImpl(reqPath, location);
            return guardedDataset;
        }

        public String getServerVersion() {
            return "opendap/3.7";
        }
    }


}


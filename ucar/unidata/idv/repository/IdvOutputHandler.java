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

package ucar.unidata.idv.repository;

import ucar.unidata.repository.*;


import org.w3c.dom.*;



import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.util.CacheManager;
import ucar.unidata.data.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
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


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class IdvOutputHandler extends OutputHandler {


    //    public static void processScript(String scriptFile) throws Exception {


    /** _more_ */
    public static final String OUTPUT_IDV = "idv.idv";

    IntegratedDataViewer idv;


    public static final Object MUTEX = new Object();

/**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public IdvOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        idv = new IntegratedDataViewer(false);
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
        return output.equals(OUTPUT_IDV);
    }


    protected void getOutputTypesForEntry(Request request,
                                          Entry entry, List types)
        throws Exception         {
        String type = entry.getTypeHandler().getType();
        if(type.equals("level3radar") || type.equals("level2radar")) {
            types.add(new TwoFacedObject("Preview Image", OUTPUT_IDV));        
        }
    }


    public Result outputEntry(Request request, Entry entry) throws Exception {
        if(!request.exists("doimage")) {
            StringBuffer sb = new StringBuffer();
            String url =    HtmlUtil.url(getRepository().URL_ENTRY_SHOW+"/preview.png", ARG_ID,
                                         entry.getId(),ARG_OUTPUT, OUTPUT_IDV,"doimage","true");
            
            request.put(ARG_OUTPUT,OutputHandler.OUTPUT_HTML);
            String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                                                             false,"");
            
            String      title = crumbs[0];
            sb.append(crumbs[1]);
            sb.append("&nbsp;<p>");
            sb.append(HtmlUtil.img(url));
            Result result =  new Result("Preview - " + title,sb);
            result.putProperty(
                               PROP_NAVSUBLINKS,
                               getHeader(
                                         request, OUTPUT_IDV,
                                         getRepository().getOutputTypesForEntry(request, entry)));
            return result;
        }

        String type = entry.getTypeHandler().getType();
        String thumbDir = getStorageManager().getThumbDir();
        File image = new File(IOUtil.joinDir(thumbDir,
                                      "preview_" + entry.getId() + ".png"));
        if(image.exists()) {
            //            return new Result("preview.png", new FileInputStream(image), "image/png");
        }
        

        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");
        String datasource;
        datasource = "FILE.RADAR";
        isl.append("<datasource type=\"" + datasource +"\" url=\"" + entry.getResource().getPath() +"\">\n");
        isl.append("<display type=\"planviewcolor\" param=\"#0\"><property name=\"id\" value=\"thedisplay\"/></display>\n");
        isl.append("</datasource>\n");
        //        isl.append("<center display=\"thedisplay\" useprojection=\"true\"/>\n");
        //        isl.append("<pause/>\n");
        //        isl.append("<display type=\"rangerings\" wait=\"false\"/>\n");
        isl.append("<image file=\"" + image+"\"/>\n");
        isl.append("</isl>\n");

        //        System.err.println(isl);
        synchronized(MUTEX) {
            ucar.unidata.util.Trace.startTrace();
            idv.getImageGenerator().processScriptFile("xml:" + isl);
            idv.cleanup();
            ucar.unidata.util.Trace.stopTrace();
        }
        return new Result("preview.png", new FileInputStream(image), "image/png");
    }
    

}


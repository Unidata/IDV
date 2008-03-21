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


import org.w3c.dom.*;

import ucar.unidata.sql.SqlUtil;



import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.repository.*;
import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.CacheManager;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
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

    /** _more_          */
    IntegratedDataViewer idv;

    /** _more_          */
    int callCnt = 0;


    /** _more_          */
    public static final Object IDV_MUTEX = new Object();

    /**
     *     _more_
     *    
     *     @param repository _more_
     *     @param element _more_
     *     @throws Exception _more_
     */
    public IdvOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        try {
            java.awt.GraphicsEnvironment e =
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            e.getDefaultScreenDevice();
        } catch (Throwable exc) {
            System.err.println(
                "To run the IdvOutputHandler a graphics environment is needed");
            throw new IllegalStateException(
                "To run the IdvOutputHandler a graphics environment is needed");
        }

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


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
    protected void xxxgetOutputTypesForGroup(Request request, Group group,
                                          List<Group> subGroups,
                                          List<Entry> entries, List types)

            throws Exception {
        getOutputTypesForEntries(request, entries,types);
    }


    protected void getOutputTypesForEntries(Request request,
                                            List<Entry> entries, List types)
            throws Exception {

        List<Entry> theEntries = getRadarEntries(entries);
        if(theEntries.size()>0) {
            types.add(new TwoFacedObject("Preview Image", OUTPUT_IDV));
        }
    }


    private List<Entry> getRadarEntries(List<Entry> entries) {
        List<Entry> theEntries = new ArrayList<Entry>();
        for(Entry entry: entries) {
            String type = entry.getTypeHandler().getType();
            if (type.equals("level3radar") || type.equals("level2radar")) {
                theEntries.add(entry);
            }
        }
        return theEntries;
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
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        
        final List<Entry> theEntries = getRadarEntries(entries);
        if(theEntries.size() == 0) {
            return new Result(msg("Image Preview"),  new StringBuffer("No radar entries found"));
        }

        Entry theEntry = null;
        String id = group.getId();
        if (group.isDummy()) {
            if(theEntries.size()==1) {
                theEntry = theEntries.get(0);
                id = theEntries.get(0).getId();
            } 
        }

        if ( !request.exists("doimage")) {
            StringBuffer sb = new StringBuffer();
            //TODO: the id is wrong if we are a search result
            String url = HtmlUtil.url(getRepository().URL_ENTRY_SHOW
                                      + "/preview.gif", ARG_ID,
                                      id, ARG_OUTPUT,
                                          OUTPUT_IDV, "doimage", "true");

            request.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML);
            String title="";
            if (!group.isDummy() || theEntry!=null) {
                String[] crumbs = getRepository().getBreadCrumbs(request,
                                                                 (theEntry!=null?theEntry:(Entry)group), false,"");
                title = crumbs[0];
                sb.append(crumbs[1]);
            }

            sb.append("&nbsp;<p>");
            sb.append(HtmlUtil.img(url));
            Result result = new Result("Preview - " + title, sb);
            result.putProperty(
                PROP_NAVSUBLINKS,
                getHeader(
                    request, OUTPUT_IDV,
                    getRepository().getOutputTypesForGroup(
                                                           request, group, subGroups, entries)));
            return result;
        }

        String thumbDir = getStorageManager().getThumbDir();
        File image = new File(IOUtil.joinDir(thumbDir,
                                             "preview_" + id.replace("/","_")
                                             + ".gif"));
        if (image.exists()) {
            return new Result("preview.gif", new FileInputStream(image),
                              "image/gif");
        }


        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"false\" loop=\"1\" offscreen=\"true\">\n");
        String datasource;
        datasource = "FILE.RADAR";
        //        isl.append("<datasource type=\"" + datasource + "\" url=\""
        //                   + entry.getResource().getPath() + "\">\n");
        isl.append("<datasource type=\"" + datasource + "\" >\n");
        int cnt = 0;
        for(Entry entry: theEntries) {
            isl.append("<fileset file=\"" + entry.getResource().getPath() +"\"/>\n");
        }
        isl.append(
            "<display type=\"planviewcolor\" param=\"#0\"><property name=\"id\" value=\"thedisplay\"/></display>\n");
        isl.append("</datasource>\n");
        //        isl.append("<center display=\"thedisplay\" useprojection=\"true\"/>\n");
        isl.append("<display type=\"rangerings\" wait=\"false\"/>\n");
        isl.append("<pause/>\n");
        //        isl.append("<pause seconds=\"60\"/>\n");

        if(cnt==1) {
            isl.append("<image file=\"" + image + "\"/>\n");
        } else {
            isl.append("<movie file=\"" + image + "\"/>\n");
        }
        isl.append("</isl>\n");
        //        System.out.println(isl);
        executeIsl(request, isl);
        return new Result("preview.png", new FileInputStream(image),
                          "image/png");
    }

    private void executeIsl(Request request, StringBuffer isl) throws Exception {
        //        System.err.println(isl);
        //For now just have one
        synchronized (IDV_MUTEX) {
            if (callCnt++ > 100) {
                idv     = null;
                callCnt = 0;
            }
            if (idv == null) {
                idv = new IntegratedDataViewer(false);
            }
            /*
            Trace.addNot(".*Shadow.*");
            Trace.addNot(".*Azimuth.*");
            Trace.addNot(".*Set\\(.*");
            Trace.addNot(".*ProjectionCoord.*");
            Trace.addNot(".*Display_List.*");
            Trace.addNot(".*MapProjection.*");
            Trace.startTrace();
            Trace.call1("Make image");*/
            idv.getImageGenerator().processScriptFile("xml:" + isl);
            //            Trace.call2("Make image");
            idv.cleanup();
            //            ucar.unidata.util.Trace.stopTrace();
        }

    }



}


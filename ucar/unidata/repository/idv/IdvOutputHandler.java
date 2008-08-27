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


import org.w3c.dom.*;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSourceDescriptor;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.IdvServer;

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
    IdvServer idvServer;

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
            idvServer = new IdvServer();
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




    protected void addOutputTypes(Request request,
                                  State state, 
                                  List<OutputType> types) throws Exception {
        List<Entry> theEntries = null;
        if(state.entry!=null) {
            DataSourceDescriptor descriptor = getDescriptor(state.entry);
            if(descriptor!=null) {
                types.add(new OutputType("Preview " + descriptor.getLabel(), OUTPUT_IDV));
                return;
            }

            /*
              if(state.entry.getResource().getPath().endsWith(".shp")) {
              types.add(new TwoFacedObject("Preview Shapefile", OUTPUT_IDV));
              return;
              }
            */
            theEntries = new ArrayList<Entry>();
            theEntries.add(state.entry);
        } else {
            theEntries = getRadarEntries(state.getAllEntries());
        }

        if(theEntries!=null && theEntries.size()>0) {
            types.add(new OutputType("Preview Radar", OUTPUT_IDV));
        }
    }


    private IntegratedDataViewer getIdv() throws Exception  {
        return idvServer.getIdv();
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

    private DataSourceDescriptor getDescriptor(Entry entry ) throws Exception {
        String path = entry.getResource().getPath();
        if(path.length()>0) {
            List<DataSourceDescriptor> descriptors = getIdv().getDataManager().getDescriptors();
            for(DataSourceDescriptor descriptor: descriptors) {
                if(descriptor.getPatternFileFilter()!=null && descriptor.getPatternFileFilter().match(path)) {
                    return descriptor;
                }
            }
        }
        return null;
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
        
        final List<Entry> radarEntries = getRadarEntries(entries);
        Entry theEntry = null;
        String id = group.getId();
        if (group.isDummy()) {
            if(entries.size()==1) {
                theEntry = entries.get(0);
                id = entries.get(0).getId();
            } 
        }

        StringBuffer sb = new StringBuffer();



        if (!request.exists("doimage")) {

            //TODO: the id is wrong if we are a search result
            String url = HtmlUtil.url(getRepository().URL_ENTRY_SHOW
                                      + "/" + theEntry.getId() +"_preview.gif", ARG_ID,
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


            DataSourceDescriptor descriptor = getDescriptor(theEntry);
            if(false && descriptor!=null) {
                DataSource dataSource = getIdv().makeOneDataSource(theEntry.getResource().getPath(),   descriptor.getId(), null);
                if(dataSource!=null) {
                    sb.append(dataSource.getFullDescription());
                    Result result = new Result("Metadata - " + title, sb);
                    addLinks(request, result,new State(group, subGroups, entries));
                    return result;
                }
            }

            sb.append("&nbsp;<p>");
            sb.append(HtmlUtil.img(url));
            Result result = new Result("Preview - " + title, sb);
            addLinks(request, result,new State(group, subGroups, entries));
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
        String datasource="";
        if(radarEntries.size()>0) {
            datasource = "FILE.RADAR";
        } else if(theEntry.getResource().getPath().endsWith(".shp")) {
            datasource = "FILE.SHAPEFILE";
        } else {
            String path = theEntry.getResource().getPath();
            if(path.length()>0) {
                List<DataSourceDescriptor> descriptors = getIdv().getDataManager().getDescriptors();
                for(DataSourceDescriptor descriptor: descriptors) {
                    if(descriptor.getPatternFileFilter()!=null && descriptor.getPatternFileFilter().match(path)) {
                        datasource =  descriptor.getId();
                    }
                }
            }
        }
        //        isl.append("<datasource type=\"" + datasource + "\" url=\""
        //                   + entry.getResource().getPath() + "\">\n");
        isl.append("<datasource type=\"" + datasource + "\" >\n");
        int cnt = 0;
        for(Entry entry: entries) {
            isl.append("<fileset file=\"" + entry.getResource().getPath() +"\"/>\n");
        }
        //        System.err.println ("datasource:" + datasource);
        if(datasource.equalsIgnoreCase("FILE.RADAR")) {
            isl.append(
                       "<display type=\"planviewcolor\" param=\"#0\"><property name=\"id\" value=\"thedisplay\"/></display>\n");
            isl.append("</datasource>\n");
            //        isl.append("<center display=\"thedisplay\" useprojection=\"true\"/>\n");
            isl.append("<display type=\"rangerings\" wait=\"false\"/>\n");
        } else if(datasource.equalsIgnoreCase("FILE.SHAPEFILE")) {
            isl.append(
                       "<display type=\"shapefilecontrol\" param=\"#0\"><property name=\"id\" value=\"thedisplay\"/></display>\n");
            isl.append("</datasource>\n");
        } else if(datasource.equalsIgnoreCase("FILE.AREAFILE")) {
            isl.append(
                       "<display type=\"imagedisplay\" param=\"#0\"><property name=\"id\" value=\"thedisplay\"/></display>\n");
            isl.append("</datasource>\n");
        } else {
            isl.append("</datasource>\n");
        }
        isl.append("<pause/>\n");
        //        isl.append("<pause seconds=\"60\"/>\n");

        if(cnt==1) {
            isl.append("<image file=\"" + image + "\"/>\n");
        } else {
            isl.append("<movie file=\"" + image + "\"/>\n");
        }
        isl.append("</isl>\n");
        //        System.out.println(isl);
        idvServer.evaluateIsl(isl);
        return new Result("preview.png", new FileInputStream(image),
                          "image/png");
    }


}


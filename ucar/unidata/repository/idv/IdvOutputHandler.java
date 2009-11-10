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


import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.dataset.VariableEnhanced;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.TypedDatasetFactory;

import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.NetcdfCFWriter;

import ucar.unidata.data.DataManager;

import ucar.unidata.data.*;
import ucar.unidata.data.grid.*;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.idv.IdvServer;


import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.ImageGenerator;

import ucar.unidata.repository.*;
import ucar.unidata.repository.data.*;
import ucar.unidata.repository.output.*;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.Trace;
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


import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Date;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class IdvOutputHandler extends OutputHandler {
    public static final String ARG_PARAM = "param";
    public static final String ARG_DISPLAY = "display";
    public static final String ARG_ACTION = "action";
    public static final String ARG_TIME = "time";

    public static final String ARG_IMAGE_WIDTH = "imagewidth";
    public static final String ARG_IMAGE_HEIGHT = "imageheight";



    public static final String ACTION_MAKEFORM = "action.makeform";
    public static final String ACTION_MAKEPAGE = "action.makepage";
    public static final String ACTION_MAKEIMAGE = "action.makeimage";


    //    public static void processScript(String scriptFile) throws Exception {


    /** _more_ */
    public static final OutputType OUTPUT_IDV_GRID =
        new OutputType("Grid Preview", "idv.grid", OutputType.TYPE_HTML);


    /** _more_ */
    IdvServer idvServer;

    /** _more_ */
    int callCnt = 0;


    /** _more_ */
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
            idvServer = new IdvServer(new File(getStorageManager().getDir("idv")));
            addType(OUTPUT_IDV_GRID);
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
     * @return _more_
     *
     * @throws Exception _more_
     */
    public DataOutputHandler getDataOutputHandler() throws Exception {
        return (DataOutputHandler) getRepository().getOutputHandler(
            DataOutputHandler.OUTPUT_OPENDAP);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {

        List<Entry> theEntries = null;
        if (state.entry != null) {
            if ( !getDataOutputHandler().canLoadAsGrid(state.entry)) {
                return;
            }
            links.add(makeLink(request, state.getEntry(), OUTPUT_IDV_GRID));
        } else {
            //            theEntries = getRadarEntries(state.getAllEntries());
        }

        /*        if(theEntries!=null && theEntries.size()>0) {
            types.add(OUTPUT_IDV);
            }*/
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private IntegratedDataViewer getIdv() throws Exception {
        return idvServer.getIdv();
    }



    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     */
    private List<Entry> getRadarEntries(List<Entry> entries) {
        List<Entry> theEntries = new ArrayList<Entry>();
        for (Entry entry : entries) {
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
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private DataSourceDescriptor getDescriptor(Entry entry) throws Exception {
        String path = entry.getResource().getPath();
        if (path.length() > 0) {
            List<DataSourceDescriptor> descriptors =
                getIdv().getDataManager().getDescriptors();
            for (DataSourceDescriptor descriptor : descriptors) {
                if ((descriptor.getPatternFileFilter() != null)
                        && descriptor.getPatternFileFilter().match(path)) {
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
    public Result outputEntry(final Request request, Entry entry)
            throws Exception {

        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_IDV_GRID)) {
            return outputGrid(request, entry);
        }
        return super.outputEntry(request, entry);
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
    public Result outputGrid(final Request request, Entry entry)
            throws Exception {
	String action = request.getString(ARG_ACTION, ACTION_MAKEFORM);


        DataOutputHandler dataOutputHandler = getDataOutputHandler();
        String path = dataOutputHandler.getPath(entry);
        if (path == null) {
	    StringBuffer sb  =new StringBuffer();
            sb.append("Could not load grid");
            return new Result("Grid Preview", sb);
        }

        GridDataset dataset =
            dataOutputHandler.getGridDataset(entry,
                                             path);


        GeoGridDataSource dataSource = new GeoGridDataSource(dataset);

	try {
	    if(action.equals(ACTION_MAKEFORM)) {
		return outputGridForm(request, entry, dataSource);
	    } else	if(action.equals(ACTION_MAKEPAGE)) {
		return outputGridPage(request, entry, dataSource);
	    } else {
		return outputGridImage(request, entry, dataSource);
	    }
	} finally {
	    dataOutputHandler.returnGridDataset(path, dataset);
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
    private Result outputGridForm(final Request request, Entry entry, GeoGridDataSource dataSource)
            throws Exception {
        StringBuffer sb   = new StringBuffer();

        String       formUrl  = getRepository().URL_ENTRY_SHOW.getFullUrl();
        sb.append(HtmlUtil.form(formUrl, ""));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT,OUTPUT_IDV_GRID));
        sb.append(HtmlUtil.hidden(ARG_ACTION,ACTION_MAKEPAGE));



        List<DataChoice> choices = (List<DataChoice>)dataSource.getDataChoices();            

	List<String> tabLabels = new ArrayList<String>();
	List<String> tabContents = new ArrayList<String>();
	List displays = new ArrayList();
	displays.add(new TwoFacedObject("Contour Plan View","planviewcontour"));
	displays.add(new TwoFacedObject("Color Filled Contour Plan View","planviewcontourfilled"));
	displays.add(new TwoFacedObject("Color Shaded Plan View","planviewcolor"));
	for(int i=1;i<=3;i++) {
	    StringBuffer tab = new StringBuffer();
	    tab.append(HtmlUtil.formTable());
	    List options = new ArrayList();
	    options.add("");
	    for(DataChoice dataChoice: choices) {
		options.add(new TwoFacedObject(dataChoice.getDescription(), dataChoice.getName()));
	    }
	    tab.append(HtmlUtil.formEntry(msgLabel("Parameter"), HtmlUtil.select(ARG_PARAM+i, options)));
	    tab.append(HtmlUtil.formEntry(msgLabel("Display Type"), HtmlUtil.select(ARG_DISPLAY+i, displays)));
	    tab.append(HtmlUtil.formTableClose());
	    tabLabels.add(msg("Display " + i));
	    tabContents.add(tab.toString());
	}
        sb.append(HtmlUtil.makeTabs(tabLabels, tabContents,true,"tab_content"));

        sb.append(HtmlUtil.submit(msg("Make image"),ARG_SUBMIT));
        sb.append(HtmlUtil.formClose());
        return new Result("Grid Preview", sb);
    }




    private Result outputGridPage(final Request request, Entry entry, GeoGridDataSource dataSource)
	throws Exception {
	StringBuffer sb = new StringBuffer();

	int displayIdx=1;
	String url = getRepository().URL_ENTRY_SHOW.getFullUrl();
	List<String> args = new ArrayList<String>();
        args.add(ARG_ENTRYID);
	args.add(entry.getId());
        args.add(ARG_OUTPUT);
	args.add(OUTPUT_IDV_GRID.toString());
        args.add(ARG_ACTION);
	args.add(ACTION_MAKEIMAGE);
	while(request.defined(ARG_PARAM+displayIdx)) {
	    args.add(ARG_DISPLAY+displayIdx);
	    args.add(request.getString(ARG_DISPLAY+displayIdx,""));
	    args.add(ARG_PARAM+displayIdx);
	    args.add(request.getString(ARG_PARAM+displayIdx,""));
	    displayIdx++;
	}
	sb.append(HtmlUtil.img(HtmlUtil.url(url, args)));

	return new Result("Grid Preview", sb);
    }


    public Result outputGridImage(final Request request, Entry entry, GeoGridDataSource dataSource)
	throws Exception {
	Trace.addNot(".*ShadowFunction.*");
	Trace.addNot(".*GeoGrid.*");
	//	Trace.addOnly(".*MapProjection.*");
	//	Trace.addOnly(".*ProjectionCoordinateSystem.*");
	Trace.startTrace();
        String id = entry.getId();
        File image = getStorageManager().getThumbFile("preview_"
                         + id.replace("/", "_") + ".gif");
        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");

	//Create a new viewmanager
	isl.append("<view><property name=\"wireframe\" value=\"false\"/><property name=\"showMaps\" value=\"false\"/></view>\n");

        isl.append("<datasource id=\"datasource\" times=\"0\" >\n");
	int displayIdx=1;
	Hashtable props = new Hashtable();
	props.put("datasource", dataSource);
	while(request.defined(ARG_PARAM+displayIdx)) {
	    String times = "1";
	    String levels = XmlUtil.attrs(ImageGenerator.ATTR_LEVEL_FROM,"#0",
					  ImageGenerator.ATTR_LEVEL_TO,"#1");
	    String attrs = XmlUtil.attrs(ImageGenerator.ATTR_TIMES, times,
					 ImageGenerator.ATTR_TYPE, request.getString(ARG_DISPLAY+displayIdx,""),
					 ImageGenerator.ATTR_PARAM,request.getString(ARG_PARAM+displayIdx,""));
	    isl.append(XmlUtil.tag("display", attrs+levels,XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
								       XmlUtil.attrs("name","id",
										     "value","thedisplay"+displayIdx))));
	    displayIdx++;
	}

        isl.append("</datasource>\n");
	//        isl.append("<pause/>\n");
	String clip = XmlUtil.tag("clip","");
	clip = "";
        isl.append(XmlUtil.tag("image", XmlUtil.attr("file",  image.toString()), clip));
        isl.append("</isl>\n");
        //        System.out.println(isl);

	long t1 = System.currentTimeMillis();
        idvServer.evaluateIsl(isl,props);
	long t2 = System.currentTimeMillis();
	System.err.println("isl time:" + (t2-t1));

	Trace.stopTrace();
        return new Result("preview.gif",
                          getStorageManager().getFileInputStream(image),
                          "image/gif");

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param suGbroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result xxxoutputGroup(Request request, Group group,
                                 List<Group> subGroups, List<Entry> entries)
            throws Exception {

        final List<Entry> radarEntries = getRadarEntries(entries);
        Entry             theEntry     = null;
        String            id           = group.getId();
        if (group.isDummy()) {
            if (entries.size() == 1) {
                theEntry = entries.get(0);
                id       = entries.get(0).getId();
            }
        }

        StringBuffer sb = new StringBuffer();

        if ( !request.exists("doimage")) {
            //TODO: the id is wrong if we are a search result
            String url =
                HtmlUtil.url(
                    getRepository().URL_ENTRY_SHOW + "/" + theEntry.getId()
                    + "_preview.gif", ARG_ENTRYID, id, ARG_OUTPUT,
                                      OUTPUT_IDV_GRID, "doimage", "true");

            request.put(ARG_OUTPUT, OutputHandler.OUTPUT_HTML);
            String title = "";
            if ( !group.isDummy() || (theEntry != null)) {
                String[] crumbs = getEntryManager().getBreadCrumbs(request,
                                      ((theEntry != null)
                                       ? theEntry
                                       : (Entry) group), false);
                title = crumbs[0];
                sb.append(crumbs[1]);
            }


            DataSourceDescriptor descriptor = getDescriptor(theEntry);
            if (false && (descriptor != null)) {
                DataSource dataSource = getIdv().makeOneDataSource(
                                            theEntry.getResource().getPath(),
                                            descriptor.getId(), null);
                if (dataSource != null) {
                    sb.append(dataSource.getFullDescription());
                    Result result = new Result("Metadata - " + title, sb);
                    addLinks(request, result,
                             new State(group, subGroups, entries));
                    return result;
                }
            }

            sb.append("&nbsp;<p>");
            sb.append(HtmlUtil.img(url));
            Result result = new Result("Preview - " + title, sb);
            addLinks(request, result, new State(group, subGroups, entries));
            return result;
        }

        File image = getStorageManager().getThumbFile("preview_"
                         + id.replace("/", "_") + ".gif");
        if (image.exists()) {
            return new Result("preview.gif",
                              getStorageManager().getFileInputStream(image),
                              "image/gif");
        }


        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"false\" loop=\"1\" offscreen=\"true\">\n");
        String datasource = "";
        if (radarEntries.size() > 0) {
            datasource = "FILE.RADAR";
        } else if (theEntry.getResource().getPath().endsWith(".shp")) {
            datasource = "FILE.SHAPEFILE";
        } else {
            String path = theEntry.getResource().getPath();
            if (path.length() > 0) {
                List<DataSourceDescriptor> descriptors =
                    getIdv().getDataManager().getDescriptors();
                for (DataSourceDescriptor descriptor : descriptors) {
                    if ((descriptor.getPatternFileFilter() != null)
                            && descriptor.getPatternFileFilter().match(
                                path)) {
                        datasource = descriptor.getId();
                    }
                }
            }
        }
        //        isl.append("<datasource type=\"" + datasource + "\" url=\""
        //                   + entry.getResource().getPath() + "\">\n");
        isl.append("<datasource type=\"" + datasource + "\" >\n");
        int cnt = 0;
        for (Entry entry : entries) {
            isl.append("<fileset file=\"" + entry.getResource().getPath()
                       + "\"/>\n");
        }
        //        System.err.println ("datasource:" + datasource);
        if (datasource.equalsIgnoreCase("FILE.RADAR")) {
            isl.append(
                "<display type=\"planviewcolor\" param=\"#0\"><property name=\"id\" value=\"thedisplay\"/></display>\n");
            isl.append("</datasource>\n");
            //        isl.append("<center display=\"thedisplay\" useprojection=\"true\"/>\n");
            isl.append("<display type=\"rangerings\" wait=\"false\"/>\n");
        } else if (datasource.equalsIgnoreCase("FILE.SHAPEFILE")) {
            isl.append(
                "<display type=\"shapefilecontrol\" param=\"#0\"><property name=\"id\" value=\"thedisplay\"/></display>\n");
            isl.append("</datasource>\n");
        } else if (datasource.equalsIgnoreCase("FILE.AREAFILE")) {
            isl.append(
                "<display type=\"imagedisplay\" param=\"#0\"><property name=\"id\" value=\"thedisplay\"/></display>\n");
            isl.append("</datasource>\n");
        } else {
            isl.append("</datasource>\n");
        }
        isl.append("<pause/>\n");
        //        isl.append("<pause seconds=\"60\"/>\n");

        if (cnt == 1) {
            isl.append("<image file=\"" + image + "\"/>\n");
        } else {
            isl.append("<movie file=\"" + image + "\"/>\n");
        }
        isl.append("</isl>\n");
        //        System.out.println(isl);
        idvServer.evaluateIsl(isl);
        return new Result("preview.png",
                          getStorageManager().getFileInputStream(image),
                          "image/png");
    }


}


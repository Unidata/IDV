/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

import ucar.nc2.ft.FeatureDatasetPoint;

import ucar.unidata.data.*;
import ucar.unidata.data.DataCategory;

import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.gis.WmsSelection;
import ucar.unidata.data.grid.*;
import ucar.unidata.data.point.NetcdfPointDataSource;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.gis.maps.MapData;
import ucar.unidata.idv.*;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.DisplayConventions;


import ucar.unidata.idv.IdvBase;
import ucar.unidata.idv.IdvServer;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.VMManager;
import ucar.unidata.idv.ViewState;
import ucar.unidata.idv.ui.ImageGenerator;

import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.data.*;
import ucar.unidata.repository.metadata.*;
import ucar.unidata.repository.output.*;
import ucar.unidata.repository.util.*;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;

import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.util.CacheManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.ThreeDSize;
import ucar.unidata.util.Trace;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import visad.Unit;


import java.awt.Color;

import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;


import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class IdvOutputHandler extends OutputHandler {

    /** _more_          */
    public static final String METADATA_TYPE_VISUALIZATION =
        "data.visualization";

    /** _more_ */
    public static final String ARG_PRODUCT = "product";

    public static final String ARG_AZIMUTH = "azimuth";
    public static final String ARG_TILT = "tilt";
    public static final String ARG_WIREFRAME = "wireframe";
    public static final String ARG_VIEWDIR = "viewdir";

    public static final String ARG_LATLON_VISIBLE = "latlon.visible";
    public static final String ARG_LATLON_SPACING = "latlon.spacing";


    public static final String ARG_LAT1 = "lat1";
    public static final String ARG_LON1 = "lon1";
    public static final String ARG_LAT2 = "lat2";
    public static final String ARG_LON2 = "lon2";

    /** _more_          */
    public static final String ARG_SUBMIT_SAVE = "submit.save";

    /** _more_          */
    public static final String ARG_SAVE_ATTACH = "save.attach";

    /** _more_          */
    public static final String ARG_SAVE_NAME = "save.name";

    /** _more_          */
    public static final String ARG_PREDEFINED = "predefined";

    /** _more_ */
    public static final String PRODUCT_IMAGE = "product.image";


    /** _more_ */
    public static final String PRODUCT_MOV = "product.mov";

    /** _more_ */
    public static final String PRODUCT_KMZ = "product.kmz";

    /** _more_          */
    public static final String PRODUCT_IDV = "product.idv";

    /** _more_          */
    public static final String PRODUCT_ISL = "product.isl";

    /** _more_ */
    private static TwoFacedObject[] products = { new TwoFacedObject("Image",
                                                   PRODUCT_IMAGE),
            new TwoFacedObject("Quicktime Movie", PRODUCT_MOV),
            new TwoFacedObject("Google Earth KMZ", PRODUCT_KMZ) };



    /** _more_          */
    public static final String ARG_SUBMIT_PUBLISH = "submit.publish";

    /** _more_ */
    public static final String ARG_PUBLISH_ENTRY = "publish.entry";

    /** _more_ */
    public static final String ARG_PUBLISH_NAME = "publish.name";

    /** _more_ */
    public static final String ARG_PUBLISH_DESCRIPTION =
        "publish.description";




    /** _more_ */
    public static final String ARG_VIEW_GLOBE = "globe";

    /** _more_ */
    public static final String ARG_VIEW_PROJECTION = "proj";

    /** _more_ */
    public static final String ARG_VIEW_VIEWPOINT = "viewpoint";

    /** _more_ */
    public static final String ARG_VIEW_BOUNDS = "bounds";

    /** _more_ */
    public static final String ARG_VIEW_JUSTCLIP = "justclip";

    /** _more_ */
    public static final String ARG_VIEW_BACKGROUNDIMAGE = "backgroundimage";



    /** _more_ */
    public static final String ARG_PARAM = "param";

    /** _more_ */
    public static final String ARG_TARGET = "target";

    /** _more_ */
    public static final String TARGET_IMAGE = "image";

    /** _more_ */
    public static final String TARGET_JNLP = "jnlp";

    /** _more_ */
    public static final String TARGET_ISL = "isl";

    /** _more_ */
    public static final String ARG_ZOOM = "zoom";


    /** _more_ */
    public static final String ARG_POINT_LAYOUTMODEL = "layoutmodel";

    /** _more_ */
    public static final String ARG_POINT_DOANIMATION = "doanimation";

    /** _more_ */
    public static final String ARG_DISPLAYLISTLABEL = "dll";

    /** _more_ */
    public static final String ARG_DISPLAYCOLOR = "clr";

    /** _more_ */
    public static final String ARG_COLORTABLE = "ct";

    /** _more_ */
    public static final String ARG_STRIDE = "stride";


    /** _more_ */
    public static final String ARG_FLOW_SCALE = "f_s";

    /** _more_ */
    public static final String ARG_FLOW_DENSITY = "f_d";

    /** _more_ */
    public static final String ARG_FLOW_SKIP = "f_sk";

    /** _more_ */
    public static final String ARG_DISPLAYUNIT = "unit";

    /** _more_ */
    public static final String ARG_ISOSURFACEVALUE = "iso_value";

    /** _more_ */
    public static final String ARG_CONTOUR_WIDTH = "c_w";

    /** _more_ */
    public static final String ARG_CONTOUR_MIN = "c_mn";

    /** _more_ */
    public static final String ARG_CONTOUR_MAX = "c_mx";

    /** _more_ */
    public static final String ARG_CONTOUR_INTERVAL = "c_int";

    /** _more_ */
    public static final String ARG_CONTOUR_BASE = "c_b";

    /** _more_ */
    public static final String ARG_CONTOUR_DASHED = "c_d";

    /** _more_ */
    public static final String ARG_CONTOUR_LABELS = "c_l";


    /** _more_ */
    public static final String ARG_SCALE_VISIBLE = "s_v";

    /** _more_ */
    public static final String ARG_SCALE_ORIENTATION = "s_o";

    /** _more_ */
    public static final String ARG_SCALE_PLACEMENT = "s_p";


    /** _more_ */
    public static final String ARG_RANGE_MIN = "r_mn";

    /** _more_ */
    public static final String ARG_RANGE_MAX = "r_mx";

    /** _more_ */
    public static final String ARG_DISPLAY = "dsp";

    /** _more_ */
    public static final String ARG_ACTION = "action";

    /** _more_ */
    public static final String ARG_TIMES = "times";


    /** _more_ */
    public static final String ARG_MAPS = "maps";

    /** _more_ */
    public static final String ARG_MAPWIDTH = "mapwidth";

    /** _more_ */
    public static final String ARG_MAPCOLOR = "mapcolor";



    /** _more_ */
    public static final String ARG_CLIP = "clip";

    /** _more_ */
    public static final String ARG_VIEW_BACKGROUND = "bg";

    /** _more_ */
    public static final String ARG_LEVELS = "levels";

    /** _more_ */
    public static final String ARG_IMAGE_WIDTH = "width";

    /** _more_ */
    public static final String ARG_IMAGE_HEIGHT = "height";


    /** _more_          */
    private static final String[] NOTARGS = {
        ARG_SUBMIT_SAVE, ARG_SUBMIT_PUBLISH, ARG_PUBLISH_NAME,
        ARG_PUBLISH_ENTRY, ARG_PUBLISH_ENTRY + "_hidden",
        ARG_PUBLISH_DESCRIPTION, ARG_SAVE_ATTACH, ARG_SAVE_NAME, ARG_ACTION
    };

    /** _more_          */
    private Hashtable exceptArgs = new Hashtable();

    /** _more_ */
    private Properties valueToAbbrev;

    /** _more_ */
    private Properties keyToAbbrev;

    /** _more_ */
    public static final String ACTION_ERROR = "action.error";


    /** _more_ */
    public static final String ACTION_MAKEINITFORM = "action.makeinitform";

    /** _more_ */
    public static final String ACTION_MAKEFORM = "action.makeform";


    /** _more_ */
    public static final String ACTION_MAKEPAGE = "action.makepage";

    /** _more_ */
    public static final String ACTION_MAKEIMAGE = "action.makeimage";


    /** _more_ */
    public static final String ACTION_POINT_MAKEPAGE =
        "action.point.makepage";

    /** _more_ */
    public static final String ACTION_POINT_MAKEIMAGE =
        "action.point.makeimage";



    public static final String DISPLAY_XS_CONTOUR = "contourxs";
    public static final String DISPLAY_XS_COLOR = "colorxs";
    public static final String DISPLAY_XS_FILLEDCONTOUR = "contourxsfilled";



    /** _more_ */
    public static final String DISPLAY_PLANVIEWFLOW = "planviewflow";

    /** _more_ */
    public static final String DISPLAY_STREAMLINES = "streamlines";

    /** _more_ */
    public static final String DISPLAY_WINDBARBPLAN = "windbarbplan";

    /** _more_ */
    public static final String DISPLAY_PLANVIEWCONTOUR = "planviewcontour";

    /** _more_ */
    public static final String DISPLAY_PLANVIEWCONTOURFILLED =
        "planviewcontourfilled";

    /** _more_ */
    public static final String DISPLAY_PLANVIEWCOLOR = "planviewcolor";

    /** _more_ */
    public static final String DISPLAY_ISOSURFACE = "isosurface";



    //    public static void processScript(String scriptFile) throws Exception {


    /** _more_ */
    public static final OutputType OUTPUT_IDV_GRID =
        new OutputType("Grid Displays", "idv.grid", OutputType.TYPE_HTML,
                       OutputType.SUFFIX_NONE, ICON_PLANVIEW);


    /** _more_ */
    public static final OutputType OUTPUT_IDV_POINT =
        new OutputType("Point Displays", "idv.point", OutputType.TYPE_HTML,
                       OutputType.SUFFIX_NONE, ICON_PLANVIEW);


    /** _more_ */
    IdvServer idvServer;

    /** _more_ */
    int callCnt = 0;

    /** _more_ */
    private List backgrounds;

    /** _more_ */
    private HashSet<String> okControls = new HashSet<String>();

    private HashSet<String> vertControls = new HashSet<String>();


    /** _more_ */
    private boolean idvOk = false;

    /** _more_ */
    private Hashtable<String, File> imageCache = new Hashtable<String,
                                                     File>();

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

        okControls.add(DISPLAY_XS_CONTOUR);
        okControls.add(DISPLAY_XS_FILLEDCONTOUR);
        okControls.add(DISPLAY_XS_COLOR);
        vertControls.add(DISPLAY_XS_CONTOUR);
        vertControls.add(DISPLAY_XS_FILLEDCONTOUR);
        vertControls.add(DISPLAY_XS_COLOR);
        vertControls.add(DISPLAY_ISOSURFACE);
        okControls.add("planviewflow");
        okControls.add("streamlines");
        okControls.add("windbarbplan");
        okControls.add("planviewcontour");
        okControls.add("planviewcontourfilled");
        okControls.add("planviewcolor");
        okControls.add("valuedisplay");
        okControls.add("isosurface");
        okControls.add("volumerender");
        okControls.add("pointvolumerender");
        for (String notArg : NOTARGS) {
            exceptArgs.put(notArg, "");
        }

        valueToAbbrev = new Properties();
        keyToAbbrev   = new Properties();
        try {

            /*
            valueToAbbrev.load(
                               IOUtil.getInputStream(
                                                     "/ucar/unidata/repository/idv/values.properties",
                                                     getClass()));
            keyToAbbrev.load(
                             IOUtil.getInputStream(
                                                   "/ucar/unidata/repository/idv/keys.properties",
                                                   getClass()));
            */

        } catch (Exception exc) {
            exc.printStackTrace();
        }
        //Call this in a thread because if there is any problem with xvfb this will just hang
        //Run in a couple of seconds because we are deadlocking deep down in Java on the mac
        Misc.runInABit(2000,this, "checkIdv",null);
    }


    /**
     * _more_
     */
    public void checkIdv() {
        try {
            //See if we have a graphics environment
            java.awt.GraphicsEnvironment e =
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            e.getDefaultScreenDevice();
            idvServer =
                new IdvServer(new File(getStorageManager().getDir("idv")));
            idvOk = true;
            //Only add the output types after we create the server
            addType(OUTPUT_IDV_GRID);
            addType(OUTPUT_IDV_POINT);
        } catch (Throwable exc) {
            logError(
                "To run the IdvOutputHandler a graphics environment is needed",
                exc);
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
            DataOutputHandler.OUTPUT_OPENDAP.toString());
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

        if ( !idvOk) {
            return;
        }

        List<Entry> theEntries = null;
        if (state.entry != null) {
            if ( !getDataOutputHandler().canLoadAsGrid(state.entry)) {
                if (getDataOutputHandler().canLoadAsPoint(state.entry)) {
                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_IDV_POINT));
                }
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
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {

        OutputType output = request.getOutput();
        if (output.equals(OUTPUT_IDV_GRID)) {
            return outputGrid(request, entry);
        }
        if (output.equals(OUTPUT_IDV_POINT)) {
            return outputPoint(request, entry);
        }
        return super.outputEntry(request, outputType, entry);
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
        DataOutputHandler dataOutputHandler = getDataOutputHandler();
        String action = request.getString(ARG_ACTION, ACTION_MAKEINITFORM);
        String            path              =
            dataOutputHandler.getPath(entry);
        if (path == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Could not load grid");
            return new Result("Grid Displays", sb);
        }

        GridDataset dataset = dataOutputHandler.getGridDataset(entry, path);
        DataSourceDescriptor descriptor =
            idvServer.getIdv().getDataManager().getDescriptor("File.Grid");
        DataSource dataSource = new GeoGridDataSource(descriptor, dataset,
                                    entry.getName(), path);

        try {
            if (action.equals(ACTION_MAKEINITFORM)) {
                return outputGridInitForm(request, entry, dataSource);
            } else if (action.equals(ACTION_MAKEFORM)) {
                return outputGridForm(request, entry, dataSource);
            } else if (action.equals(ACTION_MAKEPAGE)) {
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
     * @param dataSource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputGridForm(final Request request, Entry entry,
                                  DataSource dataSource)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        makeGridForm(request, sb, entry, dataSource);
        return new Result("Grid Displays", sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String htmlCheckbox(Request request, String arg, boolean dflt) {
        boolean value = dflt;
        if (request.exists(arg)) {
            value = request.get(arg, dflt);
        } else if (request.exists(arg + "_gvdflt")) {
            value = false;
        }
        return HtmlUtil.checkbox(arg, "true", value)
               + HtmlUtil.hidden(arg + "_gvdflt", "" + value);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param items _more_
     * @param selectFirstOne _more_
     * @param extra _more_
     *
     * @return _more_
     */
    private String htmlSelect(Request request, String arg, List items,
                              boolean selectFirstOne, String extra) {
        List selected = request.get(arg, new ArrayList());
        if ((selected.size() == 0) && selectFirstOne && (items.size() > 0)) {
            selected.add(items.get(0));
        }
        return HtmlUtil.select(arg, items, selected, extra);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param items _more_
     * @param extra _more_
     *
     * @return _more_
     */
    private String htmlSelect(Request request, String arg, List items,
                              String extra) {
        return htmlSelect(request, arg, items, false, extra);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param items _more_
     *
     * @return _more_
     */
    private String htmlSelect(Request request, String arg, List items) {
        return htmlSelect(request, arg, items, "");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param dflt _more_
     * @param width _more_
     *
     * @return _more_
     */
    private String htmlInput(Request request, String arg, String dflt,
                             int width) {
        return HtmlUtil.input(arg, request.getString(arg, dflt),
                              HtmlUtil.attr(HtmlUtil.ATTR_SIZE, "" + width));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String htmlInput(Request request, String arg, String dflt) {
        return htmlInput(request, arg, dflt, 5);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param entry _more_
     * @param dataSource _more_
     *
     * @throws Exception _more_
     */
    private void makeGridForm(Request request, StringBuffer sb, Entry entry,
                              DataSource dataSource)
            throws Exception {

        String formUrl = getRepository().URL_ENTRY_SHOW.getFullUrl();
        sb.append(HtmlUtil.form(formUrl, ""));
        sb.append(HtmlUtil.submit(msg("Make image"), ARG_SUBMIT));
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_IDV_GRID));
        sb.append(HtmlUtil.hidden(ARG_ACTION, ACTION_MAKEPAGE));

        StringBuffer basic = new StringBuffer();
        basic.append(HtmlUtil.formTable());

        basic.append(
            HtmlUtil.formEntry(
                msgLabel("Product"),
                htmlSelect(request, ARG_PRODUCT, Misc.toList(products))
                + HtmlUtil.space(2)
                + msg("Note: For KMZ make sure to set the view bounds")));


        String viewPointHtml = "";
        List   vms           = idvServer.getIdv().getVMManager().getVMState();
        if (vms.size() >= 0) {
            List viewPoints = new ArrayList<String>();
            viewPoints.add(new TwoFacedObject("--none--", ""));
            for (int i = 0; i < vms.size(); i++) {
                ViewState viewState = (ViewState) vms.get(i);
                viewPoints.add(viewState.getName());
            }
            viewPointHtml = msgLabel("Viewpoint") + HtmlUtil.space(2)
                            + htmlSelect(request, ARG_VIEW_VIEWPOINT,
                                         viewPoints);
        }

        basic.append(HtmlUtil.formEntry(msgLabel("Image Size"),
                                        htmlInput(request, ARG_IMAGE_WIDTH,
                                            "600") + HtmlUtil.space(1) + "X"
                                                + HtmlUtil.space(1)
                                                    + htmlInput(request,
                                                        ARG_IMAGE_HEIGHT,
                                                            "400")));



        basic.append(HtmlUtil.formEntry(msgLabel("Make globe"),
                                        htmlCheckbox(request, ARG_VIEW_GLOBE,
                                            false) + HtmlUtil.space(2)
                                                + viewPointHtml));



        List projections =
            idvServer.getIdv().getIdvProjectionManager().getProjections();

        Hashtable<String, List> projCatMap = new Hashtable<String, List>();
        List<String>            projCats   = new ArrayList<String>();
        for (int i = 0; i < projections.size(); i++) {
            ProjectionImpl proj = (ProjectionImpl) projections.get(i);
            String         name = proj.getName();
            List<String>   toks = StringUtil.split(name, ">", true, true);
            String         cat;
            String         label;
            if (toks.size() <= 1) {
                cat   = "Misc";
                label = name;
            } else {
                label = toks.remove(toks.size() - 1);
                cat   = StringUtil.join(">", toks);
            }
            List tfos = projCatMap.get(cat);
            if (tfos == null) {
                projCatMap.put(cat, tfos = new ArrayList());
                projCats.add(cat);
            }
            tfos.add(new TwoFacedObject(HtmlUtil.space(4) + label, name));
        }

        List projectionOptions = new ArrayList();
        projectionOptions.add(new TwoFacedObject("--none--", ""));
        for (String projCat : projCats) {
            projectionOptions.add(new TwoFacedObject(projCat, ""));
            projectionOptions.addAll(projCatMap.get(projCat));
        }

        basic.append(HtmlUtil.formEntry(msgLabel("Projection"),
                                        htmlSelect(request,
                                            ARG_VIEW_PROJECTION,
                                            projectionOptions)));


        basic.append(HtmlUtil.formEntry(msgLabel("Azimuth/Tilt"), 
                                        htmlInput(request, ARG_AZIMUTH, "", 6) +" " + 
                                        htmlInput(request, ARG_TILT, "", 6)));


        List viewOptions = new ArrayList();
        viewOptions.add(new TwoFacedObject("--none--", ""));
        viewOptions.add(new TwoFacedObject("north"));
        viewOptions.add(new TwoFacedObject("south"));
        viewOptions.add(new TwoFacedObject("east"));
        viewOptions.add(new TwoFacedObject("west"));
        viewOptions.add(new TwoFacedObject("bottom"));
        viewOptions.add(new TwoFacedObject("top"));

        basic.append(HtmlUtil.formEntry(msgLabel("View"),
                                        htmlSelect(request,
                                            ARG_VIEWDIR,
                                            viewOptions)));

        /*
          basic.append(HtmlUtil.formEntry(msgLabel("Clip image"),
                                        htmlCheckbox(request, ARG_CLIP,
                                            false)));
        */







        double   zoom     = request.get(ARG_ZOOM, 1.0);
        Object[] zoomList = new Object[] {
            new TwoFacedObject("Current", "" + zoom),
            new TwoFacedObject("Reset", "1.0"),
            new TwoFacedObject("Zoom in", "" + (zoom * 1.25)),
            new TwoFacedObject("Zoom in more", "" + (zoom * 1.5)),
            new TwoFacedObject("Zoom in even more", "" + (zoom * 1.75)),
            new TwoFacedObject("Zoom out", "" + (zoom * 0.9)),
            new TwoFacedObject("Zoom out more", "" + (zoom * 0.7)),
            new TwoFacedObject("Zoom out even more", "" + (zoom * 0.5))
        };


        basic.append(
            HtmlUtil.formEntry(
                msgLabel("Zoom Level"),
                HtmlUtil.select(
                    ARG_ZOOM, Misc.toList(zoomList),
                    Misc.newList(request.defined(ARG_ZOOM)
                                 ? "" + zoom
                                 : ""), "")));



        basic.append(HtmlUtil.formTableClose());

        StringBuffer bounds = new StringBuffer();
        String llb =
            getRepository().getMapManager().makeMapSelector(request,
                ARG_VIEW_BOUNDS, false,
                htmlCheckbox(request, ARG_VIEW_JUSTCLIP, false) + " "
                + msg("Just subset data") + HtmlUtil.space(2), "");
        bounds.append(llb);

        StringBuffer  mapSB = new StringBuffer();
        List<MapData> maps =
            idvServer.getIdv().getResourceManager().getMaps();
        Hashtable<String, List<TwoFacedObject>> mapCatMap =
            new Hashtable<String, List<TwoFacedObject>>();
        List<String> mapCats = new ArrayList<String>();
        for (MapData mapData : maps) {
            List<TwoFacedObject> mapCatList =
                mapCatMap.get(mapData.getCategory());
            if (mapCatList == null) {
                mapCatList = new ArrayList<TwoFacedObject>();
                mapCats.add(mapData.getCategory());
                mapCatMap.put(mapData.getCategory(), mapCatList);
            }
            mapCatList.add(new TwoFacedObject("&nbsp;&nbsp;"
                    + mapData.getDescription(), mapData.getSource()));
        }

        List<TwoFacedObject> mapOptions = new ArrayList<TwoFacedObject>();
        for (String cat : mapCats) {
            mapOptions.add(new TwoFacedObject(cat, ""));
            mapOptions.addAll(mapCatMap.get(cat));
        }

        //      mapSB.append(msgHeader("Maps"));
        String mapSelect = htmlSelect(request, ARG_MAPS, mapOptions,
                                      HtmlUtil.attrs(HtmlUtil.ATTR_MULTIPLE,
                                          "true", HtmlUtil.ATTR_SIZE, "10"));
        StringBuffer mapAttrs = new StringBuffer();
        mapAttrs.append(HtmlUtil.formTable());
        mapAttrs.append(
            HtmlUtil.formEntry(
                msgLabel("Map Line Width"),
                HtmlUtil.select(
                    ARG_MAPWIDTH, Misc.newList("1", "2", "3", "4"),
                    request.getString(ARG_MAPWIDTH, "1"))));
        mapAttrs.append(
            HtmlUtil.formEntry(
                msgLabel("Map Color"),
                HtmlUtil.colorSelect(
                    ARG_MAPCOLOR,
                    request.getString(
                        ARG_MAPCOLOR, StringUtil.toHexString(Color.red)))));

        mapAttrs.append(
            HtmlUtil.formEntry(
                msgLabel("Background Color"),
                HtmlUtil.colorSelect(
                    ARG_VIEW_BACKGROUND,
                    request.getString(
                        ARG_VIEW_BACKGROUND,
                        StringUtil.toHexString(Color.black)))));



        if (backgrounds == null) {
            backgrounds = new ArrayList();
            backgrounds.add(new TwoFacedObject("--none--", ""));
            for (WmsSelection selection :
                    (List<WmsSelection>) getIdv().getBackgroundImages()) {
                if (selection.getLayer().indexOf("fixed") >= 0) {
                    backgrounds.add(new TwoFacedObject(selection.getTitle(),
                            selection.getLayer()));
                }
            }
        }


        mapAttrs.append(HtmlUtil.formEntry(msgLabel("Background Image"),
                                           htmlSelect(request,
                                               ARG_VIEW_BACKGROUNDIMAGE,
                                                   backgrounds)));

        mapAttrs.append(HtmlUtil.formEntry(msgLabel("Wireframe"),
                                           HtmlUtil.checkbox(ARG_WIREFRAME,"true",
                                                             request.get(ARG_WIREFRAME,false))));

        mapAttrs.append(HtmlUtil.formEntry(msgLabel("Lat/Lon Lines"),
                                           HtmlUtil.checkbox(ARG_LATLON_VISIBLE,"true",
                                                             request.get(ARG_LATLON_VISIBLE,false)) +"  " +
                                           msgLabel("Spacing") + " " +
                                           htmlInput(request, ARG_LATLON_SPACING, "")
                                           ));



        mapAttrs.append(HtmlUtil.formTableClose());

        mapSB.append(HtmlUtil.table(new Object[] { mapSelect, mapAttrs },
                                    10));
        //      basic =new StringBuffer(HtmlUtil.table(new Object[]{basic, mapSB},10));
        List<String> tabLabels   = new ArrayList<String>();
        List<String> tabContents = new ArrayList<String>();
        tabLabels.add(msg("Basic"));
        tabContents.add(basic.toString());


        tabLabels.add(msg("View Bounds"));
        tabContents.add(bounds.toString());


        tabLabels.add(msg("Maps and Background"));
        tabContents.add(mapSB.toString());

        List colorTables =
            idvServer.getIdv().getColorTableManager().getColorTables();


        Hashtable<String, DataChoice> idToChoice = new Hashtable<String,
                                                       DataChoice>();
        List<DataChoice> choices =
            (List<DataChoice>) dataSource.getDataChoices();
        for (DataChoice dataChoice : choices) {
            idToChoice.put(dataChoice.getName(), dataChoice);
        }


        List params     = request.get(ARG_PARAM, new ArrayList());
        int  displayIdx = -1;
        for (int i = 0; i < params.size(); i++) {
            String param = (String) params.get(i);
            if (param.length() == 0) {
                continue;
            }
            displayIdx++;


            List<String> innerTabTitles   = new ArrayList<String>();
            List<String> innerTabContents = new ArrayList<String>();

            StringBuffer tab              = new StringBuffer();
            DataChoice   choice           = idToChoice.get(param);
            if (choice == null) {
                continue;
            }


            List descriptors =
                new ArrayList(
                    ControlDescriptor.getApplicableControlDescriptors(
                        choice.getCategories(),
                        idvServer.getIdv().getControlDescriptors(true)));


            List<TwoFacedObject> displays = new ArrayList<TwoFacedObject>();
            displays.add(new TwoFacedObject("--skip--", ""));
            for (ControlDescriptor controlDescriptor :
                    (List<ControlDescriptor>) descriptors) {
                String controlId = controlDescriptor.getControlId();
                if ( !okControls.contains(controlId)) {
                    continue;
                }
                displays.add(new TwoFacedObject(controlDescriptor.getLabel(),
                        controlId));
            }


            tab.append(HtmlUtil.hidden(ARG_PARAM, param));


            List options = new ArrayList();
            options.add("");
            tab.append(HtmlUtil.br());
            tab.append(msgLabel("Display Type"));
            tab.append(HtmlUtil.space(1));
            if ((displayIdx == 0) && (displays.size() > 1)) {
                //Set the default display for the first param
                if (request.defined(ARG_DISPLAY + displayIdx)) {
                    tab.append(htmlSelect(request, ARG_DISPLAY + displayIdx,
                                          displays));
                } else {
                    tab.append(HtmlUtil.select(ARG_DISPLAY + displayIdx,
                            displays, displays.get(1).getId().toString()));
                }
            } else {
                tab.append(htmlSelect(request, ARG_DISPLAY + displayIdx,
                                      displays));
            }
            tab.append(HtmlUtil.p());

            List times = choice.getAllDateTimes();
            if ((times != null) && (times.size() > 0)) {

                List<Object[]> tuples = new ArrayList<Object[]>();
                int            cnt    = 0;
                for (Object time : times) {
                    tuples.add(new Object[] { time, new Integer(cnt++) });
                }
                tuples = (List<Object[]>) Misc.sortTuples(tuples, true);
                List tfoTimes = new ArrayList();
                for (Object[] tuple : tuples) {
                    tfoTimes.add(new TwoFacedObject(tuple[0].toString(),
                            tuple[1]));
                }
                innerTabTitles.add(msg("Times"));
                innerTabContents.add(htmlSelect(request,
                        ARG_TIMES + displayIdx, tfoTimes, true,
                        HtmlUtil.attrs(HtmlUtil.ATTR_MULTIPLE, "true",
                                       HtmlUtil.ATTR_SIZE, "5")));
            }

            List levels       = choice.getAllLevels();

            List spatialComps = new ArrayList();
            if ((levels != null) && (levels.size() > 0)) {
                List tfoLevels = new ArrayList();
                int  cnt       = 0;
                for (Object level : levels) {
                    tfoLevels.add(new TwoFacedObject(level.toString(),
                            new Integer(cnt++)));
                }
                String levelWidget =
                    htmlSelect(request, ARG_LEVELS + displayIdx, tfoLevels,
                               true,
                               HtmlUtil.attrs(HtmlUtil.ATTR_MULTIPLE,
                                   "false", HtmlUtil.ATTR_SIZE, "5"));
                spatialComps.add(msgLabel("Levels"));
                spatialComps.add(levelWidget);
            }


            ThreeDSize size = (ThreeDSize) choice.getProperty(
                                  GeoGridDataSource.PROP_GRIDSIZE);
            spatialComps.add(msgLabel("X/Y Stride"));
            String strideComp = htmlInput(request, ARG_STRIDE + displayIdx,
                                          "");

            if (size != null) {
                strideComp = strideComp + HtmlUtil.br() + size;
            }
            spatialComps.add(strideComp);
            String spatial =
                HtmlUtil.table(Misc.listToStringArray(spatialComps), 5);
            innerTabTitles.add(msg("Spatial"));
            innerTabContents.add(spatial);




            ColorTable dfltColorTable =
                idvServer.getIdv().getDisplayConventions().getParamColorTable(
                    choice.getName());
            Range range =
                idvServer.getIdv().getDisplayConventions().getParamRange(
                    choice.getName(), null);

            List<String> ctCats = new ArrayList<String>();
            Hashtable<String, StringBuffer> ctCatMap = new Hashtable<String,
                                                           StringBuffer>();
            for (ColorTable colorTable : (List<ColorTable>) colorTables) {
                StringBuffer catSB = ctCatMap.get(colorTable.getCategory());
                if (catSB == null) {
                    catSB = new StringBuffer();
                    ctCatMap.put(colorTable.getCategory(), catSB);
                    ctCats.add(colorTable.getCategory());
                }
                String icon = IOUtil.cleanFileName(colorTable.getName())
                              + ".png";
                icon = icon.replace(" ", "_");
                String img = "<img border=0 src="
                             + getRepository().getUrlBase() + "/colortables/"
                             + icon + ">";
                String div = HtmlUtil.div(img + " " + colorTable.getName(),
                                          "");
                String call1 = HtmlUtil.call(
                                   "setFormValue",
                                   HtmlUtil.squote(
                                       ARG_COLORTABLE + displayIdx) + ","
                                           + HtmlUtil.squote(
                                               colorTable.getName()));
                String call2 =
                    HtmlUtil.call("setHtml", HtmlUtil.squote(ARG_COLORTABLE
                        + "_html" + displayIdx) + ","
                            + HtmlUtil.squote(colorTable.getName() + " "
                                + img));
                String call = call1 + ";" + call2;
                catSB.append(HtmlUtil.mouseClickHref(call, div));
            }




            StringBuffer ctsb = new StringBuffer();
            ctsb.append(msgLabel("Range") + HtmlUtil.space(1)
                        + htmlInput(request, ARG_RANGE_MIN + displayIdx,
                                    ((range == null)
                                     ? ""
                                     : "" + range.getMin())) + " - "
                                     + htmlInput(request,
                                         ARG_RANGE_MAX + displayIdx,
                                         ((range == null)
                                          ? ""
                                          : range.getMax() + "")));

            if ( !request.defined(ARG_COLORTABLE + displayIdx)
                    && (dfltColorTable != null)) {
                request.put(ARG_COLORTABLE + displayIdx,
                            dfltColorTable.getName());
            }

            ctsb.append(
                HtmlUtil.hidden(
                    ARG_COLORTABLE + displayIdx,
                    request.getString(ARG_COLORTABLE + displayIdx, ""),
                    HtmlUtil.id(ARG_COLORTABLE + displayIdx)));
            ctsb.append(HtmlUtil.br());
            String ctDiv = "-default-";
            if (request.defined(ARG_COLORTABLE + displayIdx)) {
                String icon =
                    IOUtil.cleanFileName(request.getString(ARG_COLORTABLE
                        + displayIdx, "")) + ".png";
                String img = HtmlUtil.img(getRepository().getUrlBase()
                                          + "/colortables/" + icon);
                ctDiv = request.getString(ARG_COLORTABLE + displayIdx,
                                          "-default-") + " " + img;

            }
            ctsb.append(HtmlUtil.table(new Object[] { msgLabel("Color table"),
                    HtmlUtil.div(ctDiv,
                                 HtmlUtil.id(ARG_COLORTABLE + "_html"
                                             + displayIdx)) }, 2));

            String call = HtmlUtil.call("setFormValue",
                                        "'" + ARG_COLORTABLE + displayIdx
                                        + "','" + "" + "'") + ";"
                                            + HtmlUtil.call("setHtml",
                                                "'" + ARG_COLORTABLE
                                                + "_html" + displayIdx
                                                + "','" + "-default-" + "'");
            ctsb.append(HtmlUtil.mouseClickHref(call, "Use default"));
            for (String ctcat : ctCats) {
                ctsb.append(HtmlUtil.makeShowHideBlock(ctcat,
                        ctCatMap.get(ctcat).toString(), false));

            }

            StringBuffer scalesb = new StringBuffer();
            scalesb.append(msgHeader("Color Scale"));
            scalesb.append(HtmlUtil.formTable());
            scalesb.append(HtmlUtil.formEntry(msgLabel("Visible"),
                    htmlCheckbox(request, ARG_SCALE_VISIBLE + displayIdx,
                                 false)));
            scalesb.append(HtmlUtil.formEntry(msgLabel("Place"),
                    htmlSelect(request, ARG_SCALE_PLACEMENT + displayIdx,
                               Misc.newList("top", "left", "bottom",
                                            "right"))));
            scalesb.append(HtmlUtil.formTableClose());





            StringBuffer contoursb = new StringBuffer();
            ContourInfo ci =
                idvServer.getIdv().getDisplayConventions()
                    .findDefaultContourInfo(choice.getName());
            contoursb.append(HtmlUtil.formTable());
            contoursb.append(HtmlUtil.formEntry(msgLabel("Interval"),
                    htmlInput(request, ARG_CONTOUR_INTERVAL + displayIdx,
                              ((ci == null)
                               ? ""
                               : ci.getInterval() + ""), 3)));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Base"),
                    htmlInput(request, ARG_CONTOUR_BASE + displayIdx,
                              ((ci == null)
                               ? ""
                               : ci.getBase() + ""), 3)));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Min"),
                    htmlInput(request, ARG_CONTOUR_MIN + displayIdx,
                              ((ci == null)
                               ? ""
                               : ci.getMin() + ""), 3)));

            contoursb.append(HtmlUtil.formEntry(msgLabel("Max"),
                    htmlInput(request, ARG_CONTOUR_MAX + displayIdx,
                              ((ci == null)
                               ? ""
                               : ci.getMax() + ""), 3)));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Line Width"),
                    htmlSelect(request, ARG_CONTOUR_WIDTH + displayIdx,
                               Misc.newList("1", "2", "3", "4"))));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Dashed"),
                    htmlCheckbox(request, ARG_CONTOUR_DASHED + displayIdx,
                                 ((ci == null)
                                  ? false
                                  : ci.getDashOn()))));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Labels"),
                    htmlCheckbox(request, ARG_CONTOUR_LABELS + displayIdx,
                                 ((ci == null)
                                  ? true
                                  : ci.getIsLabeled()))));
            contoursb.append(HtmlUtil.formTableClose());


            StringBuffer misc = new StringBuffer();
            misc.append(HtmlUtil.formTable());
            misc.append(HtmlUtil.formEntry(msgLabel("Display List Label"),
                                           htmlInput(request,
                                               ARG_DISPLAYLISTLABEL
                                                   + displayIdx, "", 30)));
            String unitString = "";
            Unit displayUnit =
                idvServer.getIdv().getDisplayConventions().getDisplayUnit(
                    choice.getName(), null);
            if (displayUnit != null) {
                unitString = displayUnit.toString();
            }
            misc.append(HtmlUtil.formEntry(msgLabel("Display Unit"),
                                           htmlInput(request,
                                               ARG_DISPLAYUNIT + displayIdx,
                                                   unitString, 6)));

            misc.append(
                HtmlUtil.formEntry(
                    msgLabel("Display Color"),
                    HtmlUtil.colorSelect(
                        ARG_DISPLAYCOLOR + displayIdx,
                        request.getString(
                            ARG_DISPLAYCOLOR + displayIdx,
                            StringUtil.toHexString(Color.red)))));


            misc.append(HtmlUtil.formEntry(msgLabel("Isosurface Value"),
                                           htmlInput(request,
                                               ARG_ISOSURFACEVALUE
                                                   + displayIdx, "", 3)));
            misc.append(HtmlUtil.formEntry(msgLabel("XS Selector"),
                                           "Lat 1: " + 
                                           htmlInput(request, ARG_LAT1 + displayIdx,"", 6) +
                                           "Lon 1: " + 
                                           htmlInput(request, ARG_LON1 + displayIdx,"", 6) +"     " +
                                           "Lat 2: " + 
                                           htmlInput(request, ARG_LAT2 + displayIdx,"", 6) +
                                           "Lon 2: " + 
                                           htmlInput(request, ARG_LON2 + displayIdx,"", 6)));


            misc.append(HtmlUtil.formEntry(msgLabel("Vector/Barb Size"),
                                           htmlInput(request,
                                               ARG_FLOW_SCALE + displayIdx,
                                                   "4", 3)));
            misc.append(HtmlUtil.formEntry(msgLabel("Streamline Density"),
                                           htmlInput(request,
                                               ARG_FLOW_DENSITY + displayIdx,
                                                   "1", 3)));
            misc.append(HtmlUtil.formEntry(msgLabel("Flow Skip"),
                                           htmlInput(request,
                                               ARG_FLOW_SKIP + displayIdx,
                                                   "0", 3)));


            misc.append(HtmlUtil.formTableClose());

            innerTabTitles.add(msg("Color Table"));
            innerTabContents.add(HtmlUtil.table(new Object[] { ctsb,
                    scalesb }, 5));

            innerTabTitles.add(msg("Contours"));
            innerTabContents.add(contoursb.toString());

            innerTabTitles.add(msg("Misc"));
            innerTabContents.add(misc.toString());



            String innerTab = HtmlUtil.makeTabs(innerTabTitles,
                                  innerTabContents, true, "tab_content");
            tab.append(HtmlUtil.inset(HtmlUtil.p() + innerTab, 10));

            tabLabels.add(
                StringUtil.camelCase(
                    StringUtil.shorten(choice.getDescription(), 25)));
            tabContents.add(tab.toString());
        }

        if ( !request.getUser().getAnonymous()) {
            StringBuffer publishSB = new StringBuffer();
            publishSB.append(HtmlUtil.formTable());
            publishSB.append(HtmlUtil.hidden(ARG_PUBLISH_ENTRY + "_hidden",
                                             "",
                                             HtmlUtil.id(ARG_PUBLISH_ENTRY
                                                 + "_hidden")));
            publishSB.append(
                HtmlUtil.row(
                    HtmlUtil.colspan(
                        msgHeader(
                            "Select a folder to publish the product to"), 2)));

            String select = OutputHandler.getSelect(request,
                                ARG_PUBLISH_ENTRY, "Select folder", false,
                                null, entry);
            publishSB.append(HtmlUtil.formEntry(msgLabel("Folder"),
                    HtmlUtil.disabledInput(ARG_PUBLISH_ENTRY, "",
                                           HtmlUtil.id(ARG_PUBLISH_ENTRY)
                                           + HtmlUtil.SIZE_60) + select));

            publishSB.append(HtmlUtil.formEntry(msgLabel("Name"),
                    htmlInput(request, ARG_PUBLISH_NAME, "", 30)));

            publishSB.append(HtmlUtil.formEntry("",
                    HtmlUtil.submit(msg("Publish image"),
                                    ARG_SUBMIT_PUBLISH)));

            if (getAccessManager().canDoAction(request, entry,
                    Permission.ACTION_EDIT)) {

                publishSB.append(HtmlUtil.row(HtmlUtil.colspan(HtmlUtil.p(),
                        2)));
                publishSB.append(
                    HtmlUtil.row(
                        HtmlUtil.colspan(
                            msgHeader("Or save these settings"), 2)));
                publishSB.append(
                    HtmlUtil.formEntry(
                        msgLabel("Settings name"),
                        HtmlUtil.input(ARG_SAVE_NAME, "", 30)));
                publishSB.append(HtmlUtil.formEntry(msg("Attach image"),
                        HtmlUtil.checkbox(ARG_SAVE_ATTACH, "true", false)));
                publishSB.append(HtmlUtil.formEntry("",
                        HtmlUtil.submit(msg("Save settings"),
                                        ARG_SUBMIT_SAVE)));

            }
            publishSB.append(HtmlUtil.formTableClose());


            tabLabels.add(msg("Publish"));
            tabContents.add(publishSB.toString());
        }


        sb.append(HtmlUtil.makeTabs(tabLabels, tabContents, true,
                                    "tab_content"));

        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.submit(msg("Make image"), ARG_SUBMIT));
        sb.append(HtmlUtil.formClose());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dataSource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputGridInitForm(final Request request, Entry entry,
                                      DataSource dataSource)
            throws Exception {
        StringBuffer sb      = new StringBuffer();

        String       formUrl = getRepository().URL_ENTRY_SHOW.getFullUrl();
        sb.append(HtmlUtil.form(formUrl, ""));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_IDV_GRID));
        sb.append(HtmlUtil.hidden(ARG_ACTION, ACTION_MAKEFORM));


        List<DataChoice> choices =
            (List<DataChoice>) dataSource.getDataChoices();


        sb.append(msgHeader("Select one or more fields to view"));

        StringBuffer fields  = new StringBuffer();
        List         options = new ArrayList();
        //            options.add(new TwoFacedObject("--Pick one--", ""));
        List<String> cats = new ArrayList<String>();
        Hashtable<String, List<TwoFacedObject>> catMap =
            new Hashtable<String, List<TwoFacedObject>>();
        for (DataChoice dataChoice : choices) {
            String label = StringUtil.camelCase(dataChoice.getDescription());
            DataCategory cat = dataChoice.getDisplayCategory();
            String       catName;
            if (cat != null) {
                catName = cat.toString();
            } else {
                catName = "Data";
            }
            List<TwoFacedObject> tfos = catMap.get(catName);
            if (tfos == null) {
                tfos = new ArrayList<TwoFacedObject>();
                catMap.put(catName, tfos);
                cats.add(catName);
            }
            tfos.add(new TwoFacedObject("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                                        + label, dataChoice.getName()));
        }

        for (String cat : cats) {
            options.add(new TwoFacedObject(cat.replace("-", "&gt;"), ""));
            options.addAll(catMap.get(cat));
        }


        fields.append(htmlSelect(request, ARG_PARAM, options,
                                 HtmlUtil.attrs(HtmlUtil.ATTR_MULTIPLE,
                                     "true", HtmlUtil.ATTR_SIZE, "10")));
        fields.append(HtmlUtil.p());

        sb.append(HtmlUtil.insetLeft(fields.toString(), 10));
        sb.append(HtmlUtil.submit(msg("Make image"), ARG_SUBMIT));
        sb.append(HtmlUtil.formClose());



        List<Metadata> metadataList =
            getMetadataManager().findMetadata(entry,
                METADATA_TYPE_VISUALIZATION, false);
        if ((metadataList != null) && (metadataList.size() > 0)) {
            sb.append(HtmlUtil.p());
            sb.append(msgHeader("Or select a predefined visualization"));
            sb.append(HtmlUtil.open(HtmlUtil.TAG_UL));
            MetadataType metadataType =
                getMetadataManager().findType(METADATA_TYPE_VISUALIZATION);
            for (Metadata metadata : metadataList) {
                String url =
                    HtmlUtil.url(getRepository().URL_ENTRY_SHOW.toString(),
                                 new String[] {
                    ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
                    OUTPUT_IDV_GRID.toString(), ARG_ACTION, ACTION_MAKEPAGE,
                    ARG_PREDEFINED, metadata.getId()
                });
                sb.append(HtmlUtil.li(HtmlUtil.href(url,
                        metadata.getAttr1()), ""));
                metadataType.decorateEntry(request, entry, sb, metadata,
                                           true, true);
            }
            sb.append(HtmlUtil.close(HtmlUtil.TAG_UL));
        }


        return new Result("Grid Displays", sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    private Hashtable getRequestArgs(Request request) {
        Hashtable requestArgs = new Hashtable(request.getArgs());
        requestArgs.remove(ARG_ACTION);
        requestArgs.remove(ARG_ENTRYID);
        requestArgs.remove(ARG_SUBMIT);
        requestArgs.remove(ARG_OUTPUT);
        requestArgs.remove(ARG_PUBLISH_ENTRY);
        requestArgs.remove(ARG_SUBMIT_PUBLISH);
        requestArgs.remove(ARG_SUBMIT_SAVE);
        requestArgs.remove(ARG_PUBLISH_ENTRY + "_hidden");
        requestArgs.remove(ARG_PUBLISH_NAME);

        return requestArgs;
    }

    /*
      Hashtable requestArgs = getRequestArgs(request);
      List<String> argList = new ArrayList<String>();
      List<String> valueList = new ArrayList<String>();

      Hashtable abbrev = new Hashtable();
      Hashtable abbrev2 = new Hashtable();
      for (Enumeration keys = requestArgs.keys(); keys.hasMoreElements(); ) {
      String arg = (String) keys.nextElement();
      if(abbrev.get(arg)==null) {
      String s = null;
      for(int length=1;length<10;length++) {
      s = null;
      for(String tok : StringUtil.split(arg,"_",true,true)) {
      String sub = tok.substring(0,Math.min(tok.length(),length));
      if(s==null) s = sub;
      else s = s+"_" + sub;
      }
      if(arg.endsWith("1")) s = s+"1";
      else if(arg.endsWith("2")) s = s+"2";
      else if(arg.endsWith("3")) s = s+"3";
      if(abbrev2.get(s)==null || Misc.equals(abbrev2.get(s), arg)) {
      abbrev2.put(s, arg);
      break;
      }
      }
      System.out.println(arg+ "=" + s);
      abbrev.put(arg,s);
      } else {
      }
      }
    */




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dataSource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputGridPage(final Request request, Entry entry,
                                  DataSource dataSource)
            throws Exception {

        StringBuffer sb = new StringBuffer();

        if (request.exists(ARG_SUBMIT_PUBLISH)
                && request.defined(ARG_PUBLISH_ENTRY + "_hidden")) {
            Group parent = getEntryManager().findGroup(request,
                               request.getString(ARG_PUBLISH_ENTRY
                                   + "_hidden", ""));
            if (parent == null) {
                return new Result(
                    "Grid Displays",
                    new StringBuffer(
                        getRepository().showDialogError(
                            msg("Could not find folder"))));
            }

            File imageFile = (File) generateGridImage(request, entry,
                                 dataSource);
            String name = request.getString(ARG_PUBLISH_NAME, "").trim();
            if (name.length() == 0) {
                name = "Generated Grid Image";
            }
            String fileName = IOUtil.cleanFileName(name).replace(" ", "_");
            imageFile = getStorageManager().copyToStorage(request, imageFile,
                    fileName + IOUtil.getFileExtension(imageFile.toString()));

            Entry newEntry = getEntryManager().addFileEntry(request,
                                 imageFile, parent, name, request.getUser());
            getRepository().getAssociationManager().addAssociation(request,
                    newEntry, entry, "generated image",
                    "product generated from");
            return new Result(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, newEntry));
        }


        if (request.defined(ARG_PREDEFINED)) {
            List<Metadata> metadataList =
                getMetadataManager().findMetadata(entry,
                    METADATA_TYPE_VISUALIZATION, false);
            String args = null;
            if ((metadataList != null) && (metadataList.size() > 0)) {
                for (Metadata metadata : metadataList) {
                    if (metadata.getId().equals(
                            request.getString(ARG_PREDEFINED, ""))) {
                        args = metadata.getAttr2();
                        break;
                    }
                }
            }
            if (args != null) {
                Hashtable urlArgs = new Hashtable();
                for (String pair : StringUtil.split(args, "&", true, true)) {
                    List<String> toks = StringUtil.splitUpTo(pair, "=", 2);
                    if ((toks == null) || (toks.size() != 2)) {
                        continue;
                    }
                    String name = java.net.URLDecoder.decode(toks.get(0),
                                      "UTF-8");
                    String value = java.net.URLDecoder.decode(toks.get(1),
                                       "UTF-8");
                    Object o = urlArgs.get(name);
                    if (o == null) {
                        urlArgs.put(name, value);
                    } else if (o instanceof List) {
                        ((List) o).add(value);
                    } else {
                        List l = new ArrayList();
                        l.add(o);
                        l.add(value);
                        urlArgs.put(name, l);
                    }
                }
                for (Enumeration keys = urlArgs.keys();
                        keys.hasMoreElements(); ) {
                    String arg   = (String) keys.nextElement();
                    Object value = urlArgs.get(arg);
                    request.put(arg, value);
                }
                request.remove(ARG_SUBMIT_SAVE);
            }
        }


        String baseName = IOUtil.stripExtension(entry.getName());
        String product  = request.getString(ARG_PRODUCT, PRODUCT_IMAGE);
        String url      = getRepository().URL_ENTRY_SHOW.getFullUrl();


        String islUrl   = url + "/" + baseName + ".isl";
        String jnlpUrl  = url + "/" + baseName + ".jnlp";

        if (product.equals(PRODUCT_IMAGE)) {
            url = url + "/" + baseName + ".gif";
        } else if (product.equals(PRODUCT_MOV)) {
            url = url + "/" + baseName + ".mov";
        } else if (product.equals(PRODUCT_KMZ)) {
            url = url + "/" + baseName + ".kmz";
        }




        String args = request.getUrlArgs(exceptArgs, null, ".*_gvdflt");
        url = url + "?" + ARG_ACTION + "=" + ACTION_MAKEIMAGE + "&" + args;



        if (request.defined(ARG_SUBMIT_SAVE)) {
            if ( !getAccessManager().canDoAction(request, entry,
                    Permission.ACTION_EDIT)) {
                throw new AccessException("No access", request);
            }

            String fileName = "";
            if (request.get(ARG_SAVE_ATTACH, false)) {
                Object fileOrResult = generateGridImage(request, entry,
                                          dataSource);
                if (fileOrResult instanceof Result) {
                    throw new IllegalArgumentException(
                        "You need to specify an image or movie product");
                }

                File imageFile = (File) fileOrResult;

                fileName = getStorageManager().copyToEntryDir(entry,
                        imageFile).getName();
            }

            Metadata metadata = new Metadata(getRepository().getGUID(),
                                             entry.getId(),
                                             METADATA_TYPE_VISUALIZATION,
                                             false,
                                             request.getString(ARG_SAVE_NAME,
                                                 ""), args, fileName, "", "");
            getMetadataManager().insertMetadata(metadata);
            entry.addMetadata(metadata);
            return new Result(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                    OUTPUT_IDV_GRID.toString()));

        }

        islUrl = islUrl + "?" + ARG_ACTION + "=" + ACTION_MAKEIMAGE + "&"
                 + args + "&" + ARG_TARGET + "=" + TARGET_ISL;
        jnlpUrl = jnlpUrl + "?" + ARG_ACTION + "=" + ACTION_MAKEIMAGE + "&"
                  + args + "&" + ARG_TARGET + "=" + TARGET_JNLP;

        boolean showForm = true;

        if (product.equals(PRODUCT_IMAGE)) {
            sb.append(HtmlUtil.img(url, "Image is being processed...",
                                   HtmlUtil.attr(HtmlUtil.ATTR_WIDTH,
                                       request.getString(ARG_IMAGE_WIDTH,
                                           "600"))));
            showForm = false;
        } else if (product.equals(PRODUCT_MOV)) {
            sb.append(HtmlUtil.href(url, "Click here to retrieve the movie"));
        } else if (product.equals(PRODUCT_KMZ)) {

            sb.append(HtmlUtil.href(url,
                                    "Click here to retrieve the KMZ file"));
        }




        StringBuffer formSB = new StringBuffer();
        makeGridForm(request, formSB, entry, dataSource);
        sb.append(HtmlUtil.div("",
                               HtmlUtil.cssClass("image_edit_box")
                               + HtmlUtil.id("image_edit_box")));

        formSB.append(HtmlUtil.space(2));
        formSB.append(HtmlUtil.href(jnlpUrl, msg("Launch in the IDV")));
        formSB.append(HtmlUtil.space(2));
        formSB.append(HtmlUtil.href(islUrl, msg("Download IDV ISL script")));


        sb.append("\n");
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.makeShowHideBlock(msg("Image Settings"), formSB.toString(),
                                             showForm));

        return new Result("Grid Displays", sb);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dataSource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGridImage(final Request request, Entry entry,
                                  DataSource dataSource)
            throws Exception {


        Object fileOrResult = generateGridImage(request, entry, dataSource);
        if (fileOrResult instanceof Result) {
            return (Result) fileOrResult;
        }
        File   imageFile = (File) fileOrResult;
        String extension = IOUtil.getFileExtension(imageFile.toString());
        return new Result("",
                          getStorageManager().getFileInputStream(imageFile),
                          getRepository().getMimeTypeFromSuffix(extension));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param dataSource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Object generateGridImage(Request request, Entry entry,
                                     DataSource dataSource)
            throws Exception {

        DataOutputHandler dataOutputHandler = getDataOutputHandler();
        //      Trace.addNot(".*ShadowFunction.*");
        //      Trace.addNot(".*GeoGrid.*");
        //      Trace.addOnly(".*MapProjection.*");
        //      Trace.addOnly(".*ProjectionCoordinateSystem.*");
        //Trace.startTrace();

        String  id      = entry.getId();

        String  product = request.getString(ARG_PRODUCT, PRODUCT_IMAGE);

        boolean forIsl  = request.getString(ARG_TARGET,
                                            "").equals(TARGET_ISL);
        boolean forJnlp = request.getString(ARG_TARGET,
                                            "").equals(TARGET_JNLP);
        if (forJnlp) {
            forIsl = true;
        }


        Hashtable    requestArgs = getRequestArgs(request);
        List<String> argList     = new ArrayList<String>();
        List<String> valueList   = new ArrayList<String>();

        for (Enumeration keys =
                requestArgs.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if (exceptArgs.get(arg) != null) {
                continue;
            }
            Object value = requestArgs.get(arg);
            String s     = value.toString();
            if (s.trim().length() == 0) {
                continue;
            }
            argList.add(arg);
        }
        Collections.sort(argList);
        StringBuffer fileKey = new StringBuffer();
        for (String arg : argList) {
            Object value = requestArgs.get(arg);
            String s     = value.toString();
            fileKey.append(arg);
            fileKey.append("=");
            fileKey.append(s);
            fileKey.append(";");
        }

        //      System.err.println ("fileKey: " + fileKey);
        boolean multipleTimes = false;
        String  suffix        = ".gif";
        if (product.equals(PRODUCT_IMAGE)) {
            suffix = ".gif";
        } else if (product.equals(PRODUCT_MOV)) {
            multipleTimes = true;
            suffix        = ".mov";
        } else if (product.equals(PRODUCT_KMZ)) {
            multipleTimes = true;
            suffix        = ".kmz";
        }

        String imageKey  = fileKey.toString();
        File   imageFile = null;
        synchronized (imageCache) {
            imageFile = imageCache.get(imageKey);
        }
        if (imageFile == null) {
            imageFile = getStorageManager().getTmpFile(request,
                    "gridimage" + suffix);
        }

        if ( !forIsl && imageFile.exists()) {
            //      System.err.println ("got  file");
            return imageFile;
        }


        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");

        StringBuffer viewProps = new StringBuffer();
        if (request.defined(ARG_VIEW_PROJECTION)) {
            viewProps.append(
                makeProperty(
                    "defaultProjectionName",
                    request.getString(ARG_VIEW_PROJECTION, "")));

        }

        viewProps.append(makeProperty("wireframe", ""+request.get(ARG_WIREFRAME,false)));



        if(request.get(ARG_LATLON_VISIBLE,false)) {
            viewProps.append(makeProperty("initLatLonVisible", "true"));
            if(request.defined(ARG_LATLON_SPACING)) {
                viewProps.append(makeProperty("initLatLonSpacing", ""+request.get(ARG_LATLON_SPACING,15.0)));
            }
        }


        viewProps.append("\n");



        if (request.get(ARG_VIEW_GLOBE, false)) {
            viewProps.append(makeProperty("useGlobeDisplay", true));
            viewProps.append("\n");
            if (request.defined(ARG_VIEW_VIEWPOINT)) {
                viewProps.append(makeProperty("initViewStateName",
                        request.getString(ARG_VIEW_VIEWPOINT, "")));
                viewProps.append("\n");
            }
        }



        viewProps.append(makeProperty("background",
                                      request.getString(ARG_VIEW_BACKGROUND,
                                          "black")));
        viewProps.append("\n");

        viewProps.append(makeProperty("initMapPaths",
                                      StringUtil.join(",",
                                          request.get(ARG_MAPS,
                                              new ArrayList()))));
        viewProps.append("\n");

        if (request.defined(ARG_MAPWIDTH)) {
            viewProps.append(makeProperty("initMapWidth",
                                          request.getString(ARG_MAPWIDTH,
                                              "1")));
            viewProps.append("\n");
        }
        if (request.defined(ARG_MAPCOLOR)) {
            viewProps.append(makeProperty("initMapColor",
                                          request.getString(ARG_MAPCOLOR,
                                              "")));
            viewProps.append("\n");
        }

        double zoom = request.get(ARG_ZOOM, 1.0);
        if (zoom != 1.0) {
            viewProps.append(makeProperty("displayProjectionZoom",
                                          "" + zoom));
            viewProps.append("\n");
        }

        String clip = "";
        if (request.get(ARG_CLIP, false)) {
            clip = XmlUtil.tag("clip", "");
        }


        String       dataSourceExtra = "";
        StringBuffer dataSourceProps = new StringBuffer();


        int          width           = request.get(ARG_IMAGE_WIDTH, 400);
        int          height          = request.get(ARG_IMAGE_HEIGHT, 400);
        if (request.defined(ARG_VIEW_BOUNDS + "_south")
                && request.defined(ARG_VIEW_BOUNDS + "_north")
                && request.defined(ARG_VIEW_BOUNDS + "_east")
                && request.defined(ARG_VIEW_BOUNDS + "_west")) {
            double south   = request.get(ARG_VIEW_BOUNDS + "_south", 0.0);
            double north   = request.get(ARG_VIEW_BOUNDS + "_north", 0.0);
            double east    = request.get(ARG_VIEW_BOUNDS + "_east", 0.0);
            double west    = request.get(ARG_VIEW_BOUNDS + "_west", 0.0);
            double bwidth  = Math.abs(east - west);
            double bheight = Math.abs(north - south);

            if ( !request.get(ARG_VIEW_JUSTCLIP, false)) {
                if ( !request.defined(ARG_VIEW_PROJECTION)) {
                    viewProps.append(makeProperty("initLatLonBounds",
                            west + "," + north + "," + bwidth + ","
                            + bheight));
                    viewProps.append("\n");
                }
            }
            dataSourceProps.append(makeProperty("defaultSelectionBounds",
                    west + "," + north + "," + bwidth + "," + bheight));
            dataSourceProps.append("\n");

            height = (int) (width * bheight / bwidth);
        }



        //Create a new viewmanager
        //For now don't do this if we are doing jnlp
        if ( !forJnlp) {
            isl.append(
                XmlUtil.tag(
                    ImageGenerator.TAG_VIEW,
                    XmlUtil.attrs(ImageGenerator.ATTR_WIDTH, "" + width)
                    + XmlUtil.attrs(
                        ImageGenerator.ATTR_HEIGHT,
                        "" + height), viewProps.toString()));
        }


        if (request.defined(ARG_VIEW_BACKGROUNDIMAGE)) {
            StringBuffer propSB = new StringBuffer();
            propSB.append(makeProperty("id", "backgroundimage"));
            propSB.append(
                makeProperty(
                    "theLayer",
                    request.getString(ARG_VIEW_BACKGROUNDIMAGE, "")));
            StringBuffer attrs = new StringBuffer();
            attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TYPE,
                                       "wmscontrol"));


            isl.append(XmlUtil.tag(ImageGenerator.TAG_DISPLAY,
                                   attrs.toString(), propSB.toString()));
        }



        if (forIsl) {
            isl.append(
                XmlUtil.openTag(
                    ImageGenerator.TAG_DATASOURCE,
                    XmlUtil.attrs(
                        "id", "datasource", "url",
                        getRepository().absoluteUrl(
                            getRepository().URL_ENTRY_SHOW
                            + dataOutputHandler.getOpendapUrl(entry)))));
        } else {
            isl.append(XmlUtil.openTag(ImageGenerator.TAG_DATASOURCE,
                                       XmlUtil.attrs("id", "datasource",
                                           "times", "0")));
        }

        isl.append(dataSourceProps);



        Hashtable props = new Hashtable();
        props.put("datasource", dataSource);
        StringBuffer firstDisplays  = new StringBuffer();
        StringBuffer secondDisplays = new StringBuffer();




        List         params         = request.get(ARG_PARAM, new ArrayList());
        int          displayIdx     = -1;
        for (int i = 0; i < params.size(); i++) {
            String param = (String) params.get(i);
            if (param.length() == 0) {
                continue;
            }
            displayIdx++;

            if ( !request.defined(ARG_DISPLAY + displayIdx)) {
                continue;
            }
            String display = request.getString(ARG_DISPLAY + displayIdx, "");

            StringBuffer propSB = new StringBuffer();
            propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                                      XmlUtil.attrs("name", "id", "value",
                                          "thedisplay" + displayIdx)));



            if (request.get(ARG_SCALE_VISIBLE + displayIdx, false)) {
                /*
                  visible=true|false;
                  color=somecolor;
                  orientation=horizontal|vertical;
                  placement=top|left|bottom|right
                */
                String placement = request.getString(ARG_SCALE_PLACEMENT
                                       + displayIdx, "");
                String orientation;
                if (placement.equals("top") || placement.equals("bottom")) {
                    orientation = "horizontal";
                } else {
                    orientation = "vertical";
                }
                String s = "visible=true;orientation=" + orientation
                           + ";placement=" + placement;
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                                          XmlUtil.attrs("name",
                                              "colorScaleInfo", "value",
                                                  s.toString())));




            }


            if (display.equals(DISPLAY_PLANVIEWCONTOUR)) {
                StringBuffer s = new StringBuffer();
                s.append("width="
                         + request.getString(ARG_CONTOUR_WIDTH + displayIdx,
                                             "1") + ";");
                if (request.defined(ARG_CONTOUR_INTERVAL + displayIdx)) {
                    s.append("interval="
                             + request.getString(ARG_CONTOUR_INTERVAL
                                 + displayIdx, "") + ";");
                }
                if (request.defined(ARG_CONTOUR_BASE + displayIdx)) {
                    s.append("base="
                             + request.getString(ARG_CONTOUR_BASE
                                 + displayIdx, "") + ";");
                }
                if (request.defined(ARG_CONTOUR_MIN + displayIdx)) {
                    s.append("min="
                             + request.getString(ARG_CONTOUR_MIN
                                 + displayIdx, "") + ";");
                }
                if (request.defined(ARG_CONTOUR_MAX + displayIdx)) {
                    s.append("max="
                             + request.getString(ARG_CONTOUR_MAX
                                 + displayIdx, "") + ";");
                }

                s.append("dashed="
                         + request.get(ARG_CONTOUR_DASHED + displayIdx,
                                       false) + ";");
                s.append("labels="
                         + request.get(ARG_CONTOUR_LABELS + displayIdx,
                                       false) + ";");
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                                          XmlUtil.attrs("name",
                                              "contourInfoParams", "value",
                                                  s.toString())));
            }

            /*
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                                          XmlUtil.attrs("name",
                                              "contourInfoParams", "value",
                                                  s.toString())));
            */


            if (request.defined(ARG_RANGE_MIN + displayIdx)
                    && request.defined(ARG_RANGE_MAX + displayIdx)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "range", "value",
                            request.getString(
                                ARG_RANGE_MIN + displayIdx, "").trim() + ":"
                                    + request.getString(
                                        ARG_RANGE_MAX + displayIdx,
                                        "").trim())));



            }


            if (request.defined(ARG_COLORTABLE + displayIdx)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "colorTableName", "value",
                            request.getString(
                                ARG_COLORTABLE + displayIdx, ""))));;

            }



            StringBuffer attrs = new StringBuffer();

            propSB.append(makeProperty("color",
                                       request.getString(ARG_DISPLAYCOLOR
                                           + displayIdx, "")));


            if(display.equals(DISPLAY_XS_FILLEDCONTOUR) ||
               display.equals(DISPLAY_XS_CONTOUR) ||
               display.equals(DISPLAY_XS_COLOR)) {
                propSB.append(makeProperty("lineVisible","false"));
                if(request.defined(ARG_LAT1+displayIdx) && 
                   request.defined(ARG_LON1+displayIdx) && 
                   request.defined(ARG_LAT2+displayIdx) && 
                   request.defined(ARG_LON2+displayIdx)) {
                    propSB.append(makeProperty("initLat1",
                                               request.getString(ARG_LAT1
                                                                 + displayIdx, "")));

                    propSB.append(makeProperty("initLon1",
                                               request.getString(ARG_LON1
                                                                 + displayIdx, "")));
                    propSB.append(makeProperty("initLat2",
                                               request.getString(ARG_LAT2
                                                                 + displayIdx, "")));

                    propSB.append(makeProperty("initLon2",
                                               request.getString(ARG_LON2
                                                                 + displayIdx, "")));
                }
            }

            if (display.equals(DISPLAY_PLANVIEWFLOW)
                    || display.equals(DISPLAY_STREAMLINES)
                    || display.equals(DISPLAY_WINDBARBPLAN)) {
                propSB.append(makeProperty("flowScale",
                                           request.getString(ARG_FLOW_SCALE
                                               + displayIdx, "")));

                propSB.append(makeProperty("streamlineDensity",
                                           request.getString(ARG_FLOW_DENSITY
                                               + displayIdx, "")));

                propSB.append(makeProperty("skipValue",
                                           request.getString(ARG_FLOW_SKIP
                                               + displayIdx, "")));

            }




            if (display.equals(DISPLAY_ISOSURFACE)) {
                if (request.defined(ARG_ISOSURFACEVALUE + displayIdx)) {
                    propSB.append(
                        XmlUtil.tag(
                            ImageGenerator.TAG_PROPERTY,
                            XmlUtil.attrs(
                                "name", "surfaceValue", "value",
                                request.getString(
                                    ARG_ISOSURFACEVALUE + displayIdx, ""))));
                }
            } 
            if(!vertControls.contains(display)) {
                String level = request.getString(ARG_LEVELS + displayIdx,
                                   "0");
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_LEVEL_FROM,
                                           "#" + level,
                                           ImageGenerator.ATTR_LEVEL_TO,
                                           "#" + level));

            }


            if (request.defined(ARG_DISPLAYLISTLABEL + displayIdx)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "displayListTemplate", "value",
                            request.getString(
                                ARG_DISPLAYLISTLABEL + displayIdx, ""))));
            }



            if (request.defined(ARG_DISPLAYUNIT + displayIdx)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "settingsDisplayUnit", "value",
                            request.getString(
                                ARG_DISPLAYUNIT + displayIdx, ""))));
            }




            attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TYPE, display,
                                       ImageGenerator.ATTR_PARAM, param));


            if (request.defined(ARG_STRIDE + displayIdx)) {
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_STRIDE,
                                           request.getString(ARG_STRIDE
                                               + displayIdx, "1")));
            }


            List times = request.get(ARG_TIMES + displayIdx, new ArrayList());
            if (times.size() > 0) {
                if (times.size() > 1) {
                    multipleTimes = true;
                }
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TIMES,
                                           StringUtil.join(",", times)));
            }


            StringBuffer which =
                ((display.equals(DISPLAY_PLANVIEWCONTOURFILLED)
                  || display.equals(DISPLAY_PLANVIEWCOLOR))
                 ? firstDisplays
                 : secondDisplays);

            which.append(XmlUtil.tag(ImageGenerator.TAG_DISPLAY,
                                     attrs.toString(), propSB.toString()));
        }

        isl.append(firstDisplays);
        isl.append(secondDisplays);

        isl.append("</datasource>\n");
        isl.append("<pause/>\n");

        if(request.defined(ARG_AZIMUTH) && request.defined(ARG_TILT)) {
            isl.append(
                       XmlUtil.tag(
                                   ImageGenerator.TAG_VIEWPOINT,
                                   XmlUtil.attrs(
                                                 ImageGenerator.ATTR_AZIMUTH, request.getString(ARG_AZIMUTH,""),
                                                 ImageGenerator.ATTR_TILT, request.getString(ARG_TILT,""))));
        }

        if(request.defined(ARG_VIEWDIR)) {
            isl.append(
                       XmlUtil.tag(
                                   ImageGenerator.TAG_VIEWPOINT,
                                   XmlUtil.attrs(
                                                 ImageGenerator.ATTR_VIEWDIR, request.getString(ARG_VIEWDIR,""))));
        }


        if ( !forIsl) {
            isl.append(XmlUtil.tag((multipleTimes
                                    ? "movie"
                                    : "image"), XmlUtil.attr("file",
                                    imageFile.toString()), clip));
        }
        isl.append("</isl>\n");


        if (forJnlp) {
            String jnlp = getRepository().getResource(
                              "/ucar/unidata/repository/idv/template.jnlp");
            StringBuffer args = new StringBuffer();
            args.append("<argument>-b64isl</argument>");
            args.append("<argument>"
                        + XmlUtil.encodeBase64(isl.toString().getBytes())
                        + "</argument>");
            jnlp = jnlp.replace("${args}", args.toString());
            return new Result("data.jnlp", new StringBuffer(jnlp),
                              "application/x-java-jnlp-file");
        }

        if (forIsl) {
            return new Result("data.isl", new StringBuffer(isl), "text/xml");
        }



        long t1 = System.currentTimeMillis();
        idvServer.evaluateIsl(isl, props);
        long t2 = System.currentTimeMillis();
        System.err.println("isl time:" + (t2 - t1));


        synchronized (imageCache) {
            if (imageCache.size() > 1000) {
                imageCache = new Hashtable<String, File>();
            }
            imageCache.put(imageKey, imageFile);
        }


        Trace.stopTrace();
        return imageFile;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param group _more_
     * @param suGbroups _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result xxxoutputGroup(Request request, OutputType outputType,
                                 Group group, List<Group> subGroups,
                                 List<Entry> entries)
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
    public Result outputPointPage(final Request request, Entry entry)
            throws Exception {

        StringBuffer sb      = new StringBuffer();




        String       formUrl = getRepository().URL_ENTRY_SHOW.getFullUrl();
        StringBuffer formSB  = new StringBuffer();

        formSB.append(HtmlUtil.form(formUrl, ""));
        formSB.append(HtmlUtil.submit(msg("Make image"), ARG_SUBMIT));
        formSB.append(HtmlUtil.p());
        formSB.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        formSB.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_IDV_POINT));
        formSB.append(HtmlUtil.hidden(ARG_ACTION, ACTION_POINT_MAKEPAGE));
        formSB.append(HtmlUtil.formTable());
        StationModelManager smm = idvServer.getIdv().getStationModelManager();
        List                layoutModels     = smm.getStationModels();
        List                layoutModelNames = new ArrayList();
        for (StationModel sm : (List<StationModel>) layoutModels) {
            layoutModelNames.add(sm.getName());
        }


        formSB.append(HtmlUtil.formEntry(msgLabel("Layout Model"),
                                         htmlSelect(request,
                                             ARG_POINT_LAYOUTMODEL,
                                             layoutModelNames)));

        formSB.append(HtmlUtil.formEntry(msgLabel("Animate"),
                                         htmlCheckbox(request,
                                             ARG_POINT_DOANIMATION, false)));


        formSB.append(HtmlUtil.formEntry(msgLabel("Image Size"),
                                         htmlInput(request, ARG_IMAGE_WIDTH,
                                             "600") + HtmlUtil.space(1) + "X"
                                                 + HtmlUtil.space(1)
                                                     + htmlInput(request,
                                                         ARG_IMAGE_HEIGHT,
                                                             "400")));


        formSB.append(HtmlUtil.formTableClose());
        formSB.append(HtmlUtil.formClose());


        String url = getRepository().URL_ENTRY_SHOW.getFullUrl();
        String islUrl = url + "/" + IOUtil.stripExtension(entry.getName())
                        + ".isl";
        String jnlpUrl = url + "/" + IOUtil.stripExtension(entry.getName())
                         + ".jnlp";

        Hashtable exceptArgs = new Hashtable();
        exceptArgs.put(ARG_ACTION, ARG_ACTION);
        String args = request.getUrlArgs(exceptArgs, null, ".*_gvdflt");


        url = url + "?" + ARG_ACTION + "=" + ACTION_POINT_MAKEIMAGE + "&"
              + args;
        islUrl = islUrl + "?" + ARG_ACTION + "=" + ACTION_POINT_MAKEIMAGE
                 + "&" + args + "&" + ARG_TARGET + "=" + TARGET_ISL;
        jnlpUrl = jnlpUrl + "?" + ARG_ACTION + "=" + ACTION_POINT_MAKEIMAGE
                  + "&" + args + "&" + ARG_TARGET + "=" + TARGET_JNLP;

        StringBuffer imageSB = new StringBuffer();

        imageSB.append(HtmlUtil.img(url, "",
                                    HtmlUtil.attr(HtmlUtil.ATTR_WIDTH,
                                        request.getString(ARG_IMAGE_WIDTH,
                                            "400"))));

        if ( !request.exists(ARG_SUBMIT)) {
            sb.append(formSB);
        } else {
            sb.append(HtmlUtil.table(new Object[] { imageSB, formSB }, 10));
        }



        return new Result("Point Display", sb);

    }



    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    private String makeProperty(String name, boolean value) {
        return makeProperty(name, "" + value);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    private String makeProperty(String name, String value) {
        return XmlUtil.tag(ImageGenerator.TAG_PROPERTY,
                           XmlUtil.attrs("name", name, "value", value));
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
    public Result outputPointImage(final Request request, Entry entry)
            throws Exception {

        Trace.addNot(".*ShadowFunction.*");
        //      Trace.addNot(".*GeoGrid.*");
        //      Trace.addOnly(".*MapProjection.*");
        //      Trace.addOnly(".*ProjectionCoordinateSystem.*");
        Trace.startTrace();




        DataOutputHandler dataOutputHandler = getDataOutputHandler();
        String action = request.getString(ARG_ACTION, ACTION_POINT_MAKEPAGE);
        String            path              =
            dataOutputHandler.getPath(entry);
        if (path == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Could not load point data");
            return new Result("", sb);
        }


        path = "/Users/jeffmc/point.nc";


        FeatureDatasetPoint dataset =
            dataOutputHandler.getPointDataset(entry, path);
        DataSourceDescriptor descriptor =
            idvServer.getIdv().getDataManager().getDescriptor("NetCDF.POINT");
        NetcdfPointDataSource dataSource = new NetcdfPointDataSource(dataset,
                                               descriptor, new Hashtable());
        //      NetcdfPointDataSource dataSource = new NetcdfPointDataSource(descriptor, "/Users/jeffmc/point.nc",new Hashtable());

        try {


            String id = entry.getId();
            File image = getStorageManager().getThumbFile("preview_"
                             + id.replace("/", "_") + ".gif");

            boolean forIsl = request.getString(ARG_TARGET,
                                 "").equals(TARGET_ISL);
            boolean forJnlp = request.getString(ARG_TARGET,
                                  "").equals(TARGET_JNLP);
            if (forJnlp) {
                forIsl = true;
            }

            StringBuffer isl = new StringBuffer();
            isl.append(
                "<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");

            StringBuffer viewProps = new StringBuffer();
            viewProps.append(XmlUtil.tag("property",
                                         XmlUtil.attrs("name", "wireframe",
                                             "value", "true")));


            int width  = request.get(ARG_IMAGE_WIDTH, 400);
            int height = request.get(ARG_IMAGE_HEIGHT, 400);
            if (request.defined(ARG_VIEW_BOUNDS + "_south")
                    && request.defined(ARG_VIEW_BOUNDS + "_north")
                    && request.defined(ARG_VIEW_BOUNDS + "_east")
                    && request.defined(ARG_VIEW_BOUNDS + "_west")) {
                double south   = request.get(ARG_VIEW_BOUNDS + "_south", 0.0);
                double north   = request.get(ARG_VIEW_BOUNDS + "_north", 0.0);
                double east    = request.get(ARG_VIEW_BOUNDS + "_east", 0.0);
                double west    = request.get(ARG_VIEW_BOUNDS + "_west", 0.0);
                double bwidth  = Math.abs(east - west);
                double bheight = Math.abs(north - south);
                viewProps.append(makeProperty("initLatLonBounds",
                        west + ";" + north + ";" + bwidth + ";" + bheight));
                height = (int) (width * bheight / bwidth);
            }




            viewProps.append(
                makeProperty(
                    "background",
                    request.getString(ARG_VIEW_BACKGROUND, "black")));


            //Create a new viewmanager
            //For now don't do this if we are doing jnlp
            if ( !forJnlp) {
                isl.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_VIEW,
                        XmlUtil.attrs(
                            ImageGenerator.ATTR_WIDTH, "" + width,
                            ImageGenerator.ATTR_HEIGHT,
                            "" + height), viewProps.toString()));
            }


            if (forIsl) {
                isl.append(
                    XmlUtil.openTag(
                        ImageGenerator.TAG_DATASOURCE,
                        XmlUtil.attrs(
                            "id", "datasource", "url",
                            getRepository().absoluteUrl(
                                getRepository().URL_ENTRY_SHOW
                                + dataOutputHandler.getOpendapUrl(entry)))));
            } else {
                isl.append(XmlUtil.openTag(ImageGenerator.TAG_DATASOURCE,
                                           XmlUtil.attrs("id",
                                               "datasource")));
            }

            Hashtable props = new Hashtable();
            props.put("datasource", dataSource);
            StringBuffer propSB = new StringBuffer();
            StringBuffer attrs  = new StringBuffer();
            propSB.append(makeProperty("id", "thedisplay"));
            propSB.append(
                makeProperty(
                    "stationModelName",
                    request.getString(ARG_POINT_LAYOUTMODEL, "Location")));


            attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TYPE,
                                       "stationmodelcontrol",
                                       ImageGenerator.ATTR_PARAM, "*"));

            isl.append(XmlUtil.tag(ImageGenerator.TAG_DISPLAY,
                                   attrs.toString(), propSB.toString()));
            isl.append("</datasource>\n");
            isl.append("<pause/>\n");



            String clip = "";
            if (request.get(ARG_CLIP, false)) {
                clip = XmlUtil.tag("clip", "");
            }

            boolean multipleTimes = request.get(ARG_POINT_DOANIMATION, false);
            isl.append(XmlUtil.tag((multipleTimes
                                    ? "movie"
                                    : "image"), XmlUtil.attr("file",
                                    image.toString()), clip));
            isl.append("</isl>\n");

            if (forJnlp) {
                String jnlp =
                    getRepository().getResource(
                        "/ucar/unidata/repository/idv/template.jnlp");
                StringBuffer args = new StringBuffer();
                args.append("<argument>-b64isl</argument>");
                args.append("<argument>"
                            + XmlUtil.encodeBase64(isl.toString().getBytes())
                            + "</argument>");
                jnlp = jnlp.replace("${args}", args.toString());
                return new Result("data.jnlp", new StringBuffer(jnlp),
                                  "application/x-java-jnlp-file");
            }
            if (forIsl) {
                return new Result("data.isl", new StringBuffer(isl),
                                  "text/xml");
            }

            long t1 = System.currentTimeMillis();
            idvServer.evaluateIsl(isl, props);
            long t2 = System.currentTimeMillis();
            System.err.println("isl time:" + (t2 - t1));

            Trace.stopTrace();
            return new Result("preview.gif",
                              getStorageManager().getFileInputStream(image),
                              "image/gif");
        } finally {
            dataOutputHandler.returnPointDataset(path, dataset);
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
    public Result outputPoint(final Request request, Entry entry)
            throws Exception {
        String action = request.getString(ARG_ACTION, ACTION_POINT_MAKEPAGE);
        if (action.equals(ACTION_POINT_MAKEPAGE)) {
            return outputPointPage(request, entry);
        } else {
            return outputPointImage(request, entry);
        }
    }




}

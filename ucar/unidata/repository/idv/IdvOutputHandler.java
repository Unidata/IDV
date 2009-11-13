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

import ucar.nc2.ft.FeatureDatasetPoint;

import ucar.unidata.data.*;
import ucar.unidata.data.DataCategory;

import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.grid.*;
import ucar.unidata.data.point.NetcdfPointDataSource;
import ucar.unidata.idv.ControlDescriptor;


import ucar.unidata.idv.IdvBase;
import ucar.unidata.idv.IdvServer;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.ImageGenerator;

import ucar.unidata.repository.*;
import ucar.unidata.repository.data.*;
import ucar.unidata.repository.output.*;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;

import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.util.CacheManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.ThreeDSize;
import ucar.unidata.util.Trace;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;


import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class IdvOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String ARG_IMAGE_CROPX1 = "image.edit.cropx1";

    /** _more_ */
    public static final String ARG_IMAGE_CROPY1 = "image.edit.cropy1";

    /** _more_ */
    public static final String ARG_IMAGE_CROPX2 = "image.edit.cropx2";

    /** _more_ */
    public static final String ARG_IMAGE_CROPY2 = "image.edit.cropy2";


    /** _more_          */
    public static final String ARG_PARAM = "param";

    /** _more_          */
    public static final String ARG_TARGET = "target";

    /** _more_          */
    public static final String TARGET_IMAGE = "image";

    /** _more_          */
    public static final String TARGET_JNLP = "jnlp";

    /** _more_          */
    public static final String TARGET_ISL = "isl";

    public static final String ARG_ZOOM = "zoom";


    /** _more_          */
    public static final String ARG_POINT_LAYOUTMODEL = "point.layoutmodel";

    /** _more_          */
    public static final String ARG_POINT_DOANIMATION = "point.doanimation";

    /** _more_          */
    public static final String ARG_DISPLAYLISTLABEL = "displaylistlabel";

    /** _more_          */
    public static final String ARG_COLORTABLE = "colortable";

    /** _more_          */
    public static final String ARG_STRIDE = "stride";


    /** _more_          */
    public static final String ARG_FLOW_SCALE = "flow_scale";

    /** _more_          */
    public static final String ARG_FLOW_DENSITY = "flow_density";

    /** _more_          */
    public static final String ARG_FLOW_SKIP = "flow_skip";

    /** _more_          */
    public static final String ARG_DISPLAYUNIT = "displayunit";

    /** _more_          */
    public static final String ARG_ISOSURFACEVALUE = "isosurfacevalue";

    /** _more_          */
    public static final String ARG_CONTOUR_MIN = "contour_min";

    /** _more_          */
    public static final String ARG_CONTOUR_MAX = "contour_max";

    /** _more_          */
    public static final String ARG_CONTOUR_INTERVAL = "contour_interval";

    /** _more_          */
    public static final String ARG_CONTOUR_BASE = "contour_base";

    /** _more_          */
    public static final String ARG_CONTOUR_DASHED = "contour_dashed";

    /** _more_          */
    public static final String ARG_CONTOUR_LABELS = "contour_labels";



    /** _more_          */
    public static final String ARG_SCALE_VISIBLE = "scale_visible";

    /** _more_          */
    public static final String ARG_SCALE_ORIENTATION = "scale_orientation";

    /** _more_          */
    public static final String ARG_SCALE_PLACEMENT = "scale_placement";


    /** _more_          */
    public static final String ARG_RANGE_MIN = "range_min";

    /** _more_          */
    public static final String ARG_RANGE_MAX = "range_max";

    /** _more_          */
    public static final String ARG_DISPLAY = "display";

    /** _more_          */
    public static final String ARG_ACTION = "action";

    /** _more_          */
    public static final String ARG_TIMES = "times";

    /** _more_          */
    public static final String ARG_SHOWMAP = "showmap";

    /** _more_          */
    public static final String ARG_CLIP = "clip";

    /** _more_          */
    public static final String ARG_WHITEBACKGROUND = "whitebackground";

    /** _more_          */
    public static final String ARG_LEVELS = "levels";

    /** _more_          */
    public static final String ARG_IMAGE_WIDTH = "imagewidth";

    /** _more_          */
    public static final String ARG_IMAGE_HEIGHT = "imageheight";


    /** _more_          */
    public static final String ACTION_GRID_MAKEINITFORM =
        "action.grid.makeinitform";

    /** _more_          */
    public static final String ACTION_GRID_MAKEFORM = "action.grid.makeform";

    /** _more_          */
    public static final String ACTION_GRID_MAKEPAGE = "action.grid.makepage";

    /** _more_          */
    public static final String ACTION_GRID_MAKEIMAGE =
        "action.grid.makeimage";


    /** _more_          */
    public static final String ACTION_POINT_MAKEPAGE =
        "action.point.makepage";

    /** _more_          */
    public static final String ACTION_POINT_MAKEIMAGE =
        "action.point.makeimage";

    /** _more_          */
    public static final int NUM_PARAMS = 3;





    /** _more_          */
    public static final String DISPLAY_PLANVIEWFLOW = "planviewflow";

    /** _more_          */
    public static final String DISPLAY_STREAMLINES = "streamlines";

    /** _more_          */
    public static final String DISPLAY_WINDBARBPLAN = "windbarbplan";


    /** _more_          */
    public static final String DISPLAY_PLANVIEWCONTOUR = "planviewcontour";

    /** _more_          */
    public static final String DISPLAY_PLANVIEWCONTOURFILLED =
        "planviewcontourfilled";

    /** _more_          */
    public static final String DISPLAY_PLANVIEWCOLOR = "planviewcolor";

    /** _more_          */
    public static final String DISPLAY_ISOSURFACE = "isosurface";



    //    public static void processScript(String scriptFile) throws Exception {


    /** _more_ */
    public static final OutputType OUTPUT_IDV_GRID =
        new OutputType("Grid Displays", "idv.grid", OutputType.TYPE_HTML,
                       OutputType.SUFFIX_NONE, ICON_PLANVIEW);


    /** _more_          */
    public static final OutputType OUTPUT_IDV_POINT =
        new OutputType("Point Displays", "idv.point", OutputType.TYPE_HTML,
                       OutputType.SUFFIX_NONE, ICON_PLANVIEW);


    /** _more_ */
    IdvServer idvServer;

    /** _more_ */
    int callCnt = 0;

    /** _more_          */
    private HashSet<String> okControls;


    /** _more_          */
    private boolean idvOk = false;


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

        okControls = new HashSet<String>();
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
        addType(OUTPUT_IDV_GRID);
        addType(OUTPUT_IDV_POINT);
        //Call this in a thread because if there is any problem with xvfb this will just hang
        Misc.run(this, "checkIdv");

    }


    /**
     * _more_
     */
    public void checkIdv() {
        try {
            java.awt.GraphicsEnvironment e =
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            e.getDefaultScreenDevice();
            idvServer =
                new IdvServer(new File(getStorageManager().getDir("idv")));
            idvOk = true;
        } catch (Throwable exc) {
            logError(
                "To run the IdvOutputHandler a graphics environment is needed",
                exc);
        }
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
        if (output.equals(OUTPUT_IDV_POINT)) {
            return outputPoint(request, entry);
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
        DataOutputHandler dataOutputHandler = getDataOutputHandler();
        String action = request.getString(ARG_ACTION,
                                          ACTION_GRID_MAKEINITFORM);
        String path = dataOutputHandler.getPath(entry);
        if (path == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Could not load grid");
            return new Result("Grid Displays", sb);
        }

        GridDataset dataset = dataOutputHandler.getGridDataset(entry, path);


        DataSourceDescriptor descriptor =
            idvServer.getIdv().getDataManager().getDescriptor("File.Grid");
        GeoGridDataSource dataSource = new GeoGridDataSource(descriptor,
                                           dataset, entry.getName(), path);

        try {
            if (action.equals(ACTION_GRID_MAKEINITFORM)) {
                return outputGridInitForm(request, entry, dataSource);
            } else if (action.equals(ACTION_GRID_MAKEFORM)) {
                return outputGridForm(request, entry, dataSource);
            } else if (action.equals(ACTION_GRID_MAKEPAGE)) {
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
                                  GeoGridDataSource dataSource)
            throws Exception {

        StringBuffer sb      = new StringBuffer();
        makeGridForm(request, sb, entry, dataSource);
        return new Result("Grid Displays", sb);
    }

        

    private String htmlCheckbox(Request request, String arg, boolean dflt) {
        return  HtmlUtil.checkbox(arg,  "true", request.get(arg, dflt));
    }


    private String htmlSelect(Request request, String arg, List items, String extra) {
        List selected  = request.get(arg,new ArrayList());
        return HtmlUtil.select(arg, items, selected, extra);
    }

    private String htmlSelect(Request request, String arg, List items) {
        return htmlSelect(request, arg, items,"");
    }


    private String htmlInput(Request request, String arg, String dflt, int width) {
        return HtmlUtil.input(arg,request.getString(arg, dflt),
                              HtmlUtil.attr(HtmlUtil.ATTR_SIZE,
                                            ""+width));
    }


    private String htmlInput(Request request, String arg, String dflt) {
        return htmlInput(request, arg, dflt, 5);
    }

    private void makeGridForm(Request request, StringBuffer sb, Entry entry,
                              GeoGridDataSource dataSource)
        throws Exception {

        String       formUrl = getRepository().URL_ENTRY_SHOW.getFullUrl();
        sb.append(HtmlUtil.form(formUrl, ""));
        sb.append(HtmlUtil.submit(msg("Make image"), ARG_SUBMIT));
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_IDV_GRID));
        sb.append(HtmlUtil.hidden(ARG_ACTION, ACTION_GRID_MAKEPAGE));

        sb.append(HtmlUtil.hidden(ARG_IMAGE_CROPX1, "",
                                  HtmlUtil.SIZE_3
                                  + HtmlUtil.id(ARG_IMAGE_CROPX1)));
        sb.append(HtmlUtil.hidden(ARG_IMAGE_CROPY1, "",
                                  HtmlUtil.SIZE_3
                                  + HtmlUtil.id(ARG_IMAGE_CROPY1)));
        sb.append(HtmlUtil.hidden(ARG_IMAGE_CROPX2, "",
                                  HtmlUtil.SIZE_3
                                  + HtmlUtil.id(ARG_IMAGE_CROPX2)));
        sb.append(HtmlUtil.hidden(ARG_IMAGE_CROPY2, "",
                                  HtmlUtil.SIZE_3
                                  + HtmlUtil.id(ARG_IMAGE_CROPY2)));

        sb.append(HtmlUtil.div("",
                               HtmlUtil.cssClass("image_edit_box")
                               + HtmlUtil.id("image_edit_box")));

        StringBuffer basic = new StringBuffer();
        basic.append(HtmlUtil.formTable());
        basic.append(HtmlUtil.formEntry(msgLabel("Show map"),
                                        HtmlUtil.checkbox(ARG_SHOWMAP,
                                            "true", true)));

        basic.append(HtmlUtil.formEntry(msgLabel("Clip image"),
                                        HtmlUtil.checkbox(ARG_CLIP, "true",
                                            false)));

        basic.append(
            HtmlUtil.formEntry(
                msgLabel("White Background"),
                HtmlUtil.checkbox(ARG_WHITEBACKGROUND, "true", false)));


        basic.append(HtmlUtil.formEntry(msgLabel("Width"),
                                        htmlInput(request,ARG_IMAGE_WIDTH,
                                                  "600")));


        basic.append(HtmlUtil.formEntry(msgLabel("Height"),
                                        htmlInput(request,
                                                  ARG_IMAGE_HEIGHT,
                                                  "400")));


        basic.append(HtmlUtil.formEntry(msgLabel("Zoom"),
                                        htmlInput(request,ARG_ZOOM,
                                                  "")));



        basic.append(HtmlUtil.formTableClose());


        List<String> tabLabels   = new ArrayList<String>();
        List<String> tabContents = new ArrayList<String>();
        tabLabels.add(msg("Basic"));
        tabContents.add(basic.toString());


        List colorTables =
            idvServer.getIdv().getColorTableManager().getColorTables();


        Hashtable<String, DataChoice> idToChoice = new Hashtable<String,
                                                       DataChoice>();
        List<DataChoice> choices =
            (List<DataChoice>) dataSource.getDataChoices();
        for (DataChoice dataChoice : choices) {
            idToChoice.put(dataChoice.getName(), dataChoice);
        }



        for (int displayIdx = 1; displayIdx <= NUM_PARAMS; displayIdx++) {
            if ( !request.defined(ARG_PARAM + displayIdx)) {
                continue;
            }

            List<String> innerTabTitles   = new ArrayList<String>();
            List<String> innerTabContents = new ArrayList<String>();

            StringBuffer tab              = new StringBuffer();
            String       param = request.getString(ARG_PARAM + displayIdx,
                                     "");
            DataChoice   choice           = idToChoice.get(param);
            if (choice == null) {
                continue;
            }


            List descriptors =
                new ArrayList(
                    ControlDescriptor.getApplicableControlDescriptors(
                        choice.getCategories(),
                        idvServer.getIdv().getControlDescriptors(true)));




            List displays = new ArrayList();
            displays.add(new TwoFacedObject("--skip--", ""));
            for (ControlDescriptor controlDescriptor : (List<ControlDescriptor>) descriptors) {
                String controlId = controlDescriptor.getControlId();
                if ( !okControls.contains(controlId)) {
                    continue;
                }
                //              System.out.println (controlId +"  " + controlDescriptor.getLabel());
                displays.add(new TwoFacedObject(controlDescriptor.getLabel(),
                        controlId));
            }


            tab.append(HtmlUtil.hidden(ARG_PARAM + displayIdx,
                                       request.getString(ARG_PARAM
                                           + displayIdx, "")));
            List options = new ArrayList();
            options.add("");
            tab.append(HtmlUtil.br());
            tab.append(msgLabel("Display Type"));
            tab.append(HtmlUtil.space(1));
            tab.append(htmlSelect(request, ARG_DISPLAY + displayIdx, displays));
            tab.append(HtmlUtil.p());

            List times = choice.getAllDateTimes();
            if ((times != null) && (times.size() > 0)) {
                List tfoTimes = new ArrayList();
                int  cnt      = 0;
                for (Object time : times) {
                    tfoTimes.add(new TwoFacedObject(time.toString(),
                            new Integer(cnt++)));
                }
                innerTabTitles.add(msg("Times"));
                innerTabContents.add(htmlSelect(request, ARG_TIMES + displayIdx,
                        tfoTimes, 
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
                                    HtmlUtil.attrs(HtmlUtil.ATTR_MULTIPLE,
                                        "false", HtmlUtil.ATTR_SIZE, "5"));
                spatialComps.add(msgLabel("Levels"));
                spatialComps.add(levelWidget);
            }


            ThreeDSize size = (ThreeDSize) choice.getProperty(
                                  GeoGridDataSource.PROP_GRIDSIZE);
            spatialComps.add(msgLabel("X/Y Stride"));
            String strideComp = htmlInput(request,ARG_STRIDE + displayIdx, "");

            if (size != null) {
                strideComp = strideComp + HtmlUtil.br() + size;
            }
            spatialComps.add(strideComp);
            String spatial =
                HtmlUtil.table(Misc.listToStringArray(spatialComps), 5);
            innerTabTitles.add(msg("Spatial"));
            innerTabContents.add(spatial);




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
                String div =
                    HtmlUtil.div(HtmlUtil.img(getRepository().getUrlBase()
                        + "/colortables/" + icon) + " "
                            + colorTable.getName(), "");
                String call = HtmlUtil.call("setFormValue",
                                            "'" + ARG_COLORTABLE + displayIdx
                                            + "','" + colorTable.getName()
                                            + "'") + ";"
                                                + HtmlUtil.call("setHtml",
                                                    "'" + ARG_COLORTABLE
                                                    + "_html" + displayIdx
                                                    + "','"
                                                    + colorTable.getName()
                                                    + "'");

                catSB.append(HtmlUtil.mouseClickHref(call, div));
            }


            StringBuffer ctsb = new StringBuffer();
            ctsb.append(
                msgLabel("Range") + HtmlUtil.space(1)
                + htmlInput(request,
                            ARG_RANGE_MIN + displayIdx, "") + " - "
                        + htmlInput(request, ARG_RANGE_MAX + displayIdx, ""));
            
            ctsb.append(HtmlUtil.hidden(ARG_COLORTABLE + displayIdx, "",
                                        HtmlUtil.id("" + ARG_COLORTABLE
                                            + displayIdx)));
            ctsb.append(HtmlUtil.div("-default-",
                                     HtmlUtil.id(ARG_COLORTABLE + "_html"
                                         + displayIdx)));
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
                    HtmlUtil.checkbox(ARG_SCALE_VISIBLE + displayIdx, "true",
                                      false)));
            scalesb.append(HtmlUtil.formEntry(msgLabel("Place"),
                    htmlSelect(request, ARG_SCALE_PLACEMENT + displayIdx,
                                    Misc.newList("top", "left", "bottom",
                                        "right"))));
            scalesb.append(HtmlUtil.formTableClose());





            StringBuffer contoursb = new StringBuffer();
            contoursb.append(HtmlUtil.formTable());
            contoursb.append(HtmlUtil.formEntry(msgLabel("Interval"),
                                                htmlInput(request,ARG_CONTOUR_INTERVAL + displayIdx, "", 3)));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Base"),
                                                htmlInput(request,ARG_CONTOUR_BASE + displayIdx, "", 3)));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Min"),
                                                htmlInput(request,ARG_CONTOUR_MIN + displayIdx, "",3)));

            contoursb.append(HtmlUtil.formEntry(msgLabel("Max"),
                                                htmlInput(request,ARG_CONTOUR_MAX + displayIdx, "",3)));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Dashed"),
                    HtmlUtil.checkbox(ARG_CONTOUR_DASHED + displayIdx,
                                      "true", false)));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Labels"),
                    HtmlUtil.checkbox(ARG_CONTOUR_LABELS + displayIdx,
                                      "true", true)));
            contoursb.append(HtmlUtil.formTableClose());


            StringBuffer misc = new StringBuffer();
            misc.append(HtmlUtil.formTable());
            misc.append(
                HtmlUtil.formEntry(
                    msgLabel("Display List Label"),
                    htmlInput(request,
                              ARG_DISPLAYLISTLABEL + displayIdx, "", 30)));
            misc.append(
                HtmlUtil.formEntry(
                    msgLabel("Display Unit"),
                    htmlInput(request, ARG_DISPLAYUNIT + displayIdx, "",6)));
            misc.append(
                HtmlUtil.formEntry(
                    msgLabel("Isosurface Value"),
                    htmlInput(request,
                              ARG_ISOSURFACEVALUE + displayIdx, "",3)));
            misc.append(
                HtmlUtil.formEntry(
                    msgLabel("Vector/Barb Size"),
                    htmlInput(request,
                              ARG_FLOW_SCALE + displayIdx, "4", 3)));
            misc.append(
                HtmlUtil.formEntry(
                    msgLabel("Streamline Density"),
                    htmlInput(request,
                              ARG_FLOW_DENSITY + displayIdx, "1",3)));
            misc.append(
                HtmlUtil.formEntry(
                    msgLabel("Flow Skip"),
                    htmlInput(request,
                              ARG_FLOW_SKIP + displayIdx, "0",3)));


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

            tabLabels.add(StringUtil.camelCase(choice.getDescription()));
            tabContents.add(tab.toString());
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
                                      GeoGridDataSource dataSource)
            throws Exception {
        StringBuffer sb      = new StringBuffer();

        String       formUrl = getRepository().URL_ENTRY_SHOW.getFullUrl();
        sb.append(HtmlUtil.form(formUrl, ""));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_IDV_GRID));
        sb.append(HtmlUtil.hidden(ARG_ACTION, ACTION_GRID_MAKEFORM));


        List<DataChoice> choices =
            (List<DataChoice>) dataSource.getDataChoices();


        sb.append(msgHeader("Select one or more fields to view"));

        StringBuffer fields = new StringBuffer();
        for (int i = 1; i <= NUM_PARAMS; i++) {
            List options = new ArrayList();
            options.add(new TwoFacedObject("--Pick one--", ""));
            List<String> cats = new ArrayList<String>();
            Hashtable<String, List<TwoFacedObject>> catMap =
                new Hashtable<String, List<TwoFacedObject>>();
            for (DataChoice dataChoice : choices) {
                String label =
                    StringUtil.camelCase(dataChoice.getDescription());
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

            fields.append(htmlSelect(request, ARG_PARAM + i, options));
            fields.append(HtmlUtil.p());
        }
        sb.append(HtmlUtil.insetLeft(fields.toString(), 10));
        sb.append(HtmlUtil.submit(msg("Make image"), ARG_SUBMIT));
        sb.append(HtmlUtil.formClose());
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
    private Result outputGridPage(final Request request, Entry entry,
                                  GeoGridDataSource dataSource)
            throws Exception {
        StringBuffer sb  = new StringBuffer();





        String       url = getRepository().URL_ENTRY_SHOW.getFullUrl();
        String islUrl = url + "/" + IOUtil.stripExtension(entry.getName())
                        + ".isl";
        String jnlpUrl = url + "/" + IOUtil.stripExtension(entry.getName())
                         + ".jnlp";

        Hashtable exceptArgs = new Hashtable();
        exceptArgs.put(ARG_ACTION, ARG_ACTION);
        String args = request.getUrlArgs(exceptArgs, null);
        url = url + "?" + ARG_ACTION + "=" + ACTION_GRID_MAKEIMAGE + "&"
              + args;


        islUrl = islUrl + "?" + ARG_ACTION + "=" + ACTION_GRID_MAKEIMAGE
                 + "&" + args + "&" + ARG_TARGET + "=" + TARGET_ISL;
        jnlpUrl = jnlpUrl + "?" + ARG_ACTION + "=" + ACTION_GRID_MAKEIMAGE
                  + "&" + args + "&" + ARG_TARGET + "=" + TARGET_JNLP;

        String clickParams =
            "event,'imgid',"
            + HtmlUtil.comma(HtmlUtil.squote(ARG_IMAGE_CROPX1),
                             HtmlUtil.squote(ARG_IMAGE_CROPY1),
                             HtmlUtil.squote(ARG_IMAGE_CROPX2),
                             HtmlUtil.squote(ARG_IMAGE_CROPY2));

        String call = HtmlUtil.onMouseClick(HtmlUtil.call("editImageClick",
                          clickParams));


        sb.append(HtmlUtil.img(url, "Image is being processed...",
                               HtmlUtil.id("imgid") + call +
                                    HtmlUtil.attr(HtmlUtil.ATTR_WIDTH,
                                        request.getString(ARG_IMAGE_WIDTH,
                                                          "600")) +
                                    HtmlUtil.attr(HtmlUtil.ATTR_HEIGHT,
                                        request.getString(ARG_IMAGE_HEIGHT,
                                                          "400"))));

        StringBuffer formSB      = new StringBuffer();
        formSB.append(HtmlUtil.href(jnlpUrl, msg("Launch in the IDV")));
        formSB.append(HtmlUtil.br());
        formSB.append(HtmlUtil.href(islUrl, msg("Download IDV ISL script")));
        makeGridForm(request, formSB, entry, dataSource);
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.makeShowHideBlock(msg("Form"),
                                                 formSB.toString(), false));

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
                                  GeoGridDataSource dataSource)
            throws Exception {

        DataOutputHandler dataOutputHandler = getDataOutputHandler();

        //      Trace.addNot(".*ShadowFunction.*");
        //      Trace.addNot(".*GeoGrid.*");
        //      Trace.addOnly(".*MapProjection.*");
        //      Trace.addOnly(".*ProjectionCoordinateSystem.*");
        //Trace.startTrace();

        String id = entry.getId();
        File image = getStorageManager().getThumbFile("preview_"
                         + id.replace("/", "_") + ".gif");

        boolean forIsl = request.getString(ARG_TARGET, "").equals(TARGET_ISL);
        boolean forJnlp = request.getString(ARG_TARGET,
                                            "").equals(TARGET_JNLP);
        if (forJnlp) {
            forIsl = true;
        }

        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");

        StringBuffer viewProps = new StringBuffer();
        viewProps.append(XmlUtil.tag("property",
                                     XmlUtil.attrs("name", "wireframe",
                                         "value", "false")));
        viewProps.append(XmlUtil.tag("property",
                                     XmlUtil.attrs("name", "showMaps",
                                         "value",
                                         "" + request.get(ARG_SHOWMAP,
                                             false))));
        if (request.get(ARG_WHITEBACKGROUND, false)) {
            viewProps.append(XmlUtil.tag("property",
                                         XmlUtil.attrs("name", "background",
                                             "value", "white")));
            viewProps.append(XmlUtil.tag("property",
                                         XmlUtil.attrs("name", "foreground",
                                             "value", "black")));
        }

        if(request.defined(ARG_ZOOM)) {
            viewProps.append(makeProperty("displayProjectionZoom",""+request.get(ARG_ZOOM, 0.0)));
        }

        //Create a new viewmanager
        //For now don't do this if we are doing jnlp
        if ( !forJnlp) {
            isl.append(XmlUtil.tag(
                                                     ImageGenerator.TAG_VIEW,
                                                     XmlUtil.attrs(
                                                                   ImageGenerator.ATTR_WIDTH,
                                                                   request.getString(ARG_IMAGE_WIDTH, "400"),
                                                                   ImageGenerator.ATTR_HEIGHT,
                                                                   request.getString(
                                                                                     ARG_IMAGE_HEIGHT, "300")), viewProps.toString()));
        }



        if (forIsl) {
            isl.append(
                XmlUtil.openTag(
                    "datasource",
                    XmlUtil.attrs(
                        "id", "datasource", "url",
                        getRepository().absoluteUrl(
                            getRepository().URL_ENTRY_SHOW
                            + dataOutputHandler.getOpendapUrl(entry)))));
        } else {
            isl.append("<datasource id=\"datasource\" times=\"0\" >\n");
        }

        Hashtable props = new Hashtable();
        props.put("datasource", dataSource);
        StringBuffer firstDisplays  = new StringBuffer();
        StringBuffer secondDisplays = new StringBuffer();
        boolean      multipleTimes  = false;

        for (int displayIdx = 1; displayIdx <= NUM_PARAMS; displayIdx++) {
            if ( !request.defined(ARG_PARAM + displayIdx)
                    || !request.defined(ARG_DISPLAY + displayIdx)) {
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


            if (display.equals(DISPLAY_PLANVIEWFLOW)
                    || display.equals(DISPLAY_STREAMLINES)
                    || display.equals(DISPLAY_WINDBARBPLAN)) {
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "flowScale", "value",
                            request.getString(
                                ARG_FLOW_SCALE + displayIdx, ""))));
                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "streamlineDensity", "value",
                            request.getString(
                                ARG_FLOW_DENSITY + displayIdx, ""))));

                propSB.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_PROPERTY,
                        XmlUtil.attrs(
                            "name", "skipValue", "value",
                            request.getString(
                                ARG_FLOW_SKIP + displayIdx, ""))));

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
            } else {
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

            System.err.println("Props:" + propSB);
            attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TYPE, display,
                                       ImageGenerator.ATTR_PARAM,
                                       request.getString(ARG_PARAM
                                           + displayIdx, "")));

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

            StringBuffer which = (display.equals(DISPLAY_PLANVIEWCONTOUR)
                                  ? secondDisplays
                                  : firstDisplays);

            which.append(XmlUtil.tag(ImageGenerator.TAG_DISPLAY,
                                     attrs.toString(), propSB.toString()));
        }

        isl.append(firstDisplays);
        isl.append(secondDisplays);

        isl.append("</datasource>\n");
        //        isl.append("<pause/>\n");
        String clip = "";
        if (request.get(ARG_CLIP, false)) {
            clip = XmlUtil.tag("clip", "");
        }


        if ( !forIsl) {
            isl.append(XmlUtil.tag((multipleTimes
                                    ? "movie"
                                    : "image"), XmlUtil.attr("file",
                                    image.toString()), clip));
        }
        isl.append("</isl>\n");
        //        System.out.println(isl);


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
     * @param subGroups _more_
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


        formSB.append(
            HtmlUtil.formEntry(
                msgLabel("Layout Model"),
                htmlSelect(request, 
                           ARG_POINT_LAYOUTMODEL, layoutModelNames)));

        formSB.append(
            HtmlUtil.formEntry(
                msgLabel("Animate"),
                HtmlUtil.checkbox(ARG_POINT_DOANIMATION, "true", false)));


        formSB.append(
            HtmlUtil.formEntry(
                msgLabel("Width"),
                htmlInput(request, ARG_IMAGE_WIDTH, "600")));

        formSB.append(
            HtmlUtil.formEntry(
                msgLabel("Height"),
                htmlInput(request, ARG_IMAGE_HEIGHT,  "400")));

        formSB.append(HtmlUtil.formEntry(msgLabel("Show map"),
                                         HtmlUtil.checkbox(ARG_SHOWMAP,
                                             "true",
                                             request.get(ARG_SHOWMAP,
                                                 true))));

        formSB.append(HtmlUtil.formTableClose());
        formSB.append(HtmlUtil.formClose());


        String url = getRepository().URL_ENTRY_SHOW.getFullUrl();
        String islUrl = url + "/" + IOUtil.stripExtension(entry.getName())
                        + ".isl";
        String jnlpUrl = url + "/" + IOUtil.stripExtension(entry.getName())
                         + ".jnlp";

        Hashtable exceptArgs = new Hashtable();
        exceptArgs.put(ARG_ACTION, ARG_ACTION);
        String args = request.getUrlArgs(exceptArgs, null);
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
        imageSB.append(HtmlUtil.br());
        imageSB.append(HtmlUtil.href(jnlpUrl, msg("Launch in the IDV")));
        imageSB.append(HtmlUtil.br());
        imageSB.append(HtmlUtil.href(islUrl, msg("Download IDV ISL script")));

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
                                             "value", "false")));
            viewProps.append(XmlUtil.tag("property",
                                         XmlUtil.attrs("name", "showMaps",
                                             "value",
                                             "" + request.get(ARG_SHOWMAP,
                                                 false))));
            if (request.get(ARG_WHITEBACKGROUND, false)) {
                viewProps.append(XmlUtil.tag("property",
                                             XmlUtil.attrs("name",
                                                 "background", "value",
                                                     "white")));
                viewProps.append(XmlUtil.tag("property",
                                             XmlUtil.attrs("name",
                                                 "foreground", "value",
                                                     "black")));
            }



            //Create a new viewmanager
            //For now don't do this if we are doing jnlp
            if ( !forJnlp) {
                isl.append(
                    XmlUtil.tag(
                        ImageGenerator.TAG_VIEW,
                        XmlUtil.attrs(
                            ImageGenerator.ATTR_WIDTH,
                            request.getString(ARG_IMAGE_WIDTH, "400"),
                            ImageGenerator.ATTR_HEIGHT,
                            request.getString(
                                ARG_IMAGE_HEIGHT,
                                "300")), viewProps.toString()));
            }


            if (forIsl) {
                isl.append(
                    XmlUtil.openTag(
                        "datasource",
                        XmlUtil.attrs(
                            "id", "datasource", "url",
                            getRepository().absoluteUrl(
                                getRepository().URL_ENTRY_SHOW
                                + dataOutputHandler.getOpendapUrl(entry)))));
            } else {
                isl.append("<datasource id=\"datasource\">\n");
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

            System.err.println("Props:" + propSB);
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


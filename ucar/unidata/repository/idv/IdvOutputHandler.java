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
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataCategory;

import ucar.unidata.data.*;
import ucar.unidata.data.grid.*;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.idv.IdvServer;
import ucar.unidata.idv.ControlDescriptor;


import ucar.unidata.idv.IdvBase;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.ImageGenerator;

import ucar.unidata.repository.*;
import ucar.unidata.repository.data.*;
import ucar.unidata.repository.output.*;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.ThreeDSize;
import ucar.unidata.util.ColorTable;
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


import java.util.HashSet;
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

    public static final String ARG_TARGET = "target";
    public static final String TARGET_IMAGE = "image";
    public static final String TARGET_JNLP = "jnlp";
    public static final String TARGET_ISL = "isl";

    public static final String ARG_DISPLAYLISTLABEL = "displaylistlabel";
    public static final String ARG_COLORTABLE= "colortable";
    public static final String ARG_STRIDE = "stride";


    public static final String ARG_FLOW_SCALE = "flow_scale";
    public static final String ARG_FLOW_DENSITY = "flow_density";
    public static final String ARG_FLOW_SKIP = "flow_skip";

    public static final String ARG_DISPLAYUNIT = "displayunit";
    public static final String ARG_ISOSURFACEVALUE = "isosurfacevalue";
    public static final String ARG_CONTOUR_MIN= "contour_min";
    public static final String ARG_CONTOUR_MAX= "contour_max";
    public static final String ARG_CONTOUR_INTERVAL= "contour_interval";
    public static final String ARG_CONTOUR_BASE= "contour_base";
    public static final String ARG_CONTOUR_DASHED= "contour_dashed";
    public static final String ARG_CONTOUR_LABELS= "contour_labels";



    public static final String ARG_SCALE_VISIBLE="scale_visible";
    public static final String ARG_SCALE_ORIENTATION="scale_orientation";
    public static final String ARG_SCALE_PLACEMENT = "scale_placement";
    //    public static final String ARG_SCALE="";


    public static final String ARG_RANGE_MIN= "range_min";
    public static final String ARG_RANGE_MAX= "range_max";
    public static final String ARG_DISPLAY = "display";
    public static final String ARG_ACTION = "action";
    public static final String ARG_TIMES = "times";
    public static final String ARG_SHOWMAP = "showmap";
    public static final String ARG_CLIP = "clip";
    public static final String ARG_WHITEBACKGROUND = "whitebackground";

    public static final String ARG_LEVELS = "levels";

    public static final String ARG_IMAGE_WIDTH = "imagewidth";
    public static final String ARG_IMAGE_HEIGHT = "imageheight";


    public static final String ACTION_MAKEINITFORM = "action.makeinitform";
    public static final String ACTION_MAKEFORM = "action.makeform";
    public static final String ACTION_MAKEPAGE = "action.makepage";
    public static final String ACTION_MAKEIMAGE = "action.makeimage";

    public static final int  NUM_PARAMS = 3;
    




    public static final String DISPLAY_PLANVIEWFLOW  = "planviewflow";
    public static final String DISPLAY_STREAMLINES = "streamlines";  
    public static final String DISPLAY_WINDBARBPLAN = "windbarbplan";  


    public static final String DISPLAY_PLANVIEWCONTOUR = "planviewcontour";
    public static final String DISPLAY_PLANVIEWCONTOURFILLED = "planviewcontourfilled";
    public static final String DISPLAY_PLANVIEWCOLOR = "planviewcolor";
    public static final String DISPLAY_ISOSURFACE = "isosurface";



    //    public static void processScript(String scriptFile) throws Exception {


    /** _more_ */
    public static final OutputType OUTPUT_IDV_GRID =
        new OutputType("Grid Visualization", "idv.grid", OutputType.TYPE_HTML,                                                    OutputType.SUFFIX_NONE,
                       ICON_PLANVIEW);


    /** _more_ */
    IdvServer idvServer;

    /** _more_ */
    int callCnt = 0;

    private 	HashSet<String> okControls;


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
	String action = request.getString(ARG_ACTION, ACTION_MAKEINITFORM);


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


        DataSourceDescriptor descriptor = idvServer.getIdv().getDataManager().getDescriptor("File.Grid");
        GeoGridDataSource dataSource = new GeoGridDataSource(descriptor, dataset, entry.getName(), path);

	try {
	    if(action.equals(ACTION_MAKEINITFORM)) {
		return outputGridInitForm(request, entry, dataSource);
	    } else   if(action.equals(ACTION_MAKEFORM)) {
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
        sb.append(HtmlUtil.submit(msg("Make image"),ARG_SUBMIT));
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT,OUTPUT_IDV_GRID));
        sb.append(HtmlUtil.hidden(ARG_ACTION,ACTION_MAKEPAGE));




	StringBuffer basic = new StringBuffer();

	basic.append(HtmlUtil.formTable());
	basic.append(HtmlUtil.formEntry(msgLabel("Show map"),
					HtmlUtil.checkbox(ARG_SHOWMAP,"true",true)));

	basic.append(HtmlUtil.formEntry(msgLabel("Clip image"),
					HtmlUtil.checkbox(ARG_CLIP,"true",false)));

	basic.append(HtmlUtil.formEntry(msgLabel("White Background"),
					HtmlUtil.checkbox(ARG_WHITEBACKGROUND,"true",false)));


	basic.append(HtmlUtil.formEntry(msgLabel("Width"),
					HtmlUtil.input(ARG_IMAGE_WIDTH,"600",HtmlUtil.attr(HtmlUtil.ATTR_SIZE,"5"))));

	basic.append(HtmlUtil.formEntry(msgLabel("Height"),
					HtmlUtil.input(ARG_IMAGE_HEIGHT,"400",HtmlUtil.attr(HtmlUtil.ATTR_SIZE,"5"))));
	


	



	basic.append(HtmlUtil.formTableClose());


	List<String> tabLabels = new ArrayList<String>();
	List<String> tabContents = new ArrayList<String>();
	tabLabels.add(msg("Basic"));
	tabContents.add(basic.toString());


        List colorTables = idvServer.getIdv().getColorTableManager().getColorTables();


	Hashtable<String,DataChoice> idToChoice = new Hashtable<String,DataChoice>();
        List<DataChoice> choices = (List<DataChoice>)dataSource.getDataChoices();            
	for(DataChoice dataChoice: choices) {
	    idToChoice.put(dataChoice.getName(),dataChoice);
	}



	for(int displayIdx=1;displayIdx<=NUM_PARAMS;displayIdx++) {
	    if(!request.defined(ARG_PARAM+displayIdx)) continue;

            List<String> innerTabTitles = new ArrayList<String>();
            List<String> innerTabContents = new ArrayList<String>();

	    StringBuffer tab = new StringBuffer();
	    String param = request.getString(ARG_PARAM+displayIdx,"");
	    DataChoice choice = idToChoice.get(param);
            if(choice == null) {
                continue;
            }

	    List  descriptors =
		new ArrayList(
			      ControlDescriptor.getApplicableControlDescriptors(
										choice.getCategories(),
										idvServer.getIdv().getControlDescriptors(true)));


	    List displays = new ArrayList();
	    displays.add(new TwoFacedObject("--skip--",""));
	    for(ControlDescriptor controlDescriptor: (List<ControlDescriptor>) descriptors) {
		String controlId = controlDescriptor.getControlId();
		if(!okControls.contains(controlId)) continue;
		//		System.out.println (controlId +"  " + controlDescriptor.getLabel());
		displays.add(new TwoFacedObject(controlDescriptor.getLabel(), controlId));
	    }


	    tab.append(HtmlUtil.hidden(ARG_PARAM+displayIdx,request.getString(ARG_PARAM+displayIdx,"")));
	    List options = new ArrayList();
	    options.add("");
            tab.append(HtmlUtil.br());
            tab.append(msgLabel("Display Type"));
            tab.append(HtmlUtil.space(1));
            tab.append(HtmlUtil.select(ARG_DISPLAY+displayIdx, displays));
            tab.append(HtmlUtil.p());

	    List times = choice.getAllDateTimes();
	    if(times!=null && times.size()>0) {
		List tfoTimes =new ArrayList();
		int cnt=0;
		for(Object time: times) {
		    tfoTimes.add(new TwoFacedObject(time.toString(), new Integer(cnt++)));
		}
		innerTabTitles.add(msg("Times"));
                innerTabContents.add(HtmlUtil.select(ARG_TIMES+displayIdx, tfoTimes,"",
                                                     HtmlUtil.attrs(HtmlUtil.ATTR_MULTIPLE,"true",
                                                                    HtmlUtil.ATTR_SIZE,"5")));
	    }
	    
	    List levels = choice.getAllLevels();

	    List spatialComps = new ArrayList();
	    if(levels!=null && levels.size()>0) {
		List tfoLevels =new ArrayList();
		int cnt=0;
		for(Object level: levels) {
		    tfoLevels.add(new TwoFacedObject(level.toString(), new Integer(cnt++)));
		}
                String levelWidget = HtmlUtil.select(ARG_LEVELS+displayIdx, tfoLevels,"",
                                                     HtmlUtil.attrs(HtmlUtil.ATTR_MULTIPLE,"false",
                                                                    HtmlUtil.ATTR_SIZE,"5"));
		spatialComps.add(msgLabel("Levels"));
		spatialComps.add(levelWidget);
	    }


            ThreeDSize size =
                (ThreeDSize) choice.getProperty(GeoGridDataSource.PROP_GRIDSIZE);
	    spatialComps.add(msgLabel("X/Y Stride"));
	    String strideComp = HtmlUtil.input(ARG_STRIDE+displayIdx,"", HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"5"));
	    if(size!=null)
		strideComp = strideComp+HtmlUtil.br()+size;
	    spatialComps.add(strideComp);
	    String spatial = HtmlUtil.table(Misc.listToStringArray(spatialComps), 5);
	    innerTabTitles.add(msg("Spatial"));
	    innerTabContents.add(spatial);




            List<String> ctCats  = new ArrayList<String>();
            Hashtable<String, StringBuffer> ctCatMap  = new Hashtable<String,StringBuffer>(); 
            for(ColorTable colorTable:(List<ColorTable>) colorTables) {
                StringBuffer catSB = ctCatMap.get(colorTable.getCategory());
                if(catSB == null) {
                    catSB = new StringBuffer();
                    ctCatMap.put(colorTable.getCategory(), catSB);
                    ctCats.add(colorTable.getCategory());
                }
                String icon = IOUtil.cleanFileName(colorTable.getName())+".png";
                icon  = icon.replace(" ","_");
                String div = HtmlUtil.div(HtmlUtil.img(getRepository().getUrlBase() +"/colortables/" + icon) +" " + colorTable.getName(),"");
                String call = HtmlUtil.call("setFormValue","'" + ARG_COLORTABLE+displayIdx+"','" + colorTable.getName()+"'") +";" +
                    HtmlUtil.call("setHtml","'" + ARG_COLORTABLE+"_html"+ displayIdx+"','" + colorTable.getName()+"'");

                catSB.append(HtmlUtil.mouseClickHref(call, div));
            }


            StringBuffer ctsb = new StringBuffer();
            ctsb.append(msgLabel("Range")+HtmlUtil.space(1) + HtmlUtil.input(ARG_RANGE_MIN+displayIdx,"", HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"5")) +" - " +
                        HtmlUtil.input(ARG_RANGE_MAX+displayIdx,"", HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"5")));
	    ctsb.append(HtmlUtil.hidden(ARG_COLORTABLE+displayIdx,"",HtmlUtil.id(""+ARG_COLORTABLE+displayIdx)));
            ctsb.append(HtmlUtil.div("-default-", HtmlUtil.id(ARG_COLORTABLE+"_html"+ displayIdx)));
            String call = HtmlUtil.call("setFormValue","'" + ARG_COLORTABLE+displayIdx+"','" + ""+"'") +";" +
                HtmlUtil.call("setHtml","'" + ARG_COLORTABLE+"_html"+ displayIdx+"','" + "-default-"+"'");
            ctsb.append(HtmlUtil.mouseClickHref(call, "Use default"));
            for(String ctcat: ctCats) {
                ctsb.append(HtmlUtil.makeShowHideBlock(ctcat,
                                                      ctCatMap.get(ctcat).toString(),false));

            }

            StringBuffer scalesb  = new StringBuffer();
	    scalesb.append(msgHeader("Color Scale"));
            scalesb.append(HtmlUtil.formTable());
            scalesb.append(HtmlUtil.formEntry(msgLabel("Visible"), HtmlUtil.checkbox(ARG_SCALE_VISIBLE+displayIdx,"true",false)));
            scalesb.append(HtmlUtil.formEntry(msgLabel("Place"), HtmlUtil.select(ARG_SCALE_PLACEMENT+displayIdx,Misc.newList("top","left","bottom","right"))));
            scalesb.append(HtmlUtil.formTableClose());


	    


            StringBuffer contoursb  = new StringBuffer();
            contoursb.append(HtmlUtil.formTable());
            contoursb.append(HtmlUtil.formEntry(msgLabel("Interval"), HtmlUtil.input(ARG_CONTOUR_INTERVAL+displayIdx,"",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"3"))));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Base"), HtmlUtil.input(ARG_CONTOUR_BASE+displayIdx,"",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"3"))));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Min"), HtmlUtil.input(ARG_CONTOUR_MIN+displayIdx,"",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"3"))));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Max"), HtmlUtil.input(ARG_CONTOUR_MAX+displayIdx,"",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"3"))));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Dashed"), HtmlUtil.checkbox(ARG_CONTOUR_DASHED+displayIdx,"true",false)));
            contoursb.append(HtmlUtil.formEntry(msgLabel("Labels"), HtmlUtil.checkbox(ARG_CONTOUR_LABELS+displayIdx,"true",true)));
            contoursb.append(HtmlUtil.formTableClose());


            StringBuffer misc = new StringBuffer();
            misc.append(HtmlUtil.formTable());
            misc.append(HtmlUtil.formEntry(msgLabel("Display List Label"), HtmlUtil.input(ARG_DISPLAYLISTLABEL+displayIdx,"",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"30"))));
            misc.append(HtmlUtil.formEntry(msgLabel("Display Unit"), HtmlUtil.input(ARG_DISPLAYUNIT+displayIdx,"",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"6"))));
            misc.append(HtmlUtil.formEntry(msgLabel("Isosurface Value"), HtmlUtil.input(ARG_ISOSURFACEVALUE+displayIdx,"",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"3"))));
            misc.append(HtmlUtil.formEntry(msgLabel("Vector/Barb Size"), HtmlUtil.input(ARG_FLOW_SCALE+displayIdx,"4",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"3"))));
            misc.append(HtmlUtil.formEntry(msgLabel("Streamline Density"), HtmlUtil.input(ARG_FLOW_DENSITY+displayIdx,"1",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"3"))));
            misc.append(HtmlUtil.formEntry(msgLabel("Flow Skip"), HtmlUtil.input(ARG_FLOW_SKIP+displayIdx,"0",HtmlUtil.attrs(HtmlUtil.ATTR_SIZE,"3"))));




            misc.append(HtmlUtil.formTableClose());

            innerTabTitles.add(msg("Color Table"));
            innerTabContents.add(HtmlUtil.table(new Object[]{ctsb,scalesb},5));

            innerTabTitles.add(msg("Contours"));
            innerTabContents.add(contoursb.toString());

            innerTabTitles.add(msg("Misc"));
            innerTabContents.add(misc.toString());



            String innerTab = HtmlUtil.makeTabs(innerTabTitles, innerTabContents,true,"tab_content");
            tab.append(HtmlUtil.inset(HtmlUtil.p()+innerTab,10));

	    tabLabels.add(StringUtil.camelCase(choice.getDescription()));
	    tabContents.add(tab.toString());
	}
        sb.append(HtmlUtil.makeTabs(tabLabels, tabContents,true,"tab_content"));

        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.submit(msg("Make image"),ARG_SUBMIT));
        sb.append(HtmlUtil.formClose());
        return new Result("Grid Preview", sb);
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
    private Result outputGridInitForm(final Request request, Entry entry, GeoGridDataSource dataSource)
            throws Exception {
        StringBuffer sb   = new StringBuffer();

        String       formUrl  = getRepository().URL_ENTRY_SHOW.getFullUrl();
        sb.append(HtmlUtil.form(formUrl, ""));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtil.hidden(ARG_OUTPUT,OUTPUT_IDV_GRID));
        sb.append(HtmlUtil.hidden(ARG_ACTION,ACTION_MAKEFORM));


        List<DataChoice> choices = (List<DataChoice>)dataSource.getDataChoices();            


	sb.append(msgHeader("Select one or more fields to view"));

	StringBuffer fields  = new StringBuffer();
	for(int i=1;i<=NUM_PARAMS;i++) {
	    List options = new ArrayList();
	    options.add(new TwoFacedObject("--Pick one--",""));
            List<String> cats = new ArrayList<String>();
            Hashtable<String,List<TwoFacedObject>> catMap = new  Hashtable<String,List<TwoFacedObject>>();
	    for(DataChoice dataChoice: choices) {
                String label =     StringUtil.camelCase(dataChoice.getDescription());
                DataCategory cat = dataChoice.getDisplayCategory();
                String catName;
                if(cat!=null)
                    catName = cat.toString();
                else
                    catName="Data";
                List<TwoFacedObject> tfos = catMap.get(catName);
                if(tfos==null) {
                    tfos = new ArrayList<TwoFacedObject>();
                    catMap.put(catName, tfos);
                    cats.add(catName);
                }
		tfos.add(new TwoFacedObject("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + label, dataChoice.getName()));
	    }

            for(String cat: cats) {
                options.add(new TwoFacedObject(cat.replace("-","&gt;"),""));
                options.addAll(catMap.get(cat));
            }

	    fields.append(HtmlUtil.select(ARG_PARAM+i, options));
	    fields.append(HtmlUtil.p());
	}
	sb.append(HtmlUtil.insetLeft(fields.toString(), 10));
        sb.append(HtmlUtil.submit(msg("Make image"),ARG_SUBMIT));
        sb.append(HtmlUtil.formClose());
        return new Result("Grid Preview", sb);
    }




    private Result outputGridPage(final Request request, Entry entry, GeoGridDataSource dataSource)
	throws Exception {
	StringBuffer sb = new StringBuffer();
	String url = getRepository().URL_ENTRY_SHOW.getFullUrl();
	String islUrl = url +"/" + IOUtil.stripExtension(entry.getName())+".isl";
	String jnlpUrl = url +"/" + IOUtil.stripExtension(entry.getName())+".jnlp";
	
	Hashtable exceptArgs = new Hashtable();
	exceptArgs.put(ARG_ACTION, ARG_ACTION);
	String args = request.getUrlArgs(exceptArgs, null);
	url = url + "?" + ARG_ACTION +"=" + ACTION_MAKEIMAGE+"&" + args;


	islUrl = islUrl + "?" + ARG_ACTION +"=" + ACTION_MAKEIMAGE+"&" + args +"&" + ARG_TARGET +"=" + TARGET_ISL;
	jnlpUrl = jnlpUrl + "?" + ARG_ACTION +"=" + ACTION_MAKEIMAGE+"&" + args +"&" + ARG_TARGET +"=" + TARGET_JNLP;

	sb.append(HtmlUtil.img(url));
	sb.append(HtmlUtil.br());
	sb.append(HtmlUtil.href(jnlpUrl,msg("Launch in the IDV")));
	sb.append(HtmlUtil.br());
	sb.append(HtmlUtil.href(islUrl,msg("Download IDV ISL script")));
	return new Result("Grid Preview", sb);
    }


    public Result outputGridImage(final Request request, Entry entry, GeoGridDataSource dataSource)
	throws Exception {
        DataOutputHandler dataOutputHandler = getDataOutputHandler();

	Trace.addNot(".*ShadowFunction.*");
	Trace.addNot(".*GeoGrid.*");
	//	Trace.addOnly(".*MapProjection.*");
	//	Trace.addOnly(".*ProjectionCoordinateSystem.*");
        //	Trace.startTrace();
        String id = entry.getId();
        File image = getStorageManager().getThumbFile("preview_"
                         + id.replace("/", "_") + ".gif");

	boolean forIsl = request.getString(ARG_TARGET,"").equals(TARGET_ISL);
	boolean forJnlp = request.getString(ARG_TARGET,"").equals(TARGET_JNLP);
	if(forJnlp) forIsl = true;

        StringBuffer isl = new StringBuffer();
        isl.append("<isl debug=\"true\" loop=\"1\" offscreen=\"true\">\n");

        StringBuffer viewProps = new StringBuffer();
        viewProps.append(XmlUtil.tag("property", XmlUtil.attrs("name","wireframe","value","false")));
        viewProps.append(XmlUtil.tag("property", XmlUtil.attrs("name","showMaps","value",""+request.get(ARG_SHOWMAP, false))));
        if(request.get(ARG_WHITEBACKGROUND,false)) {
            viewProps.append(XmlUtil.tag("property", XmlUtil.attrs("name","background","value","white")));
            viewProps.append(XmlUtil.tag("property", XmlUtil.attrs("name","foreground","value","black")));
        }

	//Create a new viewmanager
	//For now don't do this if we are doing jnlp
	if(!forJnlp) {
	isl.append(XmlUtil.tag(ImageGenerator.TAG_VIEW,
			       XmlUtil.attrs(ImageGenerator.ATTR_WIDTH,
					     request.getString(ARG_IMAGE_WIDTH,"400"),
					     ImageGenerator.ATTR_HEIGHT,
					     request.getString(ARG_IMAGE_HEIGHT,"300")),
                               viewProps.toString()));
	}



	if(forIsl) {
	    isl.append(XmlUtil.openTag("datasource",XmlUtil.attrs(
								  "id","datasource",
								  "url",
								  getRepository().absoluteUrl(
											      getRepository().URL_ENTRY_SHOW
											      + dataOutputHandler.getOpendapUrl(entry)),
								  )));
	} else {
	    isl.append("<datasource id=\"datasource\" times=\"0\" >\n");
	}

	Hashtable props = new Hashtable();
	props.put("datasource", dataSource);
	StringBuffer firstDisplays = new StringBuffer();
	StringBuffer secondDisplays = new StringBuffer();
	boolean multipleTimes = false;
	
	for(int displayIdx=1;displayIdx<=NUM_PARAMS;displayIdx++) {
	    if(!request.defined(ARG_PARAM+displayIdx) ||
	       !request.defined(ARG_DISPLAY+displayIdx)) continue;

	    String display = request.getString(ARG_DISPLAY+displayIdx,"");
	    
            StringBuffer propSB = new StringBuffer();
            propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                      XmlUtil.attrs("name","id",
                                                    "value","thedisplay"+displayIdx)));





	    if(request.get(ARG_SCALE_VISIBLE+displayIdx,false)) {
	    /*
    visible=true|false;
    color=somecolor;
    orientation=horizontal|vertical;
    placement=top|left|bottom|right
	    */
		String placement = request.getString(ARG_SCALE_PLACEMENT+displayIdx,"");
		String orientation;
		if(placement.equals("top") || placement.equals("bottom")) 
		    orientation = "horizontal";
		else
		    orientation = "vertical";
		String s  = "visible=true;orientation=" + orientation+";placement=" + placement;
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                          XmlUtil.attrs("name","colorScaleInfo",
                                                        "value",s.toString())));




	    }


            if(display.equals(DISPLAY_PLANVIEWCONTOUR)) {
                StringBuffer  s = new StringBuffer();
                if(request.defined(ARG_CONTOUR_INTERVAL+displayIdx))
                    s.append("interval=" +request.getString(ARG_CONTOUR_INTERVAL+displayIdx,"") +";");
                if(request.defined(ARG_CONTOUR_BASE+displayIdx))
                    s.append("base=" +request.getString(ARG_CONTOUR_BASE+displayIdx,"") +";");
                if(request.defined(ARG_CONTOUR_MIN+displayIdx))
                    s.append("min=" +request.getString(ARG_CONTOUR_MIN+displayIdx,"") +";");
                if(request.defined(ARG_CONTOUR_MAX+displayIdx))
                    s.append("max=" +request.getString(ARG_CONTOUR_MAX+displayIdx,"") +";");

                s.append("dashed=" +request.get(ARG_CONTOUR_DASHED+displayIdx,false) +";");
                s.append("labels=" +request.get(ARG_CONTOUR_LABELS+displayIdx,false) +";");
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                          XmlUtil.attrs("name","contourInfoParams",
                                                        "value",s.toString())));
            }



            if(request.defined(ARG_RANGE_MIN+displayIdx) && request.defined(ARG_RANGE_MAX+displayIdx)) {
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                          XmlUtil.attrs("name","range",
                                                        "value",request.getString(ARG_RANGE_MIN+displayIdx,"").trim()+":" +
                                                        request.getString(ARG_RANGE_MAX+displayIdx,"").trim())));

                

            }


            if(request.defined(ARG_COLORTABLE+displayIdx)) {
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                          XmlUtil.attrs("name","colorTableName",
                                                        "value",request.getString(ARG_COLORTABLE+displayIdx,""))));;

            }

            




	    StringBuffer attrs = new StringBuffer();


	    if(display.equals(DISPLAY_PLANVIEWFLOW) ||
	       display.equals(DISPLAY_STREAMLINES) ||
	       display.equals(DISPLAY_WINDBARBPLAN)) {
                    propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                              XmlUtil.attrs("name","flowScale",
                                                            "value",request.getString(ARG_FLOW_SCALE+displayIdx,""))));
                    propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                              XmlUtil.attrs("name","streamlineDensity",
                                                            "value",request.getString(ARG_FLOW_DENSITY+displayIdx,""))));

                    propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                              XmlUtil.attrs("name","skipValue",
                                                            "value",request.getString(ARG_FLOW_SKIP+displayIdx,""))));

	    }





            if(display.equals(DISPLAY_ISOSURFACE)) {
                if(request.defined(ARG_ISOSURFACEVALUE+displayIdx)) {
                    propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                              XmlUtil.attrs("name","surfaceValue",
                                                            "value",request.getString(ARG_ISOSURFACEVALUE+displayIdx,""))));
                }
            } else {
                String level = request.getString(ARG_LEVELS+displayIdx,"0");
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_LEVEL_FROM,"#" + level,
                                           ImageGenerator.ATTR_LEVEL_TO,"#" + level));

            }


            if(request.defined(ARG_DISPLAYLISTLABEL+displayIdx)) {
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                          XmlUtil.attrs("name","displayListTemplate",
                                                        "value",request.getString(ARG_DISPLAYLISTLABEL+displayIdx,""))));
            }



            if(request.defined(ARG_DISPLAYUNIT+displayIdx)) {
                propSB.append(XmlUtil.tag(ImageGenerator.TAG_PROPERTY, 
                                          XmlUtil.attrs("name","settingsDisplayUnit",
                                                        "value",request.getString(ARG_DISPLAYUNIT+displayIdx,""))));
            }

	    System.err.println("Props:" + propSB);
            attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TYPE, display,
                                       ImageGenerator.ATTR_PARAM,request.getString(ARG_PARAM+displayIdx,"")));

            if(request.defined(ARG_STRIDE+displayIdx)) {
                attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_STRIDE, request.getString(ARG_STRIDE+displayIdx,"1")));
            }


	    List times = request.get(ARG_TIMES+displayIdx, new ArrayList());
	    if(times.size()>0) {
                if(times.size()>1)  multipleTimes = true;
		attrs.append(XmlUtil.attrs(ImageGenerator.ATTR_TIMES, StringUtil.join(",",times)));
	    }
	    
	    StringBuffer which = (display.equals(DISPLAY_PLANVIEWCONTOUR)?secondDisplays:firstDisplays);

	    which.append(XmlUtil.tag(ImageGenerator.TAG_DISPLAY, attrs.toString(),propSB.toString()));
	}

	isl.append(firstDisplays);
	isl.append(secondDisplays);
	
        isl.append("</datasource>\n");
	//        isl.append("<pause/>\n");
	String clip = "";
	if(request.get(ARG_CLIP, false)) {
	    clip = XmlUtil.tag("clip","");
	}
	    
        isl.append(XmlUtil.tag((multipleTimes?"movie":"image"), XmlUtil.attr("file",  image.toString()), clip));
        isl.append("</isl>\n");
        //        System.out.println(isl);


	if(forJnlp) {
	    String jnlp = getRepository().getResource(
						      "/ucar/unidata/repository/idv/template.jnlp");
	    StringBuffer args = new StringBuffer();
            args.append("<argument>-b64isl</argument>");
            args.append("<argument>" + XmlUtil.encodeBase64(isl.toString().getBytes()) + "</argument>");
	    jnlp = jnlp.replace("${args}", args.toString());
	    return new Result("data.jnlp",
			      new StringBuffer(jnlp),
			      "application/x-java-jnlp-file");
	}
	if(forIsl) {
	    return new Result("data.isl",
			      new StringBuffer(isl),
			      "text/xml");
	}

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


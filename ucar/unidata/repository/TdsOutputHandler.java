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

import ucar.nc2.dt.grid.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import ucar.unidata.geoloc.*;


import ucar.unidata.repository.*;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

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

    public static final String ARG_ADDLATLON = "addlatlon";

    public static final String ARG_SUBSETAREA = "subsetarea";
    public static final String ARG_SUBSETTIME = "subsettime";

    public static final String ARG_HSTRIDE = "hstride";

    /** _more_ */
    public static final String OUTPUT_OPENDAP = "tds.opendap";

    public static final String OUTPUT_CDL = "tds.cdl";

    public static final String OUTPUT_WCS = "tds.wcs";



    public static final String OUTPUT_GRIDSUBSET_FORM = "tds.gridsubset.form";
    public static final String OUTPUT_GRIDSUBSET = "tds.gridsubset";


    /** _more_ */
    private Hashtable<String, Boolean> checkedEntries = new Hashtable<String,
                                                            Boolean>();

    private Hashtable<String, Boolean> gridEntries = new Hashtable<String,
                                                            Boolean>();


    /** _more_ */
    private Object CACHE_MUTEX = new Object();

    /** _more_ */
    private Hashtable<String, NetcdfFile> cache = new Hashtable<String,
                                                      NetcdfFile>();

    /** _more_ */
    private List<String> cachedFiles = new ArrayList<String>();



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

        //TODO: what other global configuration should be done?
        String nj22TmpFile =
            IOUtil.joinDir(getRepository().getStorageManager().getTmpDir(),
                           "nj22/");
        IOUtil.makeDir(nj22TmpFile);

        //Set the temp file and the cache policy
        ucar.nc2.util.DiskCache.setRootDirectory(nj22TmpFile);


        ucar.nc2.iosp.grib.GribServiceProvider.setIndexAlwaysInCache(true);

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
        return output.equals(OUTPUT_OPENDAP) || 
            output.equals(OUTPUT_CDL)    || 
            output.equals(OUTPUT_WCS) ||
            output.equals(OUTPUT_GRIDSUBSET)||
            output.equals(OUTPUT_GRIDSUBSET_FORM);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param state _more_
     * @param types _more_
     *
     * @throws Exception On badness
     */
    protected void addOutputTypes(Request request, State state,
                                  List<OutputType> types)
            throws Exception {
        if (state.entry == null) {
            return;
        }
        if (canLoad(request, state.entry)) {
            //            types.add(new OutputType("CDL", OUTPUT_CDL));
            types.add(new OutputType("OpenDAP", OUTPUT_OPENDAP) {
                public String assembleUrl(Request request) {
                    return request.getRequestPath() + getSuffix() + "/"
                           + request.getPathEmbeddedArgs() + "/entry.das";
                }
            });
        }
    }

    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links, boolean forHeader)
            throws Exception {
        if (!canLoad(request, entry)) {
            return;
        }
        String tdsUrl = request.getRequestPath() +  "/"
            + request.getPathEmbeddedArgs() + "/entry.das";
        
        links.add(new Link(tdsUrl, getRepository().fileUrl(ICON_DATA),"OpenDAP"));

        links.add(new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                            ARG_OUTPUT, OUTPUT_CDL), getRepository().fileUrl(ICON_DATA),"CDL"));

        if(canLoadAsGrid(entry)) {
            links.add(new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                                ARG_OUTPUT, OUTPUT_GRIDSUBSET_FORM), getRepository().fileUrl(ICON_DATA),"Subset"));

            links.add(new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW, entry,
                                                ARG_OUTPUT, OUTPUT_WCS), getRepository().fileUrl(ICON_DATA),"WCS"));
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
        return "/" + ARG_OUTPUT + ":" + Request.encodeEmbedded(OUTPUT_OPENDAP)
               + "/" + ARG_ID + ":" + Request.encodeEmbedded(entry.getId())
               + "/entry.das";
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getFullTdsUrl(Entry entry) {
        return getRepository().URL_ENTRY_SHOW.getFullUrl() + "/" + ARG_OUTPUT
               + ":" + Request.encodeEmbedded(OUTPUT_OPENDAP) + "/" + ARG_ID
               + ":" + Request.encodeEmbedded(entry.getId()) + "/entry.das";
    }


    /**
     * Can the given entry be served by the tds
     *
     *
     * @param request _more_
     * @param entry The entry
     *
     * @return Can the given entry be served by the tds
     */
    public boolean canLoad(Request request, Entry entry) {
        //If we aren't in the tomcat world then exit
        if (request!=null && request.getHttpServletRequest() == null) {
            //return false;
        }

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
                    //TODO: What is the performance hit here? Is this the best way to find out if we can serve this file
                    //Use openFile
                    NetcdfDataset dataset = NetcdfDataset.acquireDataset(file.toString(), null);
                    System.err.println ("nc:" + dataset.getClass().getName());
                    ok = true;
                } catch (Exception ignoreThis) {}
            }
            b = new Boolean(ok);
            checkedEntries.put(entry.getId(), b);
        }
        return b.booleanValue();
    }


    public boolean canLoadAsGrid(Entry entry) {
        Boolean b = gridEntries.get(entry.getId());
        if (b == null) {
            boolean ok = false;
            if (entry.isGroup()) {
                ok = false;
            } else if ( !entry.getResource().isFile()) {
                ok = false;
            } else {
                try {
                    File file = entry.getResource().getFile();
                    //TODO: What is the performance hit here? Is this the best way to find out if we can serve this file
                    //Use openFile
                    GridDataset gds = GridDataset.open(file.toString());
                    ok = true;
                } catch (Exception ignoreThis) {}
            }
            b = new Boolean(ok);
            gridEntries.put(entry.getId(), b);
        }
        return b.booleanValue();
    }


    public Result outputCdl(final Request request, Entry entry)
            throws Exception {


        StringBuffer sb = new StringBuffer();
        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                              false, "");
        sb.append(crumbs[1]);
        if(request.get(ARG_ADDMETADATA, false)) {
            if(getRepository().getAccessManager().canDoAction(request, entry,Permission.ACTION_EDIT)) {
                sb.append(HtmlUtil.p());

                List<Entry> entries = (List<Entry>)Misc.newList(entry);
                getRepository().addInitialMetadata(request, entries);
                getRepository().insertEntries(entries, false);
                sb.append(getRepository().note("Metadata added"));
                return makeLinksResult(request, "CDL",
                                       sb, new State(entry));
            }
            sb.append("You cannot add metadata");
            return makeLinksResult(request, "CDL",
                                   sb, new State(entry));
        }




        File file = entry.getResource().getFile();
        NetcdfDataset dataset = NetcdfDataset.acquireDataset(file.toString(), null);
        if(getRepository().getAccessManager().canDoAction(request, entry,Permission.ACTION_EDIT)) {
            request.put(ARG_ADDMETADATA,"true");
            sb.append(HtmlUtil.href(request.getUrl(), "Add metadata"));
        }
        if(dataset==null) {
            sb.append("Could not open dataset");
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ucar.nc2.NCdump.print(dataset, "", bos, null);
            sb.append("<pre>" +bos.toString()+"</pre>");
        }
        
        return makeLinksResult(request, "CDL",
                               sb, new State(entry));
    }

    public Result outputWcs(Request request, Entry entry) {
        return new Result("",new StringBuffer("TBD"));
    }

    public Result outputGridSubset(Request request, Entry entry) throws Exception {
        File file = entry.getResource().getFile();
        StringBuffer sb = new StringBuffer();
        String prefix =  ARG_VARIABLE+".";
        String output = request.getOutput();
        if(output.equals(OUTPUT_GRIDSUBSET)) {
            List varNames = new ArrayList();
            Hashtable args = request.getArgs();
            for (Enumeration keys =
                     args.keys(); keys.hasMoreElements(); ) {
                String arg = (String) keys.nextElement();
                if(arg.startsWith(prefix) && request.get(arg,false)) {
                    varNames.add(arg.substring(prefix.length()));
                }
            }
            System.err.println(varNames);
            LatLonRect   llr           = null;
            if(request.get(ARG_SUBSETAREA,false)) {
                llr = new LatLonRect(new LatLonPointImpl(request.get(ARG_AREA+"_north", 90),
                                                         request.get(ARG_AREA+"_west", -180)),
                                     new LatLonPointImpl(request.get(ARG_AREA+"_sourh", 0),
                                                         request.get(ARG_AREA+"_east", 180)));
            }
            int hStride = request.get(ARG_HSTRIDE,1);
            int zStride = 1;
            boolean includeLatLon=request.get(ARG_ADDLATLON,false);
            int timeStride = 1;
            Date[] dates = new Date[]{request.get(ARG_SUBSETTIME,false)?request.getDate(ARG_FROMDATE, null):null,
                                      request.get(ARG_SUBSETTIME,false)?request.getDate(ARG_TODATE, null):null};
            if(varNames.size()==0) {
                sb.append(getRepository().warning("No variables selected"));
            } else {
                NetcdfCFWriter writer = new NetcdfCFWriter();
                File f = getRepository().getStorageManager().getTmpFile(request, "subset.nc");
                GridDataset gds = GridDataset.open(file.toString());
                writer.makeFile(f.toString(), gds, varNames, llr, (dates[0]==null?null:new thredds.datatype.DateRange(dates[0],dates[1])),
                                includeLatLon, hStride, zStride, timeStride);
                return new Result("subset.nc", new FileInputStream(f),"application/x-netcdf");
            }
        }

        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                              false, "");
        NetcdfDataset dataset =
            NetcdfDataset.acquireDataset(file.toString(), null);
        sb.append(crumbs[1]);
        List<Variable>  variables  = dataset.getVariables();
        String formUrl = request.url(getRepository().URL_ENTRY_SHOW);
        sb.append(HtmlUtil.form(formUrl+"/subset.nc"));
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.submit("Subset Grid", ARG_SUBMIT));
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_GRIDSUBSET));
        sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
        sb.append(HtmlUtil.formTable());

        sb.append(HtmlUtil.formEntry(msgLabel("Horizontal Stride"),
                                     HtmlUtil.input(ARG_HSTRIDE,request.getString(ARG_HSTRIDE,"1"),HtmlUtil.SIZE_5)));


        Date[]dates = null;


        StringBuffer varSB = new StringBuffer();
        for (Variable var : variables) {
            if (var instanceof CoordinateAxis) {
                CoordinateAxis              ca = (CoordinateAxis) var;
                AxisType axisType = ca.getAxisType();
                if(axisType.equals(AxisType.Time)) {
                    dates = ThreddsMetadataHandler.getMinMaxDates(var,ca);
                }
                continue;
            }

            varSB.append(HtmlUtil.checkbox(ARG_VARIABLE+"." + var.getShortName(),"true",false));
            varSB.append(HtmlUtil.space(1));
            varSB.append(var.getName());
            varSB.append(HtmlUtil.space(1));
            if(var.getUnitsString()!=null) {
                varSB.append("(" + var.getUnitsString() +")");
            }

            varSB.append(HtmlUtil.br());
        }

        if(dates!=null) {
            sb.append(HtmlUtil.formEntry(HtmlUtil.checkbox(ARG_SUBSETTIME, "true", request.get(ARG_SUBSETTIME, false)) +
                                         msgLabel("Subset Times"),
                                         HtmlUtil.input(ARG_FROMDATE,getRepository().formatDate(request,dates[0])) +
                                         HtmlUtil.img(getRepository().fileUrl(ICON_ARROW)) +
                                         HtmlUtil.input(ARG_TODATE,getRepository().formatDate(request,dates[1]))));
        }


        for (CoordinateSystem coordSys : (List<CoordinateSystem>)dataset
                 .getCoordinateSystems()) {
            ProjectionImpl proj = coordSys.getProjection();
            if (proj == null) {
                continue;
            }
            LatLonRect llr = proj.getDefaultMapAreaLL();
            sb.append(HtmlUtil.formEntryTop(HtmlUtil.checkbox(ARG_SUBSETAREA, "true", request.get(ARG_SUBSETAREA, false))+
                                  msgLabel("Subset Spatially"),
                                            HtmlUtil.makeLatLonBox(ARG_AREA, llr.getLatMin(), llr.getLatMax(),llr.getLonMax(), llr.getLonMin())));
            break;
        }


        sb.append(HtmlUtil.formEntry(HtmlUtil.checkbox(ARG_ADDLATLON,"true",request.get(ARG_ADDLATLON,false)) +
                                     "Add Lat/Lon Variables",""));

        sb.append("</table>");

        sb.append("<hr>");
        sb.append("Select Variables:<ul>");
        sb.append(varSB);
        sb.append("</ul>");
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.submit("Subset Grid"));
        sb.append(HtmlUtil.formClose());
        return new Result("Grid Subset",sb);
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

        String output = request.getOutput();
        if(output.equals(OUTPUT_CDL)) {
            return outputCdl(request, entry);
        }
        if(output.equals(OUTPUT_WCS)) {
            return outputWcs(request, entry);
        }


        if(output.equals(OUTPUT_GRIDSUBSET) || output.equals(OUTPUT_GRIDSUBSET_FORM)) {
            return outputGridSubset(request, entry);
        }


        //        System.err.println ("entry:" + entry);

        //TODO: we create a new servlet every time we service a request.
        //any problems with that?

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

        /** _more_ */
        public static final int CACHE_LIMIT = 100;

        /** _more_ */
        Request repositoryRequest;

        /** _more_ */
        Entry entry;



        /**
         * _more_
         *
         * @param request _more_
         * @param entry _more_
         */
        public NcDODSServlet(Request request, Entry entry) {
            this.repositoryRequest = request;
            this.entry             = entry;
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
            NetcdfFile         ncFile  = null;
            //TODO: Should we be caching the ncFiles? The GuardedDatasets?
            //TODO: Is there problems having multiple GuardedDatasets accessing the same ncfile?
            synchronized (CACHE_MUTEX) {
                String cacheKey = repositoryRequest.getSessionId() + "_"
                                  + location;
                ncFile = cache.get(cacheKey);
                if (ncFile != null) {
                    //Bump it to the end of the list
                    cachedFiles.remove(location);
                    cachedFiles.add(location);
                } else {
                    ncFile = NetcdfDataset.acquireFile(location, null);
                    while (cachedFiles.size() > CACHE_LIMIT) {
                        String firstFile = cachedFiles.get(0);
                        String firstFileCacheKey =
                            repositoryRequest.getSessionId() + "_"
                            + firstFile;
                        cachedFiles.remove(0);
                        cache.get(firstFileCacheKey).close();
                        cache.remove(firstFileCacheKey);
                    }
                    cachedFiles.add(location);
                    cache.put(cacheKey, ncFile);
                }
            }

            GuardedDatasetImpl guardedDataset =
                new GuardedDatasetImpl(reqPath, ncFile, true);
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


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

import ucar.nc2.Attribute;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;



import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.TypedDatasetFactory;

import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.NetcdfCFWriter;

import ucar.unidata.data.gis.KmlUtil;
import org.w3c.dom.Element;
import ucar.unidata.xml.XmlUtil;

import ucar.unidata.geoloc.*;


import ucar.unidata.repository.*;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

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
import java.util.Iterator;
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
public class DataOutputHandler extends OutputHandler {

    /** _more_          */
    public static final String ARG_ADDLATLON = "addlatlon";

    /** _more_          */
    public static final String ARG_ADDTOREPOSITORY = "addtorepository";

    /** _more_          */
    public static final String ARG_SUBSETAREA = "subsetarea";

    /** _more_          */
    public static final String ARG_SUBSETTIME = "subsettime";

    /** _more_          */
    public static final String ARG_HSTRIDE = "hstride";

    /** _more_ */
    public static final String OUTPUT_OPENDAP = "data.opendap";

    /** _more_          */
    public static final String OUTPUT_CDL = "data.cdl";

    /** _more_          */
    public static final String OUTPUT_WCS = "data.wcs";

    /** _more_          */
    public static final String OUTPUT_POINT_MAP = "data.point.map";

    public static final String OUTPUT_POINT_CSV = "data.point.csv";

    public static final String OUTPUT_POINT_KML = "data.point.kml";



    /** _more_          */
    public static final String OUTPUT_GRIDSUBSET_FORM = "data.gridsubset.form";

    /** _more_          */
    public static final String OUTPUT_GRIDSUBSET = "data.gridsubset";


    /** _more_ */
    private Hashtable<String, Boolean> checkedEntries = new Hashtable<String,
                                                                      Boolean>();

    /** _more_          */
    private Hashtable<String, Boolean> gridEntries = new Hashtable<String,
                                                                   Boolean>();

    private Hashtable<String, Boolean> pointEntries = new Hashtable<String,
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
    public DataOutputHandler(Repository repository, Element element)
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
        return output.equals(OUTPUT_OPENDAP) || output.equals(OUTPUT_CDL)
            || output.equals(OUTPUT_WCS)
            || output.equals(OUTPUT_POINT_MAP)
            || output.equals(OUTPUT_POINT_CSV)
            || output.equals(OUTPUT_POINT_KML)
            || output.equals(OUTPUT_GRIDSUBSET)
            || output.equals(OUTPUT_GRIDSUBSET_FORM);
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
        if (canLoad(state.entry) &&         (request.getHttpServletRequest() == null)) {
            types.add(new OutputType("OpenDAP", OUTPUT_OPENDAP) {
                    public String assembleUrl(Request request) {
                        return request.getRequestPath() + getSuffix() + "/"
                            + request.getPathEmbeddedArgs() + "output:" + OUTPUT_OPENDAP+"/entry.das";
                    }
                });
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     * @param forHeader _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, Entry entry,
                                 List<Link> links, boolean forHeader)
        throws Exception {
        if ( !canLoad(entry)) {
            return;
        }
        String tdsUrl = request.getRequestPath() + "/"
            + request.getPathEmbeddedArgs() + "/entry.das";

        if (canLoadAsPoint(entry)) {
            links.add(
                      new Link(
                               request.entryUrl(
                                                getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                                                OUTPUT_POINT_MAP), getRepository().fileUrl(
                                                                                           ICON_MAP), "Map Point Data"));
            links.add(
                      new Link(
                               HtmlUtil.url(
                                            request.getRequestPath()+"/" +IOUtil.stripExtension(entry.getName())+".csv", Misc.newList(
                                                                                                                                      ARG_ID, entry.getId(),
                                                                                                                                      ARG_OUTPUT,
                                                                                                                                      OUTPUT_POINT_CSV)), getRepository().fileUrl(
                                                                                                                                                                                  ICON_CSV), "Point Data as CSV"));

            links.add(
                      new Link(
                               HtmlUtil.url(
                                            request.getRequestPath()+"/" +IOUtil.stripExtension(entry.getName())+".kml", Misc.newList(
                                                                                                                                      ARG_ID, entry.getId(),
                                                                                                                                      ARG_OUTPUT,
                                                                                                                                      OUTPUT_POINT_KML)), getRepository().fileUrl(
                                                                                                                                                                                  ICON_KML), "Point Data as KML"));



        } else if (canLoadAsGrid(entry)) {
            links.add(
                      new Link(
                               request.entryUrl(
                                                getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                                                OUTPUT_GRIDSUBSET_FORM), getRepository().fileUrl(
                                                                                                 ICON_SUBSET), "Subset"));

            /*
              links.add(
              new Link(
              request.entryUrl(
              getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
              OUTPUT_WCS), getRepository().fileUrl(ICON_DATA),
              "WCS"));
            */
        }

        links.add(new Link(tdsUrl, getRepository().fileUrl(ICON_OPENDAP),
                           "OpenDAP"));

        links.add(
                  new Link(
                           request.entryUrl(
                                            getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,
                                            OUTPUT_CDL), getRepository().fileUrl(ICON_DATA), "CDL"));



    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getTdsUrl(Entry entry) {
        return "/" + ARG_OUTPUT + ":"
            + Request.encodeEmbedded(OUTPUT_OPENDAP) + "/" + ARG_ID + ":"
            + Request.encodeEmbedded(entry.getId()) + "/entry.das";
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
                    //TODO: What is the performance hit here? Is this the best way to find out if we can serve this file
                    //Use openFile
                    NetcdfDataset dataset =
                        NetcdfDataset.openDataset(file.toString());
                    ok = true;
                } catch (Exception ignoreThis) {}
            }
            b = new Boolean(ok);
            checkedEntries.put(entry.getId(), b);
        }
        return b.booleanValue();
    }


    public boolean canLoadAsPoint(Entry entry) {
        Boolean b = pointEntries.get(entry.getId());
        if (b == null) {
            boolean ok = false;
            if (entry.isGroup()) {
                ok = false;
            } else if ( !entry.getResource().isFile()) {
                ok = false;
            } else {
                try {
                    StringBuilder    buf     = new StringBuilder();
                    File file = entry.getResource().getFile();
                    if(getPointDataset(file)!=null) {
                        ok = true;
                    }
                } catch (Exception ignoreThis) {}
            }
            pointEntries.put(entry.getId(), b = new Boolean(ok));
        }
        return b.booleanValue();
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean canLoadAsGrid(Entry entry) {
        if(!canLoad(entry)) return false;
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
    public Result outputCdl(final Request request, Entry entry)
        throws Exception {


        StringBuffer sb = new StringBuffer();
        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                                                         false, "");
        sb.append(crumbs[1]);
        if (request.get(ARG_ADDMETADATA, false)) {
            if (getRepository().getAccessManager().canDoAction(request,
                                                               entry, Permission.ACTION_EDIT)) {
                sb.append(HtmlUtil.p());
                List<Entry> entries = (List<Entry>) Misc.newList(entry);
                getRepository().addInitialMetadata(request, entries);
                getRepository().insertEntries(entries, false);
                sb.append(getRepository().note("Metadata added"));
                return makeLinksResult(request, "CDL", sb, new State(entry));
            }
            sb.append("You cannot add metadata");
            return makeLinksResult(request, "CDL", sb, new State(entry));
        }




        File file = entry.getResource().getFile();
        NetcdfDataset dataset = NetcdfDataset.openDataset(file.toString());
        if (getRepository().getAccessManager().canDoAction(request, entry,
                                                           Permission.ACTION_EDIT)) {
            request.put(ARG_ADDMETADATA, "true");
            sb.append(HtmlUtil.href(request.getUrl(), "Add metadata"));
        }
        if (dataset == null) {
            sb.append("Could not open dataset");
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ucar.nc2.NCdump.print(dataset, "", bos, null);
            sb.append("<pre>" + bos.toString() + "</pre>");
        }

        return makeLinksResult(request, "CDL", sb, new State(entry));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public Result outputWcs(Request request, Entry entry) {
        return new Result("", new StringBuffer("TBD"));
    }

    public PointObsDataset getPointDataset(File file) throws Exception {
        return (PointObsDataset)TypedDatasetFactory.open(
                                                         ucar.nc2.constants.FeatureType.POINT, file.toString(), null, new StringBuilder());
    }

    public GridDataset getGridDataset(File file) throws Exception {
        return  GridDataset.open(file.toString());
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
    public Result outputGridSubset(Request request, Entry entry)
        throws Exception {

        boolean canAdd =
            (entry.getParentGroup() != null)
            && getRepository().getAccessManager().canDoAction(request,
                                                              entry.getParentGroup(), Permission.ACTION_NEW);

        File         file   = entry.getResource().getFile();
        StringBuffer sb     = new StringBuffer();
        String       prefix = ARG_VARIABLE + ".";
        String       output = request.getOutput();
        if (output.equals(OUTPUT_GRIDSUBSET)) {
            List      varNames = new ArrayList();
            Hashtable args     = request.getArgs();
            for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
                String arg = (String) keys.nextElement();
                if (arg.startsWith(prefix) && request.get(arg, false)) {
                    varNames.add(arg.substring(prefix.length()));
                }
            }
            //            System.err.println(varNames);
            LatLonRect llr = null;
            if (request.get(ARG_SUBSETAREA, false)) {
                llr = new LatLonRect(
                                     new LatLonPointImpl(
                                                         request.get(ARG_AREA_NORTH, 90.0), request.get(
                                                                                                        ARG_AREA_WEST, -180.0)), new LatLonPointImpl(
                                                                                                                                                     request.get(ARG_AREA_SOUTH, 0.0), request.get(
                                                                                                                                                                                                   ARG_AREA_EAST, 180.0)));
                //                System.err.println("llr:" + llr);
            }
            int     hStride       = request.get(ARG_HSTRIDE, 1);
            int     zStride       = 1;
            boolean includeLatLon = request.get(ARG_ADDLATLON, false);
            int     timeStride    = 1;
            Date[]  dates = new Date[] { request.get(ARG_SUBSETTIME, false)
                                         ? request.getDate(ARG_FROMDATE, null)
                                         : null, request.get(ARG_SUBSETTIME,
                                                             false)
                                         ? request.getDate(ARG_TODATE, null)
                                         : null };
            if ((dates[0] != null) && (dates[1] != null)
                && (dates[0].getTime() > dates[1].getTime())) {
                sb.append(
                          getRepository().warning("From date is after to date"));
            } else if (varNames.size() == 0) {
                sb.append(getRepository().warning("No variables selected"));
            } else {
                NetcdfCFWriter writer = new NetcdfCFWriter();
                File f =
                    getRepository().getStorageManager().getTmpFile(request,
                                                                   "subset.nc");
                GridDataset gds = getGridDataset(file);
                synchronized(gds) {
                    writer.makeFile(f.toString(), gds, varNames, llr,
                                    ((dates[0] == null)
                                     ? null
                                     : new ucar.nc2.units.DateRange(dates[0],
                                                                    dates[1])), includeLatLon, hStride, zStride,
                                    timeStride);
                }

                if (request.get(ARG_ADDTOREPOSITORY, false)) {
                    if ( !canAdd) {
                        sb.append("Cannot add to repository");
                    } else {
                        Entry newEntry = (Entry) entry.clone();
                        File newFile =
                            getRepository().getStorageManager().moveToStorage(
                                                                              request, f);
                        newEntry.setResource(new Resource(newFile,
                                                          Resource.TYPE_STOREDFILE));
                        newEntry.setId(getRepository().getGUID());
                        newEntry.setName("subset_" + newEntry.getName());
                        newEntry.clearMetadata();
                        newEntry.setUser(request.getUser());
                        newEntry.addAssociation(
                                                new Association(
                                                                getRepository().getGUID(), "", "subset from",
                                                                entry.getId(), newEntry.getId()));
                        if (request.get(ARG_ADDMETADATA, false)) {
                            //                            System.err.println("adding metadata");
                            newEntry.clearArea();
                            List<Entry> entries =
                                (List<Entry>) Misc.newList(newEntry);
                            getRepository().addInitialMetadata(request,
                                                               entries);
                        }
                        getRepository().insertEntries(Misc.newList(newEntry),
                                                      true);
                        return new Result(
                                          request.entryUrl(
                                                           getRepository().URL_ENTRY_FORM, newEntry));
                    }
                } else {
                    return new Result(entry.getName() + ".nc",
                                      new FileInputStream(f),
                                      "application/x-netcdf");
                }
            }
        }

        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                                                         false, "");

        //        NetcdfDataset dataset =
        //            NetcdfDataset.openDataset(file.toString());
        sb.append(crumbs[1]);
        String formUrl = request.url(getRepository().URL_ENTRY_SHOW);
        String fileName = IOUtil.stripExtension(entry.getName())
            + "_subset.nc";

        sb.append(HtmlUtil.form(formUrl + "/" + fileName));
        sb.append(HtmlUtil.br());

        String submitExtra = "";
        if (canAdd) {
            submitExtra = HtmlUtil.space(1)
                + HtmlUtil.checkbox(
                                    ARG_ADDTOREPOSITORY, "true",
                                    request.get(ARG_ADDTOREPOSITORY, false)) + msg(
                                                                                   "Add to Repository") + HtmlUtil.checkbox(
                                                                                                                            ARG_ADDMETADATA, "true",
                                                                                                                            request.get(ARG_ADDMETADATA, false)) + msg(
                                                                                                                                                                       "Add metadata");

        }


        sb.append(HtmlUtil.submit("Subset Grid", ARG_SUBMIT));
        sb.append(submitExtra);
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.hidden(ARG_OUTPUT, OUTPUT_GRIDSUBSET));
        sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
        sb.append(HtmlUtil.formTable());

        sb.append(HtmlUtil.formEntry(msgLabel("Horizontal Stride"),
                                     HtmlUtil.input(ARG_HSTRIDE,
                                                    request.getString(ARG_HSTRIDE, "1"),
                                                    HtmlUtil.SIZE_3)));






        Date[]     dateRange = null;
        List<Date> dates     = null;


        GridDataset dataset = getGridDataset(file);
        StringBuffer varSB = new StringBuffer();
        synchronized(dataset) {
            for (VariableSimpleIF var : dataset.getDataVariables()) {
                if (var instanceof CoordinateAxis) {
                    CoordinateAxis ca       = (CoordinateAxis) var;
                    AxisType       axisType = ca.getAxisType();
                    if (axisType == null) {
                        continue;
                    }
                    if (axisType.equals(AxisType.Time)) {
                        dates = (List<Date>) Misc.sort(
                                                       ThreddsMetadataHandler.getDates(var, ca));
                    }
                    continue;
                }
            }
            for (GridDatatype     grid: sortGrids(dataset)) {
                VariableEnhanced var  = grid.getVariable();
                varSB.append(
                             HtmlUtil.row(
                                          HtmlUtil.cols(
                                                        HtmlUtil.checkbox(
                                                                          ARG_VARIABLE + "." + var.getShortName(), "true",
                                                                          false) + HtmlUtil.space(1) + var.getName()
                                                        + HtmlUtil.space(1)
                                                        + ((var.getUnitsString() != null)
                                                           ? "(" + var.getUnitsString() + ")"
                                                           : ""), "<i>" + var.getDescription()
                                                        + "</i>")));

            }

            if ((dates != null) && (dates.size() > 0)) {
                List formattedDates = new ArrayList();
                for (Date date : dates) {
                    formattedDates.add(getRepository().formatDate(request, date));
                }
                String fromDate = request.getUnsafeString(ARG_FROMDATE,
                                                          getRepository().formatDate(request,
                                                                                     dates.get(0)));
                String toDate = request.getUnsafeString(ARG_TODATE,
                                                        getRepository().formatDate(request,
                                                                                   dates.get(dates.size() - 1)));
                sb.append(
                          HtmlUtil.formEntry(
                                             msgLabel("Time Range"),
                                             HtmlUtil.checkbox(
                                                               ARG_SUBSETTIME, "true",
                                                               request.get(ARG_SUBSETTIME, true)) + HtmlUtil.space(
                                                                                                                   1) + HtmlUtil.select(
                                                                                                                                        ARG_FROMDATE, formattedDates,
                                                                                                                                        fromDate) + HtmlUtil.img(
                                                                                                                                                                 getRepository().fileUrl(
                                                                                                                                                                                         ICON_ARROW)) + HtmlUtil.select(
                                                                                                                                                                                                                        ARG_TODATE, formattedDates, toDate)));
            }


            /*
              for (CoordinateSystem coordSys : (List<CoordinateSystem>)dataset
              .getCoordinateSystems()) {
              ProjectionImpl proj = coordSys.getProjection();
              if (proj == null) {
              continue;
              }
              break;
              }
            */
            LatLonRect llr = dataset.getBoundingBox();
            if (llr != null) {
                sb.append(
                          HtmlUtil.formEntryTop(
                                                msgLabel("Subset Spatially"),
                                                "<table cellpadding=0 cellspacing=0><tr valign=top><td>"
                                                + HtmlUtil.checkbox(
                                                                    ARG_SUBSETAREA, "true",
                                                                    request.get(ARG_SUBSETAREA, false)) + "</td><td>"
                                                + HtmlUtil.makeLatLonBox(
                                                                         ARG_AREA, llr.getLatMin(), llr.getLatMax(),
                                                                         llr.getLonMax(),
                                                                         llr.getLonMin()) + "</table>"));
            }


            sb.append(HtmlUtil.formEntry(msgLabel("Add Lat/Lon Variables"),
                                         HtmlUtil.checkbox(ARG_ADDLATLON, "true",
                                                           request.get(ARG_ADDLATLON, true))));

        }
        sb.append("</table>");
        sb.append("<hr>");
        sb.append("Select Variables:<ul>");
        sb.append("<table>");
        sb.append(varSB);
        sb.append("</table>");
        sb.append("</ul>");
        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.submit("Subset Grid"));
        sb.append(HtmlUtil.formClose());
        return new Result("Grid Subset", sb);
    }


    public List<GridDatatype> sortGrids(GridDataset dataset) {
        List       tuples    = new ArrayList();
        for (GridDatatype grid : dataset.getGrids()) {
            VariableEnhanced var = grid.getVariable();
            tuples.add(new Object[] { var.getShortName().toLowerCase(),
                                      grid });
        }
        tuples = Misc.sortTuples(tuples, true);
        List<GridDatatype> result = new ArrayList<GridDatatype>();
        for (Object[] tuple : (List<Object[]>) tuples) {
            result.add((GridDatatype) tuple[1]);
        }
        return result;
    }


    public Result outputPointMap(Request request, Entry entry)
        throws Exception {
        File file = entry.getResource().getFile();
        PointObsDataset pod  = getPointDataset(file);
        StringBuffer sb = new StringBuffer();

        String[] crumbs = getRepository().getBreadCrumbs(request, entry,
                                                         false, "");
        sb.append(crumbs[1]);
        synchronized(pod) {
            List    vars    = pod.getDataVariables();
            int skip = request.get(ARG_SKIP,0);
            int max = request.get(ARG_MAX,200);

            StringBuffer js = new StringBuffer();
            js.append("var marker;\n");
            Iterator  dataIterator = pod.getDataIterator(16384);
            int cnt =0 ;
            int total =0 ;
            String icon = getRepository().fileUrl("/icons/pointdata.gif");

            List columnDataList = new ArrayList();
            while (dataIterator.hasNext()) {
                PointObsDatatype po = (PointObsDatatype) dataIterator.next();
                ucar.nc2.dt.EarthLocation el = po.getLocation();
                if (el == null) {
                    continue;
                }
                double lat = el.getLatitude();
                double lon = el.getLongitude();
                if(lat!=lat || lon!=lon) continue;
                if(lat<-90 || lat>90 || lon<-180 || lon>180) continue;
                total++;
                if(total<=skip) continue;
                if(total>(max+skip)) continue;
                cnt++;
                List  columnData = new ArrayList();
                StructureData structure = po.getData();
                js.append("marker = new Marker("
                          + llp(el.getLatitude(), el.getLongitude()) + ");\n");

                js.append("marker.setIcon(" + HtmlUtil.quote(icon) + ");\n");
                StringBuffer info = new StringBuffer("");
                info.append("<b>Date:</b> " +  po.getNominalTimeAsDate() +"<br>");
                for(VariableSimpleIF var: (List<VariableSimpleIF>)vars) {
                    //{name:\"Ashley\",breed:\"German Shepherd\",age:12}
                    StructureMembers.Member member = structure.findMember(var.getShortName());
                    if(var.getDataType() == DataType.STRING
                       || var.getDataType() == DataType.CHAR) {
                        String value = structure.getScalarString(member);
                        columnData.add(var.getShortName()+":" + HtmlUtil.quote(value));
                        info.append("<b>" + var.getName() +": </b>"+                        
                                    value +"</br>");

                    } else {
                        float value = structure.convertScalarFloat(member);
                        info.append("<b>" + var.getName() +": </b>"+                        
                                    value +"</br>");

                        columnData.add(var.getShortName()+":" +value);
                    }
                }
                columnDataList.add("{" + StringUtil.join(",", columnData)+"}\n");
                js.append("marker.setInfoBubble(\"" + info.toString() + "\");\n");
                js.append("initMarker(marker," + HtmlUtil.quote(""+cnt) + ");\n");
            }
        
            js.append("mapstraction.autoCenterAndZoom();\n");
            //        js.append("mapstraction.resizeTo(" + width + "," + height + ");\n");

            StringBuffer yui  = new StringBuffer();

            List  columnDefs = new ArrayList();
            List columnNames = new ArrayList();
            for(VariableSimpleIF var: (List<VariableSimpleIF>)vars) {
                columnNames.add(HtmlUtil.quote(var.getShortName()));
                String label = var.getDescription();
                //            if(label.trim().length()==0)
                label =var.getName();
                columnDefs.add("{key:" + HtmlUtil.quote(var.getShortName()) +"," +
                               "sortable:true," +
                               "label:" + HtmlUtil.quote(label) +
                               "}");
            }
        


            /*
              yui.append("YAHOO.example.data = [" + StringUtil.join(",", columnDataList)+"]\n");
              yui.append("var myDataSource = new YAHOO.util.DataSource(YAHOO.example.data);\n");
              yui.append("myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;\n");
              yui.append("myDataSource.responseSchema = {\n    fields: [" + StringUtil.join(",", columnNames) +"]};\n");
              yui.append("var myColumnDefs = [\n  " + StringUtil.join(",",columnDefs) + "\n];\n");
              yui.append("var myDataTable = new YAHOO.widget.DataTable(\"datatable\", myColumnDefs, myDataSource);\n");
            */
  
            if(total>max) {
                sb.append((skip+1) +"-" + (skip+cnt) + " of " + total +" ");
            } else {
                sb.append((skip+1) +"-" + (skip+cnt));
            }
            if(total>max) {
                boolean didone =false;
                if (skip > 0) {
                    sb.append(HtmlUtil.space(2));
                    sb.append(HtmlUtil.href(HtmlUtil.url(request.getRequestPath(),
                                                         new String[]{
                                                             ARG_OUTPUT, request.getOutput(),
                                                             ARG_ID, entry.getId(),
                                                             ARG_SKIP,""+(skip - max),
                                                             ARG_MAX, ""+max}), msg("Previous")));
                    didone = true;
                }
                if (total > (skip+cnt)) {
                    sb.append(HtmlUtil.space(2));
                    sb.append(HtmlUtil.href(HtmlUtil.url(request.getRequestPath(),
                                                         new String[]{
                                                             ARG_OUTPUT, request.getOutput(),
                                                             ARG_ID, entry.getId(),
                                                             ARG_SKIP,""+(skip + max),
                                                             ARG_MAX, ""+max}), msg("Next")));
                    didone=true;
                }
                //Just come up with some max number
                if(didone && total<2000) {
                    sb.append(HtmlUtil.space(2));
                    sb.append(HtmlUtil.href(HtmlUtil.url(request.getRequestPath(),
                                                         new String[]{
                                                             ARG_OUTPUT, request.getOutput(),
                                                             ARG_ID, entry.getId(),
                                                             ARG_SKIP,""+0,
                                                             ARG_MAX, ""+total}), msg("All")));

                }
            }
            //        sb.append("<table width=\"100%\"><tr valign=top><td>\n");
            getRepository().initMap(request, sb,800,500,true);
            /*        sb.append("</td><td>");
                      sb.append(HtmlUtil.div("",HtmlUtil.id("datatable")+HtmlUtil.cssClass(" yui-skin-sam")));
                      sb.append("</td></tr></table>");
                      sb.append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"http://yui.yahooapis.com/2.5.2/build/fonts/fonts-min.css\" />\n<link rel=\"stylesheet\" type=\"text/css\" href=\"http://yui.yahooapis.com/2.5.2/build/datatable/assets/skins/sam/datatable.css\" />\n<script type=\"text/javascript\" src=\"http://yui.yahooapis.com/2.5.2/build/yahoo-dom-event/yahoo-dom-event.js\"></script>\n<script type=\"text/javascript\" src=\"http://yui.yahooapis.com/2.5.2/build/dragdrop/dragdrop-min.js\"></script>\n<script type=\"text/javascript\" src=\"http://yui.yahooapis.com/2.5.2/build/element/element-beta-min.js\"></script>\n<script type=\"text/javascript\" src=\"http://yui.yahooapis.com/2.5.2/build/datasource/datasource-beta-min.js\"></script>\n<script type=\"text/javascript\" src=\"http://yui.yahooapis.com/2.5.2/build/datatable/datatable-beta-min.js\"></script>\n");
        
                      sb.append(HtmlUtil.script(yui.toString()));
            */

            sb.append(HtmlUtil.script(js.toString()));
            return new Result("Point Data", sb);
        }
    }

    private static String llp(double lat, double lon) {
        return "new LatLonPoint(" + lat + "," + lon + ")";
    }


    public Result outputPointCsv(Request request, Entry entry)
        throws Exception {
        File file = entry.getResource().getFile();
        PointObsDataset pod  = getPointDataset(file);
        StringBuffer sb = new StringBuffer();
        synchronized(pod) {
            List    vars    = pod.getDataVariables();
            Iterator  dataIterator = pod.getDataIterator(16384);
            int cnt =0;
            while (dataIterator.hasNext()) {
                PointObsDatatype po = (PointObsDatatype) dataIterator.next();
                ucar.nc2.dt.EarthLocation el = po.getLocation();
                if (el == null) {
                    continue;
                }
                cnt++;

                double lat = el.getLatitude();
                double lon = el.getLongitude();
                StructureData structure = po.getData();

                if(cnt==1) {
                    sb.append(HtmlUtil.quote("Time"));
                    sb.append(",");
                    sb.append(HtmlUtil.quote("Latitude"));
                    sb.append(",");
                    sb.append(HtmlUtil.quote("Longitude"));
                    for(VariableSimpleIF var: (List<VariableSimpleIF>)vars) {
                        sb.append(",");
                        String unit = var.getUnitsString();
                        if(unit!=null) {
                            sb.append(HtmlUtil.quote(var.getShortName()+" (" + unit+")"));
                        } else {
                            sb.append(HtmlUtil.quote(var.getShortName()));
                        }
                    }
                    sb.append("\n");
                }

                sb.append(HtmlUtil.quote(""+po.getNominalTimeAsDate()));
                sb.append(",");
                sb.append(el.getLatitude());
                sb.append(",");
                sb.append(el.getLongitude());

                for(VariableSimpleIF var: (List<VariableSimpleIF>)vars) {
                    StructureMembers.Member member = structure.findMember(var.getShortName());
                    sb.append(",");
                    if(var.getDataType() == DataType.STRING
                       || var.getDataType() == DataType.CHAR) {
                        sb.append(HtmlUtil.quote(structure.getScalarString(member)));
                    } else {
                        sb.append(structure.convertScalarFloat(member));
                    }
                }
                sb.append("\n");
            }
            return new Result("Point Data", sb,getRepository().getMimeTypeFromSuffix(".csv"));
        }
    }



    public Result outputPointKml(Request request, Entry entry)
        throws Exception {
        File file = entry.getResource().getFile();
        PointObsDataset pod  = getPointDataset(file);
        synchronized(pod) {
            Element root =KmlUtil.kml(entry.getName());
            Element docNode =KmlUtil.document(root,entry.getName());
            List    vars    = pod.getDataVariables();
            Iterator  dataIterator = pod.getDataIterator(16384);
            while (dataIterator.hasNext()) {
                PointObsDatatype po = (PointObsDatatype) dataIterator.next();
                ucar.nc2.dt.EarthLocation el = po.getLocation();
                if (el == null) {
                    continue;
                }
                double lat = el.getLatitude();
                double lon = el.getLongitude();
                double alt = 0;
                if(lat!=lat || lon!=lon) continue;

                StructureData structure = po.getData();
                StringBuffer info = new StringBuffer("");
                info.append("<b>Date:</b> " +  po.getNominalTimeAsDate() +"<br>");
                for(VariableSimpleIF var: (List<VariableSimpleIF>)vars) {
                    StructureMembers.Member member = structure.findMember(var.getShortName());
                    if(var.getDataType() == DataType.STRING
                       || var.getDataType() == DataType.CHAR) {
                        info.append("<b>" + var.getName() +": </b>"+                        
                                    structure.getScalarString(member) +"<br>");
                    } else {
                        info.append("<b>" + var.getName() +": </b>"+                        
                                    structure.convertScalarFloat(member) +"<br>");

                    }
                }
                KmlUtil.placemark(docNode, ""+po.getNominalTimeAsDate(), info.toString(), lat,lon,alt,null);
            }
            StringBuffer sb = new StringBuffer(XmlUtil.toString(root));
            return new Result("Point Data", sb,getRepository().getMimeTypeFromSuffix(".kml"));
        }
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
        if (output.equals(OUTPUT_CDL)) {
            return outputCdl(request, entry);
        }
        if (output.equals(OUTPUT_WCS)) {
            return outputWcs(request, entry);
        }


        if (output.equals(OUTPUT_GRIDSUBSET)
            || output.equals(OUTPUT_GRIDSUBSET_FORM)) {
            return outputGridSubset(request, entry);
        }


        if (output.equals(OUTPUT_POINT_MAP)) {
            return outputPointMap(request, entry);
        }
        if (output.equals(OUTPUT_POINT_CSV)) {
            return outputPointCsv(request, entry);
        }

        if (output.equals(OUTPUT_POINT_KML)) {
            return outputPointKml(request, entry);
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


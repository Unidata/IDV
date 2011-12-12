/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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



package ucar.unidata.data.sounding;


import ucar.ma2.Range;

import ucar.nc2.*;

import ucar.unidata.data.*;

import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.ui.SqlShell;

import ucar.unidata.ui.TwoListPanel;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;

import ucar.visad.Util;
import ucar.visad.quantities.CommonUnits;


import visad.*;

import visad.georef.EarthLocationTuple;

import visad.util.DataUtility;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.net.MalformedURLException;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A data source for balloon and aircraft tracks
 *
 * @author IDV Development Team
 * @version $Revision: 1.90 $ $Date: 2007/08/06 17:02:27 $
 */
public class TrackDataSource extends FilesDataSource {


    /** mutex for initTrack */
    private static final Object MUTEX = new Object();




    /** track categories */
    protected List traceCats;

    /** sounding categories */
    protected List soundingCats;

    /** raob categories */
    private List raobCats;

    /** point categories */
    private List pointCats;


    /** first adapter */
    private TrackAdapter traceAdapter;

    /** Holds the variable names that we only show. If empty then we show all. */
    private Hashtable pointDataFilter = new Hashtable();

    /** sounding ob id */
    public static final String ID_SOUNDINGOB = "id.soundingob";

    /** sounding trace id */
    public static final String ID_SOUNDINGTRACE = "id.soundingtrace";

    /** time trace id */
    public static final String ID_TIMETRACE = "id.timetrace";

    /** The whole track */
    public static final String ID_WHOLETRACE = "id.wholetrace";

    /** Point track type */
    public static final String ID_POINTTRACE = "id.pointtrace";

    /** Last point track type */
    public static final String ID_LASTOB = "id.lastob";



    /** data identifiers */
    private static String[] DATA_IDS = { ID_SOUNDINGOB, ID_SOUNDINGTRACE,
                                         ID_TIMETRACE, ID_POINTTRACE,
                                         ID_LASTOB };


    /** labels for ids */
    private static String[] DATA_LABELS = { "Sounding Data", "Skew-T data",
                                            "Time Trace", "Point Data",
                                            "Last Point" };


    /** track type identifier */
    public static final String PROP_TRACKTYPE = "track type";


    /** The stride */
    private int stride = 1;

    /** last n minutes */
    private int lastNMinutes = -1;


    /** _more_ */
    private boolean usingDataBase = false;


    /** widget for properties dialog */
    private JTextField strideFld;

    /** widget for properties dialog */
    private JTextField lastNMinutesFld;

    /** widget for properties dialog */
    private TwoListPanel twoListPanel;


    /** widget for properties dialog */
    private boolean haveAskedToSubset = false;

    /** _more_ */
    private SqlShell sqlShell;

    /** Default Constructor */
    public TrackDataSource() {}

    /**
     * Create a TrackDataSource from the specification given.
     *
     * @param descriptor    descriptor for the DataSource
     * @param source        file location or URL
     * @param properties    extra properties
     *
     * @throws VisADException
     *
     */
    public TrackDataSource(DataSourceDescriptor descriptor, String source,
                           Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(source), properties);
    }



    /**
     * Create a TrackDataSource from the specification given.
     *
     * @param descriptor    data source descriptor
     * @param sources       List of sources of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public TrackDataSource(DataSourceDescriptor descriptor, List sources,
                           Hashtable properties)
            throws VisADException {
        this(descriptor, sources, "Track Files", properties);
    }



    /**
     * Create a TrackDataSource from the specification given.
     *
     * @param descriptor    data source descriptor
     * @param sources       List of sources of data (filename/URL)
     * @param description   dataset description
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public TrackDataSource(DataSourceDescriptor descriptor, List sources,
                           String description, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, ((sources.size() > 1)
                                    ? description
                                    : sources.get(0).toString()), 
                                    description, properties);
    }


    /**
     * _more_
     */
    public void doRemove() {
        super.doRemove();
        if (sqlShell != null) {
            sqlShell.close();

        }
    }


    /**
     * _more_
     *
     * @param actions _more_
     */
    protected void addActions(List actions) {
        super.addActions(actions);
        if (traceAdapter != null) {
            traceAdapter.addActions(actions);
        }
    }



    /**
     * _more_
     */
    public void showSqlShell() {
        if (sqlShell == null) {
            sqlShell = new SqlShell(
                "Sql Shell - " + getName(),
                ((EolDbTrackAdapter) traceAdapter).getConnection());
        }
        sqlShell.show();
    }


    /**
     * _more_
     */
    protected void initAfter() {
        super.initAfter();
        getAdapters();
        if (getInError()) {
            return;
        }
        if ((traceAdapter != null)
                && (traceAdapter.getDataSourceDescription() != null)) {
            setDescription(traceAdapter.getDataSourceDescription());
        }
    }


    /**
     * _more_
     */
    public void initAfterCreation() {
        super.initAfterCreation();
        if (getInError()) {
            return;
        }
        if ((traceAdapter != null)
                && (traceAdapter.getDataSourceName() != null)) {
            setName(traceAdapter.getDataSourceName());
        }
    }

    /**
     * Initialize the categories
     */
    protected void initCategories() {
        if (traceCats == null) {
            traceCats    = DataCategory.parseCategories("Track;trace", true);
            soundingCats = Misc.newList(DataCategory.TRACK_SOUNDING_CATEGORY);
            raobCats     = Misc.newList(DataCategory.RAOB_SOUNDING_CATEGORY);
            pointCats    = Misc.newList(DataCategory.POINT_PLOT_CATEGORY);
        }
    }



    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased();
    }


    /**
     * Get the full description for this data source.  Subclasses should
     * override if they want something other than the default.  This is what
     * gets displayed in the details.
     *
     * @return  the full description of this data source (description + name)
     */
    public String getFullDescription() {
        StringBuffer desc = new StringBuffer("Track Data Source<p>");
        getAdapters();
        if ((traceAdapter != null)
                && (traceAdapter.getDataSourceDescription() != null)) {
            desc.append(traceAdapter.getDataSourceDescription());
            desc.append("<hr>");
        }

        desc.append("Stride: " + stride + "<p>");
        if (lastNMinutes > 0) {
            desc.append("Last N Minutes: " + lastNMinutes + "<p>");
        }
        List adapters = getAdapters();
        if (adapters == null) {
            desc.append("No track files<p>\n");
        } else {
            desc.append(
                "Track files:<table><tr><td><b>File</b></td><td><b>Time</b></td></td></tr>\n");
            for (int i = 0; i < adapters.size(); i++) {
                TrackAdapter adapter = (TrackAdapter) adapters.get(i);
                desc.append("<tr><td>" + adapter.getFilename() + "</td><td>"
                            + adapter.getStartTime() + " -- <br>"
                            + adapter.getEndTime() + "</td></tr>\n");
            }
            desc.append("</table>\n");
        }


        if (traceAdapter == null) {
            return desc.toString();
        }

        List trackInfos = traceAdapter.getTrackInfos();
        for (int trackIdx = 0; trackIdx < trackInfos.size(); trackIdx++) {
            int          total     = 0;
            int          hidden    = 0;
            StringBuffer extra     = null;
            TrackInfo    trackInfo = (TrackInfo) trackInfos.get(trackIdx);
            desc.append("<p>");
            if (trackInfos.size() > 1) {
                desc.append(trackInfo.getTrackName() + "<p>");
            }
            try {
                desc.append(" Total observations:"
                            + trackInfo.getNumberPoints() + "<p>\n");
            } catch (Exception exc) {}
            StringBuffer  params = new StringBuffer();


            List<VarInfo> vars   = trackInfo.getVariables();
            for (VarInfo varInfo : vars) {
                total++;

                String name = varInfo.getName();
                if ( !trackInfo.includeInPointData(name)) {
                    hidden++;
                    if (extra == null) {
                        extra = new StringBuffer("");
                    }
                    extra.append("<li>" + varInfo.getDescription() + " ("
                                 + name + ")\n");
                } else {
                    params.append("<li>" + varInfo.getDescription() + " ("
                                  + name + ")\n");
                }
            }
            desc.append("Parameters: ");
            if (hidden != 0) {
                desc.append((total - hidden) + "/" + total);
            } else {
                desc.append(total + "");
            }
            desc.append(" <ul>\n");
            desc.append(params.toString());
            desc.append("</ul>\n");
            if (extra != null) {
                desc.append("Hidden Parameters: " + hidden + "/" + total
                            + " <ul>\n");
                desc.append(extra.toString());
                desc.append("</ul>");
            }
        }
        return desc.toString();
    }





    /**
     * Get the default categories trace choices
     *
     * @return list of categories
     */
    protected List getTraceCategories() {
        initCategories();
        return traceCats;
    }

    /**
     * Get the default categories sounding choices
     *
     * @return list of categories
     */
    protected List getSoundingCategories() {
        initCategories();
        return soundingCats;
    }

    /**
     * Get the default categories RAOB sounding choices
     *
     * @return list of categories
     */
    protected List getRaobCategories() {
        initCategories();
        return raobCats;
    }



    /**
     * Initialize the track
     */
    protected void initTrack() {
        synchronized (MUTEX) {
            if ((adapters != null) && (adapters.size() > 0)) {
                return;
            }
            //Make sure they exist
            List tmp = new ArrayList(sources);
            sources = new ArrayList();
            for (int i = 0; i < tmp.size(); i++) {
                String fileOrUrl = tmp.get(i).toString();
                if (fileOrUrl.startsWith("jdbc:")) {
                    usingDataBase = true;
                    sources.add(fileOrUrl);
                } else if ((new File(fileOrUrl)).exists()) {
                    sources.add(fileOrUrl);
                } else {
                    try {
                        new URL(fileOrUrl);
                        sources.add(fileOrUrl);
                    } catch (Exception exc) {}
                }
            }

            traceAdapter = null;
            adapters     = new ArrayList();

            //If none of them exist then keep the list around and return

            if (sources.size() == 0) {
                sources = tmp;
                return;
            }
            String file = "";
            flushCache();
            List exceptions = new ArrayList();
            List messages   = new ArrayList();

            for (int i = 0; i < sources.size(); i++) {
                try {
                    file = (String) sources.get(i);
                    TrackAdapter adapter = doMakeAdapter(file,
                                               pointDataFilter, stride,
                                               lastNMinutes);
                    if (traceAdapter == null) {
                        traceAdapter = adapter;
                    }
                    adapters.add(adapter);
                } catch (Throwable e) {
                    exceptions.add(e);
                    messages.add("Problem opening file:" + file + " " + e);
                }
            }
            if (exceptions.size() > 0) {
                LogUtil.printExceptions(messages, exceptions);
                if (sources.size() == exceptions.size()) {
                    setInError(true, false, null);
                }
            }
        }
    }



    /**
     * Factory method to make the adapter
     *
     * @param file file or url
     * @param pointDataFilter  When creating point data this holds the map of variables to use
     * @param stride string
     * @param lastNMinutes clipping
     *
     * @return The adapter
     *
     * @throws Exception On badness
     */
    protected TrackAdapter doMakeAdapter(String file,
                                         Hashtable pointDataFilter,
                                         int stride, int lastNMinutes)
            throws Exception {
        if (file.startsWith("jdbc:")) {
            usingDataBase = true;
            return new EolDbTrackAdapter(this, file, pointDataFilter, stride,
                                         lastNMinutes);
        }
        return new CdmTrackAdapter(this, file, pointDataFilter, stride,
                                   lastNMinutes);
    }



    /**
     * Create, if needed, and return the list of adapters.
     * Will return null if there are no valid adapters.
     *
     * @return List of adapters or null
     */
    public List getAdapters() {
        if ((adapters == null) || (adapters.size() == 0)) {
            initTrack();
        }
        if (adapters.size() == 0) {
            adapters = null;
        }
        return adapters;
    }



    /**
     * Make the {@link DataChoice}s associated with this dataset
     */
    protected void doMakeDataChoices() {
        List adapters = getAdapters();
        if (adapters == null) {
            return;
        }
        initCategories();
        List trackInfos = traceAdapter.getTrackInfos();
        List categories = traceCats;
        for (int trackIdx = 0; trackIdx < trackInfos.size(); trackIdx++) {
            TrackInfo     trackInfo = (TrackInfo) trackInfos.get(trackIdx);
            List<VarInfo> vars      = trackInfo.getVariables();

            String        trackName = trackInfo.getTrackName();


            String        basicCat  = null;
            //If there are any categories then use the Basic
            //            if(parameterCats.size()>0 && parameterCats.get(0)!=null) {
            //                basicCat = "Basic";
            //            }

            if (trackInfos.size() > 1) {
                categories = DataCategory.parseCategories(trackName
                        + "-Tracks" + ";trace", true);
            }
            Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                                  "/auxdata/ui/icons/TrajectoryData16.gif");
            for (VarInfo varInfo : vars) {
                List cats = categories;
                if (varInfo.getCategory() != null) {
                    String cat = StringUtil.replace(varInfo.getCategory(),
                                     "-", " ");
                    cats = DataCategory.parseCategories("Track-" + cat
                            + ";trace", true);
                }
                DirectDataChoice ddc = new DirectDataChoice(this,
                                           new String[] { trackName,
                        varInfo.getName() }, varInfo.getName(),
                                             varInfo.getDescription(), cats,
                                             props);
                addDataChoice(ddc);
            }
            // add in a station plot choice as well
            List pointCatList = pointCats;
            if (trackInfos.size() > 1) {
                pointCatList = DataCategory.parseCategories(trackName + ";"
                        + DataCategory.POINT_PLOT_CATEGORY, true);
            }
            props = Misc.newHashtable(DataChoice.PROP_ICON,
                                      "/auxdata/ui/icons/Placemark16.gif");
            String pointLabel = getDataChoiceLabel(ID_POINTTRACE);
            addDataChoice(new DirectDataChoice(this, new String[] { trackName,
                    ID_POINTTRACE }, pointLabel, pointLabel, pointCatList,
                                     props));
            /*
            addDataChoice(new DirectDataChoice(this, new String[] { trackName,
                    ID_LASTOB }, getDataChoiceLabel(ID_LASTOB),
                                 getDataChoiceLabel(ID_LASTOB), pointCatList,
                                 props));
            */

        }


    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean canDoView() {
        return true;
    }

    /**
     * Get the data choice id
     *
     * @param id   id for a given name
     *
     * @return the data choice ID
     */
    private String getDataChoiceId(Object id) {
        for (int i = 0; i < DATA_IDS.length; i++) {
            if (Misc.equals(id, DATA_IDS[i])
                    || Misc.equals(id, DATA_LABELS[i])) {
                return DATA_IDS[i];
            }
        }
        return id.toString();
    }

    /**
     * Get the label for a particular data choice
     *
     * @param id  id for the choice
     *
     * @return the appropriate label
     */
    protected String getDataChoiceLabel(Object id) {
        for (int i = 0; i < DATA_IDS.length; i++) {
            if (Misc.equals(id, DATA_IDS[i])) {
                return DATA_LABELS[i];
            }
        }
        return id.toString();
    }





    /**
     * Get the VisAD Data object that corresponds to the dataChoice
     * category and dataSelection criteria.
     *
     * @param dataChoice         choice for data
     * @param category           specific category of data (not used currently)
     * @param dataSelection      additional selection criteria
     * @param requestProperties  extra request properties
     *
     * @return corresponding Data object
     *
     * @throws VisADException  unable to create Data object
     * @throws RemoteException (some kind of remote error.
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        Data retData = null;

        try {
            //Call getDataChoiceId to handle legacy bundles that had the id also be the label
            Object id = getChoiceId(dataChoice);
            if (id.equals(ID_SOUNDINGTRACE)) {
                retData = getSoundingTrace(dataChoice, dataSelection);
            } else if (id.equals(ID_SOUNDINGOB)) {
                retData = getSoundingObs(dataChoice, dataSelection);
            } else if (id.equals(ID_POINTTRACE)) {
                if ( !getHaveAskedToSubset()) {
                    if ((pointDataFilter == null)
                            || pointDataFilter.isEmpty()) {
                        if (GuiUtils.askYesNo("Subset Data",
                                "<html>It is recommended that you subset the point data.<br>Do you want to subset the point data?</html>")) {
                            if ( !showPropertiesDialog("Subset Point Data",
                                    true)) {
                                return null;
                            }
                        }
                    }
                    setHaveAskedToSubset(true);
                }
                retData = getPointObTrack(dataChoice, dataSelection, null);
            } else if (id.equals(ID_LASTOB)) {
                retData = getLastPointOb(dataChoice, dataSelection);
            } else {
                Object id0 = getChoiceId0(dataChoice);

                List tracks = getTracks(dataChoice, dataSelection,
                                        requestProperties);
                if ((tracks != null) && (tracks.size() > 0)) {
                    retData = aggregateTracks(tracks, id0);
                }
            }
        } catch (Exception exc) {
            throw new WrapperException(exc);

        }
        if (retData == null) {}
        return retData;
    }


    /**
     * Get the base time to be used for the track. This is the largest of the end times
     *
     * @return Base time.
     */
    protected DateTime getBaseTime() {
        DateTime[] minMax = getMinMaxTime();
        if (minMax == null) {
            return null;
        }
        return minMax[1];
    }

    /**
     * Get the min and max times in the  tracks
     *
     * @return man/max times of the tracks
     */
    protected DateTime[] getMinMaxTime() {
        List adapters = getAdapters();
        if (adapters == null) {
            try {
                return new DateTime[] { new DateTime(), new DateTime() };
            } catch (Exception exc) {
                logException("Create null datetimes", exc);
                return null;
            }
        }
        DateTime startTime = null;
        DateTime endTime   = null;
        for (int i = 0; i < adapters.size(); i++) {
            TrackAdapter adapter = (TrackAdapter) getAdapters().get(i);
            DateTime     theTime = adapter.getEndTime();
            if ((endTime == null)
                    || (theTime.getValue() > endTime.getValue())) {
                endTime = theTime;
            }
            if ((startTime == null)
                    || (theTime.getValue() > startTime.getValue())) {
                startTime = theTime;
            }
        }
        return new DateTime[] { startTime, endTime };
    }


    /**
     * Aggregate the list of track data
     *
     * @param tracks list of track data
     * @param id _more_
     *
     * @return Aggregation
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected FieldImpl aggregateTracks(List tracks, Object id)
            throws VisADException, RemoteException {
        FlatField mergedTracks = mergeTracks(tracks);
        FunctionType fiType = new FunctionType(RealType.Time,
                                  mergedTracks.getType());
        DateTime endTime = getBaseTime();
        FieldImpl fi =
            new FieldImpl(fiType,
                          new SingletonSet(new RealTuple(new Real[] {
                              endTime })));
        fi.setSample(0, mergedTracks, false);
        return fi;
    }

    /**
     * Make the list of times associated with this DataSource for
     * DataSelection.
     *
     * @return DateTimes as a list.
     */
    protected List doMakeDateTimes() {
        DateTime baseTime = getBaseTime();
        if (baseTime == null) {
            return new ArrayList();
        }
        return Misc.newList(baseTime);
    }

    /**
     * Utility to get the array that holds the trajectory id/variable name.
     * For old bundles the id of the data choice will be a string.
     * For new code it will be an array.
     *
     * @param dc The data choice
     *
     * @return id array
     */
    private String[] getIdArray(DataChoice dc) {
        Object id = dc.getId();
        if (id instanceof String) {
            id = new String[] { null, id.toString() };
        }
        return (String[]) id;
    }


    /**
     * Gets the track associated with this DataChoice
     *
     * @param dc                 DataChoice for selection
     * @param dataSelection      sub selection criteria
     * @param requestProperties  selection properties
     * @return associated track
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected List getTracks(DataChoice dc, DataSelection dataSelection,
                             Hashtable requestProperties)
            throws VisADException, RemoteException {
        try {
            boolean withTimes = false;
            if (requestProperties != null) {
                withTimes = ID_TIMETRACE.equals(
                    requestProperties.get(PROP_TRACKTYPE));
            }
            List tracks   = new ArrayList();
            List adapters = getAdapters();
            if (adapters == null) {
                return tracks;
            }
            String[] idArray = getIdArray(dc);
            for (int i = 0; i < adapters.size(); i++) {
                Data data = null;
                if (withTimes) {
                    data = ((TrackAdapter) adapters.get(i)).getTrackWithTime(
                        idArray[0], idArray[1], null);

                } else {
                    data = ((TrackAdapter) adapters.get(i)).getTrack(
                        idArray[0], idArray[1], null);
                }
                if (data == null) {
                    return null;
                }
                tracks.add(data);
            }
            return tracks;
        } catch (Exception exc) {
            LogUtil.logException("Could not read data", exc);
            return null;
        }
    }


    /**
     * Gets the track associated with this DataChoice
     *
     * @param dc                 DataChoice for selection
     * @param dataSelection      sub selection criteria
     * @param requestProperties  selection properties
     * @return associated track
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected FlatField getTrack(DataChoice dc, DataSelection dataSelection,
                                 Hashtable requestProperties)
            throws VisADException, RemoteException {

        List datas = getTracks(dc, dataSelection, requestProperties);
        if (datas == null) {
            return null;
        }
        return mergeTracks(datas);
    }




    /**
     * Gets the last point ob associated with this track
     *
     * @param dc                 DataChoice for selection
     * @param dataSelection      sub selection criteria
     * @return associated track
     *
     * @throws Exception On badness
     */
    protected FieldImpl getLastPointOb(DataChoice dc,
                                       DataSelection dataSelection)
            throws Exception {

        List adapters = getAdapters();
        if (adapters == null) {
            return null;
        }
        Range lastRange = ((TrackAdapter) adapters.get(0)).getLastPointRange(
                              getTrackId(dc));
        return getPointObTrack(dc, dataSelection, lastRange);
    }


    /**
     * Gets the point ob track associated with this DataChoice
     *
     * @param dc                 DataChoice for selection
     * @param dataSelection      sub selection criteria
     * @param range The data range
     * @return associated track
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected FieldImpl getPointObTrack(DataChoice dc,
                                        DataSelection dataSelection,
                                        Range range)
            throws VisADException, RemoteException {

        List adapters = getAdapters();
        if (adapters == null) {
            //TODO: What should we return here?
            return null;
        }
        FieldImpl retField = null;
        try {
            List datas = new ArrayList(adapters.size());
            for (int i = 0; i < adapters.size(); i++) {
                TrackAdapter adapter = (TrackAdapter) adapters.get(i);
                Data data = adapter.getPointObTrack(getTrackId(dc), range);
                if (data == null) {
                    return null;
                }
                datas.add(data);
            }
            retField = PointObFactory.mergeData(datas);
        } catch (Exception exc) {
            logException("Creating track obs", exc);
        }
        return retField;
    }


    /**
     * Gets the sounding trace associated with this DataChoice
     *
     * @param dc                 DataChoice for selection
     * @param dataSelection      sub selection criteria
     * @return associated track
     *
     * @throws Exception On badness
     */
    protected Data getSoundingTrace(DataChoice dc,
                                    DataSelection dataSelection)
            throws Exception {
        return traceAdapter.getAerologicalDiagramData(getTrackId(dc));
    }


    /**
     * Gets the sounding trace associated with this DataChoice
     * @return associated trace adapter
     *
     * @throws Exception On badness
     */
    public TrackAdapter getTraceAdapter() throws Exception {
        return traceAdapter;
    }

    /**
     * Utility to extract the choice id
     *
     * @param dc the data choice
     *
     * @return The id. eg: parameter name
     */
    protected String getChoiceId(DataChoice dc) {
        String[] idArray = getIdArray(dc);
        return idArray[1];
    }

    /**
     * Utility to extract the choice id
     *
     * @param dc the data choice
     *
     * @return The id. eg: parameter name
     */
    protected String getChoiceId0(DataChoice dc) {
        String[] idArray = getIdArray(dc);
        return idArray[0];
    }

    /**
     * Utility to get the track id
     *
     * @param dc data choice
     *
     * @return track id
     */
    protected String getTrackId(DataChoice dc) {
        String[] idArray = getIdArray(dc);
        return idArray[0];
    }


    /**
     * Get a set of sounding obs based on the choice
     *
     * @param dataChoice        DataChoice for data
     * @param subset            subselection criteria
     * @return  corresponding sounding observations
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private Data getSoundingObs(DataChoice dataChoice, DataSelection subset)
            throws VisADException, RemoteException {
        Vector v       = new Vector();
        List   choices = (dataChoice instanceof CompositeDataChoice)
                         ? ((CompositeDataChoice) dataChoice).getDataChoices()
                         : Arrays.asList(new DataChoice[] { dataChoice });
        for (Iterator iter = choices.iterator(); iter.hasNext(); ) {
            Data ob = getSoundingOb((DataChoice) iter.next(), subset);
            if (ob != null) {
                v.add(ob);
            }
        }
        return (v.isEmpty())
               ? null
               : new Tuple((Data[]) v.toArray(new Data[v.size()]), false);
    }


    /**
     * Gets the SoundingOb associated with this DataChoice
     *
     * @param dc                 DataChoice for selection
     * @param dataSelection      sub selection criteria
     *
     * @return SoundingOb
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getSoundingOb(DataChoice dc, DataSelection dataSelection)
            throws VisADException, RemoteException {
        SoundingOb so = ((SoundingAdapter) traceAdapter).getSoundingObs()[0];
        RAOB            raob = so.getRAOB();
        SoundingStation ss   = (SoundingStation) so.getStation();
        Tuple data = new Tuple(new Data[] { so.getTimestamp(),
                                            new EarthLocationTuple(
                                                ss.getLatitude(),
                                                ss.getLongitude(),
                                                ss.getAltitude().getValue()),
                                            raob.getTemperatureProfile(),
                                            raob.getDewPointProfile() });
        return data;
    }


    /**
     * Set the source property (filename or URL).  Used by persistence
     *
     * @param value  data source
     */
    public void setName(String value) {
        super.setName(value);
        // support for old bundles
        if (sources == null) {
            sources = Misc.newList(getName());
        }
    }

    /**
     * Get the point observartion data
     *
     *
     * Test routine
     *
     * @param args    filename
     *
     * @throws Exception  problem
     */
    public static void main(String[] args) throws Exception {




        if (true) {
            return;
        }



        if (args.length == 0) {
            System.err.println("Must supply a file name");
            System.exit(1);
        }
        TrackDataSource v5d = new TrackDataSource(null, Misc.toList(args),
                                  null);
        /*
        for (Iterator iter = v5d.getDataChoices().iterator(); iter.hasNext();) {
            System.err.println(iter.next());
        }
        Data testData = v5d.getData ((DataChoice) v5d.getDataChoices().get(0), null);
        visad.python.JPythonMethods.dumpTypes (testData);
        */
        for (Iterator iter =
                v5d.getAllDateTimes().iterator(); iter.hasNext(); ) {
            System.err.println(iter.next());
        }

    }

    /**
     * Merge a List of FieldImpls of point obs into one.
     * @param datas   List of FieldImpls of point obs
     * @return merged FieldImpl
     *
     * @throws VisADException problem getting the data
     */
    public FlatField mergeTracks(List datas) throws VisADException {

        if (datas.isEmpty()) {
            return null;
        }
        if (datas.size() == 1) {
            return (FlatField) datas.get(0);
        }
        FlatField retField = null;
        try {
            int        numObs    = 0;
            GriddedSet domainSet = null;
            FlatField  ff        = null;
            for (int i = 0; i < datas.size(); i++) {
                ff        = (FlatField) datas.get(i);
                domainSet = (GriddedSet) ff.getDomainSet();
                numObs    += domainSet.getLength();
            }
            FunctionType  retType = (FunctionType) ff.getType();
            RealTupleType rtt     = DataUtility.getFlatRangeType(ff);
            double[][] domainVals =
                new double[domainSet.getDimension()][numObs];
            float[][] values = new float[rtt.getDimension()][numObs];
            int       curPos = 0;
            for (int i = 0; i < datas.size(); i++) {
                FlatField  data    = (FlatField) datas.get(i);
                GriddedSet dset    = (GriddedSet) data.getDomainSet();
                double[][] samples = dset.getDoubles(false);
                int        length  = dset.getLength();
                float[][]  vals    = data.getFloats(false);
                for (int j = 0; j < samples.length; j++) {
                    System.arraycopy(samples[j], 0, domainVals[j], curPos,
                                     length);
                }
                for (int j = 0; j < vals.length; j++) {
                    System.arraycopy(vals[j], 0, values[j], curPos, length);
                }
                curPos += length;
            }
            // now make the new data
            // First make the domain set
            GriddedSet newDomain = null;
            if (domainSet instanceof Gridded1DDoubleSet) {
                newDomain = new Gridded1DDoubleSet(domainSet.getType(),
                        domainVals, numObs, domainSet.getCoordinateSystem(),
                        domainSet.getSetUnits(), domainSet.getSetErrors());
            } else {
                newDomain = GriddedSet.create(domainSet.getType(),
                        Set.doubleToFloat(domainVals), new int[] { numObs },
                        domainSet.getCoordinateSystem(),
                        domainSet.getSetUnits(), domainSet.getSetErrors());
            }
            retField = new FlatField(retType, newDomain);
            retField.setSamples(values, false);

        } catch (RemoteException re) {
            throw new VisADException("got RemoteException " + re);
        }
        return retField;
    }




    /**
     *  Set the PointDataFilter property.
     *
     *  @param value The new value for PointDataFilter
     */
    public void setPointDataFilter(Hashtable value) {
        pointDataFilter = value;
    }

    /**
     *  Get the PointDataFilter property.
     *
     *  @return The PointDataFilter
     */
    public Hashtable getPointDataFilter() {
        return pointDataFilter;
    }

    /**
     * Add the gui components into the list for the properties dialog
     *
     * @param comps List of components
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);

        if (usingDataBase) {
            comps.add(GuiUtils.filler());
            comps.add(getPropertiesHeader("Database"));
        }


        strideFld       = new JTextField("" + stride, 5);
        lastNMinutesFld = new JTextField("" + ((lastNMinutes > 0)
                ? "" + lastNMinutes
                : "all"), 5);
        comps.add(GuiUtils.filler());
        comps.add(getPropertiesHeader("Time Subsetting"));

        comps.add(GuiUtils.rLabel("Use Every: "));
        comps.add(GuiUtils.left(GuiUtils.hbox(strideFld,
                GuiUtils.lLabel(" Time Steps  "),
                GuiUtils.makeButton("Reset", this, "resetStrideFld"))));

        comps.add(GuiUtils.rLabel("Use Last: "));
        comps.add(GuiUtils.left(GuiUtils.hbox(lastNMinutesFld,
                GuiUtils.lLabel(" Minutes  "),
                GuiUtils.makeButton("Reset", this, "resetLastNMinutesFld"))));
    }

    /**
     * Reset last N minutes field
     */
    public void resetLastNMinutesFld() {
        if (lastNMinutesFld != null) {
            lastNMinutesFld.setText("all");
        }
    }

    /**
     * reset stride field
     */
    public void resetStrideFld() {
        if (strideFld != null) {
            strideFld.setText("1");
        }
    }


    /**
     * Apply properties components
     *
     * @return false if something failed and we need to keep showing the dialog
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }

        if (traceAdapter == null) {
            return true;
        }
        String what = "stride";
        String text = "";
        try {
            text = strideFld.getText().trim();
            setStride(new Integer(text).intValue());
            List adapters = getAdapters();
            if (adapters != null) {
                for (int i = 0; i < adapters.size(); i++) {
                    ((TrackAdapter) adapters.get(i)).setStride(stride);
                }
            }

            what = "last n minutes";
            text = lastNMinutesFld.getText().trim();
            if (text.equals("all") || (text.length() == 0)) {
                setLastNMinutes(-1);
            } else {
                setLastNMinutes(new Integer(text).intValue());
            }
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad " + what + " value:" + text);
            return false;
        }


        flushCache();
        if (twoListPanel != null) {
            List current =
                TwoFacedObject.getIdStrings(twoListPanel.getCurrentEntries());
            pointDataFilter.clear();
            for (int i = 0; i < current.size(); i++) {
                pointDataFilter.put(current.get(i), current.get(i));
            }
        }


        return true;

    }




    /**
     * Add any extra tabs into the properties tab
     *
     * @param tabbedPane The properties tab
     */
    public void addPropertiesTabs(JTabbedPane tabbedPane) {
        super.addPropertiesTabs(tabbedPane);
        if (traceAdapter == null) {
            return;
        }


        List trackInfos = traceAdapter.getTrackInfos();

        if (trackInfos.size() == 0) {
            return;
        }
        twoListPanel = new TwoListPanel(new ArrayList(), "All Fields",
                                        new ArrayList(), "Current Fields",
                                        null);

        TrackInfo     trackInfo = (TrackInfo) trackInfos.get(0);
        List<VarInfo> vars      = trackInfo.getVariables();

        HashSet       skipVars  = new HashSet();

        skipVars.add("time");
        skipVars.add("latitude");
        skipVars.add("longitude");
        skipVars.add("altitude");

        List      labels     = new ArrayList();
        List      ids        = new ArrayList();
        Hashtable currentMap = new Hashtable();
        for (VarInfo varInfo : vars) {
            String name = varInfo.getName();
            if (skipVars.contains(name.toLowerCase())) {
                continue;
            }
            labels.add(varInfo.getDescription() + "  (" + varInfo.getName()
                       + ")");
            ids.add(varInfo.getName());
            if (traceAdapter.includeInPointData(varInfo.getName())) {
                currentMap.put(varInfo.getName(), varInfo.getName());
            }
        }
        JComponent contents =
            twoListPanel.getContents("Select fields to show", labels, ids,
                                     currentMap);
        tabbedPane.add("Subset Point Data", contents);
    }




    /**
     * Set the Stride property.
     *
     * @param value The new value for Stride
     */
    public void setStride(int value) {
        stride = value;
    }

    /**
     * Get the Stride property.
     *
     * @return The Stride
     */
    public int getStride() {
        return stride;
    }


    /**
     * Set the HaveAskedToSubset property.
     *
     * @param value The new value for HaveAskedToSubset
     */
    public void setHaveAskedToSubset(boolean value) {
        haveAskedToSubset = value;
    }

    /**
     * Get the HaveAskedToSubset property.
     *
     * @return The HaveAskedToSubset
     */
    public boolean getHaveAskedToSubset() {
        return haveAskedToSubset;
    }


    /**
     * Set the LastNMinutes property.
     *
     * @param value The new value for LastNMinutes
     */
    public void setLastNMinutes(int value) {
        lastNMinutes = value;
        List adapters = getAdapters();
        if (adapters != null) {
            for (int i = 0; i < adapters.size(); i++) {
                ((TrackAdapter) adapters.get(i)).setLastNMinutes(value);
            }
        }

    }

    /**
     * Get the LastNMinutes property.
     *
     * @return The LastNMinutes
     */
    public int getLastNMinutes() {
        return lastNMinutes;
    }


}


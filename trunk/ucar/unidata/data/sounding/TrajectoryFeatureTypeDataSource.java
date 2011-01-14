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

package ucar.unidata.data.sounding;


import ucar.ma2.Range;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;

import ucar.unidata.data.*;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import visad.*;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Formatter;

import java.util.Hashtable;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Oct 5, 2009
 * Time: 2:47:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajectoryFeatureTypeDataSource extends TrackDataSource {

    /**
     * sonde locations
     * @deprecated use ID_SONDESTARTLOCATIONS
     */
    public static final String ID_SONDELOCATIONS = "id.sondelocations";

    /** sounding trace id */
    public static final String ID_SONDESTARTLOCATIONS =
        "id.sondestartlocations";

    /** sounding trace id */
    private static final String ID_SONDEENDLOCATIONS = "id.sondeendlocations";

    /** Default Constructor */
    public TrajectoryFeatureTypeDataSource() {}

    /** point data category */
    private List pointCats = Misc.newList(DataCategory.POINT_PLOT_CATEGORY);

    /** list of selected time index */
    private List selectTimes;

    /** _more_          */
    private boolean isCosmic = false;

    /**
     * Create a SondeDataSource from the specification given.
     *
     * @param descriptor    descriptor for the DataSource
     * @param source        file location or URL
     * @param properties    extra properties
     *
     * @throws VisADException  problem creating data
     */
    public TrajectoryFeatureTypeDataSource(DataSourceDescriptor descriptor,
                                           String source,
                                           Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(source), properties);
    }

    /**
     * Create a SondeDataSource from the specification given.
     *
     * @param descriptor    descriptor for the DataSource
     * @param sources        file location or URL
     * @param properties    extra properties
     *
     * @throws VisADException  problem creating data
     */
    public TrajectoryFeatureTypeDataSource(DataSourceDescriptor descriptor,
                                           List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, "Trajectory Soundings", properties);
    }


    /**
     * init sounding category
     */
    protected void initCategories() {
        if (traceCats == null) {
            super.initCategories();
            soundingCats =
                Misc.newList(DataCategory.TRAJECTORY_SOUNDING_CATEGORY);
        }
    }


    /**
     * Get the base time for the sondes
     *
     * @return base time
     */
    protected DateTime getBaseTime() {
        DateTime[] minMax = getMinMaxTime();
        if (minMax == null) {
            return null;
        }
        return minMax[0];
    }

    /**
     * Get the  times in the  tracks
     *
     * @return times of the tracks
     */
    protected DateTime[] getTimeList() {
        List adapters = getAdapters();
        if (adapters == null) {
            try {
                return new DateTime[] { new DateTime(), new DateTime() };
            } catch (Exception exc) {
                logException("Create null datetimes", exc);
                return null;
            }
        }
        DateTime[] timeList = new DateTime[adapters.size()];

        for (int i = 0; i < adapters.size(); i++) {
            TrackAdapter adapter = (TrackAdapter) getAdapters().get(i);
            DateTime     theTime = adapter.getEndTime();
            timeList[i] = theTime;
        }
        return timeList;
    }

    /**
     * Make the list of times associated with this DataSource for
     * DataSelection.
     *
     * @return DateTimes as a list.
     */
    protected List doMakeDateTimes() {
        DateTime[] timeList = getTimeList();

        if (timeList == null) {
            return new ArrayList();
        }
        return Misc.newList(timeList);
    }

    /**
     * Aggregate the tracks
     *
     * @param tracks List of sonde tracks
     * @param id0 _more_
     *
     * @return FieldImpl of aggregated tracks
     *
     * @throws java.rmi.RemoteException Java RMI Exception
     *
     * @throws RemoteException _more_
     * @throws VisADException  problem in VisAD
     */
    protected FieldImpl aggregateTracks(List tracks, Object id0)
            throws VisADException, RemoteException {

        List         adapters = getAdapters();
        FunctionType fiType   = null;
        DateTime[]   times    = new DateTime[tracks.size()];
        if (selectTimes == null) {
            for (int i = 0; i < tracks.size(); i++) {
                TrackAdapter adapter = (TrackAdapter) adapters.get(i);
                DateTime     time    = adapter.getEndTime();
                if (fiType == null) {
                    FieldImpl data = (FieldImpl) tracks.get(i);
                    fiType = new FunctionType(RealType.Time, data.getType());

                }
                times[i] = time;  //adapter.getStartTime();
            }
        } else {
            int len = selectTimes.size();
            times = new DateTime[len];
            for (int i = 0; i < len; i++) {
                Integer      ii      = (Integer) selectTimes.get(i);
                int          j       = ii.intValue();
                TrackAdapter adapter = (TrackAdapter) adapters.get(j);
                DateTime     time    = adapter.getEndTime();
                if (fiType == null) {
                    FieldImpl data = (FieldImpl) tracks.get(j);
                    fiType = new FunctionType(RealType.Time, data.getType());

                }
                times[i] = time;  //adapter.getStartTime();
            }
        }
        FieldImpl fi = new FieldImpl(fiType, DateTime.makeTimeSet(times));
        for (int i = 0; i < tracks.size(); i++) {
            FieldImpl data = (FieldImpl) tracks.get(i);
            fi.setSample(i, data, false);
        }
        return fi;
    }


    /**
     * Make the {@link ucar.unidata.data.DataChoice}s associated with this dataset
     */
    protected void doMakeDataChoices() {

        List sources = getSources();
        if (sources.size() == 0) {
            return;
        }
        String source = sources.get(0).toString();
        File   f      = new File(source);
        if (f.isDirectory()) {
            return;
        }
        // super.doMakeDataChoices();

        List adapters = getAdapters();
        if (adapters == null) {
            return;
        }
        initCategories();
        List trackInfos = null;
        try {
            trackInfos = getTraceAdapter().getTrackInfos();
        } catch (Exception e) {}

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

            // addDataChoice(new DirectDataChoice(this, new String[] { trackName,
            //         ID_LASTOB }, getDataChoiceLabel(ID_LASTOB),
            //                      getDataChoiceLabel(ID_LASTOB), pointCatList,
            //                      props));


        }

        if (f.getPath().contains("ionPrf")
                || f.getPath().contains("atmPhs")) {
            return;
        }

        //List       adapters       = getAdapters();
        if (isCosmic) {
            DataChoice soundingChoice = null;
            if ((adapters != null) && (adapters.size() > 1)) {
                TrackAdapter    adapter = (TrackAdapter) adapters.get(0);
                String          sName   = adapter.toString();
                List<TrackInfo> tinfo   = adapter.getTrackInfos();
                String          tName   = tinfo.get(0).trackName;
                String          id;
                List            category;
                if (sName.contains("wetPrf") || sName.contains("atmPrf")) {
                    id       = ID_SOUNDINGTRACE;
                    category = getSoundingCategories();
                } else {
                    id       = ID_SOUNDINGOB;
                    category = getRaobCategories();
                }

                soundingChoice = new CompositeDataChoice(this, id,
                        getDataChoiceLabel(id), getDataChoiceLabel(id),
                        category);
                for (int i = 0; i < adapters.size(); i++) {
                    TrackAdapter ta = (TrackAdapter) adapters.get(i);
                    TrackInfo trackInfo =
                        (TrackInfo) ta.getTrackInfos().get(0);
                    String trackName = trackInfo.getTrackName() + i;
                    // System.err.println("track name = " + trackName);
                    ((CompositeDataChoice) soundingChoice).addDataChoice(
                        new DirectDataChoice(
                            this, id, trackName, ("Sonde " + i), category));
                }
            } else {
                soundingChoice = new DirectDataChoice(this, ID_SOUNDINGTRACE,
                        getDataChoiceLabel(ID_SOUNDINGTRACE),
                        getDataChoiceLabel(ID_SOUNDINGTRACE),
                        getSoundingCategories());
            }
            addDataChoice(soundingChoice);




            List locCats = DataCategory.parseCategories("locations", false);
            //     addDataChoice(new DirectDataChoice(this, ID_SONDESTARTLOCATIONS,
            //                                        "Sonde Start Locations",
            //                                        "Sonde Start Locations", locCats));
            /*  End locations don't work now because lat/lon/alt values are NaN  */
            addDataChoice(new DirectDataChoice(this, ID_SONDEENDLOCATIONS,
                    "Sonde End Locations", "Sonde End Locations", locCats));
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

        TrajectoryFeatureTypeAdapter td =
            new TrajectoryFeatureTypeAdapter(this, file, pointDataFilter,
                                             stride, lastNMinutes);
        isCosmic = td.isCosmic();
        return td;

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
        Object id = getChoiceId(dataChoice);
        selectTimes = dataSelection.getTimes();
        if (id.equals(ID_SONDESTARTLOCATIONS)
                || id.equals(ID_SONDEENDLOCATIONS)) {
            List   adapters = getAdapters();
            String name     = id.equals(ID_SONDEENDLOCATIONS)
                              ? "Sonde End Locations"
                              : "Sonde Start Locations";
            StringBuffer xml = new StringBuffer("<stationtable name=\""
                                   + name + "\">\n");
            if (adapters != null) {
                for (int i = 0; i < adapters.size(); i++) {
                    TrajectoryFeatureTypeAdapter cta =
                        (TrajectoryFeatureTypeAdapter) adapters.get(i);
                    List infos = cta.getTrackInfos();

                    if (infos.size() == 0) {
                        continue;
                    }
                    TrackInfo cfti;

                    if (isCosmic) {
                        cfti = (CosmicTrajectoryFeatureTypeInfo) infos.get(0);
                    } else {
                        cfti = (CDMTrajectoryFeatureTypeInfo) infos.get(0);
                    }

                    int numObs = 0;
                    try {
                        numObs = cfti.getNumberPoints();  //trackInfo.getNumberPoints();
                        Range    range = cfti.getFullRange();
                        double[] times = cfti.getTimeVals(range);
                        //   trackInfo.getTimeVals(todt.getFullRange());
                        boolean ascendingTimes = (times[0]
                                                  < times[times.length - 1]);
                        int rVal;
                        if (id.equals(ID_SONDEENDLOCATIONS)) {
                            rVal = (ascendingTimes)
                                   ? times.length - 1
                                   : 0;
                        } else {
                            rVal = (ascendingTimes)
                                   ? 0
                                   : times.length - 1;
                        }
                        Range r = new Range(rVal, rVal);
                        String trueStartDate =
                            ((cfti.getStartTime().getValue()
                              > cfti.getEndTime().getValue())
                             ? cfti.getEndTime().formattedString("HH:mm",
                                 DateTime.DEFAULT_TIMEZONE)
                             : cfti.getStartTime().formattedString("HH:mm",
                                 DateTime.DEFAULT_TIMEZONE));

                        //  String time = new DateTime(
                        //                    trueStartDate).formattedString(
                        //                    "HH:mm", DateTime.DEFAULT_TIMEZONE);
                        xml.append("<station "
                                + XmlUtil.attr("id", trueStartDate)
                                + XmlUtil.attr("name",
                                    IOUtil.getFileTail(cta.getFilename())) + XmlUtil.attr("lat",
                                        "" + cfti.getLatitude(r)[0]) + XmlUtil.attr("lon",
                                            "" + cfti.getLongitude(r)[0]) + XmlUtil.attr("elev",
                                                "" + cfti.getAltitude(r)[0]) + XmlUtil.attr("time",
                                                    trueStartDate) + "/>");
                    } catch (Exception exc) {
                        throw new IllegalStateException(
                            "Got error creating sonde positions " + exc);

                    }
                }
            }

            xml.append("</stationtable>");
            return new visad.Text(xml.toString());
        } else {

            return super.getDataInner(dataChoice, category, dataSelection,
                                      requestProperties);

        }
    }



}

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

package ucar.unidata.data.storm;


import ucar.unidata.data.*;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.DateUtil;

import ucar.visad.Util;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;


import java.rmi.RemoteException;

import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 4:57:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class StormDataSource extends DataSourceImpl {


    /** _more_ */
    public static final int CATEGORY_DB = 0;  // - disturbance,

    /** _more_ */
    public static final int CATEGORY_TD = 1;  // - tropical depression,

    /** _more_ */
    public static final int CATEGORY_TS = 2;  // - tropical storm,

    /** _more_ */
    public static final int CATEGORY_TY = 3;  // - typhoon,

    /** _more_ */
    public static final int CATEGORY_ST = 4;  // - super typhoon,

    /** _more_ */
    public static final int CATEGORY_TC = 5;  // - tropical cyclone,

    /** _more_ */
    public static final int CATEGORY_HU = 6;  // - hurricane,

    /** _more_ */
    public static final int CATEGORY_SD = 7;  // - subtropical depression,

    /** _more_ */
    public static final int CATEGORY_SS = 8;  // - subtropical storm,

    /** _more_ */
    public static final int CATEGORY_EX = 9;  // - extratropical systems,

    /** _more_ */
    public static final int CATEGORY_IN = 10;  // - inland,

    /** _more_ */
    public static final int CATEGORY_DS = 11;  // - dissipating,

    /** _more_ */
    public static final int CATEGORY_LO = 12;  // - low,

    /** _more_ */
    public static final int CATEGORY_WV = 13;  // - tropical wave,

    /** _more_ */
    public static final int CATEGORY_ET = 14;  // - extrapolated,

    /** _more_ */
    public static final int CATEGORY_XX = 15;  // - unknown.

    /** _more_ */
    // public static StormParam PARAM_DISTANCEERROR;

    /** _more_ */
    public static StormParam PARAM_MINPRESSURE;

    /** _more_ */
    public static StormParam PARAM_MAXWINDSPEED_KTS;


    /** _more_ */
    public static final int[] CATEGORY_VALUES = {
        CATEGORY_DB, CATEGORY_TD, CATEGORY_TS, CATEGORY_TY, CATEGORY_ST,
        CATEGORY_TC, CATEGORY_HU, CATEGORY_SD, CATEGORY_SS, CATEGORY_EX,
        CATEGORY_IN, CATEGORY_DS, CATEGORY_LO, CATEGORY_WV, CATEGORY_ET,
        CATEGORY_XX
    };

    /** _more_ */
    public static final String[] CATEGORY_NAMES = {
        "DB", "TD", "TS", "TY", "ST", "TC", "HU", "SD", "SS", "EX", "IN",
        "DS", "LO", "WV", "ET", "XX"
    };

    /** _more_ */
    public static final String ATTR_CATEGORY = "attr.category";


    /** _more_ */
    public static StormParam PARAM_STORMCATEGORY;

    /** _more_ */
    protected StormParam[] obsParams;

    /** _more_ */
    protected StormParam[] forecastParams;



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public StormDataSource() throws Exception {}

    /**
     * _more_
     *
     * @param descriptor _more_
     * @param name _more_
     * @param description _more_
     * @param properties _more_
     */
    public StormDataSource(DataSourceDescriptor descriptor, String name,
                           String description, Hashtable properties) {
        super(descriptor, name, description, properties);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEditable() {
        return false;
    }

    /**
     * _more_
     *
     * @param dataChoice _more_
     *
     * @return _more_
     */
    public boolean canAddCurrentName(DataChoice dataChoice) {
        return false;
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param alias _more_
     * @param unit _more_
     *
     * @return _more_
     */
    protected static RealType makeRealType(String id, String alias,
                                           Unit unit) {
        try {
            alias = alias + "[unit:" + ((unit == null)
                                        ? "null"
                                        : DataUtil.cleanName(
                                            unit.toString())) + "]";
            return ucar.visad.Util.makeRealType(id, alias, unit);
        } catch (VisADException exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     */
    protected final void initAfter() {
        try {
            incrOutstandingGetDataCalls();
            initializeStormData();
        } finally {
            decrOutstandingGetDataCalls();
        }
    }


    /**
     * _more_
     */
    protected void initializeStormData() {}



    /**
     * _more_
     *
     * @throws VisADException _more_
     */
    protected void initParams() throws VisADException {
        if (PARAM_STORMCATEGORY == null) {
            PARAM_STORMCATEGORY =
                new StormParam(Util.makeRealType("stormcategory",
                    "Storm_Category", null));
            PARAM_MINPRESSURE = new StormParam(makeRealType("minpressure",
                    "Min_Pressure", DataUtil.parseUnit("mb")));
            //  PARAM_DISTANCEERROR =
            //      new StormParam(Util.makeRealType("forecastlocationerror",
            //           "Distance_Error", Util.parseUnit("km")), true,
            //              false);
            PARAM_MAXWINDSPEED_KTS =
                new StormParam(makeRealType("maxwindspeedkts",
                                            "Max_Windspeed",
                                            DataUtil.parseUnit("kts")));

        }
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public int getCategory(String name) {
        if (name == null) {
            return CATEGORY_XX;
        }
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            if (name.equals(CATEGORY_NAMES[i])) {
                return CATEGORY_VALUES[i];
            }
        }
        return CATEGORY_XX;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public abstract List<StormInfo> getStormInfos();

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract String getId();


    /**
     * _more_
     */
    protected void doMakeDataChoices() {
        List cats = DataCategory.parseCategories("stormtrack", false);
        DataChoice choice = new DirectDataChoice(this, "stormtrack",
                                "Storm Track", "Storm Track", cats,
                                (Hashtable) null);
        addDataChoice(choice);

    }

    /**
     * Re-initialize the storm data.
     */
    public void reloadData() {
        initializeStormData();
        super.reloadData();
    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param waysToUse _more_
     * @param obsWay _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StormTrackCollection getTrackCollection(StormInfo stormInfo,
            Hashtable<String, Boolean> waysToUse, Way obsWay)
            throws Exception {

        try {
            incrOutstandingGetDataCalls();
            return getTrackCollectionInner(stormInfo, waysToUse, obsWay);
        } finally {
            decrOutstandingGetDataCalls();
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getWayName() {
        return "Way";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getWaysName() {
        return getWayName() + "s";
    }

    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param waysToUse _more_
     * @param observationWay _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */

    public abstract StormTrackCollection getTrackCollectionInner(
            StormInfo stormInfo, Hashtable<String, Boolean> waysToUse,
            Way observationWay)
     throws Exception;


    /** _more_ */
    private Hashtable seenWays = new Hashtable();

    /** _more_ */
    private List<Way> ways = new ArrayList();

    /** _more_ */
    private Hashtable<String, Way> wayMap = new Hashtable<String, Way>();


    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected Way addWay(Way way) {
        if (seenWays.get(way) == null) {
            seenWays.put(way, way);
            ways.add(way);
        }
        return way;
    }




    /**
     * _more_
     *
     * @param w _more_
     * @param name _more_
     *
     * @return _more_
     */
    protected Way getWay(String w, String name) {
        Way way = wayMap.get(w);
        if (way == null) {
            way = new Way(w, name);
            wayMap.put(w, way);
        }
        addWay(way);
        return way;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List<Way> getWays() {
        return new ArrayList<Way>(ways);
    }

    /**
     * _more_
     *
     * @param stormId _more_
     *
     * @return _more_
     */
    public StormInfo getStormInfo(String stormId) {
        List<StormInfo> stormInfos = getStormInfos();
        for (StormInfo sInfo : stormInfos) {
            if (sInfo.getStormId().equals(stormId)) {
                return sInfo;
            }
        }
        return null;
    }


    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public static int getYear(DateTime dttm) throws VisADException {
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        cal.setTime(ucar.visad.Util.makeDate(dttm));
        return cal.get(Calendar.YEAR);
    }


    /**
     * _more_
     *
     * @param obsTrack _more_
     * @param fctTrack _more_
     *
     * @throws VisADException _more_
     */
    public static void addDistanceError(StormTrack obsTrack,
                                        StormTrack fctTrack)
            throws VisADException {
        List<StormTrackPoint> obsTrackPoints = obsTrack.getTrackPoints();
        List<StormTrackPoint> fctTrackPoints = fctTrack.getTrackPoints();

        for (StormTrackPoint stp : fctTrackPoints) {
            DateTime dt = stp.getTime();
            //  StormTrackPoint stpObs = getClosestPoint(obsTrackPoints, dt);
            //  double          der    = getDistance(stpObs, stp);
            //  stp.addAttribute(PARAM_DISTANCEERROR.getReal(der));
        }

    }


    /**
     * _more_
     *
     * @param obsTrack _more_
     * @param fctTrack _more_
     * @param param _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static StormTrack difference(StormTrack obsTrack,
                                        StormTrack fctTrack, StormParam param)
            throws VisADException, RemoteException {
        List<StormTrackPoint> obsTrackPoints = obsTrack.getTrackPoints();
        List<StormTrackPoint> fctTrackPoints = fctTrack.getTrackPoints();
        List<StormTrackPoint> diffPoints = new ArrayList<StormTrackPoint>();

        for (StormTrackPoint forecastPoint : fctTrackPoints) {
            Real forecastValue = forecastPoint.getAttribute(param);
            if (forecastValue == null) {
                continue;
            }
            DateTime forecastDttm = forecastPoint.getTime();
            StormTrackPoint[] range = getClosestPointRange(obsTrackPoints,
                                          forecastDttm);
            if (range == null) {
                continue;
            }
            Real obsValue = null;
            if (range.length == 1) {
                //exact match:
                obsValue = range[0].getAttribute(param);
            } else {
                //Interpolate between the two points
                Real v1 = range[0].getAttribute(param);
                Real v2 = range[1].getAttribute(param);
                if ((v1 == null) || (v2 == null)) {
                    continue;
                }
                DateTime t1 = range[0].getTime();
                DateTime t2 = range[1].getTime();
                double percent = forecastDttm.getValue()
                                 - t1.getValue()
                                   / (t2.getValue() - t1.getValue());

                double interpolatedValue = v2.getValue()
                                           + percent
                                             * (v2.getValue()
                                                - v1.getValue());
                obsValue = v1.cloneButValue(interpolatedValue);
                System.err.println("interp %:" + percent + " v:" + obsValue
                                   + " v1:" + v1 + " v2:" + v2 + "\n\tt1:"
                                   + t1 + " t2:" + t2);
            }

            if (obsValue == null) {
                continue;
            }

            Real difference = (Real) forecastValue.__sub__(obsValue);
            StormTrackPoint newStormTrackPoint =
                new StormTrackPoint(forecastPoint.getLocation(),
                                    forecastDttm,
                                    forecastPoint.getForecastHour(),
                                    new ArrayList<Real>());
            newStormTrackPoint.addAttribute(difference);
            diffPoints.add(newStormTrackPoint);
        }
        if (diffPoints.size() == 0) {
            return null;
        }
        return new StormTrack(fctTrack.getStormInfo(), fctTrack.getWay(),
                              diffPoints, null);
    }

    /**
     * _more_
     *
     * @param aList _more_
     * @param dt _more_
     *
     * @return _more_
     */
    public static StormTrackPoint[] getClosestPointRange(
            List<StormTrackPoint> aList, DateTime dt) {
        double timeToLookFor = dt.getValue();
        int    numPoints     = aList.size();
        double lastTime      = -1;

        for (int i = 0; i < numPoints; i++) {
            StormTrackPoint stp         = aList.get(i);
            double          currentTime = stp.getTime().getValue();
            if (timeToLookFor == currentTime) {
                return new StormTrackPoint[] { stp };
            }
            if (timeToLookFor < currentTime) {
                if (i == 0) {
                    return null;
                }
                if (timeToLookFor > lastTime) {
                    return new StormTrackPoint[] { aList.get(i - 1), stp };
                }
            }
            lastTime = currentTime;
        }
        return null;
    }


    /**
     * _more_
     *
     * @param aList _more_
     * @param dt _more_
     *
     * @return _more_
     */
    public static StormTrackPoint getClosestPoint(
            List<StormTrackPoint> aList, DateTime dt) {


        int             numPoints    = aList.size();
        StormTrackPoint stp1         = aList.get(0);
        StormTrackPoint stp2         = aList.get(numPoints - 1);

        double          pValue       = dt.getValue();
        double          minDiffLeft  = 200000;
        double          minDiffRight = 200000;

        for (int i = 0; i < numPoints; i++) {
            StormTrackPoint stp11   = aList.get(i);
            StormTrackPoint stp21   = aList.get(numPoints - i - 1);

            double          p1Value = stp11.getTime().getValue();
            double          p2Value = stp21.getTime().getValue();
            double          diff1   = Math.abs(p1Value - pValue);
            double          diff2   = Math.abs(p2Value - pValue);

            if ((pValue >= p1Value) && (diff1 < minDiffRight)) {
                if (pValue == p1Value) {
                    return stp11;
                }
                stp1         = stp11;
                minDiffRight = diff1;

            }

            if ((pValue <= p2Value) && (diff2 < minDiffLeft)) {
                if (pValue == p2Value) {
                    return stp21;
                }
                stp2        = stp21;
                minDiffLeft = diff2;
            }

        }

        double        diff = minDiffLeft + minDiffRight;
        EarthLocation el1  = stp1.getLocation();
        EarthLocation el2  = stp1.getLocation();

        double lat = ((diff - minDiffLeft) * el1.getLatitude().getValue()
                      + (diff - minDiffRight)
                        * el2.getLatitude().getValue()) / diff;
        double lon = ((diff - minDiffLeft) * el1.getLongitude().getValue()
                      + (diff - minDiffRight)
                        * el2.getLongitude().getValue()) / diff;

        EarthLocation el = new EarthLocationLite(new Real(RealType.Latitude,
                               lat), new Real(RealType.Longitude, lon), null);

        return new StormTrackPoint(el, dt, 0, null);


    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsObservationWayChangeable() {
        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Way getDefaultObservationWay() {
        return null;
    }

    /**
     * _more_
     *
     * @param p1 _more_
     * @param p2 _more_
     *
     * @return _more_
     */
    public static double getDistance(StormTrackPoint p1, StormTrackPoint p2) {


        EarthLocation el1 = p1.getLocation();
        EarthLocation el2 = p2.getLocation();


        Bearing b = Bearing.calculateBearing(el1.getLatitude().getValue(),
                                             el1.getLongitude().getValue(),
                                             el2.getLatitude().getValue(),
                                             el2.getLongitude().getValue(),
                                             null);
        return b.getDistance();

    }
}

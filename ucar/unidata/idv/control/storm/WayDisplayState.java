/**
 * $Id: TrackControl.java,v 1.69 2007/08/21 11:32:08 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.control.storm;


import ucar.unidata.data.grid.GridUtil;


import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;



import ucar.unidata.data.storm.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.FlatEarth;
import ucar.unidata.geoloc.projection.LatLonProjection;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.ui.colortable.ColorTableManager;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;

import ucar.visad.display.*;

import visad.*;

import visad.bom.Radar2DCoordinateSystem;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;

import java.awt.*;

import java.awt.Color;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class WayDisplayState {


    /** Type for Azimuth */
    private final RealType azimuthType = RealType.getRealType("Azimuth",
                                             CommonUnit.degree);


    private Way way;

    /** _more_ */
    private StormDisplayState stormDisplayState;

    private DisplayState trackState;

    private DisplayState coneState;
    private DisplayState wayState;

    /** _more_ */
    private DisplayState ringsState;


    /** _more_          */
    List<PointOb> pointObs = new ArrayList<PointOb>();



    /** _more_ */
    private List<StormTrack> tracks = new ArrayList<StormTrack>();


    /** _more_ */
    private List<DateTime> times = new ArrayList<DateTime>();





    /** _more_ */
    private Color color;

    /** _more_ */
    private GuiUtils.ColorSwatch colorSwatch;

    /** _more_ */
    private CompositeDisplayable holder;

    /** _more_          */
    private StationModelDisplayable labelDisplay;


    /** _more_ */
    private TrackDisplayable trackDisplay;

    /** _more_ */
    private TrackDisplayable ringsDisplay;

    /** _more_ */
    private CompositeDisplayable conesHolder;


    /** _more_          */
    private List<StormParam> coneParams;

    /** _more_          */
    private StormParam ringsParam;

    /** _more_          */
    private StormParam colorParam;






    public WayDisplayState() {}


    /**
     * _more_
     *
     *
     * @param stormDisplayState _more_
     * @param way _more_
     */
    public WayDisplayState(StormDisplayState stormDisplayState, Way way) {
        this.stormDisplayState = stormDisplayState;
        this.way               = way;
        wayState = new DisplayState(this, "Show/Hide All", true);
        trackState = new DisplayState(this,"Show/Hide Track", true);
        coneState = new DisplayState(this,"Show/Hide Cone", false);
        ringsState = new DisplayState(this,"Show/Hide Rings", false);
    }




    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected CompositeDisplayable getHolder()
            throws VisADException, RemoteException {
        if (holder == null) {
            holder = new CompositeDisplayable("way  holder");
            stormDisplayState.addDisplayable(holder);
        }
        return holder;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasTrackDisplay() {
        return trackDisplay != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasLabelDisplay() {
        return labelDisplay != null;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasRingsDisplay() {
        return ringsDisplay != null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasConeDisplay() {
        return conesHolder != null;
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void updateDisplay() throws Exception {
        //        FieldImpl field = makeTrackField(obsTrack, null);
        //        obsDisplayState.addTrack(obsTrack, field);
        //        obsDisplayState.getTrackDisplay().setTrack(field);


        if ( !shouldShow()) {
            if (holder != null) {
                holder.setVisible(false);
            }
            return;
        }

        //        System.err.println (way + " " + shouldShow() + " " + shouldShowTrack() + " " + shouldShowCone());
        getHolder().setVisible(true);

        if (shouldShowTrack()) {
            StormParam tmp = stormDisplayState.getColorParam(this);

            if ( !hasTrackDisplay() || !Misc.equals(colorParam, tmp)) {
                boolean hadTrack = hasTrackDisplay();
                colorParam = tmp;
                FieldImpl trackField = makeTrackField();
                if (trackField != null) {
                    getTrackDisplay().setTrack(trackField);
                    Range[] range = GridUtil.getMinMax(trackField);
                    getTrackDisplay().setRangeForColor(range[0].getMin(),
                            range[0].getMax());


                }
                setTrackColor();
                if ( !hadTrack && way.isObservation()) {
                    getLabelDisplay().setStationData(
                        PointObFactory.makeTimeSequenceOfPointObs(
                            pointObs, -1, -1));
                }
            }
            getTrackDisplay().setVisible(true);
        } else {
            if (hasTrackDisplay()) {
                getTrackDisplay().setVisible(false);
            }
            if (hasLabelDisplay()) {
                getLabelDisplay().setVisible(false);
            }
        }



        if (shouldShowCone()) {
            List<StormParam> tmp = stormDisplayState.getConeParams(this);
            if ( !hasConeDisplay() || !Misc.equals(tmp, coneParams)) {
                this.coneParams = tmp;
                getConesHolder().clearDisplayables();
                setConeColor();
                for (StormParam param : coneParams) {
                    TrackDisplayable coneDisplay = makeConeDisplay(param);
                    if (coneDisplay != null) {
                        getConesHolder().addDisplayable(coneDisplay);
                    }
                }
                setConeColor();
            }
            getConesHolder().setVisible(true);
        } else {
            if (hasConeDisplay()) {
                getConesHolder().setVisible(false);
            }
        }


        if (shouldShowRings()) {
            StormParam tmp = stormDisplayState.getRingsParam(this);
            if ( !hasRingsDisplay() || !Misc.equals(tmp, ringsParam)) {
                this.ringsParam = tmp;
                setRingsColor();
                FieldImpl field = makeRingsField(ringsParam);
                if (field == null) {
                    getRingsDisplay().setData(new Real(0));
                } else {
                    getRingsDisplay().setTrack(field);
                }
                setRingsColor();
            }
            getRingsDisplay().setVisible(true);
        } else {
            if (hasRingsDisplay()) {
                getRingsDisplay().setVisible(false);
            }
        }


    }





    /**
     * _more_
     *
     * @return _more_
     */
    public boolean shouldShow() {
        if (tracks.size() == 0) {
            return false;
        }
        if ( !way.isObservation()
                && !stormDisplayState.getForecastState().getWayState()
                    .getVisible()) {
            return false;
        }
        //        return visible;
        return wayState.getVisible();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean shouldShowTrack() {
        if ( !way.isObservation()
                && !stormDisplayState.getForecastState().getTrackState()
                    .getVisible()) {
            return false;
        }
        return shouldShow() && trackState.getVisible();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean shouldShowRings() {
        if ( !way.isObservation()
                && !stormDisplayState.getForecastState().getRingsState()
                    .getVisible()) {
            return false;
        }
        return shouldShow() && ringsState.getVisible();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean shouldShowCone() {
        if ( !way.isObservation()
                && !stormDisplayState.getForecastState().getConeState()
                    .getVisible()) {
            return false;
        }
        return shouldShow() && coneState.getVisible();
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StationModelDisplayable getLabelDisplay() throws Exception {
        if (labelDisplay == null) {
            labelDisplay = new StationModelDisplayable("dots");
            addDisplayable(labelDisplay);
            labelDisplay.setScale(
                stormDisplayState.getStormTrackControl().getDisplayScale());
            labelDisplay.setStationModel(
                stormDisplayState.getObservationStationModel());
            setLabelColor();
        }
        return labelDisplay;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TrackDisplayable getTrackDisplay() throws Exception {
        if (trackDisplay == null) {
            trackDisplay = new TrackDisplayable("track_"
                    + stormDisplayState.getStormInfo().getStormId());
            if (way.isObservation()) {
                trackDisplay.setLineWidth(3);
            } else {
                trackDisplay.setUseTimesInAnimation(false);
            }
            setTrackColor();
            addDisplayable(trackDisplay);
        }
        return trackDisplay;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public CompositeDisplayable getConesHolder() throws Exception {
        if (conesHolder == null) {
            conesHolder = new CompositeDisplayable("cone_"
                    + stormDisplayState.getStormInfo().getStormId());
            conesHolder.setUseTimesInAnimation(false);
            addDisplayable(conesHolder);
        }
        return conesHolder;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TrackDisplayable getRingsDisplay() throws Exception {
        if(ringsDisplay == null) {
            ringsDisplay =
                new TrackDisplayable(
                                     "ring_" + stormDisplayState.getStormInfo().getStormId()+"_"+getWay());
            ringsDisplay.setVisible(true);
            ringsDisplay.setUseTimesInAnimation(false);
            addDisplayable(ringsDisplay);
        }
        return ringsDisplay;
    }




    /**
     * _more_
     *
     * @param param _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TrackDisplayable makeConeDisplay(StormParam param)
            throws Exception {
        FieldImpl field = makeConeField(param);
        if (field == null) {
            return null;
        }
        TrackDisplayable coneDisplay =
            new TrackDisplayable(
                "cone_" + stormDisplayState.getStormInfo().getStormId());
        coneDisplay.setUseTimesInAnimation(false);
        coneDisplay.setTrack(field);
        coneDisplay.setUseTimesInAnimation(false);
        return coneDisplay;
    }








    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getColorSwatch() {
        if (colorSwatch == null) {
            colorSwatch = new GuiUtils.ColorSwatch(getColor(),
                    "Set track color") {
                public void setBackground(Color newColor) {
                    super.setBackground(newColor);
                    WayDisplayState.this.color = newColor;
                    try {
                        setTrackColor();
                        setConeColor();
                        setRingsColor();
                        setLabelColor();
                    } catch (Exception exc) {
                        LogUtil.logException("Setting color", exc);
                    }
                }
            };
            colorSwatch.setMinimumSize(new Dimension(15, 15));
            colorSwatch.setPreferredSize(new Dimension(15, 15));
        }
        return colorSwatch;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float[][] getColorPalette() {
        if (colorParam != null) {
            ColorTable ct =
                stormDisplayState.getStormTrackControl().getColorTable();
            //            System.err.println("Using:" + ct);
            //                stormDisplayState.getStormTrackControl().getControlContext()
            //                    .getColorTableManager().getColorTable(colorTable);
            if (ct != null) {
                return stormDisplayState.getStormTrackControl()
                    .getColorTableForDisplayable(ct);
            }
        }
        return getColorPalette(getColor());
    }



    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static float[][] getColorPalette(Color c) {
        if (c == null) {
            c = Color.red;
        }
        return ColorTableDefaults.allOneColor(c, true);
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void setTrackColor() throws Exception {
        if (trackDisplay != null) {
            trackDisplay.setColorPalette(getColorPalette());
        }

    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void setLabelColor() throws Exception {
        if (labelDisplay != null) {
            labelDisplay.setColor(getColor());
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void setConeColor() throws Exception {
        if (conesHolder != null) {
            conesHolder.setColorPalette(getColorPalette(getColor()));
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void setRingsColor() throws Exception {
        if (ringsDisplay != null) {
            ringsDisplay.setColor(getColor());
        }
    }



    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void deactivate() throws VisADException, RemoteException {
        ringsDisplay = null;
        conesHolder = null;
        if (holder != null) {}
        trackDisplay = null;
        holder       = null;
        tracks       = new ArrayList<StormTrack>();
        times        = new ArrayList<DateTime>();
    }




    /** _more_ */
    private static TextType textType;

    /**
     * _more_
     *
     * @param track _more_
     *
     * @throws Exception _more_
     */
    public void addTrack(StormTrack track) throws Exception {
        tracks.add(track);
    }



    /**
     * _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected FieldImpl makeTrackField() throws Exception {
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        List<DateTime>  times  = new ArrayList<DateTime>();

        pointObs = new ArrayList<PointOb>();
        for (StormTrack track : tracks) {
            FieldImpl field =
                stormDisplayState.getStormTrackControl().makeTrackField(
                    track, colorParam);
            if (field == null) {
                continue;
            }
            fields.add(field);
            times.add(track.getStartTime());
            pointObs.addAll(makePointObs(track));
        }

        if (fields.size() == 0) {
            return null;
        }
        //        if(fields.size()==1) return fields.get(0);
        return Util.makeTimeField(fields, times);
    }






    /**
     * _more_
     *
     *
     * @param stormParam _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected FieldImpl makeConeField(StormParam stormParam)
            throws Exception {
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        List<DateTime>  times  = new ArrayList<DateTime>();
        for (StormTrack track : tracks) {
            StormTrack coneTrack = makeConeTrack(track, stormParam);
            fields.add(
                stormDisplayState.getStormTrackControl().makeTrackField(
                    coneTrack, null));
            times.add(track.getStartTime());
        }

        if (fields.size() == 0) {
            return null;
        }
        return Util.makeTimeField(fields, times);
    }


    /**
     * _more_
     *
     * @param track _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    List<PointOb> makePointObs(StormTrack track) throws Exception {
        boolean               isObservation = way.isObservation();
        DateTime              startTime     = track.getStartTime();
        List<StormTrackPoint> stps          = track.getTrackPoints();
        if (textType == null) {
            textType = new TextType("label");
        }
        List<PointOb> pointObs = new ArrayList<PointOb>();
        for (int i = 0; i < stps.size(); i++) {
            StormTrackPoint stp   = stps.get(i);
            DateTime        time  = startTime;
            String          label = "";
            if (isObservation) {
                time = stp.getTime();
            } else {
                if (i == 0) {
                    label = way + ": " + track.getStartTime();
                } else {
                    label = "" + stp.getForecastHour() + "H";
                }
            }
            Tuple tuple = new Tuple(new Data[] {
                              new visad.Text(textType, label) });
            pointObs.add(PointObFactory.makePointOb(stp.getLocation(), time,
                    tuple));
        }
        return pointObs;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormTrack> getTracks() {
        return tracks;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<DateTime> getTimes() {
        return times;
    }

    /**
     * _more_
     *
     * @param displayable _more_
     *
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void addDisplayable(Displayable displayable)
            throws VisADException, RemoteException {
        getHolder().addDisplayable(displayable);
    }






    /**
     * Set the ConeState property.
     *
     * @param value The new value for ConeState
     */
    public void setConeState(DisplayState value) {
        coneState = value;
    }

    /**
     * Get the ConeState property.
     *
     * @return The ConeState
     */
    public DisplayState getConeState() {
        return coneState;
    }




    /**
     * Set the TrackState property.
     *
     * @param value The new value for TrackState
     */
    public void setTrackState(DisplayState value) {
        trackState = value;
    }

    /**
     * Get the TrackState property.
     *
     * @return The TrackState
     */
    public DisplayState getTrackState() {
        return trackState;
    }



    /**
     * Set the RingsState property.
     *
     * @param value The new value for RingsState
     */
    public void setRingsState(DisplayState value) {
        ringsState = value;
    }

    /**
     * Get the RingsState property.
     *
     * @return The RingsState
     */
    public DisplayState getRingsState() {
        return ringsState;
    }



    /**
     * Set the WayState property.
     *
     * @param value The new value for WayState
     */
    public void setWayState(DisplayState value) {
        wayState = value;
    }

    /**
     * Get the WayState property.
     *
     * @return The WayState
     */
    public DisplayState getWayState() {
        return wayState;
    }









    /**
     * Set the Color property.
     *
     * @param value The new value for Color
     */
    public void setColor(Color value) {
        color = value;
    }

    /**
     * Get the Color property.
     *
     * @return The Color
     */
    public Color getColor() {
        return color;
    }


    /**
     * Set the StormDisplayState property.
     *
     * @param value The new value for StormDisplayState
     */
    public void setStormDisplayState(StormDisplayState value) {
        stormDisplayState = value;
    }

    /**
     * Get the StormDisplayState property.
     *
     * @return The StormDisplayState
     */
    public StormDisplayState getStormDisplayState() {
        return stormDisplayState;
    }



    /**
     * Set the Way property.
     *
     * @param value The new value for Way
     */
    public void setWay(Way value) {
        way = value;
    }

    /**
     * Get the Way property.
     *
     * @return The Way
     */
    public Way getWay() {
        return way;
    }


    /**
     * Set the ColorTable property.
     *
     * @param value The new value for ColorTable
     */
    public void setColorTable(String value) {}



    /**
     * _more_
     *
     * @param track _more_
     * @param param _more_
     *
     * @return _more_
     */
    private List<StormTrackPoint> getRealTrackPoints(StormTrack track,
            StormParam param) {
        List<StormTrackPoint> newStps = new ArrayList();
        List<StormTrackPoint> stps    = track.getTrackPoints();

        newStps.add(stps.get(0));
        Iterator<StormTrackPoint> it = stps.iterator();

        while (it.hasNext()) {
            StormTrackPoint stp = it.next();
            if (stp.getAttribute(param) != null) {
                newStps.add(stp);
            }
        }
        return newStps;
    }

    protected FieldImpl makeRingsField(StormParam stormParam)
            throws Exception {
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        List<DateTime>  times  = new ArrayList<DateTime>();
        for (StormTrack track : tracks) {
            FieldImpl ringField = makeRingTracks(track, stormParam);
            fields.add(ringField); 
            times.add(track.getStartTime());
        }

        if (fields.size() == 0) {
            return null;
        }
        return Util.makeTimeField(fields, times);
    }


    /**
      * _more_
      *
      * @param track _more_
      * @param param _more_
      *
      * @return _more_
      *
      * @throws VisADException _more_
      */
     public FieldImpl makeRingTracks(StormTrack track, StormParam param)
             throws Exception {
        List<StormTrackPoint> stps  = getRealTrackPoints(track,
                                                         param);
        List<StormTrack> stracks = new ArrayList();
        int size = stps.size();
        DateTime       dt    = stps.get(0).getTime();
        Way ringWay = new Way(getWay()+"_RING");
        int  numberOfPoints = 73;
        double angleDelta =  360.0/numberOfPoints;
        for(int i= 0; i < size; i++) {
            StormTrackPoint       stp         = stps.get(i);
            Real           r      = stp.getAttribute(param);
            if(r != null) {
                double rr = r.getValue();
                double azi = 0.0;
                List ringList = new ArrayList<StormTrackPoint>();
                for (int j = 0; j < numberOfPoints; j++) {
                    ringList.add(getCirclePoint(stp, rr, azi, dt));
                    azi = azi + angleDelta;
                 }
                 stracks.add(new StormTrack(track.getStormInfo(), ringWay,
                                            ringList,null));
            }
        }

        Data[] datas= new Data[stracks.size()];
        int i=0;
        for(StormTrack ringTrack: stracks){
             datas[i++] =  stormDisplayState.getStormTrackControl().makeTrackField(
                                                                                   ringTrack, null);
        }
        return Util.indexedField(datas, false);
     }

     public StormTrackPoint getCirclePoint(StormTrackPoint stp, double r0,
            double azimuth, DateTime dt)
            throws VisADException {
        //

        EarthLocation el = stp.getLocation();
        double lat0 = el.getLatitude().getValue();
        double lon0 = el.getLongitude().getValue();
        //DateTime dt = stp.getTime();
        LatLonPointImpl lp = Bearing.findPoint(lat0, lon0, azimuth, r0, null);

        EarthLocation el1 = new EarthLocationLite(lp.getLatitude(),
                                   lp.getLongitude(), 0);
        StormTrackPoint stp1 = new StormTrackPoint(el1, dt, 0, null);


        return stp1;
    }
    /**
     * _more_
     *
     * @param track _more_
     * @param param _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public StormTrack makeConeTrack(StormTrack track, StormParam param)
            throws VisADException {
        List<StormTrackPoint> stps          = getRealTrackPoints(track,
                                                  param);
        int                   size          = stps.size();
        int                   numberOfPoint = size * 2 + 11;
        StormTrackPoint[]     conePoints = new StormTrackPoint[numberOfPoint];

        StormTrackPoint       stp1          = stps.get(0);
        conePoints[0]                 = stp1;  // first point  & last point
        conePoints[numberOfPoint - 1] = stp1;

        StormTrackPoint stp2;
        StormTrackPoint stp;

        // circle  1 to n

        for (int i = 1; i < size; i++) {
            stp2 = stps.get(i);
            //right point
            stp = getPointToCircleTangencyPoint(stp1, stp2, param, true);
            conePoints[i] = stp;
            //left point
            stp = getPointToCircleTangencyPoint(stp1, stp2, param, false);
            conePoints[numberOfPoint - i - 1] = stp;
            stp1                              = stp2;
        }

        // end point half circle take 11 points
        StormTrackPoint last   = stps.get(size - 1);
        EarthLocation   lastEl = last.getLocation();
        EarthLocation   endEl  = conePoints[size - 1].getLocation();
        double          ang    = getCircleAngleRange(lastEl, endEl);

        Real            r      = last.getAttribute(param);
        StormTrackPoint[] halfCircle = getHalfCircleTrackPoint(lastEl, ang,
                                           ((r != null)
                                            ? r.getValue()
                                            : 0), last.getTime());

        for (int i = 0; i < 11; i++) {
            conePoints[size + i] = halfCircle[i];
        }

        List coneList = new ArrayList<StormTrackPoint>();
        for (int i = 0; i < numberOfPoint; i++) {
            coneList.add(conePoints[i]);
        }

        return new StormTrack(track.getStormInfo(), new Way(getWay() +"_CONE"),
                              coneList,null);

    }

    /**
     * _more_
     *
     * @param sp1 _more_
     * @param sp2 _more_
     * @param param _more_
     * @param right _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public StormTrackPoint getPointToCircleTangencyPoint(StormTrackPoint sp1,
            StormTrackPoint sp2, StormParam param, boolean right)
            throws VisADException {

        int              sign = 1;
        EarthLocation    el1  = sp1.getLocation();
        EarthLocation    el2  = sp2.getLocation();

        Real             rl   = sp2.getAttribute(param);

        double           r    = rl.getValue();
        double           lat1 = el1.getLatitude().getValue();
        double           lon1 = el1.getLongitude().getValue();
        LatLonPointImpl  p1   = new LatLonPointImpl(lat1, lon1);

        double           lat2 = el2.getLatitude().getValue();
        double           lon2 = el2.getLongitude().getValue();
        LatLonPointImpl  p2   = new LatLonPointImpl(lat2, lon2);

        LatLonProjection pj1  = new LatLonProjection();
        ProjectionPoint  pp1  = pj1.latLonToProj(p1);
        LatLonProjection pj2  = new LatLonProjection();
        ProjectionPoint  pp2  = pj2.latLonToProj(p2);

        Bearing b = Bearing.calculateBearing(lat1, lon1, lat2, lon2, null);
        double           dist = b.getDistance();


        if ( !right) {
            sign = -1;
        }
        double x = pp2.getX() + sign * r * (pp2.getY() - pp1.getY()) / dist;
        double y = pp2.getY() + sign * r * (pp1.getX() - pp2.getX()) / dist;


        ProjectionPoint  pp   = new ProjectionPointImpl(x, y);
        LatLonPointImpl  lp   = new LatLonPointImpl();
        LatLonProjection e3   = new LatLonProjection();
        LatLonPoint      lp11 = e3.projToLatLon(pp, lp);

        EarthLocation el = new EarthLocationLite(lp11.getLatitude(),
                               lp11.getLongitude(), 0);
        StormTrackPoint sp = new StormTrackPoint(el, sp1.getTime(), 0, null);
        return sp;
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param d _more_
     *
     * @return _more_
     */
    public double getCircleAngleRange(EarthLocation c, EarthLocation d) {


        double           lat1 = c.getLatitude().getValue();
        double           lon1 = c.getLongitude().getValue();
        LatLonPointImpl  p1   = new LatLonPointImpl(lat1, lon1);

        double           lat2 = d.getLatitude().getValue();
        double           lon2 = d.getLongitude().getValue();
        LatLonPointImpl  p2   = new LatLonPointImpl(lat2, lon2);

        LatLonProjection pj1  = new LatLonProjection();
        ProjectionPoint  pp1  = pj1.latLonToProj(p1);
        LatLonProjection pj2  = new LatLonProjection();
        ProjectionPoint  pp2  = pj2.latLonToProj(p2);
        double           dx   = pp2.getX() - pp1.getX();
        double           dy   = pp2.getY() - pp1.getY();

        double           a    = Math.atan2(dy, dx);

        return a;
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param angle _more_
     * @param r _more_
     * @param dt _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public StormTrackPoint[] getHalfCircleTrackPoint(EarthLocation c,
            double angle, double r, DateTime dt)
            throws VisADException {
        // return 10 track point
        int               size  = 11;

        StormTrackPoint[] track = new StormTrackPoint[size];

        double            lat0  = c.getLatitude().getValue();
        double            lon0  = c.getLongitude().getValue();

        for (int i = 0; i < size; i++) {
            double          af   = (angle + i * 15 * Math.PI / 180.0) * 180.0 /Math.PI;
            // change angle to azimuth
            if(af <= 90 && af >= 0) {
                af = 90 - af;
            }
            else if ( af > 90 && af <= 180) {
                af = 360 + (90 - af);
            }
            else if ( af < 0 && af >= -180) {
                af = 90 - af;
            }
            else if ( af > 180 && af <= 360) {
                af = 450 - af;
            }


            LatLonPointImpl lp = Bearing.findPoint(lat0, lon0, af, r, null);


            EarthLocation el = new EarthLocationLite(lp.getLatitude(),
                                   lp.getLongitude(), 0);
            StormTrackPoint sp = new StormTrackPoint(el, dt, 0, null);

            track[i] = sp;
        }

        return track;
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param angle _more_
     * @param r _more_
     * @param dt _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public StormTrackPoint[] getHalfCircleTrackPointOld(EarthLocation c,
            double angle, double r, DateTime dt)
            throws VisADException {
        // return 10 track point
        int               size  = 11;

        StormTrackPoint[] track = new StormTrackPoint[size];
        FlatEarth         e     = new FlatEarth();
        ProjectionPointImpl p0 = e.latLonToProj(c.getLatitude().getValue(),
                                     c.getLongitude().getValue());

        for (int i = 0; i < size; i++) {
            double          af   = angle + i * 15 * Math.PI / 180.0;
            double          x    = p0.getX() + r * Math.cos(af);
            double          y    = p0.getY() + r * Math.sin(af);

            ProjectionPoint pp   = new ProjectionPointImpl(x, y);
            LatLonPointImpl lp   = new LatLonPointImpl();
            FlatEarth       e3   = new FlatEarth();
            LatLonPoint     lp11 = e3.projToLatLon(pp, lp);
            EarthLocation el = new EarthLocationLite(lp11.getLatitude(),
                                   lp11.getLongitude(), 0);
            StormTrackPoint sp = new StormTrackPoint(el, dt, 0, null);

            track[i] = sp;
        }

        return track;
    }



}


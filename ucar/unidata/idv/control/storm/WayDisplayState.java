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
import ucar.unidata.data.storm.StormParam;
import ucar.unidata.data.storm.StormTrack;
import ucar.unidata.data.storm.StormTrackPoint;
import ucar.unidata.data.storm.Way;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.FlatEarth;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.gis.SpatialGrid;
import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.util.*;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;
import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.Displayable;
import ucar.visad.display.StationModelDisplayable;
import ucar.visad.display.TrackDisplayable;

import visad.*;
import visad.Set;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.*;
import java.util.List;

import javax.swing.*;


/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class WayDisplayState {


    /** Type for Azimuth */
    private final RealType azimuthType = RealType.getRealType("Azimuth",
                                             CommonUnit.degree);


    /** _more_ */
    private Way way;

    /** _more_ */
    private StormDisplayState stormDisplayState;

    /** _more_ */
    private DisplayState trackState;

    /** _more_ */
    private DisplayState coneState;

    /** _more_ */
    private DisplayState wayState;

    /** _more_ */
    private DisplayState ringsState;


    /** _more_ */
    List<PointOb> pointObs = new ArrayList<PointOb>();

    /** _more_ */
    List<PointOb> allPointObs = new ArrayList<PointOb>();

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

    /** _more_ */
    private StationModelDisplayable labelDisplay;

    /** _more_ */
    private StationModelDisplayable obsPointDisplay;

    /** _more_ */
    private TrackDisplayable trackDisplay;

    /** _more_ */
    private TrackDisplayable ringsDisplay;

    /** _more_ */
    private CompositeDisplayable conesHolder;


    /** _more_ */
    private List<StormParam> coneParams;

    /** _more_ */
    private StormParam ringsParam;

    /** _more_ */
    private StormParam colorParam;

    /** _more_ */
    private int modeParam = 99;





    /**
     * _more_
     */
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
        wayState               = new DisplayState(this, "Show/Hide All",
                true);
        trackState = new DisplayState(this, "Show/Hide Track", true);
        coneState = new DisplayState(this, "Show/Hide Cone", false);
        ringsState = new DisplayState(this, "Show/Hide Rings", false);
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
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void removeTrackDisplay() throws VisADException, RemoteException {
        if (trackDisplay != null) {
            removeDisplayable(trackDisplay);
            trackDisplay = null;
        }
    }


    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void removeLabelDisplay() throws VisADException, RemoteException {
        if (labelDisplay != null) {
            removeDisplayable(labelDisplay);
            labelDisplay = null;
        }
    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void removeObsPointDisplay()
            throws VisADException, RemoteException {
        if (obsPointDisplay != null) {
            removeDisplayable(obsPointDisplay);
            obsPointDisplay = null;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasObsPointDisplay() {
        return obsPointDisplay != null;
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
     *
     * @param force _more_
     * @throws Exception _more_
     */
    public void updateDisplay(boolean force) throws Exception {

        if ( !shouldShow()) {
            if (holder != null) {
                holder.setVisible(false);
            }
            return;
        }

        getHolder().setVisible(true);
        int forecastAnimationMode =
            stormDisplayState.getForecastAnimationMode();
        if (shouldShowTrack()) {
            StormParam tmpParam     = stormDisplayState.getColorParam(this);
            boolean    hadTrack     = hasTrackDisplay();
            boolean    paramChanged = !Misc.equals(colorParam, tmpParam);
            boolean    modeChanged  = !(modeParam == forecastAnimationMode);
            if (force || !hadTrack || paramChanged || modeChanged || stormDisplayState.isColorRangeChanged()) {
                //                System.err.println("makeing field");
                colorParam = tmpParam;
                // modeParam = forecastAnimationMode;
                FieldImpl trackField = makeTrackField(forecastAnimationMode);
                if (trackField != null) {
                    if(paramChanged){
                        trackDisplay    = null;
                        initTrackDisplay();
                    }
                    getTrackDisplay().setUseTimesInAnimation(false);
                    getTrackDisplay().setTrack(trackField);
                    Range range = null;
                    if (colorParam != null) {
                        String paramName = colorParam.getName();
                        range =
                            stormDisplayState.getStormTrackControl().getIdv()
                                .getParamDefaultsEditor()
                                .getParamRange(paramName);
                        if(stormDisplayState.isColorRangeChanged()){
                            range =  stormDisplayState.getStormTrackControl().getRangeForColorTable();
                            stormDisplayState.getStormTrackControl().getColorTableWidget(range);
                        }

                        Unit displayUnit =
                            stormDisplayState.getStormTrackControl().getIdv()
                                .getParamDefaultsEditor()
                                .getParamDisplayUnit(paramName);
                        if (displayUnit != null) {
                            getTrackDisplay().setDisplayUnit(displayUnit);
                        } else {
                            Unit[] u =GridUtil.getParamUnits(trackField);
                            if(u[0] != null){
                                 getTrackDisplay().setDisplayUnit(u[0]);
                            }
                        }
                    }
                    if (range == null) {
                        range = GridUtil.getMinMax(trackField)[0];
                    }
                    getTrackDisplay().setRangeForColor(range.getMin(),
                            range.getMax());
                }
            }
            setTrackColor();
            getTrackDisplay().setVisible(true);
        } else {
            if (hasTrackDisplay()) {
                getTrackDisplay().setVisible(false);
            }
        }

        updateLayoutModel();


        if (shouldShowCone()) {
            List<StormParam> tmp = stormDisplayState.getConeParams(this);
            if ( !hasConeDisplay() || !Misc.equals(tmp, coneParams)
                    || !(modeParam == forecastAnimationMode)) {
                this.coneParams = tmp;
                getConesHolder().clearDisplayables();
                setConeColor();
                for (StormParam param : coneParams) {
                    TrackDisplayable coneDisplay = makeConeDisplay(param,
                                                       forecastAnimationMode);
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
            StormParam       tmp = stormDisplayState.getRingsParam(this);
            TrackDisplayable ringDisplay = getRingsDisplay();
            if ( !hasRingsDisplay() || !Misc.equals(tmp, ringsParam)
                    || !(modeParam == forecastAnimationMode)) {
                this.ringsParam = tmp;
                setRingsColor();
                FieldImpl field = makeRingsField(ringsParam,
                                      forecastAnimationMode);
                if ((field == null) || (field.getLength() == 0)) {
                    ringDisplay.setData(new Real(0));
                } else {
                    ringDisplay.setTrack(field);
                }
                setRingsColor();
            }
            ringsDisplay.setVisible(true);
        } else {
            if (hasRingsDisplay()) {
                getRingsDisplay().setVisible(false);
            }
        }

        modeParam = forecastAnimationMode;

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
     *
     * @throws Exception _more_
     */
    public void updateLayoutModel() throws Exception {
        StationModel sm;
        //If we are showing the track then create (if needed) the station model displays
        if (shouldShowTrack()) {
            if (way.isObservation()) {
                sm = stormDisplayState.getObsPointLayoutModel();
                //We won't create them (or will remove them) if the layout model is null
                if (sm == null) {
                    removeObsPointDisplay();
                } else {
                    if (true) {  //(!hasObsPointDisplay()) {
                        FieldImpl pointField;
                        pointField =
                            PointObFactory.makeTimeSequenceOfPointObs(
                                allPointObs, -1, -1);

                        FieldImpl pointField1 = doDeclutter(pointField, sm);
                        getObsPointDisplay().setStationData(pointField1);

                    }
                    if (hasObsPointDisplay()) {  //&& !Misc.equals(sm, getObsPointDisplay().getStationModel())) {
                        getObsPointDisplay().setStationModel(sm);
                    }
                }
            }


            sm = (way.isObservation()
                  ? stormDisplayState.getObsLayoutModel()
                  : stormDisplayState.getForecastLayoutModel());
            if (sm == null) {
                removeLabelDisplay();
            } else {
                if (pointObs.size() > 0) {  //(!hasLabelDisplay()) {
                    FieldImpl pointField =
                        PointObFactory.makeTimeSequenceOfPointObs(pointObs,
                            -1, -1);

                    getLabelDisplay().setStationData(pointField);
                    getLabelDisplay().setStationModel(sm);
                }
            }
        }

        setLabelColor();
        if (hasObsPointDisplay()) {
            getObsPointDisplay().setVisible(shouldShowTrack());
        }


        if (hasLabelDisplay()) {
            getLabelDisplay().setVisible(shouldShowTrack());
        }

    }

    /**
     * Declutters the observations.  This is just a wrapper around
     * the real decluttering in doTheActualDecluttering(FieldImpl)
     * to handle the case where there is a time sequence of observations.
     *
     * @param  obs initial field of observations.
     * @param sModel _more_
     *
     * @return a decluttered version of obs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private FieldImpl doDeclutter(FieldImpl obs, StationModel sModel)
            throws VisADException, RemoteException {


        //  long      millis           = System.currentTimeMillis();
        boolean   isTimeSequence   = GridUtil.isTimeSequence(obs);
        FieldImpl declutteredField = null;
        if (isTimeSequence) {
            Set timeSet = obs.getDomainSet();
            declutteredField = new FieldImpl((FunctionType) obs.getType(),
                                             timeSet);
            int numTimes = timeSet.getLength();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) obs.getSample(i);
                FieldImpl subTime = doTheActualDecluttering(oneTime, sModel);
                if (subTime != null) {
                    declutteredField.setSample(i, subTime, false);
                }
            }
        } else {
            declutteredField = doTheActualDecluttering(obs, sModel);
        }
        //System.out.println("Subsetting took : " +
        //    (System.currentTimeMillis() - millis) + " ms");
        return declutteredField;
    }

    /**
     * a     * Declutters a single timestep of observations.
     *
     * @param pointObs  point observations for one timestep.
     *
     * @return a decluttered version of pointObs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */

    /** grid for decluttering */
    private SpatialGrid stationGrid;

    /**
     * _more_
     *
     * @param pointObs _more_
     * @param sm _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private FieldImpl doTheActualDecluttering(FieldImpl pointObs,
            StationModel sm)
            throws VisADException, RemoteException {
        if ((pointObs == null) || pointObs.isMissing()) {
            return pointObs;
        }
        FieldImpl retField    = null;
        Set       domainSet   = pointObs.getDomainSet();
        int       numObs      = domainSet.getLength();
        Vector    v           = new Vector();

        long      t1          = System.currentTimeMillis();
        Rectangle glyphBounds = sm.getBounds();
        float myScale = getObsPointDisplay().getScale() * .0025f
                        * getDeclutterFilter();
        //System.out.println("\ndecluttering  myScale=" + myScale +
        //                           " filter=" +getDeclutterFilter());
        Rectangle2D scaledGlyphBounds =
            new Rectangle2D.Double(glyphBounds.getX() * myScale,
                                   glyphBounds.getY() * myScale,
                                   glyphBounds.getWidth() * myScale,
                                   glyphBounds.getHeight() * myScale);
        NavigatedDisplay navDisplay =
            stormDisplayState.getStormTrackControl().getNavigatedDisplay();

        Rectangle2D.Double obBounds = new Rectangle2D.Double();
        obBounds.width  = scaledGlyphBounds.getWidth();
        obBounds.height = scaledGlyphBounds.getHeight();

        if (stationGrid == null) {
            stationGrid = new SpatialGrid(200, 200);
        }
        stationGrid.clear();
        stationGrid.setGrid(getBounds(), scaledGlyphBounds);
        if (getDeclutterFilter() < 0.3f) {
            //      stationGrid.setOverlap((int)((1.0-getDeclutterFilter())*100));
            //      stationGrid.setOverlap(          (int)((.5f-getDeclutterFilter())*100));
        } else {
            //      stationGrid.setOverlap(0);
        }

        double[] xyz = new double[3];
        //TODO: The repeated getSpatialCoords is a bit expensive
        for (int i = 0; i < numObs; i++) {
            PointOb ob = (PointOb) pointObs.getSample(i);
            xyz = navDisplay.getSpatialCoordinates(ob.getEarthLocation(),
                    xyz);
            obBounds.x = xyz[0];
            obBounds.y = xyz[1];
            if (stationGrid.markIfClear(obBounds, "")) {
                v.add(ob);  // is in the bounds
            }
        }
        //      stationGrid.print();
        long t2 = System.currentTimeMillis();


        if (v.isEmpty()) {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), 1));
            retField.setSample(0, pointObs.getSample(0), false);
        } else if (v.size() == numObs) {
            retField = pointObs;  // all were in domain, just return input
        } else {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), v.size()));
            retField.setSamples((PointOb[]) v.toArray(new PointOb[v.size()]),
                                false, false);
        }

        long t3 = System.currentTimeMillis();
        //System.err.println("size:" + v.size() +" declutter:" + (t2-t1) + " " + (t3-t2));


        return retField;
    }

    /** decluttering filter factor */
    private float declutterFilter = 1.0f;

    /**
     * _more_
     *
     * @return _more_
     */
    public float getDeclutterFilter() {
        return declutterFilter;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected Rectangle2D getBounds() {
        return calculateRectangle();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected Rectangle2D calculateRectangle() {
        try {
            Rectangle2D.Double box =
                stormDisplayState.getStormTrackControl().getNavigatedDisplay()
                    .getVisadBox();
            if ( !box.isEmpty()) {
                // pad rectangle by 5%
                double deltaWidth  = (double) (.05 * box.width);
                double deltaHeight = (double) (.05 * box.height);
                double newX        = box.x - deltaWidth;
                double newY        = box.y - deltaHeight;
                box.setRect(newX, newY, box.width + (2.0 * deltaWidth),
                            box.height + (2.0 * deltaHeight));
            }
            return box;
        } catch (Exception excp) {
            LogUtil.logException("calculating Rectangle ", excp);
            return new Rectangle2D.Double(0, 0, 0, 0);
        }
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
            StationModel sm = (way.isObservation()
                               ? stormDisplayState.getObsLayoutModel()
                               : stormDisplayState.getForecastLayoutModel());
            if (sm != null) {
                labelDisplay = new StationModelDisplayable("dots");
                labelDisplay.setRotateShapes(true);
                labelDisplay.setUseTimesInAnimation(false);
                addDisplayable(labelDisplay);
                labelDisplay
                    .setScale(stormDisplayState.getStormTrackControl()
                        .getDisplayScale());
            }
        }
        return labelDisplay;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public StationModelDisplayable getObsPointDisplay()
            throws VisADException, RemoteException {
        if (obsPointDisplay == null) {
            obsPointDisplay = new StationModelDisplayable("dots");
            obsPointDisplay.setRotateShapes(true);
            obsPointDisplay.setUseTimesInAnimation(false);
            addDisplayable(obsPointDisplay);
            obsPointDisplay.setScale(
                stormDisplayState.getStormTrackControl().getDisplayScale());
        }
        return obsPointDisplay;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
     public void initTrackDisplay() throws Exception {

        trackDisplay = new TrackDisplayable("track_"
                + stormDisplayState.getStormInfo().getStormId() ); // + stormDisplayState.getColorParam(this));
        if (way.isObservation()) {
            trackDisplay.setLineWidth(3);
        } else {
            trackDisplay.setLineWidth(2);
            trackDisplay.setUseTimesInAnimation(false);
        }
        //setTrackColor();
       int cnt = holder.displayableCount();

        for(int i = 0; i<cnt; i++ ){
            Displayable dp = holder.getDisplayable(i);
            if(dp.getClass().isInstance(trackDisplay)) {
                TrackDisplayable dd = (TrackDisplayable)dp;
                if(dd.toString().equalsIgnoreCase(trackDisplay.toString())){
                    holder.removeDisplayable(dp);
                    cnt = cnt -1;
                }
            }
        }

        addDisplayable(trackDisplay);

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
           initTrackDisplay();
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
            conesHolder.setVisible(true);
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
        if (ringsDisplay == null) {
            ringsDisplay = new TrackDisplayable("ring_"
                    + stormDisplayState.getStormInfo().getStormId() + "_"
                    + getWay());
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
     * @param mode _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TrackDisplayable makeConeDisplay(StormParam param, int mode)
            throws Exception {
        FieldImpl field = makeConeField(param, mode);
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
     * @param param _more_
     * @param mode _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TrackDisplayable makeRingDisplay(StormParam param, int mode)
            throws Exception {
        FieldImpl field = makeRingsField(param, mode);
        if (field == null) {
            return null;
        }
        TrackDisplayable ringDisplay =
            new TrackDisplayable(
                "ring_" + stormDisplayState.getStormInfo().getStormId());
        ringDisplay.setUseTimesInAnimation(false);
        ringDisplay.setTrack(field);
        ringDisplay.setUseTimesInAnimation(false);
        return ringDisplay;
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
        ColorTable ct = stormDisplayState.getColorTable(colorParam);
        if (ct != null) {
            return stormDisplayState.getStormTrackControl()
                .getColorTableForDisplayable(ct);
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
            if(colorParam == null || colorParam.getName().equalsIgnoreCase("Fixed") ) {
                trackDisplay.setColor(getColor());
            }
            else
                trackDisplay.setColorPalette(getColorPalette());
        }

    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void setLabelColor() throws Exception {
        Color c = getColor();
        if (labelDisplay != null) {  //&& !Misc.equals(c, labelDisplay.getColor())) {
            labelDisplay.setColor(c);
        }
        if (obsPointDisplay != null) {  //&& !Misc.equals(c, obsPointDisplay.getColor())) {
            obsPointDisplay.setColor(c);
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
        conesHolder  = null;
        if (holder != null) {}
        trackDisplay    = null;
        labelDisplay    = null;
        obsPointDisplay = null;
        holder          = null;
        tracks          = new ArrayList<StormTrack>();
        times           = new ArrayList<DateTime>();
    }




    /** _more_ */
    private static TextType fhourType;

    /** _more_ */
    private static TextType rhourType;

    /** _more_ */
    private static TextType shourType;

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
     *
     * @param mode _more_
     * @return _more_
     * @throws Exception _more_
     */


    protected FieldImpl makeTrackField(int mode) throws Exception {
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        List<DateTime>  times  = new ArrayList<DateTime>();

        //Use a local list to hold the point obs so we don't run into a race condition
        List<PointOb> localPointObs    = new ArrayList<PointOb>();
        List<PointOb> localAllPointObs = new ArrayList<PointOb>();
        Data[]        datas            = new Data[tracks.size()];
        int           i                = 0;
        for (StormTrack track : tracks) {
            FieldImpl field =
                stormDisplayState.getStormTrackControl().makeTrackField(
                    track, colorParam);
            if (field == null) {
                continue;
            }
            if(i == 0)
                datas[i++] = field;
            else
                datas[i++] = field.changeMathType(datas[0].getType());
            fields.add(field);
            times.add(track.getStartTime());
            //  if(!way.isObservation() && mode == 0)
            localPointObs.addAll(makePointObs(track, !way.isObservation()));
            if (way.isObservation()) {
                localAllPointObs.addAll(makeObsPointObs(track));
            }
        }

        pointObs    = localPointObs;
        allPointObs = localAllPointObs;

        if (fields.size() == 0) {
            return null;
        }
        //        if(fields.size()==1) return fields.get(0);

        if ( !way.isObservation() && (mode == 1)) {
            return Util.indexedField(datas, false);
        } else {
            return Util.makeTimeField(fields, times);
        }
    }





    /**
     * _more_
     *
     *
     * @param stormParam _more_
     * @param mode _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected FieldImpl makeConeField(StormParam stormParam, int mode)
            throws Exception {
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        List<DateTime>  times  = new ArrayList<DateTime>();
        Data[]          datas  = new Data[tracks.size()];
        int             i      = 0;
        for (StormTrack track : tracks) {
            StormTrack coneTrack = makeConeTrack(track, stormParam);
            if (coneTrack == null) {
                continue;
            }
            FieldImpl field =
                stormDisplayState.getStormTrackControl().makeTrackField(
                    coneTrack, null);
            fields.add(field);

            times.add(track.getStartTime());
            datas[i++] = field;
        }

        if (fields.size() == 0) {
            return null;
        }

        if ( !way.isObservation() && (mode == 1)) {
            return Util.indexedField(datas, false);
        } else {
            return Util.makeTimeField(fields, times);
        }
    }


    /**
     * _more_
     *
     * @param track _more_
     * @param useStartTime _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    private List<PointOb> makePointObs(StormTrack track, boolean useStartTime)
            throws Exception {
        boolean               isObservation = way.isObservation();
        DateTime              startTime     = track.getStartTime();
        List<StormTrackPoint> stps          = track.getTrackPoints();
        if (fhourType == null) {
            fhourType = new TextType("fhour");
        }

        if (rhourType == null) {
            rhourType = new TextType("rhour");
        }

        if (shourType == null) {
            shourType = new TextType("shour");
        }
        List<PointOb>    pointObs  = new ArrayList<PointOb>();

        DecimalFormat    format    = new DecimalFormat("0.#");
        Date             startDate = Util.makeDate(startTime);
        List<StormParam> params    = track.getParams();
        for (int i = 0; i < stps.size(); i++) {
            StormTrackPoint stp    = stps.get(i);
            DateTime        time   = (useStartTime
                                      ? startTime
                                      : stp.getTime());
            String          flabel = "";
            String          rlabel = "";
            String          slabel = "";
            if ( !isObservation) {
                if (i == 0) {
                    //                 label = way.getId() + ": " + track.getStartTime();
                } else {
                    flabel = "" + stp.getForecastHour() + "H";
                    Date dttm = Util.makeDate(stp.getTime());
                    rlabel = "" + dttm.toString();
                    slabel = "" + getMonDayHour(dttm);;
                }
            } else if (useStartTime && (i > 0)) {
                Date dttm = Util.makeDate(stp.getTime());
                double diffSeconds = (dttm.getTime() - startDate.getTime())
                                     / 1000.0;
                double diffHours = diffSeconds / 3600.0;

                flabel = format.format(diffHours) + "H";
                rlabel = "" + dttm.toString();
                slabel = "" + getMonDayHour(dttm);
            }
            Data[] data = new Data[params.size() + 3];

            data[0] = new visad.Text(rhourType, rlabel);
            data[1] = new visad.Text(fhourType, flabel);
            data[2] = new visad.Text(shourType, slabel);
            for (int paramIdx = 0; paramIdx < params.size(); paramIdx++) {
                Real r = stp.getAttribute(params.get(paramIdx));
                if (r == null) {
                    r = params.get(paramIdx).getReal(Double.NaN);
                }
                data[paramIdx + 3] = r;

            }
            Tuple tuple = new Tuple(data);
            pointObs.add(PointObFactory.makePointOb(stp.getLocation(), time,
                    tuple));

        }
        return pointObs;
    }

    /**
     * _more_
     *
     * @param dt _more_
     *
     * @return _more_
     */
    private String getMonDayHour(Date dt) {
        Calendar cal = Calendar.getInstance();

        cal.setTime(dt);
        int m = cal.get(Calendar.MONTH) + 1;  // 0 base, ie 0 for Jan
        int d = cal.get(Calendar.DAY_OF_MONTH);
        int h = cal.get(Calendar.HOUR_OF_DAY);

        return "" + m + "/" + d + "/" + h + "H";
    }


    /**
     * _more_
     *
     * @param track _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<PointOb> makeObsPointObs(StormTrack track) throws Exception {
        DateTime              startTime = track.getStartTime();
        List<StormTrackPoint> stps      = track.getTrackPoints();
        if (fhourType == null) {
            fhourType = new TextType("fhour");
        }
        if (rhourType == null) {
            rhourType = new TextType("rhour");
        }
        if (shourType == null) {
            shourType = new TextType("shour");
        }
        List<PointOb>    pointObs = new ArrayList<PointOb>();
        DecimalFormat    format   = new DecimalFormat("0.#");
        List<StormParam> params   = track.getParams();
        for (int i = 0; i < stps.size(); i++) {
            StormTrackPoint baseStp  = stps.get(i);
            DateTime        baseTime = baseStp.getTime();
            Date            baseDate = Util.makeDate(baseTime);
            for (int j = i; j < stps.size(); j++) {
                StormTrackPoint stp    = stps.get(j);
                String          flabel = "";
                String          rlabel = "";
                String          slabel = "";
                if (j > 0) {
                    Date dttm = Util.makeDate(stp.getTime());
                    double diffSeconds = (dttm.getTime()
                                          - baseDate.getTime()) / 1000.0;
                    double diffHours = diffSeconds / 3600.0;
                    flabel = format.format(diffHours) + "H";
                    rlabel = "" + stp.getTime().toString(); //dttm.toString();
                    slabel = "" + getMonDayHour(dttm);
                }
                Data[] data = new Data[params.size() + 3];
                data[0] = new visad.Text(fhourType, flabel);
                data[1] = new visad.Text(rhourType, rlabel);
                data[2] = new visad.Text(shourType, slabel);
                for (int paramIdx = 0; paramIdx < params.size(); paramIdx++) {
                    Real r = stp.getAttribute(params.get(paramIdx));
                    if (r == null) {
                        r = params.get(paramIdx).getReal(Double.NaN);
                    }
                    data[paramIdx + 3] = r;
                }
                Tuple tuple = new Tuple(data);
                pointObs.add(PointObFactory.makePointOb(stp.getLocation(),
                        baseTime, tuple));
            }
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
     * _more_
     *
     * @param displayable _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void removeDisplayable(Displayable displayable)
            throws VisADException, RemoteException {
        getHolder().removeDisplayable(displayable);
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

    /**
     * _more_
     *
     * @param stormParam _more_
     * @param mode _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FieldImpl makeRingsField(StormParam stormParam, int mode)
            throws Exception {
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        List<DateTime>  times  = new ArrayList<DateTime>();
        Data[]          datas  = new Data[tracks.size() * 10];
        int             i      = 0;

        if ( !way.isObservation() && (mode == 1)) {
            for (StormTrack track : tracks) {
                List<StormTrack> stList = makeRingTrackList(track,
                                              stormParam);
                for (StormTrack stk : stList) {
                    FieldImpl field =
                        stormDisplayState.getStormTrackControl()
                            .makeTrackField(stk, null);
                    fields.add(field);
                    datas[i++] = field;
                }

            }

            return Util.indexedField(datas, false);
        }

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
     * @throws Exception _more_
     */
    public List makeRingTrackList(StormTrack track, StormParam param)
            throws Exception {
        List<StormTrackPoint> stps = getRealTrackPoints(track, param);
        List<StormTrack>      stracks        = new ArrayList();

        int                   size           = stps.size();
        DateTime              dt             = stps.get(0).getTime();
        Way                   ringWay        = new Way(getWay() + "_RING");
        int                   numberOfPoints = 73;
        double                angleDelta     = 360.0 / (numberOfPoints - 1);
        for (int i = 0; i < size; i++) {
            StormTrackPoint stp = stps.get(i);
            Real            r   = stp.getAttribute(param);
            if (r != null) {
                double rr       = r.getValue();
                double azi      = 0.0;
                List   ringList = new ArrayList<StormTrackPoint>();
                for (int j = 0; j < numberOfPoints; j++) {
                    ringList.add(getCirclePoint(stp, rr, azi, dt));
                    azi = azi + angleDelta;
                }
                stracks.add(new StormTrack(track.getStormInfo(), ringWay,
                                           ringList, null));
            }
        }

        return stracks;

    }

    /**
     *  _more_
     *
     *  @param track _more_
     *  @param param _more_
     *
     *  @return _more_
     *
     *
     * @throws Exception _more_
     */
    public FieldImpl makeRingTracks(StormTrack track, StormParam param)
            throws Exception {
        List<StormTrackPoint> stps = getRealTrackPoints(track, param);
        List<StormTrack>      stracks        = new ArrayList();
        int                   size           = stps.size();
        DateTime              dt             = stps.get(0).getTime();
        Way                   ringWay        = new Way(getWay() + "_RING");
        int                   numberOfPoints = 73;
        double                angleDelta     = 360.0 / (numberOfPoints - 1);
        for (int i = 0; i < size; i++) {
            StormTrackPoint stp = stps.get(i);
            Real            r   = stp.getAttribute(param);
            if (r != null) {
                double rr       = r.getValue();
                double azi      = 0.0;
                List   ringList = new ArrayList<StormTrackPoint>();
                for (int j = 0; j < numberOfPoints; j++) {
                    ringList.add(getCirclePoint(stp, rr, azi, dt));
                    azi = azi + angleDelta;
                }
                stracks.add(new StormTrack(track.getStormInfo(), ringWay,
                                           ringList, null));
            }
        }

        Data[] datas = new Data[stracks.size()];
        int    i     = 0;
        for (StormTrack ringTrack : stracks) {
            datas[i++] =
                stormDisplayState.getStormTrackControl().makeTrackField(
                    ringTrack, null);
        }
        return Util.indexedField(datas, false);
    }

    /**
     * _more_
     *
     * @param stp _more_
     * @param r0 _more_
     * @param azimuth _more_
     * @param dt _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public StormTrackPoint getCirclePoint(StormTrackPoint stp, double r0,
                                          double azimuth, DateTime dt)
            throws VisADException {
        //

        EarthLocation el   = stp.getLocation();
        double        lat0 = el.getLatitude().getValue();
        double        lon0 = el.getLongitude().getValue();
        //DateTime dt = stp.getTime();
        LatLonPointImpl lp = Bearing.findPoint(lat0, lon0, azimuth, r0, null);

        EarthLocation el1 = new EarthLocationLite(lp.getLatitude(),
                                lp.getLongitude(), 0);
        StormTrackPoint stp1 = new StormTrackPoint(el1, dt, 0, null);


        return stp1;
    }

    /**
     * old
     *
     * @param track _more_
     * @param param _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public StormTrack makeConeTrack_Old(StormTrack track, StormParam param)
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
        StormTrackPoint endSTP = conePoints[size - 1];
        int             ii     = 0;
        while ((endSTP == null) && (ii < (size - 2))) {
            ii++;
            last   = stps.get(size - 1 - ii);
            lastEl = last.getLocation();
            endSTP = conePoints[size - 1 - ii];
        }

        if ((endSTP == null) || (ii == (size - 2))) {
            return null;
        }
        EarthLocation endEl = endSTP.getLocation();

        double        ang   = getCircleAngleRange(lastEl, endEl);

        Real          r     = last.getAttribute(param);
        StormTrackPoint[] halfCircle = getHalfCircleTrackPoint(lastEl, ang,
                                           ((r != null)
                                            ? r.getValue()
                                            : 0), last.getTime());

        for (int i = 0; i < 11; i++) {
            conePoints[size + i] = halfCircle[i];
        }

        List coneList = new ArrayList<StormTrackPoint>();
        for (int i = 0; i < numberOfPoint; i++) {
            if (conePoints[i] != null) {
                coneList.add(conePoints[i]);
            }
        }

        return new StormTrack(track.getStormInfo(),
                              new Way(getWay() + "_CONE"), coneList, null);

    }

    /**
     * construct the cone track as track of point to circle and circle to circle
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

        List<StormTrackPoint> stps = getRealTrackPoints(track, param);
        int                   size          = stps.size();
        int                   numberOfPoint = size * 2 + 100;
        List<StormTrackPoint> conePointsLeft =
            new ArrayList<StormTrackPoint>();
        List<StormTrackPoint> conePointsRight =
            new ArrayList<StormTrackPoint>();

        StormTrackPoint stp1 = stps.get(0);
        conePointsRight.add(stp1);  // first point  & last point
        conePointsLeft.add(stp1);
        StormTrackPoint stp2 = stps.get(1);
        StormTrackPoint stp3 = stps.get(2);
        int             nn   = 3;
        // first point to circle
        List<StormTrackPoint> p2c = getPointToCircleTangencyPointA(stp1,
                                        stp2, stp3, param, true);
        while (p2c == null) {  // need to find the first point with param value
            stp2 = stp3;
            if (nn < size) {
                stp3 = stps.get(nn);
            } else {
                stp3 = null;
                //   return null;
            }
            p2c = getPointToCircleTangencyPointA(stp1, stp2, stp3, param,
                    true);
            nn++;
            if (nn >= size) {
                break;
            }
        }
        if (p2c != null) {
            conePointsRight.addAll(p2c);
            p2c = getPointToCircleTangencyPointA(stp1, stp2, stp3, param,
                    false);
            conePointsLeft.addAll(p2c);
        }

        // circle  to circle 1 to n
        stp1 = stp2;
        stp2 = stp3;
        for (int i = nn; i < size; i++) {
            stp3 = stps.get(i);
            //right point
            p2c = getCircleToCircleTangencyPointA(stp1, stp2, stp3, param,
                    true);
            if (p2c != null) {
                conePointsRight.addAll(p2c);
                //left point
                p2c = getCircleToCircleTangencyPointA(stp1, stp2, stp3,
                        param, false);
                conePointsLeft.addAll(p2c);
                stp1 = stp2;  // update the first point only after the valid second point
            }

            stp2 = stp3;
        }
        // last circle
        stp3 = null;
        p2c  = getCircleToCircleTangencyPointA(stp1, stp2, stp3, param, true);
        if (p2c != null) {
            conePointsRight.addAll(p2c);
            //left point
            p2c = getCircleToCircleTangencyPointA(stp1, stp2, stp3, param,
                    false);
            conePointsLeft.addAll(p2c);
            stp1 = stp2;
        }


        // end point half circle take 11 points
        StormTrackPoint last = stp2;
        if (last == null) {
            last = stp1;
        }
        if (last == null) {
            return null;
        }
        EarthLocation lastEl = last.getLocation();
        StormTrackPoint endSTP = conePointsRight.get(conePointsRight.size()
                                     - 1);
        /*   int             ii     = 0;
           while ((endSTP == null) && (ii < (size - 2))) {
               ii++;
               last   = stps.get(size - 1 - ii);
               lastEl = last.getLocation();
               endSTP = conePointsRight.get(size - 1 - ii);
           }

           if ((endSTP == null) || (ii == (size - 2))) {
               return null;
           }
        */
        if (endSTP == null) {
            return null;
        }
        EarthLocation endEl = endSTP.getLocation();

        double        ang   = getCircleAngleRange(lastEl, endEl);

        Real          r     = last.getAttribute(param);
        StormTrackPoint[] halfCircle = getHalfCircleTrackPoint(lastEl, ang,
                                           ((r != null)
                                            ? r.getValue()
                                            : 0), last.getTime());

        for (int i = 0; i < 11; i++) {}
        //merge three lists
        List<StormTrackPoint> coneList = new ArrayList<StormTrackPoint>();
        int                   s1       = conePointsRight.size();
        for (int i = 0; i < s1; i++) {
            if (conePointsRight.get(i) != null) {
                coneList.add(conePointsRight.get(i));
            }
        }
        for (int i = 0; i < 11; i++) {
            coneList.add(halfCircle[i]);
        }
        int s2 = conePointsLeft.size();
        for (int i = s2; i > 0; i--) {
            if (conePointsLeft.get(i - 1) != null) {
                coneList.add(conePointsLeft.get(i - 1));
            }
        }

        return new StormTrack(track.getStormInfo(),
                              new Way(getWay() + "_CONE"), coneList, null);


    }

    /**
     * calculate the bearing of two storm track points
     *
     * @param sp1 _more_
     * @param sp2 _more_
     *
     * @return _more_
     */
    public Bearing getStormPoinsBearing(StormTrackPoint sp1,
                                        StormTrackPoint sp2) {
        EarthLocation el1 = sp1.getLocation();
        EarthLocation el2 = sp2.getLocation();
        return Bearing.calculateBearing(el1.getLatitude().getValue(),
                                        el1.getLongitude().getValue(),
                                        el2.getLatitude().getValue(),
                                        el2.getLongitude().getValue(), null);

    }

    /**
     * get the tangency point to the circle of the second point  and the third point
     * as its direction  of adding additional points
     *
     * @param sp1       outside point
     * @param sp2       the center of the circle
     * @param sp3 _more_
     * @param param _more_
     * @param right _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public List<StormTrackPoint> getPointToCircleTangencyPointA(
            StormTrackPoint sp1, StormTrackPoint sp2, StormTrackPoint sp3,
            StormParam param, boolean right)
            throws VisADException {

        List<StormTrackPoint> trackPoints = new ArrayList<StormTrackPoint>();
        if (sp3 == null) {
            return getPointToCircleTangencyPointB(sp1, sp2, param, right);
        }

        EarthLocation el1 = sp1.getLocation();
        EarthLocation el2 = sp2.getLocation();
        EarthLocation el3 = sp3.getLocation();

        Real          rl  = sp2.getAttribute(param);
        double        r   = rl.getValue();

        if (Float.isNaN((float) r) || (r == 0.0)) {
            return null;
        }

        double  lat1  = el1.getLatitude().getValue();
        double  lon1  = el1.getLongitude().getValue();

        double  lat2  = el2.getLatitude().getValue();
        double  lon2  = el2.getLongitude().getValue();

        double  lat3  = el3.getLatitude().getValue();
        double  lon3  = el3.getLongitude().getValue();


        Bearing b     = Bearing.calculateBearing(lat1, lon1, lat2, lon2,
                            null);
        Bearing c     = Bearing.calculateBearing(lat2, lon2, lat3, lon3,
                            null);
        double  dist1 = b.getDistance();

        if (dist1 < r) {  // first point is inside the circle
            trackPoints.add(getPointToCircleTangencyPoint(sp1, sp2, param,
                    right));
            return trackPoints;
        }


        double af  = getCircleAngleRange(el1, el2);
        double ddt = Math.abs(b.getAngle() - c.getAngle());
        double bt  = getCircleTangencyAngle(el1, el2, r);

        af = af * 180.0 / Math.PI;
        bt = bt * 180.0 / Math.PI;
        if (right) {
            af = af - 90;
        } else {
            af = af + 90;
        }
        // change angle to azimuth
        double az = af;
        if ((af <= 90) && (af >= 0)) {
            az = 90 - af;
        } else if ((af > 90) && (af <= 180)) {
            az = 360 + (90 - af);
        } else if ((af < 0) && (af >= -180)) {
            az = 90 - af;
        } else if ((af > 180) && (af <= 360)) {
            az = 450 - af;
        } else if ((af < -180) && (af >= -360)) {
            az = -270 - af;
        }
        if (right) {
            az = az + bt;
        } else {
            az = az - bt;
        }

        if (ddt > 270) {
            ddt = 360 - ddt;
        } else if (ddt > 180) {
            ddt = ddt - 180;
        } else if (ddt > 90) {
            ddt = ddt - 90;
        }

        double dt = bt;

        if (right) {
            if ((c.getAngle() < b.getAngle())
                    && (Math.abs(b.getAngle() - c.getAngle()) < 90)) {
                dt = bt + ddt;
            } else if ((c.getAngle() > b.getAngle())
                       && (Math.abs(b.getAngle() - c.getAngle()) > 180)) {
                dt = bt + ddt;
            } else {
                dt = bt - ddt;
            }
        } else {
            if ((c.getAngle() > b.getAngle())
                    && (Math.abs(b.getAngle() - c.getAngle()) < 90)) {
                dt = bt + ddt;
            } else if ((c.getAngle() < b.getAngle())
                       && (Math.abs(b.getAngle() - c.getAngle()) > 180)) {
                dt = bt + ddt;
            } else {
                dt = bt - ddt;
            }

        }


        int n = (int) dt / 5 + 1;
        if (n <= 0) {
            n = 1;
        }
        double dtt = dt / n;
        if (dtt < 0) {
            dtt = 0;
            n   = 1;
        }
        for (int i = 0; i < n; i++) {

            LatLonPointImpl lp1 = Bearing.findPoint(lat2, lon2, az, r, null);
            //add more points along the circle

            EarthLocation el = new EarthLocationLite(lp1.getLatitude(),
                                   lp1.getLongitude(), 0);
            trackPoints.add(new StormTrackPoint(el, sp1.getTime(), 0, null));
            if (right) {
                az = az - dtt;
            } else {
                az = az + dtt;
            }
        }

        return trackPoints;
    }

    /**
     * get the tangency point to the circle of the second point
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
    public List<StormTrackPoint> getPointToCircleTangencyPointB(
            StormTrackPoint sp1, StormTrackPoint sp2, StormParam param,
            boolean right)
            throws VisADException {

        List<StormTrackPoint> trackPoints = new ArrayList<StormTrackPoint>();

        if (sp2 == null) {
            return null;
        }
        EarthLocation el1 = sp1.getLocation();
        EarthLocation el2 = sp2.getLocation();


        Real          rl  = sp2.getAttribute(param);
        double        r   = rl.getValue();

        if (Float.isNaN((float) r) || (r == 0.0)) {
            return null;
        }

        double  lat1  = el1.getLatitude().getValue();
        double  lon1  = el1.getLongitude().getValue();

        double  lat2  = el2.getLatitude().getValue();
        double  lon2  = el2.getLongitude().getValue();




        Bearing b     = Bearing.calculateBearing(lat1, lon1, lat2, lon2,
                            null);
        double  dist1 = b.getDistance();

        if (dist1 < r) {  // first point is inside the circle
            trackPoints.add(getPointToCircleTangencyPoint(sp1, sp2, param,
                    right));
            return trackPoints;
        }


        double af = getCircleAngleRange(el1, el2);
        double bt = getCircleTangencyAngle(el1, el2, r);

        af = af * 180.0 / Math.PI;
        bt = bt * 180.0 / Math.PI;
        if (right) {
            af = af - 90;
        } else {
            af = af + 90;
        }
        // change angle to azimuth
        double az = af;
        if ((af <= 90) && (af >= 0)) {
            az = 90 - af;
        } else if ((af > 90) && (af <= 180)) {
            az = 360 + (90 - af);
        } else if ((af < 0) && (af >= -180)) {
            az = 90 - af;
        } else if ((af > 180) && (af <= 360)) {
            az = 450 - af;
        } else if ((af < -180) && (af >= -360)) {
            az = -270 - af;
        }
        if (right) {
            az = az + bt;
        } else {
            az = az - bt;
        }


        double dt  = bt;

        int    n   = (int) dt / 5 + 1;
        double dtt = dt / n;
        for (int i = 0; i < n; i++) {

            LatLonPointImpl lp1 = Bearing.findPoint(lat2, lon2, az, r, null);
            //add more points along the circle

            EarthLocation el = new EarthLocationLite(lp1.getLatitude(),
                                   lp1.getLongitude(), 0);
            trackPoints.add(new StormTrackPoint(el, sp1.getTime(), 0, null));
            if (right) {
                az = az - dtt;
            } else {
                az = az + dtt;
            }
        }

        return trackPoints;
    }

    /**
     * get the approximate tangency points of circle to the circle
     *
     * @param sp1       outside point
     * @param sp2       the center of the circle
     * @param sp3 _more_
     * @param param _more_
     * @param right _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public List<StormTrackPoint> getCircleToCircleTangencyPointA(
            StormTrackPoint sp1, StormTrackPoint sp2, StormTrackPoint sp3,
            StormParam param, boolean right)
            throws VisADException {

        List<StormTrackPoint> trackPoints = new ArrayList<StormTrackPoint>();
        if (sp3 == null) {
            if (sp2 == null) {
                return null;
            }
            trackPoints.add(getPointToCircleTangencyPoint(sp1, sp2, param,
                    right));
            return trackPoints;
        }

        EarthLocation el1 = sp1.getLocation();
        EarthLocation el2 = sp2.getLocation();
        EarthLocation el3 = sp3.getLocation();

        Real          rl  = sp2.getAttribute(param);
        double        r   = rl.getValue();

        if (Float.isNaN((float) r) || (r == 0.0)) {
            return null;
        }

        double  lat1  = el1.getLatitude().getValue();
        double  lon1  = el1.getLongitude().getValue();

        double  lat2  = el2.getLatitude().getValue();
        double  lon2  = el2.getLongitude().getValue();

        double  lat3  = el3.getLatitude().getValue();
        double  lon3  = el3.getLongitude().getValue();


        Bearing b     = Bearing.calculateBearing(lat1, lon1, lat2, lon2,
                            null);
        double  dist1 = b.getDistance();
        Bearing c     = Bearing.calculateBearing(lat2, lon2, lat3, lon3,
                            null);
        double  x     = Math.abs(c.getAngle() - b.getAngle());

        if (right) {
            if ((c.getAngle() > b.getAngle()) || (x > 180)) {
                trackPoints.add(getPointToCircleTangencyPoint(sp1, sp2,
                        param, right));
                return trackPoints;
            }
        }

        if ( !right) {
            if ((c.getAngle() < b.getAngle()) && (x < 90)) {
                trackPoints.add(getPointToCircleTangencyPoint(sp1, sp2,
                        param, right));
                return trackPoints;
            }
        }
        double af = getCircleAngleRange(el1, el2);
        double dt = 0;  //= Math.abs(b.getAngle() - c.getAngle());

        if (x > 270) {
            dt = 360 - x;
        } else if (x > 180) {
            dt = x - 180;
        } else if (x > 90) {
            dt = x - 90;
        } else {
            dt = x;
        }

        af = af * 180.0 / Math.PI;
        if (right) {
            af = af - 90;
        } else {
            af = af + 90;
        }
        // change angle to azimuth
        double az = af;
        if ((af <= 90) && (af >= 0)) {
            az = 90 - af;
        } else if ((af > 90) && (af <= 180)) {
            az = 360 + (90 - af);
        } else if ((af < 0) && (af >= -180)) {
            az = 90 - af;
        } else if ((af > 180) && (af <= 360)) {
            az = 450 - af;
        } else if ((af < -180) && (af >= -360)) {
            az = -270 - af;
        }

        int    n   = (int) dt / 5 + 1;
        double dtt = dt / n;

        for (int i = 0; i < n; i++) {

            LatLonPointImpl lp1 = Bearing.findPoint(lat2, lon2, az, r, null);
            //add more points along the circle

            EarthLocation el = new EarthLocationLite(lp1.getLatitude(),
                                   lp1.getLongitude(), 0);
            trackPoints.add(new StormTrackPoint(el, sp1.getTime(), 0, null));
            if (right) {
                az = az - dtt;
            } else {
                az = az + dtt;
            }
        }

        return trackPoints;
    }

    /**
     * get the 90 degree point to the line of the two points
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


        EarthLocation el1 = sp1.getLocation();
        EarthLocation el2 = sp2.getLocation();

        Real          rl  = sp2.getAttribute(param);
        double        r   = rl.getValue();

        if (Float.isNaN((float) r) || (r == 0.0)) {
            return null;
        }


        double lat2 = el2.getLatitude().getValue();
        double lon2 = el2.getLongitude().getValue();


        double af   = getCircleAngleRange(el1, el2);
        af = af * 180.0 / Math.PI;
        if (right) {
            af = af - 90;
        } else {
            af = af + 90;
        }
        // change angle to azimuth
        if ((af <= 90) && (af >= 0)) {
            af = 90 - af;
        } else if ((af > 90) && (af <= 180)) {
            af = 360 + (90 - af);
        } else if ((af < 0) && (af >= -180)) {
            af = 90 - af;
        } else if ((af > 180) && (af <= 360)) {
            af = 450 - af;
        } else if ((af < -180) && (af >= -360)) {
            af = -270 - af;
        }


        LatLonPointImpl lp1 = Bearing.findPoint(lat2, lon2, af, r, null);

        EarthLocation el = new EarthLocationLite(lp1.getLatitude(),
                               lp1.getLongitude(), 0);
        StormTrackPoint sp = new StormTrackPoint(el, sp1.getTime(), 0, null);
        return sp;
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param d _more_
     * @param r _more_
     *
     * @return _more_
     */
    public double getCircleTangencyAngle(EarthLocation c, EarthLocation d,
                                         double r) {


        double  lat1 = c.getLatitude().getValue();
        double  lon1 = c.getLongitude().getValue();

        double  lat2 = d.getLatitude().getValue();
        double  lon2 = d.getLongitude().getValue();


        Bearing b    = Bearing.calculateBearing(lat1, lon1, lat2, lon2, null);
        double  dist = b.getDistance();
        double  a    = Math.asin(r / dist);

        return a;

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
            double af = (angle + (i + 1) * 15 * Math.PI / 180.0) * 180.0
                        / Math.PI;
            // change angle to azimuth
            if ((af <= 90) && (af >= 0)) {
                af = 90 - af;
            } else if ((af > 90) && (af <= 180)) {
                af = 360 + (90 - af);
            } else if ((af < 0) && (af >= -180)) {
                af = 90 - af;
            } else if ((af > 180) && (af <= 360)) {
                af = 450 - af;
            } else if ((af < -180) && (af >= -360)) {
                af = -270 - af;
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



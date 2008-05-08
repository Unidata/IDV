/*
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


import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;


import ucar.unidata.data.storm.*;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.projection.FlatEarth;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.ui.colortable.ColorTableManager;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.display.*;

import visad.*;

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


    /** _more_ */
    private StormDisplayState stormDisplayState;

    /** _more_ */
    private JCheckBox visibilityCbx;

    /** _more_ */
    private Way way;

    /** _more_ */
    private boolean visible = true;

    /** _more_ */
    private boolean ringsVisible = true;

    /** _more_ */
    private List<StormTrack> tracks = new ArrayList<StormTrack>();

    /** _more_ */
    private List<FieldImpl> fields = new ArrayList<FieldImpl>();

    /** _more_ */
    private List<DateTime> times = new ArrayList<DateTime>();

    /** _more_ */
    private List<PointOb> pointObs = new ArrayList<PointOb>();

    /** _more_ */
    private Color color;


    /** _more_ */
    private JComboBox paramBox;

    /** _more_ */
    private GuiUtils.ColorSwatch colorSwatch;

    /** _more_ */
    private CompositeDisplayable holder;

    /** _more_ */
    private CompositeDisplayable ringsHolder;

    /** _more_ */
    private TrackDisplayable trackDisplay;

    /** _more_ */
    private String colorTable = "Bright38";



    /**
     * _more_
     */
    public WayDisplayState() {}


    /** _more_ */
    private RealType ringsType;

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
     * @param ringsType _more_
     * @param rings _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void setRings(RealType ringsType, List<RingSet> rings)
            throws VisADException, RemoteException {
        this.ringsType = ringsType;
        if (ringsHolder == null) {
            ringsHolder = new CompositeDisplayable("rings holder");
            addDisplayable(ringsHolder);
        }
        ringsHolder.clearDisplayables();
        if (rings != null) {
            for (RingSet ring : rings) {
                ringsHolder.addDisplayable(ring);
            }
        }
        setRingsVisible(getRingsVisible());
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
     *
     * @param stormDisplayState _more_
     * @param way _more_
     */
    public WayDisplayState(StormDisplayState stormDisplayState, Way way) {
        this.stormDisplayState = stormDisplayState;
        this.way               = way;
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
                    if (trackDisplay != null) {
                        try {
                            setTrackColor();
                        } catch (Exception exc) {
                            LogUtil.logException("Setting color", exc);
                        }
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
        RealType type = getParamType();
        if ((type != null) && (colorTable != null)) {
            ColorTable ct =
                stormDisplayState.getStormTrackControl().getControlContext()
                    .getColorTableManager().getColorTable(colorTable);
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
     * @param attrNames _more_
     *
     * @return _more_
     */
    protected Component getParamComponent(Vector attrNames) {
        if ((attrNames == null) || (attrNames.size() == 0)) {
            return GuiUtils.filler();
        }
        if (paramBox == null) {
            paramBox = new JComboBox(attrNames);
            paramBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        makeField();
                        setTrackColor();
                        stormDisplayState.wayParamChanged(
                            WayDisplayState.this);
                    } catch (Exception exc) {
                        LogUtil.logException("Making new field", exc);
                    }
                }
            });
        }
        return paramBox;
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
     * @return _more_
     */
    public boolean usingDefaultParam() {
        if (paramBox == null) {
            return false;
        }
        TwoFacedObject tfo = (TwoFacedObject) paramBox.getSelectedItem();
        Object         id  = tfo.getId();
        if (id == null) {
            return false;
        }
        if (id instanceof RealType) {
            return false;
        }
        return id.toString().equals("default");
    }

    //    protected ColorTable getParamColorTable() {
    //    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected RealType getParamType() {
        if (paramBox == null) {
            return null;
        }
        TwoFacedObject tfo = (TwoFacedObject) paramBox.getSelectedItem();
        Object         id  = tfo.getId();
        if (id == null) {
            return null;
        }
        if (id instanceof RealType) {
            return (RealType) id;
        }
        if (id.toString().equals("default")) {
            return stormDisplayState.getForecastParamType();
        }
        return null;
    }


    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void deactivate() throws VisADException, RemoteException {
        ringsHolder = null;
        if (holder != null) {}
        trackDisplay = null;
        holder       = null;
        ringsHolder  = null;
        tracks       = new ArrayList<StormTrack>();
        fields       = new ArrayList<FieldImpl>();
        times        = new ArrayList<DateTime>();
        pointObs     = new ArrayList<PointOb>();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JCheckBox getVisiblityCheckBox() {
        if (visibilityCbx == null) {
            visibilityCbx = new JCheckBox("", getVisible());
            visibilityCbx.setToolTipText("Show/Hide Track");
            visibilityCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        setVisible(visibilityCbx.isSelected());
                        stormDisplayState.wayVisibilityChanged(
                            WayDisplayState.this);
                    } catch (Exception exc) {
                        LogUtil.logException("Toggling way visibility", exc);
                    }
                }
            });


        }
        return visibilityCbx;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<PointOb> getPointObs() {
        return pointObs;
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
     * @throws Exception _more_
     */
    protected void makeField() throws Exception {
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        List<DateTime>  times  = new ArrayList<DateTime>();

        RealType        type   = getParamType();
        for (StormTrack track : tracks) {
            StormTrack cornTrack = makeCornTrack(track);
            FieldImpl  field     = stormDisplayState.makeField(track, type);
            //  fields.add(field);
            FieldImpl cfield = stormDisplayState.makeField(cornTrack, type);
            fields.add(cfield);

            //  times.add(track.getTrackStartTime());
            times.add(track.getTrackStartTime());
        }

        if (fields.size() == 0) {
            return;
        }

        FieldImpl timeField = ucar.visad.Util.makeTimeField(fields, times);
        getTrackDisplay().setTrack(timeField);
    }


    /**
     * _more_
     *
     * @param track _more_
     * @param field _more_
     *
     * @throws Exception _more_
     */
    public void addTrack(StormTrack track, FieldImpl field) throws Exception {
        tracks.add(track);
        times.add(track.getTrackStartTime());
        fields.add(field);

        boolean               isObservation = way.isObservation();
        DateTime              startTime     = track.getTrackStartTime();
        List<StormTrackPoint> locs          = track.getTrackPoints();
        //        return makePointOb(el,dt, new RealTuple(new Real[] { new Real(0) }));
        if (textType == null) {
            textType = new TextType("label");
        }

        for (int i = 0; i < locs.size(); i++) {
            StormTrackPoint stp   = locs.get(i);
            DateTime        time  = startTime;
            String          label = "";
            if (isObservation) {
                time = stp.getTrackPointTime();
            } else {
                if (i == 0) {
                    label = way + ": " + track.getTrackStartTime();
                } else {
                    label = "" + stp.getForecastHour() + "H";
                }
            }
            Tuple tuple = new Tuple(new Data[] {
                              new visad.Text(textType, label) });
            pointObs.add(
                PointObFactory.makePointOb(
                    stp.getTrackPointLocation(), time, tuple));
        }


    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List getFields() {
        return fields;
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
        if (way.isObservation()) {
            displayable.setVisible(getVisible());
        } else {
            displayable.setVisible(getVisible()
                                   && stormDisplayState.getForecastVisible());
        }
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void checkVisibility() throws Exception {
        if (holder != null) {
            for (Iterator iter = holder.iterator(); iter.hasNext(); ) {
                Displayable displayable = (Displayable) iter.next();
                if (way.isObservation()) {
                    displayable.setVisible(getVisible());
                } else {
                    displayable.setVisible(
                        getVisible()
                        && stormDisplayState.getForecastVisible());
                }
            }
            //setRingsVisible(ringsVisible);
        }

    }

    /**
     * Set the Visible property.
     *
     * @param value The new value for Visible
     *
     * @throws Exception _more_
     */
    public void setVisible(boolean value) throws Exception {
        this.visible = value;
        checkVisibility();
    }

    /**
     * Set the Visible property.
     *
     * @param value The new value for Visible
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setRingsVisible(boolean value)
            throws VisADException, RemoteException {
        this.ringsVisible = value;
        if (ringsHolder != null) {
            ringsHolder.setVisible(ringsVisible);
        }
    }

    /**
     * _more_
     *
     * @param ringDisplayable _more_
     *
     * @throws Exception _more_
     */
    private void setRingVisibility(Displayable ringDisplayable)
            throws Exception {
        if (way.isObservation()) {
            ringDisplayable.setVisible(getVisible() && getRingsVisible());
        } else {
            ringDisplayable.setVisible(
                getVisible() && getRingsVisible()
                && stormDisplayState.getForecastVisible());
        }
    }



    /**
     * Get the Visible property.
     *
     * @return The Visible
     */
    public boolean getVisible() {
        return visible;
    }

    /**
     * Get the Visible property.
     *
     * @return The Visible
     */
    public boolean getRingsVisible() {
        return ringsVisible;
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
    public void setColorTable(String value) {
        colorTable = value;
    }

    /**
     * Get the ColorTable property.
     *
     * @return The ColorTable
     */
    public String getColorTable() {
        return colorTable;
    }

    /**
     * _more_
     *
     * @param track _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public StormTrack makeCornTrack(StormTrack track) throws VisADException {
        List<StormTrackPoint> stps          = track.getTrackPoints();
        int                   size          = stps.size();
        int                   numberOfPoint = size * 2 + 11;
        StormTrackPoint[]     cornPoints = new StormTrackPoint[numberOfPoint];

        StormTrackPoint       stp1          = stps.get(0);
        cornPoints[0]                 = stp1;  // first point  & last point
        cornPoints[numberOfPoint - 1] = stp1;

        StormTrackPoint stp2;
        StormTrackPoint stp;

        // circle  1 to n

        for (int i = 1; i < size; i++) {
            stp2 = stps.get(i);
            //right point
            stp           = getPointToCircleTangencyPoint(stp1, stp2, true);
            cornPoints[i] = stp;
            //left point
            stp = getPointToCircleTangencyPoint(stp1, stp2, false);
            cornPoints[numberOfPoint - i - 1] = stp;
            stp1                              = stp2;
        }

        // end point half circle take 11 points
        StormTrackPoint last   = stps.get(size - 1);
        EarthLocation   lastEl = last.getTrackPointLocation();
        EarthLocation   endEl = cornPoints[size - 1].getTrackPointLocation();
        double          ang    = getCircleAngleRange(lastEl, endEl);

        Real r = last.getAttribute(STIStormDataSource.TYPE_PROBABILITYRADIUS);
        StormTrackPoint[] halfCircle = getHalfCircleTrackPoint(lastEl, ang,
                                           r.getValue(),
                                           last.getTrackPointTime());

        for (int i = 0; i < 11; i++) {
            cornPoints[size + i] = halfCircle[i];
        }

        List cornList = new ArrayList<StormTrackPoint>();
        for (int i = 0; i < numberOfPoint; i++) {
            cornList.add(cornPoints[i]);
        }

        return new StormTrack(track.getStormInfo(), new Way("CORN"),
                              cornList);

    }

    /**
     * _more_
     *
     * @param sp1 _more_
     * @param sp2 _more_
     * @param right _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public StormTrackPoint getPointToCircleTangencyPoint(StormTrackPoint sp1,
            StormTrackPoint sp2, boolean right)
            throws VisADException {

        int           sign = 1;
        EarthLocation el1  = sp1.getTrackPointLocation();
        EarthLocation el2  = sp2.getTrackPointLocation();

        Real rl = sp2.getAttribute(STIStormDataSource.TYPE_PROBABILITYRADIUS);

        double        r    = rl.getValue();
        FlatEarth     e1   = new FlatEarth();
        FlatEarth     e2   = new FlatEarth();
        ProjectionPointImpl p1 =
            e1.latLonToProj(el1.getLatitude().getValue(),
                            el1.getLongitude().getValue());
        ProjectionPointImpl p2 =
            e2.latLonToProj(el2.getLatitude().getValue(),
                            el2.getLongitude().getValue());
        double dist = p1.distance(p2);

        if ( !right) {
            sign = -1;
        }
        double x = p2.getX() + sign * r * (p2.getY() - p1.getY()) / dist;
        double y = p2.getY() + sign * r * (p1.getX() - p2.getX()) / dist;


        ProjectionPoint pp   = new ProjectionPointImpl(x, y);
        LatLonPointImpl lp   = new LatLonPointImpl();
        FlatEarth       e3   = new FlatEarth();
        LatLonPoint     lp11 = e3.projToLatLon(pp, lp);

        EarthLocation el = new EarthLocationLite(lp11.getLatitude(),
                               lp11.getLongitude(), 0);
        StormTrackPoint sp = new StormTrackPoint(el, sp1.getTrackPointTime(),
                                 0, null);
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

        FlatEarth e1 = new FlatEarth();
        ProjectionPointImpl p1 = e1.latLonToProj(c.getLatitude().getValue(),
                                     c.getLongitude().getValue());
        FlatEarth e2 = new FlatEarth();
        ProjectionPointImpl p2 = e2.latLonToProj(d.getLatitude().getValue(),
                                     d.getLongitude().getValue());

        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();

        double a  = Math.atan2(dy, dx);

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


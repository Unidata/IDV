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
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;

import ucar.visad.display.*;

import visad.*;

import java.awt.*;

import java.awt.Color;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class WayDisplayState {

    /** _more_          */
    private StormDisplayState stormDisplayState;

    /** _more_          */
    private JCheckBox visibilityCbx;

    /** _more_          */
    private JCheckBox ringsCbx;

    /** _more_ */
    private Way way;

    /** _more_ */
    private boolean visible = true;

    /** _more_ */
    private boolean ringsVisible = false;

    /** _more_ */
    List<Displayable> displayables = new ArrayList<Displayable>();

    /** _more_          */
    List<Displayable> ringsDisplayables = new ArrayList<Displayable>();

    /** _more_          */
    private List<StormTrack> tracks = new ArrayList<StormTrack>();

    /** _more_          */
    private List<FieldImpl> fields = new ArrayList<FieldImpl>();

    /** _more_          */
    private List<DateTime> times = new ArrayList<DateTime>();

    /** _more_          */
    private List<PointOb> pointObs = new ArrayList<PointOb>();

    /** _more_          */
    private Color color;

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
    }

    /**
     * _more_
     */
    public void deactivate() {
        displayables      = new ArrayList<Displayable>();
        ringsDisplayables = new ArrayList<Displayable>();
        tracks            = new ArrayList<StormTrack>();
        fields            = new ArrayList<FieldImpl>();
        times             = new ArrayList<DateTime>();
        pointObs          = new ArrayList<PointOb>();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JCheckBox getVisiblityCheckBox() {
        if (visibilityCbx == null) {
            visibilityCbx = new JCheckBox("Visible", getVisible());
            visibilityCbx.setToolTipText("Show/Hide Track");
            visibilityCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        setVisible(visibilityCbx.isSelected());
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
    public JCheckBox getRingsVisiblityCheckBox() {
        if (ringsCbx == null) {
            //            ringsCbx = new JCheckBox("Rings",GuiUtils.getImageIcon("/ucar/unidata/idv/control/storm/Rings16.gif"), getRingsVisible());
            ringsCbx = new JCheckBox("Rings", getRingsVisible());
            ringsCbx.setToolTipText("Show Rings");
            ringsCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        setRingsVisible(ringsCbx.isSelected());
                    } catch (Exception exc) {
                        LogUtil.logException("Toggling way visibility", exc);
                    }
                }
            });

        }
        return ringsCbx;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<PointOb> getPointObs() {
        return pointObs;
    }

    /** _more_          */
    private static TextType textType;

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
     * @throws Exception _more_
     */
    public void addDisplayable(Displayable displayable) throws Exception {
        displayables.add(displayable);
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
     * @param displayable _more_
     *
     * @throws Exception _more_
     */
    public void addRingsDisplayable(Displayable displayable)
            throws Exception {
        ringsDisplayables.add(displayable);
        setRingVisibility(displayable);
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
     * Set the Visible property.
     *
     * @param value The new value for Visible
     *
     * @throws Exception _more_
     */
    public void setVisible(boolean value) throws Exception {
        this.visible = value;
        for (Displayable displayable : displayables) {
            if (way.isObservation()) {
                displayable.setVisible(getVisible());
            } else {
                displayable.setVisible(
                    getVisible() && stormDisplayState.getForecastVisible());
            }
        }
        //setRingsVisible(ringsVisible);
    }

    /**
     * Set the Visible property.
     *
     * @param value The new value for Visible
     *
     * @throws Exception _more_
     */
    public void setRingsVisible(boolean value) throws Exception {
        this.ringsVisible = value;
        for (Displayable displayable : ringsDisplayables) {
            setRingVisibility(displayable);
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



}


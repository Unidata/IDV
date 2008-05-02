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
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.LogUtil;

import ucar.visad.display.*;

import ucar.unidata.util.ColorTable;
import ucar.unidata.ui.colortable.ColorTableManager;

import java.rmi.RemoteException;
import visad.*;

import java.awt.*;

import java.awt.Color;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

import ucar.unidata.ui.colortable.ColorTableDefaults;


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

    /** _more_ */
    private Way way;

    /** _more_ */
    private boolean visible = true;

    private boolean ringsVisible = true;

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


    private JComboBox paramBox;

    private GuiUtils.ColorSwatch colorSwatch;

    private CompositeDisplayable holder;

    private CompositeDisplayable ringsHolder;

    private  TrackDisplayable trackDisplay;

    private String colorTable="Bright38";



    /**
     * _more_
     */
    public WayDisplayState() {}


    private RealType ringsType;

    protected CompositeDisplayable getHolder() throws VisADException, RemoteException {
        if(holder == null) {
            holder  = new CompositeDisplayable("way  holder");
            stormDisplayState.addDisplayable(holder);
        }
        return holder;
    }


    protected void setRings(RealType ringsType, List<RingSet> rings) throws VisADException, RemoteException {
        this.ringsType= ringsType;
        if(ringsHolder==null) {
            ringsHolder  = new CompositeDisplayable("rings holder");
            addDisplayable(ringsHolder);
        }
        ringsHolder.clearDisplayables();
        if(rings!=null) {
            for(RingSet ring: rings) {
                ringsHolder.addDisplayable(ring);
            }
        }
        setRingsVisible(getRingsVisible());
    }

    
    public TrackDisplayable getTrackDisplay() throws Exception {
        if(trackDisplay ==null) {
            trackDisplay = new TrackDisplayable("track_"
                                                + stormDisplayState.getStormInfo().getStormId());
            if(way.isObservation()) {
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


    protected JComponent getColorSwatch() {
        if(colorSwatch == null) {
            colorSwatch = new GuiUtils.ColorSwatch(getColor(), "Set track color") {
                    public void setBackground(Color newColor) {
                        super.setBackground(newColor);
                        WayDisplayState.this.color = newColor;
                        if(trackDisplay!=null) {
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

    public  float[][] getColorPalette() {
        RealType type = getParamType();
        if(type!=null && colorTable!=null) {
            ColorTable ct =
                stormDisplayState.getStormTrackControl().getControlContext().getColorTableManager().getColorTable(
                                                                                                                  colorTable);
            if(ct!=null) {
                return stormDisplayState.getStormTrackControl().getColorTableForDisplayable(ct);
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
    public static  float[][] getColorPalette(Color c) {
        if (c == null) {
            c = Color.red;
        }
        return ColorTableDefaults.allOneColor(c, true);
    }

    protected Component getParamComponent(Vector attrNames) {
        if(attrNames==null || attrNames.size()==0) return GuiUtils.filler();
        if(paramBox == null) {
            paramBox =  new JComboBox(attrNames);
            paramBox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            makeField();
                            setTrackColor();
                            stormDisplayState.wayParamChanged(WayDisplayState.this);
                        } catch (Exception exc) {
                            LogUtil.logException("Making new field", exc);
                        }
                    }
                });
        }
        return paramBox;
    }


    private void setTrackColor() throws Exception {
        if(trackDisplay!=null) {
            trackDisplay.setColorPalette(getColorPalette());
        }
    }

    public boolean usingDefaultParam() {
        if(paramBox == null) return false;
        TwoFacedObject tfo = (TwoFacedObject) paramBox.getSelectedItem();
        Object id = tfo.getId();
        if(id == null) return false;
        if(id instanceof RealType) {
            return false;
        }
        return id.toString().equals("default");
    }

    //    protected ColorTable getParamColorTable() {
    //    }

    protected RealType getParamType() {
        if(paramBox == null) return null;
        TwoFacedObject tfo = (TwoFacedObject) paramBox.getSelectedItem();
        Object id = tfo.getId();
        if(id == null) return null;
        if(id instanceof RealType) {
            return (RealType) id;
        }
        if(id.toString().equals("default")) {
            return stormDisplayState.getForecastParamType();
        }
        return null;
    }


    /**
     * _more_
     */
    public void deactivate()  throws VisADException , RemoteException{
        ringsHolder = null;
        if(holder!=null) {
            
        }
        trackDisplay = null;
        holder = null;
        ringsHolder = null;
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
            visibilityCbx = new JCheckBox("", getVisible());
            visibilityCbx.setToolTipText("Show/Hide Track");
            visibilityCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        setVisible(visibilityCbx.isSelected());
                        stormDisplayState.wayVisibilityChanged(WayDisplayState.this);
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
    public void addTrack(StormTrack track) throws Exception {
        tracks.add(track);
    }


    protected void makeField() throws Exception  {
        List<FieldImpl> fields = new ArrayList<FieldImpl>();
        List<DateTime> times = new ArrayList<DateTime>();
        
        RealType type = getParamType();
        for(StormTrack track: tracks) {
            FieldImpl field = stormDisplayState.makeField(track, type);
            fields.add(field);
            times.add(track.getTrackStartTime());
        }

        if (fields.size() == 0) {
            return;
        }

        FieldImpl timeField =
            ucar.visad.Util.makeTimeField(fields,
                                          times);
        getTrackDisplay().setTrack(timeField);
    }


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
    public void addDisplayable(Displayable displayable) throws VisADException, RemoteException  {

        getHolder().addDisplayable(displayable);
        if (way.isObservation()) {
            displayable.setVisible(getVisible());
        } else {
            displayable.setVisible(getVisible()
                                   && stormDisplayState.getForecastVisible());
        }
    }




    public void checkVisibility()  throws Exception {
        if(holder!=null) {
            for (Iterator iter = holder.iterator(); iter.hasNext(); ) {
                Displayable displayable = (Displayable) iter.next();
                if (way.isObservation()) {
                    displayable.setVisible(getVisible());
                } else {
                    displayable.setVisible(
                                           getVisible() && stormDisplayState.getForecastVisible());
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
     * @throws Exception _more_
     */
    public void setRingsVisible(boolean value) throws VisADException, RemoteException {
        this.ringsVisible = value;
        if(ringsHolder!=null) {
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
Set the ColorTable property.

@param value The new value for ColorTable
**/
public void setColorTable (String value) {
	colorTable = value;
}

/**
Get the ColorTable property.

@return The ColorTable
**/
public String getColorTable () {
	return colorTable;
}



}


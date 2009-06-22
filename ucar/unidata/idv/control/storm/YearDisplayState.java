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


import org.w3c.dom.*;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.data.storm.*;
import ucar.unidata.geoloc.LatLonPointImpl;


import ucar.unidata.ui.symbol.*;



import ucar.unidata.util.DateUtil;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;


import ucar.unidata.xml.XmlUtil;

import ucar.visad.*;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.display.*;


import ucar.visad.display.*;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.SelectRangeDisplayable;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.StationModelDisplayable;
import ucar.visad.display.TrackDisplayable;



import visad.*;

import visad.georef.EarthLocation;

import visad.georef.EarthLocationLite;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.beans.*;

import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class YearDisplayState {

    /** _more_ */
    public static final int STATE_INACTIVE = 0;

    /** _more_ */
    public static final int STATE_LOADING = 1;

    /** _more_ */
    public static final int STATE_ACTIVE = 2;

    /** _more_ */
    private static int[] nextColor = { 0 };

    /** _more_ */
    private StormTrackControl stormTrackControl;

    /** _more_ */
    private int year;

    /** _more_ */
    private Color color;


    /** _more_ */
    private int state = STATE_INACTIVE;

    /** _more_ */
    private TrackDisplayable trackDisplay;

    /** _more_ */
    private StationModelDisplayable labelDisplay;

    /** _more_ */
    private List<StormTrack> stormTracks = new ArrayList<StormTrack>();


    /** _more_ */
    private JLabel label;

    /** _more_ */
    private JButton button;

    /** _more_ */
    private GuiUtils.ColorSwatch colorSwatch;

    /**
     * _more_
     */
    public YearDisplayState() {}


    /**
     * _more_
     *
     *
     * @param stormTrackControl _more_
     * @param year _more_
     *
     */
    public YearDisplayState(StormTrackControl stormTrackControl, int year) {
        this.stormTrackControl = stormTrackControl;
        this.year              = year;
        color                  = StormDisplayState.getNextColor(nextColor);
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
                    YearDisplayState.this.color = newColor;
                    if (trackDisplay != null) {
                        try {
                            trackDisplay.setColor(newColor);
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
    public String toString() {
        return "" + year;
    }

    /** _more_ */
    private List pointObs;

    /**
     * _more_
     *
     * @return _more_
     */
    protected List getPointObs() {
        return pointObs;
    }

    /**
     * _more_
     *
     * @param doYearTime _more_
     * @param tracks _more_
     * @param times _more_
     * @param fields _more_
     * @param pointObs _more_
     *
     * @throws Exception _more_
     */
    public void setData(boolean doYearTime, List<StormTrack> tracks,
                        List times, List fields, List pointObs)
            throws Exception {
        this.pointObs = pointObs;
        stormTracks.clear();
        stormTracks.addAll(tracks);
        if (trackDisplay == null) {
            trackDisplay = new TrackDisplayable("year track ");
            trackDisplay.setLineWidth(2);
            stormTrackControl.addDisplayable(trackDisplay);
            trackDisplay.setColor(color);
            /*            labelDisplay =
                new StationModelDisplayable("storm year labels");
            labelDisplay.setScale(
                stormTrackControl.getDisplayScale());
            StationModelManager smm =
                stormTrackControl.getControlContext().getStationModelManager();
            StationModel model = smm.getStationModel("Location");
            labelDisplay.setStationModel(model);
            stormTrackControl.addDisplayable(labelDisplay);*/
        }

        if (doYearTime) {
            DateTime dttm = (DateTime) times.get(0);
            trackDisplay.setOverrideAnimationSet(Misc.newList(dttm));
            Data[] datas = (Data[]) fields.toArray(new Data[fields.size()]);
            times =
                Misc.newList(new DateTime(dttm.cloneButValue(dttm.getValue()
                    - 1000)), dttm,
                              new DateTime(dttm.cloneButValue(dttm.getValue()
                                  + 1000)));
            FieldImpl indexField = Util.indexedField(datas, false);
            fields = Misc.newList(indexField, indexField, indexField);
            trackDisplay.setTrack(Util.makeTimeField(fields, times));
            //            System.err.println ("field:" + Util.makeTimeField(fields, times));
        } else {
            trackDisplay.setOverrideAnimationSet((List) null);
            trackDisplay.setTrack(Util.makeTimeField(fields, times));
            //            System.err.println ("no year");
            //            labelDisplay.setStationData(
            //                                        PointObFactory.makeTimeSequenceOfPointObs(pointObs, -1, -1));
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JButton getButton() {
        if (button == null) {
            button = new JButton("");
            GuiUtils.setFixedWidthFont(button);
            setState(state);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (state == STATE_ACTIVE) {
                        state = STATE_INACTIVE;
                        unload();
                    } else if (state == STATE_LOADING) {
                        state = STATE_INACTIVE;
                    } else if (state == STATE_INACTIVE) {
                        state = STATE_LOADING;
                        stormTrackControl.loadYear(YearDisplayState.this);
                    }
                    setState(state);
                }
            });
        }
        return button;
    }


    /**
     * _more_
     *
     * @param msg _more_
     */
    public void setStatus(String msg) {
        getLabel().setText(msg);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JLabel getLabel() {
        if (label == null) {
            label = new JLabel("");
        }
        return label;
    }


    /**
     * _more_
     */
    public void unload() {
        if (trackDisplay != null) {
            try {
                stormTrackControl.removeDisplayable(trackDisplay);
                if (labelDisplay != null) {
                    stormTrackControl.removeDisplayable(labelDisplay);
                }
                stormTrackControl.unloadYear(this);
            } catch (Exception exc) {
                LogUtil.logException("Unloading tracks", exc);
            }
            trackDisplay = null;
            labelDisplay = null;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormTrack> getStormTracks() {
        return stormTracks;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public StormTrackControl getStormTrackControl() {
        return stormTrackControl;
    }


    /**
     * _more_
     *
     * @param stormTrackControl _more_
     */
    public void setStormTrackControl(StormTrackControl stormTrackControl) {
        this.stormTrackControl = stormTrackControl;
    }

    /**
     * Set the Year property.
     *
     * @param value The new value for Year
     */
    public void setYear(int value) {
        year = value;
    }

    /**
     * Get the Year property.
     *
     * @return The Year
     */
    public int getYear() {
        return year;
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
     * Get the Active property.
     *
     * @return The Active
     */
    public boolean getActive() {
        return state == STATE_ACTIVE;
    }


    /**
     * Set the State property.
     *
     * @param value The new value for State
     */
    public void setState(int value) {
        state = value;
        if (button != null) {
            if (state == STATE_ACTIVE) {
                button.setText("Unload");
            } else if (state == STATE_LOADING) {
                button.setText("Cancel");
            } else if (state == STATE_INACTIVE) {
                button.setText("Load  ");
            }
        }
    }

    /**
     * Get the State property.
     *
     * @return The State
     */
    public int getState() {
        return state;
    }


}



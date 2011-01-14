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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.data.storm.*;




import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.idv.control.chart.*;




import ucar.unidata.ui.drawing.*;
import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.*;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.display.*;


import ucar.visad.display.*;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.SelectRangeDisplayable;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.TrackDisplayable;



import visad.*;

import visad.bom.Radar2DCoordinateSystem;

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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;



/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormTrackTableModel extends AbstractTableModel {

    /** _more_ */
    private StormDisplayState stormDisplayState;

    /** _more_ */
    private StormTrack track;

    /** _more_ */
    private List<StormTrackPoint> points;

    /** _more_ */
    private List<StormParam> params;

    /**
     * _more_
     *
     * @param stormDisplayState _more_
     * @param track _more_
     */
    public StormTrackTableModel(StormDisplayState stormDisplayState,
                                StormTrack track) {
        this.stormDisplayState = stormDisplayState;
        this.track             = track;
        this.points            = track.getTrackPoints();
        List<StormParam> tmp = track.getParams();
        this.params = new ArrayList<StormParam>();
        for (StormParam param : tmp) {
            if ( !param.getDerived()) {
                this.params.add(param);
            }
        }
    }


    /**
     * _more_
     *
     * @param rowIndex _more_
     * @param columnIndex _more_
     *
     * @return _more_
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        //  if (true) {
        //      return false;
        //  }
        if (columnIndex == 0) {
            return false;
        }
        //        return stormDisplayState.getStormTrackControl().isEditable();
        return stormDisplayState.getStormTrackControl().getEditMode();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public StormTrack getStormTrack() {
        return track;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getRowCount() {
        return points.size();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getColumnCount() {
        return 3 + params.size();
    }

    /**
     * _more_
     *
     * @param aValue _more_
     * @param rowIndex _more_
     * @param column _more_
     */
    public void setValueAt(Object aValue, int rowIndex, int column) {
        StormTrackPoint stp = points.get(rowIndex);
        if (column == 0) {
            return;
        } else if (column == 1) {
            //latitude
        } else if (column == 2) {
            //longitude
        } else {
            StormParam param    = params.get(column - 3);
            Real       r        = stp.getAttribute(param);
            double     newValue = new Double(aValue.toString()).doubleValue();
            //Set the value
            Real rr = null;
            try {
                rr = r.cloneButValue(newValue);

            } catch (VisADException ep) {}

            stp.setAttribute(rr);
        }
        stormDisplayState.markHasBeenEdited();
    }

    /**
     * _more_
     *
     * @param row _more_
     * @param column _more_
     *
     * @return _more_
     */
    public Object getValueAt(int row, int column) {
        if (row >= points.size()) {
            return "";
        }
        StormTrackPoint stp = points.get(row);
        if (column == 0) {
            if (track.getWay().isObservation()) {
                return stp.getTime();
            }
            return "" + stp.getForecastHour();
        }
        if (column == 1) {
            return stp.getLocation().getLatitude();
        }
        if (column == 2) {
            return stp.getLocation().getLongitude();
        }
        StormParam param = params.get(column - 3);
        Real       r     = stp.getAttribute(param);
        if (r != null) {
            return r.toString();
        }
        return "";
    }

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public String getColumnName(int column) {
        if (column == 0) {
            return track.getWay().isObservation()
                   ? "Time"
                   : "Hour";
        }
        if (column == 1) {
            return "Lat";
        }
        if (column == 2) {
            return "Lon";
        }
        StormParam param = params.get(column - 3);
        Unit       unit  = param.getUnit();
        return param.toString() + ((unit == null)
                                   ? ""
                                   : "[" + unit + "]");
    }



}



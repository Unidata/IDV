/*
 * $Id: DataChoiceWrapper.java,v 1.10 2007/04/16 21:32:10 jeffmc Exp $
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

package ucar.unidata.idv.control.chart;


import org.jfree.chart.*;
import org.jfree.chart.annotations.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.urls.*;
import org.jfree.data.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;



import ucar.unidata.data.DataChoice;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionRect;




import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import ucar.visad.GeoUtils;
import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;

import visad.georef.*;

import visad.util.BaseRGBMap;

import visad.util.ColorPreview;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;


import java.rmi.RemoteException;







import java.text.SimpleDateFormat;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 * Wraps a data choice. Holds extra chart state.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.10 $
 */
public class DataChoiceWrapper {

    /** which side in legend */
    public static final int SIDE_UNDEFINED = 0;

    /** which side in legend */
    public static final int SIDE_LEFT = 1;

    /** which side in legend */
    public static final int SIDE_RIGHT = 2;

    /** which side in legend */
    public static final int[] SIDES = { SIDE_UNDEFINED, SIDE_LEFT,
                                        SIDE_RIGHT };

    /** which side in legend */
    public static final String[] SIDELABELS = { "Default", "Left", "Right" };


    /** the data choice */
    private DataChoice dataChoice;


    /** which side in legend */
    private int side = SIDE_UNDEFINED;



    /** Holds graphics info */
    private LineState lineState = new LineState(null, 1.0f,
                                      LineState.STROKE_SOLID);

    /**
     * The wrapper can have its own description that is used
     *   instead of the data choice's
     */
    private String myDescription;


    /**
     * ctor
     */
    public DataChoiceWrapper() {}

    /**
     * ctor
     *
     * @param dataChoice The data choice to wrap
     */
    public DataChoiceWrapper(DataChoice dataChoice) {
        this.dataChoice = dataChoice;
    }

    /**
     *  Set the DataChoice property.
     *
     *  @param value The new value for DataChoice
     */
    public void setDataChoice(DataChoice value) {
        dataChoice = value;
    }

    /**
     *  Get the DataChoice property.
     *
     *  @return The DataChoice
     */
    public DataChoice getDataChoice() {
        return dataChoice;
    }


    /**
     * Either get the color defined for this wrapper or,
     * if null, return the index'ed color
     *
     *
     * @param index color index
     *
     * @return color
     */
    public Color getColor(int index) {
        Color c = getColor();
        if (c == null) {
            c = LineState.getFixedColor(index);
        }
        return c;
    }


    /**
     * Get the color from the line state
     *
     * @return the color
     */
    public Color getColor() {
        return lineState.getColor();
    }

    /**
     * Get the name from the data choice
     *
     * @return the name
     */
    public String getName() {
        return dataChoice.getName();
    }

    /**
     * Get the description either from me or from the data choice
     *
     * @return the description
     */
    public String getDescription() {
        if ((myDescription != null) && (myDescription.trim().length() > 0)) {
            return myDescription;
        }
        if (dataChoice != null) {
            return dataChoice.getDescription();
        }
        return "";
    }


    /**
     * A utility to format the description with the given unit.
     *
     * @param unit The unit. May be null.
     *
     * @return The label
     */
    public String getLabel(Unit unit) {
        return getDescription() + ((unit == null)
                                   ? " "
                                   : " [" + unit + "]");
    }


    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        return getDescription();
    }


    /**
     * Set the Side property.
     *
     * @param value The new value for Side
     */
    public void setSide(int value) {
        side = value;
    }

    /**
     * Get the Side property.
     *
     * @return The Side
     */
    public int getSide() {
        return side;
    }


    /**
     * Set the MyDescription property.
     *
     * @param value The new value for MyDescription
     */
    public void setMyDescription(String value) {
        if ((value != null) && (dataChoice != null)
                && Misc.equals(dataChoice.getDescription(), value)) {
            return;
        }
        myDescription = value;
    }

    /**
     * Get the MyDescription property.
     *
     * @return The MyDescription
     */
    public String getMyDescription() {
        return myDescription;
    }


    /**
     * Set the LineState property.
     *
     * @param value The new value for LineState
     */
    public void setLineState(LineState value) {
        lineState = value;
    }

    /**
     * Get the LineState property.
     *
     * @return The LineState
     */
    public LineState getLineState() {
        return lineState;
    }


}


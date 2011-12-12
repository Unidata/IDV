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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.radar.RadarConstants;
import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;

import ucar.visad.Util;
import ucar.visad.display.Contour2DDisplayable;



import ucar.visad.display.Displayable;
import ucar.visad.display.Grid2DDisplayable;


import visad.*;

import visad.data.units.Parser;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * Given an earth-locared 3D VisAD Field of WSR-88D II data with a time domain,
 * make a 2D plot of the radar data against height and
 * time for one location on the Earth, and make related controls.
 * The display is in its own window; there is also a related selector point on
 * the other main map display which allows user to select and move the
 * sample location on Earth.
 *
 * @author IDV development team
 * @version $Revision: 1.8 $
 */
public class Level2TimeHeightControl extends TimeHeightControl {

    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>TimeHeightControl</code>
     */
    public Level2TimeHeightControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL | FLAG_COLOR);
    }

    // get (and make if necessary)
    // the requester Hastable of properties that is carried along with
    // the data instance

    /**
     * Get the request properties hash table
     * @return  hashtable of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        //System.out.println("  getRequestProperties  ");
        props.put(RadarConstants.PROP_TIMEHEIGHT, new Boolean(true));
        return props;
    }

    // make the requester Hastable of properties that is carried along with
    // the data instance

    /**
     * Set the request properties
     */
    protected void setRequestProperties() {
        getRequestProperties().put(RadarConstants.PROP_TIMEHEIGHT,
                                   new Boolean(true));
    }
}

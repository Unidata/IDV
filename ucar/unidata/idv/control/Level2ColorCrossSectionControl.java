/*
 * $Id: Level2ColorCrossSectionControl.java,v 1.7 2006/12/01 20:16:34 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.radar.RadarConstants;

import ucar.visad.display.DisplayableData;



import ucar.visad.display.Grid2DDisplayable;

import visad.*;

import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.Hashtable;

import java.util.List;

import javax.swing.JCheckBox;


/**
 * Class for displaying WSR-88D Level II cross sections as color shaded displays.
 * @author Unidata
 * @version $ $
 */
public class Level2ColorCrossSectionControl extends ColorCrossSectionControl {


    /**
     * Default constructor
     */
    public Level2ColorCrossSectionControl() {}


    // get (and make if necessary)
    // the requester Hastable of properties that is carried along with
    // the data instance

    /**
     * Get the request properties hash table.
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        //System.out.println("  getRequestProperties PROP_VCS ");
        props.put(RadarConstants.PROP_VCS, new Boolean(true));
        return props;
    }

    // make the requester Hastable of properties that is carried along with
    // the data instance

    /**
     * Set the request properties
     */
    protected void setRequestProperties() {
        getRequestProperties().put(RadarConstants.PROP_VCS,
                                   new Boolean(true));
    }
}


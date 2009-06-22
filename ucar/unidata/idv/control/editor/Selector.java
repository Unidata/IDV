/*
 * $Id: TransectDrawingControl.java,v 1.41 2006/12/28 19:50:59 jeffmc Exp $
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

package ucar.unidata.idv.control.editor;




import org.python.core.*;
import org.python.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.gis.MapMaker;
import ucar.unidata.data.gis.Transect;
import ucar.unidata.data.grid.GridDataInstance;

import ucar.unidata.data.radar.RadarConstants;

import ucar.unidata.geoloc.LatLonPointImpl;



import ucar.unidata.idv.control.drawing.*;


import ucar.unidata.ui.CommandManager;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.data.MapSet;

import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;



/**
 * Class Selector _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Selector {

    /** _more_ */
    public static final String TYPE_FIELD = "field";

    /** _more_ */
    public static final String TYPE_REGION_ALL = "region.all";

    /** _more_ */
    public static final String TYPE_REGION_SELECTED = "region.selected";


    /** _more_ */
    public static final String TYPE_RANGE = "range";


    /** _more_          */
    private String type;

    /** _more_          */
    private boolean inside = true;

    /** _more_          */
    private float min = 0;

    /** _more_          */
    private float max = 0;

    /**
     * _more_
     */
    public Selector() {}

    /**
     * _more_
     *
     * @param type _more_
     * @param inside _more_
     */
    public Selector(String type, boolean inside) {
        this.type   = type;
        this.inside = inside;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRegion() {
        return isRegion(type);
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public static boolean isRegion(String type) {
        return type.equals(TYPE_REGION_SELECTED)
               || type.equals(TYPE_REGION_ALL);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRange() {
        return isRange(type);
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public static boolean isRange(String type) {
        return type.equals(TYPE_RANGE);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (type.equals(TYPE_FIELD)) {
            return "entire field";
        }
        if (type.equals(TYPE_REGION_ALL)) {
            return "all regions";
        }
        if (type.equals(TYPE_REGION_SELECTED)) {
            return "selected regions";
        }
        if (type.equals(TYPE_RANGE)) {
            return "within range (" + min + "," + max + ")";
        }
        return "???";
    }





    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     *  Set the Inside property.
     *
     *  @param value The new value for Inside
     */
    public void setInside(boolean value) {
        inside = value;
    }

    /**
     *  Get the Inside property.
     *
     *  @return The Inside
     */
    public boolean getInside() {
        return inside;
    }


    /**
     *  Set the Min property.
     *
     *  @param value The new value for Min
     */
    public void setMin(float value) {
        min = value;
    }

    /**
     *  Get the Min property.
     *
     *  @return The Min
     */
    public float getMin() {
        return min;
    }

    /**
     *  Set the Max property.
     *
     *  @param value The new value for Max
     */
    public void setMax(float value) {
        max = value;
    }

    /**
     *  Get the Max property.
     *
     *  @return The Max
     */
    public float getMax() {
        return max;
    }


}



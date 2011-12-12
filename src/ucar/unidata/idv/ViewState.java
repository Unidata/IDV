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

package ucar.unidata.idv;


import org.w3c.dom.*;

import ucar.unidata.collab.*;
import ucar.unidata.data.GeoLocationInfo;

import ucar.unidata.data.gis.KmlDataSource;
import ucar.unidata.idv.publish.PublishManager;


import ucar.unidata.idv.ui.*;


import ucar.unidata.java3d.LightInfo;
import ucar.unidata.ui.Command;
import ucar.unidata.ui.CommandManager;
import ucar.unidata.ui.DropPanel;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.ui.ImagePanel;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.Timeline;
import ucar.unidata.ui.drawing.Glyph;

import ucar.unidata.util.BooleanProperty;
import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.*;
import ucar.unidata.view.geoloc.*;


import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.Plotter;




import ucar.visad.Util;

import ucar.visad.display.*;

import visad.*;

import visad.bom.SceneGraphRenderer;

import visad.georef.*;

import visad.java3d.*;

import visad.java3d.DisplayImplJ3D;


import visad.util.PrintActionListener;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.io.*;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.*;


import javax.media.j3d.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import javax.vecmath.*;






/**
 *
 * @author IDV development team
 */

public class ViewState {

    /** _more_          */
    public String viewClassName;

    /** _more_          */
    public static String PROP_MATRIX = "matrix";

    /** _more_          */
    public static String PROP_PROJECTION = "projection";

    /** _more_          */
    public static String PROP_ASPECTRATIO = "aspectratio";

    /** _more_          */
    public static String PROP_GLOBE = "globe";

    /** _more_          */
    private String name;

    /** _more_          */
    Hashtable properties = new Hashtable();

    /** _more_          */
    private boolean isLocal = true;

    /**
     * _more_
     */
    public ViewState() {}

    /**
     * _more_
     *
     * @param viewClassName _more_
     */
    public ViewState(String viewClassName) {
        this.viewClassName = viewClassName;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name;
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void put(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object get(Object key) {
        return properties.get(key);
    }


    /**
     *  Set the Properties property.
     *
     *  @param value The new value for Properties
     */
    public void setProperties(Hashtable value) {
        this.properties = value;
    }

    /**
     *  Get the Properties property.
     *
     *  @return The Properties
     */
    public Hashtable getProperties() {
        return this.properties;
    }


    /**
     *  Set the ViewClassName property.
     *
     *  @param value The new value for ViewClassName
     */
    public void setViewClassName(String value) {
        this.viewClassName = value;
    }

    /**
     *  Get the ViewClassName property.
     *
     *  @return The ViewClassName
     */
    public String getViewClassName() {
        return this.viewClassName;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return this.name;
    }

    /**
     *  Set the IsLocal property.
     *
     *  @param value The new value for IsLocal
     */
    public void setIsLocal(boolean value) {
        this.isLocal = value;
    }

    /**
     *  Get the IsLocal property.
     *
     *  @return The IsLocal
     */
    public boolean getIsLocal() {
        return this.isLocal;
    }



}

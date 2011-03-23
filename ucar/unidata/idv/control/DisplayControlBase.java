/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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


import org.w3c.dom.Element;

import ucar.unidata.collab.Sharable;
import ucar.unidata.collab.SharableImpl;

import ucar.unidata.data.DataCancelException;
import ucar.unidata.data.DataChangeListener;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.DisplayInfo;

import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewContext;

import ucar.unidata.idv.ui.IdvUIManager;


import ucar.unidata.metdata.NamedStationImpl;

import ucar.unidata.ui.Help;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.ColorTable;


import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;


import ucar.unidata.util.Range;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlPersistable;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.UtcDate;
import ucar.visad.Util;

import ucar.visad.display.*;

import ucar.visad.display.DisplayMaster;


import visad.*;

import visad.georef.EarthLocation;


import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.georef.MapProjection;


import java.awt.*;
import java.awt.event.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;



import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * Some display control oriented utilities
 * @author IDV development team
 * @version $Revision: 1.28 $
 */
public abstract class DisplayControlBase extends SharableImpl {

    /** Use this member to log messages (through calls to LogUtil) */
    public static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(DisplayControlBase.class.getName());


    /** The icon used to show locked legend components */
    protected static ImageIcon ICON_LOCK = IdvUIManager.ICON_LOCK;

    /** The icon used to show unlocked legend components */
    protected static ImageIcon ICON_UNLOCK = IdvUIManager.ICON_UNLOCK;


    /** Icon used to remove displays */
    public static ImageIcon ICON_REMOVE = IdvUIManager.ICON_REMOVE;



    /** Name of the property removed propert */
    public static final String PROP_REMOVED = "prop.removed";


    /** The preference id for the default label template for displays */
    public static final String PREF_LEGENDLABEL_TEMPLATE =
        "idv.legendlabel.template";

    /** The preference id for the default extra label template for displays */
    public static final String PREF_EXTRALABEL_TEMPLATE =
        "idv.extralabel.template";

    /** The preference id for the default label template for displays */
    public static final String PREF_DISPLAYLIST_TEMPLATE =
        "idv.displaylist.template";


    /** Macro for the short parameter name for the label */
    public static final String MACRO_SHORTNAME = "%shortname%";

    /** Macro for station */
    public static final String MACRO_STATION = "%station%";


    /** Macro for the long parameter name for the label */
    public static final String MACRO_LONGNAME = "%longname%";

    /** Macro for the short parameter name for the label */
    public static final String MACRO_VALUE = "%value%";

    /** Macro for the short parameter name for the label */
    public static final String MACRO_POSITION = "%position%";


    /** Macro for the data source name (or names) for the label */
    public static final String MACRO_DATASOURCENAME = "%datasourcename%";

    /** Macro for the display  name for the label */
    public static final String MACRO_DISPLAYNAME = "%displayname%";

    /** Macro for the display  unit for the label */
    public static final String MACRO_DISPLAYUNIT = "%displayunit%";

    /** Macro for the display  unit for the label */
    public static final String MACRO_TIMESTAMP = UtcDate.MACRO_TIMESTAMP;

    /** forecast hour macro */
    public static final String MACRO_FHOUR = "%fhour%";

    /** String used as the action command for the color setting button */
    public static final String CMD_COLORS = "cmd.colors";

    /** String used as the action command for removing this display control */
    public static final String CMD_REMOVE = "cmd.remove";

    /** String used as the action command for showing the control window */
    public static final String CMD_POPUP = "cmd.thispopup";

    /** String used as the action command for  setting the color table */
    public static final String CMD_COLORTABLE = "cmd.colortbl";


    /** String used as the action command for setting the size of wind barbs */
    public static final String CMD_BARBSIZE = "cmd.barbsize";

    /** String used as the action command for  setting the wind barb interval */
    public static final String CMD_INTERVAL = "cmd.interval";

    /** String used as the action command for setting the level */
    public static final String CMD_LEVEL = "cmd.level";

    /** String used as the action command for setting the level */
    public static final String CMD_LINEWIDTH = "cmd.linewidth";

    /** String used  to set the sampling to be weighted average */
    public static final String WEIGHTED_AVERAGE = "Weighted Average";

    /** String used  the set the sampling to be nearest neighbor */
    public static final String NEAREST_NEIGHBOR = "Nearest Neighbor";

    /** What is the default samppling mode */
    protected static String DEFAULT_SAMPLING_MODE = WEIGHTED_AVERAGE;

    /** Used for doing doLayout calls */
    protected static final Insets GRID_INSETS = new Insets(4, 4, 4, 4);


    /** Probe position property */
    public static final String SHARE_POSITION =
        "DisplayControlImpl.SHARE_POSITION";

    /** Transect Line position property */
    public static final String SHARE_TRANSECT =
        "DisplayControlImpl.SHARE_TRANSECT";

    /** Display unit property */
    public static final String SHARE_DISPLAYUNIT =
        "DisplayControlImpl.SHARE_DISPLAYUNIT";

    /** Visiblity property */
    public static final String SHARE_VISIBILITY =
        "DisplayControlImpl.SHARE_VISIBILITY";

    /** Color table property */
    public static final String SHARE_COLORTABLE =
        "DisplayControlImpl.SHARE_COLORTABLE";

    /** Color scale property */
    public static final String SHARE_COLORSCALE =
        "DisplayControlImpl.SHARE_COLORSCALE";

    /** Color property */
    public static final String SHARE_COLOR = "DisplayControlImpl.SHARE_COLOR";

    /** select range property */
    public static final String SHARE_SELECTRANGE =
        "DisplayControlImpl.SHARE_SELECTRANGE";

    /** data choices property */
    public static final String SHARE_CHOICES =
        "DisplayControlImpl.SHARE_CHOICES";

    /** Probe position property */
    public static final String SHARE_SKIPVALUE =
        "DisplayControlImpl.SHARE_SKIPVALUE";

    /** Bitmask for controls that use contour interval information */
    public static final int FLAG_CONTOUR = 1 << 1;

    /** Bitmask for controls that use colors */
    public static final int FLAG_COLOR = 1 << 2;

    /** Bitmask for controls that use color tables */
    public static final int FLAG_COLORTABLE = 1 << 3;

    /** Bitmask for controls that have the "Set data" button capability enabled */
    public static final int FLAG_DATACONTROL = 1 << 4;

    /** Bitmask for controls that can change their display units */
    public static final int FLAG_DISPLAYUNIT = 1 << 5;

    /** Bitmask for controls that can change their color units */
    public static final int FLAG_COLORUNIT = 1 << 6;

    /** Bitmask for controls that have a fixed z position */
    public static final int FLAG_ZPOSITION = 1 << 7;

    /** Bitmask for controls that allow selecting a data range */
    public static final int FLAG_SELECTRANGE = 1 << 8;

    /** Bitmask for controls that allow selecting a time range */
    public static final int FLAG_TIMERANGE = 1 << 9;

    /** Bitmask for controls that allow line width setting */
    public static final int FLAG_LINEWIDTH = 1 << 10;

    /** Bitmask for controls that allow skip factor setting */
    public static final int FLAG_SKIPFACTOR = 1 << 11;

    /** Bitmask for controls that allow texture quality setting */
    public static final int FLAG_TEXTUREQUALITY = 1 << 12;

    /** Bitmask for controls that allow grid smoothing setting */
    public static final int FLAG_SMOOTHING = 1 << 13;

    /** No-op for attributeFlags */
    public static final int FLAG_NONE = 0;

    /** Default selector point size. */
    public static final int DEFAULT_POINT_SIZE = 5;

    /** A label for None */
    public static final String LABEL_NONE = "None";


    /** search string for this */
    public static final String FIND_THIS = "this";

    /** search string for all */
    public static final String FIND_ALL = "all";

    /** search string for class: */
    public static final String FIND_CLASS = "class:";

    /** search string for category */
    public static final String FIND_CATEGORY = "category:";

    /** search string for displays with data */
    public static final String FIND_WITHDATA = "withdata";

    /** search string for display like this with data */
    public static final String FIND_WITHTHISDATA = "withthisdata";

    /** search string for display with this data */
    public static final String FIND_WITHTHISFIELD = "withthisfield";

    /** search string for special */
    public static final String FIND_SPECIAL = "special";

    /** search string with displays in this view */
    public static final String FIND_WITHTHISVIEW = "withthisview";


    /** display group setting */
    public static final String SETTINGS_GROUP_DISPLAY = "Display";

    /** group flags */
    public static final String SETTINGS_GROUP_FLAGS = "Flags";

    /** Temp properties */
    private Hashtable tmpProperties;


    /**
     * ctor
     */
    public DisplayControlBase() {}


    /**
     * Get the tmp property.
     *
     * @param key key
     *
     * @return property
     */
    public Object getTmpProperty(Object key) {
        if (tmpProperties != null) {
            return tmpProperties.get(key);
        }
        return null;
    }

    /**
     * put the tmp property. These are not persisted off
     *
     * @param key key
     * @param value value
     */
    public void putTmpProperty(Object key, Object value) {
        if (tmpProperties == null) {
            tmpProperties = new Hashtable();
        }
        tmpProperties.put(key, value);
    }


    /**
     * remove the tmp property
     *
     * @param key key
     *
     * @return the value or null if not found
     */
    public Object removeTmpProperty(Object key) {
        if (tmpProperties != null) {
            return tmpProperties.remove(key);
        }
        return null;
    }

    /**
     * A utility that takes a list
     * of NamedStationImpl-s, wraps each of them in  a TwoFacedObject,
     * truncates their label and adds them to the given combobox.
     *
     * @param stationList Station list
     * @param box Combo box to set
     */
    public static void setStations(List stationList, JComboBox box) {
        setStations(stationList, box, true);
    }

    /**
     * A utility that takes a list
     * of NamedStationImpl-s, wraps each of them in  a TwoFacedObject,
     * truncates their label and adds them to the given combobox.
     *
     * @param stationList Station list
     * @param box Combo box to set
     * @param addMessage  true to add an entry message
     */
    public static void setStations(List stationList, JComboBox box,
                                   boolean addMessage) {
        List tfos = new ArrayList();
        int  size = 30;
        for (int i = 0; i < stationList.size(); i++) {
            tfos.add(createStationTfo(stationList.get(i)));
        }
        if (addMessage) {
            tfos.add(0, "--Select a location--");
        }
        GuiUtils.setListData(box, tfos);
    }

    /**
     * A utility to wrap the given object (which should be a NamedStationImpl)
     * in a TwoFacedObject, truncating the label to a fixed size.
     *
     * @param o The station
     *
     * @return The tfo
     */
    public static TwoFacedObject createStationTfo(Object o) {
        int    size = 35;
        String label;
        if (o instanceof NamedStationImpl) {
            label = ((NamedStationImpl) o).getName();
            String id = ((NamedStationImpl) o).getID();
            if ((id != null) && !Misc.equals(id, label)) {
                label = label + " (" + id + ")";
            }
        } else {
            label = o.toString();
        }
        if (label.length() > size) {
            label = label.substring(0, size - 1);
        } else {
            label = StringUtil.padRight(label, size);
        }
        return new TwoFacedObject(label, o);
    }

    /**
     * A utility to find and return the NamedStationImpl
     * which is selected in the given combobox. May return null.
     *
     * @param box The box.
     * @return The selected station or null.
     */
    public static NamedStationImpl getSelectedStation(JComboBox box) {
        Object item = box.getSelectedItem();
        if ((item == null) || (item instanceof String)) {
            return null;
        }
        if (item instanceof TwoFacedObject) {
            return (NamedStationImpl) ((TwoFacedObject) item).getId();
        } else {
            return (NamedStationImpl) item;
        }
    }




    /**
     * Is the left button pressed
     *
     * @param event The event
     *
     * @return Is the left button pressed
     */
    public static boolean isLeftButtonDown(DisplayEvent event) {
        InputEvent inputEvent = event.getInputEvent();
        if (inputEvent == null) {
            return false;
        }
        int mods = inputEvent.getModifiers();
        return ((mods & InputEvent.BUTTON1_MASK) != 0);
    }

    /**
     * We have this here so some of the utility methods can get the navigated
     * display.
     *
     * @return The NavigatedDisplay this display control is in. My be null.
     */
    public abstract NavigatedDisplay getNavigatedDisplay();


    /**
     * Map the screen x/y of the event to an earth location
     *
     * @param event The event
     *
     * @return The earth location
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public EarthLocation toEarth(DisplayEvent event)
            throws VisADException, RemoteException {
        NavigatedDisplay d = getNavigatedDisplay();
        return (d == null)
               ? null
               : d.getEarthLocation(toBox(event));
    }


    /**
     * Map the visad box to an earth location
     *
     * @param boxCoords The box point
     *
     * @return The earth location
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public EarthLocation boxToEarth(double[] boxCoords)
            throws VisADException, RemoteException {
        return boxToEarth(boxCoords, true);
    }



    /**
     * Map the visad box to an earth location
     *
     * @param boxCoords The box point
     * @param  setZToZeroIfOverhead If in the overhead view then set Z to 0
     *
     * @return The earth location
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */

    public EarthLocation boxToEarth(double[] boxCoords,
                                    boolean setZToZeroIfOverhead)
            throws VisADException, RemoteException {
        return boxToEarth(boxCoords[0], boxCoords[1], boxCoords[2],
                          setZToZeroIfOverhead);
    }

    /**
     * Map the visad box to an earth location
     *
     * @param x x
     * @param y y
     * @param z z
     *
     * @return The earth location
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public EarthLocation boxToEarth(double x, double y, double z)
            throws VisADException, RemoteException {
        return boxToEarth(x, y, z, true);
    }



    /**
     * Map the visad box to an earth location
     *
     * @param x x
     * @param y y
     * @param z z
     * @param  setZToZeroIfOverhead If in the overhead view then set Z to 0
     *
     * @return The earth location
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public EarthLocation boxToEarth(double x, double y, double z,
                                    boolean setZToZeroIfOverhead)
            throws VisADException, RemoteException {
        NavigatedDisplay d = getNavigatedDisplay();
        return (d == null)
               ? null
               : d.getEarthLocation(x, y, z, setZToZeroIfOverhead);
    }



    /**
     * Map the visad box points to  earth locations
     *
     * @param boxPoints The box points
     *
     * @return The earth locations
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public List boxToEarth(List boxPoints)
            throws VisADException, RemoteException {
        List els = new ArrayList();
        for (int i = 0; i < boxPoints.size(); i++) {
            els.add(boxToEarth((double[]) boxPoints.get(i)));
        }
        return els;
    }



    /**
     * Map the earth locations to box points
     *
     * @param earthLocations  list of earth locations
     *
     * @return The box points
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public List earthToBox(List earthLocations)
            throws VisADException, RemoteException {
        List pts = new ArrayList();
        for (int i = 0; i < earthLocations.size(); i++) {
            pts.add(earthToBox((EarthLocation) earthLocations.get(i)));
        }
        return pts;
    }


    /**
     * Map an earth location to  a visad box point
     *
     * @param el The earth location
     *
     * @return The box point
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public double[] earthToBox(EarthLocation el)
            throws VisADException, RemoteException {
        return getNavigatedDisplay().getSpatialCoordinates(el, null);
    }


    /**
     * Map an earth location to  a visad box tuple
     *
     * @param el The earth location
     *
     * @return The tuple
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public RealTuple earthToBoxTuple(EarthLocation el)
            throws VisADException, RemoteException {
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        if (navDisplay == null) {
            return null;
        }
        return navDisplay.getSpatialCoordinates(el);
    }



    /**
     * Make an earth location from the given lat,on and alt
     *
     * @param lat the lat
     * @param lon the lon
     * @param alt the alt
     *
     * @return The earth location
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static EarthLocation makeEarthLocation(double lat, double lon,
            double alt)
            throws VisADException, RemoteException {
        return new EarthLocationTuple(new Real(RealType.Latitude, lat),
                                      new Real(RealType.Longitude, lon),
                                      new Real(RealType.Altitude, alt));
    }

    /**
     * Map the screen x/y to a visad box point
     *
     * @param event The event
     *
     * @return The box point
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public double[] toBox(DisplayEvent event)
            throws VisADException, RemoteException {
        return screenToBox(event.getX(), event.getY());
    }



    /**
     * Map the screen x/y to a visad box point
     *
     * @param screenX x
     * @param screenY y
     *
     * @return The box point
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public double[] screenToBox(int screenX, int screenY)
            throws VisADException, RemoteException {
        return getNavigatedDisplay().getSpatialCoordinatesFromScreen(screenX,
                screenY);
    }

    /**
     * Map the screen x/y to a visad box point
     *
     * @param screenX screen x
     * @param screenY screen y
     * @param zPosition The visad  Z position to use
     *
     * @return The box point
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public double[] screenToBox(int screenX, int screenY, double zPosition)
            throws VisADException, RemoteException {
        return getNavigatedDisplay().getSpatialCoordinatesFromScreen(screenX,
                screenY, zPosition);
    }

    /**
     * Return the x/y coordinate  of the center of the display
     *
     *
     * @return The box point
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public double[] getScreenCenter() throws VisADException, RemoteException {
        return getNavigatedDisplay().getScreenCenter();
    }


    /**
     * Map a visad box point to the screen coords.
     *
     * @param xyz Visad box point
     *
     * @return Screen coords
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public int[] boxToScreen(double[] xyz)
            throws VisADException, RemoteException {
        return getNavigatedDisplay().getScreenCoordinates(xyz);
    }


    /**
     * Map screen position to earth location
     *
     * @param screenX x
     * @param screenY y
     *
     * @return The earth location
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public EarthLocation screenToEarth(int screenX, int screenY)
            throws VisADException, RemoteException {
        return boxToEarth(screenToBox(screenX, screenY));
    }

    /**
     * Map earth to screen
     *
     * @param el Earth location
     *
     * @return Screen xy
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public int[] earthToScreen(EarthLocation el)
            throws VisADException, RemoteException {
        return boxToScreen(earthToBox(el));
    }



    /**
     * Get the screen bounds in Swing space
     *
     * @return the screen bounds or null if the display is null
     */
    Rectangle getScreenBounds() {
        NavigatedDisplay d = getNavigatedDisplay();
        if (d == null) {
            return null;
        }
        Component comp = d.getDisplayComponent();
        return (comp == null)
               ? null
               : comp.getBounds();
    }


    /**
     * Calculates the rectangle that is the bounds of the VisAD display.
     *
     * @return  bounds of the display
     */
    protected Rectangle2D calculateRectangle() {
        try {
            Rectangle2D.Double box = getNavigatedDisplay().getVisadBox();
            if ( !box.isEmpty()) {
                // pad rectangle by 5%
                double deltaWidth  = (double) (.05 * box.width);
                double deltaHeight = (double) (.05 * box.height);
                double newX        = box.x - deltaWidth;
                double newY        = box.y - deltaHeight;
                box.setRect(newX, newY, box.width + (2.0 * deltaWidth),
                            box.height + (2.0 * deltaHeight));
            }
            return box;
        } catch (Exception excp) {
            logException("calculating Rectangle ", excp);
            return new Rectangle2D.Double(0, 0, 0, 0);
        }
    }




    /**
     * Utility method for creating user messages
     *
     * @param msg   message to display
     */
    public static void userMessage(String msg) {
        LogUtil.userMessage(msg);
    }

    /**
     * A utility method to show an error message to the user.
     *
     * @param message The message
     */
    public void userErrorMessage(String message) {
        LogUtil.userErrorMessage(message);
    }

    /**
     * Utility method for logging exceptions.
     *
     * @param exc   Exception to handle
     */
    public static void logException(Exception exc) {
        logException("", exc);
    }

    /**
     * Utility method for logging exceptions with the given description.
     * This is just a wrapper around the {@link ucar.unidata.util.LogUtil}
     * call printException.
     *
     * @param desc  description for exception
     * @param exc   Exception to handle
     */
    public static void logException(String desc, Exception exc) {
        LogUtil.printException(log_, desc, exc);
    }



    /**
     * Format the levels to make them look presentable
     *
     * @param levels   array of levels
     *
     * @return formatted levels.  Currently an array of TwoFacedObjects
     *                            with formatted values as the label.
     */
    protected Object[] formatLevels(Object[] levels) {
        if (levels == null) {
            return null;
        }
        Object[] tfoList = new Object[levels.length];
        for (int i = 0; i < levels.length; i++) {
            tfoList[i] = getLabeledReal(levels[i]);
        }
        return tfoList;
    }


    /**
     * Get a labeled Real
     *
     * @param level the level object
     *
     * @return a labeled level as a TwoFacedObject
     */
    protected TwoFacedObject getLabeledReal(Object level) {
        if (level == null) {
            return (TwoFacedObject) level;
        }
        if (level instanceof TwoFacedObject) {
            Object lev = ((TwoFacedObject) level).getId();
            if (lev instanceof Real) {
                return (TwoFacedObject) level;
            }
        } else if (level instanceof Real) {
            return Util.labeledReal((Real) level);
        } else if (level instanceof String) {
            String tmp = (String) level;
            tmp = tmp.trim();
            if (tmp.startsWith("#")) {
                tmp = tmp.substring(1);
            }
            try {
                double value = Misc.parseValue(tmp);
                return new TwoFacedObject(tmp, new Real(value));
            } catch (Exception e) {}
        }
        userMessage("Unable to handle a level of type "
                    + level.getClass().getName() + " level=" + level);
        return null;
    }

    /**
     * Get the real value from a level object
     *
     * @param level  the level (TwoFacedObject, Real or number String);
     *
     * @return  a corresponding Real or null
     */
    protected Real getLevelReal(Object level) {
        if (level == null) {
            return (Real) level;
        }


        if (level instanceof TwoFacedObject) {
            Object lev = ((TwoFacedObject) level).getId();
            if (lev instanceof Real) {
                return (Real) lev;
            }
        } else if (level instanceof Real) {
            return (Real) level;
        } else if (level instanceof String) {
            /*
        String tmp = (String) level;
        tmp = tmp.trim();
        if (tmp.startsWith("#")) {
            tmp = tmp.substring(1);
        }
        try {
            double value = Misc.parseValue(tmp);
            return new Real(value);
        } catch (Exception e) {
            System.err.println("error parsing level: " + tmp + " " + e);
        }
        */
            try {
                return Util.toReal(level.toString());
            } catch (Exception e) {
                System.err.println("error parsing level: " + level + " " + e);
            }
        }
        userMessage("Unable to handle a level of type "
                    + level.getClass().getName() + " level=" + level);
        return null;
    }


}

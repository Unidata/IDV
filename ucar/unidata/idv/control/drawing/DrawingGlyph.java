/**
 * $Id: DrawingGlyph.java,v 1.92 2007/08/16 14:04:12 jeffmc Exp $
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


package ucar.unidata.idv.control.drawing;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;


import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.util.DateUtil;



import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.*;
import ucar.visad.data.MapSet;
import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 * Class Glyph. Base class for all drawing things
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.92 $
 */
public abstract class DrawingGlyph {

    /** For unique ids */
    protected static int uniqueCnt = 0;

    /** Xml tag name */
    public static final String TAG_POLYGON = "polygon";

    /** Xml tag name */
    public static final String TAG_ARROW = "arrow";

    /** Xml tag name */
    public static final String TAG_FRONT = "front";

    /** Xml tag name */
    public static final String TAG_TEXT = "text";

    /** Xml tag name */
    public static final String TAG_HIGH = "high";

    /** Xml tag name */
    public static final String TAG_LOW = "low";

    /** Xml tag name */
    public static final String TAG_SHAPE = "shape";

    /** Xml tag name */
    public static final String TAG_SYMBOL = "symbol";

    /** Xml tag name */
    public static final String TAG_IMAGE = "image";

    /** Xml tag name */
    public static final String TAG_MOVIE = "movie";

    /** xml attribute name */
    public static final String ATTR_COLOR = "color";
    public static final String ATTR_BGCOLOR = "bgcolor";

    /** xml attribute name */
    public static final String ATTR_FILLED = "filled";

    /** xml attribute name */
    public static final String ATTR_PICKABLE = "pickable";

    /** xml attribute name */
    public static final String ATTR_FULLLATLON = "fulllatlon";

    /** xml attribute name */
    public static final String ATTR_TIMES = "times";

    /** xml attribute for time format */
    public static final String ATTR_TIMEFORMAT = "timeformat";

    /** xml attribute name */
    public static final String ATTR_ZPOSITION = "zposition";

    /** xml attribute name */
    public static final String ATTR_COORDTYPE = "coordtype";

    /** xml attribute name */
    public static final String ATTR_POINTS = "points";

    /** xml attribute name */
    public static final String ATTR_LINEWIDTH = "linewidth";

    /** xml attr names */
    public static final String ATTR_TEXT = "text";



    /** xml attribute name */
    public static final String ATTR_NAME = "name";

    /** Represents no vertical coordinate */
    public static final int COORD_NONE = -1;

    /** Represents XYZ vertical coordinate */
    public static final int COORD_XYZ = 0;

    /** Represents XY vertical coordinate */
    public static final int COORD_XY = 1;

    /** Represents LAT/LON/ALT vertical coordinate */
    public static final int COORD_LATLONALT = 2;

    /** Represents LAT/LON vertical coordinate */
    public static final int COORD_LATLON = 3;

    /** Drawing coordinate types */
    public static final int[] COORD_TYPES = { COORD_XYZ, COORD_XY,
            COORD_LATLONALT, COORD_LATLON };


    /** Drawing coordinate names */
    public static final String[] COORD_TYPENAMES = { "XYZ", "XY", "LATLONALT",
            "LATLON" };


    /** labels for coordinate syste type. */
    public static final String[] COORD_LABELS = { "X/Y/Z", "X/Y",
            "Lat/Lon/Alt", "Lat/Lon" };

    /** The name of this glyph */
    private String name;

    /** Been removed from display */
    private boolean beenRemoved = false;

    /** Did user create us interactively */
    private boolean createdByUser = true;

    /** Is this glyph visible */
    private boolean visibleFlag = true;

    /** flag */
    public boolean oldVisibility;


    /** Is properties dialog shown */
    protected boolean propertiesUp = false;

    /** The properties dialog */
    protected JDialog propDialog;

    /** For props */
    protected JTabbedPane tabbedPane;

    /** properties widget */
    JTextField nameFld;

    /** properties widget */
    JCheckBox visibleCbx;

    /** properties widget */
    JTextField jythonFld;

    /** For props */
    private List tmpPoints;


    /** Currently being moved */
    boolean beingDragged = false;


    /** Is filled */
    private boolean filled = false;

    /** Is pickable */
    private boolean pickable = true;

    /** Is this glyph editable */
    boolean editable = true;

    /** Is full lat/lon */
    private boolean fullLatLon = false;


    /** List of points. May be lat/lon/alt or x/y/z */
    protected List points = new ArrayList();


    /** What point are we stretching */
    protected int stretchIndex = -1;

    /** Where did we start moving */
    protected double[] firstMoveLocation;

    /** actual points being displayed */
    protected List actualPoints;

    /** Where did we start moving */
    protected EarthLocation firstMoveEarthLocation;


    /** Used to create visad types */
    static int typeCnt = 0;

    /** Indices into arrays */
    public static final int PT_X = 0;

    /** Indices into arrays */
    public static final int PT_Y = 1;

    /** Indices into arrays */
    public static final int PT_Z = 2;


    /** Indices into arrays */
    public static final int IDX_X = 0;

    /** Indices into arrays */
    public static final int IDX_Y = 1;

    /** Indices into arrays */
    public static final int IDX_Z = 2;


    /** Indices into arrays */
    public static final int IDX_LAT = 0;

    /** Indices into arrays */
    public static final int IDX_LON = 1;

    /** Indices into arrays */
    public static final int IDX_ALT = 2;

    /** What drawing coord system are we on */
    protected int coordType = COORD_NONE;


    /** Fixed z position */
    protected float zPosition = 0.0f;


    /** Top level displayable */
    protected CompositeDisplayable parentDisplayable;


    /** Shows selected highlight */
    protected CompositeDisplayable selectionDisplayable;



    /** My color */
    private Color color = Color.blue;

    /** My color */
    private Color bgcolor = null;


    /** My control */
    protected DrawingControl control;


    /** Times we draw in */
    private List timeValues = new ArrayList();

    /** This is the cached time set we use to determine time visibility */
    private Set timeSet;

    /** Are we currently being created */
    private boolean beingCreated = false;

    /** Shows the color */
    private GuiUtils.ColorSwatch colorSwatch;

    private GuiUtils.ColorSwatch bgColorSwatch;

    /** _more_          */
    private AbstractTableModel pointTableModel;


    /**
     * Ctor
     */
    public DrawingGlyph() {}



    /**
     * Ctor
     *
     * @param control The control I'm in
     * @param event The display event.
     */
    public DrawingGlyph(DrawingControl control, DisplayEvent event) {
        this(control, event, false);
    }


    /**
     * Ctor
     *
     * @param control The control I'm in
     * @param event The display event.
     * @param filled Is this glyph filled
     */
    public DrawingGlyph(DrawingControl control, DisplayEvent event,
                        boolean filled) {
        this.control = control;
        this.filled  = filled;
    }


    /**
     * Do final initialization
     *
     * @return Success
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public final boolean initFinal() throws VisADException, RemoteException {
        if ( !initFinalInner()) {
            return false;
        }
        updateLocation();
        return true;
    }




    /**
     * This is called to do final initialization
     *
     * @return Successful
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean initFinalInner()
            throws VisADException, RemoteException {
        if (color != null) {
            setColor(getParent(), color);
        }
        return true;
    }


    /**
     * Initialize from a user event.
     *
     * @param control The control I'm in
     * @param event The display event.
     *
     *
     * @return OK
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public boolean initFromUser(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        this.control = control;
        setCoordType(control.getCoordType());
        setZPosition((float) control.getZPosition());
        setColor(control.getColor());
        fullLatLon = control.getFullLatLon();
        Real time = control.getTimeForGlyph();
        if (time != null) {
            addTime(time);
        }
        return true;
    }


    /**
     * Initialize when recreated from a bundle
     *
     * @param control The control I'm in
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void initFromBundle(DrawingControl control)
            throws VisADException, RemoteException {
        this.control = control;
    }

    /**
     * Initialize from xml
     *
     *
     * @param control The control I'm in
     * @param node The xml node
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void initFromXml(DrawingControl control, Element node)
            throws VisADException, RemoteException {


        this.control  = control;
        createdByUser = false;

        name          = XmlUtil.getAttribute(node, ATTR_NAME, (String) "");

        //zPosition = XmlUtil.getAttribute(node, ATTR_ZPOSITION, (float) 0.0);
        zPosition = XmlUtil.getAttribute(node, ATTR_ZPOSITION,
                                         getDefaultZPosition());
        String coordTypeName = XmlUtil.getAttribute(node, ATTR_COORDTYPE,
                                   "LATLONALT");
        coordTypeName = coordTypeName.toUpperCase();
        setCoordType(COORD_LATLONALT);
        for (int i = 0; i < COORD_TYPES.length; i++) {
            if (coordTypeName.equals(COORD_TYPENAMES[i])) {
                setCoordType(COORD_TYPES[i]);
                break;
            }
        }
        String timeAttr = XmlUtil.getAttribute(node, ATTR_TIMES,
                              (String) null);
        String timeFormat = XmlUtil.getAttribute(node, ATTR_TIMEFORMAT,
                                (String) null);
        SimpleDateFormat sdf = null;
        if (timeAttr != null) {
            List timeStrings = StringUtil.split(timeAttr, ",", true, true);
            if (timeStrings.size() > 0) {
                timeValues = new ArrayList();
                timeSet    = null;
                for (int i = 0; i < timeStrings.size(); i++) {
                    String s = timeStrings.get(i).toString();
                    if (s.equals("NEGATIVE_INFINITY")) {
                        timeValues.add(
                            new DateTime(Double.NEGATIVE_INFINITY));
                    } else if (s.equals("POSITIVE_INFINITY")) {
                        timeValues.add(
                            new DateTime(Double.POSITIVE_INFINITY));
                    } else {
                        if (timeFormat != null) {
                            if (sdf == null) {
                                sdf = new SimpleDateFormat();
                                sdf.applyPattern(timeFormat);
                                sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
                            }
                            try {
                                timeValues.add(new DateTime(sdf.parse(s)));
                            } catch (Exception exc) {
                                throw new RuntimeException(exc);

                            }
                        } else {
                            timeValues.add(DateTime.createDateTime(s));
                        }
                    }
                }
            }
            //      System.err.println ("DrawingGlyph.timeValues:" + timeValues);
        }


        List pointStrings = StringUtil.split(XmlUtil.getAttribute(node,
                                ATTR_POINTS), ",", true, true);

        int stride = 3;
        if ((coordType == COORD_XY) || (coordType == COORD_LATLON)) {
            stride = 2;
        }
        if (isInXYSpace()) {
            for (int i = 0; i < pointStrings.size(); i += stride) {
                double[] da = new double[3];
                da[IDX_X] =
                    Double.parseDouble(pointStrings.get(i).toString());
                da[IDX_Y] = Double.parseDouble(pointStrings.get(i
                        + 1).toString());
                da[IDX_Z] = (coordType == COORD_XY)
                            ? (double) zPosition
                            : Double.parseDouble(pointStrings.get(i
                            + 2).toString());
                points.add(da);
            }
        } else {
            processPointStrings(pointStrings);
        }

        bgcolor = XmlUtil.getAttribute(node, ATTR_BGCOLOR, (Color)null);
        


        Color c = XmlUtil.getAttribute(node, ATTR_COLOR, getColor());
        if (c != null) {
            setColor(c);
        } else if (colorSwatch != null) {
            setColor(colorSwatch.getSwatchColor());
        }
        setFilled(XmlUtil.getAttribute(node, ATTR_FILLED, filled));
        setPickable(XmlUtil.getAttribute(node, ATTR_PICKABLE, pickable));
        setFullLatLon(XmlUtil.getAttribute(node, ATTR_FULLLATLON,
                                           fullLatLon));
    }

    /**
     * Is this glyph selectable
     *
     * @return By default return true
     */
    public boolean isSelectable() {
        return true;
    }

    /**
     * Is the control a front display
     *
     * @return is a front display
     */
    protected boolean isFrontDisplay() {
        return control.getFrontDisplay();
    }


    /**
     * Get the default Z position for  the glyph.
     *
     * @return default Z position for  the glyph.
     */
    protected float getDefaultZPosition() {
        return (control == null)
               ? 0.f
               : (float) control.getZPosition();
    }

    /**
     * Is this glyph valid. Some glyph classes get created but are not fully
     * valid.
     *
     * @return By default return true
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Parse the List of point strings. The format depends on the coordinate
     * system type (e.g., XY, XYZ, etc.)
     *
     * @param pointStrings  List of Strings that represent double location values
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void processPointStrings(List pointStrings)
            throws VisADException, RemoteException {
        int stride = 3;
        if ((coordType == COORD_XY) || (coordType == COORD_LATLON)) {
            stride = 2;
        }
        double fixedAlt = getFixedAltitude();
        points = new ArrayList();
        for (int i = 0; i < pointStrings.size(); i += stride) {
            double lat = Misc.decodeLatLon(pointStrings.get(i).toString());
            double lon = Misc.decodeLatLon(pointStrings.get(i
                             + 1).toString());
            double alt;
            if (coordType == COORD_LATLONALT) {
                alt = Double.parseDouble((String) pointStrings.get(i + 2));
            } else {
                alt = fixedAlt;
            }
            points.add(new EarthLocationTuple(new Real(RealType.Latitude,
                    lat), new Real(RealType.Longitude, lon),
                          new Real(RealType.Altitude, alt)));

        }
    }




    /**
     * handle event
     *
     * @param event event
     *
     * @return continue processing
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public boolean mousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        return false;
    }


    /**
     * Set this glyph selected
     *
     * @param selected Is selected
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setSelected(boolean selected)
            throws VisADException, RemoteException {

        if ( !selected) {
            if (selectionDisplayable == null) {
                return;
            }
            removeDisplayable(selectionDisplayable);
            selectionDisplayable = null;
        } else {
            if (selectionDisplayable != null) {
                return;
            }
            selectionDisplayable = new CompositeDisplayable();
            //            Misc.printStack("setSelected", 10,null);
            VisADGeometryArray marker =
                ShapeUtility.setSize(
                    ShapeUtility.createShape(ShapeUtility.FILLED_SQUARE)[0],
                    .02f);

            List thePoints = getTrimmedSelectionPoints();
            for (int i = 0; (i < thePoints.size()); i++) {
                SelectorPoint point =
                    new SelectorPoint("Probe point " + (uniqueCnt++),
                                      RealTupleType.SpatialCartesian3DTuple);
                point.setAutoSize(true);
                point.setMarker(marker);
                point.setManipulable(false);
                point.setPointSize(control.getDisplayScale());
                selectionDisplayable.addDisplayable(point);
            }
            setSelectionPosition();

            //            System.err.println ("setSelected-addDisplayable-start");
            addDisplayable(selectionDisplayable);
            //            System.err.println ("setSelected-addDisplayable-end");

            //            System.err.println ("setSelected-setColor-start");
            selectionDisplayable.setColor(Color.magenta);
            //            System.err.println ("setSelected-setColor-end");

            //            System.err.println ("setSelected-setActive-start");

        }
    }

    /**
     * Get points used to select this glyph.
     *
     * @return Selection points
     */
    protected List getSelectionPoints() {
        if ((actualPoints != null) && (actualPoints.size() > 0)) {
            return actualPoints;
        }
        return points;
    }

    /**
     * This gives us a sampling  of the points that we use to draw
     * the selection points at.
     *
     * @return Selection points
     */
    private List getTrimmedSelectionPoints() {
        List pts = getSelectionPoints();
        if (pts.size() < 10) {
            return pts;
        }
        List newPts = new ArrayList();
        int  i      = 0;
        int  step   = pts.size() / 10;
        for (i = 0; i < pts.size(); i += step) {
            newPts.add(pts.get(i));
        }
        //Add in the last one
        if (i > pts.size()) {
            newPts.add(pts.get(pts.size() - 1));
        }
        return newPts;
    }

    /**
     * Set the position of the displayable  that shows we are selected.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setSelectionPosition()
            throws VisADException, RemoteException {
        if (selectionDisplayable == null) {
            return;
        }
        List thePoints = getTrimmedSelectionPoints();

        getParent().setDisplayInactive();

        //        System.err.println ("setSelectionPosition-start");
        for (int i = 0;
                (i < thePoints.size())
                && (i < selectionDisplayable.displayableCount());
                i++) {
            double[] loc = getBoxPoint(i, thePoints);
            if (isInFlatSpace()) {
                loc[IDX_Z] = getZPosition();
            }
            RealTuple rt =
                new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                              new double[] { loc[IDX_X],
                                             loc[IDX_Y], loc[IDX_Z] });
            ((SelectorPoint) selectionDisplayable.getDisplayable(i)).setPoint(
                rt);
        }
        getParent().setDisplayActive();
    }


    /**
     * Create an xml element that represents this glyph
     *
     * @param doc The doc to create with
     *
     * @return The element
     */
    public final Element getElement(Document doc) {
        Element e = doc.createElement(getTagName());
        addAttributes(e);
        return e;
    }


    /**
     * Make the time field if we have time values
     * If not, just return the data
     *
     * @param data the data to make the range of the  time field with
     * @return The time field or the data argument if there are no times
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Data getTimeField(Data data)
            throws VisADException, RemoteException {
        return Util.makeTimeField(data, timeValues);
    }


    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(Element e) {
        if ((timeValues != null) && (timeValues.size() > 0)) {
            StringBuffer timesBuff = null;
            for (int i = 0; i < timeValues.size(); i++) {
                if (timesBuff == null) {
                    timesBuff = new StringBuffer();
                } else {
                    timesBuff.append(",");
                }
                timesBuff.append(timeValues.get(i).toString());
            }
            if (timesBuff != null) {
                e.setAttribute(ATTR_TIMES, timesBuff.toString());
            }
        }

        if ((name != null) && (name.length() > 0)) {
            e.setAttribute(ATTR_NAME, name);
        }
        e.setAttribute(ATTR_FILLED, "" + filled);
        e.setAttribute(ATTR_PICKABLE, "" + pickable);
        e.setAttribute(ATTR_FULLLATLON, "" + fullLatLon);
        XmlUtil.setAttribute(e, ATTR_COLOR, color);
        if(bgcolor!=null) {
            XmlUtil.setAttribute(e, ATTR_BGCOLOR, bgcolor);
        }
        e.setAttribute(ATTR_ZPOSITION, "" + zPosition);
        for (int i = 0; i < COORD_TYPES.length; i++) {
            if (coordType == COORD_TYPES[i]) {
                e.setAttribute(ATTR_COORDTYPE, COORD_TYPENAMES[i]);
                break;
            }
        }

        try {
            StringBuffer ptBuff = new StringBuffer();
            float[][]    pts    = getPointValues();
            for (int i = 0; i < pts[0].length; i++) {
                if (i > 0) {
                    ptBuff.append(",");
                }
                if (isInXYSpace()) {
                    ptBuff.append(pts[IDX_X][i]);
                    ptBuff.append(",");
                    ptBuff.append(pts[IDX_Y][i]);
                    if ((coordType == COORD_XYZ)
                            || (coordType == COORD_LATLONALT)) {
                        ptBuff.append(",");
                        ptBuff.append(pts[IDX_Z][i]);
                    }
                } else {
                    ptBuff.append(pts[IDX_LAT][i]);
                    ptBuff.append(",");
                    ptBuff.append(pts[IDX_LON][i]);
                    if (coordType == COORD_LATLONALT) {
                        ptBuff.append(",");
                        ptBuff.append(pts[IDX_ALT][i]);
                    }
                }

            }
            e.setAttribute(ATTR_POINTS, ptBuff.toString());
        } catch (Exception exc) {
            LogUtil.logException("Setting attributes", exc);
        }
    }

    /**
     * Get xml tag name to use
     *
     * @return Xml tag name
     */
    public abstract String getTagName();

    /**
     * Get the name of this glyph type
     *
     * @return  The name
     */
    public abstract String getTypeName();

    /**
     * Get the description
     *
     * @return the description
     */
    public String getDescription() {
        return getTypeName();
    }


    /**
     * get string representation of the area for showing the user
     *
     * @return area label
     *
     * @throws Exception on badness
     */
    public String getAreaString() throws Exception {
        double       squareFeet  = getArea();
        double       acres       = squareFeet / 43560.0;
        double       hectares    = acres * 0.404685642;
        double       squareKM    = acres * 0.00404685642;
        double       squareMiles = squareFeet / 27878400.0;

        StringBuffer desc        = new StringBuffer();
        desc.append("  ");
        if (squareKM > 1.0) {
            desc.append(
                control.getDisplayConventions().formatDistance(squareKM));
            desc.append("[km^2] ");
        } else {
            desc.append(
                control.getDisplayConventions().formatDistance(hectares));
            desc.append("[hectares] ");
        }
        desc.append("  ");
        if (squareMiles > 1.0) {
            desc.append(
                control.getDisplayConventions().formatDistance(squareMiles));
            desc.append("[miles^2] ");
        } else {
            desc.append(
                control.getDisplayConventions().formatDistance(acres));
            desc.append("[acres] ");
        }
        return desc.toString();
    }


    /**
     * Get extra description to show in the JTable
     *
     * @return extra description
     */
    public String getExtraDescription() {
        if (canShowArea()) {
            try {
                Real         distance = getDistance();
                StringBuffer desc     = new StringBuffer();
                if (distance != null) {
                    desc.append(control.formatDistance(distance));
                }
                desc.append(getAreaString());
                return desc.toString();
            } catch (Exception exc) {}
        }
        return "";
    }

    /**
     * Get the main displayable.
     *
     * @return The displayable
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Displayable getDisplayable()
            throws VisADException, RemoteException {
        return getParent();
    }




    /**
     * Get the main displayable.
     *
     * @return The displayable
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected CompositeDisplayable getParent()
            throws VisADException, RemoteException {
        if (parentDisplayable == null) {
            parentDisplayable = new CompositeDisplayable();
            parentDisplayable.setVisible(visibleFlag);
        }
        return parentDisplayable;
    }



    /**
     * Add the displayable to the main parent displayable
     *
     * @param displayable The displayable to add
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void addDisplayable(Displayable displayable)
            throws VisADException, RemoteException {
        getParent().addDisplayable(displayable);
        displayable.setUseTimesInAnimation(control.getUseTimesInAnimation());
        if (getColor() != null) {
            setColor(displayable, getColor());
        }
    }


    /**
     * Remove the displayable from  the main displayable
     *
     * @param displayable The displayable to remove
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void removeDisplayable(Displayable displayable)
            throws VisADException, RemoteException {
        getParent().removeDisplayable(displayable);
        control.getNavigatedDisplay().removeDisplayable(displayable);
    }


    /**
     * Add the time to the list of times
     *
     * @param time The time
     */
    public void addTime(Real time) {
        if (timeValues == null) {
            timeValues = new ArrayList();
        }
        timeValues.add(time);
        timeSet = null;
    }




    /**
     *  Set the TimeValue property.
     *
     *  @param value The new value for TimeValue
     */
    public void setTimeValues(List value) {
        timeValues = value;
        timeSet    = null;
    }



    /**
     *  Get the TimeValue property.
     *
     *  @return The TimeValue
     */
    public List getTimeValues() {
        return timeValues;
    }


    /**
     * Is this glyph visible in the current time step
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void checkTimeVisibility() throws VisADException, RemoteException {

        if ((timeValues == null) || (timeValues.size() == 0)) {
            setVisible(true);
            return;
        }

        Animation animation = control.getSomeAnimation();
        if (animation == null) {
            setVisible(true);
            return;
        }
        Real currentAnimationTime = animation.getAniValue();
        if ((currentAnimationTime == null)
                || currentAnimationTime.isMissing()) {
            setVisible(true);
            return;
        }

        if (timeSet == null) {
            timeSet = Util.makeTimeSet(timeValues);
        }


        //If we only have one time then it has to match the animation  time
        if (timeSet.getLength() == 1) {
            Real myTime = (Real) timeValues.get(0);
            //            System.err.println("ctv one time:" + myTime + " " + currentAnimationTime);
            setVisible(myTime.equals(currentAnimationTime));
        } else {
            //Else work the visad magic
            float timeValueFloat = (float) currentAnimationTime.getValue(
                                       timeSet.getSetUnits()[0]);
            //            System.err.println("multiple times:" + timeValueFloat);
            float[][] value = {
                { timeValueFloat }
            };
            int[]     index = timeSet.valueToIndex(value);
            //            System.err.println("index:" + index[0]);
            setVisible(index[0] >= 0);
        }

    }


    /**
     * Toggle visiblity of the displayable
     *
     * @param visible Is visible
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void setVisible(boolean visible)
            throws VisADException, RemoteException {
        if (parentDisplayable == null) {
            return;
        }
        parentDisplayable.setVisible(visible && visibleFlag);
    }


    /**
     * Is this glyph visible
     *
     * @return is visible
     */
    public boolean isVisible() {
        if (parentDisplayable == null) {
            return false;
        }
        return parentDisplayable.getVisible();
    }

    /**
     * Make sure the displayable is not visible if this glyph
     * is not visible
     */
    public void checkVisibility() {
        if (parentDisplayable != null) {
            if ( !visibleFlag) {
                try {
                    parentDisplayable.setVisible(false);
                } catch (Exception exc) {
                    LogUtil.logException("Setting visibility", exc);
                }
            }
        }
    }


    /**
     * Is glyph constrained to 2d
     *
     * @return Constrained to 2d
     */
    protected boolean constrainedTo2D() {
        return filled;
    }


    /**
     *  Set the CoordType property.
     *
     *  @param value The new value for CoordType
     */
    public void setCoordType(int value) {
        coordType = value;
        if (constrainedTo2D()) {
            if (coordType == COORD_XYZ) {
                coordType = COORD_XY;
            } else if (coordType == COORD_LATLONALT) {
                coordType = COORD_LATLON;
            }
        }
    }

    /**
     *  Get the CoordType property.
     *
     *  @return The CoordType
     */
    public int getCoordType() {
        return coordType;
    }


    /**
     * Are we drawing in xy or xyz space
     *
     * @return Is in xy space
     */
    public boolean isInXYSpace() {
        return (coordType == COORD_XYZ) || (coordType == COORD_XY);
    }

    /**
     * Are we drawing in xy or lat/lon space
     *
     * @return Is in 2d space
     */
    public boolean isInFlatSpace() {
        return (coordType == COORD_XY) || (coordType == COORD_LATLON);
    }

    /**
     * In lat/lon or lat/lon/alt space
     *
     * @return Is in latlon space
     */
    public boolean isInLatLonSpace() {
        return (coordType == COORD_LATLONALT) || (coordType == COORD_LATLON);
    }


    /**
     * Return this
     *
     * @return this
     */
    private DrawingGlyph getMe() {
        return this;
    }



    /**
     * Get point to use from the event. This may be an earthlocation
     * if in latlon space or a double array if in x space.
     *
     * @param event The display event.
     *
     * @return The point
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Object getPoint(DisplayEvent event)
            throws VisADException, RemoteException {
        if (isInXYSpace()) {
            double[] point = control.toBox(event);
            if (isInFlatSpace()) {
                point[IDX_Z] = control.getVerticalValue(zPosition);
            }
            return point;
        } else if (isInLatLonSpace()) {
            EarthLocation el  = control.toEarth(event);
            LatLonPoint   llp = el.getLatLonPoint();
            if (isInFlatSpace()) {
                Real alt = el.getAltitude();
                el = new EarthLocationTuple(
                    el.getLatLonPoint(),
                    new Real((RealType) alt.getType(), getFixedAltitude()));
            }
            return el;
        } else {
            System.err.println("Unknown coordinate:" + coordType);
            return null;
        }
    }



    /**
     * Set the properties from the dialog
     */
    public synchronized final void setProperties() {

        if (propertiesUp) {
            return;
        }

        propertiesUp = true;
        List            comps   = new ArrayList();
        final Hashtable compMap = new Hashtable();
        getPropertiesComponents(comps, compMap);
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel propsPanel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                GuiUtils.WT_N);
        propsPanel = GuiUtils.inset(propsPanel, 5);
        propDialog = new JDialog((Frame) null, getName() + " Properties",
                                 true);

        tabbedPane = new JTabbedPane();
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_APPLY)
                        || cmd.equals(GuiUtils.CMD_OK)) {
                    try {
                        if ( !applyProperties(compMap)) {
                            return;
                        }
                        checkTimeVisibility();
                        updateLocation();
                        control.glyphChanged(DrawingGlyph.this);
                    } catch (Exception exc) {
                        LogUtil.logException("Applying properties", exc);
                    }
                }
                if (cmd.equals(GuiUtils.CMD_CANCEL)
                        || cmd.equals(GuiUtils.CMD_OK)) {
                    propDialog.dispose();
                }

            }
        };
        JPanel buttons  = GuiUtils.makeApplyOkCancelButtons(listener);
        JPanel contents = GuiUtils.top(propsPanel);

        if (comps.size() > 0) {
            tabbedPane.add("Properties", contents);
        }

        tmpPoints       = new ArrayList(points);

        pointTableModel = new AbstractTableModel() {

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return true;
            }

            public int getRowCount() {
                return tmpPoints.size();
            }

            public int getColumnCount() {
                if (isInFlatSpace()) {
                    return 2;
                }
                return 3;
            }

            public void setValueAt(Object aValue, int row, int col) {
                double value = Misc.parseNumber(aValue.toString().trim());
                if (isInXYSpace()) {
                    double[] d = (double[]) tmpPoints.get(row);
                    d[col] = value;
                } else {
                    try {
                        EarthLocation el  =
                            (EarthLocation) tmpPoints.get(row);
                        Real          lat = el.getLatLonPoint().getLatitude();
                        Real          lon =
                            el.getLatLonPoint().getLongitude();
                        Real          alt = el.getAltitude();


                        Real newLat = new Real((RealType) lat.getType(),
                                          ((col == 0)
                                           ? value
                                           : lat.getValue()), lat.getUnit());
                        Real newLon = new Real((RealType) lon.getType(),
                                          ((col == 1)
                                           ? value
                                           : lon.getValue()), lon.getUnit());
                        Real newAlt = ((coordType == COORD_LATLONALT)
                                       ? new Real((RealType) alt.getType(),
                                           ((col == 2)
                                            ? value
                                            : alt.getValue()), alt.getUnit())
                                       : alt);
                        EarthLocation newEl = new EarthLocationTuple(newLat,
                                                  newLon, newAlt);
                        int idx = tmpPoints.indexOf(el);
                        tmpPoints.remove(idx);
                        tmpPoints.add(idx, newEl);
                    } catch (Exception exc) {
                        LogUtil.logException("Setting points", exc);
                    }
                }

            }

            public Object getValueAt(int row, int column) {
                if (row < tmpPoints.size()) {
                    if (isInXYSpace()) {
                        double[] d = (double[]) tmpPoints.get(row);
                        return control.getDisplayConventions().format(
                            d[column]);
                    }
                    EarthLocation el    = (EarthLocation) tmpPoints.get(row);
                    String        value = null;
                    if (column == 0) {
                        value = control.getDisplayConventions().formatLatLon(
                            el.getLatitude());
                    } else if (column == 1) {
                        value = control.getDisplayConventions().formatLatLon(
                            el.getLongitude());
                    } else {
                        value =
                            control.getDisplayConventions().formatAltitude(
                                el.getAltitude());
                    }
                    return value;

                }
                return "";
            }

            public String getColumnName(int column) {
                if (isInXYSpace()) {
                    if (column == 0) {
                        return "X";
                    } else if (column == 1) {
                        return "Y";
                    } else {
                        return "Z";
                    }
                }
                if (column == 0) {
                    return "Latitude";
                } else if (column == 1) {
                    return "Longitude";
                } else {
                    return "Altitude";
                }
            }

        };


        JTable pointTable = new JTable(pointTableModel);

        JButton writeBtn = GuiUtils.makeButton("Write Points", this,
                               "writePoints");


        int width  = 300;
        int height = 200;
        JScrollPane scroller = GuiUtils.makeScrollPane(pointTable, width,
                                   height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));


        tabbedPane.add("Points",
                       GuiUtils.centerBottom(scroller,
                                             GuiUtils.left(writeBtn)));

        jythonFld = new JTextField(control.getGlyphJython());
        JButton jythonBtn = GuiUtils.makeButton("Evaluate:", this,
                                "evaluateJython");
        JPanel jythonPanel =
            GuiUtils.inset(GuiUtils.top(GuiUtils.hbox(jythonBtn, jythonFld)),
                           5);
        // tabbedPane.add("Evaluate", jythonPanel);

        propDialog.getContentPane().add(GuiUtils.centerBottom(tabbedPane,
                buttons));
        GuiUtils.showInCenter(propDialog);
        propertiesUp = false;

    }


    /**
     * write out the points as a csv file
     */
    public void writePoints() {
        String filename =
            FileManager.getWriteFile(Misc.newList(FileManager.FILTER_CSV,
                FileManager.FILTER_XLS), FileManager.SUFFIX_CSV, null);

        if (filename == null) {
            return;
        }
        GuiUtils.exportAsCsv("", pointTableModel, filename);
    }


    /**
     * Evaluate the jython from the properties dialog
     */
    public void evaluateJython() {
        control.evaluateGlyphJython(this, jythonFld.getText());

    }

    /**
     * Handle the property apply.
     *
     * @param compMap Holds property widgets
     *
     *
     * @return Success
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean applyProperties(Hashtable compMap)
            throws VisADException, RemoteException {

        setName(nameFld.getText());
        points = new ArrayList(tmpPoints);
        //        String colorName =
        //            (String) ((JComboBox) compMap.get(ATTR_COLOR)).getSelectedItem();

        if (colorSwatch != null) {
            setColor(colorSwatch.getSwatchColor());
        }

        if (bgColorSwatch != null) {
            setBgcolor(bgColorSwatch.getSwatchColor());
        }




        JList timeList = (JList) compMap.get(ATTR_TIMES);
        if (createdByUser && (timeList != null)) {
            timeValues = Misc.toList(timeList.getSelectedValues());
            timeSet    = null;
            checkVisibility();
        }
        setVisibleFlag(visibleCbx.isSelected());
        return true;

    }


    /**
     * Hide glyph
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void hide() throws VisADException, RemoteException {
        setVisibleFlag(false);
    }


    /**
     * Show glyph
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void show() throws VisADException, RemoteException {
        setVisibleFlag(true);
    }





    /**
     * Should show color selector in properties
     *
     * @return show color selector
     */
    protected boolean shouldShowColorSelector() {
        return true;
    }


    protected boolean shouldShowBgColorSelector() {
        return false;
    }

    /**
     * Make the properties widgets
     *
     * @param comps List of components
     * @param compMap Map to hold name to widget
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        comps.add(GuiUtils.rLabel("Name:"));
        comps.add(nameFld = new JTextField(getName()));

        comps.add(GuiUtils.filler());
        comps.add(visibleCbx = new JCheckBox("Visible", visibleFlag));
        if (shouldShowColorSelector()) {
            comps.add(GuiUtils.rLabel("Color:"));
            colorSwatch = new GuiUtils.ColorSwatch(color, "", false);
            colorSwatch.setMinimumSize(new Dimension(20, 20));
            colorSwatch.setPreferredSize(new Dimension(20, 20));
            comps.add(GuiUtils.left(colorSwatch));
        }


        if (shouldShowBgColorSelector()) {
            comps.add(GuiUtils.rLabel("BG Color:"));
            bgColorSwatch = new GuiUtils.ColorSwatch(bgcolor, "", false);
            bgColorSwatch.setMinimumSize(new Dimension(20, 20));
            bgColorSwatch.setPreferredSize(new Dimension(20, 20));
            comps.add(GuiUtils.left(bgColorSwatch.getPanel()));
        }

        getTimePropertiesComponents(comps, compMap);

        try {
            Real distance = getDistance();
            if (distance != null) {
                comps.add(GuiUtils.rLabel("Distance:"));
                comps.add(new JLabel(control.formatDistance(distance)));
            }
            if (canShowArea()) {
                comps.add(GuiUtils.rLabel("Area:"));
                comps.add(new JLabel(getAreaString()));
            }
        } catch (Exception exc) {
            LogUtil.logException("Setting distance", exc);
        }
    }

    /**
     * Add to the comps array the time list for selecting times
     *
     * @param comps comps
     * @param compMap map
     */
    protected void getTimePropertiesComponents(List comps,
            Hashtable compMap) {
        try {
            List       allTimes  = new ArrayList();
            DateTime[] animTimes = control.getAllTimes();
            JList      timeList  = null;
            if (createdByUser && (animTimes != null)) {
                allTimes.addAll(Misc.toList(animTimes));
            }
            if (timeValues != null) {
                for (int i = 0; i < timeValues.size(); i++) {
                    Object time = timeValues.get(i);
                    if ( !allTimes.contains(time)) {
                        allTimes.add(time);
                    }
                }
            }

            if (allTimes.size() > 0) {
                timeList = new JList();
                timeList.setListData(new Vector(allTimes));
                timeList.setSelectionMode(
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                timeList.setVisibleRowCount(5);
                if ((timeValues != null) && (timeValues.size() > 0)) {
                    int[] selected = new int[timeValues.size()];
                    for (int i = 0; i < timeValues.size(); i++) {
                        Object time = timeValues.get(i);
                        selected[i] = allTimes.indexOf(time);
                    }
                    timeList.setSelectedIndices(selected);
                }
                if ( !createdByUser) {
                    timeList.setEnabled(false);
                }
                compMap.put(ATTR_TIMES, timeList);
                comps.add(GuiUtils.top(GuiUtils.rLabel("Times:")));
                comps.add(GuiUtils.makeScrollPane(timeList, 200, 300));
            }

        } catch (Exception exc) {
            LogUtil.logException("Setting times list", exc);
        }
    }




    /**
     * Calculate the distance along the line
     *
     * @return Distance or null if this glyph type cannot calculate it
     *
     * @throws Exception On badness
     */
    public Real getDistance() throws Exception {
        if ( !isInLatLonSpace() || !canShowDistance()) {
            return null;
        }
        double    distance = 0.0;
        float[][] pts      = getPointValues();
        for (int i = 1; i < pts[0].length; i++) {
            float lat1 = pts[IDX_LAT][i - 1];
            float lon1 = pts[IDX_LON][i - 1];
            float lat2 = pts[IDX_LAT][i];
            float lon2 = pts[IDX_LON][i];
            Bearing result =
                Bearing.calculateBearing(new LatLonPointImpl(lat1, lon1),
                                         new LatLonPointImpl(lat2, lon2),
                                         null);
            if ( !Double.isNaN(result.getDistance())) {
                distance += result.getDistance();
            }
        }

        Unit kmUnit = ucar.visad.Util.parseUnit("km");
        return new Real(RealType.getRealType("distance", kmUnit), distance,
                        kmUnit);
    }




    /**
     * Get the area in square feet.
     *
     * @return area of this shape in square feet
     *
     * @throws Exception On badness
     */
    public double getArea() throws Exception {
        double    area   = 0.0;
        float[][] pts    = getLatLons(((actualPoints != null)
                                       ? actualPoints
                                       : points));
        double    minLat = Double.POSITIVE_INFINITY;
        double    minLon = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < pts[0].length; i++) {
            if (i == 0) {
                minLat = pts[IDX_LAT][i];
                minLon = pts[IDX_LON][i];
            } else {
                minLat = Math.min(minLat, pts[IDX_LAT][i]);
                minLon = Math.min(minLon, pts[IDX_LON][i]);
            }
        }
        int len = pts[0].length;
        for (int i = 0; i < len; i++) {
            double x1 = distance(minLat, minLon, minLat, pts[IDX_LON][i]);
            double y1 = distance(minLat, minLon, pts[IDX_LAT][i], minLon);
            double x2 = distance(minLat, minLon, minLat, ((i < len - 1)
                    ? pts[IDX_LON][i + 1]
                    : pts[IDX_LON][0]));
            double y2 = distance(minLat, minLon, ((i < len - 1)
                    ? pts[IDX_LAT][i + 1]
                    : pts[IDX_LAT][0]), minLon);
            area += x1 * y2 - x2 * y1;
        }
        area = 0.5 * area;
        return Math.abs(area);
    }

    /**
     * Utility to calculate distance between the points
     *
     * @param lat1 lat1
     * @param lon1 lon1
     * @param lat2 lat2
     * @param lon2 lon2
     *
     * @return distance in feet
     *
     * @throws Exception On badness
     */
    private double distance(double lat1, double lon1, double lat2,
                            double lon2)
            throws Exception {
        Bearing result = Bearing.calculateBearing(new LatLonPointImpl(lat1,
                             lon1), new LatLonPointImpl(lat2, lon2), null);
        double distance = result.getDistance();
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        Unit   feetUnit = ucar.visad.Util.parseUnit("feet");
        Real kmDistance = new Real(RealType.getRealType("distance", kmUnit),
                                   distance, kmUnit);

        return kmDistance.getValue(feetUnit);
    }


    /**
     * Can this glyph type calculate distance
     *
     * @return can do distance
     */
    public boolean canShowDistance() {
        return false;
    }

    /**
     * Can this glyph type calculate area
     *
     * @return can do area
     */
    public boolean canShowArea() {
        return false;
    }



    /**
     * currently being created
     *
     * @return being created
     */
    public boolean getBeingCreated() {
        return beingCreated;
    }


    /**
     * all done
     */
    public void doneBeingCreated() {
        beingCreated = false;
    }


    /**
     * Handle the creation event. If it returns this then
     * the DrawingControl keeps routing events  to me.
     * If returns null then no more events get routed to me.
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public DrawingGlyph handleCreation(DisplayEvent event)
            throws VisADException, RemoteException {
        beingCreated = true;
        return this;
    }



    /**
     * Handle the event. If it returns this then
     * the DrawingControl keeps routing events  to me.
     * If returns null then no more events get routed to me.
     *
     * @param event The display event.
     *
     * @return This or null.
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        return this;
    }


    /**
     * Handle the event. If it returns this then
     * the DrawingControl keeps routing events  to me.
     * If returns null then no more events get routed to me.
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMouseMoved(DisplayEvent event)
            throws VisADException, RemoteException {
        return this;
    }



    /**
     * Start streth
     *
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initStretch(DisplayEvent event)
            throws VisADException, RemoteException {
        stretchIndex = closestPoint(control.toBox(event), points);
    }

    /**
     * Stretch this glyph
     *
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doStretch(DisplayEvent event)
            throws VisADException, RemoteException {
        beingDragged = true;
        if ((stretchIndex < 0) || (stretchIndex >= points.size())) {
            return;
        }
        points.set(stretchIndex, getPoint(event));
        updateLocation();
    }

    /**
     * delete the point nearest the event
     *
     * @param event the event
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public void doDeletePoint(DisplayEvent event)
            throws VisADException, RemoteException {}



    /**
     * Move this glyph
     *
     * @param event The display event.
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void doMove(DisplayEvent event)
            throws VisADException, RemoteException {
        beingDragged = true;
        moveTo(event);
        updateLocation();
    }


    /**
     * Move this glyph to the location of the event
     *
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void moveTo(DisplayEvent event)
            throws VisADException, RemoteException {
        if (isInXYSpace()) {
            double[] loc = control.toBox(event);
            for (int i = 0; i < points.size(); i++) {
                double[] point = (double[]) points.get(i);
                point[IDX_X] += (loc[IDX_X] - firstMoveLocation[IDX_X]);
                point[IDX_Y] += (loc[IDX_Y] - firstMoveLocation[IDX_Y]);
                if (coordType == COORD_XYZ) {
                    point[IDX_Z] += (loc[IDX_Z] - firstMoveLocation[IDX_Z]);
                }
            }
            firstMoveLocation = loc;
        } else if (isInLatLonSpace()) {
            List oldPoints = points;
            points = new ArrayList();
            EarthLocation currentLoc = control.toEarth(event);
            double deltaLon =
                currentLoc.getLatLonPoint().getLongitude().getValue()
                - firstMoveEarthLocation.getLatLonPoint().getLongitude()
                    .getValue();
            double deltaLat =
                currentLoc.getLatLonPoint().getLatitude().getValue()
                - firstMoveEarthLocation.getLatLonPoint().getLatitude()
                    .getValue();
            double deltaAlt =
                currentLoc.getAltitude().getValue()
                - firstMoveEarthLocation.getAltitude().getValue();
            for (int i = 0; i < oldPoints.size(); i++) {
                EarthLocation el  = (EarthLocation) oldPoints.get(i);
                Real          lat = el.getLatLonPoint().getLatitude();
                Real          lon = el.getLatLonPoint().getLongitude();
                Real          alt = el.getAltitude();

                Real newLon = new Real((RealType) lon.getType(),
                                       lon.getValue() + deltaLon,
                                       lon.getUnit());
                Real newLat = new Real((RealType) lat.getType(),
                                       lat.getValue() + deltaLat,
                                       lat.getUnit());
                Real newAlt = ((coordType == COORD_LATLONALT)
                               ? new Real((RealType) alt.getType(),
                                          alt.getValue() + deltaAlt,
                                          alt.getUnit())
                               : alt);
                EarthLocation newEl = new EarthLocationTuple(newLat, newLon,
                                          newAlt);
                points.add(newEl);
            }
            firstMoveEarthLocation = currentLoc;
        }
    }




    /**
     * Convert points list of array of xyz or lat/lon/alt
     *
     * @return Location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected float[][] getPointValues()
            throws VisADException, RemoteException {
        return getPointValues(false);
    }


    /**
     * Convert points list of array of xyz or lat/lon/alt
     *
     *
     * @param convertToXY If true, force result to be xyz
     * @return Location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected float[][] getPointValues(boolean convertToXY)
            throws VisADException, RemoteException {
        float[][] pts = new float[3][points.size()];
        if (isInXYSpace()) {
            for (int i = 0; i < points.size(); i++) {
                double[] point = (double[]) points.get(i);
                pts[IDX_X][i] = (float) point[IDX_X];
                pts[IDX_Y][i] = (float) point[IDX_Y];
                pts[IDX_Z][i] = ((coordType == COORD_XYZ)
                                 ? (float) point[IDX_Z]
                                 : zPosition);
            }
        } else if (isInLatLonSpace()) {
            double fixedAlt = getFixedAltitude();
            for (int i = 0; i < points.size(); i++) {
                EarthLocation el = (EarthLocation) points.get(i);
                if (convertToXY) {
                    double[] point = getSpatialCoordinates(el);
                    pts[IDX_X][i] = (float) point[IDX_X];
                    pts[IDX_Y][i] = (float) point[IDX_Y];
                    if (coordType == COORD_LATLON) {
                        pts[IDX_Z][i] =
                            (float) control.getVerticalValue(zPosition);
                    }
                } else {
                    pts[IDX_LAT][i] =
                        (float) el.getLatLonPoint().getLatitude().getValue();
                    pts[IDX_LON][i] =
                        (float) el.getLatLonPoint().getLongitude().getValue();
                    if (coordType == COORD_LATLONALT) {
                        pts[IDX_ALT][i] = (float) el.getAltitude().getValue();
                    } else {
                        pts[IDX_ALT][i] = (float) fixedAlt;
                    }
                }

            }
        }
        return pts;
    }



    /**
     * get the points as an array of lat/lons
     *
     * @return latlons
     *
     * @throws Exception on badness
     */
    public float[][] getLatLons() throws Exception {
        return getLatLons(((actualPoints != null)
                           ? actualPoints
                           : points));
    }

    /**
     * Convert points list of array of xyz or lat/lon/alt
     *
     *
     * @param points List of points. Either double array or EarthLocation
     * @return lat/lons
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected float[][] getLatLons(List points)
            throws VisADException, RemoteException {
        float[][] pts = new float[2][points.size()];
        if (isInXYSpace()) {
            for (int i = 0; i < points.size(); i++) {
                double[] point = (double[]) points.get(i);
                EarthLocation el = control.boxToEarth(new double[] {
                                       point[IDX_X],
                                       point[IDX_Y], 0.0 });
                pts[IDX_LAT][i] =
                    (float) el.getLatLonPoint().getLatitude().getValue();
                pts[IDX_LON][i] =
                    (float) el.getLatLonPoint().getLongitude().getValue();
            }
        } else if (isInLatLonSpace()) {
            for (int i = 0; i < points.size(); i++) {
                EarthLocation el = (EarthLocation) points.get(i);
                pts[IDX_LAT][i] =
                    (float) el.getLatLonPoint().getLatitude().getValue();
                pts[IDX_LON][i] =
                    (float) el.getLatLonPoint().getLongitude().getValue();
            }
        }
        return pts;
    }






    /**
     * Convert points list of array of xyz or lat/lon/alt
     *
     * @return Location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected double[][] getPointValuesDouble()
            throws VisADException, RemoteException {
        return getPointValuesDouble(false);
    }


    /**
     * Convert points list of array of xyz or lat/lon/alt
     *
     *
     * @param convertToXY If true, force result to be xyz
     * @return Location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected double[][] getPointValuesDouble(boolean convertToXY)
            throws VisADException, RemoteException {
        double[][] pts = new double[3][points.size()];
        if (isInXYSpace()) {
            for (int i = 0; i < points.size(); i++) {
                double[] point = (double[]) points.get(i);
                pts[IDX_X][i] = (double) point[IDX_X];
                pts[IDX_Y][i] = (double) point[IDX_Y];
                pts[IDX_Z][i] = ((coordType == COORD_XYZ)
                                 ? (double) point[IDX_Z]
                                 : zPosition);
            }
        } else if (isInLatLonSpace()) {
            double fixedAlt = getFixedAltitude();
            for (int i = 0; i < points.size(); i++) {
                EarthLocation el = (EarthLocation) points.get(i);
                if (convertToXY) {
                    double[] point = getSpatialCoordinates(el);
                    pts[IDX_X][i] = (double) point[IDX_X];
                    pts[IDX_Y][i] = (double) point[IDX_Y];
                    if (coordType == COORD_LATLON) {
                        pts[IDX_Z][i] =
                            (double) control.getVerticalValue(zPosition);
                    }
                } else {
                    pts[IDX_LAT][i] =
                        (double) el.getLatLonPoint().getLatitude().getValue();
                    pts[IDX_LON][i] =
                        (double) el.getLatLonPoint().getLongitude()
                            .getValue();
                    if (coordType == COORD_LATLONALT) {
                        pts[IDX_ALT][i] =
                            (double) el.getAltitude().getValue();
                    } else {
                        pts[IDX_ALT][i] = (double) fixedAlt;
                    }
                }

            }
        }
        return pts;
    }






    /**
     * Try to fill the pts array
     *
     * @param pts The points to fill
     * @param dflt The default to return if cannot fill
     *
     * @return The filled data or the default if fill is not possible
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Data tryToFill(float[][] pts, Data dflt)
            throws VisADException, RemoteException {
        if ( !getFilled()) {
            return dflt;
        }
        MathType mathType2D = null;
        if (isInXYSpace()) {
            mathType2D = RealTupleType.SpatialCartesian2DTuple;
        } else if (isInLatLonSpace()) {
            mathType2D = RealTupleType.LatitudeLongitudeTuple;
        }

        float[][] rect2d = new float[2][];
        rect2d[IDX_X] = pts[IDX_X];
        rect2d[IDX_Y] = pts[IDX_Y];
        Gridded2DSet tmp = new Gridded2DSet(mathType2D, rect2d,
                                            rect2d[0].length);
        try {
            return DelaunayCustom.fill(tmp);
        } catch (VisADException vexc) {
            System.err.println("error:" + vexc);
            return dflt;
        }
    }



    /**
     * Convert to float array
     *
     * @param el Location
     *
     * @return Converted location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected float[] toLatLonAlt(EarthLocation el)
            throws VisADException, RemoteException {
        float[] a = { (float) el.getLatLonPoint().getLatitude().getValue(),
                      (float) el.getLatLonPoint().getLongitude().getValue(),
                      (float) el.getAltitude().getValue() };
        return a;
    }

    /**
     * Get altitude of zposition
     *
     * @return Altitude
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected double getFixedAltitude()
            throws VisADException, RemoteException {
        return control.boxToEarth(new double[] { 0.0, 0.0,
                control.getVerticalValue(
                    zPosition) }, false).getAltitude().getValue();
    }

    /**
     * Handle event
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void projectionChanged() throws VisADException, RemoteException {
        if ( !isInXYSpace() || getFullLatLon()) {
            updateLocation();
        }
    }

    /**
     * viewpoint changed
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void viewpointChanged() throws VisADException, RemoteException {}



    /**
     * Glyph moved. Update the Displayable location.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        setSelectionPosition();
    }



    /**
     * Started moving
     *
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initMove(DisplayEvent event)
            throws VisADException, RemoteException {
        firstMoveLocation      = control.toBox(event);
        firstMoveEarthLocation = control.toEarth(event);
    }


    /**
     * Handle event.
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public DrawingGlyph handleKeyPressed(DisplayEvent event)
            throws VisADException, RemoteException {
        return this;
    }


    /**
     * Handle event.
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMouseDragged(DisplayEvent event)
            throws VisADException, RemoteException {
        return null;
    }

    /**
     * Handle event.
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMouseReleased(DisplayEvent event)
            throws VisADException, RemoteException {
        beingDragged = false;
        return null;
    }


    /**
     * Set the Color property.
     *
     * @param value The new value for Color
     */
    public void setColor(Color value) {
        try {
            color = value;
            if ((color != null) && (parentDisplayable != null)) {
                setColor(parentDisplayable, color);
                if (selectionDisplayable != null) {
                    selectionDisplayable.setColor(Color.magenta);
                }
            }
        } catch (Exception exc) {
            LogUtil.logException("Setting color", exc);
        }
    }

    /**
     * Set color on displayable
     *
     * @param displayable displayable_
     * @param c color
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setColor(Displayable displayable, Color c)
            throws VisADException, RemoteException {
        if ((displayable != null) && (c != null)) {
            displayable.setColor(c);
        }
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
Set the Bgcolor property.

@param value The new value for Bgcolor
**/
public void setBgcolor (Color value) {
	this.bgcolor = value;
}

/**
Get the Bgcolor property.

@return The Bgcolor
**/
public Color getBgcolor () {
	return this.bgcolor;
}






    /**
     * Set the points list
     *
     * @param pts Points
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setActualPoints(float[][] pts)
            throws VisADException, RemoteException {
        actualPoints = new ArrayList();
        if (isInXYSpace()) {
            for (int i = 0; i < pts[0].length; i++) {
                actualPoints.add(new double[] { pts[0][i], pts[1][i],
                        pts[2][i] });
            }
        } else {
            for (int i = 0; i < pts[0].length; i++) {
                double lat = (double) pts[IDX_LAT][i];
                double lon = (double) pts[IDX_LON][i];
                double alt = (double) pts[IDX_ALT][i];
                actualPoints.add(makePoint(lat, lon, alt));
            }
        }
    }


    /**
     * Utility to make a point
     *
     * @param lat lat
     * @param lon lon
     * @param alt alt
     *
     * @return point
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected EarthLocation makePoint(double lat, double lon, double alt)
            throws VisADException, RemoteException {
        return new EarthLocationTuple(new Real(RealType.Latitude, lat),
                                      new Real(RealType.Longitude, lon),
                                      new Real(RealType.Altitude, alt));
    }

    /**
     * Calculate distance
     *
     * @param location from
     * @param direction direction
     *
     * @return distance
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double distance(double[] location, double[] direction)
            throws VisADException, RemoteException {
        if ( !isVisible() || !getPickable()) {
            return Double.MAX_VALUE;
        }
        if ((actualPoints != null) && (actualPoints.size() > 0)) {
            return distance(location, direction, actualPoints);
        }
        return distance(location, direction, points);
    }



    /**
     * Get distance between
     *
     * @param origin From
     * @param loc2 To
     *
     * @return Distance
     */
    public static double distanceBetween(double[] origin, double[] loc2) {
        return Math.sqrt(squared(origin[0] - loc2[0])
                         + squared(origin[1] - loc2[1])
                         + squared(origin[2] - loc2[2]));
    }



    /**
     * Get distance
     *
     * @param loc1 point 1
     * @param el point 2
     *
     * @return distance
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public double distanceBetween(double[] loc1, EarthLocation el)
            throws VisADException, RemoteException {
        tmp = control.getNavigatedDisplay().getSpatialCoordinates(el, tmp);
        return distanceBetween(loc1, tmp);
    }



    /**
     * This stretches the current point and also stretches the
     * rest  of the points by a linear delta from the initial point.
     *
     * @param event The mouse move event
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void doInterpolatedStretch(DisplayEvent event)
            throws VisADException, RemoteException {
        beingDragged = true;
        if ((stretchIndex < 0) || (stretchIndex >= points.size())) {
            return;
        }
        int    startPts = stretchIndex - 1;
        int    endPts   = points.size() - stretchIndex;
        double percent  = 1.0;

        if (isInLatLonSpace()) {
            EarthLocation oldPt = (EarthLocation) points.get(stretchIndex);
            EarthLocation newPt = (EarthLocation) getPoint(event);
            double deltaY = oldPt.getLatitude().getValue()
                            - newPt.getLatitude().getValue();
            double deltaX = oldPt.getLongitude().getValue()
                            - newPt.getLongitude().getValue();
            for (int i = stretchIndex - 1; i >= 0; i--) {
                percent -= 1.0 / (double) startPts;
                if (percent <= 0) {
                    break;
                }
                EarthLocation pt = (EarthLocation) points.get(i);
                EarthLocation newEl =
                    makePoint(
                        pt.getLatitude().getValue() - deltaY * percent,
                        pt.getLongitude().getValue() - deltaX * percent,
                        pt.getAltitude().getValue());
                points.set(i, newEl);
            }
            percent = 1.0;
            for (int i = stretchIndex + 1; i < points.size(); i++) {
                percent -= 1.0 / (double) endPts;
                if (percent <= 0) {
                    break;
                }
                EarthLocation pt = (EarthLocation) points.get(i);
                EarthLocation newEl =
                    makePoint(
                        pt.getLatitude().getValue() - deltaY * percent,
                        pt.getLongitude().getValue() - deltaX * percent,
                        pt.getAltitude().getValue());
                points.set(i, newEl);
            }
        } else {
            double[] oldPt  = (double[]) points.get(stretchIndex);
            double[] newPt  = (double[]) getPoint(event);
            double   deltaY = oldPt[IDX_Y] - newPt[IDX_Y];
            double   deltaX = oldPt[IDX_X] - newPt[IDX_X];
            for (int i = stretchIndex - 1; i >= 0; i--) {
                percent -= 1.0 / (double) startPts;
                if (percent <= 0) {
                    break;
                }
                double[] pt = (double[]) points.get(i);
                pt[IDX_Y] = pt[IDX_Y] - deltaY * percent;
                pt[IDX_X] = pt[IDX_X] - deltaX * percent;
            }
            percent = 1.0;
            for (int i = stretchIndex + 1; i < points.size(); i++) {
                percent -= 1.0 / (double) endPts;
                if (percent <= 0) {
                    break;
                }
                double[] pt = (double[]) points.get(i);
                pt[IDX_Y] = pt[IDX_Y] - deltaY * percent;
                pt[IDX_X] = pt[IDX_X] - deltaX * percent;
            }
        }
        points.set(stretchIndex, getPoint(event));
        updateLocation();
    }


    /**
     * Get distance
     *
     * @param origin Point
     * @param direction Direction
     * @param points Points
     *
     * @return distance
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private double distance(double[] origin, double[] direction, List points)
            throws VisADException, RemoteException {
        double minDistance = Double.MAX_VALUE;
        if (points.size() == 0) {
            return minDistance;
        }
        double o_x = origin[IDX_X];
        double o_y = origin[IDX_Y];
        double o_z = origin[IDX_Z];

        double d_x = direction[0];
        double d_y = direction[1];
        double d_z = direction[2];


        //        System.out.println("DrawingGlyph");
        //        System.out.println("\torigin = " + o_x + " " + o_y + " " + o_z);
        //        System.out.println("\tdirection = " + d_x + " " + d_y + " " + d_z);

        double   minx     = 0.0f,
                 miny     = 0.0f,
                 minz     = 0.0f;
        double[] buff     = new double[3];

        boolean  isDouble = (points.get(0) instanceof double[]);

        for (int i = 0; i < points.size(); i++) {
            double[] d = (isDouble
                          ? (double[]) points.get(i)
                          : control.getNavigatedDisplay()
                              .getSpatialCoordinates((EarthLocation) points
                                  .get(i), buff));

            double x   = d[IDX_X] - o_x;
            double y   = d[IDX_Y] - o_y;
            double z   = d[IDX_Z] - o_z;
            double dot = x * d_x + y * d_y + z * d_z;
            x = x - dot * d_x;
            y = y - dot * d_y;
            z = z - dot * d_z;
            double tmp = Math.sqrt(x * x + y * y + z * z);
            if (tmp < minDistance) {
                minDistance = tmp;
                minx        = x;
                miny        = y;
                minz        = z;
            }
        }
        //        System.err.println ("\tdistance=" + minDistance +"  xyz=" + minx +" " +         miny + " " + minz);
        return minDistance;
    }



    /** Temp thing */
    private double[] tmp;


    /**
     * Get xyz of location
     *
     * @param el Location
     *
     * @return xyz
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    double[] getSpatialCoordinates(EarthLocation el)
            throws VisADException, RemoteException {
        return control.getNavigatedDisplay().getSpatialCoordinates(el, null);
    }





    /**
     * Square value
     *
     * @param v1 Value
     *
     * @return Squared
     */
    public static double squared(double v1) {
        return v1 * v1;
    }


    /**
     * Find index of closest point
     *
     * @param location From
     * @param points Points
     *
     * @return Index
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public int closestPoint(double[] location, List points)
            throws VisADException, RemoteException {
        int    minIndex    = -1;
        double minDistance = Double.MAX_VALUE;
        if (points.size() == 0) {
            return minIndex;
        }

        if (points.get(0) instanceof double[]) {
            for (int i = 0; i < points.size(); i++) {
                double[] d   = (double[]) points.get(i);
                double   tmp = distanceBetween(location, d);
                if (tmp < minDistance) {
                    minDistance = tmp;
                    minIndex    = i;
                }
            }
        } else if (points.get(0) instanceof EarthLocation) {
            for (int i = 0; i < points.size(); i++) {
                EarthLocation el  = (EarthLocation) points.get(i);
                double        tmp = distanceBetween(location, el);
                if (tmp < minDistance) {
                    minDistance = tmp;
                    minIndex    = i;
                }
            }
        }
        return minIndex;
    }



    /**
     * Smooth the curve
     *
     * @param curve The curve
     * @param window Smooth window
     *
     * @return Smoothed curve
     */
    public static double[][] smoothCurve(double[][] curve, int window) {
        int        length2  = curve[0].length;
        int        length1  = curve.length;
        double[][] newcurve = new double[length1][length2];
        for (int i = 0; i < length2; i++) {
            int win = window;
            if (i < win) {
                win = i;
            }
            int ii = (length2 - 1) - i;
            if (ii < win) {
                win = ii;
            }
            double runx = 0.0;
            double runy = 0.0;
            double runz = 0.0;
            for (int j = i - win; j <= i + win; j++) {
                runx += curve[0][j];
                runy += curve[1][j];
                if (length1 > 2) {
                    runz += curve[2][j];
                }
            }
            newcurve[0][i] = (double) (runx / (2 * win + 1));
            newcurve[1][i] = (double) (runy / (2 * win + 1));
            if (length1 > 2) {
                newcurve[2][i] = (double) (runz / (2 * win + 1));
            }
        }
        return newcurve;
    }




    /**
     * Set the Zposition property.
     *
     * @param value The new value for Zposition
     */
    public void setZPosition(float value) {
        zPosition = value;
    }

    /**
     * Get the ZPosition property.
     *
     * @return The ZPosition
     */
    public float getZPosition() {
        return zPosition;
    }

    /**
     *  Set the Points property.
     *
     *  @param value The new value for Points
     */
    public void setPoints(List value) {
        points = value;
    }

    /**
     *  Get the Points property.
     *
     *  @return The Points
     */
    public List getPoints() {
        return points;
    }


    /**
     * Get xyz of point at given index
     *
     * @param i Index
     *
     * @return xyz
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected double[] getBoxPoint(int i)
            throws VisADException, RemoteException {
        return getBoxPoint(i, points);
    }


    /**
     *  Get xyz of point at given index
     *
     *  @param i Index
     *  @param l Points
     *
     *  @return xyz
     *
     *  @throws RemoteException On badness
     *  @throws VisADException On badness
     */
    protected double[] getBoxPoint(int i, List l)
            throws VisADException, RemoteException {
        return getBoxPoint(l.get(i));
    }

    /**
     * Convert point (either  latlonalt or xyz) to xyz
     *
     * @param point Point
     *
     * @return xyz
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected double[] getBoxPoint(Object point)
            throws VisADException, RemoteException {
        if (point instanceof double[]) {
            return (double[]) point;
        }
        return control.earthToBox((EarthLocation) point);
    }


    /**
     * Swap array values at index
     *
     * @param a1 Array 1
     * @param a2 Array 2
     * @param index Index
     */
    protected void swap(double[] a1, double[] a2, int index) {
        double tmp = a1[index];
        a1[index] = a2[index];
        a2[index] = tmp;
    }


    /**
     * Swap array values at index
     *
     * @param a1 Array 1
     * @param a2 Array 2
     * @param index Index
     */
    protected void swap(float[] a1, float[] a2, int index) {
        float tmp = a1[index];
        a1[index] = a2[index];
        a2[index] = tmp;
    }



    /**
     * Find bounding box of points
     *
     *
     * @param points Points
     * @return bbox
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected List getBoundingBox(List points)
            throws VisADException, RemoteException {
        double[] min = new double[3];
        double[] max = new double[3];
        for (int i = 0; i < points.size(); i++) {
            double[] pt = (double[]) points.get(i);
            for (int j = 0; j < 3; j++) {
                if ((i == 0) || (pt[j] < min[j])) {
                    min[j] = pt[j];
                }
                if ((i == 0) || (pt[j] > max[j])) {
                    max[j] = pt[j];
                }
            }
        }
        List bbox = new ArrayList();
        bbox.add(new double[] { min[0], min[1], min[2] });
        bbox.add(new double[] { max[0], min[1], min[2] });
        bbox.add(new double[] { max[0], max[1], min[2] });
        bbox.add(new double[] { min[0], max[1], min[2] });
        return bbox;
    }






    /**
     * Set the Filled property.
     *
     * @param value The new value for Filled
     */
    public void setFilled(boolean value) {
        filled = value;
    }

    /**
     * Get the Filled property.
     *
     * @return The Filled
     */
    public boolean getFilled() {
        return filled;
    }


    /**
     * Set the Pickable property.
     *
     * @param value The new value for Pickable
     */
    public void setPickable(boolean value) {
        pickable = value;
    }

    /**
     * Get the Pickable property.
     *
     * @return The Pickable
     */
    public boolean getPickable() {
        return pickable;
    }



    /**
     * Set the Fulllatlon property.
     *
     * @param value The new value for Fulllatlon
     */
    public void setFullLatLon(boolean value) {
        fullLatLon = value;
    }

    /**
     * Get the FullLatLon property.
     *
     * @return The FullLatLon
     */
    public boolean getFullLatLon() {
        return fullLatLon;
    }




    /**
     * Set the BeenRemoved property.
     *
     * @param value The new value for BeenRemoved
     */
    public void setBeenRemoved(boolean value) {
        beenRemoved = value;
    }

    /**
     * Get the BeenRemoved property.
     *
     * @return The BeenRemoved
     */
    public boolean getBeenRemoved() {
        return beenRemoved;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * to string
     *
     * @return tostring
     */
    public String toString() {
        return name;
    }


    /**
     * Set the Editable property.
     *
     * @param value The new value for Editable
     */
    public void setEditable(boolean value) {
        editable = value;
    }

    /**
     * Get the Editable property.
     *
     * @return The Editable
     */
    public boolean getEditable() {
        return editable;
    }


    /**
     * Set the VisibleFlag property.
     *
     * @param value The new value for VisibleFlag
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setVisibleFlag(boolean value)
            throws VisADException, RemoteException {
        visibleFlag = value;
        setVisible(value);
    }

    /**
     * Get the VisibleFlag property.
     *
     * @return The VisibleFlag
     */
    public boolean getVisibleFlag() {
        return visibleFlag;
    }



    /**
     * is this glyph a raster thing
     *
     * @return is raster
     */
    public boolean getIsRaster() {
        return false;
    }


    /**
     *  Set the CreatedByUser property.
     *
     *  @param value The new value for CreatedByUser
     */
    public void setCreatedByUser(boolean value) {
        createdByUser = value;
    }

    /**
     *  Get the CreatedByUser property.
     *
     *  @return The CreatedByUser
     */
    public boolean getCreatedByUser() {
        return createdByUser;
    }



}


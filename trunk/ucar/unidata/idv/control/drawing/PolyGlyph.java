/*
 * $Id: PolyGlyph.java,v 1.44 2007/04/16 20:53:47 jeffmc Exp $
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


import org.w3c.dom.Element;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.*;
import ucar.unidata.idv.flythrough.FlythroughPoint;


import ucar.unidata.idv.control.DrawingControl;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

//import ucar.visad.FrontDrawer;
import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


import javax.swing.event.ChangeEvent;



/**
 * Class PolyGlyph draws polygons
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.44 $
 */
public class PolyGlyph extends LineGlyph {

    /** Indices into arrays */
    public static final int IDX_X = 0;

    /** Indices into arrays */
    public static final int IDX_Y = 1;

    /** Indices into arrays */
    public static final int IDX_Z = 2;


    /** Xml attribute name */
    public static final String ATTR_SMOOTH = "smooth";

    /** Xml attribute name */
    public static final String ATTR_EXTRUDE = "extrude";


    /** Not sure what we do with this */
    private boolean extrude = false;

    /** Is polygon sooth */
    private boolean smooth = false;

    /** Last point */
    private Object lastPoint = null;

    /** is this polygon closed          */
    private boolean closed = false;


    /**
     * Ctor
     */
    public PolyGlyph() {}


    /**
     * Ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     * @param smooth Is smooth
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public PolyGlyph(DrawingControl control, DisplayEvent event,
                     boolean smooth)
            throws VisADException, RemoteException {
        super(control, event);
        this.smooth = smooth;
        setFilled(control.getFilled());
    }


    /**
     * Do we have a valid number of points
     *
     * @return Is this glyph valid
     */
    public boolean isValid() {
        return (points != null) && (points.size() >= 2);
    }



    /**
     * get the prop gui comps
     *
     * @param comps List of comps
     * @param compMap comp map
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        super.getPropertiesComponents(comps, compMap);

        comps.add(new JLabel(""));
        comps.add(GuiUtils.makeButton("Start Flythrough",this, "doFlythrough"));
    }

    /**
     * delete the point closest to the event
     *
     * @param event the event
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public void doDeletePoint(DisplayEvent event)
            throws VisADException, RemoteException {
        if ((stretchIndex < 0) || (stretchIndex >= points.size())) {
            return;
        }
        points.remove(stretchIndex);
        updateLocation();
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
        InputEvent inputEvent = event.getInputEvent();
        if ((inputEvent instanceof KeyEvent)) {
            KeyEvent keyEvent = (KeyEvent) inputEvent;
	    if (GuiUtils.isDeleteEvent(keyEvent)) {
                doDeletePoint(event);
                return;
            }
        }

        if ( !smooth || (event.getModifiers() & event.CTRL_MASK) != 0) {
            super.doStretch(event);
            return;
        }
        doInterpolatedStretch(event);
    }



    /**
     * Can we show calculated distance
     *
     * @return true
     */
    public boolean canShowDistance() {
        return true;
    }

    /**
     * Can we show calculated area
     *
     * @return true
     */
    public boolean canShowArea() {
        return true;
    }

    /**
     * Do the flythru
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doFlythrough() throws VisADException, RemoteException {
        ViewManager vm = control.getViewManager();
        if(vm instanceof MapViewManager) {

            if (isInLatLonSpace()) {
                List<FlythroughPoint> pts = new ArrayList<FlythroughPoint>();
                for(EarthLocation el: ((List<EarthLocation>)points)) {
                    pts.add(new FlythroughPoint(el));
                }
                ((MapViewManager)vm).flythrough(pts);
            } else {
                ((MapViewManager)vm).flythrough(getPointValues(true));
            }
        }
        if (propDialog != null) {
            propDialog.dispose();
        }
    }


    /**
     * Initialize from xml
     *
     *
     * @param control The control I'm in.
     * @param node The xml node
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initFromXml(DrawingControl control, Element node)
            throws VisADException, RemoteException {
        super.initFromXml(control, node);

        smooth  = XmlUtil.getAttribute(node, ATTR_SMOOTH, false);
        extrude = XmlUtil.getAttribute(node, ATTR_EXTRUDE, false);
    }


    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(Element e) {
        super.addAttributes(e);
        e.setAttribute(ATTR_SMOOTH, "" + smooth);
        if (extrude) {
            e.setAttribute(ATTR_EXTRUDE, "" + extrude);
        }
    }



    /**
     * Get points used to select this glyph.
     *
     * @return Selection points
     */
    protected List getSelectionPoints() {
        if ((actualPoints == null) || !getSmooth()) {
            return super.getSelectionPoints();
        }
        if (actualPoints.size() > 2) {
            return Misc.newList(actualPoints.get(0),
                                actualPoints.get(actualPoints.size() - 1));
        }
        return actualPoints;
    }


    /**
     * Get xml tag name to use
     *
     * @return Xml tag name
     */
    public String getTagName() {
        return TAG_POLYGON;
    }


    /**
     * Glyph moved. Update the Displayable location.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        if (points.size() == 0) {
            return;
        }

        //      if(fd==null)
        //          fd = new FrontDrawer(null, null, null, 8, FrontDrawer.WARM_FRONT);

        MathType   mathType = null;
        double[][] lineVals = getPointValuesDouble();
        if (isInXYSpace()) {
            mathType = RealTupleType.SpatialCartesian3DTuple;
        } else if (isInLatLonSpace()) {
            mathType = RealTupleType.LatitudeLongitudeAltitude;
        } else {
            //??
        }
        if (smooth) {
            //            lineVals = smoothCurve(lineVals, 4);
        }
        //        setActualPoints(lineVals);
        //        Data theData = new Gridded3DSet(mathType, lineVals, points.size());
        //      double[][]dv = Set.floatToDouble(lineVals);
        Data theData = new Gridded3DDoubleSet(mathType, lineVals,
                           points.size());
        try {
            Data filledData = tryToFill(Misc.toFloat(lineVals), theData);
            if (filledData != null) {
                theData = filledData;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        if (lineDisplayable != null) {
            lineDisplayable.setData(theData);
        }
        super.updateLocation();
    }


    /**
     * Handle event
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        return this;
    }



    /**
     * Handle event
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public DrawingGlyph handleMouseDragged(DisplayEvent event)
            throws VisADException, RemoteException {
        if (smooth) {
            points.add(getPoint(event));
        } else {
            lastPoint = getPoint(event);
            if (points.size() < 2) {
                points.add(getPoint(event));
            } else {
                points.set(points.size() - 1, getPoint(event));
            }
        }
        updateLocation();
        return this;
    }

    /**
     * handle the mouse event
     *
     * @param event event
     *
     * @return this
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public DrawingGlyph handleMouseReleased(DisplayEvent event)
            throws VisADException, RemoteException {
        if (closed && (points.size() > 0)) {
            points.add(points.get(0));
            updateLocation();
        }
        return super.handleMouseReleased(event);
    }

    /**
     * this glyph is done being created
     */
    public void doneBeingCreated() {
        super.doneBeingCreated();
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
        if (smooth || (points.size() < 2) || (lastPoint == null)) {
            return this;
        }
        InputEvent inputEvent = event.getInputEvent();
        if ( !(inputEvent instanceof KeyEvent)) {
            return this;
        }
        KeyEvent keyEvent = (KeyEvent) inputEvent;
        if (keyEvent.getKeyCode() != KeyEvent.VK_SPACE) {
            return this;
        }
        points.add(lastPoint);
        lastPoint = null;
        updateLocation();
        return this;
    }




    /**
     * Name to use for this glyph
     *
     * @return The glyph type name
     */
    public String getTypeName() {
        return (smooth
                ? "Smooth Polygon"
                : "Polygon");
    }



    /**
     * Get smooth
     *
     * @return  Smooth
     */
    public boolean getSmooth() {
        return smooth;
    }

    /**
     * Set smooth_
     *
     * @param v Smooth
     */
    public void setSmooth(boolean v) {
        smooth = v;
    }

    /**
     * Set the Extrude property.
     *
     * @param value The new value for Extrude
     */
    public void setExtrude(boolean value) {
        extrude = value;
    }

    /**
     * Get the Extrude property.
     *
     * @return The Extrude
     */
    public boolean getExtrude() {
        return extrude;
    }



    /**
     * Set the Closed property.
     *
     * @param value The new value for Closed
     */
    public void setClosed(boolean value) {
        closed = value;
    }

    /**
     * Get the Closed property.
     *
     * @return The Closed
     */
    public boolean getClosed() {
        return closed;
    }



}



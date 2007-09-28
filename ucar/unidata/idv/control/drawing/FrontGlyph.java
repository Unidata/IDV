/*
 * $Id: FrontGlyph.java,v 1.16 2007/08/08 18:44:36 jeffmc Exp $
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

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.DisplayConventions;


import ucar.unidata.idv.control.DrawingControl;

import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.view.geoloc.MapProjectionDisplay;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.display.FrontDrawer;

import visad.*;

import visad.georef.EarthLocation;


import visad.georef.MapProjection;


import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;


import javax.swing.event.ChangeEvent;



/**
 * Class FrontGlyph draws fronts
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.16 $
 */
public class FrontGlyph extends DrawingGlyph
//PolyGlyph 
{

    /** Shows the fronts */
    FrontDrawer frontDrawer;

    /** Xml attribute name */
    public static final String ATTR_FRONTTYPE = "fronttype";

    /** _The front type we show */
    private String frontType = FrontDrawer.TYPE_COLD_FRONT;

    /** Last point */
    private Object lastPoint = null;

    /** for changing type */
    private JComboBox typeBox;

    /** for flipping orientation */
    private JCheckBox flipItCbx;

    /** for flipping orientation */
    private boolean flipIt = false;


    /**
     * Ctor
     */
    public FrontGlyph() {}


    /**
     * Ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public FrontGlyph(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        this(control, event, FrontDrawer.TYPE_COLD_FRONT);
    }

    /**
     * ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     * @param type Front type
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public FrontGlyph(DrawingControl control, DisplayEvent event, String type)
            throws VisADException, RemoteException {
        //        super(control, event);
        super(control, event, true);
        frontType = type;
    }



    /**
     * Init at the end
     *
     * @return Success
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean initFinalInner()
            throws VisADException, RemoteException {
        if ( !super.initFinalInner()) {
            return false;
        }
        setCoordType(COORD_LATLON);
        frontDrawer = new FrontDrawer(8, frontType);
        frontDrawer.setFlipTheFlip(flipIt);
        HighLowGlyph.setAnimationSet(frontDrawer, getTimeValues());
        addDisplayable(frontDrawer);
        return true;
    }





    /**
     * noop
     *
     * @param displayable  displayable
     * @param c color
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void xxxxsetColor(Displayable displayable, Color c)
            throws VisADException, RemoteException {
        //noop
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
        if ((event.getModifiers() & event.CTRL_MASK) != 0) {
            super.doStretch(event);
            return;
        }
        doInterpolatedStretch(event);
    }


    /**
     * get the extra description to show in gui
     *
     * @return description
     */
    public String getExtraDescription() {
        List times = getTimeValues();
        if ((times != null) && (times.size() > 0)) {
            if (times.size() == 1) {
                return "Time:" + StringUtil.join(" ", times);
            } else {
                return "Times:" + StringUtil.join(" ", times);
            }
        }
        return "";
    }




    /**
     * get the prop gui comps
     *
     * @param comps List of comps
     * @param compMap comp map
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        //Call parent with dummy list
        super.getPropertiesComponents(new Vector(), compMap);
        if (getCreatedByUser()) {
            Object selected = null;
            Vector items    = new Vector();
            for (int i = 0; i < FrontDrawer.TYPES.length; i++) {
                String type = FrontDrawer.TYPES[i];
                TwoFacedObject tfo =
                    new TwoFacedObject(FrontDrawer.getLabel(type), type);
                if (type.equals(frontType)) {
                    selected = tfo;
                }
                items.add(tfo);
            }

            flipItCbx = new JCheckBox("Flip the front orientation", flipIt);
            typeBox   = new JComboBox(items);
            if (selected != null) {
                typeBox.setSelectedItem(selected);
            }
            comps.add(GuiUtils.rLabel("Front Type:"));
            comps.add(GuiUtils.left(typeBox));

            comps.add(GuiUtils.filler());
            comps.add(GuiUtils.left(flipItCbx));
        }
        //        super.getPropertiesComponents(comps, compMap);
        getTimePropertiesComponents(comps, compMap);
    }

    /**
     * apply props
     *
     * @param compMap holds comps
     *
     * @return ok
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean applyProperties(Hashtable compMap)
            throws VisADException, RemoteException {
        if (typeBox != null) {
            if (flipIt != flipItCbx.isSelected()) {
                flipIt = flipItCbx.isSelected();
                if (frontDrawer != null) {
                    frontDrawer.setFlipTheFlip(flipIt);
                }
            }

            String newType =
                TwoFacedObject.getIdString(typeBox.getSelectedItem());
            frontType = newType;
            if (frontDrawer != null) {
                frontDrawer.setFrontType(frontType);
            }
        }
        return super.applyProperties(compMap);
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
     * Do the flythru
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doFlythrough() throws VisADException, RemoteException {
        control.getNavigatedDisplay().flythrough(getPointValues(true));
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
        frontType = XmlUtil.getAttribute(node, ATTR_FRONTTYPE, frontType);
        setName(FrontDrawer.getLabel(frontType));
    }


    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(Element e) {
        super.addAttributes(e);
        e.setAttribute(ATTR_FRONTTYPE, frontType);
    }

    /**
     * Update location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void viewpointChanged() throws VisADException, RemoteException {
        if (frontDrawer != null) {
            updateLocation();
        }
    }

    /**
     * Update location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void projectionChanged() throws VisADException, RemoteException {
        if (frontDrawer != null) {
            updateLocation();
        }
    }

    /**
     * Utility
     *
     * @param control The control we're in
     * @param frontDrawer The drawer
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static void setBaseScale(DrawingControl control,
                                    FrontDrawer frontDrawer)
            throws VisADException, RemoteException {
        EarthLocation el1 = control.boxToEarth(new double[] { -1, 0, 0 });
        EarthLocation el2 = control.boxToEarth(new double[] { 1, 0, 0 });
        double width =
            Math.abs(el1.getLatLonPoint().getLongitude().getValue()
                     - el2.getLatLonPoint().getLongitude().getValue());
        //Guess at a base scale which is 1/4 of the width of the visadbox in degrees
        //go figure
        frontDrawer.setScale(width / 4);
    }

    /**
     * Get points used to select this glyph.
     *
     * @return Selection points
     */
    protected List getSelectionPoints() {
        if ((actualPoints == null)) {
            return super.getSelectionPoints();
        }
        if (actualPoints.size() > 2) {
            return Misc.newList(actualPoints.get(0),
                                actualPoints.get(actualPoints.size() - 1));
        }
        return actualPoints;
    }



    /**
     * Get the default Z position for  the glyph.
     *
     * @return _more_
     */
    protected float getDefaultZPosition() {
        return (control == null)
               ? -.98f
               : super.getDefaultZPosition();
    }


    /**
     * Glyph moved. Update the Displayable location.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        if (points.size() == 0) {
            System.out.println("points = 0");
            return;
        }
        float[][] curve = getPointValues();
        setBaseScale(control, frontDrawer);
        frontDrawer.setConstantPosition(
            control.getVerticalValue(getZPosition()),
            control.getNavigatedDisplay().getDisplayAltitudeType());
        frontDrawer.setCurve(curve, getTimeValues());
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
        points.add(getPoint(event));
        updateLocation();
        return this;
    }


    /**
     * Get xml tag name to use
     *
     * @return Xml tag name
     */
    public String getTagName() {
        return TAG_FRONT;
    }


    /**
     * Name to use for this glyph
     *
     * @return The glyph type name
     */
    public String getTypeName() {
        return "Front";
    }



    /**
     * Set the FrontType property.
     *
     * @param value The new value for FrontType
     */
    public void setFrontType(String value) {
        frontType = value;
    }

    /**
     * Get the FrontType property.
     *
     * @return The FrontType
     */
    public String getFrontType() {
        return frontType;
    }

    /**
     * Set the FlipIt property.
     *
     * @param value The new value for FlipIt
     */
    public void setFlipIt(boolean value) {
        flipIt = value;
    }

    /**
     * Get the FlipIt property.
     *
     * @return The FlipIt
     */
    public boolean getFlipIt() {
        return flipIt;
    }



}


/*
 * $Id: ArrowGlyph.java,v 1.2 2007/04/16 20:53:46 jeffmc Exp $
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
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.view.geoloc.MapProjectionDisplay;

import ucar.unidata.xml.XmlUtil;


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
 * Class ArrowGlyph draws fronts
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.2 $
 */
public class ArrowGlyph extends DrawingGlyph {

    /** Draws shape */
    FrontDrawer frontDrawer;

    /**
     * Ctor
     */
    public ArrowGlyph() {}


    /**
     * Ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public ArrowGlyph(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        super(control, event);
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
        frontDrawer = new FrontDrawer(8, FrontDrawer.TYPE_UPPER_LEVEL_JET,
                                      false);
        addDisplayable(frontDrawer);
        //frontDrawer.setLineWidth(getLineWidth());
        return true;
    }

    /**
     * make the displayable
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void createDisplayable()
            throws VisADException, RemoteException {}


    /**
     * Set width
     *
     * @param value width
     */
    public void setLineWidth(float value) {
        //super.setLineWidth(value);
        try {
            if (frontDrawer != null) {
                frontDrawer.setLineWidth(value);
            }
        } catch (Exception exc) {
            LogUtil.logException("Setting line width", exc);
        }
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
     * viewpoint changed
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
     * projection  changed
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
     * Get points used to select this glyph.
     *
     * @return Selection points
     */
    protected List getSelectionPoints() {
        if (actualPoints == null) {
            return super.getSelectionPoints();
        }
        if (actualPoints.size() > 2) {
            return Misc.newList(actualPoints.get(0),
                                actualPoints.get(actualPoints.size() - 1));
        }
        return actualPoints;
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
        float[][] curve  = getLatLons(points);
        int       length = curve[0].length;
        //Flip them so we get the arrow pointed in the right direction
        float[][] inverse = new float[curve.length][length];
        for (int i = 0; i < curve.length; i++) {
            for (int j = 0; j < curve[i].length; j++) {
                inverse[i][length - j - 1] = curve[i][j];
            }
        }
        FrontGlyph.setBaseScale(control, frontDrawer);
        frontDrawer.setCurve(inverse, getTimeValues());
        super.updateLocation();
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
        return TAG_ARROW;
    }


    /**
     * Name to use for this glyph
     *
     * @return The glyph type name
     */
    public String getTypeName() {
        return "Arrow";
    }

}


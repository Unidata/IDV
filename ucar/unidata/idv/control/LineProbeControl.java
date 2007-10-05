/*
 * $Id: LineProbeControl.java,v 1.48 2007/04/11 18:56:03 jeffmc Exp $
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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.grid.GridUtil;


import ucar.unidata.idv.ViewDescriptor;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.ShapeUtility;
import ucar.visad.display.LineProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.SelectorPoint;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;


import java.util.List;

import javax.swing.*;
import javax.swing.event.*;



/**
 * An abstract base class that manages a vertical probe
 * To create a probe call doMakeProbe
 * To be notified of changes override:
 * void probePositionChanged (double x, double y);
 *
 * @author IDV development team
 * @version $Revision: 1.48 $Date: 2007/04/11 18:56:03 $
 */
public abstract class LineProbeControl extends GridDisplayControl {

    /** profile sharing property */
    public static final String SHARE_PROFILE =
        "LineProbeControl.SHARE_PROFILE";

    /** the line probe */
    private LineProbe probe;

    /** the initial position */
    private RealTuple initPosition;

    /** The shape for the probe point */
    private String marker;

    /** The point size */
    private float pointSize = 1.0f;

    /**
     * Default Constructor.
     */
    public LineProbeControl() {
        setAttributeFlags(FLAG_COLOR);
    }

    /**
     * Default doMakeProbe method.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doMakeProbe() throws VisADException, RemoteException {
        doMakeProbe(getColor());
    }

    /**
     * Make the probe with the specific <code>Color</code>.
     *
     * @param c  color for probe.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doMakeProbe(Color c) throws VisADException, RemoteException {
        doMakeProbe(c, getDefaultViewDescriptor());
    }


    /**
     * Make the probe with the specific <code>ViewDescriptor</code>.
     *
     * @param view  view descriptor
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doMakeProbe(ViewDescriptor view)
            throws VisADException, RemoteException {
        doMakeProbe(getColor(), view);
    }

    /**
     * Make the probe with the specific <code>Color</code> and
     * <code>ViewDescriptor</code>.
     *
     * @param probeColor    color for the probe
     * @param view  view descriptor
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doMakeProbe(Color probeColor, ViewDescriptor view)
            throws VisADException, RemoteException {
        probe = null;
        if (getDisplayAltitudeType().equals(Display.Radius)) {
            //      System.err.println("Probe 1");
            probe = new LineProbe(
                new RealTuple(
                    RealTupleType.SpatialEarth2DTuple, new double[] { 0,
                    0 }));
        } else if (initPosition != null) {
            //      System.err.println("Probe 2");
            probe = new LineProbe(initPosition);
        } else {
            //      System.err.println("Probe 3");
            probe = new LineProbe(getInitialLinePosition());
            //            probe = new LineProbe(getGridCenterPosition());
        }
        initPosition = probe.getPosition();

        // it is a little colored cube 8 pixels across
        probe.setColor(probeColor);
        probe.setVisible(true);
        probe.addPropertyChangeListener(this);
        probe.setPointSize(getDisplayScale());
        if (marker != null) {
            probe.setMarker(
                SelectorPoint.reduce(ShapeUtility.makeShape(marker)));
        }
        probe.setAutoSize(true);
        addDisplayable(probe, view, FLAG_COLOR);
    }


    /**
     * Handle changes
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                SelectorDisplayable.PROPERTY_POSITION)) {
            doMoveProbe();
        } else {
            super.propertyChange(evt);
        }
    }

    /**
     * Reset the position of the probe to the center.
     */
    public void resetProbePosition() {
        try {
            setProbePosition(0.0, 0.0);
        } catch (Exception exc) {
            logException("Resetting probe position", exc);
        }
    }


    /**
     * Get edit menu items
     *
     * @param items      list of menu items
     * @param forMenuBar  true if for the menu bar
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        if (probe != null) {
            JMenuItem mi = new JMenuItem("Reset Probe Position");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    resetProbePosition();
                }
            });
            items.add(mi);
        }
        super.getEditMenuItems(items, forMenuBar);
    }

    /**
     * Set the probe position.  Probes are set in XY space.
     *
     * @param xy  X and Y position of the probe.
     *
     * @throws VisADException  problem setting probe position
     * @throws RemoteException  problem setting probe position on remote display
     */
    public void setProbePosition(RealTuple xy)
            throws VisADException, RemoteException {
        probe.setPosition(xy);
    }

    /**
     * Set the probe position from display x and y positions.
     *
     * @param x    X position of the probe.
     * @param y    Y position of the probe.
     *
     * @throws VisADException  problem setting probe position
     * @throws RemoteException  problem setting probe position on remote display
     */
    public void setProbePosition(double x, double y)
            throws VisADException, RemoteException {
        setProbePosition(new RealTuple(new Real[] {
            new Real(RealType.XAxis, x),
            new Real(RealType.YAxis, y) }));
    }

    /**
     * Set the initial position of the probe.  This is used by the
     * XML persistense.
     *
     * @param p  position
     */
    public void setPosition(RealTuple p) {
        initPosition = p;
    }

    /**
     * Get the position of the probe.  This is used by the
     * XML persistense.
     *
     * @return current probe position or null if probe has not been created.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public RealTuple getPosition() throws VisADException, RemoteException {
        return ((probe != null)
                ? probe.getPosition()
                : null);
    }

    /**
     * Get the initial position of the probe set during unpersistence.
     *
     * @return  initial position or <code>null</code> if not set during
     *          initialization.
     */
    public RealTuple getInitialPosition() {
        return initPosition;
    }

    /**
     * Method called when sharing is enabled.
     *
     * @param from  Sharable that send the data.
     * @param dataId  identifier for data to be shared
     * @param data   data to be shared.
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if (dataId.equals(SHARE_POSITION)) {
            if (probe == null) {
                return;
            }
            try {
                probe.setPosition((RealTuple) data[0]);
                probePositionChanged(getPosition());
            } catch (Exception e) {
                logException("receiveShareData:" + dataId, e);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }


    /**
     * Method called when probe is moved.
     */
    protected void doMoveProbe() {
        try {
            RealTuple position = getPosition();
            probePositionChanged(position);
            doShare(SHARE_POSITION, position);
        } catch (Exception e) {
            logException("doMoveProfile", e);
        }
    }

    /**
     * This gets called when either the user moves the probe point or
     * when we get a sharable event to move the probe point. Subclasses
     * need to implement this.
     *
     * @param position  new position for the probe.
     */
    protected void probePositionChanged(RealTuple position) {}

    /**
     * Respond to a change in the display's projection.  In this case
     * we fire the probePositionChanged() method with the probe's
     * position.
     */
    public void projectionChanged() {
        super.projectionChanged();
        try {
            probePositionChanged(getPosition());
        } catch (Exception exc) {
            logException("projectionChanged", exc);
        }
    }

    /**
     * Make a menu for controlling the probe size, shape and position.
     *
     * @param probeMenu The menu to add to
     *
     * @return The menu
     */
    public JMenu doMakeProbeMenu(JMenu probeMenu) {
        JMenu posMenu = new JMenu("Position");
        probeMenu.add(posMenu);
        posMenu.add(GuiUtils.makeMenuItem("Reset Probe Position", this,
                                          "resetProbePosition"));

        JMenu sizeMenu = new JMenu("Size");
        probeMenu.add(sizeMenu);

        sizeMenu.add(GuiUtils.makeMenuItem("Increase", this,
                                           "increaseProbeSize"));
        sizeMenu.add(GuiUtils.makeMenuItem("Decrease", this,
                                           "decreaseProbeSize"));

        JMenu shapeMenu = new JMenu("Probe Shape");
        probeMenu.add(shapeMenu);
        for (int i = 0; i < ShapeUtility.SHAPES.length; i++) {
            TwoFacedObject tof = ShapeUtility.SHAPES[i];
            String         lbl = tof.toString();
            if (Misc.equals(tof.getId(), marker)) {
                lbl = ">" + lbl;
            }
            JMenuItem mi = GuiUtils.makeMenuItem(lbl, this, "setMarker",
                               tof.getId());
            shapeMenu.add(mi);
        }
        GuiUtils.limitMenuSize(shapeMenu, "Shape Group ", 10);
        return probeMenu;
    }

    /**
     * Increase the probe size
     */
    public void increaseProbeSize() {
        if (probe == null) {
            return;
        }
        pointSize = probe.getPointScale();
        setPointSize(pointSize + pointSize * 0.5f);
    }


    /**
     * Decrease the probe size
     */
    public void decreaseProbeSize() {
        if (probe == null) {
            return;
        }
        pointSize = probe.getPointScale();
        pointSize = pointSize - pointSize * 0.5f;
        if (pointSize < 0.1f) {
            pointSize = 0.1f;
        }
        setPointSize(pointSize);
    }


    /**
     *  Set the PointSize property.
     *
     *  @param value The new value for PointSize
     */
    public void setPointSize(float value) {
        pointSize = value;
        if (probe != null) {
            try {
                probe.setAutoSize(false);
                probe.setPointSize(pointSize);
                probe.setAutoSize(true);
            } catch (Exception exc) {
                logException("Increasing probe size", exc);
            }
        }
    }

    /**
     *  Get the PointSize property.
     *
     *  @return The PointSize
     */
    public float getPointSize() {
        return pointSize;
    }


    /**
     * Get initial XY position from grid data.
     *
     * @return initial XY position of grid center point in VisAD space
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public RealTuple getGridCenterPosition()
            throws VisADException, RemoteException {
        RealTuple pos = new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                                      new double[] { 0,
                0 });
        if (getGridDataInstance() != null) {
            LatLonPoint rt = GridUtil.getCenterLatLonPoint(
                                 getGridDataInstance().getGrid());
            RealTuple xyz = earthToBoxTuple(new EarthLocationTuple(rt,
                                new Real(RealType.Altitude, 0)));
            if (xyz != null) {
                pos = new RealTuple(new Real[] { (Real) xyz.getComponent(0),
                        (Real) xyz.getComponent(1) });
            }
        }
        return pos;
    }


    /**
     * Get initial XY position from the screen
     *
     * @return initial XY position  in VisAD space
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public RealTuple getInitialLinePosition()
            throws VisADException, RemoteException {
        double[] center = getScreenCenter();
        return new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                             new double[] { center[0],
                                            center[1] });
    }

    /**
     * Set the Marker property.
     *
     * @param value The new value for Marker
     */
    public void setMarker(String value) {
        marker = value;
        if ((probe != null) && (marker != null)) {
            try {
                probe.setAutoSize(false);
                probe.setMarker(
                    SelectorPoint.reduce(ShapeUtility.makeShape(marker)));
                probe.setAutoSize(true);
            } catch (Exception exc) {
                logException("Setting marker", exc);
            }
        }
    }

    /**
     * Get the Marker property.
     *
     * @return The Marker
     */
    public String getMarker() {
        return marker;
    }

}


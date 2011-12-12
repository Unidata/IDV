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

package ucar.visad.display;


import visad.Action;

import visad.ActionImpl;

import visad.ConstantMap;

import visad.Display;

import visad.Gridded3DSet;

import visad.Real;

import visad.RealTuple;

import visad.RealTupleType;

import visad.RealType;

import visad.VisADException;
import visad.VisADGeometryArray;


import java.rmi.RemoteException;

import java.util.Iterator;


/**
 * Class for a probe.
 *
 * @author Don Murray
 * @version $Revision: 1.11 $
 */
public class PointProbe extends SelectorDisplayable {

    /** probe */
    private SelectorPoint point;

    /** flag for whether we're in the process of setting the position */
    private volatile boolean settingPosition = false;

    /**
     * Construct a point probe.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public PointProbe() throws VisADException, RemoteException {
        this(0, 0, 0);
    }

    /**
     * Construct a point probe at the location specified
     *
     * @param x   x position
     * @param y   y position
     * @param z   z position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public PointProbe(double x, double y, double z)
            throws VisADException, RemoteException {

        this(new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                           new double[] { x,
                                          y, z }));
    }

    /**
     * Construct a probe at the position specified.
     *
     * @param position  position of the probe
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public PointProbe(RealTuple position)
            throws VisADException, RemoteException {

        point = new SelectorPoint("Probe point", position);

        addDisplayable(point);
        setPosition(position);
        point.addAction(new ActionImpl("point listener") {
            public void doAction() throws VisADException, RemoteException {
                if (settingPosition) {
                    return;
                }
                notifyListenersOfMove();
            }
        });
    }


    /**
     * Get the selector point
     *
     * @return the selector point
     */
    public SelectorPoint getSelectorPoint() {
        return point;
    }

    /**
     * Set if any of the axis movements are fixed
     *
     * @param x x fixed
     * @param y y fixed
     * @param z z fixed
     */
    public void setFixed(boolean x, boolean y, boolean z) {
        point.setFixed(x, y, z);
    }


    /**
     * Get the point scale
     *
     * @return the point scale
     */
    public float getPointScale() {
        if (point != null) {
            return point.getScale();
        }
        return 1.0f;
    }

    /**
     * Set the type of marker used for the probe.
     *
     * @param marker  marker as a VisADGeometryArray
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setMarker(VisADGeometryArray marker)
            throws VisADException, RemoteException {
        point.setMarker(marker);
    }


    /**
     * Set the type of marker used for the probe.
     *
     * @param marker  marker from ucar.visad.ShapeUtility
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setMarker(String marker)
            throws VisADException, RemoteException {
        point.setMarker(marker);
    }




    /**
     * Set whether the marker should automatically resize as the
     * display is zoomed.
     *
     * @param yesorno  true to automatically resize the marker.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setAutoSize(boolean yesorno)
            throws VisADException, RemoteException {
        point.setAutoSize(yesorno);
    }

    /**
     * Get the position of the probe.
     * @return  probe's position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public RealTuple getPosition() throws VisADException, RemoteException {
        return point.getPoint();
    }

    /**
     * Set the probe's x/y position
     *
     * @param x  x position
     * @param y  y position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setPosition(double x, double y)
            throws VisADException, RemoteException {

        setPosition(new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                                  new double[] { x,
                y }));
    }

    /**
     * Set the probe's position.
     *
     * @param position  position of the probe
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setPosition(RealTuple position)
            throws VisADException, RemoteException {

        settingPosition = true;
        try {
            point.setPoint(position);
        } finally {
            settingPosition = false;
        }
    }
}

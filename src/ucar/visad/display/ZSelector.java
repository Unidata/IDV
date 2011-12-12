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


import ucar.unidata.util.Misc;

import visad.Action;

import visad.ActionImpl;

import visad.ConstantMap;

import visad.Display;

import visad.Gridded2DSet;

import visad.Real;

import visad.RealTuple;

import visad.RealTupleType;

import visad.RealType;

import visad.VisADException;


import java.awt.Color;

import java.rmi.RemoteException;


/**
 * ZSelector is a single small color-filled box on the screen that the
 * user can move vertically by dragging with mouse button three; it can
 * be used to control items whose position has a z value in a VisAD display.
 *
 * @author Metapps development group
 * @version $Revision: 1.14 $
 */
public class ZSelector extends SelectorDisplayable {

    /** the selector point */
    private SelectorPoint zSelector;

    /**
     * Construct a ZSelector with default color blue.
     *
     * @param x x position in VisAd -1 to 1 scale
     * @param y y position in VisAd -1 to 1 scale
     * @param z z position in VisAd -1 to 1 scale
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     *
     */
    public ZSelector(double x, double y, double z)
            throws VisADException, RemoteException {
        init(x, y, z, Color.blue);
    }

    /**
     * Construct a ZSelector.
     *
     * @param x x position in VisAd -1 to 1 scale
     * @param y y position in VisAd -1 to 1 scale
     * @param z z position in VisAd -1 to 1 scale
     * @param color the Java Color of the box.
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     *
     */
    public ZSelector(double x, double y, double z, Color color)
            throws VisADException, RemoteException {
        init(x, y, z, color);
    }

    /**
     * Construct a ZSelector from another instance
     *
     * @param   that   other instance
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public ZSelector(ZSelector that) throws VisADException, RemoteException {
        super(that);
    }

    /**
     * Create the actual z selector point;
     * x,y,z are on VisAD -1 to 1 scale
     *
     * @param x  x position for the selector point
     * @param y  y position for the selector point
     * @param z  z position for the selector point
     * @param color the Color of the selector.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private void init(double x, double y, double z, Color color)
            throws VisADException, RemoteException {

        Real[] reals = new Real[] { new Real(RealType.ZAxis, z) };

        zSelector = new SelectorPoint("height selector",
                                      new RealTuple(reals));

        zSelector.addConstantMap(new ConstantMap(x, Display.XAxis));
        zSelector.addConstantMap(new ConstantMap(y, Display.YAxis));
        zSelector.setVisible(true);
        zSelector.addAction(new ActionImpl("Z Selector Listener") {
            Real    lastZPosition = null;
            boolean first         = true;
            public void doAction() {
                try {
                    if ( !Misc.equals(getZSelectorPosition(), lastZPosition)
                            || first) {
                        notifyListenersOfMove();
                        lastZPosition = getZSelectorPosition();
                    }
                } catch (Exception excp) {}
                if (first) {
                    first = false;
                }
            }
        });
        zSelector.setManipulable(true);
        zSelector.setPointSize(getPointSize());
        setColor(color);
        addDisplayable(zSelector);
    }

    /**
     * Get the z value from this selector, the z value in the VisAd
     * -1 to 1 scale.
     *
     * @return  Real corresponding to the position
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public Real getZSelectorPosition()
            throws VisADException, RemoteException {
        return (Real) zSelector.getPoint().getComponent(0);
    }

    /**
     * Get the z value from this selector, the z value in the VisAd
     * -1 to 1 scale.
     *
     * @return Real corresponding to the position
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public Real getPosition() throws VisADException, RemoteException {
        return (Real) zSelector.getPoint().getComponent(0);
    }

    /**
     * Set the location along the Z Axis where you want to place the selector.
     *
     * @param newZValue  position along Z axis where component should be
     *                   located in the VisAd -1 to 1 scale.
     *
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public void setZValue(double newZValue)
            throws VisADException, RemoteException {
        RealTuple newPoint = new RealTuple(new Real[] {
                                 new Real(RealType.ZAxis, newZValue) });

        if ( !newPoint.equals(zSelector.getPoint())) {
            setOkToFireEvents(false);
            zSelector.setPoint(newPoint);
            setOkToFireEvents(true);
        }
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return                  A semi-deep clone of this instance.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws VisADException, RemoteException {
        return new ZSelector(this);
    }
}

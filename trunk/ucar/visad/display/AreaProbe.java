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


import visad.*;



import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;


import java.util.Iterator;


/**
 * Class for a probe that defines an area.
 *
 * @author IDV Development Team
 * @version $Revision: 1.4 $
 */
public class AreaProbe extends SelectorDisplayable {

    /** Type for the Area */
    private static RealTupleType areaType;

    /** Upper left selector point */
    private SelectorPoint ulPoint;

    /** Lower right selector point */
    private SelectorPoint lrPoint;

    /** Box outlining area */
    private LineDrawing box;

    /** constant */
    private Real constant;

    /** Default point size */
    private float pointSize = 5.0f;

    /** flag for whether the position is being set or not */
    private volatile boolean settingPosition = false;

    /**
     * Construct an AreaProbe with the defaults
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public AreaProbe() throws VisADException, RemoteException {
        //        this (new RealTuple(getAreaType (),  new double[]{ 0, 0, 0, 0 }));
        this(new RealTuple(getAreaType(), new double[] { -0.9, -0.9, 0.9,
                0.9 }));
    }

    /**
     * Construct an AreaProbe using the specified position
     *
     * @param position  position for probe
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public AreaProbe(RealTuple position)
            throws VisADException, RemoteException {
        this(position, new Real(RealType.ZAxis, 1));
    }

    /**
     * Construct an AreaProbe using the specified position using the
     * supplied constant
     *
     * @param position   probe position
     * @param constant   Z position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public AreaProbe(RealTuple position, Real constant)
            throws VisADException, RemoteException {

        if ( !position.getType().equals(getAreaType())) {
            throw new VisADException("Can't yet handle "
                                     + position.getType());
        }

        if ( !constant.getType().equals(RealType.ZAxis)) {
            throw new VisADException("Can't yet handle constant "
                                     + constant.getType());
        }

        this.constant = constant;




        ulPoint = new SelectorPoint("Probe point",
                                    RealTupleType.SpatialCartesian2DTuple);
        lrPoint = new SelectorPoint("Probe point",
                                    RealTupleType.SpatialCartesian2DTuple);
        box = new LineDrawing("AreaProbe.box");


        ulPoint.addConstantMap(new ConstantMap(constant.getValue(),
                Display.ZAxis));
        lrPoint.addConstantMap(new ConstantMap(constant.getValue(),
                Display.ZAxis));
        box.addConstantMap(new ConstantMap(constant.getValue(),
                                           Display.ZAxis));

        addDisplayable(ulPoint);
        addDisplayable(lrPoint);
        addDisplayable(box);

        setPosition(position);
        ActionImpl action = new ActionImpl("point listener") {
            public void doAction() throws VisADException, RemoteException {
                if (settingPosition) {
                    return;
                }
                setLinePosition();
                notifyListenersOfMove();
            }
        };

        ulPoint.addAction(action);
        lrPoint.addAction(action);
    }


    /**
     * set if area probe can be moved by the user with mouse cursor or not
     * @param manip boolean true if can move
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void setManipulable(boolean manip)
            throws VisADException, RemoteException {
        //        line.setManipulable(manip) ;
        ulPoint.setManipulable(manip);
        lrPoint.setManipulable(manip);
    }

    /**
     * Get the MathType of the area
     * @return  area's RealTupleType
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public static RealTupleType getAreaType()
            throws VisADException, RemoteException {
        if (areaType == null) {
            areaType = new RealTupleType(RealType.XAxis, RealType.YAxis,
                                         RealType.XAxis, RealType.YAxis);
        }
        return areaType;
    }

    /**
     * Get the values for the corners of the box.
     *
     * @return  values for the corners
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private double[] getValues() throws VisADException, RemoteException {
        Real[] ulReals = ulPoint.getPoint().getRealComponents();
        Real[] lrReals = lrPoint.getPoint().getRealComponents();
        return new double[] { ulReals[0].getValue(), ulReals[1].getValue(),
                              lrReals[0].getValue(), lrReals[1].getValue() };

    }

    /**
     * Get the Area as a RealTuple
     * @return  the area as a RealTuple
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public RealTuple getArea() throws VisADException, RemoteException {
        return new RealTuple(getAreaType(), getValues());
    }

    /**
     * Set whether the marker should automatically resize as the
     * display is zoomed.
     * @param yesorno  true to automatically resize the marker.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void setAutoSize(boolean yesorno)
            throws VisADException, RemoteException {
        ulPoint.setAutoSize(yesorno);
        lrPoint.setAutoSize(yesorno);
        super.setAutoSize(yesorno);
    }

    /**
     * Sets the position of the probe.  This method fires a {@link
     * PropertyChangeEvent} for {@link #PROPERTY_POSITION}.<p>
     *
     * <p>This implementation uses {@link #setPosition(RealTuple)} to actually
     * set the position of the probe.</p>
     *
     * @param x1    x position of the upper left corner
     * @param y1    y position of the upper left corner
     * @param x2    x position of the lower right corner
     * @param y2    y position of the lower right corner
     *
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void setPosition(double x1, double y1, double x2, double y2)
            throws VisADException, RemoteException {
        setPosition(new RealTuple(getAreaType(), new double[] { x1, y1, x2,
                y2 }));
    }

    /**
     * Sets the position of the probe.  This method fires a {@link
     * java.beans.PropertyChangeEvent} for {@link #PROPERTY_POSITION}.
     *
     * @param position              The position of the probe.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws VisADException       if <code>position.getType().equals(
     *                              RealTupleType.SpatialCartesian2DTuple)
     *                              </code> is false or if another VisAD failure
     *                              occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     * @see RealTupleType#SpatialCartesian2DTuple
     */
    public void setPosition(RealTuple position)
            throws VisADException, RemoteException {

        settingPosition = true;

        try {
            if ( !position.getType().equals(getAreaType())) {
                throw new VisADException("Can't yet handle "
                                         + position.getType());
            }
            Real[] values = position.getRealComponents();
            ulPoint.setPoint(
                new RealTuple(
                    RealTupleType.SpatialCartesian2DTuple,
                    new double[] { values[0].getValue(),
                                   values[1].getValue() }));
            lrPoint.setPoint(
                new RealTuple(
                    RealTupleType.SpatialCartesian2DTuple,
                    new double[] { values[2].getValue(),
                                   values[3].getValue() }));

            setLinePosition();
        } finally {
            settingPosition = false;
        }
    }


    /**
     * Set the line position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private void setLinePosition() throws VisADException, RemoteException {
        Real[]    ulReals = ulPoint.getPoint().getRealComponents();
        Real[]    lrReals = lrPoint.getPoint().getRealComponents();

        float     zValue  = (float) constant.getValue();
        float     left    = (float) ulReals[0].getValue();
        float     right   = (float) lrReals[0].getValue();
        float     top     = (float) ulReals[1].getValue();
        float     bottom  = (float) lrReals[1].getValue();

        float[][] boxVals = new float[][] {
            { left, right, right, left, left },
            { top, top, bottom, bottom, top },
            { zValue, zValue, zValue, zValue, zValue }
        };

        Gridded3DSet boxData =
            new Gridded3DSet(RealTupleType.SpatialCartesian3DTuple, boxVals,
                             5);
        if ( !boxData.equals(box.getData())) {
            box.setData(boxData);
        }
    }


}

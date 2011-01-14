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



import ucar.unidata.util.LogUtil;


import ucar.visad.ShapeUtility;

import visad.*;

import java.awt.Color;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.Iterator;


/**
 * CrossSectionSelector is a composite of two endpoints that can be
 * moved independently, a line connecting the two points, and
 * a middle point that can be used to move orthoganally to the line.
 *
 * @author Don Murray
 * @version $Revision: 1.28 $
 */
public class CrossSectionSelector extends SelectorDisplayable {


    /** position property */
    public static final String PROPERTY_STARTPOINT =
        "SelectorDisplay.startpoint";

    /** position property */
    public static final String PROPERTY_ENDPOINT = "SelectorDisplay.endpoint";

    /** position property */
    public static final String PROPERTY_MIDPOINT = "SelectorDisplay.midpoint";



    /** Real for the number 2 */
    private final static Real TWO = new Real(2.0);

    /** the profile line */
    private ProfileLine line;


    /** flag for initialization */
    private boolean beenInitialized = false;

    /** start point selector point */
    private SelectorPoint startSp;

    /** end point selector point */
    private SelectorPoint endSp;

    /** mid point selector point */
    private SelectorPoint midSp;

    /** start point location */
    private RealTuple startPoint;

    /** end point location */
    private RealTuple endPoint;

    /** mid point location */
    private RealTuple midPoint;

    /** The tuple type */
    private RealTupleType tupleType;

    /** RealTuple for zero */
    private RealTuple ZERO2D;

    /** base tuple */
    private RealTuple ZERO3D;

    /** flag for whether the selector is moving */
    private boolean amMoving = false;

    /** start point id */
    public static final int POINT_START = 0;

    /** end point id */
    public static final int POINT_END = 1;

    /** mid point id */
    public static final int POINT_MID = 2;

    /** Should we interpolate the line */
    private boolean interpolateLinePoints = false;

    /**
     * Construct a CrossSectionSelector with default.  Line will run
     * from (-1, 0) to (1,0) in (X,Y) space.
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     *
     */
    public CrossSectionSelector() throws VisADException, RemoteException {
        this(new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                           new double[] { -1.0,
                                          0.0 }), new RealTuple(
                                          RealTupleType.SpatialCartesian2DTuple,
                                          new double[] { 1.0,
                0.0 }));
    }

    /**
     * Construct a CrossSectionSelector along the points specified.
     *
     * @param   startPoint    XY position of starting point
     * @param   endPoint      XY position of ending point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public CrossSectionSelector(RealTuple startPoint, RealTuple endPoint)
            throws VisADException, RemoteException {
        this(startPoint, endPoint, Color.white);
    }

    /**
     * Construct a CrossSectionSelector along the points specified in the
     * Color specified.
     *
     * @param   startPoint    XY position of starting point
     * @param   endPoint      XY position of ending point
     * @param   color    color for all components of this composite
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public CrossSectionSelector(RealTuple startPoint, RealTuple endPoint,
                                Color color)
            throws VisADException, RemoteException {

        this.tupleType  = (RealTupleType) startPoint.getType();
        this.startPoint = startPoint;
        this.endPoint   = endPoint;
        midPoint        = calculateMidpoint();

        startSp         = makeSelectorPoint(POINT_START, "Start", startPoint);
        endSp           = makeSelectorPoint(POINT_END, "End", endPoint);

        midSp           = makeSelectorPoint(POINT_MID, "Middle", midPoint);

        line            = makeLine();
        ZERO2D = new RealTuple(RealTupleType.SpatialEarth2DTuple,
                               new double[] { 0.0,
                0.0 });
        ZERO3D = new RealTuple(RealTupleType.SpatialEarth3DTuple,
                               new double[] { 0.0,
                0.0, 0.0 });
        setColor(color);
        beenInitialized = true;
    }



    /**
     * get the selector point
     *
     * @return the selector point
     */
    public SelectorPoint getStartSelectorPoint() {
        return startSp;
    }


    /**
     * get the selector point
     *
     * @return the selector point
     */
    public SelectorPoint getEndSelectorPoint() {
        return endSp;
    }

    /**
     * get the selector point
     *
     * @return the selector point
     */
    public SelectorPoint getMiddleSelectorPoint() {
        return midSp;
    }


    /**
     * Remove the start point
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void dontShowStartPoint() throws VisADException, RemoteException {
        removeDisplayable(startSp);
    }

    /**
     * Remove the mid point
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void dontShowMiddlePoint() throws VisADException, RemoteException {
        removeDisplayable(midSp);
    }

    /**
     * Remove the end point
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void dontShowEndPoint() throws VisADException, RemoteException {
        removeDisplayable(endSp);
    }


    /**
     * Construct a CrossSectionSelector from another instance
     *
     * @param   that   other instance
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public CrossSectionSelector(CrossSectionSelector that)
            throws VisADException, RemoteException {
        super(that);
        positionHasChanged();
    }

    /**
     * Make the line
     * @return  the line.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private ProfileLine makeLine() throws VisADException, RemoteException {
        line = new ProfileLine("CrossSectionLine");
        setLineData();
        addDisplayable(line);
        return line;
    }

    /**
     * Set the data depicting the line
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setLineData() throws VisADException, RemoteException {
        RealTuple startRt   = startSp.getPoint();
        RealTuple endRt     = endSp.getPoint();

        int       numPoints = (interpolateLinePoints
                               ? 100
                               : 2);
        int       length    = startRt.getDimension();
        float[][] values    = new float[length][numPoints];
        for (int i = 0; i < length; i++) {
            float start = (float) ((Real) startRt.getComponent(i)).getValue();
            float end   = (float) ((Real) endRt.getComponent(i)).getValue();
            values[i][0]             = start;
            values[i][numPoints - 1] = end;
            for (int j = 1; j < numPoints - 1; j++) {
                double percent = j / (double) numPoints;
                values[i][j] = start + (float) (percent * (end - start));
            }
        }


        Data lineData;
        if (length == 2) {
            lineData = new Gridded2DSet(tupleType, values, numPoints);
        } else {
            lineData = new Gridded3DSet(tupleType, values, numPoints);
        }
        if ( !lineData.equals(line.getData())) {
            line.setData(lineData);
        }
    }

    /**
     * Set the size of the selector points.  The middle point is
     * always just a little bigger than the end points
     * @param  size  point size in pixels
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public void setPointSize(float size)
            throws VisADException, RemoteException {
        super.setPointSize(size);
        midSp.setPointSize(size + .02f);
    }

    /**
     * Set the color of the selector points.  The middle point is
     * always just a little darker than the end points
     * @param  newColor  color for components
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public void setColor(Color newColor)
            throws VisADException, RemoteException {
        super.setColor(newColor);
        midSp.setColor(newColor.darker());
    }

    /**
     * Set whether the marker should automatically resize as the
     * display is zoomed.
     * @param yesorno  true to automatically resize the marker.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setAutoSize(boolean yesorno)
            throws VisADException, RemoteException {
        startSp.setAutoSize(yesorno);
        midSp.setAutoSize(yesorno);
        endSp.setAutoSize(yesorno);
        super.setAutoSize(yesorno);
    }

    /**
     * Set the location along the Z Axis that you want to have
     * the line and points displayed
     * @param  zValue  position along Z axis where components should be located
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public void setZValue(double zValue)
            throws VisADException, RemoteException {
        startSp.addConstantMap(new ConstantMap(zValue, Display.ZAxis));
        endSp.addConstantMap(new ConstantMap(zValue, Display.ZAxis));
        midSp.addConstantMap(new ConstantMap(zValue, Display.ZAxis));
        line.addConstantMap(new ConstantMap(zValue, Display.ZAxis));
    }


    /**
     * Override base class method
     *
     * @param value Value
     * @param type Type
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setConstantPosition(double value, visad.DisplayRealType type)
            throws VisADException, RemoteException {
        setZValue(value);
    }



    /**
     * Calculate the midpoint position from the endpoints
     * @return  mid point position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private RealTuple calculateMidpoint()
            throws VisADException, RemoteException {
        return (RealTuple) startPoint.add(endPoint).divide(TWO);
    }


    /**
     * set whether the start point can be moved or not by user; true=yes
     * @deprecated Should use setStartPointFixed
     *
     * @param value  true to be fixed
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setStartPtFixed(boolean value)
            throws VisADException, RemoteException {
        startSp.setManipulable(value);
    }

    /**
     * set whether the start point can be moved or not by user; true=yes
     *
     * @param value  true to be fixed
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setStartPointFixed(boolean value)
            throws VisADException, RemoteException {
        startSp.setManipulable(value);
    }

    /**
     * set whether the end point can be moved or not by user; true=yes
     *
     * @param value  true to be fixed
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setEndPointFixed(boolean value)
            throws VisADException, RemoteException {
        endSp.setManipulable(value);
    }


    /**
     * Set whether the start point is visible; true=yes
     *
     * @param value  true to be fixed
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setStartPointVisible(boolean value)
            throws VisADException, RemoteException {
        startSp.setVisible(value);
    }

    /**
     * Set whether the end point is visible; true=yes
     *
     * @param value  true for visible
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setEndPointVisible(boolean value)
            throws VisADException, RemoteException {
        endSp.setVisible(value);
    }

    /**
     * Set whether the mid point is visible; true=yes
     *
     * @param value  true for visible
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setMidPointVisible(boolean value)
            throws VisADException, RemoteException {
        midSp.setVisible(value);
    }


    /**
     * Set the position of the starting point of the line.
     *
     * @param point  XY position as a RealTuple
     *
     * @throws VisADException  bad point
     * @throws RemoteException Java RMI error
     */
    public void setStartPoint(RealTuple point)
            throws VisADException, RemoteException {
        setPoint(POINT_START, point);
    }

    /**
     * Get the position of the starting point of the line
     * @return  XY position as a RealTuple
     */
    public RealTuple getStartPoint() {
        return startPoint;
    }

    /**
     * Set the position of the ending point of the line;
     * using current start and end points (ie "does nothing" -for persistence)
     *
     * @param point  XY position as a RealTuple
     *
     * @throws VisADException  bad point
     * @throws RemoteException Java RMI error
     */
    public void setMidPoint(RealTuple point)
            throws VisADException, RemoteException {
        startPoint = startSp.getPoint();
        endPoint   = endSp.getPoint();
        midPoint   = calculateMidpoint();
        midSp.setPoint(midPoint);
    }


    /**
     * Get the position of the middle point of the line.
     *
     * @return  XY position as a RealTuple
     */
    public RealTuple getMidPoint() {
        return midSp.getPoint();
    }

    /**
     * Set the position of the ending point of the line.
     *
     * @param point  XY position as a RealTuple
     *
     * @throws VisADException  bad point
     * @throws RemoteException Java RMI error
     */
    public void setEndPoint(RealTuple point)
            throws VisADException, RemoteException {
        setPoint(POINT_END, point);
    }

    /**
     * Get the position of the ending point of the line.
     *
     * @return  XY position as a RealTuple
     */
    public RealTuple getEndPoint() {
        return endPoint;
    }

    /**
     * Set the position of one end point of the line.
     * The mid point is automatically fit between them.
     *
     * @param which either CrossSectionSelector.POINT_START or POINT_END
     * @param point point position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   bad point
     */
    public void setPoint(int which, RealTuple point)
            throws VisADException, RemoteException {
        if (which == POINT_START) {
            startSp.setPoint(point);
        } else if (which == POINT_END) {
            endSp.setPoint(point);
        }
        moveEndpoint(which);
    }

    /**
     * Set the position of the ending point of the line.
     *
     * @param position an array of size 2 that holds the start and end points
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   bad point
     */
    public void setPosition(RealTuple[] position)
            throws VisADException, RemoteException {
        setPosition(position[0], position[1]);
    }

    /**
     * Get the position of the ending point of the line.
     *
     * @return an array of size 2 that holds the start and end points
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   bad point
     */
    public RealTuple[] getPosition() throws VisADException, RemoteException {
        return new RealTuple[] { getStartPoint(), getEndPoint() };
    }



    /**
     * Set the position of the ending point of the line.
     *
     * @param startPosition   starting position for the selector line
     * @param endPosition     ending position for the selector line
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   bad point
     */
    public void setPosition(RealTuple startPosition, RealTuple endPosition)
            throws VisADException, RemoteException {
        startSp.setPoint(startPosition);
        endSp.setPoint(endPosition);
        moveEndpoint(POINT_MID);
    }

    /**
     * Create a selector point for this cross section selector
     *
     * @param which     which point to create (start, end, middle)
     * @param name      name of the point
     * @param initialPoint  initial location
     * @return
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private SelectorPoint makeSelectorPoint(final int which, String name,
                                            RealTuple initialPoint)
            throws VisADException, RemoteException {
        VisADGeometryArray marker;
        if (which == POINT_START) {
            marker = ShapeUtility.makeShape(ShapeUtility.PLUS);
            marker = ShapeUtility.setSize(marker, .03f);
        } else if (which == POINT_END) {
            marker = SelectorPoint.reduce(
                ShapeUtility.makeShape(ShapeUtility.CUBE));
        } else {
            marker = ShapeUtility.makeShape(ShapeUtility.FILLED_TRIANGLE);
            marker = ShapeUtility.setSize(marker, .03f);
        }

        SelectorPoint point = new SelectorPoint(name, marker, initialPoint);
        if (which == POINT_START) {
            point.addConstantMaps(new ConstantMap[] {
                new ConstantMap(2.0f, Display.LineWidth) });

        }
        point.setManipulable(true);
        point.setPointSize(getPointSize());
        addDisplayable(point);
        point.addAction(new ActionImpl(name + " Point Selector Listener") {
            public void doAction() throws VisADException, RemoteException {
                if (amMoving) {
                    return;
                }
                amMoving = true;

                try {
                    if (which == POINT_MID) {
                        moveMidpoint();
                    } else {
                        moveEndpoint(which);
                    }
                } catch (Exception exc) {
                    System.err.println("Error moving points: " + exc);
                }
                amMoving = false;
            }
        });
        return point;
    }

    /**
     * Called when the endpoint moves
     *
     *
     * @param which Which end point
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private synchronized void moveEndpoint(int which)
            throws VisADException, RemoteException {
        if ( !beenInitialized) {
            return;
        }
        if ((startPoint.equals(startSp.getPoint())
                && endPoint.equals(endSp.getPoint()))) {
            return;
        }
        boolean wasMoving = amMoving;
        amMoving   = true;

        startPoint = startSp.getPoint();
        endPoint   = endSp.getPoint();
        midPoint   = calculateMidpoint();
        midSp.setPoint(midPoint);
        positionHasChanged();
        firePropertyChange(((which == POINT_START)
                            ? PROPERTY_STARTPOINT
                            : PROPERTY_ENDPOINT), null, null);
        amMoving = wasMoving;
    }

    // move the end points and line when the user moves the mid point;
    // keeping line length orientation preserved (no rotation on screen)

    /**
     * Called when the midpoint moves
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private synchronized void moveMidpoint()
            throws VisADException, RemoteException {
        if ( !beenInitialized) {
            return;
        }
        if ((midPoint.equals(midSp.getPoint()))) {
            return;
        }

        RealTuple delta = (RealTuple) midSp.getPoint().subtract(midPoint);
        if (delta.equals(ZERO2D) || delta.equals(ZERO3D)) {
            return;
        }

        startPoint = (RealTuple) startSp.getPoint().add(delta);
        endPoint   = (RealTuple) endSp.getPoint().add(delta);
        midPoint   = midSp.getPoint();

        boolean wasMoving = amMoving;
        amMoving = true;

        startSp.setPoint(startPoint);
        endSp.setPoint(endPoint);
        positionHasChanged();
        firePropertyChange(PROPERTY_MIDPOINT, null, null);

        amMoving = wasMoving;
    }

    /**
     * Called when the position has changed
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    protected void positionHasChanged()
            throws VisADException, RemoteException {
        setLineData();
        notifyListenersOfMove();
    }


    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws VisADException, RemoteException {
        return new CrossSectionSelector(this);
    }

    /**
     * Adds a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     */
    public void addStartPropertyChangeListener(
            PropertyChangeListener action) {
        super.addPropertyChangeListener(PROPERTY_STARTPOINT, action);
    }

    /**
     * Removes a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     */
    public void removeStartPropertyChangeListener(
            PropertyChangeListener action) {
        super.removePropertyChangeListener(PROPERTY_STARTPOINT, action);
    }


    /**
     * Adds a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     */
    public void addEndPropertyChangeListener(PropertyChangeListener action) {
        super.addPropertyChangeListener(PROPERTY_ENDPOINT, action);
    }

    /**
     * Removes a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     */
    public void removeEndPropertyChangeListener(
            PropertyChangeListener action) {
        super.removePropertyChangeListener(PROPERTY_ENDPOINT, action);
    }



    /**
     * Adds a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     */
    public void addMidPropertyChangeListener(PropertyChangeListener action) {
        super.addPropertyChangeListener(PROPERTY_MIDPOINT, action);
    }

    /**
     * Removes a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     */
    public void removeMidPropertyChangeListener(
            PropertyChangeListener action) {
        super.removePropertyChangeListener(PROPERTY_MIDPOINT, action);
    }


    /**
     * SHould we interpolate the line points
     *
     * @param value interpolate
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public void setInterpolateLinePoints(boolean value)
            throws VisADException, RemoteException {
        interpolateLinePoints = value;
        if (line != null) {
            setLineData();
        }
    }


}

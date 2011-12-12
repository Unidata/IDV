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


import ucar.visad.ShapeUtility;
import ucar.visad.Util;


import visad.*;

import java.rmi.RemoteException;

import java.util.List;


/**
 * Displayable to encompass a manipulable point that can be moved
 * around the display.  If you want to limit the movement to one
 * axis, then construct the RealTuple with one element whose RealType
 * corresponds to that axis.
 *
 * @author Don Murray
 * @version $Revision: 1.26 $
 */
public class SelectorPoint extends LineDrawing {

    /** the point's position */
    private RealTuple thePoint;

    /** marker used for point */
    private VisADGeometryArray marker;

    /** shape ScalarMap */
    private ScalarMap shapeMap = null;

    /** type for ScalarMap */
    private RealType shapeType = null;

    /** control for ScalarMap */
    private ShapeControl shapeControl = null;

    /** static instance counter */
    private static int count = 0;

    /** number of components in position */
    private int dimension;

    /** value for the marker */
    private Real markerValue;

    /** flag for autosizing */
    private boolean autoSize;

    /** size of the point */
    private float pointSize = 1.0f;

    /** Any axis fixed */
    private boolean[] fixed = { false, false, false };



    /**
     * Construct a SelectorPoint for the pointType specified.  Initial
     * value is set to 0.
     *
     * @param   name         name of this SelectorPoint
     * @param   pointType    RealType that the point should map to
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public SelectorPoint(String name, RealTupleType pointType)
            throws VisADException, RemoteException {
        this(name, new RealTuple(pointType));
    }

    /**
     * Construct a SelectorPoint for the pointType specified with the
     * specified intial value.
     *
     * @param name            name of this SelectorPoint
     * @param initialValue    RealTuple position for point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public SelectorPoint(String name, RealTuple initialValue)
            throws VisADException, RemoteException {
        this(name, reduce(ShapeUtility.createShape(ShapeUtility.CUBE)[0]),
             initialValue);
    }

    /**
     * Construct a SelectorPoint.
     *
     * @param name             name of this SelectorPoint
     * @param markerText       text for the marker
     * @param initialValue     RealTuple position for point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public SelectorPoint(String name, String markerText,
                         RealTuple initialValue)
            throws VisADException, RemoteException {
        this(name, ShapeUtility.shapeText(markerText), initialValue);
    }

    /**
     * Construct a SelectorPoint for the pointType specified with the
     *
     * @param name           name of this SelectorPoint
     * @param marker         Shape to use for maker
     * @param initialValue   RealTuple position for point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public SelectorPoint(String name, VisADGeometryArray marker,
                         RealTuple initialValue)
            throws VisADException, RemoteException {

        super(name);
        setManipulable(true);
        thePoint    = initialValue;
        dimension   = thePoint.getDimension();
        this.marker = (marker == null)
                      ? reduce(ShapeUtility.createShape(ShapeUtility.CUBE)[0])
                      : marker;
        setupShapeMap();
        markerValue = new Real(shapeType, 0);
        mySetData(thePoint, null);

    }


    /**
     * _more_
     *
     * @param aniType _more_
     * @param force _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public Set getAnimationSet(RealType aniType, boolean force)
            throws VisADException, RemoteException {
        return null;
    }

    /**
     * Set if any of the axis movements are fixed
     *
     * @param x x fixed
     * @param y y fixed
     * @param z z fixed
     */
    public void setFixed(boolean x, boolean y, boolean z) {
        fixed[0] = x;
        fixed[1] = y;
        fixed[2] = z;
    }




    /**
     * Set the point.
     *
     * @param  value    new value for this point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public void setPoint(RealTuple value)
            throws VisADException, RemoteException {
        setPointWithTime(value, null);
    }

    /**
     * _more_
     *
     * @param value _more_
     * @param marker _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setPoint(RealTuple value, VisADGeometryArray marker)
            throws VisADException, RemoteException {
        this.marker = marker;
        if (shapeControl != null) {
            shapeControl.setShape(0, marker);
        }
        setPointWithTime(value, null);
    }


    /**
     * _more_
     *
     * @param value _more_
     * @param times _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setPointWithTime(RealTuple value, List times)
            throws VisADException, RemoteException {
        if ( !(this.thePoint.getType().equals(value.getType()))) {
            throw new VisADException("Invalid type for value: mytype="
                                     + this.thePoint.getType() + " new type="
                                     + value.getType());
        }
        thePoint = value;
        mySetData(value, times);
    }

    /**
     * Construct a SelectorPoint from another instance
     *
     * @param   that         other instance
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public SelectorPoint(SelectorPoint that)
            throws VisADException, RemoteException {

        super(that);
        thePoint = that.getPoint();
        setData(that.getPoint());
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
        return new SelectorPoint(this);
    }

    /**
     * Get the current point.
     *
     * @return  the current value for this point
     */
    public RealTuple getPoint() {
        return thePoint;
    }

    /**
     * Set the marker text for this selector point
     *
     * @param markerText  VisADGeometryArray for marker
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setMarker(String markerText)
            throws VisADException, RemoteException {
        setMarker(ShapeUtility.shapeText(markerText));
    }

    /**
     * Set the marker for this selector point
     *
     * @param marker  VisADGeometryArray for marker
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setMarker(VisADGeometryArray marker)
            throws VisADException, RemoteException {
        if (shapeControl != null) {
            shapeControl.setShape(0, marker);
        }
        this.marker = marker;
    }

    /**
     * Set the size (scale) of the ShapeControl.  Usually done to set
     * the initial size.
     * @param newSize  size to use.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setPointSize(float newSize)
            throws VisADException, RemoteException {
        if ((shapeControl != null) && (newSize != pointSize)) {
            shapeControl.setScale(newSize);
        }
        pointSize = newSize;
    }


    /**
     * _more_
     *
     * @param newScale _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setScale(float newScale)
            throws VisADException, RemoteException {
        if (shapeControl != null) {
            //We set auto scale false here so the shape control 
            //clears out its controllistener which was keeping around the old initial scale
            shapeControl.setAutoScale(false);
            shapeControl.setScale(newScale);
            shapeControl.setAutoScale(true);
        }
    }


    /**
     * Get the scale of the ShapeControl.
     * @return current scale;
     */
    public float getScale() {
        return (shapeControl != null)
               ? shapeControl.getScale()
               : pointSize;
    }

    /**
     * Get the marker for this selector point
     *
     * @return VisADGeometryArray for marker
     */
    public VisADGeometryArray getMarker() {
        return marker;
    }

    /**
     * Set whether the marker should automatically resize as the
     * display is zoomed.
     * @param yesorno  true to automatically resize the marker.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setAutoSize(boolean yesorno)
            throws VisADException, RemoteException {
        autoSize = yesorno;
        if (shapeControl != null) {
            shapeControl.setAutoScale(autoSize);
        }
    }

    /**
     * Get whether the marker is automatically resized as the
     * display is zoomed.
     *
     * @return true if marker is automatically resized.
     */
    public boolean getAutoSize() {
        return autoSize;
    }

    /**
     * Called when the data changes.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    protected void dataChange() throws VisADException, RemoteException {
        RealTuple data;
        Data      currentData = getData();
        if (currentData instanceof RealTuple) {
            data = (RealTuple) currentData;
        } else {
            //This is where we have a time field
            if (true) {
                return;
            }
            FieldImpl timeField = (FieldImpl) currentData;
            data = (RealTuple) timeField.getSample(0);
        }

        Real[]    pointVals    = new Real[dimension];
        double[]  oldValues    = null;
        RealTuple currentPoint = thePoint;
        if ((currentPoint != null) && !currentPoint.isMissing()) {
            oldValues = currentPoint.getValues();
        }

        boolean didChange = false;
        for (int i = 0; i < dimension; i++) {
            Real v = (Real) data.getComponent(i);
            if ((v != null) && (oldValues != null) && (fixed != null)
                    && (i < fixed.length)) {
                if (fixed[i]) {
                    if (v.getValue() != oldValues[i]) {
                        didChange = true;
                        v         = v.cloneButValue(oldValues[i]);
                    }
                }
            }
            pointVals[i] = v;
        }
        thePoint = new RealTuple(pointVals);
        if (didChange) {
            setPoint(thePoint);
        }
        super.dataChange();
    }


    /**
     * Set the shapes in the control
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setShapesInControl() throws VisADException, RemoteException {
        if ((shapeControl != null) && (marker != null)) {
            shapeControl.setShapeSet(
            //new Gridded1DSet(shapeType, new float[][] {{0.0f}}, 1));
            new Integer1DSet(shapeType, 1));
            shapeControl.setShapes(new VisADGeometryArray[] { marker });
        }
    }

    /**
     * Called from setData
     *
     * @param value  position value
     * @param times _more_
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void mySetData(RealTuple value, List times)
            throws VisADException, RemoteException {
        int    size = value.getDimension();
        Real[] data = new Real[size + 1];
        for (int i = 0; i < value.getDimension(); i++) {
            data[i] = (Real) value.getComponent(i);
        }
        data[size] = markerValue;
        setData(Util.makeTimeRangeField(new RealTuple(data), times));
        //setData(new Tuple( new Data [] {value, new Real(shapeType,0)}));
    }

    /**
     * Reduce the size of the marker
     *
     * @param marker  marker to reduce
     * @return  reduced marker
     */
    public static VisADGeometryArray reduce(VisADGeometryArray marker) {
        return ShapeUtility.setSize(marker, .02f);
    }


    /**
     * Make the ScalarMaps for this selector
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setupShapeMap() throws VisADException, RemoteException {

        shapeType = RealType.getRealType("Selector_Point_" + count++);
        shapeMap  = new ScalarMap(shapeType, Display.Shape);
        shapeMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    shapeControl = (ShapeControl) shapeMap.getControl();

                    if (shapeControl != null) {
                        setShapesInControl();
                        shapeControl.setScale(pointSize);
                        shapeControl.setAutoScale(autoSize);
                    }

                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {}
        });
        addScalarMap(shapeMap);
    }
}

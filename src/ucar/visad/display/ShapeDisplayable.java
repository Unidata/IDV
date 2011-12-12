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


import visad.*;

import java.rmi.RemoteException;


/**
 * Displayable to encompass a manipulable point that can be moved
 * around the display.  If you want to limit the movement to one
 * axis, then construct the RealTuple with one element whose RealType
 * corresponds to that axis.
 *
 * @author Don Murray
 * @version $Revision: 1.7 $
 */
public class ShapeDisplayable extends LineDrawing {

    /** the point's position */
    private RealTuple thePoint;

    /** marker used for point */
    private VisADGeometryArray marker;

    /** marker used for point */
    private VisADGeometryArray[] markers;

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

    /**
     * Construct a ShapeDisplayable for the pointType specified.  Initial
     * value is set to 0.
     *
     * @param   name         name of this ShapeDisplayable
     * @param marker The marker to use
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public ShapeDisplayable(String name, VisADGeometryArray marker)
            throws VisADException, RemoteException {
        this(name, marker,
             new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                           new double[] { 0,
                                          0, 0 }));
    }


    /**
     * Construct a ShapeDisplayable for the pointType specified.  Initial
     * value is set to 0.
     *
     * @param   name         name of this ShapeDisplayable
     * @param   pointType    RealType that the point should map to
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public ShapeDisplayable(String name, RealTupleType pointType)
            throws VisADException, RemoteException {
        this(name, new RealTuple(pointType));
    }

    /**
     * Construct a ShapeDisplayable for the pointType specified with the
     * specified intial value.
     *
     * @param name            name of this ShapeDisplayable
     * @param initialValue    RealTuple position for point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public ShapeDisplayable(String name, RealTuple initialValue)
            throws VisADException, RemoteException {
        this(name, reduce(ShapeUtility.makeShape(ShapeUtility.CUBE)),
             initialValue);
    }

    /**
     * Construct a ShapeDisplayable.
     *
     * @param name             name of this ShapeDisplayable
     * @param markerText       text for the marker
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public ShapeDisplayable(String name, String markerText)
            throws VisADException, RemoteException {
        this(name, ShapeUtility.shapeText(markerText));
    }

    /**
     * Construct a ShapeDisplayable.
     *
     * @param name             name of this ShapeDisplayable
     * @param markerText       text for the marker
     * @param initialValue     RealTuple position for point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public ShapeDisplayable(String name, String markerText,
                            RealTuple initialValue)
            throws VisADException, RemoteException {
        this(name, ShapeUtility.shapeText(markerText), initialValue);
    }




    /**
     * Construct a ShapeDisplayable for the pointType specified with the
     *
     * @param name           name of this ShapeDisplayable
     * @param marker         Shape to use for maker
     * @param initialValue   RealTuple position for point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public ShapeDisplayable(String name, VisADGeometryArray marker,
                            RealTuple initialValue)
            throws VisADException, RemoteException {

        super(name);
        thePoint    = initialValue;
        dimension   = thePoint.getDimension();
        this.marker = (marker == null)
                      ? reduce(ShapeUtility.makeShape(ShapeUtility.CUBE))
                      : marker;
        setupShapeMap();
        markerValue = new Real(shapeType, 0);
        mySetData(thePoint);
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
        if ( !(this.thePoint.getType().equals(value.getType()))) {
            throw new VisADException("Invalid type for value");
        }
        thePoint = value;
        mySetData(value);
    }

    /**
     * Construct a ShapeDisplayable from another instance
     *
     * @param   that         other instance
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public ShapeDisplayable(ShapeDisplayable that)
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
        return new ShapeDisplayable(this);
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
     * @param x X location
     * @param y Y location
     * @param z Z location
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setMarker(VisADGeometryArray marker, double x, double y,
                          double z)
            throws VisADException, RemoteException {
        RealTuple loc = new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                                      new double[] { x,
                y, z });

        setMarker(marker);
        setPoint(loc);
    }


    /**
     * Set the location
     *
     * @param x x
     * @param y y
     * @param z z
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setPoint(double x, double y, double z)
            throws VisADException, RemoteException {
        RealTuple loc = new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                                      new double[] { x,
                y, z });

        setPoint(loc);
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
     * Set the marker
     *
     * @param marker  the marker as a geometry array
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    public void setMarker(VisADGeometryArray[] marker)
            throws VisADException, RemoteException {
        if (shapeControl != null) {
            shapeControl.setShapes(marker);
        }
        this.markers = marker;
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
        if (shapeControl != null) {
            shapeControl.setAutoScale(autoSize);
        }
        autoSize = yesorno;
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

        RealTuple data      = (RealTuple) getData();
        Real[]    pointVals = new Real[dimension];
        for (int i = 0; i < dimension; i++) {
            pointVals[i] = (Real) data.getComponent(i);
        }
        thePoint = new RealTuple(pointVals);
        super.dataChange();

        //System.out.println(point);
    }


    /**
     * Set the shapes in the control
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setShapesInControl() throws VisADException, RemoteException {
        if (shapeControl != null) {
            shapeControl.setShapeSet(
            //new Gridded1DSet(shapeType, new float[][] {{0.0f}}, 1));
            new Integer1DSet(shapeType, 1));
            if (marker != null) {
                shapeControl.setShapeSet(
                //new Gridded1DSet(shapeType, new float[][] {{0.0f}}, 1));
                new Integer1DSet(shapeType, 1));
                shapeControl.setShapes(new VisADGeometryArray[] { marker });
            } else if (markers != null) {
                shapeControl.setShapeSet(
                //new Gridded1DSet(shapeType, new float[][] {{0.0f}}, 1));
                new Integer1DSet(shapeType, markers.length));
                shapeControl.setShapes(markers);
            }
        }
    }

    /**
     * Called from setData
     *
     * @param value  position value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void mySetData(RealTuple value)
            throws VisADException, RemoteException {
        int    size = value.getDimension();
        Real[] data = new Real[size + 1];
        for (int i = 0; i < value.getDimension(); i++) {
            data[i] = (Real) value.getComponent(i);
        }
        data[size] = markerValue;
        setData(new RealTuple(data));
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

        shapeType = RealType.getRealType("ShapeDisplayable_" + count++);
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


    /**
     * Get the shape type
     *
     * @return The shape type
     */
    public RealType getShapeType() {
        return shapeType;
    }

}

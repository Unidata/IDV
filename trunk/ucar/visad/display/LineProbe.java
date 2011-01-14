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


/**
 * Class for a probe.
 *
 * @author Don Murray
 * @version $Revision: 1.20 $
 */
public class LineProbe extends SelectorDisplayable {

    /** the selector point */
    private SelectorPoint point;

    /** the line */
    private ProfileLine line;

    /** constant Z value */
    private Real constant;

    /** initial point size */
    private float pointSize = 5.0f;

    /** flag for whether we're in the process of setting the position */
    private volatile boolean settingPosition = false;

    /** fixed end point */
    private RealTuple fixedEndPoint;


    /**
     * Default Constructor
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public LineProbe() throws VisADException, RemoteException {
        this(new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                           new double[] { 0,
                                          0 }));
    }

    /**
     * Construct a new LineProbe using the x, y, and z positions
     *
     * @param x   x position
     * @param y   y position
     * @param z   z position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public LineProbe(double x, double y, double z)
            throws VisADException, RemoteException {

        this(new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                           new double[] { x,
                                          y }), new Real(RealType.ZAxis, z));
    }

    /**
     * Construct a new LineProbe at the position
     *
     * @param position  x/y position for the probe
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public LineProbe(RealTuple position)
            throws VisADException, RemoteException {
        this(position, new Real(RealType.ZAxis, 1));
    }

    /**
     * Construct a new LineProbe at the position using a constant
     * Z value
     *
     * @param position  x/y position for the probe
     * @param constant  z position of the selector point
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public LineProbe(RealTuple position, Real constant)
            throws VisADException, RemoteException {
        this(position, null, constant);
    }

    /**
     * Create a new line probe
     *
     * @param position  the starting position
     * @param fixedEndPoint  a fixed end point
     * @param constant  constant value
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    public LineProbe(RealTuple position, RealTuple fixedEndPoint,
                     Real constant)
            throws VisADException, RemoteException {

        /*
        if ( !position.getType().equals(
                RealTupleType.SpatialCartesian2DTuple)) {
            throw new VisADException("Can't yet handle "
                                     + position.getType());
        }
        */
        if ((constant != null)
                && !constant.getType().equals(RealType.ZAxis)) {
            throw new VisADException("Can't yet handle constant "
                                     + constant.getType());
        }

        this.fixedEndPoint = fixedEndPoint;
        this.constant      = constant;
        line               = new ProfileLine("ProbeLine");
        point              = new SelectorPoint("Probe point", position);

        if (constant != null) {
            point.addConstantMap(new ConstantMap(constant.getValue(),
                    Display.ZAxis));
        }
        addDisplayable(point);
        addDisplayable(line);
        setPosition(position);
        //When the point moves the also move the line.
        point.addAction(new ActionImpl("point listener") {
            public void doAction() throws VisADException, RemoteException {
                if (settingPosition) {
                    return;
                }
                setLinePosition();
                notifyListenersOfMove();
            }
        });
    }

    /**
     * set if linprobe can be moved by the user with mouse cursor or not
     *
     * @param manip    boolean true if can move
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setManipulable(boolean manip)
            throws VisADException, RemoteException {
        line.setManipulable(manip);
        point.setManipulable(manip);
    }

    /**
     * Get the position of the SelectorPoint.
     *
     * @return position in XY space
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public RealTuple getPosition() throws VisADException, RemoteException {
        return point.getPoint();
    }

    /**
     * Get the selector point
     *
     * @return  the point
     */
    public SelectorPoint getSelectorPoint() {
        return point;
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
        super.setAutoSize(yesorno);
    }

    /**
     * Sets the position of the probe.  This method fires a {@link
     * PropertyChangeEvent} for {@link #PROPERTY_POSITION}.<p>
     *
     * This implementation uses {@link #setPosition(RealTuple)} to actually
     * set the position of the probe.</p>
     *
     * @param x                     The X-axis position for the probe.
     * @param y                     The Y-axis position for the probe.
     *
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void setPosition(double x, double y)
            throws VisADException, RemoteException {

        setPosition(new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                                  new double[] { x,
                y }));
    }

    /**
     * Sets the position of the probe.  This method fires a {@link
     * java.beans.PropertyChangeEvent} for {@link #PROPERTY_POSITION}.
     *
     * @param position              The position of the probe.
     *
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
            /*
            if ( !position.getType().equals(
                    RealTupleType.SpatialCartesian2DTuple)) {
                throw new VisADException("Can't yet handle "
                                         + position.getType());
            }
            */

            if ((constant != null)
                    && !constant.getType().equals(RealType.ZAxis)) {
                throw new VisADException("Can't yet constant "
                                         + constant.getType());
            }

            point.setPoint(position);
            setLinePosition();
        } finally {
            settingPosition = false;
        }
    }

    /**
     * Set the line position.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setLinePosition() throws VisADException, RemoteException {

        float[][] lineVals = new float[3][2];
        Real[]    reals    = point.getPoint().getRealComponents();

        lineVals[0][0] = (float) reals[0].getValue();
        lineVals[1][0] = (float) reals[1].getValue();
        if ((reals.length == 2) && (constant != null)) {
            lineVals[2][0] = (float) constant.getValue();
        } else if (reals.length == 3) {
            lineVals[2][0] = (float) reals[2].getValue();
        }
        if (fixedEndPoint != null) {
            Real[] pt = fixedEndPoint.getRealComponents();
            lineVals[0][1] = (float) pt[0].getValue();
            lineVals[1][1] = (float) pt[1].getValue();
            lineVals[2][1] = (float) pt[2].getValue();
        } else {
            lineVals[0][1] = lineVals[0][0];
            lineVals[1][1] = lineVals[1][0];
            lineVals[2][1] = -1;
        }

        Gridded3DSet lineData =
            new Gridded3DSet(RealTupleType.SpatialCartesian3DTuple, lineVals,
                             2);

        if ( !lineData.equals(line.getData())) {
            line.setData(lineData);
        }
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
        point.setMarker(markerText);
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
        point.setMarker(marker);
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
        point.setPointSize(newSize);
    }

    /**
     * Get the point's scale
     *
     * @return  the scale of the point
     */
    public float getPointScale() {
        if (point != null) {
            return point.getScale();
        }
        return 1.0f;
    }

}

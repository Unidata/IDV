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

import java.rmi.RemoteException;


/**
 * Displayable to encompass a manipulable point that can be moved
 * around the display.  If you want to limit the movement to one
 * axis, then construct the RealTuple with one element whose RealType
 * corresponds to that axis.
 *
 * @author Don Murray
 * @version $Revision: 1.6 $
 */
public class IndicatorPoint extends LineDrawing {

    /** location of the point */
    private RealTuple point;

    /**
     * Construct a IndicatorPoint for the pointType specified.  Initial
     * value is set to 0.
     *
     * @param   name         name of this IndicatorPoint
     * @param   pointType    RealType that the point should map to
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public IndicatorPoint(String name, RealTupleType pointType)
            throws VisADException, RemoteException {
        this(name, new RealTuple(pointType));
    }

    /**
     * Construct a IndicatorPoint for the pointType specified with the
     *
     * @param name           name of this IndicatorPoint
     * @param initialValue   initial point
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public IndicatorPoint(String name, RealTuple initialValue)
            throws VisADException, RemoteException {

        super(name);

        setPointSize(3.0f);

        point = initialValue;

        setData(point);
    }

    /**
     * Set the point
     * @param  value    new value for this point
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public void setPoint(RealTuple value)
            throws VisADException, RemoteException {

        // Comment this out.  Do we really need it?
        //if (!(this.point.getType().equals(value.getType()))) {
        //    throw new VisADException("Invalid type for value");
        //}

        setData(value);
    }

    /**
     * Construct a IndicatorPoint from another instance
     *
     * @param   that         other instance
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    public IndicatorPoint(IndicatorPoint that)
            throws VisADException, RemoteException {

        super(that);

        setData(that.getPoint());
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
        return new IndicatorPoint(this);
    }

    /**
     * Get the current point.
     *
     * @return  the current value for this point
     */
    public RealTuple getPoint() {
        return point;
    }

    /**
     * Called when the data changes.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    protected void dataChange() throws VisADException, RemoteException {
        point = (RealTuple) getData();
        super.dataChange();
    }
}

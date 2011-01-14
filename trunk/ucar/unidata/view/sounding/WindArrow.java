/*
 * $Id: WindArrow.java,v 1.21 2005/05/13 18:33:40 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.rmi.RemoteException;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;
import visad.bom.*;


/**
 * Supports the display a horizontal wind as a wind-direction shaft with barbs.
 *
 * The VisAD MathType of the horizontal wind is the TupleType
 * (GeopotentialAltitude, (WesterlyWind, SoutherlyWind)).
 *
 * @author Steven R. Emmerson
 * @version $Id: WindArrow.java,v 1.21 2005/05/13 18:33:40 jeffmc Exp $
 */
public class WindArrow extends LineDrawing {

    /**
     * The name of the wind property.
     */
    public static String WIND = "wind";

    /** default type for wind */
    private static TupleType defaultTupleType;

    /** actual type for wind */
    private TupleType tupleType;

    static {
        try {
            defaultTupleType = new TupleType(new MathType[]{
                GeopotentialAltitude.getRealType(),
                CartesianHorizontalWind.getRealTupleType() });
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Returns the default type of the wind tuple.
     * @return                  The default type of the wind tuple.
     */
    public static TupleType getDefaultTupleType() {
        return defaultTupleType;
    }

    /**
     * Returns the default type of the geopotential altitude.
     * @return                  The default type of the geopotential altitude.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getGeopotentialAltitudeDefaultRealType()
            throws VisADException {
        return (RealType) getDefaultTupleType().getComponent(0);
    }

    /**
     * Returns the type of the vertical component of the profile quantity.
     * @return                  The type of the vertical quantity.
     * @throws VisADException   VisAD failure.
     */
    public RealType getVerticalComponentRealType() throws VisADException {
        return (RealType) tupleType.getComponent(0);
    }

    /**
     * Returns the default type of the horizontal wind.
     * @return                  The default type of the horizontal wind.
     * @throws VisADException   VisAD failure.
     */
    public static RealTupleType getHorizontalWindDefaultRealTupleType()
            throws VisADException {
        return (RealTupleType) getDefaultTupleType().getComponent(1);
    }

    /**
     * Returns the default type of the westerly wind.
     * @return                  The default type of the westerly wind.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getWesterlyWindDefaultRealType()
            throws VisADException {
        return (RealType) getHorizontalWindDefaultRealTupleType()
            .getComponent(0);
    }

    /**
     * Returns the default type of the southerly wind.
     * @return                  The default type of the southerly wind.
     * @throws VisADException   VisAD failure.
     */
    public static RealType getSoutherlyWindDefaultRealType()
            throws VisADException {
        return (RealType) getHorizontalWindDefaultRealTupleType()
            .getComponent(1);
    }

    /**
     * Constructs from nothing.  The TupleType will be the default.  The data
     * will be manipulable.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindArrow() throws VisADException, RemoteException {
        this(defaultTupleType, true);
    }

    /**
     * Constructs from the TupleType for the data.  The data will be
     * manipulable.
     * @param tupleType         The MathType for the data.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindArrow(TupleType tupleType)
            throws VisADException, RemoteException {

        this(tupleType, false);
    }

    /**
     * Constructs from the TupleType for the data.  The data will be
     * manipulable depending on the value of manip.
     * @param tupleType         The MathType for the data.
     * @param manip             if true, will be manipulable
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindArrow(TupleType tupleType, boolean manip)
            throws VisADException, RemoteException {

        super("WindArrow");

        this.tupleType = tupleType;

        setManipulable(manip);
        setData(tupleType.missingData());
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected WindArrow(WindArrow that)
            throws VisADException, RemoteException {

        super(that);

        tupleType = that.tupleType;  // immutable
    }

    /**
     * Returns the MathType of the wind tuple.
     * @return                  The MathType of the wind tuple.
     */
    public TupleType getTupleType() {
        return tupleType;
    }

    /**
     * Sets the wind.
     * @param wind              The horizontal wind
     *                          (geopotentialAltitude, (u, v)).
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWind(Tuple wind) throws VisADException, RemoteException {
        setData(Util.ensureMathType(wind, getTupleType()));
    }

    /**
     * Sets the wind.
     * @param windRef           The data reference for the horizontal wind
     *                          (geopotentialAltitude, (u, v)).
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWind(DataReference windRef)
            throws VisADException, RemoteException {
        Util.vetType(getTupleType(), windRef.getData());
        setDataReference(windRef);
    }

    /**
     * Returns the wind.
     * @return                  The horizontal wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Tuple getWind() throws VisADException, RemoteException {
        return (Tuple) getData();
    }

    /**
     * Handles a change to the Data referenced by this displayable's
     * DataReference.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void dataChange() throws VisADException, RemoteException {
        firePropertyChange(WIND, null, getWind());
    }

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof WindArrow)) {
            equals = false;
        } else {
            try {
                WindArrow that     = (WindArrow) obj;
                Tuple     thisWind = getWind();
                Tuple     thatWind = that.getWind();

                equals = (this == that)
                         || (tupleType.equals(that.tupleType)
                             && ((thisWind == null)
                                 ? thatWind == null
                                 : thisWind.equals(thatWind)) && super.equals(
                                     that));
            } catch (Exception e) {
                System.err.println(getClass().getName() + ".equals(Object): "
                                   + "Couldn't get wind data: " + e);

                equals = false;
            }
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {

        int code;

        try {
            Tuple wind = getWind();

            code = tupleType.hashCode() ^ ((wind == null)
                                           ? 0
                                           : wind.hashCode()) ^ super
                                               .hashCode();
        } catch (Exception e) {
            System.err.println(getClass().getName() + ".hashCode(): "
                               + "Couldn't get wind data: " + e);

            code = 0;
        }

        return code;
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws VisADException, RemoteException {
        return new WindArrow(this);
    }

    /**
     * Returns the DataRenderer for this displayable.  This method does not
     * verify that the VisAD display has been set.
     * @return                  The DataRenderer associated with this
     *                          displayable.
     */
    protected DataRenderer getDataRenderer() {

        LocalDisplay display = getDisplay();

        return isManipulable()
               ? (display instanceof visad.java2d.DisplayImplJ2D)
                 ? (DataRenderer) new BarbManipulationRendererJ2D()
                 : (DataRenderer) new BarbManipulationRendererJ3D()
               : (display instanceof visad.java2d.DisplayImplJ2D)
                 ? (DataRenderer) new BarbRendererJ2D()
                 : (DataRenderer) new BarbRendererJ3D();
    }

    /**
     * Returns a String representation of this WindArrow
     * @return string representing this WindArrow
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("WindArrow: ");
        buf.append(tupleType);
        try {
            buf.append(getData());
        } catch (Exception e) {
            ;
        }
        return buf.toString();
    }

}






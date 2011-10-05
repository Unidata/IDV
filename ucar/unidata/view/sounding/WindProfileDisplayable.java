/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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


import ucar.visad.WindBarbRenderer;
import ucar.visad.display.Displayable;
import ucar.visad.display.LineDrawing;

import visad.CommonUnit;
import visad.DataRenderer;
import visad.Field;
import visad.VisADException;

import visad.bom.BarbRenderer;

import visad.java2d.DisplayRendererJ2D;

import java.rmi.RemoteException;


/**
 * Supports the display a horizontal wind as a wind-direction shaft with barbs.
 *
 * The VisAD MathType of the horizontal wind is the TupleType
 * (GeopotentialAltitude, (WesterlyWind, SoutherlyWind)).
 *
 * @author Steven R. Emmerson
 */
public class WindProfileDisplayable extends LineDrawing {

    /**
     * Constructs from nothing.  The TupleType will be the default.  The data
     * will be manipulable.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindProfileDisplayable() throws VisADException, RemoteException {

        super("WindProfileDisplayable");

    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected WindProfileDisplayable(WindProfileDisplayable that)
            throws VisADException, RemoteException {

        super(that);

    }

    /**
     * Sets the wind.
     *
     * @param profile            The horizontal wind
     *                          (geopotentialAltitude, (u, v)).
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfile(Field profile)
            throws VisADException, RemoteException {

        if (profile == null) {
            throw new NullPointerException();
        }
        setData(profile);
    }

    /**
     * Returns the wind.
     *
     * @return                  The horizontal wind.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Field getProfile() throws VisADException, RemoteException {
        return (Field) getData();
    }

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof WindProfileDisplayable)) {
            equals = false;
        } else {
            try {
                WindProfileDisplayable that     =
                    (WindProfileDisplayable) obj;
                Field                  thisWind = getProfile();
                Field                  thatWind = that.getProfile();

                equals = (this == that) || ((thisWind == null)
                                            ? thatWind == null
                                            : thisWind.equals(
                                            thatWind)) && super.equals(that);
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
            Field wind = getProfile();

            code = ((wind == null)
                    ? 0
                    : wind.hashCode()) ^ super.hashCode();
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
        return new WindProfileDisplayable(this);
    }

    /**
     * Returns the {@link visad.DataRenderer} associated with this instance.
     *
     * @return             The {@link visad.DataRenderer} associated with this
     *                     instance.
     */
    protected DataRenderer getDataRenderer() {
        BarbRenderer br = (getDisplay().getDisplayRenderer()
                           instanceof DisplayRendererJ2D)
                          ? new visad.bom.BarbRendererJ2D()
                          : new WindBarbRenderer(CommonUnit.meterPerSecond);
        return (DataRenderer) br;
    }

}

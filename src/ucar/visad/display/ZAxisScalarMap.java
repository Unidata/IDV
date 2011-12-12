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



import java.awt.geom.AffineTransform;

import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;

import javax.vecmath.Vector3d;


/**
 * Provides support for adapting ScalarMap-s to {@link visad.Display#ZAxis}.
 *
 * <p>Instances of this class have the the JavaBean properties of the
 * superclass.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $
 */
public final class ZAxisScalarMap extends AxisScalarMap {

    /**
     * Constructs.
     *
     * @param realType          The type of data to be mapped to the Z-axis.
     * @param display           The adapted, VisAD display for rendering.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ZAxisScalarMap(RealType realType, DisplayAdapter display)
            throws VisADException, RemoteException {
        super(realType, Display.ZAxis, display);
    }

    /**
     * Translates the axis values.
     *
     * @param amount            The amount to translate the axis.
     * @param transform         The transformation to be translated.
     */
    protected void translate(double amount, AffineTransform transform) {}

    /**
     * Returns a new, 3-D, translation vector.
     *
     * @param amount            The amount to translate the axis.
     *
     * @return Vector3d with the amount of translation
     */
    protected Vector3d new3DTranslationVector(double amount) {
        return new Vector3d(0.0, 0.0, amount);
    }

    /**
     * Scales the axis values.
     *
     * @param amount            The amount to scale the axis by.
     * @param transform         The transformation to be scaled.
     */
    protected void scale(double amount, AffineTransform transform) {}

    /**
     * Scales an array of values by an amount.
     *
     * @param values            The values to be scaled.
     * @param amount            The amount by which to scale the values.
     */
    protected void scale(double[] values, double amount) {
        values[2] *= amount;
    }
}

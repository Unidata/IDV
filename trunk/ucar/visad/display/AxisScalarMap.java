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

import javax.media.j3d.Transform3D;

import javax.vecmath.Vector3d;


/**
 * Provides support for adapting ScalarMap-s to {@link visad.Display#XAxis},
 * {@link visad.Display#YAxis}, or {@link visad.Display#ZAxis}.
 *
 * <p>Instances of this class have the the JavaBean properties of the
 * superclass.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.10 $
 */
public abstract class AxisScalarMap extends ScalarMapAdapter {

    /** matrix for the display */
    private double[] matrix;

    /** flag for 2- vs 3-D */
    private final boolean is2D;

    /**
     * Constructs.
     *
     * @param realType          The type of data to be mapped to the axis.
     * @param displayType       The display type of the axis.
     * @param display           The adapted, VisAD display for rendering.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected AxisScalarMap(RealType realType, DisplayRealType displayType,
                            DisplayAdapter display)
            throws VisADException, RemoteException {

        super(realType, displayType, display);

        is2D = display.getDimensionality() == 2;

        if (is2D) {
            matrix = new double[6];

            new AffineTransform().getMatrix(matrix);
        } else {
            matrix = new double[16];

            new Transform3D().get(matrix);
        }
    }

    /**
     * Translates.
     *
     * @param amount            The amount by which to translate.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void translate(double amount)
            throws VisADException, RemoteException {

        if (is2D) {
            AffineTransform transform = new AffineTransform(matrix);

            translate(amount, transform);
            transform.getMatrix(matrix);
        } else {
            Transform3D transform = new Transform3D(matrix);
            Vector3d    vector    = new Vector3d();

            transform.get(vector);
            vector.add(new3DTranslationVector(amount));
            transform.setTranslation(vector);
            transform.get(matrix);
        }

        setControl();
    }

    /**
     * Scales.
     *
     * @param amount            The amount by which to scale.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void scale(double amount)
            throws VisADException, RemoteException {

        if (is2D) {
            AffineTransform transform = new AffineTransform(matrix);

            scale(amount, transform);
            transform.getMatrix(matrix);
        } else {
            Transform3D transform = new Transform3D(matrix);
            Vector3d    vector    = new Vector3d();

            transform.getScale(vector);

            double[] values = new double[3];

            vector.get(values);
            scale(values, amount);
            vector.set(values);
            transform.setScale(vector);
            transform.get(matrix);
        }

        setControl();
    }

    /**
     * Flips.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void flip() throws VisADException, RemoteException {
        scale(-1);
    }

    /**
     * Sets the control of the underlying {@link visad.ScalarMap}.  This is a
     * template method.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setControl() throws VisADException, RemoteException {

        ProjectionControl control = (ProjectionControl) getControl();

        if (control != null) {
            control.setMatrix(matrix);
        }
    }

    /**
     * Translates the axis values.
     *
     * @param amount            The amount to translate the axis.
     * @param transform         The transformation to be translated.
     */
    protected abstract void translate(double amount,
                                      AffineTransform transform);

    /**
     * Returns a new, 3-D, translation vector.
     *
     * @param amount            The amount to translate the axis.
     * @return  Vector3D of the translation amount
     */
    protected abstract Vector3d new3DTranslationVector(double amount);

    /**
     * Scales the axis values.
     *
     * @param amount            The amount to scale the axis by.
     * @param transform         The transformation to be scaled.
     */
    protected abstract void scale(double amount, AffineTransform transform);

    /**
     * Scales.
     *
     * @param values            Unknown.
     * @param amount            Unknown.
     */
    protected abstract void scale(double[] values, double amount);
}

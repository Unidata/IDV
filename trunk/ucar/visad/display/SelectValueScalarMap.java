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
 * Provides support for adapting ScalarMap-s to {@link
 * visad.Display#SelectValue}.
 *
 * <p>Instances of this class have the the JavaBean properties of the
 * superclass as well as the following, bound, JavaBean properties:<br>
 * <table border align=center>
 *
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Access</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr align=center>
 * <td>value</td>
 * <td>{@link visad.Real}</td>
 * <td>set/get</td>
 * <td>new Real(Double.NaN)</td>
 * <td align=left>The selected value</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $
 */
public class SelectValueScalarMap extends ScalarMapAdapter {

    /**
     * The name of the value property.
     */
    public static final String VALUE = "value";

    /** selection value */
    private Real value = new Real(Double.NaN);

    /**
     * Constructs.
     *
     * @param realType          The type of data to be value-selected.
     * @param display           The adapted, VisAD display for rendering.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public SelectValueScalarMap(RealType realType, DisplayAdapter display)
            throws VisADException, RemoteException {
        super(realType, Display.SelectValue, display);
    }

    /**
     * Sets the selection value.  This method fires a {@link
     * java.beans.PropertyChangeEvent} for {@link #VALUE} with this instance
     * as the source and the old and new values appropriately set.  The event
     * is fired <em>synchronously</em> -- so watch out for deadlock.
     *
     * @param value             The selection value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #VALUE
     */
    public final void setValue(float value)
            throws RemoteException, VisADException {
        setValue(new Real((RealType) getScalarType(), value));
    }

    /**
     * Sets the selection value.  This method fires a {@link
     * java.beans.PropertyChangeEvent} for {@link #VALUE} with this instance
     * as the source and the old and new values appropriately set.  The event
     * is fired <em>synchronously</em> -- so watch out for deadlock.
     *
     * @param value             The selection value.
     * @throws UnitException    The unit of the given value is not convertible
     *                          to the unit of the data type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #VALUE
     */
    public synchronized final void setValue(Real value)
            throws UnitException, RemoteException, VisADException {

        Real oldValue = value;

        try {
            this.value = value;

            setControl();
            firePropertyChange(VALUE, oldValue, value);
        } catch (Exception e) {
            this.value = oldValue;
        }
    }

    /**
     * Returns the selection value.
     *
     * @return                  The selection value.
     */
    public synchronized final Real getValue() {
        return value;
    }

    /**
     * Sets the control of the underlying {@link visad.ScalarMap}.  This is a
     * template method.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setControl() throws VisADException, RemoteException {

        ValueControl control = (ValueControl) getControl();

        if (control != null) {
            control.setValue(
                value.getValue(
                    ((RealType) getScalarType()).getDefaultUnit()));
        }
    }
}

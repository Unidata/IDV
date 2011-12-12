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



import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for adapting VisAD ConstantMap-s into something that is
 * (hopefully) easier to use.  Unlike instances of VisAD
 * {@link visad.ConstantMap}, the underlying {@link visad.ConstantMap} of
 * an instance of this class may change.
 * (Yes, I know, a modifiable ConstantMap is something of an oxymoron -- yet
 * it's still useful.)
 *
 * <p>Instances of this class have the following, bound, JavaBean
 * properties:<br>
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
 * <td>constantMap</td>
 * <td>{@link visad.ConstantMap}</td>
 * <td>set/get</td>
 * <td><code>construction-dependent</code></td>
 * <td align=left>The underlying {@link visad.ConstantMap} of this instance</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.10 $
 */
public class ConstantMapAdapter extends MapAdapter implements ConstantMaps {

    /**
     * The name of the constant-map property.
     */
    public static final String CONSTANT_MAP = "constantMap";

    /** ConstantMap to be adapted */
    private ConstantMap constantMap;

    /**
     * Constructs.  The numeric value of the underlying ConstantMap will be NaN.
     *
     * @param drt               The DisplayRealType to be associated with this
     *                          adapter.
     * @throws VisADException   VisAD failure.
     */
    public ConstantMapAdapter(DisplayRealType drt) throws VisADException {

        super(drt);

        constantMap = new ConstantMap(Double.NaN, drt);
    }

    /**
     * Constructs.
     *
     * @param value             The initial value for the DisplayRealType
     *                          in units of the default unit of the
     *                          DisplayRealType.
     * @param drt               The DisplayRealType to be associated with this
     *                          adapter.
     * @throws VisADException   VisAD failure.
     */
    public ConstantMapAdapter(Real value, DisplayRealType drt)
            throws VisADException {

        super(drt);

        constantMap = new ConstantMap(value, drt);
    }

    /**
     * Constructs.
     *
     * @param value             The initial value for the DisplayRealType
     *                          in units of the default unit of the
     *                          DisplayRealType.
     * @param drt               The DisplayRealType to be associated with this
     *                          adapter.
     * @throws VisADException   VisAD failure.
     */
    public ConstantMapAdapter(double value, DisplayRealType drt)
            throws VisADException {

        super(drt);

        constantMap = new ConstantMap(value, drt);
    }

    /**
     * Set the constant value of the associated DisplayRealType.  Fires a {@link
     * java.beans.PropertyChangeEvent} for {@link #CONSTANT_MAP} with this
     * instance as the source and with the both the old and new values
     * appropriately set.  The event is fired <em>synchronously</em> --
     * so watch out for deadlock.
     *
     * @param value             The new value for the associated
     *                          DisplayRealType.
     * @throws VisADException   VisAD failure.
     */
    public synchronized void setValue(Real value) throws VisADException {
        setConstantMap(new ConstantMap(value, getDisplayRealType()));
    }

    /**
     * Set the constant value of the associated DisplayRealType.  Fires a
     * {@link java.beans.PropertyChangeEvent} for {@link #CONSTANT_MAP} with
     * this instance as the source and with the both the old and new values
     * appropriately set.  The event is fired <em>synchronously</em> --
     * so watch out for deadlock.
     *
     * @param value             The new value for the associated DisplayRealType
     *                          in units of the default unit of the
     *                          DisplayRealType.
     * @throws VisADException   VisAD failure.
     */
    public synchronized void setValue(double value) throws VisADException {
        setConstantMap(new ConstantMap(value, getDisplayRealType()));
    }

    /**
     * Set the ConstantMap being adapted
     *
     * @param newConstantMap  new map
     */
    private void setConstantMap(ConstantMap newConstantMap) {

        if ( !constantMap.equals(newConstantMap)) {
            ConstantMap oldConstantMap = constantMap;

            constantMap = newConstantMap;

            firePropertyChange(CONSTANT_MAP, oldConstantMap, constantMap);
        }
    }

    /**
     * Returns the constant-map property.
     *
     * @return                    The constant-map property.
     */
    public ConstantMap getConstantMap() {
        return constantMap;
    }

    /**
     * Returns the constant-map properties.  This implementation returns this
     * instances constant-map in a one-element array.
     *
     * @return                    The constant-map properties.
     */
    public ConstantMap[] getConstantMaps() {
        return new ConstantMap[] { constantMap };
    }

    /**
     * Returns the {@link visad.ConstantMap}s of this instance.  It is
     * guaranteed that the returned array contains no duplicates.
     *
     * @param constantMapAdapters       An array of adapted ConstantMap-s.  May
     *                                  be <code>null</code>.
     * @return                          The array of ConstantMap-s corresponding
     *                                  to the input.  May be <code>null</code>.
     */
    public static ConstantMap[] getConstantMaps(
            ConstantMapAdapter[] constantMapAdapters) {

        ConstantMap[] constantMaps;

        if (constantMapAdapters == null) {
            constantMaps = null;
        } else {
            constantMaps = new ConstantMap[constantMapAdapters.length];

            for (int i = 0; i < constantMaps.length; ++i) {
                constantMaps[i] = constantMapAdapters[i].constantMap;
            }
        }

        return constantMaps;
    }

    /**
     * This method should be package private because it is expected that only a
     * DisplayAdapter will invoke this method.
     *
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDisplay(Display display)
            throws VisADException, RemoteException {
        display.addMap(constantMap);
    }

    /**
     * Accepts a visitor for this instance.
     *
     * @param visitor           The visitor for this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void accept(ConstantMaps.Visitor visitor)
            throws VisADException, RemoteException {
        visitor.visit(this);
    }

    /**
     * Returns the string representation of this instance.
     *
     * @return                  The string representation of this instance.
     */
    public String toString() {
        return constantMap.toString();
    }

    /**
     * Compares this instance to another object.
     *
     * @param obj               The other object.  Must be an instance of this
     *                          class.
     * @return                  A value less than, equal to, or greater than
     *                          zero depending on whether this instance is
     *                          considered less than, equal to, or greater than
     *                          the other object, respectively.
     */
    public int compareTo(Object obj) {
        return constantMap.compareTo(obj);
    }

    /**
     * Indicates if this instance is semantically identical to another object.
     *
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semantically identical to the other object.
     */
    public boolean equals(Object obj) {
        return compareTo(obj) == 0;
    }

    /**
     * Returns the hash code of this instance.
     *
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return constantMap.hashCode();
    }
}

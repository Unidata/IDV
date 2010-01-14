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
 * Provides support for adapting VisAD ScalarMap-s to something that is
 * (hopefully) easier to use.  Unlike instances of VisAD {@link ScalarMap}, one
 * can modify the associated {@link ScalarType} of an instance of this class.
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
 * <td>scalarType</td>
 * <td>{@link visad.ScalarType}</td>
 * <td>set/get</td>
 * <td>construction-dependent</td>
 * <td align=left>The {@link ScalarType} of the underlying {@link ScalarMap}
 * of this instance</td>
 * </tr>
 *
 * <tr align=center>
 * <td>scalarMap</td>
 * <td>{@link visad.ScalarMap}</td>
 * <td></td>
 * <td>construction-dependent</td>
 * <td align=left>The underlying {@link ScalarMap} of this instance</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.10 $
 */
public abstract class ScalarMapAdapter extends MapAdapter {

    /**
     * The name of the {@link ScalarType} property.
     */
    public static final String SCALAR_TYPE = "scalarType";

    /**
     * The name of the {@link ScalarMap} property.
     */
    public static final String SCALAR_MAP = "scalarMap";

    /** lower range value */
    private Real lower;

    /** upper range value */
    private Real upper;

    /** ScalarMap being adapted */
    private ScalarMap scalarMap;

    /** Set of listeners on the ScalarMaps */
    private java.util.Set scalarMapListeners = new HashSet();

    /** A local listeners */
    private ScalarMapListener myScalarMapListener;

    /** local copy of the display */
    private Display display;

    /**
     * Constructs.
     * @param st                The ScalarType to be associated with the
     *                          DisplayRealType.
     * @param drt               The DisplayRealType to be associated with the
     *                          ScalarType.
     * @param displayAdapter    The associated, adapted, VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected ScalarMapAdapter(ScalarType st, DisplayRealType drt,
                               DisplayAdapter displayAdapter)
            throws VisADException, RemoteException {

        super(drt);

        display             = displayAdapter.getDisplay();
        myScalarMapListener = new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    setControl();
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {}
        };

        scalarMapListeners.add(myScalarMapListener);
        newScalarMap(st, drt);
        displayAdapter.accept(this);
    }

    /**
     * Sets the "scalarMap" field.
     *
     * @param st                The ScalarType for the ScalarMap.
     * @param drt               The DisplayRealType for the ScalarMap.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void newScalarMap(ScalarType st, DisplayRealType drt)
            throws VisADException, RemoteException {

        if (scalarMap != null) {
            for (Iterator iter = scalarMapListeners.iterator();
                    iter.hasNext(); ) {
                scalarMap.removeScalarMapListener(
                    (ScalarMapListener) iter.next());
            }
        }

        scalarMap = new ScalarMap(st, drt);

        for (Iterator iter =
                scalarMapListeners.iterator(); iter.hasNext(); ) {
            scalarMap.addScalarMapListener((ScalarMapListener) iter.next());
        }

        setRange();
    }

    /**
     * Explicitly sets the range of {@link RealType} data values that is mapped
     * to the natural range of {@link DisplayRealType} display values.  This
     * method is used to define a linear map from Scalar to DisplayScalar
     * values.
     *
     * @param lower             The data value to be mapped to the low end
     *                          of the natural range of the DisplayRealType.
     * @param upper             The data value to be mapped to the upper end
     *                          of the natural range of the DisplayRealType.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setRange(Real lower, Real upper)
            throws VisADException, RemoteException {

        this.lower = lower;
        this.upper = upper;

        setRange();
    }

    /**
     * Sets the range of data values in the underlying ScalarMap.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setRange() throws VisADException, RemoteException {

        if ((lower != null) && (upper != null)) {
            Unit unit = ((RealType) getScalarType()).getDefaultUnit();

            scalarMap.setRange(lower.getValue(unit), upper.getValue(unit));
        }
    }

    /**
     * Copies the underlying {@link ScalarMap} from another adapter.
     *
     * @param that              The other adapter.
     */
    synchronized final void duplicate(ScalarMapAdapter that) {

        scalarMap = that.scalarMap;
        lower     = that.lower;
        upper     = that.upper;

        scalarMapChange();
    }

    /**
     * Handles a change to the underlying {@link ScalarMap}.
     * This method should be overridden in subclasses when appropriate.
     */
    protected void scalarMapChange() {}

    /**
     * Sets the {@link ScalarType} that is mapped to the associated {@link
     * DisplayRealType}.  If the new {@link ScalarType} differs from the
     * previous {@link ScalarType}, then the underlying {@link ScalarMap} is
     * changed to conform.  All {@link ScalarMapListener}s registered with this
     * instance are removed from the previous, underlying {@link ScalarMap}
     * and transferred to the new, underlying {@link ScalarType}.  This method
     * fires {@link PropertyChangeEvent}s for {@link #SCALAR_TYPE} and {@link
     * #SCALAR_MAP} with this instance as the source and the old and new values
     * appropriately set.  This is done <em>synchronously</em> -- so watch out
     * for deadlock.
     *
     * @param scalarType        The ScalarType to be mapped to the
     *                          DisplayRealType.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see DisplayAdapter#accept(ScalarMapAdapter)
     */
    protected synchronized void setScalarType(ScalarType scalarType)
            throws VisADException, RemoteException {

        if ( !scalarMap.getScalar().equals(scalarType)) {
            ScalarType oldScalarType = (ScalarType) scalarMap.getScalar();
            ScalarMap  oldScalarMap  = scalarMap;

            newScalarMap(scalarType, getDisplayRealType());
            firePropertyChange(SCALAR_TYPE, oldScalarType, scalarType);
            firePropertyChange(SCALAR_MAP, oldScalarMap, scalarMap);
        }
    }

    /**
     * Returns the {@link ScalarType} of the underlying {@link ScalarMap}.
     *
     * @return                  The {@link ScalarType} of the underlying {@link
     *                          ScalarMap}.
     */
    public synchronized ScalarType getScalarType() {
        return scalarMap.getScalar();
    }

    /**
     * Returns the {@link DisplayRealType} of the underlying {@link ScalarMap}.
     *
     * @return                  The {@link DisplayRealType} of the underlying
     *                          {@link ScalarMap}.
     */
    public synchronized DisplayRealType getDisplayType() {
        return scalarMap.getDisplayScalar();
    }

    /**
     * Adds a {@link ScalarMapListener} to the underlying {@link ScalarMap}.
     * If the underlying {@link ScalarMap} changes, then the {@link
     * ScalarMapListener}s added by this instance are removed from the previous,
     * underlying {@link ScalarType} and transferred to the new, underlying
     * {@link ScalarType}.
     *
     * @param listener          The {@link ScalarMapListener} to be added to the
     *                          underlying {@link ScalarMap}.
     */
    public synchronized void addScalarMapListener(
            ScalarMapListener listener) {
        scalarMapListeners.add(listener);
        scalarMap.addScalarMapListener(listener);
    }

    /**
     * Removes a {@link ScalarMapListener} from the underlying {@link
     * ScalarMap}.
     *
     * @param listener          The {@link ScalarMapListener} to be removed from
     *                          the underlying {@link ScalarMap}.
     */
    public synchronized void removeScalarMapListener(
            ScalarMapListener listener) {
        scalarMap.removeScalarMapListener(listener);
        scalarMapListeners.remove(listener);
    }

    /**
     * Compares this instance to another object.  The comparison is based solely
     * on the underlying {@link ScalarMap}.
     *
     * @param obj               The other object.
     * @return                  A value less-than, equal-to, or greater-than
     *                          zero depending on whether this instance is
     *                          considered less-than, equal-to, or greater-than
     *                          the other object.
     */
    public int compareTo(Object obj) {
        return scalarMap.compareTo(obj);
    }

    /**
     * Indicates if this instance is semantically identical to another object.
     * Two instances of this class are equal if their underlying {@link
     * ScalarMap}-s are equal.
     *
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if the other
     *                          object is an instance of this class and its
     *                          underlying {@link ScalarMap} is equal to this
     *                          instance's.
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
        return scalarMap.hashCode();
    }

    /**
     * Returns the control of the underlying {@link ScalarMap}.
     *
     * @return                  The control of the underlying {@link ScalarMap}.
     */
    protected Control getControl() {
        return scalarMap.getControl();
    }

    /**
     * Sets the control of the underlying {@link ScalarMap}.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void setControl()
     throws RemoteException, VisADException;

    /**
     * This method is package private to restrict untoward manipulation of the
     * underlying ScalarMap outside of this class.
     * @return
     */
    ScalarMap getScalarMap() {
        return scalarMap;
    }

    /**
     * Adds the underlying ScalarMap to the VisAD display.
     * This method is package private because it is expected that only the
     * constructor's DisplayAdapter argument will invoke this method.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    void setDisplay() throws VisADException, RemoteException {
        display.addMap(scalarMap);
    }
}

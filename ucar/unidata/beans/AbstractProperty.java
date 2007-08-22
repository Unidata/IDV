/*
 * $Id: AbstractProperty.java,v 1.11 2005/05/13 18:28:22 jeffmc Exp $
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

package ucar.unidata.beans;



import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;


/**
 * Provides support for JavaBean properties -- both vetoable and non-vetoable.
 * This implementation conserves memory by allocating storage for property
 * change listeners only when the first property change listener is added).
 *
 * @author Steven R. Emmerson
 * @version $Id: AbstractProperty.java,v 1.11 2005/05/13 18:28:22 jeffmc Exp $
 */
public abstract class AbstractProperty implements Property {

    /**
     * The set of property change listeners.
     * @serial
     */
    private PropertyChangeSupport listeners;

    /**
     * The source bean of the property.
     * @serial
     */
    private final Object sourceBean;

    /**
     * Whether or not property changes are being reported.
     * @serial
     */
    private boolean reportChanges = true;

    /**
     * The previously-reported property value.
     * @serial
     */
    private Object previousValue;

    /**
     * The current property value.
     * @serial
     */
    private Object currentValue;

    /**
     * The name of the property.
     * @serial
     */
    private final String name;

    /**
     * Constructs an instance.
     *
     * @param sourceBean        The source bean of the property.
     * @param name              The name of the property.
     */
    protected AbstractProperty(Object sourceBean, String name) {
        this.sourceBean = sourceBean;
        this.name       = name;
        previousValue   = null;
        currentValue    = null;
    }

    /**
     * Gets the source bean of the property.
     *
     * @return                  The source bean of the property.
     */
    public final Object getSourceBean() {
        return sourceBean;
    }

    /**
     * Gets the name of the property.
     *
     * @return                  The name of the property.
     */
    public final String getName() {
        return name;
    }

    /**
     * Gets the property value.
     *
     * @return                  The property value, which is not a copy.
     */
    public Object getValue() {
        return currentValue;
    }

    /**
     * Adds a PropertyChangeListener.
     *
     * @param listener          The PropertyChangeListener to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listeners == null) {
            synchronized (this) {
                if (listeners == null) {
                    listeners = new PropertyChangeSupport(sourceBean);
                }
            }
        }
        listeners.addPropertyChangeListener(name, listener);
    }

    /**
     * Removes a PropertyChangeListener.
     *
     * @param listener          The PropertyChangeListener to remove.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        if (listeners != null) {
            listeners.removePropertyChangeListener(name, listener);
        }
    }

    /**
     * Adds a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to add.
     * @throws UnsupportedOperationException
     *                          This operation is unsupported for this
     *                          type of Property.
     */
    public abstract void addVetoableChangeListener(VetoableChangeListener listener)
    throws UnsupportedOperationException;

    /**
     * Removes a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to remove.
     */
    public abstract void removeVetoableChangeListener(
        VetoableChangeListener listener);

    /**
     * Indicates if changes to this property can be vetoed.
     *
     * @return                  True if and only if changes to this property
     *                          can be vetoed.  NB: a VetoableProperty with
     *                          no registered VetoableChangeListener-s will
     *                          still return true.
     */
    public abstract boolean isVetoable();

    /**
     * Sets the property value.  Will notify any listeners if and only if
     * <code>isReporting()</code> is true.  Will always notify any, registered,
     * VetoableChangeListener-s.
     *
     * @param newValue          The new property value.
     * @throws PropertyVetoException
     *                          A registered VetoableChangeListener objected
     *                          to the change.  The change was not committed.
     */
    public abstract void setValueAndNotifyListeners(Object newValue)
    throws PropertyVetoException;

    /**
     * Sets the property value.  Will not notify any PropertyChangeListener-s
     * but will notify all VetoableChangeListener-s.
     *
     * @param newValue          The new property value.
     * @throws PropertyVetoException
     *                          A registered VetoableChangeListener objected
     *                          to the change.  The change was not committed.
     */
    public abstract void setValue(Object newValue)
    throws PropertyVetoException;

    /**
     * Sets the current value.  This is the only way to set the value.
     *
     * @param newValue          The new property value.
     */
    protected final void setCurrentValue(Object newValue) {
        currentValue = newValue;
    }

    /**
     * Enables or disables the reporting of property changes.
     *
     * @param reportChanges     Whether to enable or disable property change
     *                          reporting.
     */
    public final void setReporting(boolean reportChanges) {
        this.reportChanges = reportChanges;
    }

    /**
     * Indicates if the property is reporting changes.
     * @return _more_ 
     */
    public final boolean isReporting() {
        return reportChanges;
    }

    /**
     * Reports changes to the Property.  Changes are only actually reported if
     * <code>isReporting()</code> is true and the current value of the property
     * is not equal to the previously-reported value.
     */
    public final void notifyListeners() {
        if (reportChanges) {
            if (listeners != null) {
                if ((previousValue == null) || (currentValue == null)
                        || !currentValue.equals(previousValue)) {
                    /*
                     * Set previous value to prevent notification loops.
                     */
                    Object oldValue = previousValue;
                    previousValue = currentValue;
                    listeners.firePropertyChange(name, oldValue,
                                                 currentValue);
                }
            }
        }
    }

    /**
     *  This clears the current and previous value without notifying listeners
     */
    public void clearValue() {
        previousValue = null;
        currentValue  = null;
    }


}








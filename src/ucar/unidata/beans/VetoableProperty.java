/*
 * $Id: VetoableProperty.java,v 1.7 2005/05/13 18:28:22 jeffmc Exp $
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



import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;


/**
 * Provides support for vetoable JavaBean properties.  A VetoableProperty is
 * a Property that has the capablity of having changes to its value vetoed by
 * registered VetoableChangeListener-s.  This implementation conserves memory
 * by allocating storage for VetoableChangeListeners only when the first one
 * is added to this property.
 *
 * @author Steven R. Emmerson
 * @version $Id: VetoableProperty.java,v 1.7 2005/05/13 18:28:22 jeffmc Exp $
 */
public class VetoableProperty extends AbstractProperty {

    /**
     * The set of vetoable property change listeners.
     * @serial
     */
    private VetoableChangeSupport listeners;

    /**
     * Constructs an instance.
     *
     * @param sourceBean        The source Bean of the property.
     * @param name              The name of the property.
     */
    public VetoableProperty(Object sourceBean, String name) {
        super(sourceBean, name);
    }

    /**
     * Indicates if changes to this property can be vetoed.
     *
     * @return                  True; always.
     */
    public boolean isVetoable() {
        return true;
    }

    /**
     * Adds a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to add.
     */
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        if (listeners == null) {
            synchronized (this) {
                if (listeners == null) {
                    listeners = new VetoableChangeSupport(getSourceBean());
                }
            }
        }
        listeners.addVetoableChangeListener(getName(), listener);
    }

    /**
     * Removes a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to remove.
     */
    public void removeVetoableChangeListener(
            VetoableChangeListener listener) {
        if (listeners != null) {
            listeners.removeVetoableChangeListener(getName(), listener);
        }
    }

    /**
     * Sets the property value.  Will notify any listeners if and only if
     * <code>isReporting()</code> is true.  Will always notify all
     * VetoableChangeListener-s.
     *
     * @param newValue          The new property value.
     * @throws PropertyVetoException
     *                          A registered VetoableChangeListener objected
     *                          to the change.  The change was not committed.
     */
    public void setValueAndNotifyListeners(Object newValue)
    throws PropertyVetoException {
        setValue(newValue);
        notifyListeners();
    }

    /**
     * Sets the property value.  Will not notify any PropertyChangeListener-s
     * but will notify all VetoableChangeListener-s.
     *
     * @param newValue          The new property value.
     * @throws PropertyVetoException
     *                          A registered VetoableChangeListener objected
     *                          to the change.  The change was not committed.
     */
    public void setValue(Object newValue) throws PropertyVetoException {
        if (listeners != null) {
            Object oldValue = getValue();
            if ((oldValue == null) || (newValue == null)
                    || !oldValue.equals(newValue)) {
                listeners.fireVetoableChange(getName(), getValue(), newValue);
            }
        }
        setCurrentValue(newValue);
    }
}








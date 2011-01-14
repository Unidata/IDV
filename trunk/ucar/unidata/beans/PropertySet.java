/*
 * $Id: PropertySet.java,v 1.10 2005/05/13 18:28:22 jeffmc Exp $
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



import java.io.Serializable;

import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 * Provides support for JavaBean properties.
 *
 * @author Steven R. Emmerson
 * @version $Id: PropertySet.java,v 1.10 2005/05/13 18:28:22 jeffmc Exp $
 */
public class PropertySet implements Serializable {

    /**
     * A name -> property map.  This isn't synchronized because it should be
     * completely established during construction of the containing object.
     * @serial
     */
    private final Map propertyNameMap = new TreeMap();

    /**
     * Adds a property to the collection of properties.
     *
     * @param property
     */
    public void addProperty(Property property) {
        propertyNameMap.put(property.getName(), property);
    }

    /**
     * Removes a property from the collection of properties.
     *
     * @param property
     */
    public void removeProperty(Property property) {
        propertyNameMap.remove(property.getName());
    }

    /**
     * Gets a property by name from the collection of properties.
     *
     * @param name              The name of the property.
     * @return                  The Property corresponding to <code>name</code>.
     */
    public Property getProperty(String name) {
        return (Property) propertyNameMap.get(name);
    }

    /**
     * Disables the reporting of property changes.  After invoking this method,
     * all subsequent property change events will be deferred until the
     * method <code>enablePropertyChangeEvents()</code> is invoked.
     */
    public synchronized void disablePropertyChangeEvents() {
        for (Iterator iter = propertyNameMap.values().iterator();
                iter.hasNext(); ) {
            ((Property) iter.next()).setReporting(false);
        }
    }

    /**
     * Enables the reporting of property changes.  All deferred property change
     * events will be reported as well as all subsequent events until the next
     * invocation of <code>disablePropertyChangeEvents()</code>.
     */
    public synchronized void enablePropertyChangeEvents() {
        /*
         * Rely on the fact that a Property object won't fire
         * a property change event unless the property has changed.
         */
        for (Iterator iter = propertyNameMap.values().iterator();
                iter.hasNext(); ) {
            Property property = (Property) iter.next();
            property.setReporting(true);
            property.notifyListeners();
        }
    }

    /**
     * Adds a property change listener for all properties in the set.
     *
     * @param listener          The property change listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        for (Iterator iter = propertyNameMap.values().iterator();
                iter.hasNext(); ) {
            ((Property) iter.next()).addPropertyChangeListener(listener);
        }
    }

    /**
     * Removes a property change listener for all properties in the set.
     *
     * @param listener          The property change listener.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        for (Iterator iter = propertyNameMap.values().iterator();
                iter.hasNext(); ) {
            ((Property) iter.next()).removePropertyChangeListener(listener);
        }
    }

    /**
     * Adds a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {
        Property property = (Property) propertyNameMap.get(name);
        if (property != null) {
            property.addPropertyChangeListener(listener);
        }
    }

    /**
     * Removes a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    public void removePropertyChangeListener(
            String name, PropertyChangeListener listener) {
        Property property = (Property) propertyNameMap.get(name);
        if (property != null) {
            property.removePropertyChangeListener(listener);
        }
    }

    /**
     * Adds a vetoable property change listener for all vetoable properties
     * in the set.
     *
     * @param listener          The vetoable property change listener.
     */
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        for (Iterator iter = propertyNameMap.values().iterator();
                iter.hasNext(); ) {
            ((Property) iter.next()).addVetoableChangeListener(listener);
        }
    }

    /**
     * Removes a vetoable property change listener for all vetoable properties
     * in the set.
     *
     * @param listener          The vetoable property change listener.
     */
    public void removeVetoableChangeListener(
            VetoableChangeListener listener) {
        for (Iterator iter = propertyNameMap.values().iterator();
                iter.hasNext(); ) {
            ((Property) iter.next()).removeVetoableChangeListener(listener);
        }
    }

    /**
     * Adds a vetoable property change listener for a named vetoable property.
     * If the named property is not vetoable, then nothing happens.
     *
     * @param name              The name of the vetoable property.
     * @param listener          The vetoable property change listener.
     */
    public void addVetoableChangeListener(String name,
                                          VetoableChangeListener listener) {
        Property property = (Property) propertyNameMap.get(name);
        if (property != null) {
            property.addVetoableChangeListener(listener);
        }
    }

    /**
     * Removes a vetoable property change listener for a named vetoable
     * property.  If the named property is not vetoable, then nothing happens.
     *
     * @param name              The name of the vetoable property.
     * @param listener          The vetoable property change listener.
     */
    public void removeVetoableChangeListener(
            String name, VetoableChangeListener listener) {
        Property property = (Property) propertyNameMap.get(name);
        if (property != null) {
            property.removeVetoableChangeListener(listener);
        }
    }
}








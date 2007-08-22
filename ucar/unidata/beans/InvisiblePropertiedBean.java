/*
 * $Id: InvisiblePropertiedBean.java,v 1.7 2005/05/13 18:28:22 jeffmc Exp $
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
import java.beans.PropertyVetoException;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 * Provides support for JavaBeans that have properties.  An
 * InvisiblePropertiedBean automatically has at least one JavaBean property,
 * which is named "title".
 *
 * @author Steven R. Emmerson
 * @version $Id: InvisiblePropertiedBean.java,v 1.7 2005/05/13 18:28:22 jeffmc Exp $
 */
public abstract class InvisiblePropertiedBean implements PropertiedBean {

    /**
     * Title property.
     * @serial
     */
    private final Property titleProperty;

    /**
     * The set of JavaBean properties.
     */
    private final PropertySet propertySet = new PropertySet();

    /**
     * Constructs from a default title.  Subclasses that extend this class
     * should ensure that all properties of the Bean are added during
     * construction so as to avoid synchronization problems.
     *
     * @param title             The initial title for this Bean.  May be
     *                          <code>null</code>.
     */
    protected InvisiblePropertiedBean(String title) {
        propertySet.addProperty(titleProperty = new NonVetoableProperty(this,
                "title"));
        if (title != null) {
            try {
                titleProperty.setValue(title);
            } catch (PropertyVetoException e) {}    // can't happen because Bean just created
        }
    }

    /**
     * Adds a property to the collection of properties.
     *
     * @param property
     */
    public void addProperty(Property property) {
        propertySet.addProperty(property);
    }

    /**
     * Disables the reporting of property changes.  After invoking this method,
     * all subsequent property change events will be deferred until the
     * method <code>enablePropertyChangeEvents()</code> is invoked.
     */
    public synchronized void disablePropertyChangeEvents() {
        propertySet.disablePropertyChangeEvents();
    }

    /**
     * Enables the reporting of property changes.  All deferred property change
     * events will be reported as well as all subsequent events until the next
     * invocation of <code>disablePropertyChangeEvents()</code>.
     */
    public synchronized void enablePropertyChangeEvents() {
        propertySet.enablePropertyChangeEvents();
    }

    /**
     * Sets the title property.
     *
     * @param title             The title.
     * @throws PropertyVetoException
     *                          The new title was objected to; the change was
     *                          aborted.
     */
    public void setTitle(String title) throws PropertyVetoException {
        this.titleProperty.setValue(title);
    }

    /**
     * Gets the title property.
     *
     * @return                  The title.
     */
    public String getTitle() {
        return (String) titleProperty.getValue();
    }

    /**
     * Adds a property change listener.
     *
     * @param listener          The property change listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySet.addPropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener.
     *
     * @param listener          The property change listener.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        propertySet.removePropertyChangeListener(listener);
    }

    /**
     * Adds a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {
        propertySet.addPropertyChangeListener(name, listener);
    }

    /**
     * Removes a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    public void removePropertyChangeListener(
            String name, PropertyChangeListener listener) {
        propertySet.removePropertyChangeListener(name, listener);
    }
}








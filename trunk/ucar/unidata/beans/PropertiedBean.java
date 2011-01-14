/*
 * $Id: PropertiedBean.java,v 1.9 2005/05/13 18:28:22 jeffmc Exp $
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


/**
 * Provides support for JavaBeans that have properties.
 *
 * @author Steven R. Emmerson
 * @version $Id: PropertiedBean.java,v 1.9 2005/05/13 18:28:22 jeffmc Exp $
 */
public interface PropertiedBean extends Serializable {

    /**
     * Adds a property to the collection of properties.
     *
     * @param property
     */
    void addProperty(Property property);

    /**
     * Disables the reporting of property changes.  After invoking this method,
     * all subsequent property change events will be deferred until the
     * method <code>enablePropertyChangeEvents()</code> is invoked.
     */
    void disablePropertyChangeEvents();

    /**
     * Enables the reporting of property changes.  All deferred property change
     * events will be reported as well as all subsequent events until the next
     * invocation of <code>disablePropertyChangeEvents()</code>.
     */
    void enablePropertyChangeEvents();

    /**
     * Adds a property change listener.
     *
     * @param listener          The property change listener.
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a property change listener.
     *
     * @param listener          The property change listener.
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Adds a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    void addPropertyChangeListener(String name,
                                   PropertyChangeListener listener);

    /**
     * Removes a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    void removePropertyChangeListener(String name,
                                      PropertyChangeListener listener);
}








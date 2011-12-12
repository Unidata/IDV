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



import java.beans.*;


/**
 * Interface for classes with JavaBean properties.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $
 */
public interface Propertied {

    /**
     * Adds a PropertyChangeListener to this instance.
     *
     * @param listener          The PropertyChangeListener to be added.
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     *
     * Adds a PropertyChangeListener for a named property to this instance.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    void addPropertyChangeListener(String name,
                                   PropertyChangeListener listener);

    /**
     *
     * Removes a PropertyChangeListener from this instance.
     * @param listener          The PropertyChangeListener to be removed.
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     *
     * Removes a PropertyChangeListener for a named property from this instance.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     */
    void removePropertyChangeListener(String name,
                                      PropertyChangeListener listener);
}

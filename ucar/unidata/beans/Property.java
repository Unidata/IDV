/*
 * $Id: Property.java,v 1.13 2005/05/13 18:28:22 jeffmc Exp $
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
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;


/**
 * Provides support for JavaBean properties -- both vetoable and non-vetoable.
 *
 * @author Steven R. Emmerson
 * @version $Id: Property.java,v 1.13 2005/05/13 18:28:22 jeffmc Exp $
 */
public interface Property {

    /**
     * Gets the source bean of the property.
     *
     * @return                  The source bean of the property.
     */
    Object getSourceBean();

    /**
     * Gets the name of the property.
     *
     * @return                  The name of the property.
     */
    String getName();

    /**
     * Gets the property value.
     *
     * @return                  The property value, which is not a copy.
     */
    Object getValue();

    /**
     * Adds a PropertyChangeListener.
     *
     * @param listener          The PropertyChangeListener to add.
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a PropertyChangeListener.
     *
     * @param listener          The PropertyChangeListener to remove.
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Adds a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to add.
     * @throws UnsupportedOperationException
     *                          This operation is unsupported for this
     *                          type of Property.
     */
    abstract void addVetoableChangeListener(VetoableChangeListener listener)
    throws UnsupportedOperationException;

    /**
     * Removes a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to remove.
     */
    abstract void removeVetoableChangeListener(
        VetoableChangeListener listener);

    /**
     * Indicates if changes to this property can be vetoed.
     *
     * @return                  True if and only if changes to this property
     *                          can be vetoed.  NB: a VetoableProperty with
     *                          no registered VetoableChangeListener-s will
     *                          still return true.
     */
    abstract boolean isVetoable();

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
    abstract void setValueAndNotifyListeners(Object newValue)
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
    abstract void setValue(Object newValue) throws PropertyVetoException;

    /**
     * Enables or disables the reporting of property changes.
     *
     * @param reportChanges     Whether to enable or disable property change
     *                          reporting.
     */
    void setReporting(boolean reportChanges);

    /**
     * Indicates if the property is reporting changes.
     * @return _more_ 
     */
    boolean isReporting();

    /**
     * Reports changes to the Property.  Changes are only actually reported if
     * <code>isReporting()</code> is true and the current value of the property
     * is not equal to the previously-reported value.
     */
    void notifyListeners();

    /**
     *  This clears the current and previous value without notifying listeners
     */
    public void clearValue();


}








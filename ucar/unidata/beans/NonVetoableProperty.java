/*
 * $Id: NonVetoableProperty.java,v 1.5 2005/05/13 18:28:22 jeffmc Exp $
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


/**
 * Provides support for non-vetoable JavaBean properties.
 *
 * @author Steven R. Emmerson
 * @version $Id: NonVetoableProperty.java,v 1.5 2005/05/13 18:28:22 jeffmc Exp $
 */
public class NonVetoableProperty extends AbstractProperty {

    /**
     * Constructs an instance.
     *
     * @param sourceBean        The source bean of the property.
     * @param name              The name of the property.
     */
    public NonVetoableProperty(Object sourceBean, String name) {
        super(sourceBean, name);
    }

    /**
     * Indicates if changes to this property can be vetoed.
     *
     * @return                  False; always.
     */
    public boolean isVetoable() {
        return false;
    }

    /**
     * Sets the property value.  Will notify any listeners if and only if
     * <code>isReporting()</code> is true.
     *
     * @param newValue          The new property value.
     */
    public void setValueAndNotifyListeners(Object newValue) {
        setValue(newValue);
        notifyListeners();
    }

    /**
     * Sets the property value.
     *
     * @param newValue          The new property value.
     */
    public void setValue(Object newValue) {
        setCurrentValue(newValue);
    }

    /**
     * Doesn't add a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to add.
     * @throws UnsupportedOperationException
     *                          This operation is unsupported for this
     *                          type of Property.  Always thrown.
     */
    public void addVetoableChangeListener(VetoableChangeListener listener)
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
            "Can't add VetoableChangeListener to NonVetoableProperty");
    }

    /**
     * Doesn't remove a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to remove.
     */
    public void removeVetoableChangeListener(
            VetoableChangeListener listener) {}
}








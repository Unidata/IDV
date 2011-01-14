/*
 * $Id: Profile.java,v 1.23 2005/05/13 18:33:35 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.rmi.RemoteException;

import java.beans.*;

import java.util.*;

import ucar.unidata.beans.*;

import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;
import visad.java2d.*;


/**
 * Supports a profile trace on a thermodynamic diagram.
 */
public abstract class Profile extends LineDrawing {

    /**
     * The name of the range-value property.
     */
    public static final String RANGE_VALUE = "rangeValue";

    /**
     * The name of the profile-field property.
     */
    public static final String FIELD = "field";

    /**
     * The range-value property.
     */
    private Real rangeValue;

    /**
     * The pressure property.
     */
    private Real pressure;

    /**
     * The empty field.
     */
    private Field clearField;

    /** original data */
    private Field originalData;

    /**
     * Constructs from a name for the displayable and a function type.
     * @param name              The name for the displayable.
     * @param funcType          The type of the profile function.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @throws ClassCastException if the range isn't a {@link visad.RealType}
     */
    protected Profile(String name, FunctionType funcType)
            throws VisADException, RemoteException {

        super(name);

        setManipulable(true);
        setLineWidth(3);
        setPointSize(4);

        pressure = (Real) funcType.getDomain().getComponent(0).missingData();
        rangeValue = (Real) funcType.getRange().missingData();

        /*
         * The Profile is initialized with a minimal field to accomodate the
         * display system's desire for non-null data.
         */
        clearField = (Field) funcType.missingData();

        setData(clearField);
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @throws NullPointerException if the argument is <code>null</code>.
     */
    protected Profile(Profile that) throws RemoteException, VisADException {

        this(that.getName(), (FunctionType) that.getProfile().getType());

        setPressure(that.pressure);
    }

    /**
     * Sets the profile field property.  This method will cause a
     * PropertyChangeEvent to be fired for the FIELD property.
     * @param field             The profile field property.
     * @throws TypeException    Argument has incorrect type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @throws NullPointerException if the argument is <code>null</code>.
     */
    public void setProfile(Field field)
            throws TypeException, RemoteException, VisADException {

        if (field == null) {
            throw new NullPointerException();
        }

        FunctionType funcType = (FunctionType) clearField.getType();

        Util.vetType(funcType, field);
        setData((Field) field.dataClone());  // causes firePropertyChange(FIELD,...)
        originalData = field;
    }

    /**
     * Returns the profile-field property.  NB: Does not return a copy.
     * @return                  The profile-field property.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Field getProfile() throws VisADException, RemoteException {
        return (Field) getData();
    }

    /**
     * Resets the vertical profile to the profile of the last setProfile().
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setOriginalProfile() throws VisADException, RemoteException {
        setProfile(originalData);
    }

    /**
     * Handles a change to the data referenced by this instances's
     * DataReference.  This method is invoked by the parent class when the
     * data is either explicitly set or directly manipulated.  This method
     * fires a PropertyChangeEvent for the FIELD property with the Field
     * value of the last setProfile(Field) invocation as the old value.
     * It also causes a PropertyChangeEvent to be fired for the RANGE_VALUE
     * property.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void dataChange() throws VisADException, RemoteException {
        firePropertyChange(FIELD, null, getProfile());
        updateRangeValue();
    }

    /**
     * Indicates if this instance is semantically identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semantically identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof Profile)) {
            equals = false;
        } else {
            Profile that = (Profile) obj;

            equals = (this == that)
                     || (rangeValue.equals(that.rangeValue)
                         && pressure.equals(that.pressure)
                         && clearField.equals(that.clearField)
                         && super.equals(that));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return rangeValue.hashCode() ^ pressure.hashCode()
               ^ clearField.hashCode() ^ super.hashCode();
    }

    /**
     * Sets the pressure property.
     *
     * @param pressure          The new value.
     * @throws TypeException    if the pressure has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @throws NullPointerException if the argument is <code>null</code>.
     */
    public void setPressure(Real pressure)
            throws TypeException, RemoteException, VisADException {

        if (pressure == null) {
            throw new NullPointerException();
        }

        Util.vetType(this.pressure.getType(), pressure);

        this.pressure = pressure;

        updateRangeValue();
    }

    /**
     * Returns the pressure property.
     * @return                  The value of the pressure property.
     */
    public Real getPressure() {
        return pressure;
    }

    /**
     * Returns the range-value property.
     * @return                  The range-value property.
     */
    public Real getRangeValue() {
        return rangeValue;
    }

    /**
     * Clears the profile-field.
     */
    public void clear() {

        try {
            setProfile(clearField);
        } catch (VisADException e) {
            System.err.println(this.getClass().getName() + ".clear(): " + e);
        } catch (RemoteException e) {
            System.err.println(this.getClass().getName() + ".clear(): " + e);
        }
    }

    /**
     * Updates the range-value.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @throws NullPointerException if the profile is <code>null</code>.
     */
    protected void updateRangeValue() throws RemoteException, VisADException {

        Real newRange = (Real) getProfile().evaluate(pressure,
                            Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);

        if ( !newRange.equals(rangeValue)) {
            Real oldRange = rangeValue;

            rangeValue = newRange;

            firePropertyChange(RANGE_VALUE, oldRange, newRange);
        }
    }
}








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

package ucar.unidata.idv.control;


import ucar.unidata.beans.*;

import ucar.unidata.util.LogUtil;

import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;

import visad.java2d.*;

import java.beans.*;



import java.rmi.RemoteException;

import java.util.*;


/**
 * Supports a profile trace on a thermodynamic diagram.
 */
public class SoundingProfile extends LineDrawing {

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

    /** FunctionType for the data */
    private FunctionType funcType;

    /**
     * Constructs from a name for the displayable and a function type.
     * @param name              The name for the displayable.
     * @param funcType          The type of the profile function.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public SoundingProfile(String name, FunctionType funcType)
            throws VisADException, RemoteException {
        super(name);

        setLineWidth(3);
        setPointSize(4);

        this.funcType = funcType;

        /*
         * The SoundingProfile is initialized with a minimal field to accomodate the
         * display system's desire for non-null data.
         */
        clearField = new FlatField(funcType,
                                   new Integer1DSet(funcType.getDomain(), 2));
        setData(clearField);
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected SoundingProfile(SoundingProfile that)
            throws RemoteException, VisADException {
        super(that);
        funcType = that.funcType;  // same data
    }

    /**
     * Sets the profile field property.  This method will cause a
     * PropertyChangeEvent to be fired for the FIELD property.
     * @param field             The profile field property.
     * @throws TypeException    Argument has incorrect type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfile(Field field)
            throws TypeException, RemoteException, VisADException {
        FunctionType funcType = (FunctionType) clearField.getType();
        try {
            Util.vetType(funcType, field);
        } catch (TypeException te) {
            Util.vetType(new FunctionType(RealType.Time, funcType), field);
        }
        setData(field);  // causes firePropertyChange(FIELD,...)
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
    }

    /**
     * Indicates if this instance is semantically identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semantically identical to the other object.
     */
    public boolean equals(Object obj) {
        boolean equals;
        if ( !(obj instanceof SoundingProfile)) {
            equals = false;
        } else {
            SoundingProfile that = (SoundingProfile) obj;
            equals = (this == that)
                     || (clearField.equals(that.clearField)
                         && super.equals(that));
        }
        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return clearField.hashCode() ^ super.hashCode();
    }

    /**
     * Clears the profile-field.
     */
    public void clear() {
        try {
            setProfile(clearField);
        } catch (VisADException e) {
            LogUtil.logException("clear(): ", e);
        } catch (RemoteException e) {
            LogUtil.logException(".clear(): ", e);
        }
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new SoundingProfile(this);
    }
}

/*
 * $Id: DataCell.java,v 1.6 2005/05/13 18:33:27 jeffmc Exp $
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

import visad.ActionImpl;

import visad.Data;

import visad.DataReference;

import visad.DataReferenceImpl;

import visad.TypeException;

import visad.VisADException;


/**
 * Abstract, computational entity with one or more inputs and one output.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:27 $
 */
public abstract class DataCell {

    /** name of cell */
    private final String name;

    /** output reference */
    private final DataReference outRef;

    /** input references */
    private static DataReference[] nilRefArray = new DataReference[0];

    /**
     * Constructs from a name for the instance and the input and output data
     * references.  The name of the {@link visad.ActionImpl} will be the name
     * of this instance with the suffix ".action".  The input and output
     * data references arrays are cloned.
     *
     * @param name                  The name for the instance.
     * @param outRef                The output {@link visad.DataReference}.
     * @throws NullPointerException if any argument is <code>null</code> or if
     *                              an input or output data reference is
     *                              <code>null</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected DataCell(String name, DataReference outRef)
            throws VisADException, RemoteException {

        if (name == null) {
            throw new NullPointerException();
        }

        if (outRef == null) {
            throw new NullPointerException();
        }

        this.name   = name;
        this.outRef = outRef;
    }

    /**
     * Returns the input data references.  The returned array is not backed by
     * this instance.
     *
     * @return                      The input data references.
     */
    public DataReference[] getInputRefs() {
        return nilRefArray;
    }

    /**
     * Returns the data references to the output data objects.  The array is
     * not backed by this instance.
     *
     * @return                      The references to the output data objects.
     */
    public final DataReference getOutputRef() {
        return outRef;
    }

    /**
     * Enables all input data references.  An enabled input data reference will
     * trigger recomputation of the output data object when the referenced,
     * input data object changes.
     *
     * @throws NullPointerException if an input data reference is
     *                              <code>null</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void enableAllInputRefs() throws VisADException, RemoteException {}

    /**
     * Disables an input data reference that was specified during construction.
     * A disabled input data reference will not trigger recomputation of the
     * output data object when the referenced, input data object changes.
     *
     * @param ref                   The input data reference to disable.
     * @return                      True if and only if the input data
     *                              reference was enabled when this method was
     *                              invoked.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public boolean disableRef(DataReference ref)
            throws VisADException, RemoteException {
        return false;
    }

    /**
     * Cleans this instance.  All data references are removed from this
     * instance's {@link visad.ActionImpl} and the action is stopped.
     * This instance may be reused after invoking this method,
     *
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void clean() throws VisADException, RemoteException {}

    /**
     * Returns a string representation of this instance.  The instance's name
     * is returned.
     *
     * @return                     A string representation of this instance.
     */
    public final String toString() {
        return name;
    }
}








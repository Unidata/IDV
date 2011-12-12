/*
 * $Id: ComputeCell.java,v 1.9 2005/08/02 17:01:09 dmurray Exp $
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

import ucar.visad.functiontypes.AtmosphericProfile;

import visad.ActionImpl;

import visad.Data;

import visad.DataReference;

import visad.DataReferenceImpl;

import visad.Field;

import visad.FlatField;

import visad.Real;

import visad.RealType;

import visad.RemoteData;

import visad.RemoteVisADException;

import visad.TypeException;

import visad.VisADException;


/**
 * Abstract, computational entity with one or more inputs and one output.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $ $Date: 2005/08/02 17:01:09 $
 */
public abstract class ComputeCell {

    /** name of cell */
    private final String name;

    /** output reference */
    private final DataReference outRef;

    /** input references */
    private final DataReference[] inRefs;

    /** action */
    private final ActionImpl action;

    /**
     * Constructs from a name for the instance and the input and output data
     * references.  The name of the {@link visad.ActionImpl} will be the name
     * of this instance with the suffix ".action".  The input and output
     * data references arrays are cloned.  Subclasses should invoke {@link
     * #enableAllInputRefs()} at the end of their construction to start the
     * {@link visad.DataReference}s-listening thread.
     *
     * @param name                  The name for the instance.
     * @param inRefs                The input {@link visad.DataReference}s.
     * @param initValue             The initial value of the output data.
     * @throws RemoteVisADException if <code>initValue</code> is a
     *                              {@link visad.RemoteData}.
     * @throws NullPointerException if any argument is <code>null</code> or if
     *                              an input data reference is
     *                              <code>null</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected ComputeCell(String name, DataReference[] inRefs, Data initValue)
            throws RemoteVisADException, VisADException, RemoteException {

        if (name == null) {
            throw new NullPointerException();
        }

        if (inRefs == null) {
            throw new NullPointerException();
        }

        if (initValue == null) {
            throw new NullPointerException();  // would cause ReferenceException
        }

        this.name = name;
        outRef    = new DataReferenceImpl(name + ".DataReference");

        outRef.setData(initValue);

        this.inRefs = (DataReference[]) inRefs.clone();

        for (int i = 0; i < this.inRefs.length; i++) {
            if (this.inRefs[i] == null) {
                throw new NullPointerException(Integer.toString(i));
            }
        }

        final Data[] datums = new Data[inRefs.length];

        action = new ActionImpl(name + ".action") {

            public void doAction() {

                try {
                    for (int i = 0; i < ComputeCell.this.inRefs.length; i++) {
                        datums[i] = ComputeCell.this.inRefs[i].getData();
                    }

                    getOutputRef().setData(compute(datums));
                } catch (Exception ex) {
                    // System.err.println(ex);
                }
            }
        };
    }

    /**
     * Returns the input data references.  The returned array is not backed by
     * this instance.
     *
     * @return                      The input data references.
     */
    public final DataReference[] getInputRefs() {
        return (DataReference[]) inRefs.clone();
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
    public final void enableAllInputRefs()
            throws VisADException, RemoteException {

        for (int i = 0; i < inRefs.length; i++) {
            DataReference ref = inRefs[i];

            if (ref == null) {
                throw new NullPointerException(Integer.toString(i));
            }

            if (action.findReference(ref) == null) {
                action.addReference(ref);
            }
        }
    }

    /**
     * Disables an input data reference.  A disabled input data reference will
     * not trigger recomputation of the output data object when the referenced,
     * input data object changes.
     *
     * @param ref                   The input data reference to disable.
     * @return                      True if and only if the input data
     *                              reference was enabled when this method was
     *                              invoked.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public final boolean disableRef(DataReference ref)
            throws VisADException, RemoteException {

        if (ref == null) {
            throw new NullPointerException();
        }

        boolean enabled = action.findReference(ref) != null;

        if (enabled) {
            action.removeReference(ref);
        }

        return enabled;
    }

    /**
     * Computes the output data objects.  This method is invoked by this
     * instance's {@link visad.ActionImpl}.  It is the subclass's
     * responsibility of to set the output data references.
     *
     * @param datums                The input data in the same order as during
     *                              construction (i.e. <code>datums[i]</code>
     *                              corresponds to <code>inRefs[i]</code>.
     * @return                      The output data objec corresponding to the
     *                              input data.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of VisAD data object (e.g. a {@link
     *                              visad.Real} instead of a
     *                              {@link visad.Field}).
     * @throws TypeException        if the internal details of a VisAD data
     *                              object are wrong (e.g. incompatible unit).
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected abstract Data compute(Data[] datums)
     throws TypeException, VisADException, RemoteException;

    /**
     * Cleans this instance.  All data references are removed from this
     * instance's {@link visad.ActionImpl} and the action is stopped.
     * This instance may be reused after invoking this method,
     *
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public final void clean() throws VisADException, RemoteException {
        action.stop();
    }

    /**
     * Returns a string representation of this instance.  The instance's name
     * is returned.
     *
     * @return                     A string representation of this instance.
     */
    public final String toString() {
        return name;
    }

    /**
     * Convenience method for creating an empty atmospheric profile of a given
     * parameter.
     *
     * @param rangeType             The type of parameter.
     * @return                      An empty profile with the parameter as its
     *                              range type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected static FlatField noDataField(RealType rangeType)
            throws VisADException, RemoteException {
        return (FlatField) new AtmosphericProfile(rangeType).missingData();
    }
}








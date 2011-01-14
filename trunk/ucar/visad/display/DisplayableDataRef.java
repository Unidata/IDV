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


import visad.ActionImpl;
import visad.ConstantMap;
import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.DataRenderer;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRenderer;
import visad.VisADException;

import visad.java2d.DirectManipulationRendererJ2D;

import visad.java3d.DirectManipulationRendererJ3D;



import java.awt.Color;

import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for displaying the VisAD Data of a VisAD DataReference.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.19 $
 */
public class DisplayableDataRef extends Displayable {

    /** flag for manipulation */
    private boolean isManip;

    /** reference for data */
    private DataReference ref;

    /** renderer for data */
    private DataRenderer renderer;

    /** flag for whether reference has been added to the display or not */
    private boolean refAdded = false;

    /**
     * Constructs from a {@link visad.DataReference} and a missing data value.
     * By default, the {@link visad.DataRenderer} will be appropriate for the
     * dimensionality of the display and will not directly manipulate the data
     * (but see {@link #setManipulable}).
     *
     *
     * @param name the name of the reference
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public DisplayableDataRef(String name)
            throws RemoteException, VisADException {
        this(new DataReferenceImpl(name));
    }

    /**
     * Constructs from a {@link visad.DataReference} and a missing data value.
     * By default, the {@link visad.DataRenderer} will be appropriate for the
     * dimensionality of the display and will not directly manipulate the data
     * (but see {@link #setManipulable}).
     *
     * @param ref                   The data reference for this instance.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public DisplayableDataRef(DataReference ref)
            throws RemoteException, VisADException {

        if (ref == null) {
            throw new NullPointerException();
        }

        this.ref = ref;
    }

    /**
     * Returns the underlying {@link visad.DataReference} attribute.
     *
     * @return                    The underlying {@link visad.DataReference}.
     */
    public final DataReference getDataReference() {
        return ref;
    }


    /**
     * Set the data on this reference
     *
     * @param d  data to set
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setData(Data d) throws VisADException, RemoteException {
        ref.setData(d);
    }

    /**
     * Sets the direct-manipulability of the underlying data.  Invoking this
     * method determines whether or not the {@link visad.Data} attribute of this
     * instance's {@link visad.DataReference} attribute may be directly
     * manipulated by the VisAD display subsystem.
     *
     * @param manipulable         Whether or not this instance may be directly
     *                            manipulated by the VisAD display subsystem.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public final synchronized void setManipulable(boolean manipulable)
            throws VisADException, RemoteException {
        this.isManip = manipulable;
    }

    /**
     * Indicates whether or not this instance may be directly manipulated by the
     * VisAD display subsystem.
     *
     * @return                    <code>true</code> if and only if this instance
     *                            may be directly manipulated by the VisAD
     *                            display subsystem.
     */
    public final boolean isManipulable() {
        return isManip;
    }

    /**
     * Sets whether or not this instance is visible.
     *
     * @param visible             Whether or not this instance is visible.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public final synchronized void setVisible(boolean visible)
            throws VisADException, RemoteException {

        super.setVisible(visible);

        if (renderer != null) {
            renderer.toggle(visible);
        }
    }

    /**
     * Adds this instance's {@link visad.DataReference} to the associated VisAD
     * display.  This method does not verify that the VisAD display has been
     * set.  This method is idempotent.
     *
     * @throws VisADException   if a VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     */
    protected final synchronized void myAddDataReferences()
            throws VisADException, RemoteException {

        if ( !refAdded && (ref.getData() != null)) {
            DisplayImpl display = (DisplayImpl) getDisplay();

            renderer = getDataRenderer(display);

            renderer.toggle(getVisible());
            display.addReferences(renderer, ref, getConstantMaps());

            refAdded = true;
        }
    }

    /**
     * Removes this instance's data references from the associated VisAD
     * display.  This method does not verify that the VisAD display has been
     * set.  This method is idempotent.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected final synchronized void myRemoveDataReferences()
            throws VisADException, RemoteException {

        if (refAdded) {
            getDisplay().removeReference(ref);

            refAdded = false;
        }
    }

    /**
     * <p>Indicates whether or not this {@link Displayable} adapts a single,
     * VisAD data object.</p>
     *
     * <p>This implementation always returns <code>true</code>.</p>
     *
     * @return                  True if and only if this instance adapts a
     *                          single, VisAD data object.
     */
    public final boolean hasDataObject() {
        return true;
    }

    /**
     * Returns the DataRenderer for this displayable.
     *
     * @param display           The VisAD display for which a DataRenderer is
     *                          needed.
     * @return                  This {@link Displayable}'s
     *                          {@link visad.DataRenderer}.
     * @throws VisADException   VisAD failure.
     */
    protected DataRenderer getDataRenderer(DisplayImpl display)
            throws VisADException {

        DisplayRenderer displayRenderer = display.getDisplayRenderer();

        return !isManipulable()
               ? (DataRenderer) displayRenderer.makeDefaultRenderer()
               : displayRenderer.getMode2D()
                 ? (DataRenderer) new DirectManipulationRendererJ2D()
                 : (DataRenderer) new DirectManipulationRendererJ3D();
    }

    /**
     * Sets the color of this Displayable.
     *
     * @param   color     The color.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setColor(Color color) throws VisADException, RemoteException {

        addConstantMap(new ConstantMap(color.getRed() / 255., Display.Red));
        addConstantMap(new ConstantMap(color.getGreen() / 255.,
                                       Display.Green));
        addConstantMap(new ConstantMap(color.getBlue() / 255., Display.Blue));
    }

    /**
     * Sets the width of lines in this Displayable.
     *
     * @param   width     Width of lines (2 = normal)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setLineWidth(float width)
            throws VisADException, RemoteException {
        addConstantMap(new ConstantMap(width, Display.LineWidth));
    }

    /**
     * Sets the style of lines in this Displayable.
     *
     * @param   lineStyle     style of line
     * @see visad.GraphicsModeControl
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setLineStyle(int lineStyle)
            throws VisADException, RemoteException {
        addConstantMap(new ConstantMap(lineStyle, Display.LineStyle));
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {

        DisplayableDataRef clone = new DisplayableDataRef(ref);

        clone.setManipulable(isManip);

        return clone;
    }
}

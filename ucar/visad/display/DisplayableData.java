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


import ucar.unidata.beans.*;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.Trace;

import ucar.visad.Util;


import visad.*;



import visad.java2d.DirectManipulationRendererJ2D;
import visad.java2d.DisplayRendererJ2D;

import visad.java3d.DirectManipulationRendererJ3D;

import java.awt.event.InputEvent;



import java.beans.*;

import java.rmi.RemoteException;

import java.util.Iterator;
import java.util.List;


/**
 * Provides support for displaying data that comprises a single VisAD Data
 * object (and, consequently, needs only a single DataReference).
 *
 * <p>
 * Instances of this class have the following bound properties:<br>
 * <table border align=center>
 *
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Access</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr align=center>
 * <td>boolean</td>
 * <td>set/is</td>
 * <td><code>true</code></td>
 * <td align=left>Whether or not the data should be rendered.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>manipulable</td>
 * <td>boolean</td>
 * <td>set/is</td>
 * <td><code>false</code></td>
 * <td align=left>Whether or not the data can be manipulated (i.e. modified)
 * via the display.</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.55 $
 */
public class DisplayableData extends Displayable {

    /**
     * The name of the "manipulable" property.
     */
    public static final String MANIPULABLE = "manipulable";

    /**
     * The name of the "visible" property.
     */
    public static final String VISIBLE = "visible";

    /**
     * The name of the line-width property.
     */
    public static String LINE_WIDTH = "lineWidth";

    /** pickable flag */
    private boolean pickable = false;

    /** flag for manipulable objects */
    private boolean manipulable = false;

    /** name of the Displayable (for DataReference) */
    private final String name;

    /** the DataReference used for the data */
    private DataReference reference;

    /** listener for data changes */
    private ActionImpl changeListener;

    /** whether the display is/should be active or not */
    private boolean active = true;

    /** renderer for the data */
    protected DataRenderer renderer;

    /** flag for whether the reference as been added to the display yet */
    private boolean refAdded = false;

    /** cached set */
    private Set cachedAnimationSet = null;




    /** line width for lines */
    private float myLineWidth = 1;

    /** local point size */
    private float myPointSize = 1;

    /**
     * Constructs from a name for the Displayable.  Constructs with a default
     * DataRenderer which is appropriate for the dimensionality of the display
     * and does not directly manipulate the data.  The "visible" property
     * will initially be <code>true</code>.
     *
     * @param name                The name for the Displayable.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public DisplayableData(String name)
            throws VisADException, RemoteException {

        this.name      = name;
        reference      = new DataReferenceImpl(name + "Ref");
        changeListener = newChangeListener(true);

        changeListener.addReference(reference);
    }

    /**
     * Constructs from another instance.  The following attributes are set from
     * the other instance: name, manipulatility, visibility, the data reference,
     * and activity.  Note, in particular, that the data reference
     * is set by assignment from the data reference of the other instance (i.e.
     * the same data reference is used).
     *
     * @param that                The other instance.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public DisplayableData(DisplayableData that)
            throws VisADException, RemoteException {

        super(that);

        synchronized (that) {
            manipulable = that.manipulable;
            name        = that.name;
            myLineWidth = that.myLineWidth;
            myPointSize = that.myPointSize;

            // reference = that.reference;
            reference      = new DataReferenceImpl(name + "Ref");
            active         = that.active;
            changeListener = newChangeListener(active);
            changeListener.addReference(reference);
        }
    }


    /**
     * Set the flags for whether the Displayable uses it's methods
     * to render quickly (eg, not account for projection seams).
     *
     * @param fastRender Should the rendering be quick (and possibly
     *                   inaccurate)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setUseFastRendering(boolean fastRender)
            throws VisADException, RemoteException {
        super.setUseFastRendering(fastRender);
        addConstantMap(new ConstantMap((fastRender
                                        ? -1
                                        : 1), Display.AdjustProjectionSeam));
    }


    /**
     * Set the flags for whether the Displayable displays data
     * as points.
     *
     * @param usePoints  true to display as points
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setPointMode(boolean usePoints)
            throws VisADException, RemoteException {
        super.setPointMode(usePoints);
        addConstantMap(new ConstantMap((usePoints
                                        ? 1
                                        : -1), Display.PointMode));
    }


    /**
     * Create a new Change listener
     *
     * @param enable   whether the display should be enabled or not
     * @return  the listener
     */
    private ActionImpl newChangeListener(boolean enable) {

        ActionImpl changeListener = new ActionImpl(getClass().getName()
                                        + "ChangeListener") {

            public void doAction() {
                try {
                    if (hasData()) {

                        dataChange();
                    }
                } catch (Exception e) {
                    System.err.println(this.getClass().getName()
                                       + ".doAction(): "
                                       + "Couldn't handle new data: " + e);
                    e.printStackTrace();
                }
            }
        };

        /* ActionImpl-s are enabled by default. */
        if ( !enable) {
            changeListener.disableAction();
        }

        return changeListener;
    }

    /**
     * <p>Returns a clone of this instance suitable for another VisAD Display.
     * </p>
     *
     * <p>This implementation does not clone the underlying data.</p>
     *
     * @return                    A clone of this instance for another display.
     * @throws VisADException     if a VisAD failure occurs.
     * @throws RemoteException    if a Java RMI failure occurs.
     */
    public Displayable cloneForDisplay()
            throws VisADException, RemoteException {

        return new DisplayableData(this);
    }

    /**
     * Returns the name of this instance.
     *
     * @return                    The name of this instance as given to
     *                            the constructor.
     */
    public final String getName() {
        return name;
    }

    /**
     * <p>Sets the "manipulable" property.  This property may be set even
     * while this instance is being displayed.  This method fires a
     * PropertyChangeEvent for @link #MANIPULABLE}.</p>
     *
     * <p>This implementation uses the overridable method {@link
     * #getDataRenderer()} to obtain a new {@link visad.DataRenderer}, if
     * necessary, and the method {@link #getConstantMaps()} if the
     * data-reference needs to be (re)added to the display.</p>
     *
     * @param manipulable         Whether or not this instance may be directly
     *                            manipulated by the VisAD system.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public final void setManipulable(boolean manipulable)
            throws VisADException, RemoteException {

        if (this.manipulable != manipulable) {
            Boolean oldValue;
            synchronized (this) {
                oldValue         = new Boolean(this.manipulable);
                this.manipulable = manipulable;
                if (renderer != null) {
                    removeDataReferences();
                    addDataReferences();
                }
            }

            firePropertyChange(MANIPULABLE, oldValue,
                               new Boolean(this.manipulable));
        }
    }

    /**
     * Gets the "manipulable" property.
     *
     * @return            <code>true</code> if and only if this instance may be
     *                    directly manipulated by the VisAD system.
     */
    public final boolean isManipulable() {
        return manipulable;
    }

    /**
     * Sets the "visible" property.  This method fires a PropertyChangeEvent for
     * VISIBLE.
     *
     * @param visible            Whether or not this instance should be visible.
     * @throws VisADException    VisAD failure.
     * @throws RemoteException   Java RMI failure.
     */
    public void setVisible(boolean visible)
            throws RemoteException, VisADException {

        Boolean oldValue;
        synchronized (this) {
            oldValue = new Boolean(getVisible());
            super.setVisible(visible);

            if (renderer != null) {
                renderer.toggle(getVisible());
            }
        }

        firePropertyChange(VISIBLE, oldValue, new Boolean(getVisible()));
    }

    /**
     * Gets the "active" property.
     * @return active state
     */
    public boolean getActive() {
        return active;
    }

    /**
     * Sets the "active" property.  Changes to the data object are reported if
     * and only if the value of the "active" property is <code>true</code>.  If
     * the data object changed while the active property was <code>false</code>,
     * then the change will be reported when the active property is set
     * to <code>true</code>.
     *
     * @param active              Whether or not to report changes to the data
     *                            object.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public final void setActive(boolean active)
            throws VisADException, RemoteException {

        synchronized (this) {
            if (this.active == active) {
                return;
            }
            this.active = active;
        }

        if (active) {
            changeListener.enableAction();  // enqueues changeListener
        } else {
            changeListener.disableAction();
        }
    }

    /**
     * <p>Adds the VisAD DataReference of this instance to the associated VisAD
     * display if appropriate.  This method does not verify that the VisAD
     * display has been set.  This method is idempotent.</p>
     *
     * <p>This implementation uses the overridable methods {@link
     * #getDataRenderer()} and {@link #getConstantMaps()}.</p>
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    protected final void myAddDataReferences()
            throws VisADException, RemoteException {

        boolean callDataChange = false;
        synchronized (this) {
            if ( !refAdded && hasData()) {
                callDataChange = true;
                renderer       = getDataRenderer();
                renderer.toggle(getVisible());
                getDisplay().addReferences(renderer, reference,
                                           getConstantMaps());

                refAdded = true;
            }
        }
        if (callDataChange) {
            dataChange();
        }
    }

    /**
     * Removes the VisAD DataReference of this instance from the associated
     * VisAD display.  This method does not verify that the VisAD display has
     * been set.  This method is idempotent.  Invoking this method will result
     * in the overridable method {@link #getDataRenderer()} being invoked if
     * this instance is subsequently added to the display.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    protected final void myRemoveDataReferences()
            throws VisADException, RemoteException {
        if (refAdded) {
            getDisplay().removeReference(reference);
            refAdded           = false;
            renderer           = null;
            cachedAnimationSet = null;
            dataChange();
        }
    }

    /**
     * Adds this instance's data references to the associated VisAD display if
     * possible.  This method does not verify that the VisAD display has been
     * set.  This method is idempotent.
     *
     *
     * @param newMaps maps to add
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void myAddConstantMaps(ConstantMap[] newMaps)
            throws VisADException, RemoteException {
        if ( !refAdded) {
            return;
        }
        setDisplayInactive();
        DataDisplayLink link =
            (DataDisplayLink) ((DisplayImpl) getDisplay()).findReference(
                reference);
        link.setConstantMaps(newMaps);
        setDisplayActive();
    }

    /**
     * <p>Handles a change to the data.  This method is called when
     * the data of this instance's DataReference changes -- whether by
     * direct manipulation by the VisAD display or by the setData() or
     * setDataReference() methods.  This method should be overridden in
     * subclasses when appropriate.</p>
     *
     * <p>This implementation does nothing.</p>
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    protected void dataChange() throws VisADException, RemoteException {
        if (getDisplayMaster() != null) {
            getDisplayMaster().dataChange();
        }
    }

    /**
     * <p>
     * Set the data reference for this instance.  This method will result in the
     * invocation of the dataChange() method.  This method may be called even
     * while this instance is being rendered in the associated VisAD display:
     * it will cause the new data to be rendered.</p>
     *
     * <p>This implementations uses the overridable methods {@link
     * #getDataRenderer()} and {@link #getConstantMaps()}.<p>
     *
     * @param reference           The data reference for this instance.  May not
     *                            have <code>null</code> data.
     * @throws TypeException      Data reference has <code>null</code> data.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     * @see #dataChange()
     */
    public synchronized final void setDataReference(DataReference reference)
            throws TypeException, RemoteException, VisADException {

        if (reference.getData() == null) {
            throw new TypeException(this.getClass().getName()
                                    + ".setData(DataReference): Null data");
        }

        if (getDisplay() != null) {
            removeDataReferences();
        }

        changeListener.removeReference(this.reference);

        this.reference = reference;

        changeListener.addReference(this.reference);  // enqueues changeListener

        if (addRefsInvoked()) {
            myAddDataReferences();
        }
    }



    /**
     * Called when the displayable is removed from a display master
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void destroy() throws RemoteException, VisADException {
        if (getDestroyed()) {
            return;
        }
        if ((changeListener != null) && (reference != null)) {
            changeListener.removeReference(reference);
        }
        renderer           = null;
        changeListener     = null;
        reference          = null;
        cachedAnimationSet = null;
        super.destroy();
    }


    /**
     * <p>Sets the data for this instance.  This method will result in the
     * invocation of the dataChange() method.  This method may be called even
     * while this instance is being rendered in the associated VisAD display:
     * it will cause the new data to be rendered.</p>
     *
     * <p>This implementation uses the overridable methods {@link
     * #getDataRenderer()} and {@link #getConstantMaps()}.</p>
     *
     * @param data                The data for this instance.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     * @see #dataChange()
     */
    public final void setData(Data data)
            throws RemoteException, VisADException {

        if (data == null) {
            throw new NullPointerException();
        }
        cachedAnimationSet = null;
        if (reference == null) {
            return;  // might have been removed before by another thread
        }
        reference.setData(data);  // might enqueue changeListener
        if (addRefsInvoked()) {
            myAddDataReferences();
        }
    }

    /**
     * <p>Indicates whether or not this {@link Displayable} adapts a single,
     * VisAD data object.</p>
     *
     * <p>This implementation always returns <code>true</code>.
     *
     * @return                  True if and only if this instance adapts a
     *                          single, VisAD data object.
     */
    public final boolean hasDataObject() {
        return true;
    }

    /**
     * Returns the data of this instance or <code>null</code> if no such data
     * exists.  If the return-value is non-<code>null</code> then it is the
     * actual data and not a copy.
     *
     * @return                    The data value or <code>null</code>.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public final Data getData() throws VisADException, RemoteException {
        return reference.getData();
    }

    /**
     * Adds a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public final void addAction(Action action)
            throws VisADException, RemoteException {
        action.addReference(reference);
    }

    /**
     * Removes a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public final void removeAction(Action action)
            throws VisADException, RemoteException {
        action.removeReference(reference);
    }

    /**
     * See if any data has been set in the DataReference for this
     * DisplayableData.
     *
     * @return                    true if the data in the reference is not null.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public boolean hasData() throws VisADException, RemoteException {
        if (reference == null) {
            return false;
        }
        return reference.getData() != null;
    }

    /**
     * Obtains the DataRenderer for this displayable.  This is a template
     * method: it may be overridden in subclasss to supply a non-default
     * DataRenderer.
     *
     * @return                    The DataRenderer for this displayable.
     * @throws VisADException     VisAD failure.
     */
    protected DataRenderer getDataRenderer() throws VisADException {

        DisplayRenderer displayRenderer = getDisplay().getDisplayRenderer();
        boolean         is2d = displayRenderer instanceof DisplayRendererJ2D;
        if (isPickable() && isManipulable()) {
            throw new IllegalStateException(
                "Displayable:" + getClass().getName()
                + " cannot be both pickable and manipulable");
        }

        if (isPickable()) {
            if (is2d) {
                return new visad.bom.PickManipulationRendererJ2D();
            } else {
                // allow sloppy picking
                displayRenderer.setPickThreshhold(0.2f);
                return new visad.bom.PickManipulationRendererJ3D(
                    InputEvent.BUTTON1_MASK, InputEvent.BUTTON1_MASK);
            }
        }

        if (isManipulable()) {
            if (is2d) {
                return new DirectManipulationRendererJ2D();
            } else {
                return new DirectManipulationRendererJ3D() {
                    public void addPoint(float[] x) throws VisADException {
                        if (dragAdapter != null) {
                            if ( !dragAdapter.handleAddPoint(x)) {
                                return;
                            }
                        }
                        super.addPoint(x);
                    }

                    public void constrainDragPoint(float[] x) {
                        if (dragAdapter != null) {
                            if ( !dragAdapter.constrainDragPoint(x)) {
                                return;
                            }
                        }
                    }

                    public synchronized void drag_direct(VisADRay ray,
                            boolean first, int mouseModifiers) {
                        if (dragAdapter != null) {
                            if ( !dragAdapter.handleDragDirect(ray, first,
                                    mouseModifiers)) {
                                return;
                            }
                        }
                        super.drag_direct(ray, first, mouseModifiers);
                    }
                };

            }

        }

        return displayRenderer.makeDefaultRenderer();
    }


    /**
     * Print out my name
     *
     * @return  my name
     */
    public String showme() {
        return getClass().getName();
    }

    /**
     * Set the pickable property
     * @param pickable true to pick
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setPickable(boolean pickable)
            throws VisADException, RemoteException {
        this.pickable = pickable;
        if (renderer != null) {
            removeDataReferences();
            addDataReferences();
        }
    }

    /**
     * Get the pickable property
     * @return true if pickable
     */
    public boolean isPickable() {
        return pickable;
    }



    /**
     * <p>Returns the set of values for the given <code>aniType</code> if
     * the contained Data object adapted by this {@link DisplayableData} have
     * any data of that type. <code>null</code> will be returned if
     * this instance adapts such an object but the object is unset, or if this
     * instance does not support this type.
     *
     * @param  aniType          The type used for animation
     * @param force             true to force
     * @return                  The set of times from all data
     *                          May be <code>null</code>.
     * @throws VisADException   if a VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     * @see #hasDataObject()
     */
    public Set getAnimationSet(RealType aniType, boolean force)
            throws VisADException, RemoteException {
        Set overrideSet = super.getAnimationSet(aniType, force);
        if (overrideSet != null) {
            return overrideSet;
        }
        if (cachedAnimationSet != null) {
            return cachedAnimationSet;
        }
        Set aniSet = null;
        if (refAdded && hasData()
                && ScalarType.findScalarType(reference.getData().getType(),
                                             aniType)) {
            Data d = getData();
            /* HACK:  really need to use VisAD DataRenderer.prepareData() */
            if ((d instanceof FieldImpl) && aniType.equals(RealType.Time)
                    && GridUtil.isTimeSequence((FieldImpl) d)) {
                aniSet             = GridUtil.getTimeSet((FieldImpl) d);
                cachedAnimationSet = aniSet;
            } else if (d instanceof Gridded1DSet) {
                aniSet             = (Gridded1DSet) d;
                cachedAnimationSet = aniSet;
            }
        }
        return aniSet;
    }

    /**
     * Get a String representation of this object.
     * @return a String representation of this.
     */
    public String toString() {
        return getClass().getName() + ": " + name;
    }

    /**
     * Sets the width of lines in this Displayable.
     *
     * @param   lineWidth     Width of lines (1 = normal)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setLineWidth(float lineWidth)
            throws VisADException, RemoteException {

        float oldValue;

        synchronized (this) {
            oldValue = myLineWidth;
            addConstantMap(new ConstantMap(lineWidth, Display.LineWidth));
            myLineWidth = lineWidth;
        }

    }


    /**
     * Gets the current line width associated with this Displayable
     *
     * @return  line width
     */
    public float getLineWidth() {
        return myLineWidth;
    }

    /**
     * Sets the size of points in this Displayable.
     *
     * @param   pointSize     Size of points (2 = normal)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setPointSize(float pointSize)
            throws VisADException, RemoteException {

        float oldValue;

        synchronized (this) {
            oldValue = myPointSize;
            addConstantMap(new ConstantMap(pointSize, Display.PointSize));
            myPointSize = pointSize;
        }

    }

    /**
     * Gets the point size associated with this DisplayableData
     *
     * @return  point size
     */
    public float getPointSize() {
        return myPointSize;
    }


    /** a drag adapter */
    private DragAdapter dragAdapter;

    /**
     * Set the drag adapter
     *
     * @param dragAdapter the drag adapter
     */
    public void setDragAdapter(DragAdapter dragAdapter) {
        this.dragAdapter = dragAdapter;
    }

    /**
     * DragAdapter
     *
     *
     * @author IDV Development Team
     */
    public interface DragAdapter {

        /**
         * Handle a mouse drag
         *
         * @param ray   the ray
         * @param first is this the first
         * @param mouseModifiers mouse modifiers
         *
         * @return  okay or not
         */
        public boolean handleDragDirect(VisADRay ray, boolean first,
                                        int mouseModifiers);

        /**
         * Handle adding a point
         *
         * @param x  the points
         *
         * @return  okay or not
         */
        public boolean handleAddPoint(float[] x);

        /**
         * Constraing the drag point
         *
         * @param x  the point
         *
         * @return true if constrained
         */
        public boolean constrainDragPoint(float[] x);
    }

}

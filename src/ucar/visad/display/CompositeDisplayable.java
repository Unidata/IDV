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


import visad.*;

import java.awt.Color;



import java.beans.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.*;


/**
 * Supports composition of a list of Displayable-s into a single Displayable.
 *
 * <p>
 * A change to the list of Displayable-s of an instance of this class fires
 * a javax.swing.event.ListDataEvent.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.50 $
 */
public class CompositeDisplayable extends Displayable {

    /** Mutex */
    private Object MUTEX = new Object();


    /**
     * The list of displayables.
     * @serial
     */
    private List displayables = new ArrayList();

    /**
     * The ConstantMap-s associated with this (composite) Displayable.
     */
    private ConstantMapSet constantMaps = new ConstantMapSet();

    /** List of data listeners */
    private volatile List listDataListeners;

    /** _more_ */
    public String label = "LABEL";

    /**
     * Constructs from nothing.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public CompositeDisplayable() throws RemoteException, VisADException {
        this((LocalDisplay) null);
    }

    /**
     * _more_
     *
     * @param lbl _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public CompositeDisplayable(String lbl)
            throws RemoteException, VisADException {
        this((LocalDisplay) null);
        label = lbl;
    }

    /**
     * Constructs from an initial VisAD display.
     *
     * @param display             The VisAD display.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public CompositeDisplayable(LocalDisplay display)
            throws RemoteException, VisADException {
        super(display);
    }

    /**
     * Constructs from another instance.  The Displayable-s and ConstantMap-s are
     * copied from the other instance.  The list of ListDataListener-s, however,
     * is empty.
     * @param that                The other instance.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    protected CompositeDisplayable(CompositeDisplayable that)
            throws VisADException, RemoteException {

        super(that);

        for (Iterator iter = that.iterator(); iter.hasNext(); ) {
            addDisplayable(((Displayable) iter.next()).cloneForDisplay());
        }
        constantMaps = (ConstantMapSet) that.constantMaps.clone();
    }

    /**
     * Adds a listener for list-data events.
     * @param listener            The listener for list-data events.
     */
    public void addListDataListener(ListDataListener listener) {
        if (listDataListeners == null) {
            synchronized (this) {
                if (listDataListeners == null) {
                    listDataListeners =
                        Collections.synchronizedList(new ArrayList());
                }
            }
        }

        listDataListeners.add(listener);
    }

    /**
     * Removes a listener for list-data events.
     *
     * @param listener            The listener for list-data events.
     */
    public void removeListDataListener(ListDataListener listener) {
        listDataListeners.remove(listener);
    }

    /**
     * Adds a Displayable to the composite.
     *
     * @param displayable         The Displayable to be added.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     * @see #setDisplayable
     */
    public void addDisplayable(Displayable displayable)
            throws RemoteException, VisADException {
        synchronized (MUTEX) {
            setDisplayable(displayableCount(), displayable);
        }
    }

    /**
     * Sets a Displayable of the composite.  The Displayable will receive the
     * ConstantMap-s associated with this instance.  The ScalarMap-s of the
     * Displayable will be added to this instance.  The "visible" property of
     * the Displayable will be unaffected.
     *
     * @param index               The index of the Displayable.
     * @param displayable         The Displayable to be added.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setDisplayable(int index, final Displayable displayable)
            throws RemoteException, VisADException {
        synchronized (MUTEX) {
            if (index < displayables.size()) {
                removeDisplayable(index);
            }

            displayable.setParent(this);
            ConstantMap[] maps = constantMaps.getConstantMaps();

            for (int i = 0; i < maps.length; ++i) {
                displayable.addConstantMap(maps[i]);
            }

            addScalarMaps(displayable.getScalarMapSet());
            displayable.addPropertyChangeListener(SCALAR_MAP_SET,
                    new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {
                    try {
                        // add in new maps, but don't fire change here.
                        // we'll do that below
                        ScalarMapSet maps =
                            (ScalarMapSet) event.getNewValue();
                        if ((maps != null) && (maps.size() > 0)) {
                            for (Iterator iter = maps.iterator();
                                    iter.hasNext(); ) {
                                addScalarMap((ScalarMap) iter.next());
                            }
                        }
                    } catch (Exception exc) {
                        System.err.println("Error adding maps:" + exc);
                        exc.printStackTrace();
                    }

                    PropertyChangeEvent newEvent =
                        new PropertyChangeEvent(CompositeDisplayable.this,
                            SCALAR_MAP_SET, event.getOldValue(),
                            event.getNewValue());

                    newEvent.setPropagationId(event.getPropagationId());
                    firePropertyChange(newEvent);
                }
            });

            if (getDisplay() != null) {
                displayable.setDisplay(getDisplay());
            }

            if (addRefsInvoked()) {

                /*
                 * {@link #addDataReferences()} is invoked rather than {@link
                 * #myAddDataReferences()} so that the {@link #addRefsInvoked()}
                 * method of the added {@link Displayable} returns
                 * <code>true</code>.
                 */
                displayable.addDataReferences();
            }

            displayables.add(index, displayable);
        }

        fireListDataIntervalAdded(index, index);
    }

    /**
     * Gets a Displayable of the composite.
     *
     * @param index               The position that contains the Displayable.
     *                            Must be non-negative and less than the number
     *                            of Displayable-s.
     * @return                    The {@link Displayable} at the given position.
     * @throws IndexOutOfBoundsException
     *                            Invalid index.
     */
    public Displayable getDisplayable(int index)
            throws IndexOutOfBoundsException {
        synchronized (MUTEX) {
            List displayables = getDisplayables();
            if ((index < 0) || (index >= displayables.size())) {
                return null;
            }
            return (Displayable) displayables.get(index);
        }
    }

    /**
     * Removes a Displayable from the composite.
     *
     * @param displayable        The Displayable to be removed.
     * @return                   <code>true</code> if and only if this composite
     *                           contained the specified Displayable.
     * @throws VisADException    VisAD failure.
     * @throws RemoteException   Java RMI failure.
     */
    public boolean removeDisplayable(Displayable displayable)
            throws VisADException, RemoteException {
        if (displayable == null) {
            return false;
        }

        boolean existed;
        synchronized (MUTEX) {
            displayable.setParent(null);
            int index = displayables.indexOf(displayable);
            if (index < 0) {
                existed = false;
            } else {
                removeDisplayable(index);
                existed = true;
            }
        }

        return existed;
    }


    /**
     * Called when the displayable is removed from a display master
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    protected void destroy() throws RemoteException, VisADException {
        synchronized (MUTEX) {
            if (displayables != null) {
                for (Iterator iter = iterator(); iter.hasNext(); ) {
                    ((Displayable) iter.next()).destroy();
                }
                displayables = null;
            }
        }
        super.destroy();
    }


    /**
     * Removes a Displayable from the composite.
     *
     * @param index               The index of the displayable to be removed.
     * @return                    The given Displayable.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public Displayable removeDisplayable(int index)
            throws RemoteException, VisADException {

        Displayable displayable;

        synchronized (MUTEX) {
            displayable = (Displayable) displayables.remove(index);
            displayable.removeDataReferences();
            //Get rid of scalar maps...
            ScalarMapSet newSet = new ScalarMapSet();
            for (int i = 0; i < displayables.size(); i++) {
                ScalarMapSet mapSet =
                    ((Displayable) displayables.get(i)).getScalarMapSet();
                if ((mapSet != null) && (mapSet.size() > 0)) {
                    newSet.add(mapSet);
                }
            }
            setScalarMapSet(newSet);
        }
        fireListDataIntervalRemoved(index, index);
        return displayable;
    }

    /**
     * Obtains an Iterator over the children of this composite.
     * @return            An Iterator over the children of this composite.
     */
    public Iterator iterator() {
        if (displayables == null) {
            return new ArrayList().iterator();
        }
        return (new ArrayList(displayables)).iterator();
    }

    /**
     * Sets the associated VisAD display.
     * @param display             The associated VisAD display.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setDisplay(LocalDisplay display)
            throws RemoteException, VisADException {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            ((Displayable) iter.next()).setDisplay(display);
        }
        super.setDisplay(display);
    }

    /**
     * _more_
     *
     * @param master _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void setDisplayMaster(DisplayMaster master)
            throws VisADException, RemoteException {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            ((Displayable) iter.next()).setDisplayMaster(master);
        }
        super.setDisplayMaster(master);

    }

    /**
     * Adds DataReference-s to the associated display.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    protected final void myAddDataReferences()
            throws VisADException, RemoteException {

        /*
         * {@link #addDataReferences()} is invoked rather than {@link
         * #myAddDataReferences()} so that the {@link #addRefsInvoked()} methods
         * of the {@link Displayable}s return <code>true</code>.
         */
        if ( !isActive()) {
            return;
        }
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            ((Displayable) iter.next()).addDataReferences();
        }
    }

    /**
     * Removes this instance's DataReference-s from the associated VisAD
     * display.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    protected final void myRemoveDataReferences()
            throws VisADException, RemoteException {

        /*
         * {@link #removeDataReferences()} is invoked rather than {@link
         * #myRemoveDataReferences()} so that the {@link #addRefsInvoked()}
         * methods of the {@link Displayable}s will return <code>false</code>.
         */
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            ((Displayable) iter.next()).removeDataReferences();
        }
    }

    /**
     * Associates a ConstantMap with this Displayable.
     *
     * @param map                 A ConstantMap to be associated with this
     *                            Displayable.
     * @return                    The ConstantMap (as a convenience).
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public ConstantMap addConstantMap(ConstantMap map)
            throws VisADException, RemoteException {

        // return constantMaps.put(map);
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            ((Displayable) iter.next()).addConstantMap(
                (ConstantMap) map.clone());
        }

        constantMaps.put(map);
        return map;
    }

    /** _more_ */
    public static int cnt = 0;

    /** _more_ */
    int mycnt = cnt++;

    /**
     * _more_
     */
    public void debug() {
        System.err.println(label + " composite size=" + displayables.size());
    }


    /**
     * Sets the visibility of this Displayable.
     *
     * @param visible     If <code>true</code> and this composite's <code>
     *                    addScalarMaps()</code> and <code>addDataReferences()
     *                    </code> methods have been invoked, then the composite
     *                    will be made visible; otherwise, if
     *                    <code>false</code>, then it is or will become
     *                    invisible.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setVisible(boolean visible)
            throws RemoteException, VisADException {
        super.setVisible(visible);
        synchronized (MUTEX) {
            for (Displayable displayable : getDisplayables()) {
                if (displayable != null) {
                    displayable.setVisible(visible);
                }
            }
        }
    }

    /**
     * Sets the manipulable value of this Displayable.
     *
     * @param manipulable value
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setManipulable(boolean manipulable)
            throws VisADException, RemoteException {
        super.setManipulable(manipulable);
        for (Displayable displayable : getDisplayables()) {
            if (displayable != null) {
                displayable.setManipulable(manipulable);
            }
        }


    }


    /**
     * Sets the visibility of an interval of children.
     * @param visible             Whether or not to display the interval of
     *                            children.
     * @param lowerIndex          The lower index of the interval.
     * @param upperIndex          The upper index of the interval.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setVisible(boolean visible, int lowerIndex, int upperIndex)
            throws RemoteException, VisADException {
        synchronized (MUTEX) {
            for (int index = lowerIndex; index <= upperIndex; ++index) {
                Displayable displayable = getDisplayable(index);
                if (displayable != null) {
                    displayable.setVisible(visible);
                }
            }
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
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setUseFastRendering(fastRender);
        }
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
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setPointMode(usePoints);
        }
    }

    /**
     * Set the z position to given value
     *
     * @param value The value
     * @param type The type
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setConstantPosition(double value, DisplayRealType type)
            throws VisADException, RemoteException {
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setConstantPosition(value, type);
        }
    }




    /**
     * Brings all the children to front.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void toFront() throws RemoteException, VisADException {
        super.toFront();
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.toFront();
        }
    }

    /**
     * Sets the color of the children
     * @param c                   The color.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setColor(Color c) throws RemoteException, VisADException {
        setDisplayInactive();
        super.setColor(c);
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setColor(c);
        }
        setDisplayActive();
    }



    /**
     * Set the UseTimesInAnimation property.
     *
     * @param value The new value for UseTimesInAnimation
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setUseTimesInAnimation(boolean value)
            throws RemoteException, VisADException {
        setDisplayInactive();
        super.setUseTimesInAnimation(value);
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setUseTimesInAnimation(value);

        }
        setDisplayActive();
    }




    /**
     * Sets whether flows are adjusted.  Used by displays that don't
     * support this.
     *
     * @param adjust     true to adjust
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setAdjustFlow(boolean adjust)
            throws RemoteException, VisADException {
        setDisplayInactive();
        super.setAdjustFlow(adjust);
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setAdjustFlow(adjust);
        }
        setDisplayActive();
    }




    /**
     * Sets the unit on all contained children.
     *
     * @param unit                The display unit to use.
     *
     * @throws VisADException  if a VisADFailure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setDisplayUnit(Unit unit)
            throws RemoteException, VisADException {
        setDisplayInactive();
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setDisplayUnit(unit);
        }
        setDisplayActive();
    }

    // Should this be here?

    /**
     * <p>Sets the color range on all contained children
     *
     * @param low              The minimum value of the range for the color.
     * @param hi               The maximum value of the range for the color.
     * @throws VisADException  if a VisADFailure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     * public void setRangeForColor(double low, double hi)
     *       throws RemoteException, VisADException {
     *   setDisplayInactive();
     *   for (Iterator iter = iterator(); iter.hasNext(); ) {
     *       Displayable displayable = (Displayable) iter.next();
     *       displayable.setRangeForColor(low, hi);
     *   }
     *   setDisplayActive();
     * }
     */


    /**
     * Set the range of the selected data on each of the children
     * displayables.
     *
     * @param low              The minimum value of the selected range
     * @param hi               The maximum value of the selected range
     * @throws VisADException  if a VisADFailure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setSelectedRange(double low, double hi)
            throws RemoteException, VisADException {
        setDisplayInactive();
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setSelectedRange(low, hi);
        }
        setDisplayActive();
    }


    /**
     * Sets the unit to be used for colors on all contained children
     *
     *
     * @param unit                The display unit to use.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setColorUnit(Unit unit)
            throws RemoteException, VisADException {
        super.setColorUnit(unit);
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setColorUnit(unit);
        }
    }


    /**
     * Sets the point size of the children
     *
     * @param size  size (pixels)
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setPointSize(float size)
            throws RemoteException, VisADException {

        super.setPointSize(size);
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setPointSize(size);
        }
    }

    /**
     * Sets the line width of the children.  Calls setLineWidth
     * on each child
     *
     * @param width  width for children (pixels)
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void setLineWidth(float width)
            throws RemoteException, VisADException {
        super.setLineWidth(width);
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setLineWidth(width);
        }
    }

    /**
     * Sets the color table of the children.  Calls setColorPalette
     * on each child
     *
     * @param c  color palette
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setColorPalette(float[][] c)
            throws RemoteException, VisADException {

        super.setColorPalette(c);
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.setColorPalette(c);
        }
    }

    /**
     * Sets the visibility of an individual child.
     *
     * @param visible             Whether or not to display the
     * @param index               The index of the child.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setVisible(boolean visible, int index)
            throws RemoteException, VisADException {
        setVisible(visible, index, index);
    }

    /**
     * Removes all displayables from this composite -- causing them to be
     * removed from the VisAD display.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void clearDisplayables() throws RemoteException, VisADException {
        for (int i = 0; i < displayables.size(); i++) {
            removeDisplayable((Displayable) displayables.get(i));
        }
        //removeDataReferences();
        synchronized (MUTEX) {
            displayables.clear();
        }
    }



    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void destroyAll() throws RemoteException, VisADException {
        destroy();
    }



    /**
     * Obtains the last displayable added to this composite.
     * @return            The last Displayable added to this composite.
     */
    public Displayable lastDisplayable() {
        synchronized (MUTEX) {
            int count = displayableCount();
            return (count == 0)
                   ? null
                   : (Displayable) displayables.get(count - 1);
        }
    }

    /**
     * Gets the current number of displayables.
     * @return            The current number of Displayable-s in this composite.
     */
    public int displayableCount() {
        synchronized (MUTEX) {
            return displayables.size();
        }
    }

    /**
     * Gets the index of the particular Displayable.  If the {@link Displayable}
     * doesn't exist, then <code>-1</code> is returned.
     *
     * @param displayable The {@link Displayable} to have its index returned.
     * @return            The index of the Displayable in this composite or -1.
     */
    public int indexOf(Displayable displayable) {
        synchronized (MUTEX) {
            return displayables.indexOf(displayable);
        }
    }

    /**
     * <p>Returns the set of values for the given <code>aniType</code> if
     * the contained Data object adapted by this {@link DisplayableData} have
     * any data of that type. <code>null</code> will be returned if
     * this instance adapts such an object but the object is unset, or if this
     * instance does not support this type.
     *
     * @param  aniType          The type used for animation
     * @param force _more_
     * @return                  The set of times from all data
     *                          May be <code>null</code>.
     * @throws VisADException   if a VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     * @see #hasDataObject()
     */
    public Set getAnimationSet(RealType aniType, boolean force)
            throws RemoteException, VisADException {
        Set aniSet = null;
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            //This seems to have happened once:
            if (displayable == null) {
                continue;
            }
            if ( !force && !displayable.getUseTimesInAnimation()) {
                continue;
            }
            Set set = displayable.getAnimationSet(aniType, force);
            if (set != null) {
                aniSet = (aniSet == null)
                         ? set
                         : aniSet.merge1DSets(set);
            }
        }
        return aniSet;
    }

    /**
     * Get the displayables in this composite
     *
     * @return  the List of Displayables
     */
    private List<Displayable> getDisplayables() {
        synchronized (MUTEX) {
            return new ArrayList<Displayable>(
                (List<Displayable>) displayables);
        }
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                    A semi-deep clone of this instance.
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new CompositeDisplayable(this);
    }

    /**
     * Invokes the contentsChanged() method of all registered ListDataListener-s.
     * @param index0              The lower index of the range.
     * @param index1              The upper index of the range.
     */
    protected void fireListDataContentsChanged(int index0, int index1) {

        if (listDataListeners != null) {
            ListDataEvent event = new ListDataEvent(this,
                                      ListDataEvent.CONTENTS_CHANGED, index0,
                                      index1);
            ListDataListener[] listeners =
                (ListDataListener[]) listDataListeners.toArray(
                    new ListDataListener[0]);

            for (int i = 0; i < listeners.length; ++i) {
                listeners[i].contentsChanged(event);
            }
        }
    }

    /**
     * Invokes the intervalAdded() method of all registered ListDataListener-s.
     * @param index0              The lower index of the range.
     * @param index1              The upper index of the range.
     */
    protected void fireListDataIntervalAdded(int index0, int index1) {

        if (listDataListeners != null) {
            ListDataEvent event = new ListDataEvent(this,
                                      ListDataEvent.INTERVAL_ADDED, index0,
                                      index1);
            ListDataListener[] listeners =
                (ListDataListener[]) listDataListeners.toArray(
                    new ListDataListener[0]);

            for (int i = 0; i < listeners.length; ++i) {
                listeners[i].intervalAdded(event);
            }
        }
    }

    /**
     * Invokes the intervalRemoved() method of all registered
     * ListDataListener-s.
     *
     * @param index0              The lower index of the range.
     * @param index1              The upper index of the range.
     */
    protected void fireListDataIntervalRemoved(int index0, int index1) {

        if (listDataListeners != null) {
            ListDataEvent event = new ListDataEvent(this,
                                      ListDataEvent.INTERVAL_REMOVED, index0,
                                      index1);
            ListDataListener[] listeners =
                (ListDataListener[]) listDataListeners.toArray(
                    new ListDataListener[0]);

            for (int i = 0; i < listeners.length; ++i) {
                listeners[i].intervalRemoved(event);
            }
        }
    }

    /**
     * Returns a string representation of this composite.
     *
     * @return              A string representation of this composite.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();

        sb.append("CompositeDisplayable:");
        sb.append("\n");

        int i = 1;

        for (Iterator iter = iterator(); iter.hasNext(); i++) {
            sb.append("Displayable ");
            sb.append(Integer.toString(i));
            sb.append(":");
            sb.append("\n");
            sb.append(((Displayable) iter.next()).toString());
            sb.append("\n");
        }

        return sb.toString();
    }
}

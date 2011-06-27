/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;

import ucar.visad.Util;

import visad.*;



import java.beans.*;

import java.rmi.RemoteException;

import java.util.Iterator;
import java.util.List;


/**
 * Provides support for encapsulating one or more displayed data objects
 * together with their display-dependent state.</p>
 *
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
 * <td>display</td>
 * <td>visad.LocalDisplay</td>
 * <td>set/get</td>
 * <td><code>null</code></td>
 * <td align=left>The VisAD display associated with this instance</td>
 * </tr>
 *
 * <tr align=center>
 * <td>scalarMapSet</td>
 * <td>ScalarMapSet</td>
 * <td>get</td>
 * <td>empty set</td>
 * <td align=left>The set of non-spatial or unique ScalarMap-s associated with
 * this instance</td>
 * </tr>
 *
 * </table>
 *
 * <p>This implementation conserves memory by delaying storage allocation for
 * PropertyChangeListener-s until the first one is registered.
 *
 * @see ScalarMapSet
 * @author Steven R. Emmerson
 * @version $Revision: 1.76 $
 */
public abstract class Displayable {

    /** Used in println debuggin */
    static String tab = "";

    /** Should we use times from this displayable in the animation set */
    private boolean useTimesInAnimation = true;

    /** Should the renders adjust the seam */
    private boolean useFastRendering = false;

    /** Should the renders display as points */
    private boolean pointMode = false;

    /** are we destroyed? */
    private boolean destroyed = false;

    /**
     * The name of the VisAD display property.
     */
    public static final String DISPLAY = "display";

    /**
     * The name of the set of ScalarMapSet property.
     */
    public static final String SCALAR_MAP_SET = "scalarMapSet";

    /**
     * The set of associated ConstantMap-s.
     */
    private ConstantMapSet constantMaps = new ConstantMapSet();

    /**
     * The PropertyChangeListener-s.
     */
    private volatile PropertyChangeSupport propertyListeners;

    /** the display that this Displayable is in */
    private LocalDisplay display;

    /** Set of the scalarMaps */
    private ScalarMapSet scalarMapSet;



    /** Set of the previous scalarMaps */
    private ScalarMapSet prevScalarMapSet;

    /**
     *  This is the display unit used by the different subclasses.
     */
    private Unit displayUnit = null;

    /**
     *  Some subclasses may also use the color unit.
     */
    private Unit colorUnit = null;


    /**
     * Is this displayable visible
     */
    private boolean visible = true;

    /** flag for whether addDataReferences has been invoked */
    private boolean addRefsInvoked = false;


    /** The parent. May be null. */
    protected CompositeDisplayable parent;

    /** The display master. May be null */
    private DisplayMaster displayMaster;

    /** the animation set */
    private Set animationSet = null;


    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Displayable() throws RemoteException, VisADException {
        this((LocalDisplay) null);
    }

    /**
     * Constructs from an initial VisAD display.
     * @param display           The VisAD display.  May be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Displayable(LocalDisplay display)
            throws RemoteException, VisADException {

        scalarMapSet     = new ScalarMapSet();
        prevScalarMapSet = new ScalarMapSet();
        this.display     = display;
    }

    /**
     * Constructs from another instance.  The ConstantMap-s and ScalarMap-s are
     * copied from the other instance.  The list of PropertyChangeListener-s is
     * empty, however, and the display is unset.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Displayable(Displayable that)
            throws RemoteException, VisADException {
        synchronized (that) {
            visible          = that.visible;
            displayUnit      = that.displayUnit;
            colorUnit        = that.colorUnit;
            constantMaps     = (ConstantMapSet) that.constantMaps.clone();
            scalarMapSet     = new ScalarMapSet(that.scalarMapSet);
            prevScalarMapSet = new ScalarMapSet(that.prevScalarMapSet);
        }
    }


    /**
     * Bring the displayable to the front.  Basically this removes
     * it from the display and re-adds it.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void toFront() throws RemoteException, VisADException {
        //We only remove and then add ourselves when we are not part of a compositedisplayable
        if ((displayMaster != null) && (parent == null)) {
            DisplayMaster myMaster = displayMaster;
            myMaster.removeDisplayable(this);
            myMaster.addDisplayable(this);
        }
    }


    /**
     * Get the display master. If none set on this displayable
     * then, if there is a parent composite, ask the parent. Else
     * return null.
     *
     * @return The {@link DisplayMaster} this displayable is part of.
     */
    public DisplayMaster getDisplayMaster() {
        if (displayMaster == null) {
            if (parent != null) {
                return parent.getDisplayMaster();
            }
        }
        return displayMaster;
    }

    /**
     * Set the display master. This may be null.
     *
     * @param master The {@link DisplayMaster} this displayable is part of.
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    protected void setDisplayMaster(DisplayMaster master)
            throws VisADException, RemoteException {
        this.displayMaster = master;
    }


    /**
     * Set the flags for whether the renderer should use fast (but
     * perhaps inaccurate) rendering (ie, not account for projection
     * seams)
     *
     * @param fastRender  true to render quickly
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setUseFastRendering(boolean fastRender)
            throws VisADException, RemoteException {
        this.useFastRendering = fastRender;
    }


    /**
     * Get whether or not to use fast rendering.
     *
     * @return true to use fast rendering
     */
    public boolean getUseFastRendering() {
        return useFastRendering;
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
        pointMode = usePoints;
    }

    /**
     * Get whether or not to render data as points
     *
     * @return true to display as points
     */
    public boolean getPointMode() {
        return pointMode;
    }


    /**
     * Set the parent.
     *
     * @param parent The parent composite.
     */
    protected void setParent(CompositeDisplayable parent) {
        this.parent = parent;
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
        addConstantMap(new ConstantMap(value, type));
    }






    /**
     * A wrapper arount DisplayMaster.setDisplayInactive.
     * This must be followed by a corresponding call
     * to setDisplayActive
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDisplayInactive() throws RemoteException, VisADException {
        DisplayMaster master = getDisplayMaster();
        if (master != null) {
            master.setDisplayInactive();
        }
    }


    /**
     * A wrapper arount DisplayMaster.setDisplayActive.
     * This must be preceded by a call to setDisplayInactive
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setDisplayActive() throws RemoteException, VisADException {
        final DisplayMaster master = getDisplayMaster();
        if (master != null) {
            Misc.run(new Runnable() {
                public void run() {
                    try {
                        master.setDisplayActive();
                    } catch (Exception exc) {
                        logException("Setting display master active", exc);
                    }
                }
            });
        }
    }




    /**
     * Is the DisplayMaster this  displayable is part of active
     *
     * @return                 Is active
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public boolean isActive() throws RemoteException, VisADException {
        DisplayMaster master = getDisplayMaster();
        if (master == null) {
            return false;
        }
        return master.isActive();
    }





    /**
     * Associates this instance with a given VisAD display.  If this instance
     * was previously associated with a VisAD display, then it is first
     * removed from that display.  This method fires a PropertyChangeEvent
     * for DISPLAY with the old and new values.  This method will not
     * cause this instance to be rendered in the display.  This method may
     * be overridden in subclasses; the overriding method should invoke
     * <code>super.setDisplay(display)</code>.
     * @param display               The VisAD display.
     * @throws VisADException       if a core VisAD failure occurs.
     * @throws RemoteException      if Java RMI failure occurs.
     * @see #removeDataReferences
     */
    public void setDisplay(LocalDisplay display)
            throws RemoteException, VisADException {

        LocalDisplay oldDisplay;

        if (display == null) {
            throw new DisplayException(getClass().getName()
                                       + ".setDisplay(LocalDisplay): "
                                       + "Display is null");
        }
        synchronized (this) {
            removeDataReferences();
            oldDisplay   = this.display;
            this.display = display;
        }
        firePropertyChange(DISPLAY, oldDisplay, this.display);
    }

    /**
     * Returns the associated VisAD display.
     * @return                  The associated VisAD display.  Will be
     *                          <code>null</code> if this instance hasn't been
     *                          associated with a VisAD display yet.
     */
    public final LocalDisplay getDisplay() {
        return display;
    }

    /**
     * Returns the set of ScalarMap-s associated with this instance.
     * @return                  The set of ScalarMap-s associated with this
     *                          instance.
     */
    public final ScalarMapSet getScalarMapSet() {
        return scalarMapSet;
    }

    /**
     * Returns whether or not the client has requested that this instance's
     * {@link visad.DataReference}s be added to the display.
     *
     * @return                  <code>true</code> if and only if
     *                          {@link #addDataReferences()} has been called
     *                          without an intervening {@link
     *                          #removeDataReferences()}.
     */
    protected final boolean addRefsInvoked() {
        return addRefsInvoked;
    }

    /**
     * Adds a PropertyChangeListener to this instance.
     *
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyListeners().addPropertyChangeListener(listener);
    }

    /**
     * Adds a PropertyChangeListener for a named property to this instance.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {
        getPropertyListeners().addPropertyChangeListener(name, listener);
    }

    /**
     * Removes a PropertyChangeListener from this instance.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {

        if (propertyListeners != null) {
            propertyListeners.removePropertyChangeListener(listener);
        }
    }

    /**
     * Removes a PropertyChangeListener for a named property from this instance.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            String name, PropertyChangeListener listener) {

        if (propertyListeners != null) {
            propertyListeners.removePropertyChangeListener(name, listener);
        }
    }

    /**
     * Adds a ConstantMap to this instance.
     *
     * @param constantMap               The ConstantMap to be added.
     * @return                          The previous constant map or
     *                                  <code>null</code>.
     * @throws VisADException           VisAD failure.
     * @throws RemoteException          Java RMI failure.
     */
    public synchronized ConstantMap addConstantMap(ConstantMap constantMap)
            throws RemoteException, VisADException {
        ConstantMap prev = constantMaps.put(constantMap);
        if (addRefsInvoked && ((prev == null) || !constantMap.equals(prev))) {
            //Trace.call1("addConstantMap");
            myAddConstantMaps(getConstantMaps());
            //Trace.call2("addConstantMap");
        }

        return prev;
    }

    /**
     * Adds ConstantMaps to this instance.
     *
     * @param maps                      The ConstantMaps to be added.
     * @throws VisADException           VisAD failure.
     * @throws RemoteException          Java RMI failure.
     */
    public synchronized void addConstantMaps(ConstantMap[] maps)
            throws RemoteException, VisADException {

        boolean haveNew = false;
        if (maps.length > 0) {
            for (int i = 0; i < maps.length; i++) {
                ConstantMap prev = constantMaps.put(maps[i]);
                if ((prev == null) || !maps[i].equals(prev)) {
                    haveNew = true;
                }
            }

            if (addRefsInvoked && haveNew) {
                //Trace.call1("addConstantMaps");
                myAddConstantMaps(getConstantMaps());
                //Trace.call2("addConstantMaps");
            }
        }
        //        System.err.println ("addConstantMaps " + getClass().getName() + " " + constantMaps.size());



    }

    /**
     * Removes a ConstantMap from this instance.
     *
     * @param displayRealType   The DisplayRealType to have its ConstantMap
     *                          removed.
     * @return                  The associated ConstantMap or <code>null</code>.
     */
    public ConstantMap removeConstantMap(DisplayRealType displayRealType) {
        return constantMaps.remove(displayRealType);
    }

    /**
     *  <p>A no-op method so we can have a consistent api with the composite
     *  pattern.  This method is overwritten in CompositeDisplayable, which
     *  iterates through its children composite calling setRangeForColor.  This
     *  method is overwritten in RGBDisplayable to actually setRangeForColor.
     *  </p>
     *  <p>
     *  There is a fundamental problem with the Composite pattern (of which the
     *  Displayable class serves as the abstract root class) in that you want
     *  to have the Composite class (e.g., CompositeDisplayable) to be able to
     *  have methods which are recursively applied to its children.  However,
     *  not all methods are shared by all potential children classes.  There is
     *  a tension between having methods defined for objects that make sense and
     *  having the utility of applying methods to a group of objects through the
     *  composite.
     *  </p>
     *  <p>
     *  With these no-op methods here (e.g., setColorPalette, setColor,
     *  setPointSize) we leaned towards utility vs. method signature
     *  correctness.
     *  </p>
     *  <p>
     *  Of course, one way around this is to have a general setProperty
     *  (propertyName, value) method which is used.</p>
     *
     * @param low              The minimum value of the range for the color.
     * @param hi               The maximum value of the range for the color.
     * @throws VisADException  if a VisADFailure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setRangeForColor(double low, double hi)
            throws VisADException, RemoteException {}

    /**
     * Just a wrapper around the other
     * {@link #setRangeForColor(double, double)}
     *
     * @param r                The range for the color.
     * @throws VisADException  if a VisADFailure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setRangeForColor(Range r)
            throws VisADException, RemoteException {
        setRangeForColor(r.getMin(), r.getMax());
    }

    /**
     * Just a wrapper around the other
     * {@link #setSelectedRange(double, double)}
     *
     * @param r                The range for the color.
     * @throws VisADException  if a VisADFailure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setSelectedRange(Range r)
            throws VisADException, RemoteException {
        setSelectedRange(r.getMin(), r.getMax());
    }

    /**
     * A no-op method so we can have a consistent api with the composite
     * pattern.  This method is overwritten in CompositeDisplayable
     *
     * @param low              The minimum value of the selected range
     * @param hi               The maximum value of the selected range
     * @throws VisADException  if a VisADFailure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setSelectedRange(double low, double hi)
            throws VisADException, RemoteException {}

    /**
     *  A no-op method so we can have a consistent api with the composite
     *  pattern.  This method is overwritten in CompositeDisplayable, which
     *  iterates through its children composite calling setColorPalette.
     *
     * @param c  color palette
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setColorPalette(float[][] c)
            throws RemoteException, VisADException {}

    /**
     * A no-op method for setting contour information
     *
     * @param contourInfo   Contains contour and labeling information
     *
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public void setContourInfo(ucar.unidata.util.ContourInfo contourInfo)
            throws VisADException, RemoteException {}

    /**
     *  A wrapper  method calling setColorPalette (float[][] c)
     *
     * @param colorTable  ColorTable that defines the palette
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setColorPalette(ucar.unidata.util.ColorTable colorTable)
            throws RemoteException, VisADException {
        setColorPalette(colorTable.getColorTable());
    }

    /**
     *  A no-op method so we can have a consistent api with the composite
     *  pattern.  This method is overwritten in CompositeDisplayable, which
     *  iterates through its children composite calling setColor. This method is
     *  overwritten in LineDrawing.
     *
     * @param c  the Color
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setColor(java.awt.Color c)
            throws RemoteException, VisADException {}

    /**
     *  A no-op method so we can have a consistent api with the composite
     *  pattern.  This method is overwritten in CompositeDisplayable, which
     *  iterates through its children composite calling setColor. This method is
     *  overwritten in LineDrawing.
     *
     * @param size  point size
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setPointSize(float size)
            throws RemoteException, VisADException {}

    /**
     *  A no-op method so we can have a consistent api with the composite
     *  pattern.  This method is overwritten in CompositeDisplayable, which
     *  iterates through its children composite calling setColor. This method is
     *  overwritten in LineDrawing.
     *
     * @param size  line width size (pixels)
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setLineWidth(float size)
            throws RemoteException, VisADException {}

    /**
     * Sets the visibility property.
     *
     * @param visible           Whether or not this instance should be
     *                          displayed.
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setVisible(boolean visible)
            throws RemoteException, VisADException {
        this.visible = visible;
    }

    /**
     * Sets the manipulable value of this Displayable. This is a no-op and
     * is here for the CompositeDisplayable
     *
     * @param manipulable value
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setManipulable(boolean manipulable)
            throws VisADException, RemoteException {}


    /**
     * Gets the visibility property.
     *
     * @return                   Whether or not this instance is visible.
     */
    public boolean getVisible() {
        return this.visible;
    }

    /**
     * Returns the visibility property.
     *
     * @return                    Whether or not this instance should be
     *                            displayed.
     */
    public boolean isVisible() {
        return visible;
    }


    /**
     * Requests that this instance's {@link visad.DataReference}s be added to
     * the associated VisAD display as soon as feasible.  This might or might
     * not happen during the invocation of this method (a
     * {@link visad.DataReference}
     * might not be added if, for example, it contains no data).  Invoking this
     * method will cause all subsequent {@link #addRefsInvoked()} invocations
     * to return <code>true</code> until {@link #removeDataReferences()} is
     * invoked.  This method is idempotent: it may be invoked multiple times.
     *
     * @throws DisplayException if {@link #setDisplay(LocalDisplay)} has not
     *                          been invoked.
     * @throws VisADException   if a VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     */
    public final void addDataReferences()
            throws DisplayException, VisADException, RemoteException {

        if (display == null) {
            throw new DisplayException("Display not set");
        }

        myAddDataReferences();

        addRefsInvoked = true;
    }

    /**
     * Removes this instance's data references from the associated VisAD
     * display.  Invoking this method will cause all subsequent {@link
     * #addRefsInvoked()} invocations to return <code>false</code> until {@link
     * #addDataReferences()} is invoked.  This method is idempotent: it may
     * be invoked multiple times.
     *
     * @throws DisplayException if {@link #setDisplay(LocalDisplay)} hasn't been
     *                          invoked.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public final synchronized void removeDataReferences()
            throws DisplayException, VisADException, RemoteException {
        if (display != null) {
            myRemoveDataReferences();
            addRefsInvoked = false;
        }
    }

    /**
     * Adds this instance's data references to the associated VisAD display if
     * possible.  This method does not verify that the VisAD display has been
     * set.  This method is idempotent.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void myAddDataReferences()
     throws VisADException, RemoteException;

    /**
     * Removes this instance's data references from the associated VisAD
     * display.  This method does not verify that the VisAD display has been
     * set.  This method is idempotent.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void myRemoveDataReferences()
     throws VisADException, RemoteException;


    /**
     * Adds this instance's data references to the associated VisAD display if
     * possible.  This method does not verify that the VisAD display has been
     * set.  This method is idempotent.
     *
     *
     * @param newMaps new maps
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void myAddConstantMaps(ConstantMap[] newMaps)
            throws VisADException, RemoteException {
        //These can cause deadlock
        //setDisplayInactive();
        removeDataReferences();
        addDataReferences();
        //setDisplayActive();
    }



    /**
     * Destroy this instance
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    public void destroyDisplayable() throws RemoteException, VisADException {
        if ( !destroyed) {
            destroy();
        }
    }


    /**
     * Are we destroyed?
     *
     * @return  true if we are
     */
    public boolean getDestroyed() {
        return destroyed;
    }

    /**
     * Called when the displayable is removed from a display master
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void destroy() throws RemoteException, VisADException {
        destroyed         = true;
        display           = null;
        parent            = null;
        displayMaster     = null;
        propertyListeners = null;
        //        scalarMapSet      = null;
        //        constantMaps      = null;
        //        prevScalarMapSet  = null;
    }


    /**
     * Sets the set of ScalarMap-s of this instance.  This method invokes
     * <code>fireScalarMapSetChange()</code>.  Intermediate subclasses that
     * have their own ScalarMap-s should override this method and invoke
     * <code>super.setScalarMaps(ScalarMapSet)</code>.
     * @param maps              The set of ScalarMap-s to be added.
     * @see #fireScalarMapSetChange()
     */
    public void setScalarMapSet(ScalarMapSet maps) {
        scalarMapSet = maps;
        fireScalarMapSetChange();
    }

    /**
     * <p>Indicates whether or not this {@link Displayable} adapts a single,
     * VisAD data object.</p>
     *
     * <p>This implementation always returns <code>false</code>.</p>
     *
     * @return                  True if and only if this instance adapts a
     *                          single, VisAD data object.
     */
    public boolean hasDataObject() {
        return false;
    }

    /**
     * <p>Returns the single, VisAD data object adapted by this {@link
     * Displayable}, if one exists. <code>null</code> will be returned if
     * this instance adapts such an object but the object is unset, or if this
     * instance is not capable of adapting a single, VisAD data object.  To
     * distinguish these cases, use {@link #hasDataObject()}.</p>
     *
     * <p>This implementation always returns null.
     *
     * @return                  The single, adapted, VisAD data object if one
     *                          exists.  Not a copy.  May be <code>null</code>.
     * @throws VisADException   if a VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     * @see #hasDataObject()
     */
    public Data getData() throws VisADException, RemoteException {
        return null;
    }

    /**
     * Adds a set of ScalarMap-s to this instance's set of ScalarMap-s.  This
     * method invokes <code>fireScalarMapSetChange()</code>.
     * @param maps              The set of ScalarMap-s to be added.
     * @throws BadMappingException      Duplicate ScalarMap.
     * @see #fireScalarMapSetChange()
     */
    protected void addScalarMaps(ScalarMapSet maps)
            throws BadMappingException {
        scalarMapSet.add(maps);
        fireScalarMapSetChange();
    }

    /**
     * Adds a ScalarMap to this instance's set of ScalarMap-s.  After a
     * series of invocations of this method, concrete subclass should invoke
     * <code>fireScalarMapSetChange()</code>.
     * @param map               The ScalarMap to be added.
     * @throws BadMappingException      Duplicate ScalarMap.
     * @see #fireScalarMapSetChange()
     */
    protected void addScalarMap(ScalarMap map) throws BadMappingException {
        scalarMapSet.add(map);
    }

    /**
     * Returns the {@link visad.ScalarMap} in this instance's set of {@link
     * visad.ScalarMap}s that matches a template or <code>null</code> if no such
     * {@link visad.ScalarMap} is found.
     *
     * @param template                  The template.
     * @return                          The matching ScalarMap or
     *                                  <code>null</code>.
     */
    protected ScalarMap getScalarMap(ScalarMap template) {
        return scalarMapSet.get(template);
    }

    /**
     * Removes a ScalarMap from this instance's set of ScalarMap-s.  After a
     * series of invocations of this method, concrete subclass should invoke
     * <code>fireScalarMapSetChange()</code>.
     * @param map               The ScalarMap to be removed.
     * @see #fireScalarMapSetChange()
     */
    protected void removeScalarMap(ScalarMap map) {
        scalarMapSet.remove(map);
    }

    /**
     * Replaces a ScalarMap in this instance's set of ScalarMap-s.  After a
     * series of invocations of this method, concrete subclass should invoke
     * <code>fireScalarMapSetChange()</code>.
     * @param oldMap            The old ScalarMap to be removed.  May be
     *                          <code>null</code>.
     * @param newMap            The new ScalarMap to be added.
     * @throws BadMappingException      Duplicate ScalarMap.
     * @see #fireScalarMapSetChange()
     */
    protected synchronized void replaceScalarMap(ScalarMap oldMap,
            ScalarMap newMap)
            throws BadMappingException {

        if (oldMap != null) {
            scalarMapSet.remove(oldMap);
        }
        scalarMapSet.add(newMap);
    }

    /**
     * Finds the ScalarMap of a VisAD display that has the given ScalarType
     * and DisplayRealType.
     * @param display           The VisAD display.
     * @param scalarType        The ScalarType to match.
     * @param displayRealType   The DisplayRealType to match.
     * @return                  The matching ScalarMap of the display.  Will be
     *                          <code>null</code> if no matching ScalarMap was
     *                          found.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected static ScalarMap getScalarMap(LocalDisplay display,
                                            ScalarType scalarType,
                                            DisplayRealType displayRealType)
            throws VisADException, RemoteException {

        ScalarMap map = null;

        for (Iterator iter = display.getMapVector().iterator();
                iter.hasNext(); ) {
            map = (ScalarMap) iter.next();

            if (map.getScalar().equals(scalarType)
                    && map.getDisplayScalar().equals(displayRealType)) {
                break;
            }
        }

        return map;
    }

    /**
     * Finds the ScalarMap of a VisAD display that matches a given ScalarMap
     * in the sense that it has the same ScalarType and DisplayRealType.
     * @param display           The VisAD display.
     * @param template          The ScalarMap to match.
     * @return                  The matching ScalarMap of the display.  Will be
     *                          <code>null</code> if no matching ScalarMap was
     *                          found.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected static ScalarMap getScalarMap(LocalDisplay display,
                                            ScalarMap template)
            throws VisADException, RemoteException {
        return getScalarMap(display, template.getScalar(),
                            template.getDisplayScalar());
    }


    /**
     * Calls firePropertyChangeEventInThread so we break up potential deadlocks
     * @param event             The PropertyChangeEvent.
     */
    protected void firePropertyChange(final PropertyChangeEvent event) {
        Misc.run(new Runnable() {
            public void run() {
                firePropertyChangeInThread(event);
            }
        });
    }


    /**
     * Fires a PropertyChangeEvent.
     * @param event             The PropertyChangeEvent.
     */
    private void firePropertyChangeInThread(PropertyChangeEvent event) {
        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(event);
        }
    }

    /**
     * Fires a PropertyChangeEvent.
     * @param propertyName      The name of the property.
     * @param oldValue          The old value of the property.
     * @param newValue          The new value of the property.
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {

        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(propertyName, oldValue,
                    newValue);
        }
    }

    /**
     * Returns the PropertyChangeListener-s of this instance.
     * @return                  The PropertyChangeListener-s.
     */
    private PropertyChangeSupport getPropertyListeners() {
        if (propertyListeners == null) {
            synchronized (this) {
                if (propertyListeners == null) {
                    propertyListeners = new PropertyChangeSupport(this);
                }
            }
        }
        return propertyListeners;
    }

    /**
     * Returns copies of the ConstantMap-s of this instance.
     * @return          The ConstantMap-s of this instance.
     */
    public ConstantMap[] getConstantMaps() {
        return constantMaps.getConstantMaps();
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public abstract Displayable cloneForDisplay()
     throws RemoteException, VisADException;

    /**
     * Combines this instance's ConstantMap-s with another set of ConstantMap-s
     * and returns a copy of the set.
     * @param maps              The set of ConstantMap-s to be combined with the
     *                          ConstantMap-s of this instance.
     * @return                  The union of ConstantMap-s.
     */
    protected ConstantMap[] combineConstantMaps(ConstantMapSet maps) {
        return ConstantMapSet.combine(constantMaps, maps).getConstantMaps();
    }

    /**
     * Fires a PropertyChangeEvent for the SCALAR_MAP_SET property with the
     * value of the SCALAR_MAP_SET at the time of the last firing as the old
     * value.  This method should be invoked by concrete subclasses after a
     * sequence of changes to this instance's set of ScalarMap-s.
     * @see #addScalarMaps(ScalarMapSet)
     * @see #addScalarMap(ScalarMap)
     * @see #removeScalarMap(ScalarMap)
     * @see #replaceScalarMap(ScalarMap oldMap, ScalarMap newMap)
     */
    protected void fireScalarMapSetChange() {
        ScalarMapSet previous;
        synchronized (this) {
            previous         = prevScalarMapSet;
            prevScalarMapSet = new ScalarMapSet(scalarMapSet);
        }

        firePropertyChange(SCALAR_MAP_SET, previous, scalarMapSet);
    }


    /**
     * A wrapper around LogUtil.logException
     *
     * @param msg  message for the exception
     * @param exc  exception that is being logged
     */
    public void logException(String msg, Exception exc) {
        LogUtil.logException(msg, exc);
    }


    /**
     * Sets the unit for use by the VisAD display subsystem.</p>
     *
     * @param unit                The display unit to use.
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setDisplayUnit(Unit unit)
            throws RemoteException, VisADException {
        displayUnit = unit;
    }

    /**
     * Returns the unit that is being used by the VisAD display subsystem for
     * rendering this instance or <code>null</code> if no such unit exists.</p>
     *
     *
     * @return                 The display unit or <code>null</code>.
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public Unit getDisplayUnit() throws RemoteException, VisADException {
        return displayUnit;
    }

    /**
     * A hook to allow sub-classes to set their color unit.
     *
     * @param unit  The color unit or <code>null</code>
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setColorUnit(Unit unit)
            throws RemoteException, VisADException {
        colorUnit = unit;
    }


    /**
     * Return the color {@link Unit}.
     *
     * @return  color unit
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public Unit getColorUnit() throws RemoteException, VisADException {
        return colorUnit;
    }

    /**
     * A no-op method so we can have a consistent api with the composite
     * pattern.  This method is overwritten in CompositeDisplayable, which
     * iterates through its children composite calling setColorPalette.
     * Set whether flow should be adjusted to earth coordinates
     *
     * @param adjust     true to adjust
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    public void setAdjustFlow(boolean adjust)
            throws RemoteException, VisADException {}



    /**
     *  Is the given unit compatible (i.e., is either null or
     *  can convert to the defaultUnit) with the the given real type.
     *
     * @param realType  RealType to check
     * @param unit      {@link Unit} in question
     * @return  true if the unit is compatible with the RealType
     */
    protected boolean isUnitCompatible(RealType realType, Unit unit) {
        if ((unit == null) || (realType == null)) {
            return true;
        }
        return Unit.canConvert(unit, realType.getDefaultUnit());
    }


    /**
     * Check if the given unit is compatible with the unit of the
     * given <code>realtype</code>.
     *  If not throw a VisADException.
     *
     * @param realType  RealType to check
     * @param unit      {@link Unit} in question
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    protected void checkUnit(RealType realType, Unit unit)
            throws VisADException, RemoteException {
        if ( !isUnitCompatible(realType, unit)) {
            throw new VisADException("Unit: " + unit
                                     + " not compatible with data units "
                                     + realType.getDefaultUnit());
        }
    }


    /**
     * If the displayUnit is not compatible with the unit of the given
     * {@link RealType} then clear the displayUnit and set the override
     * unit of the given {@link ScalarMap} to the default Unit of the
     * <code>realType</code>. Else set the override unit of the
     * ScalarMap to the displayUnit.
     *
     * @param map  ScalarMap to modify
     * @param realType  RealType of default unit
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    protected void applyDisplayUnit(ScalarMap map, RealType realType)
            throws VisADException, RemoteException {
        if ((map == null) || (realType == null)) {
            return;
        }
        if ( !isUnitCompatible(realType, displayUnit)) {
            displayUnit = null;
        }
        if (displayUnit == null) {
            //Do we want to do this?
            map.setOverrideUnit(realType.getDefaultUnit());
            return;
        }
        map.setOverrideUnit(displayUnit);
    }



    /**
     *  If the colorUnit is not compatible with the unit of the given real type
     *  then clear the colorUnit and set the override unit of the given
     *  ScalarMap to the default Unit of the realType. Else set the
     *  override unit of the ScalarMap to the colorUnit.
     *
     * @param map   ScalarMap to change
     * @param realType  RealType for default unit
     *
     * @throws RemoteException  Java RMI failure.
     * @throws VisADException   VisAD failure.
     */
    protected void applyColorUnit(ScalarMap map, RealType realType)
            throws VisADException, RemoteException {
        if ((map == null) || (realType == null)) {
            return;
        }

        if ( !isUnitCompatible(realType, colorUnit)) {
            colorUnit = null;
        }
        if (colorUnit == null) {
            //Do we want to do this?
            map.setOverrideUnit(realType.getDefaultUnit());
            return;
        }
        map.setOverrideUnit(colorUnit);
    }

    /**
     * <p>Returns the set of values for the given <code>aniType</code> if
     * the contained Data objects adapted by this {@link Displayable} have
     * any data of that type. <code>null</code> will be returned if
     * this instance adapts such an object but the object is unset, or if this
     * instance does not support this type.
     *
     * @param  aniType          The type used for animation
     * @param force             force the calculation
     * @return                  The set of times from all data
     *                          May be <code>null</code>.
     * @throws VisADException   if a VisAD failure occurs.
     * @throws RemoteException  if a Java RMI failure occurs.
     * @see #hasDataObject()
     */
    public Set getAnimationSet(RealType aniType, boolean force)
            throws VisADException, RemoteException {
        return animationSet;
    }


    /**
     * Set an alternate animation set
     *
     * @param set  the set
     */
    public void setOverrideAnimationSet(Set set) {
        animationSet = set;
    }


    /**
     * Set the animation set with a list of DateTimes
     *
     * @param times  the times
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    public void setOverrideAnimationSet(List times)
            throws RemoteException, VisADException {
        if ((times == null) || (times.size() == 0)) {
            animationSet = null;
        } else {
            animationSet = Util.makeTimeSet(times);
        }
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
        useTimesInAnimation = value;
        if (getDisplayMaster() != null) {
            getDisplayMaster().dataChange();
        }
    }

    /**
     * Get the UseTimesInAnimation property.
     *
     * @return The UseTimesInAnimation
     */
    public boolean getUseTimesInAnimation() {
        return useTimesInAnimation;
    }


}

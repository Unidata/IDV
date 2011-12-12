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


import ucar.unidata.util.Counter;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;

import ucar.visad.*;

import visad.*;

import visad.java3d.*;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;

import java.io.File;
import java.io.FileOutputStream;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.*;

import javax.swing.*;


/**
 * Manages a VisAD {@link visad.DisplayImpl} and a list of {@link Displayable}s.
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
 * <td>pointMode</td>
 * <td>boolean</td>
 * <td>set/is</td>
 * <td><code>false</code></td>
 * <td align=left>Whether or not the VisAD display associated with this
 * instance displays 1-D manifold data as points or lines.</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.132 $
 */
abstract public class DisplayMaster {

    /** Use this member to log messages (through calls to LogUtil) */
    public static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(DisplayMaster.class.getName());

    /**
     * The name of the "point mode" property.
     */
    public static String POINT_MODE = "pointMode";

    /**
     * Whether or not this instance has been destroyed.
     */
    private boolean isDestroyed = false;

    /**
     * The VisAD Display.
     */
    private DisplayImpl display;

    /**
     * The list of displayables.
     */
    private Displayables displayables;

    /**
     * The PropertyChangeListener-s.
     */
    private PropertyChangeSupport changeListeners;

    /**
     * The VetoableChangeListener-s.
     */
    private volatile VetoableChangeSupport vetoableListeners;

    /**
     * The Component into which this DisplayMaster renders.
     */
    private JPanel jPanel;

    /**
     * Whether or not this instance automatically rebuilds the display.
     */
    private boolean active = false;


    /** Keeps track of when we are in setActive */
    private boolean settingActive = false;


    /**
     * Has this display been initialized. Gets set by draw.
     */
    private boolean haveInitialized = false;

    /**
     * Keeps track of pending inactivate/reactivate calls
     */
    private int inactiveCount = 0;

    /** Used for synchronization around the inactive count */
    private Object INACTIVE_MUTEX = new Object();

    /**
     * The set of {@link ScalarMap}s of this instance.
     */
    private ScalarMaps myScalarMaps = new ScalarMaps();

    /**
     * The default
     */
    private double[] myAspect = { 1.0, 1.0, 1.0 };

    /** For offscreen rendering */
    private Dimension offscreenDimension;

    /** For offscreen rendering */
    private Component offscreenComponent;

    /** animation widget */
    private AnimationWidget animationWidget;

    /** The keyboard behavior */
    protected KeyboardBehavior behavior;



    /** The default mouse function map */
    public static final int[][][] defaultMouseFunctions =
        EventMap.IDV_MOUSE_FUNCTIONS;

    /** The default mouse  wheel function map */
    private int[][] wheelEventMap = EventMap.IDV_WHEEL_FUNCTIONS;


    /** maps the mouse function */
    private int[][][] mouseFunctionMap;

    /**
     * Set the mapping between mouse wheel event and function
     *
     * @param map The mapping
     */
    public void setWheelEventMap(int[][] map) {
        wheelEventMap = map;
    }





    /**
     * The set of {@link ScalarMaps} of the {@link Displayable}s.
     */
    private final SimpleBackedScalarMaps displayableScalarMaps =
        new SimpleBackedScalarMaps() {

        protected synchronized void populate() {

            for (Iterator iter = displayables.iterator(); iter.hasNext(); ) {
                set.add(((Displayable) iter.next()).getScalarMapSet());
            }

            isDirty = false;
        }
    };

    /**
     * The set of {@link ScalarMaps} actually in the VisAD display.
     */
    private final SimpleBackedScalarMaps displayScalarMaps =
        new SimpleBackedScalarMaps() {

        protected synchronized void populate() {

            set.add(DisplayMaster.this.display.getMapVector());

            isDirty = false;
        }
    };

    /**
     * The set of {@link ScalarMaps} of this instance and the {@link
     * Displayable}s.
     */
    private final BackedScalarMaps desiredScalarMaps =
        new BackedScalarMaps() {

        public boolean isDirty() {
            return displayableScalarMaps.isDirty() || myScalarMaps.isDirty();
        }

        protected synchronized void populate() {
            set.add(displayableScalarMaps.asScalarMapSet());
            set.add(myScalarMaps.asScalarMapSet());
        }
    };

    /** listener for the ScalarMap changes */
    private PropertyChangeListener displayableScalarMapsListener;

    /** listener for Display changes */
    private PropertyChangeListener displayableDisplayListener;

    /** displayable mapped to animation */
    private Animation animation = null;

    /** _more_ */
    static Counter counter = new Counter();

    /**
     * Parameterless ctor. Note: If you instantiate a DisplayMaster
     * through this constructor you must also call the init method.
     */
    public DisplayMaster() {
        //        counter.incr();
        //        System.err.println ("DisplayMaster.ctor:"  + counter);
    }



    /**
     * Constructs from a Display.
     *
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DisplayMaster(DisplayImpl display)
            throws VisADException, RemoteException {
        this(display, 1);
    }


    /**
     * Constructs from a VisAD display and an anticipated number of
     * {@link Displayable}s.
     *
     * @param display               The VisAD display.
     * @param initialCapacity       The anticipated number of Displayable-s.
     * @throws VisADException       VisAD failure.
     * @throws RemoteException      Java RMI failure.
     */
    public DisplayMaster(DisplayImpl display, int initialCapacity)
            throws VisADException, RemoteException {
        this(display, initialCapacity, null);
    }


    /**
     * Constructs from a VisAD display and an anticipated number of
     * {@link Displayable}s.
     *
     * @param display               The VisAD display.
     * @param initialCapacity       The anticipated number of Displayable-s.
     * @param offscreenDimension     Use this to set the dimension of the offscreen component
     * @throws VisADException       VisAD failure.
     * @throws RemoteException      Java RMI failure.
     */
    public DisplayMaster(DisplayImpl display, int initialCapacity,
                         Dimension offscreenDimension)
            throws VisADException, RemoteException {
        //        counter.incr();
        //        System.err.println ("DisplayMaster.ctor:"  + counter);
        setOffscreenDimension(offscreenDimension);
        init(display, initialCapacity);
    }



    /**
     * Initialize this display master
     *
     * @param display The display
     * @param initialCapacity Initial capacity of the displayables list
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void init(DisplayImpl display, int initialCapacity)
            throws VisADException, RemoteException {
        jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.X_AXIS));
        displayables = new Displayables(initialCapacity);
        this.display = display;
        //The component will be null when we are in offscreen mode
        Component displayComp = display.getComponent();
        if (displayComp != null) {
            jPanel.add(displayComp);
            //Add the mouse wheel listener
            displayComp.addMouseWheelListener(
                new java.awt.event.MouseWheelListener() {
                public void mouseWheelMoved(
                        java.awt.event.MouseWheelEvent e) {
                    handleMouseWheelMoved(e);
                }
            });
        } else {
            jPanel.add(getOffscreenComponent());
        }
        saveProjection();
        jPanel.setAlignmentX(0.5f);
        jPanel.setAlignmentY(0.5f);
        resetMouseFunctions();
        displayableScalarMapsListener = new PropertyChangeListener() {
            /*
             * Handles a change to a Displayable's SCALAR_MAP_SET property.
             */
            public void propertyChange(PropertyChangeEvent event) {
                try {
                    displayableScalarMaps.setDirty();
                    rebuildDisplay();
                } catch (Exception e) {
                    System.err.println(
                        getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to ScalarMap-s of "
                        + event.getSource() + ": " + e);
                    e.printStackTrace();
                }
            }

        };
        displayableDisplayListener = new PropertyChangeListener() {

            /*
             * Handles a change to a Displayable's DISPLAY property.  The
             * Displayable shall have already removed its DataReference-s
             * from this instance's display.
             */
            public void propertyChange(PropertyChangeEvent event) {

                if ((LocalDisplay) event.getNewValue()
                        != DisplayMaster.this.display) {
                    releaseDisplayable((Displayable) event.getSource());
                }
            }

        };
        changeListeners   = new PropertyChangeSupport(this);
        vetoableListeners = new VetoableChangeSupport(this);
    }





    /**
     * For offscreen rendering
     *
     * @param dim The screen dimension
     */
    protected void setOffscreenDimension(Dimension dim) {
        offscreenDimension = dim;
    }

    /**
     * Get the off screen dimension
     *
     * @return off screen dimension
     */
    protected Dimension getOffscreenDimension() {
        return offscreenDimension;
    }


    /**
     * Get the offscreen component sized using the offscreenDimension
     *
     * @return offscreen component
     */
    private Component getOffscreenComponent() {
        if (offscreenComponent == null) {
            offscreenComponent = new JPanel();
            if (offscreenDimension == null) {
                offscreenDimension = new Dimension(600, 400);
            }
            offscreenComponent.setSize(offscreenDimension);
        }
        return offscreenComponent;
    }


    /**
     * Returns the component of the display. If in offscreen mode
     * returns the offscreenComponent
     *
     * @return Display component
     */
    public Component getDisplayComponent() {
        Component comp = display.getComponent();
        if (comp == null) {
            comp = getOffscreenComponent();
        }
        return comp;
    }

    /**
     * Helper to get the screen bounds
     *
     * @return Bounds
     */
    public Rectangle getScreenBounds() {
        return getDisplayComponent().getBounds();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getDestroyed() {
        return isDestroyed;
    }


    /**
     * Destroys this instance, releasing any resources.  This method should be
     * invoked when this instance is no longer needed.  The client should not
     * try to use this instance after invoking this method.  Subclasses that
     * override this method should invoke <code>super.destroy()</code>.
     */
    public void destroy() {

        if (isDestroyed) {
            return;
        }
        isDestroyed = true;

        //        counter.decr();
        //        System.err.println ("DisplayMaster.destroy:"  + counter);

        /**
         * Empty the jPanel because sometimes this DisplayMaster does not
         * get gc'ed and its link to the jPanel ends up referring (transitively)
         * to many, many objects.
         */
        if (jPanel != null) {
            GuiUtils.invokeInSwingThread(new Runnable() {
                public void run() {
                    jPanel.removeAll();
                    if (jPanel.getParent() != null) {
                        jPanel.getParent().remove(jPanel);
                    }
                    jPanel = null;
                }
            });
        }

        /*
         * Remove links to this instance from this instance's Displayable-s.
         */
        for (Iterator iter = displayables.iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            displayable.removePropertyChangeListener(
                Displayable.SCALAR_MAP_SET, displayableScalarMapsListener);
            displayable.removePropertyChangeListener(Displayable.DISPLAY,
                    displayableDisplayListener);
            try {
                displayable.destroy();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }


        /*
         * Clear the Displayable and ScalarMap databases.
         */
        displayables.removeAll();
        myScalarMaps.removeAll();

        displayables                  = null;
        myScalarMaps                  = null;
        changeListeners               = null;
        vetoableListeners             = null;
        displayableScalarMapsListener = null;
        displayableDisplayListener    = null;

        try {
            display.destroy();
        } catch (Exception e) {
            System.err.println("DisplayMaster.destroy:");
            e.printStackTrace();
            //            throw new WrapperException("DisplayMaster.destroy", e);
            //e.printStackTrace();
        }

        display         = null;
        animationWidget = null;
    }




    /**
     * Finalizes this instance.
     *
     * @throws Throwable if an error occurs during finalization.
     */
    protected void finalize() throws Throwable {
        super.finalize();
    }


    /**
     * Gets the associated AWT Component.
     * @return                  The associated AWT Component.
     */
    public Component getComponent() {
        return jPanel;
    }

    /**
     * Tells the Display to retransform all data objects.
     *
     */
    protected synchronized void reDisplayAll() {
        if (display != null) {
            display.reDisplayAll();
        }
    }

    /**
     * Autoscale the axes of the display that have not had a range
     * set on them.
     */
    public void reScale() {
        if (display != null) {
            ((DisplayImpl) display).reAutoScale();
        }
    }

    /**
     * Rebuilds the display if appropriate.  If this instance is active and
     * the display has been marked for a future rebuild, then the display is
     * rebuilt.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void rebuildDisplay()
            throws VisADException, RemoteException {

        if ( !active) {
            return;
        }
        if ( !desiredScalarMaps.isDirty()) {
            addDataReferences();
        } else {
            // something has changed

            /*
             * Rebuilding the display is only necessary if a meaningful
             * change has occurred to the set of ScalarMap-s.  Test to see
             * if this is so.  First see if we are removing old maps.
             */
            ScalarMapSet oldMaps =
                ScalarMapSet.subtract(displayScalarMaps.asScalarMapSet(),
                                      desiredScalarMaps.asScalarMapSet());

            ScalarMapSet newMaps =
                ScalarMapSet.subtract(desiredScalarMaps.asScalarMapSet(),
                                      displayScalarMaps.asScalarMapSet());

            boolean rebuild = false;

            if (oldMaps.size() > 0) {
                if ( !removeOldScalarMaps(oldMaps)) {
                    rebuild = true;  // something failed in remove
                }
            }
            if ( !rebuild && (newMaps.size() > 0)) {
                /*
                 * The display logic for the addition of a ConstantMap
                 * differs from the addition of an actual Scalarmap and
                 * necessitates a complete rebuild of the display.  Hibbard
                 * might change this.  --SRE 20020228
                 */
                for (Iterator iter = newMaps.iterator(); iter.hasNext(); ) {
                    if (iter.next() instanceof ConstantMap) {
                        rebuild = true;
                        break;
                    }
                }

                if ( !rebuild && !addNewScalarMaps(newMaps)) {
                    rebuild = true;  // something failed in adding
                }
            }
            if (rebuild) {
                rebuild();
            } else {
                addDataReferences();
            }
            // System.out.println("DisplayMaster.rebuild(" + rebuild + "): " + display);
            myScalarMaps.setClean();
        }
    }

    /**
     * Rebuild the display when needed.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    protected synchronized void rebuild()
            throws VisADException, RemoteException {

        /*
         * The DisplayImpl.run() thread must be disabled to avoid a
         * deadlock while this thread modifies the display.
         */
        display.disableAction();
        removeDataReferences();
        removeScalarMaps();

        try {
            addScalarMaps();
            addDataReferences();
        } finally {
            display.enableAction();
        }
    }

    /**
     * Set the Animation for this DisplayMaster
     *
     * @param animation  Animation object
     * @param animationWidget    the associated widget
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setAnimation(Animation animation,
                             AnimationWidget animationWidget)
            throws VisADException, RemoteException {
        addDisplayable(animation);
        this.animationWidget = animationWidget;
    }

    /**
     * Returns the associated VisAD display.
     * @return          The VisAD display.
     */
    public final LocalDisplay getDisplay() {
        return (LocalDisplay) display;
    }

    /**
     * Returns the number of Displayable-s.
     * @return          The number of Displayable-s.
     */
    public final int getDisplayableCount() {
        return displayables.size();
    }

    /**
     * Adds a Displayable to the Displayable-s managed by this instance.
     * @param displayable               The Displayable to be added.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public final synchronized void addDisplayable(Displayable displayable)
            throws RemoteException, VisADException {
        setDisplayables(getDisplayableCount(), displayable);
    }

    /**
     * Sets the Displayable managed by this instance at a given point in the
     * list of Displayable-s.
     * @param index             The position in the list of Displayable-s.
     * @param displayable       The Displayable to be in the given position.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized final void setDisplayables(int index,
            Displayable displayable)
            throws VisADException, RemoteException {

        setDisplayInactive();

        if ((index >= 0) && (index < getDisplayableCount())) {
            Displayable prev = displayables.get(index);
            if (prev != null) {
                removeDisplayable(prev);
                //??                prev.destroy();
            }
        }

        displayable.setDisplayMaster(this);
        if (displayable instanceof Animation) {
            animation = (Animation) displayable;
        }
        displayables.add(index, displayable);
        displayable.setDisplay(getDisplay());
        displayable.addPropertyChangeListener(Displayable.SCALAR_MAP_SET,
                displayableScalarMapsListener);
        displayable.addPropertyChangeListener(Displayable.DISPLAY,
                displayableDisplayListener);

        if (displayable.getScalarMapSet().size() != 0) {
            displayableScalarMaps.setDirty();
        }

        setDisplayActive();


        /*
         * NB: Moving the following statement to before the above "setActive"
         * statement causes unnecessary recomputation of the display.
         */
        if (active) {
            displayable.addDataReferences();
        }
    }

    /**
     * Sets the {@link Displayable}s managed by this instance.
     *
     * @param displayables      The Displayable-s to be managed by this
     *                          instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized final void setDisplayables(Displayable[] displayables)
            throws VisADException, RemoteException {

        setDisplayInactive();
        for (int i = 0; i < displayables.length; ++i) {
            setDisplayables(i, displayables[i]);
        }
        setDisplayActive();
    }

    /**
     * Removes a Displayable from this instance.  Invokes the Displayable's
     * <code>removeDataReferences()</code> method if and only if the
     * Displayable is in this instance's list of Displayable-s.  NOTE: If the
     * Displayable is in this instance's list of Displayable-s, then this
     * instance must be active or be made active before the Displayable is
     * can be successfully added to another display.  The display might
     * be rebuilt or marked for a rebuild.
     *
     * @param displayable      The Displayable to be removed.
     * @return                 <code>true</code> if and only if the Displayable
     *                         was in this instance's list of Displayable-s.
     * @throws VisADException  if an error occurs in core VisAD
     * @throws RemoteException if a Java RMI failure occurs.
     * @see Displayable#removeDataReferences
     */
    public synchronized boolean removeDisplayable(Displayable displayable)
            throws VisADException, RemoteException {

        if (displayable == null) {
            return false;
        }

        boolean existed = releaseDisplayable(displayable);

        if (existed) {
            displayable.removeDataReferences();

            ScalarMapSet maps = displayable.getScalarMapSet();

            if (maps.size() > 0) {
                if (displayable instanceof Animation) {
                    animation = null;
                }
                displayableScalarMaps.setDirty();
                rebuildDisplay();
            }
        }

        return existed;
    }

    /**
     * Removes all Displayables from this instance.
     *
     * @see #removeDisplayable
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void removeDisplayables()
            throws VisADException, RemoteException {
        setDisplayInactive();

        /*
         * NB: reverse order to avoid problems with iterating over elements
         * that are being removed.
         */
        for (int i = displayables.size(); --i >= 0; ) {
            removeDisplayable(displayables.get(i));
        }
        setDisplayActive();
    }

    /**
     * Sets the background color of this VisAD display.
     * @param color  a Java Color to become the background color.
     */
    public void setBackground(Color color) {
        try {
            float[] rgb = color.getRGBComponents(null);

            getDisplay().getDisplayRenderer().getRendererControl()
                .setBackgroundColor(rgb[0], rgb[1], rgb[2]);
        } catch (VisADException ve) {}
        catch (RemoteException re) {}
    }

    /*
    public void test() {
        DisplayRenderer displayRenderer  = getDisplay().getDisplayRenderer();
        BranchGroup root = ((DisplayRendererJ3D)displayRenderer).getRoot();
        }*/



    /**
     * Returns the background color being used
     * @return  color being used or null if it couldn't be determined
     */
    public Color getBackground() {

        float[] rgb =
            getDisplay().getDisplayRenderer().getRendererControl()
                .getBackgroundColor();

        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Returns the foreground color being used for the cursor and box
     *
     * @return  color being used or null if it couldn't be determined
     */
    public Color getForeground() {

        float[] rgb =
            getDisplay().getDisplayRenderer().getRendererControl()
                .getCursorColor();

        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Sets the "foreground" color of this VisAD display
     *
     * @param color  color to use
     */
    public void setForeground(Color color) {

        try {
            float[] rgb = color.getRGBComponents(null);

            getDisplay().getDisplayRenderer().getRendererControl()
                .setForegroundColor(rgb[0], rgb[1], rgb[2]);
        } catch (VisADException ve) {}
        catch (RemoteException re) {}
    }

    /**
     * Returns the {@link Displayable} at a given position in the list of
     * {@link Displayable}s.
     *
     * @param index             The position in the list to get the Displayable.
     * @return                  The Displayable at the given position.
     */
    public final Displayable getDisplayables(int index) {
        return displayables.get(index);
    }

    /**
     * Returns the {@link Displayable}s of this instance as an array.
     *
     * @return                  The array of Displayable-s.
     */
    public final Displayable[] getDisplayables() {
        return displayables.toArray();
    }

    /**
     * Returns the index of a particular {@link Displayable}.
     *
     * @param displayable The Displayable to look for.
     * @return            The index of the Displayable in this composite or -1
     *                    if this CompositeDisplayable does not contain this
     *                    Displayable.
     */
    public int indexOf(Displayable displayable) {
        return displayables.indexOf(displayable);
    }

    /**
     * Sets the point-mode of the VisAD display.
     *
     * @param usePoints         True if the display should use points rather
     *                          than connected line segments.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setPointMode(boolean usePoints)
            throws VisADException, RemoteException {

        GraphicsModeControl gmc          = display.getGraphicsModeControl();
        Boolean             oldPointMode = new Boolean(gmc.getPointMode());

        gmc.setPointMode(usePoints);
        firePropertyChange(POINT_MODE, oldPointMode, new Boolean(usePoints));
    }

    /**
     * Gets the point-mode of the VisAD display.
     *
     * @return                  True if the display is using points rather
     *                          than connected line segments.
     */
    public boolean isPointMode() {

        return display.getGraphicsModeControl().getPointMode();
    }

    /**
     * Saves the current display projection.  The projection may later be
     * restored by the method <code>resetProjection()</code>.
     *
     * @see #resetProjection()
     */
    public void saveProjection() {
        display.getProjectionControl().saveProjection();
    }

    /**
     * Sets the display aspect ratio.  The argument is passed unaltered to
     * {@link visad.ProjectionControl#setAspect(double[])}.
     *
     * @param newAspect              The new aspect ratio.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public void setDisplayAspect(double[] newAspect)
            throws VisADException, RemoteException {
        //Change the aspect ratio array to size 2 if we are in 2d
        if ( !is3D()) {
            newAspect = new double[] { newAspect[0], newAspect[1] };
        }
        display.getProjectionControl().setAspect(newAspect);
        myAspect = newAspect;
    }


    /**
     * Are we in 3D. This is rudimentary and just checks the type
     * of the display
     *
     * @return is the display 3D
     */
    protected boolean is3D() {
        return (display instanceof DisplayImplJ3D);
    }



    /**
     * Gets the current display aspect.
     *
     * @return                       The current display aspect ratio.
     */
    public double[] getDisplayAspect() {
        return myAspect;
    }

    /**
     * Gets the current display projection.  The object returned from {@link
     * visad.ProjectionControl#getMatrix()} is returned.
     *
     * @return                       The current display projection.
     */
    public double[] getProjectionMatrix() {
        return display.getProjectionControl().getMatrix();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getScale() {
        double[] currentMatrix = getProjectionMatrix();
        double[] trans         = { 0.0, 0.0, 0.0 };
        double[] rot           = { 0.0, 0.0, 0.0 };
        double[] scale         = { 0.0, 0.0, 0.0 };
        getMouseBehavior().instance_unmake_matrix(rot, scale, trans,
                currentMatrix);

        return scale[0];
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double[] getRotation() {
        double[] currentMatrix = getProjectionMatrix();
        double[] trans         = { 0.0, 0.0, 0.0 };
        double[] rot           = { 0.0, 0.0, 0.0 };
        double[] scale         = { 0.0, 0.0, 0.0 };
        getMouseBehavior().instance_unmake_matrix(rot, scale, trans,
                currentMatrix);

        return rot;
    }


    /**
     * Sets the current display projection.  The argument is passed, unaltered,
     * to {@link visad.ProjectionControl#setMatrix(double[])}.
     *
     * @param newMatrix              The new projection matrix.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public void setProjectionMatrix(double[] newMatrix)
            throws VisADException, RemoteException {
        //        System.err.print ("DisplayMaster.setProjectionMatrix ");
        //        for(int i=0;i<newMatrix.length;i++) 
        //            System.err.print(" " + newMatrix[i]);
        //        System.err.println(" ");
        display.getProjectionControl().setMatrix(newMatrix);
    }

    /**
     * Returns the saved projection Matrix.  The object returned from {@link
     * visad.ProjectionControl#getSavedProjectionMatrix()} is returned.
     *
     * @return                       The saved projection matrix.
     */
    public double[] getSavedProjectionMatrix() {
        return display.getProjectionControl().getSavedProjectionMatrix();
    }

    /**
     * Restores to projection at time of last <code>saveProjection()</code>
     * call -- if one was made -- or to initial projection otherwise.
     *
     * @see #saveProjection()
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void resetProjection() throws VisADException, RemoteException {
        display.getProjectionControl().resetProjection();
    }

    /**
     * <p>Adds a {@link visad.KeyboardBehavior} to the display that this
     * DisplayMaster manages.</p>
     *
     * <p>This implementation does nothing but check to see if this instance
     * has been destroyed.</p>
     *
     * @param behavior               The keyboard behavior to be added.
     */
    public void addKeyboardBehavior(KeyboardBehavior behavior) {}



    /**
     * Set the keyboard behavior to use
     *
     * @param behavior  the keyboard behavior
     */
    protected void setKeyboardBehavior(KeyboardBehavior behavior) {
        this.behavior = behavior;
    }


    /**
     * Set the key to function map on the current keyboard behavior
     *
     * @param map the map
     */
    public void setKeyboardEventMap(int[][] map) {
        setKeyboardEventMap(map, behavior);
    }


    /**
     * Set the key to function map on the given behavior
     *
     * @param map the map
     * @param behavior behavior to set
     */
    public static void setKeyboardEventMap(int[][] map,
                                           KeyboardBehavior behavior) {
        if (behavior == null) {
            return;
        }
        for (int i = 0; i < map.length; i++) {
            int[] settings = map[i];
            behavior.mapKeyToFunction(settings[0], settings[1], settings[2]);
        }
    }





    /**
     * Set the mouse functions for this display.
     * @param map  array of mouse functions to buttons
     * @see visad.MouseHelper#setFunctionMap(int[][][]) for info.
     *
     * @throws VisADException
     */
    public void setMouseFunctions(int[][][] map) throws VisADException {
        mouseFunctionMap = map;
        getMouseBehavior().getMouseHelper().setFunctionMap(map);
    }


    /**
     * mouse funtion map
     *
     * @return mouse funtion map
     */
    public int[][][] getMouseFunctionMap() {
        return mouseFunctionMap;
    }

    /**
     * Get the current mouse behavior
     *
     * @return mouse behavior
     */
    public MouseBehavior getMouseBehavior() {
        return getDisplay().getDisplayRenderer().getMouseBehavior();
    }


    /**
     * rotate some angle
     *
     * @param  angle rotate angle
     */
    public void rotateX(double angle) {
        rotate(angle, 0.0, 0.0);
    }


    /**
     * rotate some angle
     *
     * @param  angle rotate angle
     */
    public void rotateY(double angle) {
        rotate(0.0, angle, 0.0);
    }



    /**
     * rotate some angle
     *
     * @param  angle rotate angle
     */
    public void rotateZ(double angle) {
        rotate(0.0, 0.0, angle);
    }


    /**
     * Handle when the mouse scroll wheel has been moved
     *
     * @param e event
     */
    protected void handleMouseWheelMoved(java.awt.event.MouseWheelEvent e) {

        int    rot     = e.getWheelRotation();
        double degrees = 2.0;
        int    control = e.isControlDown()
                         ? 1
                         : 0;
        int    shift   = e.isShiftDown()
                         ? 1
                         : 0;
        int    func    = wheelEventMap[control][shift];
        if (func == EventMap.WHEEL_ROTATEZ) {
            if (rot < 0) {
                rotateZ(degrees);
            } else {
                rotateZ(-degrees);
            }
        } else if (func == EventMap.WHEEL_ROTATEX) {
            if (rot < 0) {
                rotateX(degrees);
            } else {
                rotateX(-degrees);
            }
        } else if (func == EventMap.WHEEL_ROTATEY) {
            if (rot < 0) {
                rotateY(degrees);
            } else {
                rotateY(-degrees);
            }
        } else if (func == EventMap.WHEEL_ZOOMIN) {
            if (rot < 0) {
                zoom(0.9);
            } else {
                zoom(1.1);
            }
        } else if (func == EventMap.WHEEL_ZOOMOUT) {
            if (rot < 0) {
                zoom(1.1);
            } else {
                zoom(0.9);
            }

        }
    }




    /**
     * Zoom in on the display
     *
     * @param  factor  zoom factor
     *                 ( > 1 = zoom in, 1 > zoom > 0 =  zoom out).  using
     *                 2.0 and .5 seems to work well.
     */
    public void zoom(double factor) {
        zoom(factor, factor, factor);
    }




    /**
     * Zoom in on the display
     *
     * @param  xfactor  x zoom factor
     * @param  yfactor  y zoom factor
     * @param  zfactor  z zoom factor
     *
     * ( > 1 = zoom in, 1 > zoom > 0 =  zoom out).  using
     * 2.0 and .5 seems to work well.
     */
    public void zoom(double xfactor, double yfactor, double zfactor) {
        double[] scaleMatrix = getMouseBehavior().make_matrix(0.0, 0.0, 0.0,
                                   xfactor, yfactor, zfactor, 0.0, 0.0, 0.0);
        double[] currentMatrix = getProjectionMatrix();
        scaleMatrix = getMouseBehavior().multiply_matrix(scaleMatrix,
                currentMatrix);

        try {
            setProjectionMatrix(scaleMatrix);
            //      setProjectionMatrix(xscaleMatrix);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Get the scaling factor for probes and such. The scaling is
     * the parameter that gets passed to TextControl.setSize() and
     * ShapeControl.setScale().
     *
     * @return ratio of the current matrix scale factor to the
     *         saved matrix scale factor.
     */
    public float getDisplayScale() {
        if (display != null) {
            //jeffmc:
            //            if(true) return (float)getScale();
            ProjectionControl pc          = display.getProjectionControl();
            double[]          init_matrix = pc.getSavedProjectionMatrix();
            double[]          rot_a       = new double[3];
            double[]          trans_a     = new double[3];
            double[]          scale_a     = new double[1];
            MouseBehavior     mouse       = display.getMouseBehavior();
            mouse.instance_unmake_matrix(rot_a, scale_a, trans_a,
                                         init_matrix);
            double init_zoom = scale_a[0];
            //System.out.println("initial zoom = " + init_zoom);
            double[] matrix = pc.getMatrix();
            mouse.instance_unmake_matrix(rot_a, scale_a, trans_a, matrix);
            //System.out.println("Current zoom = " + scale_a[0]);
            return ((float) (init_zoom / scale_a[0]));
        }
        return 1.0f;
    }


    /**
     *  Translate (X,Y position) of the display
     *
     *  @param  xFactor   X translation factor
     *  @param  yFactor   Y translation factor
     */
    public void translate(double xFactor, double yFactor) {
        try {
            double[] currentMatrix = getProjectionMatrix();
            double[] translateMatrix =
                getMouseBehavior().make_translate(xFactor, yFactor);
            translateMatrix =
                getMouseBehavior().multiply_matrix(translateMatrix,
                    currentMatrix);
            setProjectionMatrix(translateMatrix);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    /**
     * rotate some angle
     *
     * @param  anglex rotate angle
     * @param  angley rotate angle
     * @param  anglez rotate angle
     */
    public void rotate(double anglex, double angley, double anglez) {
        double[] t1 = getMouseBehavior().make_matrix(anglex, angley, anglez,
                          1.0, 0.0, 0.0, 0.0);
        double[] currentMatrix = getProjectionMatrix();
        t1 = getMouseBehavior().multiply_matrix(t1, currentMatrix);

        try {
            setProjectionMatrix(t1);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }





    /**
     * Reset the mouse functions to the default.
     *
     * @throws VisADException
     */
    public void resetMouseFunctions() throws VisADException {
        setDefaultMouseFunctions(display);
    }



    /**
     *  A general utility method that sets the default mouse functions
     *  on the given display.
     *
     * @param display  Display to set functions for
     *
     * @throws VisADException
     */
    public static void setDefaultMouseFunctions(DisplayImpl display)
            throws VisADException {
        display.getDisplayRenderer().getMouseBehavior().getMouseHelper()
            .setFunctionMap(defaultMouseFunctions);
    }

    /**
     * Toggle the "Please wait.." string visibility.
     * @param visible  true to make it visible
     */
    public void setWaitMessageVisible(boolean visible) {
        ((DisplayRenderer) getDisplay().getDisplayRenderer())
            .setWaitMessageVisible(visible);
    }

    /**
     * Get the state of the "Please wait.." string visibility.
     * @return true if visible
     */
    public boolean getWaitMessageVisible() {
        return ((DisplayRenderer) getDisplay().getDisplayRenderer())
            .getWaitMessageVisible();
    }

    /**
     * Toggle the animation string visibility.
     * @param visible  true to make it visible
     */
    public void setAnimationStringVisible(boolean visible) {
        ((DisplayRenderer) getDisplay().getDisplayRenderer())
            .setAnimationStringVisible(visible);
    }

    /**
     * Return whether the animation string is visible or not
     * @return  true if visible
     */
    public boolean getAnimationStringVisible() {
        return ((DisplayRenderer) getDisplay().getDisplayRenderer())
            .getAnimationStringVisible();
    }

    /**
     * Determine if this MapDisplay can do stereo.  Subclasses that support
     * this should override this method
     * @return false (unless overriden)
     */
    public boolean getStereoAvailable() {
        return false;
    }

    /**
     * Set the eye position of each eye for a stereo view. Subclasses that
     * support this should override this method
     *
     * @param position  x position of each eye (left negative, right positive).
     */
    public void setEyePosition(double position) {}


    /**
     * Adds a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to add.
     */
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        vetoableListeners.addVetoableChangeListener(listener);
    }

    /**
     * Adds a named VetoableChangeListener.
     *
     * @param name              The name of the property.
     * @param listener          The VetoableChangeListener to add.
     */
    public void addVetoableChangeListener(String name,
                                          VetoableChangeListener listener) {
        vetoableListeners.addVetoableChangeListener(name, listener);
    }

    /**
     * Removes a VetoableChangeListener.
     *
     * @param listener          The VetoableChangeListener to be removed.
     */
    public void removeVetoableChangeListener(
            VetoableChangeListener listener) {

        vetoableListeners.removeVetoableChangeListener(listener);
    }

    /**
     * Removes a named VetoableChangeListener.
     *
     * @param name              The name of the property.
     * @param listener          The VetoableChangeListener to be removed.
     */
    public void removeVetoableChangeListener(
            String name, VetoableChangeListener listener) {
        vetoableListeners.removeVetoableChangeListener(name, listener);
    }

    /**
     * Adds a PropertyChangeListener.
     *
     * @param listener          The PropertyChangeListener to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeListeners.addPropertyChangeListener(listener);
    }

    /**
     * Adds a named PropertyChangeListener.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to add.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {
        changeListeners.addPropertyChangeListener(name, listener);
    }

    /**
     * Removes a PropertyChangeListener.
     *
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {

        changeListeners.removePropertyChangeListener(listener);
    }

    /**
     * Removes a named PropertyChangeListener.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            String name, PropertyChangeListener listener) {

        changeListeners.removePropertyChangeListener(name, listener);
    }

    /**
     * Causes the Displayable-s managed by this instance to be rendered to the
     * VisAD display.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void draw() throws VisADException, RemoteException {
        haveInitialized = true;
        setActive(true);
    }

    /**
     * Indicates if this instance if semantically identical to another object.
     *
     * @param obj               The other object.
     * @return          <code>true</code> if and only if this instance is
     *                  semantically identical to <code>obj</code>.
     */
    public synchronized boolean equals(Object obj) {


        boolean equals;

        if ( !(obj instanceof DisplayMaster)) {
            equals = false;
        } else {
            DisplayMaster that = (DisplayMaster) obj;

            synchronized (that) {
                equals = (this == that) || (display.equals(
                    that.display) && displayables.equals(
                    that.displayables) && (changeListeners.equals(
                        that.changeListeners)) && ((vetoableListeners.equals(
                            that.vetoableListeners)) && myScalarMaps.equals(
                                that.myScalarMaps)));
            }
        }

        return equals;
    }

    /**
     * Gets the hash-code of this instance.
     *
     * @return          The hash-code of this instance.
     */
    public synchronized int hashCode() {

        return display.hashCode() ^ displayables.hashCode()
               ^ (changeListeners.hashCode())
               ^ (vetoableListeners.hashCode());
    }

    /**
     * Adds a VisAD {@link visad.DisplayListener} to this instance's {@link
     * visad.Display}.
     *
     * @param listener          The VisAD DisplayListener to be added.
     */
    public void addDisplayListener(DisplayListener listener) {
        getDisplay().addDisplayListener(listener);
    }

    /**
     * Fires a PropertyChangeEvent.
     *
     * @param event                     The PropertyChangeEvent to be fired.
     */
    protected void firePropertyChange(PropertyChangeEvent event) {

        changeListeners.firePropertyChange(event);
    }

    /**
     * Fires a PropertyChangeEvent.
     *
     * @param propertyName              The name of the property.
     * @param oldValue                  The old value of the property.
     * @param newValue                  The new Value of the property.
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {

        changeListeners.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Removes all DataReference-s from the VisAD display.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private synchronized void removeDataReferences()
            throws VisADException, RemoteException {

        for (int i = displayables.size(); --i >= 0; ) {
            ((Displayable) displayables.get(i)).removeDataReferences();
        }
    }

    /**
     * Removes all {@link visad.ScalarMap}s from the VisAD display.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private synchronized void removeScalarMaps()
            throws VisADException, RemoteException {

        display.clearMaps();

        if (displayScalarMaps.size() != 0) {
            displayScalarMaps.setDirty();
        }
    }

    /**
     * <p>Adds a {@link visad.ScalarMap} to this instance.  The display might
     * be rebuilt or marked for a rebuild.</p>
     *
     * <p>This implementation invokes {@link #addScalarMaps(ScalarMapSet)} with
     * the given {@link visad.ScalarMap} as the only member of the set.</p>
     *
     * @param map               The ScalarMap to add.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void addScalarMap(ScalarMap map)
            throws VisADException, RemoteException {

        ScalarMapSet set = new ScalarMapSet();

        set.add(map);
        addScalarMaps(set);
    }

    /**
     * Adds {@link visad.ScalarMap}s to this instance.
     *
     * @param mapSet            The ScalarMap-s to add.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void addScalarMaps(ScalarMapSet mapSet)
            throws VisADException, RemoteException {
        myScalarMaps.add(mapSet);
        rebuildDisplay();
    }

    /**
     * Adds new {@link visad.ScalarMap}s to the display.
     *
     * @param mapSet            The ScalarMap-s to add.
     * @return                  True if and only if the ScalarMap-s were
     *                          successfully added.
     * @throws VisADException   VisAD failure.  A {@link visad.ScalarMap} in
     *                          the set had, possibly, already been added to the
     *                          display.
     * @throws RemoteException  Java RMI failure.
     */
    private synchronized boolean addNewScalarMaps(ScalarMapSet mapSet)
            throws VisADException, RemoteException {

        boolean success = false;

        if (mapSet.size() == 0) {
            success = true;

        } else {
            setDisplayInactive();
            try {
                for (Iterator mapIter =
                        mapSet.iterator(); mapIter.hasNext(); ) {

                    display.addMap((ScalarMap) mapIter.next());
                    displayScalarMaps.setDirty();
                }

                success = true;

            } catch (DisplayException ex) {
                System.err.println(ex);
                success = false;
            } finally {
                setDisplayActive();
            }

        }

        return success;
    }

    /**
     * Removes old {@link visad.ScalarMap}s from the display.
     *
     * @param mapSet            The ScalarMap-s to remove.
     * @return                  True if and only if the ScalarMap-s were
     *                          successfully removed.
     * @throws VisADException   VisAD failure.  A {@link visad.ScalarMap} in
     *                          the set had, possibly, not been in the display.
     * @throws RemoteException  Java RMI failure.
     */
    private synchronized boolean removeOldScalarMaps(ScalarMapSet mapSet)
            throws VisADException, RemoteException {

        boolean success = false;

        if (mapSet.size() == 0) {
            success = true;

        } else {
            setDisplayInactive();
            try {
                for (Iterator mapIter =
                        mapSet.iterator(); mapIter.hasNext(); ) {

                    display.removeMap((ScalarMap) mapIter.next());
                    displayScalarMaps.setDirty();
                }

                success = true;

            } catch (DisplayException ex) {
                System.err.println(ex);
                success = false;
            } finally {
                setDisplayActive();
            }

        }

        return success;
    }

    /**
     * Removes given {@link visad.ScalarMap}s from this instance.  The display
     * might be rebuilt or marked for a rebuild.
     *
     * @param mapSet            The ScalarMap-s to remove.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void removeScalarMaps(ScalarMapSet mapSet)
            throws VisADException, RemoteException {
        myScalarMaps.remove(mapSet);
        rebuildDisplay();
    }

    /**
     * Removes a {@link visad.ScalarMap} from this instance.  The display
     * might be rebuilt or marked for a rebuild. <code>true</code> is returned
     * if and only if the {@link visad.ScalarMap} had been added via a previous
     * {@link #addScalarMap(ScalarMap)} or {@link #addScalarMaps(ScalarMapSet)}.
     *
     * @param map               The ScalarMap to remove.
     * @return                  <code>true</code> if the ScalarMap existed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized boolean removeScalarMap(ScalarMap map)
            throws VisADException, RemoteException {

        boolean existed = myScalarMaps.remove(map);

        rebuildDisplay();

        return existed;
    }

    /**
     * Replaces a {@link visad.ScalarMap} in this instance.  The display
     * might be rebuilt or marked for a rebuild.
     *
     * @param oldMap            The ScalarMap to remove.
     * @param newMap            The ScalarMap to add.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void replaceScalarMap(ScalarMap oldMap,
            ScalarMap newMap)
            throws VisADException, RemoteException {

        setDisplayInactive();
        removeScalarMap(oldMap);
        addScalarMap(newMap);
        setDisplayActive();
    }

    /**
     * Adds all desired {@link visad.ScalarMap}s to the VisAD display.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private synchronized void addScalarMaps()
            throws VisADException, RemoteException {

        if (desiredScalarMaps.size() > 0) {
            setDisplayInactive();
            for (Iterator iter =
                    desiredScalarMaps.iterator(); iter.hasNext(); ) {
                display.addMap((ScalarMap) iter.next());
            }

            myScalarMaps.setClean();
            displayScalarMaps.setDirty();
            setDisplayActive();
        }
    }

    /**
     * Adds the {@link visad.DataReference}s of the {@link Displayable}s to the
     * VisAD display.  Only those {@link Displayable}s that have not already
     * been added to the display will be added.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private synchronized void addDataReferences()
            throws VisADException, RemoteException {

        for (Iterator iter = displayables.iterator(); iter.hasNext(); ) {
            ((Displayable) iter.next()).addDataReferences();
        }
    }

    /**
     * Sets the "rebuild necessary" property.
     *
     * @deprecated
     */
    public synchronized void setRebuildNecessary() {}

    /**
     * Indicates if this instance is active (i.e will immediately rebuild the
     * display when appropriate).
     *
     * @return                  <code>true</code> if and only if the display
     *                          master is active.
     */
    public synchronized boolean isActive() {
        return active;
    }



    /** count */
    static int cnt = 0;

    /** my count */
    int myCnt = cnt++;

    /**
     * Flag for tracing
     *
     * @return true to trace
     */
    private boolean doTrace() {
        return (myCnt == 0) && false;
    }

    /**
     * Print out a message
     */
    public void printMe() {
        System.err.println(myCnt + " active?" + active + " cnt="
                           + inactiveCount);
    }

    /**
     * Sets this instance  inactive (ie: that it will not
     * automatically rebuild the display when appropriate).
     *
     * <p>This implementation invokes {@link #setActive(boolean)}.</p>
     *
     */
    public void setDisplayInactive() {
        try {
            boolean shouldCall = false;
            synchronized (INACTIVE_MUTEX) {
                if (settingActive) {
                    if (doTrace()) {
                        Trace.msg(
                            myCnt
                            + " ***** DisplayMaster.setDisplayActive - is setting active");
                    }
                    return;
                }
                if (doTrace()) {
                    Trace.call1(myCnt + " DisplayMaster.setDisplayInActive",
                                " count=" + inactiveCount);
                    Misc.printStack("setDisplayInactive", 4, null);
                }
                inactiveCount++;
                shouldCall = (inactiveCount == 1);
            }
            if (shouldCall) {
                setActive(false);
            }
            if (doTrace()) {
                Trace.call2(myCnt + " DisplayMaster.setDisplayInActive",
                            " count=" + inactiveCount);
            }
        } catch (Exception ex) {}
    }



    /**
     * Activate the display if there is one or fewer pending inactive calls
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setDisplayActive() throws RemoteException, VisADException {
        boolean shouldCall = false;
        synchronized (INACTIVE_MUTEX) {
            if (settingActive) {
                if (doTrace()) {
                    Trace.msg(
                        myCnt
                        + " ***** DisplayMaster.setDisplayActive -  is settingActive");
                }
                return;
            }
            if (doTrace()) {
                Trace.call1(myCnt + " DisplayMaster.setDisplayActive",
                            " count=" + inactiveCount);
                Misc.printStack("setDisplayActive", 4, null);
            }
            inactiveCount = Math.max(0, inactiveCount - 1);
            shouldCall    = (inactiveCount == 0);
        }
        if (shouldCall) {
            setActive(true);
        }
        if (doTrace()) {
            Trace.call2(myCnt + " DisplayMaster.setDisplayActive",
                        " count=" + inactiveCount);
        }
    }


    /**
     * Determines whether or not this instance will automatically rebuild the
     * display when appropriate.  If the argument is <code>true</code> and the
     * display has been marked for a rebuild, then the display will be rebuilt.
     * This method is idempotent.
     *
     * @param newActiveValue               <code>true</code> will cause an immediate
     *                          rebuld if appropriate and ensure future,
     *                          automatic rebuilds.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @deprecated This will become private in the future. You should use
     * {@link #setDisplayInactive()} and {@link #setDisplayActive()}
     */
    public void setActive(boolean newActiveValue)
            throws RemoteException, VisADException {
        synchronized (INACTIVE_MUTEX) {
            if (active == newActiveValue) {
                return;
            }
            //If we are in here already then return
            if (settingActive) {
                return;
            }
            settingActive = true;
            if (newActiveValue) {
                inactiveCount = 0;
            }
            if (haveInitialized) {
                active = newActiveValue;
            }
        }
        if (haveInitialized) {
            if (active) {
                rebuildDisplay();
            }
            display.setEnabled(active);
        }
        settingActive = false;
    }


    /**
     * Ensures that this instance is inactive (ie: that it will not
     * automatically rebuild the display when appropriate).
     *
     * <p>This implementation invokes {@link #setActive(boolean)}.</p>
     *
     * @return                  The previous value of the "active" property.
     * @deprecated This will become private in the future. You should use
     * {@link #setDisplayInactive()} and {@link #setDisplayActive()}
     */
    public synchronized boolean ensureInactive() {
        boolean prev = active;
        try {
            setActive(false);
        } catch (Exception ex) {}  // can't happen for setActive(false)
        return prev;
    }




    /**
     * Releases a Displayable from this instance.  Does not invoke the
     * Displayable's <code>removeDataReferences()</code> method.
     *
     * @param displayable       The Displayable to be removed.
     * @return                  <code>true</code> if and only if the Displayable
     *                          was in this instance's list of Displayable-s.
     */
    private synchronized boolean releaseDisplayable(Displayable displayable) {

        ScalarMapSet scalarMapSet = displayable.getScalarMapSet();
        if ((scalarMapSet != null) && (scalarMapSet.size() > 0)) {
            displayableScalarMaps.setDirty();
        }
        displayable.removePropertyChangeListener(Displayable.SCALAR_MAP_SET,
                displayableScalarMapsListener);
        displayable.removePropertyChangeListener(Displayable.DISPLAY,
                displayableDisplayListener);

        return displayables.remove(displayable);
    }

    /**
     * <p>Capture the display's current image and save it to a file as
     * an image (e.g., JPEG, png). No blocking occurs and the currently
     * rendered display is captured.</p>
     *
     * <p>This implementation simply calls {@link #saveCurrentDisplay(File,
     * boolean, boolean)} with <code>doSync=false</code>.</p>
     *
     * @param  toFile  The file to which to save the current image.
     */
    public void saveCurrentDisplay(File toFile) {
        saveCurrentDisplay(toFile, false, false);
    }




    /**
     * Collect the animation set that is the union of all data
     *
     * @return Set of animation samplings
     *
     * @throws RemoteException   problem computing set from remote data objects
     * @throws VisADException    problem computing set from local Data objects
     */
    protected Set getAnimationSetFromDisplayables()
            throws VisADException, RemoteException {
        Set aniSet = null;
        if (animation == null) {
            return aniSet;
        }
        RealType aniType = animation.getAnimationRealType();
        for (Iterator iter = displayables.iterator(); iter.hasNext(); ) {
            Displayable displayable = (Displayable) iter.next();
            if ((displayable == null)
                    || !displayable.getUseTimesInAnimation()) {
                continue;
            }
            Set set = displayable.getAnimationSet(aniType, false);
            if (set == null) {
                continue;
            }

            if (set != null) {
                aniSet = (aniSet == null)
                         ? set
                         : aniSet.merge1DSets(set);
            }
        }
        return aniSet;
    }


    /**
     * Build the animation set
     *
     * @return the animation set
     *
     * @throws RemoteException   problem computing set from remote data objects
     * @throws VisADException    problem computing set from local Data objects
     */
    protected Set buildAnimationSet() throws VisADException, RemoteException {
        if ((animationWidget != null)
                && animationWidget.getAnimationSetInfo().getActive()) {
            return animationWidget.getAnimationSetInfo().makeTimeSet(this);
        }
        return getAnimationSetFromDisplayables();
    }


    /**
     * Check to see if the animation sets are equals
     *
     * @param set1 first set
     * @param set2 second set
     *
     * @return true if they are equal
     *
     * @throws RemoteException   problem computing set from remote data objects
     * @throws VisADException    problem computing set from local Data objects
     */
    private boolean animationSetsEquals(Set set1, Set set2)
            throws VisADException, RemoteException {
        if ((set1 == null) || (set2 == null)) {
            return false;
        }
        if ( !(set1 instanceof Gridded1DDoubleSet)
                || !(set2 instanceof Gridded1DDoubleSet)) {
            return Misc.equals(set1, set2);
        }
        Gridded1DDoubleSet g1 = (Gridded1DDoubleSet) set1;
        Gridded1DDoubleSet g2 = (Gridded1DDoubleSet) set2;
        double[][]         v1 = g1.getDoubles(false);
        double[][]         v2 = g2.getDoubles(false);
        return Misc.arraysEquals(v1, v2);
    }

    /**
     * <p>Handles a change to the data in the displayables.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    protected void dataChange() throws VisADException, RemoteException {
        if (animation != null) {
            Set set = buildAnimationSet();
            //            System.err.println("new set:" + set + "\nold set:" + animation.getSet());
            if ( !animationSetsEquals(set, animation.getSet())) {
                animation.setSet(set, true);
            }
        }
    }

    /** private object for locking */
    private Object IMAGE_SAVE_MUTEX = new Object();


    /**
     * Capture the display's current image and save it to a file as an image
     * (eg, JPEG, png). If <code>doSync</code> is true, then the calling
     * thread will block until rendering is complete.
     *
     * @param toFile The file to which to save the current image.
     * @param doSync Whether or not to wait until the display is stable.
     * @param block  Whether or not to wait until the image is saved.
     */
    public void saveCurrentDisplay(File toFile, final boolean doSync,
                                   boolean block) {

        saveCurrentDisplay(toFile, doSync, block, 1.0f);
    }





    /**
     * Capture the display's current image and save it to a file as an image
     * (eg, JPEG, png). If <code>doSync</code> is true, then the calling
     * thread will block until rendering is complete.
     *
     * @param toFile The file to which to save the current image.
     * @param doSync Whether or not to wait until the display is stable.
     * @param block  Whether or not to wait until the image is saved.
     * @param quality JPEG quality
     */
    public void saveCurrentDisplay(File toFile, final boolean doSync,
                                   boolean block, final float quality) {
        // user has requested saving display as an image
        final File saveFile = toFile;

        try {
            Runnable captureImage = new Runnable() {
                public void run() {
                    LocalDisplay    display  = getDisplay();
                    DisplayRenderer renderer = display.getDisplayRenderer();
                    BufferedImage   image;
                    Thread          thread = Thread.currentThread();

                    //A hack to make use of the syncing feature in DisplayImpl
                    if (display instanceof DisplayImpl) {
                        renderer.setWaitMessageVisible(false);
                        image = ((DisplayImpl) display).getImage(doSync);
                        //                        System.err.println (display.getClass().getName ()+": image = " + image.getClass().getName ());
                        renderer.setWaitMessageVisible(true);
                    } else {
                        image = display.getImage();
                    }

                    try {
                        ucar.unidata.ui.ImageUtils.writeImageToFile(image,
                                saveFile.toString(), quality);
                    } catch (Exception err) {
                        LogUtil.logException("Problem saving image", err);
                    }
                }
            };
            Thread t = new Thread(captureImage);

            //For some reason visad does not allow a getImage call
            //from an AWT-EventQueue thread. So we won't block here
            //so the getImage will be called from this new thread
            if (block) {
                t.run();
            } else {
                t.start();
            }
        } catch (Exception exp) {
            LogUtil.logException("Problem saving image", exp);
        }
    }


    /**
     * Get a buffered image of the Display
     *
     * @param doSync   true to wait until display is done
     *
     * @return BufferedImage
     *
     * @throws Exception problem getting the image
     */
    public BufferedImage getImage(final boolean doSync) throws Exception {
        LocalDisplay    display  = getDisplay();
        DisplayRenderer renderer = display.getDisplayRenderer();
        BufferedImage   image;
        if (display instanceof DisplayImpl) {
            renderer.setWaitMessageVisible(false);
            image = ((DisplayImpl) display).getImage(doSync);
            renderer.setWaitMessageVisible(true);
        } else {
            image = display.getImage();
        }
        return image;
    }


    /**
     * Class BackedScalarMaps
     */
    private abstract static class BackedScalarMaps {

        /** set of ScalarMaps */
        protected final ScalarMapSet set;

        /**
         * Set of backed ScalarMaps
         */
        public BackedScalarMaps() {
            set = new ScalarMapSet();
        }

        /**
         * Return the size of this set
         * @return number of ScalarMaps
         */
        public synchronized int size() {

            sync();

            return set.size();
        }

        /**
         * Get the set iterator
         * @return the iterator
         */
        public synchronized Iterator iterator() {

            sync();

            return set.iterator();
        }

        /**
         * See if this equals the object in question
         *
         * @param obj   object to compare
         * @return  true if they are equal
         */
        public synchronized boolean equals(Object obj) {

            sync();

            if (this == obj) {
                return true;
            }

            if ( !(obj instanceof BackedScalarMaps)) {
                return false;
            }

            return set.equals(((BackedScalarMaps) obj).set);
        }

        /**
         * Calculate the hashCode for this object
         * @return hashcode
         */
        public synchronized int hashCode() {

            sync();

            return set.hashCode();
        }

        /**
         * Return this as a {@link ScalarMapSet}
         * @return a ScalarMapSet
         */
        public ScalarMapSet asScalarMapSet() {

            sync();

            return set;
        }

        /**
         * Create a String representation of this.
         * @return  string output
         */
        public synchronized String toString() {
            sync();
            return set.toString();
        }

        /**
         * Synchronize this object.
         */
        private synchronized void sync() {
            if (isDirty()) {
                set.clear();
                populate();
            }
        }

        /**
         * See if this has changed
         * @return  true if this has changes
         */
        public abstract boolean isDirty();

        /**
         * Populate this set
         */
        protected abstract void populate();
    }

    /**
     * Class SimpleBackedScalarMaps - implementation of BackedScalarMaps
     *
     */
    private abstract static class SimpleBackedScalarMaps extends BackedScalarMaps {

        /** flag for changes */
        protected boolean isDirty = true;

        /**
         * Set the dirty flag to true
         */
        public synchronized void setDirty() {
            if ( !isDirty) {
                set.clear();
            }
            isDirty = true;
        }

        /**
         * Check if this has changed (is dirty)
         * @return  true if has changed
         */
        public synchronized boolean isDirty() {
            return isDirty;
        }
    }

    /**
     * Class ScalarMaps  -
     */
    private static class ScalarMaps {

        /** flag for changes */
        private boolean isDirty = true;

        /** set of scalar maps */
        private final ScalarMapSet set;

        /**
         * Constructor
         */
        public ScalarMaps() {
            set = new ScalarMapSet();
        }

        /**
         * Add the ScalarMaps in that to this.
         *
         * @param that  set of maps
         */
        public synchronized void add(ScalarMapSet that) {

            int size = size();

            set.add(that);

            if (size != size()) {
                isDirty = true;
            }
        }

        /**
         * Remove a ScalarMap from the set
         *
         * @param map  map to remove
         * @return  true if successful
         */
        public synchronized boolean remove(ScalarMap map) {

            boolean existed = set.remove(map);

            if (existed) {
                isDirty = true;
            }

            return existed;
        }

        /**
         * Remove a set of ScalarMaps from this set
         *
         * @param that  set of maps to remove
         */
        public synchronized void remove(ScalarMapSet that) {

            int size = size();

            set.remove(that);

            if (size != size()) {
                isDirty = true;
            }
        }

        /**
         * Clear out the set of maps
         */
        public synchronized void removeAll() {

            int size = size();

            set.removeAll();

            if (size != size()) {
                isDirty = true;
            }
        }

        /**
         * Get the size of the maps
         * @return number of maps in the set
         */
        public synchronized int size() {
            return set.size();
        }

        /**
         * Return this as a ScalarMapSet
         * @return  set of ScalarMaps
         */
        public ScalarMapSet asScalarMapSet() {
            return set;
        }

        /**
         * Clear the dirty flag
         */
        public synchronized void setClean() {
            isDirty = false;
        }

        /**
         * See if this object is equal to the one in question.
         *
         * @param obj  object to compare
         * @return  true if they are equal
         */
        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }

            if ( !(obj instanceof ScalarMaps)) {
                return false;
            }

            return set.equals(((ScalarMaps) obj).set);
        }

        /**
         * Get the hashcode for this object
         * @return the hashcode
         */
        public int hashCode() {
            return set.hashCode();
        }

        /**
         * Check to see if this set is dirty (has changed)
         * @return  true if dirty
         */
        public synchronized boolean isDirty() {
            return isDirty;
        }

        /**
         * Get a String representation of this object
         * @return  string
         */
        public synchronized String toString() {
            return set.toString();
        }
    }

    /**
     * Class Displayables - holds a set of displayables
     */
    private static class Displayables {

        /** list of displayables */
        private final List<Displayable> list;

        /**
         * Create a set of displayables with the initial size.
         *
         * @param count  initial size
         *
         */
        public Displayables(int count) {
            list = Collections.synchronizedList(
                new ArrayList<Displayable>(count));
        }


        /**
         * Get the iterator for the list of Displayables
         * @return  iteratore
         */
        public Iterator iterator() {
            return new ArrayList<Displayable>(list).iterator();
        }

        /**
         * Get the size of the list of Displayables
         * @return  number of displayables
         */
        public int size() {
            return list.size();
        }

        /**
         * Get the Displayable at the particular index
         *
         * @param index  index in list
         * @return  Displayable at that index
         */
        public Displayable get(int index) {
            return (Displayable) list.get(index);
        }

        /**
         * Add a Displayable to the list at the specified index
         *
         * @param index  index to insert
         * @param displayable  Displayable to insert
         */
        public void add(int index, Displayable displayable) {
            list.add(index, displayable);
        }

        /**
         * Return the list of Displayables as an array
         * @return  array of Displayables
         */
        public Displayable[] toArray() {
            return (Displayable[]) list.toArray(new Displayable[list.size()]);
        }

        /**
         * Find the index of the Displayable in the list
         *
         * @param displayable  Displayable to search for
         * @return  index in list or -1 if not in list
         */
        public int indexOf(Displayable displayable) {
            return list.indexOf(displayable);
        }

        /**
         * See if this object is equal to the one in question
         *
         * @param obj  object in question
         * @return  true if they are equal
         */
        public boolean equals(Object obj) {

            if (obj == this) {
                return true;
            }

            if ( !(obj instanceof Displayables)) {
                return false;
            }

            return list.equals(((Displayables) obj).list);
        }

        /**
         * Get the hashcode for this object
         * @return hashcode
         */
        public int hashCode() {
            return list.hashCode();
        }

        /**
         * Remove a Displayable from the list
         *
         * @param displayable Displayable to remove
         * @return  true if sucessful
         */
        public boolean remove(Displayable displayable) {
            return list.remove(displayable);
        }

        /**
         * Remove all Displayables from the list
         */
        public void removeAll() {
            list.clear();
        }
    }

    /**
     * Print out the matrix.
     *
     * @param name  the name of the matrix
     * @param matrix  the matrix to print
     */
    public void printMatrix(String name, double[] matrix) {
        MouseBehavior behavior =
            getDisplay().getDisplayRenderer().getMouseBehavior();
        behavior.getMouseHelper().print_matrix(name, matrix);
    }


}

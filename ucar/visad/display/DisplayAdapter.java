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



import java.awt.Component;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;

import javax.swing.event.*;


/**
 * Provides support for adapting VisAD Display-s into something that is
 * (hopefully) easier to use.  Instances of this class have a separate thread
 * that updates the display.  The thread runs at a lower priority than the
 * creating thread in order to allow the accumulation of a bunch of changes to
 * the display.  Instances of this class also track changes to the JavaBean
 * properties of added {@link ScalarMapAdapter}s and {@link ConstantMapAdapter}s
 * and modify the underlying VisAD display as necessary.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.12 $
 */
public class DisplayAdapter {

    /** the display to adapt */
    private DisplayImpl display = null;

    /** an updater? */
    private Updater updater;

    /** table of data */
    private DatumTable datumTable;

    /** table of ScalarMaps */
    private ScalarMapTable scalarMapTable;

    /** table of ConstantMaps */
    private ConstantMapTable constantMapTable;

    /**
     * Constructs.
     *
     * @param display           The VisAD display to use.
     * @throws DisplayException The VisAD display is <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DisplayAdapter(DisplayImpl display)
            throws DisplayException, VisADException, RemoteException {

        if (display == null) {
            throw new DisplayException(getClass().getName() + ".<init>: "
                                       + "Display argument is null");
        }

        this.display = display;
        updater      = new Updater();

        updater.setPriority(Thread.currentThread().getPriority() / 2);

        datumTable       = new DatumTable();
        scalarMapTable   = new ScalarMapTable();
        constantMapTable = new ConstantMapTable();
    }

    /**
     * Returns the dimensionality of the display.
     *
     * @return                  The dimensionality of the display (either 2 or
     *                          3).
     */
    public int getDimensionality() {

        return display.getDisplayRenderer().getMode2D()
               ? 2
               : 3;
    }

    /**
     * Accepts a ScalarMapAdapter for inclusion.  The name "accept" is used
     * rather than "add" because this class supports the "addition" of the
     * same ScalarMapAdapter multiple times (unlike a VisAD display with
     * {@link visad.ScalarMap}).  If a ScalarMapAdapter was previously added,
     * then subsequent additions result in the underlying
     * {@link visad.ScalarMap} of the ScalarMapAdapter being set to that of
     * the previously- added ScalarMapAdapter (the "controlling"
     * ScalarMapAdapter).  Otherwise, the ScalarMapAdapter is simply added
     * and returned.  In either case, this instance registers itself with the
     * ScalarMapAdapter as a {@link java.beans.PropertyChangeListener} for
     * the {@link ScalarMapAdapter#SCALAR_MAP} property.  A change to the
     * ScalarMapAdapter's underlying ScalarMap will cause the display to
     * be rebuilt, if ncessary.
     *
     * @param adapter           The adapted ScalarMap to accept.
     * @return                  The "controlling" ScalarMapAdapter.  The
     *                          argument is returned if its ScalarMap is unique;
     *                          otherwise, the previously-added
     *                          ScalarMapAdapter with the same ScalarMap is
     *                          returned.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public final ScalarMapAdapter accept(ScalarMapAdapter adapter)
            throws VisADException, RemoteException {
        return scalarMapTable.accept(adapter);
    }

    /**
     * Removes an adapted, VisAD ScalarMap.
     *
     * @param adapter           The {@link ScalarMapAdapter} to be removed from
     *                          this instance.
     */
    public final void remove(ScalarMapAdapter adapter) {
        scalarMapTable.remove(adapter);
    }

    /**
     * Adds an adapted, VisAD data object.
     *
     * @param dataAdapter       The adapted data object to be added to this
     *                          instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public final void add(DataAdapter dataAdapter)
            throws VisADException, RemoteException {
        datumTable.add(dataAdapter);
    }

    /**
     * Removes an adapted, VisAD data object.
     *
     * @param dataAdapter       The adapted, VisAD data object to be removed
     *                          from this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public final void remove(DataAdapter dataAdapter)
            throws VisADException, RemoteException {
        datumTable.setObsolete(dataAdapter);
    }

    /**
     * Adds adapted, VisAD {@link ConstantMap}(s).
     *
     * @param constantMaps              The adapted, VisAD {@link ConstantMaps}
     *                                  to be added to this instance.
     * @throws BadMappingException      The addition of the {@link
     *                                  visad.ConstantMap}-s would cause a
     *                                  {@link visad.DisplayRealType} to have
     *                                  multiple values.
     * @throws VisADException           VisAD failure.
     * @throws RemoteException          Java RMI failure.
     */
    public final void add(ConstantMaps constantMaps)
            throws BadMappingException, VisADException, RemoteException {
        constantMapTable.add(constantMaps);
    }

    /**
     * Removes adapted, VisAD {@link ConstantMaps}.
     *
     * @param constantMaps              The adapted, VisAD
     *                                  {@link visad.ConstantMap}-s
     *                                  to be removed from this instance.
     * @throws VisADException           VisAD failure.
     * @throws RemoteException          Java RMI failure.
     */
    public final void remove(ConstantMaps constantMaps)
            throws VisADException, RemoteException {
        constantMapTable.remove(constantMaps);
    }

    /**
     * Returns the AWT component.
     *
     * @return                          The AWT component.
     */
    public Component getComponent() {
        return display.getComponent();
    }

    /**
     * Returns the VisAD display.
     *
     * @return                          The VisAD display.
     */
    Display getDisplay() {
        return display;
    }

    /**
     * Class Updater
     */
    private class Updater extends Thread {

        /** flag for updating */
        private boolean now = false;

        /**
         * Called when the display needs to be updated.
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   problem creating VisAD object
         */
        private final void updateDisplay()
                throws VisADException, RemoteException {

            boolean scalarMaps   = scalarMapTable.isChanged();
            boolean constantMaps = constantMapTable.isChanged();
            boolean data         = datumTable.isChanged();
            boolean clearDisplay = scalarMaps || constantMaps;

            display.disableAction();

            if (clearDisplay) {
                display.removeAllReferences();
                display.clearMaps();
            }

            if (constantMaps) {
                constantMapTable.setDisplay(display);
            }

            if (scalarMaps) {
                scalarMapTable.setDisplay();
            }

            if (data) {
                datumTable.setDisplay(clearDisplay);
            }

            display.enableAction();
        }

        /**
         * Update the display
         */
        public synchronized void update() {

            now = true;

            notify();
        }

        /**
         * Run this thread
         */
        public void run() {

            for (;;) {
                try {
                    synchronized (this) {
                        if ( !now) {
                            wait();
                        }

                        now = false;
                    }

                    updateDisplay();
                } catch (InterruptedException e) {
                    System.err.println(getClass().getName() + ".run(): "
                                       + "Wait was interrupted: " + e);
                } catch (VisADException e) {
                    System.err.println(getClass().getName() + ".run(): "
                                       + "VisAD failure: " + e);
                } catch (RemoteException e) {
                    System.err.println(getClass().getName() + ".run(): "
                                       + "Java RMI failure: " + e);
                }
            }
        }
    }

    /**
     * Class DatumTable
     */
    private class DatumTable {

        /** Set of data */
        private java.util.Set newDatums = new TreeSet();

        /** Old data */
        private java.util.Set obsoleteDatums = new TreeSet();

        /** extant data */
        private java.util.Set extantDatums = new TreeSet();

        /** changed data */
        private java.util.Set changedDatums = new TreeSet();

        /** whether the table has changed */
        private boolean changed = false;

        /** listener for changes */
        private final PropertyChangeListener listener =
            new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                // A ConstantMap of the adapted data has changed.
                try {
                    setChanged((DataAdapter) event.getSource());
                } catch (Exception e) {
                    System.err.println(getClass().getName()
                                       + ".propertyChange(): "
                                       + "Couldn't handle change to data: "
                                       + e);
                }
            }
        };

        /**
         * Set whether the data has changed
         *
         * @param adapter  adapted data
         */
        public synchronized void setChanged(DataAdapter adapter) {

            if (extantDatums.contains(adapter)) {
                extantDatums.remove(adapter);
                changedDatums.add(adapter);

                changed = true;

                updater.update();
            }
        }

        /**
         * Set that this data is obsolete or not
         *
         * @param adapter  adapted data
         */
        public synchronized void setObsolete(DataAdapter adapter) {

            if (newDatums.contains(adapter)) {
                newDatums.remove(adapter);
            } else {
                java.util.Set set = extantDatums.contains(adapter)
                                    ? extantDatums
                                    : changedDatums.contains(adapter)
                                      ? changedDatums
                                      : null;

                if (set != null) {

                    // Remove listener now so fewest notifications.
                    adapter.removePropertyChangeListener(
                        adapter.CONSTANT_MAP, listener);
                    set.remove(adapter);
                    obsoleteDatums.add(adapter);

                    changed = true;

                    updater.update();
                }
            }
        }

        /**
         * Add data
         *
         * @param adapter  adapted data
         */
        public synchronized void add(DataAdapter adapter) {

            newDatums.add(adapter);

            changed = true;

            updater.update();
        }

        /**
         * Is changed.
         *
         * @return  true if it's changed.
         */
        public synchronized boolean isChanged() {
            return changed;
        }

        /**
         * Set the display for the data
         *
         * @param isEmpty  true if the data is null
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   problem creating VisAD object
         */
        public synchronized void setDisplay(boolean isEmpty)
                throws VisADException, RemoteException {

            if (isEmpty) {
                for (Iterator iter =
                        extantDatums.iterator(); iter.hasNext(); ) {
                    ((DataAdapter) iter.next()).addTo();
                }
            } else {
                for (Iterator iter = obsoleteDatums.iterator();
                        iter.hasNext(); ) {
                    ((DataAdapter) iter.next()).removeFrom();
                }
            }

            obsoleteDatums.clear();

            for (Iterator iter = changedDatums.iterator(); iter.hasNext(); ) {
                DataAdapter adapter = (DataAdapter) iter.next();

                adapter.addTo();
                iter.remove();
                extantDatums.add(adapter);
            }

            for (Iterator iter = newDatums.iterator(); iter.hasNext(); ) {
                DataAdapter adapter = (DataAdapter) iter.next();

                adapter.addTo();
                iter.remove();
                extantDatums.add(adapter);

                // Add listener now so fewest notifications.
                adapter.addPropertyChangeListener(adapter.CONSTANT_MAP,
                        listener);
            }

            changed = false;
        }
    }

    /**
     * Class ScalarMapTable
     */
    private class ScalarMapTable {

        /** table of maps */
        private Map map = new TreeMap();

        /** flag for changes */
        private boolean changed = false;

        /** listener */
        private final PropertyChangeListener listener =
            new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {
                    accept((ScalarMapAdapter) event.getSource(),
                           (ScalarMap) event.getOldValue());
                } catch (Exception e) {
                    System.err.println(
                        getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to ScalarMap: " + e);
                }
            }
        };

        /**
         * Remove an adapter
         *
         * @param adapter  adapter to remove
         */
        public synchronized final void remove(ScalarMapAdapter adapter) {

            adapter.removePropertyChangeListener(adapter.SCALAR_MAP,
                    listener);

            ScalarMap        scalarMap = adapter.getScalarMap();
            ScalarMapAdapter source    =
                (ScalarMapAdapter) map.get(scalarMap);

            if ((source != null) && (source == adapter)) {
                map.remove(scalarMap);

                changed = true;

                updater.update();
            }
        }

        /**
         * Accept the adapter
         *
         * @param adapter  adapter in question
         * @return the adapter
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   problem creating VisAD object
         */
        public final ScalarMapAdapter accept(ScalarMapAdapter adapter)
                throws VisADException, RemoteException {

            adapter.addPropertyChangeListener(adapter.SCALAR_MAP, listener);

            return accept(adapter, null);
        }

        /**
         *
         * @param source
         * @param oldScalarMap  The previous ScalarMap of the ScalarMapAdapter.
         *                      May be <code>null</code>.
         * @return              The controlling ScalarMapAdapter of the display.
         *
         * @throws RemoteException
         * @throws VisADException
         */
        protected synchronized final ScalarMapAdapter accept(
                ScalarMapAdapter source, ScalarMap oldScalarMap)
                throws VisADException, RemoteException {

            ScalarMapAdapter adapter;

            if (oldScalarMap != null) {
                adapter = (ScalarMapAdapter) map.get(oldScalarMap);

                if ((adapter != null) && (adapter == source)) {
                    map.remove(oldScalarMap);

                    changed = true;
                }
            }

            ScalarMap newScalarMap = source.getScalarMap();

            adapter = (ScalarMapAdapter) map.get(newScalarMap);

            if (adapter != null) {
                source.duplicate(adapter);
            } else {
                adapter = source;

                map.put(newScalarMap, source);

                changed = true;
            }

            if (changed) {
                updater.update();
            }

            return adapter;
        }

        /**
         * Set the display.
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   problem creating VisAD object
         */
        protected synchronized void setDisplay()
                throws VisADException, RemoteException {

            for (Iterator iter = map.values().iterator(); iter.hasNext(); ) {
                ((ScalarMapAdapter) iter.next()).setDisplay();
            }

            changed = false;
        }

        /**
         * See if this has changed.
         * @return  true if this has changed.
         */
        public synchronized boolean isChanged() {
            return changed;
        }
    }

    /**
     * Class ConstantMapTable
     */
    private class ConstantMapTable {

        /** Set of ConstantMaps */
        private final ConstantMapComposite maps = new SetOfConstantMaps();

        /** flag for changes */
        private boolean changed = false;

        /** listener for changes */
        private final PropertyChangeListener listener =
            new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {
                    changed = true;

                    updater.update();
                } catch (Exception e) {
                    System.err.println(
                        getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to ConstantMap: " + e);
                }
            }
        };

        /**
         *
         * @param constantMaps
         * @throws BadMappingException  A ConstantMap for the DisplayRealType
         *                              already exists.
         * @throws RemoteException
         * @throws VisADException
         */
        public synchronized final void add(ConstantMaps constantMaps)
                throws BadMappingException, VisADException, RemoteException {

            maps.add(constantMaps);
            constantMaps.addPropertyChangeListener(ConstantMaps.CONSTANT_MAP,
                    listener);

            changed = true;

            updater.update();
        }

        /**
         * Remove a set of ConstantMaps
         *
         * @param constantMaps ConstantMaps to remove
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   problem creating VisAD object
         */
        public synchronized final void remove(ConstantMaps constantMaps)
                throws VisADException, RemoteException {

            constantMaps.removePropertyChangeListener(
                ConstantMaps.CONSTANT_MAP, listener);
            maps.remove(constantMaps);

            changed = true;

            updater.update();
        }

        /**
         * See if this has changed
         * @return true if has changed
         */
        public synchronized boolean isChanged() {
            return changed;
        }

        /**
         * Set the display.
         *
         * @param display  display to set
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   problem creating VisAD object
         */
        protected synchronized void setDisplay(Display display)
                throws RemoteException, VisADException {

            maps.setDisplay(display);

            changed = false;
        }
    }
}

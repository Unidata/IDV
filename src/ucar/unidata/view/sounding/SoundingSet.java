/*
 * $Id: SoundingSet.java,v 1.25 2005/08/02 20:49:15 dmurray Exp $
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


import ucar.unidata.beans.*;

import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;



import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for a CompositeDisplayable of Sounding-s.  For purposes of
 * PropertyChangeEvent-s, the last-added Sounding is the active one.
 */
public class SoundingSet extends CompositeDisplayable {

    /**
     * The name of the presure property.
     */
    public static String PRESSURE = "pressure";

    /**
     * The name of the temperature property.
     */
    public static String TEMPERATURE = "temperature";

    /**
     * The name of the dew-point property.
     */
    public static String DEW_POINT = "dewPoint";

    /**
     * The name of the active sounding property.
     */
    public static String ACTIVE_SOUNDING = "activeSounding";

    /**
     * The empty sounding.
     */
    private Sounding missingSounding;

    /**
     * The currently active sounding property.
     */
    private Sounding activeSounding;

    /**
     * The pressure property.
     */
    private Real pressure;

    /**
     * The constrainProfiles property.
     */
    private boolean constrainProfiles = true;

    /** temperature value */
    private Real temperature;

    /** dewpoint value */
    private Real dewPoint;

    /** temperature change listener */
    private PropertyChangeListener temperatureListener =
        new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent event) {

            temperature = (Real) event.getNewValue();

            PropertyChangeEvent newEvent =
                new PropertyChangeEvent(SoundingSet.this, TEMPERATURE,
                                        event.getOldValue(), temperature);

            newEvent.setPropagationId(event.getPropagationId());
            SoundingSet.this.firePropertyChange(newEvent);
        }
    };

    /** dewpoint change listener */
    private PropertyChangeListener dewPointListener =
        new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent event) {

            dewPoint = (Real) event.getNewValue();

            PropertyChangeEvent newEvent =
                new PropertyChangeEvent(SoundingSet.this, DEW_POINT,
                                        event.getOldValue(), dewPoint);

            newEvent.setPropagationId(event.getPropagationId());
            SoundingSet.this.firePropertyChange(newEvent);
        }
    };

    /**
     * Constructs from a VisAD display.  The value of the constrainProfiles
     * property is initially <code>true</code>.
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public SoundingSet(LocalDisplay display)
            throws VisADException, RemoteException {

        super(display);

        missingSounding = new Sounding(display);
        activeSounding  = missingSounding;
        pressure        = activeSounding.getPressure();
        temperature     = activeSounding.getTemperature();
        dewPoint        = activeSounding.getDewPoint();
    }

    /**
     * Adds a sounding to this set.  The sounding's constrainProfiles property
     * is set to the value of this instance's constrainProfiles property.
     * The sounding will be add to the end of the set.
     * @param sounding          The sounding to be added.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void addSounding(Sounding sounding)
            throws RemoteException, VisADException {
        addSounding(displayableCount(), sounding);
    }

    /**
     * Adds a sounding to this set.  The sounding's constrainProfiles property
     * is set to the value of this instance's constrainProfiles property.
     * @param index             The index of the sounding.
     * @param sounding          The sounding to be added.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void addSounding(int index, Sounding sounding)
            throws RemoteException, VisADException {
        setDisplayable(index, sounding);
        sounding.setConstrainProfiles(constrainProfiles);
    }

    /**
     * Removes a sounding from this set.
     * @param index             The index of the sounding.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void removeSounding(int index)
            throws IndexOutOfBoundsException, RemoteException,
                   VisADException {

        Sounding sounding = (Sounding) getDisplayable(index);
        if (sounding == null) {
            return;
        }

        if (sounding == activeSounding) {
            removeListeners(sounding);
        }

        removeDisplayable(sounding);

        if ((displayableCount() == 0) || (sounding == activeSounding)) {
            setActiveSounding(missingSounding);
        }
    }

    /**
     * Reset the profiles at the given index
     *
     * @param index  index of profile
     *
     * @throws RemoteException   Java RMI exception
     * @throws VisADException    VisAD problem
     */
    public void setOriginalProfiles(int index)
            throws VisADException, RemoteException {
        Sounding sounding = (Sounding) getDisplayable(index);
        if (sounding != null) {
            sounding.setOriginalProfiles();
        }
    }

    /**
     * Sets the constrainProfiles property.  When this property is set, profile
     * temperatures are constrained to be equal to or greater than their
     * corresponding profile dew-points.  This method sets the constrainProfiles
     * property in all Sounding children to match the given value.
     * @param yes                       Whether or not to constrain temperatures
     *                                  to be equal to or greater than
     *                                  corresponding dew-points.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setConstrainProfiles(boolean yes)
            throws RemoteException, VisADException {

        if (yes != constrainProfiles) {
            for (Iterator iter = iterator(); iter.hasNext(); ) {
                ((Sounding) iter.next()).setConstrainProfiles(yes);
            }
        }

        constrainProfiles = yes;
    }

    /**
     * Sets the pressure property.
     * @param pressure          The pressure property.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setPressure(Real pressure)
            throws RemoteException, VisADException {

        Real oldPressure = this.pressure;

        this.pressure = pressure;

        activeSounding.setPressure(pressure);
        firePropertyChange(PRESSURE, oldPressure, this.pressure);
    }

    /**
     * Returns the pressure property.
     * @return                  The pressure property.
     */
    public Real getPressure() {
        return pressure;
    }

    /**
     * Returns the temperature property.
     * @return                  The temperature property.
     */
    public Real getTemperature() {
        return temperature;
    }

    /**
     * Returns the dew-point property.
     * @return                  The dew-point property.
     */
    public Real getDewPoint() {
        return dewPoint;
    }

    /**
     * Returns the set of types of the pressures.
     * @return                  The set of types of the pressures.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized java.util.Set getPressureRealTypeSet()
            throws VisADException, RemoteException {

        java.util.Set realTypeSet = new TreeSet();

        for (Iterator iter = iterator(); iter.hasNext(); ) {
            realTypeSet.add(((Sounding) iter.next()).getPressureRealType());
        }

        return realTypeSet;
    }

    /**
     * Returns the set of types of the temperatures.
     * @return                  The set of types of the temperatures.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized java.util.Set getTemperatureRealTypeSet()
            throws VisADException, RemoteException {

        java.util.Set realTypeSet = new TreeSet();

        for (Iterator iter = iterator(); iter.hasNext(); ) {
            realTypeSet.add(
                ((Sounding) iter.next()).getTemperatureRealType());
        }

        return realTypeSet;
    }

    /**
     * Returns the set of types of the dew-points.
     * @return                  The set of types of the dew-points.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized java.util.Set getDewPointRealTypeSet()
            throws VisADException, RemoteException {

        java.util.Set realTypeSet = new TreeSet();

        for (Iterator iter = iterator(); iter.hasNext(); ) {
            realTypeSet.add(((Sounding) iter.next()).getDewPointRealType());
        }

        return realTypeSet;
    }

    /**
     * Clears this composite of all children.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void clear() throws VisADException, RemoteException {

        for (int i = 0; i < displayableCount(); ++i) {
            removeSounding(i);
        }
    }

    /**
     * Sets the active sounding.
     * @param index             The index of the active sounding.  A value of
     *                          -1 means that there is to be no active sounding.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setActiveSounding(int index)
            throws VisADException, RemoteException {

        setActiveSounding((index < 0)
                          ? missingSounding
                          : (Sounding) getDisplayable(index));
    }

    /**
     * Sets the active sounding property.
     * @param sounding          The new values.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void setActiveSounding(Sounding sounding)
            throws VisADException, RemoteException {

        if (sounding == null) {
            sounding = missingSounding;
        }
        Sounding old = activeSounding;

        sounding.setPressure(pressure);
        removeListeners(activeSounding);

        activeSounding = sounding;

        setTemperature(activeSounding.getTemperature());
        setDewPoint(activeSounding.getDewPoint());
        addListeners(activeSounding);
        firePropertyChange(ACTIVE_SOUNDING, old, activeSounding);
    }

    /**
     * Returns the active sounding.  NB: Does not reaturn a copy.
     * @return                  The active sounding.
     */
    public Sounding getActiveSounding() {
        return activeSounding;
    }

    /**
     * Set the temperature property
     *
     * @param temp  new temperature property
     */
    private synchronized void setTemperature(Real temp) {

        Real oldValue = temperature;

        temperature = temp;

        firePropertyChange(TEMPERATURE, oldValue, temperature);
    }

    /**
     * Set the dewpoint property
     *
     * @param temp  new dewpoint temperature property
     */
    private synchronized void setDewPoint(Real temp) {

        Real oldValue = dewPoint;

        dewPoint = temp;

        firePropertyChange(DEW_POINT, oldValue, dewPoint);
    }

    /**
     * Add listeners to the specified sounding
     *
     * @param sounding  sounding to add to
     */
    private void addListeners(Sounding sounding) {

        sounding.addPropertyChangeListener(Sounding.TEMPERATURE,
                                           temperatureListener);
        sounding.addPropertyChangeListener(Sounding.DEW_POINT,
                                           dewPointListener);
    }

    /**
     * Remove listeners from the specified sounding
     *
     * @param sounding   sounding to modify
     */
    private void removeListeners(Sounding sounding) {

        sounding.removePropertyChangeListener(Sounding.TEMPERATURE,
                temperatureListener);
        sounding.removePropertyChangeListener(Sounding.DEW_POINT,
                dewPointListener);
    }
}


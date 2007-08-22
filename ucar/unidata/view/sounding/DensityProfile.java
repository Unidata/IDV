/*
 * $Id: DensityProfile.java,v 1.13 2005/05/13 18:33:28 jeffmc Exp $
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



import java.beans.*;

import java.rmi.RemoteException;

import ucar.visad.*;
import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;


/**
 * Provides support for the computation of the vertical profile of air-density.
 *
 * @author Steven R. Emmerson
 * @version $Id: DensityProfile.java,v 1.13 2005/05/13 18:33:28 jeffmc Exp $
 */
public class DensityProfile {

    /**
     * The name of the density-profile property.
     */
    public static final String DENSITY_PROFILE = "densityProfile";

    /** missing density profile */
    private static FlatField missingDensityProfile;

    /** missing temperature profile */
    private Field temperatureProfile;

    /** missing dewpoint profile */
    private Field dewPointProfile;

    /** density profile */
    private FlatField densityProfile;

    /** active flag */
    private boolean active = true;

    /** dirty flag */
    private boolean dirty = false;

    /** change listeners */
    private volatile PropertyChangeSupport changeListeners;

    /** reference for temperature profile */
    private DataReferenceImpl temperatureProfileRef;

    /** reference for dewpoint profile */
    private DataReferenceImpl dewPointProfileRef;

    /** listener for input changes */
    private ActionImpl inputProfilesListener;

    static {
        try {
            missingDensityProfile =
                new FlatField(new FunctionType(AirPressure.getRealTupleType(),
                                               AirDensity.getRealType()));
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure;
     */
    public DensityProfile() throws VisADException {

        temperatureProfile =
            new FlatField(new FunctionType(AirPressure.getRealTupleType(),
                                           AirTemperature.getRealType()));
        dewPointProfile =
            new FlatField(new FunctionType(AirPressure.getRealTupleType(),
                                           DewPoint.getRealType()));
        densityProfile = missingDensityProfile;
        temperatureProfileRef =
            new DataReferenceImpl("DensityTemperatureProfileRef");
        dewPointProfileRef =
            new DataReferenceImpl("DensityDewPointProfileRef");
        inputProfilesListener = new ActionImpl("DensityProfileListener")  // new thread
        {

            public void doAction() throws RemoteException, VisADException {

                dirty = true;

                computeIfAppropriate();
            }
        };

        try {
            inputProfilesListener.addReference(temperatureProfileRef);
            inputProfilesListener.addReference(dewPointProfileRef);
        } catch (RemoteException e) {}  // can't happen because all data are local
    }

    /**
     * Constructs from temperature and dew-point profiles.
     * @param temperatureProfile        The temperature profile.
     * @param dewPointProfile           The dew-point profile.
     * @throws VisADException   VisAD failure;
     * @throws RemoteException  Java RMI failure.
     */
    public DensityProfile(Field temperatureProfile, Field dewPointProfile)
            throws VisADException, RemoteException {

        this();

        setProfiles(temperatureProfile, dewPointProfile);
    }

    /**
     * Adds a PropertyChangeListener.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        addPropertyChangeListener(DENSITY_PROFILE, listener);
    }

    /**
     * Adds a PropertyChangeListener for a named property.
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {

        if (name.equals(DENSITY_PROFILE)) {
            if (changeListeners == null) {
                synchronized (this) {
                    if (changeListeners == null) {
                        changeListeners = new PropertyChangeSupport(this);
                    }
                }
            }

            changeListeners.addPropertyChangeListener(listener);
        }
    }

    /**
     * Removes a PropertyChangeListener.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        removePropertyChangeListener(DENSITY_PROFILE, listener);
    }

    /**
     * Removes a PropertyChangeListener for a named property.
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public synchronized void removePropertyChangeListener(String name,
            PropertyChangeListener listener) {

        if (changeListeners != null) {
            changeListeners.removePropertyChangeListener(name, listener);
        }
    }

    /**
     * Enables or disables this instance.  If an instance is enabled, then it
     * will recompute the air-density profile when appropriate.
     * @param yes               Whether or not this instance is to be enabled.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setActive(boolean yes)
            throws VisADException, RemoteException {

        boolean wasActive = active;

        active = yes;

        computeIfAppropriate();
    }

    /**
     * Sets the temperature and dew-point profiles.  This instance will register
     * itself with the profiles in order to receive change notifications.
     * @param temperatureProfile        The air-temperature profile.
     * @param dewPointProfile           The dew-point profile.
     * @throws VisADException           VisAD failure.
     * @throws RemoteException          Java RMI failure.
     */
    public void setProfiles(Field temperatureProfile, Field dewPointProfile)
            throws VisADException, RemoteException {

        setActive(false);
        setTemperatureProfile(temperatureProfile);
        setDewPointProfile(dewPointProfile);
        setActive(true);
    }

    /**
     * Sets the temperature profile.  This instance will register itself with
     * the profile in order to receive change notifications.
     * @param temperatureProfile        The air-temperature profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setTemperatureProfile(Field temperatureProfile)
            throws VisADException, RemoteException {

        Util.vetType(this.temperatureProfile.getType(), temperatureProfile);

        this.temperatureProfile = temperatureProfile;

        temperatureProfileRef.setData(temperatureProfile);
    }

    /**
     * Sets the dew-point profile.  This instance will register itself with
     * the profile in order to receive change notifications.
     * @param dewPointProfile   The dew-point profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDewPointProfile(Field dewPointProfile)
            throws VisADException, RemoteException {

        Util.vetType(this.dewPointProfile.getType(), dewPointProfile);

        this.dewPointProfile = dewPointProfile;

        dewPointProfileRef.setData(dewPointProfile);
    }

    /**
     * Computes the air-density profile if appropriate.
     * @throws VisADException   VisAD failure;
     * @throws RemoteException  Java RMI failure;
     */
    protected void computeIfAppropriate()
            throws VisADException, RemoteException {

        if (active && dirty) {
            if (temperatureProfile.isMissing()
                    || dewPointProfile.isMissing()) {
                densityProfile = missingDensityProfile;
            } else {
                FlatField old = densityProfile;

                densityProfile = (FlatField) AirDensity.create(
                    temperatureProfile.getDomainSet(), temperatureProfile,
                    dewPointProfile);

                if (changeListeners != null) {
                    changeListeners.firePropertyChange(DENSITY_PROFILE, old,
                                                       densityProfile);
                }
            }

            dirty = false;
        }
    }

    /**
     * Returns the air-density profile.
     * @return                  The air-density profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public FlatField getDensityProfile()
            throws VisADException, RemoteException {
        return densityProfile;
    }

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof DensityProfile)) {
            equals = false;
        } else {
            DensityProfile that = (DensityProfile) obj;

            equals = (this == that)
                     || (temperatureProfile.equals(that.temperatureProfile)
                         && dewPointProfile.equals(that.dewPointProfile)
                         && densityProfile.equals(that.densityProfile)
                         && (active == that.active) && (dirty == that.dirty)
                         && ((changeListeners == null)
                             ? that.changeListeners == null
                             : ((that.changeListeners != null)
                                && changeListeners.equals(
                                    that.changeListeners))));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {

        return temperatureProfile.hashCode() ^ dewPointProfile.hashCode()
               ^ densityProfile.hashCode() ^ new Boolean(active).hashCode()
               ^ new Boolean(dirty).hashCode() ^ ((changeListeners == null)
                                                  ? 0
                                                  : changeListeners
                                                  .hashCode());
    }
}








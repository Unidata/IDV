/*
 * $Id: Sounding.java,v 1.30 2006/05/08 21:43:57 dmurray Exp $
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

import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;



import java.beans.*;

import java.rmi.RemoteException;


/**
 * Provides support for a composite displayable comprising a temperature
 * profile and a dew-point profile.
 *
 * @author Steven R. Emmerson
 * @version $Id: Sounding.java,v 1.30 2006/05/08 21:43:57 dmurray Exp $
 */
public class Sounding extends CompositeDisplayable {

    /**
     * The name of the pressure property.
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
     * The temperature profile.
     */
    private AirTemperatureProfile temperatureProfile;

    /**
     * The dew-point profile.
     */
    private DewPointProfile dewPointProfile;

    /**
     * The constrainProfiles property.
     */
    private boolean constrainProfiles = false;

    /** locking object */
    private Object constraintLock = new Object();

    /** flag for temperature constraint */
    private boolean constrainTemperatures = true;

    /** flag for dewpoint constraints */
    private boolean constrainDewPoints = true;

    /** last modified field flag */
    private int lastModifiedField = 0;

    /** Constraint for temp profile */
    private static Constraint temperatureProfileConstraint;

    /** Constraint for dewpoint profile */
    private static Constraint dewPointProfileConstraint;

    /** temperature field index */
    private static int temperatureField = 1;

    /** dewpoint field index */
    private static int dewPointField = 2;

    static {
        temperatureProfileConstraint = new Constraint() {

            public boolean isOK(Real value1, Real value2) {
                return value1.compareTo(value2) >= 0;
            }
        };
        dewPointProfileConstraint = new Constraint() {

            public boolean isOK(Real value1, Real value2) {
                return value1.compareTo(value2) <= 0;
            }
        };
    }

    /**
     * Constructs from a VisAD display.  The constrainProfiles property is
     * initially <code>false</code>.
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Sounding(LocalDisplay display) throws VisADException,
            RemoteException {

        super(display);

        temperatureProfile = new AirTemperatureProfile();
        dewPointProfile    = new DewPointProfile();

        temperatureProfile.addPropertyChangeListener(
            temperatureProfile.RANGE_VALUE, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                PropertyChangeEvent newEvent =
                    new PropertyChangeEvent(Sounding.this, TEMPERATURE,
                                            event.getOldValue(),
                                            event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                Sounding.this.firePropertyChange(newEvent);
            }
        });
        temperatureProfile.addPropertyChangeListener(
            temperatureProfile.FIELD, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                lastModifiedField = temperatureField;

                try {
                    if (constrainProfiles) {

                        /*
                         * The following logic is necessary to prevent an
                         * infinite loop.
                         */
                        //jeffmc                        synchronized (constraintLock) {
                            if (constrainTemperatures) {
                                constrainTemperatures();
                            }
                            constrainTemperatures = !constrainTemperatures;
                            //                        }
                    }
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".propertyChange(): "
                        + "Couldn't constrain profile temperatures: " + e);
                }
            }
        });
        dewPointProfile.addPropertyChangeListener(
            dewPointProfile.RANGE_VALUE, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                PropertyChangeEvent newEvent =
                    new PropertyChangeEvent(Sounding.this, DEW_POINT,
                                            event.getOldValue(),
                                            event.getNewValue());

                newEvent.setPropagationId(event.getPropagationId());
                Sounding.this.firePropertyChange(newEvent);
            }
        });
        dewPointProfile.addPropertyChangeListener(dewPointProfile.FIELD,
                new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                lastModifiedField = dewPointField;

                try {
                    if (constrainProfiles) {

                        /*
                         * The following logic is necessary to prevent an
                         * infinite loop.
                         */
                        //                        synchronized (constraintLock) {
                            if (constrainDewPoints) {
                                constrainDewPoints();
                            }

                            constrainDewPoints = !constrainDewPoints;
                            //                        }
                    }
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".propertyChange(): "
                        + "Couldn't constrain profile dew-points: " + e);
                }
            }
        });
        addDisplayable(temperatureProfile);
        addDisplayable(dewPointProfile);
    }

    /**
     * Sets the constrainProfiles property.  When this property is set, profile
     * temperatures are constrained to be equal to or greater than their
     * corresponding profile dew-points.
     * @param yes                       Whether or not to constrain temperatures
     *                                  to be equal to or greater than
     *                                  corresponding dew-points.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setConstrainProfiles(boolean yes) throws RemoteException,
            VisADException {

        constrainProfiles = yes;

        if (yes) {
            if (lastModifiedField == temperatureField) {
                constrainTemperatures();
            } else if (lastModifiedField == dewPointField) {
                constrainDewPoints();
            }
        }
    }

    /**
     * Sets the profiles.
     * @param temperatureField  The temperature profile.
     * @param dewPointField     The dew-point profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setFields(Field temperatureField,
                          Field dewPointField) throws RemoteException,
                              VisADException {
        temperatureProfile.setProfile(temperatureField);
        if(dewPointField!=null)
            dewPointProfile.setProfile(dewPointField);
    }

    /**
     * Sets the temperature profile.
     * @param temperatureField  The temperature profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setTemperatureField(
            Field temperatureField) throws RemoteException, VisADException {
        temperatureProfile.setProfile(temperatureField);
    }

    /**
     * Sets the dew-point profile.
     * @param dewPointField     The dew-point profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDewPointField(Field dewPointField) throws RemoteException,
            VisADException {
        dewPointProfile.setProfile(dewPointField);
    }

    /**
     * Returns the temperature profile.  NB: Not a copy.
     * @return                  The temperature profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Field getTemperatureField() throws VisADException,
            RemoteException {
        return temperatureProfile.getProfile();
    }

    /**
     * Returns the dew-point profile.  NB: Not a copy.
     * @return                  The dew-point profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Field getDewPointField() throws VisADException, RemoteException {
        return dewPointProfile.getProfile();
    }

    /**
     * Resets the sounding to the original data.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setOriginalProfiles() throws VisADException, RemoteException {
        temperatureProfile.setOriginalProfile();
        dewPointProfile.setOriginalProfile();
    }

    /**
     * Sets the pressure property.
     * @param pressure          The new value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setPressure(Real pressure) throws RemoteException,
            VisADException {

        Real oldPressure = getPressure();

        temperatureProfile.setPressure(pressure);
        dewPointProfile.setPressure(pressure);
        firePropertyChange(PRESSURE, oldPressure, pressure);
    }

    /**
     * Returns the pressure property.
     * @return                  The pressure property.
     */
    public Real getPressure() {
        return temperatureProfile.getPressure();
    }

    /**
     * Returns the temperature property.
     *
     * @return                  The temperature property.
     */
    public Real getTemperature() {
        return temperatureProfile.getRangeValue();
    }

    /**
     * Returns the temperature property.
     * @return                  The temperature property.
     */
    public Real getDewPoint() {
        return dewPointProfile.getRangeValue();
    }

    /**
     * Returns the type of the pressure quantity.
     * @return                  The type of the pressure quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealType getPressureRealType() throws VisADException,
            RemoteException {
        return (RealType) ((FunctionType) getTemperatureField().getType())
            .getDomain().getComponent(0);
    }

    /**
     * Returns the type of the temperature quantity.
     * @return                  The type of the temperature quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealType getTemperatureRealType() throws VisADException,
            RemoteException {
        return (RealType) ((FunctionType) getTemperatureField().getType())
            .getFlatRange().getComponent(0);
    }

    /**
     * Returns the type of the dew-point quantity.
     * @return                  The type of the dew-point quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealType getDewPointRealType() throws VisADException,
            RemoteException {
        return (RealType) ((FunctionType) getDewPointField().getType())
            .getFlatRange().getComponent(0);
    }

    /**
     * Constraint
     *
     * @author Unidata development team
     */
    private interface Constraint {

        /**
         * See if this is within the constraints
         *
         * @param value1    value 1
         * @param value2    value 2
         * @return true if in constraint
         */
        boolean isOK(Real value1, Real value2);
    }

    /**
     * Constrains the temperatures to be greater than or equal to the
     * corresponding dew-points.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void constrainTemperatures() throws VisADException,
            RemoteException {
        constrainProfile(temperatureProfile, temperatureProfileConstraint,
                         dewPointProfile);
    }

    /**
     * Constrains the dew-points to be less than or equal to the corresponding
     * temperatures.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void constrainDewPoints() throws VisADException, RemoteException {
        constrainProfile(dewPointProfile, dewPointProfileConstraint,
                         temperatureProfile);
    }

    /**
     * Constrains the values of one profile to have a given relationship to
     * the corresponding values in another profile.
     * @param profile1          The profile to have its values constrained.
     * @param constraint        The constraint between the values of the
     *                          profiles.
     * @param profile2          The profile with the constraining values.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private static void constrainProfile(
                                         final Profile profile1,final Constraint constraint,
                                         final Profile profile2) throws VisADException, RemoteException {

        //jeffmc
        //For now don't do this as it is triggering a deadlock
        if(true) return;
        if ( !profile1.getActive()) {
            return;  // avoid deadlock
        }
        profile1.setActive(false);

        Field field1 = profile1.getProfile();
        Field field2 = profile2.getProfile().resample(field1.getDomainSet(),
                                                      Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
        int      sampleCount = field1.getLength();
        MathType type1       = ((FunctionType) field1.getType()).getRange();

        for (int i = 0; i < sampleCount; ++i) {
            Real value1 = (Real) field1.getSample(i);

            if ( !value1.isMissing()) {
                Real value2 = (Real) field2.getSample(i);

                if ( !value2.isMissing()) {
                    if ( !constraint.isOK(value1, value2)) {
                        field1.setSample(i, Util.clone(value2, type1));
                    }
                }
            }
        }
        profile1.setActive(true);
    }
}


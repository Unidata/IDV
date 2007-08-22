/*
 * $Id: BuoyancyProfileBean.java,v 1.7 2005/05/13 18:33:24 jeffmc Exp $
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



import java.awt.event.ActionEvent;

import java.beans.*;

import java.rmi.RemoteException;

import ucar.visad.functiontypes.AirTemperatureProfile;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.quantities.AirDensity;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.SaturationVirtualPotentialTemperature;
import ucar.visad.quantities.VirtualPotentialTemperature;
import ucar.visad.quantities.VirtualTemperature;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.MathType;

import visad.TypeException;

import visad.VisADException;


/**
 * A Java Bean that computes an atmospheric buoyancy-profile of the trajectory
 * of a parcel of air from in-situ temperature and dew-point profiles.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:24 $
 */
public final class BuoyancyProfileBean extends ClockedBean {

    /** trajectory */
    private Field traj;

    /** temperature field */
    private Field temp;

    /** dewpoint temperature field */
    private Field dew;

    /** bouyancy profile */
    private Field buoyProfile;

    /** dirty flag */
    private boolean dirty;

    /** missing trajectory */
    static final FlatField missingTraj;

    /** missing temperature */
    static final FlatField missingTemp;

    /** missing dewpoint */
    static final FlatField missingDew;

    /** missing buoyance profile */
    static final FlatField missingBuoyProfile;

    static {
        FlatField mtraj = null;
        FlatField mtemp = null;
        FlatField mdew  = null;
        FlatField mbp   = null;

        try {
            mtemp =
                (FlatField) AirTemperatureProfile.instance().missingData();
            mtraj = mtemp;
            mdew  = mtemp;
            mbp = (FlatField) new FunctionType(
                AirPressure.getRealTupleType(),
                CapeBean.massicVolume).missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }

        missingTraj        = mtraj;
        missingTemp        = mtemp;
        missingDew         = mdew;
        missingBuoyProfile = mbp;
    }

    /**
     * The name of the output property.
     */
    public static final String OUTPUT_PROPERTY_NAME = "buoyancy profile";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network               The bean network.
     */
    public BuoyancyProfileBean(BeanNetwork network) {

        super(network);

        traj        = missingTraj;
        temp        = missingTemp;
        dew         = missingDew;
        buoyProfile = missingBuoyProfile;
        dirty       = false;
    }

    /**
     * Sets the input, in-situ air temperature profile.
     *
     * @param temp                  The input, in-situ air temperature profile.
     * @throws TypeException        if the {@link visad.MathType} of the profile isn't
     *                              {@link ucar.visad.functiontypes.AirTemperatureProfile#instance()}.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setTemperatureProfile(Field temp)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(AirTemperatureProfile.instance(), temp);

        this.temp = temp;
        dirty     = true;
    }

    /**
     * Sets the input, in-situ dew-point profile.
     *
     * @param dew                   The input, in-situ dew-point profile.
     * @throws TypeException        if the {@link visad.MathType} of the profile isn't
     *                              {@link ucar.visad.functiontypes.DewPointProfile#instance()}.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setDewPointProfile(Field dew)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(DewPointProfile.instance(), dew);

        this.dew = dew;
        dirty    = true;
    }

    /**
     * Sets the air parcel trajectory.
     *
     * @param traj                  The input, air parcel trajectory.
     * @throws TypeException        if the {@link visad.MathType} of the profile isn't
     *                              {@link ucar.visad.functiontypes.AirTemperatureProfile#instance()}.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setParcelTrajectory(Field traj)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(AirTemperatureProfile.instance(), traj);

        this.traj = traj;
        dirty     = true;
    }

    /**
     * Computes the output buoyancy-profile.  A {@link java.beans.PropertyChangeEvent} is
     * fired for the output property if it differs from the previous value.
     *
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    void clock() throws TypeException, VisADException, RemoteException {

        Field oldValue;
        Field newValue;

        synchronized (this) {
            if ( !dirty) {
                oldValue = buoyProfile;
                newValue = buoyProfile;
            } else {
                Field envVirtTemp =
                    (Field) VirtualTemperature.createFromDewPoint(
                        temp.getDomainSet(), temp, dew);
                Field envVirtPotTemp =
                    (Field) VirtualPotentialTemperature.createFromDewPoint(
                        temp.getDomainSet(), temp, dew);

                newValue = (Field) VisADMath
                    .divide(VisADMath
                        .subtract(SaturationVirtualPotentialTemperature
                            .create(traj
                                .getDomainSet(), traj), envVirtPotTemp), VisADMath
                                    .multiply(AirDensity
                                        .create(envVirtTemp
                                            .getDomainSet(), envVirtTemp), envVirtPotTemp));
                oldValue    = buoyProfile;
                buoyProfile = newValue;
                dirty       = false;
            }
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldValue, newValue);
    }

    /**
     * Returns the value of the output buoyancy-profile.  The data is not
     * copied.
     *
     * @return                  The value of the output buoyancy-profile.
     */
    public synchronized Field getBuoyancyProfile() {
        return buoyProfile;
    }
}








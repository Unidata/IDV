/*
 * $Id: CapeBean.java,v 1.6 2005/05/13 18:33:25 jeffmc Exp $
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

import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.quantities.AirDensity;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.CAPE;
import ucar.visad.quantities.Pressure;
import ucar.visad.quantities.SaturationVirtualPotentialTemperature;
import ucar.visad.quantities.VirtualPotentialTemperature;
import ucar.visad.quantities.VirtualTemperature;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.Field;

import visad.Real;

import visad.RealType;

import visad.TypeException;

import visad.VisADException;

import visad.MathType;

import visad.FunctionType;

import visad.FlatField;

import visad.RealTupleType;

import visad.Unit;

import visad.SI;


/**
 * A Java Bean that computes the Convective Available Potential Energy (CAPE)
 * an atmospheric buoyancy-profile.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:25 $
 */
public final class CapeBean extends ClockedBean {

    /** CAPE value */
    private Real cape;

    /** buoyancy profile */
    private Field buoyProfile;

    /** missing CAPE */
    static final Real missingCape;

    /** volume type */
    static final RealType massicVolume;

    static {
        RealType mv = null;
        Real     mc = null;

        try {
            mv = RealType.getRealType("MassicVolume",
                                      SI.meter.pow(3).divide(SI.kilogram));
            mc = (Real) CAPE.getRealType().missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }

        massicVolume = mv;
        missingCape  = mc;
    }

    /**
     * The name of the output property.
     */
    public static final String OUTPUT_PROPERTY_NAME = "cape";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network              The bean network.
     */
    public CapeBean(BeanNetwork network) {

        super(network);

        cape        = missingCape;
        buoyProfile = BuoyancyProfileBean.missingBuoyProfile;
    }

    /**
     * Sets the input, buoyancy profile.
     *
     * @param buoy                  The input, buoyancy profile.
     * @throws TypeException        if the domain quantity isn't pressure or the
     *                              range quantity isn't volume per mass.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void setBuoyancyProfile(Field buoy)
            throws TypeException, VisADException, RemoteException {

        FunctionType  funcType   = (FunctionType) buoy.getType();
        RealTupleType domainType = funcType.getDomain();

        if ( !Pressure.getRealType().equalsExceptNameButUnits(domainType)) {
            throw new TypeException(domainType.toString());
        }

        MathType rangeType = funcType.getRange();

        if ( !massicVolume.equalsExceptNameButUnits(rangeType)) {
            throw new TypeException(rangeType.toString());
        }

        buoyProfile = buoy;
    }

    /**
     * Computes the output property.  A {@link java.beans.PropertyChangeEvent} is fired
     * for the output property if it differs from the previous value.
     *
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occors.
     */
    void clock() throws VisADException, RemoteException {

        FunctionType funcType    = (FunctionType) buoyProfile.getType();
        float[][]    rangeValues = buoyProfile.getFloats(true);
        Field buoy = new FlatField(funcType, buoyProfile.getDomainSet());
        Real         oldCape;
        Real         newCape;

        synchronized (this) {

            /*
             * Replace missing and negative buoyancies with zero
             * buoyancy.
             */
            {
                float[] buoyancies = rangeValues[0];

                for (int i = 0; i < buoyancies.length; ++i) {
                    if ((buoyancies[i] != buoyancies[i])
                            || (buoyancies[i] < 0)) {
                        buoyancies[i] = 0;
                    }
                }

                rangeValues =
                    Unit.convertTuple(rangeValues,
                                      buoy.getDefaultRangeUnits(),
                                      Util.getRangeUnits((FlatField) buoy));

                buoy.setSamples(rangeValues);
            }

            /*
             * Integrate the buoyancy profile to compute the new CAPE.
             */
            {
                FlatField capeProfile =
                    VisADMath.curveIntegralOfGradient((FlatField) buoy);

                newCape = (Real) Util.clone(
                    (Real) capeProfile.getSample(
                        capeProfile.getLength()
                        - 1), ucar.visad.quantities.CAPE.getRealType());
            }

            oldCape = cape;
            cape    = newCape;
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldCape, newCape);
    }

    /**
     * Returns the value of the output CAPE property.  The data is not
     * copied.
     *
     * @return                  The value of the output buoyancy-profile.
     */
    public synchronized Real getCape() {
        return cape;
    }
}








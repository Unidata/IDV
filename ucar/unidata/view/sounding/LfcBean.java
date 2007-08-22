/*
 * $Id: LfcBean.java,v 1.6 2005/05/13 18:33:32 jeffmc Exp $
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

import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.Pressure;

import visad.Field;

import visad.Real;

import visad.RealType;

import visad.TypeException;

import visad.VisADException;

import visad.MathType;

import visad.FunctionType;

import visad.RealTupleType;

import visad.Set;

import visad.Unit;


/**
 * A Java Bean that computes the Level of Free Convection (LFC) from
 * an atmospheric buoyancy-profile.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:32 $
 */
public final class LfcBean extends ClockedBean {

    /** Level of Free Convection (LFC) */
    private Real lfc;

    /** buoyancy pressure */
    private Field buoyProfile;

    /** missing LFC */
    private static final Real missingLfc;

    static {
        Real mLfc = null;

        try {
            mLfc = (Real) RealType.getRealType(
                "LevelOfFreeConvection",
                AirPressure.getRealType().getDefaultUnit()).missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }

        missingLfc = mLfc;
    }

    /**
     * The name of the output property.
     */
    public static final String OUTPUT_PROPERTY_NAME = "lfc";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network               The bean network.
     */
    public LfcBean(BeanNetwork network) {

        super(network);

        buoyProfile = BuoyancyProfileBean.missingBuoyProfile;
        lfc         = missingLfc;
    }

    /**
     * Sets the input, buoyancy profile.
     *
     * @param buoyProfile           The input, buoyancy profile.
     * @throws TypeException        if the domain quantity isn't pressure or the
     *                              range quantity isn't volume per mass.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void setBuoyancyProfile(Field buoyProfile)
            throws TypeException, VisADException, RemoteException {

        FunctionType  funcType   = (FunctionType) buoyProfile.getType();
        RealTupleType domainType = funcType.getDomain();

        if ( !Pressure.getRealType().equalsExceptNameButUnits(domainType)) {
            throw new TypeException(domainType.toString());
        }

        MathType rangeType = funcType.getRange();

        if ( !CapeBean.massicVolume.equalsExceptNameButUnits(rangeType)) {
            throw new TypeException(rangeType.toString());
        }

        this.buoyProfile = buoyProfile;
    }

    /**
     * Computes the output property.  A {@link java.beans.PropertyChangeEvent}
     * is fired for the output property if it differs from the previous value.
     *
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI exception occurs.
     */
    void clock() throws VisADException, RemoteException {

        Set      domainSet = buoyProfile.getDomainSet();
        Real     oldLfc;
        Real     newLfc;
        double[] pressures = domainSet.getDoubles()[0];
        float[]  buoys     = buoyProfile.getFloats()[0];

        /* Eliminate non-finite pressures and buoyancies. */
        int n = 0;

        for (int i = 0; i < pressures.length; i++) {
            if ((pressures[i] != pressures[i]) || (buoys[i] != buoys[i])) {
                n++;
            }
        }

        if (n > 0) {
            double[] tmpPres = new double[pressures.length - n];
            float[]  tmpBuoy = new float[tmpPres.length];

            n = 0;

            for (int i = 0; i < pressures.length; i++) {
                if ((pressures[i] != pressures[i])
                        || (buoys[i] != buoys[i])) {
                    continue;
                }

                tmpPres[n] = pressures[i];
                tmpBuoy[n] = buoys[i];

                n++;
            }

            pressures = tmpPres;
            buoys     = tmpBuoy;
        }

        if (pressures.length <= 1) {
            newLfc = missingLfc;
        } else {
            Unit    presUnit  = domainSet.getSetUnits()[0];
            boolean ascending = pressures[0] > pressures[1];

            if ( !ascending) {

                /*
                 * The profile is descending.  Make the temporary value arrays
                 * ascending.
                 */
                for (int i = 0, j = pressures.length;
                        i < pressures.length / 2; i++) {
                    --j;

                    double pres = pressures[i];

                    pressures[i] = pressures[j];
                    pressures[j] = pres;

                    float buoy = buoys[i];

                    buoys[i] = buoys[j];
                    buoys[j] = buoy;
                }
            }

            /*
             * Descend from the top to positive buoyancy.
             */
            int i = buoys.length;

            while ((--i >= 0) && (buoys[i] <= 0));

            if (i < 0) {

                /*
                 * There is no positively buoyant region.
                 */
                newLfc = missingLfc;
            } else {

                /*
                 * Descend to first non-positive buoyant region.
                 */
                while ((--i >= 0) && (buoys[i] > 0));

                if (i < 0) {

                    /*
                     * There is no non-positive buoyant region.
                     */
                    newLfc = missingLfc;
                } else {

                    /*
                     * Interpolate the LFC.
                     */
                    double pressure =
                        pressures[i + 1]
                        / Math.exp(
                            buoys[i + 1]
                            * (Math.log(pressures[i] / pressures[i + 1])
                               / (buoys[i] - buoys[i + 1])));

                    newLfc = new Real((RealType) missingLfc.getType(),
                                      pressure, presUnit);
                }
            }
        }

        synchronized (this) {
            oldLfc = lfc;
            lfc    = newLfc;
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldLfc, newLfc);
    }

    /**
     * Returns the value of the output LFC property.
     *
     * @return                  The value of the output buoyancy-profile.
     */
    public synchronized Real getLfc() {
        return lfc;
    }
}








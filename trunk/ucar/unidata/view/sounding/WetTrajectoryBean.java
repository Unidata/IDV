/*
 * $Id: WetTrajectoryBean.java,v 1.6 2005/05/13 18:33:40 jeffmc Exp $
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

import ucar.visad.Util;
import ucar.visad.VisADMath;
import ucar.visad.display.*;
import ucar.visad.functiontypes.AirTemperatureProfile;
import ucar.visad.quantities.*;

import visad.*;


/**
 * A Java Bean that computes the trajectory of a parcel of saturated air lifted
 * pseudo-adiabatically from its saturation point.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:40 $
 */
public class WetTrajectoryBean extends ClockedBean {

    /** LCL temperature value */
    private Real lclTemp;

    /** LCL pressure value */
    private Real lclPres;

    /** minimum pressure value */
    private Real minPres;

    /** moist adiabatic trajectory */
    private FlatField wetTraj;

    /** factory */
    private TemperatureCalculatorFactory factory;

    /** dirty flag */
    private boolean dirty;

    /** missing LCL temp value */
    private static final Real missingLclTemp;

    /** missing LCL pressure value */
    private static final Real missingLclPres;

    /** missing min pressure value */
    private static final Real missingMinPres;

    /** missing trajectory */
    private static final FlatField missingWetTraj;

    /** log of max pressure ratio */
    private static final double logMaxPresRatio;

    /** pressure ratio type */
    private static final RealType pressureRatioType;

    /** trajectory type */
    private static final FunctionType trajectoryType;

    static {
        Real         mspt = null;
        Real         mspp = null;
        Real         mmp  = null;
        FlatField    f    = null;
        RealType     prt  = null;
        FunctionType tt   = null;

        try {
            mspt = (Real) AirTemperature.getRealType().missingData();
            mspp = (Real) AirPressure.getRealType().missingData();
            mmp  = mspp;
            tt   = AirTemperatureProfile.instance();
            f    = (FlatField) tt.missingData();
            prt = RealType.getRealType("TrajectoryPressureRatio",
                                       CommonUnit.dimensionless, null);
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }

        missingLclTemp    = mspt;
        missingLclPres    = mspp;
        missingMinPres    = mmp;
        trajectoryType    = tt;
        missingWetTraj    = f;
        logMaxPresRatio   = Math.log(0.95);
        pressureRatioType = prt;
    }

    /**
     * The name of the saturation-point pressure property.
     */
    public static final String OUTPUT_PROPERTY_NAME = "wetTrajectory";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network               The bean network.
     */
    public WetTrajectoryBean(BeanNetwork network) {

        super(network);

        lclTemp = missingLclTemp;
        lclPres = missingLclPres;
        minPres = missingMinPres;
        wetTraj = missingWetTraj;
        factory = DefaultWetTemperatureCalculatorFactory.instance();
        dirty   = false;
    }

    /**
     * Sets the input saturation-point temperature.  The data is not copied.
     *
     * @param lclTemp          The saturation-point temperature.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the temperature has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setSaturationPointTemperature(Real lclTemp)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirTemperature.getRealType(),
                     lclTemp);

        this.lclTemp = lclTemp;
        dirty        = true;
    }

    /**
     * Sets the input saturation-point pressure.  The data is not copied.
     *
     * @param lclPres               The LCL pressure.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the pressure has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setSaturationPointPressure(Real lclPres)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirPressure.getRealType(),
                     lclPres);

        this.lclPres = lclPres;
        dirty        = true;
    }

    /**
     * Sets the input minimum pressure to which to lift the parcel.  The data
     * is not copied.
     *
     * @param minPres               The minimum pressure pressure.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the pressure has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setMinimumPressure(Real minPres)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirPressure.getRealType(),
                     minPres);

        this.minPres = minPres;
        dirty        = true;
    }

    /**
     * Computes the output, saturated, pseudo-adiabatic trajectory property from
     * the input data.  A {@link java.beans.PropertyChangeEvent} is fired
     * for the output property if it differs from the previous value.
     *
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    void clock() throws TypeException, VisADException, RemoteException {

        FlatField oldValue;
        FlatField newValue;

        synchronized (this) {
            if ( !dirty) {
                oldValue = wetTraj;
                newValue = wetTraj;
            } else {
                Unit   presUnit  = AirPressure.getRealType().getDefaultUnit();
                double endPres   = minPres.getValue(presUnit);
                double startPres = lclPres.getValue(presUnit);

                if (endPres >= startPres) {
                    newValue = missingWetTraj;
                } else {
                    double logPresExtent = Math.log(endPres / startPres);
                    double logPresRatio = logPresExtent
                                          / Math.ceil(logPresExtent
                                                      / (logMaxPresRatio));
                    int count = 1 + (int) Math.round(logPresExtent
                                                     / logPresRatio);
                    Real pressureRatio = new Real(pressureRatioType,
                                                  Math.exp(logPresRatio));
                    float[] pressures    = new float[count];
                    float[] temperatures = new float[count];
                    Real    pressure     = lclPres;
                    Real    temperature  = lclTemp;
                    TemperatureCalculator calculator =
                        factory.newTemperatureCalculator(lclPres, lclTemp);

                    pressures[0] = (float) pressure.getValue(presUnit);

                    Unit tempUnit =
                        AirTemperature.getRealType().getDefaultUnit();

                    temperatures[0] = (float) temperature.getValue(tempUnit);

                    for (int i = 1; i < count; ++i) {
                        pressure = (Real) pressure.multiply(pressureRatio);
                        temperature  = calculator.nextTemperature(pressure);
                        pressures[i] = (float) pressure.getValue(presUnit);
                        temperatures[i] =
                            (float) temperature.getValue(tempUnit);
                    }

                    newValue = new FlatField(trajectoryType,
                                             (pressures.length == 1)
                                             ? (Set) new SingletonSet(AirPressure.getRealTupleType(),
                                             new double[]{ pressures[0] },
                                             (CoordinateSystem) null,
                                             new Unit[]{ presUnit },
                                             (ErrorEstimate[]) null)
                                             : new Gridded1DSet(
                                                 AirPressure.getRealTupleType(),
                                                 new float[][] {
        pressures
    }, pressures.length, (CoordinateSystem) null, new Unit[]{ presUnit }, (ErrorEstimate[]) null), (CoordinateSystem[]) null,
                            (Set[]) null, new Unit[]{ tempUnit });

                    newValue.setSamples(new float[][] {
                        temperatures
                    });
                }

                oldValue = wetTraj;
                wetTraj  = newValue;
                dirty    = false;
            }
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldValue, newValue);
    }

    /**
     * Returns the value of the saturated, pseudo-adiabatic trajectory property.
     * The data is not copied.
     *
     * @return                  The value of the saturated, pseudo-adiabatic
     *                          trajectory property.
     */
    public synchronized Data getWetTrajectory() {
        return wetTraj;
    }
}








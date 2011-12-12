/*
 * $Id: LclTemperatureBean.java,v 1.6 2005/05/13 18:33:32 jeffmc Exp $
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
import ucar.visad.quantities.*;

import visad.*;


/**
 * A Java Bean that computes the temperature of the saturation-point from an
 * initial pressure, temperature, and water-vapor mixing-ratio.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:32 $
 */
public final class LclTemperatureBean extends ClockedBean {

    /** ratio */
    private Real ratio;

    /** initial pressure */
    private Real initPres;

    /** initial temperature */
    private Real initTemp;

    /** LCL temp */
    private Real lclTemp;

    /** dirty flag */
    private boolean dirty;

    /** missing temperature */
    private static final Real missingTemp;

    /** missing pressure */
    private static final Real missingPres;

    /** missing ratio */
    private static final Real missingRatio;

    /** missing LCL temp */
    private static final Real missingLclTemp;

    static {
        Real mt   = null;
        Real mp   = null;
        Real mr   = null;
        Real mspt = null;

        try {
            mt = (Real) AirTemperature.getRealType().missingData();
            mp = (Real) AirPressure.getRealType().missingData();
            mr = (Real) WaterVaporMixingRatio.getRealType().missingData();
            mspt = (Real) SaturationPointTemperature.getRealType()
                .missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }

        missingTemp    = mt;
        missingPres    = mp;
        missingRatio   = mr;
        missingLclTemp = mspt;
    }

    /**
     * The name of the saturation-point temperature property.
     */
    public static final String OUTPUT_PROPERTY_NAME =
        "saturationPointTemperature";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network                 The bean network.
     */
    public LclTemperatureBean(BeanNetwork network) {

        super(network);

        initTemp = missingTemp;
        initPres = missingPres;
        ratio    = missingRatio;
        lclTemp  = missingLclTemp;
        dirty    = false;
    }

    /**
     * Sets the input water-vapor mixing-ratio.  The data is not copied.
     *
     * @param ratio                 The input mixing-ratio.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the profile has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setWaterVaporMixingRatio(Real ratio)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(
            ucar.visad.quantities.WaterVaporMixingRatio.getRealType(), ratio);

        this.ratio = ratio;
        dirty      = true;
    }

    /**
     * Sets the initial pressure.  The data is not copied.
     *
     * @param initPres              The initial pressure.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the pressure has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setPressure(Real initPres)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirPressure.getRealType(),
                     initPres);

        this.initPres = initPres;
        dirty         = true;
    }

    /**
     * Sets the initial temperature.  The data is not copied.
     *
     * @param initTemp              The initial temperature.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the temperature has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setTemperature(Real initTemp)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirTemperature.getRealType(),
                     initTemp);

        this.initTemp = initTemp;
        dirty         = true;
    }

    /**
     * Computes the saturation-point temperature property from the input
     * data.  A {@link java.beans.PropertyChangeEvent} is fired for the output
     * property if it differs from the previous value.
     *
     * @throws TypeException        if the pressure has the wrong type.
     * @throws VisADException       VisAD failure.
     * @throws RemoteException      Java RMI failure.
     */
    public void clock()
            throws TypeException, VisADException, RemoteException {

        Real oldValue;
        Real newValue;

        synchronized (this) {
            if ( !dirty) {
                oldValue = lclTemp;
                newValue = lclTemp;
            } else {
                oldValue = lclTemp;
                newValue = (Real) SaturationPointTemperature.create(initPres,
                        initTemp, ratio);
                lclTemp = newValue;
                dirty   = false;
            }
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldValue, newValue);
    }

    /**
     * Returns the value of the saturation-point temperature property.  The
     * data is not copied.
     *
     * @return                  The value of the saturation-point temperature
     *                          property.
     */
    public synchronized Data getSaturationPointTemperature() {
        return lclTemp;
    }
}








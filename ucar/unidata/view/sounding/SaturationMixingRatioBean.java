/*
 * $Id: SaturationMixingRatioBean.java,v 1.6 2005/05/13 18:33:36 jeffmc Exp $
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
 * A Java Bean that computes saturation mixing ratio from a pressure and the
 * dew-point at that pressure.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:36 $
 */
public final class SaturationMixingRatioBean extends ClockedBean {

    /** dewpoint value */
    private Real dewPoint;

    /** pressure value */
    private Real pressure;

    /** saturation mixing ratio */
    private Real wSat;

    /** dirty flag */
    private boolean dirty;

    /** missing dewpoint value */
    static final Real missingDewPoint;

    /** missing pressure value */
    static final Real missingPres;

    /** missing saturation mixing ratio value */
    static final Real missingWsat;

    static {
        Real mdp = null;
        Real mp  = null;
        Real mws = null;

        try {
            mdp = (Real) DewPoint.getRealType().missingData();
            mp  = (Real) AirPressure.getRealType().missingData();
            mws = (Real) WaterVaporMixingRatio.getRealType().missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }

        missingDewPoint = mdp;
        missingPres     = mp;
        missingWsat     = mws;
    }

    /**
     * The name of the saturation mixing-ratio output-property.
     */
    public static final String OUTPUT_PROPERTY_NAME = "saturationMixingRatio";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network               The bean network.
     */
    public SaturationMixingRatioBean(BeanNetwork network) {

        super(network);

        dewPoint = missingDewPoint;
        pressure = missingPres;
        wSat     = missingWsat;
        dirty    = false;
    }

    /**
     * Sets the input dew-point.  The data is not copied.
     *
     * @param dewPoint              The input dew-point.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the profile has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setDewPoint(Real dewPoint)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.DewPoint.getRealType(), dewPoint);

        this.dewPoint = dewPoint;
        dirty         = true;
    }

    /**
     * Sets the input pressure.  The data is not copied.
     *
     * @param pressure              The input pressure.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the pressure has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setPressure(Real pressure)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirPressure.getRealType(),
                     pressure);

        this.pressure = pressure;
        dirty         = true;
    }

    /**
     * Computes the saturation mixing-ratio property from the input
     * data.  A {@link java.beans.PropertyChangeEvent} is fired for the output
     * property if it differs from the previous value.
     *
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    void clock() throws TypeException, VisADException, RemoteException {

        Real oldValue;
        Real newValue;

        synchronized (this) {
            if ( !dirty) {
                oldValue = wSat;
                newValue = wSat;
            } else {
                newValue = (Real) WaterVaporMixingRatio.create(pressure,
                        dewPoint);
                oldValue = wSat;
                wSat     = newValue;
                dirty    = false;
            }
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldValue, newValue);
    }

    /**
     * Returns the value of the saturation mixing-ratio property.
     *
     * @return                  The value of the saturation mixing-ratio
     *                          property.
     */
    public synchronized Data getSaturationMixingRatio() {
        return wSat;
    }
}








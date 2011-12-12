/*
 * $Id: LclPressureBean.java,v 1.6 2005/05/13 18:33:31 jeffmc Exp $
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
 * A Java Bean that computes the pressure of the saturation-point from an
 * initial pressure and temperature, and a saturation-point pressure
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:31 $
 */
public final class LclPressureBean extends ClockedBean {

    /** inital pressure */
    private Real initPres;

    /** intial temperature */
    private Real initTemp;

    /** LCL temp */
    private Real lclTemp;

    /** LCL pressure */
    private Real lclPres;

    /** dirty flag */
    private boolean dirty;

    /** missing initial pressure */
    static final Real missingInitPres;

    /** missing initial temperature */
    static final Real missingInitTemp;

    /** missing LCL temperature */
    static final Real missingLclTemp;

    /** missing LCL pressure */
    static final Real missingLclPres;

    static {
        Real mip  = null;
        Real mit  = null;
        Real mspt = null;
        Real mspp = null;

        try {
            mip  = (Real) AirPressure.getRealType().missingData();
            mit  = (Real) AirTemperature.getRealType().missingData();
            mspt = mit;
            mspp = mip;
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }

        missingInitPres = mip;
        missingInitTemp = mit;
        missingLclTemp  = mspt;
        missingLclPres  = mspp;
    }

    /**
     * The name of the saturation-point pressure property.
     */
    public static final String OUTPUT_PROPERTY_NAME =
        "saturationPointPressure";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network               The bean network.
     */
    public LclPressureBean(BeanNetwork network) {

        super(network);

        initPres = missingInitPres;
        initTemp = missingInitTemp;
        lclTemp  = missingLclTemp;
        lclPres  = missingLclPres;
        dirty    = false;
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
     * @param initTemp            The initial temperature.
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
     * Sets the saturation-point temperature.  The data is not copied.
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
     * Computes the output, saturation-point pressure property from the input
     * data.  A {@link java.beans.PropertyChangeEvent} is fired for the output
     * property if it differs from the previous value.
     *
     * @throws TypeException        if the pressure has the wrong type.
     * @throws VisADException       VisAD failure.
     * @throws RemoteException      Java RMI failure.
     */
    void clock() throws TypeException, VisADException, RemoteException {

        Real oldValue;
        Real newValue;

        synchronized (this) {
            if ( !dirty) {
                oldValue = lclPres;
                newValue = lclPres;
            } else {
                oldValue = lclPres;
                newValue = (Real) SaturationPointPressure.create(initPres,
                        initTemp, lclTemp);
                lclPres = newValue;
                dirty   = false;
            }
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldValue, newValue);
    }

    /**
     * Returns the value of the saturation-point pressure property.  The
     * data is not copied.
     *
     * @return                  The value of the saturation-point pressure
     *                          property.
     */
    public synchronized Data getSaturationPointPressure() {
        return lclPres;
    }
}








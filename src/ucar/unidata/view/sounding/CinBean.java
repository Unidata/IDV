/*
 * $Id: CinBean.java,v 1.6 2005/05/13 18:33:26 jeffmc Exp $
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

import ucar.visad.quantities.CAPE;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.Pressure;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.MathType;

import visad.RealTupleType;

import visad.TypeException;

import visad.Real;

import visad.RealType;

import visad.VisADException;


/**
 * A Java Bean that computes the Convective INhibition (CIN) from a buoyancy
 * profile, a starting pressure, and the pressure at the Level of Free
 * Convection (LFC).
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:26 $
 */
public final class CinBean extends ClockedBean {

    /** buoyancy profile */
    private Field buoyProfile;

    /** initial pressure */
    private Real initPres;

    /** Level of Free Convection pressure */
    private Real lfcPres;

    /** CIN value */
    private Real cin;

    /** dirty flag */
    private boolean dirty;

    /** missing buoyancy profile */
    private static final Field missingBuoyProfile;

    /** missing initial pressure */
    private static final Real missingInitPres;

    /** missing LFC pressure */
    private static final Real missingLfcPres;

    /** missing CIN */
    private static final Real missingCin;

    static {
        Real mcin = null;
        Real mip  = null;

        try {
            mcin = (Real) RealType.getRealType(
                "CIN", CAPE.getRealType().getDefaultUnit()).missingData();
            mip = (Real) AirPressure.getRealType().missingData();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }

        missingBuoyProfile = BuoyancyProfileBean.missingBuoyProfile;
        missingInitPres    = mip;
        missingLfcPres     = missingInitPres;
        missingCin         = mcin;
    }

    /**
     * The name of the output property.
     */
    public static final String OUTPUT_PROPERTY_NAME = "cin";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network               The bean network.
     */
    public CinBean(BeanNetwork network) {

        super(network);

        buoyProfile = missingBuoyProfile;
        initPres    = missingInitPres;
        lfcPres     = missingLfcPres;
        cin         = missingCin;
        dirty       = false;
    }

    /**
     * Sets the input, buoyancy profile.  Because this bean only has one input,
     * an immediate computation of the output property is performed.  A {@link
     * java.beans.PropertyChangeEvent} is fired for the output property if
     * it differs from the previous value.
     *
     * @param buoyProfile           The input buoyancy-profile.
     * @throws TypeException        if the domain quantity of the profile isn't
     *                              pressure or the range quantity of the
     *                              profile isn't massic volume.
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

        synchronized (this) {
            this.buoyProfile = buoyProfile;
            dirty            = true;
        }
    }

    /**
     * Sets the initial, starting pressure.
     *
     * @param initPres              The initial, starting pressure.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the pressure has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setInitialPressure(Real initPres)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirPressure.getRealType(),
                     initPres);

        this.initPres = initPres;
        dirty         = true;
    }

    /**
     * Sets the pressure at the Level of Free Convection (LFC).
     *
     * @param lfcPres               The pressure at the LFC.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the pressure has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setLfcPressure(Real lfcPres)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirPressure.getRealType(),
                     lfcPres);

        this.lfcPres = lfcPres;
        dirty        = true;
    }

    /**
     * Computes the output Convective INhibition (CIN).  A {@link
     * PropertyChangeEvent} is fired for the output property if it differs from
     * the previous value.
     *
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    void clock() throws TypeException, VisADException, RemoteException {

        Real oldCin;
        Real newCin;

        synchronized (this) {
            if ( !dirty) {
                oldCin = cin;
                newCin = cin;
            } else {
                FlatField integral =
                    VisADMath.curveIntegralOfGradient(buoyProfile);

                newCin = (Real) integral.evaluate(lfcPres).subtract(
                    integral.evaluate(initPres));
                oldCin = cin;
                cin    = newCin;
                dirty  = false;
            }
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldCin, newCin);
    }

    /**
     * Returns the value of the output Convective INhibition (CIN) property.
     *
     * @return                  The CIN value.
     */
    public synchronized Real getCin() {
        return cin;
    }
}








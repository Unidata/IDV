/*
 * $Id: DewPointExtractorBean.java,v 1.8 2005/05/13 18:33:28 jeffmc Exp $
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

import ucar.visad.quantities.DewPoint;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.Pressure;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.DataReferenceImpl;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.MathType;

import visad.RealTupleType;

import visad.ActionImpl;

import visad.ThingChangedEvent;

import visad.ThingReferenceImpl;

import visad.TypeException;

import visad.Real;

import visad.RealType;

import visad.VisADException;


/**
 * A Java Bean that extracts the dew-point temperature from a dew-point
 * temperature profile at a given pressure.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2005/05/13 18:33:28 $
 */
public final class DewPointExtractorBean extends ClockedBean {

    /** dewpoint profile */
    private Field dewProfile;

    /** pressure */
    private Real pres;

    /** dewpoint a pressure */
    private Real dewPoint;

    /** dirty flag */
    private boolean dirty;

    /** dewpoint profile reference */
    private ThingReferenceImpl dewProfileRef;

    /** missing dewpoint profile */
    static final Field missingDewProfile;

    /** missing pressure */
    static final Real missingPres;

    /** missing dewpoint */
    static final Real missingDewPoint;

    static {
        missingPres       = SaturationMixingRatioBean.missingPres;
        missingDewProfile = BuoyancyProfileBean.missingDew;
        missingDewPoint   = SaturationMixingRatioBean.missingDewPoint;
    }

    /**
     * The name of the output property.
     */
    public static final String OUTPUT_PROPERTY_NAME = "dew point";

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network             The bean network.
     */
    public DewPointExtractorBean(BeanNetwork network) {

        super(network);

        dewProfile = missingDewProfile;
        pres       = missingPres;
        dewPoint   = missingDewPoint;
        dirty      = true;

        ThingReferenceImpl dpr = null;

        try {
            dpr = new MyThingReferenceImpl(
                "DewPointExtractorProfileChangeRef");
        } catch (VisADException ex) {}  // Exception thrown if name is null.

        dewProfileRef = dpr;
    }

    /**
     * Class MyThingReferenceImpl
     *
     * @author Unidata development team
     */
    private class MyThingReferenceImpl extends ThingReferenceImpl {

        /**
         * Create a new MyThingReferenceImpl
         *
         * @param name  name of reference
         *
         * @throws VisADException  problem creating the ref
         *
         */
        public MyThingReferenceImpl(String name) throws VisADException {
            super(name);
        }

        /**
         * Respond to changes
         * @return 0
         */
        public long incTick() {

            dewProfileChanged();

            return 0L;
        }
    }

    /**
     * Sets the input, dew-point profile.
     *
     * @param dewProfile            The input dew-point profile.
     * @throws TypeException        if the domain quantity of the profile isn't
     *                              pressure or the range quantity of the
     *                              profile isn't dew-point temperature.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void setDewPointProfile(Field dewProfile)
            throws TypeException, VisADException, RemoteException {

        FunctionType  funcType   = (FunctionType) dewProfile.getType();
        RealTupleType domainType = funcType.getDomain();

        if ( !AirPressure.getRealTupleType().equalsExceptNameButUnits(
                domainType)) {
            throw new TypeException(domainType.toString());
        }

        MathType rangeType = funcType.getRange();

        if ( !DewPoint.getRealType().equalsExceptNameButUnits(rangeType)) {
            throw new TypeException(rangeType.toString());
        }

        if (dewProfile != this.dewProfile) {
            synchronized (this) {
                this.dewProfile = dewProfile;

                dewProfileRef.setThing(dewProfile);  // will set "dirty"
            }
        }
    }

    /**
     * Handle changes to the dewpoint profile
     */
    private void dewProfileChanged() {

        synchronized (this) {
            dirty = true;
        }
    }

    /**
     * Sets the extraction pressure.
     *
     * @param pres                  The extraction pressure.
     * @throws NullPointerException if the argument is <code>null</code>.
     * @throws TypeException        if the argument has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public synchronized void setPressure(Real pres)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(ucar.visad.quantities.AirPressure.getRealType(), pres);

        this.pres = pres;
        dirty     = true;
    }

    /**
     * Computes the output dew point.  A {@link java.beans.PropertyChangeEvent}
     * is fired for the output property if it differs from the previous value.
     *
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    void clock() throws TypeException, VisADException, RemoteException {

        Real oldDew;
        Real newDew;

        synchronized (this) {
            if ( !dirty) {
                oldDew = dewPoint;
                newDew = dewPoint;
            } else {
                newDew   = (Real) dewProfile.evaluate(pres);
                oldDew   = dewPoint;
                dewPoint = newDew;
                dirty    = false;
            }
        }

        firePropertyChange(OUTPUT_PROPERTY_NAME, oldDew, newDew);
    }

    /**
     * Returns the value of the output dew-point temperature.
     *
     * @return                  The dew-point temperature value.
     */
    public synchronized Real getDewPoint() {
        return dewPoint;
    }
}








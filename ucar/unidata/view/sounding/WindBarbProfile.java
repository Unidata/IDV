/*
 * $Id: WindBarbProfile.java,v 1.8 2005/05/13 18:33:40 jeffmc Exp $
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



import java.rmi.RemoteException;

import java.util.*;

import java.beans.*;

import ucar.visad.display.*;

import ucar.visad.quantities.CommonUnits;

import ucar.visad.quantities.PolarHorizontalWind;
import ucar.visad.functiontypes.CartesianHorizontalWindOfGeopotentialAltitude;
import ucar.visad.functiontypes.CartesianHorizontalWindOfPressure;

import visad.*;


/**
 * Provides support for the display of a wind profile on in an
 * AerologicalDisplay as a set of as a set of wind barbs.  Winds must
 * be of form CartesianHorizontalWindOfPressure;
 *
 * @author Unidata Development Team
 * @version $Id: WindBarbProfile.java,v 1.8 2005/05/13 18:33:40 jeffmc Exp $
 */
public class WindBarbProfile extends WindProfile {

    /** displayable for profile */
    private WindProfileDisplayable profileDisplayable;

    /** original field */
    private Field originalData;

    /** CoordinateSystem */
    private AerologicalCoordinateSystem aeroCS;

    /** missing wind field value */
    private FlatField missingWindField;

    /** set for decimating data */
    private Gridded1DSet windLevels = null;

    /**
     * Constructs from a VisAD display.
     * @param display           The VisAD display. (not used)
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindBarbProfile(LocalDisplay display)
            throws VisADException, RemoteException {
        this(display, null);
    }

    /**
     * Constructs from a VisAD display.
     * @param display           The VisAD display. (not used)
     * @param cs                The AerologicalCoordinateSystem
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindBarbProfile(LocalDisplay display, AerologicalCoordinateSystem cs)
            throws VisADException, RemoteException {

        profileDisplayable = new WindProfileDisplayable();
        setProfile(getMissingWindField());
        aeroCS = cs;
        addDisplayable(profileDisplayable);

    }

    /**
     * Set the coordinate system for this display.
     *
     * @param acs   the coordinate system
     *
     * @throws RemoteException   Java RMI failure
     * @throws VisADException    VisAD failure
     */
    public void setCoordinateSystem(AerologicalCoordinateSystem acs)
            throws VisADException, RemoteException {
        aeroCS = acs;
        profileDisplayable.setData(convertToYDomain(getProfile()));
    }

    /**
     * Set the coordinate system for this display.
     *
     * @return the coordinate system for this display (may be null)
     */
    public AerologicalCoordinateSystem getCoordinateSystem() {
        return aeroCS;
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected WindBarbProfile(WindBarbProfile that)
            throws RemoteException, VisADException {

        super(that);

        setProfile(that.getProfile());
    }

    /**
     * Returns an instance of a wind field with no values.
     *
     * @return                 A wind field with no values.
     */
    protected FlatField getMissingWindField() {
        if (missingWindField == null) {
            try {
                missingWindField = new FlatField(
                    CartesianHorizontalWindOfPressure.instance());
            } catch (Exception e) {
                missingWindField = null;
            }
        }
        return missingWindField;
    }

    /**
     * Sets the wind profile.
     * @param profile           The wind profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setProfile(Field profile)
            throws VisADException, RemoteException {
        Field transformed = convertToYDomain(profile);
        profileDisplayable.setData(transformed);
        originalData = profile;
    }

    /**
     * Resets the vertical profile of the horizontal wind to the profile of
     * the last setProfile().
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setOriginalProfile() throws VisADException, RemoteException {
        setProfile(originalData);
    }

    /**
     * Returns the wind profile.
     * @return                  The wind profile.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected Field getProfile() throws VisADException, RemoteException {
        return originalData;
    }

    /**
     * Set the levels of the wind profile to display.
     * @param levels  the set of levels (if null, display all);
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setWindLevels(Gridded1DSet levels)
            throws RemoteException, VisADException {
        windLevels = levels;
        profileDisplayable.setData(convertToYDomain(getProfile()));
    }

    /**
     * Sets the wind speed and direction properties.  Override superclass
     * method to evaluate on pressure instead of geopotential altitude
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setSpeedAndDirection()
            throws RemoteException, VisADException {

        if (getPressure().isMissing()) {
            return;
        }
        RealTuple spdDir =
            (RealTuple) ((FunctionImpl) getProfile()).evaluate(getPressure(),
                Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
        if ( !((RealTupleType) spdDir.getType()).equals(
                PolarHorizontalWind.getRealTupleType())) {
            spdDir = PolarHorizontalWind.newRealTuple(spdDir);
        }

        setSpeed((Real) spdDir.getComponent(0));
        setDirection((Real) spdDir.getComponent(1));
    }

    /**
     * Indicates if this instance is identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof WindBarbProfile)) {
            equals = false;
        } else {
            try {
                WindBarbProfile that        = (WindBarbProfile) obj;

                Field           thisProfile = getProfile();
                Field           thatProfile = that.getProfile();

                equals = (this == that) || (((thisProfile == null)
                                             ? thatProfile == null
                                             : thisProfile.equals(
                                                 thatProfile)) && super
                                                     .equals(that));

            } catch (Exception e) {
                System.err.println(getClass().getName() + ".equals(Object): "
                                   + "Couldn't get wind data: " + e);

                equals = false;
            }
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {

        int code;

        try {
            Field profile = getProfile();

            code = ((profile == null)
                    ? 0
                    : profile.hashCode()) ^ super.hashCode();
        } catch (Exception e) {
            System.err.println(getClass().getName() + ".hashCode(): "
                               + "Couldn't get wind data: " + e);

            code = 0;
        }

        return code;
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new WindBarbProfile(this);
    }

    /**
     * Vet the winds
     *
     * @param profile  profile to vet
     * @return vetted profile
     *
     * @throws RemoteException   Java RMI exception
     * @throws VisADException    VisAD problem
     */
    private Field convertToYDomain(Field profile)
            throws VisADException, RemoteException {

        if ((profile == null) || (aeroCS == null)) {
            return profile;
        }
        boolean isSequence = ucar.unidata.data.grid.GridUtil.isTimeSequence(
                                 (FieldImpl) profile);
        FlatField data;
        if (isSequence) {
            data = (FlatField) profile.getSample(0);
        } else {
            data = (FlatField) profile;
        }
        //data = ensureCartesian(data);
        //System.out.println("data type = "+data.getType());
        if (windLevels != null) {
            data = (FlatField) data.resample(windLevels);
        }
        SampledSet pSet      = (SampledSet) data.getDomainSet();
        float[][]  pressures = pSet.getSamples();
        float[][]  pt        = new float[2][pressures[0].length];
        pt[0] = pressures[0];
        float[][] xy = aeroCS.toReference(pt,
                                          new Unit[]{ pSet.getSetUnits()[0],
                                                      CommonUnits.CELSIUS });
        Gridded1DSet ySet = new Gridded1DSet(RealType.YAxis, new float[][] {
            xy[1]
        }, xy[1].length);
        FunctionType ftype =
            new FunctionType(RealType.YAxis,
                             ((FunctionType) data.getType()).getRange());
        FlatField ff = new FlatField(ftype, ySet);
        ff.setSamples(data.getFloats());
        return ff;
    }

}

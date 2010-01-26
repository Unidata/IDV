/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.point;



import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.rmi.RemoteException;


/**
 * Implementation of PointOb as a FieldImpl.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.10 $ $Date: 2006/12/01 20:42:34 $
 */
public class PointObField extends FieldImpl implements PointOb {

    /** ob location */
    private EarthLocation location;

    /** ob time */
    private DateTime dateTime;

    /** ob data */
    private Data data;

    /**
     * Construct a new PointObField from the given location, date/time and data.
     *
     * @param location  location of the observation
     * @param dateTime  date/time of the observation
     * @param data      associated data.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public PointObField(EarthLocation location, DateTime dateTime, Data data)
            throws VisADException, RemoteException {
        super(new FunctionType(
            RealType.Time,
            new FunctionType(
                RealTupleType.LatitudeLongitudeAltitude,
                data.getType())), new SingletonSet(
                    new RealTuple(new Real[] { dateTime })));
        EarthLocationTuple llDomain = (location instanceof EarthLocationTuple)
                                      ? (EarthLocationTuple) location
                                      : new EarthLocationTuple(
                                          location.getLatitude(),
                                          location.getLongitude(),
                                          location.getAltitude());
        FunctionType rangeType =
            (FunctionType) ((FunctionType) getType()).getRange();
        FieldImpl llToData = new FieldImpl(rangeType,
                                           new SingletonSet(llDomain));
        llToData.setSample(0, data, false);
        setSample(0, llToData, false);
        this.dateTime = dateTime;
    }

    /**
     * Get the geolocated location of the observation.
     *
     * @return observation's geolocation or null if there is a problem
     */
    public EarthLocation getEarthLocation() {
        EarthLocation elt = null;
        try {
            Set       lldomain = ((FieldImpl) getSample(0)).getDomainSet();
            RealTuple sample   = visad.util.DataUtility.getSample(lldomain,
                                     0);
            elt = new EarthLocationTuple((Real) sample.getComponent(0),
                                         (Real) sample.getComponent(1),
                                         (Real) sample.getComponent(2));

        } catch (Exception excp) {
            ;
        }
        return elt;
    }

    /**
     * Get the time associated with this observation.
     *
     * @return DateTime for this observation.
     */
    public DateTime getDateTime() {
        return dateTime;
    }

    /**
     * Get the data associated with this object.
     *
     * @return Data for this observation or null if there is a problem
     */
    public Data getData() {
        Data d = null;
        try {
            d = ((FieldImpl) getSample(0)).getSample(0);
        } catch (Exception excp) {
            ;
        }
        return d;
    }

    /**
     * Clones this instance.
     *
     * @return                    A clone of this instance.
     */
    public final Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Assertion failure");
        }
    }

    /**
     * String representation of the point observation.
     *
     * @return this ob as a string.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Point Ob: ");
        buf.append("\tLocation: ");
        buf.append(location.toString());
        buf.append("\n\tDateTime: ");
        buf.append(dateTime.toString());
        buf.append("\n\tData: ");
        buf.append(data.toString());
        return buf.toString();
    }

}

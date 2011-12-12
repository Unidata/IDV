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

package ucar.visad.display;


import ucar.visad.*;



import visad.*;

import visad.data.mcidas.BaseMapAdapter;

import java.awt.Color;

import java.net.URL;

import java.rmi.RemoteException;


/**
 * Provides support for displaying map lines.
 *
 * @author Don Murray
 * @version $Revision: 1.10 $
 */
public class MapLines extends LineDrawing {

    /**
     * The set of map lines.
     */
    private SampledSet mapSet = null;

    /**
     * Constructs an instance with the supplied reference name.
     *
     * @param  name  reference name
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public MapLines(String name) throws VisADException, RemoteException {
        this(name, null);
    }

    /**
     * Constructs an instance from a name and set of map lines.
     *
     * @param  name  reference name
     * @param  mapSet  set of map lines
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     * @exception RemoteException couldn't create the remote object
     */
    public MapLines(String name, SampledSet mapSet)
            throws VisADException, RemoteException {

        super(name);

        if (mapSet != null) {
            setMapLines(mapSet);
        }
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected MapLines(MapLines that) throws RemoteException, VisADException {

        super(that);

        mapSet = that.mapSet;  // immutable object

        if (mapSet != null) {
            setMapLines(mapSet);
        }
    }

    /**
     * Sets the map lines of this instance from a set of Latitude/Longitude
     * points for the map lines.  The set dimension must be 2 and
     * the CoordinateSystem (if one exists) must have a reference of
     * RealType.Latitude and RealType.Longitude
     *
     * @param mapSet  set of map lines
     *
     * @exception VisADException  couldn't create the necessary VisAD object
     *                            or reference is not lat/lon.
     * @exception RemoteException couldn't create the remote object
     */
    public void setMapLines(SampledSet mapSet)
            throws VisADException, RemoteException {

        if (mapSet == null) {
            setData(new Real(0));
            this.mapSet = null;
            return;
        }

        if (mapSet.getDimension() != 2) {
            throw new VisADException("Set dimension must be 2");
        }

        RealTupleType    mType;
        CoordinateSystem cs = mapSet.getCoordinateSystem();

        if (cs == null) {
            mType = ((SetType) mapSet.getType()).getDomain();
        } else {
            mType = mapSet.getCoordinateSystem().getReference();
        }

        if (((RealTupleType) mType).getIndex(RealType.Latitude) == -1
                || ((RealTupleType) mType).getIndex(RealType.Longitude)
                   == -1) {
            throw new VisADException(
                "Set or CoordinateSystem must be values of "
                + "RealType.Latitude and RealType.Longitude");
        }

        this.mapSet = mapSet;

        setData(this.mapSet);
    }

    /**
     * Provide a readable description of this MapLines
     * @return  readable description
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();

        sb.append("Map Name = ");
        sb.append(getName());
        sb.append("\n");
        sb.append("\tColor = ");
        sb.append(getColor().toString());
        sb.append("\n");
        sb.append("\tHas Data = ");
        sb.append( !(mapSet == null)
                   ? "Yes"
                   : "No");
        sb.append("\n");
        sb.append("\tIs Visible = ");
        sb.append((isVisible() == true)
                  ? "Yes"
                  : "No");

        return sb.toString();
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new MapLines(this);
    }
}

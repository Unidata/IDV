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



import visad.*;

import java.awt.Color;

import java.rmi.RemoteException;

import java.util.ArrayList;


/**
 * Displayable to support Latitude and Longitude lines.
 *
 * @author Don Murray, Unidata
 * @version $Revision: 1.9 $
 */
public class LatLonLines extends LineDrawing {

    /** latitude lines */
    private UnionSet latLines;

    /** minimum value */
    private float minValue;

    /** minimum value */
    private float maxValue;

    /** spacing between lines */
    private float spacing;

    /** flag for whether this is latitidue or longitude lines */
    private boolean isLat = false;

    /**
     * Construct a LatLonLine object with default min, max and spacing
     * values.
     *
     * @param  type   lat lines when type = RealType.Latitude, lon lines
     *                when type = RealType.Longitude
     * @throws VisADException  invalid type or can't create local VisAD object
     * @throws RemoteException couldn't create remote VisAD object
     */
    public LatLonLines(RealType type) throws VisADException, RemoteException {

        this(type, (type.equals(RealType.Latitude)
                    ? -90
                    : -180), (type.equals(RealType.Latitude)
                              ? 90
                              : 180), (type.equals(RealType.Latitude)
                                       ? 30
                                       : 45));
    }

    /**
     * Construct a LatLonLine object of the given type.
     *
     * @param  type      lat lines when type = RealType.Latitude, lon lines
     *                   when type = RealType.Longitude
     * @param  minValue  starting line (degrees)
     * @param  maxValue  ending line (degrees)
     * @param  spacing   spacing between lines (degrees)
     *
     * @throws VisADException  invalid type or can't create local VisAD object
     * @throws RemoteException couldn't create remote VisAD object
     */
    public LatLonLines(RealType type, float minValue, float maxValue,
                       float spacing)
            throws VisADException, RemoteException {
        this(type, minValue, maxValue, spacing, true);
    }

    /**
     * Construct a LatLonLine object of the given type.
     *
     * @param  type      lat lines when type = RealType.Latitude, lon lines
     *                   when type = RealType.Longitude
     * @param  minValue  starting line (degrees)
     * @param  maxValue  ending line (degrees)
     * @param  spacing   spacing between lines (degrees)
     * @param  setData   if true, the data will be set on construction
     *
     * @throws VisADException  invalid type or can't create local VisAD object
     * @throws RemoteException couldn't create remote VisAD object
     */
    public LatLonLines(RealType type, float minValue, float maxValue,
                       float spacing, boolean setData)
            throws VisADException, RemoteException {

        super(makeName(type));

        isLat         = type.equals(RealType.Latitude);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.spacing  = spacing;

        createLines(setData);
    }

    /**
     * Constructs from another instance.
     *
     * @param that              The other instance.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected LatLonLines(LatLonLines that)
            throws RemoteException, VisADException {

        super(that);

        latLines = that.latLines;  // immutable
        minValue = that.minValue;  // immutable
        maxValue = that.maxValue;  // immutable
        spacing  = that.spacing;   // immutable
        isLat    = that.isLat;

        createLines();
    }

    /**
     * Create the name based on the type (lat or lon).
     *
     * @param type  type of lines
     * @return a name corresponding to the type
     *
     * @throws VisADException  invalid type (not Latitude or Longitude)
     */
    private static String makeName(RealType type) throws VisADException {

        String name;

        if (type.equals(RealType.Latitude)) {
            name = "LatitudeLines";
        } else if (type.equals(RealType.Longitude)) {
            name = "LongitudeLines";
        } else {
            throw new VisADException("Invalid type for LatLonLines: " + type);
        }

        return name;
    }

    /**
     * Change the line spacing
     *
     * @param  spacing           spacing between lines (degrees)
     *
     * @throws VisADException    couldn't create local VisAD object
     * @throws RemoteException   couldn't create remote VisAD object
     */
    public void setSpacing(float spacing)
            throws VisADException, RemoteException {

        if (spacing == this.spacing) {
            return;
        }

        this.spacing = spacing;

        createLines();
    }

    /**
     * Get the current line spacing.
     *
     * @return  spacing in degrees
     */
    public float getSpacing() {
        return spacing;
    }

    /**
     * Change the starting and ending lines
     *
     * @param  minValue          starting line (degrees)
     * @param  maxValue          ending line (degrees)
     *
     * @throws VisADException    couldn't create local VisAD object
     * @throws RemoteException   couldn't create remote VisAD object
     */
    public void setMaxMin(float minValue, float maxValue)
            throws VisADException, RemoteException {
        setLimits(minValue, maxValue, spacing);
    }

    /**
     * Set the limits and spacing of the lines.
     *
     * @param  minValue          starting line (degrees)
     * @param  maxValue          ending line (degrees)
     * @param  spacing           spacing between lines (degrees)
     * @throws VisADException    couldn't create local VisAD object
     * @throws RemoteException   couldn't create remote VisAD object
     */
    public void setLimits(float minValue, float maxValue, float spacing)
            throws VisADException, RemoteException {

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.spacing  = spacing;

        createLines();
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
        return new LatLonLines(this);
    }

    /**
     * Create lines and set data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void createLines() throws VisADException, RemoteException {
        createLines(true);
    }

    /**
     * Create the lines from the supplied parameters
     *
     * @param andSetData   set data if true
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void createLines(boolean andSetData)
            throws VisADException, RemoteException {

        Gridded2DSet lineSet;
        ArrayList    setList = new ArrayList();
        float        value;
        float        other;
        float        lalo[][];
        int          numpoints = (isLat)
                                 ? 361
                                 : 181;
        float        first     = (isLat)
                                 ? -180.f
                                 : -90.f;
        float start = (float) Math.max(spacing * (int) (minValue / spacing),
                                       minValue);

        for (value = start; value <= maxValue; value += spacing) {
            lalo  = new float[2][numpoints];
            other = first;

            for (int j = 0; j < numpoints; j++) {
                lalo[0][j] = (isLat)
                             ? value
                             : other;
                lalo[1][j] = (isLat)
                             ? other
                             : value;
                other      += 1.0f;
            }

            lineSet = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                                       lalo, numpoints);

            setList.add(lineSet);
        }

        Gridded2DSet[] latlons = new Gridded2DSet[setList.size()];

        setList.toArray(latlons);

        latLines = new UnionSet(RealTupleType.LatitudeLongitudeTuple,
                                latlons);

        if (andSetData) {
            setData(latLines);
        }
    }

    /**
     * Sets the "visible" property.  This method fires a PropertyChangeEvent for
     * VISIBLE.
     *
     * @param visible            Whether or not this instance should be visible.
     * @throws VisADException    VisAD failure.
     * @throws RemoteException   Java RMI failure.
     */
    public void setVisible(boolean visible)
            throws RemoteException, VisADException {

        if (visible && !hasData()) {
            createLines(true);
        }
        super.setVisible(visible);
    }


    /**
     * Provide a readable description of this LatLonLines
     * @return  readable description
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();

        sb.append("Name = ");
        sb.append(getName());
        sb.append("\n");
        sb.append("\tColor = ");
        sb.append(getColor().toString());
        sb.append("\n");
        sb.append("\tMin:");
        sb.append(Float.toString(minValue));
        sb.append("\t");
        sb.append("Max:");
        sb.append(Float.toString(maxValue));
        sb.append("\t");
        sb.append("Spacing:");
        sb.append(Float.toString(spacing));
        sb.append("\n");
        sb.append("\tIs Visible = ");
        sb.append((isVisible() == true)
                  ? "Yes"
                  : "No");

        return sb.toString();
    }
}

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



import ucar.unidata.data.grid.GridUtil;

import visad.*;

import visad.util.DataUtility;

import java.rmi.RemoteException;


/**
 * A class to support showing 3D gridded data as an IsoSurface
 *
 * @author IDV Development Team
 * @version $Revision: 1.20 $
 */
public class Grid3DDisplayable extends IsoSurface implements GridDisplayable {

    /** flag for coloring one parameter by another */
    private boolean coloredByAnother = false;

    /**
     * Constructs an instance with the supplied name and alphaflag.
     *
     * @param name a String identifier
     * @param alphaflag boolean flag whether to use transparency
     *
     * @exception VisADException  from construction of super class
     * @exception RemoteException from construction of super class
     */
    public Grid3DDisplayable(String name, boolean alphaflag)
            throws VisADException, RemoteException {
        super(name, null, alphaflag);
    }

    /**
     * Does this object use the displayUnit (or the colorUnit) for its
     * display unit. If we have the case where this isosurface is colored
     * by another field then this returns false.
     *
     * @return true if the display unit is also the color unit.
     */
    protected boolean useDisplayUnitForColor() {
        return !coloredByAnother;
    }



    /**
     * Set the data into the Displayable
     *
     * @param field a VisAD FlatField with a 3D nature
     *
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public void setGrid3D(FieldImpl field)
            throws VisADException, RemoteException {
        loadData(field);
    }

    /**
     * Load data into this Displayable.
     *
     * @param field  field representing the data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void loadData(FieldImpl field)
            throws VisADException, RemoteException {

        FlatField ffld = null;

        // get the first FlatField from the input FieldImpl, a time series
        // FlatFields
        if (GridUtil.isTimeSequence(field)) {
            ffld = (FlatField) field.getSample(0);
        } else {
            ffld = (FlatField) field;
        }

        // get the RealTypes to control contouring and coloring the surface

        // get how many actual fields were combined together, to look for the case
        // where one field colors isosurface of another field. If one
        // it controls both contours and colors; if two use the second
        // to color by.

        RealTupleType rtt =
            (RealTupleType) DataUtility.getFlatRangeType(ffld);

        //System.out.println("  the grid3d rtt is "+rtt);

        // uncomment to determine ad-hoc rather than programatically
        //coloredByAnother = coloredByAnother && (rtt.getDimension() == 2);
        // get the RealType of the range data; use both for
        // isosurface and for RGB
        RealType contourType = (RealType) rtt.getComponent(0);
        setContourRealType(contourType);  // in IsoSurface

        // get the type of the field for rgb color
        RealType rgbType = coloredByAnother
                           ? (RealType) rtt.getComponent(1)
                           : contourType;

        setRGBRealType(rgbType);

        setData(field);
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return this;
    }

    /**
     * Set whether this GridDisplayable should have the data colored
     * by another parameter.
     *
     * @param yesno true if colored by another
     */
    public void setColoredByAnother(boolean yesno) {
        coloredByAnother = yesno;
    }
}

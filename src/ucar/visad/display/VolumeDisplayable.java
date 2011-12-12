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



import ucar.visad.display.*;

import visad.*;

import java.rmi.RemoteException;

import java.util.*;



/**
 * A class to support showing 3D gridded data as a volume
 * in a DisplayMaster.
 *
 * @author IDV Development Team
 * @version $Revision: 1.2 $
 */
public class VolumeDisplayable extends RGBDisplayable implements GridDisplayable {

    /**
     * Constructs an instance with the supplied name.
     *
     * @param name a String identifier
     * @exception VisADException  from construction of super class
     * @exception RemoteException from construction of super class
     */
    public VolumeDisplayable(String name)
            throws VisADException, RemoteException {
        this(name, null, null);
    }

    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @param rgbRealType       The type of the RGB parameter.  May be
     *                          <code>null</code>.
     * @param colorPalette      The initial colorPalette to use. May be
     *                          <code>null</code> (Vis5D palette used
     *                          as default).
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public VolumeDisplayable(String name, RealType rgbRealType,
                             float[][] colorPalette)
            throws VisADException, RemoteException {
        super(name, null, colorPalette, true);
        addConstantMap(new ConstantMap(1.0, Display.TextureEnable));
    }


    /**
     * Constructs from another instance.  The following attributes are set from
     * the other instance: color palette, the color RealType.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected VolumeDisplayable(VolumeDisplayable that)
            throws VisADException, RemoteException {

        super(that);
    }

    /**
     * Set the data into the Displayable; set RGB Type and SelectRange
     *
     * @param field a VisAD FlatField with a 3D nature
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public void loadData(FieldImpl field)
            throws VisADException, RemoteException {

        loadData(field, 0);
    }

    public void loadData(FieldImpl field, int rgbIndex)
            throws VisADException, RemoteException {

        // get the RealType of the range from the FlatField
        TupleType tt       = GridUtil.getParamType(field);
        RealType  ffldType = tt.getRealComponents()[rgbIndex];

        if ((getRGBRealType() == null)
                || !ffldType.equals(getRGBRealType())) {
            setRGBRealType(ffldType);
        }

        setData(field);
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        return new VolumeDisplayable(this);
    }

    /**
     * Set whether this GridDisplayable should have the data colored
     * by another parameter.  This implementation is a no-op.
     *
     * @param yesno true if colored by another
     */
    public void setColoredByAnother(boolean yesno) {}
}

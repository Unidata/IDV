/*
 * $Id: AddeFlatField.java,v 1.7 2007/08/08 17:14:56 jeffmc Exp $
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




package ucar.visad;


import ucar.ma2.Array;
import ucar.ma2.Index;

//import ucar.nc2.dataset.grid.*;
import ucar.nc2.dt.grid.*;

import ucar.unidata.data.DataUtil;
import ucar.unidata.util.IOUtil;



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;


import visad.*;


import visad.meteorology.SingleBandedImage;

import visad.util.DataUtility;


import java.io.*;

import java.rmi.RemoteException;



/**
 * This is a FloatField that caches to disk its float array.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.7 $ $Date: 2007/08/08 17:14:56 $
 */
public class AddeImageFlatField extends CachedFlatField implements SingleBandedImage {




    /** _more_          */
    private String readLabel = "";

    /** _more_ */
    private GriddedSet domainSet;

    /** _more_          */
    private DateTime startTime;

    /**
     * copy ctor
     *
     * @param that What we clone from
     * @param copy copy the values
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeCoordSysArray  rangeCoordSysArray
     * @param rangeSets range sets
     * @param units units
     *
     * @throws VisADException On badness
     */
    public AddeImageFlatField(AddeImageFlatField that, boolean copy,
                              FunctionType type, Set domainSet,
                              CoordinateSystem rangeCoordSys,
                              CoordinateSystem[] rangeCoordSysArray,
                              Set[] rangeSets, Unit[] units)
            throws VisADException {
        super(that, copy, type, domainSet, rangeCoordSys, rangeCoordSysArray,
              rangeSets, units);
        this.domainSet = that.domainSet;
    }

    /**
     * Clone this object
     *
     * @param copy copy the values
     * @param type Function type
     * @param domainSet Domain set
     * @param rangeCoordSys  range CoordSystem
     * @param rangeCoordSysArray  rangeCoordSysArray
     * @param rangeSets range sets
     * @param units units
     *
     * @return New field
     *
     * @throws VisADException On badness
     */
    public CachedFlatField cloneMe(boolean copy, FunctionType type,
                                   Set domainSet,
                                   CoordinateSystem rangeCoordSys,
                                   CoordinateSystem[] rangeCoordSysArray,
                                   Set[] rangeSets, Unit[] units)
            throws VisADException {

        msg("AddeImageFlatField.cloneMe");
        CachedFlatField ccf = new AddeImageFlatField(this, copy, type,
                                  domainSet, rangeCoordSys,
                                  rangeCoordSysArray, rangeSets, units);
        return ccf;
    }





    /**
     * ctor
     *
     * @param floats The values
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeSets range sets
     * @param units units
     *
     * @throws VisADException On badness
     */
    public AddeImageFlatField(FunctionType type, Set domainSet,
                              CoordinateSystem rangeCoordSys,
                              Set[] rangeSets, Unit[] units, float[][] floats)
            throws VisADException {
        super(type, domainSet, rangeCoordSys, rangeSets, units, floats);
    }


    /**
     * _more_
     *
     * @param image _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static AddeImageFlatField createFromSingleBandedImage(
            SingleBandedImage image)
            throws VisADException, RemoteException {
        float[][] values = image.getFloats(false);

        FlatField ff     = (FlatField) image;

        AddeImageFlatField aff =
            new AddeImageFlatField((FunctionType) image.getType(),
                                   image.getDomainSet(),
        //                                        ff.getRangeCoordinateSystem(0)[0],
        null, ff.getRangeSets(), ff.getRangeUnits()[0], values);
        aff.startTime = image.getStartTime();
        //        aff.setShouldCache(true);
        return aff;
    }



    /**
     * Used to provide a hook to derived classes to dynamically read in the data
     *
     * @return data
     */
    protected float[][] readData() {
        return null;
    }


    /**
     * _more_
     *
     * @param s _more_
     */
    public void setReadLabel(String s) {
        readLabel = s;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getStartTime() {
        return startTime;
    }

    /**
     * Return a descriptive string for this image.
     * @return description
     */
    public String getDescription() {
        return "image";
    }

    /**
     * Get the minimum possible value for this image
     * @return  a Real representing the minimum possible value.  Using a
     *          Real allows us to associate units and error estimates with
     *          the value
     */
    public Real getMinRangeValue() {
        System.err.println("getminrangevalue");
        return null;
    }

    /**
     * Get the maximum possible value for this image
     * @return  a Real representing the maximum possible value.  Using a
     *          Real allows us to associate units and error estimates with
     *          the value
     */
    public Real getMaxRangeValue() {
        System.err.println("getmaxrangevalue");
        return null;
    }

    /**
     * Check to see if this image has a domain that can map to Latitude
     * and Longitude.
     *
     * @return true if it has navigation, otherwise false
     */
    public boolean isNavigated() {
        System.err.println("isnavigated");
        return true;
    }



}


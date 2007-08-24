/*
 * $Id: GeoGridFlatField.java,v 1.7 2007/08/08 17:14:56 jeffmc Exp $
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

import visad.util.DataUtility;


import java.io.*;

import java.rmi.RemoteException;



/**
 * This is a FloatField that caches to disk its float array.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.7 $ $Date: 2007/08/08 17:14:56 $
 */
public class GeoGridFlatField extends CachedFlatField {

    private String readLabel="";

    /** _more_ */
    private GriddedSet domainSet;

    /** _more_ */
    transient private GeoGrid geoGrid;

    /** _more_ */
    private int timeIndex;

    /** _more_          */
    transient private Object readLock;

    /**
     * ctor
     *
     *
     * @param geoGrid _more_
     * @param readLock _more_
     * @param timeIndex _more_
     * @param domainSet _more_
     * @param type Function type
     *
     * @throws VisADException On badness
     */
    public GeoGridFlatField(GeoGrid geoGrid, Object readLock, int timeIndex,
                            GriddedSet domainSet,
                            FunctionType type) throws VisADException {
        super(type, domainSet);
        this.readLock  = readLock;
        this.geoGrid   = geoGrid;
        this.domainSet = domainSet;
        this.timeIndex = timeIndex;
    }



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
    public GeoGridFlatField(GeoGridFlatField that, boolean copy,
                            FunctionType type, Set domainSet,
                            CoordinateSystem rangeCoordSys,
                            CoordinateSystem[] rangeCoordSysArray,
                            Set[] rangeSets,
                            Unit[] units) throws VisADException {
        super(that, copy, type, domainSet, rangeCoordSys, rangeCoordSysArray,
              rangeSets, units);
        this.readLock  = that.readLock;
        this.geoGrid   = that.geoGrid;
        this.domainSet = that.domainSet;
        this.timeIndex = that.timeIndex;
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
                                   Set[] rangeSets,
                                   Unit[] units) throws VisADException {

        msg("GeoGridFlatField.cloneMe");
        CachedFlatField ccf = new GeoGridFlatField(this, copy, type, domainSet,
                                    rangeCoordSys, rangeCoordSysArray,
                                    rangeSets, units);
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
    public GeoGridFlatField(FunctionType type, Set domainSet,
                            CoordinateSystem rangeCoordSys, Set[] rangeSets,
                            Unit[] units,
                            float[][] floats) throws VisADException {
        super(type, domainSet, rangeCoordSys, rangeSets, units, floats);
    }


    /** _more_          */
    public static Object ALLMUTEX = new Object();


    private Object getReadLock() {
        if(readLock == null) {
            readLock = ALLMUTEX;
        }
        return readLock;
    }

    /**
     * Used to provide a hook to derived classes to dynamically read in the data
     *
     * @return data
     */
    protected float[][] readData() {
        if(myParent!=null) {
            return myParent.readData();
        }
        //        Misc.printStack("GeoGridFlatField.readData",15,null);
        Array arr;
        try {
            System.err.println (myid +" GeoGridFlatField readData");
            msg("readData");
            Trace.call1("GeoGridFlatField.geogrid.readVolumeData");
            synchronized (getReadLock()) {
                LogUtil.message(readLabel);
                ucar.unidata.data.DataSourceImpl.incrOutstandingGetDataCalls();
                arr = geoGrid.readVolumeData(timeIndex);
                LogUtil.message("");
            }
            Trace.call2("GeoGridFlatField.geogrid.readVolumeData");
            // 3D grid with one level - slice to 2D grid
            if ((arr.getRank() > 2) && (domainSet.getDimension() == 2)) {
                int[] lengths    = domainSet.getLengths();
                int   sizeX      = lengths[0];
                int   sizeY      = lengths[1];
                int   levelIndex = 0;  // get the first by default
                int[] shape      = arr.getShape();
                for (int i = 0; i <= arr.getRank(); i++) {
                    // find the index whose dimension is not x or y
                    if ((shape[i] != sizeX) && (shape[i] != sizeY)) {
                        // extract the correct "z" level data:
                        arr = arr.slice(i, levelIndex);
                        break;
                    }
                }
            }
        } catch (RemoteException e) {
            LogUtil.logException("getFlatField read got RemoteException", e);
            return null;
        } catch (IOException e) {
            LogUtil.logException("getFlatField read got IOException", e);
            return null;
        } finally {
            ucar.unidata.data.DataSourceImpl.decrOutstandingGetDataCalls();
        }


        /* Simple Java arrays are used to make FlatFields:
         *   to hold coordinates:
         *   there are x,y,z values for every point in the 3 D array;
         *   they are ALL stored here (allows for non regular grid, possibly)
         */

        Trace.call1("toFloatArray", " array:" + arr.getClass().getName());
        float[][] fieldArray = new float[1][];
        fieldArray[0] = DataUtil.toFloatArray(arr);
        Trace.call2("toFloatArray", " length:" + fieldArray[0].length);
        msg("readData DONE");
        return fieldArray;
    }


    public void setReadLabel(String s) {
        readLabel = s;
    }

}


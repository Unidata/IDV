/*
 * $Id: CachedFlatField.java,v 1.9 2007/08/08 17:14:56 jeffmc Exp $
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


package ucar.visad.data;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;


import visad.*;

import visad.util.DataUtility;


import java.io.*;

import java.rmi.RemoteException;


/**
 * This is a FloatField that caches to disk its float array.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.9 $ $Date: 2007/08/08 17:14:56 $
 */
public class CachedFlatField extends FlatField {




    /** the id for this instance */
    private Object cacheId;

    private boolean inCache = false;

    /** Mutex */
    transient protected Object MUTEX = new Object();


    /** myParent */
    protected CachedFlatField myParent;

    /** The min/max ranges */
    Range[] ranges;

    /** The min/max ranges */
    Range[] sampleRanges;



    /**
     * Create a new CachedFlatField
     *
     * @param type Function type
     * @param domainSet set for this
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, Set domainSet)
            throws VisADException {
        this(type, domainSet, (CoordinateSystem) null, (Set[]) null,
             (Unit[]) null, null);
    }


    /**
     * Create a new CachedFlatField
     *
     * @param floats The values
     * @param type Function type
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, float[][] floats)
            throws VisADException {
        this(type, type.getDomain().getDefaultSet(), (CoordinateSystem) null,
             (Set[]) null, (Unit[]) null, floats);
    }

    /**
     * Create a new CachedFlatField
     *
     * @param floats The values
     * @param type Function type
     * @param domainSet Domain
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, Set domainSet, float[][] floats)
            throws VisADException {
        this(type, domainSet, (CoordinateSystem) null, (Set[]) null,
             (Unit[]) null, floats);
    }

    /**
     * Create a new CachedFlatField
     *
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeSets range sets
     * @param units units
     * @param floats The values
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, Set domainSet,
                           CoordinateSystem rangeCoordSys, Set[] rangeSets,
                           Unit[] units, float[][] floats)
            throws VisADException {
        this(type, domainSet, rangeCoordSys, null, rangeSets, units, floats);
    }

    /**
     * Create a new CachedFlatField
     *
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeCoordSyses  range CoordSystem's
     * @param rangeSets range sets
     * @param units units
     * @param floats The values
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, Set domainSet,
                           CoordinateSystem rangeCoordSys,
                           CoordinateSystem[] rangeCoordSyses,
                           Set[] rangeSets, Unit[] units, float[][] floats)
            throws VisADException {
        super(type, domainSet, rangeCoordSys, null, rangeSets, units);
        init(floats);
    }

    /**
     * Copy constructor
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
    public CachedFlatField(CachedFlatField that, boolean copy,
                           FunctionType type, Set domainSet,
                           CoordinateSystem rangeCoordSys,
                           CoordinateSystem[] rangeCoordSysArray,
                           Set[] rangeSets, Unit[] units)
            throws VisADException {
        super(type, domainSet, rangeCoordSys, rangeCoordSysArray, rangeSets,
              units);

        //TODO: handle the caching

        this.cacheId = "clone:" + that.cacheId;
        float[][] myFloatValues = null;
        if (copy) {
            myFloatValues = that.unpackFloats(copy);
            init(myFloatValues);
        } else {
            this.myParent     = that;
            this.ranges       = that.ranges;
            this.sampleRanges = that.sampleRanges;
            clearMissing();
        }
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

        CachedFlatField ccf = new CachedFlatField(this, copy, type,
                                  domainSet, rangeCoordSys,
                                  rangeCoordSysArray, rangeSets, units);
        return ccf;
    }



    public void finalize() throws Throwable {
        super.finalize();
        System.err.println("flatfield finalized");
        if(cacheId!=null) {
            DataCacheManager.getCacheManager().removeFromCache(cacheId);
        }
    }


    /**
     * Set the sample
     *
     * @param values the samples
     * @param errors errors
     * @param copy   tru to copy
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException  Problem in VisAD land
     */
    public void setSamples(float[][] values, ErrorEstimate[] errors,
                           boolean copy)
            throws VisADException, RemoteException {

        float[][]myFloatValues = new float[values.length][];
        for (int i = 0; i < myFloatValues.length; i++) {
            if (copy) {
                myFloatValues[i] = (float[]) values[i].clone();
            } else {
                myFloatValues[i] = values[i];
            }
        }
        setRangeErrors(errors);
        if(inCache) {
            DataCacheManager.getCacheManager().updateData(cacheId, myFloatValues);
        } else {
            init(myFloatValues);
        }

    }



    /**
     * Set the sample ranges
     *
     * @param sampleRanges the sample ranges
     */
    public void setSampleRanges(Range[] sampleRanges) {
        this.sampleRanges = sampleRanges;
    }

    /**
     * Override method so we clear the caches on the cloned object
     *
     * @return the clone
     */
    public Object clone() {
        CachedFlatField ccf = (CachedFlatField) super.clone();
        ccf.clearCachedRange();
        return ccf;
    }


    /**
     * Clear the cached ranges
     */
    public void clearCachedRange() {
        sampleRanges = null;
        ranges       = null;
    }




    /**
     * Get the ranges
     *
     * @return ranges
     *
     * @throws VisADException  problem getting ranges
     */
    public Range[] getRanges() throws VisADException {
        return getRanges(false);
    }

    /**
     * Get the ranges
     *
     *
     * @param force   force a recalc
     * @return ranges  the ranges
     *
     * @throws VisADException  problem getting ranges
     */
    public Range[] getRanges(boolean force) throws VisADException {
        if (force) {
            sampleRanges = null;
        }
        if (ranges != null) {
            return ranges;
        }
        if (sampleRanges != null) {
            return sampleRanges;
        }
        //        msg("making ranges");
        return getRanges(unpackFloats(false));
    }


    /**
     * Get the ranges for the values
     *
     * @param values the values
     *
     * @return the ranges
     *
     * @throws VisADException  Problem in VisAD land
     */
    public Range[] getRanges(float[][] values) throws VisADException {
        sampleRanges = null;
        if (values == null) {
            return null;
        }
        ranges = new Range[values.length];
        for (int rangeIdx = 0; rangeIdx < values.length; rangeIdx++) {
            float   pMin         = Float.POSITIVE_INFINITY;
            float   pMax         = Float.NEGATIVE_INFINITY;
            float[] values_range = values[rangeIdx];
            int     length       = values_range.length;
            for (int i = 0; i < length; i++) {
                float value = values_range[i];
                if (pMax < value) {
                    pMax = value;
                }
                if (pMin > value) {
                    pMin = value;
                }
            }
            ranges[rangeIdx] = new Range(pMin, pMax);
        }
        //        msg("done making ranges");
        return ranges;
    }






    /**
     * init
     *
     * @param data data
     *
     * @throws VisADException initializing field
     */
    protected void init(float[][] data) throws VisADException {
        if(data!=null) {
            cacheId = DataCacheManager.getCacheManager().addToCache(data);
            inCache = true;
        }
        //Read the ranges when we first have data
        if (ranges == null) {
            getRanges(data);
        }

        clearMissing();
    }


    /**
     * Used to provide a hook to derived classes to dynamically read in the data
     *
     * @return data
     */
    protected float[][] readData() {
        msg(" CachedFlatField.readData");
        return null;
    }

    /**
     * Debug statment
     *
     * @param s message to print
     */
    protected void msg(String s) {
        //        System.err.println(cacheId + " " + s);
    }

    /**
     * Read data from cache
     *
     * @return the values from the cache
     *
     * @throws VisADException   problem reading data
     */
    private float[][] getMyValues() throws VisADException {
        if (myParent != null) {
            return myParent.getMyValues();
        }
        if(inCache) {
            return DataCacheManager.getCacheManager().getFloatArray2D(cacheId);
        }

        float[][] values = readData();
        if (values == null) {
            msg("Floats still null after readData");
            return null;
        }
        init(values);
        return values;
    }


    /**
     * Get the range value at the index-th sample.
     *
     * @param index  index of the sample
     * @return Data object (Real, RealTuple, or Tuple) corresponding to
     *         the range at the index-th sample.
     * @throws VisADException  problem getting data
     * @throws RemoteException problem getting data from remote object
     */
    public Data getSample(int index) throws VisADException, RemoteException {
        //        msg("getSample");
        float[][] values = getMyValues();

        if (values == null) {
            //            msg("Floats still null");
            return null;
        }
        MathType        Type        = getType();
        ErrorEstimate[] RangeErrors = getRangeErrors();

        if (isMissing() || (index < 0) || (index >= getLength())) {
            //            msg("is missing");
            return ((FunctionType) Type).getRange().missingData();
        }
        double[][] range = new double[TupleDimension][1];
        for (int i = 0; i < TupleDimension; i++) {
            range[i][0] = (double) values[i][index];
        }

        MathType RangeType = ((FunctionType) Type).getRange();
        if (RangeType instanceof RealType) {
            return new Real((RealType) RangeType, range[0][0], RangeUnits[0],
                            RangeErrors[0]);
        } else if (RangeType instanceof RealTupleType) {
            Real[] reals = new Real[TupleDimension];
            for (int j = 0; j < TupleDimension; j++) {
                MathType type = ((RealTupleType) RangeType).getComponent(j);
                reals[j] = new Real((RealType) type, range[j][0],
                                    RangeUnits[j], RangeErrors[j]);
            }
            return new RealTuple((RealTupleType) RangeType, reals,
                                 RangeCoordinateSystem);
        } else {  // RangeType is a Flat TupleType
            int    n      = ((TupleType) RangeType).getDimension();
            int    j      = 0;
            Data[] datums = new Data[n];
            for (int i = 0; i < n; i++) {
                MathType type = ((TupleType) RangeType).getComponent(i);
                if (type instanceof RealType) {
                    datums[i] = new Real((RealType) type, range[j][0],
                                         RangeUnits[j], RangeErrors[j]);
                    j++;
                } else {  // type instanceof RealTupleType
                    int    m     = ((RealTupleType) type).getDimension();
                    Real[] reals = new Real[m];
                    for (int k = 0; k < m; k++) {
                        RealType ctype =
                            (RealType) ((RealTupleType) type).getComponent(k);
                        reals[k] = new Real(ctype, range[j][0],
                                            RangeUnits[j], RangeErrors[j]);
                        j++;
                    }
                    datums[i] = new RealTuple((RealTupleType) type, reals,
                            RangeCoordinateSystems[i]);
                }
            }
            return new Tuple(datums, false);
        }
    }


    /**
     * get the float values as doubles
     *
     * @param copy copy the values
     *
     * @return The values
     *
     * @throws VisADException On badness
     */
    protected double[][] unpackValues(boolean copy) throws VisADException {
        float[][] values = unpackFloats(false);
        if (values == null) {
            return null;
        }
        double[][] doubles = new double[values.length][];
        for (int i = 0; i < values.length; i++) {
            float[]  values_i  = values[i];
            double[] doubles_i = new double[values_i.length];
            doubles[i] = doubles_i;
            for (int j = 0; j < values_i.length; j++) {
                doubles_i[j] = values_i[j];
            }
        }
        return doubles;
    }


    /**
     * get the float values
     *
     * @param copy copy the values
     *
     * @return The values
     *
     * @throws VisADException On badness
     */
    public float[][] unpackFloats(boolean copy) throws VisADException {
        //        msg("unpackFloats copy=" + copy);
        float[][] values = getMyValues();
        if (values == null) {
            msg("unpack floats - values still null");
            return null;
        }
        float[][] result = null;
        result = new float[values.length][];
        for (int i = 0; i < result.length; i++) {
            if (copy) {
                result[i] = (float[]) values[i].clone();
            } else {
                result[i] = values[i];
            }
        }
        return result;
    }


    /**
     * Unpack floats
     *
     * @param s_index the sample index
     *
     * @return  the floats for that index
     *
     * @throws VisADException  Problem in VisAD land
     */
    protected float[] unpackFloats(int s_index) throws VisADException {
        float[][] values = getMyValues();
        if (values == null) {
            return null;
        }
        float[] range = new float[values.length];
        for (int i = 0; i < TupleDimension; i++) {
            range[i] = values[i][s_index];
        }
        return range;
    }





    /**
     * Make a clone of this using the new type, units and errors.  Called
     * from unary and binar
     *
     * @param f_type  the new FunctionType
     * @param units   the new Units
     * @param errors  the new Errors
     * @param newValues  the new values
     *
     * @return a new FlatField
     *
     * @throws VisADException  Problem in VisAD land
    protected FlatField cloneFloat(MathType f_type, Unit[] units,
                                   ErrorEstimate[] errors,
                                   float[][] newValues)
            throws VisADException {
        MathType N_type = ((f_type == null)
                           ? getType()
                           : f_type);

        // create (initially missing) FlatField for return
        // use FloatSet rather than RangeSet for intermediate computation results
        Set[] sets = new Set[TupleDimension];
        for (int i = 0; i < TupleDimension; i++) {
            SetType set_type =
                new SetType(
                    ((FunctionType) N_type).getFlatRange().getComponent(i));
            sets[i] = new FloatSet(set_type);
        }
        RealTupleType d_type  = ((FunctionType) N_type).getDomain();
        Set           new_set = null;
        if ( !d_type.equals(((FunctionType) getType()).getDomain())) {
            new_set = (Set) getDomainSet().cloneButType(d_type);
        } else {
            new_set = getDomainSet();
        }
        if (newValues == null) {
            //If we don't have the values array then copy this one.
            newValues = unpackFloats(true);
        }
        CachedFlatField field = new CachedFlatField((FunctionType) N_type,
                                    new_set, RangeCoordinateSystem,
                                    RangeCoordinateSystems, sets, units,
                                    newValues);
        return field;
    }
     */

}


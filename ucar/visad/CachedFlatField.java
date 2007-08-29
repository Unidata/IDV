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




package ucar.visad;


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


    /** _more_          */
    static int id = 0;

    /** _more_          */
    String myid = "" + (id++);

    /** Mutex */
    transient protected Object MUTEX = new Object();

    /** For uniqueness */
    private static int cnt = 0;

    /** _more_          */
    protected CachedFlatField myParent;

    /** Where we write */
    private static File cacheDir;

    /** Size of array when we actually start caching to disk */
    private static int cacheThreshold = 100000;


    /** Are we over the size threshold */
    private boolean shouldCache;

    /** Where we cache to */
    String filename;

    /** The values */
    private float[][] myFloatValues;

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
        super(type, domainSet, rangeCoordSys, rangeSets, units);
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


        this.myid = "clone:" + that.myid;
        if (copy) {
            this.myFloatValues = that.unpackFloats(copy);
            init(myFloatValues);
        } else {
            this.myParent      = that;
            this.myFloatValues = that.myFloatValues;
            this.filename      = that.getFilename();
            this.ranges        = that.ranges;
            this.sampleRanges  = that.sampleRanges;
            this.shouldCache   = that.shouldCache;
            clearMissing();
            //            checkCache();
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean haveDataOnDisk() {
        if (getFilename() == null) {
            return false;
        }
        return new File(getFilename()).exists();
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

        msg("CachedFlatField.cloneMe");


        CachedFlatField ccf = new CachedFlatField(this, copy, type,
                                  domainSet, rangeCoordSys,
                                  rangeCoordSysArray, rangeSets, units);
        return ccf;
    }




    /**
     * _more_
     *
     * @param sampleRanges _more_
     */
    public void setSampleRanges(Range[] sampleRanges) {
        this.sampleRanges = sampleRanges;
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
     * @param force _more_
     * @return ranges
     *
     * @throws VisADException  problem getting ranges
     */
    public Range[] getRanges(boolean force) throws VisADException {
        //        ucar.unidata.util.Misc.printStack("CachedFlatField.getRanges",5,null);
        if (force) {
            sampleRanges = null;
        }
        if (ranges != null) {
            return ranges;
        }
        if (sampleRanges != null) {
            return sampleRanges;
        }
        msg("making ranges");
        return getRanges(unpackFloats(false));
    }


    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
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
        msg("done making ranges");
        return ranges;
    }




    /**
     * Set where we write to
     *
     * @param f Cache dir
     */
    public static void setCacheDir(File f) {
        cacheDir = f;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static File getCacheDir() {
        return cacheDir;
    }


    /**
     * Set the size where we start caching
     *
     * @param size size
     */
    public static void setCacheThreshold(int size) {
        cacheThreshold = size;
    }



    /**
     * _more_
     *
     * @param b _more_
     */
    public void setShouldCache(boolean b) {
        shouldCache = b;

    }

    /**
     * init
     *
     * @param data data
     *
     * @throws VisADException initializing field
     */
    protected void init(float[][] data) throws VisADException {
        myFloatValues = data;

        //Read the ranges when we first have data
        if (ranges == null) {
            getRanges(myFloatValues);
        }

        clearMissing();
        if (data != null) {
            if ( !shouldCache) {
                shouldCache = data[0].length > cacheThreshold;
            }
            checkCache();
        }
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
        //        System.err.println(myid+ " " +s);
    }

    /**
     * Read data from cache
     *
     * @return the values from the cache
     *
     * @throws VisADException   problem reading data
     */
    private float[][] readCache() throws VisADException {
        synchronized (MUTEX) {
            float[][] values = myFloatValues;
            System.err.println(myid + " in readCache");
            if (values != null) {
                return values;
            }

            if (myParent != null) {
                return myParent.readCache();
            }

            if ( !haveDataOnDisk()) {
                msg(getClass().getName()
                    + " Have not written calling readData");
                values = readData();
                if (values == null) {
                    msg("Floats still null after readData");
                    return null;
                }
                init(values);
                return values;
            }


            try {
                //            System.err.println ("*** Reading from file");
                msg("reading from file cache");
                FileInputStream istream = new FileInputStream(getFilename());
                BufferedInputStream bis = new BufferedInputStream(istream,
                                              1000000);
                ObjectInputStream ois = new ObjectInputStream(bis);
                myFloatValues = values = (float[][]) ois.readObject();
                ois.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            return values;
        }
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
        msg("getSample");
        float[][] values = myFloatValues;
        if (values == null) {
            msg(" Reading cache");
            values = readCache();
            msg(" done Reading cache");
        }
        //        Misc.printStack("CachedFlatField.unpackFloats", 3, null);
        if (values == null) {
            msg("Floats still null");
            return null;
        }
        MathType        Type        = getType();
        ErrorEstimate[] RangeErrors = getRangeErrors();

        if (isMissing() || (index < 0) || (index >= getLength())) {
            msg("is missing");
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
        msg("unpackFloats copy=" + copy);
        float[][] values = myFloatValues;
        if (values == null) {
            msg(" values is null");
            msg(" Reading cache");
            values = readCache();
            msg(" done Reading cache");
        }
        if (values == null) {
            msg("Floats still null");
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
        checkCache();
        msg(" Done unpackFloats");
        return result;
    }


    /**
     * Clear the floats array and write to the cache if  needed
     */
    private void checkCache() {
        if ( !shouldCache) {
            return;
        }
        if ( !haveDataOnDisk()) {
            writeCache();
        }
        myFloatValues = null;
    }


    /**
     * _more_
     *
     * @param f _more_
     */
    public void setFilename(String f) {
        filename = f;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private String getFilename() {
        if ((filename == null) && (cacheDir != null)) {
            String uniqueName = "field_" + System.currentTimeMillis() + "_"
                                + (cnt++);
            filename = IOUtil.joinDir(cacheDir, uniqueName);
        }
        return filename;
    }


    /**
     * Write the values into the cache
     */
    private void writeCache() {
        synchronized (MUTEX) {
            if (getFilename() == null) {
                return;
            }
            float[][] values = myFloatValues;
            if (values == null) {
                return;
            }
            try {
                long t1 = System.currentTimeMillis();
                System.err.println(myid + " writing to file cache "
                                   + getFilename());
                FileOutputStream fos = new FileOutputStream(getFilename());
                BufferedOutputStream bos = new BufferedOutputStream(fos,
                                               100000);
                ObjectOutputStream p = new ObjectOutputStream(bos);
                p.writeObject(values);
                p.flush();
                fos.close();
                long t2 = System.currentTimeMillis();
                //                System.err.println ("time:" + (t2-t1));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }




}


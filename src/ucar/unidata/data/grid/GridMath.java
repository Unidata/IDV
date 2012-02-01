/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.grid;


import ucar.unidata.data.DataUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import ucar.visad.Util;

import visad.*;

import visad.util.DataUtility;


import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;


/**
 * A class to handle grid math.  This handles math between grids on
 * different manifolds where possible.
 *
 * @author IDV Development Team
 */
public class GridMath {

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_AVERAGE = "average";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_STDEV = "standardDeviation";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_PRCNTL = "ensemblePercentile";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_UPROB = "ensembleUProbability";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_SUM = "sum";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_MAX = "max";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_MIN = "min";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_RNG = "range";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_MODE = "mode";

    /** function for the timeStepFunc routine */
    public static final String FUNC_DIFFERENCE = "difference";

    /** cyclic option */
    public static final int OPT_CYCLIC = -1;

    /** missing option */
    public static final int OPT_MISSING = 0;

    /** symmetric option */
    public static final int OPT_SYMMETRIC = 1;

    /** axis identifier - X */
    public static final String AXIS_X = "X";

    /** axis identifier - Y */
    public static final String AXIS_Y = "Y";

    /** kilometers/degree (111) */
    private static final Real KM_PER_DEGREE;

    /** negative one */
    public static final Real NEGATIVE_ONE;

    static {
        try {
            Unit kmPerDegree = DataUtil.parseUnit("km/degree");
            KM_PER_DEGREE = new Real(DataUtil.makeRealType("kmPerDegree",
                    kmPerDegree), 111.0, kmPerDegree);
            NEGATIVE_ONE = new Real(-1);

        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex.toString());
        }
    }


    /**
     * Add two grids together
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the sum of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl add(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return add(grid1, grid2, false);
    }

    /**
     * Add two grids together
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param useWA  use WEIGHTED_AVERAGE for resampling
     *
     * @return  the sum of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl add(FieldImpl grid1, FieldImpl grid2,
                                boolean useWA)
            throws VisADException {
        return doMath(grid1, grid2, Data.ADD, useWA);
    }

    /**
     * Subtract two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the difference of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl subtract(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return subtract(grid1, grid2, false);
    }

    /**
     * Subtract two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param useWA _more_
     *
     * @return  the difference of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl subtract(FieldImpl grid1, FieldImpl grid2,
                                     boolean useWA)
            throws VisADException {
        return doMath(grid1, grid2, Data.SUBTRACT, useWA);
    }

    /**
     * Multiply two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the product of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl multiply(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return multiply(grid1, grid2, false);
    }

    /**
     * Multiply two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param useWA _more_
     *
     * @return  the product of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl multiply(FieldImpl grid1, FieldImpl grid2,
                                     boolean useWA)
            throws VisADException {
        return doMath(grid1, grid2, Data.MULTIPLY, useWA);
    }

    /**
     * Divide two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the quotient of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl divide(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return divide(grid1, grid2, false);
    }

    /**
     * Divide two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param useWA _more_
     *
     * @return  the quotient of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl divide(FieldImpl grid1, FieldImpl grid2,
                                   boolean useWA)
            throws VisADException {
        return doMath(grid1, grid2, Data.DIVIDE, useWA);
    }

    /**
     * Take the arctangent of two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     *
     * @return  the arctangent of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl atan2(FieldImpl grid1, FieldImpl grid2)
            throws VisADException {
        return atan2(grid1, grid2, false);
    }

    /**
     * Take the arctangent of two grids
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param useWA _more_
     *
     * @return  the arctangent of the grids
     *
     * @throws VisADException  problem doing the math
     */
    public static FieldImpl atan2(FieldImpl grid1, FieldImpl grid2,
                                  boolean useWA)
            throws VisADException {
        return doMath(grid1, grid2, Data.ATAN2, useWA);
    }

    /**
     * Do the math.  This method handles making the data as compatible
     * as possible before actually going off and doing the math.
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param op  the VisAD math operand  (see visad.Data)
     *
     * @return the grid
     *
     * @throws VisADException  problem doing the math
     */
    private static FieldImpl doMath(FieldImpl grid1, FieldImpl grid2, int op)
            throws VisADException {
        return doMath(grid1, grid2, op, false);
    }

    /**
     * Do the math.  This method handles making the data as compatible
     * as possible before actually going off and doing the math.
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param op  the VisAD math operand  (see visad.Data)
     * @param useWA  use WEIGHTED_AVERAGE
     *
     * @return the grid
     *
     * @throws VisADException  problem doing the math
     */
    private static FieldImpl doMath(FieldImpl grid1, FieldImpl grid2, int op,
                                    boolean useWA)
            throws VisADException {
        FieldImpl a         = grid1;
        FieldImpl b         = grid2;
        boolean   is3D1     = GridUtil.is3D(grid1);
        boolean   is3D2     = GridUtil.is3D(grid2);
        boolean   isVolume1 = GridUtil.isVolume(grid1);
        boolean   isVolume2 = GridUtil.isVolume(grid2);
        boolean   isSlice1  = !isVolume1 && is3D1;
        boolean   isSlice2  = !isVolume2 && is3D2;
        //boolean equalDomains = Misc.equals(GridUtil.getSpatialDomain(grid1),
        //                                   GridUtil.getSpatialDomain(grid2));

        if (isSlice1 && isSlice2) {
            //if ( !equalDomains) {
            if ( !Misc.equals(GridUtil.getSpatialDomain(grid1),
                              GridUtil.getSpatialDomain(grid2))) {
                a = GridUtil.make2DGridFromSlice(grid1, false);
                b = GridUtil.make2DGridFromSlice(grid2, false);
            }
        } else if (isSlice1 && !is3D2) {
            a = GridUtil.make2DGridFromSlice(grid1, false);
        } else if ( !is3D1 && isSlice2) {
            b = GridUtil.make2DGridFromSlice(grid2, false);
        }
        // VisAD Default is NEAREST_NEIGHBOR
        //int     mode      = (equalDomains)
        //                    ? Data.NEAREST_NEIGHBOR
        //                    : Data.WEIGHTED_AVERAGE;
        int     mode      = useWA
                            ? Data.WEIGHTED_AVERAGE
                            : Data.NEAREST_NEIGHBOR;

        boolean isLatLon1 = GridUtil.isLatLonOrder(a);
        boolean isLatLon2 = GridUtil.isLatLonOrder(b);
        if ( !(isLatLon1 == isLatLon2)) {
            if (GridUtil.canSwapLatLon(a)) {
                a = GridUtil.swapLatLon(a);
            } else if (GridUtil.canSwapLatLon(b)) {
                b = GridUtil.swapLatLon(b);
            } else {
                throw new VisADException("incompatible grid domains");
            }
        }
        return (FieldImpl) binary(a, b, op, mode, Data.NO_ERRORS);
    }

    /**
     * Wrapper for visad.Data.binary
     *
     * @param grid1  first grid
     * @param grid2  second grid
     * @param op  the VisAD math operand  (see visad.Data)
     * @param samplingMode  sampling mode
     * @param errorMode  error mode
     *
     * @return  the result  or null
     *
     * @throws VisADException  problem doing the math
     */
    private static FieldImpl binary(FieldImpl grid1, FieldImpl grid2, int op,
                                    int samplingMode, int errorMode)
            throws VisADException {
        try {
            return (FieldImpl) grid1.binary(grid2, op, samplingMode,
                                            errorMode);
        } catch (RemoteException re) {}
        return null;
    }

    /**
     * Average the grid at each point over time
     *
     * @param grid   grid to average
     * @param makeTimes If true then make a time field with the range being the same computed value
     * If false then just return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl averageOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return applyFunctionOverTime(grid, FUNC_AVERAGE, makeTimes);
    }

    /**
     * Compute the standard deviation of the grid at each point over time
     *
     * @param grid   grid to compute std
     * @param makeTimes If true then make a time field with the range being the same computed value
     * If false then just return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl standardDeviationOverTime(FieldImpl grid,
            boolean makeTimes)
            throws VisADException {
        return applyFunctionOverTime(grid, FUNC_STDEV, makeTimes);
    }

    /**
     * Average the grid over member
     *
     * @param grid   ensemble grid to average
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl averageOverMembers(FieldImpl grid)
            throws VisADException {
        return applyFunctionOverMembers(grid, FUNC_AVERAGE);
    }

    /**
     * ensemble Standard Deviation
     *
     * @param grid   ensemble grid
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl ensembleStandardDeviation(FieldImpl grid)
            throws VisADException {
        return applyFunctionOverMembers(grid, FUNC_STDEV);
    }

    /**
     * ensemble grid min values
     *
     * @param grid   ensemble grid
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl ensembleLowestValues(FieldImpl grid)
            throws VisADException {
        return applyFunctionOverMembers(grid, FUNC_MIN);
    }

    /**
     * ensemble grid max values
     *
     * @param grid   ensemble grid
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl ensembleHighestValues(FieldImpl grid)
            throws VisADException {
        return applyFunctionOverMembers(grid, FUNC_MAX);
    }

    /**
     * ensemble grid range values
     *
     * @param grid   ensemble grid
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl ensembleRangeValues(FieldImpl grid)
            throws VisADException {
        return applyFunctionOverMembers(grid, FUNC_RNG);
    }

    /**
     * ensemble grid min values
     *
     * @param grid   ensemble grid
     * @param percent _more_
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl ensemblePercentileValues(FieldImpl grid,
            String percent)
            throws VisADException {
        return applyFunctionOverMembers(grid, percent, "0", "0", FUNC_PRCNTL);
    }

    /**
     *  ensemble grid min values
     *
     *  @param grid   ensemble grid
     *  @param percent _more_
     *
     *  @return the new field
     *
     *  @throws VisADException  On badness
     */
    public static FieldImpl ensemblePercentileValues(FieldImpl grid,
            int percent)
            throws VisADException {
        return applyFunctionOverMembers(grid, percent, 0, 0, FUNC_PRCNTL);
    }

    /**
     *  ensemble grid univariate probability
     *
     *  @param grid   ensemble grid
     *  @param logicalOp gt or lt for P(X > | < pValue)
     *  @param pValue probability threshold value P(valueAtGridPoint < pValue)
     * @param exptdLoBound _more_
     * @param exptdUpBound _more_
     *
     *  @return the new field
     *
     *  @throws VisADException  On badness
     */
    public static FieldImpl ensembleUProbabilityValues(FieldImpl grid,
            String logicalOp, float pValue, float exptdLoBound,
            float exptdUpBound)
            throws VisADException {

        grid = applyFunctionOverMembers(grid, pValue, exptdLoBound,
                                        exptdUpBound, FUNC_UPROB);
        String probName =
            String.format("Ensemble Univariate Probability P(x %s %f)",
                          logicalOp, pValue);;
        RealType probType = visad.RealType.getRealType(probName,
                                visad.CommonUnit.promiscuous);

        return GridUtil.setParamType(grid, probType, true);

    }

    /**
     *  ensemble grid univariate probability
     *
     *  @param grid   ensemble grid
     *  @param logicalOp gt or lt for P(X > | < pValue)
     *  @param pValue probability threshold value P(valueAtGridPoint < pValue)
     * @param exptdLoBound _more_
     * @param exptdUpBound _more_
     *
     *  @return the new field
     *
     *  @throws VisADException  On badness
     */
    public static FieldImpl ensembleUProbabilityValues(FieldImpl grid,
            String logicalOp, String pValue, String exptdLoBound,
            String exptdUpBound)
            throws VisADException {

        grid = applyFunctionOverMembers(grid, pValue, exptdLoBound,
                                        exptdUpBound, FUNC_UPROB);
        String probName =
            String.format("Ensemble Univariate Probability P(x %s %s)",
                          logicalOp, pValue);

        RealTupleType rtt = new RealTupleType(DataUtil.makeRealType(probName,
                                CommonUnit.dimensionless));

        grid = GridUtil.setParamType(grid, rtt, false /* don't copy */);


        return grid;
    }

    /**
     *  ensemble grid min values
     *
     *  @param grid   ensemble grid
     *
     *  @return the new field
     *
     *  @throws VisADException  On badness
     */
    public static FieldImpl ensembleModeValues(FieldImpl grid)
            throws VisADException {
        return applyFunctionOverMembers(grid, 0, 0, 0, FUNC_MODE);
    }

    /**
     * This creates a field where D(T) = D(T)-D(T+offset)
     * Any time steps up to the offset time are set to missing
     * @param grid   grid to average
     * @param offset time step offset. e.g., offset=-1 results in D(T)=D(T)-D(T-1)
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl timeStepDifference(FieldImpl grid, int offset)
            throws VisADException {
        return timeStepFunc(grid, offset, FUNC_DIFFERENCE);
    }



    /**
     * This creates a field where D(T) = D(T)+D(T+offset)
     * Any time steps up to the offset time are set to missing
     * @param grid   grid to average
     * @param offset time step offset. e.g., offset=-1 results in D(T)=D(T)+D(T-1)
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl timeStepSum(FieldImpl grid, int offset)
            throws VisADException {
        return timeStepFunc(grid, offset, FUNC_SUM);
    }


    /**
     * This creates a field where D(T) = D(T)-D(0)
     * Any time steps up to the offset time are set to missing
     * @param grid   grid to average
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl differenceFromBaseTime(FieldImpl grid)
            throws VisADException {
        return timeStepFunc(grid, 0, FUNC_DIFFERENCE);
    }



    /**
     * This creates a field where D(T) = D(0)+D(1)+...+D(T-1)+D(T)
     * @param grid   grid to sum
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl sumFromBaseTime(FieldImpl grid)
            throws VisADException {
        try {
            if ( !GridUtil.isTimeSequence(grid)) {
                return grid;
            }
            FieldImpl newGrid      = (FieldImpl) grid.clone();
            Set       timeDomain   = Util.getDomainSet(newGrid);
            int       numTimeSteps = timeDomain.getLength();
            FlatField sample       = (FlatField) newGrid.getSample(0);
            float[][] baseValue    = Misc.cloneArray(sample.getFloats(false));
            for (int timeStepIdx = 1; timeStepIdx < numTimeSteps;
                    timeStepIdx++) {
                sample = (FlatField) newGrid.getSample(timeStepIdx);
                float[][] timeStepValues = sample.getFloats(false);
                // this creates a new array
                float[][] value = Misc.addArray(baseValue, timeStepValues,
                                      null);
                baseValue = value;
                sample.setSamples(value, false);
            }
            return newGrid;
        } catch (CloneNotSupportedException cnse) {
            throw new VisADException("Cannot clone field");
        } catch (RemoteException re) {
            throw new VisADException("RemoteException in timeStepFunc");
        }
    }


    /**
     * This creates a field where is either D(T) = D(T)-D(T+offset)
     * or D(T) = D(T)+D(T+offset) depending on the value of the func argument
     * Any time steps up to the offset time are set to missing. If offset == 0
     * then we use D(0) as the fixed operand foreach operator, e.g.:
     * D(T) = D(T) - D(0)
     * @param grid   grid to average
     * @param offset time step offset.
     * @param func which function to apply, SUM or DIFFERENCE
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl timeStepFunc(FieldImpl grid, int offset,
                                         String func)
            throws VisADException {
        try {
            if ( !GridUtil.isTimeSequence(grid)) {
                return grid;
            }
            List<float[][]> arrays       = new ArrayList<float[][]>();
            FieldImpl       newGrid      = (FieldImpl) grid.clone();
            float[][]       values       = null;
            float[][]       priorValues  = null;
            Set             timeDomain   = Util.getDomainSet(newGrid);
            int             numTimeSteps = timeDomain.getLength();
            for (int timeStepIdx = 0; timeStepIdx < timeDomain.getLength();
                    timeStepIdx++) {
                FieldImpl sample = (FieldImpl) newGrid.getSample(timeStepIdx);
                float[][] timeStepValues = sample.getFloats(true);
                arrays.add(Misc.cloneArray(timeStepValues));
            }
            float[][] baseValue = null;
            if ((offset == 0) && (arrays.size() > 0)) {
                baseValue = Misc.cloneArray(arrays.get(0));
            }
            for (int timeStepIdx = arrays.size() - 1; timeStepIdx >= 0;
                    timeStepIdx--) {

                float[][] value = arrays.get(timeStepIdx);
                if ((baseValue == null) && (offset == 0)) {
                    baseValue = Misc.cloneArray(value);
                }
                //System.err.println("A:" + value);
                if ((offset == 0)
                        || ((timeStepIdx + offset >= 0)
                            && (timeStepIdx + offset < arrays.size()))) {
                    //If offset = 0  then use the base value
                    float[][] oldValue = ((offset == 0)
                                          ? baseValue
                                          : arrays.get(timeStepIdx + offset));
                    if (func.equals(FUNC_DIFFERENCE)) {
                        value = Misc.subtractArray(value, oldValue, value);
                    } else if (func.equals(FUNC_SUM)) {
                        value = Misc.addArray(value, oldValue, value);
                    } else {
                        throw new IllegalArgumentException(
                            "Unknown function:" + func);
                    }
                } else {
                    //System.err.println("filling");
                    Misc.fillArray(value, Float.NaN);
                }
                FlatField sample = (FlatField) newGrid.getSample(timeStepIdx);
                sample.setSamples(value, false);
                //                newGrid.setSample(timeStepIdx,sample);
            }
            return newGrid;
        } catch (CloneNotSupportedException cnse) {
            throw new VisADException("Cannot clone field");
        } catch (RemoteException re) {
            throw new VisADException("RemoteException in timeStepFunc");
        }
    }


    /**
     * Create a running average across the time dimension.
     *
     * @param grid    grid to average
     * @param wgts    weights per step (usually odd and add to 1)
     * @param opt     options for end points
     * <pre>
     *     N = {last point in the series}
     *    xi = {input series}
     *    xo = {output series}
     *    nwgt = {number of wgts}
     *
     *    opt < 0 : utilize cyclic conditions
     *          e.g., nwgt = 2
     *                 xo(0) = w(0) * xi(0) + w(1) * xi(1)
     *                 xo(N) = w(0) * xi(N) + w(1) * xi(0)
     *          e.g., nwgt = 3
     *                 xo(0) = w(0) * xi(N) + w(1) * xi(0) + w(2) * xi(1)
     *                 xo(N) = w(0) * xi(N - 1) + w(1) * xi(N) + w(2) * xi(0)
     *          e.g., nwgt = 4
     *                 xo(0) = w(0) * xi(N) + w(1) * xi(0) + w(2) * xi(1) + w(3) * xi(2)
     *                 xo(N) = w(0) * xi(N - 1) + w(1) * xi(N) + w(2) * xi(1) + w(3) * xi(2)
     *
     *    opt = 0 : set unsmoothed beginning and end pts to x@_FillValue (most common)
     *          e.g., nwgt = 2
     *                 xo(0) = w(0) * xi(0) + w(1) * xi(1)
     *                 xo(N) = xi@_FillValue
     *          e.g., nwgt = 3
     *                 xo(0) = xi@_FillValue
     *                 xo(1) = w(0) * xi(0) + w(1) * xi(1) + w(2) * xi(2)
     *                 xi(N) = xi@_FillValue
     *          e.g., nwgt = 4
     *                 xo(0)     = xi@_FillValue
     *                 xo(1)     = w(0) * xi(0) + w(1) * xi(1) + w(2) * xi(2) + w(3) * xi(3)
     *                 xo(N - 2) = w(0) * xi(N - 3) + w(1) * xi(N - 2) + w(2) * xi(N - 1) + w(3) * xi(N)
     *                 xo(N - 1) = xi@_FillValue
     *                 xo(N)     = xi@_FillValue
     *
     *    opt > 0 : utilize reflective (symmetric) conditions
     *          e.g., nwgt = 2
     *                 xo(0) = w(0) * xi(0) + w(1) * xi(1)
     *                 xo(N) = w(0) * xi(N) + w(0) * xi(0)
     *          e.g., nwgt = 3
     *                 xo(0) = w(0) * xi(1) + w(1) * xi(0) + w(2) * xi(1)
     *                 xo(N) = w(0) * xi(N - 1) + w(1) * xi(N) + w(2) * xi(N - 1)
     *          e.g., nwgt = 4
     *                 xo(0) = w(0) * xi(1) + w(1) * xi(0) + w(2) * xi(1) + w(3) * xi(2)
     *                 xo(N) = w(0) * xi(N - 1) + w(1) * xi(N) + w(2) * xi(0) + w(3) * xi(2)
     * </pre>
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl timeWeightedRunningAverage(FieldImpl grid,
            float[] wgts, int opt)
            throws VisADException {
        return timeWeightedRunningAverage(grid, wgts, opt, true);
    }

    /**
     * Create a running average across the time dimension.
     *
     * @param grid    grid to average
     * @param wgts    weights per step (usually odd and add to 1)
     * @param opt     options for end points
     * <pre>
     *     N = {last point in the series}
     *    xi = {input series}
     *    xo = {output series}
     *    nwgt = {number of wgts}
     *
     *    opt < 0 : utilize cyclic conditions
     *          e.g., nwgt = 2
     *                 xo(0) = w(0) * xi(0) + w(1) * xi(1)
     *                 xo(N) = w(0) * xi(N) + w(1) * xi(0)
     *          e.g., nwgt = 3
     *                 xo(0) = w(0) * xi(N) + w(1) * xi(0) + w(2) * xi(1)
     *                 xo(N) = w(0) * xi(N - 1) + w(1) * xi(N) + w(2) * xi(0)
     *          e.g., nwgt = 4
     *                 xo(0) = w(0) * xi(N) + w(1) * xi(0) + w(2) * xi(1) + w(3) * xi(2)
     *                 xo(N) = w(0) * xi(N - 1) + w(1) * xi(N) + w(2) * xi(1) + w(3) * xi(2)
     *
     *    opt = 0 : set unsmoothed beginning and end pts to x@_FillValue (most common)
     *          e.g., nwgt = 2
     *                 xo(0) = w(0) * xi(0) + w(1) * xi(1)
     *                 xo(N) = xi@_FillValue
     *          e.g., nwgt = 3
     *                 xo(0) = xi@_FillValue
     *                 xo(1) = w(0) * xi(0) + w(1) * xi(1) + w(2) * xi(2)
     *                 xi(N) = xi@_FillValue
     *          e.g., nwgt = 4
     *                 xo(0)     = xi@_FillValue
     *                 xo(1)     = w(0) * xi(0) + w(1) * xi(1) + w(2) * xi(2) + w(3) * xi(3)
     *                 xo(N - 2) = w(0) * xi(N - 3) + w(1) * xi(N - 2) + w(2) * xi(N - 1) + w(3) * xi(N)
     *                 xo(N - 1) = xi@_FillValue
     *                 xo(N)     = xi@_FillValue
     *
     *    opt > 0 : utilize reflective (symmetric) conditions
     *          e.g., nwgt = 2
     *                 xo(0) = w(0) * xi(0) + w(1) * xi(1)
     *                 xo(N) = w(0) * xi(N) + w(0) * xi(0)
     *          e.g., nwgt = 3
     *                 xo(0) = w(0) * xi(1) + w(1) * xi(0) + w(2) * xi(1)
     *                 xo(N) = w(0) * xi(N - 1) + w(1) * xi(N) + w(2) * xi(N - 1)
     *          e.g., nwgt = 4
     *                 xo(0) = w(0) * xi(1) + w(1) * xi(0) + w(2) * xi(1) + w(3) * xi(2)
     *                 xo(N) = w(0) * xi(N - 1) + w(1) * xi(N) + w(2) * xi(0) + w(3) * xi(2)
     * </pre>
     * @param skipMissing  if true, missing timesteps will not be included in the average
     *                     and the average will be done on the non-missing times
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl timeWeightedRunningAverage(FieldImpl grid,
            float[] wgts, int opt, boolean skipMissing)
            throws VisADException {
        return runave(grid, wgts, -1, opt, skipMissing);
    }

    /**
     * Create a running average across the time dimension.
     *
     * @param grid    grid to average
     * @param nave    number of steps to average
     * @param opt     options for end points
     * <pre>
     *     In the following:
     *
     *         N = {last point in the series, i.e. N = npts - 1}
     *         xi = {input series}
     *         xo = {output series}
     *
     *     opt < 0 : utilize cyclic conditions
     *                e.g., nave = 2
     *                      xo(0) = (xi(0) + xi(1))/nave
     *                      xo(N) = (xi(N) + xi(0))/nave
     *                e.g., nave = 3
     *                      xo(0) = (xi(N) + xi(0) + xi(1)) / nave
     *                      xo(N) = (xi(N - 1) + xi(N) + xi(0)) / nave
     *                e.g., nave = 4
     *                      xo(0) = (xi(N) + xi(0) + xi(1) + xi(2)) / nave
     *                      xo(N) = (xi(N - 1) + xi(N) + xi(0) + xi(1)) / nave
     *
     *     opt = 0 : set unsmoothed beginning and end pts to x@_FillValue [most common]
     *                e.g., nave = 2
     *                      xo(0) = (xi(0) + xi(1)) / nave
     *                      xo(N) = xi@_FillValue
     *                e.g., nave = 3
     *                      xo(0) = xi@_FillValue
     *                      xo(1) = (xi(0) + xi(1) + xi(2)) / nave
     *                      xi(N) = xi@_FillValue
     *                e.g., nave = 4
     *                      xo(0) = xi@_FillValue
     *                      xo(1) = (xi(0) + xi(1) + xi(2) + xi(3)) / nave
     *                      xo(N - 2) = (xi(N - 3) + xi(N - 2) + xi(N - 1) + xi(N)) / nave
     *                      xo(N - 1)= xi@_FillValue
     *                      xo(N)= xi@_FillValue
     *
     *     opt > 0 : utilize reflective (symmetric) conditions
     *                e.g., nave = 2
     *                      xo(0) = (xi(0) + xi(1)) / nave
     *                      xo(N) = (xi(N) + xi(N-1)) / nave
     *                e.g., nave = 3
     *                      xo(0) = (xi(1) + xi(0) + xi(1)) / nave
     *                      xo(N) = (xi(N - 1) + xi(N) + xi(N-1)) / nave
     *                e.g., nave = 4
     *                      xo(0) = (xi(2) + xi(1) + xi(0) + xi(1)) / nave
     *                      xo(N) = (xi(N - 1) + xi(N) + xi(N - 1) + xi(N - 2)) / nave
     * </pre>
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl timeRunningAverage(FieldImpl grid, int nave,
            int opt)
            throws VisADException {
        return timeRunningAverage(grid, nave, opt, true);
    }

    /**
     * Create a running average across the time dimension.
     *
     * @param grid    grid to average
     * @param nave    number of steps to average
     * @param opt     options for end points
     * <pre>
     *     In the following:
     *
     *         N = {last point in the series, i.e. N = npts - 1}
     *         xi = {input series}
     *         xo = {output series}
     *
     *     opt < 0 : utilize cyclic conditions
     *                e.g., nave = 2
     *                      xo(0) = (xi(0) + xi(1))/nave
     *                      xo(N) = (xi(N) + xi(0))/nave
     *                e.g., nave = 3
     *                      xo(0) = (xi(N) + xi(0) + xi(1)) / nave
     *                      xo(N) = (xi(N - 1) + xi(N) + xi(0)) / nave
     *                e.g., nave = 4
     *                      xo(0) = (xi(N) + xi(0) + xi(1) + xi(2)) / nave
     *                      xo(N) = (xi(N - 1) + xi(N) + xi(0) + xi(1)) / nave
     *
     *     opt = 0 : set unsmoothed beginning and end pts to x@_FillValue [most common]
     *                e.g., nave = 2
     *                      xo(0) = (xi(0) + xi(1)) / nave
     *                      xo(N) = xi@_FillValue
     *                e.g., nave = 3
     *                      xo(0) = xi@_FillValue
     *                      xo(1) = (xi(0) + xi(1) + xi(2)) / nave
     *                      xi(N) = xi@_FillValue
     *                e.g., nave = 4
     *                      xo(0) = xi@_FillValue
     *                      xo(1) = (xi(0) + xi(1) + xi(2) + xi(3)) / nave
     *                      xo(N - 2) = (xi(N - 3) + xi(N - 2) + xi(N - 1) + xi(N)) / nave
     *                      xo(N - 1)= xi@_FillValue
     *                      xo(N)= xi@_FillValue
     *
     *     opt > 0 : utilize reflective (symmetric) conditions
     *                e.g., nave = 2
     *                      xo(0) = (xi(0) + xi(1)) / nave
     *                      xo(N) = (xi(N) + xi(N-1)) / nave
     *                e.g., nave = 3
     *                      xo(0) = (xi(1) + xi(0) + xi(1)) / nave
     *                      xo(N) = (xi(N - 1) + xi(N) + xi(N-1)) / nave
     *                e.g., nave = 4
     *                      xo(0) = (xi(2) + xi(1) + xi(0) + xi(1)) / nave
     *                      xo(N) = (xi(N - 1) + xi(N) + xi(N - 1) + xi(N - 2)) / nave
     * </pre>
     * @param skipMissing  if true, missing timesteps will not be included in the average
     *                     and the average will be done on the non-missing times
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl timeRunningAverage(FieldImpl grid, int nave,
            int opt, boolean skipMissing)
            throws VisADException {
        return runave(grid, null, nave, opt, skipMissing);
    }

    /**
     * Create a running average across the time dimension.  If wgts is null,
     * use nave.  If wgts not null, nave is the number of weights
     *
     * @param grid    grid to average
     * @param wgts    weights per step (usually odd and add to 1)
     * @param nave    number of steps to average
     * @param opt     options for end points (OPT_MISSING, OPT_CYCLIC, OPT_SYMMETRIC)
     * @param skipMissing  if true, missing timesteps will not be included in the average
     *                     and the average will be done on the non-missing times
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    private static FieldImpl runave(FieldImpl grid, float[] wgts, int nave,
                                    int opt, boolean skipMissing)
            throws VisADException {

        float wsum = 0;
        if (wgts != null) {
            nave = wgts.length;
            for (int i = 0; i < nave; i++) {
                wsum += wgts[i];
            }
        } else {
            wgts = new float[nave];
            for (int i = 0; i < nave; i++) {
                wgts[i] = 1.0f;
                wsum    += 1f;
            }
        }
        if (wsum > 1.0) {
            wsum = 1f / wsum;
        }
        try {
            if ( !GridUtil.isTimeSequence(grid) || (nave == 1)) {
                return grid;
            }
            Set timeDomain   = Util.getDomainSet(grid);
            int numTimeSteps = timeDomain.getLength();
            int npts         = numTimeSteps;
            if (nave > numTimeSteps) {
                throw new VisADException(
                    "Number of average steps is greater than number of times");
            }
            FieldImpl newGrid = (FieldImpl) grid.clone();
            int       nav2    = nave / 2;
            int       noe     = (nave % 2 == 0)
                                ? 1
                                : 0;
            //System.out.println("nave = "+nave+", nav2 = "+nav2+", noe= "+noe);
            FlatField sample      = (FlatField) newGrid.getSample(0);
            float[][] missingData = Misc.cloneArray(sample.getFloats(false));
            Misc.fillArray(missingData, Float.NaN);
            int         lwork       = numTimeSteps + 2 * nav2;
            float[][]   values      = null;
            float[][][] work        = new float[lwork][][];
            float[][][] x           = new float[npts][][];
            int[]       timeIndices = new int[lwork];
            // Prefill with missing values (OPT_MISSING)
            //System.out.println("new array has " + lwork + " elements");
            for (int n = 0; n < lwork; n++) {
                work[n]        = missingData;
                timeIndices[n] = -1;
            }
            for (int n = 0; n < npts; n++) {
                sample = (FlatField) newGrid.getSample(n);
                float[][] timeStepValues =
                    Misc.cloneArray(sample.getFloats(false));
                //System.out.println("setting timestep " + n + " at position " + (nav2+n));
                x[n]                  = timeStepValues;
                work[nav2 + n]        = x[n];
                timeIndices[nav2 + n] = n;
            }
            int lpts = npts - 1;
            if (opt == OPT_SYMMETRIC) {
                for (int n = 0; n < nav2; n++) {
                    //System.out.println("setting work array " + (nav2-(n+1)) + " with time " + (n+1));
                    work[nav2 - (n + 1)]        = x[n + 1];
                    timeIndices[nav2 - (n + 1)] = n + 1;
                    //System.out.println("setting work array " + (lpts+nav2+(n+1)) + " with time " + (lpts-(n+1)));
                    work[lpts + nav2 + (n + 1)]        = x[lpts - (n + 1)];
                    timeIndices[lpts + nav2 + (n + 1)] = lpts - (n + 1);
                }
            } else if (opt == OPT_CYCLIC) {
                for (int n = 0; n < nav2; n++) {
                    //System.out.println("setting work array " + (nav2-(n+1)) + " with time " + (lpts-n));
                    work[nav2 - (n + 1)]        = x[lpts - n];
                    timeIndices[nav2 - (n + 1)] = lpts - n;
                    //System.out.println("setting work array " + (lpts+nav2+n+1) + " with time " + (n));
                    work[lpts + nav2 + n + 1]        = x[n];
                    timeIndices[lpts + nav2 + n + 1] = n;
                }
            }
            //Misc.printArray("times", timeIndices);
            for (int n = 0; n < npts; n++) {
                int       nmid   = n + nav2 + noe;
                int       mstart = nmid - nav2;
                int       mlast  = mstart + nave;
                float[][] sum    = Misc.cloneArray(missingData);
                Misc.fillArray(sum, 0);
                boolean   haveMissing = false;
                boolean[] missData    = new boolean[nave];
                int       idx         = 0;
                for (int m = mstart; m < mlast; m++) {
                    //System.out.println("for time " + n + ", adding time " + timeIndices[m]);
                    values = work[m];
                    if ( !Misc.isNaN(values)) {
                        // TODO:  what if single timestep values are missing?
                        for (int i = 0; i < values.length; i++) {
                            for (int j = 0; j < values[i].length; j++) {
                                sum[i][j] += values[i][j] * wgts[m - mstart];
                            }
                        }
                    } else {
                        haveMissing   = true;
                        missData[idx] = true;
                        continue;
                    }
                    idx++;
                }
                if (haveMissing) {  // recalculate wsum
                    for (int i = 0; i < nave; i++) {
                        if ( !missData[i]) {
                            wsum += wgts[i];
                        }
                    }
                    if (wsum > 1.0) {
                        wsum = 1f / wsum;
                    }
                }
                if ( !haveMissing || skipMissing) {
                    for (int i = 0; i < values.length; i++) {
                        for (int j = 0; j < values[i].length; j++) {
                            sum[i][j] *= wsum;
                        }
                    }
                } else {
                    sum = missingData;
                }
                sample = (FlatField) newGrid.getSample(n);
                sample.setSamples(sum, false);
            }
            return newGrid;
        } catch (CloneNotSupportedException cnse) {
            throw new VisADException("Cannot clone field");
        } catch (RemoteException re) {
            throw new VisADException("RemoteException in timeStepFunc");
        }
    }


    /**
     * Sum each grid point
     *
     * @param grid   grid to analyze
     * @param makeTimes If true then make a time field with the range
     *                  being the same computed value. If false then just
     *                  return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl sumOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return applyFunctionOverTime(grid, FUNC_SUM, makeTimes);
    }

    /**
     * Take the min value at each grid point
     *
     * @param grid   grid to analyze
     * @param makeTimes If true then make a time field with the range
     *                  being the same computed value. If false then just
     *                  return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl minOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return applyFunctionOverTime(grid, FUNC_MIN, makeTimes);
    }

    /**
     * Take the max value at each grid point
     *
     * @param grid   grid to analyze
     * @param makeTimes If true then make a time field with the range
     *                  being the same computed value. If false then just
     *                  return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl maxOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return applyFunctionOverTime(grid, FUNC_MAX, makeTimes);
    }

    /**
     * Apply the function to the time steps of the given grid.
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param function One of the FUNC_ enums
     * @param makeTimes If true then make a time field with the range
     *                  being the same computed value. If false then just
     *                  return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionOverTime(FieldImpl grid,
            String function, boolean makeTimes)
            throws VisADException {
        return applyFunctionOverTime(grid, function, 0, 1, makeTimes);
    }

    /**
     * Apply the function to the time steps of the given grid.
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to apply function to
     * @param function One of the FUNC_ enums
     * @param startIdx  starting time index
     * @param idxStride stride for time index
     * @param makeTimes If true then make a time field with the range
     *                  being the same computed value. If false then just
     *                  return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionOverTime(FieldImpl grid,
            String function, int startIdx, int idxStride, boolean makeTimes)
            throws VisADException {
        try {
            FlatField newGrid = null;
            if ( !GridUtil.isTimeSequence(grid)) {
                newGrid = (FlatField) grid.clone();
                newGrid.setSamples(grid.getFloats(false), true);
                return newGrid;
            }
            final boolean doMax      = function.equals(FUNC_MAX);
            final boolean doMin      = function.equals(FUNC_MIN);
            final boolean doStd      = function.equals(FUNC_STDEV);
            float[][]     values     = null;
            float[][]     values2    = null;
            int[][]       nums       = null;
            final Set     timeDomain = Util.getDomainSet(grid);
            for (int timeStepIdx = startIdx;
                    timeStepIdx < timeDomain.getLength();
                    timeStepIdx += idxStride) {
                FieldImpl sample = (FieldImpl) grid.getSample(timeStepIdx);
                float[][] timeStepValues = sample.getFloats(false);
                if (values == null) {  // first pass through
                    values = Misc.cloneArray(timeStepValues);
                    nums   = new int[values.length][values[0].length];
                    if (doStd) {
                        values2 = new float[values.length][values[0].length];
                    }
                    for (int i = 0; i < timeStepValues.length; i++) {
                        for (int j = 0; j < timeStepValues[i].length; j++) {
                            float value = timeStepValues[i][j];
                            if (value != value) {
                                continue;
                            }
                            nums[i][j] = 1;
                            if (doStd) {
                                values2[i][j] = value * value;
                            }
                        }
                    }
                    newGrid = (FlatField) sample.clone();
                    continue;
                }
                for (int i = 0; i < timeStepValues.length; i++) {
                    for (int j = 0; j < timeStepValues[i].length; j++) {
                        float value = timeStepValues[i][j];
                        if (value != value) {
                            continue;
                        }
                        if (doMax) {
                            values[i][j] = Math.max(values[i][j], value);
                        } else if (doMin) {
                            values[i][j] = Math.min(values[i][j], value);
                        } else if (doStd) {
                            values[i][j]  += value;
                            values2[i][j] += value * value;
                        } else {
                            values[i][j] += value;
                        }
                        nums[i][j]++;
                    }
                }
            }
            if (function.equals(FUNC_AVERAGE)) {
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < values[i].length; j++) {
                        int num = nums[i][j];
                        if (num > 0) {
                            values[i][j] = values[i][j] / num;
                        } else {
                            values[i][j] = Float.NaN;
                        }
                    }
                }
            }
            if (function.equals(FUNC_STDEV)) {
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < values[i].length; j++) {
                        int num = nums[i][j];
                        if (num > 0) {
                            float mean = values[i][j] / num;
                            float var  = values2[i][j] / num - mean * mean;
                            values[i][j] = (float) Math.sqrt(var);
                        } else {
                            values[i][j] = Float.NaN;
                        }
                    }
                }
            }
            newGrid.setSamples(values, false);
            if (makeTimes) {
                return (FieldImpl) Util.makeTimeField(newGrid,
                        GridUtil.getDateTimeList(grid));
            }
            return newGrid;
        } catch (CloneNotSupportedException cnse) {
            throw new VisADException("Cannot clone field");
        } catch (RemoteException re) {
            throw new VisADException(
                "RemoteException in applyFunctionOverTime");
        }

    }

    /**
     * Apply the function to the grids.  Grids must have the same domains
     * The function is one of the FUNC_ enums
     *
     * @param grids   grids to apply function to
     * @param function One of the FUNC_ enums
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FlatField applyFunctionOverGrids(FlatField[] grids,
            String function)
            throws VisADException {

        try {
            int       numGrids = grids.length;
            FlatField newGrid  = null;
            if (numGrids == 1) {
                newGrid = (FlatField) grids[0].clone();
                newGrid.setSamples(grids[0].getFloats(false), true);
                return newGrid;
            }
            final boolean doMax   = function.equals(FUNC_MAX);
            final boolean doMin   = function.equals(FUNC_MIN);
            final boolean doStd   = function.equals(FUNC_STDEV);
            float[][]     values  = null;
            float[][]     values2 = null;
            int[][]       nums    = null;

            for (int gridIdx = 0; gridIdx < numGrids; gridIdx++) {
                FieldImpl sample     = (FieldImpl) grids[gridIdx];
                float[][] gridValues = sample.getFloats(false);
                // be careful about missing grids
                if (Misc.isNaN(gridValues)) {
                    continue;
                }
                if (values == null) {  // first pass through
                    values = Misc.cloneArray(gridValues);
                    nums   = new int[values.length][values[0].length];
                    if (doStd) {
                        values2 = new float[values.length][values[0].length];
                    }
                    for (int i = 0; i < gridValues.length; i++) {
                        for (int j = 0; j < gridValues[i].length; j++) {
                            float value = gridValues[i][j];
                            if (value != value) {
                                continue;
                            }
                            nums[i][j] = 1;
                            if (doStd) {
                                values2[i][j] = value * value;
                            }
                        }
                    }
                    newGrid = (FlatField) sample.clone();
                    continue;
                }
                for (int i = 0; i < gridValues.length; i++) {
                    for (int j = 0; j < gridValues[i].length; j++) {
                        float value = gridValues[i][j];
                        if (value != value) {
                            continue;
                        }
                        if (doMax) {
                            values[i][j] = Math.max(values[i][j], value);
                        } else if (doMin) {
                            values[i][j] = Math.min(values[i][j], value);
                        } else if (doStd) {
                            values[i][j]  += value;
                            values2[i][j] += value * value;
                        } else {
                            values[i][j] += value;
                        }
                        nums[i][j]++;
                    }
                }
            }
            // all grids were missing
            if (newGrid == null) {
                return null;
            }
            if (function.equals(FUNC_AVERAGE)) {
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < values[i].length; j++) {
                        int num = nums[i][j];
                        if (num > 0) {
                            values[i][j] = values[i][j] / num;
                        } else {
                            values[i][j] = Float.NaN;
                        }
                    }
                }
            }
            if (function.equals(FUNC_STDEV)) {
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < values[i].length; j++) {
                        int num = nums[i][j];
                        if (num > 0) {
                            float mean = values[i][j] / num;
                            float var  = values2[i][j] / num - mean * mean;
                            values[i][j] = (float) Math.sqrt(var);
                        } else {
                            values[i][j] = Float.NaN;
                        }
                    }
                }
            }
            newGrid.setSamples(values, false);
            return newGrid;
        } catch (CloneNotSupportedException cnse) {
            throw new VisADException("Cannot clone field");
        } catch (RemoteException re) {
            throw new VisADException(
                "RemoteException in applyFunctionOverTime");
        }

    }

    /**
     * Apply the function to the ens members of the given grid.
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param statThreshold percent for FUNC_PRCNTL, probability threshold for FUNC_UPROB
     * @param exptdLoBoundIn _more_
     * @param exptdUpBoundIn _more_
     * @param function One of the FUNC_ enums
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionOverMembers(FieldImpl grid,
            String statThreshold, String exptdLoBoundIn,
            String exptdUpBoundIn, String function)
            throws VisADException {
        float  defaultExtreme = 999999;
        String empty          = "";
        float  exptdLoBound;
        if (exptdLoBoundIn.equals(empty)) {
            exptdLoBound = -defaultExtreme;
        } else {
            exptdLoBound = (float) Misc.parseNumber(exptdLoBoundIn);
        }

        float exptdUpBound;
        if (exptdUpBoundIn.equals(empty)) {
            exptdUpBound = defaultExtreme;
        } else {
            exptdUpBound = (float) Misc.parseNumber(exptdUpBoundIn);
        }

        return applyFunctionOverMembers(
            grid, (float) Misc.parseNumber(statThreshold), exptdLoBound,
            exptdUpBound, function);
    }

    /**
     * Apply the function to the ens members of the given grid.
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param statThreshold percent for FUNC_PRCNTL, probability threshold for FUNC_UPROB
     * @param exptdLoBound _more_
     * @param exptdUpBound _more_
     * @param function One of the FUNC_ enums
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionOverMembers(FieldImpl grid,
            float statThreshold, float exptdLoBound, float exptdUpBound,
            String function)
            throws VisADException {

        try {

            FieldImpl newGrid = null;
            if ( !GridUtil.isTimeSequence(grid)) {
                newGrid = (FlatField) grid.clone();
                //todo
                return newGrid;
            }


            final Set   timeDomain   = Util.getDomainSet(grid);
            int         numMembers   = 0;
            TupleType   rangeType    = null;
            TupleType   newRangeType = null;
            float[][][] valuesAll    = null;

            for (int timeStepIdx = 0; timeStepIdx < timeDomain.getLength();
                    timeStepIdx++) {
                FieldImpl sample = (FieldImpl) grid.getSample(timeStepIdx);
                FlatField newField;
                Set       ensDomain = sample.getDomainSet();
                float[][] values    = null;
                numMembers = ensDomain.getLength();
                GriddedSet newDomain = null;

                float[][]  stdevs    = null;
                for (int k = 0; k < numMembers; k++) {
                    FlatField innerField = (FlatField) sample.getSample(k,
                                               false);
                    if (innerField == null) {
                        continue;
                    }
                    newDomain =
                        (GriddedSet) GridUtil.getSpatialDomain(innerField);

                    if (newRangeType == null) {
                        newRangeType = GridUtil.makeNewParamType(
                            GridUtil.getParamType(innerField),
                            "_" + function);
                    }

                    float[][] ensStepValues = innerField.getFloats(false);
                    if (values == null) {
                        values = Misc.cloneArray(ensStepValues);
                        valuesAll =
                            new float[values.length][values[0].length][numMembers];
                    }
                    for (int i = 0; i < ensStepValues.length; i++) {
                        for (int j = 0; j < ensStepValues[i].length; j++) {
                            float value = ensStepValues[i][j];
                            if (value != value) {
                                continue;
                            }
                            valuesAll[i][j][k] = value;
                        }
                    }

                }
                // do the math
                if (function.equals(FUNC_PRCNTL) && (numMembers > 1)) {
                    int percent = (int) statThreshold;
                    for (int i = 0; i < values.length; i++) {
                        for (int j = 0; j < values[i].length; j++) {
                            values[i][j] =
                                evaluatePercentile(valuesAll[i][j], 0,
                                    numMembers, percent);
                        }
                    }
                }

                if (function.equals(FUNC_MODE) && (numMembers > 1)) {
                    for (int i = 0; i < values.length; i++) {
                        for (int j = 0; j < values[i].length; j++) {
                            values[i][j] = evaluateMode(valuesAll[i][j]);
                        }
                    }
                }

                if (function.equals(FUNC_UPROB) && (numMembers > 1)) {
                    for (int i = 0; i < values.length; i++) {
                        for (int j = 0; j < values[i].length; j++) {
                            // check if ens values are within bounds.
                            int     numValidMembers = numMembers;

                            float[] tmpValues       = new float[numMembers];

                            for (int k = 0; k < numMembers; k++) {
                                tmpValues[k] = valuesAll[i][j][k];
                            }


                            for (int k = 0; k < numMembers; k++) {
                                if ((tmpValues[k] < exptdLoBound)
                                        | (tmpValues[k] > exptdUpBound)) {
                                    numValidMembers--;
                                    for (int mm = k; mm < numValidMembers - 1;
                                            mm++) {
                                        tmpValues[mm] = tmpValues[mm + 1];
                                    }
                                    k--;
                                }
                            }

                            float[] newValues = new float[numValidMembers];
                            for (int k = 0; k < numValidMembers; k++) {
                                newValues[k] = tmpValues[k];
                            }
                            values[i][j] = evaluateUProbability(newValues,
                                    statThreshold, numMembers);
                        }
                    }
                }

                FunctionType newFT =
                    new FunctionType(
                        ((SetType) newDomain.getType()).getDomain(),
                        newRangeType);
                newField = new FlatField(newFT, newDomain);
                newField.setSamples(values, false);

                if (newGrid == null) {
                    FunctionType newFieldType =
                        new FunctionType(
                            ((SetType) timeDomain.getType()).getDomain(),
                            newField.getType());
                    newGrid = new FieldImpl(newFieldType, timeDomain);
                }

                newGrid.setSample(timeStepIdx, newField, false);

            }
            return newGrid;
        } catch (CloneNotSupportedException cnse) {
            throw new VisADException("Cannot clone field");
        } catch (RemoteException re) {
            throw new VisADException(
                "RemoteException in applyFunctionOverTime");
        }


    }

    /**
     * Apply the function to the ens members of the given grid.
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param function One of the FUNC_ enums
     *
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionOverMembers(FieldImpl grid,
            String function)
            throws VisADException {

        try {

            FieldImpl newGrid = null;
            if ( !GridUtil.isTimeSequence(grid)) {
                newGrid = (FlatField) grid.clone();
                //todo
                return newGrid;
            }
            final boolean doMax        = function.equals(FUNC_MAX);
            final boolean doMin        = function.equals(FUNC_MIN);
            final boolean doRange      = function.equals(FUNC_RNG);

            final Set     timeDomain   = Util.getDomainSet(grid);
            int           numMembers   = 0;
            TupleType     rangeType    = null;
            TupleType     newRangeType = null;

            for (int timeStepIdx = 0; timeStepIdx < timeDomain.getLength();
                    timeStepIdx++) {
                FieldImpl sample = (FieldImpl) grid.getSample(timeStepIdx);
                FlatField newField;
                Set       ensDomain = sample.getDomainSet();
                numMembers = ensDomain.getLength();
                GriddedSet newDomain = null;
                float[][]  values    = null;
                float[][]  stdevs    = null;
                float[][]  rangev    = null;

                for (int k = 0; k < numMembers; k++) {
                    FlatField innerField = (FlatField) sample.getSample(k,
                                               false);
                    if (innerField == null) {
                        continue;
                    }
                    newDomain =
                        (GriddedSet) GridUtil.getSpatialDomain(innerField);

                    if (newRangeType == null) {
                        newRangeType = GridUtil.makeNewParamType(
                            GridUtil.getParamType(innerField),
                            "_" + function);
                    }

                    float[][] ensStepValues = innerField.getFloats(false);
                    if (values == null) {
                        values = Misc.cloneArray(ensStepValues);
                        rangev = Misc.cloneArray(ensStepValues);
                        continue;
                    }
                    for (int i = 0; i < ensStepValues.length; i++) {
                        for (int j = 0; j < ensStepValues[i].length; j++) {
                            float value = ensStepValues[i][j];
                            if (value != value) {
                                continue;
                            }
                            if (doMax) {
                                values[i][j] = Math.max(values[i][j], value);
                            } else if (doMin) {
                                values[i][j] = Math.min(values[i][j], value);
                            } else if (doRange) {
                                values[i][j] = Math.max(values[i][j], value);
                                rangev[i][j] = Math.min(rangev[i][j], value);
                            } else {
                                values[i][j] += value;
                            }
                        }
                    }

                }
                // do the math
                if (function.equals(FUNC_AVERAGE) && (numMembers > 1)) {
                    for (int i = 0; i < values.length; i++) {
                        for (int j = 0; j < values[i].length; j++) {
                            values[i][j] = values[i][j] / numMembers;
                        }
                    }
                }

                if (function.equals(FUNC_RNG) && (numMembers > 1)) {
                    for (int i = 0; i < values.length; i++) {
                        for (int j = 0; j < values[i].length; j++) {
                            values[i][j] = values[i][j] - rangev[i][j];
                        }
                    }
                }

                if (function.equals(FUNC_STDEV) && (numMembers > 1)) {
                    //cal the average
                    stdevs = new float[values.length][values[0].length];
                    for (int i = 0; i < values.length; i++) {
                        for (int j = 0; j < values[i].length; j++) {
                            values[i][j] = values[i][j] / numMembers;
                        }
                    }
                    // diff * diff
                    for (int k = 0; k < numMembers; k++) {
                        FlatField innerField =
                            (FlatField) sample.getSample(k, false);
                        if (innerField == null) {
                            continue;
                        }

                        float[][] ensStepValues = innerField.getFloats(false);
                        for (int i = 0; i < ensStepValues.length; i++) {
                            for (int j = 0; j < ensStepValues[i].length;
                                    j++) {
                                float value = ensStepValues[i][j];
                                if (value != value) {
                                    continue;
                                }
                                stdevs[i][j] += (values[i][j] - value)
                                        * (values[i][j] - value);

                            }
                        }
                    }
                    values = Misc.cloneArray(stdevs);
                    // sqrt (v/n)
                    for (int i = 0; i < values.length; i++) {
                        for (int j = 0; j < values[i].length; j++) {
                            values[i][j] = (float) Math.sqrt(values[i][j]
                                    / (numMembers - 1));
                        }
                    }
                }

                FunctionType newFT =
                    new FunctionType(
                        ((SetType) newDomain.getType()).getDomain(),
                        newRangeType);
                newField = new FlatField(newFT, newDomain);
                newField.setSamples(values, false);


                if (newGrid == null) {
                    FunctionType newFieldType =
                        new FunctionType(
                            ((SetType) timeDomain.getType()).getDomain(),
                            newField.getType());
                    newGrid = new FieldImpl(newFieldType, timeDomain);
                }

                newGrid.setSample(timeStepIdx, newField, false);

            }


            return newGrid;

        } catch (CloneNotSupportedException cnse) {
            throw new VisADException("Cannot clone field");
        } catch (RemoteException re) {
            throw new VisADException(
                "RemoteException in applyFunctionOverTime");
        }


    }

    /**
     * Apply the function to the time steps of the given grid over the levels.
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param function One of the FUNC_ enums
     * @return the new field with the function applied at each point over
     *         the levels.
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionOverLevels(FieldImpl grid,
            String function)
            throws VisADException {
        FieldImpl newField = null;
        try {
            if (GridUtil.isTimeSequence(grid)) {
                Set          timeDomain = grid.getDomainSet();
                TupleType    rangeType  = null;
                Gridded2DSet newDomain  = null;
                for (int timeStepIdx = 0;
                        timeStepIdx < timeDomain.getLength(); timeStepIdx++) {
                    FieldImpl sample =
                        (FieldImpl) grid.getSample(timeStepIdx);
                    if (sample == null) {
                        continue;
                    }
                    FieldImpl funcFF = null;
                    if ( !GridUtil.isSequence(sample)) {
                        funcFF =
                            applyFunctionOverLevelsFF((FlatField) sample,
                                function, rangeType, newDomain);
                    } else {  // ensembles & such
                        Trace.call1(
                            "GridMath.applyFunctionOverLevels inner sequence");
                        Set ensDomain = sample.getDomainSet();
                        for (int j = 0; j < ensDomain.getLength(); j++) {
                            FlatField innerField =
                                (FlatField) sample.getSample(j, false);
                            if (innerField == null) {
                                continue;
                            }
                            FlatField innerFuncFF =
                                applyFunctionOverLevelsFF(innerField,
                                    function, rangeType, newDomain);
                            if (innerFuncFF == null) {
                                continue;
                            }
                            if (rangeType == null) {
                                rangeType =
                                    GridUtil.getParamType(innerFuncFF);
                            }
                            if (newDomain == null) {
                                newDomain =
                                    (Gridded2DSet) GridUtil.getSpatialDomain(
                                        innerFuncFF);
                            }
                            if (funcFF == null) {
                                FunctionType innerType =
                                    new FunctionType(
                                        DataUtility.getDomainType(ensDomain),
                                        innerFuncFF.getType());
                                funcFF = new FieldImpl(innerType, ensDomain);
                            }
                            funcFF.setSample(j, innerFuncFF, false);
                        }
                        Trace.call1(
                            "GridMath.applyFunctionOverLevels inner sequence");
                    }
                    if (funcFF == null) {
                        continue;
                    }
                    if (rangeType == null) {
                        rangeType = GridUtil.getParamType(funcFF);
                    }
                    if (newDomain == null) {
                        newDomain =
                            (Gridded2DSet) GridUtil.getSpatialDomain(funcFF);
                    }

                    if (newField == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeDomain.getType()).getDomain(),
                                funcFF.getType());
                        newField = new FieldImpl(newFieldType, timeDomain);
                    }
                    newField.setSample(timeStepIdx, funcFF, false);
                }
            } else {
                newField = applyFunctionOverLevelsFF((FlatField) grid,
                        function, null, null);
            }
            return newField;
        } catch (RemoteException re) {
            throw new VisADException(
                "RemoteException in applyFunctionOverLevels");
        }

    }


    /**
     * Apply the function to a single time step of a grid over the levels.
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param function One of the FUNC_ enums
     * @param newRangeType   the new range type.  if null, create
     * @param newDomain   the new spatial domain.  if null, create
     * @return the new field with the function applied at each point over
     *         the levels.
     *
     * @throws VisADException  On badness
     */
    private static FlatField applyFunctionOverLevelsFF(FlatField grid,
            String function, TupleType newRangeType, Gridded2DSet newDomain)
            throws VisADException {
        final boolean doMax = function.equals(FUNC_MAX);
        final boolean doMin = function.equals(FUNC_MIN);
        if (newRangeType == null) {
            newRangeType =
                GridUtil.makeNewParamType(GridUtil.getParamType(grid),
                                          "_" + function);
        }
        FlatField newField = null;
        try {
            GriddedSet domainSet =
                (GriddedSet) GridUtil.getSpatialDomain(grid);
            int[] lengths = domainSet.getLengths();
            int   sizeX   = lengths[0];
            int   sizeY   = lengths[1];
            int sizeZ = ((lengths.length == 2)
                         || (domainSet.getManifoldDimension() == 2))
                        ? 1
                        : lengths[2];
            float[][] samples   = grid.getFloats(false);
            float[][] newValues = new float[samples.length][sizeX * sizeY];
            for (int np = 0; np < samples.length; np++) {
                float[] paramVals = samples[np];
                float[] newVals   = newValues[np];
                for (int j = 0; j < sizeY; j++) {
                    for (int i = 0; i < sizeX; i++) {
                        int   numNonMissing = 0;
                        float result        = Float.NaN;
                        for (int k = 0; k < sizeZ; k++) {
                            int   index = k * sizeX * sizeY + j * sizeX + i;
                            float value = paramVals[index];
                            if (value != value) {
                                continue;
                            }
                            if (result != result) {  // first non-missing
                                result = value;
                                numNonMissing++;
                            } else {
                                if (doMax) {
                                    result = Math.max(result, value);
                                } else if (doMin) {
                                    result = Math.min(result, value);
                                } else {
                                    result += value;
                                    numNonMissing++;
                                }
                            }
                        }
                        if (function.equals(FUNC_AVERAGE)
                                && (numNonMissing != 0)) {
                            result = result / numNonMissing;
                        }
                        int newindex = j * sizeX + i;
                        newVals[newindex] = result;
                    }
                }
            }
            if (newDomain == null) {
                newDomain = GridUtil.makeDomain2D(domainSet);
            }
            FunctionType newFT =
                new FunctionType(((SetType) newDomain.getType()).getDomain(),
                                 newRangeType);
            newField = new FlatField(newFT, newDomain);
            newField.setSamples(newValues, false);

        } catch (RemoteException re) {
            throw new VisADException("RemoteException checking missing data");
        }
        return newField;

    }

    /**
     * Apply the function to the time steps of the given grid at each level.
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param function One of the FUNC_ enums
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionToLevels(FieldImpl grid,
            String function)
            throws VisADException {
        FieldImpl newField = null;
        try {
            if (GridUtil.isTimeSequence(grid)) {
                Set       timeDomain = grid.getDomainSet();
                TupleType rangeType  = null;
                for (int timeStepIdx = 0;
                        timeStepIdx < timeDomain.getLength(); timeStepIdx++) {
                    FieldImpl sample =
                        (FieldImpl) grid.getSample(timeStepIdx);
                    if (sample == null) {
                        continue;
                    }
                    FieldImpl funcFF = null;
                    if ( !GridUtil.isSequence(sample)) {
                        funcFF = applyFunctionToLevelsFF((FlatField) sample,
                                function, rangeType);
                    } else {  // ensembles & such
                        Trace.call1(
                            "GridMath.applyFunctionOverLevels inner sequence");
                        Set ensDomain = sample.getDomainSet();
                        for (int j = 0; j < ensDomain.getLength(); j++) {
                            FlatField innerField =
                                (FlatField) sample.getSample(j, false);
                            if (innerField == null) {
                                continue;
                            }
                            FlatField innerFuncFF =
                                applyFunctionToLevelsFF(innerField, function,
                                    rangeType);
                            if (innerFuncFF == null) {
                                continue;
                            }
                            if (rangeType == null) {
                                rangeType =
                                    GridUtil.getParamType(innerFuncFF);
                            }
                            if (funcFF == null) {
                                FunctionType innerType =
                                    new FunctionType(
                                        DataUtility.getDomainType(ensDomain),
                                        innerFuncFF.getType());
                                funcFF = new FieldImpl(innerType, ensDomain);
                            }
                            funcFF.setSample(j, innerFuncFF, false);
                        }
                        Trace.call1(
                            "GridMath.applyFunctionOverLevels inner sequence");
                    }
                    if (funcFF == null) {
                        continue;
                    }
                    if (rangeType == null) {
                        rangeType = GridUtil.getParamType(funcFF);
                    }

                    if (newField == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeDomain.getType()).getDomain(),
                                funcFF.getType());
                        newField = new FieldImpl(newFieldType, timeDomain);
                    }
                    newField.setSample(timeStepIdx, funcFF, false);
                }
            } else {
                newField = applyFunctionToLevelsFF((FlatField) grid,
                        function, null);
            }
            return newField;
        } catch (RemoteException re) {
            throw new VisADException("RemoteException checking missing data");
        }

    }

    /**
     * Apply the function to a single time step of a grid
     * The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param function One of the FUNC_ enums
     * @param newRangeType   the new range type.  if null, create
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    private static FlatField applyFunctionToLevelsFF(FlatField grid,
            String function, TupleType newRangeType)
            throws VisADException {
        final boolean doMax = function.equals(FUNC_MAX);
        final boolean doMin = function.equals(FUNC_MIN);
        if (newRangeType == null) {
            newRangeType =
                GridUtil.makeNewParamType(GridUtil.getParamType(grid),
                                          "_" + function);
        }
        FlatField newField = (FlatField) GridUtil.setParamType(grid,
                                 newRangeType, true);
        try {
            GriddedSet domainSet =
                (GriddedSet) GridUtil.getSpatialDomain(grid);
            int[] lengths = domainSet.getLengths();
            int   sizeX   = lengths[0];
            int   sizeY   = lengths[1];
            int sizeZ = ((lengths.length == 2)
                         || (domainSet.getManifoldDimension() == 2))
                        ? 1
                        : lengths[2];
            float[][] samples   = grid.getFloats(false);
            float[][] newValues = newField.getFloats(false);
            for (int np = 0; np < samples.length; np++) {
                float[] paramVals = newValues[np];
                for (int k = 0; k < sizeZ; k++) {
                    int   numNonMissing = 0;
                    float result        = Float.NaN;
                    for (int j = 0; j < sizeY; j++) {
                        for (int i = 0; i < sizeX; i++) {
                            int   index = k * sizeX * sizeY + j * sizeX + i;
                            float value = paramVals[index];
                            if (value != value) {
                                continue;
                            }
                            if (result != result) {
                                result = value;
                                numNonMissing++;
                            } else {
                                if (doMax) {
                                    result = Math.max(result, value);
                                } else if (doMin) {
                                    result = Math.min(result, value);
                                } else {
                                    result += value;
                                    numNonMissing++;
                                }
                            }
                        }
                    }
                    if (function.equals(FUNC_AVERAGE)
                            && (numNonMissing != 0)) {
                        result = result / numNonMissing;
                    }
                    for (int j = 0; j < sizeY; j++) {
                        for (int i = 0; i < sizeX; i++) {
                            int index = k * sizeX * sizeY + j * sizeX + i;
                            if (paramVals[index] == paramVals[index]) {
                                paramVals[index] = result;
                            }
                        }
                    }
                }
            }

            newField.setSamples(newValues, false);

        } catch (RemoteException re) {
            throw new VisADException("RemoteException checking missing data");
        }
        return newField;

    }

    /**
     * Apply the function to the x or y of the given grid at each level.
     * The function is one of the FUNC_ enumerations
     *
     * @param grid   grid to apply
     * @param function One of the FUNC_ enumerations
     * @param axis the axis to apply the function (AXIS_X or AXIS_Y)
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionToAxis(FieldImpl grid,
            String function, String axis)
            throws VisADException {
        FieldImpl newField = null;
        try {
            if (GridUtil.isTimeSequence(grid)) {
                Set       timeDomain = grid.getDomainSet();
                TupleType rangeType  = null;
                for (int timeStepIdx = 0;
                        timeStepIdx < timeDomain.getLength(); timeStepIdx++) {
                    FieldImpl sample =
                        (FieldImpl) grid.getSample(timeStepIdx);
                    if (sample == null) {
                        continue;
                    }
                    FieldImpl funcFF = null;
                    if ( !GridUtil.isSequence(sample)) {
                        funcFF = applyFunctionToAxisFF((FlatField) sample,
                                function, axis, rangeType);
                    } else {  // ensembles & such
                        Trace.call1(
                            "GridMath.applyFunctionToAxis inner sequence");
                        Set ensDomain = sample.getDomainSet();
                        for (int j = 0; j < ensDomain.getLength(); j++) {
                            FlatField innerField =
                                (FlatField) sample.getSample(j, false);
                            if (innerField == null) {
                                continue;
                            }
                            FlatField innerFuncFF =
                                applyFunctionToAxisFF(innerField, function,
                                    axis, rangeType);
                            if (innerFuncFF == null) {
                                continue;
                            }
                            if (rangeType == null) {
                                rangeType =
                                    GridUtil.getParamType(innerFuncFF);
                            }
                            if (funcFF == null) {
                                FunctionType innerType =
                                    new FunctionType(
                                        DataUtility.getDomainType(ensDomain),
                                        innerFuncFF.getType());
                                funcFF = new FieldImpl(innerType, ensDomain);
                            }
                            funcFF.setSample(j, innerFuncFF, false);
                        }
                        Trace.call1(
                            "GridMath.applyFunctionToAxis inner sequence");
                    }
                    if (funcFF == null) {
                        continue;
                    }
                    if (rangeType == null) {
                        rangeType = GridUtil.getParamType(funcFF);
                    }

                    if (newField == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeDomain.getType()).getDomain(),
                                funcFF.getType());
                        newField = new FieldImpl(newFieldType, timeDomain);
                    }
                    newField.setSample(timeStepIdx, funcFF, false);
                }
            } else {
                newField = applyFunctionToAxisFF((FlatField) grid, function,
                        axis, null);
            }
            return newField;
        } catch (RemoteException re) {
            throw new VisADException("RemoteException checking missing data");
        }

    }

    /**
     * Apply the function to the axis of a single time step of a grid
     * The function is one of the FUNC_ enumerations
     *
     * @param grid   grid to average
     * @param function One of the FUNC_ enumerations
     * @param axis the axis to apply it to
     * @param newRangeType   the new range type.  if null, create
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    private static FlatField applyFunctionToAxisFF(FlatField grid,
            String function, String axis, TupleType newRangeType)
            throws VisADException {
        final boolean doMax = function.equals(FUNC_MAX);
        final boolean doMin = function.equals(FUNC_MIN);
        if (newRangeType == null) {
            newRangeType =
                GridUtil.makeNewParamType(GridUtil.getParamType(grid),
                                          "_" + axis + function);
        }
        FlatField newField = (FlatField) GridUtil.setParamType(grid,
                                 newRangeType, true);
        try {
            GriddedSet domainSet =
                (GriddedSet) GridUtil.getSpatialDomain(grid);
            int[] lengths = domainSet.getLengths();
            int   sizeX   = lengths[0];
            int   sizeY   = lengths[1];
            int sizeZ = ((lengths.length == 2)
                         || (domainSet.getManifoldDimension() == 2))
                        ? 1
                        : lengths[2];
            float[][] samples   = grid.getFloats(false);
            float[][] newValues = newField.getFloats(false);
            int       outer     = (axis.equals(AXIS_X))
                                  ? sizeY
                                  : sizeX;
            int       inner     = (axis.equals(AXIS_X))
                                  ? sizeX
                                  : sizeY;
            for (int np = 0; np < samples.length; np++) {
                float[] paramVals = newValues[np];
                for (int k = 0; k < sizeZ; k++) {
                    for (int j = 0; j < outer; j++) {
                        int   numNonMissing = 0;
                        float result        = Float.NaN;
                        for (int i = 0; i < inner; i++) {
                            int   index = (axis.equals(AXIS_X))
                                          ? k * inner * outer + j * inner + i
                                          : k * inner * outer + i * outer + j;
                            float value = paramVals[index];
                            if (value != value) {
                                continue;
                            }
                            if (result != result) {
                                result = value;
                                numNonMissing++;
                            } else {
                                if (doMax) {
                                    result = Math.max(result, value);
                                } else if (doMin) {
                                    result = Math.min(result, value);
                                } else {
                                    result += value;
                                    numNonMissing++;
                                }
                            }
                        }
                        if (function.equals(FUNC_AVERAGE)
                                && (numNonMissing != 0)) {
                            result = result / numNonMissing;
                        }
                        for (int i = 0; i < inner; i++) {
                            int index = (axis.equals(AXIS_X))
                                        ? k * inner * outer + j * inner + i
                                        : k * inner * outer + i * outer + j;
                            if (paramVals[index] == paramVals[index]) {
                                paramVals[index] = result;
                            }
                        }
                    }
                }
            }

            newField.setSamples(newValues, false);

        } catch (RemoteException re) {
            throw new VisADException("RemoteException checking missing data");
        }
        return newField;

    }



    /**
     * Take the partial derivative with respect to X of the given field.
     * @param grid   grid to parialize
     * @return partialized grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl ddx(FieldImpl grid)
            throws VisADException, RemoteException {
        return partial(grid, 0);
    }

    /**
     * Take the partial derivative with respect to Y of the given field.
     * @param grid   grid to parialize
     * @return partialized grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl ddy(FieldImpl grid)
            throws VisADException, RemoteException {
        return partial(grid, 1);
    }

    /**
     * Take the partial derivative with respect variable at the domain index.
     * @param grid   grid to parialize
     * @param domainIndex  index of variable to use  for derivative
     * @return partialized grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl partial(FieldImpl grid, int domainIndex)
            throws VisADException, RemoteException {
        SampledSet ss = GridUtil.getSpatialDomain(grid);
        RealType rt =
            (RealType) ((SetType) ss.getType()).getDomain().getComponent(
                domainIndex);
        return partial(grid, rt);
    }

    /**
     * Take the partial for the spatial domain of a grid.
     *
     * @param grid  FlatField to take the partial of
     * @param var  RealType for the partial
     *
     * @return  partial derivative
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FieldImpl partial(FieldImpl grid, RealType var)
            throws VisADException, RemoteException {
        boolean   isSequence = GridUtil.isTimeSequence(grid);
        FieldImpl retField   = null;
        if (isSequence) {
            Set          s         = GridUtil.getTimeSet(grid);
            Boolean      ensble    = GridUtil.hasEnsemble(grid);

            TupleType    rangeType = null;
            FunctionType innerType = null;
            for (int i = 0; i < s.getLength(); i++) {

                FieldImpl funcFF = null;
                if (ensble) {
                    FieldImpl sample    = (FieldImpl) grid.getSample(i);
                    Set       ensDomain = sample.getDomainSet();

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField =
                            (FlatField) sample.getSample(j, false);
                        if (innerField == null) {
                            continue;
                        }
                        FlatField innerFuncFF = partial(innerField, var);
                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerFuncFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerFuncFF.getType());
                        }
                        if (funcFF == null) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }
                        funcFF.setSample(j, innerFuncFF, false);
                    }
                    if (i == 0) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) s.getType()).getDomain(),
                                funcFF.getType());
                        retField = new FieldImpl(newFieldType, s);
                    }
                    retField.setSample(i, funcFF, false);
                } else {
                    FlatField f = partial(((FlatField) grid.getSample(i)),
                                          var);
                    if (i == 0) {
                        FunctionType ftype =
                            new FunctionType(
                                ((SetType) s.getType()).getDomain(),
                                f.getType());
                        retField = new FieldImpl(ftype, s);
                    }
                    retField.setSample(i, f, false);
                }
            }

        } else {
            retField = partial(((FlatField) grid), var);
        }
        return retField;
    }

    /**
     * Take the partial for the FlatField.
     *
     * @param f  FlatField to take the partial of
     * @param var  RealType for the partial
     *
     * @return the derivative
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField partial(FlatField f, RealType var)
            throws VisADException, RemoteException {
        FlatField  fToUse       = f;
        SampledSet domain       = GridUtil.getSpatialDomain(f);
        boolean    twoDManifold = false;
        // check for a slice
        if (domain.getDimension() != domain.getManifoldDimension()) {
            twoDManifold = true;
            fToUse = (FlatField) GridUtil.make2DGridFromSlice(fToUse, false);
            if ( !MathType.findScalarType(fToUse.getType(), var)) {
                throw new VisADException(
                    "Multiple levels needed for partial with respect to vertical dimension");
            }
        }

        FlatField retField = (FlatField) fToUse.derivative(var,
                                 Data.NO_ERRORS);
        if (twoDManifold) {
            retField = (FlatField) GridUtil.setSpatialDomain(retField,
                    domain);
        }
        if (var.equals(RealType.Longitude)
                || var.getName().toLowerCase().startsWith("lon")) {
            FlatField latGrid =
                (FlatField) DerivedGridFactory.createLatitudeGrid(retField);
            FlatField latCosGrid = (FlatField) latGrid.cosDegrees();
            // account for 0 at poles.
            latCosGrid = (FlatField) latCosGrid.max(
                new Real(Math.cos(Math.toRadians(89))));
            FlatField factor = (FlatField) latCosGrid.multiply(KM_PER_DEGREE);
            //visad.python.JPythonMethods.dumpTypes(factor);
            retField = (FlatField) retField.divide(factor);
            //visad.python.JPythonMethods.dumpTypes(retField);
        } else if (var.equals(RealType.Latitude)
                   || var.getName().toLowerCase().startsWith("lat")) {
            retField = (FlatField) retField.divide(KM_PER_DEGREE);
        }
        return retField;
    }

    /**
     * evaluate percentile value
     *
     * @param values the values
     * @param begin  the starting index
     * @param length  number of points
     * @param p  the percentage
     * @return the percentile
     *
     * @throws VisADException   VisAD Error
     */
    public static float evaluatePercentile(final float[] values,
                                           final int begin, final int length,
                                           final double p)
            throws VisADException {


        if ((p > 100) || (p <= 0)) {
            throw new VisADException(
                "out of bounds percentile value:  must be in (0, 100)");
        }
        if (length == 0) {
            return Float.NaN;
        }
        if (length == 1) {
            return values[begin];  // always return single value for n = 1
        }
        double  n      = length;
        double  pos    = p * (n + 1) / 100;
        double  fpos   = Math.floor(pos);
        int     intPos = (int) fpos;
        float   dif    = (float) (pos - fpos);
        float[] sorted = new float[length];
        System.arraycopy(values, begin, sorted, 0, length);
        QuickSort.sort(sorted);

        if (pos < 1) {
            return sorted[0];
        }
        if (pos >= n) {
            return sorted[length - 1];
        }
        float lower = sorted[intPos - 1];
        float upper = sorted[intPos];
        return lower + dif * (upper - lower);
    }

    /**
     * evaluate mode value
     *
     *
     * @param data _more_
     * @return the percentile
     *
     */
    public static float evaluateMode(float[] data) {
        int   t, w;
        float md, oldmd;
        int   count, oldcount;
        int   size = data.length;

        oldmd    = 0;
        oldcount = 0;
        for (t = 0; t < size; t++) {
            md    = data[t];
            count = 1;
            for (w = t + 1; w < size; w++) {
                if (md == data[w]) {
                    count++;
                }
            }
            if (count > oldcount) {
                oldmd    = md;
                oldcount = count;
            }
        }
        return oldmd;
    }

    /**
     * evaluate univariate probability of "variable with n ensemble values" < pValue
     *
     * code from $NAWIPS/gempak/source/diaglib/de/decprb.c used to make this function.
     *
     * @param values the values, within the userspecified range, at a given grid point
     *        from an ensemble model run
     * @param pValue the threshold used in the probability calculation - P(value < pValue)
     * @param length  number of ensemble members (might not be the same as values.length)
     *
     * @return prob the univariate probability that the value at the grid point is less than pValue
     *
     * @throws VisADException   VisAD Error
     */
    public static float evaluateUProbability(float[] values,
                                             final float pValue,
                                             final int length)
            throws VisADException {

        // TODO: allow users the chance to set custom weights
        double floatDiffTol = 0.000001D;  // tolerance for comparing if two floats are the same (used to replace G_DIFF calls from GEMPAK c code)
        double[] weights = new double[values.length];  // holder for fake weights until ens. weights are passed into function
        for (int ii = 0; ii < values.length; ii++) {
            weights[ii] = 1.0d / (double) values.length;
        }
        /*
         * Bubble sorting the grid values in emvalu with
         * emvalue (1) lowest and emvalu (nummbr) highest.
         */

        int    iswflg = 1;
        int    istop  = values.length - 1;
        float  swpbuf;
        double wtbuf;
        while ((iswflg != 0) && (istop > 0)) {
            iswflg = 0;
            for (int kk = 0; kk < istop; kk++) {
                if (values[kk] > values[kk + 1]) {
                    iswflg          = 1;
                    swpbuf          = values[kk];
                    wtbuf           = weights[kk];
                    values[kk]      = values[kk + 1];
                    weights[kk]     = weights[kk + 1];
                    values[kk + 1]  = swpbuf;
                    weights[kk + 1] = wtbuf;
                }
            }
            istop--;
        }

        /*
         * Check for identical values and compute intrinsic weight
         * frequency (zfreq).
         */
        int mm = values.length;
        /*
         * Initialize intrinsic weight frequency array.
         */
        double[] zfreq = new double[values.length];

        for (int kk = 0; kk < mm; kk++) {
            zfreq[kk] = 1.0D;
        }
        double tol = 0.001D * (values[mm - 1] - values[0]) / mm;
        for (int kk = 0; kk < mm - 1; kk++) {
            if (Math.abs(values[kk] - values[kk + 1]) <= tol) {
                weights[kk] += weights[kk + 1];
                zfreq[kk]   = zfreq[kk] + 1.0D;
                mm--;
                for (int jj = kk; jj < mm - 1; jj++) {
                    values[jj]  = values[jj + 1];
                    weights[jj] = weights[jj + 1];
                }
                kk--;
            }
        }
        /*
         * Fabricate order statistics if it has collapsed to a single value.
         */
        if (mm == 1) {
            if (Math.abs(values[0] - 0.0D) < floatDiffTol) {
                values[0] = -0.00001F;
                values[1] = 0.00001F;
            } else {
                float delta = 0.00001F * Math.abs(values[0]);
                values[1] = values[0] + delta;
                values[0] -= delta;
            }
            weights[0] = 0.5D;
            weights[1] = 0.5D;
            mm         = 2;
            zfreq[0]   = 1.0D;
            zfreq[1]   = 1.0D;
        }
        /*
         *Compute and sum intrinsic weights.
         */
        double[] zwts = new double[mm];
        zwts[0] = zfreq[0] / (values[1] - values[0]);
        double zsum = zwts[0];
        for (int kk = 1; kk < mm - 1; kk++) {
            zwts[kk] = (zfreq[kk] * 2.0F) / (values[kk + 1] - values[kk - 1]);
            zsum     = zsum + zwts[kk];
        }
        zwts[mm - 1] = zfreq[mm - 1] / (values[mm - 1] - values[mm - 2]);
        zsum         = zsum + zwts[mm - 1];
        /*
         * Scale external weights by normalized intrinsic weights and
         * normalize.
         */
        double psum = 0.0D;
        for (int kk = 0; kk < mm; kk++) {
            weights[kk] = (zwts[kk] / zsum) * weights[kk];
            psum        = psum + weights[kk];
        }
        for (int kk = 0; kk < mm; kk++) {
            weights[kk] = weights[kk] / psum;
        }
        /*
         * Compute Qun, the area; Vn, the normalized value;
         * w(), normalized weight; and qlt, qrt.
         */
        double vn = 0.0D;
        for (int kk = 1; kk < mm; kk++) {
            vn += 0.5D * (weights[kk] + weights[kk - 1])
                  * (values[kk] - values[kk - 1]);
        }
        vn = vn / (1.0D - 2.0D / ((double) length + 1));
        for (int kk = 0; kk < mm; kk++) {
            weights[kk] = weights[kk] / vn;
        }
        double qlt = values[0]
                     - 2.0D / (weights[0] * ((double) length + 1.0D));
        double qrt = values[mm - 1]
                     + 2.0D / (weights[mm - 1] * ((double) length + 1.0D));

        double[] newWeights = new double[mm + 2];
        double[] newValues  = new double[mm + 2];

        newWeights[0]      = 0.0D;
        newWeights[mm + 1] = 0.0D;
        newValues[0]       = qlt;
        newValues[mm + 1]  = qrt;

        for (int ii = 1; ii < mm + 1; ii++) {
            newWeights[ii] = weights[ii - 1];
            newValues[ii]  = values[ii - 1];
        }
        /*
         * Start computing univariate probability output.
         */
        float prob = 0.0F;  // probability of value at grid point < pValue)

        if (pValue < newValues[0]) {
            prob = 0.0F;
        } else if (pValue > newValues[mm + 1]) {
            prob = 1.0F;
        } else {
            psum = 0.0D;
            for (int kk = 1; kk < mm + 2; kk++) {
                if (Math.abs(pValue - newValues[kk - 1]) < floatDiffTol) {
                    prob = (float) psum;
                    break;
                } else if (pValue >= newValues[kk]) {
                    psum += 0.5D * (newWeights[kk] + newWeights[kk - 1])
                            * (newValues[kk] - newValues[kk - 1]);
                } else if (pValue > newValues[kk - 1]) {
                    double ww = newWeights[kk - 1]
                                + (newWeights[kk] - newWeights[kk - 1])
                                  * (pValue - newValues[kk - 1])
                                  / (newValues[kk] - newValues[kk - 1]);
                    double fta = 0.5D * (ww + newWeights[kk - 1])
                                 * (pValue - newValues[kk - 1]);
                    prob = (float) psum + (float) fta;
                    break;
                }
            }
        }
        return prob;

    }
}

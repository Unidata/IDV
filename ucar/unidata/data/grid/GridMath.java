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

package ucar.unidata.data.grid;


import ucar.unidata.data.DataUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import ucar.visad.Util;

import visad.Data;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.GriddedSet;
import visad.MathType;
import visad.Real;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.TupleType;
import visad.Unit;
import visad.VisADException;

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
    public static final String FUNC_SUM = "sum";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_MAX = "max";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_MIN = "min";

    /** function for the timeStepFunc routine */
    public static final String FUNC_DIFFERENCE = "difference";

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
        return doMath(grid1, grid2, Data.ADD);
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
        return doMath(grid1, grid2, Data.SUBTRACT);
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
        return doMath(grid1, grid2, Data.MULTIPLY);
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
        return doMath(grid1, grid2, Data.DIVIDE);
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
        return doMath(grid1, grid2, Data.ATAN2);
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
        FieldImpl a         = grid1;
        FieldImpl b         = grid2;
        boolean   is3D1     = GridUtil.is3D(grid1);
        boolean   is3D2     = GridUtil.is3D(grid2);
        boolean   isVolume1 = GridUtil.isVolume(grid1);
        boolean   isVolume2 = GridUtil.isVolume(grid2);
        boolean   isSlice1  = !isVolume1 && is3D1;
        boolean   isSlice2  = !isVolume2 && is3D2;
        boolean equalDomains = Misc.equals(GridUtil.getSpatialDomain(grid1),
                                           GridUtil.getSpatialDomain(grid2));

        if (isSlice1 && isSlice2) {
            if ( !equalDomains) {
                a = GridUtil.make2DGridFromSlice(grid1, false);
                b = GridUtil.make2DGridFromSlice(grid2, false);
            }
        } else if (isSlice1 && !is3D2) {
            a = GridUtil.make2DGridFromSlice(grid1, false);
        } else if ( !is3D1 && isSlice2) {
            b = GridUtil.make2DGridFromSlice(grid2, false);
        }
        int     mode      = (equalDomains)
                            ? Data.NEAREST_NEIGHBOR
                            : Data.WEIGHTED_AVERAGE;

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
     * Average the grid over time
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
     * This creates a field where D(T) = D(T)+D(0)
     * Any time steps up to the offset time are set to missing
     * @param grid   grid to average
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl sumFromBaseTime(FieldImpl grid)
            throws VisADException {
        return timeStepFunc(grid, 0, FUNC_SUM);
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
        try {
            FlatField newGrid = null;
            if ( !GridUtil.isTimeSequence(grid)) {
                newGrid = (FlatField) grid.clone();
                newGrid.setSamples(grid.getFloats(false), true);
                return newGrid;
            }
            final boolean doMax        = function.equals(FUNC_MAX);
            final boolean doMin        = function.equals(FUNC_MIN);
            float[][]     values       = null;
            final Set     timeDomain   = Util.getDomainSet(grid);
            int           numTimeSteps = timeDomain.getLength();
            for (int timeStepIdx = 0; timeStepIdx < timeDomain.getLength();
                    timeStepIdx++) {
                FieldImpl sample = (FieldImpl) grid.getSample(timeStepIdx);
                float[][] timeStepValues = sample.getFloats(false);
                if (values == null) {
                    values  = Misc.cloneArray(timeStepValues);
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
                        } else {
                            values[i][j] += value;
                        }
                    }
                }
            }
            if (function.equals(FUNC_AVERAGE) && (numTimeSteps > 0)) {
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < values[i].length; j++) {
                        values[i][j] = values[i][j] / numTimeSteps;
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
                    FlatField sample =
                        (FlatField) grid.getSample(timeStepIdx);
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
                                newDomain =
                                    (Gridded2DSet) GridUtil.getSpatialDomain(
                                        innerFuncFF);
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
            Set s = GridUtil.getTimeSet(grid);
            for (int i = 0; i < s.getLength(); i++) {
                FlatField f = partial(((FlatField) grid.getSample(i)), var);
                if (i == 0) {
                    FunctionType ftype =
                        new FunctionType(((SetType) s.getType()).getDomain(),
                                         f.getType());
                    retField = new FieldImpl(ftype, s);
                }
                retField.setSample(i, f, false);
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


}

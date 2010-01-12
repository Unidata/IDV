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

package ucar.visad;


import visad.*;

import visad.util.DataUtility;



import java.rmi.RemoteException;

import java.util.Iterator;
import java.util.Vector;


/**
 * Utility class: provides support for mathematical operations on VisAD data
 * objects.
 *
 * @author Steven R. Emmerson
 *
 * $Id: VisADMath.java,v 1.15 2005/05/13 18:34:06 jeffmc Exp $
 */
public final class VisADMath {

    /**
     * The dimensionless constant 1.
     */
    private static final Real one;

    static {
        Real o = null;

        try {
            o = new Real(RealType.getRealType("ucar_unidata_visad_Util_One",
                    CommonUnit.dimensionless, (Set) null), 1.0);
        } catch (Exception e) {
            String reason = e.getMessage();

            System.err.println("Couldn't initialize class Util"
                               + ((reason == null)
                                  ? ""
                                  : ": " + reason));
        }

        one = o;
    }

    /**
     * Default constructor
     */
    private VisADMath() {}

    /**
     * Negates a data object.
     *
     * @param data              The data object to be negated.
     * @return                  The result of negating the data object.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data negate(Data data)
            throws VisADException, RemoteException {

        return (data instanceof SampledSet)
               ? negate((SampledSet) data)
               : data.negate();
    }

    /**
     * Negates a SampledSet.
     *
     * @param set               The SampledSet to be negated.
     * @return                  The result of negating the SampledSet.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static SampledSet negate(SampledSet set) throws VisADException {

        SampledSet result;

        if (set.isMissing()) {
            result = set;
        } else {
            float[][] samples = set.getSamples(true);  // local copy

            for (int i = 0; i < samples.length; ++i) {
                float[] values = samples[i];

                for (int j = 0; j < values.length; ++j) {
                    values[j] = -values[j];
                }
            }

            result = (set instanceof GriddedSet)
                     ? (SampledSet) GriddedSet.create(
                         (SetType) set.getType(), samples,
                         ((GriddedSet) set).getLengths(),
                         set.getCoordinateSystem(), set.getSetUnits(),
                         set.getSetErrors())
                     : (SampledSet) new IrregularSet((SetType) set.getType(),
                     samples, set.getCoordinateSystem(), set.getSetUnits(),
                     set.getSetErrors());
        }

        return result;
    }

    /**
     * Inverts a data object by forming the reciprocal.
     *
     * @param data              The data object to be inverted.
     * @return                  The result of inverting the data object.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data invert(Data data)
            throws VisADException, RemoteException {

        /*
         * Don't use data.pow(-1) to invert the data object because that
         * causes range units to be set to null.
         */
        return (data instanceof SampledSet)
               ? invert((SampledSet) data)
               : one.divide(data);
    }

    /**
     * Inverts a SampledSet by forming the reciprocal.
     *
     * @param set               The SampledSet to be inverted.  It shall be 1-D.
     * @return                  The result of inverting the SampledSet.
     * @throws SetException     Set isn't 1-D.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static SampledSet invert(SampledSet set)
            throws SetException, VisADException {

        int rank = set.getDimension();

        if (rank != 1) {
            throw new SetException("Set not 1-D");
        }

        Unit      newUnit;
        float[][] samples;

        if (set.isMissing()) {
            samples = null;
            newUnit = null;
        } else {
            samples = set.getSamples(true);  // local copy

            for (int i = 0; i < samples.length; ++i) {
                float[] values = samples[i];

                for (int j = 0; j < values.length; ++j) {
                    values[j] = 1 / values[j];
                }
            }

            newUnit = set.getSetUnits()[0].pow(-1);
        }

        String newName =
            "VisADMath_Inverted_"
            + ((RealType) ((SetType) set.getType()).getDomain().getComponent(
                0)).getName();
        RealType newRealType = RealType.getRealTypeByName(newName);

        if (newRealType == null) {
            newRealType = RealType.getRealType(newName, newUnit, (Set) null);
        }

        return (set instanceof GriddedSet)
               ? (SampledSet) new Gridded1DSet(newRealType, samples,
                set.getLength(), (CoordinateSystem) null,
                new Unit[] { newUnit }, (ErrorEstimate[]) null)
               : (SampledSet) new Irregular1DSet(newRealType, samples,
                (CoordinateSystem) null, new Unit[] { newUnit },
                (ErrorEstimate[]) null);
    }

    /**
     * Exponentiates a data object.
     *
     * @param data              The data object to be exponentiated.
     * @return                  The result of exponentiating the data object.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    public static Data exp(Data data) throws VisADException, RemoteException {

        return (data instanceof SampledSet)
               ? exp((SampledSet) data)
               : data.exp();
    }

    /**
     * Exponentiates a SampledSet.
     *
     * @param set               The SampledSet to be exponentiated.
     * @return                  The result of exponentiating the SampledSet.
     *
     * @throws SetException
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static SampledSet exp(SampledSet set)
            throws SetException, VisADException {

        Unit[]    newUnits;
        float[][] samples;

        if (set.isMissing()) {
            samples  = null;
            newUnits = null;
        } else {
            samples = set.getSamples(true);  // local copy

            int rank = samples.length;

            newUnits = new Unit[rank];

            java.util.Arrays.fill(newUnits, CommonUnit.dimensionless);

            for (int i = 0; i < rank; ++i) {
                float[] values = samples[i];

                for (int j = 0; j < values.length; ++j) {
                    values[j] = (float) java.lang.Math.exp(values[j]);
                }
            }
        }

        return (set instanceof GriddedSet)
               ? (SampledSet) GriddedSet.create(RealType.Generic, samples,
                ((GriddedSet) set).getLengths(), (CoordinateSystem) null,
                newUnits, (ErrorEstimate[]) null)
               : (SampledSet) new IrregularSet(RealType.Generic, samples,
                (CoordinateSystem) null, newUnits, (ErrorEstimate[]) null);
    }

    /**
     * Takes the natural logarithm of a data object.
     *
     * @param data              The data object to have the natural logarithm
     *                          taken.
     * @return                  The result of taking the natural logarithm of
     *                          the data object.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    public static Data log(Data data) throws VisADException, RemoteException {

        return (data instanceof SampledSet)
               ? log((SampledSet) data)
               : data.log();
    }

    /**
     * Takes the natural logarithm of a SampledSet.
     *
     * @param set               The SampledSet to have the natural logarithm
     *                          taken.
     * @return                  The result of taking the natural logarithm of
     *                          the SampledSet.
     *
     * @throws SetException
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static SampledSet log(SampledSet set)
            throws SetException, VisADException {

        Unit[]    newUnits;
        float[][] samples;

        if (set.isMissing()) {
            samples  = null;
            newUnits = null;
        } else {
            samples = set.getSamples(true);  // local copy

            int rank = samples.length;

            newUnits = new Unit[rank];

            java.util.Arrays.fill(newUnits, CommonUnit.dimensionless);

            for (int i = 0; i < rank; ++i) {
                float[] values = samples[i];

                for (int j = 0; j < values.length; ++j) {
                    values[j] = (float) java.lang.Math.log(values[j]);
                }
            }
        }

        RealTupleType newRealTupleType =
            (RealTupleType) ((SetType) set.getType()).getDomain().unary(
                Data.LOG, new Vector());

        return (set instanceof GriddedSet)
               ? (SampledSet) GriddedSet.create(newRealTupleType, samples,
                ((GriddedSet) set).getLengths(), (CoordinateSystem) null,
                newUnits, (ErrorEstimate[]) null)
               : (SampledSet) new IrregularSet(newRealTupleType, samples,
                (CoordinateSystem) null, newUnits, (ErrorEstimate[]) null);
    }

    /**
     * Adds two data objects together.  The computational modes are {@link
     * Data#WEIGHTED_AVERAGE} and {@link Data#NO_ERRORS}.
     *
     * @param data1                     The first data object.
     * @param data2                     The second data object.
     * @return                          The result of adding the two data
     *                                  objects together.
     * @throws UnimplementedException   Operation not yet implemented.
     * @throws TypeException            MathTypes are incompatible.
     * @throws ArithmeticException      Invalid operation between types.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    public static Data add(Data data1, Data data2)
            throws UnimplementedException, TypeException,
                   ArithmeticException, VisADException, RemoteException {

        return (data1 instanceof SampledSet)
               ? add((SampledSet) data1, data2)
               : (data2 instanceof SampledSet)
                 ? add((SampledSet) data2, data1)
                 : data1.add(data2, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
    }

    /**
     * Adds a SampledSet and a data object.  The computation modes are {@link
     * Data#WEIGHTED_AVERAGE} and {@link Data#NO_ERRORS}.
     *
     * @param set                       The sampled set
     * @param data                      The other data object.
     * @return                          The result of adding the two data
     *                                  objects together.
     * @throws UnimplementedException   Operation not yet implemented.
     * @throws TypeException            MathTypes are incompatible.
     * @throws ArithmeticException      Invalid operation between types.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    static Data add(SampledSet set, Data data)
            throws UnimplementedException, TypeException,
                   ArithmeticException, VisADException, RemoteException {

        try {
            return (data instanceof Real)
                   ? (Data) add(set, (Real) data)
                   : (data instanceof SampledSet)
                     ? add(set, (SampledSet) data)
                     : (data instanceof Field)
                       ? (Data) newFlatField(
                           set,
                           DataUtility.simplify(
                               DataUtility.getDomainType(set)), null).add(
                                   data, Data.WEIGHTED_AVERAGE,
                                   Data.NO_ERRORS)
                       : set.add(data, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
        } catch (SetException e) {
            throw new ArithmeticException(e.getMessage());
        }
    }

    /**
     * Adds a {@link SampledSet} and a {@link Real}.  The {@link Real} will be
     * added to every component of the {@link SampledSet} that has compatible
     * units.
     *
     * @param set                   The SampledSet.
     * @param real                  The Real.
     * @return                      The result of adding the two data objects
     *                              together.
     * @throws VisADException       if a VisAD failure occurs. Throws a TypeException
     * if the {@link SampledSet}
     *                              no component to which the {@link Real}
     *                              could be added.
     */
    static SampledSet add(SampledSet set, Real real) throws VisADException {

        SetType       setType      = (SetType) set.getType();
        RealTupleType setTupleType = setType.getDomain();
        int           rank         = setTupleType.getDimension();
        RealType      realType     = (RealType) real.getType();
        int[]         indexes      = new int[setTupleType.getDimension()];
        int           count        = 0;

        for (int i = 0; i < indexes.length; i++) {
            if (realType.equalsExceptNameButUnits(
                    (RealType) setTupleType.getComponent(i))) {

                indexes[count++] = i;
            }
        }

        if (count == 0) {
            throw new TypeException("realType=" + realType
                                    + "; setTupleType=" + setTupleType);
        }

        float[][]       samples   = set.getSamples(true);  // local copy
        Unit[]          setUnits  = set.getSetUnits();
        ErrorEstimate[] newErrors = set.getSetErrors();

        for (int i = 0; i < count; i++) {
            int     realIndex   = indexes[i];
            float[] realSamples = samples[realIndex];
            float   realValue   = (float) real.getValue(setUnits[realIndex]);

            for (int j = 0; j < realSamples.length; ++j) {
                realSamples[j] += realValue;  // modify copy
            }

            ErrorEstimate realError = real.getError();

            if ((realError != null) && (newErrors[realIndex] != null)) {
                newErrors[realIndex] = new ErrorEstimate(realSamples,
                        setUnits[realIndex], Data.ADD, newErrors[realIndex],
                        realError, Data.INDEPENDENT);
            }
        }

        return (set instanceof GriddedSet)
               ? (SampledSet) GriddedSet.create(setType, samples,
                ((GriddedSet) set).getLengths(), set.getCoordinateSystem(),
                setUnits, newErrors)
               : (SampledSet) new IrregularSet(setType, samples,
                set.getCoordinateSystem(), setUnits, newErrors);
    }

    /**
     * Adds two SampledSet-s together.
     *
     * @param set1              The first SampledSet.
     * @param set2              The second SampledSet.
     * @return                  The result of adding the two data objects
     *                          together.
     * @throws SetException     The sets have different lengths.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static SampledSet add(SampledSet set1, SampledSet set2)
            throws SetException, VisADException {

        if (set1.getLength() != set2.getLength()) {
            throw new SetException("Sets have different lengths");
        }

        /*
         * Identify the components that are unique to each set or common to
         * both sets.
         */
        SetType       set1Type          = (SetType) set1.getType();
        RealTupleType set1TupleType     = set1Type.getDomain();
        SetType       set2Type          = (SetType) set2.getType();
        RealTupleType set2TupleType     = set2Type.getDomain();
        int           rank              = set1TupleType.getDimension();
        Vector        uniqueSet1Indexes = new Vector();
        Vector        uniqueSet2Indexes = new Vector();
        Vector        commonSet1Indexes = new Vector();
        Vector        commonSet2Indexes = new Vector();

        for (int set1Index = 0; set1Index < rank; ++set1Index) {
            int set2Index =
                set2TupleType.getIndex(set1TupleType.getComponent(set1Index));

            if (set2Index == -1) {
                uniqueSet1Indexes.add(new Integer(set1Index));
            } else {
                commonSet1Indexes.add(new Integer(set1Index));
                commonSet2Indexes.add(new Integer(set2Index));
            }
        }

        rank = set2TupleType.getDimension();

        for (int set2Index = 0; set2Index < rank; ++set2Index) {
            Integer set2Integer = new Integer(set2Index);

            if ( !commonSet2Indexes.contains(set2Integer)) {
                uniqueSet2Indexes.add(set2Integer);
            }
        }

        /*
         * Compute the parameters of the resulting set based on the three
         * categories of components identified above.
         */
        int newComponentCount = uniqueSet1Indexes.size()
                                + uniqueSet2Indexes.size()
                                + commonSet1Indexes.size();
        RealType[]      realTypes   = new RealType[newComponentCount];
        float[][]       set1Samples = set1.getSamples(false);
        float[][]       set2Samples = set2.getSamples(false);
        float[][]       newSamples  = new float[newComponentCount][];
        Unit[]          newUnits    = new Unit[newComponentCount];
        Unit[]          set1Units   = set1.getSetUnits();
        ErrorEstimate[] newErrors   = new ErrorEstimate[newComponentCount];
        ErrorEstimate[] set1Errors  = set1.getSetErrors();
        int             icomp       = 0;  // output component index

        for (Iterator iter = uniqueSet1Indexes.iterator(); iter.hasNext();
                icomp++) {

            /*
             * Compute parameters for component unique to first set.
             */
            int set1Index = ((Integer) iter.next()).intValue();

            realTypes[icomp] =
                (RealType) set1TupleType.getComponent(set1Index);
            newSamples[icomp] = set1Samples[set1Index];
            newUnits[icomp]   = set1Units[set1Index];
            newErrors[icomp]  = set1Errors[set1Index];
        }

        Unit[]          set2Units  = set2.getSetUnits();
        ErrorEstimate[] set2Errors = set2.getSetErrors();

        for (Iterator iter = uniqueSet2Indexes.iterator(); iter.hasNext();
                icomp++) {

            /*
             * Compute parameters for component unique to second set.
             */
            int set2Index = ((Integer) iter.next()).intValue();

            realTypes[icomp] =
                (RealType) set2TupleType.getComponent(set2Index);
            newSamples[icomp] = set2Samples[set2Index];
            newUnits[icomp]   = set2Units[set2Index];
            newErrors[icomp]  = set2Errors[set2Index];
        }

        Iterator iter1 = commonSet1Indexes.iterator();
        Iterator iter2 = commonSet2Indexes.iterator();

        for (; icomp < newComponentCount; icomp++) {

            /*
             * Compute parameters for component common to both sets.
             */
            int set1Index = ((Integer) iter1.next()).intValue();
            int set2Index = ((Integer) iter2.next()).intValue();

            realTypes[icomp] =
                (RealType) set1TupleType.getComponent(set1Index);
            newUnits[icomp] = set1Units[set1Index];

            float[] set1Values = set1Samples[set1Index];
            float[] set2Values =
                set1Units[set1Index].toThis(set2Samples[set2Index],
                                            set2Units[set2Index]);
            float[] newValues = new float[set1Values.length];

            for (int j = 0; j < newValues.length; ++j) {
                newValues[j] = set1Values[j] + set2Values[j];
            }

            newSamples[icomp] = newValues;

            ErrorEstimate set1Error = set1Errors[set1Index];
            ErrorEstimate set2Error = set2Errors[set2Index];

            newErrors[icomp] = ((set1Error == null) || (set2Error == null))
                               ? (ErrorEstimate) null
                               : new ErrorEstimate(newSamples[icomp],
                               newUnits[icomp], Data.ADD, set1Error,
                               set2Error, Data.INDEPENDENT);
        }

        /*
         * Create the resulting set and return it.
         */
        return new IrregularSet(new SetType(new RealTupleType(realTypes)),
                                newSamples,
                                ((uniqueSet1Indexes.size() == 0)
                                 && (uniqueSet2Indexes.size() == 0))
                                ? set1.getCoordinateSystem()
                                : (CoordinateSystem) null, newUnits,
                                newErrors);
    }

    /**
     * Subtracts one data object from another.
     *
     * @param data1                     The first data object.
     * @param data2                     The second data object.
     * @return                          The result of subtracting the second
     *                                  data object from the first data object.
     * @throws UnimplementedException   Operation not yet implemented.
     * @throws TypeException            MathTypes are incompatible.
     * @throws ArithmeticException      Invalid operation between types.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    public static Data subtract(Data data1, Data data2)
            throws UnimplementedException, TypeException,
                   ArithmeticException, VisADException, RemoteException {
        return add(data1, negate(data2));
    }

    /**
     * Multiplies two data objects together.  The computational modes are {@link
     * Data#WEIGHTED_AVERAGE} and {@link Data#NO_ERRORS}.
     *
     * @param data1                     The first data object.
     * @param data2                     The second data object.
     * @return                          The result of multiplying the first
     *                                  data object by the second data object.
     * @throws UnimplementedException   Operation not yet implemented.
     * @throws TypeException            MathTypes are incompatible.
     * @throws ArithmeticException      Invalid operation between types.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    public static Data multiply(Data data1, Data data2)
            throws UnimplementedException, TypeException,
                   ArithmeticException, VisADException, RemoteException {

        return (data1 instanceof SampledSet)
               ? multiply((SampledSet) data1, data2)
               : (data2 instanceof SampledSet)
                 ? multiply((SampledSet) data2, data1)
                 : data1.multiply(data2, Data.WEIGHTED_AVERAGE,
                                  Data.NO_ERRORS);
    }

    /**
     * Multiplies a SampledSet and a data object.
     *
     * @param set                       The sampled set
     * @param data2                     The other data object.
     * @return                          The result of multiplying the two data
     *                                  objects together.
     * @throws UnimplementedException   Operation not yet implemented.
     * @throws TypeException            MathTypes are incompatible.
     * @throws ArithmeticException      Invalid operation between types.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    static Data multiply(SampledSet set, Data data2)
            throws UnimplementedException, TypeException,
                   ArithmeticException, VisADException, RemoteException {

        try {
            return (data2 instanceof Real)
                   ? (Data) multiply(set, (Real) data2)
                   : (data2 instanceof Field)
                     ? (Data) newFlatField(
                         (SampledSet) ((Field) data2).getDomainSet(), set,
                         DataUtility.simplify(
                             DataUtility.getDomainType(set)), null).multiply(
                                 data2, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS)
                     : (Data) set.multiply(data2, Data.WEIGHTED_AVERAGE,
                                           Data.NO_ERRORS);
        } catch (SetException e) {
            throw new ArithmeticException(e.getMessage());
        }
    }

    /**
     * Multiplies a SampledSet and a Real.  The Real multiplies every component
     * of every sample point.
     *
     * @param set               The SampledSet.
     * @param real              The Real.
     * @return                  The result of multiplying the two data objects
     *                          together.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static SampledSet multiply(SampledSet set, Real real)
            throws VisADException {

        RealTupleType newRealTupleType =
            (RealTupleType) ((SetType) set.getType()).getDomain().binary(
                real.getType(), Data.MULTIPLY, new Vector());
        float[][]       samples;
        Unit[]          newUnits;
        ErrorEstimate[] newErrors;

        if (set.isMissing() || real.isMissing()) {
            samples   = null;
            newUnits  = null;
            newErrors = null;
        } else {
            samples = set.getSamples(true);  // local copy

            float           realValue = (float) real.getValue();
            Unit[]          setUnits  = set.getSetUnits();
            Unit            realUnit  = real.getUnit();
            ErrorEstimate[] setErrors = set.getSetErrors();
            ErrorEstimate   realError = real.getError();
            int             rank      = set.getDimension();

            newUnits  = new Unit[rank];
            newErrors = new ErrorEstimate[rank];

            for (int i = 0; i < rank; ++i) {
                float[] values = samples[i];

                for (int j = 0; j < values.length; ++j) {
                    values[j] *= realValue;
                }

                newUnits[i] = setUnits[i].multiply(realUnit);
                newErrors[i] = ((setErrors[i] == null) || (realError == null))
                               ? null
                               : new ErrorEstimate(values, newUnits[i],
                               Data.MULTIPLY, setErrors[i], realError,
                               Data.INDEPENDENT);
            }
        }

        return (set instanceof GriddedSet)
               ? (SampledSet) GriddedSet.create(newRealTupleType, samples,
                ((GriddedSet) set).getLengths(), (CoordinateSystem) null,
                newUnits, newErrors)
               : (SampledSet) new IrregularSet(newRealTupleType, samples,
                (CoordinateSystem) null, newUnits, newErrors);
    }

    /**
     * Divides one data object by another.
     *
     * @param data1                     The first data object.
     * @param data2                     The second data object.
     * @return                          The result of dividing the first
     *                                  data object by the second data object.
     * @throws UnimplementedException   Operation not yet implemented.
     * @throws TypeException            MathTypes are incompatible.
     * @throws ArithmeticException      Invalid operation between types.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    public static Data divide(Data data1, Data data2)
            throws UnimplementedException, TypeException,
                   ArithmeticException, VisADException, RemoteException {
        return multiply(data1, invert(data2));
    }

    /**
     * Raises one data object to the power of another.
     *
     * @param base                      The data object to be raised by a power.
     * @param exponent                  The exponent by which to raise the base.
     * @return                          The result of raising the base data
     *                                  object by the power of the exponent
     *                                  data object.
     * @throws UnimplementedException   Operation not yet implemented.
     * @throws TypeException            MathTypes are incompatible.
     * @throws ArithmeticException      Invalid operation between types.
     * @throws VisADException           Couldn't create necessary VisAD object.
     * @throws RemoteException          Java RMI failure.
     */
    public static Data pow(Data base, Data exponent)
            throws UnimplementedException, TypeException,
                   ArithmeticException, VisADException, RemoteException {

        Data promotedBase = (base instanceof SampledSet)
                            ? ((Data) ((exponent instanceof Field)
                                       ? newFlatField(
                                           (SampledSet) ((Field) exponent)
                                               .getDomainSet(), (SampledSet) base)
                                       : newFlatField((SampledSet) base)))
                            : base;
        Data promotedExponent = (exponent instanceof SampledSet)
                                ? ((Data) ((base instanceof Field)
                                           ? newFlatField((SampledSet) ((Field) base)
                                               .getDomainSet(), (SampledSet) exponent)
                                           : newFlatField(
                                               (SampledSet) exponent)))
                                : exponent;
        Data result = promotedBase.pow(promotedExponent);

        if ((base instanceof SampledSet) && !(exponent instanceof Field)) {

            /*
             * Demote the result from a FlatField to a SampledSet.
             */
            FlatField flatField = (FlatField) result;

            result =
                Util.newSampledSet(DataUtility.getRangeType(flatField),
                                   flatField.getFloats( /*copy=*/true),
                                   (int[]) null,
                                   flatField.getRangeCoordinateSystem()[0],
                                   flatField.getRangeUnits()[0],
                                   flatField.getRangeErrors(),
            /*copy=*/
            false);
        }

        return result;
    }

    /**
     * Transforms a data object from a reference CoordinateSystem.  When the
     * data object is a FlatField, then the transformation applies to its
     * range and not its domain.
     *
     * @param type              The VisAD MathType of the returned data object.
     *                          It shall be a RealTupleType, SetType, or
     *                          FunctionType and shall have a CoordinateSystem,
     *                          which shall be used in the transformation.
     * @param data              The data object to be transformed.  The object
     *                          shall be a Real, RealTuple, SampledSet, or
     *                          FlatField.
     * @return                  The transformed data object.  Its VisAD MathType
     *                          shall be <code>type</code>.
     * @throws TypeException    Data object has illegal type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data fromReference(MathType type, Data data)
            throws TypeException, VisADException, RemoteException {

        Data result;

        if (data instanceof Real) {
            result = fromReference((RealTupleType) type,
                                   new RealTuple(new Real[] { (Real) data }));
        } else if (data instanceof RealTuple) {
            result = fromReference((RealTupleType) type, (RealTuple) data);
        } else if (data instanceof SampledSet) {
            result = fromReference((SetType) type, (SampledSet) data);
        } else if (data instanceof FlatField) {
            result = fromReference((FunctionType) type, (FlatField) data);
        } else {
            throw new TypeException("Can't transform " + data.getClass());
        }

        return result;
    }

    /**
     * Transforms a RealTuple object from a reference CoordinateSystem.
     *
     * @param type              The MathType of the returned data object.  It
     *                          shall have a CoordinateSystem, which shall be
     *                          used in the transformation.
     * @param data              The RealTuple object to be transformed.
     * @return                  The transformed data object.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static RealTuple fromReference(RealTupleType type, RealTuple data)
            throws VisADException, RemoteException {

        CoordinateSystem cs             = type.getCoordinateSystem();
        Unit[]           referenceUnits = cs.getReferenceUnits();
        int              n              = data.getDimension();
        double[][]       coordinates    = new double[n][1];

        for (int i = n; --i >= 0; ) {
            coordinates[i][0] =
                ((Real) data.getComponent(i)).getValue(referenceUnits[i]);
        }

        coordinates = cs.fromReference(coordinates);
        coordinates = Unit.convertTuple(coordinates,
                                        cs.getCoordinateSystemUnits(),
                                        type.getDefaultUnits());
        n = cs.getDimension();

        double[] values = new double[n];

        for (int i = n; --i >= 0; ) {
            values[i] = coordinates[i][0];
        }

        return new RealTuple(type, values);
    }

    /**
     * Transforms a SampledSet object from a reference CoordinateSystem.
     *
     * @param type              The MathType of the returned data object.It
     *                          shall have a CoordinateSystem, which shall be
     *                          used in the transformation.
     * @param data              The SampledSet object to be transformed.
     * @return                  The transformed data object.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static IrregularSet fromReference(SetType type, SampledSet data)
            throws VisADException {

        CoordinateSystem cs = type.getDomain().getCoordinateSystem();

        return new IrregularSet(
            type,
            cs.fromReference(
                Unit.convertTuple(
                    data.getSamples(false), data.getSetUnits(),
                    cs.getReferenceUnits())), (CoordinateSystem) null,
                        cs.getCoordinateSystemUnits(),
                        (ErrorEstimate[]) null);
    }

    /**
     * Transforms a FlatField object from a reference CoordinateSystem.
     *
     * @param type              The MathType of the returned data object.It
     *                          shall have a CoordinateSystem, which shall be
     *                          used in the transformation.
     * @param data              The data object to be transformed.
     * @return                  The transformed data object.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static FlatField fromReference(FunctionType type, FlatField data)
            throws VisADException, RemoteException {

        CoordinateSystem cs = type.getFlatRange().getCoordinateSystem();
        FlatField result = new FlatField(type, data.getDomainSet(),
                                         (CoordinateSystem) null,
                                         (Set[]) null,
                                         cs.getCoordinateSystemUnits());

        result.setSamples(
            cs.fromReference(
                Unit.convertTuple(
                    data.getValues(false), Util.getRangeUnits(data),
                    cs.getReferenceUnits())));

        return result;
    }

    /**
     * Returns the indefinite curve integral of the gradient of a vector
     * function at the points of a SampledSet.  For each component, the value
     * of the curve integral shall be arbitrarily set to the zero at the first
     * valid point for that component.</p>
     *
     * <p>This method is only valid for a gradient of a function: if the
     * function being integrated is not a gradient of a function, then the value
     * at a point will depend upon the integration path to the point.  When
     * confronted with a path-dependent value for a point, this method forms
     * a weighted mean of the estimates.  The weight for an estimate is the
     * reciprocal of the estimated variance for the estimate.  The variance
     * for an estimate at a point is determined from the difference between
     * the linear and quadratic estimates for the point.  Estimates with zero
     * variance (i.e. infinite weight) are accumulated separately and averaged
     * to determine the final value -- regardless of any estimates with finite
     * variance.  If no such "infinite" estimates exist for a point, then the
     * weighted finite estimates determine the value for the point.</p>
     *
     * <p>The algorithm used by this method is necessarily quite general (it
     * uses, for example, the memory-intensive Set.getNeighbor(int[][]) method
     * -- which can return <code>getLength()*Math.pow(2, getDimension())</code>
     * integers).
     *
     * @param set               The sampled set.
     * @param gradients         Partial derivatives at the points of this set
     *                          for each component of the vector function.
     *                          <code>gradients[i][j][k]</code> is the partial
     *                          derivative of the <code>i</code>-th component
     *                          of the function in the <code>j</code>-th
     *                          dimension for the <code>k</code>-th point of
     *                          this set. <code>gradients[i].length</code>
     *                          shall equal <code>getDimension()</code>
     *                          for all <code>i</code> and
     *                          <code>gradients[i][j].length</code> shall equal
     *                          <code>getLength()</code> for all <code>i</code>
     *                          and <code>j</code>.
     * @param newRangeValues    Allocated space to hold the indefinite curve
     *                          integral. <code>newRangeValues.length</code>
     *                          shall equal <code>getDimension()</code>
     *                          and <code>newRangeValues[i].length</code>
     *                          shall equal <code>getLength()</code>
     *                          for all <code>i</code>. On return,
     *                          newRangeValues<code>[i][k]</code>
     *                          is the indefinite curve integral of
     *                          the <code>i</code>-th component of
     *                          <code>gradients</code> at the <code>k</code>-th
     *                          point in this set.
     * @return                  <code>newRangeValues</code>.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static double[][] curveIntegralOfGradient(SampledSet set,
            double[][][] gradients, double[][] newRangeValues)
            throws VisADException {

        int     componentCount   = gradients.length;
        int     domainDimension  = set.getDimension();
        int     sampleCount      = set.getLength();
        int[][] neighborsIndexes = new int[sampleCount][];

        set.getNeighbors(neighborsIndexes);

        /*
         * For each component of the function:
         */
        for (int icomp = 0; icomp < componentCount; ++icomp) {
            boolean    needInitialPoint = true;
            double[][] gradient         = gradients[icomp];
            double[]   compValues = newRangeValues[icomp];  // computed output

            java.util.Arrays.fill(compValues, Double.NaN);

            /*
             * For each sample point:
             */
            for (int sampleIndex = 0; sampleIndex < sampleCount;
                    ++sampleIndex) {

                /*
                 * Get the neighboring points of this sample point.
                 */
                int[] neighborIndexes = neighborsIndexes[sampleIndex];

                if (neighborIndexes != null) {
                    if (needInitialPoint) {
                        boolean isGoodInitialPoint = true;

                        /*
                         * For each dimension:
                         */
                        for (int dimIndex = 0; dimIndex < domainDimension;
                                ++dimIndex) {
                            if (Double.isNaN(
                                    gradient[dimIndex][sampleIndex])) {
                                isGoodInitialPoint = false;

                                break;
                            }
                        }

                        if (isGoodInitialPoint) {
                            compValues[sampleIndex] = 0;
                            needInitialPoint        = false;
                        }
                    } else {
                        double valueSum      = 0;
                        double weightSum     = 0;
                        double infinitySum   = 0;
                        int    infinityCount = 0;
                        double[][] sampleCoordinates =
                            set.indexToDouble(new int[] { sampleIndex });
                        double[][] neighborCoordinates =
                            set.indexToDouble(neighborIndexes);

                        /*
                         * For each neighboring point:
                         */
                        for (int i = 0; i < neighborIndexes.length; ++i) {
                            int neighborIndex = neighborIndexes[i];
                            double newRangeNeighborValue =
                                compValues[neighborIndex];

                            if ( !Double.isNaN(newRangeNeighborValue)) {

                                /*
                                 * The neighboring point already has an output
                                 * value.  Use the neighboring output value to
                                 * compute a value for the sample point based
                                 * on the value at the neighboring point and
                                 * the increment from the neighboring point
                                 * to the sample point.  The contribution of
                                 * a neighboring point is weighted by the
                                 * inverse square of the error estimate for that
                                 * neighbor.  The error estimate is computed
                                 * from the difference between the linear
                                 * estimate of the value at the sample point and
                                 * the quadratic estimate of the value at the
                                 * sample point.
                                 */
                                double delta1 = 0;

                                // linear difference estimate
                                double delta2 = 0;

                                // quadratic difference estimate

                                /*
                                 * For each dimension:
                                 */
                                for (int dimIndex = 0;
                                        dimIndex < domainDimension;
                                        ++dimIndex) {

                                    /*
                                     * Compute the contribution due to this
                                     * dimension's derivative and displacement.
                                     */
                                    double[] derivatives = gradient[dimIndex];
                                    double neighborDerivative =
                                        derivatives[neighborIndex];
                                    double deltaDomain =
                                        sampleCoordinates[dimIndex][0]
                                        - neighborCoordinates[dimIndex][i];

                                    delta1 += neighborDerivative
                                            * deltaDomain;
                                    delta2 +=
                                        (derivatives[sampleIndex]
                                         + neighborDerivative) * deltaDomain;
                                }             // dimension loop

                                delta2 /= 2;  // deferred mean computation

                                double error  = delta1 - delta2;
                                double weight = 1 / (error * error);

                                if (Double.isInfinite(weight)) {
                                    infinitySum += newRangeNeighborValue
                                            + delta2;

                                    infinityCount++;
                                } else if (infinityCount == 0) {
                                    valueSum += (newRangeNeighborValue
                                            + delta2) * weight;
                                    weightSum += weight;
                                }
                            }  // usable neighbor
                        }      // neighbor loop

                        /*
                         * The value at the output sample is the weighted mean
                         * of the values computed from the neighboring points.
                         */
                        if (infinityCount != 0) {
                            compValues[sampleIndex] = (double) (infinitySum
                                    / infinityCount);
                        } else if (weightSum != 0) {
                            compValues[sampleIndex] = (double) (valueSum
                                    / weightSum);
                        }
                    }  // have initial value
                }      // sample point has neighbors
            }          // sample point loop
        }              // component loop

        return newRangeValues;
    }

    /**
     * Returns the indefinite curve integral of the gradient of a vector
     * function at the points of a SampledSet.  For each component, the value
     * of the curve integral shall be arbitrarily set to the zero at the first
     * valid point for that component.</p>
     *
     * <p>This method is only valid for a gradient of a function: if the
     * function being integrated is not a gradient of a function, then the value
     * at a point will depend upon the integration path to the point.  When
     * confronted with a path-dependent value for a point, this method forms
     * a weighted mean of the estimates.  The weight for an estimate is the
     * reciprocal of the estimated variance for the estimate.  The variance
     * for an estimate at a point is determined from the difference between
     * the linear and quadratic estimates for the point.  Estimates with zero
     * variance (i.e. infinite weight) are accumulated separately and averaged
     * to determine the final value -- regardless of any estimates with finite
     * variance.  If no such "infinite" estimates exist for a point, then the
     * weighted finite estimates determine the value for the point.</p>
     *
     * <p>The algorithm used by this method is necessarily quite general (it
     * uses, for example, the memory-intensive Set.getNeighbor(int[][]) method
     * -- which can return <code>getLength()*Math.pow(2, getDimension())</code>
     * integers).
     *
     * @param set               The sampled set.
     * @param gradients         Partial derivatives at the points of this set
     *                          for each component of the vector function.
     *                          <code>gradients[i][j][k]</code> is the partial
     *                          derivative of the <code>i</code>-th component
     *                          of the function in the <code>j</code>-th
     *                          dimension for the <code>k</code>-th point of
     *                          this set. <code>gradients[i].length</code>
     *                          shall equal <code>getDimension()</code>
     *                          for all <code>i</code> and
     *                          <code>gradients[i][j].length</code> shall equal
     *                          <code>getLength()</code> for all <code>i</code>
     *                          and <code>j</code>.
     * @param newRangeValues    Allocated space to hold the indefinite curve
     *                          integral. <code>newRangeValues.length</code>
     *                          shall equal <code>getDimension()</code>
     *                          and <code>newRangeValues[i].length</code>
     *                          shall equal <code>getLength()</code>
     *                          for all <code>i</code>. On return,
     *                          newRangeValues<code>[i][k]</code>
     *                          is the indefinite curve integral of
     *                          the <code>i</code>-th component of
     *                          <code>gradients</code> at the <code>k</code>-th
     *                          point in this set.
     * @return                  <code>newRangeValues</code>.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static float[][] curveIntegralOfGradient(SampledSet set,
                                             float[][][] gradients,
                                             float[][] newRangeValues)
            throws VisADException {

        int     componentCount   = gradients.length;
        int     domainDimension  = set.getDimension();
        int     sampleCount      = set.getLength();
        int[][] neighborsIndexes = new int[sampleCount][];

        set.getNeighbors(neighborsIndexes);

        /*
         * For each component of the function:
         */
        for (int icomp = 0; icomp < componentCount; ++icomp) {
            boolean   needInitialPoint = true;
            float[][] gradient         = gradients[icomp];
            float[]   compValues = newRangeValues[icomp];  // computed output

            java.util.Arrays.fill(compValues, Float.NaN);

            /*
             * For each sample point:
             */
            for (int sampleIndex = 0; sampleIndex < sampleCount;
                    ++sampleIndex) {

                /*
                 * Get the neighboring points of this sample point.
                 */
                int[] neighborIndexes = neighborsIndexes[sampleIndex];

                if (neighborIndexes != null) {
                    if (needInitialPoint) {
                        boolean isGoodInitialPoint = true;

                        /*
                         * For each dimension:
                         */
                        for (int dimIndex = 0; dimIndex < domainDimension;
                                ++dimIndex) {
                            if (Double.isNaN(
                                    gradient[dimIndex][sampleIndex])) {
                                isGoodInitialPoint = false;

                                break;
                            }
                        }

                        if (isGoodInitialPoint) {
                            compValues[sampleIndex] = 0;
                            needInitialPoint        = false;
                        }
                    } else {
                        double valueSum      = 0;
                        double weightSum     = 0;
                        double infinitySum   = 0;
                        int    infinityCount = 0;
                        float[][] sampleCoordinates =
                            set.indexToValue(new int[] { sampleIndex });
                        float[][] neighborCoordinates =
                            set.indexToValue(neighborIndexes);

                        /*
                         * For each neighboring point:
                         */
                        for (int i = 0; i < neighborIndexes.length; ++i) {
                            int neighborIndex = neighborIndexes[i];
                            double newRangeNeighborValue =
                                compValues[neighborIndex];

                            if ( !Double.isNaN(newRangeNeighborValue)) {

                                /*
                                 * The neighboring point already has an output
                                 * value.  Use the neighboring output value to
                                 * compute a value for the sample point based
                                 * on the value at the neighboring point and
                                 * the increment from the neighboring point
                                 * to the sample point.  The contribution of
                                 * a neighboring point is weighted by the
                                 * inverse square of the error estimate for that
                                 * neighbor.  The error estimate is computed
                                 * from the difference between the linear
                                 * estimate of the value at the sample point and
                                 * the quadratic estimate of the value at the
                                 * sample point.
                                 */
                                double delta1 = 0;

                                // linear difference estimate
                                double delta2 = 0;

                                // quadratic difference estimate

                                /*
                                 * For each dimension:
                                 */
                                for (int dimIndex = 0;
                                        dimIndex < domainDimension;
                                        ++dimIndex) {

                                    /*
                                     * Compute the contribution due to this
                                     * dimension's derivative and displacement.
                                     */
                                    float[] derivatives = gradient[dimIndex];
                                    double neighborDerivative =
                                        derivatives[neighborIndex];
                                    double deltaDomain =
                                        sampleCoordinates[dimIndex][0]
                                        - neighborCoordinates[dimIndex][i];

                                    delta1 += neighborDerivative
                                            * deltaDomain;
                                    delta2 +=
                                        (derivatives[sampleIndex]
                                         + neighborDerivative) * deltaDomain;
                                }             // dimension loop

                                delta2 /= 2;  // deferred mean computation

                                double error  = delta1 - delta2;
                                double weight = 1 / (error * error);

                                if (Double.isInfinite(weight)) {
                                    infinitySum += newRangeNeighborValue
                                            + delta2;

                                    infinityCount++;
                                } else if (infinityCount == 0) {
                                    valueSum += (newRangeNeighborValue
                                            + delta2) * weight;
                                    weightSum += weight;
                                }
                            }  // usable neighbor
                        }      // neighbor loop

                        /*
                         * The value at the output sample is the weighted mean
                         * of the values computed from the neighboring points.
                         */
                        if (infinityCount != 0) {
                            compValues[sampleIndex] = (float) (infinitySum
                                    / infinityCount);
                        } else if (weightSum != 0) {
                            compValues[sampleIndex] = (float) (valueSum
                                    / weightSum);
                        }
                    }  // have initial value
                }      // sample point has neighbors
            }          // sample point loop
        }              // component loop

        return newRangeValues;
    }

    /**
     * Provides support for indexing sample points.
     */
    static final protected class Index {

        /** total number of points */
        private final int totalCount;

        /** lengths of each dimension */
        private final int[] lengths;

        /** jelly roll */
        private final int[] roll;

        /** index into points */
        private int index = 0;

        /** set of indexes */
        private int[] indexes;

        /**
         * Constructs from a {@link GriddedSet}.
         *
         * @param set             The {@link GriddedSet}.
         * @throws VisADException if a core VisAD failure occurs.
         */
        protected Index(GriddedSet set) throws VisADException {

            lengths    = set.getLengths();
            totalCount = set.getLength();
            roll       = new int[lengths.length];

            if (lengths.length > 0) {
                roll[0] = 1;
            }

            for (int idim = 1; idim < lengths.length; ++idim) {
                roll[idim] = roll[idim - 1] * lengths[idim - 1];
            }

            indexes = new int[lengths.length];

            java.util.Arrays.fill(indexes, 0);
        }

        /**
         * Returns the current, cumulative index.
         *
         * @return          The current, cumulative index.
         */
        protected int getIndex() {
            return index;
        }

        /**
         * Returns the previous dimensional index.
         *
         * @param idim      Which dimensional index to return.
         * @return          The previous index.
         */
        protected int getPreviousIndex(int idim) {

            return (indexes[idim] <= 0)
                   ? -1
                   : index - roll[idim];
        }

        /**
         * Indicates if there are more elements.
         *
         * @return          True if and only if there are more points.
         */
        protected boolean hasPoint() {
            return index < totalCount;
        }

        /**
         * Advances to the next element.
         */
        protected void increment() {

            if (hasPoint()) {
                index++;

                for (int idim = 0;
                        (idim < lengths.length)
                        && (++indexes[idim] >= lengths[idim]); ) {
                    indexes[idim++] = 0;
                }
            }
        }
    }

    /**
     * Returns the indefinite curve integral of the gradient of a vector
     * function at the points of a GriddedSet. same as this Set's.  For each
     * component, the value of the curve integral shall be arbitrarily set to
     * zero at the first valid point for that component.</p>
     *
     * <p>This method is only valid for a gradient of a function: if the
     * function being integrated is not a gradient of a function, then the value
     * at a point will depend upon the integration path to the point.  When
     * confronted with a path-dependent value for a point, this method forms
     * a weighted mean of the estimates.  The weight for an estimate is the
     * reciprocal of the estimated variance for the estimate.  The variance
     * for an estimate at a point is determined from the difference between
     * the linear and quadratic estimates for the point.  Estimates with zero
     * variance (i.e. infinite weight) are accumulated separately and averaged
     * to determine the final value -- regardless of any estimates with finite
     * variance.  If no such "infinite" estimates exist for a point, then the
     * weighted finite estimates determine the value for the point.</p>
     *
     * <p>This method takes approximately 830/845 the time of
     * curveIntegralOfGradient(SampledSet, double[][][], double[][]) -- which
     * is not much of a gain until you consider that it doesn't use the
     * memory-intensive Set.getNeighbor(int[][]) method (which can return
     * <code>getLength()*Math.pow(2, getDimension())</code> integers).
     *
     * @param set               The GriddedSet.
     * @param gradients         Partial derivatives at the points of this set
     *                          for each component of the vector function.
     *                          <code>gradients[i][j][k]</code> is the partial
     *                          derivative of the <code>i</code>-th component
     *                          of the function in the <code>j</code>-th
     *                          dimension for the <code>k</code>-th point of
     *                          this set. <code>gradients[i].length</code>
     *                          shall equal <code>getDimension()</code>
     *                          for all <code>i</code> and
     *                          <code>gradients[i][j].length</code> shall equal
     *                          <code>getLength()</code> for all <code>i</code>
     *                            and <code>j</code>.
     * @param newRangeValues    Allocated space to hold the indefinite curve
     *                          integral. <code>newRangeValues.length</code>
     *                          shall equal <code>getDimension()</code>
     *                          and <code>newRangeValues[i].length</code>
     *                          shall equal <code>getLength()</code>
     *                          for all <code>i</code>. On return,
     *                          newRangeValues<code>[i][k]</code>
     *                          is the indefinite curve integral of
     *                          the <code>i</code>-th component of
     *                          <code>gradients</code> at the <code>k</code>-th
     *                          point in this set.
     * @return                  <code>newRangeValues</code>.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static double[][] curveIntegralOfGradient(GriddedSet set,
            double[][][] gradients, double[][] newRangeValues)
            throws VisADException {

        int       componentCount     = gradients.length;
        int       domainDimension    = set.getDimension();
        int       sampleCount        = set.getLength();
        int[]     infinityCounts     = new int[componentCount];
        double[]  infinitySums       = new double[componentCount];
        double[]  weightSums         = new double[componentCount];
        double[]  valueSums          = new double[componentCount];
        boolean[] needInitialPoint   = new boolean[componentCount];
        boolean[] isGoodInitialPoint = new boolean[componentCount];

        java.util.Arrays.fill(needInitialPoint, true);
        java.util.Arrays.fill(isGoodInitialPoint, true);

        /*
         * For each sample point:
         */
        for (Index index = new Index(set); index.hasPoint();
                index.increment()) {
            int sampleIndex = index.getIndex();
            double[][] sampleCoordinates = set.indexToDouble(new int[] {
                                               sampleIndex });

            java.util.Arrays.fill(valueSums, 0);
            java.util.Arrays.fill(weightSums, 0);
            java.util.Arrays.fill(infinitySums, 0);
            java.util.Arrays.fill(infinityCounts, 0);

            /*
             * For each neighboring point;
             */
            for (int idim = 0; idim < domainDimension; ++idim) {
                int neighborIndex = index.getPreviousIndex(idim);

                if (neighborIndex >= 0) {
                    double[][] neighborCoordinates =
                        set.indexToDouble(new int[] { neighborIndex });
                    double deltaDomain = sampleCoordinates[idim][0]
                                         - neighborCoordinates[idim][0];

                    /*
                     * For each component of the function:
                     */
                    for (int icomp = 0; icomp < componentCount; ++icomp) {
                        if (needInitialPoint[icomp]) {
                            if (isGoodInitialPoint[icomp]) {
                                if (Double.isNaN(
                                        gradients[icomp][idim][sampleIndex])) {
                                    isGoodInitialPoint[icomp] = false;
                                }
                            }
                        } else {

                            /*
                             * Compute the contribution due to this neighboring
                             * point.  Weight the contribution by the reciprocal
                             * of the estimated variance.  Estimate the variance
                             * from the difference between the linear and
                             * quadratic estimates.
                             */
                            double[] derivatives = gradients[icomp][idim];
                            double neighborDerivative =
                                derivatives[neighborIndex];
                            double delta1 = deltaDomain * neighborDerivative;
                            double delta2 = deltaDomain
                                            * (derivatives[sampleIndex]
                                               + neighborDerivative) / 2;
                            double error  = delta1 - delta2;
                            double weight = 1 / (error * error);

                            if (Double.isInfinite(weight)) {
                                infinitySums[icomp] +=
                                    newRangeValues[icomp][neighborIndex]
                                    + delta2;

                                infinityCounts[icomp]++;
                            } else if (infinityCounts[icomp] == 0) {
                                valueSums[icomp] +=
                                    (newRangeValues[icomp][neighborIndex]
                                     + delta2) * weight;
                                weightSums[icomp] += weight;
                            }
                        }  // don't need initial value
                    }      // component loop
                }          // neighboring point exists
            }              // neghboring point loop

            /*
             * For each component:
             */
            for (int icomp = 0; icomp < componentCount; ++icomp) {
                if (needInitialPoint[icomp]) {
                    if ( !isGoodInitialPoint[icomp]) {
                        newRangeValues[icomp][sampleIndex] = Double.NaN;
                    } else {
                        newRangeValues[icomp][sampleIndex] = 0;
                        needInitialPoint[icomp]            = false;
                    }
                } else {
                    newRangeValues[icomp][sampleIndex] =
                        (infinityCounts[icomp] != 0)
                        ? (double) (infinitySums[icomp]
                                    / infinityCounts[icomp])
                        : (weightSums[icomp] != 0)
                          ? (double) (valueSums[icomp] / weightSums[icomp])
                          : Double.NaN;
                }
            }
        }  // output sample-point loop

        return newRangeValues;
    }

    /**
     * Returns the indefinite curve integral of the gradient of a vector
     * function at the points of a GriddedSet. same as this Set's.  For each
     * component, the value of the curve integral shall be arbitrarily set to
     * zero at the first valid point for that component.</p>
     *
     * <p>This method is only valid for a gradient of a function: if the
     * function being integrated is not a gradient of a function, then the value
     * at a point will depend upon the integration path to the point.  When
     * confronted with a path-dependent value for a point, this method forms
     * a weighted mean of the estimates.  The weight for an estimate is the
     * reciprocal of the estimated variance for the estimate.  The variance
     * for an estimate at a point is determined from the difference between
     * the linear and quadratic estimates for the point.  Estimates with zero
     * variance (i.e. infinite weight) are accumulated separately and averaged
     * to determine the final value -- regardless of any estimates with finite
     * variance.  If no such "infinite" estimates exist for a point, then the
     * weighted finite estimates determine the value for the point.</p>
     *
     * <p>This method takes approximately 830/845 the time of
     * curveIntegralOfGradient(SampledSet, float[][][], float[][]) -- which
     * is not much of a gain until you consider that it doesn't use the
     * memory-intensive Set.getNeighbor(int[][]) method (which can return
     * <code>getLength()*Math.pow(2, getDimension())</code> integers).
     *
     * @param set               The GriddedSet.
     * @param gradients         Partial derivatives at the points of this set
     *                          for each component of the vector function.
     *                          <code>gradients[i][j][k]</code> is the partial
     *                          derivative of the <code>i</code>-th component
     *                          of the function in the <code>j</code>-th
     *                          dimension for the <code>k</code>-th point of
     *                          this set. <code>gradients[i].length</code>
     *                          shall equal <code>getDimension()</code>
     *                          for all <code>i</code> and
     *                          <code>gradients[i][j].length</code> shall equal
     *                          <code>getLength()</code> for all <code>i</code>
     *                            and <code>j</code>.
     * @param newRangeValues    Allocated space to hold the indefinite curve
     *                          integral. <code>newRangeValues.length</code>
     *                          shall equal <code>getDimension()</code>
     *                          and <code>newRangeValues[i].length</code>
     *                          shall equal <code>getLength()</code>
     *                          for all <code>i</code>. On return,
     *                          newRangeValues<code>[i][k]</code>
     *                          is the indefinite curve integral of
     *                          the <code>i</code>-th component of
     *                          <code>gradients</code> at the <code>k</code>-th
     *                          point in this set.
     * @return                  <code>newRangeValues</code>.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    static float[][] curveIntegralOfGradient(GriddedSet set,
                                             float[][][] gradients,
                                             float[][] newRangeValues)
            throws VisADException {

        int       componentCount     = gradients.length;
        int       domainDimension    = set.getDimension();
        int       sampleCount        = set.getLength();
        int[]     infinityCounts     = new int[componentCount];
        double[]  infinitySums       = new double[componentCount];
        double[]  weightSums         = new double[componentCount];
        double[]  valueSums          = new double[componentCount];
        boolean[] needInitialPoint   = new boolean[componentCount];
        boolean[] isGoodInitialPoint = new boolean[componentCount];

        java.util.Arrays.fill(needInitialPoint, true);
        java.util.Arrays.fill(isGoodInitialPoint, true);

        /*
         * For each sample point:
         */
        for (Index index = new Index(set); index.hasPoint();
                index.increment()) {
            int sampleIndex = index.getIndex();
            float[][] sampleCoordinates = set.indexToValue(new int[] {
                                              sampleIndex });

            java.util.Arrays.fill(valueSums, 0);
            java.util.Arrays.fill(weightSums, 0);
            java.util.Arrays.fill(infinitySums, 0);
            java.util.Arrays.fill(infinityCounts, 0);

            /*
             * For each neighboring point;
             */
            for (int idim = 0; idim < domainDimension; ++idim) {
                int neighborIndex = index.getPreviousIndex(idim);

                if (neighborIndex >= 0) {
                    float[][] neighborCoordinates =
                        set.indexToValue(new int[] { neighborIndex });
                    float deltaDomain = sampleCoordinates[idim][0]
                                        - neighborCoordinates[idim][0];

                    /*
                     * For each component of the function:
                     */
                    for (int icomp = 0; icomp < componentCount; ++icomp) {
                        if (needInitialPoint[icomp]) {
                            if (isGoodInitialPoint[icomp]) {
                                if (Double.isNaN(
                                        gradients[icomp][idim][sampleIndex])) {
                                    isGoodInitialPoint[icomp] = false;
                                }
                            }
                        } else {

                            /*
                             * Compute the contribution due to this neighboring
                             * point.  Weight the contribution by the reciprocal
                             * of the estimated variance.  Estimate the variance
                             * from the difference between the linear and
                             * quadratic estimates.
                             */
                            float[] derivatives = gradients[icomp][idim];
                            double neighborDerivative =
                                derivatives[neighborIndex];
                            double delta1 = deltaDomain * neighborDerivative;
                            double delta2 = deltaDomain
                                            * (derivatives[sampleIndex]
                                               + neighborDerivative) / 2;
                            double error  = delta1 - delta2;
                            double weight = 1 / (error * error);

                            if (Double.isInfinite(weight)) {
                                infinitySums[icomp] +=
                                    newRangeValues[icomp][neighborIndex]
                                    + delta2;

                                infinityCounts[icomp]++;
                            } else if (infinityCounts[icomp] == 0) {
                                valueSums[icomp] +=
                                    (newRangeValues[icomp][neighborIndex]
                                     + delta2) * weight;
                                weightSums[icomp] += weight;
                            }
                        }  // don't need initial value
                    }      // component loop
                }          // neighboring point exists
            }              // neghboring point loop

            /*
             * For each component:
             */
            for (int icomp = 0; icomp < componentCount; ++icomp) {
                if (needInitialPoint[icomp]) {
                    if ( !isGoodInitialPoint[icomp]) {
                        newRangeValues[icomp][sampleIndex] = Float.NaN;
                    } else {
                        newRangeValues[icomp][sampleIndex] = 0;
                        needInitialPoint[icomp]            = false;
                    }
                } else {
                    newRangeValues[icomp][sampleIndex] =
                        (infinityCounts[icomp] != 0)
                        ? (float) (infinitySums[icomp]
                                   / infinityCounts[icomp])
                        : (weightSums[icomp] != 0)
                          ? (float) (valueSums[icomp] / weightSums[icomp])
                          : Float.NaN;
                }
            }
        }  // output sample-point loop

        return newRangeValues;
    }

    /**
     * Returns the indefinite curve integral of a Field, which is assumed
     * to be the gradient of a function.  This method can handle any Field
     * whose range is one of the following:
     * <pre>
     *
     *   Scalar derivative       dy/dx
     *
     *   RealTuple of partial
     *   derivatives             (du/dx, du/dy, ...)
     *
     *   Tuple of RealTuple-s
     *   of partial derivatives
     *   (one set of partial
     *   derivatives for each
     *   component of the
     *   resulting FlatField)    ((du/dx, du/dy, ...), (dv/dx, dv/dy, ...), ...)
     * </pre></p>
     *
     * </p>The domain points of the returned FlatField will be the same as the
     * Field's.  The type of the range of the returned FlatField will depend
     * on the type of the range of the Field.  The value of the range at
     * the first domain point of the returned FlatField shall be arbitrarily set
     * to (possibly vector) zero.
     *
     * @param field             The Field whose indefinite curve integral
     *                          is to be computed.
     * @return                  The indefinite curve integral of the Field.
     * @throws FieldException   The Field has a non-flat range or range
     *                          dimension of Field != domain dimension of Field.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static FlatField curveIntegralOfGradient(Field field)
            throws FieldException, VisADException, RemoteException {

        /*
         * Create the resulting FlatField with missing data.
         */
        FunctionType oldFieldType = (FunctionType) field.getType();

        if ( !oldFieldType.getFlat()) {
            throw new FieldException(
                "VisADMath.curveIntegralOfGradient(Field): Non-flat range");
        }

        Unit[] integrandUnits =
            new Unit[oldFieldType.getFlatRange().getDimension()];
        TupleType newFieldRangeType;

        {
            MathType rangeType = oldFieldType.getRange();

            if (rangeType instanceof ScalarType) {
                rangeType = new RealTupleType((RealType) rangeType);
            }

            if (rangeType instanceof RealTupleType) {
                rangeType = new TupleType(new MathType[] { rangeType });
            }

            newFieldRangeType = (TupleType) rangeType;
        }

        int           newComponentCount = newFieldRangeType.getDimension();
        int           domainDimension   = field.getDomainDimension();
        Vector        nilVector         = new Vector(0);
        RealTupleType domainType        = oldFieldType.getDomain();
        RealType[]    newComponentTypes = new RealType[newComponentCount];
        int           flatOffset        = 0;
        SampledSet    domainSet         = (SampledSet) field.getDomainSet();
        Unit[]        domainUnits       = domainSet.getSetUnits();

        for (int newComponentIndex = 0; newComponentIndex < newComponentCount;
                ++newComponentIndex) {
            RealTupleType gradientType =
                (RealTupleType) newFieldRangeType.getComponent(
                    newComponentIndex);
            int partialCount = gradientType.getDimension();

            if (partialCount != domainDimension) {
                throw new FieldException(
                    "VisADMath.curveIntegralOfGradient(): "
                    + "Number of partial derivatives for component "
                    + newComponentIndex + " (" + partialCount + ')'
                    + " != domain dimension (" + domainDimension + ')');
            }

            if (partialCount > 0) {
                RealType newComponentType =
                    (RealType) gradientType.getComponent(0).binary(
                        domainType.getComponent(0), Data.MULTIPLY, nilVector);

                for (int i = 1; i < partialCount; ++i) {
                    newComponentType = (RealType) newComponentType.binary(
                        gradientType.getComponent(i).binary(
                            domainType.getComponent(i), Data.MULTIPLY,
                            nilVector), Data.ADD, nilVector);
                }

                newComponentTypes[newComponentIndex] = newComponentType;

                Unit outUnit = newComponentType.getDefaultUnit();

                for (int i = 0; i < partialCount; ++i) {
                    integrandUnits[flatOffset + i] =
                        outUnit.divide(domainUnits[i]);
                }
            }

            flatOffset += partialCount;
        }

        FlatField newField;
        boolean   useDouble = false;
        if (field instanceof FlatField) {
            Set[] rangeSets = ((FlatField) field).getRangeSets();
            for (int i = 0; i < rangeSets.length; i++) {
                if ( !(rangeSets[i] instanceof DoubleSet)) {
                    rangeSets[i] = new FloatSet(newComponentTypes[i],
                            (CoordinateSystem) null, (Unit[]) null);
                } else {
                    useDouble = true;
                    rangeSets[i] = new DoubleSet(newComponentTypes[i],
                            (CoordinateSystem) null, (Unit[]) null);
                }
            }
            newField = new FlatField(
                new FunctionType(
                    domainType,
                    DataUtility.simplify(
                        new RealTupleType(newComponentTypes))), domainSet,
                            (CoordinateSystem) null, rangeSets,
                            (Unit[]) null);
        } else {
            newField = new FlatField(
                new FunctionType(
                    domainType,
                    DataUtility.simplify(
                        new RealTupleType(newComponentTypes))), domainSet);
        }

        if (useDouble) {
            double[][] integrandValues =
                Unit.convertTuple(field.getValues(false),
                                  field.getDefaultRangeUnits(),
                                  integrandUnits);

            /*
             * Set the data.
             */
            if (domainSet instanceof GriddedSet) {
                newField.setSamples(
                    curveIntegralOfGradient(
                        (GriddedSet) domainSet, new double[][][] {
                    integrandValues
                }, new double[newComponentCount][domainSet.getLength()]), false);
            } else {
                newField.setSamples(curveIntegralOfGradient(domainSet,
                        new double[][][] {
                    integrandValues
                }, new double[newComponentCount][domainSet.getLength()]), false);
            }
        } else {
            float[][] integrandValues =
                Unit.convertTuple(field.getFloats(false),
                                  field.getDefaultRangeUnits(),
                                  integrandUnits);

            /*
             * Set the data.
             */
            if (domainSet instanceof GriddedSet) {
                newField.setSamples(
                    curveIntegralOfGradient(
                        (GriddedSet) domainSet, new float[][][] {
                    integrandValues
                }, new float[newComponentCount][domainSet.getLength()]), false);
            } else {
                newField.setSamples(curveIntegralOfGradient(domainSet,
                        new float[][][] {
                    integrandValues
                }, new float[newComponentCount][domainSet.getLength()]), false);
            }
        }

        return newField;
    }

    /**
     * Creates a FlatField from a SampledSet.  This method should, perhaps,
     * become a method in visad.Set or a constructor in FlatField.
     *
     * @param set               The SampledSet.
     * @return                  The SampledSet promoted to a FlatField.
     *                          The MathType of the domain and range will
     *                          be the RealTupleType of the set (i.e.
     *                          <code>((SetType)set.getType()).getDomain()</code
     *                          >. The CoordinateSystem of the
     *                          range will be that of the set (i.e.
     *                          <code>set.getCoordinateSystem()</code>).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static FlatField newFlatField(SampledSet set)
            throws VisADException, RemoteException {
        return newFlatField(set, set);
    }

    /**
     * Creates a FlatField from two SampledSet-s.  This method should, perhaps,
     * become a method in visad.Set or a constructor in FlatField.
     *
     * @param domain            The domain of the FlatField.
     * @param range             The range of the FlatField.
     * @return                  The SampledSet promoted to a FlatField.
     *                          The MathType of the domain and range will
     *                          be the RealTupleType of the set (i.e.
     *                          <code>((SetType)set.getType()).getDomain()</code
     *                          >. The CoordinateSystem of the
     *                          range will be that of the set (i.e.
     *                          <code>set.getCoordinateSystem()</code>).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static FlatField newFlatField(SampledSet domain, SampledSet range)
            throws VisADException, RemoteException {

        return newFlatField(domain, range,
                            ((SetType) range.getType()).getDomain(),
                            range.getCoordinateSystem());
    }

    /**
     * Creates a FlatField from a SampledSet, a MathType for the range, and a
     * CoordinateSystem for the range.  This method should, perhaps, become a
     * method in visad.Set or a constructor in FlatField.
     *
     * @param set               The SampledSet.
     * @param rangeType         The MathType of the range for the returned
     *                          FlatField.
     * @param rangeCoordinateSystem
     *                          The CoordinateSystem for the range of the
     *                          returned FlatField.  May be <code>null</code>.
     * @return                  The SampledSet promoted to a FlatField.
     *                          The MathType of the domain will be
     *                          the RealTupleType of the set (i.e.
     *                          <code>((SetType)set.getType()).getDomain()</code
     *                          >.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static FlatField newFlatField(
            SampledSet set, MathType rangeType,
            CoordinateSystem rangeCoordinateSystem)
            throws VisADException, RemoteException {
        return newFlatField(set, set, rangeType, rangeCoordinateSystem);
    }

    /**
     * Creates a FlatField from a domain SampledSet, a range SampledSet, and a
     * CoordinateSystem for the range.  This method should, perhaps, become a
     * method in visad.Set or a constructor in FlatField.
     *
     * @param domain            The domain SampledSet.
     * @param range             The range SampledSet.
     * @param rangeType         The MathType of the range for the returned
     *                          FlatField.
     * @param rangeCoordinateSystem
     *                          The CoordinateSystem for the range of the
     *                          returned FlatField.  May be <code>null</code>.
     * @return                  The SampledSet promoted to a FlatField.
     *                          The MathType of the domain will be
     *                          the RealTupleType of the set (i.e.
     *                          <code>((SetType)set.getType()).getDomain()</code
     *                          >.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static FlatField newFlatField(
            SampledSet domain, SampledSet range, MathType rangeType,
            CoordinateSystem rangeCoordinateSystem)
            throws VisADException, RemoteException {

        /*
         * When appropriate, ensure that the domain of the resulting FlatField
         * is a {@link SingletonSet} because {@link SingletonSet} handles
         * interpolation better than a {@link GriddedSet} with one sample.
         */
        if ((domain.getLength() == 1) && !(domain instanceof SingletonSet)) {
            double[][] values = domain.getDoubles();
            double[]   vals   = new double[values.length];

            for (int i = 0; i < vals.length; i++) {
                vals[i] = values[i][0];
            }

            domain =
                new SingletonSet(((SetType) domain.getType()).getDomain(),
                                 vals, domain.getCoordinateSystem(),
                                 domain.getSetUnits(), domain.getSetErrors());
        }

        RealTupleType domainType = ((SetType) domain.getType()).getDomain();
        FlatField flatField = new FlatField(new FunctionType(domainType,
                                  rangeType), domain, rangeCoordinateSystem,
                                      (CoordinateSystem[]) null,
                                      (Set[]) null, range.getSetUnits());
        boolean useDoubles = false;

        for (int i = domainType.getDimension(); --i >= 0; ) {
            if (((RealType) domainType.getComponent(i)).getDefaultSet()
                    instanceof DoubleSet) {
                useDoubles = true;

                break;
            }
        }

        if (useDoubles) {
            flatField.setSamples(range.getDoubles(true), false);
        } else {
            flatField.setSamples(range.getSamples(true), false);
        }

        return flatField;
    }

    /**
     * Tests this class.
     *
     * @param args             Ignored.
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public static void main(String[] args)
            throws VisADException, RemoteException {

        RealType domainType = RealType.Latitude;
        FlatField flatField1 = new FlatField(
                                   new FunctionType(domainType, domainType),
                                   new visad.SingletonSet(
                                       new visad.RealTuple(
                                           new Real[] {
                                               new Real(domainType, 2.0) })));

        flatField1.setSamples(new float[][] {
            new float[] { 2.0f }
        });
        System.out.println("flatField1 = \n" + flatField1);

        domainType = RealType.Longitude;

        FlatField flatField2 = new FlatField(
                                   new FunctionType(domainType, domainType),
                                   new visad.SingletonSet(
                                       new visad.RealTuple(
                                           new Real[] {
                                               new Real(domainType, 2.0) })));

        flatField2.setSamples(new float[][] {
            new float[] { 1.0f }
        });
        System.out.println("flatField2 = \n" + flatField2);

        FlatField flatField3 = (FlatField) divide(flatField2,
                                   subtract(flatField1, flatField2));

        System.out.println("flatField3 = \n" + flatField3);
        System.out.println("flatField3.getRangeUnits()[0][0] = "
                           + flatField3.getRangeUnits()[0][0]);
    }
}

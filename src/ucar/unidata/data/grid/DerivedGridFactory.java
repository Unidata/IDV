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


import ucar.ma2.Range;

import ucar.unidata.data.DataUtil;
import ucar.unidata.data.VarInfo;
import ucar.unidata.data.point.PointObTuple;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.Misc;

import ucar.visad.UtcDate;
import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.DewPoint;
import ucar.visad.quantities.Direction;
import ucar.visad.quantities.EquivalentPotentialTemperature;
import ucar.visad.quantities.GeopotentialAltitude;
import ucar.visad.quantities.Gravity;
import ucar.visad.quantities.GridRelativeHorizontalWind;
import ucar.visad.quantities.Length;
import ucar.visad.quantities.PotentialTemperature;
import ucar.visad.quantities.RelativeHumidity;
import ucar.visad.quantities.SaturationMixingRatio;
import ucar.visad.quantities.SaturationVaporPressure;
import ucar.visad.quantities.WaterVaporMixingRatio;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;

import visad.util.DataUtility;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;


/**
 * DerivedGridFactory has static methods for creating various derived
 * quantities from grids. A grid is defined as a FieldImpl which has
 * one of the following MathTypes structures:
 * <PRE>
 *   (x,y) -> (parm)
 *   (x,y) -> (parm1, ..., parmN)
 *   (x,y,z) -> (parm)
 *   (x,y,z) -> (parm1, ..., parmN)
 *   (t -> (x,y) -> (parm))
 *   (t -> (x,y) -> (parm1, ..., parmN))
 *   (t -> (x,y,z) -> (parm))
 *   (t -> (x,y,z) -> (parm1, ..., parmN))
 *   (t -> (index -> (x,y) -> (parm)))
 *   (t -> (index -> (x,y) -> (parm1, ..., parmN)))
 *   (t -> (index -> (x,y,z) -> (parm)))
 *   (t -> (index -> (x,y,z) -> (parm1, ..., parmN)))
 * </PRE>
 * In general, t is a time variable, but it might also be just
 * an index.
 *
 * @author Don Murray
 * @version $Revision: 1.73 $
 */
public class DerivedGridFactory {

    /** negative one */
    public static final Real NEGATIVE_ONE = GridMath.NEGATIVE_ONE;

    /** EARTH RADIUS (6371 km) */
    public static final Real EARTH_RADIUS;

    /** EARTH 2 omega */
    public static final Real EARTH_TWO_OMEGA;

    /** gravity */
    public static final Real GRAVITY;

    /** climatology units */
    public static final Unit CLIMATE_UNITS;

    /** anomaly percentage type */
    private static final RealType ANOM_PERCENT_TYPE;

    static {
        try {
            EARTH_RADIUS = new Real(Length.getRealType(), 6371000, SI.meter);
            EARTH_TWO_OMEGA = new Real(DataUtil.makeRealType("frequency",
                    SI.second.pow(-1)), 0.00014584, SI.second.pow(-1));
            GRAVITY = Gravity.newReal();
            CLIMATE_UNITS = Util.parseUnit("days since 0001-01-01 00:00:00");
            ANOM_PERCENT_TYPE = RealType.getRealType("percent_of_normal",
                    CommonUnits.PERCENT);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex.toString());
        }
    }

    /** Default ctor; does nothing */
    public DerivedGridFactory() {}

    /**
     * Create an ensemble average
     *
     * @param grid the grid of ensembles
     *
     * @return  the average of the members
     *
     * @throws RemoteException java RMI problem
     * @throws VisADException  VisAD error
     */
    public static FieldImpl ensembleAverage(FieldImpl grid)
            throws VisADException, RemoteException {
        return GridMath.applyFunctionOverMembers(grid, GridMath.FUNC_AVERAGE);
    }

    /**
     * Create a 1000-500 mb thickness grid
     *
     * @param grid   grid (hopefully a height grid)
     * @return  thickness field.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     * @see #createLayerDifference(FieldImpl, String, String)
     */
    public static FieldImpl createThickness(FieldImpl grid)
            throws VisADException, RemoteException {
        return createLayerDifference(grid, 500, 1000,
                                     CommonUnits.HECTOPASCAL);
    }

    /**
     * Make the difference of one grid's values at the given levels;
     * first level subtract second level values.
     *
     * @param grid grid of data
     * @param value1 level the first as a String
     * @param value2 level the second as a String
     *
     * @return computed layer difference
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createLayerDifference(FieldImpl grid,
            String value1, String value2)
            throws VisADException, RemoteException {
        return createLayerDifference(grid, value1, value2, (String) null);
    }

    /**
     * Make the difference of one grid's values at the given levels;
     * first level subtract second level values.
     *
     * @param grid grid of data
     * @param value1 level the first as a String
     * @param value2 level the second as a String
     * @param levelUnit  unit spec for level
     *
     * @return computed layer difference
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createLayerDifference(FieldImpl grid,
            String value1, String value2, String levelUnit)
            throws VisADException, RemoteException {
        return createLayerDifference(grid, Misc.parseNumber(value1),
                                     Misc.parseNumber(value2),
                                     (levelUnit != null)
                                     ? DataUtil.parseUnit(levelUnit)
                                     : (Unit) null);
    }

    /**
     * Make the difference of one grid's values at the given levels;
     * first level subtract second level values.
     *
     * @param grid     grid of data
     * @param value1   level of first
     * @param value2   level of second
     *
     * @return computed layer difference
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createLayerDifference(FieldImpl grid,
            double value1, double value2)
            throws VisADException, RemoteException {
        return createLayerDifference(grid, value1, value2, null);
    }

    /**
     * Make the difference of one grid's values at the given levels;
     * first level subtract second level values.
     *
     * @param grid     grid of data
     * @param value1   level of first
     * @param value2   level of second
     * @param levelUnit  unit for level
     *
     * @return computed layer difference
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createLayerDifference(FieldImpl grid,
            double value1, double value2, Unit levelUnit)
            throws VisADException, RemoteException {
        RealType levelType = RealType.Generic;

        if (levelUnit != null) {
            if (Unit.canConvert(levelUnit, CommonUnits.HECTOPASCAL)) {
                levelType = AirPressure.getRealType();
            } else if (Unit.canConvert(levelUnit, CommonUnit.meter)) {
                levelType = RealType.Altitude;
            } else {  // TODO:  figure out something better
                levelUnit = null;
            }
        }

        Real      level1 = (levelUnit != null)
                           ? new Real(levelType, value1, levelUnit)
                           : new Real(levelType, value1);
        Real      level2 = (levelUnit != null)
                           ? new Real(levelType, value2, levelUnit)
                           : new Real(levelType, value2);
        FieldImpl first  =
        // GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, value1),
        GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, level1),
                                     false);
        FieldImpl second =
        // GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, value2),
        GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, level2),
                                     false);
        TupleType paramType = GridUtil.getParamType(grid);
        FieldImpl result    = (FieldImpl) first.subtract(second);

        if (paramType.getDimension() == 1) {
            RealType rt = (RealType) paramType.getComponent(0);
            String newName = rt.getName() + "_LDF_" + (int) value1 + "-"
                             + (int) value2;
            RealTupleType rtt =
                new RealTupleType(DataUtil.makeRealType(newName,
                    rt.getDefaultUnit()));

            result = GridUtil.setParamType(result, rtt,
                                           false /* don't copy */);
        }

        return result;
    }

    /**
     * Make the average of 2 levels of a grid
     *
     * @param grid grid of data
     * @param value1 level the first as a String
     * @param value2 level the second as a String
     *
     * @return computed layer difference
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createLayerAverage(FieldImpl grid, String value1,
            String value2)
            throws VisADException, RemoteException {
        return createLayerAverage(grid, value1, value2, (String) null);
    }

    /**
     * Make the average of 2 levels of a grid
     *
     * @param grid grid of data
     * @param value1 level the first as a String
     * @param value2 level the second as a String
     * @param levelUnit  unit for level
     *
     * @return computed layer difference
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createLayerAverage(FieldImpl grid, String value1,
            String value2, String levelUnit)
            throws VisADException, RemoteException {
        return createLayerAverage(grid, Misc.parseNumber(value1),
                                  Misc.parseNumber(value2),
                                  (levelUnit != null)
                                  ? DataUtil.parseUnit(levelUnit)
                                  : (Unit) null);
    }

    /**
     * Make the average of 2 levels of a grid
     *
     * @param grid     grid of data
     * @param value1   level of first
     * @param value2   level of second
     *
     * @return computed layer difference
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createLayerAverage(FieldImpl grid, double value1,
            double value2)
            throws VisADException, RemoteException {
        return createLayerAverage(grid, value1, value2, null);
    }

    /**
     * Make the average of 2 levels of a grid
     *
     * @param grid     grid of data
     * @param value1   level of first
     * @param value2   level of second
     * @param levelUnit  unit for level
     *
     * @return computed layer difference
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createLayerAverage(FieldImpl grid, double value1,
            double value2, Unit levelUnit)
            throws VisADException, RemoteException {
        RealType levelType = RealType.Generic;

        if (levelUnit != null) {
            if (Unit.canConvert(levelUnit, CommonUnits.HECTOPASCAL)) {
                levelType = AirPressure.getRealType();
            } else if (Unit.canConvert(levelUnit, CommonUnit.meter)) {
                levelType = RealType.Altitude;
            } else {  // TODO:  figure out something better
                levelUnit = null;
            }
        }

        Real      level1 = (levelUnit != null)
                           ? new Real(levelType, value1, levelUnit)
                           : new Real(levelType, value1);
        Real      level2 = (levelUnit != null)
                           ? new Real(levelType, value2, levelUnit)
                           : new Real(levelType, value2);
        FieldImpl first  =
        // GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, value1),
        GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, level1),
                                     false);
        FieldImpl second =
        // GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, value2),
        GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, level2),
                                     false);
        TupleType paramType = GridUtil.getParamType(grid);
        FieldImpl result =
            (FieldImpl) (first.add(second)).divide(new Real(2));

        if (paramType.getDimension() == 1) {
            RealType rt = (RealType) paramType.getComponent(0);
            String newName = rt.getName() + "_LDF_" + (int) value1 + "-"
                             + (int) value2;
            RealTupleType rtt =
                new RealTupleType(DataUtil.makeRealType(newName,
                    rt.getDefaultUnit()));

            result = GridUtil.setParamType(result, rtt,
                                           false /* don't copy */);
        }

        return result;
    }

    /**
     * Computes relative vorticity from grid-relative wind components.  The
     * first and second components of the range of the input
     * {@link visad.FieldImpl} are assumed to be the velocity of the wind
     * in the direction of increasing first and second dimension of the
     * domain, respectively.
     *
     * @param  uFI  grid or time sequence of grids of positive-X wind comp.
     * @param  vFI  grid or time sequence of grids of positive-Y wind comp.
     *
     * @return computed relative vorticity.
     *
     * @see "Meteorology for Scientists and Engineers, p. 233"
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createRelativeVorticity(FieldImpl uFI,
            FieldImpl vFI)
            throws VisADException, RemoteException {

        boolean      UisSequence = (GridUtil.isTimeSequence(uFI));
        boolean      VisSequence = (GridUtil.isTimeSequence(vFI));
        FieldImpl    rvFI        = null;
        FlatField    dvdx        = null;
        FlatField    dudy        = null;
        FlatField    rvFF        = null;
        FlatField    uFF         = null;
        FlatField    vFF         = null;
        FunctionType rvFFType    = null;

        if (UisSequence) {

            // Implementation:  have to take the raw data FieldImpl-s
            // apart, make a FlatField by FlatField,
            // and put all back together again into a new FieldImpl
            Set     timeSet = uFI.getDomainSet();
            Boolean ensble  = GridUtil.hasEnsemble(uFI);
            // resample to domainSet of uFI. (???)  If they are the same, this
            // should be a no-op
            if ((timeSet.getLength() > 1) && (VisSequence == true)) {
                vFI = (FieldImpl) vFI.resample(timeSet);
            }

            // compute each rel vort  FlatField for time steps in turn;
            // load in FieldImpl
            RealType     rvRT      = null;
            TupleType    rangeType = null;
            FunctionType innerType = null;

            for (int i = 0; i < timeSet.getLength(); i++) {
                FieldImpl funcFF = null;
                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) uFI.getSample(i);
                    FieldImpl sample2   = (FieldImpl) vFI.getSample(i);
                    Set       ensDomain = sample1.getDomainSet();

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);
                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        // System.out.print("      type of u FF is "+((FunctionType)uFF.getType()));
                        // the derivative of u by y
                        dudy = (FlatField) ddy(innerField1);

                        // the derivative of v by x
                        dvdx = (FlatField) ddx(innerField2);

                        // sum is dvdx - dudy  for final result at this time step
                        FlatField innerrvFF =
                            (FlatField) (dvdx.subtract(dudy));


                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerrvFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerrvFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerrvFF, false);

                    }
                    if (rvFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        rvFI = new FieldImpl(newFieldType, timeSet);
                    }
                    rvFI.setSample(i, funcFF, false);
                } else {
                    // System.out.print(" "+i); //...
                    // get u and v single grids for this time step
                    uFF = (FlatField) uFI.getSample(i);
                    vFF = (FlatField) vFI.getSample(i);

                    // System.out.print("      type of u FF is "+((FunctionType)uFF.getType()));
                    // the derivative of u by y
                    dudy = (FlatField) ddy(uFF);

                    // the derivative of v by x
                    dvdx = (FlatField) ddx(vFF);

                    // sum is dvdx - dudy  for final result at this time step
                    rvFF = (FlatField) (dvdx.subtract(dudy));

                    if (i == 0) {

                        // first time through, set up rvFI
                        // make the VisAD FunctionType for the rel vort; several steps
                        FunctionType functionType =
                            new FunctionType(
                                ((FunctionType) uFI.getType()).getDomain(),
                                rvFF.getType());

                        // System.out.println ("       rvFI func type = "+functionType);
                        // make the new FieldImpl (but as yet empty of data)
                        rvFI = new FieldImpl(functionType, timeSet);
                    }

                    // System.out.println ("    rv single grid range type = "+
                    // ((FunctionType)rvFF.getType()).getRange());

                    // set this time's grid
                    rvFI.setSample(i, rvFF, false);
                }
            }
        } else {
            rvFI = (FieldImpl) ddx(vFF).subtract(ddy(uFF));
        }

        Unit     rvUnit = GridUtil.getParamUnits(rvFI)[0];
        RealType rvRT   = DataUtil.makeRealType("relvorticity", rvUnit);

        return GridUtil.setParamType(rvFI, rvRT, false);

    }  // end method create Relative Vorticity

    /**
     * Computes relative vorticity from U and V.  The grid is not assumed to be
     * aligned with north and south.  Partial derivatives of the wind components
     * are taken with respect to the latitude and longitude dimensions of the
     * reference of the {@link visad.CoordinateSystem} of the input spatial
     * domains.
     *
     * @param  uFI  grid or time sequence of grids of the eastward  wind comp.
     * @param  vFI  grid or time sequence of grids of the northward wind comp.
     *
     * @return computed relative vorticity.
     *
     * @throws IllegalArgumentException if the input spatial domain(s) don't
     *                                  have a {@link visad.CoordinateSystem}
     *                                  whose reference contains {@link
     *                                  visad.RealType#Latitude} and {@link
     *                                  visad.RealType#Longitude}.
     * @see "Meteorology for Scientists and Engineers, p. 233"
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl relativeVorticityFromTrueWind(FieldImpl uFI,
            FieldImpl vFI)
            throws VisADException, RemoteException {
        FieldImpl trueWind = createTrueFlowVectors(uFI, vFI);

        return createRelativeVorticity(getUComponent(trueWind),
                                       getVComponent(trueWind));
    }

    /**
     * Find the component in the RealTupleType that starts with the prefix
     * (case insensitively) and has the same units as the template
     *
     * @param rtt        RealTupleType to check
     * @param prefix     name prefix  (case insensitive)
     * @param template   RealType template for checking units
     * @return
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static RealType findComponent(RealTupleType rtt, String prefix,
                                          RealType template)
            throws VisADException, RemoteException {
        for (int i = 0, n = rtt.getDimension(); i < n; i++) {
            RealType rt = (RealType) rtt.getComponent(i);

            if (rt.getName().toLowerCase().startsWith(prefix)
                    && rt.equalsExceptNameButUnits(template)) {
                return rt;
            }
        }

        return null;
    }

    /**
     * Computes absolute vorticity from grid-relative wind components.
     * Absolute vorticity is relative vorticity plus the Coriolus parameter.
     * The first and second components of the range of the input
     * {@link visad.FieldImpl} are assumed to be the velocity of the wind
     * in the direction of increasing first and second dimension of the
     * domain, respectively.
     *
     * @param  uFI  grid or time sequence of grids of positive-X wind comp.
     * @param  vFI  grid or time sequence of grids of positive-Y wind comp.
     *
     * @return computed absolute vorticity.
     *
     * @see "Meteorology for Scientists and Engineers, p. 234"
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createAbsoluteVorticity(FieldImpl uFI,
            FieldImpl vFI)
            throws VisADException, RemoteException {
        FieldImpl relVor = createRelativeVorticity(uFI, vFI);
        FieldImpl corl   = createCoriolisGrid(relVor);
        FieldImpl avFI   = (FieldImpl) relVor.add(corl);
        Unit      avUnit = GridUtil.getParamUnits(avFI)[0];
        RealType  avRT   = DataUtil.makeRealType("absvorticity", avUnit);

        return GridUtil.setParamType(avFI, avRT, false);
    }  // end method create Absolute Vorticity (FieldImpl uFI, FieldImpl vFI)

    /**
     * Make a grid of true wind vectors from grid relative u and v
     * components.
     *
     * @param uGrid  grid of U wind component
     * @param vGrid  grid of V wind component
     *
     * @return true wind components
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createTrueWindVectors(FieldImpl uGrid,
            FieldImpl vGrid)
            throws VisADException, RemoteException {
        return createTrueFlowVectors(uGrid, vGrid);
    }

    /**
     * Make a grid of true flow vectors from grid relative u and v
     * components.
     *
     * @param uGrid  grid of U wind component
     * @param vGrid  grid of V wind component
     *
     * @return true flow components
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createTrueFlowVectors(FieldImpl uGrid,
            FieldImpl vGrid)
            throws VisADException, RemoteException {
        FieldImpl uvGrid = createFlowVectors(uGrid, vGrid);

        return createTrueFlowVector(uvGrid);
    }

    /**
     * Make a grid of true flow vectors from grid relative u and v
     * components.
     *
     * @param uvGrid vector of uv grids
     *
     * @return true flow components
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createTrueFlowVector0(FieldImpl uvGrid)
            throws VisADException, RemoteException {
        if ( !isVector(uvGrid)) {
            throw new VisADException("Not a vector grid "
                                     + GridUtil.getParamType(uvGrid));
        }

        ucar.unidata.util.Trace.call1("DGF:createTrueFlowVector");

        FieldImpl result =
            (FieldImpl) GridRelativeHorizontalWind.cartesianHorizontalWind(
                uvGrid);

        ucar.unidata.util.Trace.call2("DGF:createTrueFlowVector");

        return result;
    }


    /**
     * Create true flow vectors from a uv grid.
     *
     * @param uvGrid  the uv grid
     *
     * @return  the grid converted to true (north) winds
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD problem
     */
    public static FieldImpl createTrueFlowVector(FieldImpl uvGrid)
            throws VisADException, RemoteException {
        if ( !isVector(uvGrid)) {
            throw new VisADException("Not a vector grid "
                                     + GridUtil.getParamType(uvGrid));
        }
        FieldImpl result = null;
        ucar.unidata.util.Trace.call1("DGF:createTrueFlowVector");
        Boolean ensble = GridUtil.hasEnsemble(uvGrid);

        if (ensble) {
            FunctionType outerFuncType = (FunctionType) uvGrid.getType();
            Set          timeSet       = uvGrid.getDomainSet();
            TupleType    rangeType     = null;
            FunctionType innerType     = null;
            FunctionType newinnerType  = null;

            for (int i = 0; i < timeSet.getLength(); i++) {
                FieldImpl funcFF    = null;
                FieldImpl funcFF0   = null;
                FieldImpl sample    = (FieldImpl) uvGrid.getSample(i);
                Set       ensDomain = sample.getDomainSet();

                for (int j = 0; j < ensDomain.getLength(); j++) {
                    FieldImpl innerField = (FieldImpl) sample.getSample(j,
                                               false);
                    if (innerField == null) {
                        continue;
                    }
                    if (newinnerType == null) {
                        newinnerType = new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            innerField.getType());
                    }
                    RealType index = RealType.getRealType("index");
                    SingletonSet ss =
                        new SingletonSet(new RealTuple(new Real[] {
                            new Real(index, 0) }));
                    funcFF0 = new FieldImpl(newinnerType, ss);
                    funcFF0.setSample(0, innerField, false);
                    FieldImpl innerrvFF =
                        (FieldImpl) GridRelativeHorizontalWind
                            .cartesianHorizontalWind(funcFF0);

                    if (rangeType == null) {
                        rangeType = GridUtil.getParamType(innerrvFF);
                        innerType = new FunctionType(
                            DataUtility.getDomainType(ensDomain),
                            innerrvFF.getSample(0).getType());

                    }
                    if (funcFF == null) {
                        funcFF = new FieldImpl(innerType, ensDomain);
                    }

                    funcFF.setSample(j, innerrvFF.getSample(0), false);
                }
                if (result == null) {
                    FunctionType newFieldType =
                        new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            funcFF.getType());
                    result = new FieldImpl(newFieldType, timeSet);
                }
                result.setSample(i, funcFF, false);
            }

        } else {
            result =
                (FieldImpl) GridRelativeHorizontalWind
                    .cartesianHorizontalWind(uvGrid);
        }
        ucar.unidata.util.Trace.call2("DGF:createTrueFlowVector");

        return result;
    }

    /**
     * Make a FieldImpl of wind vectors from u and v components.
     *
     * @param uGrid  grid of U wind component
     * @param vGrid  grid of V wind component
     *
     * @return combine two separate fields (u and v) into one grid (u,v)
     *
     * @throws VisADException  VisAD problem
     * @throws RemoteException  remote problem
     * @deprecated use #createFlowVectors(uGrid, vGrid)
     */
    public static FieldImpl createWindVectors(FieldImpl uGrid,
            FieldImpl vGrid)
            throws VisADException, RemoteException {
        return createFlowVectors(uGrid, vGrid);
    }

    /**
     * Make a FieldImpl of geostrophic wind.
     *
     * @param  paramFI parameter to use (height)
     *
     * @return vector of geopotential height
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createGeostrophicWindVector(FieldImpl paramFI)
            throws VisADException, RemoteException {
        Unit u = GridUtil.getParamUnits(paramFI)[0];

        if (u.equals(GeopotentialAltitude.getGeopotentialMeter())) {
            paramFI = (FieldImpl) paramFI.divide(GRAVITY);
        }

        FieldImpl corl = createCoriolisGrid(paramFI);
        FieldImpl ug   = (FieldImpl) ddy(paramFI).multiply(GRAVITY);

        ug = (FieldImpl) ug.divide(corl).negate();
        ug = GridUtil.setParamType(ug, "ugeo");

        FieldImpl vg = (FieldImpl) ddx(paramFI).multiply(GRAVITY);

        vg = (FieldImpl) vg.divide(corl);
        vg = GridUtil.setParamType(vg, "vgeo");

        return createFlowVectors(ug, vg);
    }

    /**
     * Make a FieldImpl of some parameter and topography.  We add a little
     * bit to the topography grid so it will raise it up just a tad
     *
     * @param paramGrid  parameter grid
     * @param topoGrid   grid of topography.  Must have units convertible
     *                   with meter or geopotential meter.
     *
     * @return combined grids
     *
     * @throws VisADException  VisAD problem
     * @throws RemoteException  remote problem
     */
    public static FieldImpl create2DTopography(FieldImpl paramGrid,
            FieldImpl topoGrid)
            throws VisADException, RemoteException {
        return create2DTopography(paramGrid, topoGrid, false);
    }

    /**
     * Make a FieldImpl of some parameter and topography.  We add a little
     * bit to the topography grid so it will raise it up just a tad
     *
     * @param paramGrid  parameter grid
     * @param topoGrid   grid of topography.  Must have units convertible
     *                   with meter or geopotential meter.
     * @param resampleToTopography true to resample to the topography domain
     *
     * @return combined grids
     *
     * @throws VisADException  VisAD problem
     * @throws RemoteException  remote problem
     */
    public static FieldImpl create2DTopography(FieldImpl paramGrid,
            FieldImpl topoGrid, boolean resampleToTopography)
            throws VisADException, RemoteException {

        FieldImpl grid     = paramGrid;
        TupleType tt       = GridUtil.getParamType(topoGrid);
        RealType  rt       = tt.getRealComponents()[0];
        Unit      topoUnit = rt.getDefaultUnit();
        if (Unit.canConvert(topoUnit, CommonUnits.MILLIBAR)
                && (tt.getDimension() == 1)) {
            topoGrid = convertPressureToHeight(topoGrid);
        }
        if ( !(Unit.canConvert(topoUnit,
                               GeopotentialAltitude
                                   .getGeopotentialMeter()) || Unit
                                       .canConvert(topoUnit,
                                           CommonUnit.meter))) {
            throw new VisADException("topography units " + topoUnit
                                     + " must convertible with m or gpm");
        }

        // check to make sure topo is not already in the grid
        if (MathType.findScalarType(GridUtil.getParamType(paramGrid), rt)) {
            return paramGrid;
        } else {

            // check to make sure domains are compatible
            Set topoDomain  = GridUtil.getSpatialDomain(topoGrid);
            Set paramDomain = GridUtil.getSpatialDomain(paramGrid);

            // System.err.println("topo domain " +topoDomain);
            // System.err.println("param domain " +paramDomain);

            // 3D grid on a 2D manifold over a 2D topography
            if ((topoDomain.getDimension() == 2)
                    && (paramDomain.getDimension() == 3)
                    && (paramDomain.getManifoldDimension() == 2)) {
                grid        = GridUtil.make2DGridFromSlice(paramGrid, true);
                paramDomain = GridUtil.getSpatialDomain(grid);

                // System.err.println("new param domain " +paramDomain);
                // 2D grid over a 3D topography on a 2D manifold
            } else if ((paramDomain.getDimension() == 2)
                       && (topoDomain.getDimension() == 3)
                       && (topoDomain.getManifoldDimension() == 2)) {
                topoGrid   = GridUtil.make2DGridFromSlice(topoGrid, true);
                topoDomain = GridUtil.getSpatialDomain(topoGrid);

                // System.err.println("new topo domain " +topoDomain);
                // unequal 3D grid and topo with 2D manifolds
            } else if (((paramDomain.getDimension() == 3)
                        && (paramDomain.getManifoldDimension()
                            == 2)) && ((topoDomain.getDimension() == 3)
                                       && (topoDomain.getManifoldDimension()
                                           == 2)) && !topoDomain.equals(
                                               paramDomain)) {
                grid        = GridUtil.make2DGridFromSlice(paramGrid, true);
                paramDomain = GridUtil.getSpatialDomain(grid);
                topoGrid    = GridUtil.make2DGridFromSlice(topoGrid, true);
                topoDomain  = GridUtil.getSpatialDomain(topoGrid);
            }


            RealTupleType paramRef = null;
            RealTupleType topoRef  = null;

            if (paramDomain.getCoordinateSystem() != null) {
                paramRef = paramDomain.getCoordinateSystem().getReference();
            } else {
                paramRef = ((SetType) paramDomain.getType()).getDomain();
            }

            if (topoDomain.getCoordinateSystem() != null) {
                topoRef = topoDomain.getCoordinateSystem().getReference();
            } else {
                topoRef = ((SetType) topoDomain.getType()).getDomain();
            }

            // System.err.println("paramRef = " + paramRef);
            // System.err.println("topoRef = " + topoRef);
            // lat/lon over lon/lat (or vice versa)
            if ( !paramRef.equals(topoRef)) {

                // System.out.println("refs aren't equal");
                if (topoDomain.getCoordinateSystem() == null) {
                    if ((topoRef.equals(RealTupleType.SpatialEarth2DTuple)
                            || topoRef.equals(
                                RealTupleType.LatitudeLongitudeTuple))) {
                        topoGrid = GridUtil.swapLatLon(topoGrid);
                    }
                }
            }

            // }
            if (resampleToTopography) {
                grid = GridUtil.resampleGrid(
                    grid, GridUtil.getSpatialDomain(topoGrid));
            }

            return combineGrids(grid, topoGrid);
        }

    }

    /**
     * Convert a pressure field to height values using the supplied coordinate system
     *
     * @param pressureField  the pressure field.  Must have units convertible with hPa
     *                       and have a single range dimension.
     *
     * @return  the height field
     *
     * @throws RemoteException Java RMI exception
     * @throws VisADException  Illegal field or some other VisAD error
     */
    public static FieldImpl convertPressureToHeight(FieldImpl pressureField)
            throws VisADException, RemoteException {
        return convertPressureToHeight(
            pressureField,
            DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE));
    }

    /**
     * Convert a pressure field to height values using the supplied coordinate system
     *
     * @param pressureField  the pressure field.  Must have units convertible with hPa
     *                       and have a single range dimension.
     * @param pressToHeightCS The transform.  If null, standard atmosphere is used.
     *
     * @return  the height field
     *
     * @throws RemoteException Java RMI exception
     * @throws VisADException  Illegal field or some other VisAD error
     */
    public static FieldImpl convertPressureToHeight(FieldImpl pressureField,
            CoordinateSystem pressToHeightCS)
            throws VisADException, RemoteException {
        TupleType tt        = GridUtil.getParamType(pressureField);
        RealType  rt        = tt.getRealComponents()[0];
        Unit      pressUnit = rt.getDefaultUnit();
        if ( !(Unit.canConvert(pressUnit, CommonUnits.MILLIBAR)
                && (tt.getDimension() == 1))) {
            throw new VisADException(
                "Pressure field must have units convertible with hPa and have only one range type");
        }

        if (pressToHeightCS == null) {
            pressToHeightCS =
                DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE);
        }
        // setParamType will create a new FieldImpl of the same structure as the original using Util.clone()
        FieldImpl heightGrid = GridUtil.setParamType(pressureField,
                                   RealType.getRealType("topo",
                                       CommonUnit.meter));
        if (GridUtil.isSequence(heightGrid)) {
            Set seqSet = heightGrid.getDomainSet();
            for (int i = 0; i < seqSet.getLength(); i++) {
                FlatField ff = (FlatField) heightGrid.getSample(i, false);
                float[][] pressVals = ff.getFloats();
                float[][] heightVals = pressToHeightCS.toReference(pressVals,
                                           new Unit[] { pressUnit });
                ff.setSamples(heightVals, false);
            }
        } else {
            float[][] pressVals = heightGrid.getFloats();
            float[][] heightVals = pressToHeightCS.toReference(pressVals,
                                       new Unit[] { pressUnit });
            ((FlatField) heightGrid).setSamples(heightVals, false);
        }
        return heightGrid;
    }


    /**
     * _more_
     *
     * @param pressureVelField _more_
     * @param hField _more_
     * @param pressToHeightCS _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static FieldImpl convertPressureVelocityToHeightVelocity(
            FieldImpl pressureVelField, FieldImpl hField,
            CoordinateSystem pressToHeightCS)
            throws VisADException, RemoteException {
        TupleType tt        = GridUtil.getParamType(pressureVelField);
        RealType  rt        = tt.getRealComponents()[0];
        Unit      pressUnit = rt.getDefaultUnit().multiply(CommonUnit.second);
        if (Unit.canConvert(pressUnit, CommonUnit.meter)) {
            return pressureVelField;
        } else if ( !(Unit.canConvert(pressUnit, CommonUnits.MILLIBAR)
                      && (tt.getDimension() == 1))) {
            throw new VisADException(
                "Pressure velocity field must have units convertible with hPa and have only one range type");
        }

        if (pressToHeightCS == null) {
            pressToHeightCS =
                DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE);
        }
        // setParamType will create a new FieldImpl of the same structure as the original using Util.clone()

        FieldImpl dhdp = GridMath.partial(hField, 2);
        FieldImpl dhdt = GridMath.multiply(pressureVelField, dhdp);
        FieldImpl heightGrid = GridUtil.setParamType(dhdt,
                                   RealType.getRealType("zVel",
                                       CommonUnits.METERS_PER_SECOND));

        if (GridUtil.isSequence(heightGrid)) {
            Set seqSet = heightGrid.getDomainSet();
            for (int i = 0; i < seqSet.getLength(); i++) {
                FlatField ff = (FlatField) heightGrid.getSample(i, false);
                float[][] pressVals = ff.getFloats();
                ff.setSamples(pressVals, false);
            }
        } else {
            float[][] pressVals = heightGrid.getFloats();

            ((FlatField) heightGrid).setSamples(pressVals, false);
        }
        return heightGrid;
    }

    /**
     * Make a FieldImpl of wind vectors from u and v components.
     *
     * @param uGrid  grid of U flow component
     * @param vGrid  grid of V flow component
     *
     * @return combine two separate fields (u and v) into one grid (u,v)
     *
     * @throws VisADException  VisAD problem
     * @throws RemoteException  remote problem
     */
    public static FieldImpl createFlowVectors(FieldImpl uGrid,
            FieldImpl vGrid)
            throws VisADException, RemoteException {
        FieldImpl uvGrid      = combineGrids(uGrid, vGrid,
                                             true /* flatten */);
        FieldImpl retGrid     = uvGrid;
        Unit[]    units       = GridUtil.getParamUnits(uvGrid);
        boolean   isFlowUnits = true;

        for (int i = 0; i < units.length; i++) {
            Unit u = units[i];

            isFlowUnits = (u == null)
                          || Unit.canConvert(u, CommonUnit.meterPerSecond);

            if ( !isFlowUnits) {

                // System.out.println("not flow units");
                break;
            }
        }

        if (isFlowUnits) {

            // System.out.println("making earth vector type");
            TupleType paramType = GridUtil.getParamType(uvGrid);
            RealType[] reals = Util.ensureUnit(paramType.getRealComponents(),
                                   CommonUnit.meterPerSecond);
            RealTupleType earthVectorType = new EarthVectorType(reals[0],
                                                reals[1]);

            retGrid = GridUtil.setParamType(uvGrid, earthVectorType,
                                            false /* copy */);
        }

        return retGrid;
    }

    /**
     * Make a FieldImpl of flow vectors from u, v and w components.
     *
     * @param uGrid  grid of U flow component
     * @param vGrid  grid of V flow component
     * @param wGrid  grid of W flow component
     *
     * @return combine three separate fields (u, v and w) into one grid (u,v,w)
     *
     * @throws VisADException  VisAD problem
     * @throws RemoteException  remote problem
     */
    public static FieldImpl createFlowVectors(FieldImpl uGrid,
            FieldImpl vGrid, FieldImpl wGrid)
            throws VisADException, RemoteException {
        FieldImpl uvwGrid = combineGrids(new FieldImpl[] { uGrid, vGrid,
                wGrid }, true);
        TupleType paramType = GridUtil.getParamType(uvwGrid);
        RealType[] reals = Util.ensureUnit(paramType.getRealComponents(),
                                           CommonUnit.meterPerSecond);
        RealTupleType earthVectorType = new EarthVectorType(reals[0],
                                            reals[1], reals[2]);

        return GridUtil.setParamType(uvwGrid, earthVectorType,
                                     false /* copy */);
    }

    /**
     * Combine an array of grids into one.  If the grids are on different
     * time domains, they are resampled to the domain of the first.
     *
     * @param grids  array of grids (must have at least 2)
     *
     * @return combined grid.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl combineGrids(FieldImpl[] grids)
            throws VisADException, RemoteException {
        return combineGrids(grids, false);
    }

    /**
     * Combine an array of grids into one.  If the grids are on different
     * time domains, they are resampled to the domain of the first.  Flatten
     *
     * @param grids  array of grids (must have at least 2)
     * @param flatten  flatten the structure
     *
     * @return combined grid.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl combineGrids(FieldImpl[] grids, boolean flatten)
            throws VisADException, RemoteException {
        return combineGrids(grids, GridUtil.DEFAULT_SAMPLING_MODE,
                            GridUtil.DEFAULT_ERROR_MODE, flatten);
    }

    /**
     * Combine an array of grids into one.  If the grids are on different
     * time domains, they are resampled to the domain of the first.
     *
     * @param grids  array of grids (must have at least 2)
     * @param samplingMode  sampling mode (e.g. WEIGHTED_AVERAGE, NEAREST_NEIGHBOR)
     * @param errorMode   sampling error mode (e.g. NO_ERRORS)
     * @param flatten     false to keep tuple integrity.
     *
     * @return combined grid.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl combineGrids(FieldImpl[] grids, int samplingMode,
                                         int errorMode, boolean flatten)
            throws VisADException, RemoteException {
        if (grids.length < 2) {
            throw new IllegalArgumentException(
                "must have at least 2 grids for this method");
        }

        FieldImpl outGrid = grids[0];

        for (int i = 1; i < grids.length; i++) {
            outGrid = combineGrids(outGrid, grids[i], samplingMode,
                                   errorMode, flatten);
        }

        return outGrid;
    }

    /**
     * Combine three Fields into one.  If the grids are on different
     * time domains, the second is resampled to the domain of the first.
     *
     * @param grid1  first grid.  This will be used for the time/space domain
     * @param grid2  second grid.
     * @param grid3  third grid.
     *
     * @return combined grid.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl combineGrids(FieldImpl grid1, FieldImpl grid2,
                                         FieldImpl grid3)
            throws VisADException, RemoteException {
        return combineGrids(new FieldImpl[] { grid1, grid2, grid3 });
    }

    /**
     * Combine two Fields into one.  If the grids are on different
     * time domains, the second is resampled to the domain of the first.
     *
     * @param grid1  first grid.  This will be used for the time/space domain
     * @param grid2  second grid.
     *
     * @return combined grid.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl combineGrids(FieldImpl grid1, FieldImpl grid2)
            throws VisADException, RemoteException {
        return combineGrids(grid1, grid2, false);
    }

    /**
     * Combine two Fields into one.  If the grids are on different
     * time domains, the second is resampled to the domain of the first.
     *
     * @param grid1  first grid.  This will be used for the time/space domain
     * @param grid2  second grid.
     * @param flatten  true to flatten
     *
     * @return combined grid.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl combineGrids(FieldImpl grid1, FieldImpl grid2,
                                         boolean flatten)
            throws VisADException, RemoteException {
        return combineGrids(grid1, grid2, GridUtil.DEFAULT_SAMPLING_MODE,
                            GridUtil.DEFAULT_ERROR_MODE, flatten);
    }

    /**
     * Combine two Fields into one.  If the grids are on different
     * time domains, the second is resampled to the domain of the first.
     *
     * @param grid1  first grid.  This will be used for the time/space domain
     * @param grid2  second grid.
     * @param samplingMode  sampling mode (e.g. WEIGHTED_AVERAGE, NEAREST_NEIGHBOR)
     * @param errorMode   sampling error mode (e.g. NO_ERRORS)
     * @param flatten     false to keep tuple integrity.
     *
     * @return combined grid.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl combineGrids(FieldImpl grid1, FieldImpl grid2,
                                         int samplingMode, int errorMode,
                                         boolean flatten)
            throws VisADException, RemoteException {
        return combineGrids(grid1, grid2, samplingMode, errorMode, flatten,
                            false);
    }

    /**
     * Combine two Fields into one.  If the grids are on different
     * time domains, the second is resampled to the domain of the first.
     *
     * @param grid1  first grid.  This will be used for the time/space domain
     * @param grid2  second grid.
     * @param samplingMode  sampling mode (e.g. WEIGHTED_AVERAGE, NEAREST_NEIGHBOR)
     * @param errorMode   sampling error mode (e.g. NO_ERRORS)
     * @param flatten     false to keep tuple integrity.
     * @param copy        copy the values during combine
     *
     * @return combined grid.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl combineGrids(FieldImpl grid1, FieldImpl grid2,
                                         int samplingMode, int errorMode,
                                         boolean flatten, boolean copy)
            throws VisADException, RemoteException {

        boolean   isGrid1Sequence   = GridUtil.isTimeSequence(grid1);
        boolean   isGrid2Sequence   = GridUtil.isTimeSequence(grid2);
        boolean   isBothSequence    = (isGrid2Sequence && isGrid1Sequence);
        boolean   isOnlyOneSequence = (isGrid2Sequence || isGrid1Sequence);
        FieldImpl wvFI              = null;

        if (isBothSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, combine FlatField by FlatField,
            // and put all back together again into a new combined FieldImpl
            Set     timeSet = grid1.getDomainSet();
            Boolean ensble  = GridUtil.hasEnsemble(grid1);
            // resample to domainSet of grid1.  If they are the same, this
            // should be a no-op
            if (timeSet.getLength() > 1) {
                grid2 = (FieldImpl) grid2.resample(timeSet);
            }

            // compute each combined Field in turn; load in FieldImpl
            TupleType    rangeType = null;
            FunctionType innerType = null;
            for (int i = 0; i < timeSet.getLength(); i++) {
                FieldImpl funcFF = null;
                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) grid1.getSample(i);
                    FieldImpl sample2   = (FieldImpl) grid2.getSample(i);
                    Set       ensDomain = sample1.getDomainSet();

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);
                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerwvFF =
                            (FlatField) FieldImpl.combine(new Field[] {
                                innerField1,
                                innerField2 }, samplingMode, errorMode,
                                    flatten, copy);

                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerwvFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerwvFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerwvFF, false);

                    }
                    if (wvFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        wvFI = new FieldImpl(newFieldType, timeSet);
                    }
                    wvFI.setSample(i, funcFF, false);
                } else {
                    FlatField wvFF =
                        (FlatField) FieldImpl.combine(new Field[] {
                            (FlatField) grid1.getSample(i),
                            (FlatField) grid2.getSample(i) }, samplingMode,
                                errorMode, flatten, copy);

                    if (i == 0) {  // first time through
                        FunctionType functionType =
                            new FunctionType(
                                ((FunctionType) grid1.getType()).getDomain(),
                                wvFF.getType());

                        // make the new FieldImpl for dewpoint
                        // (but as yet empty of data)
                        wvFI = new FieldImpl(functionType, timeSet);
                    }

                    wvFI.setSample(i, wvFF, false);
                }
            }  // end isSequence

        } else if (isOnlyOneSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, combine FlatField by FlatField,
            // and put all back together again into a new combined FieldImpl
            FieldImpl sequenceGrid = (isGrid1Sequence)
                                     ? grid1
                                     : grid2;
            FieldImpl otherGrid    = (sequenceGrid == grid1)
                                     ? grid2
                                     : grid1;
            Set       timeSet      = sequenceGrid.getDomainSet();

            // compute each combined Field in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {
                FlatField wvFF = (FlatField) FieldImpl.combine((grid1
                                     == sequenceGrid)
                        ? new Field[] { (FlatField) grid1.getSample(i),
                                        (FlatField) otherGrid }
                        : new Field[] { (FlatField) otherGrid,
                                        (FlatField) grid2.getSample(
                                            i) }, samplingMode, errorMode,
                                                flatten, copy);

                if (i == 0) {  // first time through
                    FunctionType functionType =
                        new FunctionType(((FunctionType) sequenceGrid
                            .getType()).getDomain(), wvFF.getType());

                    // make the new FieldImpl for dewpoint
                    // (but as yet empty of data)
                    wvFI = new FieldImpl(functionType, timeSet);
                }

                wvFI.setSample(i, wvFF, false);
            }     // end isSequence
        } else {  // both FlatFields

            // make combinded FlatField
            wvFI = (FieldImpl) FieldImpl.combine(new Field[] { grid1,
                    grid2 }, samplingMode, errorMode, flatten, copy);
        }  // end single time

        return wvFI;

    }  // end combineGrids

    /**
     * Make a FieldImpl of wind speed scalar values from u and v components.
     *
     * @param uFI  grid of U wind component
     * @param vFI  grid of V wind component
     *
     * @return wind speed grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createWindSpeed(FieldImpl uFI, FieldImpl vFI)
            throws VisADException, RemoteException {
        return createVectorMagnitude(uFI, vFI, "WindSpeed");
    }

    /**
     * Make a FieldImpl the magnitude of the vector components
     *
     * @param uFI  grid of U wind component
     * @param vFI  grid of V wind component
     *
     * @return wind speed grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createVectorMagnitude(FieldImpl uFI,
            FieldImpl vFI)
            throws VisADException, RemoteException {
        return createVectorMagnitude(uFI, vFI, "vector_mag");
    }

    /**
     * Make a FieldImpl the magnitude of the vector components
     *
     * @param vector  vector of grid of U and V wind component
     *
     * @return wind speed grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createVectorMagnitude(FieldImpl vector)
            throws VisADException, RemoteException {
        if ( !isVector(vector)) {
            throw new VisADException("Not a vector grid "
                                     + GridUtil.getParamType(vector));
        }

        return createVectorMagnitude(getUComponent(vector),
                                     getVComponent(vector), "vector_mag");
    }

    /**
     * Make a FieldImpl the magnitude of the vector components
     *
     * @param uFI  grid of U wind component
     * @param vFI  grid of V wind component
     * @param name  name of the resulting value
     *
     * @return wind speed grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createVectorMagnitude(FieldImpl uFI,
            FieldImpl vFI, String name)
            throws VisADException, RemoteException {
        if ((uFI == null) || (vFI == null)) {
            return null;
        }

        if (name == null) {
            name = "mag";
        }

        // Compute the fieldImpl of wind speed scalar values:
        // first step is squared wind speed:
        FieldImpl wsgridFI =
            (FieldImpl) (uFI.multiply(uFI)).add((vFI.multiply(vFI))).sqrt();
        Boolean ensble = GridUtil.hasEnsemble(wsgridFI);
        Unit    spdUnit;
        if (ensble) {
            FieldImpl sample     = (FieldImpl) uFI.getSample(0);
            FlatField innerField = (FlatField) sample.getSample(0, false);
            spdUnit = innerField.getRangeUnits()[0][0];

        } else {
            spdUnit = ((FlatField) uFI.getSample(0)).getRangeUnits()[0][0];
        }
        RealType spdRT = DataUtil.makeRealType(name, spdUnit);

        // reset name which was scrambled in computations
        return GridUtil.setParamType(wsgridFI, spdRT, false);
    }  // end create vector mag

    /**
     * Make a FieldImpl the magnitude of the vector components
     *
     * @param vector  vector of grid of U and V direction component
     *
     * @return flow direction grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createVectorDirection(FieldImpl vector)
            throws VisADException, RemoteException {
        if ( !isVector(vector)) {
            throw new VisADException("Not a vector grid "
                                     + GridUtil.getParamType(vector));
        }

        FieldImpl dirFI = null;

        if (GridUtil.isTimeSequence(vector)) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, make direction FlatField by FlatField,
            // and put all back together again into a new divergence FieldImpl
            Set timeSet = vector.getDomainSet();

            // compute each divFlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {
                FlatField dirFF =
                    createVectorDirectionFF((FlatField) vector.getSample(i,
                        false));

                if ((dirFI == null) && (dirFF != null)) {
                    FunctionType dirFT =
                        new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            dirFF.getType());

                    dirFI = new FieldImpl(dirFT, timeSet);
                }

                if (dirFF != null) {
                    dirFI.setSample(i, dirFF, false, false);
                }
            }
        } else {
            dirFI = createVectorDirectionFF((FlatField) vector);
        }

        return dirFI;
    }

    /**
     * Make a FieldImpl the direction of the vector components
     *
     * @param vector  vector of grid of U and V flow component
     *
     * @return flow direction grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField createVectorDirectionFF(FlatField vector)
            throws VisADException, RemoteException {
        if ( !isVector(vector)) {
            throw new VisADException("Not a vector grid "
                                     + GridUtil.getParamType(vector));
        }

        FunctionType dirFT =
            new FunctionType(
                ((SetType) vector.getDomainSet().getType()).getDomain(),
                Direction.getRealTupleType());
        FlatField dirFF   = new FlatField(dirFT, vector.getDomainSet());
        float[][] samples = vector.getFloats(false);
        float[][] dirs    = new float[1][samples[0].length];
        float     u, v, dir;

        // compute each divFlatField in turn; load in FieldImpl
        for (int i = 0; i < samples[0].length; i++) {
            u = samples[0][i];
            v = samples[1][i];

            if (Float.isNaN(u) || Float.isNaN(v)) {
                dir = Float.NaN;
            } else if ((u == 0) && (v == 0)) {
                dir = 0;
            } else {
                dir = (float) Math.toDegrees(Math.atan2(-u, -v));

                if (dir < 0) {
                    dir += 360;
                }
            }

            dirs[0][i] = dir;
        }

        dirFF.setSamples(dirs, false);

        return dirFF;
    }

    /**
     * Make a FieldImpl the direction of the vector components
     *
     * @param uFI  grid of U flow component
     * @param vFI  grid of V flow component
     *
     * @return direction grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createVectorDirection(FieldImpl uFI,
            FieldImpl vFI)
            throws VisADException, RemoteException {
        return createVectorDirection(createFlowVectors(uFI, vFI));
    }

    /**
     * Make a FieldImpl of horizontal wind divergence from u and v components.
     *
     * @param uGrid  grid of U wind component
     * @param vGrid  grid of V wind component
     *
     * @return grid of horizontal divergence
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createHorizontalDivergence(FieldImpl uGrid,
            FieldImpl vGrid)
            throws VisADException, RemoteException {
        boolean isSequence = (GridUtil.isTimeSequence(uGrid)
                              && GridUtil.isTimeSequence(vGrid));
        FieldImpl divFI = null;

        if (isSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, make divergence FlatField by FlatField,
            // and put all back together again into a new divergence FieldImpl
            Set timeSet = uGrid.getDomainSet();

            // resample to domainSet of uGrid.  If they are the same, this
            // should be a no-op
            if (timeSet.getLength() > 1) {
                vGrid = (FieldImpl) vGrid.resample(timeSet);
            }
            Boolean      ensble    = GridUtil.hasEnsemble(uGrid);
            TupleType    rangeType = null;
            FunctionType innerType = null;
            // compute each divFlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                FieldImpl funcFF = null;
                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) uGrid.getSample(i);
                    FieldImpl sample2   = (FieldImpl) uGrid.getSample(i);
                    Set       ensDomain = sample1.getDomainSet();

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);
                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerdivFF =
                            makeHorizontalDivergence(innerField1,
                                innerField2);

                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerdivFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerdivFF, false);

                    }
                    if (divFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        divFI = new FieldImpl(newFieldType, timeSet);
                    }
                    divFI.setSample(i, funcFF, false);
                } else {
                    FlatField divFF = makeHorizontalDivergence(
                                          (FlatField) uGrid.getSample(i),
                                          (FlatField) vGrid.getSample(i));

                    if (i == 0) {  // first time through
                        FunctionType functionType =
                            new FunctionType(
                                ((FunctionType) uGrid.getType()).getDomain(),
                                divFF.getType());

                        // make the new FieldImpl for dewpoint
                        // (but as yet empty of data)
                        divFI = new FieldImpl(functionType, timeSet);
                    }

                    divFI.setSample(i, divFF, false);
                }
                // how many points computed:
            }  // end isSequence
        } else {

            // make FlatField  of saturation vapor pressure from temp
            divFI = makeHorizontalDivergence((FlatField) uGrid,
                                             (FlatField) vGrid);
        }  // end single time

        return divFI;
    }  // end createHorizontalDivergence

    /**
     * Make divergence from two FlatFields
     *
     * @param uFF   U component FlatField
     * @param vFF   V component FlatField
     * @return  FlatField of horizontal divergence
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField makeHorizontalDivergence(FlatField uFF,
            FlatField vFF)
            throws VisADException, RemoteException {
        //return (FlatField) ddx(uFF).add(ddy(vFF));
        return (FlatField) GridMath.add(GridMath.ddx(uFF), GridMath.ddy(vFF));
    }

    /**
     * Make a FieldImpl of horizontal scalar flux divergence
     *  defined as u*(dp/dx) + v*(dp/dy) + p*(du/dx + dv/dy)
     *
     *  [because the Advection() routine, returns negative
     *  the formulation is (div - adv)]
     *
     * @param paramGrid grid of scalar parameter
     * @param uGrid  grid of U wind component
     * @param vGrid  grid of V wind component
     *
     * @return grid of horizontal flux divergence of scalar
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createHorizontalFluxDivergence(
            FieldImpl paramGrid, FieldImpl uGrid, FieldImpl vGrid)
            throws VisADException, RemoteException {
        /*
        return (FieldImpl) ((paramGrid.multiply(
            createHorizontalDivergence(uGrid, vGrid))).subtract(
                createHorizontalAdvection(paramGrid, uGrid, vGrid)));
       */
        FieldImpl div  = createHorizontalDivergence(uGrid, vGrid);
        FieldImpl adv  = createHorizontalAdvection(paramGrid, uGrid, vGrid);
        FieldImpl pdiv = GridMath.multiply(paramGrid, div);
        return GridMath.subtract(pdiv, adv);

    }

    /**
     * Make a FieldImpl of horizontal scalar advection from u and v components,
     *  defined as u*(dp/dx) + v*(dp/dy)
     *
     * @param paramGrid grid of scalar parameter
     * @param uGrid  grid of U wind component
     * @param vGrid  grid of V wind component
     *
     * @return grid of horizontal advection of scalar
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createHorizontalAdvection(FieldImpl paramGrid,
            FieldImpl uGrid, FieldImpl vGrid)
            throws VisADException, RemoteException {

        // test to see if a time sequence for all three
        boolean isSequence = (GridUtil.isTimeSequence(uGrid)
                              && GridUtil.isTimeSequence(vGrid)
                              && GridUtil.isTimeSequence(paramGrid));
        FieldImpl divFI = null;

        if (isSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, make advection FlatField by FlatField,
            // and put all back together again into a new advection FieldImpl
            Set timeSet = uGrid.getDomainSet();
            Boolean ensble = (GridUtil.hasEnsemble(uGrid)
                              && GridUtil.hasEnsemble(vGrid)
                              && GridUtil.hasEnsemble(paramGrid));
            // resample to domainSet of uGrid.  If they are the same, this
            // should be a no-op
            if (timeSet.getLength() > 1) {
                vGrid = (FieldImpl) vGrid.resample(timeSet);
            }
            TupleType    rangeType = null;
            FunctionType innerType = null;
            // compute each divFlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                FieldImpl funcFF = null;
                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) uGrid.getSample(i);
                    FieldImpl sample2   = (FieldImpl) vGrid.getSample(i);
                    FieldImpl sample3   = (FieldImpl) paramGrid.getSample(i);
                    Set       ensDomain = sample1.getDomainSet();

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);
                        FlatField innerField3 =
                            (FlatField) sample3.getSample(j, false);
                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerdivFF =
                            makeHorizontalAdvection(innerField3, innerField1,
                                innerField2);

                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerdivFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerdivFF, false);

                    }
                    if (divFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        divFI = new FieldImpl(newFieldType, timeSet);
                    }
                    divFI.setSample(i, funcFF, false);
                } else {
                    FlatField divFF = makeHorizontalAdvection(
                                          (FlatField) paramGrid.getSample(i),
                                          (FlatField) uGrid.getSample(i),
                                          (FlatField) vGrid.getSample(i));

                    if (i == 0) {  // first time through
                        FunctionType functionType =
                            new FunctionType(((FunctionType) paramGrid
                                .getType()).getDomain(), divFF.getType());

                        // make the new FieldImpl for advection
                        divFI = new FieldImpl(functionType, timeSet);
                    }

                    // add data for this time step
                    divFI.setSample(i, divFF, false);
                }
            }  // end isSequence
        } else {

            // make FlatField for one time
            divFI = makeHorizontalAdvection((FlatField) paramGrid,
                                            (FlatField) uGrid,
                                            (FlatField) vGrid);
        }  // end single time

        return divFI;
    }  // end createHorizontalAdvection

    /**
     * Make advection of scalar quantity from three FlatFields (a, u, v)
     * By convention, we return -(advection)
     *
     * @param aFF   Scalar to be advected
     * @param uFF   U component FlatField
     * @param vFF   V component FlatField
     *
     * @return  FlatField of horizontal advection of quantity
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField makeHorizontalAdvection(FlatField aFF,
            FlatField uFF, FlatField vFF)
            throws VisADException, RemoteException {
        FlatField udadx = (FlatField) GridMath.multiply(GridMath.ddx(aFF),
                              uFF);
        FlatField vdady = (FlatField) GridMath.multiply(GridMath.ddy(aFF),
                              vFF);
        FlatField advgrid = (FlatField) GridMath.add(udadx, vdady);

        return (FlatField) advgrid.negate();
    }

    /**
     * Make the FieldImpl of dewpoint temperature scalar values;
     * possibly for sequence of times
     *
     * @param temperFI grid of air temperature
     * @param rhFI     grid of relative humidity
     * @return dewpoint grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createDewpoint(FieldImpl temperFI, FieldImpl rhFI)
            throws VisADException, RemoteException {
        boolean isSequence = (GridUtil.isTimeSequence(temperFI)
                              && GridUtil.isTimeSequence(rhFI));
        FieldImpl dewpointFI = null;

        if (isSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, make a dew point FlatField by FlatField,
            // and put all back together again into a new depwoint FieldImpl
            Set timeSet = temperFI.getDomainSet();

            // resample to domainSet of tempFI.  If they are the same, this
            // should be a no-op
            if (timeSet.getLength() > 1) {
                rhFI = (FieldImpl) rhFI.resample(timeSet);
            }

            Boolean ensble = (GridUtil.hasEnsemble(temperFI)
                              && GridUtil.hasEnsemble(rhFI));
            TupleType    rangeType = null;
            FunctionType innerType = null;
            // compute each dewpoint FlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) temperFI.getSample(i);
                    FieldImpl sample2   = (FieldImpl) rhFI.getSample(i);

                    Set       ensDomain = sample1.getDomainSet();
                    FieldImpl funcFF    = null;

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);

                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerdivFF =
                            makeDewpointFromTAndRH(innerField1, innerField2);

                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerdivFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerdivFF, false);

                    }
                    if (dewpointFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        dewpointFI = new FieldImpl(newFieldType, timeSet);
                    }
                    dewpointFI.setSample(i, funcFF, false);
                } else {

                    FlatField dewptFF =
                        makeDewpointFromTAndRH(
                            (FlatField) temperFI.getSample(i),
                            (FlatField) rhFI.getSample(i));

                    if (i == 0) {  // first time through
                        FunctionType functionType =
                            new FunctionType(((FunctionType) temperFI
                                .getType()).getDomain(), dewptFF.getType());

                        // make the new FieldImpl for dewpoint
                        // (but as yet empty of data)
                        dewpointFI = new FieldImpl(functionType, timeSet);
                    }

                    dewpointFI.setSample(i, dewptFF, false);
                }
            }  // end isSequence
        } else {

            // make FlatField  of saturation vapor pressure from temp
            dewpointFI = makeDewpointFromTAndRH((FlatField) temperFI,
                    (FlatField) rhFI);
        }  // end single time

        return dewpointFI;
    }  // end createDewpoint()

    /**
     * Make dewpoint from two FlatFields
     *
     * @param temp   temperature flat field
     * @param rh     relative humidity flat field
     * @return  grid of dewpoint
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField makeDewpointFromTAndRH(FlatField temp,
            FlatField rh)
            throws VisADException, RemoteException {

        // make sure we have the correct units
        // temp = (FlatField) GridUtil.setParamType(temp, Temperature.getRealType(), false);
        // make es from temperature
        FlatField esFF =
            (FlatField) SaturationVaporPressure.create((FlatField) temp);

        // make grid of actual vapor pressure
        FlatField eFF = (FlatField) (GridMath.multiply(esFF, (FlatField) rh));

        // The vapor pressure e will be the saturation vapor pressure
        // when the temperature is at the dewpoint.
        // Compute the temperature at which the vapor pressure e is the
        // Saturation vapor pressure. (grid of e values is eFF)
        return (FlatField) SaturationVaporPressure.createTemperature(eFF,
                DewPoint.getRealType());
    }

    /**
     * Make a FieldImpl of Equivalent Potential Temperature; usually in 3d grids
     * in a time series (at one or more times).
     *
     * @param temperFI grid of air temperature
     * @param rhFI     grid of relative humidity
     *
     * @return grid computed mixing ratio result grids
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createEquivalentPotentialTemperature(
            FieldImpl temperFI, FieldImpl rhFI)
            throws VisADException, RemoteException {

        FieldImpl mixingRatioFI = createMixingRatio(temperFI, rhFI);
        EquivalentPotentialTemperature ept   = null;
        FieldImpl                      eptFI = null;

        // ept.create(pressure, temperFI, mixingRatioFI);
        boolean isSequence = (GridUtil.isTimeSequence(temperFI)
                              && GridUtil.isTimeSequence(rhFI));

        // get a grid of pressure values
        Boolean ensble = (GridUtil.hasEnsemble(temperFI)
                          && GridUtil.hasEnsemble(rhFI));
        TupleType    rangeType = null;
        FunctionType innerType = null;
        FlatField    press     = null;
        if (ensble) {
            FieldImpl sample1 = (FieldImpl) temperFI.getSample(0);
            press =
                createPressureGridFromDomain((FlatField) sample1.getSample(0,
                    false));
        } else {
            press = createPressureGridFromDomain(
                (FlatField) temperFI.getSample(0));
        }


        if (isSequence) {

            // Implementation:  have to take the raw time series of data FieldImpls
            // apart, make the ept FlatField by FlatField (for each time step),
            // and put all back together again into a new FieldImpl with all times.
            Set timeSet = temperFI.getDomainSet();

            // resample RH to match domainSet (list of times) of temperFI.
            // If they are the same, this should be a no-op.
            if (timeSet.getLength() > 1) {
                rhFI = (FieldImpl) rhFI.resample(timeSet);
            }

            // compute each FlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) temperFI.getSample(i);
                    FieldImpl sample2   = (FieldImpl) rhFI.getSample(i);

                    Set       ensDomain = sample1.getDomainSet();
                    FieldImpl funcFF    = null;

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);

                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerdivFF =
                            makeDewpointFromTAndRH(innerField1, innerField2);

                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerdivFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerdivFF, false);

                    }
                    if (eptFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        eptFI = new FieldImpl(newFieldType, timeSet);
                    }
                    eptFI.setSample(i, funcFF, false);
                } else {
                    FlatField eptFF =
                        (FlatField) ept.create(press,
                            (FlatField) temperFI.getSample(i),
                            (FlatField) mixingRatioFI.getSample(i));

                    // first time through
                    if (i == 0) {
                        FunctionType functionType =
                            new FunctionType(((FunctionType) temperFI
                                .getType()).getDomain(), eptFF.getType());

                        // make the new FieldImpl for mixing ratio
                        // (but as yet empty of data)
                        eptFI = new FieldImpl(functionType, timeSet);
                    }

                    eptFI.setSample(i, eptFF, false);
                }
            }  // end isSequence

        }
        // if one time only
        else {

            // make one FlatField
            mixingRatioFI = makeMixFromTAndRHAndP((FlatField) temperFI,
                    (FlatField) rhFI,
                    createPressureGridFromDomain((FlatField) temperFI));
            eptFI = (FieldImpl) ept.create(
                createPressureGridFromDomain((FlatField) temperFI), temperFI,
                mixingRatioFI);
        }  // end single time

        return eptFI;

    }

    /**
     * Make a FieldImpl of Relative Humidity; usually in 3d grids
     * in a time series (at one or more times).
     *
     *
     * @param temperFI grid of air temperature
     * @param mixingRatioFI grid of mixing ratio
     *
     * @return grid computed Relative Humidity result grids
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createRelativeHumidity(FieldImpl temperFI,
            FieldImpl mixingRatioFI)
            throws VisADException, RemoteException {
        return createRelativeHumidity(temperFI, mixingRatioFI, false);
    }

    /**
     * Make a FieldImpl of Relative Humidity; usually in 3d grids
     * in a time series (at one or more times).
     *
     *
     * @param temperFI grid of air temperature
     * @param mixingRatioFI grid of mixing ratio
     * @param isSpecificHumidity  is the mixingRationFI really SH?
     *
     * @return grid computed Relative Humidity result grids
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createRelativeHumidity(FieldImpl temperFI,
            FieldImpl mixingRatioFI, boolean isSpecificHumidity)
            throws VisADException, RemoteException {

        FieldImpl rhFI          = null;
        FlatField mixingRatioFF = null;
        FlatField press         = null;
        boolean isSequence = (GridUtil.isTimeSequence(temperFI)
                              && GridUtil.isTimeSequence(mixingRatioFI));

        Boolean ensble = (GridUtil.hasEnsemble(temperFI)
                          && GridUtil.hasEnsemble(mixingRatioFI));
        TupleType    rangeType = null;
        FunctionType innerType = null;

        if (isSequence) {

            // get a grid of pressure values
            if (ensble) {
                FieldImpl sample1 = (FieldImpl) temperFI.getSample(0);
                press = createPressureGridFromDomain(
                    (FlatField) sample1.getSample(0, false));
            } else {
                press = createPressureGridFromDomain(
                    (FlatField) temperFI.getSample(0));
            }

            Set timeSet = temperFI.getDomainSet();

            // resample mixingRationFI to match domainSet (list of times) of temperFI.
            // If they are the same, this should be a no-op.
            if (timeSet.getLength() > 1) {
                mixingRatioFI = (FieldImpl) mixingRatioFI.resample(timeSet);
            }

            // compute each FlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                if (ensble) {
                    FieldImpl sample1 = (FieldImpl) temperFI.getSample(i);
                    FieldImpl sample2 =
                        (FieldImpl) mixingRatioFI.getSample(i);

                    Set       ensDomain = sample1.getDomainSet();
                    FieldImpl funcFF    = null;

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);

                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }

                        FlatField innerdivFF =
                            (FlatField) RelativeHumidity.create(innerField2,
                                press, innerField1);

                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerdivFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerdivFF, false);

                    }
                    if (rhFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        rhFI = new FieldImpl(newFieldType, timeSet);
                    }
                    rhFI.setSample(i, funcFF, false);

                } else {
                    mixingRatioFF = (FlatField) mixingRatioFI.getSample(i);

                    if (isSpecificHumidity) {
                        mixingRatioFF =
                            (FlatField) WaterVaporMixingRatio.create(
                                mixingRatioFF);
                    }

                    FlatField rhFF =
                        (FlatField) RelativeHumidity.create(mixingRatioFF,
                            press, temperFI.getSample(i));

                    // first time through
                    if (i == 0) {
                        FunctionType functionType =
                            new FunctionType(((FunctionType) temperFI
                                .getType()).getDomain(), rhFF.getType());

                        // make the new FieldImpl for relative humidity
                        // (but as yet empty of data)
                        rhFI = new FieldImpl(functionType, timeSet);
                    }

                    rhFI.setSample(i, rhFF, false);
                }
            }  // end isSequence
        }
        // if one time only
        else {

            // get a grid of pressure values
            press         =
                createPressureGridFromDomain((FlatField) temperFI);
            mixingRatioFF = (FlatField) mixingRatioFI;

            if (isSpecificHumidity) {
                mixingRatioFF =
                    (FlatField) WaterVaporMixingRatio.create(mixingRatioFI);
            }

            // make one FlatField
            rhFI = (FieldImpl) RelativeHumidity.create(mixingRatioFF, press,
                    temperFI);
        }  // end single time

        return rhFI;

    }

    /**
     * Make a FieldImpl of mixing ratio values for series of times
     * in general mr = (saturation mixing ratio) * (RH/100%);
     *
     * @param temperFI grid of air temperature
     * @param rhFI     grid of relative humidity
     * @return grid of computed mixing ratio
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createMixingRatio(FieldImpl temperFI,
            FieldImpl rhFI)
            throws VisADException, RemoteException {

        boolean isSequence = (GridUtil.isTimeSequence(temperFI)
                              && GridUtil.isTimeSequence(rhFI));
        FieldImpl mixFI = null;

        if (isSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, make a mixing ratio FlatField by FlatField,
            // and put all back together again into a new mixing ratio FieldImpl
            Set timeSet = temperFI.getDomainSet();

            // resample to domainSet of tempFI.  If they are the same, this
            // should be a no-op
            if (timeSet.getLength() > 1) {
                rhFI = (FieldImpl) rhFI.resample(timeSet);
            }
            Boolean ensble = (GridUtil.hasEnsemble(temperFI)
                              && GridUtil.hasEnsemble(rhFI));
            TupleType    rangeType = null;
            FunctionType innerType = null;
            FlatField    press     = null;
            if (ensble) {
                FieldImpl sample1 = (FieldImpl) temperFI.getSample(0);
                press = createPressureGridFromDomain(
                    (FlatField) sample1.getSample(0, false));
            } else {
                press = createPressureGridFromDomain(
                    (FlatField) temperFI.getSample(0));
            }
            // compute each mixing ratio FlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) temperFI.getSample(i);
                    FieldImpl sample2   = (FieldImpl) rhFI.getSample(i);

                    Set       ensDomain = sample1.getDomainSet();
                    FieldImpl funcFF    = null;

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);

                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerdivFF =
                            makeMixFromTAndRHAndP(innerField1, innerField2,
                                press);


                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerdivFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerdivFF, false);

                    }
                    if (mixFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        mixFI = new FieldImpl(newFieldType, timeSet);
                    }
                    mixFI.setSample(i, funcFF, false);
                } else {
                    FlatField mixFF = makeMixFromTAndRHAndP(
                                          (FlatField) temperFI.getSample(i),
                                          (FlatField) rhFI.getSample(i),
                                          press);

                    if (i == 0) {  // first time through
                        FunctionType functionType =
                            new FunctionType(((FunctionType) temperFI
                                .getType()).getDomain(), mixFF.getType());

                        // make the new FieldImpl for mixing ratio
                        // (but as yet empty of data)
                        mixFI = new FieldImpl(functionType, timeSet);
                    }

                    mixFI.setSample(i, mixFF, false);
                }
            }  // end isSequence
        } else {

            // single time
            mixFI = makeMixFromTAndRHAndP(
                (FlatField) temperFI, (FlatField) rhFI,
                createPressureGridFromDomain((FlatField) temperFI));
        }  // end single time

        return mixFI;

    }  // end make mixing_ratio fieldimpl

    /**
     * Make mixingRatio from two FlatFields
     *
     * @param temp   temperature grid
     * @param rh     relative humidity grid
     * @param press  pressure grid
     * @return
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField makeMixFromTAndRHAndP(FlatField temp,
            FlatField rh, FlatField press)
            throws VisADException, RemoteException {

        // use above to calculate Saturation Mixing Ratio
        FlatField satMR = (FlatField) SaturationMixingRatio.create(press,
                              temp);
        RealType rhRT =
            (RealType) DataUtility.getFlatRangeType(rh).getComponent(0);
        FlatField mr = (FlatField) (satMR.multiply(rh.divide(new Real(rhRT,
                           100.0))));

        return mr;
    }

    /**
     * Make a FieldImpl of potential temperature values for series of times
     * of temperature grids.  It's assumed that the spatialDomain of the
     * grid has pressure as it's vertical dimension.
     * in general theta = t * (1000/p)** .286
     *
     * @param  temperFI  one grid or a time sequence of grids of temperature
     *                   with a spatial domain that includes pressure
     *                   in vertical
     *
     * @return computed potential temperature grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createPotentialTemperature(FieldImpl temperFI)
            throws VisADException, RemoteException {
        Boolean   ensble = GridUtil.hasEnsemble(temperFI);

        FlatField press  = null;
        if (ensble) {
            FieldImpl sample1 = (FieldImpl) temperFI.getSample(0);
            press =
                createPressureGridFromDomain((FlatField) sample1.getSample(0,
                    false));
        } else {
            if (GridUtil.isTimeSequence(temperFI) == true) {
                press = createPressureGridFromDomain(
                    (FlatField) temperFI.getSample(0));
            } else {
                press = createPressureGridFromDomain((FlatField) temperFI);
            }
        }
        return createPotentialTemperature(temperFI, press);

    }

    /**
     * Make a FieldImpl of potential temperature values for series of times
     * in general theta = t * (1000/p)** .286
     *
     * @param  temperFI  grid or time sequence of grids of temperature
     * @param  pressFI   grid or time sequence of grids of pressure
     *
     * @return computed potential temperature grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createPotentialTemperature(FieldImpl temperFI,
            FieldImpl pressFI)
            throws VisADException, RemoteException {

        boolean   TisSequence = (GridUtil.isTimeSequence(temperFI));
        boolean   PisSequence = (GridUtil.isTimeSequence(pressFI));
        FieldImpl thetaFI     = null;

        if (TisSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, make a potential temperature FlatField by FlatField,
            // and put all back together again into a new theta FieldImpl
            Set timeSet = temperFI.getDomainSet();

            // resample to domainSet of tempFI.  If they are the same, this
            // should be a no-op
            if ((timeSet.getLength() > 1) && (PisSequence == true)) {
                pressFI = (FieldImpl) pressFI.resample(timeSet);
            }
            Boolean      ensble    = GridUtil.hasEnsemble(temperFI);

            TupleType    rangeType = null;
            FunctionType innerType = null;
            // compute each theta FlatField in turn; load in FieldImpl

            for (int i = 0; i < timeSet.getLength(); i++) {

                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) temperFI.getSample(i);
                    FieldImpl sample2   = (FieldImpl) ((PisSequence == true)
                            ? pressFI.getSample(0)
                            : pressFI);

                    Set       ensDomain = sample1.getDomainSet();
                    FieldImpl funcFF    = null;

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 = (FlatField) ((PisSequence
                                                    == true)
                                ? (FlatField) sample2.getSample(j, false)
                                : sample2);

                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerdivFF =
                            (FlatField) PotentialTemperature.create(
                                innerField2, innerField1);


                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerdivFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }

                        funcFF.setSample(j, innerdivFF, false);

                    }
                    if (thetaFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        thetaFI = new FieldImpl(newFieldType, timeSet);
                    }
                    thetaFI.setSample(i, funcFF, false);
                } else {
                    FlatField thetaFF =
                        (FlatField) PotentialTemperature.create((PisSequence
                            == true)
                            ? pressFI.getSample(0)
                            : pressFI, (FlatField) temperFI.getSample(i));

                    if (i == 0) {  // first time through
                        FunctionType functionType =
                            new FunctionType(((FunctionType) temperFI
                                .getType()).getDomain(), thetaFF.getType());

                        // ((FunctionType)temperFI.getType()).getDomain() = "Time"
                        // make the new FieldImpl for theta  (but as yet empty of data)
                        thetaFI = new FieldImpl(functionType, timeSet);
                    }

                    thetaFI.setSample(i, thetaFF, false);
                }
            }
        } else {

            // make FlatField  of saturation vapor pressure from temp
            thetaFI = (FlatField) PotentialTemperature.create(temperFI,
                    (PisSequence == true)
                    ? pressFI.getSample(0)
                    : pressFI);
        }  // end single time

        return thetaFI;

    }  // end make potential temperature fieldimpl

    /**
     * Make a FieldImpl of isentropic potential vorticity
     *
     * @param  temperFI  grid or time sequence of grids of temperature with
     *                   a spatial domain that includes pressure in vertical
     * @param  absvor    grid or time sequence of grids of absolute vorticity
     *
     * @return computed grid(s)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createIPV(FieldImpl temperFI, FieldImpl absvor)
            throws VisADException, RemoteException {

        Boolean   ensble = GridUtil.hasEnsemble(temperFI);

        FlatField press  = null;
        if (ensble) {
            FieldImpl sample1 = (FieldImpl) temperFI.getSample(0);
            press =
                createPressureGridFromDomain((FlatField) sample1.getSample(0,
                    false));
        } else {
            if (GridUtil.isTimeSequence(temperFI) == true) {
                press = createPressureGridFromDomain(
                    (FlatField) temperFI.getSample(0));
            } else {
                press = createPressureGridFromDomain((FlatField) temperFI);
            }
        }

        return createIPV(temperFI, press, absvor);
    }

    /**
     * Make a grid of isentropic potential vorticity
     *
     * @param  temperFI  grid or time sequence of grids of temperature
     * @param  pressFI   grid or time sequence of grids of pressures at
     *                   levels in grid
     * @param  absvor    grid or time sequence of grids of absolute vorticity
     *
     * @return computed  grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createIPV(FieldImpl temperFI, FieldImpl pressFI,
                                      FieldImpl absvor)
            throws VisADException, RemoteException {

        boolean        TisSequence = (GridUtil.isTimeSequence(temperFI));
        boolean        PisSequence = (GridUtil.isTimeSequence(pressFI));
        FieldImpl      ipvFI       = null;
        FlatField      dtdp        = null;
        RealType       pressure    = null;
        visad.Function dthdp       = null;
        FunctionType   ipvFFType   = null;

        if (TisSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, make a potential temperature FlatField by FlatField,
            // and put all back together again into a new theta FieldImpl
            Set timeSet = temperFI.getDomainSet();

            // resample to domainSet of tempFI.  If they are the same, this
            // should be a no-op
            if ((timeSet.getLength() > 1) && (PisSequence == true)) {
                pressFI = (FieldImpl) pressFI.resample(timeSet);
            }

            // will need little "g" - Earth surface's grav accel
            Real g = ucar.visad.quantities.Gravity.newReal();

            // System.out.println ("    g = "+g.getValue() );
            Boolean ensble = GridUtil.hasEnsemble(temperFI)
                             && GridUtil.hasEnsemble(absvor);

            TupleType    rangeType = null;
            FunctionType innerType = null;
            // compute each theta FlatField for time steps in turn;
            // make IPV from it and load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) temperFI.getSample(i);
                    FieldImpl sample2   = (FieldImpl) ((PisSequence == true)
                            ? pressFI.getSample(0)
                            : pressFI);
                    FieldImpl sample3   = (FieldImpl) absvor.getSample(i);

                    Set       ensDomain = sample1.getDomainSet();
                    FieldImpl funcFF    = null;

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 = (FlatField) ((PisSequence
                                                    == true)
                                ? (FlatField) sample2.getSample(j, false)
                                : sample2);
                        FlatField innerField3 =
                            (FlatField) sample3.getSample(j, false);

                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerdivFF =
                            (FlatField) PotentialTemperature.create(
                                innerField2, innerField1);

                        if (pressure == null) {

                            // first time through
                            // get the "level" coord of the grid; x,y,level; a "RealType"
                            pressure =
                                (RealType) ((FunctionType) innerdivFF
                                    .getType()).getDomain().getComponent(2);
                        }

                        if ( !Unit.canConvert(pressure.getDefaultUnit(),
                                CommonUnits.HECTOPASCAL)) {
                            throw new VisADException(
                                "Need a pressure vertical coordinate");
                        }
                        // the derivative of theta by pressure level
                        dtdp = (FlatField) GridMath.partial(innerdivFF, 2);

                        // multiply by little g - surface gravity acceleration
                        dtdp = (FlatField) dtdp.multiply(g).negate();

                        // multiply by absolute vorticity grid for this time step
                        dtdp = (FlatField) (dtdp.multiply(innerField3));

                        if (rangeType == null) {
                            Unit ipvUnit = dtdp.getRangeUnits()[0][0];
                            RealType ipvRT = DataUtil.makeRealType("ipv",
                                                 ipvUnit);
                            ipvFFType = new FunctionType(
                                ((FunctionType) dtdp.getType()).getDomain(),
                                ipvRT);

                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                ipvFFType);
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }
                        dtdp = (FlatField) dtdp.changeMathType(ipvFFType);

                        funcFF.setSample(j, dtdp, false);

                    }
                    if (ipvFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        ipvFI = new FieldImpl(newFieldType, timeSet);
                    }
                    ipvFI.setSample(i, funcFF, false);
                } else {
                    // System.out.print(" "+i);
                    // make potential temperature "theta" for this time step
                    FlatField thetaFF =
                        (FlatField) PotentialTemperature.create((PisSequence
                            == true)
                            ? pressFI.getSample(0)
                            : pressFI, (FlatField) temperFI.getSample(i));

                    if (i == 0) {

                        // first time through
                        // get the "level" coord of the grid; x,y,level; a "RealType"
                        pressure =
                            (RealType) ((FunctionType) thetaFF.getType())
                                .getDomain().getComponent(2);
                    }

                    if ( !Unit.canConvert(pressure.getDefaultUnit(),
                                          CommonUnits.HECTOPASCAL)) {
                        throw new VisADException(
                            "Need a pressure vertical coordinate");
                    }

                    // the derivative of theta by pressure level
                    dtdp = (FlatField) GridMath.partial(thetaFF, 2);

                    // multiply by little g - surface gravity acceleration
                    dtdp = (FlatField) dtdp.multiply(g).negate();

                    // multiply by absolute vorticity grid for this time step
                    dtdp = (FlatField) (dtdp.multiply(
                        ((FlatField) absvor.getSample(i))));

                    if (i == 0) {

                        // first time through, set up ipvFI
                        // make the VisAD FunctionType for the IPV; several steps
                        Unit     ipvUnit = dtdp.getRangeUnits()[0][0];
                        RealType ipvRT = DataUtil.makeRealType("ipv",
                                             ipvUnit);

                        // change unit from 0.01 s-1 K kg-1 m2 to
                        // E-6 s-1 K kg-1 m2 the "IPV Unit"
                        // ipvUnit.scale(0.0001);
                        ipvFFType = new FunctionType(
                            ((FunctionType) dtdp.getType()).getDomain(),
                            ipvRT);

                        FunctionType functionType =
                            new FunctionType(((FunctionType) temperFI
                                .getType()).getDomain(), ipvFFType);

                        // System.out.println ("    first func type = "+functionType);
                        // make the new FieldImpl for IPV (but as yet empty of data)
                        ipvFI = new FieldImpl(functionType, timeSet);
                    }

                    dtdp = (FlatField) dtdp.changeMathType(ipvFFType);

                    // set this time's ipv grid
                    ipvFI.setSample(i, dtdp, false);
                }
            }
        } else {
            System.out.println("   not GridUtil.isTimeSequence(temperFI) ");
        }

        // System.out.println(" ");

        return ipvFI;

    }  // end make IPV

    /**
     * Make a grid of isentropic potential vorticity
     *
     * @param  thetaFI  grid or time sequence of grids of theta, thetae, et
     * @param  vectorFI  grid or time sequence of grids of u and v
     *
     * @return computed  grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createPotentialVorticity(FieldImpl thetaFI,
            FieldImpl vectorFI)
            throws VisADException, RemoteException {
        FieldImpl absvorFI = createAbsoluteVorticity(getUComponent(vectorFI),
                                 getVComponent(vectorFI));
        boolean        TisSequence = (GridUtil.isTimeSequence(thetaFI));
        boolean        AisSequence = (GridUtil.isTimeSequence(absvorFI));
        FieldImpl      pvorFI      = null;
        visad.Function dthdp       = null;
        FunctionType   pvorFIType  = null;

        if (TisSequence) {  // assumes avor is also a sequence

            // Implementation:  have to take the raw data FieldImpl
            // apart, make a potential temperature FlatField by FlatField,
            // and put all back together again into a new theta FieldImpl
            Set timeSet = thetaFI.getDomainSet();

            // resample to domainSet of tempFI.  If they are the same, this
            // should be a no-op
            if ((timeSet.getLength() > 1) && (AisSequence)) {
                absvorFI = (FieldImpl) absvorFI.resample(timeSet);
            }

            Boolean ensble = GridUtil.hasEnsemble(thetaFI)
                             && GridUtil.hasEnsemble(vectorFI);
            TupleType    rangeType = null;
            FunctionType innerType = null;
            // make PVOR from it and load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) thetaFI.getSample(i);
                    FieldImpl sample2   = (FieldImpl) absvorFI.getSample(i);

                    Set       ensDomain = sample1.getDomainSet();
                    FieldImpl funcFF    = null;

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);
                        FlatField innerField2 =
                            (FlatField) sample2.getSample(j, false);

                        if ((innerField1 == null) || (innerField2 == null)) {
                            continue;
                        }
                        FlatField innerdivFF = createPVOR(innerField1,
                                                   innerField2);

                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerdivFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerdivFF.getType());
                        }
                        if (funcFF == null) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }
                        funcFF.setSample(j, innerdivFF, false);

                    }
                    if (pvorFI == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        pvorFI = new FieldImpl(newFieldType, timeSet);
                    }
                    pvorFI.setSample(i, funcFF, false);
                } else {
                    FlatField pvorFF =
                        createPVOR((FlatField) thetaFI.getSample(i),
                                   (FlatField) absvorFI.getSample(i));

                    if ((pvorFIType == null) && (pvorFF != null)) {
                        pvorFIType = new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            pvorFF.getType());

                        // System.out.println ("    first func type = "+functionType);
                        // make the new FieldImpl for IPV (but as yet empty of data)
                        pvorFI = new FieldImpl(pvorFIType, timeSet);
                    }

                    // set this time's ipv grid
                    if (pvorFF != null) {
                        pvorFI.setSample(i, pvorFF, false);
                    }
                }
            }
        } else {
            pvorFI = (FieldImpl) createPVOR((FlatField) thetaFI,
                                            (FlatField) absvorFI);
        }

        return pvorFI;
    }  // end make PVOR

    /**
     * Make a grid of potential vorticity
     *
     * @param tempFF     temperature field (temp, theta, or thetaE)
     * @param  absvor    grid or time sequence of grids of absolute vorticity
     *
     * @return computed  grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField createPVOR(FlatField tempFF, FlatField absvor)
            throws VisADException, RemoteException {

        // will need little "g" - Earth surface's grav accel
        Real g = ucar.visad.quantities.Gravity.newReal();

        // System.out.println ("    g = "+g.getValue() );

        // compute each theta FlatField for time steps in turn;
        // make IPV from it and load in FieldImpl
        // check to make sure we have a pressure domain
        RealType pressure =
            (RealType) ((FunctionType) tempFF.getType()).getDomain()
                .getComponent(2);

        if ( !Unit.canConvert(pressure.getDefaultUnit(),
                              CommonUnits.HECTOPASCAL)) {
            throw new VisADException("Need a pressure vertical coordinate");
        }

        // the derivative of theta by pressure level
        FlatField dtdp = (FlatField) GridMath.partial(tempFF, 2);

        // multiply by minus g - surface gravity acceleration
        dtdp = (FlatField) dtdp.multiply(g).negate();

        // multiply by absolute vorticity grid for this time step
        FlatField pvor = (FlatField) dtdp.multiply(absvor);

        // make the VisAD FunctionType for the PVOR; several steps
        Unit     pvUnit = pvor.getRangeUnits()[0][0];
        RealType pvRT   = DataUtil.makeRealType("pvor", pvUnit);

        pvor = (FlatField) GridUtil.setParamType(pvor, pvRT, false);

        return pvor;
    }

    /**
     * Every data grid with pressure as the z coord can be used
     * to make a grid with pressure with the grid values as well
     *
     * @param ff  FlatField with pressure in grid domain
     * @return  grid of pressures
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FlatField createPressureGridFromDomain(FlatField ff)
            throws VisADException, RemoteException {

        // Make a flatfield of same size with pressure of each grid point
        // the domain set
        Gridded3DSet domainSet3D = (Gridded3DSet) ff.getDomainSet();
        int[]        lengths     = domainSet3D.getLengths();

        // make a new range realtype
        Unit[]   rangeUnits = null;
        RealType presRT     = AirPressure.getRealType();

        // get all the samples for the 3rd (Z) coordinate
        float[][] pressures = new float[][] {
            (float[]) domainSet3D.getSamples()[2].clone()
        };

        // get the domain of the FlatField's type and the units
        RealTupleType RTT   = ((FunctionType) (ff.getType())).getDomain();
        Unit          zUnit = RTT.getDefaultUnits()[2];

        if (Unit.canConvert(zUnit, CommonUnits.MILLIBAR)) {  // z is pressure

            /* TODO: figure out why we don't handle non-default unit */

            // rangeUnits = new Unit[]{ domainSet3D.getSetUnits()[2] };
            pressures = new float[][] {
                presRT.getDefaultUnit().toThis(pressures[0],
                        domainSet3D.getSetUnits()[2])
            };
            rangeUnits = new Unit[] { presRT.getDefaultUnit() };
        } else if (Unit.canConvert(zUnit, CommonUnit.meter)) {

            /*
             * pressures = Set.doubleToFloat(
             *   AirPressure.getStandardAtmosphereCS().fromReference(
             *       Set.floatToDouble(pressures),
             *       new Unit[]{ domainSet3D.getSetUnits()[2] }));
             */
            pressures = AirPressure.getStandardAtmosphereCS().fromReference(
                pressures, new Unit[] { domainSet3D.getSetUnits()[2] });
            rangeUnits = new Unit[] { presRT.getDefaultUnit() };
        } else {
            throw new VisADException(
                "can't create pressure from grid domain");
        }

        // make new function domain -> pressure
        FunctionType funct = new FunctionType(RTT, presRT);

        // make a flat field with this function and domain set.
        FlatField pressureFF = new FlatField(funct, domainSet3D,
                                             (CoordinateSystem[]) null,
                                             (Set[]) null, rangeUnits);

        // create a grid of pressure values; in this case the third positional
        // coord IS the pressure (x,y,pressure) for height indication,
        // so every position value at one level is that level's pressure value.
        pressureFF.setSamples(pressures, false);

        return pressureFF;
    }


    /**
     * Mask the values in a grid with the mask
     *
     * @param gridToMask  the grid to mask
     * @param mask        the masking grid
     * @param maskValue   the mask value
     *
     * @return  the masked grid
     *
     * @throws VisADException  Problem reading or creating VisAD data objects
     */
    public static FieldImpl mask(FieldImpl gridToMask, FieldImpl mask,
                                 float maskValue)
            throws VisADException {
        return mask(gridToMask, mask, maskValue, false);
    }

    /**
     * Mask the values in a grid with the mask
     *
     * @param gridToMask  the grid to mask
     * @param mask        the masking grid
     * @param maskValue   the mask value
     * @param resampleToMask   if true, resample to the mask domain
     *
     * @return  the masked grid
     *
     * @throws VisADException  Problem reading or creating VisAD data objects
     */
    public static FieldImpl mask(FieldImpl gridToMask, FieldImpl mask,
                                 float maskValue, boolean resampleToMask)
            throws VisADException {
        FieldImpl    newField = null;
        FunctionType maskType = null;

        try {

            // TODO: handle time series mask
            FlatField oneMask = (mask instanceof FlatField)
                                ? (FlatField) mask
                                : (FlatField) mask.getSample(0);

            if (GridUtil.isTimeSequence(gridToMask)) {
                Set timeDomain = gridToMask.getDomainSet();

                for (int timeStepIdx = 0;
                        timeStepIdx < timeDomain.getLength(); timeStepIdx++) {
                    FieldImpl sample =
                        (FieldImpl) gridToMask.getSample(timeStepIdx);

                    if (sample == null) {
                        continue;
                    }

                    FieldImpl maskFF = null;

                    if ( !GridUtil.isSequence(sample)) {
                        maskFF = maskField((FlatField) sample, oneMask,
                                           maskValue, maskType,
                                           resampleToMask);
                    } else {  // ensembles & such
                        Set ensDomain = sample.getDomainSet();

                        for (int j = 0; j < ensDomain.getLength(); j++) {
                            FlatField innerField =
                                (FlatField) sample.getSample(j, false);

                            if (innerField == null) {
                                continue;
                            }

                            FlatField innerMaskedFF =
                                maskField((FlatField) innerField, oneMask,
                                          maskValue, maskType,
                                          resampleToMask);

                            if (innerMaskedFF == null) {
                                continue;
                            }

                            if (maskType == null) {
                                maskType =
                                    (FunctionType) innerMaskedFF.getType();

                                FunctionType innerType =
                                    new FunctionType(
                                        DataUtility.getDomainType(ensDomain),
                                        maskType);

                                maskFF = new FieldImpl(innerType, ensDomain);
                            }

                            maskFF.setSample(j, innerMaskedFF, false);
                        }
                    }

                    if (maskFF == null) {
                        continue;
                    }

                    if (maskType == null) {  // if not an ensemble
                        maskType = (FunctionType) maskFF.getType();
                    }

                    if (newField == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                DataUtility.getDomainType(gridToMask),
                                maskFF.getType());

                        newField = new FieldImpl(newFieldType, timeDomain);
                    }

                    newField.setSample(timeStepIdx, maskFF, false);
                }
            } else {
                newField = maskField((FlatField) gridToMask, oneMask,
                                     maskValue, null, resampleToMask);
            }

            return newField;
        } catch (RemoteException re) {
            throw new VisADException("RemoteException in mask");
        }
    }

    /**
     * Mask the values in a grid with the mask
     *
     * @param gridToMask  the grid to mask
     * @param mask        the masking grid
     * @param maskValue   the mask value
     * @param newType     the masked grid type
     * @param resampleToMask _more_
     *
     * @return  the masked grid
     *
     * @throws VisADException  Problem reading or creating VisAD data objects
     * @throws RemoteException Java RMI problem
     */
    private static FlatField maskField(FlatField gridToMask, FlatField mask,
                                       float maskValue, FunctionType newType,
                                       boolean resampleToMask)
            throws VisADException, RemoteException {
        FlatField newField = null;

        if (newType == null) {
            TupleType rangeType =
                GridUtil.makeNewParamType(GridUtil.getParamType(gridToMask),
                                          "_mask");

            newType = new FunctionType(DataUtility.getDomainType(gridToMask),
                                       rangeType);
        }

        if (resampleToMask) {
            gridToMask = (FlatField) gridToMask.resample(mask.getDomainSet());
        } else {
            mask = (FlatField) mask.resample(gridToMask.getDomainSet());
        }
        newField = new FlatField(newType, gridToMask.getDomainSet());

        float[][] maskValues = mask.getFloats(false);
        float[][] gridValues = gridToMask.getFloats(false);
        float[][] maskedValues =
            new float[gridValues.length][gridValues[0].length];

        for (int i = 0; i < gridValues.length; i++) {
            for (int j = 0; j < gridValues[0].length; j++) {
                float value = maskValues[i][j];

                // TODO: Is NaN the same as a mask?
                maskedValues[i][j] = ((value == maskValue)
                                      || Float.isNaN(value))
                                     ? Float.NaN
                                     : gridValues[i][j];
            }
        }

        newField.setSamples(maskedValues, false);

        return newField;
    }

    /**
     * Every geo-located data grid can be used
     * to make a grid with the coriolis parameter for the grid values as well
     *
     * @param input         Any geolocated grid
     *
     * @return extracted grid of coriolis factor (2*OMEGA*sin(lat))
     *         at the grid points
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl createCoriolisGrid(FieldImpl input)
            throws VisADException, RemoteException {
        return createLatitudeGrid(input, true);
    }

    /**
     * Every geo-located data grid can be used
     * to make a grid with latitude with the grid values as well
     *
     * @param fi         Any geolocated grid
     *
     * @return extracted grid of latitudes at the grid points
     *
     * @deprecated  use createLatitudeGrid(FieldImpl)
     * @throws RemoteException
     * @throws VisADException
     */
    public static FieldImpl getLatitudeGrid(FieldImpl fi)
            throws VisADException, RemoteException {
        return createLatitudeGrid(fi);
    }

    /**
     * Every geo-located data grid can be used
     * to make a grid with latitude with the grid values as well
     *
     * @param fi         Any geolocated grid
     *
     * @return extracted grid of latitudes at the grid points
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public static FieldImpl createLatitudeGrid(FieldImpl fi)
            throws VisADException, RemoteException {
        return createLatitudeGrid(fi, false);
    }

    /**
     * Every geo-located data grid can be used
     * to make a grid with latitude with the grid values as well
     *
     * @param fi         Any geolocated grid
     * @param makeCoriolis  true to return a grid of the coriolis factor
     *
     * @return extracted grid of latitudes or coriolis at the grid points
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private static FieldImpl createLatitudeGrid(FieldImpl fi,
            boolean makeCoriolis)
            throws VisADException, RemoteException {

        SampledSet ss = GridUtil.getSpatialDomain(fi);

        // Determine the types latitude and longitude parameters.
        RealTupleType spatialDomType = ((SetType) ss.getType()).getDomain();
        RealType latType = findComponent(spatialDomType, "lat",
                                         RealType.Latitude);
        RealType lonType = findComponent(spatialDomType, "lon",
                                         RealType.Longitude);
        boolean domIsLatLon = true;

        if ((latType == null) || (lonType == null)) {
            domIsLatLon = false;

            CoordinateSystem cs = spatialDomType.getCoordinateSystem();

            if (cs == null) {
                throw new IllegalArgumentException("Not lat/lon domain "
                        + spatialDomType.toString());
            }

            RealTupleType spatialDomRefType = cs.getReference();

            latType = findComponent(spatialDomRefType, "lat",
                                    RealType.Latitude);
            lonType = findComponent(spatialDomRefType, "lon",
                                    RealType.Longitude);

            if ((latType == null) || (lonType == null)) {
                throw new IllegalArgumentException("Not lat/lon domain "
                        + spatialDomRefType.toString());
            }
        }

        FieldImpl latField = null;

        if (GridUtil.isTimeSequence(fi)) {
            Set          timeSet     = fi.getDomainSet();
            boolean isConstantDomain = GridUtil.isConstantSpatialDomain(fi);
            FlatField    latFF       = null;
            Boolean      ensble      = GridUtil.hasEnsemble(fi);
            TupleType    rangeType   = null;
            FunctionType innerType   = null;

            for (int i = 0; i < timeSet.getLength(); i++) {

                FieldImpl funcFF = null;
                if (ensble) {
                    FieldImpl sample1   = (FieldImpl) fi.getSample(i);

                    Set       ensDomain = sample1.getDomainSet();

                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField1 =
                            (FlatField) sample1.getSample(j, false);

                        if (innerField1 == null) {
                            continue;
                        }


                        // sum is dvdx - dudy  for final result at this time step
                        FlatField innerlatFF = null;
                        if ( !isConstantDomain || (latFF == null)) {
                            innerlatFF = createLatitudeBasedGrid(innerField1,
                                    latType, domIsLatLon, makeCoriolis);
                        }

                        if (rangeType == null) {
                            rangeType = GridUtil.getParamType(innerlatFF);
                            innerType = new FunctionType(
                                DataUtility.getDomainType(ensDomain),
                                innerlatFF.getType());
                        }
                        if (j == 0) {
                            funcFF = new FieldImpl(innerType, ensDomain);
                        }


                        funcFF.setSample(j, innerlatFF, false);

                    }
                    if (latField == null) {
                        FunctionType newFieldType =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                funcFF.getType());
                        latField = new FieldImpl(newFieldType, timeSet);
                    }
                    latField.setSample(i, funcFF, false);
                } else {
                    if ( !isConstantDomain || (latFF == null)) {
                        latFF = createLatitudeBasedGrid(
                            (FlatField) fi.getSample(i, false), latType,
                            domIsLatLon, makeCoriolis);
                    }

                    if (i == 0) {
                        FunctionType latFIType =
                            new FunctionType(
                                DataUtility.getDomainType(timeSet),
                                (FunctionType) latFF.getType());

                        latField = new FieldImpl(latFIType, timeSet);
                    }

                    latField.setSample(i, latFF);
                }
            }
        } else {
            latField = createLatitudeBasedGrid((FlatField) fi, latType,
                    domIsLatLon, makeCoriolis);
        }

        return latField;

    }

    /**
     * Every geo-located data grid can be used
     * to make a grid with longitude with the grid values as well
     *
     * @param fi         Any geolocated grid
     *
     * @return extracted grid of longitudes at the grid points
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public static FieldImpl createLongitudeGrid(FieldImpl fi)
            throws VisADException, RemoteException {
        SampledSet ss = GridUtil.getSpatialDomain(fi);

        // Determine the types latitude and longitude parameters.
        RealTupleType spatialDomType = ((SetType) ss.getType()).getDomain();
        RealType latType = findComponent(spatialDomType, "lat",
                                         RealType.Latitude);
        RealType lonType = findComponent(spatialDomType, "lon",
                                         RealType.Longitude);
        boolean domIsLatLon = true;

        if ((latType == null) || (lonType == null)) {
            domIsLatLon = false;

            CoordinateSystem cs = spatialDomType.getCoordinateSystem();

            if (cs == null) {
                throw new IllegalArgumentException("Not lat/lon domain "
                        + spatialDomType.toString());
            }

            RealTupleType spatialDomRefType = cs.getReference();

            latType = findComponent(spatialDomRefType, "lat",
                                    RealType.Latitude);
            lonType = findComponent(spatialDomRefType, "lon",
                                    RealType.Longitude);

            if ((latType == null) || (lonType == null)) {
                throw new IllegalArgumentException("Not lat/lon domain "
                        + spatialDomRefType.toString());
            }
        }

        FieldImpl lonField = null;

        if (GridUtil.isTimeSequence(fi)) {
            Set       timeSet          = fi.getDomainSet();
            boolean   isConstantDomain = GridUtil.isConstantSpatialDomain(fi);
            FlatField lonFF            = null;

            for (int i = 0; i < timeSet.getLength(); i++) {
                if ( !isConstantDomain || (lonFF == null)) {
                    lonFF =
                        createLongitudeBasedGrid((FlatField) fi.getSample(i,
                            false), lonType, domIsLatLon);
                }

                if (i == 0) {
                    FunctionType lonFIType =
                        new FunctionType(DataUtility.getDomainType(timeSet),
                                         (FunctionType) lonFF.getType());

                    lonField = new FieldImpl(lonFIType, timeSet);
                }

                lonField.setSample(i, lonFF);
            }
        } else {
            lonField = createLongitudeBasedGrid((FlatField) fi, lonType,
                    domIsLatLon);
        }

        return lonField;
    }

    /**
     * Every geo-located data grid can be used
     * to make a grid with latitude with the grid values as well
     *
     * @param ff         Any geolocated grid
     * @param latType     The {@link visad.RealType} of the latitude parameter.
     * @param domIsLatLon Whether or not the domain has latitude and longitude.
     *                    If false, then the latitude and longitude
     *                    are found in
     *                    the reference {@link visad.RealTupleType} of the
     *                    {@link visad.CoordinateSystem} of the domain.
     *
     * @param makeCoriolis  true to return a grid of the coriolis factor
     * @return extracted grid of latitudes or coriolis at the grid points
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private static FlatField createLatitudeBasedGrid(FlatField ff,
            RealType latType, boolean domIsLatLon, boolean makeCoriolis)
            throws VisADException, RemoteException {
        SampledSet    g3dset = GridUtil.getSpatialDomain(ff);
        RealTupleType rTT    = ((FunctionType) (ff.getType())).getDomain();
        FunctionType  FT     = null;

        if (makeCoriolis) {
            FT = new FunctionType(rTT, (RealType) EARTH_TWO_OMEGA.getType());
        } else {
            FT = new FunctionType(rTT, RealType.Latitude);
        }

        FlatField latff = new FlatField(FT, g3dset);
        float[]   lats  = null;

        if (domIsLatLon) {
            int latI = rTT.getIndex(latType);

            if (latI == -1) {
                throw new IllegalArgumentException(rTT.toString());
            }

            lats = (float[]) g3dset.getSamples(false)[latI].clone();
        } else {
            CoordinateSystem cs      = g3dset.getCoordinateSystem();
            RealTupleType    refType = cs.getReference();
            int              latI    = refType.getIndex(latType);

            if (latI == -1) {
                throw new IllegalArgumentException(refType.toString());
            }

            float[][] flatlon = cs.toReference(g3dset.getSamples(),
                                    g3dset.getSetUnits());

            lats = flatlon[latI];
        }

        if (makeCoriolis) {
            double twoOmega = EARTH_TWO_OMEGA.getValue();

            for (int i = 0; i < lats.length; i++) {
                lats[i] = (float) (Math.sin(Math.toRadians(lats[i]))
                                   * twoOmega);

                if (Math.abs(lats[i]) < 1.25E-05) {
                    lats[i] = Float.NaN;
                }
            }
        }

        latff.setSamples(new float[][] {
            lats
        }, false);

        return latff;
    }

    /**
     * Based on point data trajectory control, create all individual tracks and then merge them
     *
     * @param uFI _more_
     * @param vFI _more_
     * @param pwFI _more_
     * @param sFI _more_
     * @param s0FI _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static FieldImpl createTrajectoryGrid(FieldImpl uFI,
            FieldImpl vFI, FieldImpl pwFI, FieldImpl sFI, FieldImpl s0FI)
            throws VisADException, RemoteException, Exception {

        //s0FI is single level, single time step grid var
        float radius   = 6371000.0f;
        Set   timeSet  = sFI.getDomainSet();
        int   numTimes = timeSet.getLength();
        Unit  timeUnit = timeSet.getSetUnits()[0];
        Unit paramUnit = ((FlatField) sFI.getSample(0)).getRangeUnits()[0][0];
        FunctionType rt =
            (FunctionType) ((FlatField) sFI.getSample(0)).getType();
        String paramName = rt.getFlatRange().getRealComponents()[0].getName();

        double[]   timeVals = timeSet.getDoubles()[0];

        SampledSet domain0  = GridUtil.getSpatialDomain(s0FI);
        FieldImpl pFI =
            createPressureGridFromDomain((FlatField) pwFI.getSample(0));
        FieldImpl hPI = convertPressureToHeight(pFI);
        FieldImpl wFI = convertPressureVelocityToHeightVelocity(pwFI, hPI,
                            null);

        int numPoints = domain0.getLength();
        float[][] geoVals =
            GridUtil.getEarthLocationPoints((GriddedSet) domain0);

        float[][]  lats         = new float[numPoints][numTimes];
        float[][]  lons         = new float[numPoints][numTimes];
        float[][]  alts         = new float[numPoints][numTimes];
        double[]   ttts         = timeSet.getDoubles()[0];
        double[][] parcels      = new double[numPoints][numTimes];

        boolean    isLatLon     = GridUtil.isLatLonOrder(domain0);
        int        latIndex     = isLatLon
                                  ? 0
                                  : 1;
        int        lonIndex     = isLatLon
                                  ? 1
                                  : 0;
        boolean    haveAlt      = geoVals.length > 2;
        float[]    u            = new float[numPoints];
        float[]    v            = new float[numPoints];
        float[]    w            = new float[numPoints];
        float      s            = 0;

        boolean    normalizeLon = true;
        //first step  init  u,v, w, and s at all initial points
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            float alt = haveAlt
                        ? geoVals[2][i]
                        : 0;
            lats[i][0] = lat;
            if (normalizeLon) {
                lon = (float) LatLonPointImpl.lonNormal(lon);
            }
            lons[i][0] = lon;
            alts[i][0] = alt;

            EarthLocation el      = new EarthLocationLite(lat, lon, alt);
            FieldImpl     ssample = GridUtil.sample(sFI, el);
            Data          srt     = ssample.getSample(0);
            Real          sreal   = (srt instanceof RealTuple)
                                    ? (Real) ((RealTuple) srt).getComponent(0)
                                    : (Real) srt;
            s             = (float) sreal.getValue();
            parcels[i][0] = s;

            FieldImpl usample = GridUtil.sample(uFI, el);
            Data      urt     = usample.getSample(0);
            Real      ureal   = (urt instanceof RealTuple)
                                ? (Real) ((RealTuple) urt).getComponent(0)
                                : (Real) urt;
            u[i] = (float) ureal.getValue();

            FieldImpl vsample = GridUtil.sample(vFI, el);
            Data      vrt     = vsample.getSample(0);
            Real      vreal   = (vrt instanceof RealTuple)
                                ? (Real) ((RealTuple) vrt).getComponent(0)
                                : (Real) vrt;
            v[i] = (float) vreal.getValue();

            FieldImpl wsample = GridUtil.sample(wFI, el);
            Data      wrt     = wsample.getSample(0);
            Real      wreal   = (wrt instanceof RealTuple)
                                ? (Real) ((RealTuple) wrt).getComponent(0)
                                : (Real) wrt;
            if (wreal.isMissing()) {
                w[i] = 0;
            } else {
                w[i] = (float) wreal.getValue();
            }


        }
        // now cal every point at each following time step
        float f = 180.0f / 3.1415926f;
        for (int timeStepIdx = 1; timeStepIdx < numTimes; timeStepIdx++) {

            double delt = ttts[timeStepIdx] - ttts[timeStepIdx - 1];
            for (int i = 0; i < numPoints; i++) {
                float lat = lats[i][timeStepIdx - 1];
                float lon = lons[i][timeStepIdx - 1];
                float alt = alts[i][timeStepIdx - 1];


                float lat1 = (float) (lat
                                      + f * (v[i] * delt) / (radius + alt));
                float lon1 = (float) (lon
                                      + f * (u[i] * delt) / (radius + alt));
                float alt1 = (float) (alt + (w[i] * delt));
                if (alt1 < 0) {
                    alt1 = 0;
                }
                EarthLocation el = new EarthLocationLite(lat1, lon1, alt1);

                //GridUtil.sampleAtPoint for u, v, w
                FieldImpl usample = GridUtil.sample(uFI, el);
                FieldImpl vsample = GridUtil.sample(vFI, el);
                FieldImpl wsample = GridUtil.sample(wFI, el);
                FieldImpl ssample = GridUtil.sample(sFI, el);

                if ((usample == null) && (vsample == null)
                        && (wsample == null) && (ssample == null)) {
                    //outside the domain
                    lats[i][timeStepIdx]    = lats[i][timeStepIdx - 1];
                    lons[i][timeStepIdx]    = lons[i][timeStepIdx - 1];
                    alts[i][timeStepIdx]    = alts[i][timeStepIdx - 1];
                    parcels[i][timeStepIdx] = parcels[i][timeStepIdx - 1];
                } else {
                    lats[i][timeStepIdx] = lat1;
                    lons[i][timeStepIdx] = lon1;
                    alts[i][timeStepIdx] = alt1;

                    Data urt   = usample.getSample(timeStepIdx);
                    Real ureal = (urt instanceof RealTuple)
                                 ? (Real) ((RealTuple) urt).getComponent(0)
                                 : (Real) urt;
                    u[i] = (float) ureal.getValue();


                    Data vrt   = vsample.getSample(timeStepIdx);
                    Real vreal = (vrt instanceof RealTuple)
                                 ? (Real) ((RealTuple) vrt).getComponent(0)
                                 : (Real) vrt;
                    v[i] = (float) vreal.getValue();


                    Data wrt   = wsample.getSample(timeStepIdx);
                    Real wreal = (wrt instanceof RealTuple)
                                 ? (Real) ((RealTuple) wrt).getComponent(0)
                                 : (Real) wrt;
                    if (wreal.isMissing()) {
                        w[i] = 0;
                    } else {
                        w[i] = (float) wreal.getValue();
                    }

                    Data srt   = ssample.getSample(timeStepIdx);
                    Real sreal = (srt instanceof RealTuple)
                                 ? (Real) ((RealTuple) srt).getComponent(0)
                                 : (Real) srt;
                    s                       = (float) sreal.getValue();
                    parcels[i][timeStepIdx] = s;

                }

            }
        }
        /*
        // create FI of traj based on SSEC  NearTraj control
        int          numParcels = numPoints;  //10;
        FunctionType ft         = new FunctionType(
                              RealType.Generic,
                              new FunctionType(
                                  RealTupleType.SpatialEarth3DTuple,
                                  RealType.getRealType("paramName")));
        FieldImpl trajField = new FieldImpl(ft, new Integer1DSet(numParcels));
        FieldImpl trajTimeField =
            new FieldImpl(new FunctionType(RealType.Time,
                                           trajField.getType()), timeSet);

        for (int t = 0; t < numTimes; t++) {

            trajField = new FieldImpl(ft, new Integer1DSet(numParcels));
            for (int k = 0; k < numParcels; k++) {
                float[]   latsK    = subArray(lats[k], 0, t + 1, 0);
                float[]   lonsK    = subArray(lons[k], 0, t + 1, 0);
                float[]   altsK    = subArray(alts[k], 0, t + 1, 0);
                double[]  parcelsK = subArray(parcels[k], 0, t + 1, 0);

                FlatField fld = createSingleTraj("paramName", latsK, lonsK,
                                    altsK, parcelsK);
                trajField.setSample(k, fld);
            }

            trajTimeField.setSample(t, trajField);
        }
         */
        // create FI

        int numParcels = numPoints;  //10;
        FunctionType ft = new FunctionType(
                              RealType.Generic,
                              new FunctionType(
                                  RealTupleType.SpatialEarth3DTuple,
                                  RealType.getRealType(paramName)));

        List tracks = new ArrayList();
        for (int k = 0; k < numParcels; k++) {
            FlatField fft = createSingleTrack(paramName, lats[k], lons[k],
                                alts[k], parcels[k], timeSet, ft, paramUnit);
            tracks.add(fft);
        }

        FlatField mergedTracks = mergeTracks(tracks);
        FunctionType fiType = new FunctionType(RealType.Time,
                                  mergedTracks.getType());

        DateTime endTime = new DateTime(timeVals[numTimes - 1], timeUnit);

        FieldImpl fi =
            new FieldImpl(fiType,
                          new SingletonSet(new RealTuple(new Real[] {
                              endTime })));
        fi.setSample(0, mergedTracks, false);
        return fi;


    }

    /**
     * This is a duplication and should be removed later
     * @param datas _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public static FlatField mergeTracks(List datas) throws VisADException {

        if (datas.isEmpty()) {
            return null;
        }
        if (datas.size() == 1) {
            return (FlatField) datas.get(0);
        }
        FlatField retField = null;
        try {
            int        numObs    = 0;
            GriddedSet domainSet = null;
            FlatField  ff        = null;
            for (int i = 0; i < datas.size(); i++) {
                ff        = (FlatField) datas.get(i);
                domainSet = (GriddedSet) ff.getDomainSet();
                numObs    += domainSet.getLength();
            }
            FunctionType  retType = (FunctionType) ff.getType();
            RealTupleType rtt     = DataUtility.getFlatRangeType(ff);
            double[][] domainVals =
                new double[domainSet.getDimension()][numObs];
            float[][] values = new float[rtt.getDimension()][numObs];
            int       curPos = 0;
            for (int i = 0; i < datas.size(); i++) {
                FlatField  data    = (FlatField) datas.get(i);
                GriddedSet dset    = (GriddedSet) data.getDomainSet();
                double[][] samples = dset.getDoubles(false);
                int        length  = dset.getLength();
                float[][]  vals    = data.getFloats(false);
                for (int j = 0; j < samples.length; j++) {
                    System.arraycopy(samples[j], 0, domainVals[j], curPos,
                                     length);
                }
                for (int j = 0; j < vals.length; j++) {
                    System.arraycopy(vals[j], 0, values[j], curPos, length);
                }
                curPos += length;
            }
            // now make the new data
            // First make the domain set
            GriddedSet newDomain = null;
            if (domainSet instanceof Gridded1DDoubleSet) {
                newDomain = new Gridded1DDoubleSet(domainSet.getType(),
                        domainVals, numObs, domainSet.getCoordinateSystem(),
                        domainSet.getSetUnits(), domainSet.getSetErrors());
            } else {
                newDomain = GriddedSet.create(domainSet.getType(),
                        Set.doubleToFloat(domainVals), new int[] { numObs },
                        domainSet.getCoordinateSystem(),
                        domainSet.getSetUnits(), domainSet.getSetErrors());
            }
            retField = new FlatField(retType, newDomain);
            retField.setSamples(values, false);

        } catch (RemoteException re) {
            throw new VisADException("got RemoteException " + re);
        }
        return retField;
    }

    /**
     * _more_
     *
     * @param a _more_
     * @param start _more_
     * @param count _more_
     * @param strike _more_
     *
     * @return _more_
     */
    public static float[] subArray(float[] a, int start, int count,
                                   int strike) {
        float[] b = new float[count];
        for (int i = 0; i < count; i++) {
            b[i] = a[i + start];
        }
        return b;
    }

    /**
     * _more_
     *
     * @param a _more_
     * @param start _more_
     * @param count _more_
     * @param strike _more_
     *
     * @return _more_
     */
    public static double[] subArray(double[] a, int start, int count,
                                    int strike) {
        double[] b = new double[count];
        for (int i = 0; i < count; i++) {
            b[i] = a[i + start];
        }
        return b;
    }

    /**
     * _more_
     *
     * @param paramName _more_
     * @param lats _more_
     * @param lons _more_
     * @param alts _more_
     * @param param _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FlatField createSingleTraj(String paramName, float[] lats,
                                             float[] lons, float[] alts,
                                             double[] param)
            throws Exception {
        float trajCoords[][] = {
            lons, lats, alts
        };
        Gridded3DSet domain =
            new Gridded3DSet(RealTupleType.SpatialEarth3DTuple, trajCoords,
                             trajCoords[0].length);
        FunctionType fncType =
            new FunctionType(RealTupleType.SpatialEarth3DTuple,
                             RealType.getRealType(paramName));
        FlatField traj = new FlatField(fncType, domain);
        traj.setSamples(new double[][] {
            param
        }, false);
        return traj;
    }



    /**
     * _more_
     *
     * @param variableName _more_
     * @param lats _more_
     * @param lons _more_
     * @param alts _more_
     * @param param _more_
     * @param timeSet _more_
     * @param ft _more_
     * @param varUnit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FlatField createSingleTrack(String variableName,
            float[] lats, float[] lons, float[] alts, double[] param,
            Set timeSet, FunctionType ft, Unit varUnit)
            throws Exception {

        Unit       timeUnit     = timeSet.getSetUnits()[0];
        double[][] newRangeVals = new double[2][param.length];
        double[]   timeVals     = timeSet.getDoubles()[0];
        newRangeVals[0] = param;
        RealType timeType =
            RealType.getRealType(DataUtil.cleanName(variableName + "_"
                + timeUnit), timeUnit);
        RealType varType =
            RealType.getRealType(DataUtil.cleanName(variableName + "_"
                + varUnit), varUnit);
        RealTupleType rangeType = new RealTupleType(varType, timeType);
        newRangeVals[1] = timeVals;

        Set[] rangeSets = new Set[2];
        rangeSets[0] = new DoubleSet(new SetType(rangeType.getComponent(0)));
        rangeSets[1] = new DoubleSet(new SetType(rangeType.getComponent(1)));

        float trajCoords[][] = {
            lons, lats, alts
        };
        Gridded3DSet domain =
            new Gridded3DSet(RealTupleType.SpatialEarth3DTuple, trajCoords,
                             trajCoords[0].length);
        GriddedSet llaSet = domain;
        FunctionType newType =
            new FunctionType(((SetType) llaSet.getType()).getDomain(),
                             rangeType);
        FlatField timeTrack = new FlatField(newType, llaSet,
                                            (CoordinateSystem) null,
                                            rangeSets, new Unit[] { varUnit,
                timeUnit });
        timeTrack.setSamples(newRangeVals, false);

        return timeTrack;
    }

    /**
     * Every geo-located data grid can be used
     * to make a grid with longitude with the grid values as well
     *
     * @param ff         Any geolocated grid
     * @param lonType     The {@link visad.RealType} of the latitude parameter.
     * @param domIsLatLon Whether or not the domain has latitude and longitude.
     *                    If false, then the latitude and longitude
     *                    are found in
     *                    the reference {@link visad.RealTupleType} of the
     *                     of the domain.
     *
     * @return extracted grid of longitudez at the grid points
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private static FlatField createLongitudeBasedGrid(FlatField ff,
            RealType lonType, boolean domIsLatLon)
            throws VisADException, RemoteException {
        SampledSet    g3dset = GridUtil.getSpatialDomain(ff);
        RealTupleType rTT    = ((FunctionType) (ff.getType())).getDomain();
        FunctionType  FT     = null;


        FT = new FunctionType(rTT, RealType.Longitude);

        FlatField lonff = new FlatField(FT, g3dset);
        float[]   lons  = null;

        if (domIsLatLon) {
            int lonI = rTT.getIndex(lonType);

            if (lonI == -1) {
                throw new IllegalArgumentException(rTT.toString());
            }

            lons = (float[]) g3dset.getSamples(false)[lonI].clone();
        } else {
            CoordinateSystem cs      = g3dset.getCoordinateSystem();
            RealTupleType    refType = cs.getReference();
            int              lonI    = refType.getIndex(lonType);

            if (lonI == -1) {
                throw new IllegalArgumentException(refType.toString());
            }

            float[][] flatlon = cs.toReference(g3dset.getSamples(),
                                    g3dset.getSetUnits());

            lons = flatlon[lonI];
        }

        lonff.setSamples(new float[][] {
            lons
        }, false);

        return lonff;
    }

    /**
     * Take the partial derivative with respect to X of the given field.
     * @param grid   grid to parialize
     * @return partialized grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     * @deprecated use GridMath.ddx(FieldImpl)
     */
    public static FieldImpl ddx(FieldImpl grid)
            throws VisADException, RemoteException {
        return GridMath.partial(grid, 0);
    }

    /**
     * Take the partial derivative with respect to Y of the given field.
     * @param grid   grid to parialize
     * @return partialized grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     * @deprecated use GridMath.ddy(FieldImpl)
     */
    public static FieldImpl ddy(FieldImpl grid)
            throws VisADException, RemoteException {
        return GridMath.partial(grid, 1);
    }

    /**
     * Take the partial derivative with respect variable at the domain index.
     * @param grid   grid to parialize
     * @param domainIndex  index of variable to use  for derivative
     * @return partialized grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     * @deprecated use GridMath.partial(FieldImpl, int)
     */
    public static FieldImpl partial(FieldImpl grid, int domainIndex)
            throws VisADException, RemoteException {
        return GridMath.partial(grid, domainIndex);
    }

    /**
     * Is this a vector?
     * @param grid  grid to check
     * @return true if there is more than one component
     *
     * @throws VisADException   VisAD Error
     */
    public static boolean isVector(FieldImpl grid) throws VisADException {
        TupleType tt       = GridUtil.getParamType(grid);
        boolean   isVector = false;

        if (tt instanceof EarthVectorType) {
            isVector = true;
        } else {
            isVector = tt.getDimension() > 1;
        }

        return isVector;
    }

    /**
     * Is this a vector?
     * @param grid  grid to check
     * @return true if there is more than one component
     *
     * @throws VisADException   VisAD Error
     */
    public static boolean isScalar(FieldImpl grid) throws VisADException {
        return !isVector(grid);
    }

    /**
     * Get U component of a vector
     * @param vector  vector quantity
     * @return u (first) component or null if not a vector
     *
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl getUComponent(FieldImpl vector)
            throws VisADException {
        return getUComponent(vector, false);
    }

    /**
     * Get U component of a vector
     * @param vector  vector quantity
     * @param copy    true to copy values
     * @return u (first) component or null if not a vector
     *
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl getUComponent(FieldImpl vector, boolean copy)
            throws VisADException {
        return getComponent(vector, 0, copy);
    }

    /**
     * Get V component of a vector
     * @param vector  vector quantity
     * @return v (second) component or null if not a vector.  Does not copy
     *
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl getVComponent(FieldImpl vector)
            throws VisADException {
        return getVComponent(vector, false);
    }

    /**
     * Get V component of a vector
     * @param vector  vector quantity
     * @param copy    true to copy values
     * @return v (second) component or null if not a vector.
     *
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl getVComponent(FieldImpl vector, boolean copy)
            throws VisADException {
        return getComponent(vector, 1, copy);
    }

    /**
     * Get nth component of a vector
     * @param vector  vector quantity
     * @param index  index of component
     * @param copy    true to copy values
     * @return nth component or null in index &gt; number of components
     *
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl getComponent(FieldImpl vector, int index,
                                         boolean copy)
            throws VisADException {
        if ( !isVector(vector)) {
            return vector;
        }

        return GridUtil.getParam(vector, index, copy);
    }


    /**
     * Calculate a daily climatology from a grid of daily values for a period of time
     *
     * @param dailyData  the daily data
     * @param use366  create a 366 day climatology
     *
     * @return  the daily climatology grid
     *
     * @throws VisADException  illegal grid or problem calculating climatology
     */
    public static FieldImpl createDailyClimatology(FieldImpl dailyData,
            boolean use366)
            throws VisADException {
        FieldImpl dailyClim = null;
        Set       timeSet   = GridUtil.getTimeSet(dailyData);
        int[] jdays =
            UtcDate.convertDateTimeToJulianDay((Gridded1DSet) timeSet);
        // sanity check on the input
        int day1    = jdays[0];
        int day2    = jdays[1];
        int daydiff = day2 - day1;
        if ((day1 == day2) || (daydiff < 1) || (daydiff > 366)) {
            throw new VisADException(
                "createDailyClimatology: Input field must be daily data");
        }
        int numDays = use366
                      ? 366
                      : 365;
        Integer1DSet climTimes = new Integer1DSet(RealTupleType.Time1DTuple,
                                     numDays, null,
                                     new Unit[] { CLIMATE_UNITS }, null);
        try {
            for (int i = 0; i < numDays; i++) {
                int[] indexes = Misc.find(i + 1, jdays);  // jdays is 1 based
                if (indexes.length == 0) {
                    continue;
                }
                FlatField[] grids = new FlatField[indexes.length];
                for (int g = 0; g < indexes.length; g++) {
                    grids[g] = (FlatField) dailyData.getSample(indexes[g],
                            false);
                }
                FlatField avgGrid = GridMath.applyFunctionOverGrids(grids,
                                        GridMath.FUNC_AVERAGE);
                if (avgGrid == null) {
                    continue;
                }
                if (dailyClim == null) {
                    FunctionType ftype =
                        new FunctionType(RealTupleType.Time1DTuple,
                                         avgGrid.getType());
                    dailyClim = new FieldImpl(ftype, climTimes);
                }
                dailyClim.setSample(i, avgGrid, false, false);
            }
            if (use366) {
                FlatField firstDay = (FlatField) dailyClim.getSample(0,
                                         false);
                FlatField lastDay = (FlatField) dailyClim.getSample(364,
                                        false);
                dailyClim.setSample(
                    365,
                    GridMath.applyFunctionOverGrids(
                        new FlatField[] { firstDay,
                                          lastDay }, GridMath
                                          .FUNC_AVERAGE), false, false);
            }
        } catch (RemoteException re) {}
        return dailyClim;
    }


    /**
     * Calculate the daily anomaly
     *
     * @param dailyData  the daily data
     * @param dailyClim  the climatology
     *
     * @return  the daily anomaly
     *
     * @throws VisADException bad input or problem creating fields
     */
    public static FieldImpl calculateDailyAnomaly(FieldImpl dailyData,
            FieldImpl dailyClim)
            throws VisADException {
        return calculateDailyAnomaly(dailyData, dailyClim, false);
    }

    /**
     * Calculate the daily anomaly
     *
     * @param dailyData  the daily data
     * @param dailyClim  the climatology
     * @param asPercent  return the values as a percentage of normal (+/-)
     *
     * @return  the daily anomaly
     *
     * @throws VisADException bad input or problem creating fields
     */
    public static FieldImpl calculateDailyAnomaly(FieldImpl dailyData,
            FieldImpl dailyClim, boolean asPercent)
            throws VisADException {
        // TODO:  make some sanity checks
        Gridded1DSet dailyTimes =
            (Gridded1DSet) GridUtil.getTimeSet(dailyData);
        Gridded1DSet climTimes =
            (Gridded1DSet) GridUtil.getTimeSet(dailyClim);
        int[]     jdays     = UtcDate.convertDateTimeToJulianDay(dailyTimes);
        int[]     climdays  = UtcDate.convertDateTimeToJulianDay(climTimes);
        boolean   leapClim  = climdays.length == 366;
        int[]     leaps     = Misc.find(366, climdays);
        FieldImpl dailyAnom = null;
        try {
            for (int i = 0; i < jdays.length; i++) {
                int     climDay   = jdays[i] - 1;
                boolean isLeapDay = false;
                if (leaps.length > 0) {
                    for (int l = 0; l < leaps.length; l++) {
                        if (i == leaps[l] - 306) {
                            isLeapDay = true;
                        }
                    }
                }
                FlatField dayData  = (FlatField) dailyData.getSample(i,
                                         false);
                FlatField climData = null;
                // TODO: figure out leap years
                if (isLeapDay) {
                    if ( !leapClim) {
                        if (climDay == 60) {  // Feb 29 = average Feb 28 and March 1
                            FlatField climData1 =
                                (FlatField) dailyClim.getSample(59, false);
                            FlatField climData2 =
                                (FlatField) dailyClim.getSample(59, false);
                            climData = (FlatField) GridMath.add(climData1,
                                    climData2).subtract(new Real(2));
                            climData =
                                (FlatField) GridUtil.setParamType(climData,
                                    GridUtil.getParamType(climData1), false);
                        } else {
                            climData =
                                (FlatField) dailyClim.getSample(climDay - 1,
                                    false);
                        }
                    } else {
                        if ( !leapClim && (climDay == 365)) {
                            // no leap day in clim, average last and first
                            FlatField climData1 =
                                (FlatField) dailyClim.getSample(364, false);
                            FlatField climData2 =
                                (FlatField) dailyClim.getSample(0, false);
                            climData = (FlatField) GridMath.add(climData1,
                                    climData2).subtract(new Real(2));
                            climData =
                                (FlatField) GridUtil.setParamType(climData,
                                    GridUtil.getParamType(climData1), false);
                        } else {
                            climData =
                                (FlatField) dailyClim.getSample(climDay,
                                    false);
                        }
                    }
                } else {
                    climData = (FlatField) dailyClim.getSample(climDay,
                            false);
                }
                FlatField dayAnom;
                if (asPercent) {
                    dayAnom = (FlatField) GridMath.divide(dayData, climData);
                    dayAnom = (FlatField) GridUtil.setParamType(dayAnom,
                            ANOM_PERCENT_TYPE, false);
                    // subtract 100% to get the percent change from normal
                    dayAnom = (FlatField) dayAnom.subtract(
                        new Real(ANOM_PERCENT_TYPE, 100));
                } else {
                    dayAnom = (FlatField) GridMath.subtract(dayData,
                            climData);
                }
                if (dailyAnom == null) {
                    FunctionType ft =
                        new FunctionType(
                            ((SetType) dailyTimes.getType()).getDomain(),
                            dayAnom.getType());
                    dailyAnom = new FieldImpl(ft, dailyTimes);
                }
                dailyAnom.setSample(i, dayAnom, false, false);
            }
        } catch (RemoteException e) {}
        return dailyAnom;
    }
}

/*
 * $Id: DerivedGridFactory.java,v 1.73 2007/05/04 15:59:01 dmurray Exp $
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


package ucar.unidata.data.grid;


import ucar.unidata.data.DataUtil;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.Util;
import ucar.visad.VisADMath;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.EquivalentPotentialTemperature;
import ucar.visad.quantities.GeopotentialAltitude;
import ucar.visad.quantities.GridRelativeHorizontalWind;
import ucar.visad.quantities.Length;
import ucar.visad.quantities.PotentialTemperature;
import ucar.visad.quantities.SaturationMixingRatio;
import ucar.visad.quantities.SaturationVaporPressure;
import ucar.visad.quantities.Temperature;

import visad.*;

import visad.util.DataUtility;

import java.rmi.RemoteException;


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

    /** EARTH RADIUS */
    public static final Real EARTH_RADIUS;

    /** EARTH 2 omega */
    public static final Real EARTH_TWO_OMEGA;

    /** kilometers/degree (111) */
    private static final Real KM_PER_DEGREE;

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            DerivedGridFactory.class.getName());

    static {
        try {
            EARTH_RADIUS = new Real(Length.getRealType(), 6371000, SI.meter);
            EARTH_TWO_OMEGA = new Real(DataUtil.makeRealType("frequency",
                    SI.second.pow(-1)), 0.00014584, SI.second.pow(-1));
            Unit kmPerDegree = Util.parseUnit("km/degree");
            KM_PER_DEGREE = new Real(DataUtil.makeRealType("kmPerDegree",
                    kmPerDegree), 111.0, kmPerDegree);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex.toString());
        }
    }

    /** Default ctor; does nothing */
    public DerivedGridFactory() {}

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
        return createLayerDifference(grid, 500, 1000);
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
        return createLayerDifference(grid, Misc.parseNumber(value1),
                                     Misc.parseNumber(value2));
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

        FieldImpl first =
            GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, value1),
                                         false);
        FieldImpl second =
            GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(grid, value2),
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

            Set timeSet = uFI.getDomainSet();

            // resample to domainSet of uFI. (???)  If they are the same, this
            // should be a no-op 
            if ((timeSet.getLength() > 1) && (VisSequence == true)) {
                vFI = (FieldImpl) vFI.resample(timeSet);
            }

            // compute each rel vort  FlatField for time steps in turn; 
            // load in FieldImpl
            RealType rvRT = null;
            for (int i = 0; i < timeSet.getLength(); i++) {

                //System.out.print(" "+i); //...

                // get u and v single grids for this time step
                uFF = (FlatField) uFI.getSample(i);
                vFF = (FlatField) vFI.getSample(i);

                //System.out.print("      type of u FF is "+((FunctionType)uFF.getType()));

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

                    //System.out.println ("       rvFI func type = "+functionType);

                    // make the new FieldImpl (but as yet empty of data)
                    rvFI = new FieldImpl(functionType, timeSet);
                }
                //System.out.println ("    rv single grid range type = "+
                //                  ((FunctionType)rvFF.getType()).getRange());

                // set this time's grid 
                rvFI.setSample(i, (FlatField) GridUtil.setParamType(rvFF,
                        rvRT, false), false);
            }
        } else {
            rvFI = (FieldImpl) ddx(vFF).subtract(ddy(uFF));
        }
        Unit     rvUnit = GridUtil.getParamUnits(rvFI)[0];
        RealType rvRT   = DataUtil.makeRealType("relvorticity", rvUnit);
        return GridUtil.setParamType(rvFI, rvRT);
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
        return createRelativeVorticity(uFI, vFI);
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

        FieldImpl relVor  = createRelativeVorticity(uFI, vFI);
        FieldImpl latGrid = getLatitudeGrid(relVor);
        FieldImpl fc = (FieldImpl) latGrid.sin().multiply(EARTH_TWO_OMEGA);
        FieldImpl avFI    = (FieldImpl) relVor.add(fc);
        Unit      avUnit  = GridUtil.getParamUnits(avFI)[0];
        RealType  avRT    = DataUtil.makeRealType("absvorticity", avUnit);
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
        long      t1     = System.currentTimeMillis();
        FieldImpl result =
            (FieldImpl) GridRelativeHorizontalWind.cartesianHorizontalWind(
                uvGrid);
        long t2 = System.currentTimeMillis();
        //        System.err.println ((GridRelativeHorizontalWind.doNewCode?"New code":"Old code")  +" Time:"  +  " -- " + (t2-t1));
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

        RealType rt = GridUtil.getParamType(topoGrid).getRealComponents()[0];
        Unit     topoUnit = rt.getDefaultUnit();
        if ( !(Unit.canConvert(topoUnit,
                               GeopotentialAltitude
                                   .getGeopotentialMeter()) || Unit
                                       .canConvert(topoUnit,
                                           CommonUnit.meter))) {
            throw new VisADException("topography units " + topoUnit
                                     + " must convertible with m or gpm");
        }
        //check to make sure topo is not already in the grid
        if (MathType.findScalarType(GridUtil.getParamType(paramGrid), rt)) {
            return paramGrid;
        } else {
            // check to make sure domains are compatible
            if ( !GridUtil.isTimeSequence(topoGrid)) {
                Set paramDomain = GridUtil.getSpatialDomain(paramGrid);
                // System.out.println("param domain " +paramDomain);
                Set topoDomain = topoGrid.getDomainSet();
                // System.out.println("topo domain " +topoDomain);
                RealTupleType paramRef = null;
                RealTupleType topoRef  = null;
                if (paramDomain.getCoordinateSystem() != null) {
                    paramRef =
                        paramDomain.getCoordinateSystem().getReference();
                } else {
                    paramRef = ((SetType) paramDomain.getType()).getDomain();
                }
                if (topoDomain.getCoordinateSystem() != null) {
                    topoRef = topoDomain.getCoordinateSystem().getReference();
                } else {
                    topoRef = ((SetType) topoDomain.getType()).getDomain();
                }
                // System.out.println("paramRef = " + paramRef);
                // System.out.println("topoRef = " + topoRef);
                if ( !paramRef.equals(topoRef)) {
                    GriddedSet newSet = null;
                    if (topoDomain instanceof Linear2DSet) {
                        newSet = new Linear2DSet(new Linear1DSet[] {
                            ((Linear2DSet) topoDomain).getY(),
                            ((Linear2DSet) topoDomain).getX() });
                    } else if ((topoDomain.getCoordinateSystem() == null)
                               && (topoDomain instanceof Gridded2DSet)) {
                        int[] lengths =
                            ((GriddedSet) topoDomain).getLengths();
                        Unit[] units =
                            ((GriddedSet) topoDomain).getSetUnits();
                        ErrorEstimate[] errors =
                            ((GriddedSet) topoDomain).getSetErrors();
                        float[][] topoVals = topoDomain.getSamples(false);
                        newSet = new Gridded2DSet(paramRef, new float[][] {
                            topoVals[1], topoVals[0]
                        }, lengths[1], lengths[0], (CoordinateSystem) null,
                           new Unit[] { units[1],
                                        units[0] }, new ErrorEstimate[] {
                                            errors[1],
                                            errors[0] });
                    }
                    if (newSet != null) {
                        // System.out.println("newSet = " + newSet);
                        topoGrid = GridUtil.setSpatialDomain(topoGrid,
                                newSet);
                    }
                }
            }
            return combineGrids(paramGrid, topoGrid);
        }
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
        FieldImpl uvGrid    = combineGrids(uGrid, vGrid, true /* flatten */);
        TupleType paramType = GridUtil.getParamType(uvGrid);
        RealType[] reals = Util.ensureUnit(paramType.getRealComponents(),
                                           CommonUnit.meterPerSecond);
        RealTupleType earthVectorType = new EarthVectorType(reals[0],
                                            reals[1]);

        return GridUtil.setParamType(uvGrid, earthVectorType, false /*copy*/);
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
                                     false /*copy*/);
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

        boolean   isGrid1Sequence   = GridUtil.isTimeSequence(grid1);
        boolean   isGrid2Sequence   = GridUtil.isTimeSequence(grid2);
        boolean   isBothSequence    = (isGrid2Sequence && isGrid1Sequence);
        boolean   isOnlyOneSequence = (isGrid2Sequence || isGrid1Sequence);

        FieldImpl wvFI              = null;
        if (isBothSequence) {

            // Implementation:  have to take the raw data FieldImpl
            // apart, combine FlatField by FlatField,
            // and put all back together again into a new combined FieldImpl

            Set timeSet = grid1.getDomainSet();

            // resample to domainSet of grid1.  If they are the same, this
            // should be a no-op
            if (timeSet.getLength() > 1) {
                grid2 = (FieldImpl) grid2.resample(timeSet);
            }

            // compute each combined Field in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                FlatField wvFF = (FlatField) FieldImpl.combine(new Field[] {
                                     (FlatField) grid1.getSample(i),
                                     (FlatField) grid2.getSample(
                                         i) }, samplingMode, errorMode,
                                             flatten);
                if (i == 0) {  // first time through
                    FunctionType functionType =
                        new FunctionType(
                            ((FunctionType) grid1.getType()).getDomain(),
                            wvFF.getType());
                    // make the new FieldImpl for dewpoint 
                    //  (but as yet empty of data)
                    wvFI = new FieldImpl(functionType, timeSet);
                }

                wvFI.setSample(i, wvFF, false);
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
                                                flatten);
                if (i == 0) {  // first time through
                    FunctionType functionType =
                        new FunctionType(((FunctionType) sequenceGrid
                            .getType()).getDomain(), wvFF.getType());
                    // make the new FieldImpl for dewpoint 
                    //  (but as yet empty of data)
                    wvFI = new FieldImpl(functionType, timeSet);
                }

                wvFI.setSample(i, wvFF, false);
            }     // end isSequence
        } else {  // both FlatFields
            // make combinded FlatField
            wvFI = (FieldImpl) FieldImpl.combine(new Field[] { grid1,
                    grid2 }, samplingMode, errorMode, flatten);
        }  // end single time 

        return wvFI;
    }  //  end combineGrids

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


        if ((uFI == null) || (vFI == null)) {
            return null;
        }

        // Compute the fieldImpl of wind speed scalar values:
        //    first step is squared wind speed:
        FieldImpl wsgridFI =
            (FieldImpl) (uFI.multiply(uFI)).add((vFI.multiply(vFI))).sqrt();

        Unit spdUnit   = ((FlatField) uFI.getSample(0)).getRangeUnits()[0][0];
        RealType spdRT = DataUtil.makeRealType("WindSpeed", spdUnit);
        /*
        if (spdRT == null) {  // unit problems
            spdRT = RealType.getRealType("WindSpeed_" + spdUnit, spdUnit);
        }
        */

        // reset name which was scrambled in computations
        return GridUtil.setParamType(wsgridFI, spdRT, false);
        /*
        FunctionType wsFT = (FunctionType) wsgridFI.getType();
        FieldImpl fieldImpl =
            (FieldImpl) wsgridFI.changeMathType(
                new FunctionType(
                    wsFT.getDomain(),
                    new FunctionType(
                        ((FunctionType) wsFT.getRange()).getDomain(),
                        spdRT)));

        return fieldImpl;
        */
    }  //  end make wind speed field impl


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


            // compute each divFlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                FlatField divFF =
                    makeHorizontalDivergence((FlatField) uGrid.getSample(i),
                                             (FlatField) vGrid.getSample(i));
                if (i == 0) {  // first time through
                    FunctionType functionType =
                        new FunctionType(
                            ((FunctionType) uGrid.getType()).getDomain(),
                            divFF.getType());
                    // make the new FieldImpl for dewpoint 
                    //  (but as yet empty of data)
                    divFI = new FieldImpl(functionType, timeSet);
                }

                divFI.setSample(i, divFF, false);
                // how many points computed:
            }  // end isSequence
        } else {
            // make FlatField  of saturation vapor pressure from temp
            divFI = makeHorizontalDivergence((FlatField) uGrid,
                                             (FlatField) vGrid);
        }  // end single time 


        return divFI;
    }  //  end createHorizontalDivergence


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

        return (FlatField) ddx(uFF).add(ddy(vFF));
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

        return (FieldImpl) ((paramGrid.multiply(
            createHorizontalDivergence(uGrid, vGrid))).subtract(
                createHorizontalAdvection(paramGrid, uGrid, vGrid)));
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

            // resample to domainSet of uGrid.  If they are the same, this
            // should be a no-op
            if (timeSet.getLength() > 1) {
                vGrid = (FieldImpl) vGrid.resample(timeSet);
            }


            // compute each divFlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                FlatField divFF = makeHorizontalAdvection(
                                      (FlatField) paramGrid.getSample(i),
                                      (FlatField) uGrid.getSample(i),
                                      (FlatField) vGrid.getSample(i));

                if (i == 0) {  // first time through
                    FunctionType functionType =
                        new FunctionType(
                            ((FunctionType) paramGrid.getType()).getDomain(),
                            divFF.getType());
                    // make the new FieldImpl for advection
                    divFI = new FieldImpl(functionType, timeSet);
                }

                // add data for this time step
                divFI.setSample(i, divFF, false);
            }  // end isSequence
        } else {
            // make FlatField for one time 
            divFI = makeHorizontalAdvection((FlatField) paramGrid,
                                            (FlatField) uGrid,
                                            (FlatField) vGrid);
        }  // end single time 

        return divFI;
    }  //  end createHorizontalAdvection

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

        FlatField advgrid =
            (FlatField) (ddx(aFF).multiply(uFF)).add(ddy(aFF).multiply(vFF));

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


            // compute each dewpoint FlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                FlatField dewptFF =
                    makeDewpointFromTAndRH((FlatField) temperFI.getSample(i),
                                           (FlatField) rhFI.getSample(i));
                if (i == 0) {  // first time through
                    FunctionType functionType =
                        new FunctionType(
                            ((FunctionType) temperFI.getType()).getDomain(),
                            dewptFF.getType());
                    // make the new FieldImpl for dewpoint 
                    //  (but as yet empty of data)
                    dewpointFI = new FieldImpl(functionType, timeSet);
                }

                dewpointFI.setSample(i, dewptFF, false);
            }  // end isSequence
        } else {
            // make FlatField  of saturation vapor pressure from temp
            dewpointFI = makeDewpointFromTAndRH((FlatField) temperFI,
                    (FlatField) rhFI);
        }  // end single time 

        return dewpointFI;
    }  //  end createDewpoint()

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
        //temp = (FlatField) GridUtil.setParamType(temp, Temperature.getRealType(), false);
        // make es from temperature
        FlatField esFF =
            (FlatField) SaturationVaporPressure.create((FlatField) temp);

        // make grid of actual vapor pressure
        FlatField eFF = (FlatField) (esFF.multiply((FlatField) rh));

        // The vapor pressure e will be the saturation vapor pressure
        // when the temperature is at the dewpoint.
        // Compute the temperature at which the vapor pressure e is the
        // Saturation vapor pressure. (grid of e values is eFF)
        return (FlatField) SaturationVaporPressure.createTemperature(eFF);
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

        //ept.create(pressure, temperFI, mixingRatioFI);

        boolean isSequence = (GridUtil.isTimeSequence(temperFI)
                              && GridUtil.isTimeSequence(rhFI));

        // get a grid of pressure values
        FlatField press =
            createPressureGridFromDomain((FlatField) temperFI.getSample(0));

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

                FlatField eptFF = (FlatField) ept.create(press,
                                      (FlatField) temperFI.getSample(i),
                                      (FlatField) mixingRatioFI.getSample(i));

                // first time through
                if (i == 0) {
                    FunctionType functionType =
                        new FunctionType(
                            ((FunctionType) temperFI.getType()).getDomain(),
                            eptFF.getType());
                    // make the new FieldImpl for mixing ratio
                    //  (but as yet empty of data)
                    eptFI = new FieldImpl(functionType, timeSet);
                }

                eptFI.setSample(i, eptFF, false);
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

            FlatField press = createPressureGridFromDomain(
                                  (FlatField) temperFI.getSample(0));

            // compute each mixing ratio FlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                FlatField mixFF =
                    makeMixFromTAndRHAndP((FlatField) temperFI.getSample(i),
                                          (FlatField) rhFI.getSample(i),
                                          press);
                if (i == 0) {  // first time through
                    FunctionType functionType =
                        new FunctionType(
                            ((FunctionType) temperFI.getType()).getDomain(),
                            mixFF.getType());
                    // make the new FieldImpl for mixing ratio
                    //  (but as yet empty of data)
                    mixFI = new FieldImpl(functionType, timeSet);
                }

                mixFI.setSample(i, mixFF, false);
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
        return createPotentialTemperature(
            temperFI,
            createPressureGridFromDomain(
                (GridUtil.isTimeSequence(temperFI) == true)
                ? (FlatField) temperFI.getSample(0)
                : (FlatField) temperFI));
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


            // compute each theta FlatField in turn; load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                RealType pressure = null;

                FlatField thetaFF =
                    (FlatField) PotentialTemperature.create((PisSequence
                        == true)
                        ? pressFI.getSample(0)
                        : pressFI, (FlatField) temperFI.getSample(i));
                if (i == 0) {  // first time through
                    FunctionType functionType =
                        new FunctionType(
                            ((FunctionType) temperFI.getType()).getDomain(),
                            thetaFF.getType());

                    // ((FunctionType)temperFI.getType()).getDomain() = "Time"

                    // make the new FieldImpl for theta  (but as yet empty of data)
                    thetaFI = new FieldImpl(functionType, timeSet);
                }

                thetaFI.setSample(i, thetaFF, false);

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

        return createIPV(
            temperFI,
            createPressureGridFromDomain(
                (GridUtil.isTimeSequence(temperFI) == true)
                ? (FlatField) temperFI.getSample(0)
                : (FlatField) temperFI), absvor);
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
            //     System.out.println ("    g = "+g.getValue() );

            // compute each theta FlatField for time steps in turn; 
            // make IPV from it and load in FieldImpl
            for (int i = 0; i < timeSet.getLength(); i++) {

                //System.out.print(" "+i);

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

                // the derivative of theta by pressure level
                //dthdp = thetaFF.derivative(pressure, Data.NO_ERRORS);
                //dtdp  = (FlatField) dthdp;
                dthdp = partial(thetaFF, pressure);

                // multiply by little g - surface gravity acceleration
                // a minus sign is conventionally applied here, however it gives 
                // wrong (negative) results; perhaps the sign of the derivative is reversed
                // already due to VisAD coordinate directions? :
                dtdp = (FlatField) (dtdp.multiply(g));

                // multiply by absolute vorticity grid for this time step
                dtdp = (FlatField) (dtdp.multiply(
                    ((FlatField) absvor.getSample(i))));

                if (i == 0) {
                    // first time through, set up ipvFI

                    // make the VisAD FunctionType for the IPV; several steps
                    Unit     ipvUnit = dtdp.getRangeUnits()[0][0];
                    RealType ipvRT   = DataUtil.makeRealType("ipv", ipvUnit);

                    // change unit from 0.01 s-1 K kg-1 m2 to
                    // E-6 s-1 K kg-1 m2 the "IPV Unit"
                    //ipvUnit.scale(0.0001); 

                    ipvFFType = new FunctionType(
                        ((FunctionType) dtdp.getType()).getDomain(), ipvRT);

                    FunctionType functionType =
                        new FunctionType(
                            ((FunctionType) temperFI.getType()).getDomain(),
                            ipvFFType);

                    // System.out.println ("    first func type = "+functionType);

                    // make the new FieldImpl for IPV (but as yet empty of data)
                    ipvFI = new FieldImpl(functionType, timeSet);
                }
                //System.out.println ("    ipv grid type range = "+
                //                  ((FunctionType)dtdp.getType()).getRange());

                // set this time's ipv grid 
                ipvFI.setSample(
                    i, (FlatField) dtdp.changeMathType(ipvFFType), false);
            }
        } else {
            System.out.println("   not GridUtil.isTimeSequence(temperFI) ");
        }
        //System.out.println(" ");

        return ipvFI;
    }  // end make IPV


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
        //    the domain set
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
            //rangeUnits = new Unit[]{ domainSet3D.getSetUnits()[2] };
            pressures = new float[][] {
                presRT.getDefaultUnit().toThis(pressures[0],
                        domainSet3D.getSetUnits()[2])
            };
            rangeUnits = new Unit[] { presRT.getDefaultUnit() };

        } else if (Unit.canConvert(zUnit, CommonUnit.meter)) {
            /*
            pressures = Set.doubleToFloat(
                AirPressure.getStandardAtmosphereCS().fromReference(
                    Set.floatToDouble(pressures),
                    new Unit[]{ domainSet3D.getSetUnits()[2] }));
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
            Set       timeSet          = fi.getDomainSet();
            boolean   isConstantDomain = GridUtil.isConstantSpatialDomain(fi);
            FlatField latFF            = null;
            for (int i = 0; i < timeSet.getLength(); i++) {
                if ( !isConstantDomain || (latFF == null)) {
                    latFF = createLatitudeGrid((FlatField) fi.getSample(i,
                            false), latType, domIsLatLon);
                }
                if (i == 0) {
                    FunctionType latFIType =
                        new FunctionType(DataUtility.getDomainType(timeSet),
                                         (FunctionType) latFF.getType());
                    latField = new FieldImpl(latFIType, timeSet);
                }
                latField.setSample(i, latFF);
            }
        } else {
            latField = createLatitudeGrid((FlatField) fi, latType,
                                          domIsLatLon);
        }
        return latField;
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
     *                    {@link visad.CoodinateSystem} of the domain.
     *
     * @return extracted grid of latitudes at the grid points
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private static FlatField createLatitudeGrid(FlatField ff,
            RealType latType, boolean domIsLatLon)
            throws VisADException, RemoteException {

        SampledSet    g3dset = GridUtil.getSpatialDomain(ff);
        RealTupleType rTT    = ((FunctionType) (ff.getType())).getDomain();
        FunctionType  FT     = new FunctionType(rTT, RealType.Latitude);
        FlatField     latff  = new FlatField(FT, g3dset);

        if (domIsLatLon) {
            int latI = rTT.getIndex(latType);

            if (latI == -1) {
                throw new IllegalArgumentException(rTT.toString());
            }

            latff.setSamples(new float[][] {
                (float[]) g3dset.getSamples()[latI].clone()
            }, false);
        } else {
            CoordinateSystem cs      = g3dset.getCoordinateSystem();
            RealTupleType    refType = cs.getReference();
            int              latI    = refType.getIndex(latType);

            if (latI == -1) {
                throw new IllegalArgumentException(refType.toString());
            }

            float[][] flatlon = cs.toReference(g3dset.getSamples(),
                                    g3dset.getSetUnits());

            //float[][] flatlon = g3dset.doubleToFloat(latlon);

            float[][] lats = new float[][] {
                flatlon[latI]
            };

            latff.setSamples(lats, false);
        }

        return latff;
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
        }
        FlatField retField = (FlatField) fToUse.derivative(var,
                                 Data.NO_ERRORS);
        if (twoDManifold) {
            retField = (FlatField) GridUtil.setSpatialDomain(retField,
                    domain);
        }
        //System.out.println("domain = " + domain);
        //System.out.println(retField);
        if (var.equals(RealType.Longitude)
                || var.getName().toLowerCase().startsWith("lon")) {
            FlatField latGrid = (FlatField) createLatitudeGrid(retField);
            //System.out.println("latGrid:"+latGrid);
            FlatField latCosGrid = (FlatField) latGrid.cos();
            //System.out.println("latCosGrid:"+latCosGrid);
            latCosGrid = (FlatField) latCosGrid.max(new Real(.0015));
            //System.out.println("latCosMin0Grid" + latCosGrid);
            FlatField factor = (FlatField) latCosGrid.multiply(KM_PER_DEGREE);
            //System.out.println("factor:" + factor);
            //retField = (FlatField) retField.divide(KM_PER_DEGREE.multiply(latGrid.cos().multiply(KM_PER_DEGREE));
            retField = (FlatField) retField.divide(factor);
            //System.out.println(retField);
            //retField = (FlatField) retField.divide(KM_PER_DEGREE.multiply(latGrid.cos().multiply(KM_PER_DEGREE));
        } else if (var.equals(RealType.Latitude)
                   || var.getName().toLowerCase().startsWith("lat")) {
            retField = (FlatField) retField.divide(KM_PER_DEGREE);
        }
        return retField;
    }
}


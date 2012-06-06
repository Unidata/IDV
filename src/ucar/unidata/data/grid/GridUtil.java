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


import org.apache.poi.hssf.usermodel.*;

import ucar.ma2.*;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.mcidas.McIDASAreaProjection;

import ucar.unidata.data.DataUtil;

import ucar.unidata.data.point.PointObTuple;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;

import ucar.visad.ProjectionCoordinateSystem;

import ucar.visad.Util;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.bom.Radar2DCoordinateSystem;
import visad.bom.Radar3DCoordinateSystem;

import visad.data.CachedFlatField;
import visad.data.DataRange;

import visad.data.mcidas.AREACoordinateSystem
;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;
import visad.georef.MapProjection;
import visad.georef.NavigatedCoordinateSystem;
import visad.georef.TrivialMapProjection;

import visad.util.DataUtility;

import java.awt.Rectangle;

import java.awt.geom.Rectangle2D;

import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;


/**
 * Set of static methods for messing with grids.  A grid is defined
 * as a FieldImpl which has one of the following MathTypes structures:
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
 * @version $Revision: 1.112 $
 */
public class GridUtil {

    /**
     * Weighted average sampling mode
     */
    public static final int WEIGHTED_AVERAGE = Data.WEIGHTED_AVERAGE;

    /**
     * Nearest Neighbor sampling mode
     */
    public static final int NEAREST_NEIGHBOR = Data.NEAREST_NEIGHBOR;

    /**
     * No error mode
     */
    public static final int NO_ERRORS = Data.NO_ERRORS;

    /**
     * Dependent error mode
     */
    public static final int DEPENDENT_ERRORS = Data.DEPENDENT;

    /**
     * Independent error mode
     */
    public static final int INDEPENDENT_ERRORS = Data.INDEPENDENT;

    /**
     * Default sampling mode used for subsampling grids
     */
    public static final int DEFAULT_SAMPLING_MODE = WEIGHTED_AVERAGE;

    /**
     * Default error mode used for subsampling grids
     */
    public static final int DEFAULT_ERROR_MODE = NO_ERRORS;

    /**
     * function for the applyFunctionOverTime routine
     *   @deprecated use GridMath.FUNC_AVERAGE
     */
    public static final String FUNC_AVERAGE = GridMath.FUNC_AVERAGE;

    /**
     * function for the applyFunctionOverTime routine
     *   @deprecated use GridMath.FUNC_SUM
     */
    public static final String FUNC_SUM = GridMath.FUNC_SUM;

    /**
     * function for the applyFunctionOverTime routine
     *   @deprecated use GridMath.FUNC_MAX
     */
    public static final String FUNC_MAX = GridMath.FUNC_MAX;

    /**
     * function for the applyFunctionOverTime routine
     *   @deprecated use GridMath.FUNC_MIN
     */
    public static final String FUNC_MIN = GridMath.FUNC_MIN;

    /**
     * function for the timeStepFunc routine
     *   @deprecated use GridMath.FUNC_DIFFERENCE
     */
    public static final String FUNC_DIFFERENCE = GridMath.FUNC_DIFFERENCE;


    /** Five point smoother identifier */
    public static final String SMOOTH_5POINT = "SM5S";

    /** Nine point smoother identifier */
    public static final String SMOOTH_9POINT = "SM9S";

    /** Gaussian smoother identifier */
    public static final String SMOOTH_GAUSSIAN = "GWFS";

    /** Cressman smoother identifier */
    public static final String SMOOTH_CRESSMAN = "CRES";

    /** Barnes  circular smoother identifier */
    public static final String SMOOTH_CIRCULAR = "CIRC";

    /** Barnes  circular smoother identifier */
    public static final String SMOOTH_RECTANGULAR = "RECT";

    /** ensemble RealType */
    public static final RealType ENSEMBLE_TYPE =
        RealType.getRealType("Ensemble");


    /** Default ctor */
    public GridUtil() {}

    /**
     * Check to see if this field is a grid that can be handled by
     * these methods
     *
     * @param field   fieldImpl to check
     * @return  true if the MathType of the grid is compatible with the
     *               ones this class can deal with
     */
    public static boolean isGrid(FieldImpl field) {
        boolean isGrid = false;
        try {
            SampledSet ss = getSpatialDomain(field);
            isGrid = ((ss.getDimension() == 3) || (ss.getDimension() == 2));
        } catch (Exception excp) {
            isGrid = false;
        }
        return isGrid;
    }

    /**
     * See if the spatial domain of this grid is constant (ie: not
     * time varying)
     *
     * @param grid       grid to check
     *
     * @return true if the spatial domain is constant
     *
     * @throws VisADException problem getting Data object
     */
    public static boolean isConstantSpatialDomain(FieldImpl grid)
            throws VisADException {
        SampledSet ss      = getSpatialDomain(grid, 0);
        Set        timeSet = getTimeSet(grid);
        if (timeSet != null) {
            for (int i = 1; i < timeSet.getLength(); i++) {
                if (ss != getSpatialDomain(grid, i)) {
                    //System.out.println("not constant grid");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get the spatial domain for this grid.
     *
     * @param grid   grid to check
     *
     * @return  the spatial domain of the grid.  If this is a time series
     *          it is the spatial domain of the first grid in the series
     *
     * @throws VisADException  problem getting domain set
     */
    public static SampledSet getSpatialDomain(FieldImpl grid)
            throws VisADException {
        // find first non-missing grid
        if (isTimeSequence(grid)) {
            try {
                Set timeDomain = Util.getDomainSet(grid);
                for (int i = 0; i < timeDomain.getLength(); i++) {
                    FieldImpl sample = (FieldImpl) grid.getSample(i);
                    if ( !sample.isMissing()) {
                        return getSpatialDomain(grid, i);
                    }
                }
            } catch (RemoteException excp) {
                throw new VisADException("RemoteException");
            }
        }
        return getSpatialDomain(grid, 0);
    }

    /**
     * Get the spatial domain for this grid. If the grid is time sequence, this will
     * check the domain in the sequence and find the largest domain , this is very
     * useful in point data observation when returning the first time step
     * spatial domain is not big enough to cover the later time step.
     *
     * @param grid   grid to check
     *
     * @return  the spatial domain of the grid.  If this is a time series
     *          it is the spatial domain of the first grid in the series
     *
     * @throws VisADException  problem getting domain set
     */
    public static SampledSet getWholeSpatialDomain(FieldImpl grid)
            throws VisADException {
        // find first non-missing grid
        if (isTimeSequence(grid)) {
            try {
                Set        timeDomain = Util.getDomainSet(grid);
                SampledSet ss0        = null;
                int        slength    = 0;
                for (int i = 0; i < timeDomain.getLength(); i++) {
                    FieldImpl sample = (FieldImpl) grid.getSample(i);
                    if ( !sample.isMissing()) {
                        SampledSet ss = getSpatialDomain(grid, i);
                        int        ll = ss.getLength();
                        if (ll > slength) {
                            slength = ll;
                            ss0     = (SampledSet) ss.clone();
                        }

                    }
                }
                return ss0;
            } catch (RemoteException excp) {
                throw new VisADException("RemoteException");
            }
        }
        return getSpatialDomain(grid, 0);
    }

    /**
     * Get the spatial domain for this grid at the specified time step.
     *
     * @param grid   grid to check
     * @param timeIndex   timestep to check
     *
     * @return  the spatial domain of the grid at the time step.  If this
     *          is not a time series, timeIndex is ignored
     *
     * @throws VisADException  problem getting domain set
     */
    public static SampledSet getSpatialDomain(FieldImpl grid, int timeIndex)
            throws VisADException {
        SampledSet spatialDomain;
        FlatField  field = null;
        try {
            FieldImpl fi = (isSequence(grid) == true)
                           ? (FieldImpl) grid.getSample(timeIndex)
                           : (FlatField) grid;
            field         = (isSequence(fi) == true)
                            ? (FlatField) fi.getSample(0)
                            : (FlatField) fi;

            spatialDomain = (SampledSet) Util.getDomainSet(field);
        } catch (ClassCastException cce) {  //Misc.printStack("grid" + grid.getType(), 5);
            throw new IllegalArgumentException("not a known grid type "
                    + field.getDomainSet().getClass());
        } catch (RemoteException re) {
            throw new VisADException("RemoteException");
        }
        return spatialDomain;
    }

    /**
     * Change the spatial domain of a grid using the new one.  Range values
     * are not copied.
     *
     * @param grid         grid to change.
     * @param newDomain    Must have same length as current spatial domain of
     *                     grid
     *
     * @return new grid with new domain
     *
     * @throws VisADException  wrong domain length or VisAD problem.
     */
    public static FieldImpl setSpatialDomain(FieldImpl grid,
                                             SampledSet newDomain)
            throws VisADException {
        return setSpatialDomain(grid, newDomain, false);
    }

    /**
     * Change the spatial domain of a grid using the new one.
     *
     * @param grid         grid to change.
     * @param newDomain    Must have same length as current spatial domain of
     *                     grid
     * @param copy         copy values
     *
     * @return new grid with new domain
     *
     * @throws VisADException  wrong domain length or VisAD problem.
     */
    public static FieldImpl setSpatialDomain(FieldImpl grid,
                                             SampledSet newDomain,
                                             boolean copy)
            throws VisADException {

        if (getSpatialDomain(grid).getLength() != newDomain.getLength()) {
            throw new VisADException("new domain is not the right length");
        }

        TupleType    paramType = getParamType(grid);
        FunctionType rangeFT   =
            new FunctionType(((SetType) newDomain.getType()).getDomain(),
                             paramType);

        FieldImpl newFieldImpl = null;
        boolean   isSequence   = isSequence(grid);
        if (isSequence) {
            // could be (time -> (domain -> value))   or
            //          (time -> (index -> (domain -> value)))  or
            //          (index -> (domain -> value))

            try {

                Set      sequenceSet  = Util.getDomainSet(grid);
                int      numSteps     = sequenceSet.getLength();
                MathType sequenceType =
                    ((SetType) sequenceSet.getType()).getDomain();

                FieldImpl firstSample = (FieldImpl) grid.getSample(0, false);
                boolean      hasInnerSteps = isSequence(firstSample);

                FunctionType newFieldType;
                FunctionType innerFieldType = null;

                if ( !(isSequence(firstSample))) {

                    newFieldType = new FunctionType(sequenceType, rangeFT);

                } else {

                    hasInnerSteps  = true;
                    innerFieldType = new FunctionType(
                        ((FunctionType) firstSample.getType()).getDomain(),
                        rangeFT);

                    newFieldType = new FunctionType(sequenceType,
                            innerFieldType);

                }
                newFieldImpl = new FieldImpl(newFieldType, sequenceSet);

                // get each grid in turn; change domain; 
                // set result into new sequence
                for (int i = 0; i < numSteps; i++) {
                    FieldImpl data = (FieldImpl) grid.getSample(i, false);
                    FieldImpl fi;
                    if (data.isMissing()) {
                        fi = data;
                    } else {
                        if (hasInnerSteps) {
                            Set innerSet = Util.getDomainSet(data);
                            fi = new FieldImpl(innerFieldType, innerSet);
                            for (int j = 0; j < innerSet.getLength(); j++) {
                                FlatField dataFF =
                                    (FlatField) data.getSample(j, false);
                                FlatField ff = null;
                                if (dataFF.isMissing()) {
                                    ff = dataFF;
                                } else {
                                    ff = new FlatField(rangeFT, newDomain);
                                    ff.setSamples(dataFF.getFloats(copy),
                                            false);
                                }
                                fi.setSample(j, ff);
                            }
                        } else {
                            fi = new FlatField(rangeFT, newDomain);
                            ((FlatField) fi).setSamples(
                                ((FlatField) data).getFloats(copy), false);
                        }
                    }
                    newFieldImpl.setSample(i, fi);
                }
            } catch (RemoteException re) {}
        } else {  // single time
            if ( !grid.isMissing()) {
                newFieldImpl = new FlatField(rangeFT, newDomain);
                try {
                    ((FlatField) newFieldImpl).setSamples(
                        grid.getFloats(copy), false);
                } catch (RemoteException re) {}
            } else {
                newFieldImpl = grid;
            }
        }
        return newFieldImpl;
    }

    /**
     * See if the domain of the grid is a single point (only 1 x and y value).
     * May have multiple vertical values at that one point.
     *
     * @param grid  grid to check
     *
     * @return true if only one x,y value.
     *
     * @throws VisADException  problem accessing grid
     */
    public static boolean isSinglePointDomain(FieldImpl grid)
            throws VisADException {
        return isSinglePointDomain(getSpatialDomain(grid));
    }

    /**
     * See if the domain is a single point (only 1 x and y value).  May
     * have multiple vertical values at that one point
     *
     * @param ss  domain set of the grid
     *
     * @return true if only one x,y value.
     *
     * @throws VisADException  problem accessing grid
     */
    public static boolean isSinglePointDomain(SampledSet ss)
            throws VisADException {
        if (ss instanceof SingletonSet) {
            return true;
        }
        if ( !(ss instanceof GriddedSet)) {
            return false;
        }
        GriddedSet gs = (GriddedSet) ss;
        //return gs.getLength() == 1;
        int[] lengths = gs.getLengths();
        return (lengths[0] == 1) && (lengths[1] == 1);
    }

    /**
     * Check to see if this is a single grid or if it is a sequence
     * of grids.
     *
     * @param grid   grid to check
     *
     * @return  true if the domain of the grid is 1 dimensional.
     *               It is not automatically a time sequence, though.
     * @see #isTimeSequence(FieldImpl)
     */
    public static boolean isSequence(FieldImpl grid) {
        return (grid != null) && !(grid instanceof FlatField)
               && (Util.getDomainSet(grid).getDimension() == 1);
    }

    /**
     * Check to see if this is an ensemble grid
     *
     * @param grid   grid to check
     *
     * @return  true if the domain of the grid is 1 dimensional and
     *               the type is convertible with ENSEMBLE_TYPE or a sequence
     *               and the inner type has a domain of ENSEMBLE_TYPE;
     *
     * @throws VisADException  problem determining this
     */
    public static boolean hasEnsemble(FieldImpl grid) throws VisADException {
        if (isSequence(grid)) {
            if (getSequenceType(grid).equals(ENSEMBLE_TYPE)) {
                return true;
            }
            // must be time sequence
            try {
                Data inner = grid.getSample(0);
                return (inner instanceof FieldImpl)
                       && isSequence((FieldImpl) inner)
                       && getSequenceType((FieldImpl) inner).equals(
                           ENSEMBLE_TYPE);
            } catch (RemoteException re) {}
        }
        return false;
    }

    /**
     * Get the RealType of the ensemble.
     *
     * @param grid   grid to check
     *
     * @return  RealType of ensemble paramter
     *
     * @see #hasEnsemble(FieldImpl)
     *
     * @throws VisADException     unable to get the information
     */
    public static RealType getEnsembleType(FieldImpl grid)
            throws VisADException {
        if ( !hasEnsemble(grid)) {
            throw new IllegalArgumentException("grid is not an ensemble");
        }
        return ENSEMBLE_TYPE;
    }

    /**
     * Return the ensemble set for the field
     *
     * @param ensGrid  the ensemble grid
     *
     * @return the set or null if not an ensemble
     *
     * @throws VisADException  problems reading data
     */
    public static Gridded1DSet getEnsembleSet(FieldImpl ensGrid)
            throws VisADException {
        if (hasEnsemble(ensGrid)) {
            try {
                if (isTimeSequence(ensGrid)) {
                    // (Time -> (Ensemble -> (grid)))
                    FieldImpl innerGrid = (FieldImpl) ensGrid.getSample(0);
                    if (hasEnsemble(innerGrid)) {
                        return (Gridded1DSet) innerGrid.getDomainSet();
                    }
                } else {
                    // (Ensemble -> (grid))
                    return (Gridded1DSet) ensGrid.getDomainSet();
                }  // TODO:  (index -> (Ensemble -> (grid)))
            } catch (RemoteException re) {
                return null;
            }
        }
        return null;
    }


    /**
     * Check to see if this is a single grid or if it is a time sequence
     * of grids.
     *
     * @param grid   grid to check
     *
     * @return  true if the domain of the grid is 1 dimensional and
     *               the type is convertible with RealType.Time
     *
     * @throws VisADException  problem determining this
     */
    public static boolean isTimeSequence(FieldImpl grid)
            throws VisADException {
        return (isSequence(grid) &&
        //getSequenceType(grid).equalsExceptNameButUnits(RealType.Time));
        getSequenceType(grid).equals(RealType.Time));
    }

    /**
     * Get the time set from the grid.
     *
     * @param grid  grid to check
     *
     * @return set of times or null if no times.
     *
     * @throws VisADException   problem determining this
     */
    public static Set getTimeSet(FieldImpl grid) throws VisADException {
        if ( !isTimeSequence(grid)) {
            return null;
        }
        return Util.getDomainSet(grid);
    }


    /**
     * Get the list of DateTime objects from the domain of the given grid
     *
     * @param grid  grid to check
     *
     * @return list of times or null if no times.
     *
     * @throws VisADException   problem determining this
     */
    public static List<DateTime> getDateTimeList(FieldImpl grid)
            throws VisADException {
        SampledSet timeSet = (SampledSet) getTimeSet(grid);
        if (timeSet == null) {
            return null;
        }
        double[][]     times    = timeSet.getDoubles(false);
        Unit           timeUnit = timeSet.getSetUnits()[0];
        List<DateTime> result   = new ArrayList<DateTime>();
        for (int i = 0; i < timeSet.getLength(); i++) {
            result.add(new DateTime(times[0][i], timeUnit));
        }
        return result;
    }



    /**
     * Check to see if this is a navigated grid (domain can be converted to
     * lat/lon)
     *
     * @param grid   grid to check
     *
     * @return  true if the domain of the grid is in or has a reference to
     *               Latitude/Longitude
     *
     * @throws VisADException   can't create VisAD object
     */
    public static boolean isNavigated(FieldImpl grid) throws VisADException {
        return isNavigated(getSpatialDomain(grid));
    }

    /**
     * Check to see if this is a navigated domain (can be converted to
     * lat/lon)
     *
     * @param spatialSet    spatial domain of grid to check
     *
     * @return  true if the domain of the grid is in or has a reference to
     *               Latitude/Longitude
     *
     * @throws VisADException   can't create VisAD object
     */
    public static boolean isNavigated(SampledSet spatialSet)
            throws VisADException {
        RealTupleType spatialType =
            ((SetType) spatialSet.getType()).getDomain();
        RealTupleType spatialReferenceType =
            (spatialSet.getCoordinateSystem() != null)
            ? spatialSet.getCoordinateSystem().getReference()
            : null;
        return (((spatialType.getIndex(RealType.Latitude) != -1)
                && (spatialType.getIndex(RealType.Longitude)
                    != -1)) || ((spatialReferenceType != null)
                        && (spatialReferenceType.getIndex(RealType.Latitude)
                            != -1) && (spatialReferenceType.getIndex(
                                RealType.Longitude) != -1)));
    }

    /**
     * Get the navigation for this grid
     *
     * @param  grid  grid to use
     *
     * @return MapProjection for grid
     *
     * @throws VisADException   no navigation or some other error
     */
    public static MapProjection getNavigation(FieldImpl grid)
            throws VisADException {
        return getNavigation(getSpatialDomain(grid));
    }

    /**
     * Get the navigation for this spatialDomain
     *
     * @param  spatialSet  spatial set for grid
     *
     * @return MapProjection for grid
     * @throws VisADException   no navigation or some other error
     */
    public static MapProjection getNavigation(SampledSet spatialSet)
            throws VisADException {

        // don't even bother  if this isn't navigated
        if ( !isNavigated(spatialSet)) {
            throw new VisADException("Spatial domain has no navigation");
        }

        CoordinateSystem cs = spatialSet.getCoordinateSystem();

        if (cs != null) {
            if (cs instanceof CachingCoordinateSystem) {
                cs = ((CachingCoordinateSystem) cs)
                    .getCachedCoordinateSystem();
            }
            if (cs instanceof IdentityCoordinateSystem) {
                // set cs to null if identity, we'll deal with that later
                cs = null;
            } else if (cs.getDimension() == 3) {  // 3D grid
                if (cs instanceof CartesianProductCoordinateSystem) {
                    CoordinateSystem[] csArray =
                        ((CartesianProductCoordinateSystem) cs)
                            .getCoordinateSystems();
                    for (int i = 0; i < csArray.length; i++) {
                        if (csArray[i].getDimension() == 2) {
                            cs = csArray[i];
                            break;
                        }
                    }
                } else if (cs instanceof Radar3DCoordinateSystem) {
                    cs = makeRadarMapProjection(cs);

                } else if (cs instanceof EmpiricalCoordinateSystem) {

                    spatialSet =
                        ((EmpiricalCoordinateSystem) cs).getReferenceSet();
                    cs = null;

                } else if (cs instanceof NavigatedCoordinateSystem) {
                    // set cs to null, we'll deal with that later
                    cs = null;
                } else {
                    throw new VisADException(
                        "Unable to create MapProjection from "
                        + cs.getClass());
                }
                // make sure this isn't cached also
                if (cs instanceof CachingCoordinateSystem) {
                    cs = ((CachingCoordinateSystem) cs)
                        .getCachedCoordinateSystem();

                }
                if (cs instanceof IdentityCoordinateSystem) {
                    cs = null;
                }
            }

        }
        // by here, we should have a null cs or a 2D cs
        if (cs == null) {  // lat/lon or lon/lat
            cs = makeMapProjection(spatialSet);
        }
        if (cs instanceof Radar2DCoordinateSystem) {
            cs = makeRadarMapProjection(cs);
        }

        if ( !(cs instanceof MapProjection)) {
            throw new VisADException("Unable to create MapProjection from "
                                     + cs.getClass());
        }
        return (MapProjection) cs;
    }

    /**
     * Check to see if this is a navigated grid (domain can be converted to
     * lat/lon)
     *
     * @param grid   grid to check
     *
     * @return  true if the domain of the grid is in or has a reference to
     *               Latitude/Longitude
     *
     * @throws VisADException   can't get at VisAD objects
     */
    public static boolean isLatLonOrder(FieldImpl grid)
            throws VisADException {
        return isLatLonOrder(getSpatialDomain(grid));
    }

    /**
     * Check to see if this is a navigated domain (can be converted to
     * lat/lon)
     *
     * @param spatialSet   spatial domain of the grid
     *
     * @return  true if the domain of the grid is in or has a reference to
     *               Latitude/Longitude
     *
     * @throws VisADException   can't get at VisAD objects
     */
    public static boolean isLatLonOrder(SampledSet spatialSet)
            throws VisADException {
        RealTupleType spatialType =
            ((SetType) spatialSet.getType()).getDomain();
        RealTupleType spatialReferenceType =
            (spatialSet.getCoordinateSystem() != null)
            ? spatialSet.getCoordinateSystem().getReference()
            : null;
        return (spatialType.equals(RealTupleType.LatitudeLongitudeTuple)
                || spatialType
                    .equals(RealTupleType
                        .LatitudeLongitudeAltitude) || ((spatialReferenceType
                            != null) && (spatialReferenceType
                                .equals(RealTupleType
                                    .LatitudeLongitudeTuple) || spatialReferenceType
                                        .equals(RealTupleType
                                            .LatitudeLongitudeAltitude))));
    }


    /**
     * Get the RealType of the sequence.
     *
     * @param grid   grid to check
     *
     * @return  RealType of sequence paramter
     *
     * @see #isSequence(FieldImpl)
     *
     * @throws VisADException     unable to get the information
     */
    public static RealType getSequenceType(FieldImpl grid)
            throws VisADException {
        if ( !isSequence(grid)) {
            throw new IllegalArgumentException("grid is not a sequence");
        }
        return (RealType) ((SetType) Util.getDomainSet(
            grid).getType()).getDomain().getComponent(0);
    }

    /**
     * Check to see if this is a 3D grid
     *
     * @param grid    grid to check
     * @return true if the spatial domain is 3 dimensional (i.e, (x,y,z))
     *
     * @throws VisADException     unable to get the information
     */
    public static boolean is3D(FieldImpl grid) throws VisADException {
        return is3D(getSpatialDomain(grid));
    }


    /**
     * Is the gievn field a volume. It is a volume if it is a 3d grid and if the
     *      manifold dimension is 3.
     *
     * @param grid The grid
     *
     * @return Is it a volume
     *
     * @throws VisADException On badness
     */
    public static boolean isVolume(FieldImpl grid) throws VisADException {
        SampledSet domainSet = getSpatialDomain(grid);
        return is3D(domainSet) && (domainSet.getManifoldDimension() == 3);
    }

    /**
     * This samples the given grid in both time and space and trys to
     * return a Real value
     *
     * @param grid The grid
     * @param el Location
     * @param animationValue The time to sample at. If null then we
     *        just sample at the location
     * @param samplingMode mode to use
     *
     * @return Real at the given location and time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static RealTuple sampleToRealTuple(FieldImpl grid,
            EarthLocation el, Real animationValue, int samplingMode)
            throws VisADException, RemoteException {
        return sampleToRealTuple(grid, el, animationValue, samplingMode,
                                 DEFAULT_ERROR_MODE);
    }

    /**
     * This samples the given grid in both time and space and trys to return a Real value
     *
     * @param grid The grid
     * @param el Location
     * @param animationValue The time to sample at. If null then we just sample at the location
     * @param samplingMode sampling mode to use
     * @param errorMode    error mode to use
     *
     * @return Real at the given location and time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static RealTuple sampleToRealTuple(FieldImpl grid,
            EarthLocation el, Real animationValue, int samplingMode,
            int errorMode)
            throws VisADException, RemoteException {
        if (is3D(grid) && !isVolume(grid)) {
            grid = make2DGridFromSlice(grid, false);
        }

        FieldImpl sampleAtLocation;
        if (is3D(grid)) {
            sampleAtLocation = GridUtil.sample(grid, el, samplingMode,
                    errorMode);
        } else {
            sampleAtLocation = GridUtil.sample(grid, el.getLatLonPoint(),
                    samplingMode, errorMode);
        }
        Data data = ((animationValue == null)
                     ? (Data) sampleAtLocation
                     : (Data) sampleAtLocation.evaluate(animationValue,
                         samplingMode, errorMode));

        while ((data != null) && !(data instanceof RealTuple)) {
            if (data instanceof FieldImpl) {
                data = ((FieldImpl) data).getSample(0);
            } else if (data instanceof Tuple) {
                data = ((Tuple) data).getComponent(0);
            } else if (data instanceof Real) {
                data = new RealTuple(new Real[] { (Real) data });
            } else if ( !(data instanceof RealTuple)) {
                data = null;
            }
        }
        return (RealTuple) data;

    }

    /**
     * This samples the given grid in both time and space and trys to return a
     * Real value
     *
     * @param grid The grid
     * @param el Location
     * @param animationValue Time
     *
     * @return Real at the given location and time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static Real sampleToReal(FieldImpl grid, EarthLocation el,
                                    Real animationValue)
            throws VisADException, RemoteException {

        return sampleToReal(grid, el, animationValue, Data.NEAREST_NEIGHBOR);
    }

    /**
     * This samples the given grid in both time and space and trys to return a
     * Real value
     *
     * @param grid The grid
     * @param el Location
     * @param animationValue The time to sample at. If null then we just sample
     *                       at the location
     * @param samplingMode mode to use
     *
     * @return Real at the given location and time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static Real sampleToReal(FieldImpl grid, EarthLocation el,
                                    Real animationValue, int samplingMode)
            throws VisADException, RemoteException {
        return sampleToReal(grid, el, animationValue, samplingMode,
                            DEFAULT_ERROR_MODE);
    }

    /**
     * This samples the given grid in both time and space and trys to return a
     * Real value
     *
     * @param grid The grid
     * @param el Location
     * @param animationValue The time to sample at. If null then we just sample
     *                       at the location
     * @param samplingMode sampling mode to use
     * @param errorMode error mode to use
     *
     * @return Real at the given location and time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static Real sampleToReal(FieldImpl grid, EarthLocation el,
                                    Real animationValue, int samplingMode,
                                    int errorMode)
            throws VisADException, RemoteException {
        RealTuple sample = sampleToRealTuple(grid, el, animationValue,
                                             samplingMode, errorMode);
        return (sample == null)
               ? (Real) null
               : sample.getRealComponents()[0];
    }



    /**
     * Check to see if this is a 3D domain
     *
     * @param domainSet   spatial domain of the grid
     *
     * @return true if the spatial domain is 3 dimensional (i.e, (x,y,z))
     *
     * @throws VisADException     unable to get the information
     */
    public static boolean is3D(SampledSet domainSet) throws VisADException {
        return (domainSet.getDimension() == 3);
    }

    /**
     * Check to see if this is a 2D grid
     *
     * @param grid     grid to check
     *
     * @return true if the spatial domain is 2 dimensional (i.e, (x,y))
     *
     * @throws VisADException     unable to get the information
     */
    public static boolean is2D(FieldImpl grid) throws VisADException {
        return is2D(getSpatialDomain(grid));
    }

    /**
     * Check to see if this is a 2D domain
     *
     * @param domainSet    spatial domain to check
     *
     * @return true if the spatial domain is 2 dimensional (i.e, (x,y))
     *
     * @throws VisADException     unable to get the information
     */
    public static boolean is2D(SampledSet domainSet) throws VisADException {
        return domainSet.getDimension() == 2;
    }

    /**
     * Create a subset of the grid, skipping every nth point in
     * the X and Y direction.
     *
     * @param grid   grid to subset
     * @param skip   x and y skip factor
     * @return   subsampled grid
     *
     * @throws VisADException   unable to subset the grid
     */
    public static FieldImpl subset(FieldImpl grid, int skip)
            throws VisADException {
        return subset(grid, skip, skip);
    }

    /**
     * Create a subset of the grid skipping every i'th x and
     * j'th y point.
     *
     * @param grid     grid to subsample
     * @param skipx    x skip factor
     * @param skipy    y skip factor
     * @return   subsampled grid
     *
     * @throws VisADException   unable to subsample grid
     */
    public static FieldImpl subset(FieldImpl grid, int skipx, int skipy)
            throws VisADException {
        return subset(grid, skipx, skipy, 1);
    }

    /**
     * Create a subset of the grid skipping every i'th x and
     * j'th y point and k'th z point
     *
     * @param grid     grid to subsample
     * @param skipx    x skip factor
     * @param skipy    y skip factor
     * @param skipz    z skip factor
     * @return   subsampled grid
     *
     * @throws VisADException   unable to subsample grid
     */
    public static FieldImpl subset(FieldImpl grid, int skipx, int skipy,
                                   int skipz)
            throws VisADException {
        FieldImpl fi = grid;
        if ((getTimeSet(grid) == null) || isConstantSpatialDomain(grid)) {
            fi = subsetGrid(grid, skipx, skipy, skipz);
        } else {
            try {
                Set timeSet = getTimeSet(grid);
                fi = new FieldImpl((FunctionType) grid.getType(), timeSet);
                for (int i = 0; i < timeSet.getLength(); i++) {
                    FieldImpl ff    = (FieldImpl) grid.getSample(i);
                    FieldImpl slice = null;
                    if (ff.isMissing()) {
                        slice = ff;
                    } else {
                        slice = subsetGrid(ff, skipx, skipy, skipz);
                    }
                    fi.setSample(i, slice, false);
                }
            } catch (RemoteException re) {}  // won't happen - grids are local
        }
        return fi;
    }

    /**
     * Create a subset of the grid skipping every i'th x and
     * j'th y point.
     *
     * @param grid     grid to subsample
     * @param skipx    x skip factor
     * @param skipy    y skip factor
     * @return   subsampled grid
     *
     * @throws VisADException   unable to subsample grid
     */
    private static FieldImpl subsetGrid(FieldImpl grid, int skipx, int skipy)
            throws VisADException {
        return subsetGrid(grid, skipx, skipy, 1);
    }

    /**
     * Create a subset of the grid skipping every i'th x and
     * j'th y point.
     *
     * @param grid     grid to subsample
     * @param skipx    x skip factor
     * @param skipy    y skip factor
     * @param skipz    z skip factor
     * @return   subsampled grid
     *
     * @throws VisADException   unable to subsample grid
     */
    private static FieldImpl subsetGrid(FieldImpl grid, int skipx, int skipy,
                                        int skipz)
            throws VisADException {

        FieldImpl subGrid = null;
        if ((skipx == 1) && (skipy == 1) && (skipz == 1)) {
            return grid;  // no-op
        }
        GriddedSet domainSet = (GriddedSet) getSpatialDomain(grid);


        GriddedSet subDomain = null;
        if ((skipz > 1) && (domainSet.getManifoldDimension() < 3)) {
            throw new VisADException(
                "Unable to subset in Z for a 2D manifold");
        }
        if (domainSet instanceof LinearSet) {
            Linear1DSet xSet =
                ((LinearSet) domainSet).getLinear1DComponent(0);
            Linear1DSet ySet =
                ((LinearSet) domainSet).getLinear1DComponent(1);
            int         numSteps = 1 + (xSet.getLength() - 1) / skipx;
            Linear1DSet newX     = (skipx == 1)
                                   ? xSet
                                   : new Linear1DSet(xSet.getType(), xSet
                                       .getFirst(), xSet.getFirst()
                                           + (numSteps - 1) * xSet.getStep()
                                             * skipx, numSteps);
            numSteps = 1 + (ySet.getLength() - 1) / skipy;
            Linear1DSet newY = (skipy == 1)
                               ? ySet
                               : new Linear1DSet(ySet.getType(), ySet
                                   .getFirst(), ySet.getFirst()
                                       + (numSteps - 1) * ySet.getStep()
                                         * skipy, numSteps);

            if (domainSet instanceof LinearLatLonSet) {
                subDomain = new LinearLatLonSet(domainSet.getType(),
                        new Linear1DSet[] { newX,
                                            newY }, domainSet
                                            .getCoordinateSystem(), domainSet
                                            .getSetUnits(), domainSet
                                            .getSetErrors());
            } else if (domainSet instanceof Linear2DSet) {
                subDomain = new Linear2DSet(domainSet.getType(),
                                            new Linear1DSet[] { newX,
                        newY }, domainSet.getCoordinateSystem(),
                                domainSet.getSetUnits(),
                                domainSet.getSetErrors());
            } else if (domainSet instanceof Linear3DSet) {
                Linear1DSet zSet =
                    ((LinearSet) domainSet).getLinear1DComponent(2);
                if (zSet.getLength() > 1) {
                    numSteps = 1 + (zSet.getLength() - 1) / skipz;
                    Linear1DSet newZ = (skipz == 1)
                                       ? zSet
                                       : new Linear1DSet(zSet.getType(),
                                           zSet.getFirst(),
                                           zSet.getFirst()
                                           + (numSteps - 1) * zSet.getStep()
                                             * skipz, numSteps);
                    subDomain = new Linear3DSet(domainSet.getType(),
                            new Linear1DSet[] { newX,
                            newY, newZ }, domainSet.getCoordinateSystem(),
                                          domainSet.getSetUnits(),
                                          domainSet.getSetErrors());
                } else {  // single level 3D grid
                    float[][] samples    = domainSet.getSamples(false);
                    int       sizeX      = domainSet.getLength(0);
                    int       sizeY      = domainSet.getLength(1);
                    int       sizeZ      = 1;
                    int       newSizeX   = 1 + (sizeX - 1) / skipx;
                    int       newSizeY   = 1 + (sizeY - 1) / skipy;

                    float[][] subSamples =
                        new float[domainSet.getDimension()][newSizeX * newSizeY * sizeZ];
                    int l = 0;
                    for (int k = 0; k < sizeZ; k++) {
                        for (int j = 0; j < sizeY; j += skipy) {
                            for (int i = 0; i < sizeX; i += skipx) {
                                //compute stride into 1D array of 3D data
                                int elem = i + (j + k * sizeY) * sizeX;

                                subSamples[0][l] = samples[0][elem];
                                subSamples[1][l] = samples[1][elem];
                                subSamples[2][l] = samples[2][elem];
                                l++;

                            }
                        }
                    }
                    subDomain = new Gridded3DSet(domainSet.getType(),
                            subSamples, newSizeX, newSizeY,
                            domainSet.getCoordinateSystem(),
                            domainSet.getSetUnits(),
                            domainSet.getSetErrors(), false);
                }
            }

        } else {  // GriddedSet
            float[][] samples    = domainSet.getSamples(false);
            int       sizeX      = domainSet.getLength(0);
            int       sizeY      = domainSet.getLength(1);
            int       sizeZ      = (domainSet.getManifoldDimension() == 3)
                                   ? domainSet.getLength(2)
                                   : 1;
            int       newSizeX   = 1 + (sizeX - 1) / skipx;
            int       newSizeY   = 1 + (sizeY - 1) / skipy;
            int       newSizeZ   = 1 + (sizeZ - 1) / skipz;

            float[][] subSamples =
                new float[domainSet.getDimension()][newSizeX * newSizeY * newSizeZ];
            int l = 0;
            for (int k = 0; k < sizeZ; k += skipz) {
                for (int j = 0; j < sizeY; j += skipy) {
                    for (int i = 0; i < sizeX; i += skipx) {
                        //compute stride into 1D array of 3D data
                        int elem = i + (j + k * sizeY) * sizeX;

                        subSamples[0][l] = samples[0][elem];
                        subSamples[1][l] = samples[1][elem];
                        if (domainSet.getDimension() == 3) {
                            subSamples[2][l] = samples[2][elem];
                        }
                        l++;

                    }
                }
            }
            int[] newSizes = (domainSet.getManifoldDimension() == 3)
                             ? new int[] { newSizeX, newSizeY, newSizeZ }
                             : new int[] { newSizeX, newSizeY };
            try {
                subDomain = GriddedSet.create(domainSet.getType(),
                        subSamples, newSizes,
                        domainSet.getCoordinateSystem(),
                        domainSet.getSetUnits(), domainSet.getSetErrors(),
                        false, true);
            } catch (SetException se) {
                // if a SetException is thrown, then it's possible that the 
                // samples are missing or inconsistent.  Try again with 
                // test = false
                String msg = se.getMessage();
                if ((msg.indexOf("form a valid grid") >= 0)
                        || (msg.indexOf("may not be missing") >= 0)) {
                    subDomain = GriddedSet.create(domainSet.getType(),
                            subSamples, newSizes,
                            domainSet.getCoordinateSystem(),
                            domainSet.getSetUnits(),
                            domainSet.getSetErrors(), false, false);
                } else {
                    throw new VisADException(se);
                }
            }


            /*
            if (domainSet.getDimension() == 2) {
                subDomain = new Gridded2DSet(domainSet.getType(), subSamples,
                                             newSizeX, newSizeY,
                                             domainSet.getCoordinateSystem(),
                                             domainSet.getSetUnits(),
                                             domainSet.getSetErrors(), false);
                // this doesn't seem to work.
            } else if (domainSet.getManifoldDimension() == 2) {
                subDomain = new Gridded3DSet(domainSet.getType(), subSamples,
                                             newSizeX, newSizeY,
                                             domainSet.getCoordinateSystem(),
                                             domainSet.getSetUnits(),
                                             domainSet.getSetErrors(), false);
            } else if (domainSet.getDimension() == 3) {
                subDomain = new Gridded3DSet(domainSet.getType(), subSamples,
                                             newSizeX, newSizeY, newSizeZ,
                                             domainSet.getCoordinateSystem(),
                                             domainSet.getSetUnits(),
                                             domainSet.getSetErrors(), false);
            }
            */
        }

        return ((subDomain.getDimension() == 3)
                && (subDomain.getManifoldDimension() == 2))
               ? resample2DManifold(grid, subDomain, skipx, skipy)
               : resampleGrid(grid, subDomain, Data.NEAREST_NEIGHBOR);
    }

    /**
     * Slice the grid at the vertical level indictated.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  level  level to slice at.  level must have units
     *         convertible with the vertial coordinate of the spatial
     *         domain or it's reference if there is a CoordinateSystem
     *         associated with the domain.
     *
     * @return  spatial slice at level.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl sliceAtLevel(FieldImpl grid, Real level)
            throws VisADException {
        return sliceAtLevel(grid, level, DEFAULT_SAMPLING_MODE);
    }

    /**
     * Slice the grid at the vertical level indictated.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  level  level to slice at.  level must have units
     *         convertible with the vertial coordinate of the spatial
     *         domain or it's reference if there is a CoordinateSystem
     *         associated with the domain.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or Data.NEAREST_NEIGHBOR
     *
     * @return  spatial slice at level.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl sliceAtLevel(FieldImpl grid, Real level,
                                         int samplingMode)
            throws VisADException {
        return sliceAtLevel(grid, level, samplingMode, DEFAULT_ERROR_MODE);
    }

    /**
     * Slice the grid at the vertical level indictated.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  level  level to slice at.  level must have units
     *         convertible with the vertial coordinate of the spatial
     *         domain or it's reference if there is a CoordinateSystem
     *         associated with the domain.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or Data.NEAREST_NEIGHBOR
     * @param  errorMode Data.NO_ERRORS, Data.DEPENDENT, Data.INDEPENDENT
     *
     * @return  spatial slice at level.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl sliceAtLevel(FieldImpl grid, Real level,
                                         int samplingMode, int errorMode)
            throws VisADException {

        FieldImpl fi = grid;
        if ((getTimeSet(grid) == null) || isConstantSpatialDomain(grid)) {
            fi = slice(
                grid,
                makeSliceFromLevel(
                    (GriddedSet) getSpatialDomain(grid),
                    level), samplingMode, errorMode);
        } else {
            try {
                Set timeSet = getTimeSet(grid);
                for (int i = 0; i < timeSet.getLength(); i++) {
                    FieldImpl ff    = (FieldImpl) grid.getSample(i);
                    FieldImpl slice = null;
                    if (ff.isMissing()) {
                        slice = ff;
                    } else {
                        slice = slice(
                            ff,
                            makeSliceFromLevel(
                                (GriddedSet) getSpatialDomain(grid, i),
                                level), samplingMode, errorMode);
                    }
                    if (i == 0) {
                        fi = new FieldImpl(
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                (FunctionType) slice.getType()), timeSet);
                    }
                    fi.setSample(i, slice, false);
                }
            } catch (RemoteException re) {}  // won't happen - grids are local
        }
        return fi;

    }

    /**
     * Check if all real values in a FieldImpl are missing.
     *
     * @param field  fieldImpl to check
     *
     * @return true if all values are missing
     *
     * @throws VisADException  unable to open VisAD object
     */
    public static boolean isAllMissing(FieldImpl field)
            throws VisADException {
        return isAllMissing(field, false);
    }

    /**
     * Check if all real values in a FieldImpl are missing.
     *
     * @param grid   grid to check
     * @param popupErrorMessage  pop up a JOptionDialog box is all are missing
     * @return true if all values are missing
     *
     * @throws VisADException  unable to open VisAD object
     */
    public static boolean isAllMissing(FieldImpl grid,
                                       boolean popupErrorMessage)
            throws VisADException {
        try {
            float[][] values = grid.getFloats(false);
            if (values == null) {
                return true;
            }
            if (Misc.isNaN(values)) {
            /* moved to Misc.isMissing
            // if first data value is NaN, check if all are.
            if (Float.isNaN(values[0][0])) {
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < values[i].length; j++) {
                        float value = values[i][j];
                        if (value == value) {
                            return false;
                        }
                    }
                }
                */
                if (popupErrorMessage) {
                    String msg =
                        new String("All " + values.length * values[0].length
                                   + " data values missing");
                    LogUtil.userErrorMessage(msg);
                }
                // if we got here, then we're all missing
                return true;
            }
        } catch (RemoteException re) {
            throw new VisADException("RemoteException checking missing data");
        }
        return false;
    }



    /**
     * Check if any of the  real values in a FieldImpl are missing.
     *
     * @param grid   grid to check
     * @return true if all values are missing
     *
     * @throws VisADException  unable to open VisAD object
     */
    public static boolean isAnyMissing(FieldImpl grid) throws VisADException {
        try {
            float[][] values = grid.getFloats(false);
            if (values == null) {
                return true;
            }
            for (int i = 0; i < values.length; i++) {
                for (int j = 0; j < values[i].length; j++) {
                    float value = values[i][j];
                    if (value != value) {
                        return true;
                    }
                }
            }
        } catch (RemoteException re) {
            throw new VisADException("RemoteException checking missing data");
        }
        return false;
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
     * @deprecated use GridMath.averageOverTime(FieldImpl, boolean)
     */
    public static FieldImpl averageOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return GridMath.applyFunctionOverTime(grid, GridMath.FUNC_AVERAGE,
                makeTimes);
    }


    /**
     * This creates a field where D(T) = D(T)-D(T+offset)
     * Any time steps up to the offset time are set to missing
     * @param grid   grid to average
     * @param offset time step offset. e.g., offset=-1 results in D(T)=D(T)-D(T-1)
     * @return the new field
     *
     * @throws VisADException  On badness
     * @deprecated use GridMath.timeStepDifference(FieldImpl, int)
     */
    public static FieldImpl timeStepDifference(FieldImpl grid, int offset)
            throws VisADException {
        return GridMath.timeStepFunc(grid, offset, GridMath.FUNC_DIFFERENCE);
    }



    /**
     * This creates a field where D(T) = D(T)+D(T+offset)
     * Any time steps up to the offset time are set to missing
     * @param grid   grid to average
     * @param offset time step offset. e.g., offset=-1 results in D(T)=D(T)+D(T-1)
     * @return the new field
     *
     * @throws VisADException  On badness
     * @deprecated use GridMath.timeStepSum(FieldImpl, int)
     */
    public static FieldImpl timeStepSum(FieldImpl grid, int offset)
            throws VisADException {
        return GridMath.timeStepFunc(grid, offset, GridMath.FUNC_SUM);
    }


    /**
     * This creates a field where D(T) = D(T)-D(0)
     * Any time steps up to the offset time are set to missing
     * @param grid   grid to average
     * @return the new field
     *
     * @throws VisADException  On badness
     * @deprecated use GridMath.differenceFromBaseTime(FieldImpl)
     */
    public static FieldImpl differenceFromBaseTime(FieldImpl grid)
            throws VisADException {
        return GridMath.timeStepFunc(grid, 0, GridMath.FUNC_DIFFERENCE);
    }



    /**
     * This creates a field where D(T) = D(T)+D(0)
     * Any time steps up to the offset time are set to missing
     * @param grid   grid to average
     * @return the new field
     *
     * @throws VisADException  On badness
     * @deprecated use GridMath.sumFromBaseTime(FieldImpl)
     */
    public static FieldImpl sumFromBaseTime(FieldImpl grid)
            throws VisADException {
        return GridMath.timeStepFunc(grid, 0, GridMath.FUNC_SUM);
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
     * @deprecated use GridMath.timeStepFunc(FieldImpl, int, String)
     */
    public static FieldImpl timeStepFunc(FieldImpl grid, int offset,
                                         String func)
            throws VisADException {
        return GridMath.timeStepFunc(grid, offset, func);
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
     * @deprecated use GridMath.sumOverTime(FieldImpl, boolean)
     */
    public static FieldImpl sumOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return GridMath.applyFunctionOverTime(grid, GridMath.FUNC_SUM,
                makeTimes);
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
     * @deprecated use GridMath.minOverTime
     */
    public static FieldImpl minOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return GridMath.applyFunctionOverTime(grid, GridMath.FUNC_MIN,
                makeTimes);
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
     * @deprecated use GridMath.maxOverTime(FieldImpl, boolean)
     */
    public static FieldImpl maxOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return GridMath.applyFunctionOverTime(grid, GridMath.FUNC_MAX,
                makeTimes);
    }


    /**
     * Apply the function to the time steps of the given grid.
     * The function is one of the GridMath.FUNC_ enums
     *
     * @param grid   grid to average
     * @param function One of the GridMath.FUNC_ enums
     * @param makeTimes If true then make a time field with the range
     *                  being the same computed value. If false then just
     *                  return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     * @deprecated use GridMath.applyFunctionOverTime(FieldImpl, String, boolean)
     */
    public static FieldImpl applyFunctionOverTime(FieldImpl grid,
            String function, boolean makeTimes)
            throws VisADException {
        return GridMath.applyFunctionOverTime(grid, function, makeTimes);
    }

    /**
     * Transform a (possibly) 3D set into a 2D set (removing the Z dimension)
     *
     * @param domainSet  the 2 or 3D domain
     * @return a 2D version with Z values removed
     *
     * @throws VisADException   unable to create 2D slice
     */
    public static Gridded2DSet makeDomain2D(GriddedSet domainSet)
            throws VisADException {
        if ( !(domainSet.getManifoldDimension() >= 2)) {
            throw new VisADException(
                "grid needs to be at least a 2D manifold");
        }
        if (domainSet instanceof Gridded2DSet) {
            return (Gridded2DSet) domainSet;
        }
        Gridded2DSet  newDomainSet = null;
        RealTupleType domainType   =
            ((SetType) domainSet.getType()).getDomain();
        RealTupleType    newType = null;
        CoordinateSystem cs      = domainSet.getCoordinateSystem();
        if (cs != null) {
            // hack for WRF empirical cs - getNavigation returns the lat/lon set
            if (cs instanceof EmpiricalCoordinateSystem) {
                domainSet =
                    ((EmpiricalCoordinateSystem) cs).getReferenceSet();
                domainType = ((SetType) domainSet.getType()).getDomain();
                newType    =
                    new RealTupleType((RealType) domainType.getComponent(0),
                                      (RealType) domainType.getComponent(1));
            } else {
                MapProjection mp = getNavigation(domainSet);
                newType =
                    new RealTupleType((RealType) domainType.getComponent(0),
                                      (RealType) domainType.getComponent(1),
                                      mp, null);
            }
        } else {
            newType =
                new RealTupleType((RealType) domainType.getComponent(0),
                                  (RealType) domainType.getComponent(1));
        }
        if (domainSet instanceof Linear3DSet) {
            Linear3DSet linearSet = (Linear3DSet) domainSet;
            newDomainSet = new Linear2DSet(newType,
                                           new Linear1DSet[] {
                                               linearSet.getX(),
                    linearSet.getY() });
            return newDomainSet;
        }
        // if we make it to here, we have a Gridded3DSet, possibly on
        // a 1D manifold;

        float[][] samples    = domainSet.getSamples(false);
        int[]     lengths    = domainSet.getLengths();
        Unit[]    setUnits   = domainSet.getSetUnits();
        int       sizeX      = lengths[0];
        int       sizeY      = lengths[1];
        float[][] newSamples = null;
        if (domainSet.getManifoldDimension() == 2) {
            newSamples = new float[][] {
                samples[0], samples[1]
            };
        } else {  // full 3D set
            newSamples = new float[2][sizeX * sizeY];
            for (int j = 0; j < sizeY; j++) {
                for (int i = 0; i < sizeX; i++) {
                    int index = j * sizeX + i;
                    newSamples[0][index] = samples[0][index];
                    newSamples[1][index] = samples[1][index];
                }
            }
        }
        newDomainSet = new Gridded2DSet(newType, newSamples, sizeX, sizeY,
                                        (CoordinateSystem) null,
                                        new Unit[] { setUnits[0],
                setUnits[1] }, (ErrorEstimate[]) null, true);  // copy samples
        return newDomainSet;
    }


    /**
     * Make a new type for the field by appending the suffix to the exiting
     * RealTypes in the range
     *
     * @param oldParamType  old parameter type containing only real components
     * @param newSuffix     the new suffix for Range RealTypes
     *
     * @return  the new Range type
     *
     * @throws VisADException  problem creating new types
     */
    public static TupleType makeNewParamType(TupleType oldParamType,
                                             String newSuffix)
            throws VisADException {
        RealType[] rts    = oldParamType.getRealComponents();
        RealType[] newRTs = new RealType[rts.length];
        for (int i = 0; i < rts.length; i++) {
            String oldName     = rts[i].getName();
            String baseName    = Util.cleanTypeName(oldName);
            Unit   defaultUnit = rts[i].getDefaultUnit();
            newRTs[i] = Util.makeRealType(baseName + newSuffix, defaultUnit);
            // name could be   xxx[unit:foo]_1, we want to preserve the _1
            int unitBracket = oldName.indexOf("[unit:");
            if (unitBracket >= 0) {
                int finalBracket = oldName.lastIndexOf("]");
                if (finalBracket > unitBracket) {
                    String extra = oldName.substring(finalBracket + 1);
                    if ( !extra.isEmpty()) {
                        newRTs[i] = RealType.getRealType(newRTs[i].getName()
                                + extra, defaultUnit);
                        // make sure it's not null
                        if (newRTs[i] == null) {
                            newRTs[i] = Util.makeRealType(baseName
                                    + newSuffix + extra, defaultUnit);
                        }
                    }
                }
            }
        }
        if (rts.length == oldParamType.getDimension()) {  // just straight reals
            if (oldParamType instanceof RealVectorType) {
                return new EarthVectorType(newRTs);
            } else {
                return new RealTupleType(newRTs);
            }
        } else {                                          // loop through the individual types
            MathType[] types         = oldParamType.getComponents();
            MathType[] newTypes      = new MathType[types.length];
            int        usedRealTypes = 0;
            // The range of a FlatField can be a Real, a RealTuple or a
            // Tuple of Reals and RealTuples ;-)
            for (int i = 0; i < types.length; i++) {
                MathType mt = types[i];
                if (mt instanceof RealTupleType) {
                    RealType[] subTypes =
                        new RealType[((RealTupleType) mt).getDimension()];
                    for (int j = 0; j < subTypes.length; j++) {
                        subTypes[j] = newRTs[usedRealTypes++];
                    }
                    if (mt instanceof RealVectorType) {
                        newTypes[i] = new EarthVectorType(subTypes);
                    } else {
                        newTypes[i] = new RealTupleType(subTypes);
                    }
                } else if (mt instanceof RealType) {
                    newTypes[i] = newRTs[usedRealTypes++];
                } else {
                    throw new VisADException(
                        "Unable to create new MathType for old param type: "
                        + oldParamType);
                }
            }
            return new TupleType(newTypes);
        }
    }

    /**
     * Class Grid2D holds a 2d lat/lon and value array
     *
     *
     * @author IDV Development Team
     */
    public static class Grid2D {

        /** the lats */
        float[][] lats;

        /** the lons */
        float[][] lons;

        /** the values */
        float[][][] values;

        /**
         * ctor
         *
         * @param lats lats
         * @param lons lons
         * @param values values
         */
        public Grid2D(float[][] lats, float[][] lons, float[][][] values) {
            this.lats   = lats;
            this.lons   = lons;
            this.values = values;
        }

        /**
         * get the lons
         *
         * @return the lons
         */
        public float[][] getlons() {
            return lons;
        }

        /**
         * get the lats
         *
         * @return the lats
         */
        public float[][] getlats() {
            return lats;
        }

        /**
         * get the values
         *
         * @return the values
         */
        public float[][][] getvalues() {
            return values;
        }
    }

    ;

    /**
     * rectangulrize the given field, making a grid2d out of its spatial domain and values
     *
     * @param grid the grid
     *
     * @return the grid2d
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static Grid2D makeGrid2D(FieldImpl grid)
            throws VisADException, RemoteException {
        SampledSet domain = getSpatialDomain(grid);
        if ( !(domain instanceof GriddedSet)) {
            throw new IllegalArgumentException(
                "Spatial domain is not a griddedset:"
                + domain.getClass().getName());
        }
        GriddedSet griddedSet = (GriddedSet) domain;
        int[]      lengths    = griddedSet.getLengths();
        if (lengths.length != 2) {
            throw new IllegalArgumentException("Spatial domain is not 2D:"
                    + lengths.length);
        }
        int latIndex = isLatLonOrder(domain)
                       ? 0
                       : 1;
        int lonIndex = isLatLonOrder(domain)
                       ? 1
                       : 0;
        int xCnt     = lengths[0];
        int yCnt     = lengths[1];
        //System.err.println("X =" + xCnt + " Y=" + yCnt);
        float[][]   latLons  = getEarthLocationPoints(griddedSet);
        float[]     lats     = latLons[latIndex];
        float[]     lons     = latLons[lonIndex];
        float[][]   values   = grid.getFloats(false);
        float[][]   lat2D    = new float[xCnt][yCnt];
        float[][]   lon2D    = new float[xCnt][yCnt];
        int         rangeCnt = values.length;
        float[][][] value2D  = new float[rangeCnt][xCnt][yCnt];

        for (int i = 0; i < lats.length; i++) {
            //We need to map the linear idx into the 2d space
            //Do we know how to do this
            int xIdx = i % xCnt;
            int yIdx = i / xCnt;
            lat2D[xIdx][yIdx] = lats[i];
            lon2D[xIdx][yIdx] = lons[i];
            for (int rangeIdx = 0; rangeIdx < rangeCnt; rangeIdx++) {
                value2D[rangeIdx][xIdx][yIdx] = values[rangeIdx][i];
            }
        }

        /*
        for (int yIdx = 0; yIdx < yCnt; yIdx++) {
            for (int xIdx = 0; xIdx < xCnt; xIdx++) {
                System.err.print(" " + lat2D[xIdx][yIdx] + "/"
                                 + lon2D[xIdx][yIdx]);
            }
            //System.err.println("");
            }*/
        return new Grid2D(lat2D, lon2D, value2D);

    }


    /**
     * test
     *
     * @param grid test
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static void testIt(FieldImpl grid)
            throws VisADException, RemoteException {
        if ( !isTimeSequence(grid)) {
            Grid2D grid2D = makeGrid2D(grid);
            return;
        }
        float[][] values       = null;
        final Set timeDomain   = Util.getDomainSet(grid);
        int       numTimeSteps = timeDomain.getLength();
        for (int timeStepIdx = 0; timeStepIdx < timeDomain.getLength();
                timeStepIdx++) {
            FieldImpl timeStep = (FieldImpl) grid.getSample(timeStepIdx);
            if (timeStepIdx == 0) {
                Grid2D grid2D = makeGrid2D(timeStep);
            }
        }
    }



    /**
     * Slice the grid at the vertical level indictated.  Value is
     * assumed to be in the units of the domain set.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  levelValue  level value to slice at. Value is assumed
     *         to be in the units of the vertical coordinate of the
     *         spatial domain of the FieldImpl
     *
     * @return  spatial slice at level.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl sliceAtLevel(FieldImpl grid, double levelValue)
            throws VisADException {

        return sliceAtLevel(grid, new Real(levelValue));

    }


    /*  TODO: Gotta implement this
    public static EarthLocation getEarthLocation(FieldImpl grid,
                                                 RealTuple gridPoint)
      throws VisADException {

        EarthLocationTuple elt = null;
        try {
            SampledSet ss = getSpatialDomain(grid);
            CoordinateSystem cs = ss.getCoordinateSystem();
            if (ss.getDimension() == gridPoint.getDimension()) {
            } else if (ss.getDimension() < gridPoint.getDimension()) {
            } else {
            }
            elt = new EarthLocationTuple(0, 0, 0);
        } catch (RemoteException excp) {
            throw new VisADException(
                "getEarthLocation() got RemoteException " + excp);
        }
        return  elt;
    }
    */

    /**
     * Returns a vertical profile of a grid at a Lat/Lon point.  Returns
     * <code>null</code> if no such profile could be created.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  point  LatLonPoint to sample at.
     *
     * @return  vertical slice at point or <code>null</code>.  If this is a
     *          sequence of grids it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl getProfileAtLatLonPoint(FieldImpl grid,
            LatLonPoint point)
            throws VisADException {
        return getProfileAtLatLonPoint(grid, point, DEFAULT_SAMPLING_MODE);
    }

    /**
     * Returns a vertical profile of a grid at a Lat/Lon point.  Returns
     * <code>null</code> if no such profile could be created.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  point  LatLonPoint to sample at.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     *
     * @return  vertical slice at point or <code>null</code>.  If this is a
     *          sequence of grids it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl getProfileAtLatLonPoint(FieldImpl grid,
            LatLonPoint point, int samplingMode)
            throws VisADException {
        return getProfileAtLatLonPoint(grid, point, samplingMode,
                                       DEFAULT_ERROR_MODE);
    }

    /**
     * Returns a vertical profile of a grid at a Lat/Lon point.  Returns
     * <code>null</code> if no such profile could be created.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  point  LatLonPoint to sample at.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     * @param  errorMode Data.NO_ERRORS, Data.DEPENDENT, Data.INDEPENDENT
     *
     * @return  vertical slice at point or <code>null</code>.  If this is a
     *          sequence of grids it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl getProfileAtLatLonPoint(FieldImpl grid,
            LatLonPoint point, int samplingMode, int errorMode)
            throws VisADException {
        return sliceAlongLatLonLine(grid, point, point, samplingMode,
                                    errorMode);
    }

    /**
     * Slice the grid along the line specified by the two LatLonPoint-s
     *
     * @param  grid   grid to slice (must be a valid 2D or 3D grid)
     * @param  start  starting LatLonPoint of the line
     * @param  end    starting LatLonPoint of the line
     *
     * @return  spatial slice along the line.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl sliceAlongLatLonLine(FieldImpl grid,
            LatLonPoint start, LatLonPoint end)
            throws VisADException {
        return sliceAlongLatLonLine(grid, start, end, DEFAULT_SAMPLING_MODE);
    }

    /**
     * Slice the grid along the line specified by the two LatLonPoint-s
     *
     * @param  grid   grid to slice (must be a valid 2D or 3D grid)
     * @param  start  starting LatLonPoint of the line
     * @param  end    starting LatLonPoint of the line
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     *
     * @return  spatial slice along the line.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl sliceAlongLatLonLine(FieldImpl grid,
            LatLonPoint start, LatLonPoint end, int samplingMode)
            throws VisADException {
        return sliceAlongLatLonLine(grid, start, end, samplingMode,
                                    DEFAULT_ERROR_MODE);
    }

    /**
     * Slice the grid along the line specified by the two LatLonPoint-s
     *
     * @param  grid   grid to slice (must be a valid 2D or 3D grid)
     * @param  start  starting LatLonPoint of the line
     * @param  end    starting LatLonPoint of the line
     * @param samplingMode mode for sampling
     * @param  errorMode Data.NO_ERRORS, Data.DEPENDENT, Data.INDEPENDENT
     *
     * @return  spatial slice along the line.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl sliceAlongLatLonLine(FieldImpl grid,
            LatLonPoint start, LatLonPoint end, int samplingMode,
            int errorMode)
            throws VisADException {
        FieldImpl fi = grid;
        if (isSinglePointDomain(grid)) {
            return grid;
        }
        if ((getTimeSet(grid) == null) || isConstantSpatialDomain(grid)) {
            fi = slice(
                grid,
                makeSliceFromLatLonPoints(
                    (GriddedSet) getSpatialDomain(grid), start,
                    end), samplingMode, errorMode);
        } else {
            try {
                Set timeSet = getTimeSet(grid);
                for (int i = 0; i < timeSet.getLength(); i++) {
                    FieldImpl ff    = (FieldImpl) grid.getSample(i);
                    FieldImpl slice = null;
                    if (ff.isMissing()) {
                        slice = ff;
                    } else {
                        slice = slice(
                            ff,
                            makeSliceFromLatLonPoints(
                                (GriddedSet) getSpatialDomain(grid, i),
                                start, end), samplingMode, errorMode);
                    }
                    if (i == 0) {
                        fi = new FieldImpl(
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                (FunctionType) slice.getType()), timeSet);
                    }
                    fi.setSample(i, slice, false);

                }
            } catch (RemoteException re) {}  // won't happen - grids are local
        }
        return fi;
    }

    /**
     * Sample the grid at the position defined by the EarthLocation
     *
     * @param  grid   grid to sample (must be a valid 3D grid)
     * @param  location  EarthLocation to sample at.
     *
     * @return  grid representing the values of the original grid at the
     *          point defined by location.  If this is a sequence of grids
     *          it will be a sequence of the values.
     *
     * @throws  VisADException  invalid point or some other problem
     */
    public static FieldImpl sample(FieldImpl grid, EarthLocation location)
            throws VisADException {
        return sample(grid, location, DEFAULT_SAMPLING_MODE);
    }

    /**
     * Sample the grid at the position defined by the EarthLocation
     *
     * @param  grid   grid to sample (must be a valid 3D grid)
     * @param  location  EarthLocation to sample at.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     *
     * @return  grid representing the values of the original grid at the
     *          point defined by location.  If this is a sequence of grids
     *          it will be a sequence of the values.
     *
     * @throws  VisADException  invalid point or some other problem
     */
    public static FieldImpl sample(FieldImpl grid, EarthLocation location,
                                   int samplingMode)
            throws VisADException {
        return sample(grid, location, samplingMode, DEFAULT_ERROR_MODE);
    }

    /**
     * Sample the grid at the position defined by the EarthLocation
     * with the VisAD resampling method given.
     *
     * @param  grid   grid to sample (must be a valid 3D grid)
     * @param  location  EarthLocation to sample at.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     * @param  errorMode Data.NO_ERRORS, Data.DEPENDENT, Data.INDEPENDENT
     *
     * @return  grid representing the values of the original grid at the
     *          point defined by location.  If this is a sequence of grids
     *          it will be a sequence of the values.
     *
     * @throws  VisADException  invalid point or some other problem
     */
    public static FieldImpl sample(FieldImpl grid, EarthLocation location,
                                   int samplingMode, int errorMode)
            throws VisADException {
        SampledSet spatialSet = getSpatialDomain(grid);
        if ( !isNavigated(spatialSet)) {
            throw new IllegalArgumentException("Domain is not georeferenced");
        }
        if (spatialSet.getManifoldDimension() != 3) {
            throw new IllegalArgumentException("Grid must be 3D");
        }
        RealTuple point     = null;
        Real      longitude = normalizeLongitude(spatialSet,
                                            location.getLongitude());
        try {
            if (isLatLonOrder(grid)) {
                point = new RealTuple(new Real[] { location.getLatitude(),
                        longitude, location.getAltitude() });
            } else {
                point = new RealTuple(new Real[] { longitude,
                        location.getLatitude(), location.getAltitude() });
            }
        } catch (RemoteException re) {
            throw new VisADException("Can't get position from point");
        }
        return sampleAtPoint(grid, point, samplingMode, errorMode);
    }

    /**
     * Sample the grid at the position defined by the LatLonPoint
     *
     * @param  grid   grid to sample (must be a valid 3D grid)
     * @param  point  LatLonPoint to sample at.
     *
     * @return  grid representing the values of the original grid at the
     *          point defined by point.  If this is a sequence of grids
     *          it will be a sequence of the values.
     *
     * @throws  VisADException  invalid point or some other problem
     */
    public static FieldImpl sample(FieldImpl grid, LatLonPoint point)
            throws VisADException {
        return sample(grid, point, DEFAULT_SAMPLING_MODE);
    }

    /**
     * Sample the grid at the position defined by the LatLonPoint
     *
     * @param  grid   grid to sample (must be a valid 3D grid)
     * @param  point  LatLonPoint to sample at.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     *
     * @return  grid representing the values of the original grid at the
     *          point defined by point.  If this is a sequence of grids
     *          it will be a sequence of the values.
     *
     * @throws  VisADException  invalid point or some other problem
     */
    public static FieldImpl sample(FieldImpl grid, LatLonPoint point,
                                   int samplingMode)
            throws VisADException {
        return sample(grid, point, samplingMode, DEFAULT_ERROR_MODE);
    }

    /**
     * Sample the grid at the position defined by the LatLonPoint
     *
     * @param  grid   grid to sample (must be a valid 3D grid)
     * @param  point  LatLonPoint to sample at.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     * @param  errorMode Data.NO_ERRORS, Data.DEPENDENT, Data.INDEPENDENT
     *
     * @return  grid representing the values of the original grid at the
     *          point defined by point.  If this is a sequence of grids
     *          it will be a sequence of the values.
     *
     * @throws  VisADException  invalid point or some other problem
     */
    public static FieldImpl sample(FieldImpl grid, LatLonPoint point,
                                   int samplingMode, int errorMode)
            throws VisADException {
        SampledSet spatialSet = getSpatialDomain(grid);
        if ( !isNavigated(spatialSet)) {
            throw new IllegalArgumentException("Domain is not georeferenced");
        }
        if (spatialSet.getManifoldDimension() != 2) {
            throw new IllegalArgumentException(
                "Can't sample a 3-D grid on Lat/Lon only");
        }
        RealTuple location = null;
        Real longitude = normalizeLongitude(spatialSet, point.getLongitude());
        try {
            if (isLatLonOrder(grid)) {
                location = new RealTuple(new Real[] { point.getLatitude(),
                        longitude });
            } else {
                location = new RealTuple(new Real[] { longitude,
                        point.getLatitude() });
            }
        } catch (RemoteException re) {
            throw new VisADException("Can't get position from point");
        }
        return sampleAtPoint(grid, location, samplingMode, errorMode);
    }

    /**
     * Slice the grid at the positions defined by a SampledSet.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  slice  set of points to sample on.  It must be compatible
     *         with the spatial domain of the grid.
     *
     * @return  a FieldImpl the grid representing the values
     *          of the original grid at the
     *          points defined by slice.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  invalid slice or some other problem
     */
    public static FieldImpl slice(FieldImpl grid, SampledSet slice)
            throws VisADException {
        return slice(grid, slice, DEFAULT_SAMPLING_MODE);
    }

    /**
     * Slice the grid at the positions defined by a SampledSet.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  slice  set of points to sample on.  It must be compatible
     *         with the spatial domain of the grid.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     *
     * @return  a FieldImpl the grid representing the values
     *          of the original grid at the
     *          points defined by slice.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  invalid slice or some other problem
     */
    public static FieldImpl slice(FieldImpl grid, SampledSet slice,
                                  int samplingMode)
            throws VisADException {
        return slice(grid, slice, samplingMode, DEFAULT_ERROR_MODE);
    }

    /**
     * Slice the grid at the positions defined by a SampledSet.
     *
     * @param  grid   grid to slice (must be a valid 3D grid)
     * @param  slice  set of points to sample on.  It must be compatible
     *         with the spatial domain of the grid.
     * @param  samplingMode Data.WEIGHTED_AVERAGE or NEAREST_NEIGHBOR
     * @param  errorMode Data.NO_ERRORS, Data.DEPENDENT, Data.INDEPENDENT
     *
     * @return  a FieldImpl the grid representing the values
     *          of the original grid at the
     *          points defined by slice.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  invalid slice or some other problem
     */
    public static FieldImpl slice(FieldImpl grid, SampledSet slice,
                                  int samplingMode, int errorMode)
            throws VisADException {
        return resampleGrid(grid, slice, samplingMode, errorMode);
    }

    /**
     * Transform a 2D slice (3D grid with 2D manifold) into a 2D
     * grid.
     *
     * @param slice    slice to transform
     * @return   slice as a 2D grid
     *
     * @throws VisADException   unable to create 2D slice
     */
    public static FieldImpl make2DGridFromSlice(FieldImpl slice)
            throws VisADException {
        return make2DGridFromSlice(slice, true);
    }

    /**
     * Transform a 2D slice (3D grid with 2D manifold) into a 2D
     * grid.
     *
     * @param slice    slice to transform
     * @param copy     true to copy data
     * @return   slice as a 2D grid
     *
     * @throws VisADException   unable to create 2D slice
     */
    public static FieldImpl make2DGridFromSlice(FieldImpl slice, boolean copy)
            throws VisADException {
        GriddedSet domainSet = (GriddedSet) getSpatialDomain(slice);
        if ((domainSet.getDimension() != 3)
                && (domainSet.getManifoldDimension() != 2)) {
            throw new VisADException("slice is not 3D with 2D manifold");
        }
        Gridded2DSet new2DDomainSet = makeDomain2D(domainSet);
        if (isConstantSpatialDomain(slice)) {
            return setSpatialDomain(slice, new2DDomainSet, copy);
        } else {
            if (isTimeSequence(slice)) {
                Set          timeSet         = getTimeSet(slice);
                GriddedSet   lastDomainSet   = domainSet;
                Gridded2DSet last2DDomainSet = new2DDomainSet;
                try {
                    FieldImpl newSlice =
                        setSpatialDomain((FieldImpl) slice.getSample(0),
                                         last2DDomainSet, copy);
                    FieldImpl retField =
                        new FieldImpl(
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                newSlice.getType()), timeSet);
                    retField.setSample(0, newSlice, copy);
                    for (int t = 1; t < timeSet.getLength(); t++) {
                        FieldImpl timeStep = (FieldImpl) slice.getSample(t,
                                                 false);
                        GriddedSet domain =
                            (GriddedSet) getSpatialDomain(timeStep);
                        if ( !domain.equals(lastDomainSet)) {
                            lastDomainSet   = domain;
                            last2DDomainSet = makeDomain2D(domain);
                        }
                        newSlice =
                            setSpatialDomain((FieldImpl) slice.getSample(t),
                                             last2DDomainSet, copy);
                        retField.setSample(t, newSlice, copy);
                    }
                    return retField;

                } catch (RemoteException re) {
                    throw new VisADException(
                        "Got unexpected RemoteException: " + re.getMessage());
                }
            } else {
                throw new VisADException(
                    "Unable to handle time series with different spatial domains");
            }
        }
    }

    /**
     * Get the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range
     *
     * @param grid    grid to check
     * @return   TupleType of lowest element
     *
     * @throws VisADException   unable to get at data types
     */
    public static Unit[] getParamUnits(FieldImpl grid) throws VisADException {
        Unit[] units = null;
        try {
            if (grid instanceof FlatField) {                  // single time (domain -> range)
                units = DataUtility.getRangeUnits((FlatField) grid);
            } else if (isTimeSequence(grid)) {                // (time -> something)
                Data d = grid.getSample(0);
                if (d instanceof FlatField) {                 // (domain -> range)
                    units = DataUtility.getRangeUnits((FlatField) d);
                } else if (d instanceof FieldImpl) {          // (index -> (something)
                    if (isSequence((FieldImpl) d)) {
                        d = ((FieldImpl) d).getSample(0);
                        if (d instanceof Real) {              // (index -> value)
                            units = new Unit[] { ((Real) d).getUnit() };
                        } else if (d instanceof Tuple) {      // index -> (value)
                            Real[] reals = ((Tuple) d).getRealComponents();
                            units = new Unit[reals.length];
                            for (int i = 0; i < reals.length; i++) {
                                units[i] = reals[i].getUnit();
                            }
                        } else if (d instanceof FlatField) {  // index -> (value)
                            units = DataUtility.getRangeUnits((FlatField) d);
                        }
                    } else {                      // index -> value
                        units = DataUtility.getRangeUnits((FlatField) d);
                    }
                }
            } else if (isSequence(grid)) {        // (index -> something)
                Data d = grid.getSample(0);
                if (d instanceof FlatField) {     // (domain -> range)
                    units = DataUtility.getRangeUnits((FlatField) d);
                } else if (d instanceof Real) {   // (index -> value)
                    units = new Unit[] { ((Real) d).getUnit() };
                } else if (d instanceof Tuple) {  // index -> (value)
                    Real[] reals = ((Tuple) d).getRealComponents();
                    units = new Unit[reals.length];
                    for (int i = 0; i < reals.length; i++) {
                        units[i] = reals[i].getUnit();
                    }
                }
            }
        } catch (RemoteException re) {
            throw new VisADException("problem getting param units " + re);
        }
        return units;
    }

    /**
     * Print the type of the sample of a data object
     *
     * @param field  the field
     *
     * @return  the data type
     *
     * @throws RemoteException java RMI problem
     * @throws VisADException   unable to get at data types
     */
    public static String printit(FieldImpl field)
            throws VisADException, RemoteException {
        Data d = field.getSample(0);
        return "sample:" + d.getClass().getName();

    }

    /**
     * Get the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range
     *
     * @param grid    grid to check
     * @return   TupleType of lowest element
     *
     * @throws VisADException   unable to get at data types
     */
    public static TupleType getParamType(FieldImpl grid)
            throws VisADException {
        TupleType tt = null;
        try {
            if (grid instanceof FlatField) {          // single time (domain -> range)
                tt = DataUtility.getRangeTupleType(grid);
            } else if (isTimeSequence(grid)) {        // (time -> something)
                Data d = grid.getSample(0);
                if (d instanceof FlatField) {         // (domain -> range)
                    tt = DataUtility.getRangeTupleType((FlatField) d);
                } else if (d instanceof FieldImpl) {  // (index -> (something)
                    if (isSequence((FieldImpl) d)) {
                        d = ((FieldImpl) d).getSample(0);
                        if (d instanceof Real) {      // (index -> value)
                            tt = new RealTupleType(
                                (RealType) ((Real) d).getType());
                        } else if (d instanceof Tuple) {      // index -> (value)
                            tt = (TupleType) d.getType();
                        } else if (d instanceof FlatField) {  // index -> (value)
                            tt = DataUtility.getRangeTupleType((FlatField) d);
                        }
                    } else {                      // index -> value
                        tt = DataUtility.getRangeTupleType((FieldImpl) d);
                    }
                }
            } else if (isSequence(grid)) {        // (index -> something)
                Data d = grid.getSample(0);
                if (d instanceof FlatField) {     // (domain -> range)
                    tt = DataUtility.getRangeTupleType((FlatField) d);
                } else if (d instanceof Real) {   // (index -> value)
                    tt = new RealTupleType((RealType) ((Real) d).getType());
                } else if (d instanceof Tuple) {  // index -> (value)
                    tt = (TupleType) d.getType();
                }
            }
            if (tt == null) {
                throw new VisADException("Can't handle data of type "
                                         + grid.getType());
            }
        } catch (RemoteException re) {
            throw new VisADException("problem getting param type " + re);
        }
        return tt;
    }

    /**
     * Extract the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range
     *
     * @param grid    grid to check
     * @param index   parameter index
     * @return   TupleType of lowest element
     *
     * @throws VisADException   unable to get at data types
     */
    public static FieldImpl getParam(FieldImpl grid, int index)
            throws VisADException {
        return getParam(grid, index, true);
    }

    /**
     * Extract the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range
     *
     * @param grid    grid to check
     * @param index   parameter index
     * @param copy    true to make a copy
     * @return   TupleType of lowest element
     *
     * @throws VisADException   unable to get at data types
     */
    public static FieldImpl getParam(FieldImpl grid, int index, boolean copy)
            throws VisADException {

        FieldImpl newField = null;
        if (grid == null) {
            return newField;
        }
        TupleType tt = getParamType(grid);
        if (index > tt.getDimension()) {
            return null;
        }
        MathType newParam = tt.getComponent(index);

        try {
            Data         step1   = null;
            FunctionType newType = null;

            if (isSequence(grid)) {

                // get sample at first time step
                try {
                    step1 = grid.getSample(0);
                } catch (RemoteException re) {
                    throw new VisADException("problem setting param type "
                                             + re);
                }
                // if "step1" is NOT yet ANOTHER sequence
                if ( !isSequence((FieldImpl) step1)) {
                    Trace.call1("GridUtil.setParam:sequence");
                    // get "time" domain from "grid"
                    MathType domRT =
                        ((FunctionType) grid.getType()).getDomain();
                    // get "(x,y,z)->param"
                    FunctionType ffRT =
                        (FunctionType) ((FunctionType) grid.getType())
                            .getRange();
                    // get "(x,y,z)"
                    MathType ffdomRT = ffRT.getDomain();
                    // make new "time->(x,y,z) - >NEWparam"
                    newType = new FunctionType(domRT,
                            new FunctionType(ffdomRT, newParam));

                    Set timeDomain = Util.getDomainSet(grid);
                    newField = new FieldImpl(newType, timeDomain);
                    for (int i = 0; i < timeDomain.getLength(); i++) {
                        newField.setSample(i, ((FlatField) grid.getSample(i,
                                false)).extract(index, copy), false);
                    }
                    Trace.call2("GridUtil.setParam:sequence");
                }
                // if this data is a double 1D sequence, as for the radar RHI
                // time -> (integer_index -> ((Range, Azimuth, Elevation_Angle) 
                //                               -> Reflectivity_0))
                else {
                    // get "time" domain from "grid"
                    Trace.call1("GridUtil.setParam:indexsequence");
                    MathType timedomRT =
                        ((FunctionType) grid.getType()).getDomain();
                    // get "integer_index" domain from first time step, step1
                    MathType indexdomRT =
                        ((FunctionType) step1.getType()).getDomain();
                    // get "(x,y,z)->param"
                    FunctionType ffRT =
                        (FunctionType) ((FunctionType) step1.getType())
                            .getRange();
                    // get "(x,y,z)"
                    MathType ffdomRT = ffRT.getDomain();
                    // make new "time->index->(x,y,z) - >NEWparam"
                    FunctionType paramRange = new FunctionType(ffdomRT,
                                                  newParam);
                    FunctionType indexRange = new FunctionType(indexdomRT,
                                                  paramRange);
                    newType = new FunctionType(timedomRT, indexRange);
                    Set timeDomain = Util.getDomainSet(grid);
                    newField = new FieldImpl(newType, timeDomain);
                    for (int i = 0; i < timeDomain.getLength(); i++) {
                        FieldImpl indexField = (FieldImpl) grid.getSample(i,
                                                   false);
                        Set       indexSet = Util.getDomainSet(indexField);
                        FieldImpl newIndexField = new FieldImpl(indexRange,
                                                      indexSet);
                        for (int j = 0; j < indexSet.getLength(); j++) {
                            newIndexField.setSample(j,
                                    ((FlatField) indexField.getSample(j,
                                        false)).extract(index, copy), false);
                        }
                        newField.setSample(i, newIndexField);
                    }
                    Trace.call2("GridUtil.setParam:indexsequence");
                }

            } else {
                // have "grid" single FlatField; neither time nor index domain
                //newField = (FieldImpl) Util.clone(grid, newParam, true, copy);
                newField = (FieldImpl) ((FlatField) grid).extract(index,
                        copy);
            }
        } catch (RemoteException re) {
            throw new VisADException("problem setting param type " + re);
        }
        return newField;

    }

    /**
     * Set the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range.  Data is replicated.
     *
     * @param  grid  grid to change
     * @param  newName  name of new parameter
     *
     * @return   a new FieldImpl with the new parameter type
     *
     * @throws VisADException   problem setting new parameter
     */
    public static FieldImpl setParamType(FieldImpl grid, String newName)
            throws VisADException {
        return setParamType(grid, newName, true);
    }

    /**
     * Set the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range.  Data is replicated.
     *
     * @param  grid  grid to change
     * @param  newName  name of new parameter
     * @param  copy  true to make a copy
     *
     * @return   a new FieldImpl with the new parameter type
     *
     * @throws VisADException   problem setting new parameter
     */
    public static FieldImpl setParamType(FieldImpl grid, String newName,
                                         boolean copy)
            throws VisADException {
        return setParamType(grid, new String[] { newName }, copy);
    }

    /**
     * Set the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range.  Data is replicated.
     *
     * @param  grid  grid to change
     * @param  newNames  names of new parameters
     * @param  copy  true to make a copy
     *
     * @return   a new FieldImpl with the new parameter type
     *
     * @throws VisADException   problem setting new parameter
     */
    public static FieldImpl setParamType(FieldImpl grid, String[] newNames,
                                         boolean copy)
            throws VisADException {
        TupleType  tt  = getParamType(grid);
        RealType[] rts = tt.getRealComponents();
        if (rts.length != newNames.length) {
            throw new VisADException(
                "number of names must match number of components");
        }
        RealType[] newTypes = new RealType[newNames.length];
        for (int i = 0; i < newNames.length; i++) {
            newTypes[i] = DataUtil.makeRealType(newNames[i],
                    rts[i].getDefaultUnit());

        }
        RealTupleType newParam = new RealTupleType(newTypes);
        return setParamType(grid, newParam, copy);
    }

    /**
     * Set the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range.  Data is replicated.
     *
     * @param  grid  grid to change
     * @param  newParam  MathType of new parameter
     *
     * @return   a new FieldImpl with the new parameter type
     *
     * @throws VisADException   problem setting new parameter
     */
    public static FieldImpl setParamType(FieldImpl grid, RealType newParam)
            throws VisADException {
        return setParamType(grid, newParam, true);
    }

    /**
     * Set the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range.
     *
     * @param  grid  grid to change
     * @param  newParam  RealType of new parameter
     * @param  copy    true to copy data
     *
     * @return   a new FieldImpl with the new parameter type
     *
     * @throws VisADException   problem setting new parameter
     */
    public static FieldImpl setParamType(FieldImpl grid, RealType newParam,
                                         boolean copy)
            throws VisADException {
        return setParamType(grid, new RealTupleType(newParam), copy);
    }

    /**
     * Set the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range.  Data is replicated.
     *
     * @param  grid  grid to change
     * @param  newParam  MathType of new parameter
     *
     * @return   a new FieldImpl with the new parameter type
     *
     * @throws VisADException   problem setting new parameter
     */
    public static FieldImpl setParamType(FieldImpl grid, TupleType newParam)
            throws VisADException {
        return setParamType(grid, newParam, true);
    }

    /**
     * Set the range MathType of the lowest element.  If this is
     * a sequence, it will be the range type of the individual elements.
     * If not, it will be the range.
     *
     * @param  grid  grid to change
     * @param  newParam  MathType of new parameter
     * @param  copy  true to copy the data
     *
     * @return   a new FieldImpl with the new parameter type
     *
     * @throws VisADException   problem setting new parameter
     */
    public static FieldImpl setParamType(FieldImpl grid, TupleType newParam,
                                         boolean copy)
            throws VisADException {

        FieldImpl newField = null;
        if (grid == null) {
            return newField;
        }
        // TODO:  uncomment this
        /*
        if (newParam.equals(getParamType(grid)) && !copy) {
            System.out.println("new param == old param");
            return grid;
        }
        */
        try {
            Data         step1   = null;
            FunctionType newType = null;

            if (isSequence(grid)) {

                // get sample at first time step
                try {
                    step1 = grid.getSample(0);
                } catch (RemoteException re) {
                    throw new VisADException("problem setting param type "
                                             + re);
                }
                // if "step1" is NOT yet ANOTHER sequence
                if ( !isSequence((FieldImpl) step1)) {
                    Trace.call1("GridUtil.setParamType:sequence");
                    // get "time" domain from "grid"
                    MathType domRT =
                        ((FunctionType) grid.getType()).getDomain();
                    // get "(x,y,z)->param"
                    FunctionType ffRT =
                        (FunctionType) ((FunctionType) grid.getType())
                            .getRange();
                    // get "(x,y,z)"
                    MathType ffdomRT = ffRT.getDomain();
                    // make new "time->(x,y,z) - >NEWparam"
                    newType = new FunctionType(domRT,
                            new FunctionType(ffdomRT, newParam));

                    Set timeDomain = Util.getDomainSet(grid);
                    newField = new FieldImpl(newType, timeDomain);
                    for (int i = 0; i < timeDomain.getLength(); i++) {
                        newField.setSample(
                            i, (FieldImpl) Util.clone(
                                grid.getSample(i, false), newParam, true,
                                copy, false), false);
                    }
                    Trace.call2("GridUtil.setParamType:sequence");
                }
                // if this data is a double 1D sequence, as for the radar RHI
                // time -> (integer_index -> ((Range, Azimuth, Elevation_Angle) 
                //                               -> Reflectivity_0))
                else {
                    // get "time" domain from "grid"
                    Trace.call1("GridUtil.setParamType:indexsequence");
                    MathType timedomRT =
                        ((FunctionType) grid.getType()).getDomain();
                    // get "integer_index" domain from first time step, step1
                    MathType indexdomRT =
                        ((FunctionType) step1.getType()).getDomain();
                    // get "(x,y,z)->param"
                    FunctionType ffRT =
                        (FunctionType) ((FunctionType) step1.getType())
                            .getRange();
                    // get "(x,y,z)"
                    MathType ffdomRT = ffRT.getDomain();
                    // make new "time->index->(x,y,z) - >NEWparam"
                    FunctionType paramRange = new FunctionType(ffdomRT,
                                                  newParam);
                    FunctionType indexRange = new FunctionType(indexdomRT,
                                                  paramRange);
                    newType = new FunctionType(timedomRT, indexRange);
                    Set timeDomain = Util.getDomainSet(grid);
                    newField = new FieldImpl(newType, timeDomain);
                    for (int i = 0; i < timeDomain.getLength(); i++) {
                        FieldImpl indexField = (FieldImpl) grid.getSample(i,
                                                   false);
                        Set       indexSet = Util.getDomainSet(indexField);
                        FieldImpl newIndexField = new FieldImpl(indexRange,
                                                      indexSet);
                        for (int j = 0; j < indexSet.getLength(); j++) {
                            newIndexField.setSample(
                                j, (FieldImpl) Util.clone(
                                    indexField.getSample(j, false),
                                    paramRange, true, copy, false), false);
                        }
                        newField.setSample(i, newIndexField);
                    }
                    Trace.call2("GridUtil.setParamType:indexsequence");
                }

            } else {
                // have "grid" single FlatField; neither time nor index domain
                newField = (FieldImpl) Util.clone(grid, newParam, true, copy,
                        false);
            }
        } catch (RemoteException re) {
            throw new VisADException("problem setting param type " + re);
        }
        return newField;

    }

    /**
     * Extract the param from a sequence, it will be the range type
     * of the individual elements. If not, it will be the range.
     *
     * @param  grid  grid to change
     * @param  param  MathType of new parameter
     *
     * @return   grid with just param in it
     *
     * @throws VisADException   problem setting new parameter
     */
    public static FieldImpl extractParam(FieldImpl grid, ScalarType param)
            throws VisADException {

        try {

            FieldImpl newGrid = null;

            if ( !MathType.findScalarType(grid.getType(), param)) {
                newGrid = setParamType(grid, (param instanceof RealType)
                                             ? new RealTupleType(
                                             (RealType) param)
                                             : new TupleType(new MathType[] {
                                             param }));
            } else {


                if (isSequence(grid)) {

                    SampledSet   s = (SampledSet) Util.getDomainSet(grid);
                    FunctionType newType = null;
                    Data         step1   = null;

                    step1 = grid.getSample(0);

                    // if "step1" is NOT yet ANOTHER sequence
                    if ( !isSequence((FieldImpl) step1)) {
                        // get "time" domain from "grid"
                        MathType domRT =
                            ((FunctionType) grid.getType()).getDomain();
                        // get "(x,y,z)->param"
                        FunctionType ffRT =
                            (FunctionType) ((FunctionType) grid.getType())
                                .getRange();
                        // get params
                        MathType ffRange = ffRT.getRange();

                        //only one param, so must be same as what we are seeking
                        if ((ffRange instanceof RealType)
                                || ((TupleType) ffRange).getDimension()
                                   == 1) {
                            return grid;
                        }
                        TupleType ffrangeRT  = (TupleType) ffRange;

                        int       paramIndex = ffrangeRT.getIndex(param);

                        // get "(x,y,z)"
                        MathType ffdomRT = ffRT.getDomain();

                        // make new "time->(x,y,z) - >NEWparam"
                        newType = new FunctionType(domRT,
                                new FunctionType(ffdomRT, param));
                        newGrid = new FieldImpl(newType, s);
                        for (int i = 0; i < s.getLength(); i++) {
                            newGrid.setSample(i,
                                    ((FieldImpl) grid.getSample(i,
                                        false)).extract(paramIndex), false);
                        }
                    }
                    // if this data is a double 1D sequence, as for the radar RHI
                    // time -> (integer_index -> ((Range, Azimuth, Elevation_Angle) 
                    //                               -> Reflectivity_0))
                    else {
                        // get "time" domain from "grid"
                        MathType timedomRT =
                            ((FunctionType) grid.getType()).getDomain();
                        // get "integer_index" domain from first time step, step1
                        MathType indexdomRT =
                            ((FunctionType) step1.getType()).getDomain();
                        // get "(x,y,z)->param"
                        FunctionType ffRT =
                            (FunctionType) ((FunctionType) step1.getType())
                                .getRange();
                        // get params
                        TupleType ffrangeRT = (TupleType) ffRT.getRange();
                        //only one param, so must be same as what we are seeking
                        if (ffrangeRT.getDimension() == 1) {
                            return grid;
                        }
                        int paramIndex = ffrangeRT.getIndex(param);

                        // get "(x,y,z)"
                        MathType ffdomRT = ffRT.getDomain();
                        // make new "time->index->(x,y,z) - >NEWparam"
                        FunctionType indexFIType =
                            new FunctionType(indexdomRT,
                                             new FunctionType(ffdomRT,
                                                 param));
                        newType = new FunctionType(timedomRT, indexFIType);

                        newGrid = new FieldImpl(newType, s);
                        for (int i = 0; i < s.getLength(); i++) {
                            FieldImpl indexFI = (FieldImpl) grid.getSample(i,
                                                    false);
                            SampledSet domSet =
                                (SampledSet) Util.getDomainSet(indexFI);
                            FieldImpl tempFI = new FieldImpl(indexFIType,
                                                   domSet);
                            for (int j = 0; j < domSet.getLength(); j++) {
                                tempFI.setSample(
                                    j, ((FieldImpl) indexFI.getSample(
                                        j, false)).extract(
                                            paramIndex), false);
                            }
                            newGrid.setSample(i, tempFI, false);
                        }
                    }


                } else {
                    // have "grid" single FlatField; neither time 
                    // nor index domain
                    newGrid = (FieldImpl) grid.extract(param);
                }
            }
            return newGrid;
        } catch (RemoteException re) {
            throw new VisADException("problem setting param type " + re);
        }
    }

    /**
     * Extract a single parameter from a grid of multiple parameters.
     *
     * @param grid to extract from
     * @param paramType   param to extract
     *
     * @return  grid with just that param in it
     *
     * @throws VisADException some problem occured (like the param isn't
     *         in the grid)
     */
    public FieldImpl extractParam(FieldImpl grid, MathType paramType)
            throws VisADException {
        FieldImpl extractedFI = null;
        try {
            if ( !isSequence(grid)) {
                extractedFI = (FlatField) grid.extract(paramType);
            } else {               // some sort of sequence - evaluate each
                Set sequenceDomain = Util.getDomainSet(grid);
                for (int i = 0; i < sequenceDomain.getLength(); i++) {
                    Data sample = (FlatField) grid.extract(paramType);
                    if (i == 0) {  // set up the functiontype
                        FunctionType sampledType =
                            new FunctionType(((SetType) sequenceDomain
                                .getType()).getDomain(), sample.getType());
                        extractedFI = new FieldImpl(sampledType,
                                sequenceDomain);
                    }
                    extractedFI.setSample(i, sample, false);
                }
            }
        } catch (RemoteException re) {
            throw new VisADException("problem slicing remote field " + re);
        }
        return extractedFI;
    }

    /**
     * Create a slice from the level specified based on the spatial
     * domain.
     *
     * @param spatialSet    spatial set to use for values
     * @param level         level to use
     *
     * @return 3-D spatial set with a 2-D manifold.  The 3rd dimension
     *         at each of the 2-D points is the value of level.
     *
     * @throws VisADException    incompatible units or problem with data
     */
    private static SampledSet makeSliceFromLevel(GriddedSet spatialSet,
            Real level)
            throws VisADException {

        Trace.call1("GridUtil.makeSliceFromLevel",
                    " " + level.toValueString());

        // make sure this is a sliceable grid
        if (spatialSet.getManifoldDimension() != 3) {
            throw new IllegalArgumentException("Can't slice a 2-D grid");
        }


        // check the level type against the domain type and reference
        RealType      type        = (RealType) level.getType();  // level type

        RealTupleType spatialType =
            ((SetType) spatialSet.getType()).getDomain();
        RealTupleType spatialReferenceType =
            (spatialSet.getCoordinateSystem() != null)
            ? spatialSet.getCoordinateSystem().getReference()
            : null;



        RealType zType = getVerticalType(spatialSet);
        Unit     zUnit = getVerticalUnit(spatialSet);
        if (type.equals(RealType.Generic)) {
            type  = zType;
            level = new Real(zType, level.getValue(), zUnit);
        }

        RealType zRefType  = (spatialReferenceType != null)
                             ? (RealType) spatialReferenceType.getComponent(2)
                             : null;  // ref Z
        boolean  isRefType = !type.equals(zType)
                            && type.equalsExceptNameButUnits(zRefType);



        if ( !(type.equalsExceptNameButUnits(zType) || isRefType)) {
            throw new IllegalArgumentException(
                "level is incompatible with vertical component of spatial domain");
        }
        GriddedSet samplingSet = null;
        float      gridLevel;
        if ( !isRefType) {  // native coordinates
            gridLevel = (float) level.getValue(spatialSet.getSetUnits()[2]);
        } else {            // convert to native
            CoordinateSystem cs = spatialSet.getCoordinateSystem();
            if (cs instanceof EmpiricalCoordinateSystem) {
                spatialSet =
                    ((EmpiricalCoordinateSystem) cs).getReferenceSet();
                gridLevel = (float) level.getValue(zRefType.getDefaultUnit());
                isRefType = false;
            } else {
                float levVal =
                    (float) level.getValue(zRefType.getDefaultUnit());
                float[][] gridSamples = spatialSet.getSamples(false);
                float[][] zeroCoords  = new float[][] {
                    { gridSamples[0][0] }, { gridSamples[1][0] },
                    { gridSamples[2][0] }
                };
                // convert the first point in the grid to the reference
                zeroCoords =
                    spatialSet.getCoordinateSystem().toReference(zeroCoords);
                // now, take that, substitute in the level
                zeroCoords = spatialSet.getCoordinateSystem().fromReference(
                    new float[][] {
                    { zeroCoords[0][0] }, { zeroCoords[1][0] }, { levVal }
                });
                gridLevel = zeroCoords[2][0];
            }
            /*
            float levVal = (float) level.getValue(zRefType.getDefaultUnit());
            float[][] zeroCoords = new float[][] {
                { 0.0f }, { 0.0f }, { levVal }
            };
            zeroCoords =
                spatialSet.getCoordinateSystem().fromReference(zeroCoords);
            gridLevel = zeroCoords[2][0];
            // in case this doesn't work, try using the actual grid points.
            // 0, 0, may not be in the domain
            if (Float.isNaN(gridLevel)) {
                float[][] gridSamples = spatialSet.getSamples(false);
                zeroCoords = new float[][] {
                    { gridSamples[0][0] }, { gridSamples[1][0] },
                    { gridSamples[2][0] }
                };
                // convert the first point in the grid to the reference
                zeroCoords =
                    spatialSet.getCoordinateSystem().toReference(zeroCoords);
                // now, take that, substitute in the level
                zeroCoords = spatialSet.getCoordinateSystem().fromReference(
                    new float[][] {
                    { zeroCoords[0][0] }, { zeroCoords[1][0] }, { levVal }
                });
                gridLevel = zeroCoords[2][0];
            }
            */
            //gridLevel = (float) level.getValue(zRefType.getDefaultUnit());
            if (Float.isNaN(gridLevel)) {
                try {
                    spatialSet = (GriddedSet) Util.convertDomain(spatialSet,
                            spatialReferenceType, null);
                    isRefType = false;
                } catch (RemoteException re) {
                    throw new VisADException("Couldn't convert domain");
                }
            }
        }
        /*
        System.out.println("isRefType = " + isRefType);
        System.out.println("Real level = " + level);
        System.out.println("level type = " + type);
        System.out.println("spatial type = " + spatialType);
        System.out.println("spatial ref type = " + spatialReferenceType);
        System.out.println("ztype = " + zType);
        System.out.println("zUnit = " + zUnit);
        System.out.println("zreftype = " + zRefType);
        System.out.println("gridLevel = " + gridLevel);
        */


        Trace.call1("GridUtil making indices",
                    " Class=" + spatialSet.getClass().getName());
        // make new subset of grid positions showing where slice
        // in 3d space.
        int[] sizes   = spatialSet.getLengths();
        int   sizeX   = sizes[0];
        int   sizeY   = sizes[1];
        int[] indices = new int[sizeX * sizeY];
        for (int j = 0; j < sizeY; j++) {
            for (int i = 0; i < sizeX; i++) {
                //compute stride into 1D array of 3D data omit k
                int elem = i + j * sizeX;
                indices[elem] = elem;
            }
        }
        Trace.call2("GridUtil making indices");
        float[][] coords2D = spatialSet.indexToValue(indices);
        Arrays.fill(coords2D[2], gridLevel);
        // coords2D is in the native coordinates of the data grid (such as km)

        // make a Gridded3DSet of manifold dimension 2 -
        // where to sample in the 3D volume to make the planar cross section
        samplingSet = new Gridded3DSet(spatialSet.getType(), coords2D, sizeX,
                                       sizeY,
                                       spatialSet.getCoordinateSystem(),
                                       (isRefType == true)
                                       ? spatialSet.getCoordinateSystem()
                                           .getCoordinateSystemUnits()
                                       : spatialSet.getSetUnits(), spatialSet
                                           .getSetErrors(), false);
        // System.out.println("sampling set = " + samplingSet);
        Trace.call2("GridUtil.makeSliceFromLevel");
        return samplingSet;
    }

    /**
     * Create a Set describing a vertical slice, a set of locations, below the
     * two points specified, based on the spatial
     * domain.  If points are the same, this makes a vertical line.
     *
     * @param  spatialSet a GriddedSet of all data point locations
     * @param  start      starting point of slice
     * @param  end        ending point of slice
     *
     * @return a SampledSet a 3-D spatial set of 2-D manifold.
     *
     * @throws VisADException   problem creating slice
     */
    private static SampledSet makeSliceFromLatLonPoints(
            GriddedSet spatialSet, LatLonPoint start, LatLonPoint end)
            throws VisADException {

        boolean is3D = is3D(spatialSet);
        // make sure this is a sliceable grid
        if (is3D && (spatialSet.getManifoldDimension() != 3)) {
            throw new IllegalArgumentException(
                " Domain must have same manifold size as dimension");
        }
        if ( !isNavigated(spatialSet)) {
            throw new IllegalArgumentException("Domain is not georeferenced");
        }

        // for grid Field of form (time -> ((x,y,z) - > parm)
        // get the x,y,z which may be (x,y,z) in km, 
        // or row,col,level, or VisAD Latitude, Longitude, Altitude
        RealTupleType spatialType =
            ((SetType) spatialSet.getType()).getDomain();
        // System.out.println("spatialType = " + spatialType);

        // if native grid is already VisAD
        //   Latitude Longitude Altitude, the coordsys is null, and get null here
        RealTupleType spatialReferenceType =
            (spatialSet.getCoordinateSystem() != null)
            ? spatialSet.getCoordinateSystem().getReference()
            : null;
        // System.out.println("spatialRefType = " + spatialReferenceType);

        // now see whether the domain or the reference is the lat/lon
        boolean isLatLonDomain = ((spatialReferenceType == null) ||  // has to be in domain
            ((spatialType.getIndex(RealType.Latitude) != -1)
             && (spatialType.getIndex(RealType.Longitude) != -1)));
        // System.out.println("isLatLonDomain = " + isLatLonDomain);
        int latIndex, lonIndex;
        if (isLatLonDomain) {
            latIndex = spatialType.getIndex(RealType.Latitude);
            lonIndex = spatialType.getIndex(RealType.Longitude);
        } else {
            latIndex = spatialReferenceType.getIndex(RealType.Latitude);
            lonIndex = spatialReferenceType.getIndex(RealType.Longitude);
        }
        int       otherIndex = 3 - (latIndex + lonIndex);

        float[][] endpoints  = new float[(is3D)
                                         ? 3
                                         : 2][2];  // lat/lon/(possibly something) start/end
        float[][] domainCoords = spatialSet.getSamples(false);

        // start point location
        //System.out.println("     from start lat, lon "+ start);

        endpoints[latIndex][0] =
            (float) start.getLatitude().getValue(CommonUnit.degree);
        endpoints[lonIndex][0] =
            (float) start.getLongitude().getValue(CommonUnit.degree);

        if (is3D) {
            //endpoints[otherIndex][0] = 0.f;  // set vertical to 0
            endpoints[otherIndex][0] = domainCoords[otherIndex][0];  // set vertical to first point vertical
        }


        // end point location
        //System.out.println("     to end     lat, lon "+ end);

        endpoints[latIndex][1] =
            (float) end.getLatitude().getValue(CommonUnit.degree);
        endpoints[lonIndex][1] =
            (float) end.getLongitude().getValue(CommonUnit.degree);
        if (is3D) {
            //endpoints[otherIndex][1] = 0.f;  // set vertical to 0
            endpoints[otherIndex][1] = domainCoords[otherIndex][0];  // set vertical to first point vertical
        }
        float[][] savedEndpoints  = (float[][]) endpoints.clone();

        boolean   compatibleUnits = false;
        Unit[]    setUnits        = spatialSet.getSetUnits();
        Unit[]    refUnits        = Unit.copyUnitsArray(setUnits);

        if ( !isLatLonDomain) {  // convert to native
            CoordinateSystem cs = spatialSet.getCoordinateSystem();
            endpoints       = cs.fromReference(endpoints);
            refUnits        = cs.getReferenceUnits();
            compatibleUnits = Unit.canConvertArray(new Unit[] {
                setUnits[latIndex],
                setUnits[lonIndex] }, new Unit[] { refUnits[latIndex],
                    refUnits[lonIndex] });

        } else {  // make sure the units are right
            endpoints = Unit.convertTuple(endpoints,
                                          new Unit[] { CommonUnit.degree,
                    CommonUnit.degree, CommonUnit.meter }, setUnits, false);
        }


        // Interpolate a plane between end positions,
        // numLocs number of positions equal spaced along
        // a straight line in the native grid (as km) coordinate system.
        // (This straight cross section may appear curved in some
        // map projections.)
        // There will be numLocs number of positions horizontally,
        // a somewhat arbitrary but reasonable number.
        int   newi;
        float firstx, lastx, firsty, lasty, height, frac;
        float xval, yval;

        // get x and y values at first and last position: (kilometers)
        firstx = endpoints[lonIndex][0];
        lastx  = endpoints[lonIndex][1];

        firsty = endpoints[latIndex][0];
        lasty  = endpoints[latIndex][1];

        // kludge for EmpericalCoordinateSystem
        // if the cs returns null values (because of the vertical dimension
        // assume that the spatial dimensions are the same and use those
        if (Float.isNaN(firstx) || Float.isNaN(lastx) || Float.isNaN(firsty)
                || Float.isNaN(lasty)) {
            if ( !compatibleUnits) {
                // try to convert to reference and make slice there
                CoordinateSystem cs = spatialSet.getCoordinateSystem();
                if (cs != null) {
                    if (cs instanceof EmpiricalCoordinateSystem) {
                        spatialSet =
                            ((EmpiricalCoordinateSystem) cs)
                                .getReferenceSet();
                        spatialType =
                            ((SetType) spatialSet.getType()).getDomain();
                        setUnits = spatialSet.getSetUnits();

                    } else {
                        try {
                            spatialSet =
                                (GriddedSet) Util.convertDomain(spatialSet,
                                    spatialReferenceType, null);
                            spatialType =
                                ((SetType) spatialSet.getType()).getDomain();
                            setUnits = spatialSet.getSetUnits();
                        } catch (RemoteException re) {
                            throw new VisADException(
                                "Couldn't convert domain");
                        }
                    }
                    isLatLonDomain = true;
                    domainCoords   = spatialSet.getSamples(false);
                } else {
                    throw new VisADException("unable to make slice");
                }
            }
            savedEndpoints = Unit.convertTuple(savedEndpoints,
                    new Unit[] { CommonUnit.degree,
                                 CommonUnit.degree,
                                 CommonUnit.meter }, refUnits, false);
            firstx = savedEndpoints[lonIndex][0];
            lastx  = savedEndpoints[lonIndex][1];
            firsty = savedEndpoints[latIndex][0];
            lasty  = savedEndpoints[latIndex][1];
        }

        //float[][] domainCoords = spatialSet.getSamples(false);

        /*
        System.out.println("        horiz native value    "+firstx+
                          " to  "+lastx);
        System.out.println("        vertical native value "+firsty+
                      " to "+lasty);
        */


        int numLocs;
        int sizeX = spatialSet.getLengths()[lonIndex];
        int sizeY = spatialSet.getLengths()[latIndex];
        int sizeZ = (is3D)
                    ? spatialSet.getLengths()[otherIndex]
                    : 1;
        //System.out.println("size xyz = " + sizeX + "," + sizeY + "," +sizeZ);
        if ( !start.equals(end)) {
            float[] highs   = spatialSet.getHi();
            float[] lows    = spatialSet.getLow();
            float   numPerX = (highs[lonIndex] - lows[lonIndex]) / sizeX;
            float   numPerY = (highs[latIndex] - lows[latIndex]) / sizeY;

            int numXPoints  = Math.round(Math.abs(firstx - lastx) / numPerX);
            int numYPoints  = Math.round(Math.abs(firsty - lasty) / numPerY);
            numLocs = Math.max(1, Math.min(Math.max(numXPoints, numYPoints),
                                           sizeX));
        } else {  // points are the same
            numLocs = 1;
        }
        //System.out.println("        numLocs "+numLocs);

        // make a working array for x,y,z positions in 3D space
        // coords2D is in the native coordinates of the data grid (such as km),
        float[][] coords2D = new float[is3D
                                       ? 3
                                       : 2][numLocs * sizeZ];

        //System.out.println("  x vals are ");

        //  Make a Set of locations for data points
        //    loop over all heights
        for (int k = 0; k < sizeZ; k++) {

            // the index for this height value in domainCoords
            int zindex = k * sizeY * sizeX;

            // the value 
            // (can be any kind of height indication, even atmospheric pressure)
            height = (is3D)
                     ? domainCoords[otherIndex][zindex]
                     : 0f;

            // compute positions x,y,z for points in cross section (km)
            // these define positions in the 2d cross-section plane
            // cutting through the 3d x,y,z coordinate system
            for (int i = 0; i < numLocs; i++) {
                // index of this point in array coords2D
                newi = i + k * numLocs;

                // fractional distance along the cross section:
                frac = (numLocs == 1)
                       ? 1
                       : (float) i / (numLocs - 1);

                // x value for point:
                xval = (float) (firstx + ((lastx - firstx) * frac));

                // ensure its inside native grid area if lat-lon coordinates
                //if (k==0) System.out.print("    xval "+xval);
                xval = (float) normalizeLongitude(spatialSet, xval);
                //if (k==0) System.out.println(" -> "+xval);

                coords2D[lonIndex][newi] = xval;

                // y value
                yval = (float) (firsty + ((lasty - firsty) * frac));
                coords2D[latIndex][newi] = yval;

                // height value - native grid units
                if (is3D) {
                    coords2D[otherIndex][newi] = height;
                }
            }
        }

        // make a Gridded3DSet of manifold dimension 2 -
        // where to sample in the DATA grid 
        // 3D volume (not on display map) to make the planar cross section
        Unit[] units = null;
        if (isLatLonDomain) {
            units = setUnits;
        } else {
            Unit[] csUnits =
                spatialSet.getCoordinateSystem().getCoordinateSystemUnits();
            units = (is3D)
                    ? new Unit[] { csUnits[0], csUnits[1], setUnits[2] }
                    : new Unit[] { csUnits[0], csUnits[1] };
        }

        GriddedSet samplingSet = ( !is3D)
                                 ? (GriddedSet) new Gridded2DSet(spatialType,
                                     coords2D, numLocs,
                                     spatialSet.getCoordinateSystem(), units,
                                     spatialSet.getSetErrors(), false)
                                 : (numLocs > 1)
                                   ? (GriddedSet) new Gridded3DSet(
                                       spatialType, coords2D, numLocs, sizeZ,
                                       spatialSet.getCoordinateSystem(),
                                       units, spatialSet.getSetErrors(),
                                       false)
                                   : (GriddedSet) new Gridded3DSet(
                                       spatialType, coords2D, sizeZ,
                                       spatialSet.getCoordinateSystem(),
                                       units, spatialSet.getSetErrors(),
                                       false);
        // System.out.println("llslice = " + samplingSet);
        return samplingSet;
    }



    /**
     * Sample the grid at this point using the VisAD resampling defaults
     *
     * @param grid     grid to sample
     * @param point    tuple describing the point
     * @return   sampled grid
     *
     * @throws VisADException   VisAD error
     */
    private static FieldImpl sampleAtPoint(FieldImpl grid, RealTuple point)
            throws VisADException {
        return sampleAtPoint(grid, point, DEFAULT_SAMPLING_MODE);
    }



    /**
     * sample the grid at this point using "method' provided, one of
     * NEAREST_NEIGHBOR or WEIGHTED_AVERAGE; errors not considered.
     *
     * @param grid      grid to sample
     * @param point     point to sample at
     * @param samplingMode   sampling mode
     * @return   sampled grid
     *
     * @throws VisADException   problem sampling
     */
    private static FieldImpl sampleAtPoint(FieldImpl grid, RealTuple point,
                                           int samplingMode)
            throws VisADException {
        return sampleAtPoint(grid, point, samplingMode, DEFAULT_ERROR_MODE);
    }

    /**
     * sample the grid at this point using "method' provided, one of
     * NEAREST_NEIGHBOR or WEIGHTED_AVERAGE; errors not considered.
     *
     * @param grid      grid to sample
     * @param point     point to sample at
     * @param samplingMode   sampling mode
     * @param  errorMode Data.NO_ERRORS, Data.DEPENDENT, Data.INDEPENDENT
     * @return   sampled grid
     *
     * @throws VisADException   problem sampling
     */
    private static FieldImpl sampleAtPoint(FieldImpl grid, RealTuple point,
                                           int samplingMode, int errorMode)
            throws VisADException {
        FieldImpl sampledFI = null;
        // System.out.println("sampling at " + point);
        try {
            if ( !isSequence(grid)) {
                Data value = grid.evaluate(point, samplingMode, errorMode);
                RealType     index = RealType.getRealType("index");
                SingletonSet ss = new SingletonSet(new RealTuple(new Real[] {
                                      new Real(index, 0) }));
                sampledFI = new FieldImpl(new FunctionType(index,
                        value.getType()), ss);
                sampledFI.setSample(0, value, false);
            } else {  // some sort of sequence - evaluate each
                //                System.err.println("is sequence");
                Set sequenceDomain = Util.getDomainSet(grid);
                for (int i = 0; i < sequenceDomain.getLength(); i++) {
                    Data sample =
                        ((FlatField) grid.getSample(i)).evaluate(point,
                            samplingMode, errorMode);
                    if (i == 0) {  // set up the functiontype
                        FunctionType sampledType =
                            new FunctionType(((SetType) sequenceDomain
                                .getType()).getDomain(), sample.getType());
                        sampledFI = new FieldImpl(sampledType,
                                sequenceDomain);
                    }
                    sampledFI.setSample(i, sample, false);
                }
            }
        } catch (RemoteException re) {
            throw new VisADException("problem sampling remote field " + re);
        }
        return sampledFI;
    }

    /**
     * Make sure a longitude value for use in
     * a spatial domain Set with Longitude in the spatial domain
     * is inside the spatial domain's longitude range.
     * If not, then adjust so that it is.
     * If domain not of such coordinates, do nothing and return input value.
     *
     * @param domain    domain  set of value for normalization
     * @param lon       longitude
     * @return   normalized longitude
     *
     * @throws VisADException   problem accessing set
     */
    private static Real normalizeLongitude(SampledSet domain, Real lon)
            throws VisADException {
        double lonValue = normalizeLongitude(domain, lon.getValue(),
                                             lon.getUnit());
        return lon.cloneButValue(lonValue);

    }

    /**
     * Make sure a longitude value for use in
     * a spatial domain Set with Longitude in the spatial domain
     * is inside the spatial domain's longitude range.
     * If not, then adjust so that it is.
     * If domain not of such coordinates, do nothing and return input value.
     *
     * @param domain    domain  set of value for normalization
     * @param lon       longitude values
     * @return   normalized longitude
     *
     * @throws VisADException   problem accessing set
     */
    private static double normalizeLongitude(SampledSet domain, double lon)
            throws VisADException {
        return normalizeLongitude(domain, lon, null);
    }

    /**
     * Make sure a longitude value for use in
     * a spatial domain Set with Longitude in the spatial domain
     * is inside the spatial domain's longitude range.
     * If not, then adjust so that it is.
     * If domain not of such coordinates, do nothing and return input value.
     *
     * @param domain    domain  set of value for normalization
     * @param lon       longitude values
     * @param lonUnit   longitude unit
     * @return   normalized longitude
     *
     * @throws VisADException   problem accessing set
     */
    private static double normalizeLongitude(SampledSet domain, double lon,
                                             Unit lonUnit)
            throws VisADException {
        int lonindex = isLatLonOrder(domain)
                       ? 1
                       : 0;
        int latindex = (lonindex == 0)
                       ? 1
                       : 0;

        // check to see if domain really has lat/lon
        RealType lonType =
            (RealType) ((SetType) domain.getType()).getDomain().getComponent(
                lonindex);
        RealType latType =
            (RealType) ((SetType) domain.getType()).getDomain().getComponent(
                latindex);
        if ( !(lonType.equalsExceptNameButUnits(RealType.Longitude)
                && latType.equalsExceptNameButUnits(RealType.Latitude))) {
            return lon;
        }
        if (lonUnit == null) {
            lonUnit = domain.getSetUnits()[lonindex];
        }
        lon = (float) CommonUnit.degree.toThis(lon, lonUnit);

        float low = domain.getLow()[lonindex];
        low = (float) CommonUnit.degree.toThis(low, lonUnit);
        float hi = domain.getHi()[lonindex];
        hi = (float) CommonUnit.degree.toThis(hi, lonUnit);

        while ((float) lon < low && (float) lon < hi) {
            lon += 360;
        }

        while ((float) lon > hi && (float) lon > low) {
            lon -= 360;
        }
        return (float) lonUnit.toThis(lon, CommonUnit.degree);
    }

    /**
     * Create a MapProjection from the domain set
     *
     * @param domainSet    domain set
     * @return  MapProjection relating to navigation in set (or null)
     *
     * @throws VisADException   problem creating the MapProjection
     */
    private static MapProjection makeMapProjection(SampledSet domainSet)
            throws VisADException {
        boolean isLatLon = isLatLonOrder(domainSet);
        float[] lows     = domainSet.getLow();
        float[] highs    = domainSet.getHi();
        int     latIndex = (isLatLon == true)
                           ? 0
                           : 1;
        int     lonIndex = 1 - latIndex;
        float   x        = lows[lonIndex];
        float   y        = lows[latIndex];
        float   width    = highs[lonIndex] - x;
        float   height   = highs[latIndex] - y;
        if ((width == 0.f) && (height == 0.f)) {  // single point grid
            x      = x - .5f;
            width  = 1.f;
            y      = y - .5f;
            height = 1.f;
        }
        Unit[]    setUnits = domainSet.getSetUnits();
        float[][] xy       = new float[][] {
            { x, width }, { y, height }
        };
        xy = Unit.convertTuple(xy, new Unit[] { setUnits[lonIndex],
                setUnits[latIndex] }, new Unit[] { CommonUnit.degree,
                CommonUnit.degree }, false);
        return new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple,
                                        new Rectangle2D.Float(xy[0][0],
                                            xy[1][0], xy[0][1], xy[1][1]));
    }

    /**
     * Return a MapProjection that relates to the Radar*DCoordinateSystem.
     *
     * @param radarCS radar coordinate system (Radar2DCoordinateSystem or
     *                Radar3DCoordinateSystem)
     *
     * @return MapProjection corresponding to the radar CS
     *
     * @throws VisADException problem creating MapProjection.
     */
    public static MapProjection makeRadarMapProjection(
            CoordinateSystem radarCS)
            throws VisADException {
        if ( !((radarCS instanceof Radar2DCoordinateSystem)
                || (radarCS instanceof Radar3DCoordinateSystem))) {
            throw new RuntimeException("not a radar cs");
        }
        float[] lla = (radarCS instanceof Radar2DCoordinateSystem)
                      ? ((Radar2DCoordinateSystem) radarCS).getCenterPoint()
                      : ((Radar3DCoordinateSystem) radarCS).getCenterPoint();
        return new ucar.visad.RadarMapProjection(lla[0], lla[1]);
    }

    /**
     * Resample the grid at the positions defined by a SampledSet using
     * the default methods and error propagation.
     *
     * @param  grid   grid to resample (must be a valid 3D grid)
     * @param  subDomain  set of points to sample on.  It must be compatible
     *         with the spatial domain of the grid.
     *
     * @return  a FieldImpl the grid representing the values
     *          of the original grid at the
     *          points defined by subDomain.  If this is a sequence of grids
     *          it will be a sequence of the resamples.
     *
     * @throws  VisADException  invalid subDomain or some other problem
     */
    public static FieldImpl resampleGrid(FieldImpl grid, SampledSet subDomain)
            throws VisADException {
        return resampleGrid(grid, subDomain, DEFAULT_SAMPLING_MODE,
                            DEFAULT_ERROR_MODE);
    }

    /**
     * Resample the grid at the positions defined by a SampledSet using
     * the method specified and default error propagation.
     *
     * @param  grid   grid to resample (must be a valid 3D grid)
     * @param  subDomain  set of points to sample on.  It must be compatible
     *         with the spatial domain of the grid.
     * @param  samplingMode  sampling method to use for slicing
     *
     * @return  a FieldImpl the grid representing the values
     *          of the original grid at the
     *          points defined by sampling set.  If this is a sequence of grids
     *          it will be a sequence of the subsamples.
     *
     * @throws  VisADException  invalid subDomain or some other problem
     */
    public static FieldImpl resampleGrid(FieldImpl grid,
                                         SampledSet subDomain,
                                         int samplingMode)
            throws VisADException {
        return resampleGrid(grid, subDomain, samplingMode,
                            DEFAULT_ERROR_MODE);
    }

    /**
     * Method to get the center point of a grid's spatial domain
     * as a RealTuple.
     *
     * @param grid   grid to evaluate
     *
     * @return center point (x,y,z) of the grid in native coordinates.
     *         If the domain has a CoordinateSystem (e.g.,
     *         &nbsp;(x,y,z) -> (lat,lon,alt)&nbsp; that will be included
     *         in the returned tuple.
     *
     * @throws VisADException problem accessing the data
     */
    public static RealTuple getCenterPoint(FieldImpl grid)
            throws VisADException {
        return getCenterPoint(getSpatialDomain(grid));
    }


    /**
     * Method to get the center point of a spatial domain
     * as a RealTuple.
     *
     * @param spatialDomain   domain to evaluate
     *
     * @return center point (x,y,z) of the domain in native coordinates.
     *         If the domain has a CoordinateSystem (e.g.,
     *         &nbsp;(x,y,z) -> (lat,lon,alt)&nbsp; that will be included
     *         in the returned tuple.
     *
     * @throws VisADException problem accessing the data
     */
    public static RealTuple getCenterPoint(SampledSet spatialDomain)
            throws VisADException {
        float[]   highs  = spatialDomain.getHi();
        float[]   lows   = spatialDomain.getLow();
        float[][] values = new float[highs.length][1];

        for (int i = 0; i < highs.length; i++) {
            values[i][0] = lows[i] + (highs[i] - lows[i]) / 2.f;
        }
        int index = 0;
        if (isSinglePointDomain(spatialDomain)) {
            index = spatialDomain.getLength() / 2;
        } else {
            int[] indices = spatialDomain.valueToIndex(values);
            index = indices[0];
        }
        RealTuple point = null;
        try {
            point = DataUtility.getSample(spatialDomain, index);
        } catch (RemoteException re) {}
        return point;
    }

    /**
     * Get the latitude/longitude point at the center of the grid.
     *
     * @param grid grid to evaluate
     *
     * @return center lat/lon or null if not navigated
     *
     * @throws VisADException problem accessing the data
     */
    public static LatLonPoint getCenterLatLonPoint(FieldImpl grid)
            throws VisADException {
        return getCenterLatLonPoint(getSpatialDomain(grid));
    }

    /**
     * Get the latitude/longitude point at the center of the domain.
     *
     * @param spatialDomain domain to evaluate
     *
     * @return center lat/lon or null if not navigated
     *
     * @throws VisADException problem accessing the data
     */
    public static LatLonPoint getCenterLatLonPoint(SampledSet spatialDomain)
            throws VisADException {
        RealTuple   nativeCoords = getCenterPoint(spatialDomain);
        LatLonPoint latlon       = null;
        try {
            SingletonSet ss = new SingletonSet(nativeCoords);
            if (isNavigated(ss)) {  // has lat/lon
                int       latIndex     = isLatLonOrder(ss)
                                         ? 0
                                         : 1;
                int       lonIndex     = 1 - latIndex;
                RealTuple latLonCoords = nativeCoords;
                if (ss.getCoordinateSystem() != null) {
                    SampledSet latLonSet =
                        Util.convertDomain(
                            ss, ss.getCoordinateSystem().getReference(),
                            null);
                    latLonCoords = DataUtility.getSample(latLonSet, 0);
                }
                latlon = new LatLonTuple(
                    (Real) latLonCoords.getComponent(latIndex),
                    (Real) latLonCoords.getComponent(lonIndex));
            }
        } catch (RemoteException re) {}
        return latlon;
    }

    /**
     * Resample the grid at the positions defined by a SampledSet.
     *
     * @param  grid   grid to resample (must be a valid 3D grid)
     * @param  subDomain  set of points to sample on.  It must be compatible
     *         with the spatial domain of the grid.
     * @param  samplingMode sampling method to use for slicing
     * @param  errorMode  error method to use for error propagation
     *
     * @return  a FieldImpl the grid representing the values
     *          of the original grid at the
     *          points defined by subDomain.  If this is a sequence of grids
     *          it will be a sequence of the subsets.
     *
     * @throws  VisADException  invalid subDomain or some other problem
     */
    public static FieldImpl resampleGrid(FieldImpl grid,
                                         SampledSet subDomain,
                                         int samplingMode, int errorMode)
            throws VisADException {
        long      t1     = System.currentTimeMillis();
        FieldImpl result = resampleGridInner(grid, subDomain, samplingMode,
                                             errorMode);
        long t2 = System.currentTimeMillis();
        //System.err.println("Time:" + (t2 - t1));
        return result;
    }



    /**
     * Resample the grid at the positions defined by a SampledSet.
     *
     * @param  grid   grid to resample (must be a valid 3D grid)
     * @param  subDomain  set of points to sample on.  It must be compatible
     *         with the spatial domain of the grid.
     * @param  samplingMode sampling method to use for slicing
     * @param  errorMode  error method to use for error propagation
     *
     * @return  a FieldImpl the grid representing the values
     *          of the original grid at the
     *          points defined by subDomain.  If this is a sequence of grids
     *          it will be a sequence of the subsets.
     *
     * @throws  VisADException  invalid subDomain or some other problem
     */
    private static FieldImpl resampleGridInner(FieldImpl grid,
            SampledSet subDomain, int samplingMode, int errorMode)
            throws VisADException {

        Trace.call1("GridUtil.resampleGrid");

        SampledSet spatialDomain = getSpatialDomain(grid);
        if ((spatialDomain.getDimension() != subDomain.getDimension())
                && (spatialDomain.getManifoldDimension()
                    != subDomain.getManifoldDimension())) {
            throw new IllegalArgumentException(
                "resampleGrid: subDomain and grid dimensions are incompatible");
        }

        FieldImpl sampledFI = null;
        try {
            if (isSinglePointDomain(grid)) {
                SampledSet set = getSpatialDomain(grid);
                if (set instanceof SingletonSet) {  // single level
                    return grid;
                } else {
                    float[][] domainVals = set.getSamples(true);
                    float[][] sliceVals  = subDomain.getSamples(true);
                    if (subDomain.getCoordinateSystem() != null) {
                        CoordinateSystem cs = subDomain.getCoordinateSystem();
                        sliceVals = cs.fromReference(sliceVals);
                    }
                    float[] verticalLevels = sliceVals[2];
                    int     index          = 0;
                    if (verticalLevels.length != 1) {
                        // TODO: subsample
                        return grid;
                    } else {  // do a nearest neighbor
                        int[] indices = QuickSort.sort(domainVals[2]);
                        index = Math.abs(Arrays.binarySearch(domainVals[2],
                                verticalLevels[0]));
                        if (index >= domainVals[2].length) {
                            index = domainVals[2].length - 1;
                        }
                        index = indices[index];
                    }
                    if (getTimeSet(grid) == null) {  // single time
                        FunctionType ffType = (FunctionType) grid.getType();
                        sampledFI = new FlatField(ffType, subDomain);
                        sampledFI.setSample(0, grid.getSample(index), false);
                    } else {
                        Set timeSet = getTimeSet(grid);
                        sampledFI =
                            new FieldImpl((FunctionType) grid.getType(),
                                          timeSet);
                        FunctionType subType =
                            (FunctionType) grid.getSample(0).getType();
                        for (int i = 0; i < timeSet.getLength(); i++) {
                            FlatField subField =
                                (FlatField) grid.getSample(i);
                            if (i == 0) {
                                subType = (FunctionType) subField.getType();
                            }
                            FlatField ff = new FlatField(subType, subDomain);
                            ff.setSample(0, subField.getSample(index));
                            sampledFI.setSample(i, ff, false);
                        }
                    }
                }
                return sampledFI;
            }

            if ( !isSequence(grid)) {
                sampledFI = (FlatField) grid.resample(subDomain,
                        samplingMode, errorMode);
            } else {  // some sort of sequence - resample each
                Set sequenceDomain = Util.getDomainSet(grid);
                Trace.call1("GridUtil.sampleLoop",
                            " Length: " + sequenceDomain.getLength());
                for (int i = 0; i < sequenceDomain.getLength(); i++) {
                    Trace.call1("GridUtil getSample");
                    FieldImpl subField = (FieldImpl) grid.getSample(i, false);
                    Trace.call2("GridUtil getSample");
                    FieldImpl sampledField = null;
                    if ( !isSequence(subField)) {

                        Trace.call1("GridUtil resample",
                                    " Length=" + subField.getLength());
                        sampledField =
                            (FieldImpl) subField.resample(subDomain,
                                samplingMode, errorMode);
                        Trace.call2("GridUtil resample");

                    } else {  // inner sequence (e.g. ensembles)
                        Set innerSequenceDomain = subField.getDomainSet();
                        Trace.call1("GridUtil resample inner sequence",
                                    " Length= "
                                    + innerSequenceDomain.getLength());
                        for (int j = 0; j < innerSequenceDomain.getLength();
                                j++) {
                            FlatField innerSubField =
                                (FlatField) subField.getSample(j, false);
                            if (innerSubField == null) {
                                continue;
                            }
                            FlatField innerSampledField =
                                (FlatField) innerSubField.resample(subDomain,
                                    samplingMode, errorMode);
                            if (innerSampledField == null) {
                                continue;
                            }
                            if (sampledField == null) {
                                FunctionType innerType =
                                    new FunctionType(
                                        DataUtility
                                            .getDomainType(
                                                innerSequenceDomain), innerSampledField
                                                    .getType());
                                sampledField = new FieldImpl(innerType,
                                        innerSequenceDomain);
                            }
                            sampledField.setSample(j, innerSampledField,
                                    false);
                        }
                        Trace.call2("GridUtil resample inner sequence");
                    }
                    if ((sampledField != null) && (sampledFI == null)) {  // set up the functiontype
                        FunctionType sampledType =
                            new FunctionType(
                                DataUtility.getDomainType(sequenceDomain),
                                sampledField.getType());
                        sampledFI = new FieldImpl(sampledType,
                                sequenceDomain);
                    }
                    Trace.call1("GridUtil setSample");
                    if (sampledField != null) {
                        sampledFI.setSample(i, sampledField, false);
                    }
                    Trace.call2("GridUtil setSample");
                }
                Trace.call2("GridUtil.sampleLoop");
            }
        } catch (RemoteException re) {
            throw new VisADException("problem resampling remote field " + re);
        }
        Trace.call2("GridUtil.resampleGrid");
        return sampledFI;
    }

    /**
     * Resample a grid with a 2D manifold.  We need to do this because our
     * point might be in 3 space
     *
     * @param grid           grid to sample
     * @param subDomain      sampling domain
     * @param skipx          x skip factor
     * @param skipy          y skip factor
     * @return
     *
     * @throws VisADException       problem in resampling
     */
    private static FieldImpl resample2DManifold(FieldImpl grid,
            SampledSet subDomain, int skipx, int skipy)
            throws VisADException {
        SampledSet spatialDomain = getSpatialDomain(grid);
        if ((spatialDomain.getDimension() != subDomain.getDimension())
                && (spatialDomain.getManifoldDimension()
                    != subDomain.getManifoldDimension())) {
            throw new IllegalArgumentException(
                "resampleGrid: subDomain and grid dimensions are incompatible");
        }
        FieldImpl sampledFI = null;
        try {
            if ( !isSequence(grid)) {
                sampledFI = new FlatField((FunctionType) grid.getType(),
                                          subDomain);
                sampledFI.setSamples(getSubValues(getSpatialDomain(grid),
                        grid.getFloats(), skipx, skipy));
            } else {  // some sort of sequence - resample each
                Set        sequenceDomain = Util.getDomainSet(grid);
                SampledSet ss             = getSpatialDomain(grid);
                FieldImpl  sampledField   = null;
                for (int i = 0; i < sequenceDomain.getLength(); i++) {
                    FieldImpl sample = (FieldImpl) grid.getSample(i);
                    if (sample == null) {
                        continue;
                    }
                    if ( !GridUtil.isSequence(sample)) {
                        sampledField =
                            new FlatField((FunctionType) sample.getType(),
                                          subDomain);
                        sampledField.setSamples(getSubValues(ss,
                                sample.getFloats(), skipx, skipy));

                    } else {  // ensembles and such
                        Set ensDomain = sample.getDomainSet();
                        sampledField =
                            new FieldImpl((FunctionType) sample.getType(),
                                          ensDomain);
                        for (int j = 0; j < ensDomain.getLength(); j++) {
                            FlatField innerField =
                                (FlatField) sample.getSample(j, false);
                            if (innerField == null) {
                                continue;
                            }
                            FlatField sampledFF =
                                new FlatField(
                                    (FunctionType) innerField.getType(),
                                    subDomain);
                            sampledFF.setSamples(getSubValues(ss,
                                    innerField.getFloats(), skipx, skipy));
                            sampledField.setSample(j, sampledFF);
                        }
                    }
                    if ((sampledField != null) && (sampledFI == null)) {  // set up the functiontype
                        FunctionType sampledType =
                            new FunctionType(((SetType) sequenceDomain
                                .getType()).getDomain(), sampledField
                                    .getType());
                        sampledFI = new FieldImpl(sampledType,
                                sequenceDomain);
                    }
                    if (sampledField != null) {
                        sampledFI.setSample(i, sampledField, false);
                    }
                }
            }
        } catch (RemoteException re) {
            throw new VisADException("problem resampling remote field " + re);
        }
        return sampledFI;
    }

    /**
     * Get the subdomain values based on the skip factors
     *
     * @param domainSet       set to sample
     * @param values          input values
     * @param skipx           x skip factor
     * @param skipy           y skip factor
     * @return
     *
     * @throws VisADException
     */
    private static float[][] getSubValues(SampledSet domainSet,
                                          float[][] values, int skipx,
                                          int skipy)
            throws VisADException {
        int       sizeX      = ((GriddedSet) domainSet).getLength(0);
        int       sizeY      = ((GriddedSet) domainSet).getLength(1);
        float[][] subSamples =
            new float[values.length][(1 + (sizeX - 1) / skipx) * (1 + (sizeY - 1) / skipy)];
        for (int m = 0; m < values.length; m++) {
            int l = 0;
            for (int j = 0; j < sizeY; j += skipy) {
                for (int i = 0; i < sizeX; i += skipx) {
                    //compute stride into 1D array of 3D data
                    int elem = i + (j * sizeX);

                    subSamples[m][l] = values[m][elem];
                    l++;

                }
            }
        }
        return subSamples;
    }


    /**
     * Get the altitude corresponding to the level specified using
     * the domain of the grid.
     *
     * @param  grid   grid to use
     * @param  altitude  altitude to convert.
     *
     * @return  corresponding value of the vertical dimension of the grid.
     *          May be missing if conversion can't happen.
     *
     *
     * @throws VisADException
     */
    public static Real getLevel(FieldImpl grid, Real altitude)
            throws VisADException {
        if ((altitude == null) || (grid == null)) {
            throw new IllegalArgumentException(
                "GridUtil.getLevel(): grid and level must not be null");
        }
        if ( !is3D(grid)) {
            throw new IllegalArgumentException(
                "GridUtil.getLevel(): Grid must be 3D");
        }
        if ( !(Unit.canConvert(altitude.getUnit(), CommonUnit.meter))) {
            throw new IllegalArgumentException(
                "GridUtil.getLevel(): alitude units must be convertible with meters");
        }
        double     levVal    = Double.NaN;
        SampledSet domainSet = getSpatialDomain(grid);
        RealType   zType     = getVerticalType(domainSet);
        Unit       zUnit     = getVerticalUnit(domainSet);
        if (Unit.canConvert(zUnit, altitude.getUnit())) {
            levVal = altitude.getValue(zUnit);
        } else {               // better have a CoordinateSystem
            CoordinateSystem cs = domainSet.getCoordinateSystem();
            if (cs != null) {  // better be l/l/alt
                float[][] xyz = cs.fromReference(new float[][] {
                    { Float.NaN }, { Float.NaN },
                    { (float) altitude.getValue(CommonUnit.meter) }
                });
                levVal = zUnit.toThis(xyz[2][0],
                                      cs.getCoordinateSystemUnits()[2]);
            } else {
                throw new VisADException("Can't convert to a level");
            }
        }
        return new Real(zType, levVal, zUnit);
    }

    /**
     * Get the altitude corresponding to the level specified using
     * the domain of the grid.
     *
     * @param  grid   grid to use
     * @param  level  must be compatible (unit wise) with vertical coordinate
     *                of the grid
     *
     * @return  altitude (in m) corresponding to level using coordinate
     *          system of the grid's domain.  May be missing if conversion
     *          can't happen.
     *
     *
     * @throws VisADException    VisAD error
     */
    public static Real getAltitude(FieldImpl grid, Real level)
            throws VisADException {

        if ((level == null) || (grid == null)) {
            throw new IllegalArgumentException(
                "GridUtil.getAltitude(): grid and level must not be null");
        }
        if ( !is3D(grid)) {
            throw new IllegalArgumentException(
                "GridUtil.getAltitude(): Grid must be 3D");
        }

        double altVal = Double.NaN;
        if (Unit.canConvert(level.getUnit(), CommonUnit.meter)) {
            altVal = level.getValue(CommonUnit.meter);
        } else {
            SampledSet domainSet = getSpatialDomain(grid);
            Unit       zUnit     = getVerticalUnit(domainSet);
            if ( !Unit.canConvert(zUnit, level.getUnit())) {
                throw new VisADException(
                    "level units not compatible with grid units");
            }
            CoordinateSystem domainCS = domainSet.getCoordinateSystem();
            if (domainCS != null) {
                float[][] samples   = domainSet.getSamples(false);
                Unit[]    csUnits   = domainCS.getCoordinateSystemUnits();
                float[][] latlonalt = domainCS.toReference(new float[][] {
                    { samples[0][0] }, { samples[1][0] },
                    { (float) level.getValue(csUnits[2]) }
                });

                altVal = latlonalt[2][0];
            }
        }
        return new Real(RealType.Altitude, altVal);
    }

    /**
     * Get the RealType of the vertical dimension of the spatial domain
     * of the grid.
     *
     * @param  grid  grid to check
     *
     * @return RealType of the vertical dimension of the grid's spatial domain
     *
     * @throws VisADException  problem getting the type
     */
    public static RealType getVerticalType(FieldImpl grid)
            throws VisADException {
        return getVerticalType(getSpatialDomain(grid));
    }

    /**
     * Get the unit of the vertical dimension of the domain set.
     *
     * @param domainSet  domainSet to check
     *
     * @return RealType of the vertical dimension domainSet
     *
     * @throws VisADException  problem getting the type
     */
    public static RealType getVerticalType(SampledSet domainSet)
            throws VisADException {

        if ( !is3D(domainSet)) {
            throw new IllegalArgumentException(
                "GridUtil.getVerticalType(): Not a 3D domain");
        }
        return (RealType) ((SetType) domainSet.getType()).getDomain()
            .getComponent(2);

    }

    /**
     * Get the unit of the vertical dimension of the spatial domain of the grid.
     *
     * @param  grid  domain to check
     *
     * @return unit of the raw vertical data in the grid's domain set.
     *
     * @throws VisADException  problem getting the unit
     */
    public static Unit getVerticalUnit(FieldImpl grid) throws VisADException {
        return getVerticalUnit(getSpatialDomain(grid));
    }

    /**
     * Get the unit of the vertical dimension of the domain.
     *
     * @param  domainSet  domain to check
     *
     * @return unit of the raw data in the domainSet
     *
     * @throws VisADException  problem getting the unit
     */
    public static Unit getVerticalUnit(SampledSet domainSet)
            throws VisADException {

        if ( !is3D(domainSet)) {
            throw new IllegalArgumentException(
                "GridUtil.getVerticalUnit(): Not a 3D grid");
        }

        return domainSet.getSetUnits()[2];
    }


    /**
     * Determine whether the grid in question can be sliced at
     * the level specified (i.e., units or CS allows this)
     *
     * @param grid  grid in question
     * @param level  level in question
     *
     * @return true if the level is compatible with the grid.
     *
     * @throws VisADException   problem creating VisAD object
     */
    public static boolean canSliceAtLevel(FieldImpl grid, Real level)
            throws VisADException {
        return canSliceAtLevel(getSpatialDomain(grid), level);
    }

    /**
     * Determine whether the set in question can be sliced at
     * the level specified (i.e., units or CS allows this)
     *
     * @param spatialSet  domain set to check
     * @param level  level in question
     *
     * @return true if the level is compatible with the grid.
     *
     * @throws VisADException   problem creating VisAD object
     */
    public static boolean canSliceAtLevel(SampledSet spatialSet, Real level)
            throws VisADException {

        Trace.call1("GridUtil.canSliceAtLevel");
        if ((spatialSet == null) || (level == null)) {
            return false;
        }

        // make sure this is a sliceable grid
        if (spatialSet.getManifoldDimension() != 3) {
            return false;
        }

        // check the level type against the domain type and reference
        RealType type = (RealType) level.getType();  // level type
        if (type.equals(RealType.Generic)) {
            return true;
        }

        RealTupleType spatialType =
            ((SetType) spatialSet.getType()).getDomain();
        RealTupleType spatialReferenceType =
            (spatialSet.getCoordinateSystem() != null)
            ? spatialSet.getCoordinateSystem().getReference()
            : null;


        RealType zType     = getVerticalType(spatialSet);
        Unit     zUnit     = getVerticalUnit(spatialSet);

        RealType zRefType  = (spatialReferenceType != null)
                             ? (RealType) spatialReferenceType.getComponent(2)
                             : null;  // ref Z
        boolean  isRefType = type.equalsExceptNameButUnits(zRefType);

        if ( !(type.equalsExceptNameButUnits(zType) || isRefType)) {
            return false;
        }

        return true;
    }

    /**
     * Find min and max of range data in any VisAD FlatField
     *
     * @param field       a VisAD FlatField.  Cannot be null
     * @return  the range of the data.  Dimension is the number of parameters
     *          in the range of the flat field
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static Range[] fieldMinMax(visad.FlatField field)
            throws VisADException, RemoteException {
        if (field instanceof CachedFlatField) {
            return makeRanges(((CachedFlatField) field).getRanges());
        }


        float   allValues[][] = field.getFloats(false);
        Range[] result        = new Range[allValues.length];
        for (int rangeIdx = 0; rangeIdx < allValues.length; rangeIdx++) {
            float   pMin   = Float.POSITIVE_INFINITY;
            float   pMax   = Float.NEGATIVE_INFINITY;
            float[] values = allValues[rangeIdx];
            int     length = values.length;
            for (int i = 0; i < length; i++) {
                float value = values[i];
                //Note: we don't check for Float.isNaN (value) because if value is a 
                //NaN then each test below is false;
                if (pMax < value) {
                    pMax = value;
                }
                if (pMin > value) {
                    pMin = value;
                }
            }
            result[rangeIdx] = new Range(pMin, pMax);
        }
        return result;
    }


    /**
     * Make a range from a VisAD data range
     *
     * @param range  the data range
     *
     * @return  range
     */
    public static Range makeRange(visad.data.DataRange range) {
        if (range == null) {
            return null;
        }
        return new Range(range.getMin(), range.getMax());
    }


    /**
     * Make an array of Ranges from an array of DataRanges
     *
     * @param range  the DataRanges
     *
     * @return  the Ranges
     */
    public static Range[] makeRanges(visad.data.DataRange[] range) {
        if (range == null) {
            return null;
        }
        Range[] r = new Range[range.length];
        for (int i = 0; i < range.length; i++) {
            r[i] = makeRange(range[i]);
        }
        return r;
    }




    /**
     * Make a DataRange from a Range
     *
     * @param range  the Range
     *
     * @return  the DataRange
     */
    public static DataRange makeDataRange(Range range) {
        if (range == null) {
            return null;
        }
        return new DataRange(range.getMin(), range.getMax());
    }


    /**
     * Make an array of DataRanges from an array of Ranges
     *
     * @param range  the Ranges
     *
     * @return DataRanges
     */
    public static DataRange[] makeDataRanges(Range[] range) {
        if (range == null) {
            return null;
        }
        DataRange[] r = new DataRange[range.length];
        for (int i = 0; i < range.length; i++) {
            r[i] = makeDataRange(range[i]);
        }
        return r;
    }


    /**
     * get max and min of all range values in the current active fieldImpl
     *
     * @param fieldImpl      input field with outer dimension of time
     * @return  range of all parameters in the field
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static Range[] getMinMax(FieldImpl fieldImpl)
            throws VisADException, RemoteException {
        //        Trace.startTrace();
        //        Trace.call1 ("GDI.getMinMax");
        Range[] result = null;
        if (fieldImpl instanceof FlatField) {
            Range[] tmp = fieldMinMax((FlatField) fieldImpl);
            if (result == null) {
                result = new Range[tmp.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new Range(Double.POSITIVE_INFINITY,
                                          Double.NEGATIVE_INFINITY);
                }
            }

            for (int i = 0; i < result.length; i++) {
                result[i].min = Math.min(result[i].min, tmp[i].min);
                result[i].max = Math.max(result[i].max, tmp[i].max);
            }
        } else {
            int numTimes = (Util.getDomainSet(fieldImpl)).getLength();
            for (int nn = 0; nn < numTimes; nn++) {
                //FlatField   field = (FlatField) (fieldImpl.getSample(nn));
                FlatField field = null;

                Data      data  = null;

                // can be either time sequence or some other sequence
                if (fieldImpl.getDomainDimension() == 1)  // sequence
                {
                    data = fieldImpl.getSample(nn);
                    // see if this sample is either a displayable FlatField,
                    // or is ANOTHER FieldImpl sequence of FlatFields
                    if (data instanceof FlatField) {
                        field = (FlatField) data;
                    } else if (data instanceof FieldImpl) {
                        field = (FlatField) ((FieldImpl) data).getSample(0);
                    }
                }

                if (field != null) {
                    Range[] tmp = fieldMinMax(field);
                    if (result == null) {
                        result = new Range[tmp.length];
                        for (int i = 0; i < result.length; i++) {
                            result[i] = new Range(Double.POSITIVE_INFINITY,
                                    Double.NEGATIVE_INFINITY);
                        }
                    }

                    for (int i = 0; i < result.length; i++) {
                        result[i].min = Math.min(result[i].min, tmp[i].min);
                        result[i].max = Math.max(result[i].max, tmp[i].max);
                    }
                }
            }
        }
        //        Trace.call2 ("GDI.getMinMax");
        //        Trace.stopTrace();
        return result;
    }

    /**
     * Print out the sampling and error modes modes
     *
     * @param samplingMode  sampling mode
     * @param errorMode     error mode
     *
     * @return String for these modes
     */
    public static String printModes(int samplingMode, int errorMode) {

        StringBuffer buf = new StringBuffer("sampling: ");
        switch (samplingMode) {

          case Data.NEAREST_NEIGHBOR :
              buf.append("Nearest Neighbor");
              break;

          case Data.WEIGHTED_AVERAGE :
              buf.append("Weighted Average");
              break;

          default :
              break;
        }
        buf.append(" error: ");
        switch (errorMode) {

          case Data.INDEPENDENT :
              buf.append("Independent");
              break;

          case Data.DEPENDENT :
              buf.append("Dependent");
              break;

          case Data.NO_ERRORS :
              buf.append("No Errors");
              break;

          default :
              break;
        }
        return buf.toString();
    }

    /**
     * Convert a grid to point obs
     *
     * @param grid   grid to convert
     *
     * @return Field of point observations for each point
     *
     * @throws VisADException  problem getting data
     */
    public static FieldImpl getGridAsPointObs(FieldImpl grid)
            throws VisADException {
        if (grid == null) {
            return null;
        }
        RealType  index    = RealType.getRealType("index");
        FieldImpl retField = null;
        try {
            if (isTimeSequence(grid)) {
                SampledSet   timeSet      = (SampledSet) getTimeSet(grid);
                FunctionType retFieldType = null;
                double[][]   times        = timeSet.getDoubles(false);
                Unit         timeUnit     = timeSet.getSetUnits()[0];
                for (int i = 0; i < timeSet.getLength(); i++) {
                    DateTime  dt = new DateTime(times[0][i], timeUnit);
                    FieldImpl ff =
                        makePointObs((FlatField) grid.getSample(i), dt);
                    if (ff == null) {
                        continue;
                    }
                    if (retFieldType == null) {
                        retFieldType = new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            ff.getType());
                        retField = new FieldImpl(retFieldType, timeSet);
                    }
                    retField.setSample(i, ff, false);
                }
            } else {
                retField = makePointObs((FlatField) grid,
                                        new DateTime(Double.NaN));
            }
        } catch (RemoteException re) {}
        return retField;
    }

    /**
     * Make point obs from a single timestep of a grid
     *
     * @param timeStep     the grid
     * @param dt           the timestep for the grid
     *
     * @return   a Field of PointObs
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    private static FieldImpl makePointObs(FlatField timeStep, DateTime dt)
            throws VisADException, RemoteException {
        if (timeStep == null) {
            return null;
        }
        SampledSet   domain    = getSpatialDomain(timeStep);
        int          numPoints = domain.getLength();
        Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                                  numPoints);
        TupleType tt        = getParamType(timeStep);
        TupleType rangeType = new TupleType(new MathType[] {
                                  RealTupleType.LatitudeLongitudeAltitude,
                                  RealType.Time, tt });
        FieldImpl ff = new FieldImpl(
                           new FunctionType(
                               ((SetType) points.getType()).getDomain(),
                               rangeType), points);
        float[][] samples  = timeStep.getFloats(false);
        float[][] geoVals  = getEarthLocationPoints((GriddedSet) domain);
        boolean   isLatLon = isLatLonOrder(domain);
        int       latIndex = isLatLon
                             ? 0
                             : 1;
        int       lonIndex = isLatLon
                             ? 1
                             : 0;
        boolean   haveAlt  = geoVals.length > 2;
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            float alt = haveAlt
                        ? geoVals[2][i]
                        : 0;
            if ((lat == lat) && (lon == lon)) {
                if ( !(alt == alt)) {
                    alt = 0;
                }
                EarthLocation el = new EarthLocationLite(lat, lon, alt);
                // TODO:  make this  more efficient
                PointObTuple pot = new PointObTuple(el, dt,
                                       timeStep.getSample(i), rangeType);
                ff.setSample(i, pot, false, false);
            }
        }
        return ff;
    }

    /**
     * Convert the domain to the reference earth located points
     *
     * @param domain  the domain set
     *
     * @return  the lat/lon/(alt) points
     *
     * @throws VisADException  problem converting points
     */
    public static float[][] getEarthLocationPoints(GriddedSet domain)
            throws VisADException {
        CoordinateSystem cs = domain.getCoordinateSystem();
        if (cs == null) {
            return domain.getSamples();
        }
        RealTupleType refType  = cs.getReference();
        Unit[]        refUnits = cs.getReferenceUnits();
        float[][]     points   = CoordinateSystem.transformCoordinates(refType,
                               null, refUnits, null,
                               ((SetType) domain.getType()).getDomain(), cs,
                               domain.getSetUnits(), domain.getSetErrors(),
                               domain.getSamples(), false);
        return points;

    }





    /**
     * Find the indices of the domain values contained in the map
     *
     * @param domain  domain to use
     * @param map  the map lines containing bounding polygons
     *
     * @return indices in the domain
     *
     * @throws VisADException  problem sampling
     */
    public static int[][] findContainedIndices(GriddedSet domain,
            UnionSet map)
            throws VisADException {
        return findContainedIndices(getLatLon(domain), map);
    }

    /**
     * Find the indices of the latlon values contained in the map
     *
     * @param latlon  set of lat/lon values
     * @param map  the map lines containing bounding polygons
     *
     * @return indices in the domain
     *
     * @throws VisADException  problem sampling
     */
    public static int[][] findContainedIndices(float[][] latlon, UnionSet map)
            throws VisADException {
        long    t1      = System.currentTimeMillis();
        int[][] indices = findContainedIndices(latlon, map, true);
        long    t2      = System.currentTimeMillis();
        //System.err.println("indices time:" + (t2 - t1));
        return indices;
    }







    /**
     * find the indices not contained in the map domian
     *
     * @param domain grid domain
     * @param map    map of values
     *
     * @return  array of indicies
     *
     * @throws VisADException problem getting at the data
     */
    public static int[][] findNotContainedIndices(GriddedSet domain,
            UnionSet map)
            throws VisADException {
        return findNotContainedIndices(getLatLon(domain), map);
    }

    /**
     * Find the indices of the latlon values contained in the map
     *
     * @param latlon  set of lat/lon values
     * @param map  the map lines containing bounding polygons
     *
     * @return indices in the domain
     *
     * @throws VisADException  problem sampling
     */
    public static int[][] findNotContainedIndices(float[][] latlon,
            UnionSet map)
            throws VisADException {
        long    t1      = System.currentTimeMillis();
        int[][] indices = findContainedIndices(latlon, map, false);
        long    t2      = System.currentTimeMillis();
        //System.err.println("indices time:" + (t2 - t1));
        return indices;
    }



    /**
     * Find the lat/lon values in the given spatial domain for the given indices
     *
     * @param indices index array we get from findContainedIndices methods. i.e., indices[numPolygons][numIndices]
     * @param domain  domain to use
     *
     *
     * @return the lat lons of the form:<pre>
     * float[numPolygonPoints][2][numPoints]</pre>
     *
     * @throws VisADException  problem sampling
     */
    public static float[][][] getLatLons(GriddedSet domain, int[][] indices)
            throws VisADException {
        return getLatLons(getLatLon(domain), indices);
    }


    /**
     * Find the lat/lon values in the given spatial domain for the given indices
     *
     * @param indices index array we get from findContainedIndices methods. i.e., indices[numPolygons][numIndices]
     * @param latlons lat/lons from the spatial domain
     *
     * @return the lat lons of the form:<pre>
     * float[numPolygonPoints][2][numPoints]</pre>
     *
     * @throws VisADException  problem sampling
     */
    public static float[][][] getLatLons(float[][] latlons, int[][] indices)
            throws VisADException {
        float[][][] result = new float[indices.length][2][];
        for (int polygonIdx = 0; polygonIdx < indices.length; polygonIdx++) {
            result[polygonIdx][0] = new float[indices[polygonIdx].length];
            result[polygonIdx][1] = new float[indices[polygonIdx].length];
            for (int j = 0; j < indices[polygonIdx].length; j++) {
                result[polygonIdx][0][j] = latlons[0][indices[polygonIdx][j]];
                result[polygonIdx][1][j] = latlons[1][indices[polygonIdx][j]];
            }
        }
        return result;
    }




    /**
     * Find the lat/lon values in the given spatial domain contained by the polygons in the given map set
     *
     * @param domain  domain to use
     * @param maps The maps
     *
     *
     * @return the lat lons of the form:<pre>
     * float[numPolygonPoints][2][numPoints]</pre>
     *
     * @throws VisADException  problem sampling
     */
    public static float[][][] findContainedLatLons(GriddedSet domain,
            UnionSet maps)
            throws VisADException {
        return findContainedLatLons(getLatLon(domain), maps);
    }


    /**
     * Find the lat/lon values in the given spatial domain contained by the polygons in the given map set
     *
     * @param latlons the lat/lons from the domain
     * @param maps The maps
     *
     * @return the lat lons of the form:<pre>
     * float[numPolygonPoints][2][numPoints]</pre>
     *
     * @throws VisADException  problem sampling
     */

    public static float[][][] findContainedLatLons(float[][] latlons,
            UnionSet maps)
            throws VisADException {
        int[][] indices = findContainedIndices(latlons, maps);
        return getLatLons(latlons, indices);
    }









    /**
     * Recursively find all of the Gridded2DSets held by the map
     *
     * @param map the map
     * @param allSets all sets
     *
     * @throws VisADException On badness
     */
    private static void collectGriddedSets(UnionSet map,
                                           List<Gridded2DSet> allSets)
            throws VisADException {
        SampledSet[] sets = map.getSets();
        for (int j = 0; j < sets.length; j++) {
            if (sets[j] instanceof UnionSet) {
                collectGriddedSets((UnionSet) sets[j], allSets);
            } else if (sets[j] instanceof Gridded2DSet) {
                allSets.add((Gridded2DSet) sets[j]);
            } else {
                //                System.err.println("Unknown polygon type:" + sets[j].getClass().getName());
            }
        }
    }


    /**
     * Find the indicies contained inside the map bounds
     *
     * @param latlon   list of lat/lon points
     * @param map collection of polygons
     * @param inside  true for inside, false for outside
     *
     * @return indices in the domain
     *
     * @throws VisADException  problem getting data from VisAD Object
     */
    private static int[][] findContainedIndices(float[][] latlon,
            UnionSet map, boolean inside)
            throws VisADException {

        int numPoints = latlon[0].length;
        if (map == null) {
            int[][] indices = new int[1][numPoints];
            for (int i = 0; i < numPoints; i++) {
                indices[0][i] = i;
            }
            return indices;
        }

        List<Gridded2DSet> allSets = new ArrayList<Gridded2DSet>();
        collectGriddedSets(map, allSets);

        long            t1          = System.currentTimeMillis();
        int             numPolygons = allSets.size();
        List<float[][]> pts         = new ArrayList<float[][]>();
        List<Integer>[] indexLists  = new List[numPolygons];
        float[]         lonLow      = new float[numPolygons];
        float[]         lonHi       = new float[numPolygons];
        float[]         latLow      = new float[numPolygons];
        float[]         latHi       = new float[numPolygons];
        boolean         latLonOrder = isLatLonOrder(map);
        for (int polygonIdx = 0; polygonIdx < numPolygons; polygonIdx++) {
            Gridded2DSet g   = allSets.get(polygonIdx);
            float[]      low = g.getLow();
            float[]      hi  = g.getHi();
            lonLow[polygonIdx] = (latLonOrder
                                  ? low[1]
                                  : low[0]);
            latLow[polygonIdx] = (latLonOrder
                                  ? low[0]
                                  : low[1]);
            lonHi[polygonIdx]  = (latLonOrder
                                  ? hi[1]
                                  : hi[0]);
            latHi[polygonIdx]  = (latLonOrder
                                  ? hi[0]
                                  : hi[1]);
            float[][] sample = g.getSamples(false);
            pts.add(sample);
        }


        int ptCnt = 0;

        for (int i = 0; i < numPoints; i++) {
            float lat = latlon[0][i];
            float lon = latlon[1][i];
            if ((lon != lon) || (lat != lat)) {
                continue;
            }
            for (int mapIdx = 0; mapIdx < numPolygons; mapIdx++) {
                if (inside) {
                    if ((lon < lonLow[mapIdx]) || (lon > lonHi[mapIdx])
                            || (lat < latLow[mapIdx])
                            || (lat > latHi[mapIdx])) {
                        continue;
                    }
                } else {
                    if ((lon >= lonLow[mapIdx]) && (lon <= lonHi[mapIdx])
                            && (lat >= latLow[mapIdx])
                            && (lat <= latHi[mapIdx])) {
                        //                        System.out.println("Inside " + lon +  " " + lat);
                        continue;
                    } else {
                        //                        System.out.println("Not  inside " + lon +  " " + lat + " (" + lonLow[mapIdx]+" "+lonHi[mapIdx] +") ( "+
                        //                                           latLow[mapIdx]+" "+latHi[mapIdx]+")");
                    }
                }

                ptCnt++;

                /*
                boolean pointInside =
                    DelaunayCustom.inside((float[][]) pts.get(mapIdx),
                                          (latLonOrder
                                           ? lat
                                           : lon), (latLonOrder
                        ? lon
                        : lat));
                */

                boolean pointInside2 =
                    DataUtil.pointInside((float[][]) pts.get(mapIdx),
                                         (latLonOrder
                                          ? lat
                                          : lon), (latLonOrder
                        ? lon
                        : lat));
                //                if(pointInside!=pointInside2) {
                //                    System.err.println("bad point:" + lon + " " + lat);
                //                }

                boolean pointInside = pointInside2;


                boolean ok          = (inside
                                       ? pointInside
                                       : !pointInside);
                if (ok) {
                    if (indexLists[mapIdx] == null) {
                        indexLists[mapIdx] = new ArrayList<Integer>();
                    }
                    indexLists[mapIdx].add(new Integer(i));
                    break;
                }
            }
        }
        //System.err.println("total pts:" + numPoints + "  points inside box:"
        //                   + ptCnt + " # polygon points:" + numPolygonPts);
        int[][] indices = new int[numPolygons][];
        for (int mapIdx = 0; mapIdx < indexLists.length; mapIdx++) {
            if (indexLists[mapIdx] == null) {
                indices[mapIdx] = new int[0];
            } else {
                indices[mapIdx] = new int[indexLists[mapIdx].size()];
                //                System.err.println("index:" + indices[mapIdx].length);
                for (int ptIdx = 0; ptIdx < indexLists[mapIdx].size();
                        ptIdx++) {
                    indices[mapIdx][ptIdx] =
                        ((Integer) indexLists[mapIdx].get(ptIdx)).intValue();
                }
            }
        }
        long t2 = System.currentTimeMillis();
        //        System.err.println ("find indices  #pts:" + numPoints+" time:" + (t2-t1)+ "   points:" + cnt1 + " " + cnt2);
        return indices;


    }



    /**
     * Finds the indices of the values array whose value is in the given range
     *
     * @param values values
     * @param min min value
     * @param max max value
     *
     * @return indices
     *
     * @throws VisADException On badness
     */
    public static int[][] findIndicesInsideRange(float[][] values, float min,
            float max)
            throws VisADException {
        return findIndicesInRange(values, min, max, true);
    }


    /**
     * Finds the indices of the values array whose value is not in the given range
     *
     * @param values values
     * @param min min value
     * @param max max value
     *
     * @return indices
     *
     * @throws VisADException On badness
     */
    public static int[][] findIndicesOutsideRange(float[][] values,
            float min, float max)
            throws VisADException {
        return findIndicesInRange(values, min, max, false);
    }


    /**
     * Finds the indices of the values array whose value are either in or out of the given range
     * depending on the inside flag
     *
     * @param values values
     * @param min min value
     * @param max max value
     * @param inside inside flag
     *
     * @return indices
     *
     * @throws VisADException On badness
     */
    private static int[][] findIndicesInRange(float[][] values, float min,
            float max, boolean inside)
            throws VisADException {
        int   numPoints = values[0].length;
        int   cnt       = 0;
        int[] indices   = new int[1000];
        for (int i = 0; i < numPoints; i++) {
            float   value = values[0][i];
            boolean ok    = (inside
                             ? ((value >= min) && (value <= max))
                             : ((value < min) || (value > max)));
            if (ok) {
                cnt++;
                if (cnt >= indices.length) {
                    int[] tmp = indices;
                    indices = new int[tmp.length * 2];
                    System.arraycopy(tmp, 0, indices, 0, cnt);
                }
                indices[cnt] = i;
            }
        }
        int[] tmp = indices;
        indices = new int[cnt];
        System.arraycopy(tmp, 0, indices, 0, cnt);
        return new int[][] {
            indices
        };
    }

    /**
     * Convert the domain to the reference earth located points.
     * If the domain is not in lat/lon order then reset the order so
     * that result[0] is the latitudes, result[1] is the longitudes
     *
     * @param domain  the domain set
     *
     * @return  the lat/lon/(alt) points
     *
     * @throws VisADException  problem converting points
     */
    public static float[][] getLatLon(GriddedSet domain)
            throws VisADException {
        boolean   isLatLon = isLatLonOrder(domain);
        float[][] values   = getEarthLocationPoints(domain);
        if ( !isLatLon) {
            float[] tmp = values[0];
            values[0] = values[1];
            values[1] = tmp;
        }
        return values;
    }



    /**
     * test
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        for (int size = 1; size <= 10; size++) {
            float[]            test    = new float[size * 1000000];

            long               t1      = System.currentTimeMillis();
            OutputStream       ostream = new FileOutputStream("test.ser");
            ObjectOutputStream p       = new ObjectOutputStream(ostream);
            p.writeObject(test);
            p.flush();
            ostream.close();
            long              t2      = System.currentTimeMillis();

            InputStream       istream = new FileInputStream("test.ser");
            ObjectInputStream ois     = new ObjectInputStream(istream);
            float[]           tmp     = (float[]) ois.readObject();
            long              t3      = System.currentTimeMillis();
            System.err.println("Length:" + tmp.length + " write: "
                               + (t2 - t1) + " read:" + (t3 - t2));
        }
    }


    /**
     * Write grid out to an Excel spreadsheet
     *
     * @param grid grid  to write
     *
     * @throws Exception  problem writing grid
     */
    public static void writeGridToXls(FieldImpl grid) throws Exception {
        String filename = FileManager.getWriteFile(FileManager.FILTER_XLS,
                              null);
        if (filename == null) {
            return;
        }
        writeGridToXls(grid, filename);
    }

    /**
     * Write grid out to an Excel spreadsheet
     *
     * @param grid grid  to write
     * @param filename  filename
     *
     * @throws Exception  problem writing grid
     */
    public static void writeGridToXls(FieldImpl grid, String filename)
            throws Exception {

        Object loadId =
            JobManager.getManager().startLoad("Writing grid to xls", true);
        try {
            HSSFWorkbook    wb = new HSSFWorkbook();
            HSSFRow         row;
            int             sheetIdx = -1;
            List<HSSFSheet> sheets   = new ArrayList<HSSFSheet>();
            OutputStream    fileOut  =
                new BufferedOutputStream(new FileOutputStream(filename),
                                         1000000);

            int             MAXROWS    = 65000;
            List<DateTime>  times      = new ArrayList<DateTime>();
            List<FlatField> fields     = new ArrayList<FlatField>();

            float[][]       domainVals = null;
            int             colOffset  = 2;
            int             rowCnt;
            int             sheetCnt;
            HSSFSheet       sheet = null;

            if (isTimeSequence(grid)) {
                SampledSet timeSet    = (SampledSet) getTimeSet(grid);
                double[][] timeValues = timeSet.getDoubles(false);
                Unit       timeUnit   = timeSet.getSetUnits()[0];
                int        numTimes   = timeSet.getLength();
                for (int timeIdx = 0; timeIdx < numTimes; timeIdx++) {
                    DateTime dt = new DateTime(timeValues[0][timeIdx],
                                      timeUnit);
                    JobManager.getManager().setDialogLabel1(loadId,
                            "Writing grid time:" + (timeIdx + 1) + "/"
                            + numTimes);
                    FlatField ff = (FlatField) grid.getSample(timeIdx);
                    if (ff == null) {
                        continue;
                    }
                    times.add(dt);
                    fields.add(ff);
                }
            } else if (grid instanceof FlatField) {
                fields.add((FlatField) grid);
            } else {
                System.err.println("Could not find any grid fields to write");
            }


            for (int fieldIdx = 0; fieldIdx < fields.size(); fieldIdx++) {
                int       timeIdx = fieldIdx;
                DateTime  dt      = ((times.size() > 0)
                                     ? times.get(fieldIdx)
                                     : null);
                FlatField ff      = fields.get(fieldIdx);
                if (sheets.size() == 0) {
                    SampledSet ss        = getSpatialDomain(ff);
                    SampledSet latLonSet = null;
                    if (ss.getCoordinateSystem() != null) {
                        latLonSet = Util.convertDomain(ss,
                                ss.getCoordinateSystem().getReference(),
                                null);
                    } else {
                        latLonSet = ss;
                    }

                    domainVals = latLonSet.getSamples(false);
                    int numRows = domainVals[0].length;
                    rowCnt = -1;
                    for (int rowIdx = 0; rowIdx < numRows; rowIdx++) {
                        if ((rowCnt >= MAXROWS) || (rowCnt == -1)) {
                            sheets.add(sheet = wb.createSheet());
                            row = sheet.createRow(0);
                            row.createCell((short) 0).setCellValue(
                                "Latitude");
                            row.createCell((short) 1).setCellValue(
                                "Longitude");
                            if (domainVals.length > 2) {
                                row.createCell((short) 2).setCellValue(
                                    "Altitude");
                                colOffset = 3;
                            }
                            rowCnt = 0;
                        }
                        row = sheet.createRow(rowCnt + 1);
                        row.createCell((short) 0).setCellValue(
                            domainVals[0][rowIdx]);
                        row.createCell((short) 1).setCellValue(
                            domainVals[1][rowIdx]);
                        if (domainVals.length > 2) {
                            row.createCell((short) 2).setCellValue(
                                domainVals[2][rowIdx]);
                        }
                        rowCnt++;
                    }
                }
                float[][] rangeVals = ff.getFloats(false);
                rowCnt   = -1;
                sheetCnt = -1;
                sheet    = null;
                for (int rowIdx = 0; rowIdx < domainVals[0].length;
                        rowIdx++) {
                    if ((rowCnt == -1) || (rowCnt >= MAXROWS)) {
                        rowCnt = 0;
                        sheetCnt++;
                        sheet = (HSSFSheet) sheets.get(sheetCnt);
                        row   = sheet.getRow(0);
                        if (dt != null) {
                            row.createCell((short) (colOffset
                                    + timeIdx)).setCellValue(dt.toString());
                        }
                    }
                    row = sheet.getRow(rowCnt + 1);
                    row.createCell(
                        (short) (colOffset + timeIdx)).setCellValue(
                        rangeVals[0][rowIdx]);
                    rowCnt++;
                }
            }

            JobManager.getManager().setDialogLabel1(loadId,
                    "Writing spreadsheet");
            wb.write(fileOut);
            fileOut.close();
        } catch (Exception exc) {
            LogUtil.logException("Writing grid to xls file: " + filename,
                                 exc);
        } finally {
            JobManager.getManager().stopLoad(loadId);
        }

    }

    /**
     * Write grid out to a netCDF CF compliant file
     *
     * @param grid grid  to write
     *
     * @throws Exception  problem writing grid
     */
    public static void exportGridToNetcdf(FieldImpl grid) throws Exception {
        String filename = FileManager.getWriteFile(FileManager.FILTER_NETCDF,
                              null);
        if (filename == null) {
            return;
        }
        exportGridToNetcdf(grid, filename);
    }


    /**
     * Write grid out to a netCDF CF compliant file
     *
     * @param grid grid  to write
     * @param filename  filename
     *
     * @throws Exception  problem writing grid
     */
    public static void exportGridToNetcdf(FieldImpl grid, String filename)
            throws Exception {

        Object loadId =
            JobManager.getManager().startLoad("Writing grid to CF", true);
        try {
            NetcdfFileWriteable ncfile =
                NetcdfFileWriteable.createNew(filename, false);
            boolean         isTimeSequence = isTimeSequence(grid);
            List<Dimension> dims           = new ArrayList<Dimension>();
            // make variables for the time and xyz axes
            Set timeSet  = null;
            int numTimes = 0;
            if (isTimeSequence) {
                timeSet = getTimeSet(grid);
                Unit[] units = timeSet.getSetUnits();
                numTimes = timeSet.getLength();
                Dimension timeDim = new Dimension("time", numTimes, true);
                dims.add(timeDim);
                ncfile.addDimension(null, timeDim);
                Variable timeVar = new Variable(ncfile, null, null, "time",
                                       DataType.DOUBLE, "time");
                timeVar.addAttribute(new Attribute("units",
                        units[0].toString()));
                ncfile.addVariable(null, timeVar);
            }
            GriddedSet domainSet = (GriddedSet) getSpatialDomain(grid);
            CoordinateSystem         cs      = domainSet.getCoordinateSystem();
            boolean haveEmpirical = cs instanceof EmpiricalCoordinateSystem;
            HashMap<Variable, Array> varData = addSpatialVars(ncfile,
                                                   domainSet, dims);

            // TODO: figure out a better way to do this
            Variable      projVar = null;
            java.util.Set keys    = varData.keySet();
            if ( !haveEmpirical) {
                for (Iterator it = keys.iterator(); it.hasNext(); ) {
                    Variable v = (Variable) it.next();
                    if (v.findAttribute(ProjectionImpl.ATTR_NAME) != null) {
                        projVar = v;
                        break;
                    }
                }
            }
            // make variable for the parameter(s)
            TupleType  tType  = getParamType(grid);
            RealType[] rTypes = tType.getRealComponents();
            for (int i = 0; i < rTypes.length; i++) {
                RealType rt = rTypes[i];
                Variable v  = new Variable(ncfile, null, null, getVarName(rt));
                Unit     u  = rt.getDefaultUnit();
                if (u != null) {
                    v.addAttribute(new Attribute("units",
                            rt.getDefaultUnit().toString()));
                }
                if (projVar != null) {
                    v.addAttribute(new Attribute("grid_mapping",
                            projVar.getName()));
                }
                if (haveEmpirical) {
                    v.addAttribute(new Attribute("coordinates",
                            "latitude longitude"));
                }
                v.setDataType(DataType.FLOAT);
                v.setDimensions(dims);
                ncfile.addVariable(null, v);
            }
            ncfile.addGlobalAttribute(new Attribute("Conventions", "CF-1.X"));
            ncfile.addGlobalAttribute(new Attribute("History",
                    "Translated from VisAD grid to CF-1.X Conventions by IDV\n"
                    + "Original Dataset = " + grid.getType()
                    + "\nTranslation Date = " + new Date()));
            ncfile.create();
            // fill in the data
            if (isTimeSequence) {
                Variable   timeVar  = ncfile.findVariable("time");
                double[][] timeVals = timeSet.getDoubles(false);
                Array      varArray = Array.factory(DataType.DOUBLE,
                                     new int[] { numTimes }, timeVals[0]);
                ncfile.write(timeVar.getName(), varArray);
            }
            for (Iterator it = keys.iterator(); it.hasNext(); ) {
                Variable v = (Variable) it.next();
                ncfile.write(v.getName(), varData.get(v));
            }
            int   numDims = dims.size();
            int[] sizes   = new int[numDims];
            int   index   = 0;
            for (Dimension dim : dims) {
                sizes[index++] = dim.getLength();
            }
            // write the data
            Array arr = null;
            if (isTimeSequence) {
                sizes[0] = 1;
                int[] origin = new int[sizes.length];
                for (int k = 1; k < sizes.length; k++) {
                    origin[k] = 0;
                }
                for (int i = 0; i < timeSet.getLength(); i++) {
                    origin[0] = i;
                    FlatField sample  = (FlatField) grid.getSample(i, false);
                    float[][] samples = sample.getFloats(false);
                    for (int j = 0; j < rTypes.length; j++) {
                        Variable v =
                            ncfile.findVariable(getVarName(rTypes[j]));
                        arr = Array.factory(DataType.FLOAT, sizes,
                                            samples[j]);
                        ncfile.write(v.getName(), origin, arr);
                    }
                }
            } else {
                float[][] samples = ((FlatField) grid).getFloats();
                for (int j = 0; j < rTypes.length; j++) {
                    Variable v = ncfile.findVariable(getVarName(rTypes[j]));
                    arr = Array.factory(DataType.FLOAT, sizes, samples[j]);
                    ncfile.write(v.getName(), arr);
                }
            }
            // write the file
            ncfile.close();
        } catch (Exception exc) {
            LogUtil.logException("Writing grid to netCDF file: " + filename,
                                 exc);
        } finally {
            JobManager.getManager().stopLoad(loadId);
        }
    }

    /**
     * Get a netCDF variable name from a RealType
     *
     * @param r  the RealType
     *
     * @return  a valid netCDF name
     */
    private static String getVarName(RealType r) {
        return Util.cleanTypeName(r.getName());
    }

    /**
     * Add spatial variables to the netCDF file
     *
     * @param ncfile  netCDF file
     * @param domainSet  domain set of the grid
     * @param dims       list of dimensions to add to
     *
     * @return Hashtable of variable to Array
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   Problem accessing VisAD object
     */
    private static HashMap<Variable, Array> addSpatialVars(NetcdfFile ncfile,
            SampledSet domainSet, List<Dimension> dims)
            throws VisADException, RemoteException {

        HashMap<Variable, Array> varToArray = new HashMap<Variable, Array>();
        int                      dim        = domainSet.getDimension();
        int                      mdim       = domainSet.getManifoldDimension();

        CoordinateSystem         cs         = domainSet.getCoordinateSystem();
        boolean haveEmpirical = cs instanceof EmpiricalCoordinateSystem;
        Unit[]                   units      = domainSet.getSetUnits();
        int[]                    lens = ((GriddedSet) domainSet).getLengths();
        // populate the time and axes values
        float[][] spatialVals = domainSet.getSamples(false);
        boolean   is3D        = dim > 2;
        int       sizeX       = lens[0];
        int       sizeY       = lens[1];
        int       sizeZ       = 1;
        if (is3D && (mdim > 2)) {
            sizeZ = lens[2];
        }
        float[] xVals = new float[sizeX];
        float[] yVals = new float[sizeY];
        float[] zVals = new float[sizeZ];
        if (is3D) {
            for (int z = 0; z < sizeZ; z++) {
                zVals[z] = spatialVals[2][sizeX * sizeY * z];
            }
        }
        for (int y = 0; y < sizeY; y++) {
            yVals[y] = spatialVals[1][sizeX * y];
        }
        for (int x = 0; x < sizeX; x++) {
            xVals[x] = spatialVals[0][x];
        }
        RealType[] types =
            ((SetType) domainSet.getType()).getDomain().getRealComponents();
        String    xName = (haveEmpirical)
                          ? "xc"
                          : getVarName(types[0]);
        Dimension xDim  = new Dimension(xName, sizeX, true);
        ncfile.addDimension(null, xDim);

        String    yName = (haveEmpirical)
                          ? "yc"
                          : getVarName(types[1]);
        Dimension yDim  = new Dimension(yName, sizeY, true);
        ncfile.addDimension(null, yDim);
        String zName = null;
        if (dim == 3) {
            zName = getVarName(types[2]);
            Dimension zDim = new Dimension(zName, sizeZ, true);
            ncfile.addDimension(null, zDim);
            dims.add(zDim);
        }
        Variable      xVar = null;
        Variable      yVar = null;
        Array         varArray;
        MapProjection mp = getNavigation(domainSet);
        if ((mp instanceof TrivialMapProjection) && !haveEmpirical) {  // straight lat/lon(/alt)
            xVar = makeCoordinateVariable(ncfile, xName, units[0],
                                          "longitude coordinate",
                                          "longitude", xName);
            yVar = makeCoordinateVariable(ncfile, yName, units[1],
                                          "latitude coordinate", "latitude",
                                          yName);
        } else if ( !haveEmpirical) {
            xVar = makeCoordinateVariable(ncfile, xName, units[0],
                                          "x coordinate of projection",
                                          "projection_x_coordinate", xName);

            yVar = makeCoordinateVariable(ncfile, yName, units[1],
                                          "y coordinate of projection",
                                          "projection_y_coordinate", yName);

            // make variable for the projection
            Variable projVar = makeProjectionVar(ncfile, mp);
            if (projVar != null) {
                char[] data      = new char[] { 'd' };
                Array  dataArray = Array.factory(DataType.CHAR, new int[0],
                                      data);
                varToArray.put(projVar, dataArray);
            }
        } else {  // have Empirical Coordinate System
            xVar = makeCoordinateVariable(ncfile, xName, (String) null,
                                          "x coordinate", "x_coordinate",
                                          xName);

            yVar = makeCoordinateVariable(ncfile, yName, (String) null,
                                          "y coordinate", "y_coordinate",
                                          yName);

            float[] latVals = new float[sizeX * sizeY];
            float[] lonVals = new float[sizeX * sizeY];
            int     index   = 0;
            for (int y = 0; y < sizeY; y++) {
                yVals[y] = y;
                for (int x = 0; x < sizeX; x++) {
                    if (index < sizeX) {
                        xVals[x] = x;
                    }
                    lonVals[index]   = spatialVals[0][x + sizeX * y];
                    latVals[index++] = spatialVals[1][x + sizeX * y];
                }
            }

            Variable latVar = new Variable(ncfile, null, null, "latitude",
                                           DataType.FLOAT, "yc xc");
            latVar.addAttribute(new Attribute("units", "degrees_north"));
            latVar.addAttribute(new Attribute("long_name",
                    "latitude of points"));
            ncfile.addVariable(null, latVar);
            varArray = Array.factory(DataType.FLOAT, new int[] { sizeY,
                    sizeX }, latVals);
            varToArray.put(latVar, varArray);
            Variable lonVar = new Variable(ncfile, null, null, "longitude",
                                           DataType.FLOAT, "yc xc");
            lonVar.addAttribute(new Attribute("units", "degrees_east"));
            lonVar.addAttribute(new Attribute("long_name",
                    "longitude of points"));
            ncfile.addVariable(null, lonVar);
            varArray = Array.factory(DataType.FLOAT, new int[] { sizeY,
                    sizeX }, lonVals);
            varToArray.put(lonVar, varArray);
        }
        dims.add(yDim);
        dims.add(xDim);
        if (dim == 3) {
            Variable zVar = new Variable(ncfile, null, null, zName,
                                         DataType.FLOAT, zName);
            Unit zUnit = units[2];
            if (zUnit != null) {
                zVar.addAttribute(new Attribute("units", zUnit.toString()));
            }
            String upOrDown = "up";
            if (Unit.canConvert(units[2], CommonUnits.MILLIBAR)) {
                upOrDown = "down";
            }
            zVar.addAttribute(new Attribute("positive", upOrDown));
            if (cs == null) {
                zVar.addAttribute(new Attribute("long_name",
                        "altitude (MSL"));
                zVar.addAttribute(new Attribute("standard_name", "altitude"));
            }
            varArray = Array.factory(DataType.FLOAT, new int[] { sizeZ },
                                     zVals);
            varToArray.put(zVar, varArray);
            ncfile.addVariable(null, zVar);

            if (haveEmpirical) {
                String   dimString = zName + " yc xc";
                String   altName   = "height";
                Variable altVar    = new Variable(ncfile, null, null, altName,
                                      DataType.FLOAT, dimString);
                EmpiricalCoordinateSystem ecs =
                    (EmpiricalCoordinateSystem) cs;
                GriddedSet refSet   = ecs.getReferenceSet();
                int[]      refSizes = refSet.getLengths();
                Unit[]     refUnits = refSet.getSetUnits();
                Unit       altUnit  = refUnits[2];
                if (altUnit != null) {
                    altVar.addAttribute(new Attribute("units",
                            altUnit.toString()));
                }
                altVar.addAttribute(new Attribute("long_name",
                        "height/depth of " + zName));
                altVar.addAttribute(new Attribute("standard_name",
                        "altitude"));
                altVar.addAttribute(new Attribute("coordinates",
                        "latitude longitude"));
                float[] altVals = refSet.getSamples(false)[2];
                varArray = Array.factory(DataType.FLOAT,
                                         new int[] { refSizes[2],
                        refSizes[1], refSizes[0] }, altVals);
                varToArray.put(altVar, varArray);
                ncfile.addVariable(null, altVar);
            }

        }
        varArray = Array.factory(DataType.FLOAT, new int[] { sizeX }, xVals);
        varToArray.put(xVar, varArray);
        varArray = Array.factory(DataType.FLOAT, new int[] { sizeY }, yVals);
        varToArray.put(yVar, varArray);

        return varToArray;

    }

    /**
     * Make a coordinate variable
     *
     * @param ncfile  file
     * @param name    name of the variable
     * @param unit    unit of the variable
     * @param desc    description (long_name) of the variable
     * @param standard_name    CF standard name of the variable
     * @param dimName    name of the variable dimension
     *
     * @return  the Variable
     */
    private static Variable makeCoordinateVariable(NetcdfFile ncfile,
            String name, Unit unit, String desc, String standard_name,
            String dimName) {
        return makeCoordinateVariable(ncfile, name, (unit != null)
                ? unit.toString()
                : (String) null, desc, standard_name, dimName);
    }

    /**
     * Make a coordinate variable
     *
     * @param ncfile  file
     * @param name    name of the variable
     * @param unitName unit name
     * @param desc    description (long_name) of the variable
     * @param standard_name    CF standard name of the variable
     * @param dimName    name of the variable dimension
     *
     * @return  the Variable
     */
    private static Variable makeCoordinateVariable(NetcdfFile ncfile,
            String name, String unitName, String desc, String standard_name,
            String dimName) {
        Variable v = new Variable(ncfile, null, null, name);
        v.setDataType(DataType.FLOAT);
        v.setDimensions(dimName);

        if (unitName != null) {
            v.addAttribute(new Attribute("units", unitName));
        }
        v.addAttribute(new Attribute("long_name", desc));
        v.addAttribute(new Attribute("standard_name", standard_name));
        ncfile.addVariable(null, v);
        return v;

    }

    /**
     * Make a projection varaible
     *
     * @param ncfile  the file
     * @param mp      the MapProjection
     *
     * @return the variable or null;
     */
    private static Variable makeProjectionVar(NetcdfFile ncfile,
            MapProjection mp) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        Variable        projVar    = null;
        if ((mp instanceof ProjectionCoordinateSystem)
                || (mp instanceof AREACoordinateSystem)) {
            ProjectionImpl proj = null;
            if (mp instanceof AREACoordinateSystem) {
                AREACoordinateSystem acs = (AREACoordinateSystem) mp;
                int[]                dir = acs.getDirBlock();
                int[]                nav = acs.getNavBlock();
                int[]                aux = acs.getAuxBlock();
                proj = new McIDASAreaProjection(dir, nav, aux);
            } else {
                proj = ((ProjectionCoordinateSystem) mp).getProjection();
            }
            List<Parameter> params    = proj.getProjectionParameters();
            String          grid_name = "not_yet_supported";
            if (proj instanceof LambertConformal) {
                grid_name = "lambert_conformal_conic";
            } else if (proj instanceof Mercator) {
                grid_name = "mercator";
            } else if (proj instanceof Stereographic) {
                grid_name = "polar_stereographic";
            } else if (proj instanceof VerticalPerspectiveView) {
                grid_name = "vertical_perspective";
            } else if (proj instanceof McIDASAreaProjection) {
                grid_name = McIDASAreaProjection.GRID_MAPPING_NAME;
            }
            attributes.add(new Attribute(ProjectionImpl.ATTR_NAME,
                                         grid_name));
            for (Parameter param : params) {
                if (param.isString()) {
                    attributes.add(new Attribute(param.getName(),
                            param.getStringValue()));
                } else {
                    if (param.getLength() == 1) {
                        attributes.add(new Attribute(param.getName(),
                                new Double(param.getNumericValue())));
                    } else {
                        double[] data = param.getNumericValues();
                        attributes.add(new Attribute(param.getName(),
                                Array.factory(DataType.DOUBLE,
                                    new int[] { param.getLength() }, data)));
                    }
                }
            }
            projVar = new Variable(ncfile, null, null, grid_name);
            projVar.setDataType(DataType.CHAR);
            projVar.setDimensions(new ArrayList<Dimension>());  // scalar

            for (int i = 0; i < attributes.size(); i++) {
                Attribute att = (Attribute) attributes.get(i);
                projVar.addAttribute(att);
            }
            ncfile.addVariable(null, projVar);
        } else if (mp instanceof AREACoordinateSystem) {
            AREACoordinateSystem acs = (AREACoordinateSystem) mp;
            int[]                dir = acs.getDirBlock();
            int[]                nav = acs.getNavBlock();
            int[]                aux = acs.getAuxBlock();

        }
        return projVar;
    }

    /**
     * Set the pressure values for a grid
     *
     * @param grid  grid to change
     * @param pressValues  pressure values. Must match number of levels in
     *                     the grid.  Units are millibars.
     *
     * @return a grid with vertical levels in pressure
     *
     * @throws VisADException  problem setting the values
     */
    public static FieldImpl setPressureValues(FieldImpl grid,
            float[] pressValues)
            throws VisADException {
        return setVerticalValues(grid, pressValues,
                                 AirPressure.getRealType(),
                                 CommonUnits.MILLIBAR);
    }

    /**
     * Set the altitude values for a grid
     *
     * @param grid  grid to change
     * @param altValues    altitude values. Must match number of levels in
     *                     the grid.  Units are meters.
     *
     * @return a grid with vertical levels in meters
     *
     * @throws VisADException  problem setting the values
     */
    public static FieldImpl setAltitudeValues(FieldImpl grid,
            float[] altValues)
            throws VisADException {
        return setVerticalValues(grid, altValues, RealType.Altitude,
                                 CommonUnit.meter);
    }

    /**
     * Set the vertical values
     *
     * @param grid  the grid to change
     * @param newValues  the new vertical values.  Must match the number
     *                   of vertical levels in the grid.
     * @param vertType  the type of the data
     * @param vertUnit  the  unit of <code>newValues</code>
     *
     * @return  modified grid
     *
     * @throws VisADException  problem setting the values
     */
    public static FieldImpl setVerticalValues(FieldImpl grid,
            float[] newValues, RealType vertType, Unit vertUnit)
            throws VisADException {
        FieldImpl  newField  = null;
        SampledSet domainSet = getSpatialDomain(grid);
        if ( !(domainSet instanceof Gridded3DSet)) {
            throw new VisADException("Not a 3D set");
        }
        newField =
            setSpatialDomain(grid,
                             newVerticalDomain((Gridded3DSet) domainSet,
                                 newValues, vertType, vertUnit), false);
        return newField;
    }

    /**
     * Set the vertical values in the domain
     *
     * @param domainSet  the domain to change
     * @param newValues  the new vertical values.  Must match the number
     *                   of vertical levels in the domainSet.
     * @param vertType  the type of the data
     * @param vertUnit  the  unit of <code>newValues</code>
     *
     * @return  modified domain
     *
     * @throws VisADException  problem setting the values
     */
    private static Gridded3DSet newVerticalDomain(Gridded3DSet domainSet,
            float[] newValues, RealType vertType, Unit vertUnit)
            throws VisADException {

        Gridded3DSet newDSet   = null;
        int[]        lengths   = domainSet.getLengths();
        int          setLength = domainSet.getLength();
        if ((lengths[2] != newValues.length)
                && (setLength != newValues.length)) {
            throw new VisADException(
                "newValues size not equal to domain vertical dimension size");
        }
        float[] vertVals = null;
        if (newValues.length == setLength) {
            vertVals = newValues;
        } else {
            vertVals = new float[setLength];
            int l = 0;
            for (int k = 0; k < lengths[2]; k++) {
                for (int j = 0; j < lengths[1]; j++) {
                    for (int i = 0; i < lengths[0]; i++) {
                        vertVals[l++] = newValues[k];
                    }
                }
            }
        }
        float[][]        setVals   = domainSet.getSamples(true);
        float[][]        refVals   = null;
        RealTupleType    setType = ((SetType) domainSet.getType()).getDomain();
        CoordinateSystem cs        = domainSet.getCoordinateSystem();
        RealTupleType    refType   = (cs != null)
                                     ? cs.getReference()
                                     : setType;
        ErrorEstimate[]  oldErrors = domainSet.getSetErrors();
        ErrorEstimate[]  newErrors = new ErrorEstimate[oldErrors.length];
        if (cs != null) {
            Trace.call1("GridUtil.transformCoordinates");
            // transform to the reference
            refVals = CoordinateSystem.transformCoordinates(refType,
                    refType.getCoordinateSystem(), refType.getDefaultUnits(),
                    newErrors, setType, cs, domainSet.getSetUnits(),
                    oldErrors, setVals, false);

            Trace.call2("GeoGridAdapter.transformCoordinates");

        } else {
            refVals    = new float[3][];
            refVals[0] = setVals[0];
            refVals[1] = setVals[1];

        }
        refVals[2] = vertVals;

        // now create a new domain type based on the vertical transform
        Unit       vtu        = vertUnit;
        RealType[] types      = refType.getRealComponents();
        boolean    isPressure = false;


        if ( !Unit.canConvert(vtu, CommonUnit.meter)) {  // other than height
            if (Unit.canConvert(vtu, CommonUnits.MILLIBAR)) {
                isPressure = true;
            } else {
                throw new VisADException("unknown vertical coordinate");
            }
        }
        RealTupleType newDomainType = new RealTupleType(types[0], types[1],
                                          RealType.Altitude);


        if (isPressure) {  // convert to altitude using standard atmos
            CoordinateSystem vcs =
                DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE);
            refVals[2] = vcs.toReference(new float[][] {
                refVals[2]
            }, new Unit[] { vtu })[0];
            vtu        = vcs.getReferenceUnits()[0];
        }
        //for (int i = 0; i < 10; i++) {
        //    System.out.println("vals["+i+"] = " + refVals[2][i]);
        //}

        Unit[] newDomainUnits = newDomainType.getDefaultUnits();
        newDomainUnits[2] = vtu;


        Gridded3DSet newDomain =
            (Gridded3DSet) GriddedSet.create(newDomainType, refVals, lengths,
                                             null, newDomainUnits, newErrors,
                                             false, false);

        EmpiricalCoordinateSystem ecs =
            new EmpiricalCoordinateSystem(domainSet, newDomain);


        CoordinateSystem gcs        = ecs;


        RealTupleType    newSetType =
            new RealTupleType(setType.getRealComponents(), gcs, null);

        Trace.call1("GeoGridAdapter final GriddedSet");
        newDSet = (Gridded3DSet) GriddedSet.create(newSetType,
                domainSet.getSamples(false), lengths, null,
                domainSet.getSetUnits(), oldErrors, false, false);


        return newDSet;


    }



    /**
     * Find the min/max and average of a file inside the mapsets
     *
     * @param field   the field
     * @param mapSets The map sets
     *
     * @return the list of FieldStats
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  problem getting the values
     */
    public static List<FieldStats> findMinMaxAverage(FieldImpl field,
            UnionSet mapSets)
            throws VisADException, RemoteException {
        List<FieldStats> stats = new ArrayList<FieldStats>();
        if (GridUtil.isTimeSequence(field)) {
            int       numTimes = Util.getDomainSet(field).getLength();
            float[][] result   = new float[numTimes][];
            for (int timeStep = 0; timeStep < numTimes; timeStep++) {
                stats.add(
                    findMinMaxAverageFromRange(
                        (FlatField) field.getSample(timeStep), mapSets));
            }
        } else {
            stats.add(findMinMaxAverageFromRange((FlatField) field, mapSets));
        }
        return stats;
    }


    /**
     * Find the min, max and average from the range
     *
     * @param field  the field
     * @param mapSets  the mapsets
     *
     * @return the stats
     *
     * @throws RemoteException Java RMI error
     * @throws VisADException  VisAD Data error
     */
    public static FieldStats findMinMaxAverageFromRange(FlatField field,
            UnionSet mapSets)
            throws VisADException, RemoteException {
        int[][] indices = ((mapSets == null)
                           ? null
                           : GridUtil.findContainedIndices(
                               (GriddedSet) Util.getDomainSet(field),
                               mapSets));
        float[]   mma    = { 0, 0, 0, 0 };
        float[][] values = field.getFloats(false);
        if (indices == null) {
            int len = values[0].length;
            indices = new int[1][len];
            for (int i = 0; i < len; i++) {
                indices[0][i] = i;
            }
        } else {
            //      System.err.println("indices:" + indices.length +" values:" + values[0].length);
        }

        int cnt = 0;
        for (int mapIdx = 0; mapIdx < indices.length; mapIdx++) {
            int[] indexArray = indices[mapIdx];
            //      System.err.println("   index:" + indexArray.length);
            for (int j = 0; j < indexArray.length; j++) {
                int index = indexArray[j];
                if (cnt == 0) {
                    mma[2] = mma[1] = mma[0] = values[0][index];
                } else {
                    mma[0] = Math.min(values[0][index], mma[0]);
                    mma[1] = Math.max(values[0][index], mma[1]);
                    mma[2] += values[0][index];
                }
                cnt++;
            }
        }
        if (cnt > 0) {
            mma[2] = mma[2] / cnt;
        }
        mma[3] = cnt;
        return new FieldStats(mma);
    }

    /**
     * Can the lat/lons be swapped?
     * @param grid to check
     * @return true if 2D and no CS and either lat/lon or lon/lat
     *
     * @throws VisADException problem determining if we can swap
     */
    public static boolean canSwapLatLon(FieldImpl grid)
            throws VisADException {

        Set           domain    = GridUtil.getSpatialDomain(grid);

        RealTupleType domainRef = ((SetType) domain.getType()).getDomain();
        // can't do 3D grids right yet
        if (domainRef.getDimension() > 2) {
            return false;
        }
        if ( !(domainRef.equals(RealTupleType.SpatialEarth2DTuple)
                || domainRef.equals(RealTupleType.LatitudeLongitudeTuple))) {
            return false;
        }
        return true;
    }

    /**
     * Swap the lat/lon coordinates of the grid.  Grid must be 2D and have
     * no coordinate system
     *
     * @param grid   grid to swap
     *
     * @return lat/lon swapped grid
     *
     * @throws VisADException  VisAD problem
     */
    public static FieldImpl swapLatLon(FieldImpl grid) throws VisADException {
        if ( !canSwapLatLon(grid)) {
            throw new VisADException(
                "can't swap lat/lon for this type of grid");
        }
        FieldImpl retField = null;
        try {
            if (GridUtil.isTimeSequence(grid)) {
                SampledSet   timeSet = (SampledSet) GridUtil.getTimeSet(grid);
                FunctionType retFieldType = null;
                for (int i = 0; i < timeSet.getLength(); i++) {
                    FlatField ff =
                        swapLatLonFF((FlatField) grid.getSample(i));
                    if (ff == null) {
                        continue;
                    }
                    if (retFieldType == null) {
                        retFieldType = new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            ff.getType());
                        retField = new FieldImpl(retFieldType, timeSet);
                    }
                    retField.setSample(i, ff, false);
                }
            } else {
                retField = swapLatLonFF((FlatField) grid);
            }
        } catch (RemoteException re) {}
        return retField;
    }

    /**
     * Swap the lat/lons
     *
     * @param grid   grid to swap
     *
     * @return lat/lon swapped grid
     *
     * @throws VisADException  VisAD problem
     * @throws RemoteException  remote problem
     */
    private static FlatField swapLatLonFF(FlatField grid)
            throws VisADException, RemoteException {

        FlatField llGrid = null;
        // check to make sure domains are compatible
        Set llDomain = GridUtil.getSpatialDomain(grid);
        //System.err.println("grid domain " +llDomain);

        RealTupleType llRef = ((SetType) llDomain.getType()).getDomain();
        //System.err.println("llRef = " + llRef);
        if ( !(llRef.equals(RealTupleType.SpatialEarth2DTuple)
                || llRef.equals(RealTupleType.LatitudeLongitudeTuple))) {
            throw new VisADException(
                "can't swap lat/lon for this type of grid");
        }
        RealTupleType newRef   = null;
        Unit[]        setUnits = llDomain.getSetUnits();
        if (llRef.equals(RealTupleType.SpatialEarth2DTuple)) {
            newRef = RealTupleType.LatitudeLongitudeTuple;
        } else {
            newRef = RealTupleType.SpatialEarth2DTuple;
        }
        //System.err.println("new topoRef = " + newRef);
        GriddedSet newSet     = null;
        float[][]  newSamples = null;
        if (llDomain instanceof Linear2DSet) {
            //System.out.println("linear sets");
            newSet = (llDomain instanceof LinearLatLonSet)
                     ? new LinearLatLonSet(
                         newRef,
                         new Linear1DSet[] { ((Linear2DSet) llDomain).getY(),
                                             ((Linear2DSet) llDomain)
                                             .getX() }, (CoordinateSystem) null,
                                                 new Unit[] { setUnits[1],
                    setUnits[0] }, (ErrorEstimate[]) null)
                     : new Linear2DSet(newRef,
                                       new Linear1DSet[] {
                                           ((Linear2DSet) llDomain).getY(),
                                           ((Linear2DSet) llDomain)
                                           .getX() }, (CoordinateSystem) null,
                                               new Unit[] { setUnits[1],
                    setUnits[0] }, (ErrorEstimate[]) null);
            float[][] samples = grid.getFloats(false);
            newSamples = new float[samples.length][samples[0].length];
            int[] lengths = newSet.getLengths();
            int   sizeX   = lengths[0];  // oldY
            int   sizeY   = lengths[1];  // oldX
            for (int i = 0; i < samples.length; i++) {
                int l = 0;
                for (int j = 0; j < sizeY; j++) {
                    for (int k = 0; k < sizeX; k++) {
                        //compute stride into 1D array of old structure
                        int oldelem = j + k * sizeY;
                        newSamples[i][l++] = samples[i][oldelem];
                    }
                }
            }
        }
        /*
        else {
            throw new VisADException("can't swap lat/lon for gridded set (yet)");
        }
        */
        else if (llDomain instanceof Gridded2DSet) {
            // System.out.println("gridded2D sets");
            int[]           lengths = ((GriddedSet) llDomain).getLengths();
            ErrorEstimate[] errors  = ((GriddedSet) llDomain).getSetErrors();
            float[][]       llVals  = llDomain.getSamples(true);
            // do we need to do this?
            /*
            int   sizeX    = lengths[0];
            int   sizeY    = lengths[1];
            for (int i = 0; i < samples.length; i++) {
                int l = 0;
                for (int j = 0; j < sizeY; j++) {
                    for (int k = 0; k < sizeX; k++) {
                        //compute stride into 1D array of old structure
                        int oldelem = j + k * sizeY;
                        // do something here.
                    }
                }
            }
            */
            newSet = new Gridded2DSet(newRef, new float[][] {
                llVals[1], llVals[0]
            }, lengths[1], lengths[0], (CoordinateSystem) null,
               new Unit[] { setUnits[1],
                            setUnits[0] }, new ErrorEstimate[] { errors[1],
                    errors[0] });
            newSamples = grid.getFloats(false);
        } else {
            throw new VisADException("can't swap lat/lon for "
                                     + llDomain.getClass().getName());
        }
        if ((newSet != null) && (newSamples != null)) {
            //System.out.println("newSet = " + newSet);
            FunctionType newType =
                new FunctionType(((SetType) newSet.getType()).getDomain(),
                                 GridUtil.getParamType(grid));
            llGrid = new FlatField(newType, newSet);
            llGrid.setSamples(newSamples, false);
        }

        return llGrid;
    }

    /**
     * Smooth a 2D field
     *
     * @param slice  the 2D slice
     * @param type  the type of smoothing (SMOOTH_5POINT, etc)
     *
     * @return  the smoothed grid or null
     *
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl smooth(FieldImpl slice, String type)
            throws VisADException {
        return smooth(slice, type, (type.equals(SMOOTH_GAUSSIAN)
                                    ? 6
                                    : 0));
    }

    /**
     * Is this a valid smoother type
     *
     * @param type  the type of smoothing
     *
     * @return true if a valid type
     */
    private static boolean isValidSmoother(String type) {
        return (type != null)
               && (type.equals(SMOOTH_5POINT) || type.equals(SMOOTH_9POINT)
                   || type.equals(SMOOTH_GAUSSIAN)
                   || type.equals(SMOOTH_CRESSMAN)
                   || type.equals(SMOOTH_CIRCULAR)
                   || type.equals(SMOOTH_RECTANGULAR));
    }

    /**
     * Smooth a 2D field
     *
     * @param slice  the 2D slice
     * @param type  the type of smoothing (SMOOTH_5POINT, etc)
     * @param filterLevel level of filtering (used for SMOOTH_GAUSSIAN only)
     *
     * @return  the smoothed grid or null
     *
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl smooth(FieldImpl slice, String type,
                                   int filterLevel)
            throws VisADException {

        if (GridUtil.isVolume(slice)) {
            throw new VisADException("Grid must be a 2D slice");
        }
        if ( !isValidSmoother(type)) {
            return slice;
        }
        FieldImpl smoothedFI        = null;
        TupleType smoothedRangeType = null;
        try {
            if (GridUtil.isTimeSequence(slice)) {

                // Implementation:  have to take the raw data FieldImpl
                // apart, make direction FlatField by FlatField,
                // and put all back together again into a new divergence FieldImpl

                Set timeSet = slice.getDomainSet();

                // compute each smoothed FlatField in turn; load in FieldImpl
                for (int i = 0; i < timeSet.getLength(); i++) {
                    FieldImpl smoothedFF = null;
                    FieldImpl sample = (FieldImpl) slice.getSample(i, false);
                    if (sample == null) {
                        continue;
                    }
                    if ( !isSequence(sample)) {
                        if (type.equals(SMOOTH_5POINT)) {
                            smoothedFF = smooth5Point((FlatField) sample,
                                    smoothedRangeType);
                        } else if (type.equals(SMOOTH_9POINT)) {
                            smoothedFF = smooth9Point((FlatField) sample,
                                    smoothedRangeType);
                        } else if (type.equals(SMOOTH_GAUSSIAN)) {
                            smoothedFF = smoothGaussian((FlatField) sample,
                                    filterLevel, smoothedRangeType);
                        } else {
                            smoothedFF = smoothWeighted((FlatField) sample,
                                    filterLevel, type, smoothedRangeType);
                        }
                        if (smoothedFF == null) {
                            continue;
                        }
                        if (smoothedRangeType == null) {
                            smoothedRangeType =
                                GridUtil.getParamType(smoothedFF);
                        }
                    } else {  // ensembles & such
                        Trace.call1("GridUtil smooth inner sequence");
                        Set ensDomain = sample.getDomainSet();
                        for (int j = 0; j < ensDomain.getLength(); j++) {
                            FlatField innerField =
                                (FlatField) sample.getSample(j, false);
                            if (innerField == null) {
                                continue;
                            }
                            FlatField innerSmoothedField = null;
                            if (type.equals(SMOOTH_5POINT)) {
                                innerSmoothedField = smooth5Point(innerField,
                                        smoothedRangeType);
                            } else if (type.equals(SMOOTH_9POINT)) {
                                innerSmoothedField = smooth9Point(innerField,
                                        smoothedRangeType);
                            } else if (type.equals(SMOOTH_GAUSSIAN)) {
                                innerSmoothedField =
                                    smoothGaussian(innerField, filterLevel,
                                        smoothedRangeType);
                            } else {
                                innerSmoothedField =
                                    smoothWeighted(innerField, filterLevel,
                                        type, smoothedRangeType);
                            }
                            if (innerSmoothedField == null) {
                                continue;
                            }
                            if (smoothedRangeType == null) {
                                smoothedRangeType =
                                    GridUtil.getParamType(innerSmoothedField);
                            }
                            if (smoothedFF == null) {
                                FunctionType innerType =
                                    new FunctionType(
                                        DataUtility.getDomainType(ensDomain),
                                        innerSmoothedField.getType());
                                smoothedFF = new FieldImpl(innerType,
                                        ensDomain);
                            }
                            smoothedFF.setSample(j, innerSmoothedField,
                                    false);
                        }
                        Trace.call2("GridUtil smooth inner sequence");
                    }

                    if ((smoothedFI == null) && (smoothedFF != null)) {
                        FunctionType smoothedFFType =
                            (FunctionType) smoothedFF.getType();
                        FunctionType smoothedFT =
                            new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                smoothedFFType);
                        smoothedFI = new FieldImpl(smoothedFT, timeSet);
                    }
                    if (smoothedFF != null) {
                        smoothedFI.setSample(i, smoothedFF, false, false);
                    }
                }
            } else {
                if (type.equals(SMOOTH_5POINT)) {
                    smoothedFI = (FieldImpl) smooth5Point((FlatField) slice,
                            smoothedRangeType);
                } else if (type.equals(SMOOTH_9POINT)) {
                    smoothedFI = (FieldImpl) smooth9Point((FlatField) slice,
                            smoothedRangeType);
                } else if (type.equals(SMOOTH_GAUSSIAN)) {
                    smoothedFI =
                        (FieldImpl) smoothGaussian((FlatField) slice,
                            filterLevel, smoothedRangeType);
                } else {
                    smoothedFI =
                        (FieldImpl) smoothWeighted((FlatField) slice,
                            filterLevel, type, smoothedRangeType);
                }

            }
        } catch (RemoteException re) {
            throw new VisADException("RemoteException: " + re.getMessage());
        }
        return smoothedFI;

    }

    /**
     * Apply a 5 point smoothing function to the grid.  Adapted from
     * GEMPAK dfsm5s.c
     *
     * @param slice grid to smooth
     * @param rangeType  type for the range.  May be null;
     *
     * @return  the smoothed grid or null
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField smooth5Point(FlatField slice,
                                          TupleType rangeType)
            throws VisADException, RemoteException {

        int   jgymin, jgymax, jgxmin, jgxmax, kxd;
        int   ii, ip1, im1, jp1, jm1, ier, zero;
        float wt, wt4, dip1, dim1, djp1, djm1, dsum, wsum;

        /*
         * Set filter weight.
         */
        wt  = .125f;
        wt4 = 4.f * wt;

        if (rangeType == null) {
            rangeType =
                GridUtil.makeNewParamType(GridUtil.getParamType(slice),
                                          "_SM5S");
        }
        FlatField newField = (FlatField) GridUtil.setParamType(slice,
                                 rangeType, true);
        float[][]  samples = slice.getFloats(false);
        GriddedSet domain  = (GriddedSet) GridUtil.getSpatialDomain(slice);
        int[]      lengths = domain.getLengths();
        jgxmin = 1;
        jgxmax = lengths[0];
        kxd    = jgxmax;
        jgymin = 1;
        jgymax = lengths[1];
        int       kyd       = jgymax;
        int       numParams = samples.length;
        float[]   highs     = domain.getHi();
        float[]   lows      = domain.getLow();
        boolean   isCyclic  = lows[0] == highs[0] % 360.f;
        float[][] newVals   = newField.getFloats(false);
        int       nr        = 5;

        for (int np = 0; np < numParams; np++) {
            //float[] gni = samples[np];
            //float[] gno = newVals[np];
            float[] gni;
            float[] gno;
            float[] gnii = samples[np];
            float[] gnoi = newVals[np];
            if (isCyclic) {
                gni    = extendGrid(gnii, nr, jgxmax, jgymax);
                gno    = new float[gni.length];
                jgxmax += 2 * nr;
                kxd    = jgxmax;
            } else {
                gni = gnii;
                gno = gnoi;
            }


            /*
             * Apply five-point binomial smoother over subset grid.
             */
            for (int j = jgymin; j <= jgymax; j++) {
                for (int i = jgxmin; i <= jgxmax; i++) {
                    ii = (j - 1) * kxd + i;
                    if (Float.isNaN(gni[ii - 1])) {
                        /*
                         * Check for missing data.
                         */
                        gno[ii - 1] = Float.NaN;
                    } else {
                        ip1 = ii + 1;
                        if (i + 1 > jgxmax) {
                            dip1 = Float.NaN;
                        } else {
                            dip1 = gni[ip1 - 1];
                        }
                        im1 = ii - 1;
                        if (i - 1 < jgxmin) {
                            dim1 = Float.NaN;
                        } else {
                            dim1 = gni[im1 - 1];
                        }
                        jp1 = ii + kxd;
                        if (j + 1 > jgymax) {
                            djp1 = Float.NaN;
                        } else {
                            djp1 = gni[jp1 - 1];
                        }
                        jm1 = ii - kxd;
                        if (j - 1 < jgymin) {
                            djm1 = Float.NaN;
                        } else {
                            djm1 = gni[jm1 - 1];
                        }
                        dsum = gni[ii - 1] * wt4;
                        wsum = wt4;
                        if ( !Float.isNaN(dip1)) {
                            dsum += dip1 * wt;
                            wsum += wt;
                        }
                        if ( !Float.isNaN(dim1)) {
                            dsum += dim1 * wt;
                            wsum += wt;
                        }
                        if ( !Float.isNaN(djp1)) {
                            dsum += djp1 * wt;
                            wsum += wt;
                        }
                        if ( !Float.isNaN(djm1)) {
                            dsum += djm1 * wt;
                            wsum += wt;
                        }
                        gno[ii - 1] = dsum / wsum;
                    }
                }
            }
            if (isCyclic) {
                int kxdi = kxd - 2 * nr;
                int m    = 0;
                for (int i = 0; i < kyd; i++) {
                    for (int j = 0; j < kxdi; j++) {
                        int index = nr + j + i * kxd;
                        gnoi[m++] = gno[index];
                    }
                }
            }
        }
        newField.setSamples(newVals, false);

        return newField;

    }

    /**
     * Apply a 9 point smoothing function to the grid.  Adapted from
     * GEMPAK dfsm9s.c
     *
     * @param slice grid to smooth
     * @param rangeType  type for the range.  May be null;
     *
     * @return  the smoothed grid or null
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField smooth9Point(FlatField slice,
                                          TupleType rangeType)
            throws VisADException, RemoteException {

        int   ni, no, jgymin, jgymax, jgxmin, jgxmax, kxd, kyd, ksub1, ksub2;
        int   i, j, ii, ip1, im1, jp1, jm1, imjm, ipjm, imjp, ipjp, ier, zero;
        float dsum, wsum, wt, wtc, wt4;
        float dip1, dim1, djp1, djm1, dimjm, dipjm, dimjp, dipjp;

        /*
         * Set filter weight for Diamond points weight
         */
        wt = 2.0f;

        /*
         * Corner points weight
         */
        wtc = 1.0f;

        /*
         * Center point weight
         */
        wt4 = 4.0f;

        if (rangeType == null) {
            rangeType =
                GridUtil.makeNewParamType(GridUtil.getParamType(slice),
                                          "_SM9S");
        }
        FlatField newField = (FlatField) GridUtil.setParamType(slice,
                                 rangeType, true);
        float[][]  samples = slice.getFloats(false);
        GriddedSet domain  = (GriddedSet) GridUtil.getSpatialDomain(slice);
        int[]      lengths = domain.getLengths();
        jgxmin = 1;
        jgxmax = lengths[0];
        kxd    = jgxmax;
        jgymin = 1;
        jgymax = lengths[1];
        kyd    = jgymax;
        int       numParams = samples.length;
        float[]   highs     = domain.getHi();
        float[]   lows      = domain.getLow();
        boolean   isCyclic  = lows[0] == highs[0] % 360.f;
        float[][] newVals   = newField.getFloats(false);
        int       nr        = 9;

        for (int np = 0; np < numParams; np++) {
            float[] gni;
            float[] gno;
            float[] gnii = samples[np];
            float[] gnoi = newVals[np];
            if (isCyclic) {
                gni    = extendGrid(gnii, nr, jgxmax, jgymax);
                gno    = new float[gni.length];
                jgxmax += 2 * nr;
                kxd    = jgxmax;
            } else {
                gni = gnii;
                gno = gnoi;
            }

            for (j = jgymin; j <= jgymax; j++) {
                for (i = jgxmin; i <= jgxmax; i++) {
                    ii = (j - 1) * kxd + i;
                    if (Float.isNaN(gni[ii - 1])) {
                        //
                        // Check for missing data.
                        //
                        gno[ii - 1] = Float.NaN;
                    } else {
                        ip1 = ii + 1;
                        if (i + 1 > jgxmax) {
                            dip1 = Float.NaN;
                        } else {
                            dip1 = gni[ip1 - 1];
                        }

                        im1 = ii - 1;
                        if (i - 1 < jgxmin) {
                            dim1 = Float.NaN;
                        } else {
                            dim1 = gni[im1 - 1];
                        }

                        jp1 = ii + kxd;
                        if (j + 1 > jgymax) {
                            djp1 = Float.NaN;
                        } else {
                            djp1 = gni[jp1 - 1];
                        }

                        jm1 = ii - kxd;
                        if (j - 1 < jgymin) {
                            djm1 = Float.NaN;
                        } else {
                            djm1 = gni[jm1 - 1];
                        }

                        imjm = jm1 - 1;
                        if ((j - 1 < jgymin) || (i - 1 < jgxmin)) {
                            dimjm = Float.NaN;
                        } else {
                            dimjm = gni[imjm - 1];
                        }

                        ipjm = jm1 + 1;
                        if ((j - 1 < jgymin) || (i + 1 > jgxmax)) {
                            dipjm = Float.NaN;
                        } else {
                            dipjm = gni[ipjm - 1];
                        }

                        imjp = jp1 - 1;
                        if ((j + 1 > jgymax) || (i - 1 < jgxmin)) {
                            dimjp = Float.NaN;
                        } else {
                            dimjp = gni[imjp - 1];
                        }

                        ipjp = jp1 + 1;
                        if ((j + 1 > jgymax) || (i + 1 > jgxmax)) {
                            dipjp = Float.NaN;
                        } else {
                            dipjp = gni[ipjp - 1];
                        }

                        dsum = gni[ii - 1] * wt4;
                        wsum = wt4;
                        if ( !Float.isNaN(dip1)) {
                            dsum += dip1 * wt;
                            wsum += wt;
                        } else {
                            dsum += gni[ii - 1] * wt;
                            wsum += wt;
                        }

                        if ( !Float.isNaN(dim1)) {
                            dsum += dim1 * wt;
                            wsum += wt;
                        } else {
                            dsum += gni[ii - 1] * wt;
                            wsum += wt;
                        }

                        if ( !Float.isNaN(djp1)) {
                            dsum += djp1 * wt;
                            wsum += wt;
                        } else {
                            dsum += gni[ii - 1] * wt;
                            wsum += wt;
                        }

                        if ( !Float.isNaN(djm1)) {
                            dsum += djm1 * wt;
                            wsum += wt;
                        } else {
                            dsum += gni[ii - 1] * wt;
                            wsum += wt;
                        }

                        if ( !Float.isNaN(dimjm)) {
                            dsum += dimjm * wtc;
                            wsum += wtc;
                        } else {
                            dsum += gni[ii - 1] * wtc;
                            wsum += wtc;
                        }

                        if ( !Float.isNaN(dipjm)) {
                            dsum += dipjm * wtc;
                            wsum += wtc;
                        } else {
                            dsum += gni[ii - 1] * wtc;
                            wsum += wtc;
                        }

                        if ( !Float.isNaN(dimjp)) {
                            dsum += dimjp * wtc;
                            wsum += wtc;
                        } else {
                            dsum += gni[ii - 1] * wtc;
                            wsum += wtc;
                        }

                        if ( !Float.isNaN(dipjp)) {
                            dsum += dipjp * wtc;
                            wsum += wtc;
                        } else {
                            dsum += gni[ii - 1] * wtc;
                            wsum += wtc;
                        }

                        gno[ii - 1] = dsum / wsum;
                    }
                }
            }
            if (isCyclic) {
                int kxdi = kxd - 2 * nr;
                int m    = 0;
                for (i = 0; i < kyd; i++) {
                    for (j = 0; j < kxdi; j++) {
                        int index = nr + j + i * kxd;
                        gnoi[m++] = gno[index];
                    }
                }
            }
        }
        newField.setSamples(newVals, false);

        return newField;
    }

    /** max number of weights */
    private static final int MAXWTS = 100;

    /**
     * Apply a Gaussian Weighted smoothing function to the grid.  Adapted from
     * GEMPAK dfgwfs.c
     *
     * @param slice grid to smooth
     * @param filterLevel level of filtering
     * @param rangeType  type for the range.  May be null;
     *
     * @return  the smoothed grid or null
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField smoothGaussian(FlatField slice, int filterLevel,
                                            TupleType rangeType)
            throws VisADException, RemoteException {


        int       ni, no, nnw, kxd, kyd, ksub1, ksub2, zero, ier;
        int       nwl, nr, jw, iw, jj, ii, is, ie, js, je, j, i, indx;
        float[]   gnnw, gnist, gnost;
        float     sgma, sumw, sumf, sig2, aa, x, y;
        float[][] w = new float[MAXWTS][MAXWTS];

        nwl = filterLevel;

        if (nwl <= 1) {
            nwl = 2;
        }

        /*
         * Compute the array of weights.
         *
         * The range of the filter is twice the standard deviation of the
         * required Gaussian distribution.
         */
        sgma = (float) (nwl / (Math.PI * Math.sqrt(2.0)));
        nr   = (int) (2. * sgma);
        if (nr < 1) {
            nr = 1;
        }
        if (nr >= MAXWTS) {
            nr = MAXWTS - 1;
        }

        if (rangeType == null) {
            rangeType =
                GridUtil.makeNewParamType(GridUtil.getParamType(slice),
                                          "_GWFS");
        }
        FlatField newField = (FlatField) GridUtil.setParamType(slice,
                                 rangeType, true);
        float[][]  samples = slice.getFloats(false);
        GriddedSet domain  = (GriddedSet) GridUtil.getSpatialDomain(slice);
        int[]      lengths = domain.getLengths();
        kxd = lengths[0];
        kyd = lengths[1];
        float[]   highs     = domain.getHi();
        float[]   lows      = domain.getLow();
        boolean   isCyclic  = lows[0] == highs[0] % 360.f;
        int       numParams = samples.length;
        float[][] newVals   = newField.getFloats(false);

        for (int np = 0; np < numParams; np++) {
            float[] gnisti = samples[np];
            float[] gnosti = newVals[np];
            if (isCyclic) {
                gnist = extendGrid(gnisti, nr, kxd, kyd);
                gnost = new float[gnist.length];
                kxd   += 2 * nr;
            } else {
                gnist = gnisti;
                gnost = gnosti;
            }


            /*
             * Compute the matrix of weights for one quadrant using symmetry
             * of two dimensional Gaussian surface.
             */
            sumw = 0.0f;
            sig2 = sgma * sgma;
            aa   = (float) (1.f / (sig2 * Math.PI));
            for (jw = 1; jw <= nr + 1; jw++) {
                if (jw == 1) {
                    is = 2;
                } else {
                    is = jw;
                }
                for (iw = is; iw <= nr + 1; iw++) {
                    x                 = iw - 1;
                    y                 = jw - 1;
                    w[iw - 1][jw - 1] = (float) (aa
                            * Math.exp(-(x * x + y * y) / sig2));
                    w[jw - 1][iw - 1] = w[iw - 1][jw - 1];
                    if ((jw == 1) || (jw == iw)) {
                        sumw += w[iw - 1][jw - 1];
                    } else {
                        sumw += 2. * w[iw - 1][jw - 1];
                    }
                }
            }
            sumw    *= 4.f;
            w[0][0] = 1.f - sumw;

            for (jj = 1; jj <= kyd; jj++) {
                for (ii = 1; ii <= kxd; ii++) {
                    is   = ii - nr;
                    ie   = ii + nr;
                    js   = jj - nr;
                    je   = jj + nr;
                    sumw = 0.0f;
                    sumf = 0.0f;
                    for (j = js; j <= je; j++) {
                        if ((j >= 1) && (j <= kyd)) {
                            for (i = is; i <= ie; i++) {
                                if ((i >= 1) && (i <= kxd)) {
                                    iw   = Math.abs(i - ii) + 1;
                                    jw   = Math.abs(j - jj) + 1;
                                    indx = (j - 1) * kxd + i;
                                    if ( !Float.isNaN(gnist[indx - 1])) {
                                        sumw += w[iw - 1][jw - 1];
                                        sumf += gnist[indx - 1]
                                                * w[iw - 1][jw - 1];
                                    }
                                }
                            }
                        }
                    }
                    indx = (jj - 1) * kxd + ii;
                    if ( !G_DIFFT(sumw, 0.0F, GDIFFD)
                            && !Float.isNaN(gnist[indx - 1])) {
                        gnost[indx - 1] = sumf / sumw;
                    } else {
                        gnost[indx - 1] = Float.NaN;
                    }
                }
            }
            if (isCyclic) {
                int kxdi = kxd - 2 * nr;
                int m    = 0;
                for (i = 0; i < kyd; i++) {
                    for (j = 0; j < kxdi; j++) {
                        int index = nr + j + i * kxd;
                        gnosti[m++] = gnost[index];
                    }
                }
            }
        }
        newField.setSamples(newVals, false);

        return newField;
    }

    /**
     * Apply a weigthed smoothing function to the grid.  The smoothing types are:
     * <p>
     * SMOOTH_CRESSMAN: the smoothed value is given by a weighted average of values
     * at surrounding grid points.  The weighting function is the Cressman weighting
     * function:
     * <pre>
     *         w = ( D**2 - d**2 ) / ( D**2 + d**2 )
     * </pre>
     * In the above, d is the distance (in grid increments) of the neighboring point
     * to the smoothing point, and D is the radius of influence [in grid increments]
     * <p>
     * SMOOTH_CIRCULAR: the weighting function is the circular apperture
     * diffraction function (following a suggestion of Barnes et al. 1996):
     * <pre>
     *          w = bessel(3.8317*d/D)/(3.8317*d/D)
     * </pre>
     * <p>
     * SMOOTH_RECTANGULAR: the weighting function is the product of the rectangular
     * apperture diffraction function in the x and y directions (the function used
     * in Barnes et al. 1996):
     * <pre>
     *          w = [sin(pi*x/D)/(pi*x/D)]*[sin(pi*y/D)/(pi*y/D)]
     * </pre>
     * Adapted from smooth.f written by Mark Stoelinga in his RIP package
     *
     * @param slice grid to smooth
     * @param radius radius of window in grid units
     * @param type type of smoothing
     * @param rangeType  type for the range.  May be null;
     *
     * @return  the smoothed grid or null
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FlatField smoothWeighted(FlatField slice, int radius,
                                            String type, TupleType rangeType)
            throws VisADException, RemoteException {

        float beszero = 3.8317f;

        int   idist, nfp, npsq, njx, niy;
        int   is, ie, js, je, ifp, jfp;
        float dist, distsq, xfac, yfac, tot, totwt, xdist, ydist;
        //float[][] fprint = new float[MAXWTS][MAXWTS];
        float[] pslab, work;
        int     index, psindex;

        idist = radius;
        if (idist == 0) {
            return slice;
        }
        nfp = Math.min(MAXWTS, 2 * idist);
        float[][] fprint = new float[nfp][nfp];
        npsq = idist * idist;
        if (rangeType == null) {
            rangeType =
                GridUtil.makeNewParamType(GridUtil.getParamType(slice),
                                          "_" + type);
        }
        FlatField newField = (FlatField) GridUtil.setParamType(slice,
                                 rangeType, true);
        float[][]  samples = slice.getFloats(false);
        GriddedSet domain  = (GriddedSet) GridUtil.getSpatialDomain(slice);
        int[]      lengths = domain.getLengths();
        njx = lengths[0];
        niy = lengths[1];
        float[]   highs     = domain.getHi();
        float[]   lows      = domain.getLow();
        boolean   isCyclic  = lows[0] == highs[0] % 360.f;
        int       numParams = samples.length;
        float[][] newValues = newField.getFloats(false);

        for (int np = 0; np < numParams; np++) {
            float[] pslabi = samples[np];
            float[] worki  = newValues[np];

            if (type.equals(SMOOTH_CRESSMAN)) {  // Cressman function
                for (int i = 0; i < nfp; i++) {
                    for (int j = 0; j < nfp; j++) {
                        distsq = (float) (Math.pow((i - idist), 2)
                                          + Math.pow((j - idist), 2));
                        fprint[j][i] = (float) Math.max((npsq - distsq)
                                / (npsq + distsq), 0.f);
                    }
                }
            } else if (type.equals(SMOOTH_CIRCULAR)) {  // Circular diffraction function
                for (int i = 0; i < nfp; i++) {
                    for (int j = 0; j < nfp; j++) {
                        dist = (float) (beszero / idist
                                        * Math.sqrt(Math.pow((i - idist), 2)
                                            + Math.pow((j - idist), 2)));
                        if ((i == idist) && (j == idist)) {
                            fprint[j][i] = .5f;
                        } else {
                            fprint[j][i] = (float) Math.max(0.,
                                    bes(dist) / dist);
                        }
                    }
                }
            } else if (type.equals(SMOOTH_RECTANGULAR)) {  // Rect. diffraction function
                for (int i = 0; i < nfp; i++) {
                    for (int j = 0; j < nfp; j++) {
                        if (j == idist) {
                            xfac = 1.f;
                        } else {
                            xdist = (float) Math.PI / idist * (j - idist);
                            xfac  = (float) Math.sin(xdist) / xdist;
                        }
                        if (i == idist) {
                            yfac = 1.f;
                        } else {
                            ydist = (float) Math.PI / idist * (i - idist);
                            yfac  = (float) Math.sin(ydist) / ydist;
                        }
                        fprint[j][i] = xfac * yfac;
                    }
                }
            }
            if (isCyclic) {  // pad each side with idist the values
                pslab = extendGrid(pslabi, idist, njx, niy);
                work  = new float[pslab.length];
                njx   += 2 * idist;
            } else {
                pslab = pslabi;
                work  = worki;
            }
            // now do the work of smoothing
            for (int i = 0; i < niy; i++) {
                for (int j = 0; j < njx; j++) {
                    index = j + i * njx;
                    if ( !Float.isNaN(pslab[index])) {
                        tot   = 0.f;
                        totwt = 0.f;
                        is    = Math.max(0, i - idist);
                        ie    = Math.min(niy - 1, i + idist);
                        js    = Math.max(0, j - idist);
                        je    = Math.min(njx - 1, j + idist);
                        for (int ireg = is; ireg < ie; ireg++) {
                            ifp = ireg - i + idist;
                            for (int jreg = js; jreg < je; jreg++) {
                                jfp     = jreg - j + idist;
                                psindex = ireg * njx + jreg;
                                if ( !Float.isNaN(pslab[psindex])) {
                                    totwt = totwt + fprint[jfp][ifp];
                                    tot   = tot
                                          + fprint[jfp][ifp] * pslab[psindex];
                                }
                            }
                        }
                        work[index] = tot / totwt;
                    } else {
                        work[index] = Float.NaN;
                    }
                }
            }
            if (isCyclic) {
                int njxi = njx - 2 * idist;
                int m    = 0;
                for (int i = 0; i < niy; i++) {
                    for (int j = 0; j < njxi; j++) {
                        index      = idist + j + i * njx;
                        worki[m++] = work[index];
                    }
                }
            }
        }
        newField.setSamples(newValues, false);

        return newField;
    }

    /**
     * Pad the cyclical grid on each edge by ncols
     * @param data  the data to pad
     * @param ncols  the number of columns
     * @param nx  number of x points
     * @param ny  number of y points
     * @return  extended grid
     */
    private static float[] extendGrid(float[] data, int ncols, int nx,
                                      int ny) {
        float[] newData = new float[data.length + ny * ncols * 2];
        int     index   = 0;
        int     l       = 0;
        for (int i = 0; i < ny; i++) {
            for (int j = ncols; j > 0; j--) {
                index        = nx - j - 1 + i * nx;
                newData[l++] = data[index];
            }
            for (int j = 0; j < nx; j++) {
                index        = j + i * nx;
                newData[l++] = data[index];
            }
            for (int j = 0; j < ncols; j++) {
                index        = j + 1 + i * nx;
                newData[l++] = data[index];
            }
        }
        return newData;
    }

    /**
     * Bessel function.  (copied from RIP)
     *
     * @param x  the value
     *
     * @return  the function
     */
    private static float bes(float x) {
        float rint = 0.f;
        float u;
        for (int i = 0; i < 1000; i++) {
            u    = i * .001f - .0005f;
            rint = rint
                   + (float) (Math.sqrt(1. - u * u) * Math.cos(x * u) * .001);
        }
        return (float) (2.f * x * rint / (4. * Math.atan(1.)));
    }

    /**
     * Static grid differencing value
     */
    private static final float GDIFFD = 0.000001f;

    /**
     * See if the difference between two values is greater than another value
     * @param x  first value
     * @param y  second value
     * @param val  the value to check
     * @return Math.abs(x-y) > val
     */
    private static boolean G_DIFFT(float x, float y, float val) {
        return Math.abs(x - y) < val;
    }

    /**
     * Is Z ascending or descending
     *
     * @param grid  the grid
     *
     * @return true if values are descending (1000, 925, etc)
     *
     * @throws VisADException  problem getting data
     */
    private static boolean isZDescending(FieldImpl grid)
            throws VisADException {
        //if (!GridUtil.isVolume(grid)) return false;
        Gridded3DSet domain = (Gridded3DSet) GridUtil.getSpatialDomain(grid);
        float        first     = 0;
        float        last      = 0;
        boolean      notLinear = true;
        if (domain instanceof Linear3DSet) {
            Linear1DSet zSet = ((Linear3DSet) domain).getZ();
            first     = (float) zSet.getFirst();
            last      = (float) zSet.getLast();
            notLinear = false;
        } else {
            int[]     lens    = domain.getLengths();
            float[][] samples = domain.getSamples(false);
            first = samples[2][0];
            last  = samples[2][lens[0] * lens[1] + 1];
        }
        //return first > last;
        System.out.println("not linear = " + notLinear);
        return notLinear;
    }


    /**
     * Make a grid structure
     *
     * @param grid2D  the values
     * @param numCols number of columns
     * @param numRows number of rows
     * @param missingValue  the missing value
     *
     * @return the grid structure
     */
    public static float[][] makeGrid(float[][] grid2D, int numCols,
                                     int numRows, float missingValue) {
        return makeGrid(new float[][][] {
            grid2D
        }, numCols, numRows, missingValue);
    }


    /**
     * Make a grid structure
     *
     * @param grid2D  the values
     * @param numCols number of columns
     * @param numRows number of rows
     * @param missingValue  the missing value
     *
     * @return the grid structure
     */
    public static float[][] makeGrid(float[][][] grid2D, int numCols,
                                     int numRows, float missingValue) {
        int       numFields  = grid2D.length;
        float[][] gridValues = new float[numFields][numCols * numRows];
        int       m          = 0;
        for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
            for (int j = 0; j < numRows; j++) {
                for (int i = 0; i < numCols; i++) {
                    float value = (float) grid2D[fieldIdx][j][i];
                    if (value == missingValue) {
                        value = Float.NaN;
                    }
                    gridValues[fieldIdx][m] = value;
                    m++;
                }
            }
        }
        return gridValues;
    }

    /**
     * Fill a structure with missing values with nearby grid values
     *
     * @param grid2D  grid structure
     * @param missingValue  missing value
     */
    public static void fillMissing(float[][] grid2D, float missingValue) {
        int numCols = grid2D[0].length;
        int numRows = grid2D.length;
        for (int x = 0; x < numCols; x++) {
            for (int y = 0; y < numRows; y++) {
                if (grid2D[y][x] != grid2D[y][x]) {
                    int     delta                 = numCols / 100;
                    boolean foundNonMissingNearby = false;
                    for (int dx = -delta; dx < delta; dx++) {
                        for (int dy = -delta; dy < delta; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if ((nx >= 0) && (nx < grid2D[0].length)
                                    && (ny >= 0) && (ny < grid2D.length)) {
                                if ((grid2D[ny][nx] == grid2D[ny][nx])
                                        && (grid2D[ny][nx] != missingValue)) {
                                    foundNonMissingNearby = true;
                                }
                            }
                        }
                    }
                    if ( !foundNonMissingNearby) {
                        grid2D[y][x] = missingValue;
                    }
                }
            }
        }

        for (int pass = 0; pass < 1; pass++) {
            boolean anyMissing = false;
            for (int x = 0; x < numCols; x++) {
                for (int y = 0; y < numRows; y++) {
                    if (fillMissingFromNeighbors(grid2D, x, y,
                            missingValue)) {
                        anyMissing = true;
                    }
                }
            }
            if (anyMissing) {
                for (int y = 0; y < numRows; y++) {
                    for (int x = 0; x < numCols; x++) {
                        if (fillMissingFromNeighbors(grid2D, x, y,
                                missingValue)) {
                            anyMissing = true;
                        }
                    }
                }
            }
            if (anyMissing) {
                for (int y = numRows - 1; y >= 0; y--) {
                    for (int x = numCols - 1; x >= 0; x--) {
                        if (fillMissingFromNeighbors(grid2D, x, y,
                                missingValue)) {
                            anyMissing = true;
                        }
                    }
                }
            }
            if (anyMissing) {
                for (int x = numCols - 1; x >= 0; x--) {
                    for (int y = numRows - 1; y >= 0; y--) {
                        if (fillMissingFromNeighbors(grid2D, x, y,
                                missingValue)) {
                            anyMissing = true;
                        }
                    }
                }
            }
            if ( !anyMissing) {
                break;
            }
        }
    }



    /**
     * Fill in missing values with neighbor values
     *
     * @param grid the grid
     * @param x  x point
     * @param y  y point
     * @param missingValue missing value
     *
     * @return true grid was filled
     */
    private static boolean fillMissingFromNeighbors(float[][] grid, int x,
            int y, float missingValue) {
        if (grid[y][x] == grid[y][x]) {
            return false;
        }
        if (grid[y][x] == missingValue) {
            return false;
        }
        float sum = 0;
        int   cnt = 0;
        for (int dx = -1; dx < 2; dx++) {
            for (int dy = -1; dy < 2; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                if ((nx >= 0) && (nx < grid[0].length) && (ny >= 0)
                        && (ny < grid.length)) {
                    if ((grid[ny][nx] == grid[ny][nx])
                            && (grid[ny][nx] != missingValue)) {
                        sum += grid[ny][nx];
                        cnt++;
                    }
                }
            }
        }
        if (cnt > 0) {
            grid[y][x] = sum / cnt;
        }
        return true;
    }

    /**
     * Flip the grid along the central longitude.  Useful for converting 0-360 to
     * -180 to 180 or vice-versa.
     *
     * @param grid  the grid to flip
     *
     * @return  the flipped grid
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem reading data
     */
    public static FieldImpl lonFlip(FieldImpl grid)
            throws VisADException, RemoteException {
        FieldImpl flipped = grid;
        if (isSequence(grid)) {
            Set timeDomain = grid.getDomainSet();
            flipped = new FieldImpl(((FunctionType) grid.getType()),
                                    timeDomain);
            FieldImpl flippedField = null;
            for (int timeStepIdx = 0; timeStepIdx < timeDomain.getLength();
                    timeStepIdx++) {
                FieldImpl sample = (FieldImpl) grid.getSample(timeStepIdx,
                                       false);
                if (sample == null) {
                    continue;
                }
                if ( !isSequence(sample)) {
                    flippedField = lonFlipFF((FlatField) sample);
                } else {  // ensembles & such
                    Set ensDomain = sample.getDomainSet();
                    flippedField =
                        new FieldImpl(((FunctionType) sample.getType()),
                                      ensDomain);
                    for (int j = 0; j < ensDomain.getLength(); j++) {
                        FlatField innerField =
                            (FlatField) sample.getSample(j, false);
                        if (innerField == null) {
                            continue;
                        }
                        FlatField flippedFF = lonFlipFF(innerField);
                        flippedField.setSample(j, flippedFF, false, false);
                    }
                }
                flipped.setSample(timeStepIdx, flippedField, false, false);
            }
        } else {
            flipped = (FieldImpl) lonFlipFF((FlatField) grid);
        }
        return flipped;
    }

    /**
     * Do the actually longitude flipping
     *
     * @param grid  the grid to flip
     *
     * @return the flipped grid
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem reading data
     */
    private static FlatField lonFlipFF(FlatField grid)
            throws VisADException, RemoteException {

        FlatField     flipped    = grid;
        GriddedSet    domainSet  = (GriddedSet) grid.getDomainSet();
        RealTupleType domainType =
            ((SetType) domainSet.getType()).getDomain();
        CoordinateSystem refCS   = domainSet.getCoordinateSystem();
        RealTupleType    refType = null;
        float[]          highs   = domainSet.getHi();
        float[]          lows    = domainSet.getLow();
        int[]            lengths = domainSet.getLengths();
        int              sizeX   = lengths[0];
        int              sizeY   = lengths[1];
        boolean          hasZ    = domainSet.getDimension() == 3;
        int              sizeZ   = 1;
        if (hasZ && (domainSet.getManifoldDimension() == 3)) {
            sizeZ = lengths[2];
        }
        if (refCS != null) {
            refType = refCS.getReference();
        }
        int     halfX    = (int) sizeX / 2;
        boolean is360    = highs[0] > 300;
        boolean isCyclic = isLonCyclic(lows[0], highs[0]);
        if ( !isCyclic) {
            // check to see if it could be cyclic
            if (domainSet instanceof LinearSet) {
                Linear1DSet xSet =
                    ((LinearSet) domainSet).getLinear1DComponent(0);
                double step    = xSet.getStep();
                double first   = xSet.getFirst();
                double last    = xSet.getLast();
                double newLast = last + step;
                if ( !isLonCyclic(first, newLast)) {
                    throw new VisADException("not a cyclic grid");
                }
            } else {
                double first   = lows[0];
                double last    = highs[0];
                double step    = Math.abs((last - first + 1 / sizeX));
                double newLast = last + step;
                if ( !isLonCyclic(first, newLast)) {
                    throw new VisADException("not a cyclic grid");
                }
            }
        }
        float[][] values    = grid.getFloats(false);
        float[][] newValues = new float[values.length][values[0].length];
        if ((domainSet instanceof LinearSet) && false) {
            //TODO: figure out LinearSet
        } else {  // griddedNDSet
            float[][] samples    = domainSet.getSamples(false);
            float[][] newSamples =
                new float[samples.length][samples[0].length];
            for (int k = 0; k < sizeZ; k++) {
                for (int j = 0; j < sizeY; j++) {
                    for (int i = 0; i < sizeX; i++) {
                        int oldIndex = k * sizeX * sizeY + j * sizeX + i;
                        int newIndex = ((i < halfX)
                                        ? i + halfX
                                        : i - halfX) + j * sizeX
                                            + k * sizeX * sizeY;
                        if (isCyclic && (i == sizeX - 1)) {
                            oldIndex -= halfX;
                            newIndex += halfX;
                        }
                        float oldX = samples[0][oldIndex];
                        float newX = oldX;
                        if (is360) {
                            if (oldX >= 180) {
                                newX -= 360;
                            }
                        } else {
                            if (oldX < 0) {
                                newX += 360;
                            }
                        }
                        if (isCyclic && (i == sizeX - 1)) {  //set the last point to be the first point+360
                            newX = newSamples[0][newIndex - (sizeX - 1)]
                                   + 360;
                        }
                        newSamples[0][newIndex] = newX;
                        newSamples[1][newIndex] = samples[1][oldIndex];
                        if (hasZ) {
                            newSamples[2][newIndex] = samples[2][oldIndex];
                        }
                        for (int l = 0; l < newValues.length; l++) {
                            newValues[l][newIndex] = values[l][oldIndex];
                        }
                    }
                }
            }
            GriddedSet newDomain = GriddedSet.create(domainType, newSamples,
                                       lengths,
                                       domainSet.getCoordinateSystem(),
                                       domainSet.getSetUnits(),
                                       domainSet.getSetErrors());
            flipped = new FlatField(((FunctionType) grid.getType()),
                                    newDomain,
                                    grid.getRangeCoordinateSystem()[0],
                                    grid.getRangeSets(),
                                    Util.getRangeUnits(grid));
            flipped.setSamples(newValues, false);
        }
        return flipped;

    }

    /**
     * Check to see if the longitude values are cyclic within the default epsilon
     * (i.e., last == first+360 +- 0.0005)
     *
     * @param first  first value
     * @param last   last value
     *
     * @return  true if they are cyclic
     */
    public static boolean isLonCyclic(double first, double last) {
        return isLonCyclic(first, last, 0.0005);
    }

    /**
     * Check to see if the longitude values are cyclic (i.e., last == first+360
     * within esplion)
     *
     * @param first  first value
     * @param last   last value
     * @param epsilon   last value
     *
     * @return  true if they are cyclic
     */
    public static boolean isLonCyclic(double first, double last,
                                      double epsilon) {
        return visad.util.Util.isApproximatelyEqual(first + 360., last,
                epsilon);
    }
}

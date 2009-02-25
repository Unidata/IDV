/*
 * $Id: GridUtil.java,v 1.112 2007/08/09 22:06:44 dmurray Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/oar modify it
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

import ucar.unidata.data.DataUtil;


import ucar.unidata.data.point.PointObTuple;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;

import ucar.visad.Util;

import ucar.visad.data.CachedFlatField;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.bom.Radar2DCoordinateSystem;
import visad.bom.Radar3DCoordinateSystem;

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
     * Default sampling mode used for subsampling grids
     */
    public static final int DEFAULT_SAMPLING_MODE = Data.WEIGHTED_AVERAGE;

    /**
     * Default error mode used for subsampling grids
     */
    public static final int DEFAULT_ERROR_MODE = Data.NO_ERRORS;


    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_AVERAGE = "average";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_SUM = "sum";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_MAX = "max";

    /** function for the applyFunctionOverTime routine */
    public static final String FUNC_MIN = "min";



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
        if (isConstantSpatialDomain(grid) || !isSequence(grid)) {
            return getSpatialDomain(grid, 0);
        } else {
            // find first non-missing grid
            if (isTimeSequence(grid)) {
                try {
                    Set timeDomain = grid.getDomainSet();
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

            spatialDomain = (SampledSet) field.getDomainSet();
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

        TupleType paramType = getParamType(grid);
        FunctionType rangeFT =
            new FunctionType(((SetType) newDomain.getType()).getDomain(),
                             paramType);

        FieldImpl newFieldImpl = null;
        boolean   isSequence   = isSequence(grid);
        if (isSequence) {
            // could be (time -> (domain -> value))   or
            //          (time -> (index -> (domain -> value)))  or
            //          (index -> (domain -> value))

            try {

                Set sequenceSet = grid.getDomainSet();
                int numSteps    = sequenceSet.getLength();
                MathType sequenceType =
                    ((SetType) sequenceSet.getType()).getDomain();

                FieldImpl firstSample = (FieldImpl) grid.getSample(0, false);
                boolean      hasInnerSteps = isSequence(firstSample);

                FunctionType newFieldType;
                FunctionType innerFieldType = null;

                if ( !(isSequence(firstSample))) {

                    newFieldType = new FunctionType(sequenceType, rangeFT);

                } else {

                    hasInnerSteps = true;
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
                            Set innerSet = data.getDomainSet();
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
        return ((grid.getDomainSet().getDimension() == 1)
                && !(grid instanceof FlatField));
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
        return grid.getDomainSet();
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
    public static List getDateTimeList(FieldImpl grid) throws VisADException {
        SampledSet timeSet = (SampledSet) getTimeSet(grid);
        if (timeSet == null) {
            return null;
        }
        double[][] times    = timeSet.getDoubles(false);
        Unit       timeUnit = timeSet.getSetUnits()[0];
        List       result   = new ArrayList();
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
        return (RealType) ((SetType) grid.getDomainSet().getType())
            .getDomain().getComponent(0);
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
     * This samples the given grid in both time and space and trys to return a Real value
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
     * This samples the given grid in both time and space and trys to return a Real value
     *
     * @param grid The grid
     * @param el Location
     * @param animationValue The time to sample at. If null then we just sample at the location
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
        if (is3D(grid) && !isVolume(grid)) {
            grid = make2DGridFromSlice(grid, false);
        }

        FieldImpl sampleAtLocation;
        if (is3D(grid)) {
            sampleAtLocation = GridUtil.sample(grid, el, samplingMode);
        } else {
            sampleAtLocation = GridUtil.sample(grid, el.getLatLonPoint(),
                    samplingMode);
        }
        Data data = ((animationValue == null)
                     ? (Data) sampleAtLocation
                     : (Data) sampleAtLocation.evaluate(animationValue,
                         samplingMode, Data.NO_ERRORS));

        while ((data != null) && !(data instanceof Real)) {
            if (data instanceof FieldImpl) {
                data = ((FieldImpl) data).getSample(0);
            } else if (data instanceof Tuple) {
                data = ((Tuple) data).getComponent(0);
            } else if ( !(data instanceof Real)) {
                data = null;
            }
        }
        return (Real) data;

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
                                   : new Linear1DSet(xSet.getType(),
                                       xSet.getFirst(),
                                       xSet.getFirst()
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
                    float[][] samples  = domainSet.getSamples(false);
                    int       sizeX    = domainSet.getLength(0);
                    int       sizeY    = domainSet.getLength(1);
                    int       sizeZ    = 1;
                    int       newSizeX = 1 + (sizeX - 1) / skipx;
                    int       newSizeY = 1 + (sizeY - 1) / skipy;

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
            float[][] samples  = domainSet.getSamples(false);
            int       sizeX    = domainSet.getLength(0);
            int       sizeY    = domainSet.getLength(1);
            int       sizeZ    = (domainSet.getManifoldDimension() == 3)
                                 ? domainSet.getLength(2)
                                 : 1;
            int       newSizeX = 1 + (sizeX - 1) / skipx;
            int       newSizeY = 1 + (sizeY - 1) / skipy;
            int       newSizeZ = 1 + (sizeZ - 1) / skipz;

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

        FieldImpl fi = grid;
        if ((getTimeSet(grid) == null) || isConstantSpatialDomain(grid)) {
            fi = slice(
                grid,
                makeSliceFromLevel(
                    (GriddedSet) getSpatialDomain(grid),
                    level), samplingMode);
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
                                level), samplingMode);
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
     */
    public static FieldImpl averageOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return applyFunctionOverTime(grid, FUNC_AVERAGE, makeTimes);
    }


    /**
     * Sum each grid point
     *
     * @param grid   grid to analyze
     *
     * @param grid   grid to average
     * @param makeTimes If true then make a time field with the range being the same computed value
     * If false then just return a single field of the computed values
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
     * @param makeTimes If true then make a time field with the range being the same computed value
     * If false then just return a single field of the computed values
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
     * @param makeTimes If true then make a time field with the range being the same computed value
     * If false then just return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl maxOverTime(FieldImpl grid, boolean makeTimes)
            throws VisADException {
        return applyFunctionOverTime(grid, FUNC_MAX, makeTimes);
    }


    /**
     * Apply the function to the time steps of the given grid. The function is one of the FUNC_ enums
     *
     * @param grid   grid to average
     * @param function One of the FUNC_ enums
     * @param makeTimes If true then make a time field with the range being the same computed value
     * If false then just return a single field of the computed values
     * @return the new field
     *
     * @throws VisADException  On badness
     */
    public static FieldImpl applyFunctionOverTime(FieldImpl grid,
            String function, boolean makeTimes)
            throws VisADException {
        try {
            FlatField newGrid = null;
            if ( !isTimeSequence(grid)) {
                newGrid = (FlatField) grid.clone();
                newGrid.setSamples(grid.getFloats(false), true);
                return newGrid;
            }
            boolean   doMax        = function.equals(FUNC_MAX);
            boolean   doMin        = function.equals(FUNC_MIN);
            float[][] values       = null;
            Set       timeDomain   = grid.getDomainSet();
            int       numTimeSteps = timeDomain.getLength();


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
            throw new VisADException("RemoteException checking missing data");
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
        return sliceAlongLatLonLine(grid, point, point, samplingMode);
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
     * @param samplingMode mode for sampling
     *
     * @return  spatial slice along the line.  If this is a sequence of grids
     *          it will be a sequence of the slices.
     *
     * @throws  VisADException  problem in resampling
     */
    public static FieldImpl sliceAlongLatLonLine(FieldImpl grid,
            LatLonPoint start, LatLonPoint end, int samplingMode)
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
                    end), samplingMode);
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
                                start, end), samplingMode);
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
     * with the VisAD resampling method given.
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
        SampledSet spatialSet = getSpatialDomain(grid);
        if ( !isNavigated(spatialSet)) {
            throw new IllegalArgumentException("Domain is not georeferenced");
        }
        if (spatialSet.getManifoldDimension() != 3) {
            throw new IllegalArgumentException("Grid must be 3D");
        }
        RealTuple point = null;
        try {
            if (isLatLonOrder(grid)) {
                point = new RealTuple(new Real[] { location.getLatitude(),
                        location.getLongitude(), location.getAltitude() });
            } else {
                point = new RealTuple(new Real[] { location.getLongitude(),
                        location.getLatitude(), location.getAltitude() });
            }
        } catch (RemoteException re) {
            throw new VisADException("Can't get position from point");
        }
        return sampleAtPoint(grid, point, samplingMode);
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
        SampledSet spatialSet = getSpatialDomain(grid);
        if ( !isNavigated(spatialSet)) {
            throw new IllegalArgumentException("Domain is not georeferenced");
        }
        if (spatialSet.getManifoldDimension() != 2) {
            throw new IllegalArgumentException(
                "Can't sample a 3-D grid on Lat/Lon only");
        }
        RealTuple location = null;
        try {
            if (isLatLonOrder(grid)) {
                location = new RealTuple(new Real[] { point.getLatitude(),
                        point.getLongitude() });
            } else {
                location = new RealTuple(new Real[] { point.getLongitude(),
                        point.getLatitude() });
            }
        } catch (RemoteException re) {
            throw new VisADException("Can't get position from point");
        }
        return sampleAtPoint(grid, location, samplingMode);
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
        return resampleGrid(grid, slice, samplingMode);
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
        float[][] samples    = domainSet.getSamples(false);
        int[]     lengths    = domainSet.getLengths();
        Unit[]    setUnits   = domainSet.getSetUnits();
        float[][] newSamples = new float[][] {
            samples[0], samples[1]
        };
        newSamples[0] = samples[0];
        newSamples[1] = samples[1];
        RealTupleType domainType =
            ((SetType) domainSet.getType()).getDomain();
        RealTupleType newType = null;
        if (domainSet.getCoordinateSystem() != null) {
            MapProjection mp = getNavigation(domainSet);
            newType =
                new RealTupleType((RealType) domainType.getComponent(0),
                                  (RealType) domainType.getComponent(1), mp,
                                  null);
        } else {
            newType =
                new RealTupleType((RealType) domainType.getComponent(0),
                                  (RealType) domainType.getComponent(1));
        }

        Gridded2DSet newDomainSet = new Gridded2DSet(newType, newSamples,
                                        lengths[0], lengths[1],
                                        (CoordinateSystem) null,
                                        new Unit[] { setUnits[0],
                setUnits[1] }, (ErrorEstimate[]) null, true);  // copy samples
        return setSpatialDomain(slice, newDomainSet, copy);
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

                    Set timeDomain = grid.getDomainSet();
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
                    Set timeDomain = grid.getDomainSet();
                    newField = new FieldImpl(newType, timeDomain);
                    for (int i = 0; i < timeDomain.getLength(); i++) {
                        FieldImpl indexField = (FieldImpl) grid.getSample(i,
                                                   false);
                        Set indexSet = indexField.getDomainSet();
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

                    Set timeDomain = grid.getDomainSet();
                    newField = new FieldImpl(newType, timeDomain);
                    for (int i = 0; i < timeDomain.getLength(); i++) {
                        newField.setSample(
                            i, (FieldImpl) Util.clone(
                                grid.getSample(i, false), newParam, true,
                                copy), false);
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
                    Set timeDomain = grid.getDomainSet();
                    newField = new FieldImpl(newType, timeDomain);
                    for (int i = 0; i < timeDomain.getLength(); i++) {
                        FieldImpl indexField = (FieldImpl) grid.getSample(i,
                                                   false);
                        Set indexSet = indexField.getDomainSet();
                        FieldImpl newIndexField = new FieldImpl(indexRange,
                                                      indexSet);
                        for (int j = 0; j < indexSet.getLength(); j++) {
                            newIndexField.setSample(
                                j, (FieldImpl) Util.clone(
                                    indexField.getSample(j, false),
                                    paramRange, true, copy), false);
                        }
                        newField.setSample(i, newIndexField);
                    }
                    Trace.call2("GridUtil.setParamType:indexsequence");
                }

            } else {
                // have "grid" single FlatField; neither time nor index domain
                newField = (FieldImpl) Util.clone(grid, newParam, true, copy);
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

                    SampledSet   s       = (SampledSet) grid.getDomainSet();
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
                                (SampledSet) indexFI.getDomainSet();
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
                Set sequenceDomain = grid.getDomainSet();
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
        RealType type = (RealType) level.getType();  // level type

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

        RealType zRefType = (spatialReferenceType != null)
                            ? (RealType) spatialReferenceType.getComponent(2)
                            : null;  // ref Z
        boolean isRefType = !type.equals(zType)
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
            endpoints = cs.fromReference(endpoints);
            refUnits  = cs.getReferenceUnits();
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
        FieldImpl sampledFI = null;
        // System.out.println("sampling at " + point);
        try {
            if ( !isSequence(grid)) {
                Data value = grid.evaluate(point, samplingMode,
                                           DEFAULT_ERROR_MODE);
                RealType index = RealType.getRealType("index");
                SingletonSet ss = new SingletonSet(new RealTuple(new Real[] {
                                      new Real(index, 0) }));
                sampledFI = new FieldImpl(new FunctionType(index,
                        value.getType()), ss);
                sampledFI.setSample(0, value, false);
            } else {  // some sort of sequence - evaluate each
                //                System.err.println("is sequence");
                Set sequenceDomain = grid.getDomainSet();
                for (int i = 0; i < sequenceDomain.getLength(); i++) {
                    Data sample =
                        ((FlatField) grid.getSample(i)).evaluate(point,
                            samplingMode, DEFAULT_ERROR_MODE);
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
     * @param lon       longitude values
     * @return   normalized longitude
     *
     * @throws VisADException   problem accessing set
     */
    private static double normalizeLongitude(SampledSet domain, double lon)
            throws VisADException {
        int lonindex = isLatLonOrder(domain)
                       ? 1
                       : 0;

        // check to see if domain really has RealType.Longitude
        if ( !(((RealType) (((SetType) domain.getType()).getDomain()
                .getComponent(lonindex)))
                    .equalsExceptNameButUnits(RealType.Longitude))) {
            return lon;
        }
        Unit setLonUnit = domain.getSetUnits()[lonindex];
        lon = (float) CommonUnit.degree.toThis(lon, setLonUnit);

        float low = domain.getLow()[lonindex];
        low = (float) CommonUnit.degree.toThis(low, setLonUnit);
        float hi = domain.getHi()[lonindex];
        hi = (float) CommonUnit.degree.toThis(hi, setLonUnit);

        while ((float) lon < low && (float) lon < hi) {
            lon += 360;
        }

        while ((float) lon > hi && (float) lon > low) {
            lon -= 360;
        }
        return (float) setLonUnit.toThis(lon, CommonUnit.degree);
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
                Set          sequenceDomain = grid.getDomainSet();
                FunctionType sampledType    = null;
                Trace.call1("GridUtil.sampleLoop",
                            " Length: " + sequenceDomain.getLength());
                for (int i = 0; i < sequenceDomain.getLength(); i++) {
                    Trace.call1("GridUtil getSample");
                    FlatField subField = (FlatField) grid.getSample(i);
                    Trace.call2("GridUtil getSample");

                    Trace.call1("GridUtil resample",
                                " Length=" + subField.getLength());
                    FlatField sampledField =
                        (FlatField) subField.resample(subDomain,
                            samplingMode, errorMode);
                    Trace.call2("GridUtil resample");

                    if (i == 0) {  // set up the functiontype
                        sampledType = new FunctionType(
                            ((SetType) sequenceDomain.getType()).getDomain(),
                            sampledField.getType());
                        sampledFI = new FieldImpl(sampledType,
                                sequenceDomain);
                    }
                    Trace.call1("GridUtil setSample");
                    sampledFI.setSample(i, sampledField, false);
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
                Set          sequenceDomain = grid.getDomainSet();
                SampledSet   ss             = getSpatialDomain(grid);
                FunctionType sampledType    = null;
                for (int i = 0; i < sequenceDomain.getLength(); i++) {
                    FlatField ff = (FlatField) grid.getSample(i);
                    FlatField sampledField =
                        new FlatField((FunctionType) ff.getType(), subDomain);
                    sampledField.setSamples(getSubValues(ss, ff.getFloats(),
                            skipx, skipy));
                    if (i == 0) {  // set up the functiontype
                        sampledType = new FunctionType(
                            ((SetType) sequenceDomain.getType()).getDomain(),
                            sampledField.getType());
                        sampledFI = new FieldImpl(sampledType,
                                sequenceDomain);
                    }
                    sampledFI.setSample(i, sampledField, false);
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
        int sizeX = ((GriddedSet) domainSet).getLength(0);
        int sizeY = ((GriddedSet) domainSet).getLength(1);
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
            return ((CachedFlatField) field).getRanges();
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
            int numTimes = (fieldImpl.getDomainSet()).getLength();
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
                    DateTime dt = new DateTime(times[0][i], timeUnit);
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
        SampledSet domain    = getSpatialDomain(timeStep);
        int        numPoints = domain.getLength();
        Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                                  numPoints);
        TupleType tt = getParamType(timeStep);
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
        float[][] points = CoordinateSystem.transformCoordinates(refType,
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
        System.err.println("indices time:" + (t2 - t1));
        return indices;
    }


    /**
     * _more_
     *
     * @param domain _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
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
        System.err.println("indices time:" + (t2 - t1));
        return indices;
    }




    /**
     * _more_
     *
     * @param latlon _more_
     * @param map _more_
     * @param inside _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
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

        long         t1            = System.currentTimeMillis();
        SampledSet[] sets          = map.getSets();
        List[]       indexLists    = new List[sets.length];
        List         pts           = new ArrayList();
        float[]      lonLow        = new float[sets.length];
        float[]      lonHi         = new float[sets.length];
        float[]      latLow        = new float[sets.length];
        float[]      latHi         = new float[sets.length];

        boolean      latLonOrder   = isLatLonOrder(map);
        int          numPolygonPts = 0;
        for (int j = 0; j < sets.length; j++) {
            Gridded2DSet g   = (Gridded2DSet) sets[j];
            float[]      low = g.getLow();
            float[]      hi  = g.getHi();
            lonLow[j] = (latLonOrder
                         ? low[1]
                         : low[0]);
            latLow[j] = (latLonOrder
                         ? low[0]
                         : low[1]);
            lonHi[j]  = (latLonOrder
                         ? hi[1]
                         : hi[0]);
            latHi[j]  = (latLonOrder
                         ? hi[0]
                         : hi[1]);
            float[][] sample = g.getSamples(false);
            numPolygonPts = sample[0].length;
            pts.add(sample);
        }


        int ptCnt = 0;

        for (int i = 0; i < numPoints; i++) {
            float lat = latlon[0][i];
            float lon = latlon[1][i];
            if ((lon != lon) || (lat != lat)) {
                continue;
            }
            for (int mapIdx = 0; mapIdx < sets.length; mapIdx++) {
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
                /*
                if(pointInside!=pointInside2) {
                    System.err.println("bad point:" + lon + " " + lat);
                    }*/

                boolean pointInside = pointInside2;


                boolean ok          = (inside
                                       ? pointInside
                                       : !pointInside);
                if (ok) {
                    if (indexLists[mapIdx] == null) {
                        indexLists[mapIdx] = new ArrayList();
                    }
                    indexLists[mapIdx].add(new Integer(i));
                    break;
                }
            }
        }
        System.err.println("total pts:" + numPoints + "  points inside box:"
                           + ptCnt + " # polygon points:" + numPolygonPts);
        int[][] indices = new int[sets.length][];
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
     * _more_
     *
     * @param values _more_
     * @param min _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public static int[][] findIndicesInsideRange(float[][] values, float min,
            float max)
            throws VisADException {
        return findIndicesInRange(values, min, max, true);
    }


    /**
     * _more_
     *
     * @param values _more_
     * @param min _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public static int[][] findIndicesOutsideRange(float[][] values,
            float min, float max)
            throws VisADException {
        return findIndicesInRange(values, min, max, false);
    }


    /**
     * _more_
     *
     * @param values _more_
     * @param min _more_
     * @param max _more_
     * @param inside _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
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
            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFRow      row;
            int          sheetIdx = -1;
            List         sheets   = new ArrayList();
            OutputStream fileOut =
                new BufferedOutputStream(new FileOutputStream(filename),
                                         1000000);
            int MAXROWS = 65000;
            if (isTimeSequence(grid)) {
                SampledSet timeSet    = (SampledSet) getTimeSet(grid);
                double[][] times      = timeSet.getDoubles(false);
                Unit       timeUnit   = timeSet.getSetUnits()[0];
                int        numTimes   = timeSet.getLength();
                float[][]  domainVals = null;
                int        colOffset  = 2;
                int        rowCnt;
                int        sheetCnt;
                HSSFSheet  sheet = null;
                for (int timeIdx = 0; timeIdx < numTimes; timeIdx++) {
                    DateTime dt = new DateTime(times[0][timeIdx], timeUnit);
                    JobManager.getManager().setDialogLabel1(loadId,
                            "Writing grid time:" + (timeIdx + 1) + "/"
                            + numTimes);
                    FlatField ff = (FlatField) grid.getSample(timeIdx);
                    if (ff == null) {
                        continue;
                    }
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
                        rowCnt     = -1;
                        for (int rowIdx = 0; rowIdx < domainVals[0].length;
                                rowIdx++) {
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
                            row.createCell((short) (colOffset
                                    + timeIdx)).setCellValue(dt.toString());
                        }
                        row = sheet.getRow(rowCnt + 1);
                        row.createCell(
                            (short) (colOffset + timeIdx)).setCellValue(
                            rangeVals[0][rowIdx]);
                        rowCnt++;
                    }
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
        RealTupleType    setType =
            ((SetType) domainSet.getType()).getDomain();
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


        CoordinateSystem gcs = ecs;


        RealTupleType newSetType =
            new RealTupleType(setType.getRealComponents(), gcs, null);

        Trace.call1("GeoGridAdapter final GriddedSet");
        newDSet = (Gridded3DSet) GriddedSet.create(newSetType,
                domainSet.getSamples(false), lengths, null,
                domainSet.getSetUnits(), oldErrors, false, false);


        return newDSet;


    }

}


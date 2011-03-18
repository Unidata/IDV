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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;

import ucar.unidata.util.LogUtil;

import ucar.unidata.util.ObjectArray;
import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;
import ucar.unidata.util.Trace;

import ucar.visad.Util;

import visad.*;

import visad.georef.LatLonPoint;
import visad.georef.MapProjection;



import java.rmi.RemoteException;

import java.util.Hashtable;


/**
 * A wrapper around grid data.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.72 $ $Date: 2006/12/05 18:52:36 $
 */
public class GridDataInstance extends DataInstance {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            GridDataInstance.class.getName());


    /** a sequence of such FlatFields for a sequence of times */
    private FieldImpl gridData;

    /** the original data */
    private FieldImpl origData;

    // general use meta-data describing a field

    /** the range realtypes */
    private RealType[] realTypes;

    /** ranges for each of the range parameters */
    private Range[] ranges;


    /** set of times */
    private DateTime[] dateTimes = null;

    /** flag for time sequence */
    private boolean isTimeSequence = false;

    /** flag for 3D data */
    private boolean is3D = false;

    /** flag for ensemble data */
    private boolean isEnsemble = false;

    /** ensemble dimension */
    private Gridded1DSet ensSet = null;

    /** number of ensemble members */
    private int numEns = 0;

    // grid meta-data for a 3D geogrid (not defined for 2d data)

    /** array of levels */
    private Real[] levels;

    /** the 3D coordinates */
    private float[][] coords3D;

    /** the 3D domain set */
    private Gridded3DSet domainSet3D;

    /** the spatial domain */
    private SampledSet spatialSet;

    /** the 3D size */
    private ThreeDSize size;

    /** the vertical unit */
    private Unit zUnit = null;

    /** the vertical type */
    private RealType zType;

    /** the range type */
    private TupleType rangeType;

    /** flag for re-initialization */
    private boolean reInit = false;

    /** flag for detecting changes */
    private boolean haveChangedType = false;


    /**
     * Create a GridDataInstance.
     *
     * @param dataChoice        choice describing the data
     * @param dataSelection     sub selection properties
     * @param requestProperties special control request properties
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public GridDataInstance(DataChoice dataChoice,
                            DataSelection dataSelection,
                            Hashtable requestProperties)
            throws VisADException, RemoteException {
        this(dataChoice, dataSelection, requestProperties, null);
    }

    /**
     * Create a GridDataInstance.
     *
     * @param dataChoice        choice describing the data
     * @param dataSelection     sub selection properties
     * @param requestProperties special control request properties
     * @param theData             Any initial data. Usually is null.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public GridDataInstance(DataChoice dataChoice,
                            DataSelection dataSelection,
                            Hashtable requestProperties, Data theData)
            throws VisADException, RemoteException {
        super(dataChoice, dataSelection, requestProperties, theData);
        init();
    }


    /**
     * Reinitialize this GridDataInstance
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public synchronized void reInitialize()
            throws VisADException, RemoteException {
        levels          = null;
        coords3D        = null;
        reInit          = true;
        gridData        = null;
        origData        = null;
        haveChangedType = false;
        super.reInitialize();
    }






    /**
     * Initialize this object.  Get the data and populate all the
     * parameters
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void init() throws VisADException, RemoteException {

        if (haveBeenInitialized) {
            return;
        }
        super.init();

        Trace.call1("GridDataInstance.init");

        Trace.call1("GridDataInstance.getData");
        origData = (FieldImpl) getData();
        if (origData == null) {
            inError = true;
            return;
        }
        gridData = origData;
        //jeffmc: for now clear out the origData
        origData = null;


        Trace.call2("GridDataInstance.getData");

        boolean isSequence = GridUtil.isSequence(gridData);
        isTimeSequence = GridUtil.isTimeSequence(gridData);

        //System.out.println ("  GridDataInstance.init: data function "
        //  +((FunctionType)((FieldImpl)gridData).getType()) );

        if (isTimeSequence) {  // initialize the array of times
            //System.out.println("   GridDataInstance: "+gridData.getDomainSet().getLength()
            //                 +" times");

            SampledSet timeSet = (SampledSet) Util.getDomainSet(gridData);
            if (timeSet instanceof Gridded1DSet) {
                dateTimes = DateTime.timeSetToArray((Gridded1DSet) timeSet);
            } else {
                dateTimes = DateTime.timeSetToArray(
                    DateTime.makeTimeSet(timeSet.getDoubles()[0]));
            }
            //for (int b=0; b<dateTimes.length; b++) {
            //  System.out.println ("     GridDataInstance.init data time "+dateTimes[b]);
            //}
        }


        // get max and min of all range values
        String paramName = dataChoice.getStringId();

        // to handle if data choice id is an ObjectArray
        if (dataChoice.getId() instanceof ObjectArray) {
            paramName = dataChoice.getDescription();
        }


        //field = (isTimeSequence == true)
        //                       ? (FlatField) gridData.getSample (0)
        //                       : (FlatField) gridData;

        // Have either time sequence or 
        // case of radar RHI which has Interger1DSet to several FieldImpls
        // get first FlatField in either case
        FlatField field = null;
        Data      data  = null;
        if (isSequence) {
            data = gridData.getSample(0);
            // see if this sample is either a displayable FlatField, or
            // is another FieldImpl sequence of FlatFields
            if (data instanceof FlatField) {
                isEnsemble = GridUtil.hasEnsemble(gridData);
                field      = (FlatField) data;
            } else if (data instanceof FieldImpl) {
                isEnsemble = GridUtil.hasEnsemble((FieldImpl) data);
                field      = (FlatField) ((FieldImpl) data).getSample(0);
            }
        } else {
            field = (FlatField) gridData;
        }
        if (isEnsemble) {
            ensSet = GridUtil.getEnsembleSet(gridData);
            numEns = (ensSet == null)
                     ? 0
                     : ensSet.getLength();
        }

        is3D       = (Util.getDomainSet(field).getManifoldDimension() == 3);

        spatialSet = (SampledSet) Util.getDomainSet(field);
        RealTupleType spatialType =
            ((SetType) spatialSet.getType()).getDomain();
        RealTupleType spatialReferenceType =
            (spatialSet.getCoordinateSystem() != null)
            ? spatialSet.getCoordinateSystem().getReference()
            : null;

        if (is3D) {
            zType       = (RealType) spatialType.getComponent(2);
            zUnit       = spatialSet.getSetUnits()[2];
            domainSet3D = (Gridded3DSet) spatialSet;
            size        = new ThreeDSize(domainSet3D.getLengths());
        }


        if ( !reInit) {
            MathType rType  = ((FunctionType) (field.getType())).getRange();
            int      newNum = getNextId();
            if (rType instanceof RealType) {
                rangeType = getNewType((RealType) rType, newNum);
            } else if (rType instanceof RealVectorType) {
                rangeType = getNewType((RealVectorType) rType, newNum);
            } else if (rType instanceof RealTupleType) {
                rangeType = getNewType((RealTupleType) rType, newNum);
            } else if (rType instanceof TupleType) {
                TupleType  tt  = (TupleType) rType;
                MathType[] mts = new MathType[tt.getDimension()];
                for (int i = 0; i < tt.getDimension(); i++) {
                    MathType mt = tt.getComponent(i);
                    if (mt instanceof RealType) {
                        mts[i] = getNewType((RealType) mt, newNum);
                    } else if (mt instanceof RealVectorType) {
                        mts[i] = getNewType((RealVectorType) mt, newNum);
                    } else if (mt instanceof RealTupleType) {
                        mts[i] = getNewType((RealTupleType) mt, newNum);
                    }
                }
                rangeType = new TupleType(mts);
            }
            realTypes = rangeType.getRealComponents();
        }

        Trace.call2("GridDataInstance.init");

    }

    /**
     * Create a new RealTupleType with from the RealType
     *
     * @param rt  input RealType
     * @param newNum  counter for new name
     *
     * @return RealTupleType with modified types
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException problem creating new RealType.
     */
    private RealTupleType getNewType(RealType rt, int newNum)
            throws VisADException, RemoteException {
        RealType[] types = new RealType[1];
        types[0] = getRealType(rt.getName() + "_" + newNum,
                               rt.getDefaultUnit());
        return new RealTupleType(types);
    }

    /**
     * Create a new RealTupleType with from the RealTupleType
     *
     * @param rtt  input RealTupleType
     * @param newNum  counter for new name
     *
     * @return RealTupleType with modified types
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException problem creating new RealType.
     */
    private RealTupleType getNewType(RealTupleType rtt, int newNum)
            throws VisADException, RemoteException {
        RealType[] types = new RealType[rtt.getDimension()];
        for (int i = 0; i < rtt.getDimension(); i++) {
            RealType tempRT = (RealType) rtt.getComponent(i);
            types[i] = getRealType(tempRT.getName() + "_" + newNum,
                                   tempRT.getDefaultUnit());
        }
        return new RealTupleType(types, rtt.getCoordinateSystem(), null);
    }

    /**
     * Create a new RealVectorType with from the RealVectorType
     *
     * @param evt  input RealVectorType
     * @param newNum  counter for new name
     *
     * @return RealTupleType with modified types
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException problem creating new RealType.
     */
    private RealVectorType getNewType(RealVectorType evt, int newNum)
            throws VisADException, RemoteException {
        RealType[] types = new RealType[evt.getDimension()];
        for (int i = 0; i < evt.getDimension(); i++) {
            RealType tempRT = (RealType) evt.getComponent(i);
            types[i] = getRealType(tempRT.getName() + "_" + newNum,
                                   tempRT.getDefaultUnit());
        }
        return (evt instanceof EarthVectorType)
               ? (RealVectorType) new EarthVectorType(types,
                evt.getCoordinateSystem())
               : (RealVectorType) new GridVectorType(types,
                evt.getCoordinateSystem());
    }

    /**
     * Set the parameter type.  Calls out to GridUtil.setParamType.
     *
     * @param copy  true to make a copy
     * @throws VisADException  problem setting the type
     * @see GridUtil#setParamType(FieldImpl, TupleType)
     */
    private void setParamType(boolean copy) throws VisADException {
        Trace.call1("GridDataInstance.calling setParamType", " copy=" + copy);
        gridData = GridUtil.setParamType(gridData, rangeType, copy);
        //jeffmc: call super.setData with the new data
        super.setData(gridData);
        Trace.call2("GridDataInstance.calling setParamType");
        haveChangedType = true;
    }

    /**
     * Get the geolocation information for this grid
     * @return  geolocation as a MapProjection.
     */
    public MapProjection getNavigation() {
        MapProjection mp = null;
        try {
            mp = GridUtil.getNavigation(getOriginalGrid());
        } catch (VisADException ve) {
            ;
        }
        return mp;
    }

    /**
     * Get whether this is a time sequence or not
     * @return  true if a time sequence
     */
    public boolean isTimeSequence() {

        checkInit();
        return isTimeSequence;
    }

    /**
     * Check if this is a 3D grid or not
     * @return   true if a 3D grid
     */
    public boolean is3D() {
        checkInit();
        return is3D;
    }

    /**
     * Get the spatial domain associated with this grid.  It is assumed
     * that the spatial domain is the same for each timestep.
     * @return  spatial domain
     */
    public SampledSet getSpatialDomain() {
        return spatialSet;
    }


    /**
     * Get the vertical unit for a given grid
     *
     * @param field   3D grid
     * @return  Unit of vertical coordinate
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static Unit getZUnit(FieldImpl field)
            throws VisADException, RemoteException {
        RealTupleType rTT = ((FunctionType) (field.getType())).getDomain();
        RealType      zRT = (RealType) rTT.getComponent(2);
        return zRT.getDefaultUnit();
    }


    /**
     * Return the idx'th range value
     *
     * @param idx    range index
     * @return  the Range of values for that index
     */
    public Range getRange(int idx) {
        Range[] ranges = getRanges();
        if (idx < ranges.length) {
            return ranges[idx];
        }
        return ranges[0];
    }

    /**
     * Get all the data ranges for this grid
     * @return   array of Ranges
     */
    public Range[] getRanges() {
        if (ranges == null) {
            try {
                if (gridData != null) {
                    boolean isSequence = GridUtil.isSequence(gridData);
                    ranges = (isSequence == true)
                             ? GridUtil.getMinMax(getOriginalGrid())
                             : GridUtil.fieldMinMax(
                                 (FlatField) getOriginalGrid());
                }
            } catch (Exception exc) {
                LogUtil.printException(log_, "getRange", exc);
            }
        }
        return ranges;
    }

    /**
     * The real type of the field
     *
     * @param idx   index of parameter
     * @return  RealType for parameter
     */
    public RealType getRealType(int idx) {
        checkInit();
        if (idx < realTypes.length) {
            return realTypes[idx];
        }
        return realTypes[0];
    }

    /**
     * Get the number of RealType (parameters) in this grid
     *
     * @return  number of parameters
     */
    public int getNumRealTypes() {
        checkInit();
        return realTypes.length;
    }


    /**
     * The MathType of the parameter(s)
     *
     * @return  the Tuple of parameters
     */
    public MathType getRangeType() {
        checkInit();
        return rangeType;
    }

    /**
     * Get the name of the RealType of the parameter at idx
     *
     * @param idx   index of parameter
     *
     * @return  name of the RealType.
     */
    public String getRealTypeName(int idx) {
        return getRealType(idx).getName();
    }

    /**
     * Access to the FieldImpl made. Contains a sequence of times:
     * (time -> (domain -> range)).  Does not make a copy.
     *
     * @return  the data in a FieldImpl
     * @deprecated  Use getGrid() instead
     */
    public FieldImpl getFieldImpl() {
        return getGrid();
    }

    /**
     * Access to the FieldImpl made. Contains a sequence of times:
     * (time -> (domain -> range)).  Does not copy data
     *
     * @return  the data in a FieldImpl
     */
    public FieldImpl getGrid() {
        return getGrid(false);
    }

    /**
     * Access to the FieldImpl made. Contains a sequence of times:
     * (time -> (domain -> range))
     *
     * @param   copy  make a copy if true
     * @return  the data in a FieldImpl
     */
    public FieldImpl getGrid(boolean copy) {
        checkInit();
        if (inError) {
            return null;
        }
        if ( !haveChangedType) {
            try {
                setParamType(copy);
            } catch (VisADException exc) {
                LogUtil.logException("Changing parameter type", exc);
            }
        }
        return gridData;
    }

    /**
     * Access to the original FieldImpl made. Contains a sequence of times:
     * (time -> (domain -> range))
     *
     * @return  the data in a FieldImpl
     */
    private FieldImpl getOriginalGrid() {
        checkInit();
        //jeffmc: for now clear out use
        return gridData;
        //        return origData;
    }

    /**
     * Get the default unit for the parameter at idx
     *
     * @param idx   index of parameter
     * @return  default unit
     */
    public Unit getRawUnit(int idx) {
        return getRealType(idx).getDefaultUnit();
    }

    /**
     * Get the 3D domain
     * @return  3D domain or null if not a 3D grid
     */
    public Gridded3DSet getDomainSet3D() {
        checkInit();
        return domainSet3D;
    }

    /**
     * Get the array of 3D coordinate values from the spatial set, this
     *          returns the actual array, so don't change the values!
     * @return  array of coordinates or null if not a 3D grid.  This
     *          returns the actual array, so don't change the values!
     */
    public float[][] getCoords3D() {
        checkInit();
        if (coords3D == null) {
            Trace.call1("GridDataInstance:spatialSet.getSamples");
            try {
                coords3D = spatialSet.getSamples(false);
            } catch (Exception exc) {
                LogUtil.logException("GridDataInstance.getCoords3D", exc);
            }
            Trace.call1("GridDataInstance:spatialSet.getSamples");
        }
        return coords3D;
    }

    /**
     * Get the coordinate system for the 3D domain. The reference of
     * the coordinate transform should be lat/lon/alt
     *
     * @return  coordinate system with reference of lat/lon/alt or null
     *          if 2D data
     */
    public CoordinateSystem getThreeDCoordTrans() {
        return (getDomainSet3D() == null)
               ? null
               : getDomainSet3D().getCoordinateSystem();
    }


    /**
     * Get the sizes of the 3D domain
     * @return  the sizes
     */
    public ThreeDSize getSize() {
        checkInit();
        return size;
    }

    /**
     * Get the number of X components in the domain
     * @return  number of X components
     */
    public int getSizeX() {
        return getSize().sizeX;
    }

    /**
     * Get the number of Y components in the domain
     * @return  number of Y components
     */
    public int getSizeY() {
        return getSize().sizeY;
    }

    /**
     * Get the number of Z components in the domain
     * @return  number of Z components
     */
    public int getSizeZ() {
        return getSize().sizeZ;
    }

    /**
     * Get a representative grid from one timestep.  May be entire grid
     * if not a sequence.
     * @return   one timestep grid (with no time dimension)
     */
    public FlatField getFlatField() {
        checkInit();
        if (gridData == null) {
            return null;
        }
        try {
            if (GridUtil.isSequence(gridData)) {
                Data data = gridData.getSample(0);
                if (data instanceof FlatField) {
                    return (FlatField) data;
                } else if (data instanceof FieldImpl) {
                    return (FlatField) ((FieldImpl) data).getSample(0);
                }
            } else {
                return (FlatField) gridData;
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        return null;
    }

    /**
     * Get the unit of the data's Z coordinate
     * @return Unit zUnit the data's z coordinate unit.
     */
    public Unit getZUnit() {
        checkInit();
        return zUnit;
    }

    /**
     * Get the RealType of the data's Z coordinate
     * @return RealType the RealType of the Z (vertical) coordinate
     */
    public RealType getZType() {
        checkInit();
        return zType;
    }

    /**
     * Get the name of the data's Z coordinate Unit
     * @return String zUnit from z coordinate RealType unit.
     */
    public String getZUnitName() {
        return getZUnit().getIdentifier();
    }

    /**
     * Show a parameter as a string.  Used by toString()
     *
     * @param name  name
     * @param o     additional data
     * @return   string
     */
    private String show(String name, Object o) {
        return " " + name + " " + ((o == null)
                                   ? "NULL"
                                   : o.toString());
    }

    /**
     * String representation of this GridDataInstance
     * @return  output string
     */
    public String toString() {
        return "GridDataInstance: " + "\n" + "\n" + ((gridData == null)
                ? " NULL  FIELDIMPL"
                : "") + "\n" + show("dataChoice", dataChoice) + "\n"
                      + show("realTypes", realTypes) + "\n"
                      + show("ranges", ranges) + "\n"
                      + show("unit", getRawUnit(0)) + "\n"
                      + show("domainSet3D", domainSet3D) + "\n" + "";
    }


    /**
     * Get the array of levels associated with this grid
     * @return  array of levels
     */
    public Real[] getLevels() {
        checkInit();
        if ((levels == null) && (size != null)) {
            try {
                levels = new Real[size.sizeZ];
                float[][] coords = getCoords3D();
                for (int i = 0; i < size.sizeZ; i++) {
                    levels[i] =
                        new Real(zType,
                                 coords[2][i * size.sizeX * size.sizeY],
                                 zUnit);
                }
            } catch (Exception exc) {
                LogUtil.logException("GridDataInstance.getLevels", exc);
            }
        }
        return levels;
    }

    /**
     * Get the array of times for this data
     * @return  array of times
     */
    public DateTime[] getDateTimes() {
        checkInit();
        return dateTimes;
    }

    /**
     * Slice the grid at a particular level.
     * Interface to GridUtil.sliceAtLevel().
     *
     * @param level   level to slice at
     * @return   2D grid of data at the level
     *
     * @throws VisADException  problems doing slice
     *
     * @see GridUtil#sliceAtLevel(FieldImpl, Real)
     */
    public FieldImpl sliceAtLevel(Real level) throws VisADException {
        return GridUtil.sliceAtLevel(getFieldImpl(), level);
    }

    /**
     * Slice the grid at along a lat/lon line
     * Interface to GridUtil.sliceAlongLatLonLine().
     *
     * @param start  starting position for line
     * @param end    ending position for line
     * @return  slice along the line
     *
     * @throws VisADException   problem doing slice
     * @see GridUtil#sliceAlongLatLonLine(FieldImpl, LatLonPoint, LatLonPoint)
     */
    public FieldImpl sliceAlongLatLonLine(LatLonPoint start, LatLonPoint end)
            throws VisADException {
        return sliceAlongLatLonLine(start, end, Data.WEIGHTED_AVERAGE);
    }

    /**
     * Slice the grid at along a lat/lon line
     * Interface to GridUtil.sliceAlongLatLonLine().
     *
     * @param start  starting position for line
     * @param end    ending position for line
     * @param samplingMode    type of sampling
     * @return  slice along the line
     *
     * @throws VisADException   problem doing slice
     * @see GridUtil#sliceAlongLatLonLine(FieldImpl, LatLonPoint, LatLonPoint)
     */
    public FieldImpl sliceAlongLatLonLine(LatLonPoint start, LatLonPoint end,
                                          int samplingMode)
            throws VisADException {
        return GridUtil.sliceAlongLatLonLine(getFieldImpl(), start, end,
                                             samplingMode);
    }

    /**
     * Slice (resample) the grid along the slice
     * Interface to GridUtil.slice().
     *
     * @param slice  resampling domain
     * @return  slice of data from grid along the domain
     *
     * @throws VisADException  problem with slice
     */
    public FieldImpl slice(SampledSet slice) throws VisADException {
        return GridUtil.slice(getFieldImpl(), slice);
    }

    /**
     * Get the RealType for the name and unit.
     * @param name name to use
     * @param u  Unit of RealType
     * @return corresponding RealType
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Couldn't create the RealType
     */
    private RealType getRealType(String name, Unit u)
            throws VisADException, RemoteException {
        RealType rt = RealType.getRealType(Util.cleanName(name), u);
        if (rt == null) {
            rt = Util.makeRealType(name, u);
        }
        return rt;
    }

    /**
     * Is the grid an ensemble?
     *
     * @return true if an ensemble
     */
    public boolean isEnsemble() {
        checkInit();
        return isEnsemble;
    }

    /**
     * Get the ensemble set
     * @return the set or null if not an ensemble
     */
    public Gridded1DSet getEnsembleSet() {
        checkInit();
        return ensSet;
    }

    /**
     * Get number of ensembles
     * @return number of ensemble members (or 0)
     */
    public int getNumEnsembles() {
        return numEns;
    }
}

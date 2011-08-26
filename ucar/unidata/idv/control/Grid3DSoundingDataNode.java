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

package ucar.unidata.idv.control;


import ucar.visad.functiontypes.AirTemperatureProfile;
import ucar.visad.functiontypes.CartesianHorizontalWindOfPressure;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.quantities.AirPressure;
import ucar.unidata.data.grid.GridUtil;

import visad.*;

import visad.georef.LatLonPoint;



import java.rmi.RemoteException;


/**
 * The parent class of concrete {@link SoundingDataNode} classes for gridded,
 * 3D, model-output.
 *
 * @author Steven R. Emmerson
 * @author Don Murray
 * @version $Revision: 1.4 $ $Date: 2006/12/01 20:16:33 $
 */
abstract class Grid3DSoundingDataNode extends SoundingDataNode {

    /** temperature profiler maker */
    private ProfileMaker tempMaker;

    /** dew point profile maker */
    private ProfileMaker dewMaker;

    /** wind profile maker */
    private ProfileMaker windMaker;

    /** the data */
    protected Field field;

    /**
     * Constructs from an output listener.
     *
     * @param listener               The object that will receive output from
     *                               this instance.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    Grid3DSoundingDataNode(Listener listener)
            throws VisADException, RemoteException {
        super(listener);
    }

    /**
     * <p>Sets the input {@link Data} object.</p>
     *
     * <p>This implementation invokes {@link #setOutputLocations(SampledSet)}
     * with the grid points of the input data object.</p>
     *
     * @param data                   The input data object.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public final void setData(Data data)
            throws VisADException, RemoteException {

        Field grid3d = null;
        Boolean ensble = GridUtil.hasEnsemble((FieldImpl)data);
        if(ensble) {
            FieldImpl sample =
                        (FieldImpl)((FieldImpl) data).getSample(0);
            grid3d = (Field)sample.getSample(0, false);
        } else
            grid3d = (Field) ((Field) data).getSample(0);

        synchronized (this) {
            this.field = (Field) data;
            tempMaker =
                new ProfileMaker(AirTemperatureProfile.instance(),
                                 (Gridded3DSet) grid3d.getDomainSet(),
                                 grid3d.getDefaultRangeUnits()[0]);
            dewMaker = new ProfileMaker(tempMaker,
                                        DewPointProfile.instance(),
                                        grid3d.getDefaultRangeUnits()[1]);
            if (grid3d.getDefaultRangeUnits().length > 2) {
                windMaker = new ProfileMaker(
                    tempMaker, CartesianHorizontalWindOfPressure.instance(),
                    new Unit[] { grid3d.getDefaultRangeUnits()[2],
                                 grid3d.getDefaultRangeUnits()[3] });
            }
        }

        setField((Field) data);
        setOutputTimes();
        setOutputProfiles();
        setOutputTimeIndex();
        setOutputLocations();
    }

    /**
     * <p>Sets the input {@link Field} object.</p>
     *
     * <p>This implementation does nothing.</p>
     *
     * @param field                  The input field object.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    void setField(Field field) throws VisADException, RemoteException {}

    /**
     * Set the output time index.  Subclasses need to implement.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    abstract void setOutputTimeIndex() throws VisADException, RemoteException;

    /**
     * Set the output profiles.  Subclasses need to implement.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    abstract void setOutputProfiles() throws VisADException, RemoteException;

    /**
     * Make the temperature profile from the values.
     *
     * @param values  values for profile
     * @return   Field of data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    final Field makeTempProfile(float[] values)
            throws VisADException, RemoteException {
        return tempMaker.makeProfile(values);
    }

    /**
     * Make the dewpoint profile from the values.
     *
     * @param values  values for profile
     * @return   Field of data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    final Field makeDewProfile(float[] values)
            throws VisADException, RemoteException {
        return dewMaker.makeProfile(values);
    }

    /**
     * Make the wind profile from the values.
     *
     * @param values  values for profile
     * @return   Field of data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    final Field makeWindProfile(float[][] values)
            throws VisADException, RemoteException {
        return (windMaker == null)
               ? null
               : windMaker.makeProfile(values);
    }

    /**
     * <p>Sets the input, horizontal location.</p>
     *
     * <p>This implementation invokes {@link
     * SoundingDataNode.Listener#setLocation(LatLonPoint)} and {@link
     * SoundingDataNode.Listener#setProfile(Field[], Field[])} if and when
     * appropriate.</p>
     *
     * @param loc                    The horizontal location.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public final void setLocation(LatLonPoint loc)
            throws VisADException, RemoteException {

        if (loc == null) {
            throw new NullPointerException();
        }

        boolean notify = false;

        synchronized (this) {
            if ( !loc.equals(inLoc)) {
                inLoc  = loc;
                notify = true;
            }
        }

        if (notify) {
            setOutputLocation();
            setOutputProfiles();
        }
    }

    /**
     * <p>Sets the input time.</p>
     *
     * <p>This implementation invokes {@link
     * SoundingDataNode.Listener#setTimeIndex(int)} if and when
     * appropriate.</p>
     *
     * @param time                   The input time.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public final void setTime(DateTime time)
            throws VisADException, RemoteException {

        if (time == null) {
            throw new NullPointerException();
        }

        boolean notify = false;

        synchronized (this) {
            if ( !time.equals(inTime)) {
                inTime = time;
                notify = true;
            }
        }

        if (notify) {
            setOutputTimeIndex();
        }
    }

    /**
     * Set the output times
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    final void setOutputTimes() throws VisADException, RemoteException {

        SampledSet times = null;

        synchronized (this) {
            if (field != null) {
                times = (SampledSet) field.getDomainSet();
            }
        }

        if (times != null) {
            setOutputTimes(times);
        }
    }

    /**
     * Set the output locations
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    final void setOutputLocations() throws VisADException, RemoteException {

        SampledSet locs = null;

        synchronized (this) {
            if (field != null) {
                locs = (SampledSet) ((Field) field.getSample(
                    0)).getDomainSet();
            }
        }

        if (locs != null) {
            setOutputLocations(locs);
        }
    }

    /**
     * Set the output location.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    final void setOutputLocation() throws VisADException, RemoteException {

        LatLonPoint loc = null;

        synchronized (this) {
            if (inLoc != null) {
                loc = inLoc;
            }
        }

        if (loc != null) {
            setOutputLocation(loc);
        }
    }

    /**
     * Class ProfileMaker
     *
     */
    private static final class ProfileMaker {

        /** output domain set */
        private final Set outDom;

        /** output range units */
        private final Unit[] rangeUnits;

        /** function type for data */
        private final FunctionType funcType;

        /**
         * Create a new profiler maker
         *
         * @param funcType   function type
         * @param inDom      initial domain
         * @param rangeUnit  range units
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         *
         */
        ProfileMaker(FunctionType funcType, Gridded3DSet inDom,
                     Unit rangeUnit)
                throws VisADException, RemoteException {
            this(funcType, inDom, new Unit[] { rangeUnit });
        }

        /**
         * Create a new profiler maker
         *
         * @param funcType   function type
         * @param inDom      initial domain
         * @param rangeUnits  range units
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         *
         */
        ProfileMaker(FunctionType funcType, Gridded3DSet inDom,
                     Unit[] rangeUnits)
                throws VisADException, RemoteException {

            if (funcType == null) {
                throw new NullPointerException();
            }

            this.funcType = funcType;

            float[][] levels;
            float[]   levelSamples = inDom.getSamples()[2];
            int       xySize       = (inDom.getManifoldDimension() == 1)
                                     ? 1
                                     : inDom.getLength(0)
                                       * inDom.getLength(1);
            int       zSize        = (inDom.getManifoldDimension() == 1)
                                     ? inDom.getLength()
                                     : inDom.getLength(2);
            int       numBad       = 0;

            for (int i = 0; i < zSize; i++) {
                if (levelSamples[i * xySize] != levelSamples[i * xySize]) {
                    numBad++;
                }
            }

            float[] levs = new float[zSize - numBad];

            for (int i = 0, j = 0; i < zSize; i++) {
                if (levelSamples[i * xySize] == levelSamples[i * xySize]) {
                    levs[j++] = levelSamples[i * xySize];
                }
            }

            levels = new float[][] {
                levs
            };
            Unit verticalUnit = inDom.getSetUnits()[2];

            if (((SetType) inDom.getType()).getDomain().getComponent(
                    2).equals(RealType.Altitude)) {
                levels = Set.doubleToFloat(
                    AirPressure.getStandardAtmosphereCS().fromReference(
                        Set.floatToDouble(levels),
                        new Unit[] { verticalUnit }));
                verticalUnit =
                    AirPressure.getStandardAtmosphereCS()
                        .getCoordinateSystemUnits()[0];
            }

            outDom = new Gridded1DSet(AirPressure.getRealTupleType(), levels,
                                      levels[0].length, null,
                                      new Unit[] { verticalUnit }, null);
            this.rangeUnits = rangeUnits;
        }

        /**
         * Make a ProfilerMaker from another
         *
         * @param that      other to use
         * @param funcType  function type
         * @param rangeUnit range unit
         */
        ProfileMaker(ProfileMaker that, FunctionType funcType,
                     Unit rangeUnit) {

            if (funcType == null) {
                throw new NullPointerException();
            }

            if (rangeUnit == null) {
                throw new NullPointerException();
            }

            outDom        = that.outDom;
            rangeUnits    = new Unit[] { rangeUnit };
            this.funcType = funcType;
        }

        /**
         * Make a ProfilerMaker from another
         *
         * @param that        other to use
         * @param funcType    function type
         * @param rangeUnits  range unit
         */
        ProfileMaker(ProfileMaker that, FunctionType funcType,
                     Unit[] rangeUnits) {

            if (funcType == null) {
                throw new NullPointerException();
            }

            if (rangeUnits == null) {
                throw new NullPointerException();
            }

            outDom          = that.outDom;
            this.rangeUnits = rangeUnits;
            this.funcType   = funcType;
        }

        /**
         * Make a profile from the values
         *
         * @param values   values for data
         * @return  field representing the profile
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        Field makeProfile(float[] values)
                throws VisADException, RemoteException {

            //FlatField field = new FlatField(AirTemperatureProfile.instance(),
            FlatField field = new FlatField(funcType, outDom,
                                            (CoordinateSystem[]) null,
                                            (Set[]) null, rangeUnits);

            field.setSamples(new float[][] {
                values
            });

            return field;

        }

        /**
         * Make a profile from the values
         *
         * @param values   values for 2D (ie wind) data
         * @return  field representing the profile
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        Field makeProfile(float[][] values)
                throws VisADException, RemoteException {

            FlatField field =
                new FlatField(CartesianHorizontalWindOfPressure.instance(),
                              outDom, (CoordinateSystem[]) null,
                              (Set[]) null, rangeUnits);

            field.setSamples(values);

            return field;
        }
    }
}

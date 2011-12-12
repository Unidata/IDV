/*
 * $Id: WindBarbStaff.java,v 1.12 2005/05/13 18:33:40 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.awt.GridLayout;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import javax.swing.*;

import ucar.unidata.beans.*;

import ucar.visad.functiontypes.*;
import ucar.visad.quantities.*;

import visad.*;
import visad.bom.*;

import visad.java2d.*;


/**
 * Provides support for displaying a vertical profile of the horizontal wind
 * as a set of wind arrows off a vertical staff.
 *
 * @author Don Murray
 * @version $Id: WindBarbStaff.java,v 1.12 2005/05/13 18:33:40 jeffmc Exp $
 */
public class WindBarbStaff extends JPanel implements PropertyChangeListener {

    /** set of properties */
    private PropertySet propertySet;

    /** min height property */
    private NonVetoableProperty minimumGeopotentialAltitudeProperty;

    /** max height property */
    private NonVetoableProperty maximumGeopotentialAltitudeProperty;

    /** displayed speed unit property */
    private NonVetoableProperty displayedSpeedUnitProperty;

    /** wind profile property */
    private NonVetoableProperty windProfileReferenceProperty;

    /** display for wind barb staff */
    private DisplayImpl display;

    /** wind profile reference */
    private DataReferenceImpl windProfileRef;

    /** original wind profile */
    private Field originalWindProfile;

    /** working wind profile */
    private Field workingWindProfile;

    /** centerline reference for display */
    private DataReferenceImpl centerLineRef;

    /** centerline data */
    private Gridded2DSet centerLineData;

    /** empty data */
    private Data emptyData;

    /** ScalarMap for the vertical coordinate */
    private ScalarMap verticalCoordinateMap;

    /** min height */
    private float minGeopotentialAltitude;

    /** max height */
    private float maxGeopotentialAltitude;

    /** FlowControl for displayed winds */
    private FlowControl flowControl;

    /**
     * Construct a new wind staff display using the default maximum
     * and minimum heights.
     *
     * @throws   VisADException   necessary VisAD object could not be created.
     * @throws   RemoteException  Java RMI exception
     */
    public WindBarbStaff() throws VisADException, RemoteException {
        this(0.f, 16000.f);
    }

    /**
     * Construct a new wind staff display using the default maximum
     * and minimum heights.
     *
     * @param   minZ    Minimum geopotential height in meters
     * @param   maxZ    Maximum geopotential height in meters
     *
     * @throws   VisADException   necessary VisAD object could not be created.
     * @throws   RemoteException  Java RMI exception
     */
    public WindBarbStaff(float minZ, float maxZ)
            throws VisADException, RemoteException {

        super(new GridLayout(1, 1));

        // Set up the properties
        propertySet = new PropertySet();

        propertySet.addProperty(windProfileReferenceProperty =
            new NonVetoableProperty(this, "windProfileReference"));
        windProfileReferenceProperty.setValue(windProfileRef =
            new DataReferenceImpl("WindProfileRef"));
        propertySet.addProperty(minimumGeopotentialAltitudeProperty =
            new NonVetoableProperty(this, "minimumGeopotentialAltitude"));
        minimumGeopotentialAltitudeProperty.setValue(
            new Real(GeopotentialAltitude.getRealType(), minZ));
        propertySet.addProperty(maximumGeopotentialAltitudeProperty =
            new NonVetoableProperty(this, "maximumGeopotentialAltitude"));
        maximumGeopotentialAltitudeProperty.setValue(
            new Real(GeopotentialAltitude.getRealType(), maxZ));

        // Create an emptyData value
        emptyData = new Real(-9999);  // set the empty data value

        // Create the display
        display = new DisplayImplJ2D("WindStaff");

        display.getDisplayRenderer().setBoxOn(false);
        display.getDisplayRenderer().setCursorStringOn(false);

        //display.addDisplayListener(this);
        // Vertical coordinates
        verticalCoordinateMap =
            new ScalarMap(GeopotentialAltitude.getRealType(), Display.YAxis);

        display.addMap(verticalCoordinateMap);
        verticalCoordinateMap
            .setRange(0, ((Real) maximumGeopotentialAltitudeProperty
                .getValue()).getValue());

        // Horizontal Coordinates
        ScalarMap horizontalCoordinateMap = new ScalarMap(RealType.XAxis,
                                                Display.XAxis);

        display.addMap(horizontalCoordinateMap);

        //horizontalCoordinateMap.setRange(-.1, .1);
        // wind coordinates
        ScalarMap uMap = new ScalarMap(WesterlyWind.getRealType(),
                                       Display.Flow1X);

        display.addMap(uMap);
        uMap.setRange(-1.0, 1.0);

        ScalarMap vMap = new ScalarMap(SoutherlyWind.getRealType(),
                                       Display.Flow1Y);

        display.addMap(vMap);
        vMap.setRange(-1.0, 1.0);

        flowControl = (FlowControl) vMap.getControl();

        flowControl.setFlowScale(0.15f);
        flowControl.setBarbOrientation(FlowControl.NH_ORIENTATION);

        // Center line reference
        centerLineRef = new DataReferenceImpl("linerefs");

        ConstantMap[] centerLineConstantMap = new ConstantMap[4];

        centerLineConstantMap[0] = new ConstantMap(1.0, Display.Red);
        centerLineConstantMap[1] = new ConstantMap(0.0, Display.Green);
        centerLineConstantMap[2] = new ConstantMap(0.0, Display.Blue);
        centerLineConstantMap[3] = new ConstantMap(2.0, Display.LineWidth);

        display.addReference(centerLineRef, centerLineConstantMap);
        createCenterLineSet();
        centerLineRef.setData(centerLineData);
        add(display.getComponent());
    }

    /**
     * Set the wind profile to be displayed.
     *
     * @param    windProfile      Field of wind profile.  Must be an
     *                            instance of a CartesianHorizontalWindProfile
     * @throws   VisADException   necessary VisAD object could not be created,
     *                            profile is not changed.
     * @throws   RemoteException  Java RMI exception
     */
    public void setWindProfile(Field windProfile)
            throws VisADException, RemoteException {

        // Check to see if range is Polar or Cartesian
        FunctionType type = (FunctionType) windProfile.getType();

        if ( !type.equals(
                CartesianHorizontalWindOfGeopotentialAltitude.instance())) {
            throw new VisADException("invalid functionType");
        }

        /*
        workingWindProfile =
          (type.equals(
              CartesianHorizontalWindOfGeopotentialAltitude.instance()))
                  ? windProfile
                  : transformToCartesian(windProfile);
        */
        originalWindProfile = (Field) windProfile.dataClone();

        // determine heights for scaling altitude (Z) axis
        Gridded1DSet domainSet =
            (Gridded1DSet) originalWindProfile.getDomainSet();

        minimumGeopotentialAltitudeProperty.setValue(
            new Real(
                GeopotentialAltitude.getRealType(), domainSet.getLowX()));
        maximumGeopotentialAltitudeProperty.setValue(
            new Real(GeopotentialAltitude.getRealType(), domainSet.getHiX()));
        createCenterLineSet();
        centerLineRef.setData(centerLineData);
        verticalCoordinateMap.setRange(0, domainSet.getHiX());

        // clone to create a working profile
        ((DataReferenceImpl) windProfileReferenceProperty.getValue()).setData(
            windProfile);

        //workingWindProfile = (FlatField) originalWindProfile.clone();
        //createManipRefs(workingWindProfile);
    }

    /**
     *  Sets the orientation of the wind barbs
     *
     * @param    orientation      either FlowControl.NH_ORIENTATION or
     *                            FlowControl.SH_ORIENTATION
     * @throws   VisADException   necessary VisAD object could not be created,
     *                            profile is not changed.
     * @throws   RemoteException  Java RMI exception
     */
    public void setBarbOrientation(int orientation)
            throws VisADException, RemoteException {
        flowControl.setBarbOrientation(orientation);
    }

    /**
     * Set the wind profile to be displayed.
     *
     * @throws   VisADException   necessary VisAD object could not be created,
     *                            profile is not changed.
     * @throws   RemoteException  Java RMI exception
     */
    public void resetWindProfile() throws VisADException, RemoteException {

        if (originalWindProfile != null) {
            windProfileRef.setData(originalWindProfile);
        }
    }

    /**
     *  Set the reference for the wind profile.
     *
     * @param  windProfileRef   data reference to the wind profile
     *
     * @throws VisADException  reference has already been set
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setWindProfileReference(DataReferenceImpl windProfileRef)
            throws VisADException, RemoteException {

        if (display.findReference(windProfileRef) == null) {
            display.addReferences(new BarbRendererJ2D(), windProfileRef);
            windProfileReferenceProperty.setValue(windProfileRef);

            /*
            CellImpl cell = new CellImpl()
            {
                public void doAction()
                    throws VisADException, RemoteException
                {
                    DataReferenceImpl ref = (DataReferenceImpl)
                      windProfileReferenceProperty.getValue();
                    if ( ref != null)
                    {
                       createManipRefs( (Field) windProfileRef.getData());
                    }
                }
            };
            cell.addReference(windProfileRef);
            */
        } else {
            throw new VisADException("Reference already exists");
        }
    }

    /**
     * Set the visibility of the line along which the barbs are plotted
     *
     * @param   value   if true, line is displayed, otherwise it is hidden
     *
     * @throws   VisADException   necessary VisAD object could not be created,
     *                            visibility is not changed.
     * @throws   RemoteException  Java RMI exception
     */
    public void setWindStaffVisible(boolean value)
            throws VisADException, RemoteException {

        centerLineRef.setData((value == true)
                              ? (Data) centerLineData
                              : (Data) emptyData);
    }


    /**
     *  creates the data for the line down the middle
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void createCenterLineSet()
            throws VisADException, RemoteException {

        RealTupleType lineType =
            new RealTupleType(RealType.XAxis,
                              (RealType) verticalCoordinateMap.getScalar());
        float[][] linevals = new float[2][3];
        double maxGeopotentialAltitude =
            ((Real) maximumGeopotentialAltitudeProperty.getValue())
                .getValue();
        float range = (float) maxGeopotentialAltitude;

        //float range = maxGeopotentialAltitude - minGeopotentialAltitude;
        for (int i = 0; i < 3; i++) {
            linevals[0][i] = 0.0f;
            linevals[1][i] = (float) (maxGeopotentialAltitude
                                      - (range / 2 * i));
        }

        centerLineData = new Gridded2DSet(lineType, linevals, 3);
    }

    /**
     * converts an input wind profile to cartesian coordinates
     *
     * @param  input  wind profile to transform
     * @return  wind profile in cartesian coordinates
     *
     * @throws RemoteException
     * @throws VisADException
     */
    private FlatField transformToCartesian(FlatField input)
            throws VisADException, RemoteException {

        FunctionType inputFunction = (FunctionType) input.getType();

        if ( !inputFunction.getDomain().getComponent(0).equals(
                GeopotentialAltitude.getRealType())) {
            throw new VisADException("Wrong domain type");
        }

        RealTupleType cartesianType =
            CartesianHorizontalWind.getRealTupleType();
        FlatField newField =
            new FlatField(
                CartesianHorizontalWindOfGeopotentialAltitude.instance(),
                input.getDomainSet());
        RealTupleType inputType =
            (RealTupleType) ((FunctionType) input.getType()).getRange();
        ErrorEstimate[] inputErrors  = input.getRangeErrors();
        ErrorEstimate[] outputErrors = new ErrorEstimate[inputErrors.length];

        newField.setSamples(
            CoordinateSystem.transformCoordinates(
                cartesianType, cartesianType.getCoordinateSystem(),
                cartesianType.getDefaultUnits(), outputErrors, inputType,
                ucar.visad.Util.getRangeCoordinateSystem(input),
                ucar.visad.Util.getRangeUnits(input), inputErrors,
                input.getValues()));

        return newField;
    }

    /**
     * Handles a change to a property that's being listened to.
     *
     * @param pce         The property-change event.
     */
    public void propertyChange(PropertyChangeEvent pce) {

        try {
            if (pce.getPropertyName().equals("windProfile")) {
                windProfileRef.setData((Field) pce.getNewValue());
            }
        } catch (VisADException e) {
            ;
        } catch (RemoteException e) {
            ;
        }
    }

    /**
     * Tests this class.
     *
     * @param args          Invocation arguments.  Ignored.
     * @throws Exception    A problem occurred.
     */
    public static void main(String[] args) throws Exception {

        JFrame            frame = new JFrame("Wind Barb Staff");
        WindBarbStaff     wbs   = new WindBarbStaff();
        DataReferenceImpl dref  = new DataReferenceImpl("winds");

        wbs.setWindProfileReference(dref);

        // Create a dummy flat field
        float[][] levels = new float[1][10];
        float[][] uv     = new float[2][10];

        for (int i = 0; i < 10; i++) {
            levels[0][i] = i * 500.f;
            uv[0][i]     = (i % 2 == 0)
                           ? i * 2.f
                           : -i * 2.f;
            uv[1][i]     = (i % 3 == 0)
                           ? -i * 2.f
                           : i * 2.f;
        }

        FunctionType ftype =
            new FunctionType(GeopotentialAltitude.getRealType(),
                             CartesianHorizontalWind.getRealTupleType());
        Gridded1DSet domainSet =
            new Gridded1DSet(GeopotentialAltitude.getRealType(), levels, 10);
        Field windProfile = new FlatField(ftype, domainSet);

        windProfile.setSamples(uv);
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.getContentPane().add(wbs);
        frame.pack();
        frame.setSize(700, 700);
        frame.setVisible(true);
        wbs.setWindProfile(windProfile);

        /*
        for (;;)
        {
            Thread.sleep(2000);
            wbs.setWindStaffVisible(false);
            Thread.sleep(2000);
            wbs.setWindStaffVisible(true);
            Thread.sleep(2000);
            wbs.setWindProfile(windProfile);
            Thread.sleep(10000);
            wbs.resetWindProfile();
        }
        */
    }

    /**
     * Create data references for manipulating the barbs
     *
     * @param f   field to create refs from
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void createManipRefs(FlatField f)
            throws VisADException, RemoteException {

        Gridded1DSet        domain      = (Gridded1DSet) f.getDomainSet();
        int                 numLevels   = domain.getLength(0);
        float               heights[][] = domain.getSamples(true);
        DataReferenceImpl[] refs        = new DataReferenceImpl[numLevels];

        for (int i = 0; i < numLevels; i++) {
            float     z  = heights[0][i];
            RealTuple uv = (RealTuple) f.getSample(i);
            Tuple tuple = new Tuple(new Data[]{
                              new Real(GeopotentialAltitude.getRealType(), z),
                              uv });

            refs[i] = new DataReferenceImpl("ref_" + i);

            refs[i].setData(tuple);
            display.addReferences(new BarbManipulationRendererJ2D(), refs[i]);

            WindAdjuster cell = new WindAdjuster(refs[i]);

            cell.addReference(refs[i]);
        }
    }

    /**
     * Class WindAdjuster
     *
     * @author Unidata development team
     */
    class WindAdjuster extends CellImpl {

        /** reference for cell */
        DataReferenceImpl ref;

        /** index for data */
        int sampleIndex;

        /**
         * Create a new WindAdjuster cell for the given reference
         *
         * @param r  data reference to listen to
         *
         */
        public WindAdjuster(DataReferenceImpl r) {

            ref = r;

            String name = r.getName();

            sampleIndex = Integer.parseInt(name.substring(name.indexOf("_")
                    + 1));
        }

        /**
         * Handle changes.
         *
         * @throws RemoteException    Java RMI problem
         * @throws VisADException     VisAD problem
         */
        public void doAction() throws VisADException, RemoteException {

            Tuple tuple = (Tuple) ref.getData();

            //workingWindProfile.setSample(sampleIndex, tuple.getComponent(1));
        }
    }
}

/*
 * $Id: WindStaffDisplay.java,v 1.21 2005/05/13 18:33:42 jeffmc Exp $
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



import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;

import javax.swing.*;

import ucar.visad.display.*;
import ucar.visad.functiontypes.CartesianHorizontalWindOfGeopotentialAltitude;
import ucar.visad.quantities.*;
import ucar.visad.Util;

import visad.*;
import visad.java2d.DisplayImplJ2D;

import visad.LocalDisplay;


/**
 * Provides support for a display comprising a wind staff.
 *
 * @author Don Murray
 * @author Steven R. Emmerson
 * @version $Id: WindStaffDisplay.java,v 1.21 2005/05/13 18:33:42 jeffmc Exp $
 */
public class WindStaffDisplay extends WindProfileDisplay {

    /** Center pole displayable */
    private CenterPole centerPole;

    /** map for offset */
    private static ConstantMap xOffset;

    /** tuple type for mean wind */
    private static TupleType meanWindArrowTupleType;

    /** real type for mean wind height */
    private static RealType meanWindAltitudeType;

    /** missing wind arrow */
    private static WindArrow missingMeanWindArrow;

    static {
        try {
            xOffset = new ConstantMap(-0.7, Display.XAxis);
            meanWindAltitudeType =
                Util.clone(GeopotentialAltitude.getRealType(),
                           "MeanWindGeopotentialAltitude");
            meanWindArrowTupleType = new TupleType(new MathType[]{
                meanWindAltitudeType,
                CartesianHorizontalWind.getRealTupleType() });
            missingMeanWindArrow = new WindArrow(
            /*Display.Flow1X, Display.Flow1Y, */
            meanWindArrowTupleType);

            missingMeanWindArrow.setManipulable(false);
            missingMeanWindArrow.addConstantMap(xOffset);
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs from nothing.  Uses default min, max geopotential altitudes.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindStaffDisplay() throws VisADException, RemoteException {
        this(getDefaultMinAltitude(), getDefaultMaxAltitude());
    }

    /**
     * Constructs with a given altitude extent.
     * @param minZ              The minimum geopotential altitude.
     * @param maxZ              The maximum geopotential altitude.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindStaffDisplay(Real minZ, Real maxZ)
            throws VisADException, RemoteException {

        super(new DisplayImplJ2D("WindStaffDisplay", new WindStaffDisplayRenderer()),
              minZ, maxZ, 7, Display.YAxis);

        addScalarMaps();

        centerPole = new CenterPole("WindStaffCenterPole",
                                    GeopotentialAltitude.getRealType());

        centerPole.setLineWidth(2);
        centerPole.addConstantMap(xOffset);
        addDisplayable(centerPole);
        setCenterPoleExtent();
        {
            ProjectionControl pc        = getDisplay().getProjectionControl();
            AffineTransform   transform = new AffineTransform(pc.getMatrix());

            // transform.scale(3.4, 3.4);
            transform.translate(0.1 - xOffset.getConstant(), 0);

            double[] matrix = new double[6];

            transform.getMatrix(matrix);
            pc.setMatrix(matrix);
            ((JPanel) getDisplay().getComponent()).setPreferredSize(
                new Dimension(156, 400));
        }
        saveProjection();
    }

    /**
     * Returns the displayable {@link WindProfile} appropriate to this instance.
     * This is a template method.
     *
     * @return                  The displayable WindProfile appropriate to this
     *                          instance.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected WindProfile newWindProfile()
            throws VisADException, RemoteException {

        WindStaff windStaff = new WindStaff((LocalDisplay) null);

        windStaff.addConstantMap(xOffset);

        return windStaff;
    }

    /**
     * Adds the set of ScalarMap-s specific to this instance.
     *
     * @throws VisADException   VisAD failure.
     */
    protected void addScalarMaps() throws VisADException {

        try {
            ScalarMap uMap =
                new ScalarMap(WindStaff.getWesterlyWindRealType(),
                              Display.Flow1X);

            uMap.setRange(-1, 1);                                // necessary for some reason
            addScalarMap(uMap);

            final ScalarMap vMap =
                new ScalarMap(WindStaff.getSoutherlyWindRealType(),
                              Display.Flow1Y);

            vMap.setRange(-1, 1);                                // necessary for some reason
            vMap.addScalarMapListener(new ScalarMapListener() {

                public void controlChanged(ScalarMapControlEvent event)
                        throws RemoteException, VisADException {

                    int id = event.getId();

                    if ((id == event.CONTROL_ADDED)
                            || (id == event.CONTROL_REPLACED)) {
                        FlowControl flowControl =
                            (FlowControl) vMap.getControl();

                        flowControl.setBarbOrientation(
                            FlowControl.NH_ORIENTATION);         // my default
                        flowControl.setFlowScale(0.15f);         // barb size
                    }
                }

                public void mapChanged(ScalarMapEvent event) {}  // ignore
            });
            addScalarMap(vMap);

            Unit altitudeUnit = meanWindAltitudeType.getDefaultUnit();
            ScalarMap zMap = new ScalarMap(meanWindAltitudeType,
                                           Display.YAxis);

            zMap.setRange(getMinDisplayAltitude().getValue(altitudeUnit),
                          getMaxDisplayAltitude().getValue(altitudeUnit));
            zMap.setScaleEnable(false);  // don't show mean-wind scale
            addScalarMap(zMap);
        } catch (RemoteException e) {}   // ignore because data is local

        // return set;
    }

    /**
     * Handles a change to the displayed altitude extent
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void displayAltitudeExtentChange()
            throws VisADException, RemoteException {
        setAltitudeColorMapRange();
        setCenterPoleExtent();
    }

    /**
     * Sets the extent of the center pole.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setCenterPoleExtent()
            throws RemoteException, VisADException {
        centerPole.setExtent(getMinDisplayAltitude(),
                             getMaxDisplayAltitude());
    }

    /**
     * Sets the cursor position.
     * @param position          The cursor position.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setCursorPosition(double[] position)
            throws VisADException, RemoteException {
        setGeopotentialAltitude(new Real(GeopotentialAltitude.getRealType(),
                                         position[0]));
    }

    /**
     * Display or hide the background
     *
     * @param  visable          Display background if true, otherwise hide the
     *                          background
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setBackgroundVisible(boolean visable)
            throws VisADException, RemoteException {
        centerPole.setVisible(visable);
    }

    /**
     * Sets the visibility of the center pole.
     * @param visible           Whether or not the center pole should be
     *                          visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setCenterPoleVisible(boolean visible)
            throws VisADException, RemoteException {
        centerPole.setVisible(visible);
    }

    /**
     * Returns a MeanWind Displayable corresponding to a mean-wind Tuple.
     * @param meanWind          the mean-wind Tuple
     * @return                  The MeanWind Displayable corresponding to the
     *                          input mean-wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Displayable newMeanWind(Tuple meanWind)
            throws VisADException, RemoteException {

        WindArrow meanWindArrow = new WindArrow(meanWindArrowTupleType);

        meanWindArrow.setWind(meanWind);
        meanWindArrow.setManipulable(false);
        meanWindArrow.addConstantMap(xOffset);
        meanWindArrow.setRGB(1, 1, 1);

        return meanWindArrow;
    }

    /**
     * Returns a MeanWind Displayable corresponding to a data reference for a
     * mean-wind Tuple.
     * @param meanWindRef       The data reference for the mean-wind Tuple.
     * @return                  The MeanWind Displayable corresponding to the
     *                          input mean-wind.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Displayable newMeanWind(final DataReference meanWindRef)
            throws VisADException, RemoteException {

        final WindArrow meanWindArrow = new WindArrow(meanWindArrowTupleType);

        new ActionImpl("WindStaffDisplayMeanWindBridge") {

            public void doAction() throws RemoteException, VisADException {
                meanWindArrow.setWind((Tuple) meanWindRef.getData());
            }
        }.addReference(meanWindRef);
        meanWindArrow.setManipulable(false);
        meanWindArrow.addConstantMap(xOffset);
        meanWindArrow.setRGB(1, 1, 1);

        return meanWindArrow;
    }

    /**
     * Returns the MeanWind Displayable with a missing mean-wind.
     * @return                  The MeanWind Displayable with a missing
     *                          mean-wind.
     */
    protected Displayable newMeanWind() {
        return missingMeanWindArrow;
    }

    /**
     * Tests this class.
     * @param args              Test arguments.  Ignored.
     * @throws Exception        Something went wrong.
     */
    public static void main(String[] args) throws Exception {

        JFrame           frame      = new JFrame("Wind Staff");
        WindStaffDisplay wsd        = new WindStaffDisplay();
        int              levelCount = 16;

        // Create an artificial flat field.
        float[][] levels = new float[1][levelCount];
        float[][] uv     = new float[2][levelCount];

        for (int i = 0; i < levelCount; i++) {
            levels[0][i] = i * 1000.f;
            uv[0][i]     = (i % 2 == 0)
                           ? i * 2.f
                           : -i * 2.f;
            uv[1][i]     = (i % 3 == 0)
                           ? -i * 2.f
                           : i * 2.f;
        }

        Field windProfile =
            new FlatField(
                CartesianHorizontalWindOfGeopotentialAltitude.instance(),
                new Gridded1DSet(
                    GeopotentialAltitude.getRealTupleType(), levels,
                    levelCount));

        windProfile.setSamples(uv);
        wsd.addProfile(0, windProfile);
        wsd.setMeanWind(0, new Tuple(new TupleType(new MathType[]{
            GeopotentialAltitude.getRealTupleType(),
            CartesianHorizontalWind
                .getRealTupleType() }), new Data[]{ new RealTuple(
                    GeopotentialAltitude
                        .getRealTupleType(), new Real[]{ new Real(
                            GeopotentialAltitude
                                .getRealType(), 8000) }, (CoordinateSystem) null),
                                                    new RealTuple(
                                                    CartesianHorizontalWind.getRealTupleType(),
                                                    new Real[]{
                                                        new Real(
                                                            WesterlyWind.getRealType(),
                                                                -10),
                                                        new Real(SoutherlyWind.getRealType(),
                                                        0) }, (CoordinateSystem) null) }));
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.getContentPane().add(wsd.getComponent());
        frame.setSize(256, 256);
        frame.pack();
        frame.setVisible(true);
        wsd.draw();
    }
}








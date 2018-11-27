/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import visad.CoordinateSystem;
import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.DisplayEvent;
import visad.DisplayListener;
import visad.FlatField;
import visad.FunctionType;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.Text;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import ucar.unidata.collab.Sharable;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.control.GridDisplayControl;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.ShapeUtility;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.PointProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.TextDisplayable;



/**
 * An abstract base class that manages a vertical probe
 * To create a probe call doMakeProbe
 * To be notified of changes override:
 * void probePositionChanged (double x, double y);
 *
 * @author IDV development team
 * @version $Revision$Date: 2011/03/24 16:06:32 $
 */
public class Grid2DReadoutProbe extends GridDisplayControl {

    /** profile sharing property */
    public static final String SHARE_PROFILE =
            "LineProbeControl.SHARE_PROFILE";

    /** the line probe */
    //-protected LineProbe probe;
    protected PointProbe probe;

    /** the initial position */
    private RealTuple initPosition;

    /** The shape for the probe point */
    private String marker;

    /** The point size */
    private float pointSize = 1.0f;

    /** Keep around for the label macros */
    protected String positionText;

    private static final TupleType TUPTYPE = makeTupleType();

    private DataReference positionRef = null;

    private Color currentColor = Color.MAGENTA;

    private RealTuple currentPosition = null;

    private Tuple locationValue = null;

    private TextDisplayable valueDisplay = null;

    private FlatField image = null;

    private RealTupleType earthTupleType = null;

    private boolean isLonLat = true;

    private DisplayMaster master;

    private DecimalFormat numFmt;

    /**
     * Default Constructor.
     */
    public Grid2DReadoutProbe(FlatField grid2d, DisplayMaster master)
            throws VisADException, RemoteException {
        super();
        earthTupleType = check2DEarthTuple(grid2d);
        if (earthTupleType != null) {
            isLonLat = earthTupleType.equals(RealTupleType.SpatialEarth2DTuple);
        }
        setAttributeFlags(FLAG_COLOR);
        initSharable();

        currentPosition = new RealTuple(RealTupleType.Generic2D);

        positionRef = new DataReferenceImpl(hashCode() + "_positionRef");

        valueDisplay = createValueDisplayer(currentColor);
        this.image = grid2d;
        this.master = master;

        master.addDisplayable(valueDisplay);
        setSharing(true);

        master.getDisplay().addDisplayListener( new DisplayListener() {
            public void displayChanged(DisplayEvent de) {
                if (de.getId() == DisplayEvent.MOUSE_RELEASED) {
                    try {
                        RealTuple position = getPosition();
                        doShare(SHARE_POSITION, position);
                    } catch (Exception e) {
                        logException("doMoveProfile", e);
                    }
                }
            }
        });
        numFmt = new DecimalFormat();
        numFmt.setMaximumFractionDigits(2);
    }

    /**
     * Default doMakeProbe method.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doMakeProbe() throws VisADException, RemoteException {
        doMakeProbe(getColor());
    }

    /**
     * Make the probe with the specific {@code Color}.
     *
     * @param c  color for probe.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doMakeProbe(Color c) throws VisADException, RemoteException {
        //doMakeProbe(c, getDefaultViewDescriptor());
    }


    /**
     * Make the probe with the specific {@code ViewDescriptor}.
     *
     * @param view  view descriptor
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doMakeProbe(ViewDescriptor view)
            throws VisADException, RemoteException {
        //doMakeProbe(getColor(), view);
    }

    /**
     * Make the probe with the specific {@link Color} and associate it with
     * the given {@link DisplayMaster}.
     *
     * @param probeColor Color of the probe.
     * @param master {@code DisplayMaster} of the display we will be probing.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    //-public void doMakeProbe(Color probeColor, ViewDescriptor view)
    public void doMakeProbe(Color probeColor, DisplayMaster master)
            throws VisADException, RemoteException {
        probe = null;
        /*
        if (getDisplayAltitudeType().equals(Display.Radius)) {
            //      System.err.println("Probe 1");
            probe = new LineProbe(
                new RealTuple(
                    RealTupleType.SpatialEarth2DTuple, new double[] { 0,
                    0 }));
        */
        if (initPosition != null) {
            //      System.err.println("Probe 2");
            //-probe = new LineProbe(initPosition);
            probe = new PointProbe(initPosition);
        } else {
            //      System.err.println("Probe 3");
            //-probe = new LineProbe(getInitialLinePosition());
            probe = new PointProbe(getInitialLinePosition());
            //            probe = new LineProbe(getGridCenterPosition());
        }
        initPosition = probe.getPosition();

        // it is a little colored cube 8 pixels across
        probe.setColor(probeColor);
        probe.setVisible(true);
        probe.addPropertyChangeListener(this);
        probe.setPointSize(1f);
        if (marker != null) {
            /*probe.setMarker(
                SelectorPoint.reduce(ShapeUtility.makeShape(marker))); */
        }
        probe.setAutoSize(true);
        master.addDisplayable(probe);
    }


    /**
     * Handle changes
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                SelectorDisplayable.PROPERTY_POSITION)) {
            doMoveProbe();
        } else {
            super.propertyChange(evt);
        }
    }

    /**
     * Reset the position of the probe to the center.
     */
    public void resetProbePosition() {
        try {
            setProbePosition(0.0, 0.0);
        } catch (Exception exc) {
            logException("Resetting probe position", exc);
        }
    }


    /**
     * Get edit menu items
     *
     * @param items      list of menu items
     * @param forMenuBar  true if for the menu bar
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        if (probe != null) {
            JMenuItem mi = new JMenuItem("Reset Probe Position");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    resetProbePosition();
                }
            });
            items.add(mi);
        }
        super.getEditMenuItems(items, forMenuBar);
    }

    /**
     * Set the probe position.  Probes are set in XY space.
     *
     * @param xy  X and Y position of the probe.
     *
     * @throws VisADException  problem setting probe position
     * @throws RemoteException  problem setting probe position on remote display
     */
    public void setProbePosition(RealTuple xy)
            throws VisADException, RemoteException {
        probe.setPosition(xy);
    }

    /**
     * Set the probe position from display x and y positions.
     *
     * @param x    X position of the probe.
     * @param y    Y position of the probe.
     *
     * @throws VisADException  problem setting probe position
     * @throws RemoteException  problem setting probe position on remote display
     */
    public void setProbePosition(double x, double y)
            throws VisADException, RemoteException {
        setProbePosition(new RealTuple(new Real[] {
                new Real(RealType.XAxis, x),
                new Real(RealType.YAxis, y) }));
    }

    /**
     * Set the initial position of the probe.  This is used by the
     * XML persistense.
     *
     * @param p  position
     */
    public void setPosition(RealTuple p) {
        initPosition = p;
    }

    /**
     * Get the position of the probe.  This is used by the
     * XML persistense.
     *
     * @return current probe position or null if probe has not been created.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public RealTuple getPosition() throws VisADException, RemoteException {
        return ((probe != null)
                ? probe.getPosition()
                : null);
    }

    /**
     * Get the initial position of the probe set during unpersistence.
     *
     * @return  initial position or {@code null} if not set during
     *          initialization.
     */
    public RealTuple getInitialPosition() {
        return initPosition;
    }

    /**
     * Method called when sharing is enabled.
     *
     * @param from  Sharable that send the data.
     * @param dataId  identifier for data to be shared
     * @param data   data to be shared.
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if (dataId.equals(SHARE_POSITION)) {
            if (probe == null) {
                return;
            }
            try {
                probe.setPosition((RealTuple) data[0]);
                probePositionChanged(getPosition());
            } catch (Exception e) {
                logException("receiveShareData:" + dataId, e);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }


    /**
     * Method called when probe is moved.
     */
    protected void doMoveProbe() {
        try {
            RealTuple position = getPosition();
            probePositionChanged(position);
            //-doShare(SHARE_POSITION, position);
        } catch (Exception e) {
            logException("doMoveProfile", e);
        }
    }

    /**
     * This gets called when either the user moves the probe point or
     * when we get a sharable event to move the probe point. Subclasses
     * need to implement this.
     *
     * @param newPos New position for the probe.
     */
    protected void probePositionChanged(final RealTuple newPos) {
        if (!currentPosition.equals(newPos)) {
            updatePosition(newPos);
            updateLocationValue();
            currentPosition = newPos;
        }
    }

    protected void updatePosition(final RealTuple position) {
        double[] vals = position.getValues();
        try {
            EarthLocationTuple elt = (EarthLocationTuple)boxToEarth(
                    new double[] { vals[0], vals[1], 1.0 });

            positionRef.setData(elt.getLatLonPoint());
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updatePosition", e);
        }
    }

    private void updateLocationValue() {
        Tuple tup = null;
        RealTuple earthTuple;


        try {
            RealTuple location = (RealTuple)positionRef.getData();

            if (location == null) {
                return;
            }

            if (image == null) {
                return;
            }

            double[] vals = location.getValues();
            if (vals[1] < -180) {
                vals[1] += 360f;
            }

            if (vals[1] > 180) {
                vals[1] -= 360f;
            }

            if (earthTupleType != null) {
                RealTuple lonLat =
                        new RealTuple(RealTupleType.SpatialEarth2DTuple,
                                new double[] { vals[1], vals[0] });
                RealTuple latLon = new RealTuple(RealTupleType.LatitudeLongitudeTuple,
                        new double[] { vals[0], vals[1] });
                RealTuple rtup = lonLat;
                if (!(isLonLat)) {
                    rtup = latLon;
                }

                Real val = null;
                Data dat = image.evaluate(rtup, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);

                if ( ((FunctionType)image.getType()).getRange() instanceof RealTupleType ) {
                    RealTuple tmp = (RealTuple)dat;
                    val = (tmp.getRealComponents())[0];
                } else {
                    val = (Real)dat;
                }
                float fval = (float)val.getValue();

                tup = new Tuple(TUPTYPE,
                        new Data[] { lonLat, new Text(TextType.Generic, numFmt.format(fval)) });
            }

            valueDisplay.setData(tup);
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.updateLocationValue", e);
        }

        if (tup != null) {
            locationValue = tup;
        }
    }

    public NavigatedDisplay  getNavigatedDisplay() {
        return (NavigatedDisplay)master;
    }

    public static RealTupleType check2DEarthTuple(FlatField field) {
        CoordinateSystem cs;
        FunctionType ftype = (FunctionType) field.getType();
        RealTupleType domain = ftype.getDomain();
        if ( (domain.equals(RealTupleType.SpatialEarth2DTuple)) ||
                (domain.equals(RealTupleType.LatitudeLongitudeTuple)) ) {
            return domain;
        } else if ((cs = domain.getCoordinateSystem()) != null) {
            RealTupleType ref = cs.getReference();
            if (ref.equals(RealTupleType.SpatialEarth2DTuple) ||
                    ref.equals(RealTupleType.LatitudeLongitudeTuple)) {
                return ref;
            }
        }
        return null;
    }

    private static TextDisplayable createValueDisplayer(final Color color)
            throws VisADException, RemoteException
    {
        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumIntegerDigits(3);
        fmt.setMaximumFractionDigits(1);
        TextDisplayable td = new TextDisplayable(TextType.Generic);
        td.setLineWidth(2f);
        td.setColor(color);
        td.setTextSize(1.75f);
        return td;
    }

    private static TupleType makeTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] {RealTupleType.SpatialEarth2DTuple,
                    TextType.Generic});
        } catch (Exception e) {
            LogUtil.logException("HydraImageProbe.makeTupleType", e);
        }
        return t;
    }

    /**
     * Respond to a change in the display's projection.  In this case
     * we fire the probePositionChanged() method with the probe's
     * position.
     */
    public void projectionChanged() {
        super.projectionChanged();
        try {
            probePositionChanged(getPosition());
        } catch (Exception exc) {
            logException("projectionChanged", exc);
        }
    }

    /**
     * Make a menu for controlling the probe size, shape and position.
     *
     * @param probeMenu The menu to add to
     *
     * @return The menu
     */
    public JMenu doMakeProbeMenu(JMenu probeMenu) {
        JMenu posMenu = new JMenu("Position");
        probeMenu.add(posMenu);
        posMenu.add(GuiUtils.makeMenuItem("Reset Probe Position", this,
                "resetProbePosition"));

        JMenu sizeMenu = new JMenu("Size");
        probeMenu.add(sizeMenu);

        sizeMenu.add(GuiUtils.makeMenuItem("Increase", this,
                "increaseProbeSize"));
        sizeMenu.add(GuiUtils.makeMenuItem("Decrease", this,
                "decreaseProbeSize"));

        JMenu shapeMenu = new JMenu("Probe Shape");
        probeMenu.add(shapeMenu);
        for (int i = 0; i < ShapeUtility.SHAPES.length; i++) {
            TwoFacedObject tof = ShapeUtility.SHAPES[i];
            String         lbl = tof.toString();
            if (Misc.equals(tof.getId(), marker)) {
                lbl = ">" + lbl;
            }
            JMenuItem mi = GuiUtils.makeMenuItem(lbl, this, "setMarker",
                    tof.getId());
            shapeMenu.add(mi);
        }
        GuiUtils.limitMenuSize(shapeMenu, "Shape Group ", 10);
        return probeMenu;
    }

    /**
     * Increase the probe size
     */
    public void increaseProbeSize() {
        if (probe == null) {
            return;
        }
        pointSize = probe.getPointScale();
        setPointSize(pointSize + pointSize * 0.5f);
    }


    /**
     * Decrease the probe size
     */
    public void decreaseProbeSize() {
        if (probe == null) {
            return;
        }
        pointSize = probe.getPointScale();
        pointSize = pointSize - pointSize * 0.5f;
        if (pointSize < 0.1f) {
            pointSize = 0.1f;
        }
        setPointSize(pointSize);
    }


    /**
     *  Set the PointSize property.
     *
     *  @param value The new value for PointSize
     */
    public void setPointSize(float value) {
        pointSize = value;
        if (probe != null) {
            try {
                probe.setAutoSize(false);
                probe.setPointSize(pointSize);
                probe.setAutoSize(true);
            } catch (Exception exc) {
                logException("Increasing probe size", exc);
            }
        }
    }

    /**
     *  Get the PointSize property.
     *
     *  @return The PointSize
     */
    public float getPointSize() {
        return pointSize;
    }


    /**
     * Get initial XY position from grid data.
     *
     * @return initial XY position of grid center point in VisAD space
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public RealTuple getGridCenterPosition()
            throws VisADException, RemoteException {
        RealTuple pos = new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                new double[] { 0,
                        0 });
        if (getGridDataInstance() != null) {
            LatLonPoint rt = GridUtil.getCenterLatLonPoint(
                    getGridDataInstance().getGrid());
            RealTuple xyz = earthToBoxTuple(new EarthLocationTuple(rt,
                    new Real(RealType.Altitude, 0)));
            if (xyz != null) {
                pos = new RealTuple(new Real[] { (Real) xyz.getComponent(0),
                        (Real) xyz.getComponent(1) });
            }
        }
        return pos;
    }


    /**
     * Get initial XY position from the screen
     *
     * @return initial XY position  in VisAD space
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public RealTuple getInitialLinePosition()
            throws VisADException, RemoteException {
        //-double[] center = getScreenCenter();
        double[] center = new double[] {0,0};
        return new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                new double[] { center[0],
                        center[1] });
    }

    /**
     * Set the Marker property.
     *
     * @param value The new value for Marker
     */
    public void setMarker(String value) {
        marker = value;
        if ((probe != null) && (marker != null)) {
            try {
                probe.setAutoSize(false);
                probe.setAutoSize(true);
            } catch (Exception exc) {
                logException("Setting marker", exc);
            }
        }
    }

    /**
     * Get the Marker property.
     *
     * @return The Marker
     */
    public String getMarker() {
        return marker;
    }

    /**
     * Add any macro name/label pairs
     *
     * @param names List of macro names
     * @param labels List of macro labels
     */
    protected void getMacroNames(List names, List labels) {
        super.getMacroNames(names, labels);
        names.addAll(Misc.newList(MACRO_POSITION));
        labels.addAll(Misc.newList("Probe Position"));
    }

    /**
     * Add any macro name/value pairs.
     *
     *
     * @param template template
     * @param patterns The macro names
     * @param values The macro values
     */
    protected void addLabelMacros(String template, List patterns,
                                  List values) {
        super.addLabelMacros(template, patterns, values);
        patterns.add(MACRO_POSITION);
        values.add(positionText);
    }

    /**
     * This method is called  to update the legend labels when
     * some state has changed in this control that is reflected in the labels.
     */
    protected void updateLegendLabel() {
        super.updateLegendLabel();
        // if the display label has the position, we'll update the list also
        String template = getDisplayListTemplate();
        if (template.contains(MACRO_POSITION)) {
            updateDisplayList();
        }
    }

    /**
     * Append any label information to the list of labels.
     *
     * @param labels   in/out list of labels
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        labels.add(positionText);
    }
}
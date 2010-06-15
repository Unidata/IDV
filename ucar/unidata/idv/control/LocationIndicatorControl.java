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


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.idv.*;

import ucar.unidata.idv.ViewManager;


import ucar.unidata.idv.control.drawing.*;
import ucar.unidata.ui.FineLineBorder;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.*;
import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import visad.java3d.*;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



/**
 * A MetApps Display Control for drawing lines on a navigated
 * display.
 *
 * @author MetApps development team
 * @version $Revision: 1.46 $
 */

public class LocationIndicatorControl extends DisplayControlImpl {


    /** _more_          */
    public static final int CLIP_NONE = 0;

    /** _more_          */
    public static final int CLIP_POSITIVE = 1;

    /** _more_          */
    public static final int CLIP_NEGATIVE = 2;

    /** Indices into arrays */
    public static final int IDX_X = 0;

    /** Indices into arrays */
    public static final int IDX_Y = 1;

    /** Indices into arrays */
    public static final int IDX_Z = 2;

    /** _more_          */
    public static String[] CLIP_NAMES1 = { "Above", "Left", "Bottom" };

    /** _more_          */
    public static String[] CLIP_NAMES2 = { "Below", "Right", "Top" };


    /**
     * The width of the box we use. A little less than visad's 1.0 so we don't have problems
     * with clipping and the visad box display.
     */
    private static float BOX = 0.99f;

    /** The last display scale we've seen */
    private double lastDisplayScale = 1.0;



    /** Are we currently dragging the locations */
    private boolean dragging = false;

    /** Origin point */
    private float[] originLoc = { 0.0f, 0.0f, 0.0f };

    /** Loc */
    private float[] pointLoc = { 0.0f, 0.0f, 0.0f };

    /** Holds min bbox */
    private float[] minArray;

    /** Holds max bbox */
    private float[] maxArray;



    /** Should the point be shown */
    private boolean showPoint = false;

    /** Show point checkbox */
    private JCheckBox showPointCbx;


    /** Used to create visad types */
    private static int typeCnt = 0;


    /** Width of axis */
    private float span = BOX;

    /** x axis info */
    private AxisInfo xInfo;

    /** y axis info */
    private AxisInfo yInfo;

    /** z axis info */
    private AxisInfo zInfo;


    /** Show text */
    private boolean visibleTextC = true;

    /** Keep the origin in the box */
    private boolean keepInBox = false;

    /** Text displayable */
    private CompositeDisplayable textHolder;

    /** lat/lon/alt readout */
    private LocationReadout originReadout;

    /** lat/lon/alt readout */
    private LocationReadout pointReadout;

    /** lat/lon/alt readout */
    private LocationReadout xReadout;

    /** lat/lon/alt readout */
    private LocationReadout yReadout;

    /** lat/lon/alt readout */
    private LocationReadout zReadout;


    /** Shows the location point */
    private ShapeDisplayable pointDisplayable;


    /** The line from the origin to the point */
    LineDrawing pointLine;

    /** Text at center */
    private ShapeDisplayable cText;


    /** Is this control enabled */
    private boolean enabled = true;

    /** Azimuth label */
    private JLabel bearingLbl = new JLabel("Bearing: ");

    /** Azimuth label */
    private JLabel azimuthLbl = new JLabel(" ");


    /** The current line width */
    private int lineWidth = 1;

    /** The current font */
    private Font font;

    /** The default font font */
    private static final Font defaultFont = new Font("Monospaced",
                                                Font.PLAIN, 12);

    /** Holds the list of fonts */
    private JComboBox fontBox;

    /** Holds the font sizes */
    private JComboBox fontSizeBox;

    /** Top level displayable */
    CompositeDisplayable displayHolder;

    /** Top level displayable */
    CompositeDisplayable solidHolder;

    /** Color for solid axis planes */
    Color solidColor;

    /** Alpha value for solid color */
    double solidAlphaPercent = 1.0;


    /** For the text displayable */
    TextType textType;


    /** The label to show the readout in the side legend */
    private JLabel sideLegendReadout;


    /**
     * Create a new Drawing Control; set attributes.
     */
    public LocationIndicatorControl() {}


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     * @return true if everything is okay
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        displayHolder = new CompositeDisplayable();
        solidHolder   = new CompositeDisplayable();
        textHolder    = new CompositeDisplayable();



        getXInfo().initDisplayables();
        getYInfo().initDisplayables();
        getZInfo().initDisplayables();
        addDisplayable(solidHolder);
        addDisplayable(displayHolder, FLAG_COLOR);
        displayHolder.addDisplayable(textHolder);


        Color c = getColor();
        if (c == null) {
            setColor(c = Color.red);
        }
        displayHolder.setColor(c);

        if (solidColor == null) {
            solidColor = Color.gray;
        }
        setSolidColor(solidColor);

        pointDisplayable = new ShapeDisplayable("Point" + (typeCnt++),
                getTickMark());
        pointDisplayable.setAutoSize(true);
        pointLine = new LineDrawing("LocationIndicatorControl.xline");
        displayHolder.addDisplayable(pointDisplayable);
        displayHolder.addDisplayable(pointLine);



        cText = new ShapeDisplayable("Text" + (typeCnt++), "");

        if (font == null) {
            font = defaultFont;
        }
        displayHolder.setLineWidth(getLineWidth());

        originReadout = new LocationReadout(this);
        pointReadout  = new LocationReadout(this);
        xReadout      = new LocationReadout(this);
        yReadout      = new LocationReadout(this);
        zReadout      = new LocationReadout(this);

        checkVisibility();
        updatePosition();


        return true;
    }


    /**
     * Update the position from the text fields
     *
     * @param readout The readout
     */
    public void updatePositionFromReadout(LocationReadout readout) {
        try {
            boolean ensureShowPoint = true;
            float[] readoutLoc      = readout.getXYZ();
            if (readout == originReadout) {
                ensureShowPoint = false;
                originLoc       = readoutLoc;
            } else if (readout == pointReadout) {
                pointLoc = readoutLoc;
            } else if (readout == xReadout) {
                pointLoc[IDX_X]  = readoutLoc[IDX_X];
                originLoc[IDX_Y] = readoutLoc[IDX_Y];
                originLoc[IDX_Z] = readoutLoc[IDX_Z];
            } else if (readout == yReadout) {
                pointLoc[IDX_Y]  = readoutLoc[IDX_Y];
                originLoc[IDX_X] = readoutLoc[IDX_X];
                originLoc[IDX_Z] = readoutLoc[IDX_Z];

            } else if (readout == zReadout) {
                pointLoc[IDX_Z]  = readoutLoc[IDX_Z];
                originLoc[IDX_Y] = readoutLoc[IDX_Y];
                originLoc[IDX_X] = readoutLoc[IDX_X];
            }
            updatePosition();
            if (ensureShowPoint && !showPoint) {
                showPointCbx.setSelected(true);
                showPoint = true;
                checkVisibility();
            }
        } catch (Exception exc) {
            logException("Updating position", exc);
        }
    }





    /**
     * Create the tick mark
     *
     * @return The tick mark
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    VisADGeometryArray getTickMark() throws VisADException, RemoteException {
        VisADGeometryArray marker =
            ShapeUtility.setSize(ShapeUtility.makeShape(ShapeUtility.CROSS),
                                 0.04f * (float) getDisplayScale());

        double[] aspect = getNavigatedDisplay().getDisplayAspect();
        if ((aspect != null) && (aspect.length > 2) && (aspect[0] > 0)
                && (aspect[1] > 0) && (aspect[2] > 0)) {
            marker = ShapeUtility.setSize(marker, (float) (1.0 / aspect[0]),
                                          (float) (1.0 / aspect[1]),
                                          (float) (1.0 / aspect[2]));

        }
        return marker;
    }


    /**
     * Set the location of the line
     *
     * @param ld The displayable
     * @param x1 location
     * @param x2 location
     * @param y1 location
     * @param y2 location
     * @param z1 location
     * @param z2 location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static void setPts(LineDrawing ld, float x1, float x2, float y1,
                              float y2, float z1, float z2)
            throws VisADException, RemoteException {
        MathType  mathType = RealTupleType.SpatialCartesian3DTuple;
        float[][] pts      = new float[][] {
            { x1, x2 }, { y1, y2 }, { z1, z2 }
        };
        ld.setData(new Gridded3DSet(mathType, pts, 2));
    }



    /**
     * Get the min xyz
     *
     * @return min xyz
     */
    float[] getMin() {
        if (minArray == null) {
            minArray = new float[3];
        }
        minArray[IDX_X] = (float) Math.min(-span, originLoc[IDX_X]);
        minArray[IDX_Y] = (float) Math.min(-span, originLoc[IDX_Y]);
        minArray[IDX_Z] = (float) Math.min(-span, originLoc[IDX_Z]);
        return minArray;
    }


    /**
     * Get the max xyz
     *
     * @return max xyz
     */
    float[] getMax() {
        if (maxArray == null) {
            maxArray = new float[3];
        }
        maxArray[IDX_X] = (float) Math.max(span, originLoc[IDX_X]);
        maxArray[IDX_Y] = (float) Math.max(span, originLoc[IDX_Y]);
        maxArray[IDX_Z] = (float) Math.max(span, originLoc[IDX_Z]);
        return maxArray;
    }


    /**
     * _more_
     *
     * @param onlyIfOn _more_
     */
    private void checkClip(boolean onlyIfOn) {
        try {
            double           oX         = originLoc[IDX_X];
            double           oY         = originLoc[IDX_Y];
            double           oZ         = originLoc[IDX_Z];
            NavigatedDisplay navDisplay = getNavigatedDisplay();
            DisplayRendererJ3D dr =
                (DisplayRendererJ3D) navDisplay.getDisplay()
                    .getDisplayRenderer();

            AxisInfo axis;
            int      coeff;
            double   value;

            axis = getXInfo();
            if ( !onlyIfOn || (axis.getClip() != CLIP_NONE)) {
                value = oY;
                coeff = axis.getClipCoefficient();
                dr.setClip(axis.getIndex(), axis.getClip() != CLIP_NONE, 0.f,
                           (float) coeff, 0.f,
                           (float) ((-coeff * (value + coeff * 0.01f))));
            }



            axis = getYInfo();
            if ( !onlyIfOn || (axis.getClip() != CLIP_NONE)) {
                value = oX;
                coeff = -axis.getClipCoefficient();
                dr.setClip(axis.getIndex(), axis.getClip() != CLIP_NONE,
                           (float) coeff, 0.f, 0.f,
                           (float) ((-coeff * (value + coeff * 0.01f))));
            }


            axis = getZInfo();
            if ( !onlyIfOn || (axis.getClip() != CLIP_NONE)) {
                value = oZ;
                coeff = -axis.getClipCoefficient();
                dr.setClip(axis.getIndex(), axis.getClip() != CLIP_NONE, 0.f,
                           0.f, (float) coeff,
                           (float) ((-coeff * (value + coeff * 0.01f))));
            }





        } catch (Exception exc) {
            logException("Updating position", exc);
        }


    }


    /**
     * Move the axises
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void updatePosition() throws VisADException, RemoteException {

        if (displayHolder == null) {
            return;
        }

        float[]       min      = getMin();
        float[]       max      = getMax();

        float         ptX      = (float) pointLoc[IDX_X];
        float         ptY      = (float) pointLoc[IDX_Y];
        float         ptZ      = (float) pointLoc[IDX_Z];

        double        oX       = originLoc[IDX_X];
        double        oY       = originLoc[IDX_Y];
        double        oZ       = originLoc[IDX_Z];

        EarthLocation originEl = boxToEarth(oX, oY, oZ, false);
        EarthLocation ptEl     = boxToEarth(ptX, ptY, ptZ, false);

        getXInfo().updateAxisPosition(originEl, ptEl);
        getYInfo().updateAxisPosition(originEl, ptEl);
        getZInfo().updateAxisPosition(originEl, ptEl);


        checkClip(true);

        Bearing result =
            Bearing.calculateBearing(
                originEl.getLatLonPoint().getLatitude().getValue(),
                originEl.getLatLonPoint().getLongitude().getValue(),
                ptEl.getLatLonPoint().getLatitude().getValue(),
                ptEl.getLatLonPoint().getLongitude().getValue(), null);

        if (azimuthLbl != null) {
            String bearingText = "";
            if (showPoint) {
                bearingText = "" + getDisplayConventions().formatAngle(
                    result.getAngle());
            }
            azimuthLbl.setText(StringUtil.padRight(bearingText, 15));
        }



        setPts(pointLine, (float) ptX, (float) oX, (float) ptY, (float) oY,
               (float) ptZ, (float) oZ);

        pointDisplayable.setPoint(ptX, ptY, ptZ);


        StringBuffer legendSB = new StringBuffer();
        if (originReadout != null) {
            originReadout.setLocation(originEl);
            pointReadout.setLocation(ptEl);
            legendSB.append("Origin: " + originReadout.toString());
            if (showPoint) {
                legendSB.append("<br>Point: " + pointReadout.toString());
            }
            xReadout.setLocation(boxToEarth(ptX, originLoc[IDX_Y],
                                            originLoc[IDX_Z], false));
            yReadout.setLocation(boxToEarth(originLoc[IDX_X], ptY,
                                            originLoc[IDX_Z], false));
            zReadout.setLocation(boxToEarth(originLoc[IDX_X],
                                            originLoc[IDX_Y], ptZ, false));
        }



        if (sideLegendReadout == null) {
            sideLegendReadout = new JLabel("<html><br></html>");
        }
        sideLegendReadout.setText("<html>" + legendSB.toString() + "</html>");



        if ( !dragging) {
            /*
              cText.setMarker(getMarker(cx,cy,cz),cx,cy,cz);
            xText.setMarker(getMarker(minX,cy,cz),minX,cy,cz);
            */
        }

    }


    /**
     * getTuple
     *
     * @param x x
     * @param y y
     * @param z z
     * @param textType type
     *
     * @return Tuple
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private Tuple getTuple(double x, double y, double z, ScalarType textType)
            throws VisADException, RemoteException {
        return new Tuple(new Data[] {
            new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                          new double[] { x,
                                         y, z }), getLabel(x, y, z,
                                         textType) });
    }


    /**
     * get label
     *
     * @param x x
     * @param y y
     * @param z z
     * @param textType textType
     *
     * @return label
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private Text getLabel(double x, double y, double z, ScalarType textType)
            throws VisADException, RemoteException {
        EarthLocation el = boxToEarth(x, y, z);
        String label =
            getControlContext().getDisplayConventions()
                .formatEarthLocationShort(el);
        return new Text((TextType) textType, label);

    }

    /**
     * Get the lat/lon/alt string
     *
     * @param x x
     * @param y y
     * @param z z
     *
     * @return lat/lon/alt string
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private String getStr(double x, double y, double z)
            throws VisADException, RemoteException {
        EarthLocation el = boxToEarth(x, y, z);
        return getControlContext().getDisplayConventions()
            .formatEarthLocationShort(el);
    }



    /**
     *  Turn on the visibility of this display
     *
     * @param on Visible?
     */
    public void setDisplayVisibility(boolean on) {
        super.setDisplayVisibility(on);
        checkVisibility();
    }


    /**
     * Have the axisInfos check their visibilty
     */
    protected void checkVisibility() {
        try {
            if (pointDisplayable == null) {
                return;
            }
            getXInfo().checkVisibility();
            getYInfo().checkVisibility();
            getZInfo().checkVisibility();
            pointDisplayable.setVisible(getDisplayVisibility() && showPoint);
            pointLine.setVisible(getDisplayVisibility() && showPoint);
        } catch (Exception exc) {
            logException("Checking visibility", exc);
        }
    }





    /**
     * Remove this DisplayControl from the system.  Nulls out any
     * objects for garbagfe collection
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void doRemove() throws VisADException, RemoteException {
        clearCursor();
        displayHolder = null;
        super.doRemove();
    }


    /**
     * Signal base class to add this as a display listener
     *
     * @return Add as display listener
     */
    protected boolean shouldAddDisplayListener() {
        return true;
    }



    /**
     * Signal base class to add this as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }



    /**
     * Overwrite the legend labels
     *
     * @param labels List of labels
     * @param legendType Side or bottom
     */
    protected void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
    }



    /**
     * Respond to control changed events from the view manager
     */
    public void viewpointChanged() {
        super.viewpointChanged();
        try {
            double newDisplayScale = getDisplayScale();
            if (Math.abs((lastDisplayScale - newDisplayScale)
                         / lastDisplayScale) > 0.1) {
                lastDisplayScale = newDisplayScale;
                getXInfo().setLabelPosition();
                getYInfo().setLabelPosition();
                getZInfo().setLabelPosition();
            }
            //      updatePosition();
        } catch (Exception e) {
            logException("Handling control changed", e);
        }
    }

    /**
     * Respond to the projection changing event
     */
    public void projectionChanged() {
        try {
            super.projectionChanged();
            updatePosition();
        } catch (Exception e) {
            logException("Handling projection changed", e);
        }
    }



    /**
     * Assume that any display controls that have a color table widget
     * will want the color table to show up in the legend.
     *
     * @param  legendType  type of legend
     * @return The extra JComponent to use in legend
     */
    protected JComponent getExtraLegendComponent(int legendType) {
        JComponent parentComp = super.getExtraLegendComponent(legendType);
        if (legendType == BOTTOM_LEGEND) {
            return parentComp;
        }
        if (sideLegendReadout == null) {
            sideLegendReadout = new JLabel("<html><br></html>");
        }
        return GuiUtils.vbox(parentComp, sideLegendReadout);
    }





    /**
     * Should we handle display events
     *
     * @return Ok to handle events
     */
    protected boolean canHandleEvents() {
        if ( !getEnabled() || !getHaveInitialized()) {
            return false;
        }
        return isGuiShown();
    }



    /**
     * Listen for DisplayEvents
     *
     * @param event The event
     */
    public void handleDisplayChanged(DisplayEvent event) {

        InputEvent inputEvent = event.getInputEvent();

        int        id         = event.getId();
        if (id == DisplayEvent.MOUSE_MOVED) {
            return;
        }
        if ( !canHandleEvents()) {
            return;
        }

        try {

            //            System.err.println ("event:" + displayEventName(id));
            if (id == DisplayEvent.KEY_PRESSED) {
                if ((inputEvent instanceof KeyEvent)) {
                    KeyEvent keyEvent = (KeyEvent) inputEvent;
                    //              System.err.println ("key:" + keyEvent);
                    if (keyEvent.isControlDown() && keyEvent.isAltDown()) {
                        int code = keyEvent.getKeyCode();
                        if (code == KeyEvent.VK_O) {
                            originLoc = new float[] { 0.0f, 0.0f, 0.0f };
                            pointLoc  = new float[] { 0.0f, 0.0f, 0.0f };
                        } else if (code == KeyEvent.VK_L) {
                            originLoc[IDX_X] = -span;
                            pointLoc[IDX_X]  = -span;
                            //                            originLoc[IDX_X] = -0.5f;
                        } else if (code == KeyEvent.VK_R) {
                            originLoc[IDX_X] = span;
                            pointLoc[IDX_X]  = span;
                        } else if (code == KeyEvent.VK_T) {
                            originLoc[IDX_Y] = span;
                            pointLoc[IDX_Y]  = span;
                        } else if (code == KeyEvent.VK_B) {
                            originLoc[IDX_Y] = -span;
                            pointLoc[IDX_Y]  = -span;
                        } else if (code == KeyEvent.VK_U) {
                            originLoc[IDX_Z] = span;
                            pointLoc[IDX_Z]  = span;
                        } else if (code == KeyEvent.VK_D) {
                            originLoc[IDX_Z] = -span;
                            pointLoc[IDX_Z]  = -span;
                        } else {
                            return;
                        }
                        updatePosition();
                    }
                }
                return;
            }



            if (id == DisplayEvent.MOUSE_DRAGGED) {
                if ( !isLeftButtonDown(event) || inputEvent.isShiftDown()) {
                    return;
                }
                //                textHolder.setVisible(false);
                dragging = true;
                double[] location = toBox(event);
                boolean doPoint = inputEvent.isControlDown()||inputEvent.isAltDown();
                float[]  ptArray  = (doPoint
                                     ? pointLoc
                                     : originLoc);
                ptArray[0] = (float) (getXInfo().move
                                      ? location[0]
                                      : ptArray[0]);
                ptArray[1] = (float) (getYInfo().move
                                      ? location[1]
                                      : ptArray[1]);
                ptArray[2] = (float) (getZInfo().move
                                      ? location[2]
                                      : ptArray[2]);
                if (keepInBox) {
                    ptArray[0] = (float) Math.min(Math.max(-BOX, ptArray[0]),
                            BOX);
                    ptArray[1] = (float) Math.min(Math.max(-BOX, ptArray[1]),
                            BOX);
                    ptArray[2] = (float) Math.min(Math.max(-BOX, ptArray[2]),
                            BOX);
                }
                float[] min = getMin();
                float[] max = getMax();
                pointLoc[IDX_X] = (float) Math.min(max[IDX_X],
                        Math.max(pointLoc[IDX_X], min[IDX_X]));
                pointLoc[IDX_Y] = (float) Math.min(max[IDX_Y],
                        Math.max(pointLoc[IDX_Y], min[IDX_Y]));
                pointLoc[IDX_Z] = (float) Math.min(max[IDX_Z],
                        Math.max(pointLoc[IDX_Z], min[IDX_Z]));

                if (doPoint && !showPoint) {
                    showPointCbx.setSelected(true);
                    showPoint = true;
                    checkVisibility();
                }
                updatePosition();
            } else if (id == DisplayEvent.MOUSE_RELEASED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }
                dragging = false;
                updatePosition();
                //textHolder.setVisible(true);
                //              checkVisibility();
            }
        } catch (Exception e) {
            logException("Handling display event changed", e);
        }
    }


    /**
     * Method called by other classes that share the the state.
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        super.receiveShareData(from, dataId, data);
    }



    /**
     * The font has changed
     *
     * @param value The value
     */
    public void fontChanged(Object value) {
        setFont(getFont());
    }

    /**
     *  The color has changed
     *
     * @param colorName The new color name
     */
    public void colorChanged(String colorName) {
        if (colorName != null) {
            setSolidColor(getDisplayConventions().getColor(colorName));
        }
    }

    /**
     *  The line width has changed
     *
     * @param v new width
     */
    public void lineWidthChanged(Integer v) {
        setLineWidth(v.intValue());
    }

    /**
     * The span value has changed
     *
     * @param value New span value_
     */
    public void spanSliderChanged(int value) {
        try {
            double percent = value / 100.0;
            setSpan((float) (percent * BOX * 5.0f));
            updatePosition();
        } catch (Exception exc) {
            logException("Setting span position", exc);
        }
    }





    /**
     * Handle the alpha value changing
     *
     * @param selected The selected item
     */
    public void transparencyChanged(Object selected) {
        int value = 0;
        if (selected instanceof TwoFacedObject) {
            TwoFacedObject tfo = (TwoFacedObject) selected;
            value = ((Integer) tfo.getId()).intValue();
        } else {
            String text = selected.toString();
            try {
                text  = StringUtil.replace(text, "%", "").trim();
                value = (int) new Float(text).floatValue();
            } catch (Exception e) {
                logException("Setting transparency", e);
            }
        }
        //Inverse because a vlaue of 0 really means 100%
        value = 100 - value;
        //Normalize
        if (value < 0) {
            value = 0;
        }
        if (value > 100) {
            value = 100;
        }
        setSolidAlphaPercent(value / 100.0);
        setSolidColor(solidColor);

    }

    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {



        List           transparencyItems = new ArrayList();
        int[]          values            = {
            0, 20, 40, 50, 60, 80, 100
        };
        int            transInt = 100 - (int) (solidAlphaPercent * 100.0);
        TwoFacedObject selectedTfo       = null;
        for (int i = 0; i < values.length; i++) {

            TwoFacedObject tfo = new TwoFacedObject(values[i] + "%",
                                     new Integer(values[i]));
            transparencyItems.add(tfo);
            if (values[i] == transInt) {
                selectedTfo = tfo;
            }
        }
        if (selectedTfo == null) {
            selectedTfo = new TwoFacedObject(transInt + "%",
                                             new Integer(transInt));
            transparencyItems.add(selectedTfo);

        }


        JComboBox solidColorBox =
            GuiUtils.makeComboBox(
                getDisplayConventions().getColorNameList(),
                getDisplayConventions().getColorName(solidColor), false,
                this, "colorChanged");
        JSlider spanSlider = GuiUtils.makeSlider(0, 100,
                                 (int) ((span / 5.0) * 100), this,
                                 "spanSliderChanged");


        Component colorCbx   = doMakeColorControl(getColor());

        Vector    lineWidths = new Vector();
        for (int i = 1; i <= 10; i++) {
            lineWidths.add(new Integer(i));
        }
        JComponent widthComp = GuiUtils.makeComboBox(lineWidths,
                                   new Integer(lineWidth), false, this,
                                   "lineWidthChanged");


        fontBox = GuiUtils.makeComboBox(
            GuiUtils.getFontList(),
            new TwoFacedObject(StringUtil.shorten(font.getName(), 24), font),
            false, this, "fontChanged");
        fontSizeBox = GuiUtils.makeComboBox(GuiUtils.getFontSizeList(),
                                            new Integer(font.getSize()),
                                            false, this, "fontChanged");


        azimuthLbl.setFont(Font.decode("monospaced"));
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel readoutPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.filler(), GuiUtils.cLabel("Latitude"),
            GuiUtils.cLabel("Longitude"), GuiUtils.cLabel("Altitude"),
            GuiUtils.rLabel(""), GuiUtils.rLabel("Origin:"),
            originReadout.latLbl, originReadout.lonLbl, originReadout.altComp,
            GuiUtils.filler(), GuiUtils.rLabel("Point:"), pointReadout.latLbl,
            pointReadout.lonLbl, pointReadout.altComp,
            GuiUtils.hbox(bearingLbl, azimuthLbl), GuiUtils.rLabel("X Tick:"),
            xReadout.latLbl, xReadout.lonLbl, xReadout.altComp,
            GuiUtils.filler(), GuiUtils.rLabel("Y Tick:"), yReadout.latLbl,
            yReadout.lonLbl, yReadout.altComp, GuiUtils.filler(),
            GuiUtils.rLabel("Z Tick:"), zReadout.latLbl, zReadout.lonLbl,
            zReadout.altComp, GuiUtils.filler()
        }, 5, GuiUtils.WT_N, GuiUtils.WT_N);


        JLabel helpLabel =
            new JLabel(
                "<html><br>Drag: move origin; Control-drag: move point<br>Control-Alt-'key': move to<br>&nbsp;&nbsp;'o': origin; 'l': left; 'r': right; 't': top; 'b': bottom; 'u': up; 'd': down</html>");
        readoutPanel = GuiUtils.vbox(readoutPanel, helpLabel);
        readoutPanel = GuiUtils.topLeft(readoutPanel);

        JPanel fontPanel = GuiUtils.left(GuiUtils.flow(new Component[] {
                               fontBox,
                               new JLabel("Size: "), fontSizeBox }));

        JPanel solidPanel =
            GuiUtils.hbox(new JLabel("  Color: "), solidColorBox,
                          new JLabel("  Transparency: "),
                          GuiUtils.makeComboBox(transparencyItems,
                              selectedTfo, true, this,
                              "transparencyChanged"));
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel topPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.filler(), GuiUtils.cLabel("X"), GuiUtils.cLabel("Y"),
            GuiUtils.cLabel("Z"), GuiUtils.filler(), GuiUtils.rLabel("Move:"),
            GuiUtils.makeCheckbox("", getXInfo(), "move"),
            GuiUtils.makeCheckbox("", getYInfo(), "move"),
            GuiUtils.makeCheckbox("", getZInfo(), "move"),
            GuiUtils.hbox(GuiUtils.makeCheckbox("Keep in Box", this,
                "keepInBox"), GuiUtils.makeCheckbox("Enabled", this,
                    "enabled")),
            GuiUtils.rLabel("Clip:"), getXInfo().makeClipBox(),
            getYInfo().makeClipBox(), getZInfo().makeClipBox(),
            GuiUtils.filler(), GuiUtils.rLabel("Visibility:"),
            GuiUtils.makeCheckbox("", getXInfo(), "visible"),
            GuiUtils.makeCheckbox("", getYInfo(), "visible"),
            GuiUtils.makeCheckbox("", getZInfo(), "visible"),
            GuiUtils.filler(), GuiUtils.rLabel("Labels:"),
            GuiUtils.makeCheckbox("", getXInfo(), "labelVisible"),
            GuiUtils.makeCheckbox("", getYInfo(), "labelVisible"),
            GuiUtils.makeCheckbox("", getZInfo(), "labelVisible"),
            GuiUtils.filler(), GuiUtils.rLabel("Solid:"),
            GuiUtils.makeCheckbox("", getXInfo(), "solid"),
            GuiUtils.makeCheckbox("", getYInfo(), "solid"),
            GuiUtils.makeCheckbox("", getZInfo(), "solid"),
            GuiUtils.left(solidPanel), GuiUtils.rLabel("Bearing Lines:"),
            GuiUtils.makeCheckbox("", getXInfo(), "showLines"),
            GuiUtils.makeCheckbox("", getYInfo(), "showLines"),
            GuiUtils.makeCheckbox("", getZInfo(), "showLines"),
            GuiUtils.filler(),
        }, 5, GuiUtils.WT_N, GuiUtils.WT_N);


        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel bottomPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Bearing Point:"),
            GuiUtils.left(showPointCbx = GuiUtils.makeCheckbox("Show point",
                this, "showPoint")),
            GuiUtils.rLabel("Axis size:"), spanSlider,
            GuiUtils.rLabel("Line Width:"),
            GuiUtils.left(GuiUtils.hbox(widthComp,
                                        GuiUtils.lLabel("  Color: "),
                                        GuiUtils.left(colorCbx)))
            //,GuiUtils.rLabel("Font:"), fontPanel,
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);




        JTabbedPane tabbedPane  = new JTabbedPane();
        JPanel      orientPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.makeButton("Center At Origin", this, "centerAtOrigin"),
            GuiUtils.makeButton("Center At Point", this, "centerAtPoint")
            /*,                               GuiUtils.makeButton("Rotate  About X Axis",
                this, "rotateAbout", new Integer(IDX_X)),
            GuiUtils.makeButton("Rotate  About Y Axis",
                this, "rotateAbout", new Integer(IDX_Y)),
            GuiUtils.makeButton(
                "Rotate  About Z Axis", this, "rotateAbout", new Integer(
                IDX_Z))***/
        }, 1, GuiUtils.WT_N, GuiUtils.WT_N);

        orientPanel = GuiUtils.topLeft(orientPanel);
        JPanel displayPanel =
            GuiUtils.topLeft(GuiUtils.vbox(GuiUtils.left(topPanel),
                                           bottomPanel));
        tabbedPane.add("Location", readoutPanel);
        tabbedPane.add("Settings", displayPanel);
        tabbedPane.add("Orient", orientPanel);
        return tabbedPane;

    }

    /**
     * Rotate about the index
     *
     * @param i The index
     */
    public void rotateAbout(Integer i) {
        int index = i.intValue();
        System.err.println("index=" + index);
    }


    /**
     * Center at origin
     */
    public void centerAtOrigin() {
        try {
            //            System.err.println ("center: " + originLoc[IDX_X] +"x"+originLoc[IDX_Y]);
            getNavigatedDisplay().center(originLoc[IDX_X], originLoc[IDX_Y]);
        } catch (Exception exc) {
            logException("Center", exc);
        }
    }

    /**
     * Center at bearing point
     */
    public void centerAtPoint() {
        try {
            getNavigatedDisplay().center(pointLoc[IDX_X], pointLoc[IDX_Y]);
        } catch (Exception exc) {
            logException("Center", exc);
        }
    }





    /**
     * Get the solid plane color
     *
     * @return Solid color
     */
    public Color getSolidColor() {
        return solidColor;
    }

    /**
     * Set the solid plane color
     *
     * @param c color
     */
    public void setSolidColor(Color c) {
        solidColor = c;
        if (solidHolder != null) {
            try {
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                              (int) (solidAlphaPercent * 255));
                solidHolder.setColor(c);
            } catch (Exception e) {
                logException("Setting solid color", e);
            }
        }
    }


    /**
     * Set the width of the lines.
     * @param width width of lines.
     */
    public void setLineWidth(int width) {
        lineWidth = width;
        if (displayHolder != null) {
            try {
                displayHolder.setLineWidth(getLineWidth());
            } catch (Exception e) {
                logException("Setting line width of scale", e);
            }
        }
    }

    /**
     * Get the line width
     *
     * @return The line width
     */
    public int getLineWidth() {
        return lineWidth;
    }




    /**
     * Set the Enabled property.
     *
     * @param value The new value for Enabled
     */
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Get the Enabled property.
     *
     * @return The Enabled
     */
    public boolean getEnabled() {
        return enabled;
    }



    /**
     * Clear the cursor in the main display
     */
    private void clearCursor() {
        setCursor(null);
    }

    /**
     * Set the cursor in the main display
     *
     * @param c  The cursor id
     */
    private void setCursor(int c) {
        setCursor(Cursor.getPredefinedCursor(c));
    }

    /**
     * Set the cursor in the main display
     *
     * @param c The cursor
     */
    private void setCursor(Cursor c) {
        getViewManager().setCursorInDisplay(c);
    }


    /**
     * Get the font from the ui widget
     *
     * @return The font to use for new text glyphs
     */
    public Font getFont() {
        if (fontBox == null) {
            return font;
        }
        font = (Font) ((TwoFacedObject) fontBox.getSelectedItem()).getId();
        if (font == null) {
            font = defaultFont;
        }
        int fontSize = ((Integer) fontSizeBox.getSelectedItem()).intValue();
        return font.deriveFont((float) fontSize);
    }


    /**
     * Add to view menu
     *
     * @param items List of ites
     * @param forMenuBar for the menu bar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.makeMenuItem("Set origin to address", this,
                                        "goToAddress", "origin"));
        items.add(GuiUtils.makeMenuItem("Set point to address", this,
                                        "goToAddress", "point"));

        items.add(GuiUtils.MENU_SEPARATOR);
        super.getViewMenuItems(items, forMenuBar);
    }



    /**
     * Go the a street address
     *
     * @param which Which one, origin or point
     */
    public void goToAddress(final String which) {
        //Do it in a thread
        Misc.run(new Runnable() {
            public void run() {
                goToAddressInner(which);
            }
        });
    }


    /**
     * Go the a street address
     *
     * @param which Point or origin
     */
    public void goToAddressInner(String which) {
        try {
            showWaitCursor();
            LatLonPoint llp = GeoUtils.getLocationOfAddress();
            showNormalCursor();
            if (llp == null) {
                return;
            }
            double[] xyz = earthToBox(GeoUtils.toEarthLocation(llp));
            if (which.equals("origin")) {
                originLoc = Misc.toFloat(xyz);
            } else {
                pointLoc = Misc.toFloat(xyz);
            }
            updatePosition();
        } catch (Exception e) {
            showNormalCursor();
            logException("Error going to address", e);
        }

    }




    /**
     *  Set the Font property.
     *
     *  @param value The new value for Font
     */
    public void setFont(Font value) {
        font = value;
        try {
            if (textHolder != null) {
                float size = getDisplayScale() * (font.getSize() / 12.0f);
                //System.err.println ("size:" + size);
                /*
                cText.setPointSize(size);
                xText.setPointSize(size);
                yText.setPointSize(size);
                zText.setPointSize(size);
                updatePosition();
                */
            }
        } catch (Exception exc) {
            logException("Setting font", exc);
        }


    }



    /**
     * Set the Origin property.
     *
     * @param value The new value for Origin
     */
    public void setOriginLoc(float[] value) {
        originLoc = value;
    }

    /**
     * Get the Origin property.
     *
     * @return The Origin
     */
    public float[] getOriginLoc() {
        return originLoc;
    }



    /**
     * Set the Point property.
     *
     * @param value The new value for Point
     */
    public void setPointLoc(float[] value) {
        pointLoc = value;
    }

    /**
     * Get the Point property.
     *
     * @return The Point
     */
    public float[] getPointLoc() {
        return pointLoc;
    }



    /**
     * Set the VisibleX property.
     *
     * @param value The new value for VisibleX
     */
    public void setVisibleX(boolean value) {}



    /**
     * Set the Span property.
     *
     * @param value The new value for Span
     */
    public void setSpan(float value) {
        span = value;
    }

    /**
     * Get the Span property.
     *
     * @return The Span
     */
    public float getSpan() {
        return span;
    }


    /**
     * Set the KeepInBox property.
     *
     * @param value The new value for KeepInBox
     */
    public void setKeepInBox(boolean value) {
        keepInBox = value;
    }

    /**
     * Get the KeepInBox property.
     *
     * @return The KeepInBox
     */
    public boolean getKeepInBox() {
        return keepInBox;
    }


    /**
     * Get the x axis info
     *
     * @return The x axis info
     */
    public AxisInfo getXInfo() {
        if (xInfo == null) {
            xInfo = new AxisInfo(this);
        }
        xInfo.index = IDX_X;
        return xInfo;
    }

    /**
     * Set the x axis info
     *
     * @param info the x axis info
     */
    public void setXInfo(AxisInfo info) {
        xInfo = info;
        if (xInfo != null) {
            xInfo.lic = this;
        }
    }

    /**
     * Get the y axis info
     *
     * @return The y axis info
     */
    public AxisInfo getYInfo() {
        if (yInfo == null) {
            yInfo = new AxisInfo(this);
        }
        yInfo.index = IDX_Y;
        return yInfo;
    }

    /**
     * Set the y axis info
     *
     * @param info the y axis info
     */
    public void setYInfo(AxisInfo info) {
        yInfo = info;
        if (yInfo != null) {
            yInfo.lic = this;
        }
    }

    /**
     * Get the z axis info
     *
     * @return The z axis info
     */
    public AxisInfo getZInfo() {
        if (zInfo == null) {
            zInfo = new AxisInfo(this);
        }
        zInfo.index = IDX_Z;
        return zInfo;
    }

    /**
     * Set the z axis info
     *
     * @param info the z axis info
     */
    public void setZInfo(AxisInfo info) {
        zInfo = info;
        if (zInfo != null) {
            zInfo.lic = this;
        }
    }


    /**
     * Holds state about an axis
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.46 $
     */
    public static class AxisInfo {


        /** used for creating text */
        private double[] TEXT_START = { 0.0, 0.0, 0.0 };

        /** used for creating text */
        private double[] TEXT_BASE = { 1.0, 0.0, 0.0 };

        /** used for creating text */
        private double[] TEXT_UP = { 0.0, 1.0, 1.0 };


        /** The control */
        private LocationIndicatorControl lic;

        /** Should the point be shown */
        private boolean showLines = false;


        /** Axis visible */
        private boolean visible = true;

        /** Axis label visible */
        private boolean labelVisible = true;

        /** Show solid plane */
        private boolean solid = false;

        /** Can move */
        private boolean move = true;

        /** Axis holder */
        private CompositeDisplayable axis;

        /** Axis line */
        private LineDrawing axisLine;

        /** Line to point */
        private LineDrawing line;

        /** Tick mark on axis */
        private ShapeDisplayable tick;

        /** The solid */
        private ShapeDisplayable solidDisplayable;

        /** The index (x,y or x) */
        private int index;

        /** Text at center */
        private ShapeDisplayable tickTextDisplayable;

        /** X position of tick */
        private float tickX = 0.0f;

        /** Y position of tick */
        private float tickY = 0.0f;

        /** Z position of tick */
        private float tickZ = 0.0f;

        /** _more_          */
        private int clip = CLIP_NONE;


        /**
         * Ctor
         */
        public AxisInfo() {}



        /**
         * Ctor
         *
         * @param lic The control
         */
        public AxisInfo(LocationIndicatorControl lic) {
            this.lic = lic;
        }



        /**
         * Create displayables
         *
         * @throws RemoteException On badness
         * @throws VisADException On badness
         */
        public void initDisplayables()
                throws VisADException, RemoteException {
            float scale = lic.getDisplayScale();

            lic.textHolder.addDisplayable(tickTextDisplayable =
                new ShapeDisplayable("Text" + (typeCnt++), "-"));
            lic.displayHolder.addDisplayable(axis =
                new CompositeDisplayable());
            axis.addDisplayable(axisLine =
                new LineDrawing("LocationIndicatorControl.axis"));
            line = new LineDrawing("LocationIndicatorControl.xline");
            axis.addDisplayable(line);
            line.setLineStyle(GraphicsModeControl.DASH_STYLE);
            axis.addDisplayable(tick = new ShapeDisplayable("tick"
                    + (typeCnt++), lic.getTickMark()));
            lic.solidHolder.addDisplayable(solidDisplayable =
                new ShapeDisplayable("solid" + (typeCnt++), ""));


            tickTextDisplayable.setAutoSize(true);
            tick.setAutoSize(true);
        }

        /**
         * Toggle visibility of sub-components
         *
         */
        public void checkVisibility() {
            if (axis == null) {
                return;
            }
            try {
                boolean mainVis = lic.getDisplayVisibility();
                axis.setVisible(mainVis && visible);
                solidDisplayable.setVisible(mainVis && solid);
                tick.setVisible(mainVis && visible && lic.getShowPoint());
                tickTextDisplayable.setVisible(mainVis && visible
                        && lic.getShowPoint() && labelVisible);
                line.setVisible(mainVis && visible && lic.getShowPoint()
                                && getShowLines());
            } catch (Exception e) {
                lic.logException("Check visibility", e);
            }
        }



        /**
         * Move the solid plane
         *
         *
         * @param originEl Origin earth location
         * @param pointEl Point earth location
         * @throws RemoteException On badness
         * @throws VisADException On badness
         */
        void updateAxisPosition(EarthLocation originEl, EarthLocation pointEl)
                throws VisADException, RemoteException {
            solidDisplayable.setMarker(makeSolid());

            float[] min    = lic.getMin();
            float[] max    = lic.getMax();
            float[] origin = lic.getOriginLoc();
            float[] point  = lic.getPointLoc();


            tickX = ((index == IDX_X)
                     ? point[IDX_X]
                     : origin[IDX_X]);
            tickY = ((index == IDX_Y)
                     ? point[IDX_Y]
                     : origin[IDX_Y]);
            tickZ = ((index == IDX_Z)
                     ? point[IDX_Z]
                     : origin[IDX_Z]);

            tick.setPoint(tickX, tickY, tickZ);
            VisADGeometryArray textMarker = null;
            DisplayConventions dc =
                lic.getControlContext().getDisplayConventions();

            double[] aspect     =
                lic.getNavigatedDisplay().getDisplayAspect();
            double   zRotFactor = 1.0;
            if ((aspect != null) && (aspect.length > 2)) {
                if (aspect[2] > 0.0) {
                    zRotFactor = zRotFactor / aspect[2];
                }
            }
            TEXT_UP[2] = zRotFactor;


            double deltaTick = 0.05 * lic.getDisplayScale();
            if (index == IDX_X) {
                setPts(axisLine, min[IDX_X], max[IDX_X], origin[IDX_Y],
                       origin[IDX_Y], origin[IDX_Z], origin[IDX_Z]);
                setPts(line, point[IDX_X], point[IDX_X], point[IDX_Y],
                       origin[IDX_Y], point[IDX_Z], origin[IDX_Z]);
                String text =
                    dc.formatLatLon(
                        pointEl.getLatLonPoint().getLongitude().getValue());
                textMarker = PlotText.render_label(text, TEXT_START,
                        TEXT_BASE, TEXT_UP, TextControl.Justification.CENTER,
                        TextControl.Justification.TOP);
                tickTextDisplayable.setPoint(tickX, tickY - deltaTick,
                                             tickZ - deltaTick);
            } else if (index == IDX_Y) {
                setPts(axisLine, origin[IDX_X], origin[IDX_X], min[IDX_Y],
                       max[IDX_Y], origin[IDX_Z], origin[IDX_Z]);
                setPts(line, point[IDX_X], origin[IDX_X], point[IDX_Y],
                       point[IDX_Y], point[IDX_Z], origin[IDX_Z]);

                String text =
                    dc.formatLatLon(
                        pointEl.getLatLonPoint().getLatitude().getValue());
                textMarker = PlotText.render_label(text, TEXT_START,
                        TEXT_BASE, TEXT_UP, TextControl.Justification.RIGHT,
                        TextControl.Justification.CENTER);

                tickTextDisplayable.setPoint(tickX - deltaTick, tickY,
                                             tickZ - deltaTick);
            } else {
                setPts(axisLine, origin[IDX_X], origin[IDX_X], origin[IDX_Y],
                       origin[IDX_Y], min[IDX_Z], max[IDX_Z]);
                setPts(line, point[IDX_X], origin[IDX_X], point[IDX_Y],
                       origin[IDX_Y], point[IDX_Z], point[IDX_Z]);

                String text =
                    dc.formatLatLon(pointEl.getAltitude().getValue())
                    + pointEl.getAltitude().getUnit();
                textMarker = PlotText.render_label(text, TEXT_START,
                        TEXT_BASE, TEXT_UP, TextControl.Justification.RIGHT,
                        TextControl.Justification.TOP);
                tickTextDisplayable.setPoint(tickX - deltaTick,
                                             tickY - deltaTick, tickZ);
            }
            setLabelPosition();
            tickTextDisplayable.setPointSize((float) lic.getDisplayScale());
            textMarker = ShapeUtility.setSize(textMarker, 0.05f);

            tickTextDisplayable.setMarker(textMarker);
        }


        /**
         * Set the position of the label
         *
         * @throws RemoteException On badness
         * @throws VisADException On badness
         */
        public void setLabelPosition()
                throws VisADException, RemoteException {
            double deltaTick = 0.05 * lic.getDisplayScale();
            if (index == IDX_X) {
                tickTextDisplayable.setPoint(tickX, tickY - deltaTick,
                                             tickZ - deltaTick);
            } else if (index == IDX_Y) {
                tickTextDisplayable.setPoint(tickX - deltaTick, tickY,
                                             tickZ - deltaTick);
            } else {
                tickTextDisplayable.setPoint(tickX - deltaTick,
                                             tickY - deltaTick, tickZ);
            }

        }


        /**
         * Make the solid plane
         *
         * @return The solid
         */
        VisADGeometryArray makeSolid() {
            VisADGeometryArray shape = new VisADQuadArray();

            float[]            min   = lic.getMin();
            float[]            max   = lic.getMax();
            float              minX  = min[IDX_X],
                               maxX  = max[IDX_X],
                               minY  = min[IDX_Y],
                               maxY  = max[IDX_Y],
                               minZ  = min[IDX_Z],
                               maxZ  = max[IDX_Z];
            float              ox    = lic.originLoc[IDX_X];
            float              oy    = lic.originLoc[IDX_Y];
            float              oz    = lic.originLoc[IDX_Z];

            float              nx    = 0.0f;
            float              ny    = 0.0f;
            float              nz    = 0.0f;
            if (index == IDX_X) {
                nz                = 1.0f;
                shape.coordinates = new float[] {
                    minX, minY, oz, maxX, minY, oz, maxX, maxY, oz, minX,
                    maxY, oz
                };
            } else if (index == IDX_Y) {
                nx                = 1.0f;
                shape.coordinates = new float[] {
                    ox, minY, maxZ, ox, minY, minZ, ox, maxY, minZ, ox, maxY,
                    maxZ
                };
            } else {
                ny                = 1.0f;
                shape.coordinates = new float[] {
                    minX, oy, minZ, minX, oy, maxZ, maxX, oy, maxZ, maxX, oy,
                    minZ
                };
            }


            shape.vertexCount = shape.coordinates.length / 3;
            shape.normals     = new float[12];
            for (int i = 0; i < shape.normals.length; i += 3) {
                shape.normals[i]     = nx;
                shape.normals[i + 1] = ny;
                shape.normals[i + 2] = nz;
            }
            return shape;
        }


        /**
         * Set the ShowLines property.
         *
         * @param value The new value for ShowLines
         */
        public void setShowLines(boolean value) {
            showLines = value;
            checkVisibility();
        }

        /**
         * Get the ShowLines property.
         *
         * @return The ShowLines
         */
        public boolean getShowLines() {
            return showLines;
        }


        /**
         * Set the Clip property.
         *
         * @param value The new value for Clip
         */
        public void setClip(int value) {
            clip = value;
            if (lic != null) {
                lic.checkClip(false);
            }
        }

        /** _more_          */
        private JComboBox clipBox;

        /**
         * _more_
         *
         * @return _more_
         */
        JComboBox makeClipBox() {
            Object[] items = { new TwoFacedObject("None", CLIP_NONE),
                               new TwoFacedObject(CLIP_NAMES1[index],
                                   CLIP_POSITIVE),
                               new TwoFacedObject(CLIP_NAMES2[index],
                                   CLIP_NEGATIVE) };
            clipBox = new JComboBox(items);
            clipBox.setSelectedIndex(clip);
            clipBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    clip = clipBox.getSelectedIndex();
                    lic.checkClip(false);
                }
            });
            return clipBox;

        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getClipCoefficient() {
            if (clip == CLIP_POSITIVE) {
                return 1;
            }
            return -1;
        }

        /**
         * Get the Clip property.
         *
         * @return The Clip
         */
        public int getClip() {
            return clip;
        }


        /**
         *  Set the Index property.
         *
         *  @param value The new value for Index
         */
        public void setIndex(int value) {
            this.index = value;
        }

        /**
         *  Get the Index property.
         *
         *  @return The Index
         */
        public int getIndex() {
            return this.index;
        }



        /**
         * Set the Visible property.
         *
         * @param value The new value for Visible
         */
        public void setVisible(boolean value) {
            visible = value;
            if (lic != null) {
                lic.checkVisibility();
            }

        }

        /**
         * Get the Visible property.
         *
         * @return The Visible
         */
        public boolean getVisible() {
            return visible;
        }



        /**
         * Set the label Visible property.
         *
         * @param value The new value
         */
        public void setLabelVisible(boolean value) {
            labelVisible = value;
            if (lic != null) {
                lic.checkVisibility();
            }

        }

        /**
         * Get the label Visible property.
         *
         * @return The label visible
         */
        public boolean getLabelVisible() {
            return labelVisible;
        }


        /**
         * Set the Move property.
         *
         * @param value The new value for Move
         */
        public void setMove(boolean value) {
            move = value;
        }

        /**
         * Get the Move property.
         *
         * @return The Move
         */
        public boolean getMove() {
            return move;
        }


        /**
         * Set the Solid property.
         *
         * @param value The new value for Solid
         */
        public void setSolid(boolean value) {
            solid = value;
            if (lic != null) {
                lic.checkVisibility();
            }
        }

        /**
         * Get the Solid property.
         *
         * @return The Solid
         */
        public boolean getSolid() {
            return solid;
        }
    }


    /**
     * Convert the lat/lon/alt to visad box coordinates
     *
     * @param lat lat
     * @param lon lon
     * @param alt alt
     * @return The visad box coordinates of the given location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double[] latLonAltToXYZ(double lat, double lon, double alt)
            throws VisADException, RemoteException {
        return earthToBox(makeEarthLocation(lat, lon, alt));
    }


    /**
     * Provides a lat/lon/alt readout
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.46 $
     */
    private class LocationReadout implements ActionListener {

        /** Shows the alt unit */
        JLabel altUnitLbl;

        /** Lat label */
        JTextField latLbl;

        /** Lon label */
        JTextField lonLbl;

        /** Alt label */
        JTextField altLbl;

        /* Shows the altitude and unit */

        /** Altitude component */
        JComponent altComp;

        /** The lat */
        double lat;

        /** The lon */
        double lon;

        /** The alt */
        double alt;

        /** Initial string in field */
        String latString;

        /** Initial string in field */
        String lonString;

        /** Initial string in field */
        String altString;

        /** The lic */
        private LocationIndicatorControl lic;

        /**
         * Ctor
         *
         * @param lic The lic
         */
        public LocationReadout(LocationIndicatorControl lic) {
            this.lic = lic;
            Font font = Font.decode("monospaced");
            altUnitLbl = new JLabel(" ");

            latLbl     = new JTextField("", 10);
            latLbl.addActionListener(this);
            lonLbl = new JTextField("", 10);
            lonLbl.addActionListener(this);
            altLbl = new JTextField("", 10);
            altLbl.addActionListener(this);
            altComp = GuiUtils.hbox(altLbl, altUnitLbl);

            latLbl.setFont(font);
            lonLbl.setFont(font);
            altLbl.setFont(font);
        }

        /**
         * Handle  the action.
         *
         * @param ae the action
         */
        public void actionPerformed(ActionEvent ae) {
            lic.updatePositionFromReadout(this);
        }

        /**
         * Get the xyz value of the readout
         *
         * @return xyz
         *
         * @throws NumberFormatException On badness
         * @throws RemoteException On badness
         * @throws VisADException On badness
         */
        public float[] getXYZ()
                throws VisADException, RemoteException,
                       NumberFormatException {
            String newLatString = latLbl.getText().trim();
            String newLonString = lonLbl.getText().trim();
            String newAltString = altLbl.getText().trim();
            double newLat       = lat;
            double newLon       = lon;
            double newAlt       = alt;
            if ( !newLatString.equals(latString)) {
                newLat = Misc.parseDouble(newLatString);
            }
            if ( !newLonString.equals(lonString)) {
                newLon = Misc.parseDouble(newLonString);
            }
            if ( !newAltString.equals(altString)) {
                newAlt = Misc.parseDouble(newAltString);
            }


            float[] xyz = Misc.toFloat(lic.latLonAltToXYZ(newLat, newLon,
                              newAlt));
            return xyz;
        }


        /**
         * Set the labels
         *
         * @param el The location
         */
        public void setLocation(EarthLocation el) {
            LatLonPoint llp = el.getLatLonPoint();
            lat = llp.getLatitude().getValue();
            lon = llp.getLongitude().getValue();
            alt = el.getAltitude().getValue();
            DisplayConventions dc =
                getControlContext().getDisplayConventions();
            int pad = 0;
            latLbl.setText(StringUtil.padLeft(latString =
                dc.formatLatLon(lat), pad));
            lonLbl.setText(StringUtil.padLeft(lonString =
                dc.formatLatLon(lon), pad));
            altUnitLbl.setText(" " + el.getAltitude().getUnit());
            altLbl.setText(altString = "" + dc.formatDistance(alt));
        }

        /**
         * tostring
         *
         * @return tostring
         */
        public String toString() {
            return latLbl.getText() + "/" + lonLbl.getText() + " "
                   + altLbl.getText();
        }


    }



    /**
     * Set the ShowPoint property.
     *
     * @param value The new value for ShowPoint
     */
    public void setShowPoint(boolean value) {
        showPoint = value;
        checkVisibility();
        try {
            updatePosition();
        } catch (Exception exc) {
            logException("Updating position", exc);
        }

    }

    /**
     * Get the ShowPoint property.
     *
     * @return The ShowPoint
     */
    public boolean getShowPoint() {
        return showPoint;
    }




    /**
     * Set the SolidAlphaPercent property.
     *
     * @param value The new value for SolidAlphaPercent
     */
    public void setSolidAlphaPercent(double value) {
        solidAlphaPercent = value;
    }

    /**
     * Get the SolidAlphaPercent property.
     *
     * @return The SolidAlphaPercent
     */
    public double getSolidAlphaPercent() {
        return solidAlphaPercent;
    }




}

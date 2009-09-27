/*
 * $Id: ViewManager.java,v 1.401 2007/08/16 14:05:04 jeffmc Exp $
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
 * This library is distributed in the hope that it will be2 useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package ucar.unidata.idv;


import org.w3c.dom.*;

import ucar.unidata.idv.ui.CursorReadoutWindow;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.view.*;
import ucar.unidata.view.geoloc.*;

import ucar.unidata.xml.XmlUtil;



import ucar.visad.Util;

import ucar.visad.display.*;

import visad.*;

import visad.georef.*;


import visad.georef.*;

import visad.java3d.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.media.j3d.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import javax.vecmath.*;



/**
 *
 * @author IDV development team
 */

public class Flythrough implements PropertyChangeListener {

    /** _more_          */
    private MapViewManager viewManager;

    /** _more_          */
    private List<FlythroughPoint> points = new ArrayList<FlythroughPoint>();

    /** _more_          */
    private int index = 0;

    /** _more_          */
    private JTextField zoomFld;

    /** _more_          */
    private JTextField tiltFld;

    /** _more_          */
    private JRadioButton backBtn;


    /** _more_          */
    private JFrame frame;

    /** The line from the origin to the point */
    private LineDrawing flyThroughLine;


    /** _more_          */
    private double tilt = 0.01;

    /** _more_          */
    private double zoom = 1.0;

    /** _more_          */
    private boolean changeViewpoint = true;

    /** _more_          */
    private boolean showReadout = true;


    /** Animation info */
    private AnimationInfo animationInfo;

    /** _more_          */
    private Animation animation;

    /** The anim widget */
    private AnimationWidget animationWidget;

    /** _more_          */
    private FlythroughPoint currentPoint;

    /** _more_          */
    private CursorReadoutWindow readout;

    /** _more_          */
    private JLabel readoutLabel;

    /** _more_          */
    private JCheckBox showReadoutCbx;

    /** _more_          */
    private JCheckBox changeViewpointCbx;

    /**
     * _more_
     */
    public Flythrough() {}


    /**
     * _more_
     *
     * @param viewManager _more_
     */
    public Flythrough(MapViewManager viewManager) {
        this.viewManager = viewManager;
    }



    /**
     * _more_
     */
    public void destroy() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }


    /**
     * Set the AnimationInfo property.
     *
     * @param value The new value for AnimationInfo
     */
    public void setAnimationInfo(AnimationInfo value) {
        animationInfo = value;
    }

    /**
     * Get the AnimationInfo property.
     *
     * @return The AnimationInfo
     */
    public AnimationInfo getAnimationInfo() {
        if (animationWidget != null) {
            return animationWidget.getAnimationInfo();
        }
        return animationInfo;
    }


    /**
     * _more_
     *
     * @param ld _more_
     * @param x1 _more_
     * @param x2 _more_
     * @param y1 _more_
     * @param y2 _more_
     * @param z1 _more_
     * @param z2 _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
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

    /** tmp for flythrough */
    int flythroughTimeStamp = 0;

    /** tmp for flythrough */
    private Object FLYTHROUGH_MUTEX = new Object();




    /**
     * _more_
     *
     * @param pts _more_
     */
    public void setPoints(final float[][] pts) {
        List<FlythroughPoint> points     = new ArrayList<FlythroughPoint>();
        NavigatedDisplay      navDisplay = viewManager.getNavigatedDisplay();
        for (int i = 0; i < pts[0].length; i++) {
            EarthLocation el = navDisplay.getEarthLocation(pts[0][i],
                                   pts[1][i], pts[2][i], false);
            System.err.println("el:" + el + "  " + pts[2][i]);
            points.add(new FlythroughPoint(el));
        }
        this.points = points;
        show();
    }



    /**
     * tmp
     *
     * @param pts pts
     *
     * @param points _more_
     */
    public void flythrough(final List<FlythroughPoint> points) {
        this.points = points;
        setAnimationTimes();
    }

    /**
     * _more_
     */
    private void setAnimationTimes() {
        try {
            Set                   set           = null;
            List<FlythroughPoint> points        = this.points;
            boolean               showIndicator = false;
            if ((points != null) && !points.isEmpty()) {
                DateTime[] timeArray = new DateTime[points.size()];
                for (int i = 0; i < points.size(); i++) {
                    Date dttm = points.get(i).getDateTime();
                    if (dttm == null) {
                        dttm = new Date(i * 1000 * 60 * 60 * 24);
                    } else {
                        showIndicator = true;
                    }
                    timeArray[i] = new DateTime(dttm);
                }
                set = DateTime.makeTimeSet(timeArray);
            }
            System.err.println("show:" + showIndicator);
            animationWidget.showDateBox(showIndicator);
            animationWidget.setBaseTimes(set);
        } catch (Exception exc) {
            viewManager.logException("Setting flythrough", exc);
        }
    }



    /**
     * _more_
     */
    public void goToCurrent() {
        if (animation != null) {
            try {
                doStep(animation.getCurrent());
            } catch (Exception exc) {
                viewManager.logException("Setting flythrough", exc);
            }
        }
    }

    /**
     * _more_
     *
     * @param evt _more_
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Animation.ANI_VALUE)) {
            goToCurrent();
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void doMakeContents() throws Exception {
        if (frame != null) {
            return;
        }
        readout = new CursorReadoutWindow(viewManager);
        changeViewpointCbx = new JCheckBox("Change Viewpoint",
                                           changeViewpoint);
        showReadoutCbx = new JCheckBox("Show Readout", showReadout);
        readoutLabel =
            GuiUtils.getFixedWidthLabel("<html><br><br><br></html>");
        readoutLabel.setVerticalAlignment(SwingConstants.TOP);
        //        readoutLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        if (animationInfo == null) {
            animationInfo = new AnimationInfo();
        }
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                goToCurrent();
            }
        };
        animationWidget = new AnimationWidget(null, null, animationInfo);
        animationWidget.setShareGroup("flythrough");
        animation = new Animation();
        animation.addPropertyChangeListener(this);
        animation.setAnimationInfo(animationInfo);
        animationWidget.setAnimation(animation);


        flyThroughLine = new LineDrawing("LocationIndicatorControl.xline");
        flyThroughLine.setColor(Color.blue);
        viewManager.getMaster().addDisplayable(flyThroughLine);
        flyThroughLine.setLineWidth(3);

        frame   = new JFrame("IDV - Flythrough");
        zoomFld = new JTextField(zoom + "", 5);
        tiltFld = new JTextField(tilt + "", 5);
        zoomFld.addActionListener(listener);
        tiltFld.addActionListener(listener);

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent flds = GuiUtils.formLayout(new Component[] {
            changeViewpointCbx, GuiUtils.filler(), GuiUtils.rLabel("Zoom:"),
            GuiUtils.left(zoomFld), GuiUtils.rLabel("Tilt:"),
            GuiUtils.left(tiltFld)
        });

        JComponent contents = GuiUtils.vbox(animationWidget.getContents(),
                                            flds, GuiUtils.filler(400, 5));
        contents = GuiUtils.topCenter(
            contents,
            GuiUtils.topCenter(GuiUtils.left(showReadoutCbx), readoutLabel));
        contents = GuiUtils.inset(contents, 5);
        setAnimationTimes();
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setLocation(400, 400);
    }


    /**
     * _more_
     */
    public void show() {
        if (frame == null) {
            try {
                doMakeContents();
            } catch (Exception exc) {
                viewManager.logException("Showing flythrough", exc);
            }
        }
        setAnimationTimes();
        frame.show();
    }


    /**
     * tmp
     *
     * @param myTimeStamp timestamp
     *
     * @param index _more_
     *
     * @throws Exception _more_
     */
    private void doStep(int index) throws Exception {
        NavigatedDisplay navDisplay     = viewManager.getNavigatedDisplay();
        MouseBehavior    mouseBehavior  = navDisplay.getMouseBehavior();
        double[]         lastGoodMatrix = navDisplay.getProjectionMatrix();
        double[]         aspect         = navDisplay.getDisplayAspect();
        double[]         trans          = { 0.0, 0.0, 0.0 };
        double[]         scale          = { 0.0, 0.0, 0.0 };
        double[]         rot            = { 0.0, 0.0, 0.0 };
        mouseBehavior.instance_unmake_matrix(rot, scale, trans,
                                             lastGoodMatrix);
        double[] xyz1     = { 0, 0, 0 };
        double[] xyz2     = { 0, 0, 0 };

        Vector3d upVector = new Vector3d(0, 0, 1);
        if (index + 1 >= points.size()) {
            index = 0;
        } else if (index < 0) {
            index = points.size() - 1;
        }
        try {
            FlythroughPoint pt1 = points.get(index);
            currentPoint = pt1;
            FlythroughPoint pt2 = points.get(index + 1);

            xyz1 = navDisplay.getSpatialCoordinates(pt1.getEarthLocation(),
                    xyz1);
            readoutLabel.setText(readout.getReadout(pt1.getEarthLocation(),
                    showReadoutCbx.isSelected(), true));
            xyz2 = navDisplay.getSpatialCoordinates(pt2.getEarthLocation(),
                    xyz2);

            float  x1   = (float) xyz1[0];
            float  y1   = (float) xyz1[1];
            float  z1   = (float) xyz1[2];
            float  x2   = (float) xyz2[0];
            float  y2   = (float) xyz2[1];
            float  z2   = (float) xyz2[2];

            double zoom = getZoom();
            setPts(flyThroughLine, x1, x1, y1, y1, 1, -1);

            double tilt = getTilt();
            if (zoom != 0) {
                tilt = tilt / zoom;
            }
            z1 += tilt;

            //Check for nans
            if ((x2 != x2) || (y2 != y2) || (z2 != z2)) {
                return;
            }
            if ((x1 != x1) || (y1 != y1) || (z1 != z1)) {
                return;
            }

            //Transform3D t  = new Transform3D(currentMatrix);
            Transform3D t = new Transform3D();
            System.err.println("Look at:" + new Point3d(x1, y1, z1) + "   "
                               + new Point3d(x2, y2, z2));
            t.lookAt(new Point3d(x1, y1, z1), new Point3d(x2, y2, z2),
                     upVector);
            double[] m = new double[16];
            t.get(m);

            if (aspect != null) {
                //                double[] aspectMatrix = mouseBehavior.make_matrix(0.0, 0.0,
                //                                                             0.0, aspect[0],aspect[1], aspect[2], 0.0, 0.0,0.0);                    
                //                m = mouseBehavior.multiply_matrix(aspectMatrix, m);
            }

            double[] scaleMatrix = mouseBehavior.make_matrix(0.0, 0.0, 0.0,
                                       zoom, 0.0, 0.0, 0.0);

            m = mouseBehavior.multiply_matrix(scaleMatrix, m);
            if (changeViewpointCbx.isSelected()) {
                viewManager.getMaster().setProjectionMatrix(m);
            }
            lastGoodMatrix = m;
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
            try {
                viewManager.getMaster().setProjectionMatrix(lastGoodMatrix);
            } catch (Exception ignore) {}
            //                    break;
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public FlythroughPoint getCurrentPoint() {
        return currentPoint;
    }


    /**
     *  Set the Points property.
     *
     *  @param value The new value for Points
     */
    public void setPoints(List<FlythroughPoint> value) {
        this.points = value;
    }

    /**
     *  Get the Points property.
     *
     *  @return The Points
     */
    public List<FlythroughPoint> getPoints() {
        return this.points;
    }

    /**
     *  Set the Tilt property.
     *
     *  @param value The new value for Tilt
     */
    public void setTilt(double value) {
        this.tilt = value;
    }

    /**
     *  Get the Tilt property.
     *
     *  @return The Tilt
     */
    public double getTilt() {
        if (tiltFld != null) {
            this.tilt = new Double(tiltFld.getText().trim()).doubleValue();
        }
        return this.tilt;
    }

    /**
     *  Set the Zoom property.
     *
     *  @param value The new value for Zoom
     */
    public void setZoom(double value) {
        this.zoom = value;
    }

    /**
     *  Get the Zoom property.
     *
     *  @return The Zoom
     */
    public double getZoom() {
        if (zoomFld != null) {
            this.zoom = new Double(zoomFld.getText().trim()).doubleValue();
        }
        return this.zoom;
    }



    /**
     *  Set the ViewManager property.
     *
     *  @param value The new value for ViewManager
     */
    public void setViewManager(MapViewManager value) {
        this.viewManager = value;
    }

    /**
     *  Get the ViewManager property.
     *
     *  @return The ViewManager
     */
    public MapViewManager getViewManager() {
        return this.viewManager;
    }

    /**
     *  Set the ChangeViewpoint property.
     *
     *  @param value The new value for ChangeViewpoint
     */
    public void setChangeViewpoint(boolean value) {
        this.changeViewpoint = value;
    }

    /**
     *  Get the ChangeViewpoint property.
     *
     *  @return The ChangeViewpoint
     */
    public boolean getChangeViewpoint() {
        if (changeViewpointCbx != null) {
            return changeViewpointCbx.isSelected();
        }
        return this.changeViewpoint;
    }

    /**
     *  Set the ShowReadout property.
     *
     *  @param value The new value for ShowReadout
     */
    public void setShowReadout(boolean value) {
        this.showReadout = value;
    }

    /**
     *  Get the ShowReadout property.
     *
     *  @return The ShowReadout
     */
    public boolean getShowReadout() {
        if (showReadoutCbx != null) {
            return showReadoutCbx.isSelected();
        }
        return this.showReadout;
    }



}


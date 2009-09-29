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
import javax.swing.table.*;

import javax.vecmath.*;



/**
 *
 * @author IDV development team
 */

public class Flythrough implements PropertyChangeListener {

    /** _more_ */
    private MapViewManager viewManager;

    /** _more_ */
    private List<FlythroughPoint> points = new ArrayList<FlythroughPoint>();


    /** _more_ */
    private double tiltX = 0.0;

    /** _more_ */
    private double tiltY = 0.0;

    /** _more_ */
    private double tiltZ = 0.0;

    /** _more_ */
    private double zoom = 1.0;

    /** _more_ */
    private boolean changeViewpoint = true;


    /** _more_          */
    private boolean showLine = true;


    /** _more_ */
    private boolean showReadout = true;


    /** Animation info */
    private AnimationInfo animationInfo;

    /** _more_ */
    private Animation animation;

    /** The anim widget */
    private AnimationWidget animationWidget;

    /** _more_ */
    private boolean hasTimes = false;

    /** _more_ */
    private boolean showTimes = false;




    /** _more_ */
    private JCheckBox showTimesCbx;

    /** _more_ */
    private FlythroughPoint currentPoint;

    /** _more_ */
    private CursorReadoutWindow readout;

    /** _more_ */
    private JLabel readoutLabel;

    /** _more_ */
    private JCheckBox showReadoutCbx;

    /** _more_ */
    private JCheckBox changeViewpointCbx;

    /** _more_ */
    private JCheckBox overheadCbx;

    /** _more_ */
    private JTextField zoomFld;

    /** _more_ */
    private JCheckBox orientCbx;

    /** _more_ */
    private JTextField tiltxFld;

    /** _more_ */
    private JTextField tiltyFld;

    /** _more_ */
    private JTextField tiltzFld;

    /** _more_ */
    private JRadioButton backBtn;


    /** _more_ */
    private JFrame frame;

    /** The line from the origin to the point */
    private LineDrawing locationLine;

    /** _more_ */
    private SelectorPoint locationPoint;

    private JTable pointTable;
    private AbstractTableModel pointTableModel;



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
     *
     * @param that _more_
     */
    public void initWith(Flythrough that) {
        this.points          = new ArrayList<FlythroughPoint>(that.points);
        this.animationInfo   = that.animationInfo;
        this.tiltX           = that.tiltX;
        this.tiltY           = that.tiltY;
        this.tiltZ           = that.tiltZ;
        this.zoom            = that.zoom;
        this.changeViewpoint = that.changeViewpoint;
        this.showLine        = that.showLine;
        this.showReadout     = that.showReadout;
        this.showTimes       = that.showTimes;
        try {
            doMakeContents(true);
        } catch (Exception exc) {
            viewManager.logException("Initializing flythrough", exc);
        }
        setAnimationTimes();
    }


    /**
     * _more_
     */
    public void destroy() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        if (animationWidget != null) {
            animationWidget.destroy();
            animationWidget = null;
        }
        viewManager = null;
    }


    /**
     * Set the AnimationInfo property.
     *
     * @param value The new value for AnimationInfo
     */
    public void setAnimationInfo(AnimationInfo value) {
        animationInfo = value;
    }

    public AnimationWidget getAnimationWidget() {
        return animationWidget;
    }

    public Animation getAnimation() {
        return animation;
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
    private void setPts(LineDrawing ld, float x1, float x2, float y1,
                        float y2, float z1, float z2)
            throws VisADException, RemoteException {
        MathType  mathType = RealTupleType.SpatialCartesian3DTuple;
        float[][] pts      = new float[][] {
            { x1, x2 }, { y1, y2 }, { z1, z2 }
        };
        ld.setData(new Gridded3DSet(mathType, pts, 2));
    }




    /**
     * _more_
     *
     * @param pts _more_
     */
    public void flythrough(final float[][] pts) {
        List<FlythroughPoint> points     = new ArrayList<FlythroughPoint>();
        NavigatedDisplay      navDisplay = viewManager.getNavigatedDisplay();
        for (int i = 0; i < pts[0].length; i++) {
            EarthLocation el = navDisplay.getEarthLocation(pts[0][i],
                                   pts[1][i], pts[2][i], false);
            points.add(new FlythroughPoint(el));
        }
        this.points = points;
        setAnimationTimes();
        show();
    }



    /**
     * tmp
     *
     *
     * @param newPoints _more_
     *
     */
    public void flythrough(List<FlythroughPoint> newPoints) {
        while (newPoints.size()>1000) {
            ArrayList<FlythroughPoint> tmp = new ArrayList<FlythroughPoint>();
            for(int i=0;i<newPoints.size();i++) {
                if(i%3==0) {
                    tmp.add(newPoints.get(i));
                }
            }
            newPoints = tmp;
        }

        this.points = new ArrayList<FlythroughPoint>(newPoints);
        setAnimationTimes();
        show();
    }

    private double parse(JTextField fld, double d) {
        String t = fld.getText().trim();
        if(t.length()==0) return d;
        if(t.equals("-")) return d;
        try {
            return new Double(t).doubleValue();
        } catch(NumberFormatException  nfe) {
            animationWidget.setRunning(false);
            viewManager.logException("Parse error:" + t, nfe);
            return d;
        }
    }

    /**
     * _more_
     */
    private void setAnimationTimes() {
        hasTimes = false;
        if (animationWidget == null) {
            return;
        }
        try {
            Set                   set       = null;
            List<FlythroughPoint> thePoints = this.points;
            if ((thePoints != null) && !thePoints.isEmpty()) {
                DateTime[] timeArray = new DateTime[thePoints.size()];
                for (int i = 0; i < thePoints.size(); i++) {
                    DateTime dttm = thePoints.get(i).getDateTime();
                    if (dttm == null) {
                        dttm = new DateTime(new Date(i * 1000 * 60 * 60
                                * 24));
                    } else {
                        hasTimes = true;
                    }
                    timeArray[i] = dttm;
                }
                set = DateTime.makeTimeSet(timeArray);
            }
            if (showTimesCbx != null) {
                showTimesCbx.setEnabled(hasTimes);
            }
            animationWidget.showDateBox(hasTimes);
            animationWidget.setBaseTimes(set);
            if(pointTableModel!=null) {
                pointTableModel.fireTableStructureChanged();
            }
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

    /** _more_ */
    private static int cnt = 0;


    /**
     * _more_
     *
     *
     * @param force _more_
     * @throws Exception _more_
     */
    private synchronized void doMakeContents(boolean force) throws Exception {
        if ( !force && (readout != null)) {
            return;
        }
        if (readout == null) {
            readout = new CursorReadoutWindow(viewManager);
        }

        showTimesCbx = new JCheckBox("Show Animation Times", showTimes);
        showTimesCbx.setEnabled(hasTimes);
        changeViewpointCbx = new JCheckBox("Change Viewpoint",
                                           changeViewpoint);
        orientCbx      = new JCheckBox("Relative", true);
        showReadoutCbx = new JCheckBox("Show Readout", showReadout);
        readoutLabel =
            GuiUtils.getFixedWidthLabel("<html><br><br><br></html>");
        readoutLabel.setVerticalAlignment(SwingConstants.TOP);

        if (animationInfo == null) {
            animationInfo = new AnimationInfo();
        }
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                goToCurrent();
            }
        };
        if (animationWidget == null) {
            animationWidget = new AnimationWidget(null, null, animationInfo);
            animationWidget.setShareGroup("flythrough");
            animation = new Animation();
            animation.addPropertyChangeListener(this);
            animationWidget.setAnimation(animation);
        }

        animation.setAnimationInfo(animationInfo);

        if (locationLine == null) {
            locationLine = new LineDrawing("flythroughpoint.line");
            locationPoint = new SelectorPoint("flythrough.point",
                    new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                                  new double[] { 0,
                    0, 0 }));

            locationPoint.setAutoSize(true);
            locationPoint.setManipulable(false);
            locationPoint.setColor(Color.green);

            locationLine.setVisible(false);
            locationPoint.setVisible(false);
            locationLine.setLineWidth(3);
            locationLine.setColor(Color.blue);
            viewManager.getMaster().addDisplayable(locationPoint);
            viewManager.getMaster().addDisplayable(locationLine);
        }


        zoomFld  = new JTextField(zoom + "", 5);

        tiltxFld = new JTextField("" + tiltX, 4);
        tiltyFld = new JTextField("" + tiltY, 4);
        tiltzFld = new JTextField("" + tiltZ, 4);
        tiltxFld.addActionListener(listener);
        tiltyFld.addActionListener(listener);
        tiltzFld.addActionListener(listener);
        zoomFld.addActionListener(listener);


        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent orientationComp = GuiUtils.formLayout(new Component[] {
            changeViewpointCbx, GuiUtils.filler(),
            GuiUtils.rLabel("Orientation:"), GuiUtils.left(orientCbx),
            GuiUtils.rLabel("Zoom:"), GuiUtils.left(zoomFld),
            GuiUtils.rLabel("Tilt:"),
            GuiUtils.left(GuiUtils.hbox(tiltxFld, tiltyFld, tiltzFld)),
            GuiUtils.rLabel("Animation:"), GuiUtils.left(showTimesCbx),
            GuiUtils.filler(),
            GuiUtils.makeCheckbox("Show Location Line", this, "showLine")
        });



        if(pointTable==null) {
            pointTableModel = new AbstractTableModel() {
                    public int getRowCount() {
                        return points.size();
                    }

                    public int getColumnCount() {
                        return 4;
                    }

                    public Object getValueAt(int row, int column) {
                        List<FlythroughPoint> thePoints = points;
                        if(row>=thePoints.size()) return "n/a";
                        FlythroughPoint pt = thePoints.get(row);
                        if(column==0) return pt.getEarthLocation().getLatitude();
                        if(column==1) return pt.getEarthLocation().getLongitude();
                        if(column==2) return pt.getEarthLocation().getAltitude();
                        if(column==3) return pt.getDateTime();
                        return "";
                    }

                    public String getColumnName(int column) {
                        switch (column) {
                        case 0 :
                            return "Latitude";

                        case 1:
                            return "Longitude";

                        case 2:
                            return "Altitude";

                        case 3:
                            return "Date/Time";
                        }
                        return "";
                    }
                };
            pointTable = new JTable(pointTableModel);
        }
        JScrollPane scrollPane = new JScrollPane(pointTable);
        scrollPane.setPreferredSize(new Dimension(300,400));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("View", GuiUtils.top(orientationComp));
        tabbedPane.addTab("Readout",
                          GuiUtils.topCenter(GuiUtils.left(showReadoutCbx),
                                             readoutLabel));
        tabbedPane.addTab("Points", scrollPane);

        JComponent contents =
            GuiUtils.topCenter(animationWidget.getContents(), tabbedPane);
        contents = GuiUtils.inset(contents, 5);
        setAnimationTimes();
        if (frame == null) {
            frame = new JFrame("IDV - Flythrough");
        }
        frame.getContentPane().removeAll();
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
                doMakeContents(false);
            } catch (Exception exc) {
                viewManager.logException("Showing flythrough", exc);
            }
        }
        setAnimationTimes();
        frame.show();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isActive() {
        return viewManager != null;
    }


    /**
     * fly to the given point
     *
     * @param index _more_
     *
     * @throws Exception _more_
     */
    private void doStep(int index) throws Exception {

        if ( !isActive()) {
            return;
        }


        MapViewManager viewManager  = this.viewManager;
        if(viewManager==null) return;
        boolean               doGlobe       =
            viewManager.getUseGlobeDisplay();
        List<FlythroughPoint> thePoints     = this.points;
        NavigatedDisplay      navDisplay = viewManager.getNavigatedDisplay();
        MouseBehavior         mouseBehavior = navDisplay.getMouseBehavior();
        double[]              currentMatrix =
            navDisplay.getProjectionMatrix();
        double[]              aspect        = navDisplay.getDisplayAspect();
        double[]              xyz1          = { 0, 0, 0 };
        double[]              xyz2          = { 0, 0, 0 };

        Vector3d              upVector;

        if (index >= thePoints.size()) {
            index = 0;
        } else if (index < 0) {
            index = thePoints.size() - 1;
        }
        if(pointTable!=null) { 
            pointTable.getSelectionModel().setSelectionInterval(index,index);
            pointTable.repaint();
        }
        

        int     index1 = index;
        int     index2 = index + 1;
        boolean atEnd  = false;
        if (index2 >= thePoints.size()) {
            index2 = 0;
            atEnd  = true;
        }


        try {
            FlythroughPoint pt1 = thePoints.get(index1);
            FlythroughPoint pt2 = thePoints.get(index2);

            xyz1 = navDisplay.getSpatialCoordinates(pt1.getEarthLocation(),
                    xyz1);
            readoutLabel.setText(readout.getReadout(pt1.getEarthLocation(),
                    showReadoutCbx.isSelected(), true));
            xyz2 = navDisplay.getSpatialCoordinates(pt2.getEarthLocation(),
                    xyz2);

            float x1 = (float) xyz1[0];
            float y1 = (float) xyz1[1];
            float z1 = (float) xyz1[2];

            if (atEnd && (thePoints.size() > 1) && (index1 > 0)) {
                FlythroughPoint prevPt = thePoints.get(index1 - 1);

                double[] xyz3 = navDisplay.getSpatialCoordinates(
                                    prevPt.getEarthLocation(), null);
                xyz2[0] = x1 + (x1 - xyz3[0]);
                xyz2[1] = y1 + (y1 - xyz3[1]);
                xyz2[2] = z1 + (z1 - xyz3[2]);
            }


            float  x2   = (float) xyz2[0];
            float  y2   = (float) xyz2[1];
            float  z2   = (float) xyz2[2];

            double zoom = getZoom();
            if (zoom == 0) {
                zoom = 0.1;
            }

            double tiltx = getTiltX();
            double tilty = getTiltY();
            double tiltz = getTiltZ();



            //Check for nans
            if ((x2 != x2) || (y2 != y2) || (z2 != z2)) {
                return;
            }
            if ((x1 != x1) || (y1 != y1) || (z1 != z1)) {
                return;
            }

            //Transform3D t  = new Transform3D(currentMatrix);
            Transform3D t = new Transform3D();
            //            System.err.println("Look at:" + new Point3d(x1, y1, z1) + "  " + new Point3d(x2, y2, z2));
            if ( !orientCbx.isSelected()) {
                y2 = y1 + 10;
                x2 = x1;
            }

            if (doGlobe) {
                upVector = new Vector3d(x1, y1, z1);
            } else {
                upVector = new Vector3d(0, 0, 1);
            }


            t.lookAt(new Point3d(x1, y1, z1), new Point3d(x2, y2, z2),
                     upVector);
            double[] m = new double[16];
            t.get(m);


            double[] tiltMatrix = mouseBehavior.make_matrix(tiltx, tilty,
                                      tiltz, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
            m = mouseBehavior.multiply_matrix(tiltMatrix, m);
            if (aspect != null) {
                //                double[] aspectMatrix = mouseBehavior.make_matrix(0.0, 0.0,
                //                                                             0.0, aspect[0],aspect[1], aspect[2], 0.0, 0.0,0.0);                    
                //                m = mouseBehavior.multiply_matrix(aspectMatrix, m);
            }

            double[] scaleMatrix = mouseBehavior.make_matrix(0.0, 0.0, 0.0,
                                       zoom, 0.0, 0.0, 0.0);

            m = mouseBehavior.multiply_matrix(scaleMatrix, m);

            double[] screenCenter = navDisplay.getScreenCenter();

            currentPoint = pt1;
            if (changeViewpointCbx.isSelected()) {
                navDisplay.setProjectionMatrix(m);
            }

            if (hasTimes && getShowTimes()) {
                DateTime dttm = pt1.getDateTime();
                if (dttm != null) {
                    viewManager.getAnimationWidget().setTimeFromUser(dttm);
                }

            }



            if (doGlobe) {
                setPts(locationLine, 0, x1*2, 0, y1*2, 0, z1*2);
            } else {
                setPts(locationLine, x1, x1, y1, y1, 1, -1);
            }
            locationPoint.setPoint(
                new RealTuple(
                    RealTupleType.SpatialCartesian3DTuple, new double[] { x1,
                    y1, z1 }));

            locationLine.setVisible(showLine);
            //            locationPoint.setVisible(showLine);

        } catch (NumberFormatException exc) {
            viewManager.logException("Error parsing number:" + exc, exc);
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
            try {
                navDisplay.setProjectionMatrix(currentMatrix);
            } catch (Exception ignore) {}
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

    public boolean hasPoints() {
        return points.size()>0;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setTilt(double value) {}

    /**
     *  Set the Tilt property.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltX(double value) {
        tiltX = value;
    }

    /**
     *  Get the Tilt property.
     *
     *  @return The Tilt
     */
    public double getTiltX() {
        if (tiltxFld != null) {
            this.tiltX = parse(tiltxFld,tiltX);
        }
        return this.tiltX;
    }




    /**
     *  Set the Tilt property.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltY(double value) {
        tiltY = value;
    }

    /**
     *  Get the Tilt property.
     *
     *  @return The Tilt
     */
    public double getTiltY() {
        if (tiltyFld != null) {
            this.tiltY = parse(tiltyFld,tiltY);
        }
        return this.tiltY;
    }




    /**
     *  Set the Tilt propertz.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltZ(double value) {
        tiltZ = value;
    }

    /**
     *  Get the Tilt propertz.
     *
     *  @return The Tilt
     */
    public double getTiltZ() {
        if (tiltzFld != null) {
            this.tiltZ = parse(tiltzFld,tiltZ);
        }
        return this.tiltZ;
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
            this.zoom = parse(zoomFld,tiltZ);
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


    /**
     * Set the ShowTimes property.
     *
     * @param value The new value for ShowTimes
     */
    public void setShowTimes(boolean value) {
        showTimes = value;
    }

    /**
     * Get the ShowTimes property.
     *
     * @return The ShowTimes
     */
    public boolean getShowTimes() {
        if (showTimesCbx != null) {
            showTimes = showTimesCbx.isSelected();
        }
        return showTimes;
    }


    /**
     *  Set the ShowLine property.
     *
     *  @param value The new value for ShowLine
     */
    public void setShowLine(boolean value) {
        showLine = value;
        if (locationLine != null) {
            try {
                locationLine.setVisible(value);
                //                locationPoint.setVisible(value);
            } catch (Exception ignore) {}
        }
    }

    /**
     * Get the ShowLine property.
     *
     * @return The ShowLine
     */
    public boolean getShowLine() {
        return showLine;
    }




}


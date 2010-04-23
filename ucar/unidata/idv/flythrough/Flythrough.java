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

package ucar.unidata.idv.flythrough;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CompassPlot;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ThermometerPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.dial.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


import org.jfree.ui.RectangleInsets;


import org.w3c.dom.*;

import org.w3c.dom.*;

import ucar.unidata.collab.*;


import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.*;

import ucar.unidata.idv.control.ReadoutInfo;
import ucar.unidata.idv.ui.CursorReadoutWindow;
import ucar.unidata.idv.ui.EarthNavPanel;

import ucar.unidata.idv.ui.PipPanel;
import ucar.unidata.ui.ImagePanel;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.*;
import ucar.unidata.view.geoloc.*;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapeUtility;

import ucar.visad.Util;

import ucar.visad.display.*;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.georef.*;

import visad.java3d.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;



import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;




import java.io.File;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.html.*;

import javax.vecmath.*;


/**
 *
 * @author IDV development team
 */

public class Flythrough extends SharableImpl implements PropertyChangeListener,
        ImageObserver {


    /** _more_ */
    public static final String ORIENT_POINT = "point";

    /** _more_ */
    public static final String ORIENT_FORWARD = "forward";

    /** _more_ */
    public static final String ORIENT_UP = "up";

    /** _more_ */
    public static final String ORIENT_DOWN = "down";

    /** _more_ */
    public static final String ORIENT_LEFT = "left";

    /** _more_ */
    public static final String ORIENT_RIGHT = "right";


    /** _more_ */
    private static final String DIR_FORWARD = "forward";

    /** _more_ */
    private static final String DIR_BACK = "back";

    /** _more_ */
    private static final String DIR_LEFT = "left";

    /** _more_ */
    private static final String DIR_RIGHT = "right";





    /** xml tag and attr name */
    public static final String TAG_FLYTHROUGH = "flythrough";

    /** xml tag and attr name */
    public static final String TAG_DESCRIPTION = "description";

    /** xml tag and attr name */
    public static final String TAG_POINT = "point";

    /** xml tag and attr name */
    public static final String ATTR_DATE = "date";

    /** xml tag and attr name */
    public static final String ATTR_LAT = "lat";

    /** xml tag and attr name */
    public static final String ATTR_LON = "lon";

    /** xml tag and attr name */
    public static final String ATTR_ALT = "alt";

    /** xml tag and attr name */
    public static final String[] ATTR_TILT = { "tiltx", "tilty", "tiltz" };

    /** xml tag and attr name */
    public static final String ATTR_ZOOM = "zoom";

    /** xml tag and attr name */
    public static final String ATTR_MATRIX = "matrix";


    /** The view manager I am part of */
    private MapViewManager viewManager;


    /** date formatter */
    private SimpleDateFormat sdf;



    /** _more_ */
    private List<FlythroughPoint> allPoints =
        new ArrayList<FlythroughPoint>();

    /** _more_ */
    private List<FlythroughPoint> pointsToUse =
        new ArrayList<FlythroughPoint>();

    /** _more_ */
    private int stride = 1;


    /** _more_ */
    private double[] tilt = { -30.0, 0.0, 0.0 };

    /** _more_ */
    JSlider[] tiltSliders = { null, null, null };

    /** _more_ */
    JLabel[] tiltLabels = { null, null, null };


    /** _more_ */
    private double zoom = 1.0;

    /** _more_ */
    private boolean lastMoveWasTrack = true;

    /** _more_ */
    private boolean goToClick = false;

    /** _more_ */
    private boolean showAnimation = true;

    /** _more_ */
    private boolean showDecoration = true;


    /** _more_ */
    private boolean changeViewpoint = true;

    /** _more_ */
    private boolean animate = false;

    /** _more_ */
    private int animationSpeed = 50;

    /** _more_ */
    private JCheckBox animateCbx;


    /** Animation info */
    private AnimationInfo animationInfo;

    /** _more_ */
    private Animation animation;

    /** The anim widget */
    private AnimationWidget animationWidget;

    /** _more_ */
    private boolean hasTimes = false;

    /** _more_ */
    private String orientation = ORIENT_POINT;

    /** _more_ */
    private JCheckBox showTimesCbx;

    /** _more_ */
    private FlythroughPoint currentPoint;

    /** _more_ */
    private CursorReadoutWindow readout;

    /** _more_ */
    private JLabel readoutLabel;

    /** _more_ */
    private JComponent readoutDisplay;


    /** _more_ */
    private JCheckBox changeViewpointCbx;

    /** _more_ */
    private JTextField zoomFld;

    /** _more_ */
    private JComboBox orientBox;

    /** _more_ */
    private Vector<TwoFacedObject> orients = new Vector<TwoFacedObject>();


    /** _more_ */
    private JCheckBox fixedZCbx;


    /** _more_ */
    private JRadioButton backBtn;


    /** _more_ */
    private JFrame frame;

    /** _more_ */
    private Rectangle windowBounds;


    /** The line from the origin to the point */
    private LineDrawing locationLine;

    /** _more_ */
    private LineDrawing locationLine2;

    /** _more_ */
    private SelectorPoint locationMarker;

    /** _more_ */
    private JTable pointTable;

    /** _more_ */
    private FlythroughTableModel pointTableModel;

    /** _more_ */
    private JEditorPane htmlView;


    /** _more_ */
    private JComponent contents;

    /** _more_ */
    private boolean shown = false;

    /** _more_ */
    private boolean useFixedZ = true;

    /** _more_ */
    private int currentIndex = 0;

    /** _more_ */
    private EarthLocation location;

    /** _more_ */
    private double heading = 0;

    /** _more_ */
    private double currentHeading = 0;

    /** _more_ */
    private JLabel dashboardLbl;

    /** _more_ */
    private EarthNavPanel earthNavPanel;

    /** _more_ */
    private PipPanel pipPanel;

    /** _more_ */
    private JFrame pipFrame;

    /** _more_ */
    private Rectangle pipRect;

    /** _more_ */
    private Dimension dialDimension = new Dimension(180, 130);

    /**
     * The location on the dashboard image of the dials.
     * this is of the form:<pre>
     * {centerx, centery, width, height}
     * The first one is used for the map. The second for the compass.
     * The third is the lat/lon readout. The x/y is the upper left and width is ignored for the label
     * </pre>
     */
    private int[][] dialPts = {
        { 402, 100, 206, 145 }, { 256, 90, 120, 100 }, { 515, 60, 130, 100 },
        { 224, 180, 170, 100 }, { 575, 180, 170, 100 },
        { 356, 240, 150, 100 }, { 447, 240, 150, 100 }, { 687, 262, 90, 100 }
    };

    /** _more_ */
    private List<JComponent> dials = new ArrayList<JComponent>();


    /** _more_ */
    protected Point dashboardImageOffset = new Point(0, 0);



    /** _more_ */
    private Image dashboardImage;





    /** _more_ */
    private boolean showTimes = false;

    /** _more_ */
    private boolean showLine = true;

    /** _more_ */
    private boolean showMarker = true;

    /** _more_ */
    private boolean showReadout = true;

    /** list of decorators */
    private List<FlythroughDecorator> decorators =
        new ArrayList<FlythroughDecorator>();


    /** _more_ */
    private double[] lastViewpoint;



    /** _more_ */
    private EarthLocation lastLocation;

    /** _more_ */
    private int lastIndex = -1;






    /** _more_ */
    private Object REPAINT_MUTEX = new Object();

    /** _more_ */
    private int repaintCnt = 0;


    /**
     * _more_
     */
    public Flythrough() {
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        decorators.add(new ImageDecorator(this));
        decorators.add(new WeatherDecorator(this));
        decorators.add(new ChartDecorator(this));
    }



    /**
     * _more_
     *
     * @param viewManager _more_
     */
    public Flythrough(MapViewManager viewManager) {
        this();
        this.viewManager = viewManager;
    }


    /**
     * _more_
     *
     * @param viewManager _more_
     */
    public void setViewManager(MapViewManager viewManager) {
        this.viewManager = viewManager;
    }


    /**
     * _more_
     *
     * @param viewManager _more_
     */
    public void init(MapViewManager viewManager) {
        this.viewManager = viewManager;
        for (FlythroughDecorator decorator : decorators) {
            decorator.setFlythrough(this);
        }
    }



    /**
     * _more_
     *
     * @param that _more_
     */
    public void initWith(Flythrough that) {
        if (this == that) {
            return;
        }
        this.allPoints       = new ArrayList<FlythroughPoint>(that.allPoints);
        this.pointsToUse = new ArrayList<FlythroughPoint>(that.pointsToUse);
        this.stride          = that.stride;

        this.animationInfo   = that.animationInfo;
        this.tilt = new double[] { that.tilt[0], that.tilt[1], that.tilt[2] };
        this.zoom            = that.zoom;
        this.changeViewpoint = that.changeViewpoint;
        this.showLine        = that.showLine;
        this.showMarker      = that.showMarker;
        this.showReadout     = that.showReadout;
        this.showTimes       = that.showTimes;
        setAnimationTimes();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public MapViewManager getViewManager() {
        return viewManager;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public IntegratedDataViewer getIdv() {
        return viewManager.getIdv();
    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void destroy() throws VisADException, RemoteException {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }

        if ((locationMarker != null) && (viewManager != null)) {
            viewManager.getMaster().removeDisplayable(locationMarker);
            viewManager.getMaster().removeDisplayable(locationLine);
            //            viewManager.getMaster().removeDisplayable(locationLine2);
        }


        if (animationWidget != null) {
            animationWidget.destroy();
            animationWidget = null;
        }
        viewManager = null;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public AnimationWidget getAnimationWidget() {
        return animationWidget;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Animation getAnimation() {
        return animation;
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
        flythrough(points);
    }



    /**
     * tmp
     *
     *
     * @param newPoints _more_
     *
     */
    public void flythrough(List<FlythroughPoint> newPoints) {
        flythrough(newPoints, true);
    }


    /**
     * _more_
     *
     * @param newPoints _more_
     * @param andShow _more_
     */
    public void flythrough(List<FlythroughPoint> newPoints, boolean andShow) {
        this.allPoints = new ArrayList<FlythroughPoint>(newPoints);
        //subsample
        ArrayList<FlythroughPoint> tmp = new ArrayList<FlythroughPoint>();
        for (int i = 0; i < newPoints.size(); i += stride) {
            tmp.add(newPoints.get(i));
        }
        newPoints        = tmp;

        this.pointsToUse = new ArrayList<FlythroughPoint>(newPoints);
        if (animation != null) {
            animation.setCurrent(currentIndex);
            setAnimationTimes();
        }
        if (andShow) {
            show();
        }
    }


    /**
     * _more_
     *
     * @param fld _more_
     * @param d _more_
     *
     * @return _more_
     */
    private double parse(JTextField fld, double d) {
        String t = fld.getText().trim();
        if (t.length() == 0) {
            return d;
        }
        if (t.equals("-")) {
            return d;
        }
        try {
            return Misc.parseNumber(t);
        } catch (NumberFormatException nfe) {
            animationWidget.setRunning(false);
            logException("Parse error:" + t, nfe);
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
            List<FlythroughPoint> thePoints = this.pointsToUse;
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
            if (pointTableModel != null) {
                pointTableModel.fireTableStructureChanged();
            }
        } catch (Exception exc) {
            logException("Setting flythrough", exc);
        }
    }



    /**
     * _more_
     */
    public void goToCurrent() {
        try {
            if ( !lastMoveWasTrack) {
                doDrive(false, heading);
            } else {
                if (animation != null) {
                    doStep(animation.getCurrent());

                }
            }
        } catch (Exception exc) {
            logException("Setting flythrough", exc);
        }
    }

    /**
     * _more_
     */
    public void updateDashboard() {
        if (dashboardLbl != null) {
            dashboardLbl.repaint();
        }
    }

    /**
     * _more_
     *
     * @param img _more_
     * @param flags _more_
     * @param x _more_
     * @param y _more_
     * @param width _more_
     * @param height _more_
     *
     * @return _more_
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {
        if ((flags & ImageObserver.ERROR) != 0) {
            return false;
        }
        updateDashboard();
        if ((flags & ImageObserver.ALLBITS) != 0) {
            return false;
        }
        return true;
    }


    /**
     * _more_
     *
     * @param evt _more_
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Animation.ANI_VALUE)) {
            lastMoveWasTrack = true;
            goToCurrent();
        }
    }



    /**
     * _more_
     *
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void makeWidgets() throws VisADException, RemoteException {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Dashboard", doMakeDashboardPanel());
        tabbedPane.addTab("Values", doMakeValuesPanel());
        tabbedPane.addTab("Description", doMakeDescriptionPanel());
        tabbedPane.addTab("Viewpoint", doMakeViewpointPanel());
        tabbedPane.addTab("Points", doMakePointsPanel());


        JComponent innerContents =
            GuiUtils.topCenterBottom(animationWidget.getContents(),
                                     tabbedPane, doMakeNavigationPanel());

        if ( !showAnimation) {
            animationWidget.getContents().setVisible(false);
        }
        JComponent menuBar = doMakeMenuBar();
        innerContents = GuiUtils.inset(innerContents, 5);
        contents      = GuiUtils.topCenter(menuBar, innerContents);
        animation.setCurrent(currentIndex);
    }


    /**
     * Create if needed and return the editorpane for the description tab
     *
     * @return description view
     */
    private JEditorPane getHtmlView() {
        if (htmlView == null) {
            JEditorPane tmp = new JEditorPane();
            tmp.setContentType("text/html");
            tmp.setPreferredSize(new Dimension(300, 400));
            tmp.setEditable(false);
            tmp.setText(" ");
            htmlView = tmp;
        }
        return htmlView;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeDescriptionPanel() {
        JScrollPane htmlScrollPane = new JScrollPane(getHtmlView());
        htmlScrollPane.setPreferredSize(new Dimension(400, 300));
        return htmlScrollPane;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JMenuBar doMakeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = GuiUtils.makeDynamicMenu("File", this,
                             "initFileMenu");
        JMenu editMenu = GuiUtils.makeDynamicMenu("Edit", this,
                             "initEditMenu");
        JMenu viewMenu = GuiUtils.makeDynamicMenu("View", this,
                             "initViewMenu");


        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);

        return menuBar;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public JComponent doMakeViewpointPanel()
            throws VisADException, RemoteException {


        JSlider speedSlider = new JSlider(1, 200, 200 - animationSpeed);
        speedSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                if ( !slider.getValueIsAdjusting()) {
                    animationSpeed = 200 - slider.getValue() + 1;
                }
            }
        });

        showTimesCbx = new JCheckBox("Set Animation Time", showTimes);
        showTimesCbx.setEnabled(hasTimes);
        animateCbx = new JCheckBox("Animated", animate);
        changeViewpointCbx = new JCheckBox("Change Viewpoint",
                                           changeViewpoint);


        orients = new Vector<TwoFacedObject>();
        orients.add(new TwoFacedObject("Towards Next Point", ORIENT_POINT));
        orients.add(new TwoFacedObject("Forward", ORIENT_FORWARD));
        orients.add(new TwoFacedObject("Up", ORIENT_UP));
        orients.add(new TwoFacedObject("Down", ORIENT_DOWN));
        orients.add(new TwoFacedObject("Left", ORIENT_LEFT));
        orients.add(new TwoFacedObject("Right", ORIENT_RIGHT));
        orientBox = new JComboBox(orients);
        orientBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                orientation =
                    TwoFacedObject.getIdString(orientBox.getSelectedItem());
                goToCurrent();
            }
        });




        if (animationInfo == null) {
            animationInfo = new AnimationInfo();
            animationInfo.setShareIndex(true);
        }


        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Misc.run(Flythrough.this, "goToCurrent");
            }
        };


        //xxxxx

        animationWidget = new AnimationWidget(null, null, animationInfo);

        if ((getShareGroup() == null)
                || getShareGroup().equals(SharableManager.GROUP_ALL)) {
            setShareGroup("flythrough");
        }
        animationWidget.setShareGroup(getShareGroup());
        animationWidget.setSharing(getSharing());


        animation = new Animation();
        animation.setAnimationInfo(animationInfo);
        animation.addPropertyChangeListener(this);
        animationWidget.setAnimation(animation);

        locationMarker =
            new SelectorPoint(
                "flythrough.point",
                ShapeUtility.setSize(
                    ShapeUtility.createShape(ShapeUtility.AIRPLANE3D)[0],
                    .1f), new RealTuple(
                        RealTupleType.SpatialCartesian3DTuple,
                        new double[] { 0,
                                       0, 0 }));

        locationMarker.setAutoSize(true);
        locationMarker.setManipulable(false);
        locationMarker.setColor(Color.green);
        locationMarker.setVisible(false);

        locationLine = new LineDrawing("flythroughpoint.line");
        locationLine.setVisible(false);
        locationLine.setLineWidth(2);
        locationLine.setColor(Color.green);

        //        locationLine2 = new LineDrawing("flythroughpoint.line2");
        //        locationLine2.setVisible(false);
        //        locationLine2.setLineWidth(2);
        //        locationLine2.setColor(Color.red);
        //        viewManager.getMaster().addDisplayable(locationLine2);

        Misc.runInABit(2000, this, "setScaleOnMarkers", null);

        viewManager.getMaster().addDisplayable(locationMarker);
        viewManager.getMaster().addDisplayable(locationLine);


        readout = new CursorReadoutWindow(viewManager, false);




        //zoomFld = new JTextField(zoom + "", 5);
        zoomFld = new JTextField(Misc.format(zoom), 5);

        for (int i = 0; i < tilt.length; i++) {
            final int theIndex = i;
            tiltSliders[i] = new JSlider(-90, 90, (int) tilt[i]);
            tiltSliders[i].setToolTipText("Control-R: reset");
            tiltSliders[i].addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ((e.getKeyCode() == KeyEvent.VK_R)
                            && e.isControlDown()) {
                        tilt[theIndex] = 0;
                        tiltLabels[theIndex].setText(""
                                + (int) tilt[theIndex]);
                        tiltSliders[theIndex].setValue(0);
                        goToCurrent();
                    }
                }
            });

            tiltLabels[i] = new JLabel("" + (int) tilt[i]);
            tiltSliders[i].addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    JSlider slider = (JSlider) e.getSource();
                    tiltLabels[theIndex].setText("" + slider.getValue());
                    if ( !slider.getValueIsAdjusting()) {
                        tilt[theIndex] = slider.getValue();
                        goToCurrent();
                    }
                }
            });
        }

        zoomFld.addActionListener(listener);

        fixedZCbx = GuiUtils.makeCheckbox("Use fixed Z", this, "useFixedZ");
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent orientationComp = GuiUtils.formLayout(new Component[] {
            GuiUtils.filler(), GuiUtils.left(changeViewpointCbx),
            GuiUtils.rLabel("Tilt Down/Up:"),
            GuiUtils.centerRight(tiltSliders[0], tiltLabels[0]),
            GuiUtils.rLabel("Tilt Left/Right:"),
            GuiUtils.centerRight(tiltSliders[1], tiltLabels[1]),
            GuiUtils.rLabel("Zoom:"), GuiUtils.left(zoomFld),
            GuiUtils.rLabel("Orientation:"),
            GuiUtils.left(GuiUtils.hbox(orientBox, (doGlobe()
                    ? GuiUtils.filler()
                    : (JComponent) fixedZCbx))),
            //            GuiUtils.rLabel("Tilt Down/Up:"), GuiUtils.centerRight(tiltSliders[2],tiltLabels[2]),

            GuiUtils.rLabel("Transitions:"),
            GuiUtils.vbox(GuiUtils.left(animateCbx),
                          GuiUtils.leftCenter(new JLabel("Speed"),
                              speedSlider)),
            GuiUtils.filler(), GuiUtils.left(showTimesCbx),
        });

        return GuiUtils.top(orientationComp);

    }


    /**
     * _more_
     */
    public void setScaleOnMarkers() {
        if ((viewManager != null) && (locationMarker != null)) {
            try {
                NavigatedDisplay navDisplay =
                    viewManager.getNavigatedDisplay();
                locationMarker.setScale((float) navDisplay.getDisplayScale());
            } catch (Exception exc) {
                logException("Setting scale on marker", exc);
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakePointsPanel() {

        pointTableModel = new FlythroughTableModel(this);
        pointTable      = new JTable(pointTableModel);
        pointTable.setToolTipText(
            "Double click: view; Control-P: Show point properties; Delete: delete point");


        pointTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_P) && e.isControlDown()) {
                    List<FlythroughPoint> newPoints =
                        new ArrayList<FlythroughPoint>();
                    int[]                 rows = pointTable.getSelectedRows();
                    List<FlythroughPoint> oldPoints = pointsToUse;
                    for (int j = 0; j < rows.length; j++) {
                        FlythroughPoint pt = oldPoints.get(rows[j]);
                        if ( !showProperties(pt)) {
                            break;
                        }
                        pointTable.repaint();
                    }
                }

                if (GuiUtils.isDeleteEvent(e)) {
                    List<FlythroughPoint> newPoints =
                        new ArrayList<FlythroughPoint>();
                    int[]                 rows = pointTable.getSelectedRows();
                    List<FlythroughPoint> oldPoints = pointsToUse;
                    for (int i = 0; i < oldPoints.size(); i++) {
                        boolean good = true;
                        for (int j = 0; j < rows.length; j++) {
                            if (i == rows[j]) {
                                good = false;
                                break;
                            }
                        }
                        if (good) {
                            newPoints.add(oldPoints.get(i));
                        }
                    }
                    flythrough(newPoints);
                }
            }
        });

        pointTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                final int row = pointTable.rowAtPoint(e.getPoint());
                if ((row < 0) || (row >= pointsToUse.size())) {
                    return;
                }
                if (e.getClickCount() > 1) {
                    animation.setCurrent(row);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(pointTable);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        return scrollPane;

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeValuesPanel() {
        readoutLabel = GuiUtils.getFixedWidthLabel("<html></html>");
        readoutLabel.setVerticalAlignment(SwingConstants.TOP);

        readoutDisplay = new JPanel();
        readoutDisplay.setLayout(new BorderLayout());
        return readoutLabel;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeDashboardPanel() {
        dashboardImage = GuiUtils.getImage("/auxdata/ui/icons/cockpit.gif",
                                           getClass(), false);
        pipPanel = new PipPanel(viewManager);
        pipPanel.getNavigatedPanel().setBorder(null);
        pipPanel.setPreferredSize(new Dimension(100, 100));
        pipPanel.doLayout();
        pipPanel.validate();
        pipFrame = new JFrame("Show remain invisible");
        pipFrame.setContentPane(pipPanel);
        pipFrame.pack();

        dashboardLbl = new JLabel(new ImageIcon(dashboardImage)) {
            public void paint(Graphics g) {
                //                readPts();
                paintDashboardBackground(g, dashboardLbl);
                super.paint(g);
                paintDashboardAfter(g, dashboardLbl);
            }
        };
        dashboardLbl.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                pipPanel.keyPressedInMap(e);
                updateDashboard();
            }
        });

        dashboardLbl.setVerticalAlignment(SwingConstants.BOTTOM);
        MouseAdapter mouseAdapter = new MouseAdapter() {
            Point mouseStart     = new Point(0, 0);
            Point originalOffset = new Point(0, 0);
            public void mouseClicked(MouseEvent me) {
                if (pipRect == null) {
                    return;
                }
                if (goToClick
                        && pipRect.contains(new Point(me.getX(),
                            me.getY()))) {
                    try {
                        int x = me.getX() - pipRect.x;
                        int y = me.getY() - pipRect.y;
                        //                        System.err.println ("x:" + x +" y:" + y);
                        LatLonPoint llp = pipPanel.screenToLatLon(x, y);
                        location = makePoint(llp.getLatitude(),
                                             llp.getLongitude(), 0);
                        doDrive(false, heading);
                    } catch (Exception exc) {
                        logException("Driving", exc);
                    }
                }
            }

            public void mousePressed(MouseEvent me) {
                dashboardLbl.requestFocus();
                originalOffset = new Point(dashboardImageOffset);
                mouseStart.x   = me.getX();
                mouseStart.y   = me.getY();
                Rectangle b  = dashboardLbl.getBounds();
                int       w  = dashboardImage.getWidth(null);
                int       h  = dashboardImage.getHeight(null);
                Point     ul = new Point(b.width / 2 - w / 2, b.height - h);
                //                System.out.println("{" + (me.getX()-ul.x) +",  " + (me.getY()-ul.y)+"}");
            }

            public void mouseDragged(MouseEvent me) {
                dashboardImageOffset.x = originalOffset.x + me.getX()
                                         - mouseStart.x;
                dashboardImageOffset.y = originalOffset.y + me.getY()
                                         - mouseStart.y;
                updateDashboard();
            }
        };

        dashboardLbl.addMouseListener(mouseAdapter);
        dashboardLbl.addMouseMotionListener(mouseAdapter);


        //Create ome charts to force classloading (which takes some time) in a thread
        //So the gui shows quicker
        Misc.run(new Runnable() {
            public void run() {
                try {
                    MeterPlot plot =
                        new MeterPlot(new DefaultValueDataset(new Double(1)));
                    createChart(new XYSeriesCollection());
                } catch (Exception ignore) {}
            }
        });


        return dashboardLbl;
    }


    /**
     * this is used to define a new set of location points  for the dials
     * It looks for a file "pts" in the working dir, if there it reads in the values.
     * the pts file looks like the inner of the dialPts  array, e.g.:
     * <pre>{ 402, 100, 206, 145 },
     *   { 256, 90, 120, 100 },
     *   { 224, 180, 170, 100 },
     * </pre>
     *
     * This lets you run the flythrough and edit the locations, save the file, and then you repaint
     * the dialog to show the new locations
     */
    private void readPts() {
        try {
            if (new File("pts").exists()) {
                String s = IOUtil.readContents("pts", "");
                s = s.replace("{", "");
                s = s.replace("}", "");
                List<int[]> pts = new ArrayList<int[]>();
                for (String line : StringUtil.split(s, "\n", true, true)) {
                    pts.add(Misc.parseInts(line, ","));
                }
                dialPts = new int[pts.size()][];
                for (int i = 0; i < pts.size(); i++) {
                    dialPts[i] = pts.get(i);
                }
            }


        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }



    /**
     * _more_
     *
     * @param c _more_
     */
    public void doRepaint(JComponent c) {
        boolean callRepaint = false;
        synchronized (REPAINT_MUTEX) {
            repaintCnt--;
            if (repaintCnt == 0) {
                callRepaint = true;
            }
        }
        if (callRepaint) {
            c.repaint();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public EarthLocation getLastLocation() {
        return lastLocation;
    }


    /**
     * _more_
     *
     * @param g _more_
     * @param comp _more_
     */
    public void paintDashboardAfter(Graphics g, JComponent comp) {

        Graphics2D      g2           = (Graphics2D) g;
        AffineTransform oldTransform = g2.getTransform();
        Rectangle       b            = dashboardLbl.getBounds();
        int             w            = dashboardImage.getWidth(null);
        int             h            = dashboardImage.getHeight(null);
        Point           ul = new Point(b.width / 2 - w / 2, b.height - h);
        int             ptsIdx       = 0;



        try {
            pipPanel.setPreferredSize(new Dimension(dialPts[ptsIdx][2],
                    dialPts[ptsIdx][3]));
            pipFrame.setSize(dialPts[ptsIdx][2], dialPts[ptsIdx][3]);
            pipPanel.doLayout();
            pipPanel.validate();
            pipFrame.pack();
            pipPanel.resetDrawBounds();
            pipPanel.redraw();
        } catch (Exception ignore) {}



        JLabel locLbl = null;


        if (lastLocation != null) {
            try {
                locLbl = new JLabel(
                    "<html><table width=100%><tr><td align=right>&nbsp;Lat:</td></td>"
                    + getIdv().getDisplayConventions().formatLatLon(
                        getLat(lastLocation)) + "</td></tr>"
                            + "<tr><td align=right>&nbsp;Lon:</td></td>"
                            + getIdv().getDisplayConventions().formatLatLon(
                                getLon(lastLocation)) + "</td></tr>"
                                    + "<tr><td align=right>&nbsp;Alt:</td></td>"
                                    + getIdv().getDisplayConventions().formatDistance(
                                        getAlt(lastLocation)) + "</table>");
            } catch (Exception ignore) {}
        }
        if (locLbl == null) {
            locLbl = new JLabel(
                "<html><table width=100%><tr><td align=right>&nbsp;Lat:</td></td>N/A </td></tr><tr><td align=right>&nbsp;Lon:</td></td>N/A </td></tr><tr><td align=right>&nbsp;Alt:</td></td>N/A </table>");
        }
        locLbl.setOpaque(true);
        locLbl.setBackground(Color.white);


        DefaultValueDataset headingDataset =
            new DefaultValueDataset(new Double(currentHeading));

        CompassPlot plot = new CompassPlot(headingDataset);
        plot.setSeriesNeedle(0);
        plot.setSeriesPaint(0, Color.red);
        plot.setSeriesOutlinePaint(0, Color.red);
        JFreeChart chart        = new JFreeChart("", plot);
        ChartPanel compassPanel = new ChartPanel(chart);

        plot.setBackgroundPaint(new Color(255, 255, 255, 0));
        plot.setBackgroundImageAlpha(0.0f);
        chart.setBackgroundPaint(new Color(255, 255, 255, 0));
        compassPanel.setBackground(new Color(255, 255, 255, 0));
        compassPanel.setPreferredSize(dialDimension);
        //        compassPanel.setSize(new Dimension(100,100));


        g2.setTransform(oldTransform);
        pipRect = drawDial(g2, pipPanel, ptsIdx++, ul);


        JFrame dummyFrame = new JFrame("");
        dummyFrame.setContentPane(compassPanel);
        dummyFrame.pack();

        g2.setTransform(oldTransform);
        drawDial(g2, compassPanel, ptsIdx++, ul);


        g2.setTransform(oldTransform);
        dummyFrame.setContentPane(locLbl);
        dummyFrame.pack();
        drawDial(g2, locLbl, ptsIdx++, ul);


        if (showReadout) {
            for (JComponent dial : dials) {
                dummyFrame.setContentPane(dial);
                dummyFrame.pack();

                g2.setTransform(oldTransform);
                drawDial(g2, dial, ptsIdx++, ul);
            }
        }

        g2.setTransform(oldTransform);

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Image getDashboardImage() {
        return dashboardImage;
    }



    /**
     * Creates a chart.
     *
     * @param dataset  the data for the chart.
     *
     * @return a chart.
     */
    public static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart("",  // chart title
            "",                                                // x axis label
            "",                                                // y axis label
            dataset,                                           // data
            PlotOrientation.VERTICAL, true,                    // include legend
            true,                                              // tooltips
            false                                              // urls
                );
        chart.setBackgroundPaint(new Color(255, 255, 255, 0));
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        //        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        Axis domainAxis = plot.getDomainAxis();
        domainAxis.setVisible(false);
        return chart;
    }



    /**
     * _more_
     *
     * @param g2 _more_
     * @param comp _more_
     * @param ptsIdx _more_
     * @param ul _more_
     *
     * @return _more_
     */
    private Rectangle drawDial(Graphics g2, JComponent comp, int ptsIdx,
                               Point ul) {
        if (ptsIdx >= dialPts.length) {
            return null;
        }
        int     w            = comp.getWidth();
        int     h            = comp.getHeight();
        boolean useUpperLeft = false;
        if (comp instanceof ChartPanel) {
            int    desiredWidth = dialPts[ptsIdx][2];
            double scale        = w / (double) desiredWidth;
            if (scale != 0) {
                h = (int) (h / scale);
            }
            comp.setSize(new Dimension(desiredWidth, h));
        } else if (comp instanceof JLabel) {
            //Don't set size for labels
            useUpperLeft = true;
        } else {
            comp.setSize(new Dimension(dialPts[ptsIdx][2],
                                       dialPts[ptsIdx][3]));
        }
        try {
            int   x     = ul.x + dialPts[ptsIdx][0] - (useUpperLeft
                    ? 0
                    : dialPts[ptsIdx][2] / 2);
            int   y     = ul.y + dialPts[ptsIdx][1] - (useUpperLeft
                    ? 0
                    : dialPts[ptsIdx][3] / 2);
            Image image = ImageUtils.getImage(comp);
            g2.translate(x, y);
            g2.drawImage(image, 0, 0, null);
            return new Rectangle(x, y, comp.getWidth(), comp.getHeight());

        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return null;


    }


    /**
     * _more_
     *
     * @param g _more_
     * @param comp _more_
     */
    public void paintDashboardBackground(Graphics g, JComponent comp) {
        Graphics2D g2 = (Graphics2D) g;
        Rectangle  b  = dashboardLbl.getBounds();
        g.setColor(Color.white);
        g.fillRect(0, 0, b.width, b.height);

        if (showDecoration) {
            boolean callRepaint = false;
            for (FlythroughDecorator decorator : decorators) {
                if ( !decorator.getShown()) {
                    continue;
                }
                if (decorator.paintDashboard(g2, comp)) {
                    callRepaint = true;
                }
            }
            if (callRepaint) {
                synchronized (REPAINT_MUTEX) {
                    repaintCnt++;
                    Misc.runInABit(500, this, "doRepaint", comp);
                }
            }

        }

    }





    /**
     * _more_
     *
     * @param event _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void displayChanged(DisplayEvent event)
            throws VisADException, RemoteException {
        if ( !isActive()) {
            return;
        }

        double[] viewpoint = viewManager.getDisplayMatrix();
        if ( !Misc.arraysEquals(viewpoint, lastViewpoint)) {
            lastViewpoint = viewpoint;
            updateDashboard();
        }

        if (goToClick && (event.getId() == DisplayEvent.MOUSE_PRESSED)) {
            InputEvent inputEvent = event.getInputEvent();
            int        mods       = inputEvent.getModifiers();
            if ((mods & InputEvent.BUTTON1_MASK) != 0
                    && !inputEvent.isShiftDown()
                    && !inputEvent.isControlDown()) {
                NavigatedDisplay navDisplay =
                    viewManager.getNavigatedDisplay();
                location = navDisplay.screenToEarthLocation(event.getX(),
                        event.getY());
                doDrive(false, heading);
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeNavigationPanel() {
        earthNavPanel = new EarthNavPanel(viewManager, this, false);
        return earthNavPanel;
    }


    /**
     * _more_
     */
    public void driveLeft() {
        heading -= 3;
        doDrive(false, heading);
    }

    /**
     * _more_
     */
    public void driveRight() {
        heading += 3;
        doDrive(false, heading);
    }


    /**
     * _more_
     */
    public void driveForward() {
        doDrive(true, heading);
    }


    /**
     * _more_
     */
    public void driveBack() {
        doDrive(true, heading - 180, false);
    }




    /**
     * _more_
     *
     * @param takeStep _more_
     * @param heading _more_
     */
    public void doDrive(boolean takeStep, double heading) {
        doDrive(takeStep, heading, true);
    }

    /**
     * _more_
     *
     * @param takeStep _more_
     * @param heading _more_
     * @param forward _more_
     */
    public void doDrive(boolean takeStep, double heading, boolean forward) {
        lastMoveWasTrack = false;
        try {
            NavigatedDisplay navDisplay = viewManager.getNavigatedDisplay();
            if (location == null) {
                if (pointsToUse.size() > 0) {
                    location = pointsToUse.get(0).getEarthLocation();
                } else {
                    if (doGlobe()) {
                        location = makePoint(40, -100, 0);
                    } else {
                        location = navDisplay.getEarthLocation(
                            navDisplay.getScreenCenter());
                    }
                }
            }

            double          zoom         = navDisplay.getScale();
            LatLonPointImpl llp          = null;
            EarthLocation   nextLocation = null;
            double[]        xyz1         = { 0, 0, 0 };
            double[]        xyz2         = { 0, 0, 0 };
            double[]        xyz          = getXYZ(location);

            EarthLocation sll =
                navDisplay.getEarthLocation(navDisplay.getScreenUpperLeft());
            EarthLocation slr =
                navDisplay.getEarthLocation(navDisplay.getScreenLowerRight());
            Bearing bearing =
                Bearing.calculateBearing(new LatLonPointImpl(getLat(sll),
                    getLon(sll)), new LatLonPointImpl(getLat(slr),
                        getLon(slr)), null);
            double distance        = bearing.getDistance();
            double distancePerStep = distance / 30;
            if (distancePerStep != distancePerStep) {
                distancePerStep = 100;
            }


            if (true || doGlobe()) {
                llp = Bearing.findPoint(new LatLonPointImpl(getLat(location),
                        getLon(location)), heading, distancePerStep, null);

                nextLocation = makePoint(llp.getLatitude(),
                                         llp.getLongitude(), 0);
            } else {}

            if (takeStep) {
                location = nextLocation;
                llp = Bearing.findPoint(new LatLonPointImpl(getLat(location),
                        getLon(location)), heading, 100, null);
                nextLocation = makePoint(llp.getLatitude(),
                                         llp.getLongitude(), 0);
            }



            //            System.err.println (location +" " + nextLocation);

            xyz1 = getXYZ(location);
            xyz2 = getXYZ(nextLocation);
            if ( !forward) {
                double[] xyz3 = { xyz1[0] - (xyz2[0] - xyz1[0]),
                                  xyz1[1] - (xyz2[1] - xyz1[1]),
                                  xyz1[2] - (xyz2[2] - xyz1[2]) };
                xyz1 = xyz1;
                xyz2 = xyz3;
                location = navDisplay.getEarthLocation(xyz1[0], xyz1[1],
                        xyz1[2], false);
            }


            goTo(new FlythroughPoint(location), xyz1, xyz2, null, false);
        } catch (Exception exc) {
            logException("Driving", exc);
        }
    }


    /**
     * _more_
     *
     * @param location _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public double getLat(EarthLocation location) throws VisADException {
        return location.getLatitude().getValue(CommonUnit.degree);
    }

    /**
     * _more_
     *
     * @param location _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public double getLon(EarthLocation location) throws VisADException {
        return location.getLongitude().getValue(CommonUnit.degree);
    }


    /**
     * _more_
     *
     * @param location _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public double getAlt(EarthLocation location) throws VisADException {
        Real r = location.getAltitude();
        if (r == null) {
            return 0;
        }
        return r.getValue(CommonUnit.meter);
    }






    /**
     * _more_
     */
    public void clearPoints() {
        flythrough(new ArrayList<FlythroughPoint>());
    }

    /**
     * _more_
     *
     * @param editMenu _more_
     */
    public void initEditMenu(JMenu editMenu) {
        editMenu.add(GuiUtils.makeMenuItem("Add Point", this,
                                           "addPointWithoutTime"));
        editMenu.add(GuiUtils.makeMenuItem("Add Point with Time", this,
                                           "addPointWithTime"));
        editMenu.add(GuiUtils.makeMenuItem("Fly along a latitude", this,
                                           "flyAlongLatitude"));
        editMenu.add(GuiUtils.makeMenuItem("Fly along a longitude", this,
                                           "flyAlongLongitude"));

        int cnt = pointsToUse.size();
        if (cnt > 0) {
            int[] strides    = {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 50, 75, 100
            };
            JMenu strideMenu = new JMenu("Stride");
            editMenu.add(strideMenu);
            editMenu.add(GuiUtils.makeMenuItem("Remove all points (#" + cnt
                    + ")", this, "clearPoints"));
            for (int s : strides) {
                final int theStride = s;
                JMenuItem mi        = new JMenuItem(((s == 1)
                        ? "All points"
                        : "Every " + s + " points") + "  " + ((s == stride)
                        ? "(current)"
                        : ""));
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        stride = theStride;
                        flythrough(allPoints);
                    }
                });
                strideMenu.add(mi);
            }

        }

        for (FlythroughDecorator decorator : decorators) {
            decorator.initEditMenu(editMenu);
        }


        editMenu.addSeparator();
        editMenu.add(GuiUtils.makeCheckboxMenuItem("Sharing On", this,
                "sharing", null));

        editMenu.add(GuiUtils.makeMenuItem("Set Share Group", this,
                                           "showSharableDialog"));

    }


    /**
     * _more_
     *
     * @param fileMenu _more_
     */
    public void initFileMenu(JMenu fileMenu) {
        fileMenu.add(GuiUtils.makeMenuItem("Export", this, "doExport"));
        fileMenu.add(GuiUtils.makeMenuItem("Import", this, "doImport"));
        for (FlythroughDecorator decorator : decorators) {
            decorator.initFileMenu(fileMenu);
        }

    }


    /**
     * _more_
     *
     * @param viewMenu _more_
     */
    public void initViewMenu(JMenu viewMenu) {
        JMenu dashboardMenu = new JMenu("Dashboard");
        viewMenu.add(dashboardMenu);
        viewMenu.add(GuiUtils.makeCheckboxMenuItem("Go to mouse click", this,
                "goToClick", null));
        viewMenu.add(GuiUtils.makeCheckboxMenuItem("Show animation", this,
                "showAnimation", null));
        viewMenu.add(GuiUtils.makeCheckboxMenuItem("Show line", this,
                "showLine", null));
        viewMenu.add(GuiUtils.makeCheckboxMenuItem("Show marker", this,
                "showMarker", null));


        for (FlythroughDecorator decorator : decorators) {
            decorator.initViewMenu(dashboardMenu);
        }


        dashboardMenu.add(GuiUtils.makeCheckboxMenuItem("Show gauges", this,
                "showReadout", null));

        dashboardMenu.add(GuiUtils.makeMenuItem("Clear data", this,
                "clearSamples"));
    }

    /**
     * _more_
     */
    public void clearSamples() {
        for (FlythroughDecorator decorator : decorators) {
            decorator.clearSamples();
        }
        updateDashboard();
    }


    /**
     * _more_
     *
     * @param sharing _more_
     */
    public void setSharing(boolean sharing) {
        super.setSharing(sharing);
        if (animationWidget != null) {
            animationWidget.setSharing(sharing);
            animationInfo.setShared(true);
        }
    }

    /**
     * _more_
     *
     * @param shareGroup _more_
     */
    public void setShareGroup(Object shareGroup) {
        super.setShareGroup(shareGroup);
        if (animationWidget != null) {
            animationWidget.setShareGroup(shareGroup);
        }
    }



    /**
     * _more_
     *
     * @param pt _more_
     *
     * @return _more_
     */
    private boolean showProperties(FlythroughPoint pt) {
        try {
            DateTime[] times     =
                viewManager.getAnimationWidget().getTimes();
            JComboBox  timeBox   = null;
            JLabel     timeLabel = GuiUtils.rLabel("Time:");
            Vector     timesList = new Vector();
            timesList.add(0, new TwoFacedObject("None", null));
            if ((times != null) && (times.length > 0)) {
                timesList.addAll(Misc.toList(times));
            }
            if ((pt.getDateTime() != null)
                    && !timesList.contains(pt.getDateTime())) {
                timesList.add(pt.getDateTime());
            }
            timeBox = new JComboBox(timesList);
            if (pt.getDateTime() != null) {
                timeBox.setSelectedItem(pt.getDateTime());
            }

            LatLonWidget llw = new LatLonWidget("Latitude: ", "Longitude: ",
                                   "Altitude: ", null) {
                protected String formatLatLonString(String latOrLon) {
                    return latOrLon;
                }
            };

            EarthLocation el = pt.getEarthLocation();
            llw.setLatLon(el.getLatitude().getValue(CommonUnit.degree),
                          el.getLongitude().getValue(CommonUnit.degree));
            llw.setAlt(getAlt(el));

            JTextArea textArea = new JTextArea("", 5, 100);
            if (pt.getDescription() != null) {
                textArea.setText(pt.getDescription());
            }
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));
            JComponent contents = GuiUtils.formLayout(new Component[] {
                GuiUtils.rLabel("Location:"), llw, timeLabel,
                GuiUtils.left(timeBox), GuiUtils.rLabel("Description:"),
                scrollPane
            });
            if ( !GuiUtils.showOkCancelDialog(frame, "Point Properties",
                    contents, null)) {
                return false;
            }
            pt.setDescription(textArea.getText());
            pt.setEarthLocation(makePoint(llw.getLat(), llw.getLon(),
                                          llw.getAlt()));
            Object selectedDate = timeBox.getSelectedItem();
            if (selectedDate instanceof TwoFacedObject) {
                pt.setDateTime(null);
            } else {
                pt.setDateTime((DateTime) selectedDate);
            }


            return true;
        } catch (Exception exc) {
            logException("Showing point properties", exc);
            return false;
        }

    }



    /**
     * _more_
     *
     *
     * @param force _more_
     * @throws Exception _more_
     */
    private synchronized void doMakeContents(boolean force) throws Exception {

        if ( !force && (contents != null)) {
            return;
        }

        if (contents == null) {
            makeWidgets();
        }

        animation.setAnimationInfo(animationInfo);
        showTimesCbx.setSelected(showTimes);
        showTimesCbx.setEnabled(hasTimes);
        animateCbx.setSelected(animate);
        changeViewpointCbx.setSelected(changeViewpoint);

        orientBox.setSelectedItem(TwoFacedObject.findId(orientation,
                orients));
        //zoomFld.setText(zoom + "");
        zoomFld.setText(Misc.format(zoom));
        for (int i = 0; i < tilt.length; i++) {
            tiltSliders[i].setValue((int) tilt[i]);
            tiltLabels[i].setText("" + (int) tilt[i]);
        }

        setAnimationTimes();
        boolean hadFrame = true;
        if (frame == null) {
            frame = new JFrame(GuiUtils.getApplicationTitle() + "Flythrough");
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    if (locationLine != null) {
                        try {
                            locationLine.setVisible(false);
                            locationMarker.setVisible(false);
                        } catch (Exception ignore) {}
                    }
                }

                public void windowDeiconified(WindowEvent e) {
                    System.err.println("window");
                    if (locationLine != null) {
                        try {
                            locationLine.setVisible(showLine);
                            locationMarker.setVisible(showMarker);
                        } catch (Exception ignore) {}
                    }
                }
            });
            frame.setIconImage(
                GuiUtils.getImage("/auxdata/ui/icons/plane.png"));
            hadFrame = false;
        }
        frame.getContentPane().removeAll();
        frame.getContentPane().add(contents);
        frame.pack();
        if (windowBounds != null) {
            //            frame.setSize(windowBounds.width, windowBounds.height);
            frame.setLocation(windowBounds.x, windowBounds.y);
        }

        GuiUtils.toFront(frame);

    }




    /**
     * _more_
     *
     * @param latitude _more_
     * @param longitude _more_
     * @param alt _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private EarthLocation makePoint(String latitude, String longitude,
                                    String alt)
            throws VisADException, RemoteException {
        return makePoint(((latitude == null)
                          ? 0
                          : new Double(
                              latitude.trim()).doubleValue()), ((longitude
                                  == null)
                ? 0
                : new Double(longitude.trim()).doubleValue()), ((alt == null)
                ? 0
                : new Double(alt.trim()).doubleValue()));
    }



    /**
     * _more_
     *
     * @param latitude _more_
     * @param longitude _more_
     * @param alt _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private EarthLocation makePoint(double latitude, double longitude,
                                    double alt)
            throws VisADException, RemoteException {
        Real altReal = new Real(RealType.Altitude, alt);
        return new EarthLocationLite(new Real(RealType.Latitude, latitude),
                                     new Real(RealType.Longitude, longitude),
                                     altReal);
    }


    /**
     * _more_
     *
     * @param root _more_
     */
    private void importKml(Element root) {
        try {
            List tourNodes = XmlUtil.findDescendants(root, KmlUtil.TAG_TOUR);
            if (tourNodes.size() == 0) {
                LogUtil.userMessage("Could not find any tours");
                return;
            }
            Element               tourNode  = (Element) tourNodes.get(0);
            List<FlythroughPoint> thePoints =
                new ArrayList<FlythroughPoint>();
            Element playListNode = XmlUtil.findChild(tourNode,
                                       KmlUtil.TAG_PLAYLIST);
            if (playListNode == null) {
                LogUtil.userMessage("Could not find playlist");
                return;
            }

            NodeList elements = XmlUtil.getElements(playListNode);
            for (int i = 0; i < elements.getLength(); i++) {
                Element child = (Element) elements.item(i);
                if (child.getTagName().equals(KmlUtil.TAG_FLYTO)) {
                    Element cameraNode = XmlUtil.findChild(child,
                                             KmlUtil.TAG_CAMERA);
                    /*        <Camera>
                              <longitude>170.157</longitude>
                              <latitude>-43.671</latitude>
                              <altitude>9700</altitude>
                              <heading>-6.333</heading>
                              <tilt>33.5</tilt>
                              </Camera>*/
                    if (cameraNode == null) {
                        cameraNode = XmlUtil.findChild(child,
                                KmlUtil.TAG_LOOKAT);
                    }

                    if (cameraNode == null) {
                        //                        System.err.println ("no camera:" + XmlUtil.toString(child));
                        continue;
                    }
                    FlythroughPoint pt =
                        new FlythroughPoint(makePoint(XmlUtil
                            .getGrandChildText(cameraNode,
                                KmlUtil.TAG_LATITUDE), XmlUtil
                                    .getGrandChildText(cameraNode,
                                        KmlUtil.TAG_LONGITUDE), XmlUtil
                                            .getGrandChildText(cameraNode,
                                                KmlUtil.TAG_ALTITUDE)));

                    pt.setTiltX(
                        -new Double(
                            XmlUtil.getGrandChildText(
                                cameraNode, KmlUtil.TAG_TILT,
                                "0")).doubleValue());
                    pt.setTiltY(
                        new Double(
                            XmlUtil.getGrandChildText(
                                cameraNode, KmlUtil.TAG_HEADING,
                                "0")).doubleValue());
                    pt.setTiltZ(
                        new Double(
                            XmlUtil.getGrandChildText(
                                cameraNode, KmlUtil.TAG_ROLL,
                                "0")).doubleValue());

                    thePoints.add(pt);

                } else if (child.getTagName().equals(KmlUtil.TAG_WAIT)) {}
                else {}

            }


            flythrough(thePoints);
            doMakeContents(true);
            setAnimationTimes();
        } catch (Exception exc) {
            logException("Importing kml", exc);
        }
    }

    /**
     * _more_
     */
    public void doImport() {
        try {
            String filename = FileManager.getReadFile(FileManager.FILTER_XML);
            if (filename == null) {
                return;
            }
            Element root = XmlUtil.getRoot(filename, getClass());
            if (root.getTagName().equals(KmlUtil.TAG_KML)) {
                importKml(root);
                return;
            }


            if ( !root.getTagName().equals(TAG_FLYTHROUGH)) {
                throw new IllegalStateException("Unknown tag:"
                        + root.getTagName());
            }
            for (int i = 0; i < tilt.length; i++) {
                tilt[i] = XmlUtil.getAttribute(root, ATTR_TILT[i], tilt[i]);
            }
            zoom = XmlUtil.getAttribute(root, ATTR_ZOOM, zoom);

            List<FlythroughPoint> thePoints =
                new ArrayList<FlythroughPoint>();
            NodeList elements = XmlUtil.getElements(root);
            for (int i = 0; i < elements.getLength(); i++) {
                Element child = (Element) elements.item(i);

                if ( !child.getTagName().equals(TAG_POINT)) {
                    throw new IllegalStateException("Unknown tag:"
                            + child.getTagName());
                }
                FlythroughPoint pt = new FlythroughPoint();
                pt.setDescription(XmlUtil.getGrandChildText(child,
                        TAG_DESCRIPTION));

                pt.setEarthLocation(makePoint(XmlUtil.getAttribute(child,
                        ATTR_LAT, 0.0), XmlUtil.getAttribute(child, ATTR_LON,
                            0.0), XmlUtil.getAttribute(child, ATTR_ALT,
                                0.0)));

                if (XmlUtil.hasAttribute(child, ATTR_DATE)) {
                    pt.setDateTime(parseDate(XmlUtil.getAttribute(child,
                            ATTR_DATE)));
                }
                pt.setTiltX(XmlUtil.getAttribute(child, ATTR_TILT[0],
                        Double.NaN));
                pt.setTiltY(XmlUtil.getAttribute(child, ATTR_TILT[1],
                        Double.NaN));
                pt.setTiltZ(XmlUtil.getAttribute(child, ATTR_TILT[2],
                        Double.NaN));
                pt.setZoom(XmlUtil.getAttribute(child, ATTR_ZOOM,
                        Double.NaN));
                String matrixS = XmlUtil.getAttribute(child, ATTR_MATRIX,
                                     (String) null);
                if (matrixS != null) {
                    List<String> toks =
                        (List<String>) StringUtil.split(matrixS, ",", true,
                            true);
                    double[] m = new double[toks.size()];
                    for (int tokIdx = 0; tokIdx < m.length; tokIdx++) {
                        m[tokIdx] =
                            new Double(toks.get(tokIdx)).doubleValue();
                    }
                    pt.setMatrix(m);
                }
                thePoints.add(pt);
            }
            flythrough(thePoints);
            doMakeContents(true);
            setAnimationTimes();
        } catch (Exception exc) {
            logException("Initializing flythrough", exc);
        }
    }

    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String formatDate(DateTime dttm) throws Exception {
        return sdf.format(ucar.visad.Util.makeDate(dttm));
    }


    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private DateTime parseDate(String dttm) throws Exception {
        return new DateTime(sdf.parse(dttm));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public FlythroughPoint addPointWithoutTime() {
        return addPoint(false);
    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void flyAlongLatitude() throws VisADException, RemoteException {
        double value = 0;
        String s     = "0.0";
        while (true) {
            s = GuiUtils.getInput(
                "Please enter a latitude between -90 and 90", "Latitude: ",
                s);
            if (s == null) {
                return;
            }
            try {
                value = Misc.parseDouble(s.trim());
                break;
            } catch (Exception exc) {
                GuiUtils.showOkDialog(
                    null, "Oops",
                    new JLabel("Please enter a value between -90 and 90"),
                    null);
            }
        }

        List<FlythroughPoint> pts = new ArrayList<FlythroughPoint>();
        for (double other = 0; other < 360; other++) {
            EarthLocationTuple pt = new EarthLocationTuple(value, other, 0);
            pts.add(new FlythroughPoint(pt));
        }
        flythrough(pts);
    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void flyAlongLongitude() throws VisADException, RemoteException {
        double value = 0;
        String s     = "0.0";
        while (true) {
            s = GuiUtils.getInput(
                "Please enter a longitude between -180 and 180",
                "Longitude: ", s);
            if (s == null) {
                return;
            }
            try {
                value = Misc.parseDouble(s.trim());
                break;
            } catch (Exception exc) {
                GuiUtils.showOkDialog(
                    null, "Oops",
                    new JLabel("Please enter a value between -90 and 90"),
                    null);
            }
        }

        List<FlythroughPoint> pts = new ArrayList<FlythroughPoint>();
        for (double other = 90; other >= -90; other--) {
            EarthLocationTuple pt = new EarthLocationTuple(other, value, 0);
            pts.add(new FlythroughPoint(pt));
        }
        /*
        for(double other=-89;other<=90;other++) {
                EarthLocationTuple pt =
                    new EarthLocationTuple(other, value, 0);
                pts.add(new FlythroughPoint(pt));
        }
        */
        flythrough(pts);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public FlythroughPoint addPointWithTime() {
        return addPoint(true);
    }


    /**
     * _more_
     *
     * @param includeTime _more_
     *
     * @return _more_
     */
    public FlythroughPoint addPoint(boolean includeTime) {
        try {
            FlythroughPoint pt = new FlythroughPoint();
            pt.setEarthLocation(makePoint(0, 0, 0));
            NavigatedDisplay navDisplay    =
                viewManager.getNavigatedDisplay();
            double[]         currentMatrix = navDisplay.getProjectionMatrix();
            if (includeTime) {
                Real dttm =
                    viewManager.getAnimation().getCurrentAnimationValue();
                if (dttm != null) {
                    pt.setDateTime(new DateTime(dttm));
                }
            }
            pt.setMatrix(currentMatrix);
            allPoints.add(pt);
            flythrough(allPoints);
            return pt;
        } catch (Exception exc) {
            logException("Adding point", exc);
            return null;
        }
    }

    /**
     * _more_
     */
    public void doExport() {
        try {
            String filename =
                FileManager.getWriteFile(FileManager.FILTER_XML,
                                         FileManager.SUFFIX_XML);
            if (filename == null) {
                return;
            }

            Document doc  = XmlUtil.makeDocument();
            Element  root = doc.createElement(TAG_FLYTHROUGH);
            for (int i = 0; i < tilt.length; i++) {
                root.setAttribute(ATTR_TILT[i], "" + tilt[i]);
            }
            root.setAttribute(ATTR_ZOOM, "" + getZoom());

            List<FlythroughPoint> thePoints = this.pointsToUse;
            for (FlythroughPoint pt : thePoints) {
                Element ptNode = XmlUtil.create(TAG_POINT, root);

                if (pt.getDescription() != null) {
                    XmlUtil.create(root.getOwnerDocument(), TAG_DESCRIPTION,
                                   ptNode, pt.getDescription());
                }

                EarthLocation el = pt.getEarthLocation();
                if (pt.getDateTime() != null) {
                    ptNode.setAttribute(ATTR_DATE,
                                        formatDate(pt.getDateTime()));
                }
                ptNode.setAttribute(
                    ATTR_LAT,
                    "" + el.getLatitude().getValue(CommonUnit.degree));
                ptNode.setAttribute(
                    ATTR_LON,
                    "" + el.getLongitude().getValue(CommonUnit.degree));
                ptNode.setAttribute(ATTR_ALT, "" + getAlt(el));
                if (pt.hasTiltX()) {
                    ptNode.setAttribute(ATTR_TILT[0], "" + pt.getTiltX());
                }
                if (pt.hasTiltY()) {
                    ptNode.setAttribute(ATTR_TILT[1], "" + pt.getTiltY());
                }
                if (pt.hasTiltZ()) {
                    ptNode.setAttribute(ATTR_TILT[2], "" + pt.getTiltZ());
                }
                if (pt.hasZoom()) {
                    ptNode.setAttribute(ATTR_ZOOM, "" + pt.getZoom());
                }

                double[] m = pt.getMatrix();
                if (m != null) {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < m.length; i++) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        sb.append(m[i]);
                    }
                    ptNode.setAttribute(ATTR_MATRIX, sb.toString());
                }

            }
            String xml = XmlUtil.toString(root);
            IOUtil.writeFile(filename, xml);
        } catch (Exception exc) {
            logException("Exporting flythrough", exc);
        }

    }

    /**
     * _more_
     */
    public void show() {
        if (frame == null) {
            try {
                doMakeContents(false);
            } catch (Exception exc) {
                logException("Showing flythrough", exc);
            }
        }
        setAnimationTimes();
        if (frame != null) {
            frame.show();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isActive() {
        if ((frame == null) || (viewManager == null)) {
            return false;
        }
        return frame.isShowing();
    }

    /**
     * _more_
     *
     * @param el _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private double[] getXYZ(EarthLocation el)
            throws VisADException, RemoteException {
        NavigatedDisplay navDisplay = viewManager.getNavigatedDisplay();
        double           alt        = getAlt(el);
        if (doGlobe() && (alt == 0)) {
            alt = 100;
        }
        return navDisplay.getSpatialCoordinates(el, null, alt);
    }



    /**
     * fly to the given point
     *
     * @param index _more_
     *
     * @throws Exception _more_
     */
    private void doStep(int index) throws Exception {
        if (index < lastIndex) {
            clearSamples();
        }
        lastIndex        = index;
        lastMoveWasTrack = true;


        if ((pointsToUse.size() == 0) || !isActive()) {
            return;
        }

        if (viewManager == null) {
            return;
        }

        List<FlythroughPoint> thePoints   = this.pointsToUse;

        NavigatedDisplay      navDisplay  = viewManager.getNavigatedDisplay();

        double[]              xyz1        = { 0, 0, 0 };
        double[]              xyz2        = { 0, 0, 0 };
        double[]              actualPoint = { 0, 0, 0 };


        if (index >= thePoints.size()) {
            index = 0;
        } else if (index < 0) {
            index = thePoints.size() - 1;
        }
        if (pointTable != null) {
            pointTable.getSelectionModel().setSelectionInterval(index, index);
            pointTable.repaint();
        }

        int     index1 = index;
        int     index2 = index + 1;
        boolean atEnd  = false;
        if (index2 >= thePoints.size()) {
            index2 = 0;
            atEnd  = true;
        }

        FlythroughPoint pt1 = thePoints.get(index1);
        FlythroughPoint pt2 = thePoints.get(index2);

        xyz1        = getXYZ(pt1.getEarthLocation());
        xyz2        = getXYZ(pt2.getEarthLocation());
        actualPoint = xyz2;

        if (orientation.equals(ORIENT_FORWARD)) {
            //Average the next N points
            int      ptCnt = 0;
            double[] sum   = { 0, 0, 0 };
            double[] tmp   = { 0, 0, 0 };
            int      max   = 10;
            for (int ptIdx = index2;
                    (ptIdx < thePoints.size()) && (ptCnt < max); ptIdx++) {
                tmp = getXYZ(thePoints.get(ptIdx).getEarthLocation());
                if ((tmp[0] != tmp[0]) || (tmp[1] != tmp[1])
                        || (tmp[2] != tmp[2])) {
                    continue;
                }
                ptCnt++;
                //Divide by the cnt so closer points have more affect
                sum[0] += tmp[0];
                sum[1] += tmp[1];
                sum[2] += tmp[2];
            }
            xyz2 = new double[] { sum[0] / ptCnt, sum[1] / ptCnt,
                                  sum[2] / ptCnt };
        }




        float x1 = (float) xyz1[0];
        float y1 = (float) xyz1[1];
        float z1 = (float) xyz1[2];


        if (atEnd && (thePoints.size() > 1) && (index1 > 0)) {
            FlythroughPoint prevPt = thePoints.get(index1 - 1);

            double[]        xyz3   = getXYZ(prevPt.getEarthLocation());
            xyz2 = new double[] { x1 + (x1 - xyz3[0]), y1 + (y1 - xyz3[1]),
                                  z1 + (z1 - xyz3[2]) };
            actualPoint = xyz2;
        }


        goTo(pt1, xyz1, xyz2, actualPoint, getAnimate());
    }


    /**
     * _more_
     *
     * @param pt1 _more_
     * @param xyz1 _more_
     * @param xyz2 _more_
     * @param actualPoint _more_
     * @param animateMove _more_
     */
    protected void goTo(FlythroughPoint pt1, double[] xyz1, double[] xyz2,
                        double[] actualPoint, boolean animateMove) {


        currentHeading = 180;
        if (actualPoint == null) {
            actualPoint = xyz2;
        }
        NavigatedDisplay navDisplay    = viewManager.getNavigatedDisplay();
        MouseBehavior    mouseBehavior = navDisplay.getMouseBehavior();
        double[]         currentMatrix = navDisplay.getProjectionMatrix();
        double[]         aspect        = navDisplay.getDisplayAspect();
        try {

            if (pt1.getDescription() != null) {
                getHtmlView().setText(pt1.getDescription());
            } else {
                getHtmlView().setText("");
            }

            processReadout(pt1);

            float  x1   = (float) xyz1[0];
            float  y1   = (float) xyz1[1];
            float  z1   = (float) xyz1[2];

            float  x2   = (float) xyz2[0];
            float  y2   = (float) xyz2[1];
            float  z2   = (float) xyz2[2];

            double zoom = (pt1.hasZoom()
                           ? pt1.getZoom()
                           : getZoom());
            if (zoom == 0) {
                zoom = 0.1;
            }

            double tiltx = (pt1.hasTiltX()
                            ? pt1.getTiltX()
                            : tilt[0]);
            double tilty = (pt1.hasTiltY()
                            ? pt1.getTiltY()
                            : tilt[1]);
            double tiltz = (pt1.hasTiltZ()
                            ? pt1.getTiltZ()
                            : tilt[2]);




            //Check for nans
            if ((x2 != x2) || (y2 != y2) || (z2 != z2)) {
                return;
            }

            if ((x1 != x1) || (y1 != y1) || (z1 != z1)) {
                return;
            }

            double[] m = pt1.getMatrix();
            if (m == null) {
                m = new double[16];

                Transform3D t = new Transform3D();
                if (orientation.equals(ORIENT_UP)) {
                    y2 = y1 + 100;
                    x2 = x1;
                } else if (orientation.equals(ORIENT_DOWN)) {
                    y2 = y1 - 100;
                    x2 = x1;
                } else if (orientation.equals(ORIENT_LEFT)) {
                    x2 = x1 - 100;
                    y2 = y1;
                } else if (orientation.equals(ORIENT_RIGHT)) {
                    x2 = x1 + 100;
                    y2 = y1;
                }

                if ((x1 == x2) && (y1 == y2) && (z1 == z2)) {
                    return;
                }

                Vector3d upVector;
                if (doGlobe()) {
                    upVector = new Vector3d(x1, y1, z1);
                } else {
                    upVector = new Vector3d(0, 0, 1);
                }

                //Keep flat in z for non globe
                Point3d p1 = new Point3d(x1, y1, z1);
                Point3d p2 = new Point3d(x2, y2,
                                         (( !getUseFixedZ() || doGlobe())
                                          ? z2
                                          : z1));
                t.lookAt(p1, p2, upVector);

                t.get(m);

                EarthLocation el1 = navDisplay.getEarthLocation(p1.x, p1.y,
                                        p1.z, false);
                EarthLocation el2 = navDisplay.getEarthLocation(p2.x, p2.y,
                                        p2.z, false);
                Bearing bearing =
                    Bearing.calculateBearing(new LatLonPointImpl(getLat(el1),
                        getLon(el1)), new LatLonPointImpl(getLat(el2),
                            getLon(el2)), null);
                currentHeading = bearing.getAngle();



                double[] tiltMatrix = mouseBehavior.make_matrix(tiltx, tilty,
                                          tiltz, 1.0, 1.0, 1.0, 0.0, 0.0,
                                          0.0);
                m = mouseBehavior.multiply_matrix(tiltMatrix, m);
                if (aspect != null) {
                    double[] aspectMatrix = mouseBehavior.make_matrix(0.0,
                                                0.0, 0.0, aspect[0],
                                                aspect[1], aspect[2], 0.0,
                                                0.0, 0.0);
                    //                    m = mouseBehavior.multiply_matrix(aspectMatrix, m);
                }

                double[] scaleMatrix = mouseBehavior.make_matrix(0.0, 0.0,
                                           0.0, zoom, 0.0, 0.0, 0.0);

                m = mouseBehavior.multiply_matrix(scaleMatrix, m);
            }

            currentPoint = pt1;
            location     = currentPoint.getEarthLocation();

            if (doGlobe()) {
                setPts(locationLine, 0, x1 * 2, 0, y1 * 2, 0, z1 * 2);
                //                setPts(locationLine2, 0, x2 * 2, 0, y2 * 2, 0, z2 * 2);
            } else {
                setPts(locationLine, x1, x1, y1, y1, 1, -1);
            }

            RealTuple markerLocation =
                new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                              new double[] { x1,
                                             y1, z1 });

            if (xyz1[0] != xyz2[0]) {
                Transform3D rotTransform;
                VisADGeometryArray marker =
                    (VisADGeometryArray) getMarker().clone();
                double rotx = 0;
                double roty = 0;
                double rotz = 0;
                rotz = -Math.toDegrees(Math.atan2(actualPoint[1] - xyz1[1],
                        actualPoint[0] - xyz1[0])) + 90;


                if (doGlobe()) {
                    Vector3d upVector = new Vector3d(x1, y1, z1);
                    rotTransform = new Transform3D();
                    rotTransform.lookAt(new Point3d(x1, y1, z1),
                                        new Point3d(x2, y2, z2), upVector);
                    Matrix3d m3d = new Matrix3d();
                    rotTransform.get(m3d);
                    rotTransform = new Transform3D(m3d,
                            new Vector3d(0, 0, 0), 1);
                    rotTransform.invert();
                    //                    ShapeUtility.rotate(marker, rotTransform,(float)x1,(float)y1,(float)z1);
                    ShapeUtility.rotate(marker, rotTransform);

                } else {
                    double[] markerM =
                        navDisplay.getMouseBehavior().make_matrix(rotx, roty,
                            rotz, 1.0, 0.0, 0.0, 0.0);
                    rotTransform = new Transform3D(markerM);
                    ShapeUtility.rotate(marker, rotTransform);
                }

                locationMarker.setPoint(markerLocation, marker);
            } else {
                locationMarker.setPoint(markerLocation);
            }

            locationLine.setVisible(showLine);
            //            locationLine2.setVisible(showLine);
            locationMarker.setVisible(showMarker);


            if (hasTimes && getShowTimes()) {
                DateTime dttm = pt1.getDateTime();
                if (dttm != null) {
                    viewManager.getAnimationWidget().setTimeFromUser(dttm);
                }

            }



            if (changeViewpointCbx.isSelected()) {
                if (animateMove) {
                    navDisplay.animateMatrix(m, animationSpeed);
                } else {
                    navDisplay.setProjectionMatrix(m);
                }
            }

            if ( !Misc.equals(lastLocation, pt1.getEarthLocation())) {
                lastLocation = pt1.getEarthLocation();
                EarthLocationTuple tuplePosition =
                    new EarthLocationTuple(lastLocation.getLatitude(),
                                           lastLocation.getLongitude(),
                                           lastLocation.getAltitude());
                doShare(ucar.unidata.idv.control.ProbeControl.SHARE_POSITION,
                        tuplePosition);
            }


        } catch (NumberFormatException exc) {
            logException("Error parsing number:" + exc, exc);
        } catch (javax.media.j3d.BadTransformException bte) {
            try {
                navDisplay.setProjectionMatrix(currentMatrix);
            } catch (Exception ignore) {}
        } catch (Exception exc) {
            logException("Error", exc);
            if (animationWidget != null) {
                animationWidget.setRunning(false);
            }
            return;
        }



    }


    /** _more_ */
    private VisADGeometryArray marker;

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean doGlobe() {
        if (viewManager != null) {
            return viewManager.getUseGlobeDisplay();
        }
        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public VisADGeometryArray getMarker() {
        if (marker == null) {
            if (doGlobe()) {
                marker = ShapeUtility.setSize(
                    ShapeUtility.createShape(ShapeUtility.CROSS)[0], .1f);
            } else {
                marker = ShapeUtility.setSize(
                    ShapeUtility.createShape(ShapeUtility.AIRPLANE3D)[0],
                    .1f);
            }
        }
        return marker;
    }


    /**
     * _more_
     *
     * @param msg _more_
     * @param exc _more_
     */
    public void logException(String msg, Throwable exc) {
        LogUtil.logException(msg, exc);
    }




    /**
     * _more_
     */
    public void displayControlChanged() {
        doUpdate();
    }


    /**
     * _more_
     */
    public void doUpdate() {
        FlythroughPoint pt1 = getCurrentPoint();
        if (pt1 != null) {
            try {
                processReadout(pt1);
            } catch (Exception exc) {
                logException("Setting readout", exc);
            }
        }
    }


    /**
     * _more_
     */
    public void animationTimeChanged() {
        doUpdate();
    }


    /**
     * _more_
     *
     * @param pt1 _more_
     *
     * @throws Exception _more_
     */
    protected void processReadout(FlythroughPoint pt1) throws Exception {

        Font font = new Font("Dialog", Font.BOLD, 22);
        dials = new ArrayList<JComponent>();
        if ((readoutLabel == null) || (pt1 == null)) {
            return;
        }

        List<ReadoutInfo> samples = new ArrayList<ReadoutInfo>();
        readoutLabel.setText(readout.getReadout(pt1.getEarthLocation(),
                showReadout, true, samples));

        if ( !showReadout) {
            return;
        }


        List comps = new ArrayList();
        for (FlythroughDecorator decorator : decorators) {
            decorator.handleReadout(pt1, samples);
        }

        for (ReadoutInfo info : samples) {
            Real r = info.getReal();
            if (r == null) {
                continue;
            }

            Unit unit = info.getUnit();
            if (unit == null) {
                unit = r.getUnit();
            }
            String name       = ucar.visad.Util.cleanTypeName(r.getType());





            String unitSuffix = "";
            if (unit != null) {
                unitSuffix = " [" + unit + "]";
            }


            double v = r.getValue(unit);
            if (v == v) {
                v = Misc.parseNumber(Misc.format(v));
            }

            JLabel label = new JLabel(name.replace("_", " "));

            label.setFont(font);
            DefaultValueDataset dataset =
                new DefaultValueDataset(new Double(v));
            MeterPlot plot = new MeterPlot(dataset);
            if (info.getRange() != null) {
                Range range = info.getRange();
                plot.setRange(new org.jfree.data.Range(range.getMin(),
                        range.getMax()));
            }
            if (unit != null) {
                plot.setUnits(unit.toString());
            } else {
                plot.setUnits("");
            }
            plot.setDialBackgroundPaint(Color.white);
            plot.setTickLabelsVisible(true);
            plot.setValueFont(font);
            plot.setTickLabelFont(font);
            plot.setTickLabelPaint(Color.darkGray);
            plot.setTickPaint(Color.black);
            plot.setValuePaint(Color.black);

            JFreeChart chart = new JFreeChart("", plot);
            TextTitle title = new TextTitle(" " + label.getText() + " ",
                                            font);
            title.setBackgroundPaint(Color.gray);
            title.setPaint(Color.white);
            chart.setTitle(title);
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(dialDimension);
            chartPanel.setSize(new Dimension(150, 150));
            plot.setBackgroundPaint(new Color(255, 255, 255, 0));
            plot.setBackgroundImageAlpha(0.0f);
            chart.setBackgroundPaint(new Color(255, 255, 255, 0));
            chartPanel.setBackground(new Color(255, 255, 255, 0));
            comps.add(chartPanel);
            dials.add(chartPanel);
        }




        readoutDisplay.removeAll();
        updateDashboard();

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
        this.allPoints = value;
    }

    /**
     *  Get the Points property.
     *
     *  @return The Points
     */
    public List<FlythroughPoint> getPoints() {
        return this.allPoints;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<FlythroughPoint> getPointsToUse() {
        return this.pointsToUse;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasPoints() {
        return allPoints.size() > 0;
    }


    /**
     *  Set the Tilt property.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltX(double value) {}

    /**
     *  Set the Tilt property.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltY(double value) {}



    /**
     *  Set the Tilt propertz.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltZ(double value) {}




    /**
     * Set the Tilt property.
     *
     * @param value The new value for Tilt
     */
    public void setTilt(double[] value) {
        this.tilt = value;
    }

    /**
     * Get the Tilt property.
     *
     * @return The Tilt
     */
    public double[] getTilt() {
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
            this.zoom = parse(zoomFld, zoom);
        }
        return this.zoom;
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
        updateDashboard();
    }

    /**
     *  Get the ShowReadout property.
     *
     *  @return The ShowReadout
     */
    public boolean getShowReadout() {
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
            } catch (Exception ignore) {}
        }
        if (locationLine2 != null) {
            try {
                locationLine2.setVisible(value);
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



    /**
     *  Set the ShowMarker property.
     *
     *  @param value The new value for ShowMarker
     */
    public void setShowMarker(boolean value) {
        showMarker = value;
        if (locationMarker != null) {
            try {
                locationMarker.setVisible(value);
            } catch (Exception ignore) {}
        }
    }

    /**
     * Get the ShowMarker property.
     *
     * @return The ShowMarker
     */
    public boolean getShowMarker() {
        return showMarker;
    }


    /**
     * Set the Animate property.
     *
     * @param value The new value for Animate
     */
    public void setAnimate(boolean value) {
        animate = value;
    }

    /**
     * Get the Animate property.
     *
     * @return The Animate
     */
    public boolean getAnimate() {
        if (animateCbx != null) {
            animate = animateCbx.isSelected();
        }
        return animate;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setRelativeOrientation(boolean value) {}

    /**
     * Set the Orientation property.
     *
     * @param value The new value for Orientation
     */
    public void setOrientation(String value) {
        this.orientation = value;
    }

    /**
     * Get the Orientation property.
     *
     * @return The Orientation
     */
    public String getOrientation() {
        return this.orientation;
    }


    /**
     * Set the Shown property.
     *
     * @param value The new value for Shown
     */
    public void setShown(boolean value) {
        this.shown = value;
    }

    /**
     * Get the Shown property.
     *
     * @return The Shown
     */
    public boolean getShown() {
        if (frame != null) {
            return frame.isShowing();
        }
        return this.shown;
    }

    /**
     *  Set the Clip property.
     *
     *  @param value The new value for Clip
     *
     * @throws Exception _more_
     */
    public void setClip(boolean value) throws Exception {}


    /**
     *  Set the UseFixedZ property.
     *
     *  @param value The new value for UseFixedZ
     */
    public void setUseFixedZ(boolean value) {
        useFixedZ = value;
    }

    /**
     *  Get the UseFixedZ property.
     *
     *  @return The UseFixedZ
     */
    public boolean getUseFixedZ() {
        return useFixedZ;
    }


    /**
     *  Set the CurrentIndex property.
     *
     *  @param value The new value for CurrentIndex
     */
    public void setCurrentIndex(int value) {
        currentIndex = value;
    }

    /**
     *  Get the CurrentIndex property.
     *
     *  @return The CurrentIndex
     */
    public int getCurrentIndex() {
        if (animation != null) {
            try {
                currentIndex = animation.getCurrent();
            } catch (Exception ignore) {}
        }
        return currentIndex;
    }

    /**
     *  Set the AnimateSpeed property.
     *
     *  @param value The new value for AnimateSpeed
     */
    public void setAnimateSpeed(long value) {}



    /**
     * Set the AnimationSpeed property.
     *
     * @param value The new value for AnimationSpeed
     */
    public void setAnimationSpeed(int value) {
        this.animationSpeed = value;
    }

    /**
     * Get the AnimationSpeed property.
     *
     * @return The AnimationSpeed
     */
    public int getAnimationSpeed() {
        return this.animationSpeed;
    }



    /**
     *  Set the Location property.
     *
     *  @param value The new value for Location
     */
    public void setLocation(EarthLocation value) {
        this.location = value;
    }

    /**
     *  Get the Location property.
     *
     *  @return The Location
     */
    public EarthLocation getLocation() {
        return this.location;
    }

    /**
     *  Set the Heading property.
     *
     *  @param value The new value for Heading
     */
    public void setHeading(double value) {
        this.heading = value;
    }

    /**
     *  Get the Heading property.
     *
     *  @return The Heading
     */
    public double getHeading() {
        return this.heading;
    }


    /**
     * Set the FrameLocation property.
     *
     * @param value The new value for FrameLocation
     */
    public void setFrameLocation(Point value) {
        //noopx
    }


    /**
     * Set the GoToClick property.
     *
     * @param value The new value for GoToClick
     */
    public void setGoToClick(boolean value) {
        this.goToClick = value;
    }

    /**
     * Get the GoToClick property.
     *
     * @return The GoToClick
     */
    public boolean getGoToClick() {
        return this.goToClick;
    }



    /**
     *  Set the ShowChart property.
     *
     *  @param value The new value for ShowChart
     */
    public void setShowChart(boolean value) {}




    /**
     *  Set the ShowDecoration property.
     *
     *  @param value The new value for ShowDecoration
     */
    public void setShowDecoration(boolean value) {
        this.showDecoration = value;
        updateDashboard();
    }

    /**
     *  Get the ShowDecoration property.
     *
     *  @return The ShowDecoration
     */
    public boolean getShowDecoration() {
        return this.showDecoration;
    }



    /**
     *  Set the ShowAnimation property.
     *
     *  @param value The new value for ShowAnimation
     */
    public void setShowAnimation(boolean value) {
        this.showAnimation = value;
        if (animationWidget != null) {
            animationWidget.getContents().setVisible(showAnimation);
        }
    }

    /**
     *  Get the ShowAnimation property.
     *
     *  @return The ShowAnimation
     */
    public boolean getShowAnimation() {
        return this.showAnimation;
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
            animationInfo = animationWidget.getAnimationInfo();
        }
        return animationInfo;
    }

    /**
     *  Set the WindowBounds property.
     *
     *  @param value The new value for WindowBounds
     */
    public void setWindowBounds(Rectangle value) {
        this.windowBounds = value;
    }

    /**
     *  Get the WindowBounds property.
     *
     *  @return The WindowBounds
     */
    public Rectangle getWindowBounds() {
        if (frame != null) {
            this.windowBounds = frame.getBounds();
        }
        return this.windowBounds;
    }




    /**
     * Set the MaxPoints property.
     *
     * @param value The new value for MaxPoints
     */
    public void setMaxPoints(int value) {}

    /**
     * Set the Stride property.
     *
     * @param value The new value for Stride
     */
    public void setStride(int value) {
        this.stride = value;
    }

    /**
     * Get the Stride property.
     *
     * @return The Stride
     */
    public int getStride() {
        return this.stride;
    }


    /**
     * Set the Decorators property.
     *
     * @param value The new value for Decorators
     */
    public void setDecorators(List<FlythroughDecorator> value) {
        decorators = value;
    }

    /**
     * Get the Decorators property.
     *
     * @return The Decorators
     */
    public List<FlythroughDecorator> getDecorators() {
        return decorators;
    }


}

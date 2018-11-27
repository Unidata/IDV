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


import org.w3c.dom.Element;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.PointCloudDataSource;
import ucar.unidata.data.point.PointDataInstance;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObTuple;
import ucar.unidata.idv.control.drawing.DrawingGlyph;
import ucar.unidata.idv.control.drawing.GlyphCreatorCommand;
import ucar.unidata.idv.control.drawing.PolyGlyph;
import ucar.unidata.idv.control.drawing.ShapeGlyph;
import ucar.unidata.ui.PropertyFilter;
import ucar.unidata.util.*;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.GeoUtils;
import ucar.visad.Util;
import ucar.visad.display.ImageRGBDisplayable;
import ucar.visad.display.VolumeDisplayable;

import visad.*;

import visad.georef.*;

import visad.util.DataUtility;

import java.awt.Container;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;


/**
 * A display control for volume rendering of a 3D grid
 *
 * @author IDV Development Team
 */

public class PointCloudControl extends DrawingControl {

    /** the display for the volume renderer */
    VolumeDisplayable myDisplay;

    /** my display */
    ImageRGBDisplayable myRGBDisplay;


    /** the display for the volume renderer */
    boolean useTexture3D = true;

    /** trivial map projection of the points */
    MapProjection projection;

    /** the tabbed pane */
    private JTabbedPane tabbedPane;

    /** Which field in the data should be used for coloring */
    private int colorRangeIndex = -1;

    /** The data ranges */
    private Range[] dataRanges;

    /** widget */
    private JComboBox colorParamsBox = null;

    /** the range types */
    private RealType[] rangeTypes;

    /** stream flow filter */
    private boolean doFilter = false;

    /** clip the points to the shapes */
    private boolean doClip = true;

    /** clip out the things outside the shapes */
    private boolean showInside = true;

    /** copy of the data */
    private FieldImpl displayedData;

    /** flag for having RGB */
    private boolean hasRGB = false;

    /** do we have times */
    private boolean isSequence = false;

    /** should we change the view as the time changes */
    private boolean followTimeStep = false;

    /** holds center points */
    private Hashtable<Integer, LatLonPoint> timeMap = new Hashtable<Integer,
                                                          LatLonPoint>();

    /** the animation set */
    private Set animationSet;

    /** The GUI used to show and edit the filters */
    private PropertyFilter.FilterGui filterGui;

    /** Do we use and logic or or logic for the filters */
    private boolean matchAll = true;

    /** Are the filters enabled */
    private boolean filtersEnabled = true;



    /** filters for showing data */
    protected List filters = new ArrayList();


    /**
     * Default constructor; does nothing.
     */
    public PointCloudControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);
        currentCmd = GlyphCreatorCommand.CMD_RECTANGLE;
    }

    /**
     * Get the initial z position
     *
     * @return  the Z position
     */
    protected double getInitialZPosition() {
        return 1.0;
    }

    /**
     * Can we handle events?
     *
     * @return  true or false
     */
    protected boolean canHandleEvents() {

        if (tabbedPane != null) {
            return super.canHandleEvents()
                   && (tabbedPane.getSelectedIndex() != 0);
        }
        return super.canHandleEvents();
    }


    /**
     * Add export points menu tiem
     *
     * @param items menu items to add to
     * @param forMenuBar for menubar
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        super.getSaveMenuItems(items, forMenuBar);
        items.add(GuiUtils.makeMenuItem("Export Points...",
                                        this,
                                        "exportPoints"));

    }

    /**
     * add view menu items
     *
     * @param items menu items to add to
     * @param forMenuBar for menubar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        super.getViewMenuItems(items, forMenuBar);
        if (isSequence) {
            items.add(GuiUtils.makeCheckboxMenuItem("Follow Time Steps",
                    this,
                    "followTimeStep",
                    null));
        }
    }

    /**
     * noop so the drawing control doesn't add its items
     *
     * @param items menu items to add to
     * @param forMenuBar for menubar
     */
    protected void addFileMenuItems(List items, boolean forMenuBar) {}


    /**
     * get animation time changes
     *
     * @return true
     */
    protected boolean shouldAddAnimationListener() {
        return true;
    }



    /**
     * handle when the animation changes. If this is a time sequence and
     * followTimeStep is true then center the display
     *
     * @param time current time
     */
    protected void timeChanged(Real time) {
        super.timeChanged(time);
        try {
            if ( !isSequence || !followTimeStep || (animationSet == null)) {
                return;
            }
            int index = ucar.visad.Util.findIndex(animationSet, time);
            if (index < 0) {
                return;
            }

            LatLonPoint llp = timeMap.get(new Integer(index));
            if (llp == null) {
                //Find the center point of the bounding box of the points in the current time
                EarthLocation el =
                    new EarthLocationTuple(new Real(RealType.Latitude,
                                                    40), new Real(
                                                        RealType.Longitude,
                                                                -107), new Real(
                                                                    RealType
                                                                        .Altitude,
                                                                            0.0));
                llp = el.getLatLonPoint();
                timeMap.put(new Integer(index), llp);
            }
            NavigatedDisplay navDisplay = getNavigatedDisplay();
            navDisplay.center(GeoUtils.toEarthLocation(llp), false);
        } catch (Exception exc) {
            followTimeStep = false;
            logException("Handling time change", exc);
        }
    }


    /**
     * Export to KML
     *
     * @param filename the file name
     *
     * @throws Exception  problem exporting
     */
    public void exportKml(String filename) throws Exception {
        //        OutputStream os =
        //            new BufferedOutputStream(new FileOutputStream(filename));
        try {
            OutputStream os = new FileOutputStream(filename);
            Element root = KmlUtil.kml(
                               IOUtil.getFileTail(
                                   IOUtil.stripExtension(filename)));
            Element folder = KmlUtil.folder(root, "Points", false);
            KmlUtil.open(folder, false);

            FlatField points = null;
            isSequence = GridUtil.isTimeSequence(displayedData);
            if (isSequence) {
                points = (FlatField) displayedData.getSample(0, false);
            } else {
                points = (FlatField) displayedData;
            }
            // set some default indices
            int       latIndex = PointCloudDataSource.INDEX_LAT;
            int       lonIndex = PointCloudDataSource.INDEX_LON;
            int       altIndex = PointCloudDataSource.INDEX_ALT;
            float[][] pts      = points.getFloats(false);
            //        pw.write("#latitude,longitude,altitude");
            for (int i = 0; i < pts[0].length; i++) {
                KmlUtil.placemark(folder, "", null, pts[latIndex][i],
                                  pts[lonIndex][i], pts[altIndex][i], null);
            }
            byte[] bytes = XmlUtil.toString(root).getBytes();
            os.write(bytes);
            os.flush();
            os.close();
        } catch (Exception exc) {
            logException("oops", exc);
        }
    }


    /**
     * write out the points
     *
     * @throws Exception on badness
     */
    public void exportPoints() throws Exception {
        JComboBox publishCbx =
            getIdv().getPublishManager().getSelector("nc.export");
        String filename =
            FileManager.getWriteFile(Misc.newList(FileManager.FILTER_CSV),
                                     FileManager.SUFFIX_CSV,
                                     ((publishCbx != null)
                                      ? GuiUtils.top(publishCbx)
                                      : null));
        if (filename == null) {
            return;
        }
        OutputStream os =
            new BufferedOutputStream(new FileOutputStream(filename));

        if (filename.toLowerCase().endsWith(".kml")) {
            exportKml(filename);
            // TJJ Dec 2013 fix memory leak
            if (os != null) {
                os.close();
            }
            return;
        }

        PrintWriter pw     = new PrintWriter(os);
        FlatField   points = null;
        isSequence = GridUtil.isTimeSequence(displayedData);
        if (isSequence) {
            points = (FlatField) displayedData.getSample(0, false);
        } else {
            points = (FlatField) displayedData;
        }
        // set some default indices
        int       latIndex = PointCloudDataSource.INDEX_LAT;
        int       lonIndex = PointCloudDataSource.INDEX_LON;
        int       altIndex = PointCloudDataSource.INDEX_ALT;
        float[][] pts      = points.getFloats(false);
        //        pw.write("#latitude,longitude,altitude");
        for (int i = 0; i < pts[0].length; i++) {
            pw.print(pts[latIndex][i]);
            pw.print(",");
            pw.print(pts[lonIndex][i]);
            pw.print(",");
            pw.print(pts[altIndex][i]);
            for (int j = 3; j < pts.length; j++) {
                pw.print(",");
                pw.print(pts[j][i]);
            }
            pw.print("\n");
        }
        os.close();

        getIdv().getPublishManager().publishContent(filename, null,
                publishCbx);

    }

    /**
     * Define the shapes to use for the drawing
     *
     * @return shapes
     */
    protected List getShapeCommands() {
        return Misc.newList(GlyphCreatorCommand.CMD_RECTANGLE,
                            GlyphCreatorCommand.CMD_POLYGON);
    }


    /**
     * Get the color range index
     *
     * @return the index for the color range
     */
    public int getColorRangeIndex() {
        return colorRangeIndex;
    }

    /**
     * Set the color range index
     *
     * @param index  the index for getting the color range
     */
    public void setColorRangeIndex(int index) {
        colorRangeIndex = index;
    }

    /**
     * Get the color range from the data
     *
     * @return the range of the data for coloring
     */
    public Range getColorRangeFromData() {
        if (dataRanges != null) {
            int rangeIndex = colorRangeIndex;
            if ((colorRangeIndex < 0)
                    || (colorRangeIndex >= rangeTypes.length)) {
                rangeIndex = rangeTypes.length - 1;
            }
            return dataRanges[rangeIndex];
        }
        return super.getColorRangeFromData();
    }

    /**
     * Hook method to allow derived classes to return a different
     * initial {@link ucar.unidata.util.Range}
     *
     * @return The initial range to use
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
        return getColorRangeFromData();
    }


    /**
     * Get the data projection
     *
     * @return  the data projection
     */
    public MapProjection getDataProjection() {
        return projection;
    }


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !super.init((DataChoice) null)) {
            return false;
        }

        if ( !isDisplay3D()) {
            LogUtil.userMessage(log_,
                                "Can't render point cloud in 2D display");
            return false;
        }
        Hashtable props = dataChoice.getProperties();

        if ((props != null) && (props.get("doFilter") != null)) {
            String ns = (String) props.get("doFilter");
            if (ns == "true") {
                doFilter = true;
            }
        }

        if (doFilter) {

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (getHaveInitialized()) {
                        applyFilters();
                    }
                }
            };

            filterGui = new PropertyFilter.FilterGui(filters,
                    getFilterNames(), filtersEnabled, matchAll, listener);
        }

        setEditable(true);

        //Now, set the data. Return false if it fails.
        if ( !setData(dataChoice)) {
            return false;
        }

        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected List getFilterNames() {
        try {
            DataInstance pdi = (DataInstance) getDataInstance();

            if (pdi == null) {
                return null;
            }
            FieldImpl data = (FieldImpl) pdi.getData();  //pdi.getPointObs();

            Set domainSet = data.getDomainSet();
            int numObs    = domainSet.getLength();
            if (numObs == 0) {
                return null;
            }
            FieldImpl    ob  = (FieldImpl) data.getSample(0);
            FunctionType ft0 = (FunctionType) ob.getType();
            RealType[]   rt0 = ft0.getFlatRange().getRealComponents();
            //TupleType  tupleType = (TupleType) ob.getType();
            List names = new ArrayList();
            //MathType[] types     = tupleType.getComponents();
            names.add(PropertyFilter.NULL_NAME);
            for (int i = 0; i < rt0.length; i++) {
                String typeId   = rt0[i].toString();
                String typeName = Util.cleanTypeName(typeId);
                if ( !typeName.equals("Latitude")
                        && !typeName.equals("Longitude")
                        && !typeName.equals("Altitude")) {
                    names.add(new TwoFacedObject(typeName, typeId));
                }
            }

            return names;
        } catch (Exception exc) {
            logException("Getting filter names", exc);
        }
        return null;
    }


    /**
     * Make the display
     *
     * @throws RemoteException  on badness
     * @throws VisADException  on badness
     */
    private void makeDisplay() throws VisADException, RemoteException {
        if ((myDisplay != null) || (myRGBDisplay != null)) {
            return;
        }

        if (hasRGB) {
            reallySetAttributeFlags(FLAG_DATACONTROL | FLAG_DISPLAYUNIT
                                    | FLAG_SELECTRANGE);
            myRGBDisplay = new ImageRGBDisplayable("pointcloudrgb_"
                    + getDataInstance().getDataChoice().getName());
            myRGBDisplay.addConstantMap(
                new ConstantMap(visad.java3d.DisplayImplJ3D.POLYGON_POINT,
                                Display.PolygonMode));
            myRGBDisplay.addConstantMap(new ConstantMap(10,
                    Display.CurvedSize));
            myRGBDisplay.setPointSize(getPointSize());
            addDisplayable(myRGBDisplay, getAttributeFlags());
        } else {
            reallySetAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                                    | FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);

            myDisplay = new VolumeDisplayable("pointcloud_"
                    + getDataInstance().getDataChoice().getName());
            myDisplay.setUseRGBTypeForSelect(true);
            //myDisplay.addConstantMap(new ConstantMap(useTexture3D
            //                                         ? GraphicsModeControl.TEXTURE3D
            //                                       : GraphicsModeControl.STACK2D, Display.Texture3DMode));
            myDisplay.setPointSize(getPointSize());
            addDisplayable(myDisplay, getAttributeFlags());
        }

    }

    /**
     * Get the initial color table
     *
     * @return the initial color table
     * protected ColorTable getInitialColorTable() {
     *   return (hasRGB) ? getDisplayConventions().getParamColorTable("image")
     *                           : super.getInitialColorTable();
     * }
     */


    /**
     * Show the location widgets
     *
     * @return  false
     */
    protected boolean showLocationWidgets() {
        return false;
    }

    /**
     * Initialize the display unit
     */
    protected void initDisplayUnit() {}

    /**
     * Get the distance unit
     *
     * @return the distance unit
     */
    public Unit getDistanceUnit() {
        return getDefaultDistanceUnit();
    }


    /**
     * Show the time widget
     *
     * @return false
     */
    protected boolean showTimeWidgets() {
        return false;
    }


    /**
     * Make the UI contents
     *
     * @return  the UI contents
     *
     * @throws RemoteException  Java RMI exception
     * @throws VisADException   Problem creating the VisAD data
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        super.doMakeContents();
        JComponent mine     = doMakeWidgetComponent();
        JComponent controls = super.doMakeControlsPanel();
        JComponent shapes   = doMakeShapesPanel();
        tabbedPane = new JTabbedPane();
        tabbedPane.add("Point Cloud", mine);
        tabbedPane.add("Clipping", controls);
        tabbedPane.add("Shapes", shapes);
        if (doFilter) {
            tabbedPane.add("Filters", doMakeFilterGui());
        }
        return tabbedPane;
    }


    /**
     * Add in any special control widgets to the current list of widgets.
     *
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        if ( !hasRGB) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Color By:"),
                    GuiUtils.left(doMakeColorByWidget())));
        }
        super.getControlWidgets(controlWidgets);
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Point Size:"),
                                             GuiUtils.left(
                                             doMakePointSizeWidget())));

        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Clipping:"),
                                             GuiUtils.left(
                                             GuiUtils.hbox(
                                                 GuiUtils.makeCheckbox(
                                                     "Clip Enabled",
                                                             this,
                                                             "doClip"),
                                                         GuiUtils.makeCheckbox(
                                                         "Show Inside Region",
                                                                 this,
                                                                 "showInside"),
                                                         GuiUtils.makeButton(
                                                         "Reload",
                                                                 this,
                                                                 "reloadPointData")))));

    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent doMakeFilterGui() {
        JComponent buttons = GuiUtils.makeButton("Apply Filters", this,
                                 "applyFilters");

        JComponent gui =
            GuiUtils.topCenter(GuiUtils.left(GuiUtils.inset(buttons,
                                                            5)), filterGui.getContents());
        return gui;

    }

    /**
     * Init the vis filters
     */
    public void initFilters() {
        if (filterGui != null) {
            filters        = filterGui.getFilters();
            matchAll       = filterGui.getMatchAll();
            filtersEnabled = filterGui.getEnabled();
        }
    }


    /**
     * Apply the vis filters
     */
    public void applyFilters() {
        initFilters();
        try {
            loadPointData();
        } catch (Exception e) {}
    }

    /**
     * Make the color by widget
     *
     * @return the color by widget
     */
    private JComponent doMakeColorByWidget() {
        if (colorParamsBox == null) {
            colorParamsBox = new JComboBox();
            colorParamsBox.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if ((myDisplay == null)
                                || !getHaveInitialized()) {
                                return;
                            }
                            try {
                                colorRangeIndex =
                                    colorParamsBox.getSelectedIndex();

                                // TJJ Dec 2013, do NOT want to change display, 
                                // this event handler should only affect current data
                                // RealType colorType =
                                //     (RealType) colorParamsBox.getSelectedItem();
                                // myDisplay.setRGBRealType(colorType);

                                setRange(getColorRangeFromData());
                                setSelectRange(getRange());
                            } catch (Exception excp) {
                                logException("Setting rgb type", excp);
                            }
                        }
                    });
            setColorParams();
        }
        return colorParamsBox;

    }

    /**
     * Set the color params
     */
    private void setColorParams() {
        if ((colorParamsBox != null) && (rangeTypes != null)) {
            GuiUtils.setListData(colorParamsBox, rangeTypes);
        }
        colorParamsBox.setSelectedIndex(colorRangeIndex);
    }



    /**
     * set the point size
     *
     * @param value  the point size
     */
    public void setPointSize(float value) {
        super.setPointSize(value);
        try {
            if (myDisplay != null) {
                myDisplay.setPointSize(getPointSize());
            }
            if (myRGBDisplay != null) {
                myRGBDisplay.setPointSize(getPointSize());
            }
        } catch (Exception e) {
            logException("Setting point size", e);
        }
    }



    /**
     * Set the data in this control.
     *
     * @param choice  data description
     *
     * @return true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if (choice != null) {
            if ( !super.setData(choice) || (getNavigatedDisplay() == null)) {
                return false;
            }
        }
        try {
            loadPointData();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
        return true;
    }



    /**
     * Process the visad data object.
     *
     * @param data The data object
     *
     */
    protected void processData(Data data) {

        try {
            loadPointData(data);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Reload the point data
     *
     * @throws Exception  something bad happened
     */
    public void reloadPointData() throws Exception {
        try {
            loadPointData();
        } catch (Exception exc) {
            logException("Loading points", exc);
        }
    }

    /**
     * Load the volume data to the display
     *
     * @throws Exception problem loading data
     */
    private void loadPointData() throws Exception {
        loadPointData(null);
    }

    /**
     * Load the volume data to the display
     *
     * @param newData the new data
     * @throws Exception problem loading data
     */
    private void loadPointData(Data newData) throws Exception {

        FieldImpl data     = (newData == null)
                             ? (FieldImpl) getDataInstance().getData()
                             : (FieldImpl) newData;
        //System.out.println(" Filtering data0" + " Size=" + filters.size());
        if (filtersEnabled && (filters.size() > 0)) {
            try {
                LogUtil.message("Observation display: filtering data");
                Trace.call1("filterData");
                data = filterData(data);
                Trace.call2("filterData");
            } catch (Exception exc) {
                logException("Processing filters", exc);
            }
        }

        FieldImpl points = null;
        isSequence = GridUtil.isTimeSequence(data);
        if (isSequence) {
            points = (FieldImpl) data.getSample(0, false);
        } else {
            points = data;
        }
        // set some default indices
        int latIndex = PointCloudDataSource.INDEX_LAT;
        int lonIndex = PointCloudDataSource.INDEX_LON;
        int altIndex = PointCloudDataSource.INDEX_ALT;
        rangeTypes = ((TupleType) DataUtility.getRangeType(
            points)).getRealComponents();

        for (int i = 0; i < rangeTypes.length; i++) {
            if (rangeTypes[i].equals(RealType.Latitude)) {
                latIndex = i;
            } else if (rangeTypes[i].equals(RealType.Longitude)) {
                lonIndex = i;
            } else if (rangeTypes[i].equals(RealType.Altitude)) {
                altIndex = i;
            }
            if (rangeTypes[i].getName().equalsIgnoreCase(paramName)) {
                colorRangeIndex = i;
            }
        }

        float[][] pts = points.getFloats(false);
        if (colorRangeIndex == -1) {  // hasn't been set from bundle
            if (pts.length == 3) {    // just lat/lon/alt
                colorRangeIndex = altIndex;
            } else {
                colorRangeIndex = pts.length - 1;
            }
        }

        if (pts.length == 6) {
            boolean hasRed   = false,
                    hasGreen = false,
                    hasBlue  = false;
            for (int i = 0; i < rangeTypes.length; i++) {
                String typeName = rangeTypes[i].toString().toLowerCase();
                if (typeName.indexOf("red") >= 0) {
                    hasRed = true;
                } else if (typeName.indexOf("green") >= 0) {
                    hasGreen = true;
                } else if (typeName.indexOf("blue") >= 0) {
                    hasBlue = true;
                }
            }
            hasRGB = hasRed && hasGreen && hasBlue;
        }

        if (hasRGB) {
            colorRangeIndex = altIndex;
        }

        float   minX      = Float.POSITIVE_INFINITY;
        float   minY      = Float.POSITIVE_INFINITY;
        float   maxX      = Float.NEGATIVE_INFINITY;
        float   maxY      = Float.NEGATIVE_INFINITY;
        int     numFields = pts.length;
        float[] maxFields = new float[numFields];
        float[] minFields = new float[numFields];
        for (int i = 0; i < numFields; i++) {
            maxFields[i] = Float.NEGATIVE_INFINITY;
            minFields[i] = Float.POSITIVE_INFINITY;
        }
        dataRanges = new Range[numFields];
        if (isSequence) {
            animationSet = data.getDomainSet();
        }
        int         numTimes = ( !isSequence)
                               ? 1
                               : animationSet.getLength();
        int         SCALE    = 1000000;
        List        glyphs   = getGlyphs();
        int[]       scales   = new int[glyphs.size()];
        List<Shape> shapes   = new ArrayList<Shape>();
        if (doClip) {
            for (DrawingGlyph glyph : ((List<DrawingGlyph>) glyphs)) {
                if ((glyph instanceof ShapeGlyph)
                        && ((ShapeGlyph) glyph).getShapeType()
                           == ShapeGlyph.SHAPE_RECTANGLE) {
                    ShapeGlyph shapeGlyph = (ShapeGlyph) glyph;
                    float[][]  latLons    = shapeGlyph.getLatLons();
                    shapes.add(ShapeGlyph.makeRectangle2D(latLons));
                    scales[shapes.size() - 1] = 1;
                } else if (glyph instanceof PolyGlyph) {
                    float[][] latLons = glyph.getLatLons();
                    int[]     xs      = new int[latLons[0].length];
                    int[]     ys      = new int[latLons[0].length];
                    for (int i = 0; i < latLons[0].length; i++) {
                        xs[i] = (int) (latLons[DrawingGlyph.IDX_LON][i]
                                       * SCALE);
                        ys[i] = (int) (latLons[DrawingGlyph.IDX_LAT][i]
                                       * SCALE);
                    }
                    shapes.add(new Polygon(xs, ys, xs.length));
                    scales[shapes.size() - 1] = SCALE;
                }
            }
        }
        for (int j = 0; j < numTimes; j++) {
            if (j > 0) {
                pts = ((FieldImpl) data.getSample(j, false)).getFloats(false);
            }
            float timeminX = Float.POSITIVE_INFINITY;
            float timeminY = Float.POSITIVE_INFINITY;
            float timemaxX = Float.NEGATIVE_INFINITY;
            float timemaxY = Float.NEGATIVE_INFINITY;

            for (int k = 0; k < pts.length; k++) {
                float[] paramPts = pts[k];
                for (int i = 0; i < paramPts.length; i++) {
                    float value = paramPts[i];
                    if (value != value) {
                        continue;
                    }
                    maxFields[k] = Math.max(maxFields[k], value);
                    minFields[k] = Math.min(minFields[k], value);
                    if (k == lonIndex) {
                        timeminX = Math.min(timeminX, value);
                        timemaxX = Math.max(timemaxX, value);
                    } else if (k == latIndex) {
                        timeminY = Math.min(timeminY, value);
                        timemaxY = Math.max(timemaxY, value);
                    }
                }
            }
            EarthLocation el =
                new EarthLocationTuple(new Real(RealType.Latitude,
                                                timeminY
                                                + (timemaxY - timeminY)
                                                  / 2), new Real(
                                                      RealType.Longitude,
                                                              timeminX
                                                              + (timemaxX
                                                                 - timeminX) / 2), new Real(
                                                                     RealType.Altitude,
                                                                             0.0));
            timeMap.put(new Integer(j), el.getLatLonPoint());
            minX = Math.min(timeminX, minX);
            maxX = Math.max(timemaxX, maxX);
            minY = Math.min(timeminY, minY);
            maxY = Math.max(timemaxY, maxY);
        }

        if (shapes.size() > 0) {
            try {
                LogUtil.message("Observation display: clipping data");
                Trace.call1("clippingData");
                data = clippingData(data, shapes, scales, lonIndex, latIndex);
                Trace.call2("clippingData");
            } catch (Exception exc) {
                logException("Processing clipping", exc);
            }
        }


        for (int i = 0; i < numFields; i++) {
            if (Float.isInfinite(minFields[i])
                    || Float.isInfinite(maxFields[i])) {
                dataRanges[i] = new Range(Double.NaN, Double.NaN);
            } else {
                dataRanges[i] = new Range(minFields[i], maxFields[i]);
            }
        }
        //        System.err.println("Range:" + dataRange +" idx:" + colorRangeIndex);

        float width  = Math.max((maxX - minX), (maxY - minY));
        float height = Math.max((maxY - minY), (maxY - minY));
        Rectangle2D.Float rect = new Rectangle2D.Float(minX, minY, width,
                                     height);

        projection =
            new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple, rect);

        //Keep this around for exporting points
        this.displayedData = data;

        makeDisplay();
        if (myRGBDisplay != null) {
            myRGBDisplay.loadData(data);
        }

        if (myDisplay != null) {
            myDisplay.loadData(data, colorRangeIndex);
        }

    }

    /**
     * _more_
     *
     * @param obs _more_
     * @param shapes _more_
     * @param scales _more_
     * @param lonIndex _more_
     * @param latIndex _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FieldImpl clippingData(FieldImpl obs, List<Shape> shapes,
                                     int[] scales, int lonIndex, int latIndex)
            throws Exception {
        boolean   isTimeSequence = GridUtil.isTimeSequence(obs);
        FieldImpl clippedField   = null;
        if (isTimeSequence) {
            Set timeSet = obs.getDomainSet();
            clippedField = new FieldImpl((FunctionType) obs.getType(),
                                         timeSet);
            int numTimes = timeSet.getLength();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) obs.getSample(i);
                FieldImpl subTime = doTheActualClipping(oneTime, shapes,
                                        scales, lonIndex, latIndex);
                if (subTime != null) {
                    clippedField.setSample(i, subTime, false);
                }
            }
        } else {
            clippedField = doTheActualClipping((FieldImpl) obs, shapes,
                    scales, lonIndex, latIndex);
        }
        return clippedField;
    }

    /**
     * _more_
     *
     * @param points _more_
     * @param shapes _more_
     * @param scales _more_
     * @param lonIndex _more_
     * @param latIndex _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private FieldImpl doTheActualClipping(FieldImpl points,
                                          List<Shape> shapes, int[] scales,
                                          int lonIndex, int latIndex)
            throws Exception {

        float[][] pts      = points.getFloats(false);
        float[][] newPts   = new float[pts.length][pts[0].length];
        int       pointCnt = 0;
        for (int i = 0; i < pts[0].length; i++) {
            boolean ok = !showInside;
            for (int j = 0; j < shapes.size(); j++) {
                Shape shape = shapes.get(j);
                if (shape.contains(Misc.normalizeLongitude(pts[lonIndex][i]) * scales[j],
                                   pts[latIndex][i] * scales[j])) {
                    if (showInside) {
                        ok = true;
                        break;
                    } else {
                        ok = false;
                        break;
                    }
                } else {
                    if ( !showInside) {
                        ok = true;
                    }
                }
            }
            if (ok) {
                for (int j = 0; j < pts.length; j++) {
                    newPts[j][pointCnt] = pts[j][i];
                }
                pointCnt++;
            }
        }
        pts = Misc.copy(newPts, pointCnt);
        FunctionType ft = (FunctionType) points.getType();
        FieldImpl data0 = PointCloudDataSource.makeField(ft.getRange(), pts);

        return data0;
    }

    /**
     * Apply the filters to the given data
     *
     * @param obs The data
     *
     * @return The filtered data.
     *
     * @throws Exception When bad things happen.
     */
    protected FieldImpl filterData(FieldImpl obs) throws Exception {
        boolean   isTimeSequence = GridUtil.isTimeSequence(obs);
        FieldImpl filteredField  = null;
        if (isTimeSequence) {
            Set timeSet = obs.getDomainSet();
            filteredField = new FieldImpl((FunctionType) obs.getType(),
                                          timeSet);
            int numTimes = timeSet.getLength();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) obs.getSample(i);
                FieldImpl subTime = doTheActualFiltering(oneTime);
                if (subTime != null) {
                    filteredField.setSample(i, subTime, false);
                }
            }
        } else {
            filteredField = doTheActualFiltering((FieldImpl) obs);
        }
        return filteredField;
    }


    /**
     * Apply the filters to the data
     *
     * @param pointObs The data
     *
     * @return The filtered data
     *
     * @throws Exception When bad things happen
     */
    private FieldImpl doTheActualFiltering(FieldImpl pointObs)
            throws Exception {

        if ((pointObs == null) || pointObs.isMissing()) {
            return pointObs;
        }
        FieldImpl retField  = null;
        Set       domainSet = pointObs.getDomainSet();
        int       numObs    = domainSet.getLength();
        Vector    v         = new Vector();
        Object[]  tmpValues = new Object[filters.size()];


        for (int i = 0; i < numObs; i++) {
            Object tmp = pointObs.getSample(i);
            if ( !(tmp instanceof Tuple)) {
                continue;
            }
            //PointOb    ob        = (PointOb) tmp;
            Tuple      tuple     = (Tuple) tmp;
            TupleType  tupleType = (TupleType) tuple.getType();
            RealType[] types     = tupleType.getRealComponents();

            String[]   typeNames = new String[types.length];
            for (int typeIdx = 0; typeIdx < types.length; typeIdx++) {
                typeNames[typeIdx] = types[typeIdx].toString();
            }
            boolean ok          = true;
            boolean matchedSome = false;
            for (int filterIdx = 0; ok && (filterIdx < filters.size());
                    filterIdx++) {
                PropertyFilter filter =
                    (PropertyFilter) filters.get(filterIdx);
                String paramName   = filter.getName();

                Data   dataElement = null;


                int    dataIndex   = -1;
                for (int typeIdx = 0;
                        (dataIndex == -1)
                        && (typeIdx < typeNames.length); typeIdx++) {
                    if (paramName.equals(typeNames[typeIdx])) {
                        dataIndex = typeIdx;
                    }
                }

                if (dataIndex < 0) {
                    continue;
                }
                dataElement = tuple.getRealComponents()[dataIndex];

                if (dataElement == null) {
                    continue;
                }
                if (dataElement.isMissing()) {
                    if (matchAll) {
                        ok = false;
                    }
                    continue;
                }
                boolean filterOk = false;
                if ( !(dataElement instanceof Real)
                        || !filter.isNumericOperator()) {
                    filterOk = filter.ok(dataElement.toString().trim());
                } else {
                    Real obsReal = (Real) dataElement;
                    if (tmpValues[filterIdx] == null) {
                        String filterValue = filter.getValue().trim();
                        tmpValues[filterIdx] = filterValue;
                        Real filterReal = ucar.visad.Util.toReal(filterValue);
                        if (filterReal != null) {
                            //                            System.err.println("filterReal:" + filterReal.getUnit() + " " +
                            //                                         obsReal.getUnit());
                            if (obsReal.getUnit() == null) {
                                tmpValues[filterIdx] =
                                    new Double(filterReal.getValue());
                            } else {
                                tmpValues[filterIdx] = new Double(
                                    filterReal.getValue(obsReal.getUnit()));
                            }
                            //System.err.println("value:" + tmpValues[filterIdx]);
                        }
                    }
                    filterOk = filter.ok(dataElement, tmpValues[filterIdx]);
                }
                if (filterOk) {
                    matchedSome = true;
                    if ( !matchAll) {
                        break;
                    }
                } else {
                    if (matchAll) {
                        ok = false;
                    }
                }
            }

            if (ok && matchedSome) {
                v.add(tuple);
            }
        }



        //System.out.println("found " + v.size() + " decluttered obs in region");
        if (v.isEmpty()) {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(((SetType) domainSet.getType()).getDomain(),
                                 1));
        } else if (v.size() == numObs) {
            retField = pointObs;  // all were in domain, just return input
        } else {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(((SetType) domainSet.getType()).getDomain(),
                                 v.size()));

            retField.setSamples((Tuple[]) v.toArray(new Tuple[v.size()]),
                                false, false);
        }
        return retField;

    }



    /**
     * Is this a raster display
     *
     * @return true
     */
    public boolean getIsRaster() {
        return true;
    }


    /**
     *  Set the ShowInside property.
     *
     *  @param value The new value for ShowInside
     */
    public void setShowInside(boolean value) {
        this.showInside = value;
    }

    /**
     *  Get the ShowInside property.
     *
     *  @return The ShowInside
     */
    public boolean getShowInside() {
        return this.showInside;
    }


    /**
     *  Set the DoClip property.
     *
     *  @param value The new value for DoClip
     */
    public void setDoClip(boolean value) {
        this.doClip = value;
    }

    /**
     *  Get the DoClip property.
     *
     *  @return The DoClip
     */
    public boolean getDoClip() {
        return this.doClip;
    }

    /**
     *  Set the DoFilter property.
     *
     *  @param value The new value for DoClip
     */
    public void setDoFilter(boolean value) {
        this.doFilter = value;
    }

    /**
     *  Get the filters property.
     *
     *  @return The filters
     */
    public List getFilters() {
        return this.filters;
    }

    /**  Set the filters property.
    *
    *  @param value The new value for filters
    */
    public void setFilters(List value) {
        this.filters = value;
    }

    /**
     *  Get the DoFilter property.
     *
     *  @return The DoFilter
     */
    public boolean getDoFilter() {
        return this.doFilter;
    }
    /**
     *  Set the FollowTimeStep property.
     *
     *  @param value The new value for FollowTimeStep
     */
    public void setFollowTimeStep(boolean value) {
        this.followTimeStep = value;
    }

    /**
     *  Get the FollowTimeStep property.
     *
     *  @return The FollowTimeStep
     */
    public boolean getFollowTimeStep() {
        return this.followTimeStep;
    }



}

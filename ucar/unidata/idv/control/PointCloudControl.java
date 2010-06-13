/*
 * Copyright 2010 UNAVCO, 6350 Nautilus Drive, Boulder, CO 80301
 * http://www.unavco.org
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
 * 
 */

package ucar.unidata.idv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.PointCloudDataSource;

import ucar.unidata.idv.control.drawing.*;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.FileManager;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;
import ucar.visad.display.RGBDisplayable;

import ucar.visad.display.VolumeDisplayable;

import visad.*;

import visad.georef.MapProjection;


import visad.georef.TrivialMapProjection;

import visad.util.DataUtility;
import visad.util.SelectRangeWidget;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.geom.Rectangle2D;

import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


// $Id: VolumeRenderControl.java,v 1.11 2006/12/01 20:16:39 jeffmc Exp $ 

/**
 * A display control for volume rendering of a 3D grid
 *
 * @author Unidata IDV Development Team
 * @version $Revision: 1.11 $
 */
public class PointCloudControl extends DrawingControl {

    /** the display for the volume renderer */
    VolumeDisplayable myDisplay;

    /** the display for the volume renderer */
    boolean useTexture3D = true;

    /** _more_          */
    MapProjection projection;

    /** _more_          */
    private int colorRangeIndex = PointCloudDataSource.INDEX_ALT;

    /** _more_          */
    private Range dataRange;

    /** _more_          */
    private JComboBox colorParamsBox = null;

    /** _more_          */
    private RealType[] rangeTypes;

    /** _more_          */
    private boolean doClip = true;

    /** _more_          */
    private boolean showInside = true;

    private FlatField  displayedData;

    /**
     * Default constructor; does nothing.
     */
    public PointCloudControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);
        currentCmd = GlyphCreatorCommand.CMD_RECTANGLE;
    }


    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        super.getSaveMenuItems(items, forMenuBar);
        items.add(GuiUtils.makeMenuItem("Export Points...", this, "exportPoints"));

    }

    public void exportPoints() throws Exception {
        JComboBox publishCbx =
            getIdv().getPublishManager().getSelector("nc.export");
        String filename =
            FileManager.getWriteFile(FileManager.FILTER_CSV,
                                     FileManager.SUFFIX_CSV, ((publishCbx != null)
                                                              ? GuiUtils.top(publishCbx)
                                                              : null));
        if(filename == null) return;
        OutputStream os = new BufferedOutputStream(new FileOutputStream(filename));

        PrintWriter pw = new PrintWriter(os);

        FlatField points     = null;
        boolean   isSequence = GridUtil.isTimeSequence(displayedData);
        if (isSequence) {
            points = (FlatField) displayedData.getSample(0, false);
        } else {
            points = (FlatField) displayedData;
        }
        // set some default indices
        int latIndex = PointCloudDataSource.INDEX_LAT;
        int lonIndex = PointCloudDataSource.INDEX_LON;
        int altIndex = PointCloudDataSource.INDEX_ALT;
        float[][]pts = points.getFloats(false);
        //        pw.write("#latitude,longitude,altitude");
        for(int i=0;i<pts[0].length;i++) {
            pw.print(pts[latIndex][i]);
            pw.print(",");
            pw.print(pts[lonIndex][i]);
            pw.print(",");            
            pw.print(pts[altIndex][i]);
            for(int j=3;j<pts.length;j++) {
                pw.print(",");            
                pw.print(pts[j][i]);
            }
            pw.print("\n");            
        }
        os.close();

        getIdv().getPublishManager().publishContent(filename,
                                                    null, publishCbx);

    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected List getShapeCommands() {
        return Misc.newList(GlyphCreatorCommand.CMD_RECTANGLE,
                            GlyphCreatorCommand.CMD_POLYGON);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getColorRangeIndex() {
        return colorRangeIndex;
    }

    /**
     * _more_
     *
     * @param index _more_
     */
    public void setColorRangeIndex(int index) {
        colorRangeIndex = index;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Range getColorRangeFromData() {
        if (dataRange != null) {
            return dataRange;
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
     * _more_
     *
     * @return _more_
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
            LogUtil.userMessage(log_, "Can't render volume in 2D display");
            return false;
        }

        super.setEditable(true);
        myDisplay = new VolumeDisplayable("volrend_" + dataChoice);
        myDisplay.setUseRGBTypeForSelect(true);
        //myDisplay.addConstantMap(new ConstantMap(useTexture3D
        //                                         ? GraphicsModeControl.TEXTURE3D
        //                                       : GraphicsModeControl.STACK2D, Display.Texture3DMode));
        myDisplay.setPointSize(getPointSize());
        addDisplayable(myDisplay, getAttributeFlags());

        //Now, set the data. Return false if it fails.
        if ( !setData(dataChoice)) {
            return false;
        }

        //Now set up the flags and add the displayable 
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean showLocationWidgets() {
        return false;
    }

    protected void initDisplayUnit() {
    }

    public Unit getDistanceUnit() {
        return getDefaultDistanceUnit();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean showTimeWidgets() {
        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JComponent  drawingControlComp = (JComponent) super.doMakeContents();

        JComponent  mine               = doMakeWidgetComponent();
        JComponent  controls           = super.doMakeControlsPanel();
        JComponent  shapes             = doMakeShapesPanel();
        JTabbedPane tabbedPane         = new JTabbedPane();
        tabbedPane.add("Point Cloud", mine);
        tabbedPane.add("Clipping", controls);
        tabbedPane.add("Shapes", shapes);
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
        super.getControlWidgets(controlWidgets);
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Color By:"),
                GuiUtils.left(doMakeColorByWidget())));
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Point Size:"),
                GuiUtils.left(doMakePointSizeWidget())));

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Clipping:"),
                GuiUtils.left(
                    GuiUtils.hbox(
                        GuiUtils.makeCheckbox(
                            "Clip Enabled", this,
                            "doClip"), GuiUtils.makeCheckbox(
                                "Show Inside Region", this,
                                "showInside"), GuiUtils.makeButton(
                                    "Reload", this, "reloadPointData")))));

    }


    /**
     * _more_
     *
     * @return _more_
     */
    private JComponent doMakeColorByWidget() {
        if (colorParamsBox == null) {
            colorParamsBox = new JComboBox();
            colorParamsBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (myDisplay == null) {
                        return;
                    }
                    try {
                        colorRangeIndex =
                            colorParamsBox.getSelectedIndex();
                        RealType colorType = (RealType) colorParamsBox.getSelectedItem();
                        System.err.println("type:" + colorType);
                        myDisplay.setRGBRealType(colorType);
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
     * _more_
     */
    private void setColorParams() {
        if ((colorParamsBox != null) && (rangeTypes != null)) {
            GuiUtils.setListData(colorParamsBox, rangeTypes);
        }
        colorParamsBox.setSelectedIndex(colorRangeIndex);
    }



    /**
     * _more_
     *
     * @param value _more_
     */
    public void setPointSize(float value) {
        super.setPointSize(value);
        if (myDisplay != null) {
            try {
                myDisplay.setPointSize(getPointSize());
            } catch (Exception e) {
                logException("Setting point size", e);
            }
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
     * _more_
     *
     * @throws Exception _more_
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
     * @throws RemoteException   problem loading remote data
     * @throws VisADException    problem loading the data
     *
     * @throws Exception _more_
     */
    private void loadPointData() throws Exception {

        FieldImpl data       = (FieldImpl) getDataInstance().getData();
        FlatField points     = null;
        boolean   isSequence = GridUtil.isTimeSequence(data);
        if (isSequence) {
            points = (FlatField) data.getSample(0, false);
        } else {
            points = (FlatField) data;
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
        }

        float[][] pts = points.getFloats(false);
        if (pts.length == 3) {  // just lat/lon/alt
            colorRangeIndex = altIndex;
        } else {
            colorRangeIndex = pts.length - 1;
        }

        float       minX     = Float.POSITIVE_INFINITY;
        float       minY     = Float.POSITIVE_INFINITY;
        float       maxX     = Float.NEGATIVE_INFINITY;
        float       maxY     = Float.NEGATIVE_INFINITY;
        float       minField = Float.POSITIVE_INFINITY;
        float       maxField = Float.NEGATIVE_INFINITY;
        int         numTimes = ( !isSequence)
                               ? 1
                               : data.getDomainSet().getLength();
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
                        System.err.println("pts:" + xs[i] + " " + ys[i]
                                           + "   "
                                           + latLons[DrawingGlyph.IDX_LON]
                                           + " "
                                           + latLons[DrawingGlyph.IDX_LAT]);
                    }
                    shapes.add(new Polygon(xs, ys, xs.length));
                    scales[shapes.size() - 1] = SCALE;
                }
            }

            System.err.println(shapes);

        }
        for (int j = 0; j < numTimes; j++) {
            if (j > 0) {
                pts = ((FlatField) data.getSample(j, false)).getFloats(false);
            }

            for (int i = 0; i < pts[0].length; i++) {
                minX = Math.min(minX, pts[lonIndex][i]);
                maxX = Math.max(maxX, pts[lonIndex][i]);
                minY = Math.min(minY, pts[latIndex][i]);
                maxY = Math.max(maxY, pts[latIndex][i]);
                if (pts.length == 3) {
                    maxField = Math.max(maxField, pts[altIndex][i]);
                    minField = Math.min(minField, pts[altIndex][i]);
                } else {
                    maxField = Math.max(maxField, pts[colorRangeIndex][i]);
                    minField = Math.min(minField, pts[colorRangeIndex][i]);
                }
            }
        }

        if (shapes.size() > 0) {
            float[][] newPts   = new float[pts.length][pts[0].length];
            int       pointCnt = 0;
            for (int i = 0; i < pts[0].length; i++) {
                boolean ok = !showInside;
                for (int j = 0; j < shapes.size(); j++) {
                    Shape shape = shapes.get(j);
                    if (shape.contains(pts[lonIndex][i] * scales[j],
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
            FunctionType ft = (FunctionType) ((FlatField) data).getType();
            data = PointCloudDataSource.makeField(ft.getRange(), pts);
        }

        dataRange = new Range(minField, maxField);

        float width  = Math.max((maxX - minX), (maxY - minY));
        float height = Math.max((maxY - minY), (maxY - minY));
        Rectangle2D.Float rect = new Rectangle2D.Float(minX, minY, width,
                                     height);

        projection =
            new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple, rect);
        //System.err.println("type1:" + points.getType());
        this.displayedData =  (FlatField)data;
        myDisplay.loadData(data, colorRangeIndex);

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



}

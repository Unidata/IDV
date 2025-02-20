/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridTrajectory;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.IdvManager;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.util.*;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;

import ucar.visad.display.VolumeDisplayable;


import visad.*;

import visad.Set;
import visad.georef.MapProjection;

import visad.meteorology.SingleBandedImage;
import visad.util.SelectRangeWidget;

import visad.data.CachedFlatField;
import visad.util.ThreadManager;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.rmi.RemoteException;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.event.*;


// $Id: VolumeRenderControl.java,v 1.11 2006/12/01 20:16:39 jeffmc Exp $ 

/**
 * A display control for volume rendering of a 3D grid
 *
 * @author Unidata IDV Development Team
 * @version $Revision: 1.11 $
 */

public class VolumeRenderControl extends GridDisplayControl {

    /** the display for the volume renderer */
    VolumeDisplayable myDisplay;

    /** the display for the volume renderer */
    //boolean useTexture3D = true;
    boolean useTexture3D = false;

    /** _more_ */
    private boolean usePoints = false;

    //private  int[][] indices = null;
    //private  float[][] weights = null;
    //private float[][] xyzLocs = null;

    /** z position slider */
    //Linear3DSet volumeXYZ = null;
    //GriddedSet xyzSet = null;
    int xyResample = (int)(2.56 * 75);
    int zResample = (int)(1.28 * 75);
    /** Image transparency */
    private float alpha = 1.0f;
    private JSlider qualitySlider = null;
    private int volumeQuality = 75;

    /** old smoothing type */
    private String OldSmoothingType = LABEL_NONE;

    HashMap<String, FieldImpl> dataMap;

    /** old smoothing factor */
    private int OldSmoothingFactor = 0;

    /** property for radar volume */
    public static final String GRID_VOLUME = "Grid Volume Rendering";
    /**
     * Default constructor; does nothing.
     */
    public VolumeRenderControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT | FLAG_SELECTRANGE | FLAG_SMOOTHING );
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

        if ( !isDisplay3D()) {
            LogUtil.userMessage(log_, "Can't render volume in 2D display");
            return false;
        }
        myDisplay = new VolumeDisplayable("volrend_" + dataChoice);
        myDisplay.setUseRGBTypeForSelect(true);
        myDisplay.addConstantMap(new ConstantMap(useTexture3D
                ? GraphicsModeControl.TEXTURE3D
                : GraphicsModeControl.STACK2D, Display.Texture3DMode));

        myDisplay.setPointSize(getPointSize());
        addDisplayable(myDisplay, getAttributeFlags());
        dataMap = new HashMap<>();

        //Now, set the data. Return false if it fails.
        if ( !setData(dataChoice)) {
            return false;
        }

        //Now set up the flags and add the displayable 
        return true;
    }


    protected Hashtable getRequestProperties() {
        if (requestProperties == null) {
            requestProperties =
                    Misc.newHashtable(GRID_VOLUME,
                            Float.valueOf(0.0f));
        }
        return requestProperties;
    }

    /**
     * This reset data api need to apply smoothing, otherwise, no more smoothing
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected void resetData() throws VisADException, RemoteException {
        super.resetData();
        OldSmoothingType = LABEL_NONE;
        applySmoothing();
    }
    /**
     *  Use the value of the smoothing type and weight to subset the data.
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    protected void applySmoothing() throws VisADException, RemoteException {
        if (checkFlag(FLAG_SMOOTHING)) {
            if (myDisplay != null) {
                if ( !getSmoothingType().equalsIgnoreCase(LABEL_NONE)
                        || !OldSmoothingType.equalsIgnoreCase(LABEL_NONE)) {
                    if ( !getSmoothingType().equals(OldSmoothingType)
                            || (getSmoothingFactor() != OldSmoothingFactor)) {
                        OldSmoothingType   = getSmoothingType();
                        OldSmoothingFactor = getSmoothingFactor();
                        if(dataMap.get(getSmoothingType()+getSmoothingFactor()) == null)
                            try {
                                FieldImpl none6Data = dataMap.get("None6");
                                FieldImpl sdata = GridUtil.smooth(none6Data, getSmoothingType(),
                                        getSmoothingFactor());
                                dataMap.put(getSmoothingType()+getSmoothingFactor(), sdata);
                                myDisplay.loadData(sdata);
                            } catch (Exception ve) {
                                logException("applySmoothing", ve);
                            }
                        else {
                            FieldImpl sdata = dataMap.get(getSmoothingType()+getSmoothingFactor());
                            myDisplay.loadData(sdata);
                        }
                    }
                }
            }
        }
    }
    /**
     * Make the requester Hastable of properties that is carried along with
     * the data instance; this one tells Level2Adapter to call the
     * getVolume method.
     */
    protected void setRequestProperties() {
        if (requestProperties == null) {
            requestProperties =
                    Misc.newHashtable(GRID_VOLUME,
                            Float.valueOf(0.0f));
        } else {
            requestProperties.clear();
            requestProperties.put(GRID_VOLUME,
                    Float.valueOf(0.0f));
        }
    }
    /**
     * Make a grid with a Linear3DSet for the volume rendering
     *
     * @param grid grid to transform
     * @param xyzSet   coordinate system to transform to XYZ
     *
     * @return transformed grid
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   problem creating grid
     */
    static private FieldImpl makeLinearGrid(FieldImpl grid, GriddedSet xyzSet,  MapProjectionDisplay mpd,
                                            int xyResample,  int zResample, int[][]  indices, float[][] weights )
            throws VisADException, RemoteException {
        Trace.call1("VRC.setSpatialDomain");
        FieldImpl newGrid = GridUtil.setSpatialDomain(grid, xyzSet);  //, true);
        Trace.call2("VRC.setSpatialDomain");
        float[] lows  = xyzSet.getLow();
        float[] highs = xyzSet.getHi();
        //Misc.printArray("lows",lows);
        //Misc.printArray("highs",highs);

        //if(volumeXYZ == null)
        Linear3DSet   volumeXYZ = new Linear3DSet(RealTupleType.SpatialCartesian3DTuple, lows[0],
                            //-TDR highs[0], lengths[0], lows[1], highs[1],
                            highs[0], xyResample, lows[1], highs[1],
                            //-TDR lengths[1], lows[2], highs[2], lengths[2]);
                             xyResample, lows[2], highs[2], zResample);
        //System.out.println(volumeXYZ);
        Trace.call1("VRC.resampleGrid");
        // System.out.println("start resampleGrid");
        //newGrid = GridUtil.resampleGrid(newGrid, volumeXYZ);
        newGrid = resampleGrid((FlatField) newGrid, volumeXYZ,  indices, weights);
         //  System.out.println("done resampleGrid");
        Trace.call2("VRC.resampleGrid");
        Trace.call2("VRC.makeLinearGrid");
        return newGrid;
    }

    /**
     * Resample the grid at the positions defined by a SampledSet.
     *
     * @param  grid   grid to resample (must be a valid 3D grid)
     * @param  volumeXYZ  set of points to sample on.  It must be compatible
     *         with the spatial domain of the grid.
     * @param  indices
     * @param  weights
     *
     * @return  a FlatField the grid representing the values
     *          of the original grid at the
     *          points defined by subDomain.  If this is a sequence of grids
     *          it will be a sequence of the subsets.
     *
     * @throws  VisADException  invalid subDomain or some other problem
     */
    static private FlatField resampleGrid(FlatField grid, Linear3DSet volumeXYZ, int[][]  indices, float[][] weights)
             throws VisADException, RemoteException {
        int numLocs = volumeXYZ.getLength();
        FlatField newFF = new CachedFlatField((FunctionType) grid.getType(), volumeXYZ);

      //  Gridded3DSet domainSet = (Gridded3DSet) grid.getDomainSet();
      //  long              start    = System.currentTimeMillis();
      //  if (indices == null) {
      //  int[][]  indices = new int[numLocs][];
      //  float[][] weights = new float[numLocs][];
      //  float[][] xyzLocs = volumeXYZ.getSamples(false);
      //  domainSet.valueToInterp(xyzLocs, indices, weights);
      //  }

        float[][] values = grid.getFloats(false);
        float[][] newValues = new float[1][numLocs];
        float newVal = 0;
        for (int k=0; k < numLocs; k++) {
            newVal = 0;
            if(indices[k] != null && weights[k] != null   ) {
                for (int i = 0; i < indices[k].length; i++) {
                    if(indices[k][i] < values[0].length)
                        newVal += weights[k][i] * values[0][indices[k][i]];
                }

                newValues[0][k] = newVal;
            }
        }

        newFF.setSamples(newValues, false);
        return newFF;
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

        if ( !usePoints) {
            JCheckBox textureToggle = GuiUtils.makeCheckbox("", this,
                                          "useTexture3D");
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Resolution" + ":"),
                    doMakeQualitySlider() ));
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Use 3D Texture:"),
                    GuiUtils.leftCenter(textureToggle, GuiUtils.filler())));
        } else {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Point Size:"),
                    GuiUtils.left(doMakePointSizeWidget())));
        }

    }

    /**
     * _more_
     *
     * @param
     */
    protected void applyQuality()
            throws VisADException, RemoteException {
        if (myDisplay != null) {
            //showWaitCursor();
            int xyResample0 = (int)(2.56 * getVolumeQuality());
            int zResample0 = (int)(1.28 * getVolumeQuality());
            if(xyResample0 != xyResample) {
                xyResample = xyResample0;
                zResample = zResample0;
                loadVolumeData();
            }

        }
        getIdv().getIdvUIManager().clearWaitCursor();
        //showNormalCursor();
    }

    /**
     * _more_
     *
     * @param
     */
    public int getVolumeQuality() {
        return volumeQuality;
    }

    /**
     * _more_
     *
     * @param quality _more_
     */
    public void setVolumeQuality(int quality) {
        volumeQuality = quality;
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
     * Make a slider for the texture quality
     *
     * @return the slider
     */
    protected JSlider doMakeQualitySlider() {
        if (qualitySlider == null) {
            qualitySlider = GuiUtils.makeSlider(50, 100, volumeQuality, this,
                    "setQuality");
            Hashtable labels = new Hashtable();
            labels.put(Integer.valueOf(100), GuiUtils.lLabel("High"));
            labels.put(Integer.valueOf(75), GuiUtils.cLabel("Medium"));
            labels.put(Integer.valueOf(50), GuiUtils.rLabel("Low"));
            qualitySlider.setLabelTable(labels);
            qualitySlider.setPaintLabels(true);
        }
        return qualitySlider;
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
        if ( !super.setData(choice) || (getNavigatedDisplay() == null)) {
            return false;
        }
        loadVolumeData();
        return true;
    }



    /**
     * Make the gui. Align it left
     *
     * @return The gui
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        if (usePoints) {
            setAttributeFlags(FLAG_SKIPFACTOR);
        }

        return GuiUtils.left(doMakeWidgetComponent());
    }

    /**
     * _more_
     *
     * @param quality _more_
     */
    public void setQuality(int quality) {
        volumeQuality = quality;
        if (getHaveInitialized()) {
            try {
                 showWaitCursor();
                applyQuality();
            } catch (Exception exc) {
                logException("loading volume data", exc);
            } finally {
                showNormalCursor();
            }

        }
    }

    /**
     *  Use the value of the skip factor to subset the data.
     */
    protected void applySkipFactor() {
        try {
           /**
            showWaitCursor();
            loadVolumeData();
           **/
        } catch (Exception exc) {
            logException("loading volume data", exc);
        } finally {
            showNormalCursor();
        }

    }

    /**
     * Load the volume data to the display
     *
     * @throws RemoteException   problem loading remote data
     * @throws VisADException    problem loading the data
     */
    private void loadVolumeData() throws VisADException, RemoteException {
        Trace.call1("VRC.loadVolumeData");
        FieldImpl grid    = getGridDataInstance().getGrid();
        FieldImpl newGrid = grid;

        if (getSkipValue() > 0) {
            grid    = GridUtil.subset(grid, getSkipValue() + 1);
            newGrid = grid;
        }

        if ( !usePoints) {
            // make sure the projection is correct before we start
            // transforming the data
            //long              start    = System.currentTimeMillis();
            setProjectionInView(true, true);
            //showWaitCursor();
            CoordinateSystem cs =
                getNavigatedDisplay().getDisplayCoordinateSystem();
            if ((cs != null)
                    && (getNavigatedDisplay()
                        instanceof MapProjectionDisplay)) {
                MapProjectionDisplay mpd =
                        (MapProjectionDisplay) getNavigatedDisplay();
                try {
                    //System.out.println("Time used to resample0 = "
                    //        + (System.currentTimeMillis() - start) / 1000.0);
                    GriddedSet domainSet   = (GriddedSet) GridUtil.getSpatialDomain(grid);
                    SampledSet ss          = null;
                    boolean    latLonOrder = GridUtil.isLatLonOrder(domainSet);
                    //System.out.println("grid is latLonOrder " + latLonOrder);
                    Trace.call1("VRC.convertDomain");
                    if (latLonOrder) {
                        ss = Util.convertDomain(domainSet,
                                RealTupleType.LatitudeLongitudeAltitude,
                                null);
                    } else {
                        ss = Util.convertDomain(domainSet,
                                RealTupleType.SpatialEarth3DTuple, null);
                    }
                    Trace.call2("VRC.convertDomain");
                    float[][] refVals = ss.getSamples(true);
                    float[][] newVals = (latLonOrder)
                            ? refVals
                            : new float[][] {
                            refVals[1], refVals[0], refVals[2]
                    };
                    Trace.call1("VRC.toRef");
                    newVals = cs.toReference(newVals);
                    Trace.call2("VRC.toRef");
                    Trace.call1("VRC.scaleVerticalValues");
                    newVals[2] = mpd.scaleVerticalValues(newVals[2]);
                    Trace.call2("VRC.scaleVerticalValues");
                    GriddedSet xyzSet =
                            GriddedSet.create(RealTupleType.SpatialCartesian3DTuple, newVals,
                                    domainSet.getLengths(),
                                    (CoordinateSystem) null, (Unit[]) null,
                                    (ErrorEstimate[]) null, false, true);
                    float[] lows  = xyzSet.getLow();
                    float[] highs = xyzSet.getHi();
                    Linear3DSet   volumeXYZ = new Linear3DSet(RealTupleType.SpatialCartesian3DTuple, lows[0],
                            //-TDR highs[0], lengths[0], lows[1], highs[1],
                            highs[0], xyResample, lows[1], highs[1],
                            //-TDR lengths[1], lows[2], highs[2], lengths[2]);
                            xyResample, lows[2], highs[2], zResample);
                    int numLocs = volumeXYZ.getLength();

                    if (GridUtil.isConstantSpatialDomain(grid) && GridUtil.getTimeSet(grid) == null) {
                        System.out.println("ConstantSpatialDomain");
                        int[][]  indices = new int[numLocs][];
                        float[][] weights = new float[numLocs][];
                        float[][] xyzLocs = volumeXYZ.getSamples(false);
                        Gridded3DSet domainSet0 = (Gridded3DSet) grid.getDomainSet();
                        domainSet0.valueToInterp(xyzLocs, indices, weights);
                        newGrid = makeLinearGrid(grid, xyzSet, mpd, xyResample, zResample, indices, weights);
                    } else {
                        Set timeSet = GridUtil.getTimeSet(grid);
                        ExecutorService executor = Executors.newFixedThreadPool(8);
                        final List<FieldImpl> result   = new ArrayList<FieldImpl>();
                        List<Future>          pthreads = new ArrayList<Future>();
                        FieldImpl fgrid = GridUtil.setSpatialDomain((FieldImpl)grid.getSample(0), xyzSet);
                        Gridded3DSet domainSet0 = (Gridded3DSet) fgrid.getDomainSet();
                        int[][]  indices = new int[numLocs][];
                        float[][] weights = new float[numLocs][];
                        float[][] xyzLocs = volumeXYZ.getSamples(false);
                        //System.out.println("Time used to resample1 = "
                         //       + (System.currentTimeMillis() - start) / 1000.0);
                        domainSet0.valueToInterp(xyzLocs, indices, weights);
                        //System.out.println("Time used to resample2 = "
                        //        + (System.currentTimeMillis() - start) / 1000.0);
                        Trace.call1("VRC.makeLinearGrid");
                            //int[] lengths = domainSet.getLengths();
                            //Misc.printArray("lengths",lengths);

                        for (int i = 0; i < timeSet.getLength(); i++) {
                            Callable pt = new makeLinearGridThredds((FieldImpl) grid.getSample(i), xyzSet, mpd, xyResample, zResample, indices, weights);
                            Future<Object> future = executor.submit(pt);
                            pthreads.add(future);
                        }
                        for (Future<Object> o : pthreads) {
                            try {
                                result.add((FieldImpl) o.get());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }

                        for (int i = 0; i < timeSet.getLength(); i++) {
                            FieldImpl timeField = result.get(i);
                            //  makeLinearGrid((FieldImpl) grid.getSample(i,
                            //    false), xyzSet, mpd, xyResample, zResample, indices, weights);
                            if (i == 0) {
                                FunctionType ft =
                                        new FunctionType(((SetType) timeSet
                                                .getType()).getDomain(), timeField
                                                .getType());
                                newGrid = new FieldImpl(ft, timeSet);
                            }
                            newGrid.setSample(i, timeField, false);

                        }
                    }
                } catch (VisADException ve) {
                    ve.printStackTrace();
                    userErrorMessage(
                        "Can't render volume for " + paramName
                        + " in this projection. Try using the data projection");
                    newGrid = grid;
                }
            }
            //showNormalCursor();
        }
        Trace.call1("VRC.loadVolumeData.loadData");
        dataMap.put(getSmoothingType()+getSmoothingFactor(), newGrid);
        myDisplay.loadData(newGrid);
        Trace.call2("VRC.loadVolumeData.loadData");
        Trace.call2("loadVolumeData");
    }

    /**
     * Class description
     *
     *
     * @version
     * @author
     */
    static class makeLinearGridThredds implements Callable<FieldImpl> {
        /** _more_ */
        FieldImpl grid;
        /** _more_ */
        GriddedSet xyzSet;
        /** _more_ */
        MapProjectionDisplay mpd;
        /** _more_ */
        int xyResample;
        /** _more_ */
        int zResample;
        /** _more_ */
        int[][]  indices;
        /** _more_ */
        float[][] weights;

        /**
         * _more_
         *
         * @param grid _more_
         * @param xyzSet _more_
         */
        private makeLinearGridThredds(FieldImpl grid, GriddedSet xyzSet, MapProjectionDisplay mpd,
                                      int xyResample,  int zResample, int[][]  indices, float[][] weights) {
            this.grid         = grid;
            this.xyzSet       = xyzSet;
            this.mpd          = mpd;
            this.xyResample   = xyResample;
            this.zResample    = zResample;
            this.indices      = indices;
            this.weights      = weights;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public FieldImpl call() {
            FieldImpl ff = null;
            try {
                ff = makeLinearGrid(grid, xyzSet, mpd, xyResample, zResample, indices, weights);
            } catch (Exception ee){}
            //System.out.println("Thredds = " + i);
            return ff;
        }
    }



    /**
     * Method to call if projection changes.  Subclasses that
     * are worried about such events should implement this.
     */
    public void projectionChanged() {
        //System.out.println("projection changed");
        try {
            loadVolumeData();
        } catch (Exception exc) {
            logException("loading volume data", exc);
        }
        super.projectionChanged();
    }


    /**
     * Set the useTexture3D property
     *
     * @param use  the useTexture3D property
     */
    public void setUseTexture3D(boolean use) {
        useTexture3D = use;
        if (myDisplay != null) {
            try {
                myDisplay.addConstantMap(new ConstantMap(useTexture3D
                        ? GraphicsModeControl.TEXTURE3D
                        : GraphicsModeControl.STACK2D, Display
                            .Texture3DMode));
            } catch (Exception e) {
                logException("setUseTexture3D", e);
            }
        }
    }

    /**
     * Get the useTexture3D property
     *
     * @return the useTexture3D property
     */
    public boolean getUseTexture3D() {
        return useTexture3D;
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
     *  Set the UsePoints property.
     *
     *  @param value The new value for UsePoints
     */
    public void setUsePoints(boolean value) {
        usePoints = value;
    }

    /**
     *  Get the UsePoints property.
     *
     *  @return The UsePoints
     */
    public boolean getUsePoints() {
        return usePoints;
    }


}

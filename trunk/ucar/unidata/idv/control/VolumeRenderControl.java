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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;

import ucar.visad.display.VolumeDisplayable;


import visad.*;

import visad.georef.MapProjection;

import visad.util.SelectRangeWidget;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

public class VolumeRenderControl extends GridDisplayControl {

    /** the display for the volume renderer */
    VolumeDisplayable myDisplay;

    /** the display for the volume renderer */
    boolean useTexture3D = true;

    /** _more_ */
    private boolean usePoints = false;



    /**
     * Default constructor; does nothing.
     */
    public VolumeRenderControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);
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

        //Now, set the data. Return false if it fails.
        if ( !setData(dataChoice)) {
            return false;
        }

        //Now set up the flags and add the displayable 
        return true;
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
     *  Use the value of the skip factor to subset the data.
     */
    protected void applySkipFactor() {
        try {
            showWaitCursor();
            loadVolumeData();
        } catch (Exception exc) {
            logException("loading volume data", exc);
        } finally {
            showNormalCursor();
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
            setProjectionInView(true, true);

            CoordinateSystem cs =
                getNavigatedDisplay().getDisplayCoordinateSystem();
            if ((cs != null)
                    && (getNavigatedDisplay()
                        instanceof MapProjectionDisplay)) {
                try {
                    if (GridUtil.isConstantSpatialDomain(grid)) {
                        newGrid = makeLinearGrid(grid, cs);
                    } else {
                        Set timeSet = GridUtil.getTimeSet(grid);
                        for (int i = 0; i < timeSet.getLength(); i++) {
                            FieldImpl timeField =
                                makeLinearGrid((FieldImpl) grid.getSample(i,
                                    false), cs);
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
        }
        Trace.call1("VRC.loadVolumeData.loadData");
        myDisplay.loadData(newGrid);
        Trace.call2("VRC.loadVolumeData.loadData");
        Trace.call2("loadVolumeData");
    }

    /**
     * Make a grid with a Linear3DSet for the volume rendering
     *
     * @param grid grid to transform
     * @param cs   coordinate system to transform to XYZ
     *
     * @return transformed grid
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   problem creating grid
     */
    private FieldImpl makeLinearGrid(FieldImpl grid, CoordinateSystem cs)
            throws VisADException, RemoteException {

        Trace.call1("VRC.makeLinearGrid");
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
        MapProjectionDisplay mpd =
            (MapProjectionDisplay) getNavigatedDisplay();
        MapProjection mp             = mpd.getMapProjection();
        boolean       mapLatLonOrder = mp.isLatLonOrder();
        //System.out.println("map is latLonOrder " + mapLatLonOrder);
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
        int[] lengths = domainSet.getLengths();
        //Misc.printArray("lengths",lengths);
        GriddedSet xyzSet =
            GriddedSet.create(RealTupleType.SpatialCartesian3DTuple, newVals,
                              domainSet.getLengths(),
                              (CoordinateSystem) null, (Unit[]) null,
                              (ErrorEstimate[]) null, false, true);
        Trace.call1("VRC.setSpatialDomain");
        FieldImpl newGrid = GridUtil.setSpatialDomain(grid, xyzSet);  //, true);
        Trace.call2("VRC.setSpatialDomain");
        float[] lows  = xyzSet.getLow();
        float[] highs = xyzSet.getHi();
        //Misc.printArray("lows",lows);
        //Misc.printArray("highs",highs);
        Linear3DSet volumeXYZ =
            new Linear3DSet(RealTupleType.SpatialCartesian3DTuple, lows[0],
                            highs[0], lengths[0], lows[1], highs[1],
                            lengths[1], lows[2], highs[2], lengths[2]);
        // System.out.println(volumeXYZ);
        Trace.call1("VRC.resampleGrid");
        newGrid = GridUtil.resampleGrid(newGrid, volumeXYZ);
        Trace.call2("VRC.resampleGrid");
        Trace.call2("VRC.makeLinearGrid");
        return newGrid;
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

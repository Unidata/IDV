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
import ucar.unidata.data.point.PointCloudDataSource;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Range;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;

import ucar.visad.display.VolumeDisplayable;
import ucar.visad.display.RGBDisplayable;


import visad.georef.TrivialMapProjection;
import visad.*;

import visad.georef.MapProjection;

import visad.util.SelectRangeWidget;

import java.io.*;

import java.awt.geom.Rectangle2D;
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
public class PointCloudControl extends DisplayControlImpl {

    /** the display for the volume renderer */
    VolumeDisplayable myDisplay;

    /** the display for the volume renderer */
    boolean useTexture3D = true;

    MapProjection projection;

    private int colorRangeIndex = PointCloudDataSource.INDEX_ALT;

    private Range dataRange;


    /**
     * Default constructor; does nothing.
     */
    public PointCloudControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);
    }


    protected int getColorRangeIndex() {
        return colorRangeIndex;
    }

    public Range getColorRangeFromData() {
        if(dataRange!=null) return dataRange;
        return  getColorRangeFromData();
    }


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

        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Point Size:"),
                                             GuiUtils.left(doMakePointSizeWidget())));
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
        if(choice!=null) {
            if ( !super.setData(choice) || (getNavigatedDisplay() == null)) {
                return false;
            }
        }
        loadVolumeData();
        return true;
    }



    /**
     * Load the volume data to the display
     *
     * @throws RemoteException   problem loading remote data
     * @throws VisADException    problem loading the data
     */
    private void loadVolumeData() throws VisADException, RemoteException {
        FlatField points    = (FlatField)getDataInstance().getData();
        float[][]pts = points.getFloats(false);
        if(pts.length == 3) {
            colorRangeIndex = PointCloudDataSource.INDEX_ALT;
        } else {
            colorRangeIndex = pts.length-1;
        }

        float  minX     = Float.POSITIVE_INFINITY;
        float  minY     = Float.POSITIVE_INFINITY;
        float  maxX     = Float.NEGATIVE_INFINITY;
        float  maxY     = Float.NEGATIVE_INFINITY;
        float  minField = Float.POSITIVE_INFINITY;
        float  maxField = Float.NEGATIVE_INFINITY;

        for(int i=0;i<pts[0].length;i++) {
            minX = Math.min(minX, pts[PointCloudDataSource.INDEX_LON][i]);
            maxX = Math.max(maxX, pts[PointCloudDataSource.INDEX_LON][i]);
            minY = Math.min(minY, pts[PointCloudDataSource.INDEX_LAT][i]);
            maxY = Math.max(maxY, pts[PointCloudDataSource.INDEX_LAT][i]);
            if(pts.length==3) {
                maxField = Math.max(maxField, pts[PointCloudDataSource.INDEX_ALT][i]);
                minField = Math.min(minField, pts[PointCloudDataSource.INDEX_ALT][i]);
            } else {
                maxField = Math.max(maxField, pts[3][i]);
                minField = Math.min(minField, pts[3][i]);
            }
        }
        dataRange = new Range(minField, maxField);

        float width = Math.max((maxX - minX),  (maxY - minY));
        float height = Math.max((maxY - minY),  (maxY - minY));
        Rectangle2D.Float rect = new Rectangle2D.Float( minX, minY,  width, height);

        projection =  new TrivialMapProjection(
                                               RealTupleType.SpatialEarth2DTuple, rect);
        System.err.println("type1:" + points.getType());
        myDisplay.loadData(points,colorRangeIndex);
    }



    /**
     * Is this a raster display
     *
     * @return true
     */
    public boolean getIsRaster() {
        return true;
    }




}

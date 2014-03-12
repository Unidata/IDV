/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.*;
import ucar.unidata.data.imagery.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.*;

import ucar.unidata.view.geoloc.GlobeDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;

import ucar.visad.display.RubberBandBox;

import visad.*;

import visad.georef.EarthLocation;

import java.awt.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 * Class for controlling the display of images.  Designed for brightness
 * images with range of 0 to 255.
 *
 * @author IDV Development Group
 */
public class ImagePlanViewControl extends PlanViewControl {

    /** The label to show the readout in the side legend */
    private JLabel sideLegendReadout;

    /** _more_ */
    public List descripters;

    //  NB: For now, we don't subclass ColorPlanViewControl because we get
    //  the DataRange widget from getControlWidgets.  Might want this in
    //  the future.  It would be simpler if we wanted to include that.

    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public ImagePlanViewControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                          | FLAG_SKIPFACTOR | FLAG_TEXTUREQUALITY);
    }

    /**
     * @override
     *
     * @return _more_
     */
    protected boolean canDoProgressiveResolution() {
        return true;
    }


    /**
     * _more_
     *
     * @param legendType _more_
     *
     * @return _more_
     */
    protected JComponent getExtraLegendComponent(int legendType) {
        JComponent parentComp = super.getExtraLegendComponent(legendType);
        if (legendType == BOTTOM_LEGEND) {
            return parentComp;
        }

        if (sideLegendReadout == null) {
            sideLegendReadout = new JLabel("<html><br></html>");
        }

        return GuiUtils.vbox(sideLegendReadout, parentComp);
    }
    /**
     * Method to create the particular <code>DisplayableData</code> that
     * this this instance uses for data depictions.
     * @return Contour2DDisplayable for this instance.
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable gridDisplay =
            new Grid2DDisplayable("ImagePlanViewControl_"
                                  + ((datachoice != null)
                                     ? datachoice.toString()
                                     : ""), true);
        gridDisplay.setTextureEnable(true);
        gridDisplay.setCurvedSize(getTextureQuality());
        /* TODO: Find out why this causes redisplays
        if (BaseImageControl.EMPTY_IMAGE != null) {
            gridDisplay.loadData(BaseImageControl.EMPTY_IMAGE);
        }
        */
        //gridDisplay.setUseRGBTypeForSelect(true);
        addAttributedDisplayable(gridDisplay);
        return gridDisplay;
    }

    /**
     *  Use the value of the texture quality to set the value on the display
     *
     * @throws RemoteException  problem with Java RMI
     * @throws VisADException   problem setting attribute on Displayable
     */
    protected void applyTextureQuality()
            throws VisADException, RemoteException {
        if (getGridDisplay() != null) {
            getGridDisplay().setCurvedSize(getTextureQuality());
        }
    }

    /**
     * Called to initialize this control from the given dataChoice;
     * sets levels controls to match data; make data slice at first level;
     * set display's color table and display units.
     *
     * @param dataChoice  choice that describes the data to be loaded.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        boolean fromBundle = getIdv().getStateManager().getProperty(
                                 IdvConstants.PROP_LOADINGXML, false);
        if (fromBundle) {
            if (descripters != null) {
                dataChoice.setObjectProperty("descriptors", descripters);
            }
            boolean result = super.setData(dataChoice);
            if ( !result) {
                userMessage("Selected image(s) not available");
            }
            String magStr = (String) dataChoice.getProperty("MAG");
            if (magStr != null) {
                if (sideLegendReadout == null) {
                    sideLegendReadout = new JLabel("<html><br></html>");
                }
                sideLegendReadout.setText("<html>" + magStr + "</html>");
            }
            return result;
        }

        boolean hasCorner =
            dataSelection.getProperty(DataSelection.PROP_HASCORNER, false);

        boolean result = super.setData(dataChoice);
        if ( !result) {
            if (hasCorner) {
                userMessage("Selected region bounding box is not big enough");
            } else {
                userMessage("Selected image(s) not available");
            }
        }
        //sideLegendReadout
        DataChoice dc0 = null;
        if (dataChoice instanceof DerivedDataChoice) {
            dc0 = (DataChoice) ((DerivedDataChoice) dataChoice).getChoices().get(0);
        } else {
            dc0 = dataChoice;
        }
        //save imagelist
        descripters = getImageDescriptors(dataChoice);
        String magStr = (String) dc0.getProperty("MAG");
        if (magStr != null) {
            if (sideLegendReadout == null) {
                sideLegendReadout = new JLabel("<html><br></html>");
            }
            sideLegendReadout.setText("<html>" + magStr + "</html>");
        }
        return result;

    }


    /**
     * _more_
     *
     * @param dc _more_
     *
     * @return _more_
     */
    protected List getImageDescriptors(DataChoice dc) {
        List dataSources = new ArrayList();

        dc.getDataSources(dataSources);
        if(!(dataSources.get(0) instanceof AddeImageDataSource))
            return null;

        AddeImageDataSource aids = (AddeImageDataSource) dataSources.get(0);

        return aids.getDescriptorsToUse();
    }

    /**
     * Get the initial color table for the data
     *
     * @return  intitial color table
     */
    protected ColorTable getInitialColorTable() {
        ColorTable colorTable = super.getInitialColorTable();
        if (colorTable.getName().equalsIgnoreCase("default")) {
            colorTable = getDisplayConventions().getParamColorTable("image");
        }
        return colorTable;
    }

    /**
     * Return the color display used by this object.  A wrapper
     * around {@link #getPlanDisplay()}.
     * @return this instance's Grid2Ddisplayable.
     * @see #createPlanDisplay()
     */
    Grid2DDisplayable getGridDisplay() {
        return (Grid2DDisplayable) getPlanDisplay();
    }


    /**
     * Get whether this display should allow smoothing
     * @return true if allows smoothing.
     */
    public boolean getAllowSmoothing() {
        return false;
    }


    /**
     * Get the initial range for the data and color table.
     * Optimized for brightness images with range of 0 to 255.
     *
     * @return  initial range
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {

        // WARNING:  Twisty-turny logic below
        // try for the parameter.
        Range range = getDisplayConventions().getParamRange(paramName,
                          getDisplayUnit());

        // see if one is defined for the color table.
        if (range == null) {
            range = getRangeFromColorTable();
            if ((range != null) && (range.getMin() == range.getMax())) {
                range = null;
            }
        }

        // look for the default for "image" - hopefully it never changes
        boolean usingImage = false;
        Range imageRange = getDisplayConventions().getParamRange("image",
                               getDisplayUnit());
        /*
        if (range == null) {
            range = imageRange;
        }
        */
        if ((range != null) && Misc.equals(range, imageRange)) {
            usingImage = true;
        }

        // check to see if the range of the data is outside the range
        // of the default. This will be wrong if someone redefined what image
        // is supposed to be (0-255).
        if ((range != null) && usingImage
                && (getGridDataInstance() != null)) {
            Range dataRange = getDataRangeInColorUnits();
            if (dataRange != null) {
                if ((range.getMin() > dataRange.getMin())
                        || (range.getMax() < dataRange.getMax())) {
                    range = dataRange;
                }
            }
        }
        if (range == null) {
            range = super.getInitialRange();
        }
        return range;
    }

    /**
     * Get the slice for the display
     *
     * @param slice  slice to use
     *
     * @return slice with skip value applied
     *
     * @throws VisADException  problem subsetting the slice
     */
    protected FieldImpl getSliceForDisplay(FieldImpl slice)
            throws VisADException {
        checkImageSize(slice);
        return super.getSliceForDisplay(slice);
    }

    /**
     * Return the label that is to be used for the skip widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the line width widget
     */
    public String getSkipWidgetLabel() {
        return "Pixel Sampling";
    }

    /**
     * What label to use for the data projection
     *
     * @return label
     */
    protected String getDataProjectionLabel() {
        return "Use Native Image Projection";
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
     * _more_
     *
     * @return _more_
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
     * _more_
     *
     * @param descripters _more_
     */
    public void setDescripters(List descripters) {
        this.descripters = descripters;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getDescripters() {
        return this.descripters;
    }

}

/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;

import visad.FieldImpl;
import visad.VisADException;


import java.rmi.RemoteException;


/**
 * Class for controlling the display of images.  Designed for brightness
 * images with range of 0 to 255.
 *
 * @author IDV Development Group
 */
public class ImagePlanViewControl extends PlanViewControl {

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
        boolean result = super.setData(dataChoice);
        if ( !result) {
            userMessage("Selected image(s) not available");
        }
        return result;
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

}

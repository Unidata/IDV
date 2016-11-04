/*
 * Copyright 1997-2016 Unidata Program Center/University Corporation for
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
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.util.*;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;

import visad.*;
import visad.VisADException;

import visad.meteorology.ImageSequenceImpl;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Class for controlling the display of images.  Designed for brightness
 * images with range of 0 to 255.
 *
 * @author IDV Development Group
 */
public class ImagePlanViewControl extends PlanViewControl {

    /** _more_ */
    public List descripters;

    /** _more_ */
    private McVHistogramWrapper histoWrapper;

    /** _more_ */
    private FlatField image;

    /** _more_ */
    private DataSourceImpl dataSource;

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
     * _more_
     */
    private void setInitialHistogramRange() {
        try {
            Range  range = getRange();
            double lo    = range.getMin();
            double hi    = range.getMax();
            histoWrapper.setHigh(hi);
            histoWrapper.setLow(lo);
        } catch (Exception exc) {
            logException("setInitialHistogramRange", exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Container doMakeContents() {
        try {
            JTabbedPane tab = new MyTabbedPane();
            tab.add("Settings",
                    GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()),
                                   5));

            // MH: just add a dummy component to this tab for now..
            //            don't init histogram until the tab is clicked.
            tab.add("Histogram", getHistogramTabComponent());

            return tab;
        } catch (Exception exc) {
            logException("doMakeContents", exc);
        }
        return null;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getHistogramTabComponent() {
        List choices = new ArrayList();
        if (datachoice == null) {
            datachoice = getDataChoice();
        }
        choices.add(datachoice);
        histoWrapper = new McVHistogramWrapper("histo", choices,
                (DisplayControlImpl) this);
        dataSource = getDataSource();

        if (dataSource == null) {
            try {
                image = (FlatField) datachoice.getData(getDataSelection());
                histoWrapper.loadData(image);
            } catch (IllegalArgumentException e) {
                logException("Could not create histogram: nothing to show!",
                             e);
            } catch (RemoteException | VisADException e) {
                logException("Could not create histogram!", e);
            }
        } else {
            Hashtable props = dataSource.getProperties();
            try {
                DataSelection testSelection = datachoice.getDataSelection();
                DataSelection realSelection = getDataSelection();
                if (testSelection == null) {
                    datachoice.setDataSelection(realSelection);
                }
                ImageSequenceImpl seq = null;
                if (dataSelection == null) {
                    dataSelection = dataSource.getDataSelection();
                }
                if (dataSelection == null) {
                    image = (FlatField) dataSource.getData(datachoice, null,
                            props);
                    if (image == null) {
                        image = (FlatField) datachoice.getData(null);
                    }
                } else {
                    Data data = dataSource.getData(datachoice, null,
                                    dataSelection, props);
                    if (data instanceof ImageSequenceImpl) {
                        seq = (ImageSequenceImpl) data;
                    } else if (data instanceof FlatField) {
                        image = (FlatField) data;
                    } else if (data instanceof FieldImpl) {
                        image = (FlatField) ((FieldImpl) data).getSample(0,
                                false);
                    } else {
                        throw new Exception(
                            "Histogram must be made from a FlatField");
                    }
                }
                if ((seq != null) && (seq.getImageCount() > 0)) {
                    image = (FlatField) seq.getImage(0);
                }
                try {
                    histoWrapper.loadData(image);
                } catch (IllegalArgumentException e) {
                    logException(
                        "Could not create histogram: nothing to show!", e);
                }
            } catch (Exception e) {
                logException("attempting to set up histogram", e);
            }
        }

        JComponent histoComp   = histoWrapper.doMakeContents();
        JButton    resetButton = new JButton("Update");
        resetButton.addActionListener(new ActionListener() {
                                          public void actionPerformed(
                                          ActionEvent ae) {
                                              getIdv().showWaitCursor();
                                              updateHistogramPanel();
                                              setInitialHistogramRange();
                                              getIdv().clearWaitCursor();
                                              //resetColorTable();
                                          }
                                      });
        JPanel resetPanel =
            GuiUtils.center(GuiUtils.inset(GuiUtils.wrap(resetButton),
                                           4));
        return GuiUtils.centerBottom(histoComp, resetPanel);
    }

    /**
     * _more_
     */
    public void updateHistogramPanel() {
        Hashtable props = dataSource.getProperties();
        try {
            DataSelection testSelection = datachoice.getDataSelection();
            DataSelection realSelection = getDataSelection();
            if (testSelection == null) {
                datachoice.setDataSelection(realSelection);
            }
            ImageSequenceImpl seq = null;
            if (dataSelection == null) {
                dataSelection = dataSource.getDataSelection();
            }
            if (dataSelection == null) {
                image = (FlatField) dataSource.getData(datachoice, null,
                        props);
                if (image == null) {
                    image = (FlatField) datachoice.getData(null);
                }
            } else {
                Data data = dataSource.getData(datachoice, null,
                                dataSelection, props);
                if (data instanceof ImageSequenceImpl) {
                    seq = (ImageSequenceImpl) data;
                } else if (data instanceof FlatField) {
                    image = (FlatField) data;
                } else if (data instanceof FieldImpl) {
                    image = (FlatField) ((FieldImpl) data).getSample(0,
                            false);
                } else {
                    throw new Exception(
                        "Histogram must be made from a FlatField");
                }
            }
            if ((seq != null) && (seq.getImageCount() > 0)) {
                image = (FlatField) seq.getImage(0);
            }
            try {
                histoWrapper.loadData(image);
            } catch (IllegalArgumentException e) {
                logException("Could not create histogram: nothing to show!",
                             e);
            }
        } catch (Exception e) {
            logException("attempting to set up histogram", e);
        }
        ;
    }


    /**
     * _more_
     *
     * @param items _more_
     * @param forMenuBar _more_
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        super.getViewMenuItems(items, forMenuBar);
        if (true) {
            items.add(GuiUtils.MENU_SEPARATOR);
            items.add(GuiUtils.makeMenuItem("Save Chart Image...",
                                            getChart(),
                                            "saveImage"));
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public McVHistogramWrapper getChart() {
        if (histoWrapper == null) {
            getHistogramTabComponent();
            setInitialHistogramRange();
        }
        return histoWrapper;
    }

    /**
     * _more_
     *
     * @param newRange _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setRange(final Range newRange)
            throws RemoteException, VisADException {
        //        logger.trace("newRange: {} [avoiding NPE!]", newRange);
        super.setRange(newRange);
        if (histoWrapper != null) {
            histoWrapper.modifyRange(newRange.getMin(), newRange.getMax(),
                                     false);
        }
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
            //if (descripters != null) {
            //   dataChoice.setObjectProperty("descriptors", descripters);
            //}
            boolean mdr = getMatchDisplayRegion();
            dataChoice.setProperty("MatchDisplayRegion", mdr);
            boolean result = super.setData(dataChoice);
            if ( !result) {
                userMessage("Selected image(s) not available");
            }
            String magStr = (String) dataChoice.getProperty("MAG");
            if ((magStr != null) && !magStr.isEmpty()) {
                resolutionReadout = magStr;
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
        //save imagelist
        //descripters = getImageDescriptors(dataChoice);
        return result;

    }


    /**
     * _more_
     *
     * @param dc _more_
     *
     * @return _more_
     */
    /* protected List getImageDescriptors(DataChoice dc) {
        List dataSources = new ArrayList();

        dc.getDataSources(dataSources);
        if(!(dataSources.get(0) instanceof AddeImageDataSource))
            return null;

        AddeImageDataSource aids = (AddeImageDataSource) dataSources.get(0);

        return aids.getDescriptorsToUse();
    } */

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
        if ((range != null)
                && usingImage
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

    /**
     * Holds a JFreeChart histogram of image values.
     */
    private class MyTabbedPane extends JTabbedPane implements ChangeListener {

        /** Have we been painted */
        boolean painted = false;

        /** _more_ */
        boolean popupFlag = false;

        /** _more_ */
        boolean haveDoneHistogramInit = false;

        /**
         * Creates a new {@code MyTabbedPane} that gets immediately registered
         * as a {@link javax.swing.event.ChangeListener} for its own events.
         */
        public MyTabbedPane() {
            addChangeListener(this);
        }

        /**
         * The histogram isn't created unless the user selects the histogram
         * tab (this is done in an effort to avoid a spike in memory usage).
         *
         * @param e The event. Ignored for now.
         */
        public void stateChanged(ChangeEvent e) {
            // MH: don't make the histogram until user clicks the tab.
            int index = getSelectedIndex();
            if ((index >= 0)
                    && getTitleAt(index).equals("Histogram")
                    && !haveDoneHistogramInit) {
                getIdv().showWaitCursor();
                this.setComponentAt(index,
                                    GuiUtils.inset(getHistogramTabComponent(),
                                            5));
                setInitialHistogramRange();
                getIdv().clearWaitCursor();
                //                haveDoneHistogramInit = true;
            }
        }

        /**
         * MH: Not really doing anything useful...but will leave it here for now...
         *
         * @param flag _more_
         */
        private void setPopupFlag(boolean flag) {
            this.popupFlag = flag;
        }

        /**
         * MH: Not really doing anything useful...but will leave it here for now...
         *
         * @param g graphics
         */
        public void paint(java.awt.Graphics g) {
            if ( !painted) {
                painted = true;
            }
            super.paint(g);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DataSourceImpl getDataSource() {
        DataSourceImpl ds          = null;
        List           dataSources = getDataSources();
        if ( !dataSources.isEmpty()) {
            ds = (DataSourceImpl) dataSources.get(0);
        }
        return ds;
    }

    /**
     * _more_
     */
    public void resetColorTable() {
        try {
            revertToDefaultColorTable();
            revertToDefaultRange();
            histoWrapper.resetPlot();
        } catch (Exception e) {
            logException("problem resetting color table", e);
        }
    }

}

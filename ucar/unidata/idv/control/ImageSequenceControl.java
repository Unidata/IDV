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


import ucar.unidata.data.CompositeDataChoice;

import ucar.unidata.data.DataCancelException;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.IdvConstants;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;


import ucar.unidata.util.Resource;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;
import ucar.visad.display.ColorScale;
import ucar.visad.display.ImageSequenceDisplayable;

import visad.*;
import visad.FunctionType;
import visad.RealTupleType;
import visad.RealType;

import visad.georef.MapProjection;

import visad.meteorology.ImageSequence;

import visad.meteorology.ImageSequenceManager;
import visad.meteorology.SingleBandedImage;
import visad.meteorology.SingleBandedImageImpl;

import visad.util.BaseRGBMap;
import visad.util.ColorPreview;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.event.*;


/**
 * A DisplayControl for handling image sequences
 *
 * @author Don Murray
 * @version $Revision: 1.128 $ $Date: 2007/08/09 17:21:32 $
 */
public class ImageSequenceControl extends BaseImageControl {

    /** the displayable for the sequence */
    private ImageSequenceDisplayable sequenceDisplay;

    /** sequence manager for the data */
    private ImageSequenceManager sequenceManager;

    /** the data choice */
    private DataChoice choice;

    /** a progress bar */
    private JProgressBar progressBar;

    /** a progress panel (to hold the bar) */
    private JPanel progressPanel;

    /** flag for whether to keep running or not */
    private boolean keepRunning;

    /** the next id */
    private static int nextId = 0;

    /** my version of next id */
    private int myId;

    /** mutex object for locking */
    private static Object INSTANCE_MUTEX = new Object();

    /** working sequence */
    private FieldImpl workingSequence;

    /** flag for showing the progress bar */
    private boolean showProgressBar = true;

    /** private unit of data */
    private Unit myUnit;



    /** flag for whether displayable was added */
    private boolean sequenceDisplayAdded = false;

    /** flag for whether loading from bundle */
    private boolean loadingFromBundle = false;


    /**
     * Default ctor; sets the attribute flags
     */
    public ImageSequenceControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT | FLAG_ZPOSITION
                          | FLAG_SKIPFACTOR | FLAG_TEXTUREQUALITY);
    }


    /**
     * Called to make this kind of Display Control; also calls code to
     * made the Displayable.
     * This method is called from inside DisplayControlImpl init(several args).
     *
     * @param choice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice choice)
            throws VisADException, RemoteException {
        return init(choice, true);
    }

    /**
     * Called to make this kind of Display Control; also calls code to
     * made the Displayable.
     * This method is called from inside DisplayControlImpl init(several args).
     *
     * @param choice the DataChoice of the moment.
     * @param doLoad  load the data if true
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean init(DataChoice choice, boolean doLoad)
            throws VisADException, RemoteException {
        //Make the window here so it is being displayed to the user
        if ( !setData(choice)) {
            return false;
        }
        doMakeWindow();
        this.choice     = choice;

        sequenceManager = new ImageSequenceManager();
        sequenceDisplay = new ImageSequenceDisplayable("Images", null,
                getInitialColorTable().getAlphaTable());
        sequenceDisplay.setTextureEnable(true);
        sequenceDisplay.setCurvedSize(getTextureQuality());
        if (EMPTY_IMAGE != null) {
            sequenceDisplay.loadData(EMPTY_IMAGE);
        }

        if ( !getDisplayVisibility()) {
            sequenceDisplay.setVisible(false);

        }
        addAttributedDisplayable(sequenceDisplay,
                                 FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                                 | FLAG_ZPOSITION);

        if (sequenceDisplay.hasData()) {
            addDisplayable(sequenceDisplay);
            sequenceDisplayAdded = true;
        }

        loadingFromBundle = getIdv().getStateManager().getProperty(
            IdvConstants.PROP_LOADINGXML, false);

        if (doLoad) {
            if (true) {
                showWaitCursor();
                Misc.run(new Runnable() {
                    public void run() {
                        try {
                            loadData();
                        } catch (Exception exc) {
                            logException("Loading data", exc);
                        }
                        showNormalCursor();
                    }
                });
            } else {
                showWaitCursor();
                try {
                    Trace.call1("ImageControl.loadData");
                    loadData();
                    Trace.call2("ImageControl.loadData");
                } catch (Exception exc) {
                    logException("Loading data", exc);
                }
                showNormalCursor();
            }
        }


        return true;
    }

    /**
     *  Use the value of the texture quality to set the value on the display
     *
     * @throws RemoteException  problem with Java RMI
     * @throws VisADException   problem setting attribute on Displayable
     */
    protected void applyTextureQuality()
            throws VisADException, RemoteException {
        if (sequenceDisplay != null) {
            sequenceDisplay.setCurvedSize(getTextureQuality());
        }
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
     * Get the unit for the data display.
     * @return  unit to use for displaying the data
     */
    public Unit getDisplayUnit() {
        Unit unit = super.getDisplayUnit();
        if (unit == null) {
            setDisplayUnit(unit = getDisplayUnit(myUnit));
        }
        return unit;
    }

    /**
     * Called after the init method.
     */
    public void initDone() {
        super.initDone();
    }

    /**
     * Make the UI contents for this control.  Override super class to
     * add in special components.
     * @return   the UI container
     */
    protected Container doMakeContents() {
        JButton cancelButton =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Exit16.gif", this,
                                     "cancelPressed");
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setString("Selected images not available");
        progressBar.setStringPainted(true);
        progressPanel = GuiUtils.leftCenter(cancelButton, progressBar);
        return getShowProgressBar()
               ? GuiUtils.topCenter(progressPanel, doMakeWidgetComponent())
               : doMakeWidgetComponent();
    }

    /**
     * Cancel button was pressed
     */
    public void cancelPressed() {
        keepRunning = false;
    }


    /**
     * Get the initial range for the data and color table.
     * @return  initial range
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
        Range range = getDisplayConventions().getParamRange(paramName,
                          getDisplayUnit());
        //Don't do this for now
        if (range == null) {
            range = getRangeFromColorTable();
            if ((range != null) && (range.getMin() == range.getMax())) {
                range = null;
            }
        }

        if (range == null) {
            range = getDisplayConventions().getParamRange("image",
                    getDisplayUnit());
        }
        if (range == null) {
            return new Range(0, 255);
        }
        return range;
    }

    /**
     * Override base class method. Do nothing here. The default implementation
     * ends up calling getData. We want to do that in loadData to provide
     * adequate visual feedback to the user.
     *
     * @param di    data instance
     * @return  true
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean checkIfDataOk(DataInstance di)
            throws VisADException, RemoteException {
        return true;
    }


    /**
     * Reset the data
     *
     * @throws RemoteException Badness
     * @throws VisADException Badness
     */
    protected void resetData() throws VisADException, RemoteException {
        super.resetData();
        loadData(false);
    }

    /**
     * Load data into the control
     */
    private void loadData() {
        loadData(true);
    }

    /**
     * Load data
     *
     * @param firsttime  true if this has not already been done
     */
    private void loadData(boolean firsttime) {
        Trace.call1("ImageControl.loadData");
        try {
            loadDataInner(firsttime);
        } catch (Exception exc) {
            logException("Loading image data", exc);
            try {
                doRemove();
            } catch (Exception e) {}
        }
        Trace.call2("ImageControl.loadData");
    }


    /**
     * Make the data instance
     *
     * @param dataChoice the choice
     *
     * @return the data instance
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        return new GridDataInstance(dataChoice, getDataSelection(),
                                    getRequestProperties(), EMPTY_IMAGE);

    }


    /**
     * Inner load data method which does all the dirty work.
     *
     * @param firsttime  true if this has not already been done
     */
    private void loadDataInner(boolean firsttime) {

        if (firsttime) {
            synchronized (INSTANCE_MUTEX) {
                myId = nextId++;
            }
        }
        // System.out.println("loadDataInner: first = " + firsttime + " myId = " + myId);
        keepRunning = true;
        boolean hasComposite     = (choice instanceof CompositeDataChoice);
        boolean hasUserSelection = false;
        List    choices          = hasComposite
                                   ? ((CompositeDataChoice) choice)
                                       .getDataChoices()
                                   : Misc.newList(choice);

        List    selectionTimes   = getDataSelection().getTimes();
        if (selectionTimes != null) {
            hasUserSelection = true;
        }


        if (selectionTimes == null) {
            selectionTimes = choice.getSelectedDateTimes();
        }
        //        System.err.println("data selection times " + selectionTimes);

        DataSelection selectionToUse = null;
        if (hasComposite) {
            //If we have a composite then we handle our own subsetting here, so
            //we create a DataSelection that says to use the first index
            //to trick the ImageDataSource 
            selectionToUse = new DataSelection(Misc.newList(new Integer(0)));
        } else {
            selectionToUse = new DataSelection(getDataSelection());
        }

        if (selectionTimes != null) {
            selectionTimes = TwoFacedObject.getIdList(selectionTimes);
        }

        int numImages = 1;
        if ((selectionTimes != null) && (selectionTimes.size() > 0)) {
            numImages = selectionTimes.size();
        } else if (choice instanceof CompositeDataChoice) {
            numImages = ((CompositeDataChoice) choice).getNumChildren();
        }

        sequenceManager.clearSequence();
        progressBar.setValue(0);
        progressBar.setMaximum(numImages);
        int           displayedCnt         = 1;
        FunctionType  newType              = null;
        ImageSequence sequence             = null;
        boolean       loadedOnlyOne        = false;

        boolean       haveSetImageSequence = false;


        for (int dataChoiceIdx = 0; dataChoiceIdx < choices.size();
                dataChoiceIdx++) {
            if ( !keepRunning) {
                progressBar.setValue(0);
                progressBar.setString("Loading cancelled");
                break;
            }
            try {
                //see if we have been removed
                if ( !getActive()) {
                    return;
                }

                //TODO: Don't call getData directly. Rather, create a 
                //GridDataInstance and add this control as a listener so 
                // we can pick up any changes to the original data

                DataChoice dataChoice =
                    (DataChoice) choices.get(dataChoiceIdx);

                //Check if we ignore this one
                if (hasComposite && (selectionTimes != null)
                        && (selectionTimes.size() > 0)) {
                    if (selectionTimes.indexOf(new Integer(dataChoiceIdx))
                            == -1) {
                        continue;
                    }
                }

                progressBar.setString("Loading " + displayedCnt + " of "
                                      + numImages + " images");
                Data theData = null;
                Trace.call1("ImageControl.getData");
                try {
                    theData = dataChoice.getData(selectionToUse,
                            getRequestProperties());
                } catch (VisADException ve) {
                    System.out.println(ve);
                }
                Trace.call2("ImageControl.getData");


                //The image may be null which implies that the DataSource 
                //had a subset of times selected for it - with the current 
                //DataChoice is not in the set.
                if (theData == null) {
                    continue;
                }

                //see again if we have been removed
                if ( !getActive()) {
                    return;
                }


                SingleBandedImage image = (SingleBandedImage) theData;

                if (displayedCnt == 1) {
                    FunctionType imageType = (FunctionType) image.getType();
                    RealType rangeType =
                        (RealType) imageType.getFlatRange().getComponent(0);
                    myUnit = visad.util.DataUtility.getRangeUnits(
                        (FlatField) image)[0];
                    //System.out.println("myUnit = " + myUnit);
                    setDisplayUnit(myUnit);
                    newType = new FunctionType(
                        imageType.getDomain(),
                        new RealTupleType(
                            DataUtil.makeRealType(
                                rangeType.getName() + "_Sequence_" + myId,
                                rangeType.getDefaultUnit())));
                }

                Trace.call1("ImageControl-changeMathType");
                // image = (SingleBandedImage) image.changeMathType(newType);
                image =
                    new SingleBandedImageImpl((FlatField) Util.clone(image,
                        newType, false, false), image.getStartTime(),
                            image.getDescription(), false /* don't copy */);
                Trace.call2("ImageControl-changeMathType");

                sequence = sequenceManager.addImageToSequence(image);
                progressBar.setValue(displayedCnt);
                progressBar.setString("Loaded " + displayedCnt + " of "
                                      + numImages + " images");
                if (displayedCnt == 1) {
                    checkImageSize((FieldImpl) image);
                    //Load the dataInstance with some example data because 
                    //when we add the Displayable to the ViewManager it 
                    //will call dataInstance.getData to find out any auto 
                    //projections to do.
                    setDisplayInactive();
                    getDataInstance().setTheData(theData);
                    Trace.call1("ImageControl-addingDisplayable");

                    if ( !sequenceDisplayAdded) {
                        if (firsttime) {
                            sequenceDisplay.setImageSequence(
                                getWorkingSequence((FieldImpl) sequence));
                            haveSetImageSequence = true;
                        }
                        addDisplayable(sequenceDisplay);
                    } else {  // change the map projection first
                        if (firsttime) {
                            MapProjection mp =
                                GridUtil.getNavigation((FieldImpl) sequence);
                            MapViewManager mvm = getMapViewManager();
                            if ((mp != null) && (mvm != null)
                                    && !loadingFromBundle) {
                                mvm.setMapProjection(mp, true,
                                        getDisplayConventions()
                                            .getMapProjectionLabel(mp,
                                                this), true);
                            }
                            sequenceDisplay.setImageSequence(
                                getWorkingSequence((FieldImpl) sequence));
                            haveSetImageSequence = true;
                        }
                    }
                    updateLegendAndList();
                    Trace.call2("ImageControl-addingDisplayable");
                    loadedOnlyOne = true;
                    setDisplayActive();
                } else {
                    loadedOnlyOne = false;
                }
                displayedCnt++;
            } catch (DataCancelException dce) {
                try {
                    doRemove();
                } catch (Exception exc) {
                    logException("Calling doRemove", exc);
                }
                return;
            } catch (Exception e) {
                logException("Creating image sequence", e);
                displayControlFailed();
                return;
            }
        }

        if (sequence != null) {
            if ( !haveSetImageSequence) {
                try {
                    sequenceDisplay.setImageSequence(
                        getWorkingSequence((FieldImpl) sequence));
                } catch (Exception e) {
                    logException("Creating image sequence", e);
                }
            }

            try {
                workingSequence = (FieldImpl) sequence;
                if ( !loadedOnlyOne) {
                    sequenceDisplay.setImageSequence(
                        getWorkingSequence((FieldImpl) sequence));
                }
                if (getSkipValue() != 0) {
                    applySkipFactor();
                }
                skipSlider.setEnabled(true);
            } catch (Exception e) {
                logException("Loading image sequence", e);
                displayControlFailed();
            }
            updateLegendAndList();
            notifyViewManagersOfChange();
        } else {
            userMessage("Selected image(s) not available");
        }

        keepRunning = false;
        if ((progressPanel.getParent() != null) && getShowProgressBar()) {
            Container parent = progressPanel.getParent();
            parent.remove(progressPanel);
            parent.repaint();
            parent.validate();
        }

    }

    /**
     * Called when Datasource is removed.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doRemove() throws VisADException, RemoteException {
        super.doRemove();
        if (sequenceManager != null) {
            sequenceManager.clearSequence();
        }
        sequenceManager = null;
        workingSequence = null;
    }

    /**
     * Set the working sequence for the data.
     *
     * @param sequence   sequence to use
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void setWorkingSequence(FieldImpl sequence)
            throws VisADException, RemoteException {
        workingSequence = sequence;
        sequenceDisplay.setImageSequence(workingSequence);
    }


    /**
     * Apply the skip factor if set
     *
     * @param sequence the image
     *
     * @return the image or its decimated version
     *
     * @throws VisADException On badness
     */
    private FieldImpl getWorkingSequence(FieldImpl sequence)
            throws VisADException {
        if (getSkipValue() <= 0) {
            return sequence;
        }
        return GridUtil.subset(
            (FieldImpl) sequenceManager.getImageSequence(),
            getSkipValue() + 1);
    }


    /**
     *  Use the value of the skip factor JSLider to subset the data.
     */
    protected void applySkipFactor() {
        if (sequenceManager.getImageSequence() == null) {
            return;
        }
        try {
            setWorkingSequence(
                GridUtil.subset(
                    (FieldImpl) sequenceManager.getImageSequence(),
                    getSkipValue() + 1));
        } catch (Exception ve) {
            logException("applySkipFactor", ve);
        }
    }


    /**
     *  Return the range attribute of the colorTable  (if non-null)
     *  else return null;
     * @return The range from the color table attribute
     */
    public Range getColorRangeFromData() {
        try {
            if ((sequenceDisplay != null)
                    && (sequenceDisplay.getData() != null)) {
                return convertColorRange(
                    GridUtil.getMinMax(
                        (FieldImpl) sequenceDisplay.getData())[0], myUnit);
            }
        } catch (VisADException ve) {}
        catch (RemoteException ve) {}
        return null;
    }

    /**
     * Set the alpha
     *
     *
     * @param newAlpha new value
     */
    protected void setAlphaFromSlider(float newAlpha) {
        try {
            super.setAlphaFromSlider(newAlpha);
            //            if (sequenceDisplay != null) {
            //                sequenceDisplay.setAlpha(newAlpha);
            //            }
        } catch (Exception e) {
            logException("Setting alpha value", e);
        }
    }


    /**
     * Set whether we should show the progress bar or not
     * @param value true to show the progress bar
     */
    public void setShowProgressBar(boolean value) {
        showProgressBar = value;
    }


    /**
     * Should we show the progress bar or not
     * @return true to show the progress bar
     */
    public boolean getShowProgressBar() {
        return showProgressBar;
    }

    /**
     * is this display a raster display. Used for writing svg
     *
     * @return is raster
     */
    public boolean getIsRaster() {
        return true;
    }

}

/*
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.*;

import ucar.unidata.util.ContourInfo;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Range;
import ucar.visad.display.*;

import visad.*;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;

import java.awt.*;


import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * A crosssection control for displaying contours.
 *
 * @author IDV Development Team
 * @version $Revision: 1.31 $
 */
public class ContourCrossSectionControl extends CrossSectionControl {

    /** flag for color file */
    boolean isColorFill = false;

   // MyContourCrossSectionControl myContourCrossSectionControl;
    /**
     * Default constructor.
     */
    public ContourCrossSectionControl() {}

    /**
     * Create the display for the ancillary control window.
     *
     * @return  Displayable for data depiction
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected DisplayableData createVCSDisplay()
            throws VisADException, RemoteException {
        Contour2DDisplayable display = new Contour2DDisplayable("vcs_"
                                           + paramName, true, isColorFill);
        addAttributedDisplayable(display,
                                 FLAG_CONTOUR | FLAG_COLORTABLE
                                 | FLAG_DISPLAYUNIT | FLAG_SMOOTHING);
        return display;
    }

    /**
     * Create the display for the vertical cross section in the main
     * window.
     *
     * @return  Displayable for the data depiction
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected DisplayableData createXSDisplay()
            throws VisADException, RemoteException {
        Contour2DDisplayable display = new Contour2DDisplayable("vcs", true,
                                           isColorFill);
        addAttributedDisplayable(display,
                                 FLAG_CONTOUR | FLAG_COLORTABLE
                                 | FLAG_DISPLAYUNIT);
        return display;
    }

    /**
     * Called after all initialization is finished. This sets the end points
     * of the csSelector to the correct position and adds this as a property
     * change listener to the csSelector.
     */
    public void initDone() {
        super.initDone();
        try {

            List categories = getCategories();
            DataCategory categories1 = DataCategory.createCategory("*-flowvector-*");
            categories.add(categories1);
            setCategories(categories);
        } catch (Exception e) {
            logException("Initializing the csSelector", e);
        }
        // when user moves position of the Selector line, call crossSectionChanged

    }
    /**
     * Set the data for this control
     *
     * @param dataChoice   choice for data selection
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !super.setData(dataChoice)) {
            return false;
        }
        getContourInfo().setIsFilled(isColorFill);
        return true;
    }


    /**
     * Get the default contour info
     *
     * @return the default contour info
     */
    protected ContourInfo getDefaultContourInfo() {
        if (isColorFill) {
            ContourInfo contourInfo = new ContourInfo(Double.NaN, Double.NaN,
                                          Double.NaN, Double.NaN);
            contourInfo.setIsFilled(true);
            contourInfo.setIsLabeled(false);
            return contourInfo;
        }
        return null;
    }


    /**
     * Set the color fill flag.  Used for persistence mechanism
     *
     * @param v  true to color fill
     */
    public void setColorFill(boolean v) {
        isColorFill = v;
    }

    /**
     * Get the color fill flag.  Used for persistence mechanism
     *
     * @return  true if color filled
     */
    public boolean getColorFill() {
        return isColorFill;
    }

    /**
     * Is this a raster display?
     *
     * @return  true if raster
     */
    public boolean getIsRaster() {
        return isColorFill;
    }


    public void removeControl(int idx)
            throws RemoteException, VisADException {
        controlList.remove(idx-1);
    }


    static public class MyContourCrossSectionControl extends ContourCrossSectionControl {

        ContourCrossSectionControl contourCrossSectionControl;
        Unit displayunit = null;
        private Range colorRange = null;

        ContourInfo contourInfo;

        private Color color;


        public MyContourCrossSectionControl() {
            this.isColorFill = false;
            setAttributeFlags(FLAG_CONTOUR | FLAG_COLORTABLE
                    | FLAG_DISPLAYUNIT | FLAG_SMOOTHING);
        }
        public MyContourCrossSectionControl(ContourCrossSectionControl contourCrossSectionControl) {
            this.contourCrossSectionControl = contourCrossSectionControl;
            this.isColorFill = false;
            setAttributeFlags(FLAG_CONTOUR | FLAG_COLORTABLE
                    | FLAG_DISPLAYUNIT | FLAG_SMOOTHING);
        }

        /**
         * Construct the display, frame, and controls
         *
         * @param dataChoice the data to use
         *
         * @return  true if successful
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        public boolean init(DataChoice dataChoice, CrossSectionSelector crossSectionSelector)
                throws VisADException, RemoteException {

            crossSectionView = new CrossSectionViewManager(getViewContext(),
                    new ViewDescriptor("CrossSectionView" + paramName),
                    "showControlLegend=false;showScales=true", animationInfo);

            csSelector = crossSectionSelector;
            xsDisplay  = new Contour2DDisplayable("vcs_"
                    + paramName, true, false);
            addAttributedDisplayable(xsDisplay,
                    FLAG_CONTOUR | FLAG_COLORTABLE
                            | FLAG_DISPLAYUNIT | FLAG_SMOOTHING);

            vcsDisplay = new Contour2DDisplayable("vcs_"
                    + paramName, true, false);
            addAttributedDisplayable(vcsDisplay,
                    FLAG_CONTOUR | FLAG_COLORTABLE
                            | FLAG_DISPLAYUNIT | FLAG_SMOOTHING);


            if ( !setData(dataChoice)) {
                return false;
            }


            vcsDisplay.setVisible(true);
            xsDisplay.setVisible(true);
            XSDisplay csvxsDisplay = crossSectionView.getXSDisplay();

            //getIdv().getVMManager().addViewManager(crossSectionView);
            addViewManager(crossSectionView);
            setYAxisRange(csvxsDisplay, verticalAxisRange);
            csvxsDisplay.setXDisplayUnit(getDefaultDistanceUnit());
            csvxsDisplay.setYDisplayUnit(csvxsDisplay.getYDisplayUnit());
            //crossSectionView.getMaster ().addDisplayable (vcsDisplay);
            if (haveMultipleFields()) {
                addDisplayable(vcsDisplay, crossSectionView,
                        FLAG_COLORTABLE | FLAG_COLORUNIT);
            } else {
                addDisplayable(vcsDisplay, crossSectionView,
                        FLAG_COLORTABLE | FLAG_CONTOUR | FLAG_DISPLAYUNIT | FLAG_SMOOTHING);
            }


            if (displayIs3D) {
                if (haveMultipleFields()) {
                    //If we have multiple fields then we want both the
                    //color unit and the display unit
                    addDisplayable(xsDisplay, FLAG_COLORTABLE | FLAG_COLORUNIT);
                } else {
                    addDisplayable(xsDisplay);
                }
            }

            loadDataFromLine();

            return true;
        }

        /**
         * Has this control been initialized
         *
         * @return Is this control initialized
         */
        public boolean getHaveInitialized() {
            return true;
        }


        /**
         * Make the UI contents for this control window.
         *
         * @return  UI container
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        public Container doMakeContents()
         {

            // TODO:  This is what should be done - however legends don't show up.

            return doMakeWidgetComponent();

            //return GuiUtils.centerBottom(profileDisplay.getComponent(),
            //                             doMakeWidgetComponent());
        }
        /**
         * make widgets for check box for latest data time on left of x axis.
         *
         * @param controlWidgets to fill
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        public void getControlWidgets(List controlWidgets)
                throws VisADException, RemoteException {
            controlWidgets.add(contourWidget = new ContourWidget(this,
                    getContourInfo()));
            addRemovable(contourWidget);

            controlWidgets.add(getColorTableWidget(getRangeForColorTable()));

            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Smoothing:"), doMakeSmoothingWidget()));

        }

        /**
         * Return the label that is to be used for the color widget
         * This allows derived classes to override this and provide their
         * own name,
         *
         * @return Label used for the color widget
         */
        public String getColorWidgetLabel() {
            return "Color";
        }
        /**
         * User has asked to see a different new parameter in this existing display.
         * Do everything needed to load display with new kind of parameter.
         *
         * @param dataChoice    choice for data
         * @return  true if successfule
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        protected boolean setData(DataChoice dataChoice)
                throws VisADException, RemoteException {
            super.setData(dataChoice);
            paramName = dataChoice.getName();
            GridDataInstance di = (GridDataInstance)doMakeDataInstance(dataChoice);
            contourInfo = getContourInfo();
            colorRange = di.getRange(0);
            displayunit = ((GridDataInstance) di).getRawUnit(0);


            return true;
        }

        /**
         * Get the range for the color table.
         *
         * @return range being used
         * @throws RemoteException  some RMI exception occured
         * @throws VisADException  error getting the range in VisAD
         */
        public Range getRangeForColorTable()
                throws RemoteException, VisADException {
            return colorRange;
        }
        /**
         * Get the unit for the data display.
         * @return  unit to use for displaying the data
         */
        public Unit getDisplayUnit() {
            Unit unit = displayunit;

            setDisplayUnit(unit);

            return unit;
        }


        /**
         *  Use the value of the smoothing type and weight to subset the data.
         *
         * @throws RemoteException Java RMI problem
         * @throws VisADException  VisAD problem
         */
        protected void applySmoothing() throws VisADException, RemoteException {
            if (checkFlag(FLAG_SMOOTHING)) {
                GridDataInstance gdi = getGridDataInstance();
                if(gdi != null)
                    super.applySmoothing();
            }
        }


    }
}

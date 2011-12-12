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


import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.display.Contour2DDisplayable;
import ucar.visad.display.DisplayableData;

import visad.FieldImpl;
import visad.Unit;
import visad.VisADException;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * A control for displaying gridded data as 2D contours.
 *
 * @author IDV Development Team
 */

public class ContourPlanViewControl extends PlanViewControl {


    /** the displayable for the data depiction */
    private Contour2DDisplayable contourDisplay;

    /** flag for color filling */
    private boolean isColorFill = false;

    /** flag for coloring by ensembles */
    private boolean colorByMember = true;

    /**
     * Create a new <code>ContourPlanViewControl</code> setting the
     * attribute flags as appropriate.
     */
    public ContourPlanViewControl() {
        setAttributeFlags(FLAG_CONTOUR | FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                          | FLAG_SMOOTHING);
    }


    /**
     * Method to create the particular <code>DisplayableData</code> that
     * this this instance uses for data depictions.
     * @return Contour2DDisplayable for this instance.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        contourDisplay = new Contour2DDisplayable("plan_" + paramName, true,
                isColorFill);
        addAttributedDisplayable(contourDisplay);
        return contourDisplay;
    }

    /**
     * Get whether this display should use color filled contours.  Used
     * mainly by persistence.
     * @return  true if color fill should be used.
     */
    public boolean getColorFill() {
        return isColorFill;
    }

    /**
     * Return the contour display used by this object.  A wrapper
     * around {@link #getPlanDisplay()}.
     * @return this instance's Contour2Ddisplayable.
     * @see #createPlanDisplay()
     */
    Contour2DDisplayable getContourDisplay() {
        return (Contour2DDisplayable) getPlanDisplay();
    }

    /**
     * Get the contour info.
     *
     * @return the contour info
     */
    public ContourInfo getContourInfo() {
        ContourInfo contourInfo = super.getContourInfo();
        if (contourInfo != null) {
            contourInfo.setIsFilled(isColorFill);
        }
        return contourInfo;
    }

    /**
     * Get the default contour info
     *
     * @return the default contour info
     */
    protected ContourInfo getDefaultContourInfo() {
        //        if(true) return null;
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
     * Is this a raster display?
     *
     * @return  true if raster
     */
    public boolean getIsRaster() {
        return isColorFill;
    }

    /**
     * A hook for derived classes to set any state. ex: color filled contours turn off
     * labels
     *
     * @param contourInfo The contour info to initialize
     */
    protected void initializeDefaultContourInfo(ContourInfo contourInfo) {
        if (isColorFill || haveEnsemble) {
            contourInfo.setIsLabeled(false);
        }
    }

    /**
     * Do what is needed when projection changes
     */
    public void projectionChanged() {
        super.projectionChanged();
    }

    /**
     * Set whether this display should use color filled contours.  Used
     * mainly by persistence.
     * @param v  true if color fill should be used.
     */
    public void setColorFill(boolean v) {
        isColorFill = v;
    }

    /**
     * Set the data displayed by this control
     * @param data  DataChoice representing the data
     * @return true if successful
     * @throws VisADException  error creating data
     * @throws RemoteException  error creating remote data
     */
    protected boolean setData(DataChoice data)
            throws VisADException, RemoteException {

        if ( !super.setData(data)) {
            return false;
        }
        return true;
    }

    /**
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        return super.haveMultipleFields() || (haveEnsemble && colorByMember);
    }

    /**
     * Add in any special control widgets to the current list of widgets.
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);

        if (haveEnsemble) {
            JCheckBox toggle = new JCheckBox("Color by Member",
                                             colorByMember);
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        colorByMember =
                            ((JCheckBox) e.getSource()).isSelected();
                        getContourDisplay().setColoredByAnother(
                            haveMultipleFields());
                        getContourDisplay().loadData(
                            getSliceForDisplay(getCurrentSlice()));
                    } catch (Exception ve) {
                        logException("colorByMember", ve);
                    }
                }
            });
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Ensembles:"),
                    GuiUtils.leftCenter(toggle, GuiUtils.filler())));
        }

    }

    /**
     *  Use the value of the smoothing type and weight to subset the data.
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    protected void applySmoothing() throws VisADException, RemoteException {
        // NB: For some reason, when the smoothing changes and the original state was color
        // by param in a bundle, the displayUnit was not being set when smoothing changed.
        if (checkFlag(FLAG_SMOOTHING)) {
            super.applySmoothing();
            applyDisplayUnit();
        }
    }


    /**
     * Get the parameter name for color.
     * @return  color parameter name
     */
    protected String getColorParamName() {
        if (haveEnsemble) {
            return GridUtil.ENSEMBLE_TYPE.getName();
        } else if (haveMultipleFields() && (getGridDataInstance() != null)) {
            return getGridDataInstance().getDataChoice().getIndexedName(
                getColorRangeIndex());
        }
        return paramName;
    }

    /**
     * Set the color by ensemble member property
     *
     * @param yesorno  true to color by ensemble member
     */
    public void setColorByMember(boolean yesorno) {
        colorByMember = yesorno;
    }

    /**
     * Get the color by ensemble member property
     *
     * @return the color by ensemble member property
     */
    public boolean getColorByMember() {
        return colorByMember;
    }

    /**
     * If ensemble, return null, otherwise return the default
     * @return   the unit for the color parameter
     */
    protected Unit getColorUnit() {
        if (colorByMember) {
            return null;
        }
        return super.getColorUnit();
    }
}

/*
 * $Id: ContourPlanViewControl.java,v 1.48 2006/12/01 20:16:32 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.GuiUtils;

import ucar.visad.display.Contour2DDisplayable;
import ucar.visad.display.DisplayableData;

import visad.*;

import java.awt.Component;
import java.awt.Container;


import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;



/**
 * A control for displaying gridded data as 2D contours.
 *
 * @author IDV Development Team
 * @version $Revision: 1.48 $
 */

public class ContourPlanViewControl extends PlanViewControl {

    /** flag for color filling */
    boolean isColorFill = false;

    /** the displayable for the data depiction */
    private Contour2DDisplayable contourDisplay;

    /**
     * Create a new <code>ContourPlanViewControl</code> setting the
     * attribute flags as appropriate.
     */
    public ContourPlanViewControl() {
        setAttributeFlags(FLAG_CONTOUR | FLAG_COLORTABLE | FLAG_DISPLAYUNIT);
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
        applyVerticalValue();
        return true;
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
     * A hook for derived classes to set any state. ex: color filled contours turn off
     * labels
     *
     *
     * @param contourInfo The contour info to initialize
     */
    protected void initializeDefaultContourInfo(ContourInfo contourInfo) {
        if (isColorFill) {
            contourInfo.setIsLabeled(false);
        }
    }




    // Comment out for now.
    /*
     * Add in any special control widgets to the current list of widgets.
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        JCheckBox toggle = new JCheckBox("", getColorFill());
        toggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    isColorFill = ((JCheckBox) e.getSource()).isSelected();
                    getContourDisplay().setColorFill(isColorFill);

                } catch (Exception ve) {
                    logException("setSmoothed", ve);
                }
            }
        });
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Color Fill:"),
                    GuiUtils.leftCenter(toggle, GuiUtils.filler())));
    }
     */

    /**
     * Apply the vertical offset
     */
    private void applyVerticalValue() {
        /*  NB:  the toFront method should obviate this.
            If still needed, uncomment

        if ( !haveLevels()) {
            try {
                double z = getInitialZPosition();
                contourDisplay.addConstantMap(
                    new ConstantMap(getVerticalValue((isColorFill == true)
                                                     ? z
                                                     : z + .01), getDisplayAltitudeType()));
            } catch (Exception exc) {
                logException("applyVerticalValue", exc);
            }
        }
        */
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
     * Get whether this display should use color filled contours.  Used
     * mainly by persistence.
     * @return  true if color fill should be used.
     */
    public boolean getColorFill() {
        return isColorFill;
    }

    /**
     * Do what is needed when projection changes
     */
    public void projectionChanged() {
        super.projectionChanged();
        applyVerticalValue();
    }

    /**
     * Is this a raster display?
     *
     * @return  true if raster
     */
    public boolean getIsRaster() {
        return isColorFill;
    }

}


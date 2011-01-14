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


import ucar.unidata.util.GuiUtils;

import ucar.visad.display.DisplayableData;

import ucar.visad.display.Grid2DDisplayable;

import visad.*;

import java.awt.event.*;


import java.rmi.RemoteException;

import java.util.List;

import javax.swing.JCheckBox;


/**
 * Class for displaying cross sections as color shaded displays.
 * @author Jeff McWhirter
 * @version $Revision: 1.24 $
 */
public class ColorCrossSectionControl extends CrossSectionControl {

    /** flag for smoothing */
    boolean isSmoothed = false;

    /** flag for allowing smoothing */
    boolean allowSmoothing = true;

    /**
     * Default constructor
     */
    public ColorCrossSectionControl() {}

    /**
     * Create the <code>DisplayableData</code> that will be used
     * to depict the data in the main display.
     * @return  depictor for data in main display
     * @throws VisADException  unable to create depictor
     * @throws RemoteException  unable to create depictor (shouldn't happen)
     */
    protected DisplayableData createXSDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable display = new Grid2DDisplayable("vcs_col"
                                        + paramName, true);
        display.setTextureEnable( !isSmoothed);
        display.setUseRGBTypeForSelect(true);
        addAttributedDisplayable(display, FLAG_COLORTABLE | FLAG_SELECTRANGE);
        return display;
    }

    /**
     * Create the <code>DisplayableData</code> that will be used
     * to depict the data in the control's display.
     * @return  depictor for data in main display
     * @throws VisADException  unable to create depictor
     * @throws RemoteException  unable to create depictor (shouldn't happen)
     */
    protected DisplayableData createVCSDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable display = new Grid2DDisplayable("vcs_" + paramName,
                                        true);
        display.setTextureEnable( !isSmoothed);
        display.setUseRGBTypeForSelect(true);
        addAttributedDisplayable(display, FLAG_COLORTABLE | FLAG_SELECTRANGE);
        return display;
    }

    /**
     * Add any specialized control widgets for this control
     * to the list.
     * @param  controlWidgets  <code>List</code> to add to.
     * @throws VisADException  unable to create controls
     * @throws RemoteException  unable to create controls (shouldn't happen)
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        if (getAllowSmoothing()) {
            JCheckBox toggle = new JCheckBox("", isSmoothed);
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        isSmoothed = ((JCheckBox) e.getSource()).isSelected();
                        ((Grid2DDisplayable) getXSDisplay()).setTextureEnable(
                             !isSmoothed);
                        ((Grid2DDisplayable) getVerticalCSDisplay())
                            .setTextureEnable( !isSmoothed);
                    } catch (Exception ve) {
                        logException("setSmoothed", ve);
                    }
                }
            });
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Shade Colors:"), toggle));
        }
    }

    /**
     * Set whether the depictions should show smooth or blocky shading.
     * Used by XML persistence.
     * @param v  true to use smoothed depictions
     */
    public void setSmoothed(boolean v) {
        isSmoothed = v;
    }

    /**
     * Get whether the depictions show smooth or blocky shading.
     * @return true if using smoothed depictions
     */
    public boolean getSmoothed() {
        return isSmoothed;
    }

    /**
     * Set whether this display should allow smoothed colors or blocky. Used
     * by XML persistence (bundles) for the most part.
     * @param v  true to allowing smoothing.
     */
    public void setAllowSmoothing(boolean v) {
        allowSmoothing = v;
        if ( !allowSmoothing) {
            setSmoothed(false);
        }
    }

    /**
     * Get whether this display should allow smoothing
     * @return true if allows smoothing.
     */
    public boolean getAllowSmoothing() {
        return allowSmoothing;
    }

    /**
     * Is this a raster display?
     *
     * @return  true if raster
     */
    public boolean getIsRaster() {
        return true;
    }
}

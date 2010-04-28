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

import visad.VisADException;


import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;



/**
 * Class for controlling the display of color shaded plan views of
 * gridded data.
 * @author Jeff McWhirter
 * @version $Revision: 1.41 $
 */
public class ColorPlanViewControl extends PlanViewControl {

    /** flag for smoothing */
    boolean isSmoothed = false;

    /** flag for allowing smoothing */
    boolean allowSmoothing = true;

    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public ColorPlanViewControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                          | FLAG_SELECTRANGE | FLAG_TEXTUREQUALITY
                          | FLAG_SMOOTHING);
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
            new Grid2DDisplayable("ColorPlanViewControl_"
                                  + ((datachoice != null)
                                     ? datachoice.toString()
                                     : ""), true);
        gridDisplay.setPointSize(2f);
        gridDisplay.setPolygonMode(polygonMode);
        gridDisplay.setTextureEnable( !isSmoothed);
        addAttributedDisplayable(gridDisplay);
        gridDisplay.setUseRGBTypeForSelect(true);
        gridDisplay.setCurvedSize(getTextureQuality());
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
     * Add in any special control widgets to the current list of widgets.
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        JComboBox polyModeCombo = null;
        if (visad.util.Util.canDoJava3D()) {
            polyModeCombo = getPolyModeComboBox();
        }


        if (getAllowSmoothing()) {
            JCheckBox toggle = new JCheckBox("", isSmoothed);
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        isSmoothed = ((JCheckBox) e.getSource()).isSelected();
                        // textured == not smoothed
                        getGridDisplay().setTextureEnable( !isSmoothed);

                    } catch (Exception ve) {
                        logException("setSmoothed", ve);
                    }
                }
            });
            if (polyModeCombo != null) {
                controlWidgets.add(new WrapperWidget(this,
                        GuiUtils.rLabel("Display:"),
                        GuiUtils.left(GuiUtils.hbox(new Component[] {
                    GuiUtils.rLabel("Shade Colors:"), toggle,
                    new JLabel("  Mode: "), polyModeCombo,
                    new JLabel("  Point Size: "), doMakePointSizeWidget()
                }))));
            } else {
                controlWidgets.add(new WrapperWidget(this,
                        GuiUtils.rLabel("Shade Colors:"),
                        GuiUtils.leftCenter(toggle, GuiUtils.filler())));
            }
        } else if (polyModeCombo != null) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Display Mode:"),
                    GuiUtils.left(polyModeCombo)));

        }

    }


    /**
     * Add DisplaySettings appropriate for this display
     *
     * @param dsd  the dialog to add to
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        dsd.addPropertyValue(new Boolean(isSmoothed), "smoothed",
                             "Colors Shaded", SETTINGS_GROUP_DISPLAY);
    }


    /**
     * Set whether this display should be smoothed colors or blocky. Used
     * by XML persistence (bundles) for the most part.
     * @param v  true if smoothed.
     */
    public void setSmoothed(boolean v) {
        isSmoothed = v;
    }

    /**
     * Get whether this display should be smoothed colors or
     * blocky.
     * @return true if smoothed.
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

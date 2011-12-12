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
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.ViewManager;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.display.DisplayableData;

import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.quantities.Altitude;

import ucar.visad.quantities.GeopotentialAltitude;


import visad.*;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;


/**
 * A Display Control with Displayable and controls for
 * one 3D surface display of topography.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.23 $
 */
public class TopographyControl extends PlanViewControl {

    /** The displayable for the topography */
    Grid2DDisplayable gridDisplay;

    /** flag for smoothing */
    boolean isSmoothed;

    /**
     * Construct a new topography control.  Set the attribute flags.
     */
    public TopographyControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL);
    }

    /**
     * Method for creating the <code>DisplayableData</code> object
     * that is the main depiction for the data controlled by this
     * <code>PlanViewControl</code>; implemented by each subclass.
     *
     * @return <code>DisplayableData</code> for the data depiction.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        gridDisplay = new Grid2DDisplayable("topo_" + paramName, true);
        gridDisplay.setTextureEnable( !isSmoothed);
        gridDisplay.setPolygonMode(getPolygonMode());
        gridDisplay.setUseDefaultRenderer(true);
        addAttributedDisplayable(gridDisplay);
        return gridDisplay;
    }

    /**
     * Wrapper around {@link #addTopographyMap(int)} to allow subclasses
     * to set their own index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void addTopographyMap() throws VisADException, RemoteException {
        addTopographyMap(0);
    }

    /**
     * Get the multiple is topography property.  Even though there is
     * only one field, this is the only way to make sure we get the topography
     * ScalarMap added.
     *
     * @return true
     */
    public boolean getMultipleIsTopography() {
        return true;
    }

    /**
     * Get any special control widgets for this control.
     *
     * @param controlWidgets  input/ouput list of widgets
     *
     * @throws VisADException  couldn't set the data
     * @throws RemoteException  couldn't set the data
     */
    public void getControlWidgets(List<ControlWidget> controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
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
        Component right = toggle;
        if (visad.util.Util.canDoJava3D()) {
            getGridDisplay().setPointSize(getPointSize());

            Component tmpComp = GuiUtils.hgrid(
                                    Misc.newList(
                                        GuiUtils.rLabel("Display Mode: "),
                                        getPolyModeComboBox()), 0);
            right = GuiUtils.left(GuiUtils.hgrid(Misc.newList(toggle,
                    tmpComp), 0));
        }  // end canDoJava3D
        controlWidgets.add(
            new WrapperWidget(this, GuiUtils.rLabel("Shade Colors:"), right));

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Point Size:"),
                GuiUtils.left(doMakePointSizeWidget())));


    }

    /**
     * Get the main <code>DisplayableData</code> used for depicting this
     * data as a Grid2DDisplayable.
     *
     * @return data depictor
     */
    protected Grid2DDisplayable getGridDisplay() {
        return gridDisplay;
    }

    /**
     * Set whether the display should show smoothed shading or not.
     *
     * @param v true if shading is to be smoothed.  Used by XML persistence.
     */
    public void setSmoothed(boolean v) {
        isSmoothed = v;
    }

    /**
     * Return whether the display should show smoothed shading or not.
     *
     * @return true if shading is smoothed.
     */
    public boolean getSmoothed() {
        return isSmoothed;
    }


    /**
     * Is this a raster display?
     *
     * @return  true if raster
     */
    public boolean getIsRaster() {
        return true;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setPointSize(float value) {
        super.setPointSize(value);
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setPointSize(getPointSize());
            } catch (Exception e) {
                logException("Setting point size", e);
            }
        }
    }

}

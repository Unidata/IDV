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
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import ucar.visad.display.DisplayableData;


import ucar.visad.display.ImageRGBDisplayable;

import visad.*;
import visad.RealType;
import visad.VisADException;

import visad.util.BaseRGBMap;

import visad.util.ColorPreview;


import java.awt.*;



import java.awt.Component;
import java.awt.event.*;


import java.rmi.RemoteException;

import java.util.List;

import javax.swing.*;




/**
 * Class for controlling the display of color images.
 * @author Jeff McWhirter
 * @version $Revision: 1.28 $
 */
public class ImageControl extends BaseImageControl {

    /** Displayable for the data */
    ImageRGBDisplayable imageDisplay;

    /** topography type */
    private RealType topoType = null;

    /** topo flag */
    private boolean multipleIsTopography = false;

    /** topo flag */
    private boolean useTexture = true;


    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public ImageControl() {
        //setAttributeFlags(FLAG_COLORTABLE);
    }



    /**
     * Overwrite base class method to get an image color table
     *
     * @return The initial color table to use
     */
    protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable("image");
    }



    /**
     * Called to make this kind of Display Control; also calls code to
     * made the Displayable.
     * This method is called from inside DisplayControlImpl init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        imageDisplay = new ImageRGBDisplayable("plan_color_" + paramName);
        setImageTexture(useTexture);
        if (getAlpha() != 1.0) {
            imageDisplay.setAlpha(getAlpha());
        }
        if (EMPTY_RGB_IMAGE != null) {
            imageDisplay.setData(EMPTY_RGB_IMAGE);
        }
        addDisplayable(imageDisplay, getImageFlags());
        if (dataChoice != null) {
            return setData(dataChoice);
        }
        return true;
    }


    /**
     * Get control widgets specific to this control.
     *
     * @param controlWidgets   list of control widgets from other places
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Transparency:"), doMakeAlphaSlider()));

        if (getMultipleIsTopography()) {
            JCheckBox toggle = new JCheckBox("", useTexture);
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        useTexture = ((JCheckBox) e.getSource()).isSelected();
                        setImageTexture(useTexture);
                    } catch (Exception ve) {
                        logException("useTexture", ve);
                    }
                }
            });
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Display:"),
                    GuiUtils.left(GuiUtils.hbox(new Component[] {
                        GuiUtils.rLabel("Texture: "),
                        toggle }))));
        }

    }


    /**
     * Override the base class method
     * @return the contents
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   Problem making data
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        return GuiUtils.topLeft(doMakeWidgetComponent());
    }  // end doMakeContents



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
            return false;
        }
        if (haveMultipleFields()) {
            addTopographyMap(3);
        }

        FieldImpl fieldImpl = getGridDataInstance().getGrid();
        checkImageSize(fieldImpl);
        imageDisplay.loadData(getWorkingImage(fieldImpl));
        return true;
    }

    /**
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        GridDataInstance gdi = getGridDataInstance();
        if (gdi == null) {
            return false;
        }
        return gdi.getNumRealTypes() > 3;
    }


    /**
     * Apply the skip factor to the image
     * protected void applySkipFactor() {
     *   try {
     *       if (imageDisplay != null && getGridDataInstance() != null) {
     *           FieldImpl fieldImpl = getGridDataInstance().getGrid();
     *           imageDisplay.loadData(getWorkingImage(fieldImpl));
     *       }
     *   } catch (Exception e) {
     *       logException("Setting alpha value", e);
     *   }
     * }
     *
     * @param v ???
     */




    /**
     * Set the other is topography property.
     *
     * @param v true if second parameter is topography
     */
    public void setMultipleIsTopography(boolean v) {
        multipleIsTopography = v;
    }

    /**
     * Get the multiple is topography property.
     *
     * @return true if multiple grid is topography
     */
    public boolean getMultipleIsTopography() {
        return multipleIsTopography;
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
            if (imageDisplay != null) {
                imageDisplay.setAlpha(newAlpha);
            }
        } catch (Exception e) {
            logException("Setting alpha value", e);
        }
    }

    /**
     * Get the flags for the image displayable
     *
     * @return  the flags
     */
    protected int getImageFlags() {
        int imageFlags = FLAG_COLORTABLE;
        if ( !getMultipleIsTopography()) {
            imageFlags |= FLAG_ZPOSITION;
        }
        //return FLAG_COLORTABLE | FLAG_ZPOSITION;
        return imageFlags;
    }

    /**
     * Set whether this display should be textured or smoothed. Used
     * by XML persistence (bundles) for the most part.
     * @param v  true if textured.
     */
    public void setUseTexture(boolean v) {
        useTexture = v;
    }

    /**
     * Get whether this display should be textured or not.
     * @return true if textured.
     */
    public boolean getUseTexture() {
        return useTexture;
    }

    /**
     * Set the texture map
     * @param useTexture true to texture
     */
    private void setImageTexture(boolean useTexture) {
        if (imageDisplay != null) {
            try {
                imageDisplay.addConstantMap(new ConstantMap(useTexture
                        ? 1.0
                        : 0.0, Display.TextureEnable));
            } catch (Exception e) {
                logException("Setting smooth value", e);
            }
        }
    }
}

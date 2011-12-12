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
import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.ContourInfo;

import ucar.unidata.util.Misc;

import ucar.visad.display.Contour2DDisplayable;
import ucar.visad.display.DisplayableData;

import visad.VisADException;

import java.awt.*;


import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * A cross section control for displaying contours.
 *
 * @author IDV Development Team
 * @version $Revision: 1.31 $
 */
public class ContourCrossSectionControl extends CrossSectionControl {

    /** flag for color file */
    boolean isColorFill = false;

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
                                 | FLAG_DISPLAYUNIT | FLAG_COLORTABLE | FLAG_SMOOTHING);
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
}

/*
 * $Id: SoundingViewManager.java,v 1.67 2006/12/27 20:14:06 jeffmc Exp $
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


package ucar.unidata.idv;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.view.sounding.AerologicalDisplay;

import ucar.visad.display.*;

import visad.*;

import java.awt.*;

import java.rmi.RemoteException;

import javax.swing.*;



/**
 * A wrapper around a sounding display (AerologicalDisplay) like a Skew-T
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class SoundingViewManager extends ViewManager {

    /** Prefix for preferences */
    public static final String PREF_PREFIX = ViewManager.PREF_PREFIX
                                             + "SOUNDING";

    /**
     *  A paramterless ctor for XmlEncoder  based decoding.
     */
    public SoundingViewManager() {}

    /**
     *  Create a SoundingViewManager with the given context, descriptor, object store
     *  and properties string.
     *
     *  @param viewContext Provides a context for the VM to be in.
     *  @param desc The ViewDescriptor that identifies this VM
     *  @param properties A set of ";" delimited name-value pairs.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public SoundingViewManager(ViewContext viewContext, ViewDescriptor desc,
                               String properties)
            throws VisADException, RemoteException {
        this(viewContext, desc, properties, null);
    }


    /**
     *  Create a SoundingViewManager with the given context, descriptor, object store,
     *  properties string and animation state
     *
     *  @param viewContext Provides a context for the VM to be in.
     *  @param desc The ViewDescriptor that identifies this VM
     *  @param properties A set of ";" delimited name-value pairs.
     *  @param animationInfo Initial animation properties
     *
     * @throws RemoteException
     * @throws VisADException
     *
     */
    public SoundingViewManager(ViewContext viewContext, ViewDescriptor desc,
                               String properties, AnimationInfo animationInfo)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties, animationInfo);
    }


    /**
     *  Create a SoundingViewManager with the given context, descriptor, object store,
     *  properties string and animation state
     *
     *  @param viewContext Provides a context for the VM to be in.
     *  @param master  display master
     *  @param viewDescriptor The ViewDescriptor that identifies this VM
     *  @param properties A set of ";" delimited name-value pairs.
     *
     * @throws RemoteException
     * @throws VisADException
     *
     */
    public SoundingViewManager(ViewContext viewContext, DisplayMaster master,
                               ViewDescriptor viewDescriptor,
                               String properties)
            throws VisADException, RemoteException {
        this(viewContext, viewDescriptor, properties, null);
        setDisplayMaster(master);
    }

    /**
     * Initialize the view menu
     *
     * @param viewMenu the view menu
     */
    public void initializeViewMenu(JMenu viewMenu) {
        showControlMenu = false;
        super.initializeViewMenu(viewMenu);
        viewMenu.add(makeColorMenu());
        //viewMenu.addSeparator();
        //viewMenu.add(GuiUtils.makeMenuItem("Properties", this,
        //                                   "showPropertiesDialog"));
    }


    /**
     * Factory method for creating the display master
     *
     * @return The Display Master
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected DisplayMaster doMakeDisplayMaster()
            throws VisADException, RemoteException {
        return AerologicalDisplay.getInstance(
            AerologicalDisplay.SKEWT_DISPLAY);
    }

    /**
     * Set the sounding display
     *
     * @param ad  the sounding display
     */
    public void setSoundingDisplay(AerologicalDisplay ad) {
        setDisplayMaster(ad);
    }


    /**
     * Don't show the side legend
     *
     * @return false
     */
    public boolean getShowSideLegend() {
        return false;
    }

    /**
     * What type of view is this
     *
     * @return The type of view
     */
    public String getTypeName() {
        return "Sounding View";
    }

    /**
     * Do we support animation?
     *
     * @return false
     */
    public boolean animationOk() {
        return false;
    }

}


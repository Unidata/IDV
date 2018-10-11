/*
 * Copyright 1997-2018 Unidata Program Center/University Corporation for
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


import ucar.unidata.view.sounding.Hodograph3DDisplay;

import ucar.visad.display.AnimationInfo;
import ucar.visad.display.DisplayMaster;

import visad.VisADException;


import java.rmi.RemoteException;



/**
 * A wrapper around a hodograph display
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class HodographViewManager extends ViewManager {

    /** Prefix for preferences. */
    public static final String PREF_PREFIX = ViewManager.PREF_PREFIX
                                             + "HODOGRAPH";

    /**
     *  A paramterless ctor for XmlEncoder  based decoding.
     */
    public HodographViewManager() {}

    /**
     * Create a HodographViewManager with the given context,
     * descriptor, object store and properties string.
     *
     * @param viewContext  Provides a context for the VM to be in.
     * @param desc         The ViewDescriptor that identifies this VM
     * @param properties   A set of ";" delimited name-value pairs.
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public HodographViewManager(ViewContext viewContext, ViewDescriptor desc,
                                String properties)
            throws VisADException, RemoteException {
        this(viewContext, desc, properties, null);
    }


    /**
     * Create a HodographViewManager with the given context, descriptor,
     * object store, properties string and animation state.
     *
     * @param viewContext Provides a context for the VM to be in.
     * @param desc The ViewDescriptor that identifies this VM
     * @param properties A set of ";" delimited name-value pairs.
     * @param animationInfo Initial animation properties
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public HodographViewManager(ViewContext viewContext, ViewDescriptor desc,
                                String properties,
                                AnimationInfo animationInfo)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties, animationInfo);
    }


    /**
     *  Create a HodographViewManager with the given context, display,
     *  descriptor, properties string.
     *
     * @param viewContext Provides a context for the VM to be in.
     * @param master  display master
     * @param viewDescriptor The ViewDescriptor that identifies this VM
     * @param properties A set of ";" delimited name-value pairs.
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public HodographViewManager(ViewContext viewContext,
                                DisplayMaster master,
                                ViewDescriptor viewDescriptor,
                                String properties)
            throws VisADException, RemoteException {
        this(viewContext, viewDescriptor, properties, null);
        setDisplayMaster(master);
    }

    /**
     * Factory method for creating the display master.
     *
     * @return The Display Master
     * @throws VisADException On badness
     * @throws RemoteException On badness
     */
    protected DisplayMaster doMakeDisplayMaster()
            throws VisADException, RemoteException {
        Hodograph3DDisplay display = new Hodograph3DDisplay();
        return display;
    }

    /**
     * Set the hodograph display.
     *
     * @param hd  the hodograph display
     */
    public void setHodographDisplay(Hodograph3DDisplay hd) {
        setDisplayMaster(hd);
    }


    /**
     * Don't show the side legend.
     *
     * @return false
     */
    public boolean getShowSideLegend() {
        return false;
    }

    /**
     * What type of view is this.
     *
     * @return The type of view
     */
    public String getTypeName() {
        return "Hodograph View";
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

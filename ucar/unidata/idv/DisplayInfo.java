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

package ucar.unidata.idv;



import ucar.visad.display.DisplayMaster;
import ucar.visad.display.Displayable;

import visad.VisADException;

import java.rmi.RemoteException;



/**
 * This class contains the  triple:
 * {@link DisplayControl},
 * {@link ucar.visad.display.Displayable}
 * and {@link ViewManager}.
 * It is held by the ViewManager to know which DisplayControls are displaying
 * in the ViewManager. It is held by the control/DisplayControlImpl to
 * known which Displayable is displayed in which ViewManager.
 *
 * @author IDV development team
 */

public class DisplayInfo {

    /** The display control */
    private DisplayControl displayControl;

    /** The view manager */
    private ViewManager viewManager;

    /** The displayable_ */
    private Displayable displayable;

    /**
     * This keeps track of whether the displayable is, in the
     * end,  visible or not. It used used to keep the ultimate
     * visibility state of the displayable when the display control
     * turns on/off its overall visibility.
     */
    private boolean ultimateVisible = true;


    /** Has the displayable been added to the display master yet */
    private boolean isDisplayableAdded = false;


    /**
     * Create this display info.
     *
     * @param displayControl The display control
     * @param viewManager The view manager
     * @param displayable The displayable
     */
    public DisplayInfo(DisplayControl displayControl,
                       ViewManager viewManager, Displayable displayable) {
        this.displayControl = displayControl;
        this.viewManager    = viewManager;
        this.displayable    = displayable;
    }

    /**
     * Get the ultimate visible property
     *
     * @return Is displayable visible
     */
    public boolean getUltimateVisible() {
        return ultimateVisible;
    }

    /**
     * Set the ultimate visible property
     *
     * @param v  Is displayable visible
     */
    public void setUltimateVisible(boolean v) {
        ultimateVisible = v;
    }

    /**
     * Return the {@link DisplayControl}
     *
     * @return The display control
     */
    public DisplayControl getDisplayControl() {
        return displayControl;
    }

    /**
     * Return the {@link ViewManager}
     *
     * @return The view manager
     */
    public ViewManager getViewManager() {
        return viewManager;
    }

    /**
     * Set the view manager
     *
     * @param viewManager The view manager
     */
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    /**
     * Get the {@link ucar.visad.display.DisplayMaster}. This just
     * gets the display master from the view manager.
     *
     * @return The display master
     */
    public DisplayMaster getDisplayMaster() {
        return viewManager.getMaster();
    }

    /**
     * Get the {@link ucar.visad.display.Displayable}.
     *
     * @return The displayable
     */
    public Displayable getDisplayable() {
        return displayable;
    }

    /**
     *  Add this displayInfo to the ViewManager.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void addDisplayable() throws RemoteException, VisADException {
        if ((viewManager != null) && !isDisplayableAdded) {
            isDisplayableAdded = viewManager.addDisplayInfo(this);
        }
    }

    /**
     * Has the displayable that this DisplayInfo holds been added to the viewmanager
     *
     * @return Has the displayable been added.
     */
    public boolean getDisplayableAdded() {
        return isDisplayableAdded;
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setDisplayableAdded(boolean v) {
        isDisplayableAdded = v;
    }

    /**
     * Move the displayable to the newViewManager
     *
     * @param newViewManager The new view manager
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void moveTo(ViewManager newViewManager)
            throws RemoteException, VisADException {
        removeDisplayable();
        setViewManager(newViewManager);
        addDisplayable();
    }

    /**
     * Tells the ViewManager  to set the display master active
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void activateDisplay() throws RemoteException, VisADException {
        if (viewManager != null) {
            viewManager.setMasterActive();
        }
    }


    /**
     * Tells the ViewManager  to set the display master inactive
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void deactivateDisplay() throws RemoteException, VisADException {
        if (viewManager != null) {
            viewManager.setMasterInactive();
        }
    }


    /**
     * Remove the displayable from the view manager
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void removeDisplayable() throws RemoteException, VisADException {
        if ((viewManager != null) && isDisplayableAdded) {
            viewManager.removeDisplayInfo(this);
            isDisplayableAdded = false;
        }
    }

    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        return super.toString() + "---" + displayControl;

    }

}

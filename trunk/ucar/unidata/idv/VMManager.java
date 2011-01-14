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



import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.data.gis.Transect;

import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.idv.control.TransectDrawingControl;


import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.xml.XmlResourceCollection;

import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.MapProjection;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.lang.reflect.Constructor;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;




/**
 * This class manages the set of {@link ViewManager}s.  It handles
 * initializing them after unpersistence, managing the set of view manager
 * states (this is where you can save off the state of a view manager
 * and then reapply it. So you can save projection, map, etc., name it
 * and use it later.)
 *
 *
 * @author IDV development team
 */


public class VMManager extends IdvManager {


    /** The list of all active view managers */
    private List<ViewManager> viewManagers = new ArrayList<ViewManager>();

    /**
     *  List of TwoFacedObjects (name, viewmanager) for the named saved viewmanager states.
     */
    private List viewpoints;


    /** The viewmanager whose window was last active */
    private ViewManager lastActiveViewManager = null;


    /**
     * Construct  this manager with the given idv.
     *
     * @param idv The IDV
     */
    public VMManager(IntegratedDataViewer idv) {
        super(idv);
        //Switch the vmstate.xml to viewpoints.xml
        try {
            File oldFile =
                new File(IOUtil.joinDir(getStore().getUserDirectory(),
                                        "vmstate.xml"));
            File newFile =
                new File(IOUtil.joinDir(getStore().getUserDirectory(),
                                        "viewpoints.xml"));
            if (oldFile.exists() && !newFile.exists()) {
                IOUtil.moveFile(oldFile, newFile);
            }
        } catch (Exception exc) {
            logException("moving vmstate.xml to viewpoints.xml", exc);
        }
    }





    /**
     * Get  the list of active {@link ViewManager}s
     *
     * @return List of view managers.
     */
    public List<ViewManager> getViewManagers() {
        return new ArrayList<ViewManager>(viewManagers);
    }

    /**
     * Get all of the view managers of the given class
     *
     * @param c ViewManager class
     *
     * @return List of ViewManagers
     */
    public List getViewManagers(Class c) {
        List result = new ArrayList();
        List vms    = getViewManagers();
        for (int i = 0; i < vms.size(); i++) {
            ViewManager vm = (ViewManager) vms.get(i);
            if (c.isAssignableFrom(vm.getClass())) {
                result.add(vm);
            }
        }
        return result;
    }


    /**
     * Capture an image for all ViewManagers
     */
    public void captureAll() {
        List vms = getViewManagers();

        String filename = FileManager.getWriteFile(FileManager.FILTER_IMAGE,
                              FileManager.SUFFIX_JPG);
        if (filename == null) {
            return;
        }
        String       root = IOUtil.stripExtension(filename);
        String       ext  = IOUtil.getFileExtension(filename);

        StringBuffer sb   = new StringBuffer("<html>");
        sb.append(
            "Since there were multiple images they were written out as:<ul>");
        for (int i = 0; i < vms.size(); i++) {
            ViewManager vm   = (ViewManager) vms.get(i);
            String      name = vm.getName();
            if ((name == null) || (name.trim().length() == 0)) {
                name = "" + (i + 1);
            }
            if (vms.size() != 1) {
                filename = root + name + ext;
            }

            sb.append("<li> " + filename);
            vm.writeImage(filename);
        }
        sb.append("</ul></html>");
        if (vms.size() > 1) {
            GuiUtils.showDialog("Captured Images",
                                GuiUtils.inset(new JLabel(sb.toString()), 5));
        }
    }



    /**
     * Apply the user preferences to all ViewManagers
     */
    public void applyPreferences() {
        for (int i = 0; i < viewManagers.size(); i++) {
            viewManagers.get(i).applyPreferences();
        }
    }


    /**
     * Set the {@link ViewManager} whose window  was last active.
     *
     * @param viewManager The last active view manager
     */
    public void setLastActiveViewManager(ViewManager viewManager) {
        if (lastActiveViewManager != null) {
            lastActiveViewManager.setLastActive(false);
        }
        lastActiveViewManager = viewManager;
        if (lastActiveViewManager != null) {
            lastActiveViewManager.setLastActive(true);
        }
    }



    /**
     * Is there currently more than one possibly active viewmanager
     *
     * @return More than one active view managers
     */
    public boolean haveMoreThanOneMainViewManager() {
        int cnt = 0;
        for (int i = 0; i < viewManagers.size(); i++) {
            if ((viewManagers.get(i) instanceof MapViewManager)
                    || (viewManagers.get(i) instanceof TransectViewManager)) {
                cnt++;
            }
            if (cnt > 1) {
                return true;
            }
        }

        /*
        List windows = IdvWindow.getMainWindows();
        if (windows.size() > 1) {
            return true;
        }
        for (int i = 0; i < windows.size(); i++) {
            IdvWindow window       = (IdvWindow) windows.get(i);
            List      viewManagers = window.getViewManagers();
            if (viewManagers.size() > 1) {
                return true;
            }
        }
        */
        return false;
    }



    /**
     * Get the {@link ViewManager} whose window  was last active.
     *
     * @return The last active view manager
     */
    public ViewManager getLastActiveViewManager() {
        if (lastActiveViewManager == null) {
            List windows = IdvWindow.getMainWindows();
            for (int i = 0; i < windows.size(); i++) {
                IdvWindow window = (IdvWindow) windows.get(i);
                getIdvUIManager().handleWindowActivated(window);
                if (lastActiveViewManager != null) {
                    break;
                }
            }
        }
        return lastActiveViewManager;
    }


    /**
     * Find the view manager identified by the given view descriptor
     *
     * @param viewDescriptor The id of the VM
     *
     * @return The VM or null if none found
     */
    public ViewManager findViewManager(ViewDescriptor viewDescriptor) {
        ViewManager viewManager = null;
        if (viewDescriptor == null) {
            viewDescriptor = new ViewDescriptor(ViewDescriptor.LASTACTIVE);
        }

        if (viewDescriptor.nameEquals(ViewDescriptor.LASTACTIVE)) {
            viewManager = getLastActiveViewManager();
            if (viewManager != null) {
                if (viewManager.isClassOk(viewDescriptor)) {
                    return viewManager;
                }
            }
            List local = new ArrayList(viewManagers);
            for (int i = 0; i < local.size(); i++) {
                ViewManager vm = (ViewManager) local.get(i);
                if (vm.isClassOk(viewDescriptor)) {
                    return vm;
                }
            }
        }
        return findViewManagerInList(viewDescriptor);
    }


    /**
     * Find the view manager that is defined by the given view descriptor.
     *
     * @param viewDescriptor The view descriptor
     *
     * @return The view manager or null if none found
     */
    private ViewManager findViewManagerInList(ViewDescriptor viewDescriptor) {
        return findViewManagerInList(viewDescriptor,
                                     new ArrayList(viewManagers));
    }

    /**
     * Find the view manager in the given list that is defined by the given view descriptor.
     *
     * @param viewDescriptor The view descriptor
     * @param vms List of ViewManagers
     *
     * @return The view manager or null if none found
     */
    public static ViewManager findViewManagerInList(
            ViewDescriptor viewDescriptor, List vms) {
        for (int i = 0; i < vms.size(); i++) {
            ViewManager vm = (ViewManager) vms.get(i);
            if (vm.isDefinedBy(viewDescriptor)) {
                return vm;
            } else {}
        }
        return null;
    }

    /**
     * Be notified that a transect view manager has changed. This may be from a zoom, etc.
     * Notify any TransectDrawingControls.
     */
    public void transectViewsChanged() {
        List controls = findTransectDrawingControls();
        for (int i = 0; i < controls.size(); i++) {
            TransectDrawingControl tdc =
                (TransectDrawingControl) controls.get(i);
            tdc.transectViewsChanged();
        }

    }


    /**
     * Find a TDC
     *
     * @return  The TDC
     */
    public List findTransectDrawingControls() {
        List controls        = new ArrayList();
        List displayControls = getIdv().getDisplayControls();
        for (int i = 0; i < displayControls.size(); i++) {
            DisplayControl c = (DisplayControl) displayControls.get(i);
            if (c instanceof TransectDrawingControl) {
                controls.add(c);
            }
        }
        return controls;
    }




    /**
     * _more_
     */
    public void updateAllLegends() {
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager vm = (ViewManager) viewManagers.get(i);
            vm.fillLegends();
        }
    }


    /**
     *  Write the viewpoints list
     */
    public void writeVMState() {
        getVMState();
        List localViewpoints = new ArrayList();
        List tmp             = viewpoints;
        for (Object o : tmp) {
            if ((o instanceof ViewState) && !((ViewState) o).getIsLocal()) {
                continue;
            }
            localViewpoints.add(o);
        }
        try {
            XmlResourceCollection rc =
                getResourceManager().getXmlResources(
                    getResourceManager().RSC_VIEWPOINTS);
            for (int i = 0; i < rc.size(); i++) {
                if (rc.isWritableResource(i)) {
                    File f = new File(rc.get(i).toString());
                    String contents = getIdv().encodeObject(localViewpoints,
                                          true);
                    IOUtil.writeFile(f, contents);
                    return;
                }
            }
        } catch (Exception exc) {
            logException("writing viewpoints", exc);
        }
    }


    /**
     * Instantiates (if needed) and returns the list of
     * {@link TwoFacedObject}s that is the set of saved viewpoints
     *
     * @return List that holds the viewpoints
     */
    public List getVMState() {
        if (viewpoints == null) {
            List tmp = new ArrayList();
            XmlResourceCollection rc =
                getResourceManager().getXmlResources(
                    getResourceManager().RSC_VIEWPOINTS);

            for (int i = 0; i < rc.size(); i++) {
                String contents = rc.read(i);
                if (contents == null) {
                    continue;
                }
                try {
                    List resources =
                        (List) getIdv().getEncoderForRead().toObject(
                            contents);
                    tmp.addAll(resources);
                    boolean local = rc.isWritable(i);
                    for (Object o : resources) {
                        if (o instanceof ViewState) {
                            ((ViewState) o).setIsLocal(local);
                        }
                    }
                } catch (Exception exc) {
                    logException("Creating VM list", exc);
                }
            }
            viewpoints = tmp;
        }
        return viewpoints;
    }



    /**
     *  Popup a dialog asking the user for the name of the saved ViewManager.
     *  If provided, add a new TwoFacedObject to the list of saved ViewManagers
     *  and write the list to disk.
     *
     * @param vm The view manager to save
     */
    protected void saveViewManagerState(ViewManager vm) {
        try {
            String name = ((vm instanceof MapViewManager)
                           ? "Map View"
                           : "View");
            name = GuiUtils.getInput(null, "Name for saved view: ", name);
            if (name == null) {
                return;
            }
            ViewState viewState = vm.doMakeViewState();
            viewState.setName(name);
            getVMState().add(viewState);
            writeVMState();
        } catch (Exception exc) {
            logException("Saving view state", exc);
        }
    }







    /**
     * Add the new view manager into the list if we don't have
     * one with the {@link ViewDescriptor} of the new view manager
     * already.
     *
     * @param newViewManager  The new view manager
     */
    public void addViewManager(ViewManager newViewManager) {


        ViewManager vm =
            findViewManagerInList(newViewManager.getViewDescriptor());

        if (vm == null) {
            synchronized (viewManagers) {
                viewManagers.add(newViewManager);
            }
            try {
                Trace.call1("VMManager calling ViewManager.init");
                newViewManager.init();
                Trace.call2("VMManager calling ViewManager.init");
            } catch (Exception exc) {
                logException("Adding view manager", exc);
            }
            setLastActiveViewManager(newViewManager);
        }
        getIdvUIManager().viewManagerAdded(newViewManager);


    }

    /**
     * Add the view managers in the list
     *
     * @param newVms New view managers
     */
    public void addViewManagers(List newVms) {
        for (int i = 0; i < newVms.size(); i++) {
            ViewManager vm = (ViewManager) newVms.get(i);
            addViewManager(vm);
        }

    }

    /**
     * Remove all view managers
     */
    public void removeAllViewManagers() {
        removeAllViewManagers(true);
    }

    /**
     * Remove all view managers
     *
     * @param andDestroyThem If true then also call destroy
     */
    public void removeAllViewManagers(boolean andDestroyThem) {
        List local = new ArrayList(viewManagers);
        for (int i = 0; i < local.size(); i++) {
            ViewManager vm = (ViewManager) local.get(i);
            removeViewManager(vm);
            if (andDestroyThem) {
                vm.destroy();
            }
        }
    }


    /**
     * Remove the given view manager from the list.
     *
     * @param viewManager The view manager to be removed.
     */
    public void removeViewManager(ViewManager viewManager) {
        if (lastActiveViewManager == viewManager) {
            setLastActiveViewManager(null);
        }
        synchronized (viewManagers) {
            viewManagers.remove(viewManager);
        }
        transectViewsChanged();
    }


    /**
     * Find the first {@link ViewManager} in the list and
     * have it show its @[link ucar.unidata.view.geoloc.ProjectionManager}.
     */
    public void showProjectionManager() {
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager vm = (ViewManager) viewManagers.get(i);
            if (vm instanceof MapViewManager) {
                ((MapViewManager) vm).showProjectionManager();
                break;
            }
        }
    }



    /**
     * Do the initialization of the unpersisted {@link ViewManager}.
     *
     * @param newViewManagers List of view managers to unpersist
     */
    public void unpersistViewManagers(List newViewManagers) {
        try {
            for (int i = 0; i < newViewManagers.size(); i++) {
                ViewManager newViewManager =
                    (ViewManager) newViewManagers.get(i);
                newViewManager.initAfterUnPersistence(getIdv());
            }
        } catch (Exception exc) {
            logException("Unpersisting view manager", exc);
        }
    }



    /**
     *  Iterate through all of the ViewManager-s and tell each on to setMasterActive
     */
    public void setDisplayMastersActive() {
        synchronized (viewManagers) {
            for (int i = 0; i < viewManagers.size(); i++) {
                ((ViewManager) viewManagers.get(i)).setMasterActive(true);
            }
        }
    }


    /**
     *  Iterate through all of the ViewManager-s and tell each on to setMasterInactive
     */
    public void setDisplayMastersInactive() {
        synchronized (viewManagers) {
            for (int i = 0; i < viewManagers.size(); i++) {
                ((ViewManager) viewManagers.get(i)).setMasterInactive();
            }
        }
    }



    /**
     * As the name implies find the view manager identified by the given
     * viewDescriptor or create a new one.
     *
     * @param viewDescriptor The id to look for
     * @param properties Properties to pass if we create one.
     *
     * @return The found or created ViewManager
     */
    public ViewManager findOrCreateViewManager(ViewDescriptor viewDescriptor,
            String properties) {
        synchronized (viewManagers) {
            ViewManager viewManager = findViewManager(viewDescriptor);
            if (viewManager == null) {
                viewManager = createViewManager(viewDescriptor, properties);
            }
            return viewManager;
        }
    }


    /**
     * Create the given ViewManager
     *
     * @param viewDescriptor Identifies the VM
     * @param properties Property string to pass
     *
     * @return The new one
     */
    public ViewManager createViewManager(ViewDescriptor viewDescriptor,
                                         String properties) {
        synchronized (viewManagers) {
            try {
                ViewManager viewManager = null;
                if (viewDescriptor == null) {
                    viewDescriptor = new ViewDescriptor();
                }
                if (viewDescriptor.getClassNames().size() > 0) {
                    Class viewManagerClass =
                        Misc.findClass(
                            (String) viewDescriptor.getClassNames().get(0));
                    Constructor ctor = Misc.findConstructor(viewManagerClass,
                                           new Class[] {
                                               IntegratedDataViewer.class,
                            ViewDescriptor.class, String.class });

                    if (ctor == null) {
                        throw new IllegalArgumentException(
                            "cannot create ViewManager:"
                            + viewManagerClass.getName());
                    }

                    viewManager =
                        (ViewManager) ctor.newInstance(new Object[] {
                            getIdv(),
                            viewDescriptor, properties });
                } else {
                    viewManager = new MapViewManager(getIdv(),
                            viewDescriptor, properties);
                }

                addViewManager(viewManager);
                return viewManager;
            } catch (Throwable e) {
                logException("In getViewManager", e);
                return null;
            }
        }
    }

    /**
     * Center all of the MapViewManager-s at the given point
     *
     * @param el The point
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(EarthLocation el)
            throws VisADException, RemoteException {
        center(el, getViewManagers());
    }


    /**
     * Center the view managers in the list to the given point
     *
     * @param el Point to center to
     * @param viewManagers ViewManagers to center
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(EarthLocation el, List viewManagers)
            throws VisADException, RemoteException {
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if ( !(viewManager instanceof MapViewManager)) {
                continue;
            }
            NavigatedDisplay navDisplay =
                (NavigatedDisplay) ((MapViewManager) viewManager)
                    .getMapDisplay();
            navDisplay.center(el);
        }
    }


    /**
     * Center all of the MapViewManager-s at the given point
     *
     * @param mp Set all view managers projection to the
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(MapProjection mp)
            throws VisADException, RemoteException {
        center(mp, getViewManagers());
    }

    /**
     * Center all of the MapViewManager-s at the given point
     *
     * @param mp new projection
     * @param viewManagers ViewManagers to center
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(MapProjection mp, List viewManagers)
            throws VisADException, RemoteException {
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if ( !(viewManager instanceof MapViewManager)) {
                continue;
            }
            MapViewManager mvm = (MapViewManager) viewManager;
            mvm.setMapProjection(mp, false);
        }
    }



    /**
     * Zoom and center all of the MapViewManager-s at the given rect
     *
     * @param pr The projection rect to zoom and center to
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(ProjectionRect pr)
            throws VisADException, RemoteException {
        center(pr, getViewManagers());
    }

    /**
     * Zoom and center all of the MapViewManager-s at the given rect
     *
     * @param pr The projection rect to zoom and center to
     * @param viewManagers ViewManagers to center
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(ProjectionRect pr, List viewManagers)
            throws VisADException, RemoteException {
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if ( !(viewManager instanceof MapViewManager)) {
                continue;
            }
            NavigatedDisplay navDisplay =
                (NavigatedDisplay) ((MapViewManager) viewManager)
                    .getMapDisplay();
            navDisplay.setMapArea(pr);
        }
    }


    /**
     * Set the projection on all MapViewManagers to be the projection of the first
     * display.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center() throws VisADException, RemoteException {
        center(getViewManagers());
    }

    /**
     * Set the projection on all MapViewManagers to be the projection of the first
     * display.
     *
     *
     * @param viewManagers ViewManagers to center
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(List viewManagers)
            throws VisADException, RemoteException {
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if ( !(viewManager instanceof MapViewManager)) {
                continue;
            }
            ((MapViewManager) viewManager).setProjectionFromFirstDisplay();

        }
    }


    /**
     * Set the Transect used for all TransectViewManagers
     *
     * @param transect The transect
     */
    public void setTransect(Transect transect) {
        List viewManagers = getViewManagers();
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if (viewManager instanceof TransectViewManager) {
                ((TransectViewManager) viewManager).setTransect(transect);
            }
        }
    }





}

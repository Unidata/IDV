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


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.idv.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.view.geoloc.*;


import ucar.unidata.xml.*;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;



import java.io.File;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.*;




/**
 * This class is used to edit the set of {@link ucar.unidata.data.DataAlias}-s
 * used.
 *
 *
 *
 * @author IDV development team
 * @version $Revision: 1.28 $Date: 2006/12/27 20:14:08 $
 */


public class IdvProjectionManager extends IdvManager {

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            IdvProjectionManager.class.getName());


    /** Help ID for projection manager */
    private static final String PM_HELP_ID = "idv.tools.projectionmanager";

    /** Preference for  default projection */
    public static final String PREF_PROJ_DFLT = MapViewManager.PREF_PROJ_DFLT;


    /** List of projections in the projections resource */
    private List projectionList;

    /** List of system projections */
    private List nonLocalProjections = new ArrayList();

    /** Actually does most of the work */
    private ProjectionManager projectionManager;

    /**
     * Create an IdvProjectionManager with the given idv and list of resources.
     *
     * @param idv Reference to the idv
     */
    public IdvProjectionManager(IntegratedDataViewer idv) {
        super(idv);
    }





    /**
     *  Initialize the set of Projection-s that are defined in the projectionResource
     *
     */
    protected void initProjections() {

        if (projectionList == null) {
            projectionList = new ArrayList();
        }
        XmlEncoder encoder = getIdv().getEncoderForRead();
        Hashtable  seen    = new Hashtable();
        ResourceCollection rc = getResourceManager().getResources(
                                    IdvResourceManager.RSC_PROJECTIONS);
        nonLocalProjections = new ArrayList();
        List localProjections = new ArrayList();
        for (int resourceIdx = 0; resourceIdx < rc.size(); resourceIdx++) {
            String xml   = rc.read(resourceIdx);
            List   projs = null;

            //If we don't have the user's projection list then check to see
            //if they have one in their preferences. If they do then use that
            //and remove the preference.
            if (xml == null) {
                //See if we can load in the old preferences
                if (resourceIdx == 0) {
                    projs = (List) getStore().get(PREF_PROJ_LIST);
                    if (projs != null) {
                        getStore().remove(PREF_PROJ_LIST);
                        getStore().save();
                    }
                }
            } else {
                Element root = null;

                try {
                    root = XmlUtil.getRoot(xml);
                } catch (Throwable parseExc) {
                    //Should we ignore this xml (perhaps its html from a 404 on a url).
                    if (Misc.isHtml(xml)) {}
                    else {
                        logException("Decoding projections", parseExc);
                    }
                }
                if (root != null) {
                    try {
                        projs = (List) encoder.toObject(root);
                    } catch (Throwable exc) {
                        logException("Decoding projections", exc);
                    }
                }
            }
            if (projs == null) {
                continue;
            }
            if ( !rc.isWritable(resourceIdx)) {
                nonLocalProjections.addAll(projs);
            } else {
                localProjections.addAll(projs);
            }
            /*
            for (int projIdx = 0; projIdx < projs.size(); projIdx++) {
                ProjectionImpl proj = (ProjectionImpl) projs.get(projIdx);
                if (seen.get(proj.getName()) != null) {
                    continue;
                }
                projectionList.add(proj);
            }
            */
        }

        for (int i = 0; i < nonLocalProjections.size(); i++) {
            ProjectionImpl proj = (ProjectionImpl) nonLocalProjections.get(i);
            if (seen.get(proj.getName()) == null) {
                projectionList.add(proj);
                seen.put(proj.getName(), "");
            }
        }

        for (int i = 0; i < localProjections.size(); i++) {
            ProjectionImpl proj = (ProjectionImpl) localProjections.get(i);
            if ( !contains(projectionList, proj)) {
                //Now, look to see where we can put this local projection
                //              System.err.println ("** Adding local:" + proj.getName());
                for (int projIdx = 0; projIdx < projectionList.size();
                        projIdx++) {
                    ProjectionImpl tmpProj =
                        (ProjectionImpl) projectionList.get(projIdx);
                    if (Misc.equals(tmpProj.getName(), proj.getName())) {
                        projectionList.set(projIdx, proj);
                        proj = null;
                        break;
                    }
                }

                if (proj != null) {
                    projectionList.add(proj);
                    seen.put(proj.getName(), proj);
                }
            } else {
                //              System.err.println ("Not adding local:" + proj.getName());
            }
        }

        //      System.err.println ("projs:" + projectionList);

        if (projectionList.size() == 0) {
            projectionList = ProjectionManager.makeDefaultProjections();
        }
    }


    /**
     * Does the given list of projections contain the given projection. We do this because
     * we also want to use name eqauality
     *
     * @param projs List of projections
     * @param proj The projection
     *
     * @return List contains proj
     */
    private boolean contains(List projs, ProjectionImpl proj) {
        for (int i = 0; i < projs.size(); i++) {
            ProjectionImpl that = (ProjectionImpl) projs.get(i);
            if (that.equals(proj)) {
                //We do this since they don't check the name in equals
                if (Misc.equals(that.getName(), proj.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Find the projeciton with the given name
     *
     * @param n Name
     *
     * @return Projection or null if none found
     */
    public ProjectionImpl findProjectionByName(String n) {
        List projections = getProjections();
        for (int i = 0; i < projections.size(); i++) {
            ProjectionImpl proj = (ProjectionImpl) projections.get(i);
            if (proj.getName().equals(n)) {
                return proj;
            }
        }
        return null;
    }


    /**
     * Tell all MapViewManagers to update their projection
     */
    private void updateProjections() {
        List viewManagers = getIdv().getVMManager().getViewManagers();
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager vm = (ViewManager) viewManagers.get(i);
            if (vm instanceof MapViewManager) {
                try {
                    ((MapViewManager) vm).updateProjection();
                } catch (Throwable exc) {
                    logException("Updating projections", exc);
                }
            }
        }
    }



    /**
     *  Write out the  set of {@link ucar.unidata.geoloc.Projection}Projection-s
     * (using XmlEncoder) into the file defined by the writable resource of
     * the projectionResources
     *
     * @param newProjections List of {@link ucar.unidata.geoloc.Projection} objects to write
     */
    public void storeProjections(List newProjections) {
        ResourceCollection rc = getResourceManager().getResources(
                                    getResourceManager().RSC_PROJECTIONS);
        List localProjections = new ArrayList();
        for (int i = 0; i < newProjections.size(); i++) {
            ProjectionImpl proj = (ProjectionImpl) newProjections.get(i);
            if ( !contains(nonLocalProjections, proj)) {
                localProjections.add(proj);
            } else {}
        }



        if (rc.getWritable() != null) {
            try {
                String xml =
                    (getIdv().getEncoderForWrite()).toXml(localProjections,
                        true);
                FileOutputStream fos = new FileOutputStream(rc.getWritable());
                fos.write(xml.getBytes());
                fos.close();
            } catch (Throwable exc) {
                logException("Saving projections", exc);
            }
        }
    }




    /**
     * Get the list of  Projections available.
     *
     * @return The list of projections
     */
    public List getProjections() {
        return projectionList;
    }

    /**
     * Show the gui
     */
    public void show() {
        showProjectionManager();
    }

    /**
     * Show the projection manager.
     */
    private void showProjectionManager() {
        // user has requested to see the Projection Manager
        try {
            List projections = getProjections();
            List maps        = getIdv().getResourceManager().getMaps();
            if (projectionManager == null) {
                projectionManager = new ProjectionManager(null, projections,
                        true, PM_HELP_ID, maps);
                projectionManager.addPropertyChangeListener(
                    new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        if ( !e.getPropertyName().equals(
                                ProjectionManager.PROPERTY_CHANGE)) {
                            return;
                        }
                        //Store as a preference the set of projections (in case the
                        //user created new ones) and the default projection.
                        storeProjections(projectionManager.getProjections());
                        getStore().save();
                        updateProjections();
                    }
                });
            } else {
                projectionManager.setProjections(projections);
                projectionManager.setMaps(maps);

            }
            projectionManager.show();
        } catch (Exception exp) {
            logException("getUI ().getProjectionManager ()", exp);
        }
    }



    /**
     * Add in the different  preference panels.
     *
     * @param preferenceManager The preference manager to add things into
     */
    public void initPreferences(IdvPreferenceManager preferenceManager) {
        PreferenceManager projManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                applyProjectionPreferences(theStore, data);
            }
        };
        List projections = getProjections();
        ProjectionImpl defaultProjection =
            (ProjectionImpl) getStore().get(PREF_PROJ_DFLT);

        ProjectionManager pm = new ProjectionManager(null, projections,
                                   false, PM_HELP_ID);
        JPanel contents = GuiUtils.doLayout(new Component[] {
                              pm.getContents() }, 1, GuiUtils.WT_Y,
                                  GuiUtils.WT_Y);

        preferenceManager.add("Map Projections", "Default Map Projections",
                              projManager, contents, pm);
    }


    /**
     * Store the projection preferences.
     *
     * @param store The object store
     * @param data This is the projection manager
     */
    public void applyProjectionPreferences(XmlObjectStore store,
                                           Object data) {
        getIdv().getIdvProjectionManager().storeProjections(
            ((ProjectionManager) data).getProjections());
    }




    /**
     * Get the default projection to use
     *
     * @return The default projection
     */
    public ProjectionImpl getDefaultProjection() {
        ProjectionImpl dfltProjection =
            (ProjectionImpl) getStore().get(PREF_PROJ_DFLT);
        List projections = getProjections();

        if ((dfltProjection == null) && (projections.size() > 0)) {
            String projectionName =
                getStateManager().getProperty(IdvConstants.PROP_PROJ_NAME,
                    (String) null);
            if (projectionName != null) {
                projectionName = projectionName.trim();
                dfltProjection = (ProjectionImpl) projections.get(0);
                for (int projIdx = 0; projIdx < projections.size();
                        projIdx++) {
                    ProjectionImpl proj =
                        (ProjectionImpl) projections.get(projIdx);
                    if (Misc.equals(projectionName, proj.getName())
                            || proj.getName().endsWith(">"
                                + projectionName)) {
                        dfltProjection = proj;
                        break;
                    }
                }
            }
        }

        if ((dfltProjection == null) && (projections.size() > 0)) {
            dfltProjection = (ProjectionImpl) projections.get(0);
        }

        if (dfltProjection == null) {
            dfltProjection = makeDefaultProjection();
            getStore().put(PREF_PROJ_DFLT, dfltProjection);
        }



        return dfltProjection;
    }


    /**
     * Make the default display projection
     * @return Default display projection
     */
    protected ProjectionImpl makeDefaultProjection() {
        return new LatLonProjection("World",
                                    new ProjectionRect(-180., -180., 180.,
                                        180.));
    }






}

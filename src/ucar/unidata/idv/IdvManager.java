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


import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.collab.CollabManager;
import ucar.unidata.idv.publish.PublishManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.ImageGenerator;
import ucar.unidata.ui.WindowHolder;
import ucar.unidata.ui.colortable.ColorTableManager;

import ucar.unidata.ui.symbol.StationModelManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import visad.ActionImpl;

import java.awt.*;

import java.lang.management.*;

import javax.swing.*;


/**
 * This is a base class for the different IDV managers (e.g.,
 * {@link ucar.unidata.idv.ui.IdvUIManager}. It provides  a couple
 * of utilities.
 *
 *
 * @author IDV development team
 */
public abstract class IdvManager extends WindowHolder implements IdvConstants {


    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(IdvManager.class.getName());


    /** Reference to the IDV */
    private IntegratedDataViewer idv;


    /**
     * Construct this object with the given IDV
     *
     * @param idv The IDV
     */
    public IdvManager(IntegratedDataViewer idv) {
        this.idv = idv;
    }



    /**
     * Get the IDV
     *
     * @return The IDV
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }

    /**
     * Get the Class of the IDV.
     *
     * @return The  Class of the IDV
     */
    protected Class getIdvClass() {
        return getIdv().getClass();
    }


    /**
     * Wrapper method, calling into idv
     *
     * @return The store from the  IDV
     */

    protected IdvObjectStore getStore() {
        return idv.getStore();
    }

    /**
     * Wrapper method, calling into idv
     *
     * @return The ResourceManager from the IDV
     */

    protected IdvResourceManager getResourceManager() {
        return idv.getResourceManager();
    }

    /**
     * Get the station model manager
     *
     *
     * @return The station model manager
     */

    public StationModelManager getStationModelManager() {
        return getIdv().getStationModelManager();
    }


    /**
     * Get the projection manager
     *
     *
     * @return The projection manager
     */

    public IdvProjectionManager getIdvProjectionManager() {
        return getIdv().getIdvProjectionManager();
    }


    /**
     * Get the persistence manager
     *
     *
     * @return The persistence manager
     */
    public IdvPersistenceManager getPersistenceManager() {
        return idv.getPersistenceManager();
    }

    /**
     * Get the preference manager
     *
     *
     * @return The preference manager
     */
    public IdvPreferenceManager getPreferenceManager() {
        return idv.getPreferenceManager();
    }

    /**
     * Get the
     * {@link ucar.unidata.ui.colortable.ColorTableManager}
     *
     * @return The color table manager
     */
    public ColorTableManager getColorTableManager() {
        return getIdv().getColorTableManager();
    }


    /**
     * Wrapper method, calling into idv
     *
     * @return The ui manager from the IDV
     */

    protected IdvUIManager getIdvUIManager() {
        return idv.getIdvUIManager();
    }


    /**
     * Wrapper method, calling into idv
     *
     * @return The image generator from the IDV
     */

    protected ImageGenerator getImageGenerator() {
        return idv.getImageGenerator();
    }



    /**
     * Wrapper method, calling into idv
     *
     * @return The chooser manager from the IDV
     */

    protected IdvChooserManager getIdvChooserManager() {
        return idv.getIdvChooserManager();
    }


    /**
     * Wrapper method, calling into idv
     *
     * @return The jython manager from the IDV
     */

    protected JythonManager getJythonManager() {
        return idv.getJythonManager();
    }



    /**
     * Wrapper method, calling into idv
     *
     * @return The args manager from the IDV
     */

    protected ArgsManager getArgsManager() {
        return idv.getArgsManager();
    }


    /**
     * Wrapper method, calling into idv
     *
     * @return The VM manager from the IDV
     */

    protected VMManager getVMManager() {
        return idv.getVMManager();
    }


    /**
     * Wrapper method, calling into idv
     *
     * @return The Publish manager from the IDV
     */

    protected PublishManager getPublishManager() {
        return idv.getPublishManager();
    }



    /**
     * Wrapper method, calling into idv
     *
     * @return The state manager from the IDV
     */

    protected StateManager getStateManager() {
        return idv.getStateManager();
    }

    /**
     * Wrapper method, calling into idv
     *
     * @return The data manager from the IDV
     */

    protected DataManager getDataManager() {
        return idv.getDataManager();
    }


    protected InstallManager getInstallManager() {
        return idv.getInstallManager();
    }




    /**
     * Wrapper method, calling into idv
     *
     * @return The collab manager from the IDV
     */

    protected CollabManager getCollabManager() {
        return idv.getCollabManager();
    }



    /**
     * Wrapper method, calling into idv
     *
     * @param msg The message
     * @param excp The exception
     */
    public static void logException(String msg, Throwable excp) {
        LogUtil.printException(log_, msg, excp);
    }


    /**
     * Wrapper method, calling into idv
     *
     */
    protected void showWaitCursor() {
        idv.showWaitCursor();
    }

    /**
     * Wrapper method, calling into idv
     *
     */

    protected void showNormalCursor() {
        idv.showNormalCursor();
    }


    /**
     *  Utility method to retrieve a boolean property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name The name of the property
     * @param dflt The default value if the property is not found
     * @return The given property or the dflt value
     */
    public boolean getProperty(String name, boolean dflt) {
        return getStateManager().getProperty(name, dflt);
    }


    /**
     *  Utility method to retrieve an int property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name The name of the property
     * @param dflt The default value if the property is not found
     * @return The given property or the dflt value
     */
    public int getProperty(String name, int dflt) {
        return getStateManager().getProperty(name, dflt);
    }


    /**
     *  Utility method to retrieve a String property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name The name of the property
     * @param dflt The default value if the property is not found
     * @return The given property or the dflt value
     */
    public String getProperty(String name, String dflt) {
        return getStateManager().getProperty(name, dflt);
    }





    /**
     * A utility method that will wait until all displays are finished
     * being created.
     *
     * @param uiManager The ui manager. We use this to access the wait
     * cursor count
     */
    public static void waitUntilDisplaysAreDone(IdvUIManager uiManager) {
        long timeToWait = 100;
        waitUntilDisplaysAreDone(uiManager, timeToWait);
    }

    /**
     * A utility method that will wait until all displays are finished
     * being created. This looks at the DisplayControls, data sources, global wait cursor count,
     * the visad thread pool and looks at any active java3d threads
     *
     * @param uiManager The ui manager. We use this to access the wait
     * cursor count
     * @param timeToWait (milliseconds) elapsed time to wait for nothing to be active
     */
    public static void waitUntilDisplaysAreDone(IdvUIManager uiManager,
            long timeToWait) {
        Trace.call1("Waiting on displays");
        int  successiveTimesWithNoActive = 0;
        int  sleepTime                   = 10;
        long firstTime                   = System.currentTimeMillis();
        int  cnt                         = 0;
        while (true) {
            boolean cursorCount = (uiManager.getWaitCursorCount() > 0);
            boolean actionCount = ActionImpl.getTaskCount() > 0;
            boolean dataActive = DataSourceImpl.getOutstandingGetDataCalls()
                                 > 0;
            boolean anyJ3dActive = anyJava3dThreadsActive();
            boolean allDisplaysInitialized =
                uiManager.getIdv().getAllDisplaysIntialized();

            //            System.err.println ("\tAll displays init:" + allDisplaysInitialized +" cursor cnt:" + uiManager.getWaitCursorCount() + " action cnt:" +actionCount + " data active: " + dataActive);
            //            if ((cnt++) % 30 == 0) {
            //                System.err.println ("\tcnt:" + uiManager.getWaitCursorCount() + " " +actionCount + " " + dataActive);
            //            }
            boolean anyActive = actionCount || cursorCount || dataActive
                                || !allDisplaysInitialized || anyJ3dActive;
            if (dataActive) {
                firstTime = System.currentTimeMillis();
            }

            if (anyActive) {
                successiveTimesWithNoActive = 0;
            } else {
                successiveTimesWithNoActive++;
            }
            if ((timeToWait == 0) && !anyActive) {
                break;
            }
            if (successiveTimesWithNoActive * sleepTime > timeToWait) {
                break;
            }
            Misc.sleep(sleepTime);
            //At most wait 120 seconds
            if (System.currentTimeMillis() - firstTime > 120000) {
                System.err.println("Error waiting for to be done:"
                                   + LogUtil.getStackDump(false));
                return;
            }
        }
        Trace.call2("Waiting on displays");

    }


    /**
     * A total hack to see if there are any active Java3D threads running
     *
     * @return any java3d threads running
     */
    private static boolean anyJava3dThreadsActive() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[]       ids        = threadBean.getAllThreadIds();
        for (int i = 0; i < ids.length; i++) {
            ThreadInfo info = threadBean.getThreadInfo(ids[i],
                                  Integer.MAX_VALUE);
            if (info == null) {
                continue;
            }
            if (info.getThreadState() != Thread.State.RUNNABLE) {
                continue;
            }
            if (info.getThreadName().indexOf("J3D") >= 0) {
                return true;
            }
        }
        return false;
    }





}

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




import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;


import ucar.unidata.xml.*;

import java.lang.management.*;


import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.swing.*;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;




/**
 * @author IDV development team
 */


public class InstallManager extends IdvManager {

    /**
     * Create this manager
     *
     * @param idv The IDV
     *
     */
    public InstallManager(IntegratedDataViewer idv) {
        super(idv);
    }




    public static final String PROP_INSTALL_BUILDPROPERTIES = "idv.install.buildproperties";
    public static final String PROP_BUILDDATE = "idv.build.date";

    public boolean isInstallFromJars() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        String jarFile = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        return jarFile.endsWith(".jar");
    }


    public boolean isRestartable() {
        return isInstallFromJars();
    }

    public void installFromNightlyBuild() {
        if (!GuiUtils.showYesNoDialog(
                null,
                "Are you sure you want to install the latest nightly build?",
                "Install")) {
            return;
        }



        installNewRelease(getProperty("idv.install.nightlyjars",""));

    }


    public void installNewRelease(String jarsPath) {
        if(!isInstallFromJars()) {
            LogUtil.userMessage("You are not running from the installer");
            return;
        }
        if(jarsPath==null || jarsPath.length()==0) {
            LogUtil.userMessage("No install path found");
            return;
        }

        installJars(jarsPath);
    }


    private  void  installJars(String zipFileUrl) {
        if(!isRestartable()) {
            LogUtil.userMessage("You will need to restart the IDV for this change to take effect");
            return;
        }

        if(GuiUtils.askYesNo("Restart", new JLabel("<html>You will need to restart the IDV for this change to take effect<br>Do you want to restart?"))) {
            try {
                restart();
            } catch (Throwable exc) {
                logException("Restarting the IDV", exc);
            }
        }
    }



    private void automaticallyCheckForUpdates() {
        if(!haveNewRelease()) {
            return;
        }



    }


    public void checkForUpdates() {
        if(!haveNewRelease()) {
            LogUtil.userMessage("You are running the latest version from: " + getStateManager().getBuildDate());
            //            return;
        }
        if (!GuiUtils.showYesNoDialog(
                null,
                "<html>A new IDV release is available.<br>Are you sure you want to install it?</html>",
                "Install")) {
            return;
        }
        installNewRelease(getProperty("idv.install.currentjars",""));
    }

    public boolean haveNewRelease() {
        try {
            String buildPropertiesPath = getProperty(PROP_INSTALL_BUILDPROPERTIES,"");
            if(buildPropertiesPath==null || buildPropertiesPath.length()==0) {
                return false;
            }
            Properties props =
                Misc.readProperties(buildPropertiesPath, null, getClass());
            String currentBuildDateString = (String)props.get(PROP_BUILDDATE);
            if(currentBuildDateString == null) return false;
            String myBuildDateString = getStateManager().getBuildDate();
            Date currentBuildDate = DateUtil.parse(currentBuildDateString);
            Date myBuildDate = DateUtil.parse(myBuildDateString);
            return myBuildDate.getTime()<currentBuildDate.getTime();
        } catch(Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    public void restart() throws Exception {
        restart(getArgsManager().getOriginalArgs());
    }

    private void restart(String[]cmdLineArgs) throws Exception {
        if(!isRestartable())  return;
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List args = new ArrayList();
        String jarFile = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        String javaHome = System.getProperty("java.home");
        String javaBin =   javaHome +"/bin/java";  
        args.add(javaBin);
        args.addAll(bean.getInputArguments());
        args.add("-jar");
        args.add(jarFile);
        System.err.println("Restarting the IDV with:" + args);
        String[]toExec = Misc.listToStringArray(args);
        try {
            Process p = Runtime.getRuntime().exec(toExec);
            OutputStream out = p.getOutputStream();
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Misc.sleep(1000);
        System.exit(0);
    }


}

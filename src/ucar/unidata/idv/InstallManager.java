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

import ucar.unidata.util.DateUtil;




import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;


import ucar.unidata.xml.*;

import java.awt.*;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.management.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.zip.*;

import javax.swing.*;




/**
 * This handles restarting the IDV and re-installing the jar files
 * @author IDV development team
 */


public class InstallManager extends IdvManager {

    /** _more_ */
    public static final String PROP_BUILDPROPERTIES =
        "idv.install.buildproperties";

    /** _more_ */
    public static final String PROP_BUILDDATE = "idv.build.date";

    /** _more_          */
    public static final String PROP_CURRENTJARS = "idv.install.currentjars";

    /** _more_          */
    public static final String PROP_NIGHTLYJARS = "idv.install.nightlyjars";

    /** _more_          */
    public static final String PREF_CHECKFORNEWRELEASE =
        "idv.install.checkfornewrelease";

    /** _more_          */
    public static final String PREF_LASTTIMECHECKEDFORNEWRELEASE =
        "idv.install.lasttimecheckedfornewrelease";


    /**
     * Create this manager
     *
     * @param idv The IDV
     *
     */
    public InstallManager(IntegratedDataViewer idv) {
        super(idv);
    }






    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isInstallFromJars() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        String jarFile =
            getClass().getProtectionDomain().getCodeSource().getLocation()
                .getPath();
        return jarFile.endsWith(".jar");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRestartable() {
        String javaBin = getJavaPath();
        if (javaBin == null) {
            return false;
        }
        if ( !new File(javaBin).exists()) {
            System.err.println("No java at:" + javaBin);
            return false;
        }
        return isInstallFromJars();
    }

    /**
     * _more_
     */
    public void installFromNightlyBuild() {
        if ( !GuiUtils.showYesNoDialog(
                null,
                "Are you sure you want to install the latest nightly build?",
                "Install")) {
            return;
        }
        installNewRelease(getProperty(PROP_NIGHTLYJARS, ""));
    }


    /**
     * _more_
     *
     * @param jarsPath _more_
     */
    public void installNewRelease(String jarsPath) {
        if ( !isInstallFromJars()) {
            LogUtil.userMessage("You are not running from the installer");
            return;
        }
        if ((jarsPath == null) || (jarsPath.length() == 0)) {
            LogUtil.userMessage("No install path found");
            return;
        }

        installJars(jarsPath);
    }


    /**
     * _more_
     */
    public void testcp() {
        Hashtable<String, Date> jarDateMap = new Hashtable<String, Date>();
        String                  cp = System.getProperty("java.class.path");
        if (cp == null) {
            return;
        }
        boolean anyChanged = false;
        for (String path : StringUtil.split(cp, ":", true, true)) {
            File f = new File(path);
            if ( !f.exists() || !f.isFile() || !path.endsWith(".jar")) {
                continue;
            }
            String jarName = f.getName();
            System.err.println("path:" + jarName);
            Date d = jarDateMap.get(jarName);
            if (d == null) {
                continue;
            }
            if (d.getTime() > f.lastModified()) {
                anyChanged = true;
                break;
            }
        }
    }



    /**
     * _more_
     *
     * @param zipFileUrl _more_
     */
    private void installJars(String zipFileUrl) {
        if ( !isRestartable()) {
            LogUtil.userMessage(
                "You will need to restart the IDV for this change to take effect");
            return;
        }



        //write out a temp bundle now before we change all of the jar files
        String tmpBundle = getStore().getTmpFile("bundle.xidv");
        try {
            getPersistenceManager().doSave(tmpBundle);

            String jarFile =
                getClass().getProtectionDomain().getCodeSource().getLocation()
                    .getPath();
            File           dir = new File(jarFile).getParentFile();
            InputStream    is  = IOUtil.getInputStream(zipFileUrl,
                                     getClass());
            ZipInputStream zin = new ZipInputStream(is);
            ZipEntry       ze  = null;
            System.err.println("Writing new jars to: " + dir);
            while ((ze = zin.getNextEntry()) != null) {
                String entryName = ze.getName();
                if (ze.isDirectory()) {
                    continue;
                }
                //Only get jar files
                if ( !entryName.endsWith(".jar")) {
                    continue;
                }
                System.err.println("  writing jar:" + entryName);
                String dest = IOUtil.joinDir(dir, entryName);
                IOUtil.writeTo(zin, new FileOutputStream(dest));
            }

            zin.close();
            is.close();
        } catch (Throwable exc) {
            logException("Error fetching release: " + zipFileUrl, exc);
            return;
        }


        if (GuiUtils.askYesNo(
                "Restart",
                new JLabel(
                    "<html>You will need to restart the IDV for this change to take effect<br>Do you want to restart?"))) {
            try {
                restart(null, tmpBundle);
            } catch (Throwable exc) {
                exc.printStackTrace();
                logException("Restarting the IDV", exc);
            }
        }
    }



    /**
     * Automatically check if there is a new IDV version available
     */
    protected void automaticallyCheckForUpdates() {
        if(true) return;


        //If running tests or isl then punt
        if (LogUtil.getTestMode()) {
            return;
        }

        //If in dev mode then punt
        if ( !isInstallFromJars()) {
            //TEST return;
        }

        //If the user told us not to nag them then punt
        if ( !getStore().get(PREF_CHECKFORNEWRELEASE, true)) {
            return;
        }

        try {
            Date date = (Date) getStateManager().getPreference(
                            PREF_LASTTIMECHECKEDFORNEWRELEASE);
            Date now = new Date();
            if (date != null) {
                if (DateUtil.millisToHours(now.getTime() - date.getTime())
                        < 24) {
                    //TEST      return;
                }
            } else {
                //Have never checked so lets not bug them the first time
                getStore().put(PREF_LASTTIMECHECKEDFORNEWRELEASE, now);
                //TEST  return;
            }


            if ( !haveNewRelease()) {
                //TEST            return;
            }

            int result =
                GuiUtils.makeDialog(
                    null, "Install",
                    GuiUtils.inset(
                        new JLabel(
                            "<html>A new IDV release is available. Would you like to install it?</html>"), new Insets(
                            10, 10, 5, 10)), null, new String[] { "Yes",
                    "Not right now", "Don't ask anymore" });

            getStore().put(PREF_LASTTIMECHECKEDFORNEWRELEASE, now);

            //Not right now
            if (result == 1) {
                return;
            }

            //Don't bother me
            if (result == 2) {
                getStore().put(PREF_CHECKFORNEWRELEASE, false);
                return;
            }

        } finally {
            getStore().saveIfNeeded();
        }

        installNewRelease(getProperty(PROP_CURRENTJARS, ""));
    }



    /**
     * _more_
     */
    public void checkForUpdates() {
        if ( !haveNewRelease()) {
            LogUtil.userMessage("You are running the latest version from: "
                                + getStateManager().getBuildDate());
            //            return;
        }
        if ( !GuiUtils.showYesNoDialog(
                null,
                "<html>A new IDV release is available.<br>Would you like to install it?</html>",
                "Install")) {
            return;
        }
        installNewRelease(getProperty(PROP_CURRENTJARS, ""));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean haveNewRelease() {
        try {
            String buildPropertiesPath = getProperty(PROP_BUILDPROPERTIES,
                                             "");
            if ((buildPropertiesPath == null)
                    || (buildPropertiesPath.length() == 0)) {
                return false;
            }
            Properties props = Misc.readProperties(buildPropertiesPath, null,
                                   getClass());
            String currentBuildDateString =
                (String) props.get(PROP_BUILDDATE);
            if (currentBuildDateString == null) {
                return false;
            }
            String myBuildDateString = getStateManager().getBuildDate();
            Date   currentBuildDate  = DateUtil.parse(currentBuildDateString);
            Date   myBuildDate       = DateUtil.parse(myBuildDateString);
            return myBuildDate.getTime() < currentBuildDate.getTime();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void restart() throws Exception {
        restart(getArgsManager().getOriginalArgs(), null);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private String getJavaPath() {
        String javaHome = System.getProperty("java.home");
        String javaBin  = javaHome + "/bin/java";
        return javaBin;
    }



    /**
     * _more_
     *
     * @param cmdLineArgs _more_
     * @param loadBundle _more_
     *
     * @throws Exception _more_
     */
    private void restart(String[] cmdLineArgs, String loadBundle)
            throws Exception {
        if ( !isRestartable()) {
            return;
        }
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List          args = new ArrayList();
        String jarFile =
            getClass().getProtectionDomain().getCodeSource().getLocation()
                .getPath();
        String javaBin = getJavaPath();
        args.add(javaBin);
        args.addAll(bean.getInputArguments());
        args.add("-jar");
        args.add(jarFile);
        if (loadBundle != null) {
            args.add(loadBundle);
        }

        args.add("-nodefault");
        System.err.println("Restarting the IDV with:" + args);
        String[] toExec = Misc.listToStringArray(args);
        try {
            Process      p   = Runtime.getRuntime().exec(toExec);
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

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


import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


import javax.swing.*;



/**
 * Provides some IDV specific extensions to the XmlObjectStore. Creates the
 * users local .unidata/idv directory, jython cache dir, etc.
 *
 * @author IDV development team
 */

public class IdvObjectStore extends XmlObjectStore implements IdvConstants {

    /** tmp dir */
    public static final String PROP_TMPDIR = "idv.tmpdir";


    /** The subdirectory name for holding favorites bundles */
    public static final String DIR_BUNDLES = "bundles";

    /** Where we save bundles to */
    public static final String DIR_SAVEDBUNDLES = "savedbundles";


    /** Local copy of the IDV */
    private IntegratedDataViewer idv;


    /** The system directory. e.g., .unidata/idv */
    String systemDirectory;

    /** The users local state directory. e.g., This is the .unidata/idv/DefaultIdv */
    File userDirectory;

    /** Was the users directory created for the first time */
    boolean madeUserDirectory = false;

    /** Did the user override the user path or are we using the normal  one */
    boolean usingNormalUserDirectory = true;





    /**
     * Create the store.
     *
     * @param idv        The IntegratedDataViewer for this object store
     * @param systemName The name of the top level directory (e.g., .unidata/idv)
     * @param appName The name of the subdir (e.g., DefaultIdv)
     * @param encoder The encoder to use for writing out and reading in  the store
     * @param overrideUserDirectory If non-null this overrides wehre we look for
     * the user's home directory. Else we use the user.home system property.
     */
    public IdvObjectStore(IntegratedDataViewer idv, String systemName,
                          String appName, XmlEncoder encoder,
                          String overrideUserDirectory) {
        super(encoder);
        this.idv = idv;
        if (overrideUserDirectory != null) {
            systemDirectory          = overrideUserDirectory;
            userDirectory            = new File(overrideUserDirectory);
            usingNormalUserDirectory = false;
        } else {

            String userHome = Misc.getSystemProperty("user.home", ".");
            systemDirectory = IOUtil.joinDir(userHome, "." + systemName);

            if (systemName.equals("unidata/idv")) {
                if ( !new File(systemDirectory).exists()) {
                    File metappsDir = new File(IOUtil.joinDir(userHome,
                                          ".metapps"));
                    File toDir = new File(systemDirectory);
                    IOUtil.makeDirRecursive(new File(IOUtil.joinDir(userHome,
                            ".unidata")));
                    if (metappsDir.exists()) {
                        JLabel label =
                            new JLabel(
                                "<html>The IDV is moving the user state directory (.metapps):<br><i>&nbsp;&nbsp;"
                                + metappsDir
                                + "</i><br> to:<br>&nbsp&nbsp;<i>" + toDir
                                + "</i><p><br>This shouldn't be a problem and will just take a second");
                        boolean ok = metappsDir.renameTo(toDir);
                        if ( !ok) {
                            JLabel msg =
                                new JLabel(
                                    "<html>The IDV attempted to move the user directory to a new location.<br>For some reason the directory move failed.<p><br>Please move the directory:<br><i>&nbsp;&nbsp;"
                                    + metappsDir
                                    + "</i><br> to:<br>&nbsp;&nbsp;<i>"
                                    + toDir
                                    + "</i><p><br>And then restart the IDV");
                            javax.swing.JOptionPane.showMessageDialog(null,
                                    msg, "Error", JOptionPane.ERROR_MESSAGE);
                            System.exit(0);
                        } else {
                            /*
                            GuiUtils.showOkDialog(
                                null, "Move succeeded",
                                GuiUtils.inset(
                                    new JLabel(
                                    "The directory has been moved"), 5), null);*/
                        }
                    }
                }
            }

            IOUtil.makeDirRecursive(new File(systemDirectory));
            userDirectory = new File(IOUtil.joinDir(systemDirectory,
                    appName));

        }

        if ( !userDirectory.exists()) {
            madeUserDirectory = true;
            IOUtil.makeDir(userDirectory);
        }
        //Make the plugins directory
        getPluginsDir();
    }



    /**
     *  Get the MadeUserDirectory property. Was the user's directory created now.
     *
     *  @return The MadeUserDirectory
     */
    public boolean getMadeUserDirectory() {
        return madeUserDirectory;
    }



    /**
     * Get the path to the system directory. Ex: ~/.unidata/idv
     *
     * @return The system directory path.
     */
    public String getSystemDirectory() {
        return systemDirectory;
    }



    /**
     * Overrwrite base class method to tweak any old property names
     *
     * @param newTable The table to process
     *
     * @return The tweaked table
     */
    protected Hashtable processTable(Hashtable newTable) {
        return idv.getStateManager().processPropertyTable(newTable);
    }

    /**
     * Create, if needed, and return the directory to use for the jython cache.
     *
     * @return The jython cache directory
     */
    public String getJythonCacheDir() {
        String dirName = "jython";
        String jythonVersion = idv.getStateManager().getJythonVersion();
        if (jythonVersion != null) {
        	dirName += jythonVersion;
        }
        if ( !usingNormalUserDirectory) {
            return IOUtil.makeDir(IOUtil.joinDir(userDirectory, dirName));
        }
        if ( !userDirectoryOk()) {
            return null;
        }
        return IOUtil.makeDir(IOUtil.joinDir(systemDirectory, dirName));
    }

    /**
     * Get the users local directory. Example: ~/.unidata/idv/DefaultIdv
     *
     * @return The user's local IDV directory
     */
    public File getUserDirectory() {
        return userDirectory;
    }

    /**
     * Set the override directory.
     *
     * @param newDirectory dir to use
     */
    public void setOverrideDirectory(File newDirectory) {
        userDirectory   = newDirectory;
        systemDirectory = newDirectory.toString();
    }



    /**
     * Return  the full path to the directory where we save the display control templates
     *
     * @return Template directory
     */
    public String getDisplayTemplateDir() {
        String tmpDir = IOUtil.joinDir(getUserDirectory().toString(),
                                       "displaytemplates");
        IOUtil.makeDir(tmpDir);
        return tmpDir;
    }


    /**
     * Return  the full path to the directory where we save the display control templates
     *
     * @return Template directory
     */
    public String getDataSourcesDir() {
        String tmpDir = IOUtil.joinDir(getUserDirectory().toString(),
                                       "datasources");
        IOUtil.makeDir(tmpDir);
        return tmpDir;
    }


    /**
     * Return  the full path to the directory where we look for plugins
     *
     * @return Plugins directory
     */
    public String getPluginsDir() {
        String tmpDir = IOUtil.joinDir(getUserDirectory().toString(),
                                       "plugins");
        IOUtil.makeDir(tmpDir);
        return tmpDir;
    }


    /**
     * Return  the full path to the directory where we save the user's local bundles
     *
     * @return Bundle directory
     */
    public String getLocalBundlesDir() {
        String tmpDir = IOUtil.joinDir(getUserDirectory().toString(),
                                       DIR_BUNDLES);
        IOUtil.makeDir(tmpDir);
        return tmpDir;
    }


    /**
     * Return  the full path to the directory where we save the user's local bundles
     *
     * @return Bundle directory
     */
    public File getSavedBundlesDir() {
        String tmpDir = IOUtil.joinDir(getUserDirectory().toString(),
                                       DIR_SAVEDBUNDLES);
        IOUtil.makeDir(tmpDir);
        return new File(tmpDir);
    }


    /**
     *  Return the obejct held in the table identified by the given key.
     * Override the base class method. If the property is not in the
     * main.xml then check the idv properties
     *
     *  @param key The object's key.
     *  @return The Object identified by the given key or null if not found.
     */
    public synchronized Object get(String key) {
        key = StateManager.fixIds(key);
        Object obj = super.get(key);
        if (obj == null) {
            obj = idv.getStateManager().getProperty(key);
        }
        return obj;
    }




}

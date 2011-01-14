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
import org.w3c.dom.Node;


import ucar.unidata.data.DataGroup;
import ucar.unidata.data.DerivedDataDescriptor;

import ucar.unidata.geoloc.*;
import ucar.unidata.idv.IdvResourceManager;


import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.DisplaySetting;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.ImageGenerator;
import ucar.unidata.idv.ui.LoadBundleDialog;
import ucar.unidata.idv.ui.ParamInfo;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.ui.RovingProgress;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.ui.symbol.StationModel;


import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.MenuUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.PluginClassLoader;
import ucar.unidata.util.Prototypable;
import ucar.unidata.util.PrototypeManager;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;

import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.util.WrapperException;

import ucar.unidata.xml.*;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.security.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.Vector;

import java.util.jar.*;


import java.util.regex.*;

import javax.swing.*;



/**
 *
 */

public class PluginManager extends IdvManager {

    /** for url plugins */
    public static final String PLUGIN_PROTOCOL = "idvresource";

    /** xml tag */
    public static final String TAG_PLUGIN = "plugin";

    /** Xml attr */
    public static final String ATTR_NAME = "name";

    /** Xml attr */
    public static final String ATTR_SIZE = "size";

    /** Xml attr */
    public static final String ATTR_VERSION = "version";

    /** Xml attr */
    public static final String ATTR_CATEGORY = "category";

    /** Xml attr name */
    public static final String ATTR_DESC = "description";

    /** Xml attr name */
    public static final String ATTR_URL = "url";

    /** How many extension jars have we opened */
    private int jarCnt = 0;

    /** Holds the plugin editor */
    private JFrame pluginWindow;

    /** the create plugin dialog */
    private JFrame createWindow;

    /** the create plugin file chooser */
    private JFileChooser createFileChooser;

    /** List of the exclde check boxes */
    private List rbiCheckBoxes;

    /** create plugin jlist */
    private JList createList;

    /** Merge plugins */
    private JCheckBox mergeCbx = new JCheckBox("Merge Plugin", true);

    /** list of files in the create plugin list */
    private Vector createFileList = new Vector();

    /** path to plugin jar */
    private JTextField jarFileFld;

    /** Install the plugin cbx */
    private JCheckBox autoInstallCbx;


    /** For the properties list */
    private List propertyInfos;


    /** For the properties list */
    private Hashtable propertyDescriptions;

    /** For the properties list */
    private Hashtable propertyLabels;

    /** For the properties list */
    private JComponent propertyPanel;


    /** For the properties list */
    Hashtable properties;

    /** Shows the plugin contents */
    private JTextArea pluginText;

    /** Shows the plugin contents */
    private JLabel pluginTextLbl;

    /** Lists available plugins */
    private JEditorPane availablePluginEditor;

    /** Lists available plugins */
    private JEditorPane loadedPluginEditor;

    /** gui */
    private JScrollPane availablePluginScroller;

    /** List of loaded plugin files */
    private List myPlugins = new ArrayList();

    /** List of plugins that are not local */
    private List otherPlugins = new ArrayList();

    /** Tracks what categories to show */
    private Hashtable categoryToggle = new Hashtable();


    /** List of plugin class loaders */
    private List pluginClassLoaders = new ArrayList();

    /** Where are the local plugins kept */
    private File localPluginDir;

    /** error msgs */
    private List pluginErrorMessages = new ArrayList();

    /** exceptions */
    private List pluginErrorExceptions = new ArrayList();

    /** widget */
    private JComboBox categoryBox;

    /** widget */
    private JTextField nameFld = new JTextField(10);



    /**
     * ctor
     *
     * @param idv the idv
     */
    public PluginManager(IntegratedDataViewer idv) {
        super(idv);
        try {
            String extDir =
                IOUtil.joinDir(getStore().getUserDirectory().toString(),
                               "plugins");
            IOUtil.makeDir(extDir);


            List installPlugins = getArgsManager().installPlugins;
            for (int i = 0; i < installPlugins.size(); i++) {
                String plugin = (String) installPlugins.get(i);
                installPlugin(plugin, false);
            }

            if (getArgsManager().pluginsOk) {
                loadPlugins();
            }
        } catch (Exception exc) {
            logException("Loading plugins", exc);
        }


    }

    /**
     * View the plugin file. The jar file is args[0]. The jarEntry is args[1]
     *
     * @param args The args
     */
    public void viewPluginFile(Object[] args) {
        try {
            JarFile     jarFile  = (JarFile) args[0];
            JarEntry    jarEntry = (JarEntry) args[1];
            InputStream is       = jarFile.getInputStream(jarEntry);
            byte[]      bytes    = IOUtil.readBytes(is);
            is.close();
            if (args.length == 2) {
                String s = "";
                if (jarEntry.getName().endsWith(".class")) {
                    s = "Binary file";
                } else {
                    s = new String(bytes);
                }
                pluginTextLbl.setText(jarEntry.getName());
                pluginText.setText(s);
                pluginText.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
                pluginText.setCaretPosition(0);
            } else {
                String toFile = FileManager.getWriteFile(
                                    IOUtil.getFileTail(jarEntry.getName()));
                if (toFile == null) {
                    return;
                }
                IOUtil.writeBytes(new File(toFile), bytes);
            }
        } catch (Exception exc) {
            LogUtil.logException("Error viewing plugin file", exc);
        }
    }


    /**
     * List the contents of the plugin  file
     *
     * @param file The plugin file
     */
    public void listPlugin(String file) {
        file = decode(file);

        if ( !file.endsWith(".jar")) {
            LogUtil.userMessage("File: " + file
                                + " is a single file. Not a jar file");
            return;
        }

        try {
            JarFile jarFile = new JarFile(file);
            List    entries = Misc.toList(jarFile.entries());
            List    comps   = new ArrayList();
            for (int i = 0; i < entries.size(); i++) {
                JarEntry entry = (JarEntry) entries.get(i);
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.toLowerCase().endsWith("manifest.mf")) {
                    continue;
                }
                JLabel label = new JLabel(name);
                JComponent viewBtn = GuiUtils.makeImageButton(
                                         "/auxdata/ui/icons/FindAgain16.gif",
                                         this, "viewPluginFile",
                                         new Object[] { jarFile,
                        entry });
                viewBtn.setToolTipText("View this file");
                JComponent exportBtn =
                    GuiUtils.makeImageButton("/auxdata/ui/icons/Save16.gif",
                                             this, "viewPluginFile",
                                             new Object[] { jarFile,
                        entry, new Boolean(true) });
                exportBtn.setToolTipText("Export this file");

                Insets btnInsets = new Insets(1, 1, 1, 5);
                JComponent rowComp = GuiUtils.doLayout(new Component[] {
                                         GuiUtils.inset(exportBtn, btnInsets),
                                         GuiUtils.inset(viewBtn, btnInsets),
                                         label }, 3, GuiUtils.WT_NNY,
                                             GuiUtils.WT_N);
                rowComp = GuiUtils.inset(rowComp, new Insets(2, 0, 2, 0));
                comps.add(rowComp);
            }

            JComponent panel = GuiUtils.vbox(comps);
            panel = GuiUtils.inset(GuiUtils.top(panel), 5);
            JScrollPane listScroll = GuiUtils.makeScrollPane(panel, 500, 250);
            listScroll.setPreferredSize(new Dimension(650, 250));

            pluginTextLbl = new JLabel(" ");
            pluginText    = new JTextArea(10, 10);
            pluginText.setEditable(false);
            JScrollPane textScroll = GuiUtils.makeScrollPane(pluginText, 500,
                                         250);
            textScroll.setPreferredSize(new Dimension(500, 250));


            JComponent contents = GuiUtils.vsplit(
                                      listScroll,
                                      GuiUtils.topCenter(
                                          GuiUtils.inset(pluginTextLbl, 5),
                                          textScroll), 0.5);


            GuiUtils.makeDialog(null, "Plugin List", contents, null,
                                new String[] { GuiUtils.CMD_OK });
        } catch (Exception exc) {
            LogUtil.logException("Error listing plugin:" + file, exc);
        }
    }


    /**
     * Ask the user to seelct a jar file. Set the jarFileFld textfield
     *
     * @return The jar file selected
     */
    public String selectJarFile() {
        String jarFile = FileManager.getWriteFile(FileManager.FILTER_JAR,
                             "jar");
        if (jarFile == null) {
            return null;
        }
        jarFileFld.setText(jarFile);
        return jarFile;
    }



    /**
     * Utility to create a unique temp file name from a base filename
     *
     * @param filename The base name
     *
     * @return The full path to the tmp file
     */
    private String getTmpFile(String filename) {
        int    cnt     = 1;
        String tmpFile = getIdv().getObjectStore().getTmpFile(filename);
        while (new File(tmpFile).exists()) {
            tmpFile = getIdv().getObjectStore().getTmpFile(cnt + "_"
                    + filename);
            cnt++;
        }
        return tmpFile;
    }

    /**
     * Add the given text with the given filename. Write out the file
     * to a tmp directory.
     *
     * @param text Text to write
     * @param filename Filename to use
     */
    public void addText(String text, String filename) {
        try {
            String tmpFile = getTmpFile(filename);
            IOUtil.writeFile(tmpFile, text);
            addObject(tmpFile);
        } catch (Exception exc) {
            logException("Error writing text plugin file", exc);
        }
    }



    /**
     * Add the list of objects
     *
     * @param objects Objects
     */
    public void addObjects(List objects) {
        showCreatePlugin();
        for (int i = 0; i < objects.size(); i++) {
            Wrapper wrapper = new Wrapper(objects.get(i));
            if ( !createFileList.contains(wrapper)) {
                createFileList.add(wrapper);
            }
        }
        createList.setListData(createFileList);
    }


    /**
     * Add some object to the list
     *
     * @param obj The object to add
     */
    public void addObject(Object obj) {
        addObjects(Misc.newList(obj));
    }


    /**
     * Is the given object of the type of the given class. If the obj is
     * a list checks its first entry
     *
     * @param obj The object or a list of objects
     * @param c The class
     *
     * @return Is obj or list element of this type
     */
    private boolean isObject(Object obj, Class c) {
        //        if (obj.getClass().isAssignableFrom(c)) {
        //            return true;
        //        }

        if (c.isAssignableFrom(obj.getClass())) {
            return true;
        }
        if (obj instanceof List) {
            List l = (List) obj;
            if (l.size() > 0) {
                return isObject(l.get(0), c);
            }
        }
        return false;
    }


    /**
     * Write the plugin
     */
    public void createPlugin() {

        if (createWindow == null) {
            showCreatePlugin();
            return;
        }


        String jarFile = jarFileFld.getText().trim();
        if (jarFile.length() == 0) {
            jarFile = selectJarFile();
            if (jarFile == null) {
                return;
            }
        } else {
            // make sure there is a .jar on the end of it.
            String tail = IOUtil.getFileTail(jarFile);
            if (tail.indexOf(".") < 0) {
                jarFile = jarFile + ".jar";
            }

        }

        try {
            List         bundles = new ArrayList();
            List         files   = new ArrayList();

            List resources       = getResourceManager().getResourcesForUser();
            StringBuffer rbiSB   = null;
            for (int i = 0; i < resources.size(); i++) {
                JCheckBox cbx = (JCheckBox) rbiCheckBoxes.get(i);
                if ( !cbx.isSelected()) {
                    continue;
                }
                if (rbiSB == null) {
                    rbiSB = new StringBuffer();
                    rbiSB.append(XmlUtil.XML_HEADER);
                    rbiSB.append("<" + IdvResourceManager.TAG_RESOURCEBUNDLE
                                 + ">\n");
                }
                IdvResourceManager.IdvResource resource =
                    (IdvResourceManager.IdvResource) resources.get(i);
                rbiSB.append(
                    XmlUtil.tag(
                        IdvResourceManager.TAG_RESOURCES,
                        XmlUtil.attrs(
                            IdvResourceManager.ATTR_NAME, resource.getId(),
                            IdvResourceManager.ATTR_REMOVEPREVIOUS, "true")));
                rbiSB.append("\n");
            }
            if (rbiSB != null) {
                rbiSB.append("</" + IdvResourceManager.TAG_RESOURCEBUNDLE
                             + ">\n");
                String tmpFile = getTmpFile("idv.rbi");
                IOUtil.writeFile(tmpFile, rbiSB.toString());
                files.add(tmpFile);
            }


            StringBuffer propertiesSB = null;
            for (int i = 0; i < propertyInfos.size(); i++) {
                PropertyInfo pi = (PropertyInfo) propertyInfos.get(i);
                String       newValue;
                if (pi.widget instanceof JTextField) {
                    JTextField fld = (JTextField) pi.widget;
                    newValue = fld.getText().trim();
                } else if (pi.widget instanceof JTextArea) {
                    JTextArea fld       = (JTextArea) pi.widget;
                    String    delimiter = pi.delimiter;
                    if (delimiter == null) {
                        delimiter = ";";
                    } else {
                        delimiter = delimiter.trim();
                    }
                    newValue = StringUtil.join(delimiter,
                            StringUtil.split(fld.getText().trim(), "\n",
                                             true, true));
                } else {
                    JCheckBox box = (JCheckBox) pi.widget;
                    if (box.isSelected()) {
                        newValue = "true";
                    } else {
                        newValue = "false";
                    }
                }

                if ( !pi.value.trim().equals(newValue.trim())) {
                    if (propertiesSB == null) {
                        propertiesSB = new StringBuffer();
                    }
                    propertiesSB.append(pi.key);
                    propertiesSB.append(" = ");
                    propertiesSB.append(newValue);
                    propertiesSB.append("\n");
                }
            }
            if (propertiesSB != null) {
                String tmpFile = getTmpFile("idv.properties");
                IOUtil.writeFile(tmpFile, propertiesSB.toString());
                files.add(tmpFile);
            }





            Hashtable objects = new Hashtable();
            for (int i = 0; i < createFileList.size(); i++) {
                Wrapper wrapper = (Wrapper) createFileList.get(i);
                Object  obj     = wrapper.obj;
                if (obj instanceof String) {
                    files.add(obj);
                } else if (isObject(obj, DerivedDataDescriptor.class)) {
                    add(objects, obj, "derived.xml");
                } else if (isObject(obj, ParamInfo.class)) {
                    add(objects, obj, "paramdefaults.xml");
                } else if (isObject(obj, DisplaySetting.class)) {
                    add(objects, obj, "displaysettings.xml");
                } else if (isObject(obj, DataGroup.class)) {
                    add(objects, obj, "paramgroups.xml");
                } else if (isObject(obj, ColorTable.class)) {
                    add(objects, obj, "colortables.xml");
                    //                } else if (isObject(obj, Projection.class)) {
                    //                    add(objects, obj, "projections.xml");
                } else if (isObject(obj, ProjectionImpl.class)) {
                    add(objects, obj, "projections.xml");
                } else if (isObject(obj, StationModel.class)) {
                    add(objects, obj, "stationmodels.xml");
                } else if (isObject(obj, SavedBundle.class)) {
                    if (obj instanceof SavedBundle) {
                        bundles.add(obj);
                    } else {
                        bundles.addAll((List) obj);
                    }
                } else {
                    System.err.println("Unknown object type:"
                                       + obj.getClass().getName());
                }
            }
            if (bundles.size() > 0) {

                Hashtable seen = new Hashtable();
                for (int i = 0; i < bundles.size(); i++) {
                    SavedBundle savedBundle = (SavedBundle) bundles.get(i);
                    String      urlOrFile   = savedBundle.getUrl();
                    //See if this is a URL. If it isn't then write the file
                    try {
                        new URL(urlOrFile);
                    } catch (MalformedURLException mue) {
                        String path = savedBundle.getCategorizedName();
                        int    cnt  = 0;
                        //Make sure this file is unique
                        while (seen.get(path) != null) {
                            savedBundle.setUniquePrefix("v" + (cnt++) + "_");
                            path = savedBundle.getCategorizedName();
                        }
                        seen.put(path, path);
                        files.add(new TwoFacedObject(path, urlOrFile));
                    }
                }


                String bundlesXml =
                    IdvPersistenceManager.getBundleXml(bundles, true);
                String uid         = "bundles.xml";
                String bundlesFile =
                    getIdv().getObjectStore().getTmpFile(uid);
                IOUtil.writeFile(bundlesFile, bundlesXml);
                files.add(bundlesFile);
                for (int i = 0; i < bundles.size(); i++) {
                    SavedBundle savedBundle = (SavedBundle) bundles.get(i);
                    savedBundle.setUniquePrefix(null);
                }
            }


            Enumeration keys = objects.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                addEncodedObject(files, objects.get(key), key);
            }

            if (files.size() == 0) {
                LogUtil.userMessage("No files have been selected");
                //            addCreateFile();
                return;
            }

            //            System.err.println("files:" + files);
            IOUtil.writeJarFile(jarFile, files, null, true);
            if (autoInstallCbx.isSelected()) {
                installPlugin(jarFile, true);
                updatePlugins();
            }
            LogUtil.userMessage("Plugin file has been written");
        } catch (Exception exc) {
            logException("Error writing plugin file:" + jarFile, exc);
        }

    }


    /**
     *  Add the object (or the list of objects if its a list) to the
     * list element in the objects map with key filename
     *
     * @param objects Map of filename to list of objects
     * @param object The object or list of objects
     * @param filename key
     */
    private void add(Hashtable objects, Object object, String filename) {
        List l = (List) objects.get(filename);
        if (l == null) {
            l = new ArrayList();
            objects.put(filename, l);
        }
        if (object instanceof List) {
            l.addAll((List) object);
        } else {
            l.add(object);
        }
    }

    /**
     * Write the xml encoded version of the object to the filename
     * and add the file to the files list
     *
     * @param files list of files
     * @param object object to write
     * @param filename file to write to
     *
     * @throws Exception On badness
     */
    private void addEncodedObject(List files, Object object, String filename)
            throws Exception {
        String tmpFile = getIdv().getObjectStore().getTmpFile(filename);
        String xml     = getIdv().encodeObject(object, true);
        IOUtil.writeFile(tmpFile, xml);
        files.add(tmpFile);
    }



    /**
     * Close the dialog
     */
    public void closeCreatePlugin() {
        if (createWindow != null) {
            createWindow.setVisible(false);
        }
    }


    /**
     * Apply preferences
     */
    public void applyPreferences() {
        if (createFileChooser != null) {
            createFileChooser.setFileHidingEnabled(
                FileManager.getFileHidingEnabled());
        }
    }


    /**
     * Let user select a file to put into the plugin
     */
    public void addCreateFile() {
        if (createFileChooser == null) {
            createFileChooser = new JFileChooser();
            createFileChooser.setCurrentDirectory(
                new File(getResourceManager().getUserPath()));
            createFileChooser.setMultiSelectionEnabled(true);
            applyPreferences();
        }
        if (createFileChooser.showOpenDialog(null)
                == JFileChooser.APPROVE_OPTION) {
            File[] files = createFileChooser.getSelectedFiles();
            for (int i = 0; i < files.length; i++) {
                addCreateFile(files[i].toString());
            }
        }
    }

    /**
     * Add the file to the list of plugin files
     *
     * @param file The file
     */
    public void addCreateFile(String file) {
        addCreateFile(file, null);
    }

    /**
     * Get the bundle component
     *
     * @param name  name of the component
     *
     * @return  the component or null
     */
    private JComponent getBundleComponent(String name) {
        if (categoryBox == null) {
            categoryBox = getPersistenceManager().makeCategoryBox();
        }

        Object selected = categoryBox.getSelectedItem();
        GuiUtils.setListData(
            categoryBox, getPersistenceManager().getFavoritesCategories());
        if (selected != null) {
            categoryBox.setSelectedItem(selected);
        }
        if (name != null) {
            nameFld.setText(name);
        }
        return GuiUtils.top(GuiUtils.vbox(new JLabel("Name:"), nameFld,
                                          new JLabel("Category:"),
                                          categoryBox));

    }

    /**
     * add a file name with the given label to show
     *
     * @param file file
     * @param label label
     */
    private void addCreateFile(String file, String label) {
        if (getArgsManager().isBundleFile(file)) {
            String name = IOUtil.getFileTail(IOUtil.stripExtension(file));
            if ( !GuiUtils.showOkCancelDialog(null,
                    "Favorite Bundle Category",
                    GuiUtils.inset(getBundleComponent(name), 10), null)) {
                return;
            }

            makeSavedBundle(file);
            return;
        }


        Wrapper wrapper = new Wrapper(file, label);
        showCreatePlugin();
        if ( !createFileList.contains(wrapper)) {
            createFileList.add(wrapper);
            createList.setListData(createFileList);
            createList.setSelectedIndex(createFileList.size() - 1);
        }
    }


    /**
     * Remove the selected entry(ies) in the list of resources
     */
    public void removeCreateFile() {
        showCreatePlugin();
        Vector newList   = new Vector(createFileList);
        int[]  indices   = createList.getSelectedIndices();
        int    lastIndex = 0;
        for (int i = 0; i < indices.length; i++) {
            lastIndex = indices[i];
            Object o = createFileList.get(indices[i]);
            newList.remove(o);
        }
        createFileList = newList;
        createList.setListData(createFileList);
        if (createFileList.size() > 0) {
            while (lastIndex >= createFileList.size()) {
                lastIndex--;
            }
            if (lastIndex >= 0) {
                createList.setSelectedIndex(lastIndex);
            }
        }
    }


    /**
     * Add entries to menu
     *
     * @param menu the menu
     */
    public void initializeColorTableMenu(JMenu menu) {
        List           items    = new ArrayList();
        ObjectListener listener = new ObjectListener(this) {
            public void actionPerformed(ActionEvent ae, Object object) {
                addObject(object);
            }
        };
        getIdv().getColorTableManager().makeColorTableMenu(listener, items);
        GuiUtils.makeMenu(menu, items);
    }

    /**
     * Add entries to menu
     *
     * @param menu the menu
     */
    public void initializeStationModelsMenu(JMenu menu) {
        initializeMenu(menu,
                       getIdv().getStationModelManager().getStationModels(),
                       "Layout Models");
    }

    /**
     * Add entries to menu
     *
     * @param menu the menu
     */
    public void initializeFavoritesMenu(JMenu menu) {
        List favorites = getPersistenceManager().getBundles(
                             IdvPersistenceManager.BUNDLES_FAVORITES);
        List displays = getPersistenceManager().getBundles(
                            IdvPersistenceManager.BUNDLES_DISPLAY);
        List data = getPersistenceManager().getBundles(
                        IdvPersistenceManager.BUNDLES_DATA);
        boolean multiples = (favorites.size() > 0) && (displays.size() > 0);
        multiples |= (favorites.size() > 0) && (data.size() > 0);
        multiples |= (displays.size() > 0) && (data.size() > 0);
        JMenu theMenu = menu;
        if (favorites.size() > 0) {
            if (multiples) {
                menu.add(theMenu = new JMenu("Bundles"));
            }
            initializeMenu(theMenu, favorites, "Favorites");
        }
        theMenu = menu;
        if (displays.size() > 0) {
            if (multiples) {
                menu.add(theMenu = new JMenu("Displays"));
            }
            initializeMenu(theMenu, displays, "Display Favorites");
        }
        theMenu = menu;
        if (data.size() > 0) {
            if (multiples) {
                menu.add(theMenu = new JMenu("Data"));
            }
            initializeMenu(theMenu, data, "Data Favorites");
        }
        GuiUtils.limitMenuSize(menu, "Favorites", 20);
    }

    /**
     * Add entries to menu
     *
     * @param menu the menu
     */
    public void initializeFormulasMenu(JMenu menu) {
        initializeMenu(menu, getIdv().getJythonManager().getDescriptors(),
                       "Formulas");
    }



    /**
     * Load bundles from disk
     */
    public void loadBundlesFromDisk() {
        String file = FileManager.getReadFileOrURL(
                          "Bundle to load into plugin",
                          getArgsManager().getBundleFileFilters(),
                          getBundleComponent(null));
        if (file == null) {
            return;
        }
        file = file.trim();
        if (file.length() == 0) {
            return;
        }

        makeSavedBundle(file);

    }

    /**
     * Make a saved bundle
     *
     * @param file the file name
     */
    private void makeSavedBundle(String file) {
        String name = nameFld.getText().trim();
        if (name.length() == 0) {
            name = IOUtil.stripExtension(IOUtil.getFileTail(file));
        }
        Object cat = categoryBox.getSelectedItem();
        if (cat == null) {
            cat = "";
        }
        SavedBundle savedBundle =
            new SavedBundle(
                file, name,
                IdvPersistenceManager.stringToCategories(cat.toString()));
        addObject(savedBundle);
    }




    /**
     * Add entries to menu
     *
     * @param menu the menu
     */
    public void initializeParamDefaultsMenu(JMenu menu) {
        initializeMenu(menu,
                       getIdv().getParamDefaultsEditor().getParamInfos(true),
                       "Param Defaults");
    }




    /**
     * Add entries to menu
     *
     * @param menu the menu
     */
    public void initializeProjectionsMenu(JMenu menu) {
        List projections =
            getIdv().getIdvProjectionManager().getProjections();
        MapViewManager.makeProjectionsMenu(menu, projections, this,
                                           "addObject");
    }




    /**
     * Add entries to menu
     *
     * @param menu the menu
     * @param list List of entries
     * @param name name of the sub menu
     */
    public void initializeMenu(JMenu menu, List list, String name) {
        List items = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            String label;
            if (obj instanceof SavedBundle) {
                label = ((SavedBundle) obj).getLabel();
            } else {
                label = obj.toString();
            }
            items.add(GuiUtils.makeMenuItem(label, this, "addObject", obj));
        }

        MenuUtil.makeMenu(menu, items);
        GuiUtils.limitMenuSize(menu, name, 20);
    }

    /**
     * Add main.xml
     */
    public void addPreferences() {
        addCreateFile(
            getResourceManager().getResourcePath(
                getStateManager().getPreferencePaths().get(0).toString()));
    }



    /**
     * Show the create dialog
     */
    public void showCreatePlugin() {

        if (createWindow == null) {
            autoInstallCbx = new JCheckBox("Install", false);
            autoInstallCbx.setToolTipText(
                "Automatically install the plugin when it is created");
            createWindow =
                GuiUtils.createFrame(GuiUtils.getApplicationTitle()
                                     + "Plugin Creator");
            createList = new JList();
            createList.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (GuiUtils.isDeleteEvent(e)) {
                        removeCreateFile();
                    }
                }
            });
            jarFileFld = new JTextField("", 30);
            JButton deleteBtn = GuiUtils.makeImageButton(
                                    "/auxdata/ui/icons/plugin_delete.png",
                                    this, "removeCreateFile");
            JScrollPane listScroll = GuiUtils.makeScrollPane(createList, 150,
                                         250);
            JButton writeBtn = GuiUtils.makeButton("Write Plugin: ", this,
                                   "createPlugin");

            JButton browseBtn = GuiUtils.makeButton("Browse", this,
                                    "selectJarFile");
            JComponent fileComp = GuiUtils.leftCenterRight(
                                      GuiUtils.inset(
                                          GuiUtils.hbox(
                                              autoInstallCbx, writeBtn,
                                              5), 2), GuiUtils.hfill(
                                                  jarFileFld), GuiUtils.inset(
                                                  browseBtn, 2));

            GuiUtils.tmpInsets = GuiUtils.INSETS_2;
            JComponent center = GuiUtils.doLayout(new Component[] {
                                    GuiUtils.top(deleteBtn),
                                    listScroll }, 2, GuiUtils.WT_NY,
                                        GuiUtils.WT_Y);



            List resources = getResourceManager().getResourcesForUser();
            rbiCheckBoxes = new ArrayList();
            for (int i = 0; i < resources.size(); i++) {
                IdvResourceManager.IdvResource resource =
                    (IdvResourceManager.IdvResource) resources.get(i);
                rbiCheckBoxes.add(new JCheckBox(resource.getDescription(),
                        false));
            }

            properties = getIdv().getStateManager().getProperties();


            List propertyKeys = new ArrayList();
            propertyDescriptions = new Hashtable();
            propertyLabels       = new Hashtable();
            Enumeration keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                if ( !key.startsWith("idv.")) {
                    continue;
                }


                if (key.endsWith(".propignore")) {
                    continue;
                }
                if (properties.get(key + ".propignore") != null) {
                    continue;
                }

                if (key.endsWith(".propcategory")) {
                    continue;
                }

                if (key.endsWith(".propdelimiter")) {
                    continue;
                }

                if (key.endsWith(".propwidth")) {
                    continue;
                }

                if (key.endsWith(".proplabel")) {
                    propertyLabels.put(key, properties.get(key));
                    continue;
                }
                if (key.startsWith("idv.loadingxml")) {
                    continue;
                }
                if (key.endsWith(".propdesc")) {
                    propertyDescriptions.put(key, properties.get(key));
                    continue;
                }
                propertyKeys.add(key);
            }
            propertyKeys  = Misc.sort(propertyKeys);




            propertyInfos = new ArrayList();
            final Rectangle scrollRect = new Rectangle(1, 1, 1, 1);
            List            cats       = new ArrayList();
            List            catNames   = new ArrayList();
            List            catLists   = new ArrayList();
            cats.add("idv.ui");
            catNames.add("User Interface");
            catLists.add(new ArrayList());

            cats.add("idv.data");
            catNames.add("Data");
            catLists.add(new ArrayList());

            cats.add("idv.misc");
            catNames.add("Miscellaneous");
            catLists.add(new ArrayList());


            Insets labelInset = new Insets(0, 10, 0, 0);

            for (int i = 0; i < propertyKeys.size(); i++) {
                PropertyInfo pi = new PropertyInfo();
                propertyInfos.add(pi);
                pi.key   = (String) propertyKeys.get(i);
                pi.value = (String) properties.get(pi.key);
                String label = (String) propertyLabels.get(pi.key
                                   + ".proplabel");
                String desc = (String) propertyDescriptions.get(pi.key
                                  + ".propdesc");
                pi.delimiter = (String) properties.get(pi.key
                        + ".propdelimiter");
                String labelToolTip = null;
                if (label == null) {
                    label = pi.key;
                } else {
                    labelToolTip = pi.key;
                }
                JComponent propLabel = new JLabel(label);
                if (labelToolTip != null) {
                    propLabel.setToolTipText(labelToolTip);
                }

                String text = pi.value.trim();

                if (text.equals("true") || text.equals("false")) {
                    pi.widget = new JCheckBox("", text.equals("true"));
                } else if (pi.delimiter != null) {
                    pi.delimiter = pi.delimiter.trim();
                    pi.widget = new JTextArea(StringUtil.join("\n",
                            StringUtil.split(text, pi.delimiter, true,
                                             true)), 4, 20);
                    pi.value = StringUtil.join(
                        pi.delimiter,
                        StringUtil.split(
                            ((JTextArea) pi.widget).getText(), "\n", true,
                            true));
                    if (desc != null) {
                        pi.outerWidget = GuiUtils.vbox(new JLabel(desc),
                                new JScrollPane(pi.widget));
                    } else {
                        pi.outerWidget = new JScrollPane(pi.widget);
                    }
                    pi.outerWidget = GuiUtils.inset(pi.outerWidget,
                            new Insets(2, 0, 2, 0));
                    propLabel = GuiUtils.top(propLabel);
                } else {
                    String width = (String) properties.get(pi.key
                                       + ".propwidth");
                    if (width != null) {
                        pi.widget = new JTextField(text,
                                new Integer(width.trim()).intValue());
                        ((JTextField) pi.widget).setCaretPosition(0);
                        pi.outerWidget = GuiUtils.left(pi.widget);
                    } else {
                        pi.widget = new JTextField(text, 30);
                        ((JTextField) pi.widget).setCaretPosition(0);
                    }
                }
                if (desc != null) {
                    pi.widget.setToolTipText(desc);
                }
                List comps = null;
                for (int catIdx = 0; catIdx < cats.size(); catIdx++) {
                    if (pi.key.startsWith((String) cats.get(catIdx))) {
                        comps = (List) catLists.get(catIdx);
                        break;

                    }
                }
                if (comps == null) {
                    comps = (List) catLists.get(catLists.size() - 1);
                }
                comps.add(GuiUtils.inset(propLabel, labelInset));
                comps.add(pi.getComponentToDisplay());
            }
            List compsToDisplay = new ArrayList();
            for (int catIdx = 0; catIdx < cats.size(); catIdx++) {
                String name = (String) catNames.get(catIdx);
                JLabel catLabel =
                    new JLabel("<html><h2 style=\"margin-bottom:2pt;\">"
                               + name + "</h2></html>");
                compsToDisplay.add(catLabel);
                compsToDisplay.add(GuiUtils.filler());
                compsToDisplay.addAll((List) catLists.get(catIdx));
            }

            propertyPanel = GuiUtils.doLayout(compsToDisplay, 2,
                    GuiUtils.WT_NY, GuiUtils.WT_N);

            //Don't process the messages here
            Msg.SkipPanel skipPanel = new Msg.SkipPanel(new BorderLayout());
            skipPanel.add(BorderLayout.CENTER, propertyPanel);
            propertyPanel = skipPanel;

            propertyPanel =
                GuiUtils.makeScrollPane(GuiUtils.inset(propertyPanel, 5),
                                        200, 200);
            propertyPanel.setPreferredSize(new Dimension(150, 200));
            propertyPanel.setSize(150, 200);

            JComponent rbiPanel = GuiUtils.vbox(rbiCheckBoxes);
            rbiPanel = GuiUtils.makeScrollPane(rbiPanel, 200, 300);
            rbiPanel.setSize(new Dimension(150, 250));
            rbiPanel = GuiUtils.topCenter(
                new JLabel("Select the system resources to exclude"),
                GuiUtils.top(rbiPanel));

            JTabbedPane tab = new JTabbedPane();
            tab.add("Resources", center);
            tab.add("Excludes", GuiUtils.inset(rbiPanel, 5));
            tab.add("Properties", GuiUtils.inset(propertyPanel, 5));
            center = tab;
            center = GuiUtils.centerBottom(center,
                                           GuiUtils.inset(fileComp, 5));

            JMenuBar menuBar  = new JMenuBar();
            JMenu    fileMenu = new JMenu("File");
            fileMenu.add(GuiUtils.makeMenuItem("Add File", this,
                    "addCreateFile"));
            fileMenu.add(GuiUtils.makeMenuItem("Add Preferences", this,
                    "addPreferences"));
            JMenu resourceMenu = new JMenu("Add Resource");

            resourceMenu.add(GuiUtils.makeDynamicMenu("Favorites", this,
                    "initializeFavoritesMenu"));
            resourceMenu.add(GuiUtils.makeMenuItem("Bundle from disk", this,
                    "loadBundlesFromDisk"));

            resourceMenu.add(GuiUtils.makeDynamicMenu("Color Tables", this,
                    "initializeColorTableMenu"));
            resourceMenu.add(GuiUtils.makeDynamicMenu("Layout Models", this,
                    "initializeStationModelsMenu"));
            resourceMenu.add(GuiUtils.makeDynamicMenu("Projections", this,
                    "initializeProjectionsMenu"));
            resourceMenu.add(GuiUtils.makeDynamicMenu("Formulas", this,
                    "initializeFormulasMenu"));
            resourceMenu.add(GuiUtils.makeDynamicMenu("Parameter Defaults",
                    this, "initializeParamDefaultsMenu"));

            fileMenu.add(resourceMenu);
            fileMenu.addSeparator();
            fileMenu.add(GuiUtils.makeMenuItem("Import Plugin", this,
                    "importPlugin"));
            fileMenu.addSeparator();
            fileMenu.add(GuiUtils.makeMenuItem("Close", this,
                    "closeCreatePlugin"));
            JMenu helpMenu = new JMenu("Help");
            helpMenu.add(GuiUtils.makeMenuItem("Plugin Creator", this,
                    "showCreatorHelp"));
            menuBar.add(fileMenu);
            menuBar.add(helpMenu);

            JPanel bottom =
                GuiUtils.inset(GuiUtils.wrap(GuiUtils.makeButton("Close",
                    this, "closeCreatePlugin")), 2);
            JComponent contents = GuiUtils.topCenterBottom(menuBar, center,
                                      bottom);
            createWindow.getContentPane().add(contents);
            GuiUtils.decorateFrame(createWindow, menuBar);
            createWindow.pack();
            ucar.unidata.util.Msg.translateTree(createWindow);
            createWindow.setLocation(100, 100);
        }
        GuiUtils.toFront(createWindow);

    }


    /**
     * Show help
     */
    public void showCreatorHelp() {
        getIdv().getIdvUIManager().showHelp("idv.misc.plugincreator");
    }

    /**
     * Show help
     */
    public void showManagerHelp() {
        getIdv().getIdvUIManager().showHelp("idv.misc.plugins");
    }


    /**
     * Class PropertyInfo is used for the properties list
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.54 $
     */
    private static class PropertyInfo {

        /** attribute */
        String delimiter;

        /** attribute */
        String key;

        /** attribute */
        String value;

        /** attribute */
        String label;

        /** attribute */
        JComponent widget;

        /** attribute */
        JComponent outerWidget;

        /**
         * ctor
         */
        public PropertyInfo() {}



        /**
         * ctor
         *
         * @param key prop key
         * @param value prop value
         * @param widget The widget
         */
        public PropertyInfo(String key, String value, JComponent widget) {
            this.key   = key;
            this.value = value;
            //            this.label = label;
            this.widget = widget;
        }

        /**
         * Get the gui component
         *
         * @return gui component
         */
        public JComponent getComponentToDisplay() {
            if (outerWidget != null) {
                return outerWidget;
            }
            return widget;
        }


    }



    /**
     * show dialog
     */
    public void showPluginList() {
        updatePlugins();
    }


    /**
     * This gets called on System.exit and closes any open plugin jar files
     */
    protected void closeResources() {
        for (int i = 0; i < pluginClassLoaders.size(); i++) {
            ((PluginClassLoader) pluginClassLoaders.get(i)).closeJar();
        }
    }



    /**
     * Load in any plugins
     *
     * @throws Exception On badness_
     */
    protected void loadPlugins() throws Exception {
        ResourceCollection rc = getResourceManager().getResources(
                                    getResourceManager().RSC_PLUGINS);
        List plugins = new ArrayList(getArgsManager().plugins);
        for (int resourceIdx = 0; resourceIdx < rc.size(); resourceIdx++) {
            String path = rc.get(resourceIdx).toString();
            if ((localPluginDir == null) && rc.isWritable(resourceIdx)) {
                localPluginDir = new File(path);
            }
            plugins.add(path);
        }


        for (int resourceIdx = 0; resourceIdx < plugins.size();
                resourceIdx++) {
            String path = plugins.get(resourceIdx).toString();

            handlePlugin(path);
        }
    }

    /**
     * add an error
     *
     * @param message msg
     * @param exc exception
     */
    protected void addError(String message, Throwable exc) {
        pluginErrorExceptions.add(exc);
        pluginErrorMessages.add(message);
    }

    /**
     * Handle the extension file. It may be a jar, index or resource file
     *
     * @param path file path or url
     *
     * @throws Exception On badness
     */
    public void handlePlugin(String path) throws Exception {

        if ( !getArgsManager().pluginsOk) {
            return;
        }
        pluginErrorMessages   = new ArrayList();
        pluginErrorExceptions = new ArrayList();

        File f = new File(path);
        if (path.endsWith(".jar")) {
            loadJar(path);
            checkForErrors(path);
        } else if (path.endsWith("index.xml")) {}
        else if (f.exists() && f.isDirectory()) {
            scourPlugins(f);
            File[] files = f.listFiles();
            files = IOUtil.sortFilesOnAge(files, false);
            for (int jarFileIdx = 0; jarFileIdx < files.length;
                    jarFileIdx++) {
                if ( !pluginFileOK(files[jarFileIdx])) {
                    continue;
                }
                String filename = files[jarFileIdx].toString();
                //                if (filename.endsWith(".jar")) {
                String decodedFilename = decode(filename);
                if (IOUtil.getFileTail(decodedFilename).endsWith(".jar")) {
                    loadJar(filename);
                } else {
                    loadPlugin(filename, true);
                }
                checkForErrors(filename);
            }
        } else {
            loadPlugin(f.toString(), true);
            checkForErrors(f.toString());
        }
    }


    /**
     * Check for errors
     *
     * @param path plugin file path
     */
    private void checkForErrors(String path) {
        if (pluginErrorMessages.size() > 0) {
            path = IOUtil.getFileTail(path);
            LogUtil.printExceptions(
                "<html>Errors have occurred loading plugin:<br><i>" + path
                + "</i></html>", pluginErrorMessages, pluginErrorExceptions);
            pluginErrorMessages   = new ArrayList();
            pluginErrorExceptions = new ArrayList();
        }


    }


    /**
     * Try to process the given file. This deals with rbis, properties
     * and files with the default name  of resources
     *
     * @param filename The file
     * @param topLevel  Flag that designates that this plugin is one of the main files in the plugins
     * directory.
     *
     * @throws Exception On badness
     */
    protected void loadPlugin(String filename, boolean topLevel)
            throws Exception {
        if ( !getArgsManager().pluginsOk) {
            return;
        }
        loadPlugin(filename, "", topLevel);
    }

    /**
     * Add the plugin to the list of plugins
     *
     * @param file plugin
     */
    private void addPluginToList(String file) {
        if ((localPluginDir != null)
                && localPluginDir.equals(new File(file).getParentFile())) {
            if ( !myPlugins.contains(file)) {
                myPlugins.add(file);
            }
        } else {
            if ( !otherPlugins.contains(file)) {
                otherPlugins.add(file);
            }
        }
    }


    /**
     * remove plugin
     *
     * @param file file
     */
    public void removePlugin(String file) {
        removePlugin(new File(file));
    }


    /**
     * remove plugin
     *
     * @param file file
     */
    public void removePlugin(File file) {
        myPlugins.remove(file.toString());
        //System.err.println ("remove:" + file);
        try {
            String deleteThisFile = file + ".deletethis";
            IOUtil.writeFile(deleteThisFile, "");
        } catch (Exception exc) {
            System.err.println(exc);
        }
        //        file.deleteOnExit();

        Plugin p = (Plugin) Plugin.pathToPlugin.get(decode(file));
        if (p != null) {
            p.delete();
        }
        updatePlugins();
    }


    /**
     * Try to process the given file. This deals with rbis, properties
     * and files with the default name  of resources
     *
     * @param filename The file
     * @param prefix Prefix to prepend on the filename for jar based entries
     * @param topLevel  Flag that designates that this plugin is one of the main files in the plugins
     * directory.
     *
     * @throws Exception On badness
     */
    protected void loadPlugin(String filename, String prefix,
                              boolean topLevel)
            throws Exception {
        loadPlugin(filename, prefix, topLevel, null);
    }


    /**
     * load plugin
     *
     * @param filename plugin file
     * @param prefix prefix
     * @param topLevel top level
     * @param label label
     *
     * @throws Exception on badness
     */
    protected void loadPlugin(String filename, String prefix,
                              boolean topLevel, String label)
            throws Exception {
        //        System.err.println ("load plugin:" + filename);
        if ( !getArgsManager().pluginsOk) {
            return;
        }



        if (getArgsManager().isRbiFile(filename)) {
            if (topLevel) {
                addPluginToList(filename);
            }
            Element root = XmlUtil.getRoot(prefix + filename, getClass());
            if (root != null) {
                getResourceManager().processRbi(root, true);
            }
            return;
        }
        //Check if the file is one of the known files
        String tail      = IOUtil.getFileTail(filename);
        List   resources = getResourceManager().getResources();
        //      System.err.println("file:" + filename);
        for (int i = 0; i < resources.size(); i++) {
            IdvResourceManager.IdvResource idvResource =
                (IdvResourceManager.IdvResource) resources.get(i);
            Pattern pattern = idvResource.getPattern();
            if (pattern == null) {
                continue;
            }
            Matcher matcher = pattern.matcher(filename);
            //            System.err.println ("file:" + filename + " " + idvResource);

            if (matcher.find()) {
                //                System.err.println ("got it");
                if (topLevel) {
                    addPluginToList(filename);
                }
                ResourceCollection rc =
                    getResourceManager().getResources(idvResource);
                String fullPath = prefix + filename;

                if ( !rc.contains(filename) && !rc.contains(fullPath)
                        && !rc.contains("/" + filename)) {
                    rc.addResourceAtStart(fullPath, label);
                }
                //System.err.println("rc:" + rc);
                return;
            }
        }
        //Is it a property file
        if (filename.endsWith(".properties")) {
            if (topLevel) {
                addPluginToList(filename);
            }
            getArgsManager().propertyFiles.add(prefix + filename);
            getStateManager().loadProperties();
            return;
        }
        if (topLevel) {
            //            addPluginToList("unknown:" + filename);
        }
        //System.err.println("Don't know how to handle this:" + filename );
    }




    /**
     * Load the jar file
     *
     * @param jarFilePath Load the jar
     */
    protected void loadJar(String jarFilePath) {
        // System.err.println("loading jar " + jarFilePath);
        addPluginToList(jarFilePath);

        Trace.msg("IdvResourceManager.loadJar:" + jarFilePath);
        try {
            jarCnt++;
            if ( !new File(jarFilePath).exists()) {
                //                check if its  a url
                String fileName = encode(jarFilePath);
                String tmpDir   = getStore().getUserTmpDirectory().toString();
                tmpDir = IOUtil.joinDir(tmpDir, "plugins");
                IOUtil.makeDir(tmpDir);
                File newFile = new File(IOUtil.joinDir(tmpDir,
                                   Misc.getUniqueId() + ".jar"));
                byte[] bytes =
                    IOUtil.readBytes(IOUtil.getInputStream(jarFilePath,
                        getClass()));
                IOUtil.writeBytes(newFile, bytes);
                jarFilePath = newFile.toString();
            }
            String jarLabel = IOUtil.getFileTail(decode(jarFilePath));
            String prefix   = jarFilePath + "!/";
            PluginClassLoader cl = new PluginClassLoader(jarFilePath,
                                       getClass().getClassLoader()) {

                protected void handleError(String msg, Throwable exc) {
                    PluginManager.this.addError(msg, exc);
                }

                protected void checkClass(Class c) throws Exception {
                    //                    System.out.println ("loaded class:" + c.getName() + " from:" + toString());
                    IdvBase.addPluginClass(c);
                    if (java.text.DateFormat.class.isAssignableFrom(c)) {
                        visad.DateTime.setDateFormatClass(c);
                    } else if (ucar.nc2.iosp.IOServiceProvider.class
                            .isAssignableFrom(c)) {
                        ucar.nc2.NetcdfFile.registerIOProvider(c);
                    } else if (ucar.nc2.dataset.CoordSysBuilderIF.class
                            .isAssignableFrom(c)) {
                        ucar.nc2.dataset.CoordSysBuilderIF csbi =
                            (ucar.nc2.dataset
                                .CoordSysBuilderIF) c.newInstance();
                        ucar.nc2.dataset.CoordSysBuilder.registerConvention(
                            csbi.getConventionUsed(), c);
                    } else if (ucar.nc2.dataset.CoordTransBuilderIF.class
                            .isAssignableFrom(c)) {
                        ucar.nc2.dataset.CoordTransBuilderIF csbi =
                            (ucar.nc2.dataset
                                .CoordTransBuilderIF) c.newInstance();
                        ucar.nc2.dataset.CoordTransBuilder.registerTransform(
                            csbi.getTransformName(), c);
                    } else if (ucar.nc2.dt.TypedDatasetFactoryIF.class
                            .isAssignableFrom(c)) {
                        ucar.nc2.dt.TypedDatasetFactoryIF tdfi =
                            (ucar.nc2.dt
                                .TypedDatasetFactoryIF) c.newInstance();
                        ucar.nc2.dt.TypedDatasetFactory.registerFactory(
                            tdfi.getScientificDataType(), c);
                    }

                }
            };
            pluginClassLoaders.add(cl);
            Misc.addClassLoader(cl);
            List entries = cl.getEntryNames();
            for (int i = 0; i < entries.size(); i++) {
                String entry = (String) entries.get(i);
                if (getArgsManager().isRbiFile(entry)) {
                    loadPlugin(entry, prefix, false);
                }

            }

            //Now load in everything else
            for (int i = 0; i < entries.size(); i++) {
                String entry = (String) entries.get(i);
                if ( !getArgsManager().isRbiFile(entry)) {
                    if (entry.endsWith(".jar")) {
                        //Here we have a jar file inside a jar file.
                        String jarFile = prefix + entry;
                        //                        System.err.println ("loading jar file:" + jarFile);
                        loadJar(jarFile);
                    } else {
                        loadPlugin(entry, prefix, false, "From: " + jarLabel);
                    }
                }
            }
        } catch (Exception exc) {
            addError("Opening jars", exc);
        }
    }





    /**
     * Show a dialog that lists the loaded plugins
     *
     * @return the plugin html listing to include in support requests
     */
    public String getPluginHtml() {
        StringBuffer sb  = new StringBuffer();
        int          cnt = 0;
        for (int i = 0; i < myPlugins.size(); i++) {
            final File file = new File(myPlugins.get(i).toString());
            if ( !file.exists()) {
                continue;
            }
            if (cnt == 0) {
                sb.append("<h3>Plugins</h3>\n<ul>\n");
            }
            cnt++;
            sb.append("<li>" + file);
        }
        if (cnt > 0) {
            sb.append("</ul>");
        }
        return sb.toString();
    }


    /**
     * import plugin from file
     */
    public void importPlugin() {
        String filename = FileManager.getReadFile("Open Plugin",
                              Misc.newList(FileManager.FILTER_JAR),
                              GuiUtils.top(mergeCbx));
        if (filename == null) {
            return;
        }
        importPlugin(filename, mergeCbx.isSelected());
    }



    /**
     * import plugin from file
     *
     * @param filename filename
     */
    public void importPlugin(String filename) {
        importPlugin(decode(filename), true);
    }


    /**
     * import plugin from file
     *
     * @param filename filename
     * @param merge merge
     */
    public void importPlugin(String filename, boolean merge) {
        try {
            showCreatePlugin();
            String dir = IOUtil.joinDir(getStore().getUserTmpDirectory(),
                                        "plugin_" + Misc.getUniqueId());

            IOUtil.makeDir(dir);

            if ( !merge) {
                createFileList = new Vector();
                createList.setListData(createFileList);
            }

            if ( !filename.toLowerCase().endsWith(".jar")) {
                installPlugin(filename, true);
                updatePlugins();
                return;
            }
            Pattern bundlesPattern =
                IdvResourceManager.RSC_BUNDLEXML.getPattern();
            Pattern ctPattern =
                IdvResourceManager.RSC_COLORTABLES.getPattern();


            String newJarFilePath = IOUtil.joinDir(dir,
                                        IOUtil.getFileTail(filename));

            IOUtil.writeTo(IOUtil.getInputStream(filename, getClass()),
                           new FileOutputStream(newJarFilePath));
            JarFile jarFile = new JarFile(newJarFilePath);
            List    entries = Misc.toList(jarFile.entries());
            //First load in the class files
            List resources = getResourceManager().getResources();


            for (int i = 0; i < entries.size(); i++) {
                JarEntry entry = (JarEntry) entries.get(i);
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                //Assume that any bundles are accessed through the bundles.xml file

                InputStream is        = jarFile.getInputStream(entry);
                String      cleanName = IOUtil.cleanFileName(name);
                String      tmpFile   = IOUtil.joinDir(dir, cleanName);
                IOUtil.writeTo(is, new FileOutputStream(tmpFile));
                is.close();

                //Assume any bundle files are defined in a bundles.xml
                if (getArgsManager().isXidvFile(name)) {
                    continue;
                }
                if (name.toLowerCase().endsWith("manifest.mf")) {
                    continue;
                }

                if (bundlesPattern.matcher(name).find()) {
                    Element root = XmlUtil.getRoot(tmpFile, getClass());
                    List bundles = SavedBundle.processBundleXml(root, dir,
                                       getResourceManager(), true);
                    addObjects(bundles);
                    continue;
                }

                if (ctPattern.matcher(name).find()) {
                    ColorTableManager ctm = new ColorTableManager();
                    ctm.init(new ResourceCollection("tmp",
                            Misc.newList(tmpFile)));
                    addObjects(ctm.getColorTables());
                    continue;
                }

                //                System.err.println ("tmp file:" + tmpFile);
                String label = IOUtil.getFileTail(name);
                for (int resourceIdx = 0; resourceIdx < resources.size();
                        resourceIdx++) {
                    IdvResourceManager.IdvResource idvResource =
                        (IdvResourceManager.IdvResource) resources.get(
                            resourceIdx);
                    if (idvResource.getPattern() == null) {
                        continue;
                    }
                    if (idvResource.getPattern().matcher(name).find()) {
                        label = idvResource.getDescription() + ":" + label;
                    }
                }


                addCreateFile(tmpFile, label);
            }

        } catch (Throwable exc) {
            logException("Opening plugin", exc);
        }
    }


    /**
     * Prompt for a plugin filename and install it.
     */
    public void installPluginFromFile() {
        String filename = FileManager.getReadFile(FileManager.FILTER_JAR);
        if (filename != null) {
            installPluginFromFile(filename);
        }
    }


    /**
     * Install a plugin from a file
     *
     * @param filename  file name
     */
    public void installPluginFromFile(String filename) {
        try {
            installPlugin(filename, true);
            updatePlugins();
            notifyUser();
        } catch (Throwable exc) {
            logException("Installing plugin", exc);
        }
    }

    /**
     * Notify the users to restart the IDV
     */
    protected void notifyUser() {
        if ( !getInstallManager().isRestartable()) {
            LogUtil.userMessage(
                "You will need to restart the IDV for this change to take effect");
            return;
        }

        if (GuiUtils.askYesNo(
                "Plugin Confirmation",
                new JLabel(
                    "<html>You will need to restart the IDV for this change to take effect<br>Do you want to restart?"))) {

            try {
                getInstallManager().restart();
            } catch (Throwable exc) {
                logException("Restarting the IDV", exc);
            }
        }
    }

    /**
     * Prompt for a plugin url and install it.
     */
    public void installPluginFromUrl() {
        String filename = "";
        while (true) {
            filename = GuiUtils.getInput(
                "Please enter the URL to an IDV  plugins jar file", "URL: ",
                filename);
            if ((filename == null) || (filename.trim().length() == 0)) {
                return;
            }
            try {
                installPlugin(filename, true);
                updatePlugins();
                notifyUser();
                return;
            } catch (Throwable exc) {
                logException("Installing plugin", exc);
            }
        }
    }


    /**
     * Create the list of plugins
     */
    private void createPluginList() {
        XmlResourceCollection xrc = getResourceManager().getXmlResources(
                                        getResourceManager().RSC_PLUGININDEX);

        double version = getStateManager().getNumberVersion();


        for (int i = 0; i < xrc.size(); i++) {
            String path = xrc.get(i).toString();
            path = IOUtil.getFileRoot(path);
            Element root = xrc.getRoot(i);
            if (root == null) {
                continue;
            }
            List children = XmlUtil.findChildren(root, TAG_PLUGIN);
            for (int pluginIdx = 0; pluginIdx < children.size();
                    pluginIdx++) {
                Element pluginNode = (Element) children.get(pluginIdx);
                String  name = XmlUtil.getAttribute(pluginNode, ATTR_NAME);
                String desc = XmlUtil.getAttribute(pluginNode, ATTR_DESC,
                                  name);
                String size = XmlUtil.getAttribute(pluginNode, ATTR_SIZE,
                                  (String) null);
                String url = XmlUtil.getAttribute(pluginNode, ATTR_URL);
                if (IOUtil.isRelativePath(url)) {
                    url = path + "/" + url;
                }
                String category = XmlUtil.getAttribute(pluginNode,
                                      ATTR_CATEGORY, "Miscellaneous");
                Plugin plugin = new Plugin(name, desc, url, category);
                plugin.size = size;
                if (XmlUtil.hasAttribute(pluginNode, ATTR_VERSION)) {
                    plugin.version = XmlUtil.getAttribute(pluginNode,
                            ATTR_VERSION, version);
                    plugin.versionOk = plugin.version <= version;

                }
            }
        }

        if (localPluginDir != null) {
            scourPlugins(localPluginDir);
            File[] files = localPluginDir.listFiles();
            files = IOUtil.sortFilesOnAge(files, false);
            for (int fileIdx = 0; fileIdx < files.length; fileIdx++) {
                final File file = files[fileIdx];
                if (pluginFileOK(file)) {
                    new Plugin(file);
                }
            }
        }
    }

    /**
     * Is the given file ok to load in as a plugin. This checks if there is a .deletethi file
     * or if this file is a .deletethis file
     *
     * @param file file
     *
     * @return ok to load as plugin
     */
    private boolean pluginFileOK(File file) {
        if (file.toString().endsWith(".deletethis")) {
            return false;
        }
        File deleteFile = new File(file.toString() + ".deletethis");
        if (deleteFile.exists()) {
            return false;
        }
        if (file.getName().startsWith(".tmp.")) {
            return false;
        }
        return true;
    }


    /**
     * Remove all .deletethis files from the given directory
     *
     * @param dir directory to scour
     */
    private void scourPlugins(File dir) {
        File[] files = dir.listFiles();
        for (int jarFileIdx = 0; jarFileIdx < files.length; jarFileIdx++) {
            String filename = files[jarFileIdx].toString();
            if (filename.endsWith(".deletethis")) {
                File deleteFile = new File(filename);
                deleteFile.delete();
                deleteFile = new File(IOUtil.stripExtension(filename));
                if (deleteFile.exists()) {
                    deleteFile.delete();
                }
            }
        }
    }


    /**
     * install the plugin
     *
     * @param plugin url or file name of the plugin
     */
    public void installPlugin(final String plugin) {
        Misc.run(new Runnable() {
            public void run() {
                installPluginInThread(plugin);
            }
        });
    }

    /**
     * install the plugin
     *
     * @param plugin url or file name of the plugin
     */
    public void installPluginInThread(String plugin) {
        try {
            Plugin p = (Plugin) Plugin.pathToPlugin.get(plugin);
            File   f = installPlugin(plugin, false);
            if (f == null) {
                return;
            }
            if (p != null) {
                p.install();
                p.file = f;
            }
            updatePlugins();
            notifyUser();
        } catch (Throwable exc) {
            logException("Installing plugin: " + plugin, exc);
        }
    }


    /**
     * Install the plugin. May be a filename or url.
     * Copy the bytes into the plugin directory.
     *
     * @param plugin filename or url
     * @param andLoad should we also load the plugin
     *
     *
     * @return The new fil epath_
     * @throws Exception On badness
     */
    private File installPlugin(String plugin, boolean andLoad)
            throws Exception {
        String filename    = encode(plugin);
        String tmpFilename = ".tmp." + filename;
        String extDir =
            IOUtil.joinDir(getStore().getUserDirectory().toString(),
                           "plugins");
        File newTmpFile = new File(IOUtil.joinDir(extDir, tmpFilename));
        File newFile    = new File(IOUtil.joinDir(extDir, filename));
        Object loadId =
            JobManager.getManager().startLoad("Installing plugin", true);
        byte[] bytes = null;
        try {
            URL url = IOUtil.getURL(plugin, getClass());
            if (IOUtil.writeTo(url, newTmpFile, loadId) <= 0) {
                newTmpFile.delete();
                return null;
            }
            IOUtil.moveFile(newTmpFile, newFile);
            //      newTmpFile.delete();
        } finally {
            JobManager.getManager().stopLoad(loadId);
        }
        if (andLoad) {
            new Plugin(newFile);
        }
        if (andLoad) {
            handlePlugin(newFile.toString());
        }
        return newFile;
    }




    /**
     * make dialog
     */
    private void makePluginDialog() {
        Component[] availableComps = GuiUtils.getHtmlComponent("", getIdv(),
                                         700, 300);
        availablePluginEditor   = (JEditorPane) availableComps[0];
        availablePluginScroller = (JScrollPane) availableComps[1];

        Component[] loadedComps = GuiUtils.getHtmlComponent("", getIdv(),
                                      700, 200);
        loadedPluginEditor = (JEditorPane) loadedComps[0];


        JComponent contents = GuiUtils.vsplit((JComponent) loadedComps[1],
                                  (JComponent) availableComps[1], 150);

        contents.setSize(new Dimension(700, 500));
        JButton closeBtn = GuiUtils.makeButton("Close", this,
                               "closePluginDialog");
        JMenuBar menuBar  = new JMenuBar();
        JMenu    fileMenu = new JMenu("File");
        JMenu    helpMenu = new JMenu("Help");
        helpMenu.add(GuiUtils.makeMenuItem("Plugin Manager", this,
                                           "showManagerHelp"));

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        fileMenu.add(GuiUtils.makeMenuItem("Install Plugin From File", this,
                                           "installPluginFromFile"));
        fileMenu.add(GuiUtils.makeMenuItem("Install Plugin From URL", this,
                                           "installPluginFromUrl"));


        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this,
                                           "closePluginDialog"));

        String[] keys = {
            "/auxdata/ui/icons/add.png", "Install Plugin",
            "/auxdata/ui/icons/plugin_delete.png", "Delete Plugin",
            "/auxdata/ui/icons/DocumentOpen16.png", "Send to Plugin Creator",
            "/auxdata/ui/icons/FindAgain16.gif", "View Contents"
        };

        List<Component> keyComps = new ArrayList<Component>();
        keyComps.add(new JLabel("Key:  "));
        for (int keyIdx = 0; keyIdx < keys.length; keyIdx += 2) {
            keyComps.add(new JLabel(GuiUtils.getImageIcon(keys[keyIdx],
                    getClass())));
            keyComps.add(new JLabel(" " + keys[keyIdx + 1] + "  "));
        }

        JComponent bottom = GuiUtils.hbox(keyComps);
        bottom   = GuiUtils.inset(bottom, 4);
        contents = GuiUtils.topCenterBottom(menuBar, contents, bottom);

        contents = GuiUtils.centerBottom(
            contents,
            GuiUtils.center(GuiUtils.inset(GuiUtils.wrap(closeBtn), 5)));
        pluginWindow = GuiUtils.createFrame(GuiUtils.getApplicationTitle()
                                            + "Plugin Manager");

        pluginWindow.getContentPane().add(contents);
        GuiUtils.decorateFrame(pluginWindow, menuBar);
        pluginWindow.pack();
        ucar.unidata.util.Msg.translateTree(pluginWindow);
        pluginWindow.setLocation(100, 100);
    }

    /**
     * close dialog
     */
    public void closePluginDialog() {
        if (pluginWindow != null) {
            pluginWindow.setVisible(false);
        }
    }

    /**
     * Decode the name of the file. This is the full url or original file path
     *
     * @param f file
     *
     * @return decoded name
     */
    public static String decode(File f) {
        return decode(f.getName());
    }


    /**
     * Decode the name of the file. This is the full url or original file path
     *
     * @param filename filen
     *
     * @return decoded name
     */
    public static String decode(String filename) {
        try {
            return java.net.URLDecoder.decode(filename, "UTF-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            System.err.println("decoding error:" + uee);
            return null;
        }
    }

    /**
     * Encode the filename
     *
     * @param filename file or url
     *
     * @return encoded version
     */
    public static String encode(String filename) {
        try {
            return java.net.URLEncoder.encode(filename, "UTF-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            System.err.println("encoding error:" + uee);
            return null;
        }
    }

    /**
     * Show or hide the category
     *
     * @param category category to show or hide
     */
    public void toggleCategory(String category) {
        Boolean show = (Boolean) categoryToggle.get(category);
        if (show == null) {
            show = Boolean.FALSE;
        } else {
            show = new Boolean( !show.booleanValue());
        }
        categoryToggle.put(category, show);
        updatePlugins(false);
    }



    /**
     * Show a dialog that lists the loaded plugins
     */
    public void updatePlugins() {
        updatePlugins(true);
    }

    /**
     * Show a dialog that lists the loaded plugins
     * @param doLoaded Update the loaded list as well
     */
    public void updatePlugins(boolean doLoaded) {

        boolean firstTime = false;
        if (pluginWindow == null) {
            makePluginDialog();
            createPluginList();
            firstTime = true;
        }

        StringBuffer loadedBuff =
            new StringBuffer(
                "<b>Installed Plugins</b><br><table width=\"100%\" border=\"0\">");
        List      comps       = new ArrayList();
        List      havePlugins = new ArrayList();
        List      pluginComps = new ArrayList();
        List      cats        = new ArrayList();
        Hashtable catBuffs    = new Hashtable();
        for (int i = 0; i < Plugin.plugins.size(); i++) {
            Plugin plugin = (Plugin) Plugin.plugins.get(i);
            StringBuffer catBuff =
                (StringBuffer) catBuffs.get(plugin.category);
            if (catBuff == null) {
                catBuff = new StringBuffer();
                cats.add(plugin.category);
                catBuffs.put(plugin.category, catBuff);
            }

            String prefix = "";
            String encodedPath;
            if (plugin.file != null) {
                encodedPath = encode(plugin.file.toString());
                prefix =
                    "&nbsp;<a href=\"jython:idv.getPluginManager().listPlugin('"
                    + encodedPath
                    + "');\"><img src=\"idvresource:/auxdata/ui/icons/FindAgain16.gif\" border=\"0\"></a>";
            } else {
                encodedPath = encode(plugin.url.toString());
                prefix      = "&nbsp;";
            }

            prefix =
                prefix
                + "<a href=\"jython:idv.getPluginManager().importPlugin('"
                + encodedPath
                + "');\"><img alt='Import Plugin into Plugin Creator' src=\"idvresource:/auxdata/ui/icons/DocumentOpen16.png\" border=\"0\"></a>";
            String sizeString = "";
            if (plugin.size != null) {
                int s = new Integer(plugin.size).intValue();
                sizeString = "&nbsp;" + HtmlUtil.b((s / 1000) + "KB");
            }

            StringBuffer addDelete = new StringBuffer();
            String installHtml =
                "<a href=\"jython:idv.getPluginManager().installPlugin('"
                + plugin.url + "')\">";

            if ( !plugin.versionOk) {
                addDelete.append(HtmlUtil.b("requires IDV: "
                                            + plugin.version));
            } else if (plugin.deleted) {
                addDelete.append(HtmlUtil.b("removed"));
            } else if (plugin.installed) {
                String extra = "";
                if (plugin.hasOriginal) {
                    extra =
                        installHtml
                        + HtmlUtil.img(
                            "idvresource:/auxdata/ui/icons/Refresh16.gif") + "</a>";
                }


                String deleteHtml =
                    "<a href=\"jython:idv.getPluginManager().removePlugin('"
                    + plugin.getFilePath() + "')\">";
                //                System.err.println ("html: " + deleteHtml);
                addDelete.append(deleteHtml
                        + HtmlUtil.img("idvresource:/auxdata/ui/icons/plugin_delete.png")
                        + "</a>&nbsp;" + extra);
                loadedBuff.append(HtmlUtil.open(HtmlUtil.TAG_TR,
                        HtmlUtil.attr(HtmlUtil.ATTR_VALIGN, "top")));
                loadedBuff.append(HtmlUtil.col(addDelete.toString(),
                        HtmlUtil.attr(HtmlUtil.ATTR_WIDTH, "50")));
                loadedBuff.append(HtmlUtil.col(plugin.category + "&gt;"
                        + plugin.name));
                loadedBuff.append(HtmlUtil.col(prefix,
                        HtmlUtil.attr(HtmlUtil.ATTR_WIDTH, "50")));
                loadedBuff.append(HtmlUtil.close(HtmlUtil.TAG_TR));
            } else {
                addDelete.append(
                    installHtml
                    + HtmlUtil.img("idvresource:/auxdata/ui/icons/add.png"));
            }

            String rowExtra = (plugin.installed
                               ? HtmlUtil.attr(HtmlUtil.ATTR_BGCOLOR,
                                   "#dddddd")
                               : "");
            catBuff.append(
                HtmlUtil.open(
                    HtmlUtil.TAG_TR,
                    rowExtra + HtmlUtil.attr(HtmlUtil.ATTR_VALIGN, "top")));
            catBuff.append(HtmlUtil.col(addDelete.toString(),
                                        HtmlUtil.attrs(HtmlUtil.ATTR_WIDTH,
                                            "10%", HtmlUtil.ATTR_ALIGN,
                                            "right")));
            catBuff.append(HtmlUtil.col(plugin.name.replace(" ", "&nbsp;"),
                                        HtmlUtil.attr(HtmlUtil.ATTR_WIDTH,
                                            "20%")));
            catBuff.append(HtmlUtil.col(plugin.desc,
                                        HtmlUtil.attr(HtmlUtil.ATTR_WIDTH,
                                            "50%")));
            catBuff.append(HtmlUtil.col(sizeString,
                                        HtmlUtil.attrs(HtmlUtil.ATTR_WIDTH,
                                            "10%", HtmlUtil.ATTR_ALIGN,
                                            "right")));
            catBuff.append(HtmlUtil.col(prefix,
                                        HtmlUtil.attr(HtmlUtil.ATTR_WIDTH,
                                            "10%")));
            catBuff.append(HtmlUtil.close(HtmlUtil.TAG_TR));
        }

        StringBuffer sb =
            new StringBuffer(
                "<b>Available Plugins</b><br><table border=\"0\" width=\"100%\">\n");
        for (int i = 0; i < cats.size(); i++) {
            String       category = (String) cats.get(i);
            StringBuffer catBuff  = (StringBuffer) catBuffs.get(category);
            Boolean      show     = (Boolean) categoryToggle.get(category);
            if (show == null) {
                show = new Boolean(false);
                categoryToggle.put(category, show);
            }
            String toggleHref =
                "<a href=\"jython:idv.getPluginManager().toggleCategory('"
                + category + "')\">";
            String catToShow = StringUtil.replace(category, " ", "&nbsp;");
            if ((show == null) || show.booleanValue()) {
                sb.append(
                    "<tr><td colspan=\"3\">" + toggleHref
                    + "<img src=\"idvresource:/auxdata/ui/icons/CategoryOpen.gif\"  border=\"0\"></a>&nbsp; <b><span style=\"xxxxfont-size:18\">"
                    + catToShow + "</span></b></td></tr>");
                sb.append(catBuff.toString());
            } else {
                sb.append(
                    "<tr><td colspan=\"3\">" + toggleHref
                    + "<img src=\"idvresource:/auxdata/ui/icons/CategoryClosed.gif\" border=\"0\"></a>&nbsp; <b><span style=\"xxxxfont-size:18\">"
                    + catToShow + "</span></b></td></tr>");
            }
        }
        sb.append("</table>");

        loadedBuff.append("</table>");

        if (doLoaded) {
            loadedPluginEditor.setText(loadedBuff.toString());
            loadedPluginEditor.invalidate();
            loadedPluginEditor.repaint();
        }


        availablePluginEditor.setVisible(false);
        availablePluginEditor.setText(sb.toString());
        availablePluginEditor.invalidate();
        availablePluginEditor.setVisible(true);
        availablePluginEditor.repaint();
        if (firstTime) {
            GuiUtils.showDialogNearSrc(null, pluginWindow);
        } else {
            pluginWindow.setVisible(true);
        }

    }




    /**
     * Class Plugin holds info about all of the loaded and available plugins
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.54 $
     */
    private static class Plugin {

        /** mapping */
        static Hashtable pathToPlugin = new Hashtable();

        /** list */
        static List plugins = new ArrayList();


        /** file for installed plugins */
        File file;

        /** state */
        boolean deleted = false;

        /** state */
        boolean installed = false;

        /** name */
        String name;

        /** desc */
        String desc;

        /** url or original file name */
        String url;

        /** category */
        String category;

        /** Is version ok */
        boolean versionOk = true;

        /** The version */
        double version = Double.MAX_VALUE;

        /** DO we have a version of this plugin on the web site */
        boolean hasOriginal = false;

        /** The size string from the xml */
        String size;

        /**
         * ctor
         *
         * @param name name
         * @param desc desc
         * @param url url
         * @param category cat
         */
        public Plugin(String name, String desc, String url, String category) {
            this.name        = name;
            this.desc        = desc;
            this.url         = url;
            this.hasOriginal = true;
            this.category    = category;
            pathToPlugin.put(url, this);
            plugins.add(this);
        }



        /**
         * ctor for installed plugins
         *
         * @param f file
         */
        public Plugin(File f) {
            this.file = f;
            this.url  = decode(f);
            Plugin p = (Plugin) pathToPlugin.get(url);
            installed = true;
            if (p != null) {
                this.size        = p.size;
                this.name        = p.name;
                this.desc        = p.desc;
                this.category    = p.category;
                this.hasOriginal = p.hasOriginal;
                int index = plugins.indexOf(p);
                plugins.remove(p);
                plugins.add(index, this);
            } else {
                this.category = "Miscellaneous";
                this.name     = IOUtil.getFileTail(url);
                this.desc     = "";
                plugins.add(this);
            }
            pathToPlugin.put(url, this);
        }


        /**
         * set state
         */
        public void install() {
            deleted   = false;
            installed = true;
        }

        /**
         * set state
         */
        public void delete() {
            installed = false;
            deleted   = true;
        }


        /**
         * Get the file path
         *
         * @return the file path
         */
        public String getFilePath() {
            String path = file.toString();
            path = path.replaceAll("\\\\", "/");
            return path;
        }

        /**
         * tostring
         *
         * @return string
         */
        public String toString() {
            return name + " installed:" + installed + " deleted:" + deleted;
        }
    }


    /**
     * test
     *
     * @param args args
     *
     * @throws IOException on badness
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                "Error:  Usage: PluginManager jarfile <file1 file2 ...>");
            System.exit(1);
        }
        List files = Misc.toList(args);
        files.remove(0);
        IOUtil.writeJarFile(args[0], files, "/foo/bar");
    }


    /**
     * Class Wrapper is used in the create list to have a toString
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.54 $
     */
    private static class Wrapper {

        /** Object I wrap */
        Object obj;

        /** tostring */
        String label = "";

        /**
         * ctor
         *
         * @param obj the object
         */
        public Wrapper(Object obj) {
            this(obj, null);
        }

        /**
         * ctro
         *
         * @param obj obj
         * @param label lbl
         */
        public Wrapper(Object obj, String label) {
            this.obj = obj;
            if (label == null) {
                setLabel(obj, true);
            } else {
                this.label = label;
            }
        }

        /**
         * figure out the label
         *
         * @param obj object
         * @param addObj include obj.toString in the label
         */
        private void setLabel(Object obj, boolean addObj) {
            String suffix = "";
            if (addObj) {
                suffix = ": ";
            }
            if (obj instanceof ColorTable) {
                label = "Color Table" + suffix + (addObj
                        ? obj.toString()
                        : "");
            } else if (obj instanceof StationModel) {
                label = "Layout Model" + suffix + (addObj
                        ? obj.toString()
                        : "");
            } else if (obj instanceof SavedBundle) {
                SavedBundle bundle = (SavedBundle) obj;
                if (addObj) {
                    label = "Bundle" + suffix + bundle.getLabel();
                } else {
                    label = "Bundle" + suffix;
                }
            } else if (obj instanceof Projection) {
                label = "Projection" + suffix + (addObj
                        ? obj.toString()
                        : "");
            } else if (obj instanceof DerivedDataDescriptor) {
                label = "Formula" + suffix + (addObj
                        ? obj.toString()
                        : "");
            } else if (obj instanceof ParamInfo) {
                label = "Param Default" + suffix + (addObj
                        ? obj.toString()
                        : "");

            } else if (obj instanceof DisplaySetting) {
                label = "Display Setting" + suffix + (addObj
                        ? obj.toString()
                        : "");

            } else if (obj instanceof DataGroup) {
                label = "Param Group" + suffix + (addObj
                        ? obj.toString()
                        : "");
            } else if (obj instanceof List) {
                List l = (List) obj;
                if (l.size() > 0) {
                    setLabel(l.get(0), false);
                }
            } else if (obj.toString().endsWith(".py")) {
                label = "Jython" + suffix + (addObj
                                             ? obj.toString()
                                             : "");
            } else {
                label = obj.toString();
                return;
            }
        }

        /**
         * tostring
         *
         * @return tostring
         */
        public String toString() {
            return label;
        }

        /**
         * equals
         *
         * @param o object
         *
         * @return equals
         */
        public boolean equals(Object o) {
            if ( !(o instanceof Wrapper)) {
                return false;
            }
            return Misc.equals(obj, ((Wrapper) o).obj);


        }


    }



}

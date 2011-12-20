/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceResults;

import ucar.unidata.data.grid.GridDataSource;


import ucar.unidata.idv.chooser.*;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.DataSelector;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IslDialog;
import ucar.unidata.idv.ui.LoadBundleDialog;
import ucar.unidata.idv.ui.QuicklinkPanel;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.ui.RovingProgress;


import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.Prototypable;
import ucar.unidata.util.PrototypeManager;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;

import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.*;

import ucar.unidata.xml.XmlUtil;

import visad.util.ThreadManager;


import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import java.util.Vector;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.text.*;




/**
 * This class defines what is to be saved when we are
 * doing an advanced save of the state to a bundle
 *
 * @author IDV development team
 */
public class IdvPersistenceManager extends IdvManager implements PrototypeManager {

    /** The macro for the bundle path in data paths */
    public static final String PROP_BUNDLEPATH = "idv.bundlepath";


    /** The macro for the zidv path in data paths */
    public static final String PROP_ZIDVPATH = "idv.zidvpath";


    /** property id */
    public static final String PROP_TIMESLIST = "idv.timeslist";

    /** property id  for ensemble list */
    public static final String PROP_ENSLIST = "idv.enslist";


    /** Category name */
    public static final String CAT_GENERAL = "General";

    /** Category name */
    public static final String CAT_TOOLBAR = "Toolbar";


    /** The type  to specify all bundles */
    public static final int BUNDLES_ALL = -1;


    /** The type  to specify the "Favorites" bundles */
    public static final int BUNDLES_FAVORITES = SavedBundle.TYPE_FAVORITE;

    /** The type to specify the display templates */
    public static final int BUNDLES_DISPLAY = SavedBundle.TYPE_DISPLAY;

    /** The type to specify the data */
    public static final int BUNDLES_DATA = SavedBundle.TYPE_DATA;


    /** The separator to use when displaying categories */
    public static final String CATEGORY_SEPARATOR = ">";



    /** List of OjbectPairs that define a name->list of files mapping */
    private List fileMapping;

    /**
     * Use this so the persisted display control can acces the template name
     * when it is saved
     */
    private String currentTemplateName;


    /** Holds the list of SavedBundle objects created from the bundles.xml */
    private List<SavedBundle> bundlesFromXml;

    /** JCheckBox for saving the view state */
    private JCheckBox saveViewStateCbx;

    /** JCheckBox for saving the display */
    private JCheckBox saveDisplaysCbx;

    /** JCheckBox for saving the data sources */
    private JCheckBox saveDataSourcesCbx;

    /** JCheckBox for saving the visad data */
    private JCheckBox saveDataCbx;

    /** Used in file dialog to ask the user to make data editable */
    private JCheckBox makeDataEditableCbx;

    /** Used in file dialog to ask the user to make data editable */
    private boolean makeDataEditable = false;


    /** Used in file dialog to ask the user to make data relative */
    private JCheckBox makeDataRelativeCbx;

    /** Used in file dialog to ask the user to make data relative */
    private boolean makeDataRelative = false;


    /** Holds the jython save widgets */
    //    private JPanel jythonPanel;

    /** JCheckBox for saving the jython library */
    //    private JRadioButton saveNoJythonBtn;

    private JComboBox saveJythonBox;

    /** lists the publishers */
    private JComboBox publishCbx;

    /** JCheckBox for saving all of the jython library */
    //    private JRadioButton saveAllJythonBtn;


    /** JCheckBox for saving subsets of the jython library */
    //    private JRadioButton saveSelectedJythonBtn;

    /** Flag for saving the views */
    private boolean saveViewState = true;

    /** Flag for saving the displays */
    private boolean saveDisplays = true;

    /** Flag for saving the data sources */
    private boolean saveDataSources = true;

    /** Flag for saving the jython library */
    private boolean saveJython = false;


    /** Flag for saving the data */
    private boolean saveData = false;

    /** A cached list of the display templates in the users directory */
    private List<SavedBundle> displayTemplates;

    /** List of bundles for saved data sources */
    private List<SavedBundle> dataSourceBundles;


    /**
     * This is the name of the last xidv file  that was selected by the user.
     * We keep this around for when they do a <i>File-&gt;Save</i> command
     */
    String currentFileName = null;


    /** For saving isl */
    IslDialog islDialog;


    /** for saving jnlps */
    private JCheckBox includeBundleCbx;

    /** for saving jnlps */
    private JTextField bundlePrefixFld;

    /** for saving jnlps */
    private JComponent bundleUrlComp;

    /** for saving favorites */
    private boolean catSelected;




    /**
     * The ctor
     *
     * @param idv The IDV
     */
    public IdvPersistenceManager(IntegratedDataViewer idv) {

        super(idv);


        cleanupOldSavedBundles();



        //Check to see if we have the prototype dir defined:
        if (getPrototypeFile(getClass()) != null) {
            //Only set it once
            if (Misc.getPrototypeManager() == null) {
                Misc.setPrototypeManager(this);
            }
        }
        makeDataEditableCbx = new JCheckBox("Enable user to change data",
                                            false);
        makeDataEditableCbx.setToolTipText(
            "When loading in this saved bundle do you want to be able to change the file paths of the data");


        makeDataRelativeCbx = new JCheckBox("Save with relative paths",
                                            false);
        makeDataRelativeCbx.setToolTipText(
            "Write out this bundle with the data sources having paths relative to the bundle when loaded");
        saveViewStateCbx = new JCheckBox("Views", true);
        int keyCode = GuiUtils.charToKeyCode(saveViewStateCbx.getText());
        if (keyCode != -1) {
            saveViewStateCbx.setMnemonic(keyCode);
        }
        saveViewStateCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveViewState = saveViewStateCbx.isSelected();
            }
        });
        saveDisplaysCbx = new JCheckBox("Displays", true);
        keyCode         = GuiUtils.charToKeyCode(saveDisplaysCbx.getText());
        if (keyCode != -1) {
            saveDisplaysCbx.setMnemonic(keyCode);
        }
        saveDisplaysCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveDisplays = saveDisplaysCbx.isSelected();
            }
        });

        saveDataSourcesCbx = new JCheckBox("Data Sources", true);
        keyCode            = GuiUtils.charToKeyCode("S");
        if (keyCode != -1) {
            saveDataSourcesCbx.setMnemonic(keyCode);
        }
        saveDataSourcesCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveDataSources = saveDataSourcesCbx.isSelected();
                if ( !saveDataSources) {
                    saveDataCbx.setSelected(false);
                    saveData = false;
                }
            }
        });



        saveJythonBox = new JComboBox(new Vector(Misc.newList("No Jython",
                "All Local Jython", "Selected Jython")));

        ActionListener jythonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                //saveJython = !saveNoJythonBtn.isSelected();
                saveJython = saveJythonBox.getSelectedIndex() != 0;
            }
        };
        saveJythonBox.addActionListener(jythonListener);
        //        saveNoJythonBtn       = new JRadioButton("None", true);
        //        saveAllJythonBtn      = new JRadioButton("All", false);
        //        saveSelectedJythonBtn = new JRadioButton("Selected", false);

        //        GuiUtils.buttonGroup(saveNoJythonBtn,
        //                             saveAllJythonBtn).add(saveSelectedJythonBtn);

        //        saveNoJythonBtn.addActionListener(jythonListener);
        //        saveAllJythonBtn.addActionListener(jythonListener);
        //        saveSelectedJythonBtn.addActionListener(jythonListener);

        //        jythonPanel = GuiUtils.vbox(saveNoJythonBtn, saveAllJythonBtn,
        //                                    saveSelectedJythonBtn);


        saveDataCbx = new JCheckBox("Data", saveData);
        keyCode     = GuiUtils.charToKeyCode("A");
        if (keyCode != -1) {
            saveDataCbx.setMnemonic(keyCode);
        }
        saveDataCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveData = saveDataCbx.isSelected();
                if (saveData) {
                    saveDataSourcesCbx.setSelected(true);
                    saveDataSources = true;
                }
            }
        });

    }

    /**
     * do cleanup
     */
    private void cleanupOldSavedBundles() {
        boolean didAny          = false;
        File    savedBundlesDir = getStore().getSavedBundlesDir();
        IOUtil.makeDir(savedBundlesDir);
        File[] dirs = { new File(
                          IOUtil.joinDir(
                              getStore().getUserDirectory().toString(),
                              getStore().DIR_BUNDLES)),
                        new File(
                            IOUtil.joinDir(
                                getStore().getUserDirectory().toString(),
                                "displaytemplates")) };
        int[] types = { BUNDLES_FAVORITES, BUNDLES_DISPLAY };
        for (int i = 0; i < dirs.length; i++) {
            List oldFiles =
                IOUtil.getFiles(null, dirs[i], true,
                                getArgsManager().getXidvFileFilter());
            for (int fileIdx = 0; fileIdx < oldFiles.size(); fileIdx++) {
                didAny = true;
                File file = (File) oldFiles.get(fileIdx);

                try {
                    List categories = fileToCategories(dirs[i].toString(),
                                          file.getParent().toString());
                    String name = IOUtil.stripExtension(
                                      IOUtil.getFileTail(file.toString()));
                    File newFile = new File("");
                    SavedBundle savedBundle =
                        new SavedBundle(newFile.toString(), name, categories,
                                        null, true, types[i]);
                } catch (Exception exc) {
                    LogUtil.consoleMessage(
                        "Error cleaning up old bundles.\ndir:" + dirs[i]
                        + "\nfile:" + file + "\nparent:" + file.getParent()
                        + "\nError:" + exc);
                }
                //              System.err.println (types[i] + " cats:" +savedBundle.getCategories() +" " + savedBundle);
            }
        }

    }




    /**
     * Create and add into the list the specical {@link ControlDescriptor}s
     * that use the display templates.
     *
     * @param l List of control descriptors to add to
     */
    public void getControlDescriptors(List l) {
        List<SavedBundle> templates = getDisplayTemplates();
        for (int i = 0; i < templates.size(); i++) {
            SavedBundle bundle = (SavedBundle) templates.get(i);
            l.add(new ControlDescriptor(
                getIdv(), bundle.getUrl(),
                (DisplayControl) bundle.getPrototype()));
        }
    }


    /**
     * Define one or more file mappings. The ids is a list of Strings that
     * are used to identify a data source. The files is a list of Lists
     * of filenames, one list for each corresponding data source id.
     * This gets used when we unpersist data sources. If we have any file mappings
     * we pass these onto the data source so they can change their actual file
     *
     * @param ids List of String ids
     * @param files List of lists of filenames
     */
    public void setFileMapping(List ids, List files) {
        for (int i = 0; i < ids.size(); i++) {
            addFileMapping((String) ids.get(i), (List) files.get(i));
        }
    }

    /**
     * Clear any filemappings.
     */
    public void clearFileMapping() {
        fileMapping = null;
    }

    /**
     * Add a file mapping. See above.
     *
     * @param identifier Id use to identify a data source
     * @param files List of   files (or urls).
     */
    public void addFileMapping(String identifier, List files) {
        if (fileMapping == null) {
            fileMapping = new ArrayList();
        }
        fileMapping.add(new ObjectPair(identifier, files));
    }


    /**
     * Create, if needed, and return te GUI panel to put into the
     * file dialog when saving a file.
     *
     * @return The  file accessory panel
     */
    public JPanel getFileAccessory() {
        List fileAccessories = Misc.newList(saveViewStateCbx,
                                            saveDisplaysCbx,
                                            saveDataSourcesCbx);
        fileAccessories.add(GuiUtils.left(GuiUtils.inset(saveJythonBox,
                new Insets(0, 3, 0, 0))));
        fileAccessories.add(GuiUtils.filler(1, 10));
        fileAccessories.add(makeDataRelativeCbx);
        //fileAccessories.add(makeDataEditableCbx);

        if (publishCbx == null) {
            publishCbx = getIdv().getPublishManager().makeSelector();
        }
        if (publishCbx != null) {
            fileAccessories.add(GuiUtils.filler(1, 10));
            fileAccessories.add(publishCbx);
        }
        return GuiUtils.top(
            GuiUtils.vbox(
                Misc.newList(
                    new JLabel("What should be saved?"),
                    GuiUtils.vbox(fileAccessories))));
    }




    /**
     *  For each of  of the bundle files in the given file directory
     * create a SavedBundle object with the given categories list
     * ad add it into the given allBundles list.
     *
     * @param allBundles The list to put the bundles in
     * @param categories Categories for the SavedBundle objects
     * @param file Where to look
     */
    private void loadBundlesInDirectory(List<SavedBundle> allBundles,
                                        List categories, File file) {
        String[] localBundles =
            file.list(getArgsManager().getXidvZidvFileFilter());
        for (int i = 0; i < localBundles.length; i++) {
            String filename = IOUtil.joinDir(file.toString(),
                                             localBundles[i]);
            allBundles.add(
                new SavedBundle(
                    filename, IOUtil.stripExtension(localBundles[i]),
                    categories, null, true, SavedBundle.TYPE_FAVORITE));
        }
    }




    /**
     * Utility to convert a list of categories into a single string
     * to show the user.
     *
     * @param cats List of categories - String.
     *
     * @return String representation
     */
    public static String categoriesToString(List cats) {
        return StringUtil.join(CATEGORY_SEPARATOR, cats);
    }

    /**
     * Retur a list of categories from the given string
     *
     * @param category The string representation of the categories
     *
     * @return List of (String) categories
     */
    public static List stringToCategories(String category) {
        return StringUtil.split(category, CATEGORY_SEPARATOR, true, true);
    }


    /**
     * The given filename is a full path under the given root.
     * This method  prunes the root path from the filepath
     * and splits the filepath into a list of categories.
     *
     * @param root File root
     * @param filename File path to split
     *
     * @return List of (String) categories
     */
    public static List<String> fileToCategories(String root,
            String filename) {
        int idx = root.length() + 1;
        return StringUtil.split(filename.substring(idx), File.separator);
    }



    /**
     * Make the bundles  xml
     *
     * @param bundles List of saved bundle
     * @param includeCategoryInUrl Should we add the category to the file name
     *
     * @return bundles.xml
     */
    public static String getBundleXml(List<SavedBundle> bundles,
                                      boolean includeCategoryInUrl) {
        Document doc  = XmlUtil.makeDocument();
        Element  root = doc.createElement(SavedBundle.TAG_BUNDLES);
        for (SavedBundle savedBundle : bundles) {
            savedBundle.toXml(doc, root, includeCategoryInUrl);
        }
        return XmlUtil.toString(root);
    }




    /**
     * get list of xml bundles for the given type
     *
     * @param type bundle type
     *
     * @return list of bundles
     */
    public List<SavedBundle> getXmlBundles(int type) {
        if (bundlesFromXml == null) {
            bundlesFromXml = new ArrayList<SavedBundle>();
            XmlResourceCollection resources =
                getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_BUNDLEXML);

            try {
                for (int i = 0; i < resources.size(); i++) {
                    Element root = resources.getRoot(i);
                    if (root == null) {
                        continue;
                    }
                    String path    = resources.get(i).toString();
                    String dirRoot = IOUtil.getFileRoot(path);
                    bundlesFromXml.addAll(SavedBundle.processBundleXml(root,
                            dirRoot, getResourceManager(),
                            resources.isWritable(i)));
                }
            } catch (Exception exc) {
                LogUtil.logException("Error loading bundles xml", exc);
            }
        }
        List<SavedBundle> subset = new ArrayList<SavedBundle>();

        for (SavedBundle savedBundle : bundlesFromXml) {
            if ((type == BUNDLES_ALL) || (savedBundle.getType() == type)) {
                subset.add(savedBundle);
            }
        }
        return subset;
    }


    /**
     * Create, if needed, and return the list of {@link SavedBundle}s
     *
     * @return List of saved bundles
     */
    public List<SavedBundle> getFavorites() {
        List<SavedBundle> allBundles = getLocalBundles();
        allBundles.addAll(getXmlBundles(BUNDLES_FAVORITES));
        return allBundles;
    }



    /**
     * Get the last xidv filename for doing saves/saveas.
     *
     * @return The last xidv file name
     */
    public String getCurrentFileName() {
        return currentFileName;
    }


    /**
     * Set the last xidv filename for doing saves/saveas.
     *
     * @param f The filename
     */
    public void setCurrentFileName(String f) {
        currentFileName = f;
    }



    /**
     * Have the user select an xidv filename and
     * write the current application state to it.
     * This also sets the current file name and
     * adds the file to the history list.
     */
    public void doSaveAs() {
        String filename =
            FileManager.getWriteFile(getArgsManager().getBundleFileFilters(),
                                     null, getFileAccessory());
        if (filename == null) {
            return;
        }
        setCurrentFileName(filename);

        boolean prevMakeDataEditable = makeDataEditable;
        makeDataEditable = makeDataEditableCbx.isSelected();

        boolean prevMakeDataRelative = makeDataRelative;
        makeDataRelative = makeDataRelativeCbx.isSelected();
        if (doSave(filename)) {
            getPublishManager().publishContent(filename, null, publishCbx);
            getIdv().addToHistoryList(filename);
        }
        makeDataEditable = prevMakeDataEditable;
        makeDataRelative = prevMakeDataRelative;

    }



    /**
     * This will add in to the given combo box the
     * categories (Really the subdir names) under the give topDir.
     *
     * @param catBox Box to fill
     * @param defaultCategories List of categories to add by default
     * @param topDir The directory to look at
     */
    private void addBundleCategories(JComboBox catBox,
                                     List defaultCategories, String topDir) {
        catBox.removeAllItems();
        List subdirs = IOUtil.getDirectories(new File(topDir), true);
        for (int i = 0; i < defaultCategories.size(); i++) {
            String defaultCategory = (String) defaultCategories.get(i);
            catBox.addItem(defaultCategory);
            if (i == 0) {
                catBox.setSelectedItem(defaultCategory);
            }
        }


        for (int i = 0; i < subdirs.size(); i++) {
            File   subDir   = (File) (File) subdirs.get(i);
            String fullPath = subDir.toString();
            String dirName  = fullPath.substring(topDir.length() + 1);
            String thisCategory =
                categoriesToString(StringUtil.split(dirName, File.separator,
                    true, true));
            if ( !defaultCategories.contains(thisCategory)) {
                catBox.addItem(thisCategory);
            }
        }
    }


    /**
     * Move the bundle category
     *
     * @param fromCategories The category to move
     * @param toCategories Where to move to
     * @param bundleType What type are we dealing with
     */
    public void moveCategory(List fromCategories, List toCategories,
                             int bundleType) {
        File fromFile =
            new File(IOUtil.joinDir(getBundleDirectory(bundleType),
                                    StringUtil.join(File.separator + "",
                                        fromCategories)));

        String tail = IOUtil.getFileTail(fromFile.toString());
        toCategories.add(tail);
        File toFile = new File(IOUtil.joinDir(getBundleDirectory(bundleType),
                          StringUtil.join(File.separator + "",
                                          toCategories)));

        if (toFile.exists()) {
            LogUtil.userMessage(
                "The destination category already contains a category with name: "
                + tail);
            return;
        }

        if ( !fromFile.renameTo(toFile)) {
            LogUtil.userMessage(
                "There was some problem moving the given bundle category");
        }
        flushState(bundleType);
    }


    /**
     * Export the bundle
     *
     * @param bundle The bundle
     * @param bundleType What type
     */
    public void export(SavedBundle bundle, int bundleType) {
        String filename =
            FileManager.getWriteFile(getArgsManager().getXidvFileFilter(),
                                     null);
        if (filename == null) {
            return;
        }
        try {
            IOUtil.copyFile(new File(bundle.getUrl()), new File(filename));
        } catch (Exception exc) {
            logException("Exporting a bundle", exc);
        }
    }




    /**
     * Rename the bundle
     *
     * @param bundle The bundle
     * @param bundleType What type
     */
    public void rename(SavedBundle bundle, int bundleType) {
        String ext = IOUtil.getFileExtension(bundle.getUrl());
        String filename =
            IOUtil.stripExtension(IOUtil.getFileTail(bundle.getUrl()));
        while (true) {
            filename = GuiUtils.getInput("Enter a new name", "Name: ",
                                         filename);
            if (filename == null) {
                return;
            }
            filename = IOUtil.cleanFileName(filename).trim();
            if (filename.length() == 0) {
                return;
            }
            File newFile =
                new File(IOUtil.joinDir(IOUtil.getFileRoot(bundle.getUrl()),
                                        filename + ext));
            //            System.err.println(newFile);

            if (newFile.exists()) {
                LogUtil.userMessage("A file with the name: " + filename
                                    + " already exists");
            } else {
                File oldFile = new File(bundle.getUrl());
                oldFile.renameTo(newFile);
                flushState(bundleType);
                return;
            }
        }
    }




    /**
     * Copy the bundle to the given category area
     *
     * @param bundle The bundle
     * @param categories The category location
     * @param bundleType What type
     */
    public void copyBundle(SavedBundle bundle, List categories,
                           int bundleType) {
        moveOrCopyBundle(bundle, categories, bundleType, false);
    }


    /**
     * Move the bundle to the given category area
     *
     * @param bundle The bundle
     * @param categories The category location
     * @param bundleType What type
     */
    public void moveBundle(SavedBundle bundle, List categories,
                           int bundleType) {
        moveOrCopyBundle(bundle, categories, bundleType, true);
    }


    /**
     * Move or copy the bundle to the given category area, depending on the given argument.
     *
     * @param bundle The bundle
     * @param categories The category location
     * @param bundleType What type
     * @param move Move or copy
     */
    public void moveOrCopyBundle(SavedBundle bundle, List categories,
                                 int bundleType, boolean move) {

        File   fromFile = new File(bundle.getUrl());
        String tail     = IOUtil.getFileTail(bundle.getUrl());
        categories.add(tail);
        File toFile = new File(IOUtil.joinDir(getBundleDirectory(bundleType),
                          StringUtil.join(File.separator + "", categories)));


        if (toFile.exists()) {
            LogUtil.userMessage(
                "The destination category already contains a bundle with name: "
                + tail);
            return;
        }
        if (move) {
            fromFile.renameTo(toFile);
        } else {
            try {
                IOUtil.moveFile(fromFile, toFile);
            } catch (Exception exc) {
                logException("Moving a bundle", exc);
            }
        }
        flushState(bundleType);
    }

    /**
     * make category widget
     *
     * @return box
     */
    public JComboBox makeCategoryBox() {
        JComboBox catBox = new JComboBox();
        catBox.setToolTipText(
            "<html>Categories can be entered manually. <br>Use '>' as the category delimiter. e.g.:<br>General > Subcategory</html>");
        catBox.setEditable(true);
        return catBox;
    }


    /**
     * Have the user select an xidv filename for their favorites
     *
     *
     * @param title The title to use in the dialog
     * @param filename Default filename to show in the gui
     * @param bundles List of bundles
     * @param topDir Where to start looking
     * @param defaultCategories List of categories to add to the menu by default
     * @param suffix The file suffix we add on
     * @param showSubsetPanel If true then show the "What to save" panel
     *
     * @return Full path to the selected file.
     */
    private String getCategorizedFile(String title, String filename,
                                      List<SavedBundle> bundles,
                                      final String topDir,
                                      List defaultCategories, String suffix,
                                      boolean showSubsetPanel) {

        if (filename == null) {
            filename = "";
        }

        final JComboBox catBox = makeCategoryBox();

        JCheckBox zidvCbx = new JCheckBox("Save as zipped data bundle",
                                          false);
        zidvCbx.setToolTipText(
            "Select this to save the data along with the bundle");
        JComponent zidvComp = ((suffix == null)
                               ? (JComponent) zidvCbx
                               : (JComponent) new JPanel());

        addBundleCategories(catBox, defaultCategories, topDir);
        catSelected = false;
        catBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                catSelected = true;
            }
        });
        final JComboBox fileBox = new JComboBox();
        fileBox.setEditable(true);
        fileBox.setPreferredSize(new Dimension(150, 20));
        List tails = new ArrayList();

        if (bundles != null) {
            for (int i = 0; i < bundles.size(); i++) {
                SavedBundle bundle = (SavedBundle) bundles.get(i);
                if (new File(bundle.getUrl()).canWrite()) {
                    String tail = IOUtil.stripExtension(
                                      IOUtil.getFileTail(bundle.getUrl()));
                    //fileBox.addItem(new TwoFacedObject(tail, bundle));
                    tails.add(new TwoFacedObject(tail, bundle));
                }
            }
            java.util.Collections.sort(tails);

        }
        tails.add(0, filename);
        GuiUtils.setListData(fileBox, tails);
        fileBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Object selected = fileBox.getSelectedItem();
                if ( !(selected instanceof TwoFacedObject)) {
                    return;
                }
                TwoFacedObject tfo = (TwoFacedObject) selected;
                List cats = ((SavedBundle) tfo.getId()).getCategories();
                if ((cats.size() > 0) && !catSelected) {
                    catBox.setSelectedItem(
                        StringUtil.join(CATEGORY_SEPARATOR, cats));
                }
            }
        });




        GuiUtils.tmpInsets = new Insets(0, 3, 0, 3);
        JPanel catPanel = GuiUtils.left(catBox);

        GuiUtils.tmpInsets = new Insets(3, 3, 3, 3);
        JPanel panel = GuiUtils.doLayout(new Component[] {
                           GuiUtils.rLabel("Category: "),
                           catPanel, GuiUtils.rLabel("Name: "), fileBox }, 4,
                               GuiUtils.WT_NY, GuiUtils.WT_N);




        //Do we add in the file accessory subset panel
        if (showSubsetPanel) {
            JComponent extra = getFileAccessory();
            zidvComp = GuiUtils.inset(zidvComp, new Insets(0, 0, 0, 0));
            panel    = GuiUtils.vbox(panel, GuiUtils.vbox(extra, zidvComp));
        } else {
            panel = GuiUtils.vbox(panel, zidvComp);
        }

        while (true) {
            if ( !GuiUtils.askOkCancel(title, panel)) {
                return null;
            }
            filename = fileBox.getSelectedItem().toString().trim();
            filename = IOUtil.cleanFileName(filename);
            if (filename.length() == 0) {
                LogUtil.userMessage("Please enter a name");
                continue;
            }
            String defaultCategory =
                catBox.getSelectedItem().toString().trim();
            defaultCategory = IOUtil.cleanFileName(defaultCategory);
            if (defaultCategory.length() == 0) {
                LogUtil.userMessage("Please enter a category");
                continue;
            }
            String category = StringUtil.join(File.separator,
                                  stringToCategories(defaultCategory));
            File catDir = new File(IOUtil.joinDir(topDir, category));
            if ( !catDir.exists()) {
                catDir.mkdirs();
            }
            String tmpSuffix = suffix;
            if (suffix == null) {
                if (zidvCbx.isSelected()) {
                    tmpSuffix =
                        getArgsManager().getZidvFileFilter()
                            .getPreferredSuffix();
                } else {
                    tmpSuffix =
                        getArgsManager().getXidvFileFilter()
                            .getPreferredSuffix();
                }
            }

            File fullFile = new File(IOUtil.joinDir(catDir.toString(),
                                filename + tmpSuffix));
            if (fullFile.exists()) {
                int result =
                    GuiUtils.showYesNoCancelDialog(null,
                        "The file: " + filename
                        + " already exists. Do you want to overwite it? ", "File exists");
                //0->yes,1->no,2->cancel
                if (result == 2) {
                    return null;
                }
                if (result == 1) {
                    continue;
                }
            }
            return fullFile.toString();
        }

    }



    /**
     * Import the given file as the given bundle type
     *
     * @param bundleType What type
     * @param file The file
     * @param category The category
     */
    public void doImport(int bundleType, String file, String category) {
        String path = StringUtil.join(File.separator,
                                      stringToCategories(category));
        String dir      = IOUtil.joinDir(getBundleDirectory(bundleType),
                                         path);
        String filename = IOUtil.getFileTail(file);
        IOUtil.makeDir(dir);
        File dest = new File(IOUtil.joinDir(dir, filename));
        if (dest.exists()) {
            if (JOptionPane.showConfirmDialog(null,
                    "File:" + filename
                    + " exists. Do you want to overwrite?", "File exists",
                        JOptionPane.YES_NO_OPTION) == 1) {
                return;
            }
        }

        try {
            IOUtil.copyFile(new File(file), dest);
        } catch (Throwable e) {
            logException("Importing bundle", e);
        }
        flushState(bundleType);
    }


    /**
     * get a list of the categories defined by the given bundle type
     *
     * @param bundleType bundle type
     * @param cats initial list
     *
     * @return all categories including those in initial list
     */
    private List getCategories(int bundleType, List cats) {
        List favs = getBundles(bundleType);
        for (int i = 0; i < favs.size(); i++) {
            SavedBundle bundle    = (SavedBundle) favs.get(i);
            String      bundleCat =
                categoriesToString(bundle.getCategories());
            if ( !cats.contains(bundleCat)) {
                cats.add(bundleCat);
            }
        }
        return cats;
    }




    /**
     * get categories
     *
     * @return categories
     */
    public List getFavoritesCategories() {
        return getCategories(BUNDLES_FAVORITES,
                             Misc.newList(CAT_GENERAL, CAT_TOOLBAR));
    }

    /**
     * Have the user select an xidv filename for their favorites
     */
    public void doSaveAsFavorite() {
        List cats = getFavoritesCategories();
        String fullFile = getCategorizedFile("Save As Favorite", "",
                                             getLocalBundles(),
                                             getStore().getLocalBundlesDir(),
                                             cats, null, true);
        if (fullFile == null) {
            return;
        }
        doSave(fullFile);
        //Just call this since this will update the display menu, etc.
        getIdvUIManager().displayTemplatesChanged();
        QuicklinkPanel.updateQuicklinks();
    }


    /**
     * Save the current state off to the current xidv filename
     */
    public void doSave() {
        String filename = getCurrentFileName();
        doSave(filename);
        getIdv().addToHistoryList(filename);
    }



    /**
     *  Called from the menu command to save the current state as the default bundle
     */
    public void doSaveAsDefault() {
        //TODO: Put call out to the persistence manager to configure
        //what is to be saved.
        doSave(getResourceManager().getResources(
            IdvResourceManager.RSC_BUNDLES).getWritable(), false);
    }


    /**
     *  Called from the menu command to open the default bundle
     */
    public void doOpenDefault() {

        //Get the name of the default bundle file.
        //NOTE: We are assuming that it is a local file.
        String fileName = getResourceManager().getResources(
                              IdvResourceManager.RSC_BUNDLES).getWritable();

        //test if the file exists
        File file = new File(fileName);
        if ( !file.exists()) {
            LogUtil.userMessage("The default bundle: " + fileName
                                + " does not exist.");
            return;
        }

        decodeXmlFile(fileName, true);
    }

    /**
     *  Save the current state into the specified filename
     *
     * @param filename bundle file name to write to. If this ends in
     * &quot;.jnlp&quot; then we base 64 encode the bundle and wrap it in
     * a jnlp file.
     *
     *
     * @return Was this save successful
     */
    public boolean doSave(String filename) {
        return doSave(filename, true);
    }


    /**
     * A utility to create  a jnlp file from the given bundle
     *
     * @param xml The bundle xml
     *
     * @return The jnlp file xml
     */
    public String getJnlpBundle(String xml) {
        return getJnlpBundle(xml, true, null);
    }

    /**
     * A utility to create  a jnlp file from the given bundle
     *
     * @param xml The bundle xml
     * @param embedBundle Embed the b64 encoded bundle right in the jnlp
     * @param extraArgs extra jnlp args
     *
     * @return The jnlp file xml
     */
    public String getJnlpBundle(String xml, boolean embedBundle,
                                String extraArgs) {
        String templateFile = getProperty(PROP_JNLPTEMPLATE, "no template");
        String template = IOUtil.readContents(templateFile, getClass(),
                              NULL_STRING);
        String codeBase = getProperty(PROP_JNLPCODEBASE, NULL_STRING);
        String title    = getProperty(PROP_JNLPTITLE, "");

        if (template == null) {
            LogUtil.userErrorMessage(log_,
                                     "Failed to read jnlp template file: "
                                     + templateFile);
            return null;
        }

        if (codeBase == null) {
            LogUtil.userErrorMessage(log_, "Failed to read jnlp codebase");
            return null;
        }

        //Do we want to include all of the cmd line arguments?
        StringBuffer args = new StringBuffer("");
        for (int i = 0; i < getArgsManager().persistentCommandLineArgs.size();
                i++) {
            args.append("<argument>"
                        + getArgsManager().persistentCommandLineArgs.get(i)
                        + "</argument>\n");
        }


        String jnlp = template;
        //jnlp = StringUtil.replace(xml, "%DATA%", b64Xml);
        if (embedBundle) {
            String b64Xml = new String(XmlUtil.encodeBase64(xml.getBytes()));
            args.append("<argument>-b64bundle</argument>\n");
            args.append("<argument>");
            args.append(b64Xml);
            args.append("</argument>\n");
        }
        if (extraArgs != null) {
            args.append(extraArgs);
        }

        jnlp = StringUtil.replace(jnlp, "%CODEBASE%", codeBase);
        jnlp = StringUtil.replace(jnlp, "%TITLE%", title);
        jnlp = StringUtil.replace(jnlp, "%ARGS%", args.toString());
        jnlp = StringUtil.replace(jnlp, "%IDVCLASS%",
                                  getIdv().getClass().getName());
        jnlp = StringUtil.replace(jnlp, "%DESCRIPTION%", "");

        return jnlp;
    }


    /**
     *  Save the current state into the specified filename
     *
     * @param filename bundle file name to write to. If this ends in
     * &quot;.jnlp&quot; then we base 64 encode the bundle and wrap it in
     * a jnlp file.
     *
     * @param usePersistenceManager If true then we use the persistence manager
     * to determine what is to be saved.
     *
     * @return Was this save successful
     */
    public boolean doSave(String filename, boolean usePersistenceManager) {

        try {
            boolean doJnlp = filename.endsWith(SUFFIX_JNLP)
                             || filename.endsWith(SUFFIX_SH)
                             || filename.endsWith(SUFFIX_BAT);


            boolean doIsl     = getArgsManager().isIslFile(filename);
            boolean doZidv    = getArgsManager().isZidvFile(filename);

            List    zidvFiles = null;
            if (doZidv) {
                zidvFiles = showDataEmbedGui(getDataSourcesToPersist());
                if (zidvFiles == null) {
                    return false;
                }
            }

            //The !doJnlp says to create the xml without the extra spacing, etc.
            String xml = getBundleXml( !doJnlp, usePersistenceManager);
            if (xml == null) {
                clearDataSourcesState();
                return false;
            }

            //If we are writing out a jnlp (webstart) file then read the template
            //and replace the %DATA% and other macros with the base64 encoded xml
            //and other state
            if (doJnlp) {
                String shellFile = null;
                if (filename.endsWith(SUFFIX_SH)
                        || filename.endsWith(SUFFIX_BAT)) {
                    shellFile = filename;
                    filename  = IOUtil.stripExtension(filename) + SUFFIX_JNLP;
                }
                if (includeBundleCbx == null) {
                    includeBundleCbx =
                        new JCheckBox("Include Bundle in JNLP File", true);
                    includeBundleCbx.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            GuiUtils.enableTree(bundleUrlComp,
                                    !includeBundleCbx.isSelected());
                        }

                    });
                    bundlePrefixFld = new JTextField("", 40);
                    bundleUrlComp = GuiUtils
                        .vbox(new JLabel(
                            "Web URL directory where bundle will be: "), GuiUtils
                                .leftCenter(
                                    new JLabel("       "), bundlePrefixFld));
                    GuiUtils.enableTree(bundleUrlComp, false);
                }

                JComponent bundlePanel = GuiUtils.vbox(includeBundleCbx,
                                             new JLabel(" "), bundleUrlComp);

                if ( !GuiUtils.askOkCancel("Saving JNLP File", bundlePanel)) {
                    return false;
                }

                boolean embedBundle = includeBundleCbx.isSelected();
                String  bundleXml   = xml;
                String  bundleArg   = null;
                String bundleFile =
                    IOUtil.stripExtension(filename)
                    + getArgsManager().getXidvFileFilter()
                        .getPreferredSuffix();

                if ( !embedBundle) {
                    String bundlePath = bundlePrefixFld.getText().trim();
                    if ((bundlePath.length() > 0)
                            && !bundlePath.endsWith("/")) {
                        bundlePath = bundlePath + "/";
                    }
                    bundlePath = bundlePath + IOUtil.getFileTail(bundleFile);
                    bundleArg  = "<argument>" + bundlePath + "</argument>";
                }
                xml = getJnlpBundle(xml, embedBundle, ( !embedBundle
                        ? bundleArg
                        : null));
                if ( !embedBundle) {
                    IOUtil.writeFile(bundleFile, bundleXml);
                }
                if (xml == null) {
                    return false;
                }
                if (shellFile != null) {
                    if (shellFile.endsWith(SUFFIX_SH)) {
                        String shContent = "runidv.sh "
                                           + IOUtil.getFileTail(filename);
                        IOUtil.writeFile(shellFile, shContent);
                    } else if (filename.endsWith(SUFFIX_BAT)) {
                        String shContent = "runidv.bat "
                                           + IOUtil.getFileTail(filename);
                        IOUtil.writeFile(shellFile, shContent);
                    }
                }
            }

            if (doZidv) {
                GuiUtils.ProgressDialog dialog =
                    new GuiUtils.ProgressDialog("Creating Zipped Bundle",
                        true);
                dialog.setText("Writing " + filename);
                String tail =
                    IOUtil.stripExtension(IOUtil.getFileTail(filename));
                ZipOutputStream zos =
                    new ZipOutputStream(new FileOutputStream(filename));
                String fileSuffix =
                    getArgsManager().getXidvFileFilter().getPreferredSuffix();
                zos.putNextEntry(new ZipEntry(tail + fileSuffix));
                byte[] bytes = xml.getBytes();
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();

                for (int i = 0; i < zidvFiles.size(); i++) {
                    String   file     = (String) zidvFiles.get(i);
                    ZipEntry zipEntry =
                        new ZipEntry(IOUtil.getFileTail(file));
                    dialog.setText("Writing " + zipEntry.getName());
                    zos.putNextEntry(zipEntry);
                    IOUtil.writeTo(IOUtil.getInputStream(file, getClass()),
                                   zos);
                    zos.closeEntry();
                    if (dialog.isCancelled()) {
                        dialog.dispose();
                        zos.close();
                        return false;
                    }
                }
                dialog.dispose();
                zos.close();
                return true;
            }

            if (doIsl) {
                if (islDialog == null) {
                    islDialog = new IslDialog(this);
                }
                islDialog.writeIsl(filename, xml);
            } else {
                IOUtil.writeFile(filename, xml);
            }
        } catch (Throwable e) {
            logException("doSave", e);
            return false;
        }
        return true;

    }





    /**
     * This creates an xml encoded hashtable that contains the application state.
     *
     * @param formatXml Should the result be formatted (i.e., new lines and indents
     * for easy reading).
     * @return The xml encoded application state
     * @throws Exception
     */
    public String getBundleXml(boolean formatXml) throws Exception {
        return getBundleXml(formatXml, true);
    }


    /**
     * Clear all temp state from the data sources
     */
    private void clearDataSourcesState() {
        clearDataSourcesState(getDataSourcesToPersist());
    }

    /**
     * Clear all temp state from the data sources
     *
     * @param dataSources data sources to clear
     */
    private void clearDataSourcesState(List dataSources) {
        for (int dataSourceIdx = 0; dataSourceIdx < dataSources.size();
                dataSourceIdx++) {
            DataSource dataSource =
                (DataSource) dataSources.get(dataSourceIdx);
            dataSource.resetTmpState();
        }
    }


    /**
     * This creates an xml encoded hashtable that contains the application state.
     *
     * @param formatXml Should the result be formatted (i.e., new lines and indents
     * for easy reading).
     * @param usePersistenceManager If true then use the persistence manager to decide what is saved
     *
     * @return The xml encoded application state
     * @throws Exception
     */
    public String getBundleXml(boolean formatXml,
                               boolean usePersistenceManager)
            throws Exception {
        XmlEncoder encoder = getIdv().getEncoderForWrite();
        Hashtable  data    = new Hashtable();
        if ( !addToBundle(data, usePersistenceManager,
                          getIdv().getDisplayControls())) {
            clearDataSourcesState();
            return null;
        }
        String xml = encoder.toXml(data, formatXml);
        clearDataSourcesState();
        return xml;

    }


    /**
     * Create the DisplayControl from the bundle in the given templateFile
     *
     * @param templateFile File path to the template
     *
     * @return The instantiated DisplayControl
     */
    public DisplayControl instantiateFromTemplate(String templateFile) {
        try {
            String xml = IOUtil.readContents(templateFile);
            return (DisplayControl) getIdv().getEncoderForRead().toObject(
                xml);
        } catch (Throwable exc) {
            logException("Unable to load template:" + templateFile, exc);
            return null;
        }
    }



    /**
     * Get a list of all of the categories for the given bundleType
     *
     * @param bundleType What type of bundle (e.g., favorites)
     *
     * @return List of (String) categories
     */
    public List getAllCategories(int bundleType) {
        String bundleDir     = getBundleDirectory(bundleType);
        List   directories = IOUtil.getDirectories(new File(bundleDir), true);
        List   allCategories = new ArrayList();

        for (int i = 0; i < directories.size(); i++) {
            allCategories.add(categoriesToString(fileToCategories(bundleDir,
                    ((File) directories.get(i)).toString())));
        }


        return allCategories;
    }


    /**
     * Get the title to use for the given bundle type
     *
     * @param bundleType The type of bundle (e.g., favorites)
     *
     * @return The title
     */
    public String getBundleTitle(int bundleType) {
        if (bundleType == BUNDLES_FAVORITES) {
            return "Favorite Bundles";
        }
        if (bundleType == BUNDLES_DISPLAY) {
            return "Display Templates";
        }

        if (bundleType == BUNDLES_DATA) {
            return "Favorite Data Sources";
        }
        throw new IllegalArgumentException("Unknown bundle type:"
                                           + bundleType);
    }



    /**
     * Get the directory that holds the given bundle type
     *
     * @param bundleType The type of bundle (e.g., favorites)
     *
     * @return The directory
     */
    public String getBundleDirectory(int bundleType) {
        if (bundleType == BUNDLES_FAVORITES) {
            return getStore().getLocalBundlesDir();
        }
        if (bundleType == BUNDLES_DISPLAY) {
            return getStore().getDisplayTemplateDir();
        }
        if (bundleType == BUNDLES_DATA) {
            return getStore().getDataSourcesDir();
        }
        throw new IllegalArgumentException("Unknown bundle type:"
                                           + bundleType);
    }


    /**
     * Get the list of {@link SavedBundle}s
     *
     * @param bundleType The type of bundle (e.g., favorites)
     *
     * @return List of bundles
     */
    public List<SavedBundle> getBundles(int bundleType) {
        if (bundleType == BUNDLES_FAVORITES) {
            return getFavorites();
        }
        if (bundleType == BUNDLES_DISPLAY) {
            return getDisplayTemplates();
        }
        if (bundleType == BUNDLES_DATA) {
            return getDataSourceBundles();
        }
        throw new IllegalArgumentException("Unknown bundle type:"
                                           + bundleType);
    }


    /**
     * Initialize the bundle menu
     *
     * @param bundleType The type of bundle (e.g., favorites)
     * @param bundleMenu The menu
     *
     */
    public void initBundleMenu(int bundleType, JMenu bundleMenu) {
        if (bundleType == BUNDLES_FAVORITES) {
            JMenuItem mi = new JMenuItem("Save As Favorite...");
            mi.setMnemonic(GuiUtils.charToKeyCode("S"));
            GuiUtils.setIcon(mi, "/auxdata/ui/icons/disk_multiple.png");
            bundleMenu.add(mi);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    doSaveAsFavorite();
                }
            });
        }
    }



    /**
     * Get the list of {@link SavedBundle}s that are writable
     *
     * @param bundleType The type of bundle (e.g., favorites)
     *
     * @return List of writable bundles
     */
    public List getWritableBundles(int bundleType) {
        List allBundles = getBundles(bundleType);
        List bundles    = new ArrayList();
        for (int i = 0; i < allBundles.size(); i++) {
            SavedBundle bundle = (SavedBundle) allBundles.get(i);
            if ( !new File(bundle.getUrl()).canWrite()) {
                continue;
            }
            bundles.add(bundle);
        }
        return bundles;

    }


    /**
     *
     * @param dataSource data source to save
     */
    public void saveDataSource(DataSource dataSource) {
        List cats = getCategories(BUNDLES_DATA, Misc.newList(CAT_GENERAL));
        String fullFile =
            getCategorizedFile("Save Data Source", dataSource.toString(),
                               getBundles(BUNDLES_DATA),
                               getBundleDirectory(BUNDLES_DATA), cats,
                               ".xidv", false);
        if (fullFile == null) {
            return;
        }
        String xml = encodeSpecial(dataSource);
        try {
            IOUtil.writeFile(fullFile, xml);
            dataSourceBundles = null;
            QuicklinkPanel.updateQuicklinks();
        } catch (Exception exc) {
            logException("Saving data source bundle", exc);
        }
    }




    /**
     * Prompt the user for a name and write out the given display control
     * as a bundle into the user's .unidata/idv/displaytemplates directory.
     *
     * @param displayControl The display control to write
     * @param templateName Possibly null initial name for the template
     */
    public void saveDisplayControlFavorite(DisplayControl displayControl,
                                           String templateName) {
        List cats = getCategories(BUNDLES_DISPLAY, Misc.newList(CAT_GENERAL));
        String fullFile =
            getCategorizedFile("Save Display Template", templateName,
                               getBundles(BUNDLES_DISPLAY),
                               getBundleDirectory(BUNDLES_DISPLAY), cats,
                               ".xidv", false);
        if (fullFile == null) {
            return;
        }
        saveDisplayControl(displayControl, new File(fullFile));
    }


    /**
     * Write out the given  display control
     *
     * @param displayControl The display control to write
     */
    public void saveDisplayControl(DisplayControl displayControl) {
        String filename =
            FileManager.getWriteFile(getArgsManager().getXidvFileFilter(),
                                     null);
        if (filename == null) {
            return;
        }
        saveDisplayControl(displayControl, new File(filename));

    }




    /**
     * A *SPECIAL* encoding.
     *
     * @param object  the object to encode
     *
     * @return the encoded string
     */
    private String encodeSpecial(Object object) {
        try {
            XmlEncoder encoder = getIdv().getEncoderForWrite();
            //Temporarily turn off the view and data flags here
            boolean tmpSaveViewState   = getSaveViewState();
            boolean tmpSaveDataSources = getSaveDataSources();
            boolean tmpSaveJython      = getSaveJython();
            boolean tmpSaveData        = getSaveData();
            saveViewState   = false;
            saveDataSources = false;
            saveJython      = false;
            saveData        = false;
            String xml = encoder.toXml(object);
            saveViewState   = tmpSaveViewState;
            saveDataSources = tmpSaveDataSources;
            saveJython      = tmpSaveJython;
            saveData        = tmpSaveData;
            return xml;
        } catch (Exception exc) {
            logException("Saving display template", exc);
            return null;
        }
    }

    /**
     * Write out the given  display control to te given file
     *
     * @param displayControl The display control to write
     * @param file The file to write to
     */
    public void saveDisplayControl(DisplayControl displayControl, File file) {
        try {
            //Set the currentTemplateName as a way (a bit of a hack) for the display control
            //to have saved with it its template name
            String name = file.getName();
            currentTemplateName = IOUtil.stripExtension(name);
            String xml = encodeSpecial(displayControl);
            IOUtil.writeFile(file, xml);
            currentTemplateName = name;
            flushState(BUNDLES_DISPLAY);
        } catch (Exception exc) {
            logException("Saving display template", exc);
        }
    }




    /**
     * Create, if needed, and return the list of locally saved {@link SavedBundle}s
     *
     * @return List of saved bundles
     */
    public List<SavedBundle> getLocalBundles() {
        List<SavedBundle> allBundles = new ArrayList<SavedBundle>();
        List bundleDirs = Misc.newList(getStore().getLocalBundlesDir());
        String            sitePath   = getResourceManager().getSitePath();

        //If we have a site path then add the bundles subdir
        if (sitePath != null) {
            bundleDirs.add(IOUtil.joinDir(sitePath,
                                          IdvObjectStore.DIR_BUNDLES));
        }

        //Go through each top level directory that can contain bundles
        for (int i = 0; i < bundleDirs.size(); i++) {
            String topDir = (String) bundleDirs.get(i);
            //Find all  subdirs under the top dir
            List subdirs = IOUtil.getDirectories(Misc.newList(topDir), true);
            for (int subDirIdx = 0; subDirIdx < subdirs.size(); subDirIdx++) {
                File subdir = (File) subdirs.get(subDirIdx);
                loadBundlesInDirectory(allBundles,
                                       fileToCategories(topDir,
                                           subdir.getPath()), subdir);
            }
        }
        return allBundles;
    }


    /**
     * Get list of  data soruce bundles
     *
     * @return data source bundle list
     */
    public List<SavedBundle> getDataSourceBundles() {
        if (dataSourceBundles == null) {
            dataSourceBundles = new ArrayList<SavedBundle>();
            String topDir = getBundleDirectory(BUNDLES_DATA);
            List   dirs   = IOUtil.getDirectories(new File(topDir), true);
            for (int dirIdx = 0; dirIdx < dirs.size(); dirIdx++) {
                File file = (File) dirs.get(dirIdx);
                String[] templateFiles =
                    file.list(getArgsManager().getXidvFileFilter());
                for (int i = 0; i < templateFiles.length; i++) {
                    String filename = IOUtil.joinDir(file.toString(),
                                          templateFiles[i]);
                    List categories = fileToCategories(topDir,
                                          file.toString());
                    String name = IOUtil.stripExtension(templateFiles[i]);
                    dataSourceBundles.add(new SavedBundle(filename, name,
                            categories, null, true, SavedBundle.TYPE_DATA));
                }
            }

            dataSourceBundles.addAll(getXmlBundles(BUNDLES_DATA));
        }
        return dataSourceBundles;
    }


    /**
     * Get the list of display templates. This  is a list of String file paths
     *
     * @return List of display template
     */
    public List<SavedBundle> getDisplayTemplates() {
        if (displayTemplates == null) {
            displayTemplates = new ArrayList();
            String topDir = getBundleDirectory(BUNDLES_DISPLAY);
            List   dirs   = IOUtil.getDirectories(new File(topDir), true);
            for (int dirIdx = 0; dirIdx < dirs.size(); dirIdx++) {
                File file = (File) dirs.get(dirIdx);
                String[] templateFiles =
                    file.list(getArgsManager().getXidvFileFilter());
                for (int i = 0; i < templateFiles.length; i++) {
                    String filename = IOUtil.joinDir(file.toString(),
                                          templateFiles[i]);
                    DisplayControl dc = instantiateFromTemplate(filename);
                    if (dc == null) {
                        continue;
                    }
                    List categories = fileToCategories(topDir,
                                          file.toString());
                    String name = IOUtil.stripExtension(templateFiles[i]);
                    displayTemplates.add(new SavedBundle(filename, name,
                            categories, dc, true, SavedBundle.TYPE_DISPLAY));
                }
            }
            displayTemplates.addAll(getXmlBundles(BUNDLES_DISPLAY));
        }
        return displayTemplates;
    }


    /**
     * Open the given bundle
     *
     * @param bundle The bundle to open
     *
     * @return success
     */
    public boolean open(SavedBundle bundle) {
        return open(bundle, true);
    }

    /**
     * Open the given bundle
     *
     * @param bundle bundle
     * @param askToRemove ask
     *
     * @return success
     */
    public boolean open(SavedBundle bundle, boolean askToRemove) {
        return decodeXmlFile(bundle.getUrl(), bundle.getName(), askToRemove);
    }


    /**
     * Clear out the cache of template file names and prototypes
     *
     * @param type bundle type
     */
    private void flushState(int type) {
        if ((type == BUNDLES_DISPLAY) || (type == BUNDLES_ALL)) {
            displayTemplates = null;
            getIdvUIManager().displayTemplatesChanged();
        }
        if ((type == BUNDLES_DATA) || (type == BUNDLES_ALL)) {
            dataSourceBundles = null;
        }
        if ((type == BUNDLES_FAVORITES) || (type == BUNDLES_ALL)) {
            getIdvUIManager().favoriteBundlesChanged();
        }
        QuicklinkPanel.updateQuicklinks();
    }

    /**
     * Remove the given template
     *
     * @param templateFile The template file to remove
     */
    public void deleteBundle(String templateFile) {
        File file = new File(templateFile);
        file.delete();
        flushState(BUNDLES_ALL);
    }


    /**
     * Delete the directory and all of its contents
     * that the given category represents.
     *
     * @param bundleType The type of bundle (e.g., favorites)
     * @param category The category (really a directory path)
     */
    public void deleteBundleCategory(int bundleType, String category) {
        String path = StringUtil.join(File.separator,
                                      stringToCategories(category));
        path = IOUtil.joinDir(getBundleDirectory(bundleType), path);
        IOUtil.deleteDirectory(new File(path));
        flushState(bundleType);
    }



    /**
     * Add the directory
     *
     * @param bundleType The type of bundle (e.g., favorites)
     * @param category The category (really a directory path)
     * @return true if the create was successfull. False if there already is a category with that name
     */
    public boolean addBundleCategory(int bundleType, String category) {
        String path = StringUtil.join(File.separator,
                                      stringToCategories(category));
        File f = new File(IOUtil.joinDir(getBundleDirectory(bundleType),
                                         path));
        if (f.exists()) {
            return false;
        }
        IOUtil.makeDir(f);
        return true;
    }


    /**
     * Get the data sources we should persist
     *
     * @return Data sources to persist
     */
    protected List getDataSourcesToPersist() {
        List sources        = new ArrayList();
        List currentSources = getIdv().getDataSources();
        for (int i = 0; i < currentSources.size(); i++) {
            Object source = currentSources.get(i);
            if ( !DataManager.isFormulaDataSource(source)) {
                sources.add(source);
            }
        }
        return sources;
    }


    /**
     * This method adds into the given hashtable the data sources,
     * display controls, view managers and extra gui state for
     * later persistence.
     *
     * @param data The table to put things into
     * @param usePersistenceManager Should we use the settings of this manager
     * @param displayControls The display controls to add
     *
     *
     * @return If user is ok
     * @throws Exception When something bad happens
     */
    protected boolean addToBundle(Hashtable data,
                                  boolean usePersistenceManager,
                                  List displayControls)
            throws Exception {
        return addToBundle(data,
                           (( !usePersistenceManager || getSaveDataSources())
                            ? getDataSourcesToPersist()
                            : null), (( !usePersistenceManager
                                        || getSaveDisplays())
                                      ? displayControls
                                      : null), (( !usePersistenceManager
                                      || getSaveViewState())
                ? getVMManager().getViewManagers()
                : null), ((usePersistenceManager && getSaveJython())
                          ? getJythonManager().getUsersJythonText()
                          : null));
    }

    /**
     * Show gui to set the data relative flags
     *
     * @param dataSources data sources
     *
     * @return ok
     */
    private boolean showDataRelativeGui(List dataSources) {
        List checkBoxes = new ArrayList();
        List fields     = new ArrayList();
        List workingSet = new ArrayList();
        List comps      = new ArrayList();
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            List       files      = dataSource.getDataPaths();
            if ((files == null) || (files.size() == 0)) {
                continue;
            }
            if ( !new File(files.get(0).toString()).exists()) {
                continue;
            }

            workingSet.add(dataSource);
            JCheckBox cbx =
                new JCheckBox(DataSelector.getNameForDataSource(dataSource));
            checkBoxes.add(cbx);
            comps.add(cbx);
            List fileFields = new ArrayList();
            fields.add(fileFields);
            for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
                String file = files.get(fileIdx).toString();
                file = "%" + PROP_BUNDLEPATH + "%/"
                       + IOUtil.getFileTail(file);
                JTextField fld = new JTextField(file);
                fileFields.add(fld);
            }
            JComponent fileFldComp;
            if (fileFields.size() < 3) {
                fileFldComp = GuiUtils.vbox(fileFields);
            } else {
                fileFldComp =
                    GuiUtils.makeScrollPane(GuiUtils.vbox(fileFields), 200,
                                            100);
                fileFldComp.setPreferredSize(new Dimension(200, 100));
            }
            JLabel label = new JLabel((files.size() == 1)
                                      ? "File:  "
                                      : "Files:  ");

            //            comps.add(GuiUtils.inset(fileFldComp, new Insets(0, 20, 0, 0)));
            comps.add(GuiUtils.leftCenter(GuiUtils.top(label), fileFldComp));
        }
        if (checkBoxes.size() == 0) {
            return true;
        }
        JLabel label =
            GuiUtils.cLabel(
                "Select the data sources that should be saved with relative paths");
        JComponent panel = GuiUtils.vbox(GuiUtils.inset(label, 5),
                                         GuiUtils.vbox(comps));

        if ( !GuiUtils.askOkCancel("Make Data Sources Relative", panel)) {
            return false;
        }

        for (int i = 0; i < workingSet.size(); i++) {
            JCheckBox cbx = (JCheckBox) checkBoxes.get(i);
            if ( !cbx.isSelected()) {
                continue;
            }
            DataSource dataSource = (DataSource) workingSet.get(i);
            List       fileFields = (List) fields.get(i);
            //            List files = dataSource.getPathsThatCanBeRelative();
            List relativeFiles = new ArrayList();
            for (int fileIdx = 0; fileIdx < fileFields.size(); fileIdx++) {
                String file = (String) ((JTextField) fileFields.get(
                                  fileIdx)).getText().trim();
                if (file.length() > 0) {
                    relativeFiles.add(file);
                }
            }
            dataSource.setTmpPaths(relativeFiles);
        }
        return true;
    }



    /**
     * Class DataSourceComponent For showing save guis
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.177 $
     */
    private static class DataSourceComponent {

        /** data source */
        DataSource dataSource;

        /** cbx */
        JCheckBox cbx = new JCheckBox();

        /** files */
        List files;

        /**
         * ctor
         *
         * @param ds  datasource
         */
        public DataSourceComponent(DataSource ds) {
            dataSource = ds;
        }
    }


    /**
     * Show xidv gui
     *
     * @param dataSources data sources
     *
     * @return list of  files to embed. May be null.
     *
     * @throws IOException On badness
     */
    private List showDataEmbedGui(List dataSources) throws IOException {

        List      fileDataSources = new ArrayList();
        List      copyDataSources = new ArrayList();
        List      fileComps       = new ArrayList();
        List      copyComps       = new ArrayList();
        List      notSavedLabels  = new ArrayList();
        JCheckBox allCbx          = new JCheckBox("All", false);
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource          dataSource = (DataSource) dataSources.get(i);
            List                files      = dataSource.getDataPaths();
            DataSourceComponent dsc = new DataSourceComponent(dataSource);

            String dataSourceName =
                DataSelector.getNameForDataSource(dataSource);
            if (dataSource.canSaveDataToLocalDisk()) {
                copyDataSources.add(dsc);
                dsc.cbx.setText(dataSourceName);
                copyComps.add(dsc.cbx);
            } else {
                if ((files == null) || (files.size() == 0)) {
                    notSavedLabels.add(new JLabel(dataSourceName));
                    continue;
                }
                Object sampleFile = files.get(0);
                if (sampleFile.getClass().isArray()) {
                    sampleFile = ((Object[]) sampleFile)[0];
                }
                if ( !new File(sampleFile.toString()).exists()) {
                    notSavedLabels.add(new JLabel(dataSourceName));
                    continue;
                }
                fileDataSources.add(dsc);
                fileComps.add(dsc.cbx);
                fileComps.add(new JLabel(dataSourceName));
                long size = 0;
                for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
                    String file = files.get(fileIdx).toString();
                    File   f    = new File(file);
                    size += f.length();
                }
                fileComps.add(GuiUtils.filler());
                String sizeStr = (size / 1000) + " K bytes";
                fileComps.add(new JLabel(files.size() + ((files.size() == 1)
                        ? " File "
                        : " Files ") + sizeStr));
                fileComps.add(new JLabel(" "));
                fileComps.add(new JLabel(" "));
            }
        }

        if ((notSavedLabels.size() == 0) && (fileDataSources.size() == 0)
                && (copyDataSources.size() == 0)) {
            return new ArrayList();
        }

        List comps = new ArrayList();

        copyComps.addAll(fileComps);


        if (copyComps.size() > 0) {
            if (copyComps.size() > 1) {
                copyComps.add(0, allCbx);
            }
            copyComps.add(
                0, new JLabel("Select the data sources to include:"));
            if (copyComps.size() > 5) {
                JComponent sp = GuiUtils.makeScrollPane(
                                    GuiUtils.top(GuiUtils.vbox(copyComps)),
                                    300, 400);
                sp.setPreferredSize(new Dimension(300, 400));
                comps.add(sp);
            } else {
                comps.add(GuiUtils.vbox(copyComps));
            }
        }

        /*
        if (fileDataSources.size() > 0) {
            if (comps.size() > 0) {
                comps.add(new JLabel(" "));
            }
            comps.add(
                GuiUtils
                    .vbox(new JLabel(
                        "Local data to include:"), GuiUtils
                            .doLayout(
                                fileComps, 2, GuiUtils.WT_NY,
                                GuiUtils.WT_N)));
                                }*/

        if (notSavedLabels.size() > 0) {
            if ((copyComps.size() == 0) && (fileDataSources.size() == 0)) {
                comps.add(
                    new JLabel("No data will be included in the bundle"));
            }
            notSavedLabels.add(0, new JLabel("Other data sources:"));
            notSavedLabels.add(0, new JLabel(" "));
            comps.add(GuiUtils.vbox(notSavedLabels));
        }

        JComponent panel = GuiUtils.vbox(comps);
        if ( !GuiUtils.askOkCancel("Save Data", panel)) {
            return null;
        }

        File dir = getIdv().getObjectStore().getUniqueTmpDirectory();

        //TODO: change this so we use the files to set the tmpFiles on the DS
        for (int i = 0; i < copyDataSources.size(); i++) {
            DataSourceComponent dsc =
                (DataSourceComponent) copyDataSources.get(i);
            if (allCbx.isSelected() || dsc.cbx.isSelected()) {
                List files = dsc.dataSource.saveDataToLocalDisk(false,
                                 IOUtil.joinDir(dir, "data_" + i));
                if (files == null) {
                    return null;
                }
                dsc.files = files;
                fileDataSources.add(dsc);
            }
        }

        List filesToEmbed = new ArrayList();
        for (int i = 0; i < fileDataSources.size(); i++) {
            DataSourceComponent dsc =
                (DataSourceComponent) fileDataSources.get(i);
            if ( !allCbx.isSelected() && !dsc.cbx.isSelected()) {
                continue;
            }
            DataSource dataSource    = dsc.dataSource;
            List       files         = ((dsc.files != null)
                                        ? dsc.files
                                        : dataSource.getDataPaths());
            List       relativeFiles = new ArrayList();
            for (int fileIdx = 0; fileIdx < files.size(); fileIdx++) {
                Object o       = files.get(fileIdx);
                String file    = null;
                String newFile = null;
                if (o.getClass().isArray()) {
                    file    = ((Object[]) o)[0].toString();
                    newFile = ((Object[]) o)[1].toString();
                } else {
                    newFile = file = (String) o;
                }
                //Check if it exists
                filesToEmbed.add(file);
                file = "%" + PROP_ZIDVPATH + "%/"
                       + IOUtil.getFileTail(newFile);
                relativeFiles.add(file);
            }
            dataSource.setTmpPaths(relativeFiles);
        }
        return filesToEmbed;

    }



    /**
     * Show the gui to select what data sources can have their paths changed from a bundle
     *
     * @param dataSources data sources
     *
     * @return ok
     */
    private boolean showDataEditableGui(List dataSources) {
        List checkBoxes = new ArrayList();
        List workingSet = new ArrayList();
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            List       strings    = dataSource.getDataPaths();
            if ((strings != null) && (strings.size() > 0)) {
                workingSet.add(dataSource);
                JCheckBox cbx = new JCheckBox(
                                    DataSelector.getNameForDataSource(
                                        dataSource));
                checkBoxes.add(cbx);
            }
        }

        if (checkBoxes.size() == 0) {
            return true;
        }
        JComponent panel =
            GuiUtils
                .vbox(GuiUtils
                    .inset(GuiUtils
                        .cLabel(
                            "Select the data sources that should be saved with editable paths"), 5), GuiUtils
                                .vbox(checkBoxes));

        if ( !GuiUtils.askOkCancel("Data Sources", panel)) {
            return false;
        }
        //        System.err.println ("setting data editable");
        for (int i = 0; i < workingSet.size(); i++) {
            DataSource dataSource = (DataSource) workingSet.get(i);
            JCheckBox  cbx        = (JCheckBox) checkBoxes.get(i);
            dataSource.setDataIsEditable(cbx.isSelected());
        }

        return true;
    }


    /**
     * Add the given state, if non null, to the bundle hashtable
     *
     * @param data Holds the bundle state
     * @param dataSources List of data sources to add
     * @param displayControls List of displays to add
     * @param viewManagers List of view managers to add
     * @param jython jython to add
     *
     * @return If user is ok
     */
    protected boolean addToBundle(Hashtable data, List dataSources,
                                  List displayControls, List viewManagers,
                                  String jython) {

        data.put(ID_VERSION, getStateManager().getVersion());
        if (dataSources != null) {
            if (makeDataRelative) {
                if ( !showDataRelativeGui(dataSources)) {
                    return false;
                }
            }

            if (makeDataEditable) {
                if ( !showDataEditableGui(dataSources)) {
                    return false;
                }
            }

            //Check for realative files.
            data.put(ID_DATASOURCES, dataSources);
        }

        if (jython != null) {
            //            if (saveSelectedJythonBtn.isSelected()) {
            if (saveJythonBox.getSelectedIndex() == 2) {
                final JTextArea fromTextArea = new JTextArea(jython);
                fromTextArea.setEditable(false);
                final JTextArea toTextArea = new JTextArea();
                JButton         appendBtn  = new JButton("Copy Selected ->");
                appendBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        toTextArea.setText(toTextArea.getText() + "\n"
                                           + fromTextArea.getSelectedText());
                    }
                });
                JScrollPane fromSP = new JScrollPane(fromTextArea);
                JScrollPane toSP   = new JScrollPane(toTextArea);
                toSP.setPreferredSize(new Dimension(300, 400));
                fromSP.setPreferredSize(new Dimension(300, 400));

                JPanel contents = GuiUtils.doLayout(new Component[] { fromSP,
                        GuiUtils.top(appendBtn), toSP }, 3, GuiUtils.WT_YNY,
                            GuiUtils.WT_Y);

                contents = GuiUtils.topCenter(
                    GuiUtils.cLabel(
                        "Please select the Jython you want to include in the bundle"), contents);
                if ( !GuiUtils.showOkCancelDialog(null, "Save Jython",
                        GuiUtils.inset(contents, 5), null)) {
                    return false;
                }
                jython = toTextArea.getText();
            }
            data.put(ID_JYTHON, jython);
        }


        if (displayControls != null) {
            data.put(ID_DISPLAYCONTROLS, displayControls);
        }


        if (viewManagers != null) {
            //Check if we have a VM that is the main VM
            //If we don't *and* we have some MVM then set its view descriptor
            //to be the ain view descriptor.
            MapViewManager firstMvm  = null;
            boolean        gotMain3D = false;
            for (int i = 0; ( !gotMain3D) && (i < viewManagers.size()); i++) {
                if ( !(viewManagers.get(i) instanceof MapViewManager)) {
                    continue;
                }
                MapViewManager mvm = (MapViewManager) viewManagers.get(i);
                if ((mvm.getViewDescriptor() != null)
                        && mvm.getViewDescriptor().equals(
                            ViewDescriptor.LASTACTIVE)) {
                    gotMain3D = true;
                }
                if (firstMvm == null) {
                    firstMvm = mvm;
                }
            }
            if ( !gotMain3D && (firstMvm != null)) {
                //firstMvm.setViewDescriptor(ViewDescriptor.LASTACTIVE);
            }
            data.put(ID_VIEWMANAGERS, viewManagers);

            List windows = getIdvUIManager().getWindowsToPersist();
            data.put(ID_WINDOWS, windows);
        }
        getIdvUIManager().addStateToBundle(data);
        return true;
    }



    /**
     * This converts the given base 64 encoded xml bundle into an
     * xml String and loads it in. It prints out UI messages and
     * shows the wait cursor.
     *
     * @param base64Bundle The base64 encoded xml bundle
     */
    public void loadB64Bundle(String base64Bundle) {
        LogUtil.message("Loading bundle");
        showWaitCursor();
        decodeBase64Bundle(base64Bundle);
        showNormalCursor();
        LogUtil.clearMessage("Loading bundle");
    }

    /**
     * This does the actual work of converting the given base 64 encoded
     * bundle xml into a String and loading it in.
     *
     * @param base64Bundle The base64 encoded xml bundle
     */
    public void decodeBase64Bundle(String base64Bundle) {
        try {
            LogUtil.consoleMessage("Decoding a base 64 bundle\n");
            String xml = new String(XmlUtil.decodeBase64(base64Bundle));
            if (getArgsManager().printJnlpBundles) {
                System.out.println(xml);
            }
            decodeXml(xml, false, null, true);
        } catch (Throwable exc) {
            logException("Decoding base 64 bundle", exc);
        }
    }


    /**
     * Decode the jnlp file
     *
     * @param filename  the name of the file
     */
    public void decodeJnlpFile(String filename) {
        String xml = extractBundleFromJnlp(filename);
        decodeXml(xml, false, null, true);
    }


    /**
     * This reads in the  jnlp xml from the given filename. It extracts
     * from the &quot;arguments&quot; section of the xml the -b64bundle
     * values. This is the base 64 encoded xml bundle. It then loads in the
     * bundle.
     *
     * @param filename The name of the jnlp file.
     *
     * @return the bundle
     */
    public String extractBundleFromJnlp(String filename) {
        try {
            Element root = XmlUtil.getRoot(filename, getClass());
            if (root == null) {
                throw new IllegalArgumentException(
                    "Could not load JNLP file:" + filename);
            }
            List    arguments     = XmlUtil.findDescendants(root, "argument");
            boolean nextOneBundle = false;
            for (int i = 0; i < arguments.size(); i++) {
                Node   n     = (Node) arguments.get(i);
                String value = XmlUtil.getChildText(n);
                if (value == null) {
                    continue;
                }
                if (nextOneBundle) {
                    String xml = new String(XmlUtil.decodeBase64(value));
                    System.err.println("XXXXXXXXXXXXXXX"
                                       + getArgsManager().printJnlpBundles);
                    if (getArgsManager().printJnlpBundles) {
                        System.out.println(xml);
                    }
                    return xml;
                }
                if (value.equals("-b64bundle")) {
                    nextOneBundle = true;
                }
            }

        } catch (Throwable exc) {
            logException("decoding jnlp file:" + filename, exc);
        }
        return null;
    }





    /**
     *  Read in the contents of the given xmlFile and evaluate the xml
     *
     * @param xmlFile The bundle file
     * @param checkToRemove Should check the user preference to remove all/show dialog or not.
     *
     *
     * @return success
     */
    public boolean decodeXmlFile(String xmlFile, boolean checkToRemove) {
        return decodeXmlFile(xmlFile, checkToRemove, false);
    }

    /**
     *  Read in the contents of the given xmlFile and evaluate the xml
     *
     * @param xmlFile file
     * @param checkToRemove check to remove
     * @param letUserChangeData Set the 'user changes data flag' on decoding
     *
     * @return everything cool
     */
    public boolean decodeXmlFile(String xmlFile, boolean checkToRemove,
                                 boolean letUserChangeData) {
        return decodeXmlFile(xmlFile, null, checkToRemove, letUserChangeData,
                             null);
    }


    /**
     *  Read in the contents of the given xmlFile and evaluate the xml
     *
     * @param xmlFile The bundle file
     * @param checkToRemove Should check the user preference to remove all/show dialog or not.
     * @param bundleProperties  set of properties
     *
     *
     * @return success
     */
    public boolean decodeXmlFile(String xmlFile, boolean checkToRemove,
                                 Hashtable bundleProperties) {
        return decodeXmlFile(xmlFile, null, checkToRemove, bundleProperties);
    }



    /**
     *  Read in the contents of the given xmlFile and evaluate the xml
     *
     * @param xmlFile The bundle file
     * @param label If non-null ten use this as te load dialog label
     * @param checkToRemove Should check the user preference to remove all/show dialog or not.
     *
     *
     * @return success
     */
    public boolean decodeXmlFile(String xmlFile, String label,
                                 boolean checkToRemove) {

        return decodeXmlFile(xmlFile, label, checkToRemove, null);
    }

    /**
     * Import the bundle file
     *
     * @param xmlFile file name
     * @param label description for dialog
     * @param checkToRemove Should we ask the user about removing data/displays
     * @param bundleProperties  set of properties
     *
     * @return success
     */
    public boolean decodeXmlFile(String xmlFile, String label,
                                 boolean checkToRemove,
                                 Hashtable bundleProperties) {

        return decodeXmlFile(xmlFile, label, checkToRemove, false,
                             bundleProperties);
    }



    /**
     * decode the xml
     *
     * @param xmlFile file
     * @param label label
     * @param checkToRemove check to remove
     * @param letUserChangeData if true then ask user to change data paths
     * @param bundleProperties  set of properties
     *
     * @return everything cool
     */
    public boolean decodeXmlFile(String xmlFile, String label,
                                 boolean checkToRemove,
                                 boolean letUserChangeData,
                                 Hashtable bundleProperties) {

        String  name         = ((label != null)
                                ? label
                                : IOUtil.getFileTail(xmlFile));
        boolean shouldMerge  = getStore().get(PREF_OPEN_MERGE, true);
        boolean didRemoveAll = false;
        if (checkToRemove) {
            //ok[0] == did the user press cancel, ok[1] = should we remove
            boolean[] ok =
                getPreferenceManager().getDoRemoveBeforeOpening(name);
            if ( !ok[0]) {
                return false;
            }

            //Only set the letUserChangeData flag if the gui was shown
            if (ok[3]) {
                letUserChangeData = getIdv().getChangeDataPaths();
            }
            if (ok[1]) {
                //Remove the displays first because, if we remove the data some state can get cleared
                //that might be accessed from a timeChanged on the unremoved displays
                getIdv().removeAllDisplays();
                //Then remove the data
                getIdv().removeAllDataSources();
                didRemoveAll = true;
            }
            shouldMerge = ok[2];
        } else {
            if (shouldMerge) {
                didRemoveAll = true;
            }
        }


        boolean isZidv = getArgsManager().isZidvFile(xmlFile);


        if ( !isZidv && !getArgsManager().isXidvFile(xmlFile)) {
            //If we cannot tell what it is then try to open it as a zidv file
            try {
                ZipInputStream zin =
                    new ZipInputStream(IOUtil.getInputStream(xmlFile));
                isZidv = (zin.getNextEntry() != null);
            } catch (Exception exc) {}
        }


        String bundleContents = null;
        try {
            //Is this a zip file
            //            System.err.println ("file "  + xmlFile);
            if (isZidv) {
                //                System.err.println (" is zidv");
                boolean ask   = getStore().get(PREF_ZIDV_ASK, true);
                boolean toTmp = getStore().get(PREF_ZIDV_SAVETOTMP, true);
                String  dir   = getStore().get(PREF_ZIDV_DIRECTORY, "");
                if (ask || ((dir.length() == 0) && !toTmp)) {
                    JCheckBox askCbx = new JCheckBox("Don't show this again",
                                           !ask);
                    JRadioButton tmpBtn =
                        new JRadioButton("Write to temporary directory",
                                         toTmp);
                    JRadioButton dirBtn = new JRadioButton("Write to:",
                                              !toTmp);
                    GuiUtils.buttonGroup(tmpBtn, dirBtn);
                    JTextField dirFld = new JTextField(dir, 30);
                    JComponent dirComp = GuiUtils.centerRight(
                                             dirFld,
                                             GuiUtils.makeFileBrowseButton(
                                                 dirFld, true, null));
                    JComponent contents =
                        GuiUtils
                            .vbox(GuiUtils
                                .inset(new JLabel("Where should the data files be written to?"),
                                       5), tmpBtn,
                                           GuiUtils.hbox(dirBtn, dirComp),
                                           GuiUtils
                                               .inset(askCbx,
                                                   new Insets(5, 0, 0, 0)));
                    contents = GuiUtils.inset(contents, 5);
                    if ( !GuiUtils.showOkCancelDialog(null, "Zip file data",
                            contents, null)) {
                        return false;
                    }
                    ask   = !askCbx.isSelected();
                    toTmp = tmpBtn.isSelected();
                    dir   = dirFld.getText().toString().trim();
                    getStore().put(PREF_ZIDV_ASK, ask);
                    getStore().put(PREF_ZIDV_SAVETOTMP, toTmp);
                    getStore().put(PREF_ZIDV_DIRECTORY, dir);
                    getStore().save();
                }


                String tmpDir = dir;
                if (toTmp) {
                    tmpDir = getIdv().getObjectStore().getUserTmpDirectory();
                    tmpDir = IOUtil.joinDir(tmpDir, Misc.getUniqueId());
                }
                IOUtil.makeDir(tmpDir);

                getStateManager().putProperty(PROP_ZIDVPATH, tmpDir);

                ZipInputStream zin =
                    new ZipInputStream(IOUtil.getInputStream(xmlFile));
                //                Object loadId = JobManager.getManager().startLoad("Unpacking zidv file", true);
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    String entryName = ze.getName();
                    //                    if ( !JobManager.getManager().canContinue(loadId)) {
                    //                        JobManager.getManager().stopLoad(loadId);
                    //                        return false;
                    //                    }
                    //                    System.err.println ("entry : " + entryName);
                    if (getArgsManager().isXidvFile(
                            entryName.toLowerCase())) {
                        bundleContents = new String(IOUtil.readBytes(zin,
                                null, false));
                    } else {
                        //                        JobManager.getManager().setDialogLabel1(loadId, "Unpacking " + entryName);
                        if (IOUtil.writeTo(
                                zin,
                                new FileOutputStream(
                                    IOUtil.joinDir(tmpDir, entryName))) < 0) {
                            //                            JobManager.getManager().stopLoad(loadId);

                            return false;
                        }
                    }
                }
                //                JobManager.getManager().stopLoad(loadId);
            } else {
                Trace.call1("Decode.readContents");
                //                System.err.println ("reading bundle:" + xmlFile);
                bundleContents = IOUtil.readContents(xmlFile);
                Trace.call2("Decode.readContents");
            }

            Trace.call1("Decode.decodeXml");
            decodeXml(bundleContents, false, xmlFile, name, true,
                      shouldMerge, bundleProperties, didRemoveAll,
                      letUserChangeData);
            Trace.call2("Decode.decodeXml");
            return true;
        } catch (Throwable exc) {
            if (contents == null) {
                logException("Unable to load bundle:" + xmlFile, exc);
            } else {
                logException("Unable to evaluate bundle:" + xmlFile, exc);
            }
            return false;
        }
    }


    /**
     *  Using the XmlEncoder, decode the given xml string.
     *  This typically is a Hashtable, encoded by the doSave method,
     *  that holds a list of data sources, display controls, etc.
     * <p>
     * This method wraps a call to decodeXmlInner,
     * setting LOADINGXML flags, etc.
     *
     * @param xml The bundle xml
     * @param fromCollab Was this bundle from the collaboration facility.
     * If it was we treat it differently.
     * @param label The label to use in the dialog
     * @param showDialog Should the dialog be shown
     */
    public void decodeXml(String xml, boolean fromCollab, String label,
                          boolean showDialog) {
        decodeXml(xml, fromCollab, null, label, showDialog, true, null,
                  false, false);
    }



    /**
     *  Using the XmlEncoder, decode the given xml string.
     *  This typically is a Hashtable, encoded by the doSave method,
     *  that holds a list of data sources, display controls, etc.
     * <p>
     * This method wraps a call to decodeXmlInner,
     * setting LOADINGXML flags, etc.
     *
     * @param xml The bundle xml
     * @param fromCollab Was this bundle from the collaboration facility.
     * If it was we treat it differently.
     * @param xmlFile The filename this came from. May be null.
     * @param label The label to use  in the dialog
     * @param showDialog Should the dialog be shown
     * @param shouldMerge Should we merge the windows/views in the bundle into the existing windows
     * @param bundleProperties  set of properties
     * @param didRemoveAll Should we remove all data/displays
     * @param letUserChangeData Should popup the data path dialog
     */
    public void decodeXml(final String xml, final boolean fromCollab,
                          final String xmlFile, final String label,
                          final boolean showDialog,
                          final boolean shouldMerge,
                          final Hashtable bundleProperties,
                          final boolean didRemoveAll,
                          final boolean letUserChangeData) {

        Runnable runnable = new Runnable() {
            public void run() {
                decodeXmlInner(xml, fromCollab, xmlFile, label, showDialog,
                               shouldMerge, bundleProperties, didRemoveAll,
                               letUserChangeData);
            }
        };

        if ( !getStateManager().getShouldLoadBundlesSynchronously()) {
            Misc.run(runnable);
        } else {
            runnable.run();
        }
    }



    /**
     * This does the real work of decoding an xml bundle.
     *
     * @param xml The xml bundle
     * @param fromCollab Was this bundle from the collaboration facility.
     * If it was we treat it differently.
     * @param xmlFile The filename where the xml came from - may be null.
     * @param label The label to show in the gui
     * @param showDialog Should the loadbundle dialog be shown
     */
    protected synchronized void decodeXmlInner(String xml,
            boolean fromCollab, String xmlFile, String label,
            boolean showDialog) {
        decodeXmlInner(xml, fromCollab, xmlFile, label, showDialog, true,
                       null, false, false);
    }


    /**
     * replace any macros in the bundle xml
     *
     * @param xml bundle xml
     *
     * @return replaced bundle xml
     */
    protected String applyPropertiesToBundle(String xml) {
        //LOOK: For now don't try to be tricky with macros in bundles
        if (true) {
            return xml;
        }
        StringBuffer sb        = new StringBuffer(xml);
        Hashtable    map       = getResourceManager().getMacroMap();

        List         argNames  = getArgsManager().argPropertyNames;
        List         argValues = getArgsManager().argPropertyValues;
        for (int i = 0; i < argNames.size(); i++) {
            map.put(argNames.get(i), argValues.get(i));
        }

        for (Enumeration keys = map.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = (String) map.get(key);
            StringUtil.substitute(sb, "${" + key + "}", value);
        }


        xml = sb.toString();

        String tmp      = xml;
        int    idx1     = 0;
        int    idx2     = 0;
        List   unknowns = null;
        while (true) {
            idx1 = tmp.indexOf("${", idx2);
            if (idx1 < 0) {
                break;
            }
            idx2 = tmp.indexOf("}", idx1);
            if (idx2 < 0) {
                break;
            }
            if (unknowns == null) {
                unknowns = new ArrayList();
            }
            String macro = xml.substring(idx1 + 2, idx2);
            if ( !unknowns.contains(macro)) {
                unknowns.add(macro);
            }
        }
        if ((unknowns != null) && (unknowns.size() > 0)) {
            List fields = new ArrayList();
            List comps  = new ArrayList();
            comps.add(new JLabel("Macro"));
            comps.add(new JLabel("Value"));
            for (int i = 0; i < unknowns.size(); i++) {
                String macro = (String) unknowns.get(i);
                //If its really not a bundle 
                if (macro.length() > 100) {
                    throw new IllegalStateException(
                        "One of the bundle macros is quite long. Perhaps this is not a bundle file?");
                }
                final JTextField fld = new JTextField("", 40);
                fields.add(fld);
                comps.add(GuiUtils.lLabel(macro));
                comps.add(GuiUtils.centerRight(fld,
                        GuiUtils.makeFileBrowseButton(fld)));
            }
            if (comps.size() > 20) {
                throw new IllegalStateException(
                    "There seems to be a plethora of bundle macros. Perhaps this is not a bundle file?");
            }
            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            JComponent panel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                   GuiUtils.WT_N);
            panel = GuiUtils.vbox(
                GuiUtils.cLabel("There were unknown macros in the bundle"),
                panel);
            if ( !GuiUtils.askOkCancel("Bundle Macros", panel)) {
                return null;
            }
            sb = new StringBuffer(xml);
            for (int i = 0; i < unknowns.size(); i++) {
                String     macro = (String) unknowns.get(i);
                JTextField fld   = (JTextField) fields.get(i);
                StringUtil.substitute(sb, "${" + macro + "}",
                                      fld.getText().trim());
            }
            xml = sb.toString();
        }

        return xml;
    }



    /**
     * This does the real work of decoding an xml bundle.
     *
     * @param xml The xml bundle
     * @param fromCollab Was this bundle from the collaboration facility.
     * If it was we treat it differently.
     * @param xmlFile The filename where the xml came from - may be null.
     * @param label The label to show in the gui
     * @param showDialog Should the loadbundle dialog be shown
     * @param shouldMerge Should we merge the windows/views in the bundle into the existing windows
     * @param bundleProperties  set of properties
     * @param didRemoveAll Was remove all called before we decoded the xml
     * @param letUserChangeData Should popup data path change dialog
     */
    protected synchronized void decodeXmlInner(String xml,
            boolean fromCollab, String xmlFile, String label,
            boolean showDialog, boolean shouldMerge,
            Hashtable bundleProperties, boolean didRemoveAll,
            boolean letUserChangeData) {

        LoadBundleDialog loadDialog = new LoadBundleDialog(this, label);
        boolean          inError    = false;

        if ( !fromCollab) {
            showWaitCursor();
            if (showDialog) {
                loadDialog.showDialog();
            }
        }

        if (xmlFile != null) {
            getStateManager().putProperty(PROP_BUNDLEPATH,
                                          IOUtil.getFileRoot(xmlFile));
        }



        getStateManager().putProperty(PROP_LOADINGXML, true);
        DataSource datasource = null;
        try {
            xml = applyPropertiesToBundle(xml);
            if (xml == null) {
                return;
            }
            Trace.call1("Decode.toObject");
            Object data = getIdv().getEncoderForRead().toObject(xml);
            Trace.call2("Decode.toObject");

            if (data != null) {
                Hashtable properties = new Hashtable();
                if (data instanceof Hashtable) {
                    Hashtable ht = (Hashtable) data;
                    instantiateFromBundle(ht, fromCollab, loadDialog,
                                          shouldMerge, bundleProperties,
                                          didRemoveAll, letUserChangeData);
                } else if (data instanceof DisplayControl) {
                    ((DisplayControl) data).initAfterUnPersistence(getIdv(),
                            properties);
                    loadDialog.addDisplayControl((DisplayControl) data);
                } else if (data instanceof DataSource) {
                    datasource = (DataSource) data;
                    getIdv().getDataManager().addDataSource(datasource);
                } else if (data instanceof ColorTable) {
                    getColorTableManager().doImport(data, true);
                } else {
                    LogUtil.userErrorMessage(
                        log_,
                        "Decoding xml. Unknown object type:"
                        + data.getClass().getName());
                }
                if ( !fromCollab && getIdv().haveCollabManager()) {
                    getCollabManager().write(getCollabManager().MSG_BUNDLE,
                                             xml);
                }
            }

        } catch (Throwable exc) {
            if (xmlFile != null) {
                logException("Error loading bundle: " + xmlFile, exc);
            } else {
                logException("Error loading bundle", exc);
            }
            inError = true;
        }
        if ( !fromCollab) {
            showNormalCursor();
        }


        getStateManager().putProperty(PROP_BUNDLEPATH, "");
        getStateManager().putProperty(PROP_ZIDVPATH, "");
        getStateManager().putProperty(PROP_LOADINGXML, false);



        if ( !inError && getIdv().getInteractiveMode()) {
            if (xmlFile != null) {

                if (datasource != null) {
                    String identifier = datasource.getClass().getName() + "_"
                                        + xmlFile;
                    identifier = new String(
                        XmlUtil.encodeBase64(identifier.getBytes()));
                    getIdv().addToHistoryList(
                        new DataSourceHistory(
                            datasource.toString(), xml, identifier));
                } else {
                    getIdv().addToHistoryList(xmlFile);
                }
            }
        }

        loadDialog.dispose();
        if (loadDialog.getShouldRemoveItems()) {
            List displayControls = loadDialog.getDisplayControls();
            for (int i = 0; i < displayControls.size(); i++) {
                try {
                    ((DisplayControl) displayControls.get(i)).doRemove();
                } catch (Exception exc) {
                    //Ignore the exception
                }
            }
            List dataSources = loadDialog.getDataSources();
            for (int i = 0; i < dataSources.size(); i++) {
                getIdv().removeDataSource((DataSource) dataSources.get(i));
            }
        }

        loadDialog.clear();


    }


    /**
     * Do the macro substitutions
     *
     * @param dataSources data sources
     * @param letUserChangeData flag
     *
     * @return ok
     */
    private boolean updateDataPaths(List dataSources,
                                    boolean letUserChangeData) {

        //        System.err.println ("calling update data paths " +letUserChangeData);
        String bundlePath =
            (String) getStateManager().getProperty(PROP_BUNDLEPATH);
        if (bundlePath == null) {
            bundlePath = "";
        }
        String zidvPath =
            (String) getStateManager().getProperty(PROP_ZIDVPATH);

        for (int dataSourceIdx = 0; dataSourceIdx < dataSources.size();
                dataSourceIdx++) {
            DataSource dataSource =
                (DataSource) dataSources.get(dataSourceIdx);
            List tmpPaths = dataSource.getTmpPaths();
            if ((tmpPaths == null) || (tmpPaths.size() == 0)) {
                continue;
            }
            List newPaths = new ArrayList();
            //Look at the different macros and the different values
            String[] macros = { "%" + PROP_BUNDLEPATH + "%",
                                "%" + PROP_ZIDVPATH + "%" };
            String[] values = { bundlePath, zidvPath, "." };
            for (int i = 0; i < tmpPaths.size(); i++) {
                String  source = (String) tmpPaths.get(i);
                boolean gotit  = false;
                for (int macroIdx = 0; !gotit && (macroIdx < macros.length);
                        macroIdx++) {
                    for (int valueIdx = 0;
                            !gotit && (valueIdx < values.length);
                            valueIdx++) {
                        if (values[valueIdx] == null) {
                            continue;
                        }
                        String tmp = StringUtil.replace(source,
                                         macros[macroIdx], values[valueIdx]);
                        if ((new File(tmp)).exists()) {
                            source = tmp;
                            gotit  = true;
                        }
                    }
                }
                newPaths.add(source);
            }
            dataSource.setTmpPaths(newPaths);
        }


        List     editableComps       = new ArrayList();
        List     dataEditableSources = new ArrayList();
        List     dataEditableWidgets = new ArrayList();

        double[] stretchy            = new double[dataSources.size()];

        for (int dataSourceIdx = 0; dataSourceIdx < dataSources.size();
                dataSourceIdx++) {
            DataSource dataSource =
                (DataSource) dataSources.get(dataSourceIdx);


            if ( !dataSource.getDataIsEditable() && !letUserChangeData) {
                continue;
            }
            //            System.err.println(dataSource.getDataIsEditable() + " letuse:" + letUserChangeData);

            //First try the tmp paths in case we did the data relative above
            List dataPaths = dataSource.getTmpPaths();
            if ((dataPaths == null) || (dataPaths.size() == 0)) {
                dataPaths = dataSource.getDataPaths();
            }
            if ((dataPaths == null) || (dataPaths.size() == 0)) {
                continue;
            }
            dataEditableSources.add(dataSource);
            JLabel label = new JLabel(dataSource.toString());
            JButton chooserBtn = GuiUtils.makeButton("Change Data:", this,
                                     "changeData", new Object[] { dataSource,
                    label });
            JComponent widgetContents = GuiUtils.leftCenter(chooserBtn,
                                            GuiUtils.inset(label, 5));
            widgetContents = GuiUtils.inset(widgetContents,
                                            new Insets(10, 0, 0, 0));
            editableComps.add(widgetContents);
        }

        if ( !getArgsManager().getIsOffScreen()
                && (editableComps.size() > 0)) {
            JComponent panel = GuiUtils.doLayout(editableComps, 1,
                                   GuiUtils.WT_Y, stretchy);
            panel = GuiUtils.inset(
                GuiUtils.topCenter(
                    GuiUtils.cLabel(
                        "You can choose new files for the following data sources"), panel), 5);

            if ( !GuiUtils.showOkCancelDialog(null, "Data Sources", panel,
                    null)) {
                return false;
            }
        }
        return true;

    }



    /**
     * change data
     *
     * @param input the input
     */
    public void changeData(Object[] input) {
        DataSource dataSource = (DataSource) input[0];
        JLabel     label      = (JLabel) input[1];
        if (changeState(dataSource, false)) {
            label.setText(dataSource.toString());
        }
    }

    /**
     * n/a
     *
     * @param dataSource the data source
     *
     * @return n/a
     */
    public boolean changeState(DataSource dataSource) {
        return changeState(dataSource, true);
    }

    /**
     * n/a
     *
     * @param dataSource the data source
     * @param andReload and reload the data
     *
     * @return n/a
     */
    public boolean changeState(DataSource dataSource, boolean andReload) {
        List choosers = new ArrayList();
        Component comp =
            getIdv().getIdvChooserManager().createChoosers(false, choosers,
                null);

        final Object[]    result     = { null };
        final Hashtable[] properties = { null };
        final JDialog dialog = GuiUtils.createDialog(null,
                                   "Change data for: " + dataSource, true);
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Object[] newData = (Object[]) ae.getSource();
                result[0]     = newData[0];
                properties[0] = (Hashtable) newData[1];
                dialog.dispose();
            }
        };
        for (int i = 0; i < choosers.size(); i++) {
            IdvChooser chooser = (IdvChooser) choosers.get(i);
            chooser.setDataSourceListener(listener);
        }

        JButton cancelBtn = GuiUtils.makeButton("Cancel", dialog, "dispose");
        comp = GuiUtils.inset(
            GuiUtils.topCenterBottom(
                GuiUtils.inset(
                    new JLabel("Select new data for: " + dataSource),
                    5), comp, GuiUtils.wrap(cancelBtn)), 5);

        dialog.getContentPane().add(comp);
        dialog.pack();
        dialog.show();
        if (result[0] == null) {
            return false;
        }

        try {
            dataSource.updateState(result[0], properties[0]);
            if (andReload) {
                dataSource.reloadData();
            }
            return true;
        } catch (Exception exc) {
            logException("Updating data source", exc);
        }
        return false;
    }



    /**
     * The given Hashtable contains the objects taht were saved
     * in the xml bundle file. The hashtable may contain
     * a list of data sources, a list of display controls and
     * a list of view managers. This method initializes these objects
     * adn adds them into the current application state.
     *
     * @param ht Contains the unpersisted objects
     * @param fromCollab Was this bundle from the collaboration facility.
     * If it was we treat it differently.
     * @param loadDialog  The load bundle dialog
     * @param shouldMerge Should we merge the windows/views in the bundle into the existing windows
     * @param bundleProperties  set of properties
     * @param didRemoveAll Was remove all called before we decoded the xml
     * @param letUserChangeData Should popup data path change dialog
     *
     * @throws Exception
     */
    protected void instantiateFromBundle(Hashtable ht, boolean fromCollab,
                                         LoadBundleDialog loadDialog,
                                         boolean shouldMerge,
                                         Hashtable bundleProperties,
                                         boolean didRemoveAll,
                                         boolean letUserChangeData)
            throws Exception {



        if ( !loadDialog.okToRun()) {
            return;
        }

        String version = (String) ht.get(ID_VERSION);
        if (version == null) {
            version = getStateManager().getVersion();
        }

        String jython = (String) ht.get(ID_JYTHON);
        if (jython != null) {
            final String theJython = jython;
            //If we are in off screen mode (e.g., running ISL) then add the jython to the tmp library
            if (getArgsManager().getIsOffScreen()) {
                getJythonManager().appendTmpJython(theJython);
            } else {
                JLabel label =
                    new JLabel(
                        "<html>The bundle contained the following jython library.<p>&nbsp;&nbsp; What would you like to do with this?<br></html>");
                final JDialog dialog =
                    GuiUtils.createDialog("Load Jython Library", true);

                final JTextArea textArea = new JTextArea(jython);
                textArea.setEditable(false);

                JButton dontLoadBtn = new JButton("Don't load it");
                JButton addItBtn = new JButton("Add it to my local library");
                JButton addTmpBtn =
                    new JButton("Add it to my temporary library");
                JButton addSelectedBtn = new JButton("Add selected text");


                addItBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        getJythonManager().appendJythonFromBundle(theJython);
                        dialog.dispose();
                    }
                });


                addTmpBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        getJythonManager().appendTmpJython(theJython);
                        dialog.dispose();
                    }
                });

                addSelectedBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        String text = textArea.getSelectedText();
                        if ((text != null) && (text.length() > 0)) {
                            getJythonManager().appendJythonFromBundle(text);
                        }
                        dialog.dispose();
                    }
                });
                dontLoadBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        dialog.dispose();
                    }
                });
                JPanel buttons = GuiUtils.hbox(Misc.newList(addItBtn,
                                     addTmpBtn, addSelectedBtn,
                                     dontLoadBtn), 5);
                JPanel comp =
                    GuiUtils.topCenter(
                        GuiUtils.inset(
                            GuiUtils.vbox(GuiUtils.inset(label, 5), buttons),
                            5), GuiUtils.makeScrollPane(textArea, 300, 500));
                dialog.getContentPane().add(comp);
                GuiUtils.showInCenter(dialog);
            }
        }
        List overrideTimes      = ((bundleProperties == null)
                                   ? null
                                   : (List) bundleProperties.get(
                                       PROP_TIMESLIST));

        List overrideEnsMembers = ((bundleProperties == null)
                                   ? null
                                   : (List) bundleProperties.get(
                                       PROP_ENSLIST));


        List dataSources = (List) ht.get(ID_DATASOURCES);
        if (dataSources != null) {
            if ( !updateDataPaths(dataSources, letUserChangeData)) {
                return;
            }

            List localFileMapping = null;
            if ((fileMapping != null) && (fileMapping.size() > 0)) {
                localFileMapping = new ArrayList(fileMapping);
            }

            final ThreadManager threadManager =
                new ThreadManager("Data source initialization");
            for (int i = 0; i < dataSources.size(); i++) {
                final DataSource dataSource = (DataSource) dataSources.get(i);
                //Clear the error flag
                dataSource.setInError(false);
                loadDialog.setMessage1("Loading data source " + (i + 1)
                                       + " of " + dataSources.size());
                loadDialog.setMessage2(
                    "(" + DataSelector.getNameForDataSource(dataSource)
                    + ")");
                if (localFileMapping != null) {
                    for (int mappingIdx = 0;
                            mappingIdx < localFileMapping.size();
                            mappingIdx++) {
                        ObjectPair pair =
                            (ObjectPair) localFileMapping.get(mappingIdx);
                        String identifier = (String) pair.getObject1();
                        List   files      = (List) pair.getObject2();
                        if (dataSource.identifiedByName(identifier)) {
                            //Remove this data source's local files
                            localFileMapping.remove(mappingIdx);
                            dataSource.setNewFiles(files);
                            break;
                        }
                    }
                }
                long t1 = System.currentTimeMillis();
                threadManager.addRunnable(new ThreadManager.MyRunnable() {
                    public void run() throws Exception {
                        dataSource.initAfterUnpersistence();
                    }
                });
            }

            long t1 = System.currentTimeMillis();
            try {
                //Don't run in parallel for now since it screws up the ordering
                //of the displays
                //threadManager.runAllParallel();
                threadManager.runInParallel(getIdv().getMaxDataThreadCount());
            } catch (Exception exc) {
                //Catch any exceptions thrown but then get all of them and show them to the user
                List<Exception> exceptions = threadManager.getExceptions();
                if (exceptions.size() == 0) {
                    //This shouldn't happen
                    exceptions.add(exc);
                }
                LogUtil.printExceptions(exceptions);
            }
            long t2 = System.currentTimeMillis();
            //            System.err.println ("time to init data sources:" + (t2-t1));


            for (int i = 0; i < dataSources.size(); i++) {
                final DataSource dataSource = (DataSource) dataSources.get(i);
                if (overrideTimes != null) {
                    dataSource.setDateTimeSelection(overrideTimes);
                }
                if ((overrideEnsMembers != null)
                        && (dataSource instanceof GridDataSource)) {
                    ((GridDataSource) dataSource).setEnsembleSelection(
                        overrideEnsMembers);
                }
                if ( !loadDialog.okToRun()) {
                    return;
                }
                if (dataSource.getInError()) {
                    continue;
                }
                if (getDataManager().addDataSource(dataSource)) {
                    loadDialog.addDataSource(dataSource);
                }
            }



            if ((localFileMapping != null) && (localFileMapping.size() > 0)) {
                throw new IllegalArgumentException(
                    "Did not find the data source to use for the file override: "
                    + localFileMapping);
            }
            clearDataSourcesState(dataSources);
            clearFileMapping();
        }
        if ( !loadDialog.okToRun()) {
            //TODO
            return;
        }

        if ( !fromCollab) {
            getIdvUIManager().applyDataHolderState(
                (Hashtable) ht.get(ID_MISCHASHTABLE));
        }


        //        ProjectionImpl dfltProjection = null;

        getVMManager().setDisplayMastersInactive();

        try {
            List<ViewManager> currentViewManagers =
                getVMManager().getViewManagers();
            List windows         = (List) ht.get(ID_WINDOWS);
            List newViewManagers = (List) ht.get(ID_VIEWMANAGERS);


            if (newViewManagers != null) {
                //This just does basic initialization
                getVMManager().unpersistViewManagers(newViewManagers);
                for (ViewManager viewManager :
                        (List<ViewManager>) newViewManagers) {
                    //                    System.err.println ("vm:"+viewManager);
                }


            }
            List newControls = (List) ht.get(ID_DISPLAYCONTROLS);
            //            newControls = new ArrayList();

            //If we are not merging we have to reset the ViewDescriptor id
            //in the new view managers. Then we have to tell the new display  
            //controls  to use the new view descriptor ids
            if ( !shouldMerge && (newViewManagers != null)) {
                for (int i = 0; i < newViewManagers.size(); i++) {
                    ViewManager newViewManager =
                        (ViewManager) newViewManagers.get(i);
                    List oldAliases = newViewManager.getAliases();
                    List newAliases = new ArrayList();
                    for (int aliasIdx = 0; aliasIdx < oldAliases.size();
                            aliasIdx++) {
                        ViewDescriptor oldVd =
                            (ViewDescriptor) oldAliases.get(aliasIdx);
                        ViewDescriptor newVd =
                            (ViewDescriptor) new ViewDescriptor();
                        newVd.setClassNames(oldVd.getClassNames());
                        newAliases.add(newVd);
                        if (newControls != null) {
                            for (int controlIdx = 0;
                                    controlIdx < newControls.size();
                                    controlIdx++) {
                                DisplayControlImpl dc =
                                    (DisplayControlImpl) newControls.get(
                                        controlIdx);
                                dc.resetViewManager(oldVd.getName(),
                                        newVd.getName());
                            }
                        }
                    }
                    newViewManager.setAliases(newAliases);
                }
            }


            if (newViewManagers != null) {
                if (getArgsManager().getIsOffScreen()) {
                    Trace.call1("Decode.addViewManagers");
                    getVMManager().addViewManagers(newViewManagers);
                    Trace.call2("Decode.addViewManagers");
                } else {
                    if (windows != null) {
                        getIdvUIManager().unpersistWindowInfo(windows,
                                newViewManagers, shouldMerge, fromCollab,
                                didRemoveAll);

                    }
                }
            }



            //Have this here to handle old legacy bundles.
            //We know they are old if we don't have a windows list
            if ((newViewManagers != null) && (windows == null)) {
                for (int i = 0; i < newViewManagers.size(); i++) {
                    ViewManager viewManager =
                        (ViewManager) newViewManagers.get(i);
                    if (shouldMerge
                            && (viewManager.getViewDescriptor() == null)) {
                        for (int currentIdx = 0;
                                currentIdx < currentViewManagers.size();
                                currentIdx++) {
                            ViewManager vm =
                                (ViewManager) currentViewManagers.get(
                                    currentIdx);
                            if (vm.isCompatibleWith(viewManager)) {
                                currentViewManagers.remove(currentIdx);
                                vm.initWith(viewManager);
                                viewManager = null;
                                break;
                            }
                        }
                    }
                    if (viewManager != null) {
                        getVMManager().addViewManagers(
                            Misc.newList(viewManager));
                        getIdvUIManager().createNewWindow(
                            Misc.newList(viewManager));
                    }
                    if (shouldMerge) {
                        //Now get rid of any windows that are left over 
                        for (int currentIdx = 0;
                                currentIdx < currentViewManagers.size();
                                currentIdx++) {
                            ViewManager vm =
                                (ViewManager) currentViewManagers.get(
                                    currentIdx);
                            IdvWindow window = vm.getDisplayWindow();
                            if ((window != null)
                                    && !window.getHasBeenDisposed()) {
                                window.dispose();
                            }

                        }
                    }
                }

            } else if (newViewManagers != null) {
                //Add any remainders in
                getVMManager().addViewManagers(newViewManagers);
            }




            if (loadDialog.okToRun()) {
                if (newControls != null) {
                    if (getIdv().getArgsManager().getIsOffScreen()) {
                        //                    System.err.println ("initializing displays");
                    }
                    //Here we might want to first collect the displaycontrols
                    //that need to have data bound to them (i.e., those that
                    //were saved without data). Then popup one gui.

                    final Hashtable properties = new Hashtable();
                    Trace.call1("Decode.init displays");
                    final visad.util.ThreadManager displaysThreadManager =
                        new visad.util.ThreadManager(
                            "display initialization");
                    //If we are doing the time driver then do a 2 step initialization
                    //First do all of the displays that are the time driver displays
                    //next do the ones that aren't
                    //Note: This will screw up z ordering because the time driver
                    //displays will always get added first
                    int numberOfInitSteps = (getIdv().getUseTimeDriver()
                                             ? 2
                                             : 1);
                    for (int initStep = 0; initStep < numberOfInitSteps;
                            initStep++) {
                        for (int i = 0; i < newControls.size(); i++) {
                            final DisplayControl displayControl =
                                (DisplayControl) newControls.get(i);
                            if (getIdv().getUseTimeDriver()) {
                                if ((initStep == 0)
                                        && !displayControl
                                            .getIsTimeDriver()) {
                                    continue;
                                } else if ((initStep == 1)
                                           && displayControl
                                               .getIsTimeDriver()) {
                                    continue;
                                }
                            }

                            loadDialog.setMessage1("Loading display "
                                    + (i + 1) + " of " + newControls.size());
                            loadDialog.setMessage2("("
                                    + displayControl.getLabel() + ")");
                            if (getIdv().haveCollabManager() && fromCollab
                                    && getCollabManager().haveDisplayControl(
                                        displayControl)) {
                                continue;
                            }
                            displaysThreadManager.addRunnable(
                                new visad.util.ThreadManager.MyRunnable() {
                                public void run() throws Exception {
                                    displayControl.initAfterUnPersistence(
                                        getIdv(), properties);
                                }
                            });
                            loadDialog.addDisplayControl(displayControl);
                            if ( !loadDialog.okToRun()) {
                                return;
                            }
                        }
                    }


                    long tt1 = System.currentTimeMillis();
                    displaysThreadManager.runSequentially();
                    //                    displaysThreadManager.runInParallel();
                    long tt2 = System.currentTimeMillis();
                    //              System.err.println ("time to init displays:" + (tt2-tt1));
                    //                    displaysThreadManager.clearTimes();
                    Trace.call2("Decode.init displays");
                }
                if ( !fromCollab) {
                    List commandsToRun = (List) ht.get(ID_COMMANDSTORUN);
                    if (commandsToRun != null) {

                        /**
                         *  For now don't do this since old bundles
                         *  that have these commands will be broken.
                         *  Also, do we really want to run these commands
                         *  from a bundle?
                         * for (int i = 0; i < commandsToRun.size(); i++) {
                         *   jythonManager.evaluateTrusted(
                         *       commandsToRun.get(i).toString());
                         * }
                         */
                    }
                }
            }
        } finally {
            getVMManager().setDisplayMastersActive();
        }

        loadDialog.setMessage("Activating displays");
        Trace.msg("Decode.end");

    }



    /**
     *  This creates a new data source from the xml encoded representation
     * of a persisted data source. It is used in the data source history
     * mechanism.
     *
     * @param dataSourceXml The xml encoded data source representation
     * @return The results that hold the new data source.
     */
    public DataSourceResults makeDataSourceFromXml(String dataSourceXml) {
        try {
            DataSource dataSource =
                (DataSource) getIdv().decodeObject(dataSourceXml);
            dataSource.initAfterUnpersistence();
            if (dataSource.getInError()) {
                return null;
            }
            getDataManager().addDataSource(dataSource);
            return new DataSourceResults(dataSource, dataSourceXml);
        } catch (Exception exc) {
            logException("Creating data source", exc);
            return new DataSourceResults(dataSourceXml, exc);
        }
    }




    /**
     * Should the view state be saved
     *
     * @return Save the view state
     */
    public boolean getSaveViewState() {
        return saveViewState;
    }


    /**
     * Should the displays be saved
     *
     * @return Save the displays
     */
    public boolean getSaveDisplays() {
        return saveDisplays;
    }


    /**
     * Should the data sources be saved
     *
     * @return Save the data sources
     */
    public boolean getSaveDataSources() {
        return saveDataSources;
    }



    /**
     * Should the jython be saved
     *
     * @return Save the data sources
     */
    public boolean getSaveJython() {
        return saveJython;
    }


    /**
     * Should the visad data  be saved
     *
     * @return Save the visad data
     */
    public boolean getSaveData() {
        return saveData;
    }


    /**
     *  Get the CurrentTemplateName property.
     *
     *  @return The CurrentTemplateName
     */
    public String getCurrentTemplateName() {
        return currentTemplateName;
    }


    /**
     * Return the bundle file that contains the prototype for the given class
     *
     * @param c class
     *
     * @return filename that (may) holds prototype
     */
    private File getPrototypeFile(Class c) {
        String filename = c.getName() + ".xml";
        ResourceCollection rc = getResourceManager().getResources(
                                    IdvResourceManager.RSC_PROTOTYPES);

        String dir = rc.getWritable();
        if (dir == null) {
            LogUtil.consoleMessage("No prototype resoruce path defined");
            return null;
        }
        IOUtil.makeDir(dir);
        String fullPath = IOUtil.joinDir(dir, filename);
        return new File(fullPath);
    }


    /**
     * Save off the given object as a prototype
     *
     * @param object Object to write as prototype
     */
    public void writePrototype(Object object) {
        try {
            String xml  = encodeSpecial(object);
            File   file = getPrototypeFile(object.getClass());
            if (file != null) {
                IOUtil.writeFile(file, xml);
            }
        } catch (Exception exc) {
            logException("writing prototype: " + object.getClass().getName(),
                         exc);
        }
    }

    /**
     * Clear the prototype for the given class
     *
     * @param c class
     */
    public void clearPrototype(Class c) {
        try {
            File f = getPrototypeFile(c);
            if ((f != null) && f.exists()) {
                f.delete();
            }
        } catch (Exception exc) {}
    }



    /**
     * Instantiate a new object for the given class. This will return null
     * if there is not a prototype defined for the class
     *
     * @param c class
     *
     * @return new object or null
     */
    public Object getPrototype(Class c) {
        try {
            File f = getPrototypeFile(c);
            if ((f == null) || !f.exists()) {
                return null;
            }
            String xml       = IOUtil.readContents(f.toString());
            Object prototype = getIdv().getEncoderForRead().toObject(xml);
            if (prototype instanceof Prototypable) {
                ((Prototypable) prototype).initAsPrototype();
            }
            return prototype;
        } catch (Exception exc) {
            logException("reading prototype: " + c.getName(), exc);
            return null;
        }
    }



}

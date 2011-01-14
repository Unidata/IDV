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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceResults;

import ucar.unidata.idv.*;

import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import javax.swing.filechooser.FileFilter;






/**
 * This provides a JFileChooser for choosing data sets from the file system.
 *  It can be instantiated from the chooser.xml with the attributes:
 * <pre>
 * path="initial file system path"
 * filters="File filters to use" (Define file filters)
 * datasourceid="The data source id (from datasource.xml, force the file to
 *                                   be passed to the data source)
 * </pre>
 *
 * For information on the filters attribute   see
 * {@link ucar.unidata.util.PatternFileFilter#createFilters(String)}
 *
 * @author IDV development team
 */


public class FileChooser extends IdvChooser {

    /** Any initial file system path to start with */
    public static final String ATTR_PATH = "path";

    /** The most recent attribute */
    public static final String ATTR_FILECOUNT = "filecount";


    /** Default polling value */
    public static final String ATTR_POLLON = "pollon";

    /** Should we show the polling cbx */
    public static final String ATTR_DSCOMP = "showdatasourcemenu";

    /** Should we show the file pattern field */
    public static final String ATTR_SHOWPATTERNFIELD = "showpatternfield";

    /** Default pattern */
    public static final String ATTR_FILEPATTERN = "filepattern";

    /** Any filter filters to use */
    public static final String ATTR_FILTERS = "filters";

    /** Pre-defined data source  id to use */
    public static final String ATTR_DATASOURCEID = "datasourceid";


    /** Como box for choosing relative files */
    protected JComboBox recentFilesCbx;

    /** allow directory selection checkbox */
    protected JCheckBox allowDirectorySelectionCbx;

    /** Holds the file pattern */
    protected JTextField patternFld;

    /** File pattern lable */
    private JLabel patternLbl;


    /** The file chooser */
    private JFileChooser fileChooser;


    /**
     *  The chooser xml can specify a datasourceid attribute.
     *  If set this file chooser uses that (instead of relying on the
     *  file name pattern matching).
     */
    private String dfltDataSourceId;


    /** Should we do polling */
    private JCheckBox doPollingCbx;


    /**
     * Create the FileChooser, passing in the manager and the xml element
     * from choosers.xml
     *
     * @param mgr The manager
     * @param root The xml root
     *
     */
    public FileChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String[] getButtonLabels() {
        return new String[] { getLoadCommandName(), GuiUtils.CMD_UPDATE,
                              GuiUtils.CMD_HELP };
    }




    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the selected file";
    }


    /**
     * Get the tooltip for the update button
     *
     * @return The tooltip for the update button
     */
    protected String getUpdateToolTip() {
        return "Rescan the directory";
    }

    /**
     * Get the recent file component
     *
     * @return  the component
     */
    protected JComponent getRecentFilesComponent() {
        int[] values = {
            0, 1, 2, 3, 4, 5, 6, 8, 10, 15, 20,
        };
        return getRecentFilesComponent(values, 0, true);
    }

    /**
     * Get the recent files component with the appropriate params
     *
     * @param values values
     * @param value  selected value
     * @param addPatternField  the pattern field
     *
     * @return  the component
     */
    protected JComponent getRecentFilesComponent(int[] values, int value,
            boolean addPatternField) {
        Vector         relativeList = new Vector();
        TwoFacedObject selected     = null;
        for (int i = 0; i < values.length; i++) {
            String label;
            if (values[i] == Integer.MAX_VALUE) {
                label = "All Files";
            } else if (values[i] == 0) {
                label = "Use Selected File(s)";
            } else if (values[i] == 1) {
                label = "Use Most Recent File";
            } else {
                label = "Use Most Recent " + values[i] + " Files";

            }
            TwoFacedObject tfo = new TwoFacedObject(label,
                                     new Integer(values[i]));
            if (values[i] == value) {
                selected = tfo;
            }
            relativeList.add(tfo);
        }
        recentFilesCbx = new JComboBox(relativeList);
        if (selected != null) {
            recentFilesCbx.setSelectedItem(selected);
        }
        if ( !addPatternField) {
            return recentFilesCbx;
        }
        recentFilesCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                TwoFacedObject tfo =
                    (TwoFacedObject) recentFilesCbx.getSelectedItem();
                int v = ((Integer) tfo.getId()).intValue();
                setHaveData(v > 0);
                checkRecentPatternWidgetsEnable();
            }
        });
        List comps = new ArrayList();
        comps.add(recentFilesCbx);
        comps.add(patternLbl = GuiUtils.lLabel(" File Pattern: "));
        comps.add(patternFld = new JTextField(".*", 10));
        checkRecentPatternWidgetsEnable();
        return GuiUtils.hbox(comps);
    }


    /**
     * Get the count of recent files
     *
     * @return Recent file count
     */
    protected int getFileCount() {
        return ((Integer) ((TwoFacedObject) recentFilesCbx.getSelectedItem())
            .getId()).intValue();
    }


    /**
     * Disable or enable the file pattern widget and label
     */
    protected void checkRecentPatternWidgetsEnable() {
        int recentCnt = getFileCount();
        patternFld.setEnabled(recentCnt > 0);
        patternLbl.setEnabled(recentCnt > 0);
    }

    /**
     * Get the file pattern
     *
     * @return  the file pattern
     */
    protected String getFilePattern() {
        if (patternFld != null) {
            return patternFld.getText().trim();
        }
        return null;
    }



    /**
     *  Construct a JFileChooser to put into the IdvChooserManager.
     *  Initialize to point to the DEFAULTDIR preference (if defined)
     *
     * @return The gui of this chooser
     */
    protected JComponent doMakeContents() {
        String path = (String) idv.getPreference(PREF_DEFAULTDIR + getId());
        if (path == null) {
            path = XmlUtil.getAttribute(chooserNode, ATTR_PATH,
                                        (String) null);
        }

        fileChooser = doMakeFileChooser(path);
        fileChooser.setPreferredSize(new Dimension(300, 300));
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setApproveButtonText(ChooserPanel.CMD_LOAD);


        List filters = new ArrayList();
        dfltDataSourceId = XmlUtil.getAttribute(chooserNode,
                ATTR_DATASOURCEID, (String) null);
        String filterString = XmlUtil.getAttribute(chooserNode, ATTR_FILTERS,
                                  (String) null);

        filters.addAll(getDataManager().getFileFilters());
        if (filterString != null) {
            filters.addAll(PatternFileFilter.createFilters(filterString));
        }


        if ( !filters.isEmpty()) {
            for (int i = 0; i < filters.size(); i++) {
                fileChooser.addChoosableFileFilter(
                    (FileFilter) filters.get(i));
            }
            fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
        }

        JComponent chooserPanel;
        JComponent accessory = getAccessory();
        if (accessory == null) {
            chooserPanel = fileChooser;
        } else {
            chooserPanel = GuiUtils.centerRight(fileChooser,
                    GuiUtils.top(accessory));
        }
        JPanel filePanel = GuiUtils.centerBottom(chooserPanel,
                               getDefaultButtons());

        List topComps = new ArrayList();
        getTopComponents(topComps);
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JComponent topComp = GuiUtils.doLayout(topComps, 2, GuiUtils.WT_N,
                                 GuiUtils.WT_N);
        topComp = GuiUtils.left(topComp);
        setStatus("Please select a file");
        return GuiUtils.topCenter(topComp, filePanel);
    }


    /**
     * Get the checkbox for allowing directory selection
     *
     * @return  the checkbox
     */
    protected JCheckBox getAllowDirectorySelectionCbx() {
        if (allowDirectorySelectionCbx == null) {
            allowDirectorySelectionCbx =
                new JCheckBox("Allow Directory Selection");
            allowDirectorySelectionCbx.setToolTipText(
                "<html><p>Select this if you want</p><p>be able to choose a directory.</p></html>");
            allowDirectorySelectionCbx.addActionListener(
                new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (allowDirectorySelectionCbx.isSelected()) {
                        fileChooser.setFileSelectionMode(
                            JFileChooser.FILES_AND_DIRECTORIES);
                    } else {
                        fileChooser.setFileSelectionMode(
                            JFileChooser.FILES_ONLY);
                    }
                }
            });
        }
        return allowDirectorySelectionCbx;

    }

    /**
     * Get the top components for the chooser
     *
     * @param comps  the top component
     */
    protected void getTopComponents(List comps) {
        if (XmlUtil.getAttribute(chooserNode, ATTR_DSCOMP, true)) {
            JComponent dsComp = getDataSourcesComponent();
            //      dsComp = GuiUtils.leftCenter(dsComp, GuiUtils.left(getAllowDirectorySelectionCbx()));
            dsComp = GuiUtils.left(dsComp);
            comps.add(GuiUtils.rLabel("Data Source Type:"));
            comps.add(GuiUtils.left(dsComp));
        }

        if (XmlUtil.hasAttribute(chooserNode, ATTR_POLLON)) {
            doPollingCbx = new JCheckBox("Automatically check for changes",
                                         XmlUtil.getAttribute(chooserNode,
                                             ATTR_POLLON, false));
            String pattern = XmlUtil.getAttribute(chooserNode,
                                 ATTR_FILEPATTERN, (String) null);
            if ((pattern != null)
                    || XmlUtil.getAttribute(chooserNode,
                                            ATTR_SHOWPATTERNFIELD, false)) {
                patternFld = new JTextField(pattern, 10);
            }
            comps.add(GuiUtils.rLabel(""));
            JComponent widgetComp = doPollingCbx;
            if (patternFld != null) {
                widgetComp = GuiUtils.hbox(widgetComp,
                                           new JLabel(" File Pattern: "),
                                           patternFld);
            }
            comps.add(GuiUtils.left(widgetComp));
        }

        if (shouldShowRecentComponent()) {
            comps.add(GuiUtils.rLabel("Use Relative Times:"));
            comps.add(GuiUtils.left(getRecentFilesComponent()));
        }
    }


    /**
     * Make the file chooser
     *
     * @param path   the initial path
     *
     * @return  the file chooser
     */
    protected JFileChooser doMakeFileChooser(String path) {
        return new MyFileChooser(path);
    }



    /**
     * An extension of JFileChooser
     *
     * @author IDV Development Team
     * @version $Revision: 1.67 $
     */
    public class MyFileChooser extends FileManager.MyFileChooser {

        /**
         * Create the file chooser
         *
         * @param path   the initial path
         */
        public MyFileChooser(String path) {
            super(path);
            this.setFileHidingEnabled(FileManager.getFileHidingEnabled());
            setControlButtonsAreShown(false);
            setMultiSelectionEnabled(true);
        }


        /**
         * Approve the selection
         */
        public void approveSelection() {
            FileChooser.this.doLoad();
        }

        /**
         * Cancel the selection
         */
        public void cancelSelection() {
            closeChooser();
        }

        /**
         * Set the selected files
         *
         * @param selectedFiles  the selected files
         */
        public void setSelectedFiles(File[] selectedFiles) {
            super.setSelectedFiles(selectedFiles);
            setHaveData( !((selectedFiles == null)
                           || (selectedFiles.length == 0)));
        }
    }


    /**
     * _more_
     *
     * @param haveData _more_
     */
    public void setHaveData(boolean haveData) {
        super.setHaveData(haveData);
        if (haveData) {
            setStatus("Press \"" + CMD_LOAD + "\" to load the selected file",
                      "buttons");
        } else {
            setStatus("Please select a file");
        }

    }


    /**
     * Should show the recent (time relative) component
     *
     * @return  false - subclasses should override
     */
    protected boolean shouldShowRecentComponent() {
        return false;
    }



    /**
     * Get the accessory component
     *
     * @return the component
     */
    protected JComponent getAccessory() {
        return GuiUtils.left(
            GuiUtils.inset(
                FileManager.makeDirectoryHistoryComponent(
                    fileChooser, false), new Insets(13, 0, 0, 0)));
    }

    /**
     * Handle the selection of the set of files
     *
     * @param files The files the user chose
     * @param directory The directory they chose them from
     */
    protected final void selectFiles(File[] files, File directory) {
        try {
            if (selectFilesInner(files, directory)) {
                idv.getStateManager().writePreference(PREF_DEFAULTDIR
                        + getId(), directory.getPath());
            }
        } catch (Exception excp) {
            logException("File selection", excp);
        }
    }

    /**
     * Get the file chooser
     *
     * @return  the chooser for this instance
     */
    protected JFileChooser getFileChooser() {
        return fileChooser;
    }

    /**
     * Override the base class method to catch the do load
     */
    public void doLoadInThread() {
        selectFiles(fileChooser.getSelectedFiles(),
                    fileChooser.getCurrentDirectory());
    }



    /**
     * Override the base class method to catch the do update
     */
    public void doUpdate() {
        fileChooser.rescanCurrentDirectory();
    }

    /**
     * Handle the selection of the set of files
     *
     * @param files The files the user chose
     * @param directory The directory they chose them from
     * @return True if the file was successful
     * @throws Exception
     */
    protected boolean selectFilesInner(File[] files, File directory)
            throws Exception {
        if ((files == null) || (files.length == 0)) {
            userMessage("Please select a file");
            return false;
        }
        FileManager.addToHistory(files[0]);
        List    selectedFiles      = new ArrayList();
        String  fileNotExistsError = "";
        boolean didXidv            = false;

        for (int i = 0; i < files.length; i++) {
            if ( !files[i].exists()) {
                fileNotExistsError += "File does not exist: " + files[i]
                                      + "\n";
            } else {
                String filename = files[i].toString();
                //Check for the bundle or jnlp file
                if (idv.getArgsManager().isXidvFile(filename)
                        || idv.getArgsManager().isZidvFile(filename)
                        || idv.getArgsManager().isJnlpFile(filename)) {
                    didXidv = idv.handleAction(filename, null);
                } else {
                    selectedFiles.add(filename);
                }
            }
        }

        if (didXidv) {
            closeChooser();
            return true;
        }


        if (selectedFiles.size() == 0) {
            return false;
        }

        if (fileNotExistsError.length() > 0) {
            userMessage(fileNotExistsError);
            return false;
        }

        Object definingObject = selectedFiles;
        if (selectedFiles.size() == 1) {
            definingObject = selectedFiles.get(0);
        }

        String dataSourceId = getDataSourceId();
        if (dataSourceId == null) {
            dataSourceId = dfltDataSourceId;
        }



        //If the user specifically selected a data source type then pass all files to that data source and be done.
        DataSourceResults results;
        if (dataSourceId == null) {
            //If they selected one directory then ask if they want to load all the files
            if (selectedFiles.size() == 1) {
                File file = new File(selectedFiles.get(0).toString());
                if (file.isDirectory()) {
                    if ( !GuiUtils.showYesNoDialog(null,
                            "Do you want to load all of the files in the selected directory: "
                            + file, "Directory Load")) {
                        return false;
                    }
                    selectedFiles  = new ArrayList();
                    definingObject = selectedFiles;
                    File[] subFiles = file.listFiles();
                    for (int i = 0; i < subFiles.length; i++) {
                        if ( !subFiles[i].isDirectory()) {
                            selectedFiles.add(subFiles[i].toString());
                        }
                    }
                }
            }
        }

        Hashtable   properties  = new Hashtable();
        PollingInfo pollingInfo = new PollingInfo(((doPollingCbx == null)
                ? false
                : doPollingCbx.isSelected()));
        String pattern = getFilePattern();
        if ((pattern != null) && (pattern.length() > 0)) {
            pollingInfo.setFilePattern(pattern);
        }
        properties.put(DataSource.PROP_POLLINFO, pollingInfo);
        return makeDataSource(definingObject, dataSourceId, properties);
    }

    /**
     * Convert the given array of File objects
     * to an array of String file names. Only
     * include the files that actually exist.
     *
     * @param files Selected files
     * @return Selected files as Strings
     */
    protected String[] getFileNames(File[] files) {
        if (files == null) {
            return (String[]) null;
        }
        Vector v                  = new Vector();
        String fileNotExistsError = "";

        // NOTE:  If multiple files are selected, then missing files
        // are not in the files array.  If one file is selected and
        // it is not there, then it is in the array and file.exists()
        // is false
        for (int i = 0; i < files.length; i++) {
            if ((files[i] != null) && !files[i].isDirectory()) {
                if ( !files[i].exists()) {
                    fileNotExistsError += "File does not exist: " + files[i]
                                          + "\n";
                } else {
                    v.add(files[i].toString());
                }
            }
        }

        if (fileNotExistsError.length() > 0) {
            userMessage(fileNotExistsError);
            return null;
        }

        return v.isEmpty()
               ? null
               : StringUtil.listToStringArray(v);
    }



}

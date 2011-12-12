/*
 * $Id: FileManager.java,v 1.22 2007/08/13 18:38:39 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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



package ucar.unidata.util;


import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

import java.util.Vector;

import javax.swing.*;
import javax.swing.filechooser.*;




/**
 * Wrapper cover for JFileChooser.
 * @author Unidata development staff
 * @version $Id: FileManager.java,v 1.22 2007/08/13 18:38:39 jeffmc Exp $
 */
public class FileManager {

    /** _more_ */
    private static boolean fixFileLockup = false;



    /** List of listeners to notify when the list of directories changes */
    static List directoryHistoryListeners = new ArrayList();


    // JNLP workaround
    /*
    private static final Object[] noArgs = {};
    private static final Class[] noArgTypes = {};
    private static Method listRootsMethod = null;
    private static boolean listRootsMethodChecked = false;
    */

    // regular

    /** load url command */
    private static final String CMD_LOADURL = "cmd.loadurl";

    /** Property in the store for the history list */
    public static final String PROP_DIR_HISTORY =
        "FileManager.DirectoryHistory";

    /** Property in the store for the last dir */
    public static final String PROP_DIRECTORY = "filemanager.directory";

    /** parent component */
    private Component parent;

    /** my chooser */
    private javax.swing.JFileChooser chooser = null;

    /** default directories */
    private List defaultDirs = new ArrayList();

    /** flag for a good read */
    private boolean readOk = true;

    /** debug flags */
    private static boolean
        debug = false,
        test  = false;

    /**
     * Create a FileManager and use <code>parent</code> as the
     * parent for the dialog.
     * @param parent  parent component for the dialog.
     */
    public FileManager(Component parent) {
        this(parent, null, new ExtFilter(null, "All Files"));
    }

    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param file_extension  file_extention to use for a filter
     * @param desc  description of files of type <code>file_extension</code>
     */
    public FileManager(Component parent, String defDir,
                       String file_extension, String desc) {
        this(parent, defDir, new ExtFilter(file_extension, desc));
    }

    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filter  default <code>FileFilter</code>
     */
    public FileManager(Component parent, String defDir, FileFilter filter) {
        this(parent, defDir, Misc.newList(filter));
    }

    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filter  default <code>FileFilter</code>
     * @param title  title for the dialog window
     */
    public FileManager(Component parent, String defDir, FileFilter filter,
                       String title) {
        this(parent, defDir, Misc.newList(filter), title);
    }



    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filters  <code>List</code> of default <code>FileFilter</code>'s
     */
    public FileManager(Component parent, String defDir, List filters) {
        this(parent, defDir, filters, null);
    }


    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filters  <code>List</code> of default <code>FileFilter</code>'s
     * @param title  title for the dialog window
     */
    public FileManager(Component parent, String defDir, List filters,
                       String title) {
        this(parent, defDir, filters, title, true);
    }


    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filters  <code>List</code> of default <code>FileFilter</code>'s
     * @param title  title for the dialog window
     * @param includeAllFilter  true to include the "All files" filter.
     */
    public FileManager(Component parent, String defDir, List filters,
                       String title, boolean includeAllFilter) {

        this.parent = parent;

        if (defDir != null) {
            defaultDirs.add(defDir);
        }

        String osName = System.getProperty("os.name");
        if (test) {
            System.out.println("OS ==  " + osName + " def =" + defDir);
        }
        boolean isWindose = (0 <= osName.indexOf("Windows"));

        if (isWindose) {
            defaultDirs.add("C:/");
        }

        File            defaultDirectory = findDefaultDirectory(defaultDirs);
        SecurityManager backup           = System.getSecurityManager();
        System.setSecurityManager(null);
        try {

            chooser = new MyFileChooser(defaultDirectory);
            chooser.setFileHidingEnabled(getFileHidingEnabled());
            if (title != null) {
                chooser.setDialogTitle(title);
            }

            if (filters != null) {
                for (int i = 0; i < filters.size(); i++) {
                    chooser.addChoosableFileFilter(
                        (FileFilter) filters.get(i));
                }
                if (filters.size() > 0) {
                    chooser.setFileFilter((FileFilter) filters.get(0));
                }
            } else if (includeAllFilter) {
                chooser.setFileFilter(chooser.getAcceptAllFileFilter());
            }
            chooser.addPropertyChangeListener(new PropertyChangeListener() {
                File   lastFile           = null;
                String originalFileSuffix = null;
                public void propertyChange(PropertyChangeEvent e) {
                    String prop = e.getPropertyName();
                    if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(
                            prop)) {
                        if (chooser.getSelectedFile() != null) {
                            lastFile = chooser.getSelectedFile();
                            if (originalFileSuffix == null) {
                                originalFileSuffix = IOUtil.getFileExtension(
                                    lastFile.toString());
                            }
                        }
                    }
                    if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(
                            prop)) {
                        File       f          = lastFile;
                        FileFilter fileFilter = chooser.getFileFilter();
                        if (f == null) {
                            return;
                        }
                        String suffix      = null;
                        String newFileName = null;
                        if ((fileFilter != null)
                                && (fileFilter
                                    instanceof PatternFileFilter)) {
                            String filterSuffix =
                                ((PatternFileFilter) fileFilter)
                                    .getPreferredSuffix();
                            if (filterSuffix != null) {
                                suffix = filterSuffix;
                            }
                            //else {
                            //suffix = originalFileSuffix;
                            //}
                        }
                        if (suffix != null) {
                            String filename = f.toString();
                            String fileSuffix =
                                IOUtil.getFileExtension(filename);
                            if ((fileSuffix == null)
                                    || fileSuffix.equals("")) {
                                newFileName = filename + suffix;
                            } else if ( !fileSuffix.equals(suffix)) {
                                newFileName = IOUtil.stripExtension(filename)
                                        + suffix;
                            }
                        }
                        if (newFileName != null) {
                            chooser.setSelectedFile(new File(newFileName));
                        }
                    }
                }
            });
        } catch (SecurityException se) {
            System.out.println("FileManager SecurityException " + se);
            readOk = false;
            JOptionPane.showMessageDialog(
                null,
                "Sorry, this Applet does not have disk read permission.");
        }
        System.setSecurityManager(backup);

    }


    /**
     * Get the chooser that this <code>FileManager</code> wraps.
     * @return the chooser
     */
    public JFileChooser getChooser() {
        return chooser;
    }

    /** _more_ */
    private static boolean fileHidingEnabled = true;


    /**
     * Do we set the FileChooser.useShellFolder=false
     * This fixes the occasional problemo of a system lockup running under windows
     *
     * @param b value
     */
    public static void setFixFileLockup(boolean b) {
        fixFileLockup = b;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static boolean getFileHidingEnabled() {
        return fileHidingEnabled;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public static void setFileHidingEnabled(boolean value) {
        fileHidingEnabled = value;
    }

    /**
     * Set the selected file for the chooser.
     * @param file  name of the file
     */
    public void setSelectedFile(String file) {
        chooser.setSelectedFile(new File(file));
    }

    /**
     * Choose a filename.
     * @return  name of the file.
     */
    public String chooseFilename() {
        return chooseFilename(null);
    }

    /**
     * Choose a filename and set the title in the dialog.
     * @param  title for dialog
     * @return  name of the file.
     */
    public String chooseFilename(String title) {
        return chooseFilename(title, null);
    }


    /**
     * Choose a filename, set the title in the dialog, and set the
     * text of the approve button on the chooser.
     * @param  title  title for the dialog window
     * @param  approveButtonText text for the approve button in the dialog
     * @return  name of the file.
     */
    public String chooseFilename(String title, String approveButtonText) {
        return chooseFilename(title, approveButtonText, false);
    }


    /**
     * Choose a filename, set the title in the dialog, and set the
     * text of the approve button on the chooser.
     * @param  title  title for the dialog window
     * @param  approveButtonText text for the approve button in the dialog
     * @param includeUrl  inclues a URL
     * @return  name of the file.
     */
    public String chooseFilename(String title, String approveButtonText,
                                 boolean includeUrl) {
        if ( !readOk) {
            return null;
        }
        if (approveButtonText != null) {
            chooser.setApproveButtonText(approveButtonText);
        }

        SecurityManager backup = System.getSecurityManager();
        System.setSecurityManager(null);
        if (title != null) {
            chooser.setDialogTitle(title);
        }

        final JDialog dialog = new JDialog((Frame) null, ((title != null)
                ? title
                : "Choose File"), true);

        ObjectListener listener = new ObjectListener(null) {
            public void actionPerformed(ActionEvent ae) {
                theObject = ae.getActionCommand();
                dialog.dispose();
            }
        };
        chooser.addActionListener(listener);
        if (approveButtonText != null) {
            chooser.setApproveButtonText(approveButtonText);
        } else if (chooser.getDialogType() == JFileChooser.OPEN_DIALOG) {
            chooser.setApproveButtonText("Open");
        } else {
            chooser.setApproveButtonText("Save");
        }

        JComponent contents = chooser;
        JTextField urlField = null;
        if (includeUrl) {
            urlField = new JTextField("", 30);
            JButton urlButton = new JButton("Load URL:");
            urlButton.setActionCommand(CMD_LOADURL);
            urlField.setActionCommand(CMD_LOADURL);
            urlButton.addActionListener(listener);
            urlField.addActionListener(listener);
            JPanel urlPanel = LayoutUtil.doLayout(new Component[] { urlButton,
                    urlField }, 2, LayoutUtil.WT_NY, LayoutUtil.WT_N);
            contents = LayoutUtil.centerBottom(contents,
                    LayoutUtil.inset(urlPanel, 5));
        }


        List       directoryHistory = getHistoryList();
        JComponent historyComp = makeDirectoryHistoryComponent(chooser, true);
        if ((directoryHistory != null) && (directoryHistory.size() > 0)) {
            contents = LayoutUtil.centerRight(contents,
                    LayoutUtil.top(LayoutUtil.inset(historyComp,
                        new Insets(13, 0, 0, 10))));
        }



        dialog.getContentPane().add(contents);

        //Show the dialog in the awt thread to prevent a deadlock on macs
        GuiUtils.invokeInSwingThread(new Runnable() {
            public void run() {
                GuiUtils.showInCenter(dialog);
            }
        });



        System.setSecurityManager(backup);
        File file = chooser.getSelectedFile();
        addToHistory(file);


        if (debug) {
            System.out.println("FileManager result " + file);
        }
        if (listener.getObject().equals(CMD_LOADURL)) {
            return urlField.getText().trim();
        } else if (listener.getObject().equals(
                JFileChooser.APPROVE_SELECTION)) {
            return file.getPath();
        } else {
            return null;
        }
    }


    /**
     * Popup the history menu
     *
     * @param goToBtn Where to pop up
     * @param chooser Which chooser to apply directory change to
     */
    private static void showGoToMenu(JButton goToBtn,
                                     final JFileChooser chooser) {
        List directoryHistory = getHistoryList();
        List items            = new ArrayList();
        if (directoryHistory != null) {
            for (int i = 0; i < directoryHistory.size(); i++) {
                final String dirPath = directoryHistory.get(i).toString();
                JMenuItem    mi      = new JMenuItem(dirPath);
                items.add(mi);
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        chooser.setCurrentDirectory(new File(dirPath));
                    }
                });
            }
        }
        if (items.size() == 0) {
            items.add(new JMenuItem("No file history"));
        }

        JPopupMenu popup = MenuUtil.makePopupMenu(new JPopupMenu(), items);
        popup.show(goToBtn, 0, (int) goToBtn.getBounds().getHeight());


    }


    /**
     * Get the history list. Add the listener to the list of listeners.
     *
     * @param listener Listener to notify
     *
     * @return List of (String) directories from the user's past use
     */
    public static List getHistoryList(ActionListener listener) {
        if ((listener != null)
                && !directoryHistoryListeners.contains(listener)) {
            directoryHistoryListeners.add(listener);
        }
        return getHistoryList();
    }


    /**
     * Get the history list.
     *
     *
     * @return List of (String) directories from the user's past use
     */
    public static List getHistoryList() {
        if (fileStore == null) {
            return new ArrayList();
        }
        List l = (List) fileStore.get(PROP_DIR_HISTORY);
        if (l == null) {
            l = new ArrayList();
        }
        return l;
    }

    /**
     * Add the file (or its parent directory if it is not a directory)
     * to the history list.
     *
     * @param file File to add
     */
    public static void addToHistory(File file) {
        if ((file == null) || (fileStore == null)) {
            return;
        }
        List directoryHistory = getHistoryList();
        File dir              = file;
        if ( !file.isDirectory()) {
            dir = file.getParentFile();
        }
        if (directoryHistory == null) {
            directoryHistory = new ArrayList();
        }
        int index = directoryHistory.indexOf(dir.toString());
        if (index >= 0) {
            directoryHistory.remove(index);
        }
        directoryHistory.add(0, dir.toString());
        while (directoryHistory.size() > 20) {
            directoryHistory.remove(directoryHistory.size() - 1);
        }
        fileStore.put(PROP_DIR_HISTORY, directoryHistory);
        fileStore.save();
        for (int i = 0; i < directoryHistoryListeners.size(); i++) {
            ActionListener listener =
                (ActionListener) directoryHistoryListeners.get(i);
            listener.actionPerformed(null);
        }
    }


    /**
     * Set the accessory for the file chooser to <code>comp</code>.
     * @param comp  component to use for the accessory.
     */
    public void setAccessory(JComponent comp) {
        chooser.setAccessory(comp);
    }

    /**
     * Get the current directory name.
     * @return  name of the current directory.
     */
    public String getDirectoryName() {
        return chooser.getCurrentDirectory().getPath();
    }

    /**
     * Set the approve button text for the chooser.
     * @param approveButtonText  text for the button.
     */
    public void setApproveButtonText(String approveButtonText) {
        chooser.setApproveButtonText(approveButtonText);
    }

    /**
     * Find the default directory from a list
     *
     * @param tryDefaultDirectories
     * @return the default directory
     */
    private File findDefaultDirectory(List tryDefaultDirectories) {
        boolean readOK = true;
        for (int i = 0; i < tryDefaultDirectories.size(); i++) {
            try {
                String dirName = (String) tryDefaultDirectories.get(i);
                if (debug) {
                    System.out.print("FileManager try " + dirName);
                }
                File dir = new File(dirName);
                if (dir.exists()) {
                    if (debug) {
                        System.out.println(" ok ");
                    }
                    return dir;
                } else {
                    if (debug) {
                        System.out.println("no ");
                    }
                    continue;
                }
            } catch (SecurityException se) {
                if (debug) {
                    System.out.println("SecurityException in FileManager: "
                                       + se);
                }
                readOK = false;
            }
        }

        if ( !readOK) {
            JOptionPane.showMessageDialog(
                null,
                "Sorry, this Applet does not have disk read permission.");
        }
        return null;
    }

    /**
     * A <code>FileFilter</code> extension that allows specifiying
     * an extension and description.
     */
    public static class ExtFilter extends FileFilter {

        /** file extension */
        String file_extension;

        /** descriptor for the extension */
        String desc;

        /**
         * Create an <code>ExtFilter</code> with the given params.
         * @param file_extension  extension for files.
         * @param desc  description of these files.
         */
        public ExtFilter(String file_extension, String desc) {
            this.file_extension = file_extension;
            this.desc           = desc;
        }

        /**
         * Accept the file.
         * @param  file  file to accept
         * @return  true if it matches the extension for this file.
         */
        public boolean accept(File file) {
            if (null == file_extension) {
                return true;
            }
            String name = file.getName();
            return file.isDirectory() || name.endsWith(file_extension);
        }

        /**
         * Get the description for the files this filter accepts.
         * @return description.
         */
        public String getDescription() {
            return desc;
        }

    }

    /**
     * A <code>FileFilter</code> extension for netCDF files.
     */
    public static class NetcdfExtFilter extends FileFilter {

        /**
         * Accept a file if it is deemed a netCDF file (.nc or .cdf)
         *
         * @param file
         * @return true if it has the right suffix
         */
        public boolean accept(File file) {
            String name = file.getName();
            return file.isDirectory() || name.endsWith(".nc")
                   || name.endsWith(".cdf");
        }

        /**
         * Get the description for this filter
         * @return the description
         */
        public String getDescription() {
            return "netcdf";
        }
    }

    /* claimed workaround for WebStart

    http://forum.java.sun.com/read/56761/q_D4Xl3nS380AAZX5#LR


    Hi Surya,

    Unfortunately it looks like you have run into an annoying bug in the Java 2 SDK v1.2.2 or v1.3 .
    This bug will be fixed in a future Java 2 SDK release. In the meantime, there are a couple workarounds:

    1) Use the JNLP API FileOpenService and FileSaveService to do file operations; the reference
    implementation uses the workaround I list under 2) below.

    2) Keep your current code, but use a customized FileSystemView that you supply to JFileChooser
    when instantiating it on Windows. e.g. :

    new JFileChooser(currentDirectory, new WindowsAltFileSystemView ())

    Code for WindowsAltFileSystemView follows, with apologies for the formatting :

    // This class is necessary due to an annoying bug on Windows NT where
    // instantiating a JFileChooser with the default FileSystemView will
    // cause a "drive A: not ready" error every time. I grabbed the
    // Windows FileSystemView impl from the 1.3 SDK and modified it so
    // as to not use java.io.File.listRoots() to get fileSystem roots.
    // java.io.File.listRoots() does a SecurityManager.checkRead() which
    // causes the OS to try to access drive A: even when there is no disk,
    // causing an annoying "abort, retry, ignore" popup message every time
    // we instantiate a JFileChooser!
    //
    // Instead of calling listRoots() we use a straightforward alternate
    // method of getting file system roots. */


    /**
     * Class WindowsAltFileSystemView
     *
     */
    private class WindowsAltFileSystemView extends FileSystemView {

        /**
         * Returns true if the given file is a root.
         *
         * @param f
         * @return true if it is a root file system
         */
        public boolean isRoot(File f) {
            if ( !f.isAbsolute()) {
                return false;
            }

            String parentPath = f.getParent();
            if (parentPath == null) {
                return true;
            } else {
                File parent = new File(parentPath);
                return parent.equals(f);
            }
        }

        /**
         * creates a new folder with a default folder name.
         *
         * @param containingDir
         * @return new file in this directory
         *
         * @throws IOException
         */
        public File createNewFolder(File containingDir) throws IOException {
            if (containingDir == null) {
                throw new IOException("Containing directory is null:");
            }

            File newFolder = null;
            // Using NT's default folder name
            newFolder = createFileObject(containingDir, "New Folder");
            int i = 2;
            while (newFolder.exists() && (i < 100)) {
                newFolder = createFileObject(containingDir,
                                             "New Folder (" + i + ")");
                i++;
            }

            if (newFolder.exists()) {
                throw new IOException("Directory already exists:"
                                      + newFolder.getAbsolutePath());
            } else {
                newFolder.mkdirs();
            }

            return newFolder;
        }

        /**
         * Returns whether a file is hidden or not. On Windows
         * there is currently no way to get this information from
         * io.File, therefore always return false.
         *
         * @param f
         * @return true if the file is hidden
         */
        public boolean isHiddenFile(File f) {
            return false;
        }

        /**
         * Returns all root partitians on this system. On Windows, this
         * will be the A: through Z: drives.
         * @return get all the root directories
         */
        public File[] getRoots() {
            Vector rootsVector = new Vector();

            System.out.println(" getRoots ");

            // Create the A: drive whether it is mounted or not
            FileSystemRoot floppy = new FileSystemRoot("A" + ":" + "\\");
            rootsVector.addElement(floppy);

            // Run through all possible mount points and check
            // for their existance.
            for (char c = 'C'; c <= 'Z'; c++) {
                char   device[]   = { c, ':', '\\' };
                String deviceName = new String(device);
                System.out.println(" try ");
                System.out.println(" " + deviceName);
                File    deviceFile = new FileSystemRoot(deviceName);
                boolean ok         = deviceFile.exists();
                System.out.println(" " + ok);
                if ((deviceFile != null) && deviceFile.exists()) {
                    rootsVector.addElement(deviceFile);
                    System.out.println(" use " + deviceName);
                }
            }

            File[] roots = new File[rootsVector.size()];
            rootsVector.copyInto(roots);
            return roots;
        }
    }  // class WindowsAltFileSystemView

    /**
     * Class FileSystemRoot
     *
     */
    private class FileSystemRoot extends File {

        /**
         * Create a root file system for the file
         *
         * @param f  file
         *
         */
        public FileSystemRoot(File f) {
            super(f, "");
        }

        /**
         * Create a root file system for the name of the file
         *
         * @param s  name of the file
         *
         */
        public FileSystemRoot(String s) {
            super(s);
        }

        /**
         * Is this a directory
         * @return true
         */
        public boolean isDirectory() {
            return true;
        }
    }  // class FileSystemRoot



    /** default property for location of writing files */
    private static String defaultWriteProperty = "property.filewrite.dir";

    /** default property for location of reading files */
    private static String defaultReadProperty = "property.fileread.dir";

    /** the file store */
    private static PersistentStore fileStore;

    /** default for including all files filter */
    private static final boolean DFLT_INCLUDEALLFILTER = true;


    /** null list of filters */
    private static final List NULL_FILTERS = null;

    /** null suffix */
    private static final String NULL_SUFFIX = null;

    /** Suffix for XML files */
    public static final String SUFFIX_XML = ".xml";

    /** _more_ */
    public static final String SUFFIX_AVI = ".avi";

    /** Suffix for CSV files */
    public static final String SUFFIX_CSV = ".csv";

    /** Suffix for CSV files */
    public static final String SUFFIX_XLS = ".xls";

    /** Suffix for CSV files */
    public static final String SUFFIX_KML = ".kml";

    /** Suffix for CSV files */
    public static final String SUFFIX_KMZ = ".kmz";

    /** Suffix for JPEG files */
    public static final String SUFFIX_JPG = ".jpg";

    /** Suffix for PNG files */
    public static final String SUFFIX_PNG = ".png";

    /** Suffix for GIF files */
    public static final String SUFFIX_GIF = ".gif";

    /** Suffix for Quicktime files */
    public static final String SUFFIX_MOV = ".mov";

    /** Suffix for CSV files */
    public static final String SUFFIX_TXT = ".txt";

    /** Suffix for log files */
    public static final String SUFFIX_LOG = ".log";

    /** Suffix for netCDF files */
    public static final String SUFFIX_NETCDF = ".nc";

    /** Suffix for JAR files */
    public static final String SUFFIX_JAR = ".jar";


    /** null accessory */
    private static final JComponent NULL_ACCESSORY = null;


    /** Filter for XML files */
    public static final PatternFileFilter FILTER_XML =
        new PatternFileFilter(
            ".+\\.xml", "eXtensible Markup Language (XML) files (*.xml)",
            SUFFIX_XML);



    /** Filter for netCDF files */
    public static final PatternFileFilter FILTER_NETCDF =
        new PatternFileFilter(".+\\.nc", "netCDF files (*.nc)",
                              SUFFIX_NETCDF);


    /** Filter for CSV files */
    public static final PatternFileFilter FILTER_CSV =
        new PatternFileFilter(".+\\.csv",
                              "Comma-Separated Values (CSV) files (*.csv)",
                              SUFFIX_CSV);


    /** Filter for Text files */
    public static final PatternFileFilter FILTER_TXT =
        new PatternFileFilter(".+\\.txt", "Text Files (*.txt)", SUFFIX_TXT);


    /** Filter for xls files */
    public static final PatternFileFilter FILTER_XLS =
        new PatternFileFilter(".+\\.xls", "Microsoft Excel files",
                              SUFFIX_XLS);

    /** Filter for xls files */
    public static final PatternFileFilter FILTER_KML =
        new PatternFileFilter(".+\\.kml", "Google Earth files", SUFFIX_KML);

    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_JAR =
        new PatternFileFilter("(.+\\.jar$)", "Jar Files (*.jar)", ".jar");


    /** Filter for JPEG files */
    public static final PatternFileFilter FILTER_JPG =
        new PatternFileFilter(".+\\.jpg", "JPEG files (*.jpg)", SUFFIX_JPG);

    /** Filter for Image files */
    public static final PatternFileFilter FILTER_IMAGE =
        new PatternFileFilter(".+\\.jpg|.+\\.gif|.+\\.jpeg|.+\\.png",
                              "Image files (*.jpg,*.gif,*.png)");

    /** Filter for Image files for writing */
    public static final PatternFileFilter FILTER_IMAGEWRITE =
        new PatternFileFilter(".+\\.jpg|.+\\.jpeg|.+\\.png",
                              "Image files (*.jpg,*.png)");

    /** Filter for image or pdf files */
    public static final PatternFileFilter FILTER_IMAGE_OR_PDF =
        new PatternFileFilter(".+\\.jpg|.+\\.jpeg|.+\\.png|\\.pdf",
                              "Image files (*.jpg,*.png) or PDF (*.pdf)");


    /** Filter for QuickTime files */
    public static final PatternFileFilter FILTER_MOV =
        new PatternFileFilter(".+\\.mov", "QuickTime files (*.mov)",
                              SUFFIX_MOV);

    /** Filter for QuickTime files */
    public static final PatternFileFilter FILTER_AVI =
        new PatternFileFilter(".+\\.avi", "AVI files (*.avi)", SUFFIX_AVI);

    /** Filter for QuickTime files */
    public static final PatternFileFilter FILTER_ANIMATEDGIF =
        new PatternFileFilter(".+\\.gif", "Animated GIF (*.gif)", SUFFIX_GIF);




    /** Filter for log files */
    public static final PatternFileFilter FILTER_LOG =
        new PatternFileFilter(".+\\.log", "Log files (*.log)");


    /** Filter for kmz files */
    public static final PatternFileFilter FILTER_KMZ =
        new PatternFileFilter(".+\\.kmz", "Google Earth KMZ Files (*.kmz)",
                              ".kmz");




    /** null string intrinsic */
    public static final String NULL_STRING = null;

    /** null button text intrinsic */
    public static final String NULL_BTNTEXT = null;

    /** null title intrinsic */
    public static final String NULL_TITLE = null;


    /**
     * Set the persistent store for this FileManager.
     * @param  store  store for persistence
     * @param  writeProperty  write property
     * @param  readProperty   read  property
     */
    public static void setStore(PersistentStore store, String writeProperty,
                                String readProperty) {
        FileManager.defaultWriteProperty = writeProperty;
        FileManager.defaultReadProperty  = readProperty;
        FileManager.fileStore            = store;
    }


    /**
     * Return a directory selection.
     *
     *
     * @param dfltDir
     * @return A directory or nulll if none selected.
     */
    public static File getDirectory(String dfltDir) {
        return getDirectory(dfltDir, "Please select a directory");
    }

    /**
     * Have the user select a directory
     *
     * @param dfltDir Default dir
     * @param title Window title
     *
     * @return The selected directory or null  if none selected
     */
    public static File getDirectory(String dfltDir, String title) {
        return getDirectory(dfltDir, title, null);
    }

    /**
     * Have our own class here so it can put the hook in
     * to fix the lock up problem on windows.
     */
    public static class MyFileChooser extends JFileChooser {

        /**
         * _more_
         *
         * @param dir _more_
         */
        public MyFileChooser(File dir) {
            super(dir);
        }

        /**
         * _more_
         *
         * @param path _more_
         */
        public MyFileChooser(String path) {
            super(path);
        }

        /**
         * _more_
         */
        public MyFileChooser() {}

        /**
         * _more_
         */
        public void updateUI() {
            if (fixFileLockup) {
                putClientProperty("FileChooser.useShellFolder",
                                  Boolean.FALSE);
            }
            super.updateUI();
        }
    }


    /**
     * _more_
     *
     * @param dfltDir _more_
     * @param title _more_
     * @param accessory _more_
     *
     * @return _more_
     */
    public static File getDirectory(String dfltDir, String title,
                                    JComponent accessory) {
        if ((dfltDir == null) && (fileStore != null)) {
            dfltDir = (String) fileStore.get(PROP_DIRECTORY);
        }

        JFileChooser chooser = (dfltDir != null)
                               ? new MyFileChooser(dfltDir)
                               : new MyFileChooser();

        chooser.setApproveButtonText("Save");
        if (accessory != null) {
            accessory = GuiUtils.inset(accessory, 2);
            chooser.setAccessory(accessory);
        }

        if (title != null) {
            chooser.setDialogTitle(title);
        }
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }

            public String getDescription() {
                return "Directories";
            }
        });

        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (fileStore != null) {
            fileStore.put(PROP_DIRECTORY, file.toString());
            fileStore.save();
        }
        return file;
    }


    /**
     * Get the file that this FileManager will write to using the defaults.
     *
     * @param filename default file name
     *
     * @return the requested file name
     */
    public static String getWriteFile(String filename) {
        return getFile(true, NULL_TITLE, NULL_BTNTEXT, defaultWriteProperty,
                       NULL_FILTERS, NULL_SUFFIX, DFLT_INCLUDEALLFILTER,
                       NULL_ACCESSORY, false, filename);
    }


    /**
     * Get the file that this FileManager will write to using the defaults.
     * @return name of the file
     */
    public static String getWriteFile() {
        return getFile(true, NULL_TITLE, NULL_BTNTEXT, defaultWriteProperty,
                       NULL_FILTERS, NULL_SUFFIX, DFLT_INCLUDEALLFILTER,
                       NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will write to using the specified
     * parameters.
     * @param filters   <code>List</code> of file filters
     * @param suffix    default suffix
     * @return name of the file
     */
    public static String getWriteFile(List filters, String suffix) {
        return getFile(true, NULL_TITLE, NULL_BTNTEXT, defaultWriteProperty,
                       filters, suffix, DFLT_INCLUDEALLFILTER,
                       NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will write to using the specified
     * parameters.
     * @param filter   file filter
     * @param suffix   default suffix
     * @return name of the file
     */
    public static String getWriteFile(FileFilter filter, String suffix) {
        return getFile(true, NULL_TITLE, NULL_BTNTEXT, defaultWriteProperty,
                       Misc.newList(filter), suffix, DFLT_INCLUDEALLFILTER,
                       NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will write to using the specified
     * parameters.
     * @param filter   file filter
     * @param suffix    default suffix
     * @param accessory accessory component
     * @return name of the file
     */
    public static String getWriteFile(FileFilter filter, String suffix,
                                      JComponent accessory) {

        return getFile(true, NULL_TITLE, NULL_BTNTEXT, defaultWriteProperty,
                       Misc.newList(filter), suffix, DFLT_INCLUDEALLFILTER,
                       accessory, false);
    }

    /**
     * Get the file that this FileManager will write to using the specified
     * parameters.
     * @param filters   file filters
     * @param suffix    default suffix
     * @param accessory accessory component
     * @return name of the file
     */
    public static String getWriteFile(List filters, String suffix,
                                      JComponent accessory) {

        return getFile(true, NULL_TITLE, NULL_BTNTEXT, defaultWriteProperty,
                       filters, suffix, DFLT_INCLUDEALLFILTER, accessory,
                       false);
    }

    /**
     * Get the file that this FileManager will write to using the specified
     * parameters.
     * @param title     title for the dialog.
     * @param filter    file filter
     * @param suffix    default suffix
     * @return name of the file
     */
    public static String getWriteFile(String title, FileFilter filter,
                                      String suffix) {

        return getFile(true, title, NULL_BTNTEXT, defaultWriteProperty,
                       Misc.newList(filter), suffix, DFLT_INCLUDEALLFILTER,
                       NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will write to using the specified
     * parameters.
     * @param title     title for the dialog.
     * @param filters   <code>List</code> of file filters
     * @param suffix    default suffix
     * @return name of the file
     */
    public static String getWriteFile(String title, List filters,
                                      String suffix) {
        return getFile(true, title, NULL_BTNTEXT, defaultWriteProperty,
                       filters, suffix, DFLT_INCLUDEALLFILTER,
                       NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will read from using the defaults.
     * @return name of the file
     */
    public static String getReadFile() {
        return getFile(false, NULL_TITLE, NULL_BTNTEXT, defaultReadProperty,
                       NULL_FILTERS, NULL_SUFFIX, DFLT_INCLUDEALLFILTER,
                       NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will read from using the specified
     * parameters to configure the widget.
     * @param  filter  filter to use
     * @return name of the file
     */
    public static String getReadFile(FileFilter filter) {
        return getFile(false, NULL_TITLE, NULL_BTNTEXT, defaultReadProperty,
                       Misc.newList(filter), NULL_SUFFIX,
                       DFLT_INCLUDEALLFILTER, NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will read from using the specified
     * parameters to configure the widget.
     * @param  filter  filter to use
     * @param  includeAllFilter  true to include the "All files (*.*)" filter
     * @return name of the file
     */
    public static String getReadFile(FileFilter filter,
                                     boolean includeAllFilter) {
        return getFile(false, NULL_TITLE, NULL_BTNTEXT, defaultReadProperty,
                       Misc.newList(filter), NULL_SUFFIX, includeAllFilter,
                       NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will read from using the specified
     * parameters to configure the widget.
     * @param  title   title for the dialog
     * @param  filter  filter to use
     * @return name of the file
     */
    public static String getReadFile(String title, FileFilter filter) {
        return getFile(false, title, NULL_BTNTEXT, defaultReadProperty,
                       Misc.newList(filter), NULL_SUFFIX,
                       DFLT_INCLUDEALLFILTER, NULL_ACCESSORY, false);
    }

    /**
     * Get the file that this FileManager will read from using the specified
     * parameters to configure the widget.
     * @param  title   title for the dialog
     * @param  filters <code>List</code> of filters to use
     * @return name of the file
     */
    public static String getReadFile(String title, List filters) {
        return getFile(false, title, NULL_BTNTEXT, defaultReadProperty,
                       filters, NULL_SUFFIX, DFLT_INCLUDEALLFILTER,
                       NULL_ACCESSORY, false);
    }


    /**
     * Get the file that this FileManager will read from using the specified
     * parameters to configure the widget.
     * @param  title   title for the dialog
     * @param  filters <code>List</code> of filters to use
     * @param accessory  accessory for the chooser (may be null)
     * @return name of the file
     */
    public static String getReadFile(String title, List filters,
                                     JComponent accessory) {
        return getFile(false, title, NULL_BTNTEXT, defaultReadProperty,
                       filters, NULL_SUFFIX, DFLT_INCLUDEALLFILTER,
                       accessory, false);
    }

    /**
     * Get the file that this FileManager will read from using the specified
     * parameters to configure the widget.
     * @param  title   title for the dialog
     * @param  filters <code>List</code> of filters to use
     * @param accessory  accessory for the chooser (may be null)
     * @return name of the file
     */
    public static String getReadFileOrURL(String title, List filters,
                                          JComponent accessory) {
        return getFile(false, title, NULL_BTNTEXT, defaultReadProperty,
                       filters, NULL_SUFFIX, DFLT_INCLUDEALLFILTER,
                       accessory, true);
    }


    /**
     * Get the file that this FileManager is pointing to.
     * @param  forWrite     true if this is for getting a file to write to
     * @param  title        title for the dialog
     * @param  buttonText   text for the approve button
     * @param  property     property for the store
     * @param  filters <code>List</code> of filters to use
     * @param  suffix       default suffix for file to write
     * @param  includeAllFilter  true to include the "All files (*.*)" filter
     * @param  accessory    accessory for the chooser
     * @param includeUrl    allow a URL
     * @return name of the file
     */
    public static String getFile(boolean forWrite, String title,
                                 String buttonText, String property,
                                 List filters, String suffix,
                                 boolean includeAllFilter,
                                 JComponent accessory, boolean includeUrl) {

        return getFile(forWrite, title, buttonText, property, filters,
                       suffix, includeAllFilter, accessory, includeUrl, null);
    }




    /**
     * Get the file that this FileManager is pointing to.
     * @param  forWrite     true if this is for getting a file to write to
     * @param  title        title for the dialog
     * @param  buttonText   text for the approve button
     * @param  property     property for the store
     * @param  filters <code>List</code> of filters to use
     * @param  suffix       default suffix for file to write
     * @param  includeAllFilter  true to include the "All files (*.*)" filter
     * @param  accessory    accessory for the chooser
     * @param includeUrl    allow a URL
     * @param dfltFile      the default file
     * @return name of the file
     */
    public static String getFile(boolean forWrite, String title,
                                 String buttonText, String property,
                                 List filters, String suffix,
                                 boolean includeAllFilter,
                                 JComponent accessory, boolean includeUrl,
                                 String dfltFile) {

        String fileDir      = null;
        String lastFileName = null;
        if ((fileStore != null) && (property != null)) {
            lastFileName = (String) fileStore.get(property);
            if (lastFileName != null) {
                File lastFile = new File(lastFileName);
                fileDir = lastFile.getParent();
            }
        }
        title = ((title != null)
                 ? title
                 : (forWrite
                    ? "Save"
                    : "Open"));




        FileManager fileManager = new FileManager((JFrame) null, fileDir,
                                      filters, title, includeAllFilter);
        FileFilter defaultFileFilter = null;
        if (dfltFile != null) {
            File dir = fileManager.getChooser().getCurrentDirectory();
            dfltFile = IOUtil.joinDir(dir, IOUtil.getFileTail(dfltFile));
            fileManager.getChooser().setSelectedFile(new File(dfltFile));
        } else if (lastFileName != null) {
            if ((filters != null) && (filters.size() > 0)) {
                boolean ok = false;
                for (int i = 0; !ok && (i < filters.size()); i++) {
                    FileFilter tmpFileFilter = (FileFilter) filters.get(i);
                    ok = ok || tmpFileFilter.accept(new File(lastFileName));
                    if (ok) {
                        defaultFileFilter = tmpFileFilter;
                    }
                }
                if ( !ok) {
                    lastFileName = null;
                }
            }
            if (lastFileName != null) {
                fileManager.setSelectedFile(lastFileName);
            }
        }
        if (accessory != null) {
            accessory = GuiUtils.inset(accessory, 2);
            fileManager.setAccessory(accessory);
        }
        fileManager.getChooser().setDialogType((forWrite
                ? JFileChooser.SAVE_DIALOG
                : JFileChooser.OPEN_DIALOG));

        if (defaultFileFilter != null) {
            fileManager.getChooser().setFileFilter(defaultFileFilter);
        }


        while (true) {

            String filename = fileManager.chooseFilename(title,
                                  (buttonText != null)
                                  ? buttonText
                                  : (forWrite
                                     ?"Save"
                                     :"Open"),includeUrl);


            if (filename == null) {
                return null;
            }



            String tail = IOUtil.getFileTail(filename);

            if (tail.indexOf(".") < 0) {
                //Get the suffix from the file filter if we don't have one
                FileFilter fileFilter =
                    fileManager.getChooser().getFileFilter();
                if ((fileFilter != null)
                        && (fileFilter instanceof PatternFileFilter)) {
                    String filterSuffix =
                        ((PatternFileFilter) fileFilter).getPreferredSuffix();
                    if (filterSuffix != null) {
                        suffix = filterSuffix;
                    }
                }
                if (suffix != null) {
                    filename = filename + suffix;
                }
            }

            File file = new File(filename);
            if (forWrite) {
                boolean isWritable = true;
                if (file.exists()) {
                    if (JOptionPane
                            .showConfirmDialog(null, "File:" + filename
                                + " exists. Do you want to overwrite?", "File exists", JOptionPane
                                    .YES_NO_OPTION) == 1) {
                        return null;
                    }
                    isWritable = file.canWrite();
                } else {
                    File parent = file.getParentFile();
                    if (parent != null) {
                        isWritable = parent.canWrite();
                    }

                }
                if ( !isWritable) {
                    if ( !GuiUtils.askOkCancel("File Selection",
                            "The chosen file path is not writable. Select again?")) {
                        return null;
                    }
                    continue;
                }
            }
            String dir = file.getParent();
            if ((fileStore != null) && (dir != null)) {
                fileStore.put(property, filename);
                fileStore.save();
            }
            return filename;
        }
    }




    /**
     * Create the directory history button and menu
     *
     * @param fileChooser The chooser to set the dir on
     * @param includeLabel Should the label be included in the component.
     *
     * @return The jbutton and label that pops up the directory history list
     */
    public static JComponent makeDirectoryHistoryComponent(
            final JFileChooser fileChooser, boolean includeLabel) {
        final JButton goToBtn =
            new JButton(
                "",
                GuiUtils.getImageIcon("/auxdata/ui/icons/folder_go.png"));
        goToBtn.setContentAreaFilled(false);
        goToBtn.setMargin(new Insets(1, 1, 1, 1));
        //        goToBtn.setBorder(BorderFactory.createEtchedBorder());
        goToBtn.setToolTipText("Show history menu");
        goToBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showGoToMenu(goToBtn, fileChooser);
            }
        });

        if ( !includeLabel) {
            return goToBtn;
        }
        return goToBtn;
        //       JPanel contents = GuiUtils.label("  Directory History: ", goToBtn);
        //        return contents;
    }

}

